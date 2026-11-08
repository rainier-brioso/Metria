# METRIA-WEIGHT-JSON-01A — compact strict parser foundation

Implement only these new files:
- `native-android/app/src/main/java/app/metria/WeightJsonImport.kt`
- `native-android/app/src/test/java/app/metria/WeightJsonImportTest.kt`

Source baseline: `5c68de3`. Execute from the committed HEAD containing this
contract and its policy/config. Profile: `weight_json_android_offline` (existing
offline Gradle unit tests and debug assembly). No existing source changes,
dependencies, storage, UI, network, logging, IDs or generated timestamps.

## Complete production contract

Pure `object WeightJsonImport` exposes
`parse(bytes: ByteArray, now: Instant, zone: ZoneId): WeightJsonImportResult`.
Use these types in package `app.metria`:

```kotlin
sealed interface WeightJsonImportResult {
    data class Valid(val records: List<WeightJsonImportRecord>) : WeightJsonImportResult
    data class Invalid(val error: WeightJsonImportError) : WeightJsonImportResult
}
data class WeightJsonImportRecord(val kilograms: Double, val time: WeightJsonImportTime, val note: String?)
sealed interface WeightJsonImportTime {
    data class DateOnly(val date: LocalDate) : WeightJsonImportTime
    data class ExactInstant(val instant: Instant) : WeightJsonImportTime
}
data class WeightJsonImportError(val code: WeightJsonImportErrorCode, val recordNumber: Int?, val field: String?)
enum class WeightJsonImportErrorCode {
    INPUT_TOO_LARGE, INVALID_UTF8, INVALID_JSON, INVALID_ROOT,
    INVALID_RECORD_COUNT, DUPLICATE_KEY, MISSING_FIELD, UNKNOWN_FIELD,
    INVALID_FIELD_TYPE, INVALID_DATE, INVALID_TIMESTAMP, FUTURE_DATE,
    FUTURE_TIMESTAMP, INVALID_NOTE, WEIGHT_OUT_OF_RANGE,
}
```

Strict UTF-8 without BOM, maximum 1048576 bytes. Root JSON array, 1–1000 flat
objects. Each object has numeric `weight`, string `unit` exactly `kg` or `lb`,
exactly one string `date` or `recordedAt`, optional string/null `note`.
Unknown and duplicate keys are invalid. Reject malformed UTF-8/JSON, invalid
surrogates, invalid escapes, unescaped controls, non-JSON whitespace, comments,
single quotes, trailing commas/data, markdown and type coercion. Numbers follow
JSON syntax including exponents; no leading plus/zero, NaN or Infinity.
Avoid recursive parsing of arbitrary input: the only supported shape is the
flat schema. An unexpected nested value may reject immediately.

Convert lb with `WeightUnits.poundsToKilograms`; use
`WeightUnits.isValidKilograms` (finite inclusive 15–500 kg). No rounding.
Strict `date`: `[0-9]{4}-[0-9]{2}-[0-9]{2}`, real calendar date, year 0001–9999.
Reject dates after `now.atZone(zone).toLocalDate()`. Preserve `DateOnly` without
inventing midnight or an instant. Strict `recordedAt`:
`[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]{1,9})?(Z|[+-][0-9]{2}:[0-9]{2})`.
Require real calendar/time/offset values, no leap seconds, valid ZoneOffset
range. Compare parsed Instant with injected now; equality allowed, later today
also rejected. Preserve `ExactInstant`. Note omitted/null becomes null; preserve
all string text, maximum 500 Unicode code points, reject unpaired surrogates.

All-or-nothing, deterministic first error in input order and a fixed documented
field order. Return all records in input order, including duplicates. Errors
contain only code, one-based recordNumber (null for root errors), and field from
`weight`, `unit`, `date`, `recordedAt`, `note` or null. Never echo unknown key
names or raw values. Parsing ordinary invalid input must not throw exceptions.

## Bounded first test slice

Implement 6–10 focused JUnit4 tests for valid date-only, timestamp/offset and lb
conversion, future rejection, duplicate/unknown keys, strict JSON rejection,
UTF-8 rejection and safe errors. A separate following task adds the exhaustive
boundary matrix; do not attempt that entire matrix in this response.
Use fixed synthetic now/zone, never private member data.

## Output discipline and verification

Implement concise readable code, aiming for at most 300 production lines and
100 test lines. Use helpers instead of repeating cases. Return only the Aider
SEARCH/REPLACE edit blocks needed to create these files, without explanations,
repeating full listings or a schema document. Finish the edit blocks completely.
Do not change this contract/config/policy or run commands. The host runner runs
the named profile. Stop if an existing dependency/tool is unavailable, the
baseline fails, or production scope needs expansion. Limits: two attempts,
1800s task, 600s attempt, 16384 output tokens, 1024 reasoning tokens, repo map 0.
