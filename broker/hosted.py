"""Entrypoint behind an HTTPS reverse proxy, with an initialized persistent volume."""
import os
import sys

from server import main


def arguments(environment):
    host = environment.get("METRIA_PUBLIC_HOST", "")
    state = environment.get("METRIA_STATE_DIR", "/state")
    if not host or environment.get("METRIA_PERSISTENT_STATE_CONFIRMED") != "yes":
        raise ValueError("public hostname and persistent state confirmation required")
    args = ["serve", "--state-dir", state, "--public-host", host,
            "--persistent-state-confirmed"]
    live = environment.get("METRIA_LIVE", "0")
    if live not in ("0", "1"):
        raise ValueError("invalid execution mode")
    if live == "1":
        if environment.get("METRIA_ACCOUNT_VERIFIED") != "yes":
            raise ValueError("account verification required")
        args += ["--live", "--account-verified"]
    cloud_assistant = environment.get("METRIA_ALLOW_CLOUD_ASSISTANT", environment.get("METRIA_ALLOW_CLOUD_PHOTOS", "no"))
    if cloud_assistant not in ("no", "yes"):
        raise ValueError("invalid cloud assistant setting")
    if cloud_assistant == "yes":
        if live != "1":
            raise ValueError("cloud assistant requires verified live provider mode")
        args += ["--allow-cloud-assistant"]
    return args


if __name__ == "__main__":
    try:
        sys.argv = [sys.argv[0], *arguments(os.environ)]
        main()
    except Exception:
        raise SystemExit("Hosted broker startup failed; verify mode, persistent state and configuration.") from None
