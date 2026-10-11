# Phase calorie adjustment: next slice

Date: 2026-10-08; implementation updated 2026-10-09. Status: member-approved policy;
implemented, build-verified and installed in place; phone acceptance pending.
After reviewing the concrete proposal, the member approved the first-slice
defaults and preferred a 10% loss adjustment, asking for reference-backed
documentation. Implementation stages proposals locally and requires final member
confirmation before changing saved references.

## Outcome and proposed first version

During Lose/Gain setup, offer an optional phase calorie starting proposal after
the member reviews the existing maintenance inputs and explicitly selects a
maintenance reference. Show maintenance, adjustment and final daily calories
together. Calculate macros from the resulting reviewed budget using the already
approved macro method. Use changes the setup draft; final confirmation is the
only write. Maintain and Just track keep their existing behavior.

Approved product starting choices:

| Phase | Proposed adjustment | Arithmetic |
| --- | --- | --- |
| Lose | 10% below the selected maintenance reference | `E = M × 0.90` |
| Gain, regular strength training | 5% above the selected maintenance reference | `E = M × 1.05` |

## Decision record: why 10% for Lose

The member prefers a smaller initial subtraction than the 25% adjustment they
reported seeing in Fitia. That competitor behavior is member-reported context,
not a verified universal Fitia policy or clinical evidence for either percentage.

Metria chooses 10% as an optional, editable starting proposal to limit the initial
change from an uncertain maintenance estimate. This is a product rationale,
not a finding that 10% is optimal, adequate, safer or more effective for every
person. The references below support individualized review and explain why
larger deficits from supervised treatment programs are not copied universally.
None of them mandates Metria's exact 10% default.

For the fictional 2,400 kcal maintenance reference, 10% subtracts 240 kcal;
25% would subtract 600 kcal. The smaller initial subtraction is a mathematical
comparison, not a prediction of weight change or a personal intake recommendation.
The member sees and explicitly accepts the reference; recorded weights never
silently increase the deficit or recalculate the budget.

These percentages are Metria product choices, not clinically validated individual
prescriptions, NICE recommendations or promises of a particular weekly change.
The smaller surplus is informed by limited strength-training evidence; general
weight gain, underweight support and demanding sports remain manual in this v1.
The deficit is deliberately smaller than the commonly studied obesity-treatment
deficits; that does not establish that it suits every member.

No automatically accepted midpoint, extra exercise calories, adjustable speed
slider, arbitrary larger percentage, goal-date calculation or predicted kg/week.
Edit calories remains available as manual entry. A user wanting a different
adjustment reviews and enters their own calorie reference; the app does not
present it as a calculated recommendation.

For a fictional `M = 2,400 kcal`, the previews are Lose `−240 → 2,160 kcal`
and Gain `+120 → 2,520 kcal`. Those examples explain arithmetic only.

## Evidence and applicability

[NICE NG246, dietary approaches](https://www.nice.org.uk/guidance/ng246/chapter/Physical-activity-and-diet)
recommends individualized dietary approaches and support/follow-up for energy
deficits. Its 800–1,200 kcal low-energy diets require specialist contexts; a
consumer calculator must not interpret the threshold as proof that all higher
intakes are adequate.

[NHLBI adult overweight/obesity evidence review](https://www.nhlbi.nih.gov/sites/default/files/media/docs/obesity-evidence-review.pdf)
includes 500–750 kcal deficits and individualized calorie levels in treatment
programs. That does not justify subtracting 500 kcal for every Lose phase.

[Helms et al., 2023](https://link.springer.com/article/10.1186/s40798-023-00651-y)
compared maintenance, 5% and 15% surplus conditions in trained lifters, with 17
completers. Faster gain primarily related to increased skinfolds. The study's
population, supervision and duration limit generalization; 5% is not an optimal
surplus demonstrated for every adult.

[NIDDK Body Weight Planner scope](https://www.niddk.nih.gov/health-information/weight-management/body-weight-planner)
excludes children and pregnancy/breastfeeding. Metria is not implementing its
dynamic model and must not borrow its authority for fixed-percent forecasts.

Approved bounded offer rules:

- Existing age 19–64/profile/unit bounds and a fresh transient general suitability
  confirmation. Do not offer the general method when individual clinical or
  eating-disorder support is needed. Ask pregnancy/breastfeeding applicability as
  a separate transient question; never infer it from the equation coefficient.
  If it applies, route directly to care-team references and clear generated values
  and source snapshots while retaining explicit member-entered references. Back
  cannot restore cleared sources. Do not persist diagnoses or answers.
- Lose: reviewed current BMI at least 25 and below 40. This is an intentionally
  narrow product offer boundary, not a diagnosis or a full ethnicity-aware risk
  classification. Do not label members as overweight/obese in the UI. Below or
  beyond this envelope, offer manual/professionally chosen references.
- Gain: reviewed current BMI at least 18.5 and below 30, plus explicit regular
  strength training. Underweight weight restoration is outside this tool.
- Demanding sports/whole-day vigorous activity: manual references in this slice.
  The simple method cannot assess workload-specific energy availability.
- Do not offer an adjusted result below 1,500 kcal/day, or a Lose adjustment
  above 500 kcal/day. These are conservative product stopping boundaries, not
  clinically validated personal minimums or a guarantee of adequacy. Never clamp
  the proposal to a floor or silently select a smaller adjustment to make it pass.
- Existing maximum stored-calorie bound still applies. Missing/unsupported or
  conflicting inputs give a specific explanation and retain manual/skip paths.

The boundary numbers and defaults above are the approved first-slice product
decision, not validated clinical cutoffs. A generic disclaimer cannot turn
unsupported arithmetic into an individually appropriate nutrition prescription.

## Focused screen and transitions

Entry: the daily calorie review for a Lose/Gain phase offers “Phase calories”.
The focused sheet contains three compact rows:

1. Reviewed maintenance reference with the saved approximation range.
2. Phase adjustment, sign, percentage and kcal difference.
3. Proposed daily calorie reference, visually dominant, followed by the three
   optional macro values after their separate calculation/review.

One dominant action: Use calorie proposal. Secondary actions: Edit manually,
How calculated, Back/Skip. Copy: “A starting estimate. Review it as your needs
change.” Do not call the reference a measured need or an exact weight-change plan.
Keep advanced inputs and method details out of the summary card.

Idle: explicit maintenance selection and valid used inputs required. Calculation
is synchronous; no artificial loader. Missing/unsupported: explain the missing
input or fallback, retain entered references. Success: preview before any write.
Partial: no fabricated macros or zeros. Cancel/Skip: prior draft remains intact.
Use: stage calories and invalidate derived macros that used a different budget,
while keeping manual macro overrides. Recalculate macros only at explicit request.
Final review shows old and proposed references and the source of each field.
Save/retry uses the existing frozen proposal and atomic rollback contract.

Changing phase direction, maintenance inputs/reference or used training context
invalidates the phase-energy proposal. It also invalidates dependent derived
macros; manual overrides survive. Weight-only editing must explicitly warn and
route to reference review when it changes direction on a phase with an accepted
adjustment. It must not retain a Lose source under Gain or silently recalculate.

Typing calories, even the same number, makes calories manual and removes phase
energy provenance. Editing a macro only leaves calorie provenance intact.
New phase shows old inputs for review without carrying acceptance automatically.
Saved/history views use that exact phase's inputs and method, never today's weight.

Keep editing must restore a visible sheet. Insets, keyboard scrolling, 48dp
targets, font scaling, textual alternatives and TalkBack follow existing gates.
Lock clears transient health context; recreation never recalculates or saves.

## Persistence and source honesty

Add independent optional `phaseEnergySuggestion` to GoalPhase rather than putting
the adjusted value into `acceptedEstimate`. Current acceptedEstimate requires a
calorie value within the maintenance interval and cannot represent adjusted energy.

The new source retains a canonical maintenance estimate/profile, the explicitly
selected maintenance endpoint, intent, required training context, fixed method ID,
unrounded result and displayed accepted whole-kcal result. Suggested method ID:
`phase-energy-modest-v1`; positive half-up whole-kcal rounding. The displayed
adjustment is accepted calories minus selected maintenance calories so rows sum.
Coefficient, offer boundaries and method rules are fixed in code, not arbitrary
numbers or executable instructions imported from an archive.

Validate by canonical recomputation, input/intent consistency and exact agreement
with current calories. Macro provenance uses the accepted displayed calorie budget.
Archive version 3 reads legacy v1/v2 without inventing energy sources and writes
v3 only when a current/historical phase uses this representation. Unknown keys,
types, versions, nonfinite values and tampering reject before replacement.
Keep maintenance and phase-adjusted sources mutually exclusive for current calories;
retain maintenance evidence inside the phase-energy source for How calculated.

Sources remain encrypted and excluded from assistant/cloud payloads, diagnostics
and logs. Delete profile/all phases removes source inputs while retaining reference
numbers as manual, consistent with the existing disclosure. Current/history profile
presence reporting must include the new source. Atomic save/rollback is unchanged.

## Implementation order and evidence

1. Policy approved; deliberately align the M1/architecture/flow/screen contracts
   before enabling the behavior.
2. Pure calculator and fixtures: scope, units, percentages, rounding, thresholds,
   missing/conflicting inputs and no exercise double-counting.
3. Independent provenance, strict v3 codec, legacy preservation, malformed/tamper
   rejection, deletion, history and encrypted atomic rollback.
4. Focused setup preview, dependency transitions, source-aware final confirmation,
   current/history details and direction-edit handling.
5. Independent source review and full Android unit/build gate; install in place
   once a phone is connected. Verify Keep editing, kg/lb, keyboard/font/TalkBack,
   cancellation, lock/rotation, save/reopen, legacy phases and manual overrides.

Implementation now includes the pure calculator, independent energy source,
strict v3 codec and source-aware review/current/history UI. Automated gate and
installation results are recorded in implementation-checkpoints.md; those results
do not close physical acceptance. The final build was installed in place on the
target Samsung SM-S928B / Android 16 on 2026-10-09; installed APK hash was verified.

Final automated gate, 2026-10-09: 405 tests, zero failures/errors, APK assembled;
independent backend, full integration and targeted setup review found no remaining
confirmed defects. No production dependency was added. Physical acceptance is pending.
