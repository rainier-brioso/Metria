package app.metria

import org.junit.Assert.*
import org.junit.Test

class NutritionTargetInputTest {
    @Test fun acceptsPartialTargetsAndCommaDecimals() {
        assertEquals(NutritionTargets(proteinGrams = 85.5), NutritionTargetInput.parse(listOf("", "85,5", " ", "")).getOrThrow())
    }
    @Test fun allEmptyExplicitlyRemovesTargets() {
        assertNull(NutritionTargetInput.parse(listOf("", " ", "", "")).getOrThrow())
    }
    @Test fun malformedNonfiniteAndOutOfBoundsNeverBecomeEmptyTargets() {
        listOf("abc", "NaN", "Infinity", "0", "-1", "10001").forEach { value -> assertTrue(NutritionTargetInput.parse(listOf(value, "", "", "")).isFailure) }
        assertTrue(NutritionTargetInput.parse(listOf("", "1001", "", "")).isFailure)
        assertTrue(NutritionTargetInput.parse(listOf("1")).isFailure)
    }
}
