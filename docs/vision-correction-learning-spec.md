# Meal-photo reliability and private correction learning

**Status:** approved learning-scope decision — 2026-09-28. The member chose
automatic on-device learning from successfully saved, reviewed meals, without
a settings toggle or a per-meal prompt. Bounded correction-note capture and
inspect/clear controls are implemented but still require vault-level and
physical verification. Using notes to rank future photo suggestions is not
implemented.

New catalog-match hints are linked to their saved meal. Older unlinked hints
remain encrypted and clearable, but cannot safely be attributed to a meal and
are excluded from suggestion ranking.

## Product outcome

Several different meal photos per day should usually lead to a useful,
correctable draft without repeated hot-phone inference. A safe rejection and
manual entry remain necessary, but must be exceptions rather than the normal
route. Schema validity, visual correctness, and daily-use performance are
separate acceptance dimensions.

## Current evidence and gap

- A prompt-hardened complex-meal run still returned a rejected group shape
  after about 44 seconds, with incorrect food identities. A prior run returned
  a different invalid field after about 36 seconds. Parser rejection protected
  review but did not improve visual recognition.
- The current pipeline makes one local vision request and validates its JSON.
  A valid JSON food name may still be wrong for the photo. Existing unit tests
  prove the host boundary, not recognition quality.
- The vault already retains member-confirmed private dishes and positive
  catalog match hints. It does not capture a complete, attributable draft-to-
  confirmed correction, including removed foods, additions, regrouping, and
  explicitly rejected suggestions. Current same-photo matching is not
  recognition of a new photo of the same dish.

## Feedback boundary

1. Only an explicit, successful reviewed Save may teach private memory. No
   cancelled review, rejected model output, intermediate field edit, or failed
   vault write may do so. This is a normal local part of saving a meal; there
   is no learning toggle or second confirmation. No-op reviews and manual
   entries create no draft-to-confirmed correction signal.
2. Compare the initial validated proposal with the final confirmed draft by
   stable review-item identity. Distinguish accepted, renamed, removed, added,
   regrouped, portion-changed, catalog-match-changed, and saved-dish suggestion
   accepted or rejected. A changed field is not automatically a reusable fact.
3. Persist only bounded, structured feedback needed for a defined local
   suggestion. Do not persist raw model prose, source photo, hidden output, or
   a full edit history. Keep it in the encrypted vault, separate from debug
   captures and metadata-only diagnostics. Bound memory by meal and total
   size; a deleted meal must remove its linked feedback.
4. A remembered correction may rank at most a few explainable candidates.
   It must never silently replace a food, assert visual recognition, change
   nutrition, or save a meal. The member accepts, edits, or ignores it.
5. A rejected suggestion is negative evidence, but it must be contextual;
   globally blacklisting a food name would be unsafe because a future photo
   may actually contain that food. Ordinary manual entries and unchanged
   automatic catalog defaults do not create correction signals.
6. Provide a member-facing way to inspect and remove learning, including a
   clear-all action that leaves saved meals intact. Deleting a
   meal or remembered dish must not leave an orphaned correction that can
   continue to influence suggestions. Existing encrypted recovery and old
   vaults must remain readable.

"Remember this dish" remains a distinct choice for a reusable named dish.
It must not gate ordinary correction learning, and ordinary corrections must
not silently create or update a remembered dish.

## Evaluation before ranking changes

Use the member-led pilot and scoring rules in
`docs/vision-benchmark-protocol.md`. Its initial sample is diagnostic, not a
daily-use acceptance gate.

Use a consented, diverse local test set with ground-truth visible foods and
member-reviewed outcomes; keep private photos out of the repository. For each
run, separate malformed output, valid-but-wrong food, useful partial draft,
and correct draft. Measure false additions, missed foods, amount of member
editing, portion-range usefulness, manual fallback rate, and accepted versus
rejected remembered-dish suggestions. Keep novel-meal and repeat-meal results
separate so memory gains cannot hide weak base vision.

On the target phone, test multiple distinct analyses in one session and on
cool and warm starts. Record p50/p95 latency, memory, battery, thermal state,
cancel/recovery, and no vault loss. Agree on numerical quality and thermal
gates before describing the flow as suitable for several meals per day.

## Delivery sequence

1. Define and test a pure, stable-identity correction diff without persistence.
2. Add bounded encrypted storage, validation,
   migration/default-empty parsing, atomic Save/rollback, archive round-trip,
   and inspect/clear controls.
3. Apply the signals only to explainable candidate ranking. Add positive,
   negative, contradiction, and false-match tests; no automatic substitutions.
4. Benchmark the base local vision model and any candidate replacements against
   the same held-out photos and repeated-use phone gate. A stronger model or
   extra inference pass is justified only by measured accuracy and thermal
   tradeoffs while preserving offline-first privacy.

The debug-only manual fallback is a recovery feature, not evidence that
recognition or correction learning has passed these gates.
