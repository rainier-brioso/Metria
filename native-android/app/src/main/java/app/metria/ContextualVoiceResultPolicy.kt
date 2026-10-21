package app.metria

/** Keeps old assistant effects out of an active transcript preview without poisoning later turns. */
internal object ContextualVoiceResultPolicy {
    /** Every input read by the guard must also participate in the Compose effect identity. */
    data class EffectKey(
        val state: String,
        val activeCaptureToken: Long?,
        val resultCaptureToken: Long?,
    ) {
        val isolatesAssistantResults: Boolean get() = state != "idle" && activeCaptureToken != null &&
            resultCaptureToken == activeCaptureToken
    }

    fun isolatesAssistantResult(state: String, activeCaptureToken: Long?, resultCaptureToken: Long?): Boolean =
        EffectKey(state, activeCaptureToken, resultCaptureToken).isolatesAssistantResults
}
