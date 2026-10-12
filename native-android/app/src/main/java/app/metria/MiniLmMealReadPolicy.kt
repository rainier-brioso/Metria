package app.metria

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/** Host-side, closed English grammars for optional local meal reads. */
internal object MiniLmMealReadPolicy {
    enum class FollowUpOperation { TODAY_NUTRITION, LATEST_MEAL }
    data class FollowUpScope(val operation: FollowUpOperation, val referenceDate: LocalDate)
    data class ResolvedRead(
        val requiredTool: RequiredAgentTool,
        val classifierText: String,
        val allowMiniLm: Boolean = true,
    )

    private val day = "(?:today|yesterday)"
    private val readPatterns = listOf(
        Regex("^(?:please )?remind me what i (?:ate|had) $day$"),
        Regex("^(?:could you |can you )?(?:please )?remind me what i (?:ate|had) $day$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:(?:all|my|the) )?(?:(?:saved|recorded|logged) )?(?:the )?(?:meals?|foods?) (?:(?:i (?:recorded|logged|saved)) )?(?:(?:for|from) )?$day$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:all )?(?:saved|recorded|logged) (?:meals?|foods?) for $day$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:(?:all|my|the) )?$day's (?:(?:saved|recorded|logged) )?(?:meals?|foods?)$"),
        Regex("^(?:please )?(?:what|which) (?:meals?|foods?) (?:have i (?:logged|saved|recorded)|i (?:logged|saved|recorded)|are in my record) (?:for )?$day$"),
        Regex("^(?:please )?(?:what|which) meals? (?:are|were) (?:recorded|logged|saved) $day$"),
        Regex("^(?:please )?what did i (?:eat|have) $day$"),
        Regex("^(?:please )?what did i (?:log|record|save) for meals? $day$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:my )?meals? for $day$"),
    )
    private val latestPatterns = listOf(
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:what (?:were|are|was|is) the )?(?:calories|protein) (?:in|from) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:how many calories|how much protein) (?:were|was|are|is) in my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:(?:tell|show) me )?how many calories (?:were|are|was|is) (?:in|from) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:(?:tell|show) me )?how much protein (?:was|is|were|are) (?:in|from) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:(?:tell|show) me )?(?:what were|what are|what was|what is) the (?:calories|protein) (?:in|for) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?what (?:were|are|was|is) the (?:calories|protein) (?:in|for) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:tell|show) me (?:the )?(?:calories|protein) (?:in|from) my (?:last|latest|most recent) meal$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?how many calories did my (?:last|latest|most recent) meal have$"),
        Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?how much protein did my (?:last|latest|most recent) meal have$"),
    )
    private val todayNutritionPatterns = mapOf(
        "calories" to listOf(
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:what is|what's) my (?:recorded )?calorie total (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:how many calories did i have|how many calories have i had) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:remind me )?(?:how many calories (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|my (?:recorded|logged|saved) calories) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:tell|show) me (?:how many calories (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|my (?:recorded|logged|saved) calories) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:how many calories (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|show my (?:recorded|logged|saved) calories) (?:for )?today$"),
        ),
        "protein" to listOf(
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:what is|what's) my (?:recorded )?protein total (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:how much protein did i have|how much protein have i had) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:remind me )?(?:how much protein (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|my (?:recorded|logged|saved) protein) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:tell|show) me (?:how much protein (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|my (?:recorded|logged|saved) protein) (?:for )?today$"),
            Regex("^(?:please )?(?:(?:can|could|would) you (?:please )?)?(?:how much protein (?:did i (?:eat|record|log|save)|have i (?:recorded|logged|saved)|i (?:recorded|logged|saved))|show my (?:recorded|logged|saved) protein) (?:for )?today$"),
        ),
    )

    fun isInScope(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any { it.code < 0x20 && !it.isWhitespace() }) {
            return false
        }
        val normalized = normalize(text)
        return readPatterns.any { it.matches(normalized) }
    }

    fun isInScope(text: String, toolName: String): Boolean = when (toolName) {
        "read_meals" -> isInScope(text)
        "read_latest_meal" -> isLatestMealInScope(text)
        "read_day_nutrition" -> isTodayNutritionInScope(text)
        else -> false
    }

    fun isLatestMealInScope(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any { it.code < 0x20 && !it.isWhitespace() }) {
            return false
        }
        return latestPatterns.any { it.matches(normalize(text)) }
    }

    fun isTodayNutritionInScope(text: String): Boolean {
        if (!validText(text)) return false
        val normalized = normalize(text)
        return todayNutritionPatterns.values.flatten().any { it.matches(normalized) }
    }

    /** Explicit read paraphrases eligible to bypass the transition write-word guard. */
    fun isAnyDirectReadInScope(text: String): Boolean =
        isInScope(text) || isLatestMealInScope(text) || isTodayNutritionInScope(text)

    fun followUpMetric(text: String): String? {
        if (!validText(text)) return null
        return when (normalize(text)) {
            "and protein", "and how much protein", "what about protein", "and protein total" -> "protein"
            "and calories", "and how many calories", "what about calories", "and calorie total" -> "calories"
            else -> null
        }
    }

    /** Resolves one short follow-up only against a successful same-day read in this process. */
    fun resolveReadRequest(
        hostRequiredTool: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
        followUpScope: FollowUpScope? = null,
    ): ResolvedRead? {
        val metric = followUpMetric(text)
        if (metric != null) {
            if (!selectedMealId.isNullOrBlank()) return null
            val scope = followUpScope ?: return null
            if (scope.referenceDate != today) return null
            val canonical = when (scope.operation) {
                FollowUpOperation.TODAY_NUTRITION -> if (metric == "protein") {
                    "How much protein have I logged today"
                } else "How many calories have I logged today"
                FollowUpOperation.LATEST_MEAL -> if (metric == "protein") {
                    "How much protein was in my latest meal"
                } else "How many calories were in my latest meal"
            }
            val resolved = requiredReadTool(null, canonical, null, today) ?: return null
            val hostCompatible = hostRequiredTool == null ||
                requiredReadTool(hostRequiredTool, canonical, null, today) != null
            return ResolvedRead(resolved, canonical, allowMiniLm = hostCompatible)
        }
        val resolved = requiredReadTool(hostRequiredTool, text, selectedMealId, today) ?: return null
        return ResolvedRead(resolved, text)
    }

    /** Keeps fallback turns tool-bound while dropping MiniLM's host-only date anchor for latest-meal. */
    fun gemmaFallbackRequiredTool(resolvedRead: ResolvedRead?, hostRequiredTool: RequiredAgentTool?): RequiredAgentTool? {
        val required = resolvedRead?.requiredTool ?: hostRequiredTool ?: return null
        return if (required.name == "read_latest_meal") required.copy(targetDate = null) else required
    }

    private fun todayNutritionMetric(text: String): String? {
        if (!validText(text)) return null
        val normalized = normalize(text)
        return todayNutritionPatterns.entries.singleOrNull { (_, patterns) -> patterns.any { it.matches(normalized) } }?.key
    }

    /** Returns the host-approved tool only for a closed, single-purpose request. */
    fun requiredReadTool(
        hostRequiredTool: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
    ): RequiredAgentTool? {
        if (!selectedMealId.isNullOrBlank()) return null
        val nutritionMetric = todayNutritionMetric(text)
        if (nutritionMetric != null) {
            val target = today
            return when {
                hostRequiredTool == null -> RequiredAgentTool(
                    "read_day_nutrition", period = "historical_day", daysAgo = 0,
                    metric = nutritionMetric, targetDate = target,
                )
                hostRequiredTool.name == "read_day_nutrition" && hostRequiredTool.period == "historical_day" &&
                    hostRequiredTool.daysAgo == 0 && hostRequiredTool.metric == nutritionMetric &&
                    hostRequiredTool.fromDaysAgo == null && hostRequiredTool.toDaysAgo == null &&
                    hostRequiredTool.query == null && hostRequiredTool.selectedMealId == null &&
                    hostRequiredTool.selectedMealQuestion == null &&
                    (hostRequiredTool.targetDate == null || hostRequiredTool.targetDate == target) ->
                    hostRequiredTool.copy(targetDate = target)
                hostRequiredTool.name == "read_nutrition_summary" && hostRequiredTool.period == "today" &&
                    hostRequiredTool.metric == nutritionMetric && hostRequiredTool.daysAgo == null &&
                    hostRequiredTool.fromDaysAgo == null && hostRequiredTool.toDaysAgo == null &&
                    hostRequiredTool.query == null && hostRequiredTool.selectedMealId == null &&
                    hostRequiredTool.selectedMealQuestion == null && hostRequiredTool.targetDate == null ->
                    RequiredAgentTool(
                        "read_day_nutrition", period = "historical_day", daysAgo = 0,
                        metric = nutritionMetric, targetDate = target,
                    )
                else -> null
            }
        }
        if (hostRequiredTool?.name == "read_day_nutrition" && hostRequiredTool.daysAgo == 0) return null
        if (isLatestMealInScope(text)) {
            return when {
                hostRequiredTool == null -> RequiredAgentTool("read_latest_meal", targetDate = today)
                hostRequiredTool.name == "read_latest_meal" && hostRequiredTool.period == null &&
                    hostRequiredTool.daysAgo == null &&
                    hostRequiredTool.fromDaysAgo == null && hostRequiredTool.toDaysAgo == null &&
                    hostRequiredTool.query == null && hostRequiredTool.metric == null && hostRequiredTool.selectedMealId == null &&
                    hostRequiredTool.selectedMealQuestion == null &&
                    (hostRequiredTool.targetDate == null || hostRequiredTool.targetDate == today) ->
                    hostRequiredTool.copy(targetDate = today)
                else -> null
            }
        }
        if (hostRequiredTool?.name == "read_latest_meal") return null
        return requiredMealListTool(hostRequiredTool, text, selectedMealId, today)
    }

    fun daysAgo(text: String): Int? {
        if (!isInScope(text)) return null
        val normalized = normalize(text)
        return when {
            normalized.contains("yesterday") -> 1
            normalized.contains("today") -> 0
            else -> null
        }
    }

    private fun requiredMealListTool(
        hostRequiredTool: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
    ): RequiredAgentTool? {
        val offset = daysAgo(text) ?: return null
        if (!selectedMealId.isNullOrBlank()) return null
        val targetDate = today.minusDays(offset.toLong())
        return when {
            hostRequiredTool?.name == "read_meals" && hostRequiredTool.daysAgo == offset &&
                hostRequiredTool.fromDaysAgo == null && hostRequiredTool.toDaysAgo == null &&
                hostRequiredTool.selectedMealId == null && hostRequiredTool.selectedMealQuestion == null &&
                hostRequiredTool.period == null && hostRequiredTool.query == null && hostRequiredTool.metric == null &&
                (hostRequiredTool.targetDate == null || hostRequiredTool.targetDate == targetDate) ->
                hostRequiredTool.copy(targetDate = targetDate)
            hostRequiredTool == null ->
                RequiredAgentTool("read_meals", daysAgo = offset, targetDate = targetDate)
            else -> null
        }
    }

    fun hasValidFrozenMealRead(call: PrivateCoachToolCall, response: Map<String, Any?>): Boolean {
        val referenceDate = call.hostReadDate ?: return false
        val offset = call.daysAgo?.takeIf { it in 0..1 } ?: return false
        if (call.intent != "read_meals" || call.period != null || call.start != null || call.end != null ||
            call.kilograms != null || call.spokenText != null || call.fromDaysAgo != null || call.toDaysAgo != null ||
            call.mealSlot != null || call.message != null || call.query != null
        ) return false
        val targetDate = referenceDate.minusDays(offset.toLong())
        if (response["as_of"] != referenceDate.toString()) return false
        return MetriaMealRecordReply.from(
            RequiredAgentTool("read_meals", daysAgo = offset, targetDate = targetDate),
            response,
        ) != null
    }

    fun latestMealReplyForFrozenRead(
        call: PrivateCoachToolCall,
        response: Map<String, Any?>,
    ): String? {
        val referenceDate = call.hostReadDate ?: return null
        if (call.intent != "read_latest_meal" || call.daysAgo != null || call.fromDaysAgo != null ||
            call.toDaysAgo != null || call.period != null || call.start != null || call.end != null ||
            call.kilograms != null || call.spokenText != null || call.mealSlot != null ||
            call.message != null || call.query != null || response["as_of"] != referenceDate.toString()
        ) return null
        return LatestMealNutritionReply.from(response)
    }

    /** Validates the frozen host date, exact tool contract, and final deterministic reply. */
    fun validatedFastReadReply(
        requiredTool: RequiredAgentTool,
        response: Map<String, Any?>,
    ): String? {
        if (requiredTool.query != null || requiredTool.fromDaysAgo != null || requiredTool.toDaysAgo != null ||
            requiredTool.selectedMealId != null || requiredTool.selectedMealQuestion != null
        ) return null
        if (requiredTool.name != "read_day_nutrition" &&
            (requiredTool.period != null || requiredTool.metric != null)
        ) return null
        return when (requiredTool.name) {
            "read_day_nutrition" -> todayNutritionReplyForFrozenRead(requiredTool, response)
            "read_meals" -> {
                val offset = requiredTool.daysAgo?.takeIf { it in 0..1 } ?: return null
                val target = requiredTool.targetDate ?: return null
                val reference = target.plusDays(offset.toLong())
                if (response["as_of"] != reference.toString() || response["from"] != target.toString() ||
                    response["to"] != target.toString()
                ) return null
                MetriaMealRecordReply.from(requiredTool, response)
            }
            "read_latest_meal" -> {
                if (requiredTool.daysAgo != null) return null
                latestMealReplyForFrozenRead(
                    PrivateCoachToolCall("read_latest_meal", hostReadDate = requiredTool.targetDate), response,
                )
            }
            else -> null
        }
    }

    /** Validates exact-day provenance and produces the existing audited recorded-estimate answer. */
    fun todayNutritionReplyForFrozenRead(
        requiredTool: RequiredAgentTool,
        response: Map<String, Any?>,
    ): String? {
        val today = requiredTool.targetDate ?: return null
        val metric = requiredTool.metric?.takeIf { it in setOf("calories", "protein") } ?: return null
        if (requiredTool.name != "read_day_nutrition" || requiredTool.period != "historical_day" || requiredTool.daysAgo != 0 ||
            requiredTool.fromDaysAgo != null || requiredTool.toDaysAgo != null ||
            requiredTool.query != null || requiredTool.selectedMealId != null || requiredTool.selectedMealQuestion != null ||
            response["source"] != "metria_local_vault" || response["as_of"] != today.toString() ||
            response["from"] != today.toString() || response["to"] != today.toString() ||
            response["period"] != "historical_day" || response["days_ago"] != 0 ||
            response["requested_metric"] != metric || response["interpretation"] != "recorded_meals_only_not_total_intake" ||
            response["reply_language"] != "English"
        ) return null
        val meals = response["meals"] as? Map<*, *> ?: return null
        val recorded = meals.integerCount("recorded_count") ?: return null
        val complete = meals.integerCount("complete_nutrition_count") ?: return null
        val partial = meals.integerCount("partial_estimate_count") ?: return null
        val missing = meals.integerCount("missing_nutrition_count") ?: return null
        if (recorded > MAX_VALIDATED_MEAL_COUNT || complete > MAX_VALIDATED_MEAL_COUNT ||
            partial > MAX_VALIDATED_MEAL_COUNT || missing > MAX_VALIDATED_MEAL_COUNT ||
            complete + partial + missing != recorded
        ) return null
        val nutrition = response["nutrition"] as? Map<*, *> ?: return null
        val key = if (metric == "calories") "calories_kcal" else "protein_g"
        val nutritionKeys = setOf(
            "calories_kcal", "protein_g", "complete_calories_kcal", "complete_protein_g",
            "partial_calories_kcal", "partial_protein_g",
        )
        if (!nutrition.keys.containsAll(nutritionKeys)) return null
        if (nutrition.values.filterNotNull().any { !it.isValidNonnegativeNumber() }) return null
        val value = nutrition[key]
        if (complete + partial == 0 && value != null) return null
        if (complete + partial > 0 && value == null) return null
        for (metricKey in listOf("calories_kcal", "protein_g")) {
            val completeValue = nutrition["complete_$metricKey"]
            val partialValue = nutrition["partial_$metricKey"]
            if ((complete > 0) != (completeValue != null) || (partial > 0) != (partialValue != null)) return null
            if ((complete + partial > 0) != (nutrition[metricKey] != null)) return null
            if (nutrition[metricKey] != null) {
                val total = (nutrition[metricKey] as Number).toDouble()
                val componentSum = java.math.BigDecimal.valueOf((completeValue as? Number)?.toDouble() ?: 0.0)
                    .add(java.math.BigDecimal.valueOf((partialValue as? Number)?.toDouble() ?: 0.0))
                val tolerance = if (metricKey == "calories_kcal") 1.0 else 0.1
                val difference = java.math.BigDecimal.valueOf(total)
                    .subtract(componentSum)
                    .abs()
                if (difference > java.math.BigDecimal.valueOf(tolerance)) return null
            }
        }
        val answer = MetriaNutritionReplyFallback.from(
            requiredTool,
            response,
        ) ?: return null
        val answerWithCoverage = "$answer Unrecorded meals are unknown."
        return answerWithCoverage.takeIf {
            MetriaAgentFactAuditor.audit(
                it,
                MetriaAgentFactEvidence.from("read_day_nutrition", response),
            ).acceptedText == it
        }
    }

    private fun validText(text: String): Boolean =
        text.isNotBlank() && text.length <= MAX_TEXT_LENGTH && text.none { it.code < 0x20 && !it.isWhitespace() }

    private fun Map<*, *>.integerCount(key: String): Int? {
        val value = this[key] as? Number ?: return null
        val number = value.toDouble()
        if (!number.isFinite() || number < 0.0 || number % 1.0 != 0.0 || number > Int.MAX_VALUE) return null
        return number.toInt()
    }

    private fun Any.isValidNonnegativeNumber(): Boolean = (this as? Number)?.toDouble()?.let {
        it.isFinite() && it >= 0.0
    } == true

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFKC)
            .replace("\u2019", "'")
            .replace("\u2018", "'")
            .trim()
            .trimEnd('.', '?', '!', ' ')
            .replace(Regex("\\s+"), " ")

    private const val MAX_TEXT_LENGTH = 500
    private const val MAX_VALIDATED_MEAL_COUNT = 1_000_000
}
