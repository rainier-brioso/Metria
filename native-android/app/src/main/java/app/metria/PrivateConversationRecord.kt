package app.metria

import java.time.Duration
import java.time.Instant

data class PrivateConversationMessage(
    val text: String,
    val fromMember: Boolean,
    val localEvidence: Boolean = false,
)

data class PrivateConversationRecord(
    val messages: List<PrivateConversationMessage>,
    val updatedAt: Instant,
)

/** Retention and size rules for the encrypted, device-local active conversation. */
object PrivateConversationPolicy {
    const val maxMessages = 50
    const val maxMessageCharacters = 2_000
    val retention: Duration = Duration.ofHours(24)

    fun active(record: PrivateConversationRecord?, now: Instant = Instant.now()): PrivateConversationRecord? {
        record ?: return null
        if (!now.isBefore(record.updatedAt.plus(retention))) return null
        val messages = normalizedMessages(record.messages)
        if (messages.none(PrivateConversationMessage::fromMember)) return null
        return record.copy(messages = messages)
    }

    fun updated(
        messages: List<PrivateConversationMessage>,
        now: Instant = Instant.now(),
    ): PrivateConversationRecord? {
        val normalized = normalizedMessages(messages)
        if (normalized.none(PrivateConversationMessage::fromMember)) return null
        return PrivateConversationRecord(normalized, now)
    }

    private fun normalizedMessages(messages: List<PrivateConversationMessage>): List<PrivateConversationMessage> =
        messages.asSequence()
            .map { message -> message.copy(text = message.text.trim().take(maxMessageCharacters)) }
            .filter { it.text.isNotEmpty() }
            .toList()
            .takeLast(maxMessages)
}
