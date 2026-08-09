package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MealIdentityTest {
    @Test
    fun findsSameCatalogFoodsWhenPortionsAreWithinTolerance() {
        val previous = MealEntry(
            title = "Arroz con pollo",
            recordedAt = Instant.parse("2026-08-01T12:00:00Z"),
            items = listOf(
                MealEntryItem("Rice", 200, "cooked", catalogFoodId = 1, catalogRelease = "r1"),
                MealEntryItem("Chicken", 150, "roasted", catalogFoodId = 2, catalogRelease = "r1"),
            ),
            assumptions = emptyList(), confidence = "high", source = "member_confirmed_catalog",
        )
        val draft = listOf(
            MealEntryItem("Rice", 220, "cooked", catalogFoodId = 1, catalogRelease = "r1"),
            MealEntryItem("Chicken", 160, "roasted", catalogFoodId = 2, catalogRelease = "r1"),
        )

        assertEquals(previous.id, MealIdentity.similarSavedMeal(draft, listOf(previous))?.id)
    }

    @Test
    fun doesNotTreatDifferentFoodsAsARepeat() {
        val previous = MealEntry(
            items = listOf(MealEntryItem("Rice", 200, null, catalogFoodId = 1, catalogRelease = "r1")),
            assumptions = emptyList(), confidence = "high", source = "member_confirmed_catalog",
        )
        val draft = listOf(MealEntryItem("Pasta", 200, null, catalogFoodId = 9, catalogRelease = "r1"))

        assertNull(MealIdentity.similarSavedMeal(draft, listOf(previous)))
    }

    @Test
    fun matchesReviewedNamesWhenAnIngredientHasNoCatalogEntry() {
        val previous = MealEntry(
            items = listOf(
                MealEntryItem("Rice", 200, null, catalogFoodId = 1, catalogRelease = "r1"),
                MealEntryItem("Oil/Sauce", 30, null),
            ),
            assumptions = emptyList(), confidence = "medium", source = "ai_estimate",
        )
        val draft = listOf(
            MealEntryItem("Rice", 210, null, catalogFoodId = 1, catalogRelease = "r1"),
            MealEntryItem("Oil/Sauce", 35, null),
        )

        assertEquals(previous.id, MealIdentity.similarSavedMeal(draft, listOf(previous))?.id)
    }
}
