# Metria product plan

Demo checkpoint (2026-10-27): member explicitly schedules the remaining assistant
physical acceptance after the hackathon demo and authorizes commit/push of pending
changes. The actionable checklist is `m4-assistant-checkpoints.md`, section
Post-demo acceptance backlog. Deferred gates stay open; this is not full release
acceptance. Existing no-save verification limits remain in force.

Assistant QA fixtures (2026-10-27): three new debug capture-state previews reuse
production loading/partial/error/source controls with invented data and inert
callbacks. Independent review, full 749-test/debug gate and three visual renders
pass; no installation or inference required. Header Close separately verified
on installed font-fix APK. Real capture failure/partial/recovery and gestures
remain open; member explicitly defers TalkBack. No records changed. Evidence:
`m4-assistant-checkpoints.md`, `testing/assistant-previews.md`.

Assistant font/async continuation (2026-10-27): physical Stop returns to the
intentional empty capture and no late draft was seen in the observed wait.
Reselection/new-analysis/Review/Leave succeeds without saving. A foreground
font-change restoration defect was found; bounded fontScale configuration fix
passes independent review and the full 749-test/debug gate and is installed in
place. Corrected-build phone checks pass retained photo at 150%, fresh analysis,
Review action reachability and retained Review when restoring font 1.0. Original
font restored; no records changed. Home/reopen yielded a protected capture;
member lock confirmation is pending. Partial/error, TalkBack/gestures and broader
assistant font/IME gates remain open. Evidence: `m4-assistant-checkpoints.md`.

Progress evolution (2026-10-27): member approves selectable 7-day, 1-month,
3-month and 1-year charts, with 3 months initially for weight and 7 days for
nutrition, per-metric restored selection and a larger read-only detail.
Implementation complete; independent review and final unit/debug gate pass
749 tests with zero failures/errors/skips. Five affected synthetic renders
visually inspected, including empty/one-point, annual partial nutrition and
narrow large text; range wrapping found and repaired. Physical acceptance and
interaction remain pending. Follow-up: member authorized in-place installation
on Samsung SM-S928B / Android 16; final gated APK installed successfully and
cold-launched (2,938 ms). Read-only phone checks pass range/default switching,
per-metric period retention, partial nutrition marks, point/arrow selection
and Back retaining date/range; existing weight/nutrition data observed. A detail
entry scroll defect was found, fixed and gated (749 tests) then installed in
place and cold-launched (2,339 ms). Corrected-build phone follow-up passes detail
entry, portrait/landscape selection restoration, 150% physical font, pager reach
and older-page navigation, and All history Weight/Meals inspection. Font 1.0
and free rotation restored; no records changed. TalkBack, gesture navigation
and page-index configuration restoration remain separate open gates.
No record writes in this slice.
Contract and checkpoint: `design/progress-evolution.md`. Existing Today See all
entry and All history editing remain. No record writes are authorized for QA.

Assistant prototype completion (2026-10-26/27): direct searchable saved-dish
selection into unsaved Review, contextual label category/atomic Chat receipt,
and atomic update receipt for saved meals opened from Chat are implemented.
Independent review, five affected synthetic renders and full 745-test/debug gate
pass. Final APK installed in place. Phone verifies list/search/no-match/keyboard
and saved-dish selection/cancel; two discovered review copy/inset defects are
fixed and verified in portrait/landscape on final build. Original rotation
restored. No agent record Save/edit/delete. Contextual label default/change/clear,
rotation and Review re-entry, portrait keyboard and landscape final-action
reachability pass on the final APK; test capture discarded back to retained Chat.
Real receipt success needs member Save/update. Broader
accessibility/async gates remain open. See
`design/assistant-context-panel.md` and `m4-assistant-checkpoints.md`.

Assistant save follow-up (2026-10-26): contextual meal Save now atomically records
the Chat acknowledgement/View meal, including capture as the first conversation
action. Assistant Review preselects an editable local-time meal category after
explicit Today/member choices. Existing records unchanged. Two worker changes,
independent review, 743-test/debug gate and saved-receipt preview render pass.
Updated in place on reference Samsung and cold-launched successfully. Physical
default/change/clear category and cleared-state rotation checks pass; original
default and rotation restored without saving. Member subsequently saved and
confirms correct category; physical inspection verifies Chat acknowledgement,
View meal opening the same record and return without changes. Empty/expired
conversation and failure paths remain separate physical gaps.
Details: `m4-assistant-checkpoints.md`.

Synthetic previews (2026-10-26): nine debug-only assistant/review previews now
reuse actual components with fixed invented data and inert save callbacks.
Full unit/debug gate passes 738 tests; independent review and diff check pass.
Studio follow-up rendered and visually inspected all nine previews successfully;
no blocking overlap/truncation observed in their initial viewports. Interactive
scrolling and native-window/IME acceptance remain open. No install or phone
interaction in this slice. Usage and exact observations: `testing/assistant-previews.md`.
Prior live draft was lost on phone lock; physical gaps below remain open.

OCR continuation (2026-10-26): paired-row column boundaries preserve unknown
left-column fields; the subsequent actual-label check exposed detached numeric
lines with multiple values. Their element boxes now reach column selection.
Independent review and full Android test/debug gate pass (738 tests).
The formerly failing dual-column image now passes on the phone with all four
fields from the same per-serving column. Keyboard/landscape and camera/picker
cancellation were checked on that APK. Meal analysis and accessibility checks
continued: real-photo draft, shared dish editor and return without saving pass.
A bottom system-bar overlap found in review has an assistant-entry-only repair;
its 738-test/debug build passes. On that APK, portrait Review footer and keyboard
checks pass. Follow-up on the same installed APK also passes Review landscape,
dish-editor portrait/landscape footer, portrait lower-field keyboard and Cancel
return with draft retained. Original rotation restored; no record saves.
Stop, partial/error and accessibility checks remain pending. No record saves
are authorized in this cycle.
See `m4-assistant-checkpoints.md` for APK identity and acceptance gaps.
Member prioritizes visual-debugging research before further phone checks.
Repository-local guidance is in `skills/metria-visual-qa/SKILL.md`, linked from
`AGENTS.md`; tooling audit and proposed infrastructure are in
`testing/visual-debugging.md`. No app changes or new physical passes from research.

OCR follow-up (2026-10-25): geometry-based consistent-column extraction and
ambiguous/unit/subtotal regressions implemented; complete Android gate passes
730 tests. The assistant landscape inset fix is included. New-build physical
OCR/landscape acceptance remains open; see `m4-assistant-checkpoints.md`.

Assistant panel follow-up (2026-10-25): integrated photo/label capture, inline label
review and options Privacy/Lock implemented. Full Android gate passes 718 tests;
physical acceptance remains open. See `design/assistant-context-panel.md` for the
exact scope, APK identity and remaining capture/review/keyboard/rotation checks.

## Active assistant panel - 2026-10-24

Member approved implementation of only the contextual assistant panel, retaining
the current icon and all existing functions, with no redesign of other screens.
Pre-existing Chat drink quantity work below is preserved. Design, scope and
acceptance evidence: `design/assistant-context-panel.md`. Final debug test/build
gate passed (714 tests); physical acceptance remains pending.
Do not treat the interactive prototype or a passing build as phone acceptance.
Member rejected the initial installed styling as unlike the prototype. A visual
correction now uses inline actions, a unified composer, soft messages and the
existing navigation dock. Its 714-test/debug build gate passes; fresh visual
acceptance is pending. See the panel specification for the corrected APK hash.

## Active Chat drink quantities - 2026-10-24

Drink-unit checkpoint `ee36df2` is committed and pushed. Member approves typed
and spoken mL/measured-serving quantities in ordinary Chat: both an explicit
single-drink report and a correction to its live unsaved draft. Use the same
source-backed conversion and canonical grams as Review, preserve the chosen unit,
and keep explicit current quantities above remembered defaults. Missing or
ambiguous serving/conversion evidence must explain the limitation without changing
the previous draft. Existing request/session/voice guards and final Review/Save
remain authoritative. No new dependency, automatic Save or persisted preference.
Implementation, review, automated gate and phone acceptance are in progress;
see `drink-portions.md` for the bounded scope and test plan.

Implementation and independent/follow-up review pass. Final gate passes all 710
tests, debug/release assembly, release vital lint and diff check. Samsung is
connected and authorized in-place installation is underway; new initial mL,
repeated serving/gram and spoken correction checks are not yet accepted. Closed
correction phrases omit trailing food names; unsupported phrases retain the draft.

Verified APK installed in place and cold-launched successfully on Samsung
SM-S928B / Android 16. Member unlock and initial mL/half-cup phone check requested;
new physical acceptance remains pending.

Member confirms initial coffee 200 mL + Breakfast and subsequent half-cup Review
checks pass. Safe trace corroborates guarded initial drink drafting, not amount
values. Spoken correction, gram switching/invalid retention and broader physical
gates remain open. Next focused check: spoken mL edit and repeated Review.

Spoken check reported failed. Authorized visible-layout inspection shows different
consumption wording/amount from the requested correction; trace corroborates
portion rejection then a new draft. Await original spoken amount to distinguish
ASR from quantity/intent behavior. No converter change or spoken pass claimed.

Member may have said "thousand" accidentally. Retry a fresh typed coffee draft
with spoken one-hundred-milliliter correction; recognition/conversion acceptance
remains open, with no code fix inferred from the prior test.

## Active drink portions - 2026-10-22

Active follow-up: member requests remembered drink unit/portion and readable cup
decimals. Implement derived same-drink defaults from confirmed saved meals only,
with host permission for unspecified single-drink text and visible editable reuse.
Preserve explicit quantities and source conversion, no new preference payload.
Shortened approximate display keeps exact mass until actual text entry. Review,
all 691 tests, both builds, release vital lint and diff check pass. Saved/new-coffee
phone checks pending; `drink-portions.md`.

691-test phone check stopped before Review: a coffee report received only
conversational prose. Trace confirms saved-dish no-match handoff then Gemma reply.
Guarded plain-drink no-match routing now invokes the existing unsaved structured
draft task. Review, all 694 tests, both builds, release vital lint and diff check
pass; installed in place and cold-launched on Samsung. Member confirms exact
coffee draft and saved-portion Review retry works; safe trace confirms guarded
host drink drafting. Member also confirms an explicit 150 g correction wins over
the remembered portion; safe metadata shows portion_applied. Member confirms the
focused coffee ask-first/No-thanks regression also works: asks before drafting,
acknowledges decline and leaves no draft. Broader physical gates remain open.
No auto-save authority added.

Member explicitly approved committing and pushing this reviewed checkpoint on
2026-10-24. Named coffee checks pass; broader physical gates remain documented.

Latest follow-up: member reports a coffee record created from Chat without Save.
Code discovery finds only explicit-save insertion paths; do not dismiss the
observation or claim an auto-save feature. Added content-free post-persistence
insert/edit diagnostics. Independent review, all 684 tests, both builds and
release vital lint pass. Member withdrew the report as possibly mistaken and
asks to move on; no reproduction or cause is claimed. The 684-test APK is now
installed in place and cold-launched; resume drink serving/unit acceptance. See
`drink-portions.md` for artifact identity and evidence limits.

Member approved mL and measured serving sizes for drinks with correct nutrition.
Implement source-backed conversion alongside the existing canonical gram mass;
persist the chosen amount/unit and its catalog conversion in reviewed meals and
saved dishes, with backward-compatible encrypted/recovery reading. No generic
1 mL = 1 g, no clock/model-derived density and no automatic Save. Missing reliable
volume data retains grams/measured servings and a clear unavailable explanation.
Review and saved-meal editing share the controls; source changes invalidate old
conversion authority. Add bounded official USDA coffee/espresso data to make the
reported coffee case usable, with reproducible importer and source attribution.
Existing uncommitted 669-test meal-type fix is preserved. Implementation and
independent review are complete: all 682 Android tests, debug/release builds,
release vital lint, three beverage-importer tests and diff checks pass. The new
APK is installed in place and cold-launched on Samsung SM-S928B / Android 16.
Physical unit/edit/persistence/accessibility acceptance remains pending; see
`drink-portions.md`.

## Current checkpoint - 2026-10-21

Approved voice checkpoint is committed and pushed to main as `5919898`.
Active follow-up: preserve an explicit Chat meal type (e.g. "for dinner") into
Review/save. Member reported correct foods/portions but missing Dinner. Carry a
host-parsed unambiguous type on the validated proposal, preserve Today priority
and editable Review, and keep model/vault schemas unchanged. Implementation and
independent review complete; all 669 tests, debug/release builds, release vital
lint and diff check pass. In-place update/cold launch succeeded; phone retry pending. This
separate fix is not yet commit-approved.

Automatic Chat speech, pencil Edit, continuous inline activity, post-cancellation
typed delivery, cooking-report confirmation and manual-stop silence admission are
implemented. Final installed APK passes 661 tests, debug/release assembly, release
vital lint and diff check. Samsung SM-S928B / Android 16 updates preserved storage.
Member confirms named read/Edit/inline/Stop/lock, retained spoken food+gram Review,
weight Review dismissed without saving, quiet/original-background silence rejection,
normal speech and Today Speak with correct section and retained Chat history.
Final integrated review found no confirmed material introduced defect; member
explicitly approved committing and pushing this reviewed checkpoint. Save that
approved baseline before implementing the newly reported Chat dinner-label gap.

Open: wider noise/VAD accuracy, recorder/native timing, other failure/permission
paths, full visual evidence, rotation/keyboard/font/TalkBack/reduced-motion coverage.
This is a development checkpoint, not full UI/release acceptance. Next bounded task:
commit/push the approved baseline, then carry explicit Chat meal types into Review.
The entries below preserve earlier progression; this summary supersedes their
historical pending-gate statements. Full evidence: `m4-assistant-checkpoints.md`.

## Earlier checkpoint progression

Accepted focused repair (2026-10-21): member's two silent-recording checks with background
sound sent invented Chat messages. Empty-speech gate failed. Recorder manual Stop
hard-codes detectedSpeech=true, bypassing the existing caller rejection. Repair
manual/automatic stop parity using actual per-capture evidence; do not claim the
existing single-peak heuristic is a robust speech/noise classifier. Implementation
and independent review complete; 661 tests, both builds, release vital lint and
diff check pass. In-place update/cold launch succeeded. Member confirms quiet and
the same background silence are rejected, with normal speech working. Named retry
passes; broader noise/VAD and recorder timing remain unverified. Next: Today entry
regression, then checkpoint review; broader layout/accessibility gaps stay open.

Current focused repair (2026-10-21): `i have cooked rice` followed by an offer and
`yes` failed with no draft. The original report is excluded by consumption-only
confirmation candidate admission. Extend transient candidate admission to completed
cooking/preparation reports; keep native confirmation, source/session binding and
Review/Save. Implementation and independent review complete; all 658 tests,
debug/release builds, release vital lint and diff check pass. Authorized in-place
update/cold launch succeeded, preserving private storage. Member confirms `yes`
opens the correct rice Review, then Discard. Safe trace corroborates
`meal_confirmation_used` and delivery. Named confirmation retry passes. Member also
confirms automatic spoken food/gram edits and both retained Reviews are correct.
Member confirms spoken weight opens editable Review and dismissal creates no
record. Named read/edit/confirmation/weight/Stop/lock checks pass. Empty/error
speech recovery, Today regression and broader layout/accessibility gates remain
open; this is not full release acceptance. No commit/push approval for this work.

Current UI extension (2026-10-21): member requested and chose continuous activity
inside Chat. Focused Stop→Type phone check found the typed meals read remained
Thinking after MiniLM selected its tool. Voice-isolation result effects lacked
guard inputs in their keys; coherent keyed guard + terminal-voice retirement at
accepted idle text start is being repaired/reviewed/tested. Original timing is
not established; scoped review and 657-test/debug/release/vital-lint/diff gate
pass. In-place update/cold launch succeeded with private storage preserved;
member confirms focused retry correct with no late result. Safe events corroborate
MiniLM read, host handler, audit/continuation/delivery. Member also confirms
lock/history retention and fresh correct read with no late voice result; safe
events corroborate start/lock then fresh MiniLM delivery. Later 658-test build
passes member-observed spoken draft edits and weight review/no-save. Broader
visual/accessibility/Today and empty/error recovery gates remain pending.

Continuous activity baseline: member requested and chose continuous activity
inside Chat. Research/decision: `design/chat-activity.md`. Ordinary voice uses
the same inline activity surface as typed response/draft loading, with one keyed
stage, subtle standard motion and explicit controls. Today entry stays separate;
submission/record review guards remain. Implementation and scoped independent
review complete; final gate passes 655 tests, debug/release assembly, release vital
lint and diff check. In-place installation/cold launch succeeded on Samsung,
preserving private storage. Member confirms one inline flow and Type instead
keyboard/no old text. Latest safe events corroborate transcription/Chat/delivery
via Gemma, not a MiniLM or meal-read claim. Stop/lock, draft/weight review and
broader visual/accessibility/Today gates remain pending.
No commit/push authorization for this extension.

Active extension (2026-10-20): automatic ordinary Chat speech submission after
pushed checkpoint `70aa743`. Member approved removing mandatory transcript
confirmation: show Listening/Transcribing, submit bounded recognized words once
through guarded normal text Chat, then show the actual response/draft stage.
Keep one-use capture/session/request and exact draft proofs, Stop/lock/disposal
revocation, native busy ownership and explicit Review/Save. Latest member message
Edit copies words to the composer for a new explicit Send, without rewriting
history or replaying actions. Today meal entry is unchanged. Implementation and
independent review are complete. Final gate passes 653 tests, zero failures/errors/
skips, debug/release assembly, release vital lint and diff check. Samsung
in-place installation/cold launch succeeded (2026-10-21), preserving private
storage. Member confirms automatic spoken read answers once and Edit fills the
composer without sending. Latest safe metadata corroborates MiniLM read/delivery.
Spoken draft edit, weight review/no-save, Stop/lock/failure and broader phone
gates remain pending. No new
dependency, model, vault format, record-save authority or commit/push approval.
Member-requested pencil-only Edit refinement passes the same 653-test/build/
vital-lint gate and is installed in place; pencil interaction acceptance pending.

Previous accepted checkpoint (2026-10-20): shared Chat voice preview after pushed commit
`8e985dc`. All ordinary Chat microphone input uses history-free, local plain
transcription, editable words and explicit Continue through typed Chat routing.
One-use capture/session/request authority applies even without a draft; eligible
draft edits add the exact proposal/name/gram binding. Stop/lock/new work revoke
voice authority. Intentional Stop permits only quiet Type instead exit in the
unchanged session, never submission. No new model, dependency, vault format or
automatic Save. Research, decisions and chronological acceptance evidence are in
`assistant-companion-bundle.md` and `m4-assistant-checkpoints.md`.

Implementation and independent reviews complete. Final installed APK passes
647 tests, zero failures/errors/skips, debug/release assembly, release vital lint
and diff check. Samsung SM-S928B / Android 16 in-place install and cold launch
succeeded, preserving app identity/private storage. Member accepts generic spoken
meals read without a draft, lock/unlock with history and fresh voice, spoken food/
gram edits retained across Review, Type instead waiting for explicit Send, and
final Stop-to-typing without error followed by a correct typed answer and no late
result. Safe metadata corroborates transcription/MiniLM read/delivery and local
food edit; it cannot prove exact words, every repeated portion or native lock
interruption timing. Screenshots used for requested inspection were deleted.

Member explicitly approved committing/pushing this checkpoint, not a completed
release. Broader recognition,
Today/generic entry-point regression, active native timing races, rotation/font/
TalkBack, permission denial and other accessibility gates remain documented open.
No source changes after the final gate. Save the approved development checkpoint
with the recorded acceptance limitations; the earlier pending-approval notes are
superseded by this explicit approval.

Current reliability fix (2026-10-19): native single-food tool selection remains
inconsistent despite one bounded retry. Implement conservative local label edits
for an explicit single-food correction whose full phrase and canonical food head
are recognized by the packaged catalog. This is vocabulary admission only: no
catalog ID/nutrition selected, original proposal and explicit grams preserved,
Review still unmatched. Unknowns keep native handling. Implementation and
independent review complete; 632 tests/debug/release/vital-lint gate and diff
check pass. Update installed in place and cold-launched; member confirms repeated
edits yield white rice, 200 g, unmatched across two Review visits. Safe trace
confirms a local food edit; per-request event deduplication cannot count repeated
local edits. Separate native fallback also completed, without text attribution.
Member also confirms fresh basmati/150 g/unmatched Review and Discard. Latest
trace contains no distinct new turn for that report; the earlier native success
is not attributed to this isolated check. Named visible acceptance passes;
broader gaps remain in `assistant-companion-bundle.md`. Member explicitly approved
commit/push of this reviewed checkpoint. The pending-approval statements in the
historical notes below are superseded. Next bounded task: dedicated voice-path
contextual-edit parity, scoped separately from the accepted text submission path.

Active extension (2026-10-18): contextual replacement of the sole food in a live
ordinary Chat draft, such as "Actually, it was brown rice". Host-bound native
intent supplies only a replacement phrase present in the current member turn.
Keep the original proposal and separate name/portion overrides; Review clears
the old food match and requires a fresh member choice for replacement nutrition.
No saved record change or automatic Save. Multi-food edits and dedicated Speak
handoff are later slices; keyboard dictation shares the text path. Design and
scope: `assistant-companion-bundle.md`. Implementation and independent re-review
complete; final 619-test/debug/release/vital-lint gate passes, diff check clean.
Verified APK installed in place and cold-launched after member reconnect;
single-food replacement/retained grams/no inherited match phone check passes
across two Review visits. Missing fixed monitoring events diagnosed and corrected;
monitoring build passes 619 tests/debug/release/vital lint and is installed in
place. Repeated food/portion phone sequence FAILED: conversation status refresh
cleared the active correction request, then late delivery was correctly rejected.
Exact same-session write receipts and atomic hydration/write fencing implemented
and independently re-reviewed; no stale-result guard bypass. Final complete gate
passes 628 tests/debug/release/vital lint. Corrected APK installed in place;
member replay pending and
repeated-edit acceptance remains open. New phone trace confirms first rename and
local grams apply, but second rename returns no-tool prose and retains old name.
One bounded corrective native intent attempt with truthful no-edit fallback is
implemented, with malformed-tool retention and validated-context cleanup. Final
631-test/debug/release/vital-lint gate passes (2026-10-19); update installed in place,
second-rename phone acceptance pending.
No commit/push authorization for this extension yet.

Previous portion-correction checkpoint was approved and pushed as `e5afc12`;
the pending-approval language in the historical notes below is superseded.

Active extension (2026-10-18): correct the portion of a still-current unsaved
ordinary Chat draft after its initial answer. Member confirms the first amount
updated a visible draft; later amounts/natural corrections hit generic Chat.
Keep exact whole-gram updates local; use a typed native intent for natural
corrections, validated against the literal grams and host-bound original target.
Only the separate Review prefill changes. No food edits, history reconstruction,
new dependency, persistence format or automatic Save. Scope/status:
`assistant-companion-bundle.md`. Implementation and independent review complete;
609 tests, debug/release builds and release vital lint pass. Named phone sequence
passes: repeated amounts and natural 022 g correction yield 22 g across Review
return, foods unchanged. Member confirms invalid 2001 g correction retains 150 g
in Review. Stop during correction passes with cancellation events and a correct
subsequent MiniLM read, no late draft. Member also confirms lock during correction
retains history with no late draft and a correct new answer; no fresh matching
trace event identified. Named phone checks pass; broader accessibility/lifecycle
combinations remain open. Ready for explicit commit/push approval.

Active slice (2026-10-17): one host-bound gram clarification for a new Chat meal
draft, plus structured conversational meal continuation. Scope and evidence:
`assistant-companion-bundle.md` and `m4-assistant-checkpoints.md`. Gemma interprets
the next reply using conversation; empty-argument confirm/decline tools consume
only a current typed member-food candidate. Host request/session/source/read
guards, unsaved draft validation and explicit Review/Save remain required.
This replaces invitation prose matching and affirmative-list authorization.

2026-10-18 follow-up: member reports all three named checks pass: a bare amount
at the meal offer gets clarification, Skip preserves the estimate, and lock
clears the pending action while history remains. These are member-observed
replays on the last known installed checkpoint, not new implementation evidence.
Retained metadata could not independently identify these new runs. The earlier
numeric-confirmation failure remains a model-consistency gap: no intervening
source change guarantees the improved result. Broader lifecycle/accessibility
checks remain open. Next proposed feature is natural portion correction, such
as "Actually, make that 200 g", under the existing current-draft/Review guards.

Implementation and independent review are complete. Latest full gate passes:
603 tests, zero failures/errors/skips, debug/release APKs, release vital lint
and clean diff check. Reviewed build installed in place on Samsung SM-S928B /
Android 16, preserving existing app identity/private storage/models.
Named phone evidence: correct rice/portion in Review on the 602-test predecessor
with `meal_confirmation_used` and `portion_applied`; current 603-test build asks
first when requested and acknowledges "No thanks" with no draft, confirmed by
`meal_confirmation_declined`. Member also confirms a corrected food request
prepares scrambled eggs without the offered rice, then Discard. Unitless amount
rejection and explicit 150 g retention across two Review visits pass on the
predecessor 603-test build, with `portion_rejected` then `portion_applied`.
Skip and broader lifecycle/accessibility checks remain open. Historical failures and
intermediate fixes are retained in the checkpoint, not current acceptance.

Previous saved-choice checkpoint is pushed as `1d4113f`. Member explicitly
approved committing and pushing this reviewed portion/continuation checkpoint
to main on 2026-10-17. Commit identity and remote completion are recorded in Git.
Ambiguous amount-as-confirmation handling at the prior draft offer remains an
additional conversation gap; it does not establish a portion amount.

Final checkpoint review found explicit meal write verbs could bypass native
ask-first/pending-action handling through legacy routing. Fix reserves legacy
text routing for explicit weight entry and keeps meal requests native, without
another ask-first phrase matcher. Re-review and named phone evidence below
close that blocker.

Routing correction re-review and final full 603-test/debug/release/lint gate
pass; mixed requests still cannot use direct/flexible read shortcuts. Updated
APK installed in place. Named explicit-record ask-first phone check now passes:
member confirms question first, clear decline and no draft; current safe events
confirm native Gemma and accepted `meal_confirmation_declined`, no legacy route.
Reviewer's route blocker is closed. Checkpoint commit/push is approved with the
documented broader gaps; no release-readiness claim. Next bounded task: resolve
bare-amount ambiguity at the meal offer, then remaining skip/lifecycle checks.

Active slice (2026-10-16): bounded saved-dish Chat clarification, accepting
explicit ordinal/name replies to current ambiguous consumption choices before
existing review. Scope and pending gates: `assistant-companion-bundle.md`.
Baseline `05b72e3` Stop/next-read and lock/reopen/next-read phone checks pass;
force-stop/recreation and forced timeout remain open. Implementation/review and
the final 588-test/debug/release/lint gate pass (2026-10-17). Named phone gates
pass for ordinal/full-name selection, Review/Discard, retained messages,
decline, invalid-choice rejection and clearing pending choices on lock. Broader
multiple-candidate, lifecycle, failed-save and accessibility gates remain open.
Member approved committing and pushing this reviewed checkpoint to main on
2026-10-17. Earlier pending-approval notes below are historical.

Current checkpoint approved for commit and push to `main` (2026-10-16): natural
saved-dish requests, normal-intent fallback and timed drafting, retained draft
acknowledgements, request diagnostics and canonical conversation refresh.
Independent reviews and the complete 583-test/debug/release/lint gate pass.
Named phone checks cover non-food normal response, unsaved draft, saved-dish
MiniLM review and message retention across the minute refresh. Broader lifecycle,
ambiguity, failed-save and accessibility gates stay open in the companion spec.
The historical notes below preserve how the checkpoint was reached; earlier
pending-approval and failed-test notes are superseded only by the named results.

Confirmed live-delivery cause (2026-10-16): the meal retry reconciles request 2,
then `conversation_sync_normalized` clears its identity during inference. A
canonical-equivalent saved snapshot is being mistaken for different Chat history.
Normalized-equivalence preservation passed independent review and the
583-test/debug/release gate, with genuine change/expiry/lock/clear invalidation
intact; no stale-result gate bypass. Member now confirms both requested meal
cases work. Trace confirms unsaved Gemma draft delivery and saved MiniLM read
delivery, with no refresh reset/mismatch across the successful sequence.
Named regression is closed; broader acceptance gates remain documented open.

Current phone blocker (2026-10-16): the non-food test reaches a validated native
reply, but Chat rejects its request identity. The 577-test diagnostic build is
installed to trace fixed reconciliation/sync-reset codes without message content.
Actual identity-loss cause and visible non-food acceptance remain open; stale
guards are intact and no delivery fix is claimed.
The diagnostic replay now shows a normal response with reconciled continuation,
accepted audit and Chat consumption; no reset/mismatch on that turn. The earlier
failure remains unexplained. Ordinary meal/exact saved regressions remain pending.

Review fixes approved 2026-10-16 are implemented and independently reviewed;
576-test/debug/release gate passed, phone checks pending: normal intent selection once
after uncertain/no-match saved probes, with repeat-probe suppression and the
existing timed native draft path only after positive meal intent.

Checkpoint review found an open bare-consumption gap: some non-food `I had ...`
statements are masked then forced into meal extraction after no saved match.
Fix normal-intent fallback without a repeated saved probe before treating the
slice as commit-ready. Existing exact-meal and retention phone passes remain
valid; no source change or new build was made during this review.

Bare consumption probe (approved 2026-10-15) is implemented and passes the
570-test/debug/release gate; bare exact-name Review/Discard phone acceptance
passes with MiniLM and Gemma skipped: “I had san jacobo”
tries a saved dish without special keywords, offers review/choice for matches
and continues ordinary meal drafting on no match. Original text, opaque names,
one-shot cleared-search handoff and final member Save remain required. Scope:
`assistant-companion-bundle.md`. No new commit/push authorization.

The ordinary unsaved-meal draft phone check also worked, but leaving Review
lost the transient assistant card and left only the member question. A scoped
historical acknowledgement and persist-before-review fix passed independent
review and the 573-test/debug/release gate, and is installed for acceptance;
no draft payload persistence or missing-answer gate bypass is introduced.
The member confirms Review/leave-without-saving retains the assistant message,
including after closing/reopening Chat. Broader lifecycle gates remain open.

Natural saved-dish recognition extension (approved 2026-10-15) passed independent
review and the 568-test/debug/release gate; installed in place and launched
from committed checkpoint `6cfb4a0`: polite/natural recall and consumption frames,
bounded command corrections with opaque names, existing review-only draft and
ambiguity choice. No automatic save, inferred usual dish, new dependency or
commit/push authorization. Scope: `assistant-companion-bundle.md`. The missing
answer issue remains explicitly deferred. Member-observed functionality passed;
retained route fell back on MiniLM margin. A classifier-only named-slot masking
follow-up passed review and the 568-test/build gate and is installed; fast
saved-dish recall and consumption/review/discard phone checks both pass with
MiniLM, fresh saved-dish searches and Gemma skipped. Broader acceptance gates
remain open; this slice is uncommitted.

Member-approved Chat correction revision (2026-10-15) passed review and the
562-test/debug/release gate: bounded
multi-word corrections and contextual abbreviations, complete-request validation,
ambiguity clarification and one shared system-service deadline. It supersedes
the one-word policy below. The existing reply-ID mismatch remains a separate gap.
Installed in place; named two-/three-error phone checks pass with fresh MiniLM
reads, accepted audits and visible matching answers. Provider correction and
broader regression/accessibility gates remain open. Scope and
evidence: `assistant-routing-recognition.md` and M4 checkpoints.

Current development state and preserved physical acceptance are summarized in
[the October 11 checkpoint](checkpoint-2026-10-11.md). This does not close the
milestone acceptance gates below.

Current assistant recognition work and its researched development route are in
[shared routing recognition](assistant-routing-recognition.md), with execution
evidence in [M4 checkpoints](m4-assistant-checkpoints.md). The installed checkpoint
passed 562 tests, debug/release APK builds and independent review. Local MiniLM
reads and single-edit correction have named phone successes. Production Android
spelling is implemented, but usable provider correction remains unaccepted.
Gboard is selected under the member-authorized comparison; recent callbacks
reported dictionary words. Delivery monitoring caught an audited Gemma reply
skipped by Chat's current-request gate. Why identity changed remains unproven;
do not bypass stale-result checks. The member explicitly deferred this delivery
investigation and authorized a full checkpoint review followed by commit/push.
Named multi-word phone acceptance is recorded above; remaining provider,
regression and accessibility gates are not marked passed. That reviewed
checkpoint was committed and pushed as `6cfb4a0`; its authorization does not cover
committing the new saved-dish slice above.

## Goal

Goal UI continuation is tracked in the
[view-by-view goal roadmap](design/goal-view-roadmap.md): eleven PDF reference
views, implementation gaps, ordered slices and acceptance checks. Its planning
status does not mark milestone, calculation or physical-device gates passed.

Validate that a private, low-friction wellness tracker helps people record
weight and meals more consistently than conventional forms, without presenting
nutrition estimates as exact facts.

## Non-goals for the first release

- Medical diagnosis, treatment, or eating-disorder coaching.
- Automatic calorie logging without member confirmation.
- Gym/staff access to personal weight or food history.
- A cloud account, social feed, advertisements, or a subscription system.
- Photo-based food logging in the first build.

## Milestone 0 — On-device vision feasibility spike

**Outcome:** prove the core differentiator before ordinary tracker features.

- [ ] Complete the native Android-first vision spike specified in
  [vision-spike-spec.md](vision-spike-spec.md).
- [ ] Complete the task backlog in
  [vision-spike-tasks.md](vision-spike-tasks.md).
- [ ] Make and record an evidence-based go/no-go decision.

**Acceptance:** a meal photo or nutrition-label screenshot produces an editable,
offline, on-device proposal on a physical Android phone, with the stated privacy
and performance evidence.

**Implementation control:** M0 is implemented as a native Android application
in `native-android/`. The earlier Expo experiment remains preserved as evidence
for the model-artifact lifecycle, but is not a production runtime path. Follow
the commit and physical-device checkpoint
gates in [vision-spike-tasks.md](vision-spike-tasks.md). An implementation agent
may not treat simulator output, unit tests, or a successful build as sufficient
evidence for a native/model milestone.

## Milestone 1 — Product and safety foundation

**Outcome:** precise personal-data boundaries and trustworthy language before
building features.

- [ ] Write the privacy notice in plain language: local-only data, deletion,
  optional future backup, and no default sharing.
- [ ] Define assistant safety rules: supportive/non-judgmental tone, no medical
  claims, escalation text for health concerns, and no automatic writes.
- [ ] Define nutrition confidence labels: verified label, member-confirmed
  database match, and AI estimate.
- [ ] Choose initial supported locale, units (kg/lb), and food data source.
- [ ] Establish an accessibility baseline: dynamic type, screen reader labels,
  contrast, and reduced motion.

**Acceptance:** a reviewer can explain who sees each data type and why every
estimated calorie number is visibly qualified.

**Initial decision record:** [M1 product and safety foundation](m1-product-safety-foundation.md).

## Milestone 2 — Private local tracker

**Outcome:** a standalone mobile app that records and graphs member-confirmed
weight entries offline.

- [ ] Create Android/Kotlin application shell and local development build.
- [ ] Implement encrypted biometric-protected local vault.
- [ ] Add a weight-entry flow: value, date/time, optional note, units, edit,
  and delete confirmation.
- [ ] Add Android App Actions voice handoff for weight entry: Assistant extracts
  a proposed value/date and opens Metria's pre-filled confirmation screen;
  Assistant must never save an entry directly.
- [ ] Handle the voice handoff safely when the vault is locked: unlock first,
  then restore the pending validated proposal without exposing it in URLs,
  notifications, or diagnostic logs.
- [ ] Test misheard/invalid values, omitted dates/units, cancellation, vault
  unlock failure, and the current custom-intent locale limitations.
- [ ] Add 7-day, 30-day, and custom-range charts with moving-average option.
- [ ] Show trend descriptions without moralising language.
- [ ] Offer opt-in, encrypted cycle-aware context: member-entered period-start
  dates may qualify short-term weight variation, never infer identity, diagnose,
  predict cycles, alter goals, or leave the device.
- [ ] Add export/delete policy screens; do not implement export until its
  encrypted format is designed.
- [ ] Test airplane mode, app restart, biometric cancellation, invalid values,
  and accessibility navigation.

**Acceptance:** after restart and airplane mode, a member can unlock and view
their stored weight history; no network request is required.

## Milestone 3 — Assisted text meal input

**Outcome:** natural-language meal descriptions become editable proposals, not
silent records.

- [ ] Define `propose_meal` action schema: ingredients, quantities, unit,
  preparation, assumptions, confidence, and source.
- [ ] Build the confirmation sheet with editable portion chips and questions.
- [ ] Integrate the versioned offline USDA Foundation Foods catalog behind an
  adapter. Keep its complete source releases available only as development
  inputs; the member vault must never contain source files or diagnostics.
- [ ] Calculate calories/macros deterministically from confirmed quantities.
- [ ] Display value ranges for uncertain portions and list assumptions.
- [ ] Save source/confidence with each meal; support edit/delete.
- [ ] Add daily and weekly nutrition summaries labelled as estimates where
  appropriate.
- [ ] Test ambiguous language, unsupported foods, unit conversion, zero/invalid
  values, catalog version attribution, no-network lookup, and
  never-save-without-confirmation behavior.

**Acceptance:** "one chicken breast with half a plate of rice" creates an
editable estimated proposal with clear assumptions and only saves after a tap.

## Milestone 4 — Local action assistant

**Outcome:** chat is helpful but constrained, private, and auditable.

**Product decision:** the on-device assistant is a required part of Metria's
first-run setup, downloaded explicitly into private app storage rather than
bundled with the store package. It supports voice/text understanding, drafted
actions, navigation, and grounded local insights throughout the app. Manual
forms remain available when the model is busy or unsuitable; they are a
reliable fallback, not Metria's primary product identity. The assistant may
never bypass host validation. A valid weight may use direct save with visible
Undo/Edit and durable replay protection; meals remain reviewed drafts. Edits,
deletions, goals, privacy, recovery, and model management stay explicit UI flows.

- [x] Add encrypted local conversation storage, 24-hour retention, and clear deletion.
- [x] Implement the transitional allowlisted action dispatcher from `architecture.md`.
- [x] Select and validate Gemma 4 E2B with LiteRT-LM on the target Android phone.
- [x] Complete `docs/assistant-agent-spec.md` Gate A using LiteRT's native persistent
  conversation, manual tool calls, and five read-only tools. See
  [M4 assistant checkpoints](m4-assistant-checkpoints.md).
- [x] Bind factual output as typed atomic facts so valid dates, weights, meals,
  totals, goals, periods, and counts cannot be reassociated incorrectly.
- [x] Keep exact record evidence available behind collapsed chat cards and persist
  conversation updates outside the Compose UI thread.
- [ ] Physically verify host-calculated nutrition progress, exact weight precision,
  reply language, expandable evidence, and send responsiveness on the target phone.
- [ ] Complete Gate B multi-turn/restart/token-budget physical acceptance.
- [ ] Complete Gate C bilingual guardrail, scope, prompt-injection, AI disclosure,
  and member-initiated reporting acceptance.
- [ ] Complete Gate D idempotent native weight and meal action tools.
- [ ] Evaluate later runtime/model candidates on target Android hardware for
  latency, battery, RAM, language quality, and offline packaging size.
- [x] Require an explicit model download during first-run setup.
- [x] Add a visible "AI estimate" label and explanation on model proposals.
- [ ] Test prompt injection attempts, unsupported actions, confirmation
  cancellation, and model unavailability.

**Acceptance:** see the measurable gates in `docs/assistant-agent-spec.md`.

## Milestone 5 — Persisted image and label assistance

**Outcome:** a photo or screenshot assists entry while preserving accuracy and
privacy.

- [ ] Add explicit photo/screenshot consent and local processing disclosure.
- [ ] Implement OCR for nutrition labels and editable field extraction.
- [ ] Prototype a small on-device vision-language model for meal suggestions.
- [x] Use a compact, bounded on-device meal-proposal schema (two strongest
  visible candidates); strictly validate every model field and fail closed on
  unsupported values. Independently valid foods may form a visibly partial
  draft when other suggestions fail validation; no valid foods means manual
  fallback. Members can add or edit secondary foods during review.
- [ ] Require portion/preparation confirmation before nutrition calculation.
- [ ] Measure model download size, latency, battery drain, heat, and failure
  behavior on low/mid/high-tier devices.
- [ ] Test images with multiple foods, hidden ingredients, unreadable labels,
  screenshots, and no-network operation.

**Acceptance:** the app never converts an image directly into a saved meal;
the member sees the interpretation, uncertainty, and assumptions first.

## Milestone 5.5 — Private dish memory and retrieval

**Outcome:** member-confirmed corrections make repeated personal meals easier
without pretending that the vision model retrained itself or recognized a dish
with certainty.

- [x] Store a bounded structured `PersonalDish` record inside the encrypted
  vault, including its member-chosen name, confirmed components, optional
  aliases, confirmation history, and optional reduced reference thumbnail.
- [x] Let the member opt each reviewed dish in or out before it is remembered.
- [x] Add deterministic, explainable retrieval over normalized names and
  aliases, confirmed catalog identities, component overlap, preparation, and
  confirmation history. Return at most three candidates.
- [x] Surface remembered dishes as suggestions during review; accepting,
  correcting, or rejecting a suggestion must remain reversible until the meal
  is saved.
- [x] Use an exact encrypted whole-meal reference only as a clearly labelled
  same-photo fallback for up to three candidates. Do not present it as
  dish-level recognition.
- [x] Save the reviewed meal, food corrections, and remembered-dish updates in
  one encrypted-vault operation so a partial write cannot teach memory without
  saving the meal, or vice versa.
- [x] Give the assistant a bounded `search_my_saved_dishes` capability that
  returns only relevant structured candidates and never exposes the full vault.
- [x] Add member-facing saved-dish management under Privacy, including removal.
- [x] Verify on the physical Android pilot phone that a remembered San Jacobo
  survives an in-place app update, is offered from exact same-photo context,
  and replaces the provisional Cutlet identity only after member acceptance.
- [ ] Test false matches, renamed dishes, grouped components, hidden
  ingredients, duplicate names, persistence/restart, and repeat-meal use on a
  physical Android phone.

**Acceptance:** after the member confirms and remembers a corrected dish, a
later similar analysis can offer “Looks like your …” with an explanation; the
member may accept, edit, or ignore it, and no memory or meal change is persisted
until the final reviewed save succeeds.

**Future enhancement:** combine this structured retrieval with an on-device
visual embedding as a candidate signal. Visual similarity supplements rather
than replaces the confirmed component structure, and does not require a vector
database for the expected personal collection size.

## Milestone 5.6 — Reliable meal photos and private correction learning

**Outcome:** member-confirmed edits make repeat meals easier while varied new
meals remain useful, safe, offline, and practical several times per day. See
`docs/vision-correction-learning-spec.md` for the bounded feedback and
measurement contract.

- [ ] Establish a consented, varied photo benchmark that separates malformed
  output from visually wrong but valid drafts, with novel and repeat meals
  reported separately.
- [ ] Derive stable-item correction differences only from final reviewed meals.
  The member chose automatic local learning from every successfully saved,
  reviewed meal, with no toggle or per-meal prompt; "Remember this dish" stays
  separate. No-op and manual reviews create no correction signal.
- [ ] Store bounded, inspectable, deletable correction signals atomically with
  the confirmed meal; provide clear-all without deleting meals, unlink on meal
  deletion, and preserve existing vault and archive compatibility.
- [ ] Use positive and contextual negative feedback only to rank visible,
  reviewable suggestions; never silently change a food or nutrition estimate.
- [ ] Measure repeated-use latency, battery, memory, thermal behavior, and
  correction effort on the target phone before daily-use acceptance.

**Acceptance:** quality improves on held-out repeat meals without increasing
false food suggestions, and novel meals meet an agreed usefulness threshold.
Manual entry remains usable but is not counted as successful recognition.

## Milestone 6 — Private alpha and standalone release readiness

**Outcome:** a small independent tester group can use Metria safely.

- [ ] Add encrypted archive/export and restoration tests.
- [ ] Complete uninstall/reinstall, new-device, and vault-loss behaviour.
- [ ] Conduct usability testing with manual input and assistant proposals.
- [ ] Review privacy policy, store metadata, support channel, and crash
  reporting data minimisation.
- [ ] Establish App Store/Google Play account, signing, and release workflows.
- [ ] Decide whether Smart Gym module integration is valuable after validated
  independent use.

**Acceptance:** a tester can understand the data policy, use the app offline,
recover only through the stated archive route, and submit feedback without
exposing their health data.

## Milestone 7 — Optional external assistant integrations

**Outcome:** a member may deliberately connect Metria to ChatGPT or another
compatible assistant without making cloud sharing a requirement for the core
offline product.

**Prerequisites:** M6 release readiness is complete, and optional account,
subscription, and integration-service identity have separate approved product
and threat-model decisions. This milestone must not change the initial
release's no-account, local-only default.

- [ ] Choose and document the integration transport: either a minimized,
  revocable derived-data cache or a live phone relay. State availability,
  retention, and disclosure trade-offs; do not describe server-readable data
  as end-to-end encrypted.
- [ ] Add an explicit, default-off `Privacy and integrations` flow that names
  the external processor and lets the member select data categories and a
  bounded date range before anything leaves the device.
- [ ] Define a Metria-owned authenticated MCP server with narrow typed tools
  such as `get_goals`, `get_weight_progress`, `get_nutrition_summary`, and
  bounded `list_meals`; never expose arbitrary vault queries.
- [ ] Exclude photos, chat history, biometric or vault keys, diagnostic logs,
  raw model responses, and free-form notes unless a later threat-model review
  approves a separate one-time disclosure flow.
- [ ] Make the first integration read-only. Any later write tool must create a
  visible proposal for Metria to validate and confirm on the phone; it must not
  write directly to the vault.
- [ ] Add OAuth-based authorization, least-privilege scopes, short-lived access,
  token rotation, replay protection, and a member-visible access log.
- [ ] Let the member revoke an assistant, expire every active session, and
  delete all integration-side data without deleting the local vault.
- [ ] Test expired and revoked authorization, scope violations, bounded-result
  enforcement, repeated requests, phone offline behavior, deletion, and clear
  Spanish/English consent and error states.

**Acceptance:** with integration disabled, Metria behaves exactly as the
offline product. After explicit authorization, ChatGPT can retrieve only the
selected bounded summaries; the member can inspect the access, revoke it, and
delete the shared copy. No external assistant can mutate the vault directly.

## Delivery order

### Hackathon delivery proposal

The member-approved companion UI mockup is now moving into staged Android
implementation. See [UI implementation checkpoints](design/implementation-checkpoints.md)
for the first meal-entry/available-nutrients slice and its still-pending physical
gates. The member subsequently approved the maintenance-only method and encrypted
goal history in [goal-calculation-contract-proposal.md](design/goal-calculation-contract-proposal.md).
Its deterministic estimate needs explicit selection/confirmation; weight-change
adjustments and weight targets stay member-entered. On 2026-10-08 the member
approved [optional training-based macro suggestions](design/phase-nutrition-contract-proposal.md)
against a reviewed calorie budget, with explicit review and per-field source metadata.
The separately approved [phase calorie contract](design/phase-energy-contract-proposal.md)
adds reviewed 10% Lose / 5% strength-Gain starting proposals under a bounded local
offer policy. Implementation and acceptance are tracked separately; forecasts and
live cloud work are excluded from these slices. Current
implementation and open physical/recovery gates are in the design checkpoint;
the optional first-run goal introduction is implemented, with its creation-only
phone routing check and release readiness still open.

The member intends to present Metria at the Nebius x NVIDIA Global AI Hackathon.
See [hackathon-assistant-plan.md](hackathon-assistant-plan.md) for the proposed
local assistant slice, verified submission requirements, and deferred WhatsApp
feasibility. The member selected designing optional real-data Nebius assistance
with explicit consent; [optional-cloud-assistant-spec.md](optional-cloud-assistant-spec.md)
defines its proposed bounded request and privacy contract. Actual cloud sharing
is not enabled, M7 external-integration prerequisites remain, and no release
acceptance gates are closed by this design.

### Recovery archive decision (M6)

Metria recovery is a member-held `.metria` archive encrypted with a
member-created passphrase. It is independent from the device-bound biometric
key, so it can be restored to a newly created vault on another phone. The
archive includes member-confirmed weights, meals, optional confirmed reference
thumbnails, goals, unit choice, cycle context, food corrections, and saved
private dishes. It
excludes Android Keystore keys, model artifacts, temporary images, chat,
diagnostics, and local inference audit metadata. Import must authenticate and
validate the complete archive, show record counts, and obtain explicit
replacement confirmation before writing the receiving vault.

1. M0 on-device vision feasibility spike.
2. M1 safety and product decisions.
3. M2 private weight tracker.
4. M3 transparent text-assisted meals.
5. M4 local action assistant.
6. M5 persisted image/OCR assistance.
7. M5.5 private dish memory and retrieval.
8. M6 private alpha and store readiness.
9. M7 optional external assistant integrations, only after its cloud identity
   and privacy prerequisites are approved.
