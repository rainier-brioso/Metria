package app.metria

import java.time.Instant

/** Host-authored save acknowledgement, never model output or fresh record-read evidence. */
object AssistantMealSaveReceipt {
    fun append(record: PrivateConversationRecord?, meal: MealEntry, now: Instant): PrivateConversationRecord? {
        val active = PrivateConversationPolicy.active(record, now) ?: return null
        val foods = meal.items.take(4).joinToString(", ") { it.name.trim().take(80) }
        val additional = if (meal.items.size > 4) ", …" else ""
        val title = meal.title?.trim()?.takeIf(String::isNotEmpty)?.take(160)
        val text = if (title == null) {
            "Meal saved on this phone: $foods$additional. Reviewed and confirmed."
        } else {
            "Meal saved on this phone: $title.\nFoods: $foods$additional. Reviewed and confirmed."
        }
        return PrivateConversationPolicy.updated(active.messages + PrivateConversationMessage(
            text, fromMember = false, savedMealId = meal.id,
        ), now)
    }
}
