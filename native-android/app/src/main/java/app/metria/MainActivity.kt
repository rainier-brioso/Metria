package app.metria

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : FragmentActivity() {
    private lateinit var modelLifecycle: ModelLifecycle
    private lateinit var assistantTraceStore: AssistantTraceStore
    private lateinit var debugImageCaptureStore: DebugImageCaptureStore
    private lateinit var vault: EncryptedVault
    private lateinit var biometricPrompt: BiometricPrompt
    private var vaultCallback: ((EncryptedVault.Status) -> Unit)? = null
    private var pendingVaultOperation: VaultOperation? = null
    private val firstGoalOffer = FirstGoalOffer()
    private var voiceDraftCallback: ((VoiceWeightDraft) -> Unit)? = null
    private var pendingVoiceDraft: VoiceWeightDraft? = null
    private var pendingCameraCapture: File? = null
    private var externalImageFlowInProgress = false
    private var externalArchiveFlowInProgress = false
    private val onboardingPreferences by lazy { getSharedPreferences("metria-onboarding", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assistantTraceStore = AssistantTraceStore(noBackupFilesDir, BuildConfig.DEBUG)
        debugImageCaptureStore = DebugImageCaptureStore(noBackupFilesDir, BuildConfig.DEBUG)
        modelLifecycle = ModelLifecycle(applicationContext, filesDir, cacheDir, assistantTraceStore, debugImageCaptureStore)
        vault = EncryptedVault(applicationContext)
        modelLifecycle.setAnalysisAuditSink { audit ->
            runOnUiThread {
                // Audit metadata is retained only in the already-unlocked local vault.
                // A locked/backgrounded app intentionally cannot write health metadata.
                val saved = vault.recordAnalysisAudit(audit)
                if (saved is EncryptedVault.Status.Unlocked) vaultCallback?.invoke(saved)
            }
        }
        handleVoiceIntent(intent)
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val cipher = result.cryptoObject?.cipher
                    val operation = pendingVaultOperation
                    pendingVaultOperation = null
                    val vaultResult = if (cipher == null || operation == null) {
                        EncryptedVault.Status.Failed("Vault authentication did not return a usable key operation.")
                    } else if (operation == VaultOperation.Create) {
                        vault.createAfterAuthentication(cipher)
                    } else {
                        vault.unlockAfterAuthentication(cipher)
                    }
                    if (operation == VaultOperation.Create) firstGoalOffer.recordCreation(vaultResult is EncryptedVault.Status.Unlocked)
                    vaultCallback?.invoke(vaultResult)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    pendingVaultOperation = null
                    vaultCallback?.invoke(EncryptedVault.Status.Failed("Vault unlock was cancelled or unavailable."))
                }
            })
        setContent { MetriaMemberShell(vault, this) }
    }

    override fun onDestroy() {
        firstGoalOffer.clear()
        vault.lock()
        modelLifecycle.close()
        assistantTraceStore.close()
        debugImageCaptureStore.close()
        super.onDestroy()
    }

    override fun onStop() {
        firstGoalOffer.clear()
        if (!externalImageFlowInProgress && !externalArchiveFlowInProgress) {
            modelLifecycle.cancelImageAnalysis()
            if (modelLifecycle.status().imageState in setOf("ready", "preparing", "reading_label")) {
                modelLifecycle.discardPreparedImage()
            }
            vault.lock()
            modelLifecycle.lockPrivateAgentConversation()
            modelLifecycle.clearDeveloperResponse()
            debugImageCaptureStore.clear()
        } else {
            // Keep the opt-in through the system picker, but never leave a previous
            // raw capture on disk while another activity is in front of Metria.
            debugImageCaptureStore.clear()
        }
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        vaultCallback?.invoke(vault.status())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceIntent(intent)
    }

    fun setVoiceDraftCallback(callback: ((VoiceWeightDraft) -> Unit)?) {
        voiceDraftCallback = callback
        pendingVoiceDraft?.let { draft -> callback?.invoke(draft) }
    }

    private fun handleVoiceIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "metria" || data.host != "weight") return
        val kilograms = data.getQueryParameter("weight")?.toDoubleOrNull() ?: return
        if (!WeightUnits.isValidKilograms(kilograms)) return
        val date = data.getQueryParameter("date")?.let {
            runCatching { java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull()
        } ?: java.time.Instant.now()
        VoiceWeightDraft(kilograms, date).also {
            pendingVoiceDraft = it
            voiceDraftCallback?.invoke(it)
        }
    }

    fun setVaultCallback(callback: ((EncryptedVault.Status) -> Unit)?) {
        vaultCallback = callback
    }

    fun consumeFirstGoalOffer(): Boolean = firstGoalOffer.take(vault.status() is EncryptedVault.Status.Unlocked)

    /** This stores no health data; it only prevents repeating the first-run explanation. */
    fun hasCompletedOnboarding(): Boolean = onboardingPreferences.getBoolean("completed", false)

    fun completeOnboarding() {
        onboardingPreferences.edit().putBoolean("completed", true).apply()
    }

    fun modelStatus(): ModelStatus = modelLifecycle.status()

    fun assistantTraceStatus(): AssistantTraceStore.Status = assistantTraceStore.status()

    fun recordAssistantReplyDelivery(requestId: Long, reason: String) =
        assistantTraceStore.recordReplyDelivery(requestId, reason)

    fun debugImageCaptureStatus(): DebugImageCaptureStore.Status = debugImageCaptureStore.status()

    fun setDebugImageCaptureEnabled(enabled: Boolean): Boolean = debugImageCaptureStore.setEnabled(enabled)

    fun clearDebugImageCapture() = debugImageCaptureStore.clear()

    fun setAssistantTraceEnabled(enabled: Boolean) = assistantTraceStore.setEnabled(enabled)

    fun clearAssistantTrace() = assistantTraceStore.clear()

    fun downloadLocalAssistant(): ModelStatus = modelLifecycle.download()

    fun cancelLocalAssistantDownload(): ModelStatus = modelLifecycle.cancelDownload()

    fun prepareLocalAssistant(): ModelStatus = modelLifecycle.initializeRuntime()

    fun askLocalMealAssistant(description: String): ModelStatus = modelLifecycle.proposeMealFromText(description)

    fun suggestMealFoodName(currentName: String, correction: String): ModelStatus =
        modelLifecycle.suggestMealFoodName(currentName, correction)

    fun cancelMealFoodNameCorrection(requestId: Long): ModelStatus =
        modelLifecycle.cancelMealFoodNameCorrection(requestId)

    fun clearMealFoodNameCorrection(): ModelStatus = modelLifecycle.clearMealFoodNameCorrection()

    fun askPrivateCoach(question: String, snapshot: PrivateCoachSnapshot, conversationContext: String = ""): ModelStatus =
        modelLifecycle.proposeCoachInsight(question, snapshot, conversationContext)

    fun understandPrivateCoach(question: String, conversationContext: String = ""): ModelStatus =
        modelLifecycle.understandCoachRequest(question, conversationContext)

    fun understandPrivateCoachAudio(audioFile: java.io.File, conversationContext: String = ""): ModelStatus =
        modelLifecycle.understandCoachAudio(audioFile, conversationContext)

    fun startPrivateAgentTurn(
        question: String,
        history: List<PrivateConversationMessage>,
        selectedMealId: String? = null,
        confirmation: NativeMealDraftPolicy.MealConfirmationContext? = null,
    ): ModelStatus = modelLifecycle.startPrivateAgentTurn(question, history, selectedMealId, confirmation)

    fun setAssistantStyle(style: EncryptedVault.AssistantStyle): ModelStatus = modelLifecycle.setAssistantStyle(style)

    fun continuePrivateAgentTool(
        toolName: String,
        response: Map<String, Any?>,
    ): ModelStatus = modelLifecycle.continuePrivateAgentTool(toolName, response)

    fun resumeSavedDishProbeAsNormalAgentTurn(
        expectedRequestId: Long,
        call: PrivateCoachToolCall,
        originalQuestion: String,
    ): ModelStatus = modelLifecycle.resumeSavedDishProbeAsNormalAgentTurn(expectedRequestId, call, originalQuestion)

    fun completeCompanionNavigation(requestId: Long): ModelStatus? =
        modelLifecycle.completeCompanionNavigation(requestId)

    fun cancelPrivateAssistant(): ModelStatus = modelLifecycle.cancelAssistantRequest()

    fun clearLocalMealDraft(): ModelStatus = modelLifecycle.clearMealDraft()

    fun discardImageMealDraft(): ModelStatus = modelLifecycle.discardImageMealDraft()

    fun discardAssistantMealDraft(): ModelStatus = modelLifecycle.discardAssistantMealDraft()

    fun prepareLocalMealImage(uri: Uri): ModelStatus {
        externalImageFlowInProgress = false
        return modelLifecycle.prepareImage(contentResolver, uri)
    }

    fun beginLocalImageFlow(): ModelStatus = modelLifecycle.beginImageFlow()

    fun finishLocalImageCooldown(): ModelStatus = modelLifecycle.finishImageCooldown()

    fun beginExternalImageFlow() {
        externalImageFlowInProgress = true
    }

    fun newLocalCameraCaptureUri(): Uri {
        externalImageFlowInProgress = true
        val file = File.createTempFile("metria-camera-", ".jpg", cacheDir)
        pendingCameraCapture?.delete()
        pendingCameraCapture = file
        return FileProvider.getUriForFile(this, "$packageName.capture", file)
    }

    fun prepareLocalCapturedMealImage(uri: Uri): ModelStatus {
        externalImageFlowInProgress = false
        val file = pendingCameraCapture
        pendingCameraCapture = null
        return modelLifecycle.prepareImage(contentResolver, uri, file)
    }

    fun discardPendingCameraCapture() {
        externalImageFlowInProgress = false
        pendingCameraCapture?.delete()
        pendingCameraCapture = null
    }

    fun analyzeLocalMealImage(description: String): ModelStatus = modelLifecycle.analyzePreparedImage(description)
    fun cancelLocalMealImageAnalysis(): ModelStatus = modelLifecycle.cancelImageAnalysis()

    fun readLocalNutritionLabel(): ModelStatus = modelLifecycle.readNutritionLabel()

    fun discardLocalMealImage(): ModelStatus = modelLifecycle.discardPreparedImage()

    fun beginExternalArchiveFlow() { externalArchiveFlowInProgress = true }

    fun finishExternalArchiveFlow() { externalArchiveFlowInProgress = false }

    fun exportRecoveryArchive(destination: Uri, passphrase: String): Result<Unit> {
        val secret = passphrase.toCharArray()
        return try {
            vault.exportRecoveryArchive(secret).mapCatching { archive ->
                contentResolver.openOutputStream(destination)?.use { it.write(archive) }
                    ?: error("Metria could not write the selected archive file.")
            }
        } finally {
            secret.fill('\u0000')
            externalArchiveFlowInProgress = false
        }
    }

    fun inspectRecoveryArchive(source: Uri, passphrase: String): Result<EncryptedVault.ArchiveSummary> {
        val secret = passphrase.toCharArray()
        return try {
            val archive = contentResolver.openInputStream(source)?.use { input -> input.readNBytes(8 * 1024 * 1024 + 1) }
                ?: return Result.failure(IllegalStateException("Metria could not read the selected archive."))
            if (archive.size > 8 * 1024 * 1024) return Result.failure(IllegalArgumentException("This archive is too large to restore safely."))
            vault.inspectRecoveryArchive(archive, secret)
        } finally {
            secret.fill('\u0000')
            externalArchiveFlowInProgress = false
        }
    }

    fun confirmRecoveryRestore(): Result<EncryptedVault.ArchiveSummary> = vault.confirmRecoveryRestore()

    fun lockVault() {
        firstGoalOffer.clear()
        vault.lock()
        modelLifecycle.lockPrivateAgentConversation()
        modelLifecycle.clearDeveloperResponse()
        debugImageCaptureStore.clear()
        vaultCallback?.invoke(vault.status())
    }

    fun authenticateVault(create: Boolean) {
        val cipher = try {
            if (create) vault.createCipherForSetup() else vault.createCipherForUnlock()
        } catch (_: KeyPermanentlyInvalidatedException) {
            vaultCallback?.invoke(EncryptedVault.Status.RecoveryRequired(
                "Your phone's biometric security changed, so Android invalidated this vault key."
            ))
            return
        } catch (_: Exception) {
            vaultCallback?.invoke(EncryptedVault.Status.Failed("Vault authentication could not start."))
            return
        }
        pendingVaultOperation = if (create) VaultOperation.Create else VaultOperation.Unlock
        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (create) "Protect your Metria vault" else "Unlock your Metria vault")
                .setSubtitle("Your weight entries stay encrypted on this phone.")
                .setNegativeButtonText("Cancel")
                .build(),
            BiometricPrompt.CryptoObject(cipher),
        )
    }

    private enum class VaultOperation { Create, Unlock }
}

data class VoiceWeightDraft(val kilograms: Double, val recordedAt: java.time.Instant)

@Composable
private fun MemberApp(vault: EncryptedVault, activity: MainActivity) {
    var status by remember { mutableStateOf(vault.status()) }
    var tab by remember { mutableStateOf("Today") }
    var editorFor by remember { mutableStateOf<WeightEntry?>(null) }
    var showingEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WeightEntry?>(null) }
    DisposableEffect(activity) {
        activity.setVaultCallback { status = it }
        onDispose { activity.setVaultCallback(null) }
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Metria", style = MaterialTheme.typography.displaySmall)
                Text("Private wellness, on your phone.", style = MaterialTheme.typography.titleMedium)
                when (val vaultStatus = status) {
                    EncryptedVault.Status.Missing -> {
                        Text("Create your encrypted local vault. Metria does not create an account or upload your entries.")
                        Button(onClick = { activity.authenticateVault(create = true) }) { Text("Create private vault") }
                    }
                    EncryptedVault.Status.Locked -> {
                        Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Your vault is locked", style = MaterialTheme.typography.titleLarge)
                            Text("Unlock with your fingerprint to view weights and trends.")
                            Button(onClick = { activity.authenticateVault(create = false) }) { Text("Unlock vault") }
                        } }
                    }
                    is EncryptedVault.Status.RecoveryRequired -> Text(vaultStatus.message)
                    is EncryptedVault.Status.Failed -> {
                        Text(vaultStatus.message)
                        Button(onClick = { status = vault.status() }) { Text("Try again") }
                    }
                    is EncryptedVault.Status.Unlocked -> {
                        Text("Today · History · Privacy")
                        Button(onClick = { tab = "Today" }) { Text("Today") }
                        Button(onClick = { tab = "History" }) { Text("History") }
                        Button(onClick = { tab = "Privacy" }) { Text("Privacy") }
                        when (tab) {
                            "Today" -> {
                                val latest = vaultStatus.entries.maxByOrNull(WeightEntry::recordedAt)
                                Text("Today", style = MaterialTheme.typography.headlineMedium)
                                Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (latest == null) "No weight recorded yet" else "Latest weight")
                                    Text(latest?.let { "${"%.1f".format(it.kilograms)} kg" } ?: "Add your first entry", style = MaterialTheme.typography.displaySmall)
                                    Text(WeightTrend.description(WeightTrend.filter(vaultStatus.entries, LocalDate.now().minusDays(29), LocalDate.now())))
                                } }
                                Button(onClick = { editorFor = null; showingEditor = true }) { Text("Add weight") }
                            }
                            "History" -> {
                                Text("History", style = MaterialTheme.typography.headlineMedium)
                                WeightTrendSection(vaultStatus.entries)
                                vaultStatus.entries.sortedByDescending(WeightEntry::recordedAt).forEach { entry ->
                                    Card { Column(Modifier.padding(16.dp)) {
                                        Text("${"%.1f".format(entry.kilograms)} kg · ${entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}")
                                        entry.note?.let { Text(it) }
                                        Button(onClick = { editorFor = entry; showingEditor = true }) { Text("Edit") }
                                        Button(onClick = { deleting = entry }) { Text("Delete") }
                                    } }
                                }
                            }
                            else -> {
                                Text("Privacy", style = MaterialTheme.typography.headlineMedium)
                                Text("Your entries are encrypted on this phone. Metria has no account, default sync, advertising, or analytics.")
                                Text("Photos used for optional local assistance are temporary and deleted after processing.")
                                Button(onClick = { activity.lockVault(); tab = "Today" }) { Text("Lock vault now") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showingEditor && status is EncryptedVault.Status.Unlocked) {
        WeightEditor(editorFor, onDismiss = { showingEditor = false }) { kilograms, note, recordedAt ->
            status = editorFor?.let { vault.updateWeight(it.id, kilograms, note, recordedAt) }
                ?: vault.addWeight(kilograms, note, recordedAt)
            showingEditor = false
        }
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Delete weight entry?") },
            text = { Text("This removes ${"%.1f".format(entry.kilograms)} kg from this phone's encrypted vault.") },
            confirmButton = { Button(onClick = { status = vault.deleteWeight(entry.id); deleting = null }) { Text("Delete") } },
            dismissButton = { Button(onClick = { deleting = null }) { Text("Keep entry") } },
        )
    }
}

@Composable
private fun WeightEditor(entry: WeightEntry?, onDismiss: () -> Unit, onSave: (Double, String?, java.time.Instant) -> Unit) {
    var value by remember(entry) { mutableStateOf(entry?.kilograms?.toString().orEmpty()) }
    var note by remember(entry) { mutableStateOf(entry?.note.orEmpty()) }
    var date by remember(entry) { mutableStateOf(entry?.recordedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (entry == null) "Add weight" else "Edit weight") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value, { value = it }, label = { Text("Weight (kg)") })
            OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD, optional)") })
            OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
            error?.let { Text(it) }
        }
    }, confirmButton = { Button(onClick = {
        val kilograms = value.replace(',', '.').toDoubleOrNull()
        val recordedAt = if (date.isBlank()) java.time.Instant.now() else runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
        if (kilograms == null || recordedAt == null || !WeightUnits.isValidKilograms(kilograms)) error = "Enter 15–500 kg and a valid optional date." else onSave(kilograms, note, recordedAt)
    }) { Text("Save") } }, dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun MetriaApp(modelLifecycle: ModelLifecycle, vault: EncryptedVault, activity: MainActivity) {
    var status by remember { mutableStateOf(modelLifecycle.status()) }
    var vaultStatus by remember { mutableStateOf(vault.status()) }
    val context = LocalContext.current
    LaunchedEffect(modelLifecycle) {
        while (true) {
            status = modelLifecycle.status()
            delay(500)
        }
    }
    DisposableEffect(modelLifecycle) {
        onDispose { /* Activity owns lifecycle shutdown. */ }
    }
    DisposableEffect(activity) {
        activity.setVaultCallback { vaultStatus = it }
        onDispose { activity.setVaultCallback(null) }
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            status = modelLifecycle.prepareImage(
                contentResolver = context.contentResolver,
                sourceUri = uri,
            )
        }
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Metria", style = MaterialTheme.typography.displaySmall)
                VaultSection(
                    vaultStatus,
                    activity,
                    addWeight = { kilograms, note, recordedAt ->
                        vaultStatus = vault.addWeight(kilograms, note, recordedAt)
                    },
                    updateWeight = { id, kilograms, note, recordedAt ->
                        vaultStatus = vault.updateWeight(id, kilograms, note, recordedAt)
                    },
                    deleteWeight = { id -> vaultStatus = vault.deleteWeight(id) },
                    refresh = { vaultStatus = vault.status() },
                )
                Text(
                    "Native Android vision spike",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Photos and screenshots are processed locally after you choose them. " +
                        "Analysis can take up to a minute. Metria deletes its temporary copy after analysis " +
                        "and cannot save an entry in this spike.",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text("Model: ${status.state}", modifier = Modifier.padding(top = 24.dp))
                Text(status.reason, modifier = Modifier.padding(top = 8.dp))
                Text(status.backendSummary)
                Text(status.diagnostics)
                Text("Image: ${status.imageState}")
                Text(status.imageReason)
                status.proposalDiagnostic?.let { Text("Rejected format: $it") }
                status.testOutput?.let { Text("Smoke-test output: $it") }
                Text("Storage: ${status.downloadedBytes} / ${status.expectedBytes} bytes")
                Button(onClick = { status = modelLifecycle.download() }) {
                    Text("Download model")
                }
                Button(onClick = { status = modelLifecycle.cancelDownload() }) {
                    Text("Cancel download")
                }
                Button(onClick = { status = modelLifecycle.verifyInstalledModel() }) {
                    Text("Verify stored model")
                }
                Button(onClick = { status = modelLifecycle.initializeRuntime() }) {
                    Text("Initialize LiteRT")
                }
                Button(onClick = { status = modelLifecycle.unloadRuntime() }) {
                    Text("Unload LiteRT")
                }
                Button(onClick = { status = modelLifecycle.runTextSmokeTest() }) {
                    Text("Run offline text test")
                }
                Button(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Choose photo or screenshot")
                }
                Button(onClick = { status = modelLifecycle.analyzePreparedImage() }) {
                    Text("Analyze selected image locally")
                }
                Button(onClick = { status = modelLifecycle.cancelImageAnalysis() }) {
                    Text("Stop local analysis")
                }
                Button(onClick = { status = modelLifecycle.readNutritionLabel() }) {
                    Text("Read nutrition label locally")
                }
                Button(onClick = { status = modelLifecycle.discardPreparedImage() }) {
                    Text("Discard selected image")
                }
                status.proposal?.let { proposal -> EditableProposal(proposal) }
                status.ocrText?.let { text ->
                    Text("Local label text — not saved", style = MaterialTheme.typography.titleMedium)
                    Text(text)
                    Text("There is no save action in this vision spike.")
                }
                status.labelDraft?.let { draft -> EditableLabelDraft(draft) }
                Button(onClick = { status = modelLifecycle.remove() }) {
                    Text("Remove model")
                }
            }
        }
    }
}

@Composable
private fun VaultSection(
    status: EncryptedVault.Status,
    activity: MainActivity,
    addWeight: (Double, String?, java.time.Instant) -> Unit,
    updateWeight: (String, Double, String?, java.time.Instant) -> Unit,
    deleteWeight: (String) -> Unit,
    refresh: () -> Unit,
) {
    Text("Private weight vault", style = MaterialTheme.typography.titleLarge)
    when (status) {
        EncryptedVault.Status.Missing -> {
            Text("Create an encrypted local vault before recording weight. No account is created.")
            Button(onClick = { activity.authenticateVault(create = true) }) { Text("Create encrypted vault") }
        }
        EncryptedVault.Status.Locked -> {
            Text("Your entries are encrypted and locked.")
            Button(onClick = { activity.authenticateVault(create = false) }) { Text("Unlock vault") }
        }
        is EncryptedVault.Status.Unlocked -> {
            WeightEntrySection(status.entries, addWeight, updateWeight, deleteWeight)
            WeightTrendSection(status.entries)
        }
        is EncryptedVault.Status.RecoveryRequired -> Text(status.message)
        is EncryptedVault.Status.Failed -> {
            Text(status.message)
            Button(onClick = refresh) { Text("Return to vault") }
        }
    }
}

@Composable
private fun WeightTrendSection(entries: List<WeightEntry>) {
    var range by remember { mutableStateOf("7 days") }
    var average by remember { mutableStateOf(false) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val start = when (range) {
        "7 days" -> today.minusDays(6)
        "30 days" -> today.minusDays(29)
        else -> runCatching { LocalDate.parse(startText) }.getOrNull()
    }
    val end = if (range == "Custom") runCatching { LocalDate.parse(endText) }.getOrNull() else today
    val points = WeightTrend.filter(entries, start, end)
    val displayed = if (average) WeightTrend.movingAverage(points) else points
    Text("Local weight trend", style = MaterialTheme.typography.titleLarge)
    Button(onClick = { range = "7 days" }) { Text("7 days") }
    Button(onClick = { range = "30 days" }) { Text("30 days") }
    Button(onClick = { range = "Custom" }) { Text("Custom range") }
    Checkbox(checked = average, onCheckedChange = { average = it })
    Text("Show 3-entry moving average")
    if (range == "Custom") {
        OutlinedTextField(startText, { startText = it }, label = { Text("Start (YYYY-MM-DD)") })
        OutlinedTextField(endText, { endText = it }, label = { Text("End (YYYY-MM-DD)") })
    }
    Text(WeightTrend.description(points))
    if (displayed.size >= 2) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val low = displayed.minOf(WeightTrendPoint::kilograms)
            val high = displayed.maxOf(WeightTrendPoint::kilograms)
            val span = (high - low).takeIf { it > 0.0 } ?: 1.0
            val step = size.width / (displayed.size - 1)
            for (index in 1 until displayed.size) {
                val previous = displayed[index - 1]
                val current = displayed[index]
                val from = Offset((index - 1) * step, size.height - (((previous.kilograms - low) / span) * size.height).toFloat())
                val to = Offset(index * step, size.height - (((current.kilograms - low) / span) * size.height).toFloat())
                drawLine(Color(0xFF5E35B1), from, to, strokeWidth = 6f)
            }
        }
    }
}

@Composable
private fun WeightEntrySection(
    entries: List<WeightEntry>,
    addWeight: (Double, String?, java.time.Instant) -> Unit,
    updateWeight: (String, Double, String?, java.time.Instant) -> Unit,
    deleteWeight: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var pounds by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WeightEntry?>(null) }
    var deleting by remember { mutableStateOf<WeightEntry?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }
    fun recordedAtOrNull() = if (date.isBlank()) java.time.Instant.now() else runCatching {
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant()
    }.getOrNull()
    fun submit() {
        val input = value.replace(',', '.').toDoubleOrNull()
        val recordedAt = recordedAtOrNull()
        if (input == null || recordedAt == null) {
            inputError = "Enter a valid weight and an optional date as YYYY-MM-DD."
            return
        }
        val kilograms = if (pounds) WeightUnits.poundsToKilograms(input) else input
        editing?.let { updateWeight(it.id, kilograms, note, recordedAt) }
            ?: addWeight(kilograms, note, recordedAt)
        value = ""
        note = ""
        date = ""
        editing = null
        inputError = null
    }
    Text("Vault unlocked — entries stay on this phone.")
    editing?.let { Text("Editing a local entry") }
    OutlinedTextField(value, { value = it }, label = { Text(if (pounds) "Weight (lb)" else "Weight (kg)") })
    Column(horizontalAlignment = Alignment.Start) {
        Checkbox(checked = pounds, onCheckedChange = { pounds = it })
        Text("Enter in pounds")
    }
    OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
    OutlinedTextField(date, { date = it }, label = { Text("Optional date (YYYY-MM-DD)") })
    inputError?.let { Text(it) }
    Button(onClick = ::submit) { Text(if (editing == null) "Save weight locally" else "Save changes locally") }
    if (editing != null) Button(onClick = {
        editing = null
        value = ""
        note = ""
        date = ""
        inputError = null
    }) { Text("Cancel edit") }
    Text("${entries.size} local weight ${if (entries.size == 1) "entry" else "entries"}")
    entries.sortedByDescending(WeightEntry::recordedAt).take(10).forEach { entry ->
        Text("${entry.kilograms} kg — ${entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()}")
        entry.note?.let { Text(it) }
        Button(onClick = {
            editing = entry
            pounds = false
            value = entry.kilograms.toString()
            note = entry.note.orEmpty()
            date = entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString()
            inputError = null
        }) { Text("Edit") }
        Button(onClick = { deleting = entry }) { Text("Delete") }
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete local weight entry?") },
            text = { Text("This removes ${entry.kilograms} kg from this phone's encrypted vault.") },
            confirmButton = { Button(onClick = { deleteWeight(entry.id); deleting = null }) { Text("Delete") } },
            dismissButton = { Button(onClick = { deleting = null }) { Text("Keep entry") } },
        )
    }
}

@Composable
private fun EditableLabelDraft(draft: LabelNutritionDraft) {
    var calories by remember(draft) { mutableStateOf(draft.caloriesKcal?.toString().orEmpty()) }
    var protein by remember(draft) { mutableStateOf(draft.proteinGrams?.toString().orEmpty()) }
    var carbohydrate by remember(draft) { mutableStateOf(draft.carbohydrateGrams?.toString().orEmpty()) }
    var fat by remember(draft) { mutableStateOf(draft.totalFatGrams?.toString().orEmpty()) }

    Text("Editable OCR label draft — not saved", style = MaterialTheme.typography.titleMedium)
    Text("Verify each value against the packaging. OCR can make mistakes.")
    OutlinedTextField(calories, { calories = it }, label = { Text("Calories (kcal)") })
    OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") })
    OutlinedTextField(carbohydrate, { carbohydrate = it }, label = { Text("Carbohydrates (g)") })
    OutlinedTextField(fat, { fat = it }, label = { Text("Total fat (g)") })
    Text("There is no save action in this vision spike.")
}

@Composable
private fun EditableProposal(proposal: MealProposal) {
    Text("Editable meal proposal — not saved", style = MaterialTheme.typography.titleMedium)
    Text("Confidence: ${proposal.overallConfidence}. Review every field before any future save.")
    proposal.items.forEachIndexed { index, item ->
        var name by remember(proposal, index) { mutableStateOf(item.name) }
        var portion by remember(proposal, index) { mutableStateOf(item.visiblePortion) }
        Text("Item ${index + 1}: ${item.confidence} confidence, ${item.estimatedGramsRange.min}-${item.estimatedGramsRange.max} g")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food") },
        )
        OutlinedTextField(
            value = portion,
            onValueChange = { portion = it },
            label = { Text("Visible portion") },
        )
        Text("Preparation: ${item.preparation}")
    }
    if (proposal.items.isEmpty()) Text("No food was confidently identified. Enter details manually.")
    proposal.assumptions.forEach { Text("Assumption: $it") }
    proposal.questions.forEach { Text("Question: $it") }
    Text("There is no save action in this vision spike.")
}
