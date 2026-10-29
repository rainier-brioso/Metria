"""Preflight uses hidden input only; no requests, accounting mutations or secret output."""
import contextlib
import getpass
import io
import json
import os
import unittest
from unittest.mock import patch

from probe import main


class KeyPreflightTests(unittest.TestCase):
    def run_check(self, key):
        output = io.StringIO()
        with patch.dict(os.environ, {}, clear=True), patch("probe.getpass.getpass", return_value=key), \
                patch("probe.ProviderAdapter", side_effect=AssertionError("network forbidden")), \
                patch("probe.Store", side_effect=AssertionError("state mutation forbidden")), \
                contextlib.redirect_stdout(output):
            status = main(["--check-key"])
        self.assertNotIn(key, output.getvalue())
        return status, json.loads(output.getvalue())

    def test_hidden_key_check_accepts_syntax_without_network_or_state(self):
        status, result = self.run_check("fictional-secret-key")
        self.assertEqual(0, status)
        self.assertTrue(result["valid"])
        self.assertFalse(result["network_requested"])

    def test_hidden_key_check_rejects_spaces_without_network_or_secret_echo(self):
        status, result = self.run_check("fictional secret key")
        self.assertEqual(2, status)
        self.assertEqual("configuration_invalid", result["error"])
        self.assertFalse(result["network_requested"])

    def test_live_configuration_error_identifies_stage_before_state_or_network(self):
        output = io.StringIO()
        with patch.dict(os.environ, {}, clear=True), patch("probe.getpass.getpass", return_value="invalid key"), \
                patch("probe.ProviderAdapter", side_effect=AssertionError("network forbidden")), \
                patch("probe.Store", side_effect=AssertionError("state mutation forbidden")), \
                contextlib.redirect_stdout(output):
            status = main(["--live", "--account-verified"])
        self.assertEqual(2, status)
        result = json.loads(output.getvalue())
        self.assertEqual("configuration_invalid", result["diagnostic"])
        self.assertNotIn("invalid key", output.getvalue())


if __name__ == "__main__":
    unittest.main()
