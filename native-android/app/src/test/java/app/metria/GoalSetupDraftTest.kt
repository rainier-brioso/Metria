package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class GoalSetupDraftTest {
    private val estimate = MaintenanceCalculator.calculate(
        GoalProfile(35, 170.0, 70.0, EnergyEquation.FEMALE, DailyActivity.LIGHT),
    )
    private val blank = GoalSetupDraftPolicy.References(listOf("", "12", "", "8"), null)

    @Test fun selectingAnEndpointRecordsOnlyThatDisplayedEndpoint() {
        val chosen = GoalSetupDraftPolicy.chooseEstimate(blank, estimate, estimate.maximumKcal)
        assertEquals(estimate.maximumKcal.toString(), chosen.values[0])
        assertEquals(estimate, chosen.acceptedEstimate)
    }

    @Test fun changingEstimateInputsClearsDerivedCaloriesButKeepsMacros() {
        val chosen = GoalSetupDraftPolicy.chooseEstimate(blank, estimate, estimate.minimumKcal)
        val invalidated = GoalSetupDraftPolicy.invalidateEstimate(chosen)
        assertEquals("", invalidated.values[0])
        assertEquals(listOf("", "12", "", "8"), invalidated.values)
        assertNull(invalidated.acceptedEstimate)
    }

    @Test fun invalidProfileInputClearsDerivedCaloriesEvenWhenRecalculationFails() {
        val selectedButNoLongerCalculable = GoalSetupDraftPolicy.References(listOf("2200", "12", "", "8"), null)
        val invalidated = GoalSetupDraftPolicy.invalidateEstimate(selectedButNoLongerCalculable, selected = true)
        assertEquals(listOf("", "12", "", "8"), invalidated.values)
        assertNull(invalidated.acceptedEstimate)
    }

    @Test fun manualCalorieEditDropsProvenanceWhileMacroEditKeepsIt() {
        val chosen = GoalSetupDraftPolicy.chooseEstimate(blank, estimate, estimate.minimumKcal)
        val macros = GoalSetupDraftPolicy.editMacro(chosen, 1, "20")
        assertEquals(estimate, macros.acceptedEstimate)
        val manual = GoalSetupDraftPolicy.editCalories(macros, "2000")
        assertNull(manual.acceptedEstimate)
        assertEquals("2000", manual.values[0])
    }

    @Test fun phaseBuildKeepsEditedIdentityAndUsesReviewedWeightForAcceptedEstimate() {
        val confirmed = Instant.parse("2026-01-01T00:00:00Z")
        val edited = GoalPhase(id = "00000000-0000-0000-0000-000000000001", intent = GoalIntent.MAINTAIN, confirmedAt = confirmed, targets = null)
        val draft = GoalSetupDraftPolicy.createPhase(
            id = edited.id, intentName = GoalIntent.GAIN.name, editing = edited,
            references = listOf(estimate.minimumKcal.toString(), "", "", ""),
            startingWeight = "99", targetWeight = "75", minimumWeight = "", maximumWeight = "",
            acceptedEstimate = estimate, now = confirmed.plusSeconds(10),
        )
        assertEquals(edited.id, draft.id)
        assertEquals(confirmed, draft.confirmedAt)
        assertEquals(estimate.profile.weightKg, draft.referenceWeightKg)
    }

    @Test fun invalidNumericTextIsRejectedWithoutChangingTheDraftInput() {
        val inputs = mutableListOf("", "", "", "")
        val originalInputs = inputs.toList()
        val invalid = "not a number"
        val result = runCatching {
            GoalSetupDraftPolicy.createPhase("00000000-0000-0000-0000-000000000002", "MAINTAIN", null, inputs, invalid, "", "", "", null, Instant.EPOCH)
        }
        assertEquals("Check your weight values.", result.exceptionOrNull()?.message)
        assertEquals(originalInputs, inputs)
    }
}
