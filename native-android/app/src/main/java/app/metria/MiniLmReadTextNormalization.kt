package app.metria

import java.text.Normalizer
import java.util.Locale

/** Shared bounded text cleanup for the closed MiniLM read grammars. */
internal object MiniLmReadTextNormalization {
    fun normalize(text: String): String {
        var normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFKC)
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', '?', '!', ',', ';', ':', ' ')

        // Remove one complete, anchored politeness wrapper. Request content stays intact.
        normalized = normalized.replace(Regex("^(?:can|could|would) you (?:please )?"), "")
        if (normalized.startsWith("please ")) normalized = normalized.removePrefix("please ")
        normalized = normalized.replace(Regex(",? please$"), "")

        // Narrow spelling aliases used by the closed recent-weigh-in grammar.
        normalized = normalized
            .replace(Regex("\\bwigh(?=[- ]ins?\\b)"), "weigh")
            .replace(Regex("\\bresent(?= weigh[- ]ins?\\b)"), "recent")
            .replace(Regex("\\b(latest|most recent|last recorded) wight\\b"), "$1 weight")
            .replace(Regex("\\bweight hystory\\b"), "weight history")
        return normalized
    }
}
