package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCorrectionValidationTest {
    @Test fun `legacy hints remain valid while attributed hints require a safe existing meal`() {
        val legacy = FoodCorrection("chicken", 4)
        assertTrue(FoodCorrectionValidation.valid(legacy, emptySet()))
        assertTrue(FoodCorrectionValidation.valid(legacy.copy(sourceMealId = "meal-1"), setOf("meal-1")))
        assertFalse(FoodCorrectionValidation.valid(legacy.copy(sourceMealId = "missing"), setOf("meal-1")))
        assertFalse(FoodCorrectionValidation.valid(legacy.copy(sourceMealId = "bad id"), setOf("bad id")))
    }

    @Test fun `normalization merges only equivalent hints with the same provenance`() {
        val values = listOf(
            FoodCorrection("Café", 4, count = 2, sourceMealId = "meal-1"),
            FoodCorrection("cafe", 4, count = 3, sourceMealId = "meal-1"),
            FoodCorrection("cafe", 4, count = 1),
        )

        val normalized = FoodCorrectionValidation.normalized(values)

        assertEquals(2, normalized.size)
        assertEquals(5, normalized.single { it.sourceMealId == "meal-1" }.count)
        assertEquals(1, normalized.single { it.sourceMealId == null }.count)
        assertTrue(FoodCorrectionValidation.hasUniqueSources(normalized))
    }

    @Test fun `meal unlink keeps other meal and legacy hints while photo-only removal preserves learning`() {
        val source = meal().copy(referenceThumbnailBase64 = "thumbnail")
        val photoRemoved = source.copy(referenceThumbnailBase64 = null)
        val editedAndPhotoRemoved = photoRemoved.copy(title = "Updated")
        val hints = listOf(
            FoodCorrection("rice", 2),
            FoodCorrection("chicken", 4, sourceMealId = "meal-1"),
            FoodCorrection("rice", 2, sourceMealId = "meal-2"),
        )

        assertTrue(MealLearningLifecyclePolicy.removesOnlyReferencePhoto(source, photoRemoved))
        assertFalse(MealLearningLifecyclePolicy.removesOnlyReferencePhoto(source, editedAndPhotoRemoved))
        assertEquals(hints, MealLearningLifecyclePolicy.afterMealMutation(hints, "meal-1", preserveLearning = true))
        assertEquals(listOf(hints[0], hints[2]), MealLearningLifecyclePolicy.afterMealMutation(hints, "meal-1", preserveLearning = false))
    }

    private fun meal() = MealEntry(
        id = "meal-1",
        items = listOf(MealEntryItem("Rice", 100, null)),
        assumptions = emptyList(),
        confidence = "medium",
        source = "ai_estimate",
    )
}
