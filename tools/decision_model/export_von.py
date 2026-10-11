#!/usr/bin/env python3
"""Export the pinned Von option-marker model to ONNX and check parity.

All text packing, digit splitting, masks and position ids come from the pinned
Von implementation. The exported graph contains the ModernBERT encoder and the
complete option-marker scorer. Host supplied masks/positions are explicit graph
inputs so independent-option semantics are not frozen during tracing.

This is a desktop feasibility experiment. It downloads nothing and does not
claim Android operator support, latency, memory fit, or production readiness.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import subprocess
import sys
import traceback
from pathlib import Path
from typing import Any


SOURCE_REVISION = "9319ced779f8232ab1d8d3e9c69cdc52ddce77ec"
ARTIFACT_REVISION = "498ceba33390b32cfefaab6422ec380318ba9b99"
DEFAULT_SOURCE = Path(".local-state/decision-model/von-source")
DEFAULT_MODEL = Path(".local-state/decision-model/von-model")
DEFAULT_OUTPUT = Path(".local-state/decision-model/von-onnx")
DEFAULT_MANIFEST = Path(".local-state/decision-model/artifact-manifest.json")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _load_calibration(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {"temperature": 1.0, "calibration_map": None, "independent_options": False, "digit_split": False}
    with path.open(encoding="utf-8") as stream:
        raw = json.load(stream)
    return {
        "temperature": float(raw.get("temperature", 1.0)),
        "calibration_map": raw.get("calibration_map"),
        "independent_options": bool(raw.get("independent_options", False)),
        "digit_split": bool(raw.get("digit_split", False)),
    }


def _probabilities(logits, temperature: float) -> list[float]:
    import torch

    return torch.softmax(logits.float() / max(float(temperature), 1e-4), dim=-1).cpu().tolist()


def _case_inputs(model, state: str, question: str, options: list[str], torch):
    from von.models.option_marker import (
        build_independent_option_masks,
        build_option_invariant_position_ids,
    )

    packed = model.pack_sequence(state, question, options)
    encoded = model.tokenizer(packed, return_tensors="pt", truncation=False)
    input_ids = encoded["input_ids"].to(dtype=torch.long)
    attention_mask = encoded["attention_mask"].to(dtype=torch.long)
    locations = (input_ids[0] == model.mask_token_id).nonzero(as_tuple=True)[0].tolist()
    if len(locations) != len(options):
        raise RuntimeError(f"official packer emitted {len(locations)} markers for {len(options)} options")
    positions = build_option_invariant_position_ids(input_ids, attention_mask, [locations])
    masks = build_independent_option_masks(
        input_ids,
        attention_mask,
        [locations],
        positions,
        getattr(model.encoder.config, "sliding_window", None),
    )
    # Match the pinned backend's ONNX/OpenVINO mask representation: 0 for
    # allowed attention and the finite minimum float for blocked attention.
    minimum = torch.finfo(torch.float32).min
    full = torch.zeros(masks["full_attention"].shape, dtype=torch.float32).masked_fill(
        ~masks["full_attention"], minimum
    )
    sliding = torch.zeros(masks["sliding_attention"].shape, dtype=torch.float32).masked_fill(
        ~masks["sliding_attention"], minimum
    )
    marker_positions = torch.tensor([locations], dtype=torch.long)
    return input_ids, full, sliding, positions, marker_positions, attention_mask, locations


def _temperature(backend, logits, state: str, n_options: int, tokenizer) -> float:
    # Reuse Von's pinned calibration implementation rather than approximate its
    # tokenizer-count and entropy features in this experiment harness.
    return backend._effective_temperature(logits, state, n_options, tokenizer)


def run(args: argparse.Namespace) -> dict[str, Any]:
    source = args.source.resolve()
    model_dir = args.model.resolve()
    output = args.output.resolve()
    manifest_path = args.manifest.resolve()
    output.mkdir(parents=True, exist_ok=True)
    report_path = output / "export-report.json"
    report: dict[str, Any] = {
        "status": "failed",
        "source_revision_expected": SOURCE_REVISION,
        "artifact_revision_expected": ARTIFACT_REVISION,
        "source_path": str(source),
        "model_path": str(model_dir),
        "manifest_path": str(manifest_path),
        "output_path": str(output),
        "device": "cpu",
        "scope": "full independent-options encoder plus option-marker scorer; raw logits output",
        "limitations": [
            "Desktop ONNX Runtime CPU parity only; no Android operator or performance claim.",
            "Calibration remains host-side and must be applied with the pinned Von calibration code.",
            "No quantization is performed.",
        ],
    }
    try:
        required = [model_dir / "config.json", model_dir / "model.safetensors", model_dir / "option_marker.pt"]
        missing = [str(path) for path in required if not path.is_file()]
        if missing:
            raise FileNotFoundError("required local model files missing: " + ", ".join(missing))
        if not manifest_path.is_file():
            raise FileNotFoundError(f"artifact checksum manifest not found: {manifest_path}")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if manifest.get("revision") != ARTIFACT_REVISION:
            raise RuntimeError(
                f"artifact revision mismatch: expected {ARTIFACT_REVISION}, found {manifest.get('revision')}"
            )
        manifest_files = manifest.get("files")
        if not isinstance(manifest_files, dict):
            raise ValueError("artifact manifest has no files checksum map")
        verified_files = {}
        for name, entry in manifest_files.items():
            path = model_dir / name
            if not path.is_file():
                raise FileNotFoundError(f"manifest-listed artifact is missing: {path}")
            actual_size, actual_hash = path.stat().st_size, sha256(path)
            if actual_size != entry.get("bytes") or actual_hash != entry.get("sha256"):
                raise RuntimeError(f"artifact checksum mismatch for {name}")
            verified_files[name] = {"bytes": actual_size, "sha256": actual_hash}
        if not source.is_dir():
            raise FileNotFoundError(f"pinned source directory not found: {source}")
        source_head = subprocess.run(
            ["git", "-C", str(source), "rev-parse", "HEAD"],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
        if source_head != SOURCE_REVISION:
            raise RuntimeError(f"source revision mismatch: expected {SOURCE_REVISION}, found {source_head}")
        source_changes = subprocess.run(
            ["git", "-C", str(source), "status", "--porcelain"],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
        if source_changes:
            raise RuntimeError("pinned source working tree is dirty; refusing an untracked implementation")
        sys.path.insert(0, str((source / "src").resolve()))

        # Explicitly disable hub access before importing/loading any HF code.
        os.environ["HF_HUB_OFFLINE"] = "1"
        os.environ["TRANSFORMERS_OFFLINE"] = "1"
        os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
        import numpy as np
        import onnx
        import onnxruntime as ort
        import torch
        from von.backends.option_marker_backend import OptionMarkerBackend, _validate_calibration_map
        from von.models.option_marker import OptionMarkerModel

        torch.set_num_threads(max(1, args.threads))
        torch.manual_seed(0)
        calibration = _load_calibration(model_dir / "marker_calibration.json")
        if not calibration["independent_options"]:
            raise ValueError(
                "checkpoint calibration does not enable independent_options; this export preserves that mode only"
            )

        model = OptionMarkerModel(base_model_id=str(model_dir), digit_split=calibration["digit_split"])
        state_dict = torch.load(model_dir / "option_marker.pt", map_location="cpu", weights_only=True)
        model.load_state_dict(state_dict, strict=True)
        model.eval().cpu()

        backend = object.__new__(OptionMarkerBackend)
        backend._calib_map = _validate_calibration_map(calibration["calibration_map"])
        backend._default_temp = calibration["temperature"]
        backend._capture_local = __import__("threading").local()

        class CompleteVonGraph(torch.nn.Module):
            def __init__(self, official):
                super().__init__()
                self.encoder = official.encoder
                self.scorer = official.scorer

            def forward(self, input_ids, full_mask, sliding_mask, position_ids, marker_positions):
                encoded = self.encoder(
                    input_ids=input_ids,
                    attention_mask={"full_attention": full_mask, "sliding_attention": sliding_mask},
                    position_ids=position_ids,
                ).last_hidden_state
                gather_positions = marker_positions.unsqueeze(-1).expand(
                    marker_positions.shape[0], marker_positions.shape[1], encoded.shape[-1]
                )
                marker_states = torch.gather(encoded, 1, gather_positions)
                return self.scorer(marker_states)

        graph = CompleteVonGraph(model).eval()
        cases = [
            {
                "name": "short_three_options",
                "state": "Today I logged oatmeal and yogurt for breakfast.",
                "question": "Which saved record should answer the request?",
                "options": ["nutrition summary", "meal list", "latest meal"],
            },
            {
                "name": "long_four_options",
                "state": ("The member asks about yesterday's recorded food. This synthetic state is deliberately longer; "
                          "it contains no real member information. " * 10),
                "question": "Select the most relevant read operation.",
                "options": ["open goals", "list meals", "calculate nutrition", "show latest meal"],
            },
            {
                "name": "reordered_three_options",
                "state": "Today I logged oatmeal and yogurt for breakfast.",
                "question": "Which saved record should answer the request?",
                "options": ["latest meal", "nutrition summary", "meal list"],
            },
        ]

        tensors = [_case_inputs(model, item["state"], item["question"], item["options"], torch) for item in cases]
        # Validate the flat-tensor wrapper against Von's official model call
        # before tracing. This catches mask conversion or marker-gather drift
        # before ONNX export can obscure where a mismatch began.
        wrapper_results = []
        for item, values in zip(cases, tensors):
            ids, full, sliding, positions, marker_positions, attention, locations = values
            with torch.inference_mode():
                official = model(
                    input_ids=ids,
                    attention_mask=attention,
                    mask_positions=[locations],
                    independent_options=True,
                )[0]
                wrapped = graph(ids, full, sliding, positions, marker_positions)[0]
            err = float(torch.max(torch.abs(official - wrapped)).item())
            same_choice = int(torch.argmax(official).item()) == int(torch.argmax(wrapped).item())
            wrapper_results.append({
                "name": item["name"],
                "max_abs_logit_error": err,
                "same_choice": same_choice,
            })
            if not same_choice or err > args.max_logit_error:
                raise RuntimeError(
                    f"pre-export wrapper parity failed for {item['name']}: "
                    f"max_logit_error={err:g}, same_choice={same_choice}"
                )
        ex = tensors[0]
        example_inputs = ex[:5]
        dynamic_axes = {
            "input_ids": {0: "batch", 1: "sequence"},
            "full_mask": {0: "batch", 2: "sequence", 3: "sequence"},
            "sliding_mask": {0: "batch", 2: "sequence", 3: "sequence"},
            "position_ids": {0: "batch", 1: "sequence"},
            "marker_positions": {0: "batch", 1: "options"},
            "logits": {0: "batch", 1: "options"},
        }
        onnx_path = output / "von-option-marker.onnx"
        torch.onnx.export(
            graph,
            example_inputs,
            str(onnx_path),
            input_names=["input_ids", "full_mask", "sliding_mask", "position_ids", "marker_positions"],
            output_names=["logits"],
            dynamic_axes=dynamic_axes,
            opset_version=args.opset,
            do_constant_folding=True,
            export_params=True,
            external_data=True,
            dynamo=False,
        )

        exported = onnx.load(str(onnx_path), load_external_data=True)
        onnx.checker.check_model(exported)
        session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
        case_results = []
        max_logit_error = 0.0
        max_probability_error = 0.0
        for item, values in zip(cases, tensors):
            ids, full, sliding, positions, marker_positions, _attention, locations = values
            with torch.inference_mode():
                official = model(
                    input_ids=ids,
                    attention_mask=_attention,
                    mask_positions=[locations],
                    independent_options=True,
                )[0]
                expected_probs = _probabilities(
                    official,
                    _temperature(backend, official, item["state"], len(item["options"]), model.tokenizer),
                )
            ort_logits = session.run(
                ["logits"],
                {
                    "input_ids": ids.cpu().numpy(),
                    "full_mask": full.cpu().numpy(),
                    "sliding_mask": sliding.cpu().numpy(),
                    "position_ids": positions.cpu().numpy(),
                    "marker_positions": marker_positions.cpu().numpy(),
                },
            )[0]
            actual = torch.from_numpy(ort_logits[0])
            err = float(torch.max(torch.abs(official - actual)).item())
            actual_probs = _probabilities(
                actual,
                _temperature(backend, actual, item["state"], len(item["options"]), model.tokenizer),
            )
            prob_err = max(abs(a - b) for a, b in zip(expected_probs, actual_probs))
            same_choice = int(torch.argmax(official).item()) == int(torch.argmax(actual).item())
            if not same_choice:
                raise RuntimeError(f"PyTorch and ONNX selected different options for {item['name']}")
            max_logit_error = max(max_logit_error, err)
            max_probability_error = max(max_probability_error, prob_err)
            case_results.append({
                "name": item["name"],
                "sequence_length": int(ids.shape[1]),
                "option_count": len(item["options"]),
                "options": item["options"],
                "pytorch_logits": official.float().tolist(),
                "onnx_logits": actual.float().tolist(),
                "max_abs_logit_error": err,
                "max_abs_calibrated_probability_error": prob_err,
                "same_choice": same_choice,
                "pytorch_calibrated_probabilities": expected_probs,
                "onnx_calibrated_probabilities": actual_probs,
            })

        report.update({
            "status": "passed" if max_logit_error <= args.max_logit_error and max_probability_error <= args.max_probability_error else "parity_failed",
            "source_revision": SOURCE_REVISION,
            "artifact_revision": ARTIFACT_REVISION,
            "calibration": calibration,
            "verified_manifest_files": verified_files,
            "pre_export_wrapper_parity": wrapper_results,
            "onnx_file": {"path": str(onnx_path), "bytes": onnx_path.stat().st_size, "sha256": sha256(onnx_path)},
            "external_data_files": [
                {"path": str(path), "bytes": path.stat().st_size, "sha256": sha256(path)}
                for path in output.iterdir() if path.is_file() and path.name != onnx_path.name and path.name != "export-report.json"
            ],
            "versions": {"torch": torch.__version__, "onnx": onnx.__version__, "onnxruntime": ort.__version__},
            "parity_tolerances": {"max_abs_logit_error": args.max_logit_error, "max_abs_calibrated_probability_error": args.max_probability_error},
            "max_abs_logit_error": max_logit_error,
            "max_abs_calibrated_probability_error": max_probability_error,
            "cases": case_results,
            "graph_inputs": [entry.name for entry in session.get_inputs()],
            "graph_outputs": [entry.name for entry in session.get_outputs()],
            "graph_operator_types": sorted({node.op_type for node in exported.graph.node}),
        })
    except Exception as exc:  # Keep an actionable failure report even for exporter/operator gaps.
        report["error"] = f"{type(exc).__name__}: {exc}"
        report["traceback"] = traceback.format_exc()

    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--opset", type=int, default=18)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--max-logit-error", type=float, default=2e-3)
    parser.add_argument("--max-probability-error", type=float, default=2e-4)
    args = parser.parse_args()
    result = run(args)
    print(json.dumps({key: result.get(key) for key in (
        "status", "error", "onnx_file", "max_abs_logit_error", "max_abs_calibrated_probability_error"
    )}, indent=2))
    return 0 if result["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
