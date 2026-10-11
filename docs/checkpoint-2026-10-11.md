# Working-tree checkpoint — 2026-10-11

Status: reviewed development checkpoint; member approved commit and push on
2026-10-11. This is a development
checkpoint, not a completed milestone or release acceptance claim.

## Scope and existing acceptance

The checkout is on `main`, based on `c4cf506` (disconnected Nebius assistant and
local broker). A fetch on 2026-10-11 confirmed HEAD and `origin/main` match.
Uncommitted work spans several earlier sessions as well as today's model work;
the earlier changes must be preserved together with their tests and contracts.

Earlier work includes dedicated typed/spoken meal entry, prepared-food catalog
support, portion recalculation, selected-meal assistant context, goal setup and
focused editing, recorded phase periods/recaps, and encrypted meal-photo metrics
with automatic navigation to review. Detailed implementation and acceptance
remain in [design checkpoints](design/implementation-checkpoints.md).

The October 10 checkpoints already record member-reported meal-entry TalkBack
traversal, enlarged text with the keyboard visible, unsaved-text rotation,
Stop/retry and background cancellation, plus prepared-meal portion recalculation
and save/reopen. These checks are accepted for the exercised states. They do not
need to be repeated merely because a new session began.

Goal evidence includes target save/reopen and consistency across Goals, Today,
Phase details and Progress; the exercised phase-detail enlarged-text/landscape
layout; and unsaved weight-editor Back/swipe, Keep editing, discard and rotation.
This does not establish TalkBack acceptance for every goal screen or complete
archive recovery and populated historical recap acceptance.

Today's decision-model work includes public, synthetic Von research and a
debug-only Android pilot, followed by MiniLM integration into ordinary Chat.
The MiniLM route is restricted to English requests to list today's meals; the
host reads local records and validates the deterministic reply. The member
confirmed the correct today reply and diagnostics showing Gemma generation
skipped, then a correct yesterday reply through the usual route. See
[MiniLM acceptance and limits](minilm-chat-integration.md) and
[assistant checkpoints](m4-assistant-checkpoints.md).

MiniLM remains debug-only and requires Gemma to be prepared under the existing
readiness flow. Its public model is ignored by Git and recreated with the pinned
preparation script documented in the integration note. This checkpoint does not
claim a general speedup, reduced resident memory or a new benchmark result.

## Verification refreshed for this checkpoint

From `native-android/`:

```powershell
rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon
```

BUILD SUCCESSFUL on 2026-10-11, including the voice correction below. XML reports
contain **452 tests, zero failures/errors/skips**. The debug APK assembles and
the release factory compiles. No new installation or device-data inspection was
performed during this checkpoint audit.

The development-tool unit suites also passed: 22 decision-model harness/adapter
tests and four food-catalog importer tests. The first sandbox run could not write
its system temporary files; rerunning with temporary files under the ignored
workspace directory passed. No model inference benchmark was run.

The earlier approved in-place MiniLM installation contains the preceding photo
changes as well. Historical entries saying a prior photo-only APK was not
installed describe that earlier attempt; they do not establish acceptance of
the photo flow in the later combined build. Existing vault and Gemma files were
preserved during the approved test cycle.

## Open acceptance and next bounded work

Independent review found a narrow Stop/automatic-completion race in dedicated
meal entry: the controller could consume its attempt before a queued completed
clip arrived and then discard that clip. The correction leaves the attempt
active while awaiting automatic completion, shows Finishing recording and
prevents a second start. Cancel/replacement still reject and delete stale clips.
The shared recorder and Chat callers were not changed. Three fake-recorder
controller regression tests cover transfer, cancellation and replaced attempts;
focused re-review found no remaining material issue. This correction has not
been installed or exercised on the phone; the earlier meal-entry TalkBack
acceptance does not establish acceptance of this new finishing state.

- Meal entry: actual generation-error retry and broader varied-food accuracy.
  Verify the Stop/completion handoff and its finishing-state accessibility on
  the next approved in-place build.
- Photo flow: verify automatic review, explicit save/reopen, Stop with no late
  navigation, return with no replay, and partial/failure states. Verify retained
  encrypted photo measurements after restart.
- Goals: populated/partial historical recaps, archive recovery, remaining
  accessibility/navigation states and activity recreation restoration. The
  checkpoint also records a pre-existing status-bar icon contrast issue.
- MiniLM: empty today, repeat lookup, out-of-scope rejection, Stop/relock and
  missing/corrupt-model fallback. These remain separate from the two confirmed
  Chat scenarios.

Next product work is to choose another bounded local assistant operation using
existing records and host validation, after saving this reviewed checkpoint.
No additional model benchmarking is required by that product decision.

## Version-control boundary

Include app source, tests, public catalog data, design/contracts, synthetic
fixtures and reproducible development tools. Exclude `.codex-remote-attachments/`
photos, `.local-state/`, downloaded model binaries, build outputs, device dumps
and member data. Local photo attachments are now explicitly ignored; they are
preserved on disk. Commit and push require the explicit approval specified by
the repository's `AGENTS.md`.
