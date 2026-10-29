# Optional cloud assistant integration

Shared contract revision (approved 2026-10-28): local Gemma and cloud Nemotron
share one policy/tool source in `shared/assistant-contract.json`, including
argument limits, task allowlists and examples. Native SDK tool classes are
generated from it; the broker reads it. Separate runtime format adapters remain;
both feed the existing host-controlled execution/review path. Conformance tests
verify identical argument acceptance; live model selection still needs testing.

Local fictional-data capture exception (approved 2026-10-28): the operator may
enable the tunnel launcher's `--capture-fictional-responses` flag to privately
retain complete bounded provider replies/errors before validation, plus original
connection errors. All app inputs/context in that run must be fictional. This
does not change hosted/production retention below or permit real-data capture.

Member decision, 2026-10-27: preserve Gemma on-device and add Nebius NVIDIA
Nemotron Super VL as a selectable cloud provider. Offer the choice in onboarding
and permit later changes in Settings. "Integra todo" includes meal photos, typed
conversation, meal drafting and voice input. This deliberately revises the
previous offline-only assistant and photo-exclusion scope.

## Data and actions

Cloud is never a silent fallback after a local error. Cloud requests do not
require a Gemma download. Provider changes affect future work and preserve
encrypted records. Stop active work before switching and revoke stale result
authority. Local conversation retains its existing encrypted retention rules.

Consent discloses that the Metria broker and Nebius receive the selected prepared
photo/description or submitted text plus bounded necessary conversation context.
No bulk vault export, continuous synchronization, remote vault query or remote
Save. Record/action selection remains constrained by existing host validators;
deterministic read replies may remain entirely local.

The cloud action-selection system prompt enumerates the existing tools and exact
arguments for the active task. The model selects a read; the phone authorizes,
executes and renders it locally. Record data need not be present in the cloud
prompt, and a prose claim of no record access cannot satisfy a required read.

Photo preparation resizes and strips EXIF. Cloud upload additionally enforces a
1536-pixel longest side and 2 MiB JPEG bound independently of local decode sampling.
The preview and explicit cloud action
authorize this photo/description only. Label OCR remains local. Photo and text
drafts cross MealProposalParser and the existing Review/Save gate. Catalog
selection and nutrition calculations remain host-owned.

Super VL supports text/images, not audio. Cloud-mode voice uses Android's
explicit on-device speech recognizer (API 31+, installed service/language needed),
then the validated one-use transcript handoff submits text. No audio goes to
Nebius, and no generic/network ASR fallback is permitted. Unsupported devices
offer typing/local mode. Target-phone voice acceptance remains required.

## Access and retention

Provider keys stay on the broker. A private evaluator pass activates an
installation credential encrypted with Android Keystore in no-backup storage.
Public APK possession grants no paid access. Installation identity contains no
vault ID or biometric information. Activation is explicit.

Real photos, text and model responses stay in broker request memory: no content
logs/files/telemetry or synthetic diagnostic capture. Hosting/proxy logs must
likewise exclude bodies, Authorization and pass values. SQLite retains only
pseudonymous access/admission counters. Usage/network metadata is distinct from
content retention. Nebius advertises zero-retention inference; do not claim this
covers every hosting layer/account setting without verification, or describe
server-readable requests as end-to-end encrypted. Synthetic desktop capture
remains a separate authorized exception and is never connected to real requests.

All cloud routes require HTTPS, private credentials and durable quota admission
before inference. One POST per attempt, no retries or automatic fallback. Failed
dispatched calls count. Quotas are request/token reservations, not a verified
currency cap. Stop/lock/background/provider changes revoke local delivery and
attempt cancellation; already sent content cannot be recalled and remote work
may still finish/be charged. Late output must not navigate, append or save.

## Acceptance still required

- [x] Android test/debug build: 760 passing tests; broker: 106 passing tests.
- [ ] Independent security/lifecycle/host-action review.
- [ ] Synthetic onboarding/settings/preview/async UI previews.
- [ ] Physical provider choice and later switch preserving all records.
- [ ] Photo and typed cloud draft/read/edit flows, Stop/lock/late results.
- [ ] Target-phone on-device voice, one-use submission and Type instead.
- [ ] Hosting HTTPS, private keys/passes, disabled body logs, durable quotas
  across restart, and final judge APK endpoint configuration.

Build/code evidence is not physical or hosted acceptance.
