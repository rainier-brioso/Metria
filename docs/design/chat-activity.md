# Continuous Chat activity - 2026-10-21

Status: member requested unification and explicitly chose the in-Chat placement.
Phone acceptance remains required. No new dependency or model change.
Implementation and scoped independent review complete. Full automated gate:
655 tests, zero failures/errors/skips, debug/release assembly, release vital lint
and clean diff check. Phone results must be recorded separately in the M4
checkpoint; unit tests do not establish the visual/cancellation/keyboard gates.
Member confirms the named single-inline-flow and Type instead keyboard/no-stale-
text interaction on Samsung SM-S928B / Android 16. Stop/lock, difficult layout,
font scaling/TalkBack, draft/weight Review and broader regression remain open;
this is not full visual or release acceptance. No screenshot evidence captured.
After the guarded-result wake-up repair, member confirms Stop during transcription
then Type instead and a typed read completes correctly with no late voice result.
Safe metadata corroborates MiniLM/read handler/audit/delivery. Lock/unlock cleanup
and the remaining matrix are still open; see M4 for failure and repair history.
Named lock/unlock check now passes by member report: retained history, cleared
voice activity, fresh correct typed read and no late old result. Safe metadata
shows voice start/lock then fresh MiniLM handler/audit/delivery. Other matrix rows
and full native timing coverage remain open.
On the later 658-test APK, member also confirms automatic spoken food/portion
edits, retained correct Review across reopening, and spoken weight opening editable
Review with no record after dismissal. Empty/error recovery, Today regression,
visual evidence/font/TalkBack/reduced-motion and broader timing gates remain open.
The member's two silence checks with background sound failed: invented words
were automatically sent. Manual recording Stop bypasses local silence detection
by hard-coding detectedSpeech=true. Repair and physical retry are pending;
single-peak detection cannot establish reliable speech/noise discrimination.
The installed 661-test recorder repair passes the member's quiet/background
silence retries and normal speech. Broader noise and recorder timing remain open.
Member also confirms dedicated Today Speak opens correct rice Review/meal section,
with Chat history retained after Discard. This is the named Today regression, not
all meal-entry or accessibility states. Final integrated checkpoint review found
no confirmed material introduced defect; broader acceptance gaps remain open.

## Research and decision

The current ordinary voice branch replaces Chat with a modal transcription
sheet, then replaces it again with Chat thinking. The changing surface makes
one request look like two separate loading operations.

Primary design sources:

- [Apple progress guidance](https://developer.apple.com/design/human-interface-guidelines/progress-indicators):
  keep progress in a consistent location, explain real work concisely, avoid
  disruptive changes of indicator style, and permit cancellation when feasible.
- [Material motion](https://m3.material.io/styles/motion): consistent motion
  schemes connect state changes and should suit the product's character.
- [Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide):
  animateContentSize can smooth changing content without manually shifting
  surrounding content or leaving hidden interactive views in place.

Considered: a large voice orb, a fixed composer panel, or one activity card in
the conversation. Choose the activity card: it retains the conversation context,
uses existing Chat space and can share its identity with ordinary typed loading.
A large orb adds a separate focus; a fixed panel consumes keyboard space.
This recommendation is our inference from the guidelines, not a claimed standard
used by every assistant. No speculative percentage or simulated sound amplitude.

## Contract

- Ordinary Chat stays visible during ready/listening/transcription/error states.
- One keyed activity row uses the same botanical mark, indeterminate indicator,
  shape and typography for Listening, Transcribing, Thinking, Preparing a meal
  draft and Stopping. Only the actual current stage is shown. Fast results have
  no artificial minimum duration. Final answers use the normal timeline.
- Voice takes presentation precedence while its capture is active; then stopping,
  draft preparation and normal response are mutually exclusive.
- Explicit Start, Stop recording, Stop processing, Type instead and Cancel remain
  reachable. Cancel returns to the same Chat. The composer cannot start competing
  work during capture. Empty/error permits Retry/Type; no old words are prefilled.
- Request/capture/session proofs, one-use automatic submission, runtime ownership,
  temporary-file cleanup, Review/Save and weight confirmation do not change.
- Stage labels are stable polite announcements; the elapsed clock is not a live
  announcement. Standard Compose motion respects system animation duration.
- Today meal entry keeps its existing dedicated destination/sheet.

Acceptance: ordinary typed and spoken requests have one activity surface, correct
stage changes and one submission; food/portion Review remains correct; Stop,
Cancel, Type instead and lock prevent late results; failed/empty speech permits
retry without old text; keyboard/font scaling/TalkBack and layout require phone
verification. Screenshot evidence must follow the private-device runbook.
