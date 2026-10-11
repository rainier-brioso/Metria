package app.metria.debug

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

/** Minimal uncased BERT tokenizer for the pinned MiniLM vocabulary. */
internal class MiniLmWordPieceTokenizer(vocabulary: List<String>) {
    private val ids: Map<String, Int> = vocabulary.withIndex().associate { it.value to it.index }
    val padTokenId: Long get() = id(PAD)

    init {
        require(vocabulary.size in 1..MAX_VOCAB_SIZE) { "MiniLM vocabulary size is invalid" }
        require(ids.size == vocabulary.size) { "MiniLM vocabulary contains duplicate entries" }
        require(listOf(PAD, UNK, CLS, SEP).all(ids::containsKey)) { "MiniLM special tokens are missing" }
    }

    fun encode(text: String): EncodedText? {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any(::isForbiddenControl)) return null
        val pieces = basicTokenize(text).flatMap(::wordPiece)
        if (pieces.isEmpty() || pieces.size > MAX_SEQUENCE_LENGTH - 2) return null
        val tokenIds = LongArray(pieces.size + 2)
        tokenIds[0] = id(CLS)
        pieces.forEachIndexed { index, piece -> tokenIds[index + 1] = id(piece) }
        tokenIds[tokenIds.lastIndex] = id(SEP)
        return EncodedText(tokenIds, LongArray(tokenIds.size) { 1L }, LongArray(tokenIds.size))
    }

    private fun basicTokenize(text: String): List<String> {
        val lowered = stripAccents(text.lowercase(Locale.ROOT))
        val result = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                result += current.toString()
                current.setLength(0)
            }
        }
        for (character in lowered) {
            when {
                isWhitespace(character) || Character.getType(character) == Character.FORMAT.toInt() -> flush()
                isPunctuation(character) -> {
                    flush()
                    result += character.toString()
                }
                isChineseCharacter(character) -> {
                    flush()
                    result += character.toString()
                }
                else -> current.append(character)
            }
        }
        flush()
        return result
    }

    private fun wordPiece(token: String): List<String> {
        if (token.length > MAX_WORD_CHARS) return listOf(UNK)
        val result = mutableListOf<String>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var found: String? = null
            while (end > start) {
                val raw = token.substring(start, end)
                val candidate = if (start == 0) raw else "##$raw"
                if (ids.containsKey(candidate)) {
                    found = candidate
                    break
                }
                end--
            }
            if (found == null) return listOf(UNK)
            result += found
            start = end
        }
        return result
    }

    private fun id(token: String): Long = (ids[token] ?: ids.getValue(UNK)).toLong()

    private fun isForbiddenControl(character: Char): Boolean =
        Character.getType(character) == Character.CONTROL.toInt() && !character.isWhitespace()

    private fun isPunctuation(character: Char): Boolean =
        when (Character.getType(character)) {
            Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt() -> true
            else -> false
        }

    private fun isWhitespace(character: Char): Boolean =
        Character.isWhitespace(character) || Character.isSpaceChar(character)

    private fun isChineseCharacter(character: Char): Boolean = when (character.code) {
        in 0x4E00..0x9FFF, in 0x3400..0x4DBF, in 0x20000..0x2A6DF,
        in 0x2A700..0x2B73F, in 0x2B740..0x2B81F, in 0x2B820..0x2CEAF,
        in 0xF900..0xFAFF, in 0x2F800..0x2FA1F -> true
        else -> false
    }

    private fun stripAccents(value: String): String = buildString(value.length) {
        for (character in Normalizer.normalize(value, Normalizer.Form.NFD)) {
            if (Character.getType(character) != Character.NON_SPACING_MARK.toInt()) append(character)
        }
    }

    data class EncodedText(val inputIds: LongArray, val attentionMask: LongArray, val tokenTypeIds: LongArray)

    companion object {
        private const val PAD = "[PAD]"
        private const val UNK = "[UNK]"
        private const val CLS = "[CLS]"
        private const val SEP = "[SEP]"
        private const val MAX_TEXT_LENGTH = 500
        private const val MAX_SEQUENCE_LENGTH = 256
        private const val MAX_WORD_CHARS = 100
        private const val MAX_VOCAB_SIZE = 100_000

        fun fromVocabulary(input: InputStream): MiniLmWordPieceTokenizer {
            val vocabulary = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                reader.readLines()
            }
            return MiniLmWordPieceTokenizer(vocabulary)
        }
    }
}
