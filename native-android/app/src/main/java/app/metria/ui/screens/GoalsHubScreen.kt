package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import app.metria.GoalPhase
import app.metria.GoalSetup
import app.metria.NutritionTargets
import app.metria.ui.components.GoalDailyReferencesCard
import app.metria.ui.components.GoalHubCurrentCard
import app.metria.ui.components.GoalHubPreviousPhaseRow
import app.metria.ui.components.GoalMethodDisclosure
import app.metria.ui.components.MacroMethodDetails
import app.metria.ui.components.PhaseEnergyMethodDetails
import app.metria.ui.theme.MetriaColors
import androidx.compose.foundation.ScrollState

@Composable
fun GoalsHubScreen(
    targets: NutritionTargets?,
    setup: GoalSetup?,
    latestWeightKg: Double?,
    displayUnit: String,
    scrollState: ScrollState = rememberScrollState(),
    referencesExpanded: Boolean,
    moreOptionsExpanded: Boolean,
    manageDataExpanded: Boolean,
    saving: Boolean,
    onToggleReferences: () -> Unit,
    onEditReferences: () -> Unit,
    onOpenPhase: (String) -> Unit,
    onEditCurrent: () -> Unit,
    onNewPhase: () -> Unit,
    onSetUpGoal: () -> Unit,
    onRecalculate: () -> Unit,
    onAbout: () -> Unit,
    onToggleManageData: () -> Unit,
    onToggleMoreOptions: () -> Unit,
    onDeleteAll: () -> Unit,
    onDone: () -> Unit,
) {
    var methodDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxHeight()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Goals", style = MaterialTheme.typography.headlineSmall)
                    Text("Your choices and daily references", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
                if (setup != null) {
                    TextButton(onClick = onNewPhase, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("New phase")
                    }
                }
            }

            setup?.current?.let { phase ->
                GoalHubCurrentCard(
                    phase = phase,
                    latestWeightKg = latestWeightKg,
                    displayUnit = displayUnit,
                    onOpen = { onOpenPhase(phase.id) },
                    onEdit = onEditCurrent,
                )
            } ?: run {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No goal chosen", style = MaterialTheme.typography.titleMedium)
                        Text("Weight and meal tracking work without a goal. Set one up whenever you choose.",
                            style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                    }
                }
            }

            GoalDailyReferencesCard(
                targets = targets,
                estimatedCalories = setup?.current?.acceptedEstimate != null,
                macroSuggestion = setup?.current?.macroSuggestion,
                phaseEnergySuggestion = setup?.current?.phaseEnergySuggestion,
                expanded = referencesExpanded,
                onToggle = onToggleReferences,
                onEdit = onEditReferences,
            )
            if (setup?.current?.acceptedEstimate != null || setup?.current?.macroSuggestion != null || setup?.current?.phaseEnergySuggestion != null) {
                GoalMethodDisclosure(methodDetailsExpanded, { methodDetailsExpanded = !methodDetailsExpanded }) {
                    setup?.current?.acceptedEstimate?.let { MaintenanceDetails(it) }
                    setup?.current?.macroSuggestion?.let { MacroMethodDetails(it) }
                    setup?.current?.phaseEnergySuggestion?.let { PhaseEnergyMethodDetails(it) }
                }
            }

            Text("Previous phases", style = MaterialTheme.typography.titleMedium)
            if (setup?.history.isNullOrEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No previous phases", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (setup == null) "Starting a new phase adds the current goal to your history after you confirm it."
                            else "Previous goals appear here when you start and confirm a new phase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MetriaColors.InkMuted,
                        )
                    }
                }
            } else {
                setup.history.asReversed().forEach { phase ->
                    GoalHubPreviousPhaseRow(phase, displayUnit) { onOpenPhase(phase.id) }
                }
            }

            TextButton(onClick = onToggleMoreOptions, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (moreOptionsExpanded) "Hide more goal options" else "More goal options")
            }
            if (moreOptionsExpanded) {
                TextButton(onClick = onRecalculate, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (setup == null) "Explore a maintenance estimate" else "Recalculate maintenance estimate")
                }
                TextButton(onClick = onAbout, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("About goal setup")
                }
                if (setup != null) {
                    TextButton(onClick = onToggleManageData, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (manageDataExpanded) "Hide goal data controls" else "Manage goal data")
                    }
                    if (manageDataExpanded) {
                        Text("Deleting removes saved goal phases and calculation inputs. Daily references remain as member-entered. Meals and weights stay unchanged.",
                            style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                        TextButton(onClick = onDeleteAll, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete profile and all goal phases")
                        }
                    }
                }
            }
        }
        Button(
            onClick = if (setup == null) onSetUpGoal else onEditCurrent,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp),
        ) {
            Text(if (setup == null) "Set up an optional goal" else "Edit weight goal")
        }
        TextButton(onClick = onDone, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Done")
        }
    }
}
