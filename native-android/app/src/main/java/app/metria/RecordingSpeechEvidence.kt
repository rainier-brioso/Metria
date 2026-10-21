package app.metria

/** Monotonic speech evidence scoped to one recording and shared by manual/automatic stop. */
internal class RecordingSpeechEvidence(private val voiceThreshold: Int) {
    @Volatile
    var detectedSpeech: Boolean = false
        private set

    /** Returns whether this sample crosses the existing local voice threshold. */
    fun observePeak(peak: Int): Boolean {
        if (peak < voiceThreshold) return false
        detectedSpeech = true
        return true
    }
}
