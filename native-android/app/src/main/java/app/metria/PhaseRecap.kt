package app.metria

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PhaseRecapWeight(val id: String, val recordedAt: Instant, val kilograms: Double)

data class RecordedMetricTotal(
    val total: Double?,
    val contributingMeals: Int,
    val missingFullCoverageMeals: Int,
    val partialMealsExcluded: Int,
)

data class RecordedNutritionTotals(
    val calories: RecordedMetricTotal,
    val protein: RecordedMetricTotal,
    val carbohydrate: RecordedMetricTotal,
    val fat: RecordedMetricTotal,
    val mealsWithoutNutrition: Int,
)

data class PhaseRecap(
    val periodAvailable: Boolean,
    val start: Instant,
    val end: Instant?,
    val weights: List<PhaseRecapWeight>,
    val boundaryWeightTimesUnavailable: Int,
    val mealsLogged: Int,
    val boundaryDateOnlyMealsExcluded: Int,
    val loggingDays: Int,
    val nutrition: RecordedNutritionTotals?,
) {
    val weightChangeKg: Double? get() = weights.takeIf { it.size >= 2 }?.let { it.last().kilograms - it.first().kilograms }
}

/** Pure calculation over retained local records. No averages or inferred measurements. */
object PhaseRecapCalculator {
    fun calculate(
        phase: GoalPhase,
        isCurrent: Boolean,
        weights: List<WeightEntry>,
        meals: List<MealEntry>,
        zone: ZoneId,
        asOf: Instant,
    ): PhaseRecap {
        val start = phase.confirmedAt
        val end = if (isCurrent) asOf else phase.endedAt
        if (end == null || end < start) return PhaseRecap(false, start, end, emptyList(), 0, 0, 0, 0, null)
        val calendarLowerBound = start.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val calendarUpperBound = end.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

        fun containedDay(date: LocalDate) = !date.atStartOfDay(zone).toInstant().isBefore(start) &&
            !date.plusDays(1).atStartOfDay(zone).toInstant().isAfter(end)
        fun overlaps(date: LocalDate) = date.atStartOfDay(zone).toInstant().isBefore(end) &&
            date.plusDays(1).atStartOfDay(zone).toInstant().isAfter(start)
        fun inPeriod(instant: Instant) = !instant.isBefore(start) && instant.isBefore(end)

        var boundaryWeightTimesUnavailable = 0
        val includedWeights = weights.filter { entry ->
            if (entry.recordedAt.isBefore(calendarLowerBound) || !entry.recordedAt.isBefore(calendarUpperBound)) return@filter false
            val date = entry.recordedAt.atZone(zone).toLocalDate()
            val midnight = entry.recordedAt == date.atStartOfDay(zone).toInstant()
            if (midnight && !containedDay(date) && overlaps(date)) {
                boundaryWeightTimesUnavailable++
                false
            } else inPeriod(entry.recordedAt)
        }.sortedWith(compareBy<WeightEntry> { it.recordedAt }.thenBy { it.id })
            .map { PhaseRecapWeight(it.id, it.recordedAt, it.kilograms) }

        var boundaryDateOnlyMealsExcluded = 0
        val includedMeals = meals.filter { meal ->
            if (meal.recordedAt.isBefore(calendarLowerBound) || !meal.recordedAt.isBefore(calendarUpperBound)) return@filter false
            if (meal.recordedTimeKnown) inPeriod(meal.recordedAt) else {
                val date = meal.recordedAt.atZone(zone).toLocalDate()
                when {
                    containedDay(date) -> true
                    overlaps(date) -> { boundaryDateOnlyMealsExcluded++; false }
                    else -> false
                }
            }
        }
        val days = (includedWeights.map { it.recordedAt.atZone(zone).toLocalDate() } +
            includedMeals.map { it.recordedAt.atZone(zone).toLocalDate() }).toSet().size

        fun metric(select: (MealNutrition) -> Double?): RecordedMetricTotal {
            var sum = 0.0
            var contributing = 0
            var missing = 0
            var partial = 0
            includedMeals.forEach { meal ->
                val nutrition = meal.nutrition ?: return@forEach
                if (nutrition.estimateCoverage != "full") { partial++; return@forEach }
                val value = select(nutrition)
                if (value == null) missing++ else { sum += value; contributing++ }
            }
            return RecordedMetricTotal(sum.takeIf { contributing > 0 }, contributing, missing, partial)
        }
        val nutrition = RecordedNutritionTotals(
            calories = metric(MealNutrition::caloriesKcal),
            protein = metric(MealNutrition::proteinGrams),
            carbohydrate = metric(MealNutrition::carbohydrateGrams),
            fat = metric(MealNutrition::fatGrams),
            mealsWithoutNutrition = includedMeals.count { it.nutrition == null },
        )
        return PhaseRecap(
            true, start, end, includedWeights, boundaryWeightTimesUnavailable,
            includedMeals.size, boundaryDateOnlyMealsExcluded, days,
            nutrition,
        )
    }
}
