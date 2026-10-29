"""Judge HTTP contract, isolation and reverse-proxy boundaries; no external calls."""
import contextlib
import io
import json
import tempfile
import threading
import unittest
from datetime import date
from http.client import HTTPConnection
from pathlib import Path
from uuid import uuid4

from contract import MODEL, synthetic_response
from hosted import arguments
from server import LocalBroker
from store import Store


class JudgeHttpTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.now = 1791028800.0
        self.store = Store(Path(self.temp.name) / "state.sqlite3", clock=lambda: self.now)
        self.code = self.store.issue_pass(self.now + 86400 * 10, 2)
        self.server = LocalBroker(0, self.store)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.installation = str(uuid4())

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(2)
        self.temp.cleanup()

    def send(self, path, value=None, token=None, headers=None, raw=None):
        connection = HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)
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
        return self.send("/v1/judge/activate", {"pass_code": self.code, "installation_id": self.installation})

    def test_activate_replay_quota_revocation_and_no_logs(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            status, headers, body = self.activate()
            self.assertEqual(200, status)
            self.assertEqual("no-store", headers["Cache-Control"])
            token = body["credential"]
            self.assertEqual(token, self.activate()[2]["credential"])
            raw = {"scenario": "weight", "request_id": str(uuid4())}
            status, _, result = self.send("/v1/demo", raw, token)
            self.assertEqual(200, status)
            self.assertTrue(result["fictional"])
            self.assertEqual("synthetic", result["mode"])
            self.assertEqual(403, self.send("/v1/demo", raw, token)[0])
            self.assertEqual(200, self.send("/v1/demo", {**raw, "request_id": str(uuid4())}, token)[0])
            self.now += 61
            self.assertEqual(403, self.send("/v1/demo", {**raw, "request_id": str(uuid4())}, token)[0])
            self.store.revoke_pass(self.code)
            self.assertEqual(403, self.activate()[0])
        self.assertEqual("", output.getvalue())

    def test_strict_activation_cannot_supply_records_or_rebind_installation(self):
        self.assertEqual(400, self.send("/v1/judge/activate", {}, raw=b'{"pass_code":"a","pass_code":"b"}')[0])
        self.assertEqual(403, self.send("/v1/judge/activate", {"pass_code": "forged", "installation_id": self.installation})[0])
        self.assertEqual(200, self.activate()[0])
        self.assertEqual(403, self.send("/v1/judge/activate", {"pass_code": self.code, "installation_id": str(uuid4())})[0])
        token = self.activate()[2]["credential"]
        value = {"scenario": "weight", "request_id": str(uuid4()), "question": "PRIVATE_MARKER"}
        self.assertEqual(400, self.send("/v1/demo", value, token)[0])
        self.assertEqual(400, self.send("/v1/demo", {"scenario": "arbitrary", "request_id": str(uuid4())}, token)[0])
        self.assertEqual(400, self.send("/v1/judge/activate", {}, headers={"Origin": "https://evil.invalid"})[0])
        self.assertEqual(400, self.send("/v1/judge/activate", {}, headers={"Transfer-Encoding": "chunked"})[0])

    def test_activation_is_globally_bounded_without_upstream_calls(self):
        for _ in range(30):
            self.assertEqual(400, self.send("/v1/judge/activate", {})[0])
        self.assertEqual(429, self.activate()[0])

    def test_live_mode_only_dispatches_server_samples(self):
        class FakeLive:
            mode = "nebius"
            models = frozenset({MODEL})
            calls = 0
            def __call__(responder, request, preview):
                responder.calls += 1
                return synthetic_response(request)
        self.server.responder = FakeLive()
        self.server.mode = "nebius"
        token = self.activate()[2]["credential"]
        self.assertEqual(400, self.send("/v1/assistance", {"question": "PRIVATE_MARKER"}, token)[0])
        self.assertEqual(0, self.server.responder.calls)
        self.assertEqual(200, self.send("/v1/demo", {"scenario": "weight", "request_id": str(uuid4())}, token)[0])
        self.assertEqual(1, self.server.responder.calls)

    def test_hosted_mode_requires_exact_host_and_https_proxy(self):
        # Exercise handler policy over loopback without binding a public listener.
        self.server.public_host = "demo.example.org"
        self.assertEqual(400, self.activate()[0])
        body = {"pass_code": self.code, "installation_id": self.installation}
        self.assertEqual(400, self.send("/v1/judge/activate", body, headers={"Host": "demo.example.org"})[0])
        self.assertEqual(200, self.send("/v1/judge/activate", body, headers={"Host": "demo.example.org", "X-Forwarded-Proto": "https"})[0])

    def test_hosted_configuration_fails_closed_and_never_enables_live_implicitly(self):
        for environment in ({}, {"METRIA_PUBLIC_HOST": "demo.example.org"},
                            {"METRIA_PUBLIC_HOST": "demo.example.org", "METRIA_PERSISTENT_STATE_CONFIRMED": "yes", "METRIA_LIVE": "1"}):
            with self.assertRaises(ValueError):
                arguments(environment)
        base = {"METRIA_PUBLIC_HOST": "demo.example.org", "METRIA_PERSISTENT_STATE_CONFIRMED": "yes"}
        self.assertNotIn("--live", arguments(base))
        self.assertIn("--live", arguments(base | {"METRIA_LIVE": "1", "METRIA_ACCOUNT_VERIFIED": "yes"}))


if __name__ == "__main__":
    unittest.main()
