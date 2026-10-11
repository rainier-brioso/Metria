package app.metria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.metria.GoalIntent
import app.metria.GoalPhase
import app.metria.GoalMacroField
import app.metria.NutritionTargets
import app.metria.WeightUnits
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun goalDisplayNumber(value: Double) = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(value)
internal fun goalIntentIcon(intent: GoalIntent): ImageVector = when (intent) {
    GoalIntent.MAINTAIN -> Icons.Rounded.TrackChanges
    GoalIntent.GAIN -> Icons.AutoMirrored.Rounded.TrendingUp
    GoalIntent.LOSE -> Icons.AutoMirrored.Rounded.TrendingDown
    GoalIntent.TRACK -> Icons.Rounded.Spa
}

@Composable
fun GoalSummaryCard(
    phase: GoalPhase, latestWeightKg: Double? = null, onEdit: (() -> Unit)? = null,
    displayUnit: String = "kg", onUpdateWeight: (() -> Unit)? = null,
    onOpenDetails: (() -> Unit)? = null,
) {
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(value: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(value) else value)} $unit"
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(goalIntentIcon(phase.intent), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(phase.intent.title, style = MaterialTheme.typography.titleLarge)
                    Text("Since ${phase.confirmedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}", style = MaterialTheme.typography.bodySmall)
                }
                if (onEdit != null) IconButton(onClick = onEdit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Rounded.Edit, "Edit weight goal") }
            }
            val shownWeight = latestWeightKg ?: phase.referenceWeightKg
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (latestWeightKg != null) "Latest recorded weight" else "Starting reference", style = MaterialTheme.typography.labelLarge)
                Text(shownWeight?.let(::weight) ?: "No weight reference", style = MaterialTheme.typography.displaySmall)
                phase.targetWeightKg?.let { Text("Your target · ${weight(it)}", style = MaterialTheme.typography.titleMedium) }
            }
            phase.rangeMinimumKg?.let { low ->
                val high = requireNotNull(phase.rangeMaximumKg)
                Text("Your chosen range", style = MaterialTheme.typography.labelLarge)
                // This represents a member-entered interval, never a recommended weight band.
                if (shownWeight != null && high > low) {
                    val position = ((shownWeight - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(progress = { position }, modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Weight position in your chosen range, ${weight(low)} to ${weight(high)}" })
                    if (shownWeight < low || shownWeight > high) Text(if (shownWeight < low) "Below your chosen range" else "Above your chosen range", style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(weight(low), Modifier.weight(1f)); Text(weight(high), Modifier.weight(1f)) }
            }
            if (latestWeightKg != null) phase.referenceWeightKg?.let { Text("Starting reference · ${weight(it)}", style = MaterialTheme.typography.bodySmall) }
            onUpdateWeight?.let { OutlinedButton(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Update weight") } }
            onOpenDetails?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Goal and daily references") } }
        }
    }
}

@Composable
fun DailyReferenceCard(targets: NutritionTargets?, estimated: Boolean, onEdit: (() -> Unit)? = null, macroSuggestion: app.metria.MacroSuggestion? = null, phaseEnergySuggestion: app.metria.PhaseEnergySuggestion? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                Text("Daily references", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (onEdit != null) IconButton(onClick = onEdit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Rounded.Edit, "Edit daily references") }
            }
            Text(targets?.caloriesKcal?.let { "${goalDisplayNumber(it)} kcal" } ?: "No calorie target", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (estimated) Icons.Rounded.Info else Icons.Rounded.PersonOutline, null)
                when {
                    phaseEnergySuggestion != null -> PhaseEnergySourceLabel(phaseEnergySuggestion)
                    estimated -> Text("Confirmed maintenance estimate", style = MaterialTheme.typography.bodySmall)
                    else -> Text("Member-entered references", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            listOf("Protein" to targets?.proteinGrams, "Carbohydrates" to targets?.carbohydrateGrams, "Fat" to targets?.fatGrams).forEachIndexed { index, (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(value?.let { "${goalDisplayNumber(it)} g" } ?: "No target", style = MaterialTheme.typography.titleSmall)
                }
                if (value != null) MacroFieldSource(label, macroSuggestion, listOf(GoalMacroField.PROTEIN, GoalMacroField.CARBOHYDRATE, GoalMacroField.FAT)[index])
            }
        }
    }
}
