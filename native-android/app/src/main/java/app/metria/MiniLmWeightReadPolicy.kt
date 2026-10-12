package app.metria

import java.time.LocalDate

/** Closed English requests for bounded recent weight-history reads. */
internal object MiniLmWeightReadPolicy {
    enum class Kind(val metric: String, val fromDaysAgo: Int) {
        LATEST("latest_weight", 90),
        LAST_WEIGHED("last_weighed", 90),
        RECENT("recent_weigh_ins", 29),
    }

    private val patterns = mapOf(
        Kind.LATEST to listOf(
            Regex("^(?:what is|what's) (?:my|the) (?:latest|most recent|last recorded) weight$"),
            Regex("^(?:show|tell) me (?:my|the) (?:latest|most recent|last recorded) weight$"),
            Regex("^let me see (?:my|the) (?:latest|most recent|last recorded) weight$"),
        ),
        Kind.LAST_WEIGHED to listOf(
            Regex("^when did i last (?:weigh|weight) myself$"),
            Regex("^when was my last weigh[- ]in$"),
            Regex("^when was my most recent weigh[- ]in$"),
            Regex("^when did i last record my weight$"),
        ),
        Kind.RECENT to listOf(
            Regex("^(?:show|list) my recent weigh[- ]ins$"),
            Regex("^(?:show|list) my weigh[- ]ins from the last 30 days$"),
            Regex("^let me see my recent weigh[- ]ins$"),
        ),
    )

    data class ResolvedRead(val requiredTool: RequiredAgentTool, val classifierText: String)

    fun kind(text: String): Kind? {
        if (!validText(text)) return null
        val normalized = normalize(text)
        return patterns.entries.singleOrNull { (_, candidates) -> candidates.any { it.matches(normalized) } }?.key
    }

    fun isInScope(text: String): Boolean = kind(text) != null

    fun resolve(hostRequiredTool: RequiredAgentTool?, text: String, selectedMealId: String?, today: LocalDate): ResolvedRead? {
        if (!selectedMealId.isNullOrBlank()) return null
        val kind = kind(text) ?: return null
        val required = RequiredAgentTool(
            name = "read_weight", metric = kind.metric,
            fromDaysAgo = kind.fromDaysAgo, toDaysAgo = 0, targetDate = today,
        )
        if (hostRequiredTool == null) return ResolvedRead(required, normalize(text))
        val compatible = hostRequiredTool.name == "read_weight" &&
            hostRequiredTool.period == null && hostRequiredTool.daysAgo == null &&
            hostRequiredTool.fromDaysAgo == kind.fromDaysAgo && hostRequiredTool.toDaysAgo == 0 &&
            hostRequiredTool.query == null && hostRequiredTool.metric == null &&
            hostRequiredTool.selectedMealId == null && hostRequiredTool.selectedMealQuestion == null &&
            (hostRequiredTool.targetDate == null || hostRequiredTool.targetDate == today)
        return if (compatible) ResolvedRead(required, normalize(text)) else null
    }

    /** Drops only the new fast-read as-of anchor; exact-day historical host reads stay untouched. */
    fun gemmaFallbackRequiredTool(resolved: ResolvedRead?): RequiredAgentTool? =
        resolved?.requiredTool?.takeIf { it.name == "read_weight" }?.copy(targetDate = null)

    fun validatedReply(requiredTool: RequiredAgentTool, response: Map<String, Any?>): String? {
        val kind = Kind.entries.singleOrNull { it.metric == requiredTool.metric } ?: return null
        val asOf = requiredTool.targetDate ?: return null
        val from = asOf.minusDays(kind.fromDaysAgo.toLong())
        if (requiredTool.name != "read_weight" || requiredTool.fromDaysAgo != kind.fromDaysAgo ||
            requiredTool.toDaysAgo != 0 || requiredTool.daysAgo != null || requiredTool.period != null ||
            requiredTool.query != null || requiredTool.selectedMealId != null || requiredTool.selectedMealQuestion != null ||
            response["source"] != "metria_local_vault" || response["reply_language"] != "English" ||
            response["as_of"] != asOf.toString() || response["from"] != from.toString() ||
            response["to"] != asOf.toString()
        ) return null
        val total = response.integerCount("total_count") ?: return null
        val returned = response.integerCount("returned_count") ?: return null
        val omitted = response.integerCount("omitted_count") ?: return null
        if (total != returned + omitted || returned > MetriaAgentResultBounds.maxRecords ||
            response["coverage"] != if (omitted == 0) "full" else "latest_records_only"
        ) return null
        val entries = response["entries"] as? List<*> ?: return null
        if (entries.size != returned) return null
        val records = entries.map { raw ->
            val row = raw as? Map<*, *> ?: return null
            val dateText = row["date"] as? String ?: return null
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return null
            val kg = (row["kilograms"] as? Number)?.toDouble()?.takeIf { it.isFinite() && it > 0.0 && it <= 1000.0 }
                ?: return null
            if (date !in from..asOf) return null
            date to kg
        }
        if (records.zipWithNext().any { (a, b) -> a.first > b.first }) return null
        if (omitted > 0 && returned != MetriaAgentResultBounds.maxRecords) return null
        val answer = when (kind) {
            Kind.LATEST, Kind.LAST_WEIGHED -> {
                val latest = records.lastOrNull()
                if (latest == null) "No weight entries were saved within the read window. Read window: from $from through $asOf inclusive."
                else if (kind == Kind.LAST_WEIGHED) {
                    "Your last saved weigh-in in the read window was on ${latest.first} in your records. Read window: from $from through $asOf inclusive." +
                        if (omitted > 0) " Older entries are not shown." else ""
                } else {
                    "Your latest saved weight in the read window was ${formatRecordedKilograms(latest.second)} kg, recorded on ${latest.first} in your records. Read window: from $from through $asOf inclusive." +
                        if (omitted > 0) " Older entries are not shown." else ""
                }
            }
            Kind.RECENT -> when {
                records.isEmpty() -> "No weight entries were saved within the read window. Read window: from $from through $asOf inclusive."
                else -> buildString {
                    append("Read window: from $from through $asOf inclusive. ")
                    append(records.joinToString(" ") { (date, kg) -> "Weigh-in on $date: ${formatRecordedKilograms(kg)} kg." })
                    if (omitted > 0) append(" $total weight entries were recorded in this window. $omitted weight entries were omitted.")
                }.toString()
            }
        }
        return answer.takeIf { MetriaAgentFactAuditor.audit(it, MetriaAgentFactEvidence.from("read_weight", response)).acceptedText == it }
    }

    private fun Map<String, Any?>.integerCount(key: String): Int? {
        val value = this[key] as? Number ?: return null
        val number = value.toDouble()
        return number.toInt().takeIf { number.isFinite() && number >= 0.0 && number == it.toDouble() }
    }

    private fun validText(text: String): Boolean = text.isNotBlank() && text.length <= 500 &&
        text.none { it.code < 0x20 && !it.isWhitespace() }

    private fun normalize(text: String): String = MiniLmReadTextNormalization.normalize(text)
}
