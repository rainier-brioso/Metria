package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleContextTest {
    private val today = LocalDate.parse("2026-08-11")

    @Test fun `normalizes unique newest starts within the private bound`() {
        val normalized = CycleContextValidation.normalized(
            CycleContext(true, listOf(today.minusDays(1), today.minusDays(4), today.minusDays(1))),
        )
        assertEquals(listOf(today.minusDays(1), today.minusDays(4)), normalized.periodStarts)
        assertTrue(CycleContextValidation.valid(normalized, today))
    }

    @Test fun `rejects future and stale period starts`() {
        assertFalse(CycleContextValidation.valid(CycleContext(true, listOf(today.plusDays(1))), today))
        assertFalse(CycleContextValidation.valid(CycleContext(true, listOf(today.minusYears(3).minusDays(1))), today))
    }
}
