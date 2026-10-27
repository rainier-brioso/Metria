# Efficient Android visual debugging

Research and repository audit: 2026-10-26. This supplements `../TESTING.md` and
`../design/QUALITY-GATES.md`; it does not reduce their physical acceptance gates.
Agent entry point: `../skills/metria-visual-qa/SKILL.md`.

## What currently costs repeated work

The assistant checkpoint records successive footer inset fixes followed by
installation, unlock, photo selection and real inference to reach Review again.
Actual dialog measurement eventually distinguished display size from the fitted
window. Replaying inference is costly when the question is footer geometry.
These are observed workflow problems, not measured token or money savings.

Audit of the current working tree:

| Capability | Current evidence | Use |
| --- | --- | --- |
| JVM tests and debug build | Existing wrapper gate; latest assistant gate reports 738 tests | Parsing/state correctness and buildability |
| Compose previews | Initially absent; synthetic assistant/review previews added in the follow-up | See `assistant-previews.md`; compilation and rendering are separate evidence |
| Compose interaction tests | No `androidTest`, UI-test dependency or instrumentation runner configured | Separate infrastructure work; do not invent test commands |
| Screenshot regression tests | No framework or reference images configured | Evaluate later for stable reusable screens |
| Android CLI compact layout | Known instrumentation response failure on reference Samsung | Prefer when working; stop repeated identical failed probes |
| Annotated screenshots and ADB | Used in active phone cycle | Targeted pixel checks and observed navigation |
| Studio preview/inspection | CLI offers Studio commands; current Studio attachment not established | Verify availability before planning around it |

The inspected project uses AGP 8.8.2 and Compose BOM 2024.12.01. Recheck build
files before introducing tooling; do not copy today's latest sample blindly.

## Tool selection by question

| Question | First useful evidence | Limits |
| --- | --- | --- |
| Wrong draft value or state transition? | Parser/policy code and focused deterministic tests | Does not prove the UI renders correctly |
| Spacing, hierarchy, long text, large fonts? | Synthetic state-driven Compose previews | Layoutlib does not reproduce every device API/window |
| Is an action present, enabled or focused? | Compact semantics/layout; Compose interaction test once configured | Semantics alone cannot establish pixel appearance |
| Footer behind navigation/IME? | Actual dialog window frame, content bounds and insets, plus targeted screenshot | Display dimensions alone caused a misleading diagnosis here |
| Does it match the approved design? | Same-viewport before/after and difficult-state images; overlay if useful | Prototype is a design reference, not acceptance evidence |
| Wrong real OCR/model result? | One authorized real-image run and bounded relevant diagnostics | Fake drafts cannot validate ML Kit or model correctness |
| TalkBack, picker, biometrics, lifecycle? | Target-device interaction under the existing safety rules | Preview success cannot close these gates |

[Compose previews](https://developer.android.com/develop/ui/compose/tooling/previews)
render without a phone/emulator and support sample data, dimensions, locale and
font variants. Their Layoutlib environment has file, network and Context API
limitations. Separate multipreview annotations produce separate variants, not a
Cartesian product; explicitly include important combinations such as narrow width
with enlarged font. Preview a stateless content boundary with synthetic data and
inert callbacks, rather than constructing vault/model dependencies.

[Compose testing](https://developer.android.com/develop/ui/compose/testing) uses
semantics, assertions, actions and synchronization. Once configured, meaningful
tests should cover focus/reachability, cancel/return, disabled/loading states and
late-result rejection. A node's existence is weaker than proving it is visible
and operable. Retain physical visual checks for IME and system bars.

[Layout Inspector](https://developer.android.com/studio/debug/layout-inspector)
can expose component attributes and compare a design overlay. It may require an
activity restart, so do not attach it to an irreplaceable live draft without
considering lifecycle effects. Filter diagnostics to the relevant app window and
geometry; avoid dumping unrelated apps or full logs into agent context.

## Short investigation and verification cycle

1. Record the hypothesis and expected observation before changing code. For the
   prior footer failure: compare actual content height with fitted dialog height,
   then verify both complete actions above system navigation and the keyboard.
2. Inspect existing code and targeted tests; reproduce rendering with synthetic
   data if that facility exists. After a failed fix, measure the missing fact
   before trying another inset combination.
3. Finish a coherent patch, run the required full Android gate, identify the APK,
   then install once within authorization. Avoid reinstalling an unchanged build
   merely because a new check starts. New Android inputs require a fresh gate.
4. Plan the remaining checks before touching the phone. Group checks sharing a
   draft; inspect keyboard and dish editor before navigation that may discard it.
   Preserve required baseline/new/difficult-state evidence without recapturing
   equivalent states. Never reduce gates to meet an arbitrary capture budget.
5. Keep observation local to its question. A screenshot for appearance, compact
   bounds for geometry, bounded metadata for runtime status. Immediately view
   Android CLI captures; resolve targets only against the unchanged current UI.
6. Record pass/fail/pending against the exact build. Keep settings restoration,
   temporary-file cleanup and the next bounded step in the handoff.

In a no-save cycle, do not exercise Save/reopen, remember portions, or other
persistence actions. Leave those gates pending explicitly. User unlock and image
selection remain user actions where required. A selected private photo is not a
reusable source-code fixture, and an authorized diagnostic capture is not general
permission to export health data.

## Recommended next infrastructure work

1. Add a small set of synthetic assistant capture, partial/error and review
   previews using existing tooling dependencies. Fix clock/text/image inputs for
   repeatability. Do not invoke real inference, read the vault or add a production
   route. Confirm the actual dialog hosting separately on Android.
   Follow-up: conversation, choices, label and shared meal-review fixtures are
   now provided in the debug source set; see `assistant-previews.md`. Integrated
   capture launchers and real processing remain physical/integration checks.
2. Investigate the compact-layout instrumentation failure as its own bounded
   tool issue. Do not let each screen check repeat the same failed setup.
3. Propose Compose instrumentation dependencies and isolated synthetic interaction
   tests if repeated interaction regressions justify them. This needs explicit
   dependency approval under repository rules.
4. Evaluate host screenshot comparisons after previews stabilize. The official
   [Compose screenshot tooling](https://developer.android.com/studio/preview/compose-screenshot-testing)
   is experimental and its setup is evolving. It is not installed here; assess
   compatibility and maintenance cost before adding a plugin or upgrading AGP.

[Apply Changes](https://developer.android.com/studio/run#apply-changes) can avoid
some process/activity restarts for supported debug edits. It has change-specific
limits and is not proven available in this session. Evaluate it only as an inner
iteration option; retain the identifiable full-build/in-place-install acceptance
path. Do not assume Live Edit or delta install preserves an unsaved draft.

For a repeated task, compare captures, reinstallations, real inference runs,
member interventions and elapsed time before/after adopting this workflow.
Token or monetary savings require actual usage evidence; no percentage is claimed.
