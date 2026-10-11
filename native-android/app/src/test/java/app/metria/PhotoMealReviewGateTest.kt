package app.metria

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PhotoMealReviewGateTest {
    private val proposal = MealProposal(
        listOf(MealProposalItem("Rice", "", "one serving", GramRange(100, 150), "cooked", "medium")),
        emptyList(), emptyList(), "medium",
    )
    private val idle = ModelStatus(
        state = "loaded", downloadedBytes = 0, expectedBytes = 0, reason = "", testOutput = null,
        backendSummary = "", imageState = "none", imageProgress = "none", imageReason = "",
        proposal = null, proposalDiagnostic = null, ocrText = null, labelDraft = null,
        referenceThumbnailBase64 = null, imageRetryAtElapsedRealtime = null,
        debugLatestModelResponse = null, assistantProposal = null, assistantDiagnostic = null,
        coachReply = null, coachStreamingReply = null, coachDiagnostic = null,
        assistantOperation = null, assistantRequestId = null, coachToolCall = null, diagnostics = "",
        latestImageAudit = null, mealFoodCorrection = MealFoodNameCorrectionStatus(),
    )
    private fun started(id: String = "new") = idle.copy(imageState = "analyzing", imageAnalysisRequestId = id)
    private fun completed(id: String = "new", outcome: String = "draft_ready") = idle.copy(
        imageAnalysisRequestId = id, proposal = proposal,
        latestImageAudit = LocalAnalysisAudit(id, Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000,
            "model", "image_meal", outcome),
    )

    @Test fun validAndPartialDraftsOpenExactlyOnce() {
        for (outcome in listOf("draft_ready", "draft_partial")) {
            val gate = PhotoMealReviewGate()
            gate.started(idle, started())
            assertTrue(gate.ready(completed(outcome = outcome)))
            assertEquals(proposal, gate.consume(completed(outcome = outcome)))
            assertNull(gate.consume(completed(outcome = outcome)))
        }
    }

    @Test fun staleAndUnrequestedResultsDoNotNavigate() {
        val gate = PhotoMealReviewGate()
        assertNull(gate.consume(completed()))
        gate.started(idle, started())
        assertNull(gate.consume(completed("old")))
        assertNull(gate.consume(started().copy(proposal = proposal)))
        assertEquals(proposal, gate.consume(completed()))
    }

    @Test fun stopLeavingAndNewAttemptInvalidateOldResult() {
        val gate = PhotoMealReviewGate()
        gate.started(idle, started())
        gate.cancel()
        assertNull(gate.consume(completed()))
        gate.started(started(), started("second"))
        assertNull(gate.consume(completed()))
        assertEquals(proposal, gate.consume(completed("second")))
    }

    @Test fun failedCancelledRejectedAndEmptyResultsDoNotNavigateOrReplay() {
        for (outcome in listOf("runtime_failure", "cancelled", "draft_rejected")) {
            val gate = PhotoMealReviewGate()
            gate.started(idle, started())
            assertNull(gate.consume(completed(outcome = outcome)))
            assertNull(gate.consume(completed()))
        }
        val gate = PhotoMealReviewGate()
        gate.started(idle, started())
        assertNull(gate.consume(completed().copy(proposal = null)))
        assertNull(gate.consume(completed()))
    }

    @Test fun declinedStartCannotAttachToExistingRequest() {
        val gate = PhotoMealReviewGate()
        gate.started(started(), started())
        assertNull(gate.consume(completed()))
        gate.started(idle, completed())
        assertNull(gate.consume(completed()))
    }
}
