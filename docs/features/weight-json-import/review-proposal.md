# HSDLC-077 review proposal — weight JSON import

**Status:** review draft only. No feature source is implemented. The existing proposal, task files, and checkpoint are preserved.

## Reconciled source baseline

- Feature checkout: `codex/weight-json-import` at `3545c7410e4a2b0314d937b49c6fa272a42bcf78`; only `docs/features/` is untracked.
- Current main: `7f66f60`, six commits ahead of the feature base. The current Privacy/Settings implementation is `MetriaPrivacySettingsScreen`; the old proposal must target this screen and current navigation rather than the historical root shell.
- Current main’s plan records a 603-test/debug/release/lint gate. It was not rerun in the shared main checkout, to avoid changing another chat’s checkout. The feature-base offline gate was rerun separately: 468 tests, zero failures/errors/skips; debug assembly passed in 58 seconds.
- Configured local model probe: no active endpoint among two candidates. No service was restarted and no inference/delegation was attempted.
- The former 468-test note is valid only for feature baseline `3545c74`, not current main.

## Date and time contract proposal — approval required before persistence

The parser output must retain two distinct forms:

- `DateOnly(LocalDate)` for `date`; no instant, time, zone, UUID, or storage timestamp is fabricated.
- `ExactInstant(Instant)` for `recordedAt`; parse the explicit ISO offset and compare its instant to injected `now`. Different offsets resolving to the same instant represent the same exact time.

For date-only future validation, capture the import zone once and compare the date with `now.atZone(importZone).toLocalDate()`. The stored date remains a calendar date and does not shift when the device zone changes. The import zone is validation context, not part of date identity. Duplicate identity uses the tagged time value, canonical kilograms, and the contract’s normalized note.

Persistence cannot safely flatten `DateOnly` to local midnight: today’s `WeightEntry` stores only `Instant`, and `PhaseRecapCalculator` heuristically treats local-midnight weights as unknown-time. A zone change can make that heuristic misclassify a real timed measurement or move a fabricated date-only instant to another displayed date.

Recommended persistence shape is an explicit tagged weight-time value, with a legacy variant retaining old serialized instants unchanged. New `ExactInstant` entries use instant membership in recap intervals. New `DateOnly` entries keep their date across zones; for a recap over instant bounds, include them only when their whole calendar day in the recap zone is contained by the phase, and count/disclose date-only readings whose day overlaps a boundary. Apply the existing midnight heuristic only to legacy records during a compatibility period; never use it to reinterpret new exact timestamps. This matches the established date-only meal recap boundary rule.

Presentation must use the stored date for `DateOnly`, the active display zone for exact instants, and a clear “time not recorded” label. Same-day date-only entries must not be given a fabricated order relative to timed entries. The recap’s first-to-last weight change must be suppressed when the endpoint reading order is ambiguous. Weight trends and every weight consumer must use the tagged date semantics.

This proposal requires a persistence/UI scope decision before Task 02. Expected review surface includes `WeightEntry.kt`, `EncryptedVault.kt` and its codecs, `PhaseRecap.kt`, `WeightTrend.kt`, `MemberShell.kt`, `MainActivity.kt`, `PrivateCoach.kt`, `TodayGoalCard.kt`, and affected tests. Recovery/archive format compatibility must be checked before any codec edits. The current Task 02 allowed-path list is therefore incomplete and must be revised and approved before that task becomes executable. Task 01 remains pure and can preserve the tagged parser result without making storage decisions.

## Settings flow design proposal — approval required before UI work

Add an “Import weight entries” row under Privacy & Settings → Encrypted recovery/data controls, with one focused review destination. Use Android’s document picker for a member-selected JSON file; MIME is a hint only. Request local-only selection where supported, while explaining that cloud providers may still appear and offline access is not guaranteed.

Read at most 1 MiB into bounded memory. Cancelled selection returns to Settings without changes. Parse and validate the entire file before showing a review summary with valid count, new/duplicate count, date-only versus timed count, and safe record/field errors. Nothing writes until explicit confirmation. Recheck unlock/session freshness immediately before one atomic batch write. A lock, cancellation, stale preview, parse error, or write failure leaves all weights unchanged. Success reports imported and retained duplicate counts.

Provide a separate blank, copyable schema prompt. It contains no member values and asks an agent only to produce a JSON file for local review; it does not send data or authorize writes. Clipboard content is the generic prompt only.

Cover idle, picker cancellation, loading/read failure, malformed/oversized input, empty/partial rejection, review, duplicate disclosure, confirmation, successful import, stale/locked vault, atomic write failure, rotation/restoration, and keyboard/accessibility behavior. The exact hierarchy and component choices must follow current Metria tokens and the approved settings contract. Once approved, update `docs/design/FLOWS.md` and the Privacy & Settings section of `docs/design/SCREEN-SPECS.md`; no UI implementation should be added to `MemberShell.kt`.

## Runner profile and policy proposal

Draft files in this proposal bundle:

- `hybrid_sdlc.toml`: named profile `weight_json_android_offline`, using the checked-in Gradle wrapper, `--offline`, unit tests plus debug assembly, and no device commands. It derives SDK and Gradle cache paths from the existing Windows user environment inside the runner child process; it adds no local.properties file and downloads nothing.
- `docs/features/weight-json-import/weight-json-import.policy.json`: explicit exact-path policy for Task 01 only. Its intended paths are the two new parser/test files. Persistence, UI, task-policy/profile changes, and other files are outside the exported patch.
- Task identity: `METRIA-WEIGHT-JSON-01`; contract: `docs/features/weight-json-import/tasks/01-parse.md`.

The profile’s command was exercised directly on the feature checkout with the existing SDK and Gradle cache, with a complete offline pass. It has not yet been executed by Hybrid SDLC inside a fresh worker worktree. Policy timeout ceilings are proposed values and must be checked against the configured runner limits before use.

## Required approval and stop conditions

Before implementation, review/approve the tagged time and recap semantics, the resulting Task 02 path expansion, the Settings flow/design, the profile/policy values, and the proposed contract commit. Metria requires explicit approval before any commit or push. The contract and policy must be committed before delegation; do not submit the parser task before that prerequisite. Recheck endpoint and effective limits at that time. Do not restart services, install dependencies, use paid fallback, or claim that a bridge rehearsal proves async worker survival.

No commits, pushes, installation, device actions, member data access, or automatic patch integration are included in this preparation.

## Current alignment and verification — 2026-10-18 (supersedes stale baseline notes above)

- `codex/weight-json-import` is aligned with current `main` at `e5afc1240c85b5e356f0733eaf0fc99439667e6b` by fast-forward. Existing proposal and unrelated untracked files were preserved; no commit was created.
- Offline profile `weight_json_android_offline` passed in a disposable detached checkout at that baseline, then the aligned feature checkout was rerun with `--rerun-tasks`. Both runs passed: 609 tests, 0 failures/errors/skips, and `assembleDebug` succeeded. These are debug unit/build results only, not physical acceptance or the release/lint gate.
- `hybrid_sdlc.toml` explicitly selects `qwen3.8-flash-next-coder-iq1_m` and `http://127.0.0.1:8080/v1`. It sets `max_retries=2`, `task_timeout_seconds=600`, `attempt_timeout_seconds=180`, `aider_max_tokens=4096`, and `aider_reasoning_budget_tokens=1024`; policy values match. Under runner policy, `max_retries` is a maximum attempt count, so this is two attempts. The profile's 1800-second timeout is the separate Gradle command ceiling. The reasoning budget is a runner setting, not proof of model-enforced token use.
- The endpoint was unavailable when last probed; the user's Docker container was stopped (Exited 255). No service was restarted, no `/v1/models` response was obtained, and no task was delegated. Verify the selected model appears in `/v1/models` after the service is started before delegation.
- Task 01 is self-contained: it includes the complete JSON Schema, conversion and validation rules, result/error types, examples, and exact task scope. Commit the contract/config/policy artifacts before delegation, then refresh runner/model checks. No inference run ID, patch hash, or token usage is claimed.

Existing persistence and Settings proposals remain drafts and require the separate product/design approvals stated above before Tasks 02–04.


## Feature-branch alignment

A path-level comparison confirms current main’s six commits changed
`MemberShell.kt`, `PrivacySettingsScreen.kt`, and design/architecture
contracts, while `WeightEntry.kt`, `EncryptedVault.kt`, `PhaseRecap.kt`,
and `WeightTrend.kt` did not change. This confirms the current UI surface must
be used for later Settings work. Do not run UI/persistence tasks from the stale
feature base. Before those tasks, explicitly decide how to align the feature
branch with `7f66f60), preserving its untracked proposal. The pure Task 01
parser can remain bounded to the recorded feature baseline, but its patch still
requires a later integration review against current main. No branch rewrite,
merge, or commit is included here.
