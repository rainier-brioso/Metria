# Metria primary UX flows

These flows define member intent and state transitions. They do not prescribe a specific composable hierarchy.

## Proposed optional online assistance

The member selected this design direction on 2026-10-02; it is not implemented
or physically accepted. See `../optional-cloud-assistant-spec.md` for the
complete consent, data bounds, state, and revocation contract. A separate online
composer leads to an exact question/summary preview and explicit **Send online**.
It never uploads local conversation automatically, has no write tools, and
never becomes a fallback for local model failure. The local Ask Metria flow
below remains the active contract until the new privacy and acceptance gates pass.

## 1. Add weight

**Intent:** record a weight in seconds.

1. Member taps the central Metria action, a visible Today shortcut, or speaks a weight request.
2. Metria opens a compact weight editor with value first, then date/time and optional note.
3. Plausibility and unit validation happen inline.
4. A valid manual entry may be saved directly by the member. An assistant-created draft must remain visibly reviewable according to the current safety contract.
5. Success returns to the originating context and shows the saved value with a contextual comparison and Undo.

**States:** empty history, invalid number, unknown unit, future date, keyboard visible, assistant unavailable, biometric interruption, save, undo, edit, delete.

## 2. Analyse and confirm a meal

**Intent:** turn a photo into a trustworthy editable meal with minimal typing.

1. Member chooses meal analysis and takes or selects a photo.
2. The capture screen owns retake, choose-existing-photo, cancel, and analysis progress.
3. Local analysis returns a dish-oriented draft. Separate dishes appear as separate cards; visible components appear under their dish.
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

**Intent:** make a focused, reviewable correction to an existing meal.

1. Opening a saved meal starts an editable draft; direct food-name editing remains available.
2. From one food row, the member may ask Metria for a replacement food name. The local text model proposes one label only. The member sees the old → new preview and chooses **Apply name** or **Discard**.
3. Applying or manually changing a food name clears its existing catalog match. Portions, preparation, and member-entered nutrition remain unchanged; catalog nutrition updates only from still selected matches and the member's review. If catalog coverage would fall, Metria asks before saving a partial or absent estimate.
4. A food-name suggestion writes nothing to the vault. One final **Save meal changes** persists the complete validated draft. Catalog-backed entries cannot be saved if the offline catalog or an unchanged saved match cannot be resolved. Save failure keeps the editor and its pending values visible.
5. Removing an optional reference photo changes only the draft. Cancel/back leaves the saved photo and meal unchanged.
6. Draft text and selections survive configuration recreation. An active suggestion is cancelled and cleared on editor exit or recreation; the member can retry without a stale response appearing for another food or meal.

**States:** direct edit, suggestion loading, suggestion unavailable/failure, stale suggestion, apply/discard, catalog match cleared or reselected, manual nutrition preserved, pending photo removal, keyboard visible, rotation, cancel, save failure, save/reopen.

## 4. Scan a nutrition label

**Intent:** capture label values without manually transcribing every number.

1. Member chooses Scan label from the central action.
2. Capture screen guides framing and offers retake or existing photo.
3. OCR runs locally and opens a compact label review.
4. Serving size and calories are first; protein, carbohydrates, and fat follow in a simple grid.
5. Missing or ambiguous fields are highlighted individually. Recognized values remain editable.
6. Member confirms; the saved entry uses the “Verified label” source only after that review.

**States:** unreadable image, missing serving, decimal/locale ambiguity, calories derived from macros, cancel, save.

## 5. Understand progress

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
