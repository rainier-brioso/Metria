package app.metria.vision

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Gate B model lifecycle only. Inference is intentionally not exposed yet. */
class MetriaVisionModule : Module() {
  companion object {
    private const val MODEL_VERSION = "gemma-4-E2B-it@9262660a1676eed6d0c477ab1a86344430854664"
    private const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/9262660a1676eed6d0c477ab1a86344430854664/gemma-4-E2B-it.litertlm?download=true"
    private const val MODEL_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    private const val MODEL_BYTES = 2_588_147_712L
    private const val MODEL_FILE = "gemma-4-E2B-it.litertlm"
  }

  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private var download: Future<*>? = null
  private var state = "absent"
  private var downloadedBytes = 0L

  override fun definition() = ModuleDefinition {
    Name("MetriaVision")

    OnDestroy {
      download?.cancel(true)
      executor.shutdownNow()
    }

    Function("runtimeStatus") { status() }

    AsyncFunction("downloadModel") { promise: Promise ->
      if (download?.isDone == false) {
        promise.resolve(status())
        return@AsyncFunction
      }
      state = "downloading"
      downloadedBytes = modelFile().length()
      promise.resolve(status())
      download = executor.submit {
        try {
          downloadModelFile()
          state = "ready"
          downloadedBytes = MODEL_BYTES
        } catch (cancelled: InterruptedException) {
          state = "absent"
          modelFile().delete()
          downloadedBytes = 0L
        } catch (error: Exception) {
          state = "failed"
          downloadedBytes = modelFile().length()
        }
      }
    }

    AsyncFunction("cancelDownload") {
      download?.cancel(true)
      download = null
      if (state == "downloading") {
        state = "absent"
        modelFile().delete()
        downloadedBytes = 0L
      }
      status()
    }

    AsyncFunction("loadModel") {
      if (!modelFile().isFile) {
        state = "absent"
        return@AsyncFunction status()
      }
      if (!hasExpectedChecksum(modelFile())) {
        state = "failed"
        return@AsyncFunction status("Stored model checksum does not match the pinned artifact.")
      }
      state = "ready"
      downloadedBytes = MODEL_BYTES
      status("Model is verified and available; runtime initialization is deferred.")
    }

    AsyncFunction("unloadModel") {
      // There is no runtime engine to close in this lifecycle-only checkpoint.
      status()
    }

    AsyncFunction("removeModel") {
      download?.cancel(true)
      download = null
      modelFile().delete()
      state = "removed"
      downloadedBytes = 0L
      status()
    }
  }

  private fun modelFile(): File = File(appContext.reactContext.filesDir, MODEL_FILE)

  private fun downloadModelFile() {
    val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    connection.requestMethod = "GET"
    connection.connect()
    if (connection.responseCode !in 200..299) {
      throw IllegalStateException("Model download returned HTTP ${connection.responseCode}")
    }
    val partial = File(modelFile().parentFile, "$MODEL_FILE.part")
    partial.delete()
    connection.inputStream.use { input ->
      FileOutputStream(partial).use { output ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
          val count = input.read(buffer)
          if (count < 0) break
          output.write(buffer, 0, count)
          downloadedBytes += count
          if (Thread.currentThread().isInterrupted) throw InterruptedException()
        }
      }
    }
    if (!hasExpectedChecksum(partial) || partial.length() != MODEL_BYTES) {
      partial.delete()
      throw IllegalStateException("Downloaded model failed size or checksum verification")
    }
    if (!partial.renameTo(modelFile())) {
      throw IllegalStateException("Could not install verified model")
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
    return digest.digest().joinToString("") { "%02x".format(it) } == MODEL_SHA256
  }

  private fun status(reason: String? = null): Map<String, Any?> = mapOf(
    // Re-discover a verified artifact after process death/restart. Full hashing
    // remains in loadModel; status never treats a partial file as ready.
    "state" to refreshedState(),
    "runtimeVersion" to null,
    "modelVersion" to MODEL_VERSION,
    "downloadedBytes" to downloadedBytes,
    "expectedBytes" to MODEL_BYTES,
    "reason" to (reason ?: "Model lifecycle is available; image inference is disabled in this checkpoint.")
  )

  private fun refreshedState(): String {
    if ((state == "absent" || state == "removed") && modelFile().isFile) {
      downloadedBytes = modelFile().length()
      state = if (downloadedBytes == MODEL_BYTES) "ready" else "failed"
    }
    return state
  }
}
