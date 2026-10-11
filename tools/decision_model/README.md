# Synthetic decision-model benchmark

This folder contains 120 hand-written fictional English prompts for development
and 120 for locked evaluation. Each split has 20 eligible examples in each of
three read families and 60 negatives across seven categories. The prompts and
template constructions differ between splits. No member records or model files
are included.

The prompts were written as individual constructions, with distinct wording
and syntax across the two splits; no shared substitution template is used.
The fixture test checks exact overlap and flags cross-split same-category
prompts above a 0.70 token Jaccard similarity. This catches obvious shared
templates but is only a lexical screen; semantic independence still needs a
human review before locked scoring. `template_family` is a per-prompt
curation key, not evidence of that review.

The harness scores predictions supplied by an adapter. It does not load Von,
Gemma, or Metria's Kotlin rule implementation; it makes no model or Android
latency measurements and does not verify vault reads or rendered answers. The
`rules --rules-reference` option is only a small synthetic exact-phrase
reference for development. It is explicitly not the production rules
baseline. No locked predictions have been created or run.

## Fixture format

Each JSONL fixture has an id, split, family, per-prompt `template_family` key,
prompt text, eligibility flag, and expected structured route (`intent`,
`period`, `metric`). An eligible response is correct only when all three route
arguments match. For a negative, the expected label is a non-fast-path class;
it should fall back. The only accepted fast routes are `nutrition_summary`,
`meal_list`, and `latest_meal`.

Latest-meal prompts request saved calorie and protein estimates; the route is
argument-free, so the expected metric is `none`. Rolling seven-day prompts say
explicitly that today's entries are included. Five standalone meal-drafting
requests in each split ensure logging language is tested as a fallback.

## Adapter prediction format

Create one JSONL row for every fixture id, including failed, timed-out, or
abstained cases. An error row preserves a failure in the report:

```json
{"id":"dev-e-nutrition-01","error":"adapter timeout"}
```

Successful predictions use this shape. `latency_ms` is optional and must be an
actual duration measured by the adapter for the clearly documented operation.
The harness never synthesizes latency.

```json
{"id":"dev-e-nutrition-01","prediction":{"intent":"nutrition_summary","period":"today","metric":"protein","confidence":0.93,"latency_ms":42.1}}
```

Predictions must contain every fixture id exactly once. Confidence must be
finite and within 0–1. The `--threshold` is a frozen acceptance cutoff; choose
it with development data and adapter calibration only, write down the choice,
then keep the same value for locked scoring. Do not edit prompts or the
threshold in response to locked results; a retest needs a new independently
curated locked set.

## Commands

From repository root, using a Python 3 runtime:

```powershell
python tools/decision_model/benchmark.py --split dev --backend rules --rules-reference
python tools/decision_model/benchmark.py --split dev --backend von --predictions path/to/von-dev.jsonl --metadata path/to/von-dev.metadata.json --threshold 0.8
python tools/decision_model/benchmark.py --split locked --backend von --predictions path/to/frozen-von-locked.jsonl --metadata path/to/von-locked.metadata.json --threshold 0.8
python tools/decision_model/benchmark.py --split dev --backend all --predictions-dir .local-state/decision-model --threshold 0.8
python -m unittest discover -s tools/decision_model -v
```

Batch mode looks for `rules-dev.jsonl`, `von-dev.jsonl`, and `gemma-dev.jsonl`
(or the corresponding split names). Backends without files are marked
`pending`; no model is started. `--rules-reference` can replace the rules file
in dev batch mode and keeps that result explicitly labeled synthetic.

Use only `dev` to develop the adapter and select the threshold. Freeze the
model/code/weights/tokenizer/calibration, predictions and threshold before
scoring `locked`. Keep prediction and report files outside version control
(for example under `.local-state/decision-model/`, which is gitignored). The
harness reports accepted route accuracy, fast-path coverage, negative fast
routes, confident errors, Wilson 95% interval for accepted error rate, and
sample counts. Latency percentiles include only supplied timing samples and
report their count; they are not comparable unless measurement boundaries and
device conditions match.

The benchmark covers routing only. It does not establish factual correctness
of host calculations, answer rendering, safety handling, privacy/network
behavior, Android export parity, memory use, or user-visible end-to-end speed.
Imported results require a JSON metadata file with backend and split plus the
frozen adapter code, model, weights, tokenizer, calibration revisions,
threshold, fixture SHA-256, and timing scope. Keep this metadata beside predictions. Reports
include the prediction SHA-256 so a later experiment record can identify the
exact file. Explicitly identify inapplicable revisions rather than omitting
them.

## Pinned Von desktop experiment

The local development adapter is `run_von.py`. It retains fixture `id` and
`text` but sends only text to pinned Von Choice inference; `expected`, `eligible` and other labels
are projected away before the model call. The default `cautious` profile is
preserved. `concise` is a second, explicitly named development profile. Both
include the seven allowed read routes and an abstain option. They are prompt
experiments, not production policies. `confidence` in prediction rows is the
calibrated probability assigned to the winning choice and is the benchmark's
threshold input; `von_confidence` separately records the SDK's ChoiceAnswer
confidence value. The complete option probability map is retained for
development analysis.

To reproduce from the repository root in PowerShell, use the pinned source,
an isolated environment, and a separate network-enabled artifact preparation
step. The versions below are the ones recorded for this desktop run. PyTorch
must come from the CPU wheel index; no Android production dependencies change.

```powershell
rtk proxy C:/Users/ray/AppData/Local/hermes/bin/uv.EXE --cache-dir .local-state/decision-model/uv-cache venv --python C:/Users/ray/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe .local-state/decision-model/venv
rtk git clone https://github.com/wfzyx/von.git .local-state/decision-model/von-source
rtk git -C .local-state/decision-model/von-source checkout --detach 9319ced779f8232ab1d8d3e9c69cdc52ddce77ec
rtk proxy C:/Users/ray/AppData/Local/hermes/bin/uv.EXE --cache-dir .local-state/decision-model/uv-cache pip install --python .local-state/decision-model/venv/Scripts/python.exe --index-url https://download.pytorch.org/whl/cpu torch==2.14.1+cpu
rtk proxy C:/Users/ray/AppData/Local/hermes/bin/uv.EXE --cache-dir .local-state/decision-model/uv-cache pip install --python .local-state/decision-model/venv/Scripts/python.exe .local-state/decision-model/von-source transformers==5.19.0 onnx==1.23.2 onnxruntime==1.31.0 onnxscript==0.7.2 huggingface-hub==2.2.0 tokenizers==0.23.3 numpy==2.5.4
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/prepare_von.py
```

The preparation command downloads the public model snapshot at
`wfzyx/von@498ceba33390b32cfefaab6422ec380318ba9b99` and writes file checksums
to the ignored experiment directory. It requires network access. After that,
the adapter and exporter check the pinned files locally and set library offline
flags for inference and export. This does not independently prove no network
traffic. The source checkout must remain
clean at the pinned commit.

Run the two frozen prompt profiles separately; output, metadata and reports
stay under the ignored `.local-state` directory:

```powershell
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/run_von.py --split dev --profile cautious --threshold 0.8 --output .local-state/decision-model/von-cautious-dev.jsonl --metadata .local-state/decision-model/von-cautious-dev.metadata.json
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/benchmark.py --split dev --backend von --predictions .local-state/decision-model/von-cautious-dev.jsonl --metadata .local-state/decision-model/von-cautious-dev.metadata.json --threshold 0.8 --output .local-state/decision-model/von-cautious-dev.report.json
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/run_von.py --split dev --profile concise --threshold 0.8 --output .local-state/decision-model/von-concise-dev.jsonl --metadata .local-state/decision-model/von-concise-dev.metadata.json
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/benchmark.py --split dev --backend von --predictions .local-state/decision-model/von-concise-dev.jsonl --metadata .local-state/decision-model/von-concise-dev.metadata.json --threshold 0.8 --output .local-state/decision-model/von-concise-dev.report.json
```

The recorded CPU run produced 120 rows with no runtime errors for each profile.
At threshold 0.8, `cautious` accepted 3 of 60 eligible prompts, all correct,
with no negative fast routes; median and p95 inference latency were 554 ms and
595 ms. `concise` had 42 correct eligible routes out of 60, 59 accepted routes,
17 accepted errors (14 from negative prompts and 3 wrong eligible routes), and
305.578 ms median / 394.553 ms p95 latency. The concise profile is faster and
covers more eligible prompts, while its 14 negative accepts make it unsafe as
a direct fast-path gate at this cutoff. Do not interpret it as a production
go decision. The run details and artifact provenance are recorded in
[`decision-model-desktop-results.md`](../../docs/decision-model-desktop-results.md).

Export and run the bounded desktop ONNX parity check with the same isolated
environment. The command is local and uses the pinned manifest; it does not
download weights:

```powershell
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/export_von.py --threads 4 --opset 18
```

The prototype produced an ONNX artifact of about 1.582 GB and matched the
PyTorch desktop reference on three parity cases. This is narrow export evidence:
it does not verify Android operator support, tokenizer portability, mobile
latency, RAM fit beside Gemma, quantization quality, battery, or phone behavior.
There is no Android integration or installation in this experiment. No locked
prompts were scored; do not tune either profile against them. The 120-item
development set is small and synthetic, and its threshold results are not a
substitute for semantic review, host validation, safe fallback, or physical
end-to-end measurements.
