package app.metria

import java.time.Instant
import java.util.UUID

data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    val kilograms: Double,
    val recordedAt: Instant = Instant.now(),
    val note: String? = null,
)

object WeightUnits {
    private const val kilogramsPerPound = 0.45359237

    fun poundsToKilograms(value: Double) = value * kilogramsPerPound

    fun kilogramsToPounds(value: Double) = value / kilogramsPerPound

    fun isValidKilograms(value: Double) = value.isFinite() && value in 15.0..500.0
}
