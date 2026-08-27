package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantActionDispatcherTest {
    @Test
    fun `saved dish lookup accepts only a bounded query`() {
        val action = AssistantActionDispatcher.resolve(
            PrivateCoachToolCall(intent = "search_saved_dishes", query = "  San Jacobo  "),
        )

        assertEquals(AssistantAction.SearchSavedDishes("San Jacobo"), action)
    }

    @Test
    fun `saved dish lookup rejects missing and oversized queries`() {
        assertTrue(
            AssistantActionDispatcher.resolve(PrivateCoachToolCall(intent = "search_saved_dishes")) is AssistantAction.Clarify,
        )
        assertTrue(
            AssistantActionDispatcher.resolve(
                PrivateCoachToolCall(intent = "search_saved_dishes", query = "x".repeat(121)),
            ) is AssistantAction.Clarify,
        )
    }
}
