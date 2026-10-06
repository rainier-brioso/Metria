# Nebius broker admission prototype

Status: localhost synthetic Python runtime, 2026-10-03. Temporary fictional
tests provisioned credentials and exercised HTTP/process restart; all test
servers stopped and state was cleaned up. No provider or phone is connected.
The earlier Kotlin admission simulation has been removed from Android and
replaced by the independent standard-library runtime in `broker/`. Never put
operator provisioning or provider secrets in the member app.

## Identity and request admission

The proposed initial demo uses operator-provisioned, per-installation opaque
bearer credentials, generated with SecureRandom from 32 random bytes. The
broker stores SHA-256 digests, compares with hmac.compare_digest, and expires
credentials after 24 hours. Revoke invalidates active leases and future sends.
No public enrollment endpoint, embedded shared secret, account, vault ID or
biometric material is involved. Operator provisioning remains a trusted internal
operation through the local CLI. It writes an exclusive private file in an
owner-restricted directory, without printing the bearer. Remote provisioning
authentication and phone delivery are not implemented.

Bearer credentials authorize whoever possesses them. HTTPS, safe private client
storage, header redaction and secure provisioning are prerequisites, not optional
consequences of these tests. Timestamp freshness is not a cryptographic proof
of member consent. The app's exact-preview flow enforces member consent locally.

Admission authenticates, requires a timestamp within the preceding 60 seconds
(future timestamps fail), enforces body character/UTF-8 byte bounds, validates
the request and configured model, rejects replay, and reserves quotas atomically.
Each lease binds the validated immutable request; dispatch releases that request
once and rechecks expiry/revocation. Failure, Stop and timeout must finish leases.
The localhost transport checks content length before reading bodies. A provider timeout
shorter than the 60-second lease. Expired leases release concurrency; they cannot
stop an already-running provider call. The adapter must actually abort or finish
that call so concurrency limits are meaningful.

## Prototype limits and retention

| Control | Initial bound |
| --- | --- |
| Credentials | 32 active, 24-hour expiry |
| Request body | 16,384 characters and UTF-8 bytes; content-length limit before body read |
| Request freshness / lease | Strictly less than 60 seconds |
| Per credential | 3 requests per rolling minute; 20 per UTC day; 1 concurrent |
| Global | 100 requests per UTC day; 2 concurrent |
| Reserved token units | 8,704 per admitted attempt; 200,000 per credential/day; 1,000,000 globally/day |
| Replay IDs | Up to 4,096; retained until admitting credential's expiry, including after revocation |
| Content | Only active request snapshots in memory; removed on finish/expiry; no body/response logs |

Reserved units assume an eventual enforced 8,192 input-token ceiling plus 512
output tokens. No tokenizer or priced budget exists yet, so these are admission
reservations, not verified provider token counts or a monetary spending cap.
Cancellation/failure never refunds reservations. Provisioning more credentials
cannot bypass global quotas. Credential rotation and daily rollover cannot
erase still-retained replay IDs. Replay protection ends after its disclosed TTL;
new authorized requests must use new random IDs.

The runnable synthetic runtime uses UTC for quota dates and record-date
validation. Madrid near-midnight acceptance and other member zones need an
explicit protocol decision before phone integration. Clock rollback fails
closed until time catches up; local revoke/cleanup remain available. No question,
summary or answer is stored in quota state. SQLite keeps credential digests,
expiry/revocation, request IDs/admission/lease timestamps and daily quota counts.
Expired metadata is lazily pruned on successful operations; idle retention can
exceed TTL. Maintenance/deletion acceptance remains open before live use.

SQLite now persists credentials, quotas, revocation, replay and active lease
metadata across restart, with atomic reservations under BEGIN IMMEDIATE. A
crash leaves an admitted active lease until expiry; completed work retires it
before success is sent. Cancellation/failure never refunds reservations. Tests
cover separate connections/process restart. Multiple processes sharing the same
local DB coordinate; distributed replicas require another design. Database loss
or manual replacement can still reset budgets, so keep durable state and
independent provider spending controls. No content queue/cache or automatic
retry exists. HTTP request/exception logging is suppressed, and browser-origin
requests and non-loopback Host headers are rejected. This localhost HTTP service
is for fictional development requests, not public or phone real-data transport.

## Remaining live prerequisites

- Local development is the assumed first slice; hosted runtime/operator remain open.
- Implement privileged provisioning/revocation, secure client credential storage
  and TLS; no anonymous paid endpoint. Apply unauthenticated ingress limits too.
- Verify deployment durability/retention and hard provider spending limits,
  enforce actual input/output tokens, and abort timed-out provider calls.
- Verify region/model catalog, service-specific retention and deletion, hosting
  access/body/trace logs, secret storage and billing using fictional requests.
- Keep app consent/response handling separate from broker authorization. No
  deployment, live request, publication or real-data sharing is implied here.

Security references checked 2026-10-03:
[OWASP session management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
informs cryptographically random credentials;
[OWASP REST security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
informs HTTPS and rate-limit requirements. These references do not certify this
prototype or close Metria's privacy/security acceptance gates.
