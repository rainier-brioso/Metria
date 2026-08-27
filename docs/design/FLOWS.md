# Metria primary UX flows

These flows define member intent and state transitions. They do not prescribe a specific composable hierarchy.

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
4. Review starts collapsed: dish name, principal components, estimated portion, confidence, and available nutrition.
5. Tapping a dish opens a focused dish editor. The member can rename the dish once, edit components, mark possible hidden ingredients, regroup foods, and choose offline nutrition matches.
6. Metria calculates nutrition only from confirmed quantities and accepted matches. A partial calculation is valid and clearly distinguished from a complete one.
7. The member confirms the meal. Raw source imagery is discarded; an optional reduced encrypted reference thumbnail follows the existing vault policy.
8. Success returns to Today and opens the saved meal if the member chooses “Review entry”.

**States:** camera permission, cancellation, model warming, analysis, cooldown, malformed result, no food, partial proposal, catalog match ambiguity, keyboard visible, background/resume, save, discard.

## 3. Correct and teach a private dish

**Intent:** make repeated personal meals easier without claiming that the model learned globally.

1. During review, the member renames a dish or corrects its ingredients.
2. “Remember as my dish” is on by default but plainly reversible.
3. Metria shows exactly what will be remembered: private dish name, aliases, confirmed component structure, and optional reduced reference thumbnail.
4. Saving the meal updates or creates the encrypted personal-dish record.
5. A later analysis may surface “Looks like your San Jacobo” as a suggestion, never as an unquestionable identification.
6. The member can accept, edit, or ignore it. Corrections update the private dish only after confirmation.

**States:** duplicate name, no usable components, image not retained, suggestion accepted, suggestion rejected, private dish removed.

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
5. Complete and partial nutrition are visually distinct and explained on demand.
6. Assistant insights use only approved local tools and the selected period.

**States:** no data, partial estimates only, goal absent, one-day history, selected day, changed range, cycle-aware context enabled/disabled.

## 6. Ask Metria

**Intent:** use local language or voice to navigate, record, or understand data.

1. Member opens the central Metria action from any main screen.
2. Compact choices appear: Speak, Chat, Analyse meal, Scan label, Manual entry.
3. Listening is unmistakable and automatically stops after speech or timeout.
4. Tool-backed requests show grounded results or a validated action outcome.
5. General prose is allowed when safe, but it must not pretend to have read records without a matching approved tool result.
6. Conversation preserves prior messages while the unlocked session remains active and does not replay the last request merely because the screen reopens.

**States:** model warming, listening, no speech, background noise, unsupported request, tool selection, tool result, cancellation, navigation away and return.

## 7. Edit or delete an entry

**Intent:** correct personal data without ambiguous controls.

1. Member taps the whole row/card.
2. A focused detail editor opens with the current values and optional image.
3. The photo delete control is visually attached to the photo. Entry deletion is a separate destructive action at the bottom.
4. Removing a photo keeps the editor open and updates the pending draft.
5. Saving applies the complete edited draft atomically. Cancellation leaves the stored entry unchanged.

**States:** keyboard visible, photo removed, edited after photo removal, delete confirmation, save failure, cancel.
