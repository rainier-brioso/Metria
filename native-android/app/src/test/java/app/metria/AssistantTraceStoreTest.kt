package app.metria

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantTraceStoreTest {
    @Test
    fun `trace is off by default and does not create a file`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, debugBuild = true, nowMillis = { 1_000L }).use { trace ->
            trace.record("turn_started", "agent_turn")
            trace.flush()
            assertFalse(trace.status().enabled)
            assertFalse(Files.exists(root.toPath().resolve(AssistantTraceStore.relativePath)))
        }
    }

    @Test
    fun `enabled trace writes only the closed metadata vocabulary`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.record("turn_started", "agent_turn", "started")
            trace.record("not_allowed", "agent_turn", "started")
            trace.record("reply_rejected", "agent_turn", "rejected", "unsupported_personal_fact", "read_nutrition_summary", "week", 42L)
            trace.record("reply_audited", "prompt=secret", "reply=secret", "secret-record-value", "secret-tool", "secret-period", 42L)
            trace.flush()
            val text = root.resolve(AssistantTraceStore.relativePath).readText()
            assertTrue(text.contains("turn_started\tagent_turn\tstarted"))
            assertTrue(text.contains("reply_rejected\tagent_turn\trejected\tunsupported_personal_fact\tread_nutrition_summary\tweek\t42"))
            assertFalse(text.contains("not_allowed"))
            assertFalse(text.contains("secret"))
        }
    }

    @Test
    fun `trace is bounded and expires old events`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        var now = 86_400_001L
        AssistantTraceStore(root, true, { now }).use { trace ->
            trace.setEnabled(true)
            trace.record("turn_started", "agent_turn")
            trace.flush()
            now += 1L
            repeat(120) { trace.record("reply_audited", "agent_turn", "accepted") }
            trace.flush()
            assertEquals(100, trace.status().eventCount)
            assertTrue(trace.status().bytes <= 64 * 1024L)
        }
    }

    @Test
    fun `clear removes trace and delete disables it`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.record("cancelled", "agent_turn", "cancelled")
            trace.flush()
            trace.clear()
            assertEquals(0, trace.status().eventCount)
            trace.delete()
            assertFalse(trace.status().enabled)
            assertFalse(root.resolve(AssistantTraceStore.relativePath).exists())
        }
    }

    @Test
    fun `startup prunes expired and malformed content`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        root.resolve(AssistantTraceStore.relativePath).writeText("0\tbad\tbad\tbad\nnot-a-trace\n")
        AssistantTraceStore(root, true, { 86_400_001L }).use { trace ->
            trace.flush()
            assertEquals(0, trace.status().eventCount)
            assertEquals("", root.resolve(AssistantTraceStore.relativePath).readText())
        }
    }

    @Test
    fun `route metadata keeps legacy rows and bounded finite scores across restart`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.flush()
            trace.record("route_decision", "agent_turn", "rejected", "margin", "read_day_nutrition", "today", 42L,
                positiveScore = 0.71234f, competingScore = 0.68123f, fallbackScore = 0.42f)
            trace.record("route_selected", "coach_intent", "accepted", "legacy_gemma", requestId = 43L)
            trace.flush()
            val rows = root.resolve(AssistantTraceStore.relativePath).readLines()
            assertEquals(11, rows.first().split('\t').size)
            assertTrue(rows.first().endsWith("0.7123\t0.6812\t0.4200"))
            assertEquals(8, rows.last().split('\t').size)
        }
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.flush()
            assertEquals(2, trace.status().eventCount)
            val rows = root.resolve(AssistantTraceStore.relativePath).readLines()
            assertEquals(11, rows.first().split('\t').size)
            assertEquals(8, rows.last().split('\t').size)
        }
    }

    @Test
    fun `invalid scores never persist nonfinite values and malformed score rows are pruned`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.flush()
            trace.record("route_decision", "agent_turn", "rejected", "similarity", requestId = 42L,
                positiveScore = Float.NaN, competingScore = Float.POSITIVE_INFINITY, fallbackScore = 1.2f)
            trace.record("route_decision", "agent_turn", "rejected", "margin", requestId = 43L,
                positiveScore = 0.7f, competingScore = Float.NaN)
            trace.record("route_decision", "agent_turn", "accepted", "minilm_accepted", requestId = 45L,
                positiveScore = 1.0005f)
            trace.flush()
            val rows = root.resolve(AssistantTraceStore.relativePath).readLines()
            assertEquals(8, rows.first().split('\t').size)
            assertFalse(rows.any { it.contains("NaN") || it.contains("Infinity") })
            assertTrue(rows.last().endsWith("1.0000\tnone\tnone"))
        }
        val file = root.resolve(AssistantTraceStore.relativePath)
        file.appendText("\n1000\troute_decision\tagent_turn\trejected\tsimilarity\tnone\tnone\t44\tNaN\tnone\tnone\n")
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.flush()
            assertEquals(3, trace.status().eventCount)
            assertFalse(file.readText().contains("NaN"))
        }
    }

    @Test
    fun `release build is a no-op and enabled marker survives restart`() {
        val root = Files.createTempDirectory("trace-test").toFile()
        AssistantTraceStore(root, false, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.record("turn_started")
            trace.flush()
            assertEquals(AssistantTraceStore.Status(false, 0, 0), trace.status())
        }
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.setEnabled(true)
            trace.record("turn_started")
            trace.flush()
        }
        AssistantTraceStore(root, true, { 1_000L }).use { trace ->
            trace.flush()
            assertTrue(trace.status().enabled)
            assertEquals(1, trace.status().eventCount)
        }
    }
}
