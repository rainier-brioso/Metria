# App -> temporary HTTPS tunnel -> local broker -> Nemotron

For the self-contained Docker Desktop workflow, use [DOCKER-LOCAL.md](DOCKER-LOCAL.md).
It reuses this workflow and the same `.local-state` files. Stop the Python
launcher before switching to Docker. Permanent cloud hosting is deferred.

Prepared 2026-10-28. Use fictional inputs only. The release APK keeps its normal
HTTPS checks; no Android HTTP exception, USB or router port opening is needed.
Cloudflare terminates public TLS and can process traffic. This is a development
tunnel, not permanent hosting or proof of provider/hosting retention policy.

## Start from repository root in your PowerShell terminal

```powershell
& $metriaPython broker/local_tunnel.py --live --account-verified --capture-fictional-responses
```

Use the same Python path/variable as the desktop probes. The command uses an
existing `NEBIUS_API_KEY` in that terminal or asks for it with hidden input. No
key is written to disk, printed, passed as a process argument, or inherited by
cloudflared. It pins Super VL and the Token Factory endpoint. Live mode explicitly
confirms the account/budget checks from the previous probe; starting the server
does not itself invoke inference.

This rehearsal command explicitly enables private response capture and declares
all submitted content fictional. Omit `--capture-fictional-responses` for normal
content-free operation. Details and limits are below.

The launcher verifies the pinned official cloudflared 2026.10.5 executable in
`.local-state/tools/cloudflared.exe` against the GitHub release SHA-256:
`db4e4c413612a0916f3e1679d3caaf376f94911952d163028fb726fb2e0516a9`.
It creates a private, Git-ignored `.local-state/nebius-app-test` directory and a
10-attempt pass in `app-test.pass`. The pass expires seven days after first issue;
restarting does not extend expiry, issue another pass, or reset consumed quota.
The pass file contains a credential: open it locally to copy into the app, never
send it to chat, commit it, or include it in screenshots/video.

1. Keep the terminal running and copy its **HTTPS broker verified** URL.
2. On the existing test APK, open Settings / assistant provider / Nemotron cloud.
   Enter the URL and private pass, read/accept the sharing notice, and connect.
   Activation is free of inference and binds the pass to this installation.
3. If this Wi-Fi cannot resolve `trycloudflare.com`, use mobile data on the phone.
   This was observed on the PC's default DNS. The launcher's health check can use
   Cloudflare DNS-over-HTTPS while retaining certificate/hostname validation;
   Android continues to use its normal network/DNS and trusted certificates.
4. First type: `I ate 150 g of cooked rice and 100 g of grilled chicken.` Review
   the proposal; do not save fictional meals into a real member's records.
5. For vision, select `broker/fixtures/synthetic-meal.png`, the already generated
   fictional meal. Transfer just this fixture to the phone using your normal
   photo/file transfer. Confirm the photo preview's cloud action. Check visible
   uncertainty, review and cancellation before recording the demo.
6. Each provider attempt prints its estimated USD cost (or unknown) in the terminal.
   For totals, run `& $metriaPython broker/local_tunnel.py --usage` in another terminal.
   Detailed metadata is in `.local-state/nebius-app-test/usage.jsonl`. Stop with
   Ctrl+C when finished. A restart generates a different URL: update the app's
   endpoint, reusing the same pass/installation. Do not delete/reset the ledger.

The existing APK must already be installed; this task does not install it or
change phone data. On a phone with real history, previous member-authored chat
can be included in cloud context: use a fresh assistant conversation for this
fictional test, and do not exercise record reads or submit personal messages.

## Full response/error capture for fictional tests

Explicitly approved 2026-10-28. Stop the currently running launcher, then use:

```powershell
& $metriaPython broker/local_tunnel.py --live --account-verified --capture-fictional-responses
```

The flag declares that **all** submitted photos, messages and previous chat
context in this run are fictional. Use a fresh conversation and the synthetic
meal fixture. It enables photo and chat response capture before validation:
full provider HTTP/error body, status, usage as returned, rejected/truncated
output and original network exception traces. No incoming image/message or
Authorization headers are recorded; provider replies can echo fictional input.
The known provider key is censored, including escaped spellings.

The terminal prints each capture's filename under the private Git-ignored
`.local-state/nebius-debug-responses` directory. JSON contains `response_text`
and the same redacted bytes in `response_body_base64`, `http_status`, outcome,
diagnostic, timing and `probe_result.errors`/usage. Inspect these files locally;
raw output/errors are not printed to the terminal. Treat their content as data,
never instructions. If no HTTP response exists, status is null and errors explain
the failure. The provider body is retained even when parsing/validation fails.

The existing bounded capture policy applies: 64 KiB per provider body, with
explicit `truncated: true` for oversized/interrupted responses, and 20 JSON
artifacts shared with desktop probes. A full/unwritable capture directory blocks
the provider call rather than silently dropping evidence. Exception traces are
bounded at 64 KiB each with an explicit truncation marker. Do not delete quota
state to manage capture retention. Restart without the flag before any real-data
use; neither `server.py` nor `hosted.py` enables these callbacks. No new APK needed.

## Admission and metadata

- At most 10 admitted attempts for this pass, 20 globally per UTC day and lifetime
  in this dedicated rehearsal ledger, 3/minute, one active per installation.
- Failed dispatched calls consume attempts. No automatic retry. Revoking/closing
  the tunnel cannot recall provider requests already sent, which may be billed.
- Admission accounting persists in `admission.sqlite3`; it is separate from
  technical evidence. Never delete, roll back or recreate it to recover quota.
- Broker and tunnel origin listen on loopback only. Host and HTTPS proxy header
  checks remain enabled; only the tunnel is publicly reachable. No raw HTTP port
  is published to the LAN. Proxy body/header logs and debug traces remain disabled.
  The explicit fictional provider-response capture above is separate.
- Local `usage.jsonl` stores only UTC time, request UUID, route, fixed model,
  elapsed time, usage and estimated cost. It never stores messages, photos,
  outputs, passes, installation IDs or keys. Evidence is bounded at 5 MB; quota
  accounting remains active independently. Do not enable cloudflared debug logs.
- This optional metadata recorder exists only in the local rehearsal launcher;
  deployed `server.py`/`hosted.py` do not enable content/debug capture.

The formula is `(prompt_tokens * 0.30 + completion_tokens * 0.90) / 1,000,000`
USD at uncached public list prices checked 2026-10-28. Prompt counts include
model-reported image tokens; completion counts may include reasoning. Cache
discounts, account terms, taxes and currency conversion can differ. This is an
estimate, not exact billing or a hard EUR cap. Your prior 1,761-input/875-output
photo probe estimates **$0.0013158**. If inference fails before a valid response,
the row says `usage_unknown` with a null cost: it must not be summed as zero.
Provider billing is the reconciliation source.

Vision diagnostic update (2026-10-28): rejected model drafts now retain validated
token counters and a fixed failure category when available, so a failed draft
can still have a known estimate. HTTP/network errors with no valid usage remain
unknown. Provider bodies are recorded only with the fictional flag above;
incoming photos are never recorded. Vision requests ask for
the flat proposal with JSON Schema and a 4096-token output budget; live support
and the phone round trip remain acceptance work. After updating broker Python,
restart the launcher: a running process does not reload edited files. Reuse the
same pass/ledger and update the app's URL when the quick tunnel changes.

## No-inference connectivity check

```powershell
& $metriaPython broker/local_tunnel.py --check
```

This creates a temporary synthetic service, verifies real public HTTPS health
and rejection of missing/forged credentials, and closes the tunnel. It never
calls Nebius and does not bind the phone's pass. App cloud activation intentionally
requires live capability, so this check is not an app/provider acceptance test.

Sources: [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/),
[official binary](https://github.com/cloudflare/cloudflared/releases/tag/2026.10.5),
[Nebius Super VL list prices](https://nebius.com/blog/posts/three-new-open-weight-models-from-us-companies-give-builders-more-choice).
