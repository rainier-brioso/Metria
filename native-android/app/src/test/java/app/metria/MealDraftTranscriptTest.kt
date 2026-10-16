package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MealDraftTranscriptTest {
    @Test
    fun completedCurrentDraftGetsOneHistoricalReplyWithoutClaimingItWasSaved() {
        val reply = mealDraftTranscriptAcknowledgement(42, null, 42, "loaded", true, "agent_meal_draft", true)
        assertNotNull(reply)
        assertFalse(requireNotNull(reply).contains("meal saved", ignoreCase = true))
        assertNull(mealDraftTranscriptAcknowledgement(42, 42, 42, "loaded", true, "agent_meal_draft", true))
        // Discarding the transient proposal does not alter the retained conversation message.
        val transcript = listOf(PrivateConversationMessage(reply, fromMember = false))
        assertNull(mealDraftTranscriptAcknowledgement(42, 42, 42, "loaded", false, "agent_meal_draft", true))
        assertEquals(reply, transcript.single().text)
    }

    @Test
    fun staleLockedStoppedIncompleteAndFailedDraftsCannotCreateAcknowledgements() {
        assertNull(mealDraftTranscriptAcknowledgement(null, null, 42, "loaded", true, "meal", true))
        assertNull(mealDraftTranscriptAcknowledgement(43, null, 42, "loaded", true, "meal", true))
        assertNull(mealDraftTranscriptAcknowledgement(42, null, 42, "loaded", true, "meal", false))
        assertNull(mealDraftTranscriptAcknowledgement(42, null, 42, "thinking", true, "meal", true))
        assertNull(mealDraftTranscriptAcknowledgement(42, null, 42, "loaded", false, "meal", true))
        assertNull(mealDraftTranscriptAcknowledgement(42, null, 42, "loaded", true, "agent_reply", true))
    }
}
