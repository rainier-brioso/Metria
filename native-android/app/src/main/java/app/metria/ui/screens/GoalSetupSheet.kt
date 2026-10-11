package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
internal fun GoalSetupSheet(
    targets: NutritionTargets?, editing: GoalPhase?, previous: GoalPhase?, newPhase: Boolean,
    onDismiss: () -> Unit, onConfirm: suspend (GoalPhase, Boolean) -> String?, recalculating: Boolean = false,
    historyCount: Int = 0,
    displayUnit: String = "kg",
) {
    val originalPhase = remember { previous }
    val originalTargets = remember { targets }
    val originalHistoryCount = remember { historyCount }
    val profileToReview = editing ?: previous.takeIf { newPhase }
    val oldProfile = profileToReview?.phaseEnergySuggestion?.estimate?.profile ?: profileToReview?.acceptedEstimate?.profile
    val initialReferences = GoalSetupInput.initialReferences(targets, previous, newPhase)
    val intentChangedEnergySource = editing?.let { phase -> phase.phaseEnergySuggestion?.takeIf { it.intent != phase.intent } }
    val initialPhaseEnergyDraft = intentChangedEnergySource?.let {
        GoalSetupDraftPolicy.invalidatePhaseEnergy(initialReferences, it, editing.macroSuggestion)
    }
    val initialDraftReferences = initialPhaseEnergyDraft?.references ?: initialReferences
    var step by remember { mutableStateOf(0) }
    var editor by remember { mutableStateOf<String?>(null) }
    var intent by remember { mutableStateOf(editing?.intent?.name ?: GoalIntent.MAINTAIN.name) }
    var age by remember { mutableStateOf(oldProfile?.age?.toString().orEmpty()) }
    var height by remember { mutableStateOf(oldProfile?.heightCm?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(oldProfile?.weightKg?.toString().orEmpty()) }
    var heightUnit by remember { mutableStateOf("cm") }
    var weightUnit by remember { mutableStateOf("kg") }
    var equation by remember { mutableStateOf(oldProfile?.equation?.name) }
    var activity by remember { mutableStateOf(oldProfile?.activity?.name) }
    var eligible by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    var careTeamManual by remember { mutableStateOf(false) }
    var useEstimate by remember { mutableStateOf(false) }
    var selectedEstimate by remember { mutableStateOf(if (newPhase || intentChangedEnergySource != null) null else editing?.phaseEnergySuggestion?.estimate) }
    var targetWeight by remember { mutableStateOf(editing?.targetWeightKg?.toString().orEmpty()) }
    var referenceWeight by remember { mutableStateOf(editing?.referenceWeightKg?.toString().orEmpty()) }
    var minimumWeight by remember { mutableStateOf(editing?.rangeMinimumKg?.toString().orEmpty()) }
    var maximumWeight by remember { mutableStateOf(editing?.rangeMaximumKg?.toString().orEmpty()) }
    val macroPrefillPhase = editing ?: previous.takeIf { newPhase }
    val macroPrefillSource = macroPrefillPhase?.macroSuggestion
    val macroPrefillProfile = macroPrefillPhase?.phaseEnergySuggestion?.estimate?.profile ?: macroPrefillPhase?.acceptedEstimate?.profile
    val macroPrefillWeight = macroPrefillSource?.weightKg
        ?: macroPrefillProfile?.weightKg
        ?: macroPrefillPhase?.referenceWeightKg
    val macroPrefillAge = macroPrefillSource?.age ?: macroPrefillProfile?.age
    var macroAge by remember { mutableStateOf(macroPrefillAge?.toString().orEmpty()) }
    var macroWeight by remember { mutableStateOf(macroPrefillWeight?.toString().orEmpty()) }
    var macroWeightUnit by remember { mutableStateOf("kg") }
    // A new phase always asks the member to choose its current training context.
    var macroTraining by remember { mutableStateOf(editing?.macroSuggestion?.training) }
    var macroInputsChanged by remember { mutableStateOf(false) }
    val initialMacroSuggestion = editing?.macroSuggestion?.takeIf {
        initialPhaseEnergyDraft == null && (initialDraftReferences[0].isBlank() || it.caloriesKcal == initialDraftReferences[0].toDoubleOrNull())
    }
    var macroSuggestion by remember { mutableStateOf(initialMacroSuggestion) }
    var phaseEnergySuggestion by remember { mutableStateOf(if (newPhase || initialPhaseEnergyDraft != null) null else editing?.phaseEnergySuggestion) }
    var phaseEnergyTraining by remember { mutableStateOf(if (newPhase || initialPhaseEnergyDraft != null) null else editing?.phaseEnergySuggestion?.training) }
    var phaseEnergyMaintenanceKcal by remember { mutableStateOf(if (newPhase || initialPhaseEnergyDraft != null) null else editing?.phaseEnergySuggestion?.maintenanceKcal) }
    var phaseEnergyEligible by remember { mutableStateOf(false) }
    var pregnancyBreastfeeding by remember { mutableStateOf(PregnancyBreastfeedingApplicability.UNANSWERED) }
    var phaseEnergyProposal by remember { mutableStateOf<PhaseEnergySuggestion?>(null) }
    var phaseEnergyMethodExpanded by remember { mutableStateOf(false) }
    var phaseEnergyEntryTraining by remember { mutableStateOf<GoalTraining?>(null) }
    var phaseEnergyEntryMaintenance by remember { mutableStateOf<Double?>(null) }
    var phaseEnergyEntryEstimate by remember { mutableStateOf<MaintenanceEstimate?>(null) }
    var phaseEnergyEntryFields by remember { mutableStateOf<List<String>?>(null) }
    var phaseEnergyEntrySource by remember { mutableStateOf<PhaseEnergySuggestion?>(null) }
    var phaseEnergyEntryMacroSource by remember { mutableStateOf<MacroSuggestion?>(null) }
    var phaseEnergyEntryMacroEligible by remember { mutableStateOf(false) }
    var phaseEnergyEntryChanged by remember { mutableStateOf(false) }
    var phaseEnergyEntryUseEstimate by remember { mutableStateOf(false) }
    var macroEligible by remember { mutableStateOf(false) }
    var macroPreview by remember { mutableStateOf<MacroSuggestion?>(null) }
    var macroMethodExpanded by remember { mutableStateOf(false) }
    var macroPreviewExistingValues by remember { mutableStateOf<List<String>?>(null) }
    var macroPreviewSource by remember { mutableStateOf<MacroSuggestion?>(null) }
    val initialMacroReferences = when {
        initialPhaseEnergyDraft != null -> initialDraftReferences.drop(1)
        editing?.macroSuggestion != null && initialMacroSuggestion == null ->
            GoalSetupDraftPolicy.invalidateMacroReferences(initialDraftReferences.drop(1), editing.macroSuggestion).values
        else -> initialDraftReferences.drop(1)
    }
    var fields by remember { mutableStateOf(listOf(initialDraftReferences[0]) + initialMacroReferences) }
    var methodDetails by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(initialPhaseEnergyDraft != null) }
    var leaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveAttempt by remember { mutableStateOf<GoalSaveAttempt?>(null) }
    var saved by remember { mutableStateOf(false) }
    var savedPage by remember { mutableStateOf(0) }
    var savedMethodExpanded by remember { mutableStateOf(false) }
    val phaseId = remember { editing?.id ?: UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()
    fun leave() { if (!saving) { if (changed && !saved) leaving = true else onDismiss() } }
    fun edited() { changed = true; error = null }
    fun useCareTeamReferences() {
        val manualDraft = GoalSuitabilityPolicy.useCareTeamReferences(
            references = fields,
            caloriesDerived = useEstimate || selectedEstimate != null || phaseEnergySuggestion != null,
            macroSource = macroSuggestion,
        )
        fields = manualDraft.references
        macroSuggestion = manualDraft.macroSuggestion
        selectedEstimate = null
        useEstimate = false
        phaseEnergySuggestion = null
        phaseEnergyProposal = null
        phaseEnergyMaintenanceKcal = null
        phaseEnergyEligible = false
        phaseEnergyMethodExpanded = false
        macroEligible = false
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
        eligible = false
        manual = true
        careTeamManual = true
        phaseEnergyEntryFields = null
        phaseEnergyEntrySource = null
        phaseEnergyEntryMacroSource = null
        phaseEnergyEntryEstimate = null
        phaseEnergyEntryMaintenance = null
        phaseEnergyEntryTraining = null
        phaseEnergyTraining = null
        phaseEnergyEntryMacroEligible = false
        phaseEnergyEntryChanged = false
        phaseEnergyEntryUseEstimate = false
        age = ""
        height = ""
        weight = ""
        equation = null
        activity = null
        macroAge = ""
        macroWeight = ""
        macroTraining = null
        phaseEnergyProposal = null
        saveAttempt = null
        editor = "daily"
        step = 4
        edited()
    }
    fun reviewGeneralOptionsAgain() {
        if (pregnancyBreastfeeding != PregnancyBreastfeedingApplicability.DOES_NOT_APPLY) return
        careTeamManual = false
        manual = false
        editor = null
        step = 2
        eligible = false
        phaseEnergyEligible = false
        macroEligible = false
        error = null
    }
    fun answerPregnancyBreastfeeding(answer: PregnancyBreastfeedingApplicability) {
        pregnancyBreastfeeding = answer
        if (answer == PregnancyBreastfeedingApplicability.APPLIES) useCareTeamReferences()
        else error = null
    }
    fun canUseGeneralOption(generalSuitable: Boolean) =
        GoalSuitabilityPolicy.permitsAutomaticProposal(generalSuitable, pregnancyBreastfeeding)
    fun estimate() = MaintenanceCalculator.calculate(MaintenanceCalculator.profile(age, height, heightUnit, weight, weightUnit, equation?.let(EnergyEquation::valueOf), activity?.let(DailyActivity::valueOf)))
    fun invalidateMacroInputs() {
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
        macroEligible = false
        val next = GoalSetupDraftPolicy.invalidateMacroReferences(fields.drop(1), macroSuggestion)
        fields = listOf(fields[0]) + next.values
        macroSuggestion = next.source
    }
    fun invalidatePhaseEnergyInputs(clearSelectedEstimate: Boolean = true) {
        phaseEnergyProposal = null
        phaseEnergyMethodExpanded = false
        phaseEnergyEligible = false
        macroEligible = false
        val next = GoalSetupDraftPolicy.invalidatePhaseEnergy(fields, phaseEnergySuggestion, macroSuggestion)
        fields = next.references
        phaseEnergySuggestion = next.phaseEnergySuggestion
        macroSuggestion = next.macroSuggestion
        if (clearSelectedEstimate) {
            selectedEstimate = null
            phaseEnergyMaintenanceKcal = null
            useEstimate = false
        }
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
    }
    fun invalidateEstimate() {
        val hadSelectedReference = useEstimate || selectedEstimate != null || phaseEnergySuggestion != null
        invalidatePhaseEnergyInputs(clearSelectedEstimate = false)
        val next = GoalSetupDraftPolicy.invalidateEstimate(GoalSetupDraftPolicy.References(fields, selectedEstimate), selected = hadSelectedReference)
        fields = next.values
        selectedEstimate = null
        phaseEnergyMaintenanceKcal = null
        useEstimate = false
        invalidateMacroInputs()
    }
    fun calculateMacroProposal() {
        require(canUseGeneralOption(macroEligible)) {
            if (pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.UNANSWERED) "Answer whether pregnancy or breastfeeding applies before calculating suggestions."
            else "Confirm the general option is suitable, or use manual references."
        }
        val proposal = GoalMacroInput.propose(macroAge, macroWeight, macroWeightUnit, macroTraining, fields[0], macroEligible).getOrThrow()
        macroPreviewExistingValues = fields.drop(1).toList()
        macroPreviewSource = macroSuggestion
        macroPreview = proposal
        macroMethodExpanded = false
    }
    fun chooseMaintenanceEndpoint(estimate: MaintenanceEstimate, endpoint: Double) {
        if (phaseEnergySuggestion != null) invalidatePhaseEnergyInputs()
        val chosen = GoalSetupDraftPolicy.chooseEstimate(GoalSetupDraftPolicy.References(fields, null), estimate, endpoint)
        fields = chosen.values
        selectedEstimate = estimate
        phaseEnergyMaintenanceKcal = endpoint
        phaseEnergyEligible = false
        phaseEnergyProposal = null
        useEstimate = true
        if (macroSuggestion?.caloriesKcal != endpoint) invalidateMacroInputs() else macroPreview = null
        changed = true
        error = null
    }
    fun calculatePhaseEnergyProposal() {
        require(canUseGeneralOption(phaseEnergyEligible)) {
            if (pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.UNANSWERED) "Answer whether pregnancy or breastfeeding applies before calculating a proposal."
            else "Confirm the general option is suitable, or use manual references."
        }
        phaseEnergyProposal = PhaseEnergyInput.propose(
            estimate = selectedEstimate ?: if (eligible) runCatching { estimate() }.getOrNull() else null,
            maintenanceKcal = phaseEnergyMaintenanceKcal,
            intent = GoalIntent.valueOf(intent),
            training = phaseEnergyTraining,
            suitable = phaseEnergyEligible,
        ).getOrThrow()
        phaseEnergyMethodExpanded = false
    }
    fun usePhaseEnergyProposal() {
        require(canUseGeneralOption(phaseEnergyEligible)) { "Review suitability before using this proposal." }
        val proposal = requireNotNull(phaseEnergyProposal)
        val next = GoalSetupDraftPolicy.usePhaseEnergy(fields, proposal, macroSuggestion)
        fields = next.references
        phaseEnergySuggestion = next.phaseEnergySuggestion
        phaseEnergyMaintenanceKcal = proposal.maintenanceKcal
        selectedEstimate = proposal.estimate
        macroSuggestion = next.macroSuggestion
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
        macroEligible = false
        phaseEnergyProposal = null
        phaseEnergyEligible = false
        editor = null
        saveAttempt = null
        changed = true
        error = null
    }
    fun restorePhaseEnergyEditorEntry() {
        phaseEnergyEntryFields?.let { fields = it }
        phaseEnergySuggestion = phaseEnergyEntrySource
        macroSuggestion = phaseEnergyEntryMacroSource
        phaseEnergyTraining = phaseEnergyEntryTraining
        phaseEnergyMaintenanceKcal = phaseEnergyEntryMaintenance
        selectedEstimate = phaseEnergyEntryEstimate
        useEstimate = phaseEnergyEntryUseEstimate
        macroEligible = phaseEnergyEntryMacroEligible
        changed = phaseEnergyEntryChanged
        phaseEnergyProposal = null
        phaseEnergyEligible = false
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
    }
    fun useMacroProposal() {
        require(canUseGeneralOption(macroEligible)) { "Review suitability before using these suggestions." }
        val proposal = requireNotNull(macroPreview)
        fields = listOf(fields[0], proposal.proteinGrams.toString(), proposal.carbohydrateGrams.toString(), proposal.fatGrams.toString())
        macroSuggestion = proposal
        macroPreview = null
        macroPreviewExistingValues = null
        macroPreviewSource = null
        macroEligible = false
        editor = null
        saveAttempt = null
        changed = true
        error = null
    }
    val preview = if (step >= 4 && !manual && canUseGeneralOption(eligible)) runCatching { estimate() }.getOrNull() else null
    fun makeDraft() = GoalSetupDraftPolicy.createPhase(
        phaseId, intent, editing, fields, referenceWeight, targetWeight, minimumWeight, maximumWeight,
        selectedEstimate.takeUnless { phaseEnergySuggestion != null }, Instant.now(), macroSuggestion, phaseEnergySuggestion,
    )
    val sheetState = rememberGuardedEditorSheetState(changed && !saved, saving) { leaving = true }
    ModalBottomSheet(onDismissRequest = ::leave, sheetState = sheetState) {
        if (saved) {
            saveAttempt?.let { attempt ->
                GoalSavedSummaryScreen(
                    phase = attempt.phase, page = savedPage, displayUnit = displayUnit,
                    newPhase = newPhase, replacedPhase = originalPhase != null,
                    methodExpanded = savedMethodExpanded,
                    onToggleMethod = { savedMethodExpanded = !savedMethodExpanded },
                    onNext = { savedPage = (savedPage + 1).coerceAtMost(2) },
                    onBack = { savedPage = (savedPage - 1).coerceAtLeast(0) },
                    onFinish = onDismiss,
                )
            }
        } else {
        Column(Modifier.fillMaxHeight().imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (editor != null) when (editor) { "profile" -> "Review estimate inputs"; "weights" -> "Your weight references"; "daily" -> "Daily references"; "macros" -> "Macro suggestions"; "phaseEnergy" -> "Phase calories"; else -> "Review your goal" } else listOf("Choose your direction", "Your weight references", "Optional maintenance estimate", "Your whole-day activity", "Review your choices", "Confirm changes")[step], style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = ::leave, enabled = !saving) { Icon(Icons.Rounded.Close, "Close goal setup") }
                }
                Text(if (editor == "macros") "Optional macro suggestions" else "${if (newPhase) "New phase" else if (recalculating) "Recalculate references" else if (editing != null) "Edit current goal" else "Goal setup"} | Step ${step + 1} of 6", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { (step + 1) / 6f }, modifier = Modifier.fillMaxWidth())
                if (editor != null) when (editor) {
                    "profile" -> {
                        Text("Only the inputs used by this estimate are shown. Eligibility answers are not saved.")
                        GoalInput("Age at calculation (whole years)", age, !saving) { age = it; macroAge = it; eligible = false; invalidateEstimate(); edited() }
                        GoalInput("Height ($heightUnit)", height, !saving) { height = it; eligible = false; invalidateEstimate(); edited() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("cm", "in").forEach { unit -> FilterChip(selected = heightUnit == unit, enabled = !saving, onClick = { if (heightUnit != unit) { height = ""; heightUnit = unit; eligible = false; invalidateEstimate(); edited() } }, label = { Text(unit) }, modifier = Modifier.heightIn(min = 48.dp)) } }
                        GoalInput("Reviewed weight ($weightUnit)", weight, !saving) { weight = it; macroWeight = it; macroWeightUnit = weightUnit; eligible = false; invalidateEstimate(); edited() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("kg", "lb").forEach { unit -> FilterChip(selected = weightUnit == unit, enabled = !saving, onClick = { if (weightUnit != unit) { weight = ""; macroWeight = ""; weightUnit = unit; macroWeightUnit = unit; eligible = false; invalidateEstimate(); edited() } }, label = { Text(unit) }, modifier = Modifier.heightIn(min = 48.dp)) } }
                        EnergyEquation.entries.forEach { option -> GoalChoice(option.title, equation == option.name, !saving) { equation = option.name; eligible = false; invalidateEstimate(); edited() } }
                        Text("Whole-day activity: ${activity?.let { DailyActivity.valueOf(it).title } ?: "not selected"}")
                        DailyActivity.entries.forEach { option -> GoalChoice("${option.title}: ${option.detail}", activity == option.name, !saving) { activity = option.name; invalidateEstimate(); edited() } }
                        Text("Use this general option only if it is suitable for you and you do not need individual clinical or eating-disorder nutrition support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoalChoice("Confirm this estimate is suitable", eligible, !saving) { if (eligible) { invalidateEstimate(); eligible = false } else eligible = true; edited() }
                        PregnancyBreastfeedingChoice(pregnancyBreastfeeding, !saving, ::answerPregnancyBreastfeeding)
                        CareTeamReferencesButton(!saving, ::useCareTeamReferences)
                    }
                    "weights" -> {
                        Text("These are your chosen values in kilograms. Changing them does not recalculate daily references.")
                        if (selectedEstimate == null) GoalInput("Starting reference (kg)", referenceWeight, !saving) { referenceWeight = it; macroWeight = it; macroWeightUnit = "kg"; invalidateMacroInputs(); edited() }
                        else Text("Starting reference: ${goalNumber(selectedEstimate!!.profile.weightKg)} kg, reviewed estimate weight")
                        GoalInput("Chosen target weight (kg)", targetWeight, !saving) { targetWeight = it; edited() }
                        GoalInput("Chosen range minimum (kg)", minimumWeight, !saving) { minimumWeight = it; edited() }
                        GoalInput("Chosen range maximum (kg)", maximumWeight, !saving) { maximumWeight = it; edited() }
                    }
                    "daily" -> {
                        if (careTeamManual) {
                            Text("Enter calories and macros from your care team. Automatic estimates and suggestions were cleared for this setup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (manual || careTeamManual) {
                            Text("Does pregnancy or breastfeeding apply to these nutrition references?", style = MaterialTheme.typography.bodyMedium)
                            PregnancyBreastfeedingChoice(pregnancyBreastfeeding, !saving, ::answerPregnancyBreastfeeding)
                            if (!careTeamManual) CareTeamReferencesButton(!saving, ::useCareTeamReferences)
                        }
                        if (careTeamManual && pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.DOES_NOT_APPLY) {
                            TextButton(onClick = ::reviewGeneralOptionsAgain, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Review general options again") }
                        }
                        val labels = listOf("Daily calories (kcal)", "Daily protein (g)", "Daily carbohydrates (g)", "Daily fat (g)")
                        labels.forEachIndexed { index, label -> GoalInput(label, fields[index], !saving) { value ->
                            if (index == 0) {
                                val edited = GoalSetupDraftPolicy.editPhaseCalories(fields, phaseEnergySuggestion, macroSuggestion, value)
                                fields = edited.references
                                phaseEnergySuggestion = edited.phaseEnergySuggestion
                                macroSuggestion = edited.macroSuggestion
                                phaseEnergyMaintenanceKcal = null
                                phaseEnergyProposal = null
                                phaseEnergyEligible = false
                                macroEligible = false
                                selectedEstimate = null
                                useEstimate = false
                            } else {
                                val current = GoalSetupDraftPolicy.References(fields, selectedEstimate)
                                val next = GoalSetupDraftPolicy.editMacro(current, index, value)
                                fields = requireNotNull(next).values
                                selectedEstimate = next.acceptedEstimate
                                useEstimate = next.acceptedEstimate != null
                            }
                            changed = true; error = null
                            if (index == 0) {
                                macroPreview = null
                                macroPreviewExistingValues = null
                                macroPreviewSource = null
                            } else {
                                val field = listOf(GoalMacroField.PROTEIN, GoalMacroField.CARBOHYDRATE, GoalMacroField.FAT)[index - 1]
                                val reference = GoalSetupDraftPolicy.editMacroReference(fields.drop(1), macroSuggestion, field, value)
                                fields = listOf(fields[0]) + reference.values
                                macroSuggestion = reference.source
                                macroPreview = null
                                macroPreviewExistingValues = null
                                macroPreviewSource = null
                            }
                        } }
                        Text(when {
                            phaseEnergySuggestion != null -> "Calories use the reviewed phase adjustment. Macro values keep their own source."
                            useEstimate -> "Calories use the selected maintenance estimate. Macro values keep their own source."
                            else -> "Daily references keep their own source. Leave a value empty for no reference."
                        }, style = MaterialTheme.typography.bodySmall)
                        if (fields[0].isNotBlank()) {
                            when {
                                phaseEnergySuggestion != null -> PhaseEnergySourceLabel(phaseEnergySuggestion)
                                useEstimate -> Text("Calories · selected maintenance reference", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Text("Calories · member-entered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        MacroDraftSourceSummary(fields.drop(1), macroSuggestion)
                        TextButton(onClick = { editor = "macros"; macroPreview = null; error = null }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Calculate macro suggestions")
                        }
                    }
                    "macros" -> {
                        MacroInputEditor(
                            age = macroAge,
                            weight = macroWeight,
                            weightUnit = macroWeightUnit,
                            training = macroTraining,
                            prefillSource = when {
                                macroInputsChanged -> "Review the age and weight shown for this calculation. This is never the latest recorded weight."
                                macroPrefillSource != null && newPhase -> "Age and weight are prefilled from the previous phase's saved macro inputs; review them for this phase."
                                macroPrefillSource != null -> "Age and weight are prefilled from this phase's saved macro inputs."
                                macroAge.isNotBlank() && macroWeight.isNotBlank() && macroAge == age && macroWeight == weight -> "Age and weight are copied from the maintenance inputs you just reviewed."
                                macroPrefillProfile != null -> "Age and weight are prefilled from the reviewed maintenance profile."
                                macroPrefillPhase?.referenceWeightKg != null -> "Weight is prefilled from the reviewed starting reference. Age still needs your review."
                                else -> "No reviewed age or weight is available to prefill."
                            },
                            calorieBudget = fields[0],
                            priorTraining = if (newPhase) macroPrefillSource?.training else null,
                            suitable = macroEligible,
                            pregnancyBreastfeeding = pregnancyBreastfeeding,
                            enabled = !saving,
                            onAgeChange = { macroAge = it; macroInputsChanged = true; invalidateMacroInputs(); edited() },
                            onWeightChange = { macroWeight = it; macroInputsChanged = true; invalidateMacroInputs(); edited() },
                            onWeightUnitChange = { unit -> if (unit != macroWeightUnit) { macroWeightUnit = unit; macroWeight = ""; macroInputsChanged = true; invalidateMacroInputs(); edited() } },
                            onTrainingChange = { value -> if (macroTraining != value) { macroTraining = value; invalidateMacroInputs(); edited() } },
                            onSuitableChange = { macroEligible = !macroEligible; macroPreview = null; macroPreviewExistingValues = null; macroPreviewSource = null; error = null; edited() },
                            onPregnancyBreastfeedingChange = ::answerPregnancyBreastfeeding,
                            onCareTeamReferences = ::useCareTeamReferences,
                        )
                        macroPreview?.let { proposal ->
                            MacroSuggestionPreview(
                                proposal = proposal,
                                existing = macroPreviewExistingValues.orEmpty(),
                                existingSource = macroPreviewSource,
                                methodExpanded = macroMethodExpanded,
                                onToggleMethod = { macroMethodExpanded = !macroMethodExpanded },
                            )
                        }
                    }
                    "phaseEnergy" -> {
                        Text("This optional phase estimate uses the reviewed maintenance profile and one endpoint you choose. It does not predict weight change or add exercise calories.", style = MaterialTheme.typography.bodySmall)
                        PhaseEnergyMaintenanceChoice(
                            estimate = selectedEstimate ?: preview,
                            selectedKcal = phaseEnergyMaintenanceKcal,
                            enabled = !saving,
                            onChoose = { kcal ->
                                if (phaseEnergyMaintenanceKcal != kcal) invalidatePhaseEnergyInputs(clearSelectedEstimate = false)
                                phaseEnergyMaintenanceKcal = kcal
                                phaseEnergyEligible = false
                                phaseEnergyProposal = null
                                phaseEnergyMethodExpanded = false
                                error = null
                                edited()
                            },
                        )
                        PhaseEnergyTrainingChoice(
                            training = phaseEnergyTraining,
                            priorTraining = if (newPhase || intentChangedEnergySource != null) previous?.phaseEnergySuggestion?.training else null,
                            enabled = !saving,
                            onTrainingChange = { value ->
                                if (phaseEnergyTraining != value) {
                                    invalidatePhaseEnergyInputs(clearSelectedEstimate = false)
                                    phaseEnergyTraining = value
                                    phaseEnergyEligible = false
                                    phaseEnergyProposal = null
                                    error = null
                                    edited()
                                }
                            },
                        )
                        Text("This choice is for the phase adjustment only. Whole-day activity remains the calorie basis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Use this general option only if it is suitable for you and you do not need individual clinical or eating-disorder nutrition support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PhaseEnergySuitabilityButton(phaseEnergyEligible, !saving) {
                            phaseEnergyEligible = !phaseEnergyEligible
                            phaseEnergyProposal = null
                            error = null
                        }
                        PregnancyBreastfeedingChoice(pregnancyBreastfeeding, !saving, ::answerPregnancyBreastfeeding)
                        CareTeamReferencesButton(!saving, ::useCareTeamReferences)
                        phaseEnergyProposal?.let { proposal ->
                            PhaseEnergyProposalPreview(
                                suggestion = proposal,
                                methodExpanded = phaseEnergyMethodExpanded,
                                onToggleMethod = { phaseEnergyMethodExpanded = !phaseEnergyMethodExpanded },
                            )
                        }
                        if (selectedEstimate == null && preview == null) {
                            TextButton(onClick = { manual = false; eligible = false; editor = "profile"; error = null }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Review maintenance inputs")
                            }
                        }
                    }
                } else when (step) {
                    0 -> {
                        Text("Your direction does not set a weight or deadline. Lose/Gain can include an optional calorie starting estimate from reviewed maintenance inputs; macro suggestions are reviewed separately.")
                        GoalIntent.entries.forEach { option -> GoalChoice(option.title, intent == option.name, !saving, when (option) {
                            GoalIntent.MAINTAIN -> "Keep a steady direction"
                            GoalIntent.GAIN -> "Choose your own weight and daily references"
                            GoalIntent.LOSE -> "Choose your own weight and daily references"
                            GoalIntent.TRACK -> "Record meals and weight at your own pace"
                        }, goalIntentIcon(option)) {
                            val changing = intent != option.name
                            intent = option.name
                            if (changing) {
                                if (phaseEnergySuggestion != null) invalidatePhaseEnergyInputs()
                                else { phaseEnergyProposal = null; phaseEnergyEligible = false }
                            }
                            edited()
                        } }
                    }
                    1 -> {
                        Text("Optional weight context. Choose only values you want to record; direction does not select a target or range.")
                        GoalReviewRow("Starting reference", if (referenceWeight.isBlank()) "Not set" else "$referenceWeight kg", !saving) { editor = "weights" }
                        GoalReviewRow("Optional target", if (targetWeight.isBlank()) "Not set" else "$targetWeight kg", !saving) { editor = "weights" }
                        GoalReviewRow("Optional range", if (minimumWeight.isBlank() && maximumWeight.isBlank()) "Not set" else "$minimumWeight-$maximumWeight kg", !saving) { editor = "weights" }
                    }
                    2 -> {
                        Text("This estimates maintenance energy only. Manual targets and tracking remain available.")
                        if (newPhase && oldProfile != null) Text("Profile values from the previous phase are shown for review. Confirm this phase's suitability before using them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoalInput("Age (whole years, 19–64)", age, !saving) { age = it; macroAge = it; eligible = false; invalidateEstimate(); edited() }
                        GoalInput("Height ($heightUnit)", height, !saving) { height = it; eligible = false; invalidateEstimate(); edited() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("cm", "in").forEach { unit -> FilterChip(selected = heightUnit == unit, enabled = !saving, onClick = { if (heightUnit != unit) { height = ""; heightUnit = unit; eligible = false; invalidateEstimate(); edited() } }, label = { Text(unit) }, modifier = Modifier.heightIn(min = 48.dp)) } }
                        GoalInput("Reviewed current weight ($weightUnit)", weight, !saving) { weight = it; macroWeight = it; macroWeightUnit = weightUnit; eligible = false; invalidateEstimate(); edited() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("kg", "lb").forEach { unit -> FilterChip(selected = weightUnit == unit, enabled = !saving, onClick = { if (weightUnit != unit) { weight = ""; macroWeight = ""; weightUnit = unit; macroWeightUnit = unit; eligible = false; invalidateEstimate(); invalidateMacroInputs(); edited() } }, label = { Text(unit) }, modifier = Modifier.heightIn(min = 48.dp)) } }
                        Text("The historical equation uses sex-based coefficients, not gender identity. Choose only if one is suitable; otherwise use manual targets.")
                        EnergyEquation.entries.forEach { option -> GoalChoice(option.title, equation == option.name, !saving) { equation = option.name; eligible = false; invalidateEstimate(); edited() } }
                        GoalChoice("This general maintenance estimate is suitable for me", eligible, !saving) { if (eligible) { invalidateEstimate(); eligible = false } else eligible = true; edited() }
                        Text("Use this general option only if you do not need individual clinical or eating-disorder nutrition support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PregnancyBreastfeedingChoice(pregnancyBreastfeeding, !saving, ::answerPregnancyBreastfeeding)
                        CareTeamReferencesButton(!saving, ::useCareTeamReferences)
                        Text("Eligibility answers are not saved. Supported input limits are engineering limits, not healthy ranges.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { manual = true; invalidateEstimate(); step = 4; edited() }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Use manual targets instead") }
                    }
                    3 -> {
                        Text("Include work and leisure in this choice. Exercise and strength sessions are not added again.")
                        DailyActivity.entries.forEach { option -> GoalChoice("${option.title}: ${option.detail}", activity == option.name, !saving) { activity = option.name; invalidateEstimate(); edited() } }
                    }
                    4 -> {
                        Text("Review each section, then continue to the final old and new reference summary.")
                        if (manual || careTeamManual) {
                            Text("Does pregnancy or breastfeeding apply to these nutrition references?", style = MaterialTheme.typography.bodyMedium)
                            PregnancyBreastfeedingChoice(pregnancyBreastfeeding, !saving, ::answerPregnancyBreastfeeding)
                            if (!careTeamManual) CareTeamReferencesButton(!saving, ::useCareTeamReferences)
                        }
                        if (careTeamManual && pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.DOES_NOT_APPLY) {
                            TextButton(onClick = ::reviewGeneralOptionsAgain, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Review general options again") }
                        }
                        if (newPhase && previous != null) Text("Manual macro values from the previous phase remain visible for your review. Suggested values require a separate calculation and choice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (newPhase && previous?.phaseEnergySuggestion != null) Text("Previous phase calorie inputs are shown for review only. Choose a maintenance endpoint and calculate a new phase proposal to use one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoalReviewRow(
                            "Weight references",
                            "Starting ${selectedEstimate?.profile?.weightKg?.let { "${goalNumber(it)} kg" } ?: referenceWeight.takeIf(String::isNotBlank)?.let { "$it kg" } ?: "Not set"} | Optional target: ${targetWeight.takeIf(String::isNotBlank)?.let { "$it kg" } ?: "Not set"} | Optional range: ${if (minimumWeight.isBlank() && maximumWeight.isBlank()) "Not set" else "$minimumWeight-$maximumWeight kg"}.",
                            !saving,
                        ) { editor = "weights" }
                        val dailyReview = GoalReviewPresentation.dailyReferences(fields)
                        GoalReferenceStatusLabel(dailyReview)
                        GoalReviewRow("Daily references", GoalReviewPresentation.dailyReferenceValues(fields).joinToString("\n"), !saving) { editor = "daily" }
                        GoalReviewRow("Training and macro suggestions", macroSourceSummary(fields, macroSuggestion), !saving) { macroEligible = false; editor = "macros"; macroPreview = null; error = null }
                        if (intent == GoalIntent.LOSE.name || intent == GoalIntent.GAIN.name) {
                            val phaseCaloriesSummary = GoalReviewPresentation.phaseCaloriesSummary(
                                intent = GoalIntent.valueOf(intent),
                                caloriesKcal = fields[0].trim().replace(',', '.').toDoubleOrNull(),
                                maintenanceSelected = selectedEstimate != null && phaseEnergyMaintenanceKcal != null,
                                proposal = phaseEnergySuggestion,
                            )
                            GoalReviewRow(
                                "Phase calories",
                                phaseCaloriesSummary ?: "Adjustment not chosen",
                                !saving,
                            ) {
                                phaseEnergyEntryTraining = phaseEnergyTraining
                                phaseEnergyEntryMaintenance = phaseEnergyMaintenanceKcal
                                phaseEnergyEntryEstimate = selectedEstimate
                                phaseEnergyEntryFields = fields.toList()
                                phaseEnergyEntrySource = phaseEnergySuggestion
                                phaseEnergyEntryMacroSource = macroSuggestion
                                phaseEnergyEntryMacroEligible = macroEligible
                                phaseEnergyEntryChanged = changed
                                phaseEnergyEntryUseEstimate = useEstimate
                                phaseEnergyEligible = false
                                phaseEnergyProposal = null
                                editor = "phaseEnergy"
                                error = null
                            }
                            if (phaseEnergySuggestion == null) {
                                Text("To apply a phase adjustment: choose a maintenance endpoint, calculate a proposal, then select Use calorie proposal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (preview != null) {
                            GoalReviewRow("Estimate inputs", "${preview.profile.age} years | ${goalNumber(preview.profile.heightCm)} cm | ${goalNumber(preview.profile.weightKg)} kg | ${preview.profile.equation.title} | ${preview.profile.activity.title}", !saving) { editor = "profile" }
                        } else if (phaseEnergySuggestion != null) {
                            val profile = phaseEnergySuggestion!!.estimate.profile
                            GoalReviewRow("Saved maintenance inputs", "${profile.age} years | ${goalNumber(profile.heightCm)} cm | ${goalNumber(profile.weightKg)} kg | ${profile.equation.title} | ${profile.activity.title}", !saving) { editor = "profile" }
                        } else if (editing?.acceptedEstimate != null) {
                            val profile = editing.acceptedEstimate.profile
                            GoalReviewRow("Saved maintenance inputs", "${profile.age} years | ${goalNumber(profile.heightCm)} cm | ${goalNumber(profile.weightKg)} kg | ${profile.equation.title} | ${profile.activity.title}", !saving) { editor = "profile" }
                        } else if (intentChangedEnergySource != null) {
                            val profile = intentChangedEnergySource.estimate.profile
                            GoalReviewRow("Previous maintenance inputs", "${profile.age} years | ${goalNumber(profile.heightCm)} cm | ${goalNumber(profile.weightKg)} kg | ${profile.equation.title} | ${profile.activity.title}", !saving) { editor = "profile" }
                        } else {
                            GoalReviewRow("Optional maintenance estimate", "Not used | daily references remain manual", !saving) { manual = false; step = 2; error = null }
                        }
                        if (preview != null) {
                            val estimate = preview
                            val selectedEndpoint = phaseEnergyMaintenanceKcal?.takeIf { it == estimate.minimumKcal || it == estimate.maximumKcal }
                            Text(
                                if (selectedEndpoint != null) "Maintenance estimate ${goalNumber(estimate.minimumKcal)}-${goalNumber(estimate.maximumKcal)} kcal/day. Selected endpoint: ${goalNumber(selectedEndpoint)} kcal/day."
                                else "Maintenance estimate ${goalNumber(estimate.minimumKcal)}-${goalNumber(estimate.maximumKcal)} kcal/day. Choose an endpoint to use estimated calories."
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val lowerSelected = phaseEnergyMaintenanceKcal == estimate.minimumKcal
                                val upperSelected = phaseEnergyMaintenanceKcal == estimate.maximumKcal
                                TextButton(onClick = { chooseMaintenanceEndpoint(estimate, estimate.minimumKcal) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = lowerSelected; stateDescription = if (lowerSelected) "Selected maintenance endpoint" else "Not selected" }, enabled = !saving) { Text(if (lowerSelected) "Lower end · selected" else "Choose lower end") }
                                TextButton(onClick = { chooseMaintenanceEndpoint(estimate, estimate.maximumKcal) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = upperSelected; stateDescription = if (upperSelected) "Selected maintenance endpoint" else "Not selected" }, enabled = !saving) { Text(if (upperSelected) "Upper end · selected" else "Choose upper end") }
                            }
                            TextButton(onClick = { methodDetails = !methodDetails }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (methodDetails) "Hide method and inputs" else "Method and inputs") }
                            if (methodDetails) MaintenanceDetails(estimate)
                        } else if (phaseEnergySuggestion != null || editing?.acceptedEstimate != null || intentChangedEnergySource != null) {
                            TextButton(onClick = { methodDetails = !methodDetails }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (methodDetails) "Hide method and inputs" else "Method and inputs") }
                            if (methodDetails) {
                                phaseEnergySuggestion?.let { PhaseEnergyMethodDetails(it) }
                                if (phaseEnergySuggestion == null) editing?.acceptedEstimate?.let { MaintenanceDetails(it) }
                                intentChangedEnergySource?.let {
                                    Text("Previous phase source · retained only as review context", style = MaterialTheme.typography.bodySmall)
                                    PhaseEnergyMethodDetails(it)
                                }
                            }
                        }
                    }
                    5 -> {
                        saveAttempt?.let { attempt ->
                            GoalConfirmationContent(originalPhase, originalTargets, attempt.phase, newPhase, originalHistoryCount)
                        }
                    }
                }
                error?.let { Text("Could not continue: $it", color = MaterialTheme.colorScheme.error) }
                if (saving) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Saving your goal…") }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                    if (saving || saved) return@Button
                    if (editor == "macros") {
                        when {
                            macroPreview != null -> useMacroProposal()
                            macroTraining == GoalTraining.MANUAL || macroTraining == GoalTraining.DEMANDING -> {
                                invalidateMacroInputs()
                                editor = "daily"
                                manual = true
                                error = null
                            }
                            else -> runCatching { calculateMacroProposal() }
                                .onFailure { macroPreview = null; error = it.message ?: "Check the reviewed macro inputs." }
                        }
                    }
                    else if (editor == "phaseEnergy") {
                        when {
                            phaseEnergyProposal != null -> usePhaseEnergyProposal()
                            else -> runCatching { calculatePhaseEnergyProposal() }
                                .onFailure { phaseEnergyProposal = null; error = it.message ?: "Review the maintenance inputs and phase context." }
                        }
                    }
                    else if (editor != null) { editor = null; error = null }
                    else if (step < 5) {
                        val validation = runCatching {
                            if (step == 2) {
                                require(canUseGeneralOption(eligible)) {
                                    if (pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.UNANSWERED) "Answer whether pregnancy or breastfeeding applies before continuing."
                                    else "Confirm suitability, or use manual targets."
                                }
                                // Validate profile before activity selection without retaining a default activity.
                                MaintenanceCalculator.profile(age, height, heightUnit, weight, weightUnit, equation?.let(EnergyEquation::valueOf), DailyActivity.LIGHT)
                            }
                            if (step == 3) {
                                require(canUseGeneralOption(eligible)) { "Review suitability before continuing." }
                                estimate()
                            }
                            if (step == 4) {
                                if (pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.APPLIES) {
                                    require(phaseEnergySuggestion == null && selectedEstimate == null && macroSuggestion == null) {
                                        "Use care-team references because pregnancy or breastfeeding applies."
                                    }
                                }
                                if (macroSuggestion != null && macroSuggestion?.caloriesKcal != fields[0].toDoubleOrNull()) invalidateMacroInputs()
                                saveAttempt = GoalSaveAttempt(makeDraft())
                            }
                        }
                        if (validation.isFailure) error = validation.exceptionOrNull()?.message else {
                            if (step == 2) manual = false
                            step++
                            error = null
                        }
                    } else {
                        val attempt = saveAttempt
                        if (attempt != null && attempt.begin()) {
                            saving = true; error = null
                            scope.launch { try {
                                    error = onConfirm(attempt.phase, newPhase)
                                    saved = error == null
                                }
                                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (_: Exception) { error = "Your goal could not be saved. Try again." }
                                finally { attempt.finish(saved); saving = false } }
                        }
                    }
                }) { Text(if (editor == "macros") when {
                    macroPreview != null -> "Use suggestions"
                    macroTraining == GoalTraining.MANUAL || macroTraining == GoalTraining.DEMANDING -> "Edit manual references"
                    else -> "Calculate suggestions"
                } else if (editor == "phaseEnergy") when {
                    phaseEnergyProposal != null -> "Use calorie proposal"
                    else -> "Calculate proposal"
                } else if (editor != null) "Done" else if (step == 5) {
                    when {
                        error != null -> "Retry save"
                        GoalReviewPresentation.dailyReferences(saveAttempt?.phase?.targets).completeness != GoalReferenceCompleteness.SET ->
                            if (newPhase) "Start phase with unset values" else "Save with unset values"
                        newPhase -> "Start new phase"
                        editing != null -> "Save changes"
                        else -> "Set up goal"
                    }
                } else if (step == 4 && GoalReviewPresentation.dailyReferences(fields).completeness != GoalReferenceCompleteness.SET) "Continue with unset values" else "Continue") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (editor != null) TextButton(enabled = !saving, onClick = {
                        when (editor) {
                            "macros" -> { macroEligible = false; macroPreview = null; macroPreviewExistingValues = null; macroPreviewSource = null }
                            "phaseEnergy" -> {
                                restorePhaseEnergyEditorEntry()
                            }
                        }
                        editor = null
                        error = null
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                    else if (step > 0) TextButton(enabled = !saving, onClick = { if (step == 4 && manual) step = 2 else step--; saveAttempt = null; error = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                    else Spacer(Modifier.weight(1f))
                    if (editor == "phaseEnergy") TextButton(enabled = !saving, onClick = { restorePhaseEnergyEditorEntry(); editor = "daily"; error = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit calories manually") }
                    if (editor == null) TextButton(enabled = !saving, onClick = ::leave, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (step == 0 && !changed) "Skip setup" else if (step == 5 && newPhase) "Cancel new phase" else "Leave setup") }
                }
            }
        }
        }
    }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false; scope.launch { sheetState.show() } }, title = { Text("Leave without saving?") }, text = { Text("Your setup answers and edits will be discarded.") }, confirmButton = { Button(onClick = { leaving = false; scope.launch { sheetState.show() } }) { Text("Keep editing") } }, dismissButton = { TextButton(onClick = { leaving = false; onDismiss() }) { Text("Leave without saving") } })
}

@Composable
private fun GoalInput(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun GoalReviewRow(title: String, summary: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, "Edit $title")
        }
    }
}

@Composable
private fun GoalChoice(label: String, selected: Boolean, enabled: Boolean, supporting: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { this.selected = selected; stateDescription = if (selected) "Selected" else "Not selected" }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, "Selected")
        }
    }
}

private fun macroSourceSummary(fields: List<String>, source: MacroSuggestion?): String {
    return GoalReviewPresentation.macroSources(fields, source).joinToString(" · ")
}
