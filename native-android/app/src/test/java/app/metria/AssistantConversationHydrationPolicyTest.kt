package app.metria

import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantConversationHydrationPolicyTest {
    private val loadedAt = Instant.parse("2026-10-16T12:00:00Z")

    @Test
    fun canonicalRetainedSnapshotKeepsLocalTurnAcrossTrimmingAndRetention() {
        val local = (1..60).map { index ->
            PrivateConversationMessage(
                text = "  message $index ${"x".repeat(if (index == 60) 2_100 else 0)}  ",
                fromMember = index % 2 == 1,
            )
        }
        val persisted = requireNotNull(PrivateConversationPolicy.updated(local, loadedAt))

        assertEquals(
            AssistantConversationHydrationDecision.KEEP_LOCAL,
            AssistantConversationHydrationPolicy.decide(
                hydrated = true,
                previouslyLoaded = PrivateConversationRecord(emptyList(), loadedAt.minusSeconds(1)),
                localMessages = local,
                persisted = persisted,
                expired = false,
            ),
        )
    }

    @Test
    fun identicalTimestampDoesNotHideAnActualExternalTranscriptChange() {
        val local = listOf(PrivateConversationMessage("My current question", fromMember = true))
        val changed = PrivateConversationRecord(
            listOf(PrivateConversationMessage("A different external question", fromMember = true)),
            loadedAt,
        )

        assertEquals(
            AssistantConversationHydrationDecision.RESTORE_PERSISTED,
            AssistantConversationHydrationPolicy.decide(true, changed.copy(messages = local), local, changed, expired = false),
        )
    }

    @Test
    fun expiryAlwaysResetsEvenWhenTranscriptContentMatches() {
        val messages = listOf(PrivateConversationMessage("Current question", fromMember = true))
        val persisted = requireNotNull(PrivateConversationPolicy.updated(messages, loadedAt))

        assertEquals(
            AssistantConversationHydrationDecision.EXPIRED,
            AssistantConversationHydrationPolicy.decide(true, persisted, messages, persisted, expired = true),
        )
    }

    @Test
    fun missingPreviouslyLoadedRecordRestoresButInitialOrClearedEmptyStateIsKept() {
        val welcome = listOf(PrivateConversationMessage("How can I help?", fromMember = false))

        assertEquals(
            AssistantConversationHydrationDecision.RESTORE_PERSISTED,
            AssistantConversationHydrationPolicy.decide(true, PrivateConversationRecord(emptyList(), loadedAt), welcome, null, expired = false),
        )
        assertEquals(
            AssistantConversationHydrationDecision.KEEP_LOCAL,
            AssistantConversationHydrationPolicy.decide(true, null, welcome, null, expired = false),
        )
    }

    @Test
    fun firstHydrationRestoresPersistedTranscript() {
        val persisted = PrivateConversationRecord(
            listOf(PrivateConversationMessage("Existing question", fromMember = true)),
            loadedAt,
        )

        assertEquals(
            AssistantConversationHydrationDecision.RESTORE_PERSISTED,
            AssistantConversationHydrationPolicy.decide(
                hydrated = false,
                previouslyLoaded = null,
                localMessages = persisted.messages,
                persisted = persisted,
                expired = false,
            ),
        )
    }

    @Test
    fun unchangedVaultSnapshotKeepsAnUnpersistedLocalMemberTurn() {
        val previouslyLoaded = PrivateConversationRecord(
            listOf(PrivateConversationMessage("Earlier question", fromMember = true)),
            loadedAt,
        )
        val localWithPendingQuestion = previouslyLoaded.messages +
            PrivateConversationMessage("Current question", fromMember = true)

        assertEquals(
            AssistantConversationHydrationDecision.KEEP_LOCAL,
            AssistantConversationHydrationPolicy.decide(
                hydrated = true,
                previouslyLoaded = previouslyLoaded,
                localMessages = localWithPendingQuestion,
                persisted = previouslyLoaded,
                expired = false,
            ),
        )
    }

    @Test
    fun exactSuccessfulOwnWriteReceiptKeepsNewerLocalTurnAndInFlightRequest() {
        val olderWrite = PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("Earlier local turn", fromMember = true)),
            loadedAt,
        )!!
        val local = olderWrite.messages + PrivateConversationMessage("Newer question", fromMember = true)
        val receipts = AssistantConversationWriteReceipts()
        val generation = receipts.captureGeneration()
        assertTrue(receipts.register(generation, olderWrite, loadedAt))

        assertEquals(
            AssistantConversationHydrationDecision.KEEP_LOCAL,
            AssistantConversationHydrationPolicy.decide(
                hydrated = true,
                previouslyLoaded = null,
                localMessages = local,
                persisted = olderWrite,
                acknowledgedOwnWrite = receipts.recognizes(olderWrite, loadedAt.plusSeconds(1)),
                expired = false,
            ),
        )
    }

    @Test
    fun receiptUsesExactCanonicalTrimmedAndBoundedVaultRecord() {
        val local = (1..55).map { index ->
            PrivateConversationMessage(
                text = "  message $index ${"x".repeat(if (index == 55) 2_100 else 0)}  ",
                fromMember = index % 2 == 1,
            )
        }
        val canonicalRecord = requireNotNull(PrivateConversationPolicy.updated(local, loadedAt))
        val receipts = AssistantConversationWriteReceipts()
        assertTrue(receipts.register(receipts.captureGeneration(), canonicalRecord, loadedAt))

        assertTrue(receipts.recognizes(canonicalRecord.copy(messages = canonicalRecord.messages.map { it.copy() }), loadedAt))
        assertFalse(receipts.recognizes(canonicalRecord.copy(updatedAt = loadedAt.plusSeconds(1)), loadedAt))
        assertFalse(receipts.recognizes(canonicalRecord.copy(messages = canonicalRecord.messages.drop(1)), loadedAt))
        assertEquals(PrivateConversationPolicy.maxMessages, canonicalRecord.messages.size)
        assertTrue(canonicalRecord.messages.all { it.text == it.text.trim() && it.text.length <= PrivateConversationPolicy.maxMessageCharacters })
    }

    @Test
    fun unknownReplacementDeletionAndExpiryDoNotUseOwnWriteReceipt() {
        val saved = requireNotNull(PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("Saved turn", fromMember = true)), loadedAt,
        ))
        val receipts = AssistantConversationWriteReceipts()
        assertTrue(receipts.register(receipts.captureGeneration(), saved, loadedAt))
        val changed = saved.copy(messages = listOf(PrivateConversationMessage("External replacement", fromMember = true)))

        assertFalse(receipts.recognizes(changed, loadedAt))
        assertFalse(receipts.recognizes(null, loadedAt))
        assertEquals(
            AssistantConversationHydrationDecision.RESTORE_PERSISTED,
            AssistantConversationHydrationPolicy.decide(true, saved, saved.messages, changed, expired = false),
        )
        assertEquals(
            AssistantConversationHydrationDecision.RESTORE_PERSISTED,
            AssistantConversationHydrationPolicy.decide(true, saved, saved.messages, null, expired = false),
        )
        assertEquals(
            AssistantConversationHydrationDecision.EXPIRED,
            AssistantConversationHydrationPolicy.decide(
                true, saved, saved.messages, saved,
                acknowledgedOwnWrite = receipts.recognizes(saved, loadedAt.plusSeconds(121)),
                expired = true,
            ),
        )
    }

    @Test
    fun invalidationRejectsLateOldGenerationAndFailedWritesHaveNoReceipt() {
        val record = requireNotNull(PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("Saved turn", fromMember = true)), loadedAt,
        ))
        val receipts = AssistantConversationWriteReceipts()
        val saveGeneration = receipts.captureGeneration()
        receipts.invalidate()

        assertFalse(receipts.register(saveGeneration, record, loadedAt))
        // A failed save never calls register, so its returned/stale-looking record is unknown.
        assertFalse(receipts.recognizes(record, loadedAt))
    }

    @Test
    fun receiptRegistryIsBoundedAndExpiresEntries() {
        val receipts = AssistantConversationWriteReceipts(maxReceipts = 2)
        val records = (1..3).map { index ->
            requireNotNull(PrivateConversationPolicy.updated(
                listOf(PrivateConversationMessage("Turn $index", fromMember = true)), loadedAt.plusSeconds(index.toLong()),
            ))
        }
        val generation = receipts.captureGeneration()
        records.forEach { receipts.register(generation, it, loadedAt) }

        assertFalse(receipts.recognizes(records.first(), loadedAt.plusSeconds(1)))
        assertTrue(receipts.recognizes(records.last(), loadedAt.plusSeconds(1)))
        assertFalse(receipts.recognizes(records.last(), loadedAt.plusSeconds(121)))
    }

    @Test
    fun cancellationAfterVaultCommitStillRegistersReceiptBeforeReturningToCaller() = runBlocking {
        val savedAt = Instant.now()
        val record = requireNotNull(PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("Committed turn", fromMember = true)), savedAt,
        ))
        val receipts = AssistantConversationWriteReceipts()
        val saveReturnedUnlocked = EncryptedVault.Status.Unlocked(
            entries = emptyList(), activeConversation = record,
        )
        val writer = async {
            val callerJob = currentCoroutineContext().job
            receipts.saveSnapshot(emptyList()) {
                callerJob.cancel()
                saveReturnedUnlocked
            }
        }

        try {
            writer.await()
        } catch (_: CancellationException) {
            // The caller may be cancelled after the synchronous vault commit, but its receipt
            // must already exist so a status refresh cannot discard the local transcript.
        }
        assertTrue(receipts.recognizes(record, savedAt.plusSeconds(1)))
    }
}
