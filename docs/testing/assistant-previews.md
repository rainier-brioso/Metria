# Assistant and review previews

The preview entry points are in
`native-android/app/src/debug/java/app/metria/ui/screens/AssistantAndReviewPreviews.kt`.
Fixtures exist only in the debug source set. They use invented meal/label data,
fixed dates and times, the real Metria theme and production composables. No vault,
photo, OCR, model execution or new dependency is needed. Preview callbacks cannot
save member records; editable fields change only local preview state.

## Open and inspect

Open `native-android/` in Android Studio, select the `debug` variant and open the
preview file in Split or Design mode. Select the named preview rather than
rendering every variant while iterating on one component. Use Interactive mode
for local text editing, scrolling and disclosure controls; external actions and
Save intentionally have no persistence effect.

Available groups cover assistant welcome, action choices, preparing a draft,
partial draft and recoverable error; meal review and a partial review in small
or landscape viewports with enlarged text; and a partial nutrition-label editor.
The conversation previews call the full production conversation screen. The meal
previews call its shared review content directly, without the native dialog host.
They are rendering fixtures, not a simulated end-to-end analysis or camera flow.

Prototype completion adds `AssistantSavedDishesPreview` (360 dp, 150% font,
invented long dish names), `AssistantSavedDishesEmptyPreview` and
`AssistantMealUpdatedPreview`. The list calls the production dish selector with
inert selection callbacks; it never reads the vault or opens a real meal.
`LabelReviewLargeFontPreview` uses a fixed midday time to render the new editable
category. Build/render observations for these changes belong to the active M4
checkpoint; their presence alone does not establish a passed visual gate.
`SavedDishReviewPreview` reuses the same review component with saved-dish
presentation, so its title/intro can be checked without touching member memory.

Completion follow-up 2026-10-27: all five affected entry points above rendered
successfully and were visually inspected after the final 745-test gate. Populated
list text wraps at 360 dp/150% font; empty state, editable label category and
updated-meal link fit their initial viewports. Saved-dish Review uses its explicit
title/intro with both footer actions complete. Native-dialog insets remain a
separate phone check. If the CLI reports a `NoSuchMethodError` with a
Layoutlib-renamed Kotlin callback while source/build signatures agree, first
refresh the out-of-date previews in Studio and let its build finish. That resolved
this session's mixed preview classes; no production API/dependency change was
needed. Do not repeat failed renders without a relevant refresh or build change.

Initial verification: full `:app:testDebugUnitTest :app:assembleDebug --no-daemon`
gate passes 738 tests with no failures/errors/skips; independent code review and
`git diff --check` pass. All nine preview entry points compile. No phone install
or visual-render acceptance is claimed.

When a compatible Studio instance is running, the installed Android CLI supports:

```powershell
rtk proxy android studio check
rtk proxy android studio render-compose-preview native-android/app/src/debug/java/app/metria/ui/screens/AssistantAndReviewPreviews.kt MealReviewPreview --project <project-from-check> --output-image-file <temporary-path-outside-repository.png>
```

Use current `--help` if the CLI changes. Immediately view a resulting image
before further actions. Keep temporary renders outside the repository; only
deliberately approved synthetic visual evidence belongs in design evidence.
No running Studio instance was found during initial setup. Subsequent visual
review connected successfully to Android Studio Rabbit 1 / 2026.2.1 with
`MetriaNative` ready. Compilation, preview rendering and physical interaction
remain separate evidence.

## Visual inspection - 2026-10-26

All nine entry points rendered successfully through the Studio CLI and each PNG
was visually inspected immediately. No Android sources changed during this pass;
the previous 738-test/debug build gate remains the automated evidence.

| Preview | Observed result |
| --- | --- |
| AssistantIdlePreview | Header, invitation, suggestion and composer render with no visible overlap |
| AssistantChoicesPreview | All five tool choices fit the initial viewport; labels and supporting text remain distinct |
| AssistantPreparingPreview | Preparing message, review-before-save explanation and Stop appear; animation/cancellation behavior not exercised |
| AssistantPartialDraftPreview | Invented conversation, not-saved draft label, food summary, Review and Discard appear; lower card border meets the scroll viewport |
| AssistantErrorPreview | Error explanation wraps legibly; composer stays separate; no real failure/recovery performed |
| MealReviewPreview | Dish grouping, estimate labels and both complete footer buttons visible in portrait |
| MealReviewLargeFontLandscapePreview | At 852x393 dp / 150% font, both footer actions are complete; only the first content items fit above them |
| MealReviewNarrowLargeFontPreview | At 360x740 dp / 150% font, confidence and omission text wrap; both footer actions remain complete |
| LabelReviewLargeFontPreview | Fields, blank missing fat value, fixed date and complete Save label render at 150% font |

No blocking visual overlap or text truncation was observed in these initial
viewports. Content cut at a scroll-container edge is not evidence that the
remaining content is inaccessible; scrolling was not exercised through the static
renderer. In landscape, the fixed header/footer leave limited content height,
so reachability still needs an interactive check. The label preview is the inner
editor, not the complete capture panel; choices are shown without the shell's
navigation dock. Neither preview proves integrated native-window/system-bar layout.

Cost evidence for this pass: nine synthetic renders, zero installs, zero real
inference runs and zero phone interactions. Temporary PNGs were removed after
inspection. No percentage/token-cost saving is inferred from those counts.

## What these fixtures prove

Save follow-up (2026-10-26): a tenth entry point, `AssistantMealSavedPreview`,
shows the real conversation receipt rendering with a fictional available meal ID.
It was rendered and visually inspected; confirmation and View meal fit the panel.
Its navigation callback is inert. This tests appearance, not the real save path.

Use them to inspect hierarchy, wrapping, font scaling, scrollable content and
the appearance of uncertainty/errors without selecting a photo or reinstalling.
Update the smallest relevant fixture alongside a changed component. Keep long
text, missing nutrient values and fixed inputs so difficult states remain easy
to reproduce. Avoid multiplying all possible configuration combinations.

Previews do not validate real OCR/model accuracy, TalkBack traversal, biometric
unlock, native window measurement, actual keyboard/system-bar insets, cancellation
races or restoration after lock. Those checks remain in `../TESTING.md` and the
assistant checkpoint. Losing an unsaved draft on lock is not repaired by adding
previews, and preview state is not a new product persistence promise.
