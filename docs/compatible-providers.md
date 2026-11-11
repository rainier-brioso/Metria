# Ollama and custom compatible connections

Implementation branch: `codex/self-hosted-model`. Phone and live-server
acceptance are tracked in `self-hosted-assistant-plan.md`; a built APK alone is
not evidence that a particular endpoint/model has passed.

## Connect from Android

1. Run the desired model server independently. Metria does not launch services,
   install Ollama, download models or read another app's configuration.
2. Make its compatible API reachable from the phone through HTTPS with a valid
   trusted certificate. A desktop `localhost` or `127.0.0.1` address refers to
   the phone when entered there. HTTP is permitted only to exact `127.0.0.1`,
   for a phone-local server or an explicitly connected port-forwarding tunnel.
   Other HTTP addresses and trust-all certificates are not supported.
   Existing unauthenticated local runtimes can stay on PC loopback
   behind an authenticated HTTPS access layer.
3. Open Privacy & settings > Computer or compatible server > Add connection.
4. Choose Ollama (guided/default) or Custom (OpenAI-compatible). Ollama uses
   `http://127.0.0.1:11434/v1` by default for a local server or connected tunnel.
   Enter the API base address (a root or `/v1` is accepted) and an optional key.
   For example, `https://models.example/assistant/v1` retains its proxy prefix.
5. Explicitly load available models. Ollama shows installed models, with Photos
   models first and recommended for meal images. Text-only and unknown-support
   models are labeled separately. Choose a Photos model to enable the optional
   photo choice; each image still needs confirmation. If none are confirmed,
   configure a vision model in Ollama, then load again. You can also enter an
   exact served model ID; missing metadata never establishes photo support. Loading
   models is a metadata request, not inference. It does not send wellness content.
6. Review the sharing disclosure and select Use this connection. Settings saves
   the configuration; server availability and output are checked on actual use.

The Strata shortcut/model presets have been removed. Existing saved custom
connections remain until explicitly changed by the member; no settings or keys
are imported from another app.

For the paired development phone, `adb -s <current-device> reverse tcp:11434
tcp:11434` forwards that phone address to the PC's Ollama port. The ADB connection must
remain available; reconnect/recreate the forwarding after it is lost. This is
a development convenience, not automatic PC discovery or an away-from-home
connection. For independent use, configure a reachable HTTPS endpoint.
Inference uses OpenAI-compatible framing; no Anthropic adapter is required.

## Supported API behavior

- Custom metadata: `GET /v1/models`, with a `data` array containing model `id` values.
- Ollama metadata: `GET /api/tags`, then `POST /api/show` for capabilities of up
  to 24 installed models per load. Further models remain selectable with unknown
  photo support. No inference/model loading is requested. Vision recommendations
  use the explicit `vision` capability, never names or model-family guesses.
- Inference: non-streaming `POST /v1/chat/completions`, structured JSON-schema
  output through `response_format`, one final assistant choice, matching model
  ID and a completed response. Text and photo drafts retain Metria validation.
- The optional API key is sent only as a Bearer header to the configured address.
  With no saved key, no Authorization header is added. Editing with a blank key
  retains the saved key only for the same normalized address; changing address
  never forwards the old secret. Disconnect removes the saved connection/key.
- Reasoning options are off by default. Only enable the setting to turn off
  extended reasoning when that server supports `reasoning_effort: "none"`.
- Photo capability is opt-in and requires a vision model. Each prepared photo
  gets its own sharing confirmation. Label OCR remains on the phone.
  Before upload, optional server metadata is checked for the selected model's
  declared input modalities. An explicit list without `image` prevents upload.
  Missing/unsupported metadata stays unknown; it never enables photo support.
  Ollama checks `capabilities` via `/api/show`; Custom uses declared modalities.
- Voice uses on-device recognition and submits text, not audio. Missing local
  recognition support offers typing rather than network recognition.

Compatibility is narrower than an API's marketing label: servers without the
required structured-output support can reject requests. Truncated, malformed,
wrong-model or unsafe responses are rejected rather than repaired, retried or
sent to another provider. Missing token usage is unavailable, not measured zero.
Model listing and a saved configuration do not prove model quality/readiness.

## Data and cancellation

This mode sends submitted member messages plus bounded recent member-authored
context off the phone. It does not replay local tool results or record-derived
assistant replies. The operator can read requests and responses; hosting/runtime
retention must be managed by that operator. Disable Ollama cloud features when
local-only PC processing is intended. Do not test with real records or photos.

Records, nutrition calculation and save authority stay on the phone. Meal output
is an editable estimate requiring review. No silent cloud/local fallback exists.
Stop, lock, provider changes and disconnect revoke result delivery and attempt
network cancellation. Already submitted content cannot be recalled and server
computation may continue. A failed connection preserves manual entry.

Connection settings/keys are encrypted with Android Keystore in private
no-backup storage. They are not included in recovery archives or content logs.
Revoking a server-issued key is a separate server-side operation.
