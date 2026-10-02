package app.metria

/** Serializes one async native start against cancellation and suppresses duplicate cancel calls. */
internal class NativeAsyncCancellation(
    private val acquireCancelLease: () -> Boolean = { true },
    private val cancelNative: () -> Unit,
    private val releaseCancelLease: () -> Unit = {},
) {
    private var started = false
    private var starting = false
    private var cancelRequested = false
    private var cancellationSent = false
    private var completed = false

    fun start(startNative: () -> Unit): Boolean {
        synchronized(this) {
            if (cancelRequested || completed) return false
            starting = true
        }
        var startSucceeded = false
        var cancelAfterStart = false
        try {
            startNative()
            startSucceeded = true
        } finally {
            cancelAfterStart = synchronized(this) {
                starting = false
                started = startSucceeded
                if (startSucceeded && cancelRequested && !completed && !cancellationSent) {
                    cancellationSent = true
                    acquireCancelLease()
                } else false
            }
            if (cancelAfterStart) runNativeCancellation()
        }
        return true
    }

    fun cancel(onClaimed: () -> Unit = {}): Boolean {
        var cancelNow = false
        synchronized(this) {
            if (cancelRequested || completed) return false
            cancelRequested = true
            // Publish the timeout/cancellation owner while terminal callbacks are excluded
            // by this gate, before cancelProcess() can block or the callback can wake a waiter.
            runCatching(onClaimed)
            if (started && !starting && !cancellationSent) {
                cancellationSent = true
                cancelNow = acquireCancelLease()
            }
        }
        if (cancelNow) runNativeCancellation()
        return true
    }

    fun complete() {
        synchronized(this) { completed = true }
    }

    private fun runNativeCancellation() {
        try {
            runCatching(cancelNative)
        } finally {
            runCatching(releaseCancelLease)
        }
    }
}

internal data class NativeAsyncMessageSnapshot<T>(
    val text: String,
    val toolCalls: List<T>,
    val channels: Map<String, String>,
)

/** Aggregates LiteRT's delta chunks while retaining every structured tool call. */
internal class NativeAsyncMessageAccumulator<T> {
    private val text = StringBuilder()
    private var toolCalls = emptyList<T>()
    private var channels = emptyMap<String, String>()

    @Synchronized
    fun append(chunk: String, newToolCalls: List<T>, newChannels: Map<String, String>) {
        if (chunk.isNotEmpty()) text.append(chunk)
        if (newToolCalls.isNotEmpty()) toolCalls = toolCalls + newToolCalls
        if (newChannels.isNotEmpty()) channels = channels + newChannels
    }

    @Synchronized
    fun snapshot() = NativeAsyncMessageSnapshot(text.toString(), toolCalls, channels)

}
