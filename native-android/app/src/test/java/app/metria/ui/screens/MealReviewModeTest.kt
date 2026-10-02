package app.metria.ui.screens

import app.metria.MealProposal
import app.metria.MealProposalParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MealReviewModeTest {
    @Test
    fun cleanReviewCanLeaveWithoutConfirmation() {
        val initial = reviewState()

        assertEquals(false, MealReviewExitPolicy.requiresConfirmation(initial, initial.copy()))
    }

    @Test
    fun saveRelevantEditsRequireConfirmationAndUndoReturnsClean() {
        val initial = reviewState()
        val changed = initial.copy(
            items = initial.items + MealReviewItemState(
                id = "rice", dishId = "dish-2", dishName = "Rice", name = "Rice",
                description = "", grams = "120", preparation = "steamed", role = "main_food", evidence = "seen",
            ),
            title = "Stew and rice",
            date = "2026-09-30",
            mealSlot = "lunch",
            keepReference = false,
            matchIds = initial.matchIds + ("rice" to 42L),
            includedPossibleIds = setOf("rice"),
            dishMemoryChoices = mapOf("dish-2" to true),
            acceptedSuggestionIds = mapOf("dish-2" to "saved-rice"),
        )
        val undone = changed.copy(
            items = initial.items,
            title = initial.title,
            date = initial.date,
            mealSlot = initial.mealSlot,
            keepReference = initial.keepReference,
            matchIds = initial.matchIds,
            includedPossibleIds = initial.includedPossibleIds,
            dishMemoryChoices = initial.dishMemoryChoices,
            acceptedSuggestionIds = initial.acceptedSuggestionIds,
        )

        assertEquals(true, MealReviewExitPolicy.requiresConfirmation(initial, changed))
        assertEquals(false, MealReviewExitPolicy.requiresConfirmation(initial, undone))
    }

    @Test
    fun eachSaveRelevantChoiceIndependentlyRequiresConfirmation() {
        val initial = reviewState()
        val changedStates = listOf(
            initial.copy(items = initial.items.map { it.copy(name = "Corrected stew") }),
            initial.copy(matchIds = emptyMap()),
            initial.copy(includedPossibleIds = setOf("food")),
            initial.copy(title = "Lunch"),
            initial.copy(date = "2026-09-30"),
            initial.copy(mealSlot = "lunch"),
            initial.copy(keepReference = false),
            initial.copy(dishMemoryChoices = mapOf("dish-1" to false)),
            initial.copy(acceptedSuggestionIds = mapOf("dish-1" to "saved-dish")),
        )

        changedStates.forEachIndexed { index, current ->
            assertEquals("save-relevant choice $index", true, MealReviewExitPolicy.requiresConfirmation(initial, current))
        }
    }

    @Test
    fun savedBaselineCanBeRestoredAcrossConfigurationRecreation() {
        val initial = reviewState()
        val restored = MealReviewExitPolicy.restoreSaveableFields(MealReviewExitPolicy.saveableFields(initial))

        assertEquals(initial, restored)
        assertEquals(false, MealReviewExitPolicy.requiresConfirmation(initial, requireNotNull(restored)))
    }

    @Test
    fun rejectedPhotoFallbackCannotBeSavedAsAnAiEstimate() {
        assertEquals(emptyList<Any>(), MealReviewMode.blankManualProposal().items)
        assertEquals(emptyList<String>(), MealReviewMode.blankManualProposal().assumptions)
        assertEquals("manual_entry", MealReviewMode.source(manualEntry = true, hasCatalogNutrition = false))
        assertEquals("member_confirmed_catalog", MealReviewMode.source(manualEntry = true, hasCatalogNutrition = true))
    }

    @Test
    fun emptyManualReviewStartsWithAddDishNotSave() {
        assertEquals(true, MealReviewMode.needsFirstDish(manualEntry = true, itemCount = 0))
        assertEquals(false, MealReviewMode.needsFirstDish(manualEntry = true, itemCount = 1))
        assertEquals(false, MealReviewMode.needsFirstDish(manualEntry = false, itemCount = 0))
    }

    @Test
    fun omittedPhotoFoodsKeepCompleteRetainedFoodsNutritionPartial() {
        val partialPhoto = requireNotNull(MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[{"name":"Beans","visible_portion":"one bowl","estimated_grams_range":{"min":100,"max":180},"preparation":"cooked","confidence":"medium"},{"name":"Unknown","visible_portion":"some","estimated_grams_range":{"min":20,"max":40},"preparation":"","confidence":"medium","evidence":"unverified description"}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        ))
        val completePhoto = MealProposal(emptyList(), emptyList(), emptyList(), "medium")

        assertEquals("partial", MealReviewMode.estimateCoverage(partialPhoto, includedCount = 1, reviewedCount = 1))
        assertEquals("partial", MealReviewMode.estimateCoverage(completePhoto, includedCount = 1, reviewedCount = 2))
        assertEquals("full", MealReviewMode.estimateCoverage(completePhoto, includedCount = 2, reviewedCount = 2))
    }

    private fun reviewState() = MealReviewDraftState(
        items = listOf(
            MealReviewItemState(
                id = "food", dishId = "dish-1", dishName = "Stew", name = "Vegetable stew",
                description = "", grams = "250", preparation = "cooked", role = "main_food", evidence = "seen",
            ),
        ),
        matchIds = mapOf("food" to 7L),
        includedPossibleIds = emptySet(),
        title = "Vegetable stew",
        date = "2026-09-29",
        mealSlot = null,
        keepReference = true,
        dishMemoryChoices = emptyMap(),
        acceptedSuggestionIds = emptyMap(),
    )
}
