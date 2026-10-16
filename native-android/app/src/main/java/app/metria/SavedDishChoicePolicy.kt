package app.metria

import java.text.Normalizer
import java.util.Locale

/** A short-lived saved-dish choice bound to one completed Chat reply. */
internal data class SavedDishChoiceCandidate(
    val dish: PersonalDish,
    val exactNameMatch: Boolean,
)

internal data class PendingSavedDishChoices(
    val requestId: Long,
    val conversationGeneration: Long,
    val candidates: List<SavedDishChoiceCandidate>,
    /** A failed transcript write can retry this same choice, but cannot select another silently. */
    val selectedCandidateId: String? = null,
)

internal object SavedDishChoicePolicy {
    fun isCurrent(
        pending: PendingSavedDishChoices?,
        activeRequestId: Long?,
        handledReplyRequestId: Long?,
        conversationGeneration: Long,
        unlocked: Boolean,
        stopping: Boolean,
        currentDishes: List<PersonalDish>,
    ): Boolean {
        if (pending == null || !unlocked || stopping || pending.candidates.isEmpty() || pending.candidates.size > 3 ||
            pending.requestId != activeRequestId || pending.requestId != handledReplyRequestId ||
            pending.conversationGeneration != conversationGeneration
        ) return false
        val candidateIds = pending.candidates.map { it.dish.id }
        if (candidateIds.distinct().size != candidateIds.size) return false
        val currentById = currentDishes.associateBy(PersonalDish::id)
        return pending.candidates.map(SavedDishChoiceCandidate::dish).all { captured ->
            currentDishes.count { it.id == captured.id } == 1 && currentById[captured.id] == captured
        }
    }

    /**
     * Accepts only a closed ordinal phrase or a unique, complete candidate name. No fuzzy match,
     * yes/no interpretation, model output, or dish ID from Chat participates in selection.
     */
    fun resolveReply(reply: String, candidates: List<SavedDishChoiceCandidate>): String? {
        if (candidates.isEmpty() || candidates.size > 3 || reply.length > 160) return null
        val normalized = normalize(reply)
        val ordinal = ORDINAL.matchEntire(normalized)?.groupValues?.getOrNull(1)?.let(::ordinalIndex)
        if (ordinal != null) return candidates.getOrNull(ordinal)?.dish?.id

        val exact = candidates.filter { normalize(it.dish.name) == normalized }
        return exact.singleOrNull()?.dish?.id
    }

    /** Keeps an apparent but invalid choice local so Gemma cannot guess which dish was meant. */
    fun isChoiceAttempt(reply: String, candidates: List<SavedDishChoiceCandidate>): Boolean {
        if (candidates.isEmpty() || candidates.size > 3 || reply.length > 160) return false
        val normalized = normalize(reply)
        return INVALID_OR_AMBIGUOUS_CHOICE.matches(normalized) ||
            candidates.count { normalize(it.dish.name) == normalized } > 1
    }

    fun isDeclineReply(reply: String): Boolean = normalize(reply) in DECLINE_REPLIES

    fun canRetrySelection(pending: PendingSavedDishChoices, candidateId: String): Boolean =
        pending.candidates.any { it.dish.id == candidateId } &&
            (pending.selectedCandidateId == null || pending.selectedCandidateId == candidateId)

    fun acknowledgement(dishName: String): String =
        "I matched that to “$dishName”. Review its foods and portions before adding. Nothing has been saved."

    fun declineAcknowledgement(): String =
        "Okay, I left those saved dishes unchanged. Nothing has been added."

    private fun ordinalIndex(value: String): Int = when (value) {
        "first", "1", "1st", "one" -> 0
        "second", "2", "2nd", "two" -> 1
        "third", "3", "3rd", "three" -> 2
        else -> -1
    }.takeIf { it >= 0 } ?: -1

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        .replace(PUNCTUATION, " ")
        .trim()
        .replace(WHITESPACE, " ")

    private val ORDINAL = Regex("^(?:the )?(first|1st|one|second|2nd|two|third|3rd|three|1|2|3)(?: one)?$")
    private val INVALID_OR_AMBIGUOUS_CHOICE = Regex(
        "^(?:(?:the )?(?:first|1st|one|second|2nd|two|third|3rd|three|fourth|4th|four|fifth|5th|last|only|\\d+)(?: one)?|(?:option|number) \\d+|yes|yeah|yep|sure|that one|that one please)$",
    )
    private val DECLINE_REPLIES = setOf("no", "nope", "neither", "none", "cancel", "cancel that", "no thanks")
    private val PUNCTUATION = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE = Regex("\\s+")
}
