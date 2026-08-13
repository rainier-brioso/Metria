package app.metria

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * M6 recovery decision: archives use a member-created passphrase, not an
 * Android Keystore key. They can therefore be restored on another phone, but
 * Metria has no way to recover a forgotten passphrase.
 */
object EncryptedArchive {
    private const val format = "metria-recovery"
    private const val version = 1
    private const val iterations = 600_000
    private val associatedData = "metria-recovery-v1".toByteArray(Charsets.UTF_8)

    fun create(payload: JSONObject, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 12) { "Use at least 12 characters for your archive passphrase." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val key = deriveKey(passphrase, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
                updateAAD(associatedData)
            }
            return JSONObject()
                .put("format", format)
                .put("version", version)
                .put("kdf", "PBKDF2WithHmacSHA256")
                .put("iterations", iterations)
                .put("salt", encode(salt))
                .put("iv", encode(iv))
                .put("ciphertext", encode(cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))))
                .toString().toByteArray(Charsets.UTF_8)
        } finally {
            key.fill(0)
            salt.fill(0)
        }
    }

    fun open(archive: ByteArray, passphrase: CharArray): JSONObject {
        require(passphrase.size >= 12) { "Enter the archive passphrase." }
        val envelope = JSONObject(archive.toString(Charsets.UTF_8))
        require(envelope.optString("format") == format && envelope.optInt("version") == version) { "This is not a supported Metria archive." }
        require(envelope.optString("kdf") == "PBKDF2WithHmacSHA256" && envelope.optInt("iterations") == iterations) { "This archive uses unsupported protection settings." }
        val salt = decode(envelope.getString("salt"))
        val key = deriveKey(passphrase, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, decode(envelope.getString("iv"))))
                updateAAD(associatedData)
            }
            return JSONObject(cipher.doFinal(decode(envelope.getString("ciphertext"))).toString(Charsets.UTF_8))
        } finally {
            key.fill(0)
            salt.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String) = Base64.getDecoder().decode(text)
}
