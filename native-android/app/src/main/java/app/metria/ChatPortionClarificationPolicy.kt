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

sealed interface ChatPortionReply {
    data class Grams(val value: Int) : ChatPortionReply
    data object Skip : ChatPortionReply
    data object InvalidQuantity : ChatPortionReply
    data object Unrelated : ChatPortionReply
}

object ChatPortionClarificationPolicy {
    private val explicitGrams = Regex("^\\s*([0-9]{1,4})\\s*(?:g|gram|grams)\\s*$", RegexOption.IGNORE_CASE)
    private val amountOnly = Regex("^\\s*(?:(?:about|around|approximately|roughly|exactly|maybe|i\\s+think|i\\s+guess|probably)\\s+)?[-+]?[0-9]+(?:[.,][0-9]+)?(?:\\s*(?:-|to)\\s*[-+]?[0-9]+(?:[.,][0-9]+)?)?\\s*(?:g|grams?|kg|kilograms?|lb|lbs|ounces?|oz)?(?:\\s+please)?\\s*$", RegexOption.IGNORE_CASE)
    private val amountWordOnly = Regex("^\\s*(?:(?:about|around|approximately|roughly|exactly|maybe)\\s+)?(?:half|quarter|a\\s+few|two\\s+hundred|one\\s+hundred)(?:\\s+(?:g|grams?))?\\s*$", RegexOption.IGNORE_CASE)
    private val unrelatedQuestionLead = Regex("^\\s*(?:what|how|when|where|why|show|list|tell|can|could)\\b", RegexOption.IGNORE_CASE)
    private val joinedFoodName = Regex("(?:&|\\b(?:and|y)\\b)", RegexOption.IGNORE_CASE)
    private val skipPhrases = setOf(
        "skip", "skip it", "i don't know", "i dont know", "don't know", "dont know",
        "not sure", "leave it", "keep the estimate",
    )

    /** Only an ordinary, complete Chat draft may ask; image/manual/saved-dish paths are excluded by operation. */
    fun target(
        proposal: MealProposal,
        operation: String?,
        ordinaryChatDraft: Boolean,
    ): ChatPortionTarget? {
        if (!ordinaryChatDraft || operation !in setOf("meal", "agent_meal_draft") ||
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
