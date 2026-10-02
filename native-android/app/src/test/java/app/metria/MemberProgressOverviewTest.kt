package app.metria

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberProgressOverviewTest {
    @Test
    fun `overview rounds displayed nutrition before model and factual audit without rounding stored totals`() {
        val today = LocalDate.of(2026, 9, 30)
        val meal = MealEntry(
            recordedAt = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            items = listOf(MealEntryItem("test food", 100, null)),
            assumptions = emptyList(), confidence = "medium", source = "manual_entry",
            nutrition = MealNutrition(345.50000000000006, 12.350000000000001, 20.0, 10.0),
        )
        for (period in listOf("today", "week")) {
            val overview = MemberProgressOverview.from(emptyList(), listOf(meal),
                NutritionTargets(caloriesKcal = 1950.55, proteinGrams = 88.76), today, period)
            assertEquals(meal.nutrition!!.caloriesKcal, overview.caloriesKcal)
            assertEquals(meal.nutrition!!.proteinGrams, overview.proteinGrams)
            val result = overview.toolResponse()
            val nutrition = result["nutrition"] as Map<*, *>
            assertEquals(346.0, nutrition["calories_kcal"])
            assertEquals(12.4, nutrition["protein_g"])
            assertEquals(if (period == "week") 13654.0 else 1951.0, nutrition["calorie_goal_kcal"])
            assertEquals(if (period == "week") 621.3 else 88.8, nutrition["protein_goal_g"])
            assertTrue(overview.localEvidence().contains("346 /"))
            assertTrue(overview.localEvidence().contains("12.4 /"))
            for (language in listOf("English", "Spanish")) {
                val data = result + ("reply_language" to language)
                val reply = MetriaProgressReplyFallback.from("read_progress_overview",
                    RequiredAgentTool("read_progress_overview", period), data)
                assertTrue(reply != null)
                assertTrue(reply!!.contains("346 kcal"))
                assertTrue(reply.contains("12.4 g"))
                val evidence = MetriaAgentFactEvidence.from("read_progress_overview", data)
                assertEquals(reply, MetriaAgentFactAuditor.audit(reply, evidence).acceptedText)
                assertTrue(MetriaAgentFactAuditor.audit("Available estimates total 345.50000000000006 kcal.", evidence).retryable)
            }
        }
    }

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
