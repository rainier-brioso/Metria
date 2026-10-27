package app.metria

/** Closed consumption frames only; grants an unsaved draft, never a match, amount or save. */
object NativeDrinkReportPolicy {
    private val report = Regex(
        "^(?:i had|i just had|i've had|i have had|i've just had|i have just had|" +
            "i drank|i just drank)\\s+(?:a |an |some )?" +
            "(?:coffee|tea|water|milk|espresso|latte|cappuccino|hot chocolate|" +
            "orange juice|apple juice|juice)" +
            "(?:\\s+(?:for|as)\\s+(?:my )?(?:breakfast|lunch|dinner|a snack|snack))?[.!]*$",
        RegexOption.IGNORE_CASE,
    )

    fun description(
        memberText: String,
        noSavedDishMatch: Boolean,
        hasRequiredRead: Boolean,
        hasCurrentDraft: Boolean,
        hasSelectedMeal: Boolean,
        hasPendingConfirmation: Boolean,
    ): String? {
        if (!noSavedDishMatch || hasRequiredRead || hasCurrentDraft || hasSelectedMeal || hasPendingConfirmation) return null
        val text = memberText.trim()
        return text.takeIf {
            it.length in 8..160 && (report.matches(it) || ChatDrinkPortionPolicy.reportQuantity(it) != null)
        }
    }
}
