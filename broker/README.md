# Local development broker

This broker runs on **127.0.0.1 only**, with a deterministic fictional responder.
It makes no Nebius call and is not connected to the Android app or member vault.
Python 3.12+ standard-library modules are sufficient; no package installation.
The repository's bundled Python was used for verification.

`provider.py` now contains a separately tested single-call HTTPS adapter. It is
injected only against a fake local provider in automated tests; the CLI's default
server remains synthetic and does not load provider credentials or enable live
calls. `broker/.env.example` documents broker-only variables with an empty key.
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
It does not log headers, key, raw body or upstream errors. A watchdog aborts
established sockets on timeout/cancel and rejects late output; there is no retry.
OS DNS resolution is not interruptible by that watchdog, so production resolver
latency still needs verification. Requests are not sent after a completed DNS
lookup if the deadline has already elapsed. Actual model tokenizer/usage/pricing,
provider cancellation effects and retention remain live acceptance gaps. Post-call
broker authorization rejects output if its credential/lease was revoked/expired.
Remote Stop is not yet wired into the broker/app; the adapter accepts a cancel
event for that future integration. Closing a socket cannot recall processed data.
