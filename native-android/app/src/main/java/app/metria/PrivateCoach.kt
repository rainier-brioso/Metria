package app.metria

import java.time.LocalDate

/**
 * Product decision: the coach receives this deliberately small, ephemeral snapshot rather
 * than vault access. It may phrase a supportive insight but never writes or recommends goals.
 */
data class PrivateCoachSnapshot(
    val todayMeals: Int,
    val weekMeals: Int,
    val todayCalories: Double?,
    val todayProtein: Double?,
    val weekCalories: Double?,
    val weekProtein: Double?,
    val calorieGoal: Double?,
    val proteinGoal: Double?,
    val latestWeightKg: Double?,
    val previousWeightKg: Double?,
) {
    fun promptFacts(): String = buildList {
        add("today_meals=$todayMeals")
        add("week_meals=$weekMeals")
        todayCalories?.let { add("today_calories=${it.toInt()}") }
        todayProtein?.let { add("today_protein_g=${it.toInt()}") }
        weekCalories?.let { add("week_calories=${it.toInt()}") }
        weekProtein?.let { add("week_protein_g=${it.toInt()}") }
        calorieGoal?.let { add("member_calorie_goal=${it.toInt()}") }
        proteinGoal?.let { add("member_protein_goal_g=${it.toInt()}") }
        calorieGoal?.let { add("week_calorie_goal=${(it * 7).toInt()}") }
        proteinGoal?.let { add("week_protein_goal_g=${(it * 7).toInt()}") }
        latestWeightKg?.let { add("latest_weight_kg=${"%.1f".format(java.util.Locale.ROOT, it)}") }
        previousWeightKg?.let { add("previous_weight_kg=${"%.1f".format(java.util.Locale.ROOT, it)}") }
    }.joinToString("; ")

    companion object {
        fun from(entries: List<WeightEntry>, meals: List<MealEntry>, targets: NutritionTargets?, today: LocalDate = LocalDate.now(), rangeStart: LocalDate = today.minusDays(6), rangeEnd: LocalDate = today): PrivateCoachSnapshot {
            val todaySummary = MealNutritionSummaries.between(meals, today, today)
            val weekSummary = MealNutritionSummaries.between(meals, rangeStart, rangeEnd)
            val orderedWeights = entries.sortedByDescending(WeightEntry::recordedAt)
            return PrivateCoachSnapshot(
                todayMeals = todaySummary.recordedMeals,
                weekMeals = weekSummary.recordedMeals,
                todayCalories = total(todaySummary.caloriesKcal, todaySummary.partialCaloriesKcal),
                todayProtein = total(todaySummary.proteinGrams, todaySummary.partialProteinGrams),
                weekCalories = total(weekSummary.caloriesKcal, weekSummary.partialCaloriesKcal),
                weekProtein = total(weekSummary.proteinGrams, weekSummary.partialProteinGrams),
                calorieGoal = targets?.caloriesKcal,
                proteinGoal = targets?.proteinGrams,
                latestWeightKg = orderedWeights.getOrNull(0)?.kilograms,
                previousWeightKg = orderedWeights.getOrNull(1)?.kilograms,
            )
        }

        private fun total(complete: Double?, partial: Double?): Double? = when {
            complete == null && partial == null -> null
            else -> (complete ?: 0.0) + (partial ?: 0.0)
        }
    }
}

data class PrivateCoachToolCall(
    val intent: String,
    val period: String? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val kilograms: Double? = null,
    val spokenText: String? = null,
    val daysAgo: Int? = null,
    val fromDaysAgo: Int? = null,
    val toDaysAgo: Int? = null,
    val mealSlot: String? = null,
    val message: String? = null,
    val query: String? = null,
)

fun PrivateCoachSnapshot.goalProgressFacts(period: String?): String {
    val weekly = period == "week"
    val calories = if (weekly) weekCalories else todayCalories
    val protein = if (weekly) weekProtein else todayProtein
    val multiplier = if (weekly) 7 else 1
    val label = if (weekly) "Last 7 days" else "Today"
    val facts = buildList {
        calorieGoal?.let { goal -> add("${"%.0f".format(calories ?: 0.0)} / ${"%.0f".format(goal * multiplier)} kcal") }
        proteinGoal?.let { goal -> add("${"%.0f".format(protein ?: 0.0)} / ${"%.0f".format(goal * multiplier)} g protein") }
    }
    return if (facts.isEmpty()) "$label: no calorie or protein goal is set yet." else "$label: ${facts.joinToString(" · ")}."
}

object PrivateCoachQuestions {
    fun asksYesterdayWeight(text: String): Boolean {
        val normalized = text.lowercase()
        return ("ayer" in normalized || "yesterday" in normalized) &&
            listOf("pes", "weight", "weigh").any { it in normalized }
    }

    fun asksTodaySummary(text: String): Boolean {
        val normalized = text.lowercase()
        return ("today" in normalized || "hoy" in normalized) &&
            listOf("log", "record", "meal", "comida", "calor", "protein", "prote").any { it in normalized }
    }

    fun asksProgress(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("progress", "progreso", "goal", "meta", "objetivo", "week", "semana", "how am i", "como voy", "cómo voy").any { it in normalized }
    }
}
