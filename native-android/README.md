# Metria Native Android

This is the active Android-first M0 implementation: Kotlin, Jetpack Compose,
and direct LiteRT-LM integration. The repository-root Expo project is preserved
only as prior model-artifact lifecycle evidence; do not add inference features
there.

## First local build

Build the independent native project with its checked-in wrapper:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

Do not use Kotlin metadata-suppression flags.
