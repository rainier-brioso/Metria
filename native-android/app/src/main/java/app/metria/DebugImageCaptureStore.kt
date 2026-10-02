package app.metria

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * One opt-in, short-lived local image-analysis capture for development diagnosis.
 * This file is separate from the metadata-only assistant trace and from the vault.
 */
class DebugImageCaptureStore(
    noBackupFilesDir: File,
    private val debugBuild: Boolean,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    companion object {
        const val relativePath = "debug-image-capture/capture.bin"
        private const val directoryName = "debug-image-capture"
        private const val captureName = "capture.bin"
        private const val enabledPreferenceName = "debug-image-capture-enabled"
        private const val temporarySuffix = ".tmp"
        private const val magic = 0x4D494341 // MICA
        private const val formatVersion = 1
        const val MAX_RESPONSE_CHARS = 16_000
        const val MAX_THUMBNAIL_BYTES = 128 * 1024
        const val RETENTION_MILLIS = 30 * 60 * 1000L
        private const val MAX_RESPONSE_UTF8_BYTES = MAX_RESPONSE_CHARS * 4
        private val operations = setOf("image_meal")
        private val categories = setOf("none", "invalid_json", "invalid_root_shape", "invalid_group", "invalid_item", "invalid_range", "too_many_items", "multiple_omissions", "unknown")
        private val outcomes = setOf("draft_ready", "draft_partial", "draft_rejected", "runtime_failure", "cancelled", "unknown")
    }

    data class Status(val enabled: Boolean, val hasCapture: Boolean, val bytes: Long, val capturedAtMillis: Long?)

    data class Capture(
        val capturedAtMillis: Long,
        val durationMillis: Long,
        val operation: String,
        val category: String,
        val outcome: String,
        val modelResponse: String,
        val thumbnailJpeg: ByteArray?,
    )

    private val directory = File(noBackupFilesDir, directoryName)
    private val captureFile = File(directory, captureName)
    private val temporaryFile = File(directory, captureName + temporarySuffix)
    private val enabledPreferenceFile = File(noBackupFilesDir, enabledPreferenceName)
    private val enabledPreferenceTempFile = File(noBackupFilesDir, enabledPreferenceName + temporarySuffix)
    private val expiryExecutor = Executors.newSingleThreadScheduledExecutor()
    private var expiryTask: ScheduledFuture<*>? = null
    private var captureEpoch = 0L
    @Volatile private var enabled = false
    @Volatile private var closed = false

    init {
        synchronized(this) {
            // Raw diagnostic capture never survives a process boundary.
            deleteCaptureFiles()
            if (debugBuild) {
                enabled = readEnabledPreference()
            } else {
                enabled = false
                enabledPreferenceFile.delete()
                enabledPreferenceTempFile.delete()
            }
        }
    }

    @Synchronized
    fun status(): Status {
        if (!debugBuild || closed) return Status(false, false, 0, null)
        if (enabled) pruneExpiredLocked()
        val capture = if (enabled) readCaptureLocked() else null
        return Status(enabled, capture != null, captureFile.takeIf(File::isFile)?.length() ?: 0L, capture?.capturedAtMillis)
    }

    /** Returns the single current diagnostic run only while this process has capture enabled. */
    @Synchronized
    fun read(): Capture? {
        if (!debugBuild || !enabled || closed) return null
        pruneExpiredLocked()
        return readCaptureLocked()
    }

    /** A run may write only if no clear, lock, or background transition happened after it began. */
    @Synchronized
    fun recordingToken(): Long? = if (debugBuild && enabled && !closed) captureEpoch else null

    @Synchronized
    fun setEnabled(value: Boolean): Boolean {
        if (!debugBuild || (closed && value)) return !value
        if (!value) {
            enabled = false
            clear()
            val persisted = writeEnabledPreference(false)
            if (!persisted) {
                enabledPreferenceFile.delete()
                enabledPreferenceTempFile.delete()
            }
            return persisted
        }
        if (enabled) return true
        if (!writeEnabledPreference(true)) {
            enabled = false
            return false
        }
        enabled = true
        pruneExpiredLocked()
        return true
    }

    /** Returns false when capture is disabled, invalid, expired, or could not be written. */
    @Synchronized
    fun capture(
        recordingToken: Long?,
        durationMillis: Long,
        operation: String,
        outcome: String,
        category: String,
        modelResponse: String,
        thumbnailJpeg: ByteArray?,
    ): Boolean {
        if (!debugBuild || !enabled || closed || recordingToken == null || recordingToken != captureEpoch) return false
        val timestamp = nowMillis()
        pruneExpiredLocked()
        if (durationMillis < 0L) return false
        if (thumbnailJpeg != null && !isBoundedJpeg(thumbnailJpeg)) return false
        val record = Capture(
            capturedAtMillis = timestamp,
            durationMillis = durationMillis,
            operation = operation.takeIf { it in operations } ?: "image_meal",
            category = category.takeIf { it in categories } ?: "unknown",
            outcome = outcome.takeIf { it in outcomes } ?: "unknown",
            modelResponse = modelResponse.take(MAX_RESPONSE_CHARS),
            thumbnailJpeg = thumbnailJpeg?.copyOf(),
        )
        val bytes = encode(record)
        if (bytes.size > MAX_CAPTURE_FILE_BYTES) return false
        val written = writeAtomically(captureFile, bytes)
        if (!written) temporaryFile.delete()
        if (written) {
            expiryTask?.cancel(false)
            expiryTask = expiryExecutor.schedule({ synchronized(this) { pruneExpiredLocked() } }, RETENTION_MILLIS + 1, TimeUnit.MILLISECONDS)
        }
        return written
    }

    /** Clears the current run while preserving the member's opt-in for another run. */
    @Synchronized
    fun clear() {
        captureEpoch++
        expiryTask?.cancel(false)
        expiryTask = null
        deleteCaptureFiles()
    }

    /** Erases the current run and turns capture off, while keeping this store reusable. */
    @Synchronized
    fun clearAndDisable() {
        setEnabled(false)
    }

    /** Activity teardown erases the raw capture but preserves the persistent opt-in. */
    @Synchronized
    override fun close() {
        if (closed) return
        clear()
        closed = true
        expiryExecutor.shutdownNow()
    }

    @Synchronized
    private fun pruneExpiredLocked() {
        if (!captureFile.isFile) {
            temporaryFile.delete()
            return
        }
        val capturedAt = runCatching { readCaptureLocked()?.capturedAtMillis }.getOrNull()
        if (capturedAt == null || isExpired(capturedAt, nowMillis())) deleteCaptureFiles()
    }

    private fun isExpired(capturedAt: Long, now: Long): Boolean =
        capturedAt > now || now - capturedAt > RETENTION_MILLIS

    private fun readCaptureLocked(): Capture? {
        if (!captureFile.isFile || captureFile.length() !in 1..MAX_CAPTURE_FILE_BYTES.toLong()) return null
        return runCatching {
            DataInputStream(ByteArrayInputStream(captureFile.readBytes())).use { input ->
                require(input.readInt() == magic && input.readInt() == formatVersion)
                val capturedAt = input.readLong()
                val duration = input.readLong()
                require(capturedAt > 0L && duration >= 0L)
                val operation = input.readBoundedString(32)
                val category = input.readBoundedString(32)
                val outcome = input.readBoundedString(32)
                require(operation in operations && category in categories && outcome in outcomes)
                val responseLength = input.readInt()
                require(responseLength in 0..MAX_RESPONSE_UTF8_BYTES)
                val responseBytes = ByteArray(responseLength)
                input.readFully(responseBytes)
                val response = String(responseBytes, StandardCharsets.UTF_8)
                require(response.length <= MAX_RESPONSE_CHARS)
                val imageLength = input.readInt()
                require(imageLength in 0..MAX_THUMBNAIL_BYTES)
                val image = if (imageLength == 0) null else ByteArray(imageLength).also(input::readFully)
                require(image == null || isBoundedJpeg(image))
                require(input.available() == 0)
                Capture(capturedAt, duration, operation, category, outcome, response, image)
            }
        }.getOrNull()
    }

    private fun encode(capture: Capture): ByteArray {
        val response = capture.modelResponse.toByteArray(StandardCharsets.UTF_8)
        val image = capture.thumbnailJpeg
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(magic)
                output.writeInt(formatVersion)
                output.writeLong(capture.capturedAtMillis)
                output.writeLong(capture.durationMillis)
                output.writeBoundedString(capture.operation)
                output.writeBoundedString(capture.category)
                output.writeBoundedString(capture.outcome)
                output.writeInt(response.size)
                output.write(response)
                output.writeInt(image?.size ?: 0)
                if (image != null) output.write(image)
            }
            bytes.toByteArray()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        if (!directory.exists() && !directory.mkdirs()) return false
        val temp = File(directory, target.name + temporarySuffix)
        return runCatching {
            temp.outputStream().use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        }.getOrDefault(false)
    }

    private fun deleteCaptureFiles() {
        captureFile.delete()
        temporaryFile.delete()
        directory.delete()
    }

    private fun readEnabledPreference(): Boolean = runCatching {
        enabledPreferenceFile.isFile && enabledPreferenceFile.readBytes().contentEquals(byteArrayOf(1))
    }.getOrDefault(false)

    private fun writeEnabledPreference(value: Boolean): Boolean = runCatching {
        enabledPreferenceTempFile.outputStream().use { stream ->
            stream.write(if (value) 1 else 0)
            stream.fd.sync()
        }
        try {
            Files.move(enabledPreferenceTempFile.toPath(), enabledPreferenceFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(enabledPreferenceTempFile.toPath(), enabledPreferenceFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        true
    }.getOrDefault(false).also { success ->
        if (!success) enabledPreferenceTempFile.delete()
    }

    private fun isBoundedJpeg(bytes: ByteArray): Boolean =
        bytes.size in 4..MAX_THUMBNAIL_BYTES &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
            bytes[bytes.lastIndex - 1] == 0xFF.toByte() && bytes.last() == 0xD9.toByte()

    private fun DataInputStream.readBoundedString(maxBytes: Int): String {
        val length = readInt()
        require(length in 0..maxBytes)
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun DataOutputStream.writeBoundedString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private val MAX_CAPTURE_FILE_BYTES = 8 + 8 + 8 + 4 + 32 + 4 + 32 + 4 + 32 + 4 + MAX_RESPONSE_UTF8_BYTES + 4 + MAX_THUMBNAIL_BYTES
}
