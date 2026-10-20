package app.metria

/** A transient microphone capture pinned to one completed ordinary Chat draft. */
internal data class ChatVoiceDraftBinding(
    val captureToken: Long,
    val sourceRequestId: Long,
    val conversationGeneration: Long,
    val proposal: MealProposal,
    val foodContext: ChatCurrentDraftFoodContext?,
    val portionContext: ChatCurrentDraftPortionContext?,
    val foodOverride: ChatFoodReplacementOverride?,
    val portionOverride: ChatPortionOverride?,
)

/**
 * One-use authorization for handing recognized, member-reviewed words to the existing
 * Chat submit path. It carries no authority after a stale check or the first consume.
 */
internal class OneUseChatVoiceDraftHandoff private constructor(
    private val binding: ChatVoiceDraftBinding,
) {
    private var consumed = false

    fun captureToken(): Long = binding.captureToken

    fun isCurrent(
        captureToken: Long,
        currentFoodContext: ChatCurrentDraftFoodContext?,
        currentPortionContext: ChatCurrentDraftPortionContext?,
        currentFoodOverride: ChatFoodReplacementOverride?,
        currentPortionOverride: ChatPortionOverride?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean {
        if (consumed) return false
        val sourceRequestId = binding.foodContext?.sourceRequestId ?: binding.portionContext?.sourceRequestId
            ?: return false
        val sourceGeneration = binding.foodContext?.conversationGeneration
            ?: binding.portionContext?.conversationGeneration ?: return false
        val sourceProposal = binding.foodContext?.originalProposal ?: binding.portionContext?.originalProposal
            ?: return false
        return unlocked && !stopping && !pending &&
            captureToken == binding.captureToken &&
            activeRequestId == binding.sourceRequestId && activeRequestId == sourceRequestId &&
            handledReplyRequestId == binding.sourceRequestId &&
            conversationGeneration == binding.conversationGeneration &&
            currentFoodContext == binding.foodContext && currentPortionContext == binding.portionContext &&
            currentFoodOverride == binding.foodOverride && currentPortionOverride == binding.portionOverride &&
            sourceRequestId == binding.sourceRequestId &&
            sourceGeneration == binding.conversationGeneration &&
            sourceProposal == binding.proposal &&
            (binding.foodContext == null || binding.foodContext.originalProposal == binding.proposal) &&
            (binding.portionContext == null || binding.portionContext.originalProposal == binding.proposal)
    }

    fun consume(
        captureToken: Long,
        text: String?,
        currentFoodContext: ChatCurrentDraftFoodContext?,
        currentPortionContext: ChatCurrentDraftPortionContext?,
        currentFoodOverride: ChatFoodReplacementOverride?,
        currentPortionOverride: ChatPortionOverride?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean {
        if (consumed) return false
        if (VoiceEntryText.validated(text) == null) {
            consumed = true
            return false
        }
        val current = isCurrent(
            captureToken, currentFoodContext, currentPortionContext,
            currentFoodOverride, currentPortionOverride, activeRequestId,
            handledReplyRequestId, conversationGeneration, unlocked, stopping, pending,
        )
        consumed = true
        return current
    }

    fun revoke() {
        consumed = true
    }

    companion object {
        fun capture(
            captureToken: Long,
            foodContext: ChatCurrentDraftFoodContext?,
            portionContext: ChatCurrentDraftPortionContext?,
            foodOverride: ChatFoodReplacementOverride?,
            portionOverride: ChatPortionOverride?,
        ): OneUseChatVoiceDraftHandoff? {
            val sourceRequestId = foodContext?.sourceRequestId ?: portionContext?.sourceRequestId ?: return null
            val sourceGeneration = foodContext?.conversationGeneration ?: portionContext?.conversationGeneration ?: return null
            val sourceProposal = foodContext?.originalProposal ?: portionContext?.originalProposal ?: return null
            if ((foodContext != null && (
                    foodContext.sourceRequestId != sourceRequestId ||
                        foodContext.conversationGeneration != sourceGeneration ||
                        foodContext.originalProposal != sourceProposal
                    )) ||
                (portionContext != null && (
                    portionContext.sourceRequestId != sourceRequestId ||
                        portionContext.conversationGeneration != sourceGeneration ||
                        portionContext.originalProposal != sourceProposal
                    ))
            ) return null
            return OneUseChatVoiceDraftHandoff(
                ChatVoiceDraftBinding(
                    captureToken, sourceRequestId, sourceGeneration,
                    sourceProposal, foodContext, portionContext, foodOverride, portionOverride,
                ),
            )
        }
    }
}

/** One-use authority for a transcript preview in ordinary Chat, with or without a meal draft. */
internal class OneUseChatVoiceRequestProof private constructor(
    private val captureToken: Long,
    private val modelRequestId: Long?,
    private val activeRequestId: Long?,
    private val handledReplyRequestId: Long?,
    private val conversationGeneration: Long,
) {
    private var consumed = false

    fun captureToken(): Long = captureToken

    fun isCurrent(
        token: Long,
        currentModelRequestId: Long?,
        currentActiveRequestId: Long?,
        currentHandledReplyRequestId: Long?,
        currentConversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean = !consumed && unlocked && !stopping && !pending &&
        token == captureToken &&
        currentModelRequestId == modelRequestId &&
        currentActiveRequestId == activeRequestId &&
        currentHandledReplyRequestId == handledReplyRequestId &&
        currentConversationGeneration == conversationGeneration

    fun consume(
        token: Long,
        text: String?,
        currentModelRequestId: Long?,
        currentActiveRequestId: Long?,
        currentHandledReplyRequestId: Long?,
        currentConversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean {
        if (consumed) return false
        val textIsValid = VoiceEntryText.validated(text) != null
        val current = isCurrent(
            token, currentModelRequestId, currentActiveRequestId, currentHandledReplyRequestId,
            currentConversationGeneration, unlocked, stopping, pending,
        )
        consumed = true
        return textIsValid && current
    }

    fun revoke() {
        consumed = true
    }

    /** A cancellation receipt lets the member leave the sheet and type deliberately.
     * It carries identity only; it cannot authorize a voice action or submission.
     */
    fun typingExitReceipt(): OneUseChatVoiceTypingExit = OneUseChatVoiceTypingExit.capture(
        captureToken, modelRequestId, activeRequestId, handledReplyRequestId, conversationGeneration,
    )

    companion object {
        fun capture(
            captureToken: Long,
            modelRequestId: Long?,
            activeRequestId: Long?,
            handledReplyRequestId: Long?,
            conversationGeneration: Long,
        ) = OneUseChatVoiceRequestProof(
            captureToken, modelRequestId, activeRequestId, handledReplyRequestId, conversationGeneration,
        )
    }
}

internal class OneUseChatVoiceTypingExit private constructor(
    private val captureToken: Long,
    private val modelRequestId: Long?,
    private val activeRequestId: Long?,
    private val handledReplyRequestId: Long?,
    private val conversationGeneration: Long,
) {
    private var consumed = false

    fun isCurrent(
        currentCaptureToken: Long?,
        currentModelRequestId: Long?,
        currentActiveRequestId: Long?,
        currentHandledReplyRequestId: Long?,
        currentConversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean = !consumed && unlocked && !stopping && !pending &&
        captureToken == currentCaptureToken && modelRequestId == currentModelRequestId &&
        activeRequestId == currentActiveRequestId && handledReplyRequestId == currentHandledReplyRequestId &&
        conversationGeneration == currentConversationGeneration

    fun consume(
        currentCaptureToken: Long?,
        currentModelRequestId: Long?,
        currentActiveRequestId: Long?,
        currentHandledReplyRequestId: Long?,
        currentConversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        pending: Boolean,
    ): Boolean {
        val current = isCurrent(
            currentCaptureToken, currentModelRequestId, currentActiveRequestId,
            currentHandledReplyRequestId, currentConversationGeneration, unlocked, stopping, pending,
        )
        consumed = true
        return current
    }

    fun revoke() { consumed = true }

    companion object {
        fun capture(
            captureToken: Long,
            modelRequestId: Long?, activeRequestId: Long?, handledReplyRequestId: Long?,
            conversationGeneration: Long,
        ) = OneUseChatVoiceTypingExit(
            captureToken, modelRequestId, activeRequestId, handledReplyRequestId, conversationGeneration,
        )
    }
}
