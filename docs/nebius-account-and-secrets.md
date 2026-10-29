# Nebius account, secrets and member access

## Hosted judge demo using operator credits - direction, 2026-10-27

Member proposes using the reported 25 EUR promotional balance for minimal live
tests, recording and subsequent judge evaluation. Prefer a downloadable prepared
APK connecting to an operator-hosted HTTPS broker, with the operator's provider
key held only by that broker. The judge should not need a computer, ADB or their
own provider credentials for this primary path. Self-run judge credentials below
remain a reproducibility option. No deployment or inference was performed by
recording this direction, and no unbounded spend is authorized.

Before execution, verify actual currency/balance, Token Factory applicability
and expiry in the account. Current Builder Program terms advertise USD 25 and
90-day expiry from issuance; do not assume this account has identical terms.
Judge access must remain functional through December 15, 2026. Preserve most
credits for evaluation; establish an enforceable small initial test budget,
durable global/per-client quotas and controlled demo enrollment. Hosting must
preserve quota/replay state across restart. Do not embed the provider key or a
shared privileged broker credential in the public APK. Fictional sample data
remains isolated from real member records. Deployment, Android integration and
actual live cost/compatibility verification remain open.

## Judge-owned credentials - approved requirement, 2026-10-27

The member requires a judge to be able to supply their own Nebius Token Factory
API key and run the fictional-data demo from the public repository. Judge access
must not depend on the author's key, credits, machine or private configuration.
For this self-run demo the judge operates their own local broker; the key stays
in that broker process, outside the APK, repository, chat and logs. This is the
demo evaluator setup, distinct from the operator-funded member service below.

Required delivery, not yet implemented:

- Document prerequisites and exact commands in the root/broker README, with
  masked key entry, explicit endpoint/model selection and no committed secrets.
- Provide a no-network configuration check followed by an explicitly enabled
  single live sample request. Keep the existing synthetic default available.
- Ship invented English sample data and expected outcomes. Reject invalid
  configuration with actionable sanitized errors; document unavailable models,
  authentication/credit failures, timeout and invalid model output.
- Report actual execution mode and safe validation/latency/usage evidence so a
  synthetic response cannot be mistaken for live Nemotron inference.
- Verify from a fresh checkout using only the documented steps and the
  evaluator's credentials. No author secret or pre-existing local state may be
  required. Full reproducible demo acceptance includes the intended visible
  sample experience; a successful desktop probe alone does not prove Android
  integration or satisfy the separate working-demo delivery gate.

The evaluator needs available account credits/billing and an accessible model;
those provider prerequisites must be explicit. This requirement does not enable
real member-data transfer or public hosting.

Status: setup checkpoint reached, 2026-10-03. The provider adapter passes fake
localhost tests; no real account, key, model, price cap or live request has been
verified. The broker CLI still runs synthetic mode even if provider variables
are present. Account setup precedes a separately enabled fictional-data check.

Demo scope update, 2026-10-04: the member chose English-only model testing and
video. The first live probe uses fictional English requests with a local broker;
no paid broker VM is needed for those tests/video. The member reports $30 Nebius
credits; verify they apply to Token Factory, their expiry, the selected model
and billing/spending controls before inference. Key, endpoint and model were
absent from the inspected broker process environment; account configuration
remains the next prerequisite. Existing Spanish support is not removed.

## Who needs an account

Members do **not** need their own Nebius account or API key in the chosen design.
The Metria operator owns the Nebius account, pays its inference bill and operates
a hosted broker. The store app contacts that broker with its own revocable
installation credential. The broker uses its provider key to make the selected
inference call. No provider key belongs in Android, build settings, resources,
Git, member settings or a downloadable config file.

Publishing the APK does not provision a cloud service. Store readiness requires
an operated HTTPS endpoint, secure remote installation enrollment/revocation,
abuse and cost controls, processor/privacy disclosures, and support. Local
operator provisioning is a development mechanism, not store enrollment. The
no-account offline core stays available. No member subscription or Metria user
account has been introduced by this work. Funding/access rules (free capped
use, paid tier, etc.) need a separate product decision before a public service.
Provisioning unlimited installation credentials cannot be the public enrollment
policy; reinstall abuse, judge access and future credential renewal need design.

Member path after that work: choose online assistance, write a question, select
summaries and period, review exact disclosure, Send online. The app sends only
the approved request to the broker, which calls Nebius and returns validated
selection. The phone renders facts from its submitted snapshot. Local features
continue offline when online assistance is disabled/unavailable. No member
environment variables or developer setup is required.

## Repository and runtime configuration

The only committed template is `broker/.env.example`, containing empty key/model
fields and a public default endpoint. It is documentation, not an automatically
loaded environment file. `.env`, `.env.*` (except that explicit template) and
`.local-state/` are ignored. Never put a real key in the template or commit a
filled config file. Git ignore is an accidental-add guard, not secret storage.

| Variable | Location and purpose |
| --- | --- |
| `NEBIUS_API_KEY` | Secret in the broker process environment; never sent to the phone |
| `NEBIUS_BASE_URL` | Nonsecret approved HTTPS regional/base endpoint |
| `NEBIUS_MODEL` | Nonsecret exact configured NVIDIA model available in that account/region |

For local development, supply the key through a masked prompt for the current
broker process. Do not put its literal value in a command, shell history, chat,
screenshots or logs. For hosting, choose a managed secret store or protected
runtime secret injection, with least-privilege access and rotation. Environment
variables are a delivery mechanism, not encryption or a public config endpoint.
Loading the key alone never enables cloud fallback or background inference.
Server-side key rotation should not require an app update.

## Account setup now needed

1. Create/access the operator's [Nebius Token Factory account](https://tokenfactory.nebius.com/),
   following the [official quickstart](https://docs.tokenfactory.nebius.com/quickstart).
2. Confirm hackathon credit/billing availability and a low enforceable spending
   limit. Our quota units are not a currency cap. If the account lacks a hard
   spending control, define an independent enforceable cap before paid calls.
3. Verify which NVIDIA model is available and its regional endpoint, price,
   token/structured-output behavior and terms. No model name has been frozen
   from an unverified fictional fixture. The adapter currently permits only the
   documented default and US-central endpoint; other regions need verification.
4. Generate a dedicated development provider key. Keep it in your local secret
   system; do not send it in chat. Use a separate deployment credential later.
5. Configure only the broker process, then run an offline configuration check.
   This checks syntax/allowlist, not account authorization or model availability.
6. After those prerequisites, explicitly enable a single fictional-data live
   check and record behavior/latency/usage. No real member data is included.

Example offline check in PowerShell, from the repository root, after choosing
the real model. The real key is entered only in the masked prompt:

```powershell
$metriaBrokerPython = 'C:\Users\ray\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
$env:NEBIUS_BASE_URL = 'https://api.tokenfactory.nebius.com/v1/'
$env:NEBIUS_MODEL = '<verified NVIDIA model ID>'
$metriaKey = Read-Host 'Nebius development API/ key' -AsSecureString
$metriaKeyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($metriaKey)
try {
    $env:NEBIUS_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($metriaKeyPointer)
    rtk proxy $metriaBrokerPython broker/provider.py
} finally {
    $env:NEBIUS_API_KEY = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($metriaKeyPointer)
    $metriaKey.Dispose()
}
```

This example removes the transient environment value after the check and does
not make a request. A live launch command will be added only after verification.
Actual hosting logging/retention, provider deletion terms, operator identity,
secure enrollment/TLS and phone consent remain open before real-data use.
