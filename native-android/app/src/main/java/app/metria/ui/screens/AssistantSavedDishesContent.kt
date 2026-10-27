package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import app.metria.PersonalDish
import app.metria.PersonalDishStructure
import app.metria.PersonalDishMatcher
import app.metria.ui.theme.MetriaColors

/** Search and choose from the member's existing encrypted dish memory. Selection only opens Review. */
@Composable
fun AssistantSavedDishesContent(
    dishes: List<PersonalDish>,
    enabled: Boolean = true,
    busy: Boolean = false,
    diagnostic: String? = null,
    onSelectDish: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalized = PersonalDishMatcher.identityKey(query)
    val visible = dishes.sortedByDescending(PersonalDish::lastConfirmedAt).filter { dish ->
        normalized.isBlank() || PersonalDishMatcher.identityKey(dish.name).contains(normalized) ||
            dish.aliases.any { PersonalDishMatcher.identityKey(it).contains(normalized) } ||
            dish.items.any { PersonalDishMatcher.identityKey(it.name).contains(normalized) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a saved dish", style = MaterialTheme.typography.headlineSmall)
                Text("Select a dish to review its foods and portion. Nothing is added until you save the review.",
                    style = MaterialTheme.typography.bodyMedium, color = MetriaColors.InkMuted)
            }
        }
        if (busy) item {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Preparing your saved dish…", color = MetriaColors.InkMuted,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        diagnostic?.let { message -> item {
            Text(message, color = MetriaColors.Danger,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } }
        if (dishes.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search saved dishes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        when {
            dishes.isEmpty() -> item { Text("No saved dishes yet. Review and save a meal to remember it here.", color = MetriaColors.InkMuted) }
            visible.isEmpty() -> item { Text("No saved dishes match that search.", color = MetriaColors.InkMuted) }
            else -> {
                items(visible, key = PersonalDish::id) { dish ->
                    Card(
                        onClick = { onSelectDish(dish.id) },
                        enabled = enabled,
                        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
                    ) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MetriaColors.Primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(dish.name, style = MaterialTheme.typography.titleMedium)
                                val foods = PersonalDishStructure.componentItems(dish).joinToString(", ") { it.name }
                                Text(foods, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                            }
                            Text("Review", style = MaterialTheme.typography.labelLarge, color = MetriaColors.Primary)
                        }
                    }
                }
            }
        }
    }
}
