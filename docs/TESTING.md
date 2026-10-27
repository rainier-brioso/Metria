# Metria testing and physical-device runbook

This is the canonical testing entry point for Metria. It is written for a new
agent or developer who has no access to prior chat history. Milestone-specific
expected results and evidence remain in the applicable checkpoint document.

For targeted visual debugging, read
[`metria-visual-qa`](skills/metria-visual-qa/SKILL.md) and the supporting
[tool selection and research](testing/visual-debugging.md). These supplement the
required gates below; they do not authorize saves, installs or private-data export.

## 1. Evidence levels

Keep these claims separate:

1. **Code review:** implementation was inspected, but nothing was executed.
2. **Automated gate:** unit tests and the debug build passed.
3. **Installed:** the APK was installed and launched, preserving app data.
4. **Physical acceptance:** the member completed the named interaction on the
   target phone and observed the expected result.

Never mark a physical gate passed using only tests, an emulator, a screenshot,
or a successful installation. Record failures as evidence too.

## 2. Required reading and preflight

Before changing or testing code:

1. Read `AGENTS.md`, `docs/plan.md`, and the applicable milestone checkpoint.
2. For UI work, also read all files under `docs/design/` relevant to the flow.
3. For assistant work, read `docs/assistant-agent-spec.md` and
   `docs/m4-assistant-checkpoints.md`.
4. Inspect the repository without discarding existing work:

   ```powershell
   git status --short
   git log -5 --oneline
   git diff --check
   ```

5. Treat every existing working-tree change as user-owned. Do not reset,
   restore, delete, or reformat unrelated files.

The production Android application is under `native-android/`. The Expo shell
at the repository root is retained as historical spike evidence.

## 3. Automated Android gate

Run the complete gate after every Android implementation change and before an
installation or handoff:

```powershell
Set-Location native-android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
Set-Location ..
```

Record:

- command and result;
- test count and failures;
- APK assembly result;
- relevant new or changed tests;
- whether physical verification is still pending.

A successful build is necessary, but it is not physical acceptance.

## 4. Safe device selection, installation, and launch

Use the Android SDK's ADB and select the exact current device. Never save a
phone IP, pairing code, serial, or transient transport ID in the repository.

```powershell
$metriaAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $metriaAdb devices -l
& $metriaAdb -s <device-serial> install -r "native-android\app\build\outputs\apk\debug\app-debug.apk"
& $metriaAdb -s <device-serial> shell am force-stop app.metria
& $metriaAdb -s <device-serial> shell am start -W -n app.metria/.MainActivity
```

If the same wireless phone appears more than once, select one exact serial or
transport and use it consistently for that test. Do not assume an old
transport ID is still valid.

For wireless debugging, use only endpoints currently displayed by Android:

```powershell
& $metriaAdb pair <host:pairing-port>
& $metriaAdb connect <host:debug-port>
& $metriaAdb devices -l
```

### Data-preservation rules

- Use `install -r` for ordinary testing.
- Do not run `pm clear`, uninstall, downgrade, replace the signing key, or
  delete app-private storage.
- Do not remove the downloaded model or encrypted vault unless the member
  explicitly requests a destructive recovery test.
- Obtain explicit approval before installation unless repeated in-place
  installs were already authorized for the active test cycle.
- Biometric unlock is always performed by the member. Never bypass or automate
  it.

## 5. Minimum physical smoke test

After every material build installed on a phone:

1. Launch Metria and complete biometric unlock.
2. Confirm Today, History, Assistant, and Settings open without a crash.
3. Confirm existing weights, meals, thumbnails, goals, and saved dishes remain.
4. Rotate once if the changed screen supports rotation; verify no vault reset.
5. Open and dismiss the keyboard; verify the focused field and primary action
   remain above system navigation.
6. Background and resume the app; verify the expected lock/session policy.
7. Exercise the exact changed flow, including success, cancellation, and one
   applicable failure or empty state.

Use `docs/design/QUALITY-GATES.md` for the full UI matrix.

## 6. Local assistant acceptance matrix

The assistant must remain offline, bounded by approved tools, and grounded in
the encrypted vault. The current reference runtime is:

- LiteRT-LM 0.13.0;
- Gemma 4 E2B;
- text on CPU, vision on GPU, audio on CPU.

For a material assistant change, test the applicable rows below in both English
and Spanish. Use questions that can be verified against visible local records.

| Area | Physical scenario | Pass condition |
| --- | --- | --- |
| Launch feedback | Tap Send with a valid question | Member bubble and cancellable thinking state appear immediately; composer does not appear frozen |
| Factual nutrition | Ask about this week’s nutrition goals | Period, calories, protein, goals, and percentages match the local records; no stale weight or unrelated fact appears |
| Factual weight | Ask for a dated weight or trend | Dates, exact stored precision, direction, and count remain correctly bound |
| Meal history | Ask what was eaten on a specific relative date | Correct bounded meal records are used; no weight action is selected |
| Saved dishes | Ask about a remembered dish | Only returned private-dish identities and confirmed components are mentioned |
| Follow-up | Ask a question, then use “it”, “that day”, “yes”, or “and protein?” | The follow-up resolves against the visible conversation without replaying an old tool result |
| Period change | Change from today to week or another bounded date range | A new bounded query runs and the answer labels the correct period |
| Unsupported topic | Ask something outside Metria’s approved scope | Metria responds safely without inventing records or invoking an unrelated write |
| Cancellation | Cancel during thinking | Inference stops, UI returns to an actionable state, and no late answer or action is applied |
| After cancellation | Stop, then ask for calories in the latest recorded meal | Stop notice clears on submission; new bounded latest-meal read answers that meal, with partial/missing coverage; no late cancelled result |
| Lock/restart | Lock, background, force-stop, relaunch, and unlock | Vault policy is respected; completed tools do not execute again |
| Offline | Enable airplane mode and use local chat | Supported local features continue; no data leaves the device |
| Action safety | Ask to add weight or draft a meal | Weight remains reversible; meals require review; duplicate/replayed writes do not occur |

For the active M4 work, specifically measure the interval from tapping Send to
the first visible thinking state. A correct final answer does not excuse a
frozen or apparently dead launch interval.

## 7. Meal-image and nutrition-label acceptance

Use member-approved test images that contain no unnecessary private data.

### Meal analysis

Verify:

- camera and picker cancellation return to the previous usable screen;
- loading and cooldown states explain what is happening and can finish;
- the temporary image is discarded after analysis;
- the proposal distinguishes visible foods, possible ingredients, and
  uncertainty;
- a malformed food beside valid foods produces a visibly partial draft, never
  silently drops a suggestion, and saves catalog nutrition with partial
  coverage even when every retained food has a complete match;
- an invalid root or no independently valid foods uses manual entry instead
  of showing an empty AI review;
- remembered dishes are suggestions, not silent replacements;
- the review has a visible Leave without saving action; clean leave exits
  directly, edited leave asks for confirmation, Keep reviewing retains edits,
  and confirmed leave creates no meal or correction learning. Check footer,
  header Back, Android Back, keyboard-visible Back, and rotation;
- after leaving review, the original unsaved proposal remains available;
  Discard draft asks for confirmation, Keep draft preserves it, and confirmed
  discard removes only that proposal, not a saved meal or conversation;
- saving a reviewed Chat meal adds one confirmation using the final reviewed
  name/foods when Chat is reopened. Verify it survives relock/restart under
  normal retention; failed saves and discarded drafts add no success message;
- successful Chat meal save returns directly to Chat with **View meal**.
  Verify it opens the exact saved entry, as a Today meal tap does, and closing
  the editor returns to Chat. Check link persistence after relock/restart,
  deleted-target handling, keyboard, rotation, font scaling, and TalkBack;
- the member can correct names, components, portions, catalog matches, and
  nutrition before saving;
- from the expanded dish editor, ask the local text model to correct one
  selected food name; verify the old/new preview, Apply and Discard, catalog
  match invalidation on Apply, and no persisted change before final meal Save;
- verify direct editing remains available when the model is busy/unavailable,
  a malformed suggestion is rejected, cancellation or editor exit ignores late
  output, and a result cannot transfer to another selected food;
- partial catalog matches calculate only the supported portion and disclose
  incompleteness;
- after saving a partial photo review, reopening and saving that meal without
  filling the missing foods must not upgrade its estimate to complete; confirm
  the partial label remains in Progress after the second save;
- confirmed thumbnails are retained only when enabled and accepted.

### Nutrition-label OCR

Verify:

- capture shows a private preview; OCR starts only on explicit Read label;
- two-column labels use one consistent value column; missing or ambiguous
  values stay unknown rather than being filled from another column;
- values remain editable and are not silently saved;
- malformed or ambiguous values fail safely;
- camera retry and existing-photo selection work;
- the final saved record agrees with the reviewed values.

## 8. Vault, recovery, and privacy acceptance

When relevant to the change, verify:

- records remain encrypted and unavailable before biometric unlock;
- member and model data survive an in-place APK update;
- lock, process death, and restart follow the documented policy;
- encrypted archive export excludes model files, keys, chat, diagnostics, raw
  model output, and temporary images;
- restore reports record counts and requires explicit replacement approval;
- offline use does not silently require or transmit over a network;
- debug diagnostics never become part of the encrypted member record or a
  release UI.

### Temporary debug model response

After a member-authorized failed text meal draft, stay in the foreground and
open Privacy/Settings → Developer diagnostics → Latest in-memory response.
The debug build displays the bounded model text and a fixed meal-draft
validation category. Navigating out of Chat refreshes this status; the category
distinguishes malformed JSON, invalid schema/ranges, extra tool calls, and
empty or oversized output without weakening the parser. Inspect the response
only for the authorized run. Do not copy private response text into repository
evidence. It is held only in memory and clears on the next operation, vault
lock, backgrounding, or restart. Reproduce after installing a new APK because
installation restarts the process. Record only the fixed category and sanitized
findings in checkpoints. Release UI does not offer this inspection.

### Opt-in debug assistant trace

On a debug build only, the member may enable **Assistant metadata trace** in
Privacy/Settings → Developer diagnostics before reproducing an assistant issue.
It records only fixed technical event, tool-category, period, and audit-outcome
fields in app-private no-backup storage. It excludes prompts, model prose,
tool payloads, record values, photos, and raw exceptions. Confirm the trace is
off by default, bounded to 100 events / 64 KiB and a rolling 24-hour window
(pruned on startup, new writes, and hourly while open), and that **Clear
trace** removes the file while **Disable** stops capture and removes it.

Routing monitoring also records fixed MiniLM decision/rejection reasons and
native/legacy Gemma selection. A scored routing event may append positive,
competing and fallback cosine similarities, each finite and bounded; `none`
means unavailable. These are similarities, not calibrated probabilities.
The base TSV columns remain timestamp, kind, operation, outcome, reason, tool,
period and request ID. Readers accept legacy eight-column rows and routing rows
with three trailing score fields. Only allowlisted technical fields may be read;
no question/answer/record is needed for route diagnosis. Inspect this retained
trace instead of asking the member to transcribe transient Developer diagnostics,
which can clear on lock. Biometric unlock is still a member action.

After selecting the exact current device with `adb devices -l`, read only the
metadata file from the debuggable app sandbox:

```powershell
& $metriaAdb -s <device-serial> shell run-as app.metria cat no_backup/assistant-debug-trace.tsv
```

Do not redirect this output into the repository or attach raw device data to a
commit. `run-as` is unavailable for release builds. Verify that release builds
neither offer the control nor create the file. The trace remains outside the
encrypted vault and recovery archive; its contents are technical diagnostics,
not evidence that a physical assistant scenario passed.

### Opt-in image development capture

For a member-approved test image on a debug build, **Image development capture**
in Developer diagnostics is a separate, off-by-default control. Enable it
before a meal-image analysis. The enabled choice persists locally until the
member taps **Disable**, including after lock, backgrounding, and process
restart; a pre-existing installation first needs one new Enable after the
build that introduces this behavior. Only that choice persists. The one
bounded model response, reduced EXIF-free JPEG, duration, outcome, and fixed
parser-rejection category are kept in app-private no-backup storage and
replaced by the next run. This raw capture is **not** the metadata-only
assistant trace. It expires within 30 minutes while Metria runs and is
deleted on disable, clear, lock/background, process startup, or app
destruction. **Clear image capture** removes only the payload and leaves the
choice enabled. A hard process kill can leave a payload until restart; the
ADB helper rejects and removes an expired capture before showing raw content.
Release builds must not expose or write it and must ignore/remove the debug
choice. The original temporary analysis image still must be discarded after
the run. Persisted enablement is not standing permission for ADB inspection;
obtain member authorization for the active test session.

With the member's authorization for that active test session, keep Metria
unlocked and foregrounded, select the exact currently connected device, and
run from `native-android/`:

```powershell
.\tools\read-image-debug-capture.ps1
```

The helper reads only `no_backup/debug-image-capture/capture.bin` through
`run-as`, prints the bounded model response and fixed metadata, and places a
reduced JPEG in a random temporary directory outside the repository for visual
inspection. It does not start the Android CLI phone helper. If multiple
devices are connected, pass `-Serial` with the exact current `adb devices -l`
identity without saving that identity in the repository. Remove the temporary
JPEG after inspection and use **Clear image capture** or lock Metria to erase
the app-private copy. Never commit, attach, or paste a raw capture into source
control or an issue. Keep only sanitized outcome/category/timing conclusions
in checkpoint notes. ADB inspection moves this opted-in test content to the
development machine; it is not ordinary on-device-only app operation.

Uninstall/reinstall and clean-device restoration are destructive test cases.
Perform them only with explicit member approval and a verified recovery plan.

## 9. Responsiveness and device-quality evidence

Capture performance only when it helps diagnose a material issue. Reset frame
statistics immediately before the tested interaction:

```powershell
& $metriaAdb -s <device-serial> shell dumpsys gfxinfo app.metria reset
# Perform the interaction on the phone.
& $metriaAdb -s <device-serial> shell dumpsys gfxinfo app.metria
& $metriaAdb -s <device-serial> shell dumpsys meminfo app.metria
& $metriaAdb -s <device-serial> shell dumpsys battery
& $metriaAdb -s <device-serial> shell dumpsys thermalservice
```

Record the operation, duration, janky-frame count/percentage, worst visible
stall, process memory, battery level, thermal status, and active runtime. Do not
claim broad device support from one flagship phone.

For crash investigation, collect the narrowest useful log window. Clear logcat
before reproduction, avoid dumping private model prompts or records, and keep
temporary logs outside the repository:

```powershell
& $metriaAdb -s <device-serial> logcat -c
# Reproduce once.
$metriaPid = (& $metriaAdb -s <device-serial> shell pidof app.metria).Trim()
& $metriaAdb -s <device-serial> logcat -d --pid=$metriaPid -v threadtime
```

Do not commit raw logcat, UI hierarchy dumps, screenshots, photos, database
exports, or member records.

## 10. UI and accessibility matrix

For materially changed screens, verify at least:

- three-button and gesture navigation insets;
- keyboard open/closed and Android Back behavior;
- portrait and landscape where supported;
- enlarged system font and display size;
- TalkBack reading order, labels, and state descriptions;
- 48 dp minimum targets;
- contrast and non-color status cues;
- scrolling to all content and actions;
- background/resume and configuration recreation;
- empty, partial, loading, success, error, cancellation, and restoration states.

Record remaining matrix gaps rather than silently treating them as passed.

## 11. Evidence capture template

Append physical results to the applicable checkpoint using this shape:

```markdown
### Physical Android verification — YYYY-MM-DD

- Build/commit: `<sha>` or `uncommitted working tree based on <sha>`
- Automated gate: `<command and result; test count>`
- Device: `<model, Android/API>`
- Runtime: `<model/runtime/delegates>`
- Installation: `adb install -r`; existing data preserved: yes/no
- Scenarios exercised:
  - `<scenario>` — pass/fail and observed result
- Performance evidence: `<only when measured>`
- Privacy evidence: `<offline/data handling relevant to this change>`
- Remaining gaps: `<explicit list>`
- Next bounded task: `<one concrete continuation>`
```

Do not include phone addresses, serials, pairing codes, personal meal/weight
values, raw model replies, or unredacted screenshots in committed evidence.

## 12. Handoff and completion gate

Before another agent continues or work is committed:

1. Run `git diff --check`.
2. Run the complete Android automated gate.
3. Update the applicable checkpoint with honest automated and physical evidence.
4. State the next bounded task and any safety or acceptance gaps.
5. Inspect `git status --short` and preserve unrelated changes.
6. Commit and push only after explicit member approval.

The handoff is not complete if the tree is broken, the checkpoint overstates
physical evidence, or the next agent must reconstruct the current state from a
chat transcript.
