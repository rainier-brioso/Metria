"""Local rehearsal privacy, price arithmetic and network boundary tests."""
import json
import contextlib
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from local_tunnel import ORIGIN_HOST, UsageRecorder, cloudflared_environment, estimated_cost
import local_tunnel
from provider import ProviderFailure
from server import LocalBroker
from store import Store


class LocalTunnelTests(unittest.TestCase):
    def test_startup_diagnostics_never_include_arbitrary_exception_text(self):
        for error, expected in [
            (ValueError("provider key required"), "provider_key_invalid"),
            (ValueError("public authorization check failed"), "https_authorization_check_failed"),
            (json.JSONDecodeError("secret body", "secret body", 0), "https_check_response_not_json"),
            (PermissionError("private path"), "local_state_permission_denied"),
            (TimeoutError("secret endpoint"), "network_timeout"),
            (ValueError("private key"), "configuration_invalid"),
            (RuntimeError("private key"), "startup_failed"),
        ]:
            with self.subTest(expected=expected):
                self.assertEqual(expected, local_tunnel.startup_failure_code(error))

    def test_verified_tool_checks_platform_checksum_and_rejects_symlinks(self):
        for system, machine, expected_path, checksum in [
            ("Windows", "AMD64", local_tunnel.ROOT / ".local-state/tools/cloudflared.exe", local_tunnel.TOOL_HASH),
            ("Linux", "x86_64", Path("/usr/local/bin/cloudflared"), local_tunnel.LINUX_TOOL_HASHES["x86_64"]),
            ("Linux", "aarch64", Path("/usr/local/bin/cloudflared"), local_tunnel.LINUX_TOOL_HASHES["aarch64"]),
        ]:
            with self.subTest(system=system, machine=machine), \
                    patch("local_tunnel.platform.system", return_value=system), \
                    patch("local_tunnel.platform.machine", return_value=machine), \
                    patch.object(Path, "is_symlink", return_value=False), \
                    patch.object(Path, "read_bytes", return_value=b"fixture"), \
                    patch("local_tunnel.hashlib.sha256") as digest:
                digest.return_value.hexdigest.return_value = checksum
                self.assertEqual(expected_path, local_tunnel.verified_tunnel_tool())
                digest.return_value.hexdigest.return_value = "wrong"
                with self.assertRaises(ValueError):
                    local_tunnel.verified_tunnel_tool()
                with patch.object(Path, "is_symlink", return_value=True), self.assertRaises(ValueError):
                    local_tunnel.verified_tunnel_tool()

    def test_unsupported_tunnel_platform_fails_closed(self):
        with patch("local_tunnel.platform.system", return_value="Linux"), \
                patch("local_tunnel.platform.machine", return_value="unknown"), self.assertRaises(ValueError):
            local_tunnel.verified_tunnel_tool()

    def test_uncached_price_matches_real_fixture_usage(self):
        self.assertEqual("0.0013158", estimated_cost({"prompt_tokens": 1761, "completion_tokens": 875}))
        with self.assertRaises(ValueError):
            estimated_cost({"prompt_tokens": True, "completion_tokens": 2})

    def test_tunnel_never_inherits_provider_or_tunnel_secrets(self):
        with patch.dict(os.environ, {"NEBIUS_API_KEY": "private", "TUNNEL_TOKEN": "private",
                                     "TUNNEL_LOGLEVEL": "debug", "CF_API_TOKEN": "private"}):
            env = cloudflared_environment()
        self.assertNotIn("NEBIUS_API_KEY", env)
        self.assertNotIn("TUNNEL_TOKEN", env)
        self.assertNotIn("TUNNEL_LOGLEVEL", env)
        self.assertNotIn("CF_API_TOKEN", env)

    def test_usage_only_no_content_even_on_error(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "usage.jsonl"
            recorder = UsageRecorder(path)
            request = {"request_id": "fictional-id", "messages": ["private input"], "image": b"private photo"}
            answer = {"usage": {"prompt_tokens": 1000, "completion_tokens": 100, "total_tokens": 1100},
                      "reply": "private output"}
            self.assertIs(answer, recorder.wrap(lambda _: answer, "assistant")(request))

            def failure(_):
                raise ValueError("private provider body")

            with self.assertRaises(ValueError):
                recorder.wrap(failure, "vision")(request)
            text = path.read_text()
            self.assertNotIn("private", text)
            rows = [json.loads(line) for line in text.splitlines()]
            self.assertEqual("0.00039", rows[0]["estimated_cost_usd"])
            self.assertIsNone(rows[1]["estimated_cost_usd"])
            self.assertEqual("usage_unknown", rows[1]["status"])

    def test_offline_total_reports_unknown_cost_separately(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory) / ".local-state/nebius-app-test"
            folder.mkdir(parents=True)
            (folder / "usage.jsonl").write_text(
                '{"estimated_cost_usd":"0.0013158"}\n{"estimated_cost_usd":null}\n')
            output = io.StringIO()
            with patch.object(local_tunnel, "ROOT", Path(directory)), patch("sys.argv", ["local_tunnel", "--usage"]), contextlib.redirect_stdout(output):
                local_tunnel.main()
            result = json.loads(output.getvalue())
            self.assertEqual(2, result["recorded_calls"])
            self.assertEqual(1, result["calls_with_unknown_cost"])
            self.assertEqual("0.0013158", result["known_estimate_usd"])
            self.assertFalse(result["billing_exact"])

    def test_rejected_draft_still_records_reported_cost_without_output(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "usage.jsonl"
            error = ProviderFailure("private error", "response_truncated")
            error.usage = {"prompt_tokens": 1761, "completion_tokens": 875, "total_tokens": 2636,
                           "extra": "private output"}
            def failed(_):
                raise error
            output = io.StringIO()
            with contextlib.redirect_stdout(output), self.assertRaises(ProviderFailure):
                UsageRecorder(path).wrap(failed, "vision")({"request_id": "synthetic-id"})
            row = json.loads(path.read_text())
            self.assertEqual("response_truncated", row["diagnostic"])
            self.assertEqual("rejected_with_usage", row["status"])
            self.assertEqual("0.0013158", row["estimated_cost_usd"])
            self.assertNotIn("private", path.read_text() + output.getvalue())

    def test_tunnel_origin_binds_only_loopback_with_host_validation(self):
        with tempfile.TemporaryDirectory() as directory:
            server = LocalBroker(0, Store(Path(directory) / "state.sqlite3"),
                                 public_host=ORIGIN_HOST, loopback_only=True)
            try:
                self.assertEqual("127.0.0.1", server.server_address[0])
                self.assertEqual(ORIGIN_HOST, server.public_host)
            finally:
                server.server_close()


if __name__ == "__main__":
    unittest.main()
