# Shared assistant recognition and typo tolerance

Decision date: 2026-10-13. Status: implemented and reviewed; full 531-test gate
passed. Named typo, original-question and rejected-message/reopen phone checks
passed. Broader physical and accessibility gates remain open.

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
