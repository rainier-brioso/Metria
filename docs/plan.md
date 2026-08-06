# Metria product plan

## Goal

Validate that a private, low-friction wellness tracker helps people record
weight and meals more consistently than conventional forms, without presenting
nutrition estimates as exact facts.

## Non-goals for the first release

- Medical diagnosis, treatment, or eating-disorder coaching.
- Automatic calorie logging without member confirmation.
- Gym/staff access to personal weight or food history.
- A cloud account, social feed, advertisements, or a subscription system.
- Photo-based food logging in the first build.

## Milestone 0 — On-device vision feasibility spike

**Outcome:** prove the core differentiator before ordinary tracker features.

- [ ] Complete the native Android-first vision spike specified in
  [vision-spike-spec.md](vision-spike-spec.md).
- [ ] Complete the task backlog in
  [vision-spike-tasks.md](vision-spike-tasks.md).
- [ ] Make and record an evidence-based go/no-go decision.

**Acceptance:** a meal photo or nutrition-label screenshot produces an editable,
offline, on-device proposal on a physical Android phone, with the stated privacy
and performance evidence.

**Implementation control:** M0 is implemented as a native Android application
in `native-android/`. The earlier Expo experiment remains preserved as evidence
for the model-artifact lifecycle, but is not a production runtime path. Follow
the commit and physical-device checkpoint
gates in [vision-spike-tasks.md](vision-spike-tasks.md). An implementation agent
may not treat simulator output, unit tests, or a successful build as sufficient
evidence for a native/model milestone.

## Milestone 1 — Product and safety foundation

**Outcome:** precise personal-data boundaries and trustworthy language before
building features.

- [ ] Write the privacy notice in plain language: local-only data, deletion,
  optional future backup, and no default sharing.
- [ ] Define assistant safety rules: supportive/non-judgmental tone, no medical
  claims, escalation text for health concerns, and no automatic writes.
- [ ] Define nutrition confidence labels: verified label, member-confirmed
  database match, and AI estimate.
- [ ] Choose initial supported locale, units (kg/lb), and food data source.
- [ ] Establish an accessibility baseline: dynamic type, screen reader labels,
  contrast, and reduced motion.

**Acceptance:** a reviewer can explain who sees each data type and why every
estimated calorie number is visibly qualified.

**Initial decision record:** [M1 product and safety foundation](m1-product-safety-foundation.md).

## Milestone 2 — Private local tracker

**Outcome:** a standalone mobile app that records and graphs member-confirmed
weight entries offline.

- [ ] Create Android/Kotlin application shell and local development build.
- [ ] Implement encrypted biometric-protected local vault.
- [ ] Add a weight-entry flow: value, date/time, optional note, units, edit,
  and delete confirmation.
- [ ] Add Android App Actions voice handoff for weight entry: Assistant extracts
  a proposed value/date and opens Metria's pre-filled confirmation screen;
  Assistant must never save an entry directly.
- [ ] Handle the voice handoff safely when the vault is locked: unlock first,
  then restore the pending validated proposal without exposing it in URLs,
  notifications, or diagnostic logs.
- [ ] Test misheard/invalid values, omitted dates/units, cancellation, vault
  unlock failure, and the current custom-intent locale limitations.
- [ ] Add 7-day, 30-day, and custom-range charts with moving-average option.
- [ ] Show trend descriptions without moralising language.
- [ ] Add export/delete policy screens; do not implement export until its
  encrypted format is designed.
- [ ] Test airplane mode, app restart, biometric cancellation, invalid values,
  and accessibility navigation.

**Acceptance:** after restart and airplane mode, a member can unlock and view
their stored weight history; no network request is required.

## Milestone 3 — Assisted text meal input

**Outcome:** natural-language meal descriptions become editable proposals, not
silent records.

- [ ] Define `propose_meal` action schema: ingredients, quantities, unit,
  preparation, assumptions, confidence, and source.
- [ ] Build the confirmation sheet with editable portion chips and questions.
- [ ] Integrate the versioned offline USDA Foundation Foods catalog behind an
  adapter. Keep its complete source releases available only as development
  inputs; the member vault must never contain source files or diagnostics.
- [ ] Calculate calories/macros deterministically from confirmed quantities.
- [ ] Display value ranges for uncertain portions and list assumptions.
- [ ] Save source/confidence with each meal; support edit/delete.
- [ ] Add daily and weekly nutrition summaries labelled as estimates where
  appropriate.
- [ ] Test ambiguous language, unsupported foods, unit conversion, zero/invalid
  values, catalog version attribution, no-network lookup, and
  never-save-without-confirmation behavior.

**Acceptance:** "one chicken breast with half a plate of rice" creates an
editable estimated proposal with clear assumptions and only saves after a tap.

## Milestone 4 — Local action assistant

**Outcome:** chat is helpful but constrained, private, and auditable.

- [ ] Add a chat interface with local conversation storage and clear deletion.
- [ ] Implement the allowlisted action dispatcher from `architecture.md`.
- [ ] Start with a mocked/local rule parser to validate the complete UI flow.
- [ ] Evaluate small text models on target Android and iPhone hardware for
  latency, battery, RAM, language quality, and offline packaging size.
- [ ] Select a runtime only after measurement; make model download optional.
- [ ] Add a visible "AI estimate" label and explanation on all model proposals.
- [ ] Test prompt injection attempts, unsupported actions, confirmation
  cancellation, and model unavailability.

**Acceptance:** text can propose a weight or meal entry, but no assistant
output can bypass schema validation and the confirmation sheet.

## Milestone 5 — Persisted image and label assistance

**Outcome:** a photo or screenshot assists entry while preserving accuracy and
privacy.

- [ ] Add explicit photo/screenshot consent and local processing disclosure.
- [ ] Implement OCR for nutrition labels and editable field extraction.
- [ ] Prototype a small on-device vision-language model for meal suggestions.
- [ ] Require portion/preparation confirmation before nutrition calculation.
- [ ] Measure model download size, latency, battery drain, heat, and failure
  behavior on low/mid/high-tier devices.
- [ ] Test images with multiple foods, hidden ingredients, unreadable labels,
  screenshots, and no-network operation.

**Acceptance:** the app never converts an image directly into a saved meal;
the member sees the interpretation, uncertainty, and assumptions first.

## Milestone 6 — Private alpha and standalone release readiness

**Outcome:** a small independent tester group can use Metria safely.

- [ ] Add encrypted archive/export and restoration tests.
- [ ] Complete uninstall/reinstall, new-device, and vault-loss behaviour.
- [ ] Conduct usability testing with manual input and assistant proposals.
- [ ] Review privacy policy, store metadata, support channel, and crash
  reporting data minimisation.
- [ ] Establish App Store/Google Play account, signing, and release workflows.
- [ ] Decide whether Smart Gym module integration is valuable after validated
  independent use.

**Acceptance:** a tester can understand the data policy, use the app offline,
recover only through the stated archive route, and submit feedback without
exposing their health data.

## Delivery order

1. M0 on-device vision feasibility spike.
2. M1 safety and product decisions.
3. M2 private weight tracker.
4. M3 transparent text-assisted meals.
5. M4 local action assistant.
6. M5 persisted image/OCR assistance.
7. M6 private alpha and store readiness.
