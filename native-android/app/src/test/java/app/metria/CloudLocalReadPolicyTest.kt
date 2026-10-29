package app.metria

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CloudLocalReadPolicyTest {
    @Test
    fun latestMealCloudSelectionCarriesTheFrozenDateIntoLocalReplyValidation() {
        val today = LocalDate.parse("2026-10-28")
        val question = "How many calories were in my latest meal?"
        val host = MetriaAgentGroundingPolicy.requiredTool(question, emptyList(), today, null)
        assertEquals("read_latest_meal", host?.name)
        assertNull(host?.targetDate)
        val required = CloudLocalReadPolicy.resolve(host, question, null, today)!!
        assertTrue(required.accepts(PrivateCoachToolCall("read_latest_meal")))
        assertEquals(today, required.targetDate)
        val meal = MealEntry(
            recordedAt = today.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
            title = "Fictional soup", items = emptyList(), assumptions = emptyList(), confidence = "high",
            source = "manual_entry", recordedTimeKnown = true,
            nutrition = MealNutrition(350.0, 14.0, null, null, estimateCoverage = "full"),
        )
        val response = LatestMealNutritionResult.from(listOf(meal), today, "English")
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(host!!, response))
        assertNotNull(MiniLmMealReadPolicy.validatedFastReadReply(required, response))
        assertTrue(MiniLmMealReadPolicy.validatedFastReadReply(required, response)!!.contains("350"))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(
            required, response + ("as_of" to today.minusDays(1).toString()),
        ))
        assertNull(MiniLmMealReadPolicy.validatedFastReadReply(
            required, response + ("source" to "untrusted"),
        ))
    }

    @Test
    fun dateAndScopeMatchTheExistingLocalReadResolver() {
        val today = LocalDate.parse("2026-10-28")
        for (question in listOf("What did I eat today?", "What did I eat yesterday?", "How many calories did I eat today?")) {
            val host = MetriaAgentGroundingPolicy.requiredTool(question, emptyList(), today, null)
            val local = MiniLmMealReadPolicy.requiredReadTool(host, question, null, today)
            assertNotNull(question, local)
            assertEquals(local, CloudLocalReadPolicy.resolve(host, question, null, today))
        }
        val selected = RequiredAgentTool("read_selected_meal", selectedMealId = "selected")
        assertEquals(selected, CloudLocalReadPolicy.resolve(selected, "Show this meal", "selected", today))
        assertNull(CloudLocalReadPolicy.resolve(null, "Hello", null, today))
    }
}
