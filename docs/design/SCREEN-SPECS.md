# Metria screen contracts

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
3. Weight and nutrition summary cards.
4. Day strip for recent dates.
5. Today’s meals as visual rows grouped by meal time.

**Primary action:** central Metria action.  
**Secondary actions:** tap a metric for detail; tap an entry to edit; add from an empty meal slot.

**Empty state:** explain one useful first action, not the entire product.  
**Partial state:** show the available totals with the estimate marker and an accessible explanation on tap.

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

**Approved first slice:** use a fixed seven-day range including today with Weight, Calories, and Protein selectors. Selecting a metric surfaces its day value and applicable member-entered goal; selecting a chart day reveals only that day’s editable weight or meal entries in place. Partial nutrition remains included only as a visibly marked partial estimate. “All history” opens date-grouped Weight and Meals tabs containing every saved record; Android Back restores the unchanged seven-day view.

## Meal review

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

**Purpose:** correct an existing entry while keeping all changes reviewable until one final save.

- Keep direct food-name editing visible. Offer **Ask Metria about this food** beside each food and reveal one correction panel only for the selected food.
- The local text assistant may propose one replacement food name. Show the old → new names and require **Apply name** or **Discard**. Applying edits only that draft label and clears the old catalog match; the member may separately choose a new local catalog match.
- Manual nutrition values remain unchanged by a name suggestion and remain identified as member-entered. Catalog nutrition is recalculated from the member-confirmed matches and portions; unmatched foods are excluded and coverage remains visibly partial or unavailable. Before a catalog-backed save loses coverage, ask the member to confirm; block the save if the local catalog or an unchanged saved match cannot be resolved.
- **Save meal changes** is the only ordinary edit write. A vault write failure is shown inside the editor and retains the draft without replacing the unlocked shell. Removing the reference photo is staged until Save; Cancel/back leaves the stored meal unchanged.
- Saveable draft fields survive rotation. An in-flight correction is cancelled on editor exit/recreation; request and selected-food checks prevent stale proposals from being applied.

**States:** direct edit, assistant unavailable/busy, loading, result, stale result, apply/discard, manually selected replacement match, partial/no catalog estimate, manual nutrition unchanged, pending reference removal, keyboard visible, rotation, cancel, validation/save failure, save/reopen.

## Assistant overlay and conversation

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

## First run and model setup

**Purpose:** establish trust and complete required offline-assistant setup.

- Explain local-only behavior once in clear language.
- Model download shows size, network requirement, progress, cancellation, and what data is—and is not—sent.
- Completion moves directly into biometric vault setup/unlock.
- Returning after interruption resumes or clearly restarts the download; it must not strand the member in an indefinite loader.
