package app.metria

import java.math.BigDecimal
import java.math.RoundingMode

/** Deterministic English answer for the bounded, fresh overview of today. */
object TodayOverviewReply {
    fun from(requiredTool: RequiredAgentTool, response: Map<String, Any?>): String? {
        val trustedDate = requiredTool.targetDate ?: return null
        if (requiredTool.name != "read_progress_overview" || requiredTool.period != "today" ||
            requiredTool.query != null || requiredTool.metric != null || requiredTool.daysAgo != null ||
            requiredTool.fromDaysAgo != null || requiredTool.toDaysAgo != null ||
            requiredTool.selectedMealId != null || requiredTool.selectedMealQuestion != null
        ) return null

        if (response["source"] != "metria_local_vault" || response["period"] != "today" ||
            response["reply_language"] != "English" || response["as_of"] != trustedDate.toString() ||
            response["from"] != trustedDate.toString() || response["to"] != trustedDate.toString() ||
            response.containsKey("requested_metric")
        ) return null

        // The existing fallback is the canonical validator for overview counts, coverage,
        // estimate presence, date range, and the base response's factual consistency.
        if (MetriaProgressReplyFallback.from("read_progress_overview", requiredTool, response) == null) return null

        val nutrition = response["nutrition"] as? Map<*, *> ?: return null
        val calories = displayNumber(nutrition["calories_kcal"], maxDecimalPlaces = 0, minimum = 0.0, maximum = null)
        val protein = displayNumber(nutrition["protein_g"], maxDecimalPlaces = 1, minimum = 0.0, maximum = null)
        if ((nutrition["calories_kcal"] != null) != (calories != null) ||
            (nutrition["protein_g"] != null) != (protein != null)
        ) return null

        if (!nutrition.containsKey("calorie_goal_kcal") || !nutrition.containsKey("protein_goal_g")) return null
        val calorieGoal = nutrition["calorie_goal_kcal"]?.let {
            goal(it, "calories", maxDecimalPlaces = 0, maximum = 10_000.0) ?: return null
        }
        val proteinGoal = nutrition["protein_goal_g"]?.let {
            goal(it, "protein", maxDecimalPlaces = 1, maximum = 1_000.0) ?: return null
        }

        val meals = response["meals"] as? Map<*, *> ?: return null
        val partial = exactCount(meals["partial_estimate_count"]) ?: return null
        val missing = exactCount(meals["missing_nutrition_count"]) ?: return null
        val evidence = MetriaAgentFactEvidence.from("read_progress_overview", response)

        val sentences = buildList {
            // Keep the count explicit for an empty day; it is a recorded-meal count, not intake.
            val recorded = exactCount(meals["recorded_count"]) ?: return null
            add("Today you recorded $recorded meals.")
            if (calories != null && protein != null) {
                add("Available calorie estimates today total ${format(calories)} kcal.")
                add("Available protein estimates today total ${format(protein)} g protein.")
            } else {
                add("No calorie or protein estimates are available today.")
            }
            if (calorieGoal != null) add("Your daily calorie goal is ${format(calorieGoal)} kcal.")
            else add("No daily calorie goal is set.")
            if (proteinGoal != null) add("Your daily protein goal is ${format(proteinGoal)} g protein.")
            else add("No daily protein goal is set.")
            if (partial > 0) add("Some recorded meals have partial nutrition estimates.")
            if (missing > 0) add("Some recorded meals have no nutrition estimate.")
            add("Recorded meals may not represent your total intake.")
        }
        val answer = sentences.joinToString(" ")
        return answer.takeIf { MetriaAgentFactAuditor.audit(it, evidence).acceptedText == it }
    }

    private fun goal(
        raw: Any,
        metric: String,
        maxDecimalPlaces: Int,
        maximum: Double,
    ): Double? = displayNumber(raw, maxDecimalPlaces, minimum = 1.0, maximum = maximum)
        ?.takeIf { value ->
            val targets = if (metric == "calories") NutritionTargets(caloriesKcal = value)
            else NutritionTargets(proteinGrams = value)
            NutritionTargetValidation.valid(targets) && !NutritionTargetValidation.empty(targets)
        }

    private fun displayNumber(value: Any?, maxDecimalPlaces: Int, minimum: Double, maximum: Double?): Double? {
        val number = (value as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= minimum } ?: return null
        if (maximum != null && number > maximum) return null
        val decimal = BigDecimal.valueOf(number).stripTrailingZeros()
        if (decimal.scale().coerceAtLeast(0) > maxDecimalPlaces) return null
        val rounded = BigDecimal.valueOf(number).setScale(maxDecimalPlaces, RoundingMode.HALF_UP)
        return number.takeIf { BigDecimal.valueOf(it).compareTo(rounded) == 0 }
    }

    private fun exactCount(value: Any?): Int? {
        val number = (value as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 && it <= Int.MAX_VALUE } ?: return null
        return number.toInt().takeIf { it.toDouble() == number }
    }

    private fun format(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
