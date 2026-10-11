package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.metria.*
import app.metria.ui.components.goalDisplayNumber
import app.metria.ui.components.rememberGuardedEditorSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalWeightEditorSheet(phase: GoalPhase, onDismiss: () -> Unit, onReviewReferences: (GoalPhase) -> Unit, onSave: suspend (GoalPhase) -> String?) {
    var intent by remember { mutableStateOf(phase.intent) }
    var target by remember { mutableStateOf(phase.targetWeightKg?.toString().orEmpty()) }
    var minimum by remember { mutableStateOf(phase.rangeMinimumKg?.toString().orEmpty()) }
    var maximum by remember { mutableStateOf(phase.rangeMaximumKg?.toString().orEmpty()) }
    var showRange by remember { mutableStateOf(phase.rangeMinimumKg != null) }
    var changeDirection by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val changed = intent != phase.intent || target != phase.targetWeightKg?.toString().orEmpty() || minimum != phase.rangeMinimumKg?.toString().orEmpty() || maximum != phase.rangeMaximumKg?.toString().orEmpty()
    val scope = rememberCoroutineScope()
    val sheetState = rememberGuardedEditorSheetState(changed, saving) { leaving = true }
    fun leave() { if (!saving) { if (changed) leaving = true else onDismiss() } }
    ModalBottomSheet(onDismissRequest = ::leave, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight().imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit weight goal", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = ::leave, enabled = !saving) { Icon(Icons.Rounded.Close, "Close weight goal editor") }
                }
                Text(intent.title, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { changeDirection = !changeDirection }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (changeDirection) "Hide direction choices" else "Change direction") }
                if (changeDirection) GoalIntent.entries.forEach { option ->
                    FilterChip(selected = intent == option, onClick = { intent = option; error = null }, enabled = !saving, label = { Text(option.title) }, leadingIcon = if (intent == option) ({ Icon(Icons.Rounded.CheckCircle, "Selected") }) else null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
                if (intent != phase.intent && phase.phaseEnergySuggestion != null) {
                    Text(
                        "Changing direction removes this phase calorie source. Review calorie and macro references before saving the new direction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reviewed starting reference", style = MaterialTheme.typography.labelLarge)
                        Text(phase.referenceWeightKg?.let { "${goalDisplayNumber(it)} kg" } ?: "No starting reference", style = MaterialTheme.typography.headlineSmall)
                        Text("Review setup to change this reference.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(target, { target = it; error = null }, label = { Text("Your target weight (kg)") }, singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Text("Leave empty to track without a weight target. Your daily calorie and macro references stay the same.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showRange = !showRange }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (showRange) "Hide your weight range" else "Add an optional weight range") }
                if (showRange) {
                    OutlinedTextField(minimum, { minimum = it; error = null }, label = { Text("Range minimum (kg)") }, singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(maximum, { maximum = it; error = null }, label = { Text("Range maximum (kg)") }, singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Text("This is your own chosen range.", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saving) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Saving your goal…") }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !saving && changed, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                    if (intent != phase.intent && phase.phaseEnergySuggestion != null) {
                        val validationBase = phase.copy(phaseEnergySuggestion = null, macroSuggestion = null, acceptedEstimate = null)
                        val result = GoalWeightInput.parse(validationBase, intent, target, minimum, maximum)
                        if (result.isFailure) error = result.exceptionOrNull()?.message
                        else {
                            val validated = result.getOrThrow()
                            onReviewReferences(phase.copy(
                                intent = intent,
                                targetWeightKg = validated.targetWeightKg,
                                rangeMinimumKg = validated.rangeMinimumKg,
                                rangeMaximumKg = validated.rangeMaximumKg,
                            ))
                        }
                    } else {
                    val result = GoalWeightInput.parse(phase, intent, target, minimum, maximum)
                    if (result.isFailure) error = result.exceptionOrNull()?.message else {
                        saving = true; error = null
                        scope.launch { try { error = onSave(result.getOrThrow()); if (error == null) onDismiss() }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Your goal could not be saved. Try again." }
                            finally { saving = false } }
                    }
                    }
                }) { Text(if (intent != phase.intent && phase.phaseEnergySuggestion != null) "Review calorie references" else "Save weight goal") }
                TextButton(onClick = ::leave, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
            }
        }
    }
    if (leaving) AlertDialog(onDismissRequest = { leaving = false; scope.launch { sheetState.show() } }, title = { Text("Leave without saving?") }, text = { Text("Your weight goal changes will be discarded.") }, confirmButton = { Button(onClick = { leaving = false; scope.launch { sheetState.show() } }) { Text("Keep editing") } }, dismissButton = { TextButton(onClick = { leaving = false; onDismiss() }) { Text("Leave without saving") } })
}
