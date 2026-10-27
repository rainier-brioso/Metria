# Contextual assistant panel - 2026-10-24

Status: member-approved implementation. Automated and physical evidence below
are separate; this is not a release acceptance claim.

## Visual correction after member review

The member rejected the first installed presentation as unlike the approved
interactive prototype. The earlier smoke results establish basic operation only,
not visual acceptance. The correction uses a continuous white panel, soft plum
member messages, unboxed assistant prose, bordered draft cards with dominant
Review, and one rounded composer containing text, actions, labelled Speak and
plum Send. Actions and conversation options are scrollable inline panel views
with Back to chat, not floating dropdowns. The existing bottom navigation is
reused above the scrim, with its original icon and destination styling; selecting
a destination closes Chat through the existing disposal policy. The keyboard
hides this dock to preserve room for the composer. The background screen remains
excluded from accessibility traversal; the visible dock is accessible separately.
The member also requested integrated photo and label capture from the panel.
That follow-up is implemented below; physical acceptance remains pending.
English application copy is retained;
the prototype's simulated records and processing are not production behavior.

Final correction gate passed: 714 tests with zero failures/errors/skips and debug
APK assembled. Diff check and unrelated-change preservation audit passed.
Corrected APK SHA-256:
`2ca2a71d6219ff507199974e71d1d7f65596a3678a43becf23e94b077210783f`.
Visual acceptance of this correction remains pending on the reference phone.

## Approved scope

Completion approved 2026-10-26: close the remaining assistant prototype gaps.
Contextual nutrition-label Review gains the same editable/clearable local-time
category default and transactional saved-meal acknowledgement/View meal as meal
photo Review. Preserve verified-label validation and nutrition; do not estimate
label nutrients or create correction learning from OCR. Failures retain inputs
without a success receipt. Today label entry keeps its existing behavior.

The assistant actions gain My saved dishes: a searchable inline list of actual
local saved dishes with an empty state and Back to chat. Selection revalidates
the current dish and opens the existing unsaved Review; only final explicit Save
records a meal. No model request is needed to browse or choose a dish. Preserve
busy/session guards and conversation handoff before leaving Chat.
Its review is labelled Review saved dish and explains the saved-food/portion
starting point. It must not show rejected-photo/manual-fallback wording. Every
assistant-origin review uses the measured native-dialog inset treatment,
including saved-dish selection; Today/manual presentation retains its contract.

When View meal opens a saved record from Chat, a successful explicit update
records a host-authored Meal updated receipt and View meal atomically with the
update. Failure retains the editor, and cancellation writes no receipt. Editing
from other destinations does not add a Chat acknowledgement. Existing encrypted
conversation retention and deleted-target handling remain authoritative.

Hierarchy: one panel page for choosing a dish, a focused existing Review for
editing it, and the conversation for completed save/update receipts. Search,
empty/no-match, selection, cancellation, errors, rotation, keyboard and lock
follow the same panel and vault boundaries. Physical acceptance is tracked
separately from implementation and previews.

Follow-up approved 2026-10-26: successful Save from contextual Analyse meal or
its manual fallback returns to Chat with the existing host-authored saved-meal
acknowledgement and View meal link. Meal and acknowledgement must persist in one
transaction; failure retains the draft and cannot show success. When capture is
the first action, explicit Save may start a fresh receipt-only conversation;
expired transcript content must never be revived. Opening capture alone still
writes no conversation. Normal 50-message/24-hour retention remains authoritative.

Assistant-origin meal Review preselects the existing local-time meal category
only when neither a valid Today section nor explicit member wording supplies one.
Freeze the review time for that draft. Keep the category editable and allow it to
be cleared; recomposition or rotation must not refill a cleared choice. Existing
saved meals and other entry routes are not reclassified. The current category
remains visible in Meal details and is persisted only by explicit final Save.

Replace only the assistant's entry/presentation with the reviewed contextual
panel. Keep the central green `SmartToy` button, navigation destinations,
Today/Progress/Privacy and capture pipelines unchanged. Panel-origin capture and
label review are integrated below; the shared meal editor is retained. The
working tree already contains another session's Chat drink quantity changes;
preserve those code, tests and documentation. No dependency, model, vault schema,
record-save permission, commit or push is part of this slice.

## Hierarchy and interaction

- The existing central icon opens the shared Chat directly. An in-window tonal
  panel reveals the current screen behind a dismissible scrim. It is not a new
  conversation or an extra navigation destination.
- Header: Metria, private/on-device AI disclosure, expand/reduce, conversation
  options, Close. New conversation retains its destructive confirmation and
  cancellation behavior. Assistant tools opens the existing assistant home,
  including model setup and offline food search.
- Timeline: existing messages, native selection/Copy, expandable evidence,
  saved-weight Edit/Undo, draft Review/Discard, saved-dish choices, saved-meal
  links, errors and the single inline voice/response activity remain in place.
- The initial invitation replaces only the presentation of the existing generic
  welcome. The suggestion fills the composer; it never submits or reads records
  until Send. An existing selected meal uses its original removable one-question
  context. Opening from Today or Progress grants no implicit data authority.
- Composer: full-width text field, then `+`, microphone and Send. The `+` menu
  retains Speak, Chat, Analyse meal, Scan nutrition label and Add weight.
  Speech still submits once automatically after recognition; Type instead,
  retry, Stop and cancellation retain their current guarded behavior.
- Navigation to a tool first retains a nonempty member conversation through the
  existing serialized writer. Save failure stays in Chat; lock/session change,
  disposal or a changed transcript cannot complete an obsolete handoff. The
  welcome alone does not trigger a vault write. Tool actions cannot compete with
  active assistant/voice work. Manual weight opens its existing editor above
  Chat; image/label open panel pages over the current destination. Assistant tools
  still opens the existing model/catalog destination.
- One composition owns the timeline/composer throughout size changes. The size
  preference is saveable UI state. Short windows, enlarged text and the keyboard
  use full available height automatically, returning to the preference afterwards.
  The context card scrolls with the conversation instead of consuming fixed
  keyboard space. Dismissal and review keep the previous session/disposal policy;
  resizing does not reset a request, recording, clarification or draft.
- The panel is in the current window so existing modal editors still open above
  it. The underlying shell is excluded from accessibility traversal while Chat
  is open; it returns when closed. Controls are at least 48 dp and use the Metria
  theme/Material semantics. There is no decorative drag handle promising an
  unsupported gesture; expansion is an explicit labelled control.

## States and retained boundaries

Idle/welcome, restored history, model preparation, listening/transcription,
response/draft loading, success, empty records, partial nutrition, errors,
stopping/cancellation and lock use the existing actual state and callbacks.
There are no prototype timers, fabricated records or simulated success states.
Draft edits and saved-record edits remain distinct. Saved meals still open their
existing editor; only final reviewed Save writes a meal. Typed weight retains
its existing reversible save, and spoken weight retains review. Conversation
retention remains encrypted, 50 messages/24 hours, excluded from archives.

## Verification and handoff

- Pre-existing work: 17 modified tracked files and two untracked Chat drink
  policy/test files on `ee36df2`. This slice touches only the assistant surface,
  shell wiring, its tests and the applicable design/checkpoint documents.
- Final `:app:testDebugUnitTest :app:assembleDebug --no-daemon` gate passed:
  714 tests, zero failures/errors/skips. `git diff --check` passed.
  APK SHA-256: `93c2a891f4bbb7cf91e85c6652e15eda6a9c2f1dba128d366a6680b3c5f24bac`.
- Preservation audit confirmed 12 unrelated tracked diffs byte-for-byte and
  the navigation/icon file unchanged; shared assistant/document hunks were
  reviewed against the pre-edit backup.
- Added presentation-policy tests for compact/expanded portrait, short windows,
  keyboard, font scaling and restoration of the requested size.
- The project has no Compose instrumentation test setup. No new test dependency
  was added; unit tests cannot establish physical layout or interaction quality.
- Target phone: Samsung SM-S928B / Android 16 (reference device; not inspected
  in this slice before installation). Member authorized installation and panel
  verification. `adb install -r` returned `Success`; cold MainActivity launch
  returned `Status: ok` (1,309 ms). No clear/uninstall or private-storage change
  was performed. Member unlocked; retained conversation was visibly restored.
  Full meal/weight/model preservation was not audited in this panel smoke test.
  Approved captures must be temporary outside the repository
  and acceptance evidence redacted.

Physical gate pending:

- [ ] Capture authorized before/after idle and keyboard or loading evidence.
- [ ] Same icon; open, expand/reduce, Close, scrim and Android Back; underlying
      screen and navigation retained.
- [ ] Chat, native Copy, latest-message Edit, new-conversation confirmation,
      evidence expansion, model preparation, empty/partial/error/retry.
- [ ] Voice auto-send once; resize while listening/transcribing; Stop, Type
      instead, cancel, lock/resume; no late submission or lost unsent text.
- [ ] Existing mL/serving/gram corrections, repeated Review/cancel, Discard,
      saved-dish choices, final Save, View meal and return to Chat.
- [ ] All `+` routes, camera/label cancellation, manual weight, saved weight
      Edit/Undo and spoken weight review without unintended Save.
- [ ] Portrait/landscape, keyboard, rotation, enlarged fonts, TalkBack, reduced
      motion, system bars and reachable last row/action.

Do not mark the UI task fully accepted until these applicable phone gates pass.

### Integrated capture follow-up - 2026-10-25

- Analyse meal and Scan nutrition label now remain in the contextual frame,
  retaining the original navigation/icon. Source selection, private preview,
  optional meal details, real progress, Stop, cooldown, errors/manual fallback,
  result, Review and Discard share one scrollable panel. The OS camera/picker
  opens only on the corresponding member action; no simulated viewfinder.
- Photo results require the explicitly started request identity and a validated
  complete/partial outcome. Request identity and camera URI survive configuration
  recreation; unavailable process-local images/results offer a fresh capture,
  never automatic re-analysis. Cancel/discard revokes the request and existing
  preparation/OCR generation; late work cannot restore the discarded draft.
- Labels have an inline focused review with product, four existing nutrition
  fields, date and optional private reference. Values survive returning to the
  draft and configuration changes. The shared record validator remains required;
  Save failure keeps the editor and inputs. Only successful explicit Save exits.
- Photo Review retains the shared full meal editor and all its catalog,
  portions/corrections/uncertainty controls. Cancel returns to the same panel
  draft; successful Save returns to Chat. Today photo routes are unchanged.
- Options now also includes Privacy and Lock alongside New conversation and
  Assistant tools. Existing serialized conversation retention precedes handoff.
- Gate: 718 tests, zero failures/errors/skips; debug assembly and diff check pass.
  Four new tests cover request/result identity, partial drafts, repeat review,
  cancellation/stale results and reviewed-label validation/unknown values.
  Twelve unrelated tracked diffs and original navigation file preserved.
  APK SHA-256: `a39cc92a44c825bb58a828d385c5b5fbade0bb359932cc94e25524be71fd021f`.
  Final adjustments label the reference-photo checkbox for accessibility and
  expose assistant setup/recovery when photo analysis is unavailable.
  Final APK installed in place on Samsung SM-S928B / Android 16: `Success`;
  cold launch `Status: ok` (1,474 ms). No clear/uninstall or storage replacement.
  Member unlock and actual retained-record/visual interaction checks pending.
- Physical gate pending: entry/options, camera/picker cancellation, real image
  preview and processing, partial/error/Stop, both reviews, Save/reopen,
  keyboard/rotation/font scaling/TalkBack. Before-capture attempt found only
  the phone's sleeping display; it does not establish visual evidence.

### Physical smoke verification - 2026-10-24

- Samsung SM-S928B / Android 16; same verified APK and automated gate above.
- Pass: existing central green icon opens compact panel directly; retained
  conversation remains visible after closing with Android Back and reopening.
- Pass: expand control increases usable height; keyboard leaves the field,
  microphone, tools and Send visible above the IME and system navigation.
- Pass: `+` displays Speak, Chat, Analyse meal, Scan nutrition label and Add
  weight; header options display New conversation and Assistant tools.
  Opening tools dismisses the keyboard; Android Back dismisses menus and,
  after keyboard/focus handling, returns to the existing Assistant screen.
- No new message, recording, record write or destructive conversation reset
  was triggered. Left the compact panel open for member inspection.
- `android layout --flat` returned an unrecognized instrumentation response;
  visual fallback used temporary annotated captures outside the repository.
  No raw screenshot is retained as repository evidence; only sanitized findings.
- Still pending: shrink/Close/scrim controls, tool-route end-to-end behavior,
  voice and draft/review regressions, rotation, font scaling, TalkBack, gesture
  navigation and loading/error/cancellation states. This is partial physical
  acceptance, not a claim that the entire matrix passed.

### Integrated capture phone inspection - 2026-10-25

Follow-up 2026-10-26 on APK `3db12eb3...` (738-test full Android gate): the
previously failing member-selected dual-column label now has all four fields
from the same per-serving column. Inline review, Date/Save reachability above
IME, retained draft in landscape and system-bar clearance checked on phone.
Discard confirmation returns to retained Chat. Meal picker and camera cancellation
return to empty inline capture. No records saved. Meal analysis/Stop/partial/error,
font scaling and TalkBack remain pending; Save/reopen is excluded from this cycle
by the member's no-save instruction. Full evidence: `../m4-assistant-checkpoints.md`.

- Follow-up after the usage-limit interruption: on the same APK, member-selected
  label preview, actual local OCR, draft warning and inline review were observed.
  Focusing Date keeps its value visible; scrolling with the keyboard open reveals
  the complete reference-photo row and final Save button above the IME. Named
  portrait/default-font label keyboard check passes. No values edited or Save
  performed; keyboard dismissed with review left open.
- Accuracy limitation observed: the dual-column label produced nutrition values
  mixed across serving/container columns. Record this as a failed extraction
  case requiring a separate parser correction; review UI success does not imply
  nutrition accuracy. No image or extracted private values retained in these docs.
- The earlier transient draft was unavailable when returning after interruption;
  the cause is unproven. Rotation/restoration, meal-analysis end-to-end, Stop/error,
  explicit Save/reopen, font scaling and TalkBack remain open.

- Samsung SM-S928B / Android 16, final `a39cc92a...` APK above, member unlocked.
- Visually verified options inside the panel: New conversation, Assistant tools,
  Privacy, Lock and retention note. Actions list contains Speak, Write, Analyse
  meal, Scan nutrition label and Add weight. Original green icon/dock retained.
- Opened Analyse meal and Scan nutrition label: both use the white contextual
  surface over Today, botanical source area and Take photo / Choose image
  controls. No old source sheet or Assistant destination transition observed.
- Android Back from meal capture returns to retained Chat. Scrolling meal
  capture exposes the complete final privacy note above the dock; primary and
  secondary source buttons remain reachable. Portrait/default font only.
- Left label capture open and requested a member-selected test image. Preview,
  actual analysis/OCR, editing/keyboard, cancellation with work, Save/reopen,
  rotation/font scaling/TalkBack remain unverified. No photo selected, analysis,
  new message or record Save performed during this inspection.
- Layout instrumentation still returned an unrecognized response; temporary
  annotated screenshots were inspected outside the repository, then removed.
