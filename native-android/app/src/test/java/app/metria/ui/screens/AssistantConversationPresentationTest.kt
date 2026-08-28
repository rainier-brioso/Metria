package app.metria.ui.screens

import app.metria.shouldConsumeAssistantResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantConversationPresentationTest {
    @Test
    fun `assistant home uses calm member language`() {
        assertEquals("Ready when you are", assistantHomeTitle("loaded"))
        assertEquals("Private help with meals, weight, and progress", assistantHomeSupporting("loaded"))
        assertEquals("Assistant needs attention", assistantHomeTitle("failed"))
    }

    @Test
    fun `current unhandled result is consumed`() {
        assertTrue(shouldConsumeAssistantResult(activeRequestId = 42L, handledRequestId = null, resultRequestId = 42L))
    }

    @Test
    fun `completed result is not replayed after reopening`() {
        assertFalse(shouldConsumeAssistantResult(activeRequestId = 42L, handledRequestId = 42L, resultRequestId = 42L))
    }

    @Test
    fun `stale lifecycle result is ignored by a fresh conversation`() {
        assertFalse(shouldConsumeAssistantResult(activeRequestId = null, handledRequestId = null, resultRequestId = 41L))
        assertFalse(shouldConsumeAssistantResult(activeRequestId = 42L, handledRequestId = null, resultRequestId = 41L))
    }
}
