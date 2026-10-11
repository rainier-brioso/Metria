package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.metria.GoalPhase
import app.metria.WeightEntry
import app.metria.WeightUnits
import app.metria.ui.theme.MetriaColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Compact Today summary. The full goal card remains available on the goal detail screen. */
@Composable
fun TodayGoalCard(
    phase: GoalPhase?, latestWeight: WeightEntry?, unit: String,
    onOpenGoal: () -> Unit, onUpdateWeight: () -> Unit,
) {
    val shownKg = latestWeight?.kilograms ?: phase?.referenceWeightKg
    val unitLabel = if (unit == "lb") "lb" else "kg"
    fun weight(kg: Double): String {
        val display = if (unitLabel == "lb") WeightUnits.kilogramsToPounds(kg) else kg
        return "${goalDisplayNumber(display)} $unitLabel"
    }
    val weightLabel = when {
        latestWeight != null -> "Latest recorded weight"
        phase?.referenceWeightKg != null -> "Starting reference"
        else -> "Latest recorded weight"
    }
    val markerWeightLabel = shownKg?.let {
        "${if (latestWeight != null) "Current" else "Starting reference"} ${weight(it)}"
    }.orEmpty()
    val intent = phase?.intent
    val showStartTargetInterval = phase != null && shownKg != null &&
        phase.referenceWeightKg != null && phase.targetWeightKg != null &&
        phase.targetWeightKg != phase.referenceWeightKg && phase.rangeMinimumKg == null

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onOpenGoal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(if (intent != null) goalIntentIcon(intent) else Icons.Rounded.Scale, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(intent?.title ?: "Weight", style = MaterialTheme.typography.titleMedium)
                    if (phase == null) Text("No goal chosen", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
                Text(if (phase == null) "Set goal" else "Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(weightLabel, style = MaterialTheme.typography.labelMedium, color = MetriaColors.InkMuted)
                Text(shownKg?.let(::weight) ?: "No weight entry yet", style = MaterialTheme.typography.headlineSmall)
                latestWeight?.let {
                    Text(it.recordedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                        style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
            }

            if (phase?.rangeMinimumKg != null && phase.rangeMaximumKg != null) {
                val low = phase.rangeMinimumKg
                val high = phase.rangeMaximumKg
                if (high > low) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your chosen range", style = MaterialTheme.typography.labelMedium)
                        if (shownKg != null && (shownKg < low || shownKg > high)) {
                            Text(if (shownKg < low) "Below range" else "Above range", style = MaterialTheme.typography.labelSmall, color = MetriaColors.InkMuted)
                        }
                    }
                    if (shownKg != null) {
                        GoalWeightRangeMarker(shownKg, low, high, markerWeightLabel, weight(low), weight(high))
                    } else {
                        Text("${weight(low)} to ${weight(high)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    }
                } else {
                    Text("Chosen weight · ${weight(low)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    if (shownKg != null && shownKg != low) {
                        Text(if (shownKg < low) "Below chosen range" else "Above chosen range", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    }
                }
            } else if (phase != null && shownKg != null && phase.targetWeightKg != null && phase.referenceWeightKg != null && phase.targetWeightKg != phase.referenceWeightKg) {
                Text("Starting reference to target", style = MaterialTheme.typography.labelMedium)
                GoalWeightRangeMarker(shownKg, phase.referenceWeightKg, phase.targetWeightKg,
                    markerWeightLabel, "Start ${weight(phase.referenceWeightKg)}", "Target ${weight(phase.targetWeightKg)}")
                val intervalLow = minOf(phase.referenceWeightKg, phase.targetWeightKg)
                val intervalHigh = maxOf(phase.referenceWeightKg, phase.targetWeightKg)
                if (shownKg < intervalLow || shownKg > intervalHigh) {
                    Text(if (shownKg < intervalLow) "Below start-to-target interval" else "Above start-to-target interval",
                        style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
            }
            if (phase?.targetWeightKg != null && !showStartTargetInterval) {
                Text("Your target · ${weight(phase.targetWeightKg)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            }

            if (latestWeight != null && phase?.referenceWeightKg != null && !showStartTargetInterval) {
                Text("Starting reference · ${weight(phase.referenceWeightKg)}", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            }

            TextButton(onClick = onUpdateWeight, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(if (latestWeight == null) "Add weight" else "Update weight")
            }
        }
    }
}
