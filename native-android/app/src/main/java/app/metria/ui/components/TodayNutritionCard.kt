package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.MealNutritionSummary
import app.metria.NutritionTargets
import app.metria.ui.theme.MetriaColors
import java.text.NumberFormat

/** Compact, expandable summary of nutrition attached to today's recorded meals. */
@Composable
fun TodayNutritionCard(
    summary: MealNutritionSummary,
    targets: NutritionTargets?,
    onOpenReferences: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val complete = summary.completeNutritionMeals
    val partial = summary.partialNutritionMeals
    val covered = complete + partial
    val recorded = summary.recordedMeals
    val withoutNutrition = (recorded - covered).coerceAtLeast(0)
    val calories = addKnown(summary.caloriesKcal, summary.partialCaloriesKcal)
    val protein = addKnown(summary.proteinGrams, summary.partialProteinGrams)
    val carbs = addKnown(summary.carbohydrateGrams, summary.partialCarbohydrateGrams)
    val fat = addKnown(summary.fatGrams, summary.partialFatGrams)
    val hasEstimate = covered > 0
    val completeCoverage = hasEstimate && partial == 0 && withoutNutrition == 0
    val estimateLabel = if (completeCoverage) "Estimate" else "Partial estimate"

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { expanded = !expanded }
                    .semantics {
                        contentDescription = "Nutrition details"
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Nutrition", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Hide nutrition details" else "Show nutrition details", tint = MaterialTheme.colorScheme.primary)
            }

            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackCalories = maxWidth < 360.dp || fontScale > 1.3f
                if (stackCalories) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(calories?.let { formatNutrition(it, 0) } ?: "—", style = MaterialTheme.typography.headlineMedium)
                            Text("kcal", style = MaterialTheme.typography.labelLarge, color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        targets?.caloriesKcal?.let { target ->
                            Text(calorieReferenceText(calories, target), style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(calories?.let { formatNutrition(it, 0) } ?: "—", style = MaterialTheme.typography.headlineMedium)
                        Text("kcal", style = MaterialTheme.typography.labelLarge, color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 4.dp))
                        Spacer(Modifier.weight(1f))
                        targets?.caloriesKcal?.let { target ->
                            Text(calorieReferenceText(calories, target), style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                        }
                    }
                }
            }
            val calorieReference = targets?.caloriesKcal
            if (calories != null && calories >= 0.0 && calorieReference != null && calorieReference > 0.0) {
                LinearProgressIndicator(
                    progress = { (calories / calorieReference).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Calorie estimate ${formatNutrition(calories, 0)} of ${formatNutrition(calorieReference, 0)} kilocalorie daily reference"
                    },
                )
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 280.dp || fontScale > 1.3f
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionMacro("Protein", protein, targets?.proteinGrams, Modifier.fillMaxWidth())
                        NutritionMacro("Carbs", carbs, targets?.carbohydrateGrams, Modifier.fillMaxWidth())
                        NutritionMacro("Fat", fat, targets?.fatGrams, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionMacro("Protein", protein, targets?.proteinGrams, Modifier.weight(1f))
                        NutritionMacro("Carbs", carbs, targets?.carbohydrateGrams, Modifier.weight(1f))
                        NutritionMacro("Fat", fat, targets?.fatGrams, Modifier.weight(1f))
                    }
                }
            }

            if (recorded == 0) {
                Text("No meals recorded today.", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            } else if (!hasEstimate) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, contentDescription = "Unavailable", tint = MetriaColors.InkMuted, modifier = Modifier.size(18.dp))
                    Text("Nutrition unavailable · 0 of $recorded meals included", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
            } else {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, contentDescription = estimateLabel, tint = if (completeCoverage) MaterialTheme.colorScheme.primary else MetriaColors.Estimated, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(estimateLabel, style = MaterialTheme.typography.labelMedium,
                            color = if (completeCoverage) MaterialTheme.colorScheme.primary else MetriaColors.Estimated)
                        Text("$covered of $recorded meals included", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MetriaColors.Divider)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coverage", style = MaterialTheme.typography.titleSmall)
                    CoverageRow("Recorded meals", recorded)
                    CoverageRow("Meals with complete nutrition", complete)
                    CoverageRow("Partial estimates", partial)
                    CoverageRow("Meals without nutrition data", withoutNutrition)
                    Text(
                        if (hasEstimate) "Totals include meals with nutrition data. Meals without data are not counted as zero."
                        else "No nutrition totals are available. Missing values are not counted as zero.",
                        style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted,
                    )
                    TextButton(onClick = onOpenReferences, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (targets == null) "Set daily references" else "View daily references")
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionMacro(label: String, value: Double?, target: Double?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MetriaColors.InkMuted)
        Text(value?.let { "${formatNutrition(it, 1)} g" } ?: "Unavailable", style = MaterialTheme.typography.titleSmall)
        if (value != null && value >= 0.0 && target != null && target > 0.0) {
            Text("Reference ${formatNutrition(target, 0)} g", style = MaterialTheme.typography.labelSmall, color = MetriaColors.InkMuted)
            LinearProgressIndicator(
                progress = { (value / target).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "$label estimate ${formatNutrition(value, 1)} of ${formatNutrition(target, 0)} grams daily reference"
                },
            )
            if (value > target) Text("Above reference", style = MaterialTheme.typography.labelSmall, color = MetriaColors.InkMuted)
        } else if (target != null && target > 0.0) {
            Text("Reference ${formatNutrition(target, 0)} g", style = MaterialTheme.typography.labelSmall, color = MetriaColors.InkMuted)
        }
    }
}

@Composable
private fun CoverageRow(label: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(count.toString(), style = MaterialTheme.typography.labelLarge)
    }
}

private fun addKnown(complete: Double?, partial: Double?): Double? = when {
    complete == null && partial == null -> null
    else -> (complete ?: 0.0) + (partial ?: 0.0)
}

private fun formatNutrition(value: Double, maxFractionDigits: Int): String =
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = maxFractionDigits }.format(value)

private fun calorieReferenceText(value: Double?, target: Double): String =
    if (value != null && value > target) "Above daily reference of ${formatNutrition(target, 0)} kcal"
    else "of ${formatNutrition(target, 0)} kcal reference"
