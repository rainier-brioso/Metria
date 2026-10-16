package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedDishChoicePolicyTest {
    private val first = dish("San Jacobo")
    private val second = dish("Chicken and rice")
    private val choices = listOf(
        SavedDishChoiceCandidate(first, exactNameMatch = false),
        SavedDishChoiceCandidate(second, exactNameMatch = false),
    )
    private val pending = PendingSavedDishChoices(41L, 7L, choices)

    @Test
    fun `accepts a bounded ordinal or one exact complete candidate name`() {
        assertEquals(first.id, SavedDishChoicePolicy.resolveReply("the first one", choices))
        assertEquals(second.id, SavedDishChoicePolicy.resolveReply("THE SECOND ONE", choices))
        assertEquals(first.id, SavedDishChoicePolicy.resolveReply("san jacobo", choices))
        assertEquals(second.id, SavedDishChoicePolicy.resolveReply("Chicken and rice!", choices))

        val single = listOf(SavedDishChoiceCandidate(first, exactNameMatch = true))
        assertEquals(first.id, SavedDishChoicePolicy.resolveReply("the first one", single))
    }

    @Test
    fun `does not infer yes fuzzy or ambiguous dish names`() {
        assertNull(SavedDishChoicePolicy.resolveReply("yes", choices))
        assertEquals(second.id, SavedDishChoicePolicy.resolveReply("the second", choices))
        assertNull(SavedDishChoicePolicy.resolveReply("san jacob", choices))
        assertNull(SavedDishChoicePolicy.resolveReply("chicken", choices))
        val sameName = choices + SavedDishChoiceCandidate(dish("San Jacobo"), exactNameMatch = false)
        assertNull(SavedDishChoicePolicy.resolveReply("san jacobo", sameName))
        assertNull(SavedDishChoicePolicy.resolveReply("the fourth one", choices))
        assertNull(SavedDishChoicePolicy.resolveReply("x".repeat(161), choices))
        assertTrue(SavedDishChoicePolicy.isChoiceAttempt("the fourth one", choices))
        assertTrue(SavedDishChoicePolicy.isChoiceAttempt("yes", choices))
        assertFalse(SavedDishChoicePolicy.isChoiceAttempt("no", choices))
        assertTrue(SavedDishChoicePolicy.isDeclineReply("no thanks"))
        assertEquals("Okay, I left those saved dishes unchanged. Nothing has been added.", SavedDishChoicePolicy.declineAcknowledgement())
        assertFalse(SavedDishChoicePolicy.isChoiceAttempt("How many calories today?", choices))
        assertTrue(SavedDishChoicePolicy.isChoiceAttempt("san jacobo", choices + SavedDishChoiceCandidate(dish("San Jacobo"), false)))
    }

    @Test
    fun `choices require the exact consumed request unlocked session and unchanged dishes`() {
        assertTrue(current(pending))
        assertFalse(current(pending.copy(requestId = 42L), active = pending.requestId, handled = pending.requestId))
        assertFalse(current(pending, handled = null))
        assertFalse(current(pending, active = 42L))
        assertFalse(current(pending, generation = 8L))
        assertFalse(current(pending, unlocked = false))
        assertFalse(current(pending, stopping = true))
        assertFalse(current(pending, dishes = listOf(first, second.copy(name = "Chicken rice"))))
        assertFalse(current(pending, dishes = listOf(
            first.copy(items = listOf(MealEntryItem(first.name, 121, "cooked"))), second,
        )))
        assertFalse(current(pending, dishes = listOf(first)))
        assertFalse(current(pending.copy(candidates = choices + choices.first()), dishes = choices.map { it.dish }))
        val threeChoices = choices + SavedDishChoiceCandidate(dish("Soup"), false)
        assertEquals(threeChoices[2].dish.id, SavedDishChoicePolicy.resolveReply("the third one", threeChoices))
        val fourChoices = threeChoices + SavedDishChoiceCandidate(dish("Omelette"), false)
        assertNull(SavedDishChoicePolicy.resolveReply("the third one", fourChoices))
        assertFalse(current(pending.copy(candidates = fourChoices), dishes = fourChoices.map { it.dish }))
    }

    @Test
    fun `a failed write can retry only the already selected candidate`() {
        val selected = pending.copy(selectedCandidateId = second.id)
        assertTrue(SavedDishChoicePolicy.canRetrySelection(selected, second.id))
        assertFalse(SavedDishChoicePolicy.canRetrySelection(selected, first.id))
        assertFalse(SavedDishChoicePolicy.canRetrySelection(pending, "unoffered-id"))
        assertEquals(
            "I matched that to “Chicken and rice”. Review its foods and portions before adding. Nothing has been saved.",
            SavedDishChoicePolicy.acknowledgement(second.name),
        )
    }

    @Test
    fun `selection is one shot except for retrying the same failed handoff`() {
        assertTrue(SavedDishChoicePolicy.canRetrySelection(pending, first.id))
        val afterSelection = pending.copy(selectedCandidateId = first.id)
        assertTrue(SavedDishChoicePolicy.canRetrySelection(afterSelection, first.id))
        assertFalse(SavedDishChoicePolicy.canRetrySelection(afterSelection, second.id))
    }

    private fun current(
        state: PendingSavedDishChoices,
        active: Long? = state.requestId,
        handled: Long? = state.requestId,
        generation: Long = state.conversationGeneration,
        unlocked: Boolean = true,
        stopping: Boolean = false,
        dishes: List<PersonalDish> = choices.map { it.dish },
    ): Boolean = SavedDishChoicePolicy.isCurrent(
        state, active, handled, generation, unlocked, stopping, dishes,
    )

    private fun dish(name: String) = PersonalDish(name = name, items = listOf(MealEntryItem(name, 120, "cooked")))
}
