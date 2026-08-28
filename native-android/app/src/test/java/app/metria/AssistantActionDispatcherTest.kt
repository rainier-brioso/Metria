package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantActionDispatcherTest {
    @Test
    fun `native progress overview resolves through the host allowlist`() {
        assertEquals(
            AssistantAction.QueryProgressOverview("week"),
            AssistantActionDispatcher.resolve(
                PrivateCoachToolCall(intent = "read_progress_overview", period = "week"),
            ),
        )
    }

    @Test
    fun `native nutrition tool resolves through the host allowlist`() {
        assertEquals(
            AssistantAction.QueryNutrition("week"),
            AssistantActionDispatcher.resolve(
                PrivateCoachToolCall(intent = "read_nutrition_summary", period = "week"),
            ),
        )
    }

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

    @Test
    fun `personal weekly progress cannot bypass the nutrition read tool`() {
        val guarded = AssistantActionGuard.validate(
            PrivateCoachToolCall(intent = "respond", message = "Tell me more."),
            memberText = "¿Cómo va mi semana?",
            conversationContext = "",
        )

        assertEquals("get_nutrition_summary", guarded.intent)
        assertEquals("week", guarded.period)
        assertEquals(null, guarded.message)
    }

    @Test
    fun `affirmative follow up uses the pending weekly progress tool`() {
        val guarded = AssistantActionGuard.validate(
            PrivateCoachToolCall(intent = "respond", message = "Great."),
            memberText = "sí",
            conversationContext = "METRIA: ¿Quieres revisar tu proteína semanal?\nPENDING_FOLLOW_UP: ¿Quieres revisar tu proteína semanal?",
        )

        assertEquals("get_nutrition_summary", guarded.intent)
        assertEquals("week", guarded.period)
    }
}
