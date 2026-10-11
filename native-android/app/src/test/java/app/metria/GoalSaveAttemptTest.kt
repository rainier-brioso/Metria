package app.metria

import java.io.IOException
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class GoalSaveAttemptTest {
    private fun phase(index: Int) = GoalPhase(
        id = UUID(0, index.toLong()).toString(), intent = GoalIntent.TRACK,
        confirmedAt = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index.toLong()),
        targets = null,
    )

    @Test fun doubleTapAndCompletedSaveCannotStartAnotherWrite() {
        val attempt = GoalSaveAttempt(phase(1))
        assertTrue(attempt.begin())
        assertFalse(attempt.begin())
        attempt.finish(true)
        assertFalse(attempt.begin())
    }

    @Test fun failedSaveRetriesTheExactReviewedPhaseAndDates() {
        val reviewed = phase(2)
        val attempt = GoalSaveAttempt(reviewed)
        assertTrue(attempt.begin())
        attempt.finish(false)
        assertTrue(attempt.begin())
        assertSame(reviewed, attempt.phase)
        assertEquals(reviewed.confirmedAt, attempt.phase.confirmedAt)
    }

    @Test fun failedReplacementThenRetryArchivesOnceAndRemovesOnlyOldestAtLimit() {
        val current = phase(20)
        val history = (1..GoalPolicy.historyLimit).map(::phase)
        val original = GoalSnapshot(null, GoalSetup(current, history))
        var memory = original
        val attempt = GoalSaveAttempt(phase(21).copy(targets = NutritionTargets(proteinGrams = 80.0)))

        assertTrue(attempt.begin())
        try {
            val next = GoalPolicy.confirm(memory, attempt.phase, true)
            GoalPolicy.commit(memory, next, { memory = it }) { throw IOException("fixture write failed") }
            fail("Expected write failure")
        } catch (_: IOException) {
            attempt.finish(false)
        }
        assertEquals(original, memory)
        assertTrue(attempt.begin())
        val next = GoalPolicy.confirm(memory, attempt.phase, true)
        GoalPolicy.commit(memory, next, { memory = it }) {}
        attempt.finish(true)

        assertEquals(attempt.phase, memory.setup!!.current)
        assertEquals(history.drop(1) + current.copy(endedAt = attempt.phase.confirmedAt), memory.setup!!.history)
        assertEquals(1, memory.setup!!.history.count { it.id == current.id })
        assertEquals(80.0, memory.targets!!.proteinGrams!!, 0.0)
        assertFalse(attempt.begin())
    }
}
