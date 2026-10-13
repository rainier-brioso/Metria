package app.metria

import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Opt-in debug metadata only; never stores prompts, replies, records, payloads, or errors. */
class AssistantTraceStore(
    noBackupFilesDir: File,
    private val debugBuild: Boolean,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    companion object {
        const val relativePath = "assistant-debug-trace.tsv"
        private const val enabledMarker = "assistant-debug-trace.enabled"
        private const val maxEvents = 100
        private const val maxBytes = 64 * 1024L
        private const val retentionMillis = 24 * 60 * 60 * 1000L
        private val allowedKinds = setOf("turn_started", "tool_required", "tool_continued", "reply_audited", "reply_fallback", "reply_rejected", "cancelled", "failed", "locked", "route_decision", "route_selected")
        private val allowedOperations = setOf("agent_turn", "agent_tool_result", "coach", "coach_intent", "coach_audio", "meal", "unknown")
        private val allowedOutcomes = setOf("started", "accepted", "rejected", "cancelled", "failed", "waiting", "cleared")
        private val allowedReasons = setOf("none", "required_tool_missing", "unsupported_personal_fact", "reply_policy", "multiple_tools", "unsupported_tool", "timeout", "runtime_failure", "minilm_accepted", "today_overview", "goal_remaining", "saved_dish_recall", "saved_dish_draft", "navigation", "scope", "scope_text", "scope_selected", "scope_host", "scope_context", "similarity", "margin", "runtime", "inference", "interrupted", "native_gemma", "legacy_gemma", "host_reply")
        private val allowedTools = setOf("none", "read_progress_overview", "read_nutrition_summary", "read_day_nutrition", "read_weight", "read_meals", "read_latest_meal", "search_saved_dishes")
        private val allowedPeriods = setOf("none", "today", "yesterday", "week")
    }

    data class Status(val enabled: Boolean, val eventCount: Int, val bytes: Long)

    private val file = File(noBackupFilesDir, relativePath)
    private val marker = File(noBackupFilesDir, enabledMarker)
    private val writer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var enabled = false
    @Volatile private var snapshot = Status(false, 0, 0)
    @Volatile private var closed = false
    private var mutationGeneration = 0L

    init {
        if (debugBuild) writer.execute {
            synchronized(this) {
                if (mutationGeneration == 0L) enabled = marker.isFile
                val lines = pruneLocked()
                snapshot = Status(enabled, lines.size, file.takeIf(File::isFile)?.length() ?: 0L)
            }
        }
        if (debugBuild) writer.scheduleAtFixedRate({
            synchronized(this) {
                if (!closed && enabled) {
                    val lines = pruneLocked()
                    snapshot = Status(true, lines.size, file.takeIf(File::isFile)?.length() ?: 0L)
                }
            }
        }, 1, 1, TimeUnit.HOURS)
    }

    fun status(): Status = if (debugBuild) snapshot else Status(false, 0, 0)

    fun setEnabled(value: Boolean) {
        if (!debugBuild) return
        val generation = synchronized(this) { mutationGeneration += 1L; mutationGeneration }
        enabled = value
        snapshot = Status(value, if (value) snapshot.eventCount else 0, if (value) snapshot.bytes else 0)
        runCatching { writer.execute {
            synchronized(this) {
                if (!value) {
                    marker.delete()
                    file.delete()
                    if (generation == mutationGeneration) {
                        enabled = false
                        snapshot = Status(false, 0, 0)
                    }
                    return@synchronized
                }
                if (generation != mutationGeneration) return@synchronized
                marker.parentFile?.mkdirs()
                marker.writeText("enabled\n")
                enabled = value
                snapshot = Status(value, if (value) snapshot.eventCount else 0, if (value) snapshot.bytes else 0)
            }
        } }.onFailure { if (it !is RejectedExecutionException) throw it }
    }

    fun record(
        kind: String,
        operation: String = "unknown",
        outcome: String = "started",
        reason: String = "none",
        tool: String = "none",
        period: String = "none",
        requestId: Long? = null,
        positiveScore: Float? = null,
        competingScore: Float? = null,
        fallbackScore: Float? = null,
    ) {
        if (!debugBuild || !enabled || kind !in allowedKinds) return
        val safeOperation = operation.takeIf { it in allowedOperations } ?: "unknown"
        val safeOutcome = outcome.takeIf { it in allowedOutcomes } ?: "failed"
        val safeReason = reason.takeIf { it in allowedReasons } ?: "none"
        val safeTool = tool.takeIf { it in allowedTools } ?: "none"
        val safePeriod = period.takeIf { it in allowedPeriods } ?: "none"
        val safeRequestId = requestId?.takeIf { it in 1..9_999_999 }?.toString() ?: "none"
        val scores = if (kind == "route_decision") {
            listOf(positiveScore, competingScore, fallbackScore).map(::formatScore)
        } else emptyList()
        val scoreFields = if (scores.any { it != null }) scores.joinToString("\t") { it ?: "none" } else null
        val timestamp = nowMillis()
        val generation = synchronized(this) { mutationGeneration }
        runCatching { writer.execute {
            synchronized(this) {
                if (!enabled || generation != mutationGeneration) return@synchronized
                val retained = readEventsLocked(timestamp).toMutableList()
                val base = "$timestamp\t$kind\t$safeOperation\t$safeOutcome\t$safeReason\t$safeTool\t$safePeriod\t$safeRequestId"
                retained += if (scoreFields == null) base else "$base\t$scoreFields"
                val bounded = retained.takeLast(maxEvents).takeLastWhile { line -> line.substringBefore('\t').toLongOrNull()?.let { timestamp - it <= retentionMillis } ?: false }
                writeEventsLocked(bounded)
                snapshot = Status(true, bounded.size, file.length())
            }
        } }.onFailure { if (it !is RejectedExecutionException) throw it }
    }

    fun clear() {
        if (!debugBuild) return
        val generation = synchronized(this) { mutationGeneration += 1L; mutationGeneration }
        snapshot = Status(enabled, 0, 0)
        runCatching { writer.execute { synchronized(this) { file.delete(); if (generation == mutationGeneration) snapshot = Status(enabled, 0, 0) } } }
    }

    fun delete() {
        if (!debugBuild) return
        synchronized(this) { mutationGeneration += 1L }
        enabled = false
        snapshot = Status(false, 0, 0)
        runCatching { writer.execute { synchronized(this) { file.delete(); marker.delete() } } }
    }

    fun flush() {
        if (!debugBuild) return
        runCatching { writer.submit { }.get(2, TimeUnit.SECONDS) }
    }

    override fun close() {
        flush()
        closed = true
        writer.shutdownNow()
    }

    private fun readEventsLocked(now: Long = nowMillis()): List<String> {
        if (!file.isFile || file.length() > maxBytes) return emptyList()
        return file.readLines().filter { line ->
            val timestamp = line.substringBefore('\t').toLongOrNull()
            val fields = line.split('\t')
            timestamp != null && now - timestamp in 0..retentionMillis && fields.size in setOf(8, 11) &&
                fields[1] in allowedKinds && fields[2] in allowedOperations && fields[3] in allowedOutcomes &&
                fields[4] in allowedReasons && fields[5] in allowedTools && fields[6] in allowedPeriods &&
                (fields[7] == "none" || fields[7].toLongOrNull()?.let { it in 1..9_999_999 } == true) &&
                (fields.size == 8 || fields[1] == "route_decision" && fields.drop(8).all(::validStoredScore))
        }.takeLast(maxEvents)
    }

    private fun formatScore(value: Float?): String? = value
        ?.takeIf { it.isFinite() && it in -1.001f..1.001f }
        ?.coerceIn(-1f, 1f)
        ?.let { String.format(Locale.ROOT, "%.4f", it) }

    private fun validStoredScore(value: String): Boolean = value == "none" ||
        (value.matches(Regex("-?(?:0|1)\\.\\d{4}")) &&
            value.toFloatOrNull()?.let { it.isFinite() && it in -1f..1f } == true)

    private fun pruneLocked(): List<String> {
        if (file.isFile && file.length() > maxBytes) { file.delete(); return emptyList() }
        val lines = readEventsLocked()
        if (file.isFile && file.readLines() != lines) writeEventsLocked(lines)
        return lines
    }

    private fun writeEventsLocked(lines: List<String>) {
        file.parentFile?.mkdirs()
        var bounded = lines
        while (bounded.joinToString("\n").toByteArray().size > maxBytes && bounded.isNotEmpty()) bounded = bounded.drop(1)
        file.writeText(if (bounded.isEmpty()) "" else bounded.joinToString("\n") + "\n")
    }
}
