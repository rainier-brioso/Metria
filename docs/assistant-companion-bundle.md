# Local assistant companion bundle — 2026-10-12

## Dedicated voice contextual edits — 2026-10-19

Current checkpoint status (2026-10-20): implemented, independently reviewed and
installed; final gate has 647 passing tests, debug/release APKs and release vital
lint. Named member checks pass for spoken meals read, lock/history/fresh voice,
food/gram draft edits across Review, Type instead with explicit Send, and repaired
Stop-to-typing without error or late result. Final safe events corroborate a
transcription start without completion followed by a normal typed MiniLM meals
read, accepted audit and delivery. They do not prove every native race or UI
detail. Broader recognition, Today/entry-point regression, active native timing,
permission, rotation/font/TalkBack and accessibility checks remain open. All raw
inspection images were deleted. Member explicitly approved committing and pushing
this development checkpoint; it is not a completed release. Historical pending notes
below describe the earlier states and are superseded by this status.

Member approved continuing with the existing local voice stack after the
single-food checkpoint `8e985dc`; no Moshi/Mini-Omni2 migration. First slice:
an eligible live ordinary Chat draft may receive a spoken correction through
the existing editable recognized-words preview and Continue in Chat action.
Capture its host-owned original proposal, source request, generation and accepted
name/gram overlays before recording. Recognition alone cannot change that draft.
Only the matching completed local audio request may supply validated words;
audio-proposed actions are ignored. Recognition uses a separate transient request
channel and preserves the assistant draft request/operation/proposal; no request
ID is rewound. A one-use current-session handoff must validate that original
authority before shared text submission, without weakening existing checks.
Unknown food names retain native
intent handling; local grams/catalog-covered label edits reuse existing policy.

No new screen, model, dependency, audio retention or automatic Save. Initial
microphone permission/listening/Stop, recognition loading, editable success,
empty/error retry/Type instead and explicit Continue remain. Cancel, Stop,
lock/reset, disposal, newer work and mismatched generation/request revoke the
handoff; no late audio can revive a draft. A failed handoff shows a recoverable
explanation rather than silently treating a correction as a fresh meal. Today
meal entry remains separate. Same-session return from Review retains accepted
corrections under the existing policy; history cannot recreate authority.
Stopping this separate transcription cancels its token and deletes its clip;
the original unsaved draft remains unchanged. It does not call whole-assistant
cancellation against the preserved draft. The shared runtime stays unavailable
for new inference until the audio worker has actually exited. The result parser
accepts only a one-key `spoken_text` JSON object with validated bounded text;
extra action keys, malformed/empty text and stale tokens cannot dispatch.

Acceptance: fresh rice draft and explicit grams, spoken food correction preview,
Continue, correct unmatched Review with retained grams; repeated spoken gram
edit; unknown-food fallback; cancel/lock and newer-request late-result rejection;
ordinary voice and Today meal entry regressions. Implementation, independent
review, complete gate and dedicated microphone phone acceptance are pending.

Implementation and independent final review are complete. The new channel leaves
the assistant request/operation/proposal unchanged, serializes the model until
actual audio-worker exit, and clears transcript data on lock/unload/new work.
Only a strict bounded single-key JSON envelope can populate the preview. The
one-use handoff binds source, generation, proposal, accepted food and grams.
Continue revalidates before shared submission; failed consume stops without a
new request. Type instead preserves a still-current proof for explicit Send;
a stale proof is revoked with an explained new-message fallback. Generic Speak
and Today entry keep their original APIs. No additional trace content was added.

Review repaired terminal-state suppression of future Chat replies/retake, stale
consume fallthrough, completed transcript lock cleanup and Type instead losing
valid draft context. Pure handoff/parser/result-isolation regressions pass in
the current 640-test run; runtime/permission/clip behavior is code-reviewed,
not established by those pure tests. Final debug/release/vital-lint gate is
finishing; dedicated microphone, Stop/retry/next Chat, lock and typing fallback
phone checks remain open. No screenshot/raw audio/member transcript was read.

Final complete Android gate passes: 640 unit tests, zero failures/errors/skips,
debug/release APKs and release vital lint. Diff check clean. Debug APK SHA-256:
`0c692d5d32085b73e42ad74c10bf28d52a64fe4c19b000b43bb8d68720202096`.
Authorized in-place installation underway on Samsung SM-S928B / Android 16;
microphone acceptance pending. No code change after this gate, no commit/push
approval for this voice extension.

Verified APK installed in place (`Success`) and cold-launched (`Status: ok`) on
that Samsung under the active cycle authorization. `install -r` preserves app
identity/private storage; member biometric unlock and actual microphone/preview/
Continue/Review checks remain pending. No device screenshots, UI dumps, private
records or audio were inspected. No record saved, commit or push.

First microphone acceptance attempt failed (2026-10-20): member reports speaking
"Actually, it was brown rice" produced the unreadable-speech message before
recognized-word preview/Continue. Safe trace confirms the draft and accepted
portion update, but the installed build has no voice-stage events, so contextual
parse rejection and legacy missing-transcript delivery cannot be distinguished.
Do not claim a microphone pass. Transcription currently inherits the wellness
system instruction; replace it with a dedicated transcript-only system instruction
and add bounded fixed stage/rejection metadata to the existing opt-in debug trace.
No raw response, transcript, audio, record or exception content is recorded or
inspected. Keep the strict single-key parser and all draft authority checks.
Focused repair and independent review are complete. Cancellation cannot be
misclassified as parser/runtime failure: result categories are promoted only
after finalization checks the active ID, processing state, engine and interruption.
The full gate passes 642 tests, zero failures/errors/skips, debug/release assembly
and release vital lint; diff check passes. Debug APK SHA-256:
`ae9a50b91e80c14e7ddda85f21cbd66a593ceab002421eeb9337294bd530fc44`.
Authorized in-place update (`Success`) and cold launch (`Status: ok`) completed
on the same Samsung, preserving app identity/private storage through install-r.
Member biometric unlock remains manual. Fresh named microphone
retry and remaining phone gates are open; no commit/push for this extension.

Second microphone check (2026-10-20) failed reliability acceptance: member and
another speaker tried different speeds; only one attempt produced text. Retained
safe events establish eight contextual attempts, one accepted and seven rejected
as malformed envelopes, with no empty-text or runtime-failure outcome. These
events establish formatting rejection, not underlying ASR word accuracy.

Revised transport decision: use plain transcript-only generation for this
preview-only channel, following Google's [Gemma audio ASR guidance](https://ai.google.dev/gemma/docs/capabilities/audio)
and [prompt-format guidance](https://ai.google.dev/gemma/docs/core/prompt-formatting-gemma4).
Put the English transcription instruction beside the audio in the member message;
do not require the speech model to package words in JSON. Host validation remains
bounded and rejects empty/control/structured output. No generated action or JSON
is interpreted; preview text is untrusted and must still pass explicit Continue
and the same current-draft text/action validation. No auto-submit, records, model
replacement, dependency or speech-service change. This supersedes the one-key
JSON transport requirement above, not the audio-action prohibition or handoff
authority. Implementation/review/full gate and fresh phone retry remain pending.
Read-only SDK discovery checked the resolved LiteRT-LM 0.13.0 Kotlin classes:
ConversationConfig and sendMessage expose no arbitrary response-format/schema
option in this pinned runtime. The experimental conversation constraint flag is
used for declared tools; adding a transcription tool or upgrading the SDK just
to package preview words would expand this change without evidence of need.
Plain ASR prompting uses the current runtime and model. One accepted envelope
does not establish full recognition accuracy or recorder quality for all cases.
Implementation retains a hard bound on raw output before trimming, rejects raw
tabs/other non-line-ending control characters, and accepts only one content line
after ordinary surrounding whitespace/line-ending removal. Multiple content
lines and structured output remain rejected. This does not certify that plain
text is an accurate transcript: the visible member check is still required.
Plain-ASR implementation and focused independent re-review complete. Final full
gate passes 642 tests, zero failures/errors/skips, debug/release assembly and
release vital lint; diff check clean. Debug APK SHA-256:
`dc9ac70b14e34ed578086243be2857c9bf1781bb88bbd3d85aa42d1f09c4428e`.
Authorized in-place update (`Success`) and launch (`Status: ok`) completed on
Samsung SM-S928B / Android 16, preserving app identity/private storage. Fresh
microphone preview/Continue/Review retry and broader phone gates remain open.
Member subsequently confirms the named plain-ASR retry: correct recognized-word
preview and brown-rice Review with 150 g and no selected match; left without Save.
Safe tail contains two accepted contextual transcription completions and a local
food-replacement application on the retained source request. No content was read;
the events alone do not establish exact words or accuracy across both attempts.
The named food-edit microphone check passes. Next bounded check: speak a portion
on the retained draft, preview/Continue, then verify grams across Review return.
Stop/retry, lock/stale callbacks, Type instead and broader voice/accessibility
gates remain open; no general recognition-reliability claim or commit/push.
Member confirms spoken "200 grams" preview/Continue yields brown rice, 200 g and
no selected match in both Review visits, then Discard. Safe metadata confirms
another accepted contextual transcription. The existing portion-applied reason
is deduplicated by source request, so it cannot independently count this second
portion edit; the member's visible two-Review check supplies that acceptance.
Named spoken-portion/Review retention passes. Stop/retry and lock remain next.
Member confirms the Stop/retry sequence: interrupted 220-gram recognition, then
200-gram retry, correct Review and no late cancelled correction after about a
minute, followed by Discard. Named visible cancellation/retry acceptance passes.
Latest fixed-event tail contains one later contextual start without a completion;
this is consistent with cancelled work but does not independently establish the
reported retry or its exact text. No event-based retry/portion attribution claimed.
Next bounded check is lock during contextual recognition and fresh Chat afterward.
Lock-check variant failed: member spoke the new meal-read question rather than
typing it and observed a draft named for an earlier gram amount, plus legacy
"Voice request received locally" / "Voice: ..." rows. Member also reports prior
Chat contents were discarded; transcript-restoration acceptance is reopened.
Safe tail shows contextual start then lock and later reply delivery, without
enough correlation to prove which worker produced the visible legacy result.
Do not claim exact stale-worker cause. An explicitly authorized screenshot caught
another app, was immediately deleted, and supplies no Metria evidence; member
asked to reopen problematic Chat for a focused inspection.

Discovery confirms an architectural split: without an eligible draft, Chat mic
uses history-conditioned legacy audio intent and can dispatch a draft without
preview. Revised decision: all ordinary Chat mic input uses history-free plain
transcription, an editable preview and explicit Continue through shared typed
Chat handling. Freeze one-use unlocked generation/capture/request authority even
without a draft; eligible draft editing additionally retains exact original
proposal/name/gram binding. Stop/lock/newer work revoke both kinds. Never dispatch
audio intents or synthesize draft actions from recognized output; no auto-submit.
Dedicated Today entry stays separate. Tests/review/gate/install and fresh spoken
read-after-lock acceptance are pending; this supersedes generic Chat direct-audio
behavior, not the source authority checks or required meal Review/Save.
Authorized visual follow-up confirms earlier correction messages remain visible,
then the reported legacy placeholder/recognized-words rows and prepared-draft
acknowledgement; no Review card is visible. This corroborates legacy audio dispatch
presentation, not exact audio recognition or stale-worker causality. The earlier
full-history-loss report remains unproven; no speculative persistence rewrite.
Temporary screen image was outside the repository and deleted immediately after
inspection; no raw screenshot or private transcript is retained as evidence.
All-Chat transcription implementation and independent review are complete. A
generic one-use proof binds nullable model/active request IDs, handled reply,
capture and unlocked conversation generation; an eligible draft adds its exact
binding. Ordinary Chat returns from transcript submission before legacy audio
intent/placeholder code. Matching voice token/state drives processing even with
no assistant request. Stop/revocation cancel voice only and preserve transcript;
revocation also advances capture generation before cancelling the recorder so
late auto-stop callbacks only delete their clips. Type instead remains explicit
and unsent; a still-stopping worker retains words for retry rather than consuming
authority. Terminal channel isolation is cleared at the next legitimate submit/
capture boundary. The initial complete gate passes; final source-freeze gate is
running after the last recording-cancel addition. Pure tests cover proof/policy
transitions; all-entry-point and native lock/Stop UI behavior still require phone
acceptance and are not established by those tests. No new APK installed yet.
Final frozen-source full gate passes 645 tests, zero failures/errors/skips,
debug/release assembly and release vital lint; diff check clean. Debug APK SHA-256:
`549b9e0a278ef0ed80f8c15d4d116dafea8e2b06fb959baf57e6f8b07ddf299c`.
Authorized in-place update underway on Samsung SM-S928B / Android 16. Fresh
spoken read without a draft, Stop/lock and restored-history acceptance remain
open. No generic voice-action or persistence acceptance is inferred from the gate.
Verified APK installed in place (`Success`) and cold-launched (`Status: ok`) on
the same Samsung, preserving app identity/private storage. Member biometric
unlock and fresh spoken-read preview/Continue/result check pending. Both temporary
inspection screenshots were deleted; no raw capture, record Save, commit or push.
Member confirms the fresh spoken meals-read question has a correct preview,
then a correct meal answer and no new draft. Safe events establish accepted
transcription followed by a shared typed agent turn, MiniLM today-meals read,
accepted reply audit and delivered response. This closes the named generic
microphone read check on the all-Chat preview APK. It does not establish the
lock/Stop round trip, broader recognition accuracy, Today regression, Type instead
or accessibility gates; those remain separate. No model generation is needed
for the accepted MiniLM read answer, while Gemma still performs transcription.
Member confirms the lock/unlock variant: history retained, interrupted preview
gone, fresh spoken question correct, no old preview/draft after the wait. Safe
events corroborate a lock followed by accepted transcription, MiniLM meals read
and delivered answer. No distinct pre-lock transcription-start event appears in
this tail, so it cannot establish exactly when recording/recognition was revoked
or prove interruption inside the native worker. Named visible lock/fresh-voice
acceptance passes; native timing races remain a broader gap. Recheck draft edits,
Type instead and Stop on the final all-Chat proof build before checkpoint handoff.
Final all-Chat build check passes by member report: spoken brown-rice correction
works, spoken 200 grams moved to the composer by Type instead waits for explicit
Send, and both Review visits retain brown rice/200 g/unmatched before Discard.
Safe tail confirms two accepted transcriptions plus local food replacement;
the source-turn portion reason was already recorded and deduplicates repeated
edits, so no additional telemetry-only portion attribution is claimed. This
closes the named final-build draft-edit/Type instead/Review retention check.
One focused Stop followed by normal typed Chat remains before checkpoint handoff.
Final Stop-to-typing check failed. Authorized visual inspection confirms the
current draft/history remain, but Type instead shows a Chat-changed error with
an empty composer. Explicit Stop revokes the proof, then Type instead mistakes
null proof for an unexpected stale source. Temporary image deleted. Repair:
classify an intentional Stop exit only when the unlocked conversation/request
still matches; quietly open typing without auto-submit or revived voice/draft
authority. Changed session/request/lock/new capture/disposal invalidate that
receipt; actual stale cases retain their explanatory guard. This changes exit
presentation only, not the action validator or runtime ownership. Tests/review/
gate and retry pending; named Stop-to-typed-Chat acceptance remains open.
The focused receipt repair is implemented and independently reviewed. A matching
explicit Stop copies identity only before revoking the voice proof. Type instead
may consume that receipt once for quiet composer exit; Continue and submission
still require their normal proof/path, and Send remains blocked until native
cancellation releases the engine. Request/session/lock/new capture/disposal
invalidate the receipt. No history mutation. Full frozen-source gate is running;
phone retry remains pending and no new installation is claimed yet.
Final gate passes 647 tests, zero failures/errors/skips, debug/release assembly
and release vital lint; diff check clean. Debug APK SHA-256:
`95295d1f58426526e6f4b8be0b539fa636c54de2c7accc91b0c498db47d49379`.
Authorized in-place update underway on the same Samsung SM-S928B / Android 16.
Focused Stop-to-typing/normal typed answer acceptance remains open; no record
Save or commit/push. All screenshots from this investigation have been deleted.
Verified APK installed in place (`Success`) and cold-launched (`Status: ok`),
preserving app identity/private storage. Member biometric unlock and focused
Stop → Type instead → deliberate typed meals question retry are pending. No new
source changes after the passing gate; no raw device captures retained.
Member confirms the installed Stop-exit retry: no Chat-changed error, correct
typed answer, retained history and no late voice result/draft after waiting.
Safe tail shows the voice attempt starting without a completion, followed by
typed MiniLM today-meals routing and accepted reply audit/delivery. Named focused
Stop-to-typing acceptance passes. No source change since the 647-test gate; final
diff check and checkpoint handoff are next, with broader gaps above retained.
Member explicitly approved commit and push of this reviewed voice checkpoint.
Earlier pending-approval statements in the chronology are superseded; all listed
broader acceptance limitations remain open.

## Current-draft single-food replacement — 2026-10-18

Member approved starting conversational food edits with replacing one food in
the current unsaved draft. First slice supports only a complete, single-item,
simple ordinary Chat proposal, excluding grouped/joined, partial, saved-dish,
image and manual drafts. "Actually, it was brown rice" may replace that sole
food; no model-selected row/ID, guessed ambiguity, add/remove or multi-food edit.

The host freezes original proposal, request, unlocked conversation generation,
current name override and explicit gram override before model selection. A
dynamic native tool accepts one short replacement phrase. Require a literal
token-bound phrase in the current member text; numbers and mixed edits cannot
silently change portions or other foods. Model output remains untrusted.
Keep original ranges, confidence and assumptions as the source estimate, not
facts about the replacement. Only a separate transient Review prefill changes.

Success acknowledges the old/new food and keeps Review as the primary action,
with Discard. The chosen gram amount remains editable and does not establish
the replacement's nutrition. Review clears inherited description, preparation
and catalog match for the replacement; a fresh member-confirmed local catalog
choice is required before deriving replacement nutrition. No old-food nutrition
survives a changed identity. Missing catalog coverage remains visibly unavailable.
Final deterministic calculation and explicit Save remain authoritative.

Invalid/ambiguous input, no-tool clarification, malformed model selection and
runtime failure retain the last valid draft/name/amount for retry or Review.
Loading uses existing cancellable thinking/Stop. Lock, Stop, Discard, reset,
changed conversation, replacement draft and explicit read/navigation/new-meal
operations invalidate old edit authority. History cannot recreate it. Returning
from Review without Save retains accepted overlays in the same unlocked session.
No new dependency, vault format, saved-record mutation or automatic Save.

Typed messages and keyboard dictation use the same text submission. Dedicated
in-app Speak currently uses a separate audio action parser; full voice parity
requires a future guarded transcript handoff and is not claimed by this slice.
Discovery for that next step: capture the eligible source-draft contexts before
recording; consume only the validated transcript from its matching audio request,
ignoring model-authored audio actions. A one-use host proof must bridge source
draft request to audio request to normal text submission in the same unlocked
conversation. Current validators correctly reject naively passing old contexts
after audio has replaced the request/operation. Stop/lock/newer work must revoke
the handoff. This likely adds a second local model pass; measure real usability
when implementing, rather than claiming voice speed from text checks.
No new screen or layout: reuse the keyboard-safe Chat composer, selectable
timeline, draft card and existing food/portion/catalog Review controls.

Acceptance plan: single-food correction after explicit grams, repeated rename,
Review leave/reopen retention, continued gram edits, missing/ambiguous/mixed
replacement rejection, unaffected reads/new meals, Stop/lock late-result guards,
and replacement catalog matching/nutrition checks. Independent review, complete
Android gate and member phone checks pending. Commit/push not yet authorized.

Implementation and independent re-review are complete. Review found comma-list
ambiguity in literal name binding; the fix permits a leading discourse comma
and a trailing courtesy comma while rejecting food-list commas, slash and
semicolon alternatives. Regression fixtures cover these boundaries. Parent
inspection also fixed local portion acknowledgement names and suppressed a
redundant gram question when the same draft already carries explicit grams.
Those UI-coordinator details are code-reviewed; named phone behavior is pending.
The first complete gate after missing-import repair passed 619 tests and both
builds/vital lint; final complete gate after review fixes is running. No new
installation yet; ADB lost the phone connection and reconnect was requested.

Final complete gate after review fixes passes: 619 tests, zero failures/errors/
skips, debug and release builds, release vital lint and clean diff check. Debug
APK SHA-256: `eee16955de9e97c24205578e746e9d8dea18fcc165acb4a8c357815be1b965d6`.
No ADB device on the fresh post-gate check. Next bounded action: reconnect,
in-place install under active test-cycle authorization, member unlock, then
single-food replacement + continued grams + repeated Review acceptance. Do not
claim phone or dedicated microphone acceptance from the automated gate.

Member requested another connection check; Samsung SM-S928B reconnected.
Installed the verified APK in place (`install -r`, `Success`) and cold-launched
(`Status: ok`), preserving app identity/private data/models. Member biometric
unlock and named correction/Review tests pending. No raw device capture or
private content inspected; no commit/push.

First phone check passes on the APK above: member reports brown-rice food/title,
explicit 150 g and no selected match in both Review visits, then Discard. These
fields are member-observed. Safe events identify the native Gemma turn and Chat
delivery, but no food-edit outcome: parent found the new fixed reasons were
added only to the stored-record allowlist, not recordReplyDelivery's entry gate.
Monitoring fix adds the same closed reasons to delivery and accepted/rejected
mapping, with restart/deduplication and content-rejection regression coverage.
No content fields added. Complete gate and in-place monitoring update pending.

Monitoring update gate passes all 619 tests (zero failures/errors/skips), debug/
release and vital lint. APK `678cd1a811ef5026902cb5c3b3c3e5e70d63990a36fa66d2fd0cb8238920f15c`
installed in place with Success and cold-launched with Status ok, preserving
existing private data/models. Next phone check: repeated name change interleaved
with explicit gram edit, then inspect the fixed content-free outcome events.

Repeated-edit phone sequence failed on monitoring build: member reports draft
disappeared and latest visible reply was the initial gram acknowledgement. Safe
events identify successful draft/portion delivery, then a native correction,
`conversation_sync_changed` during inference, followed by `lifecycle_ready` and
`active_request_mismatch` / `chat_active_missing`. No food-edit outcome recorded.
This is hydration clearing current request state before result delivery; do not
weaken stale-result gates or infer a replacement wording failure from it.
Discovery/fix focuses on exact successful same-session transcript write receipts
that distinguish an older self-written snapshot from a changed conversation.
Unknown replacement, deletion, expiry and lock must still invalidate authority.
No raw member text/records inspected. Initial named Review pass remains evidence
for the preceding build only; broader repeated-edit acceptance is not passed.

Hydration correction is implemented for review: exact committed records form
at most eight session receipts, recognized for two minutes, invalidated on
generation changes/lock/clear/restore/expiry. Root status synchronization suspends
until an in-flight serialized writer completes, then classifies provenance.
Commit and acknowledgement execute in the same IO block so caller cancellation
cannot hide a successful save before recognition. Failed/late-generation writes
cannot establish a receipt. Existing final-result request guards remain intact.
Review's existing synchronous post-save hydration remains separate from root
refresh synchronization. Focused unit fixtures include ordering and cancellation;
complete gate, independent review and phone replay remain pending.

Final hydration re-review found no remaining blocker. Root synchronization holds
the serialized writer gate through the in-memory decision and invalidates pending
write tickets before restoring an unknown changed/deleted transcript. Queued-write
fixtures ensure older local saves are superseded; a known exact own-write KEEP
preserves legitimate current writes. Existing Review navigation already fences
old debounce tickets, so its Save callback contract is unchanged. Final complete
gate and fresh in-place replay are pending; prior repeated-edit failure stays
open until observed on the corrected build.

Final corrected gate passes 628 tests, zero failures/errors/skips, debug and release
builds, release vital lint and clean diff check. Debug APK SHA-256:
`8dd8080a14fdc41e521e34d0cdf06c995f19855375e4eb0c5590bda3f8ae8f70`.
In-place installation and named phone replay pending; no claim that unit race
fixtures alone close the observed phone failure.

Corrected APK installed in place (`Success`) and launched (`Status: ok`) on
Samsung SM-S928B / Android 16, preserving existing data/models. Member unlock and
fresh repeated-food/gram sequence plus minute refresh and Review replay pending.

Replay on corrected hydration build: member reports second rename leaves brown
rice instead of basmati rice. Safe trace now confirms first replacement applied
and local gram update applied, followed by a native turn with audited no-tool
prose accepted into Chat; no second food-edit outcome or request-reset event.
This is an intent-selection miss distinct from the preceding hydration loss.
Keep prior draft retention; add at most one corrective native attempt for a
strictly eligible single-food correction when the initial response has no tool.
Normal conversation/required reads/mixed or unsupported edits do not gain tool
authority. Final literal validation still governs any selected replacement.
If retry still selects no edit, host wording must state that the draft remains
unchanged rather than retain a model claim of an unexecuted edit. Align trusted
portion context with the current corrected display name. No food-specific rule,
new dependency or automatic Save. Implementation/gate/phone retry pending.

Bounded intent correction implemented and independently reviewed: shared anchored
single-food correction shape gates one no-tool retry and literal result binding;
ordinary discourse/sleep statements/questions, unsupported lists/mixed requests,
quantities, required reads and navigation are excluded. The final response drives
normal validation. If it still has no tool, retain the prior food/grams and use
a truthful host unchanged-draft message. Current display name now drives portion
context and old-name acknowledgements. Fixed retry/not-selected metadata excludes
text/arguments. Reviewer also confirmed dynamic conversation cleanup must apply
to food-only contexts with fixed portions; parent applied that one-line finalizer
fix. Final complete gate and repeated-rename replay pending; no phone pass claimed.

Final retention edge corrected: an unsupported/malformed tool selection during
editing now keeps the previous draft and clarifies. Raw `read_*` names alone do
not establish an explicit operation; an adapted valid read or host-required
read/navigation still retires editing, as does a valid saved-dish search. Regression
fixtures cover unknown/malformed tools and unchanged valid-operation precedence.

2026-10-19 final retry gate passes 631 tests (zero failures/errors/skips), debug/
release builds, release vital lint and diff check. Debug APK SHA-256:
`4fb244383218f79be4eae31c06bbf0c67ae0297ef357c1ee841f0850f6ce15bf`.
In-place update and repeated-name phone replay pending; do not treat prior
single-name success or automated fixtures as acceptance of the second rename.

That APK is installed in place (`Success`) and launched (`Status: ok`), preserving
existing private data/models. Member unlock and exact repeated-name replay pending.

### Explicit catalog-vocabulary label corrections — 2026-10-19

Latest phone replay remains inconsistent: member reports brown-rice correction
rejected twice while basmati succeeds. Safe events distinguish a wrong portion
tool/rejected amount, a successful food replacement, and a later no-tool retry
ending in `food_replacement_not_selected`. No raw dialogue/record inspection.
Do not add a brown-rice exception or expand repeated prompt retries.

For a structurally explicit one-food correction, evaluate a conservative local
label-edit path, analogous to exact local gram edits. Require the existing live
context/request/session guards and literal single-phrase parser, plus admission
from packaged offline catalog vocabulary: the complete phrase must have a
candidate and cover a complete canonical food head, not just a matching modifier
or category/source heading. Ambiguous/generic heads abstain. This admission must
not select, retain or infer any catalog ID or nutrition; Review still starts
unmatched with stale context removed. Preserve explicit grams and raw proposal.
Unknown/unavailable catalog or unsupported wording keeps normal native handling.
No food-specific spelling list, download, dependency or automatic Save.

Design review approves the conservative path with full phrase/head coverage,
fresh context, no candidate identity or nutrient leakage, and unknown fallback.
Worker owns the pure catalog-admission helper, shared text submission wiring and
tests; parent owns documentation, complete gate, review and in-place verification.

Implementation and independent final review complete: the shared Chat submission
path applies only a transient name overlay after actual model, active and handled
request IDs, proposal, conversation generation, unlocked, idle and pending-state
checks. Catalog admission scans description tokens and complete canonical heads;
it does not return a catalog identity or nutrients. Brown rice is covered by the
packaged descriptions; basmati remains a native fallback. Repeated label changes
retain the original proposal and explicit grams. Focused fixtures cover positive,
unknown, modifier/category-only, mixed/nonfood, identity and lifecycle boundaries.
The first full gate caught one test call missing the newly required actual model
request ID; that fixture is corrected and the full gate is running again. Phone
acceptance remains pending. No commit/push authorization for this extension.

Final complete gate passes 632 tests (zero failures/errors/skips), debug/release
assembly and release vital lint; diff check clean. Debug APK SHA-256:
`c1b4b5ca92587847df7d4cb29df334fd072bc54920ad146448266002789783d2`.
Authorized in-place installation is underway on Samsung SM-S928B / Android 16;
member replay remains pending. No physical fast-path acceptance is claimed yet.

In-place install completed (`Success`), followed by force-stop/cold launch
(`Status: ok`) on that Samsung. App identity and private storage were preserved
by `install -r`; member unlock and local repeated-edit replay are pending.
No screenshot, raw dialogue or record save was taken.

Member acceptance on this APK: the requested rice / 150 g / brown-rice / 200 g /
white-rice sequence ends with white rice, 200 g and no selected match across two
Review visits, followed by Discard. Safe trace confirms `portion_applied` and
`food_replacement_local_applied` on one draft request. The store deduplicates
identical delivery reasons per request, so it cannot independently count each
local change. A subsequent native request also completed `food_replacement_applied`
after its bounded retry; without reading text, that event cannot be attributed to
a particular phrase. Named visible outcome passes; both edits individually
skipping Gemma is not independently established. Next bounded check: a fresh
basmati correction for unknown-catalog native compatibility. Broader lifecycle,
accessibility and dedicated microphone parity remain open; no release claim.

Member subsequently confirms the requested fresh basmati correction displays
basmati rice, 150 g and no selected match, then Discard. Visible native-fallback
compatibility passes by member report. The latest metadata tail has no distinct
new request after the previously observed native replacement and subsequent
lock; it does not independently attribute an additional isolated basmati run.
Do not claim new routing evidence from that unchanged tail. No code changed
after the 632-test gate; documentation records these bounded acceptance results.
Checkpoint is review/build/named-acceptance ready for explicit commit/push
approval, with broader physical/accessibility/microphone coverage still open.

Member explicitly approved committing and pushing this reviewed checkpoint to
main. This supersedes earlier pending-approval statements. Next bounded task:
scope dedicated voice-path contextual-edit parity while preserving the same
draft/session guards and Review requirement. Dedicated microphone acceptance
and broader lifecycle/accessibility checks remain separate open coverage.

## Current-draft portion corrections — 2026-10-18

Member reports an initial explicit gram answer correctly updated a draft with
Review visible, but subsequent exact/natural corrections received the generic
no-draft reply. Safe metadata shows one local portion application followed by
normal Gemma routing and missing meal confirmation. Code inspection confirms
the initial offer accepts one answer only, then clears its override on another
submission. Natural correction was proposed, not part of `7f66f60`.

Extend the same live ordinary draft, limited to its one existing eligible
unique uncertain food. Exact whole-gram amounts can update locally more than
once, including after returning from Review while the draft is still current.
Gemma interprets natural correction through a typed portion-edit action; it
cannot choose a draft/item ID or replace foods. Freeze the host proposal,
target, request/session and current override before intent selection clears
the previous status. Require one explicit whole-gram literal matching the
selected action, 1..2,000 g; do not infer units or serving conversions. Leading
zeros are numeric notation: `022 g` means 22 g, never a guessed 200 g.

Update only the separate member-amount prefill. Preserve original model ranges,
confidence, assumptions/questions and foods; final deterministic nutrition and
explicit Review/Save remain unchanged. Rejected edits keep the prior amount
available for Review. Current-request/session/proposal checks govern late
results. Lock, Stop, draft Discard, changed/expired conversation, unrelated/new
operation and replacement draft invalidate edit authority; persisted history
cannot recreate it. Keep saved-dish/image/manual and ambiguous/multi-target
drafts outside this slice. No new dependency or vault representation.

A clarification or ordinary no-tool response while resolving a correction must
retain the visible unsaved draft and its prior portion for retry/Review. A model
intent miss cannot discard the draft. Explicit read/navigation/new-meal actions
end the portion-edit operation. Model-request retries must route using the final
response, not a tool-presence flag captured before retry. These boundaries are
part of the correction extension; no Chat history reconstruction is permitted.

Reuse the draft card/composer/Review flow. Loading is cancellable; success
retains a short portion acknowledgement and Review, without re-asking the
initial question. Invalid quantity feedback leaves the current draft editable;
missing/stale target cannot affect another draft. Exact amount editing remains
fast/local; a natural correction may invoke Gemma for intent selection, without
regenerating the food proposal. Implementation and independent review are
complete. The final Android gate passes 609 tests (zero failures/errors/skips),
debug/release builds and release vital lint; diff check passes. Member confirms
the fresh rice sequence (15 g, 150 g, then natural correction to 022 g) reaches
22 g in both Review visits with foods unchanged, then Discard. The bounded safe
trace contains accepted portion application on a native Gemma correction turn;
it contains no quantities or food text, so values come from member acceptance.
Member also confirms natural 2001 g correction is rejected and Review retains
the prior 150 g. The trace shows a native Gemma turn delivered to Chat, without
a dedicated rejection event; rejection/retained value are member-observed.
Stop during correction passes: member reports the subsequent today-meals answer
is correct with no late correction or draft after a minute. Safe events confirm
the native correction was cancelled, then the new read used MiniLM and reached
Chat, without later delivery from the cancelled request in the inspected tail.
Member confirms lock during correction retains history, permits the correct
new answer and produces no old Review card or late correction after a minute.
The inspected trace tail is unchanged from the Stop check, so this lock result
is member-observed without a newly identified matching event. Broader
accessibility and lifecycle combinations remain open.
No commit/push authorization
is carried over from the previous checkpoint.

Initial review found that a
non-edit model reply still cleared the draft, recreating the lost-Review path;
parent also found stale tool-presence tracking across a factual-audit retry.
Both findings were fixed and independently re-reviewed before the passing final
gate. Runtime disposition tests cover retention and retirement boundaries;
broader model behavior is not established by those tests alone.

## One portion clarification in Chat — 2026-10-17

### Member follow-up — 2026-10-18

Member reports all three follow-up scenarios pass: bare numeric reply at the
meal offer receives clarification, Skip keeps the estimate, and lock clears
pending action state without losing history. Last known installation is the
reviewed routing APK based on `7f66f60`; no source change or reinstall occurred
for these reports. Exact dialogue/values were not inspected. The retained
content-free tail contains previous acceptance events and a lock event, but
does not independently identify the reported new clarification/Skip runs.
Record these as named member-observed passes, not telemetry-backed new runs or
a deterministic correction of the earlier amount-as-confirmation behavior.
Model consistency, wider lifecycle, other units/amounts and visual/accessibility
gates remain open. This documentation update does not authorize another commit.

### Approved structured meal continuation — 2026-10-17

The member rejected resolving a conversational confirmation by matching the
assistant's invitation prose. Replace that mechanism with one transient typed
pending meal candidate, bound to the original eligible member consumption
statement, accepted response, request and unlocked conversation generation.
Gemma receives conversation context and a host-provided candidate-available
signal; it interprets confirmation, rejection, correction or a new topic. An
empty-argument `confirm_meal_draft` tool selects only the current host candidate.
It cannot supply replacement food text, select a historic request, override a
required read or save a record. A fresh meal description continues through
`draft_meal`; missing/stale confirmation context clarifies without extracting
acknowledgement words. No invitation regex or affirmative phrase list grants
authority to reuse the candidate. Lock, Stop, reset, expiry, disposal and the
next submission retire the offer; persisted history cannot reconstruct it.
Existing meal validation, one-portion clarification and explicit Review/Save
remain unchanged. Implementation and acceptance evidence follow below.

Structured continuation is implemented and independently reviewed. The full
Android gate passes: 602 tests, zero failures/errors/skips, debug/release APKs,
release vital lint and clean diff check. Native entry additionally verifies the
previous visible member text and assistant reply against the current accepted
turn. Candidate presence keeps the next reply on the native path, including
natural replies that would otherwise enter legacy routing. The dedicated tool
is rejected from generic tool-result continuation. Debug APK SHA-256:
`b76b02c18810e3cade166095d9386cb246b7e0c993cb5c382da2336d4bf69621`.
Authorized in-place installation succeeded on Samsung SM-S928B / Android 16;
cold launch returned `Status: ok`, preserving existing identity/private storage
and models. Member unlock and confirmation/decline/correction phone checks
remain pending. Helper/schema coverage is not a full runtime/Compose lifecycle
test or proof of Gemma's conversational choices.

Named rice/portion phone replay passes on this build: member reports a rice
draft directly and the requested portion correct after Review/Discard. The
current-run content-free trace also records `meal_confirmation_used` followed
by `portion_applied`; the tool used its bound source and the quantity was handled
locally. The member's direct-draft selection does not establish exact intervening
dialogue, and no transcript was inspected. Do not claim decline, correction,
repeated Review or lock acceptance from this result; those remain open.

The explicit ask-first phone check instead prepared a draft immediately, so
decline was not exercised. The direct-drafting default lacked an explicit
exception for a member request to wait or ask first. Revised native instructions
and tool description give that preference precedence: ask one short question
without either draft tool on that turn. This remains model intent selection,
not another host wording matcher. Updated gate and phone acceptance pending.

Ask-first instruction update passes the full 602-test/debug/release/lint gate,
zero failures/errors/skips and clean diff check. Debug APK SHA-256:
`3e76f9e82cb6c55d5baff2524b265fee84c5297bc2a348a91680096222a7982f`.
Phone acceptance of ask-first and subsequent decline is still required; prompt
changes do not establish semantic model behavior from automated tests alone.

Ask-first replay now reaches the invitation and creates no draft after the
member declines, but its acknowledgement is inaccurate. Safe metadata records
`meal_confirmation_missing`: an erroneous draft selection was blocked by the
acknowledgement input guard. This is safe rejection, not verified semantic
decline handling. Complete the structured protocol with empty-argument
`decline_meal_draft`, subject to the same current candidate/request/read guards.
It acknowledges the decline and creates no proposal or write. Invalid/missing
pending actions receive a neutral no-draft explanation rather than claiming the
food context was lost. No lexical decline list grants action authority.

Typed decline handling is implemented and independently reviewed. Its fixed
reply acknowledges the declined draft; native state clears and returns before
generation. Both pending-meal tools require a current validated candidate and
cannot override record reads or enter generic tool-result execution. Invalid
actions say only "No meal draft was prepared." Monitoring adds the closed
`meal_confirmation_declined` accepted outcome, with no text or food data.
Final Android gate and corrected-phone acknowledgement remain pending.

Final typed-decline gate passes: 603 tests, zero failures/errors/skips,
debug/release APKs, release vital lint and clean diff check. Debug SHA-256:
`f6e9ef410fc7dc49916de6bfa9a9667b5a4800b4705ea659a93faa64b40cba2f`.
No new dependency, persistence representation or write permission. Installation
and semantic decline/visible acknowledgement replay are the next physical gate.

Current 603-test APK is installed in place on Samsung SM-S928B / Android 16.
Named ask-first/decline phone check passes: member confirms clear decline
acknowledgement with no draft; the current-run safe trace records accepted
`meal_confirmation_declined` and Chat delivery. This exercises the intended
typed outcome, not the prior generic blocked-draft fallback. Existing app data
and models preserved; no meal saved. Correction, repeated Review, skip/invalid
quantity and broader lifecycle/accessibility acceptance remain open.

Named correction phone check passes on the current 603-test build: after the
ask-first rice offer, member requests scrambled eggs instead, verifies Review
contains eggs without rice, then Discards. Safe metadata shows a fresh native
Gemma turn delivered to Chat, without the original-source confirmation event.
Food identity is member-observed, not read from telemetry. This accepts the
named correction only; broader arbitrary corrections remain unverified.

Unitless/repeated-Review attempt has not reached its intended stage: member
entered the bare amount at the preceding draft-confirmation question. Metadata
records `meal_confirmation_used` and draft delivery, with no portion event for
that reply. The amount was interpreted as confirmation, not validated/applied
as grams. Record this as an unresolved ambiguous-reply behavior, not a pass or
failure of the local portion parser. Continue the portion check on the prepared
draft only when its explicit gram question is visible. No code change or new
installation made for this observation.

Completed portion-stage phone check passes on the current 603-test APK: member
confirms the unitless reply is rejected, explicit 150 g appears in Review, and
the same amount remains after leaving without saving and reopening Review,
then Discard. Same-request safe metadata records `portion_rejected` followed by
`portion_applied`, with no additional model turn between those portion events.
The repeated-Review amount is member-observed; no proposal/record content read.
This closes the named unitless and repeated-Review checks, not the earlier
amount-as-confirmation gap. Skip, other units/ranges, lock/in-flight lifecycle,
multi-food/partial drafts and broader visual/accessibility gates remain open.

Final checkpoint review found an alternate route: explicit write words could
send ask-first meal requests to the old intent handler, bypassing native
instructions and pending actions. Retire that meal route instead of matching
additional ask-first wording. Legacy text migration now requires explicit weight
entry; meal requests use native Chat even when they say record/log/save.
Preserve existing direct-read exclusions and weight entry, including complete
numeric weight commands; a food portion in kg/lb does not itself select weight
entry. Regression tests cover the reviewer's ask-first request, direct meal
logging, units in food descriptions and retained weight commands. Updated
review/full gate/install and named physical route check pending.

Routing correction re-review confirms the reported bypass is resolved. The
first gate exposed two tests asserting the superseded legacy meal route; both
now assert native routing while independently preserving rejection from direct
and flexible read shortcuts for mixed requests. Final complete gate passes:
603 tests, zero failures/errors/skips, debug/release APKs, release vital lint
and clean diff check. Current debug SHA-256:
`83cb8a400da5c6a3553114ca24bde5e9eefad1a915d1a36297498114e63c6416`.
Named on-phone explicit-record/ask-first check remains pending installation and
member unlock; earlier physical passes refer to the predecessor 603-test build.

Final routing APK installed in place and cold launched successfully on Samsung
SM-S928B / Android 16, preserving existing data/models. Reviewer's exact
explicit-record ask-first phone case passes: member confirms question first,
clear decline and no draft. Safe events confirm native Gemma and accepted
`meal_confirmation_declined` with Chat delivery, not legacy routing. No private
contents inspected. This closes the final review blocker; the named earlier
correction/portion passes remain predecessor-build evidence. Checkpoint awaits
explicit commit/push approval. Bare-amount confirmation ambiguity, skip and
broader lifecycle/integration/visual/accessibility checks remain open.

Member explicitly approved commit and push of this reviewed checkpoint to main
on 2026-10-17, including these documented acceptance gaps. Git records the final
checkpoint identity; approval does not close the open gates or authorize a
release, new dependencies or destructive device actions.

Latest member replay explains the extra question: the safe trace records
`meal_confirmation_missing`, followed by a new meal description and
`portion_applied`. The member confirms gram handling works after re-describing
the food. This does not accept the original-food confirmation handoff or the
exact amount across two Review visits. The earlier invitation-matching design
and its tests/build evidence below are historical, superseded by this decision.

Member approved this next slice after the saved-choice checkpoint `1d4113f`.
For a newly completed ordinary Chat meal draft with exactly one eligible
uncertain food, ask one host-generated question about its gram portion. Require
a unique simple food name, no joined/grouped target or partial/omitted foods.
Multiple uncertain foods retain normal editable review in this first slice. The question
belongs to the transient draft card, not retained imperative Chat history.
Review/Discard remain available without answering; skip or an unknown amount
keeps the estimate for manual review. This does not interpret arbitrary model
questions, revise saved dishes, change foods, infer serving conversions or
enable repeated draft-edit commands.

An explicit whole-gram reply from 1 to 2,000 g with a gram unit applies only to that draft item through a
host-validated transient portion override. Preserve the original model range,
confidence, questions and assumptions; the member amount is a separate review
prefill, not an exact model estimate. No additional Gemma call, automatic meal Save,
nutrition guess, raised confidence or new persistence representation. Retain the
member's reply and historical acknowledgement; other foods and their estimates
stay unchanged. Final Review still verifies foods/portions and calculates
nutrition through the existing deterministic flow before explicit Save.

Bind the offer to the completed request, original proposal and unlocked
conversation generation. Ask at most once per new draft. Stop, lock, clear,
expiry, disposal, changed proposal and unrelated submission invalidate the
unanswered question. An accepted portion stays only in unlocked session memory
for the same draft across Review/return, so repeated Review cannot silently
revert to the model midpoint. Stop/lock/reset/new turn/draft Discard or proposal
replacement invalidate that accepted override too. Restored messages cannot
recreate either state. Unsupported quantities
remain un-applied with a concise explanation; unrelated questions start normal
handling. Failed transcript retention must keep review in Chat for retry.

Implementation and independent review are complete. The reviewed repeated-
Review fix keeps accepted amounts in unlocked session memory; pending questions
remain transient. Final 2026-10-17 gate passes: 596 tests, zero failures/errors/
skips, debug/release APKs, release vital lint and diff check. APK SHA-256:
`cf6105fa93ad7eddcdae89d667b3525ba49fe2edd3b853e1b65bbbf15e9a6bab`.
Authorized `install -r` succeeded on Samsung SM-S928B / Android 16 and cold
launch returned `Status: ok`, preserving app identity/vault/models. Member
biometric unlock and physical acceptance are pending. Required
named checks: numeric reply updates only the intended portion in Review;
skip keeps the estimate; invalid/unitless reply does not invent grams; lock and
topic change clear the question; Review/Discard retains the historical reply.
The existing content-free trace records only closed portion applied/skipped/
rejected reasons and numeric request identity; names, grams and replies are
excluded, with the existing opt-in/retention/release-no-op rules unchanged.
Broader multi-food, partial-proposal, lifecycle, save-failure, keyboard/font,
TalkBack/rotation and visual gates remain explicit acceptance work.

First phone attempt is not accepted: the member reports a recording invitation
instead of an immediate draft, followed by a draft named "yes" after affirmative
consent. Portion prefill worked according to the member and fixed metadata shows
`portion_applied`, but the original meal source was lost before that step.
The draft extractor currently takes only the current message. Add a one-turn
host-bound confirmation for the preceding member consumption statement, created
only after its accepted current recording invitation; an affirmative still needs
the native draft tool and cannot authorize Save. Keep exact member description,
never food inferred from assistant prose. Bare/stale acknowledgement must not
become extraction text. Lock/Stop/reset/expiry/disposal/different topic invalidate
the offer, with no reconstruction from saved history. Build/review and original
food plus repeated-Review phone acceptance remain pending.

Confirmation correction is implemented and reviewed. Exact closed affirmatives
may use the current original member description, with punctuation normalization
for "Yes, please.". Added details/negation never reuse old source. A positive
recording invitation must occupy one bounded question clause; unrelated advice
questions, negative/non-food statements and statements without an invitation
cannot seed it. Native entry rechecks the actual prior accepted invitation,
source request and absence of selected meal/draft/navigation before existing
draft-tool validation. Missing or declined acknowledgement clarifies without
extraction. The final 599-test/debug/release/lint gate passes, zero failures/
errors/skips, diff check clean. Corrected debug APK SHA-256:
`9f624078f68f389736ce8a66e21155967d3aa234e1c38ed1cd3a5fe709f98c9f`.
Authorized in-place installation succeeded on Samsung SM-S928B / Android 16;
cold launch returned `Status: ok`. Existing identity/vault/models retained.
Original-food/portion/repeated-Review replay pending; no physical acceptance
claimed. Confirmation used/missing joins fixed content-free
monitoring; no original description, amount or assistant prose enters it.

Corrected-build replay remains failed: the member reports that the affirmative
still produces a meal named "yes". The installed APK checksum matches the
599-test artifact above. Its retained metadata shows the native Gemma turn and
draft delivery, but neither confirmation-used nor confirmation-missing, so the
cause is not yet established. Do not accept original-source or repeated-Review
behavior from this replay. Next: verify the exact short confirmation and close
the runtime-path evidence gap before changing routing rules.

## Saved-dish Chat clarification — 2026-10-16

Member approved implementing a focused clarification step after the named
cancellation and lock/reopen checks. Existing ambiguous saved-dish requests
offer at most three choices. Add a transient numbered question and accept an explicit
ordinal or unique full candidate name in the next Chat turn, alongside the
existing Review buttons. This permission applies only to a current completed
consumption/draft request; recall alone cannot acquire draft permission.

The numbered question/instructions belong only to the live choice card. Do not
persist an imperative selection prompt that would outlive transient choices
after Review, lock or disposal. Ordinary saved-search evidence and the member's
selection plus host review acknowledgement follow existing transcript retention.

Keep pending choices transient and host-owned, bound to the request/session.
Before opening review, recheck the unlocked vault and unchanged candidate,
persist the selection turn and acknowledgement, then consume the selection
once. Failed persistence stays in Chat with a recoverable retry. Stop, lock,
expiry, reset, changed conversation, unrelated submission and stale IDs must
invalidate the pending choice. Reopened historical messages cannot revive it.
No model-supplied dish ID, automatic Save, new persistence format, dependency,
OS spelling of dish names or guessed portion is permitted. Portion questions
and draft revision are a separate future slice.

Implementation and independent review are complete. Review found obsolete
choice instructions persisted after the live offer expired; the prompt and
instructions now stay only in the transient card, and retained invalid-choice
replies describe the outcome without directing a stale selection. The final
2026-10-17 gate passes: 588 tests, zero failures/errors/skips, debug/release APKs,
release vital lint and diff check. Debug APK SHA-256:
`a32590585aa9bb0af0da9384faa282cfb2d3dd5635249a2808d89b14d8e131b9`.
Named ordinal phone acceptance passes on this build: member requested the saved
San Jacobo, replied "the first one", confirmed the correct editable review and
discarded. The selection and host acknowledgement remained after returning to
Chat. This is member-observed interaction evidence; the local selection adds no
new routing trace event. Member also confirms a full-name reply opens the
correct review and "no thanks" clears a fresh offer without review or a new
meal. Member also confirms "the fourth one" selects nothing and opens no review,
while keeping the current choice available. Lock/unlock then removes the choice
card and retains history. These named invalid-choice and pending-choice lock
checks pass on the same installed APK. Remaining
physical gates include multiple-candidate selection,
unrelated-topic invalidation, changed/missing dish, Stop/lock and Review/Discard
retention, with existing keyboard/font/selection/accessibility checks applicable.

Baseline lifecycle acceptance (`05b72e3`, installed 583-test APK, Samsung
SM-S928B / Android 16): Stop during the ordinary meal path followed by a fresh
latest-meal read passes; no late draft/reply after about a minute. Lock during
ordinary meal generation clears request 10; after biometric unlock the member
confirms the sent question remains, a fresh today-meals read answers correctly
and no old draft returns. Trace confirms request 11 -> audited/consumed result
12 using MiniLM. No raw conversation or record values inspected. Forced timeout,
process recreation and broader lifecycle gates remain open.

## Review corrections — 2026-10-16

Current delivery follow-up: the latest ordinary-meal retry shows
`continuation_reconciled` for request 2, native Gemma selection, then
`conversation_sync_normalized` for the same live request while work continues.
The member observed loading disappear without a result. The prior successful
non-food replay also later logged the same normalized-snapshot reset, after
delivery. This establishes that canonical-equivalent vault refresh can invalidate
the live Chat request; it does not reveal whether count limiting or text trimming
caused the raw-list difference, and no member transcript was inspected.

Implement canonical snapshot equivalence before hydration replacement, preserving
live request/pending IDs only for equivalent saves. Genuine changed/removed/
expired conversations and explicit lock/clear must still reset; never bypass the
reply gate or revive a cancelled request. Add synthetic long-transcript/text
normalization and invalidation regressions. Review/build/phone retry pending.

Implementation now uses a pure hydration decision and the bounded last-loaded
vault record. An unchanged incoming record preserves newer unsaved local turns;
an updated record canonically equal to local Chat also preserves request/pending
state. Genuine content change (even with identical timestamp), prior-record
removal, expiry and first hydration after lock/recreation still restore/reset.
No record format, saved limit or stale-result gate changed. Six focused policy
tests cover these decisions, including 60 synthetic messages, text trimming and
a pending unsaved turn. Independent review found no material defect. Full gate
and physical retry are pending; no async lifecycle execution is claimed from
the pure tests alone.

Refresh-fix gate passed (2026-10-16): 583 tests, zero failures/errors/skips;
debug/release APK builds and release vital lint pass; diff check clean. Debug
APK SHA-256:
`444faa08aace610c961c5dcfc60080674a5236f1722e4ba9acb41aad7946f9a2`.
Physical exact-wording meal retry, refresh retention and broader lock/cancel
acceptance remain pending. No commit/push performed.

Post-refresh-fix phone result (2026-10-16, Samsung SM-S928B / Android 16,
583-test APK above): member reports both requested meal cases worked. Fixed
metadata shows the unsaved meal probe handing request 1 to native Gemma request
2, then a prepared-draft acknowledgement consumed by Chat; the second saved
dish case uses MiniLM request 3, fresh search, accepted audit and consumed reply
request 4 with no Gemma selection. The successful sequence spans more than a
minute and has no sync-reset or active-request-mismatch event. A later
already-handled observation is expected duplicate suppression. No raw contents
or nutrition values inspected. This closes the named reported regression;
broader lock/cancel, recreation, failed-save, ambiguity and accessibility gates
remain open. Non-food normal-intent success was separately observed on the
577-test build; no new intent change occurred in the hydration fix. No claim
that every historical delivery failure shares this cause. No commit/push yet.

Member explicitly confirms the exact-wording draft works and its prepared-draft
message remains after one minute in Chat, following Review/Discard. This closes
the requested periodic-refresh retention check on the 583-test APK; broader
states listed above remain open.

Status: implemented; independent review found no material defect in the fix
delta. Full gate passed; phone acceptance pending. This supersedes the forced
no-match meal-drafting behavior recorded below. A failed or partial saved-dish
lookup does not establish that an arbitrary “I had …” statement describes food.
Keep exact saved-name/alias Review and bounded ambiguity choices; send fallback
text unchanged to the ordinary native assistant for intent selection once.
Suppress the saved-consumption probe and refuse a repeated saved search during
that continuation. Positive native `draft_meal` intent alone may enter the
existing timed native drafting path; normal responses/clarification remain valid.
Do not add a growing non-food blacklist or a new food dictionary/dependency.

The continuation retains current request/call/query/original-text validation,
clears pending mandatory search and retires native state before queuing the fresh
request. Stop/lock/session invalidation and fresh request identity remain required.
Use the existing native cancellation/lifetime and generation deadline for both
intent selection and any selected draft, rather than the legacy synchronous
text-proposal helper. Draft output still requires schema validation and member
review/Save; original Chat text and retained draft acknowledgement stay intact.

Acceptance: test suppression/no-retry and positive draft-tool gating for both
ordinary food and non-food input; existing exact saved/alias and transcript
regressions; independent review and complete Android gate. Then direct phone
checks for a non-food statement, an unsaved meal and an exact saved meal.
Native timeout/cancel behavior requires its own evidence; a passing pure policy
test does not establish physical lifecycle safety. No commit/push authorized.

Implementation: `resumeSavedDishProbeAsNormalAgentTurn` queues the unchanged
submission into the normal agent with `suppressBareConsumptionProbe`. Host,
MiniLM and correction resolvers honor the suppression; model-emitted repeat
search receives a bounded retry/rejection. Host-owned
`pendingSavedConsumptionFallback` carries suppression through later native tool
responses and clears with pending state. A model-selected draft uses
`runNativeMealDraft`/`sendPrivateAgentMessage` (existing 90-second cancellation
deadline), with `agent_meal_draft` scoped to the resumed turn so fast completion
still reconciles Chat identity. No-match no longer calls legacy synchronous
`proposeMealFromText`. Tests exercise resolver suppression for food/non-food,
no OS invocation and positive tool selection. Full async lifecycle/timeout races
remain an automation gap; physical interaction evidence is required separately.

Gate (2026-10-16): `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
--no-daemon` passed, including 576 tests with zero failures/errors/skips and
release vital lint; `git diff --check` clean. Debug APK SHA-256:
`417bc8fc236dacb7d9c02457bc31b0883143de3a867026e14dc3083be41e0894`.
Phone non-food/unsaved/exact saved regression and timeout/cancel gates remain
open. No commit/push performed.

First non-food phone retry failed delivery (2026-10-16): member reports error
or missing answer. Fixed metadata shows request 1 used MiniLM probe, request 2
used native Gemma, its reply passed audit, then Chat rejected delivery with
`active_request_mismatch`. This confirms normal-intent handoff but not visible
non-food acceptance. Current metadata cannot establish what changed the active
ID. A diagnostic-only follow-up records fixed reconciliation/reset reason codes
and numeric request IDs, never text/records, to identify this resumed-turn loss.
One candidate is normalized-vault versus full in-memory transcript comparison
during refresh; it is mechanically testable, not established as the phone cause.
No stale-result guard has been weakened and no speculative behavioral fix made.

Identity-diagnostic follow-up passed independent review and the complete gate:
577 tests, zero failures/errors/skips; debug/release APK builds and release vital
lint pass; diff check clean. Only fixed reason values and existing bounded numeric
request IDs are recorded; release remains a no-op. Transcript normalization
comparison stays in memory. Explicit reset/lock paths remain a provenance blind
spot beyond the existing lock events. APK SHA-256:
`50bbcd070d2ee4cd699be705ac75e2c6599d35898b01309f752084b63dcce4d2`.
One resumed-turn replay remains required; no behavioral delivery fix claimed.

Diagnostic non-food replay passed (2026-10-16, Samsung SM-S928B / Android 16,
577-test APK above): member confirms a normal response appeared for the same
fictional sleep statement. Fixed events show submission request 1 reconciled,
MiniLM probe, continuation request 2 reconciled, native Gemma selected, accepted
reply audit and `chat_consumed`. No second saved search or sync-reset/mismatch
event occurred on this replay. This validates this named normal-intent delivery,
not the cause or repair of the previous intermittent failure. Original failure
remains unexplained; diagnostics stay available. Ordinary unsaved draft and exact
saved-meal regression on this build remain pending. No raw text/records read.

## Bare consumption with saved-dish lookup — 2026-10-15

Status: implemented; 570 tests, debug/release builds and release vital lint pass.
Bare exact-name Review/Discard phone acceptance passes; broader cases remain
pending. “I had san jacobo” is natural
language and must not require the member to say “my saved dish”. This supersedes
the marker requirement for a distinct consumption-probe path; explicit saved/
usual recall and draft contracts below remain intact.

Recognize bounded first-person consumption frames (`I had/ate …`, supported
`I've … had/eaten …` variants) with an opaque meal-name tail. Reject negation,
conditional/advice/other-person, selected-record, mixed-action and unsupported
date cases. Do not steal malformed explicit saved/usual commands from their
existing correction path. The tail is never corrected or sent to OS spelling;
only its classifier copy is masked. Original Chat/Gemma text stays unchanged.

A host-owned typed consumption probe performs one fresh bounded saved-dish
search. One identity-exact name/alias match can offer existing Review meal;
several plausible matches require existing bounded named choice, never selection
by frequency. Compound food lists without an exact whole-dish match should
continue through ordinary meal drafting, rather than select a single component.
An empty validated result hands off exactly once to ordinary text meal drafting
using the original submission. Clear mandatory-search/pending-probe state before
handoff so fallback cannot loop or retain a stale saved-query constraint. Do not
show an empty saved-search error as the final answer for an ordinary meal.

Keep the discriminator host-owned in both MiniLM and Gemma search paths; model
JSON cannot grant this permission. Reuse fresh current-vault choice/prefill,
unknown-portion blanks, review/discard and final Save. No automatic write,
frequency-based “usual”, inference threshold, dependency or release-MiniLM change.
No new UI screen, missing-answer investigation or commit/push is authorized.

Acceptance: pure frame/exclusion and opaque-slot tests; exact/ambiguous/empty/
compound disposition; validated result/query and one-shot original-text fallback
with search state cleared; cancellation/selection/session guards; existing saved
flow regression; independent review and complete Android gate before install.
Phone: bare exact-name Review/Discard first, then no-match ordinary draft and
ambiguous choices separately as available. A phone pass for the explicit saved
frames below does not validate this new path.

Implementation evidence (2026-10-15): a host-only
`SAVED_CONSUMPTION_PROBE` follows both MiniLM and native tool dispatch. A unique
full identity/alias offers review; multiple plausible matches offer choices;
empty/single-partial or compound-without-whole-identity results hand off before
empty search evidence is appended. The handoff checks current request, waiting
operation, exact pending call/query and original-text probe, clears mandatory
search and conversation state, and queues a fresh meal proposal on the serial
executor. It uses an agent continuation operation so Chat retains the new request
identity; draft failure becomes a recoverable Chat diagnostic. Cancellation and
vault-lock invalidation still use existing request/session guards.

Frame, host/query, opaque-name/no-provider, reply audit and exact/ambiguous/
empty/compound policy tests pass. Parent review corrected incomplete worker
wiring, native companion capture scope, explicit-usual correction precedence,
compound matching and continuation operation handling. The independent reviewer
could not complete because the delegated agent hit its usage limit; independent
review and physical no-match/ambiguity/cancel/lock checks remain open. Do not
describe lifecycle execution as unit-tested by these pure policy tests.

Gate: `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`
passed with 570 tests, zero failures/errors/skips; `git diff --check` clean.
Debug APK SHA-256:
`186e493638df9c6250b2b1e4365f46a00e4c2add8bdb010ebca4ba900e7e877c`.
No commit or push performed.

Full checkpoint review (2026-10-15) identified an open routing defect before
commit: the bare `I had ...` frame and finite non-food exclusions still accept
ordinary non-food tails such as “an allergic reaction” or “a bad night's sleep”.
Classifier masking removes the tail's meaning, and a no-match search then forces
the utterance into meal extraction. Review/Save safeguards prevent automatic
recording, but this violates the normal-chat boundary. Do not close this gap by
growing a phrase blacklist. The next bounded fix should preserve exact saved
matches while returning uncertain/no-match consumption probes to normal intent
selection once, with the probe suppressed to prevent a loop; only a positive
meal intent should enter extraction. This has not yet been implemented or tested.

Independent review also confirmed that the new no-match handoff calls the legacy
text-proposal helper's synchronous `sendMessage`, without the timeout used by
the native agent draft path. A stalled inference can keep Chat loading and hold
the single executor until cancellation/teardown. Reuse the timed native path in
the next fallback revision. No confirmed defect was found in host companion
binding, one-shot search clearing or persist-before-review sequencing. Native
Gemma fallback lifecycle execution remains a validation gap. Latest automated
evidence remains 573 passing tests and debug/release builds; no source edits or
new installation occurred during this full review. These findings remain open.

Physical exact-name result (2026-10-15, Samsung SM-S928B / Android 16,
APK above): member sent “I had san jacobo”, confirms Review offered without
error, foods/portions match and draft discarded. Retained metadata accepts
`saved_dish_draft` with positive 0.9808, competing 0.3948 and fallback 0.3451;
MiniLM selected a fresh saved-dish search, host audit passed and Chat consumed
the reply. Gemma was skipped for this turn. Later already-handled/request-mismatch
observations after delivery do not invalidate this named success. No private
record content inspected. No-match ordinary drafting, ambiguity and lifecycle
checks remain open; this result does not validate them.

Member subsequently confirmed the unsaved “I had scrambled eggs and toast”
ordinary draft worked very well. Safe metadata confirms MiniLM selected the
saved-dish probe (same masked classification scores); it does not independently
measure or audit the downstream meal draft. Member found that leaving Review
without saving left only their question: the ordinary draft card was transient
and had no historical assistant message. A scoped follow-up retains a generic
prepared-draft acknowledgement with the existing current-request guard, and
flushes question plus acknowledgement before Review opens. No draft payload is
persisted. Save failure remains in Chat; later deliberate Review taps may reopen
a still-current proposal, while simultaneous taps are deduplicated. Full gate
passed (573 tests, zero failures/errors/skips; debug/release APKs and release
vital lint). Independent review found and verified the repeat-review fix, with
no further concrete findings. Diff check clean. Physical return/reopen acceptance
remains pending. Follow-up APK SHA-256:
`a863b5aa8ce34b4ff4cc23aefd916da5a42da1e807ea58a554bbc7c2c916d665`.

Physical retention acceptance passed (2026-10-15, Samsung SM-S928B / Android 16,
follow-up APK above): member repeated the ordinary unsaved meal request, opened
Review and left without saving. The prepared-draft acknowledgement remained in
Chat, including after closing and reopening Chat. This verifies the named
history-retention flow; it does not close repeat-review, persistence-failure,
lock/cancel, accessibility or broader lifecycle gates. No meal was saved as part
of this test. No commit or push performed.

## Natural saved-dish language extension — 2026-10-15

Status: implemented, reviewed and installed from checkpoint `6cfb4a0`; current
568-test gate and named recall/draft phone checks pass. Broader acceptance remains
open. Earlier review/discard results alone do not validate the extended wording.

Extend the existing saved-dish recall and review-only draft paths with complete
natural English frames: polite recall such as “Can you show me my saved dish
lentil stew?” and consumption such as “I've just had my saved dish lentil stew”
or “I just ate my usual breakfast”. Preserve normalized command wording for
MiniLM and its existing competitive acceptance checks. A named-query span is
masked as `name` for classification only; actual local search/query binding,
Chat and Gemma input retain the real text. Broader intent inference,
threshold/model changes, automatic save and frequency-based “usual” selection
are excluded.

Reuse bounded local correction rules for command wording, up to three changes
and 64 candidates, only after an exact frame fails. The named-dish query is an
opaque slot: preserve its wording under existing normalization, never correct
it and never submit it to the OS spell checker. Separate recall from draft
permission; correction cannot turn a read into a draft or invent a consumption
verb. Full frame/query, original host, selected-record, date, negation and mixed
operation checks remain required. Unsupported cases retain normal handling.

Reuse fresh bounded saved-dish search, exact query/result auditing, at most three
named choices, current-vault re-resolution and existing food/portion prefill.
Recall does not create a draft. Consumption offers Review meal; tapping it opens
the existing editable unsaved draft. Unknown amounts stay blank, Discard writes
nothing and final Save alone records the meal. No vault/UI representation or
new provider disclosure is added. Missing-answer work remains explicitly deferred.

Required evidence: positive natural/polite/contraction and command-typo tests;
opaque names, recall/draft separation, original host/query conflicts, selected
records, unsupported dates, mixed/negated/other-person requests; unchanged
no-match/ambiguity/review contracts; independent review and complete Android
gate. Then one authorized in-place install and member recall/draft comparison,
review/discard and safe route inspection. No new commit/push is authorized.

Implementation/review: reuse the existing bounded enumeration with a local saved
command scope. Named-query scopes take precedence over usual-meal scopes; each
candidate independently checks the original opaque query and recall/draft kind.
Generic/system correction cannot admit saved-dish companions. Review found and
fixed malformed-frame/name leakage and draft-name/meal-slot overlap before any
installation; regressions assert no provider calls for both. Relative-day and
weekday rejection was restored alongside numeric/calendar-date checks. Short
command edits use a minimum length of three only within the closed saved frame;
the existing generic read rule is unchanged. Three generic natural-phrase debug
prototypes were added without changing model artifacts, thresholds or scoring.

The first gate caught a compile variable typo, then a removed date guard and an
overbroad test expectation for “I just ate my breakfas”. Corrected the test to
require “my usual” and added a plain-consumption rejection case rather than
inventing saved-dish permission. Independent review found no remaining blocker.
Complete Android gate passed 568 tests with zero failures/errors/skips,
debug/release APK assembly and release vital lint:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`.
Diff check clean. Debug APK SHA-256:
`f585c321c79895a8d9ba4ee9929421577abecbf0f88cd114b871eed377ac425a`.
Authorized in-place installation returned Success on Samsung SM-S928B / Android
16; cold launch returned `Status: ok`, preserving existing vault/model storage.
Member unlock and named recall/consumption/review/discard checks are pending.
Phone acceptance remains open.

First phone result: member confirms recall and draft both work, editable foods/
portions match, and review was discarded. The retained new turn has MiniLM
positive 0.6771, competing 0.6533 and fallback 0.2826: margin rejected, then a
fresh Gemma saved-dish search, successful continuation, accepted audit and Chat
append. Functional acceptance passes by member observation; fast routing is not
accepted from this trace, and content-free logs cannot prove two distinct prompt
mappings. The dish entity can dominate the similar recall/draft embeddings.

Bounded follow-up: mask only the captured opaque dish-name span with neutral
`name` in classifier input. Preserve the actual command wording and intent; do
not canonicalize the whole phrase or lower thresholds. Search, proof, pending
companion and review retain the original query; original Chat/Gemma text is
unchanged. Known usual meal types remain visible to the classifier. Span and
host/query regressions, review, full gate and repeated phone check are required.

Masking follow-up implemented with regex capture ranges (including postfix-name
recall), preserving actual query and known usual meal types. Tests cover names
containing command words, accented names, repeated words, classifier scope recheck
and real query binding. Updated one older classifier-text assertion while adding
an explicit real-query assertion. Independent review found no blocker. Full gate
passed 568 tests, zero failures/errors/skips, debug/release APKs and release vital
lint; diff check clean. Debug APK SHA-256:
`5887e6fa92fd2b885d372f328ee9a498d3d1ef63801c69e8fa27f75bf7529e4f`.
In-place retry installation returned Success on Samsung SM-S928B / Android 16;
cold launch `Status: ok`, preserving existing data/models. One recall-only turn
requested first so the content-free trace can identify its route unambiguously;
draft retry follows separately. Post-mask MiniLM acceptance is open.

Recall-only post-mask check passed: member confirms the correct saved dish with
no draft offer. Fixed metadata shows accepted `saved_dish_recall` (positive
0.9909, competing 0.8934, fallback 0.2481), MiniLM selection, required local dish
search, accepted reply audit and Chat append. No Gemma selection. This accepts
the named typo-command recall route on the masked build; draft retry is pending.

Draft-only post-mask check also passed: member confirms Review meal appears
without error, editable foods/portions match and the draft was discarded. Safe
events show accepted `saved_dish_draft` (positive 0.9568, competing 0.8717,
fallback 0.3512), MiniLM selection, required saved-dish search, accepted reply
audit and Chat append, with no Gemma selection. This closes the named natural
consumption/review/discard check; recall remains separate and read-only. No
automatic save was introduced. No-match/ambiguity, final Save/reopen, usual-meal
variants, ordinary-read regression and broader lifecycle/accessibility gates
remain open. No raw member values were inspected. Changes are not committed.

## Original companion bundle

Status: installed development bundle; named follow-up, draft and navigation
transcript/spelling checks passed. Broader acceptance remains open.

The member requested implementing the complete proposed set, installing only
after development/build/review, then testing each feature in Chat and fixing
observed failures. Preserve the already accepted MiniLM/weight/selection work.
No intermediate APK installation, benchmark, dependency, automatic save, vault
representation, network path or commit/push is authorized by this slice.

## Scope and boundaries

| Feature | Named scenario | Required behavior |
| --- | --- | --- |
| Goal comparison | How much protein do I have left today? | Fresh today's recorded estimates and current confirmed daily reference; deterministic difference, partial/missing/unrecorded coverage; no intake or advice claim |
| Saved-dish recall | Find my usual breakfast | Fresh bounded local dish search; named saved evidence only; no match/ambiguity disclosed |
| Short follow-ups | And yesterday? / And calories? | Earned transient operation/date/metric scope, fresh read; no past answer values or historical use of today's goals |
| Navigation | Open my weight history | Explicit allowlisted destination in the unlocked app; no arbitrary URI, external app or vault mutation |
| Saved-dish meal draft | I had my usual breakfast | Fresh exact saved-dish resolution; unsaved proposal, visible Review/Discard; final member confirmation alone saves |

Only bounded English intents are eligible. MiniLM selects the supported intent;
independent host validation constrains parameters and actions. Unknown wording,
mixed/write/advice/unsupported-date requests keep existing safe handling. The
saved-dish draft permission is separate from a read and never becomes a direct
save. Ambiguous matches require member choice or clarification. Cancellation,
lock, new conversation and stale request IDs prevent late actions. Existing
selected-meal and follow-up boundaries are preserved or explicitly narrowed.

No transient personal values, record IDs, prompts, replies or embeddings enter
the debug trace. Fixed technical operation/outcome reasons may extend the
existing opt-in monitoring with its established retention and release no-op.
Current Gemma readiness remains; skipping generation is not a RAM reduction.

## Delivery gates

- [x] Map existing result, dish-review and navigation seams.
- [x] Finalize bounded contracts and update applicable design/specification.
- [x] Implement all five features and meaningful negative/state tests.
- [x] Independent review and complete Android gate, including release compile.
- [x] One approved in-place installation after the complete set is ready.
- [ ] Goal: valid reference, no reference, partial/missing estimates, above reference.
- [ ] Recall: exact saved match, no match, ambiguous candidates.
- [ ] Follow-up: date/metric change, fresh read, missing scope and lock boundaries.
- [x] Navigation: History destination and return to Chat with the complete turn.
- [ ] Draft: correct saved foods/portions, review/discard, no save until confirmed.
- [ ] Native selection/copy and previously accepted reads retain behavior.

Physical gates remain open until the member observes each named behavior. Record
build/hash, target phone, preservation, retained technical route/audit results
and known gaps here or in `m4-assistant-checkpoints.md`. Material UI changes also
retain the design quality gates; code review/build alone do not close them.

## Development evidence

Goal comparisons now have a separate deterministic response/proof/reply builder.
An independent review found and resolved overly permissive goal bounds and a
protein wording mismatch with the typed factual auditor. The proof checks the
trusted requested date and metric, estimate coverage, displayed arithmetic and
confirmed-reference bounds. Companion grounding also retains the required fresh
read when MiniLM declines. These review findings alone do not establish phone
acceptance; the final automated gate is recorded below.

Final automated gate on 2026-10-12:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`
passed, with 498 tests and zero failures/errors/skips. `rtk git diff --check`
passed. Review fixes include manual provenance for saved-dish drafts, clearing
prior date/slot state, preserving blank unknown portions and catalog release,
strict follow-up date conflicts, and explicit navigation that does not steal
meal reads. The final goal test correction accepts a truthful abbreviated claim
only with the validated typed remaining amount; generated replies retain their
goal and coverage explanation. No factual-audit rule was relaxed for that test.

Debug APK SHA-256:
`1b24de8fded753d7a042e72243157f5b69882c3b17011dcaf211ae6c30c16068`.
All phone, accessibility, visual and lifecycle acceptance gates above remain
open. No raw member records or screenshots were captured during development.

The member-authorized single `adb install -r` succeeded on Samsung SM-S928B /
Android 16 after the gate. Existing app identity and private storage were
preserved; no clear, uninstall or model/vault replacement was used. Member
unlock and feature-by-feature Chat comparisons remain pending.

First phone sequence: member reported `And yesterday?` failed after the two
today goal questions. Retained content-free events confirm both goal routes
were accepted/audited, followed by an accepted historical nutrition selection
whose reply was rejected. Investigation found that the selected metric was not
carried into the native dated-read host call; the short question itself contains
no metric, so the response did not match the frozen request. Fix, regression,
full gate, in-place update and member retry are pending. No historical acceptance
is claimed and the factual validator remains enforced.

The metric handoff fix now carries only a host-selected calorie/protein metric
on the native frozen-date read; ordinary calls retain question parsing. A new
regression reproduces protein goal → calories → yesterday through the local
result and validated reply. Yesterday continuation diagnostics and retained
audit events now use the correct period. Independent review found no blocker.
The complete gate passed with 499 tests, zero failures/errors/skips, debug
assembly and release compilation; diff check passed. Updated APK SHA-256:
`945e5f29e027b0782c0b3ab8af46ac1c5297c87947c8729e8590c95d7e88d894`.
Authorized in-place update succeeded on the same Samsung SM-S928B / Android 16,
and launch returned `Status: ok`; private storage was preserved. Member unlock
and the repeated sequence remain pending.

Member confirmed the repeated yesterday answer matches recorded estimates.
Direct content-free monitoring confirms accepted MiniLM goal routes for the
first two turns and an accepted/audited `read_day_nutrition` yesterday result
for the follow-up, with Gemma generation skipped. This closes the named
historical handoff retry; missing-scope/lock/date-rollover and other coverage
states remain open. Next physical checks are saved-dish recall/review/discard
and explicit navigation with Chat return.

Saved-dish phone check: member reported recall answered correctly without a
review sheet (expected for a read), while a meal-draft request returned no match
with the syntax word `dish` included in the lookup query. Safe monitoring
confirms an accepted/audited draft shortcut and separate recall via Gemma;
records and the original question were not captured. Added bounded extraction
for `my saved [dish] <name>` and `find/show/look up my <name> dish`, preserving
normalized classifier wording and exact host-query binding. Drafts continue to
offer Review meal; only a member tap opens the editable sheet and final Save
writes. The full Android gate passed with 500 tests, zero failures/errors/skips,
debug assembly and release compilation; parent review and diff check passed.
APK SHA-256: `2114b0da62accffd11e6dc2b47b9ef6f0394b0ecf731b407ff5c8450341dbdd8`.
Authorized `install -r` and launch succeeded on the same Samsung SM-S928B /
Android 16 with private storage preserved. Member retry, review/discard and
draft acceptance remain pending.

Member retry confirmed the Review meal button opens an editable draft and
discard returns to Chat with the transient offer cleared. A validation error
also appeared below the offer. Content-free monitoring shows draft selection
accepted and reply rejected. The generated instructions referred to saved
foods in a separate sentence without naming the returned dish; the factual
auditor therefore rejected that sentence. Fixing the deterministic wording and
adding positive generated-reply audit regressions is pending. Draft food/portion
comparison and full error-free acceptance remain open; no write was requested.

The deterministic draft reply now anchors its saved-dish claim to returned
names and describes review instructions without unbound saved-record claims.
The auditor was not changed. Positive generated-reply checks cover single and
multiple matches, recall and no match; malformed evidence and fabricated names
remain rejected. Parent review and the complete Android gate passed with 501
tests and zero failures/errors/skips, debug assembly and release compilation.
APK SHA-256: `8f75e3b2d7fa15b5e7998c6d88810bc7f0c251888bd87151cb4844b71da102b9`.
Authorized in-place update and launch succeeded on Samsung SM-S928B / Android
16 with private storage preserved. Member unlock, error-free draft retry and
food/portion comparison remain pending.

Member confirmed the corrected draft offer has no error, its editable foods
and portions match, and the draft was discarded. Retained content-free events
confirm accepted MiniLM saved-dish routing and accepted reply audit, skipping
Gemma generation. This closes the named matching-dish review/discard retry.
No-match/ambiguity, final Save/reopen and broader UI/lifecycle gates remain
separate. Next named check is explicit weight-history navigation and reopening
the same Chat transcript.

Navigation phone check: History opened and existing Chat/native copy worked,
but member clarified that both the navigation question and opening reply were
missing on return. Safe monitoring confirms MiniLM navigation selected. This
does not pass the transcript-restoration gate. Investigation identified the
immediate child navigation effect racing reply insertion and cancelling the
300 ms debounced encrypted write when Chat is disposed. A coordinated,
successfully persisted handoff before closing Chat, with current-request/lock
guards, regression tests, review, gate and retry is pending.

Navigation handoff now appends the accepted reply once and awaits an acknowledged
encrypted save of the complete turn before consuming the destination token.
Regular delayed saves and clears share a session coordinator; snapshot tickets
are captured before the delay. In-flight navigation/clear blocks ordinary ticket
capture, overlapping clears retain the block, and invalidation drops retained
transcript state. Post-save generation/current/unlocked checks reject late
actions. Save failure stays in Chat with a recoverable error. The reply effect
does not restart itself when marking the reply handled.

Independent review found no remaining blocker. Concurrency regressions and the
full Android gate passed with 509 tests, zero failures/errors/skips, debug
assembly and release compilation; diff check passed. APK SHA-256:
`cd2d6a80b272048df1c6b59d102e69dea5e85053072063c9e88603ebd95d550a`.
Authorized in-place installation and launch succeeded on Samsung SM-S928B /
Android 16 with private storage preserved. Member unlock and navigation with
complete-transcript return remain pending; no restoration pass is claimed.

Member confirmed the navigation transcript fix worked on return. This closes
the named History/complete-turn gate; other destinations, persistence failure
and broader lifecycle/UI checks remain separate. A `weight hystory`
spelling error missed the closed route, invoked Gemma and ended in an error.
Added a contextual `weight hystory` → `weight history` alias, preserving explicit
navigation and request/selection/date/mixed-operation boundaries. It does not
correct arbitrary unknown words. Positive and rejection regressions, full gate,
update and exact typo retry are pending.

Safe monitoring confirms the miss was a scope rejection followed by native
Gemma and unsupported-tool rejection; the correctly spelled navigation selected
MiniLM. The contextual spelling patch passed parent review, diff check and the
full Android gate: 510 tests, zero failures/errors/skips, debug assembly and
release compilation. APK SHA-256:
`20c14e9c6a5a49fbcb63e6482175b97371a8725da785cff3f3ac17ccc45cbf14`.
Authorized in-place update and launch succeeded on Samsung SM-S928B / Android
16 with private storage preserved. Member unlock and exact typo retry remain
pending; arbitrary spelling tolerance is not claimed.

Member confirmed the exact typo retry worked, including History opening and
complete-turn return. Direct safe monitoring confirms accepted MiniLM navigation
and no Gemma route for that turn. The named typo acceptance is closed. Latest
installed gate remains 510 tests with no failures/errors/skips. No code changed
after that gate; subsequent changes only record acceptance evidence.

## Checkpoint state

The named goal/follow-up, saved-dish review/discard and History navigation with
retained messages have been exercised, fixed and accepted as recorded above.
Saved-dish recall was member-confirmed correct through the normal Gemma path;
no physical MiniLM recall acceptance is claimed. Native message selection/copy
was also member-confirmed after navigation. Remaining unchecked coverage,
accessibility, visual, lifecycle and write/reopen gates stay open. The member
authorized saving the development checkpoint after the successful phone
retry. Commit/push preparation retains these acceptance gaps; this is a
development checkpoint, not a completed release or milestone.

MiniLM abstention keeps the existing fresh-read Gemma path. Goal comparison
shortcuts have the explicit arithmetic proof above; a Gemma fallback that invents
an unsupported difference must still fail the normal factual audit. End-to-end
acceptance will record whether each named request actually takes the shortcut.
