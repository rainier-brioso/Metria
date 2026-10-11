package app.metria

/** Decides whether an editor sheet may leave its current task. */
object EditorDismissalPolicy {
    enum class Decision {
        ALLOW,
        ASK,
        BLOCK,
    }

    fun decision(hasChanges: Boolean, saving: Boolean): Decision = when {
        saving -> Decision.BLOCK
        hasChanges -> Decision.ASK
        else -> Decision.ALLOW
    }
}
