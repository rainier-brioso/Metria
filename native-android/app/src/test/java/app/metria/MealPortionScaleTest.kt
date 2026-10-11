package app.metria

import org.junit.Assert.*
import org.junit.Test

class MealPortionScaleTest {
    @Test fun scalesEveryFoodWithDeterministicGramRounding() {
        assertEquals(listOf("50", "76"), MealPortionScale.apply(listOf("100", "151"), .5))
    }
    @Test fun rejectsWholeChangeWhenAnyFoodIsInvalidOrOutsideBounds() {
        assertNull(MealPortionScale.apply(listOf("100", ""), 2.0))
        assertNull(MealPortionScale.apply(listOf("100", "1500"), 2.0))
        assertNull(MealPortionScale.apply(listOf("1"), .25))
        assertNull(MealPortionScale.apply(emptyList(), 1.0))
    }
    @Test fun rejectsInvalidAndUnsupportedFactors() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -.5, 4.1).forEach { assertNull(MealPortionScale.apply(listOf("100"), it)) }
    }
    @Test fun voicePreviewRejectsEmptyControlCharactersAndExcessiveText() {
        assertEquals("rice and chicken", VoiceEntryText.validated(" rice and chicken "))
        listOf(null, " ", "a".repeat(601), "rice\u0000").forEach { assertNull(VoiceEntryText.validated(it)) }
    }
}
