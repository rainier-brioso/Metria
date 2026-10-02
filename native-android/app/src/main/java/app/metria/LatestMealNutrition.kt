package app.metria

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

/** One recorded meal only; no notes, photos, item details, goals, or model arithmetic. */
object LatestMealNutritionResult {
    fun from(meals: List<MealEntry>, today: LocalDate, language: String): Map<String, Any?> {
        val zone = ZoneId.systemDefault()
        val candidates = meals.filter { it.recordedAt.atZone(zone).toLocalDate() in today.minusDays(90)..today }
        val latestDay = candidates.maxOfOrNull { it.recordedAt.atZone(zone).toLocalDate() }
        val onDay = candidates.filter { it.recordedAt.atZone(zone).toLocalDate() == latestDay }
        val latest = onDay.maxByOrNull(MealEntry::recordedAt)
        val ambiguous = onDay.size > 1 && (onDay.any { !it.recordedTimeKnown } ||
            onDay.count { it.recordedAt == latest?.recordedAt } > 1)
        val selected = latest?.takeUnless { ambiguous }
        val nutrition = selected?.nutrition
        val coverage = if (nutrition == null) "missing" else nutrition.estimateCoverage
        val calories = nutrition?.caloriesKcal?.displayValue(0)
        val protein = nutrition?.proteinGrams?.displayValue(1)
        return mapOf(
            "source" to "metria_local_vault", "as_of" to today.toString(),
            "from" to today.minusDays(90).toString(), "to" to today.toString(),
            "period" to "latest_meal", "reply_language" to language,
            "status" to when { latest == null -> "empty"; ambiguous -> "ambiguous"; else -> "found" },
            "meal" to selected?.let { mapOf(
                "date" to it.recordedAt.atZone(zone).toLocalDate().toString(),
                "title" to MealIdentity.displayTitle(it).take(320),
            ) },
            "estimate_coverage" to coverage,
            "nutrition" to mapOf("calories_kcal" to calories, "protein_g" to protein,
                "partial_calories_kcal" to calories.takeIf { coverage == "partial" },
                "partial_protein_g" to protein.takeIf { coverage == "partial" }),
        )
    }

    private fun Double.displayValue(scale: Int): Double? = takeIf { it.isFinite() && it in 0.0..10_000.0 }
        ?.let { BigDecimal.valueOf(it).setScale(scale, RoundingMode.HALF_UP).toDouble() }
}

/** Validated host answer avoids confusing one meal's estimate with a whole day's totals. */
object LatestMealNutritionReply {
    fun from(response: Map<String, Any?>): String? {
        if (response["source"] != "metria_local_vault" || response["period"] != "latest_meal") return null
        val today = (response["as_of"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        if (response["from"] != today.minusDays(90).toString() || response["to"] != today.toString()) return null
        val spanish = when (response["reply_language"]) { "Spanish" -> true; "English" -> false; else -> return null }
        val nutrition = response["nutrition"] as? Map<*, *> ?: return null
        val coverage = response["estimate_coverage"] as? String ?: return null
        val rawCalories = nutrition["calories_kcal"]
        val rawProtein = nutrition["protein_g"]
        val calories = number(rawCalories, 0)
        val protein = number(rawProtein, 1)
        if ((rawCalories != null && calories == null) || (rawProtein != null && protein == null)) return null
        val reply = when (response["status"]) {
            "empty", "ambiguous" -> {
                if (response["meal"] != null || coverage != "missing" || rawCalories != null || rawProtein != null) return null
                if (response["status"] == "empty") {
                    if (spanish) "No hay comidas registradas en los últimos 90 días."
                    else "No meals are recorded within the last 90 days."
                } else {
                    if (spanish) "No puedo distinguir cuál fue la última comida con las horas registradas. Consulta las comidas de ese día."
                    else "The recorded times do not identify a single latest meal. Check that day's meals."
                }
            }
            "found" -> {
                val meal = response["meal"] as? Map<*, *> ?: return null
                val date = (meal["date"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
                if (date !in today.minusDays(90)..today) return null
                val title = (meal["title"] as? String)?.trim()?.takeIf { it.length in 1..320 } ?: return null
                if (coverage !in setOf("full", "partial", "missing")) return null
                if (coverage == "missing" && (rawCalories != null || rawProtein != null)) return null
                if (coverage == "partial" && (nutrition["partial_calories_kcal"] != rawCalories || nutrition["partial_protein_g"] != rawProtein)) return null
                val heading = if (spanish) "Tu última comida registrada: $title ($date)." else "Your latest recorded meal: $title ($date)."
                val values = listOf(
                    if (calories == null) { if (spanish) "Sin estimación de calorías guardada." else "No saved calorie estimate." }
                    else { if (spanish) "Estimación guardada: $calories kcal." else "Saved estimate: $calories kcal." },
                    if (protein == null) { if (spanish) "Sin estimación de proteína guardada." else "No saved protein estimate." }
                    else { if (spanish) "Estimación guardada: $protein g de proteína." else "Saved estimate: $protein g protein." },
                ).joinToString(" ")
                val partial = if (coverage != "partial") "" else if (spanish)
                    "\nEstimación parcial: solo incluye los componentes calculados." else
                    "\nPartial estimate: only calculated components are included."
                "$heading\n$values$partial"
            }
            else -> return null
        }
        // The search bound is a host fact, explicitly included for numeric auditing.
        val evidence = MetriaAgentFactEvidence.from("read_latest_meal", response + ("search_days" to 90))
        return reply.takeIf { MetriaAgentFactAuditor.audit(it, evidence).acceptedText == it }
    }

    private fun number(value: Any?, scale: Int): String? = (value as? Number)?.toDouble()
        ?.takeIf { it.isFinite() && it in 0.0..10_000.0 }
        ?.let { BigDecimal.valueOf(it).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }
}
