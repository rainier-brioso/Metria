# METRIA-WEIGHT-JSON-01B — strict parser boundary verification

Draft: executable only after 01A is reviewed, accepted, integrated and committed.
Record that accepted prerequisite baseline in trial evidence before this run.

Allowed paths: only
`native-android/app/src/test/java/app/metria/WeightJsonImportTest.kt` and, if a
test exposes a contract violation, the minimal repair in
`native-android/app/src/main/java/app/metria/WeightJsonImport.kt`.
Use `weight_json_android_offline`. Do not change build/config/policy/docs,
dependencies, UI or storage. No private data, network, device or logging.

Use the public types and exact schema of the accepted parser. Verify:

- Byte sizes 1048576 accepted if otherwise valid; 1048577 rejected, malformed
  UTF-8 rejected, UTF-8 BOM rejected, empty input/root non-array rejected.
- Array lengths 0/1001 rejected and 1/1000 accepted; input order and duplicate
  records preserved; one invalid record rejects the whole batch.
- Required/unknown/duplicate/escaped duplicate keys, both/neither time fields,
  nested values and wrong primitive types; safe error fields never echo keys.
- Strict JSON numbers (leading zero/plus, missing exponent/fraction digits,
  NaN/Infinity rejected); no single quotes/comments/trailing commas/data,
  unescaped controls or invalid escapes. Valid escapes must decode correctly.
- Notes null/omitted/empty, 500/501 Unicode code points, astral characters,
  paired and unpaired escaped/literal surrogates. Preserve whitespace/text.
- Kilograms inclusive 15/500 and outside bounds, pound conversion precision,
  overflow, underflow and finite values.
- Valid leap days, invalid dates and year 0000, strict four-digit dates;
  today/equal and future dates using zones on opposite sides of a UTC boundary.
- Exact instants: Z/positive/negative offsets equivalent to same instant,
  equality to injected now, later today, invalid dates/hours/minutes/seconds,
  leap seconds, absent/invalid offsets and fractions of 1/9/10 digits.
- Invalid nested/deep input must return an invalid result without unbounded
  recursion or throwing. No fabricated timestamps or record IDs.

Use table-driven cases/helpers to keep tests concise. Return only complete Aider
edit blocks, no repeated code listings. The runner runs the complete profile.
Stop on a failing baseline or a needed scope expansion. Two attempts, 1800s
task, 600s attempt, 16384 output and 1024 reasoning tokens, repo map disabled.
