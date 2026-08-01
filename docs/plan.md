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

## Milestone 0 — Product and safety foundation

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

## Milestone 1 — Private local tracker

**Outcome:** a standalone mobile app that records and graphs member-confirmed
weight entries offline.

- [ ] Create Expo/TypeScript application shell and local development build.
- [ ] Implement encrypted biometric-protected local vault.
- [ ] Add a weight-entry flow: value, date/time, optional note, units, edit,
  and delete confirmation.
- [ ] Add 7-day, 30-day, and custom-range charts with moving-average option.
- [ ] Show trend descriptions without moralising language.
- [ ] Add export/delete policy screens; do not implement export until its
  encrypted format is designed.
- [ ] Test airplane mode, app restart, biometric cancellation, invalid values,
  and accessibility navigation.

**Acceptance:** after restart and airplane mode, a member can unlock and view
their stored weight history; no network request is required.

## Milestone 2 — Assisted text meal input

**Outcome:** natural-language meal descriptions become editable proposals, not
silent records.

- [ ] Define `propose_meal` action schema: ingredients, quantities, unit,
  preparation, assumptions, confidence, and source.
- [ ] Build the confirmation sheet with editable portion chips and questions.
- [ ] Integrate a nutrition data source behind an adapter.
- [ ] Calculate calories/macros deterministically from confirmed quantities.
- [ ] Display value ranges for uncertain portions and list assumptions.
- [ ] Save source/confidence with each meal; support edit/delete.
- [ ] Add daily and weekly nutrition summaries labelled as estimates where
  appropriate.
- [ ] Test ambiguous language, unsupported foods, unit conversion, zero/invalid
  values, and never-save-without-confirmation behavior.

**Acceptance:** "one chicken breast with half a plate of rice" creates an
editable estimated proposal with clear assumptions and only saves after a tap.

## Milestone 3 — Local action assistant

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

## Milestone 4 — Optional image and label assistance

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

## Milestone 5 — Private alpha and standalone release readiness

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

1. M0 safety and product decisions.
2. M1 private weight tracker.
3. M2 transparent text-assisted meals.
4. M3 local action assistant.
5. M4 vision/OCR only after M2/M3 usability validation.
6. M5 private alpha and store readiness.
