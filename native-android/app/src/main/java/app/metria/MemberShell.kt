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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import androidx.core.content.ContextCompat
import app.metria.ui.navigation.MetriaBottomNavigation
import app.metria.ui.components.AssistantPanelAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.metria.ui.screens.MetriaMealReviewSheet
import app.metria.ui.screens.MealAnalysisFallbackCard
import app.metria.ui.screens.MealDraftDiscardAction
import app.metria.ui.screens.MealReviewMode
import app.metria.ui.screens.MetriaAssistantConversationScreen
import app.metria.ui.screens.MetriaAssistantHome
import app.metria.ui.screens.MetriaChatMessage
import app.metria.ui.screens.MetriaSavedDishChoice
import app.metria.ui.screens.FirstRunOnboarding
import app.metria.ui.screens.MetriaPrivacySettingsScreen
import app.metria.ui.screens.MetriaProgressScreen
import app.metria.ui.screens.MetriaTodayScreen
import app.metria.ui.screens.SavedMealEditorScreen
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
    var photoAnalysisMode by remember { mutableStateOf(activity.photoAnalysisMode()) }
    val providerScope = rememberCoroutineScope()
    var providerChangeInFlight by remember { mutableStateOf(false) }
    var setupModelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var destination by rememberSaveable { mutableStateOf("Today") }
    // The unchanged central icon opens the shared contextual conversation directly.
    var quickAssistantOpen by rememberSaveable { mutableStateOf(false) }
    var quickAssistantStartVoice by rememberSaveable { mutableStateOf(false) }
    var quickAssistantVoiceEntry by rememberSaveable { mutableStateOf(false) }
    var quickAssistantListening by remember { mutableStateOf(false) }
    // Approved UX: the floating assistant reuses this one camera flow; no image path or storage is duplicated.
    var assistantImagePurpose by rememberSaveable { mutableStateOf<String?>(null) }
    var assistantCapturePurpose by rememberSaveable { mutableStateOf<String?>(null) }
    var addSheet by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<WeightEntry?>(null) }
    var editMeal by remember { mutableStateOf<MealEntry?>(null) }
    var selectedAskMealId by remember { mutableStateOf<String?>(null) }
    var selectedAskMealTitle by remember { mutableStateOf<String?>(null) }
    var pendingSelectedMealReadId by remember { mutableStateOf<String?>(null) }
    var voiceDraft by remember { mutableStateOf<VoiceWeightDraft?>(null) }
    var modelStatus by remember { mutableStateOf(activity.modelStatus()) }
    var assistantTraceStatus by remember { mutableStateOf(activity.assistantTraceStatus()) }
    var debugImageCaptureStatus by remember { mutableStateOf(activity.debugImageCaptureStatus()) }
    val assistantConversationSession = rememberPrivateAssistantConversationSession()
    var mealProposalForConfirmation by remember { mutableStateOf<MealProposal?>(null) }
    var portionOverrideForConfirmation by remember { mutableStateOf<ChatPortionOverride?>(null) }
    var foodReplacementForConfirmation by remember { mutableStateOf<ChatFoodReplacementOverride?>(null) }
    var savedDishPrefill by remember { mutableStateOf<CompanionAssistantPolicy.SavedDishMealPrefill?>(null) }
    var mealReviewFromAssistant by remember { mutableStateOf(false) }
    var mealReviewIsTextEntry by remember { mutableStateOf(false) }
    var mealReviewIsPhotoEntry by remember { mutableStateOf(false) }
    var manualMealReview by rememberSaveable { mutableStateOf(false) }
    var addMealSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMealSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMealDate by rememberSaveable { mutableStateOf<String?>(null) }
    var mealEntryOpen by remember { mutableStateOf(false) }
    var mealEntryVoice by remember { mutableStateOf(false) }
    var labelDraftForConfirmation by remember { mutableStateOf<LabelNutritionDraft?>(null) }
    var editMealFromAssistant by remember { mutableStateOf(false) }
    var browseFoodCatalog by remember { mutableStateOf(false) }
    var catalogFoodForConfirmation by remember { mutableStateOf<CatalogFood?>(null) }
    LaunchedEffect(
        portionOverrideForConfirmation, mealProposalForConfirmation, mealReviewFromAssistant,
        savedDishPrefill, manualMealReview, mealReviewIsPhotoEntry, mealReviewIsTextEntry,
    ) {
        val override = portionOverrideForConfirmation ?: return@LaunchedEffect
        if (!mealReviewFromAssistant || mealProposalForConfirmation != override.originalProposal ||
            savedDishPrefill != null || manualMealReview || mealReviewIsPhotoEntry || mealReviewIsTextEntry
        ) portionOverrideForConfirmation = null
    }
    LaunchedEffect(foodReplacementForConfirmation, mealProposalForConfirmation, mealReviewFromAssistant,
        savedDishPrefill, manualMealReview, mealReviewIsPhotoEntry, mealReviewIsTextEntry) {
        val replacement = foodReplacementForConfirmation ?: return@LaunchedEffect
        if (!mealReviewFromAssistant || mealProposalForConfirmation != replacement.originalProposal ||
            savedDishPrefill != null || manualMealReview || mealReviewIsPhotoEntry || mealReviewIsTextEntry
        ) foodReplacementForConfirmation = null
    }
    var editNutritionTargets by remember { mutableStateOf(false) }
    var showGoalWelcomeInitially by remember { mutableStateOf(false) }
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
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (pendingSelectedMealReadId != null) modelStatus = activity.cancelPrivateAssistant()
                selectedAskMealId = null
                selectedAskMealTitle = null
                pendingSelectedMealReadId = null
                editMealFromAssistant = false
            }
        }
        activity.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            activity.lifecycle.removeObserver(lifecycleObserver)
            activity.setVaultCallback(null)
            activity.setVoiceDraftCallback(null)
        }
    }
    LaunchedEffect(status, voiceDraft) {
        if (status is EncryptedVault.Status.Unlocked && voiceDraft != null) addSheet = true
    }
    LaunchedEffect(status) {
        debugImageCaptureStatus = activity.debugImageCaptureStatus()
        when (val current = status) {
            is EncryptedVault.Status.Unlocked -> {
                // ModelLifecycle is process-local; restore the persisted phrasing style after
                // biometric unlock or an activity/process recreation without touching history.
                activity.setAssistantStyle(current.assistantStyle)
                val requestBeforeSync = assistantConversationSession.activeRequestId.value
                val normalizedSnapshot = BuildConfig.DEBUG &&
                    assistantConversationSession.matchesPersistedSnapshot(current.activeConversation)
                assistantConversationSession.synchronizeAfterWrites(current.activeConversation, current.conversationExpired)
                if (requestBeforeSync != null && assistantConversationSession.activeRequestId.value == null) {
                    activity.recordAssistantReplyDelivery(requestBeforeSync, when {
                        current.conversationExpired -> "conversation_sync_expired"
                        normalizedSnapshot -> "conversation_sync_normalized"
                        else -> "conversation_sync_changed"
                    })
                }
                if (activity.consumeFirstGoalOffer() && voiceDraft == null && !addSheet) {
                    showGoalWelcomeInitially = true
                    editNutritionTargets = true
                }
                if (current.conversationExpired) status = vault.acknowledgeConversationExpiry()
            }
            else -> {
                assistantConversationSession.clearForLock()
                selectedAskMealId = null
                selectedAskMealTitle = null
                pendingSelectedMealReadId = null
                mealProposalForConfirmation = null
                savedDishPrefill = null
                manualMealReview = false
                mealReviewIsTextEntry = false
                mealReviewIsPhotoEntry = false
                addMealSlot = null
                pendingMealSlot = null
                pendingMealDate = null
                labelDraftForConfirmation = null
                assistantImagePurpose = null
                assistantCapturePurpose = null
                mealEntryOpen = false
                mealEntryVoice = false
                quickAssistantVoiceEntry = false
                quickAssistantOpen = false
                quickAssistantStartVoice = false
                quickAssistantListening = false
                editNutritionTargets = false
                showGoalWelcomeInitially = false
            }
        }
    }
    LaunchedEffect(status is EncryptedVault.Status.Unlocked) {
        while (status is EncryptedVault.Status.Unlocked) {
            delay(60_000)
            status = vault.refreshConversationRetention()
        }
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
    LaunchedEffect(quickAssistantStartVoice, quickAssistantVoiceEntry, modelStatus.state) {
        if ((quickAssistantStartVoice || quickAssistantVoiceEntry) && modelStatus.state == "ready") {
            modelStatus = activity.prepareLocalAssistant()
        }
    }
    LaunchedEffect(destination) {
        if (destination != "Assistant") {
            if (activity.modelStatus().imageState in setOf("ready", "preparing")) {
                activity.discardLocalMealImage()
            }
            modelStatus = activity.modelStatus()
            assistantTraceStatus = activity.assistantTraceStatus()
            debugImageCaptureStatus = activity.debugImageCaptureStatus()
        }
    }
    MetriaTheme {
        when (val vaultStatus = status) {
            EncryptedVault.Status.Missing -> if (!hasCompletedOnboarding) FirstRunOnboarding(
                selectedMode = photoAnalysisMode,
                onModeChange = { photoAnalysisMode = it },
                onComplete = { activity.completeOnboarding(photoAnalysisMode); hasCompletedOnboarding = true },
            ) else AssistantSetupGate(
                modelStatus = setupModelStatus,
                cloudPhotoMode = photoAnalysisMode == "cloud",
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
                if (photoAnalysisMode != "cloud" && setupModelStatus.state in setOf("absent", "removed", "failed", "downloading", "verifying")) {
                    AssistantSetupGate(
                        modelStatus = setupModelStatus,
                        cloudPhotoMode = false,
                        download = { setupModelStatus = activity.downloadLocalAssistant() },
                        cancelDownload = { setupModelStatus = activity.cancelLocalAssistantDownload() },
                        continueToApp = {},
                    )
                    return@MetriaTheme
                }
                Scaffold(
                    modifier = if (quickAssistantOpen || assistantCapturePurpose != null) Modifier.clearAndSetSemantics { } else Modifier,
                    containerColor = MetriaColors.Canvas,
                    bottomBar = {
                        if (!mealEntryOpen && !(destination == "Assistant" && pendingMealSlot != null)) MetriaBottomNavigation(
                            current = destination,
                            assistantListening = quickAssistantListening,
                            onNavigate = { pendingMealSlot = null; pendingMealDate = null; addMealSlot = null; destination = it },
                            onMetriaAction = {
                                pendingMealSlot = null
                                pendingMealDate = null
                                quickAssistantStartVoice = false
                                quickAssistantVoiceEntry = false
                                quickAssistantOpen = true
                            },
                        )
                    },
                ) { padding ->
                    if (mealEntryOpen) app.metria.ui.screens.MealEntryScreen(
                        slotLabel = pendingMealSlot?.let(MealSlots::label) ?: "meal",
                        lifecycleOwner = activity,
                        voiceInitially = mealEntryVoice,
                        modelStatus = modelStatus,
                        transcribeAudio = { audio -> activity.understandPrivateCoachAudio(audio).also { modelStatus = it } },
                        createDraft = { description -> activity.askLocalMealAssistant(description).also { modelStatus = it } },
                        prepareAssistant = { modelStatus = activity.prepareLocalAssistant() },
                        cancelAssistant = { modelStatus = activity.cancelPrivateAssistant() },
                        onReview = { proposal ->
                            mealEntryOpen = false
                            mealReviewFromAssistant = false
                            manualMealReview = false
                            mealReviewIsTextEntry = true
                            mealProposalForConfirmation = proposal
                        },
                        onManualEntry = {
                            mealEntryOpen = false
                            mealEntryVoice = false
                            mealReviewIsTextEntry = false
                            manualMealReview = true
                            mealReviewFromAssistant = false
                            mealProposalForConfirmation = null
                            modelStatus = activity.cancelPrivateAssistant()
                            modelStatus = activity.clearLocalMealDraft()
                        },
                        onDismiss = {
                            mealEntryOpen = false
                            mealEntryVoice = false
                            mealReviewIsTextEntry = false
                            pendingMealSlot = null
                            pendingMealDate = null
                            modelStatus = activity.cancelPrivateAssistant()
                            modelStatus = activity.clearLocalMealDraft()
                        },
                        modifier = Modifier.padding(padding),
                    ) else Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (destination) {
                            "Today" -> MetriaTodayScreen(
                                vaultStatus.entries, vaultStatus.meals, vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                cycleContext = vaultStatus.cycleContext,
                                onEditMeal = { editMealFromAssistant = false; editMeal = it },
                                onEditNutritionTargets = { editNutritionTargets = true },
                                onOpenProgress = { destination = "History" },
                                onAddMeal = { addMealSlot = it },
                                goalPhase = vaultStatus.goalSetup?.current,
                                onAddWeight = { voiceDraft = null; editEntry = null; addSheet = true },
                            )
                            "History" -> MetriaProgressScreen(
                                entries = vaultStatus.entries,
                                goalPhase = vaultStatus.goalSetup?.current,
                                meals = vaultStatus.meals,
                                unit = vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                onEditWeight = { editEntry = it },
                                onEditMeal = { editMealFromAssistant = false; editMeal = it },
                                onEditNutritionTargets = { editNutritionTargets = true },
                            )
                            "Assistant" -> AssistantScreen(
                                onOpenConversation = {
                                    quickAssistantStartVoice = false
                                    quickAssistantOpen = true
                                },
                                assistantConversationSession = assistantConversationSession,
                                onConversationChanged = { conversation ->
                                    assistantConversationSession.saveConversationSnapshot(conversation, vault::saveActiveConversation)
                                },
                                onNewConversation = {
                                    assistantConversationSession.invalidateForConversationClear()
                                    val cleared = assistantConversationSession.clearConversation {
                                        val result = withContext(Dispatchers.IO) { vault.clearActiveConversation() }
                                        status = result
                                        result is EncryptedVault.Status.Unlocked
                                    }
                                    if (cleared == ConversationPersistenceResult.SAVED) assistantConversationSession.reset()
                                    cleared == ConversationPersistenceResult.SAVED
                                },
                                modelStatus = modelStatus,
                                photoAnalysisMode = photoAnalysisMode,
                                cloudPhotoConfigured = activity.cloudPhotoConfigured(),
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
                                askMeal = { text -> activity.askLocalMealAssistant(text).also { modelStatus = it } },
                                askCoach = { question, snapshot, conversationContext -> activity.askPrivateCoach(question, snapshot, conversationContext).also { modelStatus = it } },
                                startAgent = { question, history, confirmation, portionEditContext, foodEditContext ->
                                    activity.startPrivateAgentTurn(
                                        question, history, confirmation = confirmation,
                                        portionEditContext = portionEditContext,
                                        foodEditContext = foodEditContext,
                                        unlocked = status is EncryptedVault.Status.Unlocked,
                                        conversationGeneration = assistantConversationSession.conversationGeneration.value,
                                    )
                                        .also { modelStatus = it }
                                },
                                continueAgentTool = activity::continuePrivateAgentTool,
                                understandCoach = { question, conversationContext -> activity.understandPrivateCoach(question, conversationContext).also { modelStatus = it } },
                                understandCoachAudio = { audio, conversationContext -> activity.understandPrivateCoachAudio(audio, conversationContext).also { modelStatus = it } },
                                cancelAssistant = { modelStatus = activity.cancelPrivateAssistant() },
                                saveWeightDirect = { kilograms, date ->
                                    val entry = WeightEntry(kilograms = kilograms, recordedAt = date)
                                    status = vault.addWeightEntry(entry)
                                    if (status is EncryptedVault.Status.Unlocked) entry else null
                                },
                                editSavedWeight = { editEntry = it },
                                undoSavedWeight = { entry -> status = vault.deleteWeight(entry.id) },
                                openFoodCatalog = { browseFoodCatalog = true },
                                review = {
                                    manualMealReview = false
                                    mealReviewFromAssistant = false
                                    mealReviewIsTextEntry = false
                                    mealReviewIsPhotoEntry = pendingMealSlot != null && destination == "Assistant"
                                    mealProposalForConfirmation = it
                                },
                                onManualMealReview = {
                                    manualMealReview = true
                                    mealReviewFromAssistant = false
                                    mealReviewIsTextEntry = false
                                    mealReviewIsPhotoEntry = false
                                    mealProposalForConfirmation = null
                                },
                                prepareImage = { uri -> modelStatus = activity.prepareLocalMealImage(uri) },
                                analyzeImage = { description -> activity.analyzeLocalMealImage(description).also { modelStatus = it } },
                                cancelImageAnalysis = { modelStatus = activity.cancelLocalMealImageAnalysis() },
                                readLabel = { modelStatus = activity.readLocalNutritionLabel() },
                                discardImage = { modelStatus = activity.discardLocalMealImage() },
                                discardImageMealDraft = { modelStatus = activity.discardImageMealDraft() },
                                discardAssistantMealDraft = { modelStatus = activity.discardAssistantMealDraft() },
                                reviewLabel = { labelDraftForConfirmation = it },
                                newCameraUri = { activity.newLocalCameraCaptureUri() },
                                prepareCapturedImage = { uri -> modelStatus = activity.prepareLocalCapturedMealImage(uri) },
                                discardPendingCapture = { activity.discardPendingCameraCapture() },
                                beginExternalImageFlow = { activity.beginExternalImageFlow() },
                                beginImageFlow = { activity.beginLocalImageFlow() },
                                finishCooldown = { modelStatus = activity.finishLocalImageCooldown() },
                                contextualMealSlot = pendingMealSlot?.let(MealSlots::label),
                                onBackFromContextualMeal = {
                                    if (modelStatus.imageState == "analyzing") modelStatus = activity.cancelLocalMealImageAnalysis()
                                    modelStatus = activity.discardLocalMealImage()
                                    activity.discardPendingCameraCapture()
                                    assistantImagePurpose = null
                                    labelDraftForConfirmation = null
                                    mealProposalForConfirmation = null
                                    mealReviewIsPhotoEntry = false
                                    pendingMealSlot = null
                                    pendingMealDate = null
                                    destination = "Today"
                                },
                            )
                            else -> MetriaPrivacySettingsScreen(
                                unit = vaultStatus.displayUnit,
                                nutritionTargets = vaultStatus.nutritionTargets,
                                modelStatus = modelStatus,
                                assistantTraceStatus = assistantTraceStatus,
                                debugImageCaptureStatus = debugImageCaptureStatus,
                                analysisAudits = vaultStatus.analysisAudits,
                                cycleContext = vaultStatus.cycleContext,
                                assistantStyle = vaultStatus.assistantStyle,
                                personalDishes = vaultStatus.personalDishes,
                                mealCorrectionMemories = vaultStatus.mealCorrectionMemories,
                                foodCorrectionCount = vaultStatus.foodCorrections.size,
                                legacyFoodCorrectionCount = vaultStatus.foodCorrections.count { it.sourceMealId == null },
                                archiveMessage = archiveMessage,
                                photoAnalysisMode = photoAnalysisMode,
                                cloudPhotoConfigured = activity.cloudPhotoConfigured(),
                                cloudPhotoEndpoint = activity.cloudPhotoEndpoint(),
                                onSelectPhotoAnalysisMode = { mode ->
                                    if (!providerChangeInFlight && (mode == "local" || activity.cloudPhotoConfigured())) {
                                        providerChangeInFlight = true
                                        providerScope.launch {
                                            try {
                                                withContext(Dispatchers.Default) { activity.selectPhotoAnalysisMode(mode) }
                                            } finally {
                                                withContext(NonCancellable + Dispatchers.Main.immediate) {
                                                    photoAnalysisMode = activity.photoAnalysisMode()
                                                    modelStatus = activity.modelStatus()
                                                    providerChangeInFlight = false
                                                }
                                            }
                                        }
                                    }
                                },
                                onConfigureCloudPhoto = activity::configureCloudPhoto,
                                onDisconnectCloudPhoto = {
                                    if (!providerChangeInFlight) {
                                        providerChangeInFlight = true
                                        providerScope.launch {
                                            try {
                                                withContext(Dispatchers.Default) { activity.disconnectCloudPhoto() }
                                            } finally {
                                                withContext(NonCancellable + Dispatchers.Main.immediate) {
                                                    photoAnalysisMode = activity.photoAnalysisMode()
                                                    modelStatus = activity.modelStatus()
                                                    providerChangeInFlight = false
                                                }
                                            }
                                        }
                                    }
                                },
                                onChangeUnit = { status = vault.setDisplayUnit(if (vaultStatus.displayUnit == "kg") "lb" else "kg") },
                                onEditNutritionTargets = { editNutritionTargets = true },
                                onEditCycleContext = { editCycleContext = true },
                                onAssistantStyleChanged = { style ->
                                    val updated = vault.setAssistantStyle(style)
                                    if (updated is EncryptedVault.Status.Unlocked) {
                                        status = updated
                                        activity.setAssistantStyle(updated.assistantStyle)
                                        modelStatus = activity.modelStatus()
                                        null
                                    } else {
                                        (updated as? EncryptedVault.Status.Failed)?.message
                                            ?: "Unlock Metria and try again."
                                    }
                                },
                                onDeletePersonalDish = { dish ->
                                    status = vault.deletePersonalDish(dish.id)
                                    (status as? EncryptedVault.Status.Failed)?.message
                                },
                                onClearMealLearning = {
                                    status = vault.clearLearning()
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
                                onSetAssistantTraceEnabled = { enabled -> activity.setAssistantTraceEnabled(enabled); assistantTraceStatus = activity.assistantTraceStatus() },
                                onClearAssistantTrace = { activity.clearAssistantTrace(); assistantTraceStatus = activity.assistantTraceStatus() },
                                onSetDebugImageCaptureEnabled = { enabled ->
                                    val changed = activity.setDebugImageCaptureEnabled(enabled)
                                    debugImageCaptureStatus = activity.debugImageCaptureStatus()
                                    changed
                                },
                                onClearDebugImageCapture = {
                                    activity.clearDebugImageCapture()
                                    debugImageCaptureStatus = activity.debugImageCaptureStatus()
                                },
                            )
                        }
                    }
                }
                addMealSlot?.let { slot -> app.metria.ui.screens.AddMealSheet(
                    slot = slot,
                    onDismiss = { addMealSlot = null },
                    onPhoto = {
                        pendingMealSlot = slot
                        pendingMealDate = LocalDate.now().toString()
                        addMealSlot = null
                        assistantImagePurpose = "meal"
                        destination = "Assistant"
                    },
                    onSpeak = {
                        pendingMealSlot = slot
                        pendingMealDate = LocalDate.now().toString()
                        addMealSlot = null
                        mealEntryVoice = true
                        mealEntryOpen = true
                    },
                    onType = {
                        pendingMealSlot = slot
                        pendingMealDate = LocalDate.now().toString()
                        addMealSlot = null
                        mealEntryVoice = false
                        mealEntryOpen = true
                    },
                    onManual = {
                        pendingMealSlot = slot
                        pendingMealDate = LocalDate.now().toString()
                        addMealSlot = null
                        manualMealReview = true
                        mealReviewFromAssistant = false
                        mealProposalForConfirmation = null
                    },
                ) }
                assistantCapturePurpose?.let { purpose ->
                    app.metria.ui.screens.AssistantCapturePanel(
                        initialPurpose = purpose,
                        photoAnalysisMode = photoAnalysisMode,
                        cloudPhotoConfigured = activity.cloudPhotoConfigured(),
                        initialRecordedDate = pendingMealDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        initialMealSlot = pendingMealSlot,
                        status = modelStatus,
                        bottomBar = { requestExit ->
                            MetriaBottomNavigation(
                                current = destination,
                                assistantListening = false,
                                onNavigate = { target -> requestExit {
                                    assistantCapturePurpose = null
                                    pendingMealSlot = null
                                    pendingMealDate = null
                                    destination = target
                                } },
                                onMetriaAction = { requestExit { assistantCapturePurpose = null; quickAssistantOpen = true } },
                            )
                        },
                        onBackToChat = { assistantCapturePurpose = null; quickAssistantOpen = true },
                        onClose = { assistantCapturePurpose = null },
                        onOpenTools = { assistantCapturePurpose = null; destination = "Assistant" },
                        beginImageFlow = { activity.beginLocalImageFlow().also { modelStatus = it } },
                        newCameraUri = activity::newLocalCameraCaptureUri,
                        beginExternalImageFlow = activity::beginExternalImageFlow,
                        prepareImage = { modelStatus = activity.prepareLocalMealImage(it) },
                        prepareCapturedImage = { modelStatus = activity.prepareLocalCapturedMealImage(it) },
                        discardPendingCapture = activity::discardPendingCameraCapture,
                        discardImage = { modelStatus = activity.discardLocalMealImage() },
                        analyzeImage = { activity.analyzeLocalMealImage(it).also { result -> modelStatus = result } },
                        readLabel = { modelStatus = activity.readLocalNutritionLabel() },
                        cancelAnalysis = { modelStatus = activity.cancelLocalMealImageAnalysis() },
                        finishCooldown = { modelStatus = activity.finishLocalImageCooldown() },
                        onReviewMeal = {
                            manualMealReview = false
                            mealReviewFromAssistant = false
                            mealReviewIsTextEntry = false
                            mealReviewIsPhotoEntry = false
                            mealProposalForConfirmation = it
                        },
                        onSaveLabel = { meal ->
                            val fromContextualCapture = assistantCapturePurpose == purpose
                            val sessionAtTap = assistantConversationSession.conversationGeneration.value
                            val result = assistantConversationSession.withWriterIdle {
                                if (!fromContextualCapture || sessionAtTap != assistantConversationSession.conversationGeneration.value ||
                                    assistantCapturePurpose != purpose || vault.status() !is EncryptedVault.Status.Unlocked
                                ) EncryptedVault.Status.Locked
                                else {
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    val saved = vault.addMeal(
                                        meal,
                                        recordInConversation = true,
                                        createReceiptOnlyConversation = true,
                                    )
                                    if (saved is EncryptedVault.Status.Unlocked && sessionAtTap == assistantConversationSession.conversationGeneration.value &&
                                        assistantCapturePurpose == purpose
                                    ) {
                                        assistantConversationSession.synchronize(saved.activeConversation, saved.conversationExpired)
                                    }
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    saved
                                }
                            }
                            when (result) {
                                is EncryptedVault.Status.Unlocked -> {
                                    status = result
                                    modelStatus = activity.clearLocalMealDraft()
                                    assistantCapturePurpose = null
                                    if (fromContextualCapture) {
                                        quickAssistantOpen = true
                                        pendingMealSlot = null
                                        pendingMealDate = null
                                    }
                                    null
                                }
                                is EncryptedVault.Status.Failed -> result.message
                                else -> "Unlock Metria and try saving again."
                            }
                        },
                        onManualMeal = {
                            manualMealReview = true
                            mealReviewFromAssistant = false
                            mealReviewIsTextEntry = false
                            mealReviewIsPhotoEntry = false
                            mealProposalForConfirmation = null
                        },
                    )
                }
                if (quickAssistantOpen) {
                    PrivateAssistantChat(
                        photoAnalysisMode = photoAnalysisMode,
                        bottomBar = {
                            MetriaBottomNavigation(
                                current = destination,
                                assistantListening = quickAssistantListening,
                                onNavigate = {
                                    quickAssistantOpen = false
                                    quickAssistantVoiceEntry = false
                                    quickAssistantStartVoice = false
                                    quickAssistantListening = false
                                    pendingMealSlot = null
                                    pendingMealDate = null
                                    addMealSlot = null
                                    destination = it
                                },
                                onMetriaAction = {
                                    quickAssistantOpen = false
                                    quickAssistantVoiceEntry = false
                                    quickAssistantStartVoice = false
                                    quickAssistantListening = false
                                    pendingMealSlot = null
                                },
                            )
                        },
                        conversationSession = assistantConversationSession,
                        foodCatalog = foodCatalog,
                        foodCorrections = vaultStatus.foodCorrections,
                        onPanelAction = { action ->
                            when (action) {
                                AssistantPanelAction.ANALYSE_MEAL, AssistantPanelAction.SCAN_LABEL -> {
                                    quickAssistantOpen = false
                                    modelStatus = activity.discardLocalMealImage()
                                    modelStatus = activity.beginLocalImageFlow()
                                    pendingMealSlot = null
                                    pendingMealDate = null
                                    assistantCapturePurpose = if (action == AssistantPanelAction.ANALYSE_MEAL) "meal" else "label"
                                }
                                AssistantPanelAction.ADD_WEIGHT -> {
                                    voiceDraft = null
                                    addSheet = true
                                }
                                AssistantPanelAction.ASSISTANT_TOOLS -> {
                                    quickAssistantOpen = false
                                    destination = "Assistant"
                                }
                                AssistantPanelAction.PRIVACY -> {
                                    quickAssistantOpen = false
                                    destination = "Privacy"
                                }
                                AssistantPanelAction.LOCK -> activity.lockVault()
                            }
                        },
                        onConversationChanged = { conversation ->
                            assistantConversationSession.saveConversationSnapshot(conversation, vault::saveActiveConversation)
                        },
                        onNewConversation = {
                            assistantConversationSession.invalidateForConversationClear()
                            val cleared = assistantConversationSession.clearConversation {
                                val result = withContext(Dispatchers.IO) { vault.clearActiveConversation() }
                                status = result
                                result is EncryptedVault.Status.Unlocked
                            }
                            if (cleared == ConversationPersistenceResult.SAVED) {
                                selectedAskMealId = null
                                selectedAskMealTitle = null
                                pendingSelectedMealReadId = null
                                assistantConversationSession.reset()
                            }
                            cleared == ConversationPersistenceResult.SAVED
                        },
                        modelStatus = modelStatus,
                        recordReplyDelivery = activity::recordAssistantReplyDelivery,
                        entries = vaultStatus.entries,
                        meals = vaultStatus.meals,
                        personalDishes = vaultStatus.personalDishes,
                        nutritionTargets = vaultStatus.nutritionTargets,
                        cycleContext = vaultStatus.cycleContext,
                        onBack = {
                            quickAssistantOpen = false
                            quickAssistantVoiceEntry = false
                            quickAssistantStartVoice = false
                            quickAssistantListening = false
                            pendingMealSlot = null
                        },
                        onVoiceListeningChanged = { quickAssistantListening = it },
                        startVoiceImmediately = quickAssistantStartVoice,
                        openVoiceEntry = quickAssistantVoiceEntry,
                        voiceMealEntry = pendingMealSlot != null,
                        onVoiceEntryHandled = { quickAssistantVoiceEntry = false },
                        onVoiceStartHandled = { quickAssistantStartVoice = false },
                        readCurrentModelStatus = { activity.modelStatus().also { modelStatus = it } },
                        transcribeContextualVoice = { file, token ->
                            activity.transcribeContextualChatAudio(file, token).also { modelStatus = it }
                        },
                        cancelContextualVoice = { id ->
                            activity.cancelContextualChatTranscription(id).also { modelStatus = it }
                        },
                        clearContextualVoice = { id ->
                            activity.clearContextualChatTranscription(id).also { modelStatus = it }
                        },
                        askMeal = { text -> activity.askLocalMealAssistant(text).also { modelStatus = it } },
                        askCoach = { question, snapshot, conversationContext -> activity.askPrivateCoach(question, snapshot, conversationContext).also { modelStatus = it } },
                        startAgent = { question, history, confirmation, portionEditContext, foodEditContext ->
                            val selectedId = selectedAskMealId
                            val selectedTool = MetriaAgentGroundingPolicy.requiredTool(
                                question, history, selectedMealId = selectedId,
                            )
                            pendingSelectedMealReadId = selectedId.takeIf { selectedTool?.name == "read_selected_meal" }
                            selectedAskMealId = null
                            selectedAskMealTitle = null
                            activity.startPrivateAgentTurn(
                                question, history, selectedId, confirmation, portionEditContext, foodEditContext,
                                unlocked = status is EncryptedVault.Status.Unlocked,
                                conversationGeneration = assistantConversationSession.conversationGeneration.value,
                            )
                                .also { modelStatus = it }
                        },
                        continueAgentTool = activity::continuePrivateAgentTool,
                        resumeSavedDishProbe = activity::resumeSavedDishProbeAsNormalAgentTurn,
                        completeCompanionNavigation = { requestId ->
                            if (status is EncryptedVault.Status.Unlocked &&
                                assistantConversationSession.isCurrentNavigationRequest(requestId)) {
                                val target = modelStatus.companionNavigation
                                    ?.takeIf { it.requestId == requestId }?.destination
                                val completed = activity.completeCompanionNavigation(requestId)
                                if (completed != null) {
                                    modelStatus = completed
                                    when (target) {
                                        CompanionAssistantPolicy.Destination.TODAY -> destination = "Today"
                                        CompanionAssistantPolicy.Destination.HISTORY -> destination = "History"
                                        CompanionAssistantPolicy.Destination.PRIVACY -> destination = "Privacy"
                                        null -> Unit
                                    }
                                    quickAssistantOpen = false
                                }
                                completed != null
                            } else {
                                false
                            }
                        },
                        isVaultUnlocked = { status is EncryptedVault.Status.Unlocked },
                        isSavedDishCurrent = { expected ->
                            val unlocked = vault.status() as? EncryptedVault.Status.Unlocked
                            unlocked?.personalDishes?.singleOrNull { it.id == expected.id } == expected
                        },
                        onReviewSavedDish = { expected ->
                            editMealFromAssistant = false
                            val unlocked = vault.status() as? EncryptedVault.Status.Unlocked
                            val dish = unlocked?.personalDishes?.singleOrNull { it.id == expected.id }
                                ?.takeIf { it == expected }
                            val prefill = dish?.let(CompanionAssistantPolicy::savedDishPrefill)
                            if (prefill != null) {
                                savedDishPrefill = prefill
                                pendingMealSlot = null
                                pendingMealDate = null
                                mealProposalForConfirmation = MealProposal(
                                    items = emptyList(), assumptions = emptyList(), questions = emptyList(), overallConfidence = "high",
                                )
                                mealReviewFromAssistant = true
                                manualMealReview = true
                                mealReviewIsTextEntry = true
                                mealReviewIsPhotoEntry = false
                                quickAssistantOpen = false
                                true
                            } else {
                                false
                            }
                        },
                        understandCoach = { question, conversationContext -> activity.understandPrivateCoach(question, conversationContext).also { modelStatus = it } },
                        understandCoachAudio = { audio, conversationContext -> activity.understandPrivateCoachAudio(audio, conversationContext).also { modelStatus = it } },
                        cancelAssistant = { pendingSelectedMealReadId = null; modelStatus = activity.cancelPrivateAssistant() },
                        saveWeightDirect = { kilograms, date ->
                            val entry = WeightEntry(kilograms = kilograms, recordedAt = date)
                            status = vault.addWeightEntry(entry)
                            if (status is EncryptedVault.Status.Unlocked) entry else null
                        },
                        onReviewVoiceWeightDraft = { kilograms, date ->
                            voiceDraft = VoiceWeightDraft(kilograms, date)
                            addSheet = true
                        },
                        editSavedWeight = { editEntry = it; quickAssistantOpen = false },
                        undoSavedWeight = { entry -> status = vault.deleteWeight(entry.id) },
                        reviewMeal = { proposal, portionOverride, foodReplacementOverride ->
                            portionOverrideForConfirmation = portionOverride
                            foodReplacementForConfirmation = foodReplacementOverride
                            mealReviewFromAssistant = true
                            mealProposalForConfirmation = proposal
                            quickAssistantOpen = false
                        },
                        openSavedMeal = { id -> vaultStatus.meals.firstOrNull { it.id == id }?.let {
                            editMeal = it
                            editMealFromAssistant = true
                        } },
                        discardMealDraft = { modelStatus = activity.discardAssistantMealDraft() },
                        selectedMealContextTitle = selectedAskMealTitle,
                        clearSelectedMealContext = { selectedAskMealId = null; selectedAskMealTitle = null },
                        readSelectedMeal = { replyLanguage ->
                            val selectedId = pendingSelectedMealReadId
                            pendingSelectedMealReadId = null
                            if (selectedId == null) null else {
                                val freshMeals = (vault.status() as? EncryptedVault.Status.Unlocked)?.meals.orEmpty()
                                SelectedSavedMealReply.read(freshMeals, selectedId, replyLanguage)
                                    ?: mapOf("source" to "metria_local_vault", "record_id" to selectedId, "reply_language" to replyLanguage)
                            }
                        },
                    )
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
                editMeal?.let { meal -> SavedMealEditorScreen(
                    meal,
                    catalog = foodCatalog,
                    foodNameCorrection = modelStatus.mealFoodCorrection,
                    onSuggestFoodName = { currentName, correction ->
                        activity.suggestMealFoodName(currentName, correction).also { modelStatus = it }.mealFoodCorrection
                    },
                    onCancelFoodNameCorrection = { requestId -> modelStatus = activity.cancelMealFoodNameCorrection(requestId) },
                    onClearFoodNameCorrection = { modelStatus = activity.clearMealFoodNameCorrection() },
                    onDismiss = {
                        modelStatus = activity.clearMealFoodNameCorrection()
                        editMeal = null
                        editMealFromAssistant = false
                    },
                    onAskAboutMeal = {
                        val fresh = vault.status() as? EncryptedVault.Status.Unlocked
                        val exact = fresh?.meals?.singleOrNull { it.id == meal.id }
                        if (exact == null) false else {
                            modelStatus = activity.clearMealFoodNameCorrection()
                            selectedAskMealId = exact.id
                            selectedAskMealTitle = MealIdentity.displayTitle(exact)
                            editMeal = null
                            editMealFromAssistant = false
                            quickAssistantOpen = true
                            modelStatus = activity.modelStatus()
                            true
                        }
                    },
                    onSave = { updated ->
                        val shouldAcknowledgeInAssistant = editMealFromAssistant
                        val sessionAtTap = assistantConversationSession.conversationGeneration.value
                        val mealIdAtTap = editMeal?.id
                        val update = if (shouldAcknowledgeInAssistant) {
                            assistantConversationSession.withWriterIdle {
                                if (vault.status() !is EncryptedVault.Status.Unlocked ||
                                    sessionAtTap != assistantConversationSession.conversationGeneration.value ||
                                    editMeal?.id != mealIdAtTap || !editMealFromAssistant
                                ) EncryptedVault.Status.Failed("This meal editor is no longer current. Reopen the meal before saving.")
                                else {
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    val saved = vault.updateMeal(
                                        updated, recordInConversation = true, createReceiptOnlyConversation = true,
                                    )
                                    if (saved is EncryptedVault.Status.Unlocked && sessionAtTap == assistantConversationSession.conversationGeneration.value) {
                                        assistantConversationSession.synchronize(saved.activeConversation, saved.conversationExpired)
                                    }
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    saved
                                }
                            }
                        } else vault.updateMeal(updated)
                        when (update) {
                            is EncryptedVault.Status.Unlocked -> {
                                status = update
                                editMeal = null
                                editMealFromAssistant = false
                                if (shouldAcknowledgeInAssistant) {
                                    assistantConversationSession.synchronize(update.activeConversation, update.conversationExpired)
                                    quickAssistantOpen = true
                                }
                                null
                            }
                            is EncryptedVault.Status.Failed -> update.message
                            else -> "Unlock Metria and try saving again."
                        }
                    },
                    onDelete = {
                        when (val deleted = vault.deleteMeal(meal.id)) {
                            is EncryptedVault.Status.Unlocked -> {
                                status = deleted
                                if (selectedAskMealId == meal.id || pendingSelectedMealReadId == meal.id) {
                                    selectedAskMealId = null
                                    selectedAskMealTitle = null
                                    pendingSelectedMealReadId = null
                                }
                                editMeal = null
                                editMealFromAssistant = false
                                null
                            }
                            is EncryptedVault.Status.Failed -> deleted.message
                            else -> "Unlock Metria and try deleting this meal again."
                        }
                    },
                ) }
                val activeMealReview = if (manualMealReview && savedDishPrefill == null) {
                    MealReviewMode.blankManualProposal()
                } else mealProposalForConfirmation
                activeMealReview?.let { proposal -> MetriaMealReviewSheet(
                    proposal = proposal,
                    initialPortionOverride = portionOverrideForConfirmation?.takeIf {
                        it.originalProposal == proposal && mealReviewFromAssistant &&
                            savedDishPrefill == null && !manualMealReview &&
                            !mealReviewIsPhotoEntry && !mealReviewIsTextEntry
                    },
                    initialFoodReplacementOverride = foodReplacementForConfirmation?.takeIf {
                        it.originalProposal == proposal && mealReviewFromAssistant &&
                            savedDishPrefill == null && !manualMealReview &&
                            !mealReviewIsPhotoEntry && !mealReviewIsTextEntry
                    },
                    manualEntry = manualMealReview,
                    composeDialogNavigationInsets = mealReviewFromAssistant || assistantCapturePurpose != null,
                    suggestMealSlotFromTime = mealReviewFromAssistant || assistantCapturePurpose != null,
                    initialMealSlot = pendingMealSlot,
                    initialRecordedDate = pendingMealDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    catalog = foodCatalog,
                    corrections = vaultStatus.foodCorrections,
                    referenceThumbnailBase64 = modelStatus.referenceThumbnailBase64.takeUnless { mealReviewIsTextEntry },
                    initialSavedDishPrefill = savedDishPrefill,
                    analysisAuditId = modelStatus.latestImageAudit?.id.takeUnless { manualMealReview || mealReviewIsTextEntry },
                    savedMeals = vaultStatus.meals,
                    savedPersonalDishes = vaultStatus.personalDishes,
                    findPersonalDishMatches = { query -> PersonalDishMatcher.search(query, vaultStatus.personalDishes) },
                    foodNameCorrection = modelStatus.mealFoodCorrection,
                    onSuggestFoodName = { currentName, correction ->
                        activity.suggestMealFoodName(currentName, correction).also { modelStatus = it }.mealFoodCorrection
                    },
                    onCancelFoodNameCorrection = { requestId ->
                        modelStatus = activity.cancelMealFoodNameCorrection(requestId)
                    },
                    onClearFoodNameCorrection = { modelStatus = activity.clearMealFoodNameCorrection() },
                    onDismiss = {
                        modelStatus = activity.clearMealFoodNameCorrection()
                        mealProposalForConfirmation = null
                        portionOverrideForConfirmation = null
                        savedDishPrefill = null
                        if (mealReviewFromAssistant) quickAssistantOpen = true
                        mealReviewFromAssistant = false
                        mealReviewIsTextEntry = false
                        val returnToPhotoEntry = mealReviewIsPhotoEntry
                        mealReviewIsPhotoEntry = false
                        manualMealReview = false
                        if (!returnToPhotoEntry) {
                            pendingMealSlot = null
                            pendingMealDate = null
                        }
                    },
                    onSave = { meal, corrections, personalDishes, correctionMemory ->
                        val captureSave = assistantCapturePurpose != null
                        val saveInAssistantConversation = mealReviewFromAssistant || captureSave
                        val sessionAtTap = assistantConversationSession.conversationGeneration.value
                        val proposalAtTap = proposal
                        val savedDishAtTap = savedDishPrefill
                        val result = if (saveInAssistantConversation) {
                            assistantConversationSession.withWriterIdle {
                                val reviewStillCurrent = if (manualMealReview && savedDishAtTap == null) {
                                    manualMealReview && activeMealReview == proposalAtTap
                                } else mealProposalForConfirmation == proposalAtTap
                                if (vault.status() !is EncryptedVault.Status.Unlocked || sessionAtTap != assistantConversationSession.conversationGeneration.value ||
                                    !reviewStillCurrent || savedDishPrefill != savedDishAtTap ||
                                    captureSave != (assistantCapturePurpose != null) ||
                                    (captureSave && assistantCapturePurpose == null)
                                ) {
                                    EncryptedVault.Status.Failed("This review is no longer current. Reopen it before saving.")
                                } else {
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    val saved = vault.saveReviewedMeal(
                                        meal, corrections, personalDishes, correctionMemory,
                                        recordInConversation = true,
                                        createReceiptOnlyConversation = captureSave || savedDishAtTap != null,
                                    )
                                    if (saved is EncryptedVault.Status.Unlocked && sessionAtTap == assistantConversationSession.conversationGeneration.value) {
                                        assistantConversationSession.synchronize(saved.activeConversation, saved.conversationExpired)
                                    }
                                    assistantConversationSession.invalidatePendingConversationWrites()
                                    saved
                                }
                            }
                        } else vault.saveReviewedMeal(meal, corrections, personalDishes, correctionMemory)
                        if (result is EncryptedVault.Status.Unlocked) {
                            status = result
                            if (assistantCapturePurpose != null) {
                                assistantCapturePurpose = null
                                quickAssistantOpen = true
                            }
                            if (pendingMealSlot != null && !mealReviewFromAssistant) destination = "Today"
                            if (saveInAssistantConversation) {
                                assistantConversationSession.acceptedPortionOverride.value = null
                                assistantConversationSession.acceptedFoodReplacementOverride.value = null
                                quickAssistantOpen = true
                            }
                            mealProposalForConfirmation = null
                            portionOverrideForConfirmation = null
                            foodReplacementForConfirmation = null
                            savedDishPrefill = null
                            mealReviewFromAssistant = false
                            mealReviewIsTextEntry = false
                            mealReviewIsPhotoEntry = false
                            manualMealReview = false
                            pendingMealSlot = null
                            pendingMealDate = null
                            modelStatus = activity.clearLocalMealDraft()
                            null
                        } else {
                            (result as? EncryptedVault.Status.Failed)?.message
                                ?: "Unlock your private vault and try saving again."
                        }
                    },
                ) }
                labelDraftForConfirmation?.let { label -> LabelConfirmationSheet(
                    draft = label,
                    referenceThumbnailBase64 = modelStatus.referenceThumbnailBase64,
                    initialMealSlot = pendingMealSlot,
                    initialRecordedDate = pendingMealDate,
                    onDismiss = { labelDraftForConfirmation = null },
                    onSave = { meal ->
                        status = vault.addMeal(meal)
                        labelDraftForConfirmation = null
                        if (status is EncryptedVault.Status.Unlocked && pendingMealSlot != null) {
                            destination = "Today"
                            pendingMealSlot = null
                            pendingMealDate = null
                        }
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
                if (editNutritionTargets) app.metria.ui.screens.NutritionGoalsScreen(
                    targets = vaultStatus.nutritionTargets,
                    setup = vaultStatus.goalSetup,
                    latestWeightKg = vaultStatus.entries.maxByOrNull(WeightEntry::recordedAt)?.kilograms,
                    weightEntries = vaultStatus.entries,
                    meals = vaultStatus.meals,
                    showWelcomeInitially = showGoalWelcomeInitially,
                    displayUnit = vaultStatus.displayUnit,
                    onDismiss = { editNutritionTargets = false; showGoalWelcomeInitially = false },
                    onSave = { targets, calorieWasEdited, manuallyEditedMacros ->
                        when (val result = withContext(Dispatchers.IO) { vault.setNutritionTargets(targets, calorieWasEdited, manuallyEditedMacros) }) {
                            is EncryptedVault.Status.Unlocked -> { status = result; null }
                            is EncryptedVault.Status.Failed -> "Daily targets could not be saved locally. Try again."
                            else -> { status = result; "Unlock Metria before saving targets." }
                        }
                    },
                    onConfirmGoal = { phase, newPhase ->
                        when (val result = withContext(Dispatchers.IO) { vault.confirmGoal(phase, newPhase) }) {
                            is EncryptedVault.Status.Unlocked -> { status = result; null }
                            is EncryptedVault.Status.Failed -> "Your goal could not be saved locally. Try again."
                            else -> { status = result; "Unlock Metria before saving your goal." }
                        }
                    },
                    onDeleteSetup = {
                        when (val result = withContext(Dispatchers.IO) { vault.deleteGoalSetup() }) {
                            is EncryptedVault.Status.Unlocked -> { status = result; null }
                            is EncryptedVault.Status.Failed -> "Goal data could not be deleted. Try again."
                            else -> { status = result; "Unlock Metria before changing goals." }
                        }
                    },
                    onDeleteHistory = { id ->
                        when (val result = withContext(Dispatchers.IO) { vault.deleteGoalHistory(id) }) {
                            is EncryptedVault.Status.Unlocked -> { status = result; null }
                            is EncryptedVault.Status.Failed -> "This phase could not be deleted. Try again."
                            else -> { status = result; "Unlock Metria before changing goals." }
                        }
                    },
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
                    text = { Text("The archive contains ${preview.weights} weight entries, ${preview.meals} meals and ${preview.goalPhases} goal phases${if (preview.hasGoalProfile) ", including calculation profiles" else ""}. This replaces the current records, daily references, profile and goal history on this phone. An older archive without goal data removes the current profile and history. Model files, chat, diagnostics, and device keys are not restored.") },
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
private fun AssistantSetupGate(
    modelStatus: ModelStatus,
    cloudPhotoMode: Boolean = false,
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
            Text(if (cloudPhotoMode) "Set up Metria" else "Set up your private assistant", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 20.dp))
            if (cloudPhotoMode) {
                Text("Meal photos will use cloud analysis after you connect a private pass in Settings. The selected photo is sent only after you confirm each analysis. You can choose on-device analysis any time.", modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
                Button(onClick = continueToApp, modifier = Modifier.fillMaxWidth()) { Text("Protect my records") }
            } else when (modelStatus.state) {
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
    onOpenConversation: () -> Unit,
    assistantConversationSession: PrivateAssistantConversationSession,
    onConversationChanged: suspend (List<PrivateConversationMessage>) -> Boolean,
    onNewConversation: suspend () -> Boolean,
    modelStatus: ModelStatus,
    photoAnalysisMode: String,
    cloudPhotoConfigured: Boolean,
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
    askMeal: (String) -> ModelStatus,
    askCoach: (String, PrivateCoachSnapshot, String) -> ModelStatus,
    startAgent: (String, List<PrivateConversationMessage>, NativeMealDraftPolicy.MealConfirmationContext?, ChatCurrentDraftPortionContext?, ChatCurrentDraftFoodContext?) -> ModelStatus,
    continueAgentTool: (String, Map<String, Any?>) -> ModelStatus,
    understandCoach: (String, String) -> ModelStatus,
    understandCoachAudio: (java.io.File, String) -> ModelStatus,
    cancelAssistant: () -> Unit,
    saveWeightDirect: (Double, Instant) -> WeightEntry?,
    editSavedWeight: (WeightEntry) -> Unit,
    undoSavedWeight: (WeightEntry) -> Unit,
    openFoodCatalog: () -> Unit,
    review: (MealProposal) -> Unit,
    onManualMealReview: () -> Unit,
    prepareImage: (android.net.Uri) -> Unit,
    analyzeImage: (String) -> ModelStatus,
    cancelImageAnalysis: () -> Unit,
    readLabel: () -> Unit,
    discardImage: () -> Unit,
    discardImageMealDraft: () -> Unit,
    discardAssistantMealDraft: () -> Unit,
    reviewLabel: (LabelNutritionDraft) -> Unit,
    newCameraUri: () -> android.net.Uri,
    prepareCapturedImage: (android.net.Uri) -> Unit,
    discardPendingCapture: () -> Unit,
    beginExternalImageFlow: () -> Unit,
    beginImageFlow: () -> ModelStatus,
    finishCooldown: () -> Unit,
    contextualMealSlot: String? = null,
    onBackFromContextualMeal: () -> Unit = {},
) {
    var sourceChoice by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImagePurpose by rememberSaveable { mutableStateOf<String?>(null) }
    var photoDescription by rememberSaveable { mutableStateOf("") }
    var capturedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showOcrText by remember { mutableStateOf(false) }
    var coolingSecondsLeft by remember { mutableStateOf(0) }
    val photoReviewHandoff = app.metria.ui.screens.rememberPhotoMealReviewHandoff(modelStatus, review)
    val stopPhotoAnalysis = { photoReviewHandoff.cancel(); cancelImageAnalysis() }
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
        if (modelStatus.state == "loaded" || photoAnalysisMode == "cloud") {
            if (beginImageFlow().imageState != "cooling_down") sourceChoice = purpose
            onInitialImagePurposeHandled()
        }
    }
    LaunchedEffect(modelStatus.imageState) {
        if (modelStatus.imageState == "none") {
            pendingImagePurpose = null
            photoDescription = ""
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
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "reading_image" -> "Looking for the clearest foods." to "This may take a moment while Metria reads the photo."
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "identifying_foods" -> "Identifying the main foods." to "Metria is separating the visible ingredients into your draft."
        modelStatus.imageState == "analyzing" && modelStatus.imageProgress == "estimating_portions" -> "Estimating portions and preparation." to "You will be able to adjust every estimate before saving."
        modelStatus.imageState == "analyzing" -> "Checking your editable draft." to "Metria is making sure the review is ready to show."
        modelStatus.imageState == "reading_label" -> "Metria is reading your label." to "Text is read locally and must be reviewed."
        else -> null
    }
    if (loading != null) {
        if (contextualMealSlot != null) {
            app.metria.ui.screens.TodayMealPhotoLoadingScreen(
                slotLabel = contextualMealSlot,
                onBack = onBackFromContextualMeal,
                message = when {
                    modelStatus.imageState == "analyzing" -> "Preparing your meal…"
                    modelStatus.imageState == "reading_label" -> "Reading your nutrition label…"
                    modelStatus.imageState == "preparing" -> "Preparing your photo…"
                    else -> "Preparing photo tools…"
                },
                onStop = when (modelStatus.imageState) {
                    "analyzing" -> stopPhotoAnalysis
                    "reading_label" -> discardImage
                    else -> null
                },
                cloud = when (modelStatus.imageState) {
                    "analyzing" -> photoAnalysisMode == "cloud"
                    "reading_label" -> false
                    else -> null
                },
                localName = if (modelStatus.imageState == "reading_label") "ML Kit" else "Gemma",
            )
        } else if (modelStatus.imageState == "analyzing") {
            app.metria.ui.screens.MealPhotoAnalysisProgress(loading.first, loading.second, stopPhotoAnalysis, cloud = photoAnalysisMode == "cloud")
        } else AppleByteLoading(loading.first, loading.second)
        return
    }
    if (modelStatus.imageState == "cooling_down") {
        if (contextualMealSlot != null) app.metria.ui.screens.TodayMealPhotoLoadingScreen(
            slotLabel = contextualMealSlot,
            onBack = onBackFromContextualMeal,
            message = "Photo tools are cooling down. Wait ${coolingSecondsLeft}s or come back later.",
        ) else CoolingDownLoading(coolingSecondsLeft)
        return
    }
    if (photoReviewHandoff.ready(modelStatus)) {
        app.metria.ui.screens.MealPhotoAnalysisProgress("Preparing your meal…", "", stopPhotoAnalysis, cloud = photoAnalysisMode == "cloud")
        return
    }
    if (contextualMealSlot != null) app.metria.ui.screens.TodayMealPhotoEntryScreen(
        slotLabel = contextualMealSlot,
        onBack = onBackFromContextualMeal,
        onMealPhoto = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "meal" },
        onNutritionLabel = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "label" },
    ) else MetriaAssistantHome(
        modelStatus = modelStatus,
        onDownload = downloadAssistant,
        onCancelDownload = cancelAssistantDownload,
        onOpenChat = onOpenConversation,
        onSearchFoods = openFoodCatalog,
        onScanLabel = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "label" },
        onAnalyseMeal = { if (beginImageFlow().imageState != "cooling_down") sourceChoice = "meal" },
    )
    sourceChoice?.let { choice ->
        app.metria.ui.screens.MealPhotoSourceSheet(
                purpose = choice,
                onDismiss = { sourceChoice = null },
                onTakePhoto = {
                    val uri = newCameraUri()
                    capturedUri = uri
                    pendingImagePurpose = choice
                    photoDescription = ""
                    sourceChoice = null
                    camera.launch(uri)
                },
                onChoosePhoto = {
                    sourceChoice = null
                    pendingImagePurpose = choice
                    photoDescription = ""
                    beginExternalImageFlow()
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
        )
    }
    if (pendingImagePurpose != null && modelStatus.imageState in setOf("preparing", "ready", "failed")) {
        app.metria.ui.screens.MealPhotoPreviewSheet(
            purpose = requireNotNull(pendingImagePurpose),
            thumbnail = modelStatus.referenceThumbnailBase64.takeIf { modelStatus.imageState == "ready" },
            imageState = modelStatus.imageState,
            photoAnalysisMode = photoAnalysisMode,
            cloudPhotoConfigured = cloudPhotoConfigured,
            description = photoDescription,
            onDescriptionChange = { photoDescription = it },
            onPurposeChange = { pendingImagePurpose = it; photoDescription = "" },
            onDismiss = { pendingImagePurpose = null; photoDescription = ""; discardImage() },
            onReplace = {
                val purpose = pendingImagePurpose
                pendingImagePurpose = null
                photoDescription = ""
                discardImage()
                sourceChoice = purpose
            },
            onAnalyze = {
                if (pendingImagePurpose == "label") readLabel() else {
                    val result = analyzeImage(photoDescription)
                    photoReviewHandoff.started(modelStatus, result)
                }
                pendingImagePurpose = null
                photoDescription = ""
            },
        )
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
                Text(
                    if (proposal.omittedItemCount > 0) "Some suggestions were left out. Add any missing foods during review."
                    else "This fast draft lists the strongest visible foods. Add or edit anything else before saving.",
                    color = Color(0xFF655D6E), style = MaterialTheme.typography.bodySmall,
                )
                Text("Nothing has been saved. Review every portion before confirming.", color = Plum)
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
                MealDraftDiscardAction(onDiscard = discardAssistantMealDraft)
            }
        }
    }
    if (modelStatus.proposalDiagnostic != null || modelStatus.latestImageAudit?.outcome == "runtime_failure") {
        MealAnalysisFallbackCard(onEnterManually = onManualMealReview)
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
                Text(
                    if (proposal.omittedItemCount > 0) "Some suggestions were left out. Add any missing foods and review portions before confirming. The temporary image was discarded."
                    else "The temporary image was discarded. Review every portion before confirming.",
                    color = Plum,
                )
                Button(onClick = { review(proposal) }, modifier = Modifier.fillMaxWidth()) { Text("Review and confirm meal") }
                MealDraftDiscardAction(onDiscard = discardImageMealDraft)
            }
        }
    }
}

/** UX decision: facts returned by a local tool remain visible, but secondary to Metria's answer. */
private data class PrivateChatMessage(
    val text: String,
    val fromMember: Boolean,
    val localEvidence: Boolean = false,
    val savedMealId: String? = null,
)

private fun initialPrivateChatMessages() = listOf(
    PrivateChatMessage("Ask about a meal, add a weight, or ask what you logged today. Any saved action stays visible and reversible.", false),
)

/** Small, non-Compose gate for the optimistic typed-send lifecycle. */
internal class AssistantPendingSubmissionGate {
    private var nextToken = 0L
    private var activeToken: Long? = null
    private var lifecycleRequestId: Long? = null
    private var observedTerminalRequestId: Long? = null

    @Synchronized
    fun begin(): Long? {
        if (activeToken != null) return null
        val token = ++nextToken
        activeToken = token
        lifecycleRequestId = null
        observedTerminalRequestId = null
        return token
    }

    @Synchronized
    fun isActive(token: Long): Boolean = activeToken == token

    @Synchronized
    fun reconcile(
        token: Long,
        requestId: Long?,
        previousRequestId: Long?,
        state: String,
        operation: String?,
        cloudReady: Boolean = false,
    ): Long? {
        if (activeToken != token) return null
        val accepted = requestId != null && requestId != previousRequestId &&
            (state in setOf("thinking", "loaded") || cloudReady) &&
            operation?.let { it.startsWith("coach") || it.startsWith("agent") } == true
        if (!accepted) {
            activeToken = null
            lifecycleRequestId = null
            observedTerminalRequestId = null
            return null
        }
        lifecycleRequestId = requestId
        val cloudTurnInFlight = cloudReady && operation in setOf("agent_turn", "coach_intent")
        if (observedTerminalRequestId == requestId || (state == "loaded" && !cloudTurnInFlight) ||
            (cloudReady && !cloudTurnInFlight && operation?.startsWith("agent") == true)
        ) {
            activeToken = null
            lifecycleRequestId = null
            observedTerminalRequestId = null
        }
        return requestId
    }

    @Synchronized
    fun observeLifecycle(requestId: Long?, state: String, operation: String? = null, cloudReady: Boolean = false): Boolean {
        val cloudTurnInFlight = cloudReady && operation in setOf("agent_turn", "coach_intent")
        val available = state in setOf("thinking", "loaded") || (cloudReady && !cloudTurnInFlight)
        if (activeToken == null || !available) return false
        if (lifecycleRequestId == null) {
            if (state == "loaded" || (cloudReady && !cloudTurnInFlight)) observedTerminalRequestId = requestId
            return false
        }
        if (requestId != lifecycleRequestId) return false
        activeToken = null
        lifecycleRequestId = null
        observedTerminalRequestId = null
        return true
    }

    @Synchronized
    fun cancel(token: Long? = activeToken): Boolean {
        if (token == null || activeToken != token) return false
        activeToken = null
        lifecycleRequestId = null
        observedTerminalRequestId = null
        return true
    }
}

/** Keeps Stop responsive while a native cancellation may wait for LiteRT to leave inference. */
internal class AssistantCancellationGate {
    private var generation = 0L
    private var activeToken: Long? = null

    @Synchronized
    fun begin(): Long? {
        if (activeToken != null) return null
        return (++generation).also { activeToken = it }
    }

    @Synchronized
    fun reset() {
        generation += 1
    }

    @Synchronized
    fun isInFlight(): Boolean = activeToken != null

    /** Returns true only when this completion still belongs to the current conversation. */
    @Synchronized
    fun complete(token: Long): Boolean {
        if (activeToken != token) return false
        activeToken = null
        return generation == token
    }
}

/** Serializes native assistant starts and teardown, including callers queued before Stop. */
internal class AssistantModelOperationMutex {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        mutex.lock()
        try {
            return block()
        } finally {
            mutex.unlock()
        }
    }
}

internal data class PendingAssistantSubmission(
    val token: Long,
    val text: String,
    val previousRequestId: Long?,
)

private enum class AutomaticVoiceWeightActionDisposition { ORDINARY, REVIEW, REJECT }

@Stable
private class PrivateAssistantConversationSession(
    val recoverableDiagnostic: MutableState<String?>,
    initialInput: String = "",
    initialSubmittedQuestion: String = "",
    initialSubmittedContext: String = "",
    initialActiveRequestId: Long? = null,
    initialHandledToolRequestId: Long? = null,
    initialHandledReplyRequestId: Long? = null,
    initialSavedWeightId: String? = null,
    initialVoiceMessage: String? = null,
    initialMessages: List<PrivateChatMessage> = initialPrivateChatMessages(),
) {
    private val persistence = AssistantConversationPersistence()
    private val writeReceipts = AssistantConversationWriteReceipts()
    private var hydrated = false
    private var loadedConversationRecord: PrivateConversationRecord? = null
    val input = mutableStateOf(initialInput)
    val submittedQuestion = mutableStateOf(initialSubmittedQuestion)
    val submittedContext = mutableStateOf(initialSubmittedContext)
    val activeRequestId = mutableStateOf(initialActiveRequestId)
    val handledToolRequestId = mutableStateOf(initialHandledToolRequestId)
    val handledReplyRequestId = mutableStateOf(initialHandledReplyRequestId)
    /** Member-confirmed grams are session-only and remain bound to their original Chat draft. */
    val acceptedPortionOverride = mutableStateOf<ChatPortionOverride?>(null)
    /** Corrected labels remain session-only and bound to the original unsaved proposal. */
    val acceptedFoodReplacementOverride = mutableStateOf<ChatFoodReplacementOverride?>(null)
    /** One accepted meal candidate; never reconstructed from persisted Chat history. */
    val pendingMealConfirmation = mutableStateOf<NativeMealDraftPolicy.MealConfirmationContext?>(null)
    /** Transient speech origin carried across Chat composition until its request ID is known. */
    val pendingAutomaticVoiceSubmission = mutableStateOf<PendingAutomaticVoiceSubmission?>(null)
    /** Exact unsent transcript retained across Chat recreation when automatic send must wait. */
    val automaticVoiceComposerText = mutableStateOf<String?>(null)
    /** Spoken weight actions must be presented as an editable draft before saving. */
    val automaticVoiceWeightReview = mutableStateOf<OneUseAutomaticVoiceWeightReview?>(null)
    val conversationGeneration = mutableStateOf(0L)
    val retainedFailureAnnouncementRequestId = mutableStateOf<Long?>(null)
    val savedWeightId = mutableStateOf(initialSavedWeightId)
    val voiceMessage = mutableStateOf(initialVoiceMessage)
    val messages = mutableStateOf(initialMessages)
    val pendingSubmission = mutableStateOf<PendingAssistantSubmission?>(null)
    val assistantStopping = mutableStateOf(false)
    val responseStopped = mutableStateOf(false)
    private val pendingSubmissionGate = AssistantPendingSubmissionGate()
    private val assistantCancellationGate = AssistantCancellationGate()
    private val assistantModelOperationMutex = AssistantModelOperationMutex()
    private var pendingSubmissionJob: Job? = null
    val pendingContinuation = mutableStateOf(false)
    private val pendingContinuationGate = AssistantPendingSubmissionGate()
    private var pendingContinuationToken: Long? = null

    suspend fun persistConversation(
        snapshot: List<PrivateConversationMessage>,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
    ): ConversationPersistenceResult = persistence.persist(snapshot, save)

    fun captureConversationWriteTicket(): Long? = persistence.captureWriteTicket()

    suspend fun <T> withWriterIdle(block: () -> T): T = persistence.withWriterIdle(block)

    fun invalidatePendingConversationWrites() = persistence.invalidatePendingWrites()

    suspend fun persistConversation(
        ticket: Long,
        snapshot: List<PrivateConversationMessage>,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
    ): ConversationPersistenceResult = persistence.persist(ticket, snapshot, save)

    /** Register the exact successful vault record before the serialized write releases its gate. */
    suspend fun saveConversationSnapshot(
        snapshot: List<PrivateConversationMessage>,
        save: (List<PrivateConversationMessage>) -> EncryptedVault.Status,
    ): Boolean = writeReceipts.saveSnapshot(snapshot, save)

    suspend fun clearConversation(clear: suspend () -> Boolean): ConversationPersistenceResult =
        persistence.clear(clear)

    suspend fun persistBeforeNavigation(
        requestId: Long,
        transcript: List<PrivateConversationMessage>,
        isCurrent: () -> Boolean,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
        navigate: (Long) -> Boolean,
        repeatable: Boolean = false,
    ): ConversationNavigationResult = persistence.persistBeforeNavigation(
        requestId, transcript, isCurrent, save, navigate, repeatable,
    )

    fun isCurrentNavigationRequest(requestId: Long): Boolean =
        activeRequestId.value == requestId && handledReplyRequestId.value == requestId

    fun invalidateForConversationClear() {
        conversationGeneration.value += 1
        clearAutomaticVoiceWriteOrigin()
        writeReceipts.invalidate()
        acceptedPortionOverride.value = null
        acceptedFoodReplacementOverride.value = null
        pendingMealConfirmation.value = null
        persistence.invalidatePendingWrites()
        activeRequestId.value = null
        handledToolRequestId.value = null
        handledReplyRequestId.value = null
        retainedFailureAnnouncementRequestId.value = null
        pendingSubmissionGate.cancel()
        pendingSubmission.value = null
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = null
        pendingContinuationGate.cancel(pendingContinuationToken)
        pendingContinuation.value = false
        pendingContinuationToken = null
    }

    fun clearAutomaticVoiceWriteOrigin() {
        pendingAutomaticVoiceSubmission.value = null
        automaticVoiceWeightReview.value = null
        automaticVoiceComposerText.value = null
    }

    fun guardAutomaticVoiceWeightAction(
        requestId: Long,
        currentConversationGeneration: Long,
    ): AutomaticVoiceWeightActionDisposition {
        val review = automaticVoiceWeightReview.value
        if (review?.consume(requestId, currentConversationGeneration) == true) {
            automaticVoiceWeightReview.value = null
            return AutomaticVoiceWeightActionDisposition.REVIEW
        }
        if (review != null) return AutomaticVoiceWeightActionDisposition.REJECT
        val pending = pendingAutomaticVoiceSubmission.value ?: return AutomaticVoiceWeightActionDisposition.ORDINARY
        val matchesCurrentSubmission = pending.conversationGeneration == currentConversationGeneration &&
            pending.submissionToken == pendingSubmission.value?.token
        if (matchesCurrentSubmission) {
            pendingAutomaticVoiceSubmission.value = null
            return AutomaticVoiceWeightActionDisposition.REVIEW
        }
        return AutomaticVoiceWeightActionDisposition.REJECT
    }

    fun beginPendingSubmission(text: String, previousRequestId: Long?): PendingAssistantSubmission? {
        if (assistantCancellationGate.isInFlight()) return null
        acceptedPortionOverride.value = null
        acceptedFoodReplacementOverride.value = null
        val token = pendingSubmissionGate.begin() ?: return null
        clearAutomaticVoiceWriteOrigin()
        retainedFailureAnnouncementRequestId.value = null
        responseStopped.value = false
        return PendingAssistantSubmission(token, text, previousRequestId).also { pendingSubmission.value = it }
    }

    fun beginAssistantCancellation(): Long? {
        val token = assistantCancellationGate.begin() ?: return null
        clearAutomaticVoiceWriteOrigin()
        acceptedPortionOverride.value = null
        acceptedFoodReplacementOverride.value = null
        pendingMealConfirmation.value = null
        pendingSubmissionGate.cancel(pendingSubmission.value?.token)
        pendingSubmission.value = null
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = null
        pendingContinuationGate.cancel(pendingContinuationToken)
        pendingContinuation.value = false
        pendingContinuationToken = null
        activeRequestId.value = null
        handledToolRequestId.value = null
        handledReplyRequestId.value = null
        retainedFailureAnnouncementRequestId.value = null
        recoverableDiagnostic.value = null
        responseStopped.value = false
        assistantStopping.value = true
        return token
    }

    fun finishAssistantCancellation(token: Long) {
        val belongsToCurrentConversation = assistantCancellationGate.complete(token)
        assistantStopping.value = assistantCancellationGate.isInFlight()
        if (belongsToCurrentConversation) responseStopped.value = true
    }

    fun isPendingSubmission(token: Long): Boolean = pendingSubmissionGate.isActive(token)

    suspend fun <T> withAssistantModelOperation(block: suspend () -> T): T =
        assistantModelOperationMutex.withLock(block)

    fun reconcilePendingSubmission(token: Long, result: ModelStatus): Long? {
        val requestId = pendingSubmissionGate.reconcile(
            token = token,
            requestId = result.assistantRequestId,
            previousRequestId = pendingSubmission.value?.takeIf { it.token == token }?.previousRequestId,
            state = result.state,
            operation = result.assistantOperation,
            cloudReady = result.cloudReady,
        )
        if (requestId == null && !pendingSubmissionGate.isActive(token)) {
            pendingSubmission.value = null
        } else if (requestId != null && !pendingSubmissionGate.isActive(token)) {
            pendingSubmission.value = null
        }
        return requestId
    }

    fun observePendingLifecycle(status: ModelStatus): Boolean {
        val submissionObserved = pendingSubmissionGate.observeLifecycle(status.assistantRequestId, status.state,
            status.assistantOperation, status.cloudReady)
        if (submissionObserved) pendingSubmission.value = null
        val continuationObserved = pendingContinuationGate.observeLifecycle(status.assistantRequestId, status.state,
            status.assistantOperation, status.cloudReady)
        if (continuationObserved) {
            pendingContinuation.value = false
            pendingContinuationToken = null
        }
        return submissionObserved || continuationObserved
    }

    fun failPendingSubmission(token: Long) {
        if (pendingSubmissionGate.cancel(token)) pendingSubmission.value = null
    }

    fun beginPendingContinuation(): Long? {
        val token = pendingContinuationGate.begin() ?: return null
        retainedFailureAnnouncementRequestId.value = null
        pendingContinuationToken = token
        pendingContinuation.value = true
        return token
    }

    fun isPendingContinuation(token: Long): Boolean = pendingContinuationGate.isActive(token)

    fun reconcilePendingContinuation(token: Long, previousRequestId: Long?, result: ModelStatus): Long? {
        val requestId = pendingContinuationGate.reconcile(
            token = token,
            requestId = result.assistantRequestId,
            previousRequestId = previousRequestId,
            state = result.state,
            operation = result.assistantOperation,
            cloudReady = result.cloudReady,
        )
        if (requestId == null && !pendingContinuationGate.isActive(token)) {
            pendingContinuation.value = false
            pendingContinuationToken = null
        } else if (requestId != null && !pendingContinuationGate.isActive(token)) {
            // A terminal lifecycle poll may have arrived before request-ID reconciliation.
            // The gate remembers that matching terminal state and retires the optimistic UI now.
            pendingContinuation.value = false
            pendingContinuationToken = null
        }
        return requestId
    }

    fun failPendingContinuation(token: Long): Boolean {
        if (pendingContinuationGate.cancel(token)) {
            pendingContinuation.value = false
            pendingContinuationToken = null
            return true
        }
        return false
    }

    fun cancelPendingContinuation() {
        pendingContinuationToken?.let(::failPendingContinuation)
    }

    fun cancelPendingSubmission() {
        val token = pendingSubmission.value?.token
        pendingSubmissionGate.cancel(token)
        pendingSubmission.value = null
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = null
    }

    fun setPendingSubmissionJob(job: Job) {
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = job
    }

    fun reset() {
        conversationGeneration.value += 1
        clearAutomaticVoiceWriteOrigin()
        writeReceipts.invalidate()
        acceptedPortionOverride.value = null
        acceptedFoodReplacementOverride.value = null
        pendingMealConfirmation.value = null
        persistence.invalidatePendingWrites()
        assistantCancellationGate.reset()
        assistantStopping.value = assistantCancellationGate.isInFlight()
        responseStopped.value = false
        input.value = ""
        submittedQuestion.value = ""
        submittedContext.value = ""
        activeRequestId.value = null
        handledToolRequestId.value = null
        handledReplyRequestId.value = null
        retainedFailureAnnouncementRequestId.value = null
        savedWeightId.value = null
        voiceMessage.value = null
        recoverableDiagnostic.value = null
        pendingSubmissionGate.cancel()
        pendingSubmission.value = null
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = null
        pendingContinuationGate.cancel(pendingContinuationToken)
        pendingContinuation.value = false
        pendingContinuationToken = null
        messages.value = initialPrivateChatMessages()
        hydrated = true
        loadedConversationRecord = null
    }

    fun clearForLock() {
        reset()
        hydrated = false
    }

    suspend fun synchronizeAfterWrites(record: PrivateConversationRecord?, expired: Boolean) {
        // A refresh may have captured the vault just before an in-flight save committed. Wait for
        // that serialized save to register its exact receipt. Keep the writer gate through restore
        // so its invalidated, older ticket cannot overwrite the newly hydrated transcript.
        persistence.withWriterIdle { synchronize(record, expired) }
    }

    fun synchronize(record: PrivateConversationRecord?, expired: Boolean) {
        val ownWriteAcknowledgement = writeReceipts.recognizes(record)
        when (AssistantConversationHydrationPolicy.decide(
            hydrated = hydrated,
            previouslyLoaded = loadedConversationRecord,
            localMessages = persistedMessageSnapshot(),
            persisted = record,
            acknowledgedOwnWrite = ownWriteAcknowledgement,
            expired = expired,
        )) {
        AssistantConversationHydrationDecision.EXPIRED -> {
            reset()
            messages.value = initialPrivateChatMessages() + PrivateChatMessage(
                "Your previous private conversation expired after 24 hours of inactivity. This is a new conversation.",
                false,
            )
            return
        }
        AssistantConversationHydrationDecision.KEEP_LOCAL -> {
            hydrated = true
            loadedConversationRecord = record
            return
        }
        AssistantConversationHydrationDecision.RESTORE_PERSISTED -> {
            persistence.invalidatePendingWrites()
            writeReceipts.invalidate()
            conversationGeneration.value += 1
            clearAutomaticVoiceWriteOrigin()
            acceptedPortionOverride.value = null
            acceptedFoodReplacementOverride.value = null
            pendingMealConfirmation.value = null
        }
        }
        val restored = record?.messages?.map { message ->
            PrivateChatMessage(message.text, message.fromMember, message.localEvidence, message.savedMealId)
        }
        retainedFailureAnnouncementRequestId.value = null
        input.value = ""
        submittedQuestion.value = ""
        submittedContext.value = ""
        activeRequestId.value = null
        handledToolRequestId.value = null
        handledReplyRequestId.value = null
        savedWeightId.value = null
        voiceMessage.value = null
        pendingSubmissionGate.cancel()
        pendingSubmission.value = null
        pendingSubmissionJob?.cancel()
        pendingSubmissionJob = null
        pendingContinuationGate.cancel(pendingContinuationToken)
        pendingContinuation.value = false
        pendingContinuationToken = null
        messages.value = restored?.ifEmpty(::initialPrivateChatMessages) ?: initialPrivateChatMessages()
        hydrated = true
        loadedConversationRecord = record
    }

    /** Comparison only for fixed debug provenance; no transcript content is logged. */
    fun matchesPersistedSnapshot(record: PrivateConversationRecord?): Boolean = record != null &&
        PrivateConversationPolicy.updated(persistedMessageSnapshot(), record.updatedAt)?.messages == record.messages

    private fun persistedMessageSnapshot(): List<PrivateConversationMessage> = messages.value.map {
            PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
        }
}

@Composable
private fun rememberPrivateAssistantConversationSession(): PrivateAssistantConversationSession {
    // Only a generic error is saved across configuration changes. Conversation content stays
    // in the encrypted vault, never in Android's saved-instance-state bundle.
    val recoverableDiagnostic = rememberSaveable { mutableStateOf<String?>(null) }
    return remember(recoverableDiagnostic) { PrivateAssistantConversationSession(recoverableDiagnostic) }
}

@Composable
private fun PrivateAssistantChat(
    photoAnalysisMode: String,
    bottomBar: @Composable () -> Unit,
    conversationSession: PrivateAssistantConversationSession,
    foodCatalog: OfflineFoodCatalog?,
    foodCorrections: List<FoodCorrection>,
    onPanelAction: (AssistantPanelAction) -> Unit,
    onConversationChanged: suspend (List<PrivateConversationMessage>) -> Boolean,
    onNewConversation: suspend () -> Boolean,
    modelStatus: ModelStatus,
    recordReplyDelivery: (Long, String) -> Unit,
    onReviewVoiceWeightDraft: (Double, Instant) -> Unit,
    entries: List<WeightEntry>,
    meals: List<MealEntry>,
    personalDishes: List<PersonalDish>,
    nutritionTargets: NutritionTargets?,
    cycleContext: CycleContext,
    onBack: () -> Unit,
    onVoiceListeningChanged: (Boolean) -> Unit,
    startVoiceImmediately: Boolean = false,
    openVoiceEntry: Boolean = false,
    voiceMealEntry: Boolean = false,
    onVoiceEntryHandled: () -> Unit = {},
    onVoiceStartHandled: () -> Unit = {},
    readCurrentModelStatus: () -> ModelStatus,
    transcribeContextualVoice: (java.io.File, Long) -> ModelStatus,
    cancelContextualVoice: (Long) -> ModelStatus,
    clearContextualVoice: (Long) -> ModelStatus,
    askMeal: (String) -> ModelStatus,
    askCoach: (String, PrivateCoachSnapshot, String) -> ModelStatus,
    startAgent: (String, List<PrivateConversationMessage>, NativeMealDraftPolicy.MealConfirmationContext?, ChatCurrentDraftPortionContext?, ChatCurrentDraftFoodContext?) -> ModelStatus,
    continueAgentTool: (String, Map<String, Any?>) -> ModelStatus,
    resumeSavedDishProbe: (Long, PrivateCoachToolCall, String) -> ModelStatus,
    understandCoach: (String, String) -> ModelStatus,
    understandCoachAudio: (java.io.File, String) -> ModelStatus,
    cancelAssistant: () -> Unit,
    saveWeightDirect: (Double, Instant) -> WeightEntry?,
    editSavedWeight: (WeightEntry) -> Unit,
    undoSavedWeight: (WeightEntry) -> Unit,
    reviewMeal: (MealProposal, ChatPortionOverride?, ChatFoodReplacementOverride?) -> Unit,
    openSavedMeal: (String) -> Unit,
    discardMealDraft: () -> Unit,
    selectedMealContextTitle: String?,
    clearSelectedMealContext: () -> Unit,
    readSelectedMeal: (String) -> Map<String, Any?>?,
    completeCompanionNavigation: (Long) -> Boolean,
    isVaultUnlocked: () -> Boolean,
    isSavedDishCurrent: (PersonalDish) -> Boolean,
    onReviewSavedDish: (PersonalDish) -> Boolean,
) {
    val context = LocalContext.current
    fun assistantReady(status: ModelStatus): Boolean = if (photoAnalysisMode == "cloud") status.cloudReady
        else status.state == "loaded"
    var input by conversationSession.input
    var submittedQuestion by conversationSession.submittedQuestion
    var submittedConversationContext by conversationSession.submittedContext
    var activeAssistantRequestId by conversationSession.activeRequestId
    var handledToolRequestId by conversationSession.handledToolRequestId
    var handledReplyRequestId by conversationSession.handledReplyRequestId
    var acceptedPortionOverride by conversationSession.acceptedPortionOverride
    var savedWeightId by conversationSession.savedWeightId
    var recordingVoice by remember { mutableStateOf(false) }
    var voiceEntryOpen by rememberSaveable { mutableStateOf(openVoiceEntry) }
    var voiceText by rememberSaveable { mutableStateOf("") }
    var voiceCaptureGeneration by remember { mutableStateOf(0L) }
    var voicePermissionCaptureToken by remember { mutableStateOf<Long?>(null) }
    var voiceContextHandoff by remember { mutableStateOf<OneUseChatVoiceDraftHandoff?>(null) }
    var voiceRequestProof by remember { mutableStateOf<OneUseChatVoiceRequestProof?>(null) }
    var voiceAutoSubmitGate by remember { mutableStateOf<OneUseChatVoiceAutoSubmit?>(null) }
    var voiceTypingExit by remember { mutableStateOf<OneUseChatVoiceTypingExit?>(null) }
    var composerFocusRequest by remember { mutableStateOf(0L) }
    var contextualVoiceMode by remember { mutableStateOf(false) }
    var suppressVoiceDraftContextOnce by remember { mutableStateOf(false) }
    var voiceMessage by conversationSession.voiceMessage
    var messages by conversationSession.messages
    var pendingSavedDishChoices by remember { mutableStateOf<PendingSavedDishChoices?>(null) }
    var savedDishChoiceSelectionInFlight by remember { mutableStateOf(false) }
    var portionClarification by remember { mutableStateOf<ChatPortionClarificationOffer?>(null) }
    val assistantScope = rememberCoroutineScope()
    var panelActionInFlight by remember { mutableStateOf(false) }
    DisposableEffect(conversationSession) {
        onDispose {
            conversationSession.retainedFailureAnnouncementRequestId.value = null
            pendingSavedDishChoices = null
            portionClarification = null
            conversationSession.pendingMealConfirmation.value = null
        }
    }
    LaunchedEffect(
        acceptedPortionOverride, activeAssistantRequestId, handledReplyRequestId,
        modelStatus.assistantRequestId, modelStatus.assistantProposal,
        conversationSession.conversationGeneration.value, conversationSession.assistantStopping.value,
    ) {
        val override = acceptedPortionOverride ?: return@LaunchedEffect
        val currentProposal = modelStatus.assistantProposal
        if (currentProposal == null || !ChatPortionClarificationPolicy.isCurrent(
                override, currentProposal, modelStatus.assistantRequestId,
                conversationSession.conversationGeneration.value, isVaultUnlocked(),
                conversationSession.assistantStopping.value,
            ) || activeAssistantRequestId != override.requestId || handledReplyRequestId != override.requestId
        ) acceptedPortionOverride = null
    }
    LaunchedEffect(
        modelStatus.assistantProposal, modelStatus.assistantRequestId, modelStatus.assistantOperation,
        activeAssistantRequestId, handledReplyRequestId, conversationSession.conversationGeneration.value,
        conversationSession.assistantStopping.value,
    ) {
        val proposal = modelStatus.assistantProposal ?: return@LaunchedEffect
        val request = proposal.memberDrinkQuantity ?: return@LaunchedEffect
        if (acceptedPortionOverride != null || modelStatus.assistantRequestId != activeAssistantRequestId ||
            modelStatus.assistantRequestId != handledReplyRequestId || modelStatus.assistantOperation !in
                setOf("meal", "agent_meal_draft") || !isVaultUnlocked() ||
            conversationSession.assistantStopping.value || proposal.items.size != 1 ||
            proposal.omittedItemCount != 0 || proposal.partialCategory != null
        ) return@LaunchedEffect
        val item = proposal.items.single().takeIf { it.evidence == "seen" && it.groupLabel.isNullOrBlank() }
            ?: return@LaunchedEffect
        val portion = ChatDrinkPortionPolicy.resolve(request, item.name, item.description, foodCatalog, foodCorrections)
            ?: return@LaunchedEffect
        val grams = DrinkPortionPolicy.canonicalGrams(portion) ?: return@LaunchedEffect
        acceptedPortionOverride = ChatPortionOverride(
            modelStatus.assistantRequestId!!, conversationSession.conversationGeneration.value,
            proposal, 0, item.name.trim(), grams, portion,
        )
        portionClarification = null
    }
    LaunchedEffect(messages) {
        val writeTicket = conversationSession.captureConversationWriteTicket()
        if (writeTicket != null && messages.any(PrivateChatMessage::fromMember)) {
            // Coalesce nearby member, evidence, and reply updates. The encrypted write stays
            // off-main and must not replace the whole unlocked shell state after each message.
            delay(300)
            conversationSession.persistConversation(writeTicket, messages.map { message ->
                PrivateConversationMessage(message.text, message.fromMember, message.localEvidence, message.savedMealId)
            }, onConversationChanged)
        }
    }
    val today = LocalDate.now()
    val snapshot = remember(entries, meals, nutritionTargets, today) { PrivateCoachSnapshot.from(entries, meals, nutritionTargets, today) }
    val savedWeight = savedWeightId?.let { id -> entries.firstOrNull { it.id == id } }
    fun trackRequest(result: ModelStatus, onReconciled: (Long?) -> Unit = {}) {
        activeAssistantRequestId = result.assistantRequestId
        onReconciled(activeAssistantRequestId)
    }
    fun bindSavedDishChoices(requestId: Long?, candidates: List<SavedDishChoiceCandidate>) {
        if (requestId == null || candidates.isEmpty()) {
            pendingSavedDishChoices = null
            return
        }
        pendingSavedDishChoices = PendingSavedDishChoices(
            requestId = requestId,
            conversationGeneration = conversationSession.conversationGeneration.value,
            candidates = candidates.map { candidate ->
                // Copy nested collections so the transient offer is an immutable comparison
                // snapshot even if a caller later reuses a mutable list instance.
                candidate.copy(dish = candidate.dish.copy(
                    aliases = candidate.dish.aliases.toList(),
                    items = candidate.dish.items.toList(),
                ))
            },
        )
    }
    fun trackPendingRequest(token: Long, result: ModelStatus) {
        activeAssistantRequestId = conversationSession.reconcilePendingSubmission(token, result)
        val voiceOrigin = conversationSession.pendingAutomaticVoiceSubmission.value
        if (voiceOrigin?.submissionToken == token) {
            conversationSession.pendingAutomaticVoiceSubmission.value = null
            conversationSession.automaticVoiceWeightReview.value = result.assistantRequestId?.let { requestId ->
                if (voiceOrigin.conversationGeneration == conversationSession.conversationGeneration.value) {
                    OneUseAutomaticVoiceWeightReview(requestId, voiceOrigin.conversationGeneration)
                } else null
            }
        }
        result.assistantRequestId?.let {
            recordReplyDelivery(it, if (activeAssistantRequestId == it) "submission_reconciled" else "submission_rejected")
        }
    }
    fun trackNativeRequest(onReconciled: (Long?) -> Unit = {}, block: () -> ModelStatus) {
        val previousRequestId = activeAssistantRequestId
        val token = conversationSession.beginPendingContinuation() ?: return
        // The tool-call effect is keyed to polled ModelStatus fields. Keep the handoff in the
        // composition-owned scope so a fast poll cannot cancel it before the new request ID is
        // reconciled. The pending token still lets explicit cancellation invalidate late results.
        assistantScope.launch {
            try {
                val result = conversationSession.withAssistantModelOperation {
                    if (!conversationSession.isPendingContinuation(token)) return@withAssistantModelOperation null
                    withContext(Dispatchers.Default) { block() }
                } ?: run { onReconciled(null); return@launch }
                val stillPending = conversationSession.isPendingContinuation(token)
                if (!stillPending) {
                    result.assistantRequestId?.let { recordReplyDelivery(it, "continuation_cancelled") }
                    onReconciled(null)
                    return@launch
                }
                val requestId = conversationSession.reconcilePendingContinuation(
                    token,
                    previousRequestId,
                    result,
                )
                activeAssistantRequestId = requestId
                onReconciled(requestId)
                result.assistantRequestId?.let {
                    recordReplyDelivery(it, if (requestId == it) "continuation_reconciled" else "continuation_rejected")
                }
                if (requestId == null && stillPending) {
                    conversationSession.recoverableDiagnostic.value = continuationFailureDiagnostic(result.coachDiagnostic)
                } else if (assistantReady(result) && result.coachDiagnostic != null) {
                    conversationSession.recoverableDiagnostic.value = result.coachDiagnostic
                }
            } catch (_: CancellationException) {
                conversationSession.failPendingContinuation(token)
            } catch (_: Exception) {
                if (conversationSession.failPendingContinuation(token)) {
                    activeAssistantRequestId = null
                    conversationSession.recoverableDiagnostic.value = continuationFailureDiagnostic(null)
                }
            }
        }
    }
    fun stopAssistant(showConfirmation: Boolean = true) {
        pendingSavedDishChoices = null
        savedDishChoiceSelectionInFlight = false
        val token = conversationSession.beginAssistantCancellation() ?: return
        assistantScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withContext(NonCancellable + Dispatchers.Default) {
                    conversationSession.withAssistantModelOperation { cancelAssistant() }
                }
            } catch (_: Exception) {
                // The request has already been invalidated for presentation; lifecycle polling
                // remains authoritative for whether the runtime is ready for another turn.
            } finally {
                // The native cancellation can outlive this screen. Complete the presentation
                // even if navigation disposes this coroutine, while the session gate rejects
                // stale confirmation after a reset or a newer cancellation.
                withContext(NonCancellable + Dispatchers.Main.immediate) {
                    conversationSession.finishAssistantCancellation(token)
                    if (!showConfirmation) conversationSession.responseStopped.value = false
                }
            }
        }
    }
    fun setVoiceRecording(value: Boolean) {
        recordingVoice = value
        onVoiceListeningChanged(value)
    }
    val audioRecorder = remember(context) { GemmaAudioRecorder(context) }
    fun livePortionEditContext(sourceStatus: ModelStatus = modelStatus): ChatCurrentDraftPortionContext? =
        ChatPortionClarificationPolicy.currentDraftContext(
            proposal = sourceStatus.assistantProposal,
            operation = sourceStatus.assistantOperation,
            requestId = sourceStatus.assistantRequestId,
            activeRequestId = activeAssistantRequestId,
            handledReplyRequestId = handledReplyRequestId,
            conversationGeneration = conversationSession.conversationGeneration.value,
            acceptedOverride = acceptedPortionOverride,
            unlocked = isVaultUnlocked(),
            stopping = conversationSession.assistantStopping.value,
            pending = conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
        )

    fun liveFoodEditContext(sourceStatus: ModelStatus = modelStatus): ChatCurrentDraftFoodContext? {
        val proposal = sourceStatus.assistantProposal ?: return null
        val portionGrams = acceptedPortionOverride?.takeIf {
            ChatPortionClarificationPolicy.isCurrent(
                it, proposal, sourceStatus.assistantRequestId, conversationSession.conversationGeneration.value,
                isVaultUnlocked(), conversationSession.assistantStopping.value,
            )
        }?.grams
        return ChatCurrentDraftFoodPolicy.context(
            proposal = proposal,
            operation = sourceStatus.assistantOperation,
            requestId = sourceStatus.assistantRequestId,
            activeRequestId = activeAssistantRequestId,
            handledReplyRequestId = handledReplyRequestId,
            conversationGeneration = conversationSession.conversationGeneration.value,
            acceptedOverride = conversationSession.acceptedFoodReplacementOverride.value,
            currentGrams = portionGrams,
            unlocked = isVaultUnlocked(),
            stopping = conversationSession.assistantStopping.value,
            pending = conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
        )
    }
    fun currentVoiceContexts(status: ModelStatus): Pair<ChatCurrentDraftFoodContext?, ChatCurrentDraftPortionContext?> =
        liveFoodEditContext(status) to livePortionEditContext(status)
    fun captureVoiceHandoff(token: Long, status: ModelStatus): OneUseChatVoiceDraftHandoff? {
        if (!assistantReady(status)) return null
        val (food, portion) = currentVoiceContexts(status)
        return OneUseChatVoiceDraftHandoff.capture(
            token, food, portion,
            conversationSession.acceptedFoodReplacementOverride.value,
            acceptedPortionOverride,
        )
    }
    fun captureVoiceRequestProof(token: Long, status: ModelStatus): OneUseChatVoiceRequestProof? {
        if (!assistantReady(status) || status.assistantRequestId != activeAssistantRequestId ||
            !isVaultUnlocked() || conversationSession.assistantStopping.value ||
            conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value
        ) return null
        return OneUseChatVoiceRequestProof.capture(
            token, status.assistantRequestId, activeAssistantRequestId, handledReplyRequestId,
            conversationSession.conversationGeneration.value,
        )
    }
    fun voiceRequestProofIsCurrent(
        proof: OneUseChatVoiceRequestProof?, token: Long, status: ModelStatus,
    ): Boolean = proof?.isCurrent(
        token, status.assistantRequestId, activeAssistantRequestId, handledReplyRequestId,
        conversationSession.conversationGeneration.value, isVaultUnlocked(),
        conversationSession.assistantStopping.value,
        conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
    ) == true
    fun voiceTypingExitIsCurrent(exit: OneUseChatVoiceTypingExit?, status: ModelStatus): Boolean = exit?.isCurrent(
        status.voiceTranscriptionCaptureToken, status.assistantRequestId, activeAssistantRequestId,
        handledReplyRequestId, conversationSession.conversationGeneration.value, isVaultUnlocked(),
        conversationSession.assistantStopping.value,
        conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
    ) == true
    fun handoffIsCurrent(
        handoff: OneUseChatVoiceDraftHandoff,
        token: Long,
        status: ModelStatus,
    ): Boolean {
        val busyForThisTranscript = status.state == "thinking" &&
            status.voiceTranscriptionState == "processing" &&
            status.voiceTranscriptionCaptureToken == handoff.captureToken()
        if (!assistantReady(status) && !busyForThisTranscript) return false
        val (food, portion) = currentVoiceContexts(status)
        return handoff.isCurrent(
            token, food, portion,
            conversationSession.acceptedFoodReplacementOverride.value,
            acceptedPortionOverride,
            activeAssistantRequestId, handledReplyRequestId,
            conversationSession.conversationGeneration.value,
            isVaultUnlocked(), conversationSession.assistantStopping.value,
            conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
        )
    }
    fun clearVoiceTranscriptFor(handoff: OneUseChatVoiceDraftHandoff?) {
        if (handoff == null) return
        val latest = readCurrentModelStatus()
        if (latest.voiceTranscriptionCaptureToken == handoff.captureToken()) {
            latest.voiceTranscriptionId?.let(clearContextualVoice)
        }
    }
    fun revokeVoiceHandoff(cancelRecognition: Boolean) {
        voiceCaptureGeneration += 1
        voicePermissionCaptureToken = null
        if (recordingVoice) {
            audioRecorder.cancel()
            setVoiceRecording(false)
        }
        val handoff = voiceContextHandoff
        val proof = voiceRequestProof
        voiceAutoSubmitGate?.revoke()
        voiceAutoSubmitGate = null
        voiceTypingExit?.revoke()
        voiceTypingExit = null
        handoff?.revoke()
        voiceContextHandoff = null
        proof?.revoke()
        voiceRequestProof = null
        val latest = readCurrentModelStatus()
        val captureToken = handoff?.captureToken() ?: proof?.captureToken()
        if (captureToken != null && latest.voiceTranscriptionCaptureToken == captureToken && latest.voiceTranscriptionId != null) {
            val id = latest.voiceTranscriptionId
            if (cancelRecognition && latest.voiceTranscriptionState == "processing") cancelContextualVoice(id)
            else clearContextualVoice(id)
        }
    }
    fun submitVoiceClip(clip: GemmaAudioRecorder.Clip, capture: Long) {
        setVoiceRecording(false)
        val handoff = voiceContextHandoff
        if (!voiceMealEntry) {
            val latest = readCurrentModelStatus()
            val proof = voiceRequestProof
            if (proof == null || !voiceRequestProofIsCurrent(proof, capture, latest) ||
                (handoff != null && !handoffIsCurrent(handoff, capture, latest))
            ) {
                clip.file.delete()
                revokeVoiceHandoff(cancelRecognition = false)
                voiceMessage = "This Chat changed while recording. Start a new voice request and review it before sending."
                return
            }
            if (!clip.detectedSpeech) {
                clip.file.delete()
                voiceMessage = "Metria could not hear speech. Try again or type your request."
                return
            }
            voiceMessage = null
            val status = transcribeContextualVoice(clip.file, capture)
            voiceAutoSubmitGate = if (status.voiceTranscriptionCaptureToken == capture &&
                status.voiceTranscriptionId != null && status.voiceTranscriptionState in setOf("processing", "ready")
            ) OneUseChatVoiceAutoSubmit(capture, status.voiceTranscriptionId) else null
            if (status.voiceTranscriptionCaptureToken != capture || status.voiceTranscriptionState == "failed") {
                voiceMessage = status.voiceTranscriptionDiagnostic
                    ?: "Voice recognition could not start. Try again or type your request."
            }
            return
        }
        if (handoff != null) {
            val latest = readCurrentModelStatus()
            if (!handoffIsCurrent(handoff, capture, latest)) {
                clip.file.delete()
                revokeVoiceHandoff(cancelRecognition = false)
                voiceMessage = "This meal draft changed. Try voice again from the current draft."
                return
            }
            if (!clip.detectedSpeech) {
                clip.file.delete()
                voiceMessage = "Metria could not hear speech. Try again or type your request."
                return
            }
            voiceMessage = null
            val status = transcribeContextualVoice(clip.file, capture)
            if (status.voiceTranscriptionCaptureToken != capture || status.voiceTranscriptionState == "failed") {
                voiceMessage = status.voiceTranscriptionDiagnostic
                    ?: "Voice recognition could not start. Try again or type your request."
            }
            return
        }
        if (!clip.detectedSpeech) {
            clip.file.delete()
            voiceMessage = "Metria could not hear speech. Try again or type your request."
            return
        }
        submittedConversationContext = privateConversationContext(messages)
        if (!voiceEntryOpen) messages = messages + PrivateChatMessage("Voice request received locally.", true)
        submittedQuestion = "Voice request"
        trackRequest(understandCoachAudio(clip.file, submittedConversationContext))
    }
    fun beginVoiceRecording(capture: Long) {
        audioRecorder.start { clip ->
            if (capture == voiceCaptureGeneration) submitVoiceClip(clip, capture) else clip.file.delete()
        }.onSuccess { setVoiceRecording(true) }.onFailure { voiceMessage = "Microphone recording could not start. Try again or type instead." }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val capture = voicePermissionCaptureToken
        voicePermissionCaptureToken = null
        if (capture == null || capture != voiceCaptureGeneration) return@rememberLauncherForActivityResult
        val handoff = voiceContextHandoff
        val latest = readCurrentModelStatus()
        if (contextualVoiceMode && !voiceMealEntry &&
            !voiceRequestProofIsCurrent(voiceRequestProof, capture, latest)
        ) {
            revokeVoiceHandoff(cancelRecognition = false)
            voiceMessage = "This Chat changed while recording. Start a new voice request and review it before sending."
            return@rememberLauncherForActivityResult
        }
        if (handoff != null && !handoffIsCurrent(handoff, capture, latest)) {
            revokeVoiceHandoff(cancelRecognition = false)
            voiceMessage = "This meal draft changed. Try voice again from the current draft."
            return@rememberLauncherForActivityResult
        }
        if (granted) beginVoiceRecording(capture)
        else {
            revokeVoiceHandoff(cancelRecognition = false)
            voiceMessage = "Microphone permission was not granted. You can type your request instead."
        }
    }
    fun startVoiceCapture() {
        voiceAutoSubmitGate?.revoke()
        voiceAutoSubmitGate = null
        conversationSession.automaticVoiceComposerText.value = null
        voiceText = ""
        voiceTypingExit?.revoke()
        voiceTypingExit = null
        voiceMessage = null
        if (voiceMealEntry) {
            if (voiceContextHandoff != null || voiceRequestProof != null) revokeVoiceHandoff(cancelRecognition = true)
            contextualVoiceMode = false
        } else contextualVoiceMode = true
        var latest = readCurrentModelStatus()
        if (latest.voiceTranscriptionState != "processing") {
            latest.voiceTranscriptionId?.let(clearContextualVoice)
            latest = readCurrentModelStatus()
        }
        val capture = ++voiceCaptureGeneration
        if (contextualVoiceMode && !voiceMealEntry) {
            voiceContextHandoff?.revoke()
            voiceContextHandoff = null
            voiceRequestProof?.revoke()
            voiceRequestProof = captureVoiceRequestProof(capture, latest)
            if (voiceRequestProof == null) {
                voiceMessage = "Chat is busy or changed. Wait for it to finish, then try voice again."
                return
            }
        }
        val shouldBindDraft = !voiceMealEntry && (contextualVoiceMode || voiceContextHandoff != null)
        if (shouldBindDraft) {
            val binding = captureVoiceHandoff(capture, latest)
            voiceContextHandoff = binding
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val handoff = voiceContextHandoff
            latest = readCurrentModelStatus()
            if ((contextualVoiceMode && !voiceMealEntry &&
                    !voiceRequestProofIsCurrent(voiceRequestProof, capture, latest)) ||
                (handoff != null && !handoffIsCurrent(handoff, capture, latest))
            ) {
                revokeVoiceHandoff(cancelRecognition = false)
                voiceMessage = "Chat changed while preparing the microphone. Try again and review the recognized words before sending."
                return
            }
            beginVoiceRecording(capture)
        } else {
            voicePermissionCaptureToken = capture
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(audioRecorder) {
        onDispose {
            voiceCaptureGeneration += 1
            audioRecorder.cancel()
            revokeVoiceHandoff(cancelRecognition = true)
            onVoiceListeningChanged(false)
        }
    }
    LaunchedEffect(
        modelStatus.assistantRequestId, modelStatus.assistantOperation,
        activeAssistantRequestId, handledReplyRequestId,
        conversationSession.conversationGeneration.value,
        isVaultUnlocked(), conversationSession.assistantStopping.value,
        conversationSession.pendingSubmission.value != null, conversationSession.pendingContinuation.value,
    ) {
        val typingExit = voiceTypingExit
        if (typingExit != null && !voiceTypingExitIsCurrent(typingExit, readCurrentModelStatus())) {
            typingExit.revoke()
            voiceTypingExit = null
        }
        val proof = voiceRequestProof ?: return@LaunchedEffect
        val handoff = voiceContextHandoff
        val latest = readCurrentModelStatus()
        if (!voiceRequestProofIsCurrent(proof, proof.captureToken(), latest) ||
            (handoff != null && !handoffIsCurrent(handoff, handoff.captureToken(), latest))
        ) revokeVoiceHandoff(cancelRecognition = true)
    }
    LaunchedEffect(startVoiceImmediately, modelStatus.state) {
        if (startVoiceImmediately) {
            when (modelStatus.state) {
                "loaded" -> {
                    onVoiceStartHandled()
                    if (!voiceMealEntry) contextualVoiceMode = true
                    startVoiceCapture()
                }
                "failed", "absent", "removed" -> {
                    onVoiceStartHandled()
                    voiceMessage = "Metria could not get ready for voice. Open Chat to check the local assistant."
                }
            }
        }
    }
    fun toggleVoice() {
        if (recordingVoice) {
            val capture = voiceCaptureGeneration
            setVoiceRecording(false)
            audioRecorder.stop()?.let { submitVoiceClip(it, capture) }
                ?: run {
                    voiceMessage = if (audioRecorder.awaitingRecognition()) "Recognizing speech on this phone…"
                    else "No voice clip was captured. Try again."
                }
        } else {
            if (!voiceEntryOpen && !voiceMealEntry) {
                voiceEntryOpen = true
                contextualVoiceMode = true
                voiceText = ""
                voiceContextHandoff = null
                voiceRequestProof = null
            }
            startVoiceCapture()
        }
    }
    val voiceResultEffectKey = ContextualVoiceResultPolicy.EffectKey(
        modelStatus.voiceTranscriptionState,
        voiceRequestProof?.captureToken() ?: modelStatus.voiceTranscriptionCaptureToken
            .takeIf { modelStatus.voiceTranscriptionState != "idle" },
        modelStatus.voiceTranscriptionCaptureToken,
    )
    LaunchedEffect(modelStatus.assistantRequestId, modelStatus.coachToolCall, submittedQuestion,
        activeAssistantRequestId, voiceResultEffectKey) {
        if (voiceResultEffectKey.isolatesAssistantResults) {
            if (modelStatus.coachToolCall != null) modelStatus.assistantRequestId?.let {
                recordReplyDelivery(it, "voice_result_isolated")
            }
            return@LaunchedEffect
        }
        val requestId = modelStatus.assistantRequestId ?: return@LaunchedEffect
        val call = modelStatus.coachToolCall ?: return@LaunchedEffect
        if (!shouldConsumeAssistantResult(activeAssistantRequestId, handledToolRequestId, requestId)) return@LaunchedEffect
        handledToolRequestId = requestId
        if (voiceEntryOpen) {
            val recognized = VoiceEntryText.validated(call.spokenText)
            if (recognized == null) voiceMessage = "Metria could not produce readable speech. Try again or type instead."
            else { voiceText = recognized; voiceMessage = null }
            // Recognition is a preview: none of the proposed audio actions is dispatched.
            return@LaunchedEffect
        }
        call.spokenText?.let { spoken ->
            submittedQuestion = spoken
            messages = messages + PrivateChatMessage("Voice: $spoken", true)
        }
        val validatedCall = AssistantActionGuard.validate(call, submittedQuestion, submittedConversationContext)
        val action = AssistantActionDispatcher.resolve(validatedCall)
        val nativeAgentTool = modelStatus.assistantOperation == "agent_waiting_tool"
        recordReplyDelivery(requestId, "host_tool_handler_entered")
        if (action is AssistantAction.QueryWeightHistory && call.spokenText?.let(::soundsLikeMealHistoryQuestion) == true) {
            messages = messages + PrivateChatMessage("I heard a meal-history question, but could not safely match the action. Please try again or type it.", false)
            return@LaunchedEffect
        }
        when (action) {
            AssistantAction.QueryLatestMeal -> {
                if (nativeAgentTool) {
                    val frozenMiniLmDate = call.hostReadDate.takeIf {
                        call.intent == "read_latest_meal"
                    }
                    val result = LatestMealNutritionResult.from(meals, frozenMiniLmDate ?: today,
                        coachReplyLanguage(submittedQuestion, submittedConversationContext))
                    trackNativeRequest { continueAgentTool(call.intent, result) }
                } else {
                    messages = messages + PrivateChatMessage("Ask about your latest meal in Chat.", false)
                }
            }
            AssistantAction.QuerySelectedMeal -> {
                if (nativeAgentTool) {
                    val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                    val result = readSelectedMeal(replyLanguage)
                    if (result != null) trackNativeRequest { continueAgentTool(call.intent, result) }
                    else {
                        cancelAssistant()
                        messages = messages + PrivateChatMessage("That selected meal context expired. Open the meal again and try once more.", false)
                    }
                } else {
                    messages = messages + PrivateChatMessage("Open a saved meal and choose Ask Metria to use its current record.", false)
                }
            }
            is AssistantAction.QueryProgressOverview -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val frozenTodayOverviewDate = call.hostReadDate.takeIf {
                    nativeAgentTool && call.intent == "read_progress_overview" &&
                        call.hostCompanionKind == CompanionAssistantPolicy.IntentKind.TODAY_OVERVIEW &&
                        action.period == "today"
                }
                val overview = MemberProgressOverview.from(
                    entries = entries,
                    meals = meals,
                    targets = nutritionTargets,
                    today = frozenTodayOverviewDate ?: today,
                    period = action.period,
                )
                if (nativeAgentTool) {
                    val overviewResponse = overview.toolResponse()
                    val response = if (frozenTodayOverviewDate != null) {
                        overviewResponse + mapOf(
                            "as_of" to frozenTodayOverviewDate.toString(), "reply_language" to replyLanguage,
                        )
                    } else {
                        overviewResponse + ("reply_language" to replyLanguage)
                    }
                    if (frozenTodayOverviewDate == null) {
                        messages = messages + PrivateChatMessage(overview.localEvidence(), false, localEvidence = true)
                    }
                    trackNativeRequest { continueAgentTool(call.intent, response) }
                } else {
                    messages = messages + PrivateChatMessage(overview.localEvidence(), false, localEvidence = true)
                    trackRequest(askCoach(
                        """MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion, submittedConversationContext)}
                            MEMBER_QUESTION: $submittedQuestion
                            [LOCAL_FACTS]
                            ${overview.localEvidence()}
                            [/LOCAL_FACTS]
                            Give one concise grounded progress insight and one gentle optional focus.
                        """.trimIndent(),
                        snapshot,
                        submittedConversationContext,
                    ))
                }
            }
            is AssistantAction.QueryNutrition -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val goalIntent = (call.hostCompanionKind == CompanionAssistantPolicy.IntentKind.GOAL_REMAINING)
                    .let { trustedFastIntent ->
                        if (trustedFastIntent) null else CompanionAssistantPolicy.resolve(
                            MetriaAgentGroundingPolicy.requiredTool(submittedQuestion, emptyList(), today),
                            submittedQuestion, null, today,
                        )?.takeIf { it.kind == CompanionAssistantPolicy.IntentKind.GOAL_REMAINING }
                    }
                val goalMetric = call.hostMetric ?: goalIntent?.metric
                val scopedSnapshot = snapshot
                val evidence = scopedSnapshot.goalProgressFacts(action.period)
                val facts = scopedSnapshot.selectedNutritionFacts(action.period)
                messages = messages + PrivateChatMessage(evidence, false, localEvidence = true)
                if (nativeAgentTool) {
                    if (goalMetric != null && call.intent == "read_nutrition_summary") {
                        val goalResponse = GoalRemainingNutritionResult.from(
                            meals = meals,
                            targets = nutritionTargets,
                            today = call.hostReadDate ?: today,
                            metric = goalMetric,
                        )
                        trackNativeRequest { continueAgentTool(call.intent, goalResponse) }
                        return@LaunchedEffect
                    }
                    val weekly = action.period == "week"
                    val goalMultiplier = if (weekly) 7 else 1
                    trackNativeRequest { continueAgentTool(call.intent, mapOf(
                        "source" to "metria_local_vault",
                        "as_of" to today.toString(),
                        "reply_language" to replyLanguage,
                        "period" to if (weekly) "last_7_days_including_today" else "today",
                        "meals" to mapOf(
                            "recorded_count" to if (weekly) scopedSnapshot.weekMeals else scopedSnapshot.todayMeals,
                        ),
                        "nutrition" to mapOf(
                            "calories_kcal" to if (weekly) scopedSnapshot.weekCalories else scopedSnapshot.todayCalories,
                            "protein_g" to if (weekly) scopedSnapshot.weekProtein else scopedSnapshot.todayProtein,
                            "calorie_goal_kcal" to scopedSnapshot.calorieGoal?.times(goalMultiplier),
                            "protein_goal_g" to scopedSnapshot.proteinGoal?.times(goalMultiplier),
                        ),
                        "progress" to mapOf(
                            "calorie_goal_percent" to goalProgressPercent(
                                if (weekly) scopedSnapshot.weekCalories else scopedSnapshot.todayCalories,
                                scopedSnapshot.calorieGoal?.times(goalMultiplier),
                            ),
                            "protein_goal_percent" to goalProgressPercent(
                                if (weekly) scopedSnapshot.weekProtein else scopedSnapshot.todayProtein,
                                scopedSnapshot.proteinGoal?.times(goalMultiplier),
                            ),
                        ),
                        "summary" to facts,
                    )) }
                } else {
                    val language = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                    trackRequest(askCoach("""MEMBER_LANGUAGE: $language
                        MEMBER_QUESTION: $submittedQuestion
                        [LOCAL_FACTS]
                        $facts
                        [/LOCAL_FACTS]
                        Respond in MEMBER_LANGUAGE about only the selected period. A nonzero logged_meals value means meals were recorded.
                        Write recognition, factual comparison, and one gentle optional next focus.""".trimIndent(), scopedSnapshot, submittedConversationContext))
                }
            }
            is AssistantAction.QueryDayNutrition -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val requestedMetric = MiniLmMealReadPolicy.nativeNutritionMetric(call, submittedQuestion)
                val frozenMiniLmDate = call.hostReadDate.takeIf {
                    nativeAgentTool && call.intent == "read_day_nutrition"
                }
                val queryToday = frozenMiniLmDate ?: today
                val result = HistoricalDayNutritionResult.from(meals, queryToday, action.daysAgo, requestedMetric)
                val facts = HistoricalDayNutritionEvidenceText.format(result, replyLanguage)
                if (nativeAgentTool) {
                    val response = result + mapOf("reply_language" to replyLanguage, "summary" to facts)
                    if (frozenMiniLmDate != null) {
                        val required = requestedMetric?.let { metric ->
                            RequiredAgentTool(
                                "read_day_nutrition", period = "historical_day", daysAgo = action.daysAgo,
                                metric = metric, targetDate = frozenMiniLmDate.minusDays(action.daysAgo.toLong()),
                            )
                        }
                        val fastReply = required?.let {
                            MiniLmMealReadPolicy.historicalNutritionReplyForFrozenRead(it, response)
                        }
                        if (fastReply != null) {
                            messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                        }
                    } else {
                        messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                    }
                    trackNativeRequest {
                        continueAgentTool(call.intent, response)
                    }
                } else {
                    messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                    trackRequest(askCoach("""MEMBER_LANGUAGE: $replyLanguage
                        MEMBER_QUESTION: $submittedQuestion
                        [LOCAL_FACTS]
                        $facts
                        [/LOCAL_FACTS]
                        Answer the exact historical day question using only these recorded estimates. State the recorded estimate total and disclose complete, partial, and missing coverage as relevant. Mention that unrecorded meals are unknown; do not infer a goal or complete intake.""".trimIndent(), snapshot, submittedConversationContext))
                }
            }
            is AssistantAction.QueryWeightHistory -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val frozenMiniLmDate = call.hostReadDate.takeIf {
                    nativeAgentTool && call.intent == "read_weight"
                }
                val queryToday = frozenMiniLmDate ?: today
                val oldest = queryToday.minusDays(action.fromDaysAgo.toLong())
                val newest = queryToday.minusDays(action.toDaysAgo.toLong())
                val ordered = entries.filter { date -> date.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in oldest..newest }.sortedBy(WeightEntry::recordedAt)
                val boundedWeights = MetriaAgentResultBounds.latest(ordered)
                val first = ordered.firstOrNull()
                val latest = ordered.lastOrNull()
                val trend = if (first == null || latest == null) "No weight entries were saved from $oldest to $newest." else if (first.id == latest.id) "One weight entry from $oldest to $newest: ${formatRecordedKilograms(first.kilograms)} kg." else {
                    val days = java.time.Duration.between(first.recordedAt, latest.recordedAt).toDays().coerceAtLeast(1)
                    val change = latest.kilograms - first.kilograms
                    "${ordered.size} weight entries from ${first.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()} to ${latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}: ${formatRecordedKilograms(first.kilograms)} kg to ${formatRecordedKilograms(latest.kilograms)} kg (${formatSignedRecordedKilograms(change)} kg over $days days)."
                }
                messages = messages + PrivateChatMessage(trend, false, localEvidence = true)
                if (nativeAgentTool) {
                    trackNativeRequest { continueAgentTool(call.intent, mapOf(
                        "source" to "metria_local_vault",
                        "reply_language" to replyLanguage,
                        "as_of" to queryToday.toString(),
                        "from" to oldest.toString(),
                        "to" to newest.toString(),
                        "total_count" to boundedWeights.totalCount,
                        "returned_count" to boundedWeights.records.size,
                        "omitted_count" to boundedWeights.omittedCount,
                        "coverage" to boundedWeights.coverage,
                        "entries" to boundedWeights.records.map { entry -> mapOf(
                            "date" to entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                            "kilograms" to entry.kilograms,
                        ) },
                        "trend" to if (first != null && latest != null) mapOf(
                            "first_date" to first.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                            "first_kg" to first.kilograms,
                            "latest_date" to latest.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                            "latest_kg" to latest.kilograms,
                            "change_kg" to latest.kilograms - first.kilograms,
                        ) else null,
                        "summary" to trend,
                    )) }
                } else {
                    trackRequest(askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion, submittedConversationContext)}
                        MEMBER_QUESTION: $submittedQuestion
                        [LOCAL_FACTS]
                        $trend
                        [/LOCAL_FACTS]
                        Explain only these facts in three short supportive sentences. Do not mention nutrition goals or give medical advice.""".trimIndent(), snapshot, submittedConversationContext))
                }
            }
            is AssistantAction.QueryMealHistory -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val frozenMiniLmDate = call.hostReadDate.takeIf {
                    nativeAgentTool && call.intent == "read_meals"
                }
                val queryToday = frozenMiniLmDate ?: today
                val oldest = queryToday.minusDays(action.fromDaysAgo.toLong())
                val newest = queryToday.minusDays(action.toDaysAgo.toLong())
                val saved = meals.filter {
                    it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() in oldest..newest &&
                        (action.mealSlot == null || MealSlots.matches(it, action.mealSlot))
                }.sortedBy(MealEntry::recordedAt)
                val boundedMeals = MetriaAgentResultBounds.latest(saved)
                val typeLabel = action.mealSlot?.let(MealSlots::label)?.lowercase()
                val facts = if (saved.isEmpty()) {
                    "No ${typeLabel ?: ""} meals were saved from $oldest to $newest.".replace("No  meals", "No meals")
                } else {
                    val visibleMeals = boundedMeals.records.joinToString { meal ->
                        val local = meal.recordedAt.atZone(ZoneId.systemDefault())
                        val timing = when {
                            meal.mealSlot != null -> MealSlots.label(meal.mealSlot).lowercase()
                            meal.recordedTimeKnown -> "around ${MealSlots.label(MealSlots.suggestedFor(local.toLocalTime())).lowercase()} (${local.toLocalTime().withSecond(0).withNano(0)})"
                            else -> "time not recorded"
                        }
                        "${local.toLocalDate()}: ${MealIdentity.displayTitle(meal)} ($timing)"
                    }
                    val omitted = boundedMeals.omittedCount.takeIf { it > 0 }
                        ?.let { " $it earlier matching ${if (it == 1) "meal was" else "meals were"} omitted from the model context." }
                        .orEmpty()
                    "${typeLabel?.replaceFirstChar(Char::uppercase) ?: "Meals"} from $oldest to $newest: $visibleMeals.$omitted"
                }
                val toolResponse = mapOf(
                    "source" to "metria_local_vault",
                    "reply_language" to replyLanguage,
                    "as_of" to queryToday.toString(),
                    "from" to oldest.toString(),
                    "to" to newest.toString(),
                    "meal_slot" to action.mealSlot,
                    "total_count" to boundedMeals.totalCount,
                    "returned_count" to boundedMeals.records.size,
                    "omitted_count" to boundedMeals.omittedCount,
                    "coverage" to boundedMeals.coverage,
                    "meals" to boundedMeals.records.map { meal -> mapOf(
                        "date" to meal.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                        "title" to MealIdentity.displayTitle(meal),
                        "slot" to meal.mealSlot,
                    ) },
                    "summary" to facts,
                )
                val validFrozenRead = frozenMiniLmDate?.let { frozenDate ->
                    call.hostReadDate == frozenDate && MiniLmMealReadPolicy.hasValidFrozenMealRead(call, toolResponse)
                } ?: true
                if (!validFrozenRead) {
                    // Do not add even a local-evidence chat row until the exact frozen-day
                    // response passes the same host contract used by the lifecycle continuation.
                    trackNativeRequest { continueAgentTool(call.intent, toolResponse) }
                } else {
                    messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                    if (nativeAgentTool) {
                        trackNativeRequest { continueAgentTool(call.intent, toolResponse) }
                    } else {
                        trackRequest(askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion, submittedConversationContext)}
                            MEMBER_QUESTION: $submittedQuestion
                            [LOCAL_FACTS]
                            $facts
                            [/LOCAL_FACTS]
                            Answer only from these local facts in no more than three short sentences.""".trimIndent(), snapshot, submittedConversationContext))
                    }
                }
            }
            is AssistantAction.SearchSavedDishes -> {
                val replyLanguage = coachReplyLanguage(submittedQuestion, submittedConversationContext)
                val matches = PersonalDishMatcher.search(PersonalDishQuery(name = action.query), personalDishes)
                val boundedMatches = MetriaAgentResultBounds.latest(matches)
                val consumptionProbe = call.hostCompanionKind ==
                    CompanionAssistantPolicy.IntentKind.SAVED_CONSUMPTION_PROBE
                if (consumptionProbe && CompanionAssistantPolicy.savedDishProbeDisposition(action.query, matches) ==
                    CompanionAssistantPolicy.SavedDishProbeDisposition.NORMAL_INTENT
                ) {
                    pendingSavedDishChoices = null
                    val requestId = modelStatus.assistantRequestId ?: return@LaunchedEffect
                    val originalQuestion = submittedQuestion
                    trackNativeRequest {
                        resumeSavedDishProbe(requestId, call, originalQuestion)
                    }
                    return@LaunchedEffect
                }
                val draftIntent = call.hostCompanionKind == CompanionAssistantPolicy.IntentKind.SAVED_DRAFT ||
                    consumptionProbe ||
                    CompanionAssistantPolicy.resolve(
                        MetriaAgentGroundingPolicy.requiredTool(submittedQuestion, emptyList(), today),
                        submittedQuestion, null, today,
                    )?.kind == CompanionAssistantPolicy.IntentKind.SAVED_DRAFT
                val choiceCandidates = if (draftIntent) {
                    val draftChoice = CompanionAssistantPolicy.savedDishDraftChoice(action.query, matches)
                    val candidates = draftChoice.exactCandidateId?.let { id -> matches.filter { it.dish.id == id } }
                        ?: draftChoice.candidates
                    candidates.map { match ->
                        SavedDishChoiceCandidate(
                            dish = match.dish,
                            exactNameMatch = draftChoice.exactCandidateId == match.dish.id,
                        )
                    }
                } else emptyList()
                val onSavedChoicesReconciled: (Long?) -> Unit = { finalRequestId ->
                    bindSavedDishChoices(finalRequestId, choiceCandidates)
                }
                if (draftIntent) {
                    pendingSavedDishChoices = null
                }
                val facts = if (matches.isEmpty()) {
                    "No saved private dish matched '${action.query}'."
                } else {
                    "Saved private dish matches for '${action.query}': ${boundedMatches.records.joinToString { match ->
                        "${match.dish.name} (${PersonalDishStructure.componentItems(match.dish).joinToString { it.name }}; confirmed ${match.dish.confirmationCount} ${if (match.dish.confirmationCount == 1) "time" else "times"})"
                    }}.${boundedMatches.omittedCount.takeIf { it > 0 }?.let { " $it additional matches were omitted." }.orEmpty()}"
                }
                messages = messages + PrivateChatMessage(facts, false, localEvidence = true)
                if (nativeAgentTool) {
                    trackNativeRequest(onReconciled = onSavedChoicesReconciled) { continueAgentTool(call.intent, mapOf(
                        "source" to "metria_local_vault",
                        "reply_language" to replyLanguage,
                        "query" to action.query,
                        "total_count" to boundedMatches.totalCount,
                        "returned_count" to boundedMatches.records.size,
                        "omitted_count" to boundedMatches.omittedCount,
                        "coverage" to boundedMatches.coverage,
                        "matches" to boundedMatches.records.map { match -> mapOf(
                            "name" to match.dish.name,
                            "ingredients" to PersonalDishStructure.componentItems(match.dish).map { it.name },
                            "confirmation_count" to match.dish.confirmationCount,
                        ) },
                        "summary" to facts,
                    )) }
                } else {
                    trackRequest(askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion, submittedConversationContext)}
                        MEMBER_QUESTION: $submittedQuestion
                        [LOCAL_FACTS]
                        $facts
                        [/LOCAL_FACTS]
                        Answer only from these bounded saved-dish results. Say that the member can review or remove saved dishes in Privacy when useful.""".trimIndent(), snapshot, submittedConversationContext), onSavedChoicesReconciled)
                }
            }
            is AssistantAction.DraftWeight -> {
                when (conversationSession.guardAutomaticVoiceWeightAction(
                    requestId, conversationSession.conversationGeneration.value,
                )) {
                    AutomaticVoiceWeightActionDisposition.REVIEW -> {
                        onReviewVoiceWeightDraft(action.kilograms, Instant.now())
                        messages = messages + PrivateChatMessage(
                            "I prepared a weight entry for review. Nothing has been saved yet.", false,
                        )
                    }
                    AutomaticVoiceWeightActionDisposition.REJECT -> {
                        messages = messages + PrivateChatMessage(
                            "I could not verify that spoken weight request, so I did not save it. Please send it again.", false,
                        )
                    }
                    AutomaticVoiceWeightActionDisposition.ORDINARY -> {
                        val entry = saveWeightDirect(action.kilograms, Instant.now())
                        savedWeightId = entry?.id
                        messages = messages + PrivateChatMessage(
                            if (entry != null) "${"%.1f".format(action.kilograms)} kg saved locally."
                            else "Weight could not be saved. Try again.", false,
                        )
                    }
                }
            }
            AssistantAction.DraftMeal -> trackRequest(askMeal(submittedQuestion))
            is AssistantAction.Respond -> trackRequest(askCoach("""MEMBER_LANGUAGE: ${coachReplyLanguage(submittedQuestion, submittedConversationContext)}
                MEMBER_QUESTION: $submittedQuestion
                [LOCAL_FACTS]
                No personal record facts were requested or provided.
                [/LOCAL_FACTS]
                Continue the conversation naturally in MEMBER_LANGUAGE. Do not claim to have read meals, weights, goals, or progress.""".trimIndent(), snapshot, submittedConversationContext))
            AssistantAction.Clarify -> {
                if (nativeAgentTool) {
                    trackNativeRequest { continueAgentTool(call.intent, mapOf(
                        "error" to "invalid_arguments",
                        "message" to "The requested local read was outside Metria's allowed bounds.",
                    )) }
                } else {
                    messages = messages + PrivateChatMessage("I can help with meals, weights, and your progress. Try asking in another way.", false)
                }
            }
        }
    }
    LaunchedEffect(
        modelStatus.assistantRequestId, modelStatus.coachReply, activeAssistantRequestId,
        modelStatus.assistantProposal, modelStatus.assistantPortionOverride,
        modelStatus.assistantFoodReplacementOverride, modelStatus.state,
        modelStatus.companionNavigation?.requestId,
        voiceResultEffectKey,
    ) {
        if (voiceResultEffectKey.isolatesAssistantResults) return@LaunchedEffect
        val requestId = modelStatus.assistantRequestId ?: return@LaunchedEffect
        val reply = modelStatus.coachReply ?: mealDraftTranscriptAcknowledgement(
            activeAssistantRequestId, handledReplyRequestId, requestId, modelStatus.state,
            modelStatus.assistantProposal != null, modelStatus.assistantOperation,
            isVaultUnlocked() && !conversationSession.assistantStopping.value,
        )
        var transcript = messages
        if (reply != null) {
            recordReplyDelivery(requestId, "lifecycle_ready")
            when (assistantReplyDeliveryGate(activeAssistantRequestId, handledReplyRequestId, requestId)) {
                AssistantReplyDeliveryGate.CONSUME -> {
                    handledReplyRequestId = requestId
                    transcript = transcript + PrivateChatMessage(reply, false)
                    messages = transcript
                    recordReplyDelivery(requestId, "chat_consumed")
                    modelStatus.assistantPortionOverride?.takeIf { override ->
                        override.requestId == requestId && override.originalProposal == modelStatus.assistantProposal &&
                            override.conversationGeneration == conversationSession.conversationGeneration.value &&
                            isVaultUnlocked() && !conversationSession.assistantStopping.value
                    }?.let { acceptedPortionOverride = it }
                    modelStatus.assistantFoodReplacementOverride?.takeIf { override ->
                        override.requestId == requestId && override.originalProposal == modelStatus.assistantProposal &&
                            override.conversationGeneration == conversationSession.conversationGeneration.value &&
                            isVaultUnlocked() && !conversationSession.assistantStopping.value &&
                            ChatCurrentDraftFoodPolicy.isCurrent(
                                override, modelStatus.assistantProposal ?: return@takeIf false,
                                requestId, conversationSession.conversationGeneration.value,
                                isVaultUnlocked(), conversationSession.assistantStopping.value,
                            )
                    }?.let { conversationSession.acceptedFoodReplacementOverride.value = it }
                    conversationSession.pendingMealConfirmation.value =
                        if (modelStatus.assistantOperation == "agent_reply" && modelStatus.assistantProposal == null &&
                            modelStatus.companionNavigation == null && isVaultUnlocked() &&
                            !conversationSession.assistantStopping.value
                        ) NativeMealDraftPolicy.candidate(
                            sourceRequestId = requestId,
                            conversationGeneration = conversationSession.conversationGeneration.value,
                            originalMemberText = submittedQuestion,
                        ) else null
                    val proposal = modelStatus.assistantProposal
                    val target = proposal?.let {
                        ChatPortionClarificationPolicy.target(
                            proposal = it,
                            operation = modelStatus.assistantOperation,
                            ordinaryChatDraft = isVaultUnlocked() &&
                                !conversationSession.assistantStopping.value &&
                                modelStatus.companionNavigation == null,
                        )
                    }
                    portionClarification = if (proposal != null && target != null && proposal.memberDrinkQuantity == null &&
                        modelStatus.assistantPortionOverride?.drinkPortion == null &&
                        modelStatus.assistantPortionOverride?.grams == null &&
                        acceptedPortionOverride?.drinkPortion == null &&
                        modelStatus.assistantOperation != "agent_meal_portion_edit" &&
                        !(modelStatus.assistantOperation == "agent_meal_food_edit" &&
                            modelStatus.assistantPortionOverride?.grams != null)
                    ) {
                        ChatPortionClarificationOffer(
                            requestId = requestId,
                            conversationGeneration = conversationSession.conversationGeneration.value,
                            proposal = proposal,
                            target = target,
                        )
                    } else null
                }
                AssistantReplyDeliveryGate.ACTIVE_REQUEST_MISMATCH -> {
                    recordReplyDelivery(requestId, "active_request_mismatch")
                    val activeId = activeAssistantRequestId
                    if (activeId == null) recordReplyDelivery(requestId, "chat_active_missing")
                    else recordReplyDelivery(activeId, "chat_active_request")
                }
                AssistantReplyDeliveryGate.ALREADY_HANDLED ->
                    recordReplyDelivery(requestId, "already_handled")
                null -> Unit
            }
        }
        val navigation = modelStatus.companionNavigation?.takeIf { it.requestId == requestId }
            ?: return@LaunchedEffect
        if (!conversationSession.isCurrentNavigationRequest(requestId) || activeAssistantRequestId != requestId) {
            return@LaunchedEffect
        }
        val persistedTranscript = transcript.map { message ->
            PrivateConversationMessage(message.text, message.fromMember, message.localEvidence, message.savedMealId)
        }
        val handoff = conversationSession.persistBeforeNavigation(
            requestId = requestId,
            transcript = persistedTranscript,
            isCurrent = {
                isVaultUnlocked() && assistantReady(modelStatus) &&
                    modelStatus.assistantRequestId == requestId &&
                    modelStatus.companionNavigation?.requestId == navigation.requestId &&
                    activeAssistantRequestId == requestId &&
                    conversationSession.isCurrentNavigationRequest(requestId)
            },
            save = onConversationChanged,
            navigate = completeCompanionNavigation,
        )
        if (handoff == ConversationNavigationResult.SAVE_FAILED) {
            conversationSession.recoverableDiagnostic.value =
                "Chat could not be saved, so Metria stayed here. Try again."
        } else if (handoff == ConversationNavigationResult.COMPLETED) {
            conversationSession.recoverableDiagnostic.value = null
        }
    }
    LaunchedEffect(
        modelStatus.assistantRequestId,
        modelStatus.state,
        modelStatus.assistantOperation,
        modelStatus.coachDiagnostic,
        modelStatus.coachReply,
        modelStatus.coachToolCall,
        modelStatus.assistantProposal,
        modelStatus.companionNavigation?.requestId,
        activeAssistantRequestId,
        handledReplyRequestId,
        conversationSession.pendingSubmission.value,
        conversationSession.pendingContinuation.value,
        conversationSession.assistantStopping.value,
        voiceResultEffectKey,
    ) {
        if (voiceResultEffectKey.isolatesAssistantResults) return@LaunchedEffect
        val requestId = modelStatus.assistantRequestId ?: return@LaunchedEffect
        val failure = retainableAssistantFailure(
            activeRequestId = activeAssistantRequestId,
            handledRequestId = handledReplyRequestId,
            resultRequestId = requestId,
            modelState = modelStatus.state,
            operation = modelStatus.assistantOperation,
            diagnostic = modelStatus.coachDiagnostic,
            hasReply = modelStatus.coachReply != null,
            hasToolCall = modelStatus.coachToolCall != null,
            hasProposal = modelStatus.assistantProposal != null,
            hasNavigation = modelStatus.companionNavigation != null,
            localPending = conversationSession.pendingSubmission.value != null ||
                conversationSession.pendingContinuation.value,
            stopping = conversationSession.assistantStopping.value,
            unlocked = isVaultUnlocked(),
        ) ?: return@LaunchedEffect

        // Consume before mutating the transcript so status polling or recomposition cannot
        // append the same terminal explanation twice. The ordinary message is persisted by
        // the existing encrypted conversation writer.
        handledReplyRequestId = requestId
        messages = messages + PrivateChatMessage(failure, false)
        conversationSession.retainedFailureAnnouncementRequestId.value = requestId
        conversationSession.recoverableDiagnostic.value = null
    }
    LaunchedEffect(modelStatus.assistantRequestId, modelStatus.coachDiagnostic, activeAssistantRequestId,
        handledReplyRequestId, conversationSession.pendingSubmission.value,
        conversationSession.pendingContinuation.value, voiceResultEffectKey) {
        if (voiceResultEffectKey.isolatesAssistantResults) return@LaunchedEffect
        if (shouldLatchAssistantDiagnostic(
                activeRequestId = activeAssistantRequestId,
                resultRequestId = modelStatus.assistantRequestId,
                handledRequestId = handledReplyRequestId,
                modelState = modelStatus.state,
                diagnostic = modelStatus.coachDiagnostic,
                localPending = conversationSession.pendingSubmission.value != null ||
                    conversationSession.pendingContinuation.value,
            )) {
            conversationSession.recoverableDiagnostic.value = modelStatus.coachDiagnostic
        }
    }
    LaunchedEffect(modelStatus.assistantRequestId, modelStatus.state) {
        conversationSession.observePendingLifecycle(modelStatus)
    }
    fun isSavedDishChoiceBound(state: PendingSavedDishChoices): Boolean =
        SavedDishChoicePolicy.isCurrent(
            pending = state,
            activeRequestId = conversationSession.activeRequestId.value,
            handledReplyRequestId = conversationSession.handledReplyRequestId.value,
            conversationGeneration = conversationSession.conversationGeneration.value,
            unlocked = isVaultUnlocked(),
            stopping = conversationSession.assistantStopping.value,
            currentDishes = personalDishes,
        ) && state.candidates.all { isSavedDishCurrent(it.dish) }

    fun canSelectSavedDishChoice(state: PendingSavedDishChoices): Boolean =
        isSavedDishChoiceBound(state) && modelStatus.assistantRequestId == state.requestId &&
            assistantReady(modelStatus) && conversationSession.pendingSubmission.value == null &&
            !conversationSession.pendingContinuation.value &&
            !savedDishChoiceSelectionInFlight

    fun selectSavedDishChoice(candidateId: String, memberReply: String? = null) {
        val original = pendingSavedDishChoices ?: return
        if (!canSelectSavedDishChoice(original) || !SavedDishChoicePolicy.canRetrySelection(original, candidateId)) {
            if (!isSavedDishChoiceBound(original)) pendingSavedDishChoices = null
            return
        }
        val candidate = original.candidates.singleOrNull { it.dish.id == candidateId } ?: return
        if (!isSavedDishCurrent(candidate.dish)) {
            pendingSavedDishChoices = null
            return
        }
        val firstSelection = original.selectedCandidateId == null
        val selected = original.copy(selectedCandidateId = candidateId)
        pendingSavedDishChoices = selected
        savedDishChoiceSelectionInFlight = true
        if (memberReply != null) {
            val additions = buildList {
                add(PrivateChatMessage(memberReply, true))
                if (firstSelection) add(PrivateChatMessage(SavedDishChoicePolicy.acknowledgement(candidate.dish.name), false))
            }
            messages = messages + additions
            input = ""
        }
        val transcript = messages.map { message ->
            PrivateConversationMessage(message.text, message.fromMember, message.localEvidence, message.savedMealId)
        }
        assistantScope.launch {
            try {
                val result = conversationSession.persistBeforeNavigation(
                    requestId = selected.requestId,
                    transcript = transcript,
                    isCurrent = {
                        pendingSavedDishChoices == selected && isSavedDishChoiceBound(selected) &&
                            conversationSession.pendingSubmission.value == null &&
                            !conversationSession.pendingContinuation.value &&
                            conversationSession.isCurrentNavigationRequest(selected.requestId) &&
                            isSavedDishCurrent(candidate.dish)
                    },
                    save = onConversationChanged,
                    navigate = {
                        if (pendingSavedDishChoices != selected || !isSavedDishCurrent(candidate.dish)) false
                        else if (onReviewSavedDish(candidate.dish)) {
                            pendingSavedDishChoices = null
                            true
                        } else false
                    },
                )
                when (result) {
                    ConversationNavigationResult.COMPLETED -> Unit
                    ConversationNavigationResult.SAVE_FAILED -> {
                        conversationSession.recoverableDiagnostic.value =
                            "Chat could not be saved, so Metria stayed here. Tap Review meal to try again."
                    }
                    ConversationNavigationResult.STALE -> {
                        if (pendingSavedDishChoices == selected && !isSavedDishChoiceBound(selected)) {
                            pendingSavedDishChoices = null
                        }
                    }
                    ConversationNavigationResult.DUPLICATE -> Unit
                }
            } catch (_: CancellationException) {
                // Lock, disposal or cancellation invalidates this transient choice with the session.
            } catch (_: Exception) {
                conversationSession.recoverableDiagnostic.value =
                    "Chat could not be saved, so Metria stayed here. Tap Review meal to try again."
            } finally {
                savedDishChoiceSelectionInFlight = false
            }
        }
    }
    fun consumeLocalPortionReply(context: ChatCurrentDraftPortionContext, text: String): Boolean {
        val foodReplacement = conversationSession.acceptedFoodReplacementOverride.value?.takeIf {
            ChatCurrentDraftFoodPolicy.isCurrent(
                it, context.originalProposal, context.sourceRequestId, context.conversationGeneration,
                isVaultUnlocked(), conversationSession.assistantStopping.value,
            )
        }
        val correctedFoodName = foodReplacement?.replacementName ?: context.target.itemName
        when (val drinkReply = ChatDrinkPortionPolicy.parseReply(text)) {
            is ChatDrinkPortionReply.Quantity -> {
                val sourceItem = context.originalProposal.items.getOrNull(context.target.itemIndex)
                val portion = if (foodReplacement == null && sourceItem?.evidence == "seen" && sourceItem.groupLabel.isNullOrBlank())
                    ChatDrinkPortionPolicy.resolve(
                        drinkReply.value, correctedFoodName, sourceItem.description, foodCatalog, foodCorrections,
                    ) else null
                val grams = portion?.let(DrinkPortionPolicy::canonicalGrams)
                val updated = if (portion != null && grams != null) {
                    ChatPortionClarificationPolicy.overrideForCurrentDraft(context, context.sourceRequestId, grams, portion)
                } else null
                conversationSession.automaticVoiceComposerText.value = null
                input = ""
                messages = messages + listOf(
                    PrivateChatMessage(text, true),
                    PrivateChatMessage(
                        if (updated != null) "Thanks. I set $correctedFoodName to ${DrinkPortionPolicy.format(requireNotNull(portion))} for review."
                        else "I couldn't verify that drink amount from its saved food, so the current draft estimate is unchanged and ready to review.",
                        false,
                    ),
                )
                if (updated != null) {
                    acceptedPortionOverride = updated
                    portionClarification = portionClarification?.copy(
                        answered = true, grams = grams, drinkPortion = portion, feedback = null,
                    )
                }
                recordReplyDelivery(context.sourceRequestId, if (updated != null) "portion_applied" else "portion_rejected")
                return true
            }
            ChatDrinkPortionReply.Invalid -> {
                conversationSession.automaticVoiceComposerText.value = null
                input = ""
                messages = messages + listOf(
                    PrivateChatMessage(text, true),
                    PrivateChatMessage("I couldn't apply that drink amount, so the current draft is unchanged and ready to review.", false),
                )
                recordReplyDelivery(context.sourceRequestId, "portion_rejected")
                return true
            }
            ChatDrinkPortionReply.Unrelated -> Unit
        }
        val reply = ChatPortionClarificationPolicy.parseReply(text)
        if (reply == ChatPortionReply.Unrelated) return false
        if (reply == ChatPortionReply.InvalidQuantity) {
            conversationSession.automaticVoiceComposerText.value = null
            input = ""
            messages = messages + listOf(
                PrivateChatMessage(text, true),
                PrivateChatMessage(
                    context.currentGrams?.let { "I did not change the portion. The previous $it g portion is unchanged." }
                        ?: "I did not apply that portion. The draft estimate is unchanged.",
                    false,
                ),
            )
            portionClarification = portionClarification?.copy(
                feedback = "Please use a whole gram amount from 1 to 2,000, such as 120 g, or say “skip”.",
            )
            recordReplyDelivery(context.sourceRequestId, "portion_rejected")
            return true
        }
        val grams = when (reply) {
            is ChatPortionReply.Grams -> reply.value
            ChatPortionReply.Skip -> context.currentGrams
            else -> return false
        }
        val retainedDrink = if (reply == ChatPortionReply.Skip) context.drinkPortion else null
        val updated = ChatPortionClarificationPolicy.overrideForCurrentDraft(
            context, context.sourceRequestId, grams, retainedDrink,
        ) ?: return false
        conversationSession.automaticVoiceComposerText.value = null
        input = ""
        messages = messages + listOf(
            PrivateChatMessage(text, true),
            PrivateChatMessage(
                if (reply is ChatPortionReply.Grams) "Thanks. I set $correctedFoodName to ${reply.value} g for review."
                else if (grams == null) "Okay. I kept the estimate for review."
                else "Okay. I kept $correctedFoodName at ${retainedDrink?.let(DrinkPortionPolicy::format) ?: "$grams g"} for review.",
                false,
            ),
        )
        acceptedPortionOverride = updated.takeIf { it.grams != null }
        portionClarification = portionClarification?.copy(
            answered = true, grams = updated.grams, drinkPortion = updated.drinkPortion, feedback = null,
        )
        recordReplyDelivery(
            context.sourceRequestId,
            if (reply is ChatPortionReply.Grams) "portion_applied" else "portion_skipped",
        )
        return true
    }

    fun submitText() {
        val text = input.trim()
        if (text.isBlank()) return
        val fromAutomaticVoice = conversationSession.automaticVoiceComposerText.value == text
        if (!fromAutomaticVoice) conversationSession.automaticVoiceComposerText.value = null
        val requestProof = voiceRequestProof
        if (requestProof != null && modelStatus.state == "thinking" &&
            modelStatus.voiceTranscriptionCaptureToken == requestProof.captureToken()
        ) {
            conversationSession.recoverableDiagnostic.value = "Voice recognition is stopping. Try sending your words again in a moment."
            return
        }
        val voiceRuntimeBusy = modelStatus.state == "thinking" && modelStatus.voiceTranscriptionState in setOf("processing", "cancelled")
        val modelBusy = shouldPresentAssistantThinking(
            localPending = conversationSession.pendingSubmission.value != null ||
                conversationSession.pendingContinuation.value,
            modelResultIsCurrent = modelStatus.assistantRequestId != null &&
                modelStatus.assistantRequestId == activeAssistantRequestId,
            modelState = modelStatus.state,
            operation = modelStatus.assistantOperation,
        ) || voiceRuntimeBusy
        if (requestProof != null && modelBusy) {
            conversationSession.recoverableDiagnostic.value = "Chat is still working. Your words are ready; try sending them again when it finishes."
            return
        }
        if (requestProof != null) {
            val proofValid = requestProof.consume(
                requestProof.captureToken(), text,
                modelStatus.assistantRequestId, activeAssistantRequestId, handledReplyRequestId,
                conversationSession.conversationGeneration.value, isVaultUnlocked(),
                conversationSession.assistantStopping.value,
                conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
            )
            if (!proofValid) {
                revokeVoiceHandoff(cancelRecognition = true)
                suppressVoiceDraftContextOnce = true
                portionClarification = null
                pendingSavedDishChoices = null
                conversationSession.pendingMealConfirmation.value = null
                input = text
                conversationSession.recoverableDiagnostic.value =
                    "Chat changed after voice recognition. Your words are still here; review them as a new message before sending."
                return
            }
            voiceRequestProof = null
        }
        if (voiceContextHandoff == null && modelStatus.voiceTranscriptionState != "processing") {
            modelStatus.voiceTranscriptionId?.let(clearContextualVoice)
        }
        val bypassCurrentDraftContext = suppressVoiceDraftContextOnce
        if (bypassCurrentDraftContext && modelBusy) return
        val portionOffer = portionClarification.takeUnless { bypassCurrentDraftContext }
        var frozenPortionEditContext = livePortionEditContext()
        var frozenFoodEditContext = liveFoodEditContext()
        val handoff = voiceContextHandoff
        if (handoff != null) {
            if (modelBusy) return
            val validHandoff = handoff.consume(
                handoff.captureToken(), text, frozenFoodEditContext, frozenPortionEditContext,
                conversationSession.acceptedFoodReplacementOverride.value, acceptedPortionOverride,
                activeAssistantRequestId, handledReplyRequestId,
                conversationSession.conversationGeneration.value, isVaultUnlocked(),
                conversationSession.assistantStopping.value,
                conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
            )
            voiceContextHandoff = null
            if (!validHandoff) {
                frozenPortionEditContext = null
                frozenFoodEditContext = null
                suppressVoiceDraftContextOnce = true
                portionClarification = null
                pendingSavedDishChoices = null
                conversationSession.pendingMealConfirmation.value = null
                input = text
                conversationSession.recoverableDiagnostic.value =
                    "This meal draft changed, so I did not apply your voice edit. Review your words before sending them as a new Chat message."
                clearVoiceTranscriptFor(handoff)
                return
            }
            clearVoiceTranscriptFor(handoff)
        }
        if (bypassCurrentDraftContext) {
            frozenPortionEditContext = null
            frozenFoodEditContext = null
            suppressVoiceDraftContextOnce = false
            portionClarification = null
            pendingSavedDishChoices = null
            conversationSession.pendingMealConfirmation.value = null
        }
        if (portionOffer != null && !modelBusy && ChatPortionClarificationPolicy.isOfferCurrent(
                offer = portionOffer,
                proposal = modelStatus.assistantProposal,
                requestId = modelStatus.assistantRequestId,
                conversationGeneration = conversationSession.conversationGeneration.value,
                unlocked = isVaultUnlocked(), stopping = conversationSession.assistantStopping.value,
            ) && assistantReady(modelStatus) && handledReplyRequestId == portionOffer.requestId &&
                activeAssistantRequestId == portionOffer.requestId &&
                conversationSession.pendingSubmission.value == null && !conversationSession.pendingContinuation.value
        ) {
            if (!portionOffer.answered) {
                when (val drinkReply = ChatDrinkPortionPolicy.parseReply(text)) {
                    is ChatDrinkPortionReply.Quantity -> {
                        val replacement = conversationSession.acceptedFoodReplacementOverride.value?.takeIf {
                            ChatCurrentDraftFoodPolicy.isCurrent(
                                it, portionOffer.proposal, portionOffer.requestId,
                                portionOffer.conversationGeneration, isVaultUnlocked(),
                                conversationSession.assistantStopping.value,
                            )
                        }
                        val item = portionOffer.proposal.items.getOrNull(portionOffer.target.itemIndex)
                        val portion = if (replacement == null && item != null) {
                            ChatDrinkPortionPolicy.resolve(
                                drinkReply.value, item.name, item.description, foodCatalog, foodCorrections,
                            )
                        } else null
                        val accepted = portion?.let {
                            ChatPortionClarificationPolicy.overrideForDrinkReview(
                                portionOffer.proposal, portionOffer.requestId, portionOffer.conversationGeneration,
                                portionOffer.target, it,
                            )
                        }
                        conversationSession.automaticVoiceComposerText.value = null
                        input = ""
                        val message = if (accepted != null && portion != null) {
                            "Thanks. I set ${item?.name} to ${DrinkPortionPolicy.format(portion)} for review."
                        } else {
                            "I couldn't verify that drink amount from its saved food, so the estimate is unchanged and ready to review."
                        }
                        messages = messages + listOf(PrivateChatMessage(text, true), PrivateChatMessage(message, false))
                        if (accepted != null) {
                            portionClarification = portionOffer.copy(
                                answered = true, grams = accepted.grams, drinkPortion = accepted.drinkPortion, feedback = null,
                            )
                            acceptedPortionOverride = accepted
                        } else {
                            portionClarification = portionOffer.copy(
                                feedback = "I couldn't match that drink amount to a saved conversion. You can keep the estimate and review the portion.",
                            )
                        }
                        recordReplyDelivery(portionOffer.requestId, if (accepted != null) "portion_applied" else "portion_rejected")
                        return
                    }
                    ChatDrinkPortionReply.Invalid -> {
                        conversationSession.automaticVoiceComposerText.value = null
                        input = ""
                        messages = messages + listOf(
                            PrivateChatMessage(text, true),
                            PrivateChatMessage("I couldn't apply that drink amount, so the estimate is unchanged and ready to review.", false),
                        )
                        portionClarification = portionOffer.copy(
                            feedback = "Please use one mL amount or a measured serving, such as 200 mL or half a cup.",
                        )
                        recordReplyDelivery(portionOffer.requestId, "portion_rejected")
                        return
                    }
                    ChatDrinkPortionReply.Unrelated -> Unit
                }
                when (val reply = ChatPortionClarificationPolicy.parseReply(text)) {
                    is ChatPortionReply.Grams, ChatPortionReply.Skip -> {
                        val correctedFoodName = conversationSession.acceptedFoodReplacementOverride.value?.takeIf {
                            ChatCurrentDraftFoodPolicy.isCurrent(
                                it, portionOffer.proposal, portionOffer.requestId,
                                portionOffer.conversationGeneration, isVaultUnlocked(),
                                conversationSession.assistantStopping.value,
                            )
                        }?.replacementName ?: portionOffer.target.itemName
                        val accepted = ChatPortionClarificationPolicy.overrideForReview(
                            portionOffer.proposal, portionOffer.requestId, portionOffer.conversationGeneration,
                            portionOffer.target, reply,
                        ) ?: return
                        conversationSession.automaticVoiceComposerText.value = null
                        input = ""
                        messages = messages + listOf(
                            PrivateChatMessage(text, true),
                            PrivateChatMessage(if (reply is ChatPortionReply.Grams) {
                                "Thanks. I set $correctedFoodName to ${reply.value} g for review."
                            } else {
                                "Okay. I kept the estimate for review."
                            }, false),
                        )
                        portionClarification = portionOffer.copy(answered = true, grams = accepted.grams, feedback = null)
                        acceptedPortionOverride = accepted.takeIf { it.grams != null }
                        recordReplyDelivery(portionOffer.requestId,
                            if (reply is ChatPortionReply.Grams) "portion_applied" else "portion_skipped")
                        return
                    }
                    ChatPortionReply.InvalidQuantity -> {
                        conversationSession.automaticVoiceComposerText.value = null
                        input = ""
                        messages = messages + listOf(
                            PrivateChatMessage(text, true),
                            PrivateChatMessage("I did not apply that portion. The draft estimate is unchanged.", false),
                        )
                        portionClarification = portionOffer.copy(
                            feedback = "Please use a whole gram amount from 1 to 2,000, such as 120 g, or say “skip”.",
                        )
                        recordReplyDelivery(portionOffer.requestId, "portion_rejected")
                        return
                    }
                    ChatPortionReply.Unrelated -> Unit
                }
            } else if (frozenPortionEditContext != null && consumeLocalPortionReply(frozenPortionEditContext, text)) {
                return
            }
            // Any unrelated message, or a second answer after the one-shot reply, starts
            // ordinary Chat handling and clears the transient offer first.
            portionClarification = null
        } else if (portionOffer != null && !modelBusy) {
            portionClarification = null
        }
        if (portionOffer == null && frozenPortionEditContext != null &&
            consumeLocalPortionReply(frozenPortionEditContext, text)
        ) return
        if (!modelBusy && frozenFoodEditContext != null) {
            val localReplacement = ChatCurrentDraftFoodPolicy.localCatalogReplacement(
                context = frozenFoodEditContext,
                text = text,
                catalog = foodCatalog,
                proposal = modelStatus.assistantProposal,
                requestId = modelStatus.assistantRequestId,
                activeRequestId = activeAssistantRequestId,
                handledReplyRequestId = handledReplyRequestId,
                conversationGeneration = conversationSession.conversationGeneration.value,
                operation = modelStatus.assistantOperation,
                unlocked = isVaultUnlocked(),
                stopping = conversationSession.assistantStopping.value,
                pending = conversationSession.pendingSubmission.value != null ||
                    conversationSession.pendingContinuation.value,
            )
            if (localReplacement != null) {
                val previousName = frozenFoodEditContext.currentReplacementName
                    ?: frozenFoodEditContext.originalItemName
                conversationSession.automaticVoiceComposerText.value = null
                input = ""
                messages = messages + listOf(
                    PrivateChatMessage(text, true),
                    PrivateChatMessage(
                        "Okay, I changed $previousName to ${localReplacement.replacementName}. Please review the food and its match before saving.",
                        false,
                    ),
                )
                conversationSession.acceptedFoodReplacementOverride.value = localReplacement
                recordReplyDelivery(frozenFoodEditContext.sourceRequestId, "food_replacement_local_applied")
                return
            }
        }
        // Any newly submitted request retires a prior accepted amount before other
        // local reply handlers or model work can interpret the same text.
        if (frozenPortionEditContext == null && acceptedPortionOverride != null) acceptedPortionOverride = null
        val choices = pendingSavedDishChoices.takeUnless { bypassCurrentDraftContext }
        if (choices != null && !modelBusy && canSelectSavedDishChoice(choices)) {
            val selectedId = SavedDishChoicePolicy.resolveReply(text, choices.candidates)
            if (selectedId != null && SavedDishChoicePolicy.canRetrySelection(choices, selectedId)) {
                selectSavedDishChoice(selectedId, text)
                return
            }
            if (SavedDishChoicePolicy.isDeclineReply(text)) {
                pendingSavedDishChoices = null
                savedDishChoiceSelectionInFlight = false
                input = ""
                messages = messages + listOf(
                    PrivateChatMessage(text, true),
                    PrivateChatMessage(SavedDishChoicePolicy.declineAcknowledgement(), false),
                )
                return
            }
            if (selectedId != null || SavedDishChoicePolicy.isChoiceAttempt(text, choices.candidates)) {
                input = ""
                messages = messages + listOf(
                    PrivateChatMessage(text, true),
                    PrivateChatMessage(
                        "That reply did not identify one saved dish, so I did not select or save anything.",
                        false,
                    ),
                )
                return
            }
        }
        // A different question retires the transient choice before starting model work.
        pendingSavedDishChoices = null
        savedDishChoiceSelectionInFlight = false
        val pending = if (text.isBlank() || modelBusy) null else {
            conversationSession.beginPendingSubmission(text, activeAssistantRequestId)
        }
        if (pending == null) return
        if (fromAutomaticVoice) {
            conversationSession.pendingAutomaticVoiceSubmission.value = PendingAutomaticVoiceSubmission(
                submissionToken = pending.token,
                conversationGeneration = conversationSession.conversationGeneration.value,
            )
        }
        val mealConfirmation = NativeMealDraftPolicy.consumeCandidate(
            candidate = conversationSession.pendingMealConfirmation.value,
            previousRequestId = pending.previousRequestId,
            conversationGeneration = conversationSession.conversationGeneration.value,
            previousMemberText = submittedQuestion,
            unlocked = isVaultUnlocked(),
            stopping = conversationSession.assistantStopping.value,
        )
        // The candidate is one-shot; Gemma interprets this reply against the full conversation.
        conversationSession.pendingMealConfirmation.value = null
        conversationSession.recoverableDiagnostic.value = null
        val messageSnapshot = messages
        messages = messages + PrivateChatMessage(text, true)
        input = ""
        submittedQuestion = text
        conversationSession.setPendingSubmissionJob(assistantScope.launch {
            try {
                // Let Compose commit the optimistic member bubble and thinking state before
                // building context or entering LiteRT's native startup path.
                withFrameNanos { }
                if (!conversationSession.isPendingSubmission(pending.token)) return@launch
                val (context, history, useLegacyRoute) = withContext(Dispatchers.Default) {
                    Triple(
                        privateConversationContext(messageSnapshot),
                        messageSnapshot.map { message ->
                            PrivateConversationMessage(message.text, message.fromMember, message.localEvidence, message.savedMealId)
                        },
                        mealConfirmation == null && !NativeMealDraftPolicy.isBareAcknowledgement(text) &&
                            MetriaAgentMigrationPolicy.useLegacyWriteRoute(text),
                    )
                }
                if (!conversationSession.isPendingSubmission(pending.token)) return@launch
                submittedConversationContext = context
                if (useLegacyRoute) clearSelectedMealContext()
                val result = conversationSession.withAssistantModelOperation {
                    if (!conversationSession.isPendingSubmission(pending.token)) return@withAssistantModelOperation null
                    withContext(Dispatchers.Default) {
                        if (useLegacyRoute) understandCoach(text, context) else startAgent(
                            text, history, mealConfirmation, frozenPortionEditContext, frozenFoodEditContext,
                        )
                    }
                } ?: return@launch
                trackPendingRequest(pending.token, result)
            } catch (_: CancellationException) {
                conversationSession.failPendingSubmission(pending.token)
            } catch (_: Exception) {
                conversationSession.failPendingSubmission(pending.token)
                voiceMessage = "Metria could not start that local request. Try again."
            }
        })
    }
    LaunchedEffect(
        modelStatus.voiceTranscriptionId, modelStatus.voiceTranscriptionCaptureToken,
        modelStatus.voiceTranscriptionState, modelStatus.voiceTranscriptionText,
        modelStatus.voiceTranscriptionDiagnostic, voiceCaptureGeneration,
        modelStatus.assistantRequestId, modelStatus.assistantOperation,
        activeAssistantRequestId, handledReplyRequestId,
        modelStatus.state, voiceAutoSubmitGate, voiceEntryOpen,
        conversationSession.conversationGeneration.value,
        isVaultUnlocked(), conversationSession.assistantStopping.value,
        conversationSession.pendingSubmission.value != null, conversationSession.pendingContinuation.value,
    ) {
        if (!voiceEntryOpen) return@LaunchedEffect
        val latest = readCurrentModelStatus()
        val handoff = voiceContextHandoff
        val proof = voiceRequestProof
        if (proof != null && !voiceRequestProofIsCurrent(proof, proof.captureToken(), latest)) {
            revokeVoiceHandoff(cancelRecognition = true)
            return@LaunchedEffect
        }
        if (proof == null || latest.voiceTranscriptionCaptureToken != proof.captureToken()) return@LaunchedEffect
        when (latest.voiceTranscriptionState) {
            "ready" -> {
                val transcriptionId = latest.voiceTranscriptionId
                val recognized = VoiceEntryText.validated(latest.voiceTranscriptionText)
                val gate = voiceAutoSubmitGate
                if (transcriptionId == null || recognized == null || gate == null) {
                    voiceMessage = "Metria could not produce readable speech. Try again or type instead."
                    return@LaunchedEffect
                }
                if (latest.state == "thinking" || !assistantReady(modelStatus)) {
                    // Keep the one-shot claim available while this same capture still owns native inference.
                    gate.claim(proof.captureToken(), transcriptionId, recognized, runtimeReleased = false)
                    return@LaunchedEffect
                }
                if (!assistantReady(latest) || !voiceRequestProofIsCurrent(proof, proof.captureToken(), latest) ||
                    modelStatus.state != latest.state ||
                    modelStatus.voiceTranscriptionState != latest.voiceTranscriptionState ||
                    modelStatus.voiceTranscriptionId != transcriptionId ||
                    modelStatus.voiceTranscriptionCaptureToken != proof.captureToken() ||
                    VoiceEntryText.validated(modelStatus.voiceTranscriptionText) != recognized ||
                    (handoff != null && !handoffIsCurrent(handoff, handoff.captureToken(), latest))
                ) {
                    revokeVoiceHandoff(cancelRecognition = false)
                    voiceMessage = "Chat changed during voice recognition. Start a new voice request or type your message."
                    return@LaunchedEffect
                }
                if (!gate.claim(proof.captureToken(), transcriptionId, recognized, runtimeReleased = true)) {
                    revokeVoiceHandoff(cancelRecognition = false)
                    voiceMessage = "This voice request has already been handled. Start a new recording if you still need help."
                    return@LaunchedEffect
                }
                input = recognized
                voiceText = recognized
                conversationSession.automaticVoiceComposerText.value = recognized
                voiceMessage = null
                voiceEntryOpen = false
                contextualVoiceMode = false
                onVoiceEntryHandled()
                submitText()
                if (input.isNotBlank()) {
                    voiceMessage = "Your words are in the message box. Review them and tap Send when Chat is ready."
                }
            }
            "failed", "cancelled" -> voiceMessage = latest.voiceTranscriptionDiagnostic
        }
    }
    val voiceTranscriptionOwnsRuntime = voiceRequestProof?.let { proof ->
        modelStatus.voiceTranscriptionCaptureToken == proof.captureToken() &&
            modelStatus.voiceTranscriptionState in setOf("processing", "cancelled") &&
            modelStatus.state == "thinking"
    } == true
    val modelResultIsCurrent = modelStatus.assistantRequestId != null &&
        modelStatus.assistantRequestId == activeAssistantRequestId && !voiceTranscriptionOwnsRuntime
    val visibleSavedDishChoices = pendingSavedDishChoices
        ?.takeIf(::canSelectSavedDishChoice)
        ?.let { pending ->
            pending.candidates.mapIndexedNotNull { index, candidate ->
                if (pending.selectedCandidateId != null && candidate.dish.id != pending.selectedCandidateId) null
                else MetriaSavedDishChoice(
                    id = candidate.dish.id,
                    name = candidate.dish.name,
                    foods = PersonalDishStructure.componentItems(candidate.dish).map { it.name },
                    exactNameMatch = candidate.exactNameMatch,
                    number = index + 1,
                    enabled = !savedDishChoiceSelectionInFlight,
                    retryOnly = pending.selectedCandidateId != null,
                )
            }
        }.orEmpty()

    val voiceEntryContent: (@Composable () -> Unit)? = if (voiceEntryOpen) { {
        val chatVoiceCapture = contextualVoiceMode && !voiceMealEntry
        val matchingTranscript = chatVoiceCapture && voiceRequestProof?.captureToken() != null &&
            modelStatus.voiceTranscriptionCaptureToken == voiceRequestProof?.captureToken()
        app.metria.ui.screens.VoiceEntrySheet(
            mealEntry = voiceMealEntry,
            recording = recordingVoice,
            processing = if (chatVoiceCapture) matchingTranscript &&
                (modelStatus.voiceTranscriptionState == "processing" ||
                    (modelStatus.voiceTranscriptionState == "ready" && modelStatus.state == "thinking"))
                else modelResultIsCurrent && modelStatus.state == "thinking",
            ready = assistantReady(modelStatus),
            unavailable = !assistantReady(modelStatus) && modelStatus.state in setOf("failed", "absent", "removed"),
            automaticChatSubmission = chatVoiceCapture,
            inlineChat = !voiceMealEntry,
            stopping = chatVoiceCapture && modelStatus.state == "thinking" &&
                modelStatus.voiceTranscriptionState == "cancelled",
            text = voiceText,
            error = voiceMessage ?: if (chatVoiceCapture && matchingTranscript) modelStatus.voiceTranscriptionDiagnostic
                else if (contextualVoiceMode) modelStatus.voiceTranscriptionDiagnostic
                else modelStatus.coachDiagnostic.takeIf { modelResultIsCurrent },
            onTextChange = { voiceText = it },
            onToggleRecording = { voiceText = ""; toggleVoice() },
            onCancelProcessing = {
                if (contextualVoiceMode) {
                    val proof = voiceRequestProof
                    val latest = readCurrentModelStatus()
                    val handoff = voiceContextHandoff
                    val exit = if (proof != null && voiceRequestProofIsCurrent(proof, proof.captureToken(), latest) &&
                        (handoff == null || handoffIsCurrent(handoff, handoff.captureToken(), latest)) &&
                        latest.voiceTranscriptionCaptureToken == proof.captureToken() &&
                        latest.voiceTranscriptionState == "processing"
                    ) proof.typingExitReceipt() else null
                    revokeVoiceHandoff(cancelRecognition = true)
                    voiceTypingExit = exit
                }
                else stopAssistant(showConfirmation = false)
            },
            onDismiss = {
                voiceCaptureGeneration += 1
                audioRecorder.cancel()
                setVoiceRecording(false)
                voiceTypingExit?.revoke()
                voiceTypingExit = null
                if (contextualVoiceMode) revokeVoiceHandoff(cancelRecognition = true)
                else if (voiceMealEntry) stopAssistant(showConfirmation = false)
                contextualVoiceMode = false
                voiceEntryOpen = false
                onVoiceEntryHandled()
                if (voiceMealEntry) onBack()
            },
            onTypeInstead = {
                val transcriptStatus = readCurrentModelStatus()
                val transcriptCapture = voiceCaptureGeneration
                val transcriptProof = voiceRequestProof
                val currentTranscript = if (voiceMealEntry) null else currentReadyChatTranscript(
                    voiceText = voiceText,
                    captureToken = transcriptCapture,
                    transcriptCaptureToken = transcriptStatus.voiceTranscriptionCaptureToken,
                    proofCaptureToken = transcriptProof?.captureToken(),
                    transcriptionState = transcriptStatus.voiceTranscriptionState,
                    transcriptText = transcriptStatus.voiceTranscriptionText,
                )
                voiceCaptureGeneration += 1
                audioRecorder.cancel()
                setVoiceRecording(false)
                currentTranscript?.let {
                    conversationSession.automaticVoiceComposerText.value = it
                }
                voiceAutoSubmitGate?.revoke()
                voiceAutoSubmitGate = null
                if (contextualVoiceMode) {
                    val handoff = voiceContextHandoff
                    val proof = voiceRequestProof
                    val latest = readCurrentModelStatus()
                    val proofToken = proof?.captureToken()
                    val validProof = proof != null && voiceRequestProofIsCurrent(proof, proofToken ?: -1L, latest) &&
                        (handoff == null || handoffIsCurrent(handoff, handoff.captureToken(), latest))
                    val exit = voiceTypingExit
                    val intentionalStopExit = !validProof && exit != null && exit.consume(
                        latest.voiceTranscriptionCaptureToken, latest.assistantRequestId,
                        activeAssistantRequestId, handledReplyRequestId,
                        conversationSession.conversationGeneration.value, isVaultUnlocked(),
                        conversationSession.assistantStopping.value,
                        conversationSession.pendingSubmission.value != null || conversationSession.pendingContinuation.value,
                    )
                    val idleTypingExit = proof == null && exit == null && latest.voiceTranscriptionState == "idle"
                    if (intentionalStopExit || idleTypingExit) {
                        voiceTypingExit = null
                    } else if (!validProof) {
                        exit?.revoke()
                        voiceTypingExit = null
                        suppressVoiceDraftContextOnce = true
                        portionClarification = null
                        pendingSavedDishChoices = null
                        conversationSession.pendingMealConfirmation.value = null
                        revokeVoiceHandoff(cancelRecognition = true)
                        conversationSession.recoverableDiagnostic.value =
                            "Chat changed, so I did not apply the voice request. Your words are in Chat as a new message; review them before sending."
                    } else if (latest.voiceTranscriptionCaptureToken == proofToken) {
                        voiceTypingExit?.revoke()
                        voiceTypingExit = null
                        latest.voiceTranscriptionId?.let { id ->
                            if (latest.voiceTranscriptionState == "processing") cancelContextualVoice(id)
                            else clearContextualVoice(id)
                        }
                    }
                } else if (voiceMealEntry) stopAssistant(showConfirmation = false)
                input = if (voiceMealEntry) voiceText else currentTranscript.orEmpty()
                composerFocusRequest += 1
                voiceEntryOpen = false
                contextualVoiceMode = false
                onVoiceEntryHandled()
            },
            onContinue = {
                input = if (voiceMealEntry) "Record this meal: $voiceText" else voiceText
                if (contextualVoiceMode) {
                    val handoff = voiceContextHandoff
                    val proof = voiceRequestProof
                    val latest = readCurrentModelStatus()
                    if (proof == null || !voiceRequestProofIsCurrent(proof, proof.captureToken(), latest) ||
                        (handoff != null && !handoffIsCurrent(handoff, handoff.captureToken(), latest))
                    ) {
                        revokeVoiceHandoff(cancelRecognition = true)
                        voiceMessage = "Chat changed. Your words are still here. Choose Type instead to send them as a new Chat message."
                        return@VoiceEntrySheet
                    }
                    latest.voiceTranscriptionId?.takeIf {
                        latest.voiceTranscriptionCaptureToken == proof.captureToken() &&
                            latest.voiceTranscriptionState != "processing"
                    }?.let(clearContextualVoice)
                }
                voiceEntryOpen = false
                contextualVoiceMode = false
                onVoiceEntryHandled()
                submitText()
            },
        )
    } } else null
    if (voiceEntryOpen && voiceMealEntry) {
        voiceEntryContent?.invoke()
        return
    }

    val visiblePortionOffer = portionClarification?.takeIf { offer ->
        ChatPortionClarificationPolicy.isOfferCurrent(
            offer, modelStatus.assistantProposal, modelStatus.assistantRequestId,
            conversationSession.conversationGeneration.value, isVaultUnlocked(),
            conversationSession.assistantStopping.value,
        ) && assistantReady(modelStatus) && handledReplyRequestId == offer.requestId &&
            activeAssistantRequestId == offer.requestId &&
            conversationSession.pendingSubmission.value == null && !conversationSession.pendingContinuation.value
    }
    val visibleAcceptedPortionOverride = acceptedPortionOverride?.takeIf { override ->
        ChatPortionClarificationPolicy.isCurrent(
            override, modelStatus.assistantProposal ?: return@takeIf false,
            modelStatus.assistantRequestId, conversationSession.conversationGeneration.value,
            isVaultUnlocked(), conversationSession.assistantStopping.value,
        ) && assistantReady(modelStatus) && activeAssistantRequestId == override.requestId &&
            handledReplyRequestId == override.requestId &&
            conversationSession.pendingSubmission.value == null && !conversationSession.pendingContinuation.value
    }
    val visibleAcceptedFoodReplacementOverride = conversationSession.acceptedFoodReplacementOverride.value?.takeIf { override ->
        ChatCurrentDraftFoodPolicy.isCurrent(
            override, modelStatus.assistantProposal ?: return@takeIf false,
            modelStatus.assistantRequestId, conversationSession.conversationGeneration.value,
            isVaultUnlocked(), conversationSession.assistantStopping.value,
        ) && assistantReady(modelStatus) && activeAssistantRequestId == override.requestId &&
            handledReplyRequestId == override.requestId &&
            conversationSession.pendingSubmission.value == null && !conversationSession.pendingContinuation.value
    }
    val displayedMealProposal = modelStatus.assistantProposal?.let { proposal ->
        visibleAcceptedFoodReplacementOverride?.let { replacement ->
            if (replacement.originalProposal == proposal && replacement.itemIndex in proposal.items.indices) {
                proposal.copy(items = proposal.items.mapIndexed { index, item ->
                    if (index == replacement.itemIndex) item.copy(name = replacement.replacementName, description = "", preparation = "")
                    else item
                })
            } else proposal
        } ?: proposal
    }
    MetriaAssistantConversationScreen(
        bottomBar = bottomBar,
        introductoryConversation = messages == initialPrivateChatMessages(),
        panelActionInFlight = panelActionInFlight,
        onPanelAction = { action ->
            // These are explicit UI destinations, not new model tools. Retain the
            // visible transcript before leaving, using the existing serialized writer.
            val writeTicket = conversationSession.captureConversationWriteTicket()
            if (!panelActionInFlight && writeTicket != null) {
                panelActionInFlight = true
                val generation = conversationSession.conversationGeneration.value
                val transcript = messages.map {
                    PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
                }
                assistantScope.launch {
                    try {
                        val saved = transcript.none { it.fromMember } || conversationSession.persistConversation(
                            writeTicket, transcript, onConversationChanged,
                        ) == ConversationPersistenceResult.SAVED
                        if (isVaultUnlocked() && generation == conversationSession.conversationGeneration.value) {
                            val currentTranscript = messages.map {
                                PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
                            }
                            if (saved && currentTranscript == transcript) onPanelAction(action)
                            else conversationSession.recoverableDiagnostic.value =
                                "Chat could not be saved, so Metria stayed here. Try again."
                        }
                    } finally {
                        panelActionInFlight = false
                    }
                }
            }
        },
        voiceActivity = voiceEntryContent,
        cloudSelected = modelStatus.cloudSelected,
        composerFocusRequest = composerFocusRequest,
        messages = messages.mapIndexed { index, message -> MetriaChatMessage(
            text = message.text,
            fromMember = message.fromMember,
            localEvidence = message.localEvidence,
            savedMealId = message.savedMealId,
            savedMealAvailable = meals.any { meal -> meal.id == message.savedMealId },
            announcePolitely = shouldAnnouncePrivateChatMessage(
                failedRequestId = conversationSession.retainedFailureAnnouncementRequestId.value,
                activeRequestId = activeAssistantRequestId,
                isLast = index == messages.lastIndex,
                fromMember = message.fromMember,
                localEvidence = message.localEvidence,
            ),
        ) },
        input = input,
        onInputChange = {
            input = it
            if (conversationSession.automaticVoiceComposerText.value != it.trim()) {
                conversationSession.automaticVoiceComposerText.value = null
            }
        },
        assistantThinking = shouldPresentAssistantThinking(
            localPending = conversationSession.pendingSubmission.value != null ||
                conversationSession.pendingContinuation.value,
            modelResultIsCurrent = modelResultIsCurrent,
            modelState = modelStatus.state,
            operation = modelStatus.assistantOperation,
        ),
        assistantStopping = conversationSession.assistantStopping.value ||
            (modelStatus.state == "thinking" && modelStatus.voiceTranscriptionState == "cancelled"),
        responseStopped = conversationSession.responseStopped.value,
        mealDraftThinking = modelResultIsCurrent && modelStatus.state == "thinking" &&
            modelStatus.assistantOperation?.let { !it.startsWith("coach") && !it.startsWith("agent") } == true,
        streamingReply = modelStatus.coachStreamingReply.takeIf { modelResultIsCurrent },
        assistantDiagnostic = modelStatus.assistantDiagnostic.takeIf { modelResultIsCurrent },
        coachDiagnostic = if (conversationSession.pendingSubmission.value != null ||
            conversationSession.pendingContinuation.value) null
            else conversationSession.recoverableDiagnostic.value
                ?: modelStatus.coachDiagnostic.takeIf {
                    modelResultIsCurrent && !shouldSuppressConsumedAssistantDiagnostic(
                        activeRequestId = activeAssistantRequestId,
                        handledRequestId = handledReplyRequestId,
                        resultRequestId = modelStatus.assistantRequestId,
                        modelState = modelStatus.state,
                        operation = modelStatus.assistantOperation,
                        diagnostic = it,
                        hasReply = modelStatus.coachReply != null,
                        hasToolCall = modelStatus.coachToolCall != null,
                        hasProposal = modelStatus.assistantProposal != null,
                        hasNavigation = modelStatus.companionNavigation != null,
                    )
                },
        savedWeight = savedWeight,
        mealProposal = displayedMealProposal.takeIf {
            modelResultIsCurrent && assistantReady(modelStatus) &&
                conversationSession.pendingSubmission.value == null &&
                !conversationSession.pendingContinuation.value &&
                !conversationSession.assistantStopping.value
        },
        portionQuestion = visiblePortionOffer?.takeUnless { it.answered }?.let {
            "What was the portion of ${visibleAcceptedFoodReplacementOverride?.replacementName ?: it.target.itemName}, in grams?"
        },
        portionFeedback = visiblePortionOffer?.let { offer ->
            offer.feedback ?: if (offer.answered) {
                offer.grams?.let {
                    "${visibleAcceptedFoodReplacementOverride?.replacementName ?: offer.target.itemName}: $it g is ready in Review."
                } ?: "The original estimate is ready in Review."
            } else null
        } ?: visibleAcceptedPortionOverride?.let {
            "${visibleAcceptedFoodReplacementOverride?.replacementName ?: it.itemName}: ${it.grams} g is ready in Review."
        },
        savedDishChoices = visibleSavedDishChoices,
        savedDishes = personalDishes,
        recordingVoice = recordingVoice,
        preparingVoice = startVoiceImmediately && !recordingVoice,
        voiceMessage = voiceMessage,
        modelReady = assistantReady(modelStatus),
        cloudConnectionRequired = photoAnalysisMode == "cloud" && !modelStatus.cloudReady,
        onOpenCloudSettings = { onPanelAction(AssistantPanelAction.PRIVACY) },
        selectedMealContextTitle = selectedMealContextTitle,
        onClearSelectedMealContext = clearSelectedMealContext,
        onBack = onBack,
        onNewConversation = {
            portionClarification = null
            acceptedPortionOverride = null
            stopAssistant(showConfirmation = false)
            onNewConversation()
        },
        onCancelAssistant = {
            portionClarification = null
            acceptedPortionOverride = null
            stopAssistant()
        },
        onEditSavedWeight = editSavedWeight,
        onUndoSavedWeight = { entry ->
            undoSavedWeight(entry)
            savedWeightId = null
            messages = messages + PrivateChatMessage("${"%.1f".format(entry.kilograms)} kg entry removed.", false)
        },
        onReviewMeal = { proposal ->
            val sourceProposal = modelStatus.assistantProposal ?: return@MetriaAssistantConversationScreen
            val requestId = modelStatus.assistantRequestId
            if (requestId != null && modelResultIsCurrent && isVaultUnlocked()) {
                // Flush the acknowledgement before Review disposes this Chat composition,
                // including when the member taps faster than the normal persistence debounce.
                val acknowledgement = mealDraftTranscriptAcknowledgement(
                    activeAssistantRequestId, handledReplyRequestId, requestId, modelStatus.state,
                    modelStatus.assistantProposal == sourceProposal, modelStatus.assistantOperation,
                    !conversationSession.assistantStopping.value,
                )
                if (acknowledgement != null) {
                    handledReplyRequestId = requestId
                    messages = messages + PrivateChatMessage(acknowledgement, false)
                }
                val transcript = messages.map {
                    PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
                }
                assistantScope.launch {
                    val result = conversationSession.persistBeforeNavigation(
                        requestId, transcript,
                        isCurrent = {
                            isVaultUnlocked() && activeAssistantRequestId == requestId &&
                                conversationSession.isCurrentNavigationRequest(requestId) &&
                                modelStatus.assistantRequestId == requestId && assistantReady(modelStatus) &&
                                modelStatus.assistantProposal == sourceProposal &&
                                conversationSession.pendingSubmission.value == null &&
                                !conversationSession.pendingContinuation.value &&
                                !conversationSession.assistantStopping.value
                        },
                        save = onConversationChanged,
                        navigate = {
                            val offer = visiblePortionOffer?.takeIf {
                                it.proposal == sourceProposal && ChatPortionClarificationPolicy.isOfferCurrent(
                                    it, modelStatus.assistantProposal, modelStatus.assistantRequestId,
                                    conversationSession.conversationGeneration.value, isVaultUnlocked(),
                                    conversationSession.assistantStopping.value,
                                ) && activeAssistantRequestId == it.requestId &&
                                    conversationSession.pendingSubmission.value == null &&
                                    !conversationSession.pendingContinuation.value
                            }
                            val override = offer?.takeIf(ChatPortionClarificationOffer::answered)?.let {
                                ChatPortionClarificationPolicy.overrideForReview(
                                    it.proposal, it.requestId, it.conversationGeneration, it.target,
                                    it.grams?.let { grams -> ChatPortionReply.Grams(grams) } ?: ChatPortionReply.Skip,
                                )
                            } ?: visibleAcceptedPortionOverride?.takeIf { it.originalProposal == sourceProposal }
                            val foodOverride = visibleAcceptedFoodReplacementOverride?.takeIf {
                                it.originalProposal == sourceProposal
                            }
                            reviewMeal(sourceProposal, override, foodOverride)
                            true
                        },
                        repeatable = true,
                    )
                    if (result == ConversationNavigationResult.SAVE_FAILED) {
                        conversationSession.recoverableDiagnostic.value =
                            "Chat could not be saved, so Metria stayed here. Try again."
                    }
                }
            }
        },
        onOpenSavedMeal = openSavedMeal,
        onReviewSavedDish = { id -> selectSavedDishChoice(id) },
        onSelectSavedDish = { id ->
            val expected = personalDishes.singleOrNull { it.id == id }
            val writeTicket = conversationSession.captureConversationWriteTicket()
            val selectionReady = expected != null && isSavedDishCurrent(expected) && isVaultUnlocked() &&
                !panelActionInFlight && !savedDishChoiceSelectionInFlight &&
                conversationSession.pendingSubmission.value == null && !conversationSession.pendingContinuation.value &&
                !conversationSession.assistantStopping.value && !recordingVoice && !startVoiceImmediately && voiceEntryContent == null &&
                modelStatus.state != "thinking" &&
                !shouldPresentAssistantThinking(
                    localPending = false,
                    modelResultIsCurrent = modelStatus.assistantRequestId != null &&
                        modelStatus.assistantRequestId == activeAssistantRequestId,
                    modelState = modelStatus.state,
                    operation = modelStatus.assistantOperation,
                ) && writeTicket != null
            if (!selectionReady) {
                conversationSession.recoverableDiagnostic.value = if (expected == null || !isSavedDishCurrent(expected)) {
                    "That saved dish is no longer available. Choose another one."
                } else if (writeTicket == null) {
                    "This conversation is changing. Wait a moment, then choose the dish again."
                } else {
                    "Metria is still working. Wait for it to finish, then choose the dish again."
                }
            } else {
                panelActionInFlight = true
                val generation = conversationSession.conversationGeneration.value
                val transcript = messages.map {
                    PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
                }
                assistantScope.launch {
                    try {
                        val saved = transcript.none { it.fromMember } || conversationSession.persistConversation(
                            writeTicket, transcript, onConversationChanged,
                        ) == ConversationPersistenceResult.SAVED
                        val currentTranscript = messages.map {
                            PrivateConversationMessage(it.text, it.fromMember, it.localEvidence, it.savedMealId)
                        }
                        if (saved && isVaultUnlocked() && generation == conversationSession.conversationGeneration.value &&
                            currentTranscript == transcript && expected != null && isSavedDishCurrent(expected)
                        ) {
                            if (expected == null || !onReviewSavedDish(expected)) {
                                conversationSession.recoverableDiagnostic.value =
                                    "That saved dish is no longer available. Choose another one."
                            }
                        } else if (isVaultUnlocked() && generation == conversationSession.conversationGeneration.value) {
                            conversationSession.recoverableDiagnostic.value =
                                "Chat could not be saved, so Metria stayed here. Try again."
                        }
                    } finally {
                        panelActionInFlight = false
                    }
                }
            }
        },
        onDiscardMealDraft = {
            portionClarification = null
            acceptedPortionOverride = null
            discardMealDraft()
        },
        onToggleVoice = ::toggleVoice,
        onEditLatestMemberMessage = { editedText -> input = editedText },
        onSend = ::submitText,
    )
}

private fun privateConversationContext(messages: List<PrivateChatMessage>): String =
    PrivateConversationContext.build(messages.map { message ->
        PrivateConversationTurn(
            role = when {
                message.fromMember -> PrivateConversationRole.Member
                message.localEvidence -> PrivateConversationRole.LocalEvidence
                else -> PrivateConversationRole.Assistant
            },
            text = message.text,
        )
    })

/** Historical acknowledgement only: no draft payload or saved-meal claim enters the transcript. */
internal fun mealDraftTranscriptAcknowledgement(
    activeRequestId: Long?,
    handledRequestId: Long?,
    resultRequestId: Long?,
    state: String,
    hasProposal: Boolean,
    operation: String?,
    eligible: Boolean,
): String? = if (eligible && state == "loaded" && hasProposal && operation in setOf("meal", "agent_meal_draft", "agent_meal_portion_edit", "agent_meal_food_edit") &&
    shouldConsumeAssistantResult(activeRequestId, handledRequestId, resultRequestId)
) {
    if (operation == "agent_meal_portion_edit" || operation == "agent_meal_food_edit") "Your current meal draft is still available for review."
    else "I prepared a meal draft for review. Only meals you confirm are saved."
} else null

internal enum class AssistantReplyDeliveryGate {
    CONSUME,
    ACTIVE_REQUEST_MISMATCH,
    ALREADY_HANDLED,
}

internal fun assistantReplyDeliveryGate(
    activeRequestId: Long?,
    handledRequestId: Long?,
    resultRequestId: Long?,
): AssistantReplyDeliveryGate? {
    if (resultRequestId == null) return null
    if (resultRequestId != activeRequestId) return AssistantReplyDeliveryGate.ACTIVE_REQUEST_MISMATCH
    if (resultRequestId == handledRequestId) return AssistantReplyDeliveryGate.ALREADY_HANDLED
    return AssistantReplyDeliveryGate.CONSUME
}

internal fun shouldConsumeAssistantResult(activeRequestId: Long?, handledRequestId: Long?, resultRequestId: Long?): Boolean =
    assistantReplyDeliveryGate(activeRequestId, handledRequestId, resultRequestId) == AssistantReplyDeliveryGate.CONSUME

internal fun shouldLatchAssistantDiagnostic(
    activeRequestId: Long?, resultRequestId: Long?, modelState: String,
    diagnostic: String?, localPending: Boolean,
    handledRequestId: Long? = null,
): Boolean = !localPending && diagnostic != null && modelState == "loaded" &&
    resultRequestId != null && resultRequestId == activeRequestId && resultRequestId != handledRequestId

internal fun retainableAssistantFailure(
    activeRequestId: Long?,
    handledRequestId: Long?,
    resultRequestId: Long?,
    modelState: String,
    operation: String?,
    diagnostic: String?,
    hasReply: Boolean,
    hasToolCall: Boolean,
    hasProposal: Boolean,
    localPending: Boolean,
    stopping: Boolean,
    unlocked: Boolean,
    hasNavigation: Boolean = false,
): String? {
    if (!unlocked || stopping || localPending || hasReply || hasToolCall || hasProposal || hasNavigation) return null
    if (modelState != "loaded" || operation !in setOf(
            "agent_reply", "coach_reply", "agent_turn", "agent_tool_result",
        )) return null
    if (!shouldConsumeAssistantResult(activeRequestId, handledRequestId, resultRequestId)) return null
    return diagnostic?.takeIf(String::isNotBlank)
}

internal fun shouldSuppressConsumedAssistantDiagnostic(
    activeRequestId: Long?,
    handledRequestId: Long?,
    resultRequestId: Long?,
    modelState: String,
    operation: String?,
    diagnostic: String?,
    hasReply: Boolean,
    hasToolCall: Boolean,
    hasProposal: Boolean,
    hasNavigation: Boolean,
): Boolean {
    if (diagnostic.isNullOrBlank() || hasReply || hasToolCall || hasProposal || hasNavigation) return false
    if (modelState != "loaded" || operation !in setOf(
            "agent_reply", "coach_reply", "agent_turn", "agent_tool_result",
        )) return false
    return resultRequestId != null && resultRequestId == activeRequestId && resultRequestId == handledRequestId
}

internal fun shouldAnnouncePrivateChatMessage(
    failedRequestId: Long?,
    activeRequestId: Long?,
    isLast: Boolean,
    fromMember: Boolean,
    localEvidence: Boolean,
): Boolean = failedRequestId != null && failedRequestId == activeRequestId && isLast &&
    !fromMember && !localEvidence

internal fun continuationFailureDiagnostic(modelDiagnostic: String?): String =
    modelDiagnostic?.takeIf(String::isNotBlank)
        ?: "Metria could not complete that local reply. Try asking again."

internal fun shouldPresentAssistantThinking(
    localPending: Boolean,
    modelResultIsCurrent: Boolean,
    modelState: String,
    operation: String?,
): Boolean = localPending || (modelResultIsCurrent && (
    operation == "agent_waiting_tool" ||
        (modelState == "thinking" &&
            operation?.let { it.startsWith("coach") || it.startsWith("agent") } == true)
    ))

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
private fun LabelConfirmationSheet(
    draft: LabelNutritionDraft,
    referenceThumbnailBase64: String?,
    initialMealSlot: String? = null,
    initialRecordedDate: String? = null,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var date by remember(draft, initialRecordedDate) { mutableStateOf(initialRecordedDate ?: LocalDate.now().toString()) }
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
                    mealSlot = initialMealSlot?.takeIf { it in MealSlots.allowed },
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
