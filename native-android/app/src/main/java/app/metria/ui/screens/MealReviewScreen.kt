package app.metria.ui.screens

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.view.WindowInsets as PlatformWindowInsets
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.metria.CatalogFood
import app.metria.CatalogNutrition
import app.metria.ChatPortionOverride
import app.metria.FoodCatalog
import app.metria.FoodCorrection
import app.metria.GramRange
import app.metria.MealEntry
import app.metria.MealEntryItem
import app.metria.MealEntryValidation
import app.metria.MealFoodNameCorrectionPresentation
import app.metria.MealFoodNameCorrectionStatus
import app.metria.MealCorrectionDiff
import app.metria.MealCorrectionDiffResult
import app.metria.MealCorrectionMemory
import app.metria.MealCorrectionMemoryPolicy
import app.metria.MealIdentity
import app.metria.MealNutrition
import app.metria.MealProposal
import app.metria.MealProposalItem
import app.metria.MealReviewItemSnapshot
import app.metria.MealReviewSnapshot
import app.metria.MealSlots
import app.metria.OfflineFoodCatalog
import app.metria.PersonalDish
import app.metria.PersonalDishMatch
import app.metria.PersonalDishMatchReason
import app.metria.PersonalDishMatcher
import app.metria.PersonalDishQuery
import app.metria.PersonalDishValidation
import app.metria.ui.components.MetriaEstimateBadge
import app.metria.ui.theme.MetriaColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

internal data class MealReviewItemDraft(
    val id: String,
    val dishId: String,
    val dishName: String,
    val name: String,
    val description: String,
    val grams: String,
    val preparation: String,
    val role: String,
    val evidence: String,
)

private val MealReviewItemsSaver = listSaver<List<MealReviewItemDraft>, String>(
    save = { items ->
        items.flatMap { item ->
            listOf(
                item.id, item.dishId, item.dishName, item.name, item.description,
                item.grams, item.preparation, item.role, item.evidence,
            )
        }
    },
    restore = { saved ->
        saved.chunked(9).mapNotNull { values ->
            values.takeIf { it.size == 9 }?.let {
                MealReviewItemDraft(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8])
            }
        }
    },
)

private val MatchIdsSaver = listSaver<Map<String, Long>, String>(
    save = { matches -> matches.map { (id, foodId) -> "$id|$foodId" } },
    restore = { saved ->
        saved.mapNotNull { value ->
            val split = value.lastIndexOf('|')
            if (split <= 0) null else value.substring(split + 1).toLongOrNull()?.let { value.substring(0, split) to it }
        }.toMap()
    },
)

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private val BooleanMapSaver = listSaver<Map<String, Boolean>, String>(
    save = { choices -> choices.map { (id, enabled) -> "$id|$enabled" } },
    restore = { saved ->
        saved.mapNotNull { value ->
            val split = value.lastIndexOf('|')
            if (split <= 0) null else value.substring(split + 1).toBooleanStrictOrNull()?.let { value.substring(0, split) to it }
        }.toMap()
    },
)

private val StringMapSaver = listSaver<Map<String, String>, String>(
    save = { values -> values.map { (key, value) -> "$key|$value" } },
    restore = { saved ->
        saved.mapNotNull { value ->
            val split = value.indexOf('|')
            if (split <= 0) null else value.substring(0, split) to value.substring(split + 1)
        }.toMap()
    },
)

private val MealReviewDraftStateSaver = listSaver<MealReviewDraftState, String>(
    save = { MealReviewExitPolicy.saveableFields(it) },
    restore = { fields -> requireNotNull(MealReviewExitPolicy.restoreSaveableFields(fields)) },
)

private data class DishGroup(
    val id: String,
    val name: String,
    val items: List<MealReviewItemDraft>,
)

private data class AppliedPersonalDish(
    val items: List<MealReviewItemDraft>,
    val matchIds: Map<String, Long>,
)

private fun reviewProposalItems(proposal: MealProposal, catalog: OfflineFoodCatalog?): List<MealProposalItem> = buildList {
    proposal.items.forEachIndexed { index, item ->
        val names = catalog?.let { FoodCatalog.splitUnmatchedJoinedFood(it, item.name) } ?: listOf(item.name)
        // Reserve one slot for every remaining validated food. A catalog hint
        // must never make a later model food disappear at the 12-item limit.
        val availableForThisItem = 12 - size - (proposal.items.size - index - 1)
        if (names.size == 1 || names.size > availableForThisItem) add(item) else names.forEach { name ->
            add(item.copy(
                name = name,
                visiblePortion = "part of ${item.visiblePortion}",
                estimatedGramsRange = GramRange(
                    (item.estimatedGramsRange.min / names.size).coerceAtLeast(1),
                    (item.estimatedGramsRange.max / names.size).coerceAtLeast(1),
                ),
                preparation = "",
            ))
        }
    }
}

internal fun initialDrafts(items: List<MealProposalItem>): List<MealReviewItemDraft> {
    val groupedDishIds = mutableMapOf<String, String>()
    return items.mapIndexed { index, item ->
        val groupKey = item.groupLabel?.trim()?.takeIf(String::isNotEmpty)
        val dishId = groupKey?.let { groupedDishIds.getOrPut(it) { UUID.randomUUID().toString() } }
            ?: "dish-${UUID.randomUUID()}-$index"
        MealReviewItemDraft(
            id = UUID.randomUUID().toString(),
            dishId = dishId,
            dishName = groupKey ?: item.name,
            name = item.name,
            description = item.description,
            grams = ((item.estimatedGramsRange.min + item.estimatedGramsRange.max) / 2).toString(),
            preparation = item.preparation,
            role = item.role,
            evidence = item.evidence,
        )
    }
}

internal fun initialDraftsWithPortionOverride(
    items: List<MealProposalItem>,
    proposal: MealProposal,
    override: ChatPortionOverride?,
): List<MealReviewItemDraft> {
    val drafts = initialDrafts(items)
    if (override == null || override.grams !in 1..2_000 || override.originalProposal != proposal) return drafts
    val original = proposal.items.getOrNull(override.itemIndex) ?: return drafts
    val expectedTarget = app.metria.ChatPortionClarificationPolicy.target(proposal, "meal", true)
    if (expectedTarget?.itemIndex != override.itemIndex ||
        !original.name.trim().equals(override.itemName, ignoreCase = true)
    ) return drafts
    // Match the original validated item after reviewProposalItems has performed
    // any earlier joined-food splits; its list index may have shifted.
    val matches = items.indices.filter { items[it] == original }
    if (matches.size != 1) return drafts
    return drafts.toMutableList().also { updated ->
        val index = matches.single()
        updated[index] = updated[index].copy(grams = override.grams.toString())
    }
}

internal fun savedDishInitialDrafts(
    prefill: app.metria.CompanionAssistantPolicy.SavedDishMealPrefill,
): List<MealReviewItemDraft> {
    val dishId = "dish-${UUID.randomUUID()}"
    return prefill.items.take(12).map { item ->
        MealReviewItemDraft(
            id = UUID.randomUUID().toString(),
            dishId = dishId,
            dishName = prefill.title,
            name = item.name,
            description = item.description.orEmpty(),
            grams = item.grams?.toString().orEmpty(),
            preparation = item.preparation.orEmpty(),
            role = item.role,
            evidence = "seen",
        )
    }
}

private fun correctionSnapshot(
    items: List<MealReviewItemDraft>,
    matchIds: Map<String, Long>,
    catalogRelease: String?,
): MealReviewSnapshot = MealReviewSnapshot(items.map { item ->
    val foodId = matchIds[item.id]
    MealReviewItemSnapshot(
        itemId = item.id,
        name = item.name,
        groupId = item.dishId,
        groupName = item.dishName,
        portionGrams = item.grams.replace(',', '.').toIntOrNull(),
        catalogFoodId = foodId,
        catalogRelease = catalogRelease.takeIf { foodId != null },
    )
})

private fun reviewExitState(
    items: List<MealReviewItemDraft>,
    matchIds: Map<String, Long>,
    includedPossibleIds: Set<String>,
    title: String,
    date: String,
    mealSlot: String?,
    keepReference: Boolean,
    dishMemoryChoices: Map<String, Boolean>,
    acceptedDishIds: Map<String, String>,
) = MealReviewDraftState(
    items = items.map { item ->
        MealReviewItemState(
            id = item.id,
            dishId = item.dishId,
            dishName = item.dishName,
            name = item.name,
            description = item.description,
            grams = item.grams,
            preparation = item.preparation,
            role = item.role,
            evidence = item.evidence,
        )
    },
    matchIds = matchIds,
    includedPossibleIds = includedPossibleIds,
    title = title,
    date = date,
    mealSlot = mealSlot,
    keepReference = keepReference,
    dishMemoryChoices = dishMemoryChoices,
    acceptedSuggestionIds = acceptedDishIds,
)

private fun List<MealReviewItemDraft>.dishGroups(): List<DishGroup> {
    val order = linkedSetOf<String>()
    forEach { order += it.dishId }
    return order.mapNotNull { dishId ->
        filter { it.dishId == dishId }.takeIf(List<MealReviewItemDraft>::isNotEmpty)?.let { dishItems ->
            DishGroup(dishId, dishItems.first().dishName.ifBlank { dishItems.first().name.ifBlank { "Unnamed dish" } }, dishItems)
        }
    }
}

private fun applyPersonalDishSuggestion(
    current: DishGroup,
    match: PersonalDishMatch,
    selectedMatchIds: Map<String, Long>,
    catalog: OfflineFoodCatalog?,
): AppliedPersonalDish {
    val saved = match.dish
    val photoContextOnly = PersonalDishMatchReason.SAME_PHOTO_CONTEXT in match.reasons &&
        match.reasons.none { reason ->
            reason != PersonalDishMatchReason.SAME_PHOTO_CONTEXT &&
                reason != PersonalDishMatchReason.REPEATED_CONFIRMATIONS
        }

    // A whole-photo match means the model's provisional label may be wrong
    // (for example, "Cutlet" for the member's saved "San Jacobo"). Reusing
    // that label would double-count the mistaken observation alongside the
    // confirmed dish structure. Replace it, but mark every restored component
    // as possible-inside so the member still reviews today's serving.
    if (photoContextOnly) {
        val replacementMatches = linkedMapOf<String, Long>()
        val replacementItems = saved.items.take(12).map { savedItem ->
            val id = UUID.randomUUID().toString()
            savedItem.catalogFoodId
                ?.takeIf { foodId -> catalog?.foods?.any { it.fdcId == foodId } == true }
                ?.let { replacementMatches[id] = it }
            MealReviewItemDraft(
                id = id,
                dishId = current.id,
                dishName = saved.name,
                name = savedItem.name,
                description = savedItem.description.orEmpty(),
                grams = savedItem.grams?.toString().orEmpty(),
                preparation = savedItem.preparation.orEmpty(),
                role = savedItem.role,
                evidence = "possible_inside",
            )
        }
        return AppliedPersonalDish(replacementItems, replacementMatches)
    }

    val usedSavedItems = mutableSetOf<String>()
    val updatedMatches = linkedMapOf<String, Long>()
    val updatedItems = current.items.map { item ->
        val currentFoodId = selectedMatchIds[item.id]
        val savedItem = saved.items.firstOrNull { candidate ->
            candidate.catalogFoodId != null && candidate.catalogFoodId == currentFoodId
        } ?: saved.items.firstOrNull { candidate ->
            PersonalDishMatcher.identityKey(candidate.name) == PersonalDishMatcher.identityKey(item.name)
        }
        if (savedItem != null) usedSavedItems += savedItemIdentity(savedItem)
        (currentFoodId ?: savedItem?.catalogFoodId)?.let { updatedMatches[item.id] = it }
        item.copy(
            dishName = saved.name,
            description = item.description.ifBlank { savedItem?.description.orEmpty() },
            preparation = item.preparation.ifBlank { savedItem?.preparation.orEmpty() },
        )
    }.toMutableList()

    saved.items.filterNot { savedItemIdentity(it) in usedSavedItems }.forEach { savedItem ->
        if (updatedItems.size >= 12) return@forEach
        val id = UUID.randomUUID().toString()
        updatedItems += MealReviewItemDraft(
            id = id,
            dishId = current.id,
            dishName = saved.name,
            name = savedItem.name,
            description = savedItem.description.orEmpty(),
            grams = savedItem.grams?.toString().orEmpty(),
            preparation = savedItem.preparation.orEmpty(),
            role = savedItem.role,
            evidence = "possible_inside",
        )
        savedItem.catalogFoodId?.takeIf { foodId -> catalog?.foods?.any { it.fdcId == foodId } == true }
            ?.let { updatedMatches[id] = it }
    }
    return AppliedPersonalDish(updatedItems, updatedMatches)
}

private fun savedItemIdentity(item: MealEntryItem): String =
    item.catalogFoodId?.let { "food:$it" } ?: "name:${PersonalDishMatcher.identityKey(item.name)}"

private fun mealInstantForReview(date: String, localTime: LocalTime): Instant? =
    runCatching { LocalDate.parse(date).atTime(localTime).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()

private fun nutritionLine(nutrition: CatalogNutrition): String = listOfNotNull(
    nutrition.caloriesKcal?.let { "${"%.0f".format(it)} kcal" },
    nutrition.proteinGrams?.let { "${"%.1f".format(it)} g protein" },
    nutrition.carbohydrateGrams?.let { "${"%.1f".format(it)} g carbs" },
    nutrition.fatGrams?.let { "${"%.1f".format(it)} g fat" },
).joinToString(" · ")

/**
 * Approved redesign: the first level is dish-oriented and glanceable. Editing one
 * dish happens in a focused draft; only the persistent meal action writes to the vault.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetriaMealReviewSheet(
    proposal: MealProposal,
    initialPortionOverride: ChatPortionOverride? = null,
    manualEntry: Boolean = false,
    initialMealSlot: String? = null,
    initialRecordedDate: LocalDate? = null,
    initialSavedDishPrefill: app.metria.CompanionAssistantPolicy.SavedDishMealPrefill? = null,
    catalog: OfflineFoodCatalog?,
    corrections: List<FoodCorrection>,
    referenceThumbnailBase64: String?,
    analysisAuditId: String?,
    savedMeals: List<MealEntry>,
    savedPersonalDishes: List<PersonalDish>,
    findPersonalDishMatches: (PersonalDishQuery) -> List<PersonalDishMatch>,
    foodNameCorrection: MealFoodNameCorrectionStatus,
    onSuggestFoodName: (String, String) -> MealFoodNameCorrectionStatus,
    onCancelFoodNameCorrection: (Long) -> Unit,
    onClearFoodNameCorrection: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (MealEntry, List<FoodCorrection>, List<PersonalDish>, MealCorrectionMemory?) -> String?,
) {
    val draftKey = 31 * (31 * proposal.items.hashCode() + (initialSavedDishPrefill?.hashCode() ?: 0)) +
        (initialPortionOverride?.hashCode() ?: 0)
    val proposalItems = remember(proposal, catalog, initialSavedDishPrefill) {
        if (initialSavedDishPrefill == null) reviewProposalItems(proposal, catalog) else emptyList()
    }
    // Keep the baseline IDs across configuration recreation; otherwise every
    // restored item would look removed and re-added to the learning diff.
    val initialItems = rememberSaveable(draftKey, saver = MealReviewItemsSaver) {
        initialSavedDishPrefill?.let(::savedDishInitialDrafts)
            ?: initialDraftsWithPortionOverride(proposalItems, proposal, initialPortionOverride)
    }
    var items by rememberSaveable(draftKey, stateSaver = MealReviewItemsSaver) {
        mutableStateOf(initialItems)
    }
    val initialMatchIds = rememberSaveable(draftKey, saver = MatchIdsSaver) {
        initialItems.mapIndexedNotNull { index, item ->
            if (FoodCatalog.isAmbiguousModelFood(item.name)) null else {
                val savedItem = initialSavedDishPrefill?.items?.getOrNull(index)
                val savedId = savedItem?.catalogFoodId?.takeIf {
                    savedItem.catalogRelease != null && savedItem.catalogRelease == catalog?.sourceRelease
                }
                val foodId = savedId?.takeIf { id -> catalog?.foods?.any { it.fdcId == id } == true }
                    ?: catalog?.let { FoodCatalog.suggest(it, item.name, item.description, corrections)?.fdcId }
                foodId?.let { item.id to it }
            }
        }.toMap()
    }
    var matchIds by rememberSaveable(draftKey, stateSaver = MatchIdsSaver) {
        mutableStateOf(initialMatchIds)
    }
    var includedPossibleIds by rememberSaveable(draftKey, stateSaver = StringSetSaver) {
        mutableStateOf(emptySet())
    }
    val captureTimeText = rememberSaveable(draftKey) {
        LocalTime.now().withSecond(0).withNano(0).toString()
    }
    val captureTime = remember(captureTimeText) { LocalTime.parse(captureTimeText) }
    val initialDate = rememberSaveable(draftKey, initialRecordedDate) { (initialRecordedDate ?: LocalDate.now()).toString() }
    var date by rememberSaveable(draftKey, initialRecordedDate) { mutableStateOf(initialDate) }
    val initialTitle = rememberSaveable(draftKey) {
        initialSavedDishPrefill?.title ?: MealIdentity.titleFor(proposalItems.map { MealEntryItem(it.name, null, null) })
    }
    var title by rememberSaveable(draftKey) { mutableStateOf(initialTitle) }
    var titleEdited by rememberSaveable(draftKey) { mutableStateOf(false) }
    var dishMemoryChoices by rememberSaveable(draftKey, stateSaver = BooleanMapSaver) {
        mutableStateOf(emptyMap())
    }
    var rejectedSuggestionIds by rememberSaveable(draftKey, stateSaver = StringSetSaver) {
        mutableStateOf(emptySet())
    }
    var acceptedDishIds by rememberSaveable(draftKey, stateSaver = StringMapSaver) {
        mutableStateOf(emptyMap())
    }
    var mealSlot by rememberSaveable(draftKey) { mutableStateOf(initialMealSlot?.takeIf { it in MealSlots.allowed }) }
    var keepReference by rememberSaveable(draftKey) { mutableStateOf(referenceThumbnailBase64 != null) }
    var detailsExpanded by rememberSaveable(draftKey) { mutableStateOf(false) }
    var editingDishId by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    var error by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    var showLeaveConfirmation by rememberSaveable(draftKey) { mutableStateOf(false) }

    val initialExitState by rememberSaveable(draftKey, initialRecordedDate, stateSaver = MealReviewDraftStateSaver) {
        mutableStateOf(
            reviewExitState(
                items = initialItems,
                matchIds = initialMatchIds,
                includedPossibleIds = emptySet(),
                title = initialTitle,
                date = initialDate,
                mealSlot = initialMealSlot?.takeIf { it in MealSlots.allowed },
                keepReference = referenceThumbnailBase64 != null,
                dishMemoryChoices = emptyMap(),
                acceptedDishIds = emptyMap(),
            ),
        )
    }
    val currentExitState = reviewExitState(
        items = items,
        matchIds = matchIds,
        includedPossibleIds = includedPossibleIds,
        title = title,
        date = date,
        mealSlot = mealSlot,
        keepReference = keepReference,
        dishMemoryChoices = dishMemoryChoices,
        acceptedDishIds = acceptedDishIds,
    )
    val hasSaveRelevantChanges = MealReviewExitPolicy.requiresConfirmation(initialExitState, currentExitState)

    val foodsById = remember(catalog) { catalog?.foods?.associateBy(CatalogFood::fdcId).orEmpty() }
    val catalogMatches = items.associate { item -> item.id to matchIds[item.id]?.let(foodsById::get) }
    val parsedGrams = items.associate { item -> item.id to item.grams.replace(',', '.').toIntOrNull()?.takeIf { it in 1..2_000 } }
    val perItemNutrition = items.associate { item ->
        item.id to catalogMatches[item.id]?.let { food -> parsedGrams[item.id]?.let { FoodCatalog.calculate(food, it) } }
    }
    val nonMeasuredIds = items.filter { FoodCatalog.isNonMeasuredSeasoning(it.name) }.mapTo(mutableSetOf()) { it.id }
    val includedIds = items.filter { item ->
        val nutrition = perItemNutrition[item.id]
        (item.evidence == "seen" || item.id in includedPossibleIds) &&
            nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
            nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
    }.mapTo(linkedSetOf()) { it.id }
    val totals = includedIds.takeIf(Set<String>::isNotEmpty)?.mapNotNull(perItemNutrition::get)?.let { values ->
        CatalogNutrition(
            values.sumOf { it.caloriesKcal!! },
            values.sumOf { it.proteinGrams!! },
            values.sumOf { it.carbohydrateGrams!! },
            values.sumOf { it.fatGrams!! },
            values.any { it.caloriesDerivedFromMacros },
        )
    }
    val needsReviewIds = items.filter { item ->
        val nutrition = perItemNutrition[item.id]
        item.id !in nonMeasuredIds && (
            item.evidence == "possible_inside" && item.id !in includedPossibleIds ||
                catalogMatches[item.id] == null || parsedGrams[item.id] == null ||
                nutrition?.caloriesKcal == null || nutrition.proteinGrams == null ||
                nutrition.carbohydrateGrams == null || nutrition.fatGrams == null
            )
    }.mapTo(linkedSetOf()) { it.id }
    val estimateCoverage = MealReviewMode.estimateCoverage(proposal, includedIds.size, items.size)
    val isPartialEstimate = totals != null && estimateCoverage == "partial"
    val comparableItems = items.map { item ->
        MealEntryItem(
            name = item.name.trim(),
            grams = parsedGrams[item.id],
            preparation = item.preparation.trim().takeIf(String::isNotEmpty),
            catalogFoodId = catalogMatches[item.id]?.fdcId.takeIf { item.id in includedIds },
            groupLabel = item.dishName.trim().takeIf(String::isNotEmpty),
        )
    }
    val similarMeal = MealIdentity.similarSavedMeal(comparableItems, savedMeals)
    val dishGroups = items.dishGroups()
    val personalDishQueries = dishGroups.associate { dish ->
        val queryItems = dish.items.map { item ->
            MealEntryItem(
                name = item.name.trim(),
                grams = parsedGrams[item.id],
                preparation = item.preparation.trim().takeIf(String::isNotEmpty),
                catalogFoodId = catalogMatches[item.id]?.fdcId,
                catalogRelease = catalogMatches[item.id]?.let { catalog?.sourceRelease },
                description = item.description.trim().takeIf(String::isNotEmpty),
                role = item.role,
            )
        }
        dish.id to PersonalDishQuery(dish.name, queryItems)
    }
    val semanticMatches = personalDishQueries.mapValues { (_, query) ->
        findPersonalDishMatches(query).filter { match ->
            match.dish.id !in rejectedSuggestionIds &&
                PersonalDishMatchReason.SAME_PHOTO_CONTEXT !in match.reasons
        }
    }
    // The whole-meal thumbnail is only a fallback for one unresolved dish.
    // Showing it on every card would imply dish-level visual recognition that
    // Metria does not yet have.
    val photoFallbackDishId = dishGroups.firstOrNull { dish ->
        acceptedDishIds[dish.id] == null && semanticMatches[dish.id].isNullOrEmpty()
    }?.id
    val personalDishSuggestions = dishGroups.associate { dish ->
        val matches = when {
            acceptedDishIds[dish.id] != null -> emptyList()
            !semanticMatches[dish.id].isNullOrEmpty() -> semanticMatches.getValue(dish.id)
            dish.id == photoFallbackDishId && referenceThumbnailBase64 != null -> {
                val query = personalDishQueries.getValue(dish.id).copy(referenceThumbnailBase64 = referenceThumbnailBase64)
                findPersonalDishMatches(query).filter { match ->
                    match.dish.id !in rejectedSuggestionIds &&
                        PersonalDishMatchReason.SAME_PHOTO_CONTEXT in match.reasons
                }
            }
            else -> emptyList()
        }
        dish.id to matches.take(3)
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    fun closeDishEditor() {
        onClearFoodNameCorrection()
        editingDishId = null
    }
    fun requestLeaveReview() {
        if (hasSaveRelevantChanges) showLeaveConfirmation = true else onDismiss()
    }

    // The full-screen review has no draggable/hidden state. While the keyboard
    // is visible Android owns Back and dismisses only the IME; after that, Back
    // navigates editor -> review -> analysis result.
    BackHandler(enabled = !imeVisible) {
        if (editingDishId != null) closeDishEditor() else requestLeaveReview()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
        ),
    ) {
        Surface(
            color = MetriaColors.Canvas,
            modifier = Modifier.fillMaxSize(),
        ) {
            val editingDish = items.dishGroups().firstOrNull { it.id == editingDishId }
            if (showLeaveConfirmation && editingDish == null) {
                AlertDialog(
                    onDismissRequest = { showLeaveConfirmation = false },
                    title = { Text("Leave without saving?") },
                    text = { Text("Your changes to this meal will be lost.") },
                    dismissButton = {
                        TextButton(onClick = {
                            showLeaveConfirmation = false
                            onDismiss()
                        }) { Text("Leave without saving") }
                    },
                    confirmButton = {
                        Button(onClick = { showLeaveConfirmation = false }) { Text("Keep reviewing") }
                    },
                )
            }
            if (editingDish != null) {
                DishEditor(
                    dish = editingDish,
                    otherDishes = items.dishGroups().filterNot { it.id == editingDish.id },
                    catalog = catalog,
                    corrections = corrections,
                    selectedMatchIds = matchIds.filterKeys { id -> editingDish.items.any { it.id == id } },
                    includedPossibleIds = includedPossibleIds.intersect(editingDish.items.map { it.id }.toSet()),
                    rememberDish = dishMemoryChoices[editingDish.id] ?: true,
                    canRemoveLastComponent = items.size > editingDish.items.size,
                    foodNameCorrection = foodNameCorrection,
                    onSuggestFoodName = onSuggestFoodName,
                    onCancelFoodNameCorrection = onCancelFoodNameCorrection,
                    onClearFoodNameCorrection = onClearFoodNameCorrection,
                    onCancel = ::closeDishEditor,
                    onApply = { updatedItems, updatedMatches, updatedIncluded, rememberDish ->
                        onClearFoodNameCorrection()
                        val oldIds = editingDish.items.mapTo(mutableSetOf()) { it.id }
                        val insertionIndex = items.indexOfFirst { it.dishId == editingDish.id }.coerceAtLeast(0)
                        items = items.filterNot { it.dishId == editingDish.id }.toMutableList().also {
                            it.addAll(insertionIndex.coerceAtMost(it.size), updatedItems)
                        }
                        matchIds = matchIds.filterKeys { it !in oldIds } + updatedMatches
                        includedPossibleIds = includedPossibleIds.filterNotTo(mutableSetOf()) { it in oldIds } + updatedIncluded
                        dishMemoryChoices = if (updatedItems.any { it.dishId == editingDish.id }) {
                            dishMemoryChoices + (editingDish.id to rememberDish)
                        } else {
                            dishMemoryChoices - editingDish.id
                        }
                        editingDishId = null
                        error = null
                    },
                )
            } else {
                MealReview(
                proposal = proposal,
                manualEntry = manualEntry,
                items = items,
                title = title,
                onTitleChange = {
                    title = it
                    titleEdited = true
                },
                rememberedDishNames = items.dishGroups().filter { dishMemoryChoices[it.id] == true }.map { it.name },
                personalDishSuggestions = personalDishSuggestions,
                acceptedDishIds = acceptedDishIds,
                referenceThumbnailBase64 = referenceThumbnailBase64,
                keepReference = keepReference,
                onKeepReferenceChange = { keepReference = it },
                similarMeal = similarMeal,
                totals = totals,
                onScalePortions = { factor ->
                    val scaled = app.metria.MealPortionScale.apply(items.map { it.grams }, factor)
                    if (scaled == null) false else {
                        items = items.mapIndexed { index, item -> item.copy(grams = scaled[index]) }
                        includedPossibleIds = emptySet()
                        error = null
                        true
                    }
                },
                nutrientDetails = app.metria.CatalogNutrientDetails.calculate(items.filter {
                    it.evidence == "seen" || it.id in includedPossibleIds
                }.map { catalogMatches[it.id] to parsedGrams[it.id] }),
                isPartialEstimate = isPartialEstimate,
                includedIds = includedIds,
                needsReviewIds = needsReviewIds,
                catalog = catalog,
                date = date,
                onDateChange = { date = it },
                mealSlot = mealSlot,
                suggestedMealSlot = MealSlots.suggestedFor(captureTime),
                onMealSlotChange = { mealSlot = it },
                detailsExpanded = detailsExpanded,
                onDetailsExpandedChange = { detailsExpanded = it },
                error = error,
                onEditDish = { editingDishId = it },
                onAcceptPersonalDish = { dishId, match ->
                    val currentDish = items.dishGroups().firstOrNull { it.id == dishId }
                    if (currentDish != null) {
                        val applied = applyPersonalDishSuggestion(currentDish, match, matchIds, catalog)
                        val oldIds = currentDish.items.mapTo(mutableSetOf()) { it.id }
                        val insertionIndex = items.indexOfFirst { it.dishId == dishId }.coerceAtLeast(0)
                        items = items.filterNot { it.dishId == dishId }.toMutableList().also { updated ->
                            updated.addAll(insertionIndex.coerceAtMost(updated.size), applied.items)
                        }
                        if (!titleEdited) {
                            title = items.dishGroups().map(DishGroup::name).filter(String::isNotBlank)
                                .distinct().take(3).joinToString(" and ").ifBlank { "Meal" }
                        }
                        matchIds = matchIds.filterKeys { it !in oldIds } + applied.matchIds
                        acceptedDishIds = acceptedDishIds + (dishId to match.dish.id)
                        dishMemoryChoices = dishMemoryChoices + (dishId to true)
                        error = null
                    }
                },
                onRejectPersonalDish = { match -> rejectedSuggestionIds = rejectedSuggestionIds + match.dish.id },
                onAddDish = {
                    if (items.size < 12) {
                        val dishId = UUID.randomUUID().toString()
                        items = items + MealReviewItemDraft(
                            id = UUID.randomUUID().toString(), dishId = dishId, dishName = "New dish",
                            name = "", description = "", grams = "", preparation = "",
                            role = "main_food", evidence = "seen",
                        )
                        editingDishId = dishId
                    }
                },
                onUseSimilarName = { previous ->
                    title = MealIdentity.displayTitle(previous)
                    titleEdited = true
                },
                onDismiss = ::requestLeaveReview,
                    onSave = {
                    val recordedAt = mealInstantForReview(date, captureTime)
                    val savedItems = items.map { item ->
                        MealEntryItem(
                            name = item.name.trim(),
                            grams = parsedGrams[item.id],
                            preparation = item.preparation.trim().takeIf(String::isNotEmpty),
                            catalogFoodId = catalogMatches[item.id]?.fdcId.takeIf { item.id in includedIds },
                            catalogRelease = catalog?.sourceRelease?.takeIf { item.id in includedIds },
                            groupLabel = item.dishName.trim().takeIf(String::isNotEmpty),
                            description = item.description.trim().takeIf(String::isNotEmpty),
                            role = item.role,
                        )
                    }
                    val nutrition = totals?.let {
                        MealNutrition(
                            it.caloriesKcal, it.proteinGrams, it.carbohydrateGrams, it.fatGrams,
                            source = "member_confirmed_catalog",
                            catalogRelease = catalog?.sourceRelease,
                            estimateCoverage = estimateCoverage,
                        )
                    }
                    val meal = recordedAt?.let {
                        MealEntry(
                            recordedAt = it,
                            title = title.trim().takeIf(String::isNotEmpty),
                            mealSlot = mealSlot,
                            recordedTimeKnown = true,
                            items = savedItems,
                            assumptions = proposal.assumptions,
                            confidence = if (manualEntry) "high" else proposal.overallConfidence,
                            source = MealReviewMode.source(manualEntry, nutrition != null),
                            nutrition = nutrition,
                            referenceThumbnailBase64 = referenceThumbnailBase64.takeIf { keepReference },
                            analysisAuditId = analysisAuditId,
                        )
                    }
                    val learnedCorrections = items.mapNotNull { item ->
                        catalogMatches[item.id]?.takeIf { it.fdcId != initialMatchIds[item.id] }
                            ?.let { FoodCorrection(item.name.trim(), it.fdcId) }
                    }
                    val savedItemsByDraftId = items.mapIndexed { index, draft -> draft.id to savedItems[index] }.toMap()
                    val selectedDishGroups = items.dishGroups().filter { dishMemoryChoices[it.id] == true }
                    val personalDishes = meal?.let { savedMeal ->
                        selectedDishGroups.mapNotNull { dish ->
                            val dishItems = dish.items.mapNotNull { savedItemsByDraftId[it.id] }
                            PersonalDishValidation.fromConfirmedMeal(
                                savedMeal.copy(title = dish.name, items = dishItems),
                                dish.name,
                                existing = acceptedDishIds[dish.id]?.let { acceptedId ->
                                    savedPersonalDishes.firstOrNull { it.id == acceptedId }
                                },
                            )
                        }
                    }.orEmpty()
                    if (meal == null || !MealEntryValidation.valid(meal)) {
                        error = "Review each food name, portion and the meal date before saving."
                    } else if (personalDishes.size != selectedDishGroups.size) {
                        error = "Review the name and foods in each dish you want Metria to remember."
                    } else {
                        val correctionMemory = if (manualEntry) null else {
                            val diff = MealCorrectionDiff.diff(
                                correctionSnapshot(initialItems, initialMatchIds, catalog?.sourceRelease),
                                correctionSnapshot(items, matchIds, catalog?.sourceRelease),
                            )
                            (diff as? MealCorrectionDiffResult.Valid)?.changes
                                ?.takeIf(List<app.metria.MealCorrectionChange>::isNotEmpty)
                                ?.let { MealCorrectionMemory(meal.id, meal.recordedAt, it) }
                                ?.let(MealCorrectionMemoryPolicy::normalized)
                        }
                        error = onSave(meal, learnedCorrections, personalDishes, correctionMemory)
                    }
                    },
                )
            }
        }
    }
}

@Composable
private fun MealReview(
    proposal: MealProposal,
    manualEntry: Boolean,
    items: List<MealReviewItemDraft>,
    title: String,
    onTitleChange: (String) -> Unit,
    rememberedDishNames: List<String>,
    personalDishSuggestions: Map<String, List<PersonalDishMatch>>,
    acceptedDishIds: Map<String, String>,
    referenceThumbnailBase64: String?,
    keepReference: Boolean,
    onKeepReferenceChange: (Boolean) -> Unit,
    similarMeal: MealEntry?,
    totals: CatalogNutrition?,
    onScalePortions: (Double) -> Boolean,
    isPartialEstimate: Boolean,
    nutrientDetails: List<app.metria.CatalogNutrientAmount>,
    includedIds: Set<String>,
    needsReviewIds: Set<String>,
    catalog: OfflineFoodCatalog?,
    date: String,
    onDateChange: (String) -> Unit,
    mealSlot: String?,
    suggestedMealSlot: String,
    onMealSlotChange: (String?) -> Unit,
    detailsExpanded: Boolean,
    onDetailsExpandedChange: (Boolean) -> Unit,
    error: String?,
    onEditDish: (String) -> Unit,
    onAcceptPersonalDish: (String, PersonalDishMatch) -> Unit,
    onRejectPersonalDish: (PersonalDishMatch) -> Unit,
    onAddDish: () -> Unit,
    onUseSimilarName: (MealEntry) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        ReviewHeader(title = if (manualEntry) "Enter meal manually" else "Review this meal", onClose = onDismiss)
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ReviewIntro(
                    referenceThumbnailBase64 = referenceThumbnailBase64,
                    confidence = proposal.overallConfidence,
                    dishCount = items.dishGroups().size,
                    manualEntry = manualEntry,
                )
            }
            if (!manualEntry && proposal.omittedItemCount > 0) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.EstimatedSoft)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Some suggestions were left out", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (items.size >= 12) "Check the photo before saving. To add a missing food, first remove one of the current foods."
                                else "Check the photo and add any missing foods before saving. Only the foods shown here are in this draft.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MetriaColors.InkMuted,
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Meal name") },
                    supportingText = { Text("Name it once. Foods stay inside their dishes.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            similarMeal?.let { previous ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Looks similar to ${MealIdentity.displayTitle(previous)}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "This uses the meal name only. Review today’s foods and portions separately.",
                                color = MetriaColors.InkMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { onUseSimilarName(previous) }) { Text("Use meal name") }
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Dishes", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (items.isEmpty()) "Add a dish and the foods you recognise."
                            else "Tap a dish to review its foods.",
                            color = MetriaColors.InkMuted,
                        )
                    }
                    if (items.size < 12) TextButton(onClick = onAddDish) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add dish")
                    }
                }
            }
            items.dishGroups().forEach { dish ->
                item(key = dish.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DishReviewCard(
                            dish = dish,
                            includedCount = dish.items.count { it.id in includedIds },
                            needsReviewCount = dish.items.count { it.id in needsReviewIds },
                            remembered = dish.name in rememberedDishNames,
                            usingSavedDish = dish.id in acceptedDishIds,
                            onClick = { onEditDish(dish.id) },
                        )
                        personalDishSuggestions[dish.id]?.takeIf(List<PersonalDishMatch>::isNotEmpty)?.let { matches ->
                            PersonalDishSuggestionsCard(
                                dishName = dish.name,
                                matches = matches,
                                onAccept = { match -> onAcceptPersonalDish(dish.id, match) },
                                onRejectAll = { matches.forEach(onRejectPersonalDish) },
                            )
                        }
                    }
                }
            }
            item {
                NutritionReviewCard(
                    totals = totals,
                    isPartial = isPartialEstimate,
                    includedItems = items.filter { it.id in includedIds }.map { it.name },
                    excludedItems = items.filter { it.id in needsReviewIds }.map { it.name.ifBlank { "Unnamed food" } },
                    catalog = catalog,
                )
                app.metria.ui.components.CatalogNutrientDetailsCard(nutrientDetails, catalog?.sourceRelease)
                app.metria.ui.components.MealPortionControl(onScalePortions)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onDetailsExpandedChange(!detailsExpanded) }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Meal details", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${MealSlots.label(mealSlot)} · $date",
                                    color = MetriaColors.InkMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = if (detailsExpanded) "Hide meal details" else "Show meal details",
                                modifier = Modifier.semantics { contentDescription = if (detailsExpanded) "Meal details expanded" else "Meal details collapsed" },
                            )
                        }
                        if (detailsExpanded) {
                            HorizontalDivider(color = MetriaColors.Divider)
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = onDateChange,
                                    label = { Text("Date · YYYY-MM-DD") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                MealSlotChips(mealSlot, suggestedMealSlot, onMealSlotChange)
                                referenceThumbnailBase64?.let {
                                    SettingSwitchRow(
                                        checked = keepReference,
                                        onCheckedChange = onKeepReferenceChange,
                                        title = "Keep a private thumbnail",
                                        supporting = "Encrypted on this phone; the original photo is discarded.",
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Surface(
                    color = if (rememberedDishNames.isEmpty()) MetriaColors.SurfaceSubtle else MetriaColors.PrimarySoft,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            if (rememberedDishNames.isEmpty()) "Want Metria to remember a dish?" else "Remembering privately",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (rememberedDishNames.isEmpty()) "Open a dish and keep “Remember this dish” on when you apply your correction."
                            else rememberedDishNames.joinToString(),
                            color = MetriaColors.InkMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            error?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        SystemSafeBottomActions(verticalPadding = 14.dp) {
            Button(
                onClick = if (MealReviewMode.needsFirstDish(manualEntry, items.size)) onAddDish else onSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(if (MealReviewMode.needsFirstDish(manualEntry, items.size)) "Add first dish" else "Save reviewed meal")
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Leave without saving")
            }
        }
    }
}

@Composable
private fun ReviewHeader(title: String, onClose: () -> Unit) {
    Surface(color = MetriaColors.Canvas) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun ReviewIntro(referenceThumbnailBase64: String?, confidence: String, dishCount: Int, manualEntry: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (referenceThumbnailBase64 != null) {
            MealReviewThumbnail(referenceThumbnailBase64, Modifier.size(92.dp))
        } else {
            Surface(color = MetriaColors.AccentSoft, shape = MaterialTheme.shapes.medium, modifier = Modifier.size(92.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$dishCount", style = MaterialTheme.typography.displaySmall)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (manualEntry) Text("Manual entry", style = MaterialTheme.typography.titleSmall)
            else MetriaEstimateBadge("AI estimate · ${confidence.replaceFirstChar(Char::uppercase)} confidence")
            Text(
                when {
                    manualEntry -> "No foods were taken from the rejected draft. Add and review them before saving."
                    dishCount == 1 -> "Metria found one dish. Check the foods and portions before saving."
                    else -> "Metria found $dishCount dishes. Check each one before saving."
                },
                color = MetriaColors.InkMuted,
            )
        }
    }
}

@Composable
private fun PersonalDishSuggestionsCard(
    dishName: String,
    matches: List<PersonalDishMatch>,
    onAccept: (PersonalDishMatch) -> Unit,
    onRejectAll: () -> Unit,
) {
    val photoContextOnly = matches.all { match ->
        match.reasons.none { it != PersonalDishMatchReason.SAME_PHOTO_CONTEXT && it != PersonalDishMatchReason.REPEATED_CONFIRMATIONS }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.AccentSoft)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Suggestion for $dishName",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MetriaColors.Primary)
                Text(
                    if (matches.size == 1) "Could this be your ${matches.first().dish.name}?" else "Is this one of your saved dishes?",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                if (photoContextOnly) "Metria recognises the same meal photo, not the individual dish. Choose only if it is correct."
                else "These foods or names resemble something you confirmed before.",
                color = MetriaColors.InkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            matches.forEach { match ->
                OutlinedButton(onClick = { onAccept(match) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use ${match.dish.name}")
                }
            }
            TextButton(onClick = onRejectAll) { Text("None of these") }
        }
    }
}

@Composable
private fun DishReviewCard(
    dish: DishGroup,
    includedCount: Int,
    needsReviewCount: Int,
    remembered: Boolean,
    usingSavedDish: Boolean,
    onClick: () -> Unit,
) {
    val grams = dish.items.mapNotNull { it.grams.toIntOrNull() }.sum().takeIf { it > 0 }
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${dish.name}, ${dish.items.size} foods, tap to review"
        }.clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MetriaColors.AccentSoft, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(dish.items.size.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(dish.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        dish.items.joinToString { it.name.ifBlank { "Unnamed food" } },
                        color = MetriaColors.InkMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                    if (remembered) Text(
                        if (usingSavedDish) "Using your saved dish · review changes" else "Will remember privately",
                        color = MetriaColors.Primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(Icons.Default.Edit, contentDescription = "Edit ${dish.name}", tint = MetriaColors.Primary)
            }
            HorizontalDivider(color = MetriaColors.Divider)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(grams?.let { "About $it g" } ?: "Portion needs review", color = MetriaColors.InkMuted)
                Text(
                    when {
                        needsReviewCount > 0 -> "$needsReviewCount to review"
                        includedCount == dish.items.size -> "Ready"
                        else -> "$includedCount included"
                    },
                    color = if (needsReviewCount > 0) MetriaColors.Estimated else MetriaColors.Verified,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun NutritionReviewCard(
    totals: CatalogNutrition?,
    isPartial: Boolean,
    includedItems: List<String>,
    excludedItems: List<String>,
    catalog: OfflineFoodCatalog?,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nutrition estimate", style = MaterialTheme.typography.titleLarge)
                MetriaEstimateBadge(if (isPartial) "Partial" else if (totals == null) "Needs review" else "Reviewed estimate")
            }
            if (totals == null) {
                Text("Review at least one food and its portion to calculate what Metria can.", color = MetriaColors.InkMuted)
            } else {
                app.metria.ui.components.MealNutritionMetrics(totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams)
                if (isPartial) {
                    Text(
                        "Calculated from ${includedItems.joinToString().ifBlank { "reviewed foods" }}. Not yet included: ${excludedItems.joinToString().ifBlank { "other foods" }}.",
                        color = MetriaColors.InkMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                catalog?.let {
                    Text("Offline food data · ${it.sourceRelease}", color = MetriaColors.Primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    supporting: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(supporting, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FoodNameCorrectionEditor(
    input: String,
    onInputChange: (String) -> Unit,
    isRunning: Boolean,
    anotherFoodRunning: Boolean,
    suggestedName: String?,
    diagnostic: String?,
    originalName: String,
    proposalIsCurrent: Boolean,
    onSuggest: () -> Unit,
    onCancel: () -> Unit,
    onApply: (String) -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        color = MetriaColors.PrimarySoft,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Correct this food name", style = MaterialTheme.typography.titleSmall)
            Text("Describe the correction. Metria will suggest a name for this food only.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("For example: it's albóndigas, not fried balls") },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
            )
            if (isRunning) {
                Text("Considering your correction on this phone…", color = MetriaColors.Primary, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel suggestion") }
            } else if (suggestedName != null) {
                Text(
                    if (proposalIsCurrent) "$originalName  →  $suggestedName" else "This food name changed while Metria was considering the suggestion.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                    Button(onClick = { onApply(suggestedName) }, enabled = proposalIsCurrent, modifier = Modifier.weight(1f)) { Text("Apply name") }
                }
            } else {
                if (anotherFoodRunning) {
                    Text("Finish the suggestion for ${originalName.ifBlank { "the other food" }} first.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                }
                diagnostic?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = onSuggest,
                    enabled = input.trim().length >= 3 && !anotherFoodRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Suggest a food name") }
            }
        }
    }
}

@Composable
private fun MealSlotChips(selected: String?, suggested: String?, choose: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Meal type", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("breakfast", "lunch", "dinner", "snack").forEach { slot ->
                FilterChip(
                    selected = selected == slot,
                    onClick = { choose(if (selected == slot) null else slot) },
                    label = { Text(MealSlots.label(slot) + if (selected == null && suggested == slot) " · suggested" else "") },
                )
            }
        }
    }
}

@Composable
private fun DishEditor(
    dish: DishGroup,
    otherDishes: List<DishGroup>,
    catalog: OfflineFoodCatalog?,
    corrections: List<FoodCorrection>,
    selectedMatchIds: Map<String, Long>,
    includedPossibleIds: Set<String>,
    rememberDish: Boolean,
    canRemoveLastComponent: Boolean,
    foodNameCorrection: MealFoodNameCorrectionStatus,
    onSuggestFoodName: (String, String) -> MealFoodNameCorrectionStatus,
    onCancelFoodNameCorrection: (Long) -> Unit,
    onClearFoodNameCorrection: () -> Unit,
    onCancel: () -> Unit,
    onApply: (List<MealReviewItemDraft>, Map<String, Long>, Set<String>, Boolean) -> Unit,
) {
    var localItems by rememberSaveable(dish.id, stateSaver = MealReviewItemsSaver) { mutableStateOf(dish.items) }
    var movedItems by rememberSaveable("${dish.id}-moved", stateSaver = MealReviewItemsSaver) { mutableStateOf(emptyList()) }
    var localDishName by rememberSaveable(dish.id) { mutableStateOf(dish.name) }
    var localMatchIds by rememberSaveable(dish.id, stateSaver = MatchIdsSaver) { mutableStateOf(selectedMatchIds) }
    var localIncludedPossible by rememberSaveable(dish.id, stateSaver = StringSetSaver) { mutableStateOf(includedPossibleIds) }
    var localRememberDish by rememberSaveable(dish.id) { mutableStateOf(rememberDish) }
    var expandedItemId by rememberSaveable(dish.id) { mutableStateOf(localItems.firstOrNull()?.id) }
    var catalogSearchItemId by rememberSaveable(dish.id) { mutableStateOf<String?>(null) }
    var editorError by rememberSaveable(dish.id) { mutableStateOf<String?>(null) }
    var correctionText by rememberSaveable(dish.id) { mutableStateOf("") }
    var correctionTargetId by rememberSaveable(dish.id) { mutableStateOf<String?>(null) }
    var correctionOriginalName by rememberSaveable(dish.id) { mutableStateOf("") }
    var correctionRequestId by rememberSaveable(dish.id) { mutableStateOf<Long?>(null) }
    val foodsById = remember(catalog) { catalog?.foods?.associateBy(CatalogFood::fdcId).orEmpty() }

    Column(Modifier.fillMaxSize().imePadding()) {
        ReviewHeader(title = "Edit dish", onClose = onCancel)
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = localDishName,
                    onValueChange = { value ->
                        localDishName = value
                        localItems = localItems.map { it.copy(dishName = value) }
                    },
                    label = { Text("Dish name") },
                    supportingText = { Text("This name applies to the whole dish.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SettingSwitchRow(
                    checked = localRememberDish,
                    onCheckedChange = { localRememberDish = it },
                    title = "Remember this dish",
                    supporting = "Save this corrected dish privately so Metria can suggest it next time.",
                )
            }
            item {
                Text("Foods", style = MaterialTheme.typography.titleLarge)
                Text("Open only the food you want to change.", color = MetriaColors.InkMuted)
            }
            localItems.forEach { item ->
                item(key = item.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComponentEditorCard(
                            item = item,
                            expanded = expandedItemId == item.id,
                            catalogFood = localMatchIds[item.id]?.let(foodsById::get),
                            includedPossible = item.id in localIncludedPossible,
                            canRemove = localItems.size > 1 || canRemoveLastComponent,
                            otherDishes = otherDishes,
                            onToggle = {
                                val nextExpandedId = if (expandedItemId == item.id) null else item.id
                                if (nextExpandedId != item.id && correctionTargetId != nextExpandedId) correctionText = ""
                                expandedItemId = nextExpandedId
                            },
                            onChange = { updated ->
                                if (updated.name != item.name) localMatchIds = localMatchIds - item.id
                                if (updated.evidence == "seen") localIncludedPossible = localIncludedPossible - item.id
                                localItems = localItems.map { if (it.id == item.id) updated else it }
                            },
                            onTogglePossible = {
                                localIncludedPossible = if (item.id in localIncludedPossible) localIncludedPossible - item.id else localIncludedPossible + item.id
                            },
                            onFindFood = { catalogSearchItemId = item.id },
                            onRemove = {
                                if (correctionTargetId == item.id) {
                                    onClearFoodNameCorrection()
                                    correctionRequestId = null
                                    correctionTargetId = null
                                    correctionText = ""
                                }
                                localItems = localItems.filterNot { it.id == item.id }
                                localMatchIds = localMatchIds - item.id
                                localIncludedPossible = localIncludedPossible - item.id
                                if (expandedItemId == item.id) expandedItemId = localItems.firstOrNull()?.id
                            },
                            onMove = { destination ->
                                if (correctionTargetId == item.id) {
                                    onClearFoodNameCorrection()
                                    correctionRequestId = null
                                    correctionTargetId = null
                                    correctionText = ""
                                }
                                movedItems = movedItems + item.copy(dishId = destination.id, dishName = destination.name)
                                localItems = localItems.filterNot { it.id == item.id }
                                if (expandedItemId == item.id) expandedItemId = localItems.firstOrNull()?.id
                            },
                        )
                        if (expandedItemId == item.id) {
                            FoodNameCorrectionEditor(
                                input = correctionText,
                                onInputChange = { correctionText = it.take(240) },
                                isRunning = foodNameCorrection.requestId == correctionRequestId &&
                                    foodNameCorrection.state == "running" && correctionTargetId == item.id,
                                anotherFoodRunning = foodNameCorrection.state == "running" && correctionTargetId != item.id,
                                suggestedName = MealFoodNameCorrectionPresentation.suggestionFor(
                                    foodNameCorrection,
                                    correctionRequestId,
                                    correctionTargetId,
                                    item.id,
                                ),
                                diagnostic = foodNameCorrection.diagnostic.takeIf {
                                    foodNameCorrection.requestId == correctionRequestId && correctionTargetId == item.id
                                },
                                originalName = correctionOriginalName,
                                proposalIsCurrent = item.name == correctionOriginalName,
                                onSuggest = {
                                    correctionTargetId = item.id
                                    correctionOriginalName = item.name
                                    val result = onSuggestFoodName(item.name, correctionText)
                                    correctionRequestId = result.requestId
                                },
                                onCancel = { correctionRequestId?.let(onCancelFoodNameCorrection) },
                                onApply = { name ->
                                    localItems = localItems.map { current ->
                                        if (current.id == item.id) current.copy(name = name) else current
                                    }
                                    localMatchIds = localMatchIds - item.id
                                    correctionText = ""
                                    correctionTargetId = null
                                    correctionRequestId = null
                                    onClearFoodNameCorrection()
                                },
                                onDiscard = {
                                    correctionText = ""
                                    correctionTargetId = null
                                    correctionRequestId = null
                                    onClearFoodNameCorrection()
                                },
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val added = newComponent(dish.id, localDishName, "seen")
                            localItems = localItems + added
                            expandedItemId = added.id
                        },
                        enabled = localItems.size < 12,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Visible food")
                    }
                    OutlinedButton(
                        onClick = {
                            val added = newComponent(dish.id, localDishName, "possible_inside")
                            localItems = localItems + added
                            expandedItemId = added.id
                        },
                        enabled = localItems.size < 12,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("May be inside")
                    }
                }
            }
            editorError?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        SystemSafeBottomActions {
                Button(
                    onClick = {
                        if (localItems.isEmpty() && movedItems.isEmpty() && canRemoveLastComponent) {
                            onApply(emptyList(), emptyMap(), emptySet(), false)
                        } else if (localItems.isEmpty() && movedItems.isEmpty()) {
                            editorError = "Keep at least one food in this meal."
                        } else if (localItems.isNotEmpty() && (localDishName.trim().isEmpty() || localItems.any { it.name.trim().isEmpty() })) {
                            editorError = "Give the dish and each food a short name before continuing."
                        } else {
                            onApply(localItems + movedItems, localMatchIds, localIncludedPossible, localRememberDish)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(when {
                        localItems.isEmpty() && movedItems.isEmpty() -> "Remove dish"
                        movedItems.isNotEmpty() -> "Apply moves and changes"
                        else -> "Apply dish changes"
                    })
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel changes") }
        }
    }

    catalogSearchItemId?.let { itemId ->
        val item = localItems.firstOrNull { it.id == itemId }
        CatalogSearchSheet(
            catalog = catalog,
            initialQuery = item?.name.orEmpty(),
            corrections = corrections,
            description = item?.description.orEmpty(),
            onDismiss = { catalogSearchItemId = null },
            onSelect = { food ->
                localMatchIds = localMatchIds + (itemId to food.fdcId)
                catalogSearchItemId = null
            },
        )
    }
}

@Composable
private fun SystemSafeBottomActions(
    verticalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val navigationBottomPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.getSystemService(WindowManager::class.java)
            ?.currentWindowMetrics
            ?.windowInsets
            ?.getInsetsIgnoringVisibility(PlatformWindowInsets.Type.navigationBars())
            ?.bottom
            ?: 0
    } else {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?: 0
    }
    val navigationBottom = with(density) { navigationBottomPixels.toDp() }

    Surface(color = MetriaColors.Surface, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    top = verticalPadding,
                    end = 20.dp,
                    bottom = verticalPadding + navigationBottom,
                ),
            content = content,
        )
    }
}

private fun newComponent(dishId: String, dishName: String, evidence: String) = MealReviewItemDraft(
    id = UUID.randomUUID().toString(),
    dishId = dishId,
    dishName = dishName,
    name = "",
    description = "",
    grams = "",
    preparation = "",
    role = "main_food",
    evidence = evidence,
)

@Composable
private fun ComponentEditorCard(
    item: MealReviewItemDraft,
    expanded: Boolean,
    catalogFood: CatalogFood?,
    includedPossible: Boolean,
    canRemove: Boolean,
    otherDishes: List<DishGroup>,
    onToggle: () -> Unit,
    onChange: (MealReviewItemDraft) -> Unit,
    onTogglePossible: () -> Unit,
    onFindFood: () -> Unit,
    onRemove: () -> Unit,
    onMove: (DishGroup) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = if (item.evidence == "seen") MetriaColors.AccentSoft else MetriaColors.EstimatedSoft,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (catalogFood == null) Icons.Default.Search else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (catalogFood == null) MetriaColors.Estimated else MetriaColors.Verified,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.name.ifBlank { "Unnamed food" }, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(
                            item.grams.toIntOrNull()?.let { "$it g" },
                            if (item.evidence == "seen") "Seen" else "May be inside",
                            catalogFood?.let { "Food selected" } ?: "Needs food match",
                        ).joinToString(" · "),
                        color = MetriaColors.InkMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = if (expanded) "Collapse food" else "Edit food")
            }
            if (expanded) {
                HorizontalDivider(color = MetriaColors.Divider)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { onChange(item.copy(name = it)) },
                        label = { Text("Food") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = item.grams,
                            onValueChange = { onChange(item.copy(grams = it.filter { char -> char.isDigit() })) },
                            label = { Text("Portion") },
                            suffix = { Text("g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = item.preparation,
                            onValueChange = { onChange(item.copy(preparation = it)) },
                            label = { Text("Prepared") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = item.description,
                        onValueChange = { onChange(item.copy(description = it)) },
                        label = { Text("What Metria noticed · optional") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Photo evidence", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = item.evidence == "seen",
                            onClick = { onChange(item.copy(evidence = "seen")) },
                            label = { Text("Visible in photo") },
                        )
                        FilterChip(
                            selected = item.evidence == "possible_inside",
                            onClick = { onChange(item.copy(evidence = "possible_inside")) },
                            label = { Text("May be inside") },
                        )
                    }
                    if (item.evidence == "possible_inside") {
                        SettingSwitchRow(
                            checked = includedPossible,
                            onCheckedChange = { onTogglePossible() },
                            title = "Include in estimate",
                            supporting = "Turn this on only if you confirm this food is inside the dish.",
                        )
                    }
                    Text("Type", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "main_food" to "Main",
                            "side" to "Side",
                            "drink" to "Drink",
                            "sauce_oil" to "Sauce or oil",
                            "dry_seasoning" to "Seasoning",
                        ).forEach { (role, label) ->
                            FilterChip(
                                selected = item.role == role,
                                onClick = { onChange(item.copy(role = role)) },
                                label = { Text(label) },
                            )
                        }
                    }
                    Surface(color = MetriaColors.SurfaceSubtle, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                catalogFood?.let(FoodCatalog::memberDisplayName) ?: "No food selected yet",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                if (catalogFood == null) "Choose a similar offline food to calculate this portion."
                                else "Used only for the nutrition estimate; you can change it.",
                                color = MetriaColors.InkMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = onFindFood) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Text(if (catalogFood == null) "Choose a similar food" else "Change selected food")
                            }
                        }
                    }
                    if (otherDishes.isNotEmpty()) {
                        Text("Move to another dish", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            otherDishes.forEach { destination ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onMove(destination) },
                                    label = { Text(destination.name) },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                )
                            }
                        }
                        Text(
                            "Moves this food without asking you to type it again.",
                            color = MetriaColors.InkMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (canRemove) {
                        TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MetriaColors.Danger)
                            Text("Remove food", color = MetriaColors.Danger)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSearchSheet(
    catalog: OfflineFoodCatalog?,
    initialQuery: String,
    corrections: List<FoodCorrection>,
    description: String,
    onDismiss: () -> Unit,
    onSelect: (CatalogFood) -> Unit,
) {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    val results = remember(catalog, query, description, corrections) {
        catalog?.let {
            val precise = FoodCatalog.candidatesForAssistantItem(it, query, description, corrections, limit = 6)
            if (precise.isNotEmpty()) precise else FoodCatalog.search(it, query, limit = 12)
        }.orEmpty()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MetriaColors.Canvas,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f).imePadding()) {
            ReviewHeader("Choose a similar food", onDismiss)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search food") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (catalog == null) {
                Text(
                    "The offline food list is unavailable. You can still keep this food without nutrition.",
                    color = MetriaColors.Danger,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (query.isNotBlank() && results.isEmpty()) {
                        item {
                            Text("No similar food found. Try a shorter name, such as chicken, ham, or peas.", color = MetriaColors.InkMuted)
                        }
                    }
                    results.forEach { food ->
                        item(key = food.fdcId) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(food) },
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(FoodCatalog.memberDisplayName(food), style = MaterialTheme.typography.titleMedium)
                                    if (FoodCatalog.memberDisplayName(food) != food.description) {
                                        Text(food.description, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Offline Foundation Foods", color = MetriaColors.Primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealReviewThumbnail(encoded: String, modifier: Modifier = Modifier) {
    val bitmap = remember(encoded) {
        runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Private meal reference",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(20.dp)),
        )
    }
}
