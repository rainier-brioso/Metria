package app.metria

/** Presentation event only: never stored in preferences, bundles, vault, archives or prompts. */
class FirstGoalOffer {
    private var pending = false
    fun recordCreation(succeeded: Boolean) { pending = succeeded }
    fun take(unlocked: Boolean): Boolean {
        if (!unlocked || !pending) return false
        pending = false
        return true
    }
    fun clear() { pending = false }
}
