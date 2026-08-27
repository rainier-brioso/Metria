package app.metria

/**
 * Approved M4 direction: every assistant entry point (chat today, voice later)
 * resolves model output into this tiny allowlist. Relative dates are bounded offsets
 * calculated by Metria, never calendar dates invented by the model. UI code executes the action;
 * the model never receives vault write access or navigation control directly.
 */
sealed interface AssistantAction {
    data class QueryWeightHistory(val fromDaysAgo: Int, val toDaysAgo: Int) : AssistantAction
    data class QueryMealHistory(val fromDaysAgo: Int, val toDaysAgo: Int, val mealSlot: String? = null) : AssistantAction
    data class QueryNutrition(val period: String?) : AssistantAction
    data class SearchSavedDishes(val query: String) : AssistantAction
    data class DraftWeight(val kilograms: Double) : AssistantAction
    data object DraftMeal : AssistantAction
    data class Respond(val message: String) : AssistantAction
    data object Clarify : AssistantAction
}

object AssistantActionDispatcher {
    fun resolve(call: PrivateCoachToolCall): AssistantAction = when (call.intent) {
        "get_nutrition_summary", "read_goal_progress" -> AssistantAction.QueryNutrition(call.period)
        "get_weight_history", "read_weight", "read_weight_trend" -> {
            val from = call.fromDaysAgo ?: if (call.period == "week") 6 else 0
            val to = call.toDaysAgo ?: 0
            if (from in 0..90 && to in 0..from) AssistantAction.QueryWeightHistory(from, to) else AssistantAction.Clarify
        }
        "get_meal_history", "read_meals" -> {
            val from = call.fromDaysAgo ?: call.daysAgo ?: when (call.period) { "week" -> 6; "yesterday" -> 1; "day_before_yesterday" -> 2; else -> 0 }
            val to = call.toDaysAgo ?: from
            if (from in 0..90 && to in 0..from) AssistantAction.QueryMealHistory(from, to, call.mealSlot?.takeIf { it in MealSlots.allowed }) else AssistantAction.Clarify
        }
        "search_saved_dishes" -> call.query?.trim()?.takeIf { it.length in 2..120 }
            ?.let(AssistantAction::SearchSavedDishes) ?: AssistantAction.Clarify
        "draft_weight" -> call.kilograms?.let(AssistantAction::DraftWeight) ?: AssistantAction.Clarify
        "draft_meal" -> AssistantAction.DraftMeal
        "respond" -> call.message?.takeIf { it.length in 3..480 }?.let(AssistantAction::Respond) ?: AssistantAction.Clarify
        else -> AssistantAction.Clarify
    }
}
