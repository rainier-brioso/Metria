package app.metria

import kotlin.math.roundToInt

/** An atomic quantity change; no raw/cooked yield or nutrition source conversion. */
internal object MealPortionScale {
    fun apply(grams: List<String>, factor: Double): List<String>? {
        if (grams.isEmpty() || !factor.isFinite() || factor !in 0.25..4.0) return null
        val parsed = grams.map { it.trim().toIntOrNull()?.takeIf { value -> value in 1..2_000 } ?: return null }
        return parsed.map { value ->
            (value * factor).roundToInt().takeIf { it in 1..2_000 }?.toString() ?: return null
        }
    }
}
