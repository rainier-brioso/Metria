package app.metria

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Member-authorized cloud route through Metria's HTTPS broker. */
class CloudPhotoAccess(context: Context, private val random: SecureRandom = SecureRandom()) {
    data class Configuration(val endpoint: String, val credential: String, val installationId: String)
    data class Result(val proposal: String, val promptTokens: Int, val completionTokens: Int, val latencyMs: Long)

    private val file = File(context.noBackupFilesDir, "cloud-photo-access.bin")
    private val preferences = context.getSharedPreferences("metria-cloud-photo-mode", Context.MODE_PRIVATE)
    private val lock = Any()
    @Volatile private var activeConnection: HttpURLConnection? = null
    private val cancellationGeneration = AtomicLong(0)

    fun isConfigured(): Boolean = synchronized(lock) { runCatching { read() }.getOrNull() != null }

    fun selectedMode(): String = synchronized(lock) { runCatching { readMode() }.getOrDefault("local") }

    fun setSelectedMode(mode: String) {
        require(mode == "local" || mode == "cloud")
        synchronized(lock) { modePreferences().edit().putString(MODE_KEY, mode).apply() }
    }

    fun endpoint(): String? = synchronized(lock) { runCatching { read()?.endpoint }.getOrNull() }

    /** Redeems a private judge pass once. The pass is never written to disk or logs. */
    fun configure(endpoint: String, pass: CharArray): String? {
        val normalized = validateEndpoint(endpoint)
        if (normalized == null) {
            pass.fill('\u0000')
            return "Check the secure broker address."
        }
        if (pass.isEmpty() || pass.size > 256 || pass.any { it.isISOControl() }) {
            pass.fill('\u0000')
            return "Enter the private access pass."
        }
        val installation = installationId()
        val requestGeneration = cancellationGeneration.get()
        val body = JSONObject().put("pass_code", String(pass)).put("installation_id", installation).toString().toByteArray()
        pass.fill('\u0000')
        return try {
            val response = request(normalized + "/v1/judge/activate", null, body, requestGeneration)
            val json = JSONObject(response)
            val token = json.optString("credential")
            if (json.optBoolean("fictional_only", true) || token.length !in 40..160 || token.any { it.isWhitespace() }) {
                "This access pass cannot enable the cloud assistant. Check the pass or broker settings."
            } else {
                synchronized(lock) {
                    check(cancellationGeneration.get() == requestGeneration) { "request_cancelled" }
                    write(Configuration(normalized, token, installation))
                }
                null
            }
        } catch (_: Exception) {
            "Could not connect this pass. Check the address, internet connection, and pass, then try again."
        }
    }

    fun clear() {
        cancel()
        activeConnection?.disconnect()
        activeConnection = null
        synchronized(lock) { file.delete() }
    }

    /** Sends only the already-prepared, resized, EXIF-free photo and its optional text. */
    fun analyze(image: ByteArray, description: String, requestId: String, startedAt: Long): Result {
        require(image.isNotEmpty() && image.size <= MAX_PHOTO_BYTES) { "photo_limit" }
        require(description.length <= MealPhotoContext.MAX_LENGTH && description.none { it.isISOControl() && it !in "\n\r\t" }) { "description_invalid" }
        UUID.fromString(requestId)
        val requestGeneration = cancellationGeneration.get()
        val config = synchronized(lock) { read() } ?: throw IllegalStateException("cloud_not_configured")
        val body = JSONObject().put("request_id", requestId)
            .put("image_base64", Base64.encodeToString(image, Base64.NO_WRAP))
            .put("description", description.trim()).put("consent_version", 1).toString().toByteArray()
        val response = JSONObject(request(config.endpoint + "/v1/vision", config.credential, body, requestGeneration))
        require(response.optString("request_id") == requestId &&
            response.optString("model") == "nvidia/Nemotron-3.5-Super-VL" &&
            response.optString("provider") == "nebius") { "response_identity_invalid" }
        val usage = response.optJSONObject("usage") ?: throw IllegalArgumentException("response_usage_invalid")
        val prompt = usage.optInt("prompt_tokens", -1)
        val completion = usage.optInt("completion_tokens", -1)
        val total = usage.optInt("total_tokens", -1)
        require(prompt in 0..100_000 && completion in 0..100_000 && total == prompt + completion) { "response_usage_invalid" }
        val proposal = response.optString("proposal")
        require(proposal.isNotBlank() && proposal.length <= MAX_PROPOSAL_CHARS) { "response_proposal_invalid" }
        return Result(proposal, prompt, completion, (System.nanoTime() - startedAt).coerceAtLeast(0) / 1_000_000)
    }

    fun cancel() {
        cancellationGeneration.incrementAndGet()
        activeConnection?.disconnect()
        activeConnection = null
    }

    /** Shared authenticated broker transport for the photo and future assistant routes. */
    internal fun postAuthenticated(path: String, body: ByteArray): String {
        require(path in AUTHENTICATED_PATHS && body.size in 1..MAX_REQUEST_BYTES) { "request_invalid" }
        val requestGeneration = cancellationGeneration.get()
        val config = synchronized(lock) { read() } ?: throw IllegalStateException("cloud_not_configured")
        return request(config.endpoint + path, config.credential, body, requestGeneration)
    }

    private fun request(url: String, bearer: String?, body: ByteArray, requestGeneration: Long? = null): String {
        val connection = URL(url).openConnection() as? HttpURLConnection ?: throw IllegalArgumentException("transport")
        require(connection is javax.net.ssl.HttpsURLConnection && connection.url.host.isNotBlank()) { "https_required" }
        synchronized(lock) {
            if (requestGeneration != null && cancellationGeneration.get() != requestGeneration) throw IllegalStateException("request_cancelled")
            check(activeConnection == null) { "request_in_progress" }
            activeConnection = connection
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
            if (bearer != null) connection.setRequestProperty("X-Metria-Sent-At", Instant.now().epochSecond.toString())
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            if (requestGeneration != null && cancellationGeneration.get() != requestGeneration) throw IllegalStateException("request_cancelled")
            val status = connection.responseCode
            if (requestGeneration != null && cancellationGeneration.get() != requestGeneration) throw IllegalStateException("request_cancelled")
            require(status == 200) { "provider_or_broker_rejected" }
            val input = connection.inputStream
            val response = input.use { it.readBytesBounded(MAX_RESPONSE_BYTES) }
            require(response.isNotEmpty()) { "empty_response" }
            return response.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
            synchronized(lock) { if (activeConnection === connection) activeConnection = null }
        }
    }

    private fun readMode(): String = preferences.getString(MODE_KEY, "local")?.takeIf { it in setOf("local", "cloud") } ?: "local"

    private fun installationId(): String = synchronized(lock) {
        read()?.installationId?.let { return@synchronized it }
        preferences.getString(INSTALLATION_KEY, null)?.let { saved ->
            if (runCatching { UUID.fromString(saved) }.isSuccess) return@synchronized saved
        }
        UUID.randomUUID().toString().also { id ->
            check(modePreferences().edit().putString(INSTALLATION_KEY, id).commit()) { "installation_id_save_failed" }
        }
    }

    private fun modePreferences() = preferences

    private fun read(): Configuration? {
        if (!file.isFile || file.length() !in 1..4096) return null
        val bytes = file.readBytes()
        val ivSize = bytes.firstOrNull()?.toInt() ?: return null
        if (ivSize != 12 || bytes.size <= 13) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        val json = JSONObject(cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8))
        val endpoint = validateEndpoint(json.optString("endpoint")) ?: return null
        val token = json.optString("credential")
        val installation = json.optString("installation_id")
        if (token.length !in 40..160 || token.any { it.isWhitespace() } || runCatching { UUID.fromString(installation) }.isFailure) return null
        return Configuration(endpoint, token, installation)
    }

    private fun write(value: Configuration) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true), random)
        val encrypted = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(
            JSONObject().put("endpoint", value.endpoint).put("credential", value.credential)
                .put("installation_id", value.installationId).toString().toByteArray(Charsets.UTF_8),
        )
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(encrypted)
        if (!temp.renameTo(file)) {
            temp.delete()
            throw IllegalStateException("config_write_failed")
        }
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "keystore_key_missing" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    companion object {
        const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
        const val MAX_REQUEST_BYTES = 3 * 1024 * 1024
        const val MAX_RESPONSE_BYTES = 65_536
        const val MAX_PROPOSAL_CHARS = 16_384
        private const val TIMEOUT_MS = 40_000
        private const val KEY_ALIAS = "metria.cloud-photo-access.v1"
        private const val MODE_KEY = "photo_analysis_mode"
        private const val INSTALLATION_KEY = "cloud_installation_id"
        private val AUTHENTICATED_PATHS = setOf("/v1/vision", "/v1/assistant", "/v1/transcribe")

        fun validateEndpoint(input: String): String? = runCatching {
            val url = URL(input.trim().trimEnd('/'))
            require(url.protocol == "https" && url.userInfo == null && url.query == null && url.ref == null)
            require(url.host.isNotBlank() && url.port in -1..65535)
            require(url.path.isEmpty() || url.path == "/")
            val host = url.host.lowercase().removePrefix("[").removeSuffix("]")
            require(host != "localhost" && host != "127.0.0.1" && host != "::1")
            "https://${if (host.contains(':')) "[$host]" else host}${if (url.port == -1 || url.port == 443) "" else ":${url.port}"}"
        }.getOrNull()

        private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = read(buffer)
                if (count < 0) break
                require(output.size() + count <= limit) { "response_too_large" }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }
}
