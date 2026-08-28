package app.metria.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.BuildConfig
import app.metria.CycleContext
import app.metria.LocalAnalysisAudit
import app.metria.ModelStatus
import app.metria.NutritionTargets
import app.metria.PersonalDish
import app.metria.ui.theme.MetriaColors

/**
 * Approved direction: a calm, single-screen settings hub. Routine status and choices stay
 * scannable here; recovery, dish management, and development diagnostics open focused views.
 */
@Composable
fun MetriaPrivacySettingsScreen(
    unit: String,
    nutritionTargets: NutritionTargets?,
    modelStatus: ModelStatus,
    analysisAudits: List<LocalAnalysisAudit>,
    cycleContext: CycleContext,
    personalDishes: List<PersonalDish>,
    archiveMessage: String?,
    onChangeUnit: () -> Unit,
    onEditNutritionTargets: () -> Unit,
    onEditCycleContext: () -> Unit,
    onDeletePersonalDish: (PersonalDish) -> String?,
    onExportArchive: (CharArray) -> Unit,
    onImportArchive: () -> Unit,
    onLock: () -> Unit,
) {
    var exportSheetOpen by rememberSaveable { mutableStateOf(false) }
    var savedDishesOpen by rememberSaveable { mutableStateOf(false) }
    var privacyDetailsOpen by rememberSaveable { mutableStateOf(false) }
    var assistantDetailsOpen by rememberSaveable { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    var diagnosticsOpen by rememberSaveable { mutableStateOf(false) }

    Text("Privacy & settings", style = MaterialTheme.typography.displaySmall)
    PrivacyPromiseCard(onLearnMore = { privacyDetailsOpen = true })

    SettingsSection("Personal settings") {
        SettingsRow(
            icon = Icons.Rounded.Scale,
            title = "Weight unit",
            supporting = if (unit == "kg") "Kilograms (kg)" else "Pounds (lb)",
            accessibilityLabel = "Weight unit. ${if (unit == "kg") "Kilograms" else "Pounds"}. Change unit",
            onClick = onChangeUnit,
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.Flag,
            title = "Daily goals",
            supporting = nutritionTargetsSummary(nutritionTargets),
            accessibilityLabel = "Daily goals. ${nutritionTargetsSummary(nutritionTargets)}. Edit goals",
            onClick = onEditNutritionTargets,
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.CalendarMonth,
            title = "Cycle-aware context",
            supporting = cycleContextSummary(cycleContext),
            accessibilityLabel = "Cycle-aware context. ${cycleContextSummary(cycleContext)}. Edit setting",
            onClick = onEditCycleContext,
        )
    }

    SettingsSection("Private features") {
        SettingsRow(
            icon = Icons.Rounded.SmartToy,
            title = "Local assistant",
            supporting = assistantStatusSummary(modelStatus.state),
            accessibilityLabel = "Local assistant. ${assistantStatusSummary(modelStatus.state)}. Show details",
            onClick = { assistantDetailsOpen = true },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.Restaurant,
            title = "Saved private dishes",
            supporting = privateDishSummary(personalDishes.size),
            accessibilityLabel = "Saved private dishes. ${privateDishSummary(personalDishes.size)}. Manage dishes",
            onClick = { savedDishesOpen = true },
        )
    }

    SettingsSection("Encrypted recovery") {
        SettingsRow(
            icon = Icons.Rounded.Archive,
            title = "Create recovery archive",
            supporting = "Save an encrypted copy somewhere you choose",
            accessibilityLabel = "Create encrypted recovery archive",
            onClick = { exportSheetOpen = true },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.Restore,
            title = "Restore an archive",
            supporting = "Review it before replacing records on this phone",
            accessibilityLabel = "Restore encrypted recovery archive",
            onClick = onImportArchive,
        )
    }

    archiveMessage?.let { message ->
        RecoveryMessage(message)
    }

    SettingsSection("About") {
        SettingsRow(
            icon = Icons.Rounded.Info,
            title = "About Metria",
            supporting = "Private, offline-first wellness companion",
            accessibilityLabel = "About Metria",
            onClick = { aboutOpen = true },
        )
        if (BuildConfig.DEBUG) {
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = "Developer diagnostics",
                supporting = "Local analysis timing and outcomes",
                accessibilityLabel = "Open development-only local analysis diagnostics",
                onClick = { diagnosticsOpen = true },
            )
        }
    }

    OutlinedButton(onClick = onLock, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text("Lock Metria")
    }

    if (exportSheetOpen) ArchivePassphraseDialog(
        onDismiss = { exportSheetOpen = false },
        onConfirm = { passphrase -> exportSheetOpen = false; onExportArchive(passphrase) },
    )
    if (savedDishesOpen) MetriaSavedDishesScreen(
        dishes = personalDishes,
        onBack = { savedDishesOpen = false },
        onDelete = onDeletePersonalDish,
    )
    if (privacyDetailsOpen) PrivacyDetailsDialog(onDismiss = { privacyDetailsOpen = false })
    if (assistantDetailsOpen) AssistantDetailsDialog(modelStatus, onDismiss = { assistantDetailsOpen = false })
    if (aboutOpen) AboutDialog(onDismiss = { aboutOpen = false })
    if (diagnosticsOpen && BuildConfig.DEBUG) DiagnosticsDialog(analysisAudits, modelStatus, onDismiss = { diagnosticsOpen = false })
}

@Composable
private fun PrivacyPromiseCard(onLearnMore: () -> Unit) {
    Surface(color = MetriaColors.AccentSoft, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(color = MetriaColors.Surface, shape = MaterialTheme.shapes.medium) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MetriaColors.Verified,
                    modifier = Modifier.padding(12.dp).size(26.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Protected on this phone", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your records and saved dishes are encrypted. Metria has no account, ads, analytics, or automatic cloud sync.",
                    color = MetriaColors.InkMuted,
                )
                TextButton(onClick = onLearnMore) { Text("What stays private") }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = accessibilityLabel }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = MetriaColors.SurfaceSubtle, shape = MaterialTheme.shapes.small) {
            Icon(icon, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(10.dp).size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(supporting, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MetriaColors.InkMuted)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MetriaColors.Divider, modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun RecoveryMessage(message: String) {
    val failed = archiveMessageIsFailure(message)
    Surface(
        color = if (failed) MaterialTheme.colorScheme.errorContainer else MetriaColors.AccentSoft,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics { contentDescription = if (failed) "Recovery error. $message" else "Recovery status. $message" },
    ) {
        Text(
            message,
            color = if (failed) MaterialTheme.colorScheme.onErrorContainer else MetriaColors.Ink,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ArchivePassphraseDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Protect recovery archive") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a passphrase with at least 12 characters. Metria cannot reset it or recover the archive for you.")
                OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Archive passphrase") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MetriaColors.Danger) }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    passphrase.length < 12 -> error = "Use at least 12 characters."
                    passphrase != confirmation -> error = "The passphrases do not match."
                    else -> onConfirm(passphrase.toCharArray())
                }
            }) { Text("Choose file location") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PrivacyDetailsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your privacy in Metria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrivacyDetail("Encrypted records", "Weights, meals, goals, cycle context, and saved dishes stay in your protected vault.")
                PrivacyDetail("Local assistance", "Meal photos and prompts are processed on this phone. Temporary originals are deleted after processing.")
                PrivacyDetail("Member-controlled recovery", "Nothing syncs automatically. Only an encrypted archive you create can restore your records elsewhere.")
                PrivacyDetail("Biometric protection", "Android tells Metria only whether unlock succeeded. Metria never receives or stores your fingerprint.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun PrivacyDetail(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AssistantDetailsDialog(status: ModelStatus, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local assistant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(assistantStatusSummary(status.state), style = MaterialTheme.typography.titleSmall)
                Text("The assistant is stored inside Metria and works without sending your prompts, photos, or records to a server.", color = MetriaColors.InkMuted)
                Text("Model storage: ${formatStorage(status.expectedBytes)}", color = MetriaColors.InkMuted)
                Text("Removing the app removes both the model and your local vault unless you first create a recovery archive.", color = MetriaColors.InkMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${BuildConfig.VERSION_NAME}")
                Text("A private, offline-first Android companion for reviewing weight, meals, and progress.", color = MetriaColors.InkMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun DiagnosticsDialog(audits: List<LocalAnalysisAudit>, modelStatus: ModelStatus, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developer diagnostics") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Encrypted timing and outcome metadata only. No photo or model response is stored.", color = MetriaColors.InkMuted)
                Text("Current local runtime", style = MaterialTheme.typography.titleSmall)
                Text(modelStatus.backendSummary, color = MetriaColors.InkMuted)
                Text(modelStatus.diagnostics, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                if (audits.isEmpty()) {
                    Text("No local image analyses recorded yet.")
                } else {
                    audits.sortedByDescending(LocalAnalysisAudit::completedAt).take(10).forEach { audit ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${audit.operation.replace('_', ' ')} · ${"%.1f".format(audit.durationMillis / 1_000.0)} s · ${audit.outcome.replace('_', ' ')}")
                            Text("${audit.modelId}${audit.failureCategory?.let { " · $it" }.orEmpty()}", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                modelStatus.debugLatestModelResponse?.let { response ->
                    Text("Latest in-memory response", style = MaterialTheme.typography.titleSmall)
                    Text("Clears on the next operation, vault lock, backgrounding, or restart.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                    Text(response, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

internal fun nutritionTargetsSummary(targets: NutritionTargets?): String {
    if (targets == null) return "No daily goals set"
    val values = listOfNotNull(
        targets.caloriesKcal?.let { "${it.toInt()} kcal" },
        targets.proteinGrams?.let { "${it.toInt()} g protein" },
        targets.carbohydrateGrams?.let { "${it.toInt()} g carbs" },
        targets.fatGrams?.let { "${it.toInt()} g fat" },
    )
    return values.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "No daily goals set"
}

internal fun cycleContextSummary(context: CycleContext): String = when {
    !context.enabled -> "Off · optional"
    context.periodStarts.isEmpty() -> "On · no dates saved"
    context.periodStarts.size == 1 -> "On · 1 date saved privately"
    else -> "On · ${context.periodStarts.size} dates saved privately"
}

internal fun privateDishSummary(count: Int): String = when (count) {
    0 -> "None yet · you choose what Metria remembers"
    1 -> "1 dish encrypted on this phone"
    else -> "$count dishes encrypted on this phone"
}

internal fun assistantStatusSummary(state: String): String = when (state) {
    "loaded" -> "Ready · working entirely on this phone"
    "ready" -> "Available offline · starts when needed"
    "initializing", "verifying" -> "Getting ready on this phone"
    "thinking", "testing" -> "Working locally now"
    "downloading" -> "Downloading the required local assistant"
    else -> "Needs attention before local assistance can run"
}

internal fun archiveMessageIsFailure(message: String): Boolean = listOf(
    "Archive was not", "Recovery did not", "Archive could not", "No archive was selected",
).any(message::startsWith)

private fun formatStorage(bytes: Long): String = if (bytes <= 0L) "not available" else "%.1f GB on this phone".format(bytes / 1_073_741_824.0)
