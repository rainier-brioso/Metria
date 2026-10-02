package app.metria

import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Parses only unambiguous, named or ISO calendar days for bounded local record reads. */
internal object MetriaCalendarDayPolicy {
    data class Claim(val day: LocalDate, val range: IntRange)
    data class Scan(val claims: List<Claim>, val unsupported: Boolean) {
        val hasExpression: Boolean get() = claims.isNotEmpty() || unsupported
    }

    private val months = mapOf(
        "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5,
        "junio" to 6, "julio" to 7, "agosto" to 8, "septiembre" to 9,
        "setiembre" to 9, "octubre" to 10, "noviembre" to 11, "diciembre" to 12,
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5,
        "june" to 6, "july" to 7, "august" to 8, "september" to 9,
        "october" to 10, "november" to 11, "december" to 12,
    )
    private val monthNames = months.keys.joinToString("|")
    private val dayMonth = Regex("\\b(\\d{1,2})\\s+(?:de\\s+)?($monthNames)(?:\\s+(?:de\\s+)?(\\d{1,4}))?\\b")
    private val monthDay = Regex("\\b($monthNames)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{1,4}))?\\b")
    private val isoDay = Regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")
    private val numericSlashDay = Regex("\\b\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?\\b")
    private val malformedNamedDay = Regex("\\b\\d{1,2}\\s+de\\s+[a-z]+(?:\\s+de\\s+\\d{1,4})?\\b")

    fun scan(text: String, today: LocalDate, rejectNumericDates: Boolean = false): Scan {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        val claims = mutableListOf<Claim>()
        var unsupported = rejectNumericDates && numericSlashDay.containsMatchIn(normalized)
        val recognizedRanges = mutableListOf<IntRange>()
        fun collect(match: MatchResult, day: LocalDate?) {
            recognizedRanges += match.range
            if (day == null || ChronoUnit.DAYS.between(day, today) !in 0..90) unsupported = true
            else claims += Claim(day, match.range)
        }
        isoDay.findAll(normalized).forEach { match ->
            collect(match, runCatching {
                LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            }.getOrNull())
        }
        dayMonth.findAll(normalized).forEach { match ->
            collect(match, resolve(match.groupValues[1].toInt(), months.getValue(match.groupValues[2]), match.groupValues[3], today))
        }
        monthDay.findAll(normalized).forEach { match ->
            collect(match, resolve(match.groupValues[2].toInt(), months.getValue(match.groupValues[1]), match.groupValues[3], today))
        }
        malformedNamedDay.findAll(normalized).forEach { match ->
            if (recognizedRanges.none { range -> match.range.first >= range.first && match.range.last <= range.last }) {
                unsupported = true
            }
        }
        if (claims.map(Claim::range).distinct().size != claims.size) unsupported = true
        return Scan(claims.sortedBy { it.range.first }, unsupported)
    }

    private fun resolve(day: Int, month: Int, rawYear: String, today: LocalDate): LocalDate? {
        if (rawYear.isNotEmpty()) {
            if (rawYear.length != 4) return null
            return runCatching { LocalDate.of(rawYear.toInt(), month, day) }.getOrNull()
        }
        return (today.year downTo today.year - 4)
            .mapNotNull { year -> runCatching { LocalDate.of(year, month, day) }.getOrNull() }
            .firstOrNull { !it.isAfter(today) }
    }
}
