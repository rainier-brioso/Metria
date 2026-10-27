package app.metria

import java.time.Duration
import java.time.Instant
import org.json.JSONObject

data class PrivateConversationMessage(
    val text: String,
    val fromMember: Boolean,
    val localEvidence: Boolean = false,
    val savedMealId: String? = null,
)

/** Optional local receipt link; old transcripts decode without a link. */
object PrivateConversationMessageCodec {
    fun encode(message: PrivateConversationMessage): JSONObject = JSONObject()
        .put("text", message.text)
        .put("fromMember", message.fromMember)
        .put("localEvidence", message.localEvidence)
        .apply { message.savedMealId?.let { put("savedMealId", it) } }

    fun decode(value: JSONObject): PrivateConversationMessage = PrivateConversationMessage(
        text = value.getString("text"),
        fromMember = value.getBoolean("fromMember"),
        localEvidence = value.optBoolean("localEvidence", false),
        savedMealId = value.optString("savedMealId", "").takeIf(String::isNotEmpty),
    )
}

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
        if (messages.none(PrivateConversationMessage::fromMember) && messages.none { it.hasSavedMealLink() }) return null
        return record.copy(messages = messages)
    }

    fun updated(
        messages: List<PrivateConversationMessage>,
        now: Instant = Instant.now(),
    ): PrivateConversationRecord? {
        val normalized = normalizedMessages(messages)
        if (normalized.none(PrivateConversationMessage::fromMember) && normalized.none { it.hasSavedMealLink() }) return null
        return PrivateConversationRecord(normalized, now)
    }

    private fun PrivateConversationMessage.hasSavedMealLink(): Boolean =
        !fromMember && !localEvidence && savedMealId != null

    private fun normalizedMessages(messages: List<PrivateConversationMessage>): List<PrivateConversationMessage> =
        messages.asSequence()
            .map { message -> message.copy(
                text = message.text.trim().take(maxMessageCharacters),
                savedMealId = message.savedMealId?.takeIf {
                    !message.fromMember && !message.localEvidence &&
                        it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}"))
                },
            ) }
            .filter { it.text.isNotEmpty() }
            .toList()
            .takeLast(maxMessages)
}
