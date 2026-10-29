import base64
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from debug_capture import (CAPTURE_DIR, MAX_CAPTURE_BYTES, MAX_CAPTURE_FILES, CaptureFailure,
                          DebugCapture, redact_body)
from demo import build_sample, expected_selection
from probe import DEFAULT_ENDPOINT, DEFAULT_MODEL, main
from provider import ProviderAdapter, ProviderConfig, ProviderFailure


class DebugCaptureTests(unittest.TestCase):
    def test_redacts_plain_json_escaped_and_mixed_unicode_escaped_secrets(self):
        secret = 'ab"\\cd'
        cases = [secret.encode(), json.dumps({secret: "value", "echo": secret}).encode(),
                 b'{"echo":"ab\\u0022\\\\cd"}',
                 b'{"caf\xc3\xa9":"ab\\u0022\\\\cd"}',
                 b'malformed "echo":"' + secret.encode()]
        for raw in cases:
            with self.subTest(raw=raw):
                cleaned = redact_body(raw, secret)
                self.assertNotIn(secret.encode(), cleaned)
                self.assertNotIn(b"\\u0022\\\\", cleaned)

    def test_capture_is_bounded_exclusive_and_keeps_safe_metadata(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root) / "captures"):
            capture = DebugCapture("weight", DEFAULT_MODEL, "request-123", "fictional-key")
            capture.on_response(b'{"echo":"fictional-key","ok":true}', 200)
            capture.finish("rejected", "response_selection_sections_invalid", 12)
            path, = Path(root, "captures").glob("*.json")
            record = json.loads(path.read_text())
            self.assertEqual("request-123", record["request_id"])
            self.assertEqual("weight", record["scenario"])
            self.assertEqual(200, record["http_status"])
            self.assertFalse(record["truncated"])
            self.assertEqual("rejected", record["outcome"])
            self.assertNotIn("fictional-key", path.read_text())
            self.assertEqual(b'{"echo":"[REDACTED]","ok":true}',
                             base64.b64decode(record["response_body_base64"]))
            with self.assertRaises(FileExistsError):
                os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)

    def test_valid_capture_records_validated_outcome(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            capture = DebugCapture("weight", DEFAULT_MODEL, "req-valid", "fictional-key")
            capture.on_response(b'{"ok":true}', 200)
            capture.finish("validated", latency_ms=7)
            path, = Path(root).glob("*.json")
            record = json.loads(path.read_text())
            self.assertEqual("validated", record["outcome"])
            self.assertEqual(7, record["latency_ms"])
            self.assertEqual(200, record["http_status"])

    def test_cap_truncation_and_artifact_quota(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            capture = DebugCapture("weight", DEFAULT_MODEL, "id", "key")
            capture.on_response(b"x" * MAX_CAPTURE_BYTES, 200, True)
            capture.finish("provider_error")
            path, = Path(root).glob("*.json")
            record = json.loads(path.read_text())
            self.assertTrue(record["truncated"])
            self.assertLessEqual(len(base64.b64decode(record["response_body_base64"])), MAX_CAPTURE_BYTES)
            for i in range(MAX_CAPTURE_FILES - 1):
                (Path(root) / f"old-{i}.json").write_text("{}")
            with self.assertRaises(CaptureFailure) as caught:
                DebugCapture("weight", DEFAULT_MODEL, "other", "key")
            self.assertEqual("debug_capture_limit_reached", str(caught.exception))

    def test_probe_debug_flag_requires_live_and_preflight_failure_prevents_dispatch(self):
        with self.assertRaises(SystemExit):
            main(["--check", "--debug-response"])
        request, _ = build_sample("weight", DEFAULT_MODEL)
        class Store:
            def __init__(self, *_args, **_kwargs): pass
            def provision(self): return "fake"
            def admit(self, *_args): pass
            def dispatch(self, *_args): pass
            def finish(self, *_args): pass
            def revoke(self, *_args): pass
        called = []
        with patch.dict(os.environ, {"NEBIUS_API_KEY": "fictional-key", "NEBIUS_MODEL": DEFAULT_MODEL,
                                     "NEBIUS_BASE_URL": DEFAULT_ENDPOINT}, clear=True), \
             patch("probe.DebugCapture", side_effect=CaptureFailure("debug_capture_unavailable")), \
             patch("probe.private_directory", side_effect=lambda path: Path(path)), \
             patch("probe.Store", Store), patch("probe.ProviderAdapter", side_effect=lambda *a, **k: called.append(1)):
            code = main(["--live", "--account-verified", "--debug-response"])
        self.assertEqual(2, code)
        self.assertEqual([], called)

    def test_disabled_probe_does_not_supply_response_callback(self):
        request, _ = build_sample("weight", DEFAULT_MODEL)
        class Store:
            def __init__(self, *_args, **_kwargs): pass
            def provision(self): return "fake"
            def admit(self, *_args): pass
            def dispatch(self, *_args): pass
            def authorize_result(self, *_args): pass
            def finish(self, *_args): pass
            def revoke(self, *_args): pass
        class Adapter:
            def __init__(self, *_args, **_kwargs): pass
            def __call__(self, req, preview, on_usage=None):
                return expected_selection(req, "weight")
        with tempfile.TemporaryDirectory() as root, patch.dict(os.environ, {
                "NEBIUS_API_KEY": "fictional-key", "NEBIUS_MODEL": DEFAULT_MODEL,
                "NEBIUS_BASE_URL": DEFAULT_ENDPOINT}, clear=True), \
             patch("probe.private_directory", side_effect=lambda path: Path(path)), \
             patch("probe.Store", Store), patch("probe.ProviderAdapter", Adapter), \
             patch("debug_capture.CAPTURE_DIR", Path(root) / "should-not-exist"):
            code = main(["--live", "--account-verified", "--state-dir", str(Path(root) / "state")])
        self.assertEqual(0, code)
        self.assertFalse((Path(root) / "should-not-exist").exists())

    def test_probe_retains_invalid_model_response_and_sanitizes_console(self):
        class Store:
            def __init__(self, *_args, **_kwargs): pass
            def provision(self): return "fake"
            def admit(self, *_args): pass
            def dispatch(self, *_args): pass
            def finish(self, *_args): pass
            def revoke(self, *_args): pass
        raw = b'{"choices":[{"message":{"content":"fictional-model-reasoning"}}]}'
        class Adapter:
            def __init__(self, *_args, **_kwargs): pass
            def __call__(self, *_args, on_response=None, **_kwargs):
                on_response(raw, 200, False)
                raise ProviderFailure("invalid_provider_output", "response_selection_sections_invalid")
        with tempfile.TemporaryDirectory() as root, patch.dict(os.environ, {
                "NEBIUS_API_KEY": "fictional-key", "NEBIUS_MODEL": DEFAULT_MODEL,
                "NEBIUS_BASE_URL": DEFAULT_ENDPOINT}, clear=True), \
             patch("probe.private_directory", side_effect=lambda path: Path(path)), \
             patch("probe.Store", Store), patch("probe.ProviderAdapter", Adapter), \
             patch("debug_capture.CAPTURE_DIR", Path(root) / "captures"):
            import contextlib
            import io
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = main(["--live", "--account-verified", "--debug-response",
                             "--state-dir", str(Path(root) / "state")])
            self.assertEqual(2, code)
            self.assertNotIn("fictional-model-reasoning", output.getvalue())
            record_path, = (Path(root) / "captures").glob("*.json")
            record = json.loads(record_path.read_text())
            self.assertEqual("response_selection_sections_invalid", record["diagnostic"])
            self.assertEqual(raw.decode(), record["response_text"])
            self.assertEqual("rejected", record["outcome"])
            self.assertEqual(str(record_path), json.loads(output.getvalue())["debug_file"])

    def test_default_non_200_does_not_read_body_but_capture_does(self):
        from datetime import date
        request, preview = build_sample("weight", DEFAULT_MODEL, today=date(2026, 10, 27))
        for callback in (None, lambda *_args: None):
            response = Mock(status=401)
            response.read1.side_effect = [b"body", b""]
            connection = Mock()
            connection.getresponse.return_value = response
            with patch("provider.http.client.HTTPSConnection", return_value=connection):
                try:
                    ProviderAdapter(ProviderConfig(DEFAULT_ENDPOINT, DEFAULT_MODEL, "fictional-key"),
                                    today=lambda: date(2026, 10, 27))(
                        request, preview, **({} if callback is None else {"on_response": callback}))
                except ProviderFailure:
                    pass
            self.assertEqual(2 if callback else 0, response.read1.call_count)


if __name__ == "__main__":
    unittest.main()
