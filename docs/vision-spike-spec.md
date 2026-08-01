# Metria on-device vision feasibility spike

**Status:** approved for planning  
**Target:** Android development build on a physical phone  
**Purpose:** prove that Metria can privately turn a meal photo or nutrition-label
screenshot into an editable, structured proposal on-device.

## Decision being tested

Can a small multimodal model provide a useful food-entry proposal on a target
phone without sending the image or member text to an application server, causing
unacceptable heat/battery use, or bypassing member confirmation?

The spike is not a calorie-counting release and it is not medical software.

## In scope

- Capture or select one meal image and one nutrition-label screenshot.
- Run a local vision-language model through a native Android bridge in an Expo
  development build.
- Produce constrained structured output for food candidates, visible portions,
  preparation clues, unanswered questions, and confidence.
- Use local OCR for readable nutrition-label text where appropriate.
- Render an editable confirmation sheet.
- Record performance and privacy measurements.
- Work fully offline after the optional model download completes.

## Out of scope

- Saving meals, weights, chat history, or user accounts.
- Automatic logging or medical/nutrition advice.
- Exact calorie/macronutrient calculation.
- Continuous camera/video analysis.
- iOS implementation, cloud fallback, or Smart Gym integration.

## Proposed technical shape

```text
camera / photo library / screenshot
        |
        v
image preparation + local OCR (labels)
        |
        v
native on-device VLM runtime
        |
        v
strict JSON proposal validator
        |
        v
editable confirmation sheet (never auto-save)
```

The React Native layer owns presentation and confirmation. A native Android
module owns model lifecycle, image conversion, inference, cancellation, and
memory release. The model may suggest data but cannot call storage or network
APIs.

## Candidate model/runtime

Initial candidate: Gemma 3n E2B with a Google AI Edge / MediaPipe-compatible
mobile runtime. The implementation must verify the exact available model format,
vision API stability, licensing terms, and runtime telemetry before adoption.

The design must permit replacement of the runtime/model after the spike. The
model is an optional download, not a required app-bundle asset.

## Structured proposal contract

The model response is untrusted until parsed and validated.

```json
{
  "kind": "meal_proposal",
  "items": [
    {
      "name": "chicken breast",
      "visible_portion": "one breast",
      "estimated_grams_range": { "min": 120, "max": 200 },
      "preparation": "unknown",
      "confidence": "medium"
    }
  ],
  "assumptions": ["No sauce amount can be inferred from the image."],
  "questions": ["Was oil used for cooking?"],
  "overall_confidence": "low"
}
```

Validation rules:

- Only the documented fields are allowed.
- Food names, portions, and questions are display-only drafts.
- Numeric ranges must be positive, bounded, and have `min <= max`.
- `confidence` is one of `low`, `medium`, or `high`; it is not a truth claim.
- A proposal without items is a valid result and directs the member to manual
  entry.
- No model output can cause a vault write.

## Privacy and consent requirements

- Show an explicit first-use disclosure: image/text processed locally after
  model download; the member controls photo selection and deletion.
- Before runtime adoption, document and test whether runtime performance or
  usage telemetry leaves the device. If it can, obtain informed consent or use
  a telemetry-free configuration/runtime.
- Do not retain source meal images in the spike unless the tester explicitly
  asks to keep them for comparison; default is in-memory processing only.
- Do not log prompts, model outputs, photos, screenshots, or model paths in
  production diagnostics.
- Network monitoring must show no image/prompt upload during inference.

## Test corpus

Use non-sensitive, locally held test images only:

- Single plate: chicken, rice, and vegetables.
- Plate with several components and visible sauce.
- Packaged food nutrition label.
- Screenshot with readable nutrition data.
- Blurry/dark image.
- Empty/non-food image.
- Two culturally varied meals and Spanish-language prompts.

No personal medical data, faces, or third-party images without permission.

## Measurements

Record per test device and model version:

- Model artifact/download size and free storage before/after.
- First-load time and warm-load time.
- End-to-end inference latency for photo and screenshot.
- Peak memory / process survival.
- Battery percentage change and thermal warning during a ten-image run.
- Offline inference result.
- Percentage of proposals needing material correction.
- OCR correctness for printed calories/macros.
- Any observed network traffic or runtime telemetry.

## Go/no-go criteria

Proceed to Metria meal persistence only if all are true:

- Inference works offline after download on the current Android phone.
- No photo, screenshot, or prompt is uploaded by Metria during inference.
- The app remains responsive and does not crash during ten consecutive runs.
- Every result is an editable proposal; no result auto-saves.
- At least 8 of 10 test images yield a recognisably useful proposal or a safe
  "I cannot identify this" response.
- Performance, storage, and battery measurements are acceptable enough to set
  a supported-device baseline.

If any condition fails, retain manual entry as the primary route and either
evaluate a smaller model/runtime or defer local vision.
