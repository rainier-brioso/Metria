package app.metria

import java.time.Instant
import java.util.UUID

/**
 * M3 decision: confirmed meals live in the same encrypted vault as weight.
 * A model may supply an AI-estimate draft, but its text is never a meal record
 * until the member confirms it here. Nutrition remains null unless the member
 * enters it manually or confirms an offline catalog match.
 * A reference is an optional, reduced, metadata-free thumbnail saved only after
 * confirmation. It stays encrypted in this vault and is never the source photo.
 */
data class MealEntry(
    val id: String = UUID.randomUUID().toString(),
    val recordedAt: Instant = Instant.now(),
    val items: List<MealEntryItem>,
    val assumptions: List<String>,
    val confidence: String,
    val source: String,
    val nutrition: MealNutrition? = null,
    val referenceThumbnailBase64: String? = null,
)

data class MealEntryItem(
    val name: String,
    val grams: Int?,
    val preparation: String?,
    val catalogFoodId: Long? = null,
    val catalogRelease: String? = null,
)

data class MealNutrition(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val fatGrams: Double?,
    val source: String = "manual_entry",
    val catalogFoodId: Long? = null,
    val catalogRelease: String? = null,
    /** "partial" means known ingredients only; it must not enter full-day totals. */
    val estimateCoverage: String = "full",
)

object MealEntryValidation {
    fun valid(entry: MealEntry): Boolean =
        entry.items.isNotEmpty() && entry.items.size <= 12 &&
            entry.items.all { item ->
                item.name.trim().length in 1..120 &&
                    (item.grams == null || item.grams in 1..2_000) &&
                    (item.preparation == null || item.preparation.length <= 120) &&
                    ((item.catalogFoodId == null && item.catalogRelease == null) ||
                        (item.catalogFoodId != null && !item.catalogRelease.isNullOrBlank()))
            } &&
            entry.assumptions.size <= 12 && entry.assumptions.all { it.length <= 280 } &&
            entry.confidence in setOf("low", "medium", "high") &&
            entry.source in setOf("ai_estimate", "manual_entry", "member_confirmed_catalog") &&
            (entry.referenceThumbnailBase64 == null || entry.referenceThumbnailBase64.length in 1..180_000) &&
            entry.nutrition?.let(::validNutrition) != false

    private fun validNutrition(nutrition: MealNutrition): Boolean =
        nutrition.source in setOf("manual_entry", "verified_label", "member_confirmed_catalog") &&
            nutrition.estimateCoverage in setOf("full", "partial") &&
            (nutrition.source != "member_confirmed_catalog" || !nutrition.catalogRelease.isNullOrBlank()) && listOf(
            nutrition.caloriesKcal,
            nutrition.proteinGrams,
            nutrition.carbohydrateGrams,
            nutrition.fatGrams,
        ).all { it == null || it in 0.0..10_000.0 }
}
