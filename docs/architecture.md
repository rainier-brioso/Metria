# Metria architecture

## Privacy boundary

Metria is a standalone, member-owned application. Weight, food, goals, notes,
chat history, and local model data are personal data. They are not visible to a
gym, staff member, or another application by default.

The on-device encrypted vault is the source of truth. A future optional backup
must be encrypted end-to-end and must not include device private keys or unlock
material.

## Bounded local assistant agent

The assistant uses LiteRT-LM's native persistent conversation and declared tool protocol.
It cannot write arbitrary data or access the vault directly. The app validates every tool
request, executes it against a bounded local snapshot, and returns the result to the same
conversation. See `docs/assistant-agent-spec.md`.

```text
member text
  -> persistent local model conversation
  -> native allowlisted tool request
  -> host validation + bounded local result
  -> same model conversation
  -> member-facing answer or editable action
```

Initial read-only tools:

- `read_progress_overview`
- `read_nutrition_summary`
- `read_weight`
- `read_meals`
- `search_saved_dishes`

A later low-risk weight tool may save directly with a durable idempotency key and visible
Undo/Edit. Meal output remains a reviewed draft. Delete, bulk edit, goals, privacy, recovery,
and model management are not initial assistant tools.

## Android voice handoff

Google Assistant App Actions may launch Metria's weight-entry confirmation
screen with a proposed value and date. The Android native layer receives the
explicit App Action intent extras, validates them, and passes only a pending
proposal to the React Native interface. Do not use a URL query string for
weight values and do not let Assistant write to the vault directly.

If the vault is locked, keep the validated proposal only for the active launch,
request biometric unlock, and discard it on cancellation or app termination.
The member sees and confirms the resulting entry exactly as with manual input.

## Nutrition calculations

The model extracts foods, quantities, preparation methods, and uncertainty from
natural language. A curated nutrition data source and deterministic arithmetic
produce the calorie and macro estimate. The app keeps the source, assumptions,
and confidence with each saved meal.

Vague portions produce a range, not false precision. For example, "half a plate
of rice" becomes an editable range and a visible assumption before save.

## Vision, later

Photo and screenshot understanding are optional enhancements. The app must not
auto-save a meal from an image. Vision may propose foods or read a nutrition
label, then the member confirms portions and ingredients. OCR is preferred for
label text; a vision model is used for image interpretation.

## Relationship to Smart Gym

Metria has no dependency on Smart Gym for its initial releases. A later module
integration may share UI and vault primitives, but Metria's personal records
must remain separately scoped and never become gym-visible by accident.
