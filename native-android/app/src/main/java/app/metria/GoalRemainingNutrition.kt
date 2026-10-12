package app.metria

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Validated, deterministic comparison of today's recorded estimates with a confirmed reference. */
object GoalRemainingNutritionResult {
    data class Proof(
        val metric: String,
        val unit: String,
        val actual: BigDecimal?,
        val goal: BigDecimal?,
        val remaining: BigDecimal?,
        val overBy: BigDecimal?,
        val comparison: String,
        val recorded: Int,
        val complete: Int,
        val partial: Int,
        val missing: Int,
        val day: LocalDate,
    )

    fun from(
        meals: List<MealEntry>,
        targets: NutritionTargets?,
        today: LocalDate,
        metric: String,
    ): Map<String, Any?> {
        require(metric in supportedMetrics)
        val summary = MealNutritionSummaries.between(meals, today, today)
        val scale = if (metric == "protein") 1 else 0
        val actualRaw = if (metric == "protein") {
            sum(summary.proteinGrams, summary.partialProteinGrams)
        } else sum(summary.caloriesKcal, summary.partialCaloriesKcal)
        val targetRaw = targets?.takeIf(NutritionTargetValidation::valid)?.let {
            if (metric == "protein") it.proteinGrams else it.caloriesKcal
        }
        // All arithmetic starts from displayed values, so the spoken difference exactly matches
        // the values Metria exposes elsewhere in its nutrition summaries.
        val actual = actualRaw?.let { display(it, scale) }
        val goal = targetRaw?.let { display(it, scale) }
        val comparison = when {
            actual == null -> "no_estimate"
            goal == null -> "no_goal"
            actual > goal -> "over_goal"
            actual == goal -> "at_goal"
            else -> "under_goal"
        }
        val remaining = if (comparison == "under_goal") display(goal!!.subtract(actual!!), scale) else null
        val overBy = if (comparison == "over_goal") display(actual!!.subtract(goal!!), scale) else null
        val included = summary.completeNutritionMeals + summary.partialNutritionMeals
        return linkedMapOf(
            "source" to "metria_local_vault",
            "as_of" to today.toString(),
            "from" to today.toString(),
            "to" to today.toString(),
            "period" to "today",
            "requested_metric" to metric,
            "reply_language" to "English",
            "meals" to linkedMapOf(
                "recorded_count" to summary.recordedMeals,
                "complete_nutrition_count" to summary.completeNutritionMeals,
                "partial_estimate_count" to summary.partialNutritionMeals,
                "missing_nutrition_count" to (summary.recordedMeals - included).coerceAtLeast(0),
            ),
            "nutrition" to linkedMapOf(
                "actual" to actual?.toDouble(),
                "goal" to goal?.toDouble(),
                "remaining" to remaining?.toDouble(),
                "over_by" to overBy?.toDouble(),
                "comparison" to comparison,
            ),
        )
    }

    /** Validates the complete response independently; never trusts summary text or recursively audits. */
    fun proof(response: Map<String, Any?>, expectedDate: LocalDate, metric: String): Proof? =
        runCatching { validate(response, expectedDate, metric) }.getOrNull()

    private fun validate(response: Map<String, Any?>, expectedDate: LocalDate, metric: String): Proof? {
        if (metric !in supportedMetrics || response.keys != responseKeys ||
            response["source"] != "metria_local_vault" || response["as_of"] != expectedDate.toString() ||
            response["from"] != expectedDate.toString() || response["to"] != expectedDate.toString() ||
            response["period"] != "today" || response["requested_metric"] != metric ||
            response["reply_language"] != "English"
        ) return null
        val meals = response["meals"] as? Map<*, *> ?: return null
        if (meals.keys != mealKeys) return null
        val recorded = exactCount(meals["recorded_count"]) ?: return null
        val complete = exactCount(meals["complete_nutrition_count"]) ?: return null
        val partial = exactCount(meals["partial_estimate_count"]) ?: return null
        val missing = exactCount(meals["missing_nutrition_count"]) ?: return null
        if (complete + partial + missing != recorded) return null
        val nutrition = response["nutrition"] as? Map<*, *> ?: return null
        if (nutrition.keys != nutritionKeys) return null
        val scale = if (metric == "protein") 1 else 0
        val actual = decimalOrNull(nutrition["actual"], scale)
        val goal = decimalOrNull(nutrition["goal"], scale)
        val remaining = decimalOrNull(nutrition["remaining"], scale)
        val overBy = decimalOrNull(nutrition["over_by"], scale)
        val maximumGoal = if (metric == "protein") BigDecimal.valueOf(1_000) else BigDecimal.valueOf(10_000)
        if (goal != null && (goal < BigDecimal.ONE || goal > maximumGoal)) return null
        val comparison = nutrition["comparison"] as? String ?: return null
        if ((complete + partial == 0) != (actual == null)) return null
        val expectedComparison = when {
            actual == null -> "no_estimate"
            goal == null -> "no_goal"
            actual > goal -> "over_goal"
            actual == goal -> "at_goal"
            else -> "under_goal"
        }
        if (comparison != expectedComparison) return null
        val expectedRemaining = if (expectedComparison == "under_goal") display(goal!!.subtract(actual!!), scale) else null
        val expectedOver = if (expectedComparison == "over_goal") display(actual!!.subtract(goal!!), scale) else null
        if (remaining != expectedRemaining || overBy != expectedOver) return null
        return Proof(
            metric, if (metric == "protein") "g" else "kcal", actual, goal, remaining, overBy,
            comparison, recorded, complete, partial, missing, expectedDate,
        )
    }

    private fun decimalOrNull(value: Any?, scale: Int): BigDecimal? {
        if (value == null) return null
        val number = value as? Number ?: throw InvalidResult()
        val double = number.toDouble()
        if (!double.isFinite() || double < 0.0) throw InvalidResult()
        val raw = runCatching { BigDecimal(number.toString()) }.getOrNull() ?: throw InvalidResult()
        if (raw.scale() > scale && raw.stripTrailingZeros().scale() > scale) throw InvalidResult()
        val rounded = display(raw, scale)
        if (raw.compareTo(rounded) != 0) throw InvalidResult()
        return rounded
    }

    private fun exactCount(value: Any?): Int? = when (value) {
        is Number -> value.toDouble().takeIf { it.isFinite() && it >= 0 && it <= 10_000 && it % 1.0 == 0.0 }?.toInt()
        else -> null
    }

    private fun sum(complete: Double?, partial: Double?): Double? =
        if (complete == null && partial == null) null else (complete ?: 0.0) + (partial ?: 0.0)

    private fun display(value: Double, scale: Int): BigDecimal = display(BigDecimal.valueOf(value), scale)
    private fun display(value: BigDecimal, scale: Int): BigDecimal = value.setScale(scale, RoundingMode.HALF_UP)

    private val supportedMetrics = setOf("protein", "calories")
    private val responseKeys = setOf(
        "source", "as_of", "from", "to", "period", "requested_metric", "reply_language", "meals", "nutrition",
    )
    private val mealKeys = setOf(
        "recorded_count", "complete_nutrition_count", "partial_estimate_count", "missing_nutrition_count",
    )
    private val nutritionKeys = setOf("actual", "goal", "remaining", "over_by", "comparison")

    private class InvalidResult : RuntimeException()
}

/** Builds only from a validated, same-day typed proof and rechecks Metria's normal fact auditor. */
object GoalRemainingNutritionReply {
    fun from(response: Map<String, Any?>, expectedDate: LocalDate, metric: String): String? {
        val proof = runCatching { GoalRemainingNutritionResult.proof(response, expectedDate, metric) }.getOrNull()
            ?: return null
        val name = if (metric == "protein") "protein" else "calories"
        fun quantity(value: BigDecimal): String = if (metric == "protein") {
            "${value.stripTrailingZeros().toPlainString()} g protein"
        } else {
            "${value.stripTrailingZeros().toPlainString()} kcal"
        }
        val estimate = when {
            proof.actual == null -> "No $name estimate is available from meals recorded today."
            proof.goal == null -> "Metria has no confirmed daily $name goal to compare with today's recorded estimates."
            proof.comparison == "under_goal" -> "About ${quantity(proof.remaining!!)} remain against today's confirmed goal, based on recorded meal estimates."
            proof.comparison == "over_goal" -> "Recorded meal estimates are about ${quantity(proof.overBy!!)} above today's confirmed $name goal."
            else -> "Today's recorded $name estimates are at the confirmed daily goal."
        }
        val coverage = buildList {
            if (proof.partial > 0) add("Some recorded meals have partial nutrition estimates.")
            if (proof.missing > 0) add("Some recorded meals have no nutrition estimate.")
            add("Unrecorded meals are not included; this is not a measure of total intake or a recommendation.")
        }.joinToString(" ")
        val answer = "$estimate $coverage"
        val evidence = MetriaAgentFactEvidence.from("read_goal_remaining_nutrition", response)
        return answer.takeIf { MetriaAgentFactAuditor.audit(it, evidence).acceptedText == it }
    }
}
