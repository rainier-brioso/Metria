package app.metria

import java.time.LocalDate
import java.time.ZoneId
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/** Display-only estimates; keep vault values and deterministic arithmetic at full precision. */
private fun Double.nutritionDisplayValue(scale: Int): Double = BigDecimal.valueOf(this)
    .setScale(scale, RoundingMode.HALF_UP)
    .toDouble()

private fun Double.nutritionDisplayText(scale: Int): String = BigDecimal.valueOf(this)
    .setScale(scale, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

/** Member-visible weight keeps the recorded precision up to two decimal places. */
internal fun formatRecordedKilograms(value: Double): String = BigDecimal.valueOf(value)
    .setScale(2, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

internal fun formatSignedRecordedKilograms(value: Double): String =
    if (value > 0.0) "+${formatRecordedKilograms(value)}" else formatRecordedKilograms(value)

/** Goal comparisons are host-calculated so the model never performs personal-data arithmetic. */
internal fun goalProgressPercent(actual: Double?, goal: Double?): Int? = when {
    actual == null || goal == null || !actual.isFinite() || !goal.isFinite() || goal <= 0.0 -> null
    else -> ((actual / goal) * 100.0).roundToInt().coerceAtLeast(0)
}

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
    val grams: Int? = null,
    /** Host-only frozen local reference/as-of day; `daysAgo` is applied once to select the MiniLM read day. */
    val hostReadDate: LocalDate? = null,
    /** Host-authorized intent discriminator; never parsed from model arguments. */
    val hostCompanionKind: CompanionAssistantPolicy.IntentKind? = null,
    /** Host-selected metric for a goal comparison; model tool schemas do not expose this field. */
    val hostMetric: String? = null,
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

/** Machine-readable selected-period facts for grounded assistant prose. */
fun PrivateCoachSnapshot.selectedNutritionFacts(period: String?): String {
    val weekly = period == "week"
    val multiplier = if (weekly) 7 else 1
    return buildList {
        add("selected_period=${if (weekly) "last_7_days" else "today"}")
        add("logged_meals=${if (weekly) weekMeals else todayMeals}")
        (if (weekly) weekCalories else todayCalories)?.let { add("calories_kcal=${it.toInt()}") }
        (if (weekly) weekProtein else todayProtein)?.let { add("protein_g=${it.toInt()}") }
        calorieGoal?.let { add("calorie_goal_kcal=${(it * multiplier).toInt()}") }
        proteinGoal?.let { add("protein_goal_g=${(it * multiplier).toInt()}") }
    }.joinToString("; ")
}

/** One bounded, deterministic source for broad progress questions. */
data class MemberProgressOverview(
    val period: String,
    val from: LocalDate,
    val to: LocalDate,
    val periodDays: Int,
    val recordedMeals: Int,
    val daysWithRecordedMeals: Int,
    val completeNutritionMeals: Int,
    val partialNutritionMeals: Int,
    val mealsWithoutNutrition: Int,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val calorieGoalKcal: Double?,
    val proteinGoalGrams: Double?,
    val weightEntryCount: Int,
    val firstWeightKg: Double?,
    val latestWeightKg: Double?,
) {
    val weightChangeKg: Double? = if (weightEntryCount >= 2 && firstWeightKg != null && latestWeightKg != null) {
        latestWeightKg - firstWeightKg
    } else {
        null
    }

    fun localEvidence(): String {
        val label = if (period == "week") "Last 7 days" else "Today"
        val nutrition = buildList {
            caloriesKcal?.let { calories ->
                add("${calories.nutritionDisplayText(0)}${calorieGoalKcal?.let { " / ${it.nutritionDisplayText(0)}" }.orEmpty()} kcal")
            }
            proteinGrams?.let { protein ->
                add("${protein.nutritionDisplayText(1)}${proteinGoalGrams?.let { " / ${it.nutritionDisplayText(1)}" }.orEmpty()} g protein")
            }
        }.joinToString(" · ").ifBlank { "no complete nutrition values" }
        val weight = when {
            latestWeightKg == null -> "no weight entries"
            weightChangeKg == null -> "${formatKg(latestWeightKg)} kg from one entry"
            else -> "${formatKg(firstWeightKg!!)} to ${formatKg(latestWeightKg)} kg (${formatSignedKg(weightChangeKg)} kg)"
        }
        val mealCoverage = if (period == "week") {
            "$recordedMeals meals across $daysWithRecordedMeals of $periodDays days"
        } else {
            "$recordedMeals meals"
        }
        return "$label: $mealCoverage. Recorded estimates: $nutrition. Weight: $weight. Logged meals may not represent total intake."
    }

    fun toolResponse(): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault",
        "period" to if (period == "week") "last_7_days_including_today" else "today",
        "from" to from.toString(),
        "to" to to.toString(),
        "coverage" to mapOf(
            "period_days" to periodDays,
            "days_with_recorded_meals" to daysWithRecordedMeals,
            "interpretation" to "recorded_meals_only_not_total_intake",
        ),
        "meals" to mapOf(
            "recorded_count" to recordedMeals,
            "complete_nutrition_count" to completeNutritionMeals,
            "partial_estimate_count" to partialNutritionMeals,
            "missing_nutrition_count" to mealsWithoutNutrition,
        ),
        "nutrition" to mapOf(
            "calories_kcal" to caloriesKcal?.nutritionDisplayValue(0),
            "protein_g" to proteinGrams?.nutritionDisplayValue(1),
            "calorie_goal_kcal" to calorieGoalKcal?.nutritionDisplayValue(0),
            "protein_goal_g" to proteinGoalGrams?.nutritionDisplayValue(1),
        ),
        "progress" to mapOf(
            "calorie_goal_percent" to goalProgressPercent(caloriesKcal, calorieGoalKcal),
            "protein_goal_percent" to goalProgressPercent(proteinGrams, proteinGoalGrams),
        ),
        "weight" to mapOf(
            "entry_count" to weightEntryCount,
            "first_kg" to firstWeightKg,
            "latest_kg" to latestWeightKg,
            "change_kg" to weightChangeKg,
        ),
        "summary" to localEvidence(),
    )

    companion object {
        fun from(
            entries: List<WeightEntry>,
            meals: List<MealEntry>,
            targets: NutritionTargets?,
            today: LocalDate,
            period: String,
        ): MemberProgressOverview {
            require(period in setOf("today", "week"))
            val from = if (period == "week") today.minusDays(6) else today
            val summary = MealNutritionSummaries.between(meals, from, today)
            val inRangeMeals = meals.filter { meal ->
                meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in from..today
            }
            val includedNutrition = summary.completeNutritionMeals + summary.partialNutritionMeals
            val weights = entries.filter { entry ->
                entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in from..today
            }.sortedBy(WeightEntry::recordedAt)
            val multiplier = if (period == "week") 7 else 1
            return MemberProgressOverview(
                period = period,
                from = from,
                to = today,
                periodDays = if (period == "week") 7 else 1,
                recordedMeals = summary.recordedMeals,
                daysWithRecordedMeals = inRangeMeals
                    .map { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
                    .distinct()
                    .size,
                completeNutritionMeals = summary.completeNutritionMeals,
                partialNutritionMeals = summary.partialNutritionMeals,
                mealsWithoutNutrition = (summary.recordedMeals - includedNutrition).coerceAtLeast(0),
                caloriesKcal = total(summary.caloriesKcal, summary.partialCaloriesKcal),
                proteinGrams = total(summary.proteinGrams, summary.partialProteinGrams),
                calorieGoalKcal = targets?.caloriesKcal?.times(multiplier),
                proteinGoalGrams = targets?.proteinGrams?.times(multiplier),
                weightEntryCount = weights.size,
                firstWeightKg = weights.firstOrNull()?.kilograms,
                latestWeightKg = weights.lastOrNull()?.kilograms,
            )
        }

        private fun total(complete: Double?, partial: Double?): Double? = when {
            complete == null && partial == null -> null
            else -> (complete ?: 0.0) + (partial ?: 0.0)
        }

        private fun formatKg(value: Double): String = formatRecordedKilograms(value)

        private fun formatSignedKg(value: Double): String = formatSignedRecordedKilograms(value)
    }
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
