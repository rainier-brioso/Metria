# M4 local assistant checkpoints

## Post-demo acceptance backlog - 2026-10-27

Member explicitly defers the remaining assistant physical checks until after
the hackathon demo and authorizes committing/pushing the current checkpoint.
Deferral is scheduling, not a passed gate or release acceptance. The historical
no-commit/no-push notes below describe earlier authorization boundaries.

Resume with the current build/device identity and the no-save itinerary in
`TESTING.md`; preserve records, use temporary private evidence and restore any
system settings changed. Start with synthetic previews before real inference.

- [ ] Exercise a real contextual meal-analysis failure and recovery/manual
  fallback; confirm actionable feedback, no success receipt and no stale result.
- [ ] Exercise a real partial capture result; confirm missing-food/uncertainty
  explanation, editable Review and cancellation without saving.
- [ ] Repeat affected panel/capture/review navigation with Android gestures,
  including bottom actions and keyboard clearance; restore original navigation.
- [ ] Run guided TalkBack traversal and announcements for header, actions,
  composer, async state, errors and Review; restore original accessibility state.
- [ ] Explicitly confirm authentication is required after Home/background;
  unlock as member, verify no transient draft/activity replay and a fresh request.
- [ ] Finish enlarged-font/last-field IME checks on remaining assistant pages
  and secondary editors; existing named 150% passes remain scoped as recorded.
- [ ] When the member actually wants to record/revise food, verify contextual
  label/saved-dish receipt-only and Meal updated receipt success, View meal and
  cancellation/failure behavior. Final Save/update must be member-performed;
  this backlog does not authorize test records or edits.

Demo preparation: use the already-verified normal path and keep Metria in the
foreground while an unsaved draft is needed. Real backgrounding intentionally
locks and discards temporary work. The currently installed font-fix APK and the
later preview/extraction build are distinguished in the evidence below.

## Synthetic capture-state coverage and header Close - 2026-10-27

Member requests continued no-save checks and explicitly defers TalkBack.
Added three debug-only synthetic entry points in `AssistantCaptureStatePreviews.kt`
for loading/Stop, partial meal at 360 dp/150% font and recoverable failure.
They reuse extracted production loading, meal-card, error and source-action
composables with fictional data and inert callbacks. Existing panel state,
request checks, cancellation, save and picker wiring are unchanged. They are
presentation fragments in a real panel/header, not a full capture state-machine
simulator or a physical error/retry test.

Independent review finds no material blocker. Full Android unit/debug command
succeeds; reports show 749 tests, zero failures/errors/skips. APK SHA-256:
`2db1cebe490505387649a7197ca9d69a2497c512e9bc03df1153040983061d1b`.
This extraction/preview APK was not installed. All three new previews rendered
through Android Studio MetriaNative and were visually inspected immediately:
Stop, partial-food explanation/Review/Discard at enlarged font, and error/manual
entry/source action fit their initial viewports without blocking overlap.
The existing Chat error preview was also rendered/inspected; composer and error
remain distinct. Static rendering does not prove scrolling or screen-reader
announcements. Four renders, zero inference runs and zero installs in this slice.

Separate phone check on the already-installed font-fix APK `75deb0ea...`, Samsung
SM-S928B / Android 16: member unlocked; original icon opens retained Chat.
Header Close returns to Today. No message submitted or saved record touched.
Phone is left on Today. Temporary host screenshots and synthetic renders removed.
No setting changes, real photo selection or model run in this slice.

Still open: integrated real capture failure/partial/recovery, gesture-navigation,
explicit member confirmation of authentication after Home and wider accessibility.
TalkBack remains deferred at the member's request. Source/previews do not close
those physical gates. No new record Save/edit/delete authority or commit/push.

## Contextual capture Stop continuation - 2026-10-27

Post-unlock controls continuation on the same installed `75deb0ea...` APK and
reference phone: member replied ready after the unlock request, without an
explicit statement that authentication was requested. Treat that as readiness,
not independent confirmation of the preceding lock gate. Today is visible;
opening the original assistant icon restores completed Chat and its existing
receipt, with no active temporary photo draft. Expanding and reducing the idle
panel preserves those messages and usable composer. Tapping the exposed scrim
closes the compact panel to the same Today screen without activating its
underlying control. This does not exercise header Close or TalkBack traversal.
No text submission, model run, setting change, record mutation or build/install
occurred. Temporary screenshot removed. Background lock confirmation, integrated
capture partial/error, TalkBack and gestures remain open. Discovery confirms
existing error/partial synthetic previews cover Chat rather than the integrated
capture page; no physical acceptance is inferred from them.

Corrected-build font continuation: on installed `75deb0ea...`, Samsung
SM-S928B / Android 16, member unlocked and selected a meal image. Foreground
system font 1.0 -> 1.5 preserves the capture page and identical selected photo;
the stable enlarged layout expands the panel and wraps copy. Scrolling reaches
the complete Prepare draft/source actions. One new local analysis completes
with a reviewable unsaved proposal at 150%. Review opens with complete final
Save/Leave actions above three-button navigation; neither Save nor any field
edit was performed. Font 1.5 -> 1.0 while Review is open preserves the same
review and proposal. System setting independently reads back 1.0. Rotation and
navigation mode were not changed. These named foreground font gates pass; no
claim for every assistant page, IME at 150%, TalkBack or gesture navigation.

Home/background then warm reopen was exercised to check unchanged locking.
The immediate resulting capture was black/protected, so it is not sufficient
evidence of the lock UI or draft cleanup. Member confirmation/unlock requested;
background-lock physical acceptance remains pending that observation. No
biometric automation or auth inspection attempted. No record Save/edit/delete,
new install or source change in this continuation. Reused temporary captures
were removed after visual inspection. No private values/images kept in docs.

Follow-up on the same installed build: member reselected the photo. A fresh
Prepare draft shows inline loading and completes with a reviewable unsaved
proposal. Review opens; Leave without saving returns to that same capture draft.
No Save or field change. This closes the named Stop/reselect/new-analysis/review
sequence on this build, not generic failure/partial-result acceptance.

An additional physical font-scale check exposed a restoration failure: changing
system font from 1.0 to 1.5 while the completed capture draft was open dismissed
the panel to Progress. Reopening the icon showed retained Chat instead of the
active capture. Source audit finds fontScale absent from handled configuration
changes, while Activity teardown/lock cleanup invalidates process-local work.
This is a failed active-flow font-change gate; do not count it as accepted from
the static large-text previews. System font was restored and independently read
back as 1.0; rotation was not changed (original free). A bounded fontScale
configuration repair is being prepared; new-build physical verification and
real-background lock regression remain required.

Repair follow-up: MainActivity now handles only the additional `fontScale`
configuration in place, like its existing handled rotation. Compose updates the
font without intentionally replacing the Activity; no change to onStop/onDestroy,
vault locking, image retention, saved state payloads or dependencies. Independent
review finds no material blocker. Full
`:app:testDebugUnitTest :app:assembleDebug --no-daemon` succeeds; unchanged unit
tasks are up to date and retained XML reports show 749 tests, zero failures/errors.
Diff check passes. APK SHA-256:
`75deb0ea479a0be06dfa5e9d773d09e2ee177d721fbae8a637afcea69f7a0df3`.
Installation and new-build physical result are recorded separately below.

The repair APK installed in place on the same reference phone (`install -r`:
Success) and cold-launched (Status ok, 2,464 ms). Existing app identity/private
storage retained; no clear/uninstall or record operation. Member unlock and photo
reselection requested. Active preview/draft/review font-change acceptance and
actual Home/background lock regression remain open on this new APK.

Read-only phone QA on the existing Progress checkpoint APK `090d237e...`
(source `838ea19`, 749-test/debug gate), Samsung SM-S928B / Android 16.
Member selected a meal photo. Prepare draft displayed inline preparing feedback
and a reachable Stop action. Tapping Stop returned to the empty capture selector.
A further stable observation approximately one minute later still showed the
selector, without a late draft. This is one observed cancellation window, not
proof of every native completion race. No record Save/edit/delete, new install,
configuration change or biometric automation occurred.

Read-only explorer audit confirms photo removal is intentional: panel reset
revokes its request and preparation/OCR generation; lifecycle cancellation
revokes the analysis generation and deletes the temporary image. The current
contract requires choosing a photo again to retry. Do not classify this as an
unintended draft-loss defect or silently extend temporary-photo retention.
Member asked to reselect the photo for the next bounded check: a fresh analysis
after Stop and unsaved Review/cancel. Partial/error/retry and accessibility
acceptance remain open. Temporary host screenshot removed after inspection;
no image, member values or device endpoint retained in this note.

## Source checkpoint before progress chart work - 2026-10-27

Member authorizes commit and push of the pending assistant/OCR/drink-quantity
work, synthetic previews and QA guidance before implementing longer chart ranges.
Checkpoint review found no material blocker. The complete Android command
`:app:testDebugUnitTest :app:assembleDebug --no-daemon` succeeds (tasks up to
date; retained XML reports: 745 tests, zero failures/errors/skips), and
`git diff --check` passes. No new install or physical acceptance in this step;
the open physical gates below remain open. The generated local daemon Java 25
configuration is excluded from the source checkpoint. Progress chart work has
not started; next bounded implementation is selectable ranges with a three-month
weight default, actual dated measurements and a read-only detail view.

## Assistant prototype completion - 2026-10-26/27 (implemented; physical gates partial)

Final follow-up 2026-10-27: full Android gate again passes 745 tests with zero
failures/errors/skips; independent incremental review and diff check pass.
Final APK SHA-256:
`896b069ce1d8565fe9e8308503042a6afa10900eb14f1292c528fe398d735c22`.
Installed in place on reference Samsung SM-S928B / Android 16: Success; cold
launch Status ok, 2,413 ms. No clear/uninstall/data replacement. Member unlocks.
Retained Chat and saved dishes remain visible after update; this is not a full
vault/recovery data audit.

On that final APK, explicit saved-dish selection opens Review saved dish with
the correct saved-food/portion explanation. Both footer actions are fully outside
three-button system navigation in portrait and landscape. The same active review
and saved portion remain after rotation. Leave without saving returns to the
retained Chat with no new receipt. No agent record Save/edit/delete. Original
automatic rotation (`free`) restored and rechecked. A fifth affected synthetic
preview (`SavedDishReviewPreview`) rendered and was visually inspected with
complete actions. Temporary screenshots stay outside the repository and are
removed after use; no raw member data belongs in acceptance notes.

Label continuation on the same final APK/device, 2026-10-27: one fresh
member-selected dual-column label was read locally. The four reviewed nutrients
use the same per-serving column; no nutrient/date/name values were edited.
Review shows the local-time default with a checkmark and selected text.
Changed category, then cleared it; Not classified survives landscape,
return to portrait, Back to draft and reopening Review without another OCR run.
In landscape, scrolling reaches the complete category/reference-photo row and
Save above the dock/system navigation. In portrait, focused Date remains visible
with the keyboard; scrolling exposes the complete category/reference-photo row
and Save above the IME. Keyboard dismissed; confirmed discard of this unsaved
test capture returns to retained Chat without a new receipt. Original rotation
(`free`) restored/rechecked. Temporary screenshot removed. No agent record
Save/edit/delete, no new build/install, no broader OCR-accuracy claim beyond
this image. Same 745-test gate remains applicable.

Next bounded acceptance: real Save/update receipts only when the member wants
to record/revise a meal; no agent Save authority. New label/saved-dish
receipt-only and updated-meal success paths need member-performed Save/update
for physical acceptance. Broader async Stop/late-result, failure, gesture,
system-font/TalkBack acceptance remains open. Implementation is complete for the
three identified gaps; this is not full assistant/release acceptance.

Continuation 2026-10-27: implementation and independent review complete for
the three prototype gaps. Full Android unit/debug gate passes 745 tests, zero
failures/errors/skips. Added update-receipt and deterministic writer-ordering
regressions; existing null/expired receipt tests remain applicable. Initial
completion APK SHA-256:
`e66f477d53b81baefd83929f713daa5d8bda125ece5fce549ab43ec268668179`.
Installed in place on Samsung SM-S928B / Android 16 (`install -r`: Success),
cold launch Status ok, 1,578 ms. No clear/uninstall or data replacement.

Four synthetic previews rendered and were visually inspected: populated
saved dishes at 360 dp/150% font, empty dishes, updated-meal Chat receipt and
label Review at 150% font. No blocking overlap/truncation in those viewports;
scrolling/native window/IME acceptance is separate. New selector previews first
hit a mixed Layoutlib class/signature cache; refreshing the out-of-date preview
from Studio completed a build and resolved it without production changes.

Physical checks on that APK: retained Chat/receipt and original icon observed;
sixth + action is fully reachable by scrolling; direct saved-dish list opens;
search focus with keyboard keeps controls/results visible; fictitious no-match
query displays its empty-result message; a matching query filters and opens
the existing unsaved Review with the saved portion. Leave without saving returns
to the unchanged Chat. No record save/edit/delete, image selection or inference
by agent. CLI layout instrumentation still returned an unrecognized response;
authorized temporary screenshots were used instead.

That Review exposed two defects: misleading rejected-manual-draft intro and
bottom Leave button overlapping three-button navigation. Follow-up now uses
saved-dish-specific title/intro while retaining manual review business rules,
and enables the existing measured-dialog inset treatment for every
assistant-origin Review. Incremental independent review is clean. Added
`SavedDishReviewPreview`. Final gate/install/phone retest is recorded above;
the initial Review does not count as a layout pass.

Member authorizes completion of remaining panel differences: direct saved-dish
selector, contextual label category/save receipt and assistant meal-update
receipt. Existing local OCR/drink work is preserved. Contracts are updated in
`design/assistant-context-panel.md`, `design/FLOWS.md` and
`design/SCREEN-SPECS.md`.

Independent discovery confirmed these exact gaps against the interactive
prototype. Implementation review found an in-flight transcript writer could
overwrite a newly persisted meal receipt; completion therefore includes shared
writer ordering for all assistant receipt-bearing saves. Failure feedback must
be visible within the saved-dish page, and a failed meal save must retain the
unlocked shell/draft rather than changing global vault status to Failed.

Acceptance itinerary after the final automated gate and in-place installation:

- Open + > My saved dishes, search by name/alias/component, inspect no-match,
  clear search, choose a dish and return from Review without saving. Check
  keyboard, scrolling and rotation; selection must not create a record.
- Inspect contextual label category default, change/clear and retain across
  review return/rotation. Member chooses any real label image. Agent does not
  perform final Save; label receipt success needs a member-performed Save.
- Open existing View meal, inspect editor and cancel without changes. A real
  Meal updated receipt likewise needs a member-performed explicit update.
- Existing broader Stop/late-result, partial/error, enlarged-system-font and
  TalkBack gates remain separately open until physically exercised.

Synthetic fixtures add populated/empty saved-dish lists and an update receipt.
First preview attempt could not load the unbuilt new production class; first
Android compile exposed missing imports and nullable category inference. These
are not passed gates; final build/render evidence follows after repairs.
No member record Save, edit or deletion by the agent is authorized here.

## Contextual meal save acknowledgement and time category - 2026-10-26

- Member-performed Save follow-up on installed `2bd2e81c...`, Samsung SM-S928B /
  Android 16: member confirms the new meal was created with the correct category.
  Authorized screen inspection independently confirms Chat contains the final
  saved-meal acknowledgement and View meal action. Opened that action and
  confirmed it resolves to the same saved meal; left the editor without saving
  and observed the acknowledgement/link still present in Chat. No field edits,
  duplicate record, additional Save or delete by agent. Temporary screenshots
  removed. Today categorization is member-confirmed, not independently inspected
  during this follow-up. This closes the observed existing-conversation happy
  path; empty/expired-history physical tests and failure-path acceptance remain
  separate gaps. No broader assistant accessibility/Stop acceptance claim.
- New-build physical category check on reference Samsung SM-S928B / Android 16
  with installed `2bd2e81c...` (743-test/debug gate): member regenerated and opened
  Review. Meal details shows the expected local-time default already selected.
  Changed to another category and observed the summary/chip update, then tapped
  it again to clear. The unclassified choice survives landscape and return to
  portrait rather than refilling from the clock. Restored the original default
  and confirmed it selected. Original automatic rotation (`free`) restored.
  No final Save, food/portion edit or existing-record write by agent; draft stays
  open in Meal details. Temporary private screenshot removed. Actual saved-meal
  receipt, View meal and Today grouping still need a member-performed Save after
  the member finishes reviewing this draft. No success claim for that path yet.
- Member reports manually saving the meal: record exists in Today / Other meals,
  but contextual Chat has no acknowledgement. This is member-reported save
  evidence, not an agent-run Save test. Existing record was not changed.
- Contextual capture now opts into the existing transactional saved-meal receipt
  and synchronizes the returned conversation only on successful save. Review
  origin flags remain unchanged, preserving photo/manual override and dismissal
  semantics. Empty/expired history can start a fresh receipt-only conversation
  on this explicit capture Save; expired messages never return. Valid host-linked
  receipt-only snapshots survive normal conversation serialization/hydration;
  ordinary assistant-only greetings still do not persist.
- Assistant-origin meal Review opts into an editable category default from the
  existing local-time policy after explicit Today/member choices. Time is frozen
  once per draft before category-aware state identity is derived. Changing or
  clearing the category remains saveable review state. Other entry paths and
  existing saved records retain their previous classification behavior.
- Two custom workers and independent reviewer complete; no actionable findings.
  Five new policy tests cover empty/expired receipt histories, receipt snapshot
  round-trip, category precedence and time boundaries. Full gate
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passes 743 tests with
  zero failures/errors/skips; diff check clean. APK SHA-256:
  `2bd2e81c153ee754877ded92865000f63b94d07d95272115c5c2af82bb7b65e1`.
- Installed in place on Samsung SM-S928B / Android 16 with `adb install -r`:
  Success. No clear, uninstall, new signing key or private-storage deletion.
  Cold launch returned Status ok, 2,637 ms.
  Member unlock and new-build physical verification requested. Existing saved
  meal was not reclassified or duplicated by the agent.
- Added and rendered `AssistantMealSavedPreview` with fictional data: confirmation
  and View meal both appear without visible clipping. Temporary PNG removed.
  No agent-written meal or new physical acceptance from tests/preview.
- Physical acceptance pending: save from a new capture with empty/existing Chat,
  verify confirmation and View meal, check Today category, change/clear category
  through rotation/review, and verify failure keeps the draft without receipt.
  Final Save is a member action in the current test cycle; no automatic test save.

## Review rotation and dish editor physical follow-up - 2026-10-26

- Target: Samsung SM-S928B / Android 16, three-button navigation, existing
  installed APK SHA-256 confirmed from base.apk:
  `88d8206717b68ded3112c821eff3681c5b26f2661601e1f95b7e3e38f9ec7de0`.
  Its recorded full unit/debug gate passes 738 tests. No build or installation
  in this pass; existing installation/data left intact. Newer preview APK is not
  the build tested on the phone.
- Member had already regenerated a meal-image draft and opened Review. Verified
  both complete Review footer actions outside system navigation in landscape;
  rotating back to portrait preserves that unsaved draft.
- Opened the existing dish editor. Both Apply dish changes and Cancel changes
  remain complete above navigation in portrait and landscape. Focused a lower
  visible ingredient-description field without typing: focused field and both
  editor actions remain visible above the keyboard in portrait. This is not a
  last-field-in-every-editor or landscape-keyboard acceptance claim.
- Dismissed keyboard, cancelled the editor without applying any change, and
  confirmed return to Review with the same unsaved draft. No Save, remembered
  dish write, correction or value edit was performed. Original automatic rotation
  (`free`) restored and rechecked. Temporary private screenshot removed; no raw
  member content retained in evidence.
- Member separately reports Home/background discards a draft even without a
  process kill. Read-only explorer audit confirms existing policy: MainActivity
  onStop locks the vault outside guarded external-image/archive transitions;
  shell/model session invalidation clears the in-memory proposal/capture state.
  M2 checkpoint specifies actual backgrounding locks the vault. No background
  reproduction or persistence-policy change was made with the fresh draft.
- Still open: gesture navigation, physical enlarged font/display size and
  TalkBack, last-field reachability across all editor content, stable async
  Stop/late-result checks and applicable partial/error recovery. Save/reopen
  remains excluded by member instruction. Next bounded task: plan the remaining
  accessibility checks without unnecessarily backgrounding this draft.

## Synthetic visual previews - 2026-10-26

- Studio visual follow-up: Rabbit 1 / 2026.2.1, MetriaNative ready. All nine
  preview entry points rendered successfully and each synthetic PNG was viewed.
  Initial-view hierarchy, wrapping, uncertainty labels and complete footer actions
  show no blocking overlap/truncation. Narrow and landscape Review at 150% font
  keep both footer buttons visible; landscape has little remaining content height.
  Static rendering does not prove scrolling, native dialog frame, IME/system bars,
  TalkBack, real async cancellation or lock restoration. Exact per-preview scope
  and limits are in `testing/assistant-previews.md`. Nine synthetic renders,
  no new builds/installs/inference/phone interactions; temporary PNGs removed.
  No Android source change or new physical pass. Next bounded device check is
  shared Review landscape + dish-editor footer on the previously installed build,
  using a fresh member-selected meal photo and no saves.
- Added nine debug-only previews in `AssistantAndReviewPreviews.kt`: real
  conversation welcome/preparing/partial/error, real action choices, normal meal
  review, partial landscape and narrow review at 150% font, and scrollable partial
  label review at 150% font. All data are invented, clocks fixed and persistence
  callbacks inert. No camera, OCR, model, vault, dependency or navigation entry.
- Minimal main-source seams: shared inner MealReview is internal; label review
  accepts initial date/time with existing runtime defaults and unkeyed saveable
  state; fitted-dialog footer avoids an unused platform metrics lookup. No
  change to other dirty drink work or the current assistant icon.
- Worker implementation and independent static review complete. Full gate
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passes: 738 tests,
  zero failures/errors/skips; diff check clean. No new instrumentation framework.
  Built APK SHA-256:
  `4afe964ce6c6e0ff95cec393b69bcea7c5f493840442116a0c275c0135ae67ad`;
  not installed. Existing phone acceptance belongs to the earlier APK.
- `android studio check` found no running instance; visual render acceptance is
  still pending. No phone access, install or record save in this slice. Preview
  success cannot close the outstanding physical footer/rotation/IME/accessibility
  and async gates. Member reports prior live draft lost on phone lock.
- Usage, render command and limits: `testing/assistant-previews.md`. Next bounded
  task: render the previews with `native-android/` open in compatible Studio,
  inspect normal and enlarged-font variants, then resume remaining phone gates
  with a new member-selected image; no record saves.

## Centered calorie OCR follow-up - 2026-10-26

- Member reports the unsaved meal-analysis draft was lost when the phone locked.
  Do not assume it remains available or infer a recreation/restoration pass.
  Member authorizes synthetic assistant and review previews before further phone
  checks. No record saves are authorized; real-photo continuation needs a fresh
  member-selected image when device checks resume.
- Physical follow-up on `88d82067...`, Samsung SM-S928B / Android 16:
  portrait Review shows both complete footer actions above three-button navigation.
  Actual requested content height matches the fitted window height (2108 px).
  Meal-name focus and the expanded final Date field remain visible with the
  keyboard open; both footer actions stay reachable above it. No field values
  changed and no records saved. Keyboard was subsequently dismissed.
  Landscape rotation was not executed: automatic approval review failed because
  of a usage limit, not a finding that the action was unsafe. Landscape and dish
  editor footer checks on this APK remain pending, alongside the prior async and
  accessibility gaps. Member now reports the meal-analysis draft open and asks
  to prioritize visual-debugging workflow research. No new device inspection or
  installation was performed for that research; see `testing/visual-debugging.md`.
- `40239b54...` physical retest still fails: secondary footer partially clips
  below the screen. Native window geometry exposes the mismatch: content requests
  1080x2340 while its actual fitted frame is 1080x2108, excluding status/navigation
  bars. Cached Compose UI is 1.7.6; its non-platform-width DialogLayout measurement
  uses display configuration size, consistent with the observed clipping.
- Bounded workaround for contextual review only: retain the platform's fitted
  dialog measurement (`usePlatformDefaultWidth=true`), then set that dialog window
  to MATCH_PARENT in both dimensions and ADJUST_RESIZE. Root has no separate
  system-bar inset and opted-in footer adds none; native frame already fits bars.
  Legacy entry paths retain prior settings. Worker implementation, independent
  static review and full Android test/debug gate pass: 738 tests, no failures,
  errors or skips, clean diff check. APK SHA-256:
  `88d8206717b68ded3112c821eff3681c5b26f2661601e1f95b7e3e38f9ec7de0`.
  Installed in place on Samsung SM-S928B / Android 16: Success; cold launch
  Status ok, 1,063 ms. Member unlock and direct Review retest requested.
  Confirm actual window/content size and footer/IME/rotation on this APK before
  accepting the workaround. No record saves or private evidence retained.
- Full-screen retest on `812a1a7a...`: meal analysis completed and Review opened,
  but the secondary footer still overlapped three-button navigation. The first
  helper-only inset repair therefore fails physical acceptance.
- Follow-up repair explicitly gives only contextual assistant review control of
  its dialog window (`decorFitsSystemWindows=false`) and consumes system-bar
  insets once at the Surface boundary. Existing inner IME padding remains;
  footer manual navigation padding is skipped on this opted-in path. Other
  review entries keep prior behavior. Independent static review found no blocker.
  Full Android test/debug gate passes 738 tests, zero failures/errors/skips;
  diff check clean. APK SHA-256:
  `40239b5433705a48442d9d9bf61f9a7e99ea39003abb54a5d175f1a81610a374`.
  Installed in place on Samsung SM-S928B / Android 16: Success, cold launch
  Status ok in 1,868 ms. Member unlock and direct Review retest requested.
  New-build footer/keyboard/landscape physical verification remains pending.
- `812a1a7a...` physical Stop check: member selected a meal image; Prepare draft
  entered loading with Stop visible. Tapping Stop returned immediately to the
  empty inline capture, with no preview or proposal. No record saved. A later
  inspection after a 20-second wait showed Chat in Android split-screen rather
  than the same capture page; cause of that external/layout transition is not
  established. No delayed-result/restoration pass is claimed from that capture.
  Member requested to restore full screen and reselect image for final footer
  verification. Stop's immediate UI reset is observed; stable late-result check,
  footer acceptance, font scaling and TalkBack remain pending.
- Assistant-capture review footer repair: opt-in Compose dialog navigation inset
  is combined with the existing platform inset using max, not addition. The flag
  travels through the review and dish editor; only contextual assistant capture
  opts in, with false defaults elsewhere. No persistence/business-rule changes.
  Independent final wiring review and full Android test/debug gate pass (738
  tests, zero failures/errors/skips); diff check clean. APK SHA-256:
  `812a1a7a939046dc9870db1570208a82b36bb94c4a4a7582bb0f001bb5a6a73e`.
  Installed in place on Samsung SM-S928B / Android 16: Success; cold launch
  Status ok, 3,181 ms. Member unlock and fresh meal-photo preview requested.
  New-build physical footer acceptance remains pending; earlier meal-analysis
  observations below apply to `3db12eb3...`.
- Meal-photo follow-up on `3db12eb3...`: member-selected image preview and
  Prepare draft succeeded; inline cancellable loading was visible and a validated
  meal draft appeared. Review draft opened the shared complete meal editor,
  including dish/food detail and uncertainty/review controls. Opened one dish,
  cancelled its changes without editing, then Leave without saving returned to
  the same contextual draft. No meal, personal dish or correction was saved.
- Physical defect found in that editor: the bottom secondary action border
  extends into three-button system navigation. Independent review traced the
  manual inset query in SystemSafeBottomActions; a bounded assistant-entry-only
  correction is in progress. Do not mark meal-review layout accepted yet.
  Stop during actual work, partial/error, enlarged fonts and TalkBack remain open.
- Physical retest of `3db12eb3...` on Samsung SM-S928B / Android 16: the same
  member-selected dual-column label now produces all four reviewed nutrition
  fields matching its left/per-serving column, including calories. This named
  previously failing image passes; this is not a general OCR accuracy claim.
  Preview, explicit Read label and inline Review succeed. No fields edited or
  records saved; no private values/photo retained in repository evidence.
- Same APK: Date focus stays visible above the keyboard; scrolling exposes the
  whole reference-photo row and Save button above IME. Save untouched. Landscape
  retains the draft, with fields/Close clear of right system navigation. Returned
  to portrait and free rotation. Back from Review returns to draft; subsequent
  Back requests discard confirmation. Confirmed discard returns to retained Chat.
- Same APK: Analyse meal opens inline. OS image picker opened and was cancelled
  with Back; camera opened and was cancelled without taking a photo. Both return
  to the usable empty meal capture panel, with no preview or leftover draft.
  Member-selected meal image requested for analysis/review/Stop checks. Enlarged
  text, TalkBack and other remaining assistant gates stay open. Save/reopen is
  excluded from this cycle by the explicit no-record-save instruction.
- Follow-up after observed failure: detached numeric OCR lines now use their
  individual element boxes before whole-line amount parsing. Synthetic regression
  covers a separate Calories label plus one line containing both energy values.
  Vertical row ownership rules remain unchanged; no speculative overlap expansion.
  Complete Android test/debug gate passes 738 tests, zero failures/errors/skips;
  diff check clean. APK SHA-256:
  `3db12eb336d957c853f59c517abb6d674d0ad328ee68a7f43fe47574c95e958d`.
  Installed in place on Samsung SM-S928B / Android 16: Success; cold launch
  Status ok, 1,861 ms. Member unlock and repeat image selection requested.
  Physical accuracy on this follow-up APK remains pending. No records saved.
- Physical follow-up on `78188365...`: member unlocked; retained Today state and
  Chat history visible, original icon opens the panel, five actions render.
  Member selected the same dual-column label; preview > Read label > Review
  succeeded, but calories remained unknown. This APK does not close OCR accuracy.
  Optional detected-text view showed both energy figures grouped on one numeric
  line separate from the Calories label. Parser currently rejects that whole
  line instead of using its available element boxes; bounded follow-up in progress.
  Back requested discard confirmation; Keep editing was selected. No record Save
  or field edits. Private screenshots are temporary and not repository evidence.
- Worker implementation and independent reviewer inspection replaced transitive
  coordinate clustering with boundaries from paired values on individual rows.
  Repeated paired macro rows establish the split for larger/centered calories;
  otherwise all paired evidence is retained. Missing left values and values on
  an ambiguous boundary stay unknown. Unpaired rows use a narrow left-edge
  region so differing number widths do not discard aligned values.
- Seven additional regressions cover centered/ambiguous calories, right-only
  macros with inline or separate calorie boxes, narrow/scaled columns and
  missing paired evidence. The complete required command
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passes 737 tests with
  zero failures, errors or skips. `git diff --check` is clean. APK SHA-256:
  `781883652102068a241cecf1015b0fab298fe06a9ec70f0adeb3dac49411c8ae`.
- Installed on Samsung SM-S928B / Android 16 using authorized `adb install -r`:
  Success. Cold launch Status ok, 1,112 ms. No clear/uninstall or record Save.
  Member unlock requested; retained records still need fresh visual confirmation.
- Real-label accuracy remains pending on this APK. Coordinate evidence cannot
  reliably distinguish nearly overlapping unpaired columns; OCR remains an
  editable draft requiring package comparison. No raw photo/OCR is retained.
- Member explicitly requests no record saves. Save/reopen will not be exercised
  in this cycle. Pending phone checks include fresh OCR, capture cancellation,
  meal analysis/Stop/error/partial, enlarged text and TalkBack. Previous keyboard
  and landscape observations below apply to the earlier installed APK only.

## OCR column repair and remaining panel gates - 2026-10-25

- Physical retest of `092f092c...` with the member-selected dual-column label:
  preview, explicit Read label and inline Review succeeded. All three macros
  matched the same left/per-serving column; calories remained unknown despite
  visible energy values. This is partial improvement, not an OCR accuracy pass.
  Investigating centered/larger energy-row geometry using synthetic fixtures;
  no private OCR content or photo is retained here.
- On that build, Date focus remained visible above the IME and scrolling exposed
  the complete Save action above the keyboard. Save was not pressed and no fields
  changed. Rotation retained the draft; review fields and Close stayed clear of
  the right system bar. Free rotation restored. Font scaling/TalkBack and the
  other pending assistant interaction gates remain open.
- Resumed repair: local ML Kit element boxes now reach the deterministic parser.
  Geometry takes precedence over flattened text and uses one shared left value
  column; missing/ambiguous fields stay unknown. Anchored amounts reject merged
  multi-value rows without element geometry. Split kJ/kcal/% units are handled;
  trans-fat and calories-from-fat rows cannot substitute for totals. Accented
  Spanish regression is included. Custom explorer/worker/reviewer contributed;
  parent completed the interrupted worker patch and confirmed reviewer findings.
- Final full Android test/debug assembly gate passes 730 tests, zero failures,
  errors or skips; diff check clean. APK SHA-256:
  `092f092c84b7d253e563753ece013fe78a824988d7e07ea8367a43e0a96166ce`.
  Real dual-column photo accuracy and the landscape inset repair still require
  verification on this build; automated results do not close physical gates.
- This APK installed with `adb install -r` Success on Samsung SM-S928B / Android
  16 and cold-launched with Status ok (1,324 ms). No clear/uninstall or record
  Save was performed. Member unlock requested before continuing physical gates;
  existing-record retention has not been freshly inspected on this build.
- After member unlock, Today and retained Chat history were visible; original
  assistant icon opened the panel and all five actions rendered. Label capture
  opened inline. On this APK, landscape inspection shows Close and the content
  clear of the right system navigation bar; scrolling reaches Take photo without
  overlap. Rotation restored to free. This is a named visual/interaction check,
  not complete accessibility or review-editor acceptance. Dual-column image
  retest requires member selection; no record was saved.
- Member authorized correcting mixed-column OCR and continuing phone checks,
  requesting custom subagents. Explorer traced flattened-text precedence and
  per-field spatial fallback; worker owns parser/tests and reviewer checks safety.
- On the previously installed `a39cc92a...` APK, a synthetic unsaved product name
  survived editor Back > draft > Review and rotation to landscape. No Save.
  Landscape exposed a real defect: the right system navigation bar overlaid
  Close and field edges. Panel now reserves horizontal safe-drawing insets;
  fresh build and phone verification remain pending.
- Changing system font scale to 1.3 recreated the activity and yielded a black
  capture; no enlarged-text pass is claimed. Original font scale 1.0 and free
  rotation mode restored. User unlock may be needed for further physical checks.
  No raw photo or private extracted values are retained in repository evidence.

## Integrated contextual capture - 2026-10-25

- Follow-up with a member-selected label confirmed the private preview, actual
  OCR draft and inline nutrition review. Date focus remained visible with the
  keyboard open. No record was saved; extraction correctness was not accepted
  (multi-column labels require comparison against the package). Verification of
  the final action with the keyboard open was interrupted by a tool usage limit.
  On resumption, Chat and the integrated menu were visible, but reopening label
  capture showed its empty state; the prior transient draft was unavailable.
  The cause across the interruption is not established. A fresh member-selected
  image was requested to complete that keyboard check.
- Resumed physical check on the same installed APK: member selected the label
  again; preview, local OCR and inline review succeeded. With Date focused and
  the keyboard open, scrolling exposed the complete reference-photo row and Save
  button above the IME. This named portrait/default-font keyboard gate passes.
  No values edited or records saved. OCR mixed serving/container columns in the
  selected dual-column label; extraction correctness fails for this case and
  remains an open nutrition-parser issue, not a passed scan accuracy gate.
  Keyboard dismissed and review left open. No production source change/reinstall.

- Final APK phone inspection after member unlock: options/actions render inline;
  both capture actions open the new panel over Today, original icon/dock retained.
  Meal capture Back returns to Chat and scrolling exposes the last note above
  navigation. No old capture sheet observed. Left label capture open for a
  member-selected image; actual preview/OCR/review and remaining physical gates
  are still pending. No record Save or new message. Sanitized evidence in
  `design/assistant-context-panel.md`; temporary captures removed.

- Member identified old scan/meal capture routes behind the new action menu.
  Both now open inline capture pages over the existing destination: source,
  preview, optional meal description, actual local progress/Stop/cooldown,
  partial or failed result, review and discard. Labels review inline; photo
  drafts retain the complete shared meal editor. Today routes stay unchanged.
- Options also offers Privacy and Lock. Original icon/navigation and twelve
  unrelated tracked diffs preserved. No model, dependency, vault schema or
  record-save authority changed. New parsing remains in a pure validation policy.
- Complete Android test/assemble gate: 718 tests, zero failures/errors/skips.
  Four regressions cover stale/cancelled/current photo results and label input
  validation. Diff check clean; final APK identity in `design/assistant-context-panel.md`.
- Reference phone Samsung SM-S928B / Android 16 connected. A temporary before
  capture found the sleeping screen, so no before/after visual pass is claimed.
  Initial integrated APK installed with `install -r` Success and cold launch
  Status ok (1,195 ms). Final verified accessibility/setup APK also installed
  with `install -r` Success and cold-launched Status ok (1,474 ms).
  No clear/uninstall/private-data replacement or commit/push. Await member unlock.
- Next bounded task: verify + > both capture pages and header options on phone,
  then camera/picker cancellation, preview/keyboard, Stop/error/partial, reviews,
  explicit Save/reopen, rotation/font scaling/TalkBack. Physical acceptance and
  the separate drink/voice gates remain open.

## Contextual assistant panel - 2026-10-24

- Member rejected the first installed styling as unlike the approved prototype.
  Visual correction replaces the old bubble/composer treatment and floating menus
  with the prototype's white surface, soft bubbles, unified composer and inline
  actions/options, and reuses the existing navigation dock. Earlier smoke evidence
  is not visual acceptance; the corrected APK needs a fresh on-phone review.
- Corrected build passes 714 tests and debug assembly; diff/preservation audit
  passes. In the authorized active test cycle, `install -r` returned `Success`
  and cold launch `Status: ok` (2,504 ms) on the same Samsung. Await member
  unlock and a fresh prototype-versus-device visual check. Corrected APK hash
  is recorded in `design/assistant-context-panel.md`.

- Member approved only the assistant panel, keeping the current central icon,
  all assistant functions and the rest of the app unchanged. Found and preserved
  another session's uncommitted Chat drink-unit code/tests/docs on `ee36df2`.
- Shared Chat now uses a resizable in-window panel with unchanged cards,
  evidence, native Copy, voice activity/auto-submit, review and selected-meal
  context. Existing tools move to `+`; New conversation and Assistant tools are
  in header options. New UI remains outside MemberShell; that file only wires
  entry/actions and the existing guarded transcript writer.
- Final full debug test/build gate passed: 714 tests, zero failures/errors/skips,
  including four layout policy tests; diff check passed. No phone installation or captures,
  no physical acceptance, commit, push, dependency or data migration.
- Member authorized installation and verification. Verified APK installed with
  `adb install -r` (`Success`) on Samsung SM-S928B; cold launch returned
  `Status: ok` (1,309 ms). No clear/uninstall; retained records have not yet been
  fully verified. Member unlocked; retained conversation visibly restored.
- Physical smoke passed for compact opening from the unchanged icon, expansion,
  keyboard/composer visibility, both menu contents, Android Back dismissal and
  reopening with retained history on Android 16. No message or record submitted.
  Layout instrumentation failed; temporary visual fallback was used. Remaining
  accessibility, rotation, voice/review and tool-route gates stay open.
- Next bounded task: execute the remaining phone matrix in
  `design/assistant-context-panel.md`. Keep the separate drink voice acceptance
  gap open.

## MiniLM direct Chat slice — 2026-10-11

- The member explicitly chose MiniLM and requested direct Metria integration
  rather than another external experiment. Custom agents are implementing the
  debug runtime/tokenizer and existing native Chat lifecycle seam separately;
  an independent reviewer is checking cancellation, read boundaries and resources.
- Selected scope: English today-only meal-list questions, local classification
  plus an independent conservative host read guard, existing fresh bounded read,
  deterministic validated reply and encrypted conversation persistence. Unknown
  requests and router failure fall back to Gemma. No new member screen, write,
  cloud route, raw router logs or production dependency is authorized here.
- The initial slice still requires Gemma prepared for Chat; accepted turns skip
  its generation. Reduced resident memory and unloaded-Gemma routing are not
  claimed. Implementation, artifact identity and pending acceptance are tracked
  in [MiniLM Chat integration](minilm-chat-integration.md).
- Final automated gate passed: 449 tests, zero failures/errors/skips, debug APK
  assembled and release Kotlin bridge compiled; `git diff --check` passed.
  The custom implementation/review process corrected cancellation, exception
  fallback and initial-load/close handling. Final review corrections reject
  record-type ambiguity, carry the frozen date through the read, and validate
  before appending evidence; focused re-review found no remaining material
  defect in those paths. Member-approved `adb install -r` returned `Success`
  on Samsung SM-S928B / Android 16; MainActivity launched cold successfully.
  No data clear, uninstall or existing model/vault replacement occurred.
  The member unlocked and submitted the named ordinary Chat lookup, reporting
  that its reply matches their recorded meals. They also confirmed the exact
  MiniLM-route / Gemma-generation-skipped diagnostic. The phone public-model
  hash matched; one process PSS sample was 1,591,249 KB (not peak or a controlled
  comparison). The member then confirmed yesterday's reply matched their records
  with no MiniLM route message. The selected today lookup and other-day fallback
  are physically confirmed; the broader physical acceptance matrix remains open;
  artifact/build identity is in the integration checkpoint.
  The existing opt-in metadata trace was subsequently confirmed enabled. Its
  timestamps record 611 ms from lifecycle turn start to validated reply for the
  MiniLM lookup and 15,131 ms for the native yesterday fallback. Different
  questions/dates and one sample each prohibit a controlled speedup claim;
  these are lifecycle intervals, not isolated inference or UI-render timing.
  Existing Von results do not establish MiniLM routing quality or close gates.

## Routing and model-lifetime investigation — 2026-10-11

- Member requested community investigation before improving rejection or
  connecting Von to real chat. Parent reviewed primary public documentation
  and Von issues #21/#22; an explorer checked the pinned SDK source. The
  community reports confident out-of-domain misrouting and documents domain
  calibration/training. Calibration changes acceptance, not the winning class.
- Confirmed the pilot loads one model per eight-case run, then closes it;
  routing selects operations, not weights. SDK caching is Python-side, while
  Android session reuse must be owned by the app. Keeping Von resident avoids
  repeated setup but requires an actual Gemma-coexistence memory gate.
- Recorded confidence-scale differences, local rejection patterns, quantization
  and memory options, and a recommended selected-function rejection → free-text
  parity → debug shadow-chat → validated read integration sequence in
  [research and next slice](decision-model-routing-research.md).
- Documentation only; no new model training, benchmark, Android change or
  phone operation occurred in this investigation. `git diff --check` passed.
  No community benchmark substitutes for the existing Metria quality gate.

## Selected-function Android Von pilot — 2026-10-11

- Member requested moving directly to an in-app selected-function test and
  explicitly approved in-place installation, public model copy and the eight
  fictional cases. An explorer mapped today's meal-list host seam; a worker
  implemented a debug-only activity and a reviewer checked lifecycle/resources.
- The separate-process pilot tests today's recorded-meal-list routing versus
  fallback using fixed pretokenized fictional inputs. It does not access the
  member vault, answer chat or enable production fast routing. ONNX Runtime is
  debug-only. No general Android tokenizer or dynamic calibration port is claimed.
- Complete Android gate passed after compilation and lifecycle corrections:
  440 tests, zero failures/errors, debug APK assembled. Review fixes prevent
  queued-cancellation deadlock and overlapping sessions after recreation.
  `git diff --check` passed. APK hash and fixture evidence are in the
  [Android pilot checkpoint](decision-model-android-pilot.md).
- On Samsung SM-S928B, Android 16, approved `adb install -r` returned `Success`.
  No data clear, uninstall, signing-key change or existing model replacement
  occurred. Member-record inventory was not inspected. Public model copy and
  phone checksum matched; the temporary transfer file was removed.
- The member tapped Run. The app's synthetic report confirms all eight cases
  passed numerical parity, session load 2,961 ms, per-case inference 305–324 ms,
  PSS 3,136,254 KB sampled with the session open, and end thermal status 0.
  Four eligible routes were correct; three of four fallback cases were wrongly
  accepted. This proves fixed-input Android runtime feasibility, not safe routing.
- The inspectable test-cycle update also passed all 440 tests and assembled;
  its APK hash and measurement limits are in the pilot checkpoint. The report
  is saved only in the dedicated synthetic test directory. Member protections,
  production routing and release dependencies remain unchanged.
- Next bounded task: selected-function negative rejection and independent
  evaluation before general tokenizer/calibration and host-answer integration.
  Phone cancellation/repeat/Gemma-coexistence, privacy and broader M4 gates
  remain open. Do not tune these eight cases and call them held-out evidence.

## Von desktop inference and export — 2026-10-11

- Continued the member-authorized test with custom worker/reviewer agents.
  Downloaded eight public model files at pinned revision
  `498ceba33390b32cfefaab6422ec380318ba9b99`, computed local SHA-256 checksums,
  and used a clean pinned source checkout at
  `9319ced779f8232ab1d8d3e9c69cdc52ddce77ec` in an ignored development-only
  CPU environment. No production Android dependency or source was changed.
- Ran two profiles on 120 fictional development prompts each. At provisional
  winning-probability cutoff 0.8, cautious correctly accepted 3/60 eligible
  routes with zero observed errors; concise correctly accepted 42/60 but also
  accepted 17 incorrect routes (14 unsupported requests). Warm desktop medians
  were 554 ms and 306 ms respectively. Neither configuration meets the combined
  quality/coverage gate. No Gemma or production Kotlin-rules baseline ran.
- Full encoder/scorer ONNX export passed desktop CPU parity on three cases,
  with matching choices and maximum calibrated probability error `8.9407e-7`.
  The unquantized graph is 1,582,047,027 bytes. Eight-option production routing,
  Android runtime support, tokenizer/calibration port, memory and speed remain
  unverified. See [results and provenance](decision-model-desktop-results.md).
- All 22 benchmark/adapter/export unit tests passed using the isolated Python
  with workspace-local temporary files. Independent read-only review found no
  actionable issue. `git diff --check` passed for this checkpoint. Commands
  and artifacts are described in the [runbook](../tools/decision_model/README.md).
- No locked predictions ran; semantic independence review of the reserved
  pool remains open. Library offline settings are not an instrumented privacy
  pass. The phone was not accessed or installed; there is no device/Android
  performance result or data-preservation result from this task. The Android
  test/build gate was not rerun because this task changed no Android files.
- Desktop decision: do not integrate these configurations. Next bounded task
  is revised development routing with explicit negative rejection before
  reserved-set review/freeze and Android tests. Broader M4 gates remain open.

## English-only decision-model harness — 2026-10-11

- Member requested starting the hackathon test using the custom agents. An
  explorer inspected Von, a worker built the standalone harness, and a reviewer
  checked benchmark validity. No production Android source or dependency was
  changed for this task; the phone was not accessed or installed.
- Added 120 fictional development and 120 reserved test prompts under
  `tools/decision_model/`. Eligible routes cover recorded nutrition, day meal
  lists and latest-meal estimates. Negatives include ordinary meal descriptions,
  ambiguity, unsupported dates, mixed intents, medical questions and injection.
- The scoring harness validates imported prediction IDs, full route arguments,
  errors, finite confidence/timing and frozen artifact/fixture metadata. Reports
  preserve fallback/error denominators, accepted errors, coverage, Wilson
  intervals and timing sample counts. Missing model results remain pending.
- Fifteen standard-library harness tests passed using the bundled Python with
  its temporary directory set inside ignored `.local-state/`; default sandbox
  AppData temporary-file writes failed before this workspace-local rerun.
  The development-only exact-phrase reference was exercised, with no model
  invocation or latency data. It is not a measurement of the production Kotlin
  rules. Semantic template-family review is still required before freezing the
  reserved pool; no locked predictions were evaluated.
- Source inspection found Von's custom encoder/masks/scorer path and roughly
  3.16 GB published checkpoint tree. Read-only public revision lookup selected
  source `9319ced779f8232ab1d8d3e9c69cdc52ddce77ec`; actual artifact checksums,
  full-model export and desktop/mobile parity remain unverified. See
  [feasibility findings](decision-model-feasibility.md).
- This task did not run the Android gate because no Android files changed.
  No target-device results, data-preservation result, inference privacy pass,
  latency improvement or M4 acceptance pass is claimed.
- Next bounded task: isolated development environment and pinned artifact
  checksums, actual development-set Von predictions, then complete-model desktop
  export/parity. Production dependency approval and in-place phone installation
  remain separate gates. See [experiment plan](decision-model-test-plan.md).

## Validated meal-list answer APK installed — 2026-10-02

- After explicit member approval and the complete 272-test Android gate,
  installed the host-composed meal-list answer update on the connected Samsung
  SM-S928B using `adb install -r`: `Success`. Force-stop and launch returned
  `Status: ok`, `LaunchState: COLD`. Existing app storage was preserved in
  place; no data clear, uninstall, signing change, or biometric automation
  occurred.
- Installation and launch are confirmed. Physical acceptance of the actual
  meal-list answer remains pending; no successful answer is claimed yet.
- Next bounded task: member unlock and repeat the recorded-meal question,
  expecting a concise saved-meal list for the requested day. Test general food
  advice separately; it must not create a draft. Gate C/D remain open.

## Correct meal read but irrelevant final answer — 2026-10-02

- Authorized phone/trace inspection showed the routing update selecting
  `read_meals` and a grounded meal evidence card. The model's final answer was
  an irrelevant English explanation about unavailable weight/goal information
  and internal tools. It passed the factual audit because it asserted no
  unsupported values. This is an answer-relevance failure, not a successful
  member answer. The temporary screenshot was deleted; no raw member data is
  retained here.
- Explicit exact-day meal-list contracts now use a deterministic host answer
  from that fresh result. Source, requested/as-of dates, calendar binding,
  counts, coverage, row dates/titles, and record bounds are validated before
  the answer passes the unchanged factual audit. Empty and partial lists are
  explicit; same-titled saved records are not silently merged. Invalid data
  fails closed, and native context retires under the existing worker lease.
- Complete Android gate passed: 272 tests, 0 failures/errors, debug APK built;
  `git diff --check` passed. Tests cover bilingual meal names, empty days,
  relative-day binding, invalid/stale/mismatched payloads, and partial records.
  The APK is not installed; physical answer acceptance remains pending.
- Next bounded task: approved in-place installation and repeat the recorded
  meal question. Then separately test general food advice without a draft.
  Gate C/D and broader acceptance remain open.

## Recorded-meal routing APK installed — 2026-10-02

- After explicit member approval and the complete 268-test Android gate,
  installed the recorded-meal question routing fix on the connected Samsung
  SM-S928B using `adb install -r`: `Success`. Force-stop and launch returned
  `Status: ok`, `LaunchState: COLD`. Existing app storage was preserved in
  place; no data clear, uninstall, signing change, or biometric automation
  occurred.
- Installation and launch are confirmed. Physical acceptance of the corrected
  saved-meal question remains pending; no successful answer is claimed yet.
- Next bounded task: member unlock and repeat the exact recorded-meal question,
  expecting the saved meal list rather than nutrition totals. If rejected,
  keep Metria foregrounded for authorized latest-response inspection. Test the
  general rice/protein question separately; it must not create a meal draft.

## Recorded-meal list question routed to nutrition — 2026-10-02

- The member reported rejection after the suggested Spanish recorded-meal
  question. Authorized local screen, transient response, and metadata-trace
  inspection showed `read_nutrition_summary` selected for today, followed by
  `unsupported_personal_fact` rejection. The response gave nutrition totals
  rather than a list of saved meals. Raw response, member data, screenshots,
  device IDs, and hierarchy files are not retained in this checkpoint.
- The host grounding contract recognized “What did I eat?” but missed the
  explicit recorded-meal wording. It now covers that bilingual wording for
  today, yesterday, and supported named dates through the existing exact-day
  `read_meals` correction/binding path. The agent system instruction also
  distinguishes meal lists from nutrition totals. No factual-audit relaxation,
  vault mutation, or model-authored save claim is introduced.
- Regression tests cover the failed wording and equivalent English questions,
  wrong-tool/draft rejection, named-day and unsupported-day boundaries, and
  general/hypothetical food questions remaining outside required record reads.
- Physical acceptance of this routing change remains pending. Next bounded
  task: approved in-place install, repeat the saved
  meal question, and separately verify the general rice/protein question.
  Gate C/D and broader assistant acceptance remain open.
- Complete Android gate passed: 268 tests, 0 failures/errors, debug APK built;
  `git diff --check` passed. This routing APK is not yet installed.

## Saved-meal Chat link after reopening — 2026-10-02

- The member confirmed “yes it works!” after the requested reopen check of
  the saved-meal confirmation and View meal link on the installed navigation
  build. This passes targeted member-observed persistence after reopening.
- The reply did not distinguish navigation reopening, biometric relock, or
  process restart, and did not separately confirm duplicate-record inventory.
  Explicit relock/process-restart, no-replay/no-duplication, deleted-target,
  keyboard, rotation, font scaling, and TalkBack checks remain open.
- Next bounded task: continue the assistant acceptance matrix with a
  non-recording food question and a saved-meal history question, verifying the
  former does not create a draft and the latter reads current saved records.
  Gate D and the varied-photo reliability evidence gap remain open.

## Return-to-Chat and View meal member check — 2026-10-02

- On the installed 265-test navigation/link build, the member reported
  “worked!” in response to the requested save-to-Chat, View meal, and editor
  return check. This is a targeted member-observed pass on Samsung SM-S928B;
  the reply did not separately describe each interaction.
- Deleted-target handling, relock/restart link persistence, keyboard, rotation,
  enlarged text, and TalkBack remain unverified. This does not complete Gate D
  or the broader assistant acceptance matrix.
- Next bounded task: verify saved-meal confirmation/link persistence after
  relock and restart, without replay or duplication, then continue the remaining
  assistant acceptance checks. The varied-photo pilot remains the broader meal
  analysis reliability evidence gap.

## Return-to-Chat and View meal APK installed — 2026-10-02

- After explicit member approval and the complete 265-test Android gate,
  installed the navigation/link update on the connected Samsung SM-S928B with
  `adb install -r`: `Success`. Force-stop and launch returned `Status: ok`,
  `LaunchState: COLD`. Existing app storage was preserved in place; no data
  clear, uninstall, signing change, or biometric automation occurred.
- Installation and launch are confirmed. Physical acceptance of direct Chat
  return after save, View meal, editor return, deleted-target handling, and
  link persistence remains pending. Older confirmations without a saved-meal
  ID are readable but do not gain an inferred link.
- Next bounded task: on the next genuine meal saved from Chat, verify direct
  return, View meal opening the exact saved entry, and closing the editor
  returning to Chat. Avoid saving an unwanted duplicate solely for testing.

## Stay in Chat after save and open saved meal — 2026-10-02

- The member requested returning to Chat after meal save and opening the saved
  meal from the confirmation, as from Today. Chat-origin review now returns to
  Chat on successful save (after synchronizing the persisted transcript), and
  also on leave without saving with the original draft retained.
- New host acknowledgements store an optional local saved-meal ID in the
  encrypted transcript. **View meal** resolves it against the current unlocked
  meal list and opens the same existing saved-meal editor over Chat. Closing,
  editing/saving, or deleting returns to Chat. Deleted targets show a concise
  unavailable label rather than an active action. Existing confirmations with
  no ID remain readable; links are not retroactively inferred from names.
- Link metadata is excluded from model prompts and URLs. Member/evidence
  messages and malformed identifiers cannot retain a receipt link. No meal
  write occurs when opening it, and receipt/save atomicity is unchanged.
- Complete Android gate passed: 265 tests, 0 failures/errors, debug APK built;
  `git diff --check` passed. New tests cover link serialization/retention, older
  payload compatibility, and invalid/member/evidence link removal. No Compose
  instrumentation infrastructure exists; physical navigation and layout
  acceptance remain pending. No installation or physical pass is claimed.
- Next bounded task: approved in-place install, genuine Chat meal save, verify
  direct return and View meal against Today, close/edit/delete return, then
  keyboard, rotation, font scaling, TalkBack, and link persistence after relock
  and restart. Gate D remains open.

## Chat meal-save acknowledgement APK installed — 2026-10-02

- After explicit member approval and the complete 262-test Android gate,
  installed the chat acknowledgement update on the connected Samsung SM-S928B
  with `adb install -r`: `Success`. Force-stop and launch returned `Status: ok`,
  `LaunchState: COLD`. Existing app storage was preserved in place; no data
  clear, uninstall, signing change, or biometric automation occurred.
- Installation and launch are confirmed. Member verification of a new Chat
  meal save and its acknowledgement after reopening, relock, and restart is
  pending. Earlier saves are not retroactively inserted into the transcript.
- Next bounded task: verify the acknowledgement on the next genuine meal save
  from Chat without creating an unwanted duplicate solely for testing. Gate D
  and broader physical acceptance remain open.

## Native draft saved on phone; chat acknowledgement added — 2026-10-02

- On the installed structured-generator build, the member reported a good
  meal draft, visible buttons, and a successful save. This passes that targeted
  member-observed draft/review/save scenario on Samsung SM-S928B. Separate
  Stop, enlarged text, keyboard, rotation, relock, and false-intent checks remain
  open; no broad Gate D completion is claimed.
- The member reported that saving left no trace in Chat. Chat-origin review
  now requests a host-authored acknowledgement with the final confirmed title
  and foods, persisted in the same encrypted vault transaction as the meal and
  its accepted learning. Failed saves roll everything back. Discard/leave and
  other entry paths add no Chat acknowledgement. Expired chats are not revived.
- The acknowledgement uses existing transcript storage and retention; no new
  schema or model-generated save claim is introduced. It is historical chat
  continuity and never replaces a fresh record read.
- Complete Android gate passed: 262 tests, 0 failures/errors, debug APK built;
  `git diff --check` passed. New tests cover final reviewed content, retention
  limits, untitled meals, and absence/expiry of a conversation. The transaction
  rollback path was reviewed; physical save-failure checks remain open.
- Phone acceptance of this new acknowledgement remains pending. Next bounded
  task: approved in-place install, save a reviewed Chat meal, reopen Chat, then
  verify the confirmation survives relock/restart without replay or duplication.

## Structured native draft APK installed — 2026-10-02

- After explicit member approval and the complete 259-test Android gate,
  installed the separate structured-generator and loading-state update on the
  connected Samsung SM-S928B using `adb install -r`: `Success`. Force-stop
  and launch returned `Status: ok`, `LaunchState: COLD`.
- Existing app storage was preserved in place. No data clear, uninstall,
  signing change, vault/model removal, or biometric automation occurred.
  Installation and launch are confirmed; member unlock, record verification,
  meal-draft success, and spinner/Stop physical acceptance remain pending.
- Next bounded task: repeat the natural meal description and verify a visible
  loading state followed by an unsaved Review/Discard proposal. If rejected,
  keep Metria foregrounded and inspect the authorized latest response/category
  before another request. Gate D remains open.

## Native draft prose failure diagnosed — 2026-10-02

- Authorized inspection on Samsung SM-S928B showed the new diagnostics refresh
  working: latest text draft category `invalid_json`, with a prose announcement
  instead of structured meal data. The member also reported that preparation
  looked like a static message. Raw response, member text, and device screen
  are not retained in repository evidence; the temporary screenshot was deleted.
- Native intent detection remains model-selected. Its host action now retires
  chat KV and starts a separate tool-free structured task using only the current
  member message and a dedicated system contract/example. Chat style and read
  evidence are excluded. The existing parser, request ID, timeout, cancellation
  leases, Review/Discard, and member-confirmed save remain in force.
- Draft preparation uses an explicit spinner, status text, and the existing
  Stop response action. Keyboard/insets and composer behavior are unchanged.
- Complete Android gate passed: 259 tests, 0 failures/errors, debug APK built.
  `git diff --check` passed. Phone verification of this new generator/loading
  state remains pending; no successful draft or completed Gate D is claimed.
- Next bounded task: approved in-place installation and member repetition.
  Inspect another failed output if needed; verify review, Stop, keyboard,
  rotation, and accessibility on the phone without saving unwanted duplicates.

## Text draft diagnostics APK installed — 2026-10-02

- After explicit member approval and the complete 259-test Android gate,
  installed the diagnostics update on the connected Samsung SM-S928B with
  `adb install -r`: `Success`. Force-stop and launch returned `Status: ok`,
  `LaunchState: COLD`. No data clear, uninstall, signing change, or biometric
  automation occurred; existing app storage was preserved in place.
- Installation and launch are confirmed. The previous in-memory response
  cleared on process restart. Member unlock, existing-record confirmation,
  reproduction, and inspection of the refreshed response/category remain
  pending. No successful draft or physical diagnostics acceptance is claimed.
- Next bounded task: repeat the failed description, keep Metria foregrounded,
  and inspect Privacy → Developer diagnostics for the authorized run before
  another model operation. Diagnose the actual validation mismatch.

## Text draft rejection inspection — 2026-10-02

- The member's next attempt reached the native meal-draft path but displayed
  the proposal-validation error. This establishes intent routing for that
  attempt, not successful meal drafting. The exact malformed output has not
  been inspected; no schema relaxation is justified yet.
- Developer diagnostics already held the latest bounded model response in
  memory, but the parent screen retained an old model-status snapshot after
  leaving Chat. Navigation now refreshes that status. Text draft results also
  expose a fixed parser category beside the response, including extra tool
  calls and empty/oversized output. Raw response storage remains memory only.
- Complete Android gate passed: 259 tests, 0 failures/errors, debug APK
  assembled. Regression coverage distinguishes valid output, invalid JSON,
  root shape, ranges, extra tools, and response bounds. `git diff --check`
  passed. This diagnostics APK is not yet installed or physically verified.
- Next bounded task: approved in-place install, member reproduction, then
  authorized foreground inspection in Privacy → Developer diagnostics.
  Diagnose the exact mismatch before changing the proposal contract. Gate D
  and broader assistant acceptance remain open. No private response or raw
  device hierarchy is retained in this checkpoint.

## Native meal-draft tool APK installed — 2026-10-02

- After explicit member approval and the complete 258-test Android gate,
  selected the current Samsung SM-S928B transport and installed the working-tree
  native meal-draft build with `adb install -r`. ADB returned `Success`;
  force-stop and launch returned `Status: ok`, `LaunchState: COLD`.
- No data clear, uninstall, signing change, vault/model removal, or biometric
  automation occurred. Installation and launch are confirmed; the member still
  needs to unlock, check existing records, and verify native intent selection.
- Next bounded task: repeat the natural meal description, expect an unsaved
  Review/Discard proposal, and verify its foods before saving any real meal.
  Test saved-meal reads, non-recording food questions, cancellation, and restart
  separately. Gate D and broader B–E acceptance remain open; installation
  alone does not establish native meal-draft behavior.

## Native agent meal-draft intent slice — 2026-10-02

- Authorized current-screen inspection showed the red factual-verification
  error after a natural Spanish description of food eaten. The current route
  sent messages without explicit write words into a native agent exposing only
  read tools. No raw hierarchy or member meal description is retained here.
- The member explicitly preferred exposing a draft tool so the agent can infer
  intent. Added declared native `draft_meal()` rather than a meal keyword
  classifier. The tool has no arguments or save permission; the host uses the
  current message, sends an unsaved-proposal contract back into the same native
  conversation, and validates the reply with the existing meal parser.
- Required record reads cannot switch into drafting. Further tool calls during
  proposal generation are rejected. Current request ID/lifetime checks gate
  presentation; completion clears native KV state. Existing Review/Discard and
  final reviewed Save remain the only meal persistence path. No UI source,
  production dependency, cloud capability, or direct vault-write tool was added.
- Four regression tests cover no-argument native adaptation and rejected
  replacement/save fields, current-member-message/required-read binding,
  malformed/extra-tool proposal rejection, and non-record unsaved receipt.
  Complete Android gate passed: 258 tests, 0 failures/errors, debug APK assembled.
  `git diff --check` passed. Not installed; model intent selection on the phone
  is still unverified. Gate D and the broader B–E gates remain open.
- Next bounded task: after approved in-place installation, member unlock and
  test natural meal descriptions in Spanish/English, a saved-meal read question,
  an advice/hypothetical-food question, Review/Discard, Stop/late response,
  lock/reopen, and no duplicate or unconfirmed saved meal. Record outcomes before
  claiming native draft-tool acceptance. Decimal-display phone acceptance also
  remains pending for a newly generated overview.

## Decimal-display correction installed — 2026-10-02

- After explicit member approval and the complete 254-test Android gate,
  installed the working-tree decimal-display correction on the connected
  Samsung SM-S928B using `adb install -r`. Installation returned `Success`;
  force-stop and launch returned `Status: ok`, `LaunchState: COLD`.
- No data clear, uninstall, signing change, vault/model removal, or biometric
  automation occurred. Installation and launch are confirmed; record
  preservation and new-answer formatting still require member verification.
- Next bounded task: member unlock, check existing records, send a new weekly
  progress question, and compare whole kcal / at-most-one-decimal protein in
  the answer and expandable records card. Previously retained chat is not
  rewritten. No new formatting or full M4 physical acceptance is claimed yet.

## Progress recovery phone check and decimal correction — 2026-10-02

- On the installed Samsung SM-S928B (Android 16 / API 36), the member reported
  that the two questions now worked but nutrition had long decimals. Authorized
  current-screen inspection confirmed a visible Spanish weekly overview answer
  and a yesterday response; it also confirmed floating-point artifacts in the
  host-composed overview. The hierarchy inspection failed, so a temporary
  annotated screenshot was used and removed outside the repository. No member
  values or raw screen evidence are retained here.
- This is targeted progress-reply and follow-up presentation evidence only;
  it does not independently verify yesterday's record completeness, numeric
  accuracy, lifecycle behavior, or accessibility. Gates B–E remain open.
- Corrected overview display at the evidence boundary: whole kcal and one
  decimal for protein, half-up rounded before model/factual-audit use. The
  expandable overview uses matching display precision. Saved meal values,
  underlying arithmetic, weight precision, and host progress calculations
  remain unchanged. This corrects the raw-decimal presentation introduced by
  the new overview fallback, without weakening numeric validation.
- Regression coverage exercises today/week, English/Spanish, fractional
  totals and goals, matching evidence display, unchanged underlying totals,
  accepted rounded prose and rejected raw-value claims. The complete Android
  gate passed with 254 tests, 0 failures/errors, and an assembled debug APK;
  `git diff --check` passed. This decimal-correction APK is not installed.
- Next bounded task: after explicit in-place installation approval, unlock,
  repeat the weekly-progress question, compare answer and expandable evidence,
  and verify concise decimals. Old retained chat text is not rewritten.

## Progress-overview recovery APK installed — 2026-10-02

- After explicit member approval and the complete 253-test Android gate,
  selected the current Samsung SM-S928B transport and installed the debug APK
  with `adb install -r`. ADB returned `Success`; force-stop and launch returned
  `Status: ok`, `LaunchState: COLD`. This is the working-tree build based on
  `778f457` with the progress-overview fallback change.
- The shell exit-code guard had a quoting error after the successful install;
  installation and launch success are independently confirmed by ADB output.
  No data clear, uninstall, signing-key change, vault/model removal, or biometric
  automation occurred. Existing record preservation still needs member check.
- Installation/launch only are accepted. Next bounded task: member unlock,
  check existing records, repeat the English/Spanish weekly-progress question,
  compare the answer with the expandable records card, and exercise the
  yesterday follow-up separately. No new physical reply acceptance is claimed.

## Progress-overview reply recovery — 2026-10-02

- With explicit member authorization to inspect the current Metria screen,
  used the Android CLI skill on the connected Samsung SM-S928B. Weekly-progress
  requests in English and Spanish reached records cards, but the latest answer
  showed the factual-verification error. This was an overview failure before
  the proposed yesterday follow-up. No record values or raw hierarchy/capture
  are retained here; the temporary annotated image was removed outside the repo.
- The screen establishes successful record retrieval and rejected answer
  presentation, not the precise invalid model claim. No raw model response was
  inspected. Existing nutrition-only fallback did not cover an overview.
- Added a bounded bilingual host-composed overview fallback scoped to the
  current native tool result. It validates source, tool/required period, dates,
  count/coverage consistency, numeric values and language, then passes the
  unchanged final factual audit. It includes estimate coverage and the
  recorded-meals-only boundary, never infers zero intake from absent estimates,
  and never supplies an invented weight trend. Rejected model context is
  discarded under the existing lifecycle policy.
- Five regression tests cover bilingual recovery from rejected prose, native
  today selection, empty records, wrong source/tool/period/dates/language,
  malformed counts and nonfinite nutrition. The complete Android test/build
  gate passed with 253 tests, 0 failures and 0 errors, and assembled the debug
  APK. `git diff --check` passed.
- Not installed; new physical acceptance is pending. Prior device evidence
  does not validate this APK. Next bounded task: after explicit in-place
  installation approval, have the member unlock and repeat a weekly-progress
  question in both languages, verify the answer against the records card, and
  exercise Stop/reopen and the yesterday follow-up separately. Gates B–E remain
  open; this fallback does not establish model-language reliability.

## Hackathon and optional-cloud design — 2026-10-02

- The member wants a stronger assistant experience and a Nebius hackathon
  presentation, and explicitly selected designing optional real-data cloud
  assistance with consent and revised privacy disclosure.
- `hackathon-assistant-plan.md` records verified event requirements and a
  bounded local-demo schedule. `optional-cloud-assistant-spec.md` proposes a
  default-off, app-initiated relay for a previewed question and selected
  seven-day summaries, with read-only validated answers. WhatsApp is deferred.
- Updated plan, architecture, safety, and design references distinguish this
  proposed cloud flow from the active local contract and future external MCP.
  Provider retention, service identity, shipping consent, and implementation
  acceptance remain open. No real data was transmitted.
- Documentation only; `git diff --check` passed. No Android source, build,
  installation, device capture, or new physical acceptance in this turn.
  Prior 248-test/device evidence remains unchanged. Gates B–E remain open.
- Next bounded task: run the consented varied-photo pilot and four local
  assistant journeys in the hackathon plan; verify Nebius provider retention
  and broker authentication before implementing the optional online adapter.

## Gate A native agent and conversation — 2026-08-28

**Outcome:** Gate A implementation and target-device acceptance passed. Gates B–E remain open.

### Build and device

- Repository state tested: assistant-agent working tree based on `1aa8ebd` before its checkpoint commit.
- Build gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed.
- Device: Samsung SM-S928B, Android 16 / API 36, three-button navigation.
- Installation: debug APK installed in place with `adb install -r`; encrypted vault, downloaded model, conversation, meals, and weight history were preserved.
- Runtime: LiteRT-LM 0.13.0 with the selected Gemma 4 E2B model.

### Accepted behavior

- The real native LiteRT conversation can select all five read-only Gate A tools: progress overview, nutrition summary, weight history, meal history, and saved-dish search.
- Required native arguments are validated before dispatch. Tool responses are bounded and expose counts, omitted counts, coverage, and deterministic summaries instead of unbounded vault rows.
- English and Spanish typed questions produced grounded local answers during the physical test cycle. Broad progress, dated meal/weight, nutrition, and remembered-dish questions exercised the native tool path.
- The visible conversation is encrypted in the vault, shared by every chat entry point, retained for at most 24 hours and 50 visible messages, and is not automatically resubmitted when the member returns to chat.
- The full-screen conversation uses the activity window rather than a Compose `Dialog`. On the reference phone, the composer remained above the 135 px three-button navigation bar with the keyboard closed and moved safely above the IME when open. The measured keyboard-open composer bottom was 1304 px; the navigation boundary began at 2205 px.
- The member physically confirmed the corrected layout. Android Back and the header close action return through the same conversation-close path.
- Weight actions remain reversible with visible Edit/Undo. Meal actions remain drafts requiring review.

### Automated coverage

- Native tool declarations, mandatory argument validation, output bounds, unsupported calls, and factual-number/direction checks have unit coverage.
- Encrypted conversation retention, expiry, bounded reconstruction, and process-state serialization have unit coverage.
- Presentation coverage asserts the full-screen chat contract and member-facing assistant states.

### Open acceptance and safety work

1. Physically accept the typed factual-output auditor on the target phone. Automated coverage now prevents a valid date, weight, count, meal name, total, or goal from being rebound to a different field, period, collection, or record, but live bilingual model phrasing and the single corrective turn still require device evidence.
2. Complete Gate B on the physical phone: pronouns, yes/no follow-ups, period changes, cancellation, biometric lock/unlock, process death/restart, token-budget reconstruction, airplane mode, and no replay of completed turns.
3. Complete the remaining UI quality matrix: landscape, gesture navigation, enlarged system font, TalkBack order/labels, rotation, and keyboard-visible send/voice actions.
4. Complete Gate C guardrail, prompt-injection, AI disclosure, safety-boundary, and reporting tests.
5. Move the transitional weight and meal write router to Gate D native idempotent tools before claiming the assistant action system is complete.
6. Complete Gate E bilingual fixtures and physical latency, memory, thermal, battery, and failure evidence.

### Next bounded implementation task

After the typed-auditor physical check, complete Gate B continuity acceptance: pronouns, yes/no follow-ups, period changes, cancellation, biometric lock/unlock, process death/restart, token-budget reconstruction, airplane mode, and no replay of completed turns.

## Typed factual integrity implementation — 2026-09-01

**Status:** automated implementation passed; physical acceptance remains open.

- Native nutrition tool results now expose period, recorded-meal count, actual calories/protein, and their period-specific goals as structured fields rather than one untyped facts string.
- Weight-history results expose a structured first/latest transition in addition to bounded dated entries, so omitted older rows cannot detach the summary trend from its evidence.
- The immutable per-turn evidence object preserves typed date↔weight, date↔meal, metric↔actual/goal↔period, collection↔count, and saved-dish-name relationships. Visible conversation history remains excluded.
- The final-output auditor still applies the coarse latest-response numeric gate, then validates typed relationships clause by clause. Unlabelled typed numbers, swapped records, swapped metrics, wrong goal periods, wrong collection counts, and non-returned saved-dish identities fail closed.
- The existing single corrective turn now asks for one complete factual statement per sentence. A second unsupported answer is still discarded without persisting model prose or tool payloads in diagnostics.
- Automated examples cover correct and swapped bilingual record bindings, metric and goal swaps, collection-count swaps, saved-dish identity, unsupported claims without current evidence, locale number formatting, and weight-direction contradictions.

**Automated evidence:** `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed on 2026-09-01 with 104 unit tests. No APK was installed and no physical-device claim is made by this checkpoint update.

## Factual progress and chat disclosure correction — 2026-09-02

**Status:** implementation and automated gate passed; physical acceptance remains open.

- Nutrition tools now return host-calculated calorie-goal and protein-goal percentages.
  The model may phrase those approved values but does not perform personal-data arithmetic;
  the auditor binds each percentage to its metric and selected period.
- Native tool responses include the requested reply language, and the system instruction
  explicitly prefers that language, member-owned wording such as “your records”, and no
  unrelated closing question.
- Weight evidence preserves recorded precision up to two decimal places instead of silently
  rounding the evidence card while exposing a different value to the model.
- Exact local facts remain available in the timeline but their **From your records** card is
  collapsed by default and exposes an accessible expanded/collapsed state.
- Encrypted conversation persistence now executes on the IO dispatcher from the keyed
  conversation effect rather than serializing and encrypting the vault on the Compose UI thread.

**Automated evidence:** `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed on
2026-09-02 with 108 unit tests. The debug APK was assembled successfully. No APK was installed,
and nutrition phrasing, evidence expansion, and send responsiveness still require the target-phone check.

### Target-phone follow-up failure and correction

- The first physical nutrition-progress run was rejected even though the host percentages were
  correct. Developer diagnostics showed that the persistent native LiteRT conversation repeated an
  older weight fact absent from the latest nutrition response; the factual auditor rejected the
  combined answer as designed.
- Record-backed top-level turns now reset native LiteRT conversation state before tool selection.
  The encrypted visible conversation remains intact, and the host grounding policy still uses it to
  resolve follow-up intent before the reset.
- Native start and tool-continuation calls now cross `Dispatchers.Default`. Conversation persistence
  is debounced, remains on `Dispatchers.IO`, and no longer replaces the full unlocked shell state
  after each save.
- Automated verification: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  after this correction with 108 tests, 0 failures, and 0 errors.
- Physical Android verification: pending; repeat the weekly nutrition question and observe Send
  responsiveness before closing this checkpoint.

### Second target-phone failure and runtime correction

- A second physical run produced the evidence card but no final answer. The native tool result raced
  the worker `Future` completion: the lifecycle had already published the expected tool name and
  loaded state, but rejected the immediate continuation because the just-finished future had not yet
  reported `isDone`. The pending tool name and conversation identity are the authoritative hand-off;
  the redundant future-state check was removed.
- Android frame evidence recorded a 4.45-second frame while chat and Compose shared the GPU. Moving
  Kotlin calls off-main cannot prevent GPU display starvation. The default runtime is now hybrid:
  CPU text, GPU vision, and CPU audio. Meal-image acceleration is preserved while chat no longer
  competes with Android rendering for the GPU.
- Automated verification passed after this correction with 108 tests, 0 failures,
  and 0 errors; the debug APK assembled successfully.
- The corrected APK was installed in place on the Samsung SM-S928B with existing
  vault, model, meals, weights, and conversation preserved.
- The member confirmed that the weekly nutrition response was relevant and
  factually good. This physically accepts the tool-result continuation and stale
  factual-state corrections for that scenario.
- A remaining responsiveness gap occurs between tapping Send and the first
  visible “Metria is thinking” state. Once that state appears, the corrected
  response completes. Do not close responsiveness acceptance until the UI shows
  an immediate optimistic pending state and the launch interval is rechecked on
  the phone.

### Next bounded implementation task

Publish a local pending-submission state immediately on Send, yield a frame, and
construct conversation context/start LiteRT off the UI thread. Then repeat the
weekly nutrition question and measure Send-to-thinking responsiveness using the
procedure in `docs/TESTING.md`.

## Optimistic Send responsiveness — 2026-09-21

**Status:** implementation, automated gate, and target-phone responsiveness
acceptance passed.

- A typed Send now publishes the member bubble and a cancellable local pending state
  before preparing conversation context or entering LiteRT. The pending token rejects
  duplicate sends immediately and remains visible until the matching lifecycle request
  state is observed.
- The submission coroutine waits for a Compose frame, then builds the bounded prior
  conversation context/history and starts the native or transitional request on
  `Dispatchers.Default`. The newly submitted member message is sent once as the active
  turn and is not duplicated in reconstructed history.
- Cancellation invalidates the pending token and cancels any not-yet-started submission;
  lifecycle rejection, startup failure, and the matching terminal lifecycle state clear
  the optimistic state without weakening request-ID result filtering.
- Native tool continuation also owns a local pending hand-off until its new lifecycle
  request ID is visible. A typed submission cannot bind to an already-running tool-result
  request during the lifecycle polling interval.
- Focused unit coverage verifies immediate pending presentation, duplicate-send
  prevention, cancellation cleanup, rejected-start cleanup, and optimistic-to-lifecycle
  reconciliation.

**Automated evidence:** `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
passed on 2026-09-21 with 116 tests, 0 failures, and 0 errors. The debug APK assembled
successfully. Automated results alone were not used as physical-device evidence.

### Physical Android verification — 2026-09-21

- Build/commit: uncommitted working tree based on `73a1143`
- Automated gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed;
  116 tests, 0 failures, 0 errors
- Device: Samsung SM-S928B, Android 16 / API 36
- Runtime: LiteRT-LM 0.13.0 with Gemma 4 E2B; text CPU, vision GPU, audio CPU
- Installation: `adb install -r` succeeded and the activity launched cold
- Member-reported acceptance after biometric unlock: the member bubble and cancellable
  thinking state appeared immediately after Send; duplicate Send was prevented; cancellation
  returned the chat to a usable state; and the weekly nutrition response remained relevant
  and factually grounded.
- Existing data preserved: member verified the encrypted vault, downloaded model, meals,
  weights, saved dishes, and conversation remained intact after the in-place installation.
- Result: the optimistic Send responsiveness checkpoint passed on the target phone.
- Evidence boundary: this is member-reported physical acceptance; no private screenshots or
  UI hierarchy dumps were captured.

## Gate B short nutrition follow-up correction — 2026-09-22

**Status:** implementation and automated gate passed; corrected APK installed and
target-phone interaction recheck pending.

- Physical Gate B testing exposed a bounded failure after a successful Spanish weekly-progress
  answer: a short Spanish protein follow-up executed a local read and displayed its collapsed
  evidence card, but the generated answer was rejected by the factual-output auditor. The chat
  remained responsive and no record was changed.
- The host grounding policy now recognizes short English and Spanish calorie/protein follow-ups,
  inherits `today` or `week` from the most recent explicit member period, and requires a fresh
  `read_nutrition_summary` call for that same period. Tool-name and period matching are both
  enforced; an older result or a tool call for the wrong period cannot satisfy the request.
- Nutrition fact auditing now determines actual-versus-goal roles from phrases directly attached
  to each value instead of treating any nearby goal word as applying to every value in the clause.
  Natural actual-versus-goal comparisons are accepted while swapped values remain rejected.
- Focused coverage includes the observed Spanish follow-up, the English equivalent, inherited
  week/today periods, wrong-period rejection, same-clause Spanish and English comparisons, and
  swapped-value rejection.

**Automated evidence:** `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed on
2026-09-22 with 117 tests, 0 failures, and 0 errors. The debug APK assembled successfully.

### Physical Android recheck — 2026-09-22

- Build: uncommitted Gate B correction based on `0815fd3`
- Device: Samsung SM-S928B, Android 16 / API 36
- Installation: `adb install -r` succeeded and the activity launched cold
- Existing data preserved: pending member verification after biometric unlock
- Interaction acceptance: pending; installation and launch do not establish that the corrected
  weekly-progress → protein follow-up passes on the physical model runtime
- Next bounded task: repeat the same Spanish weekly-progress → protein follow-up and confirm
  the second response is grounded in weekly evidence without a verification error

### Recheck finding and follow-up correction — 2026-09-22

- The member reported that the verification error persisted. A later on-device check showed a
  successful protein answer after a voice-labelled turn, followed by a typed `y de proteína?`
  that received a generic ungrounded reply. The visible typed bubble confirmed Send was used for
  the latter turn; the earlier voice label alone did not describe the typed turn.
- Root cause for the generic typed reply: period inheritance stopped at the persisted
  `Voice: ...` transcript and treated the recording placeholder as another member question,
  so the host did not require a fresh weekly nutrition read. The history policy now unwraps
  the transcript and skips only that placeholder while retaining the existing three-turn bound
  and unrelated-question stop.
- The live model can still phrase an otherwise valid nutrition result in a way the factual
  auditor rejects. For a short calorie/protein follow-up only, an audited deterministic reply
  may now use the same fresh, matching, vault-backed nutrition result after model wording fails.
  Missing estimates are stated as unavailable; the fallback cannot draw facts from chat history,
  accept a mismatched period, or retain the rejected native conversation.
- Automated evidence: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed with
  119 tests, 0 failures, 0 errors; debug APK assembled. Physical acceptance of this new build
  remains pending.
- Installation: the corrected debug APK was installed in place with `adb install -r` on the
  Samsung SM-S928B and cold-launched successfully. No app data was cleared.
- Member-reported physical acceptance after biometric unlock: existing records remained present;
  the typed weekly-nutrition question followed by a short protein follow-up
  sequence produced a weekly record-backed second answer without the red verification error.
  This is physical member evidence, separate from the 119 passing automated tests.
- Remaining Gate B work: pronoun and yes/no continuity, period changes, cancellation,
  biometric lock/unlock, process restart, token-budget reconstruction, airplane mode, and no
  replay of completed turns still require their own physical acceptance.
- Next bounded task: exercise the remaining Gate B continuity matrix on the target phone.

### Gate B explicit protein-goal question — 2026-09-22

- The member reported that the first weekly protein-goal turn of the next physical sequence
  failed. A member-supplied screen
  capture confirmed the red latest-record verification error after two variants
  of the weekly protein-goal question, with no new evidence card visible for
  the failed attempt. The sequence stopped there; pronoun, period-change, and
  later turns were not accepted. No personal values or raw capture are stored
  in the checkpoint.
- A second member-supplied capture of Developer diagnostics showed only runtime
  and timing metadata, with no latest in-memory response visible. The exact
  native output/tool decision therefore remains unconfirmed. The member raised
  an app-private diagnostic-file idea; no file capture is implemented or enabled
  without an explicit privacy-contract decision.
- Code inspection found that this exact wording was outside the host's required-read
  patterns. An explicit goal-comparison question now requires a fresh
  `read_nutrition_summary` for its stated week/today period and protein/calorie
  metric. Wrong-tool and wrong-period calls remain rejected; unrelated goal
  questions are not reclassified. This corrects a verified policy gap but does
  not, by itself, establish the cause of the physical failure.
- Automated gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 120 tests, 0 failures, 0 errors; the debug APK assembled. `git diff --check`
  passed. The new build has not been installed or physically rechecked.
- Next bounded task: confirm the observed failure mode on the current phone,
  then in-place install and recheck the exact question before resuming Gate B.

### Opt-in debug assistant trace — 2026-09-22

- The member approved a debug-only, app-private diagnostic file to help identify
  the exact failure stage when the in-memory response has already disappeared.
  Developer diagnostics now offers Enable, Disable, and Clear trace. Capture is
  off by default, stores only allowlisted event/tool/period/outcome metadata,
  and excludes prompts, model prose, record values, tool payloads, and raw
  exceptions. It is not encrypted; it is isolated in app-private no-backup
  storage and excluded from the vault and recovery archive.
- The trace is limited to 100 events / 64 KiB and a rolling 24-hour window,
  pruned at startup, on new writes, and hourly while the app is open. Trace
  status is an in-memory snapshot and file operations run off the UI thread.
  The debug control is absent and the store is inert in release builds.
- The local ADB read command and clear/disable checks are in `docs/TESTING.md`.
  No phone trace, member record, or screenshot was added to the repository.
- Automated gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
  passed with 126 tests, 0 failures, 0 errors; the debug APK assembled.
  `git diff --check` passed. No APK was installed for this change, and no
  physical acceptance is claimed.
- Next bounded task: with member approval, install this debug APK in place,
  enable the trace, repeat the exact weekly protein-goal question after
  biometric unlock, then inspect only the allowlisted trace through `run-as`
  and clear or disable it when finished. Verify existing data remains intact.

### Opt-in trace installation — 2026-09-22

- Build: uncommitted working tree based on `0815fd3`; complete Android gate
  passed with 126 tests, 0 failures, 0 errors, and debug APK assembly.
- Device: Samsung SM-S928B, Android 16 / API 36 (previously verified target).
- With explicit member approval, `adb install -r` succeeded and Metria
  cold-launched successfully. No app data was cleared or uninstalled.
- Biometric unlock, existing-data preservation, diagnostics UI, trace-file
  access, and the exact weekly protein-goal interaction remain pending member
  and device verification. Installation alone is not physical acceptance.
- Next bounded task: member unlocks, checks existing records, enables the
  trace in Developer diagnostics, repeats the exact question, and leaves the
  app open for a metadata-only `run-as` read; then clear or disable the trace.

### First opt-in trace read — 2026-09-23

- The member reported receiving an answer after the in-place install. A
  metadata-only `run-as` read showed one started turn, a weekly
  `read_progress_overview` selection and continuation, and an accepted final
  reply. No prompt, model prose, or record value was read or saved.
- This demonstrates a completed record-backed weekly overview turn, but the
  exact prompt and answer accuracy are not yet confirmed. The explicit
  protein-goal wording is expected to select `read_nutrition_summary`, so this
  trace cannot yet be counted as physical acceptance of that exact case.
- Existing-data preservation and diagnostics clear/disable behavior also
  remain to be confirmed by the member on the device.
- Next bounded task: confirm the exact sent wording and whether the answer
  matched visible records, then repeat the explicit protein-goal question
  with the trace enabled if these events came from a different question.

### Explicit protein-goal retest — 2026-09-23

- The member clarified that the first accepted trace came from a broad weekly
  nutrition-goals question; `read_progress_overview(week)` was appropriate.
- The member then sent the explicit weekly protein-goal question and reported
  that it produced an answer without the previous red verification error.
  The metadata-only trace confirmed a fresh `read_nutrition_summary(week)`
  selection and continuation followed by an accepted final reply.
- The member-shared answer described the period as the previous week although
  the request and tool were for the current rolling seven days. The member
  subsequently confirmed that no meals were recorded in the last seven days;
  the missing-meals claim was true, but the period wording was still wrong.
  This is not full factual acceptance despite removal of the red error. No raw
  answer or personal values are stored in this checkpoint.
- Code inspection indicates the final factual gate checks period alongside
  numeric nutrition claims, but does not reject a wrong relative-period label
  in otherwise number-free prose. A targeted period-label check and focused
  tests are the next bounded implementation task, followed by a new build and
  member-approved in-place physical recheck.

### Rolling-period wording and response-style first slice — 2026-09-23

- A weekly nutrition answer now fails the factual audit if it calls the rolling
  seven days "last week" or "la semana pasada", including when the sentence
  contains no numeric claim. Focused tests cover the rejected and accepted
  Spanish wording.
- The approved Balanced, Coach, and Records assistant styles are available as
  a tone-only choice in Privacy & settings. The preference defaults to Balanced
  for older vaults, is stored in the encrypted vault, and rebuilds only native
  conversation context at the next safe boundary. The visible encrypted chat,
  tool requirements, factual audit, and privacy/safety rules remain unchanged.
  A failed preference save keeps the selector open with an error.
- The broader idea of using older recorded history is documented separately;
  this slice does not add a new history read or permit claims beyond fresh,
  bounded tool evidence.
- Automated gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
  passed after the final adjustment with 128 tests, 0 failures, 0 errors, and
  debug APK assembly. `git diff --check` passed. No APK from this slice has been installed; period
  wording and style behavior have not been physically accepted.
- Next bounded task: obtain explicit approval for an in-place install, then
  check the exact weekly question and all three styles
  on the target phone after member biometric unlock. Verify existing records
  and model remain intact; do not infer physical acceptance from unit tests.

### Rolling-period and style build installed — 2026-09-23

- With explicit member approval, the debug APK from the 128-test gate was
  installed in place using `adb install -r` on Samsung SM-S928B, Android 16 /
  API 36. A cold launch of `app.metria/.MainActivity` completed successfully.
  No app data was cleared, no uninstall or signing-key change was performed.
- Biometric unlock, existing-record/model preservation, the corrected rolling
  period wording, and all three style choices still require member verification
  on the phone. Installation and launch alone are not physical acceptance.
- Next bounded task: member unlocks, confirms existing data and downloaded
  model remain available, repeats the weekly nutrition/protein question, then
  samples Balanced, Coach, and Records assistant for phrasing while checking
  that every answer stays tied to the same fresh local evidence.

### Member-confirmed weekly protein wording — 2026-09-23

- After the in-place install, the member reported a successful weekly protein
  answer. It correctly described the period as the last seven days, said there
  was no recorded protein estimate for that period, and presented the goal as
  a reference. The member had already confirmed that no meals were recorded
  in that period. The earlier wrong "previous week" wording and red
  verification error were not reported on this retest.
- This is physical acceptance of the targeted rolling-period protein response,
  not acceptance of every nutrition query or response style. The member has
  not yet explicitly confirmed preservation of all existing records/model or
  exercised Coach and Records assistant on the phone. No raw answer or personal
  goal value is stored in this checkpoint.
- Next bounded task: confirm existing data and downloaded model remain intact,
  then sample the three styles for phrasing and evidence consistency. Broader
  historical context remains a separately specified capability decision.

### Post-install preservation confirmation — 2026-09-23

- The member confirmed that existing records and the downloaded model are still
  present after the in-place install. This completes the preservation check for
  this build; no records or model files were inspected or copied by the agent.
- The remaining physical check for this slice is to switch among Balanced,
  Coach, and Records assistant and verify that the same weekly protein question
  stays grounded in the same fresh seven-day evidence while only phrasing changes.

### Style check and next history slice — 2026-09-23

- The member reported no red verification error after moving through the style
  check. Exact answer wording, factual equivalence, and tone differences were
  not supplied, so full physical style acceptance remains open.
- The next bounded implementation slice targets explicit older-day meal and
  weight questions within the existing 0–90-day read-tool window. The host
  must require a fresh exact-day read and reject wrong-tool or wrong-day calls;
  the answer audit must reject a different claimed date. This does not add
  all-time access or historical nutrition totals, and cannot claim that Metria
  knows all older records yet.

### Exact historical-day read slice — 2026-09-23

- Explicit Spanish/English meal-list and weight questions about one relative
  day within 0–90 days now require the matching native read tool with that
  exact offset. Wrong tools, offsets, and meal-slot substitutions are rejected;
  the model gets at most one corrective tool retry. Host responses remain
  bounded to 24 records and now carry the host's `as_of` date so the factual
  audit can reject wrong ISO and relative-day claims.
- Explicit questions beyond 90 days and unsupported historical nutrition
  totals receive a deterministic, localized no-read clarification before
  native inference. Today nutrition remains supported. This slice neither
  exposes the full vault nor enables all-time or older-period nutrition claims.
- Independent review found and prompted fixes for out-of-range/wrong-tool
  substitution, historical-nutrition reinterpretation, and wrong relative-day
  prose. A follow-up review confirmed the fixes and found a today-nutrition
  regression, which was then corrected and tested.
- Complete Android gate after the final fix:
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon` passed with 133 tests,
  0 failures, 0 errors, and debug APK assembly. `git diff --check` passed.
  This APK has not been installed; no historical-day phone acceptance is claimed.
- Next bounded task: obtain approval for `adb install -r`, member biometric
  unlock, verify existing data/model preservation, then ask one known older-day
  meal and weight question and one unsupported historical nutrition question.

### Exact historical-day build installed — 2026-09-23

- With explicit member approval, the debug APK from the 133-test gate was
  installed in place on Samsung SM-S928B, Android 16 / API 36, using
  `adb install -r`. Metria cold-launched successfully. No app data was cleared,
  no uninstall or signing-key change was performed.
- Biometric unlock, existing-record/model preservation, and the new day-specific
  meal/weight and unsupported-history responses are not yet physically verified.
  Installation and cold launch alone do not establish acceptance.
- Next bounded task: member unlocks, checks existing records/model, and asks
  one exact older-day meal question, one exact older-day weight question, and
  one unsupported historical nutrition question. Compare answers with visible
  records and report any red verification error or wrong day.

### Member historical-day screen check — 2026-09-23

- The member reported no red error and that the three responses looked good.
  With explicit permission to inspect the current phone screen, the agent read
  the compact UI hierarchy and one temporary annotated capture outside the
  repository, then deleted the capture. No raw screen or member value is saved
  here.
- The older-day meal and weight turns each displayed a collapsed local-record
  evidence card and a response about the requested day. The unsupported
  historical nutrition turn displayed the intended deterministic limit message
  without a record-evidence card. This confirms that the third turn did not
  present a made-up older-day nutrition total. The member's actual records for
  that specific day were not independently checked by the agent.
- Copy issue observed: the meal answer described the member's records in the
  first person ("my records") despite the assistant prompt requiring "your
  records". The first two answers also appended generic follow-up questions.
  These are phrasing defects, not evidence of a wrong-day or red verification
  failure. Broader historical nutrition remains outside this slice.
- The member has not yet explicitly reconfirmed existing-record/model
  preservation after this particular install. Next bounded task: confirm that,
  then address the phrasing and plan bounded historical nutrition if desired.

### Bounded exact-day nutrition and answer wording — 2026-09-23

- Implemented a separate read-only `read_day_nutrition(days_ago)` for an explicit
  day 0–90 days ago. The host calculates recorded-meal estimates from saved
  meals for that day only, combining complete and partial estimates while
  retaining separate subtotals and complete/partial/missing coverage. An absent
  estimate remains absent, not zero. The response binds its exact date, offset,
  requested metric, as-of date, and recorded-meals-only interpretation; it has
  no historical goal. Multi-day and all-time nutrition totals remain out of
  scope, and unsupported/out-of-range written offsets get a no-read limit reply.
- Exact-day questions in English and Spanish, including written offsets through
  90 days, require a matching native read. A wrong offset, date, period, or
  metric cannot be used as the answer's latest record evidence. A corrective
  retry now names the required offset. Unparsed written dates beyond the window
  and questions comparing two distinct days clarify rather than selecting an
  arbitrary single day. Today and rolling-seven-day reads retain their
  existing contracts.
- Tightened answer-style guidance to end after the requested fact and coverage
  limit unless a relevant Coach next step adds value. After factual auditing,
  a narrow presentation-only correction changes “my records” / “mis registros”
  to member-owned wording; it does not alter numbers or grant evidence.
- Automated gate from `native-android`: `./gradlew.bat :app:testDebugUnitTest
  :app:assembleDebug --no-daemon` passed, 145 tests, 0 failures/errors; debug
  APK assembled. `git diff --check` passed. Independent review found and the
  implementation closed written-offset, multi-date, and corrective-retry gaps.
- Physical status: this new APK has **not** been installed or checked on the
  Samsung SM-S928B. Existing records/model preservation, exact-day nutrition
  wording, coverage presentation, and absence of red verification errors on
  the new build remain unverified. Install only after renewed member approval,
  with `adb install -r` and member-performed biometric unlock.

### Exact-day nutrition APK installed — 2026-09-23

- With explicit member approval, installed the gate-passing debug APK in place
  on Samsung SM-S928B (Android 16, API 36) using `adb install -r`; ADB reported
  `Success`. Force-stop followed by `am start -W` reported a successful cold
  launch. No app-data clear, uninstall, downgrade, signing-key change, vault
  access, or model-file access was performed.
- Biometric unlock and verification of retained records/model are member-only
  and still pending. No exact-day nutrition response on this APK has yet been
  physically accepted. Next check: member unlocks, confirms existing data and
  model remain available, then asks an exact older-day calorie or protein
  question and checks the answer, coverage, evidence card, and red-error state.

### Calendar-date nutrition phone failure — 2026-09-23

- On the installed uncommitted build based on `0815fd3`, the member asked a
  Spanish calorie question naming a calendar day with a saved meal. They saw a
  red message that disappeared after about two seconds. Whether an answer then
  appeared and what that message said are not yet confirmed; this is a failed,
  not accepted, physical scenario. Existing records/model preservation after
  this install is also not yet explicitly confirmed.
- The opt-in metadata-only debug trace for the latest turn shows
  `turn_started` followed by `reply_rejected` with
  `unsupported_personal_fact`, with no approved read tool in between. No raw
  prompt, response, tool payload, or record value was copied to this document.
  Code inspection confirms that the exact-day grounding parser accepts
  relative offsets but not an explicit calendar date. Thus the model was not
  required to read that saved day, and its personal claim was correctly blocked.
- Next bounded task: add a strictly parsed, local-zone calendar-date form for
  unambiguous supported days, convert it to the same 0–90-day exact-day read,
  and fail closed on ambiguous/out-of-range dates. Test Spanish and English
  forms, wrong-day tool calls, empty/partial coverage, and the transient error
  lifecycle. Run the complete Android gate before requesting a new in-place
  install and physical recheck.

### Calendar-date read and persistent error build — 2026-09-24

- The uncommitted build now parses unambiguous named Spanish/English and ISO
  calendar days, binds a supported single day to the matching 0–90-day local
  read, rejects ambiguous or out-of-range forms, and audits answer dates
  against the exact response. The generic verification error is latched for the
  current turn and saved across rotation without saving conversation text in
  Android instance state.
- Review of the first build found two regressions in older evidence without an
  `as_of` field: swapped meal/date and weight/date facts were accepted. The
  auditor now derives a date reference from returned records when necessary.
  The existing date-binding tests pass again. Independent review then found
  that such a record date must not define relative words like “today”; those
  claims now fail closed without an explicit `as_of` or a bounded response
  range. Regression cases cover both incomplete response shapes.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 154 tests, 0 failures, 0 errors; debug APK assembled. `git diff --check`
  passed. This is automated evidence only, not phone acceptance.
- Target phone remains Samsung SM-S928B, Android 16 / API 36. At this build
  checkpoint the APK had **not** been installed, and vault, model, and member
  records had not been rechecked. The next step was an approved in-place install
  followed by member biometric unlock and the named-date calorie recheck.

### Calendar-date build installed in place — 2026-09-24

- After explicit member approval, the 154-test debug APK was installed on the
  connected Samsung SM-S928B (Android 16) with `adb install -r`; ADB reported
  `Success`. A force-stop and `am start -W` reported a successful cold launch.
  No data clear, uninstall, downgrade, signing-key change, or vault/model-file
  access was performed.
- Biometric unlock, retained-record/model confirmation, the named-date calorie
  response, and error persistence have **not** been physically accepted yet.
  The next check belongs to the member after unlocking: confirm existing data
  and model, ask the named-date calorie question for a day with saved meals,
  compare the answer and coverage with visible records, and report any red
  verification error or disappearance without an answer.

### Exact-day answer screen inspection — 2026-09-24

- With the member's explicit permission, a compact UI layout and one temporary
  annotated screenshot of the current Samsung SM-S928B screen were inspected.
  The screenshot was deleted after inspection; no raw screen, meal value, or
  model prose was added to the repository.
- The visible chat showed two member attempts and one dated, record-backed
  calorie reply. The local evidence indicated one recorded meal with a partial
  estimate. No red verification error was visible at inspection time. This is
  screen observation, not an independent check against the saved meal or a
  member-confirmed physical acceptance result.
- Presentation failure: expanding “From your records” exposed internal field
  labels, `null` values, and an unrounded floating-point artifact; the answer
  repeated the artifact and implied partial estimates were shown separately
  when the answer did not do so. Next bounded task: make the exact-day evidence
  member-facing and quantize the tool's exposed estimate consistently with the
  factual auditor, then run the Android gate before requesting another install.

### Exact-day nutrition presentation build — 2026-09-24

- The exact-day read now exposes deterministically rounded estimates (whole
  kcal and protein to one decimal) to the local model and the same factual
  auditor. Saved meal values and underlying deterministic summaries are not
  rewritten; absent estimates remain absent. The evidence card now uses
  Spanish or English member-language date, recorded-meal coverage, the
  requested estimate when available, and a recorded-meals-only limit instead
  of internal field names. The host fallback no longer claims partial estimates
  are separately shown; it mentions partial coverage only when present and
  explicitly qualifies available totals when any recorded meal has no nutrition
  estimate.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 156 tests, 0 failures, 0 errors; debug APK assembled. New tests cover
  floating-point-artifact rounding, null and bilingual evidence, audited
  fallback date/metric binding, and partial/missing coverage combinations.
  This is automated evidence, not phone acceptance.
- At this build checkpoint the APK had **not** been installed. The existing encrypted conversation
  may still contain its older technical evidence card until it expires or the
  member starts a new conversation; the implementation does not rewrite saved
  history. Next bounded task: after new approval, install with `adb install -r`,
  member unlocks and repeats a supported exact-day nutrition question, then
  inspect the new card and answer for concise wording, partial coverage,
  sensible precision, and absence of red verification errors. Confirm existing
  vault/model data remains available.

### Exact-day presentation APK installed in place — 2026-09-24

- After explicit member approval, installed the 156-test debug APK on Samsung
  SM-S928B (Android 16) with `adb install -r`; ADB reported `Success`. A
  force-stop and `am start -W` reported a successful cold launch. No app-data
  clear, uninstall, downgrade, signing-key change, or private vault/model-file
  access was performed.
- Member biometric unlock, retained-record/model confirmation, the revised
  card/answer wording, and any red-error check remain physically unverified.
  Next bounded step: member unlocks and asks an exact-day calorie question for
  a day with a saved meal, compares the answer to visible records, and reports
  whether the new card avoids technical fields and floating-point artifacts.

### Member feedback on revised exact-day presentation — 2026-09-24

- On the installed 156-test build, the member reported that the new answer and
  expanded “From your records” section both look very good. This is positive
  physical-device feedback for wording and presentation; no screenshot or raw
  member value is retained here.
- The member subsequently confirmed that the information matched the saved
  records and was correct. This verifies factual content for this exact-day
  scenario. Retained vault/model availability, rotation/accessibility checks,
  and the explicit no-red-error condition remain unconfirmed; do not infer
  them from the answer-quality report.

### Next Gate B phone check: period change — 2026-09-24

- The installed 156-test build is ready for a member-run continuity check;
  there is no new APK to install. Ask for today's recorded calories, then the
  rolling seven-day recorded calories, then follow with a short protein question.
  The second and third turns must use fresh weekly evidence, and every answer
  must label its own period without borrowing today's result. Check for a red
  verification error and compare any returned values with visible records.
- This is a planned check, not physical acceptance. Cancellation, lock/restart,
  token-budget reconstruction, airplane mode, and the remaining UI quality
  matrix are still open Gate B work.

### Gate B period-change phone finding — 2026-09-24

- With the member's permission, inspected the current chat on the connected
  Samsung SM-S928B (Android 16) using a temporary screen capture outside the
  repository. The first two turns had visible today and weekly answers. After
  the short protein follow-up, a weekly local-record evidence card appeared,
  but there was no assistant reply beneath it even after scrolling to the end.
  No red verification error was visible. This sequence fails the Gate B
  period-change acceptance despite the evidence card selecting the week.
- The member was hiding the keyboard when the answer seemed to disappear; the
  inspected screen had the keyboard closed, so the missing final bubble was
  not merely obscured by the keyboard. The precise continuation failure stage
  has not been established. No screenshot, raw answer, or personal values are
  retained in this checkpoint. Next bounded task: inspect metadata-only trace
  for a fresh reproduction if the member approves, then fix the confirmed
  handoff/failure path and rerun the complete Android gate before installation.
- The member immediately resent the short protein follow-up and reported that
  it worked very well. This confirms a successful retry on the same installed
  build, but does not erase the observed missing-reply failure or establish its
  cause. The first-attempt failure remains open as an intermittent Gate B gap.
- During a later repeat of the sequence, the member reported a red message on
  the second question and that the phone felt hot. The exact diagnostic text,
  tool stage, and thermal status at the moment of failure were not captured; no cause is inferred.
  Stop repeated model tests until the phone cools. This is another failed Gate B
  observation, not acceptance of the period-change path. A metadata-only trace
  may be inspected only with member permission if it was enabled for that turn.
- The member also reported substantial battery use by the Android CLI phone
  helper. A later read-only ADB sample found its instrumentation process active
  at 116% CPU, battery at 31% and 39.4°C, and Android thermal status 2 with
  skin temperature about 41°C. This is a single subsequent sample, not proof
  that the helper caused the red error or all battery drain. No process was
  stopped, package removed, or Metria data changed while awaiting permission.
- The member force-stopped the Android CLI helper directly on the phone. A
  subsequent plain-ADB check found no helper process; thermal status had
  dropped to 1, with skin temperature about 39.6°C. This supports that the
  helper stayed stopped, but is not a controlled battery or causal test. The
  agent did not restart Android CLI or access the assistant trace.

### Gate B continuation reliability hardening — 2026-09-25

- Code inspection found two paths that could leave a local evidence card without a
  visible final outcome: a rejected tool continuation reused the previous request
  ID and lost UI correlation, and status polling could supersede the keyed tool
  effect before the new request ID was adopted. These are defensible code risks,
  not a confirmed diagnosis of the member's intermittent phone failure. No trace
  was read and the Android CLI phone helper was not restarted.
- The tool hand-off now runs in the conversation's composition-owned coroutine
  scope, keeps optimistic pending state until a matching lifecycle request is
  adopted, and presents a recoverable error if continuation is rejected. A
  cancelled token cannot apply a late result. Final reply observation also
  rechecks when the active request ID changes, while handled IDs still prevent
  duplicate bubbles. The pending gate handles a final lifecycle poll that
  arrives before request-ID reconciliation without leaving “thinking” stuck.
- Focused tests cover successful and fast-completed continuation, rejection,
  cancellation and stale IDs, loaded-before-reconcile ordering, fallback error
  text, and exactly-once result matching. Independent review identified the
  early-final-poll case; a follow-up review found its correction sound.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 164 tests, 0 failures, 0 errors; the debug APK assembled. This is
  automated evidence only. The new APK has not been installed, and Gate B
  physical acceptance remains open. No vault, model, or phone data was changed.
- Next bounded step: request approval for an in-place `adb install -r` when the
  member is ready, then have the member unlock and repeat the today → rolling
  seven days → protein follow-up sequence once on a cool phone. Verify that
  each turn ends with a reply or a persistent recoverable error, and compare
  record-backed periods and values with visible records. Do not attribute any
  observed red error to a particular cause without fresh evidence.

### Gate B continuation build installed in place — 2026-09-25

- With explicit member approval, installed the 164-test debug APK using
  `adb install -r` on Samsung SM-S928B, Android 16 / API 36; ADB returned
  `Success`. A subsequent force-stop and `am start -W` reported a successful
  cold launch. No app-data clear, uninstall, downgrade, signing-key change,
  vault/model-file access, assistant-trace read, or Android CLI screen-helper
  start was performed.
- Member biometric unlock, retained-record/model confirmation, and the
  today → rolling-seven-day → protein follow-up recheck remain pending.
  Installation and launch do not establish physical Gate B acceptance.

### Gate B first-turn today nutrition failure — 2026-09-25

- On the installed 164-test build, the member reported a red error on the first
  Spanish question asking for today's recorded calories. The planned later
  weekly and protein turns were stopped; this sequence failed physical Gate B
  acceptance. Retained records/model availability has not yet been explicitly
  reconfirmed for this install.
- With explicit member permission, a plain-ADB read of the opt-in metadata-only
  assistant trace showed that the latest turn started and then its reply was
  rejected as `unsupported_personal_fact`, with no approved record-read tool
  between those events. No prompt, reply, record value, or raw trace was copied
  into the repository. The Android CLI phone helper was not restarted.
- Code inspection found a matching host-policy coverage gap: explicit current-
  period calorie-total questions without a goal-comparison or short-follow-up
  form did not require `read_nutrition_summary`. The model could therefore
  return ungrounded personal prose, which the factual auditor correctly blocked.
  This explains the observed event sequence, but does not establish why the
  model omitted a tool in this particular run.
- Next bounded task: require the correct fresh today/rolling-seven-day nutrition
  read for explicit current-period calorie/protein totals, preserve historical
  and unsupported-period boundaries, add focused tests, and run the complete
  Android gate before requesting another installation.

### Explicit current-period nutrition read build — 2026-09-25

- Clear one-metric recorded calorie/protein amount questions for today or the
  rolling last seven days now require a fresh matching
  `read_nutrition_summary` response. The host rejects wrong-tool and
  wrong-period substitutions; an audited deterministic reply can use only that
  same validated result if model wording fails. Historical exact-day rules,
  factual auditing, and saved-record calculations were not changed.
- The new routing fails closed for mixed metrics/periods, unsupported month or
  previous-week ranges, per-day rates, and advice phrasing. Focused Spanish and
  English tests cover the observed wording, common verb variants, missing
  estimates, and those boundaries. Independent review found two wording gaps
  and an advice-intent false route; each was corrected and tested.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 166 tests, 0 failures, 0 errors; the debug APK assembled. This build
  has **not** been installed. The physical first-turn failure remains open
  until a member-approved in-place install and repeat on the target phone.
- Next bounded step: ask approval for `adb install -r`; after member biometric
  unlock, repeat the today recorded-calorie question once, confirm a fresh
  today evidence card and an answer without the red verification error, then
  resume the weekly-period and protein follow-up checks only if that first
  turn passes. Confirm existing records and downloaded model remain available.

### Explicit current-period nutrition build installed — 2026-09-25

- With explicit member approval, installed the 166-test debug APK in place on
  Samsung SM-S928B (Android 16 / API 36) using `adb install -r`; ADB returned
  `Success`. A force-stop and `am start -W` reported a successful cold launch.
  No app-data clear, uninstall, signing-key change, vault/model-file access,
  trace read, or Android CLI screen-helper start was performed during install.
- Biometric unlock, retained-record/model confirmation, and the corrected
  first-turn today-calorie recheck remain pending. Installation does not
  establish physical acceptance; stop before weekly follow-ups if the first
  turn still fails.

### Member first-turn feedback — 2026-09-25

- On the installed 166-test build, the member reported **no red verification
  message** after repeating the first today-calorie question. This is positive
  physical feedback for the specific previous error state, not yet full
  acceptance of the answer: a final assistant bubble, matching today evidence,
  factual accuracy, and retained vault/model data were not explicitly
  confirmed in that report.
- Next check: confirm that the turn ended with a today record card and final
  answer matching visible records, plus preservation of existing records and
  downloaded model. Only then continue to the weekly-calorie and short-protein
  period-change turns.
- The member subsequently confirmed that the existing records and downloaded
  model are still present after this in-place update. The wording of that
  confirmation did not establish whether the assistant's latest turn had a
  final answer/card or whether its values matched the records; those checks
  remain open.
- The member then confirmed that the first turn had a final assistant answer
  and a today “From your records” card, and that the answer matched their
  saved records. Together with the earlier no-red-error and preservation
  reports, this physically accepts the corrected first-turn today-calorie
  scenario on the 166-test build. The weekly-period change and short protein
  follow-up remain separate, unverified Gate B checks.
- The member then ran the weekly-calorie question and short protein follow-up
  on the same installed build and reported that the sequence “ended very well.”
  This is positive phone feedback for completion, but the report did not
  explicitly state whether both cards/answers used fresh seven-day evidence,
  matched visible records, or avoided a red error. Do not mark the period-
  change Gate B check fully accepted until those details are confirmed.
- The member chose to move to the next check without further period/value
  confirmation. Keep that evidence gap open. The next bounded physical check
  is cancellation during a local assistant turn: cancel while thinking, verify
  no late answer or action appears, then confirm the composer remains usable.

### Stop responsiveness implementation — 2026-09-25

- Stop now invalidates the active request and pending handoffs synchronously,
  presents an accessible “Stopping…” status, and keeps Send disabled while the
  synchronized LiteRT cancellation runs on a background dispatcher. On
  completion it shows “Response stopped” and returns the composer to use.
- Typed request starts, native tool continuations, and cancellation share a
  session-scoped mutex. Each queued start rechecks its pending token after
  acquiring the lock; cancellation teardown runs non-cancellably even if the
  conversation screen is disposed.
- Cancellation completion is generation-checked so a reset/new conversation
  cannot receive stale stop confirmation. Late replies remain ineligible via
  the cleared active request ID.
- Focused `AssistantConversationPresentationTest` passed, including duplicate
  cancellation gating, reset-during-cancel coverage, and a queued-start race
  check. The full Android gate remains for the parent handoff; no physical
  device check or install has been performed for this change.
- Next physical check: on the existing installation after an approved build,
  tap Stop during a local turn; verify immediate “Stopping…”, no late answer,
  “Response stopped” after shutdown, and a usable composer.
- Independent review caught a queued-start race: a native request could have
  begun after Stop had finished. A session-owned operation mutex now orders
  typed starts, tool continuations, and teardown; each queued start rechecks
  its pending token under the lock. A focused concurrency test covers the
  stop-before-start ordering. Follow-up read-only review found no remaining
  ordering defect; scope disposal and LiteRT timing still need phone evidence.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 169 tests, 0 failures, 0 errors, and a debug APK. `git diff --check`
  passed. Physical cancellation feedback and late-result acceptance remained
  open pending an approved in-place install.

### Stop responsiveness build installed in place — 2026-09-25

- With explicit member approval, installed the 169-test debug APK on the one
  connected Samsung SM-S928B (Android 16 / API 36) using plain ADB
  `install -r`; Android reports the updated package. A force-stop and
  `am start -W` completed a successful cold launch. The Android CLI phone
  helper was not started; no data clear, uninstall, signing-key change, or
  vault/model-file access was performed.
- Member biometric unlock, retained-data confirmation, and the physical Stop
  check are pending. Ask the member to tap Stop during a local turn and report
  whether “Stopping…” appears promptly, no late answer appears, “Response
  stopped” is shown, and the composer accepts a new message afterward.

### Native cancellation force-close — 2026-09-25

- The member reported that Stop appeared briefly, then Metria force-closed; the
  control also did not feel like a clear cancellation action. With explicit
  permission, the parent read a narrowly filtered crash buffer and identified
  `SIGABRT: Pure virtual function called!` in `liblitertlm_jni.so` during native
  `sendMessage` on the private assistant worker. This is a failed physical
  cancellation check on the installed 169-test build; the corrected flow has
  not yet been installed or physically accepted.
- Failure signature: Stop interrupted the `Future`, called `cancelProcess`, and
  immediately closed the native conversation while synchronous `sendMessage()`
  could still be in JNI. `Future.cancel(true)` can report done before that
  native call exits. The crash identifies `nativeSendMessage` as the active JNI
  frame, but does not by itself establish whether concurrent `cancelProcess`,
  premature conversation close, or their interaction caused the abort.
- The proposed lifecycle fix introduces a use lease for each private native conversation. Stop
  invalidates the request immediately and asks LiteRT to cancel, but native
  conversation close waits until the worker releases its lease. Runtime unload
  also defers engine close while a private worker is active. Private assistant
  turns now use LiteRT's asynchronous callback API, the documented cancellation
  mode. Stop waits for the completion/error callback on a background dispatcher
  before releasing the UI send gate; no lifecycle monitor is held during that
  wait. If LiteRT never delivers a terminal callback, Metria deliberately keeps
  the conversation and engine alive and keeps Send unavailable rather than
  closing native state whose safety is unknown. Lock/reset and queued-start
  paths continue to reject stale requests.
- The Stop action now has a filled tonal affordance and is labelled “Stop
  response”; the explicit “Stopping…” state remains visible until native worker
  cleanup finishes.
- Deterministic tests exercise a blocked send, retirement racing `cancelProcess`,
  a terminal callback arriving during a blocked cancel, competing timeout
  cancellation sources, cancel-before-start, and callback aggregation of delta
  text, every tool call, and channels. Each cancel claim now acquires a
  conversation-lifetime lease before publishing timeout and invoking
  `cancelProcess`; the lease is released only after that call returns. Callback
  tool calls append across messages, including identical calls, so multi-call
  responses remain rejected by the existing host validator. Whitespace-only
  text deltas are also preserved. The focused Android tests passed (7 tests,
  0 failures/errors). Follow-up read-only review confirmed the native lifetime
  and tool-call fixes, then identified the whitespace-delta gap corrected above.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 176 tests, 0 failures, 0 errors, and a debug APK. `git diff --check`
  passed. The corrected APK has **not** been installed. Physical verification
  must confirm no force-close, no late reply, retained data, and that the
  composer becomes usable only after Stop completes.

### Native cancellation fix installed in place — 2026-09-25

- With explicit member approval, installed the 176-test debug APK in place on
  the one connected Samsung SM-S928B (Android 16 / API 36) via plain ADB
  `install -r`. Android reports a fresh package update time, and a subsequent
  force-stop plus `am start -W` completed a successful cold launch. The Android
  CLI phone helper was not restarted. No app-data clear, uninstall, signing-key
  change, or vault/model-file access was performed.
- Biometric unlock, retained-record/model confirmation, and the physical Stop
  regression remain pending. Verify the new “Stop response” tap feedback,
  prompt “Stopping…” state, no force-close or late reply, and a usable composer
  after “Response stopped.” Automated tests do not establish physical passage.

### Member cancellation feedback and meal-image follow-up — 2026-09-27

- On the installed 176-test build, the member reported that cancellation worked
  without the earlier force-close. The cancellation status card subsequently
  vanished after the member left the app and returned, consistent with its
  transient, non-persisted status contract. Do not mark the complete Stop
  acceptance row passed until the no-late-reply, composer-reuse, and status
  lifetime checks are confirmed.
- The member reported that two member-selected meal photos completed local
  analysis but showed the recoverable “draft was not safe to display” message
  instead of opening an editable proposal. This identifies strict proposal
  rejection, not a confirmed cause. No raw photo, model response, or meal
  interpretation is stored in this checkpoint. The member is checking the
  existing Developer diagnostics outcome; repeated hot-device inference should
  be avoided.
- Next bounded task: classify the proposal rejection without persisting raw
  output, then add a narrowly justified parser/prompt regression test and
  correction if the rejected shape can be proven safe. Repeat the complete
  Android gate before requesting any new in-place install.

### Debug image-analysis inspection implementation — 2026-09-27

- The member clarified that screenshot/manual-diagnostics handoffs are too
  slow for development and explicitly requested direct developer inspection
  of image-analysis trace, model response, and a reduced test image. This is
  a deliberate debug-only, opt-in exception to the ordinary local-only data
  policy; the metadata-only assistant trace remains raw-content-free.
- The strict meal-proposal parser now returns a fixed, content-free rejection
  category for invalid JSON, root, group, item, or portion range. It still
  rejects the entire unsafe draft. The category is available in local audit
  metadata; the model response is not added to that audit.
- At this checkpoint, a separate session-only **Image development capture**
  control started off on each app launch. This enablement policy was superseded
  by the persistent-opt-in checkpoint below. When enabled for a test image, it kept at most one bounded
  raw vision response and reduced EXIF-free JPEG in app-private no-backup
  storage, with duration, outcome, and parser category. Disable, Clear,
  lock/background, process startup, or destruction removes it; a 30-minute
  expiry is enforced while the app runs. After a hard process kill, the next
  launch or authorized ADB helper read removes an expired capture. Release
  builds cannot enable or write it. A local ADB
  helper reads the capture outside the repository after member authorization.
- Complete Android gate from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` passed
  with 189 tests, 0 failures, 0 errors after a release-startup cleanup
  regression test; the debug APK assembled.
  `git diff --check` passed. The phone was not listed by ADB at the time of
  attempted read, so the old in-memory response could not be inspected. No
  new APK has been installed and the two meal-photo failures remain undiagnosed.
- Independent review caught binary redirection incompatibility with older
  Windows PowerShell and the hard-process-kill expiry gap. The ADB helper now
  copies native output as bytes, checks age before decoding the response,
  and removes an expired capture. Documentation names the residual crash
  lifetime explicitly. Follow-up review found no further material code issue;
  the helper still needs a device-side exercise after installation.
- Next bounded step: independent review of the new capture path, then request
  approval for an in-place debug APK install. On a cool phone, enable Image
  development capture, analyze one member-approved failing image once, keep
  Metria open/unlocked, and inspect only that capture via plain ADB. Use the
  captured parser category and bounded response to justify a safety-preserving
  parser/prompt correction, followed by another full gate and physical test.

### Image-inspection debug build installed in place — 2026-09-27

- With explicit member approval, installed the 189-test debug APK on the one
  connected Samsung SM-S928B (Android 16 / API 36) using plain ADB
  `install -r`; ADB returned `Success`. A force-stop and `am start -W` reported
  `Status: ok` and a cold launch. No app-data clear, uninstall, signing-key
  change, vault/model-file access, or Android CLI phone-helper start occurred.
- Member biometric unlock, retained-record/model check, capture-toggle UI
  check, and one member-run image analysis are still pending. Installation
  and launch do not establish that the meal-photo failure is corrected or that
  the new ADB capture helper works on the phone.

### First image-capture finding — 2026-09-27

- The member completed one meal-image run on the installed debug build and
  reported the recoverable draft-rejected message. With the member's explicit
  development-capture authorization, the plain-ADB helper successfully read
  the one app-private opt-in capture without a screen dump or Android CLI
  phone helper. The result was `draft_rejected`, fixed category `invalid_item`,
  after about 36 seconds.
- Inspection of the bounded output and reduced test image showed that the
  model used descriptive prose in the required enum-only `evidence` field.
  It also misidentified several visible dishes and invented an uncertain
  extra component. Merely accepting that malformed field would expose a
  misleading draft, so the parser's fail-closed behavior remains appropriate.
  No raw photo, response, or meal names/values are kept in this checkpoint.
- The second member-provided photo has not yet been captured on this build.
  Existing vault/model preservation after installation and the full meal-review
  path are still not physically confirmed. Next bounded task: review a safer
  vision prompt/UX fallback for complex multi-dish photos, add targeted tests,
  then run the complete Android gate before another installation. Avoid
  repeated inference while the phone is warm.

### Meal-image prompt hardening after first capture — 2026-09-27

- The vision request now asks for at most the two strongest clearly visible
  candidates, omits uncertain extra foods, and requires the exact `seen` or
  `possible_inside` evidence token. Descriptive evidence text remains rejected;
  it is not silently converted to a visible-food claim. An empty item list also
  fails closed instead of opening a review with no foods. These changes do not
  alter deterministic nutrition, member review, or the temporary-image policy.
- The complete Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  192 tests, 0 failures, 0 errors, and debug APK assembled. `git diff --check`
  passed. The new APK has **not** been installed. The observed photo failure
  remains unresolved on the physical phone until the member authorizes an
  in-place install and tries one image on a cool device. A prompt constraint
  cannot guarantee that the local model will follow the schema or identify
  every dish correctly; rejected drafts must continue to offer manual entry.

### Meal-image prompt build installed in place — 2026-09-27

- With the member's explicit approval, installed the 192-test debug APK on the
  one connected Samsung SM-S928B (Android 16 / API 36) using plain ADB
  `install -r`; ADB returned `Success`. Force-stop and `am start -W` reported
  `Status: ok` and `LaunchState: COLD`. The Android CLI phone helper was not
  started. No app-data clear, uninstall, signing-key change, or vault/model
  access was performed.
- Member biometric unlock, retained-record/model confirmation, and the
  complex-meal photo test are pending. Installation and launch do not establish
  that the model now follows the evidence enum or identifies the photo safely.

### Rejected-photo follow-up and manual route — 2026-09-27

- The member retried one image on the installed prompt-hardened build and
  reported the same draft-rejected message. The opt-in, plain-ADB capture
  classified it as `invalid_group` after about 44 seconds. Inspection showed
  an unsupported grouped-item type and materially inaccurate food labels, so
  accepting or loosely coercing the whole draft would have been misleading.
  The one app-private capture and temporary reduced JPEG were removed after
  inspection; no raw image, response, food names, or record values are retained
  in this checkpoint. This is a **failed physical vision acceptance**, not a
  successful correction from prompt changes.
- A direct manual-entry card has been added for rejected photo drafts. It opens
  the existing focused meal review with no model-suggested foods, starts with
  **Add first dish** instead of an empty Save action, and records unmatched
  reviewed food as `manual_entry` rather than `ai_estimate`. The strict parser
  remains unchanged. The optional reduced reference follows the existing
  member-confirmation path. Manual review closes at vault lock.
- The complete Android gate after this UX change and a post-review safety-test
  addition passed with 194 tests, 0 failures, 0 errors, and debug APK assembly.
  Independent read-only review found no confirmed defect in the fallback's
  empty proposal, manual provenance, or save gating, but Compose interaction
  and physical states remain unverified. The APK has not been installed, so
  keyboard, rotation, thumbnail opt-in, and save provenance still need a
  member-run phone check. Do not claim that the local model now recognises the
  complex meal correctly.

### Reliable meal-photo and correction-learning design slice — 2026-09-27

- In response to the member's several-meals-per-day reliability concern, the
  working `docs/vision-correction-learning-spec.md` separates schema validity,
  visual food accuracy, and thermal/latency acceptance. Manual entry remains a
  recovery path, not evidence that recognition is reliable. The proposed
  correction-memory retention scope still awaits the member's choice.
- A pure, bounded, in-memory diff now compares initial and confirmed review
  items by stable ID. It distinguishes additions, removals, renames, real dish
  moves, portion changes, and catalog-match changes; a dish-label edit alone
  does not count as a move. Independent review found and prompted that last
  correction. No new vault persistence, ranking, automatic suggestion, debug
  capture, or production call site was added for this slice.
- The complete Android gate passed from `native-android`:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  202 tests, 0 failures, 0 errors, and debug APK assembled. The new APK was
  **not installed** and no physical acceptance is claimed for this slice.
- Next bounded step, after the member chooses retention scope: wire the diff
  only to successful reviewed Save, then design bounded encrypted storage,
  member inspection/deletion, vault/archive compatibility, and tests before
  letting feedback affect any candidate ranking. Base vision quality still
  requires a varied, consented photo benchmark and repeated-use phone gate.

### Automatic private correction-note foundation — 2026-09-28

- The member chose automatic, on-device capture from a successfully saved,
  corrected photo meal, without a settings toggle or second confirmation.
  "Remember this dish" remains a separate choice. The review flow now derives
  a bounded stable-item diff only at Save and passes it into the same encrypted
  vault transaction as the meal. No-op, manual, rejected, cancelled, and failed
  saves do not retain a draft-to-confirmed note. Privacy settings offers
  inspect and clear controls without deleting meals or saved dishes.
- The optional version-1 vault/archive field defaults empty for older payloads.
  Pure-JVM codec tests cover round-trip, malformed records, and dangling meal
  links. New food-match hints carry meal provenance; edit/delete removes only
  that meal's hints and note with rollback, while removing only a private
  reference thumbnail leaves learning intact. Older global hints have no
  provenance: they remain readable in the vault/archive and member-clearable,
  but no longer influence catalog ranking. The settings dialog discloses
  their inactive status.
- Correction notes are **not yet used to rank future photo suggestions**; the
  member-facing wording says so. Existing catalog-match hint behavior is
  separate. No claim of better visual recognition or several-meals-per-day
  readiness follows from this storage work.
- Complete Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  212 tests, 0 failures, 0 errors, and debug APK assembled. `git diff --check`
  passed. The APK was **not installed**; vault compatibility, review UI,
  rotation, clear/unlink, and photo outcomes remain unverified on the phone.
  No vault instrumentation suite exists yet, so encrypted storage/rollback
  behavior is not covered end-to-end by the pure-JVM tests.
- Next bounded task: use only explainable, member-reviewable correction candidates with
  negative/contradiction tests. Separately benchmark varied photos and repeated
  cool/warm phone use. Obtain installation approval before physical checks.

### Correction-note foundation installed in place — 2026-09-28

- With the member's explicit approval, paired plain ADB to the current
  wireless-debugging endpoint and selected the Samsung SM-S928B running
  Android 16 / API 36. The 212-test debug APK was installed with `adb install
  -r`; ADB returned `Success`. A force-stop followed by `am start -W` reported
  `Status: ok` and `LaunchState: COLD`. The Android CLI phone helper was not
  started. No app-data clear, uninstall, signing-key replacement, or vault/model
  deletion was performed.
- This confirms installation and launch only. Member biometric unlock,
  preservation of existing records/model, correction-note capture and clear,
  thumbnail-only update, rotation/keyboard accessibility, and repeat-photo
  outcomes remain physically unverified. No screenshot or raw member data was
  captured. Next: member-led smoke test of existing data and the changed meal
  review flow before any physical acceptance claim.

### First photo attempt on installed correction-note build — 2026-09-28

- The member reported the visible "Couldn't make a reliable meal draft"
  fallback after local photo analysis. This is the parser-rejected-draft path:
  no model-proposed foods are displayed and no meal is saved. The fallback
  offers manual entry. The correction-note feature cannot learn from this run
  because it requires a valid draft followed by a successful reviewed Save.
- The precise rejection category and raw model output for this run have not
  been inspected. Existing-record/model preservation and the manual fallback
  interaction are not yet confirmed by the member. Treat this as failed
  physical photo-draft acceptance, not improved recognition. Avoid repeated
  inference while the phone is warm; a later single opt-in development capture
  can diagnose the output if the member authorizes it.

### Opt-in repeat-photo capture on installed build — 2026-09-28

- The member enabled Image development capture and repeated the same photo
  once. With that authorization, the plain-ADB helper read the one debug-only
  app-private capture: `draft_rejected`, category `invalid_group`, about 54.3
  seconds. The model used an unsupported grouped-item kind, despite the prompt
  specifying the allowed enum. It also produced more candidate components and
  stronger identity confidence than the photo justified. Converting that enum
  alone would not make the draft trustworthy; fail-closed validation was the
  correct outcome.
- The reduced temporary JPEG on the development computer was inspected and
  deleted immediately afterward, along with its temporary directory. No raw
  photo, model response, meal names, device address, or member values were
  added to the repository. The app-private capture should be cleared with the
  in-app control or vault lock; no claim is made that it has already been
  cleared. This remains a failed physical vision case and further evidence
  that prompt-only hardening is insufficient.
- Next bounded task: evaluate candidate draft quality separately from schema
  validity on a consented set of varied photos, then make a measured model or
  review-flow improvement without loosening food-identity validation.

### Persistent debug image-capture opt-in — 2026-09-28

- After a member-edited, one-dish photo yielded a meal draft on the installed
  build, the member requested that Image development capture remain enabled
  until explicitly disabled. This reports one successful draft, not a verified
  nutrition estimate or general multi-dish reliability.
- The debug-only setting now persists as a one-byte choice in app-private
  no-backup storage. It is off for a fresh or pre-existing installation until
  enabled once, then survives lock, backgrounding, activity recreation, and
  process restart until **Disable**. The raw reduced photo and model response
  still clear on lock/background/startup, explicit Clear or Disable, and
  activity teardown, with the existing 30-minute expiry. Release startup
  removes any leftover debug choice and capture. A per-analysis recording token
  also prevents an in-flight result from recreating a capture after a clear.
- The diagnostic UI and testing/safety/screen contracts now distinguish the
  durable choice from the short-lived payload. Persisted enablement alone is
  not authorization to read a run over ADB; ask for the active test session.
- The complete Android gate passed from `native-android` with
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`:
  216 tests, 0 failures, 0 errors, and the debug APK assembled. The new APK
  was subsequently installed in place after separate member approval (see
  installation note below). No physical acceptance is claimed from these tests
  or the prior one-dish run.

### Persistent image-capture setting installed in place — 2026-09-28

- With explicit member approval, selected one currently connected transport
  for the Samsung SM-S928B (Android 16 / API 36) and installed the 216-test
  debug APK using only `adb install -r`; ADB returned `Success`. A force-stop
  and `am start -W` returned `Status: ok`, `LaunchState: COLD`. No app-data
  clear, uninstall, signing-key replacement, or vault/model deletion occurred.
- Installation and cold launch are confirmed, but biometric unlock, preservation
  of existing records, persistent opt-in after background/restart, disable
  behavior, and raw-capture cleanup still require member-led physical checks.
  The prior enabled session does not migrate to the new durable setting: enable
  Image development capture once more in Developer diagnostics, then it should
  remain enabled until explicitly disabled.

### Local model-assisted food-name correction — 2026-09-29

- The expanded dish editor now lets the member describe a correction for one
  selected food in ordinary language. LiteRT uses the local text model to
  suggest only a replacement name. A strict one-key parser rejects prose,
  extra nutrition/portion/ingredient fields, unchanged names, and malformed or
  oversized output.
- The member sees an old → new preview with **Apply name** and **Discard**.
  Applying changes only the local draft's name and clears its catalog match;
  portion, ingredients, preparation, and any saved record are not changed by
  the model. The existing dish Apply and final meal Save gates still control
  when review state and the meal reach the vault. Busy/unavailable runtime,
  cancellation, editor exit, and stale request/item results are handled. A
  canceled native conversation stays owned by its inference worker through
  teardown; another correction waits for that worker, and runtime unload defers
  closing the engine until active workers have exited.
- The complete gate from `native-android` passed with
  `:app:testDebugUnitTest :app:assembleDebug --no-daemon --rerun-tasks`:
  222 tests, 0 failures, 0 errors, and debug APK assembly. `git diff --check`
  passed. The APK was subsequently installed in place with member approval
  (see below). Physical checks of keyboard behavior,
  correction quality, cancellation, and final-save persistence remain pending;
  no visual food-recognition improvement is claimed from the automated tests.

### Food-name correction installed in place — 2026-09-29

- With explicit member approval, selected the connected Samsung SM-S928B
  (Android 16) and installed the tested debug APK using `adb install -r`.
  ADB returned `Success`; force-stop and `am start -W` returned `Status: ok`
  and `LaunchState: COLD`. No app-data clear, uninstall, signing-key change,
  vault removal, or model removal was performed.
- Installation and launch are confirmed. Biometric unlock, preservation of
  existing records, the keyboard-visible correction flow, safe cancellation,
  suggestion quality, and final Save/reopen still require member-led physical
  verification. No physical acceptance is claimed yet.
- Member follow-up: the food-name suggestion worked on the installed phone.
  This confirms the suggestion path for that attempt, but does not yet verify
  that Apply cleared the old catalog match, the corrected meal saved and
  reopened accurately, or that cancellation and keyboard states pass.
- Subsequent authorized on-device inspection showed a saved meal whose title
  used the member's corrected dish wording while its Food field retained the
  model's earlier incorrect food label. The member had tapped **Use this name**
  on a similar-meal suggestion, which changes the meal title rather than a
  component. This is a failed end-to-end food-correction check and a confusing
  distinction in the review UI, not evidence that **Apply name** persisted.
  The temporary screenshot was removed from the development machine; no raw
  image or member meal values are retained in this checkpoint.

### Saved-meal correction and atomic photo removal — 2026-09-29

- The similar-meal action now reads **Use meal name**, with copy clarifying it
  changes the meal title only. This distinguishes it from food correction.
- Saved-meal editing is extracted into `SavedMealEditorScreen`. A member may
  ask the local text assistant about one food, review the old → new label, and
  Apply or Discard. Direct manual rename stays available. A name change clears
  that food's prior catalog match; a new match can be selected separately.
  Assistant output changes only the in-memory draft. Member-entered nutrition
  remains unchanged and is explicitly described as unchanged in the correction
  panel. Saving still uses the existing single validated meal update.
- Removing a reference photo is staged in the editor and reaches the vault only
  with Save. Cancel/back therefore leaves the previously saved meal and photo
  untouched. Draft fields and selection state are saveable across configuration
  recreation; an active suggestion is cancelled and cleared on exit/recreation.
  IME padding and a scrollable editor keep the save action reachable.
- Six policy tests cover name-change match invalidation, photo-removal draft
  immutability, validation of a renamed catalog meal without stale nutrition,
  unavailable or unresolved catalog blocking, and explicit confirmation when
  existing nutrition coverage would be reduced. A failed vault Save or Delete
  now keeps the editor and its draft visible with a local error; the Delete
  dialog displays that error and deletion remains behind its confirmation.
  An unavailable old catalog ID does not block a member-selected valid
  replacement match.
  No `androidTest` source set or Compose UI-test dependency is present, so visual,
  keyboard, and button-flow UI tests remain a physical acceptance gap.
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`
  passed: 228 tests, 0 failures, 0 errors; debug APK assembled. `git diff --check`
  passed. The APK was not installed and no physical acceptance is
  claimed for this slice.
- Follow-up: `EncryptedVault.updateMeal` currently removes correction learning
  linked to a meal after substantive edits and does not add a new correction
  diff from saved-meal editing. This slice preserves that vault contract;
  saved-editor corrections do not yet influence future suggestions. Physical
  verification remains required for save/reopen, catalog-rematch nutrition,
  cancellation/rotation, keyboard reachability, and Cancel after photo removal.

### Saved-meal correction APK installed in place — 2026-09-30

- After the final 228-test gate and clean `git diff --check`, the member
  explicitly approved installation. A single wireless ADB transport for the
  Samsung SM-S928B (Android 16 / API 36) was selected. `adb install -r`
  returned `Success`; force-stop and `am start -W` returned `Status: ok` and
  `LaunchState: COLD`. No app-data clear, uninstall, signing-key replacement,
  vault/model removal, or biometric automation was performed.
- Installation and cold launch are confirmed only. Preservation of existing
  records/model after biometric unlock, saved-food correction quality, Save
  and reopen, catalog re-match, cancellation, photo-removal Cancel, rotation,
  keyboard reachability, and TalkBack still require member-led physical checks.
  No raw device screenshot or member record was captured for this install.

### Partial photo-draft schema recovery — 2026-09-30

- With the member's authorization for this active test run, a local ADB read of
  the opt-in debug capture showed a structured meal response rejected after
  about 34.6 seconds as `invalid_group`. The model used a grouped-item kind
  outside the parser's enum and a second nonstandard evidence token. The
  reduced test JPEG was inspected and removed from the development machine;
  no photo, model prose, meal names, or device identity was added to the repo.
  The app-private capture should still be cleared by the member's Clear control
  or vault lock.
- The parser now normalizes only an explicit, unambiguous added group/evidence
  alias while retaining its earlier safe aliases. It validates foods
  independently, keeps only valid siblings in a visibly partial draft, and
  falls back to manual entry for an invalid root or no valid foods. Unknown
  fields, free-text evidence, invalid portions, and unsupported groups are not
  guessed into accepted foods. A partial result has low overall confidence,
  bounded omitted-food count, and a fixed `draft_partial` audit/capture outcome.
- The result and review screens disclose omitted suggestions. Catalog-backed
  nutrition saved from such a review is marked partial even when all retained
  foods have matches. The review's 12-food limit no longer silently discards a
  validated food when a catalog hint splits another name. At the cap, the
  notice explains that a food must be removed before adding a missing one.
  Meal Save and deterministic nutrition still require member review.
- Independent review found and prompted preservation of earlier parser aliases
  and singleton groups, and the partial-nutrition coverage fix. The full
  Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  238 tests, 0 failures, 0 errors, and debug APK assembled. `git diff --check`
  passed. The new APK has **not** been installed. No physical photo-analysis,
  visual-accuracy, nutrition, thermal, or privacy acceptance is claimed from
  automated tests.
- Next bounded task: with explicit in-place install approval, have the member
  unlock and run one cool-device photo analysis, verify review contents and
  partial/full labeling against the photo, then test Save/reopen without
  assuming the model's food identities or portions are correct. Do not treat
  this schema recovery as proof of several-meals-per-day reliability.

### Partial photo-draft APK installed in place — 2026-09-30

- After member approval and the 238-test Android gate, selected one current
  ADB transport for the Samsung SM-S928B (Android 16 / API 36). `adb install -r`
  returned `Success`; force-stop and `am start -W` returned `Status: ok` and
  `LaunchState: COLD`. No app-data clear, uninstall, signing-key change,
  vault/model deletion, or biometric automation was performed.
- After biometric unlock, the member confirmed that existing records remained
  intact. One photo analysis ran on the installed build, and the member reported
  that the proposed foods matched the photo and the partial-draft notice matched
  what was shown. This is a successful member-observed check for that one photo,
  not evidence of reliable recognition across varied meals or repeated warm
  runs. The member also reported that a photo with two separate dishes reached
  review and offered a remembered-dish suggestion for a provisional food label,
  rather than silently replacing it. The member accepted the suggestion, saved
  and reopened the meal, and confirmed that the corrected food label persisted.
  The corrected entry also appeared in Progress's calories/protein meal list.
  This passes the named save/reopen and cross-screen label check for that meal;
  nutrition-value accuracy and partial-coverage persistence, cancellation,
  keyboard/rotation accessibility, and thermal behavior remain unverified.

### Saved partial-estimate resave guard — 2026-09-30

- A read-only path audit found that the first reviewed Save preserves a partial
  catalog estimate, and vault reload and Progress display it, but the saved-meal
  editor could recalculate it as full on a later Save when every retained food
  row had a catalog match. Foods omitted from the photo proposal are not saved
  as rows, so that recalculation could overstate coverage.
- The editor now conservatively retains an existing partial catalog coverage
  flag when rebuilding nutrition. Missing current matches still produce partial
  coverage; no catalog totals still produce no estimate. Nutrition values and
  the encrypted vault format are unchanged. Four focused policy cases cover
  these outcomes. Independent review found no material defect in the fix.
- The full Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  242 tests, 0 failures, 0 errors, and debug APK assembled. No installation
  or physical acceptance is claimed for this build.
- Next bounded task: after explicit in-place install approval, have the member
  save a photo review that says suggestions were omitted, verify the Partial
  estimate marker in Progress, open Edit and Save without filling the missing
  foods, then confirm the marker remains. Compare the visible nutrition values
  with the reviewed foods; broader repeated-meal and thermal gates remain open.

### Saved partial-estimate guard installed in place — 2026-09-30

- After member approval and the 242-test Android gate, selected one current ADB
  transport for the Samsung SM-S928B (Android 16 / API 36). `adb install -r`
  returned `Success`; force-stop and `am start -W` returned `Status: ok` and
  `LaunchState: COLD`. No data clear, uninstall, signing-key replacement,
  vault/model deletion, or biometric automation was performed.
- Following the requested unchanged Edit/Save check, the member reported that
  "Partial estimate" still appears in Progress. This passes that specific
  member-observed resave-label check; it does not establish nutrition-value
  accuracy or reliability across other meals. Existing-record preservation was
  requested but not separately confirmed in this exchange.
- Next bounded task: compare the saved estimate with the foods and portions the
  member actually confirmed, then exercise a small varied photo set on cool
  and warm starts without treating one correct proposal as daily-use acceptance.

### Saved partial-meal nutrition spot check — 2026-09-30

- For the same saved partial meal, the member compared its food names, portions,
  nutrition matches, and displayed calories/protein in the editor and Progress,
  then reported that they match after an unchanged Save. The Partial estimate
  label also remained visible. This is a member-observed consistency check for
  one entry, not independent validation of the food database values or broad
  photo-recognition accuracy. No private values or screenshots were collected.
- Next bounded task: establish a consented varied-photo baseline, separating
  novel from repeat meals and malformed from visually wrong drafts, before
  claiming several-meals-per-day reliability or changing suggestion ranking.

### Varied-photo pilot protocol prepared — 2026-09-30

- `docs/vision-benchmark-protocol.md` now defines a member-led first pilot:
  about eight novel-meal photos and four newly photographed repeat meals across
  cool and consecutive warm runs. It separates technical rejection from valid
  but visually wrong drafts, tracks missed/unsupported foods and review effort,
  and uses existing metadata audit and standard ADB device metrics. It keeps
  photos, meal identities, model text, and record values out of the repository.
- This is a protocol only. No test set has been selected or consented to, no
  varied-photo runs or thermal measurements were performed, and no daily-use
  reliability gate is marked passed. The installed APK is unchanged; the last
  Android gate remains 242 tests with debug assembly successful.
- Next bounded task: ask the member to select a small first set of photos and
  approve a session, then run and score one cool-device photo before attempting
  consecutive warm analyses. No raw debug capture read without separate
  active-run authorization.

### Three-dish same-photo retest — 2026-09-30

- The member reanalysed a three-dish photo that had failed the prior day and
  reported an accurate current draft. This is an exact-photo retest, not a
  novel or newly photographed repeat result in the varied-photo pilot. No
  duration, thermal measurement, or raw capture was collected.
- The member clarified that the remembered-dish suggestion was correct for the
  second dish and was explicitly accepted; the saved entry contained the
  correct dish. The confusion was visual placement: the suggestion card sat
  between the first and second dish cards, making its target unclear. Code
  inspection confirms the suggestion card is rendered *before* its associated
  dish card. This is a presentation issue, not evidence of a false match or
  automatic application. Do not count this exact-photo retest as varied-photo
  reliability.
- Next bounded task: place each suggestion after its associated dish card and
  name the target dish explicitly; verify the three-dish reading order on the
  phone after the full Android gate and an approved in-place install.

### Saved-dish suggestion target clarified — 2026-09-30

- Meal review now places each remembered-dish suggestion directly after its
  associated dish card and labels it "Suggestion for [dish name]" as an
  accessible heading. Accept/reject still targets the same dish ID; retrieval
  scoring, whole-photo fallback, and persisted meal behavior are unchanged.
  The screen contract now requires an unambiguous target and reading order.
- The full Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  242 tests, 0 failures, 0 errors, and debug APK assembled. A focused review
  mode test passed. No Compose UI test infrastructure currently checks the
  visual order. `git diff --check` passed.
- The APK has not been installed. Physical acceptance remains pending for the
  three-dish review, keyboard/scroll and enlarged-text layout, and verifying
  the correct dish is still saved only after explicit acceptance.
- Next bounded task: request in-place installation approval, then have the
  member reanalyse the same photo once on a cool phone and confirm the
  suggestion follows the fried-items card with its target label. Do not count
  that same-photo run toward novel/repeat-new-photo reliability.

### Suggestion-placement APK installed in place — 2026-09-30

- After explicit member approval and the 242-test gate, selected one current
  ADB transport for the Samsung SM-S928B (Android 16 / API 36). `adb install -r`
  returned `Success`; force-stop and `am start -W` returned `Status: ok` and
  `LaunchState: COLD`. No data clear, uninstall, signing change, vault/model
  removal, or biometric automation occurred.
- Installation and launch are confirmed only. The member still needs to unlock,
  verify existing records, and check the three-dish suggestion placement on the
  phone. No physical visual/accessibility acceptance is claimed yet.
- After the requested member-led three-dish review, the member reported the
  suggestion placement "fixed." This passes the targeted visual association
  check for that layout on this device. The reply did not separately confirm
  existing-record preservation, enlarged text, TalkBack, rotation, or wider
  meal-photo reliability. The same-photo rerun is not counted as a novel or
  newly photographed repeat benchmark sample.
- Next bounded task: begin the consented varied-photo pilot with a genuinely
  new meal photo, tracking visual correctness and suggestion relevance
  separately. Avoid repeated hot-phone analysis.

### Visible meal-review exit and edited-draft guard — 2026-09-30

- On the installed Samsung SM-S928B (Android 16 / API 36), authorized compact
  layout and one temporary visual inspection showed Save reviewed meal as the
  only labelled bottom action; the header had only a Back icon. The temporary
  screenshot was removed from the development machine and not saved in the
  repository. Code review confirmed Back closed review without a meal write,
  but the route for leaving without saving was not visible at the action area.
- Meal review now has a persistent secondary **Leave without saving** action.
  An untouched or fully reverted draft exits immediately; save-relevant edits
  prompt **Leave without saving?** with **Keep reviewing** and **Leave without
  saving**. Header Back and Android Back use the same guard; keyboard Back
  still hides the keyboard first, and dish-editor Back still closes that editor.
  Confirmed leave uses the existing no-save dismiss path. Initial draft state
  and the leave dialog survive configuration recreation. A code review caught
  and corrected an initial dialog-wiring defect before the full gate; a second
  independent review found no remaining material defect.
- Seven focused policy tests cover clean, edited, reverted, individual
  save-relevant fields, and baseline save/restore. The full Android gate from
  `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  246 tests, 0 failures, 0 errors, and debug APK assembled. No Compose UI-test
  infrastructure currently verifies actual button/Back interactions.
- The APK has not been installed. Physical checks still needed: clean leave,
  edited leave/Keep reviewing/confirmed leave, no new meal or learning after
  leaving, bottom actions with keyboard and three-button navigation, rotation,
  and enlarged text. Existing-record preservation remains to be confirmed
  after any in-place install.
- Next bounded task: request explicit in-place install approval, then have the
  member exercise the leave paths on one review without saving a duplicate meal.

### Visible review-exit APK installed in place — 2026-10-01

- After explicit member approval and the 246-test Android gate, selected a
  current ADB transport for the Samsung SM-S928B (Android 16 / API 36).
  `adb install -r` returned `Success`; force-stop and `am start -W` returned
  `Status: ok` and `LaunchState: COLD`. No data clear, uninstall, signing
  change, vault/model removal, or biometric automation occurred.
- Installation and launch are confirmed only. The member still needs to unlock,
  verify existing records, and exercise clean leave, edited leave/Keep
  reviewing/confirmed leave, and bottom actions with keyboard/system bars.
  Rotation and enlarged-text checks remain open. No physical acceptance is
  claimed from the automated gate or installation.
- Next bounded task: ask the member to verify the visible leave action and
  confirmation on one review without saving a duplicate meal, then record the
  observed device outcome.

### Grouped sauce proposal recovery — 2026-10-01

- With explicit member authorization for this active debug run, inspected the
  opt-in image-development capture through local ADB. The sanitized outcome
  was `draft_partial` / `invalid_group`: a visible sauce was proposed as a
  separately grouped ingredient, but its group kind was outside the parser's
  accepted vocabulary. Other valid foods reached review. The temporary
  reduced photo on the development machine was removed; no raw response,
  photo, or member record was placed in the repository.
- The parser now accepts only the additional explicit `sauce` group kind and
  still validates each nested ingredient normally. Two focused tests cover a
  valid sauce with no omissions and a malformed sauce ingredient that remains
  visibly partial. Unknown group kinds remain rejected.
- Full Android gate from `native-android` passed:
  `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`;
  248 tests, 0 failures, 0 errors, and debug APK assembled. This build is not
  installed. Physical confirmation of the grouped-sauce review and nutrition
  coverage is pending; prior member feedback does not validate this new APK.
- Next bounded task: request in-place installation approval, then have the
  member analyse a sauce-containing meal once on a cool phone and confirm the
  sauce appears for review without a false omitted-suggestion warning.

### Grouped-sauce APK installed in place — 2026-10-01

- After explicit member approval and the 248-test gate, selected a current
  ADB transport for the Samsung SM-S928B (Android 16 / API 36). `adb install -r`
  returned `Success`; force-stop and `am start -W` returned `Status: ok` and
  `LaunchState: COLD`. No data clear, uninstall, signing change, vault/model
  removal, or biometric automation occurred.
- Installation and launch are confirmed only. The member still needs to unlock,
  verify existing records, and test a visible-sauce analysis. No physical
  acceptance of the parser change is claimed yet.
- Next bounded task: ask the member to run one sauce-containing photo on a
  cool phone and report whether the sauce is visible in review and the
  omitted-suggestion warning is absent; do not save an unwanted duplicate.

### Grouped-sauce physical check — 2026-10-01

- On the installed Samsung SM-S928B (Android 16 / API 36), the member analysed
  a sauce-containing meal and reported that the creamy sauce appeared in review
  with no omitted-suggestion warning. This passes the targeted grouped-sauce
  presentation check for that photo and installed build.
- The member did not report a save/reopen, nutrition-total comparison, existing
  record inventory, or accessibility/rotation check in this turn. Those gates
  remain open; the single photo does not establish broader vision reliability.
- Next bounded task: finish the meal-review leave-without-saving physical
  checks, then continue the consented varied-photo pilot with genuinely new
  photos and separately track detection, partial coverage, and saved values.

### Explicit unsaved meal-draft discard — 2026-10-01

- The member confirmed that leaving review should keep the original proposal,
  but asked for a way to remove it without killing Metria. Photo and text meal
  proposal cards now have a secondary **Discard draft** action. A short
  confirmation distinguishes it from **Leave without saving**: **Keep draft**
  preserves the proposal; confirmed discard clears only that unsaved proposal
  (and its unsaved photo thumbnail for a photo draft). Saved meals, the vault,
  and visible conversation are unchanged. The controls use 48 dp targets.
- The final Android unit-test/build gate passed with 248 tests, 0 failures,
  0 errors, and a debug APK. `git diff --check` passed. No Compose UI-test
  source set exists, and this APK has not been installed; no physical
  acceptance is claimed.
- Next bounded task: after an approved in-place install, verify Keep draft and
  confirmed Discard draft on a photo proposal and a text proposal, check that
  saved meals and the visible chat remain, and exercise keyboard, Back,
  rotation, enlarged text, and screen-reader reading order.

### Unsaved-draft discard APK installed in place — 2026-10-01

- After explicit member approval and the 248-test Android gate, selected one
  current ADB transport for the Samsung SM-S928B (Android 16 / API 36).
  `adb install -r` returned `Success`; force-stop and `am start -W` returned
  `Status: ok` and `LaunchState: COLD`. No data clear, uninstall, signing-key
  change, vault/model removal, or biometric automation was performed.
- Installation and launch only are confirmed. The member still needs to unlock,
  confirm existing records, and test Keep draft and confirmed Discard draft on
  an unsaved proposal. No physical interaction or accessibility acceptance is
  claimed yet.

### Unsaved-draft discard member check — 2026-10-02

- On the installed Samsung SM-S928B (Android 16 / API 36), the member reported
  that the requested discard interaction worked. This is a targeted,
  member-observed phone check. The reply did not distinguish photo from text
  proposal or separately confirm both choices in the dialog, existing-record
  preservation, keyboard and Back behavior, rotation, enlarged text, or
  TalkBack. Those checks remain open; automated results alone are not physical
  acceptance for them.
- Next bounded task: begin the consented varied-photo pilot with new photos,
  tracking visual correctness, omissions, correction effort, duration, and
  thermal behavior separately. Do not read raw capture without authorization
  for that diagnostic run.

### General food question corrective retry — 2026-10-02

- The member confirmed the first recorded-meal question worked on the installed
  build, but the subsequent general rice/protein question was filtered. Authorized
  phone inspection showed the latest in-memory response acknowledging internal
  tool/record instructions instead of answering the question. The factual audit
  rejected its personal-record wording. The initial pre-retry response was not
  retained, so its precise failure is unverified. Temporary screenshots were deleted.
- The no-evidence retry now includes the original bounded question as quoted,
  untrusted content and asks for a direct answer, without acknowledging internal
  corrections. Prompts explicitly permit qualitative general food education in
  the member's language while reserving personal-record claims for approved local
  reads. Audit acceptance rules, privacy, and meal review requirements are unchanged.
- The complete `:app:testDebugUnitTest :app:assembleDebug --no-daemon` gate
  passed: 274 tests, 0 failures, 0 errors, and a debug APK. Regression tests cover
  original-question preservation, the separate evidence-backed retry, a permitted
  qualitative answer, and continued rejection of unsupported personal claims.
  This newest APK is not installed; physical acceptance remains pending.
- Next bounded task: approved in-place installation, member unlock and record
  preservation check, then repeat the recorded-meal question followed by the
  general food question. Confirm both answer the actual question in Spanish.
  Broader Gates B–E remain open.

### General food retry APK installed — 2026-10-02

- After explicit member approval and the 274-test complete Android gate,
  installed the debug APK in place on the Samsung SM-S928B (Android 16 / API 36).
  `adb install -r` returned `Success`; force-stop and `am start -W` returned
  `Status: ok` and `LaunchState: COLD`.
- No data clear, uninstall, signing change, vault/model removal, or biometric
  automation occurred. Installation and launch are confirmed; the member must
  unlock and confirm existing records and both question results. Physical
  acceptance of this correction remains pending.

### General food retry member check — 2026-10-02

- After the approved in-place installation of the 274-test build on the
  Samsung SM-S928B (Android 16 / API 36), the member reported that the requested
  two-question sequence worked: today's recorded meals followed by the general
  rice/protein question. This is a targeted member-observed phone pass.
- The reply did not separately confirm existing-record inventory, exact answer
  wording, offline mode, restart, accessibility, or cancellation. Those checks
  and broader Gates B–E remain open.
- Next bounded task: test a context-dependent follow-up after a general food
  answer and then switch back to a personal-record question, verifying that the
  latter performs a fresh local read and uses the requested date.

### Food follow-up and personal-record context switch member check — 2026-10-02

- On the installed 274-test build, the member reported that the requested
  sequence worked: the lentil follow-up after the rice answer, followed by
  yesterday's recorded-meal question. This passes the targeted member-observed
  follow-up and context-switch check on the Samsung SM-S928B (Android 16 / API 36).
- No diagnostic trace was inspected for this turn, so a fresh tool invocation
  is not independently verified. Exact wording, empty-day behavior, cancellation,
  offline use, restart, and broader Gates B–E remain open.
- Next bounded task: exercise a recorded-meal question for a known empty day
  and confirm the answer explicitly reports no saved meals for that date.

### Empty-day meal history member check — 2026-10-02

- The member reported that the requested known-empty-day meal-history check
  worked on the installed 274-test build on the Samsung SM-S928B (Android 16 /
  API 36). This is a targeted member-observed pass for the empty result state.
- The exact date and answer were not independently inspected. This does not
  establish offline operation, cancellation, restart behavior, or completion
  of broader Gates B–E.
- Next bounded task: repeat a general food question and a saved-meal question
  with airplane mode enabled and Wi-Fi disabled, then restore connectivity.

### Offline assistant member check — 2026-10-02

- The member reported that both requested questions worked during the offline
  check (airplane mode enabled and Wi-Fi disabled): general food education and
  today's saved-meal question, on the installed 274-test build on the Samsung
  SM-S928B (Android 16 / API 36). This passes targeted member-observed offline
  functionality for those two paths.
- No network instrumentation was collected; this functional check does not
  independently establish the full privacy gate. Connectivity restoration,
  cancellation, restart, repeated-run performance, and broader Gates B–E are
  not confirmed by this reply.
- Next bounded task: cancel one answer while thinking, verify no late reply or
  action appears, and ask a new question to confirm the chat remains usable.

### Cancellation recovery and latest-meal nutrition read — 2026-10-02

- The member could continue chatting after Stop; its transient confirmation
  disappeared when the next question began. That is the existing presentation
  policy, now documented explicitly. The next latest-meal calorie question was
  discarded. Metadata inspection showed cancellation followed by a new request
  rejected for unsupported personal facts, without a read-tool event. The latest
  raw model response was not inspected, so its wording is unverified. No late
  result from the cancelled request appears in that trace; this does not prove
  every cancellation race is resolved.
- Added the no-argument native `read_latest_meal` read and bilingual required-tool
  routing for explicit latest-meal nutrition questions. The host selects one meal
  by recorded date/time within day offsets 0–90, excludes future dates, and
  returns only its title/date and saved display-rounded calorie/protein estimates.
  Tied latest times or multiple latest-day meals with an unknown time produce a
  clarification. Daily totals, chat history, goals, photos, notes, and model
  nutrition calculations cannot supply this answer. No vault write or dependency
  was added.
- Host-composed results validate source, search bounds, selected date, language,
  value ranges, and coverage, then pass the factual audit. Missing values remain
  missing, partial estimates are labelled, and an empty bounded history is explicit.
  Added typed latest-meal nutrition evidence without relaxing existing audits.
- Final complete Android gate passed: 280 tests, 0 failures, 0 errors, and a
  debug APK. Six new tests exercise selection versus day totals, rounding without
  storage changes, metric binding, old/future exclusion, empty/missing/partial
  states, ambiguous times, malformed results, and bilingual intent boundaries.
  `git diff --check` passed. The newest APK is not installed; physical acceptance
  remains pending and broader Gates B–E remain open.
- Next bounded task: approved in-place installation; member compares latest-meal
  estimates with its saved entry, then repeats Stop followed by that question and
  checks that no late cancelled reply appears.

### Latest-meal nutrition APK installed — 2026-10-02

- After explicit member approval and the complete 280-test Android gate,
  installed the debug APK in place on the Samsung SM-S928B (Android 16 / API 36).
  `adb install -r` returned `Success`; force-stop and `am start -W` returned
  `Status: ok` and `LaunchState: COLD`.
- No data clear, uninstall, signing change, vault/model removal, or biometric
  automation occurred. Installation and launch are confirmed; existing-record
  inventory and latest-meal/cancellation physical acceptance await member checks.

### Latest-meal language contract failure and correction — 2026-10-02

- The member reported the installed latest-meal test failed. Authorized metadata
  inspection confirmed `read_latest_meal` was selected and continued, followed by
  host-result rejection. Screen inspection confirmed the meal-answer verification
  error. The temporary screenshot was kept outside the repository and deleted.
- Found a reproducible integration mismatch: the old shared language helper
  omitted the member's Spanish calorie/latest-meal wording and could return
  “the member's language”, while the host answer validator accepts only concrete
  Spanish/English values. The trace does not contain the rejected payload, so
  this cause is supported by the caller/validator reproduction rather than a
  captured runtime field value. Unit fixtures had supplied supported languages
  directly and missed this caller boundary.
- Extracted shared language selection outside UI code; it recognizes accented
  and unaccented Spanish, returns supported concrete values, prioritizes the
  current question, and uses recent member context for neutral follow-ups.
  Added an end-to-end host-result test using the exact reported question, plus
  bilingual/context-switch tests. No factual audit rule or vault storage changed.
- Complete Android gate passed: 283 tests, 0 failures, 0 errors, and a debug APK.
  `git diff --check` passed. This APK is not installed and no physical pass is
  claimed. Next bounded task: approved in-place install, then the exact Spanish
  latest-meal calorie question and cancellation/retry sequence against the saved
  entry. Broader Gates B–E remain open.

### Language correction installed; phone cooldown — 2026-10-02

- With explicit member approval, installed the 283-test debug APK in place on
  the Samsung SM-S928B (Android 16 / API 36). `adb install -r` returned `Success`.
  Metria was force-stopped and left closed for the member's requested cooldown;
  no inference or additional phone test was started.
- No data clear, uninstall, signing change, vault/model removal, or biometric
  automation occurred. Launch and physical acceptance of this newest build are
  pending. Resume with the exact Spanish latest-meal question after cooldown,
  comparing the answer with the saved entry; then test cancellation recovery.

### Latest-meal language correction member pass — 2026-10-02

- The member reported the latest-meal question worked after installing the
  283-test language correction build on the Samsung SM-S928B (Android 16 /
  API 36). This is a targeted member-observed pass. Cancellation recovery,
  exact saved-value comparison, and broader Gates B–E are not separately
  confirmed by that reply.
- The member requested a phone cooldown, authorized a local commit, and asked
  to begin Nebius work that can be developed and tested without the phone.
  Next bounded task: optional-cloud request/consent contracts and synthetic
  desktop tests. No real-data transfer or deployment is enabled by this step.

### MiniLM yesterday lookup installed — 2026-10-11

- Following checkpoint `2efa900`, extended the existing debug-only MiniLM meal
  lookup to explicit English yesterday questions. The host freezes the reference
  day, applies offset 0/1 once and validates source, exact date, bounded coverage
  and the deterministic reply before publication. Nutrition, advice, writes,
  mixed questions and other dates keep the ordinary assistant path.
- Custom explorer, worker and reviewer handled scope, implementation and
  independent review. No material review finding remains. Complete Android
  gate passed: 454 tests, zero failures/errors/skips, debug APK assembly and
  release Kotlin compilation. `git diff --check` passed.
- Existing authorized in-place Chat cycle: `adb install -r` returned `Success`
  on Samsung SM-S928B / Android 16; launch returned `Status: ok`. No data clear,
  uninstall, signing change, vault/model deletion or biometric automation.
  The member compared yesterday's reply with retained records and confirmed
  “Reply matches; MiniLM yesterday route shown.” This is the named yesterday
  physical pass; no inference benchmark or broader gate completion is claimed.
- The member also confirmed today's meal lookup remained correct with MiniLM,
  and the two-days-ago lookup was correct through the normal route. Empty,
  cancellation/relock and missing/corrupt-model checks remain open.
- Details, APK hash and next bounded checks are in
  [MiniLM integration](minilm-chat-integration.md#yesterday-lookup-expansion--2026-10-11).

### MiniLM latest-meal estimates installed — 2026-10-12

- Extended the debug fast route to explicit English calorie/protein questions
  about the latest recorded meal, including polite paraphrases. Separate MiniLM
  intent prototypes compete with each other and fallback examples; the host
  independently checks scope. The existing deterministic latest-meal reply keeps
  saved estimates, full/partial/missing coverage, ambiguous-time handling and
  factual audit. No new nutrition calculation, write, payload or dependency.
- Shared pure response validation freezes the reference date for latest reads
  and correctly validates target date plus offset for today/yesterday lists.
  The lifecycle uses that same validator covered by the tests. Independent
  review found no remaining material issue.
- Complete Android gate passed: 457 tests, zero failures/errors/skips, debug
  assembly and release Kotlin compilation; `git diff --check` passed. A concurrent
  focused Gradle run was stopped after an incremental-cache error; compiler
  fallback completed the full gate. Details and APK hash are in
  [MiniLM integration](minilm-chat-integration.md#latest-meal-estimate-expansion--2026-10-12).
- Within the already approved in-place Chat cycle, install returned `Success`
  on Samsung SM-S928B / Android 16 and launch returned `Status: ok`. No data
  clear, uninstall, signing change, vault/model deletion or biometric automation.
  The member reported “Matches saved meal; no MiniLM latest route.” Saved-record
  correctness is confirmed, but the fast-route gate failed. Existing content-free
  event metadata confirms native tool selection and continuation; the router's
  reason for declining remains unknown. Next bounded work: ephemeral debug
  decision outcome, then retry the same in-app phrase without loosening thresholds.
  No new benchmark or broader milestone acceptance is claimed.

### Latest-meal fallback diagnostics installed — 2026-10-12

- The existing content-free event trace confirmed the reported latest-meal
  answer used Gemma's native tool path. The exact requested polite phrase passes
  host-policy tests; the prior boolean router concealed the reason for declining.
- Added ephemeral debug-only typed outcomes and finite similarity scores, kept
  separate from Gemma's timing string. No raw question, answer, record, embedding,
  exception text or new persisted trace is added. Similarity/margin cutoffs remain
  unchanged. Reset boundaries cover new turns, cancellation, lock, explicit
  conversation clear, unload and idle style changes.
- Full gate passed: 460 tests, zero failures/errors/skips; debug assembly and
  release compilation. Review's stale diagnostic reset findings were fixed;
  parent checked the correction and `git diff --check` passed.
- Retry build installed in place under existing approval; `install -r` returned
  `Success` on Samsung SM-S928B / Android 16. The member was asked to report only
  the MiniLM route outcome for the same question. ADB lost its connection during
  launch, so the member opened Metria manually.
- On retry, the member reported an instant answer similar to the previous one
  and confirmed `Assistant route: MiniLM latest-meal read; Gemma generation skipped.`
  This is a targeted latest-meal fast-route pass on the retry build. The initial
  fallback cause remains unknown; thresholds/scope were unchanged. Broader
  reliability, protein, partial/missing/empty/ambiguous and lifecycle checks
  remain open; no measured latency or milestone completion is claimed.

### MiniLM today's recorded nutrition installed — 2026-10-12

- Added a debug fast route for explicit English questions about one recorded
  calorie/protein total today. Matching today summary scope is narrowed to the
  existing exact-day offset-zero result, with frozen date, no goals and unchanged
  calculations. Strict source/date/metric/language/count/value/interpretation
  checks precede evidence and deterministic reply publication. Partial/missing
  estimates and unknown unrecorded meals remain explicit.
- Independent review corrected date scope, validation order, competing intent
  coverage and aggregate/subtotal validation. Decimal rounding checks preserve
  legitimate separately rounded values. Final re-review found no material issue.
- Complete gate passed: 464 tests, zero failures/errors/skips, debug assembly and
  release Kotlin compilation. `git diff --check` passed. Initial fixture failure
  was corrected to follow the existing all-four-fields summary inclusion rule;
  calculation behavior remains unchanged. Details/hash in
  [MiniLM integration](minilm-chat-integration.md#todays-recorded-nutrition-expansion--2026-10-12).
- Approved ongoing in-place cycle: `install -r` returned `Success` on Samsung
  SM-S928B / Android 16; launch returned `Status: ok`. No data clear, uninstall,
  signing replacement, vault/model deletion, biometric automation or private
  record inspection. The member's calorie/protein comparison and route checks
  were partly reported: calories used MiniLM, protein used Gemma. Calorie route
  activation is confirmed; numeric comparison was not separately reported and
  protein fast-route acceptance remains open. Next bounded task: inspect the
  content-free protein outcome/scores from Developer diagnostics before changing
  the router. No benchmark or milestone completion is claimed.

### Retained route monitoring installed — 2026-10-12

- The member requested routing decisions in existing monitoring instead of
  manually copying diagnostics. Extended the existing opt-in content-free trace
  with MiniLM decision/rejection outcomes and bounded finite cosine scores,
  native/legacy Gemma selection and host replies. Eight-column legacy rows are
  preserved; only route decisions may append three score fields. No question,
  answer, record, embedding or exception text is stored. Existing retention,
  release no-op, deletion and archive exclusion remain.
- Complete gate passed: 466 tests, zero failures/errors/skips; debug assembly
  and release compilation. Score parsing, normalization and restart compatibility
  are covered. Independent re-review found no issue; `git diff --check` passed.
  Details/hash in [MiniLM integration](minilm-chat-integration.md#retained-routing-monitoring--2026-10-12).
- Approved in-place installation succeeded on Samsung SM-S928B / Android 16;
  launch returned `Status: ok`. No data clear, uninstall, signing replacement,
  vault/model deletion or biometric automation. Member unlock and one protein
  question were requested; diagnosis will use the retained technical events.
  The member confirmed the named protein question answered. Direct content-free
  trace inspection confirmed MiniLM acceptance/selection, finite scores and
  an accepted host reply audit. Old rows and the new scored row were readable.
  This verifies monitoring and that named protein route; numeric comparison,
  broader acceptance and the original fallback cause remain unconfirmed.

### Natural read phrasing and metric follow-ups installed — 2026-10-12

- Broadened full-match English phrasing for the existing read operations. Short
  calories/protein follow-ups consume a same-day, unlocked, transient scope from
  a validated MiniLM nutrition/latest-meal result and perform a fresh read.
  No earlier values or model prose become evidence; unrelated requests and
  lifecycle boundaries revoke that scope. Selected-meal, mixed and unsupported
  requests retain normal handling.
- Complete gate passed: 468 tests, zero failures/errors/skips, debug assembly and
  release Kotlin compilation. Independent review found no material issue;
  `git diff --check` passed. Details/hash in
  [MiniLM integration](minilm-chat-integration.md#natural-phrasing-and-metric-follow-ups--2026-10-12).
- Approved in-place installation succeeded on Samsung SM-S928B / Android 16;
  launch returned `Status: ok`. No data clear, uninstall, signing replacement,
  vault/model deletion or biometric automation. The member confirmed natural
  day calories, immediate protein follow-up and natural meal lookup all matched
  their records. Direct content-free trace inspection confirmed MiniLM selection
  and accepted host audits for all three, with fresh day/day/meal reads and no
  Gemma generation. A later post-unlock short follow-up was confirmed from
  retained events: MiniLM scope rejection, native Gemma selection, fresh today's
  nutrition read and accepted reply audit. The member reported completion but
  did not separately compare its numbers. The reported topic-change sequence
  has no matching retained turn evidence; topic-change/restart/date-rollover and
  other rejection checks remain open. Combined checkpoint review found no
  material gap. Next bounded task: obtain explicit approval to commit/push this
  reviewed checkpoint, then verify the remaining follow-up boundary cases.

### Bounded weight read shortcuts — 2026-10-12

- The previous MiniLM checkpoint was committed/pushed with member approval as
  `3545c74`; its remaining boundary gates are still open. The member approved
  latest saved weight, last weigh-in date and recent weigh-in shortcuts after
  an error/no answer to the date question. Existing content-free trace showed
  a compatible native turn rejected for a personal fact without a read; it
  cannot independently identify the question text.
- New strict English weight policy reuses `read_weight` with latest/date range
  90-days-ago through today, or recent 30 calendar days, and at most 24 rows.
  Fresh validated replies state kilograms, recorded dates, exact window and
  omissions. Empty windows imply no global absence. Weight answers neither
  grant writes nor seed nutrition follow-up scope. Eligible native fallback
  retains its required weight read; original dated-weight requirements remain.
- Complete gate passed: 473 tests, zero failures/errors/skips; debug assembly
  and release Kotlin compilation. Final independent review and `diff --check`
  passed after correcting dispatch/formatting/test issues without changing
  factual-auditor rules. Details/hash in
  [MiniLM integration](minilm-chat-integration.md#weight-read-shortcuts--2026-10-12).
- Approved in-place installation returned `Success` on Samsung SM-S928B /
  Android 16, then launch returned `Status: ok`, without data clear, uninstall,
  signing replacement, vault/model deletion or biometric automation. Member
  unlock and the three comparisons were requested. Named phone comparisons
  and empty/omitted/out-of-scope/lifecycle cases are pending. Next bounded task:
  inspect content-free route selections/reply audits after member completion.
  No benchmark, new dependency, vault representation or milestone completion
  is claimed.

- Initial member feedback: latest value matched and was fast; trace confirmed
  MiniLM weight selection and accepted host audit. Date question failed scope
  eligibility, fell back to Gemma without a weight read, and was rejected for an
  unsupported personal fact. Member clarification identified `weight myself`
  versus the supported `weigh myself` wording. Added that exact typo variant
  and regression coverage without relaxing thresholds or factual checks.
  Corrected complete gate passed again (473 tests, zero failures/errors/skips,
  debug assembly/release compilation), and independent review confirmed the
  literal-only scope change. Corrected in-place install and launch succeeded
  on the same Samsung SM-S928B / Android 16 with existing data preserved.
  Exact typo/date retry and recent-list phone acceptance were requested and
  remain pending alongside direct retained-route confirmation.

- Subsequent feedback identified recent wording with `resent wigh-ins`. Added
  contextual spelling normalization before the complete weight grammar and
  used that same normalized text for embeddings. Mixed/write/dated/selected
  requests stay excluded. Full gate passed: 474 tests, zero failures/errors/
  skips, debug assembly/release compilation, independent review and diff check.
  Earlier inspection was temporarily blocked by approval-service rate limiting;
  normal review later recovered. Trace confirmed the previous date retry used
  MiniLM and passed audit; recent-list attempt rejected scope and failed an
  unsupported Gemma tool. Normalized build installed/launched in place on the
  same phone with preservation rules. Recent-list retry and explicit member
  comparison of the date answer are pending; no broader gate is closed.

- Final member comparison confirmed recent records and the previous last-weigh-
  in date both matched saved records. Direct content-free monitoring confirmed
  the recent-list typo retry selected MiniLM `read_weight` and passed host audit,
  skipping Gemma generation. All three named weight cases now have member
  comparison and route evidence across the installed builds. Empty/omitted-row,
  out-of-scope, lifecycle and broader phrasing phone cases remain open. The
  474-test gate and review remain valid; no code changed after installation.
  Next bounded task: prepare the weight checkpoint for explicit commit/push
  approval, retaining these broader acceptance gaps.

### Broader wording ready for phone test — 2026-10-12

- Member approved wording tolerance across existing reads before more features.
  Added shared bounded normalization for polite wrappers and existing contextual
  spelling aliases, plus closed single-purpose weight/meal/latest-nutrition/day-
  total paraphrases. Host date/metric scope, qualifier exclusions, selected
  context, fresh reads, thresholds and factual-audit rules remain.
- First verification caught comma-before-please normalization; corrected without
  stripping request content. Independent review found asymmetric intent scoring
  from the earlier weight extension; all four intent groups now compete against
  the other three. Complete corrected gate passed: 476 tests, zero failures/
  errors/skips, debug assembly/release compilation, final review and diff check.
  Details/hash in [MiniLM integration](minilm-chat-integration.md#broader-read-wording--2026-10-12).
- Phone disconnected before installation; reconnection requested. No installed
  or physical pass is claimed. Next bounded task: approved in-place installation,
  member unlock and four representative paraphrases, then retained-route audit.
  Commit/push remains subject to explicit approval; preserve existing dirty
  weight work and all open broader acceptance gates.

- Reconnection completed and the member requested installation. `install -r`
  succeeded on Samsung SM-S928B / Android 16 and launch returned `Status: ok`
  with existing data preserved. Member unlock and four representative wording
  checks were requested; comparisons and retained route/audit evidence remain
  pending. No code changed after the 476-test gate and review.

### Chat message copy and failure visibility — 2026-10-12

- Member reported thinking disappearing with Chat still open and no answer,
  then requested message Copy. Retained events showed scope rejection/native
  Gemma/no record read/unsupported-personal-fact rejection. Exact question and
  installed APK were checked; a real-host regression accepts that phrase. The
  original runtime cause remains unresolved and no lost accepted reply is claimed.
- Added accessible 48dp completed-message options/Copy, including displayed
  errors and expanded evidence only. Plain text enters the sensitive-marked OS
  clipboard only by explicit action; no clipboard reads/content logs/network or
  hidden model/tool state. Error changes now trigger scrolling and polite live
  announcements. New closed metadata reasons distinguish wording/selection/
  host conflict/unresolved follow-up, without storing context IDs or text.
- Selected context still disables weight shortcuts; explicit weight native
  fallback now requires its own fresh bounded read instead of ungrounded prose.
  Host conflicts and validators remain. Complete gate passed: 479 tests, zero
  failures/errors/skips, debug assembly/release compilation, independent review
  and diff check. Details/hash in
  [MiniLM integration](minilm-chat-integration.md#chat-copy-and-visible-failures--2026-10-12).
- Installation, exact-question retry, Copy/paste and error-visibility acceptance
  are pending. Keyboard/font/TalkBack/rotation and before/after visual evidence
  remain open; no Compose-test dependencies were introduced. Next bounded task:
  approved in-place installation, member unlock/retry and Copy into composer
  without submitting, then inspect retained route/audit reasons.

- Approved in-place installation and launch succeeded on Samsung SM-S928B /
  Android 16 with preservation rules. Member unlock, exact polite weight retry
  and Copy/paste into the composer were requested. Physical results and retained
  route reasons remain pending; broader UI gates above stay open.

- Copy was member-confirmed working, but the member clarified native long-press
  word/range selection was intended. Replaced the visible menu with per-message
  native selection/toolbar and sensitive writes, leaving composer paste, saved-
  meal actions and record disclosure separate. The pasted actual failed question
  differed from the retyped one: `the latest wight` was outside grammar, explaining
  `scope_text`. Added that bounded wording/spelling and real-host/validated-reply
  regression with meaningful height/other-person/date/mixed/write exclusions.
  Gate, review and exact copied-question/native-selection phone retry pending.

- Corrected native-selection build passed the full 480-test gate (zero failures/
  errors/skips), debug assembly/release compilation, independent review and diff
  check. Approved in-place installation/launch succeeded on the same Samsung
  SM-S928B / Android 16 with preservation rules. Member exact copied-question
  retry and native word/range Copy/paste were requested; physical comparison,
  route confirmation and broader UI gates remain pending. No raw/private screen
  evidence was captured; redacted visual acceptance remains open.

- Member confirmed the exact copied latest-weight question answered correctly
  and native word/range selection, Copy and composer paste worked. Direct
  content-free trace confirmed MiniLM weight selection and accepted host audit,
  with Gemma generation skipped. The named reply failure/basic Copy acceptance
  is closed; error-display, TalkBack/font/rotation/lifecycle, broader wording
  and redacted visual gates remain open. No code changed after the 480-test gate.
  Next bounded task: review the accumulated checkpoint for explicit commit/push
  approval, retaining all broader acceptance gaps.

- Member repeated all four broader-wording questions and reported success and
  fast replies. Direct allowlisted monitoring confirmed MiniLM selections and
  accepted audits for weight, today's nutrition, today's meal list and latest-
  meal reads; all skipped Gemma generation and used fresh records. This closes
  the named operation retry set on the installed 480-test build. It is not a
  benchmark or proof of arbitrary English/typo coverage. Error-display,
  accessibility/visual/lifecycle and previously open rejection gates remain.

### Companion bundle installed for acceptance — 2026-10-12

- Implemented the member-approved five-feature set: recorded estimates against
  today's references, saved-dish recall, fresh date/metric follow-ups, explicit
  allowlisted navigation, and member-selected saved-dish meal review. Contract,
  scope and detailed open gates: [companion bundle](assistant-companion-bundle.md).
- Independent review fixes prevent wrong draft provenance and stale slot/date;
  tests caught a conflicting-day follow-up admission. Unknown portions remain
  blank, catalog release is preserved, and final review/Save is the only write.
  Monitoring adds fixed technical route reasons only.
- Full Android gate passed with 498 tests, zero failures/errors/skips, debug
  assembly and release compilation; diff check passed. APK hash is in the
  bundle note. One authorized `install -r` succeeded on Samsung SM-S928B /
  Android 16, preserving private storage; launch returned `Status: ok`.
- Member unlock, all five feature comparisons, native selection regression and
  relevant accessibility/visual/lifecycle gates remain pending. No physical pass
  is claimed. Next bounded task: goal comparison and fresh follow-ups in Chat,
  then recall/draft review and navigation; inspect content-free retained routes.

- First sequence exposed a historical follow-up handoff defect: MiniLM selected
  yesterday correctly, but the short question's selected metric did not reach
  the record lookup, so validation rejected the response. Fixed the trusted
  dated-read metric handoff and yesterday monitoring labels. Exact-sequence
  regression, independent review and full 499-test gate passed with zero
  failures/errors/skips plus debug/release compilation. Authorized in-place
  update and launch succeeded with preservation rules. Member retry remains
  pending; no historical phone acceptance is claimed.

- Member confirmed the corrected yesterday answer matches. Retained safe
  monitoring confirms both goal turns accepted and the yesterday dated read
  accepted/audited via MiniLM, skipping Gemma generation. Named handoff retry
  passed; other scope/lifecycle gates remain open. Continue with saved-dish
  recall, member-selected review/discard, then navigation and Chat return.

- Saved-dish test clarified recall stays in Chat and draft offers Review meal.
  Member reported a correct recall and an empty draft lookup whose query
  included a syntax word. Bounded wording extraction now handles optional
  `dish` after `my saved` and a recall name before trailing `dish`; exact
  host-query binding and member review remain enforced. Synthetic regressions,
  parent review, diff check and full 500-test/debug/release gate passed. Approved
  in-place update and launch succeeded with data preservation. Draft review,
  discard and named acceptance remain pending; no new phone pass is claimed.

- Member confirmed draft Review opens and discard returns to Chat with the
  transient offer cleared, but an answer-validation error appeared. Safe events
  confirm accepted selection/rejected reply. Deterministic wording now binds
  each saved-record claim to returned names; the auditor is unchanged. Positive
  generated-reply regressions plus malformed/fabricated-evidence rejection,
  parent review and full 501-test/debug/release gate passed. Authorized in-place
  update/launch succeeded with private storage preserved. Error-free retry and
  draft food/portion comparison remain open, followed by navigation acceptance.

- Member confirmed the corrected draft is error-free and matches saved foods/
  portions, then discarded it. Safe monitoring confirms accepted MiniLM draft
  selection and answer audit with Gemma generation skipped. Named review/
  discard retry passed; ambiguity/empty, save/reopen and broader UI gates stay
  open. Continue with explicit navigation and retained Chat transcript.

- Navigation opened History and native copy worked, but the member clarified
  both the Open question and opening reply were absent on return. Immediate
  navigation raced reply insertion and cancelled the delayed conversation save.
  Handoff now retains the full turn successfully before closing Chat, through
  a serialized session writer with early snapshot tickets, clear/lock/new-turn
  invalidation, acknowledged failure and stale-action checks. Independent
  review and full 509-test/debug/release gate passed with zero failures/errors/
  skips; diff check passed. Authorized in-place update/launch succeeded with
  existing storage preserved. Member complete-transcript retry remains pending;
  no navigation restoration acceptance is claimed. Details/hash in bundle note.

- Member confirmed the corrected navigation/complete-turn return worked.
  Safe events confirm MiniLM navigation selected. A contextual `weight hystory`
  typo also missed scope and Gemma rejected an unsupported tool. Added the
  bounded spelling alias with positive/polite and mixed/date/other-person/
  selection/host-conflict regressions. Parent review, diff check and complete
  510-test/debug/release gate passed with zero failures/errors/skips. Update
  installation/launch succeeded with private storage preserved. Exact typo
  acceptance remains pending. Broader spelling and
  lifecycle/visual gates are not claimed.

- Member confirmed the exact contextual navigation typo now works. Safe trace
  confirms accepted MiniLM navigation and Gemma skipped for that turn. Named
  History/complete-transcript/spelling acceptance is closed on the installed
  510-test build. The development checkpoint is ready for explicit commit/push
  approval, retaining all unexercised coverage/UI/lifecycle/write gates and
  saved-recall MiniLM acceptance. No code changed after the successful gate.

### Fast today overview installed for acceptance

- Member selected a fast “How am I doing today?” recap. A closed English
  today-only route now uses a frozen fresh progress snapshot and strict
  deterministic reply: recorded count, available calorie/protein estimates,
  current confirmed goals and coverage. It ignores weight/freeform text, adds
  no advice/judgment and does not seed an ambiguous overview follow-up scope.
  Existing auditor and normal grounded weekly/bilingual fallback remain.
- Independent review, diff check and full 521-test/debug/release gate passed
  with zero failures/errors/skips. Details, hash and open gates:
  [today overview](minilm-today-overview.md).
- Authorized in-place install and launch succeeded on Samsung SM-S928B /
  Android 16 with private data/model preservation. Member unlock, comparison
  and safe monitoring acceptance remain pending; no phone pass is claimed.
  Next bounded task: named today question, then paraphrase and existing read
  regression. No commit/push is authorized for this new slice yet.

- Member reported the named daily summary worked well on the installed
  521-test build. Recorded the comparison acceptance; direct retained-route
  verification remains open because ADB currently lists no connected phone.
  Next: alternate overview wording and an existing meal read. Other physical
  states remain open; no additional installation or code change was needed.

### Shared read recognition decision

- Member requested current-project research and approved shared typo-tolerant
  recognition. Research, decision, alternatives and staged route are recorded in
  [recognition decision](assistant-routing-recognition.md). The first slice
  keeps exact routes and retries one unambiguous routing-word correction through
  the complete existing read policy and competitive MiniLM check. No dependency,
  general dictionary download, member-text rewrite or new permission is added.
- Initial review identified height/weight meaning confusion and a blanket
  previous-follow-up-scope exclusion. These are being addressed with negative
  and conversational regressions before the complete gate and installation.
  Independent UI inspection found no proven missing-error defect; actual rejected
  turn feedback remains an open phone check, not a claimed UI repair.

- Shared typo implementation and independent review completed. The full gate
  passed all 524 tests with zero failures/errors/skips, debug assembly and
  release compilation; diff check clean. Authorized in-place install/launch
  succeeded on Samsung SM-S928B / Android 16, preserving existing private data
  and models. APK hash and limitations are in the recognition decision.
  Next bounded task: copied overview typo and a meal-read typo, then exact-route
  regression and rejected-turn feedback. No commit/push is approved yet.

- Member confirmed both shared-recognition phone questions returned matching
  answers. Safe metadata confirms accepted MiniLM today overview and today meal
  list, each with an accepted factual audit and Gemma generation skipped.
  Named typo acceptance is recorded in the recognition decision; original-route
  regression and the earlier rejected-turn feedback observation remain open.

- Member confirmed the original overview and today-meal questions still match
  after the shared typo update. The latest metadata verifies an additional
  accepted MiniLM meal read/audit; no separate new overview event appeared in
  that tail, so do not claim a new overview trace correlation. Named regression
  comparisons are recorded; rejected-turn feedback and broader physical gates
  remain open. The installed build still has 524 passing tests; no source change
  or new installation was needed for these comparisons.

### Completed Chat failure retention

- Member continued work on the rejected-answer flow. The confirmed retention
  gap is that diagnostic cards are transient while record evidence is saved.
  The original immediate-disappearance cause is still not established. Current
  terminal failures now become one host-authored assistant timeline message,
  using the same encrypted representation and reply-consumption request gate.
  Pending/stale/cancelled/tool/action results must not complete a failed turn.
- Parent review caught duplicate transient-error rendering and a transient
  TalkBack-announcement field affecting restored transcript equality. These are
  being corrected before the full gate and in-place installation. UI semantics,
  selection and normal conversation expiry remain; phone visibility/reopening
  and broader accessibility/lifecycle acceptance are separate open gates.

- Completed-failure retention passed independent review and the full gate:
  531 tests, zero failures/errors/skips, debug assembly and release compilation.
  Duplicate transient rendering is suppressed only for a consumed current
  terminal failure. Announcement state is ephemeral session state, excluded
  from transcript equality/serialization and cleared on Chat exit/new turn.
  Physical rejection visibility, reopen/copy and TalkBack remain unverified;
  the original disappearance cause is not claimed solved by automated checks.

- Authorized 531-test build installed in place and launched successfully on
  Samsung SM-S928B / Android 16 with existing data/models preserved. Next bounded
  check: unsupported-field read with visible reply/error, then close/reopen;
  safe trace must establish whether this exercised rejection. A normal generic
  reply cannot establish rejected-message retention acceptance.

- Member confirmed the unsupported-field turn displayed a message and kept it
  after closing/reopening Chat. Safe trace confirms native Gemma followed by
  `reply_rejected / unsupported_personal_fact`, without a record tool: this
  verifies the rejection path. Named failure visibility/reopen acceptance is
  closed on the 531-test build. TalkBack/new-error selection and broader
  lifecycle/visual states remain open. No source edit or reinstall was required
  for acceptance; no raw member reply or record was inspected.

### Approved development checkpoint

- On 2026-10-14 the member explicitly approved committing and pushing this
  combined checkpoint: fast daily overview, shared bounded read typo recognition,
  completed Chat failure retention, regressions and research/decision documents.
  Independent review and the complete 531-test/debug/release gate passed, and
  named phone checks are recorded above. No Android source changed after that
  successful gate; final documentation alignment and diff check are complete.
- This is a development checkpoint, not milestone/release completion. Preserve
  unexercised TalkBack, error selection, broader goal/navigation, keyboard/font/
  rotation, expiry and lock/process-restoration gates. Next bounded development
  slice is flexible read intent/slot recognition as specified in the researched
  route. The photo-context question was investigated only; the member deferred
  changes, and none are included here.

### Compositional read phrasing in development

- Member approved the next flexible-read slice after checkpoint `21a7296`.
  Read-only discovery identified the existing resolver as the integration seam:
  keep exact routes first, parse reusable read/date/metric components next and
  reuse existing host compatibility before scoring original wording with MiniLM.
  The scope and exclusions are in [recognition decision](assistant-routing-recognition.md).
- No meal-slot filtering is added. A breakfast-only request must not become an
  all-day answer. No new dependency, training, automatic write or photo change.
  One reviewed build is intended for the existing authorized in-place test cycle;
  automated and phone acceptance remain open. Current ADB preflight shows no
  connected phone; reconnect requested while development continues.
- Next bounded task: implement/review the parser and adversarial regressions,
  complete the Android gate, install once and compare one new phrasing per read
  family plus existing-route and ambiguous/unsupported-request behavior. No new
  commit or push is authorized by this development continuation.

- Implementation and independent review completed. Parent review tightened full
  ordered phrase consumption, weight-kind semantics and input bounds. Complete
  gate passed 539 tests with zero failures/errors/skips, debug assembly and
  release Kotlin compilation; diff check clean. Exact routes, thresholds, fresh
  record validation and factual audits remain. No new model/dependency or photo
  change. APK hash, known internal possessive-date coverage limit and named phone
  batch are in the recognition decision.
- Final ADB preflight still lists no device. Installation, comparisons and safe
  routing-event verification remain pending; the member has been asked to reconnect.
  Do not repeat implementation or claim previous phone checks cover this build.
  Next bounded task is install-r/launch on the freshly selected phone, member
  unlock, then named language checks and metadata-only route inspection.

- Member reconnected through wireless debugging and explicitly requested
  connection and installation. The checksum-matched 539-test APK installed with
  `install -r` (Success) on Samsung SM-S928B / Android 16; force-stop and cold
  launch returned `Status: ok`. Existing app storage was preserved. No raw
  records/screenshots, biometric interaction or destructive action was used.
  Next bounded task: member unlock and first meal/nutrition phrasing batch, then
  metadata-only route verification. Named physical acceptance is still open.

- First installed language batch is not accepted: member reported a failed turn
  with Gemma loading and no answer. Retained content-free events show one
  yesterday meal read accepted by MiniLM and audited (positive 0.7578, competing
  0.6301, fallback 0.6503). Two surrounding turns rejected before inference with
  `scope_text`; the earlier Gemma turn completed a latest-meal read/audit, while
  the final turn stopped at `tool_required / read_nutrition_summary / today`
  with no retained continuation or completion. Monitoring omits question text,
  so exact failed wording has been requested. Do not attribute this to a known
  grammar bug or mark all batch answers compared correctly. Next bounded task:
  copied-question reproduction plus read-only tool-handoff investigation, then
  a reviewed fix and complete gate if evidence establishes a defect.

- Member supplied the copied protein-total wording: initial `tday`, then `today`
  with trailing spaces. Added real-host/history/whitespace/selection regressions;
  the correct request resolves and the date typo intentionally abstains. No
  parser change or date alias is justified. Installed APK hash was verified
  against the reviewed artifact. Trace has no later turn-start event, so a
  blocked retry is consistent with the unfinished prior tool; it is not evidence
  of a second parser rejection for the corrected wording.
- A concrete tool-effect dependency gap was found: dispatch checked the active
  request ID without keying the effect to it. Added that dependency so late
  reconciliation retries dispatch, keeping duplicate/stale-request guards.
  This fixes a code ordering hazard but does not prove the observed failure's
  cause. An expired native continuation now records one fixed technical failure
  reason in the existing safe trace, without extending content capture. Full
  gate/review and named phone checks remain open for these follow-up changes.

- Follow-up independent review found no new stale/cancel or duplicate-dispatch
  issue. Complete gate passed all 542 tests, zero failures/errors/skips, debug
  assembly and release Kotlin compilation. Corrected-wording/history regressions
  and closed metadata persistence passed. Actual Compose scheduling and reaching
  the expired continuation guard are not covered by instrumentation. APK hash is
  in the recognition decision. In-place installation and corrected-request-first
  phone acceptance are next; no physical repair is claimed from code review.

- The reviewed 542-test follow-up APK installed in place (Success) and cold
  launched (`Status: ok`) on Samsung SM-S928B / Android 16, preserving existing
  app storage. Next: member unlock, send the corrected protein-total question
  first and inspect content-free routing; only afterward check the independent
  date-typo/Gemma handoff. No member records or screenshots were inspected.

### Production Android spelling extension in development

- Member explicitly requested production implementation of Android's selected
  spell checker after discussing provider/availability/offline limitations.
  This supersedes the proposed debug-only pilot restriction. Implementation will
  run only after existing recognition misses, on Send, with a bounded session and
  safe normal fallback. Only one locally identified possible misspelled routing
  word is passed; full questions, saved records and history remain excluded.
- Scope, authority checks, date-typo handling, privacy disclosure, API references
  and acceptance are in the recognition decision. Adapter belongs to main source;
  existing release MiniLM abstention is unchanged. No dependency or dictionary
  download. Parent owns docs and concise disclosure text/scrolling; worker owns
  the pure policy, service adapter, lifecycle integration and tests.
- Current 542-test build is installed, but its corrected-protein and handoff phone
  checks remain open. New spelling code requires its own independent review and
  complete gate before one in-place install. Do not attribute a future spelling
  success to the earlier unverified handoff repair or claim offline provider
  behavior from API availability. No commit/push is authorized.

- Production spelling implementation and independent review completed. Parent
  review tightened callback segment validation, exception-safe cancel/close,
  no-double-correction validation and explicit-request/migration behavior. The
  complete gate passed 550 tests, zero failures/errors/skips, debug assembly and
  release APK assembly with vital lint. The production adapter is main-source;
  release MiniLM remains separately disabled. Hash, command and named framework/
  provider/legacy/physical gaps are in the recognition decision. No dependencies
  or signing keys changed. In-place installation is proceeding under the existing
  authorized cycle; no real-provider result is claimed from fake-provider tests.

- The checksum-matched 550-test build installed in place (Success) and cold
  launched (`Status: ok`) on Samsung SM-S928B / Android 16. Existing vault/model
  storage was preserved; only debug APK was installed for existing safe route
  inspection, while both variants include the production adapter. Next: member
  unlock, correct protein-total question then `tday` variant in the same Chat,
  followed by metadata correlation. Provider behavior, actual framework session,
  failed-turn repair and disclosure/large-font/Stop/lock gates remain open.

- Member confirmed both named protein-total answers match on the 550-test build.
  Safe events show one MiniLM exact-day nutrition acceptance/audit and one
  `spell_check / rejected / no_suggestions` followed by native Gemma nutrition
  tool, continuation and accepted audit. Thus named functional comparison and
  observed fallback handoff pass; actual Samsung correction does not yet pass.
  The trace stores no text, and `no_suggestions` also covers unusable provider
  results, so do not claim a zero-alternative response or question-order mapping.
  Next bounded check is the copied `protteen` routing-word typo; no new build or
  installation is needed. Provider/offline, UI disclosure and broader lifecycle
  gates remain open. No raw records or outputs were inspected.

- The member confirmed the ordinary `protteen` test answered and matched.
  Safe trace again shows no usable spelling result and Gemma selection/audit;
  no fresh record-tool event is present for this turn. Provider correction is
  still unaccepted. Primary API/source inspection identified an API 31+ builder
  omission: default supported-attribute mask is zero, while our policy requires
  typo metadata. Added the same dictionary/typo/recommendation attribute mask
  supplied by the legacy overload. Independent review found no blocker; no
  acceptance rule or service input was widened. Full test/debug/release gate is
  running before a new in-place install and one copied typo check. Details and
  source references are in the recognition decision. No raw provider results or
  member values were captured.

- Attribute-mask repair passed independent review and the complete 550-test,
  debug/release APK and release vital-lint gate, with zero failures/errors/skips.
  No correction/privacy/host guard was relaxed. Hash is in the recognition
  decision. Authorized in-place installation is underway, then one repeat of the
  same ordinary word typo will distinguish successful provider correction from
  another accepted Gemma fallback. No new code changed after the passing gate.

- Checksum-matched attribute repair installed in place (Success) and cold launched
  (`Status: ok`) on Samsung SM-S928B / Android 16, preserving existing app storage.
  Next bounded task: member unlock and same copied `protteen` Chat request,
  followed by fixed spelling/route/audit event inspection. Provider acceptance
  remains open; no raw prompts, responses, records or suggestions were inspected.

- Attribute repair did not close provider acceptance: the copied `protteen`
  request still reports no usable spelling result and uses Gemma. User-tested
  `prottein` instead uses MiniLM with accepted audit and no system-spelling event,
  proving the local one-edit path only. Filtered OS service metadata advertises
  English; it does not prove useful corrections. Do not present this as Android
  correction success or weaken validation based on the similar words.
- Added only fixed spelling rejection categories to separate empty response,
  malformed segment, cookie/sequence mismatch, dictionary word, missing typo flag
  and no matching suggestion. No correction guard, service payload or privacy
  boundary changed. Independent review found no blocker. Policy/restart privacy
  tests added; complete Android unit/debug/release gate is running. Next task is
  one authorized install and repeated `protteen` turn to identify the failed check.

- Detailed safe spelling reasons passed independent review and the complete
  552-test/debug/release APK/release vital-lint gate, zero failures/errors/skips;
  diff check clean. Hash is in the recognition decision. Installation is underway
  under the active in-place test authorization. No matching safeguard, service
  payload or model threshold changed. Repeat `protteen` only after member unlock
  to identify the rejection stage; avoid further spelling patches without evidence.

- 2026-10-15 continuation: confirmed the diagnostic APK installation completed
  before interruption. On member-reconnected Samsung, installed SHA-256 matches
  the reviewed 552-test artifact; safe trace has no new diagnostic turn yet.
  Force-stop and cold launch returned `Status: ok`. No reinstall or code change,
  clearing, biometric automation or private-content capture occurred. Next
  bounded task is member unlock and copied `protteen` question to collect the
  detailed fixed rejection reason. Actual system correction remains unaccepted.

- 2026-10-15 repeated diagnostic check: Samsung returns `dictionary_word` after
  request/segment validation. Dictionary-word rejection remains unchanged.
  Gemma selection and accepted reply audit have no fresh record-tool event;
  member reports no answer bubble. Visible fallback therefore failed and is
  being investigated separately. Member approved temporary Gboard comparison;
  selected spell-checker change was read back successfully, with original
  Samsung selection retained transiently for restoration. Keyboard, installed
  552-test APK, vault and models remain unchanged. Await the same copied typo
  test and safe metadata inspection. No private records/replies/suggestions
  captured, dependency added, build repeated or commit/push performed.

- Gboard comparison completed on the unchanged 552-test APK. Member reports a
  visible matching answer; content-free trace shows a 400 ms spelling timeout,
  Gemma selection and accepted audit with no fresh record tool. This does not
  validate system correction or MiniLM acceleration. Samsung selection restored
  and verified; keyboard unchanged. No rebuild/install, content capture or guard
  relaxation. Missing-bubble investigation remains open independently.

- Added content-free reply-delivery monitoring under the existing opt-in debug
  trace: lifecycle reply observed, current transcript append, or fixed
  current/handled-request rejection. `chat_consumed` means transcript state was
  assigned; it does not prove frame rendering, scrolling or persisted storage.
  Per-request stages use bounded in-memory dedup, reset by clear/disable/delete;
  writes reuse the existing asynchronous writer. Original delivery/route gates
  remain unchanged. Independent review found no blocker. Initial compile caught
  a missing callback parameter; explicit callback wiring was corrected before
  installation. Full Android unit/debug/release gate is running. No root-cause
  fix or physical pass is claimed; next task is one same-question reproduction.

- Delivery-monitoring slice passed the full 554-test/debug/release APK and
  release vital-lint gate (zero failures/errors/skips), independent review and
  clean diff check. APK hash is in the recognition decision. Authorized in-place
  installation is underway; no spelling matching, deadline or reply gate changed.

- Reviewed 554-test APK installed in place (`Success`) on Samsung SM-S928B /
  Android 16 and cold launched (`Status: ok`); existing storage preserved.
  Samsung system spell checker was restored before this installation. Await
  member unlock and one copied `protteen` request, then inspect fixed delivery
  stages. No biometric automation, private-content capture or commit/push.

- Member confirms the 554-test Gemma repeat answered correctly. Safe events
  include accepted audit, `lifecycle_ready` and `chat_consumed`, confirming the
  transcript append for that attempt. A subsequent MiniLM day-nutrition turn
  also has accepted audit and transcript append. Later stale-request observations
  do not undo those completed deliveries. The earlier intermittent missing
  bubble is not reproduced or proved fixed. Member requested a realistic typo
  outside existing local handling; next copied request uses `proteen`, a vowel
  substitution outside the local nearby-key rule, with the same supported full
  question. No alias, build, install or provider setting change for this test.

- Realistic-typo follow-up: member reports `proteen` used Gemma and a subsequent
  `gime...` question appeared to use MiniLM. Latest two retained turns instead
  both select native Gemma, pass reply audit and append to Chat. The first has
  `spell_check / rejected / dictionary_word`; the second has no spelling event.
  Monitoring contains no text, so exact question-to-turn attribution remains
  conditional on the member's reported order. Code inspection finds `give` is
  absent from both correction vocabularies; `gime` cannot itself trigger the
  system-candidate path. Requested copied question only to resolve wording and
  route discrepancy. No successful OS correction claimed, no new alias or code
  change. This exposes candidate coverage limits, not a general provider verdict.

- Member's next `recored` question felt faster. Newest retained turn confirms
  MiniLM exact-day nutrition, accepted audit and Chat transcript append, with no
  system-spelling event. Turn start to transcript assignment is about 284 ms.
  `recored` to `recorded` is a single deletion handled by the existing local
  candidate rule; this is useful local robustness evidence, not OS correction.

- Member requested returning to Gboard for further tests and reported `tot`
  instead of `total` used Gemma. Latest retained turn has native Gemma, accepted
  audit and transcript append, with no system-spelling attempt. Exact copied
  full question requested before attributing the candidate rejection. Gboard
  spell-checker selection changed and verified under explicit authorization;
  keyboard, app binary and storage unchanged. Pending same `proteen` request
  twice (first/warm comparison) then `tday`, using existing 554-test APK and safe
  metadata. Gboard remains selected during this active comparison; original
  Samsung selection is available for restoration. No provider success claimed.

- Gboard follow-up: selected component is still Gboard. Two latest spelling
  attempts returned fixed `dictionary_word`, then native Gemma. One accepted
  audited reply was observed by Chat but rejected by `active_request_mismatch`
  with no transcript append; the following attempt has `chat_consumed`.
  This localizes the intermittent missing bubble to the active-request handoff
  for that turn, without proving why identity changed. Read-only investigation
  resumed; do not bypass the stale-result guard. Member supplied the complete
  older question `give me my recored protein tot for today?`: two independently
  misspelled words exceed the current one-token correction policy, explaining
  why that example cannot exercise the provider path. No APK reinstall is needed
  to switch the already-installed system provider; app uses the selected service
  at session creation. No provider/record content was inspected.

- Read-only follow-up bounds the missing-bubble investigation: vault refresh/
  resume synchronization can clear active request identity when stored and live
  transcripts differ, but Stop/reset/new requests are other sources. Current
  trace proves mismatch, not the invalidation source. Regional English versus
  requested `en` is a separate untested spelling-provider variable; no locale
  cause is established. Next bounded engineering task is fixed invalidation
  reason evidence plus session-sync regression coverage before changing delivery
  behavior. No new implementation, build or install in this follow-up. Gboard
  remains selected for the member-requested test cycle.

- 2026-10-15 member authorized replacing the restrictive one-word correction
  policy for natural Chat. Implementing a shared bounded planner: up to three
  changed words, 64 complete combinations, local conservative edits/contextual
  abbreviations and provider-confirmed broader edits under one shared 400 ms
  deadline. All complete candidates retain original host/date/read constraints;
  distinct valid meanings require clarification. Privacy disclosure revised to
  up to three individually checked words. No full question/records, new dependency,
  locale/threshold change, saved-write authority or delivery-ID bypass. Parent
  owns docs/disclosure; worker owns planner/integration/tests. Review, full gate,
  in-place install and phone acceptance pending; no commit/push authorized.

- Multi-word revision implemented and independently reviewed with no blocker.
  Parent review corrected protected-source/target handling, changed-word bounds,
  equivalent prefix grouping and valid-word preservation before the gate.
  Tests include the exact copied question and three-change/equivalent-prefix
  cases, two isolated provider corrections, command candidates, meaning/host
  constraints, four-word rejection and deterministic shared-deadline expiry.
  Full gate passed 562 tests (zero failures/errors/skips), debug/release APKs
  and release vital lint; diff check clean. Initial calorie-test expectation
  error was corrected, with production behavior unchanged. Hash and command in
  recognition decision. Authorized installation underway; no provider success
  or missing-bubble repair is claimed. No commit/push.

- Checksum-identified 562-test APK installed in place (`Success`) on Samsung
  SM-S928B / Android 16 and cold launched (`Status: ok`), preserving existing
  vault, records and models. Gboard selection remains as previously authorized;
  no new system setting change. Await member unlock and exact two-/three-error
  protein-total questions, then content-free route/delivery inspection. Broader
  provider, ambiguity and disclosure/font/TalkBack gates remain open; the known
  intermittent active-request mismatch is not claimed fixed. No private content
  capture, biometric automation, commit or push.

- Named multi-word phone acceptance passed on the installed 562-test build:
  member confirms both exact two-/three-error protein questions appear and
  match saved records. Two new safe traces each show accepted MiniLM
  `read_day_nutrition / today`, required fresh read, accepted factual audit and
  Chat transcript append. Neither turn has a system-spelling event or Gemma
  selection; this validates the local compositional correction/abbreviation
  path, not Android-provider correction. Both have positive 0.8988, competing
  0.7497, fallback 0.4739. Later stale-result observation does not undo the
  completed append. Recorded only technical metadata, no question/answer/record
  capture. Original-question regression, actual OS suggestions, ambiguity UI,
  disclosure/font/TalkBack and known intermittent request-ID gap remain open.

- Checkpoint review/commit scope (2026-10-15): member requested reviewing the
  complete pending diff for gaps, then committing and pushing. This explicitly
  authorizes commit/push after review; no additional permission prompt is needed.
  Member explicitly deferred the missing-answer investigation; retain its
  observed request-ID mismatch and unknown invalidation source as open evidence,
  without speculative fix or stale-result bypass. Gboard remains selected per
  member preference; named local correction successes do not prove Gboard use.
  Independent recognition and runtime/privacy reviews are in progress. Current
  full gate remains 562 passing tests/debug/release and clean diff check; no
  Android code has changed since the accepted phone build at review start.

- Full pre-commit review completed: independent recognition/authority and
  Android-runtime/privacy reviewers found no actionable blocking defects in the
  complete diff, including new untracked source/tests. Parent corrected stale
  current-status and disclosure notes; no Android implementation changed during
  this review. Reconfirmed 562 tests with zero failures/errors/skips and matching
  accepted debug APK SHA-256; existing complete debug/release/vital-lint gate
  remains applicable without another identical run. Diff check clean. Remote
  main matched local base before commit. User-authorized commit/push proceeding.
  This is a checkpoint, not M4/release acceptance: actual OS correction, direct
  Android adapter callback/cleanup tests, pre-31, Stop/lock/late-callback physical
  checks, ordinary-question regression and disclosure/accessibility gates remain
  open. Missing-answer request-ID investigation explicitly deferred. Next bounded
  task when resumed: choose provider/session acceptance or another approved
  feature; do not silently resume deferred delivery work or enable release MiniLM.

- Next approved slice (2026-10-15): natural saved-dish recall and consumption
  wording, building on clean committed/pushed `6cfb4a0`. Existing exact recall/
  draft, fresh search, named choices, editable prefill and Review/Discard/Save
  seams mapped. Extending complete command frames and bounded local corrections,
  preserving opaque dish names and separate recall/draft permission. No names to
  OS spelling, auto-save, frequency inference, dependency or missing-answer work.
  Parent owns docs; worker owns recognition/integration/tests. Complete review/
  gate then one in-place install before named physical acceptance. No new
  commit/push authorized for this slice.

- Saved-dish implementation and independent review complete. Full candidate
  validation now uses a saved-frame mask with immutable named query and
  recall/draft permission; saved companions cannot enter the generic/system
  correction path. Fixed named/usual overlap, malformed-frame name leakage,
  short-command minimum handling and relative-day/weekday rejection; dedicated
  no-provider and query-preservation regressions added. Exact consumption verbs
  and saved/usual markers remain required; a plain-consumption test expectation
  was narrowed rather than granting new permission. Three generic natural-frame
  prototypes added, with scoring/model artifacts unchanged. Initial compile and
  test failures were corrected before installation. Reviewer found no remaining
  blocker; complete unit/debug/release gate running. Missing-answer work remains
  deferred. Phone is connected; no new install or physical acceptance yet.

- Saved-dish extension complete gate passed: 568 tests, zero failures/errors/
  skips, debug/release APK assembly and release vital lint. Diff check clean;
  independent review found no remaining blocker after name/date fixes. Command
  and checksum are in the companion-bundle specification. Installation under
  the active authorized in-place cycle is proceeding; no new code change after
  the gate. Named recall/draft/review/discard phone tests remain open. No commit
  or push; no broad release/provider/accessibility acceptance claimed.

- Reviewed 568-test debug APK installed in place (`Success`) on Samsung SM-S928B
  / Android 16 and cold launched (`Status: ok`). Existing vault/records/models
  preserved; no system provider setting change or biometric automation. Await
  member typo-command recall, natural named consumption, Review food/portion
  comparison and Discard; then inspect only fixed routing/audit/delivery events.
  No raw device content captured and no physical acceptance claimed yet.

- Member reports both saved-dish requests work, review matches foods/portions and
  draft was discarded. Retained new turn shows MiniLM margin rejection (positive
  0.6771, competing 0.6533, fallback 0.2826), then Gemma with fresh saved-dish
  search/continuation, accepted audit and transcript append. This accepts the
  member-observed functionality but does not prove fast routes for both prompts.
  No prompt/reply/record values captured. Bounded follow-up masks only opaque
  query spans as `name` for classification, retaining command wording, actual
  query/tool/proof, original Chat/Gemma text and all margins/authority gates.
  Separate span/query tests, review and full gate before an in-place retry;
  missing-answer investigation remains deferred.

- Named-slot masking follow-up passed independent review and the complete
  568-test/debug/release/vital-lint gate with zero failures/errors/skips. Exact
  capture spans replace only classifier entities, including postfix names;
  actual query and usual-meal input stay intact. Added capture/name/host and
  debug-scope regressions; corrected one legacy classifier-text expectation and
  strengthened real-query assertion. Hash in companion-bundle spec; diff check
  clean. Authorized retry install proceeding. No thresholds, factual audits,
  actual query or save authority changed; fast phone acceptance remains open.

- Masking update installed in place (`Success`) and cold launched (`Status: ok`)
  on Samsung SM-S928B / Android 16, preserving existing data/models. Await one
  recall-only typo request, then inspect fixed route/audit/delivery metadata;
  test draft separately to avoid uncertain text-to-turn correlation. No private
  content capture, biometric action, commit or push.

- Post-mask recall-only physical gate passed: member confirms correct saved dish
  and no meal-draft offer. Safe trace shows accepted `saved_dish_recall` score
  0.9909 versus competing 0.8934/fallback 0.2481, MiniLM selection, required saved
  search, accepted audit and Chat append. No Gemma route or system-spelling event.
  Requested consumption/draft retry separately; no broad draft/ambiguity or
  provider acceptance implied. No private values inspected.

- Post-mask consumption/draft physical gate passed: member confirms Review
  offered without error, foods/portions match and draft discarded. Safe trace
  accepts `saved_dish_draft` (0.9568 positive, 0.8717 competing, 0.3512 fallback),
  selects MiniLM, requires saved search, accepts the reply audit and appends Chat.
  No Gemma selection. Later already-handled observation does not undo accepted
  delivery. Both named recall and draft cases now pass on the installed masking
  build (568 tests; hash in companion spec). Broader no-match/ambiguity, usual
  variants, final Save/reopen, ordinary-read and accessibility/lifecycle checks
  remain open. Missing-answer issue still deferred. No raw data capture or new
  code change, and no commit/push authorized/performed for this slice.

- Bare-consumption slice approved (2026-10-15) after “I had san jacobo” missed
  the marker-bound path. Adding a distinct host-owned saved-dish probe before
  ordinary meal drafting: exact-name/alias review, bounded ambiguous choices,
  validated no-match original-text fallback with mandatory search cleared once.
  Compound lists without an exact whole-dish match retain ordinary drafting.
  Preserve existing explicit saved/usual parsing, opaque names/no OS spelling,
  current request/session checks and final Save. No new dependency, release
  MiniLM activation, missing-answer work or commit/push. Worker owns code/tests;
  parent owns docs; review, full gate and direct in-app checks pending.

- Bare-consumption implementation gate passed: 570 tests with zero failures,
  errors or skips; debug/release builds and release vital lint pass; diff check
  clean. Parent reviewed the typed probe, exact/compound disposition and guarded
  original-text handoff, correcting incomplete worker wiring and continuation
  operation handling. Independent reviewer hit its delegated usage limit and
  did not complete; do not claim independent approval. Pure policy tests do not
  exercise the native handoff lifecycle. APK hash is in companion-bundle spec.
  Installed in place (`Success`) on Samsung SM-S928B / Android 16 under ongoing
  authorization, preserving vault/model files. Bare exact-name Review/Discard,
  no-match draft, ambiguity and cancellation/lock acceptance remain pending.
  Missing-answer investigation remains deferred. No commit/push.

- Bare exact-name phone gate passed on the installed 570-test build: member
  confirms “I had san jacobo” offered Review without error, saved foods/portions
  match and draft discarded. Safe metadata accepts saved-dish draft (0.9808
  positive / 0.3948 competing / 0.3451 fallback), selects MiniLM, requires fresh
  saved search, passes audit and records Chat consumption. No Gemma selection
  for this turn. Subsequent duplicate/stale observations followed delivery;
  they are not a failure of the confirmed interaction. No private values read.
  No-match draft, ambiguity, lifecycle and independent review remain open.

- Member confirms ordinary unsaved-meal draft succeeded for the requested
  eggs/toast case, then reports the assistant card vanished after leaving
  Review without saving. Safe trace confirms the MiniLM saved-dish probe, but
  contains no downstream draft audit; member observation is the draft evidence.
  Added a plain historical prepared-draft acknowledgement, gated to current
  completed meal operations; question/ack flush before Review navigation using
  the existing encrypted transcript serializer. No draft payload persisted.
  Independent review caught one-time navigation dedupe blocking later Review
  attempts; bounded repeatable review now retains overlap/stale/lock checks
  while allowing deliberate reopening. Reviewer verified that correction and
  found no further concrete defects. New helper and overlapping/sequential review
  tests added. Final build and physical retention/reopen verification pending.

- Draft-history fix final gate passes: 573 tests, zero failures/errors/skips;
  debug/release APK builds and release vital lint pass; diff check clean.
  Installed in place (`Success`) and cold launched (`Status: ok`) on Samsung
  SM-S928B / Android 16, preserving existing data/models. Hash recorded in
  companion-bundle specification. Physical draft/discard/Chat retention and
  reopen checks pending; no new commit/push authorization or action.

- Draft-history physical retention gate passed: member confirms the ordinary
  eggs/toast request, Review and leave-without-saving sequence retains the
  prepared-draft assistant acknowledgement, including after closing/reopening
  Chat. Tested on the installed 573-test build, Samsung SM-S928B / Android 16;
  hash in companion spec. No saved meal or raw device-content capture. Repeat
  Review, failed transcript save, lock/cancel and accessibility remain open.
  No commit or push performed.

- Full uncommitted checkpoint review identified two P2 fixes before commit:
  broad bare `I had ...` plus classifier masking can force non-food statements
  into meal extraction on no saved match; the no-match handoff also invokes the
  legacy synchronous proposal helper without the native-agent timeout. Next
  bounded task: normal intent selection on uncertain/no-match probes, suppress
  the probe once to prevent repetition, and use timed native drafting only after
  positive meal intent. Independent review found no confirmed host-binding,
  one-shot clearing or transcript-handoff defect. Native fallback lifecycle
  coverage remains open. No source changes/build/install during this review;
  existing 573-test gate and named phone acceptance remain evidence, not proof
  these newly found branches are safe. No commit/push performed.

- Review corrections approved 2026-10-16: implementing original-text normal
  intent selection after uncertain/no-match saved probes, with suppression of
  the probe and model-emitted repeated saved search for that continuation.
  Only a positive native draft tool decision may enter existing timed extraction.
  Preserve exact saved fast path, typed authority, fresh request and search-state
  clearing, cancellation/lock, review/Save and retained Chat acknowledgement.
  Worker owns code/tests; parent owns docs, final gate and authorized in-place
  installation. No new dependencies, blacklist growth or commit/push authority.

- Review-fix implementation passed independent review and the full gate on
  2026-10-16: 576 tests, zero failures/errors/skips; debug/release APK builds and
  release vital lint pass; diff check clean. Parent/reviewer caught and worker
  corrected suppression re-entry in a lower companion resolver and after a
  native tool result. The host suppression flag now survives/reset with pending
  tools; resumed normal intent and selected native draft use the existing timed
  native path. Scoped `agent_meal_draft` preserves continuation reconciliation.
  Pure tests cover resolver/tool decisions and no OS spelling; full async
  lifecycle/timeout-race execution remains a coverage gap. APK hash in companion
  specification. Authorized in-place install underway; phone gates remain open.
  No commit/push performed.

- Review-fix APK installed in place (`Success`) and cold launched (`Status: ok`) on
  Samsung SM-S928B / Android 16, preserving existing data/models. Await member
  non-food response, unsaved meal drafting and exact saved-meal regression.

- First review-fix non-food retry failed visible delivery: member reports error
  or no answer. Safe trace confirms MiniLM probe request 1 -> normal native Gemma
  request 2 -> accepted reply audit -> lifecycle-ready -> active-request mismatch.
  No repeat search or automatic draft is recorded, but response content was not
  inspected and the visible non-food gate remains failed. Added diagnostic-only
  fixed-ID/reason provenance for continuation reconciliation, transcript refresh
  reset and missing/different active identity. No prompt/record capture or stale
  guard bypass. Normalized transcript refresh is a candidate, not a proven phone
  cause. Diagnostic review/gate/retry pending; global delivery work remains
  outside scope except identifying this resumed-turn failure.

- Identity-provenance diagnostic gate passes: 577 tests, zero failures/errors/
  skips; debug/release builds and release vital lint pass; diff check clean.
  Independent review confirms fixed codes/numeric IDs only, debug opt-in and
  no transcript capture. Diagnostic hash in companion specification. Authorized
  in-place installation underway; actual cause and visible non-food response
  remain unverified. No behavioral bypass, commit or push.

- Identity-provenance APK installed in place (`Success`) and cold launched
  (`Status: ok`) on Samsung SM-S928B / Android 16; existing vault/models preserved.
  Await same-Chat fictional non-food replay. No biometric automation or raw
  device-content inspection.

- Diagnostic non-food replay passes: member confirms a normal response appeared.
  Safe metadata shows request 1 submission reconciled, one MiniLM saved probe,
  request 2 continuation reconciled, native Gemma selection, accepted reply audit
  and Chat consumption. No sync reset, mismatch or repeated search in this turn.
  This named success does not establish the cause or repair of the prior failure.
  Existing 577-test APK/hash, Samsung SM-S928B / Android 16. Await ordinary meal
  draft and exact saved-meal regression; retain intermittent delivery as an open
  diagnostic gap. No private content inspected, commit or push.

- Ordinary-meal regression failed: member reports loading then disappearance
  for “I had screamble eggs and toast”. Safe trace confirms request 2 reconciled,
  native Gemma selected, then `conversation_sync_normalized` cleared its active
  identity while work continued. Prior non-food success also logged this reset
  after delivery. This establishes the normalized-equivalent refresh bug, without
  reading raw conversation contents or claiming which size normalization differed.
  Added pure hydration decisions with bounded last-loaded record comparison:
  unchanged and canonical-equivalent snapshots preserve live/unsaved local state;
  genuine change/removal/expiry/first hydration still restore/reset. Parent review
  caught the need to preserve debounced unsaved turns under unchanged snapshots;
  worker corrected it. Independent review clean; six focused tests added. Full
  gate/phone retry pending; no stale guard bypass, dependency or schema change.

- Canonical refresh fix gate passed: 583 tests with zero failures/errors/skips;
  debug/release APKs and release vital lint pass; diff check clean. Independent
  review found no material defect. Hash in companion specification. Authorized
  in-place install underway; exact reported meal wording and retained response
  across periodic refresh remain physical acceptance gates. No commit/push.

- Refresh-fix APK installed in place (`Success`) and cold launched (`Status: ok`)
  on Samsung SM-S928B / Android 16. Existing vault/models preserved. Await exact
  reported wording retry, Review/Discard and response retention. No biometric
  automation, private-content capture, commit or push.

- Post-refresh-fix meal regressions pass: member reports both requested meal
  cases worked. Safe trace confirms unsaved probe -> native Gemma -> current
  prepared-draft acknowledgement consumed, then saved-dish MiniLM -> fresh
  search -> accepted audit -> consumed reply without Gemma. Successful sequence
  spans over one minute; no normalized refresh reset or active-ID mismatch.
  Expected later already-handled event only. Samsung SM-S928B / Android 16,
  installed 583-test APK/hash in companion spec. No private values inspected.
  Named loading/disappearance regression closed; broader lifecycle, ambiguity,
  failed-save and accessibility acceptance remain open. Await explicit current
  checkpoint commit/push approval; none performed.

- Member explicitly confirms draft success and prepared-draft message retention
  after the requested full minute, following Review/Discard, on the 583-test
  refresh-fix build. Periodic-refresh retention phone gate passed. Broader gates
  remain open as documented; no commit or push performed yet.

- Member explicitly approved committing and pushing the reviewed checkpoint to
  `main` on 2026-10-16. Scope: natural saved dishes, normal-intent fallback,
  timed native draft selection, retained draft messages, request-ID diagnostics
  and canonical hydration/refresh. Final complete gate remains 583 passing tests
  plus debug/release APKs and release vital lint; independent review and named
  phone checks pass. Broader acceptance gaps remain documented, not closed by
  authorization. No unrelated files or private device artifacts are included.

- Post-checkpoint cancellation acceptance (2026-10-16, `05b72e3`, Samsung
  SM-S928B / Android 16, unchanged installed 583-test build): member stopped
  the fictional ordinary-meal request during loading, then asked for the latest
  recorded meal's calories and waited about a minute. Member confirms the new
  answer matched and no cancelled draft/reply appeared. Content-free trace
  confirms saved probe request 5 -> normal Gemma continuation 6 -> cancelled,
  followed by fresh MiniLM latest-meal request 7 -> audited/consumed result 8.
  No late delivery for cancelled request 6 appears in the retained sequence.
  This closes the named Stop/next-read gate only; lock/reopen, process recreation,
  forced timeout and broader lifecycle checks remain open. No new APK installed,
  raw member content inspected, meal saved, commit or push performed.

- Lock/reopen acceptance on the same baseline: member locked the phone during
  ordinary meal loading, then unlocked Metria with biometrics and reopened Chat.
  Sent question remained, fresh today-meals answer matched and no interrupted
  draft returned. Trace shows request 10 cleared by lock, then fresh MiniLM
  request 11 and audited/consumed result 12, with no late result from 10.
  Named lock/interrupted-request/next-read gate passed; force-stop/recreation,
  forced timeout and other broader states remain open. Begin bounded saved-dish
  typed clarification per companion specification; no new installation yet.

- Saved-dish typed clarification implementation (2026-10-17): host-owned
  candidate snapshots, completed-request/session-generation binding and fresh
  full-dish equality checks protect ordinal/full-name replies and existing
  buttons. Both persist Chat before review; failed writes keep same-choice
  retry without renumbering. Declines clear locally and invalid choices do not
  delegate selection to Gemma. New pure policy tests cover bounded matching,
  duplicate names/IDs, changed/removed dishes, stale/locked sessions and retries.
  First gate exposed a test setup that changed both pending and active IDs;
  corrected the setup to retain the original current ID, without weakening
  production checks. Intermediate 588-test/debug/release gate passed.
  Independent review found an imperative prompt could outlive transient choices;
  removed its transcript insertion. Choice instructions stay in the live card,
  and invalid-choice history is a non-imperative outcome statement. Parent
  checked the follow-up wording fix; final gate/install/phone acceptance pending.
  No new dependency, persistence format, private-data capture, commit or push.

- Final clarification gate (2026-10-17) passes: 588 tests, zero
  failures/errors/skips, debug/release APK builds, release vital lint and clean
  diff check. Hash in companion specification. Active-cycle `adb install -r`
  succeeded on Samsung SM-S928B / Android 16, followed by cold launch `Status:
  ok`; existing app identity/vault/models preserved, with no clear/uninstall.
  Member biometric unlock and ordinal selection -> Review/Discard -> retained
  selection/acknowledgement are pending. Name selection, decline, ambiguity,
  stale-choice/lifecycle, failed-write retry, accessibility and visual gates
  remain open. No private content captured, commit or push performed.

- Clarification ordinal acceptance (2026-10-17, same 588-test APK/hash and
  Samsung): member confirms the saved San Jacobo offer followed by "the first
  one" opens the correct editable review, and selection/acknowledgement remain
  after Discard and returning to Chat. No meal saved. This named phone gate is
  passed from member observation; no new selection trace event is implemented.
  Full-name selection, decline, multiple-candidate/invalid choice and broader
  lifecycle/failed-write/accessibility states remain open. No source change,
  new install, private content inspection, commit or push.

- Clarification full-name/decline acceptance (2026-10-17, unchanged 588-test
  build/hash and Samsung): member confirms "san jacobo" selects the correct
  review from its current offer, then Discard. On a fresh offer "no thanks"
  clears choices without opening review or creating a meal. Named full-name
  and decline gates pass. Invalid choice, pending-choice lock, multiple-candidate
  and broader lifecycle/failed-write/accessibility gates remain open. No code,
  build or installation change; no private device content inspected, commit
  or push performed.

- Clarification invalid-choice/lock acceptance (2026-10-17, unchanged 588-test
  APK/hash and Samsung): member confirms "the fourth one" is rejected without
  Review while the current choice remains available. Lock/biometric unlock and
  Chat reopen clear the card while preserving history. Named gates pass.
  Independent review corrections and final full gate remain valid; only docs
  changed after installation. Multiple-candidate selection on phone, changed/
  deleted-dish handling, failed-save retry, force-stop/recreation, forced
  timeout, accessibility and visual gates remain open. Ready for checkpoint
  approval; no new commit/push authorization and neither action performed.

- Member explicitly approved committing and pushing the reviewed clarification
  checkpoint to `main` on 2026-10-17. Scope: bounded ordinal/full-name saved-dish
  replies, shared guarded Review handoff, decline, policy tests and documentation.
  Final evidence remains 588 passing tests, debug/release builds, release vital
  lint, clean diff check and named phone passes. Broader acceptance gaps above
  remain open. No new installation or private-device capture is part of this
  commit/push action.

- Portion clarification approved (2026-10-17) from clean pushed `1d4113f`.
  Read-only discovery confirmed that model questions lack typed item binding and
  model confidence is not portion provenance. First slice therefore uses a
  host-generated question for exactly one eligible uncertain food and a separate
  transient member-gram review prefill; raw proposal/ranges/confidence remain
  unchanged. Skip keeps estimates and existing Review/Save remains mandatory.
  Request/proposal/session binding and no restored offer are required. Parent
  adds only fixed applied/skipped/rejected trace reasons with privacy/restart
  tests; implementation agent owns Kotlin flow/review initialization/tests.
  Scope in companion spec; development, review and full gate pending. No new
  dependency or saved-meal change; no installation, commit or push performed.

- Initial portion gate passed: 596 tests, zero failures/errors/skips,
  debug/release APKs and release vital lint. Independent review confirms a
  repeated-Review defect: Chat disposal drops a locally accepted portion while
  leaving the original proposal available; reopening Review can silently revert
  to its estimated midpoint. Fix retains only accepted overrides in unlocked
  conversation-session memory, with unanswered offers still composition-local.
  Lock/reset/new turn/explicit draft Discard invalidate accepted state. New
  regression and final gate/phone acceptance are pending; do not install the
  intermediate build. No raw member content inspected, commit or push.

- Repeated-Review fix is implemented and independently reviewed without a new
  material defect: accepted numeric overrides are session-owned and rebound only
  to the same current proposal/request/generation. Unanswered questions remain
  local and cannot reappear from history; Stop/reset/lock/new turn/discard/save
  clear accepted state. Parent also blocks Review navigation while a newer
  submission/continuation is pending, including before its request-ID handoff.
  Pure tests cover binding, unchanged model data, exact review-prefill identity
  after earlier item transformation and trace privacy. No automated full Compose
  disposal/reentry integration test exists; physical repeated Review is required.
  Final build gate and in-place phone acceptance pending. No commit/push.

- Final portion gate (2026-10-17) passes after the repeated-Review fix and
  nullable-cleared-proposal guard: 596 tests, zero failures/errors/skips,
  debug/release APKs, release vital lint and diff check. Debug hash in companion
  specification. Authorized in-place installation underway; no physical
  acceptance claimed. Same-draft repeated Review, actual quantity/skip/invalid/
  topic/lock behavior and layout/accessibility gates require member observation.
  No new dependency, model/vault replacement, raw capture, commit or push.

- Portion APK installed in place successfully and cold launched (`Status: ok`)
  on Samsung SM-S928B / Android 16. Existing identity/private storage retained;
  no clear/uninstall/signing change or biometric automation. Member test pending:
  fictional cooked-rice draft -> "150 g" -> Review/Discard -> Review again,
  checking unchanged member grams plus retained reply/acknowledgement. Safe
  trace will verify the fixed local portion outcome, without reading content.

- Portion phone attempt exposed a meal-source bug (2026-10-17, installed
  596-test APK/hash above): member reports the initial cooked-rice statement
  produced a recording invitation, then "Yes" produced a draft named "yes".
  A subsequent portion reply updated Review, but exact-value and repeated-Review
  acceptance are not established by this report. Content-free metadata confirms
  first probe -> normal Gemma reply, next native Gemma draft, then local
  `portion_applied`; no prompt, draft payload or record value inspected.
  Read-only discovery confirms NativeMealDraftPolicy uses the current turn as
  extraction text, so the affirmative loses its original food description.
  Fix a one-turn host-held invitation -> affirmative source binding, requiring
  the existing draft tool and original member text. Never extract food from
  assistant prose or draft a bare acknowledgement. Invalidation/tests/review/
  final build and phone replay pending; no acceptance or save claimed.

- Original-source confirmation fix implemented: a current accepted recording
  invitation can hold the exact preceding member consumption text for one next
  closed affirmative. UI session request/generation/unlocked checks and native
  previous-request/actual-invitation/tool checks remain separate. No source
  comes from assistant food prose, model arguments or restored history. A bare,
  stale, declined or correction-bearing acknowledgement cannot be a meal name;
  a selected draft tool without valid source gets a host clarification. Parent
  completed validation after implementation-agent interruption, strengthened
  same-clause positive invitation matching, negative/non-food source rejection,
  punctuation handling and native routing for acknowledgements. Prompt clarifies
  direct unsaved drafting for clear consumption reports. Independent review's
  comma and negative-source findings are covered by focused regressions. The
  599-test/debug/release/lint gate passed before the final acknowledgement guard;
  final source verification and phone replay pending. No install/commit/push.

- Final original-source fix gate passes (2026-10-17): 599 tests, zero
  failures/errors/skips, debug/release APKs, release vital lint and clean diff
  check. Debug hash in companion spec. Independent review identified comma
  confirmation and negative-source cases; parent fixed and tested them while
  preserving correction/decline/missing-context rejection and mandatory native
  tool/read gates. In-place installation underway for corrected cooked-rice
  draft identity plus exact 150 g/repeated-Review test. No phone pass, raw data
  capture, meal save, commit or push claimed.

- Corrected original-source APK installed in place (`Success`) and cold launched
  (`Status: ok`) on Samsung SM-S928B / Android 16. Existing private storage and
  models preserved. Await member biometric unlock and same original cooked-rice
  statement, optional "Yes, please.", exact "150 g", Review/Discard/repeated
  Review comparisons. No raw device content read and no meal saved.
### Confirmation replay still failing — 2026-10-17

- Member reports the corrected build still produces a meal named "yes" after
  the recording invitation. Original-food and repeated-Review gates remain open.
- Installed APK SHA-256 was checked on-device and matches the 599-test artifact
  `9f624078f68f389736ce8a66e21155967d3aa234e1c38ed1cd3a5fe709f98c9f`.
- Inspected only the existing content-free trace: current native Gemma turn and
  draft delivery appear, with neither confirmation-used nor confirmation-missing.
  No prompt, food proposal, vault contents or raw UI dump was inspected.
- Independent read-only path audit found exact affirmative extraction guarded in
  current source. Cause remains unproven; exact copied confirmation requested.
  Do not broaden food/confirmation authority to hide this evidence mismatch.

### Structured meal continuation approved — 2026-10-17

- Latest replay produces the safe clarification rather than an acknowledgement
  as food. Content-free metadata confirms `meal_confirmation_missing`, then a
  fresh meal turn and `portion_applied`. Member reports gram handling works after
  repeating the food; source continuity and repeated-Review gates remain open.
- Member questioned why conversation was being resolved by host string rules.
  Approved replacing invitation prose matching with one typed session-bound
  original meal candidate and an empty-argument Gemma confirmation tool.
- Gemma interprets the next reply using conversation context. Host validates
  candidate provenance, request/session identity and required-read constraints;
  model arguments cannot replace the original food or select old history.
  Existing unsaved draft validation and explicit Review/Save remain required.
- Pending: implementation, independent review, complete Android gate and
  in-place physical replay. No new dependency, private capture, commit or push.

- Structured implementation and independent review complete. Added native
  empty-argument confirmation tool, typed one-turn source/session binding and
  prior visible member/assistant exchange checks. Candidate turns retain native
  routing; no invitation string matching or affirmative list authorizes source
  reuse. Generic tool-result continuation cannot execute confirmation.
- Full Android gate passes: 602 tests, zero failures/errors/skips, debug/release
  APKs and release vital lint; diff check clean. Artifact hash in companion spec.
  No new dependencies or changes to saved records/model files. In-place install
  underway under active test-cycle authorization; phone behavior still pending.
- Review found no material deterministic host defect. Runtime/Compose
  orchestration, Gemma's confirmation/decline/correction choices, required-read
  retry and in-flight lock remain physical/integration gaps. The factual-auditor
  retry prompt's older tool wording can still cause a missed model selection;
  it cannot bypass source validation or Save review.

- Reviewed 602-test build installed with `install -r` (`Success`) and cold
  launched (`Status: ok`) on Samsung SM-S928B / Android 16. Existing app identity,
  private storage and models retained; no biometric automation or raw capture.
  Member replay requested: original rice report, natural confirmation only if
  invited, original-food draft with no repeated food question, then explicit
  gram prefill and Discard. Direct drafting does not exercise confirmation;
  record that distinction when acceptance arrives. No meal save/commit/push.

- Named rice/portion replay: member selects "Rice draft directly; portion
  correct" after the prescribed Review/Discard check. This confirms visible
  food and quantity for one Review, not repeated Review or decline/correction.
- Read only current-run content-free events: saved probe, normal Gemma reply,
  then structured `meal_confirmation_used`, delivered draft and `portion_applied`.
  Metadata therefore confirms bound-source confirmation and local portion
  handling occurred; exact intervening dialogue is not inferred from the member's
  direct-draft selection. No prompt/proposal/record/UI content inspected.
- Next bounded physical check: decline a current offer, then verify no draft;
  correction/repeated-Review/lifecycle/accessibility gates remain open.

- Ask-first/decline attempt did not exercise decline: member reports an immediate
  draft despite explicitly requesting a question first. Direct-drafting system
  instruction had no stated exception for that preference. Updated native prompt
  and draft-tool description to prioritize ask-first/wait requests and return
  one question with no draft tool. No host phrase-matching branch added.
  New gate/install/physical acceptance pending; original-source/portion evidence
  above remains bounded to the previous named run.

- Ask-first instruction update passes 602 tests (zero failures/errors/skips),
  debug/release APKs, release vital lint and diff check. Debug artifact hash in
  companion spec. `install -r` succeeded on the same Samsung/Android target,
  preserving existing app identity/private storage/models. Replayed ask-first
  and decline behavior remains pending member unlock/observation; no model
  reliability claim or new host language matcher. No save/commit/push.

- Ask-first replay reaches an offer, and member reports no draft after decline,
  but the reply incorrectly says the meal was unclear. Content-free trace shows
  `meal_confirmation_missing`: draft selection was safely blocked, not a typed
  semantic decline outcome. No prompt or record content inspected.
- Next bounded correction: native empty-argument decline action with the same
  pending-source/request/no-required-read validation as confirmation. Valid
  decline acknowledges no draft; missing/stale actions use neutral wording.
  Build/review/phone acceptance pending. No lexical authorization or Save added.

- Typed decline implemented; independent review found no material defect in
  empty-argument schema, current-source/request/read guards or no-generation
  return. Invalid pending actions use neutral no-draft text. Fixed content-free
  `meal_confirmation_declined` joins monitoring. Policy/schema and trace tests
  extended; final Android gate running. Actual Gemma decline selection and
  corrected visible acknowledgement require phone replay on the new artifact.

- Final typed-decline gate passes: 603 tests, zero failures/errors/skips,
  debug/release APKs, release vital lint and clean diff check. Debug hash in
  companion spec. Authorized in-place install underway; no phone pass claimed.
  Independent reviewer confirms schema/provenance/read/no-generation boundaries;
  Gemma's actual decline selection remains a phone acceptance gate.

- Reviewed 603-test build installed in place successfully on Samsung SM-S928B /
  Android 16. Existing app identity/private storage and models retained. Member
  unlock and ask-first/No-thanks replay requested; inspect fixed decline event
  rather than assuming a safe generic rejection is semantic success. No private
  capture, saved meal, commit or push.

- Named 603-test ask-first/decline acceptance passes on Samsung SM-S928B /
  Android 16: member confirms a clear decline acknowledgement and no draft.
  Current content-free trace records `meal_confirmation_declined` accepted and
  delivered to Chat. No prompt/food/record/UI data inspected. This is the typed
  decline path, distinct from the earlier blocked draft selection. Remaining:
  correction uses new food, repeated Review preserves explicit grams, skip /
  invalid amount and broader lifecycle/accessibility checks. No save/commit/push.

- Named correction acceptance passes on the same installed 603-test build:
  member changes the offered rice to scrambled eggs, confirms eggs only in
  Review, then Discards. Current metadata shows a fresh native Gemma turn and
  Chat delivery, with no original-source confirmation event for that turn.
  No food/proposal/private transcript inspected; identity is member-observed.
  Remaining bounded portion check: reject unitless quantity, accept explicit
  grams and retain them across repeated Review. Broader gates stay open.

- Intended unitless/repeated-Review check did not reach the portion stage:
  member entered the bare amount at the preceding meal-draft offer. Safe trace
  records `meal_confirmation_used` and delivered draft, with no portion event
  for this reply. No numeric portion application is established. Gemma's
  ambiguous amount-as-confirmation behavior remains a separate open gap;
  unitless portion rejection/repeated Review remain untested on this run.
  Continue using the existing prepared draft at its explicit gram question;
  no build/install/code change or private-content inspection for this observation.

- Completed portion-stage acceptance passes on the installed 603-test APK:
  member confirms unitless reply rejected, explicit 150 g correct in first
  Review and retained in second Review after leaving without saving, then
  Discard. Same request's safe metadata records `portion_rejected` then
  `portion_applied`; no intervening model turn for those local answers. Exact
  amount and repeated-Review state are member-observed, not read from telemetry.
  No meal save, private capture, commit or push. Skip, other unsupported amounts,
  broad lifecycle/visual/accessibility and prior numeric-confirmation ambiguity
  remain explicit gaps. Next: final checkpoint review and commit/push decision.

- Final reviewer found a real route gap: first-turn meal statements containing
  record/log/save could enter legacy intent handling, which lacks the new
  ask-first/pending-action contract. Example is a rice report asking permission
  before recording. This blocks checkpoint proposal despite prior named passes.
- Parent correction reserves legacy Chat text routing for explicit weight
  entry; non-weight meal writes use the approved native draft path. No extra
  ask-first spelling/phrase matcher. Existing read exclusions remain, and food
  portions in kg/lb alone do not qualify as weight entry. Added regression
  assertions for the reported route plus meal/weight/unit cases. Updated review,
  complete gate and phone route acceptance pending. No commit/push.

- Routing re-review confirms original blocker fixed and explicit weight
  commands retained. First gate failed two obsolete legacy meal-route test
  expectations; updated them to native while explicitly checking mixed requests
  still fail read-shortcut eligibility. Final full gate passes 603 tests, zero
  failures/errors/skips, debug/release APKs, release vital lint and diff check.
  Current debug hash in companion spec. Authorized in-place install underway;
  named explicit-record/ask-first phone acceptance still pending.

- Final routing APK installed in place (`Success`) on Samsung SM-S928B /
  Android 16. Existing app identity/private storage/models preserved. Member
  unlock and exact reviewer-repro replay requested; no new physical pass or
  commit/push claimed. No private content or biometric interaction captured.

- Final reviewer-repro phone acceptance passes on current routing APK: member
  confirms question first, clear decline and no draft for the explicit-record
  wording. Safe events confirm native Gemma and accepted
  `meal_confirmation_declined` delivered to Chat, no legacy route in this run.
  Cold launch returned `Status: ok`; existing data/models preserved. No raw
  capture or meal save. Final route blocker closed; automated gate and diff
  check pass. No source change after the passing full gate, only evidence notes.
- Checkpoint ready for explicit commit/push decision. Include portion policy and
  repeated-Review session override, structured confirm/decline, native meal
  routing, fixed monitoring, tests and evidence. Bare numeric confirmation
  ambiguity, skip/other amounts, full lifecycle/Compose/visual/accessibility
  gates remain open. No release-readiness claim and no commit/push yet.

- Member explicitly approved committing and pushing the reviewed checkpoint
  to main on 2026-10-17. Final source gate remains the passing 603-test/debug/
  release/lint run; subsequent changes are acceptance/approval documentation
  only. Diff check clean. Commit identity/remote completion are recorded in Git.
  Remaining bounded task: bare-amount ambiguity at a meal offer, followed by
  skip/lifecycle checks; broader accessibility/integration gates remain open.

### Member follow-up checks — 2026-10-18

- Member reports all three suggested checks pass: bare amount at the draft
  offer clarifies, Skip preserves the estimate, and lock clears pending actions
  while retaining Chat history. Last known installed artifact is the final
  routing APK documented above, based on committed/pushed `7f66f60`.
- Read only the existing bounded technical trace. It contains prior named
  acceptance events and a lock event, but does not independently identify the
  newly reported clarification/Skip scenarios. No exact private dialogue,
  quantities, records, UI dump or screenshot inspected.
- Named passes are member-observed. No code/build/install/model update occurred
  between the earlier ambiguous-numeric failure and this success, so no durable
  semantic fix or broad reliability claim is established. Existing automated
  baseline remains 603 passing tests/debug/release/lint on the checkpoint.
- Documentation-only update; diff check required, no rebuild necessary. No meal
  save or new commit/push. Broader lifecycle/other quantities/accessibility gates
  stay open. Next proposed bounded feature: contextual correction of the current
  draft portion without changing foods or bypassing Review/Save.

### Current-draft portion correction extension — 2026-10-18

- Member reports initial explicit grams correctly updated a still-visible
  draft/Review button, followed by generic no-draft replies to more amounts or
  natural corrections. Safe events show `portion_applied`, then a new native
  Gemma turn with `meal_confirmation_missing`; no private contents inspected.
- Read-only discovery confirms one-shot implementation: answered offer no
  longer parses input, then ordinary submission clears accepted override and
  proposal. This is a missing correction capability, not evidence that the
  first amount was lost. Natural corrections were proposed, not implemented.
- Implement subsequent exact gram edits plus a native typed natural portion
  edit, bound to the same current eligible target/proposal/session. Validate
  explicit whole grams against model action; `022 g` means 22 g. Preserve prior
  amount on rejection and after Review return. No food/confidence mutation,
  history resurrection, dependency, vault change or automatic Save.
- Design states updated before implementation. Worker implementation, review,
  full Android gate and member in-app checks pending. No install/commit/push.

- Initial production implementation frozen and first gate started. Independent
  review found non-edit model replies still discard draft state; parent found
  tool-presence tracking used the initial response after an audit retry. Fix
  retains visible draft/amount on prose clarification or intent miss, while
  explicit read/navigation/new-meal actions retire editing; use final response
  for selection. Runtime decision seam/regression tests in progress. No install
  or acceptance claimed from the first implementation snapshot.

- First gate failed compilation at two root callbacks: an inner Chat unlock
  helper was referenced outside its scope. Use the root's current unlocked-vault
  status there. No APK installed; worker is correcting that bridge together
  with the review findings before the final complete gate.

- Final independent review found no remaining blocker in the changed paths.
  No-tool clarification retains the current draft and amount; explicit reads,
  navigation and genuine new meals retire edit context. Final-response tool
  selection and current unlock checks are covered by the reviewed source.
- Final complete gate passes: 609 unit tests, zero failures/errors/skips,
  debug and release APK builds, and release vital lint. `git diff --check`
  passes. Debug APK SHA-256:
  `aeddee7bf66948e0d2904bbb03e944a17327f79255b311fe143f681302e3a4d7`.
  Physical acceptance of repeated grams, natural correction and Review return
  remains pending; automated checks do not establish Gemma tool selection.
- Installed that debug APK in place on Samsung SM-S928B / Android 16 using
  `install -r` (`Success`), under the active test-cycle authorization. Existing
  app identity, private storage and models preserved; cold launch succeeded.
  Member unlock and fresh repeated-amount/natural-correction acceptance pending.
  No raw dialogue, record capture, meal Save, commit or push performed.
- Member confirms the fresh rice draft accepts 15 g, then 150 g, then natural
  correction to 022 g; both Review visits show 22 g with foods unchanged, followed
  by Discard. Bounded content-free events include accepted `portion_applied` on
  the native Gemma correction request and accepted Chat delivery. Amounts/foods
  are member-observed, not inferred from the trace. No screenshot/private record
  inspection. Invalid-edit retention, Stop/lock during correction and broader
  accessibility gates remain open. No commit/push authorization requested yet.
- Invalid-edit phone check: member confirms natural 2001 g request is rejected,
  Review remains available and retains prior 150 g, then Discard. Safe trace
  records a native Gemma turn accepted into Chat but no dedicated rejection
  event; rejection and retained amount are member-observed. No code change or
  rebuild. Stop/lock during correction and broader accessibility remain open.
- Stop phone check passes: member confirms subsequent today-meals answer is
  correct and no late correction/draft returns after about a minute. Safe events
  show the native correction request cancelled, followed by accepted MiniLM
  `read_meals` for today and Chat delivery; no later delivery from the cancelled
  request in the inspected tail. No raw content inspected. Lock during correction
  and broader accessibility remain open. Documentation-only evidence update.
- Lock phone check: member confirms history retained, new today-meals answer
  correct, no old Review card or late correction after about a minute. Inspected
  trace tail is unchanged from the Stop check, so no fresh matching lock event
  independently corroborates this result. Record it as member-observed only.
- Current checkpoint is ready for explicit commit/push decision: independent
  review complete, 609 tests/debug/release/vital lint pass, diff check clean,
  named correction/retention/Stop/lock phone checks recorded with evidence limits.
  Broader accessibility/lifecycle combinations and pre-draft numeric confirmation
  consistency remain documented gaps. No new commit/push performed.
- Member explicitly approved committing and pushing this reviewed portion-edit
  checkpoint to main on 2026-10-18. Passing source gate remains 609 tests plus
  debug/release/vital lint; later updates are acceptance/approval notes only.
  Git records commit identity and remote completion. Broader documented gates
  remain open; no release-readiness claim.

### Current-draft single-food replacement — 2026-10-18

- Member approved starting one-food replacement and noted its value for voice
  commands. Read-only discovery identifies the existing current-portion context,
  native tool lifecycle and Review name/match controls as reusable seams.
- First slice limited to one simple ordinary Chat food; retain immutable source
  proposal and transient name/amount overlays. Host validates literal replacement
  phrase and current unlocked request/session; no model-selected item/ID or new
  nutrition. Review removes old food matching/context and requires fresh local
  match choice. Explicit final Save remains the only record mutation.
- Design states updated before implementation in companion/FLOWS/SCREEN-SPECS.
  Worker owns code/tests; parent owns docs and final gate. Independent review,
  full Android gate and named phone checks pending. No new dependency, vault
  format, install, commit or push for this extension yet.
- Keyboard dictation shares text handling; in-app Speak recognition currently
  uses a separate action parser and can clear current-draft context. Dedicated
  microphone parity is a later bounded handoff task, not an implemented benefit.
- Production and test snapshots frozen for independent review. New policy and
  native tool bind a literal single replacement to the host's sole current food;
  Review applies a name overlay, clears stale descriptive/matching context and
  requires a fresh local match. Explicit member grams remain; otherwise Review
  leaves grams blank rather than reusing an old-food portion estimate.
- Parent inspection caught punctuation/standalone-label validation mistakes and
  an explicit-operation failure path that could retain old food context; worker
  corrected them before the frozen review snapshot. First complete gate then
  failed compilation because Review lacked two imports for the new policy/types.
  Parent added those imports and restarted the complete gate. No new APK installed
  or phone result claimed. Review/tests/build are separate evidence levels.
- Gate after import fix passes tests/debug/release/vital lint. Independent review
  found a remaining comma-list ambiguity: a single model replacement phrase
  could be accepted from a multi-food comma-separated turn. Fix and regression
  tests are required before installation. Parent also identified local portion
  acknowledgements still using the original label after rename. Production and
  test owners are correcting these separate paths, then final gate/re-review.
- Final-source follow-up review found no material blocker: comma-list ambiguity
  fixed without rejecting intended discourse/courtesy commas; local portion
  acknowledgement names use current request/proposal/session-bound overlay;
  retained explicit grams suppress redundant questioning. The latter coordinator
  behaviors still need named phone acceptance. First repaired gate passed 619
  tests; final complete gate after these fixes is running. ADB currently has no
  phone connection; member reconnect requested. No install yet.
- Final complete gate after review fixes passes: 619 unit tests with zero
  failures/errors/skips, debug/release builds and release vital lint. Diff check
  clean. Debug APK SHA-256:
  `eee16955de9e97c24205578e746e9d8dea18fcc165acb4a8c357815be1b965d6`.
- Fresh ADB post-gate snapshot lists no device. No APK installed for this
  extension yet; existing member data/models untouched. Next bounded task is
  reconnect, in-place install, member unlock and named single-food correction /
  gram continuation / Review return checks. Unsupported/mixed-edit retention,
  Stop/lock, layout/accessibility and dedicated mic parity remain unaccepted.
  No commit/push authorization. Source tree remains buildable with this gate;
  subsequent changes are documentation-only evidence/status updates.
- Member requested reconnect retry. Fresh device list identified Samsung
  SM-S928B; selected one exact transport consistently. Installed the verified
  food-replacement debug APK above with `install -r` (`Success`) and cold launch
  returned `Status: ok`. Existing app identity/private data/models preserved;
  no biometric automation or raw private capture. Member unlock and named phone
  acceptance pending. No code change, commit or push.
- First single-food phone check passes on `eee16955...`: member confirms new
  brown-rice name/title, retained 150 g and no selected match across two Review
  visits, then Discard. Values are member-observed; bounded trace confirms native
  Gemma and accepted Chat delivery, but not the specific food-edit outcome.
- Parent diagnosed missing monitoring event: food replacement reasons were in
  stored-record vocabulary but absent from recordReplyDelivery's entry gate and
  accepted mapping. Added only those fixed reasons and expanded existing restart/
  deduplication/content-exclusion regression test. No new private fields. Full
  gate and in-place monitoring build pending; phone behavior is already observed
  on the preceding build, not inferred from the monitoring change.
- Monitoring fix complete gate passes: 619 tests, zero failures/errors/skips,
  debug/release builds and release vital lint. Debug APK SHA-256:
  `678cd1a811ef5026902cb5c3b3c3e5e70d63990a36fa66d2fd0cb8238920f15c`.
  Existing trace regression now exercises fixed food replacement accepted/rejected
  outcomes through delivery, deduplication and restart, excluding content suffixes.
- Monitoring APK installed in place on Samsung SM-S928B / Android 16 (`Success`)
  and cold-launched (`Status: ok`). Existing data/models preserved. Repeated
  replacement/gram continuation and monitoring replay pending member unlock/test.
  No record save, private capture, commit or push.
- Repeated correction phone sequence FAILED: member reports draft disappeared,
  with initial gram reply the last visible assistant message. Safe trace shows
  draft confirmation and portion application, then correction request 4 native
  Gemma; during inference `conversation_sync_changed` clears Chat request. Its
  later lifecycle result is withheld by `active_request_mismatch` and
  `chat_active_missing`. No specific food-edit outcome. Stale delivery guard is
  correct; do not bypass it. No raw text, private records or screenshots read.
- Read-only discovery and bounded worker assigned hydration provenance fix:
  recognize exact successful same-session transcript writes despite a newer
  in-memory member turn, while unknown changed/removed/expired conversations and
  lock/reset still restore/invalidate. Parent owns docs; worker owns policy/
  persistence/session wiring/tests. No new install or acceptance claimed yet.
- Discovery confirms a policy gap: an exact successful intermediate self-write
  is neither previouslyLoaded nor the newer canonical local transcript, so it
  currently restores and clears request identity. The exact status callback
  exposing this run is unknown: ordinary Chat writes do not update root status,
  and refresh can also originate from lifecycle/other vault updates. Do not
  attribute it specifically to the 60-second timer without matching evidence.
- Hydration worker implemented bounded exact successful-write receipts (8,
  two-minute recognition lifetime), generation invalidation and writer-idle
  synchronization. Unknown changed/removed/expired snapshots retain restore
  behavior; no final delivery guard change. Focused tests cover intermediate
  own writes, normalization/message cap, exact identity mismatch, expiry/reset,
  failed/late saves, receipt bounds and writer ordering.
- First gate caught a compile integration issue: Review's synchronous onSave
  called the newly suspended synchronization function. Split root status refresh
  into suspend synchronizeAfterWrites while preserving existing synchronous
  post-Review-save hydration. Parent/reviewer also found dispatcher cancellation
  could discard a committed save before acknowledgement; moved commit+receipt
  registration into the same IO block before returning to the caller, with a
  cancellation-after-commit regression. Final gate/review pending; no install.
- Gate after compile/cancellation fixes passes tests/debug/release/vital lint.
  Reviewer validated exact receipt lifecycle and existing Review navigation fence,
  then found one restore gap: unknown persisted replacement/removal must invalidate
  already-captured autosave tickets. Otherwise a queued write can overwrite the
  restored conversation. Add invalidation atomically under writer-idle hydration
  barrier, with queued-write regression fixtures; no broadened Save UX needed.
- Final hydration source re-reviewed: root synchronization holds writer mutex
  through classification/invalidation, unknown restore invalidates old tickets,
  exact own-write KEEP preserves current ones. Reviewer confirms no deadlock or
  remaining scoped blocker. IO cancellation-safe receipt registration and existing
  Review navigation fence remain intact. Final complete gate and phone replay
  pending; do not mark the observed repeated-edit failure closed yet.
- Final corrected full gate passes: 628 unit tests, zero failures/errors/skips,
  debug/release builds, release vital lint and clean diff check. Debug APK SHA-256:
  `8dd8080a14fdc41e521e34d0cdf06c995f19855375e4eb0c5590bda3f8ae8f70`.
  In-place install/replay pending. Automated fixtures establish the named policy/
  ordering outcomes, not physical acceptance or proof of exact callback trigger.
- Corrected hydration APK installed in place on Samsung SM-S928B / Android 16
  (`Success`) and launched (`Status: ok`, launch state reported unknown), preserving app identity/private
  data/models. Member unlock and repeated correction / minute refresh / Review
  replay pending. No raw capture, record save, commit or push.
- Replay on corrected hydration build: member reports second rename retains
  brown rice. Trace confirms request 4 `food_replacement_applied`, then local
  `portion_applied`; request 5 native Gemma produces audited prose and Chat delivery
  with no food-edit action and no hydration-reset/mismatched-request events in
  this run. Replacement expectation failed; current draft retention worked.
- Bounded follow-up assigned: align current-food prompt and add one native
  corrective retry only for an eligible no-tool food correction; preserve final
  literal/request validation and truthful unchanged-draft reply if still no edit.
  No specific food/typo rule or raw content inspection. Gate/re-review/replay pending.
- Retry implementation and tests frozen; independent review confirms one strict
  no-tool retry, no extra factual retry loop, final-response action selection and
  truthful retained-draft fallback. Portion prompt and acknowledgement old-name
  use the current display label. Parent/reviewer found food-only fixed-portion
  no-tool completion left dynamic conversation state installed; finalizer now
  retires either food or portion edit context. No authority relaxation.
  Gate before finalizer fix passed; final complete gate/replay pending.
- Reviewer identified one final malformed-selection edge: default disposition
  retired a draft for unknown tool names. Parent changed default to retained
  clarification; raw read names require valid adapter output unless the member's
  host-required read/navigation already ends editing. Valid saved-dish search
  still retires. Added unknown/malformed vs valid-operation regression coverage.
  Final full gate running; no new APK installed for bounded retry yet.
- Cleanup uses validated active edit contexts, not merely the UI's offered raw
  contexts. Required read/navigation paths retire edit authority before model
  continuation; cleanup must not destroy a read conversation waiting for its local
  tool result. No-tool food-only edits still retire dynamic edit state. Final gate
  rerun after this narrow lifecycle predicate correction; phone replay pending.

### Repeated-rename corrective build — 2026-10-19

- Final complete gate after bounded intent retry, malformed-tool retention and
  validated-context cleanup passes: 631 unit tests, zero failures/errors/skips,
  debug/release APKs, release vital lint and clean diff check.
- Debug APK SHA-256:
  `4fb244383218f79be4eae31c06bbf0c67ae0297ef357c1ee841f0850f6ce15bf`.
  In-place installation started under the active test-cycle authorization;
  repeated rename/portion phone replay remains pending. No commit/push.
- Installed that APK in place (`Success`) and launched (`Status: ok`) on Samsung
  SM-S928B / Android 16. Existing app identity/private data/models preserved;
  member unlock and named repeated-rename replay pending. No raw capture or Save.
- 631-test phone replay still inconsistent: member reports brown rice rejected
  twice, basmati accepted. Safe trace includes wrong portion selection/rejection,
  a successful food replacement, and a later dedicated retry ending no-tool/
  `food_replacement_not_selected`. No raw dialogue/private record inspection.
- Assessing conservative explicit label edits locally: same live single-food
  authority and literal frame, admission by complete packaged catalog vocabulary
  plus canonical food-head coverage, no catalog selection or nutrition. Unknowns
  retain native handling. No concrete food/typo exception or further retry loops.
  Design review pending before implementation; parent owns documentation.
- Read-only design review approves full-phrase/canonical-head vocabulary admission
  under current draft authority, with conservative false negatives and unknown
  native fallback. Worker assigned production/tests; no new dependency, model,
  dictionary, catalog selection/nutrition or Save behavior. New flow states are
  documented before implementation. Final gate/review/phone acceptance pending.
- Local correction implementation and independent final review complete. Shared
  submission requires actual model/active/handled request identity and current
  proposal/generation/unlocked/idle authority; only the transient name changes.
  Complete description vocabulary and canonical food-head coverage admit brown
  rice without selecting its raw catalog entry or nutrition. Unknown basmati
  keeps native handling. Review remains unmatched and explicit grams preserved.
  First gate caught a missing required request-ID argument in one test fixture;
  corrected, with complete gate rerunning. No new install or physical pass yet.
- Final local-label complete gate passes 632 unit tests, zero failures/errors/
  skips, debug/release assembly, release vital lint and clean diff check. APK
  SHA-256: `c1b4b5ca92587847df7d4cb29df334fd072bc54920ad146448266002789783d2`.
  Authorized in-place installation underway; local repeated-edit and native
  unknown-food replay remain pending. No commit/push or raw device capture.
- Local-label APK installed in place (`Success`) and cold-launched (`Status: ok`)
  on Samsung SM-S928B / Android 16 under the active test-cycle authorization.
  `install -r` preserved app identity/private storage; member unlock and actual
  repeated-local-edit acceptance remain pending. No raw capture or record Save.
- Member confirms the requested repeated sequence ends in white rice, 200 g,
  no selected match across both Review visits, then Discard. Safe trace confirms
  accepted local food and portion edits on the same request. Fixed delivery
  reasons deduplicate per request, so repeated local events cannot be counted.
  Later native food replacement also completes after bounded retry; no raw text
  was inspected, so its phrase is not attributed. Visible repeated-edit gate
  passes; do not claim both renames independently proved Gemma-free. Next bounded
  check: fresh unknown-catalog basmati correction. Broader lifecycle/accessibility
  and dedicated microphone parity remain open. No commit/push authorization yet.
- Member confirms requested fresh basmati correction: basmati rice, 150 g and
  no selected match, then Discard. Named visible fallback compatibility passes
  by member report. Latest metadata tail contains no distinct new request after
  the earlier native success and lock, so no extra independently attributed
  basmati routing pass is claimed. Production remains the reviewed 632-test APK;
  only acceptance documentation changed. Next bounded task: explicit checkpoint
  commit/push approval; broader physical/accessibility/microphone gaps stay open.
- Member explicitly approved commit and push of this reviewed checkpoint to
  main. Handoff gate remains 632 passing tests, debug/release assembly, release
  vital lint and clean diff check; named Review checks pass with the monitoring
  limitations above. Historical pending-approval notes are superseded. Next
  bounded development task: scope dedicated voice-path contextual-edit parity;
  do not infer microphone acceptance from text or keyboard dictation results.

### Dedicated voice contextual-edit handoff — 2026-10-19

- Member approved continuing after discussion of Moshi/Mini-Omni2. Keep the
  current Gemma 4 E2B/LiteRT stack; no new model/runtime dependency. Baseline
  checkpoint `8e985dc` is pushed to main and starts clean.
- Read-only discovery confirms generic audio replaces the assistant request ID
  and operation, invalidating the original draft and gram binding for later
  text submission. Do not rewind request IDs or relax context validators.
- Approved bounded direction: a separate transient transcription channel keeps
  the original eligible ordinary Chat draft identity, with a one-use capture/
  audio/source/session proof. Existing recognized-words preview and Continue
  submit through ordinary text correction; audio actions have no authority.
  Stop/lock/reset/disposal/newer work revoke; temporary clips always delete.
- Flow/spec updated before implementation. Worker owns production/tests; parent
  owns docs/final gate/install; independent design/implementation review planned.
  Microphone, cancellation, Review retention and accessibility gates pending.
  No new APK installed or commit/push approval for this extension.
- Independent design review agrees separate token/state/result is required:
  preserve the old assistant tuple rather than restoring IDs after generic
  audio. Strict one-key JSON transcript, exact proposal/name/gram/source/session
  binding, delayed permission/clip checks and atomic one-use Continue are required.
  Separate voice Stop revokes/deletes the attempt without cancelling the original
  unsaved draft; shared inference remains busy until actual audio worker exit.
  Worker implementation underway; phone reconnected, no install yet.
- Transcript-only runtime/UI and pure handoff/parser tests implemented. Parent
  compile caught an obsolete duplicate microphone helper, removed by worker;
  app debug/release Kotlin compilation now passes. First full gate caught fixture
  parameter-name mismatches in the new handoff tests; repair pending. Final
  review is examining native worker shutdown, stale Continue/Type instead and
  delayed permission safety. No APK installed or microphone pass claimed.
- Final review found a material terminal-state gap: failed/cancelled recognition
  could leave non-idle state suppressing later Chat delivery, and a previous
  terminal token could invalidate retake. Parent also confirmed a failed consume
  could fall through to normal submission and completed transcript cleanup must
  cover lock/unload. Worker assigned one grouped repair with terminal/retry and
  revoked-handoff regressions, preserving runtime busy ownership until actual
  worker exit. No acceptance is inferred from the earlier partial gate.
- Grouped repair full gate passes tests/debug/release/vital lint. Final review
  confirms terminal isolation/retake, lock cleanup and failed-consume early
  return. One last UX repair is pending: Type instead must preserve a still-current
  one-use handoff for explicit keyboard Send, rather than always dropping draft
  context; stale handoffs still revoke and explain a new-message path. Parent and
  reviewer agree the narrow split preserves authority checks. Rebuild/review of
  that branch precedes installation; no phone microphone pass yet.
- Final Type instead split implemented and independently re-reviewed: current
  proof survives for explicit keyboard Send; stale proof revokes with context
  bypass and explanation. No auto-send. Reviewer reports no remaining scoped
  blocker. Final gate has 640 passing unit tests, zero failures/errors/skips,
  and debug assembly; release/vital lint finishing. Native audio cancellation/
  permission/Review behavior still needs actual Samsung checks. No install yet.
- Final full gate passes: 640 unit tests, zero failures/errors/skips, debug/release
  assembly, release vital lint and clean diff check. Debug APK SHA-256:
  `0c692d5d32085b73e42ad74c10bf28d52a64fe4c19b000b43bb8d68720202096`.
  In-place installation underway on Samsung SM-S928B / Android 16 under the
  existing repeated-install authorization. Member microphone checks pending.
- Verified voice APK installed in place (`Success`) and cold-launched (`Status:
  ok`) on the same Samsung. App identity/private storage preserved by install-r;
  biometric unlock and actual microphone acceptance remain member actions.
  Next bounded check: current rice draft at 150 g, speak brown-rice correction,
  preview/check words, Continue, then unmatched brown rice/150 g in Review and
  return without Save. No raw captures, private-record reads, commit or push.
- First dedicated microphone check failed (2026-10-20): member reports spoken
  brown-rice correction returned unreadable-speech error before preview/Continue.
  Reconnected Samsung safe trace confirms draft/portion acceptance but has no
  voice-stage evidence. The same error exists for contextual parse rejection and
  legacy missing speech; exact path remains unproven. Discovery identified general
  wellness system instructions in the transcription conversation. Replace with
  transcript-only system instructions and add fixed content-free path/rejection
  stages; preserve strict parser and all authority/cleanup guards. Implementation,
  review, full gate and member retry pending. No raw audio/output/records read.
- Focused repair implemented: contextual audio has dedicated transcript-only
  system instructions, no automatic tool calling, unchanged strict parsing with
  fixed failure categories, and an opt-in legacy missing-transcript marker.
  Review found cancelled inference could be misreported as a parser/runtime
  failure; finalization now checks current ID/state/engine/interruption before
  emitting that event. Initial gate caught literal-tab test assertions; repaired.
  Final full gate/re-review running. No replacement APK installed yet.
- Focused re-review has no remaining blocker. Complete final gate passes 642
  unit tests, zero failures/errors/skips, debug/release assembly and release vital
  lint; diff check clean. Debug APK SHA-256:
  `ae9a50b91e80c14e7ddda85f21cbd66a593ceab002421eeb9337294bd530fc44`.
  Authorized in-place installation underway on the same Samsung SM-S928B /
  Android 16. First microphone retry remains pending; no recognition pass claimed.
- Verified APK installed in place (`Success`) and cold-launched (`Status: ok`)
  with app identity/private storage preserved. Member unlock and fresh rice/150 g
  spoken brown-rice preview/Continue/Review retry pending. Existing opt-in trace
  can distinguish contextual parse stages from legacy missing transcript without
  reading raw output or speech. No record saved, commit or push.
- Member reports repeated attempts by two speakers at different speeds yielded
  text only once. Safe trace establishes eight contextual attempts: one accepted,
  seven malformed-envelope rejections, no empty/runtime failure. Formatting is
  the observed failure boundary; ASR accuracy remains unproven. Adopt Google's
  documented plain-text English ASR prompt alongside audio, removing JSON packaging
  only for the contextual preview channel. Host bounds/invalid-output checks,
  explicit member preview/Continue and all action/source guards remain. Decision
  and primary sources recorded in companion bundle; implementation/review/gate/
  fresh phone retry pending. No raw output/audio inspected.
- Plain-ASR repair implemented with explicit English transcription text beside
  audio; preview parser accepts bounded plain words and extracts no structured
  action/JSON fields. Initial complete gate passes. Focused review caught shared
  text validation allowing tabs and pre-trim hiding edge controls; contextual-only
  raw size/control checks and regressions now repair that, with final full gate
  and re-review running. Boundary CR/LF formatting is trimmed; multiple content
  lines remain invalid. No new model/dependency/service or APK installed yet.
- Final focused re-review has no remaining blocker. Full gate passes 642 unit
  tests, zero failures/errors/skips, debug/release assembly and release vital lint;
  diff check clean. Plain-ASR APK SHA-256:
  `dc9ac70b14e34ed578086243be2857c9bf1781bb88bbd3d85aa42d1f09c4428e`.
  Authorized in-place update underway on the same Samsung. Recognition accuracy,
  preview/Continue/Review and broader phone acceptance remain open.
- Plain-ASR APK installed in place (`Success`) and launched (`Status: ok`;
  Android reported unknown launch state). App identity/private storage preserved.
  Member unlock and one fresh rice/150 g spoken brown-rice preview/Continue/Review
  check pending. No raw recordings/output inspected; no record Save, commit/push.
- Member confirms correct spoken brown-rice preview and Review, retaining 150 g
  with no selected match, leaving without Save. Safe tail confirms accepted
  contextual transcription (two completions in the tail) and local food edit on
  the retained source request. No text/audio inspected or per-attempt word claim.
  Named food-edit microphone acceptance passes on the installed plain-ASR APK;
  spoken portion/Review retention and lifecycle/accessibility gates remain open.
- Member confirms spoken 200 grams gives brown rice/200 g/unmatched in both
  Review visits, then Discard. Safe tail confirms a further accepted contextual
  transcription; deduplicated source-turn portion events cannot count repeated
  edits, so no extra event attribution is claimed. Named spoken-portion retention
  passes. Stop/retry, lock and remaining voice/accessibility gates stay open.
- Member confirms Stop while recognizing, retry at 200 g, retained 200 g Review
  and no late 220 g correction after the wait; discarded. Named visible Stop/retry
  check passes. Safe tail has a later contextual start without a result event;
  cancellation is consistent, but retry is not independently established by that
  tail. No stronger telemetry claim is made. Lock/new-Chat acceptance next.
- Lifecycle check failed in a spoken variant: member reports prior Stop test
  discarded all Chat, and after locking/unlocking, speaking the meals-read
  request produced legacy voice rows and a draft named for an earlier amount.
  Safe events show a pre-lock contextual start/lock followed by later reply
  delivery, without enough correlation to establish stale-worker publication.
  Discovery confirms generic Chat mic still dispatches history-conditioned audio
  intents directly; contextual mic alone is preview-only. All ordinary Chat mic
  is now scoped to the same history-free plain transcription and explicit typed
  submit, with one-use capture/session/request proof even without a source draft.
  Stop/lock history retention acceptance reopens. Implementation/review/gate and
  phone retry pending. Requested screenshot caught another app and was deleted;
  awaiting member to leave problematic Metria Chat visible. No raw data retained.
- Authorized Metria screen inspection now confirms earlier correction messages
  are retained, followed by legacy placeholder/recognized-words rows and a draft
  acknowledgement; the Review card is absent. This confirms the reported legacy
  presentation but not which audio yielded it. Temporary image deleted after
  inspection, outside repository. Explorer found no transcript-clear call on
  contextual Stop/dismiss; lock restores encrypted history after clearing memory.
  No full-history-loss root cause or speculative persistence change is claimed.
- All ordinary Chat mic now uses the transcript channel plus one-use session/
  request proof, explicit preview and shared typed submit; eligible drafts add
  the exact source handoff. No ordinary audio intent/placeholder dispatch.
  Processing handles nullable request IDs; Stop/revoke cancel transcription and
  recorder without resetting history. Generation advances before recorder cancel
  so queued clips cannot submit. Independent review and narrow cancellation
  recheck find no blocker. Initial full gate passes; final frozen-source gate is
  running. Pure proof/isolation tests do not replace native all-entry-point and
  lock/Stop phone acceptance. No replacement APK installed yet.
- Frozen-source full gate passes 645 tests, zero failures/errors/skips,
  debug/release assembly and release vital lint; diff check clean. Debug APK:
  `549b9e0a278ef0ed80f8c15d4d116dafea8e2b06fb959baf57e6f8b07ddf299c`.
  Authorized in-place update underway on the same Samsung. Fresh spoken meal
  read without a draft and Stop/lock/restoration phone acceptance remain open.
- All-Chat preview APK installed in place (`Success`) and cold-launched (`Status:
  ok`) on the same Samsung with app identity/private storage preserved. Member
  unlock and spoken read without an active draft are next. No record saved,
  commit or push. Temporary inspection images deleted; raw captures not retained.
- Member confirms fresh ordinary Chat speech yields the correct question preview
  and meals answer without a new draft. Safe events corroborate accepted speech
  transcription, shared typed turn, MiniLM today-meals read, accepted audit and
  delivery. Named generic microphone read acceptance passes on the installed
  all-Chat preview build. Lock/Stop/restoration and other voice gates remain open.
- Member confirms history retained after lock, interrupted preview removed and
  fresh voice answer correct with no old result after waiting. Safe events confirm
  lock then accepted transcription/MiniLM meals read/delivery; no distinct pre-lock
  transcription start is present, so exact native interruption timing remains
  unproven. Named visible lock/fresh-voice check passes. Final-build draft editing,
  Type instead and Stop checks are next; broader lifecycle/accessibility gaps stay.
- Final-build member check passes spoken food correction, Type instead holding
  recognized gram words unsent until explicit Send, and correct unmatched food/
  200 g across two Review visits followed by Discard. Safe metadata corroborates
  two accepted transcriptions and local food replacement; repeated portion events
  deduplicate on source request. Named final-build draft/Type instead check passes.
  Final focused Stop-to-normal-Chat acceptance remains; no source changes.
- Stop-to-typing check failed with an error. Requested screen inspection confirms
  original draft/history retained and a misleading Chat-changed diagnostic after
  explicit Stop followed by Type instead. Image deleted. Confirmed code cause:
  Stop revokes proof; Type instead treats null proof as unexpected stale source.
  Bounded repair distinguishes same-session intentional cancellation for quiet
  typing exit, granting no submission/draft authority. Genuine stale sources keep
  their guard; changed request/session/new capture invalidates the exit receipt.
  Implementation/review/gate and focused retry pending. No commit/push.
- Focused Stop-to-typing receipt implemented and independently reviewed with no
  blocker. Only explicit matching Stop may create it; it permits quiet composer
  exit once, never Continue/action/draft submission. Request/session/lock/new
  capture/disposal invalidate it, runtime ownership still gates Send, and history
  remains untouched. Final frozen-source full gate running; phone retry pending.
- Final complete gate passes 647 tests, zero failures/errors/skips, debug/release
  assembly and release vital lint; diff check clean. Debug APK SHA-256:
  `95295d1f58426526e6f4b8be0b539fa636c54de2c7accc91b0c498db47d49379`.
  Authorized in-place update underway on the same Samsung. Focused Stop/Type
  instead/normal typed reply acceptance remains open. No commit/push.
- Verified Stop-exit APK installed in place (`Success`) and cold-launched
  (`Status: ok`) with app identity/private storage preserved. Member unlock and
  focused cancellation-to-typing retry pending. No source changes after the full
  gate; inspection screenshot deleted and no raw capture retained.
- Member confirms final Stop-to-typing retry: no misleading error, correct typed
  answer, history retained and no late voice result/draft after waiting. Safe
  metadata confirms a voice start without completion followed by typed MiniLM
  today-meals read and accepted audit/delivery. Named focused acceptance passes.
  No source changes since final 647-test/debug/release/vital-lint gate. Ready to
  request explicit commit/push approval for the voice checkpoint, preserving
  broader recognition/Today/native-timing/permission/accessibility gaps. This
  does not complete the release or establish every native cancellation race.
- Member explicitly approved commit and push of the reviewed voice checkpoint.
  Earlier pending-approval notes are superseded. Preserve the documented broader
  acceptance limitations; no source changes or new test claims accompany saving.

### Automatic spoken Chat submission - 2026-10-20

- Baseline `70aa743` is pushed and main starts clean. Member approved replacing
  mandatory ordinary Chat transcript confirmation with Listening/Transcribing,
  recognized member bubble and automatic shared text submission. Keep exact
  one-use capture/session/request/draft authority and Stop/lock/disposal revocation;
  no audio action dispatch, automatic Save, new model/dependency or vault format.
- Approved recovery: latest-member Edit copies text into the focused composer
  for explicit Send as a new turn; history is not rewritten and old actions
  cannot be replayed. Known stages get truthful loading text. Empty/failed
  transcription retains Retry/Type instead. Today meal entry stays separate.
- Design/spec/architecture updated before implementation. Explorer mapped the
  existing preview effect/shared submit boundary; one worker owns Kotlin/tests,
  parent owns docs/gate/device handoff, independent review follows. Automated
  and physical gates pending; no replacement APK or commit/push authorization.
- Design review identified the existing typed DraftWeight direct-save path as
  unsuitable for unchecked ASR. Preserve speech origin in the unlocked session,
  bind it to the submitted request and route spoken weight actions to the existing
  editable weight review before Save. Typed behavior is unchanged. Implementation
  review also caught early provenance clearing on rejected Send; repair must
  preserve the review obligation through busy/fallback paths. Final gate and
  phone checks remain pending; no installation or new acceptance claim yet.
- First complete gate caught a missing weight-review callback in the Chat
  signature (debug/release Kotlin compilation). Review additionally found a new
  failed recording could retain prior recognized words for Type instead; clear
  capture text at the fresh-recording boundary. Grouped repair and final gate
  pending. No failed build was installed and no physical pass is claimed.
- Grouped repair is implemented and independently re-reviewed with no scoped
  blocker. Automatic submit requires captured and fresh loaded/ready status with
  matching transcript/capture before its one-shot claim; busy fallback retains
  session-bound words and weight-review provenance. New recording clears old
  words; Type instead accepts only a matching ready capture. Speech weight actions
  open existing editable weight review, never direct-save. Latest-member Edit
  focuses the composer and waits for Send. Pure tests cover helper gates; no
  Compose instrumentation infrastructure exists and no dependency was added.
- Final complete gate passes 653 tests, zero failures/errors/skips, debug/release
  assembly, release vital lint and clean diff check. Debug APK SHA-256:
  `b4d4c13f3cf5adfa4176a744524aecba0f96398f7dad0837ff3cd3e936d73d2f`.
  On 2026-10-21 member reconnected Samsung SM-S928B for authorized in-place
  installation. Installation/launch and named microphone acceptance pending;
  broader lifecycle, keyboard/font/rotation/TalkBack, Today and recognition gates
  remain open. No record Save, raw capture, commit or push performed.
- Verified APK installed in place (`Success`) and cold-launched (`Status: ok`)
  on the same Samsung, preserving app identity/private storage with install-r.
  Member biometric unlock and first fresh spoken read/automatic-once/Edit-to-
  composer acceptance requested. No acceptance is inferred from installation;
  no source changes after the passing gate and no commit/push authorization.
- Member confirms fresh spoken meals read automatically sends/answers once and
  Edit fills the keyboard composer without sending. Named automatic-submit/Edit
  interaction passes on installed 653-test build. Safe tail shows two accepted
  transcription-to-Chat turns; the latest uses accepted MiniLM today-meals read,
  audit and delivery. The earlier uses Gemma/read_meals; words were not inspected
  and neither is attributed beyond that metadata. User observation, not event
  count, establishes the named once-only UI behavior. No record Save or source
  changes. Spoken draft edits, weight review/no-save, failure/Stop/lock and broader
  UI/accessibility/Today checks remain open. Next: fresh draft spoken edits.
- Member requested icon-only Edit (2026-10-21). Replace the visible label with
  the shared rounded pencil icon in a 48 dp IconButton, keeping "Edit as new
  message" as the accessibility label. Behavior is unchanged. Design contract
  updated; replacement build/install and physical acceptance pending. Earlier
  653-test phone results apply to the preceding text-labelled control.
- Icon-only source gate passes 653 tests, zero failures/errors/skips,
  debug/release assembly, release vital lint and diff check. Debug APK SHA-256:
  `688353d1ad75a86fc823d9998e04ae9747c7e5a07cab1bba08562d78848d6da5`.
  Authorized in-place installation underway; named automatic-submit/Edit check
  passed on the prior build, pencil control and remaining physical gates pending.
- Pencil APK installed in place (`Success`) and cold-launched (`Status: ok`) on
  the same Samsung with private storage preserved. Member unlock/pencil check
  and the previously requested fresh spoken food/portion edit test remain next.
  No source changes after this gate, record Save, raw capture or commit/push.

### Cooking-report confirmation repair - 2026-10-21

- Member's next draft check failed after answering `yes`. Exact source copied by
  member: `i have cooked rice`. Safe trace records native reply delivery followed
  by `meal_confirmation_missing`; no proposal was prepared. Source review confirms
  this wording cannot create the consumption-only one-turn candidate. Broaden only
  candidate admission to completed cooking/preparation reports, preserving native
  confirmation selection, original-source binding and explicit Review/Save.
  Implementation/review/build/install/exact retry pending. The earlier automatic
  spoken draft-edit test remains incomplete; do not mark it passed from this turn.
- Policy/test-only implementation now admits completed cooking/preparation reports
  as transient original-source candidates. Existing consumption behavior is retained;
  native confirmation, stale-source/session rejection and final Review/Save remain.
  Independent scoped review has no material finding. Full gate passes 658 tests,
  zero failures/errors/skips, debug/release assembly, release vital lint and clean
  diff check. Debug APK SHA-256:
  `434af73890bbe705a7f7a9fab832e5d5853fb25e885ddb6c6e3b6a3c416bb3bb`.
  Authorized in-place update underway; exact phone retry pending. No source change
  after gate, record Save, raw capture, commit or push.
- Repair APK installed in place (`Success`) and cold-launched (`Status: ok`) on
  Samsung SM-S928B / Android 16, preserving private app storage through install-r.
  Member unlock/exact cooking-report confirmation retry requested. Physical
  acceptance is pending; automatic spoken edits and weight review/no-save remain
  open. No biometric automation or record save performed.
- Member confirms exact retry: after `i have cooked rice` and the offer, `yes`
  produced rice in Review; member discarded it. Safe events corroborate native
  confirmation via `meal_confirmation_used`, lifecycle-ready and Chat consumption.
  Named confirmation gate passes on the 658-test APK. No source changes after
  build. Next: automatic spoken food/gram edits and retained Review, then spoken
  weight review without Save; broader layout/accessibility/Today gates stay open.
- Member confirms automatic spoken food and portion edits, no Continue step,
  with brown rice, 200 g and no selected match retained in both Reviews; discarded.
  Named visible spoken-edit/Review gate passes on the installed 658-test build.
  Safe trace shows two accepted transcriptions and local food replacement. It also
  contains a native portion rejection in this interval; fixed/deduplicated metadata
  cannot prove the exact spoken words or every amount. Correct final values and
  restoration are member-observed evidence. No source/build change or record Save.
  Next: spoken weight entry must open editable Review and dismiss without Save.
- Member confirms spoken fictional weight opened editable 70 kg Review; dismissal
  left no new history entry. Named spoken-weight confirmation/no-save gate passes
  on the same Samsung/658-test APK. Safe metadata corroborates accepted local
  transcription, legacy intent selection and host handler entry, not the weight
  value or absence of vault writes; those are member-observed acceptance. No source
  changes or new build. Empty/error speech recovery, Today entry regression,
  difficult layout/font/TalkBack/reduced-motion and broader native timing coverage
  remain open. Next bounded check: empty speech recovery into typing.

### Silent manual-stop recording rejection - 2026-10-21

- Member reports two silence tests sent invented messages; background sound was
  present. Safe metadata corroborates two accepted transcriptions followed by
  normal assistant turns, not the audio content. This is a failed recovery gate.
- Source inspection finds manual GemmaAudioRecorder.stop() always returns
  detectedSpeech=true, unlike auto-stop's actual heardSpeech flag. All Chat
  submission paths already reject/delete clips without detected speech. Preserve
  real per-capture evidence on manual Stop; keep existing text/action/Save guards.
  The current peak threshold is only a coarse heuristic and can admit background
  sound. This bounded bug repair is not proof of robust noise rejection. No raw
  audio, transcript or vault content inspected. Implementation/review/gate and
  quiet/background silence plus normal-speech phone checks pending.
- Recorder now uses a fresh thread-visible evidence object per capture and reads
  it for both manual and automatic Clip completion. The peak heuristic remains
  unchanged. Scoped independent review has no material finding. Helper tests cover
  quiet evidence, threshold crossing, monotonic evidence after quiet and reset;
  they do not exercise Android recorder stop/callback races. Complete gate passes
  661 tests, zero failures/errors/skips, debug/release builds, release vital lint
  and diff check. Debug APK SHA-256:
  `008c37179930eb8b32847cc268435a2563bab5de7073e8e01872281c2849b274`.
  No new dependency/model/prompt or source change after gate. Authorized in-place
  install and quiet/background/ordinary-speech physical retry pending; the reported
  background false positive remains open. VAD research/limits recorded in the
  companion bundle; no VAD model or runtime dependency adopted.
- Verified recorder APK installed with install-r (`Success`) and cold-launched
  (`Status: ok`) on Samsung SM-S928B / Android 16. Private storage preserved;
  biometric unlock is the member's action. No new phone acceptance yet. Request
  quiet manual-stop silence, then same background condition, followed by a normal
  spoken read. No raw audio/transcript captured, record Save, commit or push.
- Member confirms quiet manual-stop silence and the previous background condition
  are both rejected, while normal speech works. Named observed silence/speech retry
  passes on the Samsung/661-test APK. Safe metadata includes an accepted ordinary
  transcription/Chat delivery and a contextual_no_text rejection, but cannot bind
  words/conditions or show that every silent clip was rejected before inference.
  Do not claim a robust VAD, all-noise rejection or complete recorder timing coverage.
  No source changes after gate. Next: dedicated Today Speak regression, then
  checkpoint review; visual/font/TalkBack/reduced-motion gaps remain open.
- Member confirms dedicated Today meal-section Speak opens rice Review with the
  correct section selected; Discard preserves Chat history. Named Today regression
  passes on the same Samsung/661-test APK, by member observation. No source changes
  after full gate. Final integrated review running across current uncommitted voice,
  inline activity and repair changes. Broader noise/native/recorder timing, error/
  permission, visual/font/rotation/TalkBack/reduced-motion gaps remain documented;
  this is not a release-complete gate. Commit/push still requires explicit approval.
- Final independent integrated review found no confirmed material defect introduced
  across automatic submission/provenance, spoken-weight Review, confirmation source
  binding, voice result isolation/wakeup, recorder evidence and inline cancel/Type
  handoff. No source changes after the 661-test/debug/release/vital-lint gate.
  Final diff check clean. Current development checkpoint is ready for explicit
  commit/push approval, with the above broader acceptance gaps retained. Nothing
  staged, committed or pushed for this checkpoint yet.
- Member explicitly approves commit and push of this reviewed 661-test checkpoint.
  This supersedes its pending-approval notes. A newly reported follow-up remains
  open: Chat `I had rice, beand and chicken breast for dinner` creates correct food
  drafts, but Dinner is not preselected/saved without later member editing. No
  dinner-label implementation is included in this approved baseline. Commit/push
  it first, then investigate/fix the separate metadata handoff with review/gates.

### Explicit Chat meal-type handoff - 2026-10-21

- Approved 661-test voice baseline committed/pushed to main as `5919898`; working
  tree was clean after push. Member separately reports `I had rice, beand and
  chicken breast for dinner` produces correct foods, but Dinner is absent on the
  saved meal until manually changed. No private record inspection was needed.
- Source map: Review initial type receives only Today pendingMealSlot; Chat's
  validated food proposal carries no literal member type. Add optional host-only
  proposal metadata from the exact description, not model output or clock. Keep
  strict model schema, Today section priority, member-editable Review and final
  Save. Native confirmation must use its retained original meal description.
  Implementation/review/build/install and phone acceptance pending. No approval
  yet to commit/push this separate follow-up.
- Implementation attaches host-only memberMealSlot after strict parsing in native
  and direct text generation, using the exact source (including retained meal
  confirmation). Review resolves valid Today section first, otherwise host type;
  both initial state and exit snapshot agree. Existing member edits/clear and Save
  are unchanged. Pure tests cover the reported typo sentence, absent/conflicting/
  negated/long-negated text, stale metadata reset, retained proposal copies, valid
  Today precedence and rejection of model-injected host fields. Narrow unsupported
  or mixed phrasing abstains, including breakfast-food/type ambiguity.
- Independent scoped review and final re-review found no material defect. Full
  gate passes 669 tests, zero failures/errors/skips, debug/release assembly, release
  vital lint and clean diff check. Debug APK SHA-256:
  `10d519f8eadc223f94764d6a502269bc1c072ebc1cec40ebc0c33214c1ec24dc`.
  No source changes after gate. Authorized in-place install underway on Samsung;
  explicit Dinner prefill/portion edit/member type change phone checks pending.
  No record Save, raw capture, new dependency or commit/push for this follow-up.
- Verified meal-type APK installed with install-r (`Success`) and cold-launched
  (`Status: ok`) on Samsung SM-S928B / Android 16, preserving private storage.
  Member unlock and exact Dinner Review retry requested; physical acceptance is
  pending. Check portion edits retain the type and an explicit member type change
  stays selected; Discard afterward to avoid recording a duplicate test meal.

### Chat drink quantities - 2026-10-24

- Prior drink checkpoint committed and pushed as `ee36df2` with member approval.
  Member approves the next typed/spoken mL and measured-serving slice. Discovery
  confirms grams-only Chat handoff beside existing source-backed Review conversion.
  Extend host-owned initial quantity and current-draft overrides, preserve chosen
  units and explicit-quantity precedence, and share the existing voice/text path.
  Missing/ambiguous source evidence must retain the previous draft with feedback.
  Design, scope and physical test plan: `drink-portions.md`. No new Save authority,
  dependency or production network request. Implementation/review/build and all
  new phone acceptance pending. ADB currently lists no phone; reconnect requested.

- Chat drink implementation and independent review complete. Host-parsed quantity
  metadata/snapshot overrides reuse the packaged conversion, same Review food and
  request/session identity; initial quantity warning and unchanged-draft errors
  preserve review. Root found/fixed fixed-range gram prefill and singular glass
  parsing with regressions/follow-up review. Missing import from first compile
  repaired. Final frozen-source gate passes 710 tests (zero failures/errors/skips),
  debug/release builds, release vital lint and diff check. Debug SHA-256:
  `49924366d169af0b9062f5e94037d7229ba2f7f166ee16b2110cad2a4c84ce8d`.
  Samsung reconnected; install-r underway. New phone checks all pending. No new
  dependency, member Save, private capture or commit/push. Trailing food names in
  quantity corrections remain unsupported; use the closed amount phrase.

- Verified 710-test APK installed with install-r (`Success`) and cold-launched
  (`Status: ok`) on Samsung SM-S928B / Android 16, preserving app data. Member
  initial 200 mL coffee / half-cup Review check requested; physical acceptance
  remains pending. Unlock is a member action; no private capture or record Save.

- Member confirms initial coffee Review shows 200 mL + Breakfast, and subsequent
  "Make that half a cup" Review shows 0.5 of the listed cup, on the installed
  710-test build. Named typed phone gates pass by member observation. Safe trace
  confirms initial saved-dish probe/no-match/host drink drafting and delivery;
  amounts are not logged, and inspected tail has no separate portion-applied
  event. No private capture. Next: spoken mL correction and repeated Review;
  gram switching, invalid retention and broader physical gates remain open.

- Spoken check reported failed. Authorized Android CLI flat/no-idle inspection
  finds consumption statements with a different amount instead of the requested
  correction; safe trace shows accepted ASR, portion rejection, then a new drink
  draft. No screenshot/raw device artifact committed. Original spoken amount
  requested to distinguish recognition from intent/conversion. No code change or
  spoken-quantity pass established from this report.

- Member clarifies they may have spoken "thousand" accidentally. No ASR defect
  confirmed. Fresh typed coffee plus spoken 100 mL correction requested; new
  spoken acceptance remains open. No source change or reinstall required.

### Drink quantity units - 2026-10-22

- Member explicitly requests mL and serving sizes for drinks with correct nutrition
  conversions after asking about a coffee draft. Scope/decision and test matrix:
  `drink-portions.md`. Review and saved-meal editing should preserve chosen units
  in encrypted meal/saved-dish/recovery metadata, while canonical grams continue
  to drive deterministic calculation. No assumed water density or automatic Save.
- Catalog inspection finds measured cup masses for some milk/juice references,
  but no packaged coffee and no volume retained by the adapter. Acquire only
  official USDA brewed coffee/espresso references with explicit volume wording;
  preserve original data and source conversion provenance. Core, public data and
  UI work delegated with disjoint files. Existing 669-test meal-type work remains
  uncommitted and must be preserved. Review/full gate/install/phone acceptance
  pending; no new dependency, private-data capture or commit/push performed.

- Drink implementation complete. Optional encrypted quantity metadata is bounded
  and tied to the selected source food; deterministic nutrition still uses grams.
  Shared Review/saved-meal controls offer source-backed mL and measured servings.
  Rebinding permits a new catalog release only with the same conversion basis;
  unsupported old sources retain grams with a warning. Scaling rejects an invalid
  selected-unit result rather than silently dropping the unit. Reviewer found no
  remaining blocker after these fixes. Full gate passes 682 tests (zero failures,
  errors or skips), debug/release builds and release vital lint; importer tests
  pass 3/3 and diff check passes. Debug APK SHA-256:
  `f6f55204921f959e1ff38f60031aadcb34939b3ab9a9c7ab62a67821936dd592`.
  ADB lists no phone, so this APK is not installed. All new physical unit, saved
  persistence/recovery, keyboard/accessibility and source-upgrade gates remain
  pending. Chat/voice mL corrections are outside this Review/editor slice. No
  private capture, record Save, commit or push performed.

- Wireless pairing and discovered debugging connection succeeded. The verified
  682-test drink APK installed in place (`Success`) and launched cold (`Status:
  ok`) on Samsung SM-S928B / Android 16. No data clear/uninstall, biometric
  automation, private capture or record Save occurred. Member coffee-unit Review
  acceptance requested; no new physical interaction pass claimed yet.

- Member confirms coffee appeared in records without Save after a Chat food
  report. Discovery finds no automatic assistant write path, so cause remains
  unresolved. Added opt-in debug-only fixed-origin events after successful vault
  insert/edit persistence, without payloads/IDs/amounts; observer failures cannot
  affect saving. Independent review finds no blocker. All 684 tests, both builds,
  release vital lint and diff check pass. Diagnostic debug APK SHA-256:
  `2a1a101a083d02e3b96349eba3d84d4fd0849ac3f92f4d1e6cd20ce81fd7fb3b`.
  Phone disconnected and reconnection refused; diagnostic APK not installed.
  No existing record altered, no automatic-save UX approved by this diagnostic
  change, no cause or physical gate claimed. Next: reconnect/install, repeat the
  exact coffee message without Save, inspect only fixed write/routing metadata.

- Member sets aside the apparent auto-save report as possibly mistaken; no further
  reproduction requested. The 684-test build installed in place and launched cold
  on the same Samsung. Member then requests learning drink unit/portion for future
  drinks and reports long cup decimals. Discovery confirms active Review and
  saved editor persist the snapshot; reuse for new proposals was absent. Added
  source-bound same-name/food defaults derived from existing confirmed meals,
  host permission only for unspecified single-drink text, visible reuse note and
  override precedence. Saved dishes/photo/manual/mixed foods retain own values.
  Concise approximate display retains exact canonical mass until actual editing.
  Review caught colloquial explicit quantities (couple/pair); guard/tests fixed,
  re-review has no blockers. One missing import and an incorrect test expectation
  for an exact tiny decimal were fixed before the final gate. Final gate and
  physical saved/new-coffee round trip still pending; no auto-save behavior added.

- Final remembered-drink gate passes 691 tests (zero failures/errors/skips),
  debug/release assembly, release vital lint and diff check. Frozen APK SHA-256:
  `981341e8d6327c49b19705480575f11c429855d9070e636adefb5f9e1a6b27c7`.
  Installed in place (`Success`) and cold-launched (`Status: ok`) on Samsung
  SM-S928B / Android 16 with storage preserved. Member existing-coffee Save/reopen,
  new single-coffee default and concise serving display acceptance pending.
  Broader mixed-drink learning, voice mL edits, physical recovery/catalog-upgrade,
  rotation/font/TalkBack and keyboard gates remain open. No auto-save/record write
  by automation, private capture, commit or push.

- Exact coffee retry on the 691-test build produced no draft. Safe trace confirms
  MiniLM saved-dish probing then no-match fallback and conversational Gemma reply.
  Added closed literal drink-consumption no-match route into the existing unsaved
  structured task; read/edit/selected/confirmation scopes retain priority. Member
  confirms exact text "I had a coffee for breakfast"; it is covered. Unknown and
  ask-first wording stays normal. Independent review finds no lifecycle blocker.
  All 694 tests, both builds, release vital lint and diff check pass. Debug SHA-256:
  `21a248d16cf89bd73951159d83551fce53209b163e9916f333e86569cbc4e60a`.
  Installed in place (`Success`), cold-launched (`Status: ok`) on the same Samsung;
  exact draft/portion phone retry pending. No auto-save, private capture or commit.

- Member confirms exact "I had a coffee for breakfast" retry works, including the
  requested saved-unit/portion Review check. Safe trace confirms accepted MiniLM
  saved-dish probe, no-match continuation, guarded `host_drink_draft`, lifecycle
  ready and Chat consumed. No portion/private text read; quantity acceptance is
  member-observed. Named no-draft regression passes on the 694-test installed APK.
  Explicit override/ask-first and wider recovery/accessibility gates remain open;
  no new general routing, automatic Save, commit or push acceptance is inferred.

- Member confirms active coffee draft explicit "150 g" correction overrides the
  remembered portion in Review. Safe retained metadata shows `portion_applied`.
  This named override gate passes on the 694-test build; no private quantity read
  or generalized mL/serving Chat-parser acceptance inferred. Next: ask-first/
  decline regression, then review this uncommitted checkpoint and retain gaps.

- Member confirms the focused coffee ask-first/No-thanks regression works on the
  installed 694-test Samsung build: asks first, clearly acknowledges decline and
  leaves no draft. Member-observed acceptance; no additional private capture.
  Named coffee/default/gram-override/decline checks pass. Wider saved-record,
  readable-cup, recovery and accessibility gates remain open in drink-portions.md.
  Documentation-only acceptance update; no commit/push approval yet.

- On 2026-10-24 the member explicitly approved committing and pushing the reviewed
  drink checkpoint. Named checks pass; wider physical gates remain documented.

### Continuous in-Chat activity - 2026-10-21

- Member requested research/unification of the voice loading popup and Chat
  loading, and explicitly chose inside Chat. Primary Material/Apple/Compose
  sources, alternatives and decision recorded in `design/chat-activity.md`;
  flow/spec updated. Keep one inline activity surface and stable botanical mark,
  truthful stage, indeterminate progress and accessible controls. No fake progress,
  new dependency, model or persistence change.
- Ordinary voice no longer replaces Chat with the modal. One keyed activity row
  prioritizes voice, stopping, meal preparation and normal thinking. The same
  shared card styles voice and typed work; composer cannot compete during voice.
  Cancel stays in Chat; Today meal-entry modal stays separate. Proofs and automatic
  submission/weight Review are unchanged. Type instead requests keyboard focus,
  uses only matching current transcript text and quietly exits before capture.
- Added presentation regression for overlapping activity flags. Independent
  review/full Android gate running; installation, visual/layout/accessibility,
  Stop/lock and spoken draft-edit acceptance remain pending. No record Save,
  raw device capture, commit or push performed.
- First compile caught the existing Compose version requiring FlowRow opt-in;
  added the local annotation without a dependency change. Review found busy
  model-ready Chat could open voice and hide its Stop; mic now rejects busy
  starts, with a focused regression, while recording can always Stop. Text input
  is disabled only during inline voice; Type instead focuses it. Scoped re-review
  has no remaining blocker. Cancelled native voice work also presents Stopping
  after Type/Cancel until release. Final frozen-source gate running; no new APK
  installed or acceptance claimed yet.
- Final gate passes 655 unit tests, zero failures/errors/skips, debug/release
  assembly, release vital lint and clean diff check. Frozen debug APK SHA-256:
  `21d96defc6433634c5628114cdbfc51eef18972a84a68eba7948fe527c24f8be`.
  Authorized in-place installation underway on Samsung SM-S928B / Android 16.
  New continuous voice/typed activity, Type instead keyboard, Stop/Cancel/lock,
  draft Review, weight review/no-save, visual/font/TalkBack/Today gates pending.
  Prior automatic-submit/Edit evidence does not establish the new layout.
- Verified continuous-activity APK installed in place (`Success`) and
  cold-launched (`Status: ok`) on the same Samsung, preserving app identity/private
  storage. Member biometric unlock, fresh spoken read with no popup/one activity,
  and Type instead keyboard/no-stale-text check requested. No source changes
  after final gate. New-layout visual/interaction acceptance remains pending;
  no raw screen capture, record Save, commit or push.
- Member confirms one inline spoken flow with no popup and Type instead opening
  the keyboard without old words. Named continuous-presentation/typing check
  passes on the installed 655-test build. Latest safe metadata confirms accepted
  transcription, ordinary Chat, native Gemma selection, accepted audit/delivery;
  no record-read tool appears for that turn, so do not claim MiniLM routing or
  factual meal correctness from this visual check. No words/output inspected.
  Stop/lock, food/gram Review, spoken weight review/no-save and broader visual/
  accessibility/Today gates remain open. Next: inline transcription Stop to typing.
- Inline Stop/Type check partially failed: member reports cancellation/typing
  work, but the typed request remained Thinking. Safe metadata confirms two voice
  starts without completions, then accepted/reconciled typed MiniLM today-meals
  read and tool_required waiting, with no continuation/audit/delivery. This is not
  a successful post-cancellation read. No words or private records inspected.
- Read-only discovery confirms all four voice-isolation-gated Compose result
  effects omit that guard's inputs from their effect keys. A metadata-only clear
  can leave an already-blocked tool/reply/failure effect asleep. This fits the
  trace; precise original ordering is not proven. Repair uses one immutable key
  for both effect identity and isolation across tool/reply/failure/diagnostic.
  New accepted idle native text turns retire terminal voice metadata, while
  active workers remain rejected. Current-request/handled gates stay unchanged.
- Added pure key-transition/one-delivery regressions and fixed content-free
  voice_result_isolated/host_tool_handler_entered monitoring stages. These do not
  establish real Compose scheduling or native cancellation. Independent review,
  full gate, in-place update and exact phone retry pending; no new acceptance or
  commit/push. Continue broader gates only after this focused retry passes.
- Independent scoped review finds no material defect in coherent effect keys,
  guarded terminal retirement or fixed trace markers. Complete final gate passes
  657 tests, zero failures/errors/skips, debug/release assembly, release vital
  lint and clean diff check. Debug APK SHA-256:
  `ee4d11d06b349fc280eb2a035dafb29d6ee2940a95fd4292056127d9d62cd3b4`.
  In-place update underway on the same Samsung. Physical Stop→Type→typed-read
  retry remains pending; helper tests do not prove the native timing or original
  failure cause. No source changes after gate; no record Save or commit/push.
- Verified repair APK installed in place (`Success`) and cold-launched (`Status:
  ok`) on the same Samsung with private storage preserved. Member unlock and
  exact inline Stop→Type instead→typed meals-read retry requested. No new phone
  acceptance claimed; monitoring now distinguishes guarded tool from host handler.
- Member confirms repaired Stop→Type→typed-read sequence finishes with correct
  records and no late voice result/draft after the wait. Safe trace corroborates
  voice start without acceptance followed by typed MiniLM today-meals routing,
  host_tool_handler_entered, accepted audit, continuation and Chat delivery.
  Named visible post-cancellation read gate passes on the installed 657-test APK.
  This supports the repair without proving the earlier exact race timing. No
  source changes. Next: inline lock/unlock cleanup/fresh request; draft food/gram,
  spoken weight review/no-save, visual/font/TalkBack/Today gates remain open.
- Member confirms inline lock/unlock check: history retained, interrupted activity
  gone, fresh typed answer correct and no old voice result/draft after the wait.
  Safe trace shows transcription start, lock with no accepted transcription, then
  fresh MiniLM today-meals read/host handler/audit/continuation/delivery. Named
  visible lock/fresh-request gate passes on the 657-test APK; this does not prove
  every native shutdown timing. No source changes. Next: automatic spoken food/
  gram draft edits and retained Review; spoken weight review/no-save and broader
  layout/accessibility/Today gates remain open.
