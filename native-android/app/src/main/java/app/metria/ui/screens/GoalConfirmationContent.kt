package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.*
import app.metria.ui.components.GoalReferenceStatusLabel
import app.metria.ui.components.MacroFieldSource
import app.metria.ui.components.PhaseEnergySourceLabel
import app.metria.ui.components.goalDisplayNumber

/** Presentation only; the parent owns the reviewed draft and save operation. */
@Composable
internal fun GoalConfirmationContent(
    previous: GoalPhase?, currentTargets: NutritionTargets?, proposed: GoalPhase,
    newPhase: Boolean, historyCount: Int,
) {
    if (previous != null) {
        GoalConfirmationCard("Current phase", previous, previous.targets)
    } else {
        GoalConfirmationCard("Current references", null, currentTargets)
    }
    GoalConfirmationCard("Proposed phase", proposed, proposed.targets)
    Text(
        GoalReviewPresentation.finalReviewGuidance(proposed),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        when {
            newPhase && previous != null -> "Your current phase stays active until this saves. It will then move to Previous phases."
            previous != null -> "This updates your current phase and keeps its start date and history."
            else -> "This sets up your first phase. Your meals and weight records stay as they are."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (newPhase && previous != null) {
        Text(
            if (historyCount >= GoalPolicy.historyLimit)
                "Your history is full. Starting this phase will remove the oldest previous phase; the most recent ${GoalPolicy.historyLimit} will remain."
            else "Up to ${GoalPolicy.historyLimit} previous phases are kept.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun GoalConfirmationCard(title: String, phase: GoalPhase?, targets: NutritionTargets?) {
    val referenceReview = GoalReviewPresentation.dailyReferences(targets)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(phase?.intent?.title ?: "No goal phase", style = MaterialTheme.typography.titleLarge)
            GoalReferenceStatusLabel(referenceReview)
            phase?.let {
                Text("Starting reference: ${weight(it.referenceWeightKg)}")
                Text("Optional target: ${weight(it.targetWeightKg)}")
                Text("Optional range: ${it.rangeMinimumKg?.let { low -> "${number(low)}-${number(requireNotNull(it.rangeMaximumKg))} kg" } ?: "Not set"}")
                GoalReviewPresentation.phaseCaloriesSummary(
                    intent = it.intent,
                    caloriesKcal = it.targets?.caloriesKcal,
                    maintenanceSelected = it.acceptedEstimate != null,
                    proposal = it.phaseEnergySuggestion,
                )?.let { status -> Text(status, style = MaterialTheme.typography.bodySmall) }
            }
            HorizontalDivider()
            Text("Calories: ${targets?.caloriesKcal?.let { "${number(it)} kcal/day" } ?: "Not set"}")
            if (targets?.caloriesKcal == null) {
                Text("Calorie source: Not set", style = MaterialTheme.typography.bodySmall)
            } else when {
                phase?.phaseEnergySuggestion != null -> PhaseEnergySourceLabel(phase.phaseEnergySuggestion)
                phase?.acceptedEstimate != null -> Text("Calorie source: Selected maintenance estimate", style = MaterialTheme.typography.bodySmall)
                else -> Text("Calorie source: Member-entered", style = MaterialTheme.typography.bodySmall)
            }
            Text("Protein: ${grams(targets?.proteinGrams)}")
            targets?.proteinGrams?.let { MacroFieldSource("Protein", phase?.macroSuggestion, GoalMacroField.PROTEIN) }
            Text("Carbohydrates: ${grams(targets?.carbohydrateGrams)}")
            targets?.carbohydrateGrams?.let { MacroFieldSource("Carbohydrates", phase?.macroSuggestion, GoalMacroField.CARBOHYDRATE) }
            Text("Fat: ${grams(targets?.fatGrams)}")
            targets?.fatGrams?.let { MacroFieldSource("Fat", phase?.macroSuggestion, GoalMacroField.FAT) }
        }
    }
}

private fun number(value: Double) = goalDisplayNumber(value)
private fun weight(value: Double?) = value?.let { "${number(it)} kg" } ?: "Not set"
private fun grams(value: Double?) = value?.let { "${number(it)} g/day" } ?: "Not set"
