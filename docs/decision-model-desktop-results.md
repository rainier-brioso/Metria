# Von desktop experiment — 2026-10-11

**Decision: do not integrate either tested configuration into Metria yet.**
Local inference and full-model ONNX export work on this Windows desktop, but
neither prompt profile meets the proposed quality/coverage gate. This is an
English-only development experiment with fictional prompts, not a locked-test
result or an Android acceptance pass. See the [test plan](decision-model-test-plan.md).

## Measured routing results

Each run used the same 120 development prompts: 60 eligible record questions
and 60 requests that must fall back. The adapter passed only prompt text to
the model; fixture labels did not enter inference. Both runs completed with
zero runtime errors. The provisional cutoff was 0.8 on the calibrated winning
option probability, **not** the SDK's separately defined `ChoiceAnswer.confidence`.

| Profile | Correct eligible routes | All accepted routes | Incorrect accepted routes | Unsupported requests accepted | Warm median | Warm p95 |
| --- | --- | --- | --- | --- | --- | --- |
| Cautious | 3/60 (5%) | 3 | 0 | 0 | 554 ms | 595 ms |
| Concise | 42/60 (70%) | 59 | 17 | 14 | 306 ms | 395 ms |

Concise accepted 45 eligible questions, of which 42 were correctly routed.
Its accepted accuracy was 42/59 (71.2%). The proposed gate requires zero
incorrect accepted routes as well as at least 70% correct eligible coverage.
The shorter prompt trades useful coverage for unacceptable misrouting.

For example, fictional "I just ate two eggs and toast" selected today's meal
list at probability 0.9395; "What calories did I record on January 2?" selected
today's calorie total at 0.9855. Increasing the cutoff cannot recover a strong
result here: a development-only sweep of observed probability boundaries found
at most 6/60 correct eligible routes with zero observed accepted errors, at
0.9979. That post-hoc cutoff is exploratory and is not a frozen release choice.
With only six accepted cases, the Wilson 95% upper error bound is 39.0%; zero
observed errors is weak evidence. For cautious's three cases it is 56.1%.

Timings include tokenization and a single local `von.decide` call on CPU with
four Torch threads. They exclude model loading and one separate warmup call;
all 120 calls contributed to each percentile. The two sequential runs are
descriptive measurements, not a controlled paired speed study. No Gemma or
production Kotlin-rules baseline ran, so no Gemma speedup or incremental
coverage claim is established. The exact-phrase Python reference is synthetic.

## Export result

The full independent-options encoder and option-marker scorer exported to
ONNX opset 18 and passed ONNX checking plus desktop ONNX Runtime CPU parity.
The wrapper first matched the official PyTorch model exactly on the tested
cases. Exported logits then selected the same choices in three cases: sequence
lengths 32 and 254, three and four options, including reordered options.
Maximum absolute logit error was `2.2173e-5`; calibrated probability error was
`8.9407e-7`, within the configured tolerances `2e-3` and `2e-4` respectively.

The unquantized graph is **1,582,047,027 bytes** (about 1.58 GB decimal), with
embedded weights and no external-data files. Its SHA-256 is
`949c7be11a8a2225999089651c34a7dcc88567d8d0c906a67d99eb45cf5a7482`.
This removes the duplicate checkpoint files from the exported artifact; it
does not measure resident memory. Explicit full/sliding attention masks,
position IDs and marker positions remain host inputs. Tokenization, packing
and input-conditioned calibration remain host-side.

Android operator execution, tokenizer/mask/calibration parity, eight-option
routing parity, batch sizes beyond one, quantization, cold load, memory,
thermal behavior and coexistence with Gemma remain untested. The legacy Torch
exporter emitted tracing/deprecation warnings; the tested shapes passed, but
dynamic axis annotations alone do not prove arbitrary shapes work.

## Reproduction and provenance

The [runbook](../tools/decision_model/README.md) contains commands. Public model
files and a development-only virtual environment live in ignored
`.local-state/decision-model/`; no production dependency was added.

- Source: `wfzyx/von@9319ced779f8232ab1d8d3e9c69cdc52ddce77ec`.
- Model: `wfzyx/von@498ceba33390b32cfefaab6422ec380318ba9b99`.
- Download: eight files, 3,164,061,662 bytes. Manifest file hashes were computed
  locally and checked before inference/export; these identify the downloaded
  artifacts, not independently authenticated upstream checksums.
- Runtime: Torch `2.14.1+cpu`, Transformers `5.19.0`, Von SDK `1.3.7`,
  ONNX `1.23.2`, ONNX Runtime `1.31.0`, tokenizers `0.23.3`.
- Development fixture SHA-256:
  `f6360ff0a795d8b95b0f62e0e35f92527264c64ad406bdaf4cc7b874b74bd233`.
- Artifact manifest SHA-256:
  `940b1b3fc43b75acdfea8b53702f8b2d678c7a57f0516c25155d8027093a4f68`.
- Cautious prediction SHA-256:
  `d8c48cd66d9f9b94b4d35136bc4e534227c1be7ed8ce94869843421b556f4618`.
  Original adapter SHA-256:
  `b7e3e6dea61c7f644b3f306823a98121c5753632e75386ac9380ac9ac576e940`.
- Concise prediction SHA-256:
  `9148cfeff36f2ec1a50bfdddcb6cf51deb0c418a582829ab4691e779908b897e`.
  Adapter SHA-256:
  `9dfdecfdea40331cbd51e7e8d319f7cfdf2aebb1b51b733f8e65e981a4c53a3c`.

Preserved evidence includes `von-dev-cautious.jsonl`, its metadata/report,
`run_von-v1.py` (original cautious adapter), `von-dev-concise.jsonl`, its
metadata/report, `artifact-manifest.json`, `runtime-freeze.txt`, and
`von-onnx/export-report.json`. Cautious's original metadata predates the added
profile and separate SDK-confidence fields; its saved source identifies the
same winning-probability threshold semantics. Concise records a clean source
checkout explicitly. The export report records graph operators and file hashes.

HF/Transformers offline settings were enabled during inference and chain
subcalls disabled. This is not OS-level network isolation or an independently
instrumented privacy pass. Only public weights and fictional text were used;
the phone and member vault were not accessed. No locked predictions ran.

## Next bounded step

Prefer deterministic reads for the hackathon while retaining Gemma fallback.
Before spending time on Android integration, test a revised decision approach
on development data with explicit unsupported-date, logging and mixed-intent
rejection. Any new model should repeat the same quality gate. Freeze a promising
configuration and complete semantic independence review of the reserved pool
before locked scoring. Only after that should the full Android execution and
end-to-end speed/memory tests begin. Fine-tuning and another candidate were not
performed in this experiment.
