package app.metria

internal sealed interface VoiceTranscriptParseResult {
    data class Accepted(val text: String) : VoiceTranscriptParseResult
    data object NoText : VoiceTranscriptParseResult
    data object StructuredOutput : VoiceTranscriptParseResult
    data object InvalidTranscript : VoiceTranscriptParseResult
}

/** Validates a plain transcript without extracting text from model formatting or actions. */
internal object VoiceTranscriptParser {
    fun parse(output: String): String? =
        (classify(output) as? VoiceTranscriptParseResult.Accepted)?.text

    fun classify(output: String): VoiceTranscriptParseResult {
        if (output.length > 8_192) return VoiceTranscriptParseResult.InvalidTranscript
        if (output.any { it.isISOControl() && it != '\r' && it != '\n' }) {
            return VoiceTranscriptParseResult.InvalidTranscript
        }
        val text = output.trim()
        if (text.isEmpty()) return VoiceTranscriptParseResult.NoText
        if (looksStructured(text)) return VoiceTranscriptParseResult.StructuredOutput
        if (text.length > VoiceEntryText.MAX_LENGTH || text.any { it == '\n' || it == '\r' }) {
            return VoiceTranscriptParseResult.InvalidTranscript
        }
        val validated = VoiceEntryText.validated(text) ?: return VoiceTranscriptParseResult.InvalidTranscript
        return VoiceTranscriptParseResult.Accepted(validated)
    }

    private fun looksStructured(text: String): Boolean {
        val lower = text.lowercase()
        return text.startsWith('{') || text.startsWith('[') || text.startsWith("```") || text.startsWith('<') ||
            lower.startsWith("action:") || lower.startsWith("tool_call:") || lower.startsWith("function_call:")
    }
}
