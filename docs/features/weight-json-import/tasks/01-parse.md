# Task 01 - Parse and validate a weight JSON batch

**Task ID:** `METRIA-WEIGHT-JSON-01`  
**Source baseline:** `5c68de3` (`codex/weight-json-import`, fast-forwarded to `origin/main` on 2026-11-09). The execution baseline is the subsequent contract/config commit containing this file; record its full hash in trial evidence.  
**Runner config:** `hybrid_sdlc.toml`; profile `weight_json_android_offline`.  
**Policy:** `docs/features/weight-json-import/weight-json-import.policy.json`.  
**Prerequisites:** this contract, policy, and named profile are committed unchanged; endpoint/model and effective settings are verified; offline baseline/profile gate passes.  
**Scope:** pure parser only. No storage, UI, model calls, IDs, or writes.

## Allowed paths

- Add `native-android/app/src/main/java/app/metria/WeightJsonImport.kt`.
- Add `native-android/app/src/test/java/app/metria/WeightJsonImportTest.kt`.
- Do not edit any existing production file, build file, config, task policy, or design document.

## Input schema

The input is UTF-8 JSON, with no byte-order mark, containing only a root array. This is the complete JSON Schema (Draft 2020-12) for shape and types; the parser additionally enforces the byte, date/time, numeric-conversion, and future-value rules below.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "array",
  "minItems": 1,
  "maxItems": 1000,
  "items": {
    "oneOf": [
      {
        "type": "object",
        "required": ["weight", "unit", "date"],
        "properties": {
          "weight": {"type": "number"},
          "unit": {"enum": ["kg", "lb"]},
          "date": {
            "type": "string",
            "format": "date",
            "pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}$"
          },
          "note": {"type": ["string", "null"], "maxLength": 500}
        },
        "not": {"required": ["recordedAt"]},
        "additionalProperties": false
      },
      {
        "type": "object",
        "required": ["weight", "unit", "recordedAt"],
        "properties": {
          "weight": {"type": "number"},
          "unit": {"enum": ["kg", "lb"]},
          "recordedAt": {
            "type": "string",
            "format": "date-time",
            "pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?(Z|[+-][0-9]{2}:[0-9]{2})$"
          },
          "note": {"type": ["string", "null"], "maxLength": 500}
        },
        "not": {"required": ["date"]},
        "additionalProperties": false
      }
    ]
  }
}
```

Shape details:

- Each object has exactly `weight`, `unit`, one of `date` or `recordedAt`, and optional `note`. Both time fields, neither time field, unknown fields, and duplicate object keys are invalid.
- `weight` is a JSON number; strings, booleans, and null are invalid. JSON number syntax may include an exponent. The converted Kotlin `Double` must be finite.
- `unit` is exactly `kg` or `lb`.
- `date` is a real Gregorian calendar date in exactly `YYYY-MM-DD`, years 0001-9999.
- `recordedAt` is a valid ISO-8601 offset date-time with seconds, explicit `Z` or `+/-HH:MM` offset, and optional 1-9 fractional-second digits. Reject invalid calendar/time/offset values, offsets outside `ZoneOffset` range, leap seconds, missing offsets, and timestamp fractions beyond nanoseconds.
- `note` may be omitted, null, or a string of at most 500 Unicode code points. Preserve the decoded text exactly; do not trim or normalize it. Reject ill-formed surrogate sequences.
- Maximum input is 1,048,576 bytes; maximum record count is 1,000. Reject empty arrays, malformed UTF-8/JSON, BOM, duplicate keys, comments, markdown fences, trailing non-whitespace, and partial acceptance. Whitespace permitted by JSON around the root is allowed.

## Conversion and time validation

- Convert pounds with the existing constant `0.45359237`: `kilograms = pounds * 0.45359237`. For `kg`, retain the numeric value as kilograms. Do not round or format during conversion.
- Validate the converted finite kilograms inclusively in `15.0..500.0`. Conversion overflow, underflow to an out-of-range value, and values outside the inclusive range are invalid.
- Capture the supplied `ZoneId` as validation context. Accept `date` only when it is not after `now.atZone(zone).toLocalDate()`. It remains a `DateOnly`; do not derive an instant, time, zone-bearing stored date, UUID, or entry.
- Parse `recordedAt` with its stated offset and compare the resulting `Instant` to `now`; accept equality and reject any later instant, including later today. Preserve it as an `ExactInstant`; do not replace its offset-aware instant with a local date.
- The parser does not deduplicate. It returns every validated input record in input order. Duplicate identity and reporting belong to the later persistence contract.

## Result and error API

Implement a pure API equivalent to:

```kotlin
sealed interface WeightJsonImportResult {
    data class Valid(val records: List<WeightJsonImportRecord>) : WeightJsonImportResult
    data class Invalid(val error: WeightJsonImportError) : WeightJsonImportResult
}

data class WeightJsonImportRecord(
    val kilograms: Double,
    val time: WeightJsonImportTime,
    val note: String?,
)

sealed interface WeightJsonImportTime {
    data class DateOnly(val date: LocalDate) : WeightJsonImportTime
    data class ExactInstant(val instant: Instant) : WeightJsonImportTime
}

data class WeightJsonImportError(
    val code: WeightJsonImportErrorCode,
    val recordNumber: Int?,
    val field: String?,
)

enum class WeightJsonImportErrorCode {
    INPUT_TOO_LARGE, INVALID_UTF8, INVALID_JSON, INVALID_ROOT,
    INVALID_RECORD_COUNT, DUPLICATE_KEY, MISSING_FIELD, UNKNOWN_FIELD,
    INVALID_FIELD_TYPE, INVALID_DATE, INVALID_TIMESTAMP, FUTURE_DATE,
    FUTURE_TIMESTAMP, INVALID_NOTE, WEIGHT_OUT_OF_RANGE,
}

object WeightJsonImport {
    fun parse(bytes: ByteArray, now: Instant, zone: ZoneId): WeightJsonImportResult
}
```

Return either all records or one invalid result; never return a partial list. Select the first error deterministically by input order, then field-validation order. Use one-based `recordNumber`; use null for a root-level error. `field` may contain only a known schema field name (`weight`, `unit`, `date`, `recordedAt`, or `note`), never an arbitrary key or input value. Error codes, record number, and field are safe to display/log; never include raw JSON, notes, or rejected values. No new dependency.

## Examples

Valid date-only record:

```json
[{"weight":72.4,"unit":"kg","date":"2026-10-12","note":null}]
```

Valid exact instant with positive offset:

```json
[{"weight":160,"unit":"lb","recordedAt":"2026-10-12T08:30:00+02:00","note":"sample"}]
```

The second example converts to 72.5747792 kg without rounding. An offset such as `2026-10-12T06:30:00Z` represents the same exact instant. A date equal to the current date in the supplied zone and a timestamp exactly equal to `now` are accepted; a later timestamp on that same date is rejected.

## Acceptance and stop conditions

Test both variants, exact schema/key rules, valid and invalid UTF-8, BOM, duplicate keys, trailing data, byte/record/note limits, all malformed/root/type cases, date validity, zone-dependent date boundary, future date, exact timestamp/offset/equality/later-today, pounds conversion, finite/range boundaries, whole-array rejection, safe errors, and input-order preservation. Include no IDs, synthetic timestamps, vault operations, UI, network, or new dependencies.

Run only `weight_json_android_offline`. Its exact command is the checked-in Gradle wrapper from `native-android/`: `--offline :app:testDebugUnitTest :app:assembleDebug --no-daemon`. Do not add flags or change the profile during a run. Stop if the profile cannot resolve its SDK/cache offline, baseline fails, scope grows, or schema/time semantics are ambiguous.
