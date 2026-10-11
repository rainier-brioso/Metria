"""Download only public pinned Von artifacts into an ignored experiment folder."""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import os

REPOSITORY = "wfzyx/von"
REVISION = "498ceba33390b32cfefaab6422ec380318ba9b99"
FILES = ["config.json", "model.safetensors", "option_marker.pt", "tokenizer.json",
         "tokenizer_config.json", "marker_calibration.json", "calibration.json", "README.md"]


def digest(path: Path) -> str:
    checksum = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            checksum.update(chunk)
    return checksum.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(".local-state/decision-model"))
    args = parser.parse_args()
    root = args.root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
    os.environ["HF_HOME"] = str(root / "hf-cache")
    os.environ["HF_HUB_DISABLE_XET"] = "1"
    from huggingface_hub import snapshot_download
    destination = root / "von-model"
    snapshot_download(repo_id=REPOSITORY, revision=REVISION, local_dir=destination,
                      allow_patterns=FILES, max_workers=2)
    required = set(FILES) - {"calibration.json", "README.md"}
    missing = [name for name in required if not (destination / name).is_file()]
    if missing:
        raise RuntimeError(f"Incomplete pinned snapshot: {missing}")
    manifest = {
        "repository": REPOSITORY, "revision": REVISION,
        "files": {name: {"bytes": (destination / name).stat().st_size,
                          "sha256": digest(destination / name)}
                  for name in FILES if (destination / name).is_file()},
        "packages": {name: importlib.metadata.version(name)
                     for name in ("torch", "transformers", "von-sdk", "onnx", "onnxruntime",
                                  "huggingface-hub", "tokenizers", "numpy")},
    }
    (root / "artifact-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"revision": REVISION, "file_count": len(manifest["files"]),
                      "total_bytes": sum(row["bytes"] for row in manifest["files"].values())}))


if __name__ == "__main__":
    main()
