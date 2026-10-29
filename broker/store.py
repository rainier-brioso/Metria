"""Content-free SQLite admission accounting. No provider credentials or responses."""
import hashlib
import base64
import hmac
import secrets
import sqlite3
import time
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone


@dataclass(frozen=True)
class Limits:
    per_minute: int = 3
    per_day: int = 20
    global_day: int = 100
    concurrent: int = 2
    credential_units: int = 200_000
    global_units: int = 1_000_000
    global_lifetime: int = 1000

    def __post_init__(self):
        if not (1 <= self.per_minute <= 20 and 1 <= self.per_day <= 100 and 1 <= self.global_day <= 1000
                and 1 <= self.concurrent <= 10 and 1 <= self.credential_units <= 10_000_000
                and 1 <= self.global_units <= 100_000_000 and 1 <= self.global_lifetime <= 100_000):
            raise ValueError("limits")


class Denied(Exception):
    """Fixed public outcome; never attach credential or request content."""


class Store:
    UNITS = 8704
    MAX_PASSES = 32
    PASS_DEADLINE = datetime(2026, 12, 16, tzinfo=timezone.utc).timestamp()

    def __init__(self, path, limits=Limits(), clock=time.time):
        self.path, self.limits, self.clock = str(path), limits, clock
        with self._connection() as db:
            version = db.execute("PRAGMA user_version").fetchone()[0]
            if version not in (0, 1, 2):
                raise ValueError("unsupported state version")
            tables = {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            expected_v1 = {"credentials", "attempts", "quotas", "clock"}
            expected_v2 = expected_v1 | {"passes"}
            if ((version == 0 and tables) or (version == 1 and tables != expected_v1)
                    or (version == 2 and tables != expected_v2)):
                raise ValueError("incomplete state schema")
            if version == 0:
                db.executescript("""
                    BEGIN IMMEDIATE;
                    CREATE TABLE credentials (digest TEXT PRIMARY KEY, expires REAL NOT NULL, revoked INTEGER NOT NULL DEFAULT 0);
                    CREATE TABLE attempts (id TEXT PRIMARY KEY, credential TEXT NOT NULL, admitted REAL NOT NULL,
                        expires REAL NOT NULL, lease_expires REAL NOT NULL, active INTEGER NOT NULL, dispatched INTEGER NOT NULL DEFAULT 0);
                    CREATE TABLE quotas (day TEXT NOT NULL, owner TEXT NOT NULL, requests INTEGER NOT NULL, units INTEGER NOT NULL,
                        PRIMARY KEY(day, owner));
                    CREATE TABLE clock (id INTEGER PRIMARY KEY CHECK(id=1), last REAL NOT NULL);
                    PRAGMA user_version=1;
                    COMMIT;
                """)
                version = 1
            if version == 1:
                db.executescript("""
                    BEGIN IMMEDIATE;
                    ALTER TABLE credentials ADD COLUMN pass_digest TEXT;
                    ALTER TABLE clock ADD COLUMN lifetime_requests INTEGER NOT NULL DEFAULT 0;
                    UPDATE clock SET lifetime_requests=COALESCE((SELECT SUM(requests) FROM quotas WHERE owner='global'),0);
                    CREATE TABLE passes (digest TEXT PRIMARY KEY, expires REAL NOT NULL, max_requests INTEGER NOT NULL,
                        installation TEXT, revoked INTEGER NOT NULL DEFAULT 0);
                    PRAGMA user_version=2;
                    COMMIT;
                """)

    @contextmanager
    def _connection(self):
        db = sqlite3.connect(self.path, timeout=5, isolation_level=None)
        try:
            db.execute("PRAGMA journal_mode=DELETE")
            db.execute("PRAGMA synchronous=FULL")
            yield db
        finally:
            db.close()

    @contextmanager
    def _transaction(self):
        with self._connection() as db:
            db.execute("BEGIN IMMEDIATE")
            try:
                yield db
                db.commit()
            except BaseException:
                db.rollback()
                raise

    def _now(self, db):
        now = self.clock()
        last = db.execute("SELECT last FROM clock WHERE id=1").fetchone()
        if last and now < last[0]:
            raise Denied()
        db.execute("INSERT INTO clock(id,last,lifetime_requests) VALUES(1,?,0) "
                   "ON CONFLICT(id) DO UPDATE SET last=excluded.last", (now,))
        db.execute("DELETE FROM attempts WHERE expires<=?", (now,))
        db.execute("DELETE FROM credentials WHERE expires<=?", (now,))
        db.execute("UPDATE attempts SET active=0 WHERE lease_expires<=?", (now,))
        # Daily totals outlive credentials; neither rotation nor restart resets them.
        cutoff = datetime.fromtimestamp(now - 2 * 86400, timezone.utc).date().isoformat()
        db.execute("DELETE FROM quotas WHERE day<?", (cutoff,))
        return now

    @staticmethod
    def _digest(bearer):
        if type(bearer) is not str or len(bearer) != 43 or any(c not in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-" for c in bearer):
            raise Denied()
        return hashlib.sha256(bearer.encode("ascii")).hexdigest()

    def _credential(self, db, bearer, now):
        digest = self._digest(bearer)
        row = db.execute("SELECT digest,expires,pass_digest FROM credentials WHERE digest=? AND revoked=0 AND expires>?", (digest, now)).fetchone()
        if not row or not hmac.compare_digest(digest, row[0]):
            raise Denied()
        if row[2] is not None:
            pass_row = db.execute("SELECT expires,revoked FROM passes WHERE digest=?", (row[2],)).fetchone()
            if pass_row is None or pass_row[1] or pass_row[0] <= now:
                raise Denied()
        return row

    @classmethod
    def _opaque_digest(cls, value):
        if type(value) is not str or len(value) != 43 or any(c not in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-" for c in value):
            raise Denied()
        return hashlib.sha256(value.encode("ascii")).hexdigest()

    @staticmethod
    def _pass_bearer(code, installation):
        raw = hmac.new(code.encode("ascii"), ("metria-judge-installation:" + installation).encode("ascii"), hashlib.sha256).digest()
        return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")

    def issue_pass(self, expires_at, max_requests=50):
        if type(expires_at) not in (int, float) or type(expires_at) is bool or type(max_requests) is not int or not 1 <= max_requests <= 100:
            raise ValueError("invalid pass bounds")
        with self._transaction() as db:
            now = self._now(db)
            if not now < expires_at <= min(now + 90 * 86400, self.PASS_DEADLINE):
                raise ValueError("pass expiry outside allowed window")
            if db.execute("SELECT count(*) FROM passes WHERE revoked=0 AND expires>?", (now,)).fetchone()[0] >= self.MAX_PASSES:
                raise Denied()
            code = secrets.token_urlsafe(32)
            db.execute("INSERT INTO passes(digest,expires,max_requests) VALUES(?,?,?)",
                       (self._opaque_digest(code), float(expires_at), max_requests))
            return code

    def activate_pass(self, pass_code, installation_id):
        import uuid
        if type(installation_id) is not str:
            raise Denied()
        try:
            if str(uuid.UUID(installation_id)) != installation_id:
                raise ValueError()
        except ValueError:
            raise Denied() from None
        digest = self._opaque_digest(pass_code)
        bearer = self._pass_bearer(pass_code, installation_id)
        bearer_digest = self._digest(bearer)
        with self._transaction() as db:
            now = self._now(db)
            row = db.execute("SELECT expires,max_requests,installation,revoked FROM passes WHERE digest=?", (digest,)).fetchone()
            if row is None or row[0] <= now or row[3]:
                raise Denied()
            if row[2] is not None and row[2] != installation_id:
                raise Denied()
            if row[2] is None:
                if db.execute("SELECT count(*) FROM credentials WHERE revoked=0 AND expires>?", (now,)).fetchone()[0] >= 32:
                    raise Denied()
                db.execute("UPDATE passes SET installation=? WHERE digest=? AND installation IS NULL AND revoked=0",
                           (installation_id, digest))
            db.execute("INSERT OR IGNORE INTO credentials(digest,expires,pass_digest) VALUES(?,?,?)",
                       (bearer_digest, row[0], digest))
            db.execute("INSERT OR IGNORE INTO quotas(day,owner,requests,units) VALUES('9999-12-31',?,0,0)",
                       ("pass:" + digest,))
            return bearer

    def revoke_pass(self, pass_code):
        digest = self._opaque_digest(pass_code)
        with self._transaction() as db:
            changed = db.execute("UPDATE passes SET revoked=1 WHERE digest=? AND revoked=0", (digest,)).rowcount
            db.execute("UPDATE credentials SET revoked=1 WHERE pass_digest=?", (digest,))
            db.execute("UPDATE attempts SET active=0 WHERE credential IN (SELECT digest FROM credentials WHERE pass_digest=?)", (digest,))
            return bool(changed)

    def provision(self):
        with self._transaction() as db:
            now = self._now(db)
            if db.execute("SELECT count(*) FROM credentials WHERE revoked=0").fetchone()[0] >= 32:
                raise Denied()
            bearer = secrets.token_urlsafe(32)
            db.execute("INSERT INTO credentials(digest,expires) VALUES(?,?)", (self._digest(bearer), now + 86400))
            return bearer

    def revoke(self, bearer):
        digest = self._digest(bearer)
        with self._transaction() as db:
            changed = db.execute("UPDATE credentials SET revoked=1 WHERE digest=?", (digest,)).rowcount
            db.execute("UPDATE attempts SET active=0 WHERE credential=?", (digest,))
            return bool(changed)

    def authenticate(self, bearer):
        with self._transaction() as db:
            now = self._now(db)
            self._credential(db, bearer, now)

    def admit(self, bearer, sent_at, request_id):
        with self._transaction() as db:
            now = self._now(db)
            digest, expires, pass_digest = self._credential(db, bearer, now)
            if not 0 <= now - sent_at < 60:
                raise Denied()
            if db.execute("SELECT 1 FROM attempts WHERE id=?", (request_id,)).fetchone():
                raise Denied()
            if db.execute("SELECT count(*) FROM attempts").fetchone()[0] >= 4096:
                raise Denied()
            if db.execute("SELECT count(*) FROM attempts WHERE active=1").fetchone()[0] >= self.limits.concurrent:
                raise Denied()
            if db.execute("SELECT 1 FROM attempts WHERE credential=? AND active=1", (digest,)).fetchone():
                raise Denied()
            recent = db.execute("SELECT count(*) FROM attempts WHERE credential=? AND admitted>?", (digest, now - 60)).fetchone()[0]
            if recent >= self.limits.per_minute:
                raise Denied()
            lifetime_count = db.execute("SELECT lifetime_requests FROM clock WHERE id=1").fetchone()[0]
            if lifetime_count >= self.limits.global_lifetime:
                raise Denied()
            db.execute("UPDATE clock SET lifetime_requests=lifetime_requests+1 WHERE id=1")
            if pass_digest is not None:
                pass_quota = db.execute("SELECT requests FROM quotas WHERE day='9999-12-31' AND owner=?",
                                        ("pass:" + pass_digest,)).fetchone()
                max_requests = db.execute("SELECT max_requests FROM passes WHERE digest=? AND revoked=0", (pass_digest,)).fetchone()
                if pass_quota is None or max_requests is None or pass_quota[0] >= max_requests[0]:
                    raise Denied()
                db.execute("UPDATE quotas SET requests=requests+1,units=units+? WHERE day='9999-12-31' AND owner=?",
                           (self.UNITS, "pass:" + pass_digest))
            day = datetime.fromtimestamp(now, timezone.utc).date().isoformat()
            for owner, max_requests, max_units in ((digest, self.limits.per_day, self.limits.credential_units),
                                                   ("global", self.limits.global_day, self.limits.global_units)):
                row = db.execute("SELECT requests,units FROM quotas WHERE day=? AND owner=?", (day, owner)).fetchone() or (0, 0)
                if row[0] >= max_requests or row[1] + self.UNITS > max_units:
                    raise Denied()
                db.execute("INSERT INTO quotas VALUES(?,?,1,?) ON CONFLICT(day,owner) DO UPDATE SET requests=requests+1, units=units+excluded.units",
                           (day, owner, self.UNITS))
            db.execute("INSERT INTO attempts(id,credential,admitted,expires,lease_expires,active) VALUES(?,?,?,?,?,1)",
                       (request_id, digest, now, expires, min(now + 60, expires)))

    def dispatch(self, bearer, request_id):
        with self._transaction() as db:
            now = self._now(db)
            digest, _, _ = self._credential(db, bearer, now)
            changed = db.execute("UPDATE attempts SET dispatched=1 WHERE id=? AND credential=? AND active=1 AND dispatched=0 AND lease_expires>?",
                                 (request_id, digest, now)).rowcount
            if changed != 1:
                raise Denied()

    def finish(self, bearer, request_id):
        # Cleanup/revocation remain possible during clock rollback; no refund or replay deletion.
        with self._transaction() as db:
            db.execute("UPDATE attempts SET active=0 WHERE id=? AND credential=?", (request_id, self._digest(bearer)))

    def authorize_result(self, bearer, request_id):
        with self._transaction() as db:
            now = self._now(db)
            digest, _, _ = self._credential(db, bearer, now)
            if not db.execute("SELECT 1 FROM attempts WHERE id=? AND credential=? AND active=1 AND dispatched=1 AND lease_expires>?",
                              (request_id, digest, now)).fetchone():
                raise Denied()
