package app.metria.ui.screens

import app.metria.shouldConsumeAssistantResult
import app.metria.assistantReplyDeliveryGate
import app.metria.AssistantReplyDeliveryGate
import app.metria.AssistantPendingSubmissionGate
import app.metria.AssistantCancellationGate
import app.metria.continuationFailureDiagnostic
import app.metria.retainableAssistantFailure
import app.metria.shouldPresentAssistantThinking
import app.metria.shouldLatchAssistantDiagnostic
import app.metria.shouldSuppressConsumedAssistantDiagnostic
import app.metria.shouldAnnouncePrivateChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import app.metria.AssistantModelOperationMutex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AssistantConversationPresentationTest {
    @Test
    fun `microphone cannot obscure an active response but recording can always stop`() {
        assertFalse(canUseChatMicrophone(ready = true, busy = true, recording = false))
        assertFalse(canUseChatMicrophone(ready = false, busy = false, recording = false))
        assertTrue(canUseChatMicrophone(ready = true, busy = false, recording = false))
        assertTrue(canUseChatMicrophone(ready = false, busy = true, recording = true))
    }
    @Test
    fun `one activity state takes precedence over overlapping lifecycle flags`() {
        assertEquals(ChatActivity.VOICE, chatActivity(true, true, true, true, true))
        assertEquals(ChatActivity.STOPPING, chatActivity(false, true, true, true, true))
        assertEquals(ChatActivity.PREPARING_MEAL, chatActivity(false, false, true, true, true))
        assertEquals(ChatActivity.THINKING, chatActivity(false, false, false, true, true))
        assertEquals(ChatActivity.STOPPED, chatActivity(false, false, false, false, true))
        assertEquals(ChatActivity.NONE, chatActivity(false, false, false, false, false))
    }

    @Test
    fun `current completed rejection becomes one retained assistant message`() {
        assertEquals("Metria could not verify that answer.", retainableFailure())
        assertEquals("That local answer took too long.", retainableFailure(
            operation = "agent_turn",
            diagnostic = "That local answer took too long.",
        ))
        assertEquals("That local answer took too long.", retainableFailure(
            operation = "agent_tool_result",
            diagnostic = "That local answer took too long.",
        ))
    }

    @Test
    fun `retained rejection gate rejects duplicate pending waiting and successful results`() {
        assertEquals(null, retainableFailure(handledRequestId = 42L))
        assertEquals(null, retainableFailure(localPending = true))
        assertEquals(null, retainableFailure(operation = "agent_waiting_tool"))
        assertEquals(null, retainableFailure(hasReply = true))
        assertEquals(null, retainableFailure(hasToolCall = true))
        assertEquals(null, retainableFailure(hasProposal = true))
        assertEquals(null, retainableFailure(hasNavigation = true))
    }

    @Test
    fun `stale cancelled and locked rejections are not retained`() {
        assertEquals(null, retainableFailure(activeRequestId = 41L))
        assertEquals(null, retainableFailure(activeRequestId = null))
        assertEquals(null, retainableFailure(stopping = true))
        assertEquals(null, retainableFailure(unlocked = false))
    }

    @Test
    fun `new turn makes the previous failure stale`() {
        assertEquals(null, retainableAssistantFailure(
            activeRequestId = 43L,
            handledRequestId = null,
            resultRequestId = 42L,
            modelState = "loaded",
            operation = "agent_reply",
            diagnostic = "Old failure",
            hasReply = false,
            hasToolCall = false,
            hasProposal = false,
            localPending = false,
            stopping = false,
            unlocked = true,
        ))
    }

    @Test
    fun `blank diagnostic does not create a retained message`() {
        assertEquals(null, retainableFailure(diagnostic = "  "))
    }

    @Test
    fun `only a consumed terminal failure suppresses its transient diagnostic`() {
        assertTrue(shouldSuppressFailureDiagnostic())
        assertFalse(shouldSuppressFailureDiagnostic(hasReply = true))
        assertFalse(shouldSuppressFailureDiagnostic(hasNavigation = true))
        assertFalse(shouldSuppressFailureDiagnostic(activeRequestId = 43L))
        assertFalse(shouldSuppressFailureDiagnostic(handledRequestId = null))
    }

    @Test
    fun `polite announcement is limited to the newest retained failure message`() {
        assertTrue(shouldAnnouncePrivateChatMessage(
            failedRequestId = 42L, activeRequestId = 42L, isLast = true,
            fromMember = false, localEvidence = false,
        ))
        assertFalse(shouldAnnouncePrivateChatMessage(
            failedRequestId = 42L, activeRequestId = 42L, isLast = false,
            fromMember = false, localEvidence = false,
        ))
        assertFalse(shouldAnnouncePrivateChatMessage(
            failedRequestId = 41L, activeRequestId = 42L, isLast = true,
            fromMember = false, localEvidence = false,
        ))
        assertFalse(shouldAnnouncePrivateChatMessage(
            failedRequestId = 42L, activeRequestId = 42L, isLast = true,
            fromMember = true, localEvidence = false,
        ))
        assertFalse(shouldAnnouncePrivateChatMessage(
            failedRequestId = 42L, activeRequestId = 42L, isLast = true,
            fromMember = false, localEvidence = true,
        ))
    }

    @Test
    fun `current failed turn can keep its error visible after lifecycle status changes`() {
        assertTrue(shouldLatchAssistantDiagnostic(42L, 42L, "loaded", "Could not verify", false))
        assertFalse(shouldLatchAssistantDiagnostic(42L, 41L, "loaded", "Stale error", false))
        assertFalse(shouldLatchAssistantDiagnostic(42L, 42L, "thinking", "Early error", false))
        assertFalse(shouldLatchAssistantDiagnostic(42L, 42L, "loaded", "Previous error", true))
        assertFalse(shouldLatchAssistantDiagnostic(42L, 42L, "loaded", "Handled", false, handledRequestId = 42L))
    }

    private fun retainableFailure(
        activeRequestId: Long? = 42L,
        handledRequestId: Long? = null,
        resultRequestId: Long? = 42L,
        modelState: String = "loaded",
        operation: String? = "agent_reply",
        diagnostic: String? = "Metria could not verify that answer.",
        hasReply: Boolean = false,
        hasToolCall: Boolean = false,
        hasProposal: Boolean = false,
        hasNavigation: Boolean = false,
        localPending: Boolean = false,
        stopping: Boolean = false,
        unlocked: Boolean = true,
    ): String? = retainableAssistantFailure(
        activeRequestId = activeRequestId,
        handledRequestId = handledRequestId,
        resultRequestId = resultRequestId,
        modelState = modelState,
        operation = operation,
        diagnostic = diagnostic,
        hasReply = hasReply,
        hasToolCall = hasToolCall,
        hasProposal = hasProposal,
        hasNavigation = hasNavigation,
        localPending = localPending,
        stopping = stopping,
        unlocked = unlocked,
    )

    private fun shouldSuppressFailureDiagnostic(
        activeRequestId: Long? = 42L,
        handledRequestId: Long? = 42L,
        resultRequestId: Long? = 42L,
        modelState: String = "loaded",
        operation: String? = "agent_reply",
        diagnostic: String? = "Metria could not verify that answer.",
        hasReply: Boolean = false,
        hasToolCall: Boolean = false,
        hasProposal: Boolean = false,
        hasNavigation: Boolean = false,
    ): Boolean = shouldSuppressConsumedAssistantDiagnostic(
        activeRequestId = activeRequestId,
        handledRequestId = handledRequestId,
        resultRequestId = resultRequestId,
        modelState = modelState,
        operation = operation,
        diagnostic = diagnostic,
        hasReply = hasReply,
        hasToolCall = hasToolCall,
        hasProposal = hasProposal,
        hasNavigation = hasNavigation,
    )
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
    fun `reply delivery reports active and already handled gate outcomes`() {
        assertEquals(AssistantReplyDeliveryGate.CONSUME, assistantReplyDeliveryGate(42L, null, 42L))
        assertEquals(AssistantReplyDeliveryGate.ACTIVE_REQUEST_MISMATCH, assistantReplyDeliveryGate(null, null, 42L))
        assertEquals(AssistantReplyDeliveryGate.ACTIVE_REQUEST_MISMATCH, assistantReplyDeliveryGate(43L, null, 42L))
        assertEquals(AssistantReplyDeliveryGate.ALREADY_HANDLED, assistantReplyDeliveryGate(42L, 42L, 42L))
        assertEquals(null, assistantReplyDeliveryGate(42L, null, null))
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
    fun `assistant cancellation stays busy until native cancellation completes`() {
        val gate = AssistantCancellationGate()
        val cancellation = gate.begin()!!

        assertTrue(gate.isInFlight())
        assertEquals(null, gate.begin())
        assertTrue(gate.complete(cancellation))
        assertFalse(gate.isInFlight())
    }

    @Test
    fun `conversation reset prevents old cancellation from presenting stopped confirmation`() {
        val gate = AssistantCancellationGate()
        val cancellation = gate.begin()!!

        gate.reset()

        assertTrue(gate.isInFlight())
        assertFalse(gate.complete(cancellation))
        assertFalse(gate.isInFlight())
    }

    @Test
    fun `queued assistant start rechecks its token after cancellation owns the operation lock`() = runBlocking {
        val operations = AssistantModelOperationMutex()
        val pending = AssistantPendingSubmissionGate()
        val token = pending.begin()!!
        val cancellationEntered = CompletableDeferred<Unit>()
        val finishCancellation = CompletableDeferred<Unit>()
        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
            operations.withLock {
                cancellationEntered.complete(Unit)
                finishCancellation.await()
            }
        }
        cancellationEntered.await()

        val startQueued = CompletableDeferred<Unit>()
        var started = false
        val start = launch(start = CoroutineStart.UNDISPATCHED) {
            startQueued.complete(Unit)
            operations.withLock {
                if (pending.isActive(token)) started = true
            }
        }
        startQueued.await()
        pending.cancel(token)
        finishCancellation.complete(Unit)

        cancellation.join()
        start.join()
        assertFalse(started)
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

    @Test
    fun `accepted tool continuation adopts only its new lifecycle request`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(43L, gate.reconcile(
            token = token,
            requestId = 43L,
            previousRequestId = 42L,
            state = "thinking",
            operation = "agent_tool_result",
        ))
        assertTrue(gate.observeLifecycle(43L, "thinking"))
        assertTrue(shouldConsumeAssistantResult(43L, null, 43L))
    }

    @Test
    fun `fast completed tool continuation is adopted when reply arrives before handoff`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(43L, gate.reconcile(token, 43L, 42L, "loaded", "agent_reply"))
        assertFalse(gate.isActive(token))
        assertTrue(shouldConsumeAssistantResult(43L, null, 43L))
    }

    @Test
    fun `loaded poll before request reconciliation retires matching continuation`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertFalse(gate.observeLifecycle(43L, "loaded"))
        assertEquals(43L, gate.reconcile(token, 43L, 42L, "thinking", "agent_tool_result"))
        assertFalse(gate.isActive(token))
        assertTrue(shouldConsumeAssistantResult(43L, null, 43L))
    }

    @Test
    fun `loaded poll for old request does not retire a newer continuation`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertFalse(gate.observeLifecycle(42L, "loaded"))
        assertEquals(43L, gate.reconcile(token, 43L, 42L, "thinking", "agent_tool_result"))
        assertTrue(gate.isActive(token))
        assertTrue(gate.observeLifecycle(43L, "thinking"))
    }

    @Test
    fun `rejected tool handoff without a new request clears pending ownership`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!

        assertEquals(null, gate.reconcile(
            token = token,
            requestId = 42L,
            previousRequestId = 42L,
            state = "loaded",
            operation = "agent_waiting_tool",
        ))
        assertFalse(gate.isActive(token))
        assertFalse(shouldConsumeAssistantResult(null, null, 42L))
    }

    @Test
    fun `rejected tool handoff exposes a safe fallback when no lifecycle diagnostic exists`() {
        assertEquals(
            "Metria could not complete that local reply. Try asking again.",
            continuationFailureDiagnostic(null),
        )
        assertEquals("The local answer could not be checked.", continuationFailureDiagnostic("The local answer could not be checked."))
        assertEquals(
            "Metria could not complete that local reply. Try asking again.",
            continuationFailureDiagnostic("  "),
        )
    }

    @Test
    fun `cancelled tool continuation cannot expose a late result`() {
        val gate = AssistantPendingSubmissionGate()
        val token = gate.begin()!!
        assertEquals(43L, gate.reconcile(token, 43L, 42L, "thinking", "agent_tool_result"))

        assertTrue(gate.cancel(token))
        assertFalse(shouldConsumeAssistantResult(null, null, 43L))
    }

    @Test
    fun `matching assistant result is consumed exactly once`() {
        assertTrue(shouldConsumeAssistantResult(43L, null, 43L))
        // The production effect records the request ID before appending the visible reply.
        assertFalse(shouldConsumeAssistantResult(43L, 43L, 43L))
    }
}
