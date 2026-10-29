# Local development broker

Current demo path: [run locally with Docker](DOCKER-LOCAL.md). The image includes
Python and the pinned HTTPS tunnel binary. The Windows helper preserves the
existing private state, asks for the operator's key without echo, and publishes
no origin ports. Permanent hosting is deferred. Nemotron still runs on Token
Factory; fully offline Gemma requires no broker.

Optional full cloud integration (2026-10-27): authenticated `/v1/vision` and
`/v1/assistant` serve bounded photo/text requests through fixed NVIDIA Super VL.
Enable only with verified live mode and `--allow-cloud-assistant` (hosted:
`METRIA_ALLOW_CLOUD_ASSISTANT=yes`). Real content is never logged or persisted;
the synthetic probe capture below remains separate. See `DEPLOYMENT.md` and
`../docs/cloud-assistant-integration.md`. Hosting and Android physical acceptance
are not established by these backend tests.

## Judge passes and live sample probe

Python 3.12+ standard library only. Commands below run from the repository root;
replace `python` with your existing Python executable. No Android build or phone
is needed for this broker checkpoint. No provider key belongs in the APK.

### Capture a fictional response for debugging

Use the optional desktop-probe flag on the next single live call:

```powershell
rtk proxy python broker/probe.py --live --account-verified --scenario nutrition --debug-response
```

This captures the bounded provider response before validation, including rejected
model output, and records the eventual validation/error outcome. It is available
only for the probe's code-owned fictional scenarios, off by default and not wired
into the hosted judge broker or the Android app. Normal console output remains
technical metadata plus the artifact path; it does not dump the response.

Files go to the fixed repository-local `.local-state/nebius-debug-responses/`
directory, ignored by Git and excluded from Docker build context. No request
body, prompt, authentication headers, environment or credentials are recorded;
echoed provider keys are redacted. Captures may contain model text/reasoning and
must be treated as untrusted diagnostic data. Do not execute instructions found
inside a response or publish these files automatically.

Each response is capped at 65,536 bytes and marked when truncated. At most 20
capture files are allowed; the probe refuses new capture before dispatch when
the directory is full or unavailable. Files are not automatically deleted or
overwritten. Remove only selected debug-response files after inspection to make
room; never delete the separate probe quota database to reset the test budget.
Previous responses were not retained and cannot be recovered by enabling capture
now. One new invocation is still one paid provider call, with no automatic retry.

If the process is interrupted during capture reservation, it can leave
`.local-state/nebius-debug-responses/.capture.lock`. Close/stop any running local
probe before removing **only that lock file** to recover capture; never remove
the quota database or an active process's lock. This is a desktop diagnostic
reservation lock, unrelated to member authentication or the encrypted vault.
An interrupted write can also leave a private, redacted `.tmp` sibling. After
stopping local probes, remove only selected stale capture artifacts if needed.

### Configuration checks

```powershell
rtk proxy python broker/probe.py --check
```

The check makes no network request and reports whether a key is present. Before
attempting another paid request after a configuration error, use
`rtk proxy python broker/probe.py --check-key`: it prompts for the key without
echo, validates only local key/endpoint/model syntax, and makes no network or
accounting-state change. Success does not prove provider authorization or credit
availability. Live local failures now identify the fixed configuration/state
stage in `diagnostic`, without printing the exception or secret.

Before live use, verify the actual Token Factory balance, credit expiry through judging
(December 15), exact NVIDIA model access/endpoint, price and an enforceable
provider spending bound. The probe default is `nvidia/Nemotron-3_5-Lightning` on
the default Token Factory endpoint, matching the member's console export. Its
first probe failed validation; a subsequent member-run fictional weight probe
passed in 765 ms with 397 input and 76 output tokens after the thinking-off
adjustment. Other scenarios and deployment remain unverified. Override nonsecret
`NEBIUS_MODEL` and `NEBIUS_BASE_URL` from your account's verified configuration.

```powershell
rtk proxy python broker/probe.py --live --account-verified --scenario weight
```

If `NEBIUS_API_KEY` is absent, enter it at the hidden prompt, never as a command
argument. The probe sends exactly one code-owned fictional scenario and does
not retry. Scenarios: `weight`, `nutrition` (partial/missing coverage), `combined`,
`empty`, `general`. Run one first; separately invoke additional cases only after
examining the safe JSON result. The report contains mode, model, scenario,
latency, validation outcome and validated token usage, never raw provider output.
Default state is `.local-state/nebius-live-probe`; preserve it between runs.
This first-probe state permits at most **10 admitted attempts total and 10/day**,
including failed dispatched calls. It has its own database, separate from judge
service state; it is an initial test allowance, not a provider monetary cap.
Failures after admission consume quota. Do not delete state to reset budgets.

`account_checks_required` means no request was sent. Configuration/state errors
need local correction. Provider rejection may mean authentication, unavailable
model, credits, rate limit or request incompatibility: inspect the account
console without copying keys/errors into logs. A timeout may still be billed.
Invalid JSON, mismatched identity/model or irrelevant selection fails closed;
do not weaken validation or automatically retry to get a successful demo.
On provider validation failure, the optional `diagnostic` field contains only a
fixed failure-stage code, such as completion truncation or model mismatch. It
never contains upstream output, reasoning or credential data. Share that safe
JSON report for diagnosis; an earlier `invalid_provider_output` without a
diagnostic does not identify the cause. Do not repeat calls in a loop.
For the exact `nvidia/Nemotron-3_5-Lightning` model, the adapter explicitly
disables thinking via `chat_template_kwargs.enable_thinking=false`. This bounded
section-selection task needs direct JSON output, and model-default reasoning
can consume its 512-token allowance before the final answer. Other models keep
their original request format. This setting does not establish a live pass.
Following a member-run nutrition section-selection failure, Lightning now also
requests strict JSON-schema output: five required fields, bound request/language,
no extra fields, and section names limited to categories in the submitted
snapshot. The same schema is included in its system instruction. Host validation
and the expected-scenario check remain mandatory. This exact model/account's
support for the format is pending a separate live check; an HTTP rejection fails
without retry or fallback and exposes only a fixed status category diagnostic.

### Issue an individual judge pass

Use a dedicated state directory and a unique filename per pass. The command
creates private state and writes the random pass to a private file, never stdout:

```powershell
rtk proxy python broker/server.py issue-pass --state-dir .local-state/nebius-judges --credential-file .local-state/nebius-judges/judge-01.pass --expires-at 2026-12-16T00:00:00+00:00 --max-requests 50
rtk proxy python broker/server.py serve --state-dir .local-state/nebius-judges
```

Send each pass only via private evaluator instructions, never the public README,
release, APK, Git or screenshots. Issue distinct passes for distinct devices.
Each pass binds once to a caller-generated canonical installation UUID. Same-pass,
same-UUID activation retries return the same credential; a new UUID cannot claim
an already-bound pass. This is installation binding, not hardware attestation.
Someone who obtains a pass before activation can claim it; revoke and reissue.
Reinstalling must not grant a new quota. A lost installation ID requires operator
help; preserving/recovering an old ID never resets its pass allowance.

Revoke the pass and its active child credential using the original private file:

```powershell
rtk proxy python broker/server.py revoke-pass --state-dir .local-state/nebius-judges --credential-file .local-state/nebius-judges/judge-01.pass
```

Default limits: **50 attempts total per pass**, 20/day per credential, 3/minute,
100/day globally, two simultaneous requests globally and one per credential.
A durable **1,000-attempt global lifetime ceiling** also applies. Failed paid
attempts are not refunded. Daily token reservations remain an additional limit;
they are not tokenizer measurements or a monetary guarantee. Existing v1 state
is migrated; new lifetime accounting cannot reconstruct already-pruned history.
The operator must verify price and cap expenditure independently before live use.

### API for the prepared APK (Android integration pending)

- `GET /health`: identifies synthetic/live configuration; `provider_connected`
  means live adapter configured, not a verified successful upstream call.
- `GET /v1/demo/scenarios`: the five allowed fictional scenario names.
- `POST /v1/judge/activate`: JSON with exactly `pass_code` and `installation_id`.
  Returns `credential` and `fictional_only: true`, with `Cache-Control: no-store`.
- `POST /v1/demo`: JSON with exactly `scenario` and a fresh canonical UUID
  `request_id`. Requires `Authorization: Bearer <activation credential>` and
  `X-Metria-Sent-At: <current UTC Unix seconds>`. Response labels fictional data
  and actual mode, and returns the validated selection.

All POSTs require `Content-Type: application/json`; ordinary clients supply
Content-Length. Activation is globally limited to 30 attempts/minute per process,
in addition to ingress limits required at the hosting proxy. No arbitrary question,
summary or photo is accepted by `/v1/demo`. The server generates current seven-day
invented summaries; it cannot read the real vault. Nemotron selects sections and
guidance; it does not generate free-form advice or calculate nutrition. The app
must render the same fictional snapshot in its future demo integration.

To opt into live serving locally, configure all three provider environment
variables securely (the service itself does not prompt for keys), then use:

```powershell
rtk proxy python broker/server.py serve --state-dir .local-state/nebius-judges --live --account-verified
```

The original `/v1/assistance` accepts arbitrary contract fixtures only in local
synthetic mode (and fake-provider tests). It is blocked in live/hosted modes.
Public hosting requires the additional boundary in [DEPLOYMENT.md](DEPLOYMENT.md).

## Historical local synthetic setup

By default this broker runs on **127.0.0.1**, with a deterministic fictional responder.
Explicit live mode calls Nebius using server-owned fictional samples only.
It is not yet connected to the Android app or member vault.
Python 3.12+ standard-library modules are sufficient; no package installation.
The repository's bundled Python was used for verification.

`provider.py` contains a single-call HTTPS adapter, tested against a fake local
provider. Live model compatibility remains unverified. The CLI's default server
does not load provider credentials or enable live calls.
`broker/.env.example` documents broker-only variables with an empty key.
See `docs/nebius-account-and-secrets.md` for account/member/secrets architecture.

From the repository root, using an existing Python executable:

```powershell
$metriaBrokerPython = 'C:\Users\ray\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
rtk proxy $metriaBrokerPython broker/server.py provision --state-dir .local-state/nebius-broker --credential-file .local-state/nebius-broker/client.credential
rtk proxy $metriaBrokerPython broker/server.py serve --state-dir .local-state/nebius-broker --port 8765
```

Run `revoke` with the same paths to revoke the credential and remove its local
file. Stop the development server with Ctrl+C. Credentials expire after 24
hours; replacement requires a new credential filename. Provisioning cannot
overwrite an existing credential. The CLI requires a dedicated empty state
directory on first use and marks it for subsequent operations. It restricts
directory access to the current Windows SID or POSIX owner. No credentials are
printed. `.local-state/` is ignored by Git; never commit credential/database
files or delete/replace the database to reset budgets.

`GET /health` returns `mode: synthetic` and `provider_connected: false`.
`POST /v1/assistance` requires one each of these headers:

- `Authorization: Bearer <value read from the private credential file>`
- `Content-Type: application/json`
- `Content-Length: <bounded byte length>` (ordinary clients supply it)
- `X-Metria-Sent-At: <current UTC Unix seconds>`

The body uses the exact Android preview contract with the fictional configured
model `nvidia/fictional-test-model`. See the shared
`native-android/app/src/test/resources/online-contract-fixtures.json` examples.
The response contains the bounded selection contract, plus HTTP
`X-Metria-Mode: synthetic` and `Cache-Control: no-store`. A synthetic response is
not evidence of Nebius model behavior. Browser Origin headers, non-loopback Host
names, transfer encoding, unknown routes, duplicate required headers, malformed
bodies and oversized requests fail closed. Content length is checked before
reading the body. Eight bounded handler slots and five-second socket timeouts
limit local ingress; no HTTP provisioning or revocation route exists.

SQLite transactions reserve quotas and replay IDs before dispatch. Credentials,
revocation, quotas, replay and active leases persist across process restarts.
Content is never put in SQLite or HTTP logs. Completion retires the active lease
before sending success; failure still keeps the quota/replay reservation. A
crash before completion leaves the lease unavailable until its 60-second expiry.
SQLite coordinates processes sharing **the same database**, not distributed
replicas. Losing/replacing the database loses protection; preserve it and add
provider spending controls before live calls.

For this synthetic runtime, record dates and quota rollover use **UTC**. Madrid
near-midnight phone acceptance and a validated member-zone protocol remain open.
Expired metadata is pruned on successful store operations, not by a background
worker; an idle service can retain it longer. Live retention/deletion acceptance
requires explicit maintenance and hosting verification. Budget units are
reservations, not actual provider tokens or a verified currency cap. HTTPS,
phone credential storage, remote provisioning, provider timeout/token limits,
service identity, region/model and processor terms remain required for live use.

Tests (temporary state and servers are removed/stopped):

```powershell
rtk proxy $metriaBrokerPython -m unittest discover -s broker -p 'test_*.py' -v
```

Shared fixtures also run in the Android unit gate. The process test starts a
hidden server, verifies success, stops/restarts it, rejects replay, confirms a
new request works, revokes through the operator CLI and rejects the old bearer.

The provider adapter uses exactly one tool-free Chat Completions POST with the
exact submitted JSON in the user message and fixed policy in the system message.
It enforces approved HTTPS hosts/certificate checking, rejects redirects without
following, bounds input bytes/output bytes, requires terminal single-choice JSON,
matching model/request/language, bounded usage and strict selection fields.
By default it does not log headers, key, raw body or upstream errors. Only the
explicit fictional desktop capture described above retains a redacted response.
A watchdog aborts
established sockets on timeout/cancel and rejects late output; there is no retry.
OS DNS resolution is not interruptible by that watchdog, so production resolver
latency still needs verification. Requests are not sent after a completed DNS
lookup if the deadline has already elapsed. Actual model tokenizer/usage/pricing,
provider cancellation effects and retention remain live acceptance gaps. Post-call
broker authorization rejects output if its credential/lease was revoked/expired.
Remote Stop is not yet wired into the broker/app; the adapter accepts a cancel
event for that future integration. Closing a socket cannot recall processed data.
## Super VL food-photo test

See [VISION-PROBE.md](VISION-PROBE.md) for the pinned synthetic image and the
single-call vision experiment. `python broker/vision_probe.py --prepare` is
offline; `--live --account-verified` captures the entire fictional request, image,
bounded response, validation, usage and estimated cost locally. This is separate
from the Lightning summary probe and is not integrated into Android or hosting.
