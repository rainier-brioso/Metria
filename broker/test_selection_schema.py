import json
import unittest
from datetime import date
from unittest.mock import Mock, patch

from demo import SCENARIOS, build_sample
from provider import ProviderAdapter, ProviderConfig, ProviderFailure, request_body, validate_selection


LIGHTNING = "nvidia/Nemotron-3_5-Lightning"
TODAY = date(2026, 10, 27)


class LightningSelectionSchemaTests(unittest.TestCase):
    def test_schema_bounds_categories_and_identity_for_every_demo_scenario(self):
        for scenario in SCENARIOS:
            with self.subTest(scenario=scenario):
                request, preview = build_sample(scenario, LIGHTNING, today=TODAY)
                body = json.loads(request_body(request, preview))
                self.assertEqual(preview.decode("utf-8"), body["messages"][1]["content"])
                self.assertEqual(512, body["max_tokens"])
                self.assertEqual({"enable_thinking": False}, body["chat_template_kwargs"])

                response_format = body["response_format"]
                self.assertEqual("json_schema", response_format["type"])
                self.assertEqual("metria_selection", response_format["json_schema"]["name"])
                self.assertTrue(response_format["json_schema"]["strict"])
                schema = response_format["json_schema"]["schema"]
                self.assertEqual("object", schema["type"])
                self.assertFalse(schema["additionalProperties"])
                self.assertEqual(["schema_version", "request_id", "language", "sections", "guidance"],
                                 schema["required"])
                properties = schema["properties"]
                self.assertEqual([1], properties["schema_version"]["enum"])
                self.assertEqual([request["request_id"]], properties["request_id"]["enum"])
                self.assertEqual([request["language"]], properties["language"]["enum"])
                allowed = [key for key in ("weight", "nutrition") if key in request]
                self.assertEqual(len(allowed), properties["sections"]["maxItems"])
                self.assertEqual(allowed or ["weight", "nutrition"],
                                 properties["sections"]["items"]["enum"])
                self.assertEqual(response_format["json_schema"]["schema"],
                                 json.loads(body["messages"][0]["content"].split(
                                     "Follow this exact JSON schema: ", 1)[1]))

    def test_other_models_keep_their_existing_request_shape(self):
        request, preview = build_sample("nutrition", "nvidia/fictional-test-model", today=TODAY)
        body = json.loads(request_body(request, preview))
        self.assertEqual({"model", "stream", "max_tokens", "messages"}, set(body))

    def test_existing_validator_rejects_invalid_section_shapes_and_accepts_partial_selection(self):
        request, _ = build_sample("nutrition", LIGHTNING, today=TODAY)
        base = {"schema_version": 1, "request_id": request["request_id"], "language": request["language"],
                "sections": [], "guidance": "recorded_context_only"}
        self.assertEqual([], validate_selection(json.dumps(base), request)["sections"])
        for invalid in ([{"nutrition": True}], [1], ["weight"], ["nutrition", "nutrition"]):
            with self.subTest(sections=invalid):
                selection = dict(base, sections=invalid)
                with self.assertRaises(ProviderFailure) as caught:
                    validate_selection(json.dumps(selection), request)
                self.assertEqual("response_selection_sections_invalid", caught.exception.diagnostic)

    def test_http_status_diagnostics_are_classified_without_reading_error_body(self):
        request, preview = build_sample("weight", LIGHTNING, today=TODAY)
        config = ProviderConfig("http://127.0.0.1:1/v1", LIGHTNING, "fictional-key", _fake=True)
        cases = {400: "provider_request_rejected", 422: "provider_request_rejected",
                 401: "provider_authentication_rejected", 403: "provider_authentication_rejected",
                 429: "provider_rate_limited", 503: "provider_upstream_error", 302: "provider_http_rejected"}
        for status, diagnostic in cases.items():
            with self.subTest(status=status):
                connection = Mock()
                response = Mock(status=status)
                connection.getresponse.return_value = response
                with patch("provider.http.client.HTTPConnection", return_value=connection):
                    with self.assertRaises(ProviderFailure) as caught:
                        ProviderAdapter(config, today=lambda: TODAY)(request, preview)
                self.assertEqual("provider_rejected", str(caught.exception))
                self.assertEqual(diagnostic, caught.exception.diagnostic)
                response.read1.assert_not_called()


if __name__ == "__main__":
    unittest.main()
