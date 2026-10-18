package app.metria.ui.screens

import app.metria.ChatPortionOverride
import app.metria.ChatFoodReplacementOverride
import app.metria.GramRange
import app.metria.MealProposal
import app.metria.MealProposalItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    @Test fun singleFoodReplacementUpdatesReviewLabelAndClearsStalePreparationFields() {
        val item = item("white rice", 100, 180).copy(
            role = "main_food",
            evidence = "member_report",
        )
        val proposal = MealProposal(
            listOf(item), listOf("original assumption"), listOf("original question"), "medium",
        )
        val replacement = ChatFoodReplacementOverride(
            requestId = 10,
            conversationGeneration = 3,
            originalProposal = proposal,
            itemIndex = 0,
            originalItemName = "white rice",
            replacementName = "brown rice",
        )
        val explicitPortion = ChatPortionOverride(10, 3, proposal, 0, "white rice", 150)
        val baseline = initialDrafts(proposal.items).single()

        assertTrue(isFoodReplacementReviewPrefillValid(proposal, replacement))
        val changed = initialDraftsWithFoodReplacement(
            proposal.items, proposal, replacement, explicitPortion,
        ).single()
        assertEquals("brown rice", changed.name)
        assertEquals("brown rice", changed.dishName)
        assertEquals("", changed.description)
        assertEquals("", changed.preparation)
        assertEquals("150", changed.grams)
        assertEquals(item.role, changed.role)
        assertEquals(item.evidence, changed.evidence)
        assertEquals("white rice", baseline.name)
        assertFalse(shouldSuggestInitialCatalogMatch(0, proposal, replacement))
    }

    @Test fun replacementWithUnknownPortionDoesNotInventGramsAndNormalDraftPathStaysTheSame() {
        val proposal = MealProposal(
            listOf(item("white rice", 100, 180)), emptyList(), emptyList(), "medium",
        )
        val replacement = ChatFoodReplacementOverride(10, 3, proposal, 0, "white rice", "brown rice")
        val normal = initialDraftsWithFoodReplacement(proposal.items, proposal, null, null).single()
        val replaced = initialDraftsWithFoodReplacement(proposal.items, proposal, replacement, null).single()

        assertEquals("white rice", normal.name)
        assertEquals("white rice", normal.dishName)
        assertEquals("brown rice", replaced.name)
        assertEquals("brown rice", replaced.dishName)
        assertEquals("", replaced.grams)
        assertEquals("", replaced.description)
        assertEquals("", replaced.preparation)
        assertTrue(shouldSuggestInitialCatalogMatch(0, proposal, null))
    }

    @Test fun invalidOrStaleReplacementFallsBackToTheUnchangedProposalReview() {
        val proposal = MealProposal(
            listOf(item("white rice", 100, 180)), emptyList(), emptyList(), "medium",
        )
        val baseline = initialDrafts(proposal.items).single()
        val invalid = ChatFoodReplacementOverride(10, 3, proposal, 0, "white rice", "rice with beans")
        val stale = invalid.copy(originalProposal = proposal.copy(assumptions = listOf("changed")))

        assertFalse(isFoodReplacementReviewPrefillValid(proposal, invalid))
        assertFalse(isFoodReplacementReviewPrefillValid(proposal, stale))
        assertEquals(
            baseline.reviewFields(),
            initialDraftsWithFoodReplacement(proposal.items, proposal, invalid, null).single().reviewFields(),
        )
        assertEquals(
            baseline.reviewFields(),
            initialDraftsWithFoodReplacement(proposal.items, proposal, stale, null).single().reviewFields(),
        )
    }
}
