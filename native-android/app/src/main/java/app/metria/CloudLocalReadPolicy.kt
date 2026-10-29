package app.metria

import java.time.LocalDate

/** Adds the same host-owned reply scope used by local fast reads; never model arguments. */
internal object CloudLocalReadPolicy {
    fun resolve(
        required: RequiredAgentTool?,
        text: String,
        selectedMealId: String?,
        today: LocalDate,
    ): RequiredAgentTool? =
        MiniLmWeightReadPolicy.resolve(required, text, selectedMealId, today)?.requiredTool
            ?: MiniLmMealReadPolicy.requiredReadTool(required, text, selectedMealId, today)
            ?: required
}
