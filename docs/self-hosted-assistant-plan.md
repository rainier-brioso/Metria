# Optional Ollama and custom compatible assistant: feature plan

Latest scope - 2026-11-10: Ollama-first setup replaces the Strata shortcut and
Qwen presets. Installed-model capabilities guide selection, recommending Photos
models. Custom compatible connections remain. New Ollama default is loopback
port 11434, requiring a PC tunnel or reachable HTTPS. Earlier checkpoints below
are historical; current build/install/acceptance is in M4 checkpoints.

Initial planning checkpoint: 2026-11-09. Implementation authorized 2026-11-10;
Android code is present on the feature branch. No model installation, network
exposure, live inference or phone verification has been performed. Automated
gate status is recorded below; physical/live acceptance remains separate.

Scope revision, 2026-11-10: member confirms there is no Metria PC application
or existing device sync. Discard the mistaken PC-sync/pull-to-sync request.
Support Ollama first and a configurable OpenAI-compatible provider, following
the connection pattern in the local Wealthfolio fork. Strata must be usable
through that custom provider; do not require a new model installation or a
Metria gateway to use an already configured compatible server. The new setup
and transport below supersede the earlier gateway-first recommendation.

## Branch boundary

- Integration branch: `development`, created from `main` at
  `5c68de32ee7ea753154b46fad560315c4579fc0c`.
- Working feature branch: `codex/self-hosted-model`, based on `development`.
- `main` must remain unchanged. Future reviewed integration targets
  `development`, never `main`, unless the member explicitly changes this rule.
- Existing uncommitted work was carried across unchanged. It is not part of
  this feature and must be separated when preparing any later commit.
- No commit, push or merge is authorized by this planning task.

## Recommended direction

Preserve on-device Gemma as default and the existing Nebius connection. Add
Ollama and Custom (OpenAI-compatible) as configurable connection types. Strata
is a custom-provider setup option, not a separate inference protocol. Both new
types can use a shared Chat Completions transport with runtime-specific framing
only where needed. A custom URL can point to a self-hosted or hosted service;
never infer privacy/location from the compatible API or provider type alone.

```text
Android: explicit request + bounded context
  -> Android compatible-provider adapter: shared contract + task framing
  -> HTTPS + configured access authentication
  -> chosen Ollama/Strata/compatible endpoint (TLS proxy when needed)
  -> bounded untrusted model response
  -> Android: session/action validation + local calculation + existing save rules
```

For these new connections, direct compatible API access is the initial design;
Android owns strict response parsing, framing and capability handling. The
existing Nebius broker route stays separate and unchanged. An optional Metria
gateway can remain a later deployment option for centralized admission or
credentials, but it is not a prerequisite. A TLS/authentication reverse proxy
may still be needed for a runtime that serves only unauthenticated HTTP.

## Wealthfolio reference and Strata configuration

Read-only inspection of the local fork at
`C:/Users/ray/Documents/workspace/forks/wealthfolio` found:

- `docs/architecture/custom-ai-provider.md`: explicit URL, optional secret,
  fetched models, conservative capabilities and no implicit provider fallback.
- `crates/ai/src/provider_urls.rs`: root versus `/v1` normalization; rejects
  credentials embedded in the URL, query parameters and fragments.
- `crates/ai/src/ai_providers.json`: distinct Ollama and custom entries, with
  no invented default model for the custom provider.
- `crates/ai/src/provider_service.rs`: discovery and conditional Bearer header.
- `docs/fork-guide.md`: desktop loopback addresses do not reach the PC from
  Android; portfolio sync does not copy provider credentials or create reachability.

Reuse these design patterns in Kotlin, not the Rust/Tauri implementation or
Wealthfolio's financial tools. No other repository is modified. The desktop
guide is evidence of intended Strata configuration, not proof the service is
currently running or compatible with Metria's safety contract.

Proposed per-connection configuration:

| Field | Contract |
| --- | --- |
| Name and kind | Member label plus Ollama or Custom; optional Strata setup preset. |
| Base URL | Explicit reachable HTTPS API root; normalize a root or trailing `/v1` once, preserving reverse-proxy path prefixes. Never default to a public service. |
| API key | Masked and optional at the model API layer; omit Authorization when absent. A separate access layer must authenticate off-phone access if the runtime cannot. Store supplied secrets encrypted with Android Keystore, outside backups/logs. |
| Model ID | Fetch from `/v1/models` on explicit refresh; select a returned ID. Allow explicit manual entry for servers without discovery, but mark it unverified until a compatibility test. No automatic model substitution or download. |
| Task capabilities | Text, tools, structured output and vision tracked separately; unknown means unverified, not supported. Model-list success alone proves none of these. |
| Bounded tuning | Context/output limits and a small allowlist of tested server-specific options; no arbitrary JSON, headers, system prompt replacement or tool definitions. |

Use the same endpoint/model/settings as an existing Strata deployment after the
member deliberately enters its phone-reachable equivalent. Do not copy keys
from another app, scan private config, or import an entire Hybrid SDLC task/runtime
configuration. No service launch, Docker reconfiguration or model download is
part of adding a connection. A later configuration import would need a narrow,
secret-free format and an explicit review flow.

The fork has a task-specific `reasoning_effort: "none"` setting in
`crates/ai/src/import_matching.rs`. This is not a universal Strata default or
proof of Metria compatibility. Test the selected deployment's reasoning options
with fictional fixtures before adding a bounded preset; never show reasoning
content as a final reply or let it consume the entire structured-output budget.

For custom responses, bind endpoint/configuration revision, selected model and
task to an immutable local request. Chat Completions responses are not Metria's
Nebius envelope and need their own parser; do not invent a provider identifier
in the remote response or relax the existing Nebius checks. Process only the
expected final content/tool format, reject truncation/multiple or conflicting
actions and validate against the shared contract before host dispatch. Missing
usage is unknown, not zero; it must not weaken content or token-budget limits.

## Privacy and authority

Self-hosted is off-phone processing even when both devices are on home Wi-Fi.
The request and response are readable by the gateway/runtime and machine owner.
A rented GPU server also introduces its infrastructure operator. Do not call
either mode on-device, or promise that data never leaves the phone.

Proposed disclosure: "Your selected messages are sent to the configured service
for processing. Your records remain encrypted on this phone. The server
operator can read requests. You can keep using Metria on this phone instead."
Photo support needs a separate explicit photo disclosure before it is enabled.
This proposed copy does not amend the approved production privacy contract yet.

For the first slice, reuse the current member-authored-only history limits:
10 messages, 600 characters per message and a 16 KiB request body. Exclude
record-derived assistant replies, local tool results, vault keys, biometrics,
diagnostic content and bulk records. A local read can be selected remotely but
must execute and render on the phone without replaying its result to the server.

Shared policy/tools remain in `shared/assistant-contract.json`. Returned JSON
does not authorize anything: existing task allowlists, argument validation,
session/request freshness, grounding, deterministic nutrition and reviewed meal
Save remain authoritative. Preserve the existing special reversible/idempotent
weight-action rules; add no new remote write authority.

No automatic fallback between phone, self-hosted or cloud. A provider change
cancels active delivery, invalidates its generation and affects future work.
Stopping cannot recall content already sent or guarantee immediate server abort.

Disable request/response logging and external tools at every server layer;
inspect runtime caches, crash dumps, swap and any persistence before claiming
no content retention. No production fictional-response capture. Provision the
model separately: a phone request must never trigger a model download or cloud
substitution. For Ollama, disable cloud features with `OLLAMA_NO_CLOUD=1`,
restart, and test inference with outbound access denied after model provisioning.
[Ollama documents the local-only switch and its network defaults.](https://docs.ollama.com/faq)

## Runtime possibilities

| Runtime | Where it fits | Caveats / initial position |
| --- | --- | --- |
| Ollama | First desktop or single-member server experiment; local API and model management | Local API key values are ignored; provide an authenticated access boundary. Disable cloud features and use a pinned downloaded model. Recommended first adapter. |
| LM Studio | Desktop users who prefer a graphical model setup | Offers LAN serving and optional API-token authentication; downloaded models can run offline. A Metria adapter still needs conformance tests. Good second adapter. |
| llama.cpp server | Small, controlled deployment with quantized GGUF models | Provides chat-completions serving and structured output support; operator owns model/chat-template/build configuration. Useful lean alternative. |
| vLLM | Dedicated serving deployments when concurrency becomes important | Online serving supports compatible APIs; deployment, supported hardware and model-specific formats need evaluation. Defer until measured load justifies it. |

References checked 2026-11-09:
[Ollama compatibility](https://docs.ollama.com/api/openai-compatibility),
[LM Studio offline use](https://lmstudio.ai/docs/app/offline),
[LM Studio authentication](https://lmstudio.ai/docs/developer/core/authentication),
[llama.cpp server](https://github.com/ggml-org/llama.cpp/tree/master/tools/server),
[vLLM online serving](https://docs.vllm.ai/en/latest/serving/online_serving/).

Tool selection, JSON schema, image support, usage fields and chat templates vary
by runtime/model/version. Listing a model or accepting a chat request is not
proof it can safely handle Metria tasks. Pin the tested runtime version, model
artifact/digest, quantization, template and context size. Do not silently repair
malformed model output or remove validation to accommodate weaker models.

## Hardware and operating costs

Do not buy hardware before testing an existing machine with fictional fixtures.
These are exploration bands, not verified compatibility or performance promises:

| Available hardware | Experiment | Likely constraint |
| --- | --- | --- |
| CPU-only, around 16 GB RAM | Small quantized text model, one request at a time | Slow warm-up/generation; quality and timeout behavior must be measured. |
| Desktop GPU, around 8-12 GB VRAM and 16-32 GB RAM | Quantized 4B-8B-class text model at bounded context | Model, context cache and runtime must all fit; photo support may exceed memory. |
| Around 16-24 GB VRAM, 32 GB+ RAM | Larger text model or selected vision candidate | Greater capacity does not establish safe tool use or nutrition accuracy. |
| Apple Silicon with unified memory | Select a model leaving substantial memory for OS and context | Unified memory is shared; do not treat all installed RAM as available model memory. |
| Dedicated home GPU server | Always-available private service for the phone | Electricity, cooling, noise, maintenance and home uplink reliability. |
| Rented GPU server | Trial larger models without purchasing equipment | Recurring compute/storage/traffic cost, operator access and stronger deployment obligations. |

Sizing intuition: 8 billion parameters at 4 bits is about 4 GB of raw weights
alone (decimal). Actual allocations add quantization metadata, runtime buffers,
context/KV cache and possibly a vision encoder. A 4 GB model file does not mean
a 4 GB GPU is sufficient. Longer context and concurrent requests add memory.
[Ollama documents context memory growth](https://docs.ollama.com/context-length);
[LM Studio recommends at least 16 GB RAM on Windows](https://lmstudio.ai/docs/app/system-requirements).
The bands above are engineering estimates, not those vendors' model guarantees.

Select candidates by English/Spanish quality, strict JSON/action conformance,
latency, memory and model license. Establish a text baseline first; evaluate
vision separately. Do not promise a model or latency target until benchmarked.

## Connectivity choices

| Access | Benefits | Caveats |
| --- | --- | --- |
| Same home LAN | No internet required after setup/model provisioning | PC must be awake; guest Wi-Fi isolation, firewall, DHCP and certificate setup can prevent access. Wi-Fi alone does not encrypt application traffic. |
| Private VPN to home server | Reach the same machine while away without a public model port | VPN provisioning, access rules, battery and home connectivity. Managed VPNs add an account/control-plane dependency. Retain HTTPS and gateway authentication. |
| Public HTTPS gateway | Reachable without a VPN client | Public attack surface, rate limits, TLS/domain renewal, patching and hosting. Defer from the first slice. |

Recommend private LAN access first, private VPN later. Keep unauthenticated model
APIs on loopback, expose only an authenticated HTTPS access layer, and restrict firewall
access. When gateway and runtime are on different hosts, require a separate
authenticated encrypted hop; loopback HTTP is only for the same host.

TLS is a real setup task. Prefer a trusted certificate and hostname. A private
CA would require a deliberately scoped Android trust/pairing design and renewal
path; never use trust-all TLS, hostname-verification bypass or global cleartext.
[Android supports scoped trust configuration.](https://developer.android.com/privacy-and-security/security-config)
A VPN is not a replacement for the app's HTTPS policy. Tailscale is one possible
VPN/HTTPS option; its certificate setup publishes machine/domain names in
certificate transparency and requires renewal planning.
[Tailscale HTTPS documentation](https://tailscale.com/docs/how-to/set-up-https-certificates).
Use neutral hostnames without member information. No actual endpoint, credential,
phone serial or pairing material belongs in this plan or the repository.

## Repository work required

Current seams inspected:

- `CloudPhotoAccess.kt` owns encrypted connection settings, local/cloud mode,
  activation, HTTPS transport and cancellation. Introduce explicit provider
  identities and separate credentials/configuration without migrating or
  overwriting existing cloud credentials. Preserve legacy local/cloud defaults.
- `cloud/CloudAssistantRequest.kt` validates a fixed Nebius/model response.
  Preserve it and add a separate compatible-provider parser/adapter binding
  endpoint, selected model, capabilities and contract revision to the immutable
  local request. Reuse common policy and host dispatch, not the Nebius envelope.
- `broker/cloud_assistant.py`, `cloud_vision.py` and `provider.py` deliberately
  restrict Nebius endpoints. Retain those restrictions. Custom Android settings
  must not turn the Nebius broker into an arbitrary upstream proxy.
- Add separate Keystore-backed connection storage and bounded compatible
  transport. Disconnect removes that connection's secret and revokes in-flight
  delivery; external key revocation remains the server operator's responsibility.
  Keep request/concurrency limits independent of Nebius/judge accounting.
- Use existing contract vectors and provider tests, plus negative self-hosted
  transport/capability tests. Do not introduce dependencies without approval.

Local connection metadata includes protocol/contract revision, selected model
identity, verified tasks and limits. Capability changes require rechecking
configuration; a text-only model must not receive an image. Health checks use
no private content; an optional fictional compatibility probe requires an explicit
test action. Capability claims still require validated inference fixtures.

## Proposed member flow and state contract

Settings first; onboarding choice follows once setup is proven. One summary row
shows the selected connection and readiness. Add connection opens a focused
Ollama/Custom choice, then address and optional masked API key. Primary action:
Connect, followed by focused model selection. Strata uses the Custom route with
setup guidance. Keep advanced tuning in a secondary surface. Setup/discovery
does not submit health content; an explicit Test action uses only fictional data.

Success shows the computer/server label and supported tasks. Empty model list or
missing capability explains what to fix. Loading/warm-up is cancellable. Error
distinguishes unreachable server, failed authentication, certificate failure,
unsupported task and rejected output without exposing raw server exceptions.
Partial/uncertain drafts use existing Review. Manual entry remains available.

Provider selection is distinct from connection readiness. No repeated automatic
requests during restoration. Persist verified configuration and selection using
the existing secure-storage pattern; keep entered secrets out of saved UI state. Follow
existing lock/background disposal, preserve safe setup fields across rotation,
and revoke in-flight request authority on disconnect/switch/lock. Test recreation
without replay or surprise submission. No new persistent meal/chat draft policy.

Before UI implementation, update the approved privacy/architecture and design
contracts (`FLOWS.md`, `SCREEN-SPECS.md`) with the selected copy and scope. Use
shared components outside `MemberShell.kt`; keyboard, font scale, semantics and
all applicable `QUALITY-GATES.md` checks remain required.

## Bounded delivery and acceptance checkpoint

The member explicitly chose direct implementation instead of the preliminary
desktop feasibility spike on 2026-11-10. Phase 1 below is superseded as a blocking
prerequisite; live compatibility/performance measurement remains acceptance work.
Implementation uses the existing shared contract, not a changed assistant model
or a new production dependency. Setup instructions: `compatible-providers.md`.

- [x] Create isolated local branches; verify `main` ref unchanged.
- [x] Inspect existing provider seams and document alternatives/caveats.
- [x] Inspect Wealthfolio's custom-provider pattern and revise scope for
  Ollama/Custom/Strata without PC sync or a required Metria gateway.
- Superseded as an implementation prerequisite: fictional desktop-only spike. Inspect the deliberately selected
  existing Strata setup first, or choose an available Ollama setup; record the
  runtime/model revision. For local inference disable cloud and verify
  outbound-blocked inference. Measure cold/warm latency, peak RAM/VRAM,
  Spanish/English tool and schema pass rates. No Android changes or real records.
- [x] Phase 2 implementation: Kotlin configuration, encrypted no-backup storage,
  bounded HTTPS transport, optional key omission, model discovery/manual entry
  and separate compatible parser. URL/schema/action/partial/usage tests added.
  Transport cancellation, credential revocation and live-server behavior still
  need acceptance; implementation is not physical verification.
- [ ] Verify the existing Strata deployment with an explicitly selected reachable
  endpoint/model and fictional Metria fixtures; do not change or restart it.
- [x] Phase 3 implementation: focused Settings setup, explicit consent,
  provider routing/labels and existing text/action dispatch. Live unavailable
  server and Stop/lock/background/switch races remain acceptance work.
- [x] Phase 4 implementation: opt-in compatible vision with per-photo consent,
  existing preparation/parser/uncertainty/Review. OCR remains on-device and voice
  uses the existing on-device recognition-to-text path. No vision model quality,
  physical photo interaction or voice acceptance is claimed.
- [ ] Phase 5: onboarding and optional remote VPN guide after setup acceptance.
- [x] Before Android installation: full unit/debug gate from `docs/TESTING.md`
  and `git diff --check`. Broker production code is unchanged; unrelated existing
  broker test edits are preserved. No androidTest/Compose UI-test infrastructure
  is present; a synthetic connection-card preview compiles with the app.
- [ ] With explicit installation/inspection authorization: in-place installation,
  physical flow/keyboard/rotation/font/accessibility checks, cancellation and
  connection recovery. Record device/Android/build, redacted before/after
  evidence and data preservation. Member performs biometric unlock and saves.
- [ ] Review for integration into `development`; commit/push/merge only with
  explicit authorization. No integration into `main` under the current rule.

Implementation evidence and remaining acceptance are recorded in the checkpoint
below. No production dependency, commit, push, merge or change to main. Existing
unrelated dirty files remain. No model setup, live provider request, phone
installation, screenshot or record write was performed.

### Implementation checkpoint - 2026-11-10

- Completed the Settings/compatible-provider implementation directly as requested;
  the earlier desktop feasibility experiment was not run.
- Independent code review found the fixed Chat header's on-phone privacy claim
  contradicted a remote route; changed it to neutral Your assistant while the
  explicit route label remains. Reviewed transport uses destination-bound keys,
  HTTPS, no redirects/retries, bounds and generation-based cancellation. Schema
  review repaired invalid response-format construction before handoff.
- Automated gate: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
  passes with 785 tests, zero failures/errors/skips and debug assembly. The ten
  new tests cover URL/model configuration, legacy provider identity isolation,
  missing usage, shared tool/schema construction, malformed/truncated/foreign
  outputs, partial drafts/clarification and prepared-image framing. `git diff
  --check` and a whitespace check of the new files pass.
- APK: `native-android/app/build/outputs/apk/debug/app-debug.apk`, SHA-256
  `eeb762f603e89bc381dd2e8f33f34810b64673d5bfad7a77382c189526073d1c`.
  Baseline refs for main/development/feature HEAD remain
  `5c68de32ee7ea753154b46fad560315c4579fc0c`; work is uncommitted.
- Rendering attempt: `android studio check` found no running compatible Studio
  instance. Synthetic preview was not rendered; no visual pass is claimed.
- Physical reference target remains the previously documented Samsung SM-S928B /
  Android 16, but no device was inspected or modified in this task. Installation,
  preservation of its existing data and keyboard/font/rotation/accessibility,
  discovery errors, cancellation and live Ollama/Strata behavior are unverified.
- Next bounded acceptance task: authorize an in-place installation and targeted
  inspection of the built APK, then choose a phone-reachable HTTPS endpoint for
  fictional text/read/draft and optional vision tests. Member unlocks and saves.
  Do not restart an existing service, clear data or alter its keys/configuration.

### Authorized phone installation - 2026-11-10

- Member explicitly authorized pairing and installation. Paired successfully and
  selected the connected Samsung SM-S928B using `adb devices -l`.
- Installed the APK identified above with `adb install -r`: Success. No uninstall,
  data clear or signing-key replacement. Existing record preservation still
  requires member confirmation after biometric unlock.
- Force-stopped and launched `app.metria/.MainActivity`: Status ok, cold launch
  1010 ms. No private screen or record was inspected.
- Full physical acceptance, live provider behavior and accessibility checks
  remain pending. Next bounded task: member unlock and Settings connection setup
  with a phone-reachable HTTPS provider, followed by targeted flow verification.

### Strata preset follow-up - 2026-11-10

- Supersedes HTTPS-only/desktop-loopback rejection above only for exact
  `http://127.0.0.1` local-server/tunnel access. Default Strata URL is the supplied
  `http://127.0.0.1:8080/v1`; default model is
  `qwen3.8-flash-next-coder-iq1_m`. Other cleartext hosts remain prohibited.
- PC metadata confirms this loaded text-only model. Q2/other variants appear
  only when actually returned by discovery; no unsupported model IDs invented.
- Full gate: 786 tests, no failures/errors/skips, debug assembly succeeds.
  Phone port reverse configured; in-place installation and launch succeed.
  See M4 checkpoint for limits and remaining physical/live acceptance.
