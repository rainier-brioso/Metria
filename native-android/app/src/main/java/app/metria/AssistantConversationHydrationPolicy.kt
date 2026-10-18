package app.metria

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AssistantConversationHydrationDecision {
    KEEP_LOCAL,
    RESTORE_PERSISTED,
    EXPIRED,
}

/** Keeps transient request identity when a vault snapshot is only its canonical retained form. */
internal object AssistantConversationHydrationPolicy {
    fun decide(
        hydrated: Boolean,
        previouslyLoaded: PrivateConversationRecord?,
        localMessages: List<PrivateConversationMessage>,
        persisted: PrivateConversationRecord?,
        expired: Boolean,
        acknowledgedOwnWrite: Boolean = false,
    ): AssistantConversationHydrationDecision {
        if (expired) return AssistantConversationHydrationDecision.EXPIRED
        if (!hydrated) return AssistantConversationHydrationDecision.RESTORE_PERSISTED

        // A successful write from this unlocked session can be an intermediate snapshot: newer
        // local turns may already be on screen while the debounced writer is still catching up.
        // Only an exact record receipt can make that snapshot safe to ignore.
        if (acknowledgedOwnWrite) return AssistantConversationHydrationDecision.KEEP_LOCAL

        // Ignore status refreshes that return the last loaded vault snapshot. The local transcript
        // may already contain a new member turn that has not reached the debounced writer yet.
        if (persisted == previouslyLoaded) return AssistantConversationHydrationDecision.KEEP_LOCAL

        val canonicalLocal = persisted?.let {
            PrivateConversationPolicy.updated(localMessages, it.updatedAt)?.messages
        }
        return if (persisted != null && canonicalLocal == persisted.messages) {
            AssistantConversationHydrationDecision.KEEP_LOCAL
        } else {
            AssistantConversationHydrationDecision.RESTORE_PERSISTED
        }
    }
}

/** Bounded, session-only acknowledgements for exact successful encrypted conversation writes. */
internal class AssistantConversationWriteReceipts(
    private val maxReceipts: Int = 8,
    private val lifetime: Duration = Duration.ofMinutes(2),
) {
    private data class Receipt(
        val generation: Long,
        val record: PrivateConversationRecord,
        val recordedAt: Instant,
    )

    private var generation = 0L
    private val receipts = ArrayDeque<Receipt>()

    init {
        require(maxReceipts > 0)
        require(!lifetime.isNegative && !lifetime.isZero)
    }

    @Synchronized
    fun captureGeneration(): Long = generation

    @Synchronized
    fun register(
        capturedGeneration: Long,
        record: PrivateConversationRecord,
        now: Instant = Instant.now(),
    ): Boolean {
        if (capturedGeneration != generation) return false
        prune(now)
        val exactRecord = record.copy(messages = record.messages.map { it.copy() })
        receipts.removeAll { it.generation == generation && it.record == exactRecord }
        receipts.addLast(Receipt(generation, exactRecord, now))
        while (receipts.size > maxReceipts) receipts.removeFirst()
        return true
    }

    /** Commit and acknowledge on IO before cancellation can discard the save result. */
    suspend fun saveSnapshot(
        snapshot: List<PrivateConversationMessage>,
        save: (List<PrivateConversationMessage>) -> EncryptedVault.Status,
    ): Boolean {
        val capturedGeneration = captureGeneration()
        val result = withContext(Dispatchers.IO) {
            save(snapshot).also { status ->
                if (status is EncryptedVault.Status.Unlocked) {
                    status.activeConversation?.let { register(capturedGeneration, it) }
                }
            }
        }
        return result is EncryptedVault.Status.Unlocked
    }

    @Synchronized
    fun recognizes(record: PrivateConversationRecord?, now: Instant = Instant.now()): Boolean {
        prune(now)
        if (record == null) return false
        return receipts.any { it.generation == generation && it.record == record }
    }

    /** Invalidate on lock, clear, expiry, or an unknown persisted transcript replacement. */
    @Synchronized
    fun invalidate() {
        generation += 1
        receipts.clear()
    }

    private fun prune(now: Instant) {
        receipts.removeAll { receipt ->
            receipt.generation != generation || now.isBefore(receipt.recordedAt) ||
                !now.isBefore(receipt.recordedAt.plus(lifetime))
        }
    }
}
