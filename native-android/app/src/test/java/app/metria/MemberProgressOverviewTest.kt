package app.metria

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberProgressOverviewTest {
    @Test
    fun `weekly overview combines bounded nutrition goals and in-period weight trend`() {
        val today = LocalDate.of(2026, 8, 28)
        fun instant(daysAgo: Long) = today.minusDays(daysAgo)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val meals = listOf(
            MealEntry(
                recordedAt = instant(1),
                items = listOf(MealEntryItem("rice", 150, null)),
                assumptions = emptyList(),
                confidence = "high",
                source = "manual_entry",
                nutrition = MealNutrition(500.0, 20.0, 70.0, 10.0),
            ),
            MealEntry(
                recordedAt = instant(2),
                items = listOf(MealEntryItem("chicken", 120, null)),
                assumptions = emptyList(),
                confidence = "medium",
                source = "member_confirmed_catalog",
                nutrition = MealNutrition(300.0, 35.0, 5.0, 12.0, source = "member_confirmed_catalog", catalogRelease = "test", estimateCoverage = "partial"),
            ),
        )
        val overview = MemberProgressOverview.from(
            entries = listOf(
                WeightEntry(kilograms = 92.0, recordedAt = instant(6)),
                WeightEntry(kilograms = 91.4, recordedAt = instant(0)),
                WeightEntry(kilograms = 95.0, recordedAt = instant(8)),
            ),
            meals = meals,
            targets = NutritionTargets(caloriesKcal = 2_000.0, proteinGrams = 100.0),
            today = today,
            period = "week",
        )

        assertEquals(2, overview.recordedMeals)
        assertEquals(2, overview.daysWithRecordedMeals)
        assertEquals(800.0, overview.caloriesKcal)
        assertEquals(700.0, overview.proteinGoalGrams)
        assertEquals(2, overview.weightEntryCount)
        assertEquals(-0.6, overview.weightChangeKg!!, 0.0001)
        assertTrue(overview.localEvidence().startsWith("Last 7 days:"))
        assertTrue(overview.localEvidence().contains("Logged meals may not represent total intake."))
        assertEquals("last_7_days_including_today", overview.toolResponse()["period"])
        assertEquals(
            "recorded_meals_only_not_total_intake",
            (overview.toolResponse()["coverage"] as Map<*, *>)["interpretation"],
        )
        assertEquals(6, (overview.toolResponse()["progress"] as Map<*, *>)["calorie_goal_percent"])
        assertEquals(8, (overview.toolResponse()["progress"] as Map<*, *>)["protein_goal_percent"])
    }
}
