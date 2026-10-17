package app.metria.ui.screens

import app.metria.ChatPortionOverride
import app.metria.GramRange
import app.metria.MealProposal
import app.metria.MealProposalItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatPortionReviewPrefillTest {
    private fun item(name: String, min: Int, max: Int) = MealProposalItem(
        name, "description", "portion", GramRange(min, max), "prepared", "low",
    )

    private fun MealReviewItemDraft.reviewFields() = listOf(name, description, grams, preparation, role, evidence)

    @Test fun appliesOnlyExactUniqueOriginalItemAndKeepsEveryOtherDraftField() {
        val proposal = MealProposal(listOf(item("Toast", 30, 50), item("Egg", 40, 40)), emptyList(), emptyList(), "medium")
        val override = ChatPortionOverride(9, 2, proposal, 0, "Toast", 120)
        val baseline = initialDrafts(proposal.items)
        val changed = initialDraftsWithPortionOverride(proposal.items, proposal, override)
        assertEquals("120", changed[0].grams)
        assertEquals("40", changed[1].grams)
        assertEquals(baseline[0].copy(id = changed[0].id, dishId = changed[0].dishId, grams = "120"), changed[0])
        assertEquals(baseline[1].reviewFields(), changed[1].reviewFields())
        assertEquals(GramRange(30, 50), proposal.items[0].estimatedGramsRange)
    }

    @Test fun rejectsChangedProposalIndexNameAndAmbiguousTransformedMatch() {
        val proposal = MealProposal(listOf(item("Toast", 30, 50), item("Egg", 40, 40)), emptyList(), emptyList(), "medium")
        val override = ChatPortionOverride(9, 2, proposal, 0, "Toast", 120)
        val changedProposal = proposal.copy(assumptions = listOf("changed"))
        assertEquals(initialDrafts(proposal.items).map { it.reviewFields() },
            initialDraftsWithPortionOverride(proposal.items, changedProposal, override).map { it.reviewFields() })
        val wrongName = override.copy(itemName = "Egg")
        assertEquals(initialDrafts(proposal.items).map { it.reviewFields() },
            initialDraftsWithPortionOverride(proposal.items, proposal, wrongName).map { it.reviewFields() })
        val duplicateTransformedItems = listOf(item("Toast", 30, 50), item("Toast", 40, 60))
        val duplicateProposal = proposal.copy(items = duplicateTransformedItems)
        val duplicateOverride = override.copy(originalProposal = duplicateProposal)
        assertEquals(initialDrafts(duplicateTransformedItems).map { it.reviewFields() },
            initialDraftsWithPortionOverride(duplicateTransformedItems, duplicateProposal, duplicateOverride).map { it.reviewFields() })
        assertNotEquals("120", initialDraftsWithPortionOverride(proposal.items, proposal, override.copy(grams = null))[0].grams)
    }

    @Test fun locatesTargetAfterEarlierJoinedFoodTransformationChangesItsIndex() {
        val proposal = MealProposal(
            listOf(item("rice and beans", 100, 100), item("Toast", 30, 50)),
            emptyList(), emptyList(), "medium",
        )
        val override = ChatPortionOverride(9, 2, proposal, 1, "Toast", 120)
        // A caller may pass transformed items whose earlier split shifted Toast.
        val transformed = listOf(item("rice", 50, 50), item("beans", 50, 50), proposal.items[1])
        val result = initialDraftsWithPortionOverride(transformed, proposal, override)
        assertEquals("50", result[0].grams)
        assertEquals("50", result[1].grams)
        assertEquals("120", result[2].grams)
    }
}
