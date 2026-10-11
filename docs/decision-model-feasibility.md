# Von export feasibility checkpoint

**2026-10-11 — desktop prototype completed.** Full-model ONNX export and
desktop parity passed; Android integration and acceleration remain unverified.
Measured routing quality does not support integrating either tested prompt
profile. See [desktop results](decision-model-desktop-results.md) and the
[experiment plan](decision-model-test-plan.md). The source-inspection findings
below describe the initial checkpoint; the linked results record subsequent
downloads, isolated development dependencies and actual inference/export.

## Artifact and runtime findings

The [Hugging Face tree](https://huggingface.co/wfzyx/von/tree/main) inspected on
this date reports approximately 3.16 GB in total:

| File | Published approximate size | Role |
| --- | --- | --- |
| `model.safetensors` | 1.58 GB | Encoder checkpoint |
| `option_marker.pt` | 1.58 GB | Complete option-marker model state |
| `tokenizer.json` | 3.58 MB | Exact text tokenization |
| `tokenizer_config.json` | 571 bytes | Tokenizer settings |
| `config.json` | 6.33 KB | Model configuration |
| `marker_calibration.json` | 1.46 KB | Decision calibration/settings |

The [model card](https://huggingface.co/wfzyx/von) and repository identify
Apache-2.0 licensing and a 395M ModernBERT model. The inspected HF tree revision
is `498ceba33390b32cfefaab6422ec380318ba9b99`; its weight/calibration links
reference release revision `5df8185a4f2327ad0a7cd117cc4f701ac557b9ae`.
These are repository revision IDs, **not downloaded-file SHA-256 checksums**.
Freeze and verify all actual file checksums before running a benchmark.

The [backend source](https://raw.githubusercontent.com/wfzyx/von/master/src/von/backends/option_marker_backend.py)
constructs a model using the encoder checkpoint and then strictly loads the
option-marker state dictionary. Thus the published loader uses both large
files; source inspection suggests duplicated encoder parameters on disk.
The subsequent desktop export consolidated parameters into a 1.58 GB graph;
see the results for its hash and tested parity scope.
Do not describe 3.16 GB as confirmed resident RAM or as the eventual mobile size.

The inspected [package metadata](https://raw.githubusercontent.com/wfzyx/von/master/pyproject.toml)
reports `von-sdk` 1.3.7, Python >=3.12, PyTorch >=2 and Transformers >=5.
The bundled workspace Python lacks PyTorch, Transformers, ONNX, ONNX Runtime
and huggingface_hub. A separate development environment is needed for inference;
the synthetic scoring harness can run with the standard library.

Read-only `git ls-remote` resolved the current GitHub `master` revision to
`9319ced779f8232ab1d8d3e9c69cdc52ddce77ec`. The initial sandboxed command failed
before connecting; the network-enabled read succeeded. Use this immutable
revision for the prototype. The raw source inspection links above point to
mutable `master`; the source snapshot still needs fetching and checksum
verification alongside the actual weights before a reproducible model run.

## What must survive export

The [engine](https://raw.githubusercontent.com/wfzyx/von/master/src/von/engine.py)
selects the option-marker backend. The
[model implementation](https://raw.githubusercontent.com/wfzyx/von/master/src/von/models/option_marker.py)
packs question/state text and marker-prefixed option descriptions. It scores
the encoder's option-marker hidden states with a normalization/MLP head;
the backend applies calibration and softmax.

The independent-options configuration changes full/sliding attention masks and
position IDs. Packing, truncation, tokenizer special tokens, marker locations,
neutralization and digit splitting must match the pinned reference. The host
may construct masks, but must demonstrate parity with the published code.

The existing OpenVINO helpers offer an encoder conversion shape with explicit
input IDs, masks and position IDs. They do not establish ONNX operator support,
mobile execution or full-model parity. Exporting a generic ModernBERT encoder
alone loses the decision head and is not a Von benchmark.

## Original bounded prototype plan

The linked desktop results now complete download, development inference and
three-case export parity. Padding/truncation parity and Android execution are
still pending; neither tested routing profile warrants integration yet.

1. Fetch and verify the selected immutable source and artifact pins; create a development-only
   environment without changing Android production dependencies.
2. Run short fictional development fixtures through pinned PyTorch. Preserve
   raw logits/probabilities in synthetic-only benchmark output.
3. Export the encoder plus marker-gather/scoring head with a small bounded set
   of shapes. Keep mask preparation explicit if tracing Python loops would
   otherwise freeze input-dependent behavior.
4. Execute the graph with desktop ONNX Runtime CPU. Check selected-choice and
   probability parity under the thresholds in the plan, including varied
   lengths, padding, option order and truncation.
5. Inspect operators and memory before trying Android ARM. Quantization is a
   separate parity/quality check, not an assumed safe optimization.

[ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/) supports
Android Java/C/C++ deployment generally. No evidence yet establishes that this
specific graph is supported, fast enough, or fits alongside resident Gemma.
The unique FP32 parameters alone are approximately 1.58 GB; hypothetical FP16
or INT8 weight sizes omit activations/runtime overhead and are not measured.

If export needs a substantial attention/runtime rewrite, stop the hackathon
experiment and retain the rules/current-assistant result. No source-level
claim here closes the phone, privacy, factual-quality or speed gates.

## Production seam identified during inspection

`ModelLifecycle.runPrivateAgentTurn` currently calculates the host's required
tool, then still sends a native Gemma message and may retry its tool selection.
The experimental bypass belongs before that generation step, while preserving
the current request identity, lock/cancellation checks, fresh bounded read,
deterministic response and factual audit. This is an integration observation,
not an implemented bypass. The Python reference rules are not the Kotlin policy.
