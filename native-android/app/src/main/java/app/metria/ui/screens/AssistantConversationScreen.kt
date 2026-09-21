package app.metria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.MealProposal
import app.metria.ModelStatus
import app.metria.SafeMarkdownText
import app.metria.WeightEntry
import app.metria.ui.theme.MetriaColors

data class MetriaChatMessage(
    val text: String,
    val fromMember: Boolean,
    val localEvidence: Boolean = false,
)

@Composable
fun MetriaAssistantHome(
    modelStatus: ModelStatus,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenChat: () -> Unit,
    onSearchFoods: () -> Unit,
    onScanLabel: () -> Unit,
    onAnalyseMeal: () -> Unit,
) {
    Text("Assistant", style = MaterialTheme.typography.displaySmall)
    Surface(color = MetriaColors.PrimarySoft, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = MetriaColors.Surface, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(12.dp).size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(assistantHomeTitle(modelStatus.state), style = MaterialTheme.typography.titleLarge)
                Text(assistantHomeSupporting(modelStatus.state), color = MetriaColors.InkMuted)
            }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth()) {
            when (modelStatus.state) {
                "absent", "removed", "failed" -> AssistantHomeRow(
                    Icons.Rounded.Download,
                    if (modelStatus.state == "failed") "Try download again" else "Download local assistant",
                    "Required for private voice, chat, and photo help",
                    onDownload,
                )
                "downloading" -> {
                    val percent = if (modelStatus.expectedBytes > 0) (modelStatus.downloadedBytes * 100 / modelStatus.expectedBytes).coerceIn(0, 100) else 0
                    AssistantHomeRow(Icons.Rounded.Close, "Cancel download", "$percent% downloaded", onCancelDownload)
                }
                else -> {
                    AssistantHomeRow(Icons.AutoMirrored.Rounded.Chat, "Chat with Metria", "Ask about your records or record something", onOpenChat)
                    AssistantHomeDivider()
                    AssistantHomeRow(Icons.Rounded.CameraAlt, "Analyse a meal", "Take or choose a photo", onAnalyseMeal)
                    AssistantHomeDivider()
                    AssistantHomeRow(Icons.Rounded.DocumentScanner, "Scan a nutrition label", "Read package values locally", onScanLabel)
                    AssistantHomeDivider()
                    AssistantHomeRow(Icons.Rounded.Search, "Search foods", "Browse the offline nutrition list", onSearchFoods)
                }
            }
        }
    }
    if (modelStatus.imageState == "failed") ErrorMessage(modelStatus.imageReason)
}

@Composable
private fun AssistantHomeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, supporting: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
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
    }
}

@Composable
private fun AssistantHomeDivider() {
    androidx.compose.material3.HorizontalDivider(color = MetriaColors.Divider, modifier = Modifier.padding(start = 68.dp))
}

internal fun assistantHomeTitle(state: String): String = when (state) {
    "loaded" -> "Ready when you are"
    "ready", "initializing", "verifying" -> "Getting ready on this phone"
    "downloading" -> "Downloading your assistant"
    "failed" -> "Assistant needs attention"
    else -> "Set up your local assistant"
}

internal fun assistantHomeSupporting(state: String): String = when (state) {
    "loaded" -> "Private help with meals, weight, and progress"
    "downloading" -> "The required model stays inside Metria"
    "failed" -> "Your records are safe; try the model download again"
    else -> "Stored locally and available offline after setup"
}

/**
 * Approved Assistant slice: conversation is a full-screen, keyboard-safe destination while
 * tool selection and vault mutations remain owned by the existing coordinator.
 */
@Composable
fun MetriaAssistantConversationScreen(
    messages: List<MetriaChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    assistantThinking: Boolean,
    mealDraftThinking: Boolean,
    streamingReply: String?,
    assistantDiagnostic: String?,
    coachDiagnostic: String?,
    savedWeight: WeightEntry?,
    mealProposal: MealProposal?,
    recordingVoice: Boolean,
    preparingVoice: Boolean,
    voiceMessage: String?,
    modelReady: Boolean,
    onBack: () -> Unit,
    onNewConversation: () -> Unit,
    onCancelAssistant: () -> Unit,
    onEditSavedWeight: (WeightEntry) -> Unit,
    onUndoSavedWeight: (WeightEntry) -> Unit,
    onReviewMeal: (MealProposal) -> Unit,
    onToggleVoice: () -> Unit,
    onSend: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    var confirmNewConversation by remember { mutableStateOf(false) }
    val transientCount = listOfNotNull(
        assistantThinking.takeIf { it },
        mealDraftThinking.takeIf { it },
        savedWeight,
        mealProposal,
        assistantDiagnostic,
        coachDiagnostic,
    ).size
    LaunchedEffect(messages.size, streamingReply, transientCount) {
        val lastIndex = messages.size + transientCount - 1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Surface(
        color = MetriaColors.Canvas,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            AssistantHeader(onBack, onNewConversation = { confirmNewConversation = true })
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(messages) { _, message -> ChatBubble(message) }
                if (assistantThinking) {
                    item {
                        ThinkingBubble(streamingReply = streamingReply, onCancel = onCancelAssistant)
                    }
                }
                if (mealDraftThinking) {
                    item {
                        StatusCard("Preparing a meal draft", "You will review it before anything is saved.")
                    }
                }
                savedWeight?.let { entry ->
                    item {
                        SavedWeightCard(entry, onEditSavedWeight, onUndoSavedWeight)
                    }
                }
                mealProposal?.let { proposal ->
                    item {
                        MealDraftCard(proposal, onReviewMeal)
                    }
                }
                assistantDiagnostic?.let { message -> item { ErrorMessage(message) } }
                coachDiagnostic?.let { message -> item { ErrorMessage(message) } }
            }
            AssistantComposer(
                input = input,
                onInputChange = onInputChange,
                recordingVoice = recordingVoice,
                preparingVoice = preparingVoice,
                voiceMessage = voiceMessage,
                modelReady = modelReady,
                assistantBusy = assistantThinking || mealDraftThinking,
                onToggleVoice = onToggleVoice,
                onSend = onSend,
            )
        }
    }
    if (confirmNewConversation) {
        AlertDialog(
            onDismissRequest = { confirmNewConversation = false },
            title = { Text("Start a new conversation?") },
            text = { Text("This permanently removes the current private conversation from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewConversation = false
                    onNewConversation()
                }) { Text("Start new") }
            },
            dismissButton = { TextButton(onClick = { confirmNewConversation = false }) { Text("Keep conversation") } },
        )
    }
}

@Composable
private fun AssistantHeader(onBack: () -> Unit, onNewConversation: () -> Unit) {
    Surface(color = MetriaColors.Surface, shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Metria conversation")
            }
            Surface(color = MetriaColors.AccentSoft, shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Metria", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MetriaColors.Verified, modifier = Modifier.size(13.dp))
                    Text("AI assistant · private on this phone", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onNewConversation, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Start a new private conversation")
            }
        }
    }
}

@Composable
private fun ChatBubble(message: MetriaChatMessage) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (message.fromMember) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        when {
            message.localEvidence -> EvidenceCard(message.text)
            message.fromMember -> Surface(
                color = MetriaColors.Primary,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Text(message.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            else -> Surface(
                color = MetriaColors.Surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.widthIn(max = 340.dp),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    SafeMarkdownText(message.text, MetriaColors.Ink)
                }
            }
        }
    }
}

@Composable
private fun EvidenceCard(text: String) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = evidenceStateDescription(expanded) }
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MetriaColors.Verified, modifier = Modifier.size(17.dp))
                Text("From your records", color = MetriaColors.Verified, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Hide record details" else "Show record details",
                    tint = MetriaColors.InkMuted,
                )
            }
            if (expanded) SafeMarkdownText(text, MetriaColors.Ink)
        }
    }
}

internal fun evidenceStateDescription(expanded: Boolean): String =
    if (expanded) "Record details expanded" else "Record details collapsed"

@Composable
private fun ThinkingBubble(streamingReply: String?, onCancel: () -> Unit) {
    Surface(
        color = MetriaColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.widthIn(max = 340.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Metria is thinking locally"
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            streamingReply?.takeIf(String::isNotBlank)?.let { SafeMarkdownText(it, MetriaColors.Ink) } ?: TypingDots()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (streamingReply.isNullOrBlank()) "Thinking locally" else "Finishing this reply", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(18.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "metria-chat-typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "metria-chat-dots",
    )
    Text("Metria is thinking${".".repeat(phase.toInt() + 1)}", color = MetriaColors.Ink)
}

@Composable
private fun StatusCard(title: String, supporting: String) {
    Surface(color = MetriaColors.PrimarySoft, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(supporting, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SavedWeightCard(entry: WeightEntry, onEdit: (WeightEntry) -> Unit, onUndo: (WeightEntry) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.AccentSoft)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MetriaColors.Verified)
                Text("Weight saved", style = MaterialTheme.typography.titleSmall)
            }
            Text("${"%.1f".format(entry.kilograms)} kg · stored in your encrypted vault", color = MetriaColors.InkMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEdit(entry) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.padding(end = 5.dp).size(18.dp))
                    Text("Edit")
                }
                TextButton(onClick = { onUndo(entry) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.padding(end = 5.dp).size(18.dp))
                    Text("Undo")
                }
            }
        }
    }
}

@Composable
private fun MealDraftCard(proposal: MealProposal, onReview: (MealProposal) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.PrimarySoft)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal draft ready", style = MaterialTheme.typography.titleMedium)
            Text(proposal.items.joinToString { it.name }, color = MetriaColors.InkMuted)
            Text("Review portions and ingredients before saving.", color = MetriaColors.Estimated, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onReview(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review meal") }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun AssistantComposer(
    input: String,
    onInputChange: (String) -> Unit,
    recordingVoice: Boolean,
    preparingVoice: Boolean,
    voiceMessage: String?,
    modelReady: Boolean,
    assistantBusy: Boolean,
    onToggleVoice: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MetriaColors.Surface, shadowElevation = 4.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, end = 12.dp),
        ) {
            when {
                recordingVoice -> {
                    ListeningStatus()
                    Spacer(Modifier.height(7.dp))
                }
                preparingVoice -> {
                    Text("Getting the local assistant ready…", color = MetriaColors.Primary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(7.dp))
                }
                voiceMessage != null -> {
                    Text(voiceMessage, color = MetriaColors.Danger, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(7.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Message Metria") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (recordingVoice) MetriaColors.AccentSoft else MetriaColors.SurfaceSubtle,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    IconButton(onClick = onToggleVoice, enabled = modelReady, modifier = Modifier.size(56.dp)) {
                        Icon(
                            if (recordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (recordingVoice) "Stop listening" else "Speak to Metria",
                            tint = if (recordingVoice) MetriaColors.Verified else MetriaColors.Primary,
                        )
                    }
                }
                Surface(
                    color = if (input.isNotBlank() && modelReady && !assistantBusy) MetriaColors.Accent else MetriaColors.SurfaceSubtle,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = input.isNotBlank() && modelReady && !assistantBusy,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send message", tint = MetriaColors.Ink)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ListeningStatus() {
    val transition = rememberInfiniteTransition(label = "metria-listening")
    val pulse by transition.animateFloat(
        initialValue = .45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing)),
        label = "listening-pulse",
    )
    Row(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Metria is listening locally" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(12.dp)) { drawCircle(MetriaColors.Verified.copy(alpha = pulse)) }
        Text("Listening locally · stops automatically", color = MetriaColors.Verified, style = MaterialTheme.typography.bodySmall)
    }
}
