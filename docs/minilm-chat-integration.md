# MiniLM selected-function chat integration

Status: debug Chat MiniLM lookup and dated fallback confirmed on the phone; broader acceptance pending — 2026-10-11.

The member chose MiniLM and requested direct integration into Metria rather than
another external benchmark. The first debug-build slice handles an explicit,
English-only request to list today's recorded meals. All other requests retain
the existing assistant path. This is a read-only optimization, never a new meal
write, nutrition estimate or medical decision.

## Contract

- A small local MiniLM classifier may select the candidate operation only when
  the independent host policy also requires today's bounded `read_meals`.
- Reject mixed requests, writes, other dates, nutrition totals, advice and
  ambiguous follow-ups from the fast path. Uncertain scores, missing artifacts
  or runtime failure fall back to the existing Gemma path.
- Read fresh unlocked records through the existing dispatcher and render with
  `MetriaMealRecordReply`, preserving source/date/bounds/factual validation and
  expandable evidence. Use the existing encrypted conversation persistence.
- Preserve request identity, Stop, lock, context reset and stale-result rejection.
  Private text and embeddings are transient; there are no raw router reports,
  prompt logs, remote inference or new persistence schema.
- Keep ONNX Runtime and public MiniLM artifacts in the debug source set. Release
  routing stays unchanged. Public artifact provisioning is reproducible and
  pinned; generated model binaries are excluded from Git.
- Initially the existing chat readiness requirement still prepares Gemma.
  Accepted turns skip Gemma generation, not Gemma residency. Removing that
  prerequisite and measuring simultaneous memory remain separate work.

## Acceptance on the phone

Use ordinary Chat, not a synthetic activity. Check an eligible lookup with
records and with no records; confirm that its answer matches the visible meals
and its evidence. Check yesterday, meal logging, nutrition totals and mixed
lookup/advice requests follow the normal path. Exercise repeat lookup, Stop,
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

## Implementation and automated evidence

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
