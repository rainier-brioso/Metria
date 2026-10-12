package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GoalRemainingNutritionTest {
    private val today = LocalDate.of(2026, 10, 12)

    @Test fun `under goal returns deterministic remaining amount from displayed values`() {
        val result = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 42.26)), NutritionTargets(proteinGrams = 100.04), today, "protein",
        )
        val nutrition = result["nutrition"] as Map<*, *>
        assertEquals(42.3, nutrition["actual"])
        assertEquals(100.0, nutrition["goal"])
        assertEquals(57.7, nutrition["remaining"])
        assertEquals("under_goal", nutrition["comparison"])
        assertEquals("today", result["period"])
        assertEquals(today.toString(), result["from"])
        assertEquals(today.toString(), result["to"])
        assertEquals("g", GoalRemainingNutritionResult.proof(result, today, "protein")?.unit)
        assertNotNullReply(result, "protein")
    }

    @Test fun `missing goal or estimates are explicit and never treated as zero`() {
        val noGoal = GoalRemainingNutritionResult.from(listOf(meal(today, protein = 30.0)), null, today, "protein")
        assertEquals("no_goal", (noGoal["nutrition"] as Map<*, *>)["comparison"])
        assertNull((noGoal["nutrition"] as Map<*, *>)["remaining"])
        assertTrue(GoalRemainingNutritionReply.from(noGoal, today, "protein")!!.contains("no confirmed daily protein goal"))

        val noEstimate = GoalRemainingNutritionResult.from(emptyList(), NutritionTargets(proteinGrams = 100.0), today, "protein")
        assertEquals("no_estimate", (noEstimate["nutrition"] as Map<*, *>)["comparison"])
        assertNull((noEstimate["nutrition"] as Map<*, *>)["actual"])
        assertNull(GoalRemainingNutritionResult.proof(noEstimate, today, "carbs"))
        assertTrue(GoalRemainingNutritionReply.from(noEstimate, today, "protein")!!.contains("No protein estimate"))
    }

    @Test fun `above goal is neutral and reports only amount above`() {
        val result = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 120.0)), NutritionTargets(proteinGrams = 100.0), today, "protein",
        )
        val nutrition = result["nutrition"] as Map<*, *>
        assertEquals("over_goal", nutrition["comparison"])
        assertEquals(20.0, nutrition["over_by"])
        assertNull(nutrition["remaining"])
        val reply = GoalRemainingNutritionReply.from(result, today, "protein")!!
        assertTrue(reply.contains("20 g protein above"))
        assertFalse(reply.contains("should"))
    }

    @Test fun `partial and missing meal coverage are disclosed while available estimates are used`() {
        val result = GoalRemainingNutritionResult.from(
            listOf(
                meal(today, protein = 30.0, coverage = "full"),
                meal(today, protein = 10.0, coverage = "partial"),
                meal(today, protein = null),
            ),
            NutritionTargets(proteinGrams = 100.0), today, "protein",
        )
        val meals = result["meals"] as Map<*, *>
        assertEquals(3, meals["recorded_count"])
        assertEquals(1, meals["complete_nutrition_count"])
        assertEquals(1, meals["partial_estimate_count"])
        assertEquals(1, meals["missing_nutrition_count"])
        val reply = GoalRemainingNutritionReply.from(result, today, "protein")!!
        assertTrue(reply.contains("partial nutrition estimates"))
        assertTrue(reply.contains("no nutrition estimate"))
        assertTrue(reply.contains("Unrecorded meals are not included"))
    }

    @Test fun `empty day does not claim a zero estimate`() {
        val result = GoalRemainingNutritionResult.from(emptyList(), null, today, "calories")
        val nutrition = result["nutrition"] as Map<*, *>
        assertNull(nutrition["actual"])
        assertEquals("no_estimate", nutrition["comparison"])
        assertNull(GoalRemainingNutritionResult.proof(result, today, "protein"))
    }

    @Test fun `proof rejects wrong date metric count provenance and arithmetic`() {
        val valid = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 30.0)), NutritionTargets(proteinGrams = 100.0), today, "protein",
        )
        assertNull(GoalRemainingNutritionResult.proof(valid, today.plusDays(1), "protein"))
        assertNull(GoalRemainingNutritionResult.proof(valid, today, "calories"))
        assertNull(GoalRemainingNutritionResult.proof(valid + ("unrecognized" to "value"), today, "protein"))
        assertNull(GoalRemainingNutritionResult.proof(valid + ("source" to "other"), today, "protein"))
        assertNull(GoalRemainingNutritionResult.proof(valid + ("from" to today.minusDays(1).toString()), today, "protein"))
        assertNull(GoalRemainingNutritionResult.proof(mutated(valid, "remaining", 69.0), today, "protein"))
        assertNull(GoalRemainingNutritionResult.proof(mutated(valid, "comparison", "over_goal"), today, "protein"))
        val badCounts = valid.toMutableMap()
        badCounts["meals"] = (valid["meals"] as Map<*, *>).toMutableMap().apply { put("missing_nutrition_count", 1) }
        assertNull(GoalRemainingNutritionResult.proof(badCounts, today, "protein"))
        assertNull(GoalRemainingNutritionResult.proof(mutated(valid, "remaining", Double.NaN), today, "protein"))
    }

    @Test fun `proof applies confirmed goal bounds even when forged arithmetic is consistent`() {
        val protein = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 30.0)), NutritionTargets(proteinGrams = 100.0), today, "protein",
        )
        assertNull(GoalRemainingNutritionResult.proof(
            withNutrition(protein, mapOf("goal" to 0.0, "comparison" to "over_goal", "remaining" to null, "over_by" to 30.0)),
            today,
            "protein",
        ))
        assertNull(GoalRemainingNutritionResult.proof(
            withNutrition(protein, mapOf("goal" to 1_001.0, "comparison" to "under_goal", "remaining" to 971.0)),
            today,
            "protein",
        ))

        val calories = GoalRemainingNutritionResult.from(
            listOf(meal(today, calories = 1_500.0)), NutritionTargets(caloriesKcal = 1_000.0), today, "calories",
        )
        assertNull(GoalRemainingNutritionResult.proof(
            withNutrition(calories, mapOf("goal" to 10_001.0, "comparison" to "under_goal", "remaining" to 8_501.0, "over_by" to null)),
            today,
            "calories",
        ))

        val largeRecordedAggregate = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 6_000.0), meal(today, protein = 6_000.0)),
            NutritionTargets(proteinGrams = 1_000.0), today, "protein",
        )
        assertEquals("over_goal", (largeRecordedAggregate["nutrition"] as Map<*, *>)["comparison"])
        assertTrue(GoalRemainingNutritionResult.proof(largeRecordedAggregate, today, "protein") != null)
    }

    @Test fun `typed auditor binds remaining and above amounts to their role and metric`() {
        val under = GoalRemainingNutritionResult.from(
            listOf(meal(today, protein = 30.0)), NutritionTargets(proteinGrams = 100.0), today, "protein",
        )
        val text = GoalRemainingNutritionReply.from(under, today, "protein")!!
        val evidence = MetriaAgentFactEvidence.from("read_goal_remaining_nutrition", under)
        assertEquals(text, MetriaAgentFactAuditor.audit(text, evidence).acceptedText)
        assertTrue(MetriaAgentFactAuditor.audit(text.replace("70 g", "71 g"), evidence).retryable)
        assertTrue(MetriaAgentFactAuditor.audit(text.replace("protein", "calories"), evidence).retryable)

        val over = GoalRemainingNutritionResult.from(
            listOf(meal(today, calories = 1_500.0)), NutritionTargets(caloriesKcal = 1_000.0), today, "calories",
        )
        val overText = GoalRemainingNutritionReply.from(over, today, "calories")!!
        val overEvidence = MetriaAgentFactEvidence.from("read_goal_remaining_nutrition", over)
        assertEquals(overText, MetriaAgentFactAuditor.audit(overText, overEvidence).acceptedText)
        assertTrue(MetriaAgentFactAuditor.audit(overText.replace("500 kcal", "400 kcal"), overEvidence).retryable)
        assertEquals("About 70 g protein remain today.",
            MetriaAgentFactAuditor.audit("About 70 g protein remain today.", evidence).acceptedText)
        assertTrue(MetriaAgentFactAuditor.audit("About 70 g protein are above today's goal.", evidence).retryable)
    }

    @Test fun `generated protein and calorie replies pass typed audit for all result states`() {
        val cases = listOf(
            GoalRemainingNutritionResult.from(listOf(meal(today, protein = 30.0)), NutritionTargets(proteinGrams = 100.0), today, "protein") to "protein",
            GoalRemainingNutritionResult.from(listOf(meal(today, calories = 800.0)), NutritionTargets(caloriesKcal = 1_000.0), today, "calories") to "calories",
            GoalRemainingNutritionResult.from(listOf(meal(today, calories = 1_500.0)), NutritionTargets(caloriesKcal = 1_000.0), today, "calories") to "calories",
            GoalRemainingNutritionResult.from(listOf(meal(today, protein = 30.0)), null, today, "protein") to "protein",
            GoalRemainingNutritionResult.from(emptyList(), NutritionTargets(caloriesKcal = 1_000.0), today, "calories") to "calories",
            GoalRemainingNutritionResult.from(listOf(meal(today, protein = 100.0)), NutritionTargets(proteinGrams = 100.0), today, "protein") to "protein",
            GoalRemainingNutritionResult.from(listOf(meal(today, calories = 1_000.0)), NutritionTargets(caloriesKcal = 1_000.0), today, "calories") to "calories",
        )
        cases.forEach { (response, metric) ->
            val reply = GoalRemainingNutritionReply.from(response, today, metric)
            assertTrue("reply for $metric must be generated", reply != null)
            val evidence = MetriaAgentFactEvidence.from("read_goal_remaining_nutrition", response)
            assertEquals(reply, MetriaAgentFactAuditor.audit(reply!!, evidence).acceptedText)
        }
        assertTrue(GoalRemainingNutritionReply.from(cases[0].first, today, "protein")!!.contains("70 g protein remain"))
        assertTrue(GoalRemainingNutritionReply.from(cases[1].first, today, "calories")!!.contains("200 kcal remain"))
        assertTrue(GoalRemainingNutritionReply.from(cases[2].first, today, "calories")!!.contains("500 kcal above"))
        assertTrue(GoalRemainingNutritionReply.from(cases[3].first, today, "protein")!!.contains("no confirmed daily protein goal"))
        assertTrue(GoalRemainingNutritionReply.from(cases[4].first, today, "calories")!!.contains("No calories estimate"))
        assertTrue(GoalRemainingNutritionReply.from(cases[5].first, today, "protein")!!.contains("at the confirmed daily goal"))
        assertTrue(GoalRemainingNutritionReply.from(cases[6].first, today, "calories")!!.contains("at the confirmed daily goal"))
    }

    private fun assertNotNullReply(response: Map<String, Any?>, metric: String) {
        assertTrue(GoalRemainingNutritionReply.from(response, today, metric)!!.contains("not a measure of total intake"))
    }

    private fun mutated(response: Map<String, Any?>, key: String, value: Any?): Map<String, Any?> =
        withNutrition(response, mapOf(key to value))

    private fun withNutrition(response: Map<String, Any?>, updates: Map<String, Any?>): Map<String, Any?> =
        response.toMutableMap().apply {
            put("nutrition", (response["nutrition"] as Map<*, *>).toMutableMap().apply { putAll(updates) })
        }

    private fun meal(
        date: LocalDate,
        protein: Double? = 20.0,
        calories: Double? = 300.0,
        coverage: String = "full",
    ) = MealEntry(
        recordedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        items = listOf(MealEntryItem(name = "test food", grams = null, preparation = null)),
        assumptions = emptyList(),
        confidence = "high",
        source = "manual_entry",
        nutrition = if (protein == null && calories == null) null else MealNutrition(
            caloriesKcal = calories,
            proteinGrams = protein,
            carbohydrateGrams = 20.0,
            fatGrams = 10.0,
            estimateCoverage = coverage,
        ),
    )
}
