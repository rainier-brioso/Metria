package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniLmRouteScoringTest {
    @Test
    fun acceptsOnlyWhenPositiveThresholdAndCompetitorMarginPass() {
        assertEquals(
            MiniLmRouteOutcome.ACCEPTED,
            MiniLmRouteScoring.decide(0.64f, 0.51f, 0.52f).outcome,
        )
        assertEquals(
            MiniLmRouteOutcome.SIMILARITY_REJECTED,
            MiniLmRouteScoring.decide(0.579f, 0.20f, 0.30f).outcome,
        )
        assertEquals(
            MiniLmRouteOutcome.MARGIN_REJECTED,
            MiniLmRouteScoring.decide(0.64f, 0.59f, 0.52f).outcome,
        )
    }

    @Test
    fun invalidScoresNeverProduceAnAcceptanceOrDiagnosticScore() {
        val result = MiniLmRouteScoring.decide(Float.NaN, 0.1f, 0.2f)

        assertEquals(MiniLmRouteOutcome.INFERENCE_FAILED, result.outcome)
        assertEquals("MiniLM route: latest-meal inference failed", result.diagnostic("read_latest_meal"))
        assertFalse(
            MiniLmRouteDecision(MiniLmRouteOutcome.MARGIN_REJECTED, Float.NaN, 0.5f, 0.4f)
                .diagnostic("read_latest_meal").contains("NaN"),
        )
    }

    @Test
    fun rejectionDiagnosticContainsOnlyFixedRouteAndScores() {
        val result = MiniLmRouteScoring.decide(0.61f, 0.56f, 0.40f)
        val diagnostic = result.diagnostic("read_latest_meal")

        assertTrue(diagnostic.startsWith("MiniLM route: latest-meal margin below threshold"))
        assertTrue(diagnostic.contains("positive=0.610"))
        assertFalse(diagnostic.contains("Could you tell me"))
    }
}
