package app.metria

/** Keeps old assistant effects out of an active transcript preview without poisoning later turns. */
internal object ContextualVoiceResultPolicy {
    fun isolatesAssistantResult(state: String, activeCaptureToken: Long?, resultCaptureToken: Long?): Boolean =
        state != "idle" && activeCaptureToken != null &&
            resultCaptureToken == activeCaptureToken
}
