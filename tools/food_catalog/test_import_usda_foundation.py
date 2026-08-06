import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("import_usda_foundation.py")
SPEC = importlib.util.spec_from_file_location("usda_importer", MODULE_PATH)
assert SPEC and SPEC.loader
IMPORTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(IMPORTER)


class ImportFoundationFoodsTest(unittest.TestCase):
    def test_preserves_full_development_fields_and_null_placeholders(self) -> None:
        source = {
            "FoundationFoods": [
                {
                    "fdcId": 123,
                    "description": "Test food",
                    "foodCategory": {"description": "Test category"},
                    "publicationDate": "4/30/2026",
                    "foodNutrients": [
                        {
                            "amount": 12.5,
                            "min": 10,
                            "max": 15,
                            "median": 12,
                            "dataPoints": 3,
                            "nutrient": {
                                "id": 1003,
                                "number": "203",
                                "name": "Protein",
                                "unitName": "g",
                            },
                            "foodNutrientDerivation": {"code": "A"},
                        },
                        {
                            "amount": 1,
                            "nutrient": {
                                "id": 9999,
                                "name": "Development-only nutrient",
                                "unitName": "mg",
                            },
                        },
                    ],
                    "foodPortions": [
                        {
                            "amount": 1,
                            "gramWeight": 75,
                            "measureUnit": {"name": "cup", "abbreviation": "cup"},
                        }
                    ],
                },
                None,
            ]
        }
        with tempfile.TemporaryDirectory() as directory:
            input_path = Path(directory) / "foundation.json"
            input_path.write_text(json.dumps(source), encoding="utf-8")
            catalog = IMPORTER.build_catalog(input_path, "test-release")

        self.assertEqual(catalog["food_count"], 1)
        self.assertEqual(catalog["ignored_null_records"], 1)
        food = catalog["foods"][0]
        self.assertEqual(food["fdc_id"], 123)
        self.assertEqual(food["nutrients"][0]["amount_per_100g"], 12.5)
        self.assertEqual(food["nutrients"][1]["number"], None)
        self.assertEqual(food["portions"][0]["gram_weight"], 75.0)

    def test_rejects_food_without_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            input_path = Path(directory) / "foundation.json"
            input_path.write_text(
                json.dumps({"FoundationFoods": [{"description": "No ID"}]}),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "food.fdcId"):
                IMPORTER.build_catalog(input_path, "test-release")


if __name__ == "__main__":
    unittest.main()
