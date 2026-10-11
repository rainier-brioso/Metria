package app.metria

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedSavedMealReplyTest {
    private fun meal(id: String, calories: Double? = 485.0): MealEntry = MealEntry(
        id = id,
        recordedAt = Instant.parse("2026-10-07T11:00:00Z"),
        title = "Lunch bowl",
        items = listOf(MealEntryItem("Rice", 150, null), MealEntryItem("Beans", null, null)),
        assumptions = emptyList(),
        confidence = "medium",
        source = "manual_entry",
        nutrition = calories?.let { MealNutrition(it, 20.0, 60.0, 8.0, "manual_entry") },
    )

    @Test
    fun `read resolves exact id with duplicate meal titles and excludes other record details`() {
        val selected = meal("chosen-id")
        val duplicateTitle = meal("other-id", calories = 900.0)

        val result = SelectedSavedMealReply.read(listOf(selected, duplicateTitle), "chosen-id", "English")!!
        val resultMeal = result["meal"] as Map<*, *>
        val nutrition = resultMeal["nutrition"] as Map<*, *>

        assertEquals("chosen-id", result["record_id"])
        assertEquals("chosen-id", resultMeal["id"])
        assertEquals(485.0, nutrition["calories_kcal"] as Double, 0.0)
        assertFalse(resultMeal.containsKey("notes"))
        assertFalse(resultMeal.containsKey("photo"))
    }

    @Test
    fun `read returns missing for stale or ambiguous exact id and never searches title`() {
        assertNull(SelectedSavedMealReply.read(listOf(meal("actual-id")), "deleted-id", "English"))
        assertNull(SelectedSavedMealReply.read(listOf(meal("same"), meal("same")), "same", "English"))
    }

    @Test
    fun `explicit selected meal tool has no model arguments and rejects invented id`() {
        assertEquals("read_selected_meal", MetriaNativeToolCallAdapter.adapt("read_selected_meal", emptyMap<String, Any>())?.intent)
        assertNull(MetriaNativeToolCallAdapter.adapt("read_selected_meal", mapOf("id" to "invented-id")))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("What are the calories?"))
        assertNull(MetriaAgentGroundingPolicy.requiredTool("How many calories are in this meal?", selectedMealId = null))
        assertEquals(
            "read_selected_meal",
            MetriaAgentGroundingPolicy.requiredTool("How many calories are in this meal?", selectedMealId = "host-id")?.name,
        )
    }

    @Test
    fun `selected question keeps latest meal route and unsupported questions out of selected scope`() {
        assertEquals(
            "read_latest_meal",
            MetriaAgentGroundingPolicy.requiredTool("How many calories are in my latest meal?", selectedMealId = "host-id")?.name,
        )
        assertTrue(SelectedSavedMealReply.supportsQuestion("¿Cuántas calorías tiene esta comida?"))
        assertTrue(SelectedSavedMealReply.supportsQuestion("What ingredients are in this meal?"))
        assertFalse(SelectedSavedMealReply.supportsQuestion("Is this meal healthy?"))
    }

    @Test
    fun `meal reply describes only supported saved fields and fails closed on mismatched id`() {
        val exact = SelectedSavedMealReply.read(listOf(meal("chosen-id")), "chosen-id", "Spanish")!!
        val required = RequiredAgentTool(
            name = "read_selected_meal",
            selectedMealId = "chosen-id",
            selectedMealQuestion = "¿Cuántas calorías tiene esta comida?",
        )
        assertTrue(SelectedSavedMealReply.from(required, exact)!!.contains("485 kcal"))
        assertNull(SelectedSavedMealReply.from(required, exact + ("record_id" to "other-id")))
        assertNull(SelectedSavedMealReply.from(required, exact + ("source" to "external_source")))
        val english = SelectedSavedMealReply.read(listOf(meal("chosen-id")), "chosen-id", "English")!!
        val unsupported = required.copy(selectedMealQuestion = "Is this healthy?")
        assertTrue(SelectedSavedMealReply.from(unsupported, english)!!.contains("can't judge"))
    }

    @Test
    fun `missing and partial coverage stay explicit and zero remains a recorded value`() {
        val missingMeal = meal("missing", calories = null)
        val missing = SelectedSavedMealReply.read(listOf(missingMeal), "missing", "English")!!
        val missingSummary = SelectedSavedMealReply.from(
            RequiredAgentTool("read_selected_meal", selectedMealId = "missing", selectedMealQuestion = "Tell me about this meal"),
            missing,
        )!!
        assertTrue(missingSummary.contains("calories not recorded"))
        assertTrue(missingSummary.contains("protein not recorded"))
        assertTrue(missingSummary.contains("No nutrition estimate is saved"))

        val partialMeal = meal("partial").copy(
            nutrition = MealNutrition(0.0, 20.0, null, 8.0, "manual_entry", estimateCoverage = "partial"),
        )
        val partial = SelectedSavedMealReply.read(listOf(partialMeal), "partial", "English")!!
        val partialCalories = SelectedSavedMealReply.from(
            RequiredAgentTool("read_selected_meal", selectedMealId = "partial", selectedMealQuestion = "How many calories are in this meal?"),
            partial,
        )!!
        assertTrue(partialCalories.contains("0 kcal"))
        assertTrue(partialCalories.contains("partial estimate"))
        assertFalse(partialCalories.contains("No calories"))
    }
}
