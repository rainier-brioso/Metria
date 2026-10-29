import base64
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from debug_capture import DebugCapture
import vision_probe as vision


def completion(content=None, finish="stop", usage=None):
    draft = {"foods": [{"name": "chicken", "estimated_grams": 150, "confidence": "medium"}],
             "uncertainties": ["No scale; hidden oil is unknown."], "needs_review": True}
    return json.dumps({"model": vision.MODEL,
                       "choices": [{"finish_reason": finish, "message": {"content": json.dumps(draft) if content is None else content}}],
                       "usage": usage or {"prompt_tokens": 1000, "completion_tokens": 100, "total_tokens": 1100}}).encode()


class VisionProbeTests(unittest.TestCase):
    def test_fixture_is_pinned_and_prompt_does_not_reveal_expected_foods(self):
        body, image, manifest = vision.build_request()
        request = json.loads(body)
        encoded = request["messages"][1]["content"][1]["image_url"]["url"].split(",", 1)[1]
        self.assertEqual(image, base64.b64decode(encoded))
        self.assertEqual(manifest["sha256"], hashlib.sha256(image).hexdigest())
        self.assertNotIn("chicken", body.decode().split('data:image')[0].lower())
        self.assertEqual(vision.MODEL, request["model"])

    def test_usage_kept_on_bad_content_or_truncation_and_cost_estimated(self):
        valid = vision.inspect_response(completion(), 200, False)
        self.assertTrue(valid["validated"])
        self.assertEqual(.00039, valid["estimated_cost_usd"])
        for raw, truncated in [(completion("not JSON"), False), (completion(finish="length"), False),
                               (completion(), True)]:
            result = vision.inspect_response(raw, 200, truncated)
            self.assertFalse(result["validated"])
            self.assertEqual(1100, result["usage"]["total_tokens"])

    def test_rejects_unsafe_draft_and_bad_usage_without_losing_raw_capture(self):
        draft = {"foods": [{"name": "rice", "estimated_grams": float("nan"), "confidence": "high"}],
                 "uncertainties": ["unknown"], "needs_review": True}
        self.assertFalse(vision.inspect_response(completion(json.dumps(draft)), 200, False)["validated"])
        self.assertFalse(vision.inspect_response(completion(usage={"prompt_tokens": True,
                         "completion_tokens": 1, "total_tokens": 2}), 200, False)["validated"])
        self.assertFalse(vision.inspect_response(b'{"error":"denied"}', 403, False)["validated"])

    def test_capture_preserves_exact_request_image_and_redacts_secret(self):
        body, image, manifest = vision.build_request()
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            capture = DebugCapture("synthetic-food-vision", vision.MODEL, "id", "fictional-secret")
            capture.record_fictional_request(body, image, manifest, vision.ENDPOINT + "chat/completions")
            capture.on_response(b'{"error":"fictional-secret"}', 400)
            capture.record_probe_result({"validated": False, "usage": None})
            capture.finish("rejected")
            data = json.loads(capture.path.read_text())
            self.assertEqual(body, data["request_body_text"].encode())
            self.assertEqual(image, capture.path.with_suffix(".png").read_bytes())
            self.assertNotIn("fictional-secret", capture.path.read_text())

    def test_prepare_never_reads_key_or_sends_and_live_requires_account_check(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)), \
                patch("vision_probe.send_once") as send, patch("vision_probe._read_key") as read:
            self.assertEqual(0, vision.main(["--prepare"]))
            self.assertEqual(2, vision.main(["--live"]))
            send.assert_not_called()
            read.assert_not_called()

    def test_live_one_call_and_quota_persists(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root) / "captures"), \
                patch("vision_probe.STATE", Path(root) / "state"), \
                patch("vision_probe._read_key", return_value="fake-provider-key"), \
                patch("vision_probe.send_once", return_value=(200, completion(), False)) as send:
            self.assertEqual(0, vision.main(["--live", "--account-verified"]))
            send.assert_called_once()
            import sqlite3
            from contextlib import closing
            with closing(sqlite3.connect(Path(root) / "state/admission.sqlite3")) as db:
                self.assertEqual(1, db.execute("SELECT lifetime_requests FROM clock").fetchone()[0])

    def test_capture_failure_blocks_network_and_http_error_keeps_body(self):
        from debug_capture import CaptureFailure
        with patch("vision_probe._read_key", return_value="fake-provider-key"), \
                patch("vision_probe.DebugCapture", side_effect=CaptureFailure("debug_capture_unavailable")), \
                patch("vision_probe.send_once") as send:
            self.assertEqual(2, vision.main(["--live", "--account-verified"]))
            send.assert_not_called()
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root) / "captures"), \
                patch("vision_probe.STATE", Path(root) / "state"), \
                patch("vision_probe._read_key", return_value="fake-provider-key"), \
                patch("vision_probe.send_once", return_value=(400, b'{"error":"image unsupported"}', False)) as send:
            self.assertEqual(2, vision.main(["--live", "--account-verified"]))
            send.assert_called_once()
            record = json.loads(next(Path(root, "captures").glob("*.json")).read_text())
            self.assertEqual('{"error":"image unsupported"}', record["response_text"])
            self.assertEqual(400, record["http_status"])
            self.assertIsNone(record["probe_result"]["usage"])


if __name__ == "__main__":
    unittest.main()
