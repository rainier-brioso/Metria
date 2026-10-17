package app.metria

import java.util.Locale

/** A one-turn clarification target. The model proposal remains the source estimate. */
data class ChatPortionTarget(
    val itemIndex: Int,
    val itemName: String,
    val minimumGrams: Int,
    val maximumGrams: Int,
)

/** Host-owned prefill, bound to the exact completed Chat draft that asked the question. */
data class ChatPortionOverride(
    val requestId: Long,
    val conversationGeneration: Long,
    val originalProposal: MealProposal,
    val itemIndex: Int,
    val itemName: String,
    val grams: Int?,
)

data class ChatPortionClarificationOffer(
    val requestId: Long,
    val conversationGeneration: Long,
    val proposal: MealProposal,
    val target: ChatPortionTarget,
    val answered: Boolean = false,
    val grams: Int? = null,
    val feedback: String? = null,
)

/** Frozen host-owned target for one model-interpreted correction to a live draft. */
data class ChatCurrentDraftPortionContext(
    val sourceRequestId: Long,
    val conversationGeneration: Long,
    val originalProposal: MealProposal,
    val target: ChatPortionTarget,
    val currentGrams: Int?,
)

enum class CurrentDraftTurnDisposition {
    APPLY_PORTION_EDIT,
    RETAIN_AND_CLARIFY_CONFIRMATION,
    RETAIN_CONVERSATIONAL_REPLY,
    RETIRE_FOR_EXPLICIT_OPERATION,
    NO_CURRENT_DRAFT,
}

sealed interface ChatPortionReply {
    data class Grams(val value: Int) : ChatPortionReply
    data object Skip : ChatPortionReply
    data object InvalidQuantity : ChatPortionReply
    data object Unrelated : ChatPortionReply
}

object ChatPortionClarificationPolicy {
    private val explicitGrams = Regex("^\\s*([0-9]{1,4})\\s*(?:g|gram|grams)\\s*$", RegexOption.IGNORE_CASE)
    private val oneEmbeddedGramAmount = Regex("(?<![\\w.+-])([0-9]{1,4})\\s*(?:g|grams?)\\b", RegexOption.IGNORE_CASE)
    private val amountOnly = Regex("^\\s*(?:(?:about|around|approximately|roughly|exactly|maybe|i\\s+think|i\\s+guess|probably)\\s+)?[-+]?[0-9]+(?:[.,][0-9]+)?(?:\\s*(?:-|to)\\s*[-+]?[0-9]+(?:[.,][0-9]+)?)?\\s*(?:g|grams?|kg|kilograms?|lb|lbs|ounces?|oz)?(?:\\s+please)?\\s*$", RegexOption.IGNORE_CASE)
    private val amountWordOnly = Regex("^\\s*(?:(?:about|around|approximately|roughly|exactly|maybe)\\s+)?(?:half|quarter|a\\s+few|two\\s+hundred|one\\s+hundred)(?:\\s+(?:g|grams?))?\\s*$", RegexOption.IGNORE_CASE)
    private val unrelatedQuestionLead = Regex("^\\s*(?:what|how|when|where|why|show|list|tell|can|could)\\b", RegexOption.IGNORE_CASE)
    private val joinedFoodName = Regex("(?:&|\\b(?:and|y)\\b)", RegexOption.IGNORE_CASE)
    private val skipPhrases = setOf(
        "skip", "skip it", "i don't know", "i dont know", "don't know", "dont know",
        "not sure", "leave it", "keep the estimate",
    )

    /** Classifies the final model response for an already-frozen current-draft turn. */
    fun currentDraftTurnDisposition(
        contextAvailable: Boolean,
        finalToolNames: Set<String>,
        selectedIntent: String?,
        requiredRead: Boolean,
        navigation: Boolean,
        newMealIntent: Boolean,
    ): CurrentDraftTurnDisposition {
        if (!contextAvailable) return CurrentDraftTurnDisposition.NO_CURRENT_DRAFT
        if (requiredRead || navigation || finalToolNames.any { it.startsWith("read_") } ||
            selectedIntent?.startsWith("read_") == true
        ) return CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION
        if (finalToolNames.contains("adjust_current_draft_portion")) {
            return CurrentDraftTurnDisposition.APPLY_PORTION_EDIT
        }
        if (selectedIntent == "draft_meal" || finalToolNames.contains("draft_meal")) {
            return if (newMealIntent) CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION
            else CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION
        }
        if (selectedIntent == "confirm_meal_draft" || selectedIntent == "decline_meal_draft" ||
            finalToolNames.any { it == "confirm_meal_draft" || it == "decline_meal_draft" }
        ) {
            return CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION
        }
        if (finalToolNames.isEmpty()) return CurrentDraftTurnDisposition.RETAIN_CONVERSATIONAL_REPLY
        return CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION
    }

    /** Only an ordinary, complete Chat draft may ask; image/manual/saved-dish paths are excluded by operation. */
    fun target(
        proposal: MealProposal,
        operation: String?,
        ordinaryChatDraft: Boolean,
    ): ChatPortionTarget? {
        if (!ordinaryChatDraft || operation !in setOf("meal", "agent_meal_draft", "agent_meal_portion_edit") ||
            proposal.omittedItemCount != 0 || proposal.partialCategory != null || proposal.items.isEmpty()
        ) return null

        val uncertain = proposal.items.withIndex().filter { (_, item) ->
            item.estimatedGramsRange.min < item.estimatedGramsRange.max
        }
        if (uncertain.size != 1) return null
        val (index, item) = uncertain.single()
        val name = item.name.trim()
        if (name.isEmpty() || item.groupLabel?.isNotBlank() == true || joinedFoodName.containsMatchIn(name)) return null
        if (proposal.items.count { it.name.trim().equals(name, ignoreCase = true) } != 1) return null
        return ChatPortionTarget(index, name, item.estimatedGramsRange.min, item.estimatedGramsRange.max)
    }

    fun parseReply(text: String): ChatPortionReply {
        val normalized = text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        if (normalized in skipPhrases) return ChatPortionReply.Skip
        val grams = explicitGrams.matchEntire(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (grams != null && grams in 1..2_000) return ChatPortionReply.Grams(grams)
        if (unrelatedQuestionLead.containsMatchIn(text)) return ChatPortionReply.Unrelated
        val amountLike = amountOnly.matches(text) || amountWordOnly.matches(text)
        return if (amountLike) ChatPortionReply.InvalidQuantity else ChatPortionReply.Unrelated
    }

    /** Accept exactly one explicit whole-gram literal from free text, and require it to match Gemma's integer. */
    fun matchingTypedCorrection(text: String, modelGrams: Int): Int? {
        if (modelGrams !in 1..2_000) return null
        val matches = oneEmbeddedGramAmount.findAll(text).toList()
        if (matches.size != 1) return null
        val match = matches.single()
        val grams = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..2_000 } ?: return null
        if (grams != modelGrams) return null
        val remainder = text.removeRange(match.range)
        // Other numbers/units make the correction ambiguous (ranges, decimals, other measures, or multiple amounts).
        if (Regex("[0-9]|\\b(?:kg|kilograms?|lb|lbs|ounces?|oz)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(remainder)
        ) return null
        return grams
    }

    fun currentDraftContext(
        proposal: MealProposal?,
        operation: String?,
        requestId: Long?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        acceptedOverride: ChatPortionOverride?,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): ChatCurrentDraftPortionContext? {
        val current = proposal ?: return null
        val request = requestId ?: return null
        if (!unlocked || stopping || pending || request != activeRequestId ||
            request != handledReplyRequestId || operation !in setOf("meal", "agent_meal_draft", "agent_meal_portion_edit")
        ) return null
        val target = target(current, operation, ordinaryChatDraft = true) ?: return null
        if (acceptedOverride != null && !isCurrent(
                acceptedOverride, current, request, conversationGeneration, unlocked, stopping,
            )
        ) return null
        return ChatCurrentDraftPortionContext(
            sourceRequestId = request,
            conversationGeneration = conversationGeneration,
            originalProposal = current,
            target = target,
            currentGrams = acceptedOverride?.grams,
        )
    }

    fun contextIsCurrent(
        context: ChatCurrentDraftPortionContext,
        proposal: MealProposal?,
        previousRequestId: Long?,
        conversationGeneration: Long,
        operation: String?,
        unlocked: Boolean,
        stopping: Boolean,
    ): Boolean = unlocked && !stopping && (context.currentGrams == null || context.currentGrams in 1..2_000) &&
        previousRequestId == context.sourceRequestId &&
        conversationGeneration == context.conversationGeneration && operation in setOf("meal", "agent_meal_draft", "agent_meal_portion_edit") &&
        proposal != null && proposal == context.originalProposal &&
        target(proposal, operation, ordinaryChatDraft = true) == context.target

    fun overrideForCurrentDraft(
        context: ChatCurrentDraftPortionContext,
        resultRequestId: Long,
        grams: Int?,
    ): ChatPortionOverride? {
        if (grams != null && grams !in 1..2_000) return null
        val item = context.originalProposal.items.getOrNull(context.target.itemIndex) ?: return null
        if (!item.name.trim().equals(context.target.itemName, ignoreCase = true) ||
            item.estimatedGramsRange.min >= item.estimatedGramsRange.max
        ) return null
        return ChatPortionOverride(
            resultRequestId, context.conversationGeneration, context.originalProposal,
            context.target.itemIndex, context.target.itemName, grams,
        )
    }

    fun overrideForReview(
        originalProposal: MealProposal,
        requestId: Long,
        conversationGeneration: Long,
        target: ChatPortionTarget,
        reply: ChatPortionReply,
    ): ChatPortionOverride? {
        val grams = when (reply) {
            is ChatPortionReply.Grams -> reply.value.takeIf { it in 1..2_000 } ?: return null
            ChatPortionReply.Skip -> null
            else -> return null
        }
        if (originalProposal.items.getOrNull(target.itemIndex)?.let { item ->
                item.name.trim().equals(target.itemName, ignoreCase = true) &&
                    item.estimatedGramsRange.min < item.estimatedGramsRange.max
            } != true
        ) return null
        return ChatPortionOverride(requestId, conversationGeneration, originalProposal, target.itemIndex, target.itemName, grams)
    }

    fun isCurrent(
        override: ChatPortionOverride,
        proposal: MealProposal,
        requestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
    ): Boolean = unlocked && !stopping && requestId == override.requestId &&
        conversationGeneration == override.conversationGeneration && proposal == override.originalProposal &&
        proposal.items.getOrNull(override.itemIndex)?.name?.trim()?.equals(override.itemName, ignoreCase = true) == true

    fun isOfferCurrent(
        offer: ChatPortionClarificationOffer,
        proposal: MealProposal?,
        requestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
    ): Boolean = unlocked && !stopping && requestId == offer.requestId &&
        conversationGeneration == offer.conversationGeneration && proposal == offer.proposal
}
