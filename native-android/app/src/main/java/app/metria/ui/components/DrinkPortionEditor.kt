package app.metria.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.metria.*

/** Stateless quantity control: its caller owns invalid input as well as the confirmed conversion. */
@Composable
fun DrinkPortionEditor(
    grams: String,
    portion: DrinkPortion?,
    amountText: String?,
    food: CatalogFood?,
    catalogRelease: String?,
    onChange: (String, DrinkPortion?, String?) -> Unit,
) {
    val options = if (food != null && catalogRelease != null) DrinkPortionPolicy.options(food, catalogRelease) else emptyList()
    val resolved = DrinkPortionPolicy.edited(portion, amountText)
    val portionDisplay = portion?.let(DrinkPortionPolicy::display)
    val text = if (portion == null) grams else amountText ?: portionDisplay?.amountText.orEmpty()
    val invalidGrams = portion == null && grams.isNotBlank() && grams.toIntOrNull()?.let { it in 1..2_000 } != true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                val bounded = value.take(24)
                if (portion == null) onChange(bounded, null, null) else {
                    val updated = DrinkPortionPolicy.edited(portion, bounded)
                    onChange(updated?.let(DrinkPortionPolicy::canonicalGrams)?.toString().orEmpty(), portion, bounded)
                }
            },
            label = { Text(if (portionDisplay?.isApproximate == true && amountText == null) "Portion (approx.)" else "Portion") },
            suffix = { Text(if (portion == null) "g" else if (portion.unit == "ml") "mL" else "servings") },
            isError = invalidGrams || portion != null && resolved == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = portion == null, onClick = { onChange(grams, null, null) },
                label = { Text("g") }, modifier = Modifier.heightIn(min = 48.dp))
            options.sortedBy { if (it.unit == "ml") 0 else 1 }.forEach { option ->
                FilterChip(
                    selected = portion?.let { it.unit == option.unit && it.sourceLabel == option.sourceLabel } == true,
                    onClick = {
                        val mass = grams.toIntOrNull()?.takeIf { it in 1..2_000 }
                        val initial = mass?.let { DrinkPortionPolicy.fromGrams(option, it) }
                        val template = initial ?: DrinkPortionPolicy.resolve(option, "1")
                        if (template != null) onChange(
                            initial?.let(DrinkPortionPolicy::canonicalGrams)?.toString().orEmpty(), template,
                            if (initial == null) "" else null,
                        )
                    },
                    label = { Text(option.label) }, modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        when {
            invalidGrams || portion != null && resolved == null -> Text("Enter an amount that converts to 1–2,000 g.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            resolved != null -> Text(
                "Nutrition estimate uses ${DrinkPortionPolicy.canonicalGrams(resolved)} g${if (portionDisplay?.isApproximate == true && amountText == null) " · displayed quantity is rounded" else ""} · ${resolved.sourceLabel}",
                style = MaterialTheme.typography.bodySmall)
            food == null -> Text("Choose a matching drink to use mL or measured servings.", style = MaterialTheme.typography.bodySmall)
            options.none { it.unit == "ml" } -> Text("No mL conversion is available for this food. Use grams or a listed serving.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
