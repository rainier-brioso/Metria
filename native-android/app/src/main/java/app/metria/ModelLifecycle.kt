package app.metria

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Owns only the local model artifact lifecycle. LiteRT engine initialization and
 * image inference are deliberately added in a later checkpoint.
 */
data class ModelStatus(
    val state: String,
    val downloadedBytes: Long,
    val expectedBytes: Long,
    val reason: String,
    val testOutput: String?,
    val backendSummary: String,
)

class ModelLifecycle(private val privateFilesDir: File) : AutoCloseable {
    companion object {
        const val expectedBytes = 2_588_147_712L
        private const val modelFileName = "gemma-4-E2B-it.litertlm"
        private const val modelUrl =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                "9262660a1676eed6d0c477ab1a86344430854664/" +
                "gemma-4-E2B-it.litertlm?download=true"
        private const val modelSha256 =
            "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var activeDownload: Future<*>? = null
    private var activeVerification: Future<*>? = null
    private var activeRuntimeLoad: Future<*>? = null
    private var activeInference: Future<*>? = null
    private var engine: Engine? = null
    private var activeConversation: Conversation? = null
    private var generation = 0L
    private var runtimeGeneration = 0L
    private var state = "absent"
    private var bytes = 0L
    private var reason = "No local model is installed."
    private var testOutput: String? = null

    @Synchronized
    fun status(): ModelStatus {
        if ((state == "absent" || state == "removed") && modelFile().isFile) {
            bytes = modelFile().length()
            beginVerificationLocked()
        }
        return statusLocked()
    }

    @Synchronized
    fun download(): ModelStatus {
        if (activeDownload?.isDone == false) return statusLocked()
        val currentGeneration = ++generation
        partialFile().delete()
        state = "downloading"
        bytes = 0L
        reason = "Downloading the selected local model."
        activeDownload = executor.submit { downloadModel(currentGeneration) }
        return statusLocked()
    }

    @Synchronized
    fun cancelDownload(): ModelStatus {
        generation += 1
        activeDownload?.cancel(true)
        activeDownload = null
        partialFile().delete()
        if (state == "downloading") {
            state = "absent"
            bytes = modelFile().takeIf(File::isFile)?.length() ?: 0L
            reason = "Model download cancelled."
        }
        return statusLocked()
    }

    @Synchronized
    fun verifyInstalledModel(): ModelStatus {
        val file = modelFile()
        if (!file.isFile) {
            state = "absent"
            bytes = 0L
            reason = "No local model is installed."
        } else {
            bytes = file.length()
            beginVerificationLocked()
        }
        return statusLocked()
    }

    @Synchronized
    fun initializeRuntime(): ModelStatus {
        if (activeRuntimeLoad?.isDone == false || engine?.isInitialized() == true) {
            return statusLocked()
        }
        if (state != "ready" || !modelFile().isFile) {
            reason = "Verify the local model before initializing LiteRT."
            return statusLocked()
        }
        val currentRuntimeGeneration = ++runtimeGeneration
        state = "initializing"
        reason = "Initializing LiteRT locally. This can take a moment."
        activeRuntimeLoad = executor.submit { initializeRuntime(currentRuntimeGeneration) }
        return statusLocked()
    }

    @Synchronized
    fun unloadRuntime(): ModelStatus {
        runtimeGeneration += 1
        activeRuntimeLoad?.cancel(true)
        activeRuntimeLoad = null
        activeInference?.cancel(true)
        activeInference = null
        activeConversation?.cancelProcess()
        activeConversation?.close()
        activeConversation = null
        engine?.close()
        engine = null
        if (modelFile().isFile) {
            state = "ready"
            reason = "Model verified; LiteRT runtime unloaded."
        }
        return statusLocked()
    }

    @Synchronized
    fun runTextSmokeTest(): ModelStatus {
        val loadedEngine = engine
        if (state != "loaded" || loadedEngine == null || !loadedEngine.isInitialized()) {
            reason = "Initialize LiteRT before running the local smoke test."
            return statusLocked()
        }
        if (activeInference?.isDone == false) return statusLocked()
        state = "testing"
        testOutput = null
        reason = "Running a fixed offline text smoke test."
        activeInference = executor.submit { runTextSmokeTest(loadedEngine) }
        return statusLocked()
    }

    @Synchronized
    fun remove(): ModelStatus {
        unloadRuntime()
        generation += 1
        activeDownload?.cancel(true)
        activeVerification?.cancel(true)
        activeDownload = null
        activeVerification = null
        partialFile().delete()
        modelFile().delete()
        state = "removed"
        bytes = 0L
        reason = "Model removed from this device."
        return statusLocked()
    }

    @Synchronized
    override fun close() {
        unloadRuntime()
        generation += 1
        activeDownload?.cancel(true)
        activeVerification?.cancel(true)
        partialFile().delete()
        executor.shutdownNow()
    }

    private fun modelFile() = File(privateFilesDir, modelFileName)

    private fun partialFile() = File(privateFilesDir, "$modelFileName.part")

    private fun downloadModel(downloadGeneration: Long) {
        try {
            val connection = URL(modelUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            try {
                connection.connect()
                require(connection.responseCode in 200..299) {
                    "Model download returned HTTP ${connection.responseCode}"
                }
                FileOutputStream(partialFile()).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (Thread.currentThread().isInterrupted) throw InterruptedException()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            synchronized(this) {
                                if (downloadGeneration != generation) throw InterruptedException()
                                bytes += count
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            val partial = partialFile()
            require(partial.length() == expectedBytes && hasExpectedChecksum(partial)) {
                "Downloaded model failed size or checksum verification."
            }
            synchronized(this) {
                if (downloadGeneration != generation || Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
                require(partial.renameTo(modelFile())) { "Could not install verified model." }
                state = "ready"
                bytes = expectedBytes
                reason = "Model verified. LiteRT engine initialization is the next checkpoint."
            }
        } catch (_: InterruptedException) {
            synchronized(this) {
                if (downloadGeneration == generation) {
                    partialFile().delete()
                    state = "absent"
                    bytes = modelFile().takeIf(File::isFile)?.length() ?: 0L
                    reason = "Model download cancelled."
                }
            }
        } catch (_: Exception) {
            synchronized(this) {
                if (downloadGeneration == generation) {
                    partialFile().delete()
                    state = "failed"
                    bytes = modelFile().takeIf(File::isFile)?.length() ?: 0L
                    reason = "Model download failed verification or could not complete."
                }
            }
        }
    }

    private fun initializeRuntime(currentRuntimeGeneration: Long) {
        var candidate: Engine? = null
        try {
            val config = EngineConfig(
                modelFile().absolutePath,
                Backend.CPU(),
                Backend.CPU(),
                Backend.CPU(),
                4096,
                1,
                privateFilesDir.absolutePath,
            )
            candidate = Engine(config)
            synchronized(this) {
                if (currentRuntimeGeneration != runtimeGeneration) {
                    candidate.close()
                    return
                }
                engine = candidate
            }
            candidate.initialize()
            synchronized(this) {
                if (currentRuntimeGeneration != runtimeGeneration) {
                    candidate.close()
                    if (engine === candidate) engine = null
                    return
                }
                state = "loaded"
                reason = "LiteRT is initialized locally. Image inference is not enabled yet."
            }
        } catch (_: Exception) {
            synchronized(this) {
                candidate?.close()
                if (engine === candidate) engine = null
                if (currentRuntimeGeneration == runtimeGeneration && modelFile().isFile) {
                    state = "ready"
                    reason = "LiteRT initialization failed; the verified model remains available."
                }
            }
        }
    }

    private fun runTextSmokeTest(loadedEngine: Engine) {
        var conversation: Conversation? = null
        try {
            conversation = loadedEngine.createConversation()
            synchronized(this) { activeConversation = conversation }
            val reply = conversation.sendMessage("Reply with exactly: METRIA_READY")
            val output = reply.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
                .trim()
            synchronized(this) {
                if (engine !== loadedEngine) return
                state = "loaded"
                testOutput = output.take(500)
                reason = "Offline text smoke test completed."
            }
        } catch (_: Exception) {
            synchronized(this) {
                if (engine === loadedEngine) {
                    state = "loaded"
                    reason = "Offline text smoke test failed; LiteRT remains loaded."
                }
            }
        } finally {
            conversation?.close()
            synchronized(this) {
                if (activeConversation === conversation) activeConversation = null
            }
        }
    }

    @Synchronized
    private fun beginVerificationLocked() {
        if (activeVerification?.isDone == false || state == "downloading") return
        state = "verifying"
        reason = "Verifying the stored model before it can be used."
        activeVerification = executor.submit {
            val file = modelFile()
            val valid = file.isFile && file.length() == expectedBytes && hasExpectedChecksum(file)
            synchronized(this) {
                if (state == "removed") return@synchronized
                if (valid) {
                    state = "ready"
                    bytes = expectedBytes
                    reason = "Model verified. LiteRT engine initialization is the next checkpoint."
                } else {
                    file.delete()
                    state = "failed"
                    bytes = 0L
                    reason = "Stored model failed integrity verification and was removed."
                }
            }
        }
    }

    private fun hasExpectedChecksum(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == modelSha256
    }

    private fun statusLocked() = ModelStatus(
        state,
        bytes,
        expectedBytes,
        reason,
        testOutput,
        "Text: CPU | Vision: CPU | Audio: CPU",
    )
}
