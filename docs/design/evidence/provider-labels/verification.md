# Provider label and cloud meal-entry verification — 2026-10-29

Scope: shared non-interactive icon/text label in Chat, Today meal entry, photo
preview and analysis. Gemma/local, Nemotron/cloud and ML Kit/local OCR are
distinguished; cloud dictation describes its on-device transcription boundary.
Selected route does not claim connectivity or historical-response provenance.

Automated: 770 Android JVM tests pass, no failures/errors/skips; complete
testDebugUnitTest/assembleDebug gate passes. Broker suite 130 tests passes,
including structured meal proposals, malformed output rejection, clarification
and reported usage on rejection. git diff --check clean. No new dependencies.

Installed with member's explicit approval on Samsung SM-S928B / Android 16:
app.metria update with install --user 0 -r, Success. Cold launch Status ok,
1170 ms. APK SHA-256:
`1073fd2ebfce41de25cb651adb413aca5b16dfc53ff52d008e42d55e63afee5c`.
Original first-install identity retained; no clear/uninstall/model replacement or
record mutation. Metria Demo was not installed, stopped or changed.

Visual acceptance remains pending. Studio reports no running instance. The
known Android layout instrumentation issue was not retried. Before observation
found the launcher, so it is not before-state screen evidence. After launch,
the screenshot is protected/black; no attempt to bypass protection or biometrics.
Both temporary captures were immediately viewed and deleted outside the repo.
No font, rotation, navigation or system settings changed.

Member has been asked to unlock, inspect cloud label in meal entry/Chat, and
restart the broker/update its temporary URL before one fictional meal attempt.
Label visibility, local-mode label, photo/loading/ML Kit labeling, keyboard,
landscape, enlarged font, TalkBack, Stop/lock and actual live structured-output
acceptance are not established by this installation. No inference was performed
by the agent. Next bounded check: member confirms labels and one cloud meal
draft or supplies the next fictional capture; preserve quotas and no retries.

Member follow-up confirms the provider label is visible, but meal preparation
still fails. The reply does not identify all screens, so no blanket visual gate
is marked passed. Investigation confirms the live broker container still uses
the pre-repair image; restart is required before testing the new schema path.

After the restart instruction the member reports a correct draft. This closes
the reported cloud transcription-to-draft functional failure for that attempt.
The visible label is member-confirmed; other screens and difficult layout states
listed above remain pending. No agent-captured private screenshot is retained.
