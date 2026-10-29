# Separate onboarding recording app

Metria Demo uses the same onboarding implementation with a separate Android
application ID, `app.metria.demo`, and launcher label, `Metria Demo`. The regular
application remains `app.metria`. No fake onboarding screens or authentication
bypass are added. No records, model files or cloud credentials are copied.

Build from native-android:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDemo --no-daemon
```

Recording APK: app/build/outputs/apk/demo/app-demo.apk. It is a debug-derived
recording variant, not a public release. It reuses debug sources/assets and
dependencies without adding any production dependency. File-provider authorities
use the demo application ID. The normal weight URI scheme is not claimed and
normal-app weight shortcuts are disabled for this onboarding-focused variant.
Weight deep-link recording is outside this variant's acceptance scope.

## Recording

- Open **Metria Demo** for the fresh onboarding clip. Its internal brand text is
  the real Metria UI; the launcher name distinguishes the installations.
- Stop before downloading Gemma if only recording onboarding/provider choice.
  Downloading Gemma here creates another private copy; the normal app's existing
  model remains separate. Provider keys/passes are not inherited either.
- Use the regular app for prepared model/fictional-history clips, after checking
  the recording frame contains no personal information. Do not splice distinct
  providers into a purported single uninterrupted inference.
- Leave biometric setup/unlock as a member action. No onboarding completion,
  model download or test record creation is required merely to leave the first
  screen ready for recording.
- A normal uninstall of **Metria Demo only** removes its private model/data.
  It does not remove the normal app's private copy. No uninstall, clearing or
  reset is performed by this preparation. Resetting onboarding later requires
  an explicit decision for the demo app; never clear app.metria.

See the current checkpoint for installed APK identity and actual phone results.
