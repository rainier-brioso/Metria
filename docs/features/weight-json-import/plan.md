# Weight JSON import plan

**Status:** proposal only; implementation has not started. The member approved a
JSON-array import under PrivacySettings and a blank, copyable agent prompt.
Both date-only and exact-timestamp inputs are approved. This
document does not authorize implementation before the contract is committed and
a named test profile is approved.

## Contract

Input is a UTF-8 JSON array containing 1–1000 records, at most 1 MiB. These are
proposed safety bounds, not measured performance limits. A record has a finite
numeric `weight`, `unit` (`kg` or `lb`), exactly one of `date` (strict
`YYYY-MM-DD`) or `recordedAt` (ISO timestamp with explicit offset), and optional
`note` (string or null, at most 500 characters). No IDs, account/user metadata,
or extra fields. Do not coerce types, infer missing values, accept markdown, or
partially import: any invalid record rejects the whole file. Errors identify
record and field without echoing raw values. Use injected `now: Instant` and
explicit `zone: ZoneId`. Accept `date <= now.atZone(zone).toLocalDate()`;
accept exact `recordedAt` only when its parsed instant is `<= now`, regardless
of its textual offset. Equality is allowed; a later timestamp today is rejected.

Synthetic examples (illustrative only):

```json
[{"weight":72.4,"unit":"kg","date":"2026-10-12","note":null}]
```

```json
[{"weight":160,"unit":"lb","recordedAt":"2026-10-12T08:30:00+02:00","note":"sample"}]
```

Canonical storage is `WeightEntry(id: UUID, kilograms: Double, recordedAt:
Instant, note: nullable)`. Accept only finite 15–500 kg after unit conversion.
Retain native conversion precision; do not add rounding. Storage has no account
or user ID. IDs are generated internally. Duplicate handling must never
overwrite existing entries: proposed identity is canonical temporal identity,
kilograms, and normalized note; retain same-time entries with different values
and disclose duplicate/retained counts.

Out of scope: CSV, in-app AI extraction, archive replacement and automatic goal
updates.

**Design gate before persistence work:** date-only provenance and zone semantics
are absent from `WeightEntry`. `PhaseRecap.kt` also treats stored local midnight
as unknown-time heuristically, which may fail after zone changes. Demonstrate
safe behavior across zone changes, re-imports, phase recaps and legacy decoding.
If preservation requires additive entry/codec metadata or recap/test changes,
list and approve those expanded paths before editing them; this plan does not
authorize widening implementation scope by itself. Never silently represent
unknown time as known midnight. Parser output must preserve sealed `DateOnly`
versus `ExactInstant` and must not create IDs, entries, or conversion-time
timestamps before this gate is resolved.

Import stays encrypted and local. No account, network use (manifest permission
is not authorization), broad storage, dependency, sharing, file/URI logging or
copy. Request local-only picker where supported; cloud providers may appear and
access is not guaranteed offline.
The blank agent prompt contains only the canonical schema and asks an agent to
generate a JSON file from data the member chooses, for local review and import.
It contains no personal data, authorizes no vault writes, and must not trigger
external sharing. Read `../../m1-product-safety-foundation.md`,
`../../architecture.md`, and [Testing](../../TESTING.md).

## Planned slices and gates

1. Parse into pure typed records; no vault access.
2. After the design gate, validate the unlocked batch before one atomic write
   with stale-preview protection and rollback.
3. Add dedicated PrivacySettings review UI and prompt; rules stay outside
   composables and feature UI stays out of `MemberShell.kt`.
4. Reconcile concurrent UI/design edits, integrate, run approved gates, then
   complete physical acceptance separately.

See [Task 01](tasks/01-parse.md), [Task 02](tasks/02-persist.md),
[Task 03](tasks/03-ui-prompt.md), [Task 04](tasks/04-integrate-verify.md),
and [checkpoint](checkpoint.md). Tasks are not executable before contract
commit and named test profile approval with offline dependencies available. One
local worker implements
serialized slices; separate analyst/reviewer checks are not implementation
workers. There is no controller role. One eventual PR; commit, push, install,
and device use require their applicable authorization.
