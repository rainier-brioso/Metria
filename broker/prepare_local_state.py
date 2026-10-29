"""Repair directory ownership for Windows bind mounts before dropping to the broker UID.

Runs only in a short-lived container with CHOWN capability. Never reads files,
recurses, changes host ACLs, creates state, or resets admission counters.
"""
import os
from pathlib import Path
import stat


def prepare(root=Path("/app/.local-state")):
    if root != Path("/app/.local-state") or root.is_symlink() or not root.is_dir():
        raise ValueError("invalid state mount")
    for name in ("nebius-app-test", "nebius-debug-responses"):
        path = root / name
        try:
            metadata = path.lstat()
            if not stat.S_ISDIR(metadata.st_mode):
                raise ValueError("invalid state directory")
            if metadata.st_uid == 10001 and metadata.st_gid == 10001:
                continue
            fd = os.open(path, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
        except FileNotFoundError:
            continue
        try:
            if not stat.S_ISDIR(os.fstat(fd).st_mode):
                raise ValueError("invalid state directory")
            os.fchown(fd, 10001, 10001)
        finally:
            os.close(fd)


if __name__ == "__main__":
    try:
        prepare()
    except Exception:
        raise SystemExit("Local state ownership preparation failed; no state was replaced.") from None
    print("Local state directories prepared; existing pass and quotas preserved.")
