# Goal journey mockup - 2026-10-07

Status: original fictional design preview for S0; hierarchy approved by the
member ("like it!", 2026-10-07). Android/phone acceptance remains separate.
Open [interactive goal journey](prototypes/goal-journey.html).
The [canonical roadmap](goal-view-roadmap.md) retains all reference ideas and
deferred calculation/data decisions. This preview supersedes the goal journey
portion of the older meal-entry prototype, whose unused exercise/strength fields
are not proposed for Android implementation.

## Hierarchy and navigation

Goals leads with a current-phase summary and one dominant Edit weight goal
action. Update weight stays quiet; daily references and compact previous rows
follow. Each history row opens its selected phase. New phase is distinct from
Edit. Reference provenance and data management disclose on demand.

New phase follows direction -> optional chosen weight context -> reviewed
references/used inputs -> explicit replacement confirmation. All setup changes
stay in a fictional in-memory draft until the simulated save succeeds. Editing
the current weight goal preserves its starting reference, phase start and daily
references. The prototype provides focused target/range editing; it does not
implement a new calculation or replace the app's suitability workflow.

After save: Saved -> new-phase acknowledgement -> daily references -> chosen
weight summary -> Goals. Each optional summary is navigable without swiping.
The acknowledgement describes a saved action, not reaching a weight target.

| View | Preview destination | Adaptation |
| --- | --- | --- |
| G01 | Phase changed | Acknowledgement after successful replacement |
| G02 | Previous phase | Saved phase references; unknown end date explicitly disclosed |
| G03 | Confirm phase | Draft review, replacement effect, save failure/retry |
| G04 | Direction | Lose, gain, maintain, just track; selected text and mark |
| G05 | Weight context | Optional rows and focused editor, starting reference distinguished |
| G06 | Review inputs | Used-input review; manual alternative, no unused exercise/diet fields |
| G07 | Saved | Static accessible success with Continue |
| G08 | Daily references | Manual references or illustrative accepted activity-scenario interval; missing macros unset |
| G09 | Weight summary | Chosen endpoints/range, no forecast or deadline |
| G10 | Goals | Compact current/history hierarchy and selected-phase navigation |
| G11 | Today | Compact weight/goal and separate partial recorded-nutrition example |

## State exploration and boundaries

The conversation preview exposes view, example state, reference source and
enlarged text through its design controls. The standalone page has a small
fallback view/state selector. Product buttons support the new-phase journey,
focused editors, Back, Keep editing, cancellation, simulated save/retry and
history detail. All sample records are fictional; nothing connects to a model,
vault, member dataset, cloud service or account. Preview state is temporary.

States include recorded goal, just track, no goal/history, no readings, equal
range, unknown historical dates, save error and saving. Estimate examples are
illustrative saved values, not a calculator. Accepted-estimate starting reference
and calculation inputs are read-only here: changes must use the app's existing
reviewed calculation or manual workflow. Progress/Privacy navigation is disabled
in this focused preview. Weight entry is a layout/navigation placeholder; it
does not create records or demonstrate actual weight persistence.

Historical phases have no explicit end timestamp in today's data contract.
This mockup consequently withholds period-derived weights, logging consistency,
nutrition averages and top foods. It does not invent a period using updatedAt
or adjacency. Exact closure data and those calculations remain S0/S3/S4 work.

## Verification and next slice

Isolated local browser inspection checked all eleven views at 320, 390 and
736 px with normal/enlarged text; no horizontal product overflow or script
errors. The new-phase save journey, selecting different historical phases,
and failed-save retry were exercised. Rendered hub and narrow enlarged-weight
views were visually inspected. Temporary renders contain fictional examples
only and are removed after review. This is browser design evidence, not phone,
keyboard, TalkBack, rotation, encrypted-write or recovery acceptance.

Independent read-only review identified wrong history selection and editable
accepted-estimate starting context; both were corrected before delivery.
No Android code or dependencies changed. No Android gate/install, persistence
extension, new nutrition method, commit or push is part of this slice.

Next: review this hierarchy, then implement S1 compact Goals hub and selected
historical-phase detail with the unknown-period fallback. Continue S2-S5 in the
roadmap order. The installed compact Today card physical gate remains open.
