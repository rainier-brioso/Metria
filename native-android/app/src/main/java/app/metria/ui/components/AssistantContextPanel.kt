package app.metria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

enum class AssistantPanelAction { ANALYSE_MEAL, SCAN_LABEL, ADD_WEIGHT, ASSISTANT_TOOLS, PRIVACY, LOCK }

internal fun assistantPanelFullHeight(expanded: Boolean, heightDp: Float, fontScale: Float, keyboardVisible: Boolean): Boolean =
    expanded || keyboardVisible || heightDp < 600f || fontScale >= 1.3f

/** In-window surface: existing review/editor surfaces can still open above Chat.
 * Only constraints change on expansion; the content never moves to another composition.
 */
@Composable
fun AssistantContextPanel(
    expanded: Boolean,
    onDismiss: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.(fullHeight: Boolean, canResize: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val dismiss by rememberUpdatedState(onDismiss)
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = .32f))
                .pointerInput(Unit) { detectTapGestures { dismiss() } },
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .imePadding()) {
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val fullHeight = assistantPanelFullHeight(expanded, maxHeight.value, density.fontScale, keyboardVisible)
            val canResize = !assistantPanelFullHeight(false, maxHeight.value, density.fontScale, keyboardVisible)
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(if (fullHeight) 1f else .78f)
                    .semantics {
                        paneTitle = "Metria assistant"
                        isTraversalGroup = true
                        stateDescription = if (fullHeight) "Expanded conversation" else "Compact conversation"
                    },
                shape = MaterialTheme.shapes.large,
                color = MetriaColors.Surface,
            ) {
                Column(Modifier.fillMaxSize()) { content(fullHeight, canResize) }
            }
        }
        if (!keyboardVisible) bottomBar()
        }
    }
}

@Composable
fun AssistantPanelHeader(
    fullHeight: Boolean,
    canResize: Boolean,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit,
    onOptions: () -> Unit,
    optionsEnabled: Boolean,
) {
    Surface(color = MetriaColors.Surface) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Metria", style = MaterialTheme.typography.titleLarge)
                Text("AI · Private on this phone", color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onToggleExpanded, enabled = canResize, modifier = Modifier.size(48.dp)) {
                Icon(if (fullHeight) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (fullHeight) "Reduce conversation" else "Expand conversation")
            }
            Box {
                IconButton(onClick = onOptions, enabled = optionsEnabled, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "Conversation options")
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Metria conversation")
            }
        }
    }
}

@Composable
fun AssistantPanelActions(
    enabled: Boolean,
    onOpen: () -> Unit,
) {
    Box {
        IconButton(onClick = onOpen, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "More assistant actions", tint = MetriaColors.Primary)
        }
    }
}

/** Inline choices share the panel instead of opening a floating popup. */
@Composable
fun AssistantPanelChoices(
    options: Boolean,
    enabled: Boolean,
    voiceEnabled: Boolean,
    newConversationEnabled: Boolean,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onChat: () -> Unit,
    onAction: (AssistantPanelAction) -> Unit,
    onNewConversation: () -> Unit,
    onSavedDishes: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.padding(end = 8.dp))
            Text("Back to chat")
        }
        Text(if (options) "Conversation" else "What would you like to do?",
            style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        if (options) {
            AssistantChoice("New conversation", "Start again after confirmation", Icons.Rounded.Refresh, newConversationEnabled, onNewConversation)
            AssistantChoice("Assistant tools", "Model setup and offline food search", Icons.Rounded.Settings, enabled) { onAction(AssistantPanelAction.ASSISTANT_TOOLS) }
            AssistantChoice("Privacy", "Your data and on-device controls", Icons.Rounded.PrivacyTip, enabled) { onAction(AssistantPanelAction.PRIVACY) }
            AssistantChoice("Lock Metria", "Protect your private records", Icons.Rounded.Lock, enabled) { onAction(AssistantPanelAction.LOCK) }
            Text("Your active conversation is kept for 24 hours, up to 50 messages.",
                style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted,
                modifier = Modifier.padding(vertical = 8.dp))
        } else {
            AssistantChoice("Speak", "Tell Metria", Icons.Rounded.Mic, enabled && voiceEnabled, onSpeak)
            AssistantChoice("Write", "Continue the conversation", Icons.AutoMirrored.Rounded.Chat, enabled, onChat)
            AssistantChoice("Analyse meal", "Prepare a draft from a photo", Icons.Rounded.CameraAlt, enabled) { onAction(AssistantPanelAction.ANALYSE_MEAL) }
            AssistantChoice("Scan nutrition label", "Review nutrition information", Icons.Rounded.DocumentScanner, enabled) { onAction(AssistantPanelAction.SCAN_LABEL) }
            AssistantChoice("Add weight", "Enter a weight manually", Icons.Rounded.Add, enabled) { onAction(AssistantPanelAction.ADD_WEIGHT) }
            AssistantChoice("My saved dishes", "Choose one to review", Icons.Rounded.Restaurant, enabled, onSavedDishes)
        }
    }
}

@Composable
private fun AssistantChoice(title: String, supporting: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = MetriaColors.Canvas, shape = MaterialTheme.shapes.small) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MetriaColors.Primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
            }
        }
    }
}
