package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPortionClarificationPolicyTest {
    private fun proposal(
        items: List<MealProposalItem> = listOf(item("Toast", 30, 50), item("Egg", 40, 40)),
        omitted: Int = 0,
        partial: MealProposalOmissionCategory? = null,
    ) = MealProposal(items, emptyList(), emptyList(), "medium", omitted, partial)

    private fun item(name: String, min: Int, max: Int, group: String? = null) = MealProposalItem(
        name = name, description = "", visiblePortion = "", estimatedGramsRange = GramRange(min, max),
        preparation = "", confidence = "low", groupLabel = group,
    )

    @Test fun offersOnlyForSingleSimpleUncertainItemInOrdinaryChatDraft() {
        val p = proposal()
        assertEquals(ChatPortionTarget(0, "Toast", 30, 50), ChatPortionClarificationPolicy.target(p, "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(p, "meal", false))
        assertNull(ChatPortionClarificationPolicy.target(p, "saved_dish", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(listOf(item("Toast", 30, 50), item("Egg", 35, 55))), "meal", true))
    }

    @Test fun skipsPartialGroupedJoinedDuplicateAndCertainProposals() {
        assertNull(ChatPortionClarificationPolicy.target(proposal(omitted = 1), "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(partial = MealProposalOmissionCategory.MULTIPLE), "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(listOf(item("Toast and egg", 30, 50))), "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(listOf(item("Toast", 30, 50, "Breakfast"))), "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(listOf(item("Toast", 30, 50), item("toast", 40, 40))), "meal", true))
        assertNull(ChatPortionClarificationPolicy.target(proposal(listOf(item("Toast", 30, 30))), "meal", true))
    }

    @Test fun parsesOnlyExplicitBoundedWholeGramsAndClosedSkipReplies() {
        assertEquals(ChatPortionReply.Grams(120), ChatPortionClarificationPolicy.parseReply(" 120 grams "))
        assertEquals(ChatPortionReply.Grams(1), ChatPortionClarificationPolicy.parseReply("1g"))
        assertEquals(ChatPortionReply.Grams(2000), ChatPortionClarificationPolicy.parseReply("2000 g"))
        listOf("120", "1.5 g", "2,000 g", "0 g", "2001 grams", "-40 g", "120 kg", "about 120 g", "two hundred grams")
            .forEach { assertEquals(it, ChatPortionReply.InvalidQuantity, ChatPortionClarificationPolicy.parseReply(it)) }
        assertEquals(ChatPortionReply.Skip, ChatPortionClarificationPolicy.parseReply("I don't know"))
        assertEquals(ChatPortionReply.Skip, ChatPortionClarificationPolicy.parseReply("skip"))
        assertEquals(ChatPortionReply.Unrelated, ChatPortionClarificationPolicy.parseReply("What did I eat yesterday?"))
        assertEquals(ChatPortionReply.Unrelated, ChatPortionClarificationPolicy.parseReply("What did I eat 2 days ago?"))
        assertEquals(ChatPortionReply.Unrelated, ChatPortionClarificationPolicy.parseReply("Show my last 3 meals"))
        assertEquals(ChatPortionReply.Unrelated, ChatPortionClarificationPolicy.parseReply("How many grams are in an apple?"))
        assertEquals(ChatPortionReply.Unrelated, ChatPortionClarificationPolicy.parseReply("I had 150 g of toast"))
    }

    @Test fun overrideIsBoundToOriginalRequestProposalAndConversationAndPreservesEstimate() {
        val p = proposal()
        val target = requireNotNull(ChatPortionClarificationPolicy.target(p, "meal", true))
        val applied = requireNotNull(ChatPortionClarificationPolicy.overrideForReview(
            p, 17, 4, target, ChatPortionReply.Grams(120),
        ))
        assertEquals(GramRange(30, 50), p.items[0].estimatedGramsRange)
        assertTrue(ChatPortionClarificationPolicy.isCurrent(applied, p, 17, 4, true, false))
        assertFalse(ChatPortionClarificationPolicy.isCurrent(applied, p, 18, 4, true, false))
        assertFalse(ChatPortionClarificationPolicy.isCurrent(applied, p, 17, 5, true, false))
        assertFalse(ChatPortionClarificationPolicy.isCurrent(applied, p, 17, 4, false, false))
        assertFalse(ChatPortionClarificationPolicy.isCurrent(applied, p, 17, 4, true, true))
        assertFalse(ChatPortionClarificationPolicy.isOfferCurrent(
            ChatPortionClarificationOffer(17, 4, p, target), p.copy(assumptions = listOf("changed")), 17, 4, true, false,
        ))
        assertNull(ChatPortionClarificationPolicy.overrideForReview(p, 17, 4, target, ChatPortionReply.InvalidQuantity))
        assertNull(ChatPortionClarificationPolicy.overrideForReview(p, 17, 4, target, ChatPortionReply.Unrelated))
        assertNull(ChatPortionClarificationPolicy.overrideForReview(p, 17, 4, target, ChatPortionReply.Grams(2001)))
        val skipped = requireNotNull(ChatPortionClarificationPolicy.overrideForReview(p, 17, 4, target, ChatPortionReply.Skip))
        assertNull(skipped.grams)
    }

    @Test fun repeatedLiveDraftAmountAndSkipRetainThePreviousAcceptedValue() {
        val p = proposal(listOf(item("Rice", 120, 180)))
        val target = requireNotNull(ChatPortionClarificationPolicy.target(p, "agent_meal_draft", true))
        val context = ChatCurrentDraftPortionContext(17, 4, p, target, currentGrams = 15)
        assertEquals(ChatPortionReply.Grams(150), ChatPortionClarificationPolicy.parseReply("150 g"))
        assertEquals(150, ChatPortionClarificationPolicy.overrideForCurrentDraft(context, 17, 150)?.grams)
        assertEquals(ChatPortionReply.InvalidQuantity, ChatPortionClarificationPolicy.parseReply("150"))
        assertEquals(15, ChatPortionClarificationPolicy.overrideForCurrentDraft(context, 17, context.currentGrams)?.grams)
        val retainedForRetry = requireNotNull(
            ChatPortionClarificationPolicy.overrideForCurrentDraft(context, resultRequestId = 18, grams = context.currentGrams),
        )
        assertEquals(18, retainedForRetry.requestId)
        assertEquals(15, retainedForRetry.grams)
    }

    @Test fun typedCorrectionRequiresOneExplicitMatchingWholeGramLiteral() {
        assertEquals(22, ChatPortionClarificationPolicy.matchingTypedCorrection("Actually, make that 022 g", 22))
        assertEquals(200, ChatPortionClarificationPolicy.matchingTypedCorrection("Please change it to 200 grams.", 200))
        listOf(
            "Actually make it 200", "Actually make it 1.5 g", "Actually make it 100 to 200 g",
            "Actually make it -22 g", "Actually make it 22 kg", "Actually make it 22 g and add 50 g",
            "Actually make it 22 g, not 25 g",
        ).forEach { text ->
            val modelValue = if (text.endsWith("200 g")) 200 else 22
            assertNull(text, ChatPortionClarificationPolicy.matchingTypedCorrection(text, modelValue))
        }
        assertNull(ChatPortionClarificationPolicy.matchingTypedCorrection("Make it 2001 g", 2001))
    }

    @Test fun frozenEditContextMatchesTheLiveRequestGenerationProposalAndEligibility() {
        val p = proposal(listOf(item("Rice", 120, 180)))
        val target = requireNotNull(ChatPortionClarificationPolicy.target(p, "agent_meal_draft", true))
        val context = ChatCurrentDraftPortionContext(17, 4, p, target, 15)
        fun valid(
            proposal: MealProposal? = p,
            request: Long? = 17,
            generation: Long = 4,
            operation: String? = "agent_meal_draft",
            unlocked: Boolean = true,
            stopping: Boolean = false,
        ) = ChatPortionClarificationPolicy.contextIsCurrent(
            context, proposal, request, generation, operation, unlocked, stopping,
        )
        assertTrue(valid())
        assertFalse(valid(request = 16))
        assertFalse(valid(generation = 5))
        assertFalse(valid(proposal = p.copy(assumptions = listOf("different"))))
        assertFalse(valid(operation = "agent_reply"))
        assertFalse(valid(unlocked = false))
        assertFalse(valid(stopping = true))
        assertFalse(ChatPortionClarificationPolicy.contextIsCurrent(
            context.copy(currentGrams = 2001), p, 17, 4, "agent_meal_draft", true, false,
        ))
    }

    @Test fun finalCurrentDraftTurnDispositionKeepsConversationRetriesAndRetiresExplicitOperations() {
        fun disposition(finalTools: Set<String>, finalIntent: String? = null,
                        requiredRead: Boolean = false, navigation: Boolean = false,
                        newMealIntent: Boolean = false) =
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                contextAvailable = true,
                finalToolNames = finalTools,
                selectedIntent = finalIntent,
                requiredRead = requiredRead,
                navigation = navigation,
                newMealIntent = newMealIntent,
            )

        val firstNoToolResponse = disposition(emptySet())
        assertEquals(CurrentDraftTurnDisposition.RETAIN_CONVERSATIONAL_REPLY, firstNoToolResponse)
        // Production classifies the final response after the one fact-auditor retry, not the initial response.
        val auditedRetryFinalTools = setOf("adjust_current_draft_portion")
        assertEquals(
            CurrentDraftTurnDisposition.APPLY_PORTION_EDIT,
            disposition(auditedRetryFinalTools),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_CONVERSATIONAL_REPLY,
            disposition(emptySet()),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION,
            disposition(setOf("confirm_meal_draft")),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION,
            disposition(setOf("decline_meal_draft"), "decline_meal_draft"),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            disposition(setOf("read_meals"), "read_meals"),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            disposition(setOf("draft_meal"), "draft_meal", newMealIntent = true),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION,
            disposition(setOf("draft_meal"), "draft_meal"),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            disposition(emptySet(), requiredRead = true),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            disposition(emptySet(), navigation = true),
        )
        assertEquals(
            CurrentDraftTurnDisposition.NO_CURRENT_DRAFT,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                contextAvailable = false, finalToolNames = setOf("adjust_current_draft_portion"),
                selectedIntent = "adjust_current_draft_portion", requiredRead = false, navigation = false,
                newMealIntent = false,
            ),
        )
    }

    @Test fun malformedOrUnknownToolSelectionKeepsCurrentDraftUnlessMemberRequestedAnExplicitRead() {
        listOf("unknown_tool", "read_unknown", "read_meals", "search_saved_dishes").forEach { malformedTool ->
            assertEquals(
                CurrentDraftTurnDisposition.RETAIN_AND_CLARIFY_CONFIRMATION,
                ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                    true, setOf(malformedTool), null, false, false, false,
                ),
            )
        }
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                true, setOf("read_meals"), null, true, false, false,
            ),
        )
        assertEquals(
            CurrentDraftTurnDisposition.RETIRE_FOR_EXPLICIT_OPERATION,
            ChatPortionClarificationPolicy.currentDraftTurnDisposition(
                true, setOf("search_saved_dishes"), "search_saved_dishes", false, false, false,
            ),
        )
    }
}
