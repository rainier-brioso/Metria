package app.metria.ui.screens

import app.metria.shouldConsumeAssistantResult
import app.metria.AssistantPendingSubmissionGate
import app.metria.shouldPresentAssistantThinking
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

    @Test
    fun `record evidence is collapsed until the member expands it`() {
        assertEquals("Record details collapsed", evidenceStateDescription(expanded = false))
        assertEquals("Record details expanded", evidenceStateDescription(expanded = true))
    }

    @Test
    fun `pending submission presents thinking before lifecycle starts`() {
        assertTrue(shouldPresentAssistantThinking(
            localPending = true,
            modelResultIsCurrent = false,
            modelState = "loaded",
            operation = null,
        ))
    }

    @Test
    fun `pending submission rejects duplicate sends`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()

        assertTrue(token != null)
        assertFalse(gate.begin() != null)
        assertTrue(gate.isActive(token!!))
    }

    @Test
    fun `cancellation clears pending submission`() {
        val gate = AssistantPendingSubmissionGate()
        val cancelled = gate.begin()!!

        assertTrue(gate.cancel(cancelled))
        assertFalse(gate.isActive(cancelled))
    }

    @Test
    fun `rejected lifecycle start clears pending submission`() {
        val gate = AssistantPendingSubmissionGate()
        val rejected = gate.begin()!!

        assertEquals(null, gate.reconcile(
            rejected,
            requestId = null,
            previousRequestId = null,
            state = "loaded",
            operation = null,
        ))
        assertFalse(gate.isActive(rejected))
    }

    @Test
    fun `accepted lifecycle request owns presentation before optimistic state clears`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(42L, gate.reconcile(
            token,
            requestId = 42L,
            previousRequestId = 41L,
            state = "thinking",
            operation = "agent_turn",
        ))
        assertTrue(gate.isActive(token))
        assertTrue(gate.observeLifecycle(42L, state = "thinking"))
        assertFalse(gate.isActive(token))
    }

    @Test
    fun `lifecycle failure observation clears reconciled pending submission`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(42L, gate.reconcile(
            token,
            requestId = 42L,
            previousRequestId = 41L,
            state = "thinking",
            operation = "agent_turn",
        ))
        assertTrue(gate.observeLifecycle(42L, state = "loaded"))
        assertFalse(gate.isActive(token))
    }

    @Test
    fun `existing continuation request cannot accept a new typed submission`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(null, gate.reconcile(
            token,
            requestId = 42L,
            previousRequestId = 42L,
            state = "thinking",
            operation = "agent_tool_result",
        ))
        assertFalse(gate.isActive(token))
    }

    @Test
    fun `waiting tool handoff remains busy before continuation coroutine starts`() {
        assertTrue(shouldPresentAssistantThinking(
            localPending = false,
            modelResultIsCurrent = true,
            modelState = "loaded",
            operation = "agent_waiting_tool",
        ))
    }
}
