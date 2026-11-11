package app.metria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.metria.compatible.CompatibleProviderSettings
import app.metria.compatible.CompatibleModel
import app.metria.ui.theme.MetriaTheme
import app.metria.ui.theme.MetriaColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CompatibleProviderSettingsCard(
    mode: String,
    settings: CompatibleProviderSettings?,
    onSelectMode: (String) -> Unit,
    onConfigure: (CompatibleProviderSettings, CharArray) -> String?,
    onDiscoverModels: (String, String, CharArray) -> List<CompatibleModel>,
    onDisconnect: () -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SettingsRow(
        icon = Icons.Rounded.Devices,
        title = "Computer or compatible server",
        supporting = when {
            settings == null -> "Use Ollama or another OpenAI-compatible connection"
            mode == "compatible" -> "${settings.model} · selected when you use the assistant"
            else -> "${compatibleProviderName(settings.kind)} · ${settings.model}"
        },
        accessibilityLabel = "Computer or compatible server assistant settings",
        onClick = { expanded = !expanded },
    )
    if (expanded) {
        Column(
            Modifier.fillMaxWidth().padding(start = 56.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Messages you choose to send go to this connection. Its operator can see them; your saved records stay encrypted on this phone.",
                color = MetriaColors.InkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (settings != null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Use ${compatibleProviderName(settings.kind)} ${settings.model} assistant"
                        }
                        .selectable(
                            selected = mode == "compatible",
                            role = Role.RadioButton,
                            onClick = {
                                if (mode == "compatible") onSelectMode("local")
                                else {
                                    error = "Open this connection to review the sharing details before enabling it."
                                    editorOpen = true
                                }
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == "compatible", onClick = null, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text("${compatibleProviderName(settings.kind)} · computer/server")
                        Text(settings.model, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (mode == "compatible") {
                OutlinedButton(onClick = { error = null; editorOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Edit connection")
                }
                TextButton(onClick = onDisconnect, modifier = Modifier.heightIn(min = 48.dp)) { Text("Disconnect this server") }
            } else {
                Button(onClick = { error = null; editorOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (settings == null) "Add a connection" else "Use this connection")
                }
                if (settings != null) {
                    TextButton(onClick = onDisconnect, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove saved connection") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    if (editorOpen) {
        CompatibleProviderEditorDialog(
            initialSettings = settings,
            onDismiss = { cancelled ->
                editorOpen = false
                if (cancelled) onCancel()
            },
            onConfigure = onConfigure,
            onDiscoverModels = onDiscoverModels,
            onSelectMode = onSelectMode,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CompatibleProviderEditorDialog(
    initialSettings: CompatibleProviderSettings?,
    onDismiss: (cancelled: Boolean) -> Unit,
    onConfigure: (CompatibleProviderSettings, CharArray) -> String?,
    onDiscoverModels: (String, String, CharArray) -> List<CompatibleModel>,
    onSelectMode: (String) -> Unit,
) {
    var kind by rememberSaveable(initialSettings?.kind) { mutableStateOf(initialSettings?.kind ?: "ollama") }
    var baseUrl by rememberSaveable(initialSettings?.baseUrl) { mutableStateOf(initialSettings?.baseUrl ?: OLLAMA_DEFAULT_BASE_URL) }
    var model by rememberSaveable(initialSettings?.model) { mutableStateOf(initialSettings?.model.orEmpty()) }
    var vision by rememberSaveable(initialSettings?.vision) { mutableStateOf(initialSettings?.vision ?: false) }
    var disableReasoning by rememberSaveable(initialSettings?.disableReasoning) { mutableStateOf(initialSettings?.disableReasoning ?: false) }
    // Deliberately not saveable: a credential must not be restored from saved UI state.
    var apiKey by remember { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<CompatibleModel>>(emptyList()) }
    var modelFilter by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var closing by remember { mutableStateOf(false) }
    val latestBusy = rememberUpdatedState(busy)
    val latestClosing = rememberUpdatedState(closing)
    val latestOnCancel = rememberUpdatedState(onDismiss)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            if (latestBusy.value && !latestClosing.value) latestOnCancel.value(true)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) { closing = true; onDismiss(true) } },
        title = { Text(if (initialSettings == null) "Add assistant connection" else "Edit assistant connection") },
        text = {
            Column(
                Modifier.fillMaxWidth().imePadding().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Choose how Metria reaches the model. This does not copy settings from another app or start a server.", color = MetriaColors.InkMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = kind == "ollama", onClick = {
                        if (kind != "ollama") {
                            kind = "ollama"; baseUrl = OLLAMA_DEFAULT_BASE_URL; model = ""
                            models = emptyList(); apiKey = ""; vision = false; error = null; status = null; consent = false
                        }
                    }, label = { Text("Ollama") }, enabled = !busy)
                    FilterChip(selected = kind == "custom", onClick = {
                        if (kind != "custom") {
                            kind = "custom"; baseUrl = ""; model = ""
                            models = emptyList(); apiKey = ""; vision = false; error = null; status = null; consent = false
                        }
                    }, label = { Text("Custom") }, enabled = !busy)
                }
                Text(
                    if (kind == "ollama") "Load your installed Ollama models and choose one marked Photos for meal analysis. The default address needs a connected tunnel to Ollama on your computer; otherwise use its reachable HTTPS address."
                    else "Use your OpenAI-compatible server address and load its available models.",
                    color = MetriaColors.InkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; error = null; models = emptyList(); status = null; vision = false; consent = false },
                    label = { Text("Server address") },
                    placeholder = { Text(if (kind == "ollama") "https://computer.example" else "https://computer.example/v1") },
                    supportingText = { Text("Use reachable HTTPS, or 127.0.0.1 for a phone-local server or connected tunnel. A PC loopback address alone cannot reach the PC.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; error = null; consent = false },
                    label = { Text("API key (optional)") },
                    supportingText = { Text("Leave blank to keep the saved key for this same address. For a new address, blank means no key. Disconnect to remove it.") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; error = null; vision = false; consent = false },
                    label = { Text("Model name") },
                    placeholder = { Text("Select from the server or enter a model name") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    enabled = !busy && baseUrl.isNotBlank(),
                    onClick = {
                        busy = true; error = null; status = null
                        val secret = apiKey.toCharArray()
                        scope.launch {
                            try {
                                val fetched = withContext(Dispatchers.IO) { onDiscoverModels(baseUrl, kind, secret) }
                                models = fetched.distinctBy { it.id }
                                if (models.firstOrNull { it.id == model }?.vision == false) vision = false
                                consent = false
                                status = when {
                                    models.isEmpty() -> "No models were returned. Add a vision model to your server for meal photos, then load again."
                                    models.any { it.vision == true } -> "Recommended for meal photos: choose a model marked Photos. Capability support does not guarantee estimate quality."
                                    else -> "No photo-capable model was confirmed. Configure a vision model on your server, then load again, or use text descriptions."
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = "Could not load models. Check the address, key and connection."
                            } finally {
                                secret.fill('\u0000')
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(if (busy) "Loading models…" else "Load available models") }
                if (models.size > 12) {
                    OutlinedTextField(modelFilter, { modelFilter = it }, label = { Text("Find a model") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    Text("Showing up to 12 matches. Enter part of a model name to narrow the list.",
                        style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
                models.filter { it.id.contains(modelFilter, ignoreCase = true) }.take(12).forEach { candidate ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = candidate.id == model, enabled = !busy, role = Role.RadioButton,
                                onClick = { model = candidate.id; vision = candidate.vision == true; error = null; consent = false }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = candidate.id == model, onClick = null, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(candidate.id, style = MaterialTheme.typography.bodyMedium)
                            Text(when (candidate.vision) {
                                true -> "Photos · recommended for meal analysis"
                                false -> "Text only · describe meals in chat"
                                null -> "Photo support unknown"
                            }, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                        }
                    }
                }
                status?.let { Text(it, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall) }
                ToggleChoiceRow(vision, !busy && models.firstOrNull { it.id == model }?.vision != false,
                    { vision = it; consent = false }, "Use this model for meal photos")
                if (models.firstOrNull { it.id == model }?.vision == false) Text(
                    "Choose a model marked Photos for meal analysis. This model can still use meal descriptions in chat.",
                    style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                ToggleChoiceRow(disableReasoning, !busy, { disableReasoning = it; consent = false }, "Turn off extended reasoning when supported")
                Text("When selected, the server receives the messages and any photo you explicitly choose to send. Its operator can see them. Saved records stay on this phone.",
                    color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                ToggleChoiceRow(consent, !busy, { consent = it }, "I agree to send selected messages to this connection when I use it.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && consent && baseUrl.isNotBlank() && model.isNotBlank(), onClick = {
                busy = true; error = null
                val secret = apiKey.toCharArray()
                apiKey = ""
                val config = CompatibleProviderSettings(
                    kind = kind,
                    baseUrl = baseUrl.trim(),
                    model = model.trim(),
                    vision = vision,
                    disableReasoning = disableReasoning,
                )
                scope.launch {
                    try {
                        val failure = withContext(Dispatchers.IO) { onConfigure(config, secret) }
                        if (failure == null) {
                            apiKey = ""
                            closing = true
                            busy = false
                            onSelectMode("compatible")
                            onDismiss(false)
                        } else error = failure
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = "Could not save this connection. Check the address and try again."
                    } finally {
                        secret.fill('\u0000')
                        busy = false
                    }
                }
            }) { Text(if (busy) "Saving…" else "Use this connection") }
        },
        dismissButton = {
            TextButton(onClick = { closing = true; onDismiss(true) }) { Text(if (busy) "Stop" else "Cancel") }
        },
    )
}

private const val OLLAMA_DEFAULT_BASE_URL = "http://127.0.0.1:11434/v1"

@Composable
private fun ToggleChoiceRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = Modifier.padding(end = 8.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

private fun compatibleProviderName(kind: String): String = when (kind) {
    "ollama" -> "Ollama"
    else -> "Custom provider"
}

@Preview(showBackground = true)
@Composable
private fun CompatibleProviderSettingsPreview() {
    MetriaTheme {
        CompatibleProviderSettingsCard(
            mode = "local",
            settings = CompatibleProviderSettings(
                kind = "custom",
                baseUrl = "https://computer.example/v1",
                model = "gemma3:12b",
            ),
            onSelectMode = {},
            onConfigure = { _, _ -> null },
            onDiscoverModels = { _, _, _ -> emptyList() },
            onDisconnect = {},
            onCancel = {},
        )
    }
}
