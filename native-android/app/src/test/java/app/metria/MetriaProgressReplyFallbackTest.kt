package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetriaProgressReplyFallbackTest {
    private fun response(language: String = "English", period: String = "week"): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault",
        "period" to if (period == "week") "last_7_days_including_today" else "today",
        "from" to if (period == "week") "2026-09-24" else "2026-09-30",
        "to" to "2026-09-30",
        "reply_language" to language,
        "coverage" to mapOf("period_days" to if (period == "week") 7 else 1,
            "days_with_recorded_meals" to 1, "interpretation" to "recorded_meals_only_not_total_intake"),
        "meals" to mapOf("recorded_count" to 3, "complete_nutrition_count" to 1,
            "partial_estimate_count" to 1, "missing_nutrition_count" to 1),
        "nutrition" to mapOf("calories_kcal" to 820.0, "protein_g" to 31.5),
        "weight" to mapOf("entry_count" to 0),
    )

    @Test fun `bilingual overview recovers from rejected prose without changing its evidence`() {
        for (language in listOf("English", "Spanish")) {
            val data = response(language)
            val evidence = MetriaAgentFactEvidence.from("read_progress_overview", data)
            assertNull(MetriaAgentFactAuditor.audit("You recorded 999 meals.", evidence).acceptedText)
            val reply = MetriaProgressReplyFallback.from("read_progress_overview", RequiredAgentTool("read_progress_overview", "week"), data)
            assertNotNull(reply)
            assertEquals(reply, MetriaAgentFactAuditor.audit(reply!!, evidence).acceptedText)
            assertTrue(reply.contains("820 kcal"))
            assertTrue(reply.contains("31.5 g"))
            assertTrue(reply.contains(if (language == "Spanish") "estimaciones parciales" else "partial estimates"))
            assertTrue(reply.contains(if (language == "Spanish") "no tienen estimación" else "no nutrition estimate"))
            assertTrue(reply.contains(if (language == "Spanish") "toda tu ingesta" else "total intake"))
            assertFalse(reply.contains("999"))
        }
    }

    @Test fun `today selected by native model can recover without host required tool`() {
        val data = response(period = "today")
        val reply = MetriaProgressReplyFallback.from("read_progress_overview", null, data)
        assertNotNull(reply)
        assertTrue(reply!!.startsWith("Today"))
        assertEquals(reply, MetriaAgentFactAuditor.audit(reply, MetriaAgentFactEvidence.from("read_progress_overview", data)).acceptedText)
    }

    @Test fun `empty overview describes missing estimates without inventing zero intake`() {
        val data = response() + mapOf(
            "coverage" to mapOf("period_days" to 7, "days_with_recorded_meals" to 0,
                "interpretation" to "recorded_meals_only_not_total_intake"),
            "meals" to mapOf("recorded_count" to 0, "complete_nutrition_count" to 0,
                "partial_estimate_count" to 0, "missing_nutrition_count" to 0),
            "nutrition" to mapOf("calories_kcal" to null, "protein_g" to null),
        )
        val reply = MetriaProgressReplyFallback.from("read_progress_overview", null, data)
        assertNotNull(reply)
        assertTrue(reply!!.contains("No nutrition estimates"))
        assertFalse(reply.contains("0 kcal"))
    }

    @Test fun `wrong tool period source and date range cannot become fallback evidence`() {
        val data = response()
        assertNull(MetriaProgressReplyFallback.from("read_weight", null, data))
        assertNull(MetriaProgressReplyFallback.from("read_progress_overview", RequiredAgentTool("read_progress_overview", "today"), data))
        assertNull(MetriaProgressReplyFallback.from("read_progress_overview", RequiredAgentTool("read_weight"), data))
        for (invalid in listOf(data + ("source" to "chat_history"), data + ("from" to "2026-09-01"),
            data + ("reply_language" to "unknown"))) {
            assertNull(MetriaProgressReplyFallback.from("read_progress_overview", null, invalid))
        }
    }

    @Test fun `malformed totals coverage and nonfinite values fail closed`() {
        val data = response()
        val invalid = listOf(
            data + ("meals" to mapOf("recorded_count" to 3, "complete_nutrition_count" to 3,
                "partial_estimate_count" to 1, "missing_nutrition_count" to 0)),
            data + ("nutrition" to mapOf("calories_kcal" to Double.NaN, "protein_g" to 31.5)),
            data + ("nutrition" to mapOf("calories_kcal" to -10, "protein_g" to 31.5)),
            data + ("nutrition" to mapOf("calories_kcal" to null, "protein_g" to null)),
            data + ("weight" to mapOf("entry_count" to 0.5)),
        )
        invalid.forEach { assertNull(MetriaProgressReplyFallback.from("read_progress_overview", null, it)) }
    }
}
