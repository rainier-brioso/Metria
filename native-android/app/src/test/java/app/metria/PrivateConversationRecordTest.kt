package app.metria

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class PrivateConversationRecordTest {
    private val now = Instant.parse("2026-08-28T12:00:00Z")

    @Test fun `saved meal receipt link survives encrypted payload message encoding`() {
        val receipt = PrivateConversationMessage("Meal saved", false, savedMealId = "meal-123")
        assertEquals(receipt, PrivateConversationMessageCodec.decode(PrivateConversationMessageCodec.encode(receipt)))
        val updated = PrivateConversationPolicy.updated(listOf(PrivateConversationMessage("meal", true), receipt), now)!!
        assertEquals("meal-123", PrivateConversationPolicy.active(updated, now.plusSeconds(1))!!.messages.last().savedMealId)
    }

    @Test fun `old conversation messages remain readable without link fields`() {
        val old = JSONObject("""{"text":"Old reply","fromMember":false}""")
        assertEquals(PrivateConversationMessage("Old reply", false), PrivateConversationMessageCodec.decode(old))
    }

    @Test fun `member evidence and malformed identifiers cannot become receipt links`() {
        val messages = listOf(
            PrivateConversationMessage("member", true, savedMealId = "meal-123"),
            PrivateConversationMessage("evidence", false, true, "meal-123"),
            PrivateConversationMessage("reply", false, savedMealId = "https://example.com/meal"),
            PrivateConversationMessage("reply", false, savedMealId = "x".repeat(81)),
        )
        assertTrue(PrivateConversationPolicy.updated(messages, now)!!.messages.all { it.savedMealId == null })
    }

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

    @Test fun `linked saved-meal receipt remains active without inventing a member message`() {
        val receipt = PrivateConversationMessage("Meal saved", fromMember = false, savedMealId = "meal-123")

        val decoded = PrivateConversationMessageCodec.decode(PrivateConversationMessageCodec.encode(receipt))
        val record = PrivateConversationPolicy.updated(listOf(decoded), now)!!

        val snapshot = PrivateConversationPolicy.updated(record.messages, record.updatedAt)
        assertEquals(listOf(receipt), PrivateConversationPolicy.active(snapshot, now)?.messages)
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
