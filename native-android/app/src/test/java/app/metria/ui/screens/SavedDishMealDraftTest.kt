package app.metria.ui.screens

import app.metria.CompanionAssistantPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedDishMealDraftTest {
    @Test
    fun `saved dish components stay grouped and unknown portions remain blank`() {
        val prefill = CompanionAssistantPolicy.SavedDishMealPrefill(
            sourceDishId = "dish-id",
            title = "Usual breakfast",
            items = listOf(
                CompanionAssistantPolicy.SavedDishItemPrefill(
                    name = "Oats", description = null, grams = null, preparation = null,
                    catalogFoodId = null, catalogRelease = null, role = "main_food",
                ),
                CompanionAssistantPolicy.SavedDishItemPrefill(
                    name = "Milk", description = null, grams = 180, preparation = null,
                    catalogFoodId = null, catalogRelease = null, role = "main_food",
                ),
            ),
        )

        val drafts = savedDishInitialDrafts(prefill)

        assertEquals(1, drafts.map(MealReviewItemDraft::dishId).distinct().size)
        assertEquals("", drafts[0].grams)
        assertEquals("180", drafts[1].grams)
        assertEquals(listOf("Oats", "Milk"), drafts.map(MealReviewItemDraft::name))
    }
}
