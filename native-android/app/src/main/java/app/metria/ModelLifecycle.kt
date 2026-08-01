package app.metria

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Contents
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
    val imageState: String,
    val imageReason: String,
    val proposal: MealProposal?,
    val proposalDiagnostic: String?,
)

class ModelLifecycle(
    private val privateFilesDir: File,
    private val cacheDirectory: File,
) : AutoCloseable {
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
    private var preparedImage: File? = null
    private var generation = 0L
    private var runtimeGeneration = 0L
    private var state = "absent"
    private var bytes = 0L
    private var reason = "No local model is installed."
    private var testOutput: String? = null
    private var imageState = "none"
    private var imageReason = "No image selected."
    private var proposal: MealProposal? = null
    private var proposalDiagnostic: String? = null

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
    fun prepareImage(contentResolver: ContentResolver, sourceUri: Uri): ModelStatus {
        if (activeInference?.isDone == false) {
            imageReason = "Wait for the active local operation to finish."
            return statusLocked()
        }
        imageState = "preparing"
        imageReason = "Preparing a temporary local image copy."
        executor.submit { prepareImageInBackground(contentResolver, sourceUri) }
        return statusLocked()
    }

    @Synchronized
    fun analyzePreparedImage(): ModelStatus {
        val loadedEngine = engine
        val image = preparedImage
        if (state != "loaded" || loadedEngine == null || !loadedEngine.isInitialized()) {
            imageReason = "Initialize LiteRT before analyzing an image."
            return statusLocked()
        }
        if (imageState != "ready" || image == null || !image.isFile) {
            imageReason = "Select and prepare an image before analysis."
            return statusLocked()
        }
        imageState = "analyzing"
        imageReason = "Analyzing the temporary image locally."
        testOutput = null
        proposal = null
        proposalDiagnostic = null
        activeInference = executor.submit { analyzePreparedImage(loadedEngine, image) }
        return statusLocked()
    }

    @Synchronized
    fun discardPreparedImage(): ModelStatus {
        preparedImage?.delete()
        preparedImage = null
        proposal = null
        proposalDiagnostic = null
        imageState = "none"
        imageReason = "Temporary image discarded."
        return statusLocked()
    }

    @Synchronized
    fun remove(): ModelStatus {
        unloadRuntime()
        discardPreparedImage()
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
        discardPreparedImage()
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

    private fun prepareImageInBackground(contentResolver: ContentResolver, sourceUri: Uri) {
        var prepared: File? = null
        try {
            val orientation = contentResolver.openInputStream(sourceUri).use { input ->
                requireNotNull(input) { "Selected image could not be opened." }
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(sourceUri).use { input ->
                requireNotNull(input) { "Selected image could not be decoded." }
                BitmapFactory.decodeStream(input, null, bounds)
            }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not a supported image." }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 1536)
            }
            val decoded = contentResolver.openInputStream(sourceUri).use { input ->
                requireNotNull(input) { "Selected image could not be decoded." }
                requireNotNull(BitmapFactory.decodeStream(input, null, options)) {
                    "Selected file is not a supported image."
                }
            }
            val normalized = applyOrientation(decoded, orientation)
            if (normalized !== decoded) decoded.recycle()
            prepared = File.createTempFile("metria-image-", ".jpg", cacheDirectory)
            prepared.outputStream().use { output ->
                require(normalized.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                    "Temporary image conversion failed."
                }
            }
            normalized.recycle()
            synchronized(this) {
                preparedImage?.delete()
                preparedImage = prepared
                imageState = "ready"
                imageReason = "Image prepared locally with metadata removed."
            }
        } catch (_: Exception) {
            prepared?.delete()
            synchronized(this) {
                imageState = "failed"
                imageReason = "Image preparation failed. Choose a different image or enter details manually."
            }
        }
    }

    private fun analyzePreparedImage(loadedEngine: Engine, image: File) {
        var conversation: Conversation? = null
        try {
            conversation = loadedEngine.createConversation()
            synchronized(this) { activeConversation = conversation }
            val response = conversation.sendMessage(
                Contents.of(
                    Content.ImageFile(image.absolutePath),
                    Content.Text(
                        "Return only one JSON object with exactly these keys: kind, items, assumptions, " +
                            "questions, overall_confidence. kind must be meal_proposal. Each item must have " +
                            "exactly name, visible_portion, estimated_grams_range, preparation, confidence. " +
                            "estimated_grams_range must have integer min and max. Confidence values must be " +
                            "low, medium, or high. Describe visible food or readable nutrition-label facts only. " +
                            "Use empty items if you cannot identify food. This is an editable draft, not advice."
                    ),
                )
            )
            val output = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
                .trim()
            synchronized(this) {
                if (engine === loadedEngine) {
                    proposal = MealProposalParser.parse(output)
                    if (proposal == null) {
                        proposalDiagnostic = MealProposalParser.describeFormat(output)
                        imageReason = "Local analysis completed, but its draft was not safe to display. Enter details manually."
                    } else {
                        proposalDiagnostic = null
                        imageReason = "Local image analysis completed. Temporary image discarded."
                    }
                }
            }
        } catch (error: Exception) {
            synchronized(this) {
                if (engine === loadedEngine) {
                    val category = error.javaClass.simpleName.ifBlank { "runtime error" }
                    imageReason = "Local image analysis failed ($category). Try another image or enter details manually."
                }
            }
        } finally {
            conversation?.close()
            image.delete()
            synchronized(this) {
                if (activeConversation === conversation) activeConversation = null
                if (preparedImage === image) preparedImage = null
                imageState = "none"
            }
        }
    }

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest > maxDimension * 2) {
            sample *= 2
            largest /= 2
        }
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
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
        imageState,
        imageReason,
        proposal,
        proposalDiagnostic,
    )
}
