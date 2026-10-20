package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVoiceDraftHandoffTest {
    private val proposal = MealProposal(
        items = listOf(MealProposalItem(
            name = "rice", description = "plain", visiblePortion = "one serving",
            estimatedGramsRange = GramRange(100, 180), preparation = "cooked",
            confidence = "medium", evidence = "member said rice",
        )),
        assumptions = emptyList(), questions = emptyList(), overallConfidence = "medium",
        omittedItemCount = 0, partialCategory = null,
    )
    private val portion = ChatCurrentDraftPortionContext(
        sourceRequestId = 41, conversationGeneration = 7, originalProposal = proposal,
        target = ChatPortionTarget(0, "rice", 100, 180), currentGrams = 150,
    )
    private val gramsOverride = ChatPortionOverride(41, 7, proposal, 0, "rice", 150)

    private fun handoff(token: Long = 9, override: ChatPortionOverride? = null) = requireNotNull(
        OneUseChatVoiceDraftHandoff.capture(token, null, portion, null, override),
    )

    private fun current(
        binding: OneUseChatVoiceDraftHandoff,
        token: Long = 9,
        context: ChatCurrentDraftPortionContext? = portion,
        active: Long? = 41,
        handled: Long? = 41,
        generation: Long = 7,
        override: ChatPortionOverride? = null,
        unlocked: Boolean = true,
        stopping: Boolean = false,
        pending: Boolean = false,
    ) = binding.isCurrent(
        token, null, context, null, override, active, handled, generation,
        unlocked, stopping, pending,
    )

    @Test fun `eligible portion draft can be previewed then consumed once`() {
        val binding = handoff()
        assertTrue(current(binding))
        assertTrue(binding.consume(
            9, "Actually, make that 200 g", null, portion, null, null,
            41, 41, 7, true, false, false,
        ))
        assertFalse(current(binding))
        assertFalse(binding.consume(
            9, "150 g", null, portion, null, null,
            41, 41, 7, true, false, false,
        ))
    }

    @Test fun `revoked handoff cannot authorize a later preview or submission`() {
        val binding = handoff()
        binding.revoke()
        assertFalse(current(binding))
        assertFalse(binding.consume(
            9, "Actually, make that 200 g", null, portion, null, null,
            41, 41, 7, true, false, false,
        ))
    }

    @Test fun `voice handoff rejects stale identities overrides and lifecycle states`() {
        assertFalse(current(handoff(), token = 10))
        assertFalse(current(handoff(), active = 42))
        assertFalse(current(handoff(), handled = null))
        assertFalse(current(handoff(), generation = 8))
        assertFalse(current(handoff(), context = portion.copy(currentGrams = 200)))
        assertFalse(current(handoff(override = gramsOverride), override = gramsOverride.copy(grams = 200)))
        assertFalse(current(handoff(), unlocked = false))
        assertFalse(current(handoff(), stopping = true))
        assertFalse(current(handoff(), pending = true))
    }

    @Test fun `invalid or empty member preview burns the one use authority`() {
        val binding = handoff()
        assertFalse(binding.consume(
            9, "\u0000", null, portion, null, null,
            41, 41, 7, true, false, false,
        ))
        assertFalse(current(binding))
        assertNull(OneUseChatVoiceDraftHandoff.capture(1, null, null, null, null))
    }

    @Test fun `food and portion contexts must refer to same proposal and turn`() {
        val food = ChatCurrentDraftFoodContext(
            sourceRequestId = 41, conversationGeneration = 7, originalProposal = proposal,
            itemIndex = 0, originalItemName = "rice", currentReplacementName = null, currentGrams = 150,
        )
        assertNotNull(OneUseChatVoiceDraftHandoff.capture(9, food, portion, null, null))
        assertNull(OneUseChatVoiceDraftHandoff.capture(
            9, food.copy(sourceRequestId = 42), portion, null, null,
        ))
    }
}

class ChatVoiceRequestProofTest {
    private fun proof() = OneUseChatVoiceRequestProof.capture(
        captureToken = 12,
        modelRequestId = null,
        activeRequestId = null,
        handledReplyRequestId = null,
        conversationGeneration = 7,
    )

    @Test fun `allows one valid generic preview without an active model request`() {
        val proof = proof()
        assertTrue(proof.isCurrent(12, null, null, null, 7, true, false, false))
        assertTrue(proof.consume(12, "I had brown rice", null, null, null, 7, true, false, false))
        assertFalse(proof.consume(12, "I had brown rice", null, null, null, 7, true, false, false))
    }

    @Test fun `rejects a transcript after source identity or session state changes`() {
        assertFalse(proof().isCurrent(12, 41, null, null, 7, true, false, false))
        assertFalse(proof().isCurrent(12, null, 42, null, 7, true, false, false))
        assertFalse(proof().isCurrent(12, null, null, null, 8, true, false, false))
        assertFalse(proof().isCurrent(12, null, null, null, 7, false, false, false))
        assertFalse(proof().isCurrent(12, null, null, null, 7, true, true, false))
        assertFalse(proof().isCurrent(12, null, null, null, 7, true, false, true))
        assertFalse(proof().isCurrent(13, null, null, null, 7, true, false, false))
    }

    @Test fun `invalid text and revocation consume authority`() {
        val invalid = proof()
        assertFalse(invalid.consume(12, "\u0000", null, null, null, 7, true, false, false))
        assertFalse(invalid.consume(12, "rice", null, null, null, 7, true, false, false))
        val revoked = proof()
        revoked.revoke()
        assertFalse(revoked.isCurrent(12, null, null, null, 7, true, false, false))
    }

    private fun typingExit() = proof().typingExitReceipt()

    @Test fun `Stop receipt permits only a same-session Type instead exit once`() {
        val exit = typingExit()
        assertTrue(exit.isCurrent(12, null, null, null, 7, true, false, false))
        assertTrue(exit.consume(12, null, null, null, 7, true, false, false))
        assertFalse(exit.consume(12, null, null, null, 7, true, false, false))
    }

    @Test fun `Stop receipt rejects stale request session lock and newer capture`() {
        assertFalse(typingExit().consume(12, 41, null, null, 7, true, false, false))
        assertFalse(typingExit().consume(12, null, 42, null, 7, true, false, false))
        assertFalse(typingExit().consume(12, null, null, null, 8, true, false, false))
        assertFalse(typingExit().consume(12, null, null, null, 7, false, false, false))
        assertFalse(typingExit().consume(13, null, null, null, 7, true, false, false))
    }
}

class VoiceTranscriptParserTest {
    @Test fun `accepts natural plain transcriptions including food corrections and portions`() {
        assertEquals("Actually, it was brown rice", VoiceTranscriptParser.parse("Actually, it was brown rice"))
        assertEquals("200 grams", VoiceTranscriptParser.parse("200 grams"))
        assertEquals("I had scrambled eggs and toast", VoiceTranscriptParser.parse(" I had scrambled eggs and toast "))
    }

    @Test fun `rejects empty oversized multiline and control text`() {
        assertTrue(VoiceTranscriptParser.classify("") is VoiceTranscriptParseResult.NoText)
        assertTrue(VoiceTranscriptParser.classify("  ") is VoiceTranscriptParseResult.NoText)
        assertTrue(VoiceTranscriptParser.classify(" \r\nI had rice\r\n ") is VoiceTranscriptParseResult.Accepted)
        assertTrue(VoiceTranscriptParser.classify("one line\nsecond line") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("\u0000rice") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("rice\u0000") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("\trice") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("rice\t") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("word\u0001word") is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify("x".repeat(VoiceEntryText.MAX_LENGTH + 1)) is VoiceTranscriptParseResult.InvalidTranscript)
        assertTrue(VoiceTranscriptParser.classify(" ".repeat(8_193)) is VoiceTranscriptParseResult.InvalidTranscript)
    }

    @Test fun `rejects obvious structured action and formatted output without extracting text`() {
        listOf(
            "{\"spoken_text\":\"rice\"}",
            "[tool_call: draft_meal]",
            "```rice```",
            "<transcript>rice</transcript>",
            "ACTION: draft meal",
            "TOOL_CALL: create meal",
            "FUNCTION_CALL: draft_meal",
        ).forEach { output ->
            assertTrue(output, VoiceTranscriptParser.classify(output) is VoiceTranscriptParseResult.StructuredOutput)
            assertNull(VoiceTranscriptParser.parse(output))
        }
    }
}
