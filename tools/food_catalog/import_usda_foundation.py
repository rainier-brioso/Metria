#!/usr/bin/env python3
"""Build Metria's development-only offline catalog from a USDA JSON release.

Decision record: keep the complete Foundation source locally during M3
development, but write a deterministic app-shaped catalog with explicit source
versioning. This importer never processes member data.
"""

from __future__ import annotations

import argparse
import json
import os
import tempfile
from datetime import UTC, datetime
from pathlib import Path
from typing import Any


SOURCE_NAME = "USDA FoodData Central Foundation Foods"
SOURCE_LICENSE = "CC0-1.0"


def required(value: Any, label: str) -> Any:
    if value is None or value == "":
        raise ValueError(f"Missing required {label}")
    return value


def clean_number(value: Any) -> float | None:
    if value is None:
        return None
    if not isinstance(value, (int, float)):
        raise ValueError(f"Expected numeric nutrient value, received {value!r}")
    return float(value)


def build_food(food: dict[str, Any]) -> dict[str, Any]:
    nutrients = []
    for item in food.get("foodNutrients", []):
        nutrient = item.get("nutrient") or {}
        amount = clean_number(item.get("amount"))
        if amount is None:
            continue
        nutrients.append(
            {
                "id": required(nutrient.get("id"), "nutrient.id"),
                # A small number of Foundation rows have a valid nutrient
                # identity/name but no public number. Keep them for development
                # inspection instead of making the whole catalog non-importable.
                "number": nutrient.get("number") or None,
                "name": required(nutrient.get("name"), "nutrient.name"),
                "unit": required(nutrient.get("unitName"), "nutrient.unitName"),
                "amount_per_100g": amount,
                "minimum": clean_number(item.get("min")),
                "maximum": clean_number(item.get("max")),
                "median": clean_number(item.get("median")),
                "data_points": item.get("dataPoints"),
                "derivation": (item.get("foodNutrientDerivation") or {}).get("code"),
            }
        )

    portions = []
    for portion in food.get("foodPortions", []):
        grams = clean_number(portion.get("gramWeight"))
        if grams is None:
            continue
        unit = portion.get("measureUnit") or {}
        portions.append(
            {
                "amount": clean_number(portion.get("amount")),
                "modifier": portion.get("modifier") or None,
                "gram_weight": grams,
                "measure": unit.get("name"),
                "abbreviation": unit.get("abbreviation"),
            }
        )

    category = food.get("foodCategory") or {}
    return {
        "fdc_id": required(food.get("fdcId"), "food.fdcId"),
        "description": required(food.get("description"), "food.description"),
        "category": category.get("description"),
        "publication_date": food.get("publicationDate"),
        "nutrients": sorted(
            nutrients, key=lambda item: (item["number"] is None, item["number"] or "", item["id"])
        ),
        "portions": portions,
    }


def build_catalog(input_path: Path, release: str) -> dict[str, Any]:
    source = json.loads(input_path.read_text(encoding="utf-8"))
    foods = source.get("FoundationFoods")
    if not isinstance(foods, list) or not foods:
        raise ValueError("Expected a non-empty FoundationFoods list")

    # The source release may contain null placeholders. They have no food
    # identity to preserve, so count them explicitly rather than crashing or
    # pretending every array position was imported.
    food_records = [food for food in foods if isinstance(food, dict)]
    catalog_foods = [build_food(food) for food in food_records]
    return {
        "schema_version": 1,
        "source": {
            "name": SOURCE_NAME,
            "license": SOURCE_LICENSE,
            "release": release,
            "release_url": "https://fdc.nal.usda.gov/download-datasets/",
        },
        "generated_at_utc": datetime.now(UTC).replace(microsecond=0).isoformat(),
        "food_count": len(catalog_foods),
        "ignored_null_records": len(foods) - len(food_records),
        "foods": sorted(catalog_foods, key=lambda item: item["fdc_id"]),
    }


def write_atomically(output_path: Path, catalog: dict[str, Any]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{output_path.name}.", suffix=".tmp", dir=output_path.parent
    )
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(catalog, handle, ensure_ascii=False, separators=(",", ":"))
            handle.write("\n")
        os.replace(temporary_name, output_path)
    except Exception:
        Path(temporary_name).unlink(missing_ok=True)
        raise


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--release", default="2026-04-30")
    arguments = parser.parse_args()

    if not arguments.input.is_file():
        raise SystemExit(f"Input file not found: {arguments.input}")
    catalog = build_catalog(arguments.input, arguments.release)
    write_atomically(arguments.output, catalog)
    print(
        f"Wrote {catalog['food_count']} foods from {catalog['source']['name']} "
        f"release {arguments.release} to {arguments.output}"
    )


if __name__ == "__main__":
    main()
