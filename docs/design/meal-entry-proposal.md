# Meal entry proposal — 2026-10-06

Status: member-approved direction, under staged Android implementation. Physical
acceptance remains open; see `implementation-checkpoints.md`. Open
`prototypes/meal-entry.html` in a browser; it is
self-contained, uses fictional examples and makes no network requests.

## Purpose and hierarchy

- Today: compact recorded nutrition summary, recent date context, meal sections
  with labelled Add actions. The central Metria action remains dominant.
- Add meal: preserve the chosen date/type, then choose photo, library, voice,
  description, saved dishes or manual entry. Choices lead to one shared review.
- Photo: meal/label mode, local photo preview, optional member description,
  framing tips, one Create meal draft action. Label mode has its own Read label
  action and omits the meal-description field. Barcode lookup is deferred.
  The member explicitly deferred barcode scanning on 2026-10-07 to avoid a scan
  followed by another label scan. The two photo modes remain Meal photo and
  Nutrition label. See `barcode-slice-proposal.md` for future design; no decoder
  approval is pending and no barcode dependency should be added in this cycle.
- Voice: explicit start/stop and bounded recording. Successful local recognition
  continues directly into meal draft preparation, then required review. Recognized
  text remains editable after a preparation failure or cancellation, with explicit
  retry and Type instead. This prototype simulates recognition;
  implementation must evaluate whether editable transcription can be supplied
  by the existing local audio runtime rather than assuming it exists.

Mockup illustrations are original vector shapes, not reference app assets.
Reference screenshots are not redistributed. All controls remain local to the
browser mockup. Camera, library, inference, voice and saving are simulations.

## Proposed data and state decisions

- Carry origin, selected date and explicit meal type to the review; no inferred
  time silently changes a member-selected type. Existing records remain intact.
  Before implementation, inspect the saved meal schema and define compatible
  treatment of records without a type; do not invent historical classifications.
- Optional photo description is bounded to 600 characters and sent only to the
  local model as member context. It cannot bypass schema/portion validation.
  Description stays transient and is not saved as a note automatically. Image
  lifecycle and thumbnail consent continue to follow the existing contract.
- Tips are available on demand; a first-use hint is dismissible. Use full plate,
  good light, steady camera, and separate clearly visible dishes. Hidden foods
  require member context and review, not a promise of photo accuracy.
- Voice starts only after a tap and permission. Always show Listening and Stop;
  auto-stop uses the recorder's bounded duration. Successful validated local
  recognition immediately requests a meal draft once, without an intermediate
  transcript-confirmation step (member-approved change, 2026-10-10). Preparation
  failure or Stop retains recognized words for editing and explicit retry.
  Empty/failed recognition permits retry or typing. Cancel deletes temporary
  audio. Permission denial keeps typing. Saving still requires member review.
- Idle, loading, partial draft, error, cancellation, and success must be explicit.
  Stop invalidates pending output. Mode/image changes invalidate prior results.
  Lock/background cancels capture/analysis and clears sensitive transient content.
  Rotation preserves editable unlocked state without recording or sending twice.
  Process restart never replays recording/inference; resume follows the existing
  unsaved-draft policy rather than creating new persistence implicitly.
- Save from Today returns to the selected date and section. Save from Chat keeps
  the existing durable confirmation and View meal link. No automatic writes.

## Implementation order after visual approval

1. Today grouping and contextual add chooser, using extracted screens and shared
   components. Inspect meal type persistence before extending its contract.
2. Photo preview/tips/optional description, retaining existing local camera and
   photo-picker infrastructure. A custom live camera is a separate decision;
   this preview does not require a camera dependency.
3. Dedicated voice entry, subject to local transcription feasibility.

Update FLOWS and SCREEN-SPECS with accepted details before Android changes.
Run the complete Android gate and approved in-place phone checks for each
material flow. Required physical evidence includes Back, cancel, save/reopen,
keyboard, rotation, larger fonts, TalkBack, partial/error/loading, and lock.
The browser mockup is design evidence only. Nebius setup/live tests stay deferred.

## Verification

JavaScript syntax check and `git diff --check` passed. Automated visual and
interaction inspection is pending: the available browser tool rejected local
file URLs under its protocol policy. Open the HTML directly in your own browser
to review all four screens, tips, modes, and simulated voice states. No Android
code changed during that original prototype review. Subsequent Android evidence
is recorded in `implementation-checkpoints.md`.
