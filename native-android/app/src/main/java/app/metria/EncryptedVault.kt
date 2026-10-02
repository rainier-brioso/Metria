package app.metria

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * M2 decision: a biometric-authenticated Android Keystore key unwraps a random
 * vault data key. The data key exists only for the foreground session and is
 * cleared on lock; encrypted files never contain a plaintext reusable key.
 */
class EncryptedVault(context: Context) {
    enum class AssistantStyle(val storageValue: String, val title: String, val description: String) {
        BALANCED("balanced", "Balanced", "Warm and clear, with concise answers."),
        COACH("coach", "Coach", "Encouraging and reflective, with one gentle next step."),
        RECORDS("records", "Records assistant", "Direct and evidence-first, with less framing."),
        ;
        companion object {
            fun fromStorage(value: String?): AssistantStyle = entries.firstOrNull { it.storageValue == value } ?: BALANCED
        }
    }

    data class ArchiveSummary(val weights: Int, val meals: Int)

    sealed interface Status {
        data object Missing : Status
        data object Locked : Status
        data class Unlocked(
            val entries: List<WeightEntry>,
            val displayUnit: String = "kg",
            val meals: List<MealEntry> = emptyList(),
            val foodCorrections: List<FoodCorrection> = emptyList(),
            val mealCorrectionMemories: List<MealCorrectionMemory> = emptyList(),
            val personalDishes: List<PersonalDish> = emptyList(),
            val analysisAudits: List<LocalAnalysisAudit> = emptyList(),
            val nutritionTargets: NutritionTargets? = null,
            val cycleContext: CycleContext = CycleContext(),
            val assistantStyle: AssistantStyle = AssistantStyle.BALANCED,
            val activeConversation: PrivateConversationRecord? = null,
            val conversationExpired: Boolean = false,
        ) : Status
        data class RecoveryRequired(val message: String) : Status
        data class Failed(val message: String) : Status
    }

    private val vaultDirectory = File(context.filesDir, "vault")
    private val envelopeFile = File(vaultDirectory, "key-envelope.json")
    private val dataFile = File(vaultDirectory, "weights.json.enc")
    private var sessionKey: ByteArray? = null
    private var entries: MutableList<WeightEntry> = mutableListOf()
    private var meals: MutableList<MealEntry> = mutableListOf()
    private var foodCorrections: MutableList<FoodCorrection> = mutableListOf()
    private var mealCorrectionMemories: MutableList<MealCorrectionMemory> = mutableListOf()
    private var personalDishes: MutableList<PersonalDish> = mutableListOf()
    private var analysisAudits: MutableList<LocalAnalysisAudit> = mutableListOf()
    private var nutritionTargets: NutritionTargets? = null
    private var cycleContext = CycleContext()
    private var assistantStyle = AssistantStyle.BALANCED
    private var activeConversation: PrivateConversationRecord? = null
    private var conversationExpired = false
    private var displayUnit = "kg"
    private var pendingArchiveRestore: LoadedPayload? = null

    @Synchronized
    fun status(): Status = when {
        sessionKey != null -> unlockedStatus()
        envelopeFile.isFile && dataFile.isFile -> Status.Locked
        envelopeFile.isFile || dataFile.isFile -> Status.RecoveryRequired(
            "This vault is incomplete and cannot be opened safely. Remove its local data to start again."
        )
        else -> Status.Missing
    }

    @Synchronized
    fun createCipherForSetup(): Cipher {
        require(!envelopeFile.exists() && !dataFile.exists()) { "A vault already exists." }
        return cipher(Cipher.ENCRYPT_MODE, keystoreKey(createIfMissing = true))
    }

    @Synchronized
    fun createAfterAuthentication(authenticatedCipher: Cipher): Status {
        val dataKey = ByteArray(32).also(SecureRandom()::nextBytes)
        return try {
            val wrappedKey = authenticatedCipher.doFinal(dataKey)
            val envelope = JSONObject()
                .put("version", 1)
                .put("iv", encode(authenticatedCipher.iv))
                .put("wrappedKey", encode(wrappedKey))
            vaultDirectory.mkdirs()
            writeAtomic(envelopeFile, envelope.toString().toByteArray(Charsets.UTF_8))
            sessionKey = dataKey
            entries = mutableListOf()
            meals = mutableListOf()
            foodCorrections = mutableListOf()
            mealCorrectionMemories = mutableListOf()
            personalDishes = mutableListOf()
            analysisAudits = mutableListOf()
            nutritionTargets = null
            cycleContext = CycleContext()
            activeConversation = null
            conversationExpired = false
            persist()
            unlockedStatus()
        } catch (error: Exception) {
            dataKey.fill(0)
            Status.Failed("Vault setup could not complete. No entries were saved.")
        }
    }

    @Synchronized
    fun createCipherForUnlock(): Cipher {
        val envelope = readEnvelope()
        return cipher(
            Cipher.DECRYPT_MODE,
            keystoreKey(createIfMissing = false),
            decode(envelope.getString("iv")),
        )
    }

    @Synchronized
    fun unlockAfterAuthentication(authenticatedCipher: Cipher): Status {
        return try {
            val envelope = readEnvelope()
            val dataKey = authenticatedCipher.doFinal(decode(envelope.getString("wrappedKey")))
            require(dataKey.size == 32) { "Unexpected vault key length." }
            val loaded = decryptPayload(dataKey)
            lock()
            sessionKey = dataKey
            entries = loaded.entries.toMutableList()
            displayUnit = loaded.displayUnit
            meals = loaded.meals.toMutableList()
            foodCorrections = loaded.foodCorrections.toMutableList()
            mealCorrectionMemories = loaded.mealCorrectionMemories.toMutableList()
            personalDishes = loaded.personalDishes.toMutableList()
            analysisAudits = loaded.analysisAudits.toMutableList()
            nutritionTargets = loaded.nutritionTargets
            cycleContext = loaded.cycleContext
            assistantStyle = loaded.assistantStyle
            activeConversation = PrivateConversationPolicy.active(loaded.activeConversation)
            conversationExpired = loaded.activeConversation != null && activeConversation == null
            if (conversationExpired) persist()
            unlockedStatus()
        } catch (_: KeyPermanentlyInvalidatedException) {
            lock()
            Status.RecoveryRequired(
                "Your phone's biometric security changed, so Android invalidated this vault key. " +
                    "The encrypted vault cannot be recovered without a future encrypted export."
            )
        } catch (_: Exception) {
            lock()
            Status.Failed("Vault unlock failed. Your encrypted data remains unchanged.")
        }
    }

    @Synchronized
    fun addWeight(kilograms: Double, note: String?, recordedAt: Instant = Instant.now()): Status {
        if (!WeightUnits.isValidKilograms(kilograms)) {
            return Status.Failed("Enter a weight between 15 and 500 kg.")
        }
        if (sessionKey == null) return Status.Locked
        val entry = WeightEntry(
            kilograms = kilograms,
            recordedAt = recordedAt,
            note = note?.trim()?.takeIf(String::isNotEmpty),
        )
        return addWeightEntry(entry)
    }

    @Synchronized
    fun addWeightEntry(entry: WeightEntry): Status {
        if (!WeightUnits.isValidKilograms(entry.kilograms)) return Status.Failed("Enter a weight between 15 and 500 kg.")
        if (sessionKey == null) return Status.Locked
        return try {
            entries.add(entry)
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            entries.remove(entry)
            Status.Failed("Weight could not be saved locally. Try again before locking the vault.")
        }
    }

    @Synchronized
    fun updateWeight(id: String, kilograms: Double, note: String?, recordedAt: Instant): Status {
        if (!WeightUnits.isValidKilograms(kilograms)) {
            return Status.Failed("Enter a weight between 15 and 500 kg.")
        }
        if (sessionKey == null) return Status.Locked
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return Status.Failed("That local entry is no longer available.")
        val previous = entries[index]
        entries[index] = previous.copy(
            kilograms = kilograms,
            recordedAt = recordedAt,
            note = note?.trim()?.takeIf(String::isNotEmpty),
        )
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            entries[index] = previous
            Status.Failed("Weight changes could not be saved locally. Try again before locking the vault.")
        }
    }

    @Synchronized
    fun deleteWeight(id: String): Status {
        if (sessionKey == null) return Status.Locked
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return Status.Failed("That local entry is no longer available.")
        val removed = entries.removeAt(index)
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            entries.add(index, removed)
            Status.Failed("Weight deletion could not be saved locally. Try again before locking the vault.")
        }
    }

    @Synchronized
    fun setDisplayUnit(unit: String): Status {
        if (unit !in setOf("kg", "lb")) return Status.Failed("Unsupported weight unit.")
        if (sessionKey == null) return Status.Locked
        val previous = displayUnit
        displayUnit = unit
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            displayUnit = previous
            Status.Failed("Weight unit could not be saved locally.")
        }
    }

    @Synchronized
    fun setCycleContext(context: CycleContext): Status {
        if (sessionKey == null) return Status.Locked
        val normalized = CycleContextValidation.normalized(context)
        if (!CycleContextValidation.valid(normalized)) return Status.Failed("Cycle context could not be saved safely.")
        val previous = cycleContext
        cycleContext = normalized
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            cycleContext = previous
            Status.Failed("Cycle context could not be saved locally.")
        }
    }

    @Synchronized
    fun setAssistantStyle(style: AssistantStyle): Status {
        if (sessionKey == null) return Status.Locked
        val previous = assistantStyle
        assistantStyle = style
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            assistantStyle = previous
            Status.Failed("Assistant style could not be saved locally.")
        }
    }

    @Synchronized
    fun lock() {
        sessionKey?.fill(0)
        sessionKey = null
        entries = mutableListOf()
        meals = mutableListOf()
        foodCorrections = mutableListOf()
        mealCorrectionMemories = mutableListOf()
        personalDishes = mutableListOf()
        analysisAudits = mutableListOf()
        nutritionTargets = null
        cycleContext = CycleContext()
        assistantStyle = AssistantStyle.BALANCED
        activeConversation = null
        conversationExpired = false
        displayUnit = "kg"
        pendingArchiveRestore = null
    }

    /** Export contains member records only, never device keys, model files, temporary images, chat, diagnostics, or audits. */
    @Synchronized
    fun exportRecoveryArchive(passphrase: CharArray): Result<ByteArray> {
        if (sessionKey == null) return Result.failure(IllegalStateException("Unlock your vault before exporting."))
        return runCatching {
            val payload = currentPayloadRoot()
            payload.remove("analysisAudits")
            payload.remove("activeConversation")
            EncryptedArchive.create(payload, passphrase)
        }
    }

    /** Authenticates and fully validates an archive without changing current member data. */
    @Synchronized
    fun inspectRecoveryArchive(archive: ByteArray, passphrase: CharArray): Result<ArchiveSummary> {
        if (sessionKey == null) return Result.failure(IllegalStateException("Unlock or create a vault before restoring."))
        return runCatching {
            val restored = parsePayload(EncryptedArchive.open(archive, passphrase))
            pendingArchiveRestore = restored.copy(activeConversation = null)
            ArchiveSummary(restored.entries.size, restored.meals.size)
        }
    }

    /** Replaces current data only after a successful inspection and an explicit member confirmation. */
    @Synchronized
    fun confirmRecoveryRestore(): Result<ArchiveSummary> {
        if (sessionKey == null) return Result.failure(IllegalStateException("Unlock or create a vault before restoring."))
        return runCatching {
            val restored = requireNotNull(pendingArchiveRestore) { "Review an encrypted archive before restoring." }
            val previous = LoadedPayload(displayUnit, entries, meals, foodCorrections, mealCorrectionMemories, personalDishes, analysisAudits, nutritionTargets, cycleContext, assistantStyle, activeConversation)
            val previousConversationExpired = conversationExpired
            try {
                displayUnit = restored.displayUnit
                entries = restored.entries.toMutableList()
                meals = restored.meals.toMutableList()
                foodCorrections = restored.foodCorrections.toMutableList()
                mealCorrectionMemories = restored.mealCorrectionMemories.toMutableList()
                personalDishes = restored.personalDishes.toMutableList()
                analysisAudits = mutableListOf()
                nutritionTargets = restored.nutritionTargets
                cycleContext = restored.cycleContext
                assistantStyle = restored.assistantStyle
                activeConversation = null
                conversationExpired = false
                persist()
                pendingArchiveRestore = null
                ArchiveSummary(entries.size, meals.size)
            } catch (error: Exception) {
                displayUnit = previous.displayUnit
                entries = previous.entries.toMutableList()
                meals = previous.meals.toMutableList()
                foodCorrections = previous.foodCorrections.toMutableList()
                mealCorrectionMemories = previous.mealCorrectionMemories.toMutableList()
                personalDishes = previous.personalDishes.toMutableList()
                analysisAudits = previous.analysisAudits.toMutableList()
                nutritionTargets = previous.nutritionTargets
                cycleContext = previous.cycleContext
                assistantStyle = previous.assistantStyle
                activeConversation = previous.activeConversation
                conversationExpired = previousConversationExpired
                throw error
            }
        }
    }

    private fun currentPayloadRoot(): JSONObject {
        val key = requireNotNull(sessionKey) { "Vault is locked." }
        val envelope = JSONObject(dataFile.readText(Charsets.UTF_8))
        val decrypted = cipher(Cipher.DECRYPT_MODE, rawKey(key), decode(envelope.getString("iv")))
            .doFinal(decode(envelope.getString("ciphertext")))
        return JSONObject(decrypted.toString(Charsets.UTF_8))
    }

    private fun unlockedStatus() = Status.Unlocked(
        entries = entries.toList(),
        displayUnit = displayUnit,
        meals = meals.toList(),
        foodCorrections = foodCorrections.toList(),
        mealCorrectionMemories = mealCorrectionMemories.toList(),
        personalDishes = personalDishes.toList(),
        analysisAudits = analysisAudits.toList(),
        nutritionTargets = nutritionTargets,
        cycleContext = cycleContext,
        assistantStyle = assistantStyle,
        activeConversation = activeConversation,
        conversationExpired = conversationExpired,
    )

    private fun persist() {
        val key = requireNotNull(sessionKey) { "Vault is locked." }
        val payload = JSONObject().put("version", 1).put("displayUnit", displayUnit).put("assistantStyle", assistantStyle.storageValue).put("entries", JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject()
                    .put("id", entry.id)
                    .put("kilograms", entry.kilograms)
                    .put("recordedAt", entry.recordedAt.toString())
                    .put("note", entry.note))
            }
        }).put("meals", JSONArray().apply {
            meals.forEach { meal ->
                put(JSONObject()
                    .put("id", meal.id)
                    .put("recordedAt", meal.recordedAt.toString())
                    .put("title", meal.title)
                    .put("mealSlot", meal.mealSlot)
                    .put("recordedTimeKnown", meal.recordedTimeKnown)
                    .put("confidence", meal.confidence)
                    .put("source", meal.source)
                    .put("referenceThumbnail", meal.referenceThumbnailBase64)
                    .put("analysisAuditId", meal.analysisAuditId)
                    .put("assumptions", JSONArray(meal.assumptions))
                    .put("items", JSONArray().apply {
                        meal.items.forEach { item -> put(JSONObject()
                            .put("name", item.name).put("grams", item.grams)
                            .put("preparation", item.preparation)
                            .put("catalogFoodId", item.catalogFoodId)
                            .put("catalogRelease", item.catalogRelease)
                            .put("groupLabel", item.groupLabel)
                            .put("description", item.description)
                            .put("role", item.role)) }
                    })
                    .put("nutrition", meal.nutrition?.let { nutrition -> JSONObject()
                        .put("caloriesKcal", nutrition.caloriesKcal).put("proteinGrams", nutrition.proteinGrams)
                        .put("carbohydrateGrams", nutrition.carbohydrateGrams).put("fatGrams", nutrition.fatGrams)
                        .put("source", nutrition.source)
                        .put("catalogFoodId", nutrition.catalogFoodId)
                        .put("catalogRelease", nutrition.catalogRelease)
                        .put("estimateCoverage", nutrition.estimateCoverage)
                    }))
            }
        }).put("foodCorrections", JSONArray().apply {
            foodCorrections.forEach { correction -> put(JSONObject()
                .put("observedName", correction.observedName)
                .put("confirmedFoodId", correction.confirmedFoodId)
                .put("count", correction.count)
                .put("sourceMealId", correction.sourceMealId)) }
        }).put(MealCorrectionMemoryCodec.PAYLOAD_FIELD, MealCorrectionMemoryCodec.encode(mealCorrectionMemories))
        .put("personalDishes", JSONArray().apply {
            personalDishes.forEach { dish ->
                put(JSONObject()
                    .put("id", dish.id)
                    .put("name", dish.name)
                    .put("aliases", JSONArray(dish.aliases))
                    .put("createdAt", dish.createdAt.toString())
                    .put("lastConfirmedAt", dish.lastConfirmedAt.toString())
                    .put("confirmationCount", dish.confirmationCount)
                    .put("referenceThumbnail", dish.referenceThumbnailBase64)
                    .put("items", JSONArray().apply {
                        dish.items.forEach { item ->
                            put(JSONObject()
                                .put("name", item.name).put("grams", item.grams)
                                .put("preparation", item.preparation).put("catalogFoodId", item.catalogFoodId)
                                .put("catalogRelease", item.catalogRelease).put("groupLabel", item.groupLabel)
                                .put("description", item.description).put("role", item.role))
                        }
                    }))
            }
        }).put("analysisAudits", JSONArray().apply {
            analysisAudits.forEach { audit -> put(JSONObject()
                .put("id", audit.id)
                .put("startedAt", audit.startedAt.toString())
                .put("completedAt", audit.completedAt.toString())
                .put("durationMillis", audit.durationMillis)
                .put("modelId", audit.modelId)
                .put("operation", audit.operation)
                .put("outcome", audit.outcome)
                .put("failureCategory", audit.failureCategory)) }
        }).put("nutritionTargets", nutritionTargets?.let { targets -> JSONObject()
            .put("caloriesKcal", targets.caloriesKcal)
            .put("proteinGrams", targets.proteinGrams)
            .put("carbohydrateGrams", targets.carbohydrateGrams)
            .put("fatGrams", targets.fatGrams)
        }).put("cycleContext", JSONObject()
            .put("enabled", cycleContext.enabled)
            .put("periodStarts", JSONArray(cycleContext.periodStarts.map(LocalDate::toString)))
        ).put("activeConversation", activeConversation?.let { conversation -> JSONObject()
            .put("updatedAt", conversation.updatedAt.toString())
            .put("messages", JSONArray().apply {
                conversation.messages.forEach { message -> put(JSONObject()
                    .put("text", message.text)
                    .put("fromMember", message.fromMember)
                    .put("localEvidence", message.localEvidence))
                }
            })
        }).toString().toByteArray(Charsets.UTF_8)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, rawKey(key))
        val envelope = JSONObject()
            .put("version", 1)
            .put("iv", encode(encrypted.iv))
            .put("ciphertext", encode(encrypted.doFinal(payload)))
        writeAtomic(dataFile, envelope.toString().toByteArray(Charsets.UTF_8))
    }

    private data class LoadedPayload(
        val displayUnit: String,
        val entries: List<WeightEntry>,
        val meals: List<MealEntry>,
        val foodCorrections: List<FoodCorrection>,
        val mealCorrectionMemories: List<MealCorrectionMemory>,
        val personalDishes: List<PersonalDish>,
        val analysisAudits: List<LocalAnalysisAudit>,
        val nutritionTargets: NutritionTargets?,
        val cycleContext: CycleContext,
        val assistantStyle: AssistantStyle,
        val activeConversation: PrivateConversationRecord?,
    )

    private fun decryptPayload(key: ByteArray): LoadedPayload {
        val envelope = JSONObject(dataFile.readText(Charsets.UTF_8))
        require(envelope.getInt("version") == 1) { "Unsupported vault version." }
        val decrypted = cipher(Cipher.DECRYPT_MODE, rawKey(key), decode(envelope.getString("iv")))
            .doFinal(decode(envelope.getString("ciphertext")))
        return parsePayload(JSONObject(decrypted.toString(Charsets.UTF_8)))
    }

    /** Parses and fully validates data before an archive can replace this vault. */
    private fun parsePayload(root: JSONObject): LoadedPayload {
        require(root.getInt("version") == 1) { "Unsupported archive data version." }
        val loadedDisplayUnit = root.optString("displayUnit", "kg").takeIf { it == "kg" || it == "lb" } ?: "kg"
        val loadedAssistantStyle = AssistantStyle.fromStorage(root.optString("assistantStyle").takeIf(String::isNotEmpty))
        val items = root.getJSONArray("entries")
        val loadedEntries = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(WeightEntry(
                    id = item.getString("id"),
                    kilograms = item.getDouble("kilograms"),
                    recordedAt = Instant.parse(item.getString("recordedAt")),
                    note = item.optString("note").takeIf(String::isNotEmpty),
                ))
            }
        }
        val loadedMeals = root.optJSONArray("meals")?.let { items -> buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val nutrition = item.optJSONObject("nutrition")?.let { values -> MealNutrition(
                    caloriesKcal = values.optNullableDouble("caloriesKcal"),
                    proteinGrams = values.optNullableDouble("proteinGrams"),
                    carbohydrateGrams = values.optNullableDouble("carbohydrateGrams"),
                    fatGrams = values.optNullableDouble("fatGrams"),
                    source = values.optString("source", "manual_entry"),
                    catalogFoodId = if (values.isNull("catalogFoodId")) null else values.optLong("catalogFoodId"),
                    catalogRelease = values.optString("catalogRelease").takeIf(String::isNotEmpty),
                    estimateCoverage = values.optString("estimateCoverage", "full"),
                )
                }
                val meal = MealEntry(
                    id = item.getString("id"), recordedAt = Instant.parse(item.getString("recordedAt")),
                    title = item.optString("title").takeIf(String::isNotEmpty),
                    mealSlot = item.optString("mealSlot").takeIf { it in MealSlots.allowed },
                    recordedTimeKnown = item.optBoolean("recordedTimeKnown", false),
                    confidence = item.getString("confidence"), source = item.getString("source"),
                    assumptions = item.getJSONArray("assumptions").let { assumptions -> List(assumptions.length()) { assumptions.getString(it) } },
                    items = item.getJSONArray("items").let { mealItems -> List(mealItems.length()) { position ->
                        mealItems.getJSONObject(position).let { entry -> MealEntryItem(
                            name = entry.getString("name"),
                            grams = entry.optNullableInt("grams"),
                            preparation = entry.optString("preparation").takeIf(String::isNotEmpty),
                            catalogFoodId = if (entry.isNull("catalogFoodId")) null else entry.optLong("catalogFoodId"),
                            catalogRelease = entry.optString("catalogRelease").takeIf(String::isNotEmpty),
                            groupLabel = entry.optString("groupLabel").takeIf(String::isNotEmpty),
                            description = entry.optString("description").takeIf(String::isNotEmpty),
                            role = entry.optString("role", "main_food"),
                        ) }
                    } }, nutrition = nutrition,
                    referenceThumbnailBase64 = item.optString("referenceThumbnail").takeIf(String::isNotEmpty),
                    analysisAuditId = item.optString("analysisAuditId").takeIf(String::isNotEmpty),
                )
                require(MealEntryValidation.valid(meal)) { "Invalid meal record." }
                add(meal)
            }
        } } ?: emptyList()
        val loadedCorrections = root.optJSONArray("foodCorrections")?.let { values ->
            require(values.length() <= FoodCorrectionValidation.MAX_STORED) { "Too many food corrections." }
            List(values.length()) { index -> values.getJSONObject(index).let { item -> FoodCorrection(
                observedName = item.getString("observedName"),
                confirmedFoodId = item.getLong("confirmedFoodId"),
                count = item.optInt("count", 1),
                sourceMealId = if (!item.has("sourceMealId") || item.isNull("sourceMealId")) null else {
                    item.opt("sourceMealId") as? String
                        ?: throw IllegalArgumentException("Invalid food correction source meal ID.")
                },
            ) }
            }
        } ?: emptyList()
        val savedMealIds = loadedMeals.map(MealEntry::id).toSet()
        require(loadedCorrections.all { FoodCorrectionValidation.valid(it, savedMealIds) }) { "Invalid food correction." }
        val normalizedCorrections = FoodCorrectionValidation.normalized(loadedCorrections)
        val loadedCorrectionMemories = MealCorrectionMemoryCodec.decode(root, loadedMeals)
        val loadedPersonalDishes = root.optJSONArray("personalDishes")?.let { dishes ->
            buildList {
                for (index in 0 until dishes.length()) {
                    val dish = dishes.getJSONObject(index)
                    val dishItems = dish.getJSONArray("items")
                    val saved = PersonalDish(
                        id = dish.getString("id"),
                        name = dish.getString("name"),
                        aliases = dish.optJSONArray("aliases")?.let { aliases ->
                            List(aliases.length()) { aliases.getString(it) }
                        } ?: emptyList(),
                        createdAt = Instant.parse(dish.getString("createdAt")),
                        lastConfirmedAt = Instant.parse(dish.getString("lastConfirmedAt")),
                        confirmationCount = dish.optInt("confirmationCount", 1),
                        referenceThumbnailBase64 = dish.optString("referenceThumbnail").takeIf(String::isNotEmpty),
                        items = List(dishItems.length()) { position ->
                            val item = dishItems.getJSONObject(position)
                            MealEntryItem(
                                name = item.getString("name"),
                                grams = item.optNullableInt("grams"),
                                preparation = item.optString("preparation").takeIf(String::isNotEmpty),
                                catalogFoodId = if (item.isNull("catalogFoodId")) null else item.optLong("catalogFoodId"),
                                catalogRelease = item.optString("catalogRelease").takeIf(String::isNotEmpty),
                                groupLabel = item.optString("groupLabel").takeIf(String::isNotEmpty),
                                description = item.optString("description").takeIf(String::isNotEmpty),
                                role = item.optString("role", "main_food"),
                            )
                        },
                    )
                    require(PersonalDishValidation.valid(saved)) { "Invalid personal dish." }
                    add(saved)
                }
            }
        } ?: emptyList()
        val loadedAudits = root.optJSONArray("analysisAudits")?.let { values -> List(values.length()) { index ->
            values.getJSONObject(index).let { item -> LocalAnalysisAudit(
                id = item.getString("id"),
                startedAt = Instant.parse(item.getString("startedAt")),
                completedAt = Instant.parse(item.getString("completedAt")),
                durationMillis = item.getLong("durationMillis"),
                modelId = item.getString("modelId"),
                operation = item.getString("operation"),
                outcome = item.getString("outcome"),
                failureCategory = item.optString("failureCategory").takeIf(String::isNotEmpty),
            ).also { require(LocalAnalysisAuditValidation.valid(it)) { "Invalid analysis audit." } }
        } } } ?: emptyList()
        val loadedTargets = root.optJSONObject("nutritionTargets")?.let { values -> NutritionTargets(
            caloriesKcal = values.optNullableDouble("caloriesKcal"),
            proteinGrams = values.optNullableDouble("proteinGrams"),
            carbohydrateGrams = values.optNullableDouble("carbohydrateGrams"),
            fatGrams = values.optNullableDouble("fatGrams"),
        ).also { require(NutritionTargetValidation.valid(it)) { "Invalid nutrition targets." } }
        }
        val loadedCycleContext = root.optJSONObject("cycleContext")?.let { values ->
            CycleContext(
                enabled = values.optBoolean("enabled", false),
                periodStarts = values.optJSONArray("periodStarts")?.let { starts ->
                    List(starts.length()) { index -> LocalDate.parse(starts.getString(index)) }
                } ?: emptyList(),
            ).let(CycleContextValidation::normalized)
        } ?: CycleContext()
        require(CycleContextValidation.valid(loadedCycleContext)) { "Invalid cycle context." }
        val loadedConversation = root.optJSONObject("activeConversation")?.let { conversation ->
            val messageValues = conversation.getJSONArray("messages")
            val record = PrivateConversationRecord(
                messages = List(messageValues.length()) { index -> messageValues.getJSONObject(index).let { message ->
                    PrivateConversationMessage(
                        text = message.getString("text"),
                        fromMember = message.getBoolean("fromMember"),
                        localEvidence = message.optBoolean("localEvidence", false),
                    )
                } },
                updatedAt = Instant.parse(conversation.getString("updatedAt")),
            )
            require(record.messages.size <= PrivateConversationPolicy.maxMessages) { "Conversation history is too large." }
            require(record.messages.all { it.text.isNotBlank() && it.text.length <= PrivateConversationPolicy.maxMessageCharacters }) {
                "Conversation history is invalid."
            }
            record
        }
        return LoadedPayload(loadedDisplayUnit, loadedEntries, loadedMeals, normalizedCorrections, loadedCorrectionMemories, loadedPersonalDishes, loadedAudits, loadedTargets, loadedCycleContext, loadedAssistantStyle, loadedConversation)
    }

    @Synchronized
    fun saveActiveConversation(
        messages: List<PrivateConversationMessage>,
        updatedAt: Instant = Instant.now(),
    ): Status {
        if (sessionKey == null) return Status.Locked
        val previous = activeConversation
        val previousExpired = conversationExpired
        activeConversation = PrivateConversationPolicy.updated(messages, updatedAt)
        conversationExpired = false
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            activeConversation = previous
            conversationExpired = previousExpired
            Status.Failed("The private conversation could not be saved.")
        }
    }

    @Synchronized
    fun clearActiveConversation(): Status {
        if (sessionKey == null) return Status.Locked
        val previous = activeConversation
        val previousExpired = conversationExpired
        activeConversation = null
        conversationExpired = false
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            activeConversation = previous
            conversationExpired = previousExpired
            Status.Failed("The private conversation could not be cleared.")
        }
    }

    @Synchronized
    fun acknowledgeConversationExpiry(): Status {
        if (sessionKey == null) return Status.Locked
        conversationExpired = false
        return unlockedStatus()
    }

    @Synchronized
    fun refreshConversationRetention(now: Instant = Instant.now()): Status {
        if (sessionKey == null) return Status.Locked
        val previous = activeConversation ?: return unlockedStatus()
        val current = PrivateConversationPolicy.active(previous, now)
        if (current != null) return unlockedStatus()
        activeConversation = null
        conversationExpired = true
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            activeConversation = previous
            conversationExpired = false
            Status.Failed("The private conversation retention could not be updated.")
        }
    }

    @Synchronized
    fun recordFoodCorrections(corrections: List<FoodCorrection>): Status {
        if (sessionKey == null) return Status.Locked
        val mealIds = meals.map(MealEntry::id).toSet()
        if (corrections.any { !FoodCorrectionValidation.valid(it, mealIds) }) {
            return Status.Failed("A private food correction was not valid.")
        }
        val previous = foodCorrections.toList()
        applyFoodCorrections(corrections)
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            foodCorrections = previous.toMutableList()
            Status.Failed("The private food memory could not be saved.")
        }
    }

    /** Clears reusable food hints and per-meal correction memory, leaving meals and saved dishes intact. */
    @Synchronized
    fun clearLearning(): Status {
        if (sessionKey == null) return Status.Locked
        val previousCorrections = foodCorrections.toList()
        val previousMemories = mealCorrectionMemories.toList()
        foodCorrections.clear()
        mealCorrectionMemories.clear()
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            foodCorrections = previousCorrections.toMutableList()
            mealCorrectionMemories = previousMemories.toMutableList()
            Status.Failed("Private learning could not be cleared locally.")
        }
    }

    /** Stores a member-confirmed personal dish for bounded local assistant retrieval. */
    @Synchronized
    fun savePersonalDish(dish: PersonalDish): Status {
        if (sessionKey == null) return Status.Locked
        if (!PersonalDishValidation.valid(dish)) return Status.Failed("That personal dish was not valid.")
        val previous = personalDishes.toList()
        if (!mergePersonalDish(dish)) return Status.Failed("Your private dish memory is full. Remove a saved dish before adding another.")
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            personalDishes = previous.toMutableList()
            Status.Failed("The personal dish could not be saved.")
        }
    }

    /**
     * One durable member action: either the meal and all accepted learning are
     * encrypted together, or the in-memory state is restored unchanged.
     */
    @Synchronized
    fun saveReviewedMeal(
        entry: MealEntry,
        corrections: List<FoodCorrection>,
        dishes: List<PersonalDish>,
        correctionMemory: MealCorrectionMemory? = null,
    ): Status {
        if (sessionKey == null) return Status.Locked
        if (!MealEntryValidation.valid(entry)) return Status.Failed("This reviewed meal contains invalid values.")
        if (corrections.any {
                it.sourceMealId != null || !FoodCorrectionValidation.valid(it, emptySet())
            } || (corrections.isNotEmpty() && !FoodCorrectionValidation.hasValidSourceId(entry.id))) {
            return Status.Failed("A reviewed food match was not valid.")
        }
        if (dishes.any { !PersonalDishValidation.valid(it) }) return Status.Failed("A remembered dish was not valid.")
        val normalizedMemory = correctionMemory?.let(MealCorrectionMemoryPolicy::normalized)
        if (correctionMemory != null && (normalizedMemory == null || entry.source !in setOf("ai_estimate", "member_confirmed_catalog") || correctionMemory.mealId != entry.id || correctionMemory.recordedAt != entry.recordedAt)) {
            return Status.Failed("The meal correction memory did not match this reviewed meal.")
        }

        val previousMeals = meals.toList()
        val previousCorrections = foodCorrections.toList()
        val previousMemories = mealCorrectionMemories.toList()
        val previousDishes = personalDishes.toList()
        return try {
            meals.add(entry)
            applyFoodCorrections(corrections.map { it.copy(sourceMealId = entry.id) })
            normalizedMemory?.let { memory ->
                mealCorrectionMemories = MealCorrectionMemoryPolicy.withRecord(mealCorrectionMemories, memory).toMutableList()
            }
            require(dishes.all(::mergePersonalDish)) { "Private dish memory is full." }
            persist()
            unlockedStatus()
        } catch (error: Exception) {
            meals = previousMeals.toMutableList()
            foodCorrections = previousCorrections.toMutableList()
            mealCorrectionMemories = previousMemories.toMutableList()
            personalDishes = previousDishes.toMutableList()
            Status.Failed(
                if (error.message == "Private dish memory is full.") {
                    "Your private dish memory is full. Remove a saved dish before adding another."
                } else {
                    "The meal and its private memory could not be saved. Review it and try again."
                },
            )
        }
    }

    @Synchronized
    fun deletePersonalDish(id: String): Status {
        if (sessionKey == null) return Status.Locked
        val index = personalDishes.indexOfFirst { it.id == id }
        if (index < 0) return Status.Failed("That saved dish is no longer available.")
        val removed = personalDishes.removeAt(index)
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            personalDishes.add(index, removed)
            Status.Failed("The saved dish could not be removed.")
        }
    }

    private fun applyFoodCorrections(corrections: List<FoodCorrection>) {
        corrections.forEach { correction ->
            val identity = FoodCorrectionValidation.identityKey(correction)
            val index = foodCorrections.indexOfFirst { FoodCorrectionValidation.identityKey(it) == identity }
            if (index >= 0) {
                foodCorrections[index] = foodCorrections[index].copy(
                    count = (foodCorrections[index].count + correction.count).coerceAtMost(10_000),
                )
            } else if (foodCorrections.size < FoodCorrectionValidation.MAX_STORED) {
                foodCorrections.add(correction.copy(observedName = correction.observedName.trim()))
            }
        }
    }

    private fun mergePersonalDish(dish: PersonalDish): Boolean {
        val incomingNames = (dish.aliases + dish.name).map(PersonalDishMatcher::identityKey).filter(String::isNotEmpty).toSet()
        val existing = personalDishes.indexOfFirst { candidate ->
            candidate.id == dish.id ||
                (candidate.aliases + candidate.name).map(PersonalDishMatcher::identityKey).any(incomingNames::contains)
        }
        if (existing >= 0) {
            val old = personalDishes[existing]
            val aliases = (old.aliases + dish.aliases + old.name)
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.equals(dish.name, ignoreCase = true) }
                .distinctBy(PersonalDishMatcher::identityKey)
                .take(8)
            personalDishes[existing] = dish.copy(
                id = old.id,
                aliases = aliases,
                createdAt = old.createdAt,
                confirmationCount = (old.confirmationCount + 1).coerceAtMost(10_000),
            )
            return true
        }
        if (personalDishes.size >= 200) return false
        personalDishes.add(dish)
        return true
    }

    /** Stores bounded, non-content audit metadata only while the private vault is unlocked. */
    @Synchronized
    fun recordAnalysisAudit(audit: LocalAnalysisAudit): Status {
        if (sessionKey == null) return Status.Locked
        if (!LocalAnalysisAuditValidation.valid(audit)) return Status.Failed("The local analysis audit was invalid.")
        val previous = analysisAudits.toList()
        analysisAudits.add(audit)
        analysisAudits.sortBy(LocalAnalysisAudit::completedAt)
        while (analysisAudits.size > 50) analysisAudits.removeAt(0)
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            analysisAudits = previous.toMutableList()
            Status.Failed("The local analysis audit could not be saved.")
        }
    }

    @Synchronized
    fun setNutritionTargets(targets: NutritionTargets?): Status {
        if (sessionKey == null) return Status.Locked
        val normalized = targets?.takeUnless(NutritionTargetValidation::empty)
        if (normalized != null && !NutritionTargetValidation.valid(normalized)) {
            return Status.Failed("Enter positive daily targets within the supported range.")
        }
        val previous = nutritionTargets
        nutritionTargets = normalized
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            nutritionTargets = previous
            Status.Failed("Daily nutrition targets could not be saved locally.")
        }
    }

    @Synchronized
    fun addMeal(entry: MealEntry): Status {
        if (sessionKey == null) return Status.Locked
        if (!MealEntryValidation.valid(entry)) return Status.Failed("This meal draft contains invalid values.")
        meals.add(entry)
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            meals.remove(entry)
            Status.Failed("Meal could not be saved locally. Try again before locking the vault.")
        }
    }

    @Synchronized
    fun updateMeal(entry: MealEntry): Status {
        if (sessionKey == null) return Status.Locked
        if (!MealEntryValidation.valid(entry)) return Status.Failed("This meal contains invalid values.")
        val index = meals.indexOfFirst { it.id == entry.id }
        if (index < 0) return Status.Failed("This meal record is no longer available.")
        val previous = meals[index]
        val previousCorrections = foodCorrections.toList()
        val previousMemories = mealCorrectionMemories.toList()
        val removingOnlyReferencePhoto = MealLearningLifecyclePolicy.removesOnlyReferencePhoto(previous, entry)
        meals[index] = entry
        if (!removingOnlyReferencePhoto) {
            // Legacy un-attributed hints remain recoverable, but FoodCatalog excludes them from ranking.
            foodCorrections = MealLearningLifecyclePolicy.afterMealMutation(
                foodCorrections,
                entry.id,
                preserveLearning = false,
            ).toMutableList()
            mealCorrectionMemories.removeAll { it.mealId == entry.id }
        }
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            meals[index] = previous
            foodCorrections = previousCorrections.toMutableList()
            mealCorrectionMemories = previousMemories.toMutableList()
            Status.Failed("Meal changes could not be saved locally. Try again before locking the vault.")
        }
    }

    @Synchronized
    fun deleteMeal(id: String): Status {
        if (sessionKey == null) return Status.Locked
        val index = meals.indexOfFirst { it.id == id }
        if (index < 0) return Status.Failed("This meal record is no longer available.")
        val removed = meals.removeAt(index)
        val previousCorrections = foodCorrections.toList()
        val previousMemories = mealCorrectionMemories.toList()
        // Preserve legacy un-attributed hints in the archive; they cannot be safely linked to this meal.
        foodCorrections = MealLearningLifecyclePolicy.afterMealMutation(
            foodCorrections,
            id,
            preserveLearning = false,
        ).toMutableList()
        mealCorrectionMemories.removeAll { it.mealId == id }
        return try {
            persist()
            unlockedStatus()
        } catch (_: Exception) {
            meals.add(index, removed)
            foodCorrections = previousCorrections.toMutableList()
            mealCorrectionMemories = previousMemories.toMutableList()
            Status.Failed("Meal deletion could not be saved locally. Try again before locking the vault.")
        }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (isNull(name)) null else optDouble(name).takeIf { !it.isNaN() }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name)) null else optInt(name).takeIf { it != 0 || opt(name) == 0 }

    private fun readEnvelope(): JSONObject {
        require(envelopeFile.isFile && dataFile.isFile) { "No complete vault is available." }
        return JSONObject(envelopeFile.readText(Charsets.UTF_8)).also {
            require(it.getInt("version") == 1) { "Unsupported vault version." }
        }
    }

    private fun keystoreKey(createIfMissing: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        require(createIfMissing) { "Vault key is unavailable." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            val builder = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
            init(builder.build())
        }.generateKey()
    }

    private fun cipher(mode: Int, key: SecretKey, iv: ByteArray? = null): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            if (iv == null) init(mode, key) else init(mode, key, GCMParameterSpec(128, iv))
        }

    private fun rawKey(bytes: ByteArray) = javax.crypto.spec.SecretKeySpec(bytes, "AES")

    private fun writeAtomic(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.new")
        temporary.writeBytes(bytes)
        require(temporary.renameTo(target)) { "Could not persist encrypted vault." }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val keyAlias = "metria.vault.wrap.v1"
    }
}
