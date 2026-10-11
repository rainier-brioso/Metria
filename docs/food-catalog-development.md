# Offline food catalog: development policy

## Decision

Metria uses the USDA FoodData Central Foundation Foods release as the first
offline catalog source. The source data is public domain under CC0 1.0.

## Bounded prepared-food supplement — 2026-10-10

Member approved filling the spaghetti/meatball gap. Bundle two unchanged USDA
SR Legacy references alongside Foundation, rather than inventing a recipe:

- [169737: Pasta, cooked, enriched, without added salt](https://fdc.nal.usda.gov/food-details/169737/nutrients).
  Per 100 g: 158 kcal, 5.8 g protein, 30.86 g carbohydrate, 0.93 g fat.
  This is a cooked-pasta approximation for ordinary spaghetti; it is not dry
  pasta, whole-wheat pasta or an exact household recipe.
- [171638: Meatballs, frozen, Italian style](https://fdc.nal.usda.gov/food-details/171638/nutrients).
  Per 100 g: 286 kcal, 14.4 g protein, 8.06 g carbohydrate, 22.21 g fat.
  Keep the frozen/Italian-style reference visible. It is a reviewable similar-food
  estimate, not evidence of a particular meat species or homemade recipe.

The existing Foundation tomato sauce remains separate. Do not match a meatball
reference containing sauce while also calculating a separate sauce portion.
Explicit dry/raw descriptions cannot use the two prepared references; specific
meat names with no supported match remain unmatched for manual review.
English/Spanish spaghetti and generic meatball aliases propose these references;
they never replace member confirmation or add nutrients to previously saved meals.

Acquired official USDA food-detail JSON using the public demonstration API during
development only. Both records are SR Legacy, published April 1, 2019 from the
final April 2018 dataset. The [official download page](https://fdc.nal.usda.gov/download-datasets/)
identifies that release. Raw files remain ignored under data/usda/raw; the app
contains only the normalized two-food supplement and uses no runtime API/key.
Reproduce with `python tools/food_catalog/import_prepared_foods.py --input-dir
data/usda/raw --output native-android/app/src/main/assets/usda-prepared-sr-2018-v1.json`.
Download inputs from `https://api.nal.usda.gov/fdc/v1/food/<id>?api_key=DEMO_KEY`.
The importer verifies exact IDs/descriptions/type and complete macros, preserves
available nutrients and writes atomically. Source URLs are retained in the asset.

The merged bundle release is `2026-04-30+sr-2018-v1`, identifying Foundation plus
the bounded SR supplement; no SR food is attributed to Foundation. Existing food
IDs/values and previously saved releases are unchanged. New reviewed saves use
the bundle release; duplicate IDs reject rather than replace a source silently.

The complete April 2026 releases are intentionally retained in a developer's
local `data/usda/raw/` directory while M3 is being designed:

- JSON: structured food data, including nutrients, portions, categories, and
  provenance.
- CSV: the full relational release, roughly 32 MB unpacked, used to inspect
  fields before any release-size decision.

Those files are ignored by Git. A reproducible importer and the exact official
URLs are tracked instead. The raw source is not an Android asset and is never
written into the encrypted member vault.

## Development catalog shape

The importer creates a local, versioned JSON catalog containing every
Foundation food and its available nutrient values, category, portions, and
source release metadata. This deliberately preserves more than calories and
macros during development. It lets us later evaluate fields such as fiber,
sugars, sodium, nutrient derivation, and household portions without reworking
the acquisition pipeline.

The app-facing adapter will read only the fields it needs for the member's
confirmed calculation. A production build may later use a reduced catalog, but
only after a size and UX decision is documented; it must retain the source ID,
release, and attribution.

Metria maintains a small, versioned Spanish-to-English search-alias list for
common foods. An alias only finds a candidate in the offline USDA catalog; it
does not rename, translate, or silently select a food. The member still sees
and confirms the USDA description and portion.

For a curated subset of common foods, Metria also has a Spanish display name
for Spanish-device users. The USDA English description remains visible beneath
it during search and confirmation, while the saved record retains its USDA ID
and release.

To avoid mixed-language result lists, recognized USDA food descriptions also
receive deterministic local phrase translation for Spanish-device users. This
is only a display fallback: exact reviewed names take precedence, unknown words
remain in the USDA English line, and neither source ID nor nutrient data is
changed.

## Privacy and integrity constraints

- No runtime network lookup, API key, account, prompt, photo, or food query is
  sent to USDA or any other provider.
- Food catalog lookup proposes a match. It cannot create a meal entry without
  the member's explicit review and save action.
- A saved meal records the selected source and catalog release, not the full
  external dataset.
- The importer fails when essential identity fields are missing and writes its
  output atomically.

## Acquire and build locally

```powershell
Invoke-WebRequest `
  -Uri 'https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_foundation_food_json_2026-04-30.zip' `
  -OutFile 'data/usda/raw/FoodData_Central_foundation_food_json_2026-04-30.zip'
Expand-Archive 'data/usda/raw/FoodData_Central_foundation_food_json_2026-04-30.zip' `
  'data/usda/raw/2026-04-30'
python tools/food_catalog/import_usda_foundation.py `
  --input data/usda/raw/2026-04-30/FoodData_Central_foundation_food_json_2026-04-30.json `
  --output data/usda/generated/usda-foundation-2026-04-30.json
```

See the [official USDA downloads](https://fdc.nal.usda.gov/download-datasets/)
and [licensing statement](https://fdc.nal.usda.gov/) when refreshing the
release.
