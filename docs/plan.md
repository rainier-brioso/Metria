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
- [ ] Offer opt-in, encrypted cycle-aware context: member-entered period-start
  dates may qualify short-term weight variation, never infer identity, diagnose,
  predict cycles, alter goals, or leave the device.
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

**Product decision:** the on-device assistant is a required part of Metria's
first-run setup, downloaded explicitly into private app storage rather than
bundled with the store package. It supports voice/text understanding, drafted
actions, navigation, and grounded local insights throughout the app. Manual
forms remain available when the model is busy or unsuitable; they are a
reliable fallback, not Metria's primary product identity. The assistant may
never bypass host validation. A valid weight may use direct save with visible
Undo/Edit and durable replay protection; meals remain reviewed drafts. Edits,
deletions, goals, privacy, recovery, and model management stay explicit UI flows.

- [x] Add encrypted local conversation storage, 24-hour retention, and clear deletion.
- [x] Implement the transitional allowlisted action dispatcher from `architecture.md`.
- [x] Select and validate Gemma 4 E2B with LiteRT-LM on the target Android phone.
- [x] Complete `docs/assistant-agent-spec.md` Gate A using LiteRT's native persistent
  conversation, manual tool calls, and five read-only tools. See
  [M4 assistant checkpoints](m4-assistant-checkpoints.md).
- [ ] Bind factual output as typed atomic facts so valid dates, weights, meals,
  totals, goals, periods, and counts cannot be reassociated incorrectly.
- [ ] Complete Gate B multi-turn/restart/token-budget physical acceptance.
- [ ] Complete Gate C bilingual guardrail, scope, prompt-injection, AI disclosure,
  and member-initiated reporting acceptance.
- [ ] Complete Gate D idempotent native weight and meal action tools.
- [ ] Evaluate later runtime/model candidates on target Android hardware for
  latency, battery, RAM, language quality, and offline packaging size.
- [x] Require an explicit model download during first-run setup.
- [x] Add a visible "AI estimate" label and explanation on model proposals.
- [ ] Test prompt injection attempts, unsupported actions, confirmation
  cancellation, and model unavailability.

**Acceptance:** see the measurable gates in `docs/assistant-agent-spec.md`.

## Milestone 5 — Persisted image and label assistance

**Outcome:** a photo or screenshot assists entry while preserving accuracy and
privacy.

- [ ] Add explicit photo/screenshot consent and local processing disclosure.
- [ ] Implement OCR for nutrition labels and editable field extraction.
- [ ] Prototype a small on-device vision-language model for meal suggestions.
- [x] Use a compact, bounded on-device meal-proposal schema (two strongest
  visible candidates); strictly validate every model field and fail closed on
  unsupported values. Members can add or edit secondary foods during review.
- [ ] Require portion/preparation confirmation before nutrition calculation.
- [ ] Measure model download size, latency, battery drain, heat, and failure
  behavior on low/mid/high-tier devices.
- [ ] Test images with multiple foods, hidden ingredients, unreadable labels,
  screenshots, and no-network operation.

**Acceptance:** the app never converts an image directly into a saved meal;
the member sees the interpretation, uncertainty, and assumptions first.

## Milestone 5.5 — Private dish memory and retrieval

**Outcome:** member-confirmed corrections make repeated personal meals easier
without pretending that the vision model retrained itself or recognized a dish
with certainty.

- [x] Store a bounded structured `PersonalDish` record inside the encrypted
  vault, including its member-chosen name, confirmed components, optional
  aliases, confirmation history, and optional reduced reference thumbnail.
- [x] Let the member opt each reviewed dish in or out before it is remembered.
- [x] Add deterministic, explainable retrieval over normalized names and
  aliases, confirmed catalog identities, component overlap, preparation, and
  confirmation history. Return at most three candidates.
- [x] Surface remembered dishes as suggestions during review; accepting,
  correcting, or rejecting a suggestion must remain reversible until the meal
  is saved.
- [x] Use an exact encrypted whole-meal reference only as a clearly labelled
  same-photo fallback for up to three candidates. Do not present it as
  dish-level recognition.
- [x] Save the reviewed meal, food corrections, and remembered-dish updates in
  one encrypted-vault operation so a partial write cannot teach memory without
  saving the meal, or vice versa.
- [x] Give the assistant a bounded `search_my_saved_dishes` capability that
  returns only relevant structured candidates and never exposes the full vault.
- [x] Add member-facing saved-dish management under Privacy, including removal.
- [x] Verify on the physical Android pilot phone that a remembered San Jacobo
  survives an in-place app update, is offered from exact same-photo context,
  and replaces the provisional Cutlet identity only after member acceptance.
- [ ] Test false matches, renamed dishes, grouped components, hidden
  ingredients, duplicate names, persistence/restart, and repeat-meal use on a
  physical Android phone.

**Acceptance:** after the member confirms and remembers a corrected dish, a
later similar analysis can offer “Looks like your …” with an explanation; the
member may accept, edit, or ignore it, and no memory or meal change is persisted
until the final reviewed save succeeds.

**Future enhancement:** combine this structured retrieval with an on-device
visual embedding as a candidate signal. Visual similarity supplements rather
than replaces the confirmed component structure, and does not require a vector
database for the expected personal collection size.

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

### Recovery archive decision (M6)

Metria recovery is a member-held `.metria` archive encrypted with a
member-created passphrase. It is independent from the device-bound biometric
key, so it can be restored to a newly created vault on another phone. The
archive includes member-confirmed weights, meals, optional confirmed reference
thumbnails, goals, unit choice, cycle context, food corrections, and saved
private dishes. It
excludes Android Keystore keys, model artifacts, temporary images, chat,
diagnostics, and local inference audit metadata. Import must authenticate and
validate the complete archive, show record counts, and obtain explicit
replacement confirmation before writing the receiving vault.

1. M0 on-device vision feasibility spike.
2. M1 safety and product decisions.
3. M2 private weight tracker.
4. M3 transparent text-assisted meals.
5. M4 local action assistant.
6. M5 persisted image/OCR assistance.
7. M5.5 private dish memory and retrieval.
8. M6 private alpha and store readiness.
