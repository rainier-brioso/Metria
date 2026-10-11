package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalWelcomeSheet(onSetUp: () -> Unit, onContinueTracking: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onContinueTracking, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.Spa, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Start with your direction", style = MaterialTheme.typography.headlineMedium)
                        Text("Maintain, gain, lose or simply track. Choose what fits your next chapter.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                GoalWelcomePoint("Optional by design", "Meals and weight tracking work with or without targets.")
                GoalWelcomePoint("A reference you review", "Explore an approximate maintenance-energy range or enter your own daily references. Nothing is saved until you confirm.")
                GoalWelcomePoint("Personal stays personal", "Setup answers stay on this phone. Confirmed goal information is stored in your encrypted vault.")
                Text("You can come back from Today → Your goal and daily references.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSetUp, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Set up my goal") }
                TextButton(onClick = onContinueTracking, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continue tracking") }
            }
        }
    }
}

@Composable
private fun GoalWelcomePoint(title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
