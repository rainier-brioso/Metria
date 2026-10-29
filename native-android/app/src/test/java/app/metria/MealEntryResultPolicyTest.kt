package app.metria

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealEntryResultPolicyTest {
    @Test fun `manual stop keeps attempt active until queued auto-stop callback transfers clip`() {
        val recorder = FakeAudioRecorder()
        val capture = MealEntryAudioCapture(recorder)
        val token = capture.beginAttempt()
        val delivered = mutableListOf<GemmaAudioRecorder.Clip>()
        capture.start(token) { delivered += it }.getOrThrow()

        assertEquals(MealEntryStopResult.AwaitingAutoStop, capture.stop(token))
        assertTrue(capture.isActive(token))
        val clip = clipFile()
        recorder.callbacks.single()(clip)

        assertEquals(listOf(clip), delivered)
        assertTrue(clip.file.exists()) // Ownership transferred to the transcription callback.
        clip.file.delete()
    }

    @Test fun `cancel while awaiting auto-stop deletes the queued clip`() {
        val recorder = FakeAudioRecorder()
        val capture = MealEntryAudioCapture(recorder)
        val token = capture.beginAttempt()
        var delivered = false
        capture.start(token) { delivered = true }.getOrThrow()
        assertEquals(MealEntryStopResult.AwaitingAutoStop, capture.stop(token))
        capture.cancel()

        val clip = clipFile()
        recorder.callbacks.single()(clip)
        assertFalse(delivered)
        assertFalse(clip.file.exists())
    }

    @Test fun `queued auto-stop from earlier attempt cannot replace a newer recording`() {
        val recorder = FakeAudioRecorder()
        val capture = MealEntryAudioCapture(recorder)
        val oldToken = capture.beginAttempt()
        val delivered = mutableListOf<GemmaAudioRecorder.Clip>()
        capture.start(oldToken) { delivered += it }.getOrThrow()
        assertEquals(MealEntryStopResult.AwaitingAutoStop, capture.stop(oldToken))

        val newToken = capture.beginAttempt()
        capture.start(newToken) { delivered += it }.getOrThrow()
        val oldClip = clipFile()
        recorder.callbacks[0](oldClip)
        assertFalse(oldClip.file.exists())

        val newClip = clipFile()
        recorder.callbacks[1](newClip)
        assertEquals(listOf(newClip), delivered)
        assertTrue(newClip.file.exists())
        newClip.file.delete()
    }

    private fun clipFile(): GemmaAudioRecorder.Clip = GemmaAudioRecorder.Clip(
        File.createTempFile("meal-entry-", ".wav"), durationMs = 500, detectedSpeech = true,
    )

    private class FakeAudioRecorder : MealEntryAudioRecorder {
        val callbacks = mutableListOf<(GemmaAudioRecorder.Clip) -> Unit>()

        override fun start(onAutoStop: (GemmaAudioRecorder.Clip) -> Unit): Result<Unit> {
            callbacks += onAutoStop
            return Result.success(Unit)
        }

        override fun stop(): GemmaAudioRecorder.Clip? = null
        override fun cancel() = Unit
    }

    @Test fun `permission and auto-stop callbacks are invalidated on cancel or a newer attempt`() {
        val gate = MealEntryAttemptGate()
        val pendingPermission = gate.begin()
        assertTrue(gate.isCurrent(pendingPermission))
        gate.cancel()
        assertFalse(gate.isCurrent(pendingPermission))
        assertFalse(gate.finish(pendingPermission))

        val currentCapture = gate.begin()
        assertFalse(gate.finish(pendingPermission))
        assertTrue(gate.finish(currentCapture))
        assertFalse(gate.finish(currentCapture))
    }

    @Test fun `only a new matching operation can be attached to the entry`() {
        val previous = status(id = 7, operation = "meal", state = "loaded")
        assertEquals(8L, MealEntryResultPolicy.startedAfter(previous, status(8, "meal", "thinking"), "meal"))
        assertNull(MealEntryResultPolicy.startedAfter(previous, status(7, "meal", "thinking"), "meal"))
        assertNull(MealEntryResultPolicy.startedAfter(previous, status(8, "coach_audio", "thinking"), "meal"))
        assertNull(MealEntryResultPolicy.startedAfter(previous, status(8, "meal", "loaded"), "meal"))
    }

    @Test fun `late or wrong operation completion cannot be consumed`() {
        assertTrue(MealEntryResultPolicy.isCompletedResult(8, status(8, "meal", "loaded"), "meal"))
        assertFalse(MealEntryResultPolicy.isCompletedResult(8, status(9, "meal", "loaded"), "meal"))
        assertFalse(MealEntryResultPolicy.isCompletedResult(8, status(8, "coach_audio", "loaded"), "meal"))
        assertFalse(MealEntryResultPolicy.isCompletedResult(8, status(8, "meal", "thinking"), "meal"))
        assertFalse(MealEntryResultPolicy.isCompletedResult(null, status(8, "meal", "loaded"), "meal"))
    }

    @Test fun `draft details are exposed only for the matching completed meal request`() {
        val failed = status(
            id = 8,
            operation = "meal",
            state = "loaded",
            draftCategory = "invalid_root_shape",
            debugResponse = "not JSON",
        )
        assertEquals(
            MealEntryResultPolicy.DraftDebugDetails(8, "invalid_root_shape", "not JSON"),
            MealEntryResultPolicy.draftDebugDetails(8, failed),
        )
        assertNull(MealEntryResultPolicy.draftDebugDetails(7, failed))
        assertNull(MealEntryResultPolicy.draftDebugDetails(8, failed.copy(assistantOperation = "coach_audio")))
        assertNull(MealEntryResultPolicy.draftDebugDetails(8, failed.copy(state = "thinking")))
    }

    @Test fun `voice text remains an editable validated transcript`() {
        assertEquals("rice and chicken", MealEntryResultPolicy.transcript(status(spoken = " rice and chicken ")))
        assertNull(MealEntryResultPolicy.transcript(status(spoken = "\u0000")))
        assertNull(MealEntryResultPolicy.transcript(status(spoken = " ")))
    }

    @Test fun `automatic voice draft requires the successful matching audio attempt`() {
        val completed = status(8, "coach_audio", spoken = "rice and chicken")
        assertEquals("rice and chicken", MealEntryResultPolicy.transcriptForDraft(8, completed))
        assertNull(MealEntryResultPolicy.transcriptForDraft(7, completed))
        assertNull(MealEntryResultPolicy.transcriptForDraft(null, completed))
        assertNull(MealEntryResultPolicy.transcriptForDraft(8, completed.copy(state = "failed")))
        assertNull(MealEntryResultPolicy.transcriptForDraft(8, completed.copy(state = "thinking")))
        assertNull(MealEntryResultPolicy.transcriptForDraft(8, completed.copy(assistantOperation = "meal")))
        assertNull(MealEntryResultPolicy.transcriptForDraft(8, status(8, "coach_audio", spoken = " ")))
    }

    @Test fun `cloud meal entry accepts the local transcript without claiming Gemma is loaded`() {
        val cloudStart = status(8, "coach_audio", state = "thinking", cloudReady = true)
        val cloudComplete = status(8, "coach_audio", state = "absent", spoken = "rice and chicken", cloudReady = true)
        assertEquals(8L, MealEntryResultPolicy.startedAfter(status(7), cloudStart, "coach_audio"))
        assertEquals("rice and chicken", MealEntryResultPolicy.transcriptForDraft(8, cloudComplete))
    }

    @Test fun `cloud clarification and connection failure survive entry presentation without stale feedback`() {
        val clarification = status(8, "meal", state = "absent", cloudReady = true)
            .copy(cloudSelected = true, coachReply = "Did you mean rice?")
        assertEquals("Did you mean rice?", MealEntryResultPolicy.feedback(8, clarification))
        assertNull(MealEntryResultPolicy.feedback(7, clarification))
        assertNull(MealEntryResultPolicy.feedback(8, clarification.copy(state = "thinking")))
        val failure = clarification.copy(coachReply = null, assistantDiagnostic = "Could not reach the cloud assistant.")
        assertEquals(failure.assistantDiagnostic, MealEntryResultPolicy.feedback(8, failure))
        assertTrue(MealEntryResultPolicy.canPrepare(clarification))
        assertFalse(MealEntryResultPolicy.canPrepare(clarification.copy(state = "thinking")))
        assertFalse(MealEntryResultPolicy.canPrepare(clarification.copy(state = "loaded", cloudReady = false)))
        assertTrue(MealEntryResultPolicy.canPrepare(status(state = "loaded")))
        assertFalse(MealEntryResultPolicy.canPrepare(status(state = "absent")))
    }

    private fun status(
        id: Long? = null,
        operation: String? = null,
        state: String = "loaded",
        spoken: String? = null,
        cloudReady: Boolean = false,
        draftCategory: String? = null,
        debugResponse: String? = null,
    ) = ModelStatus(
        state = state,
        downloadedBytes = 0,
        expectedBytes = 0,
        reason = "",
        testOutput = null,
        backendSummary = "",
        imageState = "idle",
        imageProgress = "",
        imageReason = "",
        proposal = null,
        proposalDiagnostic = null,
        ocrText = null,
        labelDraft = null,
        referenceThumbnailBase64 = null,
        imageRetryAtElapsedRealtime = null,
        debugLatestModelResponse = debugResponse,
        assistantProposal = null,
        assistantDiagnostic = null,
        coachReply = null,
        coachStreamingReply = null,
        coachDiagnostic = null,
        assistantOperation = operation,
        assistantRequestId = id,
        coachToolCall = spoken?.let { PrivateCoachToolCall("respond", spokenText = it) },
        diagnostics = "",
        latestImageAudit = null,
        mealFoodCorrection = MealFoodNameCorrectionStatus(),
        debugLatestMealDraftCategory = draftCategory,
        cloudReady = cloudReady,
    )
}
