# Metria screen contracts

Assistant completion (approved 2026-10-26): My saved dishes is an inline panel
page with Back to chat, a labelled search field, readable selectable dish rows
and explicit empty/no-match states. Selection opens the shared unsaved Review;
it never saves automatically. The list uses the Metria theme, scrolls above the
keyboard/dock and supports enlarged text. Label Review exposes a labelled meal
category selection with a clearable clock default. Successful contextual label
Save and assistant-origin meal update show host-authored Chat receipts with
View meal only after their atomic vault operation succeeds. Cancellation and
failure create no receipt. No new persistence or background-draft promise.

Assistant save follow-up (2026-10-26): after a successful contextual meal Save,
Chat shows the existing saved-meal acknowledgement and View meal action, linked
to the actual saved record. No success message on failure. In assistant meal
Review, Meal details starts with an editable local-time category when no explicit
Today/member category exists; a member-cleared value remains unclassified.

Assistant capture pages (2026-10-25): use the same panel header, expandable white
surface and original navigation as Chat. One dominant action per stage: Take photo,
Prepare draft/Read label, Review, or explicit Save in label review. Source/preview
uses a botanical tonal area; results use a bordered draft card with visible
uncertainty. Inline scrolling, IME insets, source cancellation, cooldown, Stop,
partial/error/manual fallback and unsaved-exit confirmation are required. The
shared photo meal editor preserves all detailed correction tools. Physical gates
remain pending in `assistant-context-panel.md`.

Chat drink quantities (2026-10-24, in development): use the existing Chat timeline,
activity state and Review action. Applied amount feedback identifies mL or the
measured serving. Unsupported/ambiguous/invalid amounts explain that the draft is
unchanged and keep Review available. Review displays the chosen unit using its
existing concise quantity control and source conversion; no new layout or primary
action. Shared typed/spoken behavior and acceptance: `../drink-portions.md`.

Drink Review (2026-10-22): identify a reused saved portion with one concise note;
keep Edit and explicit Save authoritative. Quantity controls show readable mL or
servings, label rounded conversions as approximate, and retain exact mass until
text entry establishes a new quantity. No extra preference screen or popup.
Saved-meal reopening retains the stored amount/unit; new defaults do not rewrite
existing records. Acceptance: `../drink-portions.md`.

Chat correction revision (2026-10-15): preserve the original message and existing
Chat hierarchy while validating up to three corrections/abbreviations together.
Distinct valid meanings show a concise host clarification in the ordinary reply
bubble, with selection/retention and no automatic record operation. Privacy and
assistant detail dialogs disclose up to three individually checked words, never
the full question or records. Existing scroll/font/TalkBack checks remain open.

Shared typo recognition (2026-10-13) must not visibly rewrite sent messages or
change the existing Chat hierarchy. A local evidence card alone is not a
completed answer: a rejected current turn must expose recoverable error feedback.
The reported missing feedback remains a physical investigation gap; no UI fix
is claimed from static inspection. See `../assistant-routing-recognition.md`.

Approved completed-failure retention: current terminal rejected turns display
one host-authored recovery message in the existing assistant timeline. Preserve
text selection, normal auto-scroll and encrypted retention. Do not duplicate it
with the transient diagnostic card or consume a pending tool/navigation action
as a failed turn. Physical visibility/restoration acceptance is a separate gate.

**Approved phase calories — 2026-10-08:**
[phase calorie contract](phase-energy-contract-proposal.md). A focused review shows
the explicitly selected maintenance reference and approximate range, signed
percentage/kcal adjustment, and dominant daily calorie proposal. One primary action:
Use calorie proposal. Manual entry, method disclosure and Back/Skip are secondary.
Calculate/Use never save; final phase confirmation shows source-aware changes.
Current/saved/history calorie values distinguish maintenance, phase-adjusted and
manual sources. No personal forecast or measured-need claim. Missing/unsupported
states retain manual tracking, cancellation preserves prior draft, and changed
dependencies remove derived values while preserving explicit manual overrides.
Keep editing restores a visible sheet; keyboard/insets/font/TalkBack gates apply.
Code/build verification and physical acceptance are tracked separately.

**Approved optional macro suggestions — 2026-10-08:** see
[calculation contract](phase-nutrition-contract-proposal.md). Focused goal review has
an optional training/macro route. Inputs lead to a proposal showing the current
budget and three gram values, with Use suggestions dominant and Edit/Back secondary.
No artificial loading or write during calculation. Missing/unsupported inputs offer
manual entry, with no inferred sport classification or unused frequency/diet fields.
Input changes invalidate stale sources; explicit macro overrides remain per-field.
Final confirmation, exact saved summary, current and historical details distinguish
suggested from manual references and disclose original method/inputs on demand.
Calories retain independent maintenance/manual provenance. Keyboard/insets, scrolling,
48dp actions, selection text/icons, TalkBack and font scaling remain phone gates.

All proposal routes show two independent checks: whether the general option is
suitable without individual clinical or eating-disorder support, and whether
pregnancy or breastfeeding applies. Do not derive the latter from an equation
coefficient. An applicable answer leads directly to care-team reference entry;
generated values and hidden source snapshots are cleared, explicit member-entered
values remain, and Back cannot restore the cleared source. The applicability answer
is transient and is not saved. Manual entry offers the applicability choice without
requiring an answer to save manual references; macro proposals require both checks.

These are prototype contracts for the first redesign pass. Visual mockups must be approved before implementation changes navigation or replaces a complete screen.

## App shell

**Purpose:** keep location, primary destinations, and the Metria action predictable.

- Stable destinations: Today, Progress, Privacy/Settings.
- Central Metria action is visually distinct and available from every unlocked primary destination.
- Selected navigation state uses icon, label, and tonal background—not color alone.
- Transient menus dismiss on outside tap or back and restore the current screen unchanged.
- System bars, gesture navigation, keyboard, orientation, and resumed-process behavior are explicit acceptance states.

## Today

**Purpose:** answer “How is today going?” and make logging immediate.

**Hierarchy:**

1. Contextual greeting/date and quiet profile/privacy access.
2. One daily/weekly hero with the most relevant progress summary.
3. Full-width goal/weight summary and a separate nutrition summary card.
4. Day strip for recent dates.
5. Today’s meals as visual rows grouped by meal time.

**Primary action:** central Metria action.  
**Secondary actions:** tap a metric for detail; tap an entry to edit; add from an empty meal slot.

**Empty state:** explain one useful first action, not the entire product.  
**Partial state:** show the available totals with the estimate marker and an accessible explanation on tap.

**Today goal-card refinement — 2026-10-06:** replace the latest-weight KPI with
the shared current-goal card. Show latest recorded weight (or labelled starting
reference), member-entered target and optional chosen range, in the selected kg/lb
unit. No range or goal is inferred. Update weight opens the existing weight-entry
sheet; Goal and daily references opens Goals. With no goal, show a calm weight
summary and optional Set up a goal action; tracking remains available. Keep these
actions outlined/text so the central capture action retains visual priority.
Nutrition remains a separate full-width card, with no duplicate goal-entry row.

**Approved meal-entry direction — 2026-10-06:** group member-classified meals
under Breakfast, Lunch, Dinner and Snacks with a quiet labelled Add action.
Existing records without an explicit meal type remain under Other meals; do not
reclassify them from inferred times. Add opens a focused method chooser and
passes the chosen type into the visible meal review. Photo/manual saves return
to Today; chat-origin saves retain the existing Chat confirmation/View meal.
Type/Speak from a meal card also stay in a dedicated contextual meal flow;
they do not open Chat. The explicit section survives draft creation and review.

**Today nutrition refinement — 2026-10-07:** Today's nutrition card presents
calories, protein, carbohydrates and fat from the existing recorded-meal summary.
Known complete and partial contributions are included with explicit coverage;
missing values remain unknown, and reported zero remains a value. Expand details
for complete/partial/unavailable meal counts and a quiet daily-reference route.
This is recorded nutrition, never a claim of total intake or a nutrition score.
Current daily references remain optional. Narrow/enlarged-text layouts stack
content instead of forcing metrics and references into a fixed-width row.

**Compact Today summaries - 2026-10-07:** Today uses a dedicated compact goal
card instead of the full goal-detail component. A short intent/weight summary,
quiet goal-detail affordance and Update weight action remain. A small visual
marker uses only the member's chosen range, or a distinct starting reference and
target. Bounds and current value have textual equivalents; outside-range values
are disclosed rather than hidden by visual clamping. No range, weight target or
judgment is inferred. Empty/track-only goals remain useful without a graph.

Nutrition uses a compact expandable header, kcal summary and small macro rows.
Bars compare known recorded estimates with existing positive daily references;
they do not imply complete intake, a score or a health recommendation. Drawing
may cap at the reference but actual values and over-reference text remain visible.
Unknown data has no filled bar. Estimate/partial and meal coverage stay visible;
expanded details retain the counts and reference-editing route. Large text or a
narrow viewport expands vertically rather than clipping. Detailed goal screens
retain their existing full cards. The central capture action remains dominant.

## Progress

**Purpose:** answer “What changed over the selected period?”

**Hierarchy:**

1. Screen title and date-range control.
2. Segmented metric selector: Weight, Calories, Protein.
3. One dominant metric and goal context.
4. Accessible chart with a selected day/value.
5. Small supporting cards only when they add distinct information.

**Primary action:** choose a point/day to inspect.  
**Secondary action:** set or edit a goal in context.  
**Rule:** do not place Today’s full dashboard inside Progress.

**Approved evolution extension - 2026-10-27:** replace the initial fixed
seven-day chart with Your evolution and 7-day, 1-month, 3-month and 1-year
periods. Weight defaults to 3 months; Calories and Protein to 7 days. Retain
per-metric period and selected reading across configuration/detail return.
Use actual dates and unknown gaps, neutral dated weight change, accessible
point selection and partial nutrition disclosure. View detail shows a larger
chart and read-only dated values; All history keeps its existing edit routes.
See [the approved contract and acceptance status](progress-evolution.md).

**Progress goal context — 2026-10-07:** below the selected-period chart, a compact supporting
card shows the current direction and optional member-entered weight target/range
in the selected kg/lb unit. It identifies daily references when present and opens
the existing goal hub. Missing goals offer optional setup without interrupting
tracking. The card explicitly separates current references from the selected
past day; it does not repeat Today's weight hero, display a nutrition score or
invent a goal. The nutrition hero labels its comparison as a recorded estimate
against the current daily reference, rather than historical goal adherence.
Chart/day inspection stays the primary purpose; goal editing is a quiet action.

## Photo preview — approved companion slice, 2026-10-06

Purpose: check the selected photo before requesting a local draft. A full-height
scrollable sheet shows a reduced preview without cropping, explicit Meal photo /
Nutrition label selection, on-demand framing tips and replace/cancel controls.
Create meal draft is the dominant action; Read label replaces it in label mode.
Preparation shows a labelled progress indicator and disables analysis. Failure
offers replacement or cancel. Meal details are optional, at most 600 characters,
local and transient; changing mode/image clears them. Label mode omits them.
The existing validator and reviewed-save policy remain unchanged.

**Member decision — 2026-10-07:** retain Meal photo / Nutrition label as the two
photo modes. Barcode is deferred until product lookup can remove steps rather
than leading to a second label scan. No barcode control, decoder dependency or
product lookup is part of this UI cycle.

Cancel deletes the prepared image and invalidates background preparation. Lock
or leaving the assistant discards an unanalysed preview; backgrounding cancels
meal analysis. Rotation retains the active unlocked preview via the existing
orientation configuration policy; process restart never replays analysis or
persists the description. Keyboard-safe scrolling keeps the action reachable.
No custom camera, barcode lookup, new dependency or cloud processing is added.

Physical checks and before/after evidence remain required before acceptance.

**Capture presentation refinement — 2026-10-07:** camera and library actions
include recognizable icons and text labels. Photo tips open a focused dismissible
dialog with short framing/light guidance and Done, rather than expanding prose
inside the preview. Photo analysis uses one short Preparing your meal status,
a progress indicator and Stop analysis. The existing system camera and photo
picker remain the capture mechanisms; a custom live camera is outside this slice.
From a Today meal card, photo entry has its own section-specific destination and
minimal loading state, without Assistant home or the main navigation tabs.
The chosen section/date reaches both meal and label review. Back returns to Today
and discards temporary capture; label Stop invalidates any pending OCR output.

## Meal review

**Drink quantity controls - 2026-10-22:** keep the existing food/dish editor as
the focused entry surface. For drinks, show a readable amount and selected unit,
with grams, available mL and measured serving choices progressively disclosed.
Serving labels identify their defined size, never a guessed household cup. Show
the conversion basis concisely when needed; no unsupported nutrition calculation.
Reuse the same controls in the saved-meal editor. Invalid amount/conversion is
visible and blocks Apply/Save; Cancel restores prior amount/unit. Selection and
errors have text/semantics, targets are at least 48 dp, controls wrap and remain
reachable with keyboard/font scaling. Unit changes alone do not save records.

**Explicit Chat meal type - 2026-10-21:** preselect an unambiguous meal type stated
in the original description, such as "for dinner", when no explicit Today section
was supplied. This is host-owned draft metadata, not a model food-schema field or
time-of-day guess. Use the existing editable meal-type controls; member changes
and clearing remain authoritative. The initial exit snapshot includes this prefill,
so untouched preselection alone is not reported as an unsaved member edit.

**Automatic photo review - 2026-10-11:** after the member explicitly starts
Create meal draft, a validated matching photo result opens review automatically
once, including partial drafts. No additional result-screen tap is required.
Loading remains minimal; rejection/runtime failure stays in entry with new-photo
and manual options. Stop, leaving or a newer attempt prevents stale navigation.
Returning from review does not automatically reopen it. Contextual date/section,
thumbnail choice, uncertainty, deterministic nutrition and explicit Save remain.
Nutrition-label review is unchanged. Physical transition/cancellation checks remain
required; code/build evidence alone does not close them.

**Saved-meal assistant entry — 2026-10-07:** the saved-meal editor offers a quiet
Ask Metria action for the saved record. Changed edits must be saved or cancelled
first; the action neither saves nor discards them. Chat opens with a removable
selected-meal indicator and preserves existing composer text. It never submits
automatically. Send may request a fresh read of that exact host-held meal ID for
its saved foods/nutrition; broader/latest/date/progress questions retain their
existing bounded routes. The selection is transient and one-turn only, and clears
on lock/background/new conversation/deletion. No ID is model-authored or sent in
prompts. Answers describe saved values and coverage, not inferred health quality.
Manual tracking and cancellation remain available if the local model is absent.

**Purpose:** turn an assistant draft into a member-confirmed meal.

**Hierarchy:**

1. Title, source thumbnail, and one-line estimate status.
2. Meal name and optional remembered-dish suggestion.
3. Collapsed dish cards with ingredient summaries.
4. Compact nutrition summary that updates from confirmed components.
5. Meal date/time and type behind “Meal details” unless attention is required.
6. Persistent bottom save action.

**Primary action:** Save reviewed meal.  
**Secondary actions:** edit dish, add dish, review estimate details, Leave without saving.

**Rule:** never render every ingredient’s name, description, grams, preparation, role, match, and delete action in one continuous form.

**Available nutrient display — 2026-10-06:** expandable details show all available
nutrient values for selected offline matches and validated portions, with USDA
source/release, source units and per-nutrient food coverage. Reported zero is a
value; absent data is unknown. Never sum unlike units or invent missing values,
daily reference percentages or micronutrient targets. These live draft values
do not add fields to saved meals or rewrite existing nutrition. Manual/label
values keep their own provenance. Further persisted nutrients need a separate
schema/archive decision.

The persistent action area shows **Leave without saving** as a clearly
secondary action. Leaving an unchanged review exits immediately. If the member
has changed save-relevant meal details, the footer action and Back both ask
whether to keep reviewing or leave without saving. Leaving discards only the
current review edits and makes no vault write; changing a field back to its
original value removes that warning. Keyboard Back still hides the keyboard
first, and dish-editor Back still returns to meal review.

Leaving review retains the original unsaved proposal. Both photo and text
proposal cards offer a secondary **Discard draft** action with a short
confirmation. Confirming removes only that proposal and its unsaved photo
thumbnail where applicable; it does not delete saved meals or conversation.
Dismissal or **Keep draft** leaves the proposal available.

**Correction learning:** successful Save may retain bounded, structured
draft-to-confirmed edits locally and automatically. Do not add a toggle or a
second save prompt. Keep "Remember this dish" as a separate reusable-dish
choice. Privacy offers inspect and clear controls without deleting meals.

Saved-dish suggestions appear immediately after the dish they concern and
name that target dish. Their placement and reading order must not make a
suggestion for one dish look attached to a neighbouring dish.

**Rejected-photo fallback:** show a concise explanation and one **Enter meal manually** action. The manual review starts without model-derived foods or an AI-confidence badge; its initial primary action is **Add first dish**. Save remains unavailable until the member has added a valid food. An unmatched manual meal is stored as manual entry, not an AI estimate; a confirmed catalog match retains its catalog provenance. The optional reduced thumbnail still requires member confirmation.

**Partial-photo draft:** if the root is valid and at least one food independently passes strict validation, retain only validated foods. Show a concise, nontechnical notice on both the result and review screens that some suggestions were left out and the member should add any missing foods. A partial draft has low overall confidence; it is not a complete meal or a complete nutrition estimate. Unknown fields, unsupported values, and invalid portions are never guessed into accepted foods. If no foods survive, use the rejected-photo fallback instead.

A separately grouped visible sauce with a valid ingredient is a reviewable
suggestion, not an omitted dish. Its portion, evidence, and role still pass
normal ingredient validation; malformed sauce ingredients remain partial.

The similar-meal suggestion button is **Use meal name**. Supporting text must say this changes only the meal title; food names, dish contents, and nutrition remain for separate review.

## Dish editor

**Purpose:** correct one dish without repeatedly typing its name.

- Dish name appears once at the top.
- Components are compact rows with name, portion, evidence state, and match status.
- Tap a component to edit its details in place or in a subordinate sheet.
- “Add component” asks whether it was visible or is a possible hidden ingredient in member language.
- On one expanded food at a time, the member may describe a label correction in ordinary language. The local text model may return only a replacement name; show the old → new preview with explicit **Apply name** and **Discard** actions. Apply updates only that food name and clears its catalog match. It must not alter portion, preparation, components, or nutrition, and nothing is saved until the existing dish Apply and meal Save actions.
- Busy/unavailable model, invalid response, cancellation, stale request, and keyboard-visible correction states leave direct name editing available. A result for a previous request or different selected food is never applied or presented as current.
- Components can move between dishes without retyping the destination name.
- Deleting a component does not dismiss the editor.
- Save/cancel are persistent and keyboard-safe.

## Saved meal editor

**Dismissal and catalog refinement - 2026-10-07:** a changed editor vetoes
sheet hiding before showing Keep editing / Leave. Keep editing retains a visible
sheet and its draft; Leave removes the sheet and its blocking layer. Back closes
catalog search or the focused ingredient before leaving the meal. Catalog search
shows its available food count and source, has a keyboard-aware Back to ingredient
action, and explains that no match may mean the food is absent. Missing nutrition
stays unknown; search does not manufacture a replacement value. Saved entries
without photos use a drink icon for reviewed drink items or exact standalone
coffee names; mixed meals and coffee desserts retain the meal fallback.

**Companion detail slice — 2026-10-06:** show four summary metrics, compact food
rows and a focused ingredient editor. Apply stages food edits; Cancel restores
the food and prior match. Name/preparation edits clear stale matches. Save/Leave
remain in a keyboard-aware footer; changed whole-meal drafts confirm leaving.
Date/type and manual nutrition edit fields expand on demand. Catalog-backed
whole-portion changes scale all valid grams together (0.25–4 multiplier, nearest
integer, each 1–2,000 g). Invalid input rejects the entire change. Manual and
label macro values are not automatically scaled. Source/coverage stays visible.

## Voice entry

**Continuous Chat activity - 2026-10-21:** ordinary Chat capture uses one inline
activity card rather than the modal below. The same compact botanical mark,
indeterminate progress and surface continue into response/draft loading. Display
one activity state at a time; preserve Stop/Cancel/Type, idle-only pencil editing,
48 dp controls and keyboard insets. See `chat-activity.md`. The modal description
below remains for the separate meal-entry branch.

**Approved ordinary Chat revision - 2026-10-20 (implemented, phone gates pending):** keep
explicit Start recording; tell the member that recognized words send automatically.
Listening and Transcribing have clear accessible status and Stop. A valid current
transcript closes the sheet, appears as the member message and submits once through
normal Chat, without editable preview or Continue. Empty/invalid speech stays in
the sheet with Retry and Type instead. Revoked/stale speech cannot send or revive
a draft. Keep native runtime busy until worker exit. Ordinary response/loading
then uses the existing Chat timeline; state labels must reflect known work.
The latest member bubble offers a secondary Edit action when idle.
This action is a pencil icon without visible text, with a 48 dp touch target
and the accessibility label "Edit as new message" (member decision 2026-10-21).
It focuses the composer with that text; Send submits a new turn and leaves history intact.
Edit itself performs no action; Send is a new command and can prepare a new draft.
It does not undo an already completed action.
No automatic record save; meal Review and final Save remain required. Preserve
long-press text selection/copy, keyboard insets and 48 dp controls. Today meal
entry is unchanged. Earlier ordinary Chat preview/Continue is superseded.
For a spoken weight write, require explicit confirmation of the interpreted
weight before persistence; ordinary transcription completion cannot save it.

**Purpose:** recognize a short spoken request and continue the current Chat task.
Speak opens a dedicated sheet without starting the mic. Start recording requests
permission; Listening and Stop recording stay explicit. The existing recorder
stops after speech pause or 12 seconds. Local recognition returns plain words,
bounded to 600 characters; audio-returned actions are withheld from dispatch.
Today meal entry automatically submits a successful validated transcript to draft
generation, with no intermediate correction screen. Ordinary Chat follows the
automatic typed-routing contract above. No record is saved by recognition alone. Retry, Type instead,
Stop processing and Cancel remain available; errors/empty speech keep typing.
Cancellation/disposal invalidates late capture callbacks and deletes temporary
audio. Lock clears entry presentation. No new transcription dependency or cloud
speech service is introduced. Recognition accuracy and physical gates are open.

For a live eligible ordinary Chat draft, preserve the draft while transcription
runs through a separate transient request. Shared text submission requires the
exact source/current-draft checks; audio actions cannot change the draft. All
ordinary Chat captures require current one-use unlocked capture/session authority.
No request identity is rewound or restored from history. The recognizer receives
no Chat history and rejects empty, invalid or structured output. Only accepted
normal text routing can create a draft; no placeholder voice message is added.
After explicit Stop, Type instead quietly opens the composer if the unlocked
conversation/request is unchanged. Cancellation grants no authority to submit
recognized words or revive a draft. A real changed/stale source still explains
why the voice request was not applied. History and current draft remain intact.

**Today meal-entry refinement — 2026-10-07:** a dedicated full-height focused
destination handles typed descriptions and spoken meals from a selected card.
Its purpose is creating one reviewable meal, with the section visible in the
title. Idle voice offers a large labelled microphone action; Listening exposes
Stop and elapsed time. Recognizing speech and Preparing your meal use a compact
progress indicator with one short line, accessible status and cancellation.
If Stop overlaps automatic recording completion, Finishing recording is shown
with an accessible state label until its queued clip arrives. The recording
action is disabled during that handoff; navigation and Type instead cancel it.
Successful recognition transitions directly to Preparing meal draft and then
review; the words remain editable only on generation failure or cancellation.
Typed text retains Create meal draft as its dominant action.
Failure/empty speech retains a typed fallback; draft failure
retains the description for retry or explicit manual review. No subscription
banner, conversational transcript or inferred action is part of this view.
The existing strict local proposal validator and review/save gates remain.
Controls use theme tokens, 48 dp targets, scrolling and keyboard/system insets.
Physical voice, cancellation, save-to-section, rotation and accessibility checks
remain required; model or speech accuracy is not claimed from the UI redesign.

**Failure inspection — 2026-10-07:** in debug builds only, a failed contextual
meal draft may expose a quiet Draft details action. It opens the current
attempt's existing in-memory validation category and model output for deliberate
inspection without leaving the entry screen. It is absent from release builds
and cannot show another operation's response. No prompt/output logging, file
capture, vault write or new persistence is introduced. Retry, cancellation,
lock/background and the existing response lifecycle invalidate the details.

## Saved meal editor (existing behavior)

**Purpose:** correct an existing entry while keeping all changes reviewable until one final save.

- Keep direct food-name editing visible. Offer **Ask Metria about this food** beside each food and reveal one correction panel only for the selected food.
- The local text assistant may propose one replacement food name. Show the old → new names and require **Apply name** or **Discard**. Applying edits only that draft label and clears the old catalog match; the member may separately choose a new local catalog match.
- Manual nutrition values remain unchanged by a name suggestion and remain identified as member-entered. Catalog nutrition is recalculated from the member-confirmed matches and portions; unmatched foods are excluded and coverage remains visibly partial or unavailable. Before a catalog-backed save loses coverage, ask the member to confirm; block the save if the local catalog or an unchanged saved match cannot be resolved.
- **Save meal changes** is the only ordinary edit write. A vault write failure is shown inside the editor and retains the draft without replacing the unlocked shell. Removing the reference photo is staged until Save; Cancel/back leaves the stored meal unchanged.
- Saveable draft fields survive rotation. An in-flight correction is cancelled on editor exit/recreation; request and selected-food checks prevent stale proposals from being applied.

**States:** direct edit, assistant unavailable/busy, loading, result, stale result, apply/discard, manually selected replacement match, partial/no catalog estimate, manual nutrition unchanged, pending reference removal, keyboard visible, rotation, cancel, validation/save failure, save/reopen.

## Assistant overlay and conversation

**Approved contextual panel - 2026-10-24:** replace only the assistant's
presentation with a compact/expanded in-window panel, entered from the existing
green SmartToy action. Use the prototype's continuous white surface, soft member
messages, plain assistant prose, rounded unified composer and inline action views.
Reuse the original navigation dock when the keyboard is closed. Keep all existing timeline, voice, evidence, action cards,
review, Copy and saved-meal links. A full-width composer has `+` for existing
capture/manual routes; header options retain confirmed New conversation and
Assistant tools. Size changes retain one composition; keyboard, large fonts and
short windows use the full available height. Only the already-bound selected
meal receives a context label. The background shell is excluded from accessibility
traversal while open. This supersedes the initial full-screen/selector layout
below without changing other screens or business rules. Detailed state and
acceptance contract: [assistant-context-panel.md](assistant-context-panel.md).

An explicit single-food label correction admitted by the packaged food vocabulary
may complete locally (2026-10-19), using the same acknowledgement/card and no
additional screen. Review still has no selected nutrition match; retained grams
are member-entered, not a newly inferred amount. Unknown labels use normal model
handling. Apply only under current unlocked draft/request/session authority.

Current-draft single-food replacement (2026-10-18) reuses the composer, timeline
and Review card. A clear replacement for the sole simple food acknowledges old
and new names, retains the chosen amount and leaves Review dominant. The Review
food row uses the new label with stale descriptive/matching context removed;
the member chooses a fresh local match before replacement nutrition is available.
Uncertainty remains visible. Failed/ambiguous interpretation leaves the previous
draft usable; cancellable thinking, Stop/lock/reset/Discard and no stale restored
authority follow current portion editing. No new screen or automatic Save.

Current-draft portion correction (2026-10-18) reuses the draft card and composer.
A subsequent explicit amount or understood natural correction updates the same
eligible food and retains a concise acknowledgement. Review remains the dominant
action, with Discard available. Model interpretation can show existing cancellable
loading; the food proposal is not regenerated. Invalid input leaves the previous
amount and Review usable. Lock/Stop/Discard/new context clears edit authority;
returning from Review in the same unlocked draft retains the member's amount.
A clarification or unsuccessful intent selection must not remove that draft
card; keep the previous portion available for retry and Review.

A contextual meal confirmation preserves the current pending member food
description for the unsaved draft. The assistant interprets natural replies;
invitation wording/punctuation does not establish the pending action. Never
present “yes” or another acknowledgement as food. An expired/missing pending
request gets a concise clarification and cannot reopen an old action from
history. Existing loading, Stop, review and retained messages apply; no additional
confirmation screen is added.
Declining a current meal offer receives a concise no-draft acknowledgement.
Do not describe that decision as missing food context or ask for the same food
again. A rejected pending action must only state the no-draft outcome it knows.

One portion clarification (2026-10-17) uses the existing transient meal draft
card and composer. Show one short food-specific question with an example in
grams and a skip hint. Keep Review as the dominant draft action and Discard
available. A valid reply updates the unsaved draft and retains a historical
acknowledgement; it never claims the meal is saved or nutrition is verified.
Unsupported quantity feedback is concise and does not remove Review. Questions
are ephemeral; request/proposal/session changes cannot apply a stale reply.

Saved-dish clarification (2026-10-16) reuses the selectable Chat timeline and
existing candidate card, with consistent numbered candidates and a concise
question in the transient card, without retaining obsolete choice instructions
in history. A current explicit typed selection may open the same review as its
button after transcript retention and fresh record validation. No transient
choice authority survives lock, Stop, unrelated input or conversation reset.
An invalid/ambiguous selection must not silently choose a dish; failed saves
remain recoverable in Chat. Final meal Save remains a separate member action.

An equivalent bounded vault snapshot must not remove loading or a live response
by resetting Chat request identity. Compare the same normalized transcript
representation; preserve existing invalidation for changed/expired/cleared
conversation and lock. No stale completion may regain authority through refresh.

Ordinary meal draft completion retains an assistant message independently of the
temporary draft card. Review opens only after the transcript is saved; failure
keeps Chat open with a recoverable explanation. After leaving without Save,
history still shows that a draft was prepared. This acknowledgement provides no
reopen control and does not persist draft contents or imply a saved meal.

Bare consumption (2026-10-15) reuses the existing loading, named choice and Review
meal states. A saved-dish probe cannot save automatically, and no match must
continue into normal intent selection without a misleading terminal
saved-search error or a forced meal draft (2026-10-16 correction). Only a positive
meal intent opens the timed draft path; non-food input can receive normal prose
or clarification. Suppress repeat probes. Preserve original message, text selection, Stop and current
request/session guards. No new primary action or screen is introduced.

Natural saved-dish frames (2026-10-15) reuse existing named recall bubbles and
review-only draft offers. Preserve long-press selection, loading/Stop, no-match
and bounded ambiguity choices. Correct command wording only; keep dish names
unaltered. No automatic review opening or save is introduced; member taps Review
meal, edits known/unknown portions and confirms Save in the existing review.

**Approved companion bundle — 2026-10-12:** goal differences are concise recorded-
estimate comparisons with reference and coverage limitations, never prescriptions.
Saved-dish search presents at most three named possibilities; draft ambiguity
offers explicit candidate choice. The selected draft leads to the existing
focused review with saved foods/known amounts and blank unknown amounts. Review,
Discard and final Save remain distinct; no assistant action writes automatically.
New feature UI belongs in shared components/screens, not the shell coordinator.
Navigation may close Chat to a known app destination while retaining conversation
history, including the current question and opening reply. Leave only after
those messages are retained; save failure keeps Chat open with a recoverable
error. Late completion must not navigate after lock/Stop/new turn. Follow-up
answers read fresh records and maintain the selected day/metric explicitly.
Keyboard/font/TalkBack/rotation and visual gates remain required; the bundle's
build does not close physical acceptance. See `../assistant-companion-bundle.md`.

**Approved fast today overview:** use the existing assistant bubble for a short
recap of today's recorded meals, available calorie/protein estimates and
confirmed daily goals. Missing references/estimates and partial coverage stay
explicit. Use neutral language with no intake-completeness or medical claim,
judgment, recommendation or inferred weight trend. Preserve ordinary message
selection, error announcements and conversation retention. This adds no new
screen or action; acceptance is tracked in `../minilm-today-overview.md`.

**Approved message selection — 2026-10-12:** completed message text supports
long-press word/range selection, native selection handles and a floating Copy
toolbar. The member replaced the initial visible ⋮ menu design with this
interaction. Selection is contained within one message; saved-meal actions and
record disclosure remain separate. Copy the selected plain text only on explicit
member action, mark it sensitive for
Android clipboard preview handling, and never log or inspect clipboard text.
Keep this secondary to Send and preserve record collapse/saved-meal navigation.
No Copy is offered on transient thinking/streaming. Menus dismiss normally,
remain usable with keyboard/font scaling/TalkBack, and do not replay on rotation
or restoration. Physical interaction and visual gates remain separate.
Displayed recoverable errors also support text selection/Copy, announce politely, and scroll
into view when replacing thinking even if the visible item count is unchanged.

**Selected-function debug optimization — 2026-10-11:** an explicit English
question listing today's recorded meals may use the local MiniLM route to the
existing validated meal-list answer. Keep the same Send, thinking/Stop, evidence,
empty-record and encrypted timeline behavior. There is no additional member
screen or router terminology. Unsupported requests keep the existing assistant
flow; physical acceptance is tracked in `../minilm-chat-integration.md`.

**Approved weight shortcut extension — 2026-10-12:** existing Chat may answer
latest saved weight/date or recent weigh-ins from a fresh bounded local read.
Keep concise dated replies with explicit kilograms, read-window limits and
omission disclosure. No additional controls or router terminology appear to the
member. Existing thinking/Stop, empty, error, cancellation and restoration
behavior remains; code/build evidence does not establish phone acceptance.

**Proposed online extension, 2026-10-02:** use a separate, clearly labelled
online composer, a focused sharing preview with **Send online** as its primary
action, and read-only answers labelled **Online · Nebius**. The exact selected
summary, dates, coverage, question, processor, and verified retention terms
must be reviewable before sending. Privacy/settings controls enablement,
revocation, and bounded access metadata. See `../optional-cloud-assistant-spec.md`
for full states and bounds. This extension is not implemented or physically
accepted and must not silently change the existing local conversation.

**Purpose:** let Metria help without making the rest of the app disappear.

- Compact action menu: Speak, Chat, Analyse meal, Scan label, Manual entry.
- Listening state replaces the central action with visible motion, “Listening”, and Stop.
- Chat opens as a large sheet or full screen with a familiar message timeline, persistent composer, and cancel-generation control.
- Stop immediately invalidates the active reply and announces “Stopping…” while local runtime cancellation runs. Keep Send disabled until it finishes, then show “Response stopped” and re-enable the composer. A reset or lock during cancellation must not show a stale stop confirmation.
- Tool results appear as compact structured cards collapsed by default; the member can expand the
  exact local facts on demand while model prose stays short and conversational.
  Expanded nutrition evidence uses the member's language and shows the requested
  date, recorded-meal coverage, and an estimate with sensible display precision;
  it must not expose internal field names, `null`, or floating-point artifacts.
- Reopening the conversation never resubmits the previous message.
- Saving a reviewed chat meal adds a concise host-authored confirmation to the
  active encrypted timeline, using the final reviewed name and foods. The meal
  and confirmation persist together; leaving/discarding or a failed save adds
  no success message. The confirmation follows chat retention and does not
  replace a fresh record read for later factual questions.
  A successful Chat meal save returns directly to Chat. The confirmation's
  **View meal** action opens the same saved-meal editor used by Today; closing
  it returns to Chat. The target is the exact saved meal, not a name search.
  Deleted targets show **Meal no longer available** without an active button.
- If a freshly read progress overview cannot be worded safely by the local
  model, show a concise host-composed summary only after it passes the same
  factual checks. Retain the expandable records card and qualify partial or
  missing nutrition; do not invent intake or trends from missing records.
  Overview nutrition uses whole kcal and at most one decimal for protein,
  consistently across the answer and evidence, without changing saved values.
- Header exposes a labelled **New conversation** action with destructive confirmation.
- A latest-meal nutrition question identifies the recorded meal and date, shows
  its saved calorie/protein estimates with readable precision, and discloses
  partial or missing values. An empty bounded history or ambiguous meal time
  receives a clear explanation. It must not show a day total as that meal's value.
- An explicit saved-meal question for one day receives a concise list of the
  saved meal names for that date in the member's language. No meals is an
  explicit empty result; bounded omissions are labelled as a partial list.
  Do not replace this answer with nutrition totals or an explanation of model
  tools/policy. The expandable record card remains available.

**Approved first slice:** conversation opens as a full-screen, keyboard-safe destination from the central Metria action. The header communicates on-device privacy once; member messages, assistant prose, and facts from encrypted records have distinct visual roles. Thinking and listening are live, cancellable states. Weight saves and meal drafts appear as structured, reversible action cards. The timeline scrolls to new content, survives configuration recreation while the vault session remains active, and a restored completed tool call must never execute twice. Runtime and raw-response inspection live only under the debug-build diagnostics entry in Privacy.

**Conversation continuity:** typed conversation uses one native LiteRT `Conversation` while the vault session is unlocked. It is rebuilt from a token-bounded window of encrypted visible turns after restart or context rollover; local evidence and hidden reasoning are not replayed as model memory. The active timeline is encrypted in the vault, shared by all Chat entry points, capped at 50 visible messages, and retained for 24 hours after its latest visible activity. It survives biometric relock and process restart, but is excluded from recovery archives. Locking clears in-memory model context. Assistant prose may resolve conversational references but cannot become record evidence; every personal factual answer executes the matching approved local read tool. Model results carry request IDs and are displayed only when they match the active request and have not already been consumed.

## Privacy and settings

Production system-spelling disclosure (revised 2026-10-15): Privacy details and
Local assistant details explain that spelling help may check up to three possible
misspelled words individually with Android's selected spell checker, following that provider's privacy
settings. Keep the existing Done dismissal and make the detail text scrollable
at large font sizes. No new development terminology, primary action, settings
toggle or consent dialog is introduced by this approved integration. Provider
offline behavior is not asserted. Chat retains original text and existing
loading/Stop/selection/restoration states.

**Purpose:** manage private data, assistant storage, preferences, and recovery.

- Present privacy as understandable settings and status, not a developer diagnostics page.
- Group: Privacy promise, local assistant, units and goals, cycle context, encrypted recovery, saved private dishes, and About.
- The Local assistant settings group shows the selected response style and opens a focused,
  single-choice selector for Balanced, Coach, or Records assistant. Explain that
  the choice changes tone only: all three use the same private records, evidence
  checks, and safety rules. The selected option is identified by text and
  accessibility state, not color alone; closing the selector leaves it unchanged.
- Development diagnostics are absent from release UI and live behind one explicit development-only entry.
- The debug-only latest in-memory model response refreshes after leaving Chat.
  For text meal drafts it includes a fixed validation category. It is not
  persisted and clears on the next operation, vault lock, backgrounding, or
  restart; inspecting private response text requires authorization for that run.
- In debug builds, Developer diagnostics may offer an off-by-default, bounded
  metadata-only assistant trace with clear status and a delete action. It must
  not display or persist member records, prompts, or model prose in that trace.
- A separate debug-only, explicit-opt-in image development capture may retain
  one reduced test photo and bounded raw vision response for member-authorized
  local ADB inspection. Label this as raw private test content, distinguish it
  from the metadata-only trace, and provide Enable, Disable, and Clear controls.
  Enable persists locally until explicit Disable; Clear, lock/background, and
  a new process erase only the raw capture. Release builds never expose it or
  inherit the debug opt-in.
- Destructive actions have clear consequences and confirmation.

**Approved first slice:** use one calm, scrollable settings hub. Lead with the encrypted-on-device promise, then group personal settings (units, member-entered goals, cycle context), private features (local assistant status and saved dishes), encrypted archive actions, and About. Routine rows reveal their current state before opening a focused editor or detail. Archive creation and restoration remain separate actions. Locking Metria is visually separate at the end, and development diagnostics appear only as one explicit debug-build row.

## Daily nutrition goals

**Saved summaries S5 - 2026-10-08:** G07 leads to optional G08/G09 read-only
summaries after a successful commit. Daily references lead with calories/source;
only an accepted saved estimate has an approximate activity-range marker. Macros
show their saved member-entered values or Unset, without invented percentages or
recommended targets. Method details disclose on demand. Weight context shows the
saved intent, labelled starting reference, optional chosen target/range, and a
static endpoint marker where sufficient distinct values exist. Equal/absent bounds
use text. A starting reference outside a chosen range is explicitly described.
Use the member's kg/lb display choice; never call the starting reference a fresh
measurement. Each page has scrollable content and a pinned primary Next/Continue;
Back and Return to Goals are secondary. No swipe-only navigation, timed animation,
second save or restored success event. Graphs include complete textual descriptions.

**Staged replacement S3 - 2026-10-08:** final confirmation has one purpose: review
the exact phase to save. Show separate Current and Proposed summaries with direction,
optional weight references, daily references and calorie provenance. Show the actual
archive effect and oldest-removal warning when history is full. One dominant Start
new phase (or Save changes/Set up goal) action; Back and Cancel are secondary. Saving
shows labelled progress, blocks duplicates and dismissal; failure retains the same
reviewed proposal with Retry. Only successful callback completion opens a static
Saved acknowledgement, with one Continue to Goals action. Success is transient,
has no additional save action and is not restored after recreation or lock. Existing
atomic validation/rollback and history limit remain unchanged. Physical interaction,
font, keyboard, restoration and accessibility gates remain open until verified.

**Approved focused setup S2 - 2026-10-07:** G04-G06 use one task within an
extracted setup sheet: direction cards with selected text/icon, optional
starting/target/range summary rows and focused editors, then daily references
and used-calculation-input summaries before final review. First setup,
recalculation/edit and New phase modes are labelled distinctly. Manual entry
does not request unused profile fields. A requested maintenance estimate uses
the approved suitability/profile/equation/activity steps, with explicit choice
of a displayed endpoint; entering calories makes that reference manual. Final
review shows existing and proposed reference values/source, optional chosen
weight context and the existing replacement effect. Empty values remain unset.
Accepted starting weight is the reviewed calculation weight; target/range changes
do not recalculate. Only used inputs are collected, and estimate input changes
must not leave stale accepted source/value. Fields and suitability answers stay
transient; one stable draft ID survives recomposition/retry within the task.
Keep editing restores the same visible sheet. Keyboard-aware scrolling/footer,
enlarged text, state semantics and 48dp actions remain required physical gates.
No automatic target/speed/macros, new method, cloud call, persistent draft or
vault/archive extension is authorized by this presentation slice.

The final review lists Calories, Protein, Carbohydrates and Fat individually,
showing `Not set` for each missing value and its source for each supplied value.
Set/Partly set/Not set plus an icon communicates only how many references were
supplied, never whether they are safe or adequate. Optional weight references say
Not set without a dangling unit. For Lose/Gain, show whether maintenance was
selected and whether a phase adjustment was actually applied; a selected
maintenance endpoint alone must read `Phase adjustment not chosen`. Explain that
the member may choose maintenance, calculate and Use phase calories/macros, or
enter references manually. All references remain optional and the primary action
may continue with unset values.

**Approved compact hub/detail S1 - 2026-10-07:** follow the member-approved
original [goal journey](prototypes/goal-journey.html), tracked by G10/G11 and
the unknown-period fallback for G02. One compact current-phase summary precedes
daily references and previous-phase rows. Use actual latest weight or a labelled
starting reference; show chosen weight context only when saved. Edit weight goal
is the dominant action; New phase is distinct. Reference/method inspection,
recalculation and data controls use progressive disclosure rather than a feed
of full editors and archived cards. Empty goals/history and unset macros stay
useful. The focused selected-phase detail uses its own saved target/range,
starting reference and nutrition source, never today's latest weight or targets.
Show a known local start/end period only when explicitly saved. Unknown historical
ends say Period unavailable and withhold period totals. For known periods, show a
read-only recap from retained weight/meal records: actual weight readings, known
full-coverage recorded nutrition with per-metric meal counts, unique logging days,
and disclosed date/time exclusions. Current phase is labelled In progress and
uses an as-of preview. No inferred boundaries, averages, adherence, streaks,
forecasts, or synthetic weight points. Deletion sits inside data controls with
the existing confirmation.
Navigation remembers only the selected ID/page within the unlocked session,
resolves the current snapshot on every render and safely handles removed phases.
Back, saving/error, empty/restoration, scrolling/system insets, enlarged text,
48dp actions and TalkBack remain acceptance requirements. This approved recap
uses only explicitly saved period boundaries and retained local records.

**Compact detail override:** Use an inline intent/start/end/current header without
a separate phase-date card. Show starting and target values compactly, omitting
an unset range. Daily calories and macros each appear once with one truthful
collapsed source summary; exact mixed per-field provenance stays in Method and
inputs. Recap defaults to actual weights and meal/day counts. Empty records,
unknown periods and meals without nutrition each use one concise state. Partial
or missing coverage details, per-metric counts and timezone exclusions remain
available through an accessible 48dp disclosure. Keep only the current Edit
weight goal or previous Back to Goals action in the bottom bar.

Purpose: inspect or edit the member's optional daily nutrition references.
Current goals show intent, member-entered weight target/range when present, daily
references and their calorie source. Unset values read No target. Estimate inputs
and approximation are available on demand; macro values remain member-entered.
The primary action is optional setup or Edit weight goal; quiet actions open
manual editing, New phase, Previous phases and disclosed profile/history deletion.
Existing members are not forced through setup. Without goal data, daily references
remain readable unchanged. Editing the current phase preserves its ID/start date;
New phase explicitly replaces references and adds the old phase to bounded history.

**Goal presentation refinement — 2026-10-06:** the member supplied further goal
references and reported dirty-sheet dismissal → Keep editing freezing the UI.
The goal view now leads with a tonal current-goal card: direction, local start
date, latest recorded weight labelled separately from reviewed starting reference,
member-entered target and chosen range when present. No range is invented. A
separate daily-reference card leads with calories/source and compact macro rows;
method/inputs remain on demand. Previous phases and data management are secondary.
Existing goals use Edit weight goal as the dominant action; calorie recalculation
is a separate quiet route. Today adds an explicit Your goal and daily references
entry. Styling follows Metria's theme, not the reference app's palette/assets.

Focused weight editing changes direction, optional target/range only; current
phase ID/start time, reviewed baseline, calorie provenance and daily references
remain intact. Changing a starting reference still uses setup/recalculation.
Setup uses visible step progress, icon/label choices and an estimate hero. The
reference app's digestion/exercise breakdown is not adopted; Metria keeps its
approved whole-day method. Dismissal of changed editors must be vetoed before
the sheet becomes Hidden. Keep editing restores the visible editor and inputs;
explicit Leave removes it. Saving also blocks dismissal. The shared guard uses
current flags/callbacks, not captured initial values. Physical reproduction and
visual acceptance of this fix/refinement remain open.

Setup has four focused steps: direction, optional profile/equation/suitability,
whole-day activity and review. Declining an equation/suitability or unsupported
inputs leaves manual targets available. Units are explicit; changing units clears
the numeric entry to prevent reinterpretation. Eligibility answers are transient.
No midpoint is automatically accepted. Choosing a range endpoint explicitly marks
calories as estimated; typing calories marks them as manual. No deficit, surplus,
protein recommendation or weight band is invented. Review shows the old reference
and replacement effect, then requires Confirm goal and references. Additional
macros and member-entered kg weight target/range are disclosed on demand.

Manual editing leads with calories/protein and additional macros on demand.
Targets may be partial or absent. Removing all existing targets is confirmed;
delete profile/all phases retains references as manual. Historical deletion is
confirmed and does not change the current goal. Changed drafts confirm leaving;
Skip writes nothing. Saving disables duplicates, shows progress and retains a
recoverable error. Scroll bodies are bounded and footers keyboard-aware. Lock
clears presentation; orientation retains the unlocked draft; process restart does
not replay a calculation or write. Versioned encrypted archive replacement includes
goal/profile disclosure. Physical rotation, keyboard, scaling and accessibility
acceptance remain open.

## First run and model setup

**Purpose:** establish trust and complete required offline-assistant setup.

- Explain local-only behavior once in clear language.
- Model download shows size, network requirement, progress, cancellation, and what data is—and is not—sent.
- Completion moves directly into biometric vault setup/unlock.
- Returning after interruption resumes or clearly restarts the download; it must not strand the member in an indefinite loader.

**Optional goal offer slice — 2026-10-06:** after successful biometric creation
of a new vault, offer an introduction with Set up my goal as the sole dominant
action and Continue tracking as the skip choice. Targets are optional, answers
stay on this phone, and saving requires the later explicit confirmation. Setup
opens the existing four-step flow; skip/dismiss returns to Today and writes
nothing. Existing vault unlocks do not trigger this offer, even when targets or
profile are absent. About goal setup in Goals lets members inspect it voluntarily.

The offer event is consumed once in the current new-vault session. A pending voice
handoff or meal-entry sheet takes priority and suppresses the introduction; Goals
remains available afterward. Lock,
background and activity/process recreation clear it rather than persisting a
health-related flag or replaying onboarding. Interrupted members can return from
Today → Your goal and daily references. No profile answers precede vault creation;
showing/dismissing the offer causes no model/network call, target calculation,
schema change or write. Physical first-run routing needs a separate empty test
vault/device; never clear the member's phone to exercise it.
