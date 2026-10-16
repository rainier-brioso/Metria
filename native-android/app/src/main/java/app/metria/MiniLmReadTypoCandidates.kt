package app.metria

/**
 * Generates one conservative correction candidate for the closed English read
 * grammars. This is a routing vocabulary, not a general-purpose spellchecker.
 */
internal object MiniLmReadTypoCandidates {
    internal val vocabulary = setOf(
        "overview", "summary", "meals", "meal", "recorded", "logged", "calories",
        "calorie", "protein", "weight", "weigh", "history", "latest", "recent",
        "total", "remaining",
    )
    // Valid English words close to routing terms must never be rewritten as typos.
    internal val protectedWords = setOf("height", "heights", "meat", "meats", "protean", "lodged", "lamest")
    private val tokenPattern = Regex("(?<![a-z])([a-z]+)(?![a-z])")

    /** Returns a corrected phrase only when exactly one token has one nearest candidate. */
    fun uniqueSingleCorrection(normalizedText: String): String? {
        val tokens = tokenPattern.findAll(normalizedText).toList()
        var correction: Pair<IntRange, String>? = null
        for (token in tokens) {
            val word = token.groupValues[1]
            if (word.length < MIN_TOKEN_LENGTH || word in vocabulary || word in protectedWords) continue
            val nearest = localCorrectionsFor(word)
            if (nearest.isEmpty()) continue
            if (nearest.size != 1 || correction != null) return null
            correction = token.groups[1]!!.range to nearest.single()
        }
        val (range, replacement) = correction ?: return null
        return buildString(normalizedText.length + replacement.length - range.count()) {
            append(normalizedText, 0, range.first)
            append(replacement)
            append(normalizedText, range.last + 1, normalizedText.length)
        }
    }

    /** Conservative one-edit local alternatives, exposed so bounded multiword plans can compose them. */
    fun localCorrectionsFor(word: String): Set<String> = localCorrectionsFor(word, vocabulary)

    /** Reuses the same conservative edit rule for a caller's small, closed vocabulary. */
    fun localCorrectionsFor(
        word: String,
        targets: Set<String>,
        minimumLength: Int = MIN_TOKEN_LENGTH,
    ): Set<String> = if (word.length < minimumLength) emptySet() else targets
        .filterTo(linkedSetOf()) { optimalStringAlignmentDistanceAtMostOne(word, it) }

    /** One insertion, deletion, adjacent transposition, or nearby-key substitution. */
    private fun optimalStringAlignmentDistanceAtMostOne(left: String, right: String): Boolean {
        if (kotlin.math.abs(left.length - right.length) > 1) return false
        if (left.length == right.length) {
            val mismatches = left.indices.filter { left[it] != right[it] }
            return when (mismatches.size) {
                1 -> isNearbyKeyboardKey(left[mismatches.single()], right[mismatches.single()])
                2 -> {
                    val first = mismatches[0]
                    val second = mismatches[1]
                    second == first + 1 && left[first] == right[second] && left[second] == right[first]
                }
                else -> false
            }
        }
        val longer = if (left.length > right.length) left else right
        val shorter = if (left.length > right.length) right else left
        var longIndex = 0
        var shortIndex = 0
        var skipped = false
        while (longIndex < longer.length && shortIndex < shorter.length) {
            if (longer[longIndex] == shorter[shortIndex]) {
                longIndex++
                shortIndex++
            } else if (skipped) {
                return false
            } else {
                skipped = true
                longIndex++
            }
        }
        return true
    }

    /** QWERTY row/column proximity limits substitution fixes to likely finger slips. */
    private fun isNearbyKeyboardKey(left: Char, right: Char): Boolean {
        val leftPosition = keyboardPosition(left) ?: return false
        val rightPosition = keyboardPosition(right) ?: return false
        val rowDistance = kotlin.math.abs(leftPosition.first - rightPosition.first)
        val columnDistance = kotlin.math.abs(leftPosition.second - rightPosition.second)
        return if (rowDistance == 0) columnDistance == 1.0
        else rowDistance == 1 && columnDistance <= 1.0
    }

    private fun keyboardPosition(key: Char): Pair<Int, Double>? {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val offsets = listOf(0.0, 0.25, 0.75)
        rows.forEachIndexed { row, keys ->
            val column = keys.indexOf(key)
            if (column >= 0) return row to (offsets[row] + column)
        }
        return null
    }

    private const val MIN_TOKEN_LENGTH = 5
}
