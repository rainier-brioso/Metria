package app.metria

import java.time.LocalDate

/**
 * Approved cycle-aware insight boundary: this optional context is encrypted in
 * the member vault, never inferred from identity, and only supports cautious
 * interpretation of short-term weight variation. It is not medical data for
 * diagnosis or treatment.
 */
data class CycleContext(
    val enabled: Boolean = false,
    val periodStarts: List<LocalDate> = emptyList(),
)

object CycleContextValidation {
    private const val maxPeriodStarts = 24

    fun valid(value: CycleContext, today: LocalDate = LocalDate.now()): Boolean =
        value.periodStarts.size <= maxPeriodStarts &&
            value.periodStarts.distinct().size == value.periodStarts.size &&
            value.periodStarts.all { it <= today && !it.isBefore(today.minusYears(3)) }

    fun normalized(value: CycleContext): CycleContext = value.copy(
        periodStarts = value.periodStarts.distinct().sortedDescending().take(maxPeriodStarts),
    )
}
