# Vision spike implementation notes

## Native Android migration decision - 2026-08-02

**Chosen direction:** native Android (Kotlin + Jetpack Compose) in
`native-android/`, with direct LiteRT-LM integration.

**Why:** LiteRT-LM Android 0.13.0 and 0.10.2 publish Kotlin 2.3 metadata.
The Expo SDK 53, 56, and 57 experiments compiled their own Android sources with
Kotlin 2.0.21 or 2.1.20, so none could consume that artifact. Compiler metadata
suppression is not an acceptable production foundation. Native Android gives
Metria control of the supported Kotlin/Gradle pair and follows LiteRT-LM's
intended API.

**Migration boundary:** preserve the Expo project unchanged as the physical
artifact-lifecycle prototype. Port and retest download, checksum, cancellation,
restart discovery, storage reporting, and removal before declaring a native
Gate B pass. Do not copy the already-downloaded model from a member device or
infer any native Gate completion from the Expo result.

## Native compile checkpoint - 2026-08-02

- Project: `native-android/`, standalone Kotlin/Jetpack Compose application.
- Runtime dependency: `com.google.ai.edge.litertlm:litertlm-android:0.13.0`.
- Compiler: Kotlin `2.3.10`; no metadata-suppression flag is present.
- Build command: `./gradlew.bat :app:assembleDebug --no-daemon`.
- Result: passed locally after aligning Java and Kotlin bytecode targets to 17.
- Scope: dependency linkage only; no LiteRT engine initialization, model load, or
  inference claim yet. The next required evidence is physical-device Gate A.

## Native lifecycle physical evidence - 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- Installation: native package `app.metria` installed separately from the
  historical Expo prototype (`app.metria.mobile`).
- Download: the member explicitly downloaded the pinned 2,588,147,712-byte
  artifact through the native Kotlin app.
- Restart recovery: the native app was force-stopped and relaunched; it
  reported `verifying`, then `ready`, without starting another download.
- Data isolation: the Expo prototype was cleared and uninstalled. Its model was
  not reused; the native app owns its app-private verified model file.
- Not yet exercised: native cancellation during an active download, native
  removal, LiteRT engine initialization, offline inference, and image input.

## Native LiteRT initialization evidence - 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- Action: the member selected `Initialize LiteRT` after native model
  verification completed.
- Result: the state changed to `loaded` without a crash.
- Runtime configuration: CPU text backend, GPU vision backend, CPU audio
  backend, 256 maximum output tokens, and one input image maximum. No prompt,
  image, conversation, or persistence operation was run in this checkpoint.

## Native offline generation evidence - 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- First attempt: GPU vision delegation failed on this device with an OpenGL
  shared-memory-manager limitation. CPU vision removed that delegate failure.
- Second attempt: a `maxNumTokens` value of 256 failed LiteRT tensor allocation
  (`DYNAMIC_UPDATE_SLICE`). This Gemma 4 artifact requires the 4096-token
  allocation used by its native runtime guidance.
- Final configuration: CPU text, CPU vision, CPU audio, 4096 maximum tokens,
  and one input image maximum.
- Result: after initialization, the fixed non-personal offline test returned
  `METRIA_READY` on the physical device.
- Scope: this proves local text generation only. No photo has been selected,
  processed, retained, or sent anywhere; no model output can create an entry.

## Native local image inference evidence - 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- Input: the member explicitly selected a non-sensitive test image through the
  Android system photo picker; no broad photo-library permission was requested.
- Preparation: Metria decoded, orientation-normalized, resized, and recompressed
  the input into a temporary JPEG without EXIF metadata.
- Result: the local model returned a visible food description with questions and
  assumptions. It remained an untrusted, non-saving draft.
- Cleanup: after the local analysis, `run-as app.metria ls cache` returned no
  files. The temporary prepared image had been deleted.
- Scope: raw model text is displayed only for this transport checkpoint. Strict
  JSON validation and the editable proposal interface remain required before
  Gate C can pass.

## Native validated image-proposal evidence - 2026-08-02

- Physical device: Samsung SM-S928B, Android 16.
- Result: a second local image run completed after the parser fix and produced
  an accepted structured draft. The UI exposes editable food and portion fields,
  plus assumptions and questions, with an explicit no-save statement.
- Validation: only the exact documented proposal fields, positive bounded gram
  ranges, and allowed confidence values can reach this UI. A single Markdown
  JSON fence is normalized; all other wrapper text, malformed JSON, and extra
  fields are rejected.
- Safety regression found and fixed: an unescaped closing brace in the fence
  normalizer caused a `PatternSyntaxException`; it was a Metria parser bug, not
  a model or image failure.
- Cleanup check: `run-as app.metria ls cache` returned no temporary image file
  after accepted analysis. There was no fatal-exception or native crash evidence.

## Gate A baseline

This first implementation adds the Expo/TypeScript shell and an Android native
bridge that reports a conservative `unavailable` status. It intentionally does
not load or run a model. `loadModel` and `unloadModel` are lifecycle probes only
until the runtime and artifact are selected through the required Gate B checks.
The generated `android/` project is intentionally ignored; `npx expo prebuild`
regenerates it from the checked-in app and local module sources.

## Runtime and model record

## Gate B artifact record

- Runtime: LiteRT-LM Android `0.13.0` API family; direct Expo SDK 53
  integration is blocked because it requires Kotlin 2.3 metadata and Expo
  SDK 53 rejects Kotlin 2.3. Do not bypass the compiler metadata check; select
  a build-compatible runtime path before enabling image inference.
- Model artifact/version: `gemma-4-E2B-it.litertlm`, repository
  `litert-community/gemma-4-E2B-it-litert-lm`, revision
  `9262660a1676eed6d0c477ab1a86344430854664`.
- License acknowledgement route: the model repository declares Apache-2.0;
  Gemma 4 base-model terms remain applicable and must be reviewed before
  distribution.
- Expected size: `2,588,147,712` bytes.
- SHA-256/checksum recorded from the Hugging Face `X-Linked-ETag` header:
  `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`.
- Download source: the pinned Hugging Face `resolve/<revision>/...` URL in
  `MetriaVisionModule.kt`.

This checkpoint implements download, cancellation, verification, restart
discovery, visible storage usage, and removal. It does not initialize LiteRT-LM
or expose image/text inference yet.

## Gate B physical checkpoint

- Build/install command: `npx expo run:android` with the Android SDK configured.
- Expected behavior: status starts `absent`; download is explicit and reports
  progress; cancellation removes the partial file; a verified model reports
  `ready` after app restart; remove returns `removed` and frees the model file.
- Pass evidence: physical device model/OS, state transitions, bytes before/after
  removal, checksum verification result, and no crash during cancellation/restart.
- Fail evidence: unverified file accepted, partial file retained as ready,
  download that cannot cancel, incorrect storage accounting, or a crash.

## Gate A physical checkpoint

- Build/install command: `npx expo run:android` from the repository root after
  Android SDK and a physical device are configured.
- Expected behavior: the app launches, shows `Runtime: unavailable`, and the
  “Check runtime” action remains safe. Leaving the screen invokes unload without
  crashing. No image, prompt, model, or persistence API exists in this bridge.
- Pass evidence: physical Android device model/OS, install and launch result,
  captured runtime status, and evidence that load/unload did not crash.
- Fail evidence: crash log, unexpected state transition, or any network request
  caused by the bridge. Simulator and automated tests are not sufficient to
  pass this checkpoint.

## Gate A evidence — 2026-08-01

- Physical device: Samsung SM-S928B, Android 16, connected by ADB.
- Build: `:app:assembleDebug` completed successfully after configuring the
  installed Android SDK path.
- Install/launch: the debug APK installed successfully and rendered `Metria
  vision spike`, `Runtime: absent`, and `Storage: 0 / 2,588,147,712 bytes`.
- Safe lifecycle check: `Reload model` reported `The local model is not
  installed.`; a forced process restart returned to the same `absent` state
  without a crash.
- Scope: no model was downloaded, no model runtime was initialized, and no
  image/prompt/persistence function was exercised.

## Gate B correction before physical download

The lifecycle manager now verifies a persisted model asynchronously before it
can report `ready`, removes partial files after cancellation or failure, and
uses a download generation to prevent a cancelled/removed background download
from installing a late artifact. Gate B remains unpassed until the explicit
2.59 GB model download and physical lifecycle evidence are completed.

## Gate B partial physical evidence — 2026-08-01

- Physical device: Samsung SM-S928B, Android 16, connected by ADB.
- Download: the member explicitly downloaded the pinned artifact. The app
  reported `2,588,147,712 / 2,588,147,712 bytes` and `Runtime: ready`.
- Restart recovery: after force-stopping and relaunching the app, it again
  reported `Runtime: ready` with the exact expected byte count. The lifecycle
  verification path completed without a crash.
- Not yet exercised: cancellation during an active download, removal, an
  offline network assertion, LiteRT-LM initialization, or image inference.
  The model is retained on the phone so those later runtime tests do not
  require another 2.59 GB download.
