# Onboarding safe-area repair - 2026-10-29

Target: Samsung SM-S928B / Android 16, separate app.metria.demo. Only Demo was
updated. APK SHA-256:
`9240dc7669cf1efc0a2916db4ce903f79867ccf3f00ce2931c9b87758b8fed4d`.

Before: physical portrait screenshot showed Continue overlapping the three-button
system navigation bar. Reviewed the actual screen before installation.

After: safeDrawingPadding reserves system areas; a fixed footer holds the page
indicator and Continue/Back while explanatory content scrolls independently.
Choice chips use FlowRow; page uses rememberSaveable. The three pages' copy,
mode selection and completion behavior are unchanged. No new dependency/test
infrastructure, automatic setup, inference or model download.

## Physical checks

| Check | Evidence/result |
| --- | --- |
| Portrait, three-button navigation, font scale 1.0 | All three pages inspected; footer clear of navigation; actual Continue transitions 1 -> 2 -> 3 |
| Page 3, portrait, font scale 1.3 | Entire description/choices reachable; chips wrap onto two rows without clipping; footer readable |
| Page 3, landscape, font scale 1.3 | Page retained; content viewport scrolls to both complete assistant choices and setup description; footer remains above/beside system navigation |
| Back and initial state | Back transitions 3 -> 2 -> 1; Demo relaunched at page 1 without completion or model setup |
| Settings restoration | font_scale 1.0, accelerometer_rotation 1, user_rotation 0 restored and read back |
| Original app isolation | app.metria lastUpdateTime unchanged; no install/clear/force-stop/data inspection of original app |

Before/after and stress screenshots were immediately inspected outside the
repository and deleted after review (status-bar metadata excluded from retained
evidence). android layout remains unavailable from the previously observed
instrumentation response failure; it was not repeatedly retried.

Full testDebugUnitTest, assembleDebug and assembleDemo gate passes, 769 tests,
zero failures/errors/skips. install --user 0 -r succeeded for Demo only. Cold
launch after update Status ok, 1138 ms; final return to first page 1093 ms.

Scope limits: gesture navigation, TalkBack traversal and process-death restoration
are not physically exercised. Keyboard is not applicable to these pages (no
text entry); onboarding completion, biometric creation, model download and cloud
activation intentionally remain untouched. This is targeted acceptance of the
safe-area/scroll repair, not full onboarding or full app acceptance.
