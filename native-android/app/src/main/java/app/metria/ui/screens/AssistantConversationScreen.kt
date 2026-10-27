package app.metria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import app.metria.MealProposal
import app.metria.ModelStatus
import app.metria.PersonalDish
import app.metria.SafeMarkdownText
import app.metria.WeightEntry
import app.metria.ui.theme.MetriaColors
import app.metria.ui.components.SelectableChatText
import app.metria.ui.components.AssistantActivityCard
import app.metria.ui.components.AssistantContextPanel
import app.metria.ui.components.AssistantPanelAction
import app.metria.ui.components.AssistantPanelActions
import app.metria.ui.components.AssistantPanelHeader
import app.metria.ui.components.AssistantPanelChoices

data class MetriaChatMessage(
    val text: String,
    val fromMember: Boolean,
    val localEvidence: Boolean = false,
    val savedMealId: String? = null,
    val savedMealAvailable: Boolean = false,
    val announcePolitely: Boolean = false,
)

internal enum class ChatActivity { VOICE, STOPPING, PREPARING_MEAL, THINKING, STOPPED, NONE }

internal fun chatActivity(
    voiceActive: Boolean, stopping: Boolean, preparingMeal: Boolean, thinking: Boolean, stopped: Boolean,
): ChatActivity = when {
    voiceActive -> ChatActivity.VOICE
    stopping -> ChatActivity.STOPPING
    preparingMeal -> ChatActivity.PREPARING_MEAL
    thinking -> ChatActivity.THINKING
    stopped -> ChatActivity.STOPPED
    else -> ChatActivity.NONE
}

internal fun canUseChatMicrophone(ready: Boolean, busy: Boolean, recording: Boolean): Boolean =
    recording || (ready && !busy)

data class MetriaSavedDishChoice(
    val id: String,
    val name: String,
    val foods: List<String>,
    val exactNameMatch: Boolean = false,
    val number: Int = 1,
    val enabled: Boolean = true,
    val retryOnly: Boolean = false,
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
 * Conversation stays in one resizable, keyboard-safe contextual panel while
 * tool selection and vault mutations remain owned by the existing coordinator.
 */
@Composable
fun MetriaAssistantConversationScreen(
    messages: List<MetriaChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    assistantThinking: Boolean,
    assistantStopping: Boolean,
    responseStopped: Boolean,
    mealDraftThinking: Boolean,
    streamingReply: String?,
    assistantDiagnostic: String?,
    coachDiagnostic: String?,
    savedWeight: WeightEntry?,
    mealProposal: MealProposal?,
    portionQuestion: String? = null,
    portionFeedback: String? = null,
    savedDishChoices: List<MetriaSavedDishChoice> = emptyList(),
    savedDishes: List<PersonalDish> = emptyList(),
    recordingVoice: Boolean,
    preparingVoice: Boolean,
    voiceMessage: String?,
    modelReady: Boolean,
    selectedMealContextTitle: String? = null,
    onClearSelectedMealContext: () -> Unit = {},
    onBack: () -> Unit,
    onNewConversation: suspend () -> Boolean,
    onCancelAssistant: () -> Unit,
    onEditSavedWeight: (WeightEntry) -> Unit,
    onUndoSavedWeight: (WeightEntry) -> Unit,
    onReviewMeal: (MealProposal) -> Unit,
    onOpenSavedMeal: (String) -> Unit,
    onReviewSavedDish: (String) -> Unit = {},
    onSelectSavedDish: (String) -> Unit = {},
    onDiscardMealDraft: () -> Unit,
    onToggleVoice: () -> Unit,
    onEditLatestMemberMessage: (String) -> Unit,
    onSend: () -> Unit,
    voiceActivity: (@Composable () -> Unit)? = null,
    composerFocusRequest: Long = 0L,
    onPanelAction: (AssistantPanelAction) -> Unit,
    panelActionInFlight: Boolean = false,
    introductoryConversation: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
) {
    var panelView by rememberSaveable { mutableStateOf("chat") }
    BackHandler { if (panelActionInFlight) Unit else if (panelView != "chat") panelView = "chat" else onBack() }
    val listState = rememberLazyListState()
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var focusComposerAfterEdit by remember { mutableStateOf(false) }
    var editingMessageAsNewTurn by rememberSaveable { mutableStateOf(false) }
    var confirmNewConversation by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activity = chatActivity(voiceActivity != null, assistantStopping, mealDraftThinking, assistantThinking, responseStopped)
    val transientCount = listOfNotNull(
        activity.takeUnless { it == ChatActivity.NONE },
        savedWeight,
        mealProposal,
        savedDishChoices.takeIf { it.isNotEmpty() },
        assistantDiagnostic,
        coachDiagnostic,
    ).size
    val showIntroduction = (messages.isEmpty() || introductoryConversation) && transientCount == 0
    val visibleMessages = if (showIntroduction) emptyList() else messages
    LaunchedEffect(messages.size, streamingReply, transientCount, activity, assistantDiagnostic, coachDiagnostic, selectedMealContextTitle) {
        val lastIndex = visibleMessages.size + transientCount +
            (if (selectedMealContextTitle != null) 1 else 0) + (if (showIntroduction) 1 else 0) - 1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }
    LaunchedEffect(focusComposerAfterEdit) {
        if (focusComposerAfterEdit) {
            androidx.compose.runtime.withFrameNanos { }
            composerFocusRequester.requestFocus()
            keyboardController?.show()
            focusComposerAfterEdit = false
        }
    }
    LaunchedEffect(composerFocusRequest) {
        if (composerFocusRequest > 0L) {
            androidx.compose.runtime.withFrameNanos { }
            composerFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val latestMemberMessageIndex = messages.indexOfLast { it.fromMember && !it.localEvidence }
    val canEditMessage = !assistantThinking && !assistantStopping && !mealDraftThinking &&
        !recordingVoice && !preparingVoice && voiceActivity == null && !panelActionInFlight

    AssistantContextPanel(expanded = expanded, onDismiss = { if (!panelActionInFlight) onBack() }, bottomBar = bottomBar) { fullHeight, canResize ->
            AssistantPanelHeader(
                fullHeight = fullHeight,
                canResize = canResize,
                onToggleExpanded = { expanded = !expanded },
                onClose = { if (!panelActionInFlight) onBack() },
                onOptions = { keyboardController?.hide(); panelView = if (panelView == "options") "chat" else "options" },
                optionsEnabled = canEditMessage,
            )
            if (panelView != "chat") {
                if (panelView == "dishes") {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        androidx.compose.material3.TextButton(
                            onClick = { if (!panelActionInFlight) panelView = "chat" },
                            enabled = !panelActionInFlight,
                            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 20.dp),
                        ) {
                            androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Back to chat")
                        }
                        AssistantSavedDishesContent(
                            dishes = savedDishes,
                            enabled = canEditMessage,
                            busy = panelActionInFlight,
                            diagnostic = assistantDiagnostic ?: coachDiagnostic,
                            onSelectDish = onSelectSavedDish,
                        )
                    }
                } else AssistantPanelChoices(
                    options = panelView == "options", enabled = canEditMessage,
                    voiceEnabled = modelReady, newConversationEnabled = !panelActionInFlight,
                    onBack = { panelView = "chat" },
                    onSpeak = { panelView = "chat"; onToggleVoice() },
                    onChat = { panelView = "chat"; focusComposerAfterEdit = true },
                    onAction = onPanelAction,
                    onNewConversation = { confirmNewConversation = true },
                    onSavedDishes = { keyboardController?.hide(); panelView = "dishes" },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                selectedMealContextTitle?.let { title ->
                    item(key = "selected_meal_context") {
                        SelectedMealContext(title, onClearSelectedMealContext)
                    }
                }
                if (showIntroduction) {
                    item(key = "assistant_introduction") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (selectedMealContextTitle == null) "How can I help?" else "What would you like to know?",
                                style = MaterialTheme.typography.headlineSmall)
                            Text("Ask about your records or prepare a new entry.", color = MetriaColors.InkMuted)
                            OutlinedButton(onClick = {
                                onInputChange(if (selectedMealContextTitle == null) "What meals have I recorded today?"
                                    else "What portion is recorded for this meal?")
                                focusComposerAfterEdit = true
                            }, enabled = !panelActionInFlight, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (selectedMealContextTitle == null) "Ask about my meals" else "Ask about this portion")
                            }
                        }
                    }
                }
                itemsIndexed(visibleMessages) { index, message ->
                    ChatBubble(
                        message = message,
                        onOpenSavedMeal = onOpenSavedMeal,
                        showEdit = canEditMessage && index == latestMemberMessageIndex,
                        onEdit = {
                            editingMessageAsNewTurn = true
                            onEditLatestMemberMessage(message.text)
                            focusComposerAfterEdit = true
                        },
                    )
                }
                savedWeight?.let { entry ->
                    item {
                        SavedWeightCard(entry, onEditSavedWeight, onUndoSavedWeight)
                    }
                }
                mealProposal?.let { proposal ->
                    item {
                        MealDraftCard(proposal, portionQuestion, portionFeedback, onReviewMeal, onDiscardMealDraft)
                    }
                }
                if (savedDishChoices.isNotEmpty()) {
                    item {
                        SavedDishChoicesCard(savedDishChoices, onReviewSavedDish)
                    }
                }
                assistantDiagnostic?.let { message -> item { ErrorMessage(message, allowCopy = true) } }
                coachDiagnostic?.let { message -> item { ErrorMessage(message, allowCopy = true) } }
                if (activity != ChatActivity.NONE) {
                    item(key = "metria_activity") {
                        when (activity) {
                            ChatActivity.VOICE -> voiceActivity?.invoke()
                            ChatActivity.STOPPING -> StoppingBubble()
                            ChatActivity.STOPPED -> StatusCard("Response stopped", "You can send another message.")
                            ChatActivity.PREPARING_MEAL -> ThinkingBubble(null, onCancelAssistant, preparingMeal = true)
                            ChatActivity.THINKING -> ThinkingBubble(streamingReply, onCancelAssistant)
                            ChatActivity.NONE -> Unit
                        }
                    }
                }
            }
            }
            if (panelView == "chat") AssistantComposer(
                input = input,
                recordingVoice = recordingVoice && voiceActivity == null,
                preparingVoice = preparingVoice && voiceActivity == null,
                voiceMessage = voiceMessage.takeIf { voiceActivity == null },
                modelReady = modelReady && voiceActivity == null,
                assistantBusy = assistantThinking || assistantStopping || mealDraftThinking || voiceActivity != null || panelActionInFlight,
                focusRequester = composerFocusRequester,
                editingMessageAsNewTurn = editingMessageAsNewTurn,
                inputEnabled = voiceActivity == null && !panelActionInFlight,
                panelActionsEnabled = canEditMessage,
                onOpenActions = { keyboardController?.hide(); panelView = if (panelView == "actions") "chat" else "actions" },
                onToggleVoice = onToggleVoice,
                onInputChange = {
                    onInputChange(it)
                    editingMessageAsNewTurn = false
                },
                onSend = {
                    onSend()
                    editingMessageAsNewTurn = false
                },
            )
    }
    if (confirmNewConversation) {
        AlertDialog(
            onDismissRequest = { confirmNewConversation = false },
            title = { Text("Start a new conversation?") },
            text = { Text("This permanently removes the current private conversation from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (onNewConversation()) { confirmNewConversation = false; panelView = "chat" }
                    }
                }) { Text("Start new") }
            },
            dismissButton = { TextButton(onClick = { confirmNewConversation = false }) { Text("Keep conversation") } },
        )
    }
}

@Composable
private fun SavedDishChoicesCard(choices: List<MetriaSavedDishChoice>, onReview: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Which saved dish did you mean?", style = MaterialTheme.typography.titleSmall)
            Text(if (choices.all(MetriaSavedDishChoice::exactNameMatch)) {
                "Review the saved foods and portions before adding. Nothing is saved yet."
            } else {
                "These are possible matches. Choose one to review. Nothing is saved yet."
            },
                color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            Text(if (choices.any(MetriaSavedDishChoice::retryOnly)) {
                "The chat could not be saved. Tap Review meal to retry."
            } else if (choices.size == 1) {
                "You can also reply “the first one” or type the full dish name."
            } else {
                "You can reply with its number, like “the second one”, or type a full dish name."
            }, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            choices.forEach { choice ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${choice.number}. ${choice.name}", style = MaterialTheme.typography.bodyLarge)
                    Text(if (choice.exactNameMatch) "Exact name match" else "Possible match",
                        color = MetriaColors.InkMuted, style = MaterialTheme.typography.labelSmall)
                    if (choice.foods.isNotEmpty()) Text(choice.foods.joinToString(", "),
                        color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                    FilledTonalButton(
                        onClick = { onReview(choice.id) }, enabled = choice.enabled,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Review meal")
                    }
                }
            }
        }
    }
}

@Composable
private fun StoppingBubble() {
    AssistantActivityCard("Stopping…", busy = true) { }
}

@Composable
private fun SelectedMealContext(title: String, onClear: () -> Unit) {
    Surface(color = MetriaColors.PrimarySoft, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text("One question about this saved meal", style = MaterialTheme.typography.labelLarge)
                Text(title, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove selected meal context", tint = MetriaColors.InkMuted)
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: MetriaChatMessage,
    onOpenSavedMeal: (String) -> Unit,
    showEdit: Boolean = false,
    onEdit: () -> Unit = {},
) {
    var evidenceExpanded by rememberSaveable(message.text) { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (message.fromMember) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        when {
            message.localEvidence -> Column(Modifier.fillMaxWidth()) {
                EvidenceCard(message.text, evidenceExpanded, onExpandedChange = { evidenceExpanded = it })
            }
            message.fromMember -> Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = MetriaColors.PrimarySoft,
                    contentColor = MetriaColors.Ink,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.widthIn(max = 320.dp),
                ) {
                    SelectableChatText {
                        Text(message.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                }
                if (showEdit) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit as new message", tint = MetriaColors.InkMuted)
                    }
                }
            }
            else -> Column(horizontalAlignment = Alignment.Start) {
                Surface(
                    color = MetriaColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.widthIn(max = 340.dp).then(
                        if (message.announcePolitely) {
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        } else Modifier
                    ),
                ) {
                    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectableChatText { SafeMarkdownText(message.text, MetriaColors.Ink) }
                        message.savedMealId?.let { id ->
                            if (message.savedMealAvailable) {
                                FilledTonalButton(onClick = { onOpenSavedMeal(id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text("View meal")
                                }
                            } else {
                                Text("Meal no longer available", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceCard(text: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = evidenceStateDescription(expanded) }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onExpandedChange(!expanded) },
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
            if (expanded) SelectableChatText { SafeMarkdownText(text, MetriaColors.Ink) }
        }
    }
}

internal fun evidenceStateDescription(expanded: Boolean): String =
    if (expanded) "Record details expanded" else "Record details collapsed"

@Composable
private fun ThinkingBubble(streamingReply: String?, onCancel: () -> Unit, preparingMeal: Boolean = false) {
    AssistantActivityCard(
        title = if (preparingMeal) "Preparing a meal draft…"
            else if (streamingReply.isNullOrBlank()) "Thinking…" else "Finishing your reply…",
        busy = true,
    ) {
        if (preparingMeal) {
            Text("You will review it before anything is saved.", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            streamingReply?.takeIf(String::isNotBlank)?.let { SafeMarkdownText(it, MetriaColors.Ink) }
        }
        FilledTonalButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(18.dp))
            Text("Stop")
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
private fun MealDraftCard(
    proposal: MealProposal,
    portionQuestion: String?,
    portionFeedback: String?,
    onReview: (MealProposal) -> Unit,
    onDiscard: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface), border = BorderStroke(1.dp, MetriaColors.Divider)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Edit, null, tint = MetriaColors.Primary, modifier = Modifier.size(18.dp))
                Text("Draft · Not saved", color = MetriaColors.Primary, style = MaterialTheme.typography.labelMedium)
            }
            Text("Meal draft ready", style = MaterialTheme.typography.titleMedium)
            Text(proposal.items.joinToString { it.name }, color = MetriaColors.InkMuted)
            Text("Review portions and ingredients before saving.", color = MetriaColors.Estimated, style = MaterialTheme.typography.bodySmall)
            if (portionQuestion != null) {
                Text(portionQuestion, style = MaterialTheme.typography.bodyMedium)
                Text("Reply with a whole gram amount, such as 120 g, or say “skip”. You can also review without answering.",
                    color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            portionFeedback?.let { Text(it, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { onReview(proposal) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review meal") }
            MealDraftDiscardAction(onDiscard = onDiscard)
        }
    }
}

@Composable
private fun ErrorMessage(message: String, allowCopy: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            if (allowCopy) SelectableChatText { Text(message, color = MaterialTheme.colorScheme.onErrorContainer) }
            else Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
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
    focusRequester: FocusRequester,
    editingMessageAsNewTurn: Boolean,
    inputEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onSend: () -> Unit,
    panelActionsEnabled: Boolean,
    onOpenActions: () -> Unit,
) {
    Surface(color = MetriaColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (editingMessageAsNewTurn) {
                Text(
                    "Ready to send as a new message.",
                    color = MetriaColors.InkMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Edited text copied to the message box. Send to start a new message."
                    },
                )
                Spacer(Modifier.height(7.dp))
            }
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
            Text("Write or use your voice", style = MaterialTheme.typography.bodySmall,
                color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 8.dp))
            Surface(color = MetriaColors.Canvas, shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MetriaColors.Divider)) {
            Column(Modifier.padding(8.dp)) {
            TextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Tell me…") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 4,
                    enabled = inputEnabled,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = "Message Metria" },
                )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistantPanelActions(
                    enabled = panelActionsEnabled,
                    onOpen = onOpenActions,
                )
                    TextButton(onClick = onToggleVoice,
                        enabled = canUseChatMicrophone(modelReady, assistantBusy, recordingVoice),
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(
                            if (recordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = if (recordingVoice) MetriaColors.Verified else MetriaColors.Primary,
                        )
                        Text(if (recordingVoice) "Stop" else "Speak", modifier = Modifier.padding(start = 4.dp))
                    }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MetriaColors.Primary,
                    shape = MaterialTheme.shapes.small,
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = input.isNotBlank() && modelReady && !assistantBusy,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Send message", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            }
            }
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
