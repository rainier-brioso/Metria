package app.metria

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPhaseEnergyTest {
    private fun rejected(block: () -> Any?) {
        assertTrue("Expected phase energy input to be rejected", runCatching(block).isFailure)
    }

    private fun estimate(
        age: Int = 64,
        heightCm: Double = 150.0,
        weightKg: Double = 56.25,
        activity: DailyActivity = DailyActivity.ACTIVE,
    ): MaintenanceEstimate = MaintenanceCalculator.calculate(
        GoalProfile(age, heightCm, weightKg, EnergyEquation.MALE, activity),
    )

    private fun loss(energy: PhaseEnergySuggestion = PhaseEnergyCalculator.calculate(estimate(), 2000.0, GoalIntent.LOSE, GoalTraining.GENERAL)): GoalPhase =
        GoalPhase(
            intent = GoalIntent.LOSE,
            targets = NutritionTargets(caloriesKcal = energy.caloriesKcal),
            referenceWeightKg = energy.estimate.profile.weightKg,
            phaseEnergySuggestion = energy,
        )

    @Test fun lossUsesSelectedEndpointAndDoesNotAddTrainingCalories() {
        val reviewed = estimate()
        assertEquals(2000.0, reviewed.minimumKcal, 0.0)
        val general = PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL)
        val exercise = PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.LOSE, GoalTraining.EXERCISE)
        val strength = PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.LOSE, GoalTraining.STRENGTH)
        assertEquals(1800.0, general.rawCaloriesKcal, 0.0)
        assertEquals(1800.0, general.caloriesKcal, 0.0)
        assertEquals(general, exercise.copy(training = GoalTraining.GENERAL))
        assertEquals(general, strength.copy(training = GoalTraining.GENERAL))
        assertEquals(PhaseEnergyCalculator.method, general.method)
        assertEquals(PhaseEnergyCalculator.roundingVersion, general.roundingVersion)
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal + 1.0, GoalIntent.LOSE, GoalTraining.GENERAL) }
    }

    @Test fun gainRequiresStrengthAndUsesFivePercentSurplus() {
        val reviewed = estimate(age = 35, heightCm = 200.0, weightKg = 74.0, activity = DailyActivity.LIGHT)
        val proposal = PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH)
        assertEquals(reviewed.minimumKcal * 1.05, proposal.rawCaloriesKcal, 1e-12)
        assertEquals(kotlin.math.floor(proposal.rawCaloriesKcal + 0.5), proposal.caloriesKcal, 0.0)
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.GAIN, GoalTraining.GENERAL) }
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.GAIN, GoalTraining.MANUAL) }
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.GAIN, null) }
    }

    @Test fun bmiBoundariesAreInclusiveAtLowAndExclusiveAtHigh() {
        val lossAt25 = estimate(age = 35, heightCm = 160.0, weightKg = 64.0, activity = DailyActivity.LIGHT)
        assertTrue(PhaseEnergyCalculator.calculate(lossAt25, lossAt25.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL).caloriesKcal >= 1500.0)
        val justBelowLoss25 = estimate(age = 35, heightCm = 160.0, weightKg = 63.999999, activity = DailyActivity.LIGHT)
        rejected { PhaseEnergyCalculator.calculate(justBelowLoss25, justBelowLoss25.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL) }
        val justBelowLoss40 = estimate(age = 35, heightCm = 160.0, weightKg = 102.399999, activity = DailyActivity.LIGHT)
        assertTrue(PhaseEnergyCalculator.calculate(justBelowLoss40, justBelowLoss40.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL).caloriesKcal >= 1500.0)
        val lossAt40 = estimate(age = 35, heightCm = 160.0, weightKg = 102.4, activity = DailyActivity.LIGHT)
        rejected { PhaseEnergyCalculator.calculate(lossAt40, lossAt40.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL) }

        val gainAt185 = estimate(age = 35, heightCm = 200.0, weightKg = 74.0, activity = DailyActivity.LIGHT)
        assertTrue(PhaseEnergyCalculator.calculate(gainAt185, gainAt185.minimumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH).caloriesKcal >= 1500.0)
        val justBelowGain185 = estimate(age = 35, heightCm = 200.0, weightKg = 73.999999, activity = DailyActivity.LIGHT)
        rejected { PhaseEnergyCalculator.calculate(justBelowGain185, justBelowGain185.minimumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH) }
        val gainAt30 = estimate(age = 35, heightCm = 200.0, weightKg = 120.0, activity = DailyActivity.LIGHT)
        rejected { PhaseEnergyCalculator.calculate(gainAt30, gainAt30.minimumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH) }
    }

    @Test fun vigorousUnsupportedTrainingAndNonPhaseDirectionsRemainManual() {
        val vigorous = estimate(activity = DailyActivity.VIGOROUS)
        rejected { PhaseEnergyCalculator.calculate(vigorous, vigorous.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL) }
        val reviewed = estimate()
        listOf(null, GoalTraining.MANUAL, GoalTraining.DEMANDING).forEach { training ->
            rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.LOSE, training) }
        }
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.MAINTAIN, GoalTraining.GENERAL) }
        rejected { PhaseEnergyCalculator.calculate(reviewed, reviewed.minimumKcal, GoalIntent.TRACK, GoalTraining.GENERAL) }
    }

    @Test fun rawFloorDeficitAndStorageMaximumRejectWithoutClamping() {
        val floorEstimate = estimate(age = 64, heightCm = 140.0, weightKg = 49.0, activity = DailyActivity.LIGHT)
        assertEquals(1450.0, floorEstimate.minimumKcal, 0.0)
        val gainAtFloor = PhaseEnergyCalculator.calculate(floorEstimate, floorEstimate.minimumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH)
        assertEquals(1522.5, gainAtFloor.rawCaloriesKcal, 0.0)
        assertEquals(1523.0, gainAtFloor.caloriesKcal, 0.0)
        rejected { PhaseEnergyCalculator.calculate(floorEstimate, floorEstimate.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL) }

        val largeDeficit = estimate(age = 19, heightCm = 200.0, weightKg = 150.0, activity = DailyActivity.ACTIVE)
        assertTrue(largeDeficit.maximumKcal * 0.10 > 500.0)
        rejected { PhaseEnergyCalculator.calculate(largeDeficit, largeDeficit.maximumKcal, GoalIntent.LOSE, GoalTraining.STRENGTH) }

        val maxEnergy = estimate(age = 19, heightCm = 230.0, weightKg = 150.0, activity = DailyActivity.ACTIVE)
        assertTrue(PhaseEnergyCalculator.calculate(maxEnergy, maxEnergy.maximumKcal, GoalIntent.GAIN, GoalTraining.STRENGTH).caloriesKcal <= 10_000.0)
    }

    @Test fun freshSuitabilityCoordinatorRequiresEstimateAndEndpoint() {
        val reviewed = estimate()
        assertTrue(PhaseEnergyInput.propose(reviewed, reviewed.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL, false).isFailure)
        assertTrue(PhaseEnergyInput.propose(null, reviewed.minimumKcal, GoalIntent.LOSE, GoalTraining.GENERAL, true).isFailure)
        assertTrue(PhaseEnergyInput.propose(reviewed, null, GoalIntent.LOSE, GoalTraining.GENERAL, true).isFailure)
        assertEquals(1800.0, PhaseEnergyInput.propose(reviewed, 2000.0, GoalIntent.LOSE, GoalTraining.GENERAL, true).getOrThrow().caloriesKcal, 0.0)
    }

    @Test fun sourceValidationBindsIntentWeightAndCurrentCalories() {
        val phase = loss()
        GoalPolicy.validate(phase)
        rejected { GoalPolicy.validate(phase.copy(intent = GoalIntent.GAIN)) }
        rejected { GoalPolicy.validate(phase.copy(referenceWeightKg = 57.0)) }
        rejected { GoalPolicy.validate(phase.copy(targets = phase.targets!!.copy(caloriesKcal = 1810.0))) }
        rejected { GoalPolicy.validate(phase.copy(acceptedEstimate = phase.phaseEnergySuggestion!!.estimate)) }
        rejected { GoalPolicy.validate(phase.copy(phaseEnergySuggestion = phase.phaseEnergySuggestion!!.copy(rawCaloriesKcal = 1799.0))) }
    }

    @Test fun profilePresenceAndNewPhasePrefillRespectEnergyProvenance() {
        val phase = loss()
        assertTrue(GoalPolicy.hasProfile(GoalSetup(phase)))
        assertEquals(listOf("", "", "", ""), GoalSetupInput.initialReferences(phase.targets, phase, newPhase = true))
        assertEquals(listOf("1800.0", "", "", ""), GoalSetupInput.initialReferences(phase.targets, phase))
        assertFalse(GoalPolicy.hasProfile(null))
    }

    @Test fun calorieEditsClearEnergyAndDependentMacrosButMacroEditsDoNot() {
        val energy = PhaseEnergyCalculator.calculate(estimate(), 2000.0, GoalIntent.LOSE, GoalTraining.GENERAL)
        val macro = MacroCalculator.calculate(64, 56.25, energy.caloriesKcal, GoalTraining.GENERAL).copy(fatDerived = false)
        val targets = NutritionTargets(energy.caloriesKcal, macro.proteinGrams, macro.carbohydrateGrams, 95.0)
        val phase = loss(energy).copy(
            targets = targets,
            macroSuggestion = macro,
        )
        GoalPolicy.validate(phase)
        val snapshot = GoalSnapshot(targets, GoalSetup(phase))
        val macroOnly = GoalPolicy.manualTargets(snapshot, targets.copy(proteinGrams = 110.0), manuallyEditedMacros = setOf(GoalMacroField.PROTEIN))
        assertEquals(energy, macroOnly.setup!!.current.phaseEnergySuggestion)
        assertFalse(macroOnly.setup!!.current.macroSuggestion!!.proteinDerived)

        val typedSame = GoalPolicy.manualTargets(snapshot, targets, calorieWasEdited = true)
        assertNull(typedSame.setup!!.current.phaseEnergySuggestion)
        assertNull(typedSame.setup!!.current.macroSuggestion)
        assertNull(typedSame.targets!!.proteinGrams)
        assertNull(typedSame.targets!!.carbohydrateGrams)
        assertEquals(95.0, typedSame.targets!!.fatGrams!!, 0.0)
    }

    @Test fun draftInvalidationAndDirectionEditorKeepOnlyManualValues() {
        val phase = loss()
        val energy = requireNotNull(phase.phaseEnergySuggestion)
        val mixedMacro = MacroCalculator.calculate(64, 56.25, energy.caloriesKcal, GoalTraining.GENERAL).copy(fatDerived = false)
        val invalidated = GoalSetupDraftPolicy.invalidatePhaseEnergy(listOf("1800", "100", "200", "95"), energy, mixedMacro)
        assertEquals(listOf("", "", "", "95"), invalidated.references)
        assertNull(invalidated.phaseEnergySuggestion)
        assertNull(invalidated.macroSuggestion)
        val edited = GoalSetupDraftPolicy.editPhaseCalories(listOf("1800", "100", "200", "95"), energy, mixedMacro, "1800")
        assertEquals(listOf("1800", "", "", "95"), edited.references)
        assertNull(edited.phaseEnergySuggestion)
        assertTrue(GoalWeightInput.parse(phase, GoalIntent.GAIN, "", "", "").isFailure)
        assertTrue(GoalWeightInput.parse(phase.copy(phaseEnergySuggestion = null), GoalIntent.GAIN, "", "", "").isSuccess)
    }

    @Test fun usePhaseEnergyHandlesFirstUseBudgetChangeAndSameBudget() {
        val proposal = PhaseEnergyCalculator.calculate(estimate(), 2000.0, GoalIntent.LOSE, GoalTraining.GENERAL)
        val priorMacro = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL).copy(fatDerived = false)
        val firstUse = GoalSetupDraftPolicy.usePhaseEnergy(
            listOf("2400", "120", "300", "95"), proposal, priorMacro,
        )
        assertEquals(listOf("1800.0", "", "", "95"), firstUse.references)
        assertEquals(proposal, firstUse.phaseEnergySuggestion)
        assertNull(firstUse.macroSuggestion)

        val sameBudgetMacro = MacroCalculator.calculate(64, 56.25, proposal.caloriesKcal, GoalTraining.GENERAL)
            .copy(fatDerived = false)
        val sameBudget = GoalSetupDraftPolicy.usePhaseEnergy(
            listOf("1800", "90", "225", "95"), proposal, sameBudgetMacro,
        )
        assertEquals(listOf("1800.0", "90", "225", "95"), sameBudget.references)
        assertEquals(sameBudgetMacro, sameBudget.macroSuggestion)
    }

    @Test fun codecReadsLegacyRepresentationsAndWritesV3ForEnergyHistory() {
        val legacy = GoalSetup(GoalPhase(intent = GoalIntent.TRACK, targets = null))
        assertEquals(1, GoalSetupCodec.encode(legacy).getInt("version"))
        val macroSource = MacroCalculator.calculate(35, 75.0, 2400.0, GoalTraining.GENERAL)
        val macroPhase = GoalPhase(
            intent = GoalIntent.MAINTAIN,
            targets = NutritionTargets(2400.0, macroSource.proteinGrams, macroSource.carbohydrateGrams, macroSource.fatGrams),
            macroSuggestion = macroSource,
        )
        assertEquals(2, GoalSetupCodec.encode(GoalSetup(macroPhase)).getInt("version"))

        val energy = loss()
        val setup = GoalSetup(energy, listOf(macroPhase))
        val encoded = GoalSetupCodec.encode(setup)
        assertEquals(3, encoded.getInt("version"))
        assertEquals(setup, GoalSetupCodec.decode(encoded, energy.targets))
        val archive = JSONObject().put("version", 2).put("goalSetup", encoded)
        assertEquals(setup, GoalSetupCodec.fromPayload(archive, energy.targets))
        assertNull(GoalSetupCodec.decode(GoalSetupCodec.encode(legacy), null).current.phaseEnergySuggestion)
    }

    @Test fun encryptedArchiveV3RoundTripsCurrentEnergyWithMixedManualMacroSources() {
        val energy = PhaseEnergyCalculator.calculate(estimate(), 2000.0, GoalIntent.LOSE, GoalTraining.GENERAL)
        val macro = MacroCalculator.calculate(64, 56.25, energy.caloriesKcal, GoalTraining.GENERAL)
            .copy(proteinDerived = false, fatDerived = false)
        val targets = NutritionTargets(energy.caloriesKcal, 115.0, macro.carbohydrateGrams, 95.0)
        val current = loss(energy).copy(targets = targets, macroSuggestion = macro)
        GoalPolicy.validate(current)
        val setup = GoalSetup(current)
        val rootPayload = JSONObject()
            .put("version", 2)
            .put("nutritionTargets", GoalSetupCodec.encodeTargets(targets))
            .put("goalSetup", GoalSetupCodec.encode(setup))
        val passphrase = "phase energy archive fixture"
        val archive = EncryptedArchive.create(rootPayload, passphrase.toCharArray())
        val openedPayload = EncryptedArchive.open(archive, passphrase.toCharArray())

        val restored = requireNotNull(GoalSetupCodec.fromPayload(openedPayload, targets))
        assertEquals(3, openedPayload.getJSONObject("goalSetup").getInt("version"))
        assertEquals(setup, restored)
        assertFalse(restored.current.macroSuggestion!!.proteinDerived)
        assertTrue(restored.current.macroSuggestion!!.carbohydrateDerived)
        assertFalse(restored.current.macroSuggestion!!.fatDerived)
    }

    @Test fun archivePayloadV3RetainsEnergySourceOnlyInHistory() {
        val historicalEnergy = loss()
        val currentLegacy = GoalPhase(
            intent = GoalIntent.MAINTAIN,
            targets = NutritionTargets(caloriesKcal = 2100.0),
        )
        val setup = GoalSetup(currentLegacy, listOf(historicalEnergy))
        val targets = requireNotNull(currentLegacy.targets)
        val payload = JSONObject()
            .put("version", 2)
            .put("nutritionTargets", GoalSetupCodec.encodeTargets(targets))
            .put("goalSetup", GoalSetupCodec.encode(setup))

        val restored = requireNotNull(GoalSetupCodec.fromPayload(payload, targets))
        assertEquals(3, payload.getJSONObject("goalSetup").getInt("version"))
        assertEquals(setup, restored)
        assertNull(restored.current.phaseEnergySuggestion)
        assertEquals(historicalEnergy.phaseEnergySuggestion, restored.history.single().phaseEnergySuggestion)
        assertTrue(GoalPolicy.hasProfile(restored))
    }

    @Test fun v3CodecRejectsMalformedEnergyFieldsAndTampering() {
        val phase = loss()
        fun encoded() = GoalSetupCodec.encode(GoalSetup(phase))
        rejected { GoalSetupCodec.decode(encoded().put("version", 4), phase.targets) }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("phaseEnergySuggestion").put("training", "FUTURE") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("phaseEnergySuggestion").put("rawCaloriesKcal", "1800") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("phaseEnergySuggestion").put("future", 1) }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("phaseEnergySuggestion").put("roundingVersion", "future") }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected {
            encoded().also { it.getJSONObject("current").getJSONObject("phaseEnergySuggestion").put("maintenanceKcal", 2100.0) }
                .let { GoalSetupCodec.decode(it, phase.targets) }
        }
        rejected { GoalSetupCodec.decode(encoded(), phase.targets!!.copy(caloriesKcal = 2000.0)) }
    }

    @Test fun atomicCommitRollbackRestoresEnergySource() {
        val phase = loss()
        val previous = GoalSnapshot(null, null)
        val next = GoalPolicy.confirm(previous, phase, newPhase = false)
        var live = previous
        rejected { GoalPolicy.commit(previous, next, { live = it }) { error("fixture persistence failure") } }
        assertEquals(previous, live)
        val success = GoalPolicy.commit(previous, next, { live = it }) {}
        assertEquals(phase.phaseEnergySuggestion, live.setup!!.current.phaseEnergySuggestion)
        assertEquals(Unit, success)
    }
}
