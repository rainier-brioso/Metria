import json
import tempfile
import unittest
from pathlib import Path

from import_beverage_foods import SELECTED, build_beverages, source_volume_milliliters


class BeverageFoodsImportTest(unittest.TestCase):
    def write_inputs(self, directory: Path) -> None:
        for food_id, description in SELECTED.items():
            portions = (
                [
                    {"amount": 1.0, "modifier": "cup (8 fl oz)", "gramWeight": 237.0,
                     "measureUnit": {"name": "undetermined", "abbreviation": "undetermined"}},
                    {"amount": 1.0, "modifier": "fl oz", "gramWeight": 29.6,
                     "measureUnit": {"name": "undetermined", "abbreviation": "undetermined"}},
                    {"amount": 6.0, "modifier": "fl oz", "gramWeight": 178.0,
                     "measureUnit": {"name": "undetermined", "abbreviation": "undetermined"}},
                ] if food_id == 171890 else [
                    {"amount": 1.0, "modifier": "fl oz", "gramWeight": 29.6,
                     "measureUnit": {"name": "undetermined", "abbreviation": "undetermined"}},
                ]
            )
            value = {
                "fdcId": food_id,
                "description": description,
                "dataType": "SR Legacy",
                "foodNutrients": [
                    {"nutrient": {"id": index, "number": number, "name": number, "unitName": "g"},
                     "amount": amount}
                    for index, (number, amount) in enumerate(
                        (("208", 1), ("203", 0.12), ("204", 0.02), ("205", 0)), 1
                    )
                ],
                "foodPortions": portions,
            }
            (directory / f"{food_id}.json").write_text(json.dumps(value), encoding="utf-8")

    def test_preserves_usda_foods_nutrients_and_source_portions_with_explicit_metric_volume(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).parent) as temporary:
            directory = Path(temporary)
            self.write_inputs(directory)
            catalog = build_beverages(directory)

        self.assertEqual("beverages-sr-2018-v1", catalog["source"]["release"])
        self.assertEqual("CC0-1.0", catalog["source"]["license"])
        self.assertEqual([171890, 171891], [food["fdc_id"] for food in catalog["foods"]])
        coffee = catalog["foods"][0]
        self.assertEqual(SELECTED[171890], coffee["description"])
        self.assertEqual("https://fdc.nal.usda.gov/food-details/171890/nutrients", coffee["source_url"])
        cup = next(portion for portion in coffee["portions"] if portion["modifier"] == "cup (8 fl oz)")
        one_fl_oz = next(portion for portion in coffee["portions"] if portion["amount"] == 1.0 and portion["modifier"] == "fl oz")
        six_fl_oz = next(portion for portion in coffee["portions"] if portion["amount"] == 6.0)
        self.assertAlmostEqual(236.58824, cup["volume_milliliters"], places=5)
        self.assertEqual(237.0, cup["gram_weight"])
        self.assertAlmostEqual(29.57353, one_fl_oz["volume_milliliters"], places=5)
        self.assertEqual(29.6, one_fl_oz["gram_weight"])
        self.assertAlmostEqual(177.44118, six_fl_oz["volume_milliliters"], places=5)
        self.assertEqual(178.0, six_fl_oz["gram_weight"])
        self.assertIn("https://www.nist.gov/", catalog["source"]["volume_conversion"]["source_url"])

    def test_rejects_wrong_usda_identity_or_missing_essential_macros(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).parent) as temporary:
            directory = Path(temporary)
            self.write_inputs(directory)
            path = directory / "171890.json"
            value = json.loads(path.read_text(encoding="utf-8"))
            value["description"] = "Coffee, invented portion"
            path.write_text(json.dumps(value), encoding="utf-8")
            with self.assertRaises(ValueError):
                build_beverages(directory)

            self.write_inputs(directory)
            value = json.loads(path.read_text(encoding="utf-8"))
            value["foodNutrients"].pop()
            path.write_text(json.dumps(value), encoding="utf-8")
            with self.assertRaises(ValueError):
                build_beverages(directory)

    def test_never_infers_volume_from_a_cup_or_serving_label_alone(self):
        self.assertIsNone(source_volume_milliliters({"amount": 1, "modifier": "cup", "gram_weight": 240}))
        self.assertIsNone(source_volume_milliliters({"amount": 1, "modifier": "mug", "gram_weight": 240}))
        self.assertEqual(236.58824, source_volume_milliliters({"amount": 1, "modifier": "cup (8 fl oz)"}))


if __name__ == "__main__":
    unittest.main()
