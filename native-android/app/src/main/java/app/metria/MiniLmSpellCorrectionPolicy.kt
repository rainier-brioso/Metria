package app.metria

import java.time.LocalDate
import java.util.Locale

/** Privacy-safe statuses. These values may be written to assistant metadata; never add text here. */
internal enum class SpellCheckReason {
    ACCEPTED, NO_CANDIDATE, UNAVAILABLE, DISABLED, NO_SUGGESTIONS, TIMEOUT,
    AMBIGUOUS, INTERRUPTED, RUNTIME_FAILURE,
    EMPTY_RESPONSE, SEGMENT_MISMATCH, REQUEST_MISMATCH, DICTIONARY_WORD,
    UNMARKED_TYPO, NO_MATCH,
}

internal data class SpellSuggestion(
    val word: String,
    val offset: Int,
    val length: Int,
    val looksLikeTypo: Boolean,
    val inDictionary: Boolean,
)

internal data class SpellCheckResult(
    val reason: SpellCheckReason,
    val suggestions: List<SpellSuggestion> = emptyList(),
)

/** Android only receives an isolated candidate word. The default keeps pure test providers simple. */
internal fun interface EnglishSpellChecker {
    fun check(token: String, isActive: () -> Boolean): SpellCheckResult

    fun checkWithin(token: String, isActive: () -> Boolean, budgetMillis: Long): SpellCheckResult =
        check(token, isActive)
}

internal data class SpellCorrectionResolution(
    val resolvedRead: MiniLmMealReadPolicy.ResolvedRead?,
    val reason: SpellCheckReason,
    val providerAttempted: Boolean,
)

/** Bounded typo recovery for existing, complete MiniLM read grammars. */
internal object MiniLmSpellCorrectionPolicy {
    private val wordPattern = Regex("[a-zA-Z]+")
    private val grammarTargets = setOf("show", "list", "display", "view", "give", "tell", "please")
    private val dateTerms = setOf(
        "tomorrow", "tonight", "yesterday", "today", "monday", "tuesday", "wednesday", "thursday",
        "friday", "saturday", "sunday", "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
    )
    private val protected = MiniLmReadTypoCandidates.protectedWords + dateTerms + setOf(
        "add", "log", "logged", "record", "recorded", "save", "saved", "track", "tracked", "ate", "eaten",
        "create", "update", "change", "delete", "remove", "set", "note", "remember", "and", "or", "not",
        "he", "she", "they", "we", "friend", "someone", "their", "his", "her",
    )
    private val localTargets = MiniLmReadTypoCandidates.vocabulary + grammarTargets
    private val checkerTargets = localTargets + setOf("today", "yesterday") + dateTerms

    private data class Token(val text: String, val range: IntRange)
    private data class Variant(val replacement: String, val source: Source)
    private enum class Source { ORIGINAL, LOCAL, SYSTEM }
    private data class Interpretation(
        val text: String,
        val read: MiniLmMealReadPolicy.ResolvedRead,
        val key: SemanticKey,
        val providerIndexes: Set<Int>,
    )
    private data class SemanticKey(
        val tool: RequiredAgentTool,
        val classifierKey: String,
        val companionKind: CompanionAssistantPolicy.IntentKind?,
    )

    /**
     * Local, high-confidence edits are tried first. Broader provider suggestions are requested
     * only for isolated tokens which can complete the full host-constrained read grammar.
     */
    fun resolve(
        hostRequiredTool: RequiredAgentTool?,
        originalText: String,
        selectedMealId: String?,
        today: LocalDate,
        checker: EnglishSpellChecker,
        nowNanos: () -> Long = System::nanoTime,
        isActive: () -> Boolean,
    ): SpellCorrectionResolution {
        if (!selectedMealId.isNullOrBlank() || !isActive()) return abstain(SpellCheckReason.NO_CANDIDATE)
        val tokens = tokenize(originalText) ?: return abstain(SpellCheckReason.NO_CANDIDATE)
        val localOptions = tokens.map { token -> localVariants(token.text) }
        val localReads = enumerate(originalText, tokens, localOptions, hostRequiredTool, today)
        if (localReads.overflow) return abstain(SpellCheckReason.NO_CANDIDATE)
        unique(localReads.interpretations)?.let { return accepted(it, attempted = false) }
        if (localReads.interpretations.size > 1) return abstain(SpellCheckReason.AMBIGUOUS)

        val possibleOptions = potentialProviderOptions(tokens, localOptions)
            ?: return abstain(SpellCheckReason.NO_CANDIDATE)
        val potentialReads = enumerate(originalText, tokens, possibleOptions, hostRequiredTool, today)
        if (potentialReads.overflow) return abstain(SpellCheckReason.NO_CANDIDATE)
        val candidateIndexes = potentialReads.interpretations.flatMap { it.providerIndexes }.distinct().sorted()
        if (candidateIndexes.isEmpty() || candidateIndexes.size > MAX_PROVIDER_WORDS) {
            return abstain(SpellCheckReason.NO_CANDIDATE)
        }

        val startedNanos = nowNanos()
        val providerOptions = localOptions.map { it.toMutableList() }
        var lastFailure = SpellCheckReason.NO_SUGGESTIONS
        var attempted = false
        for (index in candidateIndexes) {
            if (!isActive() || Thread.currentThread().isInterrupted) {
                return abstain(SpellCheckReason.INTERRUPTED, attempted)
            }
            val remainingMillis = TOTAL_DEADLINE_MILLIS - elapsedMillis(startedNanos, nowNanos())
            if (remainingMillis <= 0) return abstain(SpellCheckReason.TIMEOUT, attempted)
            attempted = true
            val token = tokens[index].text
            val response = try {
                checker.checkWithin(token, isActive, remainingMillis)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return abstain(SpellCheckReason.INTERRUPTED, attempted)
            } catch (_: Exception) {
                lastFailure = SpellCheckReason.RUNTIME_FAILURE
                continue
            }
            if (!isActive() || Thread.currentThread().isInterrupted) {
                return abstain(SpellCheckReason.INTERRUPTED, attempted)
            }
            if (elapsedMillis(startedNanos, nowNanos()) >= TOTAL_DEADLINE_MILLIS) return abstain(SpellCheckReason.TIMEOUT, attempted)
            if (response.reason != SpellCheckReason.ACCEPTED) {
                lastFailure = response.reason
                continue
            }
            val bounded = response.suggestions.take(MAX_SUGGESTIONS).filter {
                it.word.length in 1..MAX_WORD_LENGTH && it.offset == 0 && it.length == token.length
            }
            if (bounded.isEmpty()) {
                lastFailure = if (response.suggestions.isEmpty()) SpellCheckReason.NO_SUGGESTIONS else SpellCheckReason.SEGMENT_MISMATCH
                continue
            }
            val eligible = bounded.filter { it.looksLikeTypo && !it.inDictionary }.mapNotNull { suggestion ->
                val replacement = suggestion.word.lowercase(Locale.ROOT)
                if (replacement !in checkerTargets || replacement == token.lowercase(Locale.ROOT) ||
                    editDistanceAtMostTwo(token.lowercase(Locale.ROOT), replacement) > MAX_PROVIDER_EDIT_DISTANCE
                ) null
                else Variant(preserveCase(token, replacement), Source.SYSTEM)
            }.distinctBy { it.replacement.lowercase(Locale.ROOT) }
            if (eligible.isEmpty()) {
                lastFailure = when {
                    bounded.any { it.inDictionary } -> SpellCheckReason.DICTIONARY_WORD
                    bounded.any { it.looksLikeTypo } -> SpellCheckReason.NO_MATCH
                    else -> SpellCheckReason.UNMARKED_TYPO
                }
                continue
            }
            providerOptions[index].addAll(eligible)
            lastFailure = SpellCheckReason.NO_MATCH
        }

        if (!attempted) return abstain(SpellCheckReason.NO_CANDIDATE)
        if (!isActive() || Thread.currentThread().isInterrupted) return abstain(SpellCheckReason.INTERRUPTED, attempted)
        if (elapsedMillis(startedNanos, nowNanos()) >= TOTAL_DEADLINE_MILLIS) return abstain(SpellCheckReason.TIMEOUT, attempted)
        val totalProduct = productSize(providerOptions) ?: return abstain(SpellCheckReason.NO_CANDIDATE, attempted)
        if (totalProduct > MAX_COMBINATIONS) return abstain(SpellCheckReason.NO_CANDIDATE, attempted)
        val results = enumerate(originalText, tokens, providerOptions, hostRequiredTool, today)
        if (results.overflow) return abstain(SpellCheckReason.NO_CANDIDATE, attempted)
        val interpretations = results.interpretations
        if (interpretations.size > 1) return abstain(SpellCheckReason.AMBIGUOUS, attempted)
        val selected = interpretations.singleOrNull() ?: return abstain(lastFailure, attempted)
        if (selected.text == originalText) return abstain(lastFailure, attempted)
        return accepted(selected, attempted)
    }

    /** Pure migration-gate check; it never calls Android or inherits a follow-up scope. */
    fun hasUniqueEligibleCandidate(originalText: String, today: LocalDate = LocalDate.now()): Boolean {
        val tokens = tokenize(originalText) ?: return false
        val local = tokens.map { localVariants(it.text) }
        val product = productSize(local) ?: return false
        if (product > MAX_COMBINATIONS) return false
        val reads = enumerate(originalText, tokens, local, null, today)
        if (reads.overflow) return false
        if (reads.interpretations.size == 1) return true
        if (reads.interpretations.size > 1) return true
        val possible = potentialProviderOptions(tokens, local) ?: return false
        val potential = enumerate(originalText, tokens, possible, null, today)
        if (potential.overflow) return false
        val candidates = potential.interpretations.flatMap { it.providerIndexes }.distinct()
        return candidates.isNotEmpty() && candidates.size <= MAX_PROVIDER_WORDS
    }

    private data class Enumeration(val interpretations: List<Interpretation>, val overflow: Boolean)

    private fun enumerate(
        original: String,
        tokens: List<Token>,
        options: List<List<Variant>>,
        host: RequiredAgentTool?,
        today: LocalDate,
    ): Enumeration {
        val product = productSize(options) ?: return Enumeration(emptyList(), true)
        if (product > MAX_COMBINATIONS) return Enumeration(emptyList(), true)
        val meanings = LinkedHashMap<SemanticKey, Interpretation>()
        val indexes = IntArray(tokens.size)
        repeat(product) {
            val builder = StringBuilder(original)
            for (index in tokens.indices.reversed()) {
                val variant = options[index][indexes[index]]
                if (variant.source != Source.ORIGINAL) {
                    builder.replace(tokens[index].range.first, tokens[index].range.last + 1, variant.replacement)
                }
            }
            val corrected = builder.toString()
            if (corrected != original) {
                val read = MiniLmMealReadPolicy.resolveReadRequestWithoutTypo(host, corrected, null, today, null)
                    ?.takeIf(::eligible)
                if (read != null && changedTokenCount(indexes, options) <= MAX_CHANGED_WORDS) {
                    val key = semanticKey(read)
                    val providerIndexes = tokens.indices.filter { options[it][indexes[it]].source == Source.SYSTEM }.toSet()
                    val existing = meanings[key]
                    meanings[key] = if (existing == null) Interpretation(corrected, read, key, providerIndexes)
                    else existing.copy(providerIndexes = existing.providerIndexes + providerIndexes)
                }
            }
            increment(indexes, options)
        }
        return Enumeration(meanings.values.toList(), false)
    }

    private fun potentialProviderOptions(
        tokens: List<Token>,
        localOptions: List<List<Variant>>,
    ): List<List<Variant>>? {
        val options = tokens.indices.map { index ->
            val token = tokens[index].text
            val broader = checkerVariants(token).map { Variant(preserveCase(token, it), Source.SYSTEM) }
            (localOptions[index] + broader).distinctBy { it.replacement.lowercase(Locale.ROOT) }
        }
        val product = productSize(options) ?: return null
        return options.takeIf { product <= MAX_COMBINATIONS }
    }

    private fun localVariants(token: String): List<Variant> {
        val lower = token.lowercase(Locale.ROOT)
        if (lower in protected || lower in localTargets || token.length !in 3..MAX_WORD_LENGTH) {
            return listOf(Variant(token, Source.ORIGINAL))
        }
        val candidates = linkedSetOf<String>()
        if (lower.length >= MIN_PREFIX_LENGTH) {
            candidates += localTargets.filter { it.length > lower.length && it.startsWith(lower) }
        }
        MiniLmReadTypoCandidates.localCorrectionsFor(lower).forEach(candidates::add)
        val filtered = candidates.filter { it in localTargets }
        return listOf(Variant(token, Source.ORIGINAL)) + filtered.map { Variant(preserveCase(token, it), Source.LOCAL) }
    }

    private fun checkerVariants(token: String): List<String> {
        val lower = token.lowercase(Locale.ROOT)
        if (lower in protected || lower in localTargets || token.length !in 3..MAX_WORD_LENGTH) return emptyList()
        return checkerTargets.filter { candidate ->
            candidate != lower && editDistanceAtMostTwo(lower, candidate) <= MAX_PROVIDER_EDIT_DISTANCE
        }
    }

    private fun tokenize(text: String): List<Token>? {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any { it.code < 0x20 && !it.isWhitespace() }) return null
        val matches = wordPattern.findAll(text).toList()
        if (matches.isEmpty() || matches.size > MAX_TOKENS) return null
        return matches.map { Token(it.value, it.range) }
    }

    private fun unique(values: List<Interpretation>): Interpretation? = values.singleOrNull()

    private fun semanticKey(read: MiniLmMealReadPolicy.ResolvedRead) = SemanticKey(
        read.requiredTool, read.classifierKey, read.companion?.kind,
    )

    private fun eligible(read: MiniLmMealReadPolicy.ResolvedRead): Boolean =
        read.allowMiniLm && read.companion == null && read.requiredTool.name in ELIGIBLE_TOOLS

    private fun accepted(value: Interpretation, attempted: Boolean) =
        SpellCorrectionResolution(value.read, SpellCheckReason.ACCEPTED, attempted)

    private fun abstain(reason: SpellCheckReason, attempted: Boolean = false) =
        SpellCorrectionResolution(null, reason, attempted)

    private fun productSize(options: List<List<Variant>>): Int? {
        var product = 1
        for (values in options) {
            if (values.isEmpty()) return null
            if (product > MAX_COMBINATIONS / values.size) return MAX_COMBINATIONS + 1
            product *= values.size
        }
        return product
    }

    private fun increment(indexes: IntArray, options: List<List<Variant>>) {
        for (index in indexes.indices.reversed()) {
            indexes[index]++
            if (indexes[index] < options[index].size) return
            indexes[index] = 0
        }
    }

    private fun changedTokenCount(indexes: IntArray, options: List<List<Variant>>): Int =
        indexes.indices.count { options[it][indexes[it]].source != Source.ORIGINAL }

    private fun preserveCase(token: String, replacement: String): String = when {
        token.all(Char::isUpperCase) -> replacement.uppercase(Locale.ROOT)
        token.firstOrNull()?.isUpperCase() == true -> replacement.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> replacement
    }

    private fun elapsedMillis(startedNanos: Long, currentNanos: Long): Long =
        (currentNanos - startedNanos).coerceAtLeast(0L) / 1_000_000L

    private fun editDistanceAtMostTwo(left: String, right: String): Int {
        if (kotlin.math.abs(left.length - right.length) > 2) return 3
        var previous = IntArray(right.length + 1) { it }
        for (i in 1..left.length) {
            val current = IntArray(right.length + 1)
            current[0] = i
            var rowMinimum = current[0]
            for (j in 1..right.length) {
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + if (left[i - 1] == right[j - 1]) 0 else 1)
                rowMinimum = minOf(rowMinimum, current[j])
            }
            if (rowMinimum > MAX_PROVIDER_EDIT_DISTANCE) return 3
            previous = current
        }
        return previous[right.length]
    }

    private val ELIGIBLE_TOOLS = setOf("read_meals", "read_day_nutrition", "read_latest_meal", "read_weight")
    private const val MAX_TEXT_LENGTH = 500
    private const val MIN_PREFIX_LENGTH = 3
    private const val MAX_WORD_LENGTH = 24
    private const val MAX_TOKENS = 20
    private const val MAX_PROVIDER_WORDS = 3
    private const val MAX_CHANGED_WORDS = 3
    private const val MAX_SUGGESTIONS = 3
    private const val MAX_COMBINATIONS = 64
    private const val MAX_PROVIDER_EDIT_DISTANCE = 2
    private const val TOTAL_DEADLINE_MILLIS = 400L
}
