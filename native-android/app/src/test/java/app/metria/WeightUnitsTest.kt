package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightUnitsTest {
    @Test
    fun `pounds round-trip through canonical kilograms`() {
        assertEquals(180.0, WeightUnits.kilogramsToPounds(WeightUnits.poundsToKilograms(180.0)), 0.0001)
    }

    @Test
    fun `weight validation keeps implausible values out of vault`() {
        assertTrue(WeightUnits.isValidKilograms(75.0))
        assertFalse(WeightUnits.isValidKilograms(0.0))
        assertFalse(WeightUnits.isValidKilograms(501.0))
    }
}
