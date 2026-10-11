package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import app.metria.GoalPhase
import app.metria.GoalMacroField
import app.metria.NutritionTargets
import app.metria.WeightUnits
import app.metria.ui.theme.MetriaColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalHubCurrentCard(
    phase: GoalPhase,
    latestWeightKg: Double?,
    displayUnit: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(kg: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(kg) else kg)} $unit"
    val markerKg = latestWeightKg ?: phase.referenceWeightKg
    val markerLabel = markerKg?.let { "${if (latestWeightKg != null) "Latest recorded weight" else "Starting reference"} · ${weight(it)}" }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(goalIntentIcon(phase.intent), null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(phase.intent.title, style = MaterialTheme.typography.titleMedium)
                    Text("Since ${formatGoalDate(phase.confirmedAt)} · In progress", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
                androidx.compose.material3.IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Edit current weight goal" },
                ) { Icon(Icons.Rounded.Edit, contentDescription = null) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (latestWeightKg != null) "Latest recorded weight" else "Starting reference", style = MaterialTheme.typography.labelMedium, color = MetriaColors.InkMuted)
                Text(latestWeightKg?.let(::weight) ?: phase.referenceWeightKg?.let(::weight) ?: "No weight reference", style = MaterialTheme.typography.headlineSmall)
            }
            if (latestWeightKg != null) phase.referenceWeightKg?.let {
                Text("Starting reference · ${weight(it)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            }
            phase.targetWeightKg?.let { Text("Your target · ${weight(it)}", style = MaterialTheme.typography.bodySmall) }
            if (phase.rangeMinimumKg != null && phase.rangeMaximumKg != null) {
                Text("Your chosen range · ${weight(phase.rangeMinimumKg)} to ${weight(phase.rangeMaximumKg)}", style = MaterialTheme.typography.bodySmall)
                if (phase.rangeMaximumKg > phase.rangeMinimumKg && markerKg != null && markerLabel != null) {
                    if (markerKg < phase.rangeMinimumKg || markerKg > phase.rangeMaximumKg) {
                        Text(if (markerKg < phase.rangeMinimumKg) "Below chosen range" else "Above chosen range", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    }
                    GoalWeightRangeMarker(markerKg, phase.rangeMinimumKg, phase.rangeMaximumKg, markerLabel,
                        weight(phase.rangeMinimumKg), weight(phase.rangeMaximumKg))
                }
            } else if (markerKg != null && markerLabel != null && phase.referenceWeightKg != null &&
                phase.targetWeightKg != null && phase.referenceWeightKg != phase.targetWeightKg
            ) {
                Text("Starting reference to target", style = MaterialTheme.typography.bodySmall)
                val low = minOf(phase.referenceWeightKg, phase.targetWeightKg)
                val high = maxOf(phase.referenceWeightKg, phase.targetWeightKg)
                if (markerKg < low || markerKg > high) {
                    Text(if (markerKg < low) "Below start-to-target interval" else "Above start-to-target interval", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
                GoalWeightRangeMarker(markerKg, phase.referenceWeightKg, phase.targetWeightKg, markerLabel,
                    "Start ${weight(phase.referenceWeightKg)}", "Target ${weight(phase.targetWeightKg)}")
            }
            TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Phase details")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
fun GoalHubPreviousPhaseRow(phase: GoalPhase, displayUnit: String, onOpen: () -> Unit) {
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(kg: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(kg) else kg)} $unit"
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(phase.intent.title, style = MaterialTheme.typography.titleSmall)
                Text(if (phase.endedAt == null) "Started ${formatGoalDate(phase.confirmedAt)} - Period unavailable" else "${formatGoalDate(phase.confirmedAt)} to ${formatGoalDate(phase.endedAt)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                phase.targetWeightKg?.let { Text("Chosen target · ${weight(it)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted) }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun GoalDailyReferencesCard(
    targets: NutritionTargets?,
    estimatedCalories: Boolean,
    macroSuggestion: app.metria.MacroSuggestion?,
    phaseEnergySuggestion: app.metria.PhaseEnergySuggestion?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Daily references", style = MaterialTheme.typography.titleMedium)
                    Text(
                        targets?.caloriesKcal?.let { "${goalDisplayNumber(it)} kcal / day" } ?: "No calorie reference",
                        style = MaterialTheme.typography.bodySmall,
                        color = MetriaColors.InkMuted,
                    )
                    Text(
                        when {
                            targets == null -> "No daily targets set"
                            targets.caloriesKcal == null -> "Calories not set"
                            phaseEnergySuggestion != null -> "Phase estimate"
                            estimatedCalories -> "Maintenance estimate"
                            else -> "Entered by you"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (estimatedCalories && targets?.caloriesKcal != null) MetriaColors.Estimated else MetriaColors.InkMuted,
                    )
                }
                TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "Daily reference details"
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }) {
                    Text(if (expanded) "Hide" else "Details")
                }
            }
            if (expanded) {
                Text(
                    when {
                        targets == null -> "No daily references saved"
                        targets.caloriesKcal == null -> "Calories not set"
                        phaseEnergySuggestion != null -> "Calories · phase-adjusted starting estimate"
                        estimatedCalories -> "Calories · confirmed maintenance estimate"
                        else -> "Daily references"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MetriaColors.InkMuted,
                )
                GoalReferenceValue("Protein", targets?.proteinGrams, "g / day")
                if (targets?.proteinGrams != null) MacroFieldSource("Protein", macroSuggestion, GoalMacroField.PROTEIN)
                GoalReferenceValue("Carbohydrates", targets?.carbohydrateGrams, "g / day")
                if (targets?.carbohydrateGrams != null) MacroFieldSource("Carbohydrates", macroSuggestion, GoalMacroField.CARBOHYDRATE)
                GoalReferenceValue("Fat", targets?.fatGrams, "g / day")
                if (targets?.fatGrams != null) MacroFieldSource("Fat", macroSuggestion, GoalMacroField.FAT)
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (targets == null) "Set daily references" else "Edit daily references")
                }
            }
        }
    }
}

@Composable
fun GoalPhaseReferences(phase: GoalPhase) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily references", style = MaterialTheme.typography.titleMedium)
        Text("Calories · ${phase.targets?.caloriesKcal?.let { "${goalDisplayNumber(it)} kcal / day" } ?: "No target"}", style = MaterialTheme.typography.bodyMedium)
        if (phase.targets?.caloriesKcal != null) {
            when {
                phase.phaseEnergySuggestion != null -> PhaseEnergySourceLabel(phase.phaseEnergySuggestion)
                phase.acceptedEstimate != null -> Text("Calories · confirmed maintenance estimate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text("Calories · member-entered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GoalReferenceValue("Protein", phase.targets?.proteinGrams, "g / day")
        if (phase.targets?.proteinGrams != null) MacroFieldSource("Protein", phase.macroSuggestion, GoalMacroField.PROTEIN)
        GoalReferenceValue("Carbohydrates", phase.targets?.carbohydrateGrams, "g / day")
        if (phase.targets?.carbohydrateGrams != null) MacroFieldSource("Carbohydrates", phase.macroSuggestion, GoalMacroField.CARBOHYDRATE)
        GoalReferenceValue("Fat", phase.targets?.fatGrams, "g / day")
        if (phase.targets?.fatGrams != null) MacroFieldSource("Fat", phase.macroSuggestion, GoalMacroField.FAT)
        Text(
            when {
                phase.targets == null -> "No daily references saved"
                phase.targets.caloriesKcal == null -> "Calories not set"
                phase.phaseEnergySuggestion != null -> "Phase estimate · optional starting reference"
                phase.acceptedEstimate != null -> "Calories · confirmed estimate"
                else -> "Daily references"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MetriaColors.InkMuted,
        )
    }
}

@Composable
private fun GoalReferenceValue(label: String, value: Double?, unit: String) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val textValue = value?.let { "${goalDisplayNumber(it)} $unit" } ?: "Not set"
        if (maxWidth < 320.dp || fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text(textValue, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Text(textValue, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun GoalMethodDisclosure(expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            contentDescription = "Method and input details"
            stateDescription = if (expanded) "Expanded" else "Collapsed"
        }) {
            Icon(Icons.Rounded.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "Hide method and inputs" else "Method and inputs")
        }
        if (expanded) content()
    }
}

private fun formatGoalDate(instant: java.time.Instant): String =
    instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
