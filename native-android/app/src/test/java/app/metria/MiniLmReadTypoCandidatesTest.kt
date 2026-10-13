package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MiniLmReadTypoCandidatesTest {
    @Test
    fun `corrects one substitution deletion insertion or transposition`() {
        mapOf(
            "show me an oberview of my day today" to "show me an overview of my day today",
            "show me an overvieew of my day today" to "show me an overview of my day today",
            "show me an overvie of my day today" to "show me an overview of my day today",
            "show my recoreded meals today" to "show my recorded meals today",
            "what is my calorie totall for today" to "what is my calorie total for today",
            "what is my protien total for today" to "what is my protein total for today",
            "what is my latest weightt" to "what is my latest weight",
        ).forEach { (input, expected) ->
            assertEquals(input, expected, MiniLmReadTypoCandidates.uniqueSingleCorrection(input))
        }
    }

    @Test
    fun `abstains on short unknown ambiguous or multiple misspelled tokens`() {
        listOf(
            "meel",
            "show my mealx today",
            "show my caloriex today",
            "show my recoreded mealx today",
            "show me my usual breakfast",
            "show my latest height",
            "show my lamest meal",
            "show my meats today",
            "show my meals for sunday",
            "show Maria my latest weight",
            "show my deals today",
        ).forEach { input ->
            assertNull(input, MiniLmReadTypoCandidates.uniqueSingleCorrection(input))
        }
    }
}
