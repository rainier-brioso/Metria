package app.metria

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugImageCaptureStoreTest {
    @Test
    fun `capture is off by default and creates no files`() {
        val root = Files.createTempDirectory("image-capture-off").toFile()
        DebugImageCaptureStore(root, debugBuild = true, nowMillis = { 1_000L }).use { store ->
            assertFalse(store.status().enabled)
            assertFalse(store.capture(store.recordingToken(), 12, "image_meal", "draft_ready", "none", "response", jpeg()))
            assertFalse(root.resolve("debug-image-capture").exists())
        }
    }

    @Test
    fun `explicit opt in survives a new store while capture payload is removed`() {
        val root = Files.createTempDirectory("image-capture-persistent-opt-in").toFile()
        DebugImageCaptureStore(root, true, { 2_000L }).use { store ->
            assertFalse(store.status().enabled)
            assertTrue(store.setEnabled(true))
            assertTrue(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "response", jpeg()))
            assertTrue(root.resolve("debug-image-capture-enabled").isFile)
        }
        DebugImageCaptureStore(root, true, { 3_000L }).use { restarted ->
            assertTrue(restarted.status().enabled)
            assertNull(restarted.read())
            assertFalse(root.resolve("debug-image-capture").exists())
        }
    }

    @Test
    fun `clear equivalent to background or lock keeps opt in across restart`() {
        val root = Files.createTempDirectory("image-capture-clear-retains-opt-in").toFile()
        DebugImageCaptureStore(root, true, { 2_500L }).use { store ->
            assertTrue(store.setEnabled(true))
            assertTrue(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "response", jpeg()))
            store.clear()
            assertTrue(store.status().enabled)
            assertNull(store.read())
        }
        DebugImageCaptureStore(root, true, { 3_000L }).use { restarted ->
            assertTrue(restarted.status().enabled)
            assertNull(restarted.read())
        }
    }

    @Test
    fun `late analysis cannot recreate capture after background or lock clear`() {
        val root = Files.createTempDirectory("image-capture-late-result").toFile()
        DebugImageCaptureStore(root, true, { 4_000L }).use { store ->
            assertTrue(store.setEnabled(true))
            val inFlightToken = requireNotNull(store.recordingToken())
            store.clear() // MainActivity.onStop or lockVault
            assertTrue(store.status().enabled)
            assertFalse(store.capture(inFlightToken, 5, "image_meal", "draft_ready", "none", "late response", jpeg()))
            assertNull(store.read())
            val nextRunToken = requireNotNull(store.recordingToken())
            assertTrue(store.capture(nextRunToken, 6, "image_meal", "draft_ready", "none", "next response", jpeg()))
            assertEquals("next response", store.read()?.modelResponse)
        }
    }

    @Test
    fun `opted-in capture reads bounded technical fields response and reduced jpeg`() {
        val root = Files.createTempDirectory("image-capture-enabled").toFile()
        val jpeg = jpeg(42)
        DebugImageCaptureStore(root, debugBuild = true, nowMillis = { 10_000L }).use { store ->
            store.setEnabled(true)
            assertTrue(store.capture(store.recordingToken(), 4_321, "image_meal", "draft_rejected", "invalid_range", "model response", jpeg))
            assertEquals(
                DebugImageCaptureStore.Status(true, true, root.resolve(DebugImageCaptureStore.relativePath).length(), 10_000L),
                store.status(),
            )
            val capture = requireNotNull(store.read())
            assertEquals(10_000L, capture.capturedAtMillis)
            assertEquals(4_321L, capture.durationMillis)
            assertEquals("image_meal", capture.operation)
            assertEquals("invalid_range", capture.category)
            assertEquals("draft_rejected", capture.outcome)
            assertEquals("model response", capture.modelResponse)
            assertArrayEquals(jpeg, capture.thumbnailJpeg)
        }
    }

    @Test
    fun `partial proposal capture records only the fixed outcome and omission category`() {
        val root = Files.createTempDirectory("image-capture-partial").toFile()
        DebugImageCaptureStore(root, debugBuild = true, nowMillis = { 11_000L }).use { store ->
            store.setEnabled(true)
            assertTrue(store.capture(
                store.recordingToken(),
                5_000,
                "image_meal",
                "draft_partial",
                MealProposalOmissionCategory.MULTIPLE.auditCode,
                "synthetic model response",
                jpeg(),
            ))
            val capture = requireNotNull(store.read())
            assertEquals("draft_partial", capture.outcome)
            assertEquals("multiple_omissions", capture.category)
            assertEquals("synthetic model response", capture.modelResponse)
        }
    }

    @Test
    fun `response is truncated and oversized or malformed thumbnails are rejected`() {
        val root = Files.createTempDirectory("image-capture-bounds").toFile()
        val response = "x".repeat(DebugImageCaptureStore.MAX_RESPONSE_CHARS + 100)
        DebugImageCaptureStore(root, true, { 20_000L }).use { store ->
            store.setEnabled(true)
            assertTrue(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", response, jpeg()))
            assertEquals(DebugImageCaptureStore.MAX_RESPONSE_CHARS, store.read()?.modelResponse?.length)
            assertFalse(
                store.capture(
                    store.recordingToken(),
                    2,
                    "image_meal",
                    "draft_ready",
                    "none",
                    "replacement",
                    jpeg(DebugImageCaptureStore.MAX_THUMBNAIL_BYTES + 1),
                ),
            )
            assertFalse(store.capture(store.recordingToken(), 2, "image_meal", "draft_ready", "none", "replacement", byteArrayOf(1, 2, 3, 4)))
            assertEquals(response.take(DebugImageCaptureStore.MAX_RESPONSE_CHARS), store.read()?.modelResponse)
        }
    }

    @Test
    fun `capture expires on read and is deleted on next process launch`() {
        val root = Files.createTempDirectory("image-capture-ttl").toFile()
        var now = 30_000L
        DebugImageCaptureStore(root, true, { now }).use { store ->
            store.setEnabled(true)
            assertTrue(store.capture(store.recordingToken(), 10, "image_meal", "draft_ready", "none", "response", jpeg()))
            now += DebugImageCaptureStore.RETENTION_MILLIS + 1
            assertNull(store.read())
            assertFalse(root.resolve(DebugImageCaptureStore.relativePath).exists())
        }

        now = 80_000L
        DebugImageCaptureStore(root, true, { now }).use { firstProcess ->
            firstProcess.setEnabled(true)
            assertTrue(firstProcess.capture(firstProcess.recordingToken(), 10, "image_meal", "draft_ready", "none", "session-only", jpeg()))
        }
        DebugImageCaptureStore(root, true, { now }).use { nextProcess ->
            assertTrue(nextProcess.status().enabled)
            assertNull(nextProcess.read())
            assertFalse(root.resolve(DebugImageCaptureStore.relativePath).exists())
        }
    }

    @Test
    fun `clear preserves opt-in while disable erases capture and requires a new opt-in`() {
        val root = Files.createTempDirectory("image-capture-clear").toFile()
        DebugImageCaptureStore(root, true, { 40_000L }).use { store ->
            store.setEnabled(true)
            assertTrue(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "first", jpeg()))
            store.clear()
            assertTrue(store.status().enabled)
            assertNull(store.read())
            assertTrue(store.capture(store.recordingToken(), 2, "image_meal", "draft_ready", "none", "second", jpeg()))

            store.clearAndDisable()
            assertFalse(store.status().enabled)
            assertFalse(store.capture(store.recordingToken(), 3, "image_meal", "draft_ready", "none", "third", jpeg()))
            assertFalse(root.resolve(DebugImageCaptureStore.relativePath).exists())

            store.setEnabled(true)
            assertTrue(store.capture(store.recordingToken(), 4, "image_meal", "draft_ready", "none", "fourth", jpeg()))
            store.setEnabled(false)
            assertFalse(store.status().enabled)
            assertFalse(root.resolve(DebugImageCaptureStore.relativePath).exists())
        }
        DebugImageCaptureStore(root, true).use { restarted ->
            assertFalse(restarted.status().enabled)
            assertNull(restarted.read())
        }
    }

    @Test
    fun `release build cannot opt in capture or create diagnostic files`() {
        val root = Files.createTempDirectory("image-capture-release").toFile()
        DebugImageCaptureStore(root, debugBuild = false, nowMillis = { 50_000L }).use { store ->
            store.setEnabled(true)
            assertFalse(store.status().enabled)
            assertFalse(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "response", jpeg()))
            store.clear()
            store.clearAndDisable()
            assertNull(store.read())
            assertFalse(root.resolve("debug-image-capture").exists())
            DebugImageCaptureStore(root, true, { 40_000L }).use { restarted ->
                assertFalse(restarted.status().enabled)
            }
        }
    }

    @Test
    fun `release process removes a leftover debug capture on startup`() {
        val root = Files.createTempDirectory("image-capture-release-cleanup").toFile()
        val stale = root.resolve(DebugImageCaptureStore.relativePath)
        stale.parentFile!!.mkdirs()
        stale.writeText("stale raw test data")
        val staleOptIn = root.resolve("debug-image-capture-enabled")
        staleOptIn.writeBytes(byteArrayOf(1))

        DebugImageCaptureStore(root, debugBuild = false).use { store ->
            assertFalse(stale.exists())
            assertFalse(staleOptIn.exists())
            assertFalse(store.status().enabled)
        }
    }

    @Test
    fun `close clears capture and disables this process instance`() {
        val root = Files.createTempDirectory("image-capture-close").toFile()
        val store = DebugImageCaptureStore(root, true, { 60_000L })
        store.setEnabled(true)
        assertTrue(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "response", jpeg()))
        store.close()
        assertFalse(store.status().enabled)
        assertFalse(root.resolve(DebugImageCaptureStore.relativePath).exists())
        DebugImageCaptureStore(root, true).use { restarted -> assertTrue(restarted.status().enabled) }
    }

    @Test
    fun `failed opt in write remains disabled and reports failure`() {
        val parent = Files.createTempDirectory("image-capture-write-failure").toFile()
        val root = parent.resolve("not-a-directory").apply { writeText("block preference writes") }
        DebugImageCaptureStore(root, true).use { store ->
            assertFalse(store.setEnabled(true))
            assertFalse(store.status().enabled)
            assertFalse(store.capture(store.recordingToken(), 1, "image_meal", "draft_ready", "none", "response", jpeg()))
        }
    }

    private fun jpeg(payloadSize: Int = 8): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + ByteArray((payloadSize - 4).coerceAtLeast(0)) { 0x2A } +
            byteArrayOf(0xFF.toByte(), 0xD9.toByte())
}
