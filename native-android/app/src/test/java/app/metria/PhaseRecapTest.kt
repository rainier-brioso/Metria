package app.metria

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PhaseRecapTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val start = Instant.parse("2024-03-30T23:00:00Z") // local midnight before the DST-short day
    private val end = Instant.parse("2024-04-02T12:00:00Z") // partial local day at the end
    private val phase = GoalPhase(id = "00000000-0000-0000-0000-000000000001", intent = GoalIntent.MAINTAIN, confirmedAt = start, targets = null, endedAt = end)
    private fun weight(id: String, at: String, kg: Double) = WeightEntry(id = id, kilograms = kg, recordedAt = Instant.parse(at))
    private fun meal(id: String, at: String, knownTime: Boolean, nutrition: MealNutrition? = null) = MealEntry(
        id = id, recordedAt = Instant.parse(at), recordedTimeKnown = knownTime, items = listOf(MealEntryItem("Food", null, null)), assumptions = emptyList(),
        confidence = "low", source = "manual_entry", nutrition = nutrition,
    )

    @Test fun `known interval is start inclusive end exclusive and known weights order by instant then id`() {
        val recap = PhaseRecapCalculator.calculate(phase, false, listOf(
            weight("b", "2024-04-01T10:00:00Z", 81.0),
            weight("z", start.toString(), 80.0),
            weight("a", "2024-04-01T10:00:00Z", 82.0),
            weight("end", end.toString(), 99.0),
        ), emptyList(), zone, end)
        assertEquals(listOf("z", "a", "b"), recap.weights.map { it.id })
        assertEquals(1.0, recap.weightChangeKg!!, 0.0)
    }

    @Test fun `unknown historical end is unavailable and active preview before start is safely unavailable`() {
        val unknown = PhaseRecapCalculator.calculate(phase.copy(endedAt = null), false, listOf(weight("w", start.toString(), 80.0)), emptyList(), zone, end)
        assertFalse(unknown.periodAvailable)
        assertNull(unknown.nutrition)
        val future = PhaseRecapCalculator.calculate(phase.copy(endedAt = null), true, emptyList(), emptyList(), zone, start.minusSeconds(1))
        assertFalse(future.periodAvailable)
    }

    @Test fun `active preview includes captured asOf instant and logging days union records once`() {
        val asOf = Instant.parse("2024-04-01T12:00:00Z")
        val dayWeight = weight("same-day", "2024-04-01T10:00:00Z", 81.0)
        val dayMeal = meal("same-day-meal", "2024-04-01T11:00:00Z", true)
        val excluded = meal("at-asof", asOf.toString(), true)
        val recap = PhaseRecapCalculator.calculate(phase.copy(endedAt = null), true, listOf(dayWeight), listOf(dayMeal, excluded), zone, asOf)
        assertTrue(recap.periodAvailable)
        assertEquals(1, recap.weights.size)
        assertEquals(1, recap.mealsLogged)
        assertEquals(1, recap.loggingDays)
    }

    @Test fun `date-only meal requires whole local day and DST day uses zone boundaries`() {
        val inside = meal("dst", "2024-03-31T12:00:00Z", false)
        val beforeBoundary = meal("before", "2024-03-30T23:00:00Z", false)
        val afterBoundary = meal("after", "2024-04-02T00:00:00Z", false)
        val dstStart = Instant.parse("2024-03-29T23:00:00Z") // local midnight before the 23-hour day
        val recap = PhaseRecapCalculator.calculate(phase.copy(confirmedAt = dstStart), false, emptyList(), listOf(inside, beforeBoundary, afterBoundary), zone, end)
        assertEquals(2, recap.mealsLogged)
        assertEquals(1, recap.boundaryDateOnlyMealsExcluded)
        assertEquals(1, recap.loggingDays)
    }

    @Test fun `date-only meal on partially included start day is excluded`() {
        val partialStart = Instant.parse("2024-03-31T10:00:00Z")
        val dateOnly = meal("start-day", "2024-03-30T23:00:00Z", false)
        val recap = PhaseRecapCalculator.calculate(phase.copy(confirmedAt = partialStart), false, emptyList(), listOf(dateOnly), zone, end)
        assertEquals(0, recap.mealsLogged)
        assertEquals(1, recap.boundaryDateOnlyMealsExcluded)
    }

    @Test fun `nutrition preserves valid zero and excludes partial and missing fields`() {
        val zero = meal("zero", "2024-04-01T10:00:00Z", true, MealNutrition(0.0, 0.0, null, null, estimateCoverage = "full"))
        val partial = meal("partial", "2024-04-01T11:00:00Z", true, MealNutrition(100.0, 20.0, 1.0, 2.0, estimateCoverage = "partial"))
        val noNutrition = meal("unknown", "2024-04-01T12:00:00Z", true)
        val recap = PhaseRecapCalculator.calculate(phase, false, emptyList(), listOf(zero, partial, noNutrition), zone, end)
        val nutrition = recap.nutrition!!
        assertEquals(0.0, nutrition.calories.total!!, 0.0)
        assertEquals(1, nutrition.calories.contributingMeals)
        assertEquals(1, nutrition.calories.partialMealsExcluded)
        assertEquals(1, nutrition.carbohydrate.missingFullCoverageMeals)
        assertNull(nutrition.carbohydrate.total)
        assertEquals(1, nutrition.mealsWithoutNutrition)
    }

    @Test fun `all meals without nutrition remain explicit and empty period has zero records`() {
        val meal = meal("unknown", "2024-04-01T12:00:00Z", true)
        val withMissing = PhaseRecapCalculator.calculate(phase, false, emptyList(), listOf(meal), zone, end)
        assertEquals(1, withMissing.nutrition!!.mealsWithoutNutrition)
        assertNull(withMissing.nutrition.calories.total)
        val empty = PhaseRecapCalculator.calculate(phase, false, emptyList(), emptyList(), zone, end)
        assertEquals(0, empty.mealsLogged)
        assertEquals(0, empty.nutrition!!.mealsWithoutNutrition)
        assertNull(empty.nutrition.calories.total)
    }

    @Test fun `extreme records outside local period are safely ignored`() {
        val recap = PhaseRecapCalculator.calculate(
            phase, false,
            listOf(weight("ancient", Instant.MIN.toString(), 80.0), weight("future", Instant.MAX.toString(), 90.0)),
            listOf(meal("ancient-meal", Instant.MIN.toString(), false), meal("future-meal", Instant.MAX.toString(), true)),
            zone, end,
        )
        assertTrue(recap.weights.isEmpty())
        assertEquals(0, recap.mealsLogged)
    }

    @Test fun `deleting neighboring phase preserves bounds and deleted records leave recap`() {
        val older = phase.copy(id = "00000000-0000-0000-0000-000000000002", confirmedAt = start.minusSeconds(86400), updatedAt = start.minusSeconds(86400), endedAt = start)
        val current = phase.copy(id = "00000000-0000-0000-0000-000000000003", confirmedAt = end, updatedAt = end, endedAt = null)
        val setup = GoalSetup(current, listOf(older, phase))
        val afterPhaseDeletion = setup.copy(history = setup.history.filterNot { it.id == older.id })
        GoalPolicy.validate(GoalSnapshot(null, afterPhaseDeletion))
        assertEquals(phase.endedAt, afterPhaseDeletion.history.single().endedAt)
        val first = weight("first", "2024-04-01T09:00:00Z", 81.0)
        val last = weight("last", "2024-04-02T09:00:00Z", 80.0)
        val savedMeal = meal("saved", "2024-04-01T12:00:00Z", true, MealNutrition(200.0, 10.0, 20.0, 8.0))
        val before = PhaseRecapCalculator.calculate(phase, false, listOf(first, last), listOf(savedMeal), zone, end)
        assertEquals(-1.0, before.weightChangeKg!!, 0.0)
        assertEquals(200.0, before.nutrition!!.calories.total!!, 0.0)
        val after = PhaseRecapCalculator.calculate(afterPhaseDeletion.history.single(), false, listOf(last), emptyList(), zone, end)
        assertEquals(phase.confirmedAt, after.start)
        assertEquals(phase.endedAt, after.end)
        assertNull(after.weightChangeKg)
        assertEquals(0, after.mealsLogged)
        assertEquals(1, after.loggingDays)
        assertNull(after.nutrition!!.calories.total)
    }

    @Test fun `boundary local midnight weights are excluded and reported but precise times remain`() {
        val partialStart = Instant.parse("2024-03-31T10:00:00Z")
        val recap = PhaseRecapCalculator.calculate(phase.copy(confirmedAt = partialStart), false, listOf(
            weight("boundary", "2024-03-30T23:00:00Z", 70.0),
            weight("precise", "2024-03-31T11:00:00Z", 71.0),
        ), emptyList(), zone, end)
        assertEquals(listOf("precise"), recap.weights.map { it.id })
        assertEquals(1, recap.boundaryWeightTimesUnavailable)
        assertNull(recap.weightChangeKg)
    }
}
