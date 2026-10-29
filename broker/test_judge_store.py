"""Judge-pass activation, revocation and non-resetting quota tests."""
import concurrent.futures
import tempfile
import unittest
import uuid
from pathlib import Path

from store import Denied, Limits, Store


class JudgePassTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / "state.sqlite3"
        self.now = 1791014400.0  # 2026-10-03 UTC
        self.store = Store(self.path, clock=lambda: self.now)

    def tearDown(self):
        self.temp.cleanup()

    def new_pass(self, max_requests=2):
        return self.store.issue_pass(self.now + 7 * 86400, max_requests)

    def test_activation_is_bound_to_one_installation_and_is_idempotent(self):
        code = self.new_pass()
        installation = str(uuid.uuid4())
        bearer = self.store.activate_pass(code, installation)
        self.assertEqual(43, len(bearer))
        self.assertEqual(bearer, self.store.activate_pass(code, installation))
        with self.assertRaises(Denied):
            self.store.activate_pass(code, str(uuid.uuid4()))
        with self.assertRaises(Denied):
            self.store.activate_pass("x" * 43, str(uuid.uuid4()))

    def test_expiry_and_maximum_demo_window_are_enforced(self):
        with self.assertRaises(ValueError):
            self.store.issue_pass(self.now + 91 * 86400)
        with self.assertRaises(ValueError):
            self.store.issue_pass(self.now + 1, max_requests=101)
        code = self.new_pass()
        bearer = self.store.activate_pass(code, str(uuid.uuid4()))
        self.now += 8 * 86400
        with self.assertRaises(Denied):
            self.store.authenticate(bearer)

    def test_revocation_invalidates_pass_and_installed_bearer(self):
        code = self.new_pass()
        bearer = self.store.activate_pass(code, str(uuid.uuid4()))
        self.assertTrue(self.store.revoke_pass(code))
        self.assertFalse(self.store.revoke_pass(code))
        with self.assertRaises(Denied):
            self.store.authenticate(bearer)
        with self.assertRaises(Denied):
            self.store.activate_pass(code, str(uuid.uuid4()))

    def test_lifetime_pass_quota_survives_restart_and_utc_day_rollover(self):
        code = self.new_pass(max_requests=2)
        installation = str(uuid.uuid4())
        bearer = self.store.activate_pass(code, installation)
        self.store.admit(bearer, int(self.now), "attempt-one")
        self.store.finish(bearer, "attempt-one")
        self.now += 86400
        self.store = Store(self.path, clock=lambda: self.now)
        self.assertEqual(bearer, self.store.activate_pass(code, installation))
        self.store.admit(bearer, int(self.now), "attempt-two")
        self.store.finish(bearer, "attempt-two")
        self.now += 86400
        self.store = Store(self.path, clock=lambda: self.now)
        with self.assertRaises(Denied):
            self.store.admit(bearer, int(self.now), "attempt-three")

    def test_different_passes_share_durable_global_lifetime_cap(self):
        self.store = Store(self.path, limits=Limits(global_lifetime=1), clock=lambda: self.now)
        first = self.store.provision()
        self.store.admit(first, int(self.now), "global-one")
        self.store.finish(first, "global-one")
        self.now += 86400
        self.store = Store(self.path, limits=Limits(global_lifetime=1), clock=lambda: self.now)
        judge_code = self.new_pass()
        second = self.store.activate_pass(judge_code, str(uuid.uuid4()))
        with self.assertRaises(Denied):
            self.store.admit(second, int(self.now), "global-two")

    def test_concurrent_activation_claims_pass_once(self):
        code = self.new_pass()
        installations = [str(uuid.uuid4()), str(uuid.uuid4())]
        def activate(installation):
            try:
                return self.store.activate_pass(code, installation)
            except Denied:
                return None
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(activate, installations))
        self.assertEqual(1, sum(value is not None for value in results))

    def test_migrates_complete_v1_state_and_fails_closed_on_partial_schema(self):
        import sqlite3
        migrated = Path(self.temp.name) / "v1.sqlite3"
        db = sqlite3.connect(migrated)
        db.executescript("""
            CREATE TABLE credentials (digest TEXT PRIMARY KEY, expires REAL NOT NULL, revoked INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE attempts (id TEXT PRIMARY KEY, credential TEXT NOT NULL, admitted REAL NOT NULL,
                expires REAL NOT NULL, lease_expires REAL NOT NULL, active INTEGER NOT NULL, dispatched INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE quotas (day TEXT NOT NULL, owner TEXT NOT NULL, requests INTEGER NOT NULL, units INTEGER NOT NULL,
                PRIMARY KEY(day, owner));
            CREATE TABLE clock (id INTEGER PRIMARY KEY CHECK(id=1), last REAL NOT NULL);
            PRAGMA user_version=1;
        """)
        db.execute("INSERT INTO quotas VALUES('2026-10-02','global',3,26112)")
        db.execute("INSERT INTO clock(id,last) VALUES(1,?)", (self.now - 1,))
        db.commit()
        db.close()
        Store(migrated, clock=lambda: self.now)
        db = sqlite3.connect(migrated)
        self.assertEqual(3, db.execute("SELECT lifetime_requests FROM clock WHERE id=1").fetchone()[0])
        db.close()
        db = sqlite3.connect(migrated)
        self.assertEqual(2, db.execute("PRAGMA user_version").fetchone()[0])
        db.close()
        partial = Path(self.temp.name) / "partial.sqlite3"
        db = sqlite3.connect(partial)
        db.execute("CREATE TABLE credentials (digest TEXT)")
        db.execute("PRAGMA user_version=1")
        db.close()
        with self.assertRaises(ValueError):
            Store(partial, clock=lambda: self.now)


if __name__ == "__main__":
    unittest.main()
