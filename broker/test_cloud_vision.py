"""Authenticated optional image endpoint; only synthetic JPEG bytes and fake providers."""
import base64
import contextlib
import io
import json
import tempfile
import threading
import unittest
from datetime import datetime, timezone
from http.client import HTTPConnection
from pathlib import Path
from unittest.mock import patch
from uuid import uuid4

from cloud_vision import (ENDPOINT, MAX_IMAGE_BYTES, MODEL, SuperVLVisionAdapter,
                          parse_vision_request, validate_proposal)
from hosted import arguments
from provider import ProviderFailure
from server import LocalBroker
from store import Store

IMAGE = (b"\xff\xd8" + b"\xff\xc0\x00\x11\x08\x00\x01\x00\x01\x03"
         b"\x01\x11\x00\x02\x11\x00\x03\x11\x00"
         b"\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00\x00\xff\xd9")
PROPOSAL = {"kind": "meal_proposal", "items": [{"name": "Rice", "description": "white rice",
    "visible_portion": "small mound", "estimated_grams_range": {"min": 100, "max": 200},
    "preparation": "cooked", "confidence": "medium", "evidence": "seen", "role": "side"}],
    "assumptions": [], "questions": ["Was oil added?"], "overall_confidence": "medium"}
USAGE = {"prompt_tokens": 1761, "completion_tokens": 875, "total_tokens": 2636,
         "prompt_tokens_details": {"cached_tokens": 0, "multimodal_tokens": {"image": 1602}},
         "completion_tokens_details": {"reasoning_tokens": 658}}


def request_value(**changes):
    value = {"request_id": str(uuid4()), "image_base64": base64.b64encode(IMAGE).decode(),
             "description": "Please help me estimate this meal.", "consent_version": 1}
    value.update(changes)
    return value


def completion(status=200, content=None, usage=None, model=MODEL, finish="stop"):
    return status, json.dumps({"model": model, "choices": [{"index": 0, "finish_reason": finish,
        "message": {"role": "assistant", "content": json.dumps(PROPOSAL) if content is None else content}}],
        "usage": USAGE if usage is None else usage}).encode()


class VisionContractTests(unittest.TestCase):
    def test_image_and_exact_request_contract(self):
        parsed = parse_vision_request(json.dumps(request_value()).encode())
        self.assertEqual(IMAGE, parsed["image"])
        for raw in (b'{"request_id":"x","request_id":"y"}',
                    json.dumps(request_value(consent_version=True)).encode(),
                    json.dumps(request_value(extra="no")).encode(),
                    json.dumps(request_value(request_id="not-a-uuid")).encode(),
                    json.dumps(request_value(image_base64=base64.b64encode(b"not jpeg").decode())).encode(),
                    json.dumps(request_value(image_base64="!!!")).encode(),
                    json.dumps(request_value(description="x" * 601)).encode()):
            with self.subTest(raw=raw[:60]), self.assertRaises(ValueError):
                parse_vision_request(raw)

    def test_image_byte_limit_is_checked_after_base64_decode(self):
        big = b"\xff\xd8\xff" + b"x" * (MAX_IMAGE_BYTES - 5) + b"\xff\xd9"
        self.assertEqual(len(big), MAX_IMAGE_BYTES)
        oversized = b"\xff\xd8\xff" + b"x" * (MAX_IMAGE_BYTES - 4) + b"\xff\xd9"
        with self.assertRaises(ValueError):
            parse_vision_request(json.dumps(request_value(image_base64=base64.b64encode(oversized).decode())).encode())

    def test_jpeg_dimensions_are_capped_to_prepared_photo_size(self):
        too_wide = bytearray(IMAGE)
        too_wide[9:11] = (1537).to_bytes(2, "big")
        with self.assertRaises(ValueError):
            parse_vision_request(json.dumps(request_value(image_base64=base64.b64encode(too_wide).decode())).encode())

    def test_native_meal_proposal_schema_is_strict_and_bounded(self):
        self.assertEqual(json.dumps(PROPOSAL, ensure_ascii=False, separators=(",", ":")),
                         validate_proposal(json.dumps(PROPOSAL)))
        variants = []
        variants.append({**PROPOSAL, "calories": 1})
        variants.append({**PROPOSAL, "items": [{**PROPOSAL["items"][0], "estimated_grams_range": {"min": True, "max": 5}}]})
        variants.append({**PROPOSAL, "items": [{**PROPOSAL["items"][0], "role": "medical"}]})
        variants.append({**PROPOSAL, "items": [{**PROPOSAL["items"][0], "confidence": "high", "evidence": "possible_inside"}]})
        variants.append({**PROPOSAL, "items": PROPOSAL["items"] * 9})
        for value in variants:
            with self.subTest(value=value), self.assertRaises(ProviderFailure):
                validate_proposal(json.dumps(value))
        with self.assertRaises(ProviderFailure):
            validate_proposal('{"kind":"meal_proposal","kind":"x"}')

    def test_environment_is_fixed_to_an_allowed_https_host_and_server_key(self):
        adapter = SuperVLVisionAdapter({"NEBIUS_API_KEY": "test-key"})
        self.assertEqual("api.tokenfactory.nebius.com", adapter.host)
        for env in ({"NEBIUS_API_KEY": ""}, {"NEBIUS_API_KEY": "has space"},
                    {"NEBIUS_API_KEY": "k", "NEBIUS_BASE_URL": "https://evil.example/v1/"}):
            with self.subTest(env=env), self.assertRaises(ValueError):
                SuperVLVisionAdapter(env)


class FakeResponse:
    def __init__(self, status, body):
        self.status, self.body = status, body
        self.headers = {"Content-Type": "application/json"}
    def read1(self, size):
        value, self.body = self.body[:size], self.body[size:]
        return value
    def getheader(self, name, default=""):
        return self.headers.get(name, default)


class FakeConnection:
    instances = []
    status_body = None
    def __init__(self, *args, **kwargs):
        self.sock = None
        self.auto_open = 1
        self.calls = []
        self.__class__.instances.append(self)
    def connect(self): pass
    def request(self, *args): self.calls.append(args)
    def getresponse(self): return FakeResponse(*self.status_body)
    def close(self): pass


class VisionProviderTests(unittest.TestCase):
    def setUp(self):
        FakeConnection.instances.clear()
        FakeConnection.status_body = None

    def test_one_openai_compatible_multimodal_request_and_content_free_usage(self):
        FakeConnection.status_body = completion()
        request = parse_vision_request(json.dumps(request_value()).encode())
        with patch("cloud_vision.http.client.HTTPSConnection", FakeConnection):
            result = SuperVLVisionAdapter({"NEBIUS_API_KEY": "server-secret"})(request)
        self.assertEqual(1, len(FakeConnection.instances))
        method, path, body, headers = FakeConnection.instances[0].calls[0]
        payload = json.loads(body)
        self.assertEqual(("POST", "/v1/chat/completions"), (method, path))
        self.assertEqual("Bearer server-secret", headers["Authorization"])
        self.assertEqual(MODEL, payload["model"])
        self.assertEqual(4096, payload["max_tokens"])
        self.assertEqual("json_schema", payload["response_format"]["type"])
        schema = payload["response_format"]["json_schema"]["schema"]
        self.assertEqual(set(PROPOSAL), set(schema["required"]))
        self.assertFalse(schema["additionalProperties"])
        self.assertIn("image/jpeg;base64,", payload["messages"][1]["content"][1]["image_url"]["url"])
        self.assertEqual({"prompt_tokens": 1761, "completion_tokens": 875, "total_tokens": 2636}, result["usage"])
        self.assertNotIn("reasoning", json.dumps(result))
        self.assertEqual(request["request_id"], result["request_id"])
        self.assertEqual(ENDPOINT, ENDPOINT)

    def test_rejected_response_retains_only_usage_and_fixed_diagnostic(self):
        request = parse_vision_request(json.dumps(request_value()).encode())
        for result, reason in ((completion(finish="length"), "response_truncated"),
                               (completion(content="private non-json output"), "response_content_json_invalid"),
                               (completion(content=json.dumps({**PROPOSAL, "save": True})), "response_selection_schema_invalid"),
                               (completion(status=401), "provider_authentication_rejected")):
            FakeConnection.status_body = result
            with patch("cloud_vision.http.client.HTTPSConnection", FakeConnection), self.assertRaises(ProviderFailure) as caught:
                SuperVLVisionAdapter({"NEBIUS_API_KEY": "server-secret"})(request)
            self.assertEqual(reason, caught.exception.diagnostic)
            self.assertNotIn("private", str(caught.exception))
            if result[0] == 200:
                self.assertEqual({"prompt_tokens": 1761, "completion_tokens": 875, "total_tokens": 2636},
                                 caught.exception.usage)
            else:
                self.assertIsNone(caught.exception.usage)

    def test_upstream_errors_bad_shape_and_missing_usage_fail_closed(self):
        request = parse_vision_request(json.dumps(request_value()).encode())
        cases = [completion(status=401), (200, b"[]"), (200, b'{"choices":[{}]}'), completion(content="not json"), completion(model="other"),
                 completion(finish="length"), completion(usage={}),
                 completion(content=json.dumps({**PROPOSAL, "save": True}))]
        for status_body in cases:
            FakeConnection.status_body = status_body
            with patch("cloud_vision.http.client.HTTPSConnection", FakeConnection), self.subTest(status_body=status_body[0]), self.assertRaises(ProviderFailure):
                SuperVLVisionAdapter({"NEBIUS_API_KEY": "server-secret"})(request)


class VisionHttpTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.now = datetime(2026, 10, 27, tzinfo=timezone.utc).timestamp()
        self.store = Store(Path(self.temp.name) / "state.sqlite", clock=lambda: self.now)
        self.code = self.store.issue_pass(self.now + 86400 * 10, 2)
        self.adapter = lambda request: {"request_id": request["request_id"], "model": MODEL,
            "provider": "nebius", "proposal": json.dumps(PROPOSAL),
            "usage": {"prompt_tokens": 100, "completion_tokens": 20, "total_tokens": 120}}
        self.server = LocalBroker(0, self.store, vision_responder=self.adapter, allow_cloud_photos=True)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.token = self.send("/v1/judge/activate", {"pass_code": self.code,
            "installation_id": str(uuid4())})[2]["credential"]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(2)
        self.temp.cleanup()

    def send(self, path, value=None, token=None, raw=None, headers=None):
        connection = HTTPConnection("127.0.0.1", self.server.server_port, timeout=4)
        try:
            fields = {"Content-Type": "application/json", "X-Metria-Sent-At": str(int(self.now))}
            if token:
                fields["Authorization"] = "Bearer " + token
            fields.update(headers or {})
            connection.request("POST", path, raw if raw is not None else json.dumps(value).encode(), fields)
            response = connection.getresponse()
            return response.status, dict(response.getheaders()), json.loads(response.read())
        finally:
            connection.close()

    def test_activation_discloses_mode_and_authenticated_photo_returns_validated_result(self):
        extra_pass = self.store.issue_pass(self.now + 86400, 2)
        status, headers, body = self.send("/v1/judge/activate", {"pass_code": extra_pass,
            "installation_id": str(uuid4())})
        self.assertEqual(200, status)
        self.assertFalse(body["fictional_only"])
        status, headers, result = self.send("/v1/vision", request_value(), self.token)
        self.assertEqual(200, status)
        self.assertEqual("no-store", headers["Cache-Control"])
        self.assertEqual(MODEL, result["model"])
        self.assertEqual("nebius", result["provider"])
        self.assertEqual(120, result["usage"]["total_tokens"])

    def test_requires_activation_header_valid_body_and_explicit_enable(self):
        self.assertEqual(403, self.send("/v1/vision", request_value(), token="invalid")[0])
        self.assertEqual(400, self.send("/v1/vision", request_value(), self.token,
                                       headers={"Origin": "https://attacker.invalid"})[0])
        self.assertEqual(400, self.send("/v1/vision", {}, self.token)[0])
        disabled = LocalBroker(0, self.store)
        thread = threading.Thread(target=disabled.serve_forever, daemon=True)
        thread.start()
        try:
            conn = HTTPConnection("127.0.0.1", disabled.server_port, timeout=3)
            conn.request("POST", "/v1/vision", json.dumps(request_value()).encode(),
                         {"Content-Type": "application/json", "Authorization": "Bearer " + self.token,
                          "X-Metria-Sent-At": str(int(self.now))})
            self.assertEqual(400, conn.getresponse().status)
            conn.close()
        finally:
            disabled.shutdown()
            disabled.server_close()
            thread.join(2)

    def test_rate_quota_denials_and_provider_failure_do_not_return_or_log_photo(self):
        code = self.store.issue_pass(self.now + 3600, 1)
        token = self.send("/v1/judge/activate", {"pass_code": code,
            "installation_id": str(uuid4())})[2]["credential"]
        self.assertEqual(200, self.send("/v1/vision", request_value(), token)[0])
        self.assertEqual(403, self.send("/v1/vision", request_value(), token)[0])
        self.server.vision_responder = lambda request: (_ for _ in ()).throw(ProviderFailure("provider_unavailable"))
        code = self.store.issue_pass(self.now + 3600, 1)
        token = self.send("/v1/judge/activate", {"pass_code": code,
            "installation_id": str(uuid4())})[2]["credential"]
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            status, _, body = self.send("/v1/vision", request_value(), token)
        self.assertEqual(502, status)
        self.assertEqual({"error": "provider_response_unavailable"}, body)
        self.assertNotIn(base64.b64encode(IMAGE).decode(), output.getvalue())

    def test_hosted_setting_requires_verified_live_mode(self):
        base = {"METRIA_PUBLIC_HOST": "demo.example.org", "METRIA_PERSISTENT_STATE_CONFIRMED": "yes"}
        with self.assertRaises(ValueError):
            arguments(base | {"METRIA_ALLOW_CLOUD_PHOTOS": "yes"})
        args = arguments(base | {"METRIA_LIVE": "1", "METRIA_ACCOUNT_VERIFIED": "yes",
                                "METRIA_ALLOW_CLOUD_PHOTOS": "yes"})
        self.assertIn("--allow-cloud-assistant", args)
        self.assertNotIn("--allow-cloud-photos", arguments(base))


if __name__ == "__main__":
    unittest.main()
