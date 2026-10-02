# Metria local assistant agent

**Status:** Gate A passed on the target Android phone; Gates B–E remain open — 2026-08-28. See [M4 checkpoints](m4-assistant-checkpoints.md).

## Purpose and boundary

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

| Tool | Arguments | Result boundary |
| --- | --- | --- |
| `read_progress_overview` | `period` (`today` or `week`) | bounded meal/nutrition/goal summary and in-period weight trend |
| `read_nutrition_summary` | `period` (`today` or `week`) | totals, member goals, coverage, as-of date |
| `read_weight` | `from_days_ago`, `to_days_ago` (0–90) | bounded dated readings and summary |
| `read_meals` | `days_ago` (0–90), optional `meal_slot` | bounded meal names/dates/slots |
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
Gate D moves weight recording and meal drafting into native tool calls only after durable
idempotency and physical acceptance exist. Delete, bulk edit, goal, privacy, archive, and
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

For a short calorie or protein follow-up with an inherited period, the host may instead display a
brief deterministic answer from that turn's validated `read_nutrition_summary` result after model
wording fails the audit. It must match the requested metric and period, pass the same factual audit,
state when no recorded estimate is available, and discard the unsafe native conversation. This
fallback never uses visible history as factual evidence.

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
