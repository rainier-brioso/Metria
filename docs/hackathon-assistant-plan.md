# Metria assistant and Nebius hackathon plan

**Status:** proposed delivery scope, 2026-10-02. The member selected designing
optional real-data cloud assistance with explicit consent. Actual sharing,
WhatsApp integration, deployment, and public release are not enabled.

## Direction

Make Metria feel like a capable personal assistant through continuity, useful
follow-ups, private dish memory, and clear action outcomes. Keep the encrypted
phone vault as the source of truth and meal output as a reviewed draft.

The member wants to finish the local experience and then integrate Nebius for
the Nebius x NVIDIA Global AI Hackathon. Local completion must mean a bounded,
tested demonstration slice rather than every release milestone: a working
Nebius feature is required before submission, not merely a future roadmap item.

## Verified event requirements

Checked on 2026-10-02 against the [event overview](https://nebiusglobalaihackathon.devpost.com/)
and [official rules](https://nebiusglobalaihackathon.devpost.com/rules).

- Deadline: October 30, 2026, 10:00 PDT (18:00 Europe/Madrid).
- The working application must run on Nebius Token Factory or Nebius AI Cloud
  and use at least one NVIDIA open-source model.
- Recommended track: Best Apps and Agents. Personal AI is an alternative if
  the demonstrated scope matches its broader personal-assistant requirements.
- Deliver a working demo/test build, public code repository with an open-source
  license and setup instructions, and a public YouTube demo of at most three
  minutes. Document substantial changes since August 26 for this existing project.
- Provide feedback on the Nebius/NVIDIA tools used. Keep judge access available
  through the end of judging, December 15, 2026.
- Do not assume a decorative API call or roadmap satisfies the requirements;
  Nebius must perform a useful, visible part of the working demonstration.

Publishing the repository, choosing its license, deploying a service, and
submitting the entry are separate actions requiring member authorization.

## Local assistant slice

**2026-10-11 experiment:** the member requested an English-only test of a local
Jev-like decision model to avoid Gemma on simple record questions. See
[decision-model test plan](decision-model-test-plan.md) for Von feasibility,
rules/Gemma comparisons, proposed phone thresholds and go/no-go gates.
This is planned work, not measured speed or completed Android acceptance.

Use the approved assistant screen and central capture action. Avoid a navigation
redesign while proving the interaction. The member chose English-only testing
and video for the demo on 2026-10-04. Spanish support remains implemented; its
broader acceptance is outside the immediate demo scope. Prioritize these journeys
in English:

| Journey | Expected behavior | Required evidence |
| --- | --- | --- |
| Ask about recorded progress, then change period | Short grounded answer; resolve the follow-up and read fresh evidence for the new period | Gate B continuity and Gate C factual binding |
| Describe a meal, then review it | Editable draft; focused corrections; deterministic nutrition; explicit Save | Draft/review/save, cancel, and no duplicate writes |
| Photograph a repeated dish from a new angle | Explainable private suggestion which can be ignored or corrected | Repeat-new-photo scoring, separately from exact-photo fallback |
| Stop, close, relock, and reopen | No late reply, duplicate request, or automatic replay; restore only what the current retention policy promises | Phone cancellation, restart, and restoration checks |

Assistant character comes from remembering conversational intent within the
existing bounded session, asking a specific clarification when needed, and
offering a relevant next action. Do not add generic trailing questions, implied
continuous monitoring, or judgment about food and weight. Longer-term memory
uses confirmed dishes and approved local correction signals, not invented facts
or indefinite chat retention.

Native weight tools remain gated on durable idempotency and visible Undo/Edit.
Do not promote unfinished Gate D behavior for the demonstration. Voice is a
candidate only after verifying that its selected recognition path preserves the
on-device contract; system speech input is not proof of offline processing.

For each material UI change, first define hierarchy and idle/loading/success/
empty/partial/error/cancellation/restoration behavior, then update the relevant
approved flow and screen contract. Keep Send reachable with the keyboard open,
use shared semantic components, and verify rotation, enlarged text, and TalkBack
on the physical phone under `docs/design/QUALITY-GATES.md`.

## Nebius assistance and demonstration

**Judge reproducibility requirement, 2026-10-27:** a judge must be able to run
the fictional-data demo with their own Nebius Token Factory key, endpoint and
available NVIDIA model, following README instructions from a fresh checkout.
The evaluator operates the local broker and supplies its secret privately;
the APK and repository never contain the key. No author account, credits or
private workstation state may be required. See `nebius-account-and-secrets.md`
for the acceptance contract. This is a requirement, not a completed live test
or Android integration.

The member chose optional real-data cloud assistance with explicit consent and
a revised privacy contract. See [optional-cloud-assistant-spec.md](optional-cloud-assistant-spec.md)
for the concrete first scope: a default-off, app-initiated request relay,
previewed question and selected seven-day summaries, no remote vault tools,
and validated read-only answers. Provider retention and service identity are
implementation prerequisites. Develop and demonstrate using fictional data.

Create a clearly labelled fictional sample profile, isolated from the member
vault, chats, images, saved dishes, and diagnostics. Nebius/Nemotron can answer
questions about bounded sample summaries. This provides a substantive comparison between
local and hosted assistance while keeping the production local privacy promise.

The intended flow is:

```text
explicit sample-demo entry
  -> isolated fictional conversation and bounded sample tools
  -> authenticated service holding the Nebius credential
  -> NVIDIA Nemotron hosted on Nebius
  -> host schema/factual validation
  -> visible read-only sample answer
```

Never inherit the real conversation when switching into the sample demo. The
sample path must have no capability to read or mutate the real vault. Keep API
keys outside APKs and source control; limit requests, tool rounds, response size,
and time. Errors, cancellation, and offline behavior must be visible and must
never trigger an automatic cloud fallback from the real local assistant.

[Nebius's official Nano cookbook](https://github.com/nebius/token-factory-cookbook/blob/main/models/nemotron/nemotron3-nano-30b.md)
shows a hosted Nemotron candidate with structured output and function calling.
Verify the account's live model catalog, regional endpoint, tool behavior, and
pricing before selecting a model; do not freeze a guessed model identifier or
claim it has been validated. Preserve deterministic nutrition and member review
for hosted output too. Record provider/model, latency, failures, and cost using
synthetic requests; no real prompts or records belong in telemetry.

Implementing the selected design requires shipping privacy disclosures, verified
processors and retention, explicit bounded sharing, revocation/deletion, and
the acceptance matrix in the cloud specification. External-assistant M7
prerequisites still apply to external integrations. No real photos are included.

## WhatsApp decision

Defer a WhatsApp bot from the initial hackathon slice. A conversational surface
does not supply continuity, reliable tools, or memory by itself, and messages
sent outside Metria introduce a separate privacy boundary. A future feasibility
task must verify the current official WhatsApp platform capabilities and terms,
identity binding, delivery and retention behavior, phone-offline behavior, and
proposal handoff. No external conversation may directly save to the local vault.

## Proposed schedule and acceptance

| Target | Bounded deliverable |
| --- | --- |
| October 2–8 | Verify the chosen cloud boundary's provider/identity requirements; run the consented 8-novel/4-new-repeat photo pilot; record local assistant failures in the four journeys above |
| October 9–15 | Fix the highest-impact local failures and verify the demo journeys; begin the isolated Nebius slice once the boundary is decided |
| October 16–22 | Demonstrate useful live Nemotron behavior; verify schema rejection, factual binding, cancellation, timeout, and isolation from the real vault |
| October 23–29 | Rehearse phone demo, package test build/setup instructions, prepare sanitized repository and video, document measured limitations |
| October 30 before 18:00 Madrid | Submit with a working Nebius feature and all required materials |

Keep the varied-photo pilot under `vision-benchmark-protocol.md`: novel meals,
new photos of repeats, and identical-photo reuse have separate denominators.
Report accuracy, edits, latency, and thermal behavior separately. A small pilot
does not establish daily-use reliability, and a polished video does not close
M0, M4 B–E, M5.6, or M6 acceptance gaps.

Implementation gates remain the complete Android unit-test/build command,
`git diff --check`, approved in-place installation, and member-observed physical
checks. Do not clear app data or automate biometrics. Public examples must use
fictional records and permitted imagery.

## Current checkpoint and next bounded task

**2026-10-03 continuation:** the local assistant slice is committed as
`c00b9d0` after the 283-test gate and targeted latest-meal phone pass. The
Nebius request/consent/response foundation and a localhost synthetic Python
broker now pass fictional-data gates. SQLite preserves credentials/replay/quotas
across process restart; see [Nebius checkpoints](nebius-assistant-checkpoints.md)
and [broker runbook](../broker/README.md). The fake-provider adapter now passes;
account/region/model, processor terms and spending controls are the next setup
prerequisites. See [account/secrets setup](nebius-account-and-secrets.md). No live
provider or phone cloud connection is enabled; local physical,
hosted and real-data gates remain open.

### Initial planning checkpoint (historical)

Repository inspection found HEAD `778f457` and no tracked modifications; only
`.codex-remote-attachments/` was untracked. The prior 248-test build and in-place
installation are recorded evidence in `m4-assistant-checkpoints.md`; no build,
installation, or new phone acceptance was performed for this planning change.

Next task: exercise the local conversation journeys and consented varied-photo
pilot, and verify provider retention/authentication for the selected optional
cloud design. Record actual failures before selecting the next Android patch. This
document leaves all unfinished milestone acceptance gates open.
