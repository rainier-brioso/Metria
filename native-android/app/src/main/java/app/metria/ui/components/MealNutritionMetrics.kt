package app.metria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

@Composable
fun MealNutritionMetrics(calories: Double?, protein: Double?, carbs: Double?, fat: Double?) {
    val metrics = listOf("Calories" to (calories to "kcal"), "Protein" to (protein to "g"), "Carbs" to (carbs to "g"), "Fat" to (fat to "g"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (label, valueAndUnit) ->
                    val (value, unit) = valueAndUnit
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(value?.takeIf { it.isFinite() && it >= 0 }?.let {
                                NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it) + " $unit"
                            } ?: "Not available", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
