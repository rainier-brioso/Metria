# Shared assistant recognition and typo tolerance

## Chat correction revision (approved 2026-10-15)

The member requested replacing the one-word restriction after a natural question
contained both `recored` and `tot`. This revision supersedes the single-token
candidate limit below; prior phone results do not validate this revision.

Keep exact recognition first. A shared bounded planner may correct up to three
words and evaluate at most 64 complete combinations, failing closed on overflow.
Local alternatives use existing conservative edits plus contextual prefixes of
at least three letters, such as `tot` for `total`. This is a vocabulary/grammar
rule, not a list of misspelled aliases. Supported read-command words participate
only when the complete existing read grammar validates.

Validate every candidate against the original host requirement, frozen date and
existing read eligibility. Equivalent spellings must select the same complete
tool/metric/date operation; distinct valid interpretations require a concise
host-authored clarification. Known dates, numbers, protected real words, write
actions, other people and unsupported filters retain their meaning. Selected
records and fuzzy inherited follow-ups remain excluded. Saved dishes, drafts,
goals, overview and navigation are not expanded.

Broader spelling alternatives still require a compatible Android suggestion
marked as a typo and not a dictionary word. Up to three locally identified words
may be checked individually; never send the full question, history or records.
All attempts share one 400 ms monotonic deadline and current-request/cancellation
checks. Unavailable, rejected or expired service results retain normal handling.
No provider locale, dependency or model threshold change is included.

Original Chat and Gemma text stays unchanged. Validated candidates only constrain
the existing read and classifier input; fresh lookup, deterministic replies,
factual audits and save review remain. Privacy details disclose the maximum of
three isolated words. Monitoring must not store those words or suggestions.
Release MiniLM activation remains a separate existing boundary.

Acceptance: two/three corrections and abbreviations; four-word/combination limits;
equivalent versus ambiguous candidates; protected meaning and conflicting host,
date or selected context; provider flags, shared deadline and cancellation;
independent review and full gate before installation. Phone checks separately
cover local multi-word routing, genuine provider correction if available,
ordinary questions and ambiguity. Existing reply-ID mismatch remains open; this
change must not bypass stale-result checks.

Implemented and independently reviewed with no blocking findings. Tests cover
the exact `recored ... tot` question, three local changes, equivalent `cal`
prefixes, two provider-backed changes, command-word candidates, protected meaning,
four-change rejection, original host/date conflicts, migration/router scope,
decreasing shared budget and late-result rejection. The initial gate caught a
new calorie-case assertion incorrectly expecting protein; corrected the test's
expected metric without changing production behavior. Full gate then passed
562 tests, zero failures/errors/skips, debug/release APK assembly and release
vital lint; diff check clean. Command:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`.
Debug APK SHA-256:
`0af2931d243262fbb9e4b06a3d1031063d9fa981061e1fdb007f33ee536315fb`.
Authorized in-place installation returned Success on Samsung SM-S928B / Android
16 and cold launch returned `Status: ok`, preserving existing storage. Physical routing, provider
behavior and disclosure/accessibility acceptance remain separate open gates.

Named 562-test physical result: member confirms both copied two-/three-error
protein-total questions appear and match. Each safe turn selects MiniLM's exact
today-nutrition read, requires the fresh tool, passes its factual audit and is
appended to Chat. No system spelling or Gemma selection occurs in either turn.
This accepts local multi-word correction plus abbreviation for those questions,
not provider correction or all natural wording. Other stated gates remain open.

Full checkpoint review (2026-10-15): independent recognition/authority and
Android runtime/privacy reviews found no blocking defect in the complete pending
change. No further implementation edit was needed; 562-test/build evidence and
accepted artifact remain applicable. Direct Android session callback/cleanup
tests and a real usable provider correction are still missing evidence. Member
explicitly deferred the intermittent missing-answer investigation and authorized
committing/pushing this reviewed checkpoint with those gaps recorded. Gboard
remains selected by member preference, not by application startup behavior.

## Production Android spelling extension (approved 2026-10-14)

The member selected Android's Spell Checker Framework, explicitly requesting
production integration in both build variants, with behavior assessed on the
Samsung during the existing in-app test cycle. This supersedes the proposed
debug-only/Samsung-only pilot scope; it is implementation authorization, not
physical or release acceptance. Existing 542-test results below precede this
extension and do not validate it. The extension has now passed independent
review and the 550-test/debug/release-assembly gate and is installed; selected
provider phone acceptance is recorded separately below.

Use the selected system provider and explicit English suggestions. Respect
disabled spelling; do not change Android settings, install a checker or download
a dictionary. Missing service/language, errors, timeout and cancellation preserve
the existing assistant fallback. No production dependency is needed.

Recognition order stays exact, compositional and existing bounded typo fallback
first. Only if those miss may a pure local candidate policy identify a single
token/replacement that satisfies one existing supported read. Bound the token,
edit distance, request size and vocabulary; preserve the rest of the request.
The vocabulary represents supported routing terms, not a replacement English
spell-checking dictionary. Protected real words, known dates, numbers and write
verbs are not rewritten. Selected records, inherited follow-ups, saved-dish
queries, drafts, navigation, goal comparisons and overview corrections are
outside this new extension.

Pass only the identified possible misspelled word to the OS service, never a
whole Chat question, record, history, dish query or photo. Suggestions are
untrusted: require a typo result, reject a dictionary-word result, require the
unique eligible matching suggestion and validate the entire corrected candidate
again against the original host requirement and frozen date. A date-word typo
such as `tday` may become `today` only through this provider-confirmed, unique
complete-read path. The existing local typo layer still does not guess dates.
Never alter a correctly spelled or explicitly unsupported date to obtain a hit.

The original question remains in Chat and in Gemma's fallback request. Only the
transient validated recognition candidate can affect the required read and
MiniLM input. No threshold, model artifact, direct write or factual audit changes.
The existing release MiniLM abstainer remains separate; production spelling can
still bind Gemma to a corrected validated read in release.

Create a short-lived session for one eligible request, asynchronously on a valid
Android callback thread. Wait only on the existing assistant worker and impose a
bounded deadline. Always close/cancel the session, ignore late callbacks and
queued work after expiry, and preserve interruption. Check request/engine identity
again before using results. Do not cache or separately persist words/suggestions.
Monitoring adds only fixed spelling outcome categories under existing opt-in,
debug-only, content-free bounds; no provider identifier, token or suggestion.

The framework delegates text to the selected service; it is not itself an
offline/privacy guarantee about that provider. The revised M1 boundary and
Privacy/Local assistant details disclose this narrow OS-service processing.
Testing an airplane-mode suggestion alone does not establish absence of network
use or later provider retention. Provider assurance remains a named open gate.

Primary API references checked:

- [Framework and service boundary](https://developer.android.com/develop/ui/views/touch-and-input/spell-checker-framework)
- [TextServicesManager: nullable sessions, locale and selected service](https://developer.android.com/reference/android/view/textservice/TextServicesManager)
- [SpellCheckerSession: callbacks, cancellation and close](https://developer.android.com/reference/android/view/textservice/SpellCheckerSession)

Acceptance: independent concurrency/privacy/authority review; pure candidate and
provider-fake tests for ambiguity, dictionary words, scope/host/date conflicts,
timeouts/cancellation and no-query cases; complete Android unit/debug/release
gate; authorized in-place install; named Samsung spelling and original-phrase
checks with safe trace correlation. Verify disclosure scrolling/font scaling and
Stop/lock/late callback behavior separately. No physical pass is implied yet.

Implementation and automated evidence: `AndroidEnglishSpellChecker` is in main
source and uses a 400 ms bounded worker wait, explicit English and a one-shot
sentence-suggestion session for the isolated token. It checks real segment
offset/length, cookie/sequence and bounded suggestion metadata; callback parse
failure fails closed. Creation, cancellation and close use the main callback
lane. Cancel and close are attempted independently, late callbacks are ignored,
and request/engine identity is checked before routing. API 31+ checks system
enablement and requests English directly. The legacy path respects enabled
language settings and can abstain if the selected subtype is not English; it
never silently uses a different language.

The pure policy validates candidates without invoking the existing manual typo
retry a second time. Explicit new questions remain eligible after prior reads,
but fuzzy short follow-ups cannot inherit their scope. The legacy write guard
uses only pure candidate eligibility so read questions containing `log`/`record`
can reach the provider; mixed/imperative writes retain their normal path.
Unrelated alternative spellings do not create ambiguity when only one suggestion
can satisfy the full local contract. No full message is submitted to the service.

Independent review completed with no remaining material blocker. Parent review
corrected fabricated segment metadata, callback exception handling, cleanup when
cancel throws, duplicate correction, scope/migration interactions and test
expectations before the final gate. All 550 tests passed with zero failures,
errors or skips. Both debug and release APK assembly, including release vital
lint, passed using:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`.
`git diff --check` passed. Debug APK SHA-256:
`6502150199f141d502c409f49f57bd33896fef392c5dab1c418b53a1622c654b`.
No production dependency, signing key or model activation changed.

Automated tests use a fake provider and cover one-token disclosure, unique
correction, host/selected-context conflicts, offsets/flags, unrelated suggestions,
two typos, missing/disabled/timed-out/failing providers, cancellation, migration
and closed trace categories. They do not exercise Android's actual session
implementation, Samsung callbacks or provider network/retention behavior. Those
remain acceptance gaps alongside Stop/lock/late-callback scheduling, disclosure
scrolling/font/TalkBack and pre-31 behavior. Production implementation does not
close production release acceptance.

Authorized `adb install -r` of the checksum-matched debug APK returned Success
on Samsung SM-S928B / Android 16. The debug package contains the same main-source
production adapter and also enables the existing opt-in metadata diagnostics.
The release APK was built but not installed over the signed debug app. Force-stop
and cold launch returned `Status: ok`; existing vault/model storage was preserved.
Next named check is the correctly spelled protein-total question followed by the
same request ending in `tday`, in the same Chat. Compare both answers and inspect
fixed `spell_check`, route and audit events. No provider success, offline guarantee
or repaired handoff acceptance is claimed before that observed check.

Named 550-test Samsung result: the member confirmed both protein-total requests
answered and matched the saved records. Content-free events show one accepted
MiniLM exact-day nutrition read (positive 0.8988, competing 0.7497, fallback
0.4739) and one spelling attempt rejected with `no_suggestions`, followed by a
native Gemma nutrition-summary tool, successful continuation and accepted factual
audit. Both final answers passed their audits. No raw prompt/reply or values
were inspected. Request order cannot be independently tied to question wording
because monitoring intentionally omits text.

This closes the named answer/comparison and observed Gemma nutrition-handoff
check on this build. It does not prove the original stall's cause or general
ordering coverage. Samsung did not provide a usable correction for the observed
spelling attempt, so provider correction/typo acceleration is not accepted yet.
`no_suggestions` can represent absent suggestions or results that failed strict
metadata/flag/matching checks; it must not be presented as proof the provider
returned zero alternatives. Next bounded check is a copied ordinary routing-word
typo (`protteen`) in the same recorded-protein question, with safe event inspection.
No code change or repeat installation was made for this acceptance step.

The member also confirmed a matching answer for the copied `protteen` test.
Safe events again show `spell_check / rejected / no_suggestions`, native Gemma
selection and an accepted reply audit; no fresh record-tool event appears in
that turn's retained events. This confirms a visible accepted fallback reply,
not usable spelling correction or independently proven fresh-record lookup.

Follow-up API inspection found a concrete adapter configuration defect: the
API 31+ session builder defaults `supportedAttributes` to zero, while Metria's
policy requires a typo flag before considering a suggestion. The legacy overload
sets its supported flags internally; the new overload requires an explicit mask.
Added `IN_THE_DICTIONARY | LOOKS_LIKE_TYPO | HAS_RECOMMENDED_SUGGESTIONS` to the
API 31+ builder, retaining all matching/scope/flag/privacy checks. No provider
behavior was relaxed. The default and required setter are verified by the
[Android builder contract](https://developer.android.com/reference/android/view/textservice/SpellCheckerSession.SpellCheckerSessionParams.Builder)
and [AOSP compatibility tests](https://android.googlesource.com/platform/cts/+/9227b7202d3eb41fdea5ec9adc60fc8bcf4cdae3/tests/inputmethod/src/android/view/inputmethod/cts/SpellCheckerTest.kt).
Independent review found no blocker. This defect is established from the API
contract, but it does not prove which unusable-result subcondition occurred in
the two original phone callbacks. Full gate passed all 550 tests with zero
failures/errors/skips, debug/release assembly and release vital lint. Diff check
passed. Configuration-repair debug APK SHA-256:
`afbea20ad03eb2f9c8d8d50e981d1de8949dce9feb0bf9dc440b3e9e663dfd9e`.
Authorized `install -r` returned Success on Samsung SM-S928B / Android 16;
force-stop and cold launch returned `Status: ok`, preserving existing app storage.
Member unlock and a new named provider check remain pending for this configuration
repair. No guard was loosened to make the phone test pass.

After the attribute repair, the copied `protteen` request still selected Gemma;
fixed events again show `no_suggestions` before MiniLM inference and an accepted
Gemma reply audit without a fresh record-tool event. The member then tested
`prottein`, which selected MiniLM exact-day nutrition and passed its audit with
the same scores as the correctly spelled question; no system-spelling event was
recorded. This is the existing one-edit deletion path, not proof Android spelling
worked. `protteen` requires the separate provider-confirmed path.

Filtered system configuration shows Samsung advertises an English spell-checker
subtype. That metadata does not establish available suggestions or dictionary
behavior. The broad rejection category cannot distinguish empty results from
guarded metadata/flags, so a diagnostic refinement now separates fixed reasons:
empty response, segment mismatch, request-cookie/sequence mismatch, dictionary
word, unmarked typo and no compatible suggestion. Existing acceptance guards and
the token-only boundary remain unchanged. Tests cover policy distinctions and
trace restart/allowlist/privacy behavior. No raw words/suggestions or numeric
provider payload is added to telemetry. Review found no blocker. The complete
gate passed 552 tests, zero failures/errors/skips, debug/release assembly and
release vital lint; diff check passed. Diagnostic-update debug APK SHA-256:
`7a2be51902a344436e0bb5ddc4b04e3a3e9315e8df2df326ceec4d35be917be9`.
In-place installation and a single repeated Chat check follow; provider correction
remains unverified, and no response-matching safeguard was relaxed.

Continuation on 2026-10-15: the diagnostic APK had installed successfully before
the interrupted turn. After the member reconnected wireless debugging, the
installed binary checksum was verified as
`7a2be51902a344436e0bb5ddc4b04e3a3e9315e8df2df326ceec4d35be917be9`.
No new spelling test was present in the inspected metadata tail. Fresh force-stop
and cold launch returned `Status: ok` on the same Samsung SM-S928B / Android 16.
No reinstall, code change, private-content inspection or biometric interaction
was needed. Next: member unlock and one copied `protteen` request, then inspect
the detailed fixed rejection stage. Existing records/models remain preserved.

The repeated 552-test check reports `spell_check / rejected / dictionary_word`:
Samsung passed request/segment validation but marked the isolated token as a
dictionary word. Metria retained its dictionary-word safeguard. This does not
establish whether suggestions existed or why the provider considered it valid.
Gemma was selected and a reply audit was accepted with no fresh record tool;
the member reports no answer bubble, so this is a failed visible-response check,
not successful fallback acceptance. A separate Chat-delivery investigation is
open. No raw records, suggestions or reply contents were inspected.

The member explicitly approved a temporary comparison using the already
installed Gboard spell checker. The selected system spell-checker component was
changed and read back successfully; the keyboard selection, app binary and
records were not changed. Original Samsung selection is retained transiently
for restoration after comparison. This authorized test is an exception to the
application policy of never changing Android settings itself. Repeat the same
copied question and inspect fixed metadata; provider usefulness and offline
assurance remain open. No new build is required.

Gboard comparison result on the same APK: member reports the answer appears and
matches. Fixed events show `spell_check / failed / timeout` at the 400 ms bound,
then native Gemma and accepted reply audit without a fresh record-tool event.
This is visible fallback acceptance by the member, not provider correction or
MiniLM acceleration. One cold comparison cannot establish steady-state provider
performance. Original Samsung spell-checker selection was restored and read
back successfully; no keyboard setting was changed. The earlier missing bubble
remains an intermittent delivery issue under investigation. Do not increase the
deadline or relax correction guards based on this result alone.

Next bounded diagnostic slice adds opt-in `reply_delivery` metadata when Chat
observes a lifecycle reply and either appends it to the transcript or skips the
existing current/handled-request gate. Fixed reasons are `lifecycle_ready`,
`chat_consumed`, `active_request_mismatch` and `already_handled`. Request/stage
pairs are deduplicated in bounded memory; existing trace retention and off-main
writer apply. `chat_consumed` proves transcript assignment, not visible pixels,
scroll position or encrypted persistence. No reply text is included and no
delivery policy is changed. The missing-bubble cause remains unproven until a
reproduction correlates these stages. Review/build/installation evidence follows.

Delivery slice passed independent review and the complete
`:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon`
gate: 554 tests, zero failures/errors/skips, both APKs and release vital lint.
Diff check passed. Debug APK SHA-256:
`86768281bd669add5d11da6bf7dcb0524a1b726bc8f70f5ca47a4a944e93faaf`.
No correction guard, deadline or reply-consumption policy changed. Physical
delivery acceptance remains open; installation proceeds under the active cycle.

Further 554-test phone evidence and read-only review (2026-10-15): with Gboard
selected, two spelling callbacks returned `dictionary_word`. One audited Gemma
reply was observed but not appended because the active request ID mismatched;
the next reply was appended. This separates correction rejection from delivery.
Vault synchronization can clear active identity when persisted and live
transcripts differ (including stale/debounced or truncated snapshots), but Stop,
reset and new requests can also invalidate it. The exact invalidation source is
not in current telemetry; no lifecycle fix is proved. Do not bypass identity
checks. Regionless `Locale.ENGLISH` with settings-reference disabled is another
untested provider variable; an English regional subtype comparison may be useful,
but there is no evidence yet that locale caused dictionary-word results. No
locale/deadline/guard change was made from this hypothesis.

Decision date: 2026-10-13. Status: implemented and reviewed; full 531-test gate
passed. Named typo, original-question and rejected-message/reopen phone checks
passed. Broader physical and accessibility gates remain open.

The next member-approved compositional read-phrasing slice passed independent
review and the 539-test/debug/release gate (2026-10-14), followed by the installed
542-test handoff repair below. Named phone acceptance remains open after a failed
first batch; the earlier phone results validate the prior
531-test build only.

## Problem and evidence

Metria's fast debug MiniLM routes resolve a closed request grammar before
embedding inference. A request outside that grammar never reaches MiniLM. This
caused a copied `oberview` spelling to miss the today-overview shortcut, fall
back to Gemma, and have its generated answer rejected by the unchanged factual
auditor. The member saw a local evidence card without the expected answer.
Earlier contextual weight spelling aliases helped individual requests but do
not constitute a scalable recognition strategy.

The member approved shared offline typo-tolerant recognition and documenting
the research, decision and development route. A full English autocorrect or a
new production dependency is not part of this decision.

## Research

Primary sources checked during this session:

- [Home Assistant local-first Assist](https://www.home-assistant.io/blog/2025/02/13/voice-chapter-9-speech-to-phrase/):
  its local command handler gets the first opportunity, with LLM fallback for
  unrecognized requests. This supports the fast-handler/large-model split.
- [HassIL source and template contract](https://github.com/OHF-Voice/hassil):
  alternatives, optional phrases and typed slots separate reusable language
  patterns from handler inputs. Its [changelog](https://github.com/OHF-Voice/hassil/blob/main/CHANGELOG.md)
  records both introducing and later removing an n-gram fuzzy matcher. Do not
  infer that unrestricted fuzzy matching is always beneficial or that a source
  branch feature is enabled in every shipped Home Assistant build.
- [Current Rasa NLU command adapter](https://rasa.com/docs/reference/config/components/nlu-command-adapter/):
  a classifier may start a defined application flow above a configured threshold
  without an LLM request; application flow remains independently controlled.
- [Rasa misspelling guidance](https://rasa.com/blog/10-best-practices-for-designing-nlu-training-data):
  recommends noisy examples and character features before a generic spellchecker,
  which can introduce incorrect changes. The example pipeline is historical;
  this is methodological evidence, not a dependency recommendation.
- [Intent/slot robustness research](https://aclanthology.org/2021.nlp4convai-1.7/)
  and [BERT retrieval typo research](https://arxiv.org/abs/2108.12139):
  noise can degrade neural understanding; noise-aware training can improve it.
  Neither study measures Metria's MiniLM artifact or target Android latency.
- [SymSpell](https://github.com/wolfgarbe/SymSpell) and
  [SymSpellKt](https://github.com/Darkrock-Studios/SymSpellKt): offline edit-distance
  candidate generation is available. Android compatibility, dependency size and
  performance in Metria have not been established.
- [Rhasspy matching documentation](https://rhasspy.readthedocs.io/en/v2.4.20/intent-recognition/):
  fuzzy command matching exists, but ignoring unknown words can cause unexpected
  text-chat interpretation. [Rhasspy 3](https://github.com/rhasspy/rhasspy3) is
  archived, so treat it as historical implementation evidence.

## Decision

Retain MiniLM, Gemma, fresh local reads and factual audits. Build a small bundled
vocabulary of correctly spelled assistant-routing words from supported features;
generate typo candidates rather than enumerate misspellings. No general English
dictionary download, model training, network operation or added dependency.

The first slice is conservative: exact routes first, then a bounded typo
candidate retry for supported read operations. Each candidate must satisfy the
existing complete host policy before competitive MiniLM scoring. Corrections
are transient classifier input; Chat retains the member's original text.
Uncertainty abstains. This slice does not claim arbitrary paraphrase support.

The candidate matcher uses one edit (insertion/deletion/substitution/adjacent
transposition), one sufficiently long token, and an unambiguous nearest routing
word. Substitutions must use neighboring keys on a staggered English QWERTY
layout; this is an explicit hackathon input assumption, not general phonetic or
keyboard-independent correction. Other tokens are preserved. Never drop unknown
words to obtain a match.
Dates, numbers, negation, write actions, people and saved dish queries are not
correction targets. Saved-dish/draft/navigation operations and fuzzy follow-ups
are excluded from the initial retry. Existing exact routes remain available.
The routing vocabulary is a recognition aid, never authority to read or write.
Known valid-word confusables such as height versus weight are protected as well.
The small vocabulary is not a complete English lexical oracle: this slice does
not claim to resolve every real-word spelling ambiguity. Host/grammar rejection
and conservative abstention remain necessary.

## Development route

1. Implement a pure bounded candidate generator and shared exact-first resolver.
   Replace the isolated overview spelling patch with this common mechanism.
2. Cover unseen typos across existing reads and adversarial meaning changes;
   independently review host conflicts, selected context and fallback handling.
3. Diagnose visible rejection handling separately. Do not invent a UI fix without
   code or physical evidence; the local evidence card is not a successful answer.
4. Run the full Android test/debug assembly/release compilation gate, then one
   authorized in-place install preserving private records and models.
5. Member tests exact copied typo, other read typos, original working routes and
   a rejected request. Inspect only opt-in content-free routing events.
6. Later, separate broader intent recognition from typed operation/date/metric
   validation. Evaluate original semantic candidates and lexical candidates
   together; confidence alone must never authorize a tool or choose an ambiguous
   date. More flexible paraphrases are a later slice, not evidence from this one.

## Acceptance

### Approved next slice: compositional read phrasing (2026-10-14)

The member approved broadening existing meal, nutrition and weight reads before
adding more assistant features. Preserve exact-route priority, then recognize
reusable request heads, read subjects and explicit supported date positions.
The new parser must consume the complete normalized message and nominate one
typed operation with unambiguous slots. Unknown qualifiers are never discarded.

Scope is day-wide meal lists and one recorded calorie/protein total for today
or yesterday, calorie/protein information for the latest meal, and the existing
latest-weight, last-weigh-in-date and recent-weigh-in windows. Breakfast or other
meal-slot filtering is not supported by the fast handler and must abstain rather
than return all meals. No new goal, overview, navigation, saved-dish, draft,
write or conversational follow-up capability is introduced by this slice.

The candidate reuses the exact resolver's host-tool compatibility and frozen
dates; classifier input remains the original normalized wording, not a template
selected to guarantee a match. The existing single typo candidate may retry
either parser without inheriting conversational scope. Existing competitive
MiniLM thresholds, original Gemma fallback wording, fresh local reads, response
validation and factual auditing remain mandatory. Selected-record contexts and
conflicting host requirements cannot be overridden. Semantic similarity alone
does not authorize an operation or select a date.

Automated acceptance must cover composition across request/date/subject choices,
real host extraction, original classifier text, host conflicts, selected context,
mixed actions, negation, other people, advice, dates, unknown filters and spelling
regressions. One reviewed build will be installed in place for member comparisons
and content-free route inspection. This approval does not close any build or
physical acceptance gate. No dependency, model or dictionary download is needed.

Implementation is in `MiniLmFlexibleReadPolicy`: anchored body components and
one explicit leading/trailing today/yesterday date, followed by the existing
host-compatible resolver. Latest-meal questions have no explicit date and weight
requests retain their existing 90-day latest/date or 30-calendar-day recent
windows. The debug router rechecks the same parser/key. The legacy write guard
recognizes complete read questions containing `log`/`record`, including one
existing bounded routing-word correction; mixed writes still fail that check.
Original length/control-character bounds apply before that correction.

This is wider compositional coverage, not unrestricted English understanding.
Internal possessive-date phrasing such as “What's yesterday's calorie total?”
can still miss this parser and use Gemma. Existing exact phrases retain priority.
No prototype embedding, model artifact or similarity/margin threshold changed;
automated grammar acceptance does not prove MiniLM will accept every new phrase.

Independent review found no remaining blocker. Parent review replaced an early
token-bag prototype with ordered anchored components, checked singular/plural
weight semantics and added input-bound/window/conflict regressions. The complete
gate passed 539 tests, zero failures/errors/skips, debug assembly and release
Kotlin compilation. Command from `native-android/`:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`.
`git diff --check` passed. Debug APK SHA-256:
`0f40f18255ce69c9288e8a810bfbf513caa7cec2423d94cb9f5e1840229068e8`.
Initial ADB preflight listed no connected device. The member subsequently
authorized wireless connection and installation, and the reviewed APK above was
installed with `adb install -r` on Samsung SM-S928B / Android 16 (Success).
Force-stop and cold launch returned `Status: ok`. Existing app storage was
preserved; no clear, uninstall or model replacement was performed. Member unlock,
record comparison and routing acceptance remain pending. No raw member records
or device screenshots were inspected, and no transient endpoint or pairing code
is retained in the repository.

Next named phone batch after authorized `install -r` and member unlock:

1. “Today, can I see my recorded meals?”
2. “What is in my food log for yesterday?”
3. “Give me my recorded protein total for today.”
4. “Tell me the protein content of my most recent meal.”
5. “What is the most recent weight I recorded?”
6. “On what date was my last weigh-in?”
7. “Can I see my recent weigh-ins?”

Compare dates, records and estimates with the vault and inspect opt-in
content-free events for MiniLM selection/audit or precise abstention. Then check
an original working meal phrase, a single-word typo and “Show my food log for
breakfast today” (must not become an all-day fast answer). No writes are needed.
Retain existing open lifecycle/accessibility gates; do not infer them from this
language-only change. No commit/push has been authorized for this slice.

### Failed first phone batch: copied wording and handoff repair

The member reported no answer after Gemma loading for a recorded-protein-total
question ending in `tday`, then the corrected `today` with trailing spaces.
Date words are intentionally outside the shared typo vocabulary; the first
phrase therefore abstains from MiniLM. The corrected phrase is covered by the
new grammar. New regressions exercise that exact phrase, whitespace, prior
today/latest-meal/yesterday histories, earned follow-up scopes and selected-record
conflicts through real host extraction. No new date-spelling alias is added.

The installed APK checksum matched the reviewed 539-test artifact. Retained
events show a scope rejection and native Gemma nutrition read, then no
continuation/completion or later turn start before lock. Thus there is no trace
evidence that the corrected retry reached routing at all. A pending native tool
can block new submissions. This does not prove why the first handoff stalled.

Read-only review identified a concrete effect-dependency gap: the Chat tool
effect checked the active request ID without subscribing to its changes. It now
reruns when that ID is reconciled, retaining the existing handled-request guard
against duplicate or stale execution. This repairs an ordering hazard; the
specific physical failure is not yet reproduced under controlled conditions.
Expired native continuations now emit the fixed `continuation_expired` reason
through existing opt-in debug metadata before returning their existing recovery
diagnostic. No question, tool arguments/results, exception text or record values
are added. Normal trace bounds, release no-op and retention remain unchanged.
Independent review found no new stale/cancelled or duplicate-dispatch issue.
Full gate passed 542 tests with zero failures/errors/skips, debug assembly and
release Kotlin compilation. The new regressions confirm the corrected wording
through real host extraction and prior-history contexts; the trace test verifies
fixed continuation-failure metadata survives reopening. Actual Compose scheduling
and the expired lifecycle guard lack instrumentation coverage and remain physical
or integration-test gaps. APK SHA-256:
`608deb51534a1a040288261f4b8889577561f58b06cbaaf29a76a86354e9036d`.
Authorized in-place installation returned Success on the same Samsung SM-S928B /
Android 16; force-stop/cold launch returned `Status: ok`. Existing app storage
was preserved. Member unlock and corrected-request-first phone comparison plus
metadata correlation are pending. Then test the date-typo fallback independently;
it is intentionally not a MiniLM date correction.

- [x] Pure candidate and shared resolver regressions pass.
- [x] Independent review and full Android gate pass.
- [x] Authorized data-preserving installation/launch.
- [x] Copied overview typo returns audited local summary on the target phone.
- [x] Other read typo and exact-route regression checks on the target phone.
- [x] Rejection has visible recovery feedback; lifecycle/accessibility gaps named.

No new commit/push approval is implied. No member records, raw screenshots,
questions beyond voluntarily copied test wording, or model outputs belong in
monitoring or acceptance artifacts.

### Automated evidence

Worker implementation and independent reviewer inspection completed; parent
review preserved exact-first routing and corrected test assumptions for ambiguous
`caloriee` and `weigth` candidates. Tests cover actual host grounding, typed tool
binding, one-edit types, ambiguity, previous conversation scope, selected context,
saved-query preservation and valid-word collisions. All 524 tests passed with
zero failures/errors/skips. Debug assembly and release Kotlin compilation passed
using the complete runbook command; `git diff --check` passed. No dependency or
classifier threshold changed.

Debug APK SHA-256:
`eef7f9c850291328f501ed6d5f9be2b6aa0ed39773867b74c8cf378f14e33401`.
Authorized `adb install -r` returned Success on Samsung SM-S928B / Android 16;
force-stop and launch returned `Status: ok`. Existing vault, records and model
files were preserved. Member unlock and named Chat comparisons remain pending.

### Named phone acceptance

The member confirmed both requested typo-bearing answers appeared and matched
the saved records on the installed 524-test build. Authorized metadata-only
inspection confirmed MiniLM selected today's progress overview (positive 0.9169,
competing 0.5695, fallback 0.5627) and today's meal list (positive 0.9693,
competing 0.8067, fallback 0.7636). Both replies passed the factual audit and
skipped Gemma generation. Only these named scenarios are accepted. Exact-route
regression and rejected-turn feedback remain open; no record values or replies
were inspected or copied into evidence.

The member also confirmed both original overview and today-meal questions still
match the records on this build. The latest retained events show another
accepted MiniLM today-meal read and accepted factual audit. No separate new
overview event was present in that inspected tail, so its regression comparison
is member-observed rather than newly trace-correlated. Rejected-turn feedback
and broader language/lifecycle/accessibility states remain open.

### Error-display investigation

Independent inspection found no mismatched transient-row count or scroll index.
The current loaded-turn diagnostic is latched and rendered beneath evidence;
both diagnostic changes trigger scrolling. The actual reported absence is not
explained by that static evidence. Existing tests cover latch predicates, not
Compose rendering. The app currently has no Compose instrumentation-test
dependencies; adding them is outside this no-new-dependency slice. Verify the
named rejected-turn transition on the target phone and retain this gap if it
cannot be reproduced. Do not mark it repaired by fixing intent recognition.

Follow-on scope approved by the member's continuation: static inspection does
confirm that transient diagnostics are absent from the encrypted message list,
so a retained evidence card can survive reopening without the failure reason.
Retain a completed current-turn rejection once as an ordinary host-authored
assistant message, using the existing transcript and reply-consumption gate.
No new schema/dependency or model-generated factual text is added. This addresses
the confirmed restoration gap; it does not establish the original immediate
disappearance's cause or physical fix before an observed phone check.

Completed-failure implementation passed independent review and the full Android
gate with 531 tests, zero failures/errors/skips, debug assembly and release
compilation. The current request is consumed before appending the generic host
diagnostic; pending/stale/success/action/tool/cancel states abstain. Consumed
diagnostics are not rendered a second time. Polite announcement state is separate
from persisted transcript equality and is cleared on Chat exit and new/cleared/
locked sessions. Existing transcript persistence and expiry are reused.

Parent review corrected duplicate-error rendering and a transient-announcement
equality issue before the successful gate. Tests cover those predicates and
runtime-failure operations; actual Compose rendering, rejection/restoration and
TalkBack remain physical gates. APK SHA-256:
`b3ab273b027a93a9acedd40e1de44e5e7dc511308936e506994555525eb1c6bd`.
Authorized in-place install returned Success on Samsung SM-S928B / Android 16;
launch returned `Status: ok`, preserving existing vault/records/models. Next
phone scenario is a read outside Metria's tracked fields, then Chat close/reopen.
Inspect metadata to distinguish a rejected turn from a safely accepted generic
explanation; a generic success alone does not close the rejection gate.

The member confirmed the unsupported-field test displayed a message and retained
it after Chat close/reopen on this installed build. Authorized content-free
metadata confirmed scope rejection, native Gemma selection and final
`reply_rejected / unsupported_personal_fact` with no record tool. This did exercise
the terminal rejection path, rather than a normal generic answer. Named visible
feedback and reopen acceptance are closed. Actual message text was not inspected.
New-failure selection, TalkBack announcement, font/keyboard/rotation, expiry and
lock/process-restoration combinations remain unexercised physical gates.
