# Nebius Serverless Endpoints investigation - 2026-10-28

Status: research only. No resource, secret, image publication or paid deployment
created. Member confirmed the installed cloud latest-meal calorie answer works;
this closes that local phone rehearsal case, not hosted acceptance.

## Findings

Serverless AI can run our Python container on CPU with managed HTTPS; it is a
separate product from Token Factory model inference. The intended arrangement is
Android -> CPU broker endpoint -> existing Super VL Token Factory API.

The console is https://console.nebius.com/ under Serverless AI -> Endpoints ->
Custom. Container image/port, ordinary environment variables and SecretStash
secret environment variables are supported. Keep the provider key in a secret.
Do not select a GPU model-serving template for the broker.

Sources: [endpoint quickstart](https://docs.nebius.com/serverless/quickstart/endpoints),
[endpoint management](https://docs.nebius.com/serverless/endpoints/manage).

Endpoints run on managed Compute VMs, billed while running, rather than only
when a request arrives. Stopping releases the underlying VM and its disk but
preserves endpoint configuration and managed HTTPS URL. Therefore container-local
SQLite is unsuitable: stopping would lose admission state. Mount a separate shared
filesystem at /state, with exactly one broker instance accessing its files.
Do not use an Object Storage bucket for SQLite.

Sources: [overview](https://docs.nebius.com/serverless/overview),
[stop/start](https://docs.nebius.com/serverless/maintenance/stop-start),
[billing](https://docs.nebius.com/serverless/pricing-quotas).

Nebius shared filesystems support sizes from 1 GiB, virtiofs mounting and POSIX
semantics within a single VM. This is a plausible storage option for the current
DELETE-journal, synchronous=FULL SQLite store, not a validated deployment. Still
test locking, fsync, permissions, transaction failure and stop/start persistence;
never attach a second writer VM. Current generic runbook's local-storage
requirement needs an explicit host-specific acceptance decision before enabling
persistent-state confirmation.

Source: [storage specifications and consistency](https://docs.nebius.com/compute/storage/types).

## Illustrative cost, not an account quote

Published CPU platform presets include 2vcpu-8gb. Its availability to this specific
Serverless project must be checked. For cpu-d3 (AMD), published October rates:
2 * $0.015 + 8 * $0.0045 = $0.066/hour, or $48.18 over 730 hours.
CPU-only endpoint container disks require at least 20 GiB. At the published
Network SSD rate this adds $1.42/730 hours; a 1 GiB shared filesystem adds $0.08.
Illustrative subtotal: $49.68 per 730 hours, before taxes, inference and any
network, registry or secret-service charges. The 4vcpu-16gb quickstart is larger
and should not be copied blindly. No claim that Serverless always selects the
smallest Compute preset, scales to zero automatically, or has a free tier.

Sources: [CPU presets](https://docs.nebius.com/compute/virtual-machines/types),
[current resource prices](https://docs.nebius.com/compute/resources/pricing).

The hackathon's official FAQ explicitly says promotional credits are Token
Factory credits and no separate AI Cloud grant is published. Do not apply the
member's visible Token Factory balance to the above hosting budget. A separate
Cloud promotional agreement could change this only if confirmed for the account.
The FAQ also says the rest of the app need not be hosted on AI Cloud when it calls
an NVIDIA model through Token Factory.

Source: [official hackathon FAQ](https://nebiusglobalaihackathon.devpost.com/details/faqs).

## Integration items still to verify

- Platform-level token authentication uses Authorization: Bearer, as does our
  installation credential. These are different tokens. Do not embed a global
  platform token in the APK. Candidate: leave platform token authentication off
  and enforce our broker activation/credential/quota controls, after verifying
  proxy behavior. Nebius recommends platform tokens for production; this is an
  application-authenticated alternative requiring deployment validation.
- Confirm forwarded Host/HTTPS headers, header stripping, credential forwarding,
  request size/time limits and outbound Token Factory connectivity. Do not weaken
  hosted.py's trusted-proxy checks to make an unverified setup pass.
- Initialize durable state and issue private evaluator passes; preserve state
  through stop/start and deployment replacement. Decide how to reconcile state
  before any backup restoration. Keep one writer.
- Check platform/proxy log contents; capture-fictional-responses stays disabled
  on hosted service. Current local fictional capture is a separate test mode.
- Confirm IAM editor access, project/subnet/CPU/storage quotas, exact cost and
  funded availability through the judging window. Final APK round trip remains
  mandatory after synthetic auth/quota/revocation/restart checks.

Conclusion: technically plausible within Nebius with CPU + separate filesystem,
but neither free nor covered by the published hackathon Token Factory grant.
No hosted deployment acceptance claimed.
