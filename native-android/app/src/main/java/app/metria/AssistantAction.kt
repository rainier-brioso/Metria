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
    data class QueryDayNutrition(val daysAgo: Int) : AssistantAction
    data object QueryLatestMeal : AssistantAction
    data class QueryProgressOverview(val period: String) : AssistantAction
    data class SearchSavedDishes(val query: String) : AssistantAction
    data class DraftWeight(val kilograms: Double) : AssistantAction
    data object DraftMeal : AssistantAction
    data class Respond(val message: String) : AssistantAction
    data object Clarify : AssistantAction
}

object AssistantActionDispatcher {
    fun resolve(call: PrivateCoachToolCall): AssistantAction = when (call.intent) {
        "read_latest_meal" -> AssistantAction.QueryLatestMeal
        "get_nutrition_summary", "read_goal_progress", "read_nutrition_summary" -> AssistantAction.QueryNutrition(call.period)
        "read_day_nutrition" -> call.daysAgo
            ?.takeIf { it in 0..90 }
            ?.takeIf { call.period == "historical_day" }
            ?.let(AssistantAction::QueryDayNutrition)
            ?: AssistantAction.Clarify
        "read_progress_overview" -> call.period
            ?.takeIf { it in setOf("today", "week") }
            ?.let(AssistantAction::QueryProgressOverview)
            ?: AssistantAction.Clarify
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

/**
 * Host-side validation for unambiguous record questions and affirmative follow-ups. Gemma still
 * selects the tool; this guard prevents a generic prose response from bypassing an obviously
 * required local read. It never creates a write action or supplies record facts.
 */
object AssistantActionGuard {
    fun validate(call: PrivateCoachToolCall, memberText: String, conversationContext: String): PrivateCoachToolCall {
        if (call.intent !in setOf("respond", "clarify")) return call
        val normalized = memberText.lowercase()
        return when {
            PrivateCoachQuestions.asksYesterdayWeight(memberText) -> call.copy(
                intent = "get_weight_history",
                fromDaysAgo = 1,
                toDaysAgo = 1,
                message = null,
            )
            PrivateCoachQuestions.asksTodaySummary(memberText) -> call.copy(
                intent = "get_nutrition_summary",
                period = "today",
                message = null,
            )
            PrivateCoachQuestions.asksProgress(memberText) -> call.copy(
                intent = "get_nutrition_summary",
                period = if ("today" in normalized || "hoy" in normalized) "today" else "week",
                message = null,
            )
            isAffirmative(normalized) && pendingFollowUp(conversationContext).let(::asksAboutNutritionProgress) -> call.copy(
                intent = "get_nutrition_summary",
                period = if (listOf("today", "hoy").any { it in pendingFollowUp(conversationContext).lowercase() }) "today" else "week",
                message = null,
            )
            else -> call
        }
    }

    private fun pendingFollowUp(context: String): String =
        context.lineSequence().lastOrNull { it.startsWith("PENDING_FOLLOW_UP:") }
            ?.substringAfter(':')
            ?.trim()
            .orEmpty()

    private fun asksAboutNutritionProgress(text: String): Boolean =
        listOf("progress", "progreso", "week", "semana", "protein", "proteína", "proteina", "calorie", "caloría", "caloria", "goal", "meta")
            .any { it in text.lowercase() }

    private fun isAffirmative(text: String): Boolean =
        text.trim().trim('.', '!', '¡').let { it in setOf("yes", "yeah", "yep", "sure", "sí", "si", "vale", "claro", "ok", "okay") }
}
