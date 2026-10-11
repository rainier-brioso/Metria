package app.metria

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Instant

class GoalSetupTest {
    private val profile = GoalProfile(30, 180.0, 80.0, EnergyEquation.MALE, DailyActivity.LIGHT)
    private fun estimatedPhase(): GoalPhase {
        val estimate = MaintenanceCalculator.calculate(profile)
        return GoalPhase(intent = GoalIntent.MAINTAIN, confirmedAt = Instant.parse("2020-10-06T12:00:00Z"), targets = NutritionTargets(caloriesKcal = estimate.minimumKcal), referenceWeightKg = profile.weightKg, acceptedEstimate = estimate)
    }
    private fun rejected(block: () -> Unit) = assertTrue(runCatching(block).isFailure)

    @Test fun `male and female fixtures use different explicit coefficients`() {
        val male = MaintenanceCalculator.calculate(profile)
        assertEquals(2450.0, male.minimumKcal, 0.0)
        assertEquals(3050.0, male.maximumKcal, 0.0)
        val female = MaintenanceCalculator.calculate(profile.copy(equation = EnergyEquation.FEMALE))
        assertEquals(2250.0, female.minimumKcal, 0.0)
        assertEquals(2750.0, female.maximumKcal, 0.0)
    }
    @Test fun `category intervals include both ends and round outward`() {
        val expected = listOf(2450.0 to 3050.0, 3000.0 to 3550.0, 3550.0 to 4300.0)
        DailyActivity.entries.forEachIndexed { index, activity ->
            val estimate = MaintenanceCalculator.calculate(profile.copy(activity = activity))
            assertEquals(expected[index].first, estimate.minimumKcal, 0.0)
            assertEquals(expected[index].second, estimate.maximumKcal, 0.0)
        }
    }
    @Test fun `inch and pound conversion matches metric fixture before calculation`() {
        val converted = MaintenanceCalculator.profile("30", (180 / 2.54).toString(), "in", (80 / 0.45359237).toString(), "lb", EnergyEquation.MALE, DailyActivity.LIGHT)
        assertEquals(profile.heightCm, converted.heightCm, 0.00001)
        assertEquals(profile.weightKg, converted.weightKg, 0.00001)
        assertEquals(MaintenanceCalculator.calculate(profile).minimumKcal, MaintenanceCalculator.calculate(converted).minimumKcal, 0.0)
    }
    @Test fun `missing equation activity and malformed inputs never default`() {
        rejected { MaintenanceCalculator.profile("30", "180", "cm", "80", "kg", null, DailyActivity.LIGHT) }
        rejected { MaintenanceCalculator.profile("30", "180", "cm", "80", "kg", EnergyEquation.MALE, null) }
        rejected { MaintenanceCalculator.profile("30.5", "180", "cm", "80", "kg", EnergyEquation.MALE, DailyActivity.LIGHT) }
        rejected { MaintenanceCalculator.profile("30", "180", "feet", "80", "kg", EnergyEquation.MALE, DailyActivity.LIGHT) }
        rejected { MaintenanceCalculator.profile("30", "NaN", "cm", "80", "kg", EnergyEquation.MALE, DailyActivity.LIGHT) }
    }
    @Test fun `technical bounds are inclusive and unsupported profiles are rejected`() {
        MaintenanceCalculator.calculate(profile.copy(age = 19, heightCm = 120.0, weightKg = 35.0))
        MaintenanceCalculator.calculate(profile.copy(age = 64, heightCm = 230.0, weightKg = 250.0))
        listOf(profile.copy(age = 18), profile.copy(age = 65), profile.copy(heightCm = 119.99), profile.copy(heightCm = 230.01), profile.copy(weightKg = 34.99), profile.copy(weightKg = 250.01), profile.copy(weightKg = Double.POSITIVE_INFINITY), profile.copy(heightCm = Double.NaN)).forEach { rejected { MaintenanceCalculator.calculate(it) } }
    }
    @Test fun `intent alone does not create deficits macros or weight targets`() {
        GoalIntent.entries.forEach { intent ->
            val phase = GoalPhase(intent = intent, targets = null)
            GoalPolicy.validate(phase)
            assertNull(phase.targets)
            assertNull(phase.targetWeightKg)
            assertNull(phase.acceptedEstimate)
        }
    }
    @Test fun `member weight range must be complete ordered and contain chosen target`() {
        val phase = GoalPhase(intent = GoalIntent.MAINTAIN, targets = null)
        rejected { GoalPolicy.validate(phase.copy(rangeMinimumKg = 80.0)) }
        rejected { GoalPolicy.validate(phase.copy(rangeMinimumKg = 85.0, rangeMaximumKg = 80.0)) }
        rejected { GoalPolicy.validate(phase.copy(targetWeightKg = 75.0, rangeMinimumKg = 80.0, rangeMaximumKg = 85.0)) }
        rejected { GoalPolicy.validate(phase.copy(targetWeightKg = Double.NaN)) }
        GoalPolicy.validate(phase.copy(targetWeightKg = 82.0, rangeMinimumKg = 80.0, rangeMaximumKg = 85.0))
    }
    @Test fun `accepted source must match reproducible estimate and selected reference`() {
        val phase = estimatedPhase()
        GoalPolicy.validate(phase)
        rejected { GoalPolicy.validate(phase.copy(acceptedEstimate = phase.acceptedEstimate!!.copy(method = "future"))) }
        rejected { GoalPolicy.validate(phase.copy(acceptedEstimate = phase.acceptedEstimate!!.copy(minimumKcal = 2400.0))) }
        rejected { GoalPolicy.validate(phase.copy(targets = NutritionTargets(caloriesKcal = 2000.0))) }
        rejected { GoalPolicy.validate(phase.copy(referenceWeightKg = 81.0)) }
    }
    @Test fun `manual calorie override loses estimate source while macro edit preserves it`() {
        val phase = estimatedPhase()
        val snapshot = GoalSnapshot(phase.targets, GoalSetup(phase))
        val macroEdit = GoalPolicy.manualTargets(snapshot, phase.targets!!.copy(proteinGrams = 90.0))
        assertEquals(phase.acceptedEstimate, macroEdit.setup!!.current.acceptedEstimate)
        val manualEdit = GoalPolicy.manualTargets(snapshot, phase.targets.copy(caloriesKcal = 2000.0))
        assertNull(manualEdit.setup!!.current.acceptedEstimate)
        assertEquals(2000.0, manualEdit.targets!!.caloriesKcal!!, 0.0)
        assertEquals(phase.id, manualEdit.setup.current.id)
        assertNull(GoalPolicy.manualTargets(snapshot, phase.targets, calorieWasEdited = true).setup!!.current.acceptedEstimate)
        assertNull(GoalPolicy.manualTargets(snapshot, null).setup!!.current.acceptedEstimate)
    }
    @Test fun `replacement wizard never silently copies derived calories into a manual reference`() {
        val estimated = estimatedPhase().copy(targets = estimatedPhase().targets!!.copy(proteinGrams = 90.0))
        val initial = GoalSetupInput.initialReferences(estimated.targets, estimated)
        assertEquals("", initial[0])
        assertEquals("90.0", initial[1])
        assertNull(NutritionTargetInput.parse(initial).getOrThrow()!!.caloriesKcal)
        val manual = estimated.copy(acceptedEstimate = null)
        assertEquals("2450.0", GoalSetupInput.initialReferences(manual.targets, manual)[0])
        assertTrue(GoalSetupInput.initialReferences(null, null).all(String::isEmpty))
    }
    @Test fun `edit keeps identity and history new phase alone archives current bounded to twelve`() {
        var snapshot = GoalPolicy.confirm(GoalSnapshot(null, null), GoalPhase(intent = GoalIntent.TRACK, targets = null), false)
        val first = snapshot.setup!!.current
        snapshot = GoalPolicy.confirm(snapshot, first.copy(intent = GoalIntent.MAINTAIN), false)
        assertEquals(first.id, snapshot.setup!!.current.id)
        assertTrue(snapshot.setup.history.isEmpty())
        rejected { GoalPolicy.confirm(snapshot, first.copy(id = java.util.UUID.randomUUID().toString()), false) }
        repeat(14) { snapshot = GoalPolicy.confirm(snapshot, GoalPhase(intent = GoalIntent.LOSE, targets = NutritionTargets(caloriesKcal = 2000.0)), true) }
        assertEquals(12, snapshot.setup!!.history.size)
        assertEquals(snapshot.setup.current.confirmedAt, snapshot.setup.history.last().endedAt)
        assertNull(snapshot.setup.current.endedAt)
        assertEquals(2000.0, snapshot.targets!!.caloriesKcal!!, 0.0)
        rejected { GoalPolicy.confirm(snapshot, snapshot.setup!!.current, true) }
    }
    @Test fun `phase replacement freezes old period at new confirmation and rejects clock reversal`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val old = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = start, targets = null)
        val saved = GoalPolicy.confirm(GoalSnapshot(null, null), old, false)
        val next = GoalPhase(intent = GoalIntent.GAIN, confirmedAt = start.plusSeconds(3600), targets = null)
        val replaced = GoalPolicy.confirm(saved, next, true)
        assertEquals(next.confirmedAt, replaced.setup!!.history.single().endedAt)
        assertNull(replaced.setup.current.endedAt)
        assertEquals(old.id, GoalPolicy.confirm(saved, old.copy(updatedAt = start.plusSeconds(1800)), false).setup!!.current.id)
        rejected { GoalPolicy.confirm(saved, next.copy(confirmedAt = start.minusSeconds(1)), true) }
    }
    @Test fun `known phase intervals may touch but never overlap`() {
        val aStart = Instant.parse("2024-01-01T00:00:00Z")
        val boundary = Instant.parse("2024-02-01T00:00:00Z")
        val current = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = boundary, targets = null)
        val adjacent = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = aStart, updatedAt = aStart, targets = null, endedAt = boundary)
        GoalPolicy.validate(GoalSnapshot(null, GoalSetup(current, listOf(adjacent))))
        val overlapping = adjacent.copy(endedAt = boundary.plusSeconds(1))
        rejected { GoalPolicy.validate(GoalSnapshot(null, GoalSetup(current, listOf(overlapping)))) }
        rejected { GoalPolicy.validate(GoalSnapshot(null, GoalSetup(current.copy(endedAt = boundary), emptyList()))) }
        val older = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = aStart.minusSeconds(86400), targets = null, endedAt = aStart)
        rejected { GoalPolicy.validate(GoalSnapshot(null, GoalSetup(current, listOf(adjacent, older.copy(endedAt = boundary))))) }
    }
    @Test fun `v4 encrypted archive retains phase ends and older formats remain unknown`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-02-01T00:00:00Z")
        val history = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = start, targets = null, endedAt = end)
        val current = GoalPhase(intent = GoalIntent.GAIN, confirmedAt = end, targets = null)
        val setup = GoalSetup(current, listOf(history))
        assertEquals(4, GoalSetupCodec.encode(setup).getInt("version"))
        val root = JSONObject().put("version", 2).put("goalSetup", GoalSetupCodec.encode(setup))
        val passphrase = "goal archive v4 fixture".toCharArray()
        val encrypted = EncryptedArchive.create(root, passphrase)
        val restored = GoalSetupCodec.fromPayload(EncryptedArchive.open(encrypted, passphrase), null)
        assertEquals(setup, restored)
        val v3 = GoalSetupCodec.encode(GoalSetup(current)).also { root ->
            root.put("version", 3)
            root.getJSONObject("current").put("macroSuggestion", JSONObject.NULL).put("phaseEnergySuggestion", JSONObject.NULL)
        }
        assertNull(GoalSetupCodec.decode(v3, null).current.endedAt)
        rejected { GoalSetupCodec.decode(GoalSetupCodec.encode(setup).also { it.getJSONArray("history").getJSONObject(0).put("endedAt", "yesterday") }, null) }
        rejected { GoalSetupCodec.decode(GoalSetupCodec.encode(setup).also { it.getJSONObject("current").remove("endedAt") }, null) }
    }
    @Test fun `atomic persistence failure restores profile goal history and references together`() {
        val old = GoalSnapshot(NutritionTargets(caloriesKcal = 2200.0), null)
        val next = GoalPolicy.confirm(old, estimatedPhase(), false)
        var memory = old
        rejected { GoalPolicy.commit(old, next, { memory = it }) { throw IOException("disk full") } }
        assertEquals(old, memory)
        var written: GoalSnapshot? = null
        GoalPolicy.commit(old, next, { memory = it }) { written = memory }
        assertEquals(next, memory)
        assertEquals(next, written)
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val current = GoalPhase(intent = GoalIntent.TRACK, confirmedAt = start, targets = null)
        val beforeReplacement = GoalPolicy.confirm(GoalSnapshot(null, null), current, false)
        val replacement = GoalPolicy.confirm(beforeReplacement, GoalPhase(intent = GoalIntent.GAIN, confirmedAt = start.plusSeconds(3600), targets = null), true)
        var replacementMemory = beforeReplacement
        rejected { GoalPolicy.commit(beforeReplacement, replacement, { replacementMemory = it }) { throw IOException("disk full") } }
        assertEquals(beforeReplacement, replacementMemory)
        assertNull(replacementMemory.setup!!.current.endedAt)
    }
    @Test fun `encrypted recovery archive round trips current history source and explicit nulls`() {
        val phase = estimatedPhase()
        val setup = GoalSetup(phase, listOf(GoalPhase(intent = GoalIntent.TRACK, targets = null)))
        val root = JSONObject().put("version", 2).put("goalSetup", GoalSetupCodec.encode(setup))
        val archive = EncryptedArchive.create(root, "goal archive fixture".toCharArray())
        val restored = EncryptedArchive.open(archive, "goal archive fixture".toCharArray())
        assertEquals(setup, GoalSetupCodec.fromPayload(restored, phase.targets))
    }
    @Test fun `legacy archive and removed setup restore absence while future versions reject`() {
        assertNull(GoalSetupCodec.fromPayload(JSONObject().put("version", 1), NutritionTargets(caloriesKcal = 2100.0)))
        assertNull(GoalSetupCodec.fromPayload(JSONObject().put("version", 2).put("goalSetup", JSONObject.NULL), null))
        rejected { GoalSetupCodec.fromPayload(JSONObject().put("version", 3), null) }
        rejected { GoalSetupCodec.fromPayload(JSONObject().put("version", "2"), null) }
        val setup = GoalSetup(estimatedPhase())
        rejected { GoalSetupCodec.fromPayload(JSONObject().put("version", 1).put("goalSetup", GoalSetupCodec.encode(setup)), setup.current.targets) }
        rejected { GoalSetupCodec.decode(GoalSetupCodec.encode(setup).put("version", 2), setup.current.targets) }
    }
    @Test fun `strict codec rejects unknown fields coercion missing source and altered provenance`() {
        val setup = GoalSetup(estimatedPhase())
        fun encoded() = GoalSetupCodec.encode(setup)
        rejected { GoalSetupCodec.decode(encoded().put("unknown", true), setup.current.targets) }
        rejected { GoalSetupCodec.decode(encoded().also { it.getJSONObject("current").remove("acceptedEstimate") }, setup.current.targets) }
        rejected { GoalSetupCodec.decode(encoded().also { it.getJSONObject("current").getJSONObject("acceptedEstimate").getJSONObject("profile").put("age", "30") }, setup.current.targets) }
        rejected { GoalSetupCodec.decode(encoded().also { it.getJSONObject("current").getJSONObject("acceptedEstimate").getJSONObject("profile").put("age", 30.5) }, setup.current.targets) }
        rejected { GoalSetupCodec.decode(encoded().also { it.getJSONObject("current").getJSONObject("targets").put("caloriesKcal", "2450") }, setup.current.targets) }
        rejected { GoalSetupCodec.decode(encoded(), NutritionTargets(caloriesKcal = 2100.0)) }
    }
    @Test fun `deletion retains only references and never reuses profile`() {
        val phase = estimatedPhase()
        val previous = GoalSnapshot(phase.targets, GoalSetup(phase))
        var memory = previous
        val deleted = GoalSnapshot(previous.targets, null)
        GoalPolicy.commit(previous, deleted, { memory = it }) {}
        assertEquals(previous.targets, memory.targets)
        assertNull(memory.setup)
    }
    @Test fun `oversized history duplicate IDs and invalid dates reject before writing`() {
        val phase = estimatedPhase()
        rejected { GoalPolicy.validate(GoalSnapshot(phase.targets, GoalSetup(phase, List(13) { GoalPhase(intent = GoalIntent.TRACK, targets = null) }))) }
        rejected { GoalPolicy.validate(GoalSnapshot(phase.targets, GoalSetup(phase, listOf(phase)))) }
        rejected { GoalPolicy.validate(phase.copy(updatedAt = phase.confirmedAt.minusSeconds(1))) }
        rejected { GoalPolicy.validate(phase.copy(confirmedAt = Instant.MAX)) }
    }
}
