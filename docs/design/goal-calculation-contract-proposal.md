# Optional goal setup: calculation and storage proposal

Extension approved 2026-10-08: [phase nutrition contract](phase-nutrition-contract-proposal.md)
adds optional reviewed macro proposals with independent source metadata and explicit
training context. The maintenance method below is unchanged; its original
manual-macro boundary is superseded only for that approved optional proposal flow.
The separately approved [phase calorie contract](phase-energy-contract-proposal.md)
adds bounded reviewed 10% Lose / 5% strength-Gain proposals. Forecasts remain excluded.

Status: member approved the maintenance-estimate and encrypted-history scope
with “Go ahead”, 2026-10-06. Implemented for automated verification; physical
acceptance remains open. Weight-change adjustments and macro targets stay
member-entered. M1 and the encrypted snapshot/archive contract are revised below
and in the architecture/design documents. This document retains the method's
limits and approval record; it does not close release or privacy gates.

## Recommended first scope

Offer an optional estimated maintenance-energy starting point after vault
unlock. Maintain / Gain / Lose / Track describe member intent; they do not
automatically select a weight, deadline, deficit or surplus. Gain/Lose initially
keep calorie adjustments and weight targets member-entered. Protein and other
macros also remain member-entered; strength frequency alone cannot select them.
Every estimate is a draft until the member explicitly accepts or edits it.

This lets first-run setup help without claiming a validated weight-change plan.
Skip setup and tracking without targets remain available. No assistant tool may
set goals, and no cloud model computes or receives profile answers.

## Candidate energy method and its limits

Proposed version identifier: `maintenance-msj-pal-v1`.

Use the simplified Mifflin–St Jeor resting-energy equation:

`REE kcal/day = 10 × kg + 6.25 × cm − 5 × age + coefficient`

Coefficient: +5 for the original male equation, −161 for the original female
equation. The study describes healthy adults aged 19–78 and both normal-weight
and obese participants. It predicts resting expenditure, not an individual's
measured daily needs. [Original paper, PMID 2305711](https://pubmed.ncbi.nlm.nih.gov/2305711/).

Explain the sex-based equation choice as a limitation of the historical method,
not gender identity. Do not infer it from a name, photo or voice. Declining or
not finding a suitable coefficient leaves manual targets available; do not
substitute an average coefficient silently.

For a daily starting estimate, use one member-selected whole-day activity
scenario. Proposed categories use the FAO lifestyle intervals: light 1.40–1.69,
active 1.70–1.99, vigorous 2.00–2.40. The descriptions must account for both work
and leisure; exercise/strength sessions are not added again. [FAO Human energy
requirements, chapter 5](https://www.fao.org/4/y5686e/y5686e07.htm).

Important method distinction: FAO defines PAL against basal metabolic rate,
whereas Mifflin predicts resting energy expenditure. Using these intervals with
Mifflin is a proposed approximation combining sources, not a method validated
by either cited source. Adoption of this combination is an explicit decision.
An alternative is a consistent FAO basal-rate method; do not switch algorithms
silently under the same method identifier.

Display `REE × category minimum` through `REE × category maximum`, rounded
outward to 50 kcal. This is an activity-scenario range, not a statistical
confidence interval or a promise that actual needs fall within it. A member may
choose a daily reference from the preview or enter their own; do not automatically
save the midpoint. Expose method/version, inputs and assumptions on demand.

## Proposed applicability and validation

For this first product scope, offer estimates only to adults 19–64 who confirm
that a general maintenance estimate is appropriate. This narrower age window
is a proposed product boundary, not a claim of validation for every member.
Do not offer automatic estimates for pregnancy/breastfeeding or a context needing
individual clinical nutrition guidance. Keep manual tracking accessible without
collecting diagnoses or saving the eligibility answers. NIDDK's planner similarly
excludes under-18 and pregnant/breastfeeding use, but its different dynamic model
is not implemented here. [NIDDK scope](https://www.niddk.nih.gov/health-information/weight-management/body-weight-planner).

Proposed technical input envelope: age integer 19–64, finite height 120–230 cm,
finite explicitly reviewed weight 35–250 kg, a selected coefficient and a selected
activity category. These bounds are UI engineering limits, not healthy ranges or
the research population's proven validity envelope. Outside them, omit the
estimate and retain manual tracking. Missing or invalid values never default.

Reject nonpositive/nonfinite REE or results outside the existing energy-target
storage bounds; never clamp a result into an apparently supported estimate.
Units convert explicitly before arithmetic. No calculation uses meal calories
as evidence of actual intake or automatically recalibrates from incomplete logs.
New weights do not change a confirmed reference; recalculation is a separate
member action showing old/new estimates and requiring confirmation.

## Proposed encrypted compatibility contract

Existing `nutritionTargets` remains readable unchanged. Its current manual-only
meaning must be deliberately revised in M1 before adopting derived references.
Add optional, versioned goal-setup data to the encrypted snapshot only:

- A reviewed profile snapshot containing age-at-calculation (not date of birth),
  height, reviewed reference weight, chosen equation coefficient and whole-day
  activity category. Do not collect unused exercise/strength counts in v1.
- Current goal with stable ID, intent, confirmed time, optional member-entered
  target/range, reviewed reference weight and source metadata for each derived
  daily reference. Maintain does not create a medically ideal weight band.
- A bounded history of up to 12 previous phases, created only by explicit New
  phase. Edit changes the current phase; no silent phase creation/recalculation.
  Replacing a phase previews the effect on current daily references.
- Energy method ID, input snapshot and activity-scenario range where a derived
  reference was explicitly accepted. Manual overrides remain identified as such.

Save profile/goal/reference changes atomically; failure rolls back all of them.
Legacy vaults without these fields open with no profile/history and their existing
targets intact. Do not infer a historical goal or profile from recorded weights.
Archive export/import must gain a reviewed versioned representation with strict
validation before this persistence ships; old archives restore absence rather
than fabricated defaults. Unsupported newer goal versions must not be silently
dropped. Round-trip, rollback, legacy, malformed and replacement tests are needed.

Member deletion removes the profile and selected goal history without removing
meals/weights; disclose separately whether current daily targets will remain or
be removed before confirmation. Clear-all/recovery follow the existing vault
policy. Profile, intent, history and calculation inputs stay excluded from current
cloud requests, prompts, analytics, logs and debug traces.

## States and acceptance before enabling

Setup is offered after vault unlock, can be skipped, and has one explicit final
confirmation. Inputs remain transient until then. Cancel writes nothing; changed
edits confirm leaving. Lock/background clears sensitive transient setup. Rotation
preserves the unlocked task; process restart never calculates or saves again.
Unsupported/missing-input states offer manual targets without fabricated values.

Calculator tests need coefficient/unit fixtures, category boundaries, rounding,
invalid/unsupported inputs, no default coefficient, and no automatic deficit or
protein calculation. Privacy/archive tests need source metadata, deletion and
atomic failure cases. Phone checks need skip, edit, decline equation choice,
cancel, explicit confirm, recalculate, new phase, larger fonts, TalkBack,
keyboard, rotation and save/reopen. These are open gates, not passing evidence.

Approval adopted `maintenance-msj-pal-v1` and its stated approximation and scope.
The app offers setup from the unlocked Goals view rather than forcing a wizard
on existing members. The optional first-run introduction is now implemented after
successful new-vault creation; skip/dismiss saves nothing, and existing members
can preview it through About goal setup. Automatic creation routing still needs
a separate fresh-vault physical check. The
implemented encrypted payload uses data version 2 when goal data exists, and
goal representation version 1; legacy absence is preserved. Historical phases
can be deleted individually, and Delete profile and all goal phases retains
daily references as member-entered. Each stored calculation profile belongs to
an accepted estimate; manual overrides stop retaining that calculation snapshot.
