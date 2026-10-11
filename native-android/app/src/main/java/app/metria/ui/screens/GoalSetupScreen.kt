package app.metria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.*
import app.metria.ui.components.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

private fun goalNumber(value: Double) = goalDisplayNumber(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionGoalsScreen(
    targets: NutritionTargets?, setup: GoalSetup?, latestWeightKg: Double?, onDismiss: () -> Unit,
    onSave: suspend (NutritionTargets?, Boolean, Set<GoalMacroField>) -> String?, onConfirmGoal: suspend (GoalPhase, Boolean) -> String?,
    onDeleteSetup: suspend () -> String?, onDeleteHistory: suspend (String) -> String?,
    showWelcomeInitially: Boolean = false,
    displayUnit: String = "kg",
    weightEntries: List<WeightEntry> = emptyList(),
    meals: List<MealEntry> = emptyList(),
) {
    var page by rememberSaveable(
        stateSaver = Saver<String, String>(
            save = { if (it == "detail") "detail" else "hub" },
            restore = { if (it == "detail") "detail" else "hub" },
        ),
    ) { mutableStateOf(if (showWelcomeInitially) "welcome" else "hub") }
    var selectedPhaseId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingWeightReview by remember { mutableStateOf<GoalPhase?>(null) }
    var welcomeFromFirstRun by remember { mutableStateOf(showWelcomeInitially) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var referencesExpanded by rememberSaveable { mutableStateOf(false) }
    var moreOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var manageDataExpanded by rememberSaveable { mutableStateOf(false) }
    val hubScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun requestDelete(id: String) {
        if (saving) return
        error = null
        deleteId = id
    }

    fun delete(id: String) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            try {
                error = if (id == "all") onDeleteSetup() else onDeleteHistory(id)
                if (error == null) {
                    deleteId = null
                    selectedPhaseId = null
                    page = "hub"
                    manageDataExpanded = false
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = if (id == "all") "Goal data could not be deleted. Try again." else "This phase could not be deleted. Try again."
            } finally {
                saving = false
            }
        }
    }

    when (page) {
        "welcome" -> GoalWelcomeSheet(
            onSetUp = { welcomeFromFirstRun = false; page = if (setup == null) "setup" else "edit" },
            onContinueTracking = { if (welcomeFromFirstRun) onDismiss() else page = "hub" },
        )
        "manual" -> ManualNutritionTargetsSheet(targets, setup?.current?.acceptedEstimate != null, setup?.current?.macroSuggestion, setup?.current?.phaseEnergySuggestion, { page = "hub" }, onSave)
        "weight" -> setup?.current?.let { phase ->
            GoalWeightEditorSheet(
                phase = phase,
                onDismiss = { page = "hub" },
                onReviewReferences = { changed -> pendingWeightReview = changed; page = "edit" },
            ) { changed -> onConfirmGoal(changed, false) }
        }
        "setup", "edit", "new", "recalculate" -> GoalSetupSheet(
            targets = targets,
            editing = (pendingWeightReview ?: setup?.current).takeIf { page == "edit" || page == "recalculate" },
            previous = setup?.current,
            newPhase = page == "new",
            onDismiss = { pendingWeightReview = null; page = "hub" },
            onConfirm = { phase, isNew ->
                val result = onConfirmGoal(phase, isNew)
                if (result == null) pendingWeightReview = null
                result
            },
            recalculating = page == "recalculate",
            historyCount = setup?.history?.size ?: 0,
            displayUnit = displayUnit,
        )
        else -> {
            val selectedPhase = when {
                selectedPhaseId != null && setup?.current?.id == selectedPhaseId -> setup?.current
                else -> setup?.history?.firstOrNull { it.id == selectedPhaseId }
            }
            LaunchedEffect(page, selectedPhase?.id) {
                if (page == "detail" && selectedPhase == null) {
                    page = "hub"
                    selectedPhaseId = null
                }
            }
            val currentPage = rememberUpdatedState(page)
            val currentSaving = rememberUpdatedState(saving)
            val returnToHub = rememberUpdatedState {
                page = "hub"
                selectedPhaseId = null
                error = null
            }
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { target ->
                    when {
                        target != androidx.compose.material3.SheetValue.Hidden -> true
                        currentSaving.value -> false
                        currentPage.value == "detail" -> { returnToHub.value(); false }
                        else -> true
                    }
                },
            )
            ModalBottomSheet(
                onDismissRequest = {
                    if (currentPage.value == "detail") {
                        returnToHub.value()
                        scope.launch { sheetState.show() }
                    } else if (!currentSaving.value) {
                        onDismiss()
                    }
                },
                sheetState = sheetState,
            ) {
                if (page == "detail" && selectedPhase != null) {
                    GoalPhaseDetailScreen(
                        phase = selectedPhase,
                        isCurrent = selectedPhase.id == setup?.current?.id,
                        displayUnit = displayUnit,
                        deleting = saving,
                        weightEntries = weightEntries,
                        meals = meals,
                        onBackToHub = { page = "hub"; selectedPhaseId = null; error = null },
                        onDone = onDismiss,
                        onEditCurrent = { page = "weight"; selectedPhaseId = null },
                        onDeletePrevious = { requestDelete(selectedPhase.id) },
                    )
                } else {
                    GoalsHubScreen(
                        targets = targets,
                        setup = setup,
                        latestWeightKg = latestWeightKg,
                        displayUnit = displayUnit,
                        scrollState = hubScrollState,
                        referencesExpanded = referencesExpanded,
                        manageDataExpanded = manageDataExpanded,
                        moreOptionsExpanded = moreOptionsExpanded,
                        saving = saving,
                        onToggleReferences = { referencesExpanded = !referencesExpanded },
                        onEditReferences = { page = "manual"; error = null },
                        onOpenPhase = { id -> selectedPhaseId = id; page = "detail"; error = null },
                        onEditCurrent = { page = "weight"; error = null },
                        onNewPhase = { page = "new"; error = null },
                        onSetUpGoal = { page = "setup"; error = null },
                        onRecalculate = { page = if (setup == null) "setup" else "recalculate"; error = null },
                        onAbout = { welcomeFromFirstRun = false; page = "welcome" },
                        onToggleManageData = { manageDataExpanded = !manageDataExpanded },
                        onToggleMoreOptions = { moreOptionsExpanded = !moreOptionsExpanded },
                        onDeleteAll = { requestDelete("all") },
                        onDone = onDismiss,
                    )
                }
            }
            BackHandler(enabled = page == "detail" && deleteId == null && !saving) {
                page = "hub"
                selectedPhaseId = null
                error = null
            }
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { if (!saving) { deleteId = null; error = null } },
            title = { Text(if (id == "all") "Delete profile and goal phases?" else "Delete this previous phase?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (id == "all") "This removes all calculation profiles and goal history. Daily targets remain as member-entered references. Meals and weights stay unchanged."
                        else "This removes the selected phase and its calculation profile. Your current goal, daily references, meals and weights stay unchanged.",
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (saving) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Deleting locally...") }
                }
            },
            confirmButton = {
                Button(enabled = !saving, onClick = { delete(id) }) {
                    Text(if (saving) "Deleting..." else if (error != null) "Retry delete" else "Delete")
                }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleteId = null; error = null }) { Text("Keep") } },
        )
    }
}

@Composable
internal fun MaintenanceDetails(estimate: MaintenanceEstimate) {
    Text("Activity scenario: ${goalNumber(estimate.minimumKcal)}-${goalNumber(estimate.maximumKcal)} kcal / day. This range is not a confidence interval or a guarantee.", style = MaterialTheme.typography.bodyMedium)
    Text("Age at calculation: ${estimate.profile.age}. Height: ${goalNumber(estimate.profile.heightCm)} cm. Reviewed weight: ${goalNumber(estimate.profile.weightKg)} kg. ${estimate.profile.equation.title}. Activity: ${estimate.profile.activity.title}.", style = MaterialTheme.typography.bodySmall)
    Text("Mifflin-St Jeor resting energy times a FAO whole-day activity interval, rounded outward to 50 kcal. FAO uses basal energy; combining it with this resting-energy equation is an approximation, not a validated combined method. ${estimate.method}.", style = MaterialTheme.typography.bodySmall)
}
