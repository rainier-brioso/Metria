package app.metria.vision

import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Gate B model lifecycle only. Image inference is intentionally not exposed yet. */
class MetriaVisionModule : Module() {
  companion object {
    private const val MODEL_VERSION = "gemma-4-E2B-it@9262660a1676eed6d0c477ab1a86344430854664"
    private const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/9262660a1676eed6d0c477ab1a86344430854664/gemma-4-E2B-it.litertlm?download=true"
    private const val MODEL_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    private const val MODEL_BYTES = 2_588_147_712L
    private const val MODEL_FILE = "gemma-4-E2B-it.litertlm"
  }

  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val lifecycleLock = Any()
  private var download: Future<*>? = null
  private var verification: Future<*>? = null
  private var downloadGeneration = 0L
  private var state = "absent"
  private var downloadedBytes = 0L
  private var lastReason: String? = null

  override fun definition() = ModuleDefinition {
    Name("MetriaVision")

    OnDestroy {
      synchronized(lifecycleLock) {
        downloadGeneration += 1
        download?.cancel(true)
        verification?.cancel(true)
        partialFile().delete()
      }
      executor.shutdownNow()
    }

    Function("runtimeStatus") { status() }

    AsyncFunction("downloadModel") { promise: Promise ->
      synchronized(lifecycleLock) {
        if (download?.isDone == false) {
          promise.resolve(statusLocked())
          return@AsyncFunction
        }
        val generation = ++downloadGeneration
        partialFile().delete()
        state = "downloading"
        downloadedBytes = 0L
        lastReason = null
        download = executor.submit { downloadModel(generation) }
        promise.resolve(statusLocked())
      }
    }

    AsyncFunction("cancelDownload") {
      synchronized(lifecycleLock) {
        downloadGeneration += 1
        download?.cancel(true)
        download = null
        partialFile().delete()
        if (state == "downloading") {
          state = "absent"
          downloadedBytes = modelFile().takeIf { it.isFile }?.length() ?: 0L
          lastReason = "Model download cancelled."
        }
        statusLocked()
      }
    }

    AsyncFunction("loadModel") {
      synchronized(lifecycleLock) {
        val file = modelFile()
        if (!file.isFile) {
          state = "absent"
          downloadedBytes = 0L
          lastReason = "The local model is not installed."
        } else {
          downloadedBytes = file.length()
          beginVerificationLocked()
        }
        statusLocked()
      }
    }

    AsyncFunction("unloadModel") {
      // A LiteRT-LM engine is not connected in this lifecycle-only checkpoint.
      status()
    }

    AsyncFunction("removeModel") {
      synchronized(lifecycleLock) {
        downloadGeneration += 1
        download?.cancel(true)
        verification?.cancel(true)
        download = null
        verification = null
        partialFile().delete()
        modelFile().delete()
        state = "removed"
        downloadedBytes = 0L
        lastReason = "Model removed from this device."
        statusLocked()
      }
    }
  }

  private fun modelFile(): File {
    val filesDir = appContext.reactContext?.filesDir
      ?: throw IllegalStateException("Metria Vision is not attached to an Android application context.")
    return File(filesDir, MODEL_FILE)
  }

  private fun partialFile(): File = File(modelFile().parentFile, "$MODEL_FILE.part")

  private fun downloadModel(generation: Long) {
    try {
      val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
      connection.connectTimeout = 15_000
      connection.readTimeout = 30_000
      connection.requestMethod = "GET"
      try {
        connection.connect()
        if (connection.responseCode !in 200..299) {
          throw IllegalStateException("Model download returned HTTP ${connection.responseCode}")
        }
        FileOutputStream(partialFile()).use { output ->
          connection.inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
              if (Thread.currentThread().isInterrupted) throw InterruptedException()
              val count = input.read(buffer)
              if (count < 0) break
              output.write(buffer, 0, count)
              synchronized(lifecycleLock) {
                if (generation != downloadGeneration) throw InterruptedException()
                downloadedBytes += count
              }
            }
          }
        }
      } finally {
        connection.disconnect()
      }

      val partial = partialFile()
      if (partial.length() != MODEL_BYTES || !hasExpectedChecksum(partial)) {
        throw IllegalStateException("Downloaded model failed size or checksum verification")
      }
      synchronized(lifecycleLock) {
        if (generation != downloadGeneration || Thread.currentThread().isInterrupted) {
          throw InterruptedException()
        }
        if (!partial.renameTo(modelFile())) {
          throw IllegalStateException("Could not install verified model")
        }
        state = "ready"
        downloadedBytes = MODEL_BYTES
        lastReason = "Model is verified and available; runtime initialization is deferred."
      }
    } catch (cancelled: InterruptedException) {
      synchronized(lifecycleLock) {
        if (generation == downloadGeneration) {
          partialFile().delete()
          state = "absent"
          downloadedBytes = modelFile().takeIf { it.isFile }?.length() ?: 0L
          lastReason = "Model download cancelled."
        }
      }
    } catch (error: Exception) {
      synchronized(lifecycleLock) {
        if (generation == downloadGeneration) {
          partialFile().delete()
          state = "failed"
          downloadedBytes = modelFile().takeIf { it.isFile }?.length() ?: 0L
          lastReason = "Model download failed verification or could not complete."
        }
      }
    }
  }

  private fun beginVerificationLocked() {
    if (verification?.isDone == false || state == "downloading") return
    state = "verifying"
    lastReason = "Verifying the stored model before it can be used."
    verification = executor.submit {
      val file = modelFile()
      val valid = file.isFile && file.length() == MODEL_BYTES && hasExpectedChecksum(file)
      synchronized(lifecycleLock) {
        if (modelFile() != file || state == "removed") return@synchronized
        if (valid) {
          state = "ready"
          downloadedBytes = MODEL_BYTES
          lastReason = "Model is verified and available; runtime initialization is deferred."
        } else {
          file.delete()
          state = "failed"
          downloadedBytes = 0L
          lastReason = "Stored model failed integrity verification and was removed."
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
    return digest.digest().joinToString("") { "%02x".format(it) } == MODEL_SHA256
  }

  private fun status(): Map<String, Any?> = synchronized(lifecycleLock) {
    if ((state == "absent" || state == "removed") && modelFile().isFile) {
      downloadedBytes = modelFile().length()
      beginVerificationLocked()
    }
    statusLocked()
  }

  private fun statusLocked(): Map<String, Any?> = mapOf(
    "state" to state,
    "runtimeVersion" to null,
    "modelVersion" to MODEL_VERSION,
    "downloadedBytes" to downloadedBytes,
    "expectedBytes" to MODEL_BYTES,
    "reason" to (lastReason ?: "Model lifecycle is available; image inference is disabled in this checkpoint.")
  )
}
