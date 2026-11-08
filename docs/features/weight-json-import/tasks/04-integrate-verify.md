# Task 04 — Integrate and verify

**Prerequisites:** Tasks 01–03 complete; design gate names exact signatures,
paths and approved test profile. Reconcile concurrent work before shared edits.

## Allowed proposed paths

- Narrow integration edits to `MemberShell.kt`, `MainActivity.kt`, and approved
  design flow/screen-spec documents only.
- Candidate `PrivacySettingsScreen.kt` and focused integration tests; exact
  allowed files remain subject to gate approval.
- No unrelated refactor or production scope expansion.

## Contract and acceptance

Wire PrivacySettings to the screen. Scope picker result to active request and
unlocked vault. Explicit confirm/cancel; stale results, lock, cancellation or
process loss never write. Preserve navigation and privacy behavior.

Run approved profile and from `native-android/` run
`.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --no-daemon`
with cached dependencies. Record automated results separately. Physical checks: cancellation,
invalid/duplicate files, zone changes, rotation, lock/background, restart,
airplane mode, accessibility and data preservation. Install/device action needs
authorization; code/tests are not physical acceptance.
