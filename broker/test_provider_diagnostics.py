import contextlib
import io
import json
import os
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import Mock, patch

from contract import parse_request, synthetic_response
from demo import build_sample
from probe import DEFAULT_ENDPOINT, DEFAULT_MODEL, main
from provider import ProviderAdapter, ProviderConfig, ProviderFailure, parse_completion, request_body


class ProviderDiagnosticTests(unittest.TestCase):
    def setUp(self):
        self.request, _ = build_sample("weight", DEFAULT_MODEL, today=date(2026, 10, 27))
        self.secret = "raw-upstream-secret-and-payload"
        self.envelope = {
            "model": self.request["model"],
            "choices": [{"index": 0, "finish_reason": "stop", "message": {
                "role": "assistant", "content": json.dumps(synthetic_response(self.request))}}],
            "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
        }

    def assert_diagnostic(self, raw, diagnostic, public_code="invalid_provider_output"):
        with self.assertRaises(ProviderFailure) as caught:
            parse_completion(raw, self.request)
        self.assertEqual(public_code, str(caught.exception))
        self.assertEqual(diagnostic, caught.exception.diagnostic)
        self.assertNotIn(self.secret, str(caught.exception))
        self.assertNotIn(self.secret, repr(caught.exception.diagnostic))

    def test_default_matches_user_console_configuration(self):
        self.assertEqual("nvidia/Nemotron-3_5-Lightning", DEFAULT_MODEL)
        self.assertEqual("https://api.tokenfactory.nebius.com/v1/", DEFAULT_ENDPOINT)

    def test_thinking_is_disabled_only_for_lightning_without_changing_token_limit(self):
        _, preview = build_sample("weight", DEFAULT_MODEL, today=date(2026, 10, 27))
        body = json.loads(request_body(self.request, preview))
        self.assertEqual({"enable_thinking": False}, body["chat_template_kwargs"])
        self.assertEqual(512, body["max_tokens"])
        other_request, other_preview = build_sample("weight", "nvidia/fictional-test-model",
                                                    today=date(2026, 10, 27))
        other = json.loads(request_body(other_request, other_preview))
        self.assertNotIn("chat_template_kwargs", other)
        self.assertEqual(512, other["max_tokens"])

    def test_envelope_and_truncation_failures_are_separated(self):
        self.assert_diagnostic(self.secret.encode(), "response_not_json")
        self.assert_diagnostic(b"[]", "response_envelope_invalid")
        changed = dict(self.envelope, model="nvidia/other-alias")
        self.assert_diagnostic(json.dumps(changed).encode(), "response_model_mismatch")
        changed = dict(self.envelope, choices=[])
        self.assert_diagnostic(json.dumps(changed).encode(), "response_choices_invalid")
        changed = json.loads(json.dumps(self.envelope))
        changed["choices"][0]["finish_reason"] = "length"
        self.assert_diagnostic(json.dumps(changed).encode(), "response_truncated")

    def test_selection_failures_identify_json_schema_and_identity(self):
        changed = json.loads(json.dumps(self.envelope))
        changed["choices"][0]["message"]["content"] = self.secret
        self.assert_diagnostic(json.dumps(changed).encode(), "response_content_json_invalid")
        changed["choices"][0]["message"]["content"] = json.dumps({"unexpected": self.secret})
        self.assert_diagnostic(json.dumps(changed).encode(), "response_selection_schema_invalid")
        selection = synthetic_response(self.request)
        selection["request_id"] = self.secret
        changed["choices"][0]["message"]["content"] = json.dumps(selection)
        self.assert_diagnostic(json.dumps(changed).encode(), "response_selection_identity_mismatch")

    def test_missing_content_and_usage_limit_have_distinct_diagnostics(self):
        changed = json.loads(json.dumps(self.envelope))
        changed["choices"][0]["message"]["content"] = None
        self.assert_diagnostic(json.dumps(changed).encode(), "response_content_missing")
        changed = json.loads(json.dumps(self.envelope))
        changed["usage"]["completion_tokens"] = 513
        self.assert_diagnostic(json.dumps(changed).encode(), "response_usage_limit", "provider_usage_limit")

    def test_transport_content_type_and_length_mismatch_are_classified(self):
        _, preview = build_sample("weight", DEFAULT_MODEL, today=date(2026, 10, 27))
        request = parse_request(preview, date(2026, 10, 27), frozenset({DEFAULT_MODEL}))
        config = ProviderConfig(DEFAULT_ENDPOINT, DEFAULT_MODEL, "fictional-key")

        def call_with(response):
            connection = Mock()
            connection.getresponse.return_value = response
            with patch("provider.http.client.HTTPSConnection", return_value=connection):
                with self.assertRaises(ProviderFailure) as caught:
                    ProviderAdapter(config, today=lambda: date(2026, 10, 27))(request, preview)
            return caught.exception

        response = Mock(status=200)
        response.getheader.side_effect = lambda name, default=None: "text/plain" if name == "Content-Type" else default
        failure = call_with(response)
        self.assertEqual("invalid_provider_output", str(failure))
        self.assertEqual("response_content_type_header_invalid", failure.diagnostic)

        raw = json.dumps(self.envelope).encode()
        response = Mock(status=200)
        response.getheader.side_effect = lambda name, default=None: (
            "application/json" if name == "Content-Type" else str(len(raw) + 1))
        response.read1.side_effect = [raw, b""]
        failure = call_with(response)
        self.assertEqual("invalid_provider_output", str(failure))
        self.assertEqual("response_content_length_mismatch", failure.diagnostic)

    def test_probe_emits_only_fixed_diagnostic_with_generic_error(self):
        def fail(*_args, **_kwargs):
            raise ProviderFailure("invalid_provider_output", "response_truncated")

        class MemoryStore:
            def __init__(self, *_args, **_kwargs):
                pass

            def provision(self):
                return "fictional-bearer"

            def admit(self, *_args):
                pass

            def dispatch(self, *_args):
                pass

            def finish(self, *_args):
                pass

            def revoke(self, *_args):
                pass

        with patch.dict(os.environ, {
                "NEBIUS_API_KEY": self.secret,
                "NEBIUS_MODEL": DEFAULT_MODEL,
                "NEBIUS_BASE_URL": DEFAULT_ENDPOINT,
        }, clear=True), patch("probe.ProviderAdapter", side_effect=fail), \
                patch("probe.private_directory", side_effect=lambda path: Path(path)), \
                patch("probe.Store", MemoryStore):
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                code = main(["--live", "--account-verified", "--state-dir", ".diagnostic-test-state"])
        result = json.loads(out.getvalue())
        self.assertEqual(2, code)
        self.assertEqual("invalid_provider_output", result["error"])
        self.assertEqual("response_truncated", result["diagnostic"])
        self.assertNotIn(self.secret, out.getvalue())

    def test_unapproved_diagnostic_is_dropped_and_string_stays_generic(self):
        failure = ProviderFailure("invalid_provider_output", self.secret)
        self.assertEqual("invalid_provider_output", str(failure))
        self.assertIsNone(failure.diagnostic)
        self.assertNotIn(self.secret, repr(failure.diagnostic))


if __name__ == "__main__":
    unittest.main()
