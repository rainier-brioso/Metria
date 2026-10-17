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
}
