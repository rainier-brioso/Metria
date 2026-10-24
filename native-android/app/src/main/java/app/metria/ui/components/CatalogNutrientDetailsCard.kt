package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.CatalogNutrientAmount
import java.util.Locale

@Composable
fun CatalogNutrientDetailsCard(values: List<CatalogNutrientAmount>, release: String?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (expanded) "Available nutrients · Hide" else "Available nutrients · Show",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(role = Role.Button) { expanded = !expanded },
            )
            if (expanded) {
                Text("USDA FoodData Central${release?.let { " · $it" }.orEmpty()} · selected foods and portions",
                    style = MaterialTheme.typography.bodySmall)
                if (values.isEmpty()) Text("No additional nutrient values are available for the selected foods.")
                values.forEach { nutrient ->
                    Column {
                        Text(nutrient.name, style = MaterialTheme.typography.bodyMedium)
                        val amount = if (nutrient.amount == 0.0) "0" else
                            if (nutrient.amount < 0.01) "<0.01" else String.format(Locale.getDefault(), "%.2f", nutrient.amount)
                        Text("$amount ${nutrient.unit}" +
                            if (nutrient.includedFoods < nutrient.totalFoods) " · Partial: ${nutrient.includedFoods} of ${nutrient.totalFoods} foods" else "",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Missing values are unknown. No daily reference percentages are assigned.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
