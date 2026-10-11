package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.GoalPhase
import app.metria.ui.components.SavedDailyReferenceCard
import app.metria.ui.components.SavedWeightContextCard
import app.metria.ui.components.MacroMethodDetails
import app.metria.ui.components.PhaseEnergyMethodDetails

/** Read-only successful-save journey. All state and navigation belong to the host task. */
@Composable
internal fun GoalSavedSummaryScreen(
    phase: GoalPhase, page: Int, displayUnit: String, newPhase: Boolean, replacedPhase: Boolean,
    methodExpanded: Boolean, onToggleMethod: () -> Unit,
    onNext: () -> Unit, onBack: () -> Unit, onFinish: () -> Unit,
) {
    val summaryScroll = remember(page) { ScrollState(0) }
    Column(Modifier.fillMaxHeight()) {
        Column(Modifier.weight(1f).verticalScroll(summaryScroll).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row {
                Text(when (page) {
                    1 -> "Your daily references"
                    2 -> "Your weight context"
                    else -> if (newPhase) "New phase saved" else "Goal saved"
                }, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
                IconButton(onClick = onFinish) { Icon(Icons.Rounded.Close, "Close saved goal summary") }
            }
            when (page) {
                0 -> {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (newPhase && replacedPhase) "Your new phase is current. The previous phase is in your history." else "Your reviewed choices are saved.")
                    Text("You can review your saved references or return to Goals.")
                }
                1 -> {
                    Text("Saved summary 1 of 2", style = MaterialTheme.typography.labelLarge)
                    SavedDailyReferenceCard(phase)
                    if (phase.acceptedEstimate != null || phase.macroSuggestion != null || phase.phaseEnergySuggestion != null) {
                        TextButton(onClick = onToggleMethod, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (methodExpanded) "Hide method and inputs" else "Method and inputs") }
                        if (methodExpanded) {
                            phase.acceptedEstimate?.let { MaintenanceDetails(it) }
                            phase.macroSuggestion?.let { MacroMethodDetails(it) }
                            phase.phaseEnergySuggestion?.let { PhaseEnergyMethodDetails(it) }
                        }
                    }
                }
                2 -> {
                    Text("Saved summary 2 of 2", style = MaterialTheme.typography.labelLarge)
                    SavedWeightContextCard(phase, displayUnit)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = if (page == 2) onFinish else onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(when (page) { 0 -> "Review saved references"; 1 -> "Next: weight context"; else -> "Continue to Goals" })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (page > 0) TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                if (page < 2) TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = 48.dp)) { Text("Return to Goals") }
            }
        }
    }
}
