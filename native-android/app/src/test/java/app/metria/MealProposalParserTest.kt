package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MealProposalParserTest {
    @Test
    fun acceptsSingletonAssumptionFromOtherwiseValidModelResponse() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Fries","visible_portion":"one serving","estimated_grams_range":{"min":300,"max":500},"preparation":"fried","confidence":"high"}],"assumptions":"The visible plate may contain sauce.","questions":["What protein is present?"],"overall_confidence":"high"}"""
        )

        assertNotNull(proposal)
        assertEquals(listOf("The visible plate may contain sauce."), proposal.assumptions)
    }

    @Test
    fun acceptsKnownCamelCaseAndQuotedWholeNumbersOnly() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":{"name":"Rice","visiblePortion":"half plate","estimatedGramsRange":{"min":"120","max":"180"},"preparation":"cooked","confidence":" HIGH "},"assumptions":["portion is estimated"],"questions":"Was oil added?","overallConfidence":"Medium"}"""
        )

        assertNotNull(proposal)
        assertEquals("medium", proposal.overallConfidence)
        assertEquals(120, proposal.items.single().estimatedGramsRange.min)
        assertEquals(listOf("Was oil added?"), proposal.questions)
    }

    @Test
    fun acceptsTwoValueRangeArrayFromImageModel() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Chicken","visible_portion":"several pieces","estimated_grams_range":[300,500],"preparation":"fried","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertEquals(300, proposal.items.single().estimatedGramsRange.min)
        assertEquals(500, proposal.items.single().estimatedGramsRange.max)
    }

    @Test
    fun normalizesOrdinaryNutritionRoleLabelsFromTheModel() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Rice","description":"white rice","visible_portion":"large serving","estimated_grams_range":[200,300],"preparation":"cooked","confidence":"high","role":"Base"},{"name":"Chicken","description":"cooked pieces","visible_portion":"several pieces","estimated_grams_range":[120,220],"preparation":"stir-fried","confidence":"high","role":"Protein"},{"name":"Peas","description":"green peas","visible_portion":"scattered","estimated_grams_range":[20,60],"preparation":"cooked","confidence":"medium","role":"Vegetable"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertEquals(listOf("main_food", "main_food", "side"), proposal.items.map { it.role })
    }

    @Test
    fun normalizesFatMoistureToSauceOil() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Oil/Sauce","description":"visible cooking liquid","visible_portion":"thin layer","estimated_grams_range":[10,30],"preparation":"cooked","confidence":"medium","role":"Fat/Moisture"}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        )

        assertEquals("sauce_oil", proposal.items.single().role)
    }

    @Test fun `flattens nested dish ingredients while retaining the dish label`() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Chicken with potatoes","kind":"dish","ingredients":[{"name":"Chicken","description":"roasted pieces","visible_portion":"one breast","estimated_grams_range":{"min":140,"max":200},"preparation":"roasted","confidence":"high"},{"name":"Potatoes","description":"golden French fries","visible_portion":"one side","estimated_grams_range":{"min":120,"max":180},"preparation":"fried","confidence":"medium"}]},{"name":"Beer","kind":"drink","ingredients":[{"name":"Beer","description":"pale beer in a glass","visible_portion":"one can","estimated_grams_range":{"min":300,"max":350},"preparation":"","confidence":"high"}]}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        )
        assertEquals(3, proposal.items.size)
        assertEquals("Chicken with potatoes", proposal.items[0].groupLabel)
        assertEquals("golden French fries", proposal.items[1].description)
        assertEquals("Beer", proposal.items[2].groupLabel)
    }

    @Test
    fun rejectsRangeArrayThatIsNotExactlyMinAndMax() {
        val proposal = MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[{"name":"Chicken","visible_portion":"pieces","estimated_grams_range":[100,200,300],"preparation":"fried","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertNull(proposal)
    }

    @Test
    fun stillRejectsUnknownFields() {
        val proposal = MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[],"assumptions":[],"questions":[],"overall_confidence":"high","extra":"must not be accepted"}"""
        )

        assertNull(proposal)
    }
}
