import contextlib
import io
import json
import os
import queue
import re
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timezone
from http.client import HTTPConnection
from pathlib import Path
from uuid import uuid4

from contract import MODEL, parse_request
from server import LocalBroker, private_directory
from store import Denied, Limits, Store


def payload(request_id=None, question="My fictional weight?"):
    return json.dumps({"schema_version": 1, "request_id": request_id or str(uuid4()), "provider": "nebius",
                       "model": MODEL, "question": question, "language": "English",
                       "period": {"start": "2026-10-03", "end": "2026-10-03"}}).encode()


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / "state.sqlite3"
        self.now = 1791028800.0  # 2026-10-03 noon UTC.
        self.store = self.open()
        self.token = self.store.provision()

    def tearDown(self):
        self.temp.cleanup()

    def open(self, limits=Limits()):
        return Store(self.path, limits, lambda: self.now)

    def attempt(self, store=None, token=None, request_id=None):
        request_id = request_id or str(uuid4())
        store = store or self.store
        store.admit(token or self.token, self.now, request_id)
        return request_id

    def test_credentials_survive_restart_but_revocation_and_expiry_do_not_reauthorize(self):
        self.open().authenticate(self.token)
        self.store.revoke(self.token)
        with self.assertRaises(Denied):
            self.open().authenticate(self.token)
        other = self.store.provision()
        self.now += 86400
        with self.assertRaises(Denied):
            self.open().authenticate(other)

    def test_replay_and_dispatch_survive_restart(self):
        request_id = self.attempt()
        self.open().dispatch(self.token, request_id)
        with self.assertRaises(Denied):
            self.open().dispatch(self.token, request_id)
        self.store.finish(self.token, request_id)
        with self.assertRaises(Denied):
            self.attempt(self.open(), request_id=request_id)
        other = self.store.provision()
        with self.assertRaises(Denied):
            self.attempt(self.open(), other, request_id)

    def test_global_and_credential_budget_survive_restart_rotation_and_failure(self):
        limits = Limits(global_day=1)
        store = self.open(limits)
        request_id = self.attempt(store)
        store.finish(self.token, request_id)
        with self.assertRaises(Denied):
            self.attempt(self.open(limits), self.store.provision())
        with contextlib.closing(sqlite3.connect(self.path)) as db:
            self.assertEqual(2, db.execute("SELECT count(*) FROM quotas").fetchone()[0])

    def test_token_reservation_and_rate_limits_do_not_refund(self):
        limits = Limits(credential_units=Store.UNITS)
        store = self.open(limits)
        request_id = self.attempt(store)
        store.finish(self.token, request_id)
        self.now += 61
        with self.assertRaises(Denied):
            self.attempt(self.open(limits))
        with self.assertRaises(Denied):
            self.attempt(self.open(Limits(global_units=Store.UNITS - 1)), self.store.provision())

    def test_concurrency_and_freshness(self):
        request_id = self.attempt()
        with self.assertRaises(Denied):
            self.attempt()
        self.now += 60
        with self.assertRaises(Denied):
            self.store.dispatch(self.token, request_id)
        with self.assertRaises(Denied):
            self.store.admit(self.token, self.now - 60, str(uuid4()))
        with self.assertRaises(Denied):
            self.store.admit(self.token, self.now + 1, str(uuid4()))
        self.attempt()

    def test_clock_rollback_survives_restart_cleanup_and_revoke_remain_available(self):
        request_id = self.attempt()
        self.now -= 1
        with self.assertRaises(Denied):
            self.open().authenticate(self.token)
        self.store.finish(self.token, request_id)
        self.assertTrue(self.store.revoke(self.token))

    def test_atomic_cross_connection_replay_race(self):
        request_id = str(uuid4())
        def admit(_):
            try:
                self.attempt(self.open(), request_id=request_id)
                return True
            except Denied:
                return False
        with ThreadPoolExecutor(max_workers=4) as pool:
            self.assertEqual(1, sum(pool.map(admit, range(8))))

    def test_utc_rollover_preserves_replay_and_resets_only_daily_quota(self):
        store = self.open(Limits(per_day=1))
        request_id = self.attempt(store)
        store.finish(self.token, request_id)
        self.now += 12 * 3600 + 60
        with self.assertRaises(Denied):
            self.attempt(self.open(), request_id=request_id)
        self.attempt(self.open(Limits(per_day=1)))

    def test_store_contains_only_metadata_and_schema_fails_closed(self):
        request_id = self.attempt()
        self.store.finish(self.token, request_id)
        data = self.path.read_bytes()
        self.assertNotIn(self.token.encode(), data)
        self.assertNotIn(b"My fictional weight", data)
        with contextlib.closing(sqlite3.connect(self.path)) as db:
            db.execute("PRAGMA user_version=99")
        with self.assertRaises(ValueError):
            self.open()

    def test_missing_quota_table_is_not_silently_recreated(self):
        with contextlib.closing(sqlite3.connect(self.path)) as db:
            db.execute("DROP TABLE quotas")
        with self.assertRaises(ValueError):
            self.open()


class ContractTests(unittest.TestCase):
    def test_shared_android_fixtures(self):
        fixtures = Path(__file__).resolve().parents[1] / "native-android/app/src/test/resources/online-contract-fixtures.json"
        for case in json.loads(fixtures.read_text(encoding="utf-8")):
            with self.subTest(case=case["name"]):
                raw = case["raw"].encode() if "raw" in case else json.dumps(case["payload"]).encode()
                if case["valid"]:
                    parse_request(raw, date(2026, 10, 3))
                else:
                    with self.assertRaises((ValueError, TypeError)):
                        parse_request(raw, date(2026, 10, 3))

    def test_strict_json_unicode_and_question_bounds(self):
        for raw in (b'{"a":1,"\\u0061":2}', b'{"x":NaN}', b'{"x":1,}', b"{}{}", b'//comment\n{}',
                    b'{"x":01}', b'{"x":' + b'[' * 10 + b'0' + b']' * 10 + b'}', b'\xff', payload(question="x" * 1001),
                    payload(question="\u0000"), b"x" * 16385):
            with self.subTest(raw=raw[:20]), self.assertRaises((ValueError, UnicodeError)):
                parse_request(raw, date(2026, 10, 3))


class HttpTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.now = 1791028800.0
        self.store = Store(Path(self.temp.name) / "state.sqlite3", clock=lambda: self.now)
        self.token = self.store.provision()
        self.server = LocalBroker(0, self.store)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(2)
        self.temp.cleanup()

    def request(self, raw=None, headers=None, path="/v1/assistance", method="POST"):
        connection = HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)
        try:
            default = {"Authorization": f"Bearer {self.token}", "Content-Type": "application/json",
                       "X-Metria-Sent-At": str(int(self.now))}
            default.update(headers or {})
            connection.request(method, path, body=raw if raw is not None else payload(), headers=default)
            response = connection.getresponse()
            return response.status, dict(response.getheaders()), json.loads(response.read())
        finally:
            connection.close()

    def test_http_synthetic_success_replay_and_no_store(self):
        raw = payload()
        status, headers, answer = self.request(raw)
        self.assertEqual(200, status)
        self.assertEqual("synthetic", headers["X-Metria-Mode"])
        self.assertEqual("no-store", headers["Cache-Control"])
        self.assertEqual(json.loads(raw)["request_id"], answer["request_id"])
        self.assertEqual(set(answer), {"schema_version", "request_id", "language", "sections", "guidance"})
        self.assertEqual(403, self.request(raw)[0])
        self.assertEqual({"mode": "synthetic", "provider_connected": False}, self.request(path="/health", method="GET")[2])

    def test_forgery_invalid_bounds_browser_origins_and_host_rebinding(self):
        self.assertEqual(403, self.request(headers={"Authorization": "Bearer forged"})[0])
        self.assertEqual(400, self.request(b"x" * 16385)[0])
        self.assertEqual(400, self.request(b"{}")[0])
        self.assertEqual(400, self.request(headers={"Origin": "https://example.invalid"})[0])
        self.assertEqual(400, self.request(headers={"Host": "example.invalid"})[0])
        self.assertEqual(400, self.request(path="/provision")[0])
        self.assertEqual(400, self.request(headers={"Transfer-Encoding": "chunked"})[0])

    def test_logs_do_not_contain_question_authorization_or_error_details(self):
        capture = io.StringIO()
        with contextlib.redirect_stderr(capture), contextlib.redirect_stdout(capture):
            self.request(payload(question="FICTIONAL_PRIVATE_MARKER"))
            self.request(b"not json")
        self.assertEqual("", capture.getvalue())

    def test_private_directory_is_restricted_and_rejects_linked_paths(self):
        directory = private_directory(Path(self.temp.name) / "private")
        self.assertTrue(directory.is_dir())
        unrelated = Path(self.temp.name) / "unrelated"
        unrelated.mkdir()
        (unrelated / "keep.txt").write_text("unrelated")
        with self.assertRaises(ValueError):
            private_directory(unrelated)


class ProcessTests(unittest.TestCase):
    def test_cli_provision_process_restart_replay_and_revoke(self):
        with tempfile.TemporaryDirectory() as root:
            state = Path(root) / "broker-state"
            credential = state / "client.credential"
            script = Path(__file__).with_name("server.py")
            flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
            base = [sys.executable, str(script)]
            def command(action):
                result = subprocess.run(base + [action, "--state-dir", str(state), "--credential-file", str(credential)],
                                        capture_output=True, text=True, timeout=10, creationflags=flags)
                self.assertEqual(0, result.returncode, "CLI operation failed")
                return result.stdout
            command("provision")
            token = credential.read_text(encoding="ascii")
            process = None
            def start():
                nonlocal process
                process = subprocess.Popen(base + ["serve", "--state-dir", str(state), "--port", "0"],
                                           stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, creationflags=flags)
                ready = queue.Queue()
                threading.Thread(target=lambda: ready.put(process.stdout.readline()), daemon=True).start()
                line = ready.get(timeout=10)
                self.assertNotIn(token, line)
                match = re.search(r"127\.0\.0\.1:(\d+)", line)
                self.assertIsNotNone(match, "Broker did not start")
                return int(match.group(1))
            def stop():
                if process is not None:
                    process.terminate()
                    output, error = process.communicate(timeout=10)
                    self.assertNotIn(token, output + error)
            def send(port, raw):
                conn = HTTPConnection("127.0.0.1", port, timeout=3)
                try:
                    conn.request("POST", "/v1/assistance", raw, {"Authorization": f"Bearer {token}",
                                 "Content-Type": "application/json", "X-Metria-Sent-At": str(int(time.time()))})
                    response = conn.getresponse()
                    response.read()
                    return response.status
                finally:
                    conn.close()
            today = datetime.now(timezone.utc).date().isoformat()
            request = json.loads(payload())
            request["period"] = {"start": today, "end": today}
            raw = json.dumps(request).encode()
            try:
                port = start()
                self.assertEqual(200, send(port, raw))
                stop()
                port = start()
                self.assertEqual(403, send(port, raw))
                request["request_id"] = str(uuid4())
                self.assertEqual(200, send(port, json.dumps(request).encode()))
                self.assertNotIn(token, command("revoke"))
                request["request_id"] = str(uuid4())
                self.assertEqual(403, send(port, json.dumps(request).encode()))
                self.assertFalse(credential.exists())
            finally:
                stop()
            (state / "admission.sqlite3").unlink()
            refused = subprocess.run(base + ["provision", "--state-dir", str(state), "--credential-file", str(credential)],
                                     capture_output=True, text=True, timeout=10, creationflags=flags)
            self.assertNotEqual(0, refused.returncode)
            self.assertFalse((state / "admission.sqlite3").exists())


if __name__ == "__main__":
    unittest.main()
