package app.metria

import java.text.NumberFormat

enum class GoalReferenceCompleteness {
    SET, PARTLY_SET, NOT_SET;

    fun label() = when (this) {
        SET -> "Set"
        PARTLY_SET -> "Partly set"
        NOT_SET -> "Not set"
    }
}

data class GoalDailyReferenceReview(
    val completeness: GoalReferenceCompleteness,
    val suppliedCount: Int,
    val missingLabels: List<String>,
)

object GoalReviewPresentation {
    private val labels = listOf("Calories", "Protein", "Carbohydrates", "Fat")

    fun dailyReferences(targets: NutritionTargets?): GoalDailyReferenceReview {
        return dailyReferencesPresent(listOf(
            targets?.caloriesKcal,
            targets?.proteinGrams,
            targets?.carbohydrateGrams,
            targets?.fatGrams,
        ).map { it != null })
    }

    fun dailyReferences(fields: List<String>): GoalDailyReferenceReview {
        require(fields.size == labels.size)
        return dailyReferencesPresent(fields.map(String::isNotBlank))
    }

    fun dailyReferenceValues(fields: List<String>): List<String> {
        require(fields.size == labels.size)
        val units = listOf("kcal/day", "g/day", "g/day", "g/day")
        return labels.mapIndexed { index, label ->
            val raw = fields[index].trim()
            val value = raw.takeIf(String::isNotBlank)?.let { text ->
                text.replace(',', '.').toDoubleOrNull()?.let(::number) ?: text
            }
            "$label: ${value?.let { "$it ${units[index]}" } ?: "Not set"}"
        }
    }

    private fun dailyReferencesPresent(present: List<Boolean>): GoalDailyReferenceReview {
        val missing = labels.filterIndexed { index, _ -> !present[index] }
        val count = labels.size - missing.size
        return GoalDailyReferenceReview(
            completeness = when (count) {
                0 -> GoalReferenceCompleteness.NOT_SET
                labels.size -> GoalReferenceCompleteness.SET
                else -> GoalReferenceCompleteness.PARTLY_SET
            },
            suppliedCount = count,
            missingLabels = missing,
        )
    }

    /** `fields` is [calories, protein, carbohydrate, fat]; only present macros receive sources. */
    fun macroSources(fields: List<String>, source: MacroSuggestion?): List<String> = listOf(
        "Protein" to GoalMacroField.PROTEIN,
        "Carbohydrates" to GoalMacroField.CARBOHYDRATE,
        "Fat" to GoalMacroField.FAT,
    ).mapIndexedNotNull { macroIndex, (label, field) ->
        val value = fields.getOrNull(macroIndex + 1).orEmpty()
        if (value.isBlank()) null
        else {
            val derived = when (field) {
                GoalMacroField.PROTEIN -> source?.proteinDerived
                GoalMacroField.CARBOHYDRATE -> source?.carbohydrateDerived
                GoalMacroField.FAT -> source?.fatDerived
            } == true
            "$label: ${if (derived) "suggested" else "member-entered"}"
        }
    }.ifEmpty { listOf("No macro values set") }

    fun phaseCalories(phase: GoalPhase): String? = phaseCalories(
        intent = phase.intent,
        caloriesKcal = phase.targets?.caloriesKcal,
        maintenanceSelected = phase.acceptedEstimate != null,
        proposal = phase.phaseEnergySuggestion,
    )

    fun finalReviewGuidance(phase: GoalPhase): String {
        val daily = dailyReferences(phase.targets)
        val phaseAction = if (phase.intent == GoalIntent.GAIN || phase.intent == GoalIntent.LOSE) {
            "You can choose maintenance, calculate and Use a phase calorie proposal, calculate and Use macro suggestions, or enter values manually. "
        } else {
            "You can calculate and Use macro suggestions or enter values manually. "
        }
        return when (daily.completeness) {
            GoalReferenceCompleteness.NOT_SET -> "Daily references are optional. ${phaseAction}You can also continue with no daily references."
            GoalReferenceCompleteness.PARTLY_SET -> "Unset daily references can stay unset. To add them, $phaseAction"
            GoalReferenceCompleteness.SET -> "Your supplied daily values are shown above with their sources. Optional weight targets and ranges can remain unset."
        }
    }

    fun phaseCaloriesSummary(
        intent: GoalIntent,
        caloriesKcal: Double?,
        maintenanceSelected: Boolean,
        proposal: PhaseEnergySuggestion?,
    ): String? {
        if (intent != GoalIntent.GAIN && intent != GoalIntent.LOSE) return null
        val current = caloriesKcal?.let { "${number(it)} kcal/day" }
        return proposal?.let { "Adjustment applied · ${number(it.caloriesKcal)} kcal/day" }
            ?: if (maintenanceSelected) "Maintenance selected${current?.let { " · $it" }.orEmpty()} · adjustment not chosen"
            else "Adjustment not chosen"
    }

    fun phaseCalories(
        intent: GoalIntent,
        caloriesKcal: Double?,
        maintenanceSelected: Boolean,
        proposal: PhaseEnergySuggestion?,
    ): String? {
        if (intent != GoalIntent.GAIN && intent != GoalIntent.LOSE) return null
        proposal?.let {
            return "Phase calorie proposal used: ${number(it.caloriesKcal)} kcal/day."
        }
        val currentCalories = caloriesKcal?.let { "${number(it)} kcal/day" } ?: "Not set"
        return if (maintenanceSelected) {
            "Maintenance selected: $currentCalories. Phase adjustment not chosen. To apply one, calculate a proposal and select Use calorie proposal."
        } else {
            "Phase adjustment not chosen; current calorie reference: $currentCalories. To apply one, choose maintenance, calculate a proposal, then select Use calorie proposal."
        }
    }

    private fun number(value: Double) = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(value)
}
