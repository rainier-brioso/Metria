package app.metria

import java.time.Instant

/** Focused weight editing cannot change the calorie calculation or its reviewed inputs. */
object GoalWeightInput {
    fun parse(phase: GoalPhase, intent: GoalIntent, target: String, minimum: String, maximum: String, now: Instant = Instant.now()): Result<GoalPhase> = runCatching {
        require(phase.phaseEnergySuggestion == null || intent == phase.intent) {
            "Changing the phase direction requires reviewing its calorie proposal and references."
        }
        fun kilograms(value: String): Double? = if (value.isBlank()) null else
            requireNotNull(value.trim().replace(',', '.').toDoubleOrNull()) { "Enter a weight in kg, or leave it empty." }
        phase.copy(intent = intent, targetWeightKg = kilograms(target), rangeMinimumKg = kilograms(minimum),
            rangeMaximumKg = kilograms(maximum), updatedAt = maxOf(phase.updatedAt, now)).also(GoalPolicy::validate)
    }
}
