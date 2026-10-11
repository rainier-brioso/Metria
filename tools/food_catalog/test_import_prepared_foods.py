import json
import tempfile
import unittest
from pathlib import Path
from import_prepared_foods import SELECTED, build_supplement

class PreparedFoodsImportTest(unittest.TestCase):
    def inputs(self, folder):
        for food_id, (description, _) in SELECTED.items():
            value = {"fdcId": food_id, "description": description, "dataType": "SR Legacy",
                     "foodNutrients": [{"nutrient": {"id": i, "number": n, "name": n, "unitName": "g"}, "amount": i}
                                       for i, n in enumerate(("208", "203", "204", "205"), 1)]}
            (folder / f"{food_id}.json").write_text(json.dumps(value), encoding="utf-8")

    def test_preserves_identity_and_rejects_a_substituted_sauced_recipe(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            self.inputs(folder)
            result = build_supplement(folder)
            self.assertEqual([169737, 171638], [f["fdc_id"] for f in result["foods"]])
            path = folder / "171638.json"
            value = json.loads(path.read_text())
            value["description"] = "Meatballs with sauce"
            path.write_text(json.dumps(value))
            with self.assertRaises(ValueError):
                build_supplement(folder)

    def test_rejects_incomplete_macro_source(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            self.inputs(folder)
            path = folder / "169737.json"
            value = json.loads(path.read_text())
            value["foodNutrients"].pop()
            path.write_text(json.dumps(value))
            with self.assertRaises(ValueError):
                build_supplement(folder)

if __name__ == "__main__":
    unittest.main()
