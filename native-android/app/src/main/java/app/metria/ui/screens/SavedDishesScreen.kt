package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.metria.PersonalDish
import app.metria.PersonalDishStructure
import app.metria.ui.theme.MetriaColors

@Composable
fun MetriaSavedDishesScreen(
    dishes: List<PersonalDish>,
    onBack: () -> Unit,
    onDelete: (PersonalDish) -> String?,
) {
    var pendingDelete by remember { mutableStateOf<PersonalDish?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        Surface(color = MetriaColors.Canvas, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
                Surface(color = MetriaColors.Canvas) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to privacy settings")
                        }
                        Column(Modifier.padding(start = 4.dp)) {
                            Text("Saved private dishes", style = MaterialTheme.typography.headlineMedium)
                            Text("Encrypted on this phone", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Metria uses these only to suggest familiar dishes. A suggestion is never saved without your review.",
                            color = MetriaColors.InkMuted,
                        )
                    }
                    error?.let { message ->
                        item {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                                Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                    if (dishes.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No saved dishes yet", style = MaterialTheme.typography.titleLarge)
                                    Text("When you review a meal, leave “Remember this dish” on to make future reviews quicker.", color = MetriaColors.InkMuted)
                                }
                            }
                        }
                    } else {
                        items(dishes.sortedByDescending(PersonalDish::lastConfirmedAt), key = PersonalDish::id) { dish ->
                            Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(dish.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            PersonalDishStructure.componentItems(dish).joinToString { it.name },
                                            color = MetriaColors.InkMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                        )
                                        Text(
                                            if (dish.confirmationCount == 1) "Confirmed once" else "Confirmed ${dish.confirmationCount} times",
                                            color = MetriaColors.Primary,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        Text(
                                            if (dish.referenceThumbnailBase64 == null) "No photo context saved" else "Encrypted photo context saved",
                                            color = MetriaColors.InkMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = dish },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove ${dish.name}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { dish ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Forget ${dish.name}?") },
            text = { Text("This removes the saved dish from Metria's private memory. Meals you already recorded stay unchanged.") },
            confirmButton = {
                Button(onClick = {
                    error = onDelete(dish)
                    if (error == null) pendingDelete = null
                }) { Text("Forget dish") }
            },
            dismissButton = { Button(onClick = { pendingDelete = null }) { Text("Keep dish") } },
        )
    }
}
