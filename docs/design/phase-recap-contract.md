# Recorded phase periods and recaps

Approved continuation: 2026-10-10, member said “ok lets go” after the proposed
phase history/recap slice. This specifies the bounded first S4 implementation;
build evidence and phone acceptance are separate.

## Period and persistence

Add optional `endedAt` to a saved phase. Current phases must have no end.
On explicit replacement only, archive the old current phase with an end equal
to the new phase's frozen confirmation/start instant. This is the reviewed
transition instant, not the time setup opened or an inferred weight milestone.
It becomes durable only with the successful atomic replacement. Failure rolls
back end, history, references and current phase together; cancellation writes
nothing. Edits preserve current ID/start and never end a phase. Reject a new
boundary before the previous current phase's last edit; do not silently repair
device-clock reversals. No automatic closure or member backdating is added.

Goal representation 4 includes explicit nullable `endedAt` on every phase;
representations 1–3 remain readable with unknown historical ends. Never infer
legacy ends from adjacent phases or `updatedAt`. Validate timestamp types,
start <= last edit <= known end, current end absence, known intervals not
overlapping other retained phases, IDs and existing reference provenance.
Do not reject legacy unknown-end history solely for ordering that older versions
did not enforce. Preserve the 12-phase cap, encrypted archive compatibility,
strict unknown/future-field rejection and atomic restore rollback. Data payload
and cryptographic envelope versions stay unchanged. Metadata remains excluded
from assistant/cloud payloads and diagnostics.

Deleting a phase removes its metadata only; it neither reallocates records nor
changes any neighboring boundary. Deleted weight/meal records disappear from
subsequent recaps. Derived recaps are never persisted.

## Local recap calculation

Use a pure calculator outside composables. Known historical intervals are
`[confirmedAt, endedAt)`; active preview is `[confirmedAt, asOf)` with a captured
read-only instant. Unknown historical ends return Period unavailable and no
period totals. Recompute from retained records, with the current device timezone
explicitly supplied for calendar-day grouping and disclosed as local dates.

Weight endpoints and chart use actual in-period readings sorted by instant and
ID; starting/target references are not measurements. No readings means No weight
readings; one means One reading, with no signed change. Two or more show the
first and last actual readings and signed kg/lb change, with neutral language.
No projected curve, target attainment, rate, or health judgment.
Weight records currently lack a time-precision flag and date editing stores
local midnight. Treat local-midnight weights conservatively like date-only
records for incomplete boundary days; exclude and disclose them in the
time-unavailable count. Do not infer that a midnight timestamp was a measured
intraday time. This may exclude a genuinely midnight reading; no precision
metadata migration is part of this slice.

Meals with known times use the exact interval. Date-only legacy meals qualify
only when their whole local calendar day is contained in the interval; boundary
day meals are excluded and counted as time unavailable. Never assume their
stored midnight is a known time. Logging days count the union of qualifying
weight and meal dates once, independent of nutrition availability.

Show meals logged, days with records, and known recorded nutrition sums with
per-metric contributing-meal counts. Only full-coverage, present numeric fields
contribute; partial meals and missing fields are excluded visibly. A valid zero
is a value; no values means unavailable, not zero. Label figures Recorded
nutrition, not total intake or daily average. Explain retained-record and
coverage limits. No adherence score, perfect day, logging streak or top-food
ranking in this first slice; those require a later counting/display contract.

## View and states

Selected phase details retain chosen context and saved references, then show a
compact recorded recap. Known previous rows show start/end; unknown rows say
Period unavailable. Active details say In progress and show a preview without
ending anything. One dominant existing edit/back action; no new write action,
mandatory pre-setup recap, loader, animation, or nested sheet is required.
The detail header combines intent with the known period or current status. Compact
weight context omits an unset range. Daily values appear once with a concise
source summary; per-field sources and calculation inputs stay in the expanded
method disclosure. The recap shows actual weights and meal/day counts by default;
an empty period, unavailable period, or all-missing nutrition uses one concise
state. Positive coverage cautions and per-metric missing/partial counts are
available through an accessible 48dp details disclosure.
Empty, single-reading, partial nutrition and ambiguous-time states are explicit.
Refresh against the unlocked current snapshot on navigation/recreation; retain
only selected ID/page, never derived private records in saved instance state.
Use existing semantic theme/components and a textual chart summary for TalkBack,
48dp controls, scrollable content and enlarged text. Physical before/after,
rotation, deletion, save/reopen and archive restoration remain acceptance gates.

## Required automated evidence

Boundary exclusivity, active preview, unknown legacy, single/empty weights,
equal-time ordering, unit conversion, date-only boundary exclusion, timezone/DST,
unique logging days, partial/missing/zero per-metric nutrition, deleted-record
recomputation. Persistence: replacement/edit/cancel semantics, clock reversal,
known overlap rejection, legacy versions 1–3, strict v4 malformed/future/current-end
rejection, encrypted archive round trip, history cap/deletion and write rollback.
