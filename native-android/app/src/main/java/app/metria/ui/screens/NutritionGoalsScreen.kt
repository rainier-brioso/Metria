package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.metria.NutritionTargets
import app.metria.NutritionTargetInput
import app.metria.GoalMacroField
import app.metria.GoalSetupDraftPolicy
import app.metria.MacroSuggestion
import app.metria.PhaseEnergySuggestion
import app.metria.ui.components.MacroFieldSource
import app.metria.ui.components.PhaseEnergySourceLabel
import app.metria.ui.components.rememberGuardedEditorSheetState
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualNutritionTargetsSheet(targets: NutritionTargets?, caloriesEstimated: Boolean, macroSuggestion: MacroSuggestion?, phaseEnergySuggestion: PhaseEnergySuggestion?, onDismiss: () -> Unit, onSave: suspend (NutritionTargets?, Boolean, Set<GoalMacroField>) -> String?) {
    val initialFields = listOf(targets?.caloriesKcal, targets?.proteinGrams, targets?.carbohydrateGrams, targets?.fatGrams).map { it?.toString().orEmpty() }
    var fields by remember(targets) { mutableStateOf(initialFields) }
    var calorieWasEdited by remember { mutableStateOf(false) }
    var manuallyEditedMacros by remember { mutableStateOf(emptySet<GoalMacroField>()) }
    var macroSource by remember(targets, macroSuggestion) { mutableStateOf(macroSuggestion) }
    var energySource by remember(targets, phaseEnergySuggestion) { mutableStateOf(phaseEnergySuggestion) }
    var editing by remember { mutableStateOf(true) }
    var showMore by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun requestLeave() {
        if (saving) return
        if (fields != initialFields || calorieWasEdited || manuallyEditedMacros.isNotEmpty()) leaving = true else onDismiss()
    }
    fun save(value: NutritionTargets?) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            try {
                error = onSave(value, calorieWasEdited, manuallyEditedMacros)
                if (error == null) onDismiss()
            } catch (_: kotlinx.coroutines.CancellationException) {
                throw kotlinx.coroutines.CancellationException()
            } catch (_: Exception) {
                error = "Your targets could not be saved. Try again."
            } finally { saving = false }
        }
    }
    val dirty = fields != initialFields || calorieWasEdited || manuallyEditedMacros.isNotEmpty()
    val sheetState = rememberGuardedEditorSheetState(dirty, saving) { leaving = true }
    ModalBottomSheet(onDismissRequest = ::requestLeave, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight().imePadding()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (editing) "Choose daily targets" else "Your nutrition goals", style = MaterialTheme.typography.headlineMedium)
                Text("Optional daily references you choose. Tracking works without targets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editing) {
                    val labels = listOf("Calories (kcal)", "Protein (g)", "Carbohydrates (g)", "Fat (g)")
                    labels.forEachIndexed { index, label ->
                        if (index < 2 || showMore || fields[index].isNotBlank()) {
                            OutlinedTextField(value = fields[index], onValueChange = { value ->
                                val updated = fields.toMutableList().also { it[index] = value }
                                when (index) {
                                    0 -> {
                                        calorieWasEdited = true
                                        energySource = null
                                        val invalidated = GoalSetupDraftPolicy.invalidateMacroReferences(updated.drop(1), macroSource)
                                        fields = listOf(updated[0]) + invalidated.values
                                        macroSource = invalidated.source
                                    }
                                    else -> {
                                        val field = listOf(GoalMacroField.PROTEIN, GoalMacroField.CARBOHYDRATE, GoalMacroField.FAT)[index - 1]
                                        val edited = GoalSetupDraftPolicy.editMacroReference(updated.drop(1), macroSource, field, value)
                                        fields = listOf(updated[0]) + edited.values
                                        macroSource = edited.source
                                        manuallyEditedMacros = manuallyEditedMacros + field
                                    }
                                }
                                error = null
                            }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                    TextButton(onClick = { showMore = !showMore }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (showMore) "Hide additional targets" else "Additional targets") }
                    Text(
                        when {
                            calorieWasEdited -> "Changing calories removes linked suggestions. Enter values here or recalculate in goal setup."
                            energySource != null -> "Calories use an optional phase estimate. Changing calories removes that source and switches to a manual reference."
                            caloriesEstimated -> "Calories currently use your confirmed maintenance estimate. Changing calories switches their source to member-entered. Other targets keep their own source unless you edit them."
                            else -> "Leave a value empty to track it without a target. Macro suggestions are optional; values you edit are member-entered."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val names = listOf("Calories", "Protein", "Carbohydrates", "Fat")
                    val values = listOf(targets?.caloriesKcal, targets?.proteinGrams, targets?.carbohydrateGrams, targets?.fatGrams)
                    names.forEachIndexed { index, name ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(name, style = MaterialTheme.typography.labelLarge)
                                Text(values[index]?.let { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it) + if (index == 0) " kcal / day" else " g / day" } ?: "No target", style = MaterialTheme.typography.titleLarge)
                                if (index in 1..3 && values[index] != null) MacroFieldSource(name, macroSource, listOf(GoalMacroField.PROTEIN, GoalMacroField.CARBOHYDRATE, GoalMacroField.FAT)[index - 1])
                            }
                        }
                    }
                    if (targets?.caloriesKcal != null) {
                        when {
                            energySource != null -> PhaseEnergySourceLabel(energySource)
                            caloriesEstimated -> Text("Calories · confirmed maintenance estimate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> Text("Calories · member-entered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Daily references are stored on this phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (targets != null) TextButton(onClick = { removing = true }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Track without targets") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saving) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Saving your targets…") }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (!editing) editing = true else {
                        val result = NutritionTargetInput.parse(fields)
                        if (result.isFailure) error = result.exceptionOrNull()?.message ?: "Check your target values."
                        else if (result.getOrNull() == null && targets != null) removing = true
                        else save(result.getOrNull())
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (editing) "Save daily targets" else "Edit targets") }
                TextButton(onClick = ::requestLeave, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (editing) "Cancel" else "Done") }
            }
        }
    }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false; scope.launch { sheetState.show() } }, title = { Text("Leave without saving?") }, text = { Text("Your changes to daily targets will be lost.") }, confirmButton = { Button(onClick = { leaving = false; scope.launch { sheetState.show() } }) { Text("Keep editing") } }, dismissButton = { TextButton(onClick = { leaving = false; onDismiss() }) { Text("Leave without saving") } })
    if (removing) AlertDialog(onDismissRequest = { removing = false }, title = { Text("Track without targets?") }, text = { Text("This removes your daily nutrition targets. Your recorded meals and weights stay unchanged.") }, confirmButton = { Button(onClick = { removing = false; save(null) }) { Text("Remove targets") } }, dismissButton = { TextButton(onClick = { removing = false }) { Text("Keep targets") } })
}
