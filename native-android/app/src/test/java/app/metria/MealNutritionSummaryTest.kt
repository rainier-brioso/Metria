package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MealNutritionSummaryTest {
    private fun meal(day: Int, nutrition: MealNutrition?) = MealEntry(
        recordedAt = Instant.parse("2026-08-%02dT12:00:00Z".format(day)),
        items = listOf(MealEntryItem("Food", 100, null)),
        assumptions = emptyList(), confidence = "high", source = "manual_entry", nutrition = nutrition,
    )

    @Test fun `totals complete meals and never interprets missing nutrition as zero`() {
        val summary = MealNutritionSummaries.between(
            listOf(
                meal(6, MealNutrition(100.0, 10.0, 20.0, 5.0)),
                meal(6, MealNutrition(200.0, 20.0, 30.0, 10.0)),
                meal(6, null),
                meal(6, MealNutrition(10.0, null, 2.0, 1.0)),
                meal(5, MealNutrition(500.0, 1.0, 1.0, 1.0)),
            ),
            java.time.LocalDate.of(2026, 8, 6), java.time.LocalDate.of(2026, 8, 6),
        )
        assertEquals(4, summary.recordedMeals)
        assertEquals(2, summary.completeNutritionMeals)
        assertEquals(300.0, summary.caloriesKcal!!, 0.001)
        assertEquals(30.0, summary.proteinGrams!!, 0.001)
    }

    @Test fun `empty complete set has no misleading numeric totals`() {
        val summary = MealNutritionSummaries.between(
            listOf(meal(6, null)),
            java.time.LocalDate.of(2026, 8, 6), java.time.LocalDate.of(2026, 8, 6),
        )
        assertEquals(1, summary.recordedMeals)
        assertEquals(0, summary.completeNutritionMeals)
        assertNull(summary.caloriesKcal)
    }

    @Test fun `partial ingredient estimates remain separate from complete totals`() {
        val summary = MealNutritionSummaries.between(
            listOf(
                meal(6, MealNutrition(200.0, 10.0, 30.0, 4.0, estimateCoverage = "partial")),
                meal(6, MealNutrition(100.0, 5.0, 10.0, 2.0)),
            ),
            java.time.LocalDate.of(2026, 8, 6), java.time.LocalDate.of(2026, 8, 6),
        )
        assertEquals(2, summary.recordedMeals)
        assertEquals(1, summary.completeNutritionMeals)
        assertEquals(100.0, summary.caloriesKcal!!, 0.001)
        assertEquals(1, summary.partialNutritionMeals)
        assertEquals(200.0, summary.partialCaloriesKcal!!, 0.001)
    }
}
