package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateCoachSnapshotTest {
    private val snapshot = PrivateCoachSnapshot(
        todayMeals = 1,
        weekMeals = 4,
        todayCalories = 700.0,
        todayProtein = 45.0,
        weekCalories = 5_706.0,
        weekProtein = 294.0,
        calorieGoal = 2_500.0,
        proteinGoal = 180.0,
        latestWeightKg = null,
        previousWeightKg = null,
    )

    @Test
    fun `weekly grounding explicitly includes logged meals and weekly goals`() {
        val facts = snapshot.selectedNutritionFacts("week")

        assertTrue(facts.contains("selected_period=last_7_days"))
        assertTrue(facts.contains("logged_meals=4"))
        assertTrue(facts.contains("calories_kcal=5706"))
        assertTrue(facts.contains("calorie_goal_kcal=17500"))
        assertFalse(facts.contains("today"))
    }
}
