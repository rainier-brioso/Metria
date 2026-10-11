package app.metria.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.GoalMacroField
import app.metria.GoalTraining
import app.metria.MacroSuggestion
import app.metria.NutritionTargets
import app.metria.PregnancyBreastfeedingApplicability

internal fun goalTrainingTitle(training: GoalTraining): String = when (training) {
    GoalTraining.GENERAL -> "General wellness / no regular training"
    GoalTraining.STRENGTH -> "Regular strength training"
    GoalTraining.EXERCISE -> "Other regular exercise"
    GoalTraining.DEMANDING -> "Demanding endurance or mixed training"
    GoalTraining.MANUAL -> "Prefer manual references"
}

@Composable
internal fun MacroFieldSource(label: String, suggestion: MacroSuggestion?, field: GoalMacroField) {
    val derived = when (field) {
        GoalMacroField.PROTEIN -> suggestion?.proteinDerived
        GoalMacroField.CARBOHYDRATE -> suggestion?.carbohydrateDerived
        GoalMacroField.FAT -> suggestion?.fatDerived
    } == true
    Text(
        when {
            suggestion == null -> "$label · member-entered"
            derived -> "$label · suggested (${goalTrainingShortTitle(suggestion.training)})"
            else -> "$label · member-entered, custom split"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun MacroReferenceSources(targets: NutritionTargets?, suggestion: MacroSuggestion?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (targets?.proteinGrams != null) MacroFieldSource("Protein", suggestion, GoalMacroField.PROTEIN)
        if (targets?.carbohydrateGrams != null) MacroFieldSource("Carbohydrates", suggestion, GoalMacroField.CARBOHYDRATE)
        if (targets?.fatGrams != null) MacroFieldSource("Fat", suggestion, GoalMacroField.FAT)
        if (targets == null || listOf(targets.proteinGrams, targets.carbohydrateGrams, targets.fatGrams).all { it == null }) {
            Text("No macro references set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MacroDraftSourceSummary(values: List<String>, suggestion: MacroSuggestion?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (values.getOrNull(0).orEmpty().isNotBlank()) MacroFieldSource("Protein", suggestion, GoalMacroField.PROTEIN)
        if (values.getOrNull(1).orEmpty().isNotBlank()) MacroFieldSource("Carbohydrates", suggestion, GoalMacroField.CARBOHYDRATE)
        if (values.getOrNull(2).orEmpty().isNotBlank()) MacroFieldSource("Fat", suggestion, GoalMacroField.FAT)
        if (values.getOrNull(0).orEmpty().isBlank() && values.getOrNull(1).orEmpty().isBlank() && values.getOrNull(2).orEmpty().isBlank()) {
            Text("No macro references set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MacroSuggestionPreview(
    proposal: MacroSuggestion,
    existing: List<String>,
    existingSource: MacroSuggestion?,
    methodExpanded: Boolean,
    onToggleMethod: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Suggested starting references", style = MaterialTheme.typography.titleMedium)
            Text("Review these values before using them. They are optional starting references, not measured requirements.", style = MaterialTheme.typography.bodySmall)
            Text("Current values", style = MaterialTheme.typography.labelLarge)
            MacroCurrentValue("Protein", existing.getOrNull(0).orEmpty(), existingSource, GoalMacroField.PROTEIN)
            MacroCurrentValue("Carbohydrates", existing.getOrNull(1).orEmpty(), existingSource, GoalMacroField.CARBOHYDRATE)
            MacroCurrentValue("Fat", existing.getOrNull(2).orEmpty(), existingSource, GoalMacroField.FAT)
            Text("Proposed values", style = MaterialTheme.typography.labelLarge)
            MacroProposedValue("Protein", proposal.proteinGrams, GoalMacroField.PROTEIN, proposal)
            MacroProposedValue("Carbohydrates", proposal.carbohydrateGrams, GoalMacroField.CARBOHYDRATE, proposal)
            MacroProposedValue("Fat", proposal.fatGrams, GoalMacroField.FAT, proposal)
            androidx.compose.material3.TextButton(onClick = onToggleMethod, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (methodExpanded) "Hide method and inputs" else "How calculated")
            }
            if (methodExpanded) MacroMethodDetails(proposal)
        }
    }
}

@Composable
internal fun MacroInputEditor(
    age: String,
    weight: String,
    weightUnit: String,
    training: GoalTraining?,
    prefillSource: String,
    calorieBudget: String,
    priorTraining: GoalTraining?,
    suitable: Boolean,
    pregnancyBreastfeeding: PregnancyBreastfeedingApplicability,
    enabled: Boolean,
    onAgeChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWeightUnitChange: (String) -> Unit,
    onTrainingChange: (GoalTraining) -> Unit,
    onSuitableChange: () -> Unit,
    onPregnancyBreastfeedingChange: (PregnancyBreastfeedingApplicability) -> Unit,
    onCareTeamReferences: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Optional macro suggestions", style = MaterialTheme.typography.titleMedium)
        Text("These references depend on a reviewed calorie budget, age, weight and training context. The budget is not a claim of adequate energy. Training does not change the calorie method or add exercise calories.", style = MaterialTheme.typography.bodySmall)
        Text("Reviewed daily calorie budget · ${calorieBudget.ifBlank { "Not set" }}${if (calorieBudget.isNotBlank()) " kcal/day" else ""}", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(age, onAgeChange, label = { Text("Age at review (whole years)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(weight, onWeightChange, label = { Text("Reviewed weight ($weightUnit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("kg", "lb").forEach { unit ->
                FilterChip(selected = weightUnit == unit, onClick = { onWeightUnitChange(unit) }, enabled = enabled, label = { Text(unit) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text(prefillSource, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (priorTraining != null) {
            Text("Previous phase context · ${goalTrainingTitle(priorTraining)}. Choose what applies now.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    Text("Training context for macro references only", style = MaterialTheme.typography.labelLarge)
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
        var strengthDetails by remember { mutableStateOf(false) }
        if (training == GoalTraining.STRENGTH) {
            TextButton(onClick = { strengthDetails = !strengthDetails }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (strengthDetails) "Hide strength training details" else "What counts as strength training?")
            }
            if (strengthDetails) Text("Strength training means resistance exercise, such as weights or bodyweight exercises.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Frequency and diet type are not needed. Whole-day activity remains the calorie basis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.OutlinedButton(
            onClick = onSuitableChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                selected = suitable
                stateDescription = if (suitable) "Confirmed" else "Not confirmed"
            },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (suitable) "Confirmed: this general option is suitable" else "Confirm this general option is suitable")
                if (suitable) Icon(androidx.compose.material.icons.Icons.Rounded.CheckCircle, contentDescription = "Confirmed")
            }
        }
        Text("Use this general option only if it is suitable for you and you do not need individual clinical or eating-disorder nutrition support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PregnancyBreastfeedingChoice(pregnancyBreastfeeding, enabled, onPregnancyBreastfeedingChange)
        CareTeamReferencesButton(enabled, onCareTeamReferences)
        if (training == GoalTraining.DEMANDING || training == GoalTraining.MANUAL) {
            Text("Automatic suggestions are not available for this choice. Your existing references remain available for manual editing.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun PregnancyBreastfeedingChoice(
    answer: PregnancyBreastfeedingApplicability,
    enabled: Boolean,
    onAnswer: (PregnancyBreastfeedingApplicability) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Does pregnancy or breastfeeding apply to you right now?", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = answer == PregnancyBreastfeedingApplicability.DOES_NOT_APPLY,
                onClick = { onAnswer(PregnancyBreastfeedingApplicability.DOES_NOT_APPLY) },
                enabled = enabled,
                label = { Text("No") },
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    stateDescription = if (answer == PregnancyBreastfeedingApplicability.DOES_NOT_APPLY) "Selected" else "Not selected"
                },
            )
            FilterChip(
                selected = answer == PregnancyBreastfeedingApplicability.APPLIES,
                onClick = { onAnswer(PregnancyBreastfeedingApplicability.APPLIES) },
                enabled = enabled,
                label = { Text("Yes") },
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    stateDescription = if (answer == PregnancyBreastfeedingApplicability.APPLIES) "Selected; use care-team references" else "Not selected"
                },
            )
        }
        if (answer == PregnancyBreastfeedingApplicability.UNANSWERED) {
            Text("Choose an answer before using an automatic starting estimate. This answer is temporary and is not saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CareTeamReferencesButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text("Enter targets from your care team") }
}

@Composable
private fun MacroCurrentValue(label: String, value: String, source: MacroSuggestion?, field: GoalMacroField) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label · ${value.ifBlank { "Not set" }}${if (value.isNotBlank()) " g/day" else ""}", style = MaterialTheme.typography.bodyMedium)
        if (value.isNotBlank()) MacroFieldSource(label, source, field)
    }
}

@Composable
private fun MacroProposedValue(label: String, value: Double, field: GoalMacroField, source: MacroSuggestion) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label · ${goalDisplayNumber(value)} g/day", style = MaterialTheme.typography.bodyMedium)
        MacroFieldSource(label, source, field)
    }
}

@Composable
internal fun MacroMethodDetails(suggestion: MacroSuggestion) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("How these references were calculated", style = MaterialTheme.typography.titleSmall)
        Text(
            when (suggestion.training) {
                GoalTraining.GENERAL -> "Balanced starting option: 20% of the reviewed calorie budget from protein, 30% from fat and 50% from carbohydrates."
                GoalTraining.STRENGTH -> "Strength-training option: protein based on 1.6 g/kg, fat at 30% of the reviewed calorie budget and remaining energy from carbohydrates."
                GoalTraining.EXERCISE -> "Exercise option: protein based on 1.4 g/kg, fat at 30% of the reviewed calorie budget and remaining energy from carbohydrates."
                GoalTraining.DEMANDING, GoalTraining.MANUAL -> "No automatic macro calculation is available for this choice."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Age at review · ${suggestion.age} years", style = MaterialTheme.typography.bodySmall)
        Text("Reviewed weight · ${goalDisplayNumber(suggestion.weightKg)} kg", style = MaterialTheme.typography.bodySmall)
        Text("Daily calorie budget · ${goalDisplayNumber(suggestion.caloriesKcal)} kcal", style = MaterialTheme.typography.bodySmall)
        Text("Training context · ${goalTrainingTitle(suggestion.training)}", style = MaterialTheme.typography.bodySmall)
        Text("Unrounded calculation · protein ${goalDisplayNumber(suggestion.rawProteinGrams)} g, carbohydrates ${goalDisplayNumber(suggestion.rawCarbohydrateGrams)} g, fat ${goalDisplayNumber(suggestion.rawFatGrams)} g", style = MaterialTheme.typography.bodySmall)
        Text("Values are rounded to the nearest whole gram; half grams round up. Macro energy is approximate and may differ slightly after rounding.", style = MaterialTheme.typography.bodySmall)
        Text("Method version · ${suggestion.method}; ${suggestion.roundingVersion}.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun goalTrainingShortTitle(training: GoalTraining): String = when (training) {
    GoalTraining.GENERAL -> "general wellness"
    GoalTraining.STRENGTH -> "strength training"
    GoalTraining.EXERCISE -> "other exercise"
    GoalTraining.DEMANDING -> "demanding training"
    GoalTraining.MANUAL -> "manual references"
}
