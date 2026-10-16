package app.metria

internal enum class AssistantConversationHydrationDecision {
    KEEP_LOCAL,
    RESTORE_PERSISTED,
    EXPIRED,
}

/** Keeps transient request identity when a vault snapshot is only its canonical retained form. */
internal object AssistantConversationHydrationPolicy {
    fun decide(
        hydrated: Boolean,
        previouslyLoaded: PrivateConversationRecord?,
        localMessages: List<PrivateConversationMessage>,
        persisted: PrivateConversationRecord?,
        expired: Boolean,
    ): AssistantConversationHydrationDecision {
        if (expired) return AssistantConversationHydrationDecision.EXPIRED
        if (!hydrated) return AssistantConversationHydrationDecision.RESTORE_PERSISTED

        // Ignore status refreshes that return the last loaded vault snapshot. The local transcript
        // may already contain a new member turn that has not reached the debounced writer yet.
        if (persisted == previouslyLoaded) return AssistantConversationHydrationDecision.KEEP_LOCAL

        val canonicalLocal = persisted?.let {
            PrivateConversationPolicy.updated(localMessages, it.updatedAt)?.messages
        }
        return if (persisted != null && canonicalLocal == persisted.messages) {
            AssistantConversationHydrationDecision.KEEP_LOCAL
        } else {
            AssistantConversationHydrationDecision.RESTORE_PERSISTED
        }
    }
}
