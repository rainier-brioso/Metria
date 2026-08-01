# Vision spike implementation notes

## Gate A baseline

This first implementation adds the Expo/TypeScript shell and an Android native
bridge that reports a conservative `unavailable` status. It intentionally does
not load or run a model. `loadModel` and `unloadModel` are lifecycle probes only
until the runtime and artifact are selected through the required Gate B checks.
The generated `android/` project is intentionally ignored; `npx expo prebuild`
regenerates it from the checked-in app and local module sources.

## Runtime and model record

## Gate B artifact record

- Runtime: LiteRT-LM Android `0.13.0` API family; inference integration deferred.
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
