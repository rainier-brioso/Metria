package app.metria

import java.text.Normalizer
import java.util.Locale

/** Concrete supported language for both native prompts and host-validated replies. */
internal fun coachReplyLanguage(text: String, conversationContext: String = ""): String {
    fun detect(value: String): String? {
        val normalized = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        val spanish = Regex("""\b(?:que|cual|cuales|cuanto|cuanta|cuantos|cuantas|como|comi|peso|semana|meta|metas|progreso|hoy|ayer|mi|mis|ultima|ultimo|comida|comidas|calorias|proteina|arroz|lentejas|si|vale)\b""")
        val english = Regex("""\b(?:what|which|how|when|does|did|should|my|last|latest|meal|meals|calories|protein|weight|today|yesterday|yes|okay)\b""")
        return when {
            spanish.containsMatchIn(normalized) -> "Spanish"
            english.containsMatchIn(normalized) -> "English"
            else -> null
        }
    }
    // The current member message leads; older assistant prose cannot override a clear switch.
    detect(text)?.let { return it }
    val recentMemberLanguage = conversationContext.lineSequence().toList().asReversed()
        .filter { it.startsWith("Member:", ignoreCase = true) }
        .firstNotNullOfOrNull { detect(it.substringAfter(':')) }
    return recentMemberLanguage ?: detect(conversationContext) ?: "English"
}
