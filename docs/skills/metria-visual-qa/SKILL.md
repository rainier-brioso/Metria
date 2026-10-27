---
name: metria-visual-qa
description: Diagnose and verify Metria Android UI with targeted evidence, fewer device round trips, and preserved member data. Use for assistant panels, capture/review screens, keyboard, system bars, rotation, and visual regressions.
---

# Metria visual QA

This repository-local skill is routed explicitly from `AGENTS.md`; it does not
depend on a globally installed skill. Paths below are relative to repository root.
Read `docs/TESTING.md` for authorization, installation, privacy and acceptance
rules. Read `docs/testing/visual-debugging.md` when choosing tooling or adding
preview/test infrastructure. Neither document grants device access or record saves.
For assistant/review rendering, start with `docs/testing/assistant-previews.md`
and its debug-only synthetic preview entry points before replaying real inference.

## Choose the smallest useful observation

1. Read the current checkpoint and dirty diff. State the exact defect, current
   APK identity, expected result and next observation. Continue from the member's
   current screen; do not restart a live draft merely to establish a baseline.
2. Inspect code/state and existing tests first. For rendering changes, use an
   existing synthetic Compose preview if available. Missing previews are an
   implementation gap, not a reason to claim a preview passed.
3. For device interaction, use the installed `android-cli` skill and its required
   references. Check installed command help when necessary. Prefer compact
   `android layout --flat`; `--diff` is deprecated and a no-op.
4. If the known instrumentation error repeats, record that inspection path as
   unavailable for this session. Retry only after a relevant condition changes.
   A clipped dialog calls for actual window bounds/insets, not repeated guesses
   based on physical display size or more screenshots of the same state.
5. Capture when pixels answer the question or required acceptance evidence is
   missing. Immediately view every Android CLI capture before another action.
   Reuse it for inspection and label resolution while that UI state is unchanged.
   After scrolling, keyboard, rotation or navigation, obtain fresh target evidence.
6. Execute a short known sequence only while its targets remain established.
   Observe at meaningful transitions; never batch blind coordinate taps. Prefer
   bounded state-based waits to repeated screenshots or long fixed sleeps.

## Avoid expensive replay

- Separate visual rendering from OCR/model correctness. Propose synthetic
  in-memory draft previews for repeated layout work; use the member-selected
  image only for the real processing and integration checks that require it.
- Do not preserve private images, bypass unlock, write test records or introduce
  a production debug entry to make tests convenient. Debug fixtures need an
  explicit design and must not read/write the member vault or run the model.
- Make a coherent fix supported by evidence, then run the complete Android gate
  before an authorized `install -r`. Reuse that exact build's gate for subsequent
  checks if Android inputs have not changed. Documentation-only changes need no
  new APK. Keep mandatory smoke/quality gates and honest gaps.
- Prepare one phone itinerary for the affected flow: relevant normal state,
  last-field keyboard, supported rotation, secondary editor, cancel and async
  states. Under a no-save instruction, leave Save/reopen explicitly untested.
- Record original rotation/font/navigation settings before changing them and
  restore any settings actually changed. Preserve the current draft where the
  product's lifecycle policy allows it; do not promise persistence it lacks.
- Apply Changes/Live Edit and screenshot frameworks are conditional tools, not
  configured defaults. Verify support and scope first; final acceptance uses an
  identifiable APK and the canonical build/install process.

## Evidence and handoff

One agent controls the phone. Delegate only bounded independent code discovery,
implementation or review when it saves work; share a concise brief rather than
duplicating the entire device session. Name `android-cli` in delegated device work.

Keep private screenshots and hierarchy dumps temporary, outside the repository;
remove them after use. Keep only sanitized conclusions in checkpoints and approved
redacted/synthetic evidence where required. Never record device endpoints or raw
member values. Do not infer physical acceptance from a preview, unit test or build.

Record build, device/Android, state, expected/observed result and remaining gaps.
For repeated work also note captures, installs, real inference runs and member
interventions when useful; report savings only after measuring them. On tool or
approval failure, retain the last verified state and next bounded check. Do not
retry the same blocked action through another tool.
