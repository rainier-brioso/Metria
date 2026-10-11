# Companion UI implementation checkpoints

## Final design review and first Android slice — 2026-10-06

The member approved starting implementation after reviewing the shared mockup.
The visual direction is usable for staged implementation. The review identified
these boundaries before claiming the mockup as a production specification:

- First-run goal calculation remains a separate deterministic method, eligibility,
  uncertainty and persistence decision. Profile mockups do not establish it.
- Editable voice transcription needs local runtime feasibility; current Speak
  continues to use the working audio-to-assistant path rather than pretending
  a new transcription screen already exists.
- Capture tips and preview can reuse the existing camera/photo-picker lifecycle.
  Optional description needs its local request/validation contract before wiring.
- Ingredient and whole-portion edits must keep source/match associations correct;
  raw/cooked weight cannot be converted using an invented yield.
- The member clarified that nutrients present in the database must be shown.
  Unavailable applies only to missing values, with coverage evaluated separately
  for each nutrient. The bundled catalog already contains names, units and values;
  no new data source or network lookup is needed for this first slice.
- Third-party screenshots and raw member/device data stay outside commits.

### Implemented

Today groups explicit stored meal types into Breakfast, Lunch, Dinner and Snacks.
Each has a quiet Add action. Untyped historical entries stay under Other meals;
no time-based classification or vault migration occurs. The extracted AddMealSheet
offers meal photo (existing take/choose flow), Speak, Type and Manual entry.
Chosen type is visible/editable in review, survives expected recreation, and
clears on lock/navigation/dismiss/save. Photo/manual saves initiated here return
to Today; Chat retains its existing save confirmation and View meal behavior.

FoodCatalog retains nutrient metadata, including rows without public nutrient
numbers using their stable USDA nutrient IDs. A pure display calculator scales
each available nutrient by selected grams; reported zero remains zero and absent
values do not become zero. Unlike units, invalid amounts and invalid portions
are excluded. An expandable shared card shows available selected-match nutrients
in meal review and catalog-backed saved-meal editing, with source release, units
and partial food coverage. Nutrient values can appear even where a food lacks
complete macros. Unconfirmed possible ingredients remain excluded.

These are live draft catalog values, not new persisted nutrition fields. Manual
and label sources are not silently replaced. Existing saved macros, encrypted
vault/archive formats, deterministic reviewed saving and correction learning
remain on their existing paths. No runtime dependency or cloud route was added.

### Automated evidence

- Complete command from native-android:
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon`.
- Passed: 316 unit tests, 0 failures/errors, APK assembled.
- Six new tests cover parsing source metadata/unnumbered nutrients, scaling,
  reported zero versus missing food coverage, micronutrients without macros,
  unlike units, and invalid values/portions.
- `git diff --check` passed. Parent commit: `c4cf506`; UI slice uncommitted.
- No Compose instrumentation infrastructure is configured; no production or
  test dependency was added without approval. Automated arithmetic/contract
  tests do not establish UI interaction acceptance.

### Physical acceptance — pending

No target device was inspected or installed in this slice. The installed app
and its encrypted data were untouched. Before/after evidence is still required:
Today populated/empty/untyped sections; Add Back/cancel; photo/library entry;
manual and Chat type carry-through; save/reopen and correct return destination;
available/partial/unavailable nutrient details; larger fonts/TalkBack; rotation,
keyboard and system insets; lock clearing and save failure.

No screenshot or visual test is claimed from code inspection or the passing build.
Obtain explicit in-place installation approval under AGENTS.md before phone
verification. Do not clear, uninstall, downgrade or bypass biometric unlock.

### Next bounded work

Physical acceptance of this slice, then the photo preview/tips/optional description
slice. Dedicated voice and meal-detail layout/portion controls follow separately.
Goal calculation and profile/history schema remain pending decisions. Nebius
token setup and real provider tests remain deferred as requested.

### Authorized in-place installation — 2026-10-06

- Member explicitly requested Install and supplied current wireless pairing
  details. Pairing and connection succeeded; transient transport details were
  not persisted in the repository.
- Target: Samsung SM-S928B, Android 16. Build: uncommitted first UI slice on
  `c4cf506`, verified by the complete 316-test/APK gate above.
- `adb install -r` succeeded. Force-stop and launch of
  `app.metria/.MainActivity` completed with Status ok.
- Replacement installation retained the existing app identity/data path; no
  clear, uninstall, downgrade, signing change or private-storage deletion was
  performed. Member confirmation of existing records/model is pending.
- No biometric action, screen capture or layout interaction was performed.
  The member must unlock. All physical interaction/accessibility gates above
  remain pending; successful installation is not physical acceptance.

## Photo preview and temporary details — 2026-10-06

The member reported Done and requested continuation after installation. This
does not identify enough states to close the earlier full physical matrix.

Implemented the next approved slice in extracted photo source/preview/progress
components. Capture and library selection now prepare a reduced private preview
without automatically analyzing. Explicit Meal photo / Nutrition label choices,
on-demand framing tips, replacement and cancellation precede Create meal draft
or Read label. The preview fits the whole thumbnail rather than cropping it.
The sheet scrolls with keyboard insets; controls have 48 dp minimum targets and
use existing Material/Metria theme styles. Larger fonts and TalkBack need phone
checks. Photo preparation and failure have explicit states.

Optional meal details are bounded to 600 characters and validated again outside
the UI. A JSON-quoted local prompt treats them as untrusted context, preserves
the existing schema instructions and qualifies hidden ingredients as possible,
low-confidence candidates. This is prompt guidance, not proof that the model
obeys it: strict output validation, visible uncertainty and member review remain
required. Details are not automatically stored as a meal note. Changing mode,
image, cancel, lock or navigation clears them. Label mode omits them.

Preparation uses a generation guard so cancellation/lock cannot restore a
late image or thumbnail. Backgrounding discards unanalysed previews and cancels
meal analysis. Stop analysis and Android Back cancel the active meal request;
late completion cannot replace a newer image state. The external camera/picker
exception remains intact. Orientation uses the existing manifest handling;
process restart never restores/replays image analysis or saves details.

No dependencies, cloud requests, vault/archive schema changes, commits, pushes,
installation or device data access were performed in this slice. Nebius token
configuration and real-provider testing remain deferred.

Automated evidence: the complete `:app:testDebugUnitTest :app:assembleDebug
--no-daemon` gate passed: 319 tests, 0 failures/errors, APK assembled.
Three added tests cover empty-context compatibility, JSON quoting/schema and
hidden-evidence guidance, length bounds and control-character rejection.
`git diff --check` passed. Parent commit remains `c4cf506`; changes are uncommitted.
No Compose instrumentation infrastructure was added; these tests do not prove
model obedience, UI behavior or physical acceptance.

Next bounded task: authorize an in-place install, then exercise
capture/library → preview → explicit analysis → reviewed save; replace/mode
change; preparation cancel; Stop and retry; label flow; background/lock; keyboard,
rotation, enlarged fonts, TalkBack and before/after evidence. Dedicated voice
transcription and meal-detail layout follow separately.

### Photo-preview installation — 2026-10-06

The member approved the proposed in-place install with “lets go next”. The
319-test verified APK installed successfully with `adb install -r` on Samsung
SM-S928B (Android 16). Force-stop and launch completed with Status ok.
No app data was cleared, no uninstall/downgrade/signing change occurred, and
transient device identifiers were not recorded. Existing record/model retention
still needs member confirmation after biometric unlock. No screen capture,
hierarchy inspection or biometric interaction was performed. The photo-preview
physical acceptance matrix above remains pending.

### Reported Create meal draft crash — 2026-10-06

The member reported a crash after choosing a photo and tapping Create meal draft.
Code inspection found an invalid nested vertical scroller: the new analysis
progress card was inside MemberShell's already scrollable Column. That parent
measures children with unbounded height, which Compose rejects for another
vertical scroller. Removed the progress card's scroll modifier; the parent now
owns scrolling, including the Stop analysis action. Preview/source sheets keep
their separate bounded scrolling surfaces.

The phone was disconnected and its advertised debug endpoint refused connection.
No crash trace was obtained, so this matches the trigger but is not a trace-
confirmed diagnosis. No member data, screen, photo or biometric state was read.
The complete Android test/build gate passed: 319 tests, 0 failures/errors, APK
assembled; `git diff --check` passed. The corrected APK is not installed
and the original trigger needs physical retesting. No new test infrastructure
or dependencies were added. Tests do not establish Compose layout acceptance.

### Crash-fix installation — 2026-10-06

Following the member's explicit Install request, the corrected 319-test APK
installed successfully in place on Samsung SM-S928B, Android 16, with
`adb install -r`. No data clear, uninstall, downgrade, signing change or private
storage deletion occurred. Transient transport identifiers were not retained.
The choose-photo → Create meal draft trigger still requires member retesting;
successful installation does not establish that the reported crash is resolved.

## Sequential voice and meal-detail batch — 2026-10-06

The member requested sequential implementation and one installation at the end.
The approved voice/detail slices were built in that order. Automatic personalized
goals/profile/history remain separate: their equation, applicability and encrypted
schema/archive contract are still undecided. No invented goal method was enabled.

### Voice entry

Speak now opens an extracted dedicated sheet. Recording starts only after Start
recording and permission. Listening/elapsed time/Stop are explicit, using the
existing 12-second/speech-pause recorder. Local audio understanding already
returns spoken_text; the host intercepts that result for editable preview and
does not dispatch its audio-returned action. The member submits checked words
with Create meal draft from a meal section or Continue in Chat from generic
Speak. Existing text tool validation/reviewed saving and durable Chat receipt
remain authoritative. Recognition can summarize or mishear speech; member review
and physical English accuracy checks remain necessary.

Permission denial, empty speech, recognition error, model unavailable, Stop,
Retry, Type instead and Cancel remain explicit. Capture callbacks are invalidated
on cancellation/disposal; temporary audio follows existing local deletion. Lock
clears entry presentation. No network speech service, dependency or new vault
field was added. Existing Chat voice retains its established direct route; the
dedicated preview applies to the new Speak entry.

### Meal detail and portions

Saved meals show four metrics and compact ingredient rows. Tapping one opens a
focused editor in the same sheet. Apply stages edits; Cancel restores that food
and its prior match. Name/preparation changes clear stale matches; correction
requests clear on editor exit. Date/type and manual nutrition editors expand on
demand. A bounded scroll body and keyboard-aware footer keep Save/Leave or
Apply/Cancel reachable. Changed meal drafts confirm leaving; save/coverage-loss
and delete confirmations retain their existing write policies and stable IDs.

Whole-portion adjustments on new meal reviews and catalog-backed saved meals use
an atomic pure calculator: multiplier 0.25–4, nearest integer grams, all input and
result amounts 1–2,000 g. Invalid/missing input rejects the entire change. Food
identity, preparation, source and matches are not converted. New-review scaling
clears possible-hidden inclusion for rechecking. Manual/label macro values are
not automatically scaled. No yield conversion, inferred nutrition, persisted
additional nutrient fields or automatic vault write is introduced.

### Evidence and next work

The voice slice passed its full gate, then the combined final gate passed:
323 tests, 0 failures/errors, APK assembled. Four new tests cover deterministic
portion rounding, atomic invalid/bounds rejection, unsupported factors and
bounded recognized text. `git diff --check` passed. Parent remains `c4cf506`;
all UI work is uncommitted. No Compose instrumentation infrastructure was added.
No phone interaction or accuracy gate is closed by this evidence.

Next: one authorized in-place installation, then physical checks for the prior
photo crash trigger, voice start/auto-stop/edit/create/cancel/permission/error,
ingredient Apply versus Cancel, match clearing, portion rounding/bounds, manual
nutrition preservation, changed-draft Back, save/reopen, keyboard, rotation,
enlarged fonts and TalkBack. Before/after evidence and existing record/model
retention still need confirmation. Nebius token/live-provider tests remain deferred.

### Batch installation

The final 323-test APK installed successfully once with `adb install -r` on
Samsung SM-S928B, Android 16, under the member's batch-install instruction.
No data clear, uninstall, downgrade, signing change or private-storage deletion
occurred. No transient transport identifiers were retained. Member biometric
unlock and the acceptance matrix above remain pending; installation does not
prove record/model retention or UI acceptance. No screenshot/hierarchy capture
or member-record inspection occurred.

## Current nutrition goals and automatic-setup proposal — 2026-10-06

The member requested the next slice. Replaced the legacy MemberShell target form
with an extracted current-goal summary/editor using only existing NutritionTargets
fields. Current optional references show No target when absent; edit leads with
calories/protein, with additional macro fields on demand. The bounded scroll body
and keyboard-aware footer retain Save/Cancel. Changed edits confirm leaving;
removing all existing targets requires explicit confirmation. Lock clears the
editor flag. No profile, weight-target band or phase history is fabricated.

The write callback now runs the existing vault setter off-main and updates shell
status only on success (or actual lock). A failed write retains the unlocked
editor, draft and local error rather than dismissing it into a failed shell.
Duplicate saves are disabled while saving; persistence keeps its existing atomic
rollback. New pure input parsing supports partial/absent values and comma decimals,
rejecting malformed, nonfinite or out-of-storage-bounds values without interpreting
them as a remove request. Existing encrypted schema/archive/assistant/cloud
contracts and manual-only targets remain unchanged.

Prepared `goal-calculation-contract-proposal.md` with verified primary references,
a maintenance-first candidate, explicit approximation/uncertainty, eligibility,
unit/input limits, manual overrides, privacy, profile/phase compatibility and test
requirements. It is a reviewable proposal, not an enabled calculation or approved
M1 revision. The original goal proposal links it. No automatic weight-change,
protein recommendation or first-run profile collection was implemented.

Automated gate and physical status are recorded below. Next decision: adopt or
replace the maintenance method/scope and deliberately revise M1 plus the encrypted
schema/archive contract before implementing automatic setup. Physical checks for
all new screens remain open; the latest installed APK is still the prior batch.

Final full gate passed: 326 tests, 0 failures/errors, APK assembled. Three new
tests cover partial/comma-decimal targets, explicit all-empty removal and invalid
input rejection. `git diff --check` passed. Parent remains `c4cf506`, uncommitted.
No new dependencies, device installation, screenshots or record inspection were
performed for this slice. Save/reopen, failure retention, optional/absent values,
removal, Back/cancel, keyboard, rotation, larger fonts and TalkBack are still
required on the target phone; code/unit/build evidence does not close those gates.

## Approved maintenance estimate and encrypted goal phases — 2026-10-06

The member approved the concrete maintenance-only method and storage scope with
“Go ahead”. M1, architecture, flows, screen specs and plan now reflect this
decision. `maintenance-msj-pal-v1` uses an explicit original equation coefficient
and one whole-day activity scenario, with supported adult inputs and outward
50-kcal rounding. The combined resting-energy/activity method remains explicitly
approximate. No midpoint, deficit/surplus, macro target, target weight, deadline
or ideal weight band is automatically chosen. Eligibility answers remain transient.

The extracted Goals view offers optional direction/profile/activity/review setup
after unlock, manual targets, explicit current-phase review/recalculation, New
phase and Previous phases. Estimated calories require an endpoint choice; manual
typing is labelled manual. Old/new ranges and reference replacement are previewed.
Member-entered weight targets/ranges and additional macros expand on demand.
Existing members are not forced into onboarding; a first-run offer remains later
work. Changed drafts ask before leaving, Skip writes nothing, duplicate saves and
sheet hiding are blocked during writes, and failed writes retain the editor.
New setup and manual-reference fields use memory-only state to avoid serializing
health drafts into Android instance-state bundles. The existing manifest handles
ordinary orientation changes in place; recreation/lock clears the task rather
than restoring sensitive answers from a process bundle. Physical behavior is open.

Goal/profile/reference writes use one validated snapshot with atomic rollback.
Current phase keeps a stable ID/start time on edit. Only explicit New phase adds
the prior phase to history, bounded to 12 with visible eviction disclosure. New
recorded weights do not recalculate targets. Manual calorie changes, including
retyping the same number, remove derived source metadata; macro-only edits retain
the energy source. Individual previous phases can be deleted. Deleting profile
and all phases retains daily references as member-entered and keeps meals/weights.

Encrypted data payload version 2 includes strict goal representation version 1;
encryption/key envelopes remain unchanged. Legacy absence preserves references
and creates no profile/history. Future versions, extra/missing goal fields, invalid
numeric types, non-reproducible sources and mismatched current references reject
before replacement. Recovery preview includes phase/profile presence and explains
older archives removing current goal data. Restore rollback includes goal data.
Old app versions cannot read goal-bearing version 2 data; downgrades are unsupported.
Profile, intent, history and inputs were not added to assistant/cloud serializers
or diagnostics. Nebius credentials/live tests remain deferred.

Independent read-only review identified one source-label defect: edit/new-phase
could copy a derived calorie value while calling it manual. Fixed by clearing
estimate-backed calories when opening that wizard, requiring an explicit endpoint
selection or manual entry; old reference/range remain visible. Existing manual
references and macro values can be retained. A regression test covers this case.
No other material issue was reported; follow-up confirmation and final automated
gate/installation result are recorded below.

Phone acceptance remains open: optional/skip/decline/unsupported inputs; both
equations, unit selection and explicit estimate choice; manual override; partial
targets and removal; edit/recalculate/new-phase/history cap; save/reopen and
failure retention; phase/profile deletion; legacy/new encrypted archive restore
and replacement; keyboard/last field, orientation, landscape, enlarged fonts,
TalkBack and cancellation/lock. Redacted before/after visual evidence and member
record/model retention need member verification. No private screen/data inspection
or physical gate is inferred from tests/build/install.

### Final verification and installation

Follow-up independent review confirmed the source-label fix. The final required
command from `native-android/`,
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`,
passed: **343 tests, 0 failures/errors**, APK assembled. Seventeen new goal tests
cover coefficient/category fixtures, units, bounds, absent/default rejection,
manual intent, range consistency, reproducible source/provenance, same-value
manual override, replacement-wizard source honesty, stable edit/new-phase behavior,
history limits, atomic failed-write rollback, encrypted recovery round-trip,
legacy absence, future/malformed rejection and deletion retaining references.
`git diff --check` passed. No production dependencies, commit or push were added.
Parent remains `c4cf506`; this combined UI tree is uncommitted.

The combined APK installed once in place on Samsung SM-S928B, Android 16, under
the member's sequential-slices/final-install instruction. SDK ADB `install -r`
returned Success; force-stop and `am start -W -n app.metria/.MainActivity` returned
Status ok, cold launch 941 ms. APK SHA-256:
`E008289001AEF9FF731DFD61FB39CB161E1BE323A9824FFB2D1F403BAAED84FF`.
No data clear, uninstall, downgrade, signing change, private-storage deletion,
biometric automation or member-record/screen inspection occurred. In-place
installation preserves the app's storage; actual record/model retention and all
physical acceptance checks above still need member verification after unlock.
Transient device endpoints/identifiers were not retained.

Next bounded task: member verification from Today → Nutrition → optional goal
setup, including explicit choice/confirm, reopen and current/new-phase behavior,
followed by the remaining layout/privacy/archive matrix. First-run onboarding
integration and the broader varied-photo pilot remain separate open work.

## Goal presentation and dismissal repair — 2026-10-06

The member found the installed Goals view, asked for a more attractive hierarchy
using six supplied reference screenshots, and reported that accidentally dragging
the edited setup down → Keep editing left the app frozen. Those references are
inspiration only; no screenshots/assets were copied into the app or repository.
The earlier physical Keep editing gate is failed by this member report, not passed
by the previous 343-test build. No ANR/crash trace or before screenshot was captured;
the code defect matches an invisible modal intercepting touch after its content hid.

Root cause in the editor flow: checking dirty state in onDismissRequest happens
after a sheet may become Hidden. Closing the leave dialog only cleared its flag,
leaving the still-mounted hidden sheet/window. Shared GuardedEditorSheetState now
vetoes Hidden before confirmation for dirty editors and while saving, reading
current flags/callback through rememberUpdatedState. Setup, manual daily targets
and the focused weight editor use this guard. Keep editing (and dismissing the
confirmation) also explicitly restores the expanded sheet. Explicit Leave still
disposes the editor without writing. Same-value calorie edits remain dirty because
they change source provenance. Physical swipe/back/scrim/Keep editing acceptance
must still be retested after installation; pure policy tests do not replace it.

The summary now leads with a theme-toned current-goal card and separate daily
reference card. Latest recorded weight is labelled separately from starting
reference, target and chosen range; ranges exist only when member-entered, with
non-color labels for out-of-range positions. Accessibility describes the range
instead of implying a completion percentage. Calorie source and macro values are
clear; calculation inputs/method are on demand. History has a gentle empty state,
and destructive data controls expand only when requested. Today gains an explicit
Your goal and daily references entry. Setup adds step progress, icon/label choices
and an estimate hero while retaining all optional, validation and source rules.

Edit weight goal is a focused direction/target/range sheet. Its pure input helper
preserves stable phase ID/start, baseline, accepted calorie provenance and daily
references; it validates optional/decimal-comma inputs and rejects invalid ranges.
Starting-reference changes remain in the separate setup/recalculation route.
No persistence schema, energy algorithm, deficit/surplus or exercise/digestion
breakdown was introduced. Nebius credentials/live tests remain deferred.

A bounded worker implemented only the guard/policy/tests, and an independent
read-only reviewer found no material issue in the final scoped change. Automated
and installation evidence follows below. All phone visual/accessibility gates,
keyboard/landscape/large-font checks, save/reopen, and redacted before/after evidence
remain open. Foreground inspection found the phone's launcher, so no Metria or
other-app screenshots/hierarchies/member records were captured during this slice.

### Final gate and installed build

The complete Android unit-test/APK gate passed: **348 tests, 0 failures/errors**.
Five additional tests cover dirty/clean/saving dismissal decisions and focused
weight editing preserving source/identity/references while rejecting invalid
weights/ranges. These are policy/input regressions, not Compose or phone
interaction evidence. A missing Today layout import was corrected before the
final successful gate. `git diff --check` passed. No dependency, schema or cloud
change, commit or push was made; parent remains `c4cf506`, uncommitted UI tree.

The APK installed once in place on Samsung SM-S928B, Android 16, with `install -r`
under the active repeated-install test cycle. Installation returned Success;
force-stop and `am start -W -n app.metria/.MainActivity` returned Status ok,
cold launch 2075 ms. The installed base APK checksum was independently read and
matched the final tested build, SHA-256:
`28486F91C7DA238CBB54DCC844DC1AC999FB5AFC2F7F20C90F5079286BFF9BF2`.
No data clear, uninstall, signing change, private-storage deletion or biometric
automation occurred. Vault/model retention still requires member confirmation.
No transient phone identifiers or raw private evidence were retained.

Next required phone check: unlock → Today → Your goal and daily references →
setup or editor → change an input → drag down → Keep editing → type/continue.
Verify the same values remain and touch works. Also test scrim, system Back,
confirmation dismissal, explicit Leave, clean cancellation and save/reopen;
capture redacted portrait/landscape/keyboard/large-font evidence before closing
the goal UI gate. The member-reported freeze remains pending physical retest;
the code repair and successful installation do not turn it into a passing gate.

### Member follow-up

The member reported “worked good” after this installed slice, in response to
the requested change input → drag down → Keep editing → continue check. Record
the reported interaction as successful on the current 348-test build. This is
member-reported evidence without a captured visual trace; it closes that specific
freeze retest, not the broader swipe/scrim/Back, font, keyboard, landscape,
TalkBack, save/reopen, archive, or record/model-retention matrix. The member
authorized continuing with the next implementation slice.

## Optional first-run goal introduction — 2026-10-06

The member selected this next slice. After successful biometric creation of a new
vault, a transient, creation-only event opens a focused introduction: Set up my
goal or Continue tracking. Skip, swipe, Back and scrim dismissal save nothing.
Existing vault unlocks never replay the introduction; About goal setup in Goals
provides a voluntary preview. A pending voice handoff/meal entry takes priority.
Lock, background and activity recreation clear the event. No preferences, saved
state, profile, target, history, archive, assistant prompt or network payload is
added by the introduction. Existing explicit confirmation remains the only goal
write boundary. The UI uses theme colors, scrolling content and reachable actions.

The full Gradle unit-test/APK gate passed: **351 tests, 0 failures/errors**.
Three additional event-policy tests verify creation-only, consume-once, failed
creation, locked access, clearing and recreation behavior. `git diff --check`
passed. This evidence does not verify Compose interaction or automatic first-run
routing on a phone. Installation and independent review are recorded below.

Physical checks remain open: preview from Goals → About goal setup, skip/dismiss,
setup → cancel/confirm, keyboard, large fonts, landscape and TalkBack. Automatic
creation routing requires a separate empty test vault/device; never clear or
replace the member's existing vault to test this introduction. No commit, push,
production dependency or live Nebius test was performed.

Independent read-only review found no concrete defect in the scoped event,
lifecycle, routing and welcome UI change. It confirmed creation-only triggering,
skip/dismiss behavior and suppression during pending voice/meal entry; it did not
provide physical interaction evidence.

Installed once with `install -r` on Samsung SM-S928B, Android 16, under the active
test-cycle authorization. ADB returned Success; force-stop and cold launch returned
Status ok (1114 ms). Installed base APK SHA-256 matches the tested build:
`5BD990A94983252F44AAD58AAB17588B72C4C1E4B65BFD245ECC7794BB0DFE37`.
No clear, uninstall, key replacement, private-storage deletion or biometric
automation occurred. Existing-vault/model retention and visual acceptance require
member confirmation. No screenshot, hierarchy dump or raw member record was
captured. The member can preview through Today → Your goal and daily references
→ About goal setup; creation-only routing remains a separate open physical gate.

## Today goal card — 2026-10-06

The member requested a goal card inspired by the supplied reference, replacing
Today's latest-weight KPI. Today now uses the shared goal summary full-width:
chosen direction, latest recorded weight (or labelled starting reference), optional
member-entered target/range, Update weight, and Goal and daily references. It uses
Metria theme/components, outlined/text actions and no third-party assets. The
nutrition summary is separate and full-width; the duplicate goal-entry row is
removed. With no goal, weight remains visible with optional setup and weight entry.
Selected kg/lb units apply consistently to latest, baseline, target, range and
accessibility text; range position still uses the original kg quantities.

Update weight opens the existing entry sheet with no stale edit or voice draft.
Goal details opens the existing hub. Existing goal-hub edit routing is preserved
through a named callback after extending the shared card. No estimates, targets,
schema, storage or assistant behavior changed. No production dependency or new
unit tests were added for this presentation change; Compose test infrastructure
is not yet available. The complete Gradle test/APK gate passed: **351 tests,
0 failures/errors**. Physical acceptance remains open: active and absent goal,
missing weight, chosen range/outside range, kg/lb, add weight/save/reopen, goal
navigation, scrolling, portrait/landscape, enlarged text and TalkBack. Redacted
before/after visual evidence remains outstanding. No commit or push was made.

`git diff --check` passed. Installed in place on Samsung SM-S928B, Android 16;
ADB returned Success and cold launch Status ok (1004 ms). Installed base APK
checksum matches the tested build:
`A8714FF0E23B226C19E2D6C4F16DD459C24073875D5714CC09FCD44ECD9A789D`.
No data clear, uninstall, key replacement or biometric automation occurred;
member confirmation of record/model retention and interaction remains required.
No private screenshot or hierarchy was captured for this slice.

Independent review found selected pounds reverted to kilograms in the goal hub.
Threaded the selected display unit into its current/history summaries; existing
setup and focused goal editing remain explicitly kg-labelled inputs under the
approved calculation contract. Review found no other concrete regression. After
that display correction, the complete gate passed again (351 tests, 0 failures/
errors), and the final APK was installed in place, launch Status ok (984 ms).
The final installed/tested APK checksum matches:
`3D63B7D1F069BAE8DB21EBF8FB3B633B1DC1370B7C10A0E33E6BD66B10FF4C48`.
The earlier checksum above records the superseded first installation. Physical
visual/interaction acceptance remains open; no stored values were converted.

## Progress current-goal context — 2026-10-07

The member authorized the next UI slice after the Today goal card. Progress now
has a compact supporting card below its seven-day chart: current direction,
optional member-entered target/range in selected kg/lb, presence of daily nutrition
references, and a quiet route to the existing goal hub. Tracking-only and absent
goal states remain optional; no weight target, range or score is generated. The
card explicitly distinguishes current references from a selected past day. The
nutrition hero comparison now says recorded estimate against the current daily
reference, avoiding an implied historical adherence claim. The former nutrition
hero goal button is consolidated into the shared card action. All-history routing
and chart/date selection are unchanged; goal dismissal leaves Progress composed.

SCREEN-SPECS and FLOWS document the hierarchy and route. UI lives under components
and screens; MemberShell only passes current goal state. No persistence, estimate
algorithm, assistant, cloud, dependency, commit or push changed. Independent
read-only review found no scoped correctness/state regression, including unit
conversion, tracking-only state and retained metric/day on goal-hub dismissal.

The full Android test/APK gate passed: **351 tests, 0 failures/errors** using
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`.
Existing ProgressTimeline and goal input/calculation tests are included; no new
unit test mirrors this presentation-only component. Compose test infrastructure
is not available. `git diff --check` passed. Tested APK SHA-256:
`DCBF3626B85AC61C29BC5755BF70568909B823BA61E98CCF110E69F1EA36A117`.
Parent remains `c4cf506`; changes are uncommitted.

ADB devices enumeration returned no connected phone, so this slice is **not
installed or physically accepted**. The previous reference phone was Samsung
SM-S928B, Android 16; no current-device result is claimed. No private screenshot,
hierarchy, vault access, data clear or biometric automation occurred.

Next bounded task: reconnect using current member-provided endpoints, install
the tested APK in place, then unlock and inspect Progress with current/absent/
tracking-only goals, kg/lb and target/range. Select a different metric and past
day, open Goals, dismiss and verify selection remains. Exercise save/reopen,
All history/Back, scrolling, enlarged text, portrait/landscape and TalkBack;
capture authorized redacted before/after evidence. These physical gates and
existing-data/model retention remain open.

## Remaining companion slices — 2026-10-07

The member requested implementing the Today nutrition, saved-meal Ask Metria and
barcode slices. Today now replaces the single kcal KPI with a recorded-meal
nutrition card: known complete plus partial calories/macros, missing-vs-zero
distinction, explicit estimate wording, expandable coverage and quiet daily
reference access. It stacks macro content for narrow/enlarged-text layouts and
exposes disclosure state to accessibility. Independent read-only review found
no scoped correctness/accessibility defect. Physical visual checks remain open.

Saved-meal chat integration is implemented: clean editors hand the exact saved
ID to local Chat without replacing existing composer text or automatically
submitting. Dirty editors explicitly offer Save and ask, Discard edits and ask,
or Keep editing. A removable one-question context indicator identifies the meal.
Supported food/nutrition questions request argument-free `read_selected_meal`;
the host resolves the ID freshly from the current unlocked vault and validates
identity, source, quantities and coverage before composing a deterministic reply.
This first slice explains saved records, not broad meal advice. Explicit latest/
day/history/progress questions retain their routes. Cancellation, lock/background,
new conversation and deletion clear the transient scope. No record ID enters
model prompts, and no new persistence format or cloud route is introduced.

Independent review found a stale Save-and-ask intent after failed validation or
save. Replaced the ambient flag with an explicit per-attempt argument; only an
active nutrition-coverage confirmation may retain it, and dismissing/keeping
edits clears it. Follow-up review confirmed the repair and found no remaining
scoped defect. The first integration gate found two imports removed from Today
but still used by meal rows/weight formatting; those were restored before the
successful gates. No automated evidence closes physical interaction gates.

Final complete Android gate passed after these fixes: **357 tests, 0 failures/
errors**, APK assembled. Six new selected-meal tests cover exact IDs with duplicate
titles, missing/duplicate IDs, argument rejection, latest-meal precedence, Spanish
calorie wording, mismatched identity, unsupported advice/source, and missing/
partial/zero nutrition. `git diff --check` passed. Final tested APK SHA-256:
`32394E1F24582EF1097B43F0DD95C251D5BC647EE56BF8F3F3785DF3A5221156`.

Barcode investigation found no decoder dependency or GTIN/UPC/EAN product index
in the existing bundled Foundation Foods catalog. `barcode-slice-proposal.md`
defines an honest first local scan/fallback scope; production dependency approval
was requested, then superseded by the member's explicit deferral below. No barcode library, scan view, product match
or runtime lookup has been added. The label/manual routes remain available.
Actual nutrition lookup requires a separately approved licensed offline dataset.

ADB enumeration returned no connected device in this slice. Installation, all
physical gates and record/model retention checks remain open. No commit/push,
private-device evidence capture, dependency or online lookup was performed.

Next bounded task: reconnect the reference phone with current endpoints and
install this tested APK in place. Check Today empty/full/partial/missing/zero
nutrition and daily references, details expansion, enlarged fonts and TalkBack.
From a saved meal, test clean Ask, dirty Keep editing/Discard/Save and ask, invalid
Save and ask followed by ordinary Save, coverage-confirmation dismissal, existing
composer text, explicit Send, selected calorie/foods/summary questions, scope
removal, latest-meal precedence, cancellation, lock/background and new conversation.
Include same-name meals, deletion, save/reopen, keyboard, rotation/landscape and
redacted before/after evidence. The model tool-selection/read route remains
physically unverified; these slices have not been installed. No commit/push.

### Barcode deferral and photo naming — 2026-10-07

The member chose to defer barcode scanning to avoid making someone decode a
product code and then scan the label anyway. The photo screen should offer
Meal photo and Nutrition label. Code inspection confirms these exact choices
already exist in MealPhotoPreviewSheet, with Nutrition label also used by the
photo-source screen and explicit Scan nutrition label actions elsewhere. No
production UI change or new dependency was needed. Updated screen/flow/proposal
status; the outstanding ZXing approval request is superseded, not granted.
The prior tested APK and 357-test gate remain current; no Android code changed,
new build/install occurred or physical acceptance was claimed in this follow-up.

### Companion slices installed — 2026-10-07

At the member's explicit request, installed the tested APK on the connected
Samsung SM-S928B, Android 16, using `adb install -r`; result: Success. Selected
one freshly enumerated wireless connection consistently. Force-stop and cold
launch completed with Status ok in 1322 ms. The installed base APK SHA-256
matches the 357-test build above:
`32394E1F24582EF1097B43F0DD95C251D5BC647EE56BF8F3F3785DF3A5221156`.
No Android code changed since that complete test/build gate. No clear, uninstall,
signing replacement, private-storage deletion or biometric automation occurred.
The in-place update preserves app data; member confirmation of records and
downloaded-model retention after unlock remains pending. No screenshot or UI
hierarchy was captured. Today nutrition, Progress goal context and saved-meal
Ask Metria are now installed; their physical acceptance matrix above remains
open. Working-tree changes remain uncommitted; barcode remains deferred.

## Dedicated Today meal entry — 2026-10-07

The member reported that adding from Lunch went into Chat and that a spoken
meal failed after its words were recognized. The exact failure message was not
provided; speech recognition success does not establish the later failure's
cause. The member approved dedicated entry destinations inspired by supplied
capture/voice references, with concise loading and save into the chosen card.

Today Type and Speak now open an extracted MealEntryScreen. Checked descriptions
call the existing local meal-proposal path directly, bypassing conversational
intent selection. Audio-returned actions are withheld; recognized words remain
editable before Create meal draft. Request identity/operation gates reject stale
results; synchronous attempt/capture tokens guard duplicate submission, delayed
permission and auto-stop callbacks. Stop retains editable words, and errors keep
retry/typing/manual entry available. ON_STOP explicitly cancels capture and active
requests and clears sensitive presentation before a posted callback can resume
work. Ordinary rotation uses the existing manifest configuration handling;
true destruction locks the vault, with no sensitive prompt persisted in Bundle.

The contextual camera path now renders a dedicated TodayMealPhotoEntryScreen
and minimal loading without Assistant home/main tabs. It reuses the system camera,
photo picker and local preview. Camera/gallery actions have icons and labels;
photo tips open a focused dialog. Meal photo and Nutrition label remain the modes.
The selected section and entry date reach meal and label review, remain editable,
and save returns to Today. Text/voice review excludes stale image references and
image audits; no Today-origin chat receipt is created. Generic Chat is unchanged.

Review caught a late OCR publication after Cancel: label success/error/finally
now require the original preparation generation and exact image identity.
Discard/Stop/replacement/background invalidates that scope. A late worker deletes
its original file without publishing output or clearing a newer image state.
Follow-up independent review found no remaining concrete issue in the requested
paths. An initial rotation finding was withdrawn after checking the manifest.

Final complete gate from native-android passed:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`.
**361 tests, 0 failures/errors**, APK assembled; four added tests cover request
correlation, wrong/late completion, transcript validation and attempt cancellation.
`git diff --check` passed. Final tested APK SHA-256:
`677A1F20D97BF02F454591E30E2443718E88625E4BA0853FF84CAF8F6F7AB79A`.

Installed on the freshly enumerated Samsung SM-S928B, Android 16, with
`adb install -r`: Success. Cold launch Status ok, 1566 ms. Installed base APK
checksum matches the tested APK. No clear/uninstall/private-storage deletion,
biometric automation, new dependency, cloud call, screenshot/hierarchy capture,
commit or push occurred. Installation used the authorized in-place test cycle;
member verification of existing data/model retention after unlock remains open.

Next bounded task: unlock and test Lunch Type/Voice/Photo and Nutrition label.
Check explicit transcript review, automatic/manual stop, cancellation/retry,
permission denial, malformed/empty draft and manual fallback. Save and reopen
under Lunch; change type/date in review deliberately and verify the new choice.
Test OCR Stop/Back and immediate replacement, recording/processing background,
rotation, keyboard, enlarged text and TalkBack, with authorized redacted
before/after evidence. The reported voice failure is not claimed resolved until
the member retests it. These physical gates and the varied-photo pilot remain
open; barcode and live Nebius configuration remain deferred.

### Recognized voice meal rejected: shared generation contract — 2026-10-07

The member retested the installed dedicated Lunch flow and reported another
discarded draft. Authorized screen inspection confirmed recognized words stayed
editable and the completed-generation validation message was displayed. Android
CLI flat layout returned an unrecognized instrumentation response; a temporary
annotated screen outside the repository was inspected and deleted. No raw member
description or device screenshot is recorded here. The failed run's raw response
could not be inspected without leaving/clearing that entry, so its exact parser
rejection category remains unknown.

Code inspection found an instruction mismatch: direct text generation still used
the general conversational system prompt and a separate older JSON user prompt,
while native Chat meal drafts used the complete dedicated meal system contract.
Both now share one tool-free conversation factory and send only the validated
member description as user content. Automatic tool calling stays off; the prior
constrained-decoding flag is restored after conversation creation. Request/engine
checks, strict parsing, uncertainty and explicit review/save remain unchanged.
This repairs the identified mismatch; the particular phone failure is not claimed
resolved until another physical retest.

A failed contextual draft can now open Draft details in debug builds only,
showing the matching current request's existing in-memory category and raw output.
It is correlated by request/operation/state and cleared on edit, retry, stop,
background/lock or response lifecycle change. Nothing is logged, exported,
written to the vault or persisted for diagnostics. Member-facing failure wording
offers retry/manual entry without implying that the food itself is unsafe.
Independent review found no concrete issue in the final fix.

Complete Android test/build gate passed: **363 tests, 0 failures/errors**, APK
assembled. Two added tests cover diagnostic request correlation and the structured
generation contract. Tested APK SHA-256:
`9FCB720B9327DAA6CB8EF40BF8AA4D7C8272F9231F9F8F83B9B401C06E486F65`.
The next bounded physical check is the same recognized-meal draft; if rejected,
open Draft details before editing/leaving so the response/category can be inspected.
The broader meal-entry and varied-photo acceptance gates remain open.

Installed this tested APK in place on the freshly enumerated Samsung SM-S928B,
Android 16: `adb install -r` Success; cold launch Status ok in 990 ms. Installed
base APK checksum matches the tested APK above. `git diff --check` passed.
No data clear/uninstall/signing change, new dependency, cloud call, commit or
push occurred. Member biometric unlock and retention/interaction checks remain
pending; installation itself does not close the reported failure gate.

### Saved coffee and hidden editor dismissal - 2026-10-07

The member reported that the recognized-meal draft now worked. This closes that
specific retest of the preceding generation-contract repair; broader voice and
photo acceptance remains open. The next report was clarified as an invisible
editor intercepting Today touches, rather than an app freeze: Back still showed
the dirty-editor confirmation and leaving restored Today.

The saved editor now uses the shared guarded sheet state to veto hiding while
dirty, and Keep editing explicitly shows the sheet. Back closes nested catalog
or ingredient editing first. Catalog search has a keyboard-aware explicit return
action, source/count context, and a clear unavailable-food explanation.

The bundled 363-food USDA Foundation release contains no coffee entry; its local
source release was also checked. No alias can provide absent nutrition. Coffee
remains unknown unless the member supplies values or selects a real catalog match.
Presentation-only drink fallback recognizes reviewed drink roles or exact single
coffee names, including accent-normalized cafe largo. It never uses meal titles,
changes persisted roles, or supplies nutrition. Mixed foods and coffee desserts
remain meal icons. Two unit tests cover these positive and negative cases.

Independent review found no concrete defect in these changes. Physical dismissal,
keyboard, font scaling, accessibility, coffee icon and existing-data retention
checks remain open. Next bounded phone check: open the saved breakfast coffee,
search and return to its ingredient, change a field, swipe/back, choose Keep editing,
then Leave; confirm the editor stays visible or Today becomes responsive as chosen.

Complete Android gate passed: **365 tests, 0 failures/errors**, APK assembled.
`git diff --check` passed. Tested APK SHA-256:
`12B49C90B76DC4E66E42DE4D48C7E240500AE3C9C18B8CA2884152FD03973E8A`.
Installed in place on freshly enumerated Samsung SM-S928B, Android 16 with
`adb install -r`: Success. Cold launch Status ok, 1608 ms; installed base APK
checksum matches. No app data clear, uninstall, signing change, private-storage
deletion, biometric automation, cloud request, dependency, commit or push.
Existing-data retention and the phone interaction checklist above await member
verification after unlock; automated checks do not close those physical gates.

The member subsequently reported "worked" for the installed hidden-editor repair.
This closes the reported dismissal retest only; expanded-font, TalkBack and broader
catalog/meal-entry acceptance still require their specific physical checks.

### Compact Today goal and nutrition summaries - 2026-10-07

The member approved reducing the oversized Today cards and adding visual goal
and nutrition context inspired by the supplied references. Today will stop using
the full goal-detail card. Compact weight context retains quiet goal navigation
and weight entry; graphics use only chosen ranges or distinct start/target values.
Nutrition retains recorded estimates and coverage, compares to existing references
with small bars, and reveals detail from its header. No score, inferred weight
range, new nutrient estimate or persistence change is part of this slice.

Physical acceptance to check after installation: Today portrait and landscape,
enlarged text, TalkBack, chosen range and start/target goals, missing/track-only
goals, complete/partial/unknown nutrition and values above references. Check
header expansion, goal navigation and Update weight, and that meal cards become
easier to reach. Before/after redacted evidence and these gates remain pending.
No Compose instrumentation-test infrastructure is currently configured; no new
dependency is introduced for this presentation slice.

Before implementation installation, the current Today screen was visually
inspected with the Android CLI. Flat layout again returned an unrecognized
instrumentation response; a temporary annotated screenshot confirmed that the
full goal card pushed the nutrition summary below the initial viewport. The raw
screen was kept outside the repository and deleted immediately after inspection.
No record values or raw screenshot are retained as acceptance evidence here.

Implemented only the dedicated Today goal and nutrition components; full goal
detail remains unchanged. Goal markers use fixed confirmed endpoints, padded
marker bounds and textual outside-interval disclosure. Equal endpoints show a
textual chosen weight; targets remain visible alongside ranges. Nutrition headers
expand coverage on tap; normal phone widths use three macro columns and narrow
or enlarged-text layouts stack. Known recorded totals retain actual values and
Above reference text when drawing is capped. Unknown totals have no filled bar.
These are presentation changes, with no model, nutrition, vault or reference write.

Parent review corrected Canvas color capture, endpoint labelling/clipping,
large-text wrapping, equal-range and target visibility. Final independent review
found no material code issue. The full Android gate passed **365 tests, 0
failures/errors**, APK assembled; no new tests were added for this reversible
presentation slice. `git diff --check` passed. Tested APK SHA-256:
`6246393E5DCF888372B83ACF77B24293EA8527F5FFBD6B08134BF8308CD177BB`.
Installed in place on freshly enumerated Samsung SM-S928B, Android 16 with
`adb install -r`: Success. Cold launch Status ok, 1951 ms; installed base APK
checksum matched. No app data clear, uninstall, signing change, private-storage
deletion, biometric automation, dependency, cloud request, commit or push.
Member unlock is requested for the after-layout check; physical acceptance and
existing-record retention remain pending and are not inferred from installation.

### Goal reference inventory and roadmap - 2026-10-07

The member requested a plan from the six-page goal reference PDF. All pages were
rendered and visually reviewed, yielding eleven stable view IDs G01-G11. The
[goal view roadmap](goal-view-roadmap.md) records each view, current implementation,
adaptation, explicit deferred features, dependencies and automated/phone gates.
It is linked from the product plan and original goal proposal. Reference PDF and
page renders are not committed; temporary renders were removed after inspection.

The next bounded work is original Metria mockups and phase-period design (S0),
then the current/previous goal hub (S1). No new Android code, calculation, vault
format, account configuration, install, commit or push is part of this planning
request. Existing compact-card physical acceptance remains pending.

Independent read-only review found no missing G IDs, contract contradiction or
unsupported physical-pass claim in the roadmap. Documentation diff checks passed.
No Android gate was rerun for this documentation-only request.

### Goal journey mockups S0 - 2026-10-07

The member authorized starting the roadmap's next slice. Created an original
[interactive goal prototype](prototypes/goal-journey.html), covering G01-G11,
and [review notes](goal-journey-mockup.md). All data is fictional. The preview
contains the current/previous hub, selected-phase details, staged new setup,
focused editors, failed save/retry, acknowledgement and reference/weight summaries.
Used the Metria semantic palette and spacing, with target-free, missing-reading,
equal-range, unknown-period and enlarged-text states. No reference-app images
or personal example values were copied into the prototype.

Browser inspection checked all eleven views at 320/390/736 px and normal/enlarged
text, with no horizontal product overflow or script errors. New-phase save,
different history selections and retry were exercised. Hub and narrow enlarged
weight renders were visually reviewed. Independent review caught wrong history
selection and starting-reference/provenance mismatch; both were corrected.
Mockup notes distinguish navigation/layout placeholders from real app behavior.

Previous phases use End date unavailable and withhold derived period recaps.
No endedAt field, date inference, schema migration or recap calculation was
introduced. Android source, installed APK and existing record storage are
unchanged. No Android gate/install, dependency, account setup, commit or push.
Compact Today phone acceptance remains pending. Next bounded task is member
hierarchy review, then S1 UI-only hub/selected-phase navigation; S3/S4 exact
period/recap work needs its separate data contract.

### Compact Goals hub and selected-phase detail S1 - 2026-10-07

The member approved the S0 hierarchy with "like it!". Implemented G10/G11 hub
navigation and the unknown-period G02 detail fallback. Goals now uses a compact
current-phase card, compact daily references with source visible while collapsed,
and individually clickable previous-phase rows. Edit weight goal remains the
dominant action. New phase still enters the existing staged wizard; More goal
options discloses recalculation, About and goal data controls. Daily-reference
and method disclosures remain available independently.

Selected-phase details resolve an opaque selected ID from the current unlocked
snapshot. They show that phase's own starting reference, chosen target/range,
daily references and accepted method inputs. Historical phases explicitly show
End date unavailable and no weight/nutrition/streak/duration recap. No adjacency
or updatedAt inference, current-record substitution or closure metadata was added.
A removed selection returns to the hub. Current detail has one dominant Edit
action; historical detail has Back to Goals, with deletion in disclosed management.

The coordinator retains page/selected ID and presentation disclosures, not health
objects or wizard answers. Its page saver restores only hub/detail, normalizing
interrupted welcome/setup/editor destinations to hub. First-run welcome remains
transient. Modal dismissal uses updated state and routes detail back to the visible
hub while vetoing hiding; explicit/system Back and Done remain separate. Existing
dirty-editor guards and write callbacks are retained. Historical/all-goal deletion
still requires confirmation, blocks duplicate actions, catches ordinary failures
for retry while preserving cancellation, and keeps saved data intact on failure.

Files: `ui/screens/GoalSetupScreen.kt` coordinator;
`ui/screens/GoalsHubScreen.kt`, `ui/screens/GoalPhaseDetailScreen.kt`;
`ui/components/GoalsHubCards.kt`, `ui/components/GoalWeightRangeMarker.kt`.
The latter extracts the existing Today range marker unchanged; TodayGoalCard
now calls that shared primitive. Hub visuals draw only saved chosen range or
start/target intervals, with textual outside-interval disclosure. Missing/equal
bounds remain textual. Reference/detail rows stack at narrow widths or enlarged
font scale. No vault, calculator, assistant, cloud or dependency change.

Authorized before-state inspection on Samsung SM-S928B, Android 16 confirmed
the old full goal/reference cards pushed history below the initial viewport.
Android CLI flat layout returned an unrecognized instrumentation response;
a temporary annotated screen was visually inspected outside the repository and
deleted immediately. No actual member values/raw screen were retained in this
checkpoint. The after-state and difficult-state phone checks remain pending.

Parent/independent reviews corrected collapsed provenance, duplicate primary
actions, saving controls, modal-state handling and restoration of incomplete
destinations. Final independent source review found no remaining confirmed
defect. Complete required Android gate passed after final edits:
`:app:testDebugUnitTest :app:assembleDebug --no-daemon`, BUILD SUCCESSFUL,
**365 tests, 0 failures/errors**, APK assembled. `git diff --check` passed.
APK SHA-256: `A7C0A05E2C41D23624507E7068E8C92F0AD67AF4A830C3F2F548C74CAA39954A`.
No new Compose test infrastructure/dependency was introduced; source/unit evidence
does not replace physical interaction tests.

Not installed: the member prefers sequential slices followed by a coherent
installation batch. The phone still runs the previously installed compact Today
build. No clear/uninstall, biometric automation, data replacement, commit or push.
Physical gates remain open: Today/Progress -> Goals -> current and multiple previous
details -> Back/Done, kg/lb, target-free/equal-range/partial references, keyboard
edit/Keep editing, rotation, enlarged text, TalkBack, system insets, deletion Keep
and failure/retry (without deleting member records for testing), and save/reopen.

Next bounded implementation: S2 focused direction/weight/input review using the
approved existing calculation and provenance rules. S3/S4 exact historical period
and recap calculations remain a separate data decision. S1 and compact Today
phone acceptance must be recorded after the eventual in-place installation.

### Focused goal setup and input review S2 - 2026-10-08

Implemented G04/G05/G06 from the approved goal journey. Direction uses calm cards
with an icon, selected mark and text. Optional starting/target/range summary rows
follow before maintenance estimation. Weight references, daily references and used
estimate inputs open focused editors within the same modal. The used-input editor
includes whole-day activity; no exercise, strength or diet fields were added.
First setup, edit, recalculation and new phase have distinct context labels.

The final review validates the proposed phase before entry and compares current
and proposed daily references, including existing manual references without a goal
phase. It states calorie provenance separately from member-entered macros and shows
chosen weight context. Returning to previous steps preserves unchanged choices.
Manual setup can return to the estimate path without leaving the manual flag active.
Final confirmation still uses the existing onConfirm callback, GoalPolicy and atomic
vault behavior; new-phase acknowledgement/saved summaries remain S3/S5 work.

Selected MaintenanceEstimate is explicit transient state. Changing calculation
inputs, units or suitability clears selected derived calories, including when the
changed input is invalid and cannot be recalculated. Independent manual calories
and macros are retained; macro-only edits preserve an accepted calorie selection.
A calorie override becomes manual. Selecting an estimate sets the saved starting
reference to that estimate's reviewed profile weight. Target/range and direction
changes do not recalculate calories or create targets. Prior estimated calories are
not silently prefilled as manual, and suitability starts unconfirmed on each entry.

Files: extracted `ui/screens/GoalSetupSheet.kt`; minimal coordinator routing in
`ui/screens/GoalSetupScreen.kt`; pure parsing, phase validation and provenance
transitions in `GoalSetupDraft.kt`; six new `GoalSetupDraftTest` cases. All health
answers/drafts remain in transient remember state, with stable phase ID across retry.
No SavedState health payload, new calculation method, closure metadata, vault format,
assistant/cloud sharing or dependency was introduced. Existing dirty-dismissal and
Keep editing behavior is retained. FLOWS/SCREEN-SPECS and the roadmap reflect S2.

Parent and independent source review corrected estimate invalidation, activity
editing, Back navigation, manual-path re-entry, source copy and encoding artifacts.
Final independent review found no remaining confirmed material defect. Required
Android gate passed after final edits:
`:app:testDebugUnitTest :app:assembleDebug --no-daemon`, BUILD SUCCESSFUL,
**371 tests, 0 failures/errors**, APK assembled; `git diff --check` passed.
APK SHA-256: `0BC5D9FECF094F72F066AB138E15CC7FE204E1A52155E58B63685802057EDC61`.

Not installed or physically accepted. The phone remains on the earlier compact
Today build; installation is queued for the coherent slice batch requested by the
member. No private device capture in S2, clear/uninstall, commit or push. Target
remains Samsung SM-S928B, Android 16. Pending checks include normal/enlarged text,
portrait/landscape, keyboard-visible editors/footer, kg/lb input selection, missing
references and equal ranges, suitability decline, invalid-input retention, Back,
dirty swipe/Keep editing, save failure/retry, rotation and lock/background clearing,
TalkBack, insets and save/reopen with existing-data retention. Unit/source evidence
does not close those physical gates; no Compose test infrastructure was added.

Next bounded slice: S3 staged new-phase replacement, cancellation, retry and success
behavior using the existing atomic contract. Exact historical boundaries and
period-derived recaps still require the separate S0 data decision; S4 can retain
the explicit unknown-period fallback. Install and record S1/S2 acceptance with the
eventual coherent batch.

### Staged new-phase confirmation and save/retry S3 - 2026-10-08

Final confirmation now uses a focused Current/Proposed comparison with direction,
optional starting/target/range values, daily references and calorie source. Unset
values remain explicit. The current phase remains active until save succeeds.
The archive effect is stated; when the captured previous-history count reaches
12, confirmation explicitly warns that the oldest previous phase will be removed.
Editing preserves the existing phase ID/start date. First setup handles existing
manual daily references without inventing a prior phase.

The final-review transition freezes a validated GoalPhase in GoalSaveAttempt;
current phase, targets and history count are also captured transiently, so callback
snapshot refresh cannot rewrite the comparison. The save gate begins synchronously
before launch and rejects duplicate taps. Failure retains the exact proposal, ID
and timestamps for Retry save. Back returns to review and permits a newly reviewed
proposal. Before confirmation, no navigation/cancel action invokes a vault write.
Saving blocks dismissal and controls. Ordinary exceptions show retry; coroutine
cancellation is rethrown. Success makes that attempt terminal and opens a static
Saved acknowledgement with one Continue to Goals action. The acknowledgement has
scrollable content, a pinned 48dp action and a labelled accessibility announcement;
it contains no save action. Swipe/Back after success simply finishes the task.

All proposal/attempt/success state remains transient remember state and drops with
the existing lock/recreation policy. No health payload is added to SavedState,
diagnostics, assistant or cloud serializers. No closure timestamp, new method,
dependency, encrypted format or vault rule changed. The existing confirmed callback
still uses GoalPolicy and synchronized vault atomic replacement with rollback.

Files: `GoalSaveAttempt.kt`, `GoalSaveAttemptTest.kt`,
`ui/screens/GoalConfirmationContent.kt`; focused changes in GoalSetupSheet and
coordinator history-count wiring in GoalSetupScreen. Three regression tests cover
duplicate/completed attempts, exact-proposal retry, and failed replacement then
successful retry at the history limit, preserving the old snapshot on failure and
archiving it exactly once on success. Independent source review found no confirmed
material defect. Full Android gate passed: `:app:testDebugUnitTest :app:assembleDebug
--no-daemon`, BUILD SUCCESSFUL, **374 tests, 0 failures/errors**, APK assembled.
APK SHA-256: `D7206BDEF8E6F18DF586DB853E06CBE5D5D497C40A00C138DF181F2D2F60B1F7`.
`git diff --check` passed. FLOWS, SCREEN-SPECS and roadmap are aligned.

Not installed or physically accepted; the phone still runs the earlier compact
Today build. Samsung SM-S928B/Android 16 remains the target. Pending combined-batch
checks: current/new comparison, Cancel and Keep editing, valid/invalid/manual/estimate
paths, rapid taps, safe write failure/retry, saving dismissal, one-time acknowledgement,
save/reopen, history-limit disclosure, lock/background/recreation, portrait/landscape,
enlarged font, keyboard/insets and TalkBack. No raw member capture, clear/uninstall,
commit or push. Automated evidence does not close those physical gates.

Next bounded task: S5 saved daily-reference and chosen-weight summaries. S4 continues
to use the explicit unavailable-period fallback already implemented in S1. Exact
historical closure and recorded-period highlights still need the separate S0 data
contract before implementation. Installation stays queued for the coherent batch.

### Saved daily-reference and weight-context summaries S5 - 2026-10-08

Saved acknowledgement now offers optional read-only daily-reference and chosen-weight
summary pages before returning to Goals. Explicit Next/Back/Continue controls work
without gestures; Return to Goals and external dismissal can finish early. Each page
starts at the top, scrolls and retains a pinned primary action. Success uses a static
check/text state and live-region heading, without timers or motion. Summary state and
method disclosure remain transient; the exact successful GoalSaveAttempt.phase is
the sole source, and there is no save callback or mutation from any summary action.

Daily references show saved calories and source prominently. Only an accepted saved
maintenance estimate has a static marker within its own approximate activity-scenario
range, with text explaining uncertainty. Manual calories have no fabricated range.
Protein/carbohydrate/fat references display saved values or Unset, without generated
targets, composition percentages or intake judgments. Method/inputs disclose on demand.

Weight context uses the selected kg/lb display choice and labels the saved starting
reference separately from the optional chosen target/range. The static marker shows
only those saved points; no latest reading is invented or substituted. Absent/equal
bounds use text, and an outside-range starting reference is explicitly disclosed.
No predicted curve, deadline, progress percentage, trophy or achievement claim.

Files: `ui/screens/GoalSavedSummaryScreen.kt`,
`ui/components/GoalSavedSummaryCards.kt`, coordinator/setup wiring, and extraction of
the existing GoalWeightRangeMarker drawing into a generic static interval primitive.
Existing weight marker calls preserve their appearance and descriptions. The old
standalone saved content and unused comparison formatter were removed. No business
calculation, persistence, archive format, assistant/cloud sharing, dependency or test
infrastructure changes; the full existing regression suite was run.

Independent source review found no confirmed material defect. Full required Android
gate passed: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`, BUILD SUCCESSFUL,
**374 tests, 0 failures/errors**, APK assembled. `git diff --check` passed.
APK SHA-256: `DB05A1C8E150B897518611E51B0F86315C4F8895B665EE57FD59FDC6A76AB89A`.
FLOWS/SCREEN-SPECS and roadmap include the new read-only journey.

Combined S1-S3/S5 installation preflight: `adb devices -l` returned no connected
device. Current USB/wireless connection requested; not installed. The earlier compact
Today build remains on the phone. Samsung SM-S928B/Android 16 remains the target.
No screenshots, biometric interaction, clear/uninstall, commit or push occurred.
Physical checks remain open: before/after and difficult-state layout, saved/manual/
estimated/unset references, kg/lb, target-free/equal/outside-range context, method
disclosure, Next/Back/early finish, repeated-save prevention, save/reopen, lock/
background/recreation, portrait/landscape, enlarged fonts, keyboard/system insets and
TalkBack. Source/unit/build evidence does not mark these accepted.

Installation update, 2026-10-08: combined S1-S3/S5 APK installed on the connected
Samsung SM-S928B, Android 16, using `adb install -r`; result Success. Force-stop
and `am start -W -n app.metria/.MainActivity` returned Status ok, COLD launch,
TotalTime 991 ms. Installed base.apk SHA-256 matches the verified APK above.
Existing app data was retained by the in-place installation; no clear/uninstall,
biometric interaction or member-record inspection occurred. Physical acceptance
remains pending member unlock and journey checks; installation is not UI acceptance.

Next bounded task: record the phone journey checks for this coherent build.
S4 retains S1's explicit unknown-period history fallback;
historical closure metadata and recorded-period highlights remain a separate data
decision, not a hidden implementation promise or inferred date calculation.

### Phase calorie and macro research - 2026-10-08

Member requested research into generating editable phase calorie/protein/carbohydrate/
fat references from collected inputs. Added `phase-nutrition-research.md` with EFSA,
ISSN, resistance-training research, FAO conversion factors, NHLBI/NIDDK energy
context and a surplus trial. Separates published evidence from proposed Metria
defaults; maps actual GoalProfile/GoalPhase inputs and missing training context.
Includes deterministic candidate arithmetic, a fictional example, manual fallback,
energy-adjustment/applicability gaps, source/override/archive requirements and tests.
Roadmap links the research without marking calculation implementation approved.
No Android, persisted record, cloud, dependency or installation change. Existing
maintenance-only safety/data contract remains active. `git diff --check` passed.
Next bounded task: resolve a phase-nutrition calculation contract before enabling
automatic phase calories/macros; no phone testing is needed for this research note.

### Phase nutrition contract proposal - 2026-10-08

After member clarification that Fitia's referenced setup contains training questions,
checked the actual Metria GoalProfile and form: only whole-day activity exists.
Member authorized continuing the next slice. Added
`phase-nutrition-contract-proposal.md`: concrete optional training choice, proposed
balanced/exercise formulas, bounded offer/fallback, no double-counting activity,
review/invalidation/override semantics, independent provenance and versioned codec
proposal, sequential calculator/data/UI/build/phone gates. Frequency and diet type
are intentionally omitted until a method uses them. Gain/Lose energy adjustments
remain separate; macros can use a reviewed manually chosen calorie budget.
Exact new nutrition policy is proposed for review rather than silently enabled
under the existing maintenance-only M1 approval. No Android or stored data change;
installed build remains the 374-test combined goal APK. Next bounded task: approve
the precise first-slice policy, then implement calculator/source model and tests.

### Approved phase macro suggestions - implementation started 2026-10-08

Member explicitly approved the exact first-slice contract with “go ahead!”. M1,
architecture, product plan and flow/screen contracts now record that deliberate
extension. Bounded work: deterministic optional training-based proposals; independent
encrypted per-field source; legacy-readable strict goal representation; focused
input/proposal review and honest saved/current/history inspection. Calorie adjustment,
frequency/diet type, forecasts, assistant/cloud sharing and automatic recalculation
remain outside this slice. Existing uncommitted work is preserved. Full automated
gate, independent source review and phone acceptance remain pending implementation.

### Approved phase macro suggestions - build verified 2026-10-08

Implemented the optional focused training/input review, current/proposed macro
preview, explicit Use suggestions and existing final phase confirmation. Balanced
and exercise formulas are deterministic local code. Saved/current/history views
disclose each field's source and the saved method inputs. Editing calories or
calculation inputs removes dependent derived references for re-review; explicit
manual overrides are preserved, including same-value edits.

Independent backend and UI source reviews found no confirmed material defects.
The complete `:app:testDebugUnitTest :app:assembleDebug --no-daemon` gate passed:
389 tests, zero failures/errors, APK assembled. `git diff --check` passed. Tests
cover formula bounds/fallback, parsing, rounding, canonical source validation,
legacy/versioned/malformed codecs, encrypted round-trip, override/invalidation,
archive profile presence and atomic rollback. No production dependency was added.

APK SHA-256: `BAEF78DFB27BBC00601B8201BA487F631F612CC66C9D22FA27985F2F2C155F4C`.
ADB showed no connected device, so this macro build is not yet installed. The
previous 374-test goal build remains the last verified phone installation;
member storage was not touched during this gate. No device capture was made.

Next bounded task: connect the target Samsung SM-S928B / Android 16, install
in place, and exercise Goals → phase setup → Training and macro suggestions.
Verify general/strength/exercise/manual fallback, manual and estimated calories,
Use/Skip, dependency edits and overrides, final save/reopen/current/history,
Keep editing, keyboard/font/insets/TalkBack, rotation and lock/retry. Capture
authorized temporary before/after evidence and remove raw member captures.
Physical acceptance, automatic weight-change calorie adjustment and S4 exact
history-period metadata remain open. Working-tree changes remain uncommitted.

### Phase calorie adjustment - concrete next-slice proposal 2026-10-08

Member authorized progressing after the macro slice. Rechecked primary NICE,
NHLBI, NIDDK and strength-training research and drafted
`phase-energy-contract-proposal.md`. It defines visible maintenance/adjustment/final
calories, proposed modest Lose/Gain defaults, explicit bounded offer/manual
fallback, a separate encrypted source/strict v3 codec, macro invalidation,
direction-edit behavior, focused states and sequential verification. The exact
default percentages and applicability boundaries are product choices requiring
review, not claimed universal nutrition guidelines. No Android or saved-data
change, dependency, commit, push or installation occurred. Next: settle that
concrete policy, then implement calculator/provenance before the UI.

### Phase calorie policy approved and rationale recorded - 2026-10-08

Member approved the concrete next-slice proposal, explicitly preferring a 10%
loss adjustment over the 25% they reported in Fitia, and requested references.
The phase energy contract now distinguishes member-reported competitor behavior,
the approved Metria product default, supporting primary evidence and limitations.
No source is described as proving 10% universally optimal or adequate. The
fictional 2,400 kcal example compares a 240 kcal versus 600 kcal subtraction
without a weight-change forecast. The approved 5% strength-training gain default
and bounded offer/manual fallback remain as specified. Code and installed app
are unchanged; policy implementation and physical acceptance remain pending.
Next bounded task: implement the pure phase-energy calculator and independent
provenance/strict codec, then the focused review and regression/phone gates.

### Approved phase calorie implementation started - 2026-10-08

Member additionally confirmed the 5% Gain default and instructed implementation.
M1, architecture and flow/screen contracts were deliberately revised before
enabling the extension. Work is bounded to the pure calculator, independent
encrypted phase-energy source/strict legacy-readable goal v3, explicit setup
preview/Use/final confirmation, source-aware summaries and dependency/direction
review. Unsupported contexts retain manual references; forecasts and extra
exercise calories remain excluded. Calculator/data and UI ownership are separate;
independent review and the complete gate will run after integration. Existing
uncommitted changes are preserved. Phone acceptance remains pending.

### Phase calorie proposals - final automated gate 2026-10-09

Implemented approved optional Lose −10% / strength-Gain +5% calculations with
canonical maintenance endpoint, reviewed training and bounded offer/manual fallback.
Current and historical phases retain independent encrypted source metadata; strict
goal v3 reads v1/v2 without fabricated sources. Confirmation/saved/current/history
and manual-reference views disclose phase-adjusted versus maintenance/manual calories.
No forecasts, extra exercise energy, network or new production dependency.

Focused setup calculates and stages only; final frozen phase confirmation writes.
Used-input changes remove accepted phase energy and affected derived macros while
preserving manual overrides. Typed calories, including same-value edits, become
manual. Back from phase calories restores the entry draft, source, macros and dirty
state. Weight-editor direction changes route to full reference review. New phases
require fresh acceptance; original phase methods remain inspectable as review context.

Independent backend and integration review found no remaining confirmed material
defects. Parent fixed first-use macro-budget invalidation, endpoint/training stale
sources, incomplete Back restoration, stale-direction initialization, and a setup
activity transition that cleared the maintenance suitability confirmation. The last
targeted review verified profile changes still require fresh suitability while
activity selection preserves the reviewed profile confirmation and invalidates
derived references. Physical interaction has not yet established those outcomes.

Final command from native-android:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`.
Result: BUILD SUCCESSFUL, 405 tests, zero failures/errors, APK assembled.
`git diff --check` passed. Added fixtures cover exact BMI boundaries, unsupported
activity/training, floor/deficit limits, canonical/tampered source, per-field manual
invalidation, first-use/same-budget macro transitions, legacy/current/history v3
payloads, actual encrypted archive create/open and atomic rollback.

APK SHA-256: `CE259B664C9607B9BEBFD9F2C6C0E69782ED8F77402CC146ED966789E1CCE527`.
Fresh ADB inventory showed no connected device. This build is not installed;
Samsung SM-S928B / Android 16 is the intended target, with the previous 374-test
goal build the last verified installation. Existing phone data was not touched.
No screenshot or hierarchy capture was made. Working tree remains uncommitted.

Next bounded task: connect the current wireless-debugging endpoint, install -r,
launch and verify installed hash, then member unlock. Exercise Goals → New phase
→ Lose/Gain → reviewed maintenance → Phase calories → Calculate/Use → macro
review → final confirmation → saved/current/history. Test endpoint/training change
then Back, manual/same-value calorie edits, partial manual macro overrides,
unsupported fallback, direction review, Keep editing, save/reopen/lock/rotation,
keyboard/scaled fonts/TalkBack and system insets. Capture authorized temporary
before/after/difficult-state evidence outside the repo, remove raw captures and
record physical results separately. S4 exact history-period metadata remains open.

### Phase calorie proposals - installed in place 2026-10-09

Member requested Install. Fresh ADB inventory identified the target Samsung
SM-S928B; Android release verified as 16. Used exact-device `install -r` with the
final 405-test APK: Success. Force-stop and `am start -W` cold launch returned
Status ok, TotalTime 986 ms. Installed base APK SHA-256 exactly matches
`CE259B664C9607B9BEBFD9F2C6C0E69782ED8F77402CC146ED966789E1CCE527`.
This replaces the previous build in place, with no uninstall, data clear,
downgrade, signing change or private-storage operation. Existing-data retention
uses Android's in-place update path; reopening member records still requires
member unlock and physical confirmation. No biometric interaction, screenshot
or hierarchy capture occurred. Physical acceptance remains open for the named
phase/macro/manual/cancellation/accessibility journeys above.
### Separate proposal suitability and pregnancy applicability - 2026-10-09

Updated the goal setup to ask general clinical/eating-disorder suitability
separately from pregnancy/breastfeeding applicability across maintenance, phase
energy, macro and manual reference entry. Applicability is never inferred from
equation coefficients. An applicable answer routes to care-team-entered references,
clears calculated values and hidden proposal snapshots while preserving explicit
member values, and cannot be reversed with Back. A fresh not-applicable answer can
return the member to general options without reviving old proposals. Answers remain
transient. Added pure policy tests for both independent checks and source-aware
clearing. Automated Android gate and physical accessibility/keyboard/rotation
acceptance remain pending; no phone is connected, and no installation was done.

Final automated verification: `rtk proxy .\gradlew.bat :app:testDebugUnitTest
:app:assembleDebug --no-daemon` from native-android passed on 2026-10-09:
408 tests, zero failures/errors, APK assembled. Independent review found an
unnecessary applicability-answer requirement on manual paths; parent removed it
from manual saving and the macro editor's manual/demanding fallback. Targeted
re-review found no remaining confirmed routing defect. Both answers gate only
automatic proposals; manual references can be saved without health disclosure.
`git diff --check` passed. No persistence schema, network or dependency changes.
APK SHA-256: `5D42F292E4D17628B75290AE314950FD82EF07DA0D0ABED7F8321B05ABF7A99A`.
Fresh final ADB inventory still showed no device; this APK is not installed.
The previous 405-test installation remains the last verified phone build.

Next bounded physical check after in-place installation and member unlock:
maintenance/macro-only routes require both answers for proposals; applicability
Yes opens manual care-team references; manually entered overrides remain while
derived references disappear; Back cannot revive them. Correct Yes to No and use
Review general options again for fresh confirmation. Manual/Just track saving
and manual/demanding macro fallback must work with applicability unanswered.
Verify Keep editing, save/reopen, lock/rotation, keyboard, scaled font and TalkBack;
capture authorized temporary before/after evidence outside the repository.
Working tree remains uncommitted and physical acceptance remains open.

### Separate suitability checks - installed in place 2026-10-09

Member requested Install and then confirmed connection. Fresh ADB inventory
identified Samsung SM-S928B / Android 16. Exact-device `install -r` returned
Success for the 408-test APK; force-stop and `am start -W` returned Status ok,
cold launch 2610 ms. Installed base APK SHA-256 matches
`5D42F292E4D17628B75290AE314950FD82EF07DA0D0ABED7F8321B05ABF7A99A`.
In-place installation used no uninstall, data clear, downgrade, signing change
or private-storage operation. Member unlock and reopening existing records
remain physical checks. No biometric interaction, screenshot or hierarchy capture
occurred. The suitability/manual/cancellation/accessibility checks above remain
pending; installation alone does not establish physical acceptance.
### Goal final review clarity - 2026-10-10

The goal setup final review now names all four daily references, shows each
missing value as Not set, and describes Set/Partly set/Not set by supplied-value
count only. Optional weight target/range omissions are explicit and unit-free.
Review text distinguishes a selected maintenance endpoint from an applied phase
proposal and gives the optional next actions. The macro source summary now indexes
protein/carbohydrate/fat independently of the calorie slot and omits sources for
absent fields. The member reported the suitability step “worked”; this is a
member-reported flow check, not a broad physical acceptance pass. Focused unit
tests cover the presentation mapping. Parent owns the complete Android gate;
phone is disconnected and no installation was performed.

Final parent verification: complete Android gate passed, 412 tests, zero
failures/errors, APK assembled. Independent review found no confirmed defect.
Parent final refinements put each daily value on its own line, distinguish
Continue with unset values (review) from Start phase/Save with unset values
(actual save), and retain Retry save precedence. No calculator, persistence,
suitability requirement or automatic saving changed. `git diff --check` passed.
APK SHA-256: `DEDFCA5212F58C5D24607D5422219E573FD17550306E84633157ADEF9E7C6B53`.

The phone reconnected during verification. Reused active-cycle in-place install
authorization: exact-device install -r succeeded on Samsung SM-S928B / Android 16;
launch Status ok, cold TotalTime 988 ms. Installed base APK hash matches the
verified build. No uninstall, data clear, downgrade, signing change or private
storage operation. No biometric automation or screenshot/hierarchy capture.
Member unlock and existing-record reopening remain physical checks. The supplied
screenshots are before-state reference only and were not copied into tracked docs.
Awaiting member opening Step 5 of 6 with partial references for authorized
after-state visual inspection. Keyboard, scaled font, TalkBack, rotation,
cancellation and saving/reopening remain pending. Working tree stays uncommitted.

Member feedback, 2026-10-10: the updated review “worked”, followed by “they match”
in response to the save/reopen comparison check. Record this as member-reported
matching saved/reopened values for the tested phase; the exact intent, manual
source label and both Lose/Gain coverage were not separately confirmed. No raw
member values were recorded. This does not close the remaining layout, keyboard,
TalkBack, rotation, cancellation or archive-recovery acceptance gates.

### Recorded phase periods and recap - 2026-10-10

Member approved the next phase-history/recap slice with “ok lets go”. The bounded
contract is [phase-recap-contract.md](phase-recap-contract.md), covering G02 and
the S0/S3 period foundation. Explicit replacement alone stamps the old phase's
end equal to the new frozen review/start instant, atomically with references and
history. Failed persistence restores the complete prior snapshot. Edits and
cancellation never end a phase. Goal representation 4 retains nullable ends;
legacy representations 1–3 retain unknown ends without inference. Known periods,
current end absence and supported timestamp years 1–9999 are validated before
replacement. Payload/encryption envelope versions are unchanged.

Phase details now show a read-only current preview or known historical recap:
actual first/last weight readings, signed change and chart; meals logged and
unique days with retained records; per-metric recorded nutrition sums and coverage.
Missing values stay unavailable; partial nutrition and uncertain boundary-day
times are excluded visibly. Local calendar filtering handles DST and skips
out-of-period extreme timestamps before timezone conversion. Unknown historical
ends and active clock reversal show Period unavailable. No forecast, average
intake, adherence score, streak, top-food ranking, cloud request or new dependency.
Record lists are explicit screen inputs; MemberShell changes are plumbing only.

Independent review found and resolved duplicate/contradictory date copy, missing
nutrition coverage and timestamp robustness issues; final review found no remaining
confirmed material defect. Parent added deletion/neighbor-boundary recomputation
coverage. Complete Android gate passed: **425 tests, 0 failures/errors**, APK
assembled. `git diff --check` passed. APK SHA-256:
`8148BE84CBAE123D70D7BB08EB58F4AE866B93C45F29C5D3D59373F2C101C3CD`.

Implementation evidence does not close physical acceptance. Before/after phone
inspection was offered; member readiness has not been confirmed. No screenshot,
hierarchy or biometric inspection occurred. Pending: existing-record retention,
current/old/known-history display, new-phase cancel/save/reopen, deletion,
portrait/landscape/enlarged text/TalkBack, lock/restoration and encrypted archive
restore on phone. Keep unrelated working-tree changes; no commit/push performed.

Installation: fresh ADB inventory selected Samsung SM-S928B / Android 16.
Active-cycle in-place installation authorization reused; `install -r` returned
Success. Force-stop and `am start -W` returned Status ok, cold launch 931 ms.
Installed base APK hash matches the verified 425-test build above. No uninstall,
data clear, downgrade, signing/private-storage changes or biometric automation.
Existing-record retention still needs member unlock/reopening confirmation;
installation alone does not close the physical gates.

**Compact phase-detail refinement - 2026-10-10:** condensed the intent/date
header, weight context, and daily values; removed the duplicate action row in favor
of one bottom action (Edit weight goal for current phase, Back to Goals for past
phases). Recap now uses one concise unavailable/empty/no-nutrition state, keeps
actual first/last readings and dates visible, and puts positive partial/missing
coverage and boundary-time counts behind an accessible details disclosure.
Collapsed source summary is concise; per-field calorie and macro provenance stays
available under Method and inputs. No Compose UI test infrastructure was present.
The complete Android gate passed (`:app:testDebugUnitTest` and `:app:assembleDebug`);
`git diff --check` passed. Phone visual acceptance is still pending, and no
installation or private-data inspection was performed.

Parent final gate: 425 tests, zero failures/errors, APK assembled;
`git diff --check` passed. Independent final review found no confirmed defect.
Review corrections preserve boundary exclusions in otherwise-empty recaps,
count partial meals once, and leave coverage context available even with full
coverage. Parent removed unused weight-row UI and made mixed sources explicitly
include estimates; entirely unset references do not repeat a source badge.
No calculation, goal data, codec, saved value or dependency changes.
APK SHA-256: `858CD792C3388EBC393A2197CDD914C8D82F23AA1174A34420B74E17D5FCDB96`.
The member supplied Metria/Fitia before screenshots; these remain untracked raw
attachments and were not copied into evidence. After-layout readiness was asked
but has not been confirmed. Font scaling, TalkBack, landscape and navigation
acceptance remain pending; tests are not a substitute for those checks.

Compact detail installed in place on Samsung SM-S928B / Android 16 using fresh
exact-device inventory and existing active-cycle authorization. `install -r`
returned Success; launch Status ok, cold 1115 ms. Installed APK hash matches the
verified build above. No uninstall, data clear, signing/storage manipulation,
biometric automation or screenshot/hierarchy capture. Member unlock and visual
comparison remain pending. Working tree remains uncommitted.

**Physical visual inspection - 2026-10-10:** member confirmed
Goals > Phase details was open and authorized inspection. Samsung SM-S928B,
Android 16, compact-detail APK `858CD792C3388EBC393A2197CDD914C8D82F23AA1174A34420B74E17D5FCDB96`.
Default-font portrait, three-button navigation: current phase with populated
daily references, unset weight target and empty recorded recap was readable.
Header, calorie/macros, source summary, empty message and fixed edit action fit
without overlap. Method disclosure opened, scrolled to its final row and recap,
and kept the primary action above the navigation bar. Layout semantics expose
Back to Goals and the method disclosure; inspected actions measure at least
48 dp. This does not establish TalkBack traversal acceptance.

Changing system font scale from 1.0 to 1.5 recreated the activity and requested
member biometric unlock. No authentication action was automated; larger-text
inspection initially waited for member unlock/reopening. Raw device captures are
temporary outside the repository; no member values or raw evidence are copied
here. This bounded check does not close the full physical quality gate.

After the member unlocked and reopened Phase details, font scale 1.5 portrait
passed: macros switch to stacked lines, labels fit and the primary action remains
reachable. Landscape at font scale 1.5 passed for the same state, including
scrolling the complete final recap row into view. Rotation to landscape kept the
active Phase details screen and vault unlocked. Return-to-portrait inspection
could not obtain an idle hierarchy before settings restoration, so round-trip
restoration is not marked passed. Original settings were restored and read back:
font scale 1.0, automatic rotation enabled, user rotation 0. Font changes recreate
the activity and require member unlock/reopening; task restoration across that
recreation remains a separate acceptance gap. The pale background also leaves
system status-bar icons with low contrast, a pre-existing shell issue still open.

Temporary raw captures were removed after local inspection. No code or member
records changed, no install was needed, and `git diff --check` passed. Manual
TalkBack, gesture navigation, navigation/edit/save, historical/populated/partial
recap states and archive recovery remain pending. Keyboard is not applicable to
this read-only screen; editor keyboard acceptance is still separate.

**Member target save/reopen and cross-view consistency - 2026-10-10:** member
reported Saved and reopened correctly, then clarified that a target weight was
added. On Samsung SM-S928B / Android 16, installed APK
`A931928AF1CF01BA786D587D63896329C0771C6AD115B9BA53B27236F0E29E50`,
fresh authorized layouts confirmed the chosen target in Goals, Phase details and
Today. Goals and Today expose the start-to-target marker with textual endpoints;
Phase details distinguishes the starting reference and chosen target. Current
phase start/status and daily calorie/macro references match their pre-edit
presentation. Back to Goals and Done returned to Today without a stranded editor.
The agent performed navigation only; the member chose and saved the target.
No raw member values or screenshots are recorded here. This closes the tested
member-reported target save/reopen and observed cross-view consistency check,
not new-phase replacement, known-end/populated historical recaps, archive restore
or all goal-editor states. No code change or new installation was required.

**Progress consistency continuation - 2026-10-10:** retained the requested future
estimated-time-to-target idea, references and validation requirements in
goal-view-roadmap.md; no forecast implementation authorized or added. On the same
Samsung SM-S928B / Android 16 and A931928A build, authorized read-only layouts
confirmed Progress shows the same chosen target and units as Goals/Today, with
current-goal context distinguished from the selected historical day. Opening
Goals from Progress and choosing Done returned to Progress with its selected
day, metric and scroll position intact. No member records, settings or code
changed; no new installation or screenshot was required. Full TalkBack traversal,
dirty-editor Keep editing, known-end/populated recap and archive restoration
remain open. `git diff --check` passed.

**Unsaved weight editor / Keep editing - 2026-10-10:** authorized physical
check on Samsung SM-S928B / Android 16, existing A931928A installed build.
Opened Goals from Progress, opened the current weight editor and entered a
temporary unsaved change. Dismissed the keyboard, then used Android Back.
Leave without saving confirmation appeared. Keep editing restored the visible
editor with the unsaved field intact. Back remained responsive and opened the
confirmation again. Leave without saving discarded the draft, returned to Goals
and showed the original saved target and daily references unchanged. No Save
action, new phase, record deletion or biometric interaction occurred.

The layout service twice returned Unrecognized response from instrumentation
server. Authorized local annotated captures and screenshot-resolved coordinates
were used instead; all captures were temporary outside the repository and removed
after inspection. No raw member values are recorded here. The Android Back
dismissal/Keep editing/discard path passed; swipe dismissal, rotation while dirty,
TalkBack and failure/retry remain separate pending checks. No code change or
installation was required; `git diff --check` passed.

**Historical navigation and focused editor check - 2026-10-10:** on the same
Samsung SM-S928B / Android 16 and installed compact-detail build, Today goal
entry opened Goals. A legacy previous phase opened with mixed reference sources,
unset macros explicitly labelled and one Period unavailable recap. No period
totals were invented. Header/cards/action fit in portrait. Back to Goals returned
to the hub. Current weight editor opened; focusing the target showed a numeric
keyboard while the focused control, save and cancel remained reachable. Cancel
with no entered changes dismissed both editor and keyboard and returned to Goals.
No save, deletion, new phase or member record change was performed. This verifies
the untouched editor cancellation path only; dirty dismissal, save/reopen and
populated/known-end/partial recaps remain pending. Local temporary captures were
removed; no raw member data is retained as repository evidence.

Inspection found a hub copy defect: the current phase used the legacy historical
Period unavailable label because its end is absent by design. GoalHubCurrentCard
now says Since <start date> and In progress, consistent with Phase details.
Historical unknown-end rows retain Period unavailable. One display string changed;
no calculation, persistence, dependency or saved reference changed. Complete gate:
425 tests, 0 failures/errors, APK assembled. APK SHA-256:
`A931928AF1CF01BA786D587D63896329C0771C6AD115B9BA53B27236F0E29E50`.
Active-cycle in-place install -r succeeded; cold launch Status ok, 965 ms. No
uninstall, data clear, signing change or authentication automation. Corrected-label
physical verification awaits member unlock/Goals reopening. Working tree remains
uncommitted; no push performed.

Member reopened Goals after the update. Fresh hierarchy and local visual capture
confirmed the current card says In progress with its start date; previous legacy
rows still say Period unavailable. The corrected label fits without overlapping
the edit control. Existing current references and historical rows remain visible
after the in-place install. This confirms presentation/record visibility for the
inspected state, not a complete storage/archive acceptance gate. Capture removed
after inspection; `git diff --check` passed. Populated historical recap and actual
save/reopen were not exercised because this check did not create or alter records.

**Unsaved weight editor / swipe and rotation - 2026-10-10:** authorized
physical check on Samsung SM-S928B / Android 16, existing A931928A build.
Opened the current weight editor from Goals and made a temporary unsaved target
edit. Numeric keyboard kept the focused target and Save/Cancel reachable.
After hiding the keyboard, a downward swipe from the editor header opened Leave
without saving. Keep editing restored the visible editor and retained the draft.
Rotated the dirty editor to landscape and back to portrait: the target survived,
the field and Save/Cancel remained reachable, and no frozen or stranded editor
was observed. Back opened confirmation; Leave without saving returned to Goals
with the original saved target and daily references unchanged. No Save occurred.

Commands used: `android screen capture --annotate`, screenshot label resolution,
ADB input tap/text/keyevent/swipe, and temporary system rotation settings.
Restored the original accelerometer_rotation=1 and user_rotation=0. Initial
swipe at the system status area opened the notification shade rather than the
sheet; Back dismissed it, and the actual sheet-header swipe was then exercised.
The layout service remained unavailable, so local visual evidence was used.
All temporary captures were kept outside the repository and removed afterward;
no raw member values are retained here. These checks close the tested swipe
Keep editing and dirty-editor orientation round trip, not process-death recovery,
TalkBack, gesture-navigation mode, failure/retry or populated recap acceptance.
No production code change or installation was required. Next bounded acceptance
task: manual accessibility traversal, followed by populated recap checks when
member-intended records are available.

**Today voice-to-draft continuation - 2026-10-10:** member requested removing
the transcript-confirmation step from successful Today spoken meal entry.
MealEntryScreen now consumes only its matching successful validated audio result
and immediately submits the local tool-free meal proposal. Request consumption
precedes submission to prevent replay; manual typed submission shares the same
bounded helper. Draft loading covers submission as well as inference, avoiding
an intermediate editable-screen flash. Generation failure/Stop retains words for
editing and explicit retry. Recognition errors/empty speech retain the existing
voice/typing fallback. Cancellation, background clearing, strict parsing and
explicit review/Save remain; generic Chat speech is unchanged.

Member also reported a recognized saved meal without nutrition. Authorized local
inspection on Samsung SM-S928B / Android 16 confirmed saved portions but no
nutrition. The bundled 363-food Foundation catalog lacks cooked spaghetti and
meatballs; its existing tomato sauce was not found by the Spanish compound name.
Added the conservative Spanish tomato-sauce alias and regression coverage that
does not match plain tomatoes or invent matches for absent pasta/meatballs.
No nutrient values, replacement recipe, meat type or quantities were fabricated,
and no saved meal was rewritten. Broader prepared-food catalog coverage remains
an open nutrition task. Local screenshots were removed; raw private values are
not retained in this checkpoint.

Complete Android gate passed: 427 tests, 0 failures/errors; APK assembled.
SHA-256: `49A14BEDA03E6F869553E20251EEB8E4E1EDFC5C827C65F0A25BADBB54641C9C`.
Active-cycle in-place `install -r` succeeded; no uninstall or data clear.
Physical acceptance pending for continuous voice-to-draft, Stop/failure retry,
rotation/background cancellation and enlarged-font/accessibility states. New
regression tests cover successful matching audio consumption and stale, cancelled,
failed, wrong-operation and empty recognition results. No new dependencies,
cloud processing, persistence format, commit or push.

**Prepared-food catalog extension - 2026-10-10:** member approved filling the
spaghetti/meatball gap. Added a reproducible two-record USDA SR Legacy supplement:
169737 cooked enriched pasta without added salt and 171638 Italian-style frozen
meatballs. Existing Foundation sauce is calculated separately, avoiding the
FNDDS meatball-with-sauce double count. Exact public source URLs, dataset version,
nutrients and limitations are documented in food-catalog-development.md. All
available nutrients are preserved; no model-authored nutrition or recipe is used.
The composite release distinguishes the supplement from Foundation, rejects
duplicate IDs and retains every original food/value unchanged. Previously saved
meals are not automatically rematched or rewritten.

English/Spanish generic spaghetti/meatball aliases and Spanish display names
find the reviewable candidates. Explicit raw/dry and unsupported specific-meat
names remain unmatched. Regression testing exposed an unrelated-grain match
through the Cereal Grains and Pasta category; pasta searches now require pasta
in the actual food name, and generic spaghetti excludes sauce-only entries.
No new UI, runtime network, production dependency, vault format, commit or push.

Complete Android gate: 431 tests, 0 failures/errors, APK assembled. Four source
importer tests passed, including identity/recipe substitution and missing-macro
rejection. APK SHA-256:
`EC717F883BAC15A237A0976B8B9C17DAC7A981E0C280E80471021BEE1793F7C1`.
Active-cycle in-place `install -r` succeeded without data clearing or uninstall.
Physical new-draft lookup, reference/portion review, save-to-Lunch and reopen
remain pending on Samsung SM-S928B / Android 16. A successful calculation in
tests does not validate speech/model ingredient accuracy or a household recipe.

**Member-reported prepared-meal acceptance - 2026-10-10:** on the installed
431-test EC717F88 build, the member reported that the dish worked, changing its
portion recalculated nutrition, and saving/reopening retained the portions and
nutrition correctly. These are member-reported physical results; no new device
capture or private values were collected. They close the reported recalculation
and save/reopen checks for this example, not broad recipe accuracy, speech-language
coverage, or independent ingredient/source inspection. Existing saved meals are
not automatically rewritten. No production code or installation was needed.

Next bounded check: Stop during dedicated voice-to-draft preparation, confirm no
late draft appears and recognized words remain available, then retry to required
review without saving an unintended record. Error retry, rotation/background
cancellation, enlarged text and TalkBack remain open. The original meal-entry
proposal now agrees with FLOWS/SCREEN-SPECS on the approved automatic continuation
after successful recognition; editable words are the failure/cancellation fallback.

**Member-reported voice cancellation checks - 2026-10-10:** on the same installed
EC717F88 build / Samsung SM-S928B Android 16, the member reported that the requested
Stop/retry check worked. The subsequent Home/background check also behaved as
described: preparation cancelled, no late draft appeared, and a fresh attempt
worked after returning. These are member-reported physical checks, without new
captures or private content. They close those tested cancellation paths only;
rotation, actual generation-error retry, enlarged text and TalkBack remain open.
No production change or installation was required. Next bounded check: rotate
dedicated meal entry with an unsaved typed description and verify retained text,
reachable actions and no unsolicited inference or save.

**Member-reported meal-entry layout checks - 2026-10-10:** on the same installed
EC717F88 build, the member reported that an unsaved typed description survived
the requested landscape/portrait rotation, actions stayed reachable and nothing
submitted automatically. The member also reported that meal entry/review worked
with enlarged Android text and the keyboard open, following the requested
readability/action-reachability check. No exact font scale or independent capture
was collected; these are member-reported acceptance results for the exercised
states. TalkBack traversal and actual generation-error retry remain open. No
production change or installation was required; diff whitespace check passed.

**Member-reported meal-entry TalkBack check - 2026-10-10:** after instructions
to enable Android TalkBack and traverse the meal-entry flow, the member reported
that it worked. This records acceptance of the exercised traversal only, without
independent spoken-output capture or a comprehensive accessibility audit. The
member-reported checks now cover this example's nutrition recalculation and
save/reopen, Stop/retry, background cancellation, unsaved-description rotation,
enlarged text/keyboard reachability and TalkBack traversal. Actual generation-error
retry and broader varied-food/photo accuracy remain open. No production change,
installation or private-data capture was required.

**Encrypted meal-photo performance metrics - 2026-10-11:** member approved
retaining first-output/stage timings and thermal severity for the varied-photo
pilot. LocalAnalysisAudit now has an optional technical metrics object: image
preparation, conversation setup, model request, first nonblank streamed-text
callback, validation, and Android thermal severity at analysis start/end. The
existing total covers analysis after preparation; stage sums need not equal it.
Failure/cancellation retain measurements already reached, including elapsed
request time when started. Unknown values remain null, not zero. Generation
identity guards keep stale callbacks from changing a newer run's measurements.

The shared additive codec preserves absent/null legacy metrics, rejects malformed
numeric types, noninteger/out-of-range values, invalid thermal severity and first
output later than a known request duration. Vault writes remain unlocked-only,
atomic with rollback and bounded to 50 records. Recovery export still strips the
whole audit list. No content, photos, prompts, model output, cloud telemetry,
dependencies or changes to meal calculations are added. Existing diagnostics UI
is unchanged; this slice adds persistence, not a new metrics dashboard. Voice,
text/chat and label OCR performance history remain outside scope.

Complete Android gate: 435 tests, 0 failures/errors; APK assembled. Initial gate
found a test-only JSONObject.keySet Android API mismatch, corrected to keys()
before the successful complete rerun. Four new tests cover legacy/partial reads,
roundtrip/content-free fields, malformed inputs and bounded/order validation.
APK SHA-256:
`181B1CC22EE3AF09AF6D0D3DBEE6855C214FE2EA086B9DD9C13287D34268D459`.
Active-cycle install -r succeeded on Samsung SM-S928B / Android 16; no uninstall,
data clear, key replacement or authentication automation. Working tree remains
uncommitted; no push. Physical collection/reopen acceptance is pending: unlock,
run a member-chosen meal photo, then verify retained measurements after restart.
Locked/background cancellation intentionally cannot persist when the vault is
already locked; this extension does not promise every interrupted run is retained.

**Automatic meal-photo review - 2026-10-11:** member approved going directly from
successful analysis to meal review. An accepted image analysis now allocates a
transient request UUID, reused as its existing audit ID. The extracted capture
handoff and pure one-shot gate attach only to that new request and consume its
validated complete/partial result before opening review. The first transition
keeps the minimal loader rather than flashing the intermediate result card.
Stop/destination disposal invalidate navigation; returning from review cannot
reopen it automatically. Rejected/failed results remain in entry with the existing
manual/new-photo options. Label OCR and chat routes are unchanged. Selected
date/section, thumbnail consent, strict parsing, nutrition and explicit Save remain.
No production dependency or new vault format; no meal is saved by this navigation.

Complete Android gate passed: 440 tests, 0 failures/errors, APK assembled. Five
new tests cover complete/partial one-shot navigation, stale/unrequested results,
cancel/new-attempt isolation, rejected/failed/empty results and declined starts.
Initial compile found a nullable audit access, corrected before the complete
successful rerun. APK SHA-256:
`96279C9DFD0499AECB268F380D0FD539893659F5F9144FE5FB71B62C381ED47B`.
Diff whitespace check passed. Installation/physical visual acceptance remain
pending: ADB shows no connected phone; both currently advertised wireless
endpoints refused connection. Requested the member's current debugging endpoint;
no transient address or serial is retained here. No authentication inspection,
data clear, uninstall, commit or push. Next bounded acceptance: install -r, then
member-chosen photo -> automatic review -> save/reopen in selected section,
plus Stop/no-late-review, return/no-replay and partial/failure states. Capture
before/after evidence only after the member unlocks and authorizes inspection.

**Checkpoint audit and voice Stop correction — 2026-10-11:** preserved the
October 10 member-reported meal-entry TalkBack, enlarged-text/keyboard, rotation,
cancellation and save/reopen results. Independent review found a narrow manual
Stop/automatic-completion handoff race. Dedicated meal entry now retains its
attempt while the completed clip's callback is pending, shows Finishing recording
with an accessible state label and disables another recording. Cancel, exit and
replacement still reject/delete stale audio. The shared recorder and Chat
callers are unchanged. Three controller tests using a fake recorder cover pending
clip transfer, cancellation and replacement. Focused review found no remaining
material issue; full gate passed with 452 tests, zero failures/errors/skips,
debug assembly and release Kotlin compilation. APK SHA-256:
`392864e7c2a47db226e12f7007b06e327c1d5685bd2468ec46513c5bada01b9c`.
This correction is not installed or physically accepted, including its new
finishing-state TalkBack announcement. No new device capture or member record
inspection occurred. See [the working-tree checkpoint](../checkpoint-2026-10-11.md)
for scope, preserved acceptance, remaining gates and the proposed commit boundary.
