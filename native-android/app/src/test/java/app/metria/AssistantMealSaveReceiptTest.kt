package app.metria

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class AssistantMealSaveReceiptTest {
    private val now = Instant.parse("2026-10-02T12:00:00Z")
    private val meal = MealEntry(
        title = "Reviewed lunch", items = listOf(MealEntryItem("confirmed beans", 150, "cooked")),
        assumptions = emptyList(), confidence = "low", source = "ai_estimate",
    )

    @Test fun `receipt uses the confirmed meal and preserves the member conversation`() {
        val message = PrivateConversationMessage("Draft another food", true)
        val previous = PrivateConversationRecord(listOf(message), now.minusSeconds(60))
        val updated = AssistantMealSaveReceipt.append(previous, meal, now)!!
        assertEquals(message, updated.messages.first())
        assertEquals(now, updated.updatedAt)
        val receipt = updated.messages.last()
        assertFalse(receipt.fromMember)
        assertFalse(receipt.localEvidence)
        assertEquals(meal.id, receipt.savedMealId)
        assertTrue(receipt.text.contains("Reviewed lunch"))
        assertTrue(receipt.text.contains("confirmed beans"))
        assertFalse(receipt.text.contains("another food"))
        assertFalse(receipt.text.contains("kcal"))
    }

    @Test fun `save receipt cannot create or revive an expired conversation`() {
        assertNull(AssistantMealSaveReceipt.append(null, meal, now))
        val expired = PrivateConversationRecord(listOf(PrivateConversationMessage("meal", true)), now.minusSeconds(86_400))
        assertNull(AssistantMealSaveReceipt.append(expired, meal, now))
    }

    @Test fun `receipt respects chat limits and works without a meal title`() {
        val previous = PrivateConversationRecord(List(50) { PrivateConversationMessage("meal $it", true) }, now)
        val updated = AssistantMealSaveReceipt.append(previous, meal.copy(title = null), now)!!
        assertEquals(50, updated.messages.size)
        assertTrue(updated.messages.last().text.startsWith("Meal saved on this phone: confirmed beans"))
        assertTrue(updated.messages.last().text.length <= PrivateConversationPolicy.maxMessageCharacters)
    }
}
