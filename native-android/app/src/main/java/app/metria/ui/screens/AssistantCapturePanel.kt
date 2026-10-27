package app.metria.ui.screens

import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.metria.*
import app.metria.ui.components.*
import app.metria.ui.theme.MetriaColors
import kotlinx.coroutines.delay

/** Capture is a panel page. OS launchers and the existing local pipeline own the image. */
@Composable
fun AssistantCapturePanel(
    initialPurpose: String,
    status: ModelStatus,
    bottomBar: @Composable (requestExit: (() -> Unit) -> Unit) -> Unit,
    onBackToChat: () -> Unit,
    onClose: () -> Unit,
    beginImageFlow: () -> ModelStatus,
    newCameraUri: () -> Uri,
    beginExternalImageFlow: () -> Unit,
    prepareImage: (Uri) -> Unit,
    prepareCapturedImage: (Uri) -> Unit,
    discardPendingCapture: () -> Unit,
    discardImage: () -> Unit,
    analyzeImage: (String) -> ModelStatus,
    readLabel: () -> Unit,
    cancelAnalysis: () -> Unit,
    finishCooldown: () -> Unit,
    onReviewMeal: (MealProposal) -> Unit,
    onSaveLabel: suspend (MealEntry) -> String?,
    initialRecordedDate: java.time.LocalDate? = null,
    initialMealSlot: String? = null,
    onManualMeal: () -> Unit,
    onOpenTools: () -> Unit,
) {
    var purpose by rememberSaveable { mutableStateOf(initialPurpose) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    var capturedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var requestId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingLabel by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    var labelSaveInFlight by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }
    val labelEditorState = rememberSaveableStateHolder()
    val label = purpose == "label"
    val busy = status.imageState in setOf("preparing", "analyzing", "reading_label")
    val proposal = AssistantCaptureResultPolicy.meal(requestId, status)
    val labelReady = label && attempted && status.imageState == "none" && !status.ocrText.isNullOrBlank()
    val reset = {
        requestId = null
        attempted = false
        editingLabel = false
        labelEditorState.removeState("label-review")
        showDetails = false
        description = ""
        error = null
        if (status.imageState == "analyzing") cancelAnalysis()
        discardImage() // Also invalidates preparation and OCR completion generations.
        discardPendingCapture()
        capturedUri = null
    }
    val requestExit: (() -> Unit) -> Unit = { action ->
        if (labelSaveInFlight) Unit
        else if (busy || status.imageState == "ready" || attempted || capturedUri != null) pendingExit = action
        else { reset(); action() }
    }
    BackHandler { if (labelSaveInFlight) Unit else if (editingLabel) editingLabel = false else requestExit(onBackToChat) }
    LaunchedEffect(status.imageState, status.imageRetryAtElapsedRealtime) {
        val retryAt = status.imageRetryAtElapsedRealtime
        if (status.imageState == "cooling_down" && retryAt != null) {
            while (true) {
                val remaining = (retryAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                secondsLeft = ((remaining + 999L) / 1000L).toInt()
                if (remaining == 0L) { finishCooldown(); break }
                delay(250)
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) discardPendingCapture() else prepareImage(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = capturedUri?.let(Uri::parse)
        capturedUri = null
        if (success && uri != null) prepareCapturedImage(uri) else discardPendingCapture()
    }
    val choose: (Boolean) -> Unit = { takePhoto ->
        if (!busy && beginImageFlow().imageState != "cooling_down") {
            reset()
            try {
                if (takePhoto) {
                    val uri = newCameraUri()
                    capturedUri = uri.toString()
                    camera.launch(uri)
                } else {
                    beginExternalImageFlow()
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            } catch (_: Exception) {
                discardPendingCapture()
                capturedUri = null
                error = "Could not open photos. Try again or choose another source."
            }
        }
    }
    AssistantContextPanel(expanded, { requestExit(onClose) }, bottomBar = { bottomBar(requestExit) }) { full, resize ->
        AssistantPanelHeader(full, resize, { expanded = !expanded }, { requestExit(onClose) }, {}, false)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { if (labelSaveInFlight) Unit else if (editingLabel) editingLabel = false else requestExit(onBackToChat) },
                enabled = !labelSaveInFlight,
                modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text(if (editingLabel) "Back to draft" else "Back to chat")
            }
            Text(if (editingLabel) "Review nutrition label" else if (label) "Scan nutrition label" else "Analyse meal",
                style = MaterialTheme.typography.headlineMedium)
            when {
                editingLabel && labelReady -> labelEditorState.SaveableStateProvider("label-review") {
                    AssistantLabelReviewContent(
                        draft = status.labelDraft ?: LabelNutritionDraft(null, null, null, null),
                        thumbnail = status.referenceThumbnailBase64,
                        onSave = onSaveLabel,
                        onSavingChanged = { labelSaveInFlight = it },
                        initialDate = initialRecordedDate ?: java.time.LocalDate.now(),
                        initialMealSlot = initialMealSlot,
                    )
                }
                busy -> {
                    AssistantCaptureLoadingState(status.imageState, onStop = reset)
                }
                status.imageState == "cooling_down" -> {
                    Text("Photo tools are cooling down. Try again in ${secondsLeft}s.")
                    Text("You can return to chat while your phone cools.", color = MetriaColors.InkMuted)
                }
                proposal != null -> AssistantMealDraftCaptureCard(
                    proposal = proposal,
                    onReview = { onReviewMeal(proposal) },
                    onDiscard = { pendingExit = {} },
                )
                labelReady -> CaptureDraftCard {
                    Text("Your label draft", style = MaterialTheme.typography.titleLarge)
                    Text(if (status.labelDraft == null) "The text was read, but nutrition values need to be entered from the package."
                        else "Check the values against the package. Text recognition can make mistakes.")
                    Text("Nothing has been saved.", color = MetriaColors.InkMuted)
                    TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (showDetails) "Hide detected text" else "Show detected text")
                    }
                    if (showDetails) Text(status.ocrText.orEmpty())
                    Button(onClick = { editingLabel = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review label") }
                    TextButton(onClick = { pendingExit = {} }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard draft") }
                }
                else -> {
                    Text(if (label) "Include the serving size and the full nutrition panel. Use good light and avoid glare."
                        else "Keep the whole plate in frame. You can add details and check every estimate before saving.", color = MetriaColors.InkMuted)
                    Surface(color = MetriaColors.AccentSoft, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (status.imageState == "ready") MetriaMealThumbnail(
                                encoded = status.referenceThumbnailBase64, size = 192.dp, contentScale = ContentScale.Fit)
                            else Icon(if (label) Icons.Rounded.DocumentScanner else Icons.Rounded.CameraAlt,
                                contentDescription = null, tint = MetriaColors.Verified, modifier = Modifier.size(48.dp))
                            Text(if (status.imageState == "ready") "Your private photo" else "Take a photo or choose an image")
                        }
                    }
                    if (attempted || status.imageState == "failed") {
                        AssistantCaptureFailureState(label = label, onManualMeal = onManualMeal)
                    }
                    error?.let { Text(it, color = MetriaColors.Danger) }
                    if (status.imageState == "ready") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!label, { purpose = "meal" }, label = { Text("Meal") }, modifier = Modifier.heightIn(min = 48.dp))
                            FilterChip(label, { purpose = "label" }, label = { Text("Label") }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                        if (!label) OutlinedTextField(description, { value ->
                            if (value.length <= MealPhotoContext.MAX_LENGTH && value.none { it.isISOControl() && it !in "\n\r\t" }) description = value
                        }, label = { Text("Meal details (optional)") }, supportingText = { Text("${description.length}/${MealPhotoContext.MAX_LENGTH}") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(onClick = {
                            attempted = true
                            if (label) readLabel() else {
                                val next = analyzeImage(description)
                                requestId = next.imageAnalysisRequestId?.takeIf { next.imageState == "analyzing" && it != status.imageAnalysisRequestId }
                            }
                        }, enabled = label || status.state == "loaded", modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (label) "Read label" else "Prepare draft")
                        }
                        if (!label && status.state != "loaded") {
                            Text(if (status.state in setOf("ready", "initializing", "verifying", "downloading"))
                                "The assistant is getting ready. You can keep this preview open."
                                else "Set up the assistant to analyse a meal photo.", color = MetriaColors.InkMuted)
                            TextButton(onClick = { requestExit(onOpenTools) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Assistant setup") }
                        }
                        OutlinedButton(onClick = { choose(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Retake photo") }
                    } else AssistantCaptureSourceAction(
                        text = "Take photo", primary = true, onClick = { choose(true) },
                    )
                    AssistantCaptureSourceAction(
                        text = "Choose image", primary = false, onClick = { choose(false) },
                    )
                    Text("Review before saving. Photos are processed on this phone.", style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
                }
            }
        }
    }
    pendingExit?.let { action ->
        AlertDialog(onDismissRequest = { pendingExit = null }, title = { Text("Discard this capture?") },
            text = { Text("The unsaved photo and draft will be removed. Your saved meals are kept.") },
            confirmButton = { TextButton(onClick = { pendingExit = null; reset(); action() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { pendingExit = null }) { Text("Keep editing") } })
    }
}

@Composable
private fun CaptureDraftCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MetriaColors.Surface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MetriaColors.Divider)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun AssistantCaptureLoadingState(imageState: String, onStop: () -> Unit) {
    LinearProgressIndicator(Modifier.fillMaxWidth())
    Text(when (imageState) {
        "preparing" -> "Preparing your photo…"
        "reading_label" -> "Reading the nutrition label…"
        else -> "Preparing your meal draft…"
    }, style = MaterialTheme.typography.titleMedium)
    Text("Everything stays on this phone. Nothing has been saved.", color = MetriaColors.InkMuted)
    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop") }
}

@Composable
internal fun AssistantMealDraftCaptureCard(
    proposal: MealProposal,
    onReview: () -> Unit,
    onDiscard: () -> Unit,
) {
    CaptureDraftCard {
        Text("Your meal draft", style = MaterialTheme.typography.titleLarge)
        Text(proposal.items.joinToString(" · ") { it.name })
        Text("Estimate · ${proposal.overallConfidence} confidence", color = MetriaColors.InkMuted)
        Text(if (proposal.omittedItemCount > 0) "Some foods could not be identified. Add anything missing during review."
            else "Check foods and portions before saving.")
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review draft") }
        TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard draft") }
    }
}

@Composable
internal fun AssistantCaptureFailureState(label: Boolean, onManualMeal: () -> Unit) {
    Text(if (label) "Could not prepare a label draft. Try a clearer photo or enter a meal manually."
        else "Could not prepare a meal draft. Try another photo or enter the foods manually.", color = MetriaColors.Danger)
    TextButton(onClick = onManualMeal, modifier = Modifier.heightIn(min = 48.dp)) { Text("Enter manually") }
}

@Composable
internal fun AssistantCaptureSourceAction(text: String, primary: Boolean, onClick: () -> Unit) {
    if (primary) Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text)
    } else OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text)
    }
}
