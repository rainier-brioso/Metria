package app.metria.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The original proposal remains available after leaving review until explicitly discarded. */
@Composable
fun MealDraftDiscardAction(onDiscard: () -> Unit) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Discard draft")
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this meal draft?") },
            text = { Text("This removes only the unsaved suggestion. Saved meals and your records stay unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard draft") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Keep draft") }
            },
        )
    }
}
