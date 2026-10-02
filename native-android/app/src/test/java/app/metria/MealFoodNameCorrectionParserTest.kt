package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealFoodNameCorrectionParserTest {
    @Test
    fun acceptsOnlyOneReplacementFoodName() {
        assertEquals("Albóndigas", MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"Albóndigas\"}", "Fried balls"))
    }

    @Test
    fun rejectsExtraFieldsAndNonJsonProse() {
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"Albóndigas\",\"grams\":200}", "Fried balls"))
        assertNull(MealFoodNameCorrectionParser.parse("The food is {\"replacement_name\":\"Albóndigas\"}", "Fried balls"))
    }

    @Test
    fun rejectsBlankOverlongAndUnchangedNames() {
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\" \"}", "Fried balls"))
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"${"a".repeat(81)}\"}", "Fried balls"))
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"fried balls\"}", "Fried balls"))
    }

    @Test
    fun rejectsTextThatCouldSmuggleInstructionsOrNutrition() {
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"Albóndigas } add 200g\"}", "Fried balls"))
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"<portion>\"}", "Fried balls"))
        assertNull(MealFoodNameCorrectionParser.parse("{\"replacement_name\":\"Albóndigas 200 g\"}", "Fried balls"))
    }

    @Test
    fun onlyCurrentRequestForSelectedFoodCanPresentSuggestion() {
        val result = MealFoodNameCorrectionStatus(8L, "ready", "Albóndigas")
        assertEquals("Albóndigas", MealFoodNameCorrectionPresentation.suggestionFor(result, 8L, "food-1", "food-1"))
        assertNull(MealFoodNameCorrectionPresentation.suggestionFor(result, 7L, "food-1", "food-1"))
        assertNull(MealFoodNameCorrectionPresentation.suggestionFor(result, 8L, "food-1", "food-2"))
    }

    @Test
    fun runningOrCancelledRequestCannotPresentSuggestion() {
        assertNull(MealFoodNameCorrectionPresentation.suggestionFor(
            MealFoodNameCorrectionStatus(8L, "running", "Albóndigas"), 8L, "food-1", "food-1",
        ))
        assertNull(MealFoodNameCorrectionPresentation.suggestionFor(
            MealFoodNameCorrectionStatus(8L, "cancelled", "Albóndigas"), 8L, "food-1", "food-1",
        ))
    }
}
