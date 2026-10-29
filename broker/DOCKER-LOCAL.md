# Run the broker on your PC with Docker

Demo decision: no rented server. The broker and temporary HTTPS tunnel run on
your PC. **Nemotron still runs on Nebius Token Factory and consumes your API
credits.** For fully offline use, choose Gemma in the app; no broker is needed.

## Windows quick start

Prerequisites: Docker Desktop running with **Linux containers**, PowerShell,
internet access, and your own Nebius Token Factory API key with Super VL access.
No host Python, cloudflared installation, GPU, USB or router configuration needed.
The Android APK must already be installed. APK/repository publication is separate.

From the repository root, first verify connectivity without using a key or making
any inference request:

```powershell
.\broker\local-docker.ps1 -Action Check
```

The first build downloads Python and the pinned official cloudflared 2026.10.5
binary; its SHA-256 is checked during build and again at startup. The command
creates a temporary public tunnel, checks HTTPS and unauthorized-request
rejection, then closes it. Its URL is temporary and cannot be reused afterward.

**Before switching from the old launcher, stop its terminal with Ctrl+C.** Do not
run Python and Docker launchers together. Docker mounts the existing repository's
`.local-state` directory: it preserves your pass, installation binding, quota
ledger, cost metadata and prior fictional captures. It does not issue fresh quota
when state already exists. Do not delete, replace, copy back an old version of, or
run another funded instance from a duplicate of this directory.

To start the app rehearsal with complete responses/errors saved for fictional
inputs only:

```powershell
.\broker\local-docker.ps1 -CaptureFictionalResponses
```

Enter the Nebius key at the hidden prompt. The script intentionally does not pass
the host's key environment variable into Docker: the key lives in the broker
process, not Docker configuration, image layers or command arguments. Cloudflared
does not inherit it. Do not enter the key in the app or in chat.

1. Keep the terminal open. Wait for **HTTPS broker verified** and copy that URL.
2. Copy your private pass from `.local-state/nebius-app-test/app-test.pass` locally
   (for example, open it in Notepad). Never show it in the recording or commit it.
3. In Metria Settings, connect Nemotron with the URL and pass. When the tunnel URL
   changes, reconnect using the same pass and app installation. Ordinary Gemma /
   Nemotron switching preserves the connection: do not choose Disconnect.
4. Use a fresh conversation and fictional records/photos only. Analyze
   `broker/fixtures/synthetic-meal.png`, review the draft, and save it only in a
   test installation containing fictional records. Then ask:
   **How many calories were in my latest meal?**
5. Inspect the capture filename printed after each request and the cost estimate.
   Captures are in `.local-state/nebius-debug-responses`; quotas remain separate.
6. Stop with Ctrl+C. The public URL stops working. The PC must remain running for
   someone else to use this broker. A judge can run their own clone with their own
   API key; they do not need access to your PC or your credits.

If Windows blocks the script, use a one-command, process-only policy override:
`powershell -NoProfile -ExecutionPolicy Bypass -File .\broker\local-docker.ps1 -Action Check`.
Replace `-Action Check` with `-CaptureFictionalResponses` for the fictional test.
No machine-wide execution policy change is required.

For normal content-free operation, omit the capture flag:

```powershell
.\broker\local-docker.ps1
```

Usage totals (another terminal; no inference or key needed):

```powershell
.\broker\local-docker.ps1 -Action Usage
```

## Limits, privacy and troubleshooting

- If PowerShell ends with `Local broker command failed`, inspect the preceding
  `Local tunnel failed [code]` line. It now reports only fixed diagnostic codes
  (key format, local state permissions, DNS/HTTPS or startup failure), never
  arbitrary exception text. The trailing PowerShell error alone is not a cause.
  A successful `-Action Check` closes its disposable tunnel; start normally and
  use that new URL in the app. Checks never send inference requests.
- Same rehearsal limits: 10 attempts/pass, 20 global/day and lifetime, 3/minute,
  one active request per installation. A failed dispatched call consumes an
  attempt. These are attempt limits, not an exact currency cap.
- The first pass expires after seven days; restarts neither extend it nor reset
  usage. If expired/exhausted, inspect state and explicitly plan a new allowance;
  do not erase the ledger. This is not an unattended judging deployment.
- No origin port is published by Docker. Broker and cloudflared communicate over
  loopback in the same container. Cloudflare terminates public HTTPS and can
  process traffic; the tunnel is for temporary tests.
- An internet connection is needed for Docker downloads, the tunnel and Nebius.
  If Wi-Fi blocks trycloudflare.com DNS, try mobile data on the phone. TLS checks
  remain enabled. An assigned URL is not proof of a healthy broker: wait for the
  verified message.
- Capture is explicit, bounded (20 JSON files, 64 KiB bodies), and may contain
  fictional model reasoning/output. It blocks new inference if unavailable/full.
  Never enable it for real health information or real prior chat context.
- The bind mount deliberately reuses the same private directory on Windows. Keep
  Docker Desktop access restricted and never publish `.local-state`. Removing the
  disposable container leaves this directory intact. No Docker volume pruning or
  data deletion is part of this workflow.
- Start prepares ownership of only the existing `nebius-app-test` and
  `nebius-debug-responses` directories in a short-lived container with only
  CHOWN capability. This fixes Windows-created folders appearing owned by root
  inside Docker. It does not recurse, read or replace files, or loosen Windows
  ACLs. The broker still runs as UID 10001 and applies private directory modes.
- If `metria-local` already exists, inspect `docker ps -a --filter name=metria-local`.
  Stop the running rehearsal with `docker stop metria-local` before starting
  another. Do not terminate someone else's service or remove their state.
- Code changes require rebuilding and restarting; the helper builds before Start
  and Check, using Docker cache. The URL changes on each restart.

The helper targets Windows Docker Desktop. The image has Linux amd64 and arm64
binary hashes, but only the platform actually exercised is verified. Linux host
bind mounts need owner permissions for container UID 10001; do not loosen directory
permissions to work around this. See [the original Python workflow](LOCAL-APP-TEST.md)
for detailed capture/cost semantics, or [the image source](Dockerfile.local).
