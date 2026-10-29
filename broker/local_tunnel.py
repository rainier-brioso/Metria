"""Temporary HTTPS rehearsal. Optional full response capture requires fictional inputs."""
import argparse
from datetime import datetime, timezone
from decimal import Decimal
import getpass
import hashlib
import http.client
import ipaddress
import json
import os
import platform
from pathlib import Path
import re
import socket
import subprocess
import signal
import threading
import time
import urllib.request
import urllib.error
from urllib.parse import urlsplit

from cloud_assistant import SuperVLAssistantAdapter
from cloud_vision import MODEL, SuperVLVisionAdapter, reported_usage
from provider import DIAGNOSTICS, ProviderAdapter, ProviderConfig, ProviderFailure
from server import LocalBroker, private_directory
from store import Limits, Store

ROOT = Path(__file__).resolve().parent.parent
TOOL_HASH = "db4e4c413612a0916f3e1679d3caaf376f94911952d163028fb726fb2e0516a9"
LINUX_TOOL_HASHES = {
    "x86_64": "e1bc0cf084fd9e590554fe769a86b1db46701c3f847a43d902f7b974a4e16298",
    "aarch64": "72e0d05a67e09da87cf495e409a28c3cc5994dd23fc66a982eebea6a55923b7d",
}
ORIGIN_HOST = "metria-tunnel.invalid"
RATES = {"input_per_million_usd": "0.30", "output_per_million_usd": "0.90"}


def verified_tunnel_tool():
    if platform.system() == "Windows":
        tool, expected = ROOT / ".local-state/tools/cloudflared.exe", TOOL_HASH
    elif platform.system() == "Linux" and platform.machine() in LINUX_TOOL_HASHES:
        tool, expected = Path("/usr/local/bin/cloudflared"), LINUX_TOOL_HASHES[platform.machine()]
    else:
        raise ValueError("unsupported tunnel platform")
    if tool.is_symlink() or hashlib.sha256(tool.read_bytes()).hexdigest() != expected:
        raise ValueError("verified cloudflared 2026.10.5 required")
    return tool


def estimated_cost(usage):
    """Uncached list-price estimate, never an invoice or a spending cap."""
    incoming, outgoing = usage["prompt_tokens"], usage["completion_tokens"]
    if any(type(v) is not int or v < 0 for v in (incoming, outgoing)):
        raise ValueError("invalid usage")
    return str((Decimal(incoming) * Decimal("0.30") + Decimal(outgoing) * Decimal("0.90")) / 1_000_000)


class UsageRecorder:
    def __init__(self, path):
        self.path = path
        self.lock = threading.Lock()

    def wrap(self, adapter, route):
        def call(request):
            started = time.monotonic()
            row = {"time_utc": datetime.now(timezone.utc).isoformat(),
                   "request_id": request["request_id"], "route": route, "model": MODEL,
                   "status": "usage_unknown", "estimated_cost_usd": None}
            try:
                answer = adapter(request)
                row.update(status="validated", usage=answer["usage"],
                           estimated_cost_usd=estimated_cost(answer["usage"]),
                           rates=RATES, estimate_basis="uncached_list_price_2026-10-28")
                return answer
            except ProviderFailure as error:
                diagnostic = error.diagnostic if error.diagnostic in DIAGNOSTICS else "provider_upstream_error"
                row["diagnostic"] = diagnostic
                usage = reported_usage(getattr(error, "usage", None))
                if usage is not None:
                    row.update(status="rejected_with_usage", usage=usage,
                               estimated_cost_usd=estimated_cost(usage), rates=RATES,
                               estimate_basis="uncached_list_price_2026-10-28")
                raise
            finally:
                row["latency_ms"] = round((time.monotonic() - started) * 1000)
                with self.lock:
                    # Bound technical evidence; never evict/reset quota state.
                    if not self.path.is_symlink() and (not self.path.exists() or self.path.stat().st_size < 5_000_000):
                        with self.path.open("a", encoding="utf-8") as handle:
                            handle.write(json.dumps(row, separators=(",", ":")) + "\n")
                print(json.dumps({key: row[key] for key in ("route", "status", "latency_ms", "estimated_cost_usd", "diagnostic") if key in row}),
                      flush=True)
        return call


def cloudflared_environment():
    # The tunnel process must not inherit the provider secret or tunnel settings.
    return {k: v for k, v in os.environ.items()
            if not k.upper().startswith(("NEBIUS_", "TUNNEL_", "CLOUDFLARE_", "CF_"))}


def public_check(url, path="/health", body=None):
    """Validate TLS even when local DNS filters temporary tunnel hostnames."""
    host = urlsplit(url).hostname
    connection = http.client.HTTPSConnection(host, timeout=5)
    alternate_dns = False
    try:
        try:
            connection.connect()
        except socket.gaierror:
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
            query = urllib.request.Request("https://cloudflare-dns.com/dns-query?type=A&name=" + host,
                                           headers={"Accept": "application/dns-json"})
            with opener.open(query, timeout=5) as result:
                answers = json.loads(result.read(8192)).get("Answer", [])
            addresses = [item["data"] for item in answers if item.get("type") == 1
                         and ipaddress.ip_address(item["data"]).is_global]
            if not addresses:
                raise ValueError("public DNS unavailable")
            # Override only address resolution. HTTPSConnection still verifies
            # the original hostname/certificate and uses that hostname as SNI.
            connection._create_connection = lambda address, timeout, source_address=None: socket.create_connection(
                (addresses[0], address[1]), timeout, source_address)
            connection.connect()
            alternate_dns = True
        payload = None if body is None else json.dumps(body).encode("utf-8")
        connection.request("GET" if body is None else "POST", path, payload,
                           {"Content-Type": "application/json"} if payload is not None else {})
        response = connection.getresponse()
        return response.status, json.loads(response.read(2048)), alternate_dns
    finally:
        connection.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--account-verified", action="store_true")
    parser.add_argument("--check", action="store_true", help="HTTPS health rehearsal only; no provider call")
    parser.add_argument("--usage", action="store_true", help="show private local cost estimate totals; no network")
    parser.add_argument("--capture-fictional-responses", action="store_true",
                        help="declare all app inputs fictional and privately retain raw provider responses/errors")
    args = parser.parse_args()
    if args.capture_fictional_responses and (not args.live or args.check or args.usage):
        parser.error("fictional response capture requires --live --account-verified")
    if args.usage:
        if args.live or args.check or args.account_verified:
            parser.error("--usage is a separate offline command")
        path = ROOT / ".local-state/nebius-app-test/usage.jsonl"
        if path.is_symlink() or (path.exists() and path.stat().st_size > 5_100_000):
            raise ValueError("usage evidence unavailable")
        rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()] if path.exists() else []
        print(json.dumps({"recorded_calls": len(rows),
                          "calls_with_unknown_cost": sum(row["estimated_cost_usd"] is None for row in rows),
                          "known_estimate_usd": str(sum((Decimal(row["estimated_cost_usd"]) for row in rows
                                                        if row["estimated_cost_usd"] is not None), Decimal(0))),
                          "billing_exact": False}))
        return
    if args.live == args.check or (args.live and not args.account_verified):
        parser.error("choose --check OR --live --account-verified")
    tool = verified_tunnel_tool()
    # Fail closed when state was previously initialized and its ledger is missing.
    state_path = ROOT / ".local-state/nebius-app-test"
    initialized = (state_path / ".metria-broker-state").exists()
    state = private_directory(state_path)
    db = state / "admission.sqlite3"
    if db.is_symlink() or (initialized and not db.is_file()):
        raise ValueError("missing quota ledger")
    store = Store(db, Limits(global_lifetime=20, global_day=20))
    pass_file = state / "app-test.pass"
    if not pass_file.exists():
        with pass_file.open("x", encoding="ascii") as handle:
            handle.write(store.issue_pass(min(time.time() + 7 * 86400, Store.PASS_DEADLINE), 10))
    elif pass_file.is_symlink() or pass_file.stat().st_size != 43:
        raise ValueError("invalid private pass file")

    provider = vision = assistant = None
    if args.live:
        env = {"NEBIUS_API_KEY": os.environ.get("NEBIUS_API_KEY") or getpass.getpass("Nebius API key (hidden): "),
               "NEBIUS_BASE_URL": "https://api.tokenfactory.nebius.com/v1/", "NEBIUS_MODEL": MODEL}
        provider = ProviderAdapter(ProviderConfig.from_environment(env))
        usage = UsageRecorder(state / "usage.jsonl")
        vision, assistant = SuperVLVisionAdapter(env), SuperVLAssistantAdapter(env)
        if args.capture_fictional_responses:
            from fictional_app_capture import FictionalAppCapture
            capture = FictionalAppCapture(env["NEBIUS_API_KEY"])
            vision, assistant = capture.wrap(vision, "vision"), capture.wrap(assistant, "assistant")
            print("FULL RESPONSE CAPTURE ENABLED: fictional inputs only, including prior chat context.", flush=True)
        vision = usage.wrap(vision, "vision")
        assistant = usage.wrap(assistant, "assistant")
    server = LocalBroker(0, store, provider, public_host=ORIGIN_HOST, loopback_only=True,
                         vision_responder=vision, assistant_responder=assistant,
                         allow_cloud_photos=args.live, allow_cloud_assistant=args.live)
    worker = threading.Thread(target=server.serve_forever, daemon=True)
    worker.start()
    process = None
    try:
        process = subprocess.Popen([str(tool), "tunnel", "--no-autoupdate", "--protocol", "http2",
                                    "--loglevel", "info", "--metrics", "127.0.0.1:0",
                                    "--management-diagnostics=false",
                                    "--url", f"http://127.0.0.1:{server.server_port}",
                                    "--http-host-header", ORIGIN_HOST],
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, encoding="utf-8", errors="replace", env=cloudflared_environment())
        ready = threading.Event()
        address = []

        def read_address():
            for line in process.stdout:
                match = re.search(r"https://[a-z0-9-]+\.trycloudflare\.com\b", line)
                if match and not address:
                    address.append(match.group())
                    ready.set()
            ready.set()

        threading.Thread(target=read_address, daemon=True).start()
        if not ready.wait(45) or not address:
            raise ValueError("tunnel startup failed")
        url = address[0]
        print("Tunnel address assigned: " + url + "; checking HTTPS...", flush=True)
        healthy = False
        failure_kind = "not_ready"
        for _ in range(30):
            try:
                status, body, alternate_dns = public_check(url)
                healthy = status == 200 and body.get("provider_connected") == args.live
                if healthy:
                    break
            except (OSError, ValueError) as error:
                failure_kind = ("http_" + str(error.code) if isinstance(error, urllib.error.HTTPError)
                                else type(getattr(error, "reason", error)).__name__)
            if process.poll() is not None:
                break
            time.sleep(2)
        if not healthy:
            print("HTTPS check unavailable: " + failure_kind, flush=True)
            raise ValueError("HTTPS health check failed")
        print("HTTPS broker verified: " + url, flush=True)
        if alternate_dns:
            print("Local DNS filters this hostname; use mobile data/public DNS on the test phone.", flush=True)
        print("Private pass file: " + str(pass_file), flush=True)
        print("Limits: 10 attempts/pass, 20 total/day and lifetime, 3/min, 1 active/installation.", flush=True)
        print("Fictional inputs only. Ctrl+C closes the public tunnel.", flush=True)
        if args.check:
            if public_check(url, "/v1/assistant", {})[0] != 403 or public_check(
                    url, "/v1/judge/activate", {"pass_code": "invalid", "installation_id": "invalid"})[0] not in (400, 403):
                raise ValueError("public authorization check failed")
            print("CHECK PASSED: no provider inference requested.", flush=True)
            return
        while process.poll() is None:
            time.sleep(0.5)
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(5)
        server.shutdown()
        server.server_close()
        worker.join(2)


def startup_failure_code(error):
    """Fixed diagnostics only: exception messages can contain secrets or content."""
    known = {
        "missing quota ledger": "quota_ledger_missing",
        "invalid private pass file": "private_pass_invalid",
        "provider key required": "provider_key_invalid",
        "verified cloudflared 2026.10.5 required": "tunnel_binary_invalid",
        "tunnel startup failed": "tunnel_startup_failed",
        "HTTPS health check failed": "https_health_check_failed",
        "public authorization check failed": "https_authorization_check_failed",
        "public DNS unavailable": "public_dns_unavailable",
    }
    if type(error) is ValueError and len(error.args) == 1:
        return known.get(error.args[0], "configuration_invalid") if type(error.args[0]) is str else "configuration_invalid"
    if isinstance(error, json.JSONDecodeError):
        return "https_check_response_not_json"
    if isinstance(error, PermissionError):
        return "local_state_permission_denied"
    if isinstance(error, (TimeoutError, socket.timeout)):
        return "network_timeout"
    if isinstance(error, (OSError, http.client.HTTPException)):
        return "local_io_or_network_failed"
    return "startup_failed"


if __name__ == "__main__":
    # Docker stop must close the public tunnel and database server gracefully.
    def stop_requested(_signum, _frame):
        raise KeyboardInterrupt
    signal.signal(signal.SIGTERM, stop_requested)
    try:
        main()
    except KeyboardInterrupt:
        print("Tunnel stopped.")
    except Exception as error:
        raise SystemExit("Local tunnel failed [" + startup_failure_code(error) +
                         "]; no content or credentials logged.") from None
