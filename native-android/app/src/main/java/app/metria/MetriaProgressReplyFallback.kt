package app.metria

/** A bounded record summary when model wording fails; it passes the same final factual audit. */
object MetriaProgressReplyFallback {
    fun from(
        toolName: String,
        requiredTool: RequiredAgentTool?,
        response: Map<String, Any?>,
    ): String? {
        if (toolName != "read_progress_overview" || response["source"] != "metria_local_vault") return null
        val evidence = MetriaAgentFactEvidence.from(toolName, response)
        val period = evidence.period?.takeIf { it in setOf("today", "week") } ?: return null
        if (requiredTool != null && (requiredTool.name != toolName || requiredTool.period != period)) return null
        val range = evidence.responseDateRange ?: return null
        val expectedDays = if (period == "week") 7 else 1
        if (range.start != range.endInclusive.minusDays((expectedDays - 1).toLong())) return null
        // Do not accept reversed dates hidden by the evidence object's normalized range.
        if (response["from"] != range.start.toString() || response["to"] != range.endInclusive.toString()) return null
        val meals = response["meals"] as? Map<*, *> ?: return null
        val coverage = response["coverage"] as? Map<*, *> ?: return null
        val weight = response["weight"] as? Map<*, *> ?: return null
        val nutrition = response["nutrition"] as? Map<*, *> ?: return null
        if (coverage["interpretation"] != "recorded_meals_only_not_total_intake") return null
        if (count(coverage["period_days"]) != expectedDays) return null
        val recorded = count(meals["recorded_count"]) ?: return null
        val complete = count(meals["complete_nutrition_count"]) ?: return null
        val partial = count(meals["partial_estimate_count"]) ?: return null
        val missing = count(meals["missing_nutrition_count"]) ?: return null
        if (complete.toLong() + partial + missing != recorded.toLong()) return null
        val days = count(coverage["days_with_recorded_meals"]) ?: return null
        if (days > expectedDays || days > recorded || (days == 0) != (recorded == 0)) return null
        val weights = count(weight["entry_count"]) ?: return null
        if (!nutrition.containsKey("calories_kcal") || !nutrition.containsKey("protein_g")) return null
        val calories = value(nutrition["calories_kcal"])
        val protein = value(nutrition["protein_g"])
        if ((complete + partial > 0) != (calories != null && protein != null)) return null
        if (nutrition["calories_kcal"] != null && calories == null ||
            nutrition["protein_g"] != null && protein == null) return null
        val spanish = when (response["reply_language"]) {
            "Spanish" -> true
            "English" -> false
            else -> return null
        }
        val label = if (spanish) {
            if (period == "week") "En los últimos 7 días" else "Hoy"
        } else {
            if (period == "week") "In the last 7 days" else "Today"
        }
        val sentences = buildList {
            add(if (spanish) "$label has registrado $recorded comidas." else "$label you recorded $recorded meals.")
            if (calories != null && protein != null) {
                add(if (spanish) "Las estimaciones disponibles suman $calories kcal." else "Available estimates total $calories kcal.")
                add(if (spanish) "La estimación de proteína es $protein g de proteína." else "The protein estimate is $protein g protein.")
            } else {
                add(if (spanish) "No hay estimaciones nutricionales disponibles para ese periodo." else "No nutrition estimates are available for that period.")
            }
            add(if (spanish) "Tienes $weights registros de peso en ese periodo." else "You have $weights weight entries in that period.")
            if (partial > 0) add(if (spanish) "Algunas comidas tienen estimaciones parciales." else "Some meals have partial estimates.")
            if (missing > 0) add(if (spanish) "Algunas comidas registradas no tienen estimación nutricional." else "Some recorded meals have no nutrition estimate.")
            add(if (spanish) "Las comidas registradas pueden no representar toda tu ingesta." else "Recorded meals may not represent your total intake.")
        }
        val answer = sentences.joinToString(" ")
        return answer.takeIf { MetriaAgentFactAuditor.audit(it, evidence).acceptedText == it }
    }

    private fun count(value: Any?): Int? {
        val number = (value as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 && it <= Int.MAX_VALUE } ?: return null
        return number.toInt().takeIf { it.toDouble() == number }
    }

    private fun value(value: Any?): String? {
        val number = (value as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 } ?: return null
        return java.math.BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()
    }
}
