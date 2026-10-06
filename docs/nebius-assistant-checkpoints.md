# Optional Nebius assistant checkpoints

## Disconnected request and consent foundation — 2026-10-02

- The member confirmed the installed local latest-meal question worked,
  authorized a local commit, and requested Nebius development while the phone
  cools down. Local assistant changes and their evidence were committed as
  `c00b9d0`. No push or publication occurred; transient remote attachments were
  excluded. Local release/assistant/vision acceptance gaps remain open.
- Added pure Kotlin code under `app/metria/cloud/`: typed question/language,
  selected weight/nutrition summaries, bounded seven-day period, request identity,
  and explicit NVIDIA model configuration. Serialization has no fields for
  photos, notes, individual meals, goals, cycle context, local chat, keys, or
  correction memory. Categories default to absent. Counts, dates, missing values,
  numeric bounds, and weight change are validated. Canonical preview values use
  readable precision without changing input records; displayed weight change is
  calculated from the displayed endpoints.
- Added in-memory opt-in, immutable exact preview, explicit one-time approval,
  five-minute expiry, and invalidation on edit/clear/disable. Late results and
  duplicate completion are rejected by the lifecycle gate. This gate does not
  validate response content; factual and safety validation remain pending.
- Added a tool-free, single-turn Chat Completions request formatter consuming
  that one-time approval. Question/summary content stays in the user message;
  system policy is separate. Output tokens are bounded to 512; no API key or
  network client is present. Nothing is connected to phone UI, local assistant,
  vault, or an automatic cloud fallback. No phone operation was performed.
- Tests use fictional records and a deliberately fictional model ID. A test
  fixture is not proof that the model is available or live inference works.
  Service identity/authentication, broker schema/replay/abuse controls, retention,
  shipping disclosures, response audit, access metadata, UI, and phone gates are
  all still open. No production dependencies were added.
- Complete Android test/build gate passed with 293 tests, 0 failures, 0 errors,
  and a debug APK. Ten new tests cover category omission, summary precision,
  date/question/model bounds, coverage/count consistency, numeric/date binding,
  disabled and foreign consent, exact one-time send/completion, invalidating
  edits and revocation, expiry/clock rollback, and tool-free provider formatting.
  `git diff --check` passed. This foundation is uncommitted after the local
  assistant checkpoint commit; it was not installed and has no physical or live
  provider acceptance yet.

## Provider references checked

Checked official sources on 2026-10-02:

- [Token Factory quickstart](https://docs.tokenfactory.nebius.com/quickstart)
  documents the OpenAI-compatible Chat Completions request structure and default
  API base URL. The foundation formats that structure but makes no call.
- [Nebius Nemotron Nano cookbook](https://github.com/nebius/token-factory-cookbook/blob/main/models/nemotron/nemotron3-nano-30b.md)
  shows an NVIDIA Nemotron candidate and a regional endpoint. Actual account
  catalog, chosen region, cost, output schema behavior, and service-specific
  retention terms must be verified before selecting a live configuration.
- Marketing privacy language alone does not close processor/retention acceptance.
  No zero-retention claim or real-data sharing is enabled by this checkpoint.

## Strict request and response contract — 2026-10-03

- Added reusable pure Kotlin contract code alongside the disconnected foundation.
  There is no running broker or HTTP endpoint. `OnlineBrokerContract` rejects
  unexpected fields at every object level, explicit-null categories, invalid
  scalar types, noncanonical UUIDs, unconfigured models, unsupported metadata,
  date/count/estimate inconsistencies, and excess numeric precision. Validated
  requests round-trip to the preview representation without adding categories.
- A bounded strict JSON syntax gate rejects duplicate keys (including escaped
  equivalents), comments, unquoted/single-quoted strings, trailing commas or
  content, invalid numbers/escapes and excessive nesting before org.json reads
  the structure. Request bodies are capped at 16,384 characters; responses at
  2,048. Network byte limits remain a separate transport requirement.
- Model output now selects only supplied whole summary sections and one of
  three host-owned guidance categories, tied to the submitted request ID and
  language. It cannot return personal values, arbitrary prose, extra categories
  or actions. The formatter requests this JSON structure through the system
  instruction; actual provider compliance is untested and must be validated.
  No provider-specific structured-output option is assumed by this slice.
- This is a contract prototype, not a finished online answer. Host rendering,
  question relevance, deterministic medical/safety boundaries and consent
  lifecycle integration remain pending. A matching schema does not prove a
  useful answer. Validation must use the immutable submitted snapshot, never
  refreshed local records. Authentication/replay/rate controls are not provided
  by schema validation. No member data, phone operations, dependencies, live
  inference, deployment, commit or push occurred in this step.
- Seven new synthetic tests exercise round trips, excluded nested fields,
  strict types/configuration, independent semantic checks, adversarial JSON,
  bilingual request association, and invented values/actions/sections.
- Final complete Android gate (`:app:testDebugUnitTest :app:assembleDebug
  --no-daemon`) passed: 300 tests, 0 failures/errors, debug APK assembled.
  `git diff --check` passed. Changes remain uncommitted and uninstalled;
  physical interaction, running-broker and live-provider gates remain open.

## Next bounded task

Account setup and live testing are deferred by the member as of 2026-10-06.
The next bounded task is to review member-provided reference screenshots and
design improvements to the relevant Metria views before implementation, using
the repository's design contracts and physical acceptance gates. When cloud
testing resumes, verify account/region/model, processor terms and
hard spending limits, inject a broker-only development secret and then prepare
an explicitly enabled fictional-data live check. Keep the provider secret outside the
APK/repository. Hosted identity, TLS, maintenance TTL and phone timezone/consent
acceptance remain prerequisites before real-data sharing.
The first live check uses fictional data; phone preview/settings and real-data
acceptance follow later. No deployment or public endpoint is created yet.

## Bilingual host replies and terminal lifecycle — 2026-10-03

- Preview creation reparses the canonical payload into a typed snapshot. Replies
  use only that approved snapshot, including its displayed rounding. Synchronized
  completion validates identity/expiry/content, renders, and retires consent.
  Invalid terminal output also retires the attempt. Unsent, foreign, expired,
  cancelled, edited, disabled and completed permits cannot deliver replies.
  These are local controls, not remote abort or service revocation.
- Fixed English/Spanish copy labels Online/Nebius and the submitted period,
  preserves endpoint dates/units, distinguishes empty/single weight records,
  and separates complete/partial/missing nutrition. Missing estimates are
  unknown rather than zero; no total intake or model arithmetic is provided.
  Unshared categories require a new preview; unrelated sections are omitted.
- Conservative lexical routing supports summary questions and general balanced
  meals. Recognized medical topics or model-selected professional support use
  the fixed medical boundary without record advice. Unsupported questions get
  scope text; missing relevant supplied sections reject output. This is not a
  comprehensive medical classifier or semantic quality proof. Model prose
  cannot enter the renderer. Copy/relevance still need member acceptance.
- Nine new synthetic tests cover bilingual precision/associations, empty/single/
  partial records, category omission, relevance rejection, medical overrides,
  general scope, lifecycle invalidation and terminal failures. The final full
  Android test/build gate passed: 309 tests, 0 failures/errors, debug APK built.
  `git diff --check` passed. Changes remain uncommitted and uninstalled;
  physical and live-provider gates remain open.
- No phone, UI entry, network, provider secret, new dependency, member-data
  transfer, commit, push or deployment was introduced. UI must clear transient
  returned answers on lock/exit and reject stale display. The next bounded work
  is broker identity/replay/cost controls and privacy prerequisites above.

## Broker admission simulation — 2026-10-03

- Added `BrokerAdmission`, a disconnected single-process contract prototype.
  Trusted provisioning generates 256-bit random bearer credentials, keeps only
  their digests, expires them after 24 hours and supports revocation. No real
  client credential, provider secret or public provisioning endpoint exists.
- Admission validates authentication, timestamp, UTF-8/character limits, strict
  request/model schema, replay and quotas atomically. Dispatch returns the same
  admitted typed request once; lease identity/expiry/revocation are rechecked.
  Cancel/failure releases concurrency without refunding reserved capacity or
  removing replay IDs. Rolling-minute/day/global limits and in-flight limits
  are bounded; revoked/finished IDs persist until credential expiry.
- Token reservations are conservative units, not measured tokens or a monetary
  cap. Restart resets credentials and quotas; old credentials fail closed, but
  new provisioning after restart could bypass budgets. Durable atomic state or
  independent hard provider spending controls are prerequisites for paid calls.
  Provider timeout/abort, HTTPS, ingress limits, multi-instance coordination,
  secure client storage and privileged provisioning are unimplemented.
- Ten new synthetic tests cover forgery/randomness, expiry/revocation, bound
  one-time dispatch, replay across changed bodies/credentials, timestamps and
  rollback, rate/day/token/global quotas, concurrency/lease expiry, simultaneous
  admission, restart/capacity, UTC quota rollover and Madrid record-date bounds.
  The complete Android gate passed: 319 tests, 0 failures/errors, APK assembled.
  `git diff --check` passed. No installation, phone use, dependency, online call,
  commit, push or deployment occurred; all Nebius changes remain uncommitted.
- Detailed limits, retention and prerequisites are in
  `nebius-broker-security-design.md`. Broker prototype code currently uses the
  Android test harness and must move to the selected server runtime before live
  use. A preference question for local versus hosted first test is pending;
  neither endpoint has been launched.

## Runnable localhost broker with durable admission — 2026-10-03

- Assumed local development for the first runtime while the earlier hosting
  preference remained unanswered. Added a standalone Python standard-library
  broker under `broker/`; removed the Kotlin broker-only simulation and its ten
  tests from Android. Android retains request/consent/response/host rendering.
  No production dependency was added. This replaces the previous prototype,
  without marking hosted, provider, phone or real-data gates passed.
- SQLite commits credential digests/expiry/revocation, replay IDs, active lease
  metadata and per-credential/global daily quotas atomically before dispatch.
  Nothing stores a question, selected summary, answer or provider credential.
  Reopen/process restart preserves authorization, revocation, replay and budgets.
  Reservations are never refunded. Incomplete schemas and a missing database in
  an initialized state directory fail closed instead of resetting limits.
- Privileged local CLI provisioning writes a new credential file only, never
  stdout; state/credentials live in a dedicated owner-restricted directory.
  Windows owner-SID ACL and exclusive file creation were exercised. Revoke
  invalidates leases and removes the local credential file. No HTTP admin route
  or remote enrollment is available. `.local-state/` is Git-ignored.
- The HTTP service binds only 127.0.0.1. It validates Host, rejects browser Origin,
  transfer encoding and duplicate required headers, checks content length before
  body read, authenticates, parses the strict wire schema and atomically admits
  work. Handler concurrency/socket time are bounded; all response/request/error
  logging is suppressed. Responses identify synthetic mode and disable caching.
  The only responder is deterministic and fictional: no Nebius transport exists.
- Process acceptance exercised hidden CLI provisioning/serve, a valid fictional
  request, actual stop/restart, rejected replay, a new valid request, operator
  revocation and rejected old bearer. It caught and fixed a success/lease-finish
  race: work now retires before success is announced. All ephemeral servers
  stopped, credentials/databases cleaned up, no service left running.
- Final broker gate passed: 17 Python tests, 0 failures/errors. The full Android
  test/build gate passed: 310 tests, 0 failures/errors, APK assembled. Count
  changed from 319 because ten broker simulation tests moved out and one shared
  fixture test was added. Ten fictional wire fixtures run in both runtimes.
  `git diff --check` passed. All Nebius work remains uncommitted and uninstalled;
  no phone operation, paid call, member-data transfer, push or deployment occurred.
- Remaining limits: development record-date validation uses UTC; Madrid/other
  member-zone protocol needs acceptance before phone use. Metadata cleanup is
  lazy on successful operations, so idle retention can exceed expiry until
  maintenance is implemented. Database loss/rollback/manual replacement can
  weaken protection despite missing-state checks; provider spending controls
  remain required. SQLite coordinates only processes sharing this local DB.
  Token units are reservations, not verified tokenization or currency caps.
  HTTPS, hosted operator identity, provider terms/configuration, phone credential
  storage, timeout/abort and real-data/privacy acceptance remain open. See
  `broker/README.md` and `nebius-broker-security-design.md` for runbook/limits.

## Fake-provider adapter and account setup notification — 2026-10-03

- The member requested continued adapter work and notification when account
  setup is needed, and clarified store-user access. Confirmed the selected
  operator-owned account/broker architecture: members need no Nebius account
  or key. Public enrollment, operator hosting/billing/support and funding rules
  are not implemented or decided by local development provisioning.
- Added a dependency-free HTTPS Chat Completions adapter, tested only against
  fake loopback providers. Configuration reads only `NEBIUS_API_KEY`,
  `NEBIUS_BASE_URL` and `NEBIUS_MODEL` from the broker process environment.
  Exact HTTPS host/path allowlists and explicit NVIDIA model are required;
  redirects/URL credentials and malformed secret header values are rejected.
  No key is in Android, fixtures, SQLite or committed configuration. The sole
  `.env.example` has an empty key and no selected real model; no dotenv loader.
- Requests preserve exact preview JSON in the user message, separate host policy,
  tool-free single-turn format and 512 output-token parameter. There is one POST,
  no retry, no proxy-environment routing or redirect-following. Response envelope
  and content are bounded/validated: terminal single choice, exact configured
  model, strict request/language/sections/guidance, no writes/prose/extra facts,
  finite bounded integer usage and consistent totals. Raw upstream errors and
  secrets never become displayed errors or logs. Input byte limits and reported
  usage checks do not independently prove tokenizer or currency spending caps.
- Timeout/cancel watchdog closes established sockets and rejects late output;
  a connection cannot auto-reopen after abort. DNS itself is not interruptible;
  an expired completed lookup cannot proceed to sending. Production resolver/
  TLS/latency and actual provider cancellation effects remain open. Adapter
  cancellation accepts an event but remote Stop/app integration is pending.
  Broker post-call authorization rejects revoked/expired results.
- Complete broker gate passed: 28 tests, 0 failures/errors (17 existing + 11
  adapter tests). Cases include HTTP failures/rate errors/redirects without
  retries, forged model/action/identity/output, oversized/chunked/non-JSON
  responses, timeout/cancel, changed snapshots, environment bounds, no logs,
  and an end-to-end authenticated broker/fake-provider path with late revocation.
  All temporary servers/state stopped/cleaned up. Android source unchanged in
  this slice; its latest full gate remains 310 tests with APK assembly.
  `git diff --check` passed. Work remains uncommitted/uninstalled; no real key,
  external inference, paid call, phone operation, member data, push or deployment.
- Official Token Factory quickstart was rechecked for bearer authentication,
  environment-key configuration and Chat Completions endpoint. Actual account
  catalog/region/model/terms/spending controls remain unverified. The member was
  notified account configuration is now the next prerequisite. Instructions and
  a masked, no-network offline config check are in `nebius-account-and-secrets.md`.

## English-only demo scope — 2026-10-04

- Member chose English-only testing/video for the demo. Existing Spanish support
  remains; broader bilingual acceptance stays open. Local broker is sufficient
  for fictional-data tests/video; public/judge service availability is separate.
  Member reports $30 credits, whose Token Factory applicability/expiry and
  spending controls need account verification.
- Presence-only inspection found no broker process `NEBIUS_API_KEY`,
  `NEBIUS_BASE_URL` or `NEBIUS_MODEL`; no values were displayed. A real inference
  check cannot run until those are configured and account prerequisites verified.
- Added five English fake-provider scenarios: balanced meal information, weight
  summary, partial/missing nutrition, combined categories and empty weight.
  Verified language, selected-category scope, exact submitted coverage and
  bounded response selection. These are synthetic integration checks, not live
  Nemotron accuracy or visible phone demo acceptance.
- Focused English provider suite passed: 12 tests, 0 failures/errors. Test
  servers stopped and temporary state removed. `git diff --check` passed.
  Android production code unchanged; latest complete Android gate remains 310
  tests with APK assembled. No phone/installation, real inference/member data,
  dependencies, commit, push or deployment. Next bounded task remains safe
  broker-only account/key/endpoint/model setup and a fictional English live probe.

## Commit and push checkpoint — 2026-10-06

- Member authorized committing and pushing the disconnected cloud foundation,
  localhost broker and fake-provider adapter. Token configuration and real
  inference testing are deferred. Reference screenshots will be supplied in
  chat for later view improvements; transient attachments are excluded from
  this commit.
- Complete broker suite passed: 29 tests, 0 failures/errors. The required
  Android test/build command succeeded with tasks up to date; existing test
  reports confirm 310 tests, 0 failures/errors, and the debug APK is assembled.
  No phone operation or installation occurred; physical and live-provider
  acceptance, shipping consent, hosted identity/TLS, retention, timezone and
  spending controls remain open. No real key or member-data transfer occurred.
- Remote history was fetched before publication. Local `main` contained the
  remote tip and two previously approved local-assistant commits; no merge was
  needed. The new checkpoint commit and those preceding local commits are
  included in the authorized push. No deployment or cloud enablement is part
  of this publication.
