package app.metria

import java.time.Instant
import java.util.UUID

/**
 * M3 decision: confirmed meals live in the same encrypted vault as weight.
 * A model may supply an AI-estimate draft, but its text is never a meal record
 * until the member confirms it here. Nutrition remains null unless the member
 * enters it manually; Metria has intentionally selected no food database yet.
 */
data class MealEntry(
    val id: String = UUID.randomUUID().toString(),
    val recordedAt: Instant = Instant.now(),
    val items: List<MealEntryItem>,
    val assumptions: List<String>,
    val confidence: String,
    val source: String,
    val nutrition: MealNutrition? = null,
)

data class MealEntryItem(
    val name: String,
    val grams: Int?,
    val preparation: String?,
)

data class MealNutrition(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val fatGrams: Double?,
    val source: String = "manual_entry",
)

object MealEntryValidation {
    fun valid(entry: MealEntry): Boolean =
        entry.items.isNotEmpty() && entry.items.size <= 12 &&
            entry.items.all { item ->
                item.name.trim().length in 1..120 &&
                    (item.grams == null || item.grams in 1..2_000) &&
                    (item.preparation == null || item.preparation.length <= 120)
            } &&
            entry.assumptions.size <= 12 && entry.assumptions.all { it.length <= 280 } &&
            entry.confidence in setOf("low", "medium", "high") &&
            entry.source in setOf("ai_estimate", "manual_entry") &&
            entry.nutrition?.let(::validNutrition) != false

    private fun validNutrition(nutrition: MealNutrition): Boolean =
        nutrition.source == "manual_entry" && listOf(
            nutrition.caloriesKcal,
            nutrition.proteinGrams,
            nutrition.carbohydrateGrams,
            nutrition.fatGrams,
        ).all { it == null || it in 0.0..10_000.0 }
}
