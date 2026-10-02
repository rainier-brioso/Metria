package app.metria

/** Defers native conversation deletion until every in-flight native operation has exited. */
internal class NativeConversationLifetime<T : Any>(
    internal val conversation: T,
    private val cancel: (T) -> Unit,
    private val close: (T) -> Unit,
) {
    private var users = 0
    private var retired = false
    private var closed = false

    @Synchronized
    fun acquire(): Boolean {
        if (retired || closed) return false
        users += 1
        return true
    }

    /** Holds native lifetime for cancelProcess even if retirement already began. */
    @Synchronized
    fun acquireCancellationLease(): Boolean {
        if (closed) return false
        users += 1
        return true
    }

    fun retire() {
        val cancelActiveOperation = synchronized(this) {
            retired = true
            if (users == 0) false else {
                // Keep a lease for the cancel call itself so the last worker cannot close
                // the handle between retirement and cancelProcess().
                users += 1
                true
            }
        }
        if (cancelActiveOperation) {
            runCatching { cancel(conversation) }
            release()
        } else {
            val closeNow = synchronized(this) { claimCloseIfUnused() }
            if (closeNow) runCatching { close(conversation) }
        }
    }

    fun release() {
        val closeNow = synchronized(this) {
            check(users > 0) { "Conversation use released without a matching acquire." }
            users -= 1
            claimCloseIfUnused()
        }
        if (closeNow) runCatching { close(conversation) }
    }

    @Synchronized
    private fun claimCloseIfUnused(): Boolean {
        if (!retired || users != 0 || closed) return false
        closed = true
        return true
    }
}
