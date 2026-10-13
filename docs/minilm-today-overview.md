# Fast today overview

Status: implementation, review and automated gate passed; member confirmed the
named daily summary, copied typo and original-question regression work. MiniLM
route/audit acceptance is recorded below; broader physical states remain open.
Baseline: `7c6f9e5`, with the installed 510-test companion checkpoint preserved.

The member selected a fast “How am I doing today?” summary as the next assistant
feature. Implement directly in ordinary Chat, then build/review/install and test
on the existing phone. No standalone benchmark, dependency, automatic record
write, model/vault replacement, or new commit/push is authorized by this slice.

## Contract

- Only closed, explicit English questions asking for today's broad overview are
  eligible. MiniLM uses the normalized member wording and competes against all
  existing intent groups at unchanged thresholds.
- The host independently binds the request to `read_progress_overview(today)`
  and freezes its local date. Read fresh unlocked records and current confirmed
  daily references through `MemberProgressOverview`.
- Return a concise, deterministic factual recap: recorded meal count, available
  displayed calorie/protein estimates, confirmed daily goals when present, and
  partial/missing coverage. Missing estimates and goals are explicit; unrecorded
  intake stays unknown. Empty does not mean zero intake.
- Do not include weight values/trends, percentages, remaining amounts, judgments,
  recommendations or clinical interpretations in this fast reply. Ignore the
  response's freeform summary. The normal factual auditor remains enforced.
- A strict wrapper validates the trusted tool/date, local source, English,
  counts/coverage, printed values and reference bounds before returning text.
  Preserve existing weekly and bilingual fallback behavior.
- Advice, medical, mixed-operation, historical/weekly, selected-meal and
  conflicting-host requests retain normal safe handling. MiniLM abstention keeps
  the existing grounded Gemma path. The broad overview does not establish a
  singular metric/date follow-up scope.
- Use existing Chat message retention, native selection/copy and cancellation
  behavior. The only monitoring addition is fixed technical route metadata;
  prompts, records, replies and embeddings remain excluded.

## Delivery gates

- [x] Map the existing overview, fallback, factual evidence and router seams.
- [x] Record member-approved scope and update design/assistant contract.
- [x] Implement policy, competitive routing, frozen fresh read and audited reply.
- [x] Meaningful full/partial/missing/empty/no-reference and rejection tests.
- [x] Independent review; full Android test/debug assembly/release compile gate.
- [x] Approved in-place installation with private data preserved.
- [x] Member comparison of today's recorded estimates and confirmed references.
- [x] Direct retained metadata confirms accepted local route and answer audit.
- [x] Prior today-meal read retains its behavior on the combined build.
- [ ] Broader goal reads, navigation/selection and lifecycle regression checks.

Only named observed states close physical gates. Document unexercised coverage,
lifecycle, accessibility and visual states as open; a development checkpoint
does not establish release or milestone completion.

## Evidence

Independent review found no blocker. The gate caught a missing `an overview`
phrase form and an empty-state test assertion matching the zero inside a valid
goal; both were corrected without relaxing the factual validator. The complete
Android gate passed with 521 tests and zero failures/errors/skips, debug
assembly and release compilation; `rtk git diff --check` passed. Command from
`native-android/`:
`rtk proxy .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin --no-daemon`.

Debug APK SHA-256:
`7aa41d67e1582a766cfab1ac76dc521ea96ce388c0d99318cd5fcfb41f5e5c50`.
Authorized `install -r` succeeded on Samsung SM-S928B / Android 16 and launch
returned `Status: ok`, preserving existing private storage and model files.
The member reported that the named daily-summary question worked well on this
installed build. Retained-route confirmation remains pending: ADB currently
lists no connected device. Alternate wording and prior-route regression checks
remain open. No raw member record, question or device screen was captured.

On reconnection, content-free retained events confirmed the initial summary
selected `today_overview` through MiniLM and passed the answer audit (positive
similarity 0.9888, competing 0.5405, fallback 0.4780). The alternate-question
attempt was rejected as `scope_text`, selected native Gemma, read today's
nutrition summary, and rejected the reply as `unsupported_personal_fact`.
The member saw only the local evidence card after loading. Exact copied wording
and the missing visible-error cause are being investigated; alternate wording
acceptance remains open. No factual validator was relaxed.

The copied request identified the spelling `oberview`. An isolated spelling
patch passed the automated gate but was not installed. The member then approved
replacing that patch with the shared bounded read-recognition mechanism in
[the researched decision](assistant-routing-recognition.md). Historical, mixed,
negated and conflicting-host requests must still abstain. No evidence supports
a separate UI lifecycle fix yet; the reported missing rejection display remains
an open observation, rather than a claimed repaired state.

The member subsequently confirmed the copied overview typo returns a matching
answer on the shared 524-test recognition build. Retained metadata confirms
accepted MiniLM `today_overview` and accepted answer audit. This closes that
named typo scenario, not the independent rejected-turn feedback observation.
