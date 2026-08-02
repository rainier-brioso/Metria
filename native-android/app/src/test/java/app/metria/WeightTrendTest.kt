package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WeightTrendTest {
    private val entries = listOf(
        WeightEntry(kilograms = 70.0, recordedAt = Instant.parse("2026-08-01T12:00:00Z")),
        WeightEntry(kilograms = 71.0, recordedAt = Instant.parse("2026-08-02T12:00:00Z")),
        WeightEntry(kilograms = 72.0, recordedAt = Instant.parse("2026-08-03T12:00:00Z")),
    )

    @Test fun `filters entries inclusively by local date`() {
        assertEquals(2, WeightTrend.filter(entries, LocalDate.parse("2026-08-02"), null).size)
    }

    @Test fun `computes deterministic trailing moving average`() {
        assertEquals(71.0, WeightTrend.movingAverage(WeightTrend.filter(entries, null, null)).last().kilograms, 0.001)
    }

    @Test fun `uses neutral language for a falling trend`() {
        assertTrue(WeightTrend.description(WeightTrend.filter(entries.reversed(), null, null)).contains("higher"))
    }
}
