#!/usr/bin/env python3
"""Score frozen decision-model predictions against synthetic Metria fixtures.

This tool does not call models and does not measure latency. Prediction files
must be produced and frozen separately by a model adapter.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent
SETS = {"dev": ROOT / "fixtures" / "dev.jsonl", "locked": ROOT / "fixtures" / "locked.jsonl"}
BACKENDS = ("rules", "von", "gemma")
INTENTS = {"nutrition_summary", "meal_list", "latest_meal", "draft_meal", "general_food", "medical_safety", "ambiguous", "unsupported"}
PERIODS = {"today", "yesterday", "last_7_days", "none"}
METRICS = {"calories", "protein", "none"}


def load_fixtures(path: Path) -> list[dict[str, Any]]:
    rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    validate_fixtures(rows)
    return rows


def validate_fixtures(rows: list[dict[str, Any]]) -> None:
    if len(rows) != 120:
        raise ValueError(f"expected 120 fixtures, got {len(rows)}")
    ids: set[str] = set()
    eligible: dict[str, int] = {"nutrition": 0, "meal_list": 0, "latest_meal": 0}
    negatives = 0
    for row in rows:
        required = {"id", "split", "family", "text", "eligible", "expected"}
        if not required <= row.keys():
            raise ValueError(f"fixture missing keys: {required - row.keys()}")
        if row["id"] in ids or not row["text"].strip():
            raise ValueError(f"duplicate id or empty text: {row.get('id')}")
        ids.add(row["id"])
        exp = row["expected"]
        if not {"intent", "period", "metric"} <= exp.keys():
            raise ValueError(f"{row['id']}: expected intent, period and metric")
        if exp["intent"] not in INTENTS or exp["period"] not in PERIODS or exp["metric"] not in METRICS:
            raise ValueError(f"{row['id']}: invalid expected route")
        if row["eligible"]:
            if row["family"] not in eligible or exp["intent"] not in {"nutrition_summary", "meal_list", "latest_meal"}:
                raise ValueError(f"{row['id']}: eligible case outside supported family")
            eligible[row["family"]] += 1
        else:
            negatives += 1
            if exp["intent"] in {"nutrition_summary", "meal_list", "latest_meal"}:
                raise ValueError(f"{row['id']}: negative fixture has fast-path intent")
    if set(eligible.values()) != {20} or negatives != 60:
        raise ValueError(f"expected 20 eligible per family and 60 negatives; got {eligible}, {negatives}")


def wilson(errors: int, n: int, z: float = 1.959963984540054) -> tuple[float, float] | None:
    if n == 0:
        return None
    p = errors / n
    d = 1 + z * z / n
    center = (p + z * z / (2 * n)) / d
    margin = z * math.sqrt((p * (1 - p) + z * z / (4 * n)) / n) / d
    lower = max(0.0, center - margin)
    return (0.0 if lower < 1e-12 else lower), min(1.0, center + margin)


def load_predictions(path: Path) -> dict[str, dict[str, Any]]:
    data: dict[str, dict[str, Any]] = {}
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        row = json.loads(line)
        if not isinstance(row, dict):
            raise ValueError(f"prediction line {line_no}: row must be an object")
        if not isinstance(row.get("id"), str) or not row["id"].strip() or row["id"] in data:
            raise ValueError(f"prediction line {line_no}: missing or duplicate id")
        error = row.get("error")
        if error is not None:
            if not isinstance(error, str) or not error.strip() or "prediction" in row:
                raise ValueError(f"prediction line {line_no}: error must be non-empty text and cannot include prediction")
        else:
            pred = row.get("prediction")
            if not isinstance(pred, dict) or not {"intent", "period", "metric", "confidence"} <= pred.keys():
                raise ValueError(f"prediction line {line_no}: require prediction intent/period/metric/confidence or error")
            if pred["intent"] not in INTENTS | {"abstain"} or pred["period"] not in PERIODS or pred["metric"] not in METRICS:
                raise ValueError(f"prediction line {line_no}: invalid prediction labels")
            if isinstance(pred["confidence"], bool) or not isinstance(pred["confidence"], (int, float)) or not math.isfinite(pred["confidence"]) or not 0 <= pred["confidence"] <= 1:
                raise ValueError(f"prediction line {line_no}: confidence must be in [0,1]")
        data[row["id"]] = row
    return data


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_metadata(path: Path, backend: str, split: str, threshold: float, fixture_hash: str) -> dict[str, Any]:
    metadata = json.loads(path.read_text(encoding="utf-8"))
    required = {"backend", "split", "code_revision", "model_revision", "weights_revision", "tokenizer_revision", "calibration_revision", "threshold", "fixture_sha256", "timing_scope"}
    if not isinstance(metadata, dict) or required - metadata.keys():
        raise ValueError(f"{path}: metadata missing {sorted(required - metadata.keys()) if isinstance(metadata, dict) else sorted(required)}")
    if metadata["backend"] != backend or metadata["split"] != split:
        raise ValueError(f"{path}: backend or split does not match evaluation")
    if metadata["threshold"] != threshold:
        raise ValueError(f"{path}: threshold must match the frozen CLI threshold")
    if metadata["fixture_sha256"] != fixture_hash:
        raise ValueError(f"{path}: fixture hash does not match the evaluated fixtures")
    for key in ("code_revision", "model_revision", "weights_revision", "tokenizer_revision", "calibration_revision", "timing_scope"):
        if not isinstance(metadata[key], str) or not metadata[key].strip():
            raise ValueError(f"{path}: {key} must identify the frozen artifact or explicitly say not applicable")
    return metadata


def exact_correct(expected: dict[str, str], prediction: dict[str, Any]) -> bool:
    return all(prediction.get(k) == expected[k] for k in ("intent", "period", "metric"))


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    pos = (len(ordered) - 1) * p
    lo = int(pos)
    hi = min(lo + 1, len(ordered) - 1)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (pos - lo)


def evaluate(fixtures: list[dict[str, Any]], predictions: dict[str, dict[str, Any]], threshold: float) -> dict[str, Any]:
    fixture_ids = {x["id"] for x in fixtures}
    extras = set(predictions) - fixture_ids
    if extras:
        raise ValueError(f"predictions include unknown fixture ids: {sorted(extras)[:3]}")
    missing_ids = fixture_ids - set(predictions)
    if missing_ids:
        raise ValueError(f"predictions missing fixture ids: {sorted(missing_ids)[:3]}")
    accepted = eligible_accepted = eligible_correct = errors = covered = 0
    confident_errors = accepted_negative = eligible = 0
    latencies: list[float] = []
    missing = 0
    per_family: dict[str, dict[str, int]] = {}
    for fx in fixtures:
        if fx["eligible"]:
            eligible += 1
            fam = per_family.setdefault(fx["family"], {"n": 0, "accepted": 0, "correct_accepted": 0})
            fam["n"] += 1
        rec = predictions.get(fx["id"])
        if rec is None or rec.get("error"):
            missing += 1
            continue
        pred = rec["prediction"]
        confidence = float(pred["confidence"])
        is_fast_route = pred["intent"] in {"nutrition_summary", "meal_list", "latest_meal"}
        accepts = is_fast_route and confidence >= threshold
        correct = exact_correct(fx["expected"], pred)
        if is_fast_route and confidence >= threshold:
            accepted += 1
            covered += 1
            errors += not correct
            confident_errors += not correct
            if not fx["eligible"]:
                accepted_negative += 1
        if fx["eligible"]:
            if accepts:
                eligible_accepted += 1
                fam["accepted"] += 1
                if correct:
                    eligible_correct += 1
                    fam["correct_accepted"] += 1
        if "latency_ms" in pred:
            if isinstance(pred["latency_ms"], bool) or not isinstance(pred["latency_ms"], (int, float)) or not math.isfinite(pred["latency_ms"]) or pred["latency_ms"] < 0:
                raise ValueError(f"{fx['id']}: invalid latency_ms")
            latencies.append(float(pred["latency_ms"]))
    return {
        "fixture_count": len(fixtures), "prediction_count": len(predictions), "missing_or_error_count": missing,
        "threshold": threshold, "accepted_count": accepted, "coverage": covered / len(fixtures),
        "accepted_accuracy": (accepted - errors) / accepted if accepted else None,
        "accepted_errors": errors, "confident_errors": confident_errors,
        "accepted_negative_count": accepted_negative, "eligible_count": eligible,
        "eligible_accepted_count": eligible_accepted, "eligible_correct_accepted_count": eligible_correct,
        "eligible_coverage": eligible_accepted / eligible if eligible else None,
        "accepted_error_wilson_95": wilson(errors, accepted),
        "latency_sample_count": len(latencies), "latency_p50_ms": percentile(latencies, .50),
        "latency_p95_ms": percentile(latencies, .95), "families": per_family,
        "latency_note": "Only supplied adapter timings; no timing is inferred or synthesized.",
    }


def rules_reference(fixtures: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    """Deliberately conservative synthetic reference, not production Kotlin rules."""
    result = {}
    exact_phrases = {
        "how much protein did i record today": ("nutrition_summary", "today", "protein"),
        "how many calories did i record today": ("nutrition_summary", "today", "calories"),
        "protein total for the last seven days": ("nutrition_summary", "last_7_days", "protein"),
        "list the meals i logged today": ("meal_list", "today", "none"),
        "what meals did i record yesterday": ("meal_list", "yesterday", "none"),
        "show my meal log for today": ("meal_list", "today", "none"),
        "what was my most recent recorded meal": ("latest_meal", "none", "none"),
        "tell me about the latest meal i logged": ("latest_meal", "none", "none"),
        "show my last recorded meal": ("latest_meal", "none", "none"),
    }
    # Exact phrase matching intentionally demonstrates high precision and low coverage.
    for fx in fixtures:
        norm = " ".join(fx["text"].lower().strip(" ?.!\t").split())
        pred: dict[str, Any] = {"intent": "abstain", "period": "none", "metric": "none", "confidence": 1.0}
        if norm in exact_phrases:
            intent, period, metric = exact_phrases[norm]
            pred = {"intent": intent, "period": period, "metric": metric, "confidence": 1.0}
        result[fx["id"]] = {"id": fx["id"], "prediction": pred}
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--split", choices=("dev", "locked"), required=True)
    parser.add_argument("--predictions", type=Path, help="Frozen JSONL adapter predictions")
    parser.add_argument("--metadata", type=Path, help="Frozen adapter manifest JSON")
    parser.add_argument("--backend", choices=(*BACKENDS, "all"), required=True)
    parser.add_argument("--predictions-dir", type=Path, help="Batch mode: look for <backend>-<split>.jsonl")
    parser.add_argument("--threshold", type=float, default=0.8)
    parser.add_argument("--output", type=Path, help="Write JSON report (defaults to stdout)")
    parser.add_argument("--rules-reference", action="store_true", help="Run conservative synthetic exact-phrase reference on dev only")
    args = parser.parse_args(argv)
    if not 0 <= args.threshold <= 1:
        parser.error("--threshold must be in [0,1]")
    if args.rules_reference and (args.split != "dev" or args.backend not in {"rules", "all"} or args.predictions or args.predictions_dir):
        parser.error("--rules-reference is dev-only and mutually exclusive with prediction files")
    if args.backend == "all" and (args.predictions or args.metadata or (not args.predictions_dir and not args.rules_reference)):
        parser.error("--backend all requires --predictions-dir (or dev --rules-reference)")
    if args.backend != "all" and (args.predictions_dir or (not args.rules_reference and (not args.predictions or not args.metadata))):
        parser.error("--predictions and --metadata required unless --rules-reference is used")
    fixtures = load_fixtures(SETS[args.split])
    fixture_hash = sha256(SETS[args.split])
    claims = {"model_invoked_by_scorer": False, "latency_measured_by_scorer": False, "production_kotlin_rules": False, "host_read_or_render_verified": False, "threshold_selection": "external; must be selected on dev then frozen before locked evaluation"}
    if args.backend == "all":
        results = {}
        for backend in BACKENDS:
            path = args.predictions_dir / f"{backend}-{args.split}.jsonl" if args.predictions_dir else None
            metadata_path = args.predictions_dir / f"{backend}-{args.split}.metadata.json" if args.predictions_dir else None
            if backend == "rules" and args.rules_reference:
                results[backend] = {"status": "synthetic_reference_only", "source": "synthetic_rules_reference", "metrics": evaluate(fixtures, rules_reference(fixtures), args.threshold)}
            elif path and path.exists() and metadata_path and metadata_path.exists():
                metadata = load_metadata(metadata_path, backend, args.split, args.threshold, fixture_hash)
                results[backend] = {"status": "scored_imported_predictions", "source": str(path), "prediction_sha256": sha256(path), "metadata": metadata, "metrics": evaluate(fixtures, load_predictions(path), args.threshold)}
            else:
                reason = "prediction file unavailable" if not path or not path.exists() else "frozen metadata file unavailable"
                results[backend] = {"status": "pending", "reason": f"{reason}; no model run performed"}
        report = {"split": args.split, "fixture_sha256": fixture_hash, "claims": claims, "backends": results}
    else:
        preds = rules_reference(fixtures) if args.rules_reference else load_predictions(args.predictions)
        metadata = None if args.rules_reference else load_metadata(args.metadata, args.backend, args.split, args.threshold, fixture_hash)
        report = {"backend": args.backend, "split": args.split, "fixture_sha256": fixture_hash, "prediction_source": "synthetic_rules_reference" if args.rules_reference else str(args.predictions), "prediction_sha256": None if args.rules_reference else sha256(args.predictions), "metadata": metadata, "claims": claims, "metrics": evaluate(fixtures, preds, args.threshold)}
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, json.JSONDecodeError, OSError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        raise SystemExit(2)
