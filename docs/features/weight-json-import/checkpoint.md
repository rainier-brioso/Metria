# Weight JSON import checkpoint

## Active trial — 2026-11-09

Current source baseline is `5c68de3`; contract/config execution baseline is
`e9306ab3abd0d1ab3369d6606540218058210c8f` on `codex/weight-json-import`.
The source baseline passed 775 tests with zero failures/errors/skips and debug
assembly. The managed Strata endpoint/model matches the repository configuration.
Three bounded local runs terminated without an accepted source patch: one
timeout/cache-scope rejection and two output-ceiling/zero-diff failures. Their
model judgments are rejected; no parser implementation was integrated.
Separate [draft PR #2](https://github.com/rainier-brioso/Metria/pull/2) contains
the setup and evidence. Next: diagnose the missing Strata/Aider edit output
before another feature run. See [current trial evidence](trial-2026-11-09.md).

This trial supersedes the historical baseline/runtime statements below. It does
not approve or complete the persistence, UI, or physical acceptance gates.

## Current alignment and verification — 2026-10-18

- Branch `codex/weight-json-import` is fast-forward aligned with `main` at `e5afc1240c85b5e356f0733eaf0fc99439667e6b`. Existing proposal files and unrelated untracked files were preserved; no commit was created.
- The offline `weight_json_android_offline` profile passed in a disposable detached checkout and again on the aligned branch with `--rerun-tasks`: 609 tests, 0 failures/errors/skips; `assembleDebug` succeeded both times. This verifies the named debug profile only, not physical acceptance or the full release/lint gate.
- Config and policy agree: two maximum attempts, task timeout 600s, attempt timeout 180s, output budget 4096, reasoning budget 1024. The offline Gradle command has a separate 1800s timeout.
- Strata is explicitly set to `qwen3.8-flash-next-coder-iq1_m` at `http://127.0.0.1:8080/v1`. The service was stopped (Exited 255); endpoint and `/v1/models` are unverified. No service was restarted and no delegation or inference was attempted. Verify the model after starting the service before delegation.
- Task 01 documents the exact schema, conversion and validation rules, result/error types, examples, and scoped files. No implementation source changed.

## Remaining gates

- [ ] Commit the reviewed contract, runner config, policy, and checkpoint before delegating Task 01. No commit was made in this preparation.
- [ ] Start/verify the intended local endpoint and confirm the selected model via `/v1/models` before delegation.
- [ ] Resolve date-only persistence/zone/recap and ordering design before Task 02.
- [ ] Approve Settings flow/spec changes before Task 03.
- [ ] Complete integration and physical acceptance for Task 04; automated tests do not substitute for device checks.

Preserve `DateOnly(LocalDate)` separately from `ExactInstant(Instant)`; do not flatten dates to midnight. Revalidate the full import before one atomic write. Cancellation, stale preview, lock, malformed input, or write failure must not change member state. Keep encrypted vault/archive compatibility and unrelated edits intact. Record future run ID, patch hash, reviewer provenance, profile, result and available token evidence. No commit, push, installation, device action, service restart, dependency download, model inference or member-data access occurred in this preparation.

**Evidence:** planning only. No production edits, install, app network/model/
service operations or device interaction. Official Android SAF docs consulted.
The baseline offline gate passed: 468 tests, no failures/errors/skips, and debug
APK assembled. First attempt lacked SDK configuration; retry used the existing
SDK via child-only `ANDROID_HOME`. No ignored `local.properties` was copied.

Preparation checks resolve six local links and parse two synthetic JSON examples.
Independent review identified a future-timestamp ambiguity; the plan and parser
contract now distinguish calendar dates from exact instants with explicit bounds.
The repair was re-reviewed with no further finding in that narrow pass.
No feature implementation or physical acceptance is claimed by these checks.

## Current state

- [x] Member scope captured: JSON array file import under PrivacySettings;
  date-only and offset timestamp inputs; blank copyable prompt.
- [x] Proposed bounded strict contract documented.
- [x] Baseline offline unit/build gate passed (before feature implementation).
- [ ] Contract committed and named test profile approved, with required offline
  dependencies and SDK environment available (prerequisite to implementation).
- [ ] Date-only persistence/zone/recap design gate resolved and approved.
- [ ] Parser, atomic persistence, UI, integration and automated gate complete.
- [ ] Physical acceptance performed and evidenced separately.

Worktree base: `3545c7410e4a2b0314d937b49c6fa272a42bcf78`, branch
`codex/weight-json-import`. Main has concurrent MemberShell/design edits to
reconcile before integration; do not overwrite them.

## Evidence to record later

Record run ID, baseline/patch hash, reviewer provenance, profile,
command/result, test count/failures, observed tokens if available and gaps.
Automated success is not physical acceptance. Android command:
`.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --no-daemon`
from `native-android/` with cached dependencies. Authorized installs
preserve data; never automate biometric
unlock. Physical evidence records device/version, states and data preservation,
without serials or transient endpoints.

## Stop conditions

Stop if date-only/legacy meaning cannot be preserved, input is malformed, or
atomic failure changes any member state. Stale preview, lock/process loss and
cancellation must not write. Record rollback testability gaps; policy tests do
not prove vault rollback.


## HSDLC-077 preparation update — 2026-10-18

Rechecked source state before planning changes. Feature checkout remains at
`3545c7410e4a2b0314d937b49c6fa272a42bcf78` on `codex/weight-json-import`;
`docs/features/` is the sole untracked path. Main remains at `7f66f60`,
six commits ahead. Its plan records the current 603-test gate; it was not rebuilt
in the shared main checkout to avoid disturbing concurrent work.

Ran the feature baseline from `native-android/` using the checked-in wrapper,
`--offline :app:testDebugUnitTest :app:assembleDebug --no-daemon --rerun-tasks`.
The existing SDK was supplied only as child-process `ANDROID_HOME`; Gradle used
the existing user cache. Result: 468 tests, 0 failures, 0 errors, 0 skipped;
debug APK assembled; 58 seconds; no installation or device use. This is evidence
for baseline `3545c74` only. The earlier 468-test checkpoint must not be cited
as current-main baseline.

The current local-model probe returned `SERVER_PROBE_FAILED`: no active
inference endpoint among two candidates. No service restart or delegation was
attempted. The profile command and policy are draft artifacts in
`review-proposal.md`; profile execution in an isolated Hybrid SDLC worker and
policy timeout compatibility remain unverified.

The data-model review found the old midnight heuristic in
`PhaseRecapCalculator` and confirmed date/time consumers extend beyond recap
to weight trends, summaries and assistant context. Recommended tagged
DateOnly/ExactInstant semantics and candidate scope expansion are recorded in
`review-proposal.md`. The parser slice can remain pure; persistence and UI stay
blocked on explicit approval of date-only storage, zone, recap and ordering
behavior. Existing proposal/task files are retained; no implementation source
changed, and nothing was committed.
