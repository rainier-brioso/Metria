package app.metria.ui.screens

import app.metria.MealProposal

internal data class MealReviewItemState(
    val id: String,
    val dishId: String,
    val dishName: String,
    val name: String,
    val description: String,
    val grams: String,
    val preparation: String,
    val role: String,
    val evidence: String,
    val drinkPortion: String = "",
    val portionInput: String = "",
)

/** Values that can affect the meal eventually written by the review flow. */
internal data class MealReviewDraftState(
    val items: List<MealReviewItemState>,
    val matchIds: Map<String, Long>,
    val includedPossibleIds: Set<String>,
    val title: String,
    val date: String,
    val mealSlot: String?,
    val keepReference: Boolean,
    val dishMemoryChoices: Map<String, Boolean>,
    val acceptedSuggestionIds: Map<String, String>,
)

internal object MealReviewExitPolicy {
    fun requiresConfirmation(initial: MealReviewDraftState, current: MealReviewDraftState): Boolean =
        initial != current

    /** A primitive-only list lets Compose restore the baseline after rotation. */
    fun saveableFields(state: MealReviewDraftState): List<String> = buildList {
        add("2")
        add(state.items.size.toString())
        state.items.forEach { item ->
            addAll(listOf(item.id, item.dishId, item.dishName, item.name, item.description, item.grams, item.preparation, item.role, item.evidence, item.drinkPortion, item.portionInput))
        }
        add(state.matchIds.size.toString())
        state.matchIds.forEach { (id, foodId) -> add(id); add(foodId.toString()) }
        add(state.includedPossibleIds.size.toString())
        addAll(state.includedPossibleIds)
        add(state.title)
        add(state.date)
        add(state.mealSlot ?: "~")
        add(if (state.keepReference) "1" else "0")
        add(state.dishMemoryChoices.size.toString())
        state.dishMemoryChoices.forEach { (id, enabled) -> add(id); add(if (enabled) "1" else "0") }
        add(state.acceptedSuggestionIds.size.toString())
        state.acceptedSuggestionIds.forEach { (dishId, suggestionId) -> add(dishId); add(suggestionId) }
    }

    fun restoreSaveableFields(fields: List<String>): MealReviewDraftState? = runCatching {
        var index = 0
        fun next(): String = fields[index++]
        fun count(): Int = next().toInt().also { require(it >= 0) }
        val version = next()
        require(version in setOf("1", "2"))
        val items = List(count()) {
            val item = MealReviewItemState(next(), next(), next(), next(), next(), next(), next(), next(), next())
            if (version == "2") item.copy(drinkPortion = next(), portionInput = next()) else item
        }
        val matches = buildMap { repeat(count()) { put(next(), next().toLong()) } }
        val included = buildSet { repeat(count()) { add(next()) } }
        val title = next()
        val date = next()
        val slot = next().takeUnless { it == "~" }
        val keepReference = next().toBooleanFlag()
        val memoryChoices = buildMap { repeat(count()) { put(next(), next().toBooleanFlag()) } }
        val accepted = buildMap { repeat(count()) { put(next(), next()) } }
        require(index == fields.size)
        MealReviewDraftState(items, matches, included, title, date, slot, keepReference, memoryChoices, accepted)
    }.getOrNull()

    private fun String.toBooleanFlag(): Boolean = when (this) {
        "1" -> true
        "0" -> false
        else -> error("Invalid saved boolean")
    }
}

/** Keeps manual fallbacks distinct from accepted model drafts at the save boundary. */
internal object MealReviewMode {
    fun blankManualProposal(): MealProposal = MealProposal(emptyList(), emptyList(), emptyList(), "low")

    fun source(manualEntry: Boolean, hasCatalogNutrition: Boolean): String = when {
        hasCatalogNutrition -> "member_confirmed_catalog"
        manualEntry -> "manual_entry"
        else -> "ai_estimate"
    }

    fun needsFirstDish(manualEntry: Boolean, itemCount: Int): Boolean = manualEntry && itemCount == 0

    /** Unknown omitted photo foods keep recorded nutrition explicitly partial. */
    fun estimateCoverage(proposal: MealProposal, includedCount: Int, reviewedCount: Int): String =
        if (proposal.omittedItemCount > 0 || includedCount != reviewedCount) "partial" else "full"
}
