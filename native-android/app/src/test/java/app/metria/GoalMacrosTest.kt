package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.time.Instant

class GoalMacrosTest {
    private fun rejected(block: () -> Any?) {
        val result = runCatching(block)
        assertTrue("Expected a rejected macro proposal", result.isFailure)
    }

    private fun macroPhase(source: MacroSuggestion = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)): GoalPhase =
        GoalPhase(
            intent = GoalIntent.MAINTAIN,
            targets = NutritionTargets(source.caloriesKcal, source.proteinGrams, source.carbohydrateGrams, source.fatGrams),
            macroSuggestion = source,
        )

    @Test fun balancedModeUsesExplicitSplitAndReviewedWeightFloor() {
        val value = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
        assertEquals(MacroCalculator.balancedMethod, value.method)
        assertEquals(120.0, value.rawProteinGrams, 0.0)
        assertEquals(300.0, value.rawCarbohydrateGrams, 0.0)
        assertEquals(80.0, value.rawFatGrams, 0.0)
        assertEquals(120.0, value.proteinGrams, 0.0)
        assertEquals(300.0, value.carbohydrateGrams, 0.0)
        assertEquals(80.0, value.fatGrams, 0.0)
        MacroCalculator.validate(value)
        rejected { MacroCalculator.calculate(35, 70.0, 1000.0, GoalTraining.GENERAL) }
    }

    @Test fun exerciseModesUseTheirOwnProteinCoefficientsAndRemainder() {
        val strength = MacroCalculator.calculate(40, 70.0, 2100.0, GoalTraining.STRENGTH)
        assertEquals(MacroCalculator.exerciseMethod, strength.method)
        assertEquals(112.0, strength.rawProteinGrams, 0.0)
        assertEquals(70.0, strength.rawFatGrams, 0.0)
        assertEquals(255.5, strength.rawCarbohydrateGrams, 1e-12)
        assertEquals(256.0, strength.carbohydrateGrams, 0.0)
        val exercise = MacroCalculator.calculate(40, 70.0, 2100.0, GoalTraining.EXERCISE)
        assertEquals(98.0, exercise.rawProteinGrams, 0.0)
        assertEquals(269.5, exercise.rawCarbohydrateGrams, 1e-12)
        assertEquals(270.0, exercise.carbohydrateGrams, 0.0)
    }

    @Test fun exerciseOfferEnvelopeIncludesBoundariesAndRejectsValuesOutside() {
        val lower = MacroCalculator.calculate(30, 70.0, 1792.0, GoalTraining.STRENGTH)
        val upper = MacroCalculator.calculate(30, 70.0, 4480.0, GoalTraining.STRENGTH)
        assertEquals(45.0, lower.rawCarbohydrateGrams * 4.0 / lower.caloriesKcal * 100.0, 1e-12)
        assertEquals(60.0, upper.rawCarbohydrateGrams * 4.0 / upper.caloriesKcal * 100.0, 1e-12)
        rejected { MacroCalculator.calculate(30, 70.0, 1700.0, GoalTraining.STRENGTH) }
        rejected { MacroCalculator.calculate(30, 70.0, 5000.0, GoalTraining.STRENGTH) }
        rejected { MacroCalculator.calculate(30, 70.0, 2000.0, GoalTraining.DEMANDING) }
        rejected { MacroCalculator.calculate(30, 70.0, 2000.0, GoalTraining.MANUAL) }
    }

    @Test fun positiveHalfUpRoundingAndInputBoundsAreDeterministic() {
        val value = MacroCalculator.calculate(19, 40.0, 700.0, GoalTraining.GENERAL)
        assertEquals(87.5, value.rawCarbohydrateGrams, 0.0)
        assertEquals(88.0, value.carbohydrateGrams, 0.0)
        assertEquals(MacroCalculator.roundingVersion, value.roundingVersion)
        rejected { MacroCalculator.calculate(18, 70.0, 2400.0, GoalTraining.GENERAL) }
        rejected { MacroCalculator.calculate(65, 70.0, 2400.0, GoalTraining.GENERAL) }
        rejected { MacroCalculator.calculate(30, Double.NaN, 2400.0, GoalTraining.GENERAL) }
        rejected { MacroCalculator.calculate(30, 70.0, Double.POSITIVE_INFINITY, GoalTraining.GENERAL) }
        rejected { MacroCalculator.calculate(30, 70.0, 0.0, GoalTraining.GENERAL) }
    }

    @Test fun parserConvertsOnlyExplicitUnitsAndRetainsManualFallback() {
        val parsed = GoalMacroInput.parse("35", "154.3236", "lb", GoalTraining.STRENGTH).getOrThrow()
        assertEquals(35, parsed.age)
        assertEquals(70.0, parsed.weightKg, 0.0001)
        assertEquals(GoalTraining.STRENGTH, parsed.training)
        assertEquals(GoalTraining.MANUAL, GoalMacroInput.parse("35", "70", "kg", GoalTraining.MANUAL).getOrThrow().training)
        assertTrue(GoalMacroInput.parse("", "", "kg", GoalTraining.GENERAL).isFailure)
        assertTrue(GoalMacroInput.parse("35", "70", "stone", GoalTraining.GENERAL).isFailure)
    }

    @Test fun proposalCoordinatorRequiresSuitabilityTrainingAndExplicitCalories() {
        assertTrue(GoalMacroInput.propose("35", "75", "kg", GoalTraining.GENERAL, "2400", suitable = false).isFailure)
        assertTrue(GoalMacroInput.propose("35", "75", "kg", null, "2400", suitable = true).isFailure)
        assertTrue(GoalMacroInput.propose("35", "75", "kg", GoalTraining.GENERAL, "", suitable = true).isFailure)
        assertTrue(GoalMacroInput.propose("35", "75", "kg", GoalTraining.DEMANDING, "2400", suitable = true).isFailure)

        val proposal = GoalMacroInput.propose("35", "75", "kg", GoalTraining.GENERAL, "2400,0", suitable = true).getOrThrow()
        assertEquals(35, proposal.age)
        assertEquals(75.0, proposal.weightKg, 0.0)
        assertEquals(2400.0, proposal.caloriesKcal, 0.0)
        assertEquals(GoalTraining.GENERAL, proposal.training)
    }

    @Test fun perFieldOverridePreservesOtherDerivedFieldsAndInvalidationRetainsManualOverrides() {
        val source = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
        val edited = GoalSetupDraftPolicy.markMacroManual(source, GoalMacroField.PROTEIN)!!
        assertFalse(edited.proteinDerived)
        assertTrue(edited.carbohydrateDerived)
        assertTrue(edited.fatDerived)
        val draft = GoalSetupDraftPolicy.editMacroReference(listOf("120", "300", "80"), source, GoalMacroField.FAT, "95")
        assertEquals(listOf("120", "300", "95"), draft.values)
        assertFalse(draft.source!!.fatDerived)
        val invalidated = GoalSetupDraftPolicy.invalidateMacroReferences(draft.values, draft.source)
        assertEquals(listOf("", "", "95"), invalidated.values)
        assertNull(invalidated.source)
    }

    @Test fun policyPreservesIndependentMacroSourcesAndClearsDerivedValuesOnCalorieEdit() {
        val full = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
        val mixed = full.copy(fatDerived = false)
        val initialPhase = macroPhase(mixed)
        val phaseTargets = requireNotNull(initialPhase.targets).copy(fatGrams = 95.0)
        val phase = initialPhase.copy(targets = phaseTargets)
        GoalPolicy.validate(phase)
        val snapshot = GoalSnapshot(phase.targets, GoalSetup(phase))

        val macroEdit = GoalPolicy.manualTargets(
            snapshot,
            phase.targets!!.copy(proteinGrams = 130.0),
            manuallyEditedMacros = setOf(GoalMacroField.PROTEIN),
        )
        val retained = macroEdit.setup!!.current.macroSuggestion!!
        assertFalse(retained.proteinDerived)
        assertTrue(retained.carbohydrateDerived)
        assertFalse(retained.fatDerived)
        assertEquals(95.0, macroEdit.targets!!.fatGrams!!, 0.0)

        val calorieEdit = GoalPolicy.manualTargets(snapshot, phase.targets.copy(caloriesKcal = 2500.0), calorieWasEdited = true)
        assertNull(calorieEdit.setup!!.current.macroSuggestion)
        assertEquals(95.0, calorieEdit.targets!!.fatGrams!!, 0.0)
        assertNull(calorieEdit.targets!!.proteinGrams)
        assertNull(calorieEdit.targets!!.carbohydrateGrams)

        val simultaneous = GoalPolicy.manualTargets(
            snapshot,
            phase.targets.copy(caloriesKcal = 2500.0, proteinGrams = 135.0),
            calorieWasEdited = true,
            manuallyEditedMacros = setOf(GoalMacroField.PROTEIN),
        )
        assertEquals(135.0, simultaneous.targets!!.proteinGrams!!, 0.0)
        assertNull(simultaneous.targets!!.carbohydrateGrams)
        assertEquals(95.0, simultaneous.targets!!.fatGrams!!, 0.0)
    }

    @Test fun goalPolicyRejectsTamperedSourceAndRequiresCurrentDerivedTargetMatch() {
        val phase = macroPhase()
        GoalPolicy.validate(phase)
        rejected { GoalPolicy.validate(phase.copy(targets = phase.targets!!.copy(proteinGrams = 121.0))) }
        rejected { GoalPolicy.validate(phase.copy(macroSuggestion = phase.macroSuggestion!!.copy(method = "future"))) }
        rejected { GoalPolicy.validate(phase.copy(macroSuggestion = phase.macroSuggestion!!.copy(roundingVersion = "future"))) }
        rejected { GoalPolicy.validate(phase.copy(macroSuggestion = phase.macroSuggestion!!.copy(proteinDerived = false, carbohydrateDerived = false, fatDerived = false))) }
    }

    @Test fun codecWritesVersionOneWithoutMacrosAndVersionTwoWithStrictMacroProvenance() {
        val legacy = GoalSetup(GoalPhase(intent = GoalIntent.TRACK, targets = null))
        assertEquals(1, GoalSetupCodec.encode(legacy).getInt("version"))
        assertEquals(legacy, GoalSetupCodec.decode(GoalSetupCodec.encode(legacy), null))

        val phase = macroPhase()
        val setup = GoalSetup(phase)
        val encoded = GoalSetupCodec.encode(setup)
        assertEquals(2, encoded.getInt("version"))
        assertEquals(setup, GoalSetupCodec.decode(encoded, phase.targets))
        val badMethod = encoded.getJSONObject("current").getJSONObject("macroSuggestion").put("method", "future")
        rejected { GoalSetupCodec.decode(encoded, phase.targets) }
        assertEquals("future", badMethod.getString("method"))
    }

    @Test fun encryptedArchivePayloadRoundTripsMixedMacroSources() {
        val source = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
            .copy(proteinDerived = false, fatDerived = false)
        val initialPhase = macroPhase(source)
        val phaseTargets = requireNotNull(initialPhase.targets).copy(proteinGrams = 135.0, fatGrams = 95.0)
        val phase = initialPhase.copy(targets = phaseTargets)
        val setup = GoalSetup(phase)
        val payload = JSONObject()
            .put("version", 2)
            .put("nutritionTargets", GoalSetupCodec.encodeTargets(phase.targets))
            .put("goalSetup", GoalSetupCodec.encode(setup))
        val restored = GoalSetupCodec.fromPayload(payload, phase.targets)
        assertEquals(setup, restored)
        assertFalse(restored!!.current.macroSuggestion!!.proteinDerived)
        assertTrue(restored.current.macroSuggestion!!.carbohydrateDerived)
        assertFalse(restored.current.macroSuggestion!!.fatDerived)
    }

    @Test fun macroCodecRejectsMalformedProvenanceAndInconsistentTargets() {
        val phase = macroPhase()
        fun encoded() = GoalSetupCodec.encode(GoalSetup(phase))
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("proteinDerived", "true") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("age", 35.5) }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("weightKg", "75") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("unknown", 1) }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("roundingVersion", "future") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("macroSuggestion").put("caloriesKcal", 2500.0) }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            GoalSetupCodec.decode(encoded(), phase.targets!!.copy(proteinGrams = 121.0))
        }
    }

    @Test fun profileSummaryDetectsMacroOnlySourcesInCurrentOrHistory() {
        val phase = macroPhase()
        assertTrue(GoalPolicy.hasProfile(GoalSetup(phase)))
        val historical = phase.copy(macroSuggestion = null)
        assertTrue(GoalPolicy.hasProfile(GoalSetup(GoalPhase(intent = GoalIntent.TRACK, targets = null), listOf(historical.copy(macroSuggestion = phase.macroSuggestion)))) )
        assertFalse(GoalPolicy.hasProfile(GoalSetup(GoalPhase(intent = GoalIntent.TRACK, targets = null))))
        assertFalse(GoalPolicy.hasProfile(null))
    }

    @Test fun newPhaseDoesNotRelabelCopiedDerivedMacroValuesAsManual() {
        val source = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
            .copy(fatDerived = false)
        val phase = macroPhase(source).copy(targets = macroPhase(source).targets!!.copy(fatGrams = 95.0))
        assertEquals(listOf("2400.0", "", "", "95.0"), GoalSetupInput.initialReferences(phase.targets, phase, newPhase = true))
        assertEquals(listOf("2400.0", "120.0", "300.0", "95.0"), GoalSetupInput.initialReferences(phase.targets, phase))
    }

    @Test fun macroSnapshotCommitRollsBackWithTheExistingAtomicPolicy() {
        val phase = macroPhase()
        val old = GoalSnapshot(null, null)
        val next = GoalPolicy.confirm(old, phase, newPhase = false)
        var live = old
        rejected { GoalPolicy.commit(old, next, { live = it }) { error("fixture failure") } }
        assertEquals(old, live)
        val committed = GoalPolicy.confirm(old, phase.copy(confirmedAt = Instant.EPOCH, updatedAt = Instant.EPOCH), newPhase = false)
        assertEquals(phase.macroSuggestion, committed.setup!!.current.macroSuggestion)
    }
}
