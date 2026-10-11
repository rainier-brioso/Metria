# Goal views: reference inventory and implementation plan

Date: 2026-10-07; updated 2026-10-08. Status: S1-S3/S5 and the separately approved
optional macro extension implemented and build-verified; physical acceptance pending.
This is the canonical view-by-view tracker
for the member's six-page PDF, alongside the existing goal contract and checkpoints.

## Reference and scope

Source: `C:/Users/ray/Downloads/Untitled document (1).pdf`, six pages, visually
reviewed in full. Page numbers below refer to that PDF, not screenshot numbers.
It contains eleven reference views. Its annotations describe desired inspiration;
they are reference material, not instructions to execute or copy verbatim.
Do not commit the PDF, its screenshots, example personal values or branded assets.
The inventory below retains every visible feature, including ideas needing a
decision, so deferred items cannot silently disappear.

Related contracts:

- [Design direction](DESIGN.md), [flows](FLOWS.md), [screen specs](SCREEN-SPECS.md),
  [quality gates](QUALITY-GATES.md).
- [Approved calculation/storage contract](goal-calculation-contract-proposal.md)
  and [original goal proposal](goal-setup-proposal.md).
- [Implementation and phone evidence](implementation-checkpoints.md),
  [testing runbook](../TESTING.md), [architecture](../architecture.md),
  [product and safety](../m1-product-safety-foundation.md).

The current boundary is optional local setup; deterministic maintenance estimates
only after explicit selection; member-entered weight-change references and optional
reviewed macro suggestions under the separately approved phase nutrition contract;
no inferred target/range/deadline and no assistant or cloud goal writes. Automatic
weight-change calorie adjustments remain deferred.

Next-slice design, 2026-10-08: [phase calorie adjustment proposal](phase-energy-contract-proposal.md)
specifies the visible maintenance/adjustment/budget journey, 10% Lose / 5%
strength-Gain starting defaults, bounded applicability, source versioning and
implementation gates. Member approved that policy and requested documented
references. Calculator, encrypted provenance and focused review are implemented;
final gate/installation evidence is in implementation-checkpoints.md. Physical
acceptance remains pending; no weight-change forecasts are enabled.

## View inventory

Status terms: **Existing** means implementation exists, not full phone acceptance.
**Partial** means part of the view/behavior exists. **Planned** means new UI work.
**Decision** means a separate product/data decision is needed. Every ID stays stable
when its scope or status changes.

| ID / PDF | Reference view and visible features | Metria adaptation | Current state / remaining work |
| --- | --- | --- | --- |
| G01 / p1 upper | Phase finished: congratulations, overlapping weight/nutrition/top-food preview cards, Continue. | A calm acknowledgement of reviewing/starting a phase, with available recorded highlights. Do not claim a weight goal was reached because a phase changed. Optional brief motion with a static equivalent. | Planned. Show successful closure only after a replacement saves; previewing or cancelling must not end the old phase. Highlight calculations depend on S0/S4. |
| G02 / p1 lower | Previous-goal summary: direction/title, duration, weight endpoints/chart/change, days tracked, best streak, perfect streak; Start a new goal and Keep my weight. | Dedicated phase detail with actual recorded weights, dates, signed change and optional logging consistency. Start new phase opens a draft. Keep tracking returns unchanged; choosing maintenance is a separate explicit setup choice. | Partial: archived goal/reference cards exist, but no recorded-phase recap. Accurate period boundaries need S0. Perfect streak is deferred; logged-day/best logging run needs a stated definition. |
| G03 / p2 upper | Start New Phase confirmation warns the current phase will end; OK and Cancel. | Clear staged replacement: entering setup writes nothing. Final confirmation previews old/new goal and references, archive effect and history limit. Cancel leaves current phase intact. An early explanation can be informational, not another write/mandatory confirmation. | Partial: final review already explains archiving and the 12-phase limit; no focused replacement confirmation view. Improve its hierarchy and labels. |
| G04 / p2 lower | Choose goal: back, icon, short intro, Fat loss, Muscle gain, Weight maintenance cards. | Focused direction cards: Lose weight, Gain weight, Maintain weight, plus Just track. One selected state with icon/text and Continue. No muscle-preservation/body-composition promise. | S2 implemented: direction cards with selected icon/text, followed by optional weight context. First setup, edit, recalculation and new-phase labels are distinct; phone acceptance pending. |
| G05 / p3 upper | Set goal: current-weight row, selectable goal weight, recommended speed, Create my plan. | Compact editable rows for starting reference, target and optional chosen range, opening focused editors. Latest reading remains distinct from confirmed starting reference. Manual daily references stay reachable. | S2 row-based context and focused editor implemented; physical acceptance pending. Recommended speed, automatic deficit/surplus and implied completion dates remain Decision items. |
| G06 / p3 lower | Does this still look right?: editable About you, exercise level, strength training, lifestyle, diet type; Continue. | Review only inputs used by the selected method: age-at-calculation, height, reviewed weight, equation choice, whole-day activity. Tap a row to edit; continue shows changed assumptions/references before confirmation. Manual/declined setup remains available. | S2 used-input review and focused profile/activity editing implemented; physical acceptance pending. Exercise/strength/diet fields remain deferred until a useful specified method needs them. |
| G07 / p4 upper | Success animation/check and Next. | Short accessible Saved state after vault commit, with one Continue action. Reduced-motion/static check equivalent. Save failure stays in review with Retry. | Implemented S3/S5: static saved acknowledgement, optional read-only summaries, terminal save attempt and transient success state. Physical acceptance pending. |
| G08 / p4 lower | Success summary 1: calorie reference with curved range indicator, three macros/bars, carousel dots, Next. | Reviewed daily-reference summary. For accepted maintenance estimates, plot the saved selected value within its saved activity-scenario interval and label its approximation. Manual references have no fabricated interval. Show only entered macro references; missing stays unset. | S5 saved visual summary, method disclosure and explicit Next/Back implemented; physical acceptance pending. No invented macro percentages or swipe-only controls. |
| G09 / p5 upper | Success summary 2: starting weight, target, predicted descending path, trophy/next goal, Continue. | Starting reference and optional member target/range with endpoint marker; real readings only when available. Label target as chosen. No forecast curve, achievement trophy or predicted next phase. Tracking-only state has no invented target. | S5 saved chosen-context summary with kg/lb, static marker and missing/equal/outside-range text implemented; physical acceptance pending. Prediction/timeline/automatic next-goal behavior remains Decision. |
| G10 / p5 lower | Goal hub: back, New, Current card/edit, starting/target progression, Previous card with dates/weights/change and drill-in. | Current phase followed by compact previous-phase rows; open one phase for details. Edit and New phase remain distinct. Daily references/method/data management use progressive disclosure. | S1 implemented: compact rows, selected-ID detail, compact current/reference cards and progressive controls. Full Android gate passed; installation/physical acceptance pending. Historical end dates remain unavailable; dated recaps wait for the period contract. |
| G11 / p6 | Goal card: intent, start/target, bar, Time to goal, Update Progress, detail arrow. Surrounding nutrition-score week/coach banner and statistics. | Compact Today/Progress goal context with recorded weight, member target/range and Update weight. Details opens Goals. Nutrition remains a separate recorded-estimate card. Retain the improved compact Metria style. | Compact Today/Progress implemented; latest Today build installed, phone acceptance pending. Time to goal, nutrition score, perfect-day judgments and promotional coach overlay remain deferred. |

## Proposed Metria journey

1. Today or Progress opens Goals: current phase, references and previous phases.
2. New phase can show the current phase's recorded recap before setup. It is a
   preview; the current phase remains active. Keep tracking returns unchanged.
3. Choose direction, review starting/target/range, then review applicable profile
   and whole-day activity or choose manual references. Each detail edits in a
   focused destination, not a long feed form.
4. Review the proposed daily references, sources and optional weight context.
   Final **Start new phase** explicitly previews the old phase being archived,
   changed references and any oldest-phase removal at the history limit.
5. One atomic save replaces the phase and references. Failure preserves the draft
   and old saved state. Edit current phase preserves its ID and start time.
6. After success, show a calm saved acknowledgement and optional two-part summary
   of daily references and chosen weight context. Continue returns to Goals/Today.
7. Previous-phase details remain accessible without starting another phase.

This deliberately adjusts the reference's ordering: celebrating before replacement
has saved would incorrectly imply the current phase had already ended. No phase
ends automatically when a weight crosses a target, after a timer or through AI.

## Implementation slices and dependencies

### S0 - Baseline, designs and phase-period contract

- [ ] Accept the compact installed Today cards on the phone; preserve issues as
  separate findings rather than claiming the baseline passed.
- [x] Produce original Metria mockups for G01-G11 using fictional records; include
  target-free, missing-data, equal-range and large-text variants.
  See [interactive journey](prototypes/goal-journey.html) and
  [mockup review notes](goal-journey-mockup.md). This is design evidence only;
  hierarchy approval, Android implementation and phone acceptance remain open.
- [x] Approve the view hierarchy/journey before replacing complete screens.
  Member approval: "like it!" after the S0 interactive preview, 2026-10-07.
  S1 starts with the approved compact hub and unknown-period history fallback;
  exact phase periods/recaps received separate approval on 2026-10-10.
- [x] Define whether phase recaps require exact closure boundaries. Current phases
  store confirmedAt/updatedAt, not an explicit endedAt. UpdatedAt is an edit time,
  not a phase end. Deleted/truncated history prevents reliable inference from
  adjacent phases. Never invent a historical period.
- [x] If exact historical recaps are approved, specify closure metadata and the
  encrypted codec/archive migration, legacy unknown-boundary state, validation,
  rollback and deletion semantics before implementation. Do not silently expand
  persistence from a visual reference. UI-only slices can proceed independently.
- [x] Define recap calculations outside composables: phase interval inclusivity,
  local dates/timezone, first/last actual records, absent/single reading, meals with
  partial/missing nutrition, and meaningful denominators. Recorded-day calories
  must not be called average total intake. Count logged days once; define which
  records qualify and label streaks as logging, never dietary perfection.

### S1 - Goal hub and historical navigation (G10, G11)

- [x] Compact current goal and previous-phase rows, with one main detail purpose.
- [x] Dedicated selected-phase detail; progressive reference/method disclosure.
- [x] Separate Edit current, New phase and data-management actions.
- [x] Retain no-goal/history-empty, deleted-history and history-limit states.
- [ ] Check Today/Progress -> hub -> detail -> Back preserves context and units.

S1 code/build evidence: 2026-10-07, 365 unit tests with no failures/errors and
APK assembly; independent source review found no remaining confirmed defect.
Checked implementation items do not close phone layout/navigation/accessibility
gates. Installation is deferred to the coherent slice batch as requested.

Depends on approved layouts; dated historical recaps wait for S0's period decision.
Primary files: `ui/screens/GoalSetupScreen.kt`, goal summary components and
`ui/components/ProgressGoalCard.kt`. Extract newly material screens under
`ui/screens/`; keep vault/model logic outside them and new UI out of MemberShell.

### S2 - Focused setup and input review (G04, G05, G06)

- [x] Direction selection as calm cards, including Just track.
- [x] Starting-reference, target and range rows with focused editing and units.
- [x] Review retained calculation inputs as editable summary rows; avoid unused
  exercise/strength/diet fields and double-counting activity.
- [x] Show old/new references and clear manual versus accepted-estimate sources.
- [x] Keep first setup, editing, recalculation and new-phase mode explicit.
- [ ] Verify suitability decline, omitted targets, invalid input, keyboard, Back,
  dirty dismissal/Keep editing, rotation and lock/background clearing.

Depends on S0 designs. Reuse GoalWeightEditorSheet, ManualNutritionTargetsSheet,
GoalSetupInput, GoalWeightInput and the approved MaintenanceCalculator. A direction
does not itself choose energy/macros or a target. Changing a weight does not
recalculate references. A manual override retains its existing provenance rules.

S2 code/build evidence: 2026-10-08, 371 unit tests with no failures/errors and
APK assembly after final review fixes. Independent source review found no remaining
confirmed material defect. The checked items describe implementation; keyboard,
font scaling, dismissal, restoration and suitability interaction acceptance on the
phone remain open. Installation is deferred to the coherent slice batch.

### S3 - Staged new-phase transition (G03; foundation for G01)

- [x] Clear review/confirmation with old/new phase and reference differences.
- [x] Cancel at every pre-save step leaves the current phase/history untouched.
- [x] One stable draft phase ID; prevent double-save and duplicate archives.
- [x] Archive only after successful atomic replacement, preserving existing
  history limit and explicit oldest-removal disclosure.
- [x] Failure retains the draft; retry cannot replay an earlier successful save.
- [x] If approved in S0, implement/test exact closure metadata with encrypted
  legacy/archive compatibility before enabling time-bounded history summaries.

Depends on S2 for inputs and S0 for any data-format extension. Preserve GoalPolicy
and EncryptedVault atomic rollback. This is the slice requiring independent review
of persistence/concurrency and recovery tests if its data contract changes.

S3 code/build evidence: 2026-10-08, 374 tests, no failures/errors, APK assembled.
Confirmation freezes the exact reviewed proposal and initial current-phase context;
failed save retries that same proposal and successful save is terminal for the task.
Saved acknowledgement is transient. Independent review found no confirmed material
defect. Existing GoalPolicy/vault atomic replacement and rollback remain unchanged;
closure metadata is not approved or implemented. Cancel/retry/history/accessibility
physical checks remain open for the combined install.

### S4 - Recorded phase recap and acknowledgement (G01, G02)

- [x] Preview active phase as of a captured instant; known historical phases use
  their exact `[confirmedAt, endedAt)` interval. Unknown periods withhold totals.
- [x] Actual weight chart/endpoints and signed change; no synthetic measurements
  or assumption that the latest current weight belongs to a previous phase. Legacy
  midnight weight times are excluded at boundary days and disclosed.
- [x] Recorded nutrition totals include only per-metric full-coverage values and
  contributing-meal counts; partial/missing values are excluded visibly. Do not
  add top foods, averages, adherence scores or streaks in this first slice.
- [x] Logging days count the union of qualifying weight and meal dates once;
  date-only meals qualify only when their whole local day is inside the phase.
- [ ] Keep tracking returns unchanged; maintenance/new goal requires setup and save.
- [x] Acknowledge the member's action without praising weight loss or claiming
  achievement; brief optional motion/static equivalent, no blocking animation.

Depends on the approved `phase-recap-contract.md`. Historical daily references are phase data,
not the current references substituted into a previous phase. Summaries remain
local and use available retained records; do not persist derived health summaries
without a separate reason and contract.

S4 bounded implementation evidence, 2026-10-10: 425 tests, zero failures/errors,
APK assembled, independent review completed and installed in place. Checked
items above mean implementation and automated verification only. Phone layout,
history/recovery interaction and accessibility acceptance remain open; see
implementation-checkpoints.md. Top foods/streaks and a separate pre-setup recap
journey remain outside this first slice.

### S5 - Saved goal summaries and visual polish (G07, G08, G09, G11)

- [x] Success acknowledgement only after a successful save; do not replay on restart.
- [x] Daily-reference summary with saved energy-scenario visualization where valid,
  manual/no-reference fallback and explicit unset macros.
- [x] Chosen weight context with actual endpoints and no future-weight prediction.
- [x] Visible Next/Back/Continue; no swipe-only carousel, no second save action.
- [ ] Check consistency with compact Today/Progress cards, reduced motion, enlarged
  text and TalkBack graph summaries; keep details a tap away.

Depends on S2/S3 saved-state transitions. Finish with the full journey phone check,
including cancel/retry, edit versus new phase, save/reopen and archive recovery.

S5 code/build evidence: 2026-10-08, 374 tests, no failures/errors and APK assembly.
Saved -> daily references -> chosen weight context uses the exact saved proposal,
explicit buttons, unset values and kg/lb display. Source review found no confirmed
material defect. All visuals are static; method inputs disclose on demand. No new
business rules, data format or dependencies. Phone consistency/accessibility gates
remain open. Combined install attempted preflight: no ADB device connected; current
connection details requested. Earlier compact Today APK remains installed.

## Retained decisions and deferred features

### Future slice: estimated time to target (not yet approved for implementation)

2026-10-10: the member asked to retain the idea of a deterministic timeline
based on the chosen target and reviewed phase adjustment. Keep this as a future
planning feature while completing the current goal acceptance work.

- Same inputs must produce the same on-device result; deterministic does not
  mean the date is certain. Target weight and adjustment percentage alone are
  insufficient to predict an arrival date.
- Prefer an optional estimated time range with disclosed assumptions, not a
  promised deadline, achievement claim or automatic phase closure.
- Review a validated dynamic energy/weight model before implementation. Do not
  use a fixed calorie-per-kilogram rule as a reliable forecast. Reference:
  [NIH Body Weight Planner explanation](https://www.niddk.nih.gov/health-information/professionals/diabetes-discoveries-practice/nih-body-weight-planner)
  and [Hall et al., 2011](https://pmc.ncbi.nlm.nih.gov/articles/PMC3880593/).
- Specify applicability, required inputs, uncertainty, unsupported/no-result
  states, calculation version and reference tests. Validate gain separately;
  do not reverse the loss formula or claim a muscle-gain timeline.
- Potential later refinement can use actual recorded weight trends, with a
  separate rule for sufficient data and uncertainty. No automatic calorie
  adjustment, target change, cloud request or inferred adherence is authorized.
- Proposed member copy: Estimated time to target; Assumes your planned intake
  and activity. Exact range wording and computation require design approval.

This note retains the request; it does not supersede the current no-forecast
screen contract or approve a calculation/persistence change.

| Reference idea | Current disposition | Requirement before revisiting |
| --- | --- | --- |
| Recommended speed, deficit/surplus and Time to goal | Deferred from implementation | Explicit product approval plus reviewed deterministic method, applicability, uncertainty, source, overrides, privacy and tests. No model-generated prescription. |
| Automatic macro/protein targets or diet plan | Optional macro suggestions implemented; phone acceptance pending | [Approved phase nutrition contract](phase-nutrition-contract-proposal.md): deterministic balanced/exercise proposals for a reviewed calorie budget, explicit Use and final confirmation, independent encrypted per-field source and strict legacy-readable codec. Complete gate: 389 tests, zero failures/errors. Diet plans and automatic calorie deficits/surpluses remain deferred. |
| Exercise frequency, strength training, diet type | Optional training choice implemented; phone acceptance pending | Optional general/strength/other exercise context affects the approved protein method; demanding training uses manual fallback. Frequency and diet type remain deferred because this method does not use them. No exercise energy is added to the existing whole-day activity calculation. |
| Predicted weight path, trophy and next goal | Replace with chosen endpoints/actual records | A forecast needs a separate validated specification; starting a new phase is not proof of reaching a weight target. |
| Perfect streak and nutrition score | Deferred | Explicit scoring rationale, coverage/error handling and safety review. Logging consistency can be shown after its simple rules are defined. |
| Coach analysis/banner | Later assistant design | Goal screens stay usable independently; no automatic prompt/profile sharing or cloud call. Keep live Nebius work deferred. |
| Congratulations/animation | Planned adaptation | Recognize a saved/reviewed action with nonjudgmental copy, reduced-motion fallback and no delay to navigation. |
| Top foods and recorded nutrition highlight cards | Planned after S0 | Define date bounds/counting and coverage; use only retained local records, never imply complete intake. |

## Verification and maintenance of this tracker

For each slice: update relevant FLOWS/SCREEN-SPECS before implementation, preserve
existing unrelated changes, perform independent review where warranted, run the
complete Android test/build gate and `git diff --check`. Do not add dependencies,
commit or push without authorization. Sequential slices may form one coherent
installation batch, but each Android change still gets the required gate.

Phone acceptance needs portrait/landscape, normal/enlarged text, keyboard-visible
editing, system insets, 48dp targets, TalkBack, empty/partial/error/loading,
cancel/Keep editing, rotation, lock/background and save/reopen. Persistence changes
also need legacy/malformed/versioned archive, deletion and failure rollback tests.
Install in place with existing-data retention; biometric unlock remains a member
action. Keep authorized raw screenshots temporary/outside the repo and only retain
redacted approved evidence. Builds and installation do not mark physical gates passed.

After each slice, record its G IDs, implementation files, gate/build identifier,
phone results, evidence and remaining gaps in implementation-checkpoints.md. Mark
an item complete only with its appropriate evidence. Keep deferred IDs in this
document and update their disposition explicitly when the member decides.

Installation update, 2026-10-08: combined S1-S3/S5 build installed in place on
Samsung SM-S928B / Android 16; installed APK hash matches the 374-test build
recorded in implementation-checkpoints.md. Cold launch succeeded in 991 ms.
Physical acceptance remains pending.

The member approved S4's exact period and recorded-recap contract on 2026-10-10.
Implementation is bounded by `phase-recap-contract.md`; physical acceptance still
requires an in-place phone build and the relevant history, deletion, rotation,
accessibility and restore checks.

S0 update, 2026-10-07: the interactive mockups cover all eleven IDs. The member
approved S4 exact period/recap behavior on 2026-10-10; see
`phase-recap-contract.md`. Earlier unknown-period-only notes describe the original
S1 boundary and are superseded for S4. Physical acceptance remains separate.
