package app.metria

import java.time.LocalDate

/** Closed, host-owned intents for the local companion shortcuts. */
object CompanionAssistantPolicy {
    enum class IntentKind { TODAY_OVERVIEW, GOAL_REMAINING, SAVED_RECALL, SAVED_DRAFT, SAVED_CONSUMPTION_PROBE, NAVIGATION }
    enum class Destination { TODAY, HISTORY, PRIVACY }

    data class SavedDishItemPrefill(
        val name: String,
        val description: String?,
        val grams: Int?,
        val preparation: String?,
        val catalogFoodId: Long?,
        val catalogRelease: String?,
        val role: String,
    )

    data class SavedDishMealPrefill(
        val sourceDishId: String,
        val title: String,
        val items: List<SavedDishItemPrefill>,
    )

    data class SavedDishDraftChoice(val exactCandidateId: String?, val candidates: List<PersonalDishMatch>)
    enum class SavedDishProbeDisposition { EXACT_DRAFT, CHOOSE, NORMAL_INTENT }

    data class Resolution(
        val kind: IntentKind,
        val requiredTool: RequiredAgentTool?,
        val classifierKey: String,
        val classifierText: String,
        val metric: String? = null,
        val query: String? = null,
        val destination: Destination? = null,
    )

    /** Local correction scope for saved-dish commands; query spans are deliberately excluded. */
    internal data class SavedDishCorrectionScope(
        val isSavedDishFrame: Boolean,
        val editableTargetsByWordIndex: Map<Int, Set<String>> = emptyMap(),
        val opaqueQuery: String? = null,
        val intent: IntentKind? = null,
    )

    private val goalRemaining = Regex(
        "^(?:how much protein|how many calories) do i have left today$",
    )
    private val goalRemainingAlternate = Regex(
        "^(?:what is|what's) (?:my )?(?:remaining|leftover) (?:protein|calories) for today$",
    )
    private val todayOverview = listOf(
        Regex("^how am i doing today$"),
        Regex("^how am i doing for today$"),
        Regex("^(?:give|show) me (?:(?:a )?summary|(?:an? )?overview) of today$"),
        Regex("^(?:give|show) me (?:(?:a )?summary|(?:an? )?overview) of my day today$"),
    )
    private val recallUsual = Regex("^(?:find|show|look up) (?:me )?my usual (breakfast|lunch|dinner|snack)$")
    private val recallNamed = Regex("^(?:find|show|look up) (?:me )?my saved (?:dish )?(.{2,100})$|^(?:find|show|look up) (?:me )?my (.{2,100}) dish$")
    private val draftUsual = Regex("^(?:i had|i ate|i've had|i've eaten|i have had|i have eaten|i just had|i just ate|i've just had|i've just eaten|i have just had|i have just eaten) my usual (breakfast|lunch|dinner|snack)$")
    private val draftNamed = Regex("^(?:i had|i ate|i've had|i've eaten|i have had|i have eaten|i just had|i just ate|i've just had|i've just eaten|i have just had|i have just eaten) my saved (?:dish )?(.{2,100})$")
    private val bareConsumption = Regex("^(?:i had|i ate|i've had|i've eaten|i have had|i have eaten|i just had|i just ate|i've just had|i've just eaten|i have just had|i have just eaten) (.{2,100})$")
    private val navigation = listOf(
        Regex("^(?:open|go to) (?:my )?weight history$|^open weight history$") to Destination.HISTORY,
        Regex("^open (?:my )?meals for today$|^open today$|^go to today$|^open today's summary$") to Destination.TODAY,
        Regex("^open (?:my )?privacy settings$|^open privacy$|^go to privacy settings$") to Destination.PRIVACY,
    )

    fun resolve(
        hostRequiredTool: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
        allowBareConsumptionProbe: Boolean = true,
    ): Resolution? {
        if (!selectedMealId.isNullOrBlank() || !validText(text)) return null
        val normalized = normalize(text)
        if (todayOverview.any { it.matches(normalized) }) {
            val hostExpected = RequiredAgentTool("read_progress_overview", period = "today")
            if (!compatible(hostRequiredTool, hostExpected)) return null
            return Resolution(
                IntentKind.TODAY_OVERVIEW,
                hostExpected.copy(targetDate = today),
                "today_overview",
                normalized,
            )
        }
        val goal = goalRemaining.matchEntire(normalized)?.value
            ?: goalRemainingAlternate.matchEntire(normalized)?.value
        if (goal != null) {
            val metric = if (normalized.contains("protein")) "protein" else "calories"
            val hostExpected = RequiredAgentTool("read_nutrition_summary", period = "today", metric = metric)
            if (!compatible(hostRequiredTool, hostExpected)) return null
            val expected = hostExpected.copy(targetDate = today)
            return Resolution(
                IntentKind.GOAL_REMAINING,
                expected,
                "goal_remaining",
                normalized,
                metric = metric,
            )
        }

        val draftQuery = draftUsual.matchEntire(normalized)?.groupValues?.get(1)
            ?: draftNamed.matchEntire(normalized)?.groupValues?.get(1)?.trim()
        if (draftQuery != null) {
            val expected = RequiredAgentTool("search_saved_dishes", query = draftQuery)
            if (!compatible(hostRequiredTool, expected)) return null
            if (!safeSavedDishQuery(draftQuery, today)) return null
            val namedMatch = draftNamed.matchEntire(normalized)
            val classifierText = namedMatch?.let { maskNamedQuery(normalized, it, 1) } ?: normalized
            return Resolution(IntentKind.SAVED_DRAFT, expected, "saved_draft", classifierText, query = draftQuery)
        }

        val bareConsumptionMatch = bareConsumption.matchEntire(normalized)
        val bareQuery = bareConsumptionMatch?.groupValues?.get(1)?.trim()
        if (allowBareConsumptionProbe && bareQuery != null && safeBareConsumptionQuery(bareQuery, today)) {
            val expected = RequiredAgentTool("search_saved_dishes", query = bareQuery)
            if (!compatible(hostRequiredTool, expected) || !safeSavedDishQuery(bareQuery, today)) return null
            return Resolution(
                IntentKind.SAVED_CONSUMPTION_PROBE,
                expected,
                "saved_draft",
                maskNamedQuery(normalized, bareConsumptionMatch, 1),
                query = bareQuery,
            )
        }

        val recallMatch = recallNamed.matchEntire(normalized)
        val recallQuery = recallUsual.matchEntire(normalized)?.groupValues?.get(1)
            ?: recallMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.trim()
        if (recallQuery != null) {
            val expected = RequiredAgentTool("search_saved_dishes", query = recallQuery)
            if (!compatible(hostRequiredTool, expected)) return null
            if (!safeSavedDishQuery(recallQuery, today)) return null
            val classifierText = recallMatch?.let { match ->
                val queryGroup = (1 until match.groups.size).firstOrNull { match.groups[it] != null }
                queryGroup?.let { maskNamedQuery(normalized, match, it) }
            } ?: normalized
            return Resolution(IntentKind.SAVED_RECALL, expected, "saved_recall", classifierText, query = recallQuery)
        }

        navigation.singleOrNull { (pattern, _) -> pattern.matches(normalized) }?.let { (_, destination) ->
            if (hostRequiredTool != null) return null
            return Resolution(IntentKind.NAVIGATION, null, "navigation", normalized, destination = destination)
        }
        return null
    }

    /**
     * Identifies saved-dish-shaped turns before generic typo recovery. Only closed command/meal
     * slots can be edited; the member's named-dish span is never a correction target.
     */
    internal fun savedDishCorrectionScope(normalizedText: String): SavedDishCorrectionScope? {
        val words = Regex("[a-zA-Z]+").findAll(normalizedText).toList()
        fun indexesIn(range: IntRange): List<Int> = words.indices.filter { index ->
            val word = words[index]
            word.range.first >= range.first && word.range.last <= range.last
        }
        fun targets(range: IntRange, vocabulary: Set<String>): Map<Int, Set<String>> =
            indexesIn(range).associateWith { index ->
                val word = words[index].value.lowercase(java.util.Locale.ROOT)
                MiniLmReadTypoCandidates.localCorrectionsFor(word, vocabulary, minimumLength = 3)
            }.filterValues(Set<String>::isNotEmpty)

        val namedRecall = Regex("^(.{1,30}) my saved (?:dish )?(.{2,100})$").matchEntire(normalizedText)
            ?: Regex("^(.{1,30}) my (.{2,100}) dish$").matchEntire(normalizedText)
        if (namedRecall != null && isRecallCommandFrame(namedRecall.groups[1]!!.value)) {
            return SavedDishCorrectionScope(
                true,
                targets(namedRecall.groups[1]!!.range, RECALL_COMMAND_WORDS),
                opaqueQuery = normalize(namedRecall.groups[2]!!.value),
                intent = IntentKind.SAVED_RECALL,
            )
        }

        val usualRecall = Regex("^(.{1,30}) my (usual|[a-z]+) (breakfast|lunch|dinner|snack|[a-z]+)$")
            .matchEntire(normalizedText)
        if (usualRecall != null) {
            val prefix = usualRecall.groups[1]!!.range
            if (isRecallCommandFrame(normalizedText.substring(prefix))) {
                val usualWord = usualRecall.groups[2]!!.value
                val mealWord = usualRecall.groups[3]!!.value
                val usualTargets = if (usualWord == "usual" ||
                    MiniLmReadTypoCandidates.localCorrectionsFor(usualWord, setOf("usual"), minimumLength = 3).size == 1
                ) targets(usualRecall.groups[2]!!.range, setOf("usual")) else emptyMap()
                val mealTargets = if (mealWord in MEAL_TYPES ||
                    MiniLmReadTypoCandidates.localCorrectionsFor(mealWord, MEAL_TYPES, minimumLength = 3).size == 1
                ) targets(usualRecall.groups[3]!!.range, MEAL_TYPES) else emptyMap()
                val editable = targets(prefix, RECALL_COMMAND_WORDS) + usualTargets + mealTargets
                return SavedDishCorrectionScope(true, editable, intent = IntentKind.SAVED_RECALL)
            }
        }

        val draftNamedFrame = Regex("^(i(?:'ve| have)? (?:just )?)([a-z]+) my saved (?:dish )?(.{2,100})$")
            .matchEntire(normalizedText)
        if (draftNamedFrame != null) {
            val permission = draftNamedFrame.groups[2]!!.value in CONSUMPTION_VERBS
            return SavedDishCorrectionScope(
                true,
                opaqueQuery = normalize(draftNamedFrame.groups[3]!!.value),
                intent = IntentKind.SAVED_DRAFT.takeIf { permission },
            )
        }

        val bareConsumptionFrame = bareConsumption.matchEntire(normalizedText)
        val bareConsumptionQuery = bareConsumptionFrame?.groupValues?.get(1)?.trim()
        if (bareConsumptionFrame != null && bareConsumptionQuery != null &&
            !bareConsumptionQuery.startsWith("my ")
        ) {
            return SavedDishCorrectionScope(
                true,
                opaqueQuery = bareConsumptionQuery,
                intent = IntentKind.SAVED_CONSUMPTION_PROBE.takeIf {
                    safeBareConsumptionQuery(bareConsumptionQuery, LocalDate.of(2000, 1, 1))
                },
            )
        }

        val draftUsualFrame = Regex("^(i(?:'ve| have)? (?:just )?)([a-z]+) my ([a-z]+) ([a-z]+)$")
            .matchEntire(normalizedText)
        if (draftUsualFrame != null) {
            val verb = draftUsualFrame.groups[2]!!.value
            val draftPermission = verb in CONSUMPTION_VERBS
            val usualSlot = draftUsualFrame.groups[3]!!.range
            val mealSlot = draftUsualFrame.groups[4]!!.range
            val usualWord = draftUsualFrame.groups[3]!!.value
            val mealWord = draftUsualFrame.groups[4]!!.value
            val usualTargets = if (usualWord == "usual" ||
                MiniLmReadTypoCandidates.localCorrectionsFor(usualWord, setOf("usual"), minimumLength = 3).size == 1
            ) targets(usualSlot, setOf("usual")) else emptyMap()
            val mealTargets = if (mealWord in MEAL_TYPES ||
                MiniLmReadTypoCandidates.localCorrectionsFor(mealWord, MEAL_TYPES, minimumLength = 3).size == 1
            ) targets(mealSlot, MEAL_TYPES) else emptyMap()
            return SavedDishCorrectionScope(
                true,
                if (draftPermission) usualTargets + mealTargets else emptyMap(),
                intent = IntentKind.SAVED_DRAFT.takeIf { draftPermission },
            )
        }

        // A date, qualifier, negation or mixed command makes an otherwise recognizable saved
        // frame ineligible. Keep it out of the generic system-checker path and normal read parser.
        val recallFrame = Regex("^(.{1,60})\\bmy (?:saved|usual)\\b.*$").matchEntire(normalizedText)
        if (recallFrame != null) {
            return SavedDishCorrectionScope(true)
        }
        val draftFrame = Regex("^i(?:'ve| have)? .*\\bmy (?:saved|usual)\\b.*$").matchEntire(normalizedText)
        if (draftFrame != null) {
            val permission = Regex("^i(?:'ve| have)? (?:just )?(had|ate|eaten)\\b").find(normalizedText)
                ?.groupValues?.get(1) in CONSUMPTION_VERBS
            return SavedDishCorrectionScope(true, intent = IntentKind.SAVED_DRAFT.takeIf { permission })
        }
        return null
    }

    private fun isRecallCommandFrame(prefix: String): Boolean {
        val words = Regex("[a-zA-Z]+").findAll(prefix).toList()
        if (words.size !in 1..2) return false
        val normalized = words.map { it.value.lowercase(java.util.Locale.ROOT) }
        return when (normalized.size) {
            1 -> normalized.single() in RECALL_SINGLE_WORDS ||
                MiniLmReadTypoCandidates.localCorrectionsFor(normalized.single(), RECALL_SINGLE_WORDS, minimumLength = 3).isNotEmpty()
            else -> (normalized[0] in setOf("find", "show", "look") ||
                MiniLmReadTypoCandidates.localCorrectionsFor(normalized[0], RECALL_COMMAND_WORDS, minimumLength = 3).isNotEmpty()) &&
                (normalized[1] in setOf("me", "up") ||
                    MiniLmReadTypoCandidates.localCorrectionsFor(normalized[1], RECALL_COMMAND_WORDS, minimumLength = 3).isNotEmpty())
        }
    }

    private val RECALL_SINGLE_WORDS = setOf("find", "show")
    private val RECALL_COMMAND_WORDS = setOf("find", "show", "look", "up")
    private val MEAL_TYPES = setOf("breakfast", "lunch", "dinner", "snack")
    private val CONSUMPTION_VERBS = setOf("had", "ate", "eaten")

    fun savedDishDraftChoice(query: String, matches: List<PersonalDishMatch>): SavedDishDraftChoice {
        val exact = matches.filter { match ->
            val sought = PersonalDishMatcher.identityKey(query)
            PersonalDishMatcher.identityKey(match.dish.name) == sought ||
                match.dish.aliases.any { PersonalDishMatcher.identityKey(it) == sought }
        }
        val unique = when {
            exact.size == 1 -> exact.single()
            else -> null
        }
        return SavedDishDraftChoice(unique?.dish?.id, matches.take(3))
    }

    /**
     * A bare consumption statement may use a saved dish only when the full saved identity matches.
     * A partial or empty result returns control to normal native intent selection.
     */
    fun savedDishProbeDisposition(query: String, matches: List<PersonalDishMatch>): SavedDishProbeDisposition {
        val choice = savedDishDraftChoice(query, matches)
        return when {
            choice.exactCandidateId != null -> SavedDishProbeDisposition.EXACT_DRAFT
            Regex("\\b(?:and|with|plus)\\b|[,;+]").containsMatchIn(query) -> SavedDishProbeDisposition.NORMAL_INTENT
            matches.size > 1 -> SavedDishProbeDisposition.CHOOSE
            else -> SavedDishProbeDisposition.NORMAL_INTENT
        }
    }

    fun savedDishPrefill(dish: PersonalDish): SavedDishMealPrefill? {
        if (!PersonalDishValidation.valid(dish)) return null
        val items = PersonalDishStructure.componentItems(dish).take(12)
        if (items.isEmpty()) return null
        return SavedDishMealPrefill(
            sourceDishId = dish.id,
            title = dish.name,
            items = items.map { item -> SavedDishItemPrefill(
                name = item.name,
                description = item.description,
                grams = item.grams,
                preparation = item.preparation,
                catalogFoodId = item.catalogFoodId,
                catalogRelease = item.catalogRelease,
                role = item.role,
            ) },
        )
    }

    /** Strict host response reader for the existing bounded search_saved_dishes tool. */
    fun savedDishReply(
        response: Map<String, Any?>,
        expectedQuery: String,
        draft: Boolean,
        consumptionProbe: Boolean = false,
    ): String? {
        if (response.keys != SAVED_DISH_RESPONSE_KEYS || response["source"] != "metria_local_vault" ||
            response["reply_language"] != "English" || response["query"] != expectedQuery) return null
        val total = exactCount(response["total_count"]) ?: return null
        val returned = exactCount(response["returned_count"]) ?: return null
        val omitted = exactCount(response["omitted_count"]) ?: return null
        if (returned > 3 || total != returned + omitted) return null
        val expectedCoverage = if (omitted == 0) "full" else "latest_records_only"
        if (response["coverage"] != expectedCoverage) return null
        val matches = response["matches"] as? List<*> ?: return null
        if (matches.size != returned) return null
        val rows = matches.map { item ->
            val row = item as? Map<*, *> ?: return null
            if (row.keys != SAVED_DISH_MATCH_KEYS) return null
            val name = (row["name"] as? String)?.takeIf { it.isNotBlank() && it.length <= 120 } ?: return null
            val ingredients = row["ingredients"] as? List<*> ?: return null
            if (ingredients.size > 12 || ingredients.any { it !is String || it.isBlank() || it.length > 120 }) return null
            val confirmations = exactCount(row["confirmation_count"]) ?: return null
            if (confirmations !in 1..10_000) return null
            name to ingredients.filterIsInstance<String>()
        }
        val answer = when {
            rows.isEmpty() -> "No saved dish matched “$expectedQuery”."
            draft && rows.size == 1 -> "I found the saved dish “${rows.single().first}”. Review its foods and portions before adding it as a meal. This is an unsaved draft."
            draft -> "Possible saved dish matches: ${rows.joinToString(", ") { it.first }}. Choose one to review.${if (consumptionProbe) " This is an unsaved draft." else " I cannot tell which is usual. This is an unsaved draft."}"
            else -> "Saved dish matches for “$expectedQuery”: " + rows.joinToString(". ") { (name, foods) ->
                if (foods.isEmpty()) name else "$name contains ${foods.joinToString(", ")}"
            } + "."
        }
        return answer.takeIf {
            MetriaAgentFactAuditor.audit(it, MetriaAgentFactEvidence.from("search_saved_dishes", response)).acceptedText == it
        }
    }

    private fun compatible(host: RequiredAgentTool?, expected: RequiredAgentTool): Boolean {
        if (host == null) return true
        return host.name == expected.name && host.period == expected.period && host.query == expected.query &&
            host.metric == expected.metric && host.daysAgo == null && host.fromDaysAgo == null &&
            host.toDaysAgo == null && (host.targetDate == null || host.targetDate == expected.targetDate) &&
            host.selectedMealId == null &&
            host.selectedMealQuestion == null
    }

    private fun validText(text: String): Boolean = text.isNotBlank() && text.length <= 500 &&
        text.none { it.code < 0x20 && !it.isWhitespace() }

    private fun safeSavedDishQuery(query: String, today: LocalDate): Boolean {
        val normalized = normalize(query)
        if (normalized.length !in 2..100) return false
        if (MetriaCalendarDayPolicy.scan(query, today, rejectNumericDates = true).hasExpression) return false
        if (Regex(
                "\\b(?:today|yesterday|tomorrow|last week|this week|next week|two days ago|" +
                    "(?:on )?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday))\\b",
            ).containsMatchIn(normalized)
        ) return false
        return !Regex("\\b(?:and (?:then )?(?:save|log|add|calculate|show|tell|remind)|then (?:save|log|add)|also (?:save|log|add))\\b").containsMatchIn(normalized)
    }

    private fun safeBareConsumptionQuery(query: String, today: LocalDate): Boolean {
        val normalized = normalize(query)
        if (normalized.length !in 2..100) return false
        if (Regex("^my (?:saved|usual)\\b").containsMatchIn(normalized)) return false
        val usualFrame = Regex("^my ([a-z]+) (?:breakfast|lunch|dinner|snack)$").matchEntire(normalized)
        if (usualFrame != null && MiniLmReadTypoCandidates.localCorrectionsFor(
                usualFrame.groupValues[1], setOf("usual"), minimumLength = 3,
            ).isNotEmpty()
        ) return false
        if (Regex("^(?:no|not|never|maybe|if|would|could|should|might|used to|have to|had to|want to|need to)\\b").containsMatchIn(normalized)) return false
        if (Regex("^(?:a |an |the )?(?:headache|migraine|fever|cold|flu|pain|stomach ache|sore throat|appointment|meeting|long day|hard time|question|problem|choice|chance|to work|to leave)\\b").containsMatchIn(normalized)) return false
        return !Regex("\\b(?:and (?:then )?(?:save|log|add|calculate|show|tell|remind)|then (?:save|log|add)|also (?:save|log|add))\\b").containsMatchIn(normalized) &&
            !MetriaCalendarDayPolicy.scan(query, today, rejectNumericDates = true).hasExpression
    }

    private fun maskNamedQuery(text: String, match: MatchResult, groupIndex: Int): String {
        val queryRange = match.groups[groupIndex]?.range ?: return text
        return text.replaceRange(queryRange, "name")
    }

    private fun normalize(text: String): String = MiniLmReadTextNormalization.normalize(text)

    private fun exactCount(value: Any?): Int? {
        val number = (value as? Number)?.toDouble() ?: return null
        return number.toInt().takeIf { number.isFinite() && number >= 0.0 && number <= 10_000.0 && number == it.toDouble() }
    }

    private val SAVED_DISH_RESPONSE_KEYS = setOf(
        "source", "reply_language", "query", "total_count", "returned_count", "omitted_count", "coverage", "matches", "summary",
    )
    private val SAVED_DISH_MATCH_KEYS = setOf("name", "ingredients", "confirmation_count")
}

/** One-turn guard for a saved-dish probe that found no usable identity match. */
internal object SavedConsumptionFallbackPolicy {
    enum class ToolDecision { NORMAL, RETRY_WITHOUT_SAVED_SEARCH, MEAL_DRAFT_SELECTED }

    fun toolDecision(call: PrivateCoachToolCall?): ToolDecision = when (call?.intent) {
        "search_saved_dishes" -> ToolDecision.RETRY_WITHOUT_SAVED_SEARCH
        "draft_meal" -> ToolDecision.MEAL_DRAFT_SELECTED
        else -> ToolDecision.NORMAL
    }

    fun retryInstruction(): String =
        "Do not search saved dishes again. Respond or clarify normally, unless the member clearly reports eating food and you choose draft_meal. A meal proposal is unsaved and requires member review."
}
