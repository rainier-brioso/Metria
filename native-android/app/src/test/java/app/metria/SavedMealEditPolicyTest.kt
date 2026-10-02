package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SavedMealEditPolicyTest {
    @Test
    fun foodRenameIsDetectedForClearingOldCatalogSelection() {
        assertTrue(SavedMealEditPolicy.foodNameChanged("Fried balls", "Albóndigas"))
        assertTrue(!SavedMealEditPolicy.foodNameChanged(" Fried balls ", "Fried balls"))
        assertNull(SavedMealEditPolicy.catalogMatchAfterFoodNameEdit("Fried balls", "Albóndigas", "123"))
        assertEquals("456", SavedMealEditPolicy.catalogMatchAfterFoodNameEdit("Albóndigas", "Albóndigas", "456"))
    }

    @Test
    fun pendingPhotoRemovalChangesOnlyTheSavedDraftValueAndLeavesOriginalForCancel() {
        val original = MealEntry(
            recordedAt = Instant.parse("2026-09-29T12:00:00Z"),
            items = listOf(MealEntryItem("Rice", 100, null)),
            assumptions = emptyList(),
            confidence = "medium",
            source = "manual_entry",
            referenceThumbnailBase64 = "private-thumbnail",
        )

        val saveDraft = SavedMealEditPolicy.applyReferenceThumbnail(original, null)

        assertNull(saveDraft.referenceThumbnailBase64)
        assertEquals("private-thumbnail", original.referenceThumbnailBase64)
        assertEquals(original, SavedMealEditPolicy.applyReferenceThumbnail(original, original.referenceThumbnailBase64))
    }

    @Test
    fun correctedCatalogMealCanBeRebuiltWithoutStaleNutritionMatch() {
        val original = MealEntryItem("Fried balls", 300, null, 123L, "release-1")
        val corrected = original.copy(name = "Albóndigas", catalogFoodId = null, catalogRelease = null)
        val updatedMeal = MealEntry(
            items = listOf(corrected), assumptions = emptyList(), confidence = "medium",
            source = "member_confirmed_catalog",
            nutrition = null,
        )

        assertTrue(MealEntryValidation.valid(updatedMeal))
        assertNull(updatedMeal.items.single().catalogFoodId)
        assertNull(updatedMeal.nutrition)
    }

    @Test
    fun catalogBackedSaveIsBlockedWhenCatalogIsUnavailableOrStoredMatchIsUnresolved() {
        assertFalse(SavedMealEditPolicy.canSaveCatalogBackedEdit(isCatalogMeal = true, catalogAvailable = false, unresolvedKeptMatch = false))
        assertFalse(SavedMealEditPolicy.canSaveCatalogBackedEdit(isCatalogMeal = true, catalogAvailable = true, unresolvedKeptMatch = true))
        assertTrue(SavedMealEditPolicy.canSaveCatalogBackedEdit(isCatalogMeal = true, catalogAvailable = true, unresolvedKeptMatch = false))
        assertTrue(SavedMealEditPolicy.canSaveCatalogBackedEdit(isCatalogMeal = false, catalogAvailable = false, unresolvedKeptMatch = false))
    }

    @Test
    fun catalogIdentityIsOnlyUnresolvedWhenTheFoodNameWasLeftUnchanged() {
        assertTrue(SavedMealEditPolicy.catalogMatchIsUnresolved("Fried balls", "Fried balls", 99L, null, setOf(12L)))
        assertFalse(SavedMealEditPolicy.catalogMatchIsUnresolved("Fried balls", "Albóndigas", 99L, null, setOf(12L)))
        assertFalse(SavedMealEditPolicy.catalogMatchIsUnresolved("Fried balls", "Fried balls", 12L, 12L, setOf(12L)))
        assertFalse(SavedMealEditPolicy.catalogMatchIsUnresolved("Fried balls", "Fried balls", 99L, 12L, setOf(12L)))
    }

    @Test
    fun catalogCoverageDowngradesRequireConfirmationIncludingNoEstimateAndPartialCoverage() {
        val full = catalogNutrition(coverage = "full")
        val partial = catalogNutrition(coverage = "partial")
        assertTrue(SavedMealEditPolicy.requiresNutritionCoverageConfirmation(full, null))
        assertTrue(SavedMealEditPolicy.requiresNutritionCoverageConfirmation(full, partial))
        assertFalse(SavedMealEditPolicy.requiresNutritionCoverageConfirmation(full, catalogNutrition(coverage = "full")))
        assertFalse(SavedMealEditPolicy.requiresNutritionCoverageConfirmation(null, null))
    }

    @Test
    fun savedPartialCatalogCoverageRemainsPartialWhenAllRowsStillMatch() {
        assertEquals(
            "partial",
            SavedMealEditPolicy.catalogEstimateCoverage(
                catalogNutrition(coverage = "partial"),
                hasTotals = true,
                includedFoodCount = 2,
                foodCount = 2,
            ),
        )
    }

    @Test
    fun savedFullCatalogCoverageStaysFullWhenAllRowsStillMatch() {
        assertEquals(
            "full",
            SavedMealEditPolicy.catalogEstimateCoverage(
                catalogNutrition(coverage = "full"),
                hasTotals = true,
                includedFoodCount = 2,
                foodCount = 2,
            ),
        )
    }

    @Test
    fun missingCurrentCatalogMatchMakesCoveragePartial() {
        assertEquals(
            "partial",
            SavedMealEditPolicy.catalogEstimateCoverage(
                catalogNutrition(coverage = "full"),
                hasTotals = true,
                includedFoodCount = 1,
                foodCount = 2,
            ),
        )
    }

    @Test
    fun absentCatalogTotalsProduceNoEstimateCoverage() {
        assertNull(
            SavedMealEditPolicy.catalogEstimateCoverage(
                catalogNutrition(coverage = "partial"),
                hasTotals = false,
                includedFoodCount = 0,
                foodCount = 2,
            ),
        )
    }

    private fun catalogNutrition(coverage: String) = MealNutrition(
        caloriesKcal = 500.0,
        proteinGrams = 30.0,
        carbohydrateGrams = 40.0,
        fatGrams = 20.0,
        source = "member_confirmed_catalog",
        catalogRelease = "release-1",
        estimateCoverage = coverage,
    )
}
