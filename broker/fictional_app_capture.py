"""Explicit local fictional-data rehearsal only; never enabled by hosted serving."""
import time
import traceback

from debug_capture import CaptureFailure, DebugCapture
from provider import ProviderFailure


class FictionalAppCapture:
    def __init__(self, provider_key):
        self.__key = provider_key

    def wrap(self, adapter, route):
        if route not in ("vision", "assistant"):
            raise ValueError("capture route")

        def call(request):
            # Reserve/write before invoking the adapter. Full disk/retention cap
            # fails closed rather than spending credits without debug evidence.
            try:
                capture = DebugCapture("fictional_app_" + route, "nvidia/Nemotron-3.5-Super-VL",
                                       request["request_id"], self.__key)
                capture.on_response(b"", None)
            except CaptureFailure:
                print("Fictional capture unavailable/full; provider request was not sent.", flush=True)
                raise
            started = time.monotonic()
            errors = []
            outcome, diagnostic, usage = "provider_error", None, None

            def record_error(error):
                text = "".join(traceback.format_exception(type(error), error, error.__traceback__))
                errors.append({"type": type(error).__name__, "traceback": text[:65_536],
                               "truncated": len(text) > 65_536})

            try:
                result = adapter(request, on_response=capture.on_response, on_error=record_error)
                outcome, usage = "validated", result.get("usage")
                return result
            except Exception as error:
                record_error(error)
                if isinstance(error, ProviderFailure):
                    diagnostic, usage = error.diagnostic, getattr(error, "usage", None)
                    outcome = "rejected" if error.args == ("invalid_provider_output",) else "provider_error"
                raise
            finally:
                try:
                    capture.record_probe_result({"fictional_data_declared": True, "route": route,
                                                 "errors": errors, "usage": usage})
                    capture.finish(outcome, diagnostic, round((time.monotonic() - started) * 1000))
                except CaptureFailure:
                    print("Fictional capture write failed; provider work may already be billed.", flush=True)
                    raise
                # Never print raw responses, exceptions or credentials to the terminal.
                print("Fictional response capture: " + str(capture.path), flush=True)
        return call
