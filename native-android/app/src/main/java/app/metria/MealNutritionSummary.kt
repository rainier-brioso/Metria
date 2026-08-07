package app.metria

import java.time.LocalDate
import java.time.ZoneId

/** Pure local aggregation for member-confirmed meals; incomplete meals are never treated as zero. */
data class MealNutritionSummary(
    val recordedMeals: Int,
    val completeNutritionMeals: Int,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val fatGrams: Double?,
)

object MealNutritionSummaries {
    fun between(meals: List<MealEntry>, start: LocalDate, end: LocalDate): MealNutritionSummary {
        require(!end.isBefore(start)) { "End date must not precede start date." }
        val inRange = meals.filter { meal ->
            val date = meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(start) && !date.isAfter(end)
        }
        val complete = inRange.mapNotNull { it.nutrition }.filter { nutrition ->
            nutrition.estimateCoverage == "full" && nutrition.caloriesKcal != null && nutrition.proteinGrams != null &&
                nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
        }
        return MealNutritionSummary(
            recordedMeals = inRange.size,
            completeNutritionMeals = complete.size,
            caloriesKcal = complete.takeIf { it.isNotEmpty() }?.sumOf { it.caloriesKcal!! },
            proteinGrams = complete.takeIf { it.isNotEmpty() }?.sumOf { it.proteinGrams!! },
            carbohydrateGrams = complete.takeIf { it.isNotEmpty() }?.sumOf { it.carbohydrateGrams!! },
            fatGrams = complete.takeIf { it.isNotEmpty() }?.sumOf { it.fatGrams!! },
        )
    }
}
