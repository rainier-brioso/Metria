package app.metria

/** One-shot navigation for an explicitly started photo request, never a vault write. */
class PhotoMealReviewGate {
    private var requestId: String? = null

    fun started(before: ModelStatus, after: ModelStatus) {
        requestId = after.imageAnalysisRequestId?.takeIf {
            after.imageState == "analyzing" && it != before.imageAnalysisRequestId
        }
    }

    fun cancel() { requestId = null }

    fun ready(status: ModelStatus): Boolean = requestId != null &&
        status.imageState == "none" && status.latestImageAudit?.id == requestId &&
        status.imageAnalysisRequestId == requestId &&
        status.latestImageAudit?.outcome in setOf("draft_ready", "draft_partial") && status.proposal != null

    fun consume(status: ModelStatus): MealProposal? {
        if (status.imageState != "none" || status.latestImageAudit?.id != requestId || requestId == null) return null
        val proposal = status.proposal.takeIf { ready(status) }
        requestId = null // Consume before navigation; rejection/cancellation also end this attempt.
        return proposal
    }
}
