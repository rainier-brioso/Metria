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

## Install the demo APK

Download [Metria 0.1.0 — hackathon APK](https://github.com/rainier-brioso/Metria/releases/tag/v0.1.0-hackathon)
from the release assets; `SHA256SUMS.txt` provides its checksum. This installs
the normal **Metria** app (`app.metria`), not the separate onboarding recording app.

This is a development-signed debug APK for evaluation, with MiniLM included.
Download/prepare Gemma in the app for local inference, or follow the BYOK Nebius
instructions below for cloud. Physical verification uses Samsung SM-S928B /
Android 16; performance and model support on other phones are unverified.
For updates, preserve the existing installation and signing identity.

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

## Run the Android app

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

The current demo runs the broker on your PC using Docker and a temporary HTTPS
tunnel. **Nemotron runs remotely on Nebius Token Factory.** A judge can run their
own broker with their own Nebius key; no shared public key or funded public
service is embedded in the APK. A provider endpoint serves the model; the broker
adds app authentication and durable consumption limits.

Prerequisites: Docker Desktop running Linux containers, PowerShell, internet,
and a Nebius Token Factory API key with access to the model above. No USB,
router port forwarding or PC GPU is needed for this cloud route.

Check the tunnel without a key or inference:

```powershell
.\broker\local-docker.ps1 -Action Check
```

Then start the broker in its normal, content-free logging mode:

```powershell
.\broker\local-docker.ps1
```

1. Stop any earlier broker first. Enter your Nebius key at the hidden prompt.
2. Wait for **HTTPS broker verified**. Keep that terminal and the PC running.
3. In Metria's cloud settings, enter that HTTPS URL and the private pass from
   `.local-state/nebius-app-test/app-test.pass`. The key stays in the broker.
4. Choose cloud and try an invented meal description or the generated photo at
   [`broker/fixtures/synthetic-meal.png`](broker/fixtures/synthetic-meal.png).
   Use an installation and conversation containing fictional data only.
5. Review the draft. Try the latest-meal question after saving a fictional meal.

For fictional debugging only, start with `-CaptureFictionalResponses` to retain
the complete bounded provider output/error and available usage. The pass,
installation binding and quota ledger survive restarts. Current rehearsal limits
are 10 attempts per pass, 20 global attempts per day and lifetime, three per
minute, and one active request per installation. Dispatched failures consume an
attempt. A pass expires seven days after issuance; restarts do not extend it.
These are attempt limits, not an exact currency cap. Cost reports are estimates.

The URL changes when the tunnel restarts: update it in the app and retain the
same pass. Rebuilding the image requires restarting the running broker to apply
changes. Stopping the broker makes cloud assistance unavailable; switching to
Gemma is an explicit user choice.

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
