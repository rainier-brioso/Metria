package app.metria

import org.junit.Assert.*
import org.junit.Test

class AssistantReplyLanguageTest {
    @Test fun `accented and plain Spanish and English record questions have concrete languages`() {
        listOf("¿Qué comidas tengo registradas hoy?", "que comidas tengo registradas hoy?",
            "cuantas calorías tiene mi ultima comida?", "¿Y proteína?").forEach {
            assertEquals(it, "Spanish", coachReplyLanguage(it))
        }
        listOf("Which meals have I logged today?", "How much protein is in my last meal?").forEach {
            assertEquals(it, "English", coachReplyLanguage(it))
        }
    }

    @Test fun `current question takes precedence and neutral followup inherits recent member language`() {
        assertEquals("English", coachReplyLanguage("What did I eat today?", "Member: ¿Cómo voy?"))
        assertEquals("Spanish", coachReplyLanguage("cuantas calorias tiene mi ultima comida?", "Member: How am I doing?"))
        assertEquals("Spanish", coachReplyLanguage("...", "Member: ¿Qué comidas tengo?\nMetria: Your recorded meals"))
        assertEquals("English", coachReplyLanguage("...", "Member: ¿Cómo voy?\nMember: What did I eat?"))
        assertEquals("English", coachReplyLanguage("..."))
    }
}
