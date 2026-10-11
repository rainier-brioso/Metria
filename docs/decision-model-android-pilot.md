# Selected-function Android Von pilot

**2026-10-11:** debug-only runtime test built, installed and executed after
explicit member approval. All eight cases passed phone numerical parity;
three fallback cases were incorrectly routed. The target is Samsung SM-S928B,
Android 16. This checkpoint is separate from the [desktop no-go](decision-model-desktop-results.md).

The selected operation is listing today's recorded meals versus falling back.
The first phone test runs the full exported Von model on eight fixed fictional
questions in Metria's debug APK. A separate process isolates it from the Gemma
engine and vault. It does not read member records, answer chat, or change
production routing. ONNX Runtime Android 1.22.0 is a debug-only dependency;
the activity and fixtures are absent from release builds.

The fixed inputs avoid making a general Android tokenizer port a prerequisite
for the first device measurement. `prepare_android_pilot.py` uses the pinned
official Python tokenizer, packing and attention-mask code. The APK contains
the resulting tensors and reference logits, not the model weights. Each
case's temperature is frozen from its reference logits; this is not an
Android implementation of input-conditioned calibration. No arbitrary typed
question is accepted by this test.

Eight two-option cases passed desktop ONNX parity before packaging. Maximum
absolute logit error was `1.3113e-5`. At the provisional 0.8 cutoff, all four
eligible questions route correctly, but three of four fallback questions
incorrectly select today's meals. This small selected sample is not a held-out
quality result; phone execution can succeed even when routing correctness fails.
The pilot displays correctness and numerical parity separately.

## Build and test entry

Generate assets only when the pinned public source/model environment is ready:

```powershell
rtk proxy .local-state/decision-model/venv/Scripts/python.exe tools/decision_model/prepare_android_pilot.py
```

Asset SHA-256: `ce335c78b9f58c104bf41919f9505597fa95703fb55de4ddf943549ae4c4ad33`.
Eight cases, 701,069 bytes. The full model retains the desktop graph hash and
size documented in the results. It must be copied into the debug app's private
`files/decision-model/von-option-marker.onnx` after approved in-place installation.
Do not overwrite the vault, Gemma, or any existing private member file.

Run the canonical gate from `native-android/`:

```powershell
rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

After explicit installation approval, select the exact current device with
`adb devices -l`, install with `install -r`, copy only the public graph, stop
Metria, and open `app.metria/app.metria.debug.VonPilotActivity`. The member can
press **Run synthetic test**. The activity hashes the graph before opening
it, loads a CPU session with four threads, checks all case logits and winners
against the reference, and displays timings, process PSS and thermal status.
No phone identifiers belong in this document.

## Acceptance evidence to record

The complete Gradle gate passed after lifecycle fixes: 440 tests, zero failures,
zero errors, and an assembled debug APK. Independent follow-up review confirmed
process-wide run ownership and queued-cancellation cleanup. `git diff --check`
passed. APK size: 196,968,043 bytes; SHA-256:
`169940d5ad117d2170baf95d35fd7124a91cecb99c6bcea4e024b95d8f04dcf4`.
The approved `adb install -r` returned `Success`. No data clear, uninstall,
signing-key change or existing model replacement was performed. Member-record
inventory was not inspected; install success is distinct from member confirmation.

## Observed phone run

The public graph transferred successfully, was copied into the new app-private
test directory, and matched its expected SHA-256 on the phone. The temporary
transfer copy was removed. The updated debug APK installed with `install -r`;
explicit pilot launch returned `Status: ok`, `LaunchState: COLD`. The member
tapped Run. The synthetic activity saved its completed report as
`files/decision-model/pilot-result.txt`; it was read through `run-as` without
accessing the vault or other private app files.

| Measurement | Observed result |
| --- | --- |
| ONNX session creation | 2,961.320 ms |
| Eight-case report elapsed, including graph hash | 7,216.290 ms |
| Individual case inference, including input tensor creation | 304.777–323.637 ms |
| Process PSS sampled while the session was still open | 3,136,254 KB (about 2.99 GiB) |
| End thermal status | 0 (none) |
| Numerical parity | All eight passed; same winner and absolute logit error <= 0.002 |
| Eligible routes | 4/4 correct |
| Fallback cases | 1/4 correctly rejected; 3/4 incorrectly accepted |

Per-case durations in fixture order were 320.864, 315.668, 304.777, 317.938,
315.611, 310.877, 310.632 and 323.637 ms. No separate warmup was excluded.
Maximum displayed absolute logit error was 0.000014. The misroutes were a new
food description, yesterday's meals and a combined meal-list/advice request,
matching the desktop reference's failures. The model runs locally on this
Android target; these results do not establish a safe gate for real questions.

Memory is one sample, not peak allocation or a Gemma-coexistence measurement.
Elapsed time is sampled before report publication, not the moment the screen
finishes drawing. Hashing/cold load and fixed pretokenized inputs prevent direct
comparison with the existing chat path. Airplane-mode/network instrumentation,
rotation/background cancellation, repeated runs and vault inventory confirmation
remain unverified on the phone. Android CLI layout returned an unrecognized
instrumentation response. A protected first capture was black; screenshot
protection was removed only from this fictional debug screen in the updated
APK, and a temporary screen inspection confirmed the synthetic pilot state.
Member-screen protections were not changed. Raw screen captures remain outside
the repository and are deleted after inspection.

**Decision:** Android runtime feasibility passed for these fixed inputs; routing
quality failed. Keep the pilot separate from member chat. The next meaningful
step is a bounded negative-rejection strategy for the selected function before
general tokenization and host-answer integration, with an independent evaluation
set rather than tuning these eight examples into a release claim.

- Full Android test/build result and APK hash; independent review outcome.
- Approved installation and model-copy result; existing app data preserved by
  in-place installation, with member confirmation distinguished from command success.
- Model checksum, ONNX session load, per-case timings, process PSS, thermal
  status, same-choice and logit parity results on the phone.
- Background/cancel and repeat behavior; no late result after cancellation.
- Cold session setup and individual case times separately. There is no warmup
  exclusion and no Gemma speed comparison in this first smoke run.

Pending after this runtime test: free-text tokenization and calibration parity,
safe negative rejection, actual host read/validated answer integration,
Gemma coexistence and end-to-end comparison, broader cancellation/lock gates,
and an instrumented privacy check. No M4 physical gate is closed by this pilot.
