package app.metria

import java.time.LocalDate

/** Answer an explicit day meal-list request only from its current validated host result. */
object MetriaMealRecordReply {
    fun from(required: RequiredAgentTool, response: Map<String, Any?>): String? {
        if (required.name != "read_meals" || response["source"] != "metria_local_vault") return null
        val daysAgo = required.daysAgo?.takeIf { it in 0..90 } ?: return null
        val asOf = (response["as_of"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val day = asOf.minusDays(daysAgo.toLong())
        if (required.targetDate != null && required.targetDate != day) return null
        if (response["from"] != day.toString() || response["to"] != day.toString() || response["meal_slot"] != null) return null
        val total = count(response["total_count"]) ?: return null
        val returned = count(response["returned_count"]) ?: return null
        val omitted = count(response["omitted_count"]) ?: return null
        val meals = response["meals"] as? List<*> ?: return null
        if (returned != meals.size || returned > MetriaAgentResultBounds.maxRecords || total != returned + omitted) return null
        if (response["coverage"] != if (omitted == 0) "full" else "latest_records_only") return null
        if (omitted > 0 && returned != MetriaAgentResultBounds.maxRecords) return null
        val titles = meals.map { value ->
            val row = value as? Map<*, *> ?: return null
            if (row["date"] != day.toString()) return null
            (row["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= 320 } ?: return null
        }
        val spanish = when (response["reply_language"]) {
            "Spanish" -> true
            "English" -> false
            else -> return null
        }
        val reply = if (titles.isEmpty()) {
            if (spanish) "No hay comidas registradas el $day en tus registros."
            else "No meals are recorded for $day in your records."
        } else {
            val heading = if (spanish) "Comidas registradas el $day:" else "Meals recorded for $day:"
            val partial = if (omitted == 0) "" else if (spanish) "\nLista parcial: se muestran las más recientes."
                else "\nPartial list: only the most recent records are shown."
            heading + "\n" + titles.joinToString("\n") { "- $it" } + partial
        }
        val evidence = MetriaAgentFactEvidence.from("read_meals", response)
        return reply.takeIf { MetriaAgentFactAuditor.audit(it, evidence).acceptedText == it }
    }

    private fun count(value: Any?): Int? = (value as? Number)?.toDouble()?.takeIf {
        it.isFinite() && it in 0.0..1_000_000.0 && it % 1.0 == 0.0
    }?.toInt()
}
