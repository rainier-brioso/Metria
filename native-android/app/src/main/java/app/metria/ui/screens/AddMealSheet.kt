package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.MealSlots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(slot: String, onDismiss: () -> Unit, onPhoto: () -> Unit,
                 onSpeak: () -> Unit, onType: () -> Unit, onManual: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add ${MealSlots.label(slot).lowercase()}", style = MaterialTheme.typography.headlineMedium)
            Text("Today · How would you like to describe it?")
            listOf("Meal photo · Take or choose" to onPhoto, "Speak to Metria" to onSpeak,
                "Type to Metria" to onType, "Enter manually" to onManual).forEach { (label, action) ->
                OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
            }
        }
    }
}
