# Local assistant companion bundle — 2026-10-12

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
