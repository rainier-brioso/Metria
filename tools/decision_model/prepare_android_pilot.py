"""Prepare fixed fictional Android smoke inputs with the pinned official packer.

This is a runtime/parity test, not an Android tokenizer or live-chat adapter.
The temperature is frozen from each reference call; Android does not implement
Von's dynamic calibration in this pilot. No reserved benchmark prompts are used.
"""
from __future__ import annotations

import gc
import json
import threading
from pathlib import Path

import export_von as export
import run_von

CASES = [
    ("read-1", "List the meals I recorded today.", "today_meal_list"),
    ("read-2", "What did I log for meals today?", "today_meal_list"),
    ("read-3", "Show today's saved meals.", "today_meal_list"),
    ("read-4", "Which meals are in my record for today?", "today_meal_list"),
    ("fallback-1", "I just ate two eggs and toast.", "fallback"),
    ("fallback-2", "List the meals I recorded yesterday.", "fallback"),
    ("fallback-3", "What should I eat tomorrow?", "fallback"),
    ("fallback-4", "List today's meals and suggest dinner.", "fallback"),
]
QUESTION = "Choose the operation requested. If the message is not solely a request to list today's saved meals, select fallback."
OPTIONS = ["List meals already recorded today.", "Fallback: adding food, advice, any other date or metric, multiple requests, or unclear request."]


def main() -> None:
    # Check source revision/cleanliness and every artifact before importing Von.
    run_von.load_local_von()
    import numpy as np
    import onnxruntime as ort
    import torch
    from von.models.option_marker import OptionMarkerModel
    from von.backends.option_marker_backend import OptionMarkerBackend, _validate_calibration_map

    calibration = export._load_calibration(run_von.MODEL_DIR / "marker_calibration.json")
    model = OptionMarkerModel(base_model_id=str(run_von.MODEL_DIR), digit_split=calibration["digit_split"])
    weights = torch.load(run_von.MODEL_DIR / "option_marker.pt", map_location="cpu", weights_only=True)
    model.load_state_dict(weights, strict=True)
    del weights
    model.eval().cpu()
    backend = object.__new__(OptionMarkerBackend)
    backend._calib_map = _validate_calibration_map(calibration["calibration_map"])
    backend._default_temp = calibration["temperature"]
    backend._capture_local = threading.local()
    rows = []
    names = ("input_ids", "full_mask", "sliding_mask", "position_ids", "marker_positions")
    for case_id, text, expected in CASES:
        tensors = export._case_inputs(model, text, QUESTION, OPTIONS, torch)
        ids, full, sliding, positions, markers, attention, locations = tensors
        with torch.inference_mode():
            logits = model(input_ids=ids, attention_mask=attention, mask_positions=[locations], independent_options=True)[0]
            temperature = export._temperature(backend, logits, text, 2, model.tokenizer)
        rows.append({"id": case_id, "text": text, "expected": expected,
                     "temperature": temperature, "reference_logits": logits.tolist(),
                     "tensors": {name: {"shape": list(value.shape),
                                        "dtype": "float32" if value.dtype == torch.float32 else "int64",
                                        "data": value.flatten().tolist()}
                                 for name, value in zip(names, tensors[:5])}})
    del model, tensors, logits, backend
    gc.collect()
    opts = ort.SessionOptions()
    opts.intra_op_num_threads = 4
    opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_DISABLE_ALL
    graph_path = run_von.STATE_DIR / "von-onnx/von-option-marker.onnx"
    assert export.sha256(graph_path) == "949c7be11a8a2225999089651c34a7dcc88567d8d0c906a67d99eb45cf5a7482"
    session = ort.InferenceSession(str(graph_path), sess_options=opts, providers=["CPUExecutionProvider"])
    for row in rows:
        inputs = {name: np.array(t["data"], dtype=np.float32 if t["dtype"] == "float32" else np.int64).reshape(t["shape"])
                  for name, t in row["tensors"].items()}
        actual = session.run(["logits"], inputs)[0][0]
        error = float(np.max(np.abs(actual - np.array(row["reference_logits"]))))
        assert error <= .002 and int(np.argmax(actual)) == int(np.argmax(row["reference_logits"])), row["id"]
        row["desktop_onnx_max_logit_error"] = error
    payload = {"scope": "Fixed fictional runtime/parity smoke test; no live tokenization, vault read or Gemma comparison",
               "model_sha256": export.sha256(graph_path), "source_revision": run_von.SOURCE_REVISION,
               "model_revision": run_von.MODEL_REVISION, "question": QUESTION, "options": OPTIONS,
               "threshold": .8, "temperature_scope": "Frozen reference temperature per case; not a dynamic Android calibration port",
               "cases": rows}
    destination = run_von.ROOT / "native-android/app/src/debug/assets/decision-model/pilot.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, separators=(",", ":"), allow_nan=False) + "\n", encoding="utf-8")
    print(json.dumps({"cases": len(rows), "bytes": destination.stat().st_size,
                      "fixture_sha256": export.sha256(destination), "path": str(destination)}))


if __name__ == "__main__":
    main()
