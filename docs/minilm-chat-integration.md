# MiniLM selected-function chat integration

Status: today/yesterday meal lists and a latest-meal estimate confirmed on the phone; today nutrition expansion in progress — 2026-10-12.

The member chose MiniLM and requested direct integration into Metria rather than
another external benchmark. The first debug-build slice handles an explicit,
English-only request to list today's recorded meals. Subsequent slices extend
this to yesterday's recorded meals and saved calorie/protein estimates for the
latest recorded meal. The 2026-10-12 extension adds a single requested
recorded calorie/protein total for today using the exact-day nutrition result.
All other requests retain the existing assistant path. This is a read-only optimization, never a new meal
write, nutrition estimate or medical decision.

## Contract

- A small local MiniLM classifier may select the candidate operation only when
  the independent host policy also requires today's or yesterday's bounded
  `read_meals`, a bounded `read_latest_meal` for saved calorie/protein estimates,
  or today's single-metric `read_day_nutrition`.
- Reject mixed requests, writes, other explicit dates, unsupported nutrition totals, advice and
  ambiguous follow-ups from the fast path. Short metric follow-ups require the
  validated transient scope described below. Uncertain scores, missing artifacts
  or runtime failure fall back to the existing Gemma path.
- Read fresh unlocked records through the existing dispatcher and render with
  validated deterministic replies, preserving source/date/bounds/factual validation and
  expandable evidence. Use the existing encrypted conversation persistence.
- Preserve request identity, Stop, lock, context reset and stale-result rejection.
  Private text and embeddings are transient; there are no raw router reports,
  prompt logs, remote inference or new persistence schema.
- Keep ONNX Runtime and public MiniLM artifacts in the debug source set. Release
  inference stays on Gemma. Public artifact provisioning is reproducible and
  pinned; generated model binaries are excluded from Git.
- Initially the existing chat readiness requirement still prepares Gemma.
  Accepted turns skip Gemma generation, not Gemma residency. Removing that
  prerequisite and measuring simultaneous memory remain separate work.

## Acceptance on the phone

Use ordinary Chat, not a synthetic activity. Check an eligible lookup with
records and with no records; confirm that its answer matches the visible meals
and its evidence. Check yesterday's lookup and its exact date; meal logging,
dates beyond yesterday, unsupported nutrition totals and mixed lookup/advice requests must
follow the normal path. Exercise repeat lookup, Stop,
rotation, background/relock and missing/corrupt artifact fallback. No private
chat content may be copied into development reports. Record only content-free
timings and technical outcomes with explicit measurement limits.

The complete Android unit-test/build gate and independent review precede
installation. Physical acceptance remains a separate gate.

## Public artifacts

Repository: `sentence-transformers/all-MiniLM-L6-v2`, Apache 2.0, revision
`1110a243fdf4706b3f48f1d95db1a4f5529b4d41`.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `onnx/model_qint8_arm64.onnx` | 23,026,053 | `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474` |
| `vocab.txt` | 231,508 | `07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3` |

The initial selection uses similarity to fixed synthetic intent examples with
a rejection threshold and margin, not a trained Metria classification head.
Scores are similarities, not calibrated probabilities. This is a conservative
debug integration; no accuracy, latency or resident-memory improvement is
claimed before phone measurement.

## Initial implementation and automated evidence

`ModelLifecycle` selects the candidate on its existing assistant worker, then
emits the normal typed `read_meals(days_ago=0)` handoff without creating or
sending a Gemma conversation. The host guard full-matches a closed English
read grammar, rejects selected-meal context and another required tool, and
freezes the current local day. The normal Chat dispatcher supplies fresh bounded
records and evidence. Its continuation checks the frozen date and existing
`MetriaMealRecordReply` validation before publishing through the normal request-ID
and encrypted-history path.

The debug factory loads and reuses one CPU ONNX session (two intra-op threads,
one inter-op thread, basic optimization). It verifies both artifact hashes,
implements uncased BERT WordPiece tokenization, attention-aware mean pooling
and L2 normalization of the 384-dimensional vector. It compares maximum cosine
similarity to six fixed positive and ten fixed fallback examples. Acceptance
requires positive similarity at least 0.58 and a margin of at least 0.06. These
cutoffs are provisional; fixed-example separation is not independent accuracy.
The release factory always abstains and uses no ONNX dependency.

Stop/lock invalidate the request and interrupt pending router work; native work
already executing may finish before its result is discarded. Session close is
serialized with inference. A close during initial construction causes the new
session to close instead of registering on a destroyed lifecycle. Ordinary
router exceptions fall back; VM errors are not swallowed to start more model
allocations. Private inputs and embeddings are never stored in the public model
directory or synthetic Von report.

Preparation from the repository root:

```powershell
rtk proxy .local-state\decision-model\venv\Scripts\python.exe tools\decision_model\prepare_minilm.py
```

The public model is generated into the debug assets and excluded from Git by
an exact path; vocabulary, model notice and Apache license are retained.
No new production dependency was added. Existing debug ONNX Runtime 1.22.0 is
reused. The public asset preparation verifies pinned size and SHA-256 before copy.

Final automated gate from `native-android/`:

```powershell
rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon
```

Passed: 449 tests, zero failures/errors/skips; debug assembly and release Kotlin
compilation passed. Nine new tests cover closed read scope, frozen host-tool
selection and tokenizer cases. Four ordinary chat phrases additionally match
token IDs generated independently with Hugging Face's tokenizer and the pinned
real vocabulary. `git diff --check` passed. Custom workers implemented the
runtime and lifecycle separately; independent review corrected cancellation,
exception fallback and initial-load/close issues. Final review also rejected
record-type-ambiguous questions such as “What did I log today?” and required
carrying the frozen date through a host-only tool-call field. The UI validates
that exact response before appending evidence. A focused re-review found no
remaining material defect in those corrected paths. The final complete gate
passed after these changes; phone runtime/acceptance remains pending.

Final debug APK: 214,729,304 bytes; SHA-256
`a549fe51f5b4097f3eecb3f0d60102e603b374219b83660d53ef510972b0a2d6`.
On Samsung SM-S928B / Android 16, the member explicitly approved installation
and the real-Chat test cycle. The APK checksum matched the reviewed build before
installation. Approved `adb install -r` returned `Success`; force-stop and
`am start -W -n app.metria/.MainActivity` returned `Status: ok`, COLD launch,
990 ms activity launch time (not model-load or answer latency). No data clear,
uninstall, signing-key replacement, vault replacement or Gemma-file deletion
was performed. Existing-record inventory has not been inspected or confirmed.
The member was asked to perform biometric unlock and submit the ordinary Chat
question “Show the meals I recorded today.” No biometric inspection or automation
was attempted. The member completed that named Chat interaction and reported
“The reply matches my records.” This is member-observed positive lookup
acceptance. The member then checked Developer diagnostics and confirmed the
exact MiniLM route / Gemma-generation-skipped message. This establishes actual
free-text MiniLM activation plus a correct member-observed real-Chat lookup on
this phone, not held-out accuracy or timing. The dedicated public model file
exists at the expected 23,026,053-byte size; phone SHA-256 matches the pinned
`4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474`.
No member records or chat text were read into device diagnostic output.

One content-free `dumpsys meminfo app.metria` snapshot during the active Chat
test reports TOTAL PSS 1,591,249 KB and TOTAL RSS 1,706,824 KB. Gemma readiness
is required by this slice and the MiniLM route has been confirmed. This is one
process-level snapshot, not peak memory, a per-model attribution, a controlled
comparison with the Von pilot, or a complete coexistence/performance gate.

The member was asked to submit “What did I eat yesterday?” and verify the reply
against yesterday's records and that diagnostics no longer show the MiniLM route.
The member reported “Correct yesterday reply; no MiniLM route.” This confirms
the named other-day request abstained from the shortcut and the normal Chat path
returned a member-observed correct record reply. Today's fast path and yesterday's
fallback are the two physically exercised scenarios; they are not an exhaustive
negative set, a latency benchmark or release acceptance.

## Existing monitoring evidence

The member correctly pointed out that Gemma monitoring already exists. Inspection
confirmed the existing opt-in content-free assistant trace is enabled on the
phone and contains event timestamps for both named Chat checks. This inspection
read only the allowlisted timestamp/event/operation/outcome/tool/period/request-ID
fields, never private questions, answers or records. No raw phone trace was saved
to the repository.

| Physically confirmed path | Lifecycle turn start → validated reply | Scope |
| --- | ---: | --- |
| MiniLM today-meal lookup | 611 ms | First tested router request; includes routing/setup and host continuation |
| Normal Gemma yesterday-meal lookup | 15,131 ms | Native tool-selection turn and host continuation |

These intervals are derived from existing wall-clock event timestamps, paired
through adjacent tool/continuation events (the continuation gets a new request
ID). They are not isolated inference time, tap-to-render latency, repeated warm
statistics or a controlled baseline: different questions and record dates were
used. They establish actual recorded monitoring samples, not a general speedup
claim. The MiniLM turn reached its tool handoff after 495 ms; the native fallback
reached its accepted tool handoff after 14,934 ms. First-output time is not present
in this assistant trace.

Gemma already publishes latest-stage elapsed time, process PSS, battery percentage
and thermal severity in Developer diagnostics. That stage string is overwritten
by later stages, so it is not a retained complete-turn metric. The MiniLM path
currently replaces that string with its route confirmation; it does not yet
publish comparable detailed stage metrics. Separately, image-analysis audits
persist encrypted stage timings, first nonblank streamed-output timing and
thermal start/end; those richer audit fields do not cover ordinary Chat.

Next bounded task: verify repeat lookup, Stop and background/relock in ordinary
Chat if continuing this test cycle. Record each physical
result separately. Repeated warm latency, isolated first-load cost, controlled
Gemma coexistence/peak memory, missing/corrupt artifacts and the lifecycle matrix
remain unmeasured; these event samples and single memory snapshot do not close
those gates.

## Yesterday lookup expansion — 2026-10-11

After the reviewed checkpoint `2efa900`, the member asked to continue improving
the assistant directly in Metria, without more model benchmarking. The next
bounded operation is an explicit English meal-list lookup for yesterday.

`MiniLmMealReadPolicy` independently full-matches today/yesterday requests and
resolves only offsets 0 or 1. A conflicting existing host tool, date, range,
metric, period or selected-meal context prevents the shortcut. The debug router
adds six fixed yesterday examples; the similarity threshold and rejection margin
are unchanged. These examples are routing prototypes, not accuracy evidence.

The handoff carries the frozen local reference day in `hostReadDate` and the
requested offset in `daysAgo`. The ordinary dispatcher applies that offset
once; the host checks the response's as-of day and exact requested day before
adding evidence or publishing the deterministic reply. Today's diagnostic stays
unchanged. Yesterday's diagnostic is
`Assistant route: MiniLM yesterday-meals read; Gemma generation skipped.`
Existing trace fields stay unchanged; yesterday uses the allowed `none` period
and the existing `read_meals` tool name.

Release abstention, Gemma readiness, cancellation, lock, transient inputs and
encrypted Chat retention retain their existing contracts. Nutrition totals,
advice, mixed requests, writes, implicit follow-ups and other dates continue
through the normal assistant path. No new model, dependency, vault format,
member-facing screen or inference benchmark is added.

The complete gate passed after the final test correction:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`
from `native-android/`. XML reports contain 454 tests, zero failures/errors/skips.
Debug assembly and release Kotlin compilation passed. The first test run exposed
an incorrect test assumption that the general grounder always selects a tool for
every accepted strict phrase; the corrected tests exercise both recognized host
phrases and the already-supported null-host candidate path. Production grounding
was not expanded. Tests retain today validation, check populated yesterday and
empty results, year boundaries, date/offset mismatch and conflicting host scope.
Independent review found no material defect; `git diff --check` passed.

APK: 214,729,304 bytes; SHA-256
`4ba4601a34241bd9e8a644496c6334fe677f87ae757620bb0dc1abd596a23247`.
Installation and physical results remain separate from this automated evidence.

Under the member's existing explicit approval for the active in-place Chat test
cycle, `adb install -r` returned `Success` on Samsung SM-S928B / Android 16.
Force-stop and `am start -W -n app.metria/.MainActivity` returned `Status: ok`,
COLD launch, 1,290 ms activity time (not model or answer latency). No data clear,
uninstall, signing replacement, vault or Gemma deletion was performed. No private
screen or record data was inspected. This build also contains the checkpoint's meal-entry
Stop correction; installation alone does not accept its finishing state.

Next bounded phone check: the member asks “What did I eat yesterday?”, compares
the exact dated reply with saved records and confirms the yesterday MiniLM
diagnostic. Then recheck today's route and one other-date or mixed request's
fallback, followed by Stop/background/relock as applicable. No physical pass or
new performance result is claimed until separately confirmed.

The member completed “What did I eat yesterday?” and reported
“Reply matches; MiniLM yesterday route shown.” This confirms the requested
yesterday lookup matched their retained records and the debug diagnostic showed
Gemma generation skipped on this build. It is a targeted member-observed pass,
not exhaustive acceptance or a latency measurement. The member then completed
“Show the meals I recorded today” and “What did I eat two days ago?” and reported
“Both correct; today MiniLM, older date normal route.” This confirms today's
fast-route regression and the named older-date fallback on the installed build.
Cancellation, empty records, relock and missing/corrupt-model cases remain open.

## Latest-meal estimate expansion — 2026-10-12

Status: full automated gate and independent review passed; physical acceptance pending.

The member asked to continue after discussing MiniLM embeddings and the limits
of strict wording. The next operation retrieves saved calorie/protein estimates
for the latest recorded meal. Supported English paraphrases include polite
questions about how many calories or how much protein was in the member's last,
latest or most recent meal. This is still a bounded read, not unrestricted
language understanding. Unrecognized phrasing retains the ordinary assistant.

MiniLM distinguishes the meal-list and latest-meal operations using separate
fixed intent examples. The host independently checks the requested operation,
rejects competing scope and freezes the local reference day. The existing
`LatestMealNutritionResult` reads one latest recorded meal within the existing
90-day window, excludes future records and returns ambiguity rather than
guessing when recorded times do not identify a single meal.

The direct reply reuses `LatestMealNutritionReply` and its factual audit after
matching the frozen as-of date. Saved estimates retain their existing display
rounding; partial and missing estimates stay explicit. It returns only the
existing saved calorie/protein fields, without calculating new nutrition or
presenting one meal as daily intake. No goal, note, photo or food-item payload is
added. The existing unlocked dispatcher, request identity, cancellation,
encrypted Chat retention, debug-only runtime and release abstention remain.

Named phone checks after review/build/install: compare a polite latest-meal
calorie/protein question with the saved meal and confirm the MiniLM latest-meal
diagnostic; verify a daily-total request retains the normal route; recheck a
today/yesterday lookup. Partial/missing/empty/ambiguous, Stop and relock states
are separate checks, never inferred from a successful build.

The complete gate passed on 2026-10-12 (Europe/Madrid):
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`
from `native-android/`; 457 tests, zero failures/errors/skips, debug assembly and
release Kotlin compilation passed. An overlapping focused Gradle run caused an
incremental-cache error during this gate; the extra run was stopped and Gradle's
non-daemon compiler fallback completed the same gate successfully. No source
change was made to bypass that error.

Custom explorer, worker and reviewer handled discovery, implementation and
independent review. Review corrected an intermediate shared date-check regression
before installation: day-list replies use target date plus offset as the as-of
reference, while latest-meal replies use their frozen search reference directly.
The lifecycle now calls the same pure validator exercised by the tests. New
tests cover polite latest-meal wording, conflicting scope, frozen latest results
and crossed tools; existing latest-meal tests preserve partial/missing/empty/
ambiguous behavior. The final review found no material defect.
`git diff --check` passed.

APK: 214,729,304 bytes; SHA-256
`70fd3a028ebf93fc10c339c57a4631f4efc0dfffc2f077dd213dbb8a0306800b`.

Under the existing explicit approval for the active in-place Chat cycle,
`adb install -r` returned `Success` on Samsung SM-S928B / Android 16. Force-stop
and launch returned `Status: ok`, COLD activity time 1,362 ms (not model/answer
latency). No data clear, uninstall, signing replacement, vault/Gemma deletion
or biometric automation occurred. No private screen or record data was inspected.
The member was asked to unlock and compare “Could you tell me how many calories
were in my last meal?” with the saved meal and confirm
`Assistant route: MiniLM latest-meal read; Gemma generation skipped.`
The member reported “Matches saved meal; no MiniLM latest route.” This confirms
the saved-record answer but fails acceptance of the latest-meal optimization.
Content-free existing trace inspection confirms a native `read_latest_meal`
handoff and native tool-result continuation; it does not identify why MiniLM
declined. No private question, answer or record payload was inspected. The
requested phrase passes the host policy in unit tests. Router exceptions and
similarity rejection currently collapse to the same false result, so the next
bounded correction is an ephemeral content-free router outcome in Developer
diagnostics followed by the same in-app question. No threshold adjustment or
speedup claim follows from this failure.
Broader record preservation, cancellation,
empty/partial/ambiguous phone states and release acceptance remain separate.

### Content-free fallback diagnosis

The router now returns a typed outcome instead of silently collapsing every
decline to false. Outcomes distinguish host scope, low similarity, insufficient
margin, inference failure, unavailable runtime and interruption. Scores are
finite cosine similarities, not probabilities. The existing 0.58 similarity
threshold and 0.06 competitor margin are unchanged.

A debug-only transient outcome string stays separate from the replaceable
Gemma timing string. It contains only a fixed operation/outcome and optional
scores; no input, embedding, exception message, record or response is retained.
It clears on the next Chat turn, explicit conversation clear, cancellation or
vault lock. It is not added to the vault or trace store. Release still abstains.
The same pure score evaluator is used by inference and unit tests.

Final full Android gate passed after the diagnostic lifecycle fixes: 460 tests,
zero failures/errors/skips; debug assembly and release Kotlin compilation.
Independent review found stale diagnostics after unload/idle style changes;
both now clear while a queued style preserves the active turn. Parent inspection
confirmed those boundaries; `git diff --check` passed. No score cutoff was changed.

Retry APK: 214,729,304 bytes; SHA-256
`0c0b8b17619dd3523df4cb93b60fca64d7a2c87d6cff5caef9f627b29f7f8cc9`.

Within the existing approved cycle, retry `adb install -r` returned `Success`
on Samsung SM-S928B / Android 16. No data clear, uninstall, signing replacement,
vault/model deletion or biometric automation occurred. The member was asked to
open/unlock Metria, retry the exact polite calorie question and report only the
content-free `MiniLM route:` line. ADB lost its daemon connection during the
launch attempt; installation succeeded, but automated launch was not confirmed.
The member was directed to open Metria manually. On retry, the member reported
an instant reply similar to the previous Gemma answer and confirmed the exact
`Assistant route: MiniLM latest-meal read; Gemma generation skipped.` diagnostic.
This establishes the named latest-meal fast route on the retry build, alongside
the preceding saved-meal comparison. Both paths reuse the same deterministic
saved-record reply. The first fallback's cause remains unknown; no score cutoff
or scope rule was loosened, and the successful retry does not explain it.
“Instant” is member-observed responsiveness, not a measured latency result.
Other latest-meal phrasing, repeated-run reliability, protein-only questions,
partial/missing/empty/ambiguous records and cancellation/relock remain separate
physical checks.

## Today's recorded nutrition expansion — 2026-10-12

Status: full automated gate passed; physical acceptance pending.

The member requested continued direct in-app integration. This slice handles an
explicit English question about one metric: today's recorded calories or
protein. Meal advice, goal comparisons, multiple metrics, other dates, latest/
selected meals, writes and implicit follow-ups remain outside this shortcut.

The host independently validates intent and the requested metric. A matching
today summary request is narrowed to the existing exact-day `read_day_nutrition`
with offset zero, a frozen local reference date and no goals. The existing
`HistoricalDayNutritionResult` uses the same local summary arithmetic and display
rounding. A strict direct-reply validator checks source, day, metric, language,
counts, estimate availability and the recorded-meals-only interpretation before
the factual audit and publication. Partial and missing estimates remain visible;
no estimate is not converted to zero and recorded totals are not complete intake.

The existing summary conservatively excludes a nutrition entry lacking any of
the four stored macro fields from its totals; this slice does not change that
calculation or fill missing fields. No new model, production dependency, vault
format, raw logging or cloud request is added. The model and runtime remain
debug-only. Other routes and their phone evidence must be preserved.

After review and the full gate, test a today calorie request against the visible
records and confirm `MiniLM today-nutrition read; Gemma generation skipped`, then
protein, an existing meal lookup and an out-of-scope goal/advice request.
Partial/missing/empty, cancellation/relock and repeated-use reliability remain
separate physical checks. No inference benchmark is requested.

The full Android gate passed after the final corrections:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`
from `native-android/`; 464 tests, zero failures/errors/skips, debug APK assembly
and release Kotlin compilation passed. The first run caught a test fixture that
incorrectly expected an entry missing two macro fields to contribute; the fixture
now follows the unchanged four-field inclusion rule. Existing calculation tests
retain incomplete-entry exclusion. Four new tests cover scope/host agreement,
conflicting operations and dates, full/partial/missing/empty results, malformed
payloads and independent rounding boundaries.

Custom exploration and review corrected broad date matching, premature evidence
publication and missing competing intents. Aggregate totals must match complete
plus partial subtotals within the independent display-rounding allowance of
1 kcal or 0.1 g protein. Decimal comparison avoids a floating-point false
rejection at that boundary. The same pure validator runs before evidence is
added and before the lifecycle publishes the answer. No calculation or global
fallback rule was changed. `git diff --check` passed.

APK: 214,729,304 bytes; SHA-256
`db38ef4039482907fd65f04cf14a8813c11bc90bf487fda1c2efaf2da48f6c46`.

Final focused re-review found no remaining material issue. Under the existing
approved in-place Chat cycle, `adb install -r` returned `Success` on Samsung
SM-S928B / Android 16. Launch returned `Status: ok`, COLD activity time 1,990 ms
(not model or answer latency). No data clear, uninstall, signing replacement,
vault/Gemma deletion or biometric automation occurred. No private records or
screens were inspected. The member was asked to compare today's calorie and
protein answers with saved meals and confirm the today-nutrition route after
each. The member reported that the first (calorie) question used MiniLM and the
second (protein) question used Gemma. This confirms the calorie shortcut's route,
but does not separately confirm its numeric comparison; the protein fast route
has not passed. The member was asked for only the complete content-free
`MiniLM route:` outcome and scores after the protein turn. No routing threshold
was changed on the basis of this partial result. Other physical states remain
pending; installation alone does not close them.

## Retained routing monitoring — 2026-10-12

Status: automated gate and review passed; phone verification pending.

The member requested using the existing monitoring instead of asking them to
copy diagnostics. The existing eight-column event trace confirms the protein
turn entered native Gemma and later reached a vault-lock boundary. It does not
retain the MiniLM decision, and locking clears the temporary diagnostic. Thus
the missing on-screen line cannot establish why the earlier turn declined.

The extension records fixed MiniLM decision outcomes, Gemma/native or legacy
route selection, approved tool/period/request IDs and optional bounded finite
cosine scores in the existing opt-in trace. Its original eight-column rows must
remain readable; only routing score rows add three numeric-or-unknown fields.
Existing retention, size bounds, release no-op and deletion remain. No raw
question, answer, record, embedding or exception is added. Future diagnosis uses
authorized local reads of those allowlisted technical fields, without asking
the member to transcribe them. Biometric unlock remains a member action.

The trace retains eight-column legacy/selection rows and accepts eleven-column
`route_decision` rows. Cosine inputs must be finite and within the small numerical
tolerance; tolerated drift is clamped to [-1, 1] and formatted to four decimals
with a fixed locale. Stored values outside [-1, 1], malformed score strings and
score columns attached to another event kind are pruned. Routing periods include
the fixed `yesterday` category alongside existing periods. Native fallback
selection is recorded before conversation construction, so initialization
failure cannot hide the selected path. Engine/request checks protect the events.

Complete Android gate passed: 466 tests, zero failures/errors/skips; debug APK
assembly and release Kotlin compilation. Store tests cover old/new row restart,
fixed score formatting, numerical drift, invalid/non-finite scores and pruning.
Initial compilation caught a nullable period access; it was corrected before
the passing gate. Independent re-review found no remaining issue; parent checked
the final paths and `git diff --check` passed. No routing thresholds or product
operations changed in this monitoring slice.

APK: 214,729,304 bytes; SHA-256
`5d2561245c980a5c0883ae4cec5434d90e2f35500cd961330099a30ddb31ed2e`.

Under the existing approved cycle, `install -r` returned `Success` on Samsung
SM-S928B / Android 16. Launch returned `Status: ok`, COLD activity time 2,130 ms
(not model/answer timing). No data clear, uninstall, signing replacement,
vault/model deletion or biometric automation occurred. The member was asked
only to unlock and send the protein question again; subsequent diagnosis will
read the allowlisted retained events directly. The member confirmed the question
answered, and authorized local metadata inspection found `route_decision` and
`route_selected` with `minilm_accepted`, `read_day_nutrition`, `today`, followed
by `reply_audited/accepted`. The retained scores were positive 0.9841, competing
0.8176 and fallback 0.5197; these pass the unchanged similarity/margin checks.
Legacy rows remained readable alongside the new scored row. This verifies
retained routing monitoring and the named protein fast route on the installed
build, without reading any private prompt, answer or record. Numeric comparison
by the member was not separately reported. The previous fallback cause cannot
be reconstructed because its decision was not recorded; no threshold or scope
change occurred between these two builds. Future declines can now be diagnosed
from retained events rather than manual transcription. No benchmark was run.

## Natural phrasing and metric follow-ups — 2026-10-12

Status: implementation and independent review complete; the three named phone
cases passed. Broader lifecycle and rejection acceptance remains open.

The member approved broader natural phrasing for the existing operations, then
short follow-ups using fresh reads. Named paraphrases include “Remind me what I
ate today”, “What's my protein total today?” and “Calories in my last meal?”
This expands bounded English read grammar and intent examples, not arbitrary
language understanding or new assistant permissions. Negation, writes, mixed
requests, advice, goals, unsupported metrics/dates and selected-meal context
continue to abstain from these shortcuts.

“And protein?” / “And calories?” may inherit only the operation and reference
date of the most recent successfully validated MiniLM today-nutrition or latest-
meal read in the current unlocked session. The host resolves that scope to an
explicit request for classification; every response comes from a fresh read.
No prior meal/nutrition values, model prose or conversation text become evidence.
The next turn consumes the prior scope; only a newly validated result can seed
another follow-up. Cancel, lock, new conversation, unload, unrelated/legacy
operations and date rollover revoke it. Restart leaves no fast scope and keeps
the normal Gemma fallback; no new persistence representation is added.

Explicit eligible read questions containing words like “log” must reach the
native read route rather than the older write migration path. Only the same
independently full-matched read grammar is exempted; actual writes and mixed
requests preserve their existing handling. The release factory still abstains
from MiniLM; explicitly matched read questions use the existing native Gemma
protocol instead of the old write-word migration path on both build variants.

After the full gate and review, exercise a natural day-total question followed
immediately by “And protein?” in the same Chat, and a natural meal lookup or
latest-meal question. Inspect the retained route events directly; ask the member
only to unlock, send questions and confirm replies. Missing scope, lock/restart,
date rollover and out-of-scope follow-ups remain separate physical gates.

The complete Android gate passed: 468 tests, zero failures/errors/skips, debug
assembly and release Kotlin compilation. Independent review found no remaining
material issue; `git diff --check` passed. A test expectation was corrected to
match the intended conflicting-history behavior: decline MiniLM, then require a
fresh Gemma read bound to the validated follow-up scope. Latest-meal fallback
retains its normal 90-day read window instead of an exact-day validation anchor.

APK: 214,729,304 bytes; SHA-256
`29fd9eba10d7d9de231f5b77d63cbe179710534825187cb08bf96cb0bf25614b`.

Under the approved repeated in-place cycle, `install -r` returned `Success` on
Samsung SM-S928B / Android 16, then launch returned `Status: ok` (COLD activity
time 1,290 ms; not answer timing). No data clear, uninstall, signing replacement,
vault/model deletion or biometric automation occurred. Member unlock and three
same-Chat questions were requested: natural today calories, short protein
follow-up, and natural today meal lookup. The member confirmed all three answers
matched their records. Direct inspection of the allowlisted content-free trace
confirmed three MiniLM decisions/selections and accepted host reply audits:
`read_day_nutrition/today` for the first two turns, then `read_meals/today`.
Positive/competing/fallback scores were respectively 0.9431/0.7088/0.6037,
0.9928/0.7837/0.4883, and 0.9305/0.5762/0.7314. These pass the unchanged
similarity/margin checks. Each turn required a fresh tool read; Gemma generation
was skipped for these three accepted routes. No private question, answer or
record was inspected by the host. No controlled benchmark is claimed.

Named natural phrasing and same-Chat metric follow-up acceptance is complete.
Missing scope, lock/restart/date-rollover and out-of-scope follow-up physical
checks remain open; this does not close the broader assistant milestone.

The member subsequently reported both topic-change and lock/unlock sequences
answered. Two local metadata reads found no new Chat-turn events for those
sequences; a later lock event was retained and monitoring remained enabled.
Consequently these rejection routes are not yet verified. A single post-unlock
short follow-up retry was requested to separate member completion from routing
evidence. No fallback success is inferred from the earlier accepted cases.

The member reported the post-unlock retry completed. Its metadata read failed
because ADB then listed no connected devices. Reconnection was requested;
retained-event inspection is pending and no rejection gate is marked passed.

After reconnection, direct allowlisted trace inspection found the completed
post-unlock turn: `route_decision/rejected/scope`, then
`route_selected/accepted/native_gemma` for `read_nutrition_summary/today`, a
fresh tool continuation and `reply_audited/accepted`. This confirms the named
post-unlock short follow-up avoided MiniLM and completed through the normal
Gemma read path. Numeric comparison was not separately reported for this retry.
The earlier topic-change sequence still has no matching retained turn evidence;
topic-change, restart, date-rollover and other rejection cases remain open.
