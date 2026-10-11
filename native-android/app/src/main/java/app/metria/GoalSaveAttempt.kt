package app.metria

/** In-memory save gate for one reviewed proposal. Never serialized or shared with an assistant. */
internal class GoalSaveAttempt(val phase: GoalPhase) {
    private enum class State { READY, SAVING, SAVED }
    private var state = State.READY

    fun begin(): Boolean {
        if (state != State.READY) return false
        state = State.SAVING
        return true
    }

    fun finish(success: Boolean) {
        check(state == State.SAVING)
        state = if (success) State.SAVED else State.READY
    }
}
