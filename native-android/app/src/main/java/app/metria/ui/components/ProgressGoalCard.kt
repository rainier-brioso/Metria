package app.metria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.GoalPhase
import app.metria.NutritionTargets
import app.metria.WeightUnits

/** Current goal context, independent of the chart's selected date. */
@Composable
fun ProgressGoalCard(
    phase: GoalPhase?, targets: NutritionTargets?, displayUnit: String,
    onOpenGoal: () -> Unit,
) {
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(value: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(value) else value)} $unit"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(phase?.let { goalIntentIcon(it.intent) } ?: Icons.Rounded.TrackChanges,
                    contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your current goal", style = MaterialTheme.typography.titleMedium)
                    Text(phase?.intent?.title ?: "No weight goal chosen", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (phase != null) {
                phase.targetWeightKg?.let { Text("Your target · ${weight(it)}", style = MaterialTheme.typography.bodyMedium) }
                phase.rangeMinimumKg?.let { low ->
                    phase.rangeMaximumKg?.let { high ->
                        Text("Your chosen range · ${weight(low)} to ${weight(high)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (phase.targetWeightKg == null && phase.rangeMinimumKg == null) {
                    Text("Tracking without a weight target or range.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text("Keep tracking, or choose a direction when you're ready.", style = MaterialTheme.typography.bodyMedium)
            }
            if (targets != null) {
                Text("Daily nutrition references set", style = MaterialTheme.typography.labelLarge)
            }
            Text("Current references may differ from those on a selected past day.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (phase == null && targets == null) "Explore goal setup" else "View goal and daily references")
            }
        }
    }
}
