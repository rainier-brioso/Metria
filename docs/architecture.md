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
- `read_latest_meal` (later bounded single-meal extension)
- `search_saved_dishes`

Explicit exact-day meal-list questions use a host-composed answer after the
model selects `read_meals` and the host executes it. Validate dates, source,
counts, record bounds, and coverage, then run the same factual audit on the
rendered meal list. This avoids irrelevant model explanations passing a
value-only check. The native context is retired afterward; other read tools
retain their existing model continuation.

A later low-risk weight tool may save directly with a durable idempotency key and visible
Undo/Edit. Meal output remains a reviewed draft. Delete, bulk edit, goals, privacy, recovery,
and model management are not initial assistant tools.

The native `draft_meal()` extension requests an unsaved proposal from the
current member message. After native intent selection, the host retires the
chat context and uses a separate tool-free structured conversation with only
that message and the proposal contract. It validates the returned structure with the existing meal parser,
and presents Review/Discard without a vault write. It does not accept a
model-authored replacement description or save flag. Draft completion clears
native KV state; request IDs, cancellation, and worker leases prevent stale
results. Required record reads cannot become draft actions. Its physical
acceptance is pending; this does not complete the native-write Gate D.

A reviewed meal opened from Chat adds a host-authored acknowledgement of the
final confirmed name and foods to the active encrypted conversation in the same
vault transaction as the meal, accepted corrections, and remembered dishes.
Failure rolls back the acknowledgement as well as those records. No transcript
schema migration is required; an optional saved-meal ID on host receipts is
backward compatible with transcripts that omit it. The normal 50-message/24-hour retention and archive
exclusion apply. An expired conversation is not revived. This acknowledgement
is historical chat continuity, never evidence for a later personal factual
answer without a fresh bounded record read.
The ID is a local navigation reference, excluded from model prompts and never
interpreted as a URL or model-authored tool request. Member/evidence messages
cannot retain it. View meal resolves against the current unlocked meal list;
deleted targets have no action. The existing editor overlays Chat and returns
there on dismissal, save, or deletion.

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

## Future external assistant integrations

### App-initiated Nebius design

On 2026-10-02 the member selected designing optional cloud assistance with
explicit consent and a revised privacy contract. The proposed first scope in
`optional-cloud-assistant-spec.md` uses a default-off, app-initiated one-request
relay with a previewed question and bounded selected summaries. It is read-only,
has no remote vault tools, and excludes photos and local chat. Provider retention,
service identity, shipping disclosure, and acceptance are still open; real-data
sharing is not enabled. This is distinct from the later externally queryable
MCP integration below and does not waive that integration's prerequisites.

External-assistant access is not part of Metria's initial local-only release.
A future, default-off integration may let a member connect ChatGPT or another
compatible assistant through a Metria-owned authenticated MCP server. This is
different from a general mobile-automation MCP server: it exposes only
Metria-defined semantic tools and never grants remote control of the device UI.

The encrypted on-device vault remains the source of truth. Before connecting,
the member chooses a bounded date range and explicit data categories. Only the
minimum derived records or summaries needed by the selected tools may leave
the phone. The consent flow must explain that any tool result returned to an
external assistant is no longer exclusively on-device and may be processed by
that provider.

The initial integration is read-only. A future write-capable tool may create a
proposal, but Metria must validate it and obtain confirmation on the phone
before changing the vault. Photos, chat history, biometric and vault keys,
diagnostics, raw model responses, and free-form notes are excluded by default.
Every integration requires least-privilege authorization, bounded results,
short-lived access, an audit trail, revocation, and deletion of the shared
copy independently from the local vault.

Implementation must not begin until a threat-model decision chooses between a
minimized server-readable integration cache and a live phone relay. The design
must state the availability and privacy consequences honestly and must not
describe server-readable data as end-to-end encrypted.
