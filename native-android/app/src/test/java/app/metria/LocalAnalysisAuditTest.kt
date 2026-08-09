package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LocalAnalysisAuditTest {
    @Test
    fun acceptsSafeImageAuditMetadata() {
        val started = Instant.parse("2026-08-09T16:00:00Z")
        assertTrue(LocalAnalysisAuditValidation.valid(LocalAnalysisAudit(
            startedAt = started,
            completedAt = started.plusSeconds(12),
            durationMillis = 12_000,
            modelId = "gemma-4-E2B-it.litertlm@9262660",
            operation = "image_meal",
            outcome = "draft_ready",
        )))
    }

    @Test
    fun rejectsAnAuditWithAnUnboundedDuration() {
        val started = Instant.parse("2026-08-09T16:00:00Z")
        assertFalse(LocalAnalysisAuditValidation.valid(LocalAnalysisAudit(
            startedAt = started,
            completedAt = started.plusSeconds(601),
            durationMillis = 601_000,
            modelId = "model",
            operation = "image_meal",
            outcome = "draft_ready",
        )))
    }
}
