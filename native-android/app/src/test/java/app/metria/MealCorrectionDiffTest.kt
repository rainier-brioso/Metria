package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealCorrectionDiffTest {
    private val rice = item("item-a", name = "Rice", groupId = "dish-a", groupName = "Lunch")
    private val chicken = item("item-b", name = "Chicken", groupId = "dish-a", groupName = "Lunch")

    @Test
    fun identicalAndReorderedItemsProduceNoSignal() {
        assertEquals(valid(), diff(listOf(rice, chicken), listOf(rice, chicken)))
        assertEquals(valid(), diff(listOf(rice, chicken), listOf(chicken, rice)))
    }

    @Test
    fun matchesStableIdsAcrossRenameAndDoesNotPairByName() {
        val renamed = rice.copy(name = "Brown rice")
        val result = diff(listOf(rice), listOf(renamed))
        assertEquals(listOf(MealCorrectionKind.RENAMED), changes(result).map { it.kind })
        assertEquals("item-a", changes(result).single().itemId)
        assertEquals("Rice", changes(result).single().oldName)
        assertEquals("Brown rice", changes(result).single().newName)
    }

    @Test
    fun reportsAddedAndRemovedByIdEvenWhenNamesRepeat() {
        val result = diff(listOf(rice), listOf(rice, rice.copy(itemId = "item-c")))
        assertEquals(listOf(MealCorrectionKind.ADDED), changes(result).map { it.kind })
        val replaced = diff(listOf(rice), listOf(rice.copy(itemId = "item-c")))
        assertEquals(
            setOf(MealCorrectionKind.ADDED, MealCorrectionKind.REMOVED),
            changes(replaced).map { it.kind }.toSet(),
        )
    }

    @Test
    fun reportsGroupMovePortionAndCatalogChangesIndependently() {
        val changed = rice.copy(
            groupId = "dish-b",
            groupName = "Side",
            portionGrams = 180,
            catalogFoodId = 42L,
            catalogRelease = "2026-04",
        )
        val result = diff(listOf(rice), listOf(changed))
        assertEquals(
            setOf(
                MealCorrectionKind.REGROUPED,
                MealCorrectionKind.PORTION_CHANGED,
                MealCorrectionKind.CATALOG_MATCH_CHANGED,
            ),
            changes(result).map { it.kind }.toSet(),
        )
    }

    @Test
    fun renamingGroupLabelWithoutChangingStableGroupIdIsNotRegrouping() {
        assertEquals(valid(), diff(listOf(rice), listOf(rice.copy(groupName = "Dinner"))))
    }

    @Test
    fun invalidOrOversizedIdentifiersAndDuplicateIdsFailClosed() {
        val invalidItems = listOf(
            rice.copy(itemId = ""),
            rice.copy(itemId = "not safe/identifier"),
            rice.copy(itemId = "x".repeat(81)),
            rice.copy(groupId = "\u0000invalid"),
        )
        invalidItems.forEach { invalid ->
            assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(invalid), emptyList()))
        }
        assertEquals(
            MealCorrectionDiffResult.InvalidSnapshot,
            MealCorrectionDiff.diff(MealReviewSnapshot(listOf(rice, rice)), MealReviewSnapshot(emptyList())),
        )
    }

    @Test
    fun oversizedSnapshotsAndUnboundedFieldsFailClosed() {
        val tooMany = (0..MealCorrectionDiff.MAX_ITEMS).map { rice.copy(itemId = "item-$it") }
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(tooMany, emptyList()))
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(name = "x".repeat(121))), emptyList()))
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(groupName = " ")), emptyList()))
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(portionGrams = 2_001)), emptyList()))
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(catalogFoodId = -1L)), emptyList()))
    }

    @Test
    fun catalogMatchCannotHaveOnlyOneHalfOfItsIdentity() {
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(catalogFoodId = 4L)), emptyList()))
        assertEquals(MealCorrectionDiffResult.InvalidSnapshot, diff(listOf(rice.copy(catalogRelease = "release")), emptyList()))
        assertTrue(changes(diff(listOf(rice), listOf(rice.copy(catalogFoodId = 4L, catalogRelease = "release")))).isNotEmpty())
    }

    private fun item(
        itemId: String,
        name: String,
        groupId: String,
        groupName: String,
        portionGrams: Int? = 120,
        catalogFoodId: Long? = null,
        catalogRelease: String? = null,
    ) = MealReviewItemSnapshot(itemId, name, groupId, groupName, portionGrams, catalogFoodId, catalogRelease)

    private fun diff(before: List<MealReviewItemSnapshot>, after: List<MealReviewItemSnapshot>) =
        MealCorrectionDiff.diff(MealReviewSnapshot(before), MealReviewSnapshot(after))

    private fun changes(result: MealCorrectionDiffResult) = (result as MealCorrectionDiffResult.Valid).changes

    private fun valid() = MealCorrectionDiffResult.Valid(emptyList())
}
