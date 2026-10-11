package app.metria.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.GoalIntent
import app.metria.GoalTraining
import app.metria.MaintenanceEstimate
import app.metria.PhaseEnergySuggestion

@Composable
internal fun PhaseEnergyTrainingChoice(
    training: GoalTraining?,
    priorTraining: GoalTraining? = null,
    enabled: Boolean,
    onTrainingChange: (GoalTraining) -> Unit,
) {
    priorTraining?.let {
        Text("Previous phase context · ${goalTrainingTitle(it)}. Review and choose what applies now.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text("Training context for the phase adjustment", style = MaterialTheme.typography.labelLarge)
    listOf(
        GoalTraining.GENERAL to "General wellness / no regular training",
        GoalTraining.STRENGTH to "Regular strength training",
        GoalTraining.EXERCISE to "Other regular exercise",
        GoalTraining.DEMANDING to "Demanding endurance or mixed training",
        GoalTraining.MANUAL to "Prefer manual references",
    ).forEach { (option, title) ->
        FilterChip(
            selected = training == option,
            onClick = { onTrainingChange(option) },
            enabled = enabled,
            label = { Text(title) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

@Composable
internal fun PhaseEnergySuitabilityButton(suitable: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    OutlinedButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            selected = suitable
            stateDescription = if (suitable) "Confirmed" else "Not confirmed"
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (suitable) "Confirmed: this general option is suitable" else "Confirm this general option is suitable")
            if (suitable) Icon(Icons.Rounded.CheckCircle, contentDescription = "Confirmed")
        }
    }
}

@Composable
internal fun PhaseEnergyMaintenanceChoice(
    estimate: MaintenanceEstimate?,
    selectedKcal: Double?,
    enabled: Boolean,
    onChoose: (Double) -> Unit,
) {
    Text("Choose a maintenance reference", style = MaterialTheme.typography.labelLarge)
    if (estimate == null) {
        Text("Review a maintenance estimate first. Your current daily references stay unchanged.", style = MaterialTheme.typography.bodySmall)
        return
    }
    MaintenanceRangeSummary(estimate)
    listOf("Lower end" to estimate.minimumKcal, "Upper end" to estimate.maximumKcal).forEach { (label, kcal) ->
        FilterChip(
            selected = selectedKcal == kcal,
            onClick = { onChoose(kcal) },
            enabled = enabled,
            label = { Text("$label · ${goalDisplayNumber(kcal)} kcal/day") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

@Composable
internal fun PhaseEnergyProposalPreview(
    suggestion: PhaseEnergySuggestion,
    methodExpanded: Boolean,
    onToggleMethod: () -> Unit,
) {
    val adjustment = suggestion.caloriesKcal - suggestion.maintenanceKcal
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Phase calorie proposal", style = MaterialTheme.typography.titleMedium)
            Text("A starting estimate. Review it as your needs change.", style = MaterialTheme.typography.bodySmall)
            EnergyProposalRow("Reviewed maintenance reference", "${goalDisplayNumber(suggestion.maintenanceKcal)} kcal/day")
            EnergyProposalRow("Phase adjustment", "${if (adjustment < 0) "−" else "+"}${goalDisplayNumber(kotlin.math.abs(adjustment))} kcal/day")
            Text("${goalDisplayNumber(suggestion.caloriesKcal)} kcal/day", style = MaterialTheme.typography.headlineMedium)
            Text("Proposed daily calorie reference", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onToggleMethod, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (methodExpanded) "Hide method and inputs" else "How calculated")
            }
            if (methodExpanded) PhaseEnergyMethodDetails(suggestion)
        }
    }
}

@Composable
internal fun PhaseEnergyMethodDetails(suggestion: PhaseEnergySuggestion) {
    val adjustment = suggestion.caloriesKcal - suggestion.maintenanceKcal
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("How this starting estimate was calculated", style = MaterialTheme.typography.titleSmall)
        Text(
            when (suggestion.intent) {
                GoalIntent.LOSE -> "10% below the maintenance reference you selected. This is a product starting option, not a personal prescription or a prediction."
                GoalIntent.GAIN -> "5% above the maintenance reference you selected for regular strength training. This is a product starting option, not a personal prescription or a prediction."
                else -> "No automatic phase adjustment is available for this direction."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        val profile = suggestion.estimate.profile
        Text("Age at review · ${profile.age} years", style = MaterialTheme.typography.bodySmall)
        Text("Reviewed weight · ${goalDisplayNumber(profile.weightKg)} kg; height · ${goalDisplayNumber(profile.heightCm)} cm", style = MaterialTheme.typography.bodySmall)
        Text("Equation · ${profile.equation.title}; whole-day activity · ${profile.activity.title}", style = MaterialTheme.typography.bodySmall)
        Text("Maintenance range · ${goalDisplayNumber(suggestion.estimate.minimumKcal)}–${goalDisplayNumber(suggestion.estimate.maximumKcal)} kcal/day", style = MaterialTheme.typography.bodySmall)
        Text("Selected maintenance · ${goalDisplayNumber(suggestion.maintenanceKcal)} kcal/day", style = MaterialTheme.typography.bodySmall)
        Text("Adjustment · ${if (adjustment < 0) "−" else "+"}${goalDisplayNumber(kotlin.math.abs(adjustment))} kcal/day", style = MaterialTheme.typography.bodySmall)
        Text("Phase calories · ${goalDisplayNumber(suggestion.caloriesKcal)} kcal/day", style = MaterialTheme.typography.bodySmall)
        suggestion.training?.let { Text("Training context · ${goalTrainingTitle(it)}", style = MaterialTheme.typography.bodySmall) }
        Text("Rounded to the nearest whole kcal; half kcal rounds up.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun PhaseEnergySourceLabel(suggestion: PhaseEnergySuggestion?) {
    Text(
        if (suggestion == null) "Calories · member-entered" else "Calories · phase-adjusted starting estimate",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EnergyProposalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
internal fun MaintenanceRangeSummary(estimate: MaintenanceEstimate) {
    Text(
        "Reviewed maintenance range · ${goalDisplayNumber(estimate.minimumKcal)}–${goalDisplayNumber(estimate.maximumKcal)} kcal/day",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
