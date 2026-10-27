package app.metria

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class AssistantCaptureResultPolicyTest {
    private val proposal = MealProposal(
        listOf(MealProposalItem("Rice", "", "one serving", GramRange(100, 150), "cooked", "medium")),
        emptyList(), emptyList(), "medium",
    )
    private val completed = ModelStatus(
        state = "loaded", downloadedBytes = 0, expectedBytes = 0, reason = "", testOutput = null,
        backendSummary = "", imageState = "none", imageProgress = "none", imageReason = "",
        proposal = proposal, proposalDiagnostic = null, ocrText = null, labelDraft = null,
        referenceThumbnailBase64 = null, imageRetryAtElapsedRealtime = null,
        debugLatestModelResponse = null, assistantProposal = null, assistantDiagnostic = null,
        coachReply = null, coachStreamingReply = null, coachDiagnostic = null,
        assistantOperation = null, assistantRequestId = null, coachToolCall = null, diagnostics = "",
        latestImageAudit = LocalAnalysisAudit("active", Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000,
            "model", "image_meal", "draft_ready"), mealFoodCorrection = MealFoodNameCorrectionStatus(),
        imageAnalysisRequestId = "active",
    )

    @Test fun restoredRequestCanReviewItsOwnCompleteOrPartialDraftRepeatedly() {
        assertEquals(proposal, AssistantCaptureResultPolicy.meal("active", completed))
        assertEquals(proposal, AssistantCaptureResultPolicy.meal("active", completed))
        assertEquals(proposal, AssistantCaptureResultPolicy.meal("active", completed.copy(
            latestImageAudit = completed.latestImageAudit!!.copy(outcome = "draft_partial"))))
    }

    @Test fun cancellationAndOtherRequestsCannotExposeStaleDrafts() {
        assertNull(AssistantCaptureResultPolicy.meal(null, completed))
        assertNull(AssistantCaptureResultPolicy.meal("old", completed))
        assertNull(AssistantCaptureResultPolicy.meal("active", completed.copy(imageAnalysisRequestId = "other")))
        assertNull(AssistantCaptureResultPolicy.meal("active", completed.copy(imageState = "analyzing")))
        assertNull(AssistantCaptureResultPolicy.meal("active", completed.copy(proposal = null)))
        for (outcome in listOf("cancelled", "runtime_failure", "draft_rejected")) {
            assertNull(AssistantCaptureResultPolicy.meal("active", completed.copy(latestImageAudit = completed.latestImageAudit!!.copy(outcome = outcome))))
        }
    }

    @Test fun labelKeepsUnknownValuesAndReviewedDecimalInput() {
        val meal = AssistantReviewedLabelPolicy.meal(" Product ", "2026-10-25", LocalTime.NOON, listOf("120,5", "", "20", "3"), null)!!
        assertEquals("Product", meal.items.single().name)
        assertEquals(120.5, meal.nutrition!!.caloriesKcal!!, 0.0)
        assertNull(meal.nutrition!!.proteinGrams)
        assertEquals("verified_label", meal.nutrition!!.source)
        assertNull(meal.referenceThumbnailBase64)
    }

    @Test fun labelRejectsInvalidNumbersNameAndDateWithoutCreatingARecord() {
        for (bad in listOf("-1", "NaN", "Infinity", "abc")) {
            assertNull(AssistantReviewedLabelPolicy.meal("Product", "2026-10-25", LocalTime.NOON, listOf(bad, "", "", ""), null))
        }
        assertNull(AssistantReviewedLabelPolicy.meal("", "2026-10-25", LocalTime.NOON, listOf("120", "", "", ""), null))
        assertNull(AssistantReviewedLabelPolicy.meal("Product", "bad-date", LocalTime.NOON, listOf("120", "", "", ""), null))
    }
}
