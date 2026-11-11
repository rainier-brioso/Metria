package app.metria.compatible

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import app.metria.CloudPhotoAccess
import app.metria.GeneratedAssistantContract
import app.metria.cloud.CloudAssistantRequest
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.net.HttpURLConnection
import java.net.URL

/** Explicit member-selected off-phone inference. No retries, redirects, content logs or model downloads. */
class CompatibleProviderAccess(context: Context) {
    private data class Configuration(val settings: CompatibleProviderSettings, val key: String)
    private val file = AtomicFile(File(context.noBackupFilesDir, "compatible-provider.bin"))
    private val monitor = Any()
    private val generation = AtomicLong()
    @Volatile private var active: HttpURLConnection? = null

    fun settings(): CompatibleProviderSettings? = synchronized(monitor) { read()?.settings }

    fun configure(settings: CompatibleProviderSettings, apiKey: CharArray): String? = try {
        val safe = settings.validated()
        require(apiKey.size <= 4096 && apiKey.none { it.isISOControl() || it.isWhitespace() }) { "Check the API key." }
        cancel()
        synchronized(monitor) {
            val old = read()
            // Never forward a saved secret to a newly entered address.
            val secret = if (apiKey.isEmpty() && old?.settings?.baseUrl == safe.baseUrl) old.key else String(apiKey)
            write(Configuration(safe, secret))
        }
        null
    } catch (_: Exception) {
        "Could not save this connection. Check the secure address, model name and API key."
    } finally { apiKey.fill('\u0000') }

    fun clear() { cancel(); synchronized(monitor) { file.delete() } }

    fun cancel() {
        synchronized(monitor) {
            generation.incrementAndGet()
            active?.disconnect()
            active = null
        }
    }

    fun discover(baseUrl: String, kind: String, apiKey: CharArray): List<CompatibleModel> = try {
        val base = CompatibleProviderSettings.normalizeBaseUrl(baseUrl.trim())
        require(apiKey.size <= 4096 && apiKey.none { it.isWhitespace() || it.isISOControl() })
        val secret = synchronized(monitor) {
            if (apiKey.isEmpty()) read()?.takeIf { it.settings.baseUrl == base }?.key.orEmpty() else String(apiKey)
        }
        val ticket = generation.get()
        if (kind == "ollama") {
            val root = base.removeSuffix("/v1")
            val installed = CompatibleModelCatalog.ollamaModels(exchange("$root/api/tags", secret, null, ticket))
            installed.mapIndexed { index, model ->
                check(generation.get() == ticket) { "cancelled" }
                if (index >= 24) model else {
                    val capability = runCatching {
                        val body = JSONObject().put("model", model.id).toString().toByteArray(Charsets.UTF_8)
                        CompatibleModelCatalog.ollamaVision(exchange("$root/api/show", secret, body, ticket))
                    }.getOrNull()
                    check(generation.get() == ticket) { "cancelled" }
                    model.copy(vision = capability)
                }
            }.sortedWith(compareBy<CompatibleModel> { it.vision != true }.thenBy { it.id })
        } else {
        val raw = exchange("$base/models", secret, null, ticket)
        val models = JSONObject(raw).getJSONArray("data")
        require(models.length() <= 2000)
        (0 until models.length()).map { models.getJSONObject(it).getString("id") }
            .filter { it.length in 1..160 && it.none { character -> character.isWhitespace() || character.isISOControl() } }
            .distinct().sorted().map { id -> CompatibleModel(id, CompatibleAssistantProtocol.imageSupport(raw, id)) }
        }
    } finally { apiKey.fill('\u0000') }

    fun assistant(turn: CloudAssistantRequest.Turn): CloudAssistantRequest.Result? {
        var stage = CompatibleProviderFailure.Category.REQUEST
        try {
            val ticket = generation.get()
            val config = synchronized(monitor) { read() } ?: error("connection_missing")
            val request = requireNotNull(CompatibleAssistantProtocol.request(turn, config.settings.model,
                GeneratedAssistantContract.SOURCE_JSON, config.settings.disableReasoning)) { "request_invalid" }
            stage = CompatibleProviderFailure.Category.CONNECTION
            val raw = exchange("${config.settings.baseUrl}/chat/completions", config.key, request, ticket)
            stage = CompatibleProviderFailure.Category.RESPONSE
            val metadata = JSONObject(raw)
            if (metadata.optString("model") != config.settings.model) {
                throw CompatibleProviderFailure(CompatibleProviderFailure.Category.MODEL)
            }
            if (metadata.optJSONArray("choices")?.optJSONObject(0)?.optString("finish_reason") == "length") {
                throw CompatibleProviderFailure(CompatibleProviderFailure.Category.TRUNCATED)
            }
            val canonical = requireNotNull(CompatibleAssistantProtocol.response(raw, turn, config.settings.model)) { "response_invalid" }
            check(generation.get() == ticket) { "cancelled" }
            stage = CompatibleProviderFailure.Category.HOST_RESPONSE
            return requireNotNull(CloudAssistantRequest.parse(canonical, turn, "compatible", config.settings.model))
        } catch (failure: Exception) {
            val safe = if (failure is CompatibleProviderFailure) failure else CompatibleProviderFailure(
                if (failure is java.net.SocketTimeoutException) CompatibleProviderFailure.Category.TIMEOUT else stage)
            throw safe
        }
    }

    fun analyze(image: ByteArray, description: String, requestId: String, startedAt: Long): CloudPhotoAccess.Result {
        val ticket = generation.get()
        val config = synchronized(monitor) { read() } ?: error("connection_missing")
        if (!config.settings.vision) throw CompatibleProviderFailure(CompatibleProviderFailure.Category.VISION_DISABLED)
        // Model listing is optional across compatible servers. Known text-only
        // metadata blocks photo upload; an unavailable listing leaves support unknown.
        val imageSupport = runCatching {
            if (config.settings.kind == "ollama") {
                val body = JSONObject().put("model", config.settings.model).toString().toByteArray(Charsets.UTF_8)
                val metadata = exchange("${config.settings.baseUrl.removeSuffix("/v1")}/api/show", config.key, body, ticket)
                CompatibleModelCatalog.ollamaVision(metadata)
            } else {
                val metadata = exchange("${config.settings.baseUrl}/models", config.key, null, ticket)
                CompatibleAssistantProtocol.imageSupport(metadata, config.settings.model)
            }
        }.getOrNull()
        check(generation.get() == ticket) { "cancelled" }
        if (imageSupport == false) throw CompatibleProviderFailure(CompatibleProviderFailure.Category.VISION_UNSUPPORTED)
        val request = requireNotNull(CompatibleAssistantProtocol.visionRequest(image, description, config.settings.model,
            GeneratedAssistantContract.SOURCE_JSON, config.settings.disableReasoning)) { "request_invalid" }
        val raw = exchange("${config.settings.baseUrl}/chat/completions", config.key, request, ticket)
        val canonical = JSONObject(requireNotNull(CompatibleAssistantProtocol.visionResponse(raw, requestId, config.settings.model)) { "response_invalid" })
        check(generation.get() == ticket) { "cancelled" }
        val usage = canonical.optJSONObject("usage")
        return CloudPhotoAccess.Result(canonical.getString("proposal"), usage?.getInt("prompt_tokens") ?: 0,
            usage?.getInt("completion_tokens") ?: 0, (System.nanoTime() - startedAt).coerceAtLeast(0) / 1_000_000,
            usageKnown = usage != null)
    }

    private fun exchange(url: String, secret: String, body: ByteArray?, ticket: Long): String {
        require(body == null || body.size <= CloudPhotoAccess.MAX_REQUEST_BYTES)
        val connection = URL(url).openConnection() as HttpURLConnection
        val deadline = System.nanoTime() + 90_000_000_000L
        synchronized(monitor) {
            check(generation.get() == ticket && active == null) { "cancelled_or_busy" }
            active = connection
        }
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = false
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.setRequestProperty("Accept", "application/json")
            if (secret.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer $secret")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            check(generation.get() == ticket) { "cancelled" }
            if (connection.responseCode != 200) throw CompatibleProviderFailure(CompatibleProviderFailure.Category.HTTP)
            val bytes = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    check(generation.get() == ticket) { "cancelled" }
                    val remainingMillis = (deadline - System.nanoTime()) / 1_000_000
                    check(remainingMillis > 0) { "request_timeout" }
                    connection.readTimeout = remainingMillis.coerceAtMost(60_000).toInt().coerceAtLeast(1)
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(bytes.size() + count <= 256 * 1024) { "response_limit" }
                    bytes.write(buffer, 0, count)
                }
            }
            check(generation.get() == ticket) { "cancelled" }
            return bytes.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
            synchronized(monitor) { if (active === connection) active = null }
        }
    }

    private fun read(): Configuration? = runCatching {
        val data = file.openRead().use { input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= 16_384)
                bytes.write(buffer, 0, count)
            }
            require(bytes.size() >= 14)
            bytes.toByteArray()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        val json = JSONObject(cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8))
        val settings = CompatibleProviderSettings(json.getString("kind"), json.getString("base_url"),
            json.getString("model"), json.getBoolean("vision"), json.getBoolean("disable_reasoning")).validated()
        val secret = json.getString("api_key")
        require(secret.length <= 4096 && secret.none { it.isISOControl() || it.isWhitespace() })
        Configuration(settings, secret)
    }.getOrNull()

    private fun write(config: Configuration) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true))
        val settings = config.settings
        val json = JSONObject().put("kind", settings.kind).put("base_url", settings.baseUrl)
            .put("model", settings.model).put("vision", settings.vision).put("disable_reasoning", settings.disableReasoning)
            .put("api_key", config.key).toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(cipher.iv + cipher.doFinal(json)); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    companion object { private const val KEY_ALIAS = "metria.compatible-provider.v1" }
}
