package app.metria

/** Fixed diagnostic categories only; never carries a meal, identifier or member text. */
enum class MealWriteOrigin(val traceReason: String) {
    REVIEW("review_save"),
    DIRECT_ENTRY("direct_entry_save"),
    EDIT("existing_meal_edit"),
}
