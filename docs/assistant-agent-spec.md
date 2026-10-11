# Metria local assistant agent

**Status:** Gate A passed on the target Android phone; Gates B–E remain open — 2026-08-28. See [M4 checkpoints](m4-assistant-checkpoints.md).

## Purpose and boundary

The member approved a debug-only MiniLM fast path for explicit English requests
to list today's recorded meals (2026-10-11). The host read policy independently
constrains the selection; fresh bounded records and `MetriaMealRecordReply`
retain the existing validation and encrypted timeline. Accepted turns omit
native Gemma generation. Unsupported/uncertain requests and router errors keep
the native protocol below. Release behavior and Gemma preparation are unchanged.
See `minilm-chat-integration.md` for implementation and pending phone acceptance.

Metria uses LiteRT-LM's native `Conversation` and tool-call protocol to provide a
continuous, private assistant. The model receives no vault object, database handle,
filesystem access, network tool, or arbitrary Android action. It may request only a
small declared tool; the application validates and executes that request.

Personal record claims must come from the latest approved tool response. Visible chat
history provides conversational continuity but is never record evidence. The assistant
may discuss meals, nutrition, weight, goals, progress, saved dishes, general wellness,
brief greetings, and small talk. It redirects unrelated requests and never pretends to
browse the web.

## Member-selected response style

The member may choose **Balanced** (default), **Coach**, or **Records assistant** in
Local assistant settings. This is a presentation preference, not a different agent or
permission level. Balanced is warm and concise; Coach is more reflective and offers
at most one optional next step; Records assistant leads with the direct answer and
record coverage. Every style remains neutral about food, weight, and adherence and
uses the member's language. None may change tool selection, data access, factual
auditing, action confirmation, privacy, or medical-safety boundaries. Changing the
style rebuilds model context without resending a completed member turn or deleting
the encrypted visible conversation. Older vaults default to Balanced.

Knowing more of the member's recorded history is a separate capability decision.
The current `today` and rolling-seven-day tools must not imply there are no older
records. A future history-coverage map or broader query must expose only bounded,
fresh vault evidence and preserve the same factual-output audit; style selection
alone cannot make historical claims.

For an explicit meal-list, weight, or nutrition question about one day within
the existing 90-day read window, the host requires the matching exact-day read.
This includes unambiguous named calendar dates in Spanish or English and ISO
dates. An omitted year resolves to the most recent past occurrence; ambiguous
numeric dates, multiple dates, future dates, and dates outside the window do
not trigger a record read and receive a clarification instead.
It must not accept a tool call for another day or reinterpret a nutrition-total
request as a meal-list query. A day-specific nutrition result is calculated
locally from saved meals, distinguishes complete, partial, and missing estimates,
and never treats an absent estimate as zero or recorded meals as total intake.
The exact-day read exposes member-appropriate rounded estimate values to the
model and factual auditor while leaving saved meal values and deterministic
underlying arithmetic unchanged; it must not leak floating-point artifacts or
claim precision the source does not support.
The answer's claimed date must match the read range. Current nutrition goals
must not be presented as historical goals. All-time coverage and multi-day
historical nutrition aggregates require separate bounded contracts.

## Native turn protocol

```text
member message
  -> persistent LiteRT Conversation with systemInstruction + declared tools
  -> final prose OR one native tool call
  -> host allowlist, argument, permission, and range validation
  -> bounded encrypted-vault snapshot
  -> Message.tool(Content.ToolResponse(...)) to the same Conversation
  -> final member-facing answer
```

- `automaticToolCalling` is false. LiteRT never executes a vault operation itself.
- At most one call is accepted per model response, two tool rounds per member turn.
- Unsupported, malformed, repeated, or excessive calls fail closed.
- Only visible messages and structured action/evidence metadata may be persisted. Hidden
  reasoning, prompt templates, and raw tool protocol are not conversation history.
- Lock, **New conversation**, cancellation after a runtime error, and model unload destroy
  the in-memory conversation. Relaunch rebuilds it from bounded encrypted visible history.
- The visible timeline remains capped at 50 messages and expires after 24 hours. Runtime
  context is rebuilt before the 4,096-token engine limit is approached.

## Gate A tools

**Saved-meal entry extension — 2026-10-07:** Ask Metria in a clean saved-meal
editor creates transient, removable, host-held selection. Chat retains unsent
text and requires explicit Send. `read_selected_meal()` has no arguments; the
model cannot choose an arbitrary record ID. For a supported selected-meal
question the host resolves that exact current record, validates identity/source/
food quantities/nutrition coverage, and composes a deterministic saved-record
answer. Missing/deleted/mismatched records fail closed. This is a factual summary,
not broad meal analysis or a new nutrition estimate. An explicit latest-meal,
dated/history, weight/progress or goal question still follows its existing route.
The selection is one-turn only and clears on lock/background/new conversation or
deletion; IDs are excluded from model prompts and trace metadata. There is no
write, network or new persistence schema. Physical acceptance remains open.

**Native meal-draft extension — 2026-10-02:** `draft_meal()` is a bounded
proposal capability alongside the read tools. The agent infers the meal-entry
intent from the member's message, including ordinary descriptions without
"log" or "save". The tool takes no model-authored arguments: the host uses
only that current member message, bounded to 600 characters. A required record
read cannot be replaced by this draft tool, and read-result continuations may
not switch into drafting.

After intent selection, the host retires the native chat context and creates a
tool-free structured task containing only the current member description and
the proposal contract as its system instruction. This avoids chat style and
natural tool-result instructions converting the proposal to an announcement.
Both conversations retain worker leases through completion/cancellation; the
same request ID and timeout protect the task. Its reply passes the existing meal parser; additional
tool calls, invalid output, cancellation, and stale request results fail closed.
A validated proposal appears through the existing Review/Discard card and
never writes a meal or correction signal. Request IDs and the current worker
lease bound execution; restart/lock never replays the draft request. Draft
completion discards native KV state so proposal JSON and pending tools are not
reused as conversation evidence. Future conversation rebuild uses the existing
bounded encrypted visible history.

This is a partial Gate D meal-draft implementation, not native weight writes
or a completed Gate D. Existing explicit-write routing remains transitional;
natural meal descriptions can use the native draft tool. Physical bilingual
intent selection, review/cancellation/restart, and false-intent checks remain
open. Meal Save still uses the existing reviewed, atomic vault operation.

| Tool | Arguments | Result boundary |
| --- | --- | --- |
| `read_progress_overview` | `period` (`today` or `week`) | bounded meal/nutrition/goal summary and in-period weight trend |
| `read_nutrition_summary` | `period` (`today` or `week`) | totals, member goals, coverage, as-of date |
| `read_weight` | `from_days_ago`, `to_days_ago` (0–90) | bounded dated readings and summary |
| `read_meals` | `days_ago` (0–90), optional `meal_slot` | bounded meal names/dates/slots |
| `read_latest_meal` | none | one most recent recorded meal and saved calorie/protein estimates within offsets 0–90; missing, partial, empty, or ambiguous state |
| `search_saved_dishes` | short `query` | at most the local matcher candidates |

The later exact-day extension adds `read_day_nutrition(days_ago)` for one day
within 0–90 days. It returns that day's recorded-meal nutrition estimates and
coverage, with the exact date and as-of date; it does not grant arbitrary vault
access or historical goal lookup.

`week` always means the rolling last seven calendar days including today. Broad personal
questions such as “How am I doing?” and “¿Cómo va mi semana?” use
`read_progress_overview`; a generic capability list is not an acceptable answer. If the model
misses the required read once, the host permits one corrective native turn. A second
ungrounded response fails closed and is not shown.

Nutrition results describe only meals recorded in Metria. They include the number of days with
records and an explicit `recorded_meals_only_not_total_intake` interpretation boundary. The
assistant must not call these totals the member's complete intake or infer under/over-eating from
missing logs. Explicit remembered-dish questions receive the same one-correction grounding rule
for `search_saved_dishes`; generic AI-memory prose is not accepted as an answer. A short
ingredient follow-up after conversation reconstruction reuses only the previous dish name as a
query and performs a fresh vault read. For compound dishes, presentation separates the dish name
from its component list without silently deleting uncertain member data.

Record-backed prose calls these the member's records, not the assistant's
records. It answers the requested question and its coverage first, without a
generic trailing offer or question. Coach may suggest one relevant optional
next step only when it adds value and does not imply new evidence.

Explicit write requests temporarily retain the already-tested router during Gate A.
Gate D completes weight recording and meal drafting in native tool calls only after durable
idempotency and physical acceptance exist; the proposal-only meal extension above
has no vault-write permission. Delete, bulk edit, goal, privacy, archive, and
network operations are not assistant tools in the initial release.

## Action policy

- A weight action may eventually save directly only when valid and reversible, followed by
  a visible Edit/Undo card. Its durable action ID must prevent replay duplication.
- A meal action always creates an editable draft and requires member review before save.
- Delete, edits, goals, cycle context, privacy, recovery, and model management remain
  explicit UI flows with confirmation where applicable.
- An interrupted write is never replayed automatically. Metria asks the member whether to
  retry after verifying whether an action already completed.

## Guardrails

1. The real LiteRT system instruction defines scope, tone, evidence rules, and tool use.
2. Kotlin owns the tool allowlist, argument types/ranges, data bounds, and side effects.
3. Critical medical, eating-disorder, dangerous-restriction, and urgent-symptom requests
   receive a deterministic localized boundary and appropriate professional-support copy.
4. Member text, saved dish names, record fields, and tool results are untrusted content and
   cannot change policy or grant capabilities.
5. The output gate rejects empty, runaway, malformed, protocol-leaking, or unsupported
   personal claims. Safe prose longer than the mobile display budget is shortened at a sentence
   or line boundary instead of being discarded. The UI identifies Metria as an AI assistant
   running on the phone.

### Final factual-output audit

The host binds each final answer to the latest bounded tool response for that turn. Before prose
reaches the chat, Kotlin verifies that every displayed Arabic value is present in that response
and that any stated weight direction agrees with its ordered readings or explicit change. Visible
history never contributes allowed values. Without a current vault-backed tool response, prose that
claims a personal meal, weight, goal, nutrition value, or saved record is rejected.

Gate C additionally requires typed atomic binding, not value membership alone. Dates must remain
bound to their corresponding weight or meal record; counts to their collection; nutrition values
and goals to their metric and requested period; and saved-dish names to their returned candidate.
The presence of two individually valid values in one response must never permit the model to swap
their roles or associate them with the wrong record.

One local corrective retry may ask the same conversation either to use an approved read tool or to
rewrite strictly from the latest result. If the corrected answer still contains an unsupported
claim, the native conversation is discarded. Normally the member sees a neutral retry message;
the bounded nutrition follow-up exception below can show a host-composed answer. The audit stores
and logs neither the answer nor the tool payload; its evidence object is immutable and scoped to
the active continuation only.

Without current tool evidence, that corrective retry includes the bounded original member
question as quoted, untrusted content and explicitly asks for its answer rather than an
acknowledgment of the correction. General food education may use a short qualitative explanation
in the member's language without reading records. It must not infer personal intake from chat
history, introduce precise nutrition estimates, or create a meal draft. Personal-record claims
still require fresh validated local evidence and the same factual audit.

Explicit latest-meal nutrition questions in English and Spanish require
`read_latest_meal()`, not a daily nutrition summary. The host selects by recorded
meal date/time within offsets 0–90, excluding future dates. Multiple records on
the latest day with unknown times, or tied latest timestamps, produce a
clarification rather than an arbitrary choice. Only title/date and saved,
display-rounded calorie/protein estimates are returned; notes, photos, item
details, and goals are excluded. The host validates the result and composes a
short bilingual answer, which must pass the existing factual audit. Missing
values remain missing and partial coverage is explicit. It never estimates
nutrition from chat, writes a meal, or treats a meal value as a day total.

Host replies require a concrete `Spanish` or `English` language value. Shared
selection recognizes accented and unaccented member wording; the current
question takes precedence over older conversation language. Neutral follow-ups
may inherit the most recent recognized member language, with English as the
supported fallback. Do not send an instruction such as “the member's language”
as the language field of a validated host result.

Explicit recorded-meal list questions such as “¿Qué comidas tengo registradas
hoy?” and “Which meals have I logged today?” require `read_meals` for that
exact day, including yesterday and supported named dates. The existing
corrective turn rejects a nutrition-summary tool or draft action in their
place. A meal list is not answered with nutrition totals. General food advice
and hypothetical meal questions do not acquire this required-record contract;
the existing historical ambiguity and 90-day boundaries still apply.
For that explicit exact-day contract, the host now composes the member answer
directly from the fresh `read_meals` result instead of asking the model to
rewrite it. Validate source, as-of/requested dates, optional calendar binding,
collection counts, row dates/titles, result bounds, and coverage first; then
pass the full answer through the unchanged factual audit. Empty days are
explicit, omitted older records produce a visibly partial list, and separate
same-titled records remain separate. Invalid results fail closed. Retire native
KV after this answer, retain the current request ID/worker lease, and never
reuse chat history as record evidence. Other tool answers keep their existing
continuation path.

For a short calorie or protein follow-up with an inherited period, the host may instead display a
brief deterministic answer from that turn's validated `read_nutrition_summary` result after model
wording fails the audit. It must match the requested metric and period, pass the same factual audit,
state when no recorded estimate is available, and discard the unsafe native conversation. This
fallback never uses visible history as factual evidence.

For a fresh `read_progress_overview` result, the host may also recover from
rejected model wording with a short deterministic record-only summary. It
includes the selected today/rolling-seven-day period, recorded meal and weight
counts, available calorie/protein estimates, and partial/missing-coverage
qualifiers. It never treats recorded meals as total intake or invents a weight
trend. The selected tool, required period when present, source, date range,
count consistency, numeric values, and reply language must validate, and the
composed answer must pass the unchanged factual audit. Invalid/missing evidence
still fails closed. The rejected native conversation is discarded; chat history
never supplies fallback facts.

Progress-overview nutrition values and goals are quantized for display before
being passed to the model and factual auditor: whole kcal and at most one
decimal place for protein grams, using half-up rounding. The expandable
overview evidence uses the same policy. Stored nutrition and the host's
deterministic calculations remain unchanged; rounding only model prose would
break factual binding and is not permitted.

### Development-only trace

With explicit member opt-in in a debug build, Metria may retain a short,
app-private, no-backup trace of typed request stages, approved tool categories,
bounded periods, and audit outcomes. The trace is off by default, expires within
24 hours, and has a member-visible clear action. It contains no prompt, model
prose, tool arguments or results, record values, hidden reasoning, or raw
exception text. It is separate from the encrypted conversation and recovery
archive; release builds neither record nor expose it. The existing latest raw
model response remains in memory only and clears on lock/background.
This assistant trace is separate from the session-only, debug-build image
development capture described in `m1-product-safety-foundation.md`; enabling
the latter never adds raw content to the assistant trace or chat history.

## Failure and recovery

- Model timeout, cancellation, parser failure, or invalid call closes the native conversation
  and leaves records unchanged. A subsequent message rebuilds from encrypted visible history.
- Navigating away does not resubmit or cancel a completed turn. Locking removes model KV state.
- A process death during generation produces an interrupted state, never an automatic replay.
- Tool outputs include source and freshness fields and return only the minimum necessary data.

## Acceptance gates

1. **Gate A — passed 2026-08-28:** typed bilingual conversation and the five read-only native
   tools on LiteRT-LM 0.13.0; no runtime upgrade in the same change.
2. **Gate B:** multi-turn continuity, pronouns, yes/no, period changes, cancellation, lock,
   process restart, token-budget rebuild, and airplane-mode proof.
3. **Gate C:** bilingual scope, medical-risk, prompt-injection, output, AI disclosure, and
   member-initiated report tests.
4. **Gate D:** native write tools with idempotency, weight Undo/Edit, and meal review.
5. **Gate E:** at least 100 synthetic English/Spanish fixtures plus physical GPU/CPU latency,
   memory, thermal, battery, and failure evidence. Assertions verify resulting vault state,
   not merely the selected tool.

Release requires zero duplicate writes, every invalid call blocked by the host, no fabricated
personal facts in the acceptance set, no automatic replay, and a usable manual fallback.
