package app.metria

import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    private lateinit var modelLifecycle: ModelLifecycle
    private lateinit var vault: EncryptedVault
    private lateinit var biometricPrompt: BiometricPrompt
    private var vaultCallback: ((EncryptedVault.Status) -> Unit)? = null
    private var pendingVaultOperation: VaultOperation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelLifecycle = ModelLifecycle(applicationContext, filesDir, cacheDir)
        vault = EncryptedVault(applicationContext)
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val cipher = result.cryptoObject?.cipher
                    val operation = pendingVaultOperation
                    pendingVaultOperation = null
                    vaultCallback?.invoke(if (cipher == null || operation == null) {
                        EncryptedVault.Status.Failed("Vault authentication did not return a usable key operation.")
                    } else if (operation == VaultOperation.Create) {
                        vault.createAfterAuthentication(cipher)
                    } else {
                        vault.unlockAfterAuthentication(cipher)
                    })
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    pendingVaultOperation = null
                    vaultCallback?.invoke(EncryptedVault.Status.Failed("Vault unlock was cancelled or unavailable."))
                }
            })
        setContent { MetriaApp(modelLifecycle, vault, this) }
    }

    override fun onDestroy() {
        vault.lock()
        modelLifecycle.close()
        super.onDestroy()
    }

    override fun onStop() {
        vault.lock()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        vaultCallback?.invoke(vault.status())
    }

    fun setVaultCallback(callback: ((EncryptedVault.Status) -> Unit)?) {
        vaultCallback = callback
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
                VaultSection(vaultStatus, activity) { kilograms, note ->
                    vaultStatus = vault.addWeight(kilograms, note)
                }
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
    addWeight: (Double, String?) -> Unit,
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
        is EncryptedVault.Status.Unlocked -> WeightEntrySection(status.entries, addWeight)
        is EncryptedVault.Status.RecoveryRequired -> Text(status.message)
        is EncryptedVault.Status.Failed -> Text(status.message)
    }
}

@Composable
private fun WeightEntrySection(entries: List<WeightEntry>, addWeight: (Double, String?) -> Unit) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pounds by remember { mutableStateOf(false) }
    Text("Vault unlocked — entries stay on this phone.")
    OutlinedTextField(value, { value = it }, label = { Text(if (pounds) "Weight (lb)" else "Weight (kg)") })
    Column(horizontalAlignment = Alignment.Start) {
        Checkbox(checked = pounds, onCheckedChange = { pounds = it })
        Text("Enter in pounds")
    }
    OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
    Button(onClick = {
        val input = value.replace(',', '.').toDoubleOrNull()
        if (input == null) return@Button
        addWeight(if (pounds) WeightUnits.poundsToKilograms(input) else input, note)
        value = ""
        note = ""
    }) { Text("Save weight locally") }
    Text("${entries.size} local weight ${if (entries.size == 1) "entry" else "entries"}")
    entries.sortedByDescending(WeightEntry::recordedAt).take(10).forEach { entry ->
        Text("${entry.kilograms} kg — ${entry.recordedAt}")
        entry.note?.let { Text(it) }
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
