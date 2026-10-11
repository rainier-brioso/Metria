package app.metria.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.metria.BuildConfig
import app.metria.MealEntryAudioCapture
import app.metria.MealEntryResultPolicy
import app.metria.MealProposal
import app.metria.ModelStatus
import app.metria.VoiceEntryText
import kotlinx.coroutines.delay

/** Dedicated Today entry. Completed speech creates a draft; only review can save it. */
@Composable
fun MealEntryScreen(
    modifier: Modifier = Modifier,
    slotLabel: String,
    lifecycleOwner: LifecycleOwner,
    voiceInitially: Boolean,
    modelStatus: ModelStatus,
    transcribeAudio: (java.io.File) -> ModelStatus,
    createDraft: (String) -> ModelStatus,
    prepareAssistant: () -> Unit,
    cancelAssistant: () -> Unit,
    onReview: (MealProposal) -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var voiceMode by remember { mutableStateOf(voiceInitially) }
    var description by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var awaitingAutoStop by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var entryError by remember { mutableStateOf<String?>(null) }
    var voiceRequestId by remember { mutableStateOf<Long?>(null) }
    var draftRequestId by remember { mutableStateOf<Long?>(null) }
    var draftSubmitting by remember { mutableStateOf(false) }
    var draftFailureDetails by remember { mutableStateOf<MealEntryResultPolicy.DraftDebugDetails?>(null) }
    var showDraftDetails by remember { mutableStateOf(false) }
    var recordingToken by remember { mutableStateOf<Long?>(null) }
    val audioCapture = remember(context) { MealEntryAudioCapture(context) }
    val latestStatus = rememberUpdatedState(modelStatus)
    val transcribeCurrent = rememberUpdatedState(transcribeAudio)
    val createDraftCurrent = rememberUpdatedState(createDraft)
    val prepareCurrent = rememberUpdatedState(prepareAssistant)
    val cancelCurrent = rememberUpdatedState(cancelAssistant)
    val dismissCurrent = rememberUpdatedState(onDismiss)
    val reviewCurrent = rememberUpdatedState(onReview)
    val activeOperation = rememberUpdatedState(voiceRequestId != null || draftRequestId != null)

    fun submitDraft(text: String) {
        if (draftSubmitting || draftRequestId != null) return
        val checked = VoiceEntryText.validated(text) ?: return
        draftSubmitting = true
        entryError = null
        draftFailureDetails = null
        showDraftDetails = false
        val before = latestStatus.value
        val result = runCatching { createDraftCurrent.value(checked) }.getOrElse {
            draftSubmitting = false
            entryError = "The meal draft could not start. Check your description and try again."
            return
        }
        val started = MealEntryResultPolicy.startedAfter(before, result, "meal")
        if (started != null) draftRequestId = started
        else {
            draftSubmitting = false
            entryError = result.assistantDiagnostic ?: "Metria is busy. Wait until it is ready, then try again."
        }
    }

    fun transcribe(clip: app.metria.GemmaAudioRecorder.Clip) {
        if (!clip.detectedSpeech) {
            clip.file.delete()
            voiceError = "No speech was heard. Try again or type your meal."
            return
        }
        val before = latestStatus.value
        val result = runCatching { transcribeCurrent.value(clip.file) }.getOrElse {
            clip.file.delete()
            voiceError = "Voice recognition could not start. Try again or type your meal."
            return
        }
        val started = MealEntryResultPolicy.startedAfter(before, result, "coach_audio")
        if (started != null) voiceRequestId = started else {
            clip.file.delete()
            voiceError = result.coachDiagnostic ?: "Voice recognition could not start. Try again or type your meal."
        }
    }

    fun startRecording(token: Long) {
        if (!audioCapture.isActive(token)) return
        recording = true
        audioCapture.start(token) { clip ->
            awaitingAutoStop = false
            recording = false
            recordingToken = null
            transcribe(clip)
        }.onSuccess {
            entryError = null
        }.onFailure {
            awaitingAutoStop = false
            recording = false
            recordingToken = null
            voiceError = "Recording could not start. Try again or type your meal."
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val token = recordingToken
        if (token == null || !audioCapture.isActive(token)) return@rememberLauncherForActivityResult
        if (granted) startRecording(token) else {
            audioCapture.cancel()
            awaitingAutoStop = false
            recordingToken = null
            voiceError = "Microphone permission was not granted. You can type your meal instead."
        }
    }

    fun requestRecording() {
        voiceError = null
        awaitingAutoStop = false
        val token = audioCapture.beginAttempt()
        recordingToken = token
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording(token)
        } else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    BackHandler(onBack = { dismissCurrent.value() })
    DisposableEffect(lifecycleOwner, audioCapture) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val hadActiveOperation = activeOperation.value
                audioCapture.cancel()
                awaitingAutoStop = false
                recordingToken = null
                recording = false
                voiceRequestId = null
                draftRequestId = null
                draftSubmitting = false
                draftFailureDetails = null
                showDraftDetails = false
                description = ""
                voiceError = null
                entryError = null
                if (hadActiveOperation) cancelCurrent.value()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            audioCapture.cancel()
            if (activeOperation.value) cancelCurrent.value()
        }
    }
    LaunchedEffect(recording, awaitingAutoStop) {
        if (recording && !awaitingAutoStop) {
            elapsedSeconds = 0
            while (recording && !awaitingAutoStop) {
                delay(1_000)
                elapsedSeconds += 1
            }
        }
    }
    LaunchedEffect(modelStatus.assistantRequestId, modelStatus.state, modelStatus.coachToolCall) {
        val voiceId = voiceRequestId
        if (MealEntryResultPolicy.isCompletedResult(voiceId, modelStatus, "coach_audio")) {
            voiceRequestId = null
            val recognized = MealEntryResultPolicy.transcriptForDraft(voiceId, modelStatus)
            if (recognized == null) {
                voiceError = modelStatus.coachDiagnostic ?: "Metria could not recognize those words. Try again or type your meal."
            } else {
                description = recognized
                voiceError = null
                voiceMode = false
                draftFailureDetails = null
                showDraftDetails = false
                submitDraft(recognized)
            }
        }

        val draftId = draftRequestId
        if (MealEntryResultPolicy.isCompletedResult(draftId, modelStatus, "meal")) {
            draftRequestId = null
            draftSubmitting = false
            val proposal = MealEntryResultPolicy.proposal(modelStatus)
            if (proposal != null) reviewCurrent.value(proposal)
            else {
                entryError = "Couldn't prepare a meal draft. Try again or enter it manually."
                draftFailureDetails = if (BuildConfig.DEBUG) MealEntryResultPolicy.draftDebugDetails(draftId, modelStatus) else null
            }
        }
    }

    val voiceBusy = voiceRequestId != null && modelStatus.assistantRequestId == voiceRequestId && modelStatus.state == "thinking"
    val draftBusy = draftRequestId != null && modelStatus.assistantRequestId == draftRequestId && modelStatus.state == "thinking"
    val preparing = modelStatus.state == "ready" || modelStatus.state == "initializing" || modelStatus.state == "testing"
    val unavailable = modelStatus.state in setOf("failed", "absent", "removed")
    val canCreateDraft = modelStatus.state == "loaded" && VoiceEntryText.validated(description) != null &&
        !recording && !voiceBusy && !draftBusy && !draftSubmitting
    val currentDraftFailure = draftFailureDetails?.takeIf {
        BuildConfig.DEBUG && MealEntryResultPolicy.draftDebugDetails(it.requestId, modelStatus) == it
    }

    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dismissCurrent.value() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Today")
            }
            Text("Add $slotLabel", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        when {
            draftBusy || draftSubmitting -> LoadingMealEntry("Preparing meal draft…") {
                cancelCurrent.value()
                draftRequestId = null
                draftSubmitting = false
                entryError = "Draft stopped. Your description is still here."
            }
            voiceBusy -> LoadingMealEntry("Recognizing speech…") {
                cancelCurrent.value()
                voiceRequestId = null
                voiceError = "Recognition stopped. You can type your meal."
            }
            voiceMode -> {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("What did you eat?", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        when {
                            awaitingAutoStop -> "Finishing recording…"
                            recording -> "Listening · ${elapsedSeconds}s"
                            else -> "Speak a short description."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            val token = recordingToken
                            if (recording && token != null) {
                                when (val stopped = audioCapture.stop(token)) {
                                    is app.metria.MealEntryStopResult.Captured -> {
                                        recording = false
                                        recordingToken = null
                                        awaitingAutoStop = false
                                        transcribe(stopped.clip)
                                    }
                                    app.metria.MealEntryStopResult.AwaitingAutoStop -> awaitingAutoStop = true
                                    app.metria.MealEntryStopResult.Cancelled -> {
                                        recording = false
                                        recordingToken = null
                                        awaitingAutoStop = false
                                    }
                                }
                            } else requestRecording()
                        },
                        modifier = Modifier.size(112.dp).semantics {
                            contentDescription = if (awaitingAutoStop) "Finishing recording" else if (recording) "Stop recording" else "Start recording"
                            stateDescription = if (awaitingAutoStop) "Finishing recording" else if (recording) "Listening" else "Idle"
                        },
                        shape = CircleShape,
                        enabled = !preparing && !unavailable && !awaitingAutoStop,
                    ) {
                        Icon(
                            if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Text(if (awaitingAutoStop) "Finishing recording…" else if (recording) "Stop recording" else "Start recording", style = MaterialTheme.typography.titleMedium)
                    if (preparing) Text("Getting Metria ready…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (unavailable) Text("Voice is unavailable. You can type your meal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    voiceError?.let { EntryMessage(it, isError = true) }
                    TextButton(onClick = {
                        audioCapture.cancel()
                        awaitingAutoStop = false
                        recordingToken = null
                        recording = false
                        voiceError = null
                        voiceMode = false
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Type instead") }
                    TextButton(onClick = onManualEntry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Enter manually") }
                }
            }
            else -> {
                Text("Describe the foods and portions you remember.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= VoiceEntryText.MAX_LENGTH) { description = it; entryError = null; draftFailureDetails = null; showDraftDetails = false } },
                    label = { Text("Meal description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                )
                if (preparing) Text("Getting Metria ready…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (unavailable) EntryMessage("The local assistant is unavailable. Try again when it is ready.", isError = true)
                entryError?.let { EntryMessage(it, isError = true) }
                if (currentDraftFailure != null) TextButton(
                    onClick = { showDraftDetails = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Draft details") }
                Button(onClick = {
                    submitDraft(description)
                }, enabled = canCreateDraft, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Create meal draft")
                }
                OutlinedButton(onClick = { voiceMode = true; voiceError = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Speak instead")
                }
                TextButton(onClick = onManualEntry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Enter manually") }
                if (modelStatus.state in setOf("ready", "initializing")) TextButton(onClick = { prepareCurrent.value() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Prepare Metria")
                }
            }
        }
    }
    if (showDraftDetails && currentDraftFailure != null) AlertDialog(
        onDismissRequest = { showDraftDetails = false },
        title = { Text("Draft details") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Validation category: ${currentDraftFailure.category}", style = MaterialTheme.typography.titleSmall)
                Text("Model response", style = MaterialTheme.typography.titleSmall)
                Text(currentDraftFailure.response.ifBlank { "(The model returned no text.)" })
            }
        },
        confirmButton = { TextButton(onClick = { showDraftDetails = false }) { Text("Close") } },
    )
}

@Composable
private fun LoadingMealEntry(label: String, onStop: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(Modifier.semantics { stateDescription = label; contentDescription = label })
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
    }
}

@Composable
private fun EntryMessage(message: String, isError: Boolean) {
    Text(
        message,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite },
    )
}
