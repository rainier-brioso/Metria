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
**Secondary actions:** edit dish, add dish, review estimate details, discard.  
**Rule:** never render every ingredient’s name, description, grams, preparation, role, match, and delete action in one continuous form.

## Dish editor

**Purpose:** correct one dish without repeatedly typing its name.

- Dish name appears once at the top.
- Components are compact rows with name, portion, evidence state, and match status.
- Tap a component to edit its details in place or in a subordinate sheet.
- “Add component” asks whether it was visible or is a possible hidden ingredient in member language.
- Components can move between dishes without retyping the destination name.
- Deleting a component does not dismiss the editor.
- Save/cancel are persistent and keyboard-safe.

## Assistant overlay and conversation

**Purpose:** let Metria help without making the rest of the app disappear.

- Compact action menu: Speak, Chat, Analyse meal, Scan label, Manual entry.
- Listening state replaces the central action with visible motion, “Listening”, and Stop.
- Chat opens as a large sheet or full screen with a familiar message timeline, persistent composer, and cancel-generation control.
- Tool results appear as compact structured cards; model prose stays short and conversational.
- Reopening the conversation never resubmits the previous message.
- Header exposes a labelled **New conversation** action with destructive confirmation.

**Approved first slice:** conversation opens as a full-screen, keyboard-safe destination from the central Metria action. The header communicates on-device privacy once; member messages, assistant prose, and facts from encrypted records have distinct visual roles. Thinking and listening are live, cancellable states. Weight saves and meal drafts appear as structured, reversible action cards. The timeline scrolls to new content, survives configuration recreation while the vault session remains active, and a restored completed tool call must never execute twice. Runtime and raw-response inspection live only under the debug-build diagnostics entry in Privacy.

**Conversation continuity:** typed conversation uses one native LiteRT `Conversation` while the vault session is unlocked. It is rebuilt from a token-bounded window of encrypted visible turns after restart or context rollover; local evidence and hidden reasoning are not replayed as model memory. The active timeline is encrypted in the vault, shared by all Chat entry points, capped at 50 visible messages, and retained for 24 hours after its latest visible activity. It survives biometric relock and process restart, but is excluded from recovery archives. Locking clears in-memory model context. Assistant prose may resolve conversational references but cannot become record evidence; every personal factual answer executes the matching approved local read tool. Model results carry request IDs and are displayed only when they match the active request and have not already been consumed.

## Privacy and settings

**Purpose:** manage private data, assistant storage, preferences, and recovery.

- Present privacy as understandable settings and status, not a developer diagnostics page.
- Group: Privacy promise, local assistant, units and goals, cycle context, encrypted recovery, saved private dishes, and About.
- Development diagnostics are absent from release UI and live behind one explicit development-only entry.
- Destructive actions have clear consequences and confirmation.

**Approved first slice:** use one calm, scrollable settings hub. Lead with the encrypted-on-device promise, then group personal settings (units, member-entered goals, cycle context), private features (local assistant status and saved dishes), encrypted archive actions, and About. Routine rows reveal their current state before opening a focused editor or detail. Archive creation and restoration remain separate actions. Locking Metria is visually separate at the end, and development diagnostics appear only as one explicit debug-build row.

## First run and model setup

**Purpose:** establish trust and complete required offline-assistant setup.

- Explain local-only behavior once in clear language.
- Model download shows size, network requirement, progress, cancellation, and what data is—and is not—sent.
- Completion moves directly into biometric vault setup/unlock.
- Returning after interruption resumes or clearly restarts the download; it must not strand the member in an indefinite loader.
