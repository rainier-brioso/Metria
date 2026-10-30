# Optional Nebius assistant checkpoints

## Judge quickstart verified - 2026-10-30

README now starts with direct APK installation and distinguishes evaluator setup
from Android compilation. BYOK instructions cover Docker Desktop/Linux containers,
the matching release-tag clone or ZIP, hidden Nebius key prompt, automatically
issued private pass, exact Android connection fields/consent, generated photo,
reviewed Save and a latest-meal query. Gemma download is explicitly unnecessary
for cloud. The detailed Docker guide defaults to content-free logging; raw
fictional capture is optional. Key/pass/URL roles, limits, partial nutrition,
restart and DNS behavior are documented without changing runtime policy.

Verification: anonymously cloned `v0.1.0-hackathon` into isolated ignored state
and ran the documented PowerShell launcher with `-Action Check`. Docker build,
public HTTPS health and authorization rejection pass; output confirms no
provider inference. The temporary tunnel closes after the check. Existing
funded broker/pass/ledger were not mounted, stopped or reset. Anonymous HEAD
requests to APK, source ZIP and synthetic photo all return HTTP 200. App-field
names were checked against the installed APK's source. No new-key phone/live
inference or Android rebuild is claimed for this documentation-only update.

## Local/cloud meal follow-up - 2026-10-29

Member confirms analyzing and saving the same meal through both Gemma and
Nemotron. Authorized screen inspection used Leave without saving; no fields
were changed, and temporary captures were viewed/deleted outside the repo.
Gemma needed an explicit Calculate action before catalog nutrition appeared.
Nemotron returned more detailed food descriptions, but some names did not match
the offline catalog. Partial cloud nutrition excludes unmatched foods. Catalog
matching remains a follow-up issue; no repair or accuracy ranking is claimed.
The member deferred the comparison, which is absent from README.

The local trace remained available, but raw Gemma image capture had cleared.
No raw device/model output is committed. No extra inference was used for review.
Earlier, the member authorized extending the existing local pass to 20 total
attempts, preserving 10 used attempts, all history, expiry and binding. Global
limits remain 20; default newly issued passes still start at 10. No reset.

## APK release verified - 2026-10-30

Member authorizes committing product changes, pushing local commits and
publishing the APK. Video-editing materials and generated Gradle daemon config
are excluded. The normal app's debug APK is packaged as
metria-0.1.0-hackathon.apk with SHA256SUMS.txt; no binary is added to Git.
Android gate passes 775 tests/build, signature verification passes, and the
APK contains MiniLM plus license notices with no private-state file paths.
Published prerelease `v0.1.0-hackathon`, targeting source commit
`b5306731d7ac606baf09b508aea350f39123e58a`. Both assets are uploaded;
GitHub's APK digest matches the tested local artifact:
`c3b92389b1758a4299c693d56767a15c3bf63b3d6589d4f381544b06696ea925`.
Release: https://github.com/rainier-brioso/Metria/releases/tag/v0.1.0-hackathon
Member subsequently authorized making the repository public. Anonymous APK and
repository requests return HTTP 200. Main is protected: PR required, conversations
resolved, force pushes/deletion blocked, administrators included, zero required
external approvals for a solo maintainer. GitHub rejected branch protection while
private on the current plan, so it was applied immediately after visibility
changed. No required CI checks are configured because no workflow exists yet.

## Member confirms cloud voice meal draft - 2026-10-29

After the broker restart instruction, member reports: "funcionó, tengo draft
correcto". Combined with the earlier visible-provider-label confirmation, this
establishes the member-observed on-device transcript -> Nemotron -> reviewable
meal draft path on the installed normal app. It does not establish all label
locations, all layouts, a subsequent save or general model reliability. Existing
770-test Android gate/130-test broker suite remain applicable; no source changes
since that gate. No additional inference is needed to repeat this acceptance.

README refreshed around local AI, private records and explicit cloud choice,
with actual model roles, BYOK Docker instructions and limitations. Public APK,
video and project-level license remain submission work. User authorizes committing
the accumulated integration/demo work; unrelated generated Gradle daemon JVM
configuration remains outside the commit. No push requested.

## Voice-to-cloud meal repair and provider labels prepared - 2026-10-29

Member reports successful transcript followed by the generic meal draft error,
and requests visible local/cloud model labels while recording. Two authorized
fictional captures show: valid empty proposal asking whether the member meant
rice, and rejected incorrectly escaped inner JSON with string assumptions and
questions. This is downstream of transcription, not an ASR failure.

Broker meal task now requests a strict object-valued reply schema, using the
existing photo proposal schema. It serializes for the unchanged app wire contract
before existing validation; malformed output is still rejected. Reported usage
is retained on failed decisions, no retry/inference added. Chat tool contract
unchanged. Docker image rebuilt; running broker must be restarted by the member.

Native focused-entry clarification keeps operation meal so the one-use result
reaches the editor; safe question/failure is shown instead of generic replacement.
Cloud readiness no longer depends on a loaded Gemma for retry/voice controls.
Selected mode is explicit even with no cloud connection. Shared icon/text labels
identify Local · Gemma, Cloud · Nemotron in Chat/meal entry/photo preview and
analysis; label OCR says Local · ML Kit. Cloud voice describes on-device speech
recognition followed by cloud text processing. No member-record changes.

Broker suite passes 130 tests. Android final complete gate passes 770 tests and
APK assembly. Member explicitly approved normal-app installation: install
--user 0 -r Success on SM-S928B / Android 16; cold launch Status ok, 1170 ms.
APK SHA-256 `1073fd2ebfce41de25cb651adb413aca5b16dfc53ff52d008e42d55e63afee5c`.
Original first-install identity retained, Demo untouched. No clear/uninstall or
record/model/credential replacement. Studio not running; post-launch screen
capture protected/black. No attempt to bypass protection or biometrics. Temporary
captures reviewed/deleted. Member restart of broker, label/layout confirmation
and actual new live-schema request remain pending. Detailed evidence and gaps:
docs/design/evidence/provider-labels/verification.md.

Member confirms a provider label is visible but meal still fails. Latest fictional
capture is HTTP 200 with completion exhausted at 2048 reasoning tokens and no
content. Container image inspection confirms the running broker still uses the
older image (started before this repair), distinct from the rebuilt local tag.
Therefore this is not acceptance of the new schema path. No further inference
attempt requested until member restarts the existing terminal launcher, enters
their key privately, and updates the app URL while preserving the pass/ledger.

## Windows Docker state ownership repair - 2026-10-29

Live startup reported local_state_permission_denied. Metadata inspection finds
Windows-created mounted state directories owned by container UID 0 while the
broker uses UID 10001: writes are allowed but chmod fails. Disposable Check did
not cover this because its fresh directories belong to UID 10001.

Start now prepares ownership of exactly the two known state/capture directories
using an ephemeral UID-0 container with all capabilities dropped except CHOWN,
no-new-privileges, and descriptor-based no-follow directory access. No recursion,
content reads, file replacement, quota reset or host ACL expansion. Main broker
remains non-root. Isolated Windows-created fixture verified private-directory
startup, response capture and exhausted quota across two containers. Applied
directory ownership to current state and verified UID-10001 chmod/private modes
without reading pass or ledger. No inference; live key/start and phone remain
member acceptance gates.

Verification completed: 126 existing broker tests plus two ownership-boundary
tests pass; Docker fixture passes repeated preparation/start with private modes,
capture and preserved exhausted quota. Current-directory preparation is also
repeatable. Image rebuilt and git diff --check clean. No APK change/install.

## Local launcher failure diagnosis - 2026-10-29

Member reports the launcher's generic nonzero-exit error while recording.
Docker Linux engine is available and no metria-local container blocks startup.
A disposable no-key HTTPS check reproduced a failure after health verification;
its hidden exception does not establish the cause of the member's live failure.
Added finite sanitized startup codes, with tests proving arbitrary exception
messages are excluded. Subsequent rebuilt Docker HTTPS health and unauthorized
request checks passed and closed their disposable tunnel. Nine targeted tests
pass. No inference, credential read or real quota reset occurred. Live startup
with the member-entered key and phone meal acceptance remain pending.

## Demo onboarding safe-area repair installed - 2026-10-29

Member requested correcting the observed Continue/system-navigation overlap.
Added safe drawing insets, independently scrolling body and fixed footer;
assistant chips wrap and page state is saveable. Common onboarding source is
corrected; ONLY app.metria.demo was installed. Normal installed app and data were
not modified. All three pages' content and consent/completion behavior retained.

Full Android tests/Debug/Demo build pass: 769 tests, no failures/errors/skips.
Demo APK SHA-256 `9240dc7669cf1efc0a2916db4ce903f79867ccf3f00ce2931c9b87758b8fed4d`.
Samsung SM-S928B / Android 16: install --user 0 -r Success, cold launch Status ok.
Physical before/after checks resolve the overlap in all three portrait steps;
font scale 1.3 wraps choices without clipping; landscape scroll reaches both
choices while footer stays accessible. Back exercised, then Demo left at page 1
without completion. Original font/rotation values restored and verified; normal
app lastUpdateTime unchanged. No model download, record write or cloud activation.

Temporary screenshots reviewed then deleted. Detailed sanitized evidence and
remaining gesture/TalkBack/process-death gaps:
docs/design/evidence/onboarding-insets/verification.md. No full app acceptance,
uninstall, clear, commit or push claimed.

## Separate Metria Demo installed for onboarding - 2026-10-29

Member explicitly requested preparing/installing an independent onboarding app,
leaving app.metria and its data intact. Added debug-derived demo build type:
application ID app.metria.demo, launcher label Metria Demo, version 0.1.0-demo.
Shares source implementation, not data: no vault/model/credential copy, seed,
authentication bypass or model download. Demo provider authorities use its own
application ID; normal weight shortcut is disabled and normal metria URI scheme
is not claimed. Original debug identity/label/scheme remain app.metria/Metria/metria.

Full :app:testDebugUnitTest :app:assembleDebug :app:assembleDemo gate succeeds;
769 JVM tests, zero failures/errors/skips. Compiled APK manifest/resources checked
with aapt2: demo ID/label, distinct provider authorities and empty shortcut
capabilities, no sharedUserId. APK SHA-256:
`a93c66656070143100a1ad0068a6527f52616471115634b780065e75730daa50`.

Installed ONLY app-demo.apk with install --user 0 -r on Samsung SM-S928B / Android
16: Success. Demo first installation (absent before); cold launch Status ok,
1856 ms. Package listing confirms distinct UIDs for normal and demo apps. Normal
app lastUpdateTime remains unchanged before/after, 2026-10-28 23:08:52 as reported
by the device. No normal-app install/force-stop/private-file inspection occurred.
No clear, uninstall or signing-key replacement. Existing data isolation verified
by package identity/UID, not by reading member records or model bytes.

android layout returned the known instrumentation-server response error; did not
repeat it. Temporary screenshot fallback visually confirms onboarding page 1/3,
Private by default, with Continue visible; screenshot immediately inspected and
deleted outside repo. No taps, onboarding advancement, biometric interaction or
model download. Initial portrait only checked, not a complete onboarding quality
gate: the existing Continue button partially overlaps the three-button navigation
bar and needs separate layout follow-up before final polished recording. Variant
does not change the onboarding layout. Other steps/rotation/font/keyboard remain
untested. Guide: docs/demo/onboarding-installation.md.

## Local Docker rehearsal prepared and checked - 2026-10-28

Member confirmed the installed latest-meal calorie question now works in cloud
mode. This evidences model read selection -> on-device tool -> validated answer;
it does not establish all chat/voice flows. Member subsequently deferred permanent
hosting and authorized adapting the local broker to Docker and retesting.

Added Dockerfile.local with Python and pinned cloudflared 2026.10.5 (official
Linux amd64/arm64 SHA-256 checked at build and launcher startup). Broker and tunnel
share container loopback; no origin ports are published. The image uses UID 10001,
excludes private state and test fixtures, and includes the common contract.
SIGTERM now follows the launcher's normal shutdown path. Windows Python launch
remains supported with its existing pinned executable.

Windows local-docker.ps1 builds and provides Start, Check, Usage and Build actions.
Start prompts for the key inside the interactive container, without injecting
host key environment or printing it. The .local-state bind mount preserves the
existing pass, binding, counters and captures. Operator must stop the previous
Python launcher before switching; no existing service was stopped by this task.
No key/pass was read and no real ledger modified by verification.

Verification: 125 broker tests pass. Local amd64 image builds. Direct container
and final helper Check each verified public HTTPS and rejection of missing/forged
authorization, then closed without inference. Helper Usage successfully read
only aggregate existing cost metadata through a read-only mount. Two replacement
containers using isolated synthetic state verified pass binding and consumed
one-attempt quota survive container removal/recreation, rejecting a second attempt.
Test state is Git-ignored and separate from the app ledger. git diff --check passes.

Guide: broker/DOCKER-LOCAL.md and README quick start. Remaining: member starts
Docker with their hidden key, reconnects the changed tunnel URL with the same
pass, and repeats a fictional photo plus latest-meal question from the existing
APK. No Android changes or reinstallation needed; no Docker live-provider/phone
acceptance, hosted service, commit/push or release publication claimed.

## Cloud reply-scope APK installed - 2026-10-28

Member explicitly authorized installation. Verified APK SHA-256
`66b6c47322995bd21a46e417bac37e407c70b87bf51741f1735e77152125bde4`
from the 769-test successful Android gate. Installed with `install -r` on the
connected Samsung SM-S928B: Success. Force-stop/start returned Status ok, cold
launch 1446 ms. No storage clearing, uninstall, credentials inspection or biometric
interaction. Records and encrypted connection were preserved by the update;
their contents were not inspected. Next: member selects cloud without disconnecting
the saved pass and retries the latest-meal calorie question. Actual reply acceptance
remains pending; no broker restart is needed for this Android-only change.

## Cloud latest-meal reply scope repaired - 2026-10-28

Member's latest-meal calorie question failed after local result validation while
Gemma mode worked. Fictional assistant capture at 21:57:48 UTC confirms HTTP 200
and the correct `read_latest_meal` selection with empty arguments. The model
selection repair is evidenced; the displayed error came from Android afterward.

Cloud retained the broad grounding requirement (no targetDate) while local fast
reads enrich it with a frozen host date. The outgoing call received today's date,
but the pending requirement did not, so validatedFastReadReply rejected even a
valid local result. CloudLocalReadPolicy now reuses the existing local meal/weight
scope resolvers before publishing the read. No date or record is sent to the model;
all existing result provenance, stale-date and tool checks remain in force.

Regression reproduces the exact question and missing-date rejection using a
fictional 350 kcal meal, then verifies the enriched scope accepts it and rejects
stale/untrusted results. Additional parity cases cover today/yesterday meal reads,
today nutrition, selected-meal preservation and non-read prose. Full Android gate
passes 769 tests, zero failures/errors/skips, and assembleDebug. APK SHA-256:
`66b6c47322995bd21a46e417bac37e407c70b87bf51741f1735e77152125bde4`.
Installation and actual cloud reply acceptance remain pending. No provider call,
broker change/restart, private-vault read, commit or push in this repair.

## Shared-contract APK installed; private passes retained - 2026-10-28

After researching accountless automatic access, member chose to keep the existing
private-pass scheme and explicitly requested installation. No App Check/Firebase
dependency, account system or automatic public enrollment added. Evaluators
receive a private pass, never the operator's Nebius API key.

Verified the exact previously gated APK SHA-256
`3da0cbfedb1325ee5010b8dd1c23b401633ba29ef6d38c98416274361217cb70`
and 767 passing Android test reports. Installed in place with `install -r` on the
currently connected Samsung SM-S928B / Android 16: Success. MainActivity cold
launch Status ok, 2135 ms. No clear/uninstall/model deletion/signing change or
biometric action. Existing storage retained by the update; member confirmation
of records/model and live cloud-chat acceptance remain pending.

Next: member restarts their broker with the fictional-capture command, updates
the temporary URL with the same pass and tries one latest-meal calorie question.
No live inference, credentials read, device screenshot, commit or push occurred.

## Provider-independent assistant contract implemented - 2026-10-28

Member requested the structural correction: one definition of Metria capabilities
and common rules across Gemma/local and Nemotron/cloud, with runtime adapters.
Added `shared/assistant-contract.json` for common base/agent prompts, examples,
13 tools, parameter descriptions/types/limits, native tool-set grouping and task
allowlists. Gradle generates actual LiteRT tool classes/policy constants;
the broker reads the same JSON. Removed handwritten native declarations and
duplicate broker tool descriptions/limits. No SDK/provider dependency added.

Both argument validators consume shared metadata before the existing typed and
contextual host checks. Action mapping, dates, unlocked vault/draft identity,
record grounding, deterministic calculation, review and explicit Save stay on
device. Native tool calling stays manual. Runtime framing, provider identity and
task-specific generation formats remain in adapters. No new record export.

Verification: full broker gate passes 123 tests. Full Android gate
`:app:testDebugUnitTest :app:assembleDebug --no-daemon` passes 767 tests with zero
failures/errors/skips. Both run the same 47 valid/invalid conformance vectors,
covering all tools, unexpected arguments, null slots, numeric types, bounds and
enums. Native tests inspect actual generated SDK annotations and parameter
descriptions and verify the source fingerprint. Reflection tests require the
SDK's Java 21 bytecode: test launcher uses current Gradle JVM when 21+ (installed
25 here), otherwise Java 21. Production target stays 17. Unrelated untracked
daemon JVM configuration was preserved. `git diff --check` passes.

Rebuilt local container and verified hosted imports, 13-tool catalog and no
private state/test-vector inclusion. Container and native source share SHA-256
`11d562635c31848c17312f2cffb999b5b01ae4d79ac03a574826a21f7d4ddf23`;
the same fingerprint is present in the APK's classes3.dex. APK (214729387 bytes)
SHA-256 `3da0cbfedb1325ee5010b8dd1c23b401633ba29ef6d38c98416274361217cb70`.

No installation, device interaction, live inference, provider-key access,
commit/push or deployment in this slice. Existing unrelated changes retained.
Next: approve in-place installation of this APK, restart the private local
broker with fictional response capture, update the tunnel URL and verify one
actual read selection plus local answer. Repeat Gemma read smoke when feasible.
Common contracts/tests do not prove identical model choices or close physical
or hosted acceptance.

## Cloud chat missing tool definitions repaired - 2026-10-28

Member reports the same local-record verification error for a specific latest
meal calorie question. The two captured fictional chat responses are HTTP 200
valid decision envelopes but contain refusal prose (no access to records) and
`tool:null`. Android correctly rejects those against its required local read.
No record values, request messages or private credentials were inspected.

Found a concrete broker integration defect: GLOBAL_INSTRUCTIONS referred to
enumerated tools but the actual prompt did not enumerate any names/arguments.
Added explicit server-owned descriptions and exact argument contracts for the
existing 13 tools, restricted to the active task. State that records remain on
the phone, which executes authorized reads and renders the answer locally; their
absence from the prompt is expected and does not prevent selecting a read.
Include latest-meal and today's-meals selection examples. Draft extraction
still has no tool enumeration. All server/native validators, task allowlists,
local record rendering, consent, quotas and no-retry rules remain unchanged.

Complete broker suite passes 121 tests. Regressions assert every allowed tool
is actually described in the outgoing provider system prompt, task-specific
isolation and normalized latest-meal read action without invented record prose.
No Android edit/install, provider call, raw member data access or quota reset.
Member reports having saved the prior meal; save and persisted values have not
been independently inspected. Cloud chat physical acceptance remains pending.

Next: restart the member-owned local launcher with fictional capture enabled,
update the changed tunnel URL using the same pass, and ask the latest-meal
calorie question once. Inspect actual tool selection before claiming success.
Rebuild the broker image from final sources before any future hosted deployment.

## First successful app-to-Nebius meal draft - 2026-10-28

Member reports a visible draft and says it looks good. Inspected the newest
operator-declared fictional app vision capture and matching usage metadata:
Super VL HTTP 200, finish `stop`, capture not truncated, validated outcome,
no captured errors. Provider response contains a three-item flat meal proposal,
overall medium confidence, rough gram ranges, hidden-oil/scale uncertainty and
a clarifying ingredient question. No photo was inspected and no save performed.

Provider/capture elapsed time 8875 ms; wrapper elapsed time 8891 ms. Reported
usage: 1628 prompt tokens (1298 image, zero cached), 2126 completion tokens
(1723 reasoning), 3754 total. Uncached public-list estimate USD 0.0024018,
not exact invoicing. Complete redacted provider envelope is retained privately.
The successful completion exceeds the old 2048 output-token budget, supporting
the increase to 4096, but it does not prove the earlier unrecorded failure cause.

Evidence now establishes one real app -> HTTPS tunnel -> local pass/quota
broker -> Nebius -> visible reviewed draft round trip. It does not establish
food/portion accuracy, save behavior, chat/voice, cancellation/lock, revocation,
exhaustion or hosted/public evaluator acceptance. No additional inference,
installation, device inspection, commit or push by the agent.

## Complete fictional app response/error capture - 2026-10-28

Member explicitly requested full provider responses/errors during fictional-data
app tests, extending the earlier desktop-only capture scope. Added the local
launcher's `--capture-fictional-responses` flag; it requires verified live mode
and declares all submitted photos/messages/prior context fictional. Hosted and
default serving remain content-free. Architecture, M1 and cloud scope updated
to record the exact approved exception.

Vision and chat adapters have optional response/error callbacks, unused by
production. When enabled, capture the provider body/status before any HTTP,
JSON or schema rejection; interrupted bodies are marked truncated. Original
network/parser exception details and final validation traces are retained too.
The local wrapper uses the existing owner-restricted Git-ignored DebugCapture
store: complete redacted bodies up to 64 KiB, 20 artifacts, request correlation,
status/outcome/diagnostics, usage and timing. No request photo/body/auth headers
or installation credential is captured. Known provider-key spellings are
censored; only filenames/metadata are printed. Full/unwritable capture storage
blocks calls before provider invocation rather than silently losing evidence;
mid-call write failures warn that inference may already be billed.

Complete broker suite passed 119 tests, including six new tests for HTTP error
bodies, schema-rejected bodies/usage, complete successful chat envelopes,
original network errors/key censorship, unavailable capture blocking dispatch,
and capture disabled by default. No real API call or phone operation. Existing
private capture count is 2/20, checked without reading their content. The old
failed response cannot be recovered retrospectively.

Next: member restarts their broker with the new flag, updates the temporary URL
in the app and performs one fictional analysis. The printed capture filename
will allow inspecting the provider response/error directly. No APK reinstall
needed for this broker-only change; preserve the pass/quota ledger.

## First admitted app vision failure and broker diagnostics - 2026-10-28

After the pixel fix was installed, member reports loading then the same generic
failure. Content-free evidence now shows one admitted/dispatched attempt and a
vision metadata row lasting 5875 ms with `usage_unknown`. Unlike the first
attempt, upload reached the provider adapter. Old diagnostics cannot distinguish
HTTP/network rejection from malformed/truncated output; do not claim a proven
model-format root cause or treat unknown billing as free. No raw photo, content,
provider key or credential read; no request repeated by the agent.

Updated SuperVLVisionAdapter with finite HTTP/network, envelope/model/finish and
proposal-validation diagnostics. Extract and validate usage before validating
draft content, carrying only those counters on ProviderFailure. Local rehearsal
recorder retains/prints fixed diagnostics and known cost for rejected responses;
unknown usage remains unknown. No photo/output/error-body capture, replay, retry,
quota reset, Android changes or new dependency.

Vision requests now explicitly request the existing flat proposal via JSON
Schema and allow 4096 output tokens, matching the desktop probe's output budget
rather than the previous 2048. Server validators and image/pass/quota limits
remain authoritative. Official Nebius structured-output docs were checked;
actual Super VL acceptance of this request option still needs a live result.
Public model catalog has no explicit JSON-mode tag, so do not claim verified
schema support or a solved phone round trip from fake-provider tests.

Complete broker suite: 113 tests pass; diff check passes. Regressions cover
truncated/malformed drafts retaining usage and finite diagnostics, HTTP 401
without fabricated usage, and content-free local rejected-cost recording.
The member's running broker still has old Python code loaded: restart the
interactive launcher, update the changed tunnel URL in the app, reuse the same
pass and repeat one fictional case. No APK reinstall needed. If it fails, the
new metadata should identify the failure class before another paid attempt.

## Cloud photo rejection: pixel/byte boundary repair - 2026-10-28

Member reports the generic photo draft failure after connecting the app. Read
only code and content-free broker metadata: one pass bound, zero admissions or
dispatches/lifetime requests, no usage file. No member image/output or credential
was inspected; no inference repeated. The precise failed request was not
captured, so the physical failure's root cause is not yet proven.

Found a concrete incompatibility: local decode sampling can leave images up to
3072 pixels, while cloud JPEG validation caps either dimension at 1536; Android
also rejected files above 2 MiB before resizing. Added CloudPhotoPreparation on
the cloud-only request worker: fit to 1536 maintaining aspect ratio, encode an
EXIF-free JPEG within 2 MiB (bounded quality steps), recycle bitmaps, and reuse
already compliant prepared bytes. Source photo, local Gemma decode/detail,
thumbnail, review and save behavior remain unchanged. No network retry added.

Four regression tests cover typical sampled camera sizes, portrait/landscape,
no upscaling, exact boundary/extreme ratios and invalid dimensions. Complete
Android test/build gate passes 764 tests, zero failures/errors; diff check passes.
New APK SHA-256:
`bbb16649d196fa05ce68f86edf286951b8c05e6e5c27e41d8346b3762f39a26e`.
Member subsequently authorized installation ("yes install"). Verified the exact
tested APK hash and installed with `install -r`: Success, Samsung SM-S928B /
Android 16. MainActivity cold launch returned Status ok in 1023 ms. No data
clearing, uninstall, model deletion, biometric interaction or paid request.
Existing app storage preserved by in-place update; member confirmation of
contents and physical cloud-photo acceptance remain pending.
Next: member unlocks and retries once using a fictional image;
check the local usage metadata before claiming a working cloud round trip.

## Authorized in-place phone installation - 2026-10-28

Member explicitly requested installation. Re-ran the complete Android gate:
`:app:testDebugUnitTest :app:assembleDebug --no-daemon` succeeds; reports show
760 tests, zero failures/errors. Installed the same APK with SHA-256
`4d9136f71aa881bb1f10df124325d10072d6471b66f8c9f4eafb9903215b099d`
on Samsung SM-S928B / Android 16 using the currently discovered wireless ADB
connection and `install -r`: Success. Force-stop and explicit MainActivity
launch returned Status ok, cold launch 2046 ms.

No uninstall, data clearing, key replacement or model deletion performed.
In-place update preserves existing app storage; member confirmation of vault
contents/model and the physical smoke test remain pending. No biometric unlock
attempt, screenshot, private-record inspection, cloud configuration or paid
inference. Next: member unlocks the app and starts/connects the live local
tunnel with their private key/pass for fictional-data acceptance.

## Local app HTTPS rehearsal preparation - 2026-10-28

Member approved a temporary HTTPS tunnel before permanent hosting, using only
fictional inputs with a private pass and consumption limits. Added
`broker/local_tunnel.py` and `broker/LOCAL-APP-TEST.md`. The launcher verifies the
official portable cloudflared 2026.10.5 SHA-256, binds the origin only to loopback,
retains Host/HTTPS-header validation, strips provider credentials from the tunnel
child environment and stops the tunnel on exit. No Android TLS exception needed.

The dedicated private `.local-state/nebius-app-test` ledger has a 10-attempt,
seven-day, single-installation pass; global daily/lifetime cap is 20 and normal
3/minute/one-active-per-installation safeguards remain. Restart preserves state.
Local-only technical evidence contains request UUID, model, route, latency,
reported token counts and uncached list-price USD estimates, never request/output
content, photos or credentials. Failed/unvalidated responses have unknown cost,
not zero. `--usage` summarizes estimates separately from unknown calls. This is
not invoice reconciliation or a guaranteed monetary cap.

Real public HTTPS health and missing/forged-credential rejection passed in
`--check` mode without any Nebius inference. The PC's default DNS cannot resolve
the temporary hostname, but public DNS does; the verified health check used DoH
resolution with original hostname/SNI and normal certificate validation. Use
mobile data on the phone if its Wi-Fi DNS has the same restriction. Check-mode
tunnels were stopped. No live service left running or key collected.

Verification: complete broker suite passes 111 tests; `git diff --check` passes.
Offline usage summary reports zero real calls in this new rehearsal ledger.
Android sources/APK are unchanged in this task; prior 760-test/build evidence
still applies, without implying physical acceptance.

Next: member runs the interactive live command in their own terminal (existing
key or hidden prompt), connects the already-built APK with the printed URL and
private pass, and tests a fresh fictional conversation plus the synthetic meal
photo. No phone installation/interaction or paid inference occurred in this task.
Permanent host, physical acceptance and evaluator distribution remain open.

## Optional full cloud integration - 2026-10-28

Member requests proceeding from the successful synthetic Super VL photo probe to
the whole assistant, retaining Gemma/local or Nemotron/cloud selection in
onboarding and later Settings. Implementation is now in the working tree;
hosting and physical acceptance remain open, not inferred from code or builds.

- Android: explicit provider selection, focused HTTPS broker/private-pass setup,
  Keystore-encrypted no-backup credential and installation UUID stable across
  activation retries. Cloud can start without downloading Gemma. Missing cloud
  credentials fail closed; no silent Gemma fallback.
- Both photo entry points ask before sending the prepared EXIF-free JPEG and
  description. Cloud outputs use the shared nonempty meal parser/review; a
  separate empty-item clarification parser can display a chat question but never
  create an empty photo review. Nutrition and Save remain host-owned.
- Typed chat/drafts, host-required record tools, confirmations/declines and draft
  portion/food edits use bounded `/v1/assistant` tasks. Record-read replies stay
  local; cloud history centrally excludes every assistant-authored/evidence turn
  to avoid replaying vault facts. Only the new message and at most nine earlier
  member-authored messages are disclosed as cloud context. Selected draft-label
  corrections use the existing replacement validator and require review.
- Cloud-mode voice uses explicit Android on-device speech recognition (API 31+
  and installed language/service), then the existing validated one-use text
  handoff. No audio input to Super VL, no network ASR fallback, no Gemma download
  for this route. Availability and controls still need target-phone acceptance.
- Stop/lock/provider changes invalidate delivery and disconnect requests. Final
  review caught and repaired no-engine cancellation leaving `thinking`, missing
  cloud credentials falling back to Gemma, and local read replies leaking into
  subsequent context. Provider switching waits for cancellation off the UI
  thread. No encrypted member record schema or saved data was replaced.
- Broker: `/v1/vision` and `/v1/assistant`, fixed model/endpoint/server prompts,
  strict payload/tool/proposal validation, 40-second inference deadline, no
  retries, and existing durable private-pass/global admission. Real content is
  never logged/persisted. Enable separately with `--allow-cloud-assistant` or
  `METRIA_ALLOW_CLOUD_ASSISTANT=yes` plus verified live mode.
- Final backend suite: 106 tests pass with simulated providers only. Rebuilt
  local Docker image and verified runtime imports; context excludes fixtures,
  documentation and all private state. No publish/deployment/provider call.
- Final Android gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
  passes 760 tests, zero failures/errors/skips. `git diff --check` passes. APK is
  `native-android/app/build/outputs/apk/debug/app-debug.apk` (214,729,387 bytes),
  SHA-256 `4d9136f71aa881bb1f10df124325d10072d6471b66f8c9f4eafb9903215b099d`.
  Final sources include cancellation, confirmation/decline, saved-dish one-use
  handoff, member-only context and clarification-vs-photo-review regressions.
- No Studio instance was available for synthetic preview rendering. New preview
  sources compile in the gate; visual, keyboard/font/rotation and physical
  acceptance remain open. No installation, device interaction, commit or push.

Next bounded work: configure a real HTTPS broker
host with durable quotas/private secrets and issue a small private test pass.
Use the synthetic meal fixture for one full phone round trip before recording.
Host selection, endpoint URL, ingress logging/retention verification and evaluator
APK distribution are still pending. See `cloud-assistant-integration.md` and
`../broker/DEPLOYMENT.md` for the concrete checklist.

## First real Super VL fictional-image result - 2026-10-27

Member executed the prepared probe. Inspected private record
`2f395536-d9b1-4167-bd39-4ccd477fb310.json` (created 22:45:52 UTC), without
another provider call. HTTP 200, complete response, `finish_reason=stop`, draft
schema validated. Embedded request image, paired PNG and pinned fixture hash
match; captured response text and decoded response bytes match.

- All four intended visible foods identified: grilled chicken breast (180 g),
  white rice (180 g), steamed broccoli (120 g), cooked carrot slices (60 g).
  All confidence values medium. These are model guesses; fixture has no measured
  weight ground truth and this result does not establish portion accuracy.
- Explicit uncertainty about scale, hidden oil/butter/sauces, seasoning and rice
  additives. Review required. No calories/macros, save claim or medical advice.
- Recorded local latency 10,828 ms; 1,761 prompt tokens (1,602 image, 0 cached),
  875 completion tokens (658 reasoning), 2,636 total. Approximate uncached cost
  $0.0013158 using the recorded $0.30/$0.90 per-million rates; not an invoice.
- Raw provider envelope (including returned reasoning), request JSON, synthetic
  image and usage remain in private Git-ignored captures. No captured content or
  secret copied into this checkpoint. No additional paid inference was requested.

One simple image succeeded; latency/quality comparison, harder cases, production
consent/privacy and Android/cloud migration remain open. Next bounded task is to
evaluate integration and latency controls using this saved evidence before
spending on further experiments.

## Super VL synthetic vision probe - 2026-10-27

Member requests a food-photo experiment and explicitly authorizes retaining all
fictional inputs/outputs for diagnosis. Added `broker/vision_probe.py`, a pinned
imagegen-generated chicken/rice/broccoli/carrot PNG and provenance/hash manifest.
The vision prompt does not reveal the expected foods or invent measured weights.
Model is `nvidia/Nemotron-3.5-Super-VL`; no arbitrary image path or member upload.

- `--prepare` passed locally with no key, network or live quota admission; full
  JSON and paired image were saved under private `.local-state/nebius-debug-responses`.
- Live mode captures request/image before dispatch, bounded redacted response even
  on HTTP/validation failures, latency, reported usage and approximate USD cost.
  One POST, no retries, 4,096 output-token limit, 60-second transport deadline and
  separate durable ten-attempt ledger. Captures are capped at 20 shared records.
- Full broker suite: 84 tests pass with simulated providers, including request/image
  identity, invalid/truncated output usage, missing usage, HTTP error capture,
  capture preflight blocking network and persistent quota accounting. No live
  Super VL call: operator key is unavailable in the agent's environment.
- This is a desktop evaluation, not Android migration or hosted vision acceptance.
  Member prefers replacing Gemma with cloud; production privacy/consent and
  integration require their own documented implementation and acceptance.

Next: member runs the single command in `broker/VISION-PROBE.md`; inspect the
saved response/usage before any retry. Schema acceptance does not establish food
recognition accuracy, portion accuracy, full assistant behavior or account access.

## Explicit local fictional-response capture - 2026-10-27

Member explicitly requests retaining model responses to diagnose failed probes
without further blind paid calls. Implemented desktop-only `--debug-response`,
valid only with `--live`; default provider, hosted broker and Android behavior
remain content-free. No live call was made while implementing this change.

- Preflight reserves an exclusive file in the fixed, owner-restricted,
  Git-ignored `.local-state/nebius-debug-responses` directory before dispatch.
  Full/unwritable capture storage fails before the provider call. Up to 20 JSON
  records; UUID filenames never overwrite an earlier capture. No arbitrary path.
- Opt-in adapter callback captures at most 65,536 response bytes before model
  validation, including bounded non-200 bodies and invalid content. Default
  non-200 path does not read/log its body. Truncation is explicit. Response text
  and base64 contain the redacted bytes, not the original secret-bearing stream.
- Record includes actual request ID, scenario/model, creation UTC, HTTP status,
  readable response, truncation and final validation/failure plus latency. Keys,
  authentication headers, incoming request bodies/prompts, environment and
  client credentials are excluded. Known API-key echoes are redacted, including
  escaped/mixed JSON strings. Console emits only normal metadata and `debug_file`.
- Capture files may contain untrusted model text/reasoning from fictional data;
  no automatic execution/upload or inclusion in Git/Docker context. Persistence
  of real member data is not enabled. Existing quota database stays untouched.
- Writes use atomic replacement. Short-lived reservation lock is released after
  exclusive file reservation; interrupted reservation recovery is documented in
  README and requires stopping any local probe before removing only the lock.
  Interrupted atomic writes can leave a private sanitized `.tmp` sibling; remove
  only selected stale capture artifacts after stopping local probes, not state.

Final gate: 77 broker tests pass, zero failures/errors, 14.533 s outside sandbox.
Tests use simulated responses/keys and assert opt-in isolation, preflight-before-
dispatch, rejected-output retention, metadata/path, secret redaction, response
bounds and default no-body-read behavior. Independent review finds no blocker;
Docker image builds and `git diff --check` passes. No Android changes, phone
operations, real provider call, deployment, commit or push. Raw responses from
earlier attempts cannot be recovered retroactively. Next member-run nutrition
probe should include `--debug-response`; inspect its local artifact before any
further retry or inference-contract change.

## Member nutrition selection failure and structured-output trial - 2026-10-27

The next member-run fictional `nutrition` call reports
`invalid_provider_output` with fixed diagnostic
`response_selection_sections_invalid`. It did not pass. The response reached
section validation after envelope/model/finish/usage and basic selection checks;
the diagnostic covers wrong array shape, size, type, name, unshared section or
duplicates. No exact returned section, raw response or billed usage was shared,
so do not infer a specific wrong field or nutrition calculation error.

The existing request asked for its selection JSON in prose only. Prepare a
Lightning-only `response_format` JSON schema plus explicit field-name guidance,
using the official [structured-output API](https://docs.tokenfactory.nebius.com/ai-models-inference/json).
Schema must restrict sections to supplied category names, bind request/language,
disallow extra fields and preserve host-side strict validation. Do not fix the
output after generation or force the expected sample answer. Keep the model's
section subset/guidance decision and expected-selection check, 512 output tokens,
20s timeout, no retries and unchanged consumption accounting.

Provider documentation is not evidence of this exact account/model supporting
the schema. Offline tests/review precede one separate member-run nutrition retry;
schema acceptance and nutrition success remain open. No automatic paid call,
quota reset, Android change or real-record transmission is authorized by this
diagnostic follow-up.

Implemented exact-Lightning structured format plus schema in system text. Five
required fields bind identity/language/version and restrict category names to
the supplied summaries; empty/subset choices remain possible. Existing strict
validator still rejects duplicates, wrong types and unshared categories. The
request keeps the exact user preview, thinking off, 512 tokens and 20s timeout.
Other models' request shapes stay unchanged. HTTP rejection diagnostics now
distinguish fixed request/auth/rate/upstream categories without reading/logging
the raw upstream error or retrying. Full broker suite passes 69 tests with zero
failures/errors in 14.592 s outside sandbox, Docker build and diff check pass.
The weight pass above used the prior prompted-output profile; qualify all five
scenarios on the new structured profile before demo acceptance. No agent-run
external inference, real-data transfer or deployment occurred.
Independent reviewer found no material issue in the schema/diagnostic change.

## Member-run Lightning weight probe passes - 2026-10-27

After the offline hidden-key/configuration syntax check returned valid true and
network_requested false, the member ran the corrected fictional weight probe
and shared this sanitized result: mode `nebius`, model
`nvidia/Nemotron-3_5-Lightning`, scenario `weight`, validated true, latency 765 ms,
397 prompt tokens, 76 completion tokens, 473 total tokens. This is the first
member-reported successful real-provider contract/expected-selection check.
It is one request, not a latency benchmark or all-scenario acceptance. No key
or raw response was shared. The result followed the exact-model thinking-off
adjustment; the initial generic failure's cause remains unconfirmed.

Correction to the earlier local-count interpretation: lifetime_requests=1 in
one inspected local database was insufficient to establish the total number of
calls reaching the provider. The member's usage screenshot has separate input
and output billing rows, which themselves do not establish request count.
Do not claim a reconciled total or exact currency charge from these observations.

Next bounded checks: separately member-run fictional `nutrition` (partial and
missing coverage), `combined`, `empty`, and `general`, preserving the same state
and finite probe budget. No automatic batch/retry or new agent-run paid calls.
Hosted broker/TLS/persistent storage, judge activation in Android and release APK
remain open. No phone/member records or public deployment are involved in this
desktop evidence checkpoint.

## Local failure before the second dispatch - 2026-10-27

Member reports `configuration_or_probe_state_invalid` on the next manual run and
asks for a clearer explanation of the work. This remains a desktop connection/
contract smoke with invented data, not a phone demo, judge activation or hosting.
Read-only inspection of the dedicated probe DB reports schema v2 with the
expected clock columns and lifetime_requests=1. No credential/digest/content was
read or printed. Thus the reported retry has no second admitted dispatch in
this state; its exact local cause cannot be inferred from the generic error.

Added `--check-key`: masked key/configuration syntax validation without network
or accounting changes, and fixed stage labels for live configuration/storage
failures. No key is persisted or displayed. A successful local syntax check does
not prove account authentication or credits. Focused tests exercise valid/invalid
hidden input and reject any adapter/store access during the check. No agent-run
external inference or quota reset. User should use the offline check before any
further paid retry; preserve existing state and its first-call consumption.
Final broker gate passes 65 tests with zero failures/errors in 13.897 s outside
the restricted sandbox. Diff check passes; no Android source/build/install.

## First member-run Lightning probe rejected - 2026-10-27

Member copied account console configuration: model
`nvidia/Nemotron-3_5-Lightning`, base URL
`https://api.tokenfactory.nebius.com/v1/`. After hidden local key entry, the member
ran `--live --account-verified --scenario weight` and reported fixed error
`invalid_provider_output`, mode `nebius`, validated false. This is a failed live
probe, not Nemotron/demo acceptance. Under the current adapter this error is
reached after an HTTP 200, but the report does not establish which response
validation failed, or actual billed usage. No key or raw response was shared.

Add fixed-code diagnostics for the failed validation stage, preserving
strict identity/schema/usage checks, timeout/token limits and no-retry policy.
Possible completion truncation, model mismatch, absent content and schema
failure must be distinguishable without emitting raw output or reasoning.
No cause is established from the original generic error; do not claim a
reasoning cause for this call without evidence. One separately member-run
retry after offline validation is needed because no response body was retained.

Compatibility evidence found independently: the official Nebius physical-AI
[verification report](https://github.com/nebius/nebius-physical-ai/blob/main/docs/workbench/token-factory-deprecation-verification.md)
records Lightning using 506/512 output tokens for reasoning and truncating JSON,
then producing complete output with `chat_template_kwargs.enable_thinking=false`.
The [NVIDIA model card](https://huggingface.co/nvidia/NVIDIA-Nemotron-3.5-Lightning-30B-A3B-BF16)
documents that exact control and default-on reasoning. Request direct output
only for the exact selected Lightning ID; keep 512 output tokens, 20s timeout,
strict schema/identity checks, and single-call/no-retry behavior. Other models
keep their original request format. This is a source-supported compatibility
adjustment, not proof of the member's initial failure cause or a live pass.

Implemented the model-specific direct-output request and allowlisted diagnostic
codes. Probe reports only a fixed validation stage, never raw content/keys or
reasoning. Defaults now match the member's exported Lightning configuration.
Final broker suite passes 62 tests, zero failures/errors in 14.061 s outside
the restricted sandbox; independent review finds no actionable issue and
`git diff --check` passes. Updated Docker image builds. No agent-run live call,
token/timeout increase, automatic retry, deployment or Android change. Member
retry on this corrected source remains pending.

## Judge backend and live-probe preparation complete - 2026-10-27

Member authorizes preparing private evaluator passes, persistent consumption
limits, a fictional Nemotron probe and broker hosting preparation. Implementation
is complete for this backend slice; external inference, hosting and Android
judge UI/APK integration are not accepted or performed.

- Passes: operator CLI creates a high-entropy secret in a restricted file;
  one-installation activation is atomic/idempotent, has no public provisioning
  route, and supports revocation of child credentials and pending results.
  Default allowance: 50 total attempts per pass, 20/day and 3/minute; global
  100/day, 1,000 lifetime and two concurrent (one per credential). Quotas are
  reserved before dispatch and survive restart/day rollover; failures do not
  refund them. v1->v2 SQLite migration carries forward retained global usage;
  already-pruned historical usage is not reconstructable.
- Live/hosted HTTP accepts only server-owned sample scenario names plus request
  IDs. Arbitrary question/summary/photo input cannot reach the provider on this
  path. Five invented English scenarios exercise weight, partial nutrition,
  both, empty and general information. Strict schema and expected selection
  checks both run. The real member vault remains disconnected.
- Explicit probe uses a masked prompt or broker environment secret, never a
  key argument. Offline `--check` performs no network request. `--live` requires
  operator account verification acknowledgement. Separate durable probe state
  allows only 10 attempts total/day; temporary credentials are revoked, even
  on failure. One provider POST, no retries, 512 output tokens/20s as before.
  Safe validated usage and latency are reported without raw content or keys.
- Hosted entry/Docker image require exact hostname, trusted HTTPS proxy and
  pre-initialized persistent state. No new production Python library dependency.
  Runbook explains proxy log/secret controls, one-replica POSIX SQLite storage,
  restart acceptance and Render Free's incompatible ephemeral filesystem.
- Member asks about Nebius hosting: public official quickstart confirms AI Cloud
  Serverless Endpoints can host a CPU nginx/container with managed HTTPS. This
  differs from Token Factory inference endpoints. Compute/disk billing and
  availability of credits/storage/proxy contracts remain unverified; no resource
  was created. Container is provider-independent, not a verified Nebius deploy.

Final verification: 54 broker tests pass with zero failures/errors in 14.485 s,
including legacy process restart/replay, pass activation/races/revocation,
quota migration and midnight persistence, fictional HTTP and mocked live-probe
first-run/recovery/budget exhaustion. Run outside the sandbox because its local
socket/temporary-file restrictions prevented valid baseline execution. Both
independent review passes found no material defect. Docker image
`metria-broker:judge-demo` builds successfully; final Linux smoke passes private
state, activation, sample HTTP, store reopen retaining quota, and revocation.
Container offline check correctly reports no key and no network request. No
container/service is left running by smoke checks (`docker run --rm`).

`git diff --check` passes. Android unchanged; no Gradle gate, install, phone
interaction, member-data transfer, real inference, deployment, commit or push
claimed. Preserve the unrelated untracked Gradle daemon properties file.

Next bounded task: securely configure the operator's actual Token Factory
key/model/endpoint; verify balance currency, expiry through December 15 and a
spending bound, then run ONE fictional weight probe. No values were exposed by
the presence-only inspection: all three variables are absent in this process.
After compatibility/cost evidence, select hosting with verified persistence and
TLS; implement and physically verify Android activation/sample rendering before
publishing a prepared APK. The code/runbook is in `broker/README.md` and
`broker/DEPLOYMENT.md`.

## Operator-funded hosted demo direction - 2026-10-27

Member proposes allocating the reported 25 EUR credits to minimal tests, video
and judge trials. Primary intended delivery is a prepared APK plus an operated
HTTPS broker holding the author's key; judge-owned credentials become the
self-run alternative. Account balance/currency/expiry, bounded budget, durable
abuse controls and hosting still need verification. See the current direction
in `nebius-account-and-secrets.md`. This is not deployment, a live test or
authorization for unlimited provider spending.

## Judge self-run requirement - 2026-10-27

Member clarifies that a judge must be able to supply their own Nebius key and
run the demo. The live-probe work below must become a documented reproducible
entry point, not a one-off author-only script. The judge operates the local
broker with their key; no key in Android, no author account dependency and no
real vault input. Acceptance and setup requirements are in
`nebius-account-and-secrets.md`, section Judge-owned credentials. Implementation,
fresh-checkout verification and visible demo integration remain open.

## Fictional live-probe readiness audit - 2026-10-27

Member requests a review before a real Nemotron test with fictional data and
before recording the demo. This audit makes no external inference call and
does not enable the phone, real-data sharing, deployment or publication.

- Re-read the broker, provider adapter, strict contracts, account runbook and
  cloud checkpoints. The adapter already supports one bounded HTTPS completion
  call and injectable fake-provider integration. The CLI still constructs
  `LocalBroker` without a responder: setting environment variables cannot turn
  `serve` into a live service. Android cloud contracts/rendering remain isolated;
  no production screen/network caller references them.
- Presence-only process inspection reports `NEBIUS_API_KEY`, `NEBIUS_BASE_URL`
  and `NEBIUS_MODEL` absent. This says nothing about credentials in another
  shell or secret store. No secret values or other environment fields read.
- Fresh complete broker gate: 29 tests pass, zero failures/errors, Python
  standard-library runtime. The sandboxed run failed on local permissions and
  networking; the identical suite passed outside that restriction in 10.415 s.
  Tests use temporary state and fake loopback providers, never Nebius.
- `git diff --check` passes before this documentation update. No Android change,
  build, install or physical acceptance claimed. Preserve the pre-existing
  untracked `native-android/gradle/gradle-daemon-jvm.properties`.

The next bounded implementation is a separate, explicitly enabled, one-shot
fictional-data probe. Keep the default broker synthetic. Generate the request
from code-owned English sample scenarios, fresh request identity and current
valid seven-day dates; accept no vault, chat, image or arbitrary request input.
Use the existing adapter and strict validators. Do not log the key, raw upstream
errors or response bodies. Report safe model/endpoint, elapsed time, validation
outcome and bounded usage metadata; currently the adapter validates usage but
discards it, so safe metrics need an explicit implementation decision.

Before the first external request, verify Token Factory account/credits, exact
account-visible NVIDIA model ID and endpoint, current prices/terms and an
enforceable spending bound. Inject the development key outside chat/source/APK.
The official Nano cookbook is a candidate reference, not account availability
evidence. The adapter accepts only the two documented default/US-central hosts.

Compatibility checks for the first live result: model ID matches exactly;
single terminal choice; JSON content without Markdown/prose; request/language
binding; supplied sections only; allowed guidance and consistent bounded usage.
The request currently asks for JSON through instructions only, without
`response_format`; verify model support before adding JSON-schema output.
Validate reasoning/output-token behavior against the existing 512-token limit
and 20-second timeout; do not silently widen limits or retry a paid failure.

Start with one weight-summary case. If it passes, run separately bounded cases
for partial nutrition, both categories, empty records and general balanced-meal
information. Independently assert expected selection, not just schema validity:
the Python contract does not itself prove relevance. Use the existing Kotlin
renderer when testing the final displayed answer. Nemotron selects sections and
guidance; Metria renders factual text and numbers from the submitted snapshot.
Do not present this as free-form model advice, tool execution or persistent
cloud memory. A successful desktop probe is not a working phone integration or
judge-accessible deployment; those remain separate demo-delivery tasks.

Sources rechecked: [Token Factory quickstart](https://docs.tokenfactory.nebius.com/quickstart),
[structured output](https://docs.tokenfactory.nebius.com/ai-models-inference/json),
and [official Nano cookbook](https://github.com/nebius/token-factory-cookbook/blob/main/models/nemotron/nemotron3-nano-30b.md).
Provider account access, spending controls and live compatibility remain open.

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
