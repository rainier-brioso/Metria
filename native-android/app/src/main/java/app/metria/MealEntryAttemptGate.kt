package app.metria

/** Rejects microphone callbacks belonging to a cancelled/replaced meal-entry attempt. */
internal class MealEntryAttemptGate {
    private var generation = 0L
    private var activeToken: Long? = null

    @Synchronized
    fun begin(): Long = (++generation).also { activeToken = it }

    @Synchronized
    fun isCurrent(token: Long): Boolean = activeToken == token

    @Synchronized
    fun finish(token: Long): Boolean {
        if (activeToken != token) return false
        activeToken = null
        return true
    }

    @Synchronized
    fun cancel() {
        activeToken = null
        generation += 1
    }
}
