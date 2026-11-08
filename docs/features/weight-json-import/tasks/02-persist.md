# Task 02 — Persist atomically

**Prerequisites:** Task 01 complete; date-only design gate explicitly resolved
and approved. This task remains a draft until that gate names exact source/test
paths, signatures and an approved test profile.

## Allowed proposed paths

- Modify `native-android/app/src/main/java/app/metria/EncryptedVault.kt`.
- Modify `native-android/app/src/main/java/app/metria/WeightEntry.kt` only if
  the approved date-only design requires it.
- `PhaseRecap.kt`, recovery codecs and their tests are candidates only if the
  gate explicitly approves them; no automatic scope expansion.
- Add only narrowly scoped persistence tests if current seams support them.
- No unrelated vault, archive, assistant, or UI changes.

## Contract and acceptance

Revalidate the whole batch while unlocked immediately before commit; guard
stale preview/state and generate IDs internally. Apply duplicate policy without
replacing entries. Validate before one atomic write; failure preserves all
member state. Respect synchronized writes; per-entry APIs are not batch-safe.

Test batches, duplicates, stale state, locked vault and injected failure. If
infrastructure cannot prove rollback without a new framework, document that
gap; policy tests are not proof. Run approved profile and
`:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon` with cached
dependencies. Stop if date-only or legacy meaning is not preserved.
