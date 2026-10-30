# Metria

**Your nutrition assistant, under your control.**

**Local AI. Private records. Cloud when you choose.**

Metria is an offline-first Android wellness companion for meals, weight and
personal goals. Speak about a meal, describe it or take a photo; review the AI
draft, adjust its foods and portions, and save it to an encrypted vault on your
phone. Ask supported questions about your saved records, then follow your day
and trends through Today, Progress and Goals.

The choice of where AI runs is part of the product: **Gemma on-device**, or
**NVIDIA Nemotron on Nebius Token Factory** after you explicitly choose cloud.

## Install the demo APK (judges)

1. On your Android phone, [download the APK directly](https://github.com/rainier-brioso/Metria/releases/download/v0.1.0-hackathon/metria-0.1.0-hackathon.apk)
   (about 215 MB). Open the downloaded file and allow installation from that
   browser/file manager if Android asks. No GitHub account is needed.
2. Open **Metria**, complete onboarding and unlock the vault. Have Android's
   screen lock and biometric unlock configured. Use fictional data for testing.
3. Choose **Nemotron · cloud** to follow the [broker setup below](#try-nemotron-with-your-own-nebius-key).
   **Cloud does not require downloading Gemma or building the app.** For local
   inference instead, choose Gemma and download/prepare it inside Metria.

[Release notes and SHA-256 checksum](https://github.com/rainier-brioso/Metria/releases/tag/v0.1.0-hackathon).
This is the normal app (`app.metria`), a development-signed debug APK with MiniLM
included. It is not the separate onboarding recording app. Tested on Samsung
SM-S928B / Android 16; other phones are unverified. For updates, install over
the existing app with the same signing identity; do not uninstall to update.

## What makes Metria different

- **A usable local AI path.** After downloading and preparing Gemma on a
  compatible phone, local assistance runs on-device. The current local prototype
  requires no Metria account, subscription or cloud inference credits.
- **Control over cloud processing.** Choose local or cloud during onboarding,
  switch later in Settings, and see the selected provider in Chat, meal entry
  and photo analysis. There is no silent provider fallback.
- **An encrypted record on your phone.** Meals, weight and goals stay in the
  local vault. Optional cloud processes explicitly submitted content; it does
  not become the store for your wellness history.
- **Answers tied to actual records.** The assistant selects a supported action;
  the app authorizes the local read and validates its answer. Model output alone
  cannot establish a saved meal or authorize Save.
- **Estimates you can inspect.** AI proposes foods and uncertain portions.
  Nutrition is calculated from matched food data and quantities, with visible
  uncertainty and explicit review before Save.

Photo logging and conversational assistance support this experience. The central
promise is the combination of local AI, local records and explicit cloud choice.

## A short walkthrough

1. Set up your goal and choose the local or cloud assistant.
2. Add a meal by voice, text or photo. Review the proposed foods and portions.
3. Save only when the draft matches what you intended to record.
4. Ask **“How many calories were in my latest meal?”** to inspect the saved
   estimate through a supported local-record lookup.
5. Explore Today, Progress and Goals for your recorded totals and trends.

Conversational access covers supported tools, not every value on every screen.
Broader conversation across goals and trends remains future work.

## Which model does what?

| Component | Role | Processing location |
| --- | --- | --- |
| Gemma through LiteRT-LM | Local meal, photo and conversational assistance | Android phone |
| MiniLM | Integrated local routing for supported record questions in the current debug/demo builds; uncertain or unsupported requests continue through Gemma | Android phone |
| NVIDIA `nvidia/Nemotron-3.5-Super-VL` | Cloud photo/text meal drafts and conversational action selection | Nebius Token Factory |
| Android on-device speech recognition | Transcribes voice in cloud mode; Nemotron receives text, not audio | Android phone |
| ML Kit | Nutrition-label text recognition | Android phone |
| Metria's deterministic host code | Authorizes tools, reads records, calculates nutrition, validates drafts and saves confirmed edits | Android phone |

Gemma handles the local voice path. MiniLM selects supported local record
operations automatically, with host validation and a Gemma fallback. The current
debug APK includes its model; the person installing that APK has nothing extra
to configure for MiniLM. The cloud route calls Nemotron directly and does not
use MiniLM. See [MiniLM integration](docs/minilm-chat-integration.md).

## Privacy and processing choices

**Local:** member records, submitted prompts, photos and model output stay on
the phone for local assistant processing. Initial model downloads need internet;
core tracking and prepared local inference work offline.

**Cloud:** the submitted text, bounded necessary conversation context, or the
selected prepared photo and description pass through the broker to Nebius.
There is no bulk vault export; record tools and Save execute on the phone.
Cloud inference needs internet and consumes the operator's Nebius credits.
Cloud speech recognition requires an available on-device Android recognizer;
unsupported devices offer typing.

The demo's temporary HTTPS tunnel uses Cloudflare, which terminates TLS. Normal
broker operation does not persist request/response content. Explicit fictional
test capture retains bounded provider responses and errors privately for
debugging; never enable it with real personal data or real conversation history.
This is not a claim about independent providers' data-retention policies.

See [the architecture](docs/architecture.md), [product safety rules](docs/m1-product-safety-foundation.md)
and [cloud integration](docs/cloud-assistant-integration.md).

## Build from source (developers only)

**APK users can skip this section.** It is not needed to run the Docker broker.

The active application is [`native-android/`](native-android/README.md): Kotlin,
Jetpack Compose and LiteRT-LM. The root Expo shell is historical prototype work.

Use Android Studio with Android SDK 35 and configured Java toolchains: Java 17
for app compilation and Java 21 or newer for JVM tests that load LiteRT-LM tool
declarations. From the repository root on Windows:

```powershell
python tools/decision_model/prepare_minilm.py
Set-Location native-android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
Set-Location ..
```

The preparation command uses Python 3 to download the exact public MiniLM
revision, verify its size and SHA-256, and place it in the Android assets before
building. Its approximately 23 MB weight file is excluded from Git but included
in the resulting debug/demo APK. This is a developer build step, not an end-user
download. The production `release` build variant currently leaves MiniLM routing
disabled; activating and verifying that variant remains separate work.

The APK is `native-android/app/build/outputs/apk/debug/app-debug.apk`. Install it
through Android Studio or ADB, select your actual connected device, and use
`install -r` for an update that preserves existing app data. Follow
[the testing and installation runbook](docs/TESTING.md) for device selection,
model setup and physical verification. Biometric unlock remains a user action.

For a separate onboarding recording installation, build `:app:assembleDemo`.
**Metria Demo** uses a different app ID and independent storage; it does not copy
the normal app's records, downloaded model or cloud connection. See
[onboarding recording setup](docs/demo/onboarding-installation.md).

## Try Nemotron with your own Nebius key

**You run the broker on your PC; NVIDIA Nemotron runs on Nebius Token Factory.**
Requests use **your** Nebius credits. The APK contains no shared provider key.
Your phone connects over temporary HTTPS: no USB, PC GPU, public server or router
port forwarding is needed. Python and cloudflared are included in Docker.

### 1. Prepare your PC and key

- Windows with PowerShell and **Docker Desktop running Linux containers**.
- Internet on the PC and phone. They do not need to share a Wi-Fi network.
- Your [Nebius Token Factory API key](https://docs.tokenfactory.nebius.com/quickstart)
  with credits and access to **`nvidia/Nemotron-3.5-Super-VL`**. Use the API key;
  no dedicated GPU endpoint or Nebius server deployment is needed.

### 2. Get the broker and start it

With Git installed, run in PowerShell:

```powershell
git clone --branch v0.1.0-hackathon --depth 1 https://github.com/rainier-brioso/Metria.git
Set-Location Metria
powershell -NoProfile -ExecutionPolicy Bypass -File .\broker\local-docker.ps1
```

Without Git, [download the matching source ZIP](https://github.com/rainier-brioso/Metria/archive/refs/tags/v0.1.0-hackathon.zip),
extract it, open PowerShell in the extracted folder containing `broker`, and
run the last command above. The policy override applies only to this process.
The first run builds the image and downloads its dependencies; allow time.

Enter your **Nebius API key** at the hidden prompt. Wait for
**`HTTPS broker verified: https://…trycloudflare.com`**. Keep that terminal,
Docker and the PC running. The pass is generated automatically; you do not
need to request one from the Metria author.

In a second PowerShell window opened in the same repository folder, view it:

```powershell
notepad .\.local-state\nebius-app-test\app-test.pass
```

### 3. Connect the phone

In Metria, open **Privacy → Assistant mode → Connect Nemotron**:

| App field | What to enter |
| --- | --- |
| **Metria broker address** | The HTTPS URL printed by **HTTPS broker verified** |
| **Private evaluator pass** | The contents of `app-test.pass` |

Accept the cloud-processing disclosure and tap **Connect and enable Nemotron**.
Check that meal/chat screens show **Cloud · Nemotron**. The Nebius API key goes
only into the PC's hidden prompt, never into either app field. The broker URL is
not the Nebius API endpoint. Keep the pass private; it authorizes use of your key.

### 4. Try a photo, then a saved-record question

On the phone, [open this generated meal photo](https://raw.githubusercontent.com/rainier-brioso/Metria/v0.1.0-hackathon/broker/fixtures/synthetic-meal.png)
and save it to the gallery. Start a fresh chat, choose meal-photo analysis,
select the photo, confirm sending it and prepare a draft. Review the food
matches and portions before saving. Unmatched foods leave nutrition **partial**;
review/select catalog matches rather than treating that subtotal as a full meal.
Then ask **“How many calories were in my latest meal?”**.

A fresh setup allows **10 attempts per pass**, 20 global attempts per day
and lifetime, three per minute, and one active request per installation.
Dispatched failures also consume attempts. The pass expires after seven days.
Restarts preserve usage and expiry; these limits are not a currency cap.

### Troubleshooting and stopping

| Problem | What to check |
| --- | --- |
| Docker connection/build error | Start Docker Desktop and select Linux containers. |
| Script finished after `-Action Check` | That is a disposable connectivity test. Start normally and use the new verified URL. |
| Cannot connect from the phone | Check the current URL and pass, and keep the broker running. If its console reports DNS filtering, try mobile data. |
| Requests rejected after earlier tests | The pass may be expired or exhausted; restarting does not renew it. Do not erase the quota ledger. |
| `metria-local` already exists | Stop your earlier broker with Ctrl+C before starting another. |

Ctrl+C stops the broker/tunnel; cloud then becomes unavailable. Each new tunnel
has a new URL: reconnect with that URL and the **same pass**. Switching local/cloud
retains connection details; use **Disconnect** only when you intend to remove them.
For a key-free, inference-free connectivity check, run the same launcher with
`-Action Check`; its temporary URL cannot be reused for the app.

Normal startup reports technical usage/cost estimates without storing
request/response content. For **fictional inputs only**, `-CaptureFictionalResponses`
enables bounded provider-response/error capture. See the detailed guide below
before enabling it.

See [Docker setup and troubleshooting](broker/DOCKER-LOCAL.md),
[the Python alternative](broker/LOCAL-APP-TEST.md), and
[broker tests and architecture](broker/README.md).

## Demo status and limitations

- Member-confirmed phone flows include cloud photo drafts, a latest-meal calorie
  answer, and voice transcription followed by a Nemotron meal draft. Local
  Gemma meal entry and the separate onboarding recording app have also been used.
- The latest verified gate passes **775 Android tests** and **130 broker tests**.
  Build success is separate from physical acceptance; detailed gaps and evidence
  live in [the checkpoint](docs/nebius-assistant-checkpoints.md).
- Food identity and portions can be wrong. Review estimates; Metria is a wellness
  prototype and does not diagnose or prescribe.
- Local model speed, memory use and voice availability depend on the phone.
  Physical development checks use a Samsung SM-S928B with Android 16.
- There is currently no permanent public broker or public demo-video URL.
  Recording materials are in [the demo folder](docs/demo/).
- **Before public submission:** publish the demo video, ensure judges can access
  the repository/release, and verify the evaluator setup.

Development direction: [product plan](docs/plan.md). Reproducible checks and
installation: [testing runbook](docs/TESTING.md).

## License

Metria's original code and documentation are licensed under the
[MIT License](LICENSE). Copyright (c) 2026 rainier-brioso.

Third-party dependencies, model artifacts and datasets retain their own licenses
and terms. Their license files and attribution notices remain applicable; see,
for example, the bundled [MiniLM notices](native-android/app/src/debug/assets/decision-model/minilm/NOTICE.md).
