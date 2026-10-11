package app.metria

/** Prevents stale/local-assistant results from escaping into a later meal-entry attempt. */
internal object MealEntryResultPolicy {
    data class DraftDebugDetails(val requestId: Long, val category: String, val response: String)

    fun startedAfter(previous: ModelStatus, current: ModelStatus, operation: String): Long? =
        current.assistantRequestId
            ?.takeIf { it != previous.assistantRequestId && current.assistantOperation == operation && current.state == "thinking" }

    fun isCompletedResult(requestId: Long?, current: ModelStatus, operation: String): Boolean =
        requestId != null && current.assistantRequestId == requestId &&
            current.assistantOperation == operation && current.state != "thinking"

    fun transcript(current: ModelStatus): String? = VoiceEntryText.validated(current.coachToolCall?.spokenText)

    /** Automatic generation may consume only this attempt's successful audio result. */
    fun transcriptForDraft(requestId: Long?, current: ModelStatus): String? =
        if (isCompletedResult(requestId, current, "coach_audio") && current.state == "loaded") transcript(current) else null

    fun proposal(current: ModelStatus): MealProposal? = current.assistantProposal

    fun draftDebugDetails(requestId: Long?, current: ModelStatus): DraftDebugDetails? {
        val id = requestId ?: return null
        if (current.assistantRequestId != id || current.assistantOperation != "meal" || current.state != "loaded") return null
        val category = current.debugLatestMealDraftCategory ?: return null
        return DraftDebugDetails(id, category, current.debugLatestModelResponse.orEmpty())
    }
}
