"""Only invented response bodies; no external API, image or credential access."""
import base64
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from debug_capture import CaptureFailure
from fictional_app_capture import FictionalAppCapture
from provider import ProviderFailure
from cloud_vision import SuperVLVisionAdapter, parse_vision_request
from test_cloud_vision import FakeConnection, completion, request_value
from cloud_assistant import SuperVLAssistantAdapter, parse_assistant_request
from test_cloud_assistant import FakeConnection as AssistantConnection, envelope, request_value as assistant_request


class FictionalAppCaptureTests(unittest.TestCase):
    def test_chat_success_keeps_complete_provider_envelope(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            raw = envelope(json.dumps({"reply": "Fictional chat response", "tool": None}))
            AssistantConnection.status_body = raw
            with patch("cloud_assistant.http.client.HTTPSConnection", AssistantConnection), contextlib.redirect_stdout(io.StringIO()):
                result = FictionalAppCapture("test-secret").wrap(SuperVLAssistantAdapter({"NEBIUS_API_KEY": "test-secret"}), "assistant")(
                    parse_assistant_request(json.dumps(assistant_request()).encode()))
            row = json.loads(next(Path(root).glob("*.json")).read_text())
            self.assertEqual("Fictional chat response", result["reply"])
            self.assertEqual(raw[1], base64.b64decode(row["response_body_base64"]))
            self.assertEqual("validated", row["outcome"])

    def test_http_error_retained_before_validation_and_secret_redacted(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            raw = b'{"error":{"message":"fictional provider error","echo":"test-secret"}}'
            FakeConnection.status_body = (400, raw)
            output = io.StringIO()
            with patch("cloud_vision.http.client.HTTPSConnection", FakeConnection), contextlib.redirect_stdout(output), self.assertRaises(ProviderFailure):
                FictionalAppCapture("test-secret").wrap(SuperVLVisionAdapter({"NEBIUS_API_KEY": "test-secret"}), "vision")(
                    parse_vision_request(json.dumps(request_value()).encode()))
            path, = Path(root).glob("*.json")
            text = path.read_text()
            row = json.loads(text)
            self.assertEqual(400, row["http_status"])
            self.assertIn("fictional provider error", row["response_text"])
            self.assertEqual(row["response_text"].encode(), base64.b64decode(row["response_body_base64"]))
            self.assertEqual("provider_request_rejected", row["diagnostic"])
            self.assertTrue(row["probe_result"]["errors"])
            self.assertNotIn("test-secret", text + output.getvalue())
            self.assertNotIn("fictional provider error", output.getvalue())

    def test_malformed_success_body_survives_schema_rejection(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            FakeConnection.status_body = completion(content="fictional malformed draft")
            with patch("cloud_vision.http.client.HTTPSConnection", FakeConnection), contextlib.redirect_stdout(io.StringIO()), self.assertRaises(ProviderFailure):
                FictionalAppCapture("test-secret").wrap(SuperVLVisionAdapter({"NEBIUS_API_KEY": "test-secret"}), "vision")(
                    parse_vision_request(json.dumps(request_value()).encode()))
            row = json.loads(next(Path(root).glob("*.json")).read_text())
            self.assertEqual("rejected", row["outcome"])
            self.assertFalse(row["truncated"])
            self.assertIn("fictional malformed draft", row["response_text"])
            self.assertEqual(2636, row["probe_result"]["usage"]["total_tokens"])

    def test_network_exception_retains_original_details_without_key(self):
        with tempfile.TemporaryDirectory() as root, patch("debug_capture.CAPTURE_DIR", Path(root)):
            def failing(request, *, on_response, on_error):
                try:
                    raise OSError("fictional TLS problem; test-secret")
                except OSError as error:
                    on_error(error)
                raise ProviderFailure("provider_unavailable", "provider_network_error")
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(ProviderFailure):
                FictionalAppCapture("test-secret").wrap(failing, "assistant")({"request_id": "synthetic-id"})
            text = next(Path(root).glob("*.json")).read_text()
            row = json.loads(text)
            self.assertIsNone(row["http_status"])
            self.assertEqual("provider_network_error", row["diagnostic"])
            self.assertEqual("OSError", row["probe_result"]["errors"][0]["type"])
            self.assertNotIn("test-secret", text)

    def test_capture_unavailable_blocks_provider_call(self):
        calls = []
        with patch("fictional_app_capture.DebugCapture", side_effect=CaptureFailure("debug_capture_unavailable")), self.assertRaises(CaptureFailure):
            FictionalAppCapture("test-secret").wrap(lambda *a, **kw: calls.append(1), "vision")({"request_id": "synthetic-id"})
        self.assertEqual([], calls)

    def test_normal_adapter_does_not_create_capture(self):
        FakeConnection.status_body = completion()
        with patch("fictional_app_capture.DebugCapture") as capture, patch("cloud_vision.http.client.HTTPSConnection", FakeConnection):
            SuperVLVisionAdapter({"NEBIUS_API_KEY": "test-secret"})(
                parse_vision_request(json.dumps(request_value()).encode()))
        capture.assert_not_called()
