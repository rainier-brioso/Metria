# M4 local assistant checkpoints

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
