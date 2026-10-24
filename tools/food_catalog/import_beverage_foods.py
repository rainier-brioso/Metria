"""Build the bounded beverage catalog from official USDA SR Legacy details.

Acquisition is development-only. Raw USDA records are kept under the ignored
data/usda/raw directory; no runtime API or member data is used.
"""

from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path
from typing import Any

from import_usda_foundation import build_food, write_atomically


RELEASE = "beverages-sr-2018-v1"
SOURCE_URL = "https://fdc.nal.usda.gov/download-datasets/"
VOLUME_CONVERSION = {
    "name": "U.S. fluid ounce to milliliter",
    "milliliters_per_fluid_ounce": 29.57353,
    "source_url": (
        "https://www.nist.gov/pml/special-publication-811/nist-guide-si-"
        "appendix-b-conversion-factors/nist-guide-si-appendix-b9"
    ),
}

SELECTED = {
    171890: "Beverages, coffee, brewed, prepared with tap water",
    171891: "Beverages, coffee, brewed, espresso, restaurant-prepared",
}
ESSENTIAL_NUTRIENTS = {"208", "203", "204", "205"}
_CUP_FL_OZ = re.compile(r"\(\s*(\d+(?:\.\d+)?)\s*fl\s*oz\s*\)", re.IGNORECASE)
_FL_OZ = re.compile(r"\s*fl\s*oz\s*", re.IGNORECASE)
_FL_OZ_TO_ML = 29.57353  # NIST SP 811, U.S. fluid ounce conversion factor.


def source_volume_milliliters(portion: dict[str, Any]) -> float | None:
    """Return only volume explicitly named by a USDA fluid-ounce modifier."""
    modifier = portion.get("modifier")
    if not isinstance(modifier, str):
        return None
    cup_match = _CUP_FL_OZ.search(modifier)
    ounce_match = _FL_OZ.fullmatch(modifier)
    if cup_match is None and ounce_match is None:
        return None
    source_amount = portion.get("amount")
    if not isinstance(source_amount, (int, float)) or not math.isfinite(source_amount) or source_amount <= 0:
        raise ValueError("USDA fluid-ounce portion must have a positive numeric source amount")

    if cup_match:
        fluid_ounces = float(cup_match.group(1)) * float(source_amount)
    else:
        fluid_ounces = float(source_amount)

    if fluid_ounces <= 0:
        raise ValueError("USDA fluid-ounce portion must have a positive amount")
    return round(fluid_ounces * _FL_OZ_TO_ML, 5)


def _add_source_volumes(food: dict[str, Any]) -> None:
    for portion in food["portions"]:
        volume = source_volume_milliliters(portion)
        if volume is not None:
            # This is a unit conversion of USDA's explicit volume modifier,
            # not an assumed cup size or inferred liquid density.
            portion["volume_milliliters"] = volume


def build_beverages(input_dir: Path) -> dict[str, Any]:
    foods = []
    for food_id, description in SELECTED.items():
        raw_path = input_dir / f"{food_id}.json"
        raw = json.loads(raw_path.read_text(encoding="utf-8"))
        if (
            raw.get("fdcId") != food_id
            or raw.get("description") != description
            or raw.get("dataType") != "SR Legacy"
        ):
            raise ValueError(f"Unexpected USDA identity/type for {food_id}")

        food = build_food(raw)
        nutrients = {item["number"] for item in food["nutrients"]}
        missing = ESSENTIAL_NUTRIENTS - nutrients
        if missing:
            raise ValueError(f"Missing essential macro nutrient numbers for {food_id}: {sorted(missing)}")
        if not food["portions"]:
            raise ValueError(f"Missing USDA portions for {food_id}")

        _add_source_volumes(food)
        food["source_url"] = f"https://fdc.nal.usda.gov/food-details/{food_id}/nutrients"
        foods.append(food)

    return {
        "schema_version": 1,
        "source": {
            "name": "USDA FoodData Central SR Legacy",
            "license": "CC0-1.0",
            "release": RELEASE,
            "release_url": SOURCE_URL,
            "volume_conversion": VOLUME_CONVERSION,
        },
        "food_count": len(foods),
        "foods": foods,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()

    for food_id in SELECTED:
        if not (arguments.input_dir / f"{food_id}.json").is_file():
            raise SystemExit(f"Input file not found for USDA food {food_id}")
    catalog = build_beverages(arguments.input_dir)
    write_atomically(arguments.output, catalog)
    print(f"Wrote {catalog['food_count']} USDA beverages to {arguments.output}")


if __name__ == "__main__":
    main()
