# Metria UI quality gates

A material UI task is complete only when the applicable gates pass. Record failures as product work, not visual polish debt.

## 1. Design-contract gate

- Screen purpose and primary action match `SCREEN-SPECS.md`.
- UI uses semantic theme tokens and shared components.
- No development language is visible in member-facing copy.
- Estimate honesty follows `docs/m1-product-safety-foundation.md` without repeating long explanations.
- A new interaction is represented in `FLOWS.md` when it changes the approved journey.

## 2. State gate

Verify applicable states:

- Initial/idle.
- Loading or model warming.
- Empty data.
- Successful data.
- Partial nutrition or uncertain model result.
- Recoverable error.
- Cancellation.
- Background and foreground return.
- Process/configuration recreation where the workflow promises restoration.

No loader may blink between states, remain at zero indefinitely, silently rerun completed work, or lose the member's draft without an explicit policy.

## 3. Layout gate

Test on the physical reference phone in:

- Portrait.
- Landscape.
- Gesture navigation and three-button navigation when available.
- Keyboard closed and keyboard open on the last editable field.
- Default font size and at least one enlarged system font setting.

Pass conditions:

- No overlap with system bars, bottom navigation, floating controls, or keyboard.
- Primary action remains reachable.
- Content can scroll far enough to expose the complete final row and action.
- Rotation does not unexpectedly lock the vault, dismiss a confirmed external flow, or reset the active task.

## 4. Accessibility gate

- Interactive target is at least 48 × 48 dp.
- Icon-only controls have meaningful descriptions.
- Selection, confidence, progress, and error are not communicated by color alone.
- TalkBack traversal follows visual/task order.
- Dynamic text does not clip or overlap.
- Charts expose a textual summary and selected value.
- Animation respects reduced motion or has a nonanimated equivalent.

Add automated Compose accessibility checks when the UI-test dependency is introduced, and retain manual TalkBack verification for critical flows.

## 5. Interaction gate

- Back and cancel return to the expected prior context.
- Tapping a feed row opens its editor; destructive actions are not ambiguous or exposed beside routine navigation.
- Removing a photo or ingredient keeps the current editor open.
- A save action cannot be triggered twice accidentally.
- Voice/listening state is obvious, stops automatically, and can always be cancelled.
- Async completion is shown once and does not replay on tab changes.

## 6. Visual-review gate

For each redesigned screen:

1. Capture a before screenshot on the physical phone.
2. Capture the new idle/data state at the same approximate viewport.
3. Capture one difficult state: keyboard, empty, loading, partial, or error.
4. Review hierarchy at thumbnail size: the title, main information, and primary action must remain obvious.
5. Review full size for alignment, spacing rhythm, typography, clipping, and touch affordances.
6. Keep approved evidence under `docs/design/evidence/<slice>/`; do not place temporary screenshots in the repository root.

Third-party reference screenshots are inspiration only and should not be committed unless their license explicitly permits redistribution.

## 7. Build and regression gate

Run from `native-android/`:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

Then install with `adb install -r` so the encrypted local dataset is retained. Do not clear app data as part of ordinary UI verification.

For a changed primary flow, exercise at least:

- Entry to the flow.
- Its primary action.
- Cancel/back.
- Save and reopen.
- Rotation.
- Keyboard-visible interaction when applicable.

## 8. Acceptance note

Record a short note containing:

- Device and Android version.
- Build/commit identifier.
- Screens and states checked.
- Accessibility checks performed.
- Known issues intentionally deferred.
- Links to approved screenshots.

Unit tests and a successful build are necessary evidence, but they do not replace physical-device UX acceptance.
