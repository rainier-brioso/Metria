package app.metria

import org.junit.Assert.*
import org.junit.Test

class NativeDrinkReportPolicyTest {
    private fun description(text: String, unmatched: Boolean = true, read: Boolean = false,
        draft: Boolean = false, selected: Boolean = false, confirmation: Boolean = false) =
        NativeDrinkReportPolicy.description(text, unmatched, read, draft, selected, confirmation)

    @Test fun `plain consumed drinks after no match prepare original text without choosing portions`() {
        listOf("I had a coffee for breakfast", "I had tea", "I just had an espresso.",
            "I have had orange juice as breakfast").forEach { assertEquals(it, description(it)) }
    }

    @Test fun `ask first advice questions negation and mixed details never take direct route`() {
        listOf("I had a coffee for breakfast. Please ask me before preparing a draft.",
            "I had coffee; do not record it", "Should I have coffee?", "I could have coffee",
            "I haven't had coffee", "I had a coffee with a friend", "I had coffee and toast",
            "I had a bad night's sleep", "I had coffee yesterday", "I had coffee?",
            "I had 200 ml coffee", "I had a large coffee", "Ah, had a coffee for breakfast").forEach {
            assertNull(it, description(it))
        }
    }

    @Test fun `saved dish selection read draft and confirmation scopes retain priority`() {
        val text = "I had a coffee for breakfast"
        assertNull(description(text, unmatched = false))
        assertNull(description(text, read = true))
        assertNull(description(text, draft = true))
        assertNull(description(text, selected = true))
        assertNull(description(text, confirmation = true))
    }
}
