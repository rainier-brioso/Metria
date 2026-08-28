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

1. Strengthen the factual-output auditor from value membership to typed atomic facts. A valid date, weight, count, meal name, or total must not be rebound to a different field or record merely because both values appear somewhere in the same tool response.
2. Complete Gate B on the physical phone: pronouns, yes/no follow-ups, period changes, cancellation, biometric lock/unlock, process death/restart, token-budget reconstruction, airplane mode, and no replay of completed turns.
3. Complete the remaining UI quality matrix: landscape, gesture navigation, enlarged system font, TalkBack order/labels, rotation, and keyboard-visible send/voice actions.
4. Complete Gate C guardrail, prompt-injection, AI disclosure, safety-boundary, and reporting tests.
5. Move the transitional weight and meal write router to Gate D native idempotent tools before claiming the assistant action system is complete.
6. Complete Gate E bilingual fixtures and physical latency, memory, thermal, battery, and failure evidence.

### Next bounded implementation task

Implement typed field-aware factual evidence and tests for date↔weight, date↔meal, metric↔total, goal↔period, and count↔collection bindings. Fail closed after the existing single corrective turn, without persisting model prose or tool payloads in diagnostics.
