# Metria design direction

**Status:** approved foundation for the UI/UX reset — 2026-08-26  
**Scope:** native Android member app  
**Product character:** calm, private, warm, capable, and honest

## Intent

Metria should feel like a thoughtful personal companion, not a database editor and not a medical dashboard. The member should understand today in seconds, record something with one obvious action, and reach detail only when they ask for it.

The supplied reference screens are useful for their hierarchy, whitespace, rounded cards, strong metrics, visual meal rows, and central capture action. Metria must develop its own botanical identity and must not reproduce another app's branding, layouts, artwork, or assets.

## Experience principles

1. **Glance first, detail second.** Show the answer before the controls. A summary card should be understandable without opening it.
2. **One dominant action.** A screen may contain several actions, but only one should compete for first attention.
3. **Progressive disclosure.** Rows and cards open focused editors. Do not place a complete editor inside a feed.
4. **Calm honesty.** Estimates and uncertainty are visible, but technical provenance stays one level deeper unless it changes a decision.
5. **Private without repetition.** Privacy is communicated in onboarding, settings, and relevant moments. Do not repeat a privacy paragraph beneath every action.
6. **Assistant in context.** Metria may help from anywhere, while the current screen remains understandable and usable without conversation.
7. **Member language.** Use familiar words such as “food”, “portion”, “estimate”, and “review”. Avoid “schema”, “catalog match”, “model response”, “runtime”, and “inference”.
8. **Motion explains state.** Animation may show listening, progress, success, or continuity. It must never delay the member or become the only state indicator.

## Visual direction: botanical clarity

Use a warm near-white canvas, white elevated surfaces, deep ink typography, a restrained plum identity color, and a fresh botanical accent for progress and primary capture moments. The interface should feel light and tactile, with generous whitespace and limited elevation.

### Semantic color tokens

These are starting tokens for prototypes. Validate contrast on-device before locking their exact values.

| Token | Starting value | Use |
| --- | --- | --- |
| `canvas` | `#F7F7F3` | App background |
| `surface` | `#FFFFFF` | Cards, sheets, navigation |
| `surfaceSubtle` | `#F0F1EC` | Secondary groups and inactive controls |
| `ink` | `#19181C` | Primary text |
| `inkMuted` | `#66636B` | Supporting text |
| `primary` | `#66507A` | Metria identity and selected navigation |
| `primarySoft` | `#EEE8F2` | Quiet selected surfaces |
| `accent` | `#A8D94F` | Primary capture, progress, positive emphasis |
| `accentSoft` | `#E4F5BE` | Progress backgrounds |
| `verified` | `#287A5B` | Verified nutrition with icon and label |
| `estimated` | `#B87519` | Estimated nutrition with icon or pattern and label |
| `danger` | `#B3261E` | Destructive/error states only |
| `divider` | `#E4E3DF` | Quiet boundaries |

Never encode confidence solely with green or amber. A verified value uses a check icon and “Verified label”; an estimate uses a dotted/spark icon and “Estimate”.

### Typography

Use the Android system family initially for performance, localization, and predictable scaling. Hierarchy comes from size, weight, and whitespace.

| Style | Starting size | Purpose |
| --- | --- | --- |
| Display | 32 sp / semibold | Screen statement or greeting |
| Metric | 40 sp / semibold | One primary number |
| Heading | 24 sp / semibold | Screen and modal titles |
| Title | 18 sp / medium | Card titles |
| Body | 16 sp / regular | Primary readable copy |
| Supporting | 14 sp / regular | Secondary explanation |
| Label | 12 sp / medium | Short metadata and chart labels |

Avoid all-caps prose. Use tabular numerals for charts and aligned nutrition data when available.

### Spacing, shape, and elevation

- Spacing scale: 4, 8, 12, 16, 24, and 32 dp.
- Screen horizontal padding: 20 dp on phones.
- Card internal padding: 16 or 20 dp.
- Card gaps: 12 or 16 dp.
- Small-control radius: 12 dp.
- Card radius: 20 dp.
- Hero-card and sheet radius: 28 dp.
- Full radius is reserved for chips, icon circles, and primary capture controls.
- Prefer tonal separation and borders over shadows. Use one subtle elevation level for floating controls and transient sheets.

### Icons and imagery

- Use one coherent rounded Material icon family.
- Every icon-only action requires a content description and an understandable pressed state.
- Meal thumbnails use a consistent crop, rounded mask, and fixed size. They support recognition but never become the only way to identify a meal.
- The Metria assistant mark may animate when listening or thinking; it must remain still when idle and respect reduced-motion preferences.

## Component grammar

### Summary hero

One per major screen. It contains one statement, one principal metric or progress visualization, and at most one inline secondary action. It must not become a container for several forms.

### Metric card

Contains label, value, unit, small visual context, and optional trend. The value is visually dominant. Tapping opens the relevant detail; editing is not embedded in the card.

### Entry row

Contains thumbnail or icon, member-facing title, one concise value/summary, and optional confidence marker. The whole row opens detail. Destructive actions live inside the detail screen, not on the feed row.

### Dish card

Represents a meal or dish, not a raw model object. The collapsed card shows dish name, thumbnail, ingredient summary, portion, nutrition estimate, and confidence. “Review” opens a focused editor where ingredients can be renamed, regrouped, added, removed, and matched.

### Confidence marker

Compact visual treatment composed of icon/pattern, color, and short label. Detailed source and assumptions open on tap. Normal screens avoid paragraphs explaining provenance.

### Primary capture control

The persistent Metria action opens a compact action menu for voice, chat, meal photo, nutrition label, and manual entry. Listening and processing states replace the idle icon clearly; they do not navigate unexpectedly.

### Focused editor

A full-height sheet or destination with its own title, close/back action, content area, and persistent bottom action. Fields are grouped by the member's mental model. The keyboard resizes content, the focused field scrolls into view, and save remains reachable.

## Navigation target for prototyping

Prototype a bottom navigation with three stable destinations—**Today**, **Progress**, and **Privacy/Settings**—plus a visually central Metria action. The action opens assistant and capture choices in context. Assistant conversation may expand to a full destination but should not need a permanent fourth tab.

This navigation is a prototype decision, not an implementation mandate. Validate it in the approved mockups and on the physical phone before replacing the current navigation.

## Content rules

- Lead with member value: “Today”, “Your week”, “Review this meal”.
- Buttons use verbs: “Save meal”, “Add weight”, “Review ingredients”.
- Prefer one short sentence over headings followed by explanatory paragraphs.
- Errors state what happened and the next safe action.
- Do not describe a valid partial estimate as failure. Show what is included and let the member continue.
- Do not praise or criticize food, weight, calories, or adherence.
- Spanish and English strings must fit without relying on English-length assumptions.

## Responsive and system behavior

- Portrait phone is the first-release reference, but landscape, split screen, and rotation must preserve the active task.
- Content uses system bars and keyboard insets correctly.
- No fixed height may be required for localized text or scaled fonts unless content can scroll without hiding actions.
- Loading overlays belong to the operation that is loading. Navigation remains stable unless the action must block for privacy or integrity.

## Definition of visual success

A first-time tester should be able to answer these questions within five seconds of seeing a screen:

1. Where am I?
2. What is the most important information?
3. What is the main thing I can do?
4. Which numbers are estimates?
5. How do I get more detail without losing my place?
