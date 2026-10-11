package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import app.metria.GoalPhase
import app.metria.WeightEntry
import app.metria.MealEntry
import app.metria.PhaseRecapCalculator
import app.metria.WeightUnits
import app.metria.ui.components.GoalMethodDisclosure
import app.metria.ui.components.MacroMethodDetails
import app.metria.ui.components.PhaseEnergyMethodDetails
import app.metria.ui.components.goalDisplayNumber
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalPhaseDetailScreen(
    phase: GoalPhase,
    isCurrent: Boolean,
    displayUnit: String,
    deleting: Boolean,
    weightEntries: List<WeightEntry>,
    meals: List<MealEntry>,
    onBackToHub: () -> Unit,
    onDone: () -> Unit,
    onEditCurrent: () -> Unit,
    onDeletePrevious: () -> Unit,
) {
    var methodExpanded by rememberSaveable(phase.id) { mutableStateOf(false) }
    var manageExpanded by rememberSaveable(phase.id) { mutableStateOf(false) }
    val unit = if (displayUnit == "lb") "lb" else "kg"
    fun weight(value: Double) = "${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(value) else value)} $unit"
    val zone = ZoneId.systemDefault()
    val asOf = remember(phase.id, phase.updatedAt, weightEntries, meals) { Instant.now() }
    val recap = remember(phase, isCurrent, weightEntries, meals, zone, asOf) {
        PhaseRecapCalculator.calculate(phase, isCurrent, weightEntries, meals, zone, asOf)
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBackToHub, enabled = !deleting, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Back to Goals" }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(phase.intent.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (isCurrent) "Since ${formatGoalDate(phase.confirmedAt, zone)} · In progress"
                        else if (phase.endedAt == null) "Started ${formatGoalDate(phase.confirmedAt, zone)}"
                        else "${formatGoalDate(phase.confirmedAt, zone)} to ${formatGoalDate(phase.endedAt, zone)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Starting: ${phase.referenceWeightKg?.let(::weight) ?: "Not set"} · Target: ${phase.targetWeightKg?.let(::weight) ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
                    if (phase.rangeMinimumKg != null && phase.rangeMaximumKg != null) Text("Chosen range: ${weight(phase.rangeMinimumKg)} to ${weight(phase.rangeMaximumKg)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PhaseDailyValuesCard(phase)

            if (phase.acceptedEstimate != null || phase.macroSuggestion != null || phase.phaseEnergySuggestion != null) {
                GoalMethodDisclosure(methodExpanded, { methodExpanded = !methodExpanded }) {
                    val caloriesSet = phase.targets?.caloriesKcal != null
                    Text("Calories: ${when { !caloriesSet -> "Not set"; phase.phaseEnergySuggestion != null || phase.acceptedEstimate != null -> "Estimate"; else -> "Member entered" }}", style = MaterialTheme.typography.bodySmall)
                    listOf("Protein" to phase.macroSuggestion?.proteinDerived, "Carbohydrate" to phase.macroSuggestion?.carbohydrateDerived, "Fat" to phase.macroSuggestion?.fatDerived)
                        .filter { (name, _) -> when (name) { "Protein" -> phase.targets?.proteinGrams != null; "Carbohydrate" -> phase.targets?.carbohydrateGrams != null; else -> phase.targets?.fatGrams != null } }
                        .forEach { (name, derived) -> Text("$name: ${if (derived == true) "Estimate" else "Member entered"}", style = MaterialTheme.typography.bodySmall) }
                    phase.acceptedEstimate?.let { MaintenanceDetails(it) }
                    phase.macroSuggestion?.let { MacroMethodDetails(it) }
                    phase.phaseEnergySuggestion?.let { PhaseEnergyMethodDetails(it) }
                }
            }

            PhaseRecordedRecapCard(recap = recap, unit = unit, isCurrent = isCurrent, phaseId = phase.id)

            if (!isCurrent) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { manageExpanded = !manageExpanded },
                        enabled = !deleting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(if (manageExpanded) "Hide phase management" else "Manage this phase") }
                    if (manageExpanded) {
                        Text("Removing a phase also removes its saved calculation inputs. It does not change your current goal, daily references, meals or weights.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(
                            onClick = onDeletePrevious,
                            enabled = !deleting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete this previous phase")
                        }
                    }
                }
            }
        }
        Button(
            onClick = if (isCurrent) onEditCurrent else onBackToHub,
            enabled = !deleting,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
        ) { Text(if (isCurrent) "Edit weight goal" else "Back to Goals") }
    }
}

@Composable
private fun PhaseDailyValuesCard(phase: GoalPhase) {
    val values = listOf("Protein" to phase.targets?.proteinGrams, "Carbohydrate" to phase.targets?.carbohydrateGrams, "Fat" to phase.targets?.fatGrams)
    val fontScale = LocalDensity.current.fontScale
    val setCount = values.count { it.second != null }
    val derivedCount = listOf(
        phase.macroSuggestion?.proteinDerived == true && phase.targets?.proteinGrams != null,
        phase.macroSuggestion?.carbohydrateDerived == true && phase.targets?.carbohydrateGrams != null,
        phase.macroSuggestion?.fatDerived == true && phase.targets?.fatGrams != null,
    ).count { it }
    val macroSource = when { setCount == 0 -> "Not set"; derivedCount == 0 -> "Member entered"; derivedCount == setCount -> "Estimate"; else -> "Mixed sources" }
    val calories = phase.targets?.caloriesKcal
    val calorieSource = when {
        calories == null -> "Not set"
        phase.phaseEnergySuggestion != null || phase.acceptedEstimate != null -> "Estimate"
        else -> "Member entered"
    }
    val sourceSummary = when {
        calories == null -> "Macros $macroSource"
        macroSource == "Not set" -> "Calories $calorieSource · macros not set"
        calorieSource == macroSource -> if (calorieSource == "Estimate") "Estimated references" else "Member entered references"
        else -> "Mixed sources · includes estimates"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Calories / day", style = MaterialTheme.typography.labelLarge)
                    Text(calories?.let { "${goalDisplayNumber(it)} kcal" } ?: "Not set", style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (fontScale > 1.25f) values.forEach { (label, value) -> Text("$label · ${value?.let { "${goalDisplayNumber(it)} g" } ?: "Not set"}", style = MaterialTheme.typography.bodySmall) }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                values.forEach { (label, value) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value?.let { "${goalDisplayNumber(it)} g" } ?: "Not set", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (calories != null || setCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(if (calorieSource == "Estimate" || macroSource == "Estimate" || macroSource == "Mixed sources") Icons.Rounded.Info else Icons.Rounded.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sources · $sourceSummary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PhaseRecordedRecapCard(recap: app.metria.PhaseRecap, unit: String, isCurrent: Boolean, phaseId: String) {
    var detailsExpanded by rememberSaveable(phaseId) { mutableStateOf(false) }
    val points = recap.weights
    val totals = recap.nutrition
    val noNutrition = totals == null || (totals.mealsWithoutNutrition == recap.mealsLogged &&
        listOf(totals.calories, totals.protein, totals.carbohydrate, totals.fat).all { it.total == null && it.partialMealsExcluded == 0 })
    val anyRecord = points.isNotEmpty() || recap.mealsLogged > 0
    val boundaryCount = recap.boundaryDateOnlyMealsExcluded + recap.boundaryWeightTimesUnavailable
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recorded recap", style = MaterialTheme.typography.titleMedium)
            if (!recap.periodAvailable) {
                Text("Period unavailable", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            if (!anyRecord && boundaryCount == 0) {
                Text(if (isCurrent) "No records in this phase yet" else "No records in this phase", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            if (!anyRecord) Text(if (isCurrent) "No records in this phase yet" else "No records in this phase", style = MaterialTheme.typography.bodyMedium)
            else Text("${recap.mealsLogged} meals · ${recap.loggingDays} days with records", style = MaterialTheme.typography.bodyMedium)
            when (points.size) {
                0 -> Text("No weight readings", style = MaterialTheme.typography.bodySmall)
                1 -> Text("${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(points.single().kilograms) else points.single().kilograms)} $unit · 1 reading · ${formatGoalDate(points.single().recordedAt, ZoneId.systemDefault())}", style = MaterialTheme.typography.bodySmall)
                else -> {
                    val change = requireNotNull(recap.weightChangeKg)
                    val shownChange = if (unit == "lb") WeightUnits.kilogramsToPounds(change) else change
                    Text("First ${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(points.first().kilograms) else points.first().kilograms)} $unit · ${formatGoalDate(points.first().recordedAt, ZoneId.systemDefault())}  →  Last ${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(points.last().kilograms) else points.last().kilograms)} $unit · ${formatGoalDate(points.last().recordedAt, ZoneId.systemDefault())}", style = MaterialTheme.typography.bodySmall)
                    Text("${points.size} readings · change ${if (shownChange > 0) "+" else ""}${goalDisplayNumber(shownChange)} $unit", style = MaterialTheme.typography.bodySmall)
                    val minValue = points.minOf { it.kilograms }
                    val maxValue = points.maxOf { it.kilograms }
                    val color = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 96.dp).semantics {
                        contentDescription = "Recorded weights chart. ${points.size} actual readings; first ${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(points.first().kilograms) else points.first().kilograms)} $unit, last ${goalDisplayNumber(if (unit == "lb") WeightUnits.kilogramsToPounds(points.last().kilograms) else points.last().kilograms)} $unit."
                    }) {
                        val xSpan = (recap.end ?: points.last().recordedAt).toEpochMilli() - recap.start.toEpochMilli()
                        val ySpan = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
                        val inset = 6.dp.toPx()
                        val plotted = points.map { point -> Offset.Zero.copy(
                            x = if (xSpan <= 0L) size.width / 2f else ((point.recordedAt.toEpochMilli() - recap.start.toEpochMilli()).toFloat() / xSpan * size.width).coerceIn(inset, size.width - inset),
                            y = size.height - inset - ((point.kilograms - minValue).toFloat() / ySpan.toFloat() * (size.height - 2 * inset)),
                        ) }
                        plotted.zipWithNext().forEach { (from, to) -> drawLine(color, from, to, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round) }
                        plotted.forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
                    }
                }
            }
            if (noNutrition) {
                if (recap.mealsLogged > 0) Text("No nutrition recorded", style = MaterialTheme.typography.bodySmall)
            } else {
                val metrics = listOf("Calories" to totals!!.calories, "Protein" to totals.protein, "Carbohydrate" to totals.carbohydrate, "Fat" to totals.fat)
                if (metrics.none { it.second.total != null }) Text("No full-coverage nutrition totals", style = MaterialTheme.typography.bodySmall)
                metrics.filter { it.second.total != null }.forEach { (label, metric) ->
                    Text("${goalDisplayNumber(metric.total!!)} ${if (label == "Calories") "kcal" else "g"} $label · ${metric.contributingMeals} meals", style = MaterialTheme.typography.bodySmall)
                }
            }
            val metrics = totals?.let { listOf("Calories" to it.calories, "Protein" to it.protein, "Carbohydrate" to it.carbohydrate, "Fat" to it.fat) }.orEmpty()
            val partial = metrics.maxOfOrNull { it.second.partialMealsExcluded } ?: 0
            val missing = (totals?.mealsWithoutNutrition ?: 0) + metrics.sumOf { it.second.missingFullCoverageMeals }
            val hasDetails = anyRecord || boundaryCount > 0
            val hasCaution = partial + missing + boundaryCount > 0
            if (hasDetails) {
                TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { stateDescription = if (detailsExpanded) "Nutrition and time details expanded" else "Nutrition and time details collapsed" }) {
                    Text(if (detailsExpanded) "Hide details" else if (hasCaution) "Some entries are excluded · Details" else "Nutrition and time details")
                }
                if (detailsExpanded) {
                    totals?.let { summary ->
                        if (summary.mealsWithoutNutrition > 0 || partial > 0) Text("Partial estimate meals excluded: $partial · Meals without nutrition: ${summary.mealsWithoutNutrition}", style = MaterialTheme.typography.bodySmall)
                        metrics.forEach { (label, metric) -> Text("$label · ${metric.contributingMeals} full-coverage meals${if (metric.missingFullCoverageMeals > 0) " · ${metric.missingFullCoverageMeals} missing" else ""}", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (boundaryCount > 0) Text("Local boundary time unavailable: ${recap.boundaryDateOnlyMealsExcluded} date-only meals · ${recap.boundaryWeightTimesUnavailable} weights at local midnight excluded", style = MaterialTheme.typography.bodySmall)
                    Text("Local dates use this device time zone. Recorded totals describe retained entries, not average intake.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatGoalDate(value: Instant, zone: ZoneId): String =
    value.atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
