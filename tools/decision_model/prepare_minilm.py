"""Prepare pinned, public MiniLM assets for the debug Android build."""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import tempfile
from urllib.request import Request, urlopen

REPOSITORY = "sentence-transformers/all-MiniLM-L6-v2"
REVISION = "1110a243fdf4706b3f48f1d95db1a4f5529b4d41"
ARTIFACTS = {
    "model_qint8_arm64.onnx": {
        "source": "onnx/model_qint8_arm64.onnx",
        "bytes": 23_026_053,
        "sha256": "4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474",
    },
    "vocab.txt": {
        "source": "vocab.txt",
        "bytes": 231_508,
        "sha256": "07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3",
    },
}


def digest(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def verify(path: Path, metadata: dict[str, object]) -> bool:
    return path.is_file() and path.stat().st_size == metadata["bytes"] and digest(path) == metadata["sha256"]


def fetch(filename: str, cache: Path, metadata: dict[str, object]) -> Path:
    destination = cache / filename
    if verify(destination, metadata):
        return destination
    if destination.exists():
        raise RuntimeError(f"Existing artifact failed pinned checksum; preserve and inspect it: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    url = f"https://huggingface.co/{REPOSITORY}/resolve/{REVISION}/{metadata['source']}"
    request = Request(url, headers={"User-Agent": "Metria-debug-model-preparation/1.0"})
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(dir=cache, prefix=f".{filename}.", delete=False) as temporary:
            temporary_path = Path(temporary.name)
            with urlopen(request, timeout=60) as response:
                shutil.copyfileobj(response, temporary, length=1024 * 1024)
    except BaseException:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)
        raise
    assert temporary_path is not None
    if not verify(temporary_path, metadata):
        temporary_path.unlink(missing_ok=True)
        raise RuntimeError(f"Downloaded artifact failed pinned size/checksum: {filename}")
    os.replace(temporary_path, destination)
    return destination


def install(source: Path, destination: Path, metadata: dict[str, object]) -> None:
    if verify(destination, metadata):
        return
    if destination.exists():
        raise RuntimeError(f"Existing Android asset failed pinned checksum; inspect it first: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_name(destination.name + ".pending")
    try:
        shutil.copyfile(source, temporary)
        if not verify(temporary, metadata):
            raise RuntimeError(f"Copied Android asset failed pinned checksum: {destination.name}")
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache-dir", type=Path, default=Path(".local-state/decision-model/minilm"))
    parser.add_argument(
        "--asset-dir",
        type=Path,
        default=Path("native-android/app/src/debug/assets/decision-model/minilm"),
    )
    args = parser.parse_args()
    cache = args.cache_dir.resolve()
    asset_dir = args.asset_dir.resolve()
    cache.mkdir(parents=True, exist_ok=True)
    for name, metadata in ARTIFACTS.items():
        cached = fetch(name, cache, metadata)
        install(cached, asset_dir / name, metadata)
        print(f"{name}: {metadata['bytes']} bytes sha256={metadata['sha256']}")
    print(f"Pinned revision: {REPOSITORY}@{REVISION}")
    print(f"Debug assets: {asset_dir}")


if __name__ == "__main__":
    main()
