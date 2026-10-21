package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.metria.ui.components.AssistantActivityCard
import app.metria.ui.theme.MetriaColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatVoiceActivity(
    recording: Boolean,
    processing: Boolean,
    stopping: Boolean,
    ready: Boolean,
    unavailable: Boolean,
    error: String?,
    onToggleRecording: () -> Unit,
    onCancelProcessing: () -> Unit,
    onDismiss: () -> Unit,
    onTypeInstead: () -> Unit,
) {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(recording) {
        if (recording) {
            seconds = 0
            while (true) { delay(1_000); seconds += 1 }
        }
    }
    val title = when {
        recording -> "Listening…"
        stopping -> "Stopping…"
        processing -> "Transcribing…"
        unavailable -> "Voice unavailable"
        error != null -> "Try speaking again"
        !ready -> "Getting ready…"
        else -> "Ready to listen"
    }
    AssistantActivityCard(title, busy = recording || processing || stopping || (!ready && !unavailable)) {
        when {
            recording -> Text("${seconds}s · Stops after a short pause or 12 seconds.",
                color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
            error != null && !stopping -> Text(error, color = MetriaColors.Danger)
            !processing && !stopping -> Text("Your spoken words send automatically. Use the pencil to correct them in Chat.",
                color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                recording -> FilledTonalButton(onClick = onToggleRecording, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop recording") }
                processing -> FilledTonalButton(onClick = onCancelProcessing, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
                !stopping && ready && !unavailable -> FilledTonalButton(onClick = onToggleRecording,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(if (error == null) "Start recording" else "Try again") }
            }
            TextButton(onClick = onTypeInstead, modifier = Modifier.heightIn(min = 48.dp)) { Text("Type instead") }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel voice request")
            }
        }
    }
}
