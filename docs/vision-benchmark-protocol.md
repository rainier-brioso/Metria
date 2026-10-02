# Varied meal-photo baseline protocol

**Status:** protocol ready; no consented test set or baseline results yet.

## Purpose and boundary

Measure whether Metria produces useful, correctable meal drafts from *different*
photos without unacceptable latency or heat. Parser safety, visual correctness,
member effort, and device behavior are separate results. A manual-entry fallback
is safe recovery, not successful recognition. Keep novel meals, repeat meals
photographed again, and an exact reuse of the same photo in separate groups.

This is a member-led test on the target phone, not permission to read the vault,
copy photos, or inspect raw model output. Before starting, the member chooses
the test photos and agrees to the session. Photos remain on the phone; the
repository receives only non-identifying counts, outcome categories, and
technical measurements. Do not put meal names, image IDs linked to real files,
record values, model prose, device address, or screenshots in results. The
debug image-development capture is not needed for ordinary scoring; reading it
requires separate authorization for the active diagnostic run.

## First pilot: small enough to do, varied enough to find failures

- Select about eight novel-meal photos: include single-food and mixed meals,
  at least two with separate dishes or a drink, and ordinary differences in
  lighting or camera angle. Avoid faces, bystanders, documents, and location
  clues. The member knows what is visible and what is hidden/uncertain.
- Select about four **new photos** of previously confirmed dishes or meals.
  Do not score an identical image as a repeat-photo recognition success; the
  existing exact-image fallback is a separate diagnostic.
- Split runs across at least two sessions. In one session, analyse at least
  three distinct photos consecutively to expose warm-run slowdown. Start the
  other session with a phone that feels cool. Stop if the phone becomes
  uncomfortable to hold, Android reports severe thermal status, or the app
  enters cooldown; record the stop rather than pushing through it.
- Do not save a test draft merely to complete the benchmark. Save only a real
  meal the member wants in their record. Discard other drafts so the benchmark
  does not create fictitious history or teach correction memory from test data.

The pilot is diagnostic, not a statistical reliability claim. A later daily-use
gate needs a larger held-out set and pre-agreed numerical quality and thermal
thresholds. Do not calculate or advertise a meaningful p95 from this pilot.

## One-run scoring, before the next photo

The member compares the proposal with the photo and their knowledge of the
meal; the developer records only the categories below. A food hidden from the
photo is not a visual miss. Portion correctness is judged only where the member
has a reasonable reference; otherwise mark it uncertain.

1. Record the app's technical outcome: ready, partial, rejected/manual
   fallback, runtime failure, or cancellation, plus duration and a fixed
   rejection category when available. `LocalAnalysisAudit` already retains
   these without photo or model text; Developer diagnostics shows recent runs.
2. Assign exactly one visual grade: **correct draft** (visible principal foods
   right, no unsupported additions); **useful partial** (at least one right
   principal food and straightforward edits, with missing/uncertain foods
   disclosed); **valid but wrong** (schema-valid yet a principal identity or
   grouping is wrong enough to require substantial correction); or **no usable
   draft** (rejection, crash, cancellation, or no correct principal food).
   Report schema rejection separately from visually wrong valid output.
3. Count unsupported added foods, visible principal foods missed, and review
   edits by type: name, add/remove, regroup, portion, and nutrition match.
   Note whether proposed portion ranges were usable, uncertain, or implausible.
   Record remembered-dish suggestions as accepted, rejected, or absent.
4. Mark whether the member could complete or safely discard review, whether
   any late result reappeared after cancellation, and whether existing records
   remained available. Do not infer a successful Save from a draft screen.

## Device measurements

Use the exact current ADB device from `adb devices -l`; never persist its
identity. The technical audit supplies per-run duration/outcome. For the warm
sequence, record battery level and Android thermal status before and after,
plus app memory and frame/jank data when useful, following `docs/TESTING.md`.
Note the runtime/model/delegates and whether a run was first-after-launch or
consecutive. Battery percentage over a few runs is coarse evidence, not an
energy measurement. Never take a screen dump or read raw capture merely to
collect these metrics. Biometric unlock and visual grading remain member
actions.

## Reporting and decision

For novel and repeat-new-photo groups separately, report the denominator and
counts in each visual grade, technical rejection/failure rate, false additions,
missed principal foods, median and maximum latency, editing burden, and any
cooldown, heat, cancellation, or vault issue. Show the small sample size. Exact
same-photo results, if tested, have their own line and cannot improve the
repeat-new-photo score.

Only after seeing the pilot should the member and developer agree on a larger
held-out set and numerical pass/fail thresholds. Do not change correction
ranking or claim several-meals-per-day reliability based on a few successful
photos. If a run fails, use the opt-in raw capture only with authorization for
that specific diagnosis, then clear it as described in `docs/TESTING.md`.
