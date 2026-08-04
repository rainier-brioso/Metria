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
