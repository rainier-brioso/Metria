# Metria primary UX flows

Approved shared read typo tolerance (2026-10-13) preserves the original Chat
message while a transient candidate may select an existing validated read.
Ambiguous requests keep normal handling. Loading, Stop, error feedback,
selection and retention use the existing flow; no new member control is added.
Research, scope and acceptance: `../assistant-routing-recognition.md`.

Completed rejected Chat turns retain a concise failure explanation as an
assistant message. It appears once after any local evidence, remains selectable
and survives reopening within ordinary conversation retention. A subsequent
question starts fresh processing; Stop, lock and newer requests cannot append a
late failure from an obsolete turn. Pending failures remain transient until a
current turn is terminal.

These flows define member intent and state transitions. They do not prescribe a specific composable hierarchy.

**Approved phase calories — 2026-10-08:** see
[phase calorie contract](phase-energy-contract-proposal.md). Lose/Gain daily-reference
review may open a focused maintenance → adjustment → final budget preview after
explicit maintenance selection and fresh used-input/suitability review. Use stages
the approved 10% Lose or 5% strength-Gain proposal; final phase confirmation alone
writes. Unsupported cases keep manual/skip paths. Changing direction, maintenance,
used training or calories invalidates affected suggestions and derived macros;
manual overrides survive. Historical views retain their exact saved source.
Direction edits with phase-energy provenance route through full reference review.
No forecast, artificial loader, extra exercise calories or automatic weight-triggered
recalculation. Physical acceptance remains open.

For maintenance, phase-calorie and macro proposals, the general clinical/eating-
disorder suitability confirmation is separate from pregnancy/breastfeeding
applicability. Ask applicability directly; never infer it from an equation
coefficient. An applicable answer routes to care-team-entered daily references and
clears generated values and source snapshots while retaining explicit member
values. Back cannot restore cleared proposals. Answers stay transient; after an
explicit not-applicable answer, the member may return to general options and make
fresh suitability confirmations and proposals.
Manual tracking and saving member-entered references never require an applicability
answer. Both answers are required only to calculate or use automatic proposals.

## Proposed optional online assistance

The member selected this design direction on 2026-10-02; it is not implemented
or physically accepted. See `../optional-cloud-assistant-spec.md` for the
complete consent, data bounds, state, and revocation contract. A separate online
composer leads to an exact question/summary preview and explicit **Send online**.
It never uploads local conversation automatically, has no write tools, and
never becomes a fallback for local model failure. The local Ask Metria flow
below remains the active contract until the new privacy and acceptance gates pass.

## 1. Add weight

**Approved macro proposal slice — 2026-10-08:** Goals setup review exposes optional
Training and macro suggestions. A focused screen reviews age, kg/lb weight, training
context, suitability and the current calorie budget. Calculate suggestions shows
exact proposed macros and current values; Use suggestions updates only the transient
draft. Edit/Skip/Back remain available. Whole-day activity already includes exercise;
no second calorie addition or phase deficit/surplus occurs. Unsupported inputs offer
manual references without zeros or silent clamping. Used-input changes require
re-review; macro overrides retain only remaining valid per-field sources. Only final
phase confirmation writes. Existing cancellation/Keep editing, save retry, duplicate
prevention and lock/restoration policies apply. Saved and historical detail show
their own exact references and disclose macro source/method/inputs on demand.
No assistant/cloud sharing or automatic recalculation from weight entries is added.

**Saved summaries S5 - 2026-10-08:** after the successful Saved acknowledgement,
the member can inspect read-only daily references, then chosen weight context.
Visible Next/Back/Continue controls work without swiping; Return to Goals can
finish early. External dismissal also returns to Goals. These views use the exact
saved proposal and selected display unit, never recompute or save again. A selected
maintenance calorie value is positioned within its saved activity-scenario range,
labelled approximate; manual calories have no invented range. Missing macros stay
Unset. Weight graphics show only saved starting/target/range values; no synthetic
reading, progress percentage, forecast or achievement. Method inputs disclose on
request. Completion and summary navigation remain transient and do not replay on
lock/recreation. Historical-period recaps remain deferred.

**Staged replacement S3 - 2026-10-08:** New phase remains a transient setup task.
Final confirmation compares the saved current phase with the reviewed proposal,
including optional weight context, daily references and calorie source. The current
phase remains active until the existing atomic save succeeds. At the history limit,
confirmation states that the oldest previous phase will be removed. Cancel writes
nothing. Save freezes the reviewed phase, blocks repeated taps and dismissal, and
keeps that exact proposal for a failed-save retry. Success is terminal for that
task: show a static Saved acknowledgement with Continue to Goals; never save again
from the acknowledgement or replay it after recreation/lock. No closure timestamp
or historical-period inference is added.

**Approved focused setup S2 - 2026-10-07:** the approved journey separates
direction, optional chosen weight context, daily-reference/used-input review and
final confirmation. Summary rows open focused editors within the setup task;
the final review is not a form feed. A maintenance estimate remains optional and
uses only the existing reviewed profile, equation, suitability and whole-day
activity. Input review leads back to that focused calculation workflow. Manual
references stay available with absent values, and previously estimated calories
are not silently copied as manual into a new phase. Changes to estimate inputs
invalidate a selected estimate until explicitly selected again; entered macro
changes retain calorie provenance. Only final confirmation writes the complete
validated phase through the existing atomic callback. Every pre-save navigation,
cancel, invalid-input retry and Keep editing preserves the task or saved state
according to the existing transient-draft policy. S3 transition/success polish
and S4 historical-period calculations remain separate slices.

Final review names each daily value, including every `Not set` value, and labels
daily references as Set, Partly set or Not set by supplied-value count only. These
labels do not imply safety or adequacy. Optional weight targets/ranges can remain
unset. The review distinguishes a selected maintenance endpoint from a phase
calorie proposal that has actually been calculated and used, and explains the
remaining optional action. Missing references remain optional; continuing with
unset values does not add a new requirement.

**Approved goal hub slice S1 - 2026-10-07:** Today, Progress and Privacy keep
their existing Goals entry. The member approved the original goal-journey
mockup hierarchy. Goals shows a compact current summary, daily-reference
disclosure and previous-phase rows. Edit weight goal remains the dominant
action and opens the existing focused editor. New phase opens the existing
staged setup; recalculation, manual references and data management remain
separate disclosed actions. Each previous row selects one exact phase ID and
opens its own detail, using its saved references and reviewed starting weight.
Back returns to the hub; Done returns to the originating context. A deleted
selection falls back to the hub. Known historical phases show their recorded
start/end period; unknown ends remain explicitly unavailable with no inferred
recap. Selected details may show a read-only recap from retained weights and
meals, with exact interval, coverage and boundary limitations. Current details
show an as-of preview and never close the active period. Deletion stays confirmed
and cannot change current references accidentally. Recaps are not persisted.
Physical acceptance remains required.

**Compact phase-detail refinement - 2026-10-10:** Keep intent and period on one
header, followed by compact saved weight context, daily values, and the recorded
recap. Omit unset ranges; display an unset target once. Collapsed recap shows
actual weights and meal/day counts; use one empty, unavailable, or no-nutrition
state where applicable. Show detailed coverage and per-field provenance only
after disclosure. The header handles return navigation; keep one bottom action.

**Daily goals slice — 2026-10-06:** Today, Progress and Privacy open the same
unlocked Goals view. Setup is optional: direction → profile/equation/suitability
or manual fallback → whole-day activity → review and explicit confirmation.
Maintenance energy uses the approved approximate method; no weight-change or
protein reference is inferred. The preview shows old and new references; an
estimate needs an explicit choice. Edit preserves the current phase; New phase
archives it, retaining up to 12 previous phases. New weight entries never trigger
recalculation. Manual calorie edits remove derived provenance. Members can
inspect/delete individual historical phases or delete profile/all phases while
keeping daily references as manual. Blank values remain optional. Target removal
and changed-draft leaving require confirmation. Failed writes stay in the editor.

**Goal refinement — 2026-10-06:** Today also exposes Your goal and daily
references directly. The goal summary separates latest recorded weight from
reviewed starting reference, and leads with a current-goal card plus daily
reference card. Edit weight goal opens a focused direction/target/range editor
without changing calorie source or values. Recalculate remains separate.
Swiping a changed setup, daily-target or weight editor requests confirmation
before hiding. Keep editing keeps/restores the same visible editor and values;
Leave without saving removes the editor. Dirty/saving guards must read current
state. A hidden modal must never remain as an invisible touch-blocking layer.

**Optional first-run goal offer — 2026-10-06:** successful new-vault creation
queues one transient introduction for the unlocked session. Set up my goal opens
the existing four-step setup; Continue tracking/swipe/Back skips without a write.
Pending voice handoffs or meal-entry sheets take priority and suppress the offer.
Returning members are not interrupted. Goals → About goal setup opens the same
introduction voluntarily. Lock/background/recreation drops the pending event and
unsaved answers; Today remains the later entry. Setup confirmation is unchanged.

**Intent:** record a weight in seconds.

Today now shows the chosen goal with latest weight instead of the compact weight
KPI. Update weight opens the existing weight entry sheet. Goal and daily references
opens the existing goal hub; Set up a goal reaches that hub when no phase exists.
No goal/range is generated for members who simply track, and selected weight units
apply to the summary. New readings never recalculate daily references.

1. Member taps the central Metria action, a visible Today shortcut, or speaks a weight request.
2. Metria opens a compact weight editor with value first, then date/time and optional note.
3. Plausibility and unit validation happen inline.
4. A valid manual entry may be saved directly by the member. An assistant-created draft must remain visibly reviewable according to the current safety contract.
5. Success returns to the originating context and shows the saved value with a contextual comparison and Undo.

**States:** empty history, invalid number, unknown unit, future date, keyboard visible, assistant unavailable, biometric interruption, save, undo, edit, delete.

## 2. Analyse and confirm a meal

**Compact Today summaries - 2026-10-07:** Goal details opens the existing hub
from a compact Today card; Update weight keeps its existing entry flow. Weight
graphics show only confirmed range or starting-reference/target context.
Nutrition details toggles from the compact card header, restoring the existing
coverage and daily-reference actions. These graphics are presentation only;
opening, collapsing and recording a new weight do not change goal references.

**Recorded nutrition summary — 2026-10-07:** Today presents saved meal calories
and macros with a coverage label. Expand Nutrition details to inspect counts of
complete, partial and missing estimates and optionally open daily references.
Collapsing or leaving writes nothing; missing data is never treated as zero.

**Meal entry expansion — 2026-10-06:** a Today meal section opens a compact
choice of photo, Speak, Type or Manual entry. The explicit section type is
preselected in review and remains editable before Save.
Speak opens dedicated voice entry; recording starts only with a mic action.
If manual Stop overlaps automatic completion, the entry shows Finishing
recording until the pending clip arrives. Another recording cannot start during
that handoff; Type instead, Back and background cancellation still discard it.
Local audio understanding supplies recognized words. Today meal entry sends a
successful transcript directly to draft generation; proposed audio actions are
never dispatched. Generic Speak retains explicit editing and submission.
No new barcode or cloud route is enabled.
Available catalog nutrients expand on demand, with their own coverage and
source units; unavailable nutrient rows are not manufactured.

**Contextual entry refinement — 2026-10-07:** Type and Speak from a Today meal
section stay in a dedicated meal-entry destination. A visible section label
retains Lunch (or the chosen section). Member-approved refinement (2026-10-10):
successful voice recognition proceeds directly to local meal generation without
an intermediate transcript confirmation. Recognition failure offers Retry/Type;
generation failure or Stop retains editable words for an explicit retry.
Typed entry retains Create meal draft. The validated description requests the existing local meal
proposal directly, without opening Chat or asking the assistant to infer the
entry intent again. Processing shows a progress indicator, one short status and
Cancel. Failure retains the words with Retry, Type instead and manual entry.
Successful output opens the existing explicit review; Save returns to Today
and the chosen card, with type/date visible and editable in review. No chat
receipt is created for this Today-origin flow. Generic Chat and Speak retain
their existing conversational behavior. Cancellation, lock/background and
disposal invalidate pending work and delete temporary audio; restart never
replays recording or generation.
Photo from the same card presents its own section-specific capture destination
with camera/library choices and Meal photo / Nutrition label modes. Main tabs
and Chat are absent from this task. Both meal and label review carry the section
and entry date; saving returns to Today. Back/Stop invalidates label-reading
results as well as meal analysis, so cancelled work cannot restore an old draft.

**Intent:** turn a photo into a trustworthy editable meal with minimal typing.

1. Member chooses meal analysis and takes or selects a photo.
2. After capture or library selection, a private reduced preview opens. The
   member can replace the photo, read framing tips, choose Meal photo or Nutrition
   label, and optionally add up to 600 characters of meal details. Analysis starts
   only with Create meal draft (or Read label); preparation alone never sends a
   model request. Label mode has no description. Details are temporary, local,
   untrusted context, not instructions or a saved note. Changing mode or image,
   cancellation, lock or leaving capture clears them. Hidden ingredients still
   require possible-inside evidence, low confidence and member review.
3. Successful local meal-photo analysis automatically opens required meal review
   once for the matching active request, without an intermediate result-screen
   confirmation (member-approved 2026-10-11). Valid partial drafts also open review
   with missing-food disclosure. Failure/rejection stays in entry with manual or
   new-photo options. Stop/leaving invalidates automatic navigation; returning from
   review cannot replay it. Label OCR keeps its separate explicit review action.
   Separate dishes appear as separate cards; visible components appear under their dish.
   Unambiguous model labels may be normalized before strict validation. If one food is malformed but other foods validate independently, show only the valid foods as a partial draft and say that suggestions were left out. The member checks the photo and can add missing foods. An invalid root or no valid foods shows no proposed foods; a direct **Enter meal manually** action opens the same review with no AI foods, and the member adds the first dish before saving.
4. Review starts collapsed: dish name, principal components, estimated portion, confidence, and available nutrition.
5. Tapping a dish opens a focused dish editor. The member can rename the dish once, edit components, mark possible hidden ingredients, regroup foods, and choose offline nutrition matches.
6. For a selected component, the member may describe a name correction in ordinary language. The on-device text model can suggest one replacement food name; Metria shows the old and new names and requires **Apply name** or **Discard**. Applying changes only the selected label, clears its catalog match, and leaves portion, ingredients, preparation, and nutrition for the member to review. The suggestion is temporary and never writes to the vault by itself.
7. Metria calculates nutrition only from confirmed quantities and accepted matches. A partial calculation is valid and clearly distinguished from a complete one.
8. The member confirms the meal. Raw source imagery is discarded; an optional reduced encrypted reference thumbnail follows the existing vault policy.
   Corrections between a valid local draft and the successfully saved review
   teach bounded private meal suggestions automatically. There is no learning
   toggle or extra prompt; cancelling or failing to save teaches nothing.
9. Success returns to Today and opens the saved meal if the member chooses “Review entry”.

**Leave without saving** remains visible beside the persistent Save action.
If the member has changed the review draft, leaving through that action or
Back asks for confirmation; **Keep reviewing** retains the draft. A confirmed
leave returns to the prior context without saving a meal or correction learning.

The original unsaved proposal remains available after leaving review. Its
**Discard draft** action asks for confirmation, then removes only that proposal;
saved meals and the visible conversation remain unchanged. Choosing **Keep
draft** returns to the proposal.

**States:** camera permission, cancellation, model warming, analysis, cooldown, malformed result with manual entry, no food, partial proposal with omitted-suggestion disclosure, catalog match ambiguity, text-model unavailable/busy, name suggestion loading/cancel/failure, stale suggestion, explicit apply/discard, keyboard visible, background/resume, save, discard.

Similar-meal suggestions use **Use meal name** and say that only the meal title changes; they do not rename a food or dish component.

## 3. Correct and teach a private dish

**Intent:** make repeated personal meals easier without claiming that the model learned globally.

1. During review, the member renames a dish or corrects its ingredients.
2. “Remember as my dish” is on by default but plainly reversible.
3. Metria shows exactly what will be remembered: private dish name, aliases, confirmed component structure, and optional reduced reference thumbnail.
4. Saving the meal updates or creates the encrypted personal-dish record.
   This explicit reusable-dish choice is separate from ordinary automatic
   learning from confirmed meal corrections.
5. A later analysis may surface “Looks like your San Jacobo” as a suggestion, never as an unquestionable identification.
6. The member can accept, edit, or ignore it. Corrections update the private dish only after confirmation.

**States:** duplicate name, no usable components, image not retained, suggestion accepted, suggestion rejected, private dish removed.

## 7. Edit a saved meal

**Dismissal refinement - 2026-10-07:** dirty-sheet swipe/outside dismissal asks
before hiding the editor. Keep editing keeps the draft visible; Leave closes its
modal layer. Back first closes catalog search or the ingredient editor. Search
shows the bundled catalog scope and a clear no-match fallback to unknown or
member-entered nutrition. An unavailable food is never treated as zero calories.

**Ask about a saved meal — 2026-10-07:** a clean saved-meal editor may hand its
exact ID to local Chat. Dirty edits stay in the editor until saved/cancelled.
The composer retains existing text and shows removable selected context; no
inference starts until Send. Supported selected-meal questions read the current
saved record and return its recorded foods/nutrition with missing/partial labels.
Explicit latest-meal/date/progress questions keep their original scope. The
selection is consumed for one turn and cleared on lock/background/new conversation
or deletion. No meal write occurs through this read route.

The approved companion slice shows summary metrics and compact ingredient rows.
Tap one ingredient for a focused editor; Apply stages it in the meal, Cancel
restores that ingredient and its prior match. Save meal changes remains the one
vault write. Changed meal drafts ask before leaving. Date/type and manual
nutrition fields expand on demand. Whole-portion adjustment is available for
catalog meals and new reviewed drafts; it scales all valid gram values atomically
with integer rounding and bounds, never raw/cooked yield or manual macro values.
New-draft scaling clears hidden-ingredient inclusion so it must be checked again.

**Intent:** make a focused, reviewable correction to an existing meal.

1. Opening a saved meal starts an editable draft; direct food-name editing remains available.
2. From one food row, the member may ask Metria for a replacement food name. The local text model proposes one label only. The member sees the old → new preview and chooses **Apply name** or **Discard**.
3. Applying or manually changing a food name clears its existing catalog match. Portions, preparation, and member-entered nutrition remain unchanged; catalog nutrition updates only from still selected matches and the member's review. If catalog coverage would fall, Metria asks before saving a partial or absent estimate.
4. A food-name suggestion writes nothing to the vault. One final **Save meal changes** persists the complete validated draft. Catalog-backed entries cannot be saved if the offline catalog or an unchanged saved match cannot be resolved. Save failure keeps the editor and its pending values visible.
5. Removing an optional reference photo changes only the draft. Cancel/back leaves the saved photo and meal unchanged.
6. Draft text and selections survive configuration recreation. An active suggestion is cancelled and cleared on editor exit or recreation; the member can retry without a stale response appearing for another food or meal.

**States:** direct edit, suggestion loading, suggestion unavailable/failure, stale suggestion, apply/discard, catalog match cleared or reselected, manual nutrition preserved, pending photo removal, keyboard visible, rotation, cancel, save failure, save/reopen.

## 4. Scan a nutrition label

**Member decision — 2026-10-07:** the photo mode is named Nutrition label, alongside
Meal photo. Barcode scanning is deferred; this route reads printed nutrition
values locally and retains explicit draft review rather than product lookup.

**Intent:** capture label values without manually transcribing every number.

1. Member chooses Scan label from the central action.
2. Capture screen guides framing and offers retake or existing photo.
3. OCR runs locally and opens a compact label review.
4. Serving size and calories are first; protein, carbohydrates, and fat follow in a simple grid.
5. Missing or ambiguous fields are highlighted individually. Recognized values remain editable.
6. Member confirms; the saved entry uses the “Verified label” source only after that review.

**States:** unreadable image, missing serving, decimal/locale ambiguity, calories derived from macros, cancel, save.

## 5. Understand progress

**Current goal context — 2026-10-07:** the seven-day view includes a compact
current-goal card below the chart. View goal and daily references opens the same
goal hub used by Today and Privacy; without goals or references, Explore goal
setup offers the voluntary journey. Dismissal returns to the selected metric/day
without writing. Current references are explicitly distinguished from past-day
records. No historical goal comparison, nutrition score or automatic recalculation
is introduced. All history retains its separate date-grouped view.

**Intent:** see useful patterns without moral judgment or false precision.

1. Today shows a compact daily summary and the latest entries.
2. Progress defaults to the last seven days including today.
3. The selected metric is visually dominant; targets are context, not the headline.
4. Tapping a day reveals its meals and weights without navigating away unnecessarily.
5. “All history” opens every saved weight and meal, grouped newest date first; tapping a row opens its existing editor.
6. Back returns to the same seven-day Progress state without changing records.
7. Complete and partial nutrition are visually distinct and explained on demand.
8. Assistant insights use only approved local tools and the selected period.

**States:** no data, partial estimates only, goal absent, one-day history, selected day, all history, empty history tab, changed range, cycle-aware context enabled/disabled.

## 6. Ask Metria

**Approved companion bundle — 2026-10-12:** implement the complete local set
before one installation, then test each feature. Goal questions compare fresh
today estimates with current confirmed references, disclose partial/missing
coverage and are not advice. No reference or usable estimate receives an explicit
unavailable answer. Saved-dish recall shows bounded named matches; ambiguity is
disclosed rather than choosing by confirmation frequency. “I had my usual…” may
prepare only an unsaved saved-dish draft. Multiple candidates require choice;
selection is re-resolved in the current unlocked vault before review. Saved
amounts are prefilled, unknown amounts stay blank, and final reviewed Save alone
records the meal. No previous image/audit is inherited.

An earned short date/metric follow-up performs a new read; yesterday never uses
today's goals. Missing or incompatible scope keeps safe normal handling.
Explicit navigation uses an allowlisted existing destination, closes Chat and
preserves its encrypted timeline for reopening. Retain the navigation question
and opening reply before leaving Chat; if that save fails, remain in Chat with
a recoverable error. Request identity, Stop and lock
prevent stale navigation/draft actions. Success/empty/ambiguous/partial/error/
cancelled/restored states retain existing review and conversation contracts.
Detailed gates are in `../assistant-companion-bundle.md`.

**Approved fast today overview:** an explicit “How am I doing today?” request
may return a concise recorded-meal/nutrition recap immediately from a fresh
local snapshot. Show available estimates, current confirmed daily goals and
missing/partial coverage; no estimate or goal is guessed. The recap is neutral
and does not add advice or a clinical interpretation. Existing Chat empty,
error, cancellation, retention and selection behavior applies. Weekly/historical
or advice questions retain their existing flow. Detailed scope and open gates
are in `../minilm-today-overview.md`.

**Approved message selection — 2026-10-12:** long-press completed message text
to select a word or range with native handles and the floating Copy toolbar.
The member replaced the initial visible-options design with this interaction.
Explicit Copy transfers only the selected plain text to Android's clipboard,
marked sensitive to hide its preview. It does not send a turn, read clipboard
content, copy hidden model/tool state or save new records. Thinking/streaming
messages do not expose Copy. Keep keyboard, scrolling, saved-meal actions and
record-detail disclosure usable; copying has system or generic text-free
confirmation and a recoverable generic error if unavailable.

**Approved weight shortcuts — 2026-10-12:** latest saved weight/date and recent
weigh-in questions may use a debug-only local intent route and fresh existing
bounded weight read. Answers disclose the window, kilograms, recorded dates and
omissions; empty results never imply all-history absence. The same Chat Send,
Stop, lock, evidence and encrypted retention states apply. No screen/navigation
change or automatic write is introduced; acceptance is tracked in
`../minilm-chat-integration.md`.

**Debug MiniLM meal lookup — 2026-10-11:** the member approved direct Chat
integration for explicit English requests to list today's recorded meals. Send
keeps its existing pending/cancellation behavior. A local classification plus
independent host eligibility check may select the existing bounded meal read
and validated deterministic reply without Gemma generation. Uncertainty, other
intents and router failure retain the normal assistant route. Record evidence,
empty results, encrypted retention and lock/Stop rules stay the same. Physical
acceptance is pending in `../minilm-chat-integration.md`.

**Intent:** use local language or voice to navigate, record, or understand data.

**Native text meal-draft slice, 2026-10-02:** an ordinary description of food
eaten may cause the agent to request an unsaved meal draft without requiring
"log" or "save". The existing proposal card leads to Review or Discard;
the meal remains unsaved until final member confirmation. Questions about
already recorded meals remain fresh bounded reads. The host validates the
draft and rejects late results after Stop/lock; completed draft context is
rebuilt from visible chat rather than retained proposal JSON. Physical intent,
review, cancellation, and restoration acceptance remain pending.

1. Member opens the central Metria action from any main screen.
2. Compact choices appear: Speak, Chat, Analyse meal, Scan label, Manual entry.
3. Listening is unmistakable and automatically stops after speech or timeout.
4. Tool-backed requests show grounded results or a validated action outcome.
5. General prose is allowed when safe, but it must not pretend to have read records without a matching approved tool result.
6. The active conversation is encrypted inside the member vault and survives screen close, app switching, biometric relock, and process restart without replaying the last request.
7. Typed chat uses one native LiteRT conversation while unlocked. After restart or token rollover, Metria rebuilds it from a bounded window of visible encrypted turns; local evidence and hidden reasoning are excluded.
8. A native read tool call is executed manually by the host and returned to that same conversation. A meal-draft intent instead starts a separate structured task using only the current description, without chat style/history. Draft preparation displays a spinner, a concise status, and Stop response; nothing saves until review. The unlocked shell owns the session shared by every Chat entry point. Each model operation has a monotonically increasing request ID; only the matching active result may be consumed, and it may be consumed once.
9. The encrypted timeline keeps at most 50 visible messages and expires after 24 hours without a new visible message. Expiry starts a clean conversation with a short explanation; **New conversation** deletes it immediately after confirmation.
10. Locking, cancellation after a runtime failure, and **New conversation** destroy model KV state. Recovery archives exclude conversation text. App-data deletion or uninstall removes it with the vault.
11. The member can choose Balanced, Coach, or Records assistant in Local assistant
    settings. This changes the phrasing of subsequent answers only. The current
    visible timeline stays intact; the next model request rebuilds context with
    the selected style and still fetches fresh evidence for personal facts.
12. An explicit question about meals, weight, or recorded-meal nutrition on one
    older day within the supported 90-day window performs a fresh read for
    exactly that day, whether the member uses a relative day or an unambiguous
    named/ISO calendar date. Ambiguous numeric dates and questions naming
    multiple days get a clarification without a record read. Nutrition answers
    distinguish complete, partial, and
    missing estimates and never imply that recorded meals equal total intake or
    that today's goal applied historically. Dates outside the window and
    unsupported multi-day historical totals receive a clear limit explanation,
    without an all-history claim or a reused recent summary.
    The collapsed record card may expand to a concise member-language coverage
    explanation, not a raw tool dump; numbers are shown as estimates without
    floating-point precision artifacts.
13. Record-backed answers refer to the member's records, answer directly, and
    avoid generic trailing questions. A Coach-style next step must be relevant
    and optional.
14. If a turn fails verification, its generic recoverable error stays visible
    until the member starts another request, locks, or starts a new conversation.
    Rotation retains that error without placing conversation text in saved UI state.
15. After saving a meal opened from Chat, the active timeline contains a short
    confirmation with the final reviewed name and foods. It persists atomically
    with the meal and survives chat reopening, relock, and restart within the
    usual conversation retention window. Failed saves and discarded drafts
    never show a saved confirmation; an expired conversation is not revived.
    Successful save returns directly to Chat with the confirmation visible.
    **View meal** opens the existing saved-meal editor over Chat; dismissing it
    returns to the conversation. Leaving review without saving also returns
    to Chat with the original draft retained. A deleted meal has no active link.

**Cancellation:** tapping Stop immediately invalidates the active request so late model output cannot be added. While LiteRT shuts down in the background, show an accessible “Stopping…” status and keep Send disabled. Once cancellation completes, show a calm “Response stopped” confirmation and make the composer actionable. Starting a new conversation or locking during shutdown invalidates that confirmation.

The stop confirmation is a transient status and clears when another question is
submitted. That question is a new request: personal facts still require a fresh
local read. A latest-meal nutrition question reads one recorded meal rather than
a daily total, with explicit missing, partial, empty, and ambiguous states.

**States:** model warming, listening, no speech, background noise, unsupported request, tool selection, tool result, cancellation and stop confirmation, keyboard visible, rotation, navigation away and return, biometric relock, process restart, expired conversation, new conversation, style change while idle or after a completed turn.

## 7. Edit or delete an entry

**Intent:** correct personal data without ambiguous controls.

1. Member taps the whole row/card.
2. A focused detail editor opens with the current values and optional image.
3. The photo delete control is visually attached to the photo. Entry deletion is a separate destructive action at the bottom.
4. Removing a photo keeps the editor open and updates the pending draft.
5. Saving applies the complete edited draft atomically. Cancellation leaves the stored entry unchanged.

**States:** keyboard visible, photo removed, edited after photo removal, delete confirmation, save failure, cancel.
