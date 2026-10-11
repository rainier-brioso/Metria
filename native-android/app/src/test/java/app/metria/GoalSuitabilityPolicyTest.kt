package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSuitabilityPolicyTest {
    @Test fun automaticProposalsRequireBothIndependentSuitabilityAnswers() {
        assertFalse(GoalSuitabilityPolicy.permitsAutomaticProposal(false, PregnancyBreastfeedingApplicability.DOES_NOT_APPLY))
        assertFalse(GoalSuitabilityPolicy.permitsAutomaticProposal(true, PregnancyBreastfeedingApplicability.UNANSWERED))
        assertFalse(GoalSuitabilityPolicy.permitsAutomaticProposal(true, PregnancyBreastfeedingApplicability.APPLIES))
        assertTrue(GoalSuitabilityPolicy.permitsAutomaticProposal(true, PregnancyBreastfeedingApplicability.DOES_NOT_APPLY))
        assertTrue(GoalSuitabilityPolicy.shouldUseCareTeamReferences(PregnancyBreastfeedingApplicability.APPLIES))
        assertTrue(GoalSuitabilityPolicy.shouldUseCareTeamReferences(PregnancyBreastfeedingApplicability.DOES_NOT_APPLY, generalSupportNeeded = true))
        assertFalse(GoalSuitabilityPolicy.shouldUseCareTeamReferences(PregnancyBreastfeedingApplicability.UNANSWERED))
    }

    @Test fun careTeamRouteClearsDerivedReferencesButPreservesMemberValues() {
        val macroSource = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
            .copy(proteinDerived = false, fatDerived = true)
        val draft = GoalSuitabilityPolicy.useCareTeamReferences(
            references = listOf("2160", "130", "300", "80"),
            caloriesDerived = true,
            macroSource = macroSource,
        )
        assertEquals(listOf("", "130", "", ""), draft.references)
        assertNull(draft.macroSuggestion)
    }

    @Test fun manualCalorieAndMacroReferencesRemainWhenNoDerivedSourceExists() {
        val draft = GoalSuitabilityPolicy.useCareTeamReferences(
            references = listOf("2100", "110", "200", "75"),
            caloriesDerived = false,
            macroSource = null,
        )
        assertEquals(listOf("2100", "110", "200", "75"), draft.references)
        assertNull(draft.macroSuggestion)
    }
}
