# Metria vision spike task backlog

## Delivery protocol and checkpoint gates

These rules apply whether work is performed by a person or an implementation
agent. Do not batch native, model, privacy, and UI changes into one unverifiable
change.

- [ ] Work in small, scoped commits. Each commit must state one purpose and
  include its relevant automated test or a documented reason a test is not yet
  possible.
- [ ] Before changing the native runtime, record the exact runtime version,
  model artifact/version, license acknowledgement route, checksum, and download
  source in the implementation notes.
- [ ] Before each physical checkpoint, provide the build/install command, the
  expected device behavior, and the exact pass/fail evidence to capture.
- [ ] Do not advance past a checkpoint on code review or simulator evidence
  alone; use the target physical Android phone.
- [ ] Treat a crash, unbounded memory growth, unexpected network traffic, or an
  auto-save path as a blocking failure until diagnosed and retested.

### Gate A — App and native bridge

- [x] Expo development build installs and launches on the physical Android
  phone.
- [x] Native module reports runtime availability and handles load/unload without
  crashing the app.
- [x] Automated TypeScript tests and native build checks pass.

The checked Expo work above is retained as historical device evidence for the
artifact lifecycle. New inference work must be completed in the native Android
implementation; it does not inherit those checks as a pass.

### Gate A - Native Android shell and LiteRT bridge

- [ ] Native Kotlin/Compose debug build installs and launches on the physical
  Android phone.
- [ ] The app exposes a LiteRT runtime status and safely loads/unloads the
  selected local model without blocking the UI.
- [ ] Android unit tests and `assembleDebug` pass with the chosen supported
  Kotlin/LiteRT version pair.

### Gate B — Local model lifecycle

- [ ] Model download is explicit, cancellable, checks its checksum, and reports
  clear absent/downloading/ready/failed/removed states.
- [ ] The downloaded model loads after app restart and runs without network
  access.
- [ ] Storage consumed by the model is visible to the member and removable.

### Gate C — Image proposal safety

- [ ] A meal photo and a nutrition-label screenshot produce only validated,
  editable proposals.
- [ ] Blurry, empty, malformed, and non-food inputs fail safely to manual entry.
- [ ] No model result can save a meal or weight entry without a member tap.

### Gate D — Privacy and device quality

- [ ] Network inspection confirms no image, screenshot, prompt, or model output
  leaves Metria during inference.
- [ ] Any runtime telemetry is documented, consented, disabled, or replaced
  before proceeding.
- [ ] Ten consecutive physical-device runs complete without crash and record
  latency, storage, battery, and thermal evidence.

### Gate E — Go/no-go review

- [ ] Test corpus results meet every criterion in `vision-spike-spec.md`.
- [ ] A reviewer records the supported-device baseline and the decision to
  proceed, change runtime/model, or defer vision.

## 0. Decisions and setup

- [ ] Confirm Android-first device baseline and identify the physical test phone.
- [ ] Review Gemma licensing and model-download distribution requirements.
- [ ] Verify MediaPipe/AI Edge vision API maturity and telemetry behavior.
- [ ] Define the exact model artifact, version, checksum, and download source.
- [x] Preserve the Expo prototype as model-lifecycle evidence; do not extend it
  with the LiteRT runtime.
- [ ] Create the native Kotlin/Compose Android app and Android development-build
  pipeline in `native-android/`.
- [ ] Pin a Kotlin/LiteRT-LM version pair that compiles without suppressing
  Kotlin metadata checks.

## 1. Native inference bridge

- [ ] Add an Android `VisionRuntime` with `loadModel`, `inferImage`, `cancel`,
  `unloadModel`, and `runtimeStatus` operations.
- [ ] Keep inference off the Compose/UI thread.
- [ ] Implement cancellable loading and inference with clear memory cleanup.
- [ ] Add model-download state: absent, downloading, ready, failed, removed.
- [ ] Verify the model can run without network access after download.

## 2. Image and OCR input

- [ ] Add camera and photo-library permission flows.
- [ ] Resize/orient images locally and remove EXIF metadata before inference.
- [ ] Add screenshot selection.
- [ ] Add local OCR proof for nutrition labels.
- [ ] Implement no-retention image handling by default.

## 3. Safe proposal UX

- [ ] Define TypeScript types matching the proposal contract.
- [ ] Validate raw model output strictly; reject malformed/extra fields.
- [ ] Build the editable proposal sheet with visible uncertainty.
- [ ] Provide "Not right", "Ask again", and "Enter manually" paths.
- [ ] Ensure the spike has no persistence action.

## 4. Privacy and observability

- [ ] Add a first-use local-processing disclosure.
- [ ] Inspect network traffic during model download and inference.
- [ ] Document any runtime telemetry and consent/configuration decision.
- [ ] Add redacted diagnostics only: timing, memory status, model version,
  and failure category; never image/prompt/output content.

## 5. Verification

- [ ] Create the agreed non-sensitive test-image corpus.
- [ ] Run ten-image performance/quality test on the Android device.
- [ ] Capture cold/warm latency, storage, battery, and thermal observations.
- [ ] Test offline mode, app restart, cancellation, malformed model output,
  blurry image, empty image, and permission denial.
- [ ] Decide go/no-go against `vision-spike-spec.md` and record results.
