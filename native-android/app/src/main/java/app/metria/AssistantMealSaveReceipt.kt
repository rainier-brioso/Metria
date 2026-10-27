package app.metria

import java.time.Instant

/** Host-authored save acknowledgement, never model output or fresh record-read evidence. */
object AssistantMealSaveReceipt {
    fun append(
        record: PrivateConversationRecord?,
        meal: MealEntry,
        now: Instant,
        createReceiptOnlyConversation: Boolean = false,
        updated: Boolean = false,
    ): PrivateConversationRecord? {
        val active = PrivateConversationPolicy.active(record, now)
        if (active == null && !createReceiptOnlyConversation) return null
        val foods = meal.items.take(4).joinToString(", ") { it.name.trim().take(80) }
        val additional = if (meal.items.size > 4) ", …" else ""
        val title = meal.title?.trim()?.takeIf(String::isNotEmpty)?.take(160)
        val action = if (updated) "Meal updated on this phone" else "Meal saved on this phone"
        val text = if (title == null) {
            "$action: $foods$additional. Reviewed and confirmed."
        } else {
            "$action: $title.\nFoods: $foods$additional. Reviewed and confirmed."
        }
        val receipt = PrivateConversationMessage(
            text, fromMember = false, savedMealId = meal.id,
        )
        return PrivateConversationPolicy.updated(
            (active?.messages.orEmpty() + receipt), now,
        )
    }
}
