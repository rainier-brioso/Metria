package app.metria

import java.time.Instant
import java.util.UUID

/*
 * Approved local-audit direction: image analysis keeps a small, encrypted,
 * database-shaped record for migration and failure diagnosis. It deliberately
 * excludes the photo, prompt, model response, and any meal text.
 */
data class LocalAnalysisAudit(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Instant,
    val completedAt: Instant,
    val durationMillis: Long,
    val modelId: String,
    val operation: String,
    val outcome: String,
    val failureCategory: String? = null,
    val metrics: LocalAnalysisMetrics? = null,
)

/** Technical measurements only. Null means unavailable, never an inferred zero. */
data class LocalAnalysisMetrics(
    val imagePreparationMillis: Long? = null,
    val conversationSetupMillis: Long? = null,
    val modelRequestMillis: Long? = null,
    val firstTokenMillis: Long? = null,
    val validationMillis: Long? = null,
    val thermalStatusStart: Int? = null,
    val thermalStatusEnd: Int? = null,
)

object LocalAnalysisAuditValidation {
    private val outcomes = setOf("draft_ready", "draft_partial", "draft_rejected", "runtime_failure", "cancelled")

    fun valid(audit: LocalAnalysisAudit): Boolean =
        runCatching { UUID.fromString(audit.id) }.isSuccess &&
            !audit.completedAt.isBefore(audit.startedAt) &&
            audit.durationMillis in 0..600_000 &&
            audit.modelId.length in 1..160 &&
            audit.operation == "image_meal" &&
            audit.outcome in outcomes &&
            (audit.failureCategory == null || audit.failureCategory.length in 1..120) &&
            (audit.metrics == null || validMetrics(audit.metrics))

    fun validMetrics(metrics: LocalAnalysisMetrics): Boolean =
        listOf(metrics.imagePreparationMillis, metrics.conversationSetupMillis,
            metrics.modelRequestMillis, metrics.firstTokenMillis, metrics.validationMillis)
            .all { it == null || it in 0..600_000 } &&
            listOf(metrics.thermalStatusStart, metrics.thermalStatusEnd).all { it == null || it in 0..6 } &&
            (metrics.firstTokenMillis == null || metrics.modelRequestMillis == null ||
                metrics.firstTokenMillis <= metrics.modelRequestMillis)
}
