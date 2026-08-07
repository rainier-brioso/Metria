package app.metria

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

private val Ink = Color(0xFF201A2B)
private val Plum = Color(0xFF5C3E77)
private val PlumSoft = Color(0xFFECE3F3)
private val Sage = Color(0xFF3F6C5B)
private val MetriaCanvas = Color(0xFFFBF8FC)

@Composable
fun MetriaMemberShell(vault: EncryptedVault, activity: MainActivity) {
    var status by remember { mutableStateOf(vault.status()) }
    var destination by remember { mutableStateOf("Today") }
    var todayTab by remember { mutableStateOf(0) }
    var addSheet by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var editMeal by remember { mutableStateOf<MealEntry?>(null) }
    var voiceDraft by remember { mutableStateOf<VoiceWeightDraft?>(null) }
    var modelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var mealProposalForConfirmation by remember { mutableStateOf<MealProposal?>(null) }
    var labelDraftForConfirmation by remember { mutableStateOf<LabelNutritionDraft?>(null) }
    var manualMealText by remember { mutableStateOf(false) }
    var browseFoodCatalog by remember { mutableStateOf(false) }
    var catalogFoodForConfirmation by remember { mutableStateOf<CatalogFood?>(null) }
    var showAssistantLoadingPreview by remember { mutableStateOf(false) }
    val foodCatalog = remember { runCatching { FoodCatalog.load(activity.applicationContext) }.getOrNull() }
    DisposableEffect(activity) {
        activity.setVaultCallback { status = it }
        activity.setVoiceDraftCallback { voiceDraft = it }
        onDispose { activity.setVaultCallback(null); activity.setVoiceDraftCallback(null) }
    }
    LaunchedEffect(status, voiceDraft) {
        if (status is EncryptedVault.Status.Unlocked && voiceDraft != null) addSheet = true
    }
    LaunchedEffect(destination) {
        while (destination == "Assistant") {
            modelStatus = activity.modelStatus()
            delay(250)
        }
    }
    LaunchedEffect(destination, modelStatus.state) {
        if (destination == "Assistant" && modelStatus.state == "ready") {
            modelStatus = activity.prepareLocalAssistant()
        }
    }
    LaunchedEffect(destination) {
        if (destination == "Assistant" && BuildConfig.DEBUG) {
            showAssistantLoadingPreview = true
            delay(1800)
            showAssistantLoadingPreview = false
        } else {
            showAssistantLoadingPreview = false
        }
    }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = Plum, onPrimary = Color.White, secondary = Sage,
        surface = MetriaCanvas, onSurface = Ink, surfaceVariant = PlumSoft,
    )) {
        when (val vaultStatus = status) {
            EncryptedVault.Status.Missing -> VaultGate("A private space for your health.", "Create an encrypted vault on this phone. No account is needed.") {
                activity.authenticateVault(true)
            }
            EncryptedVault.Status.Locked -> VaultGate("Welcome back.", "Your entries are protected until you unlock your vault.", autoAuthenticate = true) {
                activity.authenticateVault(false)
            }
            is EncryptedVault.Status.RecoveryRequired -> VaultGate("Vault unavailable", vaultStatus.message) {}
            is EncryptedVault.Status.Failed -> VaultGate("Try again", vaultStatus.message) { status = vault.status() }
            is EncryptedVault.Status.Unlocked -> {
                Scaffold(
                    containerColor = MetriaCanvas,
                    bottomBar = { MemberNavigation(destination) { destination = it } },
                    floatingActionButton = {
                        if (destination == "Today") FloatingActionButton(
                            onClick = { if (todayTab == 0) addSheet = true else destination = "Assistant" },
                            containerColor = Plum,
                        ) { Text("+") }
                        else if (destination == "History") FloatingActionButton(onClick = { addSheet = true }, containerColor = Plum) { Text("+") }
                    },
                ) { padding ->
                    Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (destination) {
                            "Today" -> TodayScreen(
                                vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit,
                                selectedTab = todayTab, onSelectTab = { todayTab = it }, onEditMeal = { editMeal = it },
                            )
                            "History" -> HistoryScreen(vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit, onEdit = { editEntry = it }, onDelete = { deleteEntry = it }, onEditMeal = { editMeal = it })
                            "Assistant" -> AssistantScreen(
                                modelStatus = modelStatus,
                                showLoadingPreview = showAssistantLoadingPreview,
                                openManualText = { manualMealText = true },
                                openFoodCatalog = { browseFoodCatalog = true },
                                review = { mealProposalForConfirmation = it },
                                prepareImage = { uri -> modelStatus = activity.prepareLocalMealImage(uri) },
                                analyzeImage = { modelStatus = activity.analyzeLocalMealImage() },
                                readLabel = { modelStatus = activity.readLocalNutritionLabel() },
                                discardImage = { modelStatus = activity.discardLocalMealImage() },
                                reviewLabel = { labelDraftForConfirmation = it },
                                newCameraUri = { activity.newLocalCameraCaptureUri() },
                                prepareCapturedImage = { uri -> modelStatus = activity.prepareLocalCapturedMealImage(uri) },
                                discardPendingCapture = { activity.discardPendingCameraCapture() },
                                beginExternalImageFlow = { activity.beginExternalImageFlow() },
                                beginImageFlow = { activity.beginLocalImageFlow() },
                                finishCooldown = { modelStatus = activity.finishLocalImageCooldown() },
                            )
                            else -> PrivacyScreen(vaultStatus.displayUnit, { status = vault.setDisplayUnit(if (vaultStatus.displayUnit == "kg") "lb" else "kg") }) { activity.lockVault() }
                        }
                    }
                }
                if (addSheet || editEntry != null) WeightBottomSheet(editEntry, voiceDraft, onDismiss = { addSheet = false; editEntry = null; voiceDraft = null }) { kilograms, note, date ->
                    status = editEntry?.let { vault.updateWeight(it.id, kilograms, note, date) } ?: vault.addWeight(kilograms, note, date)
                    addSheet = false
                    editEntry = null
                    voiceDraft = null
                }
                deleteEntry?.let { entry -> AlertDialog(onDismissRequest = { deleteEntry = null }, title = { Text("Delete entry?") }, text = { Text("This removes ${"%.1f".format(entry.kilograms)} kg from your encrypted vault.") }, confirmButton = { Button(onClick = { status = vault.deleteWeight(entry.id); deleteEntry = null }) { Text("Delete") } }, dismissButton = { Button(onClick = { deleteEntry = null }) { Text("Keep entry") } }) }
                editMeal?.let { meal -> MealEditSheet(
                    meal,
                    catalog = foodCatalog,
                    onDismiss = { editMeal = null },
                    onSave = { updated -> status = vault.updateMeal(updated); editMeal = null },
                    onRemoveReference = { status = vault.updateMeal(meal.copy(referenceThumbnailBase64 = null)) },
                    onDelete = { status = vault.deleteMeal(meal.id); editMeal = null },
                ) }
                mealProposalForConfirmation?.let { proposal -> MealConfirmationSheet(
                    proposal = proposal,
                    catalog = foodCatalog,
                    referenceThumbnailBase64 = modelStatus.referenceThumbnailBase64,
                    onDismiss = { mealProposalForConfirmation = null },
                    onSave = { meal ->
                        status = vault.addMeal(meal)
                        mealProposalForConfirmation = null
                        modelStatus = activity.clearLocalMealDraft()
                    },
                ) }
                labelDraftForConfirmation?.let { label -> LabelConfirmationSheet(
                    draft = label,
                    referenceThumbnailBase64 = modelStatus.referenceThumbnailBase64,
                    onDismiss = { labelDraftForConfirmation = null },
                    onSave = { meal ->
                        status = vault.addMeal(meal)
                        labelDraftForConfirmation = null
                        modelStatus = activity.clearLocalMealDraft()
                    },
                ) }
                if (manualMealText) ManualMealTextSheet(
                    onDismiss = { manualMealText = false },
                    onSubmit = { text -> modelStatus = activity.askLocalMealAssistant(text); manualMealText = false },
                )
                if (browseFoodCatalog) OfflineCatalogSearchSheet(
                    catalog = foodCatalog,
                    onDismiss = { browseFoodCatalog = false },
                    onSelect = { food -> catalogFoodForConfirmation = food; browseFoodCatalog = false },
                )
                catalogFoodForConfirmation?.let { food -> CatalogMealConfirmationSheet(
                    food = food,
                    catalog = foodCatalog,
                    onDismiss = { catalogFoodForConfirmation = null },
                    onSave = { meal ->
                        status = vault.addMeal(meal)
                        catalogFoodForConfirmation = null
                    },
                ) }
            }
        }
    }
}

@Composable
private fun VaultGate(title: String, body: String, autoAuthenticate: Boolean = false, action: () -> Unit) {
    // Approved unlock UX: prompt once on entering a locked vault; the icon is a retry only.
    LaunchedEffect(autoAuthenticate) {
        if (autoAuthenticate) action()
    }
    Surface(color = MetriaCanvas, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("METRIA", color = Plum, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 20.dp))
            Text(body, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp))
            if (autoAuthenticate) {
                IconButton(onClick = action, modifier = Modifier.size(68.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Unlock with biometrics", modifier = Modifier.size(40.dp), tint = Plum)
                }
            } else {
                Button(onClick = action, enabled = title != "Vault unavailable") { Text("Create private vault") }
            }
        }
    }
}

@Composable
private fun MemberNavigation(current: String, choose: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf("Today", "History", "Assistant", "Privacy").forEach { item ->
            val icon = when (item) {
                "Today" -> Icons.Default.Home
                "History" -> Icons.Default.ShowChart
                "Assistant" -> Icons.Default.SmartToy
                else -> Icons.Default.Lock
            }
            NavigationBarItem(selected = current == item, onClick = { choose(item) }, icon = { Icon(icon, contentDescription = item) }, label = { Text(item) })
        }
    }
}

@Composable
private fun QuietProgress(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = PlumSoft), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(28.dp), color = Plum, strokeWidth = 3.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, color = Color(0xFF655D6E))
            }
        }
    }
}

/** Metria's local-AI loading identity: a food-aware apple absorbs small bytes. */
@Composable
private fun AppleByteLoading(title: String, detail: String) {
    val transition = rememberInfiniteTransition(label = "metria-apple")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "bytes",
    )
    Column(
        Modifier.fillMaxWidth().height(520.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(180.dp)) {
            val center = Offset(size.width / 2, size.height / 2 + 10.dp.toPx())
            val body = 42.dp.toPx()
            drawCircle(PlumSoft, radius = body, center = center.copy(x = center.x - body * .42f))
            drawCircle(PlumSoft, radius = body, center = center.copy(x = center.x + body * .42f))
            drawCircle(PlumSoft, radius = body * 1.05f, center = center.copy(y = center.y + body * .25f))
            drawCircle(Plum, radius = body, center = center.copy(x = center.x - body * .42f), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
            drawCircle(Plum, radius = body, center = center.copy(x = center.x + body * .42f), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
            drawOval(Sage, topLeft = Offset(center.x + 7.dp.toPx(), center.y - body - 25.dp.toPx()), size = Size(28.dp.toPx(), 15.dp.toPx()))
            listOf(0f, .33f, .66f).forEachIndexed { index, offset ->
                val step = (phase + offset) % 1f
                val x = size.width * (.1f + step * .36f)
                val y = size.height * (.26f + index * .19f) + kotlin.math.sin((phase + index) * 6.28).toFloat() * 8.dp.toPx()
                val alpha = if (step < .15f) step / .15f else 1f - ((step - .7f) / .3f).coerceIn(0f, 1f)
                drawRoundRect(Plum.copy(alpha = alpha), Offset(x, y), Size(11.dp.toPx(), 11.dp.toPx()), CornerRadius(3.dp.toPx()))
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(detail, color = Color(0xFF655D6E), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CoolingDownLoading(secondsLeft: Int) {
    val transition = rememberInfiniteTransition(label = "metria-cooldown")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "cooling-pulse",
    )
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    Column(
        Modifier.fillMaxWidth().height(520.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(180.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = 42.dp.toPx() * pulse
            drawCircle(Color(0xFFE0F2F1), radius = radius, center = center)
            repeat(8) { index ->
                val angle = index * (Math.PI / 4.0)
                val dx = kotlin.math.cos(angle).toFloat()
                val dy = kotlin.math.sin(angle).toFloat()
                drawLine(Sage.copy(alpha = 0.8f), Offset(center.x + dx * radius * .35f, center.y + dy * radius * .35f), Offset(center.x + dx * radius * 1.45f, center.y + dy * radius * 1.45f), 5.dp.toPx())
            }
            drawCircle(Color.White, radius = radius * .38f, center = center)
        }
        Text("Cooling your phone", style = MaterialTheme.typography.titleLarge)
        Text("${minutes}:${seconds.toString().padStart(2, '0')} remaining", style = MaterialTheme.typography.headlineMedium, color = Plum, modifier = Modifier.padding(top = 8.dp))
        Text("Analysis will start automatically and stays on this phone.", color = Color(0xFF655D6E), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AssistantScreen(
    modelStatus: ModelStatus,
    showLoadingPreview: Boolean,
    openManualText: () -> Unit,
    openFoodCatalog: () -> Unit,
    review: (MealProposal) -> Unit,
    prepareImage: (android.net.Uri) -> Unit,
    analyzeImage: () -> Unit,
    readLabel: () -> Unit,
    discardImage: () -> Unit,
    reviewLabel: (LabelNutritionDraft) -> Unit,
    newCameraUri: () -> android.net.Uri,
    prepareCapturedImage: (android.net.Uri) -> Unit,
    discardPendingCapture: () -> Unit,
    beginExternalImageFlow: () -> Unit,
    beginImageFlow: () -> ModelStatus,
    finishCooldown: () -> Unit,
) {
    var sourceChoice by remember { mutableStateOf<String?>(null) }
    var pendingImagePurpose by remember { mutableStateOf<String?>(null) }
    var capturedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showOcrText by remember { mutableStateOf(false) }
    var showDeveloperResponse by remember { mutableStateOf(false) }
    var coolingSecondsLeft by remember { mutableStateOf(0) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) prepareImage(uri) else {
            pendingImagePurpose = null
            discardPendingCapture()
        }
    }
    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = capturedUri
        capturedUri = null
        if (success && uri != null) prepareCapturedImage(uri) else {
            pendingImagePurpose = null
            discardPendingCapture()
        }
    }
    LaunchedEffect(modelStatus.imageState, pendingImagePurpose) {
        if (modelStatus.imageState == "ready") {
            when (pendingImagePurpose) {
                "label" -> readLabel()
                "meal" -> analyzeImage()
            }
            pendingImagePurpose = null
        }
    }
    LaunchedEffect(modelStatus.imageState, modelStatus.imageRetryAtElapsedRealtime) {
        if (modelStatus.imageState == "cooling_down") {
            val retryAt = requireNotNull(modelStatus.imageRetryAtElapsedRealtime)
            while (true) {
                val remaining = (retryAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                coolingSecondsLeft = ((remaining + 999L) / 1_000L).toInt()
                if (remaining == 0L) {
                    finishCooldown()
                    break
                }
                delay(250)
            }
        }
    }
    val loading = when {
        showLoadingPreview -> "Metria is getting ready." to "Preparing locally on this phone."
        modelStatus.state in setOf("ready", "initializing") -> "Getting your private assistant ready." to "Preparing locally on this phone."
        modelStatus.state == "thinking" -> "Metria is reading your meal." to "Creating an editable estimate locally."
        modelStatus.imageState == "preparing" -> "Metria is preparing your image." to "A temporary local copy is being made."
        modelStatus.imageState == "ready" && pendingImagePurpose != null -> "Metria is getting your result ready." to "Nothing is saved automatically."
        modelStatus.imageState == "analyzing" -> "Metria is reading your meal photo." to "This can take a little while on your phone."
        modelStatus.imageState == "reading_label" -> "Metria is reading your label." to "Text is read locally and must be reviewed."
        else -> null
    }
    if (loading != null) {
        AppleByteLoading(loading.first, loading.second)
        return
    }
    if (modelStatus.imageState == "cooling_down") {
        CoolingDownLoading(coolingSecondsLeft)
        return
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (modelStatus.state != "loaded") when (modelStatus.state) {
                "loaded" -> Text("Ready on this phone. Estimates can be wrong—review every assumption.", color = Sage)
                else -> Text("The optional local model is ${modelStatus.state}. This developer build needs a downloaded model before chat can run.")
            }
            if (modelStatus.state == "loaded") {
                Button(onClick = openManualText, modifier = Modifier.fillMaxWidth()) { Text("Describe a meal") }
                Button(onClick = openFoodCatalog, modifier = Modifier.fillMaxWidth()) { Text("Search offline food catalog") }
                Button(onClick = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "label" }, modifier = Modifier.fillMaxWidth()) { Text("Scan nutrition label") }
                Button(onClick = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "meal" }, modifier = Modifier.fillMaxWidth()) { Text("Analyze meal photo") }
            }
            if (modelStatus.imageState == "failed") Text(modelStatus.imageReason, color = Color(0xFFB3261E))
        }
    }
    sourceChoice?.let { choice ->
        ModalBottomSheet(onDismissRequest = { sourceChoice = null }) {
            Column(Modifier.padding(24.dp).padding(bottom = 56.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (choice == "label") "Scan nutrition label" else "Analyze meal photo", style = MaterialTheme.typography.headlineSmall)
                Text("The image is processed locally and the temporary copy is deleted after analysis.", color = Color(0xFF655D6E))
                Button(onClick = {
                    val uri = newCameraUri()
                    capturedUri = uri
                    pendingImagePurpose = choice
                    sourceChoice = null
                    camera.launch(uri)
                }, modifier = Modifier.fillMaxWidth()) { Text("Take photo") }
                Button(onClick = {
                    sourceChoice = null
                    pendingImagePurpose = choice
                    beginExternalImageFlow()
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.fillMaxWidth()) { Text("Choose from library") }
            }
        }
    }
    modelStatus.assistantDiagnostic?.let { Text(it, color = Color(0xFFB3261E)) }
    modelStatus.assistantProposal?.let { proposal ->
        Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review this estimate", style = MaterialTheme.typography.titleLarge)
                Text("Confidence: ${proposal.overallConfidence}. Nothing has been saved.", color = Color(0xFF655D6E))
                proposal.items.forEach { item ->
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("${item.visiblePortion} · ${item.estimatedGramsRange.min}–${item.estimatedGramsRange.max} g · ${item.confidence} confidence")
                    Text(item.preparation, color = Color(0xFF655D6E))
                }
                proposal.assumptions.forEach { Text("Assumption: $it") }
                proposal.questions.forEach { Text("Question: $it") }
                Text("Nothing has been saved. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
            }
        }
    }
    modelStatus.proposalDiagnostic?.let { Text(modelStatus.imageReason, color = Color(0xFFB3261E)) }
    if (BuildConfig.DEBUG && modelStatus.debugLatestModelResponse != null) {
        Button(onClick = { showDeveloperResponse = !showDeveloperResponse }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showDeveloperResponse) "Hide development response" else "Inspect latest local response")
        }
        if (showDeveloperResponse) {
            Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Development-only response inspector", style = MaterialTheme.typography.titleMedium)
                    Text("In memory only. It clears on the next operation, vault lock, backgrounding, or restart.", color = Color(0xFF655D6E))
                    Text(modelStatus.debugLatestModelResponse, color = Ink)
                }
            }
        }
    }
    modelStatus.ocrText?.takeIf { it.isNotBlank() }?.let {
        val draft = modelStatus.labelDraft ?: LabelNutritionDraft(null, null, null, null)
        Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review nutrition label", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (modelStatus.labelDraft == null) "Local OCR read the label, but could not safely extract nutrition values. Enter them from the package."
                    else "Verified label draft. Check it against the packaging before saving.",
                    color = Color(0xFF655D6E),
                )
                Text(nutritionSummary(MealNutrition(draft.caloriesKcal, draft.proteinGrams, draft.carbohydrateGrams, draft.totalFatGrams, "verified_label")))
                Button(onClick = { showOcrText = !showOcrText }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showOcrText) "Hide local OCR text" else "Show local OCR text")
                }
                if (showOcrText) Text(it, color = Color(0xFF655D6E))
                Button(onClick = { reviewLabel(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm label") }
            }
        }
    }
    modelStatus.proposal?.let { proposal ->
        Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review image estimate", style = MaterialTheme.typography.titleLarge)
                Text("Confidence: ${proposal.overallConfidence}. Nothing has been saved.", color = Color(0xFF655D6E))
                proposal.items.forEach { item ->
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("${item.visiblePortion} - ${item.estimatedGramsRange.min}-${item.estimatedGramsRange.max} g - ${item.confidence} confidence")
                    Text(item.preparation, color = Color(0xFF655D6E))
                }
                proposal.assumptions.forEach { Text("Assumption: $it") }
                proposal.questions.forEach { Text("Question: $it") }
                Text("The temporary image was discarded. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
            }
        }
    }
}

@Composable
private fun TodayScreen(entries: List<WeightEntry>, meals: List<MealEntry>, unit: String, selectedTab: Int, onSelectTab: (Int) -> Unit, onEditMeal: (MealEntry) -> Unit) {
    val latest = entries.maxByOrNull(WeightEntry::recordedAt)
    val month = WeightTrend.filter(entries, LocalDate.now().minusDays(29), LocalDate.now())
    val todayNutrition = MealNutritionSummaries.between(meals, LocalDate.now(), LocalDate.now())
    val weekNutrition = MealNutritionSummaries.between(meals, LocalDate.now().minusDays(6), LocalDate.now())
    Text("Today", style = MaterialTheme.typography.displaySmall)
    Text("A calm view of your own progress.", color = Color(0xFF655D6E))
    TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
        listOf("Weight", "Meals").forEachIndexed { index, label ->
            Tab(selected = selectedTab == index, onClick = { onSelectTab(index) }, text = { Text(label) })
        }
    }
    if (selectedTab == 0) {
    Card(colors = CardDefaults.cardColors(containerColor = Plum), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LATEST WEIGHT", color = Color(0xFFEADFF1), style = MaterialTheme.typography.labelMedium)
            Text(latest?.let { formatWeight(it.kilograms, unit) } ?: "No entry yet", color = Color.White, style = MaterialTheme.typography.displayMedium)
            Text(latest?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: "Your first entry stays on this phone.", color = Color(0xFFEADFF1))
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("30-day reflection", style = MaterialTheme.typography.titleMedium)
            Text(WeightTrend.description(month), color = Color(0xFF655D6E))
            if (month.size >= 2) MiniTrend(month)
        }
    }
    }
    if (selectedTab == 1) {
        NutritionOverviewCard(todayNutrition, weekNutrition)
        val todayMeals = meals.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }
            .sortedByDescending(MealEntry::recordedAt)
        Text("Today’s meals", style = MaterialTheme.typography.titleMedium)
        if (todayMeals.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("No meals recorded today. Use the plus button to open your private assistant.", color = Color(0xFF655D6E), modifier = Modifier.padding(20.dp))
            }
        } else {
            todayMeals.forEach { meal -> MealRecordCard(meal, onEditMeal) }
        }
    }
}

@Composable
private fun NutritionOverviewCard(today: MealNutritionSummary, week: MealNutritionSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estimated nutrition", style = MaterialTheme.typography.titleMedium)
            if (today.completeNutritionMeals == 0) {
                Text("No meals with complete nutrition have been recorded today.", color = Color(0xFF655D6E))
            } else {
                Text(nutritionSummary(MealNutrition(today.caloriesKcal, today.proteinGrams, today.carbohydrateGrams, today.fatGrams)), color = Plum)
                Text("Today: ${today.completeNutritionMeals} of ${today.recordedMeals} recorded meals included.", color = Color(0xFF655D6E))
            }
            if (week.completeNutritionMeals > 0) {
                Text("Last 7 days", style = MaterialTheme.typography.titleSmall)
                Text(nutritionSummary(MealNutrition(week.caloriesKcal, week.proteinGrams, week.carbohydrateGrams, week.fatGrams)), color = Plum)
                Text("Based on ${week.completeNutritionMeals} of ${week.recordedMeals} recorded meals. These are estimates, not daily targets.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Add and confirm meals with complete nutrition to see a 7-day estimate.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HistoryScreen(entries: List<WeightEntry>, meals: List<MealEntry>, unit: String, onEdit: (WeightEntry) -> Unit, onDelete: (WeightEntry) -> Unit, onEditMeal: (MealEntry) -> Unit) {
    var days by remember { mutableStateOf(30) }
    var selectedTab by remember { mutableStateOf(0) }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    val start = if (days == 0) runCatching { LocalDate.parse(customStart) }.getOrNull() else LocalDate.now().minusDays((days - 1).toLong())
    val end = if (days == 0) runCatching { LocalDate.parse(customEnd) }.getOrNull() else LocalDate.now()
    val points = WeightTrend.filter(entries, start, end)
    Text("History", style = MaterialTheme.typography.displaySmall)
    TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
        listOf("Weight", "Meals").forEachIndexed { index, label ->
            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
        }
    }
    if (selectedTab == 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 30, 90).forEach { option -> FilterChip(selected = days == option, onClick = { days = option }, label = { Text("${option}D") }) }
        FilterChip(selected = days == 0, onClick = { days = 0 }, label = { Text("Custom") })
    }
    if (selectedTab == 0) {
    if (days == 0) { OutlinedTextField(customStart, { customStart = it }, label = { Text("Start · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(customEnd, { customEnd = it }, label = { Text("End · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth()) }
    }
    if (selectedTab == 0) Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp)) {
        Text("Weight trend", style = MaterialTheme.typography.titleMedium)
        Text(WeightTrend.description(points), color = Color(0xFF655D6E))
        if (points.size >= 2) MiniTrend(points)
    } }
    if (selectedTab == 0) Text("Entries", style = MaterialTheme.typography.titleLarge)
    if (selectedTab == 0) entries.sortedByDescending(WeightEntry::recordedAt).forEach { entry ->
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(formatWeight(entry.kilograms, unit), style = MaterialTheme.typography.titleMedium); entry.note?.let { Text(it, color = Color(0xFF655D6E)) } }
            Column(horizontalAlignment = Alignment.End) {
                Text(entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(), color = Color(0xFF655D6E))
                Row {
                    IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Default.Edit, contentDescription = "Edit weight entry") }
                    IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, contentDescription = "Delete weight entry") }
                }
            }
        } }
    }
    if (selectedTab == 1 && meals.isNotEmpty()) {
        Text("Meals", style = MaterialTheme.typography.titleLarge)
        meals.sortedByDescending(MealEntry::recordedAt).forEach { meal ->
            MealRecordCard(meal, onEditMeal)
        }
    }
}

@Composable
private fun MealRecordCard(meal: MealEntry, onEdit: (MealEntry) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        val photo = meal.referenceThumbnailBase64
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable { onEdit(meal) }) {
            photo?.let { HistoryReferenceThumbnail(it) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(meal.items.joinToString { it.name }, style = MaterialTheme.typography.titleMedium)
                Text("${meal.source.replace('_', ' ')} - ${meal.confidence} confidence - ${meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}", color = Color(0xFF655D6E))
                meal.nutrition?.let { nutrition -> Text(nutritionSummary(nutrition), color = Color(0xFF655D6E)) }
            }
        }
    }
}

@Composable
private fun PrivacyScreen(unit: String, changeUnit: () -> Unit, lock: () -> Unit) {
    Text("Private by default", style = MaterialTheme.typography.displaySmall)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your data stays with you", style = MaterialTheme.typography.titleLarge)
        Text("Weight entries are encrypted on this phone. Metria has no account, advertising, analytics, or default sync.")
        Text("Optional photo assistance is processed locally and temporary copies are deleted after use. A reduced reference thumbnail is kept only when you confirm an entry and leave that option enabled.")
    } }
    Button(onClick = changeUnit, modifier = Modifier.fillMaxWidth()) { Text(if (unit == "kg") "Use pounds" else "Use kilograms") }
    Button(onClick = lock, modifier = Modifier.fillMaxWidth()) { Text("Lock vault") }
}

private fun formatWeight(kilograms: Double, unit: String): String = if (unit == "lb") "${"%.1f".format(WeightUnits.kilogramsToPounds(kilograms))} lb" else "${"%.1f".format(kilograms)} kg"

private data class EditableMealItem(val name: String, val grams: String, val preparation: String)

@Composable
private fun PrivateReferenceOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 12.dp)) {
            Text("Keep private reference photo", style = MaterialTheme.typography.titleSmall)
            Text("A reduced thumbnail only; encrypted here. The original is deleted and never uploaded.", color = Color(0xFF655D6E))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReferenceThumbnail(encoded: String, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val bitmap = remember(encoded) {
        runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Private meal reference",
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun HistoryReferenceThumbnail(encoded: String) {
    val bitmap = remember(encoded) {
        runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Private meal reference",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.width(116.dp).fillMaxHeight(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealConfirmationSheet(proposal: MealProposal, catalog: OfflineFoodCatalog?, referenceThumbnailBase64: String?, onDismiss: () -> Unit, onSave: (MealEntry) -> Unit) {
    var items by remember(proposal) { mutableStateOf(proposal.items.map { item ->
        EditableMealItem(item.name, ((item.estimatedGramsRange.min + item.estimatedGramsRange.max) / 2).toString(), item.preparation)
    }) }
    var date by remember(proposal) { mutableStateOf(LocalDate.now().toString()) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var keepReference by remember(referenceThumbnailBase64) { mutableStateOf(referenceThumbnailBase64 != null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Approved M3 direction: every assistant item has an independently
    // reviewable catalog match. A total is shown only when all items match.
    var catalogMatches by remember(proposal, catalog) {
        mutableStateOf(proposal.items.map { item -> catalog?.let { FoodCatalog.suggest(it, item.name) } })
    }
    var catalogSearchIndex by remember { mutableStateOf<Int?>(null) }
    val parsedGrams = items.map { it.grams.replace(',', '.').toIntOrNull()?.takeIf { grams -> grams in 1..2_000 } }
    val perItemNutrition = items.indices.map { index ->
        catalogMatches.getOrNull(index)?.let { food -> parsedGrams[index]?.let { FoodCatalog.calculate(food, it) } }
    }
    val hasCatalogMatchForEveryItem = catalog != null && catalogMatches.size == items.size &&
        catalogMatches.all { it != null } && parsedGrams.all { it != null }
    val catalogTotals = perItemNutrition.takeIf { hasCatalogMatchForEveryItem && it.all { nutrition ->
        nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
            nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
    } }?.let { values ->
        CatalogNutrition(
            values.sumOf { it!!.caloriesKcal!! },
            values.sumOf { it!!.proteinGrams!! },
            values.sumOf { it!!.carbohydrateGrams!! },
            values.sumOf { it!!.fatGrams!! },
            values.any { it!!.caloriesDerivedFromMacros },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirm meal", style = MaterialTheme.typography.headlineMedium)
            Text("AI estimate - correct each portion and preparation before saving. This is a private meal record, not medical advice.", color = Plum)
            items.forEachIndexed { index, item ->
                OutlinedTextField(item.name, { value ->
                    items = items.toMutableList().also { it[index] = item.copy(name = value) }
                    catalogMatches = catalogMatches.toMutableList().also { it[index] = null }
                }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value -> items = items.toMutableList().also { it[index] = item.copy(preparation = value) } }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
                val match = catalogMatches.getOrNull(index)
                if (match == null) {
                    Text("No catalog match selected; this item is not included in a nutrition total.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Suggested match: ${FoodCatalog.memberDisplayName(match)}", color = Plum, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { catalogSearchIndex = index }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (match == null) "Choose offline match" else "Change offline match")
                }
            }
            if (catalogTotals == null) {
                Text("Nutrition total unavailable", style = MaterialTheme.typography.titleMedium)
                Text("Choose a match and a valid gram portion for every item to calculate the total. You can still save this reviewed meal without nutrition values.", color = Color(0xFF655D6E))
            } else {
                Text("Member-confirmed catalog estimate", style = MaterialTheme.typography.titleMedium)
                Text(nutritionSummary(MealNutrition(
                    catalogTotals.caloriesKcal, catalogTotals.proteinGrams,
                    catalogTotals.carbohydrateGrams, catalogTotals.fatGrams,
                    "member_confirmed_catalog", catalogRelease = catalog!!.sourceRelease,
                )))
                Text("Calculated from ${catalog!!.sourceName} ${catalog.sourceRelease}.", color = Plum, style = MaterialTheme.typography.bodySmall)
                if (catalogTotals.caloriesDerivedFromMacros) Text("Calories are calculated from available protein, carbohydrates, and fat because one selected food has no USDA energy value.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            }
            if (referenceThumbnailBase64 != null) PrivateReferenceOption(keepReference) { keepReference = it }
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val recordedAt = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
                val savedItems = items.mapIndexed { index, item -> MealEntryItem(
                    item.name.trim(), parsedGrams[index], item.preparation.trim().takeIf(String::isNotEmpty),
                    catalogMatches.getOrNull(index)?.fdcId.takeIf { catalogTotals != null },
                    catalog?.sourceRelease?.takeIf { catalogTotals != null },
                ) }
                val nutrition = catalogTotals?.let { totals -> MealNutrition(
                    totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams,
                    "member_confirmed_catalog", catalogRelease = catalog?.sourceRelease,
                ) }
                val meal = if (recordedAt == null) null else MealEntry(
                    recordedAt = recordedAt, items = savedItems, assumptions = proposal.assumptions,
                    confidence = proposal.overallConfidence,
                    source = if (nutrition == null) "ai_estimate" else "member_confirmed_catalog",
                    nutrition = nutrition, referenceThumbnailBase64 = referenceThumbnailBase64.takeIf { keepReference },
                )
                if (meal == null || !MealEntryValidation.valid(meal)) error = "Review the food names, portions (1-2,000 g), and date." else onSave(meal)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (catalogTotals == null) "Save meal without nutrition" else "Save estimated nutrition locally")
            }
        }
    }
    catalogSearchIndex?.let { index -> OfflineCatalogSearchSheet(
        catalog = catalog,
        initialQuery = items.getOrNull(index)?.name.orEmpty(),
        onDismiss = { catalogSearchIndex = null },
        onSelect = { food ->
            catalogMatches = catalogMatches.toMutableList().also { it[index] = food }
            catalogSearchIndex = null
        },
    ) }
}

private fun nutritionSummary(nutrition: MealNutrition): String = listOfNotNull(
    nutrition.caloriesKcal?.let { "${"%.0f".format(it)} kcal" },
    nutrition.proteinGrams?.let { "${"%.1f".format(it)} g protein" },
    nutrition.carbohydrateGrams?.let { "${"%.1f".format(it)} g carbs" },
    nutrition.fatGrams?.let { "${"%.1f".format(it)} g fat" },
).joinToString(" - ").ifBlank { "No nutrition values entered." }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealEditSheet(meal: MealEntry, catalog: OfflineFoodCatalog?, onDismiss: () -> Unit, onSave: (MealEntry) -> Unit, onRemoveReference: () -> Unit, onDelete: () -> Unit) {
    var items by remember(meal) { mutableStateOf(meal.items.map { EditableMealItem(it.name, it.grams?.toString().orEmpty(), it.preparation.orEmpty()) }) }
    var date by remember(meal) { mutableStateOf(meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString()) }
    var calories by remember(meal) { mutableStateOf(meal.nutrition?.caloriesKcal?.toString().orEmpty()) }
    var protein by remember(meal) { mutableStateOf(meal.nutrition?.proteinGrams?.toString().orEmpty()) }
    var carbs by remember(meal) { mutableStateOf(meal.nutrition?.carbohydrateGrams?.toString().orEmpty()) }
    var fat by remember(meal) { mutableStateOf(meal.nutrition?.fatGrams?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmReferenceRemoval by remember { mutableStateOf(false) }
    var confirmMealDeletion by remember { mutableStateOf(false) }
    var referenceThumbnail by remember(meal) { mutableStateOf(meal.referenceThumbnailBase64) }
    var calculateWithCatalog by remember(meal) { mutableStateOf(meal.nutrition?.source == "member_confirmed_catalog") }
    val isCatalogMeal = calculateWithCatalog
    var catalogMatches by remember(meal, catalog) { mutableStateOf(meal.items.map { item ->
        val foodId = item.catalogFoodId ?: meal.nutrition?.catalogFoodId.takeIf { meal.items.size == 1 }
        foodId?.let { id -> catalog?.foods?.firstOrNull { it.fdcId == id } } ?: catalog?.let { FoodCatalog.suggest(it, item.name) }
    }) }
    var catalogSearchIndex by remember { mutableStateOf<Int?>(null) }
    val parsedCatalogGrams = items.map { it.grams.replace(',', '.').toIntOrNull()?.takeIf { grams -> grams in 1..2_000 } }
    val catalogValues = items.indices.map { index ->
        catalogMatches.getOrNull(index)?.let { food -> parsedCatalogGrams[index]?.let { FoodCatalog.calculate(food, it) } }
    }
    val catalogTotals = catalogValues.takeIf { isCatalogMeal && catalog != null && catalogMatches.all { it != null } &&
        parsedCatalogGrams.all { it != null } && it.all { nutrition ->
            nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
                nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
        } }?.let { values -> CatalogNutrition(
        values.sumOf { it!!.caloriesKcal!! }, values.sumOf { it!!.proteinGrams!! },
        values.sumOf { it!!.carbohydrateGrams!! }, values.sumOf { it!!.fatGrams!! },
        values.any { it!!.caloriesDerivedFromMacros },
    ) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit meal", style = MaterialTheme.typography.headlineMedium)
            referenceThumbnail?.let { reference ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(152.dp)) {
                        ReferenceThumbnail(reference, 152.dp)
                        Surface(
                            color = Color.White.copy(alpha = 0.72f),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp),
                        ) {
                            IconButton(onClick = { confirmReferenceRemoval = true }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove private reference photo", tint = Ink)
                            }
                        }
                    }
                }
            }
            items.forEachIndexed { index, item ->
                OutlinedTextField(item.name, { value ->
                    items = items.toMutableList().also { it[index] = item.copy(name = value) }
                    if (isCatalogMeal) catalogMatches = catalogMatches.toMutableList().also { it[index] = null }
                }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value -> items = items.toMutableList().also { it[index] = item.copy(preparation = value) } }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
                if (isCatalogMeal) {
                    val match = catalogMatches.getOrNull(index)
                    Text(
                        if (match == null) "No catalog match selected; choose one before saving."
                        else "Catalog match: ${FoodCatalog.memberDisplayName(match)}",
                        color = if (match == null) Color(0xFFB3261E) else Plum,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { catalogSearchIndex = index }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (match == null) "Choose offline match" else "Change offline match")
                    }
                }
            }
            if (isCatalogMeal) {
                Text("Member-confirmed catalog estimate", style = MaterialTheme.typography.titleMedium)
                if (catalogTotals == null) {
                    Text("Choose a valid whole-gram portion and a catalog match for every food to recalculate this meal.", color = Color(0xFFB3261E))
                } else {
                    Text(nutritionSummary(MealNutrition(
                        catalogTotals.caloriesKcal, catalogTotals.proteinGrams,
                        catalogTotals.carbohydrateGrams, catalogTotals.fatGrams,
                        "member_confirmed_catalog", catalogRelease = catalog!!.sourceRelease,
                    )), color = Plum)
                    if (catalogTotals.caloriesDerivedFromMacros) Text("Calories are calculated from available macros for one selected food.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(onClick = { calculateWithCatalog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Calculate from offline catalog")
                }
                Text("Metria will suggest local matches for the foods and portions above. Review them before saving.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbohydrates (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val recordedAt = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
                val updatedItems = items.mapIndexed { index, item -> MealEntryItem(
                    item.name.trim(),
                    if (isCatalogMeal) parsedCatalogGrams[index] else item.grams.replace(',', '.').toIntOrNull(),
                    item.preparation.trim().takeIf(String::isNotEmpty),
                    catalogMatches.getOrNull(index)?.fdcId.takeIf { isCatalogMeal && catalogTotals != null },
                    catalog?.sourceRelease?.takeIf { isCatalogMeal && catalogTotals != null },
                ) }
                val rawNutrition = listOf(calories, protein, carbs, fat)
                val values = rawNutrition.map { it.replace(',', '.').toDoubleOrNull() }
                val invalidNutrition = rawNutrition.zip(values).any { (raw, value) -> raw.isNotBlank() && value == null }
                val nutrition = when {
                    isCatalogMeal -> catalogTotals?.let { totals -> MealNutrition(
                        totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams,
                        "member_confirmed_catalog", catalogRelease = catalog?.sourceRelease,
                    ) }
                    rawNutrition.all(String::isBlank) -> null
                    else -> MealNutrition(values[0], values[1], values[2], values[3], "manual_entry")
                }
                val updated = if (recordedAt == null) null else meal.copy(
                    recordedAt = recordedAt,
                    items = updatedItems,
                    source = if (isCatalogMeal) "member_confirmed_catalog" else meal.source,
                    nutrition = nutrition,
                    referenceThumbnailBase64 = referenceThumbnail,
                )
                if (invalidNutrition || updated == null || !MealEntryValidation.valid(updated)) error = "Choose a valid catalog match and portion for every food, then review the date." else onSave(updated)
            }, modifier = Modifier.fillMaxWidth()) { Text("Save meal changes") }
            Button(onClick = { confirmMealDeletion = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete meal") }
        }
    }
    catalogSearchIndex?.let { index -> OfflineCatalogSearchSheet(
        catalog = catalog,
        initialQuery = items.getOrNull(index)?.name.orEmpty(),
        onDismiss = { catalogSearchIndex = null },
        onSelect = { food ->
            catalogMatches = catalogMatches.toMutableList().also { it[index] = food }
            catalogSearchIndex = null
        },
    ) }
    if (confirmReferenceRemoval) {
        AlertDialog(
            onDismissRequest = { confirmReferenceRemoval = false },
            title = { Text("Remove private reference?") },
            text = { Text("This removes only the encrypted thumbnail. The meal details stay in your vault.") },
            confirmButton = { Button(onClick = { onRemoveReference(); referenceThumbnail = null; confirmReferenceRemoval = false }) { Text("Remove photo") } },
            dismissButton = { Button(onClick = { confirmReferenceRemoval = false }) { Text("Keep photo") } },
        )
    }
    if (confirmMealDeletion) {
        AlertDialog(
            onDismissRequest = { confirmMealDeletion = false },
            title = { Text("Delete meal?") },
            text = { Text("This permanently removes the meal and its optional private reference from your encrypted vault.") },
            confirmButton = { Button(onClick = onDelete) { Text("Delete") } },
            dismissButton = { Button(onClick = { confirmMealDeletion = false }) { Text("Keep meal") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineCatalogSearchSheet(
    catalog: OfflineFoodCatalog?,
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onSelect: (CatalogFood) -> Unit,
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val results = remember(catalog, query) { catalog?.let { FoodCatalog.search(it, query) }.orEmpty() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Offline food catalog", style = MaterialTheme.typography.headlineMedium)
            if (catalog == null) {
                Text("The local catalog could not be opened. You can still describe a meal or enter a label manually.", color = Color(0xFFB3261E))
                return@Column
            }
            Text("Search ${catalog.foods.size} foods stored in this app. This never contacts a server.", color = Color(0xFF655D6E))
            OutlinedTextField(query, { query = it }, label = { Text("Food, for example rice or chicken") }, modifier = Modifier.fillMaxWidth())
            if (query.isNotBlank() && results.isEmpty()) Text("No local match. You can still describe the meal manually.", color = Color(0xFF655D6E))
            results.forEach { food ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(food) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(FoodCatalog.memberDisplayName(food), style = MaterialTheme.typography.titleMedium)
                        if (FoodCatalog.memberDisplayName(food) != food.description) {
                            Text("USDA: ${food.description}", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                        }
                        food.category?.let { Text(it, color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall) }
                        Text("USDA Foundation Foods · ${food.portions.firstOrNull()?.label ?: "100 g reference"}", color = Plum, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogMealConfirmationSheet(
    food: CatalogFood,
    catalog: OfflineFoodCatalog?,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit,
) {
    if (catalog == null) return
    var grams by remember(food) { mutableStateOf(FoodCatalog.suggestedGrams(food).toString()) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val parsedGrams = grams.replace(',', '.').toIntOrNull()
    val calculated = parsedGrams?.takeIf { it in 1..2_000 }?.let { FoodCatalog.calculate(food, it) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirm catalog match", style = MaterialTheme.typography.headlineMedium)
            Text(FoodCatalog.memberDisplayName(food), style = MaterialTheme.typography.titleLarge)
            if (FoodCatalog.memberDisplayName(food) != food.description) Text("USDA: ${food.description}", color = Color(0xFF655D6E))
            Text("Member-confirmed catalog match · ${catalog.sourceName} ${catalog.sourceRelease}", color = Plum)
            Text("These are an estimate for the portion you choose. Review before saving.", color = Color(0xFF655D6E))
            OutlinedTextField(grams, { grams = it }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
            calculated?.let { nutrition ->
                Text(nutritionSummary(MealNutrition(
                    nutrition.caloriesKcal,
                    nutrition.proteinGrams,
                    nutrition.carbohydrateGrams,
                    nutrition.fatGrams,
                    "member_confirmed_catalog",
                    food.fdcId,
                    catalog.sourceRelease,
                )))
                if (nutrition.caloriesDerivedFromMacros) Text("Calories are calculated from available macros for this food.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            } ?: Text("Enter a whole portion from 1 to 2,000 g to calculate nutrition.", color = Color(0xFFB3261E))
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val safeGrams = parsedGrams?.takeIf { it in 1..2_000 }
                val nutrition = safeGrams?.let { FoodCatalog.calculate(food, it) }
                val recordedAt = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
                val meal = if (safeGrams == null || nutrition == null || recordedAt == null) null else MealEntry(
                    recordedAt = recordedAt,
                    items = listOf(MealEntryItem(
                        name = FoodCatalog.memberDisplayName(food),
                        grams = safeGrams,
                        preparation = null,
                        catalogFoodId = food.fdcId,
                        catalogRelease = catalog.sourceRelease,
                    )),
                    assumptions = listOf("Portion selected by member from the offline USDA Foundation Foods catalog."),
                    confidence = "medium",
                    source = "member_confirmed_catalog",
                    nutrition = MealNutrition(
                        caloriesKcal = nutrition.caloriesKcal,
                        proteinGrams = nutrition.proteinGrams,
                        carbohydrateGrams = nutrition.carbohydrateGrams,
                        fatGrams = nutrition.fatGrams,
                        source = "member_confirmed_catalog",
                        catalogFoodId = food.fdcId,
                        catalogRelease = catalog.sourceRelease,
                    ),
                )
                if (meal == null || !MealEntryValidation.valid(meal)) error = "Enter a whole portion from 1 to 2,000 g and a valid date." else onSave(meal)
            }, modifier = Modifier.fillMaxWidth()) { Text("Save confirmed catalog meal locally") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualMealTextSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 56.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Describe a meal", style = MaterialTheme.typography.headlineMedium)
            Text("Metria creates an editable local estimate. Nothing is saved automatically.", color = Color(0xFF655D6E))
            OutlinedTextField(text, { text = it }, label = { Text("For example: chicken breast with half a plate of rice") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create editable estimate") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelConfirmationSheet(draft: LabelNutritionDraft, referenceThumbnailBase64: String?, onDismiss: () -> Unit, onSave: (MealEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var calories by remember { mutableStateOf(draft.caloriesKcal?.toString().orEmpty()) }
    var protein by remember { mutableStateOf(draft.proteinGrams?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(draft.carbohydrateGrams?.toString().orEmpty()) }
    var fat by remember { mutableStateOf(draft.totalFatGrams?.toString().orEmpty()) }
    var keepReference by remember(referenceThumbnailBase64) { mutableStateOf(referenceThumbnailBase64 != null) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Confirm nutrition label", style = MaterialTheme.typography.headlineMedium)
            Text("These values were read locally from a label. Compare them with the package; OCR can make mistakes.", color = Plum)
            OutlinedTextField(name, { name = it }, label = { Text("Food or product name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbohydrates (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
            if (referenceThumbnailBase64 != null) PrivateReferenceOption(keepReference) { keepReference = it }
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                fun value(raw: String) = raw.replace(',', '.').toDoubleOrNull()
                val values = listOf(value(calories), value(protein), value(carbs), value(fat))
                val raw = listOf(calories, protein, carbs, fat)
                val invalid = raw.zip(values).any { (input, parsed) -> input.isNotBlank() && parsed == null }
                val recordedAt = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
                val meal = if (recordedAt == null) null else MealEntry(
                    recordedAt = recordedAt,
                    items = listOf(MealEntryItem(name.trim(), null, null)),
                    assumptions = listOf("Nutrition values were reviewed from a package label."),
                    confidence = "high",
                    source = "manual_entry",
                    nutrition = MealNutrition(values[0], values[1], values[2], values[3], "verified_label"),
                    referenceThumbnailBase64 = referenceThumbnailBase64.takeIf { keepReference },
                )
                if (invalid || meal == null || !MealEntryValidation.valid(meal)) error = "Enter a product name, valid non-negative values, and a valid date." else onSave(meal)
            }, modifier = Modifier.fillMaxWidth()) { Text("Save verified label locally") }
        }
    }
}

@Composable
private fun MiniTrend(points: List<WeightTrendPoint>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val low = points.minOf(WeightTrendPoint::kilograms)
        val span = (points.maxOf(WeightTrendPoint::kilograms) - low).takeIf { it > 0 } ?: 1.0
        val step = size.width / (points.size - 1)
        for (index in 1 until points.size) drawLine(Plum, Offset((index - 1) * step, size.height - (((points[index - 1].kilograms - low) / span) * size.height).toFloat()), Offset(index * step, size.height - (((points[index].kilograms - low) / span) * size.height).toFloat()), 7f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightBottomSheet(entry: WeightEntry?, voiceDraft: VoiceWeightDraft?, onDismiss: () -> Unit, save: (Double, String?, Instant) -> Unit) {
    var value by remember(entry, voiceDraft) { mutableStateOf(entry?.kilograms?.toString() ?: voiceDraft?.kilograms?.toString().orEmpty()) }; var note by remember(entry) { mutableStateOf(entry?.note.orEmpty()) }; var date by remember(entry, voiceDraft) { mutableStateOf(entry?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: voiceDraft?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString().orEmpty()) }; var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (entry == null) "Record weight" else "Edit weight", style = MaterialTheme.typography.headlineMedium)
        if (voiceDraft != null) Text("Voice draft — review before saving.", color = Plum)
        Text("Saved only after you confirm.", color = Color(0xFF655D6E))
        OutlinedTextField(value, { value = it }, label = { Text("Weight in kg") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(date, { date = it }, label = { Text("Date · YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("A private note (optional)") }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Button(onClick = { val kg = value.replace(',', '.').toDoubleOrNull(); val instant = if (date.isBlank()) Instant.now() else runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull(); if (kg == null || instant == null || !WeightUnits.isValidKilograms(kg)) error = "Enter 15–500 kg and a valid optional date." else save(kg, note, instant) }, modifier = Modifier.fillMaxWidth()) { Text("Save locally") }
    } }
}
