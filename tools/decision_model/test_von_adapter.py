import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

import run_von


class VonAdapterTests(unittest.TestCase):
    def fake_answer(self, key):
        options = {**{k: 0.01 for k in run_von.CAUTIOUS_OPTIONS}, key: 0.93}
        return SimpleNamespace(choice=key, probabilities=options, confidence=0.88)

    def test_joint_choice_maps_all_supported_routes(self):
        for key, expected in run_von.ROUTES.items():
            with self.subTest(key=key):
                seen = {}

                def decide(**kwargs):
                    seen.update(kwargs)
                    return self.fake_answer(key)

                result = run_von.prediction_for({"id": "x", "text": "fictional prompt"}, decide)
                self.assertEqual(tuple(result["prediction"][k] for k in ("intent", "period", "metric")), expected)
                self.assertEqual(set(seen), {"state", "choices", "instructions", "model"})
                self.assertEqual(seen["choices"], run_von.CAUTIOUS_OPTIONS)
                self.assertEqual(seen["state"], "fictional prompt")
                self.assertEqual(seen["model"], "von-1.3.0")
                self.assertNotIn("expected", seen)
                self.assertNotIn("eligible", seen)
                self.assertEqual(result["prediction"]["von_confidence"], 0.88)
                self.assertEqual(result["prediction"]["confidence"], 0.93)

    def test_concise_profile_is_an_explicit_label_free_alternative(self):
        seen = {}

        def decide(**kwargs):
            seen.update(kwargs)
            return self.fake_answer("latest_meal")

        result = run_von.prediction_for({"id": "x", "text": "How many calories were in my last meal?"}, decide, "concise")
        self.assertEqual(seen["choices"], run_von.CONCISE_OPTIONS)
        self.assertNotEqual(seen["instructions"], run_von.CAUTIOUS_INSTRUCTIONS)
        self.assertEqual(result["prediction"]["intent"], "latest_meal")
        self.assertNotIn("expected", seen)
        self.assertNotIn("eligible", seen)

    def test_abstain_is_a_non_fast_prediction(self):
        result = run_von.prediction_for({"id": "n", "text": "please draft a meal"}, lambda **_: self.fake_answer("abstain"))
        self.assertEqual(result["prediction"]["intent"], "abstain")
        self.assertEqual(result["prediction"]["period"], "none")

    def test_malformed_input_cannot_smuggle_fixture_labels(self):
        with self.assertRaises(ValueError):
            run_von.prediction_for({"id": "x", "text": "hi", "expected": {"intent": "latest_meal"}}, lambda **_: None)

    def test_each_prompt_failure_is_kept_as_an_error_row(self):
        examples = [{"id": "a", "text": "one"}, {"id": "b", "text": "two"}]
        rows = run_von.run_predictions(examples, lambda **_: (_ for _ in ()).throw(RuntimeError("inference failed")))
        self.assertEqual([row["id"] for row in rows], ["a", "b"])
        self.assertTrue(all("error" in row and "prediction" not in row for row in rows))

    def test_fixture_reader_projects_only_id_and_text(self):
        with tempfile.TemporaryDirectory(dir=run_von.ROOT) as directory:
            path = Path(directory) / "dev.jsonl"
            path.write_text("\n".join(json.dumps({"id": str(i), "text": f"p{i}", "expected": {"secret": True}, "eligible": True}) for i in range(120)), encoding="utf-8")
            examples = run_von.fixture_examples(path)
        self.assertEqual(examples[0], {"id": "0", "text": "p0"})
        self.assertTrue(all(set(item) == {"id", "text"} for item in examples))


if __name__ == "__main__":
    unittest.main()
