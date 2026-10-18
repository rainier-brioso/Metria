package app.metria

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class ConversationPersistenceResult { SAVED, FAILED, SUPERSEDED }

internal enum class ConversationNavigationResult { COMPLETED, SAVE_FAILED, STALE, DUPLICATE }

/** Serializes encrypted conversation snapshots and clear operations for one unlocked session. */
internal class AssistantConversationPersistence {
    private val mutex = Mutex()
    private val monitor = Any()
    private var generation = 0L
    private var lastCompletedNavigationRequestId = Long.MIN_VALUE
    private val navigatingRequestIds = mutableSetOf<Long>()
    private var navigationTranscript: List<PrivateConversationMessage>? = null
    private var activeClearCount = 0

    /** Hold the writer gate through a small in-memory hydration decision and its write invalidation. */
    suspend fun <T> withWriterIdle(block: () -> T): T = mutex.withLock { block() }

    fun invalidatePendingWrites() {
        synchronized(monitor) {
            generation += 1
            navigationTranscript = null
        }
    }

    /** Reserve the snapshot's ordering before a debounce delay can let it outlive a clear. */
    fun captureWriteTicket(): Long? = synchronized(monitor) {
        if (activeClearCount > 0 || navigatingRequestIds.isNotEmpty()) null else ++generation
    }

    suspend fun persist(
        messages: List<PrivateConversationMessage>,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
    ): ConversationPersistenceResult {
        val ticket = captureWriteTicket() ?: return ConversationPersistenceResult.SUPERSEDED
        return persist(ticket, messages, save)
    }

    suspend fun persist(
        ticket: Long,
        messages: List<PrivateConversationMessage>,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
    ): ConversationPersistenceResult {
        val eligible = synchronized(monitor) {
            activeClearCount == 0 && navigatingRequestIds.isEmpty() && !isOlderThanNavigationTranscript(messages)
        }
        if (!eligible) return ConversationPersistenceResult.SUPERSEDED
        return perform(ticket) { save(messages) }
    }

    suspend fun clear(clear: suspend () -> Boolean): ConversationPersistenceResult {
        val ticket = synchronized(monitor) {
            activeClearCount += 1
            ++generation
        }
        return try {
            val result = perform(ticket, clear)
            if (result == ConversationPersistenceResult.SAVED) {
                synchronized(monitor) { navigationTranscript = null }
            }
            result
        } finally {
            synchronized(monitor) { activeClearCount -= 1 }
        }
    }

    suspend fun persistBeforeNavigation(
        requestId: Long,
        messages: List<PrivateConversationMessage>,
        isCurrent: () -> Boolean,
        save: suspend (List<PrivateConversationMessage>) -> Boolean,
        navigate: (Long) -> Boolean,
        repeatable: Boolean = false,
    ): ConversationNavigationResult {
        if (!isCurrent()) return ConversationNavigationResult.STALE
        val ticket = synchronized(monitor) {
            if (activeClearCount > 0 || (!repeatable && requestId <= lastCompletedNavigationRequestId) ||
                !navigatingRequestIds.add(requestId)) {
                return ConversationNavigationResult.DUPLICATE
            }
            ++generation
        }
        try {
            val persisted = mutex.withLock {
                if (!isGenerationCurrent(ticket)) return@withLock ConversationPersistenceResult.SUPERSEDED
                if (save(messages)) ConversationPersistenceResult.SAVED else ConversationPersistenceResult.FAILED
            }
            currentCoroutineContext().ensureActive()
            if (!isCurrent()) return ConversationNavigationResult.STALE
            if (!isGenerationCurrent(ticket)) return ConversationNavigationResult.STALE
            if (persisted == ConversationPersistenceResult.SUPERSEDED) return ConversationNavigationResult.STALE
            if (persisted == ConversationPersistenceResult.FAILED) return ConversationNavigationResult.SAVE_FAILED
            synchronized(monitor) { navigationTranscript = messages.toList() }
            currentCoroutineContext().ensureActive()
            val completed = synchronized(monitor) {
                if (generation != ticket || activeClearCount > 0 || !isCurrent()) false
                else navigate(requestId)
            }
            if (!completed) return ConversationNavigationResult.STALE
            synchronized(monitor) {
                if (!repeatable) lastCompletedNavigationRequestId = maxOf(lastCompletedNavigationRequestId, requestId)
            }
            return ConversationNavigationResult.COMPLETED
        } finally {
            synchronized(monitor) { navigatingRequestIds.remove(requestId) }
        }
    }

    private suspend fun perform(ticket: Long, write: suspend () -> Boolean): ConversationPersistenceResult {
        return mutex.withLock {
            if (!isGenerationCurrent(ticket)) return@withLock ConversationPersistenceResult.SUPERSEDED
            if (write()) ConversationPersistenceResult.SAVED else ConversationPersistenceResult.FAILED
        }
    }

    private fun isGenerationCurrent(ticket: Long): Boolean = synchronized(monitor) { generation == ticket }

    private fun isOlderThanNavigationTranscript(messages: List<PrivateConversationMessage>): Boolean {
        val saved = navigationTranscript ?: return false
        return messages.size <= saved.size && saved.take(messages.size) == messages
    }
}
