# Meal detail and editing proposal — 2026-10-06

Status: member-approved direction under staged Android implementation; see
`implementation-checkpoints.md`. Calculation-source expansion and physical
acceptance remain open.

## Borrowed interaction ideas, expressed in Metria's design

- Meal title and small reference illustration, source/estimate label, portion,
  and four compact metrics: calories, protein, carbohydrates, fat.
- Ingredient rows show name, amount, weight basis and available calorie estimate.
  Tap a row for one focused editor, rather than exposing every field at once.
- Expand Nutrition details and Vitamins & minerals only when requested. Missing
  nutrients read Not available; never invent values from an image or treat an
  absent nutrient as zero. No automatic daily reference percentages or nutrient
  targets are assumed by this proposal.
- Adjust portion scales ingredient weights proportionally and invalidates the
  checked state. Individual ingredients remain independently editable. This is
  a quantity multiplier, not an inferred cooked yield or raw-to-cooked conversion.
- Optional Remember as my dish remains a separate, reversible choice applied
  only at final save; it is not a favorite icon with ambiguous behavior.
- Ask Metria is a quiet secondary action scoped to the meal. The mockup supplies
  fixed explanations of coverage and portion scaling; it makes no model call.
- Save remains the dominant action. Review confirmation is explicit. Saved meal
  editing stages all changes until Save meal changes. Leave confirms discarding
  changed edits; ingredient Cancel does not apply its temporary edits.

## Interactive examples

Use Meal review in the preview menu, or Create meal draft from photo/voice.
Tap a saved Today row to edit it. Ingredient name/preparation changes clear its
nutrition match. Choosing a fictional sample match restores matching name and
weight basis; unknown foods remain in the meal with partial nutrition. The
portion control updates both weights and totals. Save updates Today and offers
View meal in page memory only. Existing entries retain stable IDs when moved
between meal types. Reopening/editing replaces that entry rather than adding a
duplicate. New reviewed meals receive new example IDs.

All nutrient numbers are deliberately fictional sample values per 100 g, used
to demonstrate deterministic scaling. No source image, reference app values,
actual food catalog, nutrition advice or goal target is copied. Vitamins,
minerals, sodium, fiber and saturated fat are unavailable in these fixtures.
The illustration is a placeholder, not evidence for a saved photo or food.

## Implementation boundaries

Follow existing Meal review / Dish editor / Saved meal editor contracts in
SCREEN-SPECS and FLOWS. This single-dish example does not replace collapsed
dish cards for multi-dish meals. Production calculation stays based on confirmed
quantities and accepted offline catalog matches; display only nutrients actually
supported by the persisted source. Expanding stored macro/micronutrient fields
needs schema, archive, units and source-coverage decisions first.

Name or preparation changes cannot preserve a stale catalog association.
Manual nutrition follows its separate source policy; do not replace it using
these fictional match rules. Unknown foods trigger partial coverage disclosure
and the existing saved-meal coverage-loss confirmation where applicable.

Photo cleanup, reduced reference thumbnail consent, local correction learning,
atomic reviewed saving and Chat's durable confirmation/View meal continue to
follow existing contracts. This preview updates only Today; Chat origin routing
remains an Android requirement, not simulated acceptance here.

Physical tests remain required for keyboard access, enlarged fonts, TalkBack,
rotation, cancel/back, ingredient removal, source/match changes, portion changes,
partial or absent nutrition, saving failures and save/reopen. Browser file URLs
remain blocked by the automation tool, so visual review is manual. Scripted
state checks are not a substitute for visual or phone evidence.

JavaScript syntax and state smoke checks passed for photo/voice review routing,
portion scaling, cleared/restored matches, ingredient cancellation/validation,
removal, partial/missing nutrition, save/reopen, meal-type moves with stable IDs,
input escaping and goal-setup regression. No network, storage or device APIs
are used. The temporary smoke harness was removed after execution.
