package app.metria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.GoalPhase
import app.metria.GoalMacroField
import app.metria.WeightUnits

@Composable
internal fun SavedDailyReferenceCard(phase: GoalPhase) {
    val targets = phase.targets
    val calories = targets?.caloriesKcal
    val estimate = phase.acceptedEstimate
    val phaseEnergy = phase.phaseEnergySuggestion
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
            Text("Daily calorie reference", style = MaterialTheme.typography.titleMedium)
            Text(calories?.let { "${goalDisplayNumber(it)} kcal" } ?: "Unset", style = MaterialTheme.typography.displaySmall)
            when {
                phaseEnergy != null -> PhaseEnergySourceLabel(phaseEnergy)
                estimate != null -> Text("Selected maintenance estimate")
                calories != null -> Text("Member-entered reference")
                else -> Text("No daily calorie reference")
            }
            if (estimate != null && calories != null) {
                val lower = "${goalDisplayNumber(estimate.minimumKcal)} kcal"
                val upper = "${goalDisplayNumber(estimate.maximumKcal)} kcal"
                val selected = "Saved selection: ${goalDisplayNumber(calories)} kcal/day"
                if (estimate.minimumKcal < estimate.maximumKcal) {
                    GoalReferenceRangeMarker(calories, estimate.minimumKcal, estimate.maximumKcal,
                        selected, lower, upper,
                        "Approximate maintenance activity range: $lower to $upper. $selected.")
                } else Text("$selected. Approximate maintenance reference: $lower.")
                Text("This is an approximate range for your reviewed whole-day activity, not a measured requirement.", style = MaterialTheme.typography.bodySmall)
            }
            if (phaseEnergy != null) Text("Selected maintenance reference · ${goalDisplayNumber(phaseEnergy.maintenanceKcal)} kcal/day", style = MaterialTheme.typography.bodySmall)
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily macro references", style = MaterialTheme.typography.titleMedium)
            SavedReferenceRow("Protein", targets?.proteinGrams)
            if (targets?.proteinGrams != null) MacroFieldSource("Protein", phase.macroSuggestion, GoalMacroField.PROTEIN)
            SavedReferenceRow("Carbohydrates", targets?.carbohydrateGrams)
            if (targets?.carbohydrateGrams != null) MacroFieldSource("Carbohydrates", phase.macroSuggestion, GoalMacroField.CARBOHYDRATE)
            SavedReferenceRow("Fat", targets?.fatGrams)
            if (targets?.fatGrams != null) MacroFieldSource("Fat", phase.macroSuggestion, GoalMacroField.FAT)
            if (targets?.proteinGrams == null && targets?.carbohydrateGrams == null && targets?.fatGrams == null) {
                Text("No macro references are set.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SavedReferenceRow(label: String, grams: Double?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(grams?.let { "${goalDisplayNumber(it)} g/day" } ?: "Unset", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun SavedWeightContextCard(phase: GoalPhase, displayUnit: String) {
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(kg: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(kg) else kg)} $unit"
    val starting = phase.referenceWeightKg
    val target = phase.targetWeightKg
    val low = phase.rangeMinimumKg
    val high = phase.rangeMaximumKg
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(goalIntentIcon(phase.intent), null, tint = MaterialTheme.colorScheme.primary)
            Text(phase.intent.title, style = MaterialTheme.typography.titleLarge)
            Text("Saved starting reference", style = MaterialTheme.typography.labelLarge)
            Text(starting?.let(::weight) ?: "Unset", style = MaterialTheme.typography.displaySmall)
            Text("Chosen target: ${target?.let(::weight) ?: "Unset"}")
            Text("Chosen range: ${if (low != null && high != null) "${weight(low)} to ${weight(high)}" else "Unset"}")
            when {
                starting != null && low != null && high != null && low < high -> {
                    val outside = starting !in low..high
                    val label = "Starting reference: ${weight(starting)}${if (outside) "; outside your chosen range" else ""}"
                    GoalReferenceRangeMarker(starting, low, high, label, weight(low), weight(high),
                        "$label. Chosen range ${weight(low)} to ${weight(high)}.")
                }
                starting != null && target != null && starting != target && low == null -> {
                    val label = "Starting reference: ${weight(starting)}"
                    GoalReferenceRangeMarker(starting, starting, target, label, "Start ${weight(starting)}", "Target ${weight(target)}",
                        "$label. Chosen target ${weight(target)}. These are saved reference points, not a prediction.")
                }
            }
            if (starting != null && low != null && high != null && low == high && starting != low) {
                Text("Your starting reference is outside your chosen range.", style = MaterialTheme.typography.bodySmall)
            }
            Text("These are your saved choices. New weight entries do not change your daily references.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
