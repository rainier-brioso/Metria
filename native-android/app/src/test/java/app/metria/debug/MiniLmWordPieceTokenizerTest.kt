package app.metria.debug

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class MiniLmWordPieceTokenizerTest {
    @Test fun `pinned vocabulary matches Hugging Face tokenizer for chat lookup wording`() {
        // Expected IDs produced independently with tokenizers.BertWordPieceTokenizer
        // and the pinned upstream vocabulary, not with this Kotlin implementation.
        val actualTokenizer = File("src/debug/assets/decision-model/minilm/vocab.txt")
            .inputStream().use(MiniLmWordPieceTokenizer::fromVocabulary)
        listOf(
            "Show the meals I recorded today" to longArrayOf(101, 2265, 1996, 12278, 1045, 2680, 2651, 102),
            "What did I log for meals today?" to longArrayOf(101, 2054, 2106, 1045, 8833, 2005, 12278, 2651, 1029, 102),
            "Show today's saved meals" to longArrayOf(101, 2265, 2651, 1005, 1055, 5552, 12278, 102),
            "Please list my meals for today" to longArrayOf(101, 3531, 2862, 2026, 12278, 2005, 2651, 102),
        ).forEach { (text, ids) -> assertArrayEquals(text, ids, actualTokenizer.encode(text)!!.inputIds) }
    }

    private val tokenizer = MiniLmWordPieceTokenizer(
        listOf(
            "[PAD]", "[UNK]", "[CLS]", "[SEP]", "show", "the", "meals", "i", "recorded", "today",
            "'", "s", "cafe", "中", ",", "##ing", "play"
        )
    )

    @Test fun `lowercase sentence gets cls sep and wordpiece ids`() {
        val encoded = tokenizer.encode("SHOW the meals I recorded today")!!
        assertArrayEquals(longArrayOf(2, 4, 5, 6, 7, 8, 9, 3), encoded.inputIds)
        assertArrayEquals(LongArray(8) { 1 }, encoded.attentionMask)
        assertArrayEquals(LongArray(8), encoded.tokenTypeIds)
    }

    @Test fun `punctuation splits contractions and accents are stripped`() {
        val encoded = tokenizer.encode("café, today's")!!
        assertArrayEquals(longArrayOf(2, 12, 14, 9, 10, 11, 3), encoded.inputIds)
    }

    @Test fun `Chinese characters are isolated as basic tokens`() {
        val encoded = tokenizer.encode("中")!!
        assertArrayEquals(longArrayOf(2, 13, 3), encoded.inputIds)
    }

    @Test fun `unknown words map to unk while overlong inputs fail closed`() {
        val encoded = tokenizer.encode("unknown")!!
        assertArrayEquals(longArrayOf(2, 1, 3), encoded.inputIds)
        assertNull(tokenizer.encode(List(255) { "show" }.joinToString(" ")))
        assertNull(tokenizer.encode("\u0000"))
    }
}
