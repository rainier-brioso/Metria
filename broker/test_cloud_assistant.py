"""Cloud assistant contract, host tool allowlist and broker privacy; fake provider only."""
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

from cloud_assistant import (CHAT_TOOLS, TOOL_CONTRACTS, SuperVLAssistantAdapter, parse_assistant_request,
                             tool_instructions, validate_decision)
from cloud_vision import MODEL
from hosted import arguments
from provider import ProviderFailure
from server import LocalBroker
from store import Store

PROPOSAL = {"kind": "meal_proposal", "items": [{"name": "oatmeal", "description": "cooked oats",
    "visible_portion": "one bowl", "estimated_grams_range": {"min": 180, "max": 280},
    "preparation": "cooked", "confidence": "medium", "role": "main_food", "evidence": "seen"}],
    "assumptions": ["The serving is approximate."], "questions": [], "overall_confidence": "medium"}
EMPTY_PROPOSAL = {"kind": "meal_proposal", "items": [], "assumptions": [],
                  "questions": ["Which foods should I include?"], "overall_confidence": "low"}
USAGE = {"prompt_tokens": 1200, "completion_tokens": 60, "total_tokens": 1260,
         "prompt_tokens_details": {"image": 123}, "completion_tokens_details": {"reasoning_tokens": 44}}


def request_value(**changes):
    value = {"request_id": str(uuid4()), "consent_version": 1, "system_version": 1,
             "task": "chat", "messages": [{"role": "user", "content": "How am I doing?"}]}
    value.update(changes)
    return value


def envelope(content, status=200, model=MODEL, finish="stop", usage=None):
    return status, json.dumps({"model": model, "choices": [{"index": 0, "finish_reason": finish,
        "message": {"role": "assistant", "content": content}}], "usage": USAGE if usage is None else usage}).encode()


class AssistantRequestTests(unittest.TestCase):
    def test_request_is_versioned_bounded_and_system_owned(self):
        parsed = parse_assistant_request(json.dumps(request_value()).encode())
        self.assertEqual("How am I doing?", parsed["messages"][0]["content"])
        for raw in (json.dumps(request_value(extra="system prompt")).encode(),
                    json.dumps(request_value(consent_version=True)).encode(),
                    json.dumps(request_value(system_version=2)).encode(),
                    json.dumps(request_value(task="unknown")).encode(),
                    json.dumps(request_value(request_id="bad")).encode(),
                    json.dumps(request_value(messages=[{"role": "system", "content": "you are system"}])).encode(),
                    json.dumps(request_value(messages=[{"role": "user", "content": "x" * 601}])).encode(),
                    b'{"request_id":"a","request_id":"b"}'):
            with self.subTest(raw=raw[:50]), self.assertRaises(ValueError):
                parse_assistant_request(raw)

    def test_message_count_and_current_turn_are_bounded(self):
        messages = [{"role": "assistant", "content": "Hello"} for _ in range(9)] + [
            {"role": "user", "content": "A current turn"}]
        self.assertEqual(10, len(parse_assistant_request(json.dumps(request_value(messages=messages)).encode())["messages"]))
        too_many = messages + [{"role": "user", "content": "Another"}]
        with self.assertRaises(ValueError):
            parse_assistant_request(json.dumps(request_value(messages=too_many)).encode())
        with self.assertRaises(ValueError):
            parse_assistant_request(json.dumps(request_value(messages=[{"role": "assistant", "content": "late"}])).encode())

    def test_chat_tools_are_allowlisted_and_args_mirror_native_adapter(self):
        accepted = {
            "draft_meal": {}, "confirm_meal_draft": {}, "decline_meal_draft": {},
            "adjust_current_draft_portion": {"grams": 250}, "replace_current_draft_food": {"replacement_name": "lentils"},
            "read_latest_meal": {}, "read_selected_meal": {}, "read_progress_overview": {"period": "today"},
            "read_nutrition_summary": {"period": "week"}, "read_weight": {"from_days_ago": 60, "to_days_ago": 0},
            "read_meals": {"days_ago": 1, "meal_slot": "dinner"}, "read_day_nutrition": {"days_ago": 2},
            "search_saved_dishes": {"query": "lentil soup"},
        }
        self.assertEqual(CHAT_TOOLS, set(accepted))
        for name, arguments in accepted.items():
            with self.subTest(name=name):
                reply, tool = validate_decision(json.dumps({"reply": None, "tool": {"name": name, "arguments": arguments}}), "chat")
                self.assertIsNone(reply)
                self.assertEqual(name, tool["name"])
        for task, name, args in (("meal_draft", "read_weight", {"from_days_ago": 1, "to_days_ago": 0}),
                                 ("portion_edit", "replace_current_draft_food", {"replacement_name": "peas"}),
                                 ("food_replacement", "adjust_current_draft_portion", {"grams": 50})):
            with self.subTest(task=task), self.assertRaises(ProviderFailure):
                validate_decision(json.dumps({"reply": None, "tool": {"name": name, "arguments": args}}), task)

    def test_invalid_actions_duplicate_shapes_and_malformed_outputs_fail_closed(self):
        cases = [
            {"reply": "made up", "tool": {"name": "read_latest_meal", "arguments": {}}},
            {"reply": None, "tool": {"name": "save_meal", "arguments": {}}},
            {"reply": None, "tool": {"name": "read_weight", "arguments": {"from_days_ago": True, "to_days_ago": 0}}},
            {"reply": None, "tool": {"name": "read_meals", "arguments": {"days_ago": 1, "other": "x"}}},
            {"reply": None, "tool": {"name": "read_nutrition_summary", "arguments": {"period": "month"}}},
            {"reply": None, "tool": {"name": "draft_meal", "arguments": {"description": "invented"}}},
            {"reply": "x" * 4001, "tool": None},
            {"reply": None, "tool": None},
        ]
        for value in cases:
            with self.subTest(value=value), self.assertRaises(ProviderFailure):
                validate_decision(json.dumps(value), "chat")
        for content in ("not-json", '{"reply":null,"reply":"forged","tool":null}', "[]"):
            with self.subTest(content=content), self.assertRaises(ProviderFailure):
                validate_decision(content, "chat")

    def test_meal_draft_requires_native_schema_or_empty_draft_with_question(self):
        reply, tool = validate_decision(json.dumps({"reply": json.dumps(PROPOSAL), "tool": None}), "meal_draft")
        self.assertIsNone(tool)
        self.assertEqual("meal_proposal", json.loads(reply)["kind"])
        reply, _ = validate_decision(json.dumps({"reply": json.dumps(EMPTY_PROPOSAL), "tool": None}), "meal_draft")
        self.assertEqual([], json.loads(reply)["items"])
        for proposal in ({**PROPOSAL, "save": True}, {**EMPTY_PROPOSAL, "questions": []},
                         {**PROPOSAL, "items": [{**PROPOSAL["items"][0], "estimated_grams_range": {"min": 1, "max": 2001}}]}):
            with self.subTest(proposal=proposal), self.assertRaises(ProviderFailure):
                validate_decision(json.dumps({"reply": json.dumps(proposal), "tool": None}), "meal_draft")

    def test_structured_meal_output_keeps_validation_without_escaping_repairs(self):
        for proposal in (PROPOSAL, EMPTY_PROPOSAL):
            reply, tool = validate_decision(json.dumps({"reply": proposal, "tool": None}), "meal_draft")
            self.assertEqual(proposal, json.loads(reply))
            self.assertIsNone(tool)
        for proposal in ({**PROPOSAL, "assumptions": "not an array"},
                         {**PROPOSAL, "questions": "not an array"}, {**PROPOSAL, "save": True}):
            with self.assertRaises(ProviderFailure):
                validate_decision(json.dumps({"reply": proposal, "tool": None}), "meal_draft")
        with self.assertRaises(ProviderFailure):
            validate_decision(json.dumps({"reply": json.dumps(PROPOSAL).replace('"', '\\"'), "tool": None}), "meal_draft")


class FakeResponse:
    def __init__(self, status, body):
        self.status, self.body = status, body
    def read1(self, size):
        value, self.body = self.body[:size], self.body[size:]
        return value
    def getheader(self, name, default=""):
        return "application/json" if name == "Content-Type" else default


class FakeConnection:
    instances = []
    status_body = None
    def __init__(self, *args, **kwargs):
        self.sock = None
        self.calls = []
        self.instances.append(self)
    def connect(self): pass
    def request(self, *args): self.calls.append(args)
    def getresponse(self): return FakeResponse(*self.status_body)
    def close(self): pass


class AssistantProviderTests(unittest.TestCase):
    def setUp(self):
        FakeConnection.instances.clear()

    def test_fixed_prompt_and_single_post_return_content_free_usage(self):
        output = {"reply": "We can review your recorded progress together.", "tool": None}
        FakeConnection.status_body = envelope(json.dumps(output))
        request = parse_assistant_request(json.dumps(request_value()).encode())
        with patch("cloud_assistant.http.client.HTTPSConnection", FakeConnection):
            result = SuperVLAssistantAdapter({"NEBIUS_API_KEY": "operator-secret"})(request)
        self.assertEqual(1, len(FakeConnection.instances))
        method, path, body, headers = FakeConnection.instances[0].calls[0]
        provider_request = json.loads(body)
        self.assertEqual(("POST", "/v1/chat/completions"), (method, path))
        self.assertEqual("Bearer operator-secret", headers["Authorization"])
        self.assertEqual(MODEL, provider_request["model"])
        self.assertEqual("system", provider_request["messages"][0]["role"])
        system = provider_request["messages"][0]["content"]
        for name in CHAT_TOOLS:
            self.assertIn(name + ": arguments:", system)
        self.assertIn('"name":"read_latest_meal","arguments":{}', system)
        self.assertIn("instead of saying you cannot access records", system)
        self.assertNotIn("operator-secret", body.decode())
        self.assertEqual(output["reply"], result["reply"])
        self.assertIsNone(result["tool"])
        self.assertEqual({"prompt_tokens": 1200, "completion_tokens": 60, "total_tokens": 1260}, result["usage"])

    def test_tool_contracts_cover_exact_allowlist_without_enabling_tools_for_drafting(self):
        self.assertEqual(CHAT_TOOLS, set(TOOL_CONTRACTS))
        self.assertEqual("", tool_instructions("meal_draft"))
        self.assertIn("adjust_current_draft_portion: arguments:", tool_instructions("portion_edit"))
        self.assertNotIn("read_latest_meal: arguments:", tool_instructions("portion_edit"))
        self.assertNotIn("draft_meal: arguments:", tool_instructions("food_replacement"))

    def test_latest_meal_read_request_returns_action_without_private_record_prose(self):
        output = {"reply": None, "tool": {"name": "read_latest_meal", "arguments": {}}}
        FakeConnection.status_body = envelope(json.dumps(output))
        request = parse_assistant_request(json.dumps(request_value(
            messages=[{"role": "user", "content": "How many calories were in my latest meal?"}])).encode())
        with patch("cloud_assistant.http.client.HTTPSConnection", FakeConnection):
            result = SuperVLAssistantAdapter({"NEBIUS_API_KEY": "operator-secret"})(request)
        self.assertIsNone(result["reply"])
        self.assertEqual(output["tool"], result["tool"])
        self.assertEqual(1, len(FakeConnection.instances[0].calls))

    def test_meal_draft_builds_exact_unsaved_proposal_and_no_tool(self):
        output = {"reply": PROPOSAL, "tool": None}
        FakeConnection.status_body = envelope(json.dumps(output))
        request = parse_assistant_request(json.dumps(request_value(task="meal_draft")).encode())
        with patch("cloud_assistant.http.client.HTTPSConnection", FakeConnection):
            result = SuperVLAssistantAdapter({"NEBIUS_API_KEY": "operator-secret"})(request)
        self.assertEqual("meal_proposal", json.loads(result["reply"])["kind"])
        system = json.loads(FakeConnection.instances[0].calls[0][2])["messages"][0]["content"]
        self.assertIn("Do not use a tool for this task", system)
        self.assertIn("Nothing is saved", system)
        payload = json.loads(FakeConnection.instances[0].calls[0][2])
        schema = payload["response_format"]["json_schema"]["schema"]
        self.assertEqual("object", schema["properties"]["reply"]["type"])
        self.assertEqual("array", schema["properties"]["reply"]["properties"]["questions"]["type"])
        self.assertEqual("null", schema["properties"]["tool"]["type"])

    def test_rejected_meal_retains_reported_usage_without_retry(self):
        output = {"reply": {**PROPOSAL, "assumptions": "not an array"}, "tool": None}
        FakeConnection.status_body = envelope(json.dumps(output))
        request = parse_assistant_request(json.dumps(request_value(task="meal_draft")).encode())
        with patch("cloud_assistant.http.client.HTTPSConnection", FakeConnection), self.assertRaises(ProviderFailure) as raised:
            SuperVLAssistantAdapter({"NEBIUS_API_KEY": "operator-secret"})(request)
        self.assertEqual({key: USAGE[key] for key in ("prompt_tokens", "completion_tokens", "total_tokens")}, raised.exception.usage)
        self.assertEqual("response_selection_schema_invalid", raised.exception.diagnostic)
        self.assertEqual(1, len(FakeConnection.instances[0].calls))

    def test_provider_errors_and_invalid_tool_calls_are_rejected(self):
        request = parse_assistant_request(json.dumps(request_value()).encode())
        bad = [envelope("{}", status=401), envelope("{}", model="other"), envelope("{}", finish="length"),
               envelope('{"reply":null,"tool":{"name":"save","arguments":{}}}'),
               envelope('{"reply":null,"tool":{"name":"read_weight","arguments":{"from_days_ago":false,"to_days_ago":0}}}')]
        for status_body in bad:
            FakeConnection.status_body = status_body
            with self.subTest(status=status_body[0]), patch("cloud_assistant.http.client.HTTPSConnection", FakeConnection), self.assertRaises(ProviderFailure):
                SuperVLAssistantAdapter({"NEBIUS_API_KEY": "operator-secret"})(request)


class AssistantHttpTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.now = datetime(2026, 10, 27, tzinfo=timezone.utc).timestamp()
        self.store = Store(Path(self.temp.name) / "state.sqlite", clock=lambda: self.now)
        self.code = self.store.issue_pass(self.now + 86400, 2)
        self.adapter = lambda request: {"request_id": request["request_id"], "model": MODEL,
            "provider": "nebius", "reply": "Short answer.", "tool": None,
            "usage": {"prompt_tokens": 100, "completion_tokens": 20, "total_tokens": 120}}
        self.server = LocalBroker(0, self.store, assistant_responder=self.adapter, allow_cloud_assistant=True)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.token = self.activate()[2]["credential"]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(2)
        self.temp.cleanup()

    def send(self, path, value=None, token=None, headers=None, raw=None):
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

    def activate(self):
        return self.send("/v1/judge/activate", {"pass_code": self.code, "installation_id": str(uuid4())})

    def test_chat_turn_reuses_pass_auth_and_no_store(self):
        status, headers, body = self.send("/v1/assistant", request_value(), self.token)
        self.assertEqual(200, status)
        self.assertEqual("no-store", headers["Cache-Control"])
        self.assertEqual("Short answer.", body["reply"])
        self.assertIsNone(body["tool"])
        self.assertEqual(MODEL, body["model"])

    def test_auth_consent_disabled_body_and_adapter_failure_are_bounded(self):
        self.assertEqual(403, self.send("/v1/assistant", request_value(), "wrong-token")[0])
        self.assertEqual(400, self.send("/v1/assistant", {**request_value(), "consent_version": 2}, self.token)[0])
        self.assertEqual(400, self.send("/v1/assistant", request_value(), self.token,
                                       headers={"Origin": "https://attacker.invalid"})[0])
        disabled = LocalBroker(0, self.store)
        thread = threading.Thread(target=disabled.serve_forever, daemon=True)
        thread.start()
        try:
            conn = HTTPConnection("127.0.0.1", disabled.server_port, timeout=3)
            conn.request("POST", "/v1/assistant", json.dumps(request_value()).encode(),
                         {"Content-Type": "application/json", "Authorization": "Bearer " + self.token,
                          "X-Metria-Sent-At": str(int(self.now))})
            self.assertEqual(400, conn.getresponse().status)
            conn.close()
        finally:
            disabled.shutdown()
            disabled.server_close()
            thread.join(2)
        self.server.assistant_responder = lambda request: (_ for _ in ()).throw(ProviderFailure("provider_unavailable"))
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            status, _, response = self.send("/v1/assistant", request_value(), self.token)
        self.assertEqual(502, status)
        self.assertEqual({"error": "provider_response_unavailable"}, response)
        self.assertEqual("", output.getvalue())

    def test_hosted_flag_is_explicit_and_photo_name_is_compatibility_alias(self):
        base = {"METRIA_PUBLIC_HOST": "demo.example.org", "METRIA_PERSISTENT_STATE_CONFIRMED": "yes"}
        with self.assertRaises(ValueError):
            arguments(base | {"METRIA_ALLOW_CLOUD_ASSISTANT": "yes"})
        live = base | {"METRIA_LIVE": "1", "METRIA_ACCOUNT_VERIFIED": "yes"}
        self.assertIn("--allow-cloud-assistant", arguments(live | {"METRIA_ALLOW_CLOUD_ASSISTANT": "yes"}))
        self.assertIn("--allow-cloud-assistant", arguments(live | {"METRIA_ALLOW_CLOUD_PHOTOS": "yes"}))
        self.assertNotIn("--allow-cloud-assistant", arguments(base))


if __name__ == "__main__":
    unittest.main()
