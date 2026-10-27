# Progress evolution verification

Contract and current acceptance status: [progress evolution](../design/progress-evolution.md).
Follow `../TESTING.md` and `../skills/metria-visual-qa/SKILL.md` for tooling,
privacy and installation. These instructions grant no record-write authority.

## Synthetic checks

Debug fixtures: `native-android/app/src/debug/java/app/metria/ui/screens/ProgressEvolutionPreviews.kt`.
The full-screen fixtures use a preview-only Back dispatcher and fixed
2026-10-27 date. Named previews include `ProgressOverviewPreview`,
`ProgressOverviewEmptyPreview`, `ProgressOverviewLargeTextPreview`,
`ProgressEvolutionCompactPreview`, `ProgressEvolutionDetailPreview`,
`ProgressEvolutionEmptyPreview`, `ProgressEvolutionLargeTextPreview` and
`ProgressEvolutionOnePointPreview`. Render one affected state at a time in
Android Studio with the debug variant. They do not open the vault or model.
See the contract checkpoint for which ones were actually rendered.

Use fixed invented debug fixtures and production components. Check sparse weekly
weight across 3 months; empty and single-point data; a large-font narrow viewport;
and long-range daily nutrition with partial coverage. Inspect the actual date
and value scale, selected value and neutral dated change, rather than judging
the curve alone. Static preview rendering cannot prove tap, scroll, Back or
native system-bar behavior. No model or photo processing is required.

Automated coverage should exercise inclusive calendar boundaries, leap years,
explicit time zones, missing values, latest same-day weight, multiple retained
readings, daily full/partial nutrition and neutral change in kg/lb. Run the
canonical full unit/debug gate once the final Android changes are ready.

## Bounded phone itinerary

1. Before an authorized in-place installation, inspect the previous Progress
   chart if the phone is connected and unlocked. Keep private captures temporary
   outside the repository; retain only sanitized conclusions.
2. After member unlock, enter Progress through its tab or Today See all. Confirm
   Weight initially uses 3 months, and an older selected reading has its actual
   date. A missing reading today must not appear as today's weight.
3. Choose every period; inspect empty/sparse states naturally present without
   adding test records. Touch a point and use alternative selection controls.
   Check that both update the selected date and value together.
4. Switch to Calories and Protein (initially 7 days), change their periods, then
   switch back to check each choice. Check daily-value and partial labels.
5. Open View detail. Inspect larger chart, dated values and any pagination;
   Back restores the same period and selected point. All history must still open
   its existing Weight/Meals tabs; do not edit or save any record.
6. Repeat the difficult layout in landscape and with enlarged system font.
   Check TalkBack descriptions, non-color selection, 48 dp controls and bottom
   navigation clearance. Record original settings and restore changes.
7. Rotate in detail, then go Back; verify restoration. Background/foreground
   must respect the existing vault lock policy. No biometric automation.

This flow adds no text-entry screen, Save or asynchronous operation. Existing
record editors, keyboard checks and assistant gates are not accepted by this
chart itinerary. Record device/build identity, exact states checked, failures,
remaining gaps and installation data preservation in the contract checkpoint.
