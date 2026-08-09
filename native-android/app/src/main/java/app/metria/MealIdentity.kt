package app.metria

/** Private, deterministic meal naming and similarity; never calls a model or network service. */
object MealIdentity {
    fun displayTitle(meal: MealEntry): String = meal.title?.takeIf(String::isNotBlank) ?: titleFor(meal.items)

    fun titleFor(items: List<MealEntryItem>): String = items
        .map { it.name.trim() }
        .filter(String::isNotBlank)
        .distinct()
        .take(3)
        .joinToString(" and ")
        .ifBlank { "Meal" }

    /**
     * A repeat is the same reviewed ingredient set with portions within 25% (or
     * 30 g). Catalog IDs are preferred, but an approved local food name is a
     * stable fallback when a sauce or other ingredient has no catalog match.
     * It is a suggestion only, never an automatic reuse.
     */
    fun similarSavedMeal(draftItems: List<MealEntryItem>, savedMeals: List<MealEntry>): MealEntry? {
        if (draftItems.isEmpty() || draftItems.any { it.grams == null || canonicalFood(it) == null }) return null
        val draft = draftItems.map { canonicalFood(it)!! to it.grams!! }.sortedBy { it.first }
        return savedMeals.asReversed().firstOrNull { saved ->
            if (saved.items.size != draft.size || saved.items.any { it.grams == null || canonicalFood(it) == null }) return@firstOrNull false
            val candidate = saved.items.map { canonicalFood(it)!! to it.grams!! }.sortedBy { it.first }
            draft.zip(candidate).all { (current, previous) ->
                current.first == previous.first && kotlin.math.abs(current.second - previous.second) <= maxOf(30, previous.second / 4)
            }
        }
    }

    private fun canonicalFood(item: MealEntryItem): String? = item.catalogFoodId?.let { "catalog:$it" }
        ?: item.name.trim().lowercase().replace(Regex("\\s+"), " ").takeIf(String::isNotBlank)?.let { "name:$it" }
}
