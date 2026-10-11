package app.metria

/** Transient pregnancy/breastfeeding applicability answer; it is never part of a saved phase. */
enum class PregnancyBreastfeedingApplicability { UNANSWERED, DOES_NOT_APPLY, APPLIES }

data class CareTeamReferenceDraft(
    val references: List<String>,
    val macroSuggestion: MacroSuggestion?,
)

/** Pure gates and transitions shared by maintenance, phase-energy and macro setup. */
object GoalSuitabilityPolicy {
    fun permitsAutomaticProposal(
        generalOptionSuitable: Boolean,
        pregnancyBreastfeeding: PregnancyBreastfeedingApplicability,
    ): Boolean = generalOptionSuitable && pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.DOES_NOT_APPLY

    fun shouldUseCareTeamReferences(
        pregnancyBreastfeeding: PregnancyBreastfeedingApplicability,
        generalSupportNeeded: Boolean = false,
    ): Boolean = generalSupportNeeded || pregnancyBreastfeeding == PregnancyBreastfeedingApplicability.APPLIES

    /**
     * Route to manual references without relabelling generated values. A selected estimate or phase
     * adjustment is cleared, and only derived macros are cleared; explicit manual values survive.
     */
    fun useCareTeamReferences(
        references: List<String>,
        caloriesDerived: Boolean,
        macroSource: MacroSuggestion?,
    ): CareTeamReferenceDraft {
        require(references.size == 4)
        val macros = GoalSetupDraftPolicy.invalidateMacroReferences(references.drop(1), macroSource)
        return CareTeamReferenceDraft(
            references = listOf(references[0].takeUnless { caloriesDerived }.orEmpty()) + macros.values,
            macroSuggestion = null,
        )
    }
}
