"""Private opt-in raw capture for fictional desktop probes/local app rehearsals."""
import base64
import csv
import io
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import uuid
from datetime import datetime, timezone

MAX_CAPTURE_BYTES = 65_536
MAX_CAPTURE_FILES = 20
CAPTURE_DIR = Path(__file__).resolve().parent.parent / ".local-state" / "nebius-debug-responses"


class CaptureFailure(Exception):
    """A sanitized local capture error."""


def _secure_directory(path):
    path = Path(path)
    for parent in (path, *path.parents):
        if parent.is_symlink() or getattr(parent, "is_junction", lambda: False)():
            raise CaptureFailure("debug_capture_unavailable")
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    if os.name == "nt":
        try:
            who = subprocess.run(["whoami", "/user", "/fo", "csv", "/nh"], capture_output=True,
                                 text=True, check=True).stdout
            sid = next(csv.reader(io.StringIO(who)))[1]
            if not re.fullmatch(r"S-[0-9-]+", sid):
                raise CaptureFailure("debug_capture_unavailable")
            subprocess.run(["icacls", str(path), "/inheritance:r", "/grant:r", f"*{sid}:(OI)(CI)F"],
                           capture_output=True, check=True)
        except (OSError, subprocess.SubprocessError, IndexError):
            raise CaptureFailure("debug_capture_unavailable") from None
    else:
        os.chmod(path, 0o700)
        if path.stat().st_mode & 0o077:
            raise CaptureFailure("debug_capture_unavailable")


def _redaction_spellings(secret):
    spellings = {secret, json.dumps(secret, ensure_ascii=True)[1:-1]}
    # ProviderConfig accepts ASCII keys. Also recognize unicode-escaped spellings of
    # each character, including slash/quote/backslash variants in JSON fragments.
    spellings.add("".join("\\u%04x" % ord(ch) for ch in secret))
    return sorted((s for s in spellings if s), key=len, reverse=True)


def redact_body(raw, secret):
    # Sanitize complete JSON string tokens after decoding their escapes, even when
    # the surrounding response is malformed or has duplicate object keys.
    def redact_token(match):
        token = match.group(0)
        try:
            value = json.loads(token.decode("utf-8"))
        except (UnicodeError, json.JSONDecodeError):
            return token
        return json.dumps("[REDACTED]", ensure_ascii=True).encode("ascii") if secret in value else token
    raw = re.sub(rb'"(?:\\.|[^"\\])*"', redact_token, raw)
    for spelling in _redaction_spellings(secret):
        raw = raw.replace(spelling.encode("utf-8"), b"[REDACTED]")
    return raw


class DebugCapture:
    """Reserve an exclusive artifact before dispatch; never accepts an output path."""
    def __init__(self, scenario, model, request_id, secret):
        self.scenario = scenario
        self.model = model
        self.request_id = request_id
        self.__secret = secret
        self.__path = None
        self.__lock = None
        self.__data = {"request_id": request_id, "scenario": scenario, "model": model,
                       "created_at": datetime.now(timezone.utc).isoformat(),
                       "body_limit_bytes": MAX_CAPTURE_BYTES, "body_encoding": "base64",
                       "http_status": None, "response_body_base64": "", "truncated": False,
                       "outcome": None, "diagnostic": None, "latency_ms": None}
        try:
            _secure_directory(CAPTURE_DIR)
            lock_path = CAPTURE_DIR / ".capture.lock"
            lock_fd = os.open(lock_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
            self.__lock = lock_path
            os.close(lock_fd)
            files = list(CAPTURE_DIR.glob("*.json"))
            if len(files) >= MAX_CAPTURE_FILES:
                raise CaptureFailure("debug_capture_limit_reached")
            for _ in range(3):
                path = CAPTURE_DIR / (str(uuid.uuid4()) + ".json")
                try:
                    fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
                    with os.fdopen(fd, "w", encoding="utf-8") as handle:
                        handle.write("{}")
                    self.__path = path
                    break
                except FileExistsError:
                    continue
            if self.__path is None:
                raise CaptureFailure("debug_capture_unavailable")
            self._release_lock()
        except CaptureFailure:
            self._release_lock()
            raise
        except (OSError, ValueError):
            self._release_lock()
            raise CaptureFailure("debug_capture_unavailable") from None

    def _write(self):
        if self.__path is None:
            raise CaptureFailure("debug_capture_unavailable")
        encoded = json.dumps(self.__data, ensure_ascii=True, sort_keys=True, separators=(",", ":"))
        temp = self.__path.with_suffix(".tmp")
        try:
            fd = os.open(temp, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
            with os.fdopen(fd, "w", encoding="utf-8") as handle:
                handle.write(encoded)
            os.replace(temp, self.__path)
        except (OSError, ValueError):
            try:
                temp.unlink(missing_ok=True)
            except OSError:
                pass
            raise CaptureFailure("debug_capture_write_failed") from None

    def on_response(self, body, status, truncated=False):
        if type(body) is not bytes or len(body) > MAX_CAPTURE_BYTES:
            raise CaptureFailure("debug_capture_write_failed")
        self.__data["http_status"] = status if type(status) is int else None
        if truncated and self.__secret:
            longest = max(len(s.encode("utf-8")) for s in _redaction_spellings(self.__secret))
            body = body[:max(0, len(body) - longest + 1)]
        body = redact_body(body, self.__secret)
        self.__data["response_text"] = body.decode("utf-8", errors="replace")
        self.__data["response_body_base64"] = base64.b64encode(body).decode("ascii")
        self.__data["truncated"] = bool(truncated)
        self._write()

    def record_fictional_request(self, body, image, manifest, endpoint):
        """Desktop vision probe only: retain its code-owned synthetic input, never headers."""
        if (type(body) is not bytes or len(body) > 12 * 1024 * 1024
                or type(image) is not bytes or len(image) > 8 * 1024 * 1024
                or manifest.get("synthetic") is not True
                or hashlib.sha256(image).hexdigest() != manifest.get("sha256")):
            raise CaptureFailure("debug_capture_write_failed")
        try:
            image_path = self.__path.with_suffix(".png")
            fd = os.open(image_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
            with os.fdopen(fd, "wb") as handle:
                handle.write(image)
            self.__data["request_body_text"] = redact_body(body, self.__secret).decode("utf-8")
            self.__data["endpoint"] = endpoint
            self.__data["fixture"] = manifest
            self.__data["image_file"] = str(image_path)
            self._write()
        except (OSError, UnicodeError, ValueError):
            raise CaptureFailure("debug_capture_write_failed") from None

    def record_probe_result(self, result):
        self.__data["probe_result"] = json.loads(redact_body(json.dumps(result).encode(), self.__secret))
        self._write()

    def finish(self, outcome, diagnostic=None, latency_ms=None):
        self.__data["outcome"] = outcome if outcome in ("prepared", "validated", "rejected", "provider_error") else "provider_error"
        allowed = {"response_not_json", "response_envelope_invalid", "response_model_mismatch",
                   "response_choices_invalid", "response_choice_invalid", "response_truncated",
                   "response_finish_reason_invalid", "response_message_invalid", "response_tool_call",
                   "response_refusal", "response_usage_invalid", "response_usage_limit",
                   "response_content_missing", "response_content_type_invalid", "response_content_json_invalid",
                   "response_selection_schema_invalid", "response_selection_version_invalid",
                   "response_selection_identity_mismatch", "response_selection_language_mismatch",
                   "response_selection_sections_invalid", "response_selection_guidance_invalid",
                   "response_selection_expectation_mismatch",
                   "response_content_type_header_invalid", "response_content_length_invalid",
                   "response_content_length_mismatch", "response_body_too_large", "provider_request_rejected",
                   "provider_authentication_rejected", "provider_rate_limited", "provider_upstream_error",
                   "provider_http_rejected", "provider_unavailable", "provider_timeout", "provider_network_error", "provider_cancelled",
                   "configuration_invalid", "probe_cancelled"}
        allowed.update({"debug_capture_unavailable", "debug_capture_limit_reached",
                        "response_food_draft_invalid",
                        "debug_capture_write_failed", "probe_budget_or_state_rejected",
                        "state_directory_invalid", "state_integrity_invalid", "state_database_invalid",
                        "state_admission_invalid", "state_completion_invalid"})
        self.__data["diagnostic"] = diagnostic if type(diagnostic) is str and diagnostic in allowed else None
        self.__data["latency_ms"] = latency_ms if type(latency_ms) is int and latency_ms >= 0 else None
        self._write()
        self._release_lock()

    def abort(self):
        """Remove a reservation only if it is still the untouched empty placeholder."""
        try:
            if self.__path is not None and self.__path.read_text(encoding="utf-8") == "{}":
                self.__path.unlink()
        except OSError:
            pass
        self._release_lock()

    def _release_lock(self):
        if self.__lock is not None:
            try:
                self.__lock.unlink()
            except OSError:
                pass
            self.__lock = None

    @property
    def path(self):
        return self.__path
