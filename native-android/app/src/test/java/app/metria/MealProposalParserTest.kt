package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MealProposalParserTest {
    @Test
    fun emptyClarificationNeverBecomesAReviewableMeal() {
        val proposal = MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[],"assumptions":[],"questions":["What foods were in the meal?"],"overall_confidence":"low"}"""
        )
        assertNull(proposal)

        assertNull(MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[],"assumptions":[],"questions":[],"overall_confidence":"low"}"""
        ))
    }

    @Test
    fun classifiesMalformedResponsesWithoutIncludingModelContent() {
        val result = MealProposalParser.parseWithDiagnostic("private model prose containing a unique secret")

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_JSON, result.rejectionCategory)
        assertEquals("response was not valid JSON", result.rejectionCategory?.diagnosticLabel)
        assertEquals(false, result.rejectionCategory?.diagnosticLabel?.contains("secret"))
    }

    @Test
    fun classifiesRootShapeFailures() {
        val result = MealProposalParser.parseWithDiagnostic("[]")

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_ROOT_SHAPE, result.rejectionCategory)
    }

    @Test
    fun classifiesMalformedGroupedItems() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"private-name","kind":"dish","ingredients":"bad"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_GROUP, result.rejectionCategory)
    }

    @Test
    fun classifiesMalformedItems() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"private-name","visible_portion":"serving","estimated_grams_range":{"min":100,"max":150},"preparation":"cooked","confidence":"high","role":"unknown"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_ITEM, result.rejectionCategory)
    }

    @Test
    fun rejectsDescriptiveEvidenceInsteadOfAnEnumValue() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Steak","description":"grilled steak","visible_portion":"one piece","estimated_grams_range":{"min":200,"max":300},"preparation":"grilled","confidence":"medium","evidence":"Clearly visible grilled steak","role":"main_food"}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        )

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_ITEM, result.rejectionCategory)
    }

    @Test
    fun rejectsAnEmptyMealRatherThanDisplayingAReviewWithNoFoods() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[],"assumptions":[],"questions":[],"overall_confidence":"low"}"""
        )

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_ITEM, result.rejectionCategory)
    }

    @Test
    fun imagePromptConstrainsCandidatesAndEvidenceToTheSafeContract() {
        assertEquals(true, MEAL_IMAGE_ANALYSIS_PROMPT.contains("at most the two strongest clearly visible food candidates"))
        assertEquals(true, MEAL_IMAGE_ANALYSIS_PROMPT.contains("evidence value must be exactly the enum string seen or possible_inside"))
        assertEquals(true, MEAL_IMAGE_ANALYSIS_PROMPT.contains("Omit unclear extra foods"))
        assertEquals(true, MEAL_IMAGE_ANALYSIS_PROMPT.contains("return an empty items array"))
        assertEquals(true, MEAL_IMAGE_ANALYSIS_PROMPT.contains("use low confidence"))
    }

    @Test
    fun classifiesInvalidPortionRanges() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"private-name","visible_portion":"serving","estimated_grams_range":{"min":3000,"max":4000},"preparation":"cooked","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertNull(result.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_RANGE, result.rejectionCategory)
    }

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
    fun `normalizes captured grouped kind and evidence aliases`() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Plate","kind":"side","ingredients":[{"name":"Steak","description":"","visible_portion":"one piece","estimated_grams_range":{"min":180,"max":260},"preparation":"grilled","confidence":"medium","evidence":"visible_portion","role":"main_food"}]}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        )
        val proposal = requireNotNull(result.proposal)
        assertNull(proposal.partialCategory)
        assertEquals(0, proposal.omittedItemCount)
        assertEquals("seen", proposal.items.single().evidence)
        assertEquals("Plate", proposal.items.single().groupLabel)
    }

    @Test
    fun `multi dish response with captured structural variations reaches review`() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Soup","kind":"dish","ingredients":[{"name":"Beans","visible_portion":"one bowl","estimated_grams_range":[120,220],"preparation":"cooked","confidence":"medium","evidence":"visible_portion","role":"main_food"}]},{"name":"Bread","kind":"side","ingredients":[{"name":"Toast","visible_portion":"one slice","estimated_grams_range":[30,60],"preparation":"toasted","confidence":"medium","evidence":"visible_portion","role":"side"}]},{"name":"Tea","kind":"drink","ingredients":[{"name":"Tea","visible_portion":"partial cup","estimated_grams_range":[100,180],"preparation":"beverage","confidence":"low","evidence":"visible_portion","role":"drink"}]}],"assumptions":"Portions are estimated.","questions":"Was sugar added?","overall_confidence":"medium"}"""
        )

        assertEquals(listOf("Soup", "Bread", "Tea"), proposal.items.map { it.groupLabel })
        assertEquals(0, proposal.omittedItemCount)
        assertEquals(listOf("Portions are estimated."), proposal.assumptions)
    }

    @Test
    fun `keeps a separately grouped visible sauce for member review`() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Fish plate","kind":"dish","ingredients":[{"name":"Fish","visible_portion":"one fillet","estimated_grams_range":[150,250],"preparation":"cooked","confidence":"high","evidence":"visible","role":"main_food"}]},{"name":"Creamy sauce","kind":"sauce","ingredients":[{"name":"Creamy sauce","visible_portion":"small serving","estimated_grams_range":[20,50],"preparation":"prepared","confidence":"medium","evidence":"visible","role":"sauce"}]}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        assertEquals(listOf("Fish", "Creamy sauce"), proposal.items.map { it.name })
        assertEquals(listOf("Fish plate", "Creamy sauce"), proposal.items.map { it.groupLabel })
        assertEquals("sauce_oil", proposal.items.last().role)
        assertEquals(0, proposal.omittedItemCount)
        assertNull(proposal.partialCategory)
        assertEquals("high", proposal.overallConfidence)
    }

    @Test
    fun `sauce group still rejects an invalid ingredient without losing valid foods`() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Fish","visible_portion":"one fillet","estimated_grams_range":[150,250],"preparation":"cooked","confidence":"high"},{"name":"Sauce","kind":"sauce","ingredients":[{"name":"Sauce","visible_portion":"small serving","estimated_grams_range":[20,50],"preparation":"prepared","confidence":"medium","role":"unknown"}]}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )

        val proposal = requireNotNull(result.proposal)
        assertEquals(listOf("Fish"), proposal.items.map { it.name })
        assertEquals(1, proposal.omittedItemCount)
        assertEquals(MealProposalOmissionCategory.INVALID_ITEM, proposal.partialCategory)
        assertEquals("low", proposal.overallConfidence)
    }

    @Test
    fun `preserves earlier explicit evidence and singleton group aliases`() {
        val proposal = MealProposalParser.parseOrThrow(
            """{"kind":"meal_proposal","items":[{"name":"Soup","kind":"dish","ingredients":{"name":"Beans","visible_portion":"bowl","estimated_grams_range":{"min":100,"max":180},"preparation":"cooked","confidence":"medium","evidence":"observed"}},{"name":"Sauce","visible_portion":"possible spoonful","estimated_grams_range":{"min":5,"max":20},"preparation":"","confidence":"low","evidence":"inferred"}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
        )

        assertEquals(listOf("seen", "possible_inside"), proposal.items.map { it.evidence })
        assertEquals("Soup", proposal.items.first().groupLabel)
        assertEquals(0, proposal.omittedItemCount)
    }

    @Test
    fun `keeps valid foods when group metadata or sibling ingredients are malformed`() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Dinner","kind":"dish","ingredients":[{"name":"Steak","visible_portion":"one piece","estimated_grams_range":{"min":180,"max":260},"preparation":"grilled","confidence":"medium"},{"name":"Mystery","visible_portion":"some","estimated_grams_range":{"min":80,"max":120},"preparation":"","confidence":"medium","evidence":"visible plate suggests food","role":"main_food"},{"name":"Potatoes","visible_portion":"several","estimated_grams_range":{"min":100,"max":180},"preparation":"roasted","confidence":"medium"}]},{"name":"Malformed group","kind":"unknown","ingredients":[{"name":"Ignored"}]},{"name":"Wine","kind":"drink","ingredients":[{"name":"Wine","visible_portion":"one glass","estimated_grams_range":{"min":100,"max":180},"preparation":"","confidence":"low"}]}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )
        val proposal = requireNotNull(result.proposal)
        assertEquals(listOf("Steak", "Potatoes", "Wine"), proposal.items.map { it.name })
        assertEquals(2, proposal.omittedItemCount)
        assertEquals(MealProposalOmissionCategory.MULTIPLE, proposal.partialCategory)
        assertEquals("low", proposal.overallConfidence)
    }

    @Test
    fun `rejects all invalid foods and malformed roots`() {
        val allInvalid = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Food","visible_portion":"serving","estimated_grams_range":{"min":3000,"max":4000},"preparation":"cooked","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )
        assertNull(allInvalid.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_RANGE, allInvalid.rejectionCategory)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealProposalParser.parseOrThrow(
                """{"kind":"meal_proposal","items":[{"name":"Food","visible_portion":"serving","estimated_grams_range":{"min":3000,"max":4000},"preparation":"cooked","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
            )
        }

        val invalidRoot = MealProposalParser.parseWithDiagnostic(
            """{"kind":"not_meal_proposal","items":[{}],"assumptions":[],"questions":[],"overall_confidence":"low"}"""
        )
        assertNull(invalidRoot.proposal)
        assertEquals(MealProposalRejectionCategory.INVALID_ROOT_SHAPE, invalidRoot.rejectionCategory)
    }

    @Test
    fun `rejects unapproved evidence and group kind aliases while preserving valid siblings`() {
        val result = MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[{"name":"Bad evidence","visible_portion":"some","estimated_grams_range":{"min":10,"max":20},"preparation":"","confidence":"medium","evidence":"obvious food","role":"main_food"},{"name":"Unknown group","kind":"side-ish","ingredients":[{"name":"Food"}]},{"name":"Fruit","visible_portion":"one apple","estimated_grams_range":{"min":100,"max":180},"preparation":"raw","confidence":"high"}],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        )
        val proposal = requireNotNull(result.proposal)
        assertEquals(listOf("Fruit"), proposal.items.map { it.name })
        assertEquals(2, proposal.omittedItemCount)
        assertEquals(MealProposalOmissionCategory.MULTIPLE, proposal.partialCategory)
    }

    @Test
    fun `does not silently truncate proposals above twelve foods`() {
        val foods = (1..13).joinToString(",") { index ->
            """{"name":"Food $index","visible_portion":"serving","estimated_grams_range":{"min":100,"max":150},"preparation":"cooked","confidence":"medium"}"""
        }
        val proposal = requireNotNull(MealProposalParser.parseWithDiagnostic(
            """{"kind":"meal_proposal","items":[$foods],"assumptions":[],"questions":[],"overall_confidence":"high"}"""
        ).proposal)
        assertEquals(12, proposal.items.size)
        assertEquals(1, proposal.omittedItemCount)
        assertEquals(MealProposalOmissionCategory.TOO_MANY_ITEMS, proposal.partialCategory)
        assertEquals("low", proposal.overallConfidence)
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

    @Test
    fun rejectsHostOnlyMealSlotWhenItAppearsInModelJson() {
        val proposal = MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[{"name":"Rice","visible_portion":"bowl","estimated_grams_range":{"min":100,"max":180},"preparation":"cooked","confidence":"medium"}],"assumptions":[],"questions":[],"overall_confidence":"medium","member_meal_slot":"dinner"}"""
        )

        assertNull(proposal)
    }

    @Test
    fun rejectsHostOnlyDrinkQuantityWhenItAppearsInModelJson() {
        val proposal = MealProposalParser.parse(
            """{"kind":"meal_proposal","items":[{"name":"Coffee","visible_portion":"cup","estimated_grams_range":{"min":100,"max":180},"preparation":"brewed","confidence":"medium","role":"drink"}],"assumptions":[],"questions":[],"overall_confidence":"medium","member_drink_quantity":{"amount":200,"unit":"ml"}}"""
        )
        assertNull(proposal)
    }
}
