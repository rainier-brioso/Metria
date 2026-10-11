# Routing, rejection and Android model lifetime

**Research date: 2026-10-11.** This is an investigation and proposed next slice;
it does not change the installed pilot or production chat. An explorer checked
the pinned Von source while the parent inspected public project documentation
and community issues. The [phone experiment](decision-model-android-pilot.md)
remains the measured evidence.

## Two different decisions

Routing chooses the operation for a question. For example, “list today's saved
meals” should select the existing validated meal-list read; “I ate eggs” should
fall back rather than list records. Our failures were wrong operation choices.

Model lifetime decides when weights are loaded and released. The Android pilot
creates one ONNX session, runs all eight questions, then closes the session.
Each additional press of Run recreates it. It does not load a new model for
each question. Inference measured 305–324 ms per case, while session creation
took 2,961 ms. Process PSS with that session open was about 2.99 GiB; this was
one sample, not peak or combined Von/Gemma memory.

The pinned Python SDK already caches a lazy-loaded model: `api.py` retains the
default client, `engine.py` has the engine singleton, and
`OptionMarkerBackend._get_model()` caches `_model` under a lock. Android uses
our own ONNX adapter, so that Python cache does not carry into the APK.
The [ONNX Java session API](https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OrtSession.html)
supports repeated inference calls on a session and explicit resource release.

## Is the rejection problem known?

Yes. [Von issue #21](https://github.com/wfzyx/von/issues/21), opened September
28, reports confidently wrong choices on coding-agent tasks outside its
training domain. The maintainer's [issue #22](https://github.com/wfzyx/von/issues/22),
opened September 30, identifies missing training examples and proposes domain
training plus independent probes. These are reports about another task, not
proof of a Metria fix. They closely resemble our confident date/logging errors.

The [model card](https://huggingface.co/wfzyx/von) acknowledges that its shipped
confidence map does not generalize reliably to other distributions. It supports
refitting confidence on labeled domain examples. Its temperature transformation
does not change the winning option. Calibration may cause a wrong answer to be
rejected, but cannot teach it to choose a different operation.

The pinned `von.patterns.confidence_gate` and `route` provide confidence-based
escalation/default handling. Their cutoffs use **SDK confidence**, not our
pilot's winning probability. In the source, for K options:

`SDK confidence = (K * winning_probability - 1) / (K - 1)`.

For our binary pilot, SDK confidence 0.8 corresponds to winning probability
0.9. Applying that equivalent cutoff to the existing examples would reject
the food-description error, but still accept yesterday's question and the
mixed meal-list/advice request. This is analysis of already observed development
examples, not a new test or a selected release threshold.

The [fine-tuning guide](https://github.com/wfzyx/von/blob/master/docs/finetune.md)
distinguishes confidence refitting, continue-training on labeled English domain
examples, and training a new head with another encoder. Continue-training can
change wrong choices; a smaller encoder is a separate training/export project,
not a drop-in replacement for the existing weights. No training was run here.

The wider community also supports returning no route. Aurelio's
[Semantic Router](https://github.com/aurelio-labs/semantic-router) documents local
encoders and a no-match result. Its
[threshold optimization guide](https://docs.aurelio.ai/docs/semantic-router/user-guide/features/threshold-optimization)
uses labeled examples including explicit no-route cases. This establishes a
useful pattern, not Android compatibility or quality for Metria. We should use
independent validation rather than copy its demonstration's same-data evaluation.

## Handling the loading and memory problem

The following are Metria design proposals, not measured improvements:

| Approach | Benefit | Cost or remaining test |
| --- | --- | --- |
| Keep Von resident during an active assistant session | Avoid repeated cold load | Test memory alongside Gemma, lock/background cleanup and cancellation |
| Load Von only when needed, release before Gemma fallback | Limit overlap | Cold routing plus Gemma loading may erase latency savings |
| Quantize Von and reuse a session | Potentially lower weight/runtime memory | Recheck routing quality, probabilities, Android operators and actual memory |
| Train/use a smaller local intent classifier | Potentially better mobile budget | Separate data/model/export evaluation; no proven candidate yet |

[ONNX quantization documentation](https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html)
recommends dynamic quantization as a starting point for transformer models and
warns that accuracy or speed can worsen. An INT8 prototype is worth measuring;
no fourfold runtime-memory reduction or speedup should be assumed.
[ORT-format guidance](https://onnxruntime.ai/docs/performance/model-optimizations/ort-format-models.html)
also discusses avoiding model-byte copies and disabling weight prepacking to
trade performance for memory. Those settings have format/buffer lifetime
requirements; they are not a verified fix for our current file-path ONNX loader.

Our current chat entry (`ModelLifecycle.startPrivateAgentTurn`) requires Gemma
to be loaded before accepting a turn. A useful fast-read integration must
eventually move routing/read handling before that requirement, while preserving
request identity, lock/cancellation, fresh host reads and answer validation.
Otherwise a classifier could avoid generation but still require Gemma residency.

## Recommended next bounded slice

Improve rejection for **today's meal list**, then connect in shadow mode before
letting a model decision drive member answers. Keep this work separate from a
memory optimization experiment so their effects can be assessed independently.

1. Build a selected-function development set with close negative cases:
   other dates, adding/logging food, advice, calorie/protein totals, mixed requests,
   ambiguous references and attempts to override instructions. Keep the eight
   phone examples as regression cases only. Review semantic separation before
   freezing an independent evaluation set.
2. Compare existing deterministic policy alone, Von alone, and a hybrid. Use
   explicit host rejection for unsupported/uncertain dates and mixed/write
   requests, plus a domain-calibrated model cutoff. Guards are conservative
   filters, not a guarantee of understanding every paraphrase. Measure whether
   Von adds coverage over rules at the same accepted-error gate.
3. Refit confidence only on development/calibration labels. If ranking remains
   poor, stop threshold tuning and consider domain training or a smaller
   classifier. Proposed acceptance remains zero observed incorrect accepted
   reads with useful correct coverage; report sample counts and uncertainty.
4. Add true free-text Android tokenization, independent-option masks and dynamic
   calibration parity. [ONNX Runtime Extensions](https://onnxruntime.ai/docs/extensions/)
   supplies Android text preprocessing infrastructure, but compatibility with
   Von's exact tokenizer is still unverified. The current fixed tensors do not
   support arbitrary typed chat.
5. In debug shadow mode, run routing locally on typed questions while the
   existing assistant still handles the turn. Keep prompts and candidate
   decisions transient; do not reuse the synthetic report writer or raw error
   display for member text. No cloud calls or external member-data labeling.
   Retire inputs on lock/cancel; close native sessions only after active work
   has finished. Record only approved content-free technical evidence.
6. Once rejection and lifecycle gates pass, enable just the validated host
   meal-list read/answer. Unsupported requests use the existing Gemma path.
   Measure complete answers and fallback overhead, not classifier time alone.

Before choosing a resident strategy, measure one session over repeated requests,
then a separate Gemma-coexistence/fallback run on the phone. Check cold setup,
warm latency, peak/current PSS, memory pressure, cancellation and release.
Retain one process-wide run owner so rotation cannot create overlapping sessions.
Public model weights can be retained; private input/context requires the existing
vault boundary. Any production dependency change remains a separate approval.
