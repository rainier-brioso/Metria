"""Opt-in, one-request live Nemotron probe over invented data only."""
import argparse
from datetime import datetime, timezone
import getpass
import json
import os
from pathlib import Path
import sqlite3
import time
import warnings

from demo import SCENARIOS, build_sample, expected_selection
from debug_capture import CaptureFailure, DebugCapture
from provider import ProviderAdapter, ProviderConfig, ProviderFailure
from server import private_directory
from store import Denied, Limits, Store

DEFAULT_MODEL = "nvidia/Nemotron-3_5-Lightning"
DEFAULT_ENDPOINT = "https://api.tokenfactory.nebius.com/v1/"


def _environment_config(environment, key_override=None):
    """Parse only the three approved fields; never print their values."""
    key = key_override if key_override is not None else environment.get("NEBIUS_API_KEY", "")
    return ProviderConfig(environment.get("NEBIUS_BASE_URL", DEFAULT_ENDPOINT),
                          environment.get("NEBIUS_MODEL", DEFAULT_MODEL), key)


def _print(value):
    print(json.dumps(value, sort_keys=True, separators=(",", ":")))


def _capture_path(capture):
    return str(capture.path) if capture is not None and capture.path is not None else None


def _read_key():
    if os.environ.get("NEBIUS_API_KEY"):
        return os.environ["NEBIUS_API_KEY"]
    with warnings.catch_warnings():
        warnings.simplefilter("error", getpass.GetPassWarning)
        return getpass.getpass("Nebius Token Factory development key: ")


def main(argv=None):
    parser = argparse.ArgumentParser(description="One bounded live Nemotron probe using code-owned fictional data")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="validate configuration shape; no network request")
    mode.add_argument("--check-key", action="store_true", help="hidden key/configuration syntax check only; no network or state changes")
    mode.add_argument("--live", action="store_true", help="send exactly one fictional request to Token Factory")
    parser.add_argument("--scenario", choices=SCENARIOS, default="weight")
    parser.add_argument("--state-dir", type=Path, default=Path(".local-state/nebius-live-probe"))
    parser.add_argument("--account-verified", action="store_true",
                        help="confirm you checked account credits, expiry, model access and spending bound")
    parser.add_argument("--debug-response", action="store_true",
                        help="save the bounded raw response from this fictional live probe locally")
    args = parser.parse_args(argv)

    if args.debug_response and not args.live:
        parser.error("--debug-response requires --live")

    if args.check:
        missing = [name for name in ("NEBIUS_API_KEY",) if not os.environ.get(name)]
        try:
            _environment_config(os.environ, key_override=os.environ.get("NEBIUS_API_KEY") or "configuration-check-only")
        except ValueError:
            _print({"mode": "offline-check", "valid": False,
                    "key_configured": not missing, "error": "configuration_invalid",
                    "network_requested": False})
            return 2
        _print({"mode": "offline-check", "valid": True, "key_configured": not missing,
                "network_requested": False})
        return 0

    if args.check_key:
        try:
            _environment_config(os.environ, key_override=_read_key())
            _print({"mode": "offline-key-check", "valid": True, "network_requested": False})
            return 0
        except (ValueError, OSError):
            _print({"mode": "offline-key-check", "valid": False,
                    "error": "configuration_invalid", "network_requested": False})
            return 2
        except (EOFError, KeyboardInterrupt, getpass.GetPassWarning):
            _print({"mode": "offline-key-check", "valid": False,
                    "error": "probe_cancelled", "network_requested": False})
            return 2

    if not args.account_verified:
        _print({"mode": "live", "validated": False, "error": "account_checks_required"})
        return 2
    store = bearer = request_id = None
    admitted = False
    capture = None
    dispatched = False
    started = None
    stage = "configuration"
    try:
        key = _read_key()
        config = _environment_config(os.environ, key_override=key)
        stage = "sample"
        day = datetime.now(timezone.utc).date()
        request, preview = build_sample(args.scenario, config.model, today=day)
        expected = expected_selection(request, args.scenario)
        if args.debug_response:
            stage = "debug_capture"
            capture = DebugCapture(args.scenario, config.model, request["request_id"], key)
        # private_directory creates its marker on first use, so record initialization
        # before that call or a brand-new state directory appears pre-initialized.
        initialized = (args.state_dir / ".metria-broker-state").exists()
        stage = "state_directory"
        directory = private_directory(args.state_dir)
        stage = "state_integrity"
        db = directory / "admission.sqlite3"
        if db.is_symlink() or (initialized and not db.is_file()):
            raise ValueError("probe state integrity")
        # Isolated short-lived state directory: ten lifetime live attempts max.
        stage = "state_database"
        store = Store(db, Limits(global_day=10, global_lifetime=10))
        stage = "state_admission"
        bearer = store.provision()
        request_id = request["request_id"]
        now = int(time.time())
        store.admit(bearer, now, request_id)
        admitted = True
        store.dispatch(bearer, request_id)
        dispatched = True
        usage = {}
        started = time.monotonic()
        adapter = ProviderAdapter(config, today=lambda: day)
        if capture is None:
            selection = adapter(request, preview, on_usage=usage.update)
        else:
            def record_response(body, status, truncated=False):
                try:
                    capture.on_response(body, status, truncated)
                except CaptureFailure:
                    raise ProviderFailure("debug_capture_write_failed") from None
            selection = adapter(request, preview, on_usage=usage.update, on_response=record_response)
        stage = "state_completion"
        elapsed_ms = round((time.monotonic() - started) * 1000)
        if selection != expected:
            raise ProviderFailure("unexpected_demo_selection", "response_selection_expectation_mismatch")
        store.authorize_result(bearer, request_id)
        store.finish(bearer, request_id)
        admitted = False
        if capture is not None:
            capture.finish("validated", latency_ms=elapsed_ms)
        result = {"mode": "nebius", "model": config.model, "scenario": args.scenario,
                  "latency_ms": elapsed_ms, "validated": True, "usage": usage}
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 0
    except CaptureFailure as error:
        if capture is not None:
            capture.abort()
        result = {"mode": "nebius", "scenario": args.scenario, "validated": False,
                  "error": str(error)}
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 2
    except Denied:
        if capture is not None:
            try:
                capture.finish("provider_error", "probe_budget_or_state_rejected")
            except CaptureFailure:
                pass
        result = {"mode": "nebius", "scenario": args.scenario, "validated": False,
                  "error": "probe_budget_or_state_rejected"}
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 2
    except ProviderFailure as error:
        if capture is not None:
            if dispatched and started is not None:
                try:
                    capture.finish("rejected" if error.diagnostic and error.diagnostic.startswith("response_") else "provider_error",
                                   error.diagnostic or (str(error) if str(error).startswith("debug_capture_") else None),
                                   round((time.monotonic() - started) * 1000))
                except CaptureFailure:
                    pass
            else:
                try:
                    capture.finish("provider_error", str(error), None)
                except CaptureFailure:
                    pass
        result = {"mode": "nebius", "scenario": args.scenario, "validated": False, "error": str(error)}
        if error.diagnostic is not None:
            result["diagnostic"] = error.diagnostic
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 2
    except (ValueError, OSError, sqlite3.Error):
        if capture is not None:
            if dispatched and started is not None:
                try:
                    capture.finish("provider_error", stage + "_invalid", round((time.monotonic() - started) * 1000))
                except CaptureFailure:
                    pass
            else:
                try:
                    capture.finish("provider_error", stage + "_invalid")
                except CaptureFailure:
                    pass
        result = {"mode": "nebius", "scenario": args.scenario, "validated": False,
                  "error": "configuration_or_probe_state_invalid", "diagnostic": stage + "_invalid"}
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 2
    except (EOFError, KeyboardInterrupt, getpass.GetPassWarning):
        if capture is not None:
            if dispatched and started is not None:
                try:
                    capture.finish("provider_error", "probe_cancelled", round((time.monotonic() - started) * 1000))
                except CaptureFailure:
                    pass
            else:
                try:
                    capture.finish("provider_error", "probe_cancelled")
                except CaptureFailure:
                    pass
        result = {"mode": "nebius", "scenario": args.scenario, "validated": False,
                  "error": "probe_cancelled"}
        if capture is not None:
            result["debug_file"] = _capture_path(capture)
        _print(result)
        return 2
    finally:
        if store is not None and bearer is not None:
            if admitted and request_id is not None:
                store.finish(bearer, request_id)
            store.revoke(bearer)


if __name__ == "__main__":
    raise SystemExit(main())
