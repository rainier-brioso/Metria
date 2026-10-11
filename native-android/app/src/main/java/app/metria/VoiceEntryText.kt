package app.metria

/** Recognition cannot bypass the explicit editable-text submission boundary. */
internal object VoiceEntryText {
    const val MAX_LENGTH = 600
    fun validated(value: String?): String? = value?.takeIf {
        it.length <= MAX_LENGTH && it.none { c -> c.isISOControl() && c !in "\n\r\t" }
    }?.trim()?.takeIf(String::isNotEmpty)
}
