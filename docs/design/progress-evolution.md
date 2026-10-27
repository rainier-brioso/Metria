# Progress evolution - 2026-10-27

Status: implemented; unit/debug gate, five synthetic renders and targeted
portrait/landscape/150%-font phone checks pass. TalkBack and gesture navigation
remain open; this is not full release accessibility acceptance.
Member approves replacing the fixed Your week chart with Your evolution, period
selection and a larger read-only detail. Today retains its existing See all
entry to Progress; the chart lives in Progress, not a duplicated Today dashboard.

## Hierarchy and interaction

Progress keeps its metric selector and one dominant selected reading, followed
by Your evolution, the current-goal context and selected-day records. Periods
are 7 days, 1 month, 3 months and 1 year, ending today inclusive. Calendar month
and year boundaries must handle leap years and short months deterministically.
Weight initially uses 3 months; Calories and Protein initially use 7 days.
Remember each metric's chosen range during configuration restoration and detail
return, without introducing a persisted profile preference.

The chart positions measurements by date, preserves days without values as
unknown and never supplies invented zeros or carried-forward weights. Weight
shows the latest saved reading for each recorded day; existing day/all-history
inspection retains every saved reading. A neutral first-to-last recorded change
states its actual covered dates and uses the member's kg/lb unit. It makes no
positive/negative judgment or health claim. Empty ranges say no records in this
period; a single weight reading remains a point with a request for another
reading to compare. Show the date of the displayed reading rather than implying
that an older reading is today's weight.

Touch selection exposes the selected date and value. Provide an alternative
to precise chart touches, with named controls of at least 48 dp and non-color
selected state. Avoid hundreds of day buttons or a full-year TalkBack monologue.
Show readable date/value scale context so small changes are not visually
exaggerated without numeric context. Nutrition retains daily recorded estimates,
unknown days and partial coverage disclosures; larger ranges do not silently
convert totals into averages or imply complete intake.

View detail opens a larger chart and dated read-only values for the selected
metric and period. Back restores the same range and selected point. Long-range
history must remain bounded or progressively disclosed. Existing All history
remains the explicit route to saved-record editors. No new Save action, model
request, storage format, cloud access, dependency or record mutation is added.

## States and verification

Exercise empty, one point, sparse weekly weight over 3 months, same-day multiple
weights, leap/calendar boundaries, kg/lb, dense yearly nutrition, partial-only
nutrition, selection/range changes, detail/back and configuration restoration.
This is synchronous local presentation; there is no new async loading or failure
operation. Existing vault/session lock policy remains authoritative.

Add fixed invented debug previews for compact, detail, empty and enlarged-font
layouts using production components. Run the canonical unit/debug gate and
independent review. Physical acceptance separately covers portrait/landscape,
font scaling, TalkBack, navigation-bar clearance, chart/alternative selection,
period retention and detail/back. No member-record writes are needed or authorized.
Record exact evidence and remaining gaps here before handoff.

## Implementation checkpoint - 2026-10-27

Built on source checkpoint `187fd9e`, with no changes to Today, MemberShell,
assistant handling, vault persistence or record editors. Range, timeline and
chart/detail presentation live in `ProgressScreen.kt`; debug-only fixtures live
in `ProgressEvolutionPreviews.kt`. Dates are grouped once per range using the
requested zone. The plotted daily latest weight retains all same-day readings
in detail. Date/value selection and each metric's range use `rememberSaveable`
for configuration restoration while this destination remains present; this is
not a cross-session persisted preference. Details disclose up to 12 recorded
dates per page, newest first, with existing records read-only.

Independent review identified and resolved destination-metric selection,
pagination direction, missing scale/selection context and color-only/partial
indicators. Parent render review additionally corrected yearly nutrition to
daily bars (no joining missing days), showed years across calendar boundaries
and changed narrow/large-font range controls into multiple rows. The initial
compile failures and initial large-font wrapping are not accepted results.

Final canonical gate succeeds: `:app:testDebugUnitTest :app:assembleDebug
--no-daemon`, 749 tests, zero failures/errors/skips, debug APK assembled.
`git diff --check` passes. Final APK SHA-256:
`1bdd0dfc211a0f176eab87cbd63f1257eb3e8e4accc3cb92a26b61029641186e`.
Regression cases cover calendar/leap boundaries, out-of-period weights,
latest/all same-day readings, sparse unknowns, partial nutrition and midnight
zone handling. Existing test setup has no Compose instrumentation dependency;
no UI-test dependency was added. Restoration and pointer/Back semantics still
need physical interaction, not an inferred pass from pure unit coverage.

Final Studio renders visually inspected: full populated overview; 320 dp/150%
font overview (period labels now fit in two rows); full empty overview; one
weight point; and annual nutrition detail with dated values, scale, partial
marks and both calendar years. No blocking overlap/truncation in inspected
content. Detail/list tails and below-viewport actions still require scrolling
on the real phone. A synthetic populated overview is retained outside the repo
as a review artifact; temporary renders are removed. No private data used.

No phone was connected during preflight; no installation, before/after phone
capture, inference or record write occurred. Installation authorization was
requested for this cycle and remains pending at handoff. Next bounded task:
connect phone, authorize in-place installation, member unlock, then follow
`../testing/progress-evolution.md` for range/point selection, detail/back,
rotation, font, TalkBack and navigation-bar acceptance without saving records.
This checkpoint does not close those physical gates.

### Authorized installation follow-up - 2026-10-27

Member explicitly authorizes `install -r` preserving existing data. Reference
Samsung SM-S928B / Android 16 is now connected; exact transport selected from
`devices -l` without persisting its identifier. APK hash matches the final
749-test/debug gate above. In-place installation returns Success; force-stop
and cold start return Status ok, TotalTime 2,938 ms. No clear/uninstall/storage
replacement or record write performed. Member unlock and retained-record/UI
smoke checks are pending; installation alone is not physical acceptance.
Next: member unlock, then the bounded read-only phone itinerary.

### Read-only phone checks and detail entry correction - 2026-10-27

On installed `1bdd0dfc...`, Samsung SM-S928B / Android 16, member unlocked and
opened Progress. Existing weight readings and nutrition totals render, providing
a retained-data smoke check after the in-place update. No entry, meal, goal or
profile was edited/saved/deleted. Checks observed:

- Weight starts at 3 months. All four ranges respond; the naturally empty recent
  week shows no invented weight, a month displays sparse readings and the annual
  view states both calendar years with dates positioned across the full period.
- Calories and Protein initially use 7 days. Calories retains a chosen month
  after switching to Protein and back; Weight independently retains its year.
  Existing partial nutrition has a text badge and square plotted mark.
- Previous-date control updates selection. View detail retains it; direct chart
  touch selects a different actual recorded date and updates the shown value.
  Android Back restores the same metric/year and selected date in the overview.
  Overview scale/date controls and View detail are reachable by scrolling above
  the dock; detail rows show dates/times and the visible selection check.

Found a real entry defect: detail inherited the shell's overview scroll offset
and initially opened partway down Recorded values, with its chart/header above
the viewport. This is not a passed detail-entry gate. A bounded change in
EvolutionDetail now brings its header into view once after layout, including
configuration recreation; metric/range/date/page remain saveable. It does not
repeat on ordinary recomposition. No shell, storage or record behavior changed.
The installed Compose version requires a localized ExperimentalFoundationApi
opt-in for this existing dependency API; final gate succeeds after adding it.

Corrected build: full unit/debug gate passes, 749 tests, zero
failures/errors/skips, debug assembled and diff check clean. APK SHA-256:
`090d237e045cc1e049fc3152fd567356ee25127fa67a62dc93dc68aebf6eb577`.
Correction install/launch and its physical retest are recorded below when done.

Correction installed in place during the same authorized test cycle:
`install -r` returns Success; force-stop/cold start Status ok, TotalTime
2,339 ms. No destructive storage action. Member unlock requested; corrected
entry focus, rotation/font and the remaining gates need the next phone pass.

CLI `layout --flat` still returns an unrecognized instrumentation response;
one bounded UIAutomator attempt also yielded no hierarchy. Temporary annotated
screens were used, with one path overwritten and removed after inspection.
Initial two actions did not establish the intended state while scrolling changed;
only subsequently observed selections count. No raw images/dumps or member
values are retained in source/evidence. Original user rotation is `free` and font
scale `1.0`; both were read only, not changed during this pass. Three-button
navigation observed. Rotation, enlarged physical font, TalkBack, gesture
navigation, paging and All history regression remain pending. No model runs.

### Corrected-build phone continuation - 2026-10-27

On installed `090d237e...`, Samsung SM-S928B / Android 16, member unlocked.
No Android source change, new build/install or model run in this continuation;
the final 749-test/debug gate above remains applicable. Observed:

- Opened Progress, selected an earlier actual reading with the date-step control,
  and opened View detail from a displaced overview. Header and larger graph now
  appear at the top, retaining the selected reading and 3-month period. This
  closes the reproduced detail-entry scroll defect on the corrected APK.
- Temporarily locked rotation to landscape. Detail and period survive; after
  scrolling, the identical selected date/value and both date-step controls are
  visible above the dock/system navigation. No unexpected vault lock.
- Returned to portrait and temporarily set system font scale to 1.5. After one
  transient black capture during Android's configuration change, a stable capture
  shows detail, selected reading and rows at the enlarged size without overlap.
  Scrolling exposes the complete pager above the dock. Earlier dates changes
  page 1 of 2 to 2 of 2 with appropriately older records and disabled end action.
- Android Back returns to the same selected metric, 3-month period and earlier
  reading. The overview range selector uses two readable rows at 150% font;
  title, main value and supporting copy fit the inspected viewport.
- Restored font scale to 1.0 and user rotation to free; both independently read
  back with the original values. Opened All history Weight and Meals tabs:
  existing dated records and meal thumbnails remain available. No record row
  or editor opened, and Back returns to the same Progress selection.

Metria is left in Progress. No record/goal/profile Save, edit or delete occurred.
No biometric automation or app cloud request. The authorized temporary host
captures were used only for this visual inspection. The one temporary
annotated screenshot path was reused and removed; no raw member evidence is
retained in the repository. No gesture-navigation or TalkBack pass claimed;
screen-reader traversal and alternate system-navigation acceptance remain the
next bounded accessibility checks. Detail-page retention specifically across
configuration recreation was not separately observed; only the selected reading,
period and detail route were verified across rotation/font changes. Keyboard is
not applicable to this read-only chart flow. Earlier broader assistant gates
remain separate and are not closed by these checks.
