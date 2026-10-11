package app.metria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MealPortionControl(onApply: (Double) -> Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    var factor by rememberSaveable { mutableStateOf("1.0") }
    var error by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true; error = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Adjust whole portion") }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Adjust current portions") },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Multiply every food’s current grams. Check the new portions before saving. This does not convert raw and cooked weights.")
                OutlinedTextField(value = factor, onValueChange = { factor = it; error = false }, label = { Text("Multiplier · for example 0.5 or 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (error) Text("Use 0.25–4. Every food needs valid grams, and each resulting portion must be 1–2,000 g.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = {
            val value = factor.replace(',', '.').toDoubleOrNull()
            if (value != null && onApply(value)) { open = false; factor = "1.0" } else error = true
        }) { Text("Apply portions") } },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
    )
}
