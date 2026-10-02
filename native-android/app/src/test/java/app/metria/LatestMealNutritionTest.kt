package app.metria

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class LatestMealNutritionTest {
    private val today = LocalDate.of(2026, 10, 2)
    private fun meal(day: LocalDate, hour: Int, calories: Double? = 412.345, protein: Double? = 19.876,
        coverage: String = "full", knownTime: Boolean = true) = MealEntry(
        recordedAt = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant(),
        title = "Lentil soup", items = listOf(MealEntryItem("Lentils", 100, null)),
        assumptions = emptyList(), confidence = "high", source = "manual_entry",
        recordedTimeKnown = knownTime,
        nutrition = MealNutrition(calories, protein, null, null, estimateCoverage = coverage),
    )

    @Test fun `latest meal selects one fresh record rather than day totals and rounds display only`() {
        val newest = meal(today.minusDays(1), 20)
        val result = LatestMealNutritionResult.from(listOf(newest, meal(today.minusDays(2), 22, 999.0)), today, "Spanish")
        val reply = LatestMealNutritionReply.from(result)!!
        assertTrue(reply.contains("2026-10-01"))
        assertTrue(reply.contains("412 kcal"))
        assertTrue(reply.contains("19.9 g"))
        assertFalse(reply.contains("999"))
        assertEquals(412.345, newest.nutrition!!.caloriesKcal!!, 0.0)
        val evidence = MetriaAgentFactEvidence.from("read_latest_meal", result)
        assertNull(MetriaAgentFactAuditor.audit("Your recorded meal has 19.9 kcal and 412 g protein.", evidence).acceptedText)
    }

    @Test fun `actual member question yields supported Spanish language and a validated meal answer`() {
        listOf("cuantas calorías tiene mi última comida?", "cuantas calorias tiene mi ultima comida?").forEach { question ->
            val language = coachReplyLanguage(question)
            assertEquals("Spanish", language)
            val result = LatestMealNutritionResult.from(listOf(meal(today, 12)), today, language)
            assertTrue(LatestMealNutritionReply.from(result)!!.startsWith("Tu última comida registrada:"))
        }
        val language = coachReplyLanguage("How many calories are in my last meal?", "Member: ¿Cómo voy esta semana?")
        assertEquals("English", language)
        assertNotNull(LatestMealNutritionReply.from(LatestMealNutritionResult.from(listOf(meal(today, 12)), today, language)))
    }

    @Test fun `empty old and future records do not become latest meals`() {
        val result = LatestMealNutritionResult.from(listOf(meal(today.minusDays(91), 12), meal(today.plusDays(1), 12)), today, "English")
        assertEquals("empty", result["status"])
        assertTrue(LatestMealNutritionReply.from(result)!!.contains("No meals"))
    }

    @Test fun `partial missing and absent metric estimates remain explicit`() {
        val partial = LatestMealNutritionReply.from(LatestMealNutritionResult.from(listOf(meal(today, 12, coverage = "partial")), today, "Spanish"))!!
        assertTrue(partial.contains("Estimación parcial"))
        val missing = LatestMealNutritionReply.from(LatestMealNutritionResult.from(listOf(meal(today, 12).copy(nutrition = null)), today, "English"))!!
        assertTrue(missing.contains("No saved calorie estimate"))
        assertFalse(missing.contains("0 kcal"))
        val proteinOnly = LatestMealNutritionReply.from(LatestMealNutritionResult.from(listOf(meal(today, 12, calories = null)), today, "English"))!!
        assertTrue(proteinOnly.contains("No saved calorie estimate"))
        assertTrue(proteinOnly.contains("19.9 g"))
    }

    @Test fun `unknown times and tied records request clarification instead of guessing`() {
        listOf(listOf(meal(today, 12), meal(today, 12)),
            listOf(meal(today, 12), meal(today, 15, knownTime = false))).forEach { meals ->
            val result = LatestMealNutritionResult.from(meals, today, "English")
            assertEquals("ambiguous", result["status"])
            assertNull(result["meal"])
            assertTrue(LatestMealNutritionReply.from(result)!!.contains("do not identify"))
        }
    }

    @Test fun `malformed latest meal results fail closed`() {
        val result = LatestMealNutritionResult.from(listOf(meal(today, 12)), today, "Spanish")
        listOf(mapOf("source" to "chat"), mapOf("period" to "today"), mapOf("to" to "2026-10-01"),
            mapOf("reply_language" to "unknown"), mapOf("estimate_coverage" to "invalid"),
            mapOf("meal" to mapOf("date" to "2026-10-03", "title" to "Soup")),
            mapOf("nutrition" to mapOf("calories_kcal" to Double.NaN)),
            mapOf("status" to "empty")).forEach { assertNull(LatestMealNutritionReply.from(result + it)) }
    }

    @Test fun `bilingual last meal questions require no argument tool and advice stays separate`() {
        listOf("cuantas calorías tiene mi ultima comida?", "How much protein is in my last meal?").forEach {
            assertEquals(RequiredAgentTool("read_latest_meal"), MetriaAgentGroundingPolicy.requiredTool(it, today = today))
        }
        assertNull(MetriaAgentGroundingPolicy.requiredTool("How much protein should my last meal contain?", today = today))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("How much protein does rice have?", today = today))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("Log my last meal: rice, 500 calories", today = today))
        assertEquals("read_latest_meal", MetriaNativeToolCallAdapter.adapt("read_latest_meal", emptyMap<String, Any>())!!.intent)
        assertNull(MetriaNativeToolCallAdapter.adapt("read_latest_meal", mapOf("save" to true)))
        assertEquals(AssistantAction.QueryLatestMeal, AssistantActionDispatcher.resolve(PrivateCoachToolCall("read_latest_meal")))
        assertFalse(RequiredAgentTool("read_latest_meal").accepts(PrivateCoachToolCall("read_nutrition_summary", period = "today")))
    }
}
