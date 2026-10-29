# Metria demo video: recording script

Target: 2:28, English voice and English subtitles. Timing is an editing guide,
not a recording already verified against these subtitles. Align the SRT to the
actual footage before export. Keep the final upload below three minutes.

Start with the working app. Use only fictional photos, records and conversation.
Never show the Nebius key, evaluator pass, access credential, account identifiers
or private diagnostic bodies. The fake meal can use the existing generated image
at broker/fixtures/synthetic-meal.png. Do a Docker rehearsal before filming.

Opening pitch: control over both personal records and where AI runs. Metria
offers on-device Gemma and optional Nebius cloud assistance over local records.
Prove the choice briefly in Settings, then demonstrate the photo draft and local
record answer. Reviewed drafts and conversational nutrition already exist in
other apps; they support our story but are not by themselves unique advantages.
Do not claim clinical accuracy, proven demand or superior estimation accuracy.

### Fitia comparison: research notes, not spoken competitor advertising

Checked 2026-10-28 against Fitia's own published material:

- Fitia has a free tier. Its help page lists photo logging, voice logging and
  Fitia Coach as Premium. Do not describe the entire app as paid.
  [Free versus Premium](https://fitia.app/help/articles/free-vs-premium-fitia/).
- Its privacy policy says AI features may transmit photos, messages and personal
  context to third-party AI providers. This supports a comparison about remote
  AI processing, not a claim that every Fitia function requires cloud or that
  Fitia sells data. [Privacy policy](https://fitia.app/privacy-policy/).
- Metria's concrete difference to demonstrate is locally stored encrypted
  records, on-device Gemma after initial setup/downloads, no Metria account or
  subscription required for that local mode in this prototype, and explicit
  optional cloud processing. Do not promise future pricing, universal device
  compatibility, or free Nebius usage. Optional cloud sends disclosed content
  externally and consumes provider credits.

Suggested positioning: “Your nutrition assistant, under your control. Local AI.
Private records. Cloud when you choose.” The video can make this positive claim
without naming Fitia. Show the local/cloud choice so the benefit is visible;
identify the photo sequence as cloud processing. Privacy is not an unqualified
claim that all data stays on the phone in cloud mode.

## Shot list

| Time | Screen/action | Short on-screen label |
| --- | --- | --- |
| 0:00–0:07 | Brief Settings local/cloud choice, then cut to the meal photo; begin the interaction under the pitch | Local AI. Private records. Your choice. |
| 0:07–0:12 | Show photo preview and explicit cloud confirmation; analyze | NVIDIA Nemotron · Nebius Token Factory |
| 0:12–0:20 | Actual returned draft; show foods, portions and uncertainty | AI draft · Review required |
| 0:20–0:24 | Explicit Save, using fictional records only | Saved on this phone |
| 0:24–0:30 | Ask “How many calories were in my latest meal?” and show actual answer | Answer grounded in saved records |
| 0:30–0:50 | Hold answer, then show saved meal/nutrition details; values must match | Local records · Deterministic calculation |
| 0:50–1:10 | Settings choice, then one local read if routing is verified; simple model-role labels | MiniLM: local routing · Gemma: local assistance · Nemotron: cloud |
| 1:10–1:30 | Short clean recording of Docker command, hidden-input prompt and verified URL; hide pass paths if distracting | Local broker · Your own Nebius key |
| 1:30–1:48 | App visible with simple three-box overlay: App → Local broker → Nebius Token Factory | nvidia/Nemotron-3.5-Super-VL |
| 1:48–2:07 | Revisit draft review/portion editing and visible uncertainty | Estimates need review |
| 2:07–2:28 | Show repository instructions only if available in the recorded checkout; finish on app | Reproduce locally with Docker |

The first 30 seconds must contain the actual photo result and calorie answer.
Use clean cuts between actions. If shortening an inference wait, label the cut
“Waiting time shortened”; do not imply the edited duration is measured latency.
If the interaction does not fit naturally, shorten the introduction before
speeding up taps. Let the answer remain readable.

If switching to Gemma requires model loading, record it separately; do not claim
instant switching. Do not film a public GitHub release unless it actually exists.
This video demonstrates local broker operation, not permanent hosted availability.

MiniLM routing belongs to the local route; the current cloud route calls
Nemotron directly. Never label the demonstrated cloud calorie query as a MiniLM
decision. Verify routing for the recorded local request before showing it as
executed; otherwise use an architecture overlay. Do not claim measured speed or
energy savings. Release activation is a separate acceptance gate.

## Voice track

Read these blocks in order. The same wording appears in captions-en.srt.
Natural pauses and screen holds are intentional; do not read timecodes.

**00:00–00:30 — action first**

Your nutrition assistant, under your control. Local AI. Private records.
Cloud when you choose. This is Metria.

I choose a fictional food photo and confirm cloud analysis.

NVIDIA Nemotron returns a draft with foods, portions, and uncertainty.

I review it, then choose to save.

Now I ask: “How many calories were in my latest meal?”

Metria answers from the meal saved on this phone.

**00:30–00:50 — explain the result**

The model selects a tool. Metria reads the local record and checks the answer.

Nutrition is calculated from food data and portions, rather than invented by the chat model.

The assistant proposes. The user decides what gets saved.

**00:50–01:10 — provider choice**

Gemma assists on-device. MiniLM can route supported local questions without invoking Gemma.

Or choose Nemotron through Nebius, which selects tools for cloud chat.

Cloud processes submitted content. Saved records and tool execution stay on your phone.

**01:10–01:48 — how it runs**

For this demo, the broker runs locally in Docker.

You enter your own Nebius API key at a hidden prompt.

The app connects through a temporary HTTPS tunnel, using a private access pass.

The broker keeps the provider key outside the app and enforces request limits.

The cloud model is NVIDIA Nemotron 3.5 Super VL, served by Nebius Token Factory.

It handles the photo draft and chat decisions. Record tools execute on the phone.

**01:48–02:28 — limitations and reproduction**

Photo analysis is an estimate. Hidden ingredients and uncertain portions still need review.

The app validates model output, and saving always requires your confirmation.

Cloud mode needs internet and Nebius credits. This demo has no permanently hosted broker.

To reproduce it, run Docker locally with your own key and follow the setup guide.

Metria brings meal review and record-grounded answers into one workflow.

## Editing handoff

Ready-to-paste narration: [voiceover-en.txt](voiceover-en.txt).
Editor choices and assembly order: [editing-options.md](editing-options.md).

- Import the screen clips first, then record the English voice as separate blocks.
- Import captions-en.srt and move the cues to match the recorded narration/actions.
- Use the short labels from the shot list only when they add context; do not
  overlay them on the app's primary controls or nutrition numbers.
- Keep the app text legible in the final 1080p frame. Subtitles should use a
  dedicated bottom band rather than cover the phone's controls.
- Include a brief “Fictional data” label at the start. No music is necessary;
  if used, keep it below speech volume.
- Watch the complete export once: actual returned values, readable answer,
  synchronization, masked credentials, truthful wait edits, and duration <3 min.
- Upload a public YouTube video early and verify playback/public access. This
  document does not publish or upload anything.
