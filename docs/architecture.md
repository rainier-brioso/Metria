# Metria architecture

## Shared routing recognition

Saved-dish typed clarification (2026-10-17): at most three offered consumption
candidates stay in transient host-owned state, bound to the completed request
and conversation generation. A closed ordinal phrase or unique full name may
select a candidate without model generation. Buttons use the same path. Recheck
the full saved-dish snapshot in the unlocked vault before and after serialized
transcript retention, then open the existing editable review. Stop, lock, real
conversation replacement/expiry/reset, disposal and unrelated input invalidate
the offer; history cannot revive it. Decline clears locally. Failed transcript
save retains a same-choice retry, preserving original numbering. The choice
question/instructions are transient UI; only evidence, member selection and
host acknowledgement follow normal history retention. No schema change, new
dependency or automatic meal save. Scope: `assistant-companion-bundle.md`.

Conversation refresh correction (2026-10-16): a vault snapshot equivalent to the
in-memory Chat after the existing message-count/text normalization must not
invalidate its live assistant request or pending continuation. An unchanged
last-loaded snapshot must also preserve newer unsaved local turns. Compare canonical
snapshots rather than raw transcript lists. Genuine changed/removed/expired
conversations, lock and clear still invalidate request identity; the stale-result
delivery gate stays authoritative. This changes synchronization, not encrypted
record format or retention limits.

Bare meal statements (approved 2026-10-15, in development) use a distinct typed
consumption probe: one saved-dish search, exact match review or bounded choice,
then normal assistant intent selection on an empty/uncertain result using the
original text (2026-10-16 correction). Suppress the saved-consumption probe for
that continuation so the same search cannot loop. Only a positive native
meal-draft intent enters the existing timed extraction path; a non-food statement
may receive an ordinary response or clarification. A failed saved lookup alone
is not evidence that the utterance describes food.
Mandatory saved-search state must clear before the one-shot fallback. Probe
permission is host-owned, names remain opaque/local and final Save is unchanged.
Both MiniLM and native Gemma tool paths need the same result contract. Scope:
`assistant-companion-bundle.md`.

Saved-dish language extension (2026-10-15, in development): bounded natural and
polite command frames may reuse the existing recall or unsaved-draft operation.
Local command corrections preserve the opaque named-dish slot and never send it
to the OS checker. The named span is masked only in MiniLM classifier input;
actual query, host binding, original Chat/Gemma wording and review retain it.
Recall/draft permission, fresh exact query binding, ambiguity
choice, current-vault prefill and final member Save remain separate. See
`assistant-companion-bundle.md`; no automatic save or inferred usual dish.

Production Chat correction revision (2026-10-15): after exact recognition misses,
a bounded local planner may combine up to three corrections/abbreviations for an
existing read, validating at most 64 complete candidates. Broader corrections
can check up to three words individually with Android's selected English spell
checker under one 400 ms deadline. The question, history, vault, photos and tool
results are never supplied. Compatible suggestions are revalidated against the
original host constraints and complete read grammar. Distinct valid operations
require clarification. The member's Chat text stays unchanged. Sessions have a
bounded worker-thread wait, close on completion/cancellation and ignore late callbacks.
Absent, disabled, unsupported, failed or timed-out services preserve fallback.
The service adapter is production code in all build variants; release MiniLM
abstention remains a separate existing feature boundary. Provider behavior is
not an offline guarantee from the Android framework. See the full decision in
`assistant-routing-recognition.md` and the revised system-service data boundary.

The approved recognition direction is recorded in
`assistant-routing-recognition.md`, including project research and tradeoffs.
The initial implementation retains exact read routes and adds a bounded offline
typo-candidate retry before MiniLM scoring. Candidates must pass the full host
policy; original Chat text and protected meaning stay intact. No dictionary
download, new dependency or tool authority is added. Broader semantic intent/slot
recognition is a later step, not a completed capability.

## Fast today overview

The member-approved today overview uses the existing `MemberProgressOverview`
snapshot and `read_progress_overview` host operation. A closed English request,
independent host binding and competitive MiniLM decision select the optional
debug shortcut. The host carries a frozen local date into the fresh lookup.
`TodayOverviewReply` verifies local source, exact date/tool/language, recorded
coverage and displayed estimates/references, then applies the existing factual
auditor. Its visible recap omits weight trends/values, percentages, advice and
judgment; freeform summary text is ignored. Broad overview does not seed a
singular metric/date follow-up scope. Weekly/bilingual fallback and release
MiniLM abstention remain; monitoring adds only a fixed technical reason. Scope
and physical gates: `minilm-today-overview.md`.

## Chat navigation persistence

Completed current-turn Chat failures use a host-authored assistant message in
the existing encrypted timeline, consumed once under the reply request gate.
Retaining only a temporary diagnostic allowed the saved evidence card to outlive
its failure explanation. No new conversation representation is introduced;
existing 50-message/24-hour retention, archive exclusion, reset and lock rules
apply. Pending, stale, cancelled and waiting-tool diagnostics cannot complete a
turn. The original report's immediate-disappearance cause remains unproven.

The companion navigation handoff belongs to the Chat/session coordinator.
It records the current accepted opening reply once, then awaits successful
off-main encrypted retention of the complete turn before consuming the existing
current-request navigation token. Save failure keeps Chat open. Debounced and
urgent snapshots share a serialized session writer with snapshot tickets;
conversation clear invalidates pending work before its serialized write. Lock,
new conversation, cancellation and newer requests prevent late navigation.
Temporary transcript state stays in the unlocked session and is released on
invalidation. Existing vault representation and conversation retention rules
are unchanged; no content enters monitoring.

## Optional phase calorie extension

Member-approved policy (2026-10-08), specified in
`design/phase-energy-contract-proposal.md`: reviewed maintenance and explicit
endpoint selection may support a bounded 10% Lose / 5% strength-Gain proposal.
Pure calculation and validation stay outside composables. An independent optional
phase-energy source contains canonical maintenance inputs, selected endpoint,
intent/training, fixed method/version and unrounded/displayed calories. It is
mutually exclusive with acceptedEstimate for current calorie provenance; the
maintenance evidence remains inspectable inside the adjustment source. Macros
use the reviewed displayed calorie budget, without extra exercise calories.
All automatic methods require separate fresh general-suitability and
pregnancy/breastfeeding-applicability answers. Applicability is never inferred
from equation coefficients and is not persisted. An applicable answer routes to
manual care-team references, clears generated values/source snapshots while
preserving explicit manual values, and cannot be undone by Back.
Strict goal representation 3 reads legacy 1/2, rejects inconsistent sources
and future/malformed versions before replacement, and preserve atomic rollback.
Payload/crypto envelope versions remain unchanged. Profile-presence, deletion,
history and manual override paths must account for this source. Goal direction
and used-input changes require explicit reference review; new weight records
never recompute. No source metadata enters assistant/cloud payloads or diagnostics.
Implementation exists; build/install and phone acceptance remain separate.

## Optional macro suggestions extension

Member-approved scope (2026-10-08): optional local macro proposals use explicit
training context, reviewed age/weight and a reviewed calorie budget. The pure
calculator and policy live outside composables; no model or network request is
involved. Whole-day activity remains the energy basis with no second exercise
addition. The separate approved phase calorie extension above adds bounded
reviewed Gain/Lose proposals. Macro sources are independent of accepted
maintenance estimates so manual calories can support a reviewed macro proposal.
Final phase confirmation atomically saves exact displayed values with reproducible
input/method/rounding metadata and per-field source flags. Manual overrides and
changed used inputs invalidate appropriate provenance. Phase history always uses
its own snapshot. Metadata remains excluded from assistant/cloud payloads.
Goal representation version 2 adds macro provenance, reads legacy 1 without
fabrication, rejects malformed/unsupported versions, and preserves rollback and
deletion semantics. Data payload/envelope versions are unchanged. Older apps cannot
open a goal representation they do not understand; downgrades remain unsupported.

## Recorded goal periods and recaps

Approved S4 contract: `design/phase-recap-contract.md`. `GoalPhase.endedAt` is
written only when an explicit new phase replaces the current one, using the new
phase's frozen confirmation instant; edits preserve the current phase and its
start. Codec representation 4 is emitted only when at least one end is known,
while representations 1–3 decode ends as unknown. Replacement, end boundary,
references, and retained history commit atomically. Never infer an end from
`updatedAt`, adjacency, or records.

Recaps are recalculated locally from retained encrypted weight and meal records.
Known historical periods use `[confirmedAt, endedAt)`; current preview uses an
explicit as-of instant, while unknown historical periods have no totals. Date-only
meals count only when their whole local day lies within the interval; boundary-day
meals are excluded and reported. Legacy weights saved at local midnight on a
boundary day are conservatively excluded and disclosed because their time is not
known. Nutrition totals use only per-metric full-coverage values, with contributing
meal counts and retained-record limits. Recaps are derived, never persisted or
sent to assistant/cloud/diagnostics. No averages, adherence, streaks, projections,
or inferred milestones are added.

## Optional local goal setup and recovery compatibility

Member-approved scope (2026-10-06): `GoalSetup` holds one current stable-ID phase
and at most 12 previous phases, each added only by explicit New phase. Optional
accepted maintenance energy includes the reproducible `maintenance-msj-pal-v1`
profile, activity range and source. Intent alone creates no numeric references.
`GoalPolicy` validates the complete snapshot and stages targets plus goal data
atomically with rollback; manual calorie changes remove derived provenance.
Weight entry writes do not mutate goal references. Composables emit confirmed
intent and never operate on encrypted files or compute business rules themselves.

The encrypted payload is version 2 when goal data is present, otherwise version 1.
The AES-GCM data envelope and key envelope remain version 1. `GoalSetupCodec`
validates goal representation version 1, explicit nulls, numeric types, sources,
IDs, bounded history and consistency with current daily targets. Legacy version 1
archives restore no goal data without guessing from weights. Unsupported future
payload/goal versions reject before replacement. Export includes goal data;
restore preview discloses phase/profile presence and replacement of current
references, profile and history. The restore rollback includes the whole goal
snapshot. Older app versions cannot open a version 2 data payload; downgrades
are unsupported. Deleting all goal data keeps daily targets as manual references.

Profile, intent, history and method inputs are not added to local assistant or
cloud serializers. Only already-supported daily reference numbers can appear in
existing summaries. No model, network dependency or cloud calculation is added.

## Privacy boundary

Member-approved manual Chat copy (2026-10-12): explicit Copy may place only a
chosen completed visible message on the OS-managed clipboard as plain text,
marked sensitive for preview handling. This user-controlled transfer does not
authorize automatic disclosure, clipboard reads, analytics or new persistence.
Metria performs no network operation and copies no hidden model/tool state.

Member-approved photo performance metadata extension (2026-10-11): the existing
50-record encrypted image-analysis audit optionally retains image preparation,
conversation setup, model-request, first nonblank streamed-text callback and
validation durations, plus Android thermal status at analysis start/end. Image
preparation occurs before analysis and is not part of the existing total duration.
Model-request duration includes vision/prefill/generation; a failed or cancelled
request records elapsed time to termination when available. Stage durations are
not an exhaustive sum of the total. Thermal status is the OS severity (0-6), not
a temperature or peak measurement. Missing/legacy measurements stay null; an
additive optional metrics object preserves legacy decoding without a payload or
crypto-envelope change. Metrics contain no prompt, food, photo or model text.
Existing unlocked-only writes, bounded retention, archive exclusion and deletion
semantics remain. Voice/text/chat and label OCR are outside this extension.

Metria is a standalone, member-owned application. Weight, food, goals, notes,
chat history, and local model data are personal data. They are not visible to a
gym, staff member, or another application by default.

The on-device encrypted vault is the source of truth. A future optional backup
must be encrypted end-to-end and must not include device private keys or unlock
material.

## Bounded local assistant agent

Member-selected debug optimization (2026-10-11): MiniLM may select only an
explicit English request for today's or yesterday's meal list, or saved calorie/
protein estimates for the latest recorded meal. The 2026-10-12 extension also
supports one requested recorded calorie/protein total for today, additionally constrained by
the host's independent read policy. The existing dispatcher reads fresh local
records and the existing validated deterministic meal reply answers the turn
without Gemma generation. Unknown/unsupported requests and router failure keep
the existing assistant path. Inputs and embeddings remain transient on-device;
no router reports, raw logs, new vault schema or writes are added. Debug-only
model provisioning and pending acceptance are tracked in
`minilm-chat-integration.md`. Release keeps Gemma inference and the initial
Gemma-readiness requirement; this does not prove reduced resident memory.

Approved natural phrasing/follow-up slice (2026-10-12): broader bounded English
paraphrases retain independent host validation. A terse calorie/protein follow-up
may use only an ephemeral scope earned by the last validated MiniLM today-
nutrition or latest-meal read in the current unlocked session. The scope carries
operation/date, never record values. Every follow-up reads fresh records and
passes the existing validators. Stop, lock, restart, new/unrelated operations and
date rollover invalidate it; missing scope follows the normal assistant path.
No new vault or conversation representation is introduced.

Approved weight read shortcuts (2026-10-12): debug MiniLM may recognize an
explicit English latest-weight value/date or recent-weigh-in request using the
existing `read_weight` permission. Latest reads cover 90-days-ago through today;
recent reads cover 30 calendar days. Host-composed replies disclose window dates
and omissions, validate fresh bounded rows and never claim all-history coverage.
They do not grant write access or seed nutrition follow-up scope. Implementation
and acceptance are tracked separately in `minilm-chat-integration.md`.

Approved broader read wording (2026-10-12) uses shared bounded normalization and
additional closed English paraphrases for existing operations. Both eligibility
and embeddings receive the normalized member wording; host read scope remains
independent. Date/number/write qualifiers are preserved and mixed requests still
decline. No tool, schema, classifier threshold or factual-audit change is made.

Member-approved routing monitoring (2026-10-12) extends the existing opt-in,
bounded content-free assistant trace with fixed route choices/rejection outcomes
and finite cosine scores. This allows authorized local inspection after lock has
cleared transient Developer diagnostics. It stores no questions, replies,
embeddings, record values or exception messages, and retains the existing
24-hour/100-event/64-KB limit, deletion, archive exclusion and release no-op.

Member-approved photo navigation (2026-10-11): an explicitly accepted meal-image
analysis receives a transient UUID at start, reused as its eventual local audit
ID. The active capture destination attaches only to that newly accepted request.
A one-shot gate consumes its complete/partial validated result before opening
review. Rejection, failure, Stop, destination disposal and newer attempts prevent
stale/replayed navigation. The UUID adds no content or persisted draft and does
not change audit retention. Selected date/type and explicit reviewed Save remain
host-controlled. Label OCR and conversational drafts retain their existing routes.

Today-origin typed and spoken meal entry uses the local tool-free meal-proposal
path directly. Typed entry submits an edited description; Today spoken entry
automatically chains its successful validated transcript into an unsaved draft
(member-approved 2026-10-10). Failed/cancelled generation retains the description
for editing and explicit retry. It does not create a chat
turn or require conversational intent selection. Speech understanding is used
only for a bounded transcript; its returned actions are never dispatched here.
The chosen meal section and entry date stay on the host until explicit review
and save. Typed/spoken review must not inherit a previous image thumbnail or
image audit. Request identity/operation and capture cancellation scope isolate
late results from subsequent entry attempts. No new vault format or cloud route
is introduced by this contextual entry flow.
Direct contextual proposals and native Chat meal proposals share the dedicated
tool-free meal-generation system contract. General conversational/coach system
instructions are not used for this structured task. Both paths retain the same
strict proposal parser and explicit member review.

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
- `read_selected_meal` (one-turn, host-held saved-meal selection)
- `search_saved_dishes`

The selected-meal UI handoff retains a transient exact saved-meal ID on the host,
never in model arguments, prompts, trace metadata or recovery archives. The
argument-free selected read is valid only for that deliberate context. At tool
execution the host re-resolves the ID from the current unlocked vault; a deleted,
missing or mismatched result rejects safely. A validated deterministic response
describes saved foods/nutrition and coverage. It does not infer new nutrients or
health judgments. Selection is consumed for one submitted turn, clears on
lock/background/new conversation/deletion, and cannot override an explicit
latest-meal, dated-history or progress request. Existing composer text is retained.

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

The offline Foundation catalog is supplemented by two member-approved SR Legacy
prepared-food references (2026-10-10), documented in food-catalog-development.md.
Loading merges distinct IDs without replacing Foundation values and identifies
the source bundle as `2026-04-30+sr-2018-v1`. No runtime lookup or key is added.
Cooked pasta and Italian-style frozen meatballs remain visible, reviewable
approximations; raw/dry or specific unsupported recipe names cannot silently
reuse them. Existing saved meals/releases are not rewritten.

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
