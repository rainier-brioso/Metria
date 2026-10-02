package app.metria

import org.junit.Assert.*
import org.junit.Test

class MetriaMealRecordReplyTest {
    private val required = RequiredAgentTool("read_meals", daysAgo = 0)
    private fun payload(titles: List<String> = listOf("Lentil soup", "Roasted vegetables")): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault", "reply_language" to "Spanish", "as_of" to "2026-10-02",
        "from" to "2026-10-02", "to" to "2026-10-02", "meal_slot" to null,
        "total_count" to titles.size, "returned_count" to titles.size, "omitted_count" to 0, "coverage" to "full",
        "meals" to titles.map { mapOf("date" to "2026-10-02", "title" to it, "slot" to null) },
    )

    @Test fun `explicit meal question gets meal names instead of unrelated policy prose`() {
        val reply = MetriaMealRecordReply.from(required, payload())!!
        assertTrue(reply.startsWith("Comidas registradas el 2026-10-02:"))
        assertTrue(reply.contains("- Lentil soup"))
        assertTrue(reply.contains("- Roasted vegetables"))
        assertFalse(reply.contains("weight"))
        assertFalse(reply.contains("tool"))
        assertEquals(reply, MetriaAgentFactAuditor.audit(reply, MetriaAgentFactEvidence.from("read_meals", payload())).acceptedText)
    }

    @Test fun `empty exact day and English result remain grounded`() {
        assertTrue(MetriaMealRecordReply.from(required, payload(emptyList()))!!.startsWith("No hay comidas registradas"))
        assertTrue(MetriaMealRecordReply.from(required, payload() + ("reply_language" to "English"))!!.startsWith("Meals recorded"))
        val yesterday = payload() + mapOf("as_of" to "2026-10-03")
        assertNotNull(MetriaMealRecordReply.from(required.copy(daysAgo = 1), yesterday))
    }

    @Test fun `wrong source date tool and inconsistent collections fail closed`() {
        listOf(mapOf("source" to "chat"), mapOf("to" to "2026-10-01"), mapOf("total_count" to 3),
            mapOf("returned_count" to 2.5), mapOf("coverage" to "latest_records_only"),
            mapOf("reply_language" to "unknown"), mapOf("meal_slot" to "lunch"),
            mapOf("meals" to listOf(mapOf("date" to "2026-10-01", "title" to "Lentil soup")))
        ).forEach { assertNull(MetriaMealRecordReply.from(required, payload() + it)) }
        assertNull(MetriaMealRecordReply.from(required.copy(name = "read_nutrition_summary"), payload()))
        assertNull(MetriaMealRecordReply.from(required.copy(targetDate = java.time.LocalDate.of(2026, 10, 1)), payload()))
    }

    @Test fun `bounded meal list visibly qualifies omissions and retains equal titled records`() {
        val full = payload(List(MetriaAgentResultBounds.maxRecords) { "Lentil soup" })
        val partial = full + mapOf("total_count" to 25, "omitted_count" to 1, "coverage" to "latest_records_only")
        val reply = MetriaMealRecordReply.from(required, partial)!!
        assertTrue(reply.contains("Lista parcial"))
        assertEquals(24, reply.lineSequence().count { it == "- Lentil soup" })
    }
}
