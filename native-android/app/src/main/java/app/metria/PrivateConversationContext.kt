package app.metria

/**
 * Approved conversation decision: keep a small, ephemeral window of the visible chat so
 * follow-ups can resolve references without giving the model durable memory or vault access.
 * Local evidence is clearly labelled and remains the only conversational text that may be
 * treated as a fact; assistant prose is continuity only.
 */
enum class PrivateConversationRole {
    Member,
    Assistant,
    LocalEvidence,
}

data class PrivateConversationTurn(
    val role: PrivateConversationRole,
    val text: String,
)

object PrivateConversationContext {
    private const val maxTurns = 6
    private const val maxTurnCharacters = 320
    private const val maxContextCharacters = 1_900

    fun build(turns: List<PrivateConversationTurn>): String {
        val recent = turns
            .mapNotNull(::sanitized)
            .takeLast(maxTurns)
        if (recent.isEmpty()) return ""

        val pendingFollowUp = recent
            .lastOrNull { it.role != PrivateConversationRole.LocalEvidence }
            ?.takeIf { it.role == PrivateConversationRole.Assistant && it.text.trimEnd().endsWith("?") }
            ?.text

        val body = buildList {
            recent.forEach { turn -> add("${turn.role.promptLabel()}: ${turn.text}") }
            pendingFollowUp?.let { add("PENDING_FOLLOW_UP: $it") }
        }.joinToString("\n")

        return body.takeLast(maxContextCharacters)
    }

    private fun sanitized(turn: PrivateConversationTurn): PrivateConversationTurn? {
        val text = turn.text
            .replace(Regex("[\\p{Cc}&&[^\\n\\t]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxTurnCharacters)
        return text.takeIf(String::isNotBlank)?.let { turn.copy(text = it) }
    }

    private fun PrivateConversationRole.promptLabel(): String = when (this) {
        PrivateConversationRole.Member -> "MEMBER"
        PrivateConversationRole.Assistant -> "METRIA"
        PrivateConversationRole.LocalEvidence -> "LOCAL_TOOL_EVIDENCE"
    }
}
