package app.metria

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
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
    val title: String? = null,
    /** Member-selected classification; older records intentionally remain null. */
    val mealSlot: String? = null,
    /** False means an older record was saved with a date only. */
    val recordedTimeKnown: Boolean = true,
    val items: List<MealEntryItem>,
    val assumptions: List<String>,
    val confidence: String,
    val source: String,
    val nutrition: MealNutrition? = null,
    val referenceThumbnailBase64: String? = null,
    /** Foreign key to the encrypted local audit record, when an image created this meal. */
    val analysisAuditId: String? = null,
)

data class MealEntryItem(
    val name: String,
    val grams: Int?,
    val preparation: String?,
    val catalogFoodId: Long? = null,
    val catalogRelease: String? = null,
    val groupLabel: String? = null,
    val description: String? = null,
    val role: String = "main_food",
)

/** Encrypted, member-confirmed ranking hint; never a model training example. */
data class FoodCorrection(val observedName: String, val confirmedFoodId: Long, val count: Int = 1)

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
        (entry.title == null || entry.title.trim().length in 1..120) &&
            (entry.mealSlot == null || entry.mealSlot in MealSlots.allowed) &&
            entry.items.isNotEmpty() && entry.items.size <= 12 &&
            entry.items.all { item ->
                item.name.trim().length in 1..120 &&
                    (item.grams == null || item.grams in 1..2_000) &&
                    (item.preparation == null || item.preparation.length <= 120) &&
                    (item.groupLabel == null || item.groupLabel.length in 1..120) &&
                    (item.description == null || item.description.length <= 280) &&
                    item.role in setOf("main_food", "side", "drink", "sauce_oil", "dry_seasoning") &&
                    ((item.catalogFoodId == null && item.catalogRelease == null) ||
                        (item.catalogFoodId != null && !item.catalogRelease.isNullOrBlank()))
            } &&
            entry.assumptions.size <= 12 && entry.assumptions.all { it.length <= 280 } &&
            entry.confidence in setOf("low", "medium", "high") &&
            entry.source in setOf("ai_estimate", "manual_entry", "member_confirmed_catalog") &&
            (entry.referenceThumbnailBase64 == null || entry.referenceThumbnailBase64.length in 1..180_000) &&
            (entry.analysisAuditId == null || runCatching { UUID.fromString(entry.analysisAuditId) }.isSuccess) &&
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

object MealSlots {
    val allowed = setOf("breakfast", "lunch", "dinner", "snack")

    fun label(slot: String?): String = when (slot) {
        "breakfast" -> "Breakfast"
        "lunch" -> "Lunch"
        "dinner" -> "Dinner"
        "snack" -> "Snack"
        else -> "Not classified"
    }

    /** A private hint only; a member-selected type always takes precedence. */
    fun suggestedFor(time: LocalTime): String = when (time.hour) {
        in 5..10 -> "breakfast"
        in 11..15 -> "lunch"
        in 16..18 -> "snack"
        else -> "dinner"
    }

    fun matches(entry: MealEntry, requested: String): Boolean =
        entry.mealSlot == requested ||
            (entry.mealSlot == null && entry.recordedTimeKnown &&
                suggestedFor(entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime()) == requested)
}
