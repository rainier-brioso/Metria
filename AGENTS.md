# Metria repository guidance

## Product contract

- Metria is a private, offline-first Android wellness companion. Member records, prompts, photos, model output, and inferred health context must not leave the device.
- Read `docs/architecture.md` and `docs/m1-product-safety-foundation.md` before changing persistence, assistant actions, nutrition estimates, image handling, biometrics, or recovery.
- Assistant and vision output are drafts. Preserve schema validation, deterministic nutrition calculation, visible uncertainty, and required member review.

## UI work

- Before changing a user-facing flow, read `docs/design/DESIGN.md`, the relevant flow in `docs/design/FLOWS.md`, the relevant contract in `docs/design/SCREEN-SPECS.md`, and `docs/design/QUALITY-GATES.md`.
- Design the hierarchy and states before implementation. One screen gets one primary purpose and one visually dominant action.
- Prefer progressive disclosure. Summary cards are readable at a glance; editing happens in a focused screen or sheet. Do not expose development terminology or a wall of form fields to members.
- Use the Metria theme, semantic design tokens, and shared components. Do not add raw colors, one-off spacing, arbitrary corner radii, or duplicate component styles inside feature screens.
- Every async flow must define idle, loading, success, empty, partial, error, cancellation, and restoration behavior as applicable.
- Every text-entry flow must remain usable with the keyboard visible. Preserve system insets, scroll the focused control into view, and keep the primary action reachable.
- Do not use color alone to communicate nutrition confidence, errors, trends, or selection. Pair color with an icon, shape, pattern, or concise label.
- Provide meaningful accessibility labels and state descriptions. Interactive targets must be at least 48 dp and layouts must tolerate Android font scaling.
- Treat the screenshots described in `docs/design/DESIGN.md` as inspiration for hierarchy and calmness, not assets to copy.

## Android structure

- New production UI belongs under `native-android/app/src/main/java/app/metria/ui/`, separated into `theme`, `components`, `navigation`, and `screens` as the redesign proceeds.
- Do not add new feature UI to `MemberShell.kt`. When materially redesigning an existing screen, extract that screen and the reusable primitives it needs without mixing unrelated behavior changes into the same patch.
- Keep business rules, encrypted-vault operations, model lifecycle, and parsing outside composables. Composables receive explicit state and emit user intents.
- Prefer stateless reusable components. Screen state must survive recomposition and expected configuration changes; persistent drafts require an explicit persistence decision.
- Use Material 3 semantics and components as the accessibility foundation, customized through the Metria theme.

## Verification

- Run `native-android/gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` after Android changes.
- Add Compose semantics/UI tests for materially changed flows when the test infrastructure supports them. Do not substitute unit tests for physical interaction checks.
- For every material screen change, verify the relevant checklist in `docs/design/QUALITY-GATES.md` on the target Android phone and capture before/after evidence.
- A UI task is incomplete if content is clipped, obscured by the keyboard or system bars, reset unexpectedly on rotation, inaccessible without color, or inconsistent with the approved screen contract.

## Change control

- Preserve unrelated working-tree changes.
- Do not add production dependencies, commit, push, clear app data, uninstall the app, or replace encrypted local data without explicit approval.
- Update the applicable design document when an approved UX decision changes the product contract. Keep `AGENTS.md` concise; detailed visual guidance belongs in `docs/design/`.
