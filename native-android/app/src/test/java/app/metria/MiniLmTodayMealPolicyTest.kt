package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniLmTodayMealPolicyTest {
    @Test
    fun acceptsOnlyNarrowTodayMealListRequests() {
        listOf(
            "Show the meals I recorded today",
            "List my saved meals for today",
            "What meals have I logged today",
            "What did I log for meals today",
            "What did I eat today",
            "Which meals are in my record for today",
            "Show me the food I saved today",
            "Show today's meals",
            "Which meals are recorded today",
        ).forEach { assertTrue(it, MiniLmTodayMealPolicy.isInScope(it)) }
    }

    @Test
    fun rejectsWritesAdviceOtherDatesAndMixedRequests() {
        listOf(
            "I just ate eggs and toast",
            "Log a chicken meal for today",
            "What should I eat tomorrow",
            "Show my meals from yesterday",
            "Show today's meals and suggest dinner",
            "How many calories did I eat today",
            "What did I eat this week",
            "What did I log today",
            "Help me make a meal plan",
            "Add the lunch I just had",
            "Show today's meals and my weight",
            "Show meals for 2026-10-11",
            "Do not show meals today",
            "Show meals today except breakfast",
            "What meals can you record today",
        ).forEach { assertFalse(it, MiniLmTodayMealPolicy.isInScope(it)) }
    }

    @Test
    fun createsOnlyFrozenTodayReadAndNeverOverridesAnotherHostAction() {
        val today = LocalDate.parse("2026-10-11")
        val text = "Show the meals I recorded today"
        val typed = RequiredAgentTool("read_meals", daysAgo = 0)
        val resolved = MiniLmTodayMealPolicy.requiredReadTool(typed, text, null, today)
        assertEquals(today, resolved?.targetDate)
        assertEquals(0, resolved?.daysAgo)
        assertNull(MiniLmTodayMealPolicy.requiredReadTool(RequiredAgentTool("read_weight"), text, null, today))
        assertNull(MiniLmTodayMealPolicy.requiredReadTool(null, text, "selected-meal", today))
        assertNull(MiniLmTodayMealPolicy.requiredReadTool(null, "Show today's meals and my weight", null, today))
    }

    @Test
    fun frozenHostDateMustMatchTheCompleteValidatedMealResponse() {
        val date = LocalDate.parse("2026-10-11")
        val call = PrivateCoachToolCall(intent = "read_meals", daysAgo = 0, hostReadDate = date)
        val response = mapOf<String, Any?>(
            "source" to "metria_local_vault",
            "reply_language" to "English",
            "as_of" to date.toString(),
            "from" to date.toString(),
            "to" to date.toString(),
            "meal_slot" to null,
            "total_count" to 0,
            "returned_count" to 0,
            "omitted_count" to 0,
            "coverage" to "full",
            "meals" to emptyList<Any>(),
        )
        assertTrue(MiniLmTodayMealPolicy.hasValidFrozenMealRead(call, response))
        assertFalse(MiniLmTodayMealPolicy.hasValidFrozenMealRead(call.copy(hostReadDate = null), response))
        assertFalse(MiniLmTodayMealPolicy.hasValidFrozenMealRead(call.copy(hostReadDate = date.plusDays(1)), response))
        assertFalse(MiniLmTodayMealPolicy.hasValidFrozenMealRead(call, response + ("from" to date.minusDays(1).toString())))
        assertFalse(MiniLmTodayMealPolicy.hasValidFrozenMealRead(call, response + ("as_of" to date.plusDays(1).toString())))
    }
}
