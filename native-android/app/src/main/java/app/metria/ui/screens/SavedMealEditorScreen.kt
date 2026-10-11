package app.metria.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.metria.CatalogFood
import app.metria.CatalogNutrition
import app.metria.FoodCatalog
import app.metria.MealEntry
import app.metria.MealEntryItem
import app.metria.MealEntryValidation
import app.metria.MealFoodNameCorrectionPresentation
import app.metria.MealFoodNameCorrectionStatus
import app.metria.MealIdentity
import app.metria.MealNutrition
import app.metria.MealSlots
import app.metria.OfflineFoodCatalog
import app.metria.SavedMealEditPolicy
import app.metria.ui.theme.MetriaColors
import app.metria.ui.components.rememberGuardedEditorSheetState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

private data class EditableSavedFood(
    val name: String,
    val description: String,
    val grams: String,
    val preparation: String,
    val groupLabel: String? = null,
    val role: String = "main_food",
)

private val EditableSavedFoodsSaver = listSaver<List<EditableSavedFood>, String>(
    save = { foods -> foods.flatMap { listOf(it.name, it.description, it.grams, it.preparation, it.groupLabel.orEmpty(), it.role) } },
    restore = { values -> values.chunked(6).filter { it.size == 6 }.map { EditableSavedFood(it[0], it[1], it[2], it[3], it[4].takeIf(String::isNotEmpty), it[5]) } },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMealEditorScreen(
    meal: MealEntry,
    catalog: OfflineFoodCatalog?,
    foodNameCorrection: MealFoodNameCorrectionStatus,
    onSuggestFoodName: (String, String) -> MealFoodNameCorrectionStatus,
    onCancelFoodNameCorrection: (Long) -> Unit,
    onClearFoodNameCorrection: () -> Unit,
    onDismiss: () -> Unit,
    onAskAboutMeal: () -> Boolean,
    onSave: (MealEntry) -> String?,
    onDelete: () -> String?,
) {
    var items by rememberSaveable(meal.id, stateSaver = EditableSavedFoodsSaver) { mutableStateOf(meal.items.map { EditableSavedFood(it.name, it.description.orEmpty(), it.grams?.toString().orEmpty(), it.preparation.orEmpty(), it.groupLabel, it.role) }) }
    var title by rememberSaveable(meal.id) { mutableStateOf(meal.title ?: MealIdentity.titleFor(meal.items)) }
    var date by rememberSaveable(meal.id) { mutableStateOf(meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString()) }
    val recordedTime = remember(meal) { meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0) }
    var mealSlot by rememberSaveable(meal.id) { mutableStateOf(meal.mealSlot) }
    var calories by rememberSaveable(meal.id) { mutableStateOf(meal.nutrition?.caloriesKcal?.toString().orEmpty()) }
    var protein by rememberSaveable(meal.id) { mutableStateOf(meal.nutrition?.proteinGrams?.toString().orEmpty()) }
    var carbs by rememberSaveable(meal.id) { mutableStateOf(meal.nutrition?.carbohydrateGrams?.toString().orEmpty()) }
    var fat by rememberSaveable(meal.id) { mutableStateOf(meal.nutrition?.fatGrams?.toString().orEmpty()) }
    var error by rememberSaveable(meal.id) { mutableStateOf<String?>(null) }
    var editingFoodIndex by rememberSaveable(meal.id) { mutableStateOf<Int?>(null) }
    var foodEditBaseline by rememberSaveable(meal.id, stateSaver = EditableSavedFoodsSaver) { mutableStateOf(emptyList()) }
    var foodMatchBaseline by rememberSaveable(meal.id) { mutableStateOf("") }
    var confirmLeave by rememberSaveable(meal.id) { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(meal.id) { mutableStateOf(false) }
    var nutritionEditorExpanded by rememberSaveable(meal.id) { mutableStateOf(false) }
    var confirmReferenceRemoval by rememberSaveable(meal.id) { mutableStateOf(false) }
    var confirmMealDeletion by rememberSaveable(meal.id) { mutableStateOf(false) }
    var confirmNutritionCoverageLoss by rememberSaveable(meal.id) { mutableStateOf(false) }
    var confirmAskAboutMeal by rememberSaveable(meal.id) { mutableStateOf(false) }
    var askAfterCoverageConfirmation by remember { mutableStateOf(false) }
    var removeReferenceThumbnail by rememberSaveable(meal.id) { mutableStateOf(false) }
    val referenceThumbnail = meal.referenceThumbnailBase64.takeUnless { removeReferenceThumbnail }
    var calculateWithCatalog by rememberSaveable(meal.id) { mutableStateOf(meal.nutrition?.source == "member_confirmed_catalog") }
    val isCatalogMeal = calculateWithCatalog
    var catalogMatchIds by rememberSaveable(meal.id, catalog?.sourceRelease) { mutableStateOf(meal.items.map { item ->
        val foodId = item.catalogFoodId ?: meal.nutrition?.catalogFoodId.takeIf { meal.items.size == 1 }
        if (foodId != null) {
            catalog?.foods?.firstOrNull { it.fdcId == foodId }?.fdcId?.toString().orEmpty()
        } else {
            catalog?.let { FoodCatalog.suggest(it, item.name, item.description.orEmpty())?.fdcId?.toString() }.orEmpty()
        }
    }) }
    val foodsById = remember(catalog) { catalog?.foods?.associateBy(CatalogFood::fdcId).orEmpty() }
    val catalogMatches = catalogMatchIds.map { it.toLongOrNull()?.let(foodsById::get) }
    var catalogSearchIndex by rememberSaveable(meal.id) { mutableStateOf<Int?>(null) }
    var correctionTargetIndex by rememberSaveable(meal.id) { mutableStateOf<Int?>(null) }
    var correctionInput by rememberSaveable(meal.id) { mutableStateOf("") }
    var correctionOriginalName by rememberSaveable(meal.id) { mutableStateOf("") }
    var correctionRequestId by remember { mutableStateOf<Long?>(null) }
    val latestCorrectionRequestId by rememberUpdatedState(correctionRequestId)
    val latestCorrectionStatus by rememberUpdatedState(foodNameCorrection)
    val latestCancelCorrection by rememberUpdatedState(onCancelFoodNameCorrection)
    val latestClearCorrection by rememberUpdatedState(onClearFoodNameCorrection)
    val parsedCatalogGrams = items.map { it.grams.replace(',', '.').toIntOrNull()?.takeIf { grams -> grams in 1..2_000 } }
    val catalogValues = items.indices.map { index ->
        catalogMatches.getOrNull(index)?.let { food -> parsedCatalogGrams[index]?.let { FoodCatalog.calculate(food, it) } }
    }
    val includedCatalogIndexes = catalogValues.indices.filter { index ->
        val nutrition = catalogValues[index]
        nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
            nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
    }
    val catalogTotals = includedCatalogIndexes.takeIf { isCatalogMeal && it.isNotEmpty() }?.map { catalogValues[it]!! }?.let { values -> CatalogNutrition(
        values.sumOf { it.caloriesKcal!! }, values.sumOf { it.proteinGrams!! },
        values.sumOf { it.carbohydrateGrams!! }, values.sumOf { it.fatGrams!! },
        values.any { it.caloriesDerivedFromMacros },
    ) }
    val catalogEstimateCoverage = SavedMealEditPolicy.catalogEstimateCoverage(
        original = meal.nutrition,
        hasTotals = catalogTotals != null,
        includedFoodCount = includedCatalogIndexes.size,
        foodCount = items.size,
    )
    val isPartialCatalogEstimate = catalogEstimateCoverage == "partial"
    val savedCatalogFoodIds = remember(meal) {
        meal.items.mapIndexedNotNull { index, item ->
            (item.catalogFoodId ?: meal.nutrition?.catalogFoodId.takeIf { meal.items.size == 1 })?.let { index to it }
        }
    }
    val unresolvedKeptMatch = catalog != null && savedCatalogFoodIds.any { (index, id) ->
        SavedMealEditPolicy.catalogMatchIsUnresolved(
            meal.items[index].name,
            items.getOrNull(index)?.name.orEmpty(),
            id,
            catalogMatches.getOrNull(index)?.fdcId,
            catalog.foods.mapTo(mutableSetOf(), CatalogFood::fdcId),
        )
    }

    fun editSnapshot() = listOf(title, date, mealSlot.orEmpty(), calories, protein, carbs, fat,
        removeReferenceThumbnail.toString(), calculateWithCatalog.toString()) +
        items.flatMap { listOf(it.name, it.description, it.grams, it.preparation, it.groupLabel.orEmpty(), it.role) } + catalogMatchIds
    val initialEditSnapshot by rememberSaveable(meal.id) { mutableStateOf(editSnapshot()) }
    val sheetState = rememberGuardedEditorSheetState(
        hasChanges = editSnapshot() != initialEditSnapshot,
        saving = false,
    ) { confirmLeave = true }
    val sheetScope = rememberCoroutineScope()
    fun closeFoodEditor(cancel: Boolean) {
        val index = editingFoodIndex
        if (cancel && index != null && foodEditBaseline.size == 1) {
            items = items.toMutableList().also { it[index] = foodEditBaseline.first() }
            catalogMatchIds = catalogMatchIds.toMutableList().also { it[index] = foodMatchBaseline }
        }
        correctionRequestId?.let(onCancelFoodNameCorrection)
        onClearFoodNameCorrection()
        correctionRequestId = null
        correctionTargetIndex = null
        catalogSearchIndex = null
        editingFoodIndex = null
        error = null
    }
    fun requestDismiss() {
        if (catalogSearchIndex != null) catalogSearchIndex = null
        else if (editingFoodIndex != null) closeFoodEditor(cancel = true)
        else if (editSnapshot() != initialEditSnapshot) confirmLeave = true
        else onDismiss()
    }
    BackHandler { requestDismiss() }

    DisposableEffect(meal.id) {
        onDispose {
            val requestId = latestCorrectionRequestId
            if (requestId != null && latestCorrectionStatus.state == "running" && latestCorrectionStatus.requestId == requestId) {
                latestCancelCorrection(requestId)
            }
            correctionRequestId = null
            latestClearCorrection()
        }
    }

    fun saveMealDraft(coverageLossConfirmed: Boolean = false, askAfterSaving: Boolean = false) {
        error = null
        val isCatalogBacked = isCatalogMeal || meal.nutrition?.source == "member_confirmed_catalog" || savedCatalogFoodIds.isNotEmpty()
        if (!SavedMealEditPolicy.canSaveCatalogBackedEdit(isCatalogBacked, catalog != null, unresolvedKeptMatch)) {
            error = if (catalog == null) {
                "The saved offline food catalog is unavailable. No changes were saved; try again when it can be opened."
            } else {
                "An existing food match is unavailable in the local catalog. No changes were saved, so its estimate remains intact."
            }
            return
        }
        val recordedAt = runCatching { LocalDate.parse(date).atTime(recordedTime).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
        val updatedItems = items.mapIndexed { index, item ->
            val original = meal.items[index]
            val oldCatalogId = original.catalogFoodId ?: meal.nutrition?.catalogFoodId.takeIf { meal.items.size == 1 }
            val selectedMatch = catalogMatches.getOrNull(index)
            val catalogFoodId = when {
                isCatalogMeal -> selectedMatch?.fdcId
                oldCatalogId != null && catalogMatchIds.getOrNull(index) == oldCatalogId.toString() -> selectedMatch?.fdcId ?: oldCatalogId
                else -> null
            }
            MealEntryItem(
                item.name.trim(),
                if (isCatalogMeal) parsedCatalogGrams[index] else item.grams.replace(',', '.').toIntOrNull(),
                item.preparation.trim().takeIf(String::isNotEmpty),
                catalogFoodId,
                catalogFoodId?.let { catalog?.sourceRelease ?: original.catalogRelease ?: meal.nutrition?.catalogRelease },
                item.groupLabel,
                item.description.trim().takeIf(String::isNotEmpty), item.role,
            )
        }
        val rawNutrition = listOf(calories, protein, carbs, fat)
        val values = rawNutrition.map { it.replace(',', '.').toDoubleOrNull() }
        val invalidNutrition = rawNutrition.zip(values).any { (raw, value) -> raw.isNotBlank() && value == null }
        val nutrition = when {
            isCatalogMeal -> catalogTotals?.let { totals -> MealNutrition(
                totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams,
                "member_confirmed_catalog", catalogRelease = catalog?.sourceRelease,
                estimateCoverage = catalogEstimateCoverage ?: "full",
            ) }
            rawNutrition.all(String::isBlank) -> null
            else -> MealNutrition(values[0], values[1], values[2], values[3], "manual_entry")
        }
        val updated = if (recordedAt == null) null else SavedMealEditPolicy.applyReferenceThumbnail(meal.copy(
            recordedAt = recordedAt,
            title = title.trim().takeIf(String::isNotEmpty),
            mealSlot = mealSlot,
            items = updatedItems,
            source = if (isCatalogMeal) "member_confirmed_catalog" else meal.source,
            nutrition = nutrition,
        ), referenceThumbnail)
        if (invalidNutrition || updated == null || !MealEntryValidation.valid(updated)) {
            error = "Choose at least one food and a valid portion, then review the date."
            return
        }
        if (!coverageLossConfirmed && SavedMealEditPolicy.requiresNutritionCoverageConfirmation(meal.nutrition, updated.nutrition)) {
            askAfterCoverageConfirmation = askAfterSaving
            confirmNutritionCoverageLoss = true
            return
        }
        error = onSave(updated)
        if (error == null && askAfterSaving) {
            onAskAboutMeal()
        }
    }

    ModalBottomSheet(onDismissRequest = ::requestDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight().imePadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (editingFoodIndex == null) "Your meal" else "Edit ingredient", style = MaterialTheme.typography.headlineMedium)
            if (editingFoodIndex == null) {
            OutlinedTextField(title, { title = it }, label = { Text("Meal name (optional)") }, modifier = Modifier.fillMaxWidth())
            referenceThumbnail?.let { reference ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(152.dp)) {
                        SavedMealReferenceThumbnail(reference, 152.dp)
                        Surface(
                            color = MetriaColors.Surface.copy(alpha = 0.82f),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(44.dp),
                        ) {
                            IconButton(onClick = { confirmReferenceRemoval = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove private reference photo", tint = MetriaColors.Ink)
                            }
                        }
                    }
                }
            }
            val displayed = if (isCatalogMeal) catalogTotals?.let {
                MealNutrition(it.caloriesKcal, it.proteinGrams, it.carbohydrateGrams, it.fatGrams, "member_confirmed_catalog")
            } else MealNutrition(calories.replace(',', '.').toDoubleOrNull(), protein.replace(',', '.').toDoubleOrNull(), carbs.replace(',', '.').toDoubleOrNull(), fat.replace(',', '.').toDoubleOrNull(), meal.nutrition?.source ?: "manual_entry")
            Text(if (isCatalogMeal) "Nutrition estimate · ${if (isPartialCatalogEstimate) "Partial" else "Member-confirmed match"}" else if (meal.nutrition?.source == "verified_label") "Recorded nutrition · Verified label" else "Recorded nutrition · Manual entry", style = MaterialTheme.typography.titleMedium)
            app.metria.ui.components.MealNutritionMetrics(displayed?.caloriesKcal, displayed?.proteinGrams, displayed?.carbohydrateGrams, displayed?.fatGrams)
            if (isCatalogMeal) app.metria.ui.components.MealPortionControl { factor ->
                val scaled = app.metria.MealPortionScale.apply(items.map { it.grams }, factor)
                if (scaled == null) false else {
                    items = items.mapIndexed { index, item -> item.copy(grams = scaled[index]) }
                    true
                }
            }
            Text("Ingredients", style = MaterialTheme.typography.titleLarge)
            }
            items.forEachIndexed { index, item ->
                if (editingFoodIndex == null) {
                    OutlinedButton(onClick = {
                        foodEditBaseline = listOf(item)
                        foodMatchBaseline = catalogMatchIds.getOrNull(index).orEmpty()
                        editingFoodIndex = index
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(item.name.ifBlank { "Unnamed food" }, style = MaterialTheme.typography.titleMedium)
                            Text(listOfNotNull(item.grams.takeIf(String::isNotBlank)?.let { "$it g" }, item.preparation.takeIf(String::isNotBlank)).joinToString(" · ").ifBlank { "Portion needs review" })
                            if (isCatalogMeal) catalogValues.getOrNull(index)?.caloriesKcal?.let { kcal ->
                                Text("${java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(kcal)} kcal · estimate", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else if (editingFoodIndex == index) {
                OutlinedTextField(item.name, { value ->
                    items = items.toMutableList().also { it[index] = item.copy(name = value) }
                    catalogMatchIds = catalogMatchIds.toMutableList().also {
                        it[index] = SavedMealEditPolicy.catalogMatchAfterFoodNameEdit(item.name, value, it[index]).orEmpty()
                    }
                }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                TextButton(
                    onClick = {
                        if (correctionRequestId != null) onClearFoodNameCorrection()
                        correctionTargetIndex = index
                        correctionInput = ""
                        correctionOriginalName = item.name
                        correctionRequestId = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (correctionTargetIndex == index) "Metria food-name help" else "Ask Metria about this food") }
                if (correctionTargetIndex == index) {
                    SavedFoodNameCorrectionEditor(
                        input = correctionInput,
                        onInputChange = { correctionInput = it },
                        status = foodNameCorrection,
                        requestId = correctionRequestId,
                        originalName = correctionOriginalName,
                        currentName = item.name,
                        onSuggest = {
                            val result = onSuggestFoodName(correctionOriginalName, correctionInput)
                            correctionRequestId = result.requestId
                        },
                        onCancel = {
                            correctionRequestId?.let(onCancelFoodNameCorrection)
                            onClearFoodNameCorrection()
                            correctionRequestId = null
                        },
                        onApply = { replacement ->
                            items = items.toMutableList().also { it[index] = it[index].copy(name = replacement) }
                            catalogMatchIds = catalogMatchIds.toMutableList().also {
                                it[index] = SavedMealEditPolicy.catalogMatchAfterFoodNameEdit(correctionOriginalName, replacement, it[index]).orEmpty()
                            }
                            onClearFoodNameCorrection()
                            correctionRequestId = null
                            correctionTargetIndex = null
                            correctionInput = ""
                        },
                        onDiscard = {
                            onClearFoodNameCorrection()
                            correctionRequestId = null
                            correctionTargetIndex = null
                            correctionInput = ""
                        },
                    )
                }
                OutlinedTextField(
                    item.description,
                    { value -> items = items.toMutableList().also { it[index] = item.copy(description = value) } },
                    label = { Text("What Metria saw (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value ->
                    items = items.toMutableList().also { it[index] = item.copy(preparation = value) }
                    if (item.preparation.trim() != value.trim()) catalogMatchIds = catalogMatchIds.toMutableList().also { it[index] = "" }
                }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
                if (isCatalogMeal) {
                    val match = catalogMatches.getOrNull(index)
                    Text(
                        if (match == null) "Choose this food to include it in the estimate."
                        else "Included food: ${FoodCatalog.memberDisplayName(match)}",
                        color = if (match == null) MetriaColors.Danger else MetriaColors.Primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { catalogSearchIndex = index }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (match == null) "Find this food" else "Change food")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (editingFoodIndex == null) {
            if (isCatalogMeal) {
                app.metria.ui.components.CatalogNutrientDetailsCard(
                    app.metria.CatalogNutrientDetails.calculate(items.indices.map { index ->
                        catalogMatches.getOrNull(index) to parsedCatalogGrams[index]
                    }),
                    catalog?.sourceRelease,
                )
                Text(if (isPartialCatalogEstimate) "Partial local estimate" else "Member-confirmed catalog estimate", style = MaterialTheme.typography.titleMedium)
                if (catalogTotals == null) {
                    Text("Choose at least one food and its portion to calculate an estimate.", color = MetriaColors.Danger)
                } else {
                    Text(nutritionSummary(MealNutrition(
                        catalogTotals.caloriesKcal, catalogTotals.proteinGrams,
                        catalogTotals.carbohydrateGrams, catalogTotals.fatGrams,
                        "member_confirmed_catalog", catalogRelease = catalog!!.sourceRelease,
                    )), color = MetriaColors.Primary)
                    if (isPartialCatalogEstimate) Text(
                        "Includes: ${includedCatalogIndexes.joinToString { items[it].name }}. Other foods are excluded from this estimate and full totals.",
                        color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall,
                    )
                    if (catalogTotals.caloriesDerivedFromMacros) Text("Calories are calculated from available macros for one selected food.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    "Recorded values are shown above. Missing values stay unknown.",
                    color = MetriaColors.InkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { calculateWithCatalog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Calculate from offline catalog")
                }
                Text("Metria will suggest local matches for the foods and portions above. Review them before saving.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { nutritionEditorExpanded = !nutritionEditorExpanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (nutritionEditorExpanded) "Hide nutrition editor" else "Edit nutrition values") }
                if (nutritionEditorExpanded) {
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbohydrates (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
                }
            }
            TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (detailsExpanded) "Hide meal details" else "Meal details · ${MealSlots.label(mealSlot)} · $date") }
            if (detailsExpanded) {
            SavedMealSlotSelector(mealSlot, MealSlots.suggestedFor(recordedTime).takeIf { meal.recordedTimeKnown }) { mealSlot = it }
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            }
            error?.let { Text(it, color = MetriaColors.Danger) }
            TextButton(onClick = { error = null; confirmMealDeletion = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Delete meal") }
            TextButton(
                onClick = {
                    if (editSnapshot() == initialEditSnapshot) {
                        if (!onAskAboutMeal()) error = "This saved meal is no longer available. Close and reopen it before asking."
                    } else confirmAskAboutMeal = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Ask Metria about this meal") }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (editingFoodIndex == null) {
                Button(onClick = { saveMealDraft() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save meal changes") }
                TextButton(onClick = ::requestDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Leave without saving") }
            } else {
                Button(onClick = {
                    val item = items.getOrNull(editingFoodIndex ?: -1)
                    if (item == null || item.name.isBlank() || (item.grams.isNotBlank() && item.grams.toIntOrNull()?.let { it in 1..2_000 } != true)) error = "Check the food name and portion in grams."
                    else closeFoodEditor(cancel = false)
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Apply ingredient") }
                TextButton(onClick = { closeFoodEditor(cancel = true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel ingredient changes") }
            }
        }
        }
    }
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Leave without saving?") },
        text = { Text("Your changes to this meal will be lost.") },
        confirmButton = { Button(onClick = { confirmLeave = false; sheetScope.launch { sheetState.show() } }) { Text("Keep editing") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false; onDismiss() }) { Text("Leave without saving") } },
    )
    if (confirmAskAboutMeal) AlertDialog(
        onDismissRequest = { confirmAskAboutMeal = false },
        title = { Text("Finish editing first") },
        text = { Text("Save your meal changes or discard them before asking. Metria will read the saved meal again when you send your question.") },
        confirmButton = {
            Button(onClick = {
                confirmAskAboutMeal = false
                saveMealDraft(askAfterSaving = true)
            }) { Text("Save and ask") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = {
                    if (onAskAboutMeal()) confirmAskAboutMeal = false
                    else {
                        confirmAskAboutMeal = false
                        error = "This saved meal is no longer available. Close and reopen it before asking."
                    }
                }) { Text("Discard edits and ask") }
                TextButton(onClick = { confirmAskAboutMeal = false }) { Text("Keep editing") }
            }
        },
    )
    catalogSearchIndex?.let { index -> SavedOfflineCatalogSearchSheet(
        catalog = catalog,
        initialQuery = items.getOrNull(index)?.name.orEmpty(),
        onDismiss = { catalogSearchIndex = null },
        onSelect = { food ->
            catalogMatchIds = catalogMatchIds.toMutableList().also { it[index] = food.fdcId.toString() }
            catalogSearchIndex = null
        },
    ) }
    if (confirmReferenceRemoval) {
        AlertDialog(
            onDismissRequest = { confirmReferenceRemoval = false },
            title = { Text("Remove private reference?") },
            text = { Text("The photo will be removed if you save these meal changes.") },
            confirmButton = { Button(onClick = { removeReferenceThumbnail = true; confirmReferenceRemoval = false }) { Text("Remove photo") } },
            dismissButton = { Button(onClick = { confirmReferenceRemoval = false }) { Text("Keep photo") } },
        )
    }
    if (confirmNutritionCoverageLoss) {
        val hasRemainingEstimate = catalogTotals != null
        AlertDialog(
            onDismissRequest = { confirmNutritionCoverageLoss = false; askAfterCoverageConfirmation = false },
            title = { Text(if (hasRemainingEstimate) "Save a partial estimate?" else "Save without this estimate?") },
            text = {
                Text(
                    if (hasRemainingEstimate) {
                        "These edits leave only a partial catalog estimate. Foods without a selected match are excluded. Save the meal with this reduced coverage?"
                    } else {
                        "These edits leave no usable catalog estimate. Saving will remove this meal's current nutrition values. Save without them?"
                    },
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirmNutritionCoverageLoss = false
                    val askAfterSaving = askAfterCoverageConfirmation
                    askAfterCoverageConfirmation = false
                    saveMealDraft(coverageLossConfirmed = true, askAfterSaving = askAfterSaving)
                }) { Text(if (hasRemainingEstimate) "Save partial estimate" else "Save without estimate") }
            },
            dismissButton = { TextButton(onClick = { confirmNutritionCoverageLoss = false; askAfterCoverageConfirmation = false }) { Text("Keep editing") } },
        )
    }
    if (confirmMealDeletion) {
        AlertDialog(
            onDismissRequest = { confirmMealDeletion = false },
            title = { Text("Delete meal?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This permanently removes the meal and its optional private reference from your encrypted vault.")
                    error?.let { Text(it, color = MetriaColors.Danger) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    error = onDelete()
                    if (error == null) confirmMealDeletion = false
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { confirmMealDeletion = false }) { Text("Keep meal") } },
        )
    }
}

@Composable
private fun SavedFoodNameCorrectionEditor(
    input: String,
    onInputChange: (String) -> Unit,
    status: MealFoodNameCorrectionStatus,
    requestId: Long?,
    originalName: String,
    currentName: String,
    onSuggest: () -> Unit,
    onCancel: () -> Unit,
    onApply: (String) -> Unit,
    onDiscard: () -> Unit,
) {
    val busy = status.state == "running" && status.requestId == requestId && requestId != null
    val suggested = MealFoodNameCorrectionPresentation.suggestionFor(
        status, requestId, "saved-meal-food", "saved-meal-food",
    )
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Correct this food name", style = MaterialTheme.typography.titleSmall)
            Text("Tell Metria what this food really is. The name stays a draft until Save. Nutrition you entered manually is unchanged.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("For example: these are albóndigas") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
            )
            if (busy) {
                Text("Considering your correction on this phone…", color = MetriaColors.Primary, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel suggestion") }
            } else if (suggested != null) {
                Text(
                    if (currentName == originalName) "$originalName  →  $suggested" else "This food name changed while Metria was considering the suggestion.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                    Button(onClick = { onApply(suggested) }, enabled = currentName == originalName && originalName.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Apply name") }
                }
            } else {
                status.diagnostic?.takeIf { status.requestId == requestId }?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = onSuggest, enabled = input.trim().length >= 3, modifier = Modifier.fillMaxWidth()) { Text("Suggest a food name") }
            }
        }
    }
}

@Composable
private fun SavedMealSlotSelector(selected: String?, suggested: String?, choose: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(suggested?.let { "Meal type (optional) - suggested: ${MealSlots.label(it)}" } ?: "Meal type (optional)", style = MaterialTheme.typography.labelLarge)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("breakfast", "lunch", "dinner", "snack").forEach { slot ->
                FilterChip(selected = selected == slot, onClick = { choose(if (selected == slot) null else slot) }, label = { Text(MealSlots.label(slot)) })
            }
        }
        Text("Optional — only a type you choose is used for personalised meal suggestions.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SavedMealReferenceThumbnail(encoded: String, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(encoded) {
        runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    bitmap?.let { Image(bitmap = it, contentDescription = "Private meal reference", modifier = Modifier.size(size)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedOfflineCatalogSearchSheet(catalog: OfflineFoodCatalog?, initialQuery: String, onDismiss: () -> Unit, onSelect: (CatalogFood) -> Unit) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val results = remember(catalog, query) { catalog?.let { FoodCatalog.search(it, query) }.orEmpty() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.imePadding().padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Find a food", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Back to ingredient")
            }
            if (catalog == null) {
                Text("The local catalog could not be opened. You can still describe a meal or enter a label manually.", color = MetriaColors.Danger)
                return@Column
            }
            Text("${catalog.foods.size} foods available · USDA Foundation Foods", color = MetriaColors.InkMuted)
            OutlinedTextField(query, { query = it }, label = { Text("Food name") }, placeholder = { Text("For example: rice or chicken") }, modifier = Modifier.fillMaxWidth())
            if (query.isNotBlank() && results.isEmpty()) {
                Text("No matching food in this catalog.", style = MaterialTheme.typography.titleSmall)
                Text("Try a simpler name. Some foods aren't included; you can leave nutrition unknown or enter values manually.", color = MetriaColors.InkMuted)
            }
            results.forEach { food ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(food) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(FoodCatalog.memberDisplayName(food), style = MaterialTheme.typography.titleMedium)
                        if (FoodCatalog.memberDisplayName(food) != food.description) Text("USDA: ${food.description}", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                        food.category?.let { Text(it, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall) }
                        Text("USDA Foundation Foods · ${food.portions.firstOrNull()?.label ?: "100 g reference"}", color = MetriaColors.Primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun nutritionSummary(nutrition: MealNutrition): String = listOfNotNull(
    nutrition.caloriesKcal?.let { "${"%.0f".format(it)} kcal" },
    nutrition.proteinGrams?.let { "${"%.1f".format(it)} g protein" },
    nutrition.carbohydrateGrams?.let { "${"%.1f".format(it)} g carbs" },
    nutrition.fatGrams?.let { "${"%.1f".format(it)} g fat" },
).joinToString(" - ").ifBlank { "No nutrition values entered." }
