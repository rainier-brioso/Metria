# Phase calorie and macro proposals: research

Research date: 2026-10-08. Status: research and candidate product policy only.
Later decisions: [macro contract](phase-nutrition-contract-proposal.md) is approved
and build-verified; [phase energy contract](phase-energy-contract-proposal.md)
now records the approved 10% Lose / 5% strength-Gain starting policy and its
evidence limits. The original research below is retained as decision context,
not a claim that those exact percentages are guideline-prescribed.
The member requested research into calculating phase targets from collected data.
No new calculator, targets, profile fields or archive format are implemented or
approved by this note. The existing maintenance-only contract remains in force.

## Findings and evidence

There is no single optimal macro split derivable from age, height and weight.
Energy expenditure, training, preferences and suitability affect the proposal.
Population reference ranges are useful starting evidence, not an individual's
measured requirement. EFSA explicitly distinguishes its reference values from
individual goals. [EFSA DRV overview, reviewed 2024](https://www.efsa.europa.eu/en/topics/topic/dietary-reference-values).

General adult references: EFSA's protein population reference intake is
0.83 g/kg/day; this is an adequacy reference, not a muscle-building optimum.
Its adult energy ranges are 45–60% carbohydrate and 20–35% fat. These support a
general balanced starting option without establishing one ideal percentage.
[Protein reference](https://www.efsa.europa.eu/en/press/news/120209),
[carbohydrate/fat references](https://www.efsa.europa.eu/en/press/news/nda100326).
The older press releases are interpreted alongside EFSA's current DRV overview.

Exercise changes the protein rationale. The ISSN position statement supports
1.4–2.0 g/kg/day for most healthy exercising adults. A resistance-training
meta-analysis of 49 trials found the average breakpoint near 1.6 g/kg/day for
additional fat-free-mass gains. This is population evidence, not an individual
ceiling, universal requirement or guarantee. Higher protein recommendations for
lean, trained people under energy restriction cannot be indiscriminately applied
to every person selecting Lose; fat-free-mass denominators must not be confused
with total body weight. Metria does not collect reliable lean mass.
[ISSN 2017](https://link.springer.com/article/10.1186/s12970-017-0177-8),
[Morton et al. 2018 abstract](https://pubmed.ncbi.nlm.nih.gov/28698222/).

Exercise carbohydrate needs depend on training workload, not simply a weekly
session count. The sports-nutrition position statement supports tailoring intake
to sport demands; its abstract does not supply enough detail to implement a
numeric training carbohydrate table. Such a table needs full guideline review
and duration/intensity inputs before adoption.
[ACSM/Academy/Dietitians of Canada 2016](https://pubmed.ncbi.nlm.nih.gov/26891166/).

For a macro budget, use the general energy factors: protein 4 kcal/g,
carbohydrate 4 kcal/g and fat 9 kcal/g. These are planning conventions. They must
not replace the existing catalog-derived meal calorie values: fiber, polyols,
alcohol, food-specific factors and label rounding can cause differences.
[FAO energy conversion factors](https://www.fao.org/4/Y5022E/y5022e04.htm).

Weight-loss energy guidance is context-specific. The NHLBI evidence review
includes 500–750 kcal/day deficits in adult overweight/obesity interventions;
it does not justify applying that subtraction to everyone choosing Lose.
NIDDK uses a different dynamic weight model and excludes children and pregnancy/
breastfeeding. A fixed deficit must not become an exact forecast of weight or
completion date.
[NHLBI evidence review](https://www.nhlbi.nih.gov/sites/default/files/media/docs/obesity-evidence-review.pdf),
[NIDDK planner scope](https://www.niddk.nih.gov/health-information/weight-management/body-weight-planner).

Weight-gain surplus evidence is less settled. A small 2023 resistance-training
trial compared maintenance, 5% and 15% surpluses; faster body-mass gain primarily
increased skinfolds rather than consistently improving hypertrophy. This supports
caution about automatic large surpluses, not a universal validated 5% rule.
[Helms et al. 2023](https://link.springer.com/article/10.1186/s40798-023-00651-y).

## Inputs Metria actually has

`GoalProfile` currently stores reviewed age, height, weight, the historical
sex-based equation choice and whole-day activity category, only with an accepted
maintenance estimate. `GoalPhase` stores intent and optional chosen target/range.
Manual calorie overrides currently remove the accepted calculation profile.
The app does not collect training type, frequency, duration, intensity or lean mass.
The user's description of Fitia onboarding must not be treated as existing data.

Age, height and equation choice principally affect the energy estimate; reviewed
weight and training context affect a body-weight protein calculation. They do not
justify a separate arbitrary macro multiplier for every sex or age. Whole-day
activity already includes exercise: do not add exercise calories again.

Minimal useful new input: an optional training-context choice (general wellness,
regular resistance training, other regular exercise, demanding endurance/mixed
training, or prefer manual). Do not infer training from meals, photos or intent.
Frequency alone is insufficient; collect additional workload only if an adopted
method actually uses it. Unknown context retains general/manual options.

## Candidate algorithm for review

These exact defaults are Metria product choices informed by evidence, not formulas
specified by the cited organizations. Nutrition review and a deliberate M1/data
contract revision are needed before enabling them.

1. Establish an explicitly reviewed daily calorie budget `E`. Keep the existing
   maintenance estimate and its approximation disclosure. Maintain may propose
   a clearly labelled starting value within that interval; choosing a midpoint
   automatically would itself change today's contract. Just track needs no targets.
   Gain/Lose need a separately reviewed phase energy adjustment policy. Until that
   is specified, use a member-entered calorie budget and calculate macros from it.
   Do not label an adjusted budget as an unchanged accepted maintenance estimate.
2. General balanced candidate: allocate 20% of `E` to protein and 30% to fat;
   carbohydrate receives the remaining 50%. Check the resulting protein against
   the 0.83 g/kg adequacy reference. If it fails, show an unresolved proposal and
   offer review/manual entry rather than silently raising protein or changing
   calories. The percentages are an editable starting option, not personalized
   measured needs.
3. Regular resistance-training candidate: `P = 1.6 × reviewed weightKg` grams/day.
   Other regular exercising adults could explicitly select a 1.4–2.0 g/kg reference;
   selecting a default inside that range requires a documented product decision.
   Demanding endurance/mixed sport remains manual until workload guidance is reviewed.
4. For a selected body-weight protein proposal, choose an editable fat share `f`
   (candidate starting value 0.30). Calculate `F = E × f / 9` and
   `C = (E − 4 × P − 9 × F) / 4`. Show the resulting energy shares and assumptions.
   Negative/zero remainder, invalid numbers and unsupported combinations yield no
   automatic complete proposal. Do not silently reduce protein to force a result.
   A split outside general reference ranges needs explicit contextual review;
   those ranges are not universal toxicity limits or proof the diet is unsafe.
5. Keep unrounded calculation values; show whole grams and sensible calorie
   precision. Independently rounding three displayed macros can cause up to
   roughly 9 kcal difference under 4/4/9. Explain approximation, and test the
   rounding contract. Never change a previously entered macro to hide a mismatch.
6. Display all four proposed values together, explain their source on demand,
   and offer Edit / Use proposal / Skip. Save only after final phase confirmation.
   New weights, phase labels and edits do not silently recalculate accepted targets.

Fictional example, not a member recommendation: a reviewed 2,400 kcal budget and
75 kg reference with regular resistance training produces 120 g protein,
80 g fat and 300 g carbohydrate using the candidate 1.6 g/kg and 30% fat method.
The planning arithmetic is `480 + 720 + 1,200 = 2,400 kcal`. This example does not
establish that 2,400 kcal is suitable for that person.

## Applicability and evidence gaps before implementation

Retain the current optional 19–64 adult scope and transient suitability check.
General formulas cannot stand in for individualized clinical nutrition, pregnancy/
breastfeeding support or eating-disorder care. Do not collect diagnoses merely to
implement a calculator. Offer manual tracking or existing professionally chosen
references. Existing broad input/storage bounds are engineering limits, not a
validated protein-calculator population; applying total-weight sports factors to
all 35–250 kg profiles is not justified. Define the supported population and weight
basis explicitly, without secretly using target weight, BMI-derived ideal weight
or estimated lean mass. Gain/Lose applicability, energy adequacy safeguards and
adjustment magnitude remain unresolved; no automatic deficit/surplus ships yet.

No new AI or cloud calculation is needed. A deterministic local calculator is
reproducible, available offline and keeps the profile private. Optional coaching
must not choose or save these numbers.

Storage must distinguish maintenance estimate, phase calorie adjustment, macro
method/version, reviewed inputs, rounding and per-field overrides. Existing
`acceptedEstimate` only validates calories inside the maintenance interval and
has no macro provenance; it cannot truthfully represent this extension unchanged.
Define versioned archive compatibility and atomic rollback before saving new fields.
An edited field becomes manual; dependent suggestions become stale and need review.
Historical phases retain their own accepted inputs and method, not current profile.

Required tests: formula/unit fixtures, comma decimals, missing/invalid/nonfinite
inputs, unsuitable population fallback, low-energy/high-protein conflicts, reference
range edges, rounding budget, dependent invalidation, overrides, no double-counted
activity, no implicit recalculation, duplicate save, rollback, versioned round-trip,
legacy absence and privacy exclusion. Phone acceptance includes review/edit/skip,
keyboard, accessibility, lock/recreation and save/reopen. Research is not acceptance.

Next bounded task: draft the phase-nutrition calculation contract and focused review
screen around these candidates, resolving energy adjustment and applicability before
implementing automatic phase calories. The installed app remains unchanged.
