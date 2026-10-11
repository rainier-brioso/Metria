import math
import contextlib
import io
import json
import re
import tempfile
import unittest
from pathlib import Path

import benchmark


class HarnessTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dev = benchmark.load_fixtures(benchmark.SETS["dev"])
        cls.locked = benchmark.load_fixtures(benchmark.SETS["locked"])

    def test_fixture_counts_and_template_families_are_split(self):
        self.assertEqual(120, len(self.dev))
        self.assertEqual(120, len(self.locked))
        self.assertEqual(set(), {x["template_family"] for x in self.dev} & {x["template_family"] for x in self.locked})
        self.assertEqual(60, sum(x["eligible"] for x in self.dev))
        self.assertEqual(60, sum(x["eligible"] for x in self.locked))
        dev_prompts = {" ".join(x["text"].lower().split()) for x in self.dev}
        locked_prompts = {" ".join(x["text"].lower().split()) for x in self.locked}
        self.assertFalse(dev_prompts & locked_prompts)
        for x in self.dev + self.locked:
            if x["eligible"] and x["expected"]["period"] == "last_7_days":
                self.assertIn("including today's entries", x["text"].lower())
            if x["eligible"] and x["family"] == "latest_meal":
                self.assertIn("calor", x["text"].lower())
                self.assertIn("protein", x["text"].lower())
        for rows in (self.dev, self.locked):
            drafts = [x for x in rows if x["family"] == "draft_meal"]
            self.assertEqual(5, len(drafts))
            self.assertTrue(all(x["expected"]["intent"] == "draft_meal" for x in drafts))
        # A simple lexical screen catches obvious cross-split templated paraphrases;
        # it is not presented as a substitute for human semantic review.
        for x in self.dev:
            for y in self.locked:
                if x["family"] != y["family"]:
                    continue
                left, right = set(x["text"].lower().split()), set(y["text"].lower().split())
                similarity = len(left & right) / len(left | right)
                self.assertLess(similarity, .70, (x["text"], y["text"], similarity))

    def test_all_arguments_are_required_for_correctness(self):
        fx = next(x for x in self.dev if x["eligible"])
        pred = dict(fx["expected"], confidence=.99)
        self.assertTrue(benchmark.exact_correct(fx["expected"], pred))
        pred["period"] = "yesterday"
        self.assertFalse(benchmark.exact_correct(fx["expected"], pred))

    def test_nutrition_period_labels_match_explicit_window(self):
        for fixture in self.dev + self.locked:
            if fixture["family"] != "nutrition":
                continue
            text = fixture["text"].lower()
            self.assertNotRegex(text, r"\b(?:last week|this week|past week|prior seven days)\b")
            rolling = bool(re.search(r"\b(?:seven[- ]day|seven days|7 days)\b", text))
            self.assertEqual("last_7_days" if rolling else "today", fixture["expected"]["period"])
            if rolling:
                self.assertIn("including today's entries", text)

    def test_negative_non_fast_answer_is_fallback_not_accepted(self):
        # Use one negative as the fixture so denominator semantics stay explicit.
        fx = next(x for x in self.dev if not x["eligible"])
        pred = {fx["id"]: {"id": fx["id"], "prediction": dict(fx["expected"], confidence=.99)}}
        report = benchmark.evaluate([fx], pred, .8)
        self.assertEqual(0, report["accepted_count"])
        self.assertEqual(0, report["accepted_negative_count"])
        self.assertEqual(0, report["eligible_count"])

    def test_negative_fast_route_is_confident_error(self):
        fx = next(x for x in self.dev if not x["eligible"])
        pred = {fx["id"]: {"id": fx["id"], "prediction": {"intent":"meal_list", "period":"today", "metric":"none", "confidence":.95}}}
        report = benchmark.evaluate([fx], pred, .8)
        self.assertEqual(1, report["accepted_negative_count"])
        self.assertEqual(1, report["confident_errors"])

    def test_missing_prediction_ids_are_rejected(self):
        eligible = [x for x in self.dev if x["eligible"]]
        with self.assertRaisesRegex(ValueError, "missing fixture ids"):
            benchmark.evaluate(eligible, {}, .8)

    def test_adapter_errors_count_as_unavailable_predictions(self):
        eligible = [x for x in self.dev if x["eligible"]]
        preds = {x["id"]: {"id": x["id"], "error": "adapter timeout"} for x in eligible}
        report = benchmark.evaluate(eligible, preds, .8)
        self.assertEqual(60, report["eligible_count"])
        self.assertEqual(0, report["eligible_coverage"])
        self.assertEqual(60, report["missing_or_error_count"])

    def test_wilson_zero_error_interval_is_reported(self):
        lo, hi = benchmark.wilson(0, 60)
        self.assertEqual(0, lo)
        self.assertGreater(hi, 0)
        self.assertLess(hi, .1)

    def test_percentiles_interpolate_without_inventing_samples(self):
        self.assertEqual(20, benchmark.percentile([10, 20, 30], .5))
        self.assertAlmostEqual(29, benchmark.percentile([10, 20, 30], .95))
        self.assertIsNone(benchmark.percentile([], .5))

    def test_rules_reference_has_no_latency_and_is_not_production(self):
        report = benchmark.evaluate(self.dev, benchmark.rules_reference(self.dev), .8)
        self.assertEqual(0, report["latency_sample_count"])
        self.assertFalse(math.isnan(report["eligible_coverage"]))

    def test_rules_reference_does_not_read_fixture_labels(self):
        fx = next(x for x in self.dev if x["text"] == "How much protein did I record today?")
        original = benchmark.rules_reference([fx])[fx["id"]]
        altered = dict(fx, eligible=False, expected={"intent":"unsupported", "period":"none", "metric":"none"})
        self.assertEqual(original, benchmark.rules_reference([altered])[fx["id"]])

    def test_metadata_is_frozen_to_code_split_threshold_and_fixtures(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "manifest.json"
            meta = {"backend":"von", "split":"dev", "code_revision":"sha", "model_revision":"model", "weights_revision":"weights", "tokenizer_revision":"tokenizer", "calibration_revision":"cal", "threshold":.8, "fixture_sha256":benchmark.sha256(benchmark.SETS["dev"]), "timing_scope":"adapter inference"}
            path.write_text(json.dumps(meta), encoding="utf-8")
            self.assertEqual(meta, benchmark.load_metadata(path, "von", "dev", .8, benchmark.sha256(benchmark.SETS["dev"])))
            with self.assertRaisesRegex(ValueError, "threshold"):
                benchmark.load_metadata(path, "von", "dev", .7, benchmark.sha256(benchmark.SETS["dev"]))

    def test_batch_cli_marks_absent_backends_pending(self):
        out = io.StringIO()
        with tempfile.TemporaryDirectory() as temp:
            with contextlib.redirect_stdout(out):
                self.assertEqual(0, benchmark.main(["--split", "dev", "--backend", "all", "--rules-reference", "--output", str(Path(temp) / "report.json")]))
            report = json.loads((Path(temp) / "report.json").read_text(encoding="utf-8"))
        self.assertEqual("synthetic_reference_only", report["backends"]["rules"]["status"])
        self.assertEqual("pending", report["backends"]["von"]["status"])
        self.assertEqual("pending", report["backends"]["gemma"]["status"])

    def test_nonfinite_confidence_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "pred.jsonl"
            path.write_text('{"id":"x","prediction":{"intent":"abstain","period":"none","metric":"none","confidence":NaN}}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "confidence"):
                benchmark.load_predictions(path)

    def test_false_or_empty_error_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "pred.jsonl"
            path.write_text('{"id":"x","error":""}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "error must be non-empty"):
                benchmark.load_predictions(path)
            path.write_text('{"id":"x","error":false}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "error must be non-empty"):
                benchmark.load_predictions(path)


if __name__ == "__main__":
    unittest.main()
