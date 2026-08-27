package app.metria.ui.screens

import app.metria.CycleContext
import app.metria.NutritionTargets
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySettingsPresentationTest {
    @Test
    fun `goals summary distinguishes absent and configured goals`() {
        assertEquals("No daily goals set", nutritionTargetsSummary(null))
        assertEquals(
            "1900 kcal · 120 g protein",
            nutritionTargetsSummary(NutritionTargets(caloriesKcal = 1900.0, proteinGrams = 120.0)),
        )
    }

    @Test
    fun `cycle summary exposes state without revealing dates`() {
        assertEquals("Off · optional", cycleContextSummary(CycleContext()))
        assertEquals(
            "On · 2 dates saved privately",
            cycleContextSummary(
                CycleContext(
                    enabled = true,
                    periodStarts = listOf(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-07-01")),
                ),
            ),
        )
    }

    @Test
    fun `assistant summary uses member language for runtime states`() {
        assertEquals("Ready · working entirely on this phone", assistantStatusSummary("loaded"))
        assertEquals("Available offline · starts when needed", assistantStatusSummary("ready"))
        assertEquals("Working locally now", assistantStatusSummary("thinking"))
    }

    @Test
    fun `recovery status classifies cancellation and failures`() {
        assertTrue(archiveMessageIsFailure("Archive could not be opened: damaged file"))
        assertTrue(archiveMessageIsFailure("No archive was selected."))
        assertFalse(archiveMessageIsFailure("Encrypted recovery archive saved."))
    }
}
