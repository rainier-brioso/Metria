package app.metria

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import app.metria.ui.navigation.MetriaBottomNavigation
import app.metria.ui.components.MetriaActionSheet
import app.metria.ui.screens.MetriaMealReviewSheet
import app.metria.ui.screens.MetriaPrivacySettingsScreen
import app.metria.ui.screens.MetriaProgressScreen
import app.metria.ui.screens.MetriaTodayScreen
import app.metria.ui.theme.MetriaColors
import app.metria.ui.theme.MetriaTheme

private val Ink = Color(0xFF201A2B)
private val Plum = Color(0xFF5C3E77)
private val PlumSoft = Color(0xFFECE3F3)
private val Sage = Color(0xFF3F6C5B)
private val MetriaCanvas = Color(0xFFFBF8FC)

private fun mealInstantForDate(date: String, localTime: LocalTime): Instant? =
    runCatching { LocalDate.parse(date).atTime(localTime).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MetriaMemberShell(vault: EncryptedVault, activity: MainActivity) {
    var status by remember { mutableStateOf(vault.status()) }
    var hasCompletedOnboarding by remember { mutableStateOf(activity.hasCompletedOnboarding()) }
    var setupModelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var destination by remember { mutableStateOf("Today") }
    // UX decision: the full tab remains Metria's workspace; this sheet makes the same assistant available in context.
    var quickAssistantOpen by remember { mutableStateOf(false) }
    var quickAssistantMenuOpen by remember { mutableStateOf(false) }
    var quickAssistantStartVoice by remember { mutableStateOf(false) }
    var quickAssistantListening by remember { mutableStateOf(false) }
    // Approved UX: the floating assistant reuses this one camera flow; no image path or storage is duplicated.
    var assistantImagePurpose by remember { mutableStateOf<String?>(null) }
    var addSheet by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var editMeal by remember { mutableStateOf<MealEntry?>(null) }
    var voiceDraft by remember { mutableStateOf<VoiceWeightDraft?>(null) }
    var modelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var mealProposalForConfirmation by remember { mutableStateOf<MealProposal?>(null) }
    var labelDraftForConfirmation by remember { mutableStateOf<LabelNutritionDraft?>(null) }
    var browseFoodCatalog by remember { mutableStateOf(false) }
    var catalogFoodForConfirmation by remember { mutableStateOf<CatalogFood?>(null) }
    var editNutritionTargets by remember { mutableStateOf(false) }
    var editCycleContext by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var restoreSource by remember { mutableStateOf<android.net.Uri?>(null) }
    var archivePreview by remember { mutableStateOf<EncryptedVault.ArchiveSummary?>(null) }
    var archiveMessage by remember { mutableStateOf<String?>(null) }
    val archiveExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { destination ->
        val passphrase = exportPassphrase
        exportPassphrase = null
        if (destination == null || passphrase == null) {
            passphrase?.fill('\u0000')
            activity.finishExternalArchiveFlow()
        } else {
            val result = activity.exportRecoveryArchive(destination, passphrase.concatToString())
            passphrase.fill('\u0000')
            archiveMessage = result.fold(
                onSuccess = { "Encrypted recovery archive saved. Keep its passphrase separately." },
                onFailure = { "Archive was not saved: ${it.message ?: "unknown error"}" },
            )
        }
    }
    val archiveImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        activity.finishExternalArchiveFlow()
        restoreSource = source
        if (source == null) archiveMessage = "No archive was selected."
    }
    val foodCatalog = remember { runCatching { FoodCatalog.load(activity.applicationContext) }.getOrNull() }
    DisposableEffect(activity) {
        activity.setVaultCallback { status = it }
        activity.setVoiceDraftCallback { voiceDraft = it }
        onDispose { activity.setVaultCallback(null); activity.setVoiceDraftCallback(null) }
    }
    LaunchedEffect(status, voiceDraft) {
        if (status is EncryptedVault.Status.Unlocked && voiceDraft != null) addSheet = true
    }
    LaunchedEffect(status, hasCompletedOnboarding) {
        while (hasCompletedOnboarding && (status is EncryptedVault.Status.Missing || status is EncryptedVault.Status.Unlocked)) {
            val current = activity.modelStatus()
            setupModelStatus = current
            modelStatus = current
            // Product decision: pre-warm only after biometric unlock. The downloaded artifact is already
            // local; this prepares its runtime quietly while the member can use the rest of the app.
            if (status is EncryptedVault.Status.Unlocked && current.state == "ready") {
                val initializing = activity.prepareLocalAssistant()
                setupModelStatus = initializing
                modelStatus = initializing
            }
            delay(250)
        }
    }
    LaunchedEffect(destination) {
        while (destination == "Assistant") {
            modelStatus = activity.modelStatus()
            delay(250)
        }
    }
    LaunchedEffect(quickAssistantOpen) {
        while (quickAssistantOpen) {
            modelStatus = activity.modelStatus()
            delay(250)
        }
    }
    LaunchedEffect(quickAssistantStartVoice, modelStatus.state) {
        if (quickAssistantStartVoice && modelStatus.state == "ready") {
            modelStatus = activity.prepareLocalAssistant()
        }
    }
    MetriaTheme {
        when (val vaultStatus = status) {
            EncryptedVault.Status.Missing -> if (!hasCompletedOnboarding) FirstRunOnboarding(
                onComplete = { activity.completeOnboarding(); hasCompletedOnboarding = true },
            ) else AssistantSetupGate(
                modelStatus = setupModelStatus,
                download = { setupModelStatus = activity.downloadLocalAssistant() },
                cancelDownload = { setupModelStatus = activity.cancelLocalAssistantDownload() },
                continueToApp = { activity.authenticateVault(true) },
            )
            EncryptedVault.Status.Locked -> VaultGate("Welcome back.", "Your entries are protected until you unlock your vault.", autoAuthenticate = true) {
                activity.authenticateVault(false)
            }
            is EncryptedVault.Status.RecoveryRequired -> VaultGate("Vault unavailable", vaultStatus.message) {}
            is EncryptedVault.Status.Failed -> VaultGate("Try again", vaultStatus.message) { status = vault.status() }
            is EncryptedVault.Status.Unlocked -> {
                if (setupModelStatus.state in setOf("absent", "removed", "failed", "downloading", "verifying")) {
                    AssistantSetupGate(
                        modelStatus = setupModelStatus,
                        download = { setupModelStatus = activity.downloadLocalAssistant() },
                        cancelDownload = { setupModelStatus = activity.cancelLocalAssistantDownload() },
                        continueToApp = {},
                    )
                    return@MetriaTheme
                }
                Scaffold(
                    containerColor = MetriaColors.Canvas,
                    bottomBar = {
                        MetriaBottomNavigation(
                            current = destination,
                            assistantListening = quickAssistantListening,
                            onNavigate = { destination = it },
                            onMetriaAction = { quickAssistantMenuOpen = true },
                        )
                    },
                ) { padding ->
                    Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (destination) {
                            "Today" -> MetriaTodayScreen(
                                vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                cycleContext = vaultStatus.cycleContext,
                                onEditMeal = { editMeal = it },
                                onEditNutritionTargets = { editNutritionTargets = true },
                                onOpenProgress = { destination = "History" },
                            )
                            "History" -> MetriaProgressScreen(
                                entries = vaultStatus.entries,
                                meals = vaultStatus.meals,
                                unit = vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                onEditWeight = { editEntry = it },
                                onEditMeal = { editMeal = it },
                                onEditNutritionTargets = { editNutritionTargets = true },
                            )
                            "Assistant" -> AssistantScreen(
                                modelStatus = modelStatus,
                                showLoadingPreview = false,
                                initialImagePurpose = assistantImagePurpose,
                                onInitialImagePurposeHandled = { assistantImagePurpose = null },
                                entries = vaultStatus.entries,
                                meals = vaultStatus.meals,
                                personalDishes = vaultStatus.personalDishes,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                cycleContext = vaultStatus.cycleContext,
                                downloadAssistant = { modelStatus = activity.downloadLocalAssistant() },
                                cancelAssistantDownload = { modelStatus = activity.cancelLocalAssistantDownload() },
                                openWeightDraft = { draft -> voiceDraft = draft; addSheet = true },
                                askMeal = { text -> modelStatus = activity.askLocalMealAssistant(text) },
                                askCoach = { question, snapshot -> modelStatus = activity.askPrivateCoach(question, snapshot) },
                                understandCoach = { question -> modelStatus = activity.understandPrivateCoach(question) },
                                understandCoachAudio = { audio -> modelStatus = activity.understandPrivateCoachAudio(audio) },
                                cancelAssistant = { modelStatus = activity.cancelPrivateAssistant() },
                                saveWeightDirect = { kilograms, date ->
                                    val entry = WeightEntry(kilograms = kilograms, recordedAt = date)
                                    status = vault.addWeightEntry(entry)
                                    if (status is EncryptedVault.Status.Unlocked) entry else null
                                },
                                editSavedWeight = { editEntry = it },
                                undoSavedWeight = { entry -> status = vault.deleteWeight(entry.id) },
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
                            else -> MetriaPrivacySettingsScreen(
                                unit = vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                modelStatus = modelStatus,
                                analysisAudits = vaultStatus.analysisAudits,
                                cycleContext = vaultStatus.cycleContext,
                                personalDishes = vaultStatus.personalDishes,
                                archiveMessage = archiveMessage,
                                onChangeUnit = { status = vault.setDisplayUnit(if (vaultStatus.displayUnit == "kg") "lb" else "kg") },
                                onEditNutritionTargets = { editNutritionTargets = true },
                                onEditCycleContext = { editCycleContext = true },
                                onDeletePersonalDish = { dish ->
                                    status = vault.deletePersonalDish(dish.id)
                                    (status as? EncryptedVault.Status.Failed)?.message
                                },
                                onExportArchive = { passphrase ->
                                    exportPassphrase = passphrase
                                    archiveMessage = null
                                    activity.beginExternalArchiveFlow()
                                    archiveExportLauncher.launch("metria-recovery-${LocalDate.now()}.metria")
                                },
                                onImportArchive = {
                                    archiveMessage = null
                                    activity.beginExternalArchiveFlow()
                                    archiveImportLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                                },
                                onLock = { activity.lockVault() },
                            )
                        }
                    }
                }
                if (quickAssistantMenuOpen) MetriaActionSheet(
                    onDismiss = { quickAssistantMenuOpen = false },
                    onSpeak = {
                        quickAssistantMenuOpen = false
                        quickAssistantStartVoice = true
                        quickAssistantOpen = true
                    },
                    onChat = {
                        quickAssistantMenuOpen = false
                        quickAssistantStartVoice = false
                        quickAssistantOpen = true
                    },
                    onAnalyseMeal = {
                        quickAssistantMenuOpen = false
                        assistantImagePurpose = "meal"
                        destination = "Assistant"
                    },
                    onScanLabel = {
                        quickAssistantMenuOpen = false
                        assistantImagePurpose = "label"
                        destination = "Assistant"
                    },
                    onAddWeight = {
                        quickAssistantMenuOpen = false
                        voiceDraft = null
                        addSheet = true
                    },
                )
                if (quickAssistantOpen) {
                    ModalBottomSheet(onDismissRequest = {
                        quickAssistantOpen = false
                        quickAssistantStartVoice = false
                        quickAssistantListening = false
                    }) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 28.dp)) {
                            PrivateAssistantChat(
                                modelStatus = modelStatus,
                                entries = vaultStatus.entries,
                                meals = vaultStatus.meals,
                                personalDishes = vaultStatus.personalDishes,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                cycleContext = vaultStatus.cycleContext,
                                onBack = { quickAssistantOpen = false },
                                onVoiceListeningChanged = { quickAssistantListening = it },
                                startVoiceImmediately = quickAssistantStartVoice,
                                onVoiceStartHandled = { quickAssistantStartVoice = false },
                                onWeightDraft = { draft -> voiceDraft = draft; addSheet = true; quickAssistantOpen = false },
                                askMeal = { text -> modelStatus = activity.askLocalMealAssistant(text) },
                                askCoach = { question, snapshot -> modelStatus = activity.askPrivateCoach(question, snapshot) },
                                understandCoach = { question -> modelStatus = activity.understandPrivateCoach(question) },
                                understandCoachAudio = { audio -> modelStatus = activity.understandPrivateCoachAudio(audio) },
                                cancelAssistant = { modelStatus = activity.cancelPrivateAssistant() },
                                saveWeightDirect = { kilograms, date ->
                                    val entry = WeightEntry(kilograms = kilograms, recordedAt = date)
                                    status = vault.addWeightEntry(entry)
                                    if (status is EncryptedVault.Status.Unlocked) entry else null
                                },
                                editSavedWeight = { editEntry = it; quickAssistantOpen = false },
                                undoSavedWeight = { entry -> status = vault.deleteWeight(entry.id) },
                                reviewMeal = { proposal -> mealProposalForConfirmation = proposal; quickAssistantOpen = false },
                            )
                        }
                    }
                }
                if (addSheet || editEntry != null) WeightBottomSheet(
                    entry = editEntry,
                    voiceDraft = voiceDraft,
                    onDismiss = { addSheet = false; editEntry = null; voiceDraft = null },
                    onDelete = editEntry?.let { entry ->
                        {
                            deleteEntry = entry
                            editEntry = null
                            voiceDraft = null
                        }
                    },
                ) { kilograms, note, date ->
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
                mealProposalForConfirmation?.let { proposal -> MetriaMealReviewSheet(
                    proposal = proposal,
                    catalog = foodCatalog,
                    corrections = vaultStatus.foodCorrections,
                    referenceThumbnailBase64 = modelStatus.referenceThumbnailBase64,
                    analysisAuditId = modelStatus.latestImageAudit?.id,
                    savedMeals = vaultStatus.meals,
                    savedPersonalDishes = vaultStatus.personalDishes,
                    findPersonalDishMatches = { query -> PersonalDishMatcher.search(query, vaultStatus.personalDishes) },
                    onDismiss = { mealProposalForConfirmation = null },
                    onSave = { meal, corrections, personalDishes ->
                        status = vault.saveReviewedMeal(meal, corrections, personalDishes)
                        if (status is EncryptedVault.Status.Unlocked) {
                            mealProposalForConfirmation = null
                            modelStatus = activity.clearLocalMealDraft()
                            null
                        } else {
                            (status as? EncryptedVault.Status.Failed)?.message
                                ?: "Unlock your private vault and try saving again."
                        }
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
                if (editNutritionTargets) NutritionTargetsSheet(
                    targets = vaultStatus.nutritionTargets,
                    onDismiss = { editNutritionTargets = false },
                    onSave = { targets -> status = vault.setNutritionTargets(targets); editNutritionTargets = false },
                )
                if (editCycleContext) CycleContextSheet(
                    context = vaultStatus.cycleContext,
                    onDismiss = { editCycleContext = false },
                    onSave = { context -> status = vault.setCycleContext(context); editCycleContext = false },
                )
                if (restoreSource != null && archivePreview == null) ArchivePassphraseSheet(
                    title = "Unlock recovery archive",
                    detail = "Enter the passphrase used when this archive was created. Metria will check it before changing anything.",
                    confirmLabel = "Check archive",
                    onDismiss = { restoreSource = null },
                    onConfirm = { passphrase ->
                        val source = restoreSource ?: return@ArchivePassphraseSheet
                        val result = activity.inspectRecoveryArchive(source, passphrase.concatToString())
                        passphrase.fill('\u0000')
                        result.fold(
                            onSuccess = { archivePreview = it; restoreSource = null },
                            onFailure = { archiveMessage = "Archive could not be opened: ${it.message ?: "wrong passphrase or damaged file"}"; restoreSource = null },
                        )
                    },
                )
                archivePreview?.let { preview -> AlertDialog(
                    onDismissRequest = { archivePreview = null },
                    title = { Text("Replace this vault?") },
                    text = { Text("The archive contains ${preview.weights} weight entries and ${preview.meals} meals. This replaces the current records on this phone. Model files, chat, diagnostics, and device keys are not restored.") },
                    confirmButton = { Button(onClick = {
                        val result = activity.confirmRecoveryRestore()
                        archivePreview = null
                        status = vault.status()
                        archiveMessage = result.fold(
                            onSuccess = { "Recovery completed locally: ${it.weights} weights and ${it.meals} meals restored." },
                            onFailure = { "Recovery did not change your vault: ${it.message ?: "unknown error"}" },
                        )
                    }) { Text("Replace and restore") } },
                    dismissButton = { Button(onClick = { archivePreview = null }) { Text("Keep current vault") } },
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
private fun FirstRunOnboarding(onComplete: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple("Your health data stays with you", "Metria has no account and no default cloud sync. Your entries are encrypted on this phone.", "Private by default"),
        Triple("You stay in control", "Your fingerprint unlocks the vault. Create a recovery archive later if you want a way back after device loss or a biometric change.", "Protected on this phone"),
        Triple("Your private assistant powers Metria", "Metria installs its assistant on your phone during setup. It can understand voice or text, draft actions, and guide you—while you always confirm important changes.", "Private intelligence, on your phone"),
    )
    val current = pages[page]
    Surface(color = MetriaCanvas, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("METRIA", color = Plum, style = MaterialTheme.typography.labelLarge)
                Text("${page + 1} of ${pages.size}", color = Color(0xFF655D6E), style = MaterialTheme.typography.labelMedium)
                Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(current.third, color = Plum, style = MaterialTheme.typography.labelLarge)
                        Text(current.first, style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text(current.second, color = Color(0xFF4E4655), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { index ->
                        Surface(
                            color = if (index == page) Plum else Color(0xFFE0D8E4),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(6.dp),
                        ) {}
                    }
                }
                Button(onClick = { if (page == pages.lastIndex) onComplete() else page += 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (page == pages.lastIndex) "Set up my private assistant" else "Continue")
                }
                if (page > 0) Button(onClick = { page -= 1 }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    }
}

@Composable
private fun AssistantSetupGate(
    modelStatus: ModelStatus,
    download: () -> Unit,
    cancelDownload: () -> Unit,
    continueToApp: () -> Unit,
) {
    Surface(color = MetriaCanvas, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("METRIA", color = Plum, style = MaterialTheme.typography.labelLarge)
            Text("Set up your private assistant", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 20.dp))
            when (modelStatus.state) {
                "absent", "removed", "failed" -> {
                    Text("Metria needs its on-device assistant before you begin. It is about 2.6 GB, remains in this phone’s private storage, and after setup it can work offline.", modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
                    if (modelStatus.state == "failed") Text(modelStatus.reason, color = Color(0xFFB3261E), modifier = Modifier.padding(bottom = 12.dp))
                    Button(onClick = download, modifier = Modifier.fillMaxWidth()) { Text(if (modelStatus.state == "failed") "Try download again" else "Download private assistant") }
                    Text("The download contacts only the selected model distributor. It does not include your entries, photos, prompts, or a vault identifier.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                "downloading" -> {
                    val percent = if (modelStatus.expectedBytes > 0) (modelStatus.downloadedBytes * 100 / modelStatus.expectedBytes).coerceIn(0, 100) else 0
                    Text("Downloading · $percent%", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp))
                    Text("${modelStatus.downloadedBytes / 1_000_000} MB of ${modelStatus.expectedBytes / 1_000_000} MB", color = Color(0xFF655D6E), modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                    Button(onClick = cancelDownload, modifier = Modifier.fillMaxWidth()) { Text("Cancel download") }
                }
                "ready", "initializing", "loaded" -> {
                    Text("Your private assistant is ready on this phone. Next, protect your records with biometrics.", modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
                    Button(onClick = continueToApp, modifier = Modifier.fillMaxWidth()) { Text("Continue with Metria") }
                }
                else -> Text("Checking your private assistant…", modifier = Modifier.padding(top = 16.dp))
            }
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
    initialImagePurpose: String?,
    onInitialImagePurposeHandled: () -> Unit,
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    personalDishes: List<PersonalDish>,
    nutritionTargets: NutritionTargets?,
    cycleContext: CycleContext,
    downloadAssistant: () -> Unit,
    cancelAssistantDownload: () -> Unit,
    openWeightDraft: (VoiceWeightDraft) -> Unit,
    askMeal: (String) -> Unit,
    askCoach: (String, PrivateCoachSnapshot) -> Unit,
    understandCoach: (String) -> Unit,
    understandCoachAudio: (java.io.File) -> Unit,
    cancelAssistant: () -> Unit,
    saveWeightDirect: (Double, Instant) -> WeightEntry?,
    editSavedWeight: (WeightEntry) -> Unit,
    undoSavedWeight: (WeightEntry) -> Unit,
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
    var chatOpen by remember { mutableStateOf(false) }
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
    LaunchedEffect(initialImagePurpose, modelStatus.state) {
        val purpose = initialImagePurpose ?: return@LaunchedEffect
        if (modelStatus.state == "loaded") {
            if (beginImageFlow().imageState != "cooling_down") sourceChoice = purpose
            onInitialImagePurposeHandled()
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
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "reading_image" -> "Looking for the clearest foods." to "This may take a moment while Metria reads the photo."
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "identifying_foods" -> "Identifying the main foods." to "Metria is separating the visible ingredients into your draft."
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "estimating_portions" -> "Estimating portions and preparation." to "You will be able to adjust every estimate before saving."
        modelStatus.imageState == "analyzing" -> "Checking your editable draft." to "Metria is making sure the review is ready to show."
        modelStatus.imageState == "reading_label" -> "Metria is reading your label." to "Text is read locally and must be reviewed."
        else -> null
    }
    if (chatOpen) {
        PrivateAssistantChat(
            modelStatus = modelStatus, entries = entries, meals = meals, personalDishes = personalDishes, nutritionTargets = nutritionTargets, cycleContext = cycleContext,
            onBack = { chatOpen = false }, onVoiceListeningChanged = {}, onWeightDraft = openWeightDraft, askMeal = askMeal,
            askCoach = askCoach, understandCoach = understandCoach, cancelAssistant = cancelAssistant, reviewMeal = review,
            understandCoachAudio = understandCoachAudio,
            saveWeightDirect = saveWeightDirect, editSavedWeight = editSavedWeight,
            undoSavedWeight = undoSavedWeight,
        )
        return
    }
    if (loading != null) {
        AppleByteLoading(loading.first, loading.second)
        return
    }
    if (modelStatus.imageState == "cooling_down") {
        CoolingDownLoading(coolingSecondsLeft)
        return
    }
    if (chatOpen) {
        PrivateAssistantChat(
            modelStatus = modelStatus,
            entries = entries,
            meals = meals,
            personalDishes = personalDishes,
            nutritionTargets = nutritionTargets,
            cycleContext = cycleContext,
            onBack = { chatOpen = false },
            onVoiceListeningChanged = {},
            onWeightDraft = openWeightDraft,
            askMeal = askMeal,
            askCoach = askCoach,
            understandCoach = understandCoach,
            understandCoachAudio = understandCoachAudio,
            cancelAssistant = cancelAssistant,
            saveWeightDirect = saveWeightDirect,
            editSavedWeight = editSavedWeight,
            undoSavedWeight = undoSavedWeight,
            reviewMeal = review,
        )
        return
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (modelStatus.state != "loaded") when (modelStatus.state) {
                "absent", "removed" -> {
                    Text("Add Metria's private assistant", style = MaterialTheme.typography.titleLarge)
                    Text("The model is optional. It is about 2.6 GB and stays on this phone; manual tracking already works without it.", color = Color(0xFF655D6E))
                    Button(onClick = downloadAssistant, modifier = Modifier.fillMaxWidth()) { Text("Download private assistant") }
                }
                "downloading" -> {
                    val percent = if (modelStatus.expectedBytes > 0) (modelStatus.downloadedBytes * 100 / modelStatus.expectedBytes).coerceIn(0, 100) else 0
                    Text("Downloading private assistant · $percent%", style = MaterialTheme.typography.titleMedium)
                    Text("${modelStatus.downloadedBytes / 1_000_000} MB of ${modelStatus.expectedBytes / 1_000_000} MB. You can cancel at any time.", color = Color(0xFF655D6E))
                    Button(onClick = cancelAssistantDownload, modifier = Modifier.fillMaxWidth()) { Text("Cancel download") }
                }
                "failed" -> {
                    Text(modelStatus.reason, color = Color(0xFFB3261E))
                    Button(onClick = downloadAssistant, modifier = Modifier.fillMaxWidth()) { Text("Try download again") }
                }
                "loaded" -> Text("Ready on this phone. Estimates can be wrong—review every assumption.", color = Sage)
                else -> Text("The optional local model is ${modelStatus.state}. This developer build needs a downloaded model before chat can run.")
            }
            if (modelStatus.state == "loaded") {
                Button(onClick = { chatOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Chat with Metria") }
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
                    listOf(item.description, item.preparation).filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), color = Color(0xFF655D6E))
                    }
                }
                proposal.assumptions.forEach { Text("Assumption: $it") }
                proposal.questions.forEach { Text("Question: $it") }
                Text("This fast draft lists the strongest visible foods. Add or edit anything else before saving.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                Text("Nothing has been saved. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
            }
        }
    }
    modelStatus.proposalDiagnostic?.let { Text(modelStatus.imageReason, color = Color(0xFFB3261E)) }
    if (BuildConfig.DEBUG) {
        Card(colors = CardDefaults.cardColors(containerColor = PlumSoft), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Development runtime", style = MaterialTheme.typography.titleSmall, color = Plum)
                Text(modelStatus.backendSummary, color = Ink)
                Text(modelStatus.diagnostics, color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
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
                    listOf(item.description, item.preparation).filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), color = Color(0xFF655D6E))
                    }
                }
                proposal.assumptions.forEach { Text("Assumption: $it") }
                proposal.questions.forEach { Text("Question: $it") }
                Text("The temporary image was discarded. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
            }
        }
    }
}

/** UX decision: facts returned by a local tool remain visible, but secondary to Metria's answer. */
private data class PrivateChatMessage(val text: String, val fromMember: Boolean, val localEvidence: Boolean = false)

@Composable
private fun TypingAssistantText() {
    val transition = rememberInfiniteTransition(label = "metria-chat-typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "metria-chat-dots",
    )
    Text("Metria is thinking${".".repeat(phase.toInt() + 1)}", color = Color(0xFF4E4655))
}

@Composable
private fun PrivateAssistantChat(
    modelStatus: ModelStatus,
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    personalDishes: List<PersonalDish>,
    nutritionTargets: NutritionTargets?,
    cycleContext: CycleContext,
    onBack: () -> Unit,
    onVoiceListeningChanged: (Boolean) -> Unit,
    startVoiceImmediately: Boolean = false,
    onVoiceStartHandled: () -> Unit = {},
    onWeightDraft: (VoiceWeightDraft) -> Unit,
    askMeal: (String) -> Unit,
    askCoach: (String, PrivateCoachSnapshot) -> Unit,
    understandCoach: (String) -> Unit,
    understandCoachAudio: (java.io.File) -> Unit,
    cancelAssistant: () -> Unit,
    saveWeightDirect: (Double, Instant) -> WeightEntry?,
    editSavedWeight: (WeightEntry) -> Unit,
    undoSavedWeight: (WeightEntry) -> Unit,
    reviewMeal: (MealProposal) -> Unit,
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var submittedQuestion by remember { mutableStateOf("") }
    var handledTool by remember { mutableStateOf<PrivateCoachToolCall?>(null) }
    var handledCoachReply by remember { mutableStateOf<String?>(null) }
    var savedWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var recordingVoice by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(listOf(PrivateChatMessage("Ask about a meal, add a weight, or ask what you logged today. Nothing is saved without your review.", false))) }
    val voiceRobotTransition = rememberInfiniteTransition(label = "metria-sheet-listening")
    val voiceRobotScale by voiceRobotTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(680, easing = LinearEasing)),
        label = "metria-sheet-robot-pulse",
    )
    val today = LocalDate.now()
    val todayMeals = remember(meals, today) { meals.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today } }
    val todaySummary = remember(meals, today) { MealNutritionSummaries.between(meals, today, today) }
    val snapshot = remember(entries, meals, nutritionTargets, today) { PrivateCoachSnapshot.from(entries, meals, nutritionTargets, today) }
    fun setVoiceRecording(value: Boolean) {
        recordingVoice = value
        onVoiceListeningChanged(value)
    }
    fun submitVoiceClip(clip: GemmaAudioRecorder.Clip) {
        setVoiceRecording(false)
        if (!clip.detectedSpeech) {
            clip.file.delete()
            voiceMessage = "Metria could not hear speech. Try again or type your request."
            return
        }
        messages = messages + PrivateChatMessage("Voice request received locally.", true)
        submittedQuestion = "Voice request"
        understandCoachAudio(clip.file)
    }
    val audioRecorder = remember(context) { GemmaAudioRecorder(context) }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            audioRecorder.start(::submitVoiceClip).onSuccess { setVoiceRecording(true) }.onFailure { voiceMessage = it.message ?: "Microphone recording could not start." }
        } else voiceMessage = "Microphone permission was not granted. You can type your request instead."
    }
    fun startVoiceCapture() {
        voiceMessage = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioRecorder.start(::submitVoiceClip).onSuccess { setVoiceRecording(true) }
                .onFailure { voiceMessage = it.message ?: "Microphone recording could not start." }
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(audioRecorder) { onDispose { audioRecorder.cancel(); onVoiceListeningChanged(false) } }
    LaunchedEffect(startVoiceImmediately, modelStatus.state) {
        if (startVoiceImmediately) {
            when (modelStatus.state) {
                "loaded" -> {
                    onVoiceStartHandled()
                    startVoiceCapture()
                }
                "failed", "absent", "removed" -> {
                    onVoiceStartHandled()
                    voiceMessage = "Metria could not get ready for voice. Open Chat to check the local assistant."
                }
            }
        }
    }
    LaunchedEffect(modelStatus.coachToolCall, submittedQuestion) {
        val call = modelStatus.coachToolCall ?: return@LaunchedEffect
        if (call == handledTool) return@LaunchedEffect
        handledTool = call
        call.spokenText?.let { spoken ->
            submittedQuestion = spoken
            messages = messages + PrivateChatMessage("Voice: $spoken", true)
        }
        val action = AssistantActionDispatcher.resolve(call)
        if (action is AssistantAction.QueryWeightHistory && call.spokenText?.let(::soundsLikeMealHistoryQuestion) == true) {
            messages = messages + PrivateChatMessage("I heard a meal-history question, but could not safely match the action. Please try again or type it.", false)
            return@LaunchedEffect
        }
        when (action) {
            is AssistantAction.QueryNutrition -> {
                val scopedSnapshot = snapshot
                val label = if (action.period == "week") "Last 7 days" else "Today"
                messages = messages + PrivateChatMessage("$label. ${scopedSnapshot.goalProgressFacts(action.period)}", false, localEvidence = true)
                val language = coachReplyLanguage(submittedQuestion)
                askCoach("""MEMBER_LANGUAGE: $language
                    REQUESTED_PERIOD: $label
                    Respond in MEMBER_LANGUAGE. Use only the selected-period week fields and their week goals.
                    Never mention today or daily goals. Write recognition, factual comparison, and a gentle next focus.""".trimIndent(), scopedSnapshot)
            }
            is AssistantAction.QueryWeightHistory -> {
                val oldest = today.minusDays(action.fromDaysAgo.toLong())
                val newest = today.minusDays(action.toDaysAgo.toLong())
                val ordered = entries.filter { date -> date.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in oldest..newest }.sortedBy(WeightEntry::recordedAt)
                val first = ordered.firstOrNull()
                val latest = ordered.lastOrNull()
                val trend = if (first == null || latest == null) "No weight entries were saved from $oldest to $newest." else if (first.id == latest.id) "One weight entry from $oldest to $newest: ${"%.1f".format(first.kilograms)} kg." else {
                    val days = java.time.Duration.between(first.recordedAt, latest.recordedAt).toDays().coerceAtLeast(1)
                    val change = latest.kilograms - first.kilograms
                    "${ordered.size} weight entries from ${first.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()} to ${latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}: ${"%.1f".format(first.kilograms)} kg to ${"%.1f".format(latest.kilograms)} kg (${"%+.1f".format(change)} kg over $days days)."
                }
                messages = messages + PrivateChatMessage(trend, false, localEvidence = true)
                askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion)}
                    MEMBER_QUESTION: $submittedQuestion
                    [LOCAL_FACTS]
                    $trend
                    [/LOCAL_FACTS]
                    Explain only these facts in three short supportive sentences. Do not mention nutrition goals or give medical advice.""".trimIndent(), snapshot)
            }
            is AssistantAction.QueryMealHistory -> {
                val oldest = today.minusDays(action.fromDaysAgo.toLong())
                val newest = today.minusDays(action.toDaysAgo.toLong())
                val saved = meals.filter {
                    it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in oldest..newest &&
                        (action.mealSlot == null || MealSlots.matches(it, action.mealSlot))
                }
                val typeLabel = action.mealSlot?.let(MealSlots::label)?.lowercase()
                val facts = if (saved.isEmpty()) {
                    "No ${typeLabel ?: ""} meals were saved from $oldest to $newest.".replace("No  meals", "No meals")
                } else {
                    "${typeLabel?.replaceFirstChar(Char::uppercase) ?: "Meals"} from $oldest to $newest: ${saved.joinToString { meal ->
                        val local = meal.recordedAt.atZone(ZoneId.systemDefault())
                        val timing = when {
                            meal.mealSlot != null -> MealSlots.label(meal.mealSlot).lowercase()
                            meal.recordedTimeKnown -> "around ${MealSlots.label(MealSlots.suggestedFor(local.toLocalTime())).lowercase()} (${local.toLocalTime().withSecond(0).withNano(0)})"
                            else -> "time not recorded"
                        }
                        "${local.toLocalDate()}: ${MealIdentity.displayTitle(meal)} ($timing)"
                    }}."
                }
                messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion)}
                    MEMBER_QUESTION: $submittedQuestion
                    [LOCAL_FACTS]
                    $facts
                    [/LOCAL_FACTS]
                    Answer only from these local facts in no more than three short sentences.""".trimIndent(), snapshot)
            }
            is AssistantAction.SearchSavedDishes -> {
                val matches = PersonalDishMatcher.search(PersonalDishQuery(name = action.query), personalDishes)
                val facts = if (matches.isEmpty()) {
                    "No saved private dish matched '${action.query}'."
                } else {
                    "Saved private dish matches for '${action.query}': ${matches.joinToString { match ->
                        "${match.dish.name} (${match.dish.items.joinToString { it.name }}; confirmed ${match.dish.confirmationCount} ${if (match.dish.confirmationCount == 1) "time" else "times"})"
                    }}."
                }
                messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion)}
                    MEMBER_QUESTION: $submittedQuestion
                    [LOCAL_FACTS]
                    $facts
                    [/LOCAL_FACTS]
                    Answer only from these bounded saved-dish results. Say that the member can review or remove saved dishes in Privacy when useful.""".trimIndent(), snapshot)
            }
            is AssistantAction.DraftWeight -> {
                savedWeight = saveWeightDirect(action.kilograms, Instant.now())
                messages = messages + PrivateChatMessage(if (savedWeight != null) "${"%.1f".format(action.kilograms)} kg saved locally." else "Weight could not be saved. Try again.", false)
            }
            AssistantAction.DraftMeal -> askMeal(submittedQuestion)
            is AssistantAction.Respond -> messages = messages + PrivateChatMessage(action.message, false)
            AssistantAction.Clarify -> messages = messages + PrivateChatMessage("I can help with meals, weights, and your progress. Try asking in another way.", false)
        }
    }
    LaunchedEffect(modelStatus.coachReply) {
        val reply = modelStatus.coachReply ?: return@LaunchedEffect
        if (reply != handledCoachReply) {
            handledCoachReply = reply
            messages = messages + PrivateChatMessage(reply, false)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Private assistant", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onBack) { Text("Back") }
        }
        Text("This conversation stays only while Metria is open and unlocked.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
        messages.forEach { message ->
            Card(colors = CardDefaults.cardColors(containerColor = when {
                message.fromMember -> PlumSoft
                message.localEvidence -> Color(0xFFF2F5F3)
                else -> Color.White
            })) {
                if (message.fromMember) Text(message.text, modifier = Modifier.padding(14.dp), color = Ink)
                else if (message.localEvidence) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("LOCAL RECORDS", color = Sage, style = MaterialTheme.typography.labelSmall)
                        SafeMarkdownText(message.text, Color(0xFF3F4A45))
                    }
                } else androidx.compose.foundation.layout.Box(Modifier.padding(14.dp)) { SafeMarkdownText(message.text, Color(0xFF4E4655)) }
            }
        }
        if (modelStatus.state == "thinking" && modelStatus.assistantOperation?.startsWith("coach") == true) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modelStatus.coachStreamingReply?.let { SafeMarkdownText(it, Color(0xFF4E4655)) } ?: TypingAssistantText()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (modelStatus.coachStreamingReply == null) "Metria is considering your question" else "Metria is finishing this reply", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = cancelAssistant) { Text("Cancel") }
                    }
                }
            }
        }
        savedWeight?.let { entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { editSavedWeight(entry) }, modifier = Modifier.weight(1f)) { Text("Edit saved weight") }
                Button(onClick = {
                    undoSavedWeight(entry)
                    savedWeight = null
                    messages = messages + PrivateChatMessage("${"%.1f".format(entry.kilograms)} kg entry removed.", false)
                }, modifier = Modifier.weight(1f)) { Text("Undo") }
            }
        }
        if (modelStatus.state == "thinking" && modelStatus.assistantOperation?.startsWith("coach") != true) {
            Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
                Text("Metria is preparing an editable meal draft locally…", modifier = Modifier.padding(14.dp), color = Plum)
            }
        }
        modelStatus.assistantDiagnostic?.let { Text(it, color = Color(0xFFB3261E)) }
        modelStatus.coachDiagnostic?.let { Text(it, color = Color(0xFFB3261E)) }
        modelStatus.assistantProposal?.let { proposal ->
            Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meal draft ready", style = MaterialTheme.typography.titleMedium)
                    Text(proposal.items.joinToString { it.name }, color = Color(0xFF655D6E))
                    Text("Nothing has been saved.", color = Plum, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { reviewMeal(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review meal draft") }
                }
            }
        }
        if (startVoiceImmediately && !recordingVoice) Text("Getting Metria ready…", color = Plum, style = MaterialTheme.typography.bodySmall)
        if (recordingVoice) Text("Listening locally… tap the robot to stop early.", color = Plum, style = MaterialTheme.typography.bodySmall)
        voiceMessage?.let { Text(it, color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, label = { Text("Message") }, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                if (recordingVoice) {
                    setVoiceRecording(false)
                    audioRecorder.stop()?.let(::submitVoiceClip) ?: run { voiceMessage = "No local voice clip was captured. Try again." }
                } else startVoiceCapture()
            }, enabled = modelStatus.state == "loaded") {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (recordingVoice) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Sage.copy(alpha = 0.18f), radius = size.minDimension * 0.44f * voiceRobotScale)
                        }
                    }
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = if (recordingVoice) "Metria is listening; tap to stop" else "Speak to Metria",
                        tint = if (recordingVoice) Sage else Plum,
                        modifier = Modifier.graphicsLayer {
                            val scale = if (recordingVoice) voiceRobotScale else 1f
                            scaleX = scale; scaleY = scale
                        },
                    )
                }
            }
        }
        Button(onClick = {
            val text = input.trim()
            if (text.isBlank()) return@Button
            messages = messages + PrivateChatMessage(text, true)
            input = ""
            submittedQuestion = text
            if (isWeightTrendQuestion(text)) {
                val trend = weightTrendFacts(entries, cycleContext)
                messages = messages + PrivateChatMessage(trend, false)
                askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(text)}
                    MEMBER_QUESTION: $text
                    [LOCAL_FACTS]
                    $trend
                    [/LOCAL_FACTS]
                    Explain only these facts in three short supportive sentences. Do not mention nutrition goals or give medical advice.""".trimIndent(), snapshot)
                return@Button
            }
            understandCoach(text)
            return@Button
            val kilograms = parseChatWeight(text)
            when {
                kilograms != null -> {
                messages = messages + PrivateChatMessage("Weight draft: ${"%.1f".format(kilograms)} kg. Review it before saving.", false)
                onWeightDraft(VoiceWeightDraft(kilograms, Instant.now()))
            }
                PrivateCoachQuestions.asksYesterdayWeight(text) -> {
                val entry = entries.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today.minusDays(1) }.maxByOrNull(WeightEntry::recordedAt)
                messages = messages + PrivateChatMessage(entry?.let { "Yesterday you recorded ${"%.1f".format(it.kilograms)} kg. Read locally from your encrypted vault." } ?: "You did not record a weight yesterday.", false)
            }
                PrivateCoachQuestions.asksTodaySummary(text) -> {
                val calories = (todaySummary.caloriesKcal ?: 0.0) + (todaySummary.partialCaloriesKcal ?: 0.0)
                val protein = (todaySummary.proteinGrams ?: 0.0) + (todaySummary.partialProteinGrams ?: 0.0)
                messages = messages + PrivateChatMessage(
                    if (todayMeals.isEmpty()) "You have not recorded a meal today."
                    else "Today: ${todayMeals.size} meals · ${"%.0f".format(calories)} kcal · ${"%.0f".format(protein)} g protein.",
                    false,
                )
            }
                PrivateCoachQuestions.asksProgress(text) -> {
                messages = messages + PrivateChatMessage("Metria is using the on-device model with a minimal private progress summary.", false)
                askCoach(text, snapshot)
            }
                else -> {
                messages = messages + PrivateChatMessage("I’m preparing an editable meal draft locally.", false)
                askMeal(text)
            }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = input.trim().isNotBlank() && modelStatus.state == "loaded") { Text("Send") }
    }
}

private fun parseChatWeight(text: String): Double? {
    val match = Regex("(?i)(?:weigh|weight|peso|pes[oó])?\\s*(\\d{2,3}(?:[.,]\\d+)?)\\s*(?:kg|kilos?|kilograms?|kilogramos?)").find(text) ?: return null
    return match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf(WeightUnits::isValidKilograms)
}

private fun coachReplyLanguage(text: String): String {
    val normalized = text.lowercase()
    return if (listOf("qué", "como", "cómo", "comí", "peso", "semana", "meta", "progreso").any { it in normalized }) "Spanish" else "the member's language"
}

private fun isWeightTrendQuestion(text: String): Boolean = listOf("pérdida de peso", "perdida de peso", "mi peso", "weight loss", "weight trend").any { it in text.lowercase() }

/** A safety backstop for a model action that conflicts with the member-visible transcription. */
private fun soundsLikeMealHistoryQuestion(text: String): Boolean =
    listOf("qué com", "que com", "what did i eat", "meals", "comidas").any { it in text.lowercase() }

private fun weightTrendFacts(entries: List<WeightEntry>, cycleContext: CycleContext = CycleContext()): String {
    val ordered = entries.sortedBy(WeightEntry::recordedAt)
    val first = ordered.firstOrNull(); val latest = ordered.lastOrNull()
    if (first == null || latest == null || first.id == latest.id) return "There are not enough weight entries to describe a trend yet."
    val days = java.time.Duration.between(first.recordedAt, latest.recordedAt).toDays().coerceAtLeast(1)
    val base = "${ordered.size} weight entries from ${first.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()} to ${latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}: ${"%.1f".format(first.kilograms)} kg to ${"%.1f".format(latest.kilograms)} kg (${"%+.1f".format(latest.kilograms - first.kilograms)} kg over $days days)."
    val latestDate = latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    val latestPeriodStart = cycleContext.periodStarts.filter { !it.isAfter(latestDate) }.maxOrNull()
    val cycleNote = latestPeriodStart?.let { start ->
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(start, latestDate)
        if (cycleContext.enabled && daysSinceStart in 0..7) " Cycle context: the latest reading is $daysSinceStart days after a member-logged period start. Short-term fluid variation is possible but cannot be confirmed from these records."
        else ""
    }.orEmpty()
    return base + cycleNote
}

private fun isDirectChatRequest(text: String) = parseChatWeight(text) != null || PrivateCoachQuestions.asksYesterdayWeight(text) || PrivateCoachQuestions.asksTodaySummary(text)

@Composable
private fun TodayScreen(
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    unit: String,
    nutritionTargets: NutritionTargets?,
    cycleContext: CycleContext,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    onEditNutritionTargets: () -> Unit,
) {
    val latest = entries.maxByOrNull(WeightEntry::recordedAt)
    val todayWeights = entries.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }.sortedByDescending(WeightEntry::recordedAt)
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
    CycleAwareWeightNote(entries, cycleContext)
    if (todayWeights.isNotEmpty()) {
        Text("Today’s weight entries", style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todayWeights.forEach { entry ->
                    Text("${formatWeight(entry.kilograms, unit)} · ${entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)}")
                }
            }
        }
    }
    }
    if (selectedTab == 1) {
        NutritionOverviewCard(todayNutrition, weekNutrition, meals, nutritionTargets, onEditNutritionTargets, onEditMeal)
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
private fun NutritionOverviewCard(
    today: MealNutritionSummary,
    week: MealNutritionSummary,
    meals: List<MealEntry>,
    targets: NutritionTargets?,
    editTargets: () -> Unit,
    onEditMeal: (MealEntry) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = editTargets, modifier = Modifier.fillMaxWidth()) {
                Text(if (targets == null) "Set goals" else "Edit goals")
            }
            if (targets == null) {
                Text("Optional private daily targets for your own progress view.", color = Color(0xFF655D6E))
            } else {
                NutritionProgressCard("Calories", today.caloriesKcal, today.partialCaloriesKcal, targets.caloriesKcal, "kcal", emphasis = true)
                listOfNotNull(
                    targets.proteinGrams?.let { NutritionProgress("Protein", today.proteinGrams, today.partialProteinGrams, it, "g") },
                    targets.carbohydrateGrams?.let { NutritionProgress("Carbohydrates", today.carbohydrateGrams, today.partialCarbohydrateGrams, it, "g") },
                    targets.fatGrams?.let { NutritionProgress("Fat", today.fatGrams, today.partialFatGrams, it, "g") },
                ).forEach { progress -> NutritionProgressCard(progress.label, progress.confirmed, progress.partial, progress.target, progress.unit) }
                NutritionEstimateLegend(today.completeNutritionMeals, today.partialNutritionMeals)
            }
            if (today.completeNutritionMeals == 0 && today.partialNutritionMeals == 0) {
                Text("No meals with nutrition values have been recorded today.", color = Color(0xFF655D6E))
            }
            if (week.completeNutritionMeals > 0 || week.partialNutritionMeals > 0) {
                WeeklyNutritionChart(meals, targets, onEditMeal)
            } else {
                Text("Add and confirm meals with complete nutrition to see a 7-day estimate.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WeeklyNutritionChart(meals: List<MealEntry>, targets: NutritionTargets?, onEditMeal: (MealEntry) -> Unit) {
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val daily = days.map { day -> MealNutritionSummaries.between(meals, day, day) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val calorieValues = daily.map { (it.caloriesKcal ?: 0.0) + (it.partialCaloriesKcal ?: 0.0) }
    val scale = maxOf(targets?.caloriesKcal ?: 0.0, calorieValues.maxOrNull() ?: 0.0, 1.0)
    val averageCalories = calorieValues.average()
    val averageProtein = daily.map { (it.proteinGrams ?: 0.0) + (it.partialProteinGrams ?: 0.0) }.average()
    Text("Last 7 days", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        daily.zip(days).forEach { (summary, day) ->
            Column(Modifier.weight(1f).clickable { selectedDay = day }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WeeklyNutritionBar(
                    complete = summary.caloriesKcal ?: 0.0,
                    partial = summary.partialCaloriesKcal ?: 0.0,
                    scale = scale,
                )
                Text(day.dayOfWeek.name.take(1), color = Color(0xFF655D6E), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Text("Average ${"%.0f".format(averageCalories)} kcal · ${"%.0f".format(averageProtein)} g protein per day", color = Plum, style = MaterialTheme.typography.bodyMedium)
    selectedDay?.let { day -> WeeklyDaySheet(
        day = day,
        summary = MealNutritionSummaries.between(meals, day, day),
        meals = meals.filter { it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == day }.sortedByDescending(MealEntry::recordedAt),
        onDismiss = { selectedDay = null },
        onEditMeal = { meal -> selectedDay = null; onEditMeal(meal) },
    ) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyDaySheet(
    day: LocalDate,
    summary: MealNutritionSummary,
    meals: List<MealEntry>,
    onDismiss: () -> Unit,
    onEditMeal: (MealEntry) -> Unit,
) {
    val calories = (summary.caloriesKcal ?: 0.0) + (summary.partialCaloriesKcal ?: 0.0)
    val protein = (summary.proteinGrams ?: 0.0) + (summary.partialProteinGrams ?: 0.0)
    val carbohydrates = (summary.carbohydrateGrams ?: 0.0) + (summary.partialCarbohydrateGrams ?: 0.0)
    val fat = (summary.fatGrams ?: 0.0) + (summary.partialFatGrams ?: 0.0)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(day.toString(), style = MaterialTheme.typography.headlineMedium)
            if (summary.completeNutritionMeals > 0 || summary.partialNutritionMeals > 0) {
                Text("${"%.0f".format(calories)} kcal · ${"%.0f".format(protein)} g protein", color = Plum, style = MaterialTheme.typography.titleMedium)
                Text("${"%.0f".format(carbohydrates)} g carbs · ${"%.0f".format(fat)} g fat", color = Plum)
                NutritionEstimateLegend(summary.completeNutritionMeals, summary.partialNutritionMeals)
            } else {
                Text("No nutrition values saved for this date.", color = Color(0xFF655D6E))
            }
            Text("Meals", style = MaterialTheme.typography.titleMedium)
            if (meals.isEmpty()) Text("No meals recorded for this date.", color = Color(0xFF655D6E))
            meals.forEach { meal -> MealRecordCard(meal, onEditMeal) }
        }
    }
}

@Composable
private fun WeeklyNutritionBar(complete: Double, partial: Double, scale: Double) {
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val completeHeight = (size.height * (complete / scale).toFloat().coerceIn(0f, 1f))
        val totalHeight = (size.height * ((complete + partial) / scale).toFloat().coerceIn(0f, 1f))
        drawRoundRect(Color(0xFFE5DDE9), cornerRadius = CornerRadius(8f, 8f))
        if (totalHeight > 0f) {
            drawRect(Plum.copy(alpha = 0.38f), topLeft = Offset(0f, size.height - totalHeight), size = Size(size.width, totalHeight))
            var lineX = -size.height
            while (lineX < size.width + size.height) {
                drawLine(Color.White.copy(alpha = 0.82f), Offset(lineX, size.height), Offset(lineX + size.height, 0f), strokeWidth = 2f)
                lineX += size.height
            }
        }
        if (completeHeight > 0f) drawRect(Plum, topLeft = Offset(0f, size.height - completeHeight), size = Size(size.width, completeHeight))
    }
}

private data class NutritionProgress(val label: String, val confirmed: Double?, val partial: Double?, val target: Double, val unit: String)

@Composable
private fun NutritionProgressCard(label: String, confirmed: Double?, partial: Double?, target: Double?, unit: String, emphasis: Boolean = false) {
    if (target == null) return
    val confirmedValue = confirmed ?: 0.0
    val partialValue = partial ?: 0.0
    val total = confirmedValue + partialValue
    val progress = (total / target).toFloat().coerceIn(0f, 1f)
    val remaining = (target - total).coerceAtLeast(0.0)
    Card(colors = CardDefaults.cardColors(containerColor = if (emphasis) PlumSoft else Color(0xFFF6F1F8))) {
        Column(Modifier.padding(if (emphasis) 16.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = if (emphasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
                Text("${"%.0f".format(total)} / ${"%.0f".format(target)} $unit", color = Plum)
            }
            SegmentedNutritionProgressBar(confirmedValue, partialValue, target)
            Text(
                if (total <= target) "${"%.0f".format(remaining)} $unit remaining" else "${"%.0f".format(total - target)} $unit over target",
                color = Color(0xFF655D6E),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SegmentedNutritionProgressBar(confirmed: Double, partial: Double, target: Double) {
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val confirmedWidth = (size.width * (confirmed / target).toFloat().coerceIn(0f, 1f))
        val totalWidth = (size.width * ((confirmed + partial) / target).toFloat().coerceIn(0f, 1f))
        drawRoundRect(Color(0xFFE5DDE9), cornerRadius = CornerRadius(size.height / 2, size.height / 2))
        if (confirmedWidth > 0f) drawRoundRect(Plum, size = Size(confirmedWidth, size.height), cornerRadius = CornerRadius(size.height / 2, size.height / 2))
        if (totalWidth > confirmedWidth) {
            drawRect(Plum.copy(alpha = 0.38f), topLeft = Offset(confirmedWidth, 0f), size = Size(totalWidth - confirmedWidth, size.height))
            var lineX = confirmedWidth - size.height
            while (lineX < totalWidth) {
                drawLine(Color.White.copy(alpha = 0.82f), Offset(lineX, size.height), Offset(lineX + size.height, 0f), strokeWidth = 2f)
                lineX += size.height
            }
        }
    }
}

@Composable
private fun NutritionEstimateLegend(completeMeals: Int, partialMeals: Int) {
    if (partialMeals == 0) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("● $completeMeals complete", color = Plum, style = MaterialTheme.typography.bodySmall)
        Text("▧ $partialMeals partial", color = Plum.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NutritionTargetsSheet(targets: NutritionTargets?, onDismiss: () -> Unit, onSave: (NutritionTargets?) -> Unit) {
    var calories by remember(targets) { mutableStateOf(targets?.caloriesKcal?.toString().orEmpty()) }
    var protein by remember(targets) { mutableStateOf(targets?.proteinGrams?.toString().orEmpty()) }
    var carbohydrates by remember(targets) { mutableStateOf(targets?.carbohydrateGrams?.toString().orEmpty()) }
    var fat by remember(targets) { mutableStateOf(targets?.fatGrams?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily nutrition targets", style = MaterialTheme.typography.headlineMedium)
            Text("Optional values you choose for your own progress view. Metria does not recommend targets or give medical advice.", color = Color(0xFF655D6E))
            OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(carbohydrates, { carbohydrates = it }, label = { Text("Carbohydrates (g)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
            Text("Leave every field empty to remove targets.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                fun number(value: String) = value.trim().takeIf(String::isNotEmpty)?.replace(',', '.')?.toDoubleOrNull()
                val raw = listOf(calories, protein, carbohydrates, fat)
                val parsed = raw.map(::number)
                val invalid = raw.zip(parsed).any { (value, parsedValue) -> value.isNotBlank() && parsedValue == null }
                val newTargets = NutritionTargets(parsed[0], parsed[1], parsed[2], parsed[3])
                if (invalid || !NutritionTargetValidation.valid(newTargets)) {
                    error = "Enter positive targets within the supported range, or leave all fields empty."
                } else {
                    onSave(newTargets.takeUnless(NutritionTargetValidation::empty))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save daily targets") }
        }
    }
}

/** Retained only for legacy Today/meal-list helpers while those screens are migrated. */
@Composable
private fun MealRecordCard(meal: MealEntry, onEdit: (MealEntry) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier.fillMaxWidth().clickable { onEdit(meal) }.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            meal.referenceThumbnailBase64?.let { ReferenceThumbnail(it) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(MealIdentity.displayTitle(meal), style = MaterialTheme.typography.titleMedium)
                meal.mealSlot?.let { Text(MealSlots.label(it), color = Plum, style = MaterialTheme.typography.labelMedium) }
                meal.nutrition?.let { Text(nutritionSummary(it), color = Color(0xFF655D6E)) }
                    ?: Text("Nutrition needs review", color = Color(0xFF655D6E))
            }
        }
    }
}

@Composable
private fun ArchivePassphraseSheet(title: String, detail: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(detail)
            OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Archive passphrase") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color(0xFFB3261E)) }
        } },
        confirmButton = { Button(onClick = {
            when {
                passphrase.length < 12 -> error = "Use at least 12 characters."
                confirmation.isNotEmpty() && passphrase != confirmation -> error = "The passphrases do not match."
                else -> onConfirm(passphrase.toCharArray())
            }
        }) { Text(confirmLabel) } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CycleAwareWeightNote(entries: List<WeightEntry>, context: CycleContext) {
    if (!context.enabled || entries.size < 2 || context.periodStarts.isEmpty()) return
    val ordered = entries.sortedBy(WeightEntry::recordedAt)
    val latest = ordered.last()
    val previous = ordered.dropLast(1).last()
    if (latest.kilograms <= previous.kilograms) return
    val latestDate = latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    val periodStart = context.periodStarts.filter { !it.isAfter(latestDate) }.maxOrNull() ?: return
    val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(periodStart, latestDate)
    if (daysSinceStart !in 0..7) return
    Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
        Text("A short-term increase can reflect normal fluid variation. Your private cycle context is one possible factor; follow the trend over several readings.", color = Plum, modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleContextSheet(context: CycleContext, onDismiss: () -> Unit, onSave: (CycleContext) -> Unit) {
    var enabled by remember(context) { mutableStateOf(context.enabled) }
    var starts by remember(context) { mutableStateOf(context.periodStarts.sortedDescending().joinToString("\n")) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Private cycle context", style = MaterialTheme.typography.headlineMedium)
            Text("Optional and encrypted on this phone. This helps Metria phrase short-term weight changes more carefully. It does not diagnose, predict a cycle, change goals, or share data.", color = Color(0xFF655D6E))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Use cycle-aware insights")
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            if (enabled) {
                OutlinedTextField(starts, { starts = it }, label = { Text("Period start dates · YYYY-MM-DD") }, supportingText = { Text("One date per line. You can remove every date or turn this off whenever you want.") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val parsed = starts.lineSequence().map(String::trim).filter(String::isNotEmpty).map { runCatching { LocalDate.parse(it) }.getOrNull() }.toList()
                if (enabled && (parsed.any { it == null } || parsed.size != starts.lineSequence().map(String::trim).count(String::isNotEmpty))) {
                    error = "Use one valid past or current date per line."
                } else {
                    val next = CycleContext(enabled = enabled, periodStarts = parsed.filterNotNull())
                    if (CycleContextValidation.valid(next)) onSave(next) else error = "Use at most 24 unique dates from the last three years."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save privately") }
        }
    }
}

private fun formatWeight(kilograms: Double, unit: String): String = if (unit == "lb") "${"%.1f".format(WeightUnits.kilogramsToPounds(kilograms))} lb" else "${"%.1f".format(kilograms)} kg"

private data class EditableMealItem(val name: String, val description: String, val grams: String, val preparation: String, val groupLabel: String? = null, val role: String = "main_food", val evidence: String = "seen")

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealConfirmationSheet(
    proposal: MealProposal,
    catalog: OfflineFoodCatalog?,
    corrections: List<FoodCorrection>,
    referenceThumbnailBase64: String?,
    analysisAuditId: String?,
    savedMeals: List<MealEntry>,
    onDismiss: () -> Unit,
    onSave: (MealEntry, List<FoodCorrection>, PersonalDish?) -> Unit,
) {
    // M3 recovery: if a model joins independently searchable foods, split only
    // after the local catalog disproves the joined phrase; divide its draft portion.
    val reviewItems = remember(proposal, catalog) {
        proposal.items.flatMap { item ->
            val names = catalog?.let { FoodCatalog.splitUnmatchedJoinedFood(it, item.name) } ?: listOf(item.name)
            if (names.size == 1) listOf(item) else names.map { name ->
                item.copy(
                    name = name,
                    visiblePortion = "part of ${item.visiblePortion}",
                    estimatedGramsRange = GramRange(
                        (item.estimatedGramsRange.min / names.size).coerceAtLeast(1),
                        (item.estimatedGramsRange.max / names.size).coerceAtLeast(1),
                    ),
                    preparation = "",
                )
            }
        }.take(12)
    }
    var items by remember(reviewItems) { mutableStateOf(reviewItems.map { item ->
        EditableMealItem(item.name, item.description, ((item.estimatedGramsRange.min + item.estimatedGramsRange.max) / 2).toString(), item.preparation, item.groupLabel, item.role, item.evidence)
    }) }
    var date by remember(proposal) { mutableStateOf(LocalDate.now().toString()) }
    val captureTime = remember(proposal) { LocalTime.now().withSecond(0).withNano(0) }
    var title by remember(proposal) { mutableStateOf(MealIdentity.titleFor(reviewItems.map { MealEntryItem(it.name, null, null) })) }
    var rememberAsPersonalDish by remember { mutableStateOf(false) }
    var mealSlot by remember { mutableStateOf<String?>(null) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var keepReference by remember(referenceThumbnailBase64) { mutableStateOf(referenceThumbnailBase64 != null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Approved M3 direction: every assistant item has an independently
    // reviewable catalog match. A total is shown only when all items match;
    // otherwise the saved record explicitly has no nutrition estimate.
    var catalogMatches by remember(reviewItems, catalog) {
        mutableStateOf(reviewItems.map { item -> if (FoodCatalog.isAmbiguousModelFood(item.name)) null else catalog?.let { FoodCatalog.suggest(it, item.name, item.description, corrections) } })
    }
    var confirmedPossibleIngredients by remember(reviewItems) { mutableStateOf(emptySet<Int>()) }
    val initialMatchIds = remember(reviewItems, catalog, corrections) { reviewItems.map { item -> catalog?.let { FoodCatalog.suggest(it, item.name, item.description, corrections)?.fdcId } } }
    var catalogSearchIndex by remember { mutableStateOf<Int?>(null) }
    val parsedGrams = items.map { it.grams.replace(',', '.').toIntOrNull()?.takeIf { grams -> grams in 1..2_000 } }
    val perItemNutrition = items.indices.map { index ->
        catalogMatches.getOrNull(index)?.let { food -> parsedGrams[index]?.let { FoodCatalog.calculate(food, it) } }
    }
    val includedIndexes = perItemNutrition.indices.filter { index ->
        val nutrition = perItemNutrition[index]
        (items[index].evidence == "seen" || index in confirmedPossibleIngredients) &&
            nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
            nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
    }
    val catalogTotals = includedIndexes.takeIf { it.isNotEmpty() }?.map { perItemNutrition[it]!! }?.let { values ->
        CatalogNutrition(
            values.sumOf { it!!.caloriesKcal!! },
            values.sumOf { it!!.proteinGrams!! },
            values.sumOf { it!!.carbohydrateGrams!! },
            values.sumOf { it!!.fatGrams!! },
            values.any { it!!.caloriesDerivedFromMacros },
        )
    }
    val isPartialEstimate = catalogTotals != null && includedIndexes.size != items.size
    val draftComparableItems = items.mapIndexed { index, item -> MealEntryItem(
        name = item.name.trim(),
        grams = parsedGrams.getOrNull(index),
        preparation = item.preparation.trim().takeIf(String::isNotEmpty),
        catalogFoodId = catalogMatches.getOrNull(index)?.fdcId.takeIf { index in includedIndexes },
    ) }
    val similarMeal = MealIdentity.similarSavedMeal(draftComparableItems, savedMeals)
    val nonMeasuredIndexes = items.indices.filter { index -> FoodCatalog.isNonMeasuredSeasoning(items[index].name) }
    val itemsNeedingReview = items.indices.filter { index ->
        val nutrition = perItemNutrition.getOrNull(index)
        index !in nonMeasuredIndexes && (catalogMatches.getOrNull(index) == null || parsedGrams.getOrNull(index) == null ||
            nutrition?.caloriesKcal == null || nutrition.proteinGrams == null ||
            nutrition.carbohydrateGrams == null || nutrition.fatGrams == null)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 72.dp).imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Confirm meal", style = MaterialTheme.typography.headlineMedium)
            Text("The assistant identified these foods, then Metria searched its offline catalog. Review every local match and portion before saving.", color = Plum)
            OutlinedTextField(title, { title = it }, label = { Text("Meal name (optional)") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(checked = rememberAsPersonalDish, onCheckedChange = { rememberAsPersonalDish = it })
                Column {
                    Text("Remember as my private dish", style = MaterialTheme.typography.titleSmall)
                    Text("Metria can suggest this later. It never leaves this phone.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                }
            }
            similarMeal?.let { previous ->
                Card(colors = CardDefaults.cardColors(containerColor = PlumSoft)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Similar to ${MealIdentity.displayTitle(previous)}", style = MaterialTheme.typography.titleSmall)
                        Text("Saved ${previous.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}. Review this meal’s foods and portions before saving.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { title = MealIdentity.displayTitle(previous) }) { Text("Use this name") }
                    }
                }
            }
            items.forEachIndexed { index, item ->
                // Every detected food lives in a dish card.  A model-provided group keeps its
                // label; an ungrouped food starts as a one-food dish that the member can name.
                val dishName = item.groupLabel ?: item.name
                val previousDishName = items.getOrNull(index - 1)?.let { previous -> previous.groupLabel ?: previous.name }
                if (index == 0 || previousDishName != dishName) {
                    Card(colors = CardDefaults.cardColors(containerColor = PlumSoft), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                dishName,
                                { value ->
                                    val renamed = value.trim().takeIf(String::isNotEmpty)
                                    items = items.mapIndexed { position, existing ->
                                        when {
                                            // Rename all members of an existing grouped dish.
                                            item.groupLabel != null && existing.groupLabel == item.groupLabel -> existing.copy(groupLabel = renamed)
                                            // A standalone detected food becomes a named dish, without
                                            // accidentally renaming every other ungrouped ingredient.
                                            item.groupLabel == null && position == index -> existing.copy(groupLabel = renamed)
                                            else -> existing
                                        }
                                    }
                                },
                                label = { Text("Dish name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    val label = item.groupLabel ?: item.name.trim().takeIf(String::isNotEmpty)
                                    if (label != null) {
                                        items = items + EditableMealItem("", "", "", "", groupLabel = label, evidence = "possible_inside")
                                        catalogMatches = catalogMatches + null
                                    }
                                }) { Text("Add possible inside") }
                            }
                        }
                    }
                }
                Text(
                    if (item.evidence == "possible_inside") "Possible ingredient" else "Visible ingredient",
                    color = Color(0xFF655D6E),
                    style = MaterialTheme.typography.labelMedium,
                )
                OutlinedTextField(item.name, { value ->
                    items = items.toMutableList().also { it[index] = item.copy(name = value) }
                    catalogMatches = catalogMatches.toMutableList().also { it[index] = null }
                }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    item.description,
                    { value -> items = items.toMutableList().also { it[index] = item.copy(description = value) } },
                    label = { Text("What Metria saw (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (item.evidence == "possible_inside") {
                    Text("Possible inside this dish — Metria did not see it directly.", color = Plum, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        confirmedPossibleIngredients = if (index in confirmedPossibleIngredients) confirmedPossibleIngredients - index else confirmedPossibleIngredients + index
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (index in confirmedPossibleIngredients) "Exclude from nutrition estimate" else "Include in nutrition estimate")
                    }
                }
                Button(onClick = {
                    val roles = listOf("main_food", "side", "drink", "sauce_oil", "dry_seasoning")
                    items = items.toMutableList().also { it[index] = item.copy(role = roles[(roles.indexOf(item.role) + 1) % roles.size]) }
                }, modifier = Modifier.fillMaxWidth()) { Text("Role: ${item.role.replace('_', ' ')}") }
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value -> items = items.toMutableList().also { it[index] = item.copy(preparation = value) } }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
                val match = catalogMatches.getOrNull(index)
                val ambiguousName = FoodCatalog.isAmbiguousModelFood(item.name)
                val localCandidates = remember(catalog, item.name) {
                    catalog?.let { FoodCatalog.candidatesForAssistantItem(it, item.name, item.description, corrections) }.orEmpty()
                }
                if (match == null) {
                    Text(
                        if (ambiguousName) "Needs identification - Metria could not tell exactly what this food is."
                        else if (index in nonMeasuredIndexes) "Not included in the estimate - dry herbs and seasoning are not measurable from a photo."
                        else if (localCandidates.isEmpty()) "We couldn't find this food in Metria's food list."
                        else "${localCandidates.size} possible food${if (localCandidates.size == 1) "" else "s"} found - choose one.",
                        color = if (index in nonMeasuredIndexes) Color(0xFF655D6E) else Color(0xFFB3261E),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (parsedGrams[index] == null) {
                    Text("Needs a whole portion from 1 to 2,000 g", color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Local candidate selected${if (localCandidates.size > 1) " from ${localCandidates.size}" else ""}: ${FoodCatalog.memberDisplayName(match)}", color = Plum, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { catalogSearchIndex = index }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (match == null && index in nonMeasuredIndexes) "Find a food anyway" else if (match == null) "Find this food" else "Change food")
                }
                if (items.size > 1) {
                    Button(
                        onClick = {
                            items = items.filterIndexed { position, _ -> position != index }
                            catalogMatches = catalogMatches.filterIndexed { position, _ -> position != index }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Remove this food") }
                }
            }
            if (items.size < 12) Button(
                onClick = {
                    items = items + EditableMealItem("", "", "", "")
                    catalogMatches = catalogMatches + null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add another food") }
            if (catalogTotals == null) {
                Text("No nutrition estimate", style = MaterialTheme.typography.titleMedium)
                if (itemsNeedingReview.isNotEmpty()) Text("Needs review: ${itemsNeedingReview.joinToString { items[it].name.ifBlank { "Unnamed food" } }}.", color = Color(0xFFB3261E))
                Text("Match ingredients separately. Include sauces or oils as their own food when known. You can still save this reviewed meal, but it will intentionally have no nutrition values.", color = Color(0xFF655D6E))
            } else {
                Text(if (isPartialEstimate) "Partial local estimate" else "Member-confirmed catalog estimate", style = MaterialTheme.typography.titleMedium)
                Text(nutritionSummary(MealNutrition(
                    catalogTotals.caloriesKcal, catalogTotals.proteinGrams,
                    catalogTotals.carbohydrateGrams, catalogTotals.fatGrams,
                    "member_confirmed_catalog", catalogRelease = catalog!!.sourceRelease,
                )))
                if (isPartialEstimate) Text(
                    "Includes: ${includedIndexes.joinToString { items[it].name }}. Excludes: ${itemsNeedingReview.joinToString { items[it].name }}. This is not added to full nutrition totals.",
                    color = Color(0xFF655D6E),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Calculated from ${catalog!!.sourceName} ${catalog.sourceRelease}.", color = Plum, style = MaterialTheme.typography.bodySmall)
                if (catalogTotals.caloriesDerivedFromMacros) Text("Calories are calculated from available protein, carbohydrates, and fat because one selected food has no USDA energy value.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
            }
            if (referenceThumbnailBase64 != null) PrivateReferenceOption(keepReference) { keepReference = it }
            OutlinedTextField(date, { date = it }, label = { Text("Date - YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            MealSlotSelector(mealSlot, MealSlots.suggestedFor(captureTime)) { mealSlot = it }
            error?.let { Text(it, color = Color(0xFFB3261E)) }
            Button(onClick = {
                val recordedAt = mealInstantForDate(date, captureTime)
                val savedItems = items.mapIndexed { index, item -> MealEntryItem(
                    item.name.trim(), parsedGrams[index], item.preparation.trim().takeIf(String::isNotEmpty),
                    catalogMatches.getOrNull(index)?.fdcId.takeIf { index in includedIndexes },
                    catalog?.sourceRelease?.takeIf { index in includedIndexes },
                    item.groupLabel,
                    item.description.trim().takeIf(String::isNotEmpty), item.role,
                ) }
                val nutrition = catalogTotals?.let { totals -> MealNutrition(
                    totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams,
                    "member_confirmed_catalog", catalogRelease = catalog?.sourceRelease,
                    estimateCoverage = if (isPartialEstimate) "partial" else "full",
                ) }
                val meal = if (recordedAt == null) null else MealEntry(
                    recordedAt = recordedAt, title = title.trim().takeIf(String::isNotEmpty), mealSlot = mealSlot, recordedTimeKnown = true, items = savedItems, assumptions = proposal.assumptions,
                    confidence = proposal.overallConfidence,
                    source = if (nutrition == null) "ai_estimate" else "member_confirmed_catalog",
                    nutrition = nutrition, referenceThumbnailBase64 = referenceThumbnailBase64.takeIf { keepReference },
                    analysisAuditId = analysisAuditId,
                )
                val learnedCorrections = catalogMatches.mapIndexedNotNull { index, selected ->
                    selected?.takeIf { it.fdcId != initialMatchIds[index] }?.let { FoodCorrection(items[index].name.trim(), it.fdcId) }
                }
                val personalDish = meal?.let { saved ->
                    if (rememberAsPersonalDish) PersonalDishValidation.fromConfirmedMeal(saved, title) else null
                }
                if (meal == null || !MealEntryValidation.valid(meal) || (rememberAsPersonalDish && personalDish == null)) {
                    error = if (rememberAsPersonalDish) "Give this private dish a name and review its foods before saving." else "Review the food names, portions (1-2,000 g), and date."
                } else onSave(meal, learnedCorrections, personalDish)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(when {
                    catalogTotals == null -> "Save meal without nutrition"
                    isPartialEstimate -> "Save partial estimate locally"
                    else -> "Save estimated nutrition locally"
                })
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
    var items by remember(meal) { mutableStateOf(meal.items.map { EditableMealItem(it.name, it.description.orEmpty(), it.grams?.toString().orEmpty(), it.preparation.orEmpty(), it.groupLabel, it.role) }) }
    var title by remember(meal) { mutableStateOf(meal.title ?: MealIdentity.titleFor(meal.items)) }
    var date by remember(meal) { mutableStateOf(meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString()) }
    val recordedTime = remember(meal) { meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0) }
    var mealSlot by remember(meal) { mutableStateOf(meal.mealSlot) }
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
        foodId?.let { id -> catalog?.foods?.firstOrNull { it.fdcId == id } } ?: catalog?.let { FoodCatalog.suggest(it, item.name, item.description.orEmpty()) }
    }) }
    var catalogSearchIndex by remember { mutableStateOf<Int?>(null) }
    val parsedCatalogGrams = items.map { it.grams.replace(',', '.').toIntOrNull()?.takeIf { grams -> grams in 1..2_000 } }
    val catalogValues = items.indices.map { index ->
        catalogMatches.getOrNull(index)?.let { food -> parsedCatalogGrams[index]?.let { FoodCatalog.calculate(food, it) } }
    }
    val includedCatalogIndexes = catalogValues.indices.filter { index ->
        val nutrition = catalogValues[index]
        nutrition?.caloriesKcal != null && nutrition.proteinGrams != null &&
            nutrition.carbohydrateGrams != null && nutrition.fatGrams != null
    }
    val catalogTotals = includedCatalogIndexes.takeIf { isCatalogMeal && it.isNotEmpty() }?.map { catalogValues[it]!! }?.let { values -> CatalogNutrition(
        values.sumOf { it.caloriesKcal!! }, values.sumOf { it.proteinGrams!! },
        values.sumOf { it.carbohydrateGrams!! }, values.sumOf { it.fatGrams!! },
        values.any { it.caloriesDerivedFromMacros },
    ) }
    val isPartialCatalogEstimate = catalogTotals != null && includedCatalogIndexes.size != items.size
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 72.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit meal", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(title, { title = it }, label = { Text("Meal name (optional)") }, modifier = Modifier.fillMaxWidth())
            MealSlotSelector(mealSlot, MealSlots.suggestedFor(recordedTime).takeIf { meal.recordedTimeKnown }) { mealSlot = it }
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
                OutlinedTextField(
                    item.description,
                    { value -> items = items.toMutableList().also { it[index] = item.copy(description = value) } },
                    label = { Text("What Metria saw (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(item.grams, { value -> items = items.toMutableList().also { it[index] = item.copy(grams = value) } }, label = { Text("Portion in grams") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(item.preparation, { value -> items = items.toMutableList().also { it[index] = item.copy(preparation = value) } }, label = { Text("Preparation (optional)") }, modifier = Modifier.fillMaxWidth())
                if (isCatalogMeal) {
                    val match = catalogMatches.getOrNull(index)
                    Text(
                        if (match == null) "Choose this food to include it in the estimate."
                        else "Included food: ${FoodCatalog.memberDisplayName(match)}",
                        color = if (match == null) Color(0xFFB3261E) else Plum,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { catalogSearchIndex = index }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (match == null) "Find this food" else "Change food")
                    }
                }
            }
            if (isCatalogMeal) {
                Text(if (isPartialCatalogEstimate) "Partial local estimate" else "Member-confirmed catalog estimate", style = MaterialTheme.typography.titleMedium)
                if (catalogTotals == null) {
                    Text("Choose at least one food and its portion to calculate an estimate.", color = Color(0xFFB3261E))
                } else {
                    Text(nutritionSummary(MealNutrition(
                        catalogTotals.caloriesKcal, catalogTotals.proteinGrams,
                        catalogTotals.carbohydrateGrams, catalogTotals.fatGrams,
                        "member_confirmed_catalog", catalogRelease = catalog!!.sourceRelease,
                    )), color = Plum)
                    if (isPartialCatalogEstimate) Text(
                        "Includes: ${includedCatalogIndexes.joinToString { items[it].name }}. Other foods are excluded from this estimate and full totals.",
                        color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall,
                    )
                    if (catalogTotals.caloriesDerivedFromMacros) Text("Calories are calculated from available macros for one selected food.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("No nutrition estimate", style = MaterialTheme.typography.titleMedium)
                Text(
                    "This meal was saved without complete nutrition values. It is not included in nutrition summaries.",
                    color = Color(0xFF655D6E),
                    style = MaterialTheme.typography.bodySmall,
                )
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
                val recordedAt = mealInstantForDate(date, recordedTime)
                val updatedItems = items.mapIndexed { index, item -> MealEntryItem(
                    item.name.trim(),
                    if (isCatalogMeal) parsedCatalogGrams[index] else item.grams.replace(',', '.').toIntOrNull(),
                    item.preparation.trim().takeIf(String::isNotEmpty),
                    catalogMatches.getOrNull(index)?.fdcId.takeIf { index in includedCatalogIndexes },
                    catalog?.sourceRelease?.takeIf { index in includedCatalogIndexes },
                    item.groupLabel,
                    item.description.trim().takeIf(String::isNotEmpty), item.role,
                ) }
                val rawNutrition = listOf(calories, protein, carbs, fat)
                val values = rawNutrition.map { it.replace(',', '.').toDoubleOrNull() }
                val invalidNutrition = rawNutrition.zip(values).any { (raw, value) -> raw.isNotBlank() && value == null }
                val nutrition = when {
                    isCatalogMeal -> catalogTotals?.let { totals -> MealNutrition(
                        totals.caloriesKcal, totals.proteinGrams, totals.carbohydrateGrams, totals.fatGrams,
                        "member_confirmed_catalog", catalogRelease = catalog?.sourceRelease,
                        estimateCoverage = if (isPartialCatalogEstimate) "partial" else "full",
                    ) }
                    rawNutrition.all(String::isBlank) -> null
                    else -> MealNutrition(values[0], values[1], values[2], values[3], "manual_entry")
                }
                val updated = if (recordedAt == null) null else meal.copy(
                    recordedAt = recordedAt,
                    title = title.trim().takeIf(String::isNotEmpty),
                    mealSlot = mealSlot,
                    items = updatedItems,
                    source = if (isCatalogMeal) "member_confirmed_catalog" else meal.source,
                    nutrition = nutrition,
                    referenceThumbnailBase64 = referenceThumbnail,
                )
                if (invalidNutrition || updated == null || !MealEntryValidation.valid(updated)) error = "Choose at least one food and a valid portion, then review the date." else onSave(updated)
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

@Composable
private fun MealSlotSelector(selected: String?, suggested: String? = null, choose: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            suggested?.let { "Meal type (optional) - suggested: ${MealSlots.label(it)}" } ?: "Meal type (optional)",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("breakfast", "lunch", "dinner", "snack").forEach { slot ->
                FilterChip(
                    selected = selected == slot,
                    onClick = { choose(if (selected == slot) null else slot) },
                    label = { Text(MealSlots.label(slot)) },
                )
            }
        }
        Text("Optional — only a type you choose is used for personalised meal suggestions.", color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall)
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
    val captureTime = remember { LocalTime.now().withSecond(0).withNano(0) }
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
                val recordedAt = mealInstantForDate(date, captureTime)
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
    val captureTime = remember { LocalTime.now().withSecond(0).withNano(0) }
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
                val recordedAt = mealInstantForDate(date, captureTime)
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
private fun WeightBottomSheet(
    entry: WeightEntry?,
    voiceDraft: VoiceWeightDraft?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    save: (Double, String?, Instant) -> Unit,
) {
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
        if (entry != null && onDelete != null) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete weight entry", color = MetriaColors.Danger)
            }
        }
    } }
}
