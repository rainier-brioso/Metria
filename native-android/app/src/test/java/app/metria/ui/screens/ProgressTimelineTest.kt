package app.metria.ui.screens

import app.metria.MealEntry
import app.metria.MealEntryItem
import app.metria.MealNutrition
import app.metria.WeightEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTimelineTest {
    private val end = LocalDate.parse("2026-08-27")

    @Test
    fun `builds seven inclusive days and uses latest weight per day`() {
        val weights = listOf(
            WeightEntry(kilograms = 70.0, recordedAt = Instant.parse("2026-08-26T08:00:00Z")),
            WeightEntry(kilograms = 70.4, recordedAt = Instant.parse("2026-08-26T18:00:00Z")),
        )

        val result = ProgressTimeline.sevenDays(weights, emptyList(), end, ZoneOffset.UTC)

        assertEquals(LocalDate.parse("2026-08-21"), result.first().date)
        assertEquals(end, result.last().date)
        assertEquals(70.4, result[5].latestWeightKilograms!!, 0.001)
        assertEquals(2, result[5].weightEntries.size)
        assertNull(result.last().latestWeightKilograms)
    }

    @Test
    fun `calendar periods are inclusive and handle leap and short months`() {
        assertEquals(LocalDate.parse("2024-03-01"), ProgressRange.Quarter.start(LocalDate.parse("2024-05-31")))
        assertEquals(LocalDate.parse("2024-03-02"), ProgressRange.Quarter.start(LocalDate.parse("2024-05-31")).plusDays(1))
        assertEquals(LocalDate.parse("2024-02-29"), ProgressRange.Year.start(LocalDate.parse("2025-02-28")))
        val result = ProgressTimeline.range(emptyList(), emptyList(), ProgressRange.Month, LocalDate.parse("2026-03-31"), ZoneOffset.UTC)
        assertEquals(LocalDate.parse("2026-03-01"), result.first().date)
        assertEquals(LocalDate.parse("2026-03-31"), result.last().date)
        assertEquals(31, result.size)
    }

    @Test
    fun `year range retains gaps and latest of same-day weight readings`() {
        val outside = WeightEntry(kilograms = 99.0, recordedAt = Instant.parse("2024-02-28T08:00:00Z"))
        val oldReading = WeightEntry(kilograms = 71.1, recordedAt = Instant.parse("2025-01-03T08:00:00Z"))
        val latestReading = WeightEntry(kilograms = 70.8, recordedAt = Instant.parse("2025-01-03T19:00:00Z"))
        val result = ProgressTimeline.range(
            listOf(outside, oldReading, latestReading), emptyList(), ProgressRange.Year,
            LocalDate.parse("2025-02-28"), ZoneOffset.UTC,
        )
        val recordedDay = result.first { it.date == LocalDate.parse("2025-01-03") }
        assertEquals(366, result.size) // Leap day is retained in the inclusive year.
        assertEquals(2, recordedDay.weightEntries.size)
        assertEquals(70.8, recordedDay.latestWeightKilograms!!, 0.001)
        assertTrue(result.none { day -> day.weightEntries.any { it.id == outside.id } })
        assertNull(recordedDay.caloriesKcal)
        assertNull(result.first { it.date == LocalDate.parse("2024-02-29") }.latestWeightKilograms)
    }

    @Test
    fun `daily nutrition sums partial values and preserves partial marker`() {
        val partial = meal("2026-08-27T12:00:00Z", 210.0, 14.0, "partial")
        val day = ProgressTimeline.range(emptyList(), listOf(partial), ProgressRange.Week, end, ZoneOffset.UTC).last()
        assertEquals(210.0, day.caloriesKcal!!, 0.001)
        assertEquals(14.0, day.proteinGrams!!, 0.001)
        assertTrue(day.nutritionIsPartial)
    }

    @Test
    fun `timeline groups nutrition according to requested timezone near midnight`() {
        val meal = meal("2026-08-27T23:30:00Z", 340.0, 24.0, "full")
        val timeline = ProgressTimeline.range(
            emptyList(), listOf(meal), ProgressRange.Week, LocalDate.parse("2026-08-28"), ZoneId.of("Europe/Madrid"),
        )
        val localDay = timeline.last()
        assertEquals(LocalDate.parse("2026-08-28"), localDay.date)
        assertEquals(340.0, localDay.caloriesKcal!!, 0.001)
        assertEquals(24.0, localDay.proteinGrams!!, 0.001)
    }

    @Test
    fun `adds known full and partial nutrition without hiding missing coverage`() {
        val meals = listOf(
            meal("2026-08-27T12:00:00Z", 400.0, 30.0, "full"),
            meal("2026-08-27T18:00:00Z", 200.0, 12.0, "partial"),
            MealEntry(
                recordedAt = Instant.parse("2026-08-27T20:00:00Z"),
                items = listOf(MealEntryItem("Unknown meal", null, null)),
                assumptions = emptyList(),
                confidence = "low",
                source = "manual_entry",
            ),
        )

        val day = ProgressTimeline.sevenDays(emptyList(), meals, end, ZoneOffset.UTC).last()

        assertEquals(600.0, day.caloriesKcal!!, 0.001)
        assertEquals(42.0, day.proteinGrams!!, 0.001)
        assertEquals(3, day.meals.size)
        assertTrue(day.nutritionIsPartial)
    }

    @Test
    fun `history keeps every entry grouped newest date and time first`() {
        val weights = listOf(
            WeightEntry(kilograms = 69.0, recordedAt = Instant.parse("2026-07-01T08:00:00Z")),
            WeightEntry(kilograms = 70.0, recordedAt = Instant.parse("2026-08-27T08:00:00Z")),
            WeightEntry(kilograms = 70.2, recordedAt = Instant.parse("2026-08-27T18:00:00Z")),
        )
        val meals = listOf(
            meal("2026-06-15T12:00:00Z", 300.0, 20.0, "full"),
            meal("2026-08-26T18:00:00Z", 500.0, 25.0, "full"),
        )

        val weightGroups = ProgressHistory.weightGroups(weights, ZoneOffset.UTC)
        val mealGroups = ProgressHistory.mealGroups(meals, ZoneOffset.UTC)

        assertEquals(3, weightGroups.sumOf { it.entries.size })
        assertEquals(LocalDate.parse("2026-08-27"), weightGroups.first().date)
        assertEquals(70.2, weightGroups.first().entries.first().kilograms, 0.001)
        assertEquals(2, mealGroups.sumOf { it.meals.size })
        assertEquals(LocalDate.parse("2026-08-26"), mealGroups.first().date)
    }

    private fun meal(at: String, calories: Double, protein: Double, coverage: String) = MealEntry(
        recordedAt = Instant.parse(at),
        items = listOf(MealEntryItem("Food", 100, null)),
        assumptions = emptyList(),
        confidence = "high",
        source = "member_confirmed_catalog",
        nutrition = MealNutrition(
            calories,
            protein,
            20.0,
            10.0,
            source = "member_confirmed_catalog",
            catalogRelease = "test",
            estimateCoverage = coverage,
        ),
    )
}
