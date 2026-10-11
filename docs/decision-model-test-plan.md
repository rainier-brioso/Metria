# English-only local decision-model experiment

**Status:** desktop Von inference and full-model ONNX export/parity completed
on 2026-10-11. Both tested profiles fail the combined quality/coverage gate;
Android integration, installation and physical acceptance remain pending.
See [measured desktop results](decision-model-desktop-results.md).
English-only scope applies to this hackathon
experiment, not a removal of existing Spanish support.

## Hypothesis and demonstration

A small local decision model can understand varied record questions and select
a validated read without Gemma generation. Metria then renders a short factual
answer from fresh local evidence. The useful hackathon claim is measured
end-to-end speed with private, grounded answers, not a generic benchmark score.

Demonstrate three fictional-record questions in airplane mode:

- "How much protein have I recorded today?"
- "What did I log yesterday?"
- "How many calories were in my last meal?"

Show matched baseline/experimental timings and actual Gemma invocation counts.
An ambiguous request such as "What about that one?" must take the existing
fallback. Do not describe the model as Jev itself or imply affiliation.
This experiment complements the Nebius slice; it does not replace the separate
event requirements tracked in `hackathon-assistant-plan.md`.

## First candidate and bounded scope

Start with [Von](https://github.com/wfzyx/von), a 395M ModernBERT decision model
whose documented English-only scope now fits the demo. The project reports
single-pass choice scoring and Apache-2.0 licensing. Its desktop CPU support
does not establish Android compatibility or phone latency.

Freeze a specific code revision, weights revision, tokenizer, scoring head,
calibration parameters and checksums before evaluation. Review code and weight
licences, artifact size and export requirements. No mutable "latest" model
should be used for measured results.

Only three read families can bypass Gemma in the first slice:

| Family | Allowed bounds | Answer |
| --- | --- | --- |
| Recorded nutrition | Today or rolling seven days; calories or protein | Host totals, period and coverage |
| Recorded meal list | Today or yesterday; all meal slots | Host list, date and coverage |
| Latest recorded meal | Existing argument-free latest-meal contract | Saved calorie/protein estimates and coverage |

The classifier menu must also include meal drafting, general food questions,
medical/safety requests, ambiguous/multiple intents and unsupported requests.
These labels are routing negatives: they cannot execute a read or write in this
slice. They use the existing validated assistant/boundary path.

No weight writes, goal changes, navigation redesign, vision replacement,
voice transcription, saved-dish ranking or new persistence schema in this test.
Follow-ups remain on the existing path for the first slice. Do not route using
a previous answer's record values or a hidden selected-meal ID.

## Host integration contract

```text
submitted question
  -> existing safety/date/context boundaries
  -> rules-only route or local decision candidate
  -> host validates intent, explicit period and metric
  -> existing bounded read against current unlocked vault
  -> deterministic answer + existing factual audit
  -> existing chat presentation and retention
```

Use only bounded current text and described options as model input. No whole
vault, photos, notes, free-form record history or raw record values are needed
to select this route. Existing host rules take precedence when they establish
a required tool or reject a date. A model cannot override them.

Period/metric choices need explicit host validation. Conflicting dates, missing
scope, multiple requests, general advice and a forced choice without supporting
evidence cannot enter the fast path. Include an abstention option. If the
classifier accepts a route but the host cannot validate its arguments, fall
back without executing a speculative read.

Confidence cutoffs are fitted on the development set, then frozen. Raw softmax
or a vendor's default cutoff is not proof of Metria accuracy. Unknown, timeout,
load failure or unsupported input returns to the existing local assistant;
lock/cancellation retires the request without starting a fallback. No automatic
cloud fallback. Once a host answer is complete, do not call Gemma to polish it.

Use the existing request identity, cancellation, vault lock, factual binding,
evidence cards and encrypted conversation policy. Missing nutrition remains
missing; empty recorded history is not evidence that nothing was eaten.

## Stage 1: quality and export feasibility

Timebox initial feasibility to one focused development day. Stop rather than
turn this into an unbounded runtime port before the hackathon.

1. Prepare 120 development and 120 locked test cases using fictional English
   requests and fictional records. Split by paraphrase/template family, not
   random near-duplicate rows. Keep the locked set out of threshold selection.
2. In each set include 60 eligible cases (20 per family) and 60 negatives:
   ambiguity/follow-ups, advice versus recording, dates outside scope, mixed
   intents, safety/medical questions, and prompt injection/negation/typos.
3. Compare the current Gemma path, host rules with deterministic answers, and
   host rules plus Von. Use identical final read/render logic for the latter
   two so any added value from Von is identifiable.
4. Report intent AND period AND metric correctness, accepted-case accuracy,
   fallback rate, confident errors and extra correct paraphrases over rules.
   Record failures, not only successful demo prompts.
5. Export the complete Von encoder/head to an Android-capable format, preserving
   tokenization, masks, option markers and probability calculation. A generic
   ModernBERT export without the decision head is not an equivalent model.
6. Check desktop/export parity on the development fixtures before testing the
   locked set. Proposed tolerances: identical selected choices and maximum
   absolute probability difference <= 0.02; document any quantization changes.

[ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/) is one
candidate runtime, not a selected production dependency. Verify operators,
tokenizer packaging and ARM execution. Any production dependency requires
separate member approval. Use a standalone synthetic harness before production
integration if that avoids changing the app's dependency set.

If Von cannot export or adds no useful routes beyond rules, retain the rules
result and record a no-go for Von. A small Qwen-backed
[OpenJev baseline](https://github.com/lookski/openjev) is a later candidate,
not an automatic expansion of this test. No desktop server counts as on-device.

## Stage 2: target-phone comparison

After export/quality pass, run on the Samsung SM-S928B pilot phone, confirming
its current Android version. Use an isolated fictional fixture provider; do
not insert benchmark records into, replace, or export the member vault.

Measure all three paths on the same 12 requests: three per eligible family and
three fallback requests. Run five repetitions per path, with order rotated and
thermal state recorded. Report per-family and overall p50/p95 plus sample size;
60 runs per path are pilot evidence, not a production latency guarantee.

Measure submission-to-complete-visible-answer, time to thinking feedback,
classifier latency, vault read/render time, Gemma calls, fallback overhead,
download size, process memory and start/end thermal severity. Include both
first load and warm runs. Include representative coexistence with resident
Gemma; do not hide memory pressure by reporting classifier-only results.
Battery/energy claims require a controlled longer benchmark; do not infer them
from speed or these short sessions.

Exercise empty/partial records, Stop during routing, Stop during fallback,
lock/background, restart, repeated Send and return from cancellation. No late
answers, duplicated turns, unexpected reads or replay. Check airplane-mode
functionality; independently verify no inference network traffic for a privacy
claim. Keep technical evidence content-free; no raw private capture is needed.

Android changes require the complete gate from `TESTING.md`, `git diff --check`,
approved `install -r`, member biometric unlock and relevant phone UI checks.
Existing vault/model data must survive installation. English-only pilot evidence
does not close broader bilingual M4 gates.

## Proposed go/no-go thresholds

These are experiment targets, not observed results or release guarantees:

- Locked test: zero incorrect accepted fast routes or unsupported fast routes;
  zero factual binding errors; at least 70% of eligible cases bypass Gemma.
  Report accepted sample size and confidence interval, even with zero errors.
- Von adds at least 10 percentage points of correct eligible-case coverage over
  rules at the same zero-observed-error gate. Otherwise prefer rules for this
  slice and document that a decision model added insufficient value.
- Phone: at least 2x faster median complete answers on eligible requests than
  the current Gemma baseline; warm fast-path p95 <= 1 second.
- Warm fallback overhead p95 <= 500 ms. First-load delay is reported separately;
  an unload/reload strategy must not erase the demonstrated benefit.
- No OOM, crash, late answer, vault mutation or replay in the lifecycle checks;
  combined resident memory fits the phone without destabilizing Gemma.

If a threshold fails, retain the current production path. Do not tune against
the locked set and call it held-out; create a new independent set for a retest.

## Checkpoint and next bounded task

- [x] Record English-only hackathon scope and the concrete test plan.
- [x] Create 120 development and 120 reserved test prompts plus a standalone
  scoring harness; validate the development-only synthetic rules reference.
- [ ] Complete semantic template-family review before freezing the reserved
  test set; capture actual production-rules/Gemma/Von predictions.
- [x] Pin Von artifacts; measure desktop quality and export parity.
- [x] Prove fixed-input Android execution/parity in the debug pilot.
- [ ] Measure the three complete phone paths with actual text/host integration.
- [x] Record desktop no-go for the tested configurations in M4 checkpoints.
- [ ] Complete a final go/no-go if a revised candidate reaches Android gates.

The [harness runbook](../tools/decision_model/README.md) documents imported
prediction manifests and scoring. The exact-phrase Python reference is not the
production Kotlin baseline. The reserved test pool has structural/lexical
checks, but semantic family separation remains a prerequisite for frozen
held-out scoring. No locked predictions have been evaluated.

See [Von feasibility findings](decision-model-feasibility.md) for the selected
source revision, large duplicated checkpoint files and full encoder/head export
requirements and [desktop results](decision-model-desktop-results.md). Local
development inference and export ran; the concise profile improved coverage
but produced incorrect accepted routes. Next task: a bounded revised routing
approach with explicit negative rejection on development data before member-chat
integration. The [Android pilot](decision-model-android-pilot.md) passed fixed-input
runtime/parity, but failed rejection and did not close M4 acceptance. See
[routing and runtime research](decision-model-routing-research.md) for community
findings, model lifetime choices and the next bounded slice.
