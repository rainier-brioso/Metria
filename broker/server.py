"""Localhost broker. CLI stays synthetic; tests may inject a fake-provider adapter."""
import argparse
import csv
import io
import json
import os
from pathlib import Path
import socket
import sqlite3
import subprocess
import threading
from datetime import datetime, timezone
from decimal import InvalidOperation
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from contract import MAX_BODY, MODEL, parse_request, synthetic_response
from provider import ProviderFailure
from store import Denied, Store


def private_directory(path):
    path = Path(path).absolute()
    if any(parent.is_symlink() or getattr(parent, "is_junction", lambda: False)() for parent in (path, *path.parents)):
        raise ValueError("linked state path")
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    marker = path / ".metria-broker-state"
    if not marker.exists() and any(path.iterdir()):
        raise ValueError("state directory must be dedicated and empty on first use")
    if marker.is_symlink() or (marker.exists() and marker.read_text(encoding="ascii") != "1"):
        raise ValueError("state directory marker")
    if os.name == "nt":
        # Restrict to the current Windows SID, without putting the SID or output in logs.
        result = subprocess.run(["whoami", "/user", "/fo", "csv", "/nh"], capture_output=True, text=True, check=True)
        sid = next(csv.reader(io.StringIO(result.stdout)))[1]
        if not sid.startswith("S-1-") or any(c not in "S0123456789-" for c in sid):
            raise ValueError("operator identity")
        subprocess.run(["icacls", str(path), "/inheritance:r", "/grant:r", f"*{sid}:(OI)(CI)F"],
                       capture_output=True, check=True)
    else:
        os.chmod(path, 0o700)
    if not marker.exists():
        with marker.open("x", encoding="ascii") as handle:
            handle.write("1")
    return path


class LocalBroker(ThreadingHTTPServer):
    daemon_threads = True
    request_queue_size = 8
    allow_reuse_address = False

    def __init__(self, port, store, responder=None):
        self.store = store
        self.responder = responder
        self.mode = responder.mode if responder is not None else "synthetic"
        self.models = responder.models if responder is not None else frozenset({MODEL})
        self.slots = threading.BoundedSemaphore(8)
        super().__init__(("127.0.0.1", port), Handler)

    def get_request(self):
        conn, address = super().get_request()
        conn.settimeout(5)
        return conn, address

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except BaseException:
            self.slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.slots.release()

    def handle_error(self, request, client_address):
        pass  # No exception/traceback can expose headers, credentials, question or answer.


class Handler(BaseHTTPRequestHandler):
    server_version = "MetriaLocalBroker"
    sys_version = ""

    def log_message(self, *_):
        pass

    def send_error(self, code, message=None, explain=None):
        self.reply(code, {"error": "request_rejected"})

    def reply(self, status, value):
        payload = json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Metria-Mode", self.server.mode)
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True
        self.wfile.write(payload)

    def local_headers(self):
        host = self.headers.get_all("Host", [])
        return (len(host) == 1 and host[0] in (f"127.0.0.1:{self.server.server_port}", f"localhost:{self.server.server_port}")
                and not self.headers.get_all("Origin") and not self.headers.get_all("Transfer-Encoding"))

    def do_GET(self):
        if self.path == "/health" and self.local_headers():
            self.reply(200, {"mode": self.server.mode, "provider_connected": self.server.mode == "nebius"})
        else:
            self.reply(404, {"error": "request_rejected"})

    def do_POST(self):
        bearer = request_id = None
        admitted = False
        try:
            if self.path != "/v1/assistance" or not self.local_headers():
                self.reply(400, {"error": "request_rejected"})
                return
            auth = self.headers.get_all("Authorization", [])
            if len(auth) != 1 or not auth[0].startswith("Bearer "):
                raise Denied()
            bearer = auth[0][7:]
            self.server.store.authenticate(bearer)
            length = self.headers.get_all("Content-Length", [])
            sent = self.headers.get_all("X-Metria-Sent-At", [])
            content_type = self.headers.get_all("Content-Type", [])
            if len(length) != 1 or not length[0].isascii() or not length[0].isdigit() or not 0 < int(length[0]) <= MAX_BODY:
                raise ValueError("length")
            if len(sent) != 1 or not sent[0].isascii() or not sent[0].isdigit() or len(sent[0]) > 12:
                raise ValueError("timestamp")
            if content_type != ["application/json"]:
                raise ValueError("content type")
            raw = self.rfile.read(int(length[0]))
            if len(raw) != int(length[0]):
                raise ValueError("body length")
            today = datetime.fromtimestamp(self.server.store.clock(), timezone.utc).date()
            request = parse_request(raw, today, self.server.models)
            request_id = request["request_id"]
            self.server.store.admit(bearer, int(sent[0]), request_id)
            admitted = True
            self.server.store.dispatch(bearer, request_id)
            answer = self.server.responder(request, raw) if self.server.responder is not None else synthetic_response(request)
            self.server.store.authorize_result(bearer, request_id)
            # Work is complete: retire the lease before announcing success. Replay/quota remain.
            self.server.store.finish(bearer, request_id)
            admitted = False
            self.reply(200, answer)
        except Denied:
            self.reply(403, {"error": "authorization_or_limit_rejected"})
        except ProviderFailure:
            self.reply(502, {"error": "provider_response_unavailable"})
        except (ValueError, TypeError, KeyError, UnicodeError, InvalidOperation, RecursionError):
            self.reply(400, {"error": "request_rejected"})
        except (sqlite3.Error, OSError):
            self.reply(503, {"error": "service_unavailable"})
        finally:
            if admitted:
                self.server.store.finish(bearer, request_id)


def main():
    parser = argparse.ArgumentParser(description="Localhost synthetic Metria broker; no Nebius calls")
    parser.add_argument("command", choices=("serve", "provision", "revoke"))
    parser.add_argument("--state-dir", type=Path, required=True)
    parser.add_argument("--credential-file", type=Path)
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    initialized = (args.state_dir / ".metria-broker-state").exists()
    directory = private_directory(args.state_dir)
    database = directory / "admission.sqlite3"
    if database.is_symlink() or (initialized and not database.is_file()):
        raise ValueError("missing or linked durable state")
    store = Store(database)
    if args.command == "serve":
        if not 0 <= args.port <= 65535:
            raise ValueError("port")
        server = LocalBroker(args.port, store)
        print(f"Synthetic broker: http://127.0.0.1:{server.server_port}; provider disabled", flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            server.server_close()
        return
    if args.credential_file is None:
        raise ValueError("credential file required")
    target = args.credential_file.absolute()
    # Credentials can only be written/read inside this restricted state directory.
    if target.parent.resolve() != directory.resolve() or target.is_symlink():
        raise ValueError("credential path")
    if args.command == "provision":
        bearer = None
        # Exclusive creation protects an existing credential from replacement.
        with target.open("x", encoding="ascii") as handle:
            try:
                os.chmod(target, 0o600)
                bearer = store.provision()
                handle.write(bearer)
            except BaseException:
                if bearer is not None:
                    store.revoke(bearer)
                handle.close()
                target.unlink()
                raise
        print("Credential provisioned to private file; expires after 24 hours.")
    else:
        if target.stat().st_size > 100:
            raise ValueError("credential file")
        store.revoke(target.read_text(encoding="ascii").strip())
        target.unlink()
        print("Credential revoked; local credential file removed.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, sqlite3.Error, Denied, subprocess.SubprocessError):
        raise SystemExit("Broker operation failed; no content or credentials logged.")
