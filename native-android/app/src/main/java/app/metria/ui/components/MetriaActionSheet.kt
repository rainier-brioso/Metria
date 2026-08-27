package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetriaActionSheet(
    onDismiss: () -> Unit,
    onSpeak: () -> Unit,
    onChat: () -> Unit,
    onAnalyseMeal: () -> Unit,
    onScanLabel: () -> Unit,
    onAddWeight: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MetriaColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            Text("How can Metria help?", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp))
            ActionRow("Speak", "Ask or record something hands-free", Icons.Rounded.Mic, onSpeak)
            ActionRow("Chat", "Ask about your private records", Icons.Rounded.Chat, onChat)
            HorizontalDivider(color = MetriaColors.Divider, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            ActionRow("Analyse meal", "Take or choose a meal photo", Icons.Rounded.CameraAlt, onAnalyseMeal)
            ActionRow("Scan nutrition label", "Read a package label locally", Icons.Rounded.DocumentScanner, onScanLabel)
            ActionRow("Add weight", "Open the simple manual entry", Icons.Rounded.Add, onAddWeight)
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    supporting: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(supporting, color = MetriaColors.InkMuted) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MetriaColors.Primary) },
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    )
}
