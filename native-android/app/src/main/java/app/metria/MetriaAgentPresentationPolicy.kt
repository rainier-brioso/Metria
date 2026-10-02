package app.metria

/** Presentation-only correction after factual auditing; it cannot supply record evidence. */
internal object MetriaAgentPresentationPolicy {
    private val ownedRecords = Regex("\\b(my records|mis registros)\\b", RegexOption.IGNORE_CASE)

    fun memberOwnedRecords(text: String): String = ownedRecords.replace(text) { match ->
        val replacement = if (match.value.contains("registros", ignoreCase = true)) {
            "tus registros"
        } else {
            "your records"
        }
        if (match.value.first().isUpperCase()) replacement.replaceFirstChar(Char::uppercaseChar) else replacement
    }
}
