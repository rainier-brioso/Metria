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
        data class Unlocked(val entries: List<WeightEntry>) : Status
        data class RecoveryRequired(val message: String) : Status
        data class Failed(val message: String) : Status
    }

    private val vaultDirectory = File(context.filesDir, "vault")
    private val envelopeFile = File(vaultDirectory, "key-envelope.json")
    private val dataFile = File(vaultDirectory, "weights.json.enc")
    private var sessionKey: ByteArray? = null
    private var entries: MutableList<WeightEntry> = mutableListOf()

    fun status(): Status = when {
        sessionKey != null -> Status.Unlocked(entries.toList())
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
            persist()
            Status.Unlocked(entries.toList())
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
            val loadedEntries = decryptEntries(dataKey)
            lock()
            sessionKey = dataKey
            entries = loadedEntries.toMutableList()
            Status.Unlocked(entries.toList())
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

    fun addWeight(kilograms: Double, note: String?): Status {
        if (!WeightUnits.isValidKilograms(kilograms)) {
            return Status.Failed("Enter a weight between 15 and 500 kg.")
        }
        if (sessionKey == null) return Status.Locked
        val entry = WeightEntry(kilograms = kilograms, note = note?.trim()?.takeIf(String::isNotEmpty))
        return try {
            entries.add(entry)
            persist()
            Status.Unlocked(entries.toList())
        } catch (_: Exception) {
            entries.remove(entry)
            Status.Failed("Weight could not be saved locally. Try again before locking the vault.")
        }
    }

    fun lock() {
        sessionKey?.fill(0)
        sessionKey = null
        entries = mutableListOf()
    }

    private fun persist() {
        val key = requireNotNull(sessionKey) { "Vault is locked." }
        val payload = JSONObject().put("version", 1).put("entries", JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject()
                    .put("id", entry.id)
                    .put("kilograms", entry.kilograms)
                    .put("recordedAt", entry.recordedAt.toString())
                    .put("note", entry.note))
            }
        }).toString().toByteArray(Charsets.UTF_8)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, rawKey(key))
        val envelope = JSONObject()
            .put("version", 1)
            .put("iv", encode(encrypted.iv))
            .put("ciphertext", encode(encrypted.doFinal(payload)))
        writeAtomic(dataFile, envelope.toString().toByteArray(Charsets.UTF_8))
    }

    private fun decryptEntries(key: ByteArray): List<WeightEntry> {
        val envelope = JSONObject(dataFile.readText(Charsets.UTF_8))
        require(envelope.getInt("version") == 1) { "Unsupported vault version." }
        val decrypted = cipher(Cipher.DECRYPT_MODE, rawKey(key), decode(envelope.getString("iv")))
            .doFinal(decode(envelope.getString("ciphertext")))
        val items = JSONObject(decrypted.toString(Charsets.UTF_8)).getJSONArray("entries")
        return buildList {
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
    }

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
