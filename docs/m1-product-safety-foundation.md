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
