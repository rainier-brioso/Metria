# M4 local assistant checkpoints

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
