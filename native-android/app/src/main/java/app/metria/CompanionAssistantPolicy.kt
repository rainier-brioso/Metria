package app.metria

import java.time.LocalDate

/** Closed, host-owned intents for the local companion shortcuts. */
object CompanionAssistantPolicy {
    enum class IntentKind { TODAY_OVERVIEW, GOAL_REMAINING, SAVED_RECALL, SAVED_DRAFT, NAVIGATION }
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

    data class Resolution(
        val kind: IntentKind,
        val requiredTool: RequiredAgentTool?,
        val classifierKey: String,
        val classifierText: String,
        val metric: String? = null,
        val query: String? = null,
        val destination: Destination? = null,
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
    private val recallUsual = Regex("^(?:find|show|look up) my usual (breakfast|lunch|dinner|snack)$")
    private val recallNamed = Regex("^(?:find|show|look up) my saved (?:dish )?(.{2,100})$|^(?:find|show|look up) my (.{2,100}) dish$")
    private val draftUsual = Regex("^(?:i had|i ate) my usual (breakfast|lunch|dinner|snack)$")
    private val draftNamed = Regex("^(?:i had|i ate) my saved (?:dish )?(.{2,100})$")
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
            if (!safeSavedDishQuery(draftQuery)) return null
            return Resolution(IntentKind.SAVED_DRAFT, expected, "saved_draft", normalized, query = draftQuery)
        }

        val recallMatch = recallNamed.matchEntire(normalized)
        val recallQuery = recallUsual.matchEntire(normalized)?.groupValues?.get(1)
            ?: recallMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.trim()
        if (recallQuery != null) {
            val expected = RequiredAgentTool("search_saved_dishes", query = recallQuery)
            if (!compatible(hostRequiredTool, expected)) return null
            if (!safeSavedDishQuery(recallQuery)) return null
            return Resolution(IntentKind.SAVED_RECALL, expected, "saved_recall", normalized, query = recallQuery)
        }

        navigation.singleOrNull { (pattern, _) -> pattern.matches(normalized) }?.let { (_, destination) ->
            if (hostRequiredTool != null) return null
            return Resolution(IntentKind.NAVIGATION, null, "navigation", normalized, destination = destination)
        }
        return null
    }

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
    fun savedDishReply(response: Map<String, Any?>, expectedQuery: String, draft: Boolean): String? {
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
            draft -> "Possible saved dish matches: ${rows.joinToString(", ") { it.first }}. Choose one to review; I cannot tell which is usual. This is an unsaved draft."
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

    private fun safeSavedDishQuery(query: String): Boolean {
        val normalized = normalize(query)
        if (normalized.length !in 2..100) return false
        if (Regex("\\b(?:yesterday|today|last week|two days ago|on monday|tomorrow)\\b").containsMatchIn(normalized)) return false
        return !Regex("\\b(?:and (?:then )?(?:save|log|add|calculate|show|tell|remind)|then (?:save|log|add)|also (?:save|log|add))\\b").containsMatchIn(normalized)
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
