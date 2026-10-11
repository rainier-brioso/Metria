package app.metria

import org.junit.Assert.*
import org.junit.Test

class GoalWeightInputTest {
    private fun phase(): GoalPhase {
        val estimate = MaintenanceCalculator.calculate(GoalProfile(30, 180.0, 80.0, EnergyEquation.MALE, DailyActivity.LIGHT))
        return GoalPhase(intent = GoalIntent.MAINTAIN, targets = NutritionTargets(caloriesKcal = estimate.minimumKcal), referenceWeightKg = 80.0, acceptedEstimate = estimate)
    }
    @Test fun `focused edit preserves confirmed calorie source baseline and phase identity`() {
        val original = phase()
        val edited = GoalWeightInput.parse(original, GoalIntent.LOSE, "75,5", "74", "77").getOrThrow()
        assertEquals(75.5, edited.targetWeightKg!!, 0.0)
        assertEquals(original.id, edited.id)
        assertEquals(original.confirmedAt, edited.confirmedAt)
        assertEquals(original.referenceWeightKg, edited.referenceWeightKg)
        assertEquals(original.targets, edited.targets)
        assertEquals(original.acceptedEstimate, edited.acceptedEstimate)
    }
    @Test fun `empty weight goal stays optional and malformed ranges do not alter saved phase`() {
        val original = phase().copy(targetWeightKg = 75.0)
        assertNull(GoalWeightInput.parse(original, original.intent, "", "", "").getOrThrow().targetWeightKg)
        listOf(listOf("NaN", "", ""), listOf("Infinity", "", ""), listOf("75", "74", ""), listOf("75", "77", "74"), listOf("75", "76", "77"), listOf("501", "", "")).forEach { values ->
            assertTrue(GoalWeightInput.parse(original, original.intent, values[0], values[1], values[2]).isFailure)
        }
        assertEquals(75.0, original.targetWeightKg!!, 0.0)
    }
}
