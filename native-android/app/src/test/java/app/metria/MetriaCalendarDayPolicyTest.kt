package app.metria

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetriaCalendarDayPolicyTest {
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun `Spanish and English named dates resolve to the same bounded local day`() {
        val questions = listOf(
            "¿Cuántas calorías consumí el 27 de agosto?",
            "How many calories did I eat on August 27?",
            "How many calories did I eat on 27 August 2026?",
            "How many calories did I eat on 2026-08-27?",
        )
        questions.forEach { question ->
            val result = MetriaCalendarDayPolicy.scan(question, today, rejectNumericDates = true)
            assertFalse(question, result.unsupported)
            assertEquals(question, LocalDate.of(2026, 8, 27), result.claims.single().day)
        }
    }

    @Test
    fun `month without year resolves to the latest past occurrence across New Year`() {
        val result = MetriaCalendarDayPolicy.scan(
            "How many calories did I eat on December 30?", LocalDate.of(2027, 1, 3),
        )
        assertEquals(LocalDate.of(2026, 12, 30), result.claims.single().day)
    }

    @Test
    fun `future invalid out of range and ambiguous numeric dates are unsupported`() {
        listOf(
            "el 31 de febrero",
            "el 27 de agosto de 2027",
            "el 27 de agosto de 2025",
            "el 27/08/2026",
            "el 27 de agosto de 26",
        ).forEach { question ->
            assertTrue(question, MetriaCalendarDayPolicy.scan(question, today, rejectNumericDates = true).unsupported)
        }
        assertFalse(MetriaCalendarDayPolicy.scan("How is my week?", today).hasExpression)
    }

    @Test
    fun `two named dates remain two claims for clarification`() {
        val result = MetriaCalendarDayPolicy.scan("August 27 compared with August 28", today)
        assertEquals(2, result.claims.size)
    }
}
