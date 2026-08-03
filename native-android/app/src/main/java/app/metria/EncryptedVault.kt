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
    sealed interface Status {
        data object Missing : Status
        data object Locked : Status
        data class Unlocked(
            val entries: List<WeightEntry>,
            val displayUnit: String = "kg",
            val meals: List<MealEntry> = emptyList(),
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
    private var displayUnit = "kg"

    fun status(): Status = when {
        sessionKey != null -> Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        envelopeFile.isFile && dataFile.isFile -> Status.Locked
        envelopeFile.isFile || dataFile.isFile -> Status.RecoveryRequired(
            "This vault is incomplete and cannot be opened safely. Remove its local data to start again."
        )
        else -> Status.Missing
    }

    fun createCipherForSetup(): Cipher {
        require(!envelopeFile.exists() && !dataFile.exists()) { "A vault already exists." }
        return cipher(Cipher.ENCRYPT_MODE, keystoreKey(createIfMissing = true))
    }

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
            persist()
            Status.Unlocked(entries.toList(), meals = meals.toList())
        } catch (error: Exception) {
            dataKey.fill(0)
            Status.Failed("Vault setup could not complete. No entries were saved.")
        }
    }

    fun createCipherForUnlock(): Cipher {
        val envelope = readEnvelope()
        return cipher(
            Cipher.DECRYPT_MODE,
            keystoreKey(createIfMissing = false),
            decode(envelope.getString("iv")),
        )
    }

    fun unlockAfterAuthentication(authenticatedCipher: Cipher): Status {
        return try {
            val envelope = readEnvelope()
            val dataKey = authenticatedCipher.doFinal(decode(envelope.getString("wrappedKey")))
            require(dataKey.size == 32) { "Unexpected vault key length." }
            val loaded = decryptPayload(dataKey)
            lock()
            sessionKey = dataKey
            entries = loaded.entries.toMutableList()
            meals = loaded.meals.toMutableList()
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
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
        return try {
            entries.add(entry)
            persist()
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        } catch (_: Exception) {
            entries.remove(entry)
            Status.Failed("Weight could not be saved locally. Try again before locking the vault.")
        }
    }

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
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        } catch (_: Exception) {
            entries[index] = previous
            Status.Failed("Weight changes could not be saved locally. Try again before locking the vault.")
        }
    }

    fun deleteWeight(id: String): Status {
        if (sessionKey == null) return Status.Locked
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return Status.Failed("That local entry is no longer available.")
        val removed = entries.removeAt(index)
        return try {
            persist()
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        } catch (_: Exception) {
            entries.add(index, removed)
            Status.Failed("Weight deletion could not be saved locally. Try again before locking the vault.")
        }
    }

    fun setDisplayUnit(unit: String): Status {
        if (unit !in setOf("kg", "lb")) return Status.Failed("Unsupported weight unit.")
        if (sessionKey == null) return Status.Locked
        val previous = displayUnit
        displayUnit = unit
        return try {
            persist()
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        } catch (_: Exception) {
            displayUnit = previous
            Status.Failed("Weight unit could not be saved locally.")
        }
    }

    fun lock() {
        sessionKey?.fill(0)
        sessionKey = null
        entries = mutableListOf()
        meals = mutableListOf()
        displayUnit = "kg"
    }

    private fun persist() {
        val key = requireNotNull(sessionKey) { "Vault is locked." }
        val payload = JSONObject().put("version", 1).put("displayUnit", displayUnit).put("entries", JSONArray().apply {
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
                    .put("confidence", meal.confidence)
                    .put("source", meal.source)
                    .put("assumptions", JSONArray(meal.assumptions))
                    .put("items", JSONArray().apply {
                        meal.items.forEach { item -> put(JSONObject()
                            .put("name", item.name).put("grams", item.grams)
                            .put("preparation", item.preparation)) }
                    })
                    .put("nutrition", meal.nutrition?.let { nutrition -> JSONObject()
                        .put("caloriesKcal", nutrition.caloriesKcal).put("proteinGrams", nutrition.proteinGrams)
                        .put("carbohydrateGrams", nutrition.carbohydrateGrams).put("fatGrams", nutrition.fatGrams)
                        .put("source", nutrition.source)
                    }))
            }
        }).toString().toByteArray(Charsets.UTF_8)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, rawKey(key))
        val envelope = JSONObject()
            .put("version", 1)
            .put("iv", encode(encrypted.iv))
            .put("ciphertext", encode(encrypted.doFinal(payload)))
        writeAtomic(dataFile, envelope.toString().toByteArray(Charsets.UTF_8))
    }

    private data class LoadedPayload(val entries: List<WeightEntry>, val meals: List<MealEntry>)

    private fun decryptPayload(key: ByteArray): LoadedPayload {
        val envelope = JSONObject(dataFile.readText(Charsets.UTF_8))
        require(envelope.getInt("version") == 1) { "Unsupported vault version." }
        val decrypted = cipher(Cipher.DECRYPT_MODE, rawKey(key), decode(envelope.getString("iv")))
            .doFinal(decode(envelope.getString("ciphertext")))
        val root = JSONObject(decrypted.toString(Charsets.UTF_8))
        displayUnit = root.optString("displayUnit", "kg").takeIf { it == "kg" || it == "lb" } ?: "kg"
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
                )
                }
                val meal = MealEntry(
                    id = item.getString("id"), recordedAt = Instant.parse(item.getString("recordedAt")),
                    confidence = item.getString("confidence"), source = item.getString("source"),
                    assumptions = item.getJSONArray("assumptions").let { assumptions -> List(assumptions.length()) { assumptions.getString(it) } },
                    items = item.getJSONArray("items").let { mealItems -> List(mealItems.length()) { position ->
                        mealItems.getJSONObject(position).let { entry -> MealEntryItem(
                            name = entry.getString("name"),
                            grams = entry.optNullableInt("grams"),
                            preparation = entry.optString("preparation").takeIf(String::isNotEmpty),
                        ) }
                    } }, nutrition = nutrition,
                )
                require(MealEntryValidation.valid(meal)) { "Invalid meal record." }
                add(meal)
            }
        } } ?: emptyList()
        return LoadedPayload(loadedEntries, loadedMeals)
    }

    fun addMeal(entry: MealEntry): Status {
        if (sessionKey == null) return Status.Locked
        if (!MealEntryValidation.valid(entry)) return Status.Failed("This meal draft contains invalid values.")
        meals.add(entry)
        return try {
            persist()
            Status.Unlocked(entries.toList(), displayUnit, meals.toList())
        } catch (_: Exception) {
            meals.remove(entry)
            Status.Failed("Meal could not be saved locally. Try again before locking the vault.")
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
