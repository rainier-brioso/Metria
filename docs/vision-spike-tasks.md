# Metria vision spike task backlog

## 0. Decisions and setup

- [ ] Confirm Android-first device baseline and identify the physical test phone.
- [ ] Review Gemma licensing and model-download distribution requirements.
- [ ] Verify MediaPipe/AI Edge vision API maturity and telemetry behavior.
- [ ] Define the exact model artifact, version, checksum, and download source.
- [ ] Create the Expo TypeScript app and Android development-build pipeline.

## 1. Native inference bridge

- [ ] Add an Android native module with `loadModel`, `inferImage`, `cancel`,
  `unloadModel`, and `runtimeStatus` methods.
- [ ] Keep inference off the React Native UI thread.
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
