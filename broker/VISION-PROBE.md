# Super VL fictional meal experiment

Desktop preparation only; Android still uses its existing runtime. This does not
enable real-photo uploads or the hosted judge path. No new dependency is needed.

From the repository root in PowerShell, with the existing `$metriaPython`:

```powershell
& $metriaPython broker/vision_probe.py --prepare
& $metriaPython broker/vision_probe.py --live --account-verified
```

`--prepare` saves the exact JSON request and a copy of the synthetic image without
reading a key, contacting Nebius, or consuming a live attempt. `--live` asks for the
key with hidden input (or uses `NEBIUS_API_KEY`). No key argument or key file. The
model and endpoint are fixed to `nvidia/Nemotron-3.5-Super-VL` and
`https://api.tokenfactory.nebius.com/v1/chat/completions`; the existing text-model
environment overrides cannot silently change this experiment.

`--account-verified` acknowledges checking account credits/expiry, model access
and spending. Exactly one POST, no retries, 60-second transport deadline (OS DNS
resolution can exceed the deadline), 4,096 maximum output tokens. The private
SQLite ledger at `.local-state/nebius-vision-probe` limits this experiment to ten
admissions total and ten per UTC day across restarts. It is separate from the text
probe ledger. Failed dispatched calls count; never delete/reset the ledger to retry.
This is a request limit, not a guaranteed monetary cap.

## Captured evidence

Console `debug_file` points to an owner-restricted, Git-ignored JSON record in
`.local-state/nebius-debug-responses`. A same-stem `.png` is the exact image sent.
Capture is mandatory and prepared before admission/network access. The shared
capture limit is 20 JSON records (including offline preparations and text runs).
No automatic deletion or upload. Stop probes before manually removing selected
old JSON/PNG pairs to make room; preserve quota databases. Existing capture-lock
recovery rules in `README.md` apply.

Each record contains:

- Exact serialized JSON request including system/user text, model, image data URI
  and parameters; endpoint, fixture hash, provenance and generation prompt.
- Image bytes in the paired PNG; these match the embedded request data URI.
- Provider body as readable text and base64, even on HTTP errors or invalid food
  drafts, bounded to 65,536 bytes with explicit truncation. Truncated captures
  are rejected. Transport failures before a response cannot supply a body.
- HTTP status, UTC creation time, local request ID, outcome/validation diagnostics,
  latency and reported usage, including any additional usage details from Nebius.
- Approximate uncached USD cost from reported prompt/output tokens at $0.30/$0.90
  per million, published by Nebius and checked 2026-10-27. Image tokens are included
  in provider-reported prompt tokens; no local guess at image tokenization. Missing
  usage means unknown cost, not zero. Actual invoices, caching and taxes can differ.

Credentials/auth headers/environment are never captured. Known echoed provider
keys are redacted. Responses are untrusted text; do not execute their contents.

`validated` means a structurally accepted draft with usage, not accurate food
recognition. Inspect the full response: does it see all four foods, avoid invented
ingredients, disclose lack of scale/hidden oil, and keep review required? There is
no measured gram ground truth. Calories/macros are intentionally excluded; future
integration must retain deterministic catalog calculations and explicit review.

The key exists only in the operator's terminal; preparation/tests do not establish
that this account can access Super VL. Read the captured HTTP body before making
another paid attempt if the provider rejects the call.

Sources: [Nebius model announcement and prices](https://nebius.com/blog/posts/three-new-open-weight-models-from-us-companies-give-builders-more-choice),
[live model catalog](https://tokenfactory.nebius.com/api/public/models_info).
