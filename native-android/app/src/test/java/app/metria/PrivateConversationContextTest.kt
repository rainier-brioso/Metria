package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateConversationContextTest {
    @Test
    fun `context keeps the pending assistant question for a short follow up`() {
        val context = PrivateConversationContext.build(
            listOf(
                PrivateConversationTurn(PrivateConversationRole.Member, "How is my week?"),
                PrivateConversationTurn(PrivateConversationRole.LocalEvidence, "Last 7 days: 4 meals."),
                PrivateConversationTurn(PrivateConversationRole.Assistant, "Would you like to review protein for the week?"),
            ),
        )

        assertTrue(context.contains("LOCAL_TOOL_EVIDENCE: Last 7 days: 4 meals."))
        assertTrue(context.contains("PENDING_FOLLOW_UP: Would you like to review protein for the week?"))
    }

    @Test
    fun `context includes only the six most recent sanitized turns`() {
        val context = PrivateConversationContext.build(
            (1..8).map { index -> PrivateConversationTurn(PrivateConversationRole.Member, "turn\u0000 $index") },
        )

        assertFalse(context.contains("turn 1"))
        assertFalse(context.contains("turn 2"))
        assertTrue(context.contains("MEMBER: turn 3"))
        assertTrue(context.contains("MEMBER: turn 8"))
        assertFalse(context.contains('\u0000'))
    }

    @Test
    fun `an older question is not pending after a later assistant answer`() {
        val context = PrivateConversationContext.build(
            listOf(
                PrivateConversationTurn(PrivateConversationRole.Assistant, "Would you like a summary?"),
                PrivateConversationTurn(PrivateConversationRole.Member, "No"),
                PrivateConversationTurn(PrivateConversationRole.Assistant, "Okay, we can discuss something else."),
            ),
        )

        assertFalse(context.contains("PENDING_FOLLOW_UP"))
    }
}
