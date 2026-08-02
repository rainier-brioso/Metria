package app.metria

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeightTrendPoint(val recordedAt: Instant, val kilograms: Double)

object WeightTrend {
    fun filter(entries: List<WeightEntry>, start: LocalDate?, end: LocalDate?): List<WeightTrendPoint> =
        entries.map { WeightTrendPoint(it.recordedAt, it.kilograms) }
            .filter { point ->
                val date = point.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
            }
            .sortedBy { it.recordedAt }

    fun movingAverage(points: List<WeightTrendPoint>, window: Int = 3): List<WeightTrendPoint> =
        points.indices.map { index ->
            val from = (index - window + 1).coerceAtLeast(0)
            val slice = points.subList(from, index + 1)
            points[index].copy(kilograms = slice.map(WeightTrendPoint::kilograms).average())
        }

    fun description(points: List<WeightTrendPoint>): String = when (points.size) {
        0 -> "No weight entries in this range."
        1 -> "One local entry in this range; add another to see a trend."
        else -> {
            val change = points.last().kilograms - points.first().kilograms
            when {
                kotlin.math.abs(change) < 0.05 -> "Weight is broadly unchanged across this range."
                change > 0 -> "Weight is ${"%.1f".format(change)} kg higher across this range."
                else -> "Weight is ${"%.1f".format(-change)} kg lower across this range."
            }
        }
    }
}
