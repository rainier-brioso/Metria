# Task 03 — Review UI and blank prompt

**Prerequisites:** approved contract and persistence API. This is a draft until
the design gate names signatures, paths and test profile. Before UI edits, read
`docs/design/DESIGN.md`, relevant `FLOWS.md` and `SCREEN-SPECS.md`, and
`QUALITY-GATES.md`; resolve overlapping main-checkout design work first.

## Allowed proposed paths

- Add dedicated import screen/components under `native-android/app/src/main/java/app/metria/ui/`.
- Candidate host wiring: `native-android/app/src/main/java/app/metria/ui/screens/PrivacySettingsScreen.kt` and narrowly scoped integration tests, only after approval.
- Add presentation tests and a standalone canonical blank-prompt helper/test
  only where needed.
- Do not add feature UI to `MemberShell.kt` in this slice.

## Contract and acceptance

Use cancellable bounded picker reads; MIME is only a hint. No broad storage,
dependencies, copies, URI/raw logs or saved file contents. Preserve preview on
unlocked rotation. Request local-only picker where supported; cloud providers
may appear and need not be offline. Scope picker transition; discard on genuine
background, lock or process destruction. Never replay callbacks or autosave.
Show counts and safe errors before confirmation; disclose duplicates.

Prompt uses one canonical schema, contains no member data, and requires local
review without sharing. Verify accessibility, keyboard/insets, async states and
semantics where supported. Run approved profile and
`:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon` with cached
dependencies; physical gates remain for Task 04.
