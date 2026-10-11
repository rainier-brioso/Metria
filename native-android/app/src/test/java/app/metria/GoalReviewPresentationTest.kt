package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalReviewPresentationTest {
    @Test fun dailyReferenceStatusCountsOnlySuppliedValues() {
        assertEquals(GoalReferenceCompleteness.NOT_SET, GoalReviewPresentation.dailyReferences(null).completeness)
        val partial = GoalReviewPresentation.dailyReferences(NutritionTargets(caloriesKcal = 2000.0, fatGrams = 70.0))
        assertEquals(GoalReferenceCompleteness.PARTLY_SET, partial.completeness)
        assertEquals(2, partial.suppliedCount)
        assertEquals(listOf("Protein", "Carbohydrates"), partial.missingLabels)
        assertEquals(GoalReferenceCompleteness.SET, GoalReviewPresentation.dailyReferences(NutritionTargets(2000.0, 100.0, 250.0, 70.0)).completeness)
        assertEquals(
            listOf("Calories: Not set", "Protein: Not set", "Carbohydrates: Not set", "Fat: Not set"),
            GoalReviewPresentation.dailyReferenceValues(listOf("", "", "", "")),
        )
    }

    @Test fun macroSourcesUseMacroSlotsAndOmitMissingFields() {
        val source = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
        assertEquals(
            listOf("Protein: suggested", "Fat: member-entered"),
            GoalReviewPresentation.macroSources(listOf("2400", "120", "", "70"), source.copy(proteinDerived = true, fatDerived = false)),
        )
        assertEquals(listOf("No macro values set"), GoalReviewPresentation.macroSources(listOf("2400", "", "", ""), source))
    }

    @Test fun phaseEnergyReviewDistinguishesMaintenanceSelectionFromAppliedProposal() {
        val estimate = MaintenanceCalculator.calculate(GoalProfile(35, 170.0, 75.0, EnergyEquation.MALE, DailyActivity.LIGHT))
        val endpoint = estimate.minimumKcal
        val maintenancePhase = GoalPhase(
            intent = GoalIntent.LOSE,
            targets = NutritionTargets(caloriesKcal = endpoint),
            referenceWeightKg = estimate.profile.weightKg,
            acceptedEstimate = estimate,
        )
        val pending = GoalReviewPresentation.phaseCalories(maintenancePhase).orEmpty()
        assertTrue(pending.contains("Maintenance selected"))
        assertTrue(pending.contains("Phase adjustment not chosen"))
        assertTrue(GoalReviewPresentation.phaseCaloriesSummary(GoalIntent.LOSE, endpoint, true, null).orEmpty().contains("adjustment not chosen"))
        val proposal = PhaseEnergyCalculator.calculate(estimate, endpoint, GoalIntent.LOSE, GoalTraining.GENERAL)
        val applied = maintenancePhase.copy(
            targets = NutritionTargets(caloriesKcal = proposal.caloriesKcal),
            acceptedEstimate = null,
            phaseEnergySuggestion = proposal,
        )
        assertTrue(GoalReviewPresentation.phaseCalories(applied).orEmpty().contains("Phase calorie proposal used"))
        assertTrue(GoalReviewPresentation.phaseCaloriesSummary(GoalIntent.LOSE, proposal.caloriesKcal, false, proposal).orEmpty().contains("Adjustment applied"))
        assertNull(GoalReviewPresentation.phaseCalories(maintenancePhase.copy(intent = GoalIntent.MAINTAIN)))
    }

    @Test fun allMissingReferencesRemainOptionalAndExplainAvailableActions() {
        val phase = GoalPhase(intent = GoalIntent.LOSE, targets = null)
        val guidance = GoalReviewPresentation.finalReviewGuidance(phase)
        assertTrue(guidance.contains("Daily references are optional"))
        assertTrue(guidance.contains("choose maintenance"))
        assertTrue(guidance.contains("Use a phase calorie proposal"))
        assertTrue(guidance.contains("Use macro suggestions"))
        assertTrue(guidance.contains("enter values manually"))
        assertTrue(guidance.contains("continue with no daily references"))
    }
}
