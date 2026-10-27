# Drink portions - 2026-10-22

Status: implementation and independent review complete; phone acceptance pending.
This extension preserves the
uncommitted explicit meal-type handoff on pushed voice baseline `5919898`.

## Decision and sources

Use the member's preferred amount/unit in Review and saved-meal editing, while
retaining the existing canonical gram mass for nutrition. A selected drink may
offer grams, mL with a supported volume-to-mass basis, and the catalog's measured
servings. The selected food, not the language model, supplies the conversion.

[USDA Foundation documentation](https://fdc.nal.usda.gov/Foundation_Foods_Documentation/)
describes scaling per-100-g nutrients using a serving's measured gram weight.
The current Foundation asset already retains portion amount, measure, modifier
and mass, but the adapter discarded most of that detail. A bare cup is useful
as its source-defined serving; it alone does not establish mL for this extension.

Bundle official SR Legacy brewed coffee (171890) and espresso (171891) as a
small attributed supplement. These contain explicit fluid-ounce portion wording.
The [NIST conversion table](https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b9)
defines U.S. fluid-ounce conversion to mL. The reproducible beverage importer
derives volume only from explicit corresponding USDA wording and records that
basis. No generic liquid density, nutrition value or household cup is invented.
Importer details and attribution: `food-catalog-development.md`.

## Data and calculation contract

- Gram-only records remain valid with no new metadata. Existing nutrient values
  and previously saved meals are not retroactively recalculated or relabelled.
- Optional encrypted item metadata retains the chosen amount, mL/serving unit,
  readable label, source portion, gram conversion and catalog food/release.
  Saved dishes and recovery carry the same representation.
- Canonical grams are computed once with documented integer rounding, checked
  against the existing 1..2,000 g envelope, never clamped. Nutrition continues
  through the existing deterministic per-100-g calculation.
- Metadata must be finite, bounded, supported and consistent with canonical
  mass and its source identity. Invalid recovery metadata is not conversion
  authority. The model cannot create this capability.
- Changing food/name or returning to direct gram entry invalidates an old
  conversion. Scaling and unit switches must keep displayed amount and canonical
  mass consistent. A new food cannot inherit another food's density.
- Missing volume data explains why mL is unavailable and leaves grams/known
  servings usable. No missing conversion is treated as water density or zero
  nutrition. Catalog matches remain reviewable estimates.

## Interaction and states

Use the existing focused food editor, shared with saved-meal editing. Units are
secondary choices near the amount, with a defined serving label. Apply remains
the focused editor action; Save remains the meal-level action. Reuse theme and
Material semantics, wrapping controls and at least 48 dp targets.

Initial/model-estimated grams remain estimates. Switching units preserves the
current portion; typing a new amount updates its canonical mass. Invalid entry
remains visible and blocks Apply/Save rather than silently saving an old value.
Cancel restores the prior amount/unit. Recomposition/configuration restoration
and unsaved-change snapshots include the complete quantity choice. Existing
keyboard insets, scrolling, match selection and explicit Save remain required.

## Verification and limits

Automated checks must cover non-water conversion factors, servings/fractions,
rounding/limits, invalid/nonfinite values, absent conversion, changed source,
legacy and new codec round trips, malformed metadata, saved-dish propagation,
unit switching/scaling and restored/dirty editor state. Full Android test/build
gate and independent review are required before installation.

Physical checks are separate: coffee mL, a defined serving, changing food/unit,
invalid input recovery, keyboard access, unsaved cancellation and saved-display
round trip. Review a drink with no mL basis to confirm the fallback is clear.
No physical acceptance is claimed yet. Wider accessibility, rotation and catalog
coverage remain explicit gaps until exercised. No dependency, runtime network
lookup, automatic record mutation or commit/push is authorized by this document.

### Automated evidence and next gate

On 2026-10-22, all 682 Android unit tests pass with zero failures/errors/skips.
The complete debug/release assembly and release vital lint pass; the three
beverage-importer tests and diff check also pass. Independent review found and
verified fixes for catalog-release rebinding and selected-unit scaling limits;
no blockers remain in the reviewed delta. These tests do not establish actual
Compose interactions, Android encrypted-vault round trips or recovery UI behavior.

Debug APK SHA-256:
`f6f55204921f959e1ff38f60031aadcb34939b3ab9a9c7ab62a67821936dd592`.
The verified APK was installed with `adb install -r` (`Success`) and cold-launched
(`Status: ok`) on Samsung SM-S928B / Android 16 after wireless pairing. Existing
app storage was preserved; member biometric unlock remains manual. Physical
acceptance is pending: coffee mL/serving Review, quantity editing and cancellation.
Saved-meal persistence, unsupported old-source warning, keyboard/font scaling,
rotation, TalkBack and recovery remain separate acceptance checks.

This slice exposes units in Review and saved-meal editing. Chat/voice contextual
portion corrections still accept grams; spoken mL/serving edits are not claimed.

Member reports coffee quantity editing worked and they changed a gram amount to
20 mL. They also described coffee as saved directly without a draft. Clarification
is pending on whether this means direct editable Review or a record inserted
before Save; do not mark explicit-save or persisted-unit acceptance passed from
this report. Source inspection still places reviewed-meal persistence behind the
Review Save callback. The available content-free trace does not establish a save.

Member subsequently confirms a new coffee entry exists without tapping Save,
after saying "Ah, had a coffee for breakfast" and receiving a recorded-meal
acknowledgement. Treat this as an unresolved write-path report, not a proven
hallucination or accepted auto-save feature. Independent code discovery finds no
Chat write tool or automatic Review Save invocation. To reconcile observation
with code, add opt-in debug events at successful vault meal insert/edit boundaries.
They contain only fixed reviewed/direct-entry/edit categories, no record ID,
food, quantity, member text or model output; release remains disabled. Observer
failure cannot change a successful write. Full gate and one controlled retry are
required before conclusions. No automatic-save UX decision is made by this probe.

The diagnostic delta passes independent review, all 684 tests (zero failures,
errors/skips), debug/release assembly and release vital lint. Only successful
insert/edit operations are observed; deletion/recovery is not claimed. Review
origin groups manual, photo, text and Chat Review; it does not identify which
screen invoked it. Debug APK SHA-256:
`2a1a101a083d02e3b96349eba3d84d4fd0849ac3f92f4d1e6cd20ce81fd7fb3b`.
The phone disconnected and advertised endpoints refused reconnection; this new
diagnostic APK is not yet installed. Existing coffee data remains untouched.

Member subsequently says the apparent auto-save may have happened another way
and explicitly asks to set the investigation aside. Stop reproduction of that
report; its cause is unconfirmed, and no auto-save feature is established. Resume
drink serving/unit acceptance. The fixed diagnostic events remain in the reviewed
build, without changing the explicit Review/Save behavior.

The 684-test APK was subsequently installed in place (`Success`) and cold-launched
(`Status: ok`) on the same Samsung SM-S928B / Android 16. Existing app storage is
preserved; member unlock remains manual. Next physical check: defined coffee
serving, matching estimate changes, and quantity restoration after leaving Review
without saving. No new physical pass or persisted-unit round trip is claimed yet.

## Remembered drink portions and readable quantities - 2026-10-22

Member requests reuse of their confirmed drink unit/portion and reports excessive
decimals when switching to a cup. Distinguish saved-record restoration (already
implemented, still needs a focused physical round trip) from defaults for a new
drink (previously absent). Derive the default from existing encrypted confirmed
meals rather than introducing a separate profile or schema. A new single-drink
text proposal may reuse the latest same-name/same-catalog-food quantity if the
conversion rebinds unchanged. Host source text must specify no quantity/size;
photos/manual entry, explicit portion/food overrides, mixed proposals and saved
dish prefills retain their own authority. A later gram-only matching save retires
the previous unit default; deleting the source naturally removes that evidence.
Discarded drafts never teach a preference. A brief Review note identifies reuse;
the default remains editable and nothing saves automatically.

Unit switching retains exact conversion and canonical mass. Display mL to one
decimal and servings to two (extra places only to avoid rounding a positive
amount to zero), marking rounded values as approximate. Until actual text entry,
the snapshot rather than rounded presentation controls nutrition; typing a value
intentionally establishes a new amount. Programmatic scaling/restoration preserves
this distinction. No nutrition value is silently changed to prettify a field.

Independent review/full gate and phone acceptance of these new behaviors are
pending. Dedicated checks: stored unit/amount survives reopening the saved meal;
new unspecified coffee uses that default; explicit amounts do not; cup display
is concise without changing nutrition; discard/invalid-input/keyboard still work.

Final follow-up gate passes 691 tests (zero failures/errors/skips), debug/release
assembly, release vital lint and diff check. Re-review finds no blockers after
covering colloquial explicit quantities such as couple/pair. The missing import
and exact-tiny-display test assertion discovered during the first gate were fixed;
the complete final gate was rerun on the frozen source. Debug APK SHA-256:
`981341e8d6327c49b19705480575f11c429855d9070e636adefb5f9e1a6b27c7`.
Physical acceptance of saved-record retention/default reuse remains pending.

Verified 691-test APK installed in place (`Success`) and cold-launched (`Status:
ok`) on Samsung SM-S928B / Android 16, preserving existing app storage. Member
unlock and existing-coffee Save/reopen, new-coffee default and readable-cup checks
requested. No new record written by automation, no private capture, commit or push.

### Coffee did not reach Review

Member reports a conversational coffee acknowledgement with no draft on the
691-test build. Content-free trace shows accepted MiniLM saved-dish probing, then
the no-match normal-agent handoff and a delivered Gemma reply without drafting.
The new defaults never ran because there was no Review proposal. Add a narrow
host route for closed literal drink-consumption frames only after that no-match
handoff, with no required read, active draft, selected meal or confirmation scope.
It invokes the existing structured unsaved-draft task with unchanged member text;
it cannot choose a catalog match/amount or write a record. Ask-first, mixed food,
negated, hypothetical, question and unknown wording retain normal intent handling.
Exact current wording requested from member. Full gate, review and phone retry
pending; no successful default-reuse acceptance claimed from the failed turn.

The no-match drink-route gate passes all 694 tests (zero failures/errors/skips),
debug/release assembly and release vital lint. Frozen debug APK SHA-256:
`21a248d16cf89bd73951159d83551fce53209b163e9916f333e86569cbc4e60a`.
Independent review and installation/physical retry remain pending at this point.

Independent review finds no lifecycle/lease or priority blockers. Member confirms
the failed question was exactly "I had a coffee for breakfast", covered by the
new closed frame. Earlier "Ah, had ..." wording is not covered and remains normal
intent handling; do not broaden this gate from the earlier report. Installation
of the reviewed 694-test build is underway under the active test-cycle approval.

The 694-test APK subsequently installed in place (`Success`) and cold-launched
(`Status: ok`) on Samsung SM-S928B / Android 16, with storage preserved. Exact
coffee draft/retained portion retry requested; member unlock remains manual.
No new physical acceptance or record Save claimed yet.

Member reports the exact coffee retry worked: draft offered and saved-unit/portion
Review check passes on the 694-test Samsung build. Safe retained events confirm
MiniLM saved-dish probe, no-match continuation, `host_drink_draft` and delivered
reply. They do not independently prove portion values; that is member-observed
acceptance. No persisted-meal event appears in the retained retry interval; the
test requested Discard. Explicit override, ask-first and broader saved/recovery/
accessibility gates remain separate; the earlier failed turn is superseded only
for this exact successful coffee wording.

Member confirms the focused explicit override: with an unsaved coffee draft,
"150 g" overrides the remembered quantity in Review. Safe metadata corroborates
`portion_applied`; no amounts or record payloads were inspected. This named
member-observed override gate passes on the installed 694-test build. It does not
establish natural-language mL parsing or all explicit quantities. Next focused
regression: ask-first/decline, then checkpoint review with wider gaps documented.

Member subsequently confirms the focused coffee ask-first/decline check worked:
"I had a coffee for breakfast. Please ask me before preparing a draft." asks
first, and "No thanks" produces a clear acknowledgement with no draft. This is
member-observed acceptance on the installed 694-test Samsung build; no additional
device capture was needed. Named coffee drafting, remembered quantity, explicit
gram override and ask-first/decline checks now pass. Broader saved-record round
trip, concise cup display, recovery, keyboard/font scaling and accessibility
checks remain open as recorded above. No source change or new build was needed
for this acceptance update; commit and push still require explicit approval.

On 2026-10-24 the member explicitly approved committing and pushing this reviewed
checkpoint. Acceptance scope and the broader open physical gates above remain
unchanged.

## Chat and spoken drink quantities - 2026-10-24

Member approves the next slice: explicit drink amounts such as "I had 200 mL of
coffee" and contextual corrections such as "Make that half a cup". Ordinary
speech already enters the same Chat text handler; do not introduce a separate
audio conversion path. Support bounded English mL and measured-serving expressions
with finite positive amounts. Whole-gram corrections keep their existing behavior.
No unrestricted natural-language quantity claim is made by this slice.

The host parses the original member text, binds one complete drink to one
unambiguous packaged food, and resolves the requested unit through
`DrinkPortionPolicy`. A cup is a source-defined serving, not an assumed generic
volume. Different cup sizes require clarification. Missing mL evidence cannot
fall back to water density. Preserve the precise source-bound snapshot beside the
canonical grams so Review and deterministic nutrition agree. An explicit current
quantity wins over a remembered default; grams or a food replacement invalidate
the previous drink conversion. Model output cannot author a conversion factor.

Idle/no draft uses normal Chat handling. An eligible initial drink report follows
the existing draft task and its loading/cancellation behavior. A supported live
correction updates the unsaved draft and acknowledges its unit; invalid or
ambiguous corrections keep the previous draft with an explanation. Unrelated
requests follow ordinary handling. Stop, lock, new work and stale voice/context
proofs cannot apply a late correction. Reopening Review retains the accepted unit
in the same live session; history alone cannot recreate editing authority. Only
explicit Save teaches the next default or changes records. No new controls or
screen layout are introduced.

Acceptance plan: automated parser/conversion, ambiguity/missing-source, current
identity, override precedence and Review-restoration tests; complete Android gate
and independent review. Then install in place and check an initial coffee mL
amount, repeated mL/cup/gram corrections, unsupported/invalid quantity retention
and a spoken correction on Samsung. Physical gates remain pending until observed;
the earlier checkpoint's wider UI/recovery/accessibility gaps remain open.

Implementation/review evidence: new `ChatDrinkPortionPolicy` parses bounded
positive quantities from original text. mL requires explicit volume evidence;
servings match the primary source measure, excluding parenthetical size words.
Cup requests require one unambiguous source with a one-cup basis; other measured
servings account for the numeric source basis. Plain ambiguous ounces are rejected.
Optional host-only proposal quantity metadata remains outside model JSON. The
request-bound override retains the validated snapshot across repeated Review,
Skip and unchanged-draft replies. Explicit grams clear it; successful food rename
retires it, and a new unit correction after a rename requires Review's food match.
An unresolved initial quantity has a concise Review warning and cannot silently
be treated as a successfully applied conversion.

Independent review found no blocking conversion/session issue. A trailing food
suffix such as "Make that 200 mL of coffee" is outside the correction grammar and
gets unchanged-draft feedback; use "Make that 200 mL" for this slice. Root review
fixed gram corrections on fixed-range drink estimates and singular glass parsing,
with regressions and independent follow-up review. Initial compile's missing
import was repaired. Final frozen-source gate:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`
passes 710 tests, zero failures/errors/skips, both APK builds and release vital
lint. Diff check passes. Debug APK SHA-256:
`49924366d169af0b9062f5e94037d7229ba2f7f166ee16b2110cad2a4c84ce8d`.
Samsung reconnected; authorized in-place installation is underway. All new phone
checks remain pending. No member record was saved, private screen captured, new
dependency added, commit made or push performed for this follow-up.

The verified 710-test APK installed in place (`Success`) and cold-launched
(`Status: ok`) on Samsung SM-S928B / Android 16, preserving existing app storage.
Member unlock and initial 200 mL coffee / half-cup Review acceptance requested.
No new phone interaction gate is passed merely by installation.

Member confirms both named typed checks on the installed 710-test Samsung build:
initial coffee Review shows 200 mL and Breakfast; after leaving without saving,
"Make that half a cup" shows 0.5 of the listed cup serving in Review. This is
member-observed amount/type acceptance. Safe retained trace confirms MiniLM saved
dish probing, no-match continuation, `host_drink_draft` and completed Chat reply;
it does not independently establish the displayed amounts, and no separate
`portion_applied` event appears in the inspected tail. No private text/record or
screen captured. Spoken correction, gram switching/invalid retention and wider
physical gates remain open. Next: a spoken mL correction and repeated Review.

Member reports spoken quantity/Review check failed and authorizes screen
inspection. Content-free trace shows accepted transcription, a native portion
rejection, then a separate new drink draft. Android CLI flat layout (no-idle,
because status-bar animation prevented idle) reveals two consumption statements
with 2000 mL, not the requested 250 mL correction. No screenshot or raw hierarchy
file was saved in the repository. The spoken original amount is not yet known;
ask member whether this was recognition error or intentional wording before
changing quantity authority. Spoken correction acceptance remains open; do not
infer a converter defect or silently rescale a recognized number.

Member says they may have spoken "thousand" by mistake. This does not establish
an ASR defect or a successful spoken correction. Retry with a fresh typed 200 mL
coffee draft and spoken "Make that one hundred milliliters", checking the actual
transcribed amount and repeated Review before drawing conclusions.
