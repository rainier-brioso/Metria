package app.metria

/** Small draft policies shared by the saved-meal editor and its regression tests. */
object SavedMealEditPolicy {
    fun foodNameChanged(previousName: String, updatedName: String): Boolean =
        previousName.trim() != updatedName.trim()

    fun catalogMatchAfterFoodNameEdit(previousName: String, updatedName: String, matchId: String?): String? =
        matchId.takeUnless { foodNameChanged(previousName, updatedName) }

    fun applyReferenceThumbnail(meal: MealEntry, thumbnailBase64: String?): MealEntry =
        if (thumbnailBase64 == null) meal.copy(referenceThumbnailBase64 = null)
        else meal.copy(referenceThumbnailBase64 = thumbnailBase64)

    fun canSaveCatalogBackedEdit(isCatalogMeal: Boolean, catalogAvailable: Boolean, unresolvedKeptMatch: Boolean): Boolean =
        (!isCatalogMeal || catalogAvailable) && !unresolvedKeptMatch

    fun requiresNutritionCoverageConfirmation(original: MealNutrition?, updated: MealNutrition?): Boolean {
        if (original?.source != "member_confirmed_catalog") return false
        return nutritionCoverageRank(updated) < nutritionCoverageRank(original)
    }

    fun catalogEstimateCoverage(
        original: MealNutrition?,
        hasTotals: Boolean,
        includedFoodCount: Int,
        foodCount: Int,
    ): String? = when {
        !hasTotals || includedFoodCount <= 0 -> null
        includedFoodCount < foodCount -> "partial"
        original?.source == "member_confirmed_catalog" && original.estimateCoverage == "partial" -> "partial"
        else -> "full"
    }

    fun catalogMatchIsUnresolved(
        savedFoodName: String,
        currentFoodName: String,
        savedCatalogId: Long?,
        selectedCatalogId: Long?,
        catalogFoodIds: Set<Long>,
    ): Boolean = savedCatalogId != null && !foodNameChanged(savedFoodName, currentFoodName) &&
        savedCatalogId !in catalogFoodIds && selectedCatalogId !in catalogFoodIds

    private fun nutritionCoverageRank(nutrition: MealNutrition?): Int = when {
        nutrition == null -> 0
        listOf(nutrition.caloriesKcal, nutrition.proteinGrams, nutrition.carbohydrateGrams, nutrition.fatGrams).all { it == null } -> 0
        nutrition.estimateCoverage == "partial" || listOf(
            nutrition.caloriesKcal, nutrition.proteinGrams,
            nutrition.carbohydrateGrams, nutrition.fatGrams,
        ).any { it == null } -> 1
        else -> 2
    }
}
