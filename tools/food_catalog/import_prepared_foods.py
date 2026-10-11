"""Build the bounded prepared-food supplement from official USDA detail JSON.

Acquisition is development-only. No runtime food query or member data is used.
Run with --input-dir data/usda/raw --output <asset path>.
"""
import argparse
import json
from pathlib import Path
from import_usda_foundation import build_food, write_atomically

SELECTED = {
    169737: ("Pasta, cooked, enriched, without added salt", "Pasta cocida enriquecida, sin sal añadida"),
    171638: ("Meatballs, frozen, Italian style", "Albóndigas congeladas, estilo italiano"),
}

def build_supplement(input_dir: Path) -> dict:
    foods = []
    for food_id, (description, spanish) in SELECTED.items():
        raw = json.loads((input_dir / f"{food_id}.json").read_text(encoding="utf-8"))
        if raw.get("fdcId") != food_id or raw.get("description") != description or raw.get("dataType") != "SR Legacy":
            raise ValueError(f"Unexpected USDA identity/type for {food_id}")
        food = build_food(raw)
        nutrients = {n["number"]: n["amount_per_100g"] for n in food["nutrients"]}
        if not all(n in nutrients for n in ("208", "203", "204", "205")):
            raise ValueError(f"Incomplete macros for {food_id}")
        food["spanish_display_name"] = spanish
        food["source_url"] = f"https://fdc.nal.usda.gov/food-details/{food_id}/nutrients"
        foods.append(food)
    return {
        "schema_version": 1,
        "source": {"name": "USDA FoodData Central SR Legacy", "license": "CC0-1.0",
                   "release": "sr-2018-v1", "release_url": "https://fdc.nal.usda.gov/download-datasets/"},
        "food_count": len(foods), "foods": foods,
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    write_atomically(args.output, build_supplement(args.input_dir))
