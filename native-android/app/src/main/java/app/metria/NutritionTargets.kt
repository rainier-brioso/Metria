package app.metria

/** Optional member-entered daily targets; Metria never infers or recommends them. */
data class NutritionTargets(
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
)

object NutritionTargetValidation {
    fun valid(targets: NutritionTargets): Boolean = listOf(
        targets.caloriesKcal to 10_000.0,
        targets.proteinGrams to 1_000.0,
        targets.carbohydrateGrams to 2_000.0,
        targets.fatGrams to 1_000.0,
    ).all { (value, maximum) -> value == null || value in 1.0..maximum }

    fun empty(targets: NutritionTargets) = listOf(
        targets.caloriesKcal, targets.proteinGrams, targets.carbohydrateGrams, targets.fatGrams,
    ).all { it == null }
}
