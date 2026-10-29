import contextlib
import getpass
import io
import json
import os
import tempfile
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import patch

from contract import parse_request
from demo import SCENARIOS, build_sample, expected_selection
from provider import ProviderFailure, parse_completion
from probe import main
from store import Denied, Limits, Store


MODEL = "nvidia/nvidia-nemotron-3-nano-30b-a3b"


class DemoScenarioTests(unittest.TestCase):
    def test_all_scenarios_are_valid_fictional_wire_requests(self):
        self.assertEqual(("weight", "nutrition", "combined", "empty", "general"), SCENARIOS)
        for scenario in SCENARIOS:
            with self.subTest(scenario=scenario):
                request, preview = build_sample(scenario, MODEL, today=date(2026, 10, 27))
                self.assertEqual(request, parse_request(preview, date(2026, 10, 27), frozenset({MODEL})))
                self.assertEqual("English", request["language"])
                self.assertEqual("2026-10-21", request["period"]["start"])
                self.assertEqual("2026-10-27", request["period"]["end"])
                self.assertEqual({"schema_version", "request_id", "provider", "model", "language",
                                  "period", "question", *[key for key in ("weight", "nutrition") if key in request]},
                                 set(request))
                self.assertEqual(expected_selection(request, scenario)["sections"],
                                 [key for key in ("weight", "nutrition") if key in request])
                self.assertNotIn("real", preview.decode().lower())

    def test_invented_summary_values_preserve_uncertainty_and_empty_coverage(self):
        _, weight_bytes = build_sample("weight", MODEL, today=date(2026, 10, 27))
        weight = json.loads(weight_bytes)
        self.assertEqual([70.0, 69.5, -0.5], [weight["weight"]["first_kg"],
                                              weight["weight"]["latest_kg"], weight["weight"]["change_kg"]])
        _, nutrition_bytes = build_sample("nutrition", MODEL, today=date(2026, 10, 27))
        nutrition = json.loads(nutrition_bytes)["nutrition"]
        self.assertEqual((3, 1, 1, 1), tuple(nutrition[key] for key in
                                             ("recorded_meals", "complete_meals", "partial_meals", "missing_meals")))
        self.assertEqual("recorded_estimates_not_total_intake", nutrition["interpretation"])
        _, empty_bytes = build_sample("empty", MODEL, today=date(2026, 10, 27))
        empty = json.loads(empty_bytes)["weight"]
        self.assertEqual(0, empty["count"])
        self.assertIsNone(empty["latest_kg"])

    def test_scenario_and_model_are_bounded_and_expected_selection_rejects_mismatch(self):
        for args in (("custom-prompt", MODEL), ("weight", "openai/gpt")):
            with self.subTest(args=args), self.assertRaises(ValueError):
                build_sample(args[0], args[1], today=date(2026, 10, 27))
        request, _ = build_sample("weight", MODEL, today=date(2026, 10, 27))
        with self.assertRaises(ValueError):
            expected_selection(request, "nutrition")


class ProbeCliTests(unittest.TestCase):
    def invoke(self, argv, environment=None):
        output = io.StringIO()
        with patch.dict(os.environ, environment or {}, clear=True), contextlib.redirect_stdout(output):
            code = main(argv)
        return code, json.loads(output.getvalue())

    def test_check_is_offline_and_does_not_print_secret(self):
        code, result = self.invoke(["--check"], {"NEBIUS_API_KEY": "never-print-this"})
        self.assertEqual(0, code)
        self.assertTrue(result["valid"])
        self.assertFalse(result["network_requested"])
        self.assertNotIn("never-print-this", json.dumps(result))

    def test_check_validates_configured_key_without_echoing_it(self):
        code, result = self.invoke(["--check"], {"NEBIUS_API_KEY": "bad key"})
        self.assertEqual(2, code)
        self.assertEqual("configuration_invalid", result["error"])
        self.assertNotIn("bad key", json.dumps(result))

    def test_live_is_explicitly_gated_before_configuration_or_network(self):
        code, result = self.invoke(["--live"])
        self.assertEqual(2, code)
        self.assertEqual("account_checks_required", result["error"])
        self.assertNotIn("question", result)

    def _run_mock_live(self, state_dir, deny_admission=False, adapter_calls=None):
        class MockAdapter:
            def __init__(self, *_args, **_kwargs):
                self.mode = "mock"

            def __call__(self, request, _preview, on_usage=None):
                if adapter_calls is not None:
                    adapter_calls.append(request["request_id"])
                if on_usage:
                    on_usage({"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15})
                return expected_selection(request, "weight")

        def local_private_directory(path):
            path = Path(path)
            marker = path / ".metria-broker-state"
            path.mkdir(parents=True, exist_ok=True)
            if not marker.exists():
                if any(path.iterdir()):
                    raise ValueError("state directory must be dedicated")
                marker.write_text("1", encoding="ascii")
            return path

        output = io.StringIO()
        environment = {"NEBIUS_BASE_URL": "https://api.tokenfactory.us-central1.nebius.com/v1/",
                       "NEBIUS_MODEL": MODEL}
        admission_patch = patch("probe.Store.admit", side_effect=Denied()) if deny_admission else contextlib.nullcontext()
        with patch.dict(os.environ, environment, clear=True), \
             patch("probe.getpass.getpass", return_value="secret-never-output"), \
             patch("probe.ProviderAdapter", MockAdapter), \
             patch("probe.private_directory", side_effect=local_private_directory), \
             admission_patch, \
             contextlib.redirect_stdout(output):
            code = main(["--live", "--account-verified", "--state-dir", str(state_dir)])
        return code, json.loads(output.getvalue())

    def test_first_live_run_initializes_state_and_reuses_budget_without_leaking_temp_credentials(self):
        with tempfile.TemporaryDirectory() as root:
            state_dir = Path(root) / "state"
            for expected_requests in (1, 2):
                code, result = self._run_mock_live(state_dir)
                self.assertEqual(0, code, result)
                self.assertTrue(result["validated"])
                self.assertEqual("nvidia", result["model"].split("/", 1)[0])
                self.assertEqual({"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
                                 result["usage"])
                self.assertNotIn("secret-never-output", json.dumps(result))
                store = Store(state_dir / "admission.sqlite3", Limits(global_day=10, global_lifetime=10))
                with store._connection() as db:
                    self.assertEqual(expected_requests,
                                     db.execute("SELECT lifetime_requests FROM clock WHERE id=1").fetchone()[0])
                    self.assertEqual(0, db.execute("SELECT count(*) FROM credentials WHERE revoked=0").fetchone()[0])

    def test_initialized_state_without_database_fails_closed(self):
        with tempfile.TemporaryDirectory() as root:
            state_dir = Path(root) / "state"
            state_dir.mkdir()
            (state_dir / ".metria-broker-state").write_text("1", encoding="ascii")
            code, result = self._run_mock_live(state_dir)
            self.assertEqual(2, code)
            self.assertEqual("configuration_or_probe_state_invalid", result["error"])
            self.assertFalse((state_dir / "admission.sqlite3").exists())

    def test_temporary_credential_is_revoked_when_admission_is_denied(self):
        with tempfile.TemporaryDirectory() as root:
            state_dir = Path(root) / "state"
            code, result = self._run_mock_live(state_dir, deny_admission=True)
            self.assertEqual(2, code)
            self.assertEqual("probe_budget_or_state_rejected", result["error"])
            store = Store(state_dir / "admission.sqlite3", Limits(global_day=10, global_lifetime=10))
            with store._connection() as db:
                self.assertEqual(0, db.execute("SELECT count(*) FROM credentials WHERE revoked=0").fetchone()[0])

    def test_initial_probe_lifetime_cap_blocks_before_provider_call(self):
        with tempfile.TemporaryDirectory() as root:
            state_dir = Path(root) / "state"
            adapter_calls = []
            for _ in range(10):
                code, result = self._run_mock_live(state_dir, adapter_calls=adapter_calls)
                self.assertEqual(0, code, result)
            self.assertEqual(10, len(adapter_calls))
            code, result = self._run_mock_live(state_dir, adapter_calls=adapter_calls)
            self.assertEqual(2, code)
            self.assertEqual("probe_budget_or_state_rejected", result["error"])
            self.assertEqual(10, len(adapter_calls))

    def test_echoing_password_prompt_fails_closed(self):
        with patch.dict(os.environ, {}, clear=True), \
             patch("probe.getpass.getpass", side_effect=getpass.GetPassWarning("echoing prompt")):
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = main(["--live", "--account-verified"])
        self.assertEqual(2, code)
        result = json.loads(output.getvalue())
        self.assertEqual("probe_cancelled", result["error"])


    def test_usage_is_reported_only_after_validated_output(self):
        request, _ = build_sample("weight", MODEL, today=date(2026, 10, 27))
        content = json.dumps(expected_selection(request, "weight"))
        envelope = {"model": MODEL, "choices": [{"index": 0, "finish_reason": "stop",
                     "message": {"role": "assistant", "content": content}}],
                    "usage": {"prompt_tokens": 100, "completion_tokens": 30, "total_tokens": 130}}
        events = []
        selection = parse_completion(json.dumps(envelope).encode(), request, events.append)
        self.assertEqual(expected_selection(request, "weight"), selection)
        self.assertEqual([envelope["usage"]], events)
        envelope["choices"][0]["message"]["content"] = "{}"
        with self.assertRaises(ProviderFailure):
            parse_completion(json.dumps(envelope).encode(), request, events.append)
        self.assertEqual(1, len(events))


if __name__ == "__main__":
    unittest.main()
