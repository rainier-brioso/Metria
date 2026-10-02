package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test fun `historical day result reports exact date and complete partial and missing coverage`() {
        val today = java.time.LocalDate.of(2026, 8, 10)
        val dayMeals = listOf(
            MealEntry(recordedAt = Instant.parse("2026-08-06T12:00:00Z"), items = listOf(MealEntryItem("A", 10, null)), assumptions = emptyList(), confidence = "high", source = "manual_entry", nutrition = MealNutrition(100.0, 10.0, 5.0, 2.0)),
            MealEntry(recordedAt = Instant.parse("2026-08-06T13:00:00Z"), items = listOf(MealEntryItem("B", 10, null)), assumptions = emptyList(), confidence = "high", source = "manual_entry", nutrition = MealNutrition(80.0, 8.0, 4.0, 1.0, estimateCoverage = "partial")),
            MealEntry(recordedAt = Instant.parse("2026-08-06T14:00:00Z"), items = listOf(MealEntryItem("C", 10, null)), assumptions = emptyList(), confidence = "high", source = "manual_entry", nutrition = null),
            MealEntry(recordedAt = Instant.parse("2026-08-05T12:00:00Z"), items = listOf(MealEntryItem("Other day", 10, null)), assumptions = emptyList(), confidence = "high", source = "manual_entry", nutrition = MealNutrition(999.0, 99.0, 9.0, 9.0)),
        )

        val result = HistoricalDayNutritionResult.from(dayMeals, today, 4, "protein")
        val nutrition = result.getValue("nutrition") as Map<*, *>
        val coverage = result.getValue("meals") as Map<*, *>

        assertEquals("historical_day", result["period"])
        assertEquals(4, result["days_ago"])
        assertEquals("2026-08-06", result["from"])
        assertEquals(result["from"], result["to"])
        assertEquals("protein", result["requested_metric"])
        assertEquals(3, coverage["recorded_count"])
        assertEquals(1, coverage["complete_nutrition_count"])
        assertEquals(1, coverage["partial_estimate_count"])
        assertEquals(1, coverage["missing_nutrition_count"])
        assertEquals(18.0, nutrition["protein_g"])
        assertEquals(10.0, nutrition["complete_protein_g"])
        assertEquals(8.0, nutrition["partial_protein_g"])
        assertNull(result["goal"])
        assertEquals("recorded_meals_only_not_total_intake", result["interpretation"])
    }

    @Test fun `historical day with no estimate keeps totals null`() {
        val result = HistoricalDayNutritionResult.from(
            listOf(meal(6, null)), java.time.LocalDate.of(2026, 8, 10), 4,
        )
        val nutrition = result.getValue("nutrition") as Map<*, *>
        assertEquals(1, (result.getValue("meals") as Map<*, *>)["recorded_count"])
        assertNull(nutrition["calories_kcal"])
        assertNull(nutrition["protein_g"])
        assertNull(nutrition["partial_protein_g"])
    }

    @Test fun `historical tool values are quantized while member evidence stays concise and bilingual`() {
        val sourceNutrition = MealNutrition(2210.0750000000003, 82.26, 0.0, 0.0)
        val result = HistoricalDayNutritionResult.from(
            listOf(meal(6, sourceNutrition)), java.time.LocalDate.of(2026, 8, 10), 4, "calories",
        )
        val nutrition = result.getValue("nutrition") as Map<*, *>
        assertEquals(2210.0, nutrition["calories_kcal"])
        assertEquals(82.3, nutrition["protein_g"])
        assertEquals(2210.0750000000003, sourceNutrition.caloriesKcal)

        val spanish = HistoricalDayNutritionEvidenceText.format(result, "Spanish")
        assertTrue(spanish.contains("6 de agosto de 2026"))
        assertTrue(spanish.contains("1 comida registrada"))
        assertTrue(spanish.contains("Estimación de calorías: 2210 kcal"))
        assertTrue(spanish.contains("Solo incluye comidas registradas"))
        assertFalse(spanish.contains("protein"))
        assertFalse(spanish.contains("null"))
        assertFalse(spanish.contains("calories_kcal"))

        val english = HistoricalDayNutritionEvidenceText.format(result, "English")
        assertTrue(english.contains("August 6, 2026"))
        assertTrue(english.contains("1 recorded meal"))
        assertTrue(english.contains("Calorie estimate: 2210 kcal"))
        assertTrue(english.contains("Recorded meals only"))
        assertFalse(english.contains("protein"))

        val missing = HistoricalDayNutritionResult.from(
            listOf(meal(6, null)), java.time.LocalDate.of(2026, 8, 10), 4, "protein",
        )
        assertNull((missing.getValue("nutrition") as Map<*, *>)["protein_g"])
        val missingText = HistoricalDayNutritionEvidenceText.format(missing, "Spanish")
        assertTrue(missingText.contains("sin estimación: 1"))
        assertFalse(missingText.contains("Estimación de proteína"))
        assertFalse(missingText.contains("null"))
    }
}
