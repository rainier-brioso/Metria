# Metria product plan

Current checkpoint approved for commit and push to `main` (2026-10-16): natural
saved-dish requests, normal-intent fallback and timed drafting, retained draft
acknowledgements, request diagnostics and canonical conversation refresh.
Independent reviews and the complete 583-test/debug/release/lint gate pass.
Named phone checks cover non-food normal response, unsaved draft, saved-dish
MiniLM review and message retention across the minute refresh. Broader lifecycle,
ambiguity, failed-save and accessibility gates stay open in the companion spec.
The historical notes below preserve how the checkpoint was reached; earlier
pending-approval and failed-test notes are superseded only by the named results.

Confirmed live-delivery cause (2026-10-16): the meal retry reconciles request 2,
then `conversation_sync_normalized` clears its identity during inference. A
canonical-equivalent saved snapshot is being mistaken for different Chat history.
Normalized-equivalence preservation passed independent review and the
583-test/debug/release gate, with genuine change/expiry/lock/clear invalidation
intact; no stale-result gate bypass. Member now confirms both requested meal
cases work. Trace confirms unsaved Gemma draft delivery and saved MiniLM read
delivery, with no refresh reset/mismatch across the successful sequence.
Named regression is closed; broader acceptance gates remain documented open.

Current phone blocker (2026-10-16): the non-food test reaches a validated native
reply, but Chat rejects its request identity. The 577-test diagnostic build is
installed to trace fixed reconciliation/sync-reset codes without message content.
Actual identity-loss cause and visible non-food acceptance remain open; stale
guards are intact and no delivery fix is claimed.
The diagnostic replay now shows a normal response with reconciled continuation,
accepted audit and Chat consumption; no reset/mismatch on that turn. The earlier
failure remains unexplained. Ordinary meal/exact saved regressions remain pending.

Review fixes approved 2026-10-16 are implemented and independently reviewed;
576-test/debug/release gate passed, phone checks pending: normal intent selection once
after uncertain/no-match saved probes, with repeat-probe suppression and the
existing timed native draft path only after positive meal intent.

Checkpoint review found an open bare-consumption gap: some non-food `I had ...`
statements are masked then forced into meal extraction after no saved match.
Fix normal-intent fallback without a repeated saved probe before treating the
slice as commit-ready. Existing exact-meal and retention phone passes remain
valid; no source change or new build was made during this review.

Bare consumption probe (approved 2026-10-15) is implemented and passes the
570-test/debug/release gate; bare exact-name Review/Discard phone acceptance
passes with MiniLM and Gemma skipped: “I had san jacobo”
tries a saved dish without special keywords, offers review/choice for matches
and continues ordinary meal drafting on no match. Original text, opaque names,
one-shot cleared-search handoff and final member Save remain required. Scope:
`assistant-companion-bundle.md`. No new commit/push authorization.

The ordinary unsaved-meal draft phone check also worked, but leaving Review
lost the transient assistant card and left only the member question. A scoped
historical acknowledgement and persist-before-review fix passed independent
review and the 573-test/debug/release gate, and is installed for acceptance;
no draft payload persistence or missing-answer gate bypass is introduced.
The member confirms Review/leave-without-saving retains the assistant message,
including after closing/reopening Chat. Broader lifecycle gates remain open.

Natural saved-dish recognition extension (approved 2026-10-15) passed independent
review and the 568-test/debug/release gate; installed in place and launched
from committed checkpoint `6cfb4a0`: polite/natural recall and consumption frames,
bounded command corrections with opaque names, existing review-only draft and
ambiguity choice. No automatic save, inferred usual dish, new dependency or
commit/push authorization. Scope: `assistant-companion-bundle.md`. The missing
answer issue remains explicitly deferred. Member-observed functionality passed;
retained route fell back on MiniLM margin. A classifier-only named-slot masking
follow-up passed review and the 568-test/build gate and is installed; fast
saved-dish recall and consumption/review/discard phone checks both pass with
MiniLM, fresh saved-dish searches and Gemma skipped. Broader acceptance gates
remain open; this slice is uncommitted.

Member-approved Chat correction revision (2026-10-15) passed review and the
562-test/debug/release gate: bounded
multi-word corrections and contextual abbreviations, complete-request validation,
ambiguity clarification and one shared system-service deadline. It supersedes
the one-word policy below. The existing reply-ID mismatch remains a separate gap.
Installed in place; named two-/three-error phone checks pass with fresh MiniLM
reads, accepted audits and visible matching answers. Provider correction and
broader regression/accessibility gates remain open. Scope and
evidence: `assistant-routing-recognition.md` and M4 checkpoints.

Current development state and preserved physical acceptance are summarized in
[the October 11 checkpoint](checkpoint-2026-10-11.md). This does not close the
milestone acceptance gates below.

Current assistant recognition work and its researched development route are in
[shared routing recognition](assistant-routing-recognition.md), with execution
evidence in [M4 checkpoints](m4-assistant-checkpoints.md). The installed checkpoint
passed 562 tests, debug/release APK builds and independent review. Local MiniLM
reads and single-edit correction have named phone successes. Production Android
spelling is implemented, but usable provider correction remains unaccepted.
Gboard is selected under the member-authorized comparison; recent callbacks
reported dictionary words. Delivery monitoring caught an audited Gemma reply
skipped by Chat's current-request gate. Why identity changed remains unproven;
do not bypass stale-result checks. The member explicitly deferred this delivery
investigation and authorized a full checkpoint review followed by commit/push.
Named multi-word phone acceptance is recorded above; remaining provider,
regression and accessibility gates are not marked passed. That reviewed
checkpoint was committed and pushed as `6cfb4a0`; its authorization does not cover
committing the new saved-dish slice above.

## Goal

Goal UI continuation is tracked in the
[view-by-view goal roadmap](design/goal-view-roadmap.md): eleven PDF reference
views, implementation gaps, ordered slices and acceptance checks. Its planning
status does not mark milestone, calculation or physical-device gates passed.

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
- [x] Bind factual output as typed atomic facts so valid dates, weights, meals,
  totals, goals, periods, and counts cannot be reassociated incorrectly.
- [x] Keep exact record evidence available behind collapsed chat cards and persist
  conversation updates outside the Compose UI thread.
- [ ] Physically verify host-calculated nutrition progress, exact weight precision,
  reply language, expandable evidence, and send responsiveness on the target phone.
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
  unsupported values. Independently valid foods may form a visibly partial
  draft when other suggestions fail validation; no valid foods means manual
  fallback. Members can add or edit secondary foods during review.
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

## Milestone 5.6 — Reliable meal photos and private correction learning

**Outcome:** member-confirmed edits make repeat meals easier while varied new
meals remain useful, safe, offline, and practical several times per day. See
`docs/vision-correction-learning-spec.md` for the bounded feedback and
measurement contract.

- [ ] Establish a consented, varied photo benchmark that separates malformed
  output from visually wrong but valid drafts, with novel and repeat meals
  reported separately.
- [ ] Derive stable-item correction differences only from final reviewed meals.
  The member chose automatic local learning from every successfully saved,
  reviewed meal, with no toggle or per-meal prompt; "Remember this dish" stays
  separate. No-op and manual reviews create no correction signal.
- [ ] Store bounded, inspectable, deletable correction signals atomically with
  the confirmed meal; provide clear-all without deleting meals, unlink on meal
  deletion, and preserve existing vault and archive compatibility.
- [ ] Use positive and contextual negative feedback only to rank visible,
  reviewable suggestions; never silently change a food or nutrition estimate.
- [ ] Measure repeated-use latency, battery, memory, thermal behavior, and
  correction effort on the target phone before daily-use acceptance.

**Acceptance:** quality improves on held-out repeat meals without increasing
false food suggestions, and novel meals meet an agreed usefulness threshold.
Manual entry remains usable but is not counted as successful recognition.

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

## Milestone 7 — Optional external assistant integrations

**Outcome:** a member may deliberately connect Metria to ChatGPT or another
compatible assistant without making cloud sharing a requirement for the core
offline product.

**Prerequisites:** M6 release readiness is complete, and optional account,
subscription, and integration-service identity have separate approved product
and threat-model decisions. This milestone must not change the initial
release's no-account, local-only default.

- [ ] Choose and document the integration transport: either a minimized,
  revocable derived-data cache or a live phone relay. State availability,
  retention, and disclosure trade-offs; do not describe server-readable data
  as end-to-end encrypted.
- [ ] Add an explicit, default-off `Privacy and integrations` flow that names
  the external processor and lets the member select data categories and a
  bounded date range before anything leaves the device.
- [ ] Define a Metria-owned authenticated MCP server with narrow typed tools
  such as `get_goals`, `get_weight_progress`, `get_nutrition_summary`, and
  bounded `list_meals`; never expose arbitrary vault queries.
- [ ] Exclude photos, chat history, biometric or vault keys, diagnostic logs,
  raw model responses, and free-form notes unless a later threat-model review
  approves a separate one-time disclosure flow.
- [ ] Make the first integration read-only. Any later write tool must create a
  visible proposal for Metria to validate and confirm on the phone; it must not
  write directly to the vault.
- [ ] Add OAuth-based authorization, least-privilege scopes, short-lived access,
  token rotation, replay protection, and a member-visible access log.
- [ ] Let the member revoke an assistant, expire every active session, and
  delete all integration-side data without deleting the local vault.
- [ ] Test expired and revoked authorization, scope violations, bounded-result
  enforcement, repeated requests, phone offline behavior, deletion, and clear
  Spanish/English consent and error states.

**Acceptance:** with integration disabled, Metria behaves exactly as the
offline product. After explicit authorization, ChatGPT can retrieve only the
selected bounded summaries; the member can inspect the access, revoke it, and
delete the shared copy. No external assistant can mutate the vault directly.

## Delivery order

### Hackathon delivery proposal

The member-approved companion UI mockup is now moving into staged Android
implementation. See [UI implementation checkpoints](design/implementation-checkpoints.md)
for the first meal-entry/available-nutrients slice and its still-pending physical
gates. The member subsequently approved the maintenance-only method and encrypted
goal history in [goal-calculation-contract-proposal.md](design/goal-calculation-contract-proposal.md).
Its deterministic estimate needs explicit selection/confirmation; weight-change
adjustments and weight targets stay member-entered. On 2026-10-08 the member
approved [optional training-based macro suggestions](design/phase-nutrition-contract-proposal.md)
against a reviewed calorie budget, with explicit review and per-field source metadata.
The separately approved [phase calorie contract](design/phase-energy-contract-proposal.md)
adds reviewed 10% Lose / 5% strength-Gain starting proposals under a bounded local
offer policy. Implementation and acceptance are tracked separately; forecasts and
live cloud work are excluded from these slices. Current
implementation and open physical/recovery gates are in the design checkpoint;
the optional first-run goal introduction is implemented, with its creation-only
phone routing check and release readiness still open.

The member intends to present Metria at the Nebius x NVIDIA Global AI Hackathon.
See [hackathon-assistant-plan.md](hackathon-assistant-plan.md) for the proposed
local assistant slice, verified submission requirements, and deferred WhatsApp
feasibility. The member selected designing optional real-data Nebius assistance
with explicit consent; [optional-cloud-assistant-spec.md](optional-cloud-assistant-spec.md)
defines its proposed bounded request and privacy contract. Actual cloud sharing
is not enabled, M7 external-integration prerequisites remain, and no release
acceptance gates are closed by this design.

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
9. M7 optional external assistant integrations, only after its cloud identity
   and privacy prerequisites are approved.
