package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealEntryValidationTest {
    @Test
    fun acceptsConfirmedAiEstimateWithOptionalManualNutrition() {
        assertTrue(MealEntryValidation.valid(
            MealEntry(
                items = listOf(MealEntryItem("Chicken", 180, "Grilled")),
                assumptions = listOf("Portion was estimated by the member."),
                confidence = "low",
                source = "ai_estimate",
                nutrition = MealNutrition(320.0, 42.0, 0.0, 12.0),
            )
        ))
    }

    @Test
    fun rejectsImpossiblePortionAndUnknownSource() {
        assertFalse(MealEntryValidation.valid(
            MealEntry(
                items = listOf(MealEntryItem("Chicken", 0, null)),
                assumptions = emptyList(),
                confidence = "high",
                source = "model",
            )
        ))
    }
}
