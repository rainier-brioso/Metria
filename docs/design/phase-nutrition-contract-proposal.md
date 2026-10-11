# Optional phase nutrition suggestions: proposed implementation contract

Date: 2026-10-08. Status: member approved with “go ahead!”; implemented and build-verified; phone acceptance pending.
The member requested macro research, then authorized continuing the next slice.
The exact policy below is approved as an optional starting-proposal method,
with its stated limits. See [research and primary sources](phase-nutrition-research.md).

## First slice and boundaries

Offer optional, deterministic, on-device macro suggestions for an explicitly
reviewed daily calorie budget. Keep the existing maintenance calculation and
explicit endpoint selection. For Gain/Lose, a member-entered calorie budget can
also receive macro suggestions; selecting the phase does not yet apply an
automatic deficit/surplus in the macro-only slice. The separately approved
[phase calorie contract](phase-energy-contract-proposal.md) adds bounded reviewed
starting adjustments; its implementation is tracked independently. Just track can skip all references.

This first slice changes macro proposals, not the energy method. Full phase-based
energy adjustments remain a separate method decision: research does not establish
one safe subtraction/addition for every supported profile. A daily budget used
for macros is not necessarily estimated maintenance or an adequate individual
energy intake. Source labels must preserve that distinction.

No AI, account, cloud, dependency, diet plan, deadline, forecast or automatic
recalculation is introduced. The local vault remains the source of truth.

## Inputs and focused questions

Reuse reviewed calories and weight when present, but do not require an accepted
maintenance estimate to request macros. A manual-calorie member can review a
weight specifically for the macro calculation. Never substitute target weight
or the latest recorded weight silently.

Optional Training row opens a focused selection:

- General wellness / no regular training.
- Regular strength training.
- Other regular exercise.
- Demanding endurance or mixed training.
- Prefer manual references.

Strength means resistance exercise, explained briefly on demand. No exercise
frequency, duration or diet-type field is collected in this slice: the proposed
method does not use them. A weekly count does not justify a distinct protein
multiplier. Existing whole-day activity stays visible under the calorie inputs;
training affects macro selection only and adds no exercise calories.

Previously saved answers may prefill an edit as reviewed inputs. A new phase
shows them for confirmation, rather than treating them as indefinitely current.
Unknown training context never becomes a Yes/No default. No sensitive answer is
kept in Android saved-state bundles, logs, traces or assistant/cloud requests.

## Exact candidate policy to approve

Candidate method IDs: `macro-balanced-v1`, `macro-exercise-v1`.
All suggestions require a positive finite reviewed calorie budget `E`, reviewed
weight `W` in kg, a fresh transient general suitability confirmation, and explicit
mode choice. Ask pregnancy/breastfeeding applicability separately; never infer it
from an equation coefficient. If applicable, route to care-team references, clear
generated values and hidden source snapshots, and retain explicit member-entered
values. Back cannot restore cleared sources. Manual/macro-only entry asks the same
applicability question, optional for manual saving and required for automatic
proposals. Answers remain transient. Current age/height/weight
engineering bounds remain necessary but are not claims of clinical validity.
Clinical nutrition contexts retain manual tracking.

Balanced mode:

`P = 0.20 × E / 4`
`F = 0.30 × E / 9`
`C = 0.50 × E / 4`

Require `P >= 0.83 × W` before offering the complete balanced suggestion.
If not, explain that these inputs need an individually chosen split and retain
manual entry. Do not silently increase calories or protein. The 20/30/50 split
is a proposed starting option, not an EFSA-mandated individual target.

Exercise mode:

- Regular strength training: proposed protein coefficient `r = 1.6 g/kg/day`.
- Other regular exercise: proposed coefficient `r = 1.4 g/kg/day`.
- Demanding endurance/mixed training: no automatic sports suggestion in v1;
  manual references remain available because workload-specific carbohydrate
  guidance is not implemented.

`P = r × W`
`F = 0.30 × E / 9`
`C = (E − 4 × P − 9 × F) / 4`

The precise coefficients and fat allocation are editable proposal policy,
informed by the research, not universal measured requirements. For this initial
product scope, offer the automated exercise split only when its carbohydrate
share is 45–60% and protein share is 10–35%. A combination outside the offer
envelope gets a manual fallback, not a diagnosis or a claim that the values are
unsafe. Do not clamp protein, change weight basis or invent lean mass to make it
fit. No extra protein coefficient is applied merely because intent is Lose/Gain.
This intentionally bounded offer does not promise sport-specific optimization.

Validation uses unrounded values, canonical kg and the existing target storage
bounds. Keep unrounded values through computation, round each accepted macro to
the nearest whole gram with a documented positive half-up rule. The accepted
target values are the exact displayed values; reproducibility retains the
unrounded calculation and rounding version. A planning 4/4/9 mismatch up to 9 kcal
from rounding is disclosed as approximate. Actual catalog meal energy is unchanged.

## Review screen and state contract

Purpose: review one daily-reference proposal. One dominant Use suggestions action.
Show kcal followed by three readable gram metrics, a Suggested label and concise
training context. Edit references and How calculated are secondary. Avoid a
confidence interval for macros or an apparent measured daily requirement.

Idle: offer Calculate suggestions only after the used inputs are valid. Computation
is synchronous and small; no artificial loading animation. Missing/unsupported:
show the specific missing input or manual alternative; never zeros. Successful:
preview exact proposed values without a write. Partial: entered fields remain
visible, but a complete automatic proposal is not fabricated from missing inputs.

Use suggestions updates the transient setup draft, then existing final confirmation
shows current versus proposed values. Save freezes that exact proposal and keeps
the existing duplicate prevention, retry, rollback and saved-summary journey.
Calculate, Use, Back, Skip and dismiss never write by themselves.

Edits to calories, reference weight, mode or training invalidate unaccepted
suggestions. Previously entered overrides are never silently replaced; recalculation
previews the replacement. Editing one accepted macro makes that field manual,
leaves the others' source honest and shows that the result is a custom split.
Historical detail uses its own saved input/method/overrides, never current profile.

Keep editing restores the visible draft; cancellation writes nothing. Lock/background
clears sensitive transient content. Configuration behavior follows the existing
unlocked-task contract; process recreation never recomputes, reaccepts or saves.
Keyboard-aware scrolling, pinned action, textual graph equivalents, 48dp targets,
scaled-font layouts and TalkBack states are mandatory phone checks.

## Source and persistence design

Add an optional macro-proposal provenance object independent of
`acceptedEstimate`: a manual calorie budget can still have derived macro sources.
It includes method/version, training context, age at calculation for applicability,
reviewed kg reference, calorie input, unrounded output, rounding
version and which displayed fields still retain proposal provenance. No diagnosis
or suitability-answer storage. Coefficients are fixed by the validated method and
training choice and shown on demand; they are not arbitrary archived instructions.
Inputs belong to that phase; deletion removes them.

Do not overload maintenance `acceptedEstimate` or label macros as manual after
silently deriving them. Macro-only edits continue to preserve maintenance calorie
provenance. Manual calorie edits retain the existing maintenance-source removal
and invalidate calorie-dependent macro suggestions for explicit re-review.

Proposed goal representation version 2 reads legacy version 1 with absent macro
provenance; emits version 2 only when the new representation is used. Unsupported
versions reject before replacement. Preserve existing goals/targets exactly on
legacy import. The final codec specification must define exact fields, constraints
and current-reference consistency before code; no implicit archive migration.
One atomic commit saves phase, references and provenance, rolling all back on error.
Delete profile/all phases retains reference numbers as explicitly manual, consistent
with the existing deletion disclosure. Source data stays excluded from cloud,
assistant, analytics, diagnostics and logs.

## Sequential implementation and evidence

1. Approve exact policy/scope; deliberately revise the maintenance-only M1 contract,
   architecture and relevant flow/screen contracts before enabling new behavior.
2. Implement pure calculator and fixtures, including missing/unsupported/conflicting
   inputs, reference boundaries, units, decimals, finite values and rounding.
3. Implement independent source model and strict versioned codec with legacy,
   round-trip, malformed/unknown-version, per-field override, deletion, privacy and
   atomic rollback tests. No overlapping write tasks.
4. Add training selection and focused proposal review, connect staged confirmation
   and exact saved summaries; test invalidation and repeated-save behavior.
5. Run the complete Android test/build gate, independent source review and combined
   in-place installation. Capture authorized temporary phone before/after and
   difficult-state evidence, then remove raw member captures. Exercise manual and
   estimated calories, missing weight/context, fallback, Edit/Skip/Cancel/Keep
   editing, kg/lb, keyboard/font/insets/TalkBack, lock, retry and save/reopen.

Implementation evidence: the complete Android gate passed 389 tests with zero
failures/errors and assembled the APK on 2026-10-08. Independent backend and UI
source review found no confirmed material defects. The strict versioned codec,
per-field override/invalidation, encrypted round-trip and rollback fixtures passed.
Installation of this macro build is pending a connected phone; existing installed
data has not been changed by this build verification. Physical acceptance remains open.
