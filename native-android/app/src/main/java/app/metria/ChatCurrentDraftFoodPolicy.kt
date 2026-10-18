package app.metria

import java.text.Normalizer
import java.util.Locale

/** Frozen identity of the single food eligible for a live Chat draft rename. */
data class ChatCurrentDraftFoodContext(
    val sourceRequestId: Long,
    val conversationGeneration: Long,
    val originalProposal: MealProposal,
    val itemIndex: Int,
    val originalItemName: String,
    val currentReplacementName: String?,
    val currentGrams: Int?,
)

/** Transient Review label overlay; it never mutates the model proposal. */
data class ChatFoodReplacementOverride(
    val requestId: Long,
    val conversationGeneration: Long,
    val originalProposal: MealProposal,
    val itemIndex: Int,
    val originalItemName: String,
    val replacementName: String,
)

object ChatCurrentDraftFoodPolicy {
    private val joined = Regex("\\b(?:and|or|plus|with)\\b|[&,;+]", RegexOption.IGNORE_CASE)
    private val numberOrMeasure = Regex(
        """(?:\d|(?:^|\s)(?:one|two|three|four|five|six|seven|eight|nine|ten|half|quarter)(?:\s|$)|\b(?:g|gram|grams|kg|kilograms?|lb|lbs|oz|ounces?)\b)""",
        RegexOption.IGNORE_CASE,
    )
    private val mixedEdit = Regex(
        """\b(?:add|remove|take\s+off|leave\s+out|include|plus|and\s+also|instead\s+of)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val correctionCue = Regex(
        """\b(?:actually|instead|replace|change|changed|meant|make\s+(?:it|that)|it\s+was|that\s+was|no,?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val explicitCorrectionFrame = Regex(
        """^\s*(?:(?:actually|instead)\s*,?\s*)?(?:(?:no)\s*,?\s*)?(?:(?:it|that|this)\s+(?:was|is|should\s+be)|i\s+(?:meant|mean)|(?:replace|change)(?:\s+it)?\s+(?:with|to)|use|make\s+(?:it|that))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val replacementVerb = Regex(
        """\b(?:actually|instead|change|changed|replace|make|was|were|meant|it|the|food|meal|my|i|had|have|ate|eaten|to|a|an|some|that|this|please|could|you|can|we|use)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Only a complete single-item, simple, ordinary text draft is eligible. */
    fun eligibleItem(
        proposal: MealProposal?,
        operation: String?,
        ordinaryChatDraft: Boolean,
    ): Pair<Int, MealProposalItem>? {
        val current = proposal ?: return null
        if (!ordinaryChatDraft || operation !in setOf("meal", "agent_meal_draft", "agent_meal_portion_edit", "agent_meal_food_edit") ||
            current.items.size != 1 || current.omittedItemCount != 0 || current.partialCategory != null
        ) return null
        val item = current.items.single()
        val name = item.name.trim()
        if (name.length !in 1..80 || item.groupLabel?.isNotBlank() == true ||
            joined.containsMatchIn(name) || numberOrMeasure.containsMatchIn(name)
        ) return null
        return 0 to item
    }

    fun context(
        proposal: MealProposal?,
        operation: String?,
        requestId: Long?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        acceptedOverride: ChatFoodReplacementOverride?,
        currentGrams: Int?,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): ChatCurrentDraftFoodContext? {
        val request = requestId ?: return null
        if (!unlocked || stopping || pending || request != activeRequestId || request != handledReplyRequestId) return null
        val (index, item) = eligibleItem(proposal, operation, ordinaryChatDraft = true) ?: return null
        if (currentGrams != null && currentGrams !in 1..2_000) return null
        val current = proposal ?: return null
        if (acceptedOverride != null && !isCurrent(
                acceptedOverride, current, request, conversationGeneration, unlocked, stopping,
            )
        ) return null
        return ChatCurrentDraftFoodContext(
            request, conversationGeneration, current, index, item.name.trim(),
            acceptedOverride?.replacementName, currentGrams,
        )
    }

    fun contextIsCurrent(
        context: ChatCurrentDraftFoodContext,
        proposal: MealProposal?,
        previousRequestId: Long?,
        conversationGeneration: Long,
        operation: String?,
        unlocked: Boolean,
        stopping: Boolean,
    ): Boolean {
        val eligible = eligibleItem(proposal, operation, ordinaryChatDraft = true) ?: return false
        return unlocked && !stopping && (context.currentGrams == null || context.currentGrams in 1..2_000) &&
            previousRequestId == context.sourceRequestId && conversationGeneration == context.conversationGeneration &&
            proposal == context.originalProposal && eligible.first == context.itemIndex &&
            eligible.second.name.trim().equals(context.originalItemName, ignoreCase = true)
    }

    /** Replacement must be one short phrase occurring literally, once, in this member turn. */
    fun matchingReplacement(text: String, modelName: String): String? {
        val name = validFoodLabel(modelName) ?: return null
        val statedPhrase = strictCorrectionPhrase(text) ?: return null
        if (normalizeTokens(name) != normalizeTokens(statedPhrase)) return null
        val normalizedText = normalizeTokens(text)
        val normalizedName = normalizeTokens(name)
        if (normalizedName.isEmpty()) return null
        val boundary = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(normalizedName)}(?![\\p{L}\\p{N}])")
        if (boundary.findAll(normalizedText).count() != 1) return null
        return name
    }

    /** Strict shared gate for one native correction attempt and its final tool result. */
    fun isEligibleCorrectionTurn(text: String): Boolean {
        return strictCorrectionPhrase(text) != null
    }

    /** Phrase extracted only from the explicit, single-food correction frame. */
    fun strictCorrectionPhrase(text: String): String? {
        if (text.contains('?') || Regex("""\b(?:not|never|isn't|isnt|wasn't|wasnt|was\s+it)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            !correctionCue.containsMatchIn(text) || !explicitCorrectionFrame.containsMatchIn(text) ||
            mixedEdit.containsMatchIn(text) || numberOrMeasure.containsMatchIn(text) ||
            Regex("\\b(?:add|remove|without|plus|and|or)\\b|[&;+/]", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            hasUnsupportedComma(text)
        ) return null
        return correctionPhrase(text)
    }

    /**
     * Offline vocabulary fast path. It returns only a transient name overlay after
     * current-draft/lifecycle validation and a complete phrase match against catalog
     * descriptions; catalog identities and nutrition never leave this policy.
     */
    fun localCatalogReplacement(
        context: ChatCurrentDraftFoodContext?,
        text: String,
        catalog: OfflineFoodCatalog?,
        proposal: MealProposal?,
        requestId: Long?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        operation: String?,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): ChatFoodReplacementOverride? {
        val current = context ?: return null
        val sourceRequest = current.sourceRequestId
        if (!unlocked || stopping || pending || requestId != sourceRequest || activeRequestId != sourceRequest ||
            handledReplyRequestId != sourceRequest || catalog == null ||
            !contextIsCurrent(current, proposal, requestId, conversationGeneration, operation, unlocked, stopping)
        ) return null
        val phrase = strictCorrectionPhrase(text) ?: return null
        if (!catalogSupportsPhrase(catalog, phrase)) return null
        return overrideForCurrentDraft(current, sourceRequest, phrase)
    }

    private fun catalogSupportsPhrase(catalog: OfflineFoodCatalog, phrase: String): Boolean {
        val phraseTokens = normalizedFoodTokens(phrase)
        if (phraseTokens.isEmpty()) return false
        val phraseSet = phraseTokens.toSet()
        return catalog.foods.any { food ->
            val descriptionTokens = normalizedFoodTokens(food.description)
            val descriptionSet = descriptionTokens.toSet()
            if (!phraseTokens.all { it in descriptionSet }) return@any false
            val headTokens = normalizedFoodTokens(food.description.substringBefore(','))
            if (headTokens.isEmpty() || headTokens.all(genericHeadTerms::contains)) return@any false
            phraseTokens.containsSequence(headTokens) && headTokens.all(phraseSet::contains)
        }
    }

    private fun normalizedFoodTokens(value: String): List<String> =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter(String::isNotBlank)
            .map(::singularize)

    private fun singularize(token: String): String = when {
        token.length > 4 && token.endsWith("ies") -> token.dropLast(3) + "y"
        token.length > 4 && token.endsWith("oes") -> token.dropLast(2)
        token.length > 4 && (token.endsWith("ches") || token.endsWith("shes")) -> token.dropLast(2)
        token.length > 3 && token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
        else -> token
    }

    private fun List<String>.containsSequence(sequence: List<String>): Boolean =
        sequence.isNotEmpty() && windowed(sequence.size).any { it == sequence }

    private val genericHeadTerms = setOf("food", "foods", "restaurant", "restaurants", "product", "products", "meal", "meals", "dish", "dishes")

    fun shouldRetryNoToolCorrection(
        currentDraftFoodAvailable: Boolean,
        text: String,
        noToolCalls: Boolean,
        requiredRead: Boolean,
        navigation: Boolean,
        retryAlreadyAttempted: Boolean,
    ): Boolean = currentDraftFoodAvailable && noToolCalls && !requiredRead && !navigation &&
        !retryAlreadyAttempted && isEligibleCorrectionTurn(text)

    fun correctionRetryInstruction(): String = """
        Your previous response did not apply a tool. The current member message appears to correct the identity of the one food in the unsaved draft. If it clearly gives one short replacement food phrase, call replace_current_draft_food with exactly that phrase copied from the current member message. If the replacement is unclear, ask a brief clarifying question. Do not claim that the draft changed unless the host confirms it.
    """.trimIndent()

    fun correctionRetryNotSelectedReply(): String =
        "I couldn't apply that food change, so your current draft is unchanged and still ready to review."

    private fun correctionPhrase(text: String): String? {
        var phrase = text.trim().replace(Regex("[.!]+$"), "").trim()
        phrase = Regex("""^\s*(?:actually|instead|no)\s*,?\s*""", RegexOption.IGNORE_CASE)
            .replaceFirst(phrase, "")
        phrase = Regex(
            """^\s*(?:(?:it|that|this)\s+(?:was|is|should\s+be)|i\s+(?:meant|mean)|(?:replace|change)(?:\s+it)?\s+(?:with|to)|use|make\s+(?:it|that))\s+""",
            RegexOption.IGNORE_CASE,
        ).replaceFirst(phrase, "")
        phrase = Regex(""",\s*(?:please|thanks|thank\s+you)$""", RegexOption.IGNORE_CASE)
            .replaceFirst(phrase, "")
        return validFoodLabel(phrase)
    }

    private fun hasUnsupportedComma(text: String): Boolean {
        var remaining = text
        val leadingCueComma = Regex("""^\s*(?:actually|instead|no)\s*,\s*""", RegexOption.IGNORE_CASE)
        remaining = leadingCueComma.replaceFirst(remaining, "")
        val trailingCourtesyComma = Regex(""",\s*(?:please|thanks|thank\s+you)[.!]?\s*$""", RegexOption.IGNORE_CASE)
        remaining = trailingCourtesyComma.replaceFirst(remaining, "")
        return ',' in remaining
    }

    /** Keeps correction-like or mixed-edit requests attached to the current draft. */
    fun looksLikeFoodReplacementAttempt(text: String): Boolean =
        isEligibleCorrectionTurn(text) || mixedEdit.containsMatchIn(text) ||
            Regex("""\b(?:add|include|remove|without)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    private fun validFoodLabel(value: String): String? {
        val name = value.trim().replace(Regex("\\s+"), " ")
        if (name.length !in 2..60 || name.split(' ').size !in 1..5 ||
            !Regex("^[\\p{L}][\\p{L}' -]*$").matches(name) ||
            numberOrMeasure.containsMatchIn(name) || joined.containsMatchIn(name) ||
            replacementVerb.containsMatchIn(name)
        ) return null
        return name
    }

    fun validReviewLabel(value: String): Boolean = validFoodLabel(value) == value.trim().replace(Regex("\\s+"), " ")

    private fun normalizeTokens(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    fun overrideForCurrentDraft(
        context: ChatCurrentDraftFoodContext,
        resultRequestId: Long,
        replacementName: String,
    ): ChatFoodReplacementOverride? {
        val (index, item) = eligibleItem(context.originalProposal, "agent_meal_draft", true) ?: return null
        if (index != context.itemIndex || !item.name.trim().equals(context.originalItemName, true)) return null
        val validated = validFoodLabel(replacementName) ?: return null
        return ChatFoodReplacementOverride(
            resultRequestId, context.conversationGeneration, context.originalProposal,
            index, item.name.trim(), validated,
        )
    }

    fun rebind(
        context: ChatCurrentDraftFoodContext,
        resultRequestId: Long,
        replacementName: String?,
    ): ChatFoodReplacementOverride? = replacementName?.let {
        overrideForCurrentDraft(context, resultRequestId, it)
    }

    fun isCurrent(
        override: ChatFoodReplacementOverride,
        proposal: MealProposal,
        requestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
    ): Boolean {
        val eligible = eligibleItem(proposal, "agent_meal_draft", true) ?: return false
        return unlocked && !stopping && requestId == override.requestId &&
            conversationGeneration == override.conversationGeneration && proposal == override.originalProposal &&
            eligible.first == override.itemIndex &&
            eligible.second.name.trim().equals(override.originalItemName, true) &&
            validFoodLabel(override.replacementName) == override.replacementName
    }
}
