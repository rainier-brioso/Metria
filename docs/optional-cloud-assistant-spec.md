# Optional Nebius assistance: data and consent design

**Status:** member-selected design direction, 2026-10-02. Live sharing is not
enabled. Provider retention, service identity, and
physical acceptance remain open. WhatsApp remains deferred.

**Implementation checkpoint, 2026-10-03:** the disconnected Kotlin request,
preview/one-time consent, strict incoming request validator, bounded response
selection contract, and Nebius request formatter exist with synthetic tests.
The Android app has no cloud entry point, transport, credential or vault-to-cloud
connection. A separate localhost Python broker now runs with synthetic requests
only, durable SQLite accounting and local operator provisioning. Its provider
adapter is tested against fake loopback endpoints; CLI live mode is not enabled.
No external inference occurred. See `nebius-assistant-checkpoints.md`,
`nebius-account-and-secrets.md` and `broker/README.md`.

The local broker covers revocable random credentials, short-lived one-time
dispatch, bounded replay IDs and per-credential/global rate/token reservations.
Accounting persists across process restart. Remote provisioning, HTTPS,
distributed coordination, phone credential storage, billing and actual provider
token limits remain open. This is not deployed authentication or a verified
cost cap. See `nebius-broker-security-design.md`.

## Product decision and scope

The member chose optional cloud assistance for real data with explicit consent
and a revised privacy contract. The core tracker, local model, encrypted vault,
meal analysis, and correction memory continue to work offline. Cloud assistance
is default off, requires connectivity, and is never an automatic fallback for
local failure, cooldown, or timeout.

First cloud capability: answer a member-initiated question about a selected
period's recorded weight or nutrition summaries using NVIDIA Nemotron on
Nebius Token Factory. This is read-only. A later cloud meal-drafting capability
needs a separate data/action contract; it is not included in this first scope.

Member-facing promise for this optional mode:

> Your records stay encrypted on this phone. If you choose Ask online, Metria
> sends the question and the summaries shown in your preview to its assistance
> service and Nebius for processing. This request leaves your phone. Photos,
> notes, and your local conversation are not included. Online assistance is
> optional; you can keep using Metria offline.

This copy is incomplete until the actual service operator, provider retention,
regional endpoint, deletion limits, and policy links are verified and shown.
Do not display a no-retention or end-to-end-encryption claim for server-readable
requests. Even summaries remain personal health context.

## Transport and identity decision

Choose an **app-initiated, one-request relay**, not a persistent server-side
record cache or an externally queryable MCP server. The unlocked phone builds
the preview, authorizes that exact request, and sends a minimal payload to a
Metria-operated broker. The broker holds the Nebius credential and invokes the
selected NVIDIA model. It has no phone-vault query endpoint or ongoing access.

The app validates the returned answer against the same immutable summary
snapshot that was previewed and submitted. Tool expansion cannot fetch further
records: first scope uses precomputed, typed summaries without remote tool
access. This is sufficient for a useful live hosted assistant demonstration.

The broker must authenticate clients without exporting a vault identifier or
biometric material. Proposed identity: a revocable installation credential,
stored in app-private storage, and short-lived request authorization. The exact
provisioning and abuse controls are implementation prerequisites; an embedded
shared secret or publicly open paid inference endpoint is unacceptable. A
member account is not required by this design. The service nevertheless sees
network metadata and a pseudonymous credential; the privacy notice must say so.

## Allowed payload

| Field | Initial limit and disclosure |
| --- | --- |
| Question | At most 1,000 characters, written or deliberately copied into the separate online composer and previewed verbatim |
| Language | English or Spanish |
| Period | Explicit start/end dates, at most 7 calendar days including today or earlier; no future dates |
| Weight summary, if selected | Count, period dates, first/latest measurement dates and values, host-calculated change, canonical unit |
| Nutrition summary, if selected | Recorded-meal count, host-calculated estimated calories/protein, complete/partial/missing coverage; never claim total intake |
| Request metadata | Random request ID, schema version, chosen provider/model; no vault identifier |

Categories start unselected. The member can ask a general question with no
record categories, or deliberately include one or both summaries. Do not send
meal names, individual meals, goals, cycle context, photos, thumbnails, notes,
correction signals, saved dishes, local chat, raw model output, keys, or
diagnostic content. Unknown fields and unsupported bounds fail closed.

The question itself may contain sensitive information. It must never be
silently populated from local chat, diagnostics, or selected records. A visible
preview is required even when no summary is selected. Do not promise automatic
redaction can make a free-text question anonymous.

## Consent and revocation flow

Purpose: let the member knowingly send one bounded question. Primary action
on the final preview: **Send online**. Online mode is visually labelled in its
own composer and answer; provider names appear at the sharing decision.

1. Privacy/settings offers **Online assistance**, initially off. Explain the
   service, processor, network need, allowed data, and retention/deletion limits.
2. Enabling permits entry to the flow, not background access or automatic sends.
3. Open a separate online composer. Choose question, categories, and period.
4. Compute summaries locally after unlock. Show an expandable exact payload
   preview, requested dates, coverage, and processor/retention disclosure.
5. Explicit **Send online** authorizes only that immutable payload. Any edit to
   question, categories, or dates returns to preview and needs a new send.
6. Show the answer with **Online · Nebius** and the submitted period. Validate
   personal facts using the submitted snapshot, not newer records or old chat.
7. Offer local history of access metadata and **Turn off online assistance**.
   Turning off cancels active work, revokes service authorization, and prevents
   new sends. Explain that it cannot undo processing already performed.

Initial online context is single-turn: no automatic multi-turn prompt history
is uploaded. A follow-up is a new previewed request. Online responses are
transient and clear on exit, lock, or process death; do not add them to the local
encrypted conversation or recovery archive. A later persistence/continuity
decision needs explicit retention and UX design.

Keep composer/preview scrollable with keyboard and system insets, 48 dp targets,
text/icon state labels, and localized English/Spanish copy. Use the existing
Metria theme and components. Do not add cloud controls to `MemberShell.kt`.

## States and failure behavior

| State | Member-visible behavior |
| --- | --- |
| Disabled/idle | Local assistant works; explicit online entry explains or opens settings |
| Empty selected records | Preview says no recorded data; send only the question or change selection |
| Partial nutrition | Preview and response identify partial/missing coverage as estimates |
| Preview | Exact question and selected summaries visible; Back sends nothing |
| Sending/waiting | Named online processor, clear progress, Stop; no second send |
| Success | Validated read-only answer, source mode and period; no saved record |
| Error/timeout/offline | Keep current input while unlocked; explicit retry after preview, no automatic local/cloud replay |
| Cancellation | Invalidate request immediately, attempt remote abort, reject late output; already transmitted data cannot be recalled |
| Background/lock | Cancel/discard transient response and payload; do not expose contents in notifications or logs |
| Rotation | Retain current task in unlocked memory; do not issue another request |
| Process restart | Restore disabled/enabled setting and access metadata only; report interrupted request without replay |
| Revocation | Stop new requests, revoke credential/session, show service deletion outcome and provider limits |

## Threat model and controls

| Threat | Required control / remaining limit |
| --- | --- |
| Accidental local-to-cloud routing | Separate online entry and request type; network code has no vault dependency; no fallback routing |
| Excessive disclosure | Locally enforced typed payload, category/date bounds, exact preview; broker rejects unknown fields |
| Prompt injection or invented personal facts | Deterministic safety boundary, no remote tools, bounded output, factual binding against the submitted snapshot |
| Duplicate/replayed requests | Request IDs, short-lived authorization, broker replay protection; no retry without an explicit new send |
| Credential theft or abuse | No provider secret in APK; authenticated service, rate/cost limits, credential revocation |
| Content in logs or caches | Broker content stays in request memory only; disable body logging, traces and persistent queues; verify hosting defaults |
| Provider retention/secondary use | Verify actual Nebius terms and configuration before implementation acceptance; disclose limits, never assume zero retention |
| Health information in public demo | Fictional demonstration profile and permitted imagery; never film or publish the member's real records |

Broker access metadata must omit question, summaries, and answer. Proposed
phone access log: time, provider/model, category labels, period, request status,
and request ID, encrypted locally, capped at 50 events and 30 days with Clear.
It is excluded from recovery archives. Define a minimal broker metadata TTL
before deployment and document actual deletion behavior; provider deletion is
not equivalent to deleting the phone's log. Do not add analytics SDKs.

## Implementation and acceptance gates

The first code slice covers outgoing typed bounds and in-memory exact-preview
authorization only. A preview expires after five minutes; edits (including
invalid edits), clear/cancel, disable, or lock invalidate its permit. One permit
releases one payload and retires after response completion. These are local
lifecycle controls, not broker authentication, remote cancellation, replay
protection, or factual validation of model output. Broker strict incoming schema
validation and output binding remain required before a live feature is enabled.
The disconnected incoming validator now rejects unknown/duplicate fields,
permissive JSON syntax, coercible types, unsupported configured models and
inconsistent summaries. It is not yet wired to an authenticated broker server.

The initial response contract permits only a matching request ID/language,
unique supplied summary sections (`weight`, `nutrition`) and a guidance enum
(`recorded_context_only`, `balanced_meal_information`, `professional_support`).
It rejects model-authored prose, numbers, actions and additional fields. The
host now renders values from a typed reparse of the exact approved preview,
using fixed bilingual guidance. Conservative routing supports weight/nutrition
summaries and balanced-meal information; recognized medical topics override
model selection with the safety boundary. Unsupported questions receive scope
text; missing relevant supplied sections fail closed. This vocabulary is not a
comprehensive medical classifier, and broader relevance/safety acceptance is
open. No model prose reaches the renderer. Response completion checks current
consent, validates, renders and retires the attempt atomically. Invalid terminal
output also retires consent; retry needs new preview/approval. UI must clear
returned transient answers on lock/exit. Live model behavior, integrated UI and
broker authentication remain untested.

- [ ] Confirm named service operator, Nebius region/model availability, privacy
  terms, content retention, deletion controls, cost and authentication design.
- [ ] Revise the shipping onboarding/privacy notice before enabling real-data
  requests; implement settings, preview, disable/revoke, and access log.
- [ ] Establish a network adapter whose inputs are already consented typed
  payloads; verify it cannot obtain local vault/chat/image content itself.
- [ ] Validate bounds, unknown fields, safety rules and factual association for
  synthetic English/Spanish requests and adversarial outputs.
- [ ] Test disabled mode with zero member-data egress, selected categories only,
  date bounds, changed preview, denied consent, expired/revoked credentials,
  repeated send, timeout, Stop, late output, lock, process death and rotation.
- [ ] Inspect broker/hosting logging and retention configuration using synthetic
  requests; verify secrets are absent from APK/source and limits are enforced.
- [ ] Run the complete Android test/build gate and phone interaction/layout/
  accessibility gates after an approved in-place installation.
- [ ] Demonstrate a useful live Nemotron answer using fictional records and
  record model/provider and actual latency; do not infer vision accuracy from it.

M6 release gates remain open. This app-initiated hackathon slice is distinct
from M7 external assistants polling an MCP server; it does not waive M7's
separate transport, identity, and authorization requirements. No production
dependency, network request with real data, deployment, or publication is
authorized merely by selecting this design direction.
