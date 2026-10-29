package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudMealClarificationTest {
    private val clarification = """{"kind":"meal_proposal","items":[],"assumptions":[],"questions":["What food and portion did you have?"],"overall_confidence":"low"}"""

    @Test fun clarificationIsAChatQuestionAndNeverAnEmptyMealReview() {
        assertEquals("What food and portion did you have?", CloudMealClarification.question(clarification))
        assertNull(MealProposalParser.parse(clarification))
        assertNull(CloudMealClarification.question(clarification.replace("\"items\":[]", "\"items\":[{}]")))
        assertNull(CloudMealClarification.question(clarification.replace("What food and portion did you have?", "https://example.org")))
    }
}
