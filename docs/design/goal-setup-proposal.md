# Guided goals proposal — 2026-10-06

Status: original mockup extension, followed by member-approved maintenance-only
calculation/history scope on 2026-10-06. The implementation follows
`goal-calculation-contract-proposal.md`; unused exercise/strength counts and
protein recommendations remain outside that scope. The same
`prototypes/meal-entry.html` includes the earlier Goal setup and Your goal concept.
Reference app images remain uncommitted.

## Journey

The member's six-page goal reference is tracked view by view in
[Goal views: reference inventory and implementation plan](goal-view-roadmap.md).
That roadmap records all eleven reference views, current implementation,
dependencies and explicitly deferred ideas. It supplements this proposal without
changing the approved calculation or storage contract.

1. Choose Maintain, Gain, Lose, or Track without a target.
2. Optional personal details: age, height, current weight and sex used by a
   future estimate. Explain the purpose of a sex input without equating it
   with identity; declining must leave manual targets/tracking available.
3. Describe everyday movement, exercise frequency and strength sessions as a
   subset of total sessions. Do not count strength exercise twice.
4. Review the chosen direction, reference/target, optional chosen range and
   assumptions; explicitly Confirm goal. Skip setup at any point.

The prototype starts with a fictional maintenance goal around 70 kg. Its
maintenance suggestion uses only the entered current weight. Gain/loss does
not infer a target, rate or deadline. A range is member-entered and is not a
medically recommended range. Daily calorie/protein estimates are deliberately
unimplemented placeholders; the reference app's underlying algorithm is unknown.
Latest recorded weight remains separate from the reference. Adding a weight
does not silently change a confirmed goal.

Today surfaces a compact goal summary. Progress links to the goal preview.
Your goal contains current direction, latest record, optional nutrition
references and previous phases. Edit updates the current phase; New phase
replaces it only after confirmation and retains its prior context. No nutrition
score, red/green judgment or adherence rating is introduced from incomplete
meal records. Target-free tracking remains a first-class choice.

## Decision record for Android implementation

The current-target summary/manual editor is now an Android slice using the
existing encrypted fields. The member subsequently adopted the maintenance-only
[calculation and storage contract](goal-calculation-contract-proposal.md), with
an explicit M1 revision, versioned encrypted profile/history, and manual
weight-change and macro references. The applicability and privacy requirements
below describe why that narrower scope was selected. Physical acceptance remains
open.

The current product contract allows member-entered goals. Personalized
calorie/protein suggestions expand that contract and require a reviewed,
deterministic calculation specification and deliberate M1 update. A language
model must not calculate or prescribe targets.

Specify equation/source, uncertainty, activity assumptions, supported adult
population, applicability limits, units, rounding, validation, recalculation
policy and manual overrides. Strength frequency alone does not establish a
protein requirement. Do not derive a medically ideal weight or create a
deficit/surplus automatically merely from a chosen direction. No daily target
may be treated as exact intake, and missing records remain unknown.

Official reference checked 2026-10-06:
[NIDDK Body Weight Planner](https://www.niddk.nih.gov/health-information/weight-management/body-weight-planner)
describes personalized calorie/activity planning and limits that tool to adults,
excluding pregnancy/breastfeeding. This is a scope reference, not adoption of
its calculation. Metria's eligible population and unsupported-context behavior
need explicit definition before automatic estimates are enabled.

Profile and goal-history persistence require a separately reviewed encrypted
schema/archive compatibility design. Collect only fields needed by the chosen
method, allow editing/deletion, and avoid implying entries are required for
ordinary meal tracking. Profile answers and goals remain excluded from the
current cloud payload. Setup must follow vault creation/unlock if answers will
be persisted; no unencrypted pre-vault health profile.

Define unsaved-edit cancellation, range/target consistency, omitted inputs,
invalid inputs, local calculation errors, explicit confirmation, lock/background
clearing, rotation and restart without replay. First-run suggestion is a draft,
not an automatically saved goal. No cloud, biometric or Android changes are
part of this mockup.

## Evidence

The previous meal-entry mockup was positively reviewed by the member. This
extension awaits visual review in the member's browser. The tool's local-file
protocol restriction still prevents automated visual inspection. JavaScript
syntax and scripted state smoke checks are local design checks, not Android
or physical acceptance. No dependency, commit or push is part of this slice.
