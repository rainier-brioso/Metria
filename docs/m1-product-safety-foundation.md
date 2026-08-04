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

| Data | Default location | Who can access it |
| --- | --- | --- |
| Weight entries, notes, meals, and trends | Encrypted vault on the member's device | The member after vault unlock |
| Chosen meal photo or label screenshot | Temporary private cache during local processing only | The member while processing; deleted after completion, failure, or cancellation |
| Optional confirmed reference thumbnail | Reduced, metadata-free image encrypted inside the member's vault | The member after vault unlock; only when they leave the default-on option enabled while confirming |
| Downloaded optional model | Private app storage | The operating-system app sandbox; not a member record |
| Diagnostics | In-memory, redacted technical values only | The member while the app is open |

Metria has no account, cloud sync, advertising SDK, analytics SDK, or default
backup in the initial release. App uninstall deletes the vault and model unless
an encrypted archive is explicitly introduced and exported later. Device
biometrics are never read, copied, stored, or transmitted by Metria; Android
only returns an unlock success or failure.

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
- Only a member tap on a confirmation screen may write a weight or meal entry.
- Use neutral language such as “record”, “trend”, “estimate”, and “review”.
  Do not praise/reproach eating, weight, or compliance.
- Do not diagnose conditions, prescribe diets, give medication guidance, or
  recommend dangerous restriction/purging/over-exercise behaviour.
- For medical, eating-disorder, pregnancy, or urgent-health questions, show a
  brief boundary: “Metria cannot provide medical advice. Consider a qualified
  health professional or local emergency services for urgent symptoms.”
- A failed, uncertain, malformed, or unavailable assistant result must retain
  the member's manual-entry path without inventing values.
- Voice/App Actions may prefill a validated draft after vault unlock; no value
  may be written from a voice handoff without the same confirmation tap.

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
- Initial food-data source: none. Manual entry and local nutrition-label OCR
  come first. A future public database must sit behind an adapter, show its
  source, and require explicit member approval before any network lookup.

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
