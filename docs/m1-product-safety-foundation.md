# Metria M1 product and safety foundation

**Status:** approved initial Android decisions — 2026-08-02

## Purpose

Metria is a private wellness tracker. It helps a member review and confirm
their own weight and food information; it does not diagnose, prescribe,
criticise, or automatically record health data.

These decisions constrain M2 onward. A later feature may be added only when it
preserves the boundaries in this document or this document is deliberately
revised first.

## Data boundary

**Fictional local test capture - 2026-10-28:** member explicitly requests complete
model responses and errors during fictional app tests. The local tunnel's
explicit fictional-capture flag permits private bounded raw provider output,
HTTP error bodies and redacted original exception traces for these tests only.
No production/hosted logging or retention of real member content is authorized.
Prior chat context must also be fictional; response bodies may echo that input.

**Cloud provider decision - 2026-10-27:** member explicitly authorizes optional
cloud assistant integration, selected in onboarding or Settings, alongside local
Gemma. The selected photo/description or submitted text and bounded necessary
conversation context may leave the device after disclosure and explicit action.
No silent fallback, bulk export, automatic Save or server content logging. Cloud
voice uses Android on-device speech recognition then validated text; unsupported
devices offer typing rather than network ASR. Encrypted storage, deterministic
nutrition, grounding and reviewed-save rules remain. Scope and acceptance:
`cloud-assistant-integration.md`. Earlier blanket photo/assistant exclusions are
superseded only for this explicit route.

Chat drink-quantity extension (2026-10-24, approved implementation in progress):
typed and transcribed mL/measured-serving expressions may update an unsaved draft
only using the packaged food's existing validated conversion. Unsupported or
ambiguous quantities preserve the previous draft; no model density or generic
cup conversion. Initial explicit quantities override remembered portions. Existing
Review and explicit Save remain required; no automatic recording is authorized.

**Drink quantity decision - 2026-10-22:** member approved mL and measured serving
entry with correct nutrition conversions. Preserve the chosen amount/unit and
validated source conversion in the existing encrypted meal item and saved-dish
payload, including recovery. Canonical gram mass remains the nutrition input;
rounding is deterministic and bounded rather than clamped. Only the selected
food's measured serving mass and explicitly known volume can establish a factor.
Never assume all liquids have water's density. Missing evidence leaves the
conversion unavailable; changing food/name invalidates prior conversion authority.
The model cannot supply this authority. Review and Save remain member actions;
legacy gram-only records retain their meaning. Physical acceptance is separate.

Drink-default extension (2026-10-22): member requests reuse of confirmed drink
units and usual quantities. Derive an editable default only from existing saved
matching drink records with valid source-bound conversion. Explicit current
quantities/size, photos/manual and saved-dish/portion overrides take priority;
discarded drafts cannot teach a default. Show reuse briefly in Review and retain
explicit Save. Display rounding must not silently change calculated nutrition.

**Chat correction revision - 2026-10-15:** member approved combining multiple
spelling corrections and contextual abbreviations for natural Chat. Up to three
candidate words may be checked individually with the selected Android service
under one shared 400 ms deadline. Full questions, history and records remain
excluded. Complete-candidate validation, original host/date meaning, ambiguity
clarification and unchanged original text are required; no action/save authority
is added. Privacy details state the new word maximum. This supersedes the
single-word limit in the original decision below, not its provider safeguards.

**Production Android spelling decision - 2026-10-14:** the member explicitly
approved production integration of the Android Spell Checker Framework after
discussion of provider, availability and offline limitations. A local policy
first identifies one possible routing-word correction that could satisfy a
complete existing read contract; only that single token may be submitted to the
selected Android provider for English suggestions. No full question, history,
record values, photos or tool result is supplied. This is a system-service
boundary outside Metria's app process, not an enabled Metria cloud assistant.
The framework does not establish provider network behavior; do not claim the
provider is offline from its presence or successful suggestions. Privacy and
assistant details explain that spelling help follows the provider's system
privacy settings. Metria adds no network request or private-content telemetry.
Known dates/numbers/write terms are not rewritten, suggestions remain untrusted,
and only an unambiguous full validated read may use a correction. The original
Chat text remains encrypted under its normal policy; tokens and suggestions are
not separately stored. Missing/disabled service and timeout preserve normal
handling. Physical behavior, offline-provider validation and release acceptance
remain separate gates. This deliberately extends the prior app-only processing
description for this narrow OS text-service use.

**Local photo performance metadata decision - 2026-10-11:** member approved
persisting first-output and stage timing plus start/end Android thermal severity
in the existing bounded encrypted image-analysis audit. These technical fields
exclude content, remain on-device, require the unlocked vault for writes, and
are excluded from recovery archives. Legacy or unavailable values stay unknown.
First-output timing is the first nonblank streamed text callback, not a guaranteed
single-token boundary. No cloud telemetry or new raw diagnostic capture is added.

**Optional phase calorie decision — 2026-10-08:** Member approved the concrete
`design/phase-energy-contract-proposal.md`, explicitly preferring 10% below
reviewed maintenance for Lose and confirming 5% above it for strength-training
Gain. This deliberately extends the earlier maintenance-only energy boundary.
The fixed defaults and bounded offer rules are product starting choices, not
clinical prescriptions or forecasts. Canonical reviewed maintenance inputs and
explicit endpoint selection are required, with fresh transient suitability and
used training context. Unsupported cases retain manual/skip paths; no clamping,
extra exercise energy, inferred target weight or deadline. Preview and Use stage
the draft; only final phase confirmation saves. Independent encrypted phase-energy
source retains reproducible maintenance evidence, adjustment and rounding, is
inspectable/deletable and excluded from assistant/cloud/diagnostics. Changed
direction or calculation inputs invalidate derived references for review; explicit
manual overrides survive. Pregnancy/breastfeeding applicability is asked
separately from general clinical/eating-disorder suitability and never inferred
from an equation coefficient. An applicable answer routes to care-team-entered
references and clears generated values/source snapshots, preserving explicit
manual values; Back cannot restore cleared sources. Answers are transient.
Implementation and physical acceptance are separate gates.

**Optional macro suggestion decision — 2026-10-08:** The member approved
`design/phase-nutrition-contract-proposal.md` with “go ahead!”. This deliberately
extends the earlier maintenance-only decision: eligible adults may request local,
deterministic, editable macro proposals against a reviewed calorie budget and
reviewed weight. Balanced mode proposes 20% protein / 30% fat / 50% carbohydrate,
subject to the stated protein reference check. Regular strength/other exercise
uses respectively 1.6/1.4 g protein per reviewed kg, 30% fat and carbohydrate
remainder, only within the documented offer envelope. These are starting options,
not measured requirements or clinical prescriptions. Unsupported/missing contexts
retain manual entry. Suitability remains transient; no unused training frequency,
diet type or lean mass is inferred or collected. Training never adds exercise
calories to whole-day activity. The separate phase calorie decision above permits
bounded Gain/Lose starting proposals; forecasts and deadlines remain excluded.
Only proposal acceptance followed by final phase confirmation
saves values. Per-field overrides and used-input changes cannot retain false
provenance. Macro input/method metadata stays encrypted, independently versioned,
inspectable/deletable, included only in validated recovery archives, and excluded
from assistant/cloud payloads, analytics, diagnostics and logs. New weight records
never recalculate references. Build/install evidence does not close physical gates.

**Optional goal setup decision — 2026-10-06:** The member approved an optional
maintenance-energy starting estimate and encrypted goal history under
`design/goal-calculation-contract-proposal.md`. This deliberately revises the
manual-only daily-reference contract: an energy reference may come from the
specified deterministic local method only after explicit member selection and
confirmation. The activity range is an approximation, not a validated individual
requirement or confidence interval. No assistant sets goals. No automatic deficit,
surplus, target weight, deadline or ideal weight band is selected. The later
2026-10-08 decision above separately permits reviewed macro proposals.
Unsupported or declined inputs retain manual tracking. General suitability and
pregnancy/breastfeeding applicability are separate transient checks; the latter is
never inferred from equation coefficients. If applicable, care-team entry clears
generated values and source snapshots while preserving explicit manual values, and
Back cannot restore cleared sources. Eligibility answers are
transient; accepted calculation inputs, current intent and at most 12 previous
phases stay encrypted and are excluded from assistant prompts, current cloud
requests, analytics, diagnostics and traces. Daily references can still be used
in existing bounded summaries. Editing a calorie reference removes stale derived
provenance. New recorded weights do not recalculate it. Members can inspect and
delete phases/profile without deleting meals or weights; deletion explicitly
retains daily references as member-entered. Recovery archives include this data
only through the validated versioned representation and explicit replacement.

**Recorded phase period and recap decision — 2026-10-10:** The member approved
`design/phase-recap-contract.md` (“ok lets go”). Phase ends are written only as
part of an explicit successful replacement; edits do not end phases. Legacy ends
remain unknown and are never inferred. Recaps use retained local records, exact
known periods and explicit coverage boundaries; boundary-day ambiguities are
excluded and disclosed. Summaries are read-only and derived, not persisted. They
describe recorded data only and do not score adherence or infer daily intake.
Rollback, deletion, archive validation, and physical acceptance remain gates.

**Optional-cloud design direction — 2026-10-02:** The member selected designing
default-off Nebius assistance for real data with explicit consent and revised
privacy disclosure. `optional-cloud-assistant-spec.md` defines a previewed,
one-request question plus selected bounded summaries; photos, notes, local chat,
cycle context, keys, and correction memory are excluded. Cloud answers are
read-only and host-validated. This is a design direction, not an enabled data
transfer or a completed privacy gate. Until processor retention, service
identity, shipping consent/copy, and acceptance are verified, the implemented
local boundary and disclosure below remain in force. Public demos use fictional
records. An enabled future mode must clearly qualify the local-only promise at
onboarding and every explicit sharing decision.

| Data | Default location | Who can access it |
| --- | --- | --- |
| Weight entries, notes, meals, and trends | Encrypted vault on the member's device | The member after vault unlock |
| Corrections made while reviewing a locally analysed meal | Bounded, structured correction notes linked to the saved meal in the encrypted vault | The member after vault unlock; any later use must remain local and reviewable |
| Chosen meal photo or label screenshot | Temporary private cache during local processing only | The member while processing; deleted after completion, failure, or cancellation |
| Optional confirmed reference thumbnail | Reduced, metadata-free image encrypted inside the member's vault | The member after vault unlock; only when they leave the default-on option enabled while confirming |
| Downloaded optional model | Private app storage | The operating-system app sandbox; not a member record |
| Diagnostics | Redacted technical values in memory; an explicitly enabled debug build may keep a short metadata-only trace in app-private no-backup storage | The member, and a developer using authorized local ADB access to that debug build |
| Opt-in image-development capture | One short-lived, app-private no-backup copy of a reduced EXIF-free test photo, bounded model response, and technical outcome; debug build only | The member and a developer through member-authorized local ADB access during the active test session |

Metria has no account, cloud sync, advertising SDK, analytics SDK, or default
backup in the initial release. App uninstall deletes the vault and model unless
an encrypted archive is explicitly introduced and exported later. Device
biometrics are never read, copied, stored, or transmitted by Metria; Android
only returns an unlock success or failure.

**Meal-learning decision — 2026-09-28:** Saving a reviewed local meal may
automatically retain bounded, structured correction signals in the encrypted
vault. This is part of Metria's core private assistance, not a separate toggle
or per-meal prompt. It records only the final changes needed for reviewable
local suggestions, not the source photo, raw model response, or intermediate
edit history. Correction-note capture is implemented; using these notes to rank
future photo suggestions is not yet implemented. The member can inspect and clear learning without removing meals;
deleting a linked meal removes its learning. Older food-match hints without
meal provenance remain encrypted and clearable but are not used to rank
suggestions. "Remember this dish" remains a
separate choice for a reusable named dish. No correction may bypass member
review, deterministic nutrition, or a final Save.

**Debug diagnostic exception — 2026-09-22:** The assistant trace is off by
default, opt-in from Developer diagnostics, and bounded to a rolling 24-hour
window. Expired entries are pruned on startup, new writes, and hourly while
the app is running. It may record only typed technical
events such as request stage, approved tool category, bounded period, and audit
outcome. It must not store prompts, model prose, tool arguments or results, record
values, photos, chat history, keys, or raw exceptions. It is never included in
the encrypted vault or a recovery archive and can be cleared by the member.

**Routing-monitoring extension — 2026-10-12:** the member requested recording
MiniLM/Gemma routing in that same content-free trace. Fixed route selections,
rejection categories and bounded finite classifier cosine scores may be retained
under its existing opt-in, 100-event, 24-hour/64-KB limits. Scores are technical
similarities, not confidence probabilities or nutrition values. This extension
does not permit question text, replies, embeddings, record data or exception
messages. Existing local-only access, archive exclusion, clear/delete and release
no-op rules remain.

**Image-development capture exception — 2026-09-27, opt-in revised
2026-09-28:** For member-approved test images on a debug build only, a
separate, explicit Developer diagnostics control may temporarily retain the
latest reduced, metadata-free test image and bounded raw vision response
alongside fixed parser-outcome and timing fields. This is intentionally *not*
the metadata-only assistant trace. The control starts off for an existing or
new installation until the member enables it; that choice then persists in
app-private no-backup storage until explicit Disable. Only the choice persists:
the one-capture payload expires within 30 minutes while the app runs and is
deleted on disable, clear, lock/background, or process startup. Clear retains
the enabled choice. A hard process kill can leave the payload until the next
app launch or authorized ADB read, which rejects and removes an expired
capture. Release builds must ignore and remove both the choice and any
payload, and must never write a capture. The app must not transmit either or
place a capture in the vault, recovery archive, logs, or repository.
Authorized local ADB inspection on the member's development machine remains
a deliberate, user-controlled exception to ordinary on-device-only operation;
persisted enablement alone does not authorize reading a particular test run.

## Plain-language member disclosure

Show before the first vault creation and before any optional model download:

> Your entries stay encrypted on this phone. Metria does not create an account
> or send your weight, meal details, photos, or screenshots to Metria servers.
> If you choose local photo analysis, the app makes a temporary private copy
> and deletes it after processing. When you confirm a meal, you can keep a
> small private reference thumbnail in your encrypted vault; the original is
> still deleted and no image is sent anywhere. Deleting the app removes your local data
> unless you have first made a future encrypted export.

The model download itself is an explicit member action and may contact the
selected distribution host. It must never include the member's entries, photo,
prompt, or vault identifier.

## Assistant safety rules

- Assistant and OCR output are drafts, never facts or commands.
- A meal always requires member review and an explicit save. A valid assistant weight action
  may save directly only when it produces a visible reversible card with Edit and Undo and
  uses durable replay protection. Other writes require their focused confirmation flow.
- Use neutral language such as “record”, “trend”, “estimate”, and “review”.
  Do not praise/reproach eating, weight, or compliance.
- Do not diagnose conditions, prescribe diets, give medication guidance, or
  recommend dangerous restriction/purging/over-exercise behaviour.
- For medical, eating-disorder, pregnancy, or urgent-health questions, show a
  brief boundary: “Metria cannot provide medical advice. Consider a qualified
  health professional or local emergency services for urgent symptoms.”
- A failed, uncertain, malformed, or unavailable assistant result must retain
  the member's manual-entry path without inventing values.
- Voice/App Actions may prefill a validated draft after vault unlock. Direct voice weight
  recording is permitted only under the same bounded, reversible, idempotent policy as chat.

## Nutrition confidence labels

Every nutrition value must display one source label alongside its values:

| Label | Meaning | Required member action |
| --- | --- | --- |
| **Verified label** | Read from packaging by the member or local OCR and reviewed against the packaging | Confirm or correct fields |
| **Member-confirmed match** | A member accepted a future food-database match | Confirm serving and quantities |
| **AI estimate** | A local model inferred visible foods or portions | Review assumptions, answer questions, and confirm or edit |
| **Manual entry** | The member supplied the values | Review before save |

Calories and macros derived from uncertain portions must be presented as
estimates or ranges, never as exact intake.

## Initial locale, units, and food-data policy

- Initial languages: Spanish (Spain) and English.
- Weight: kilograms by default; pounds as a member-selectable display/input
  unit. Persist a canonical kilogram value in the vault to avoid rounding drift.
- Dates and numerals follow the device locale; ISO-8601 timestamps are used in
  encrypted storage and exports.
- Initial food-data source: USDA FoodData Central Foundation Foods, packaged as
  a versioned offline catalog. During development, retain the complete April
  2026 source releases locally to evaluate fields and future needs. The member
  application must never make a runtime food lookup, and the raw source must
  never be stored in the encrypted vault. A selected catalog match must show
  its source and still require member confirmation.

## Accessibility baseline

- Support Android system font scaling without clipping or hidden actions.
- Give every interactive control a meaningful screen-reader label and state.
- Maintain WCAG-oriented text contrast; do not use colour alone for confidence,
  errors, or trends.
- Respect reduced-motion settings; do not make timing or gestures the sole path
  to complete an action.
- Use accessible numeric inputs, clear validation messages, and keyboard-safe
  layouts for weight and meal confirmation.

## M1 completion evidence

- Product copy is reviewed against the disclosure and safety rules above.
- The M2 vault design has no account, network, or biometric-data storage path.
- M3/M4/M5 designs apply the confidence labels and confirmation rule.
- Accessibility checks are included in each physical-device checkpoint.
