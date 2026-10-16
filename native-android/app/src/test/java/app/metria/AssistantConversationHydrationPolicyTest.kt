package app.metria

import java.time.Instant
import org.junit.Assert.assertEquals
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
}
