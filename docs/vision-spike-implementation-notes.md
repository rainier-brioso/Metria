# Vision spike implementation notes

## Gate A baseline

This first implementation adds the Expo/TypeScript shell and an Android native
bridge that reports a conservative `unavailable` status. It intentionally does
not load or run a model. `loadModel` and `unloadModel` are lifecycle probes only
until the runtime and artifact are selected through the required Gate B checks.
The generated `android/` project is intentionally ignored; `npx expo prebuild`
regenerates it from the checked-in app and local module sources.

## Runtime and model record

- Runtime: not selected; no native inference runtime changed.
- Model artifact/version: not selected.
- License acknowledgement route: not applicable until selection.
- Checksum: not applicable until selection.
- Download source: not applicable until selection.

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
