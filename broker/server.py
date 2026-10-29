"""Bounded demo broker; synthetic by default, live mode requires explicit opt-in."""
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
import time
from datetime import datetime, timezone
from decimal import InvalidOperation
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from contract import MAX_BODY, MODEL, parse_request, synthetic_response
from cloud_vision import MAX_REQUEST_BYTES as MAX_VISION_BODY, SuperVLVisionAdapter, parse_vision_request
from cloud_assistant import MAX_REQUEST_BYTES as MAX_ASSISTANT_BODY, SuperVLAssistantAdapter, parse_assistant_request
from provider import ProviderAdapter, ProviderConfig, ProviderFailure
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

    def __init__(self, port, store, responder=None, *, public_host=None, vision_responder=None,
                 allow_cloud_photos=False, assistant_responder=None, allow_cloud_assistant=False,
                 loopback_only=False):
        self.store = store
        self.responder = responder
        self.mode = responder.mode if responder is not None else "synthetic"
        self.models = responder.models if responder is not None else frozenset({MODEL})
        self.slots = threading.BoundedSemaphore(8)
        if public_host is not None:
            import re
            if not re.fullmatch(r"[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?", public_host) or "." not in public_host:
                raise ValueError("public host")
        self.public_host = public_host
        self.vision_responder = vision_responder
        self.allow_cloud_photos = bool(allow_cloud_photos)
        if self.allow_cloud_photos and self.vision_responder is None:
            raise ValueError("vision adapter required")
        self.assistant_responder = assistant_responder
        self.allow_cloud_assistant = bool(allow_cloud_assistant)
        if self.allow_cloud_assistant and self.assistant_responder is None:
            raise ValueError("assistant adapter required")
        self.activation_lock = threading.Lock()
        self.activation_times = []
        super().__init__(("0.0.0.0" if public_host and not loopback_only else "127.0.0.1", port), Handler)

    def activation_allowed(self):
        # Global bound does not trust caller-supplied proxy IP headers.
        with self.activation_lock:
            now = time.monotonic()
            self.activation_times = [stamp for stamp in self.activation_times if stamp > now - 60]
            if len(self.activation_times) >= 30:
                return False
            self.activation_times.append(now)
            return True

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
        allowed = ((self.server.public_host, self.server.public_host + ":443") if self.server.public_host else
                   (f"127.0.0.1:{self.server.server_port}", f"localhost:{self.server.server_port}"))
        transport = (not self.server.public_host or self.headers.get_all("X-Forwarded-Proto", []) == ["https"])
        return (transport and len(host) == 1 and host[0] in allowed
                and not self.headers.get_all("Origin") and not self.headers.get_all("Transfer-Encoding"))

    def read_json(self, limit=1024):
        from contract import _pairs
        length = self.headers.get_all("Content-Length", [])
        if (len(length) != 1 or not length[0].isascii() or not length[0].isdigit()
                or not 0 < int(length[0]) <= limit or self.headers.get_all("Content-Type", []) != ["application/json"]):
            raise ValueError("body headers")
        raw = self.rfile.read(int(length[0]))
        if len(raw) != int(length[0]):
            raise ValueError("body length")
        value = json.loads(raw.decode("utf-8"), object_pairs_hook=_pairs,
                           parse_constant=lambda _: (_ for _ in ()).throw(ValueError("nonfinite")))
        if type(value) is not dict:
            raise ValueError("body")
        return value

    def do_GET(self):
        if self.path == "/health" and self.local_headers():
            self.reply(200, {"mode": self.server.mode, "provider_connected": self.server.mode == "nebius"})
        elif self.path == "/v1/demo/scenarios" and self.local_headers():
            from demo import SCENARIOS
            self.reply(200, {"fictional": True, "scenarios": list(SCENARIOS)})
        else:
            self.reply(404, {"error": "request_rejected"})

    def do_POST(self):
        bearer = request_id = None
        admitted = False
        try:
            if self.path not in ("/v1/assistance", "/v1/demo", "/v1/judge/activate", "/v1/vision", "/v1/assistant") or not self.local_headers():
                self.reply(400, {"error": "request_rejected"})
                return
            if self.path == "/v1/judge/activate":
                if not self.server.activation_allowed():
                    self.reply(429, {"error": "activation_rate_limited"})
                    return
                value = self.read_json()
                if set(value) != {"pass_code", "installation_id"}:
                    raise ValueError("activation fields")
                token = self.server.store.activate_pass(value["pass_code"], value["installation_id"])
                self.reply(200, {"credential": token,
                                 "fictional_only": not (self.server.allow_cloud_photos or self.server.allow_cloud_assistant)})
                return
            auth = self.headers.get_all("Authorization", [])
            if len(auth) != 1 or not auth[0].startswith("Bearer "):
                raise Denied()
            bearer = auth[0][7:]
            self.server.store.authenticate(bearer)
            if self.path == "/v1/vision":
                if not self.server.allow_cloud_photos or self.server.vision_responder is None:
                    raise ValueError("cloud photos disabled")
                length = self.headers.get_all("Content-Length", [])
                sent = self.headers.get_all("X-Metria-Sent-At", [])
                if (len(length) != 1 or not length[0].isascii() or not length[0].isdigit()
                        or not 0 < int(length[0]) <= MAX_VISION_BODY
                        or self.headers.get_all("Content-Type") != ["application/json"]
                        or len(sent) != 1 or not sent[0].isascii() or not sent[0].isdigit() or len(sent[0]) > 12):
                    raise ValueError("vision headers")
                raw = self.rfile.read(int(length[0]))
                if len(raw) != int(length[0]):
                    raise ValueError("vision body")
                request = parse_vision_request(raw)
                request_id = request["request_id"]
                self.server.store.admit(bearer, int(sent[0]), request_id)
                admitted = True
                self.server.store.dispatch(bearer, request_id)
                answer = self.server.vision_responder(request)
                if type(answer) is not dict or answer.get("request_id") != request_id or answer.get("model") != "nvidia/Nemotron-3.5-Super-VL":
                    raise ProviderFailure("invalid_provider_output")
                self.server.store.authorize_result(bearer, request_id)
                self.server.store.finish(bearer, request_id)
                admitted = False
                self.reply(200, answer)
                return
            if self.path == "/v1/assistant":
                if not self.server.allow_cloud_assistant or self.server.assistant_responder is None:
                    raise ValueError("cloud assistant disabled")
                length = self.headers.get_all("Content-Length", [])
                sent = self.headers.get_all("X-Metria-Sent-At", [])
                if (len(length) != 1 or not length[0].isascii() or not length[0].isdigit()
                        or not 0 < int(length[0]) <= MAX_ASSISTANT_BODY
                        or self.headers.get_all("Content-Type") != ["application/json"]
                        or len(sent) != 1 or not sent[0].isascii() or not sent[0].isdigit() or len(sent[0]) > 12):
                    raise ValueError("assistant headers")
                raw = self.rfile.read(int(length[0]))
                if len(raw) != int(length[0]):
                    raise ValueError("assistant body")
                request = parse_assistant_request(raw)
                request_id = request["request_id"]
                self.server.store.admit(bearer, int(sent[0]), request_id)
                admitted = True
                self.server.store.dispatch(bearer, request_id)
                answer = self.server.assistant_responder(request)
                if (type(answer) is not dict or answer.get("request_id") != request_id
                        or answer.get("model") != "nvidia/Nemotron-3.5-Super-VL"
                        or answer.get("provider") != "nebius"):
                    raise ProviderFailure("invalid_provider_output")
                self.server.store.authorize_result(bearer, request_id)
                self.server.store.finish(bearer, request_id)
                admitted = False
                self.reply(200, answer)
                return
            if self.path == "/v1/demo":
                from demo import build_sample, expected_selection
                from uuid import UUID
                value = self.read_json()
                if set(value) != {"scenario", "request_id"} or type(value["request_id"]) is not str or str(UUID(value["request_id"])) != value["request_id"]:
                    raise ValueError("demo fields")
                sent = self.headers.get_all("X-Metria-Sent-At", [])
                if len(sent) != 1 or not sent[0].isascii() or not sent[0].isdigit() or len(sent[0]) > 12:
                    raise ValueError("timestamp")
                today = datetime.fromtimestamp(self.server.store.clock(), timezone.utc).date()
                request, raw = build_sample(value["scenario"], next(iter(self.server.models)), request_id=value["request_id"], today=today)
                request_id = request["request_id"]
                self.server.store.admit(bearer, int(sent[0]), request_id)
                admitted = True
                self.server.store.dispatch(bearer, request_id)
                answer = self.server.responder(request, raw) if self.server.responder is not None else synthetic_response(request)
                if answer != expected_selection(request, value["scenario"]):
                    raise ProviderFailure("invalid_provider_output")
                self.server.store.authorize_result(bearer, request_id)
                self.server.store.finish(bearer, request_id)
                admitted = False
                self.reply(200, {"fictional": True, "mode": self.server.mode, "scenario": value["scenario"], "selection": answer})
                return
            # Live/hosted service never accepts caller-supplied wellness content.
            if self.server.mode == "nebius" or self.server.public_host:
                raise ValueError("sample scenarios only")
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
    parser = argparse.ArgumentParser(description="Metria fictional demo broker; synthetic unless --live")
    parser.add_argument("command", choices=("serve", "provision", "revoke", "issue-pass", "revoke-pass"))
    parser.add_argument("--state-dir", type=Path, required=True)
    parser.add_argument("--credential-file", type=Path)
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8765")))
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--account-verified", action="store_true")
    parser.add_argument("--allow-cloud-assistant", "--allow-cloud-photos", dest="allow_cloud_assistant", action="store_true",
                        help="enable separately consented bounded member chat and meal-photo endpoints")
    parser.add_argument("--public-host")
    parser.add_argument("--persistent-state-confirmed", action="store_true")
    parser.add_argument("--expires-at", help="Pass expiry ISO-8601 with timezone; at most 90 days")
    parser.add_argument("--max-requests", type=int, default=50)
    args = parser.parse_args()
    initialized = (args.state_dir / ".metria-broker-state").exists()
    if args.public_host and (not args.persistent_state_confirmed or not initialized or args.command != "serve"):
        raise ValueError("hosted service requires initialized persistent state")
    if args.live and (not args.account_verified or args.command != "serve"):
        raise ValueError("verify account and spending controls before live serving")
    if args.allow_cloud_assistant and (not args.live or not args.account_verified or args.command != "serve"):
        raise ValueError("cloud assistant requires live verified provider service")
    directory = private_directory(args.state_dir)
    database = directory / "admission.sqlite3"
    if database.is_symlink() or (initialized and not database.is_file()):
        raise ValueError("missing or linked durable state")
    store = Store(database)
    if args.command == "serve":
        if not 0 <= args.port <= 65535:
            raise ValueError("port")
        responder = ProviderAdapter(ProviderConfig.from_environment()) if args.live else None
        vision_responder = SuperVLVisionAdapter() if args.allow_cloud_assistant else None
        assistant_responder = SuperVLAssistantAdapter() if args.allow_cloud_assistant else None
        server = LocalBroker(args.port, store, responder, public_host=args.public_host,
                             vision_responder=vision_responder,
                             allow_cloud_photos=args.allow_cloud_assistant,
                             assistant_responder=assistant_responder,
                             allow_cloud_assistant=args.allow_cloud_assistant)
        if server.mode == "synthetic" and not args.public_host:
            print(f"Synthetic broker: http://127.0.0.1:{server.server_port}; provider disabled", flush=True)
        else:
            print(f"Broker mode: {server.mode}; fictional demo; port {server.server_port}", flush=True)
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
    if args.command in ("provision", "issue-pass"):
        bearer = None
        # Exclusive creation protects an existing credential from replacement.
        with target.open("x", encoding="ascii") as handle:
            try:
                os.chmod(target, 0o600)
                if args.command == "issue-pass":
                    if not args.expires_at:
                        raise ValueError("pass expiry required")
                    expiry = datetime.fromisoformat(args.expires_at)
                    if expiry.tzinfo is None:
                        raise ValueError("timezone required")
                    bearer = store.issue_pass(expiry.timestamp(), args.max_requests)
                else:
                    bearer = store.provision()
                handle.write(bearer)
            except BaseException:
                if bearer is not None:
                    (store.revoke_pass if args.command == "issue-pass" else store.revoke)(bearer)
                handle.close()
                target.unlink()
                raise
        print("Pass issued to private file." if args.command == "issue-pass" else "Credential provisioned to private file; expires after 24 hours.")
    else:
        if target.stat().st_size > 100:
            raise ValueError("credential file")
        (store.revoke_pass if args.command == "revoke-pass" else store.revoke)(target.read_text(encoding="ascii").strip())
        target.unlink()
        print("Credential revoked; local credential file removed.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, sqlite3.Error, Denied, subprocess.SubprocessError):
        raise SystemExit("Broker operation failed; no content or credentials logged.")
