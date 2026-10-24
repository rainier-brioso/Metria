package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExplicitMealSlotPolicyTest {
    private val parsed = MealProposalParser.parseOrThrow(
        """{"kind":"meal_proposal","items":[{"name":"Rice","visible_portion":"bowl","estimated_grams_range":{"min":100,"max":180},"preparation":"cooked","confidence":"medium"}],"assumptions":[],"questions":[],"overall_confidence":"medium"}"""
    )

    @Test
    fun attachesExplicitSlotFromTheOriginalMemberDescriptionAfterParsing() {
        val proposal = ExplicitMealSlotPolicy.attach(
            parsed,
            "I had rice, beand and chicken breast for dinner",
        )

        assertEquals("dinner", proposal.memberMealSlot)
        assertEquals(listOf("Rice"), proposal.items.map { it.name })
    }

    @Test
    fun acceptsSiblingExplicitFramesButDoesNotGuessFromBareSlotWords() {
        assertEquals("breakfast", ExplicitMealSlotPolicy.explicitSlot("I had oats for breakfast"))
        assertEquals("lunch", ExplicitMealSlotPolicy.explicitSlot("I had this as lunch"))
        assertEquals("snack", ExplicitMealSlotPolicy.explicitSlot("I had this as a snack"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had breakfast cereal"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had dinner"))
    }

    @Test
    fun abstainsOnMultipleOrNegatedMealSlotMentions() {
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had rice for dinner and lunch"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had rice, not for dinner"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I did not have rice for dinner"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I didn't eat rice for dinner"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I did not have rice, beans and chicken breast for dinner"))
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had a breakfast burrito for dinner"))
    }

    @Test
    fun ignoresMemberDescriptionsOutsideTheUpstreamBound() {
        assertNull(ExplicitMealSlotPolicy.explicitSlot("I had rice for dinner".padEnd(601, ' ')))
    }

    @Test
    fun validTodaySelectionWinsOtherwiseHostSlotIsUsedAndInvalidValuesAreIgnored() {
        assertEquals("lunch", ExplicitMealSlotPolicy.initialReviewSlot("lunch", "dinner"))
        assertEquals("dinner", ExplicitMealSlotPolicy.initialReviewSlot(null, "dinner"))
        assertEquals("dinner", ExplicitMealSlotPolicy.initialReviewSlot("unknown", "dinner"))
        assertNull(ExplicitMealSlotPolicy.initialReviewSlot(null, "unknown"))
    }

    @Test
    fun proposalCopiesForMemberEditsRetainHostSlotMetadata() {
        val proposal = ExplicitMealSlotPolicy.attach(parsed, "Rice for dinner")
        val corrected = proposal.copy(items = proposal.items.map { it.copy(name = "Brown rice") })

        assertEquals("dinner", corrected.memberMealSlot)
        assertEquals("Brown rice", corrected.items.single().name)
    }

    @Test
    fun newMemberSourceWithoutASlotClearsOldHostMetadata() {
        val previous = ExplicitMealSlotPolicy.attach(parsed, "Rice for dinner")

        assertNull(ExplicitMealSlotPolicy.attach(previous, "Rice with chicken").memberMealSlot)
    }
}
