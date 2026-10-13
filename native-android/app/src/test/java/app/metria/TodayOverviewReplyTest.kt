package app.metria

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayOverviewReplyTest {
    private val date = LocalDate.of(2026, 10, 12)

    private fun required(
        targetDate: LocalDate? = date,
        name: String = "read_progress_overview",
        period: String? = "today",
        metric: String? = null,
    ) = RequiredAgentTool(name = name, period = period, targetDate = targetDate, metric = metric)

    private fun response(
        recorded: Int = 2,
        partial: Int = 0,
        missing: Int = 0,
        calories: Any? = 720.0,
        protein: Any? = 42.5,
        calorieGoal: Any? = 2_000.0,
        proteinGoal: Any? = 100.0,
        summary: String = "Untrusted summary must never be shown.",
    ): Map<String, Any?> = mapOf(
        "source" to "metria_local_vault",
        "period" to "today",
        "as_of" to date.toString(),
        "from" to date.toString(),
        "to" to date.toString(),
        "reply_language" to "English",
        "coverage" to mapOf(
            "period_days" to 1,
            "days_with_recorded_meals" to if (recorded == 0) 0 else 1,
            "interpretation" to "recorded_meals_only_not_total_intake",
        ),
        "meals" to mapOf(
            "recorded_count" to recorded,
            "complete_nutrition_count" to (recorded - partial - missing).coerceAtLeast(0),
            "partial_estimate_count" to partial,
            "missing_nutrition_count" to missing,
        ),
        "nutrition" to mapOf(
            "calories_kcal" to calories,
            "protein_g" to protein,
            "calorie_goal_kcal" to calorieGoal,
            "protein_goal_g" to proteinGoal,
        ),
        // Deliberately distinctive synthetic weight values; the member-facing summary omits them.
        "weight" to mapOf("entry_count" to 1, "first_kg" to 91.7, "latest_kg" to 91.7, "change_kg" to null),
        "summary" to summary,
    )

    @Test fun `today summary includes estimates and separately bound daily goals without weight values`() {
        val data = response()
        val reply = TodayOverviewReply.from(required(), data)
        assertEquals(
            "Today you recorded 2 meals. Available calorie estimates today total 720 kcal. " +
                "Available protein estimates today total 42.5 g protein. Your daily calorie goal is 2000 kcal. " +
                "Your daily protein goal is 100 g protein. Recorded meals may not represent your total intake.",
            reply,
        )
        val evidence = MetriaAgentFactEvidence.from("read_progress_overview", data)
        assertEquals(reply, MetriaAgentFactAuditor.audit(requireNotNull(reply), evidence).acceptedText)
        assertFalse(reply!!.contains("91.7"))
        assertFalse(reply.contains("Untrusted summary"))
    }

    @Test fun `missing goals and estimates are stated without inventing zeros`() {
        val reply = TodayOverviewReply.from(required(), response(
            missing = 2, calories = null, protein = null, calorieGoal = null, proteinGoal = null,
        ))
        assertNotNull(reply)
        assertTrue(reply!!.contains("No calorie or protein estimates are available today."))
        assertTrue(reply.contains("No daily calorie goal is set."))
        assertTrue(reply.contains("No daily protein goal is set."))
        assertFalse(reply.contains("0 kcal"))
    }

    @Test fun `empty day is explicit and does not claim total intake is zero`() {
        val reply = TodayOverviewReply.from(required(), response(
            recorded = 0, calories = null, protein = null,
        ))
        assertNotNull(reply)
        assertTrue(reply!!.startsWith("Today you recorded 0 meals."))
        assertTrue(reply.contains("No calorie or protein estimates"))
        assertTrue(reply.contains("may not represent your total intake"))
        assertFalse(Regex("(?<![0-9])0 kcal\\b").containsMatchIn(reply))
    }

    @Test fun `partial and missing estimate coverage is disclosed`() {
        val reply = TodayOverviewReply.from(required(), response(partial = 1, missing = 1))
        assertNotNull(reply)
        assertTrue(reply!!.contains("Some recorded meals have partial nutrition estimates."))
        assertTrue(reply.contains("Some recorded meals have no nutrition estimate."))
    }

    @Test fun `tool scope and date must be trusted today overview only`() {
        val data = response()
        assertNull(TodayOverviewReply.from(required(targetDate = null), data))
        assertNull(TodayOverviewReply.from(required(name = "read_weight"), data))
        assertNull(TodayOverviewReply.from(required(period = "week"), data))
        assertNull(TodayOverviewReply.from(required(metric = "protein"), data))
        assertNull(TodayOverviewReply.from(required().copy(query = "food"), data))
        assertNull(TodayOverviewReply.from(required().copy(daysAgo = 0), data))
        assertNull(TodayOverviewReply.from(required().copy(fromDaysAgo = 1), data))
        assertNull(TodayOverviewReply.from(required().copy(toDaysAgo = 1), data))
        assertNull(TodayOverviewReply.from(required().copy(selectedMealId = "id"), data))
        assertNull(TodayOverviewReply.from(required().copy(selectedMealQuestion = "question"), data))
    }

    @Test fun `response source language and every trusted date are checked`() {
        val data = response()
        val invalid = listOf(
            data + ("source" to "chat_history"),
            data + ("reply_language" to "Spanish"),
            data + ("as_of" to "2026-10-11"),
            data + ("from" to "2026-10-11"),
            data + ("to" to "2026-10-11"),
            data + ("period" to "week"),
            data + ("requested_metric" to "calories"),
        )
        invalid.forEach { assertNull(TodayOverviewReply.from(required(), it)) }
    }

    @Test fun `malformed counts and nonfinite or unrounded nutrition fail closed`() {
        val data = response()
        val invalid = listOf(
            data + ("meals" to mapOf("recorded_count" to 2, "complete_nutrition_count" to 2,
                "partial_estimate_count" to 1, "missing_nutrition_count" to 0)),
            data + ("nutrition" to ((data["nutrition"] as Map<*, *>) + ("calories_kcal" to Double.POSITIVE_INFINITY))),
            data + ("nutrition" to ((data["nutrition"] as Map<*, *>) + ("protein_g" to Double.NaN))),
            data + ("nutrition" to ((data["nutrition"] as Map<*, *>) + ("calories_kcal" to 720.5))),
            data + ("nutrition" to ((data["nutrition"] as Map<*, *>) + ("protein_g" to 42.55))),
        )
        invalid.forEach { assertNull(TodayOverviewReply.from(required(), it)) }
    }

    @Test fun `goals must be finite nonzero bounded daily references at display precision`() {
        val nutrition = response()["nutrition"] as Map<*, *>
        val invalidGoals = listOf(
            nutrition + ("calorie_goal_kcal" to 0.0),
            nutrition + ("calorie_goal_kcal" to 10_001.0),
            nutrition + ("calorie_goal_kcal" to Double.NaN),
            nutrition + ("calorie_goal_kcal" to 2_000.5),
            nutrition + ("protein_goal_g" to 0.0),
            nutrition + ("protein_goal_g" to 1_001.0),
            nutrition + ("protein_goal_g" to Double.POSITIVE_INFINITY),
            nutrition + ("protein_goal_g" to 100.05),
        )
        invalidGoals.forEach { goals ->
            val data = response() + ("nutrition" to goals)
            assertNull(TodayOverviewReply.from(required(), data))
        }
    }

    @Test fun `final audit binds calorie and protein values to their correct roles`() {
        val data = response()
        val evidence = MetriaAgentFactEvidence.from("read_progress_overview", data)
        assertNull(MetriaAgentFactAuditor.audit("Your daily calorie goal is 42.5 kcal.", evidence).acceptedText)
        assertNull(MetriaAgentFactAuditor.audit("Your daily protein goal is 720 g protein.", evidence).acceptedText)
        val reply = TodayOverviewReply.from(required(), data)
        assertEquals(reply, MetriaAgentFactAuditor.audit(requireNotNull(reply), evidence).acceptedText)
    }
}
