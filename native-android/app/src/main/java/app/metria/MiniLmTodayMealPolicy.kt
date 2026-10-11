package app.metria

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/** Host-side, closed English read grammar for the optional today-meals shortcut. */
internal object MiniLmTodayMealPolicy {
    private val readPatterns = listOf(
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:(?:all|my|the) )?(?:(?:saved|recorded|logged) )?(?:the )?(?:meals?|foods?) (?:(?:i (?:recorded|logged|saved)) )?(?:for )?today$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:all )?(?:saved|recorded|logged) (?:meals?|foods?) for today$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:(?:all|my|the) )?today's (?:(?:saved|recorded|logged) )?(?:meals?|foods?)$"),
        Regex("^(?:please )?(?:what|which) (?:meals?|foods?) (?:have i (?:logged|saved|recorded)|i (?:logged|saved|recorded)|are in my record) (?:for )?today$"),
        Regex("^(?:please )?(?:what|which) meals? (?:are|were) (?:recorded|logged|saved) today$"),
        Regex("^(?:please )?what did i (?:eat|have) today$"),
        Regex("^(?:please )?what did i (?:log|record|save) for meals? today$"),
        Regex("^(?:please )?(?:can you )?(?:please )?(?:show|list|display|view) (?:me )?(?:my )?meals? for today$"),
    )

    fun isInScope(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any { it.code < 0x20 && !it.isWhitespace() }) {
            return false
        }
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFKC)
            .replace("\u2019", "'")
            .replace("\u2018", "'")
            .trim()
            .trimEnd('.', '?', '!', ' ')
            .replace(Regex("\\s+"), " ")
        return readPatterns.any { it.matches(normalized) }
    }

    fun requiredReadTool(
        hostRequiredTool: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
    ): RequiredAgentTool? {
        if (!selectedMealId.isNullOrBlank() || !isInScope(text)) return null
        return when {
            hostRequiredTool?.name == "read_meals" && hostRequiredTool.daysAgo == 0 &&
                hostRequiredTool.fromDaysAgo == null && hostRequiredTool.toDaysAgo == null &&
                hostRequiredTool.selectedMealId == null && hostRequiredTool.period == null &&
                hostRequiredTool.query == null && hostRequiredTool.metric == null &&
                (hostRequiredTool.targetDate == null || hostRequiredTool.targetDate == today) ->
                hostRequiredTool.copy(targetDate = today)
            hostRequiredTool == null && isInScope(text) ->
                RequiredAgentTool("read_meals", daysAgo = 0, targetDate = today)
            else -> null
        }
    }

    fun hasValidFrozenMealRead(call: PrivateCoachToolCall, response: Map<String, Any?>): Boolean {
        val date = call.hostReadDate ?: return false
        if (call.intent != "read_meals" || call.daysAgo != 0 || call.mealSlot != null) return false
        return MetriaMealRecordReply.from(
            RequiredAgentTool("read_meals", daysAgo = 0, targetDate = date),
            response,
        ) != null
    }

    private const val MAX_TEXT_LENGTH = 500
}
