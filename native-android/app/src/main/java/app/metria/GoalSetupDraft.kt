package app.metria

import java.time.Instant

/** Pure transitions for transient setup provenance; callers keep the returned values in memory only. */
internal object GoalSetupDraftPolicy {
    data class References(val values: List<String>, val acceptedEstimate: MaintenanceEstimate?)
    data class MacroReferenceDraft(val values: List<String>, val source: MacroSuggestion?)
    data class PhaseEnergyDraft(
        val references: List<String>,
        val phaseEnergySuggestion: PhaseEnergySuggestion?,
        val macroSuggestion: MacroSuggestion?,
    )

    fun invalidateEstimate(current: References, selected: Boolean = false): References = current.copy(
        values = current.values.toMutableList().also { if (selected || current.acceptedEstimate != null) it[0] = "" },
        acceptedEstimate = null,
    )

    fun chooseEstimate(current: References, estimate: MaintenanceEstimate, endpoint: Double): References {
        require(endpoint == estimate.minimumKcal || endpoint == estimate.maximumKcal) {
            "Choose one of the displayed estimate endpoints."
        }
        return current.copy(values = current.values.toMutableList().also { it[0] = endpoint.toString() }, acceptedEstimate = estimate)
    }

    fun editCalories(current: References, value: String): References = current.copy(
        values = current.values.toMutableList().also { it[0] = value }, acceptedEstimate = null,
    )

    fun editMacro(current: References, index: Int, value: String): References {
        require(index in 1..3)
        return current.copy(values = current.values.toMutableList().also { it[index] = value })
    }

    fun createPhase(
        id: String,
        intentName: String,
        editing: GoalPhase?,
        references: List<String>,
        startingWeight: String,
        targetWeight: String,
        minimumWeight: String,
        maximumWeight: String,
        acceptedEstimate: MaintenanceEstimate?,
        now: Instant,
        macroSuggestion: MacroSuggestion? = null,
        phaseEnergySuggestion: PhaseEnergySuggestion? = null,
    ): GoalPhase {
        fun weight(raw: String): Double? = if (raw.isBlank()) null else requireNotNull(raw.trim().replace(',', '.').toDoubleOrNull()) { "Check your weight values." }
        val phase = GoalPhase(
            id = id,
            intent = GoalIntent.valueOf(intentName),
            confirmedAt = editing?.confirmedAt ?: now,
            updatedAt = maxOf(editing?.updatedAt ?: now, now),
            targets = NutritionTargetInput.parse(references).getOrThrow(),
            referenceWeightKg = phaseEnergySuggestion?.estimate?.profile?.weightKg ?: acceptedEstimate?.profile?.weightKg ?: weight(startingWeight),
            targetWeightKg = weight(targetWeight),
            rangeMinimumKg = weight(minimumWeight),
            rangeMaximumKg = weight(maximumWeight),
            acceptedEstimate = acceptedEstimate,
            macroSuggestion = macroSuggestion,
            phaseEnergySuggestion = phaseEnergySuggestion,
        )
        GoalPolicy.validate(phase)
        return phase
    }

    /** An explicit edit changes only that macro's source; unrelated proposal fields remain derived. */
    fun markMacroManual(source: MacroSuggestion?, field: GoalMacroField): MacroSuggestion? = source?.let {
        when (field) {
            GoalMacroField.PROTEIN -> it.copy(proteinDerived = false)
            GoalMacroField.CARBOHYDRATE -> it.copy(carbohydrateDerived = false)
            GoalMacroField.FAT -> it.copy(fatDerived = false)
        }.takeIf { updated -> updated.proteinDerived || updated.carbohydrateDerived || updated.fatDerived }
    }

    /** A changed dependency removes derived values but leaves prior member overrides untouched. */
    fun invalidateMacroReferences(values: List<String>, source: MacroSuggestion?): MacroReferenceDraft {
        require(values.size == 3)
        if (source == null) return MacroReferenceDraft(values.toList(), null)
        return MacroReferenceDraft(
            values = values.mapIndexed { index, value ->
                val derived = when (index) {
                    0 -> source.proteinDerived
                    1 -> source.carbohydrateDerived
                    else -> source.fatDerived
                }
                value.takeUnless { derived }.orEmpty()
            },
            source = null,
        )
    }

    /** Editing even an unchanged numeric value makes that field explicitly member-entered. */
    fun editMacroReference(values: List<String>, source: MacroSuggestion?, field: GoalMacroField, value: String): MacroReferenceDraft {
        require(values.size == 3)
        val index = when (field) {
            GoalMacroField.PROTEIN -> 0
            GoalMacroField.CARBOHYDRATE -> 1
            GoalMacroField.FAT -> 2
        }
        return MacroReferenceDraft(
            values = values.toMutableList().also { it[index] = value },
            source = markMacroManual(source, field),
        )
    }

    /** A changed used input removes the accepted energy value and only dependent macro values. */
    fun invalidatePhaseEnergy(
        references: List<String>,
        phaseEnergySource: PhaseEnergySuggestion?,
        macroSource: MacroSuggestion?,
    ): PhaseEnergyDraft {
        require(references.size == 4)
        val macro = if (phaseEnergySource == null) MacroReferenceDraft(references.drop(1), macroSource)
            else invalidateMacroReferences(references.drop(1), macroSource)
        return PhaseEnergyDraft(
            references = listOf(references[0].takeUnless { phaseEnergySource != null }.orEmpty()) + macro.values,
            phaseEnergySuggestion = null,
            macroSuggestion = macro.source,
        )
    }

    /** Stage a reviewed phase calorie proposal and invalidate only macros tied to another budget. */
    fun usePhaseEnergy(
        references: List<String>,
        proposal: PhaseEnergySuggestion,
        macroSource: MacroSuggestion?,
    ): PhaseEnergyDraft {
        require(references.size == 4)
        PhaseEnergyCalculator.validate(proposal)
        macroSource?.let { MacroCalculator.validate(it) }
        val macros = if (macroSource != null && macroSource.caloriesKcal != proposal.caloriesKcal) {
            invalidateMacroReferences(references.drop(1), macroSource)
        } else {
            MacroReferenceDraft(references.drop(1), macroSource)
        }
        return PhaseEnergyDraft(
            references = listOf(proposal.caloriesKcal.toString()) + macros.values,
            phaseEnergySuggestion = proposal,
            macroSuggestion = macros.source,
        )
    }

    /** Typing calories is an explicit manual edit, even when the typed value is unchanged. */
    @Suppress("UNUSED_PARAMETER")
    fun editPhaseCalories(
        references: List<String>,
        phaseEnergySource: PhaseEnergySuggestion?,
        macroSource: MacroSuggestion?,
        value: String,
    ): PhaseEnergyDraft {
        require(references.size == 4)
        val macro = invalidateMacroReferences(references.drop(1), macroSource)
        return PhaseEnergyDraft(
            references = listOf(value) + macro.values,
            phaseEnergySuggestion = null,
            macroSuggestion = macro.source,
        )
    }
}
