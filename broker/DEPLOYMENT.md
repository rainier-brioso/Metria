# Judge broker hosting runbook

Nebius-specific investigation (2026-10-28): see
[`docs/nebius-serverless-hosting-investigation.md`](../docs/nebius-serverless-hosting-investigation.md).
CPU containers, HTTPS and secret injection are documented. Endpoint internal
storage is deleted on stop: a separate shared filesystem and single-writer
SQLite acceptance are needed. Published hackathon credits cover Token Factory,
not AI Cloud hosting. No Nebius resource has been created.

The container now requires `shared/assistant-contract.json`, copied explicitly
by the Dockerfile. Build from repository root and deploy the contract together
with broker code; copying only `broker/` is no longer a complete installation.

Before hosting, rehearse the actual app through the temporary HTTPS tunnel in
[LOCAL-APP-TEST.md](LOCAL-APP-TEST.md). The broker stays on the operator PC, with
private passes, persistent quotas and content-free local cost estimates.

Status: container preparation, not a deployed public service. First verify one
live fictional request using [README.md](README.md), then choose the host and
verify this deployment boundary. Android pass entry, secure credential storage,
sample rendering and a release APK remain separate unfinished work.

Cloud integration revision (2026-10-27): `/v1/vision` and `/v1/assistant` now
exist behind the separate explicit cloud capability flag. They accept bounded
member-selected photo/text in memory, never synthetic debug capture. Android
integration is being built; deployment, final host URL and phone acceptance are
still open. Desktop Super VL synthetic-image inference has passed once.

## Required host capabilities

- Managed HTTPS reverse proxy, secret injection and request/header/body/rate
  limits. The application port must be reachable only by that proxy. Strip
  caller-supplied forwarding headers and set `X-Forwarded-Proto: https` yourself.
- Exactly one service replica and durable, local POSIX-compatible storage for
  `/state`. SQLite needs transactional locking and fsync; do not mount it on an
  object-storage bucket/FUSE mount or copy it between concurrent instances.
- Preserve `/state` across restart, redeploy and idle suspension. A marker and
  database must already exist before hosted startup; missing state fails closed.
  Restoring an old snapshot can roll back consumed quota: freeze admission and
  reconcile counters before restoring, or keep the service disabled.
- Disable HTTP request-body, Authorization and provider error logging, including
  APM/traces, crash collection, proxy logs and secret capture. Store metadata only.
- Verify operator identity, access/deletion policy, resource billing, credit
  expiry and maintenance coverage through December 15, 2026.

Render Free has ephemeral files and no persistent disk. It cannot safely host
this SQLite implementation unchanged. Its free Postgres expires after 30 days;
that is also insufficient for the full judging window without a different
durable solution. Do not mark hosting complete from a successful container build.
[Render limitations](https://render.com/docs/free).

Nebius **Token Factory inference endpoints** serve models; they do not run this
broker's Python code. Nebius **AI Cloud Serverless Endpoints** can run a custom
CPU container with a managed HTTPS URL (the official quickstart deploys nginx).
They are a possible host, subject to secret injection, proxy headers and durable
SQLite-compatible storage verification. Serverless applies Compute pricing for
running resources/disks, separate from Token Factory token pricing; do not
assume Token Factory promotional credits cover this hosting. No Nebius-specific
deployment is enabled or resource created by this repository.
[Endpoint quickstart](https://docs.nebius.com/serverless/quickstart/endpoints),
[billing](https://docs.nebius.com/serverless/pricing-quotas).

## Build and initialize

Build from repository root. The context allowlist copies only broker Python;
it excludes all private state, `.env` files, Android data and tests:

```powershell
rtk proxy docker build -f broker/Dockerfile -t metria-broker:judge-demo .
```

The image runs as UID/GID 10001. Provision a persistent directory/volume writable
only by that UID, then initialize a pass on it using the image's CLI. For a local
Docker named volume (initial copied `/state` ownership comes from the image):

```powershell
rtk proxy docker volume create metria-judge-state
rtk proxy docker run --rm --mount source=metria-judge-state,target=/state metria-broker:judge-demo python broker/server.py issue-pass --state-dir /state --credential-file /state/judge-01.pass --expires-at 2026-12-16T00:00:00+00:00 --max-requests 50
```

The pass stays in its private file; distribute through your authorized private
operator workflow, never logs or public assets. Do not mount private member data.
The same CLI can issue/revoke subsequent passes against that volume. A local
named volume is not proof that a hosting platform preserves or backs up data.

## Hosted process configuration

Use `python broker/hosted.py` (image default command) behind the proxy. Set:

| Variable | Value |
| --- | --- |
| `PORT` | Internal application port, default 8765 |
| `METRIA_PUBLIC_HOST` | Exact lowercase public hostname, without scheme/path |
| `METRIA_STATE_DIR` | `/state`, or verified persistent directory |
| `METRIA_PERSISTENT_STATE_CONFIRMED` | `yes`, only after checking the actual mount |
| `METRIA_LIVE` | `0` for synthetic rehearsal, `1` for real inference |
| `METRIA_ACCOUNT_VERIFIED` | `yes` required for live mode after account checks |
| `METRIA_ALLOW_CLOUD_ASSISTANT` | `yes` only to enable authenticated member photo/text; default disabled |
| `NEBIUS_API_KEY` | Hosted secret, not a literal command argument or image layer |
| `NEBIUS_BASE_URL` | Verified permitted Token Factory HTTPS endpoint |
| `NEBIUS_MODEL` | Exact account-visible NVIDIA model ID |

Hosted startup listens on `0.0.0.0:$PORT`, checks the exact Host and trusted
HTTPS-proxy header, and disables the legacy summary input endpoint. Member photo
and text endpoints are disabled unless `METRIA_ALLOW_CLOUD_ASSISTANT=yes` plus
verified live mode. `METRIA_ALLOW_CLOUD_PHOTOS` remains a compatibility alias.
Locally, use `serve --live --account-verified --allow-cloud-assistant` with the
existing private state directory; the older `--allow-cloud-photos` flag aliases
this capability. Do not
publish this plain HTTP port directly. The default localhost synthetic command
is still available separately; provider variables alone never enable live mode.

## Deployment acceptance

1. Start in synthetic mode. Check health through HTTPS and verify spoofed host,
   absent/forged credentials, invalid/oversized bodies and unknown scenarios fail.
2. Issue two private passes. Activate one, retry with the same installation UUID,
   reject another UUID, verify no extra quota. Configure a small test pass for
   exhaustion and verify rejection before any provider dispatch.
3. Restart/redeploy the host and confirm pass, replay, daily and lifetime quotas
   persist. Revoke a pass and confirm both activation and active credential fail.
4. Only after account/budget checks, inject the provider secret and opt into live
   mode. Run one fictional case, validate its selection and measure billed usage.
   Retain only sanitized technical evidence; no keys, pass codes or raw bodies.
5. Verify network timeout/error behavior and proxy logs. Configure a kill switch
   (disable live mode or stop the service), plus provider-level spending controls.
6. Exercise from the final APK on an evaluator device and record acceptance.
   This remains open; desktop/API tests cannot substitute for phone acceptance.

For the optional cloud routes, also verify explicit selected-content consent,
invalid/oversized JPEG and text rejection before provider dispatch, revoked and
exhausted pass behavior, no body/header logging at every proxy/APM layer, and
Stop/lock handling. Synthetic probe logs are not a production observability path.
Cloud choice permits outbound content only after the disclosed user action;
server-readability and network metadata must be explained in the privacy notice.

The 1,000-attempt lifetime limit bounds new admissions, not a guaranteed EUR cap.
Model prices, actual tokenization and provider billing must be checked. Requests
already sent cannot be recalled by revoking a pass or stopping the service.
