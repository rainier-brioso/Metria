package app.metria

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantConversationPersistenceTest {
    private val member = PrivateConversationMessage("Open History", fromMember = true)
    private val reply = PrivateConversationMessage("Opening History.", fromMember = false)

    @Test
    fun navigationWaitsForCompleteTranscriptSaveAndConsumesRequestOnlyOnce() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val transcript = listOf(member, reply)
        var stored = emptyList<PrivateConversationMessage>()
        var navigations = 0

        val result = persistence.persistBeforeNavigation(
            requestId = 41,
            messages = transcript,
            isCurrent = { true },
            save = { stored = it; true },
            navigate = { navigations += 1; assertEquals(transcript, stored); true },
        )

        assertEquals(ConversationNavigationResult.COMPLETED, result)
        assertEquals(transcript, stored)
        assertEquals(1, navigations)
        assertEquals(
            ConversationNavigationResult.DUPLICATE,
            persistence.persistBeforeNavigation(41, transcript, { true }, { true }, { navigations++; true }),
        )
        assertEquals(1, navigations)

        // A delayed LaunchedEffect holding the pre-handoff snapshot cannot overwrite the acked transcript.
        assertEquals(
            ConversationPersistenceResult.SUPERSEDED,
            persistence.persist(listOf(member)) { stored = it; true },
        )
        assertEquals(transcript, stored)
    }

    @Test
    fun failedSaveKeepsChatOpenAndDoesNotNavigate() = runBlocking {
        val persistence = AssistantConversationPersistence()
        var navigated = false

        val result = persistence.persistBeforeNavigation(
            requestId = 2,
            messages = listOf(member, reply),
            isCurrent = { true },
            save = { false },
            navigate = { navigated = true; true },
        )

        assertEquals(ConversationNavigationResult.SAVE_FAILED, result)
        assertFalse(navigated)
    }

    @Test
    fun reviewCanReopenSameDraftButConcurrentTapsCannotNavigateTwice() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        val transcript = listOf(member, reply)
        var reviews = 0
        val first = async {
            persistence.persistBeforeNavigation(42, transcript, { true }, {
                enteredSave.complete(Unit)
                releaseSave.await()
                true
            }, { reviews++; true }, repeatable = true)
        }
        enteredSave.await()
        assertEquals(ConversationNavigationResult.DUPLICATE,
            persistence.persistBeforeNavigation(42, transcript, { true }, { true },
                { reviews++; true }, repeatable = true))
        releaseSave.complete(Unit)
        assertEquals(ConversationNavigationResult.COMPLETED, first.await())
        assertEquals(1, reviews)
        assertEquals(ConversationNavigationResult.COMPLETED,
            persistence.persistBeforeNavigation(42, transcript, { true }, { true },
                { reviews++; true }, repeatable = true))
        assertEquals(2, reviews)
    }

    @Test
    fun clearInvalidatesDebouncedSnapshotBeforeItCanRunAfterClear() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val beforeDebounceTicket = assertNotNullTicket(persistence.captureWriteTicket())
        var stored = listOf(member)

        persistence.invalidatePendingWrites()
        assertEquals(ConversationPersistenceResult.SAVED, persistence.clear { stored = emptyList(); true })
        assertEquals(
            ConversationPersistenceResult.SUPERSEDED,
            persistence.persist(beforeDebounceTicket, listOf(member), { stored = it; true }),
        )
        assertTrue(stored.isEmpty())
    }

    @Test
    fun inFlightWriteIsFollowedByClearAndQueuedOldWriteCannotResurrectTranscript() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        var stored = emptyList<PrivateConversationMessage>()
        val oldTicket = assertNotNullTicket(persistence.captureWriteTicket())
        val firstWrite = async {
            persistence.persist(oldTicket, listOf(member)) {
                enteredSave.complete(Unit)
                releaseSave.await()
                stored = it
                true
            }
        }
        enteredSave.await()
        val queuedTicket = assertNotNullTicket(persistence.captureWriteTicket())
        persistence.invalidatePendingWrites()
        val clear = async { persistence.clear { stored = emptyList(); true } }
        releaseSave.complete(Unit)
        firstWrite.await()
        assertEquals(ConversationPersistenceResult.SAVED, clear.await())
        assertEquals(
            ConversationPersistenceResult.SUPERSEDED,
            persistence.persist(queuedTicket, listOf(member, reply), { stored = it; true }),
        )
        assertTrue(stored.isEmpty())
    }

    @Test
    fun hydrationBarrierHoldsWriterGateThroughSuccessfulWriteAcknowledgement() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        var acknowledgementRegistered = false
        val writer = async {
            persistence.persist(listOf(member)) {
                enteredSave.complete(Unit)
                releaseSave.await()
                acknowledgementRegistered = true
                true
            }
        }
        enteredSave.await()
        val refresh = async { persistence.withWriterIdle { acknowledgementRegistered } }

        assertFalse(refresh.isCompleted)
        releaseSave.complete(Unit)
        assertEquals(ConversationPersistenceResult.SAVED, writer.await())
        assertTrue(refresh.await())
    }

    @Test
    fun restoreForChangedOrRemovedTranscriptFencesQueuedOlderAutosave() = runBlocking {
        val previouslyLoaded = requireNotNull(PrivateConversationPolicy.updated(listOf(member), java.time.Instant.EPOCH))
        val changedRecord = requireNotNull(PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("External replacement", fromMember = true)),
            java.time.Instant.EPOCH.plusSeconds(1),
        ))

        listOf(changedRecord, null).forEach { persisted ->
            val persistence = AssistantConversationPersistence()
            val staleTicket = requireNotNull(persistence.captureWriteTicket())
            var restoredMessages: List<PrivateConversationMessage>? = null
            val decision = persistence.withWriterIdle {
                val result = AssistantConversationHydrationPolicy.decide(
                    hydrated = true,
                    previouslyLoaded = previouslyLoaded,
                    localMessages = listOf(member, reply),
                    persisted = persisted,
                    expired = false,
                )
                assertEquals(AssistantConversationHydrationDecision.RESTORE_PERSISTED, result)
                persistence.invalidatePendingWrites()
                restoredMessages = persisted?.messages
                result
            }
            assertEquals(AssistantConversationHydrationDecision.RESTORE_PERSISTED, decision)
            var oldSnapshotWrote = false
            assertEquals(
                ConversationPersistenceResult.SUPERSEDED,
                persistence.persist(staleTicket, listOf(member, reply)) {
                    oldSnapshotWrote = true
                    true
                },
            )
            assertFalse(oldSnapshotWrote)
            assertEquals(persisted?.messages, restoredMessages)
        }
    }

    @Test
    fun keepingAnExactOwnWriteDoesNotInvalidateCurrentLocalSave() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val savedRecord = requireNotNull(PrivateConversationPolicy.updated(listOf(member), java.time.Instant.EPOCH))
        val receipts = AssistantConversationWriteReceipts()
        assertTrue(receipts.register(receipts.captureGeneration(), savedRecord, java.time.Instant.EPOCH))
        val currentTicket = requireNotNull(persistence.captureWriteTicket())
        val decision = persistence.withWriterIdle {
            AssistantConversationHydrationPolicy.decide(
                hydrated = true,
                previouslyLoaded = null,
                localMessages = listOf(member, reply),
                persisted = savedRecord,
                expired = false,
                acknowledgedOwnWrite = receipts.recognizes(savedRecord, java.time.Instant.EPOCH.plusSeconds(1)),
            )
        }
        assertEquals(AssistantConversationHydrationDecision.KEEP_LOCAL, decision)
        assertEquals(
            ConversationPersistenceResult.SAVED,
            persistence.persist(currentTicket, listOf(member, reply)) { true },
        )
    }

    @Test
    fun lockOrNewRequestDuringSavePreventsLateNavigation() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        var current = true
        var navigated = false
        val handoff = async {
            persistence.persistBeforeNavigation(
                requestId = 8,
                messages = listOf(member, reply),
                isCurrent = { current },
                save = { enteredSave.complete(Unit); releaseSave.await(); true },
                navigate = { navigated = true; true },
            )
        }
        enteredSave.await()
        current = false
        persistence.invalidatePendingWrites()
        releaseSave.complete(Unit)

        assertEquals(ConversationNavigationResult.STALE, handoff.await())
        assertFalse(navigated)
    }

    @Test
    fun ordinaryDebounceCannotCaptureTicketDuringNavigationOrClear() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        val handoff = async {
            persistence.persistBeforeNavigation(
                91, listOf(member, reply), { true },
                { enteredSave.complete(Unit); releaseSave.await(); true }, { true },
            )
        }
        enteredSave.await()
        assertEquals(null, persistence.captureWriteTicket())
        releaseSave.complete(Unit)
        assertEquals(ConversationNavigationResult.COMPLETED, handoff.await())

        val clearEntered = CompletableDeferred<Unit>()
        val releaseClear = CompletableDeferred<Unit>()
        val clear = async {
            persistence.clear { clearEntered.complete(Unit); releaseClear.await(); true }
        }
        clearEntered.await()
        assertEquals(null, persistence.captureWriteTicket())
        releaseClear.complete(Unit)
        assertEquals(ConversationPersistenceResult.SAVED, clear.await())
    }

    @Test
    fun cancelledHandoffNeverNavigatesAndReleasesRequestReservation() = runBlocking {
        val persistence = AssistantConversationPersistence()
        val enteredSave = CompletableDeferred<Unit>()
        var navigated = false
        val handoff = async {
            persistence.persistBeforeNavigation(
                92, listOf(member, reply), { true },
                { enteredSave.complete(Unit); CompletableDeferred<Unit>().await(); true },
                { navigated = true; true },
            )
        }
        enteredSave.await()
        handoff.cancelAndJoin()
        assertFalse(navigated)

        assertEquals(
            ConversationNavigationResult.COMPLETED,
            persistence.persistBeforeNavigation(93, listOf(member, reply), { true }, { true }, { true }),
        )
    }

    @Test
    fun debouncedWritesStayBlockedUntilEveryQueuedClearCompletes() = runBlocking<Unit> {
        val persistence = AssistantConversationPersistence()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()
        val first = async {
            persistence.clear { firstEntered.complete(Unit); releaseFirst.await(); true }
        }
        firstEntered.await()
        val second = async {
            persistence.clear { secondEntered.complete(Unit); releaseSecond.await(); true }
        }
        releaseFirst.complete(Unit)
        secondEntered.await()
        assertEquals(null, persistence.captureWriteTicket())
        releaseSecond.complete(Unit)
        assertEquals(ConversationPersistenceResult.SAVED, first.await())
        assertEquals(ConversationPersistenceResult.SAVED, second.await())
        assertNotNullTicket(persistence.captureWriteTicket())
    }

    private fun assertNotNullTicket(ticket: Long?): Long {
        assertNotNull(ticket)
        return requireNotNull(ticket)
    }
}
