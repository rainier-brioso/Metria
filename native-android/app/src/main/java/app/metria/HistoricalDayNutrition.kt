package app.metria

import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Machine-readable, exact-day nutrition result. Goals are deliberately not part of historical evidence. */
object HistoricalDayNutritionResult {
    fun from(meals: List<MealEntry>, today: LocalDate, daysAgo: Int, requestedMetric: String? = null): Map<String, Any?> {
        require(daysAgo in 0..90)
        require(requestedMetric == null || requestedMetric in setOf("protein", "calories"))
        val day = today.minusDays(daysAgo.toLong())
        val summary = MealNutritionSummaries.between(meals, day, day)
        val missingNutrition = summary.recordedMeals - summary.completeNutritionMeals - summary.partialNutritionMeals
        val nutrition = mapOf(
            "calories_kcal" to recordedEstimateTotal(summary.caloriesKcal, summary.partialCaloriesKcal)?.quantized(0),
            "protein_g" to recordedEstimateTotal(summary.proteinGrams, summary.partialProteinGrams)?.quantized(1),
            "complete_calories_kcal" to summary.caloriesKcal?.quantized(0),
            "complete_protein_g" to summary.proteinGrams?.quantized(1),
            "partial_calories_kcal" to summary.partialCaloriesKcal?.quantized(0),
            "partial_protein_g" to summary.partialProteinGrams?.quantized(1),
        )
        return mapOf(
            "source" to "metria_local_vault",
            "as_of" to today.toString(),
            "period" to "historical_day",
            "days_ago" to daysAgo,
            "requested_metric" to requestedMetric,
            "from" to day.toString(),
            "to" to day.toString(),
            "meals" to mapOf(
                "recorded_count" to summary.recordedMeals,
                "complete_nutrition_count" to summary.completeNutritionMeals,
                "partial_estimate_count" to summary.partialNutritionMeals,
                "missing_nutrition_count" to missingNutrition,
            ),
            "nutrition" to nutrition,
            "interpretation" to "recorded_meals_only_not_total_intake",
        )
    }

    private fun recordedEstimateTotal(complete: Double?, partial: Double?): Double? = when {
        complete == null && partial == null -> null
        else -> (complete ?: 0.0) + (partial ?: 0.0)
    }

    private fun Double.quantized(scale: Int): Double = BigDecimal.valueOf(this)
        .setScale(scale, RoundingMode.HALF_UP)
        .toDouble()
}

/** Concise member-facing disclosure for the expandable exact-day evidence card. */
object HistoricalDayNutritionEvidenceText {
    fun format(result: Map<String, Any?>, language: String): String {
        val spanish = language == "Spanish"
        val rawDate = result["from"]?.toString().orEmpty()
        val date = runCatching {
            val parsed = LocalDate.parse(rawDate)
            parsed.format(DateTimeFormatter.ofPattern(
                if (spanish) "d 'de' MMMM 'de' yyyy" else "MMMM d, yyyy",
                if (spanish) Locale.forLanguageTag("es") else Locale.ENGLISH,
            ))
        }.getOrDefault(rawDate)
        val meals = result["meals"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val nutrition = result["nutrition"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val recorded = (meals["recorded_count"] as? Number)?.toInt() ?: 0
        val complete = (meals["complete_nutrition_count"] as? Number)?.toInt() ?: 0
        val partial = (meals["partial_estimate_count"] as? Number)?.toInt() ?: 0
        val missing = (meals["missing_nutrition_count"] as? Number)?.toInt() ?: 0
        val coverage = if (spanish) {
            "estimaciones completas: $complete, parciales: $partial, sin estimación: $missing"
        } else {
            "complete estimates: $complete, partial: $partial, no estimate: $missing"
        }
        val metric = result["requested_metric"] as? String
        val metricText = when (metric) {
            "calories" -> nutrition["calories_kcal"]?.let { if (spanish) "Estimación de calorías: ${displayNumber(it, 0)} kcal." else "Calorie estimate: ${displayNumber(it, 0)} kcal." }
            "protein" -> nutrition["protein_g"]?.let { if (spanish) "Estimación de proteína: ${displayNumber(it, 1)} g." else "Protein estimate: ${displayNumber(it, 1)} g." }
            else -> null
        }
        val head = if (spanish) {
            "$date · $recorded ${if (recorded == 1) "comida registrada" else "comidas registradas"}; $coverage."
        } else {
            "$date · $recorded ${if (recorded == 1) "recorded meal" else "recorded meals"}; $coverage."
        }
        val caveat = if (spanish) "Solo incluye comidas registradas; las demás se desconocen." else "Recorded meals only; other meals are unknown."
        return listOfNotNull(head, metricText, caveat).joinToString(" ")
    }

    private fun displayNumber(value: Any, scale: Int): String {
        val number = value.toString().toBigDecimalOrNull() ?: return value.toString()
        return number.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}
