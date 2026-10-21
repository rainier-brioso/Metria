package app.metria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.VoiceEntryText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntrySheet(
    mealEntry: Boolean, recording: Boolean, processing: Boolean, ready: Boolean, unavailable: Boolean,
    automaticChatSubmission: Boolean = false,
    inlineChat: Boolean = false,
    stopping: Boolean = false,
    text: String, error: String?, onTextChange: (String) -> Unit,
    onToggleRecording: () -> Unit, onCancelProcessing: () -> Unit,
    onDismiss: () -> Unit, onTypeInstead: () -> Unit, onContinue: () -> Unit,
) {
    if (inlineChat && automaticChatSubmission) {
        ChatVoiceActivity(recording, processing, stopping, ready, unavailable, error,
            onToggleRecording, onCancelProcessing, onDismiss, onTypeInstead)
        return
    }
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(recording) {
        if (recording) {
            seconds = 0
            while (true) { delay(1_000); seconds += 1 }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (mealEntry) "Describe your meal" else "Speak to Metria", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (automaticChatSubmission && !mealEntry) {
                    "Recognized words are sent as a Chat message automatically. Use Edit in Chat to correct the words."
                } else {
                    "Your voice is processed on this phone. Check the recognized words before continuing."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                recording -> {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Listening locally", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        })
                        Text(" · ${seconds}s", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Stops after a short pause or 12 seconds.")
                    Button(onClick = onToggleRecording, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop recording") }
                }
                processing -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Transcribing your request…", modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Transcribing your request"
                    })
                    OutlinedButton(onClick = onCancelProcessing, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop") }
                }
                unavailable -> Text("Voice is unavailable. Type instead or return to meal entry.")
                !ready -> Text("Getting the local assistant ready…")
                else -> OutlinedButton(onClick = onToggleRecording, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (text.isBlank()) "Start recording" else "Record again") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!recording && !processing && !automaticChatSubmission) {
                OutlinedTextField(value = text, onValueChange = { if (it.length <= VoiceEntryText.MAX_LENGTH) onTextChange(it) }, label = { Text("Recognized words · edit if needed") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Button(onClick = onContinue, enabled = ready && VoiceEntryText.validated(text) != null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (mealEntry) "Create meal draft" else "Continue in Chat") }
            }
            TextButton(onClick = onTypeInstead, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Type instead") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
        }
    }
}
