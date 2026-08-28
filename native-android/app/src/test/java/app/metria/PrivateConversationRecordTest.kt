package app.metria

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateConversationRecordTest {
    private val now = Instant.parse("2026-08-28T12:00:00Z")

    @Test
    fun `conversation remains active until 24 hours of inactivity`() {
        val record = PrivateConversationRecord(
            listOf(PrivateConversationMessage("How am I doing?", fromMember = true)),
            now.minus(Duration.ofHours(23)).minus(Duration.ofMinutes(59)),
        )

        assertEquals(record, PrivateConversationPolicy.active(record, now))
    }

    @Test
    fun `conversation expires at 24 hours of inactivity`() {
        val record = PrivateConversationRecord(
            listOf(PrivateConversationMessage("How am I doing?", fromMember = true)),
            now.minus(Duration.ofHours(24)),
        )

        assertNull(PrivateConversationPolicy.active(record, now))
    }

    @Test
    fun `conversation keeps only the most recent 50 messages`() {
        val record = PrivateConversationPolicy.updated(
            (1..60).map { index -> PrivateConversationMessage("message $index", fromMember = index % 2 == 0) },
            now,
        )

        assertEquals(50, record?.messages?.size)
        assertEquals("message 11", record?.messages?.first()?.text)
        assertEquals("message 60", record?.messages?.last()?.text)
    }

    @Test
    fun `assistant-only greeting is not retained`() {
        assertNull(PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("How can I help?", fromMember = false)),
            now,
        ))
    }

    @Test
    fun `message text is bounded before storage`() {
        val record = PrivateConversationPolicy.updated(
            listOf(PrivateConversationMessage("x".repeat(3_000), fromMember = true)),
            now,
        )

        assertTrue(record != null)
        assertEquals(PrivateConversationPolicy.maxMessageCharacters, record?.messages?.single()?.text?.length)
    }
}
