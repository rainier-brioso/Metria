package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTargetsTest {
    @Test
    fun acceptsOptionalMemberEnteredTargets() {
        assertTrue(NutritionTargetValidation.valid(NutritionTargets(
            caloriesKcal = 2200.0,
            proteinGrams = 150.0,
            carbohydrateGrams = 250.0,
            fatGrams = 70.0,
        )))
    }

    @Test
    fun rejectsZeroOrExtremeTargets() {
        assertFalse(NutritionTargetValidation.valid(NutritionTargets(caloriesKcal = 0.0)))
        assertFalse(NutritionTargetValidation.valid(NutritionTargets(proteinGrams = 1001.0)))
    }
}
