package app.metria.ui.screens

import app.metria.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.time.Instant

class DrinkPortionReviewTest {
    private val portion = DrinkPortion(200.0, "ml", "mL", 1.08, 42, "fixture", "1 glass (200 mL)")
    private fun draft() = MealReviewItemDraft("id", "dish", "Juice", "Juice", "", "216", "", "drink", "seen", portion)
    @Test fun `invalid drink text never resolves old mass and a corrected amount does`() {
        val original = draft()
        assertTrue(original.validDrinkInput())
        val invalid = original.copy(grams = "", portionInput = "0")
        assertFalse(invalid.validDrinkInput())
        assertNull(invalid.resolvedDrink())
        assertEquals(108, DrinkPortionPolicy.canonicalGrams(invalid.copy(portionInput = "100").resolvedDrink()!!))
        assertTrue(original.copy(drinkPortion = null, portionInput = null).validDrinkInput())
        assertFalse(original.copy(grams = "-200", drinkPortion = null, portionInput = null).validDrinkInput())
    }
    @Test fun `quantity unit and invalid input survive exit baseline restoration`() {
        val item = MealReviewItemState("id", "dish", "Juice", "Juice", "", "216", "", "drink", "seen")
        val baseline = MealReviewDraftState(listOf(item), emptyMap(), emptySet(), "Juice", "2026-10-22", "breakfast", false, emptyMap(), emptyMap())
        val unitChanged = baseline.copy(items = listOf(item.copy(drinkPortion = DrinkPortionCodec.encode(portion).toString(), portionInput = "200")))
        assertTrue(MealReviewExitPolicy.requiresConfirmation(baseline, unitChanged))
        assertEquals(unitChanged, MealReviewExitPolicy.restoreSaveableFields(MealReviewExitPolicy.saveableFields(unitChanged)))
        val invalid = unitChanged.copy(items = unitChanged.items.map { it.copy(portionInput = "0") })
        assertTrue(MealReviewExitPolicy.requiresConfirmation(unitChanged, invalid))
        assertEquals(invalid, MealReviewExitPolicy.restoreSaveableFields(MealReviewExitPolicy.saveableFields(invalid)))
    }
    @Test fun `remembered drink retains its source conversion in a fresh review prefill`() {
        val item = MealEntryItem("Juice", 216, null, 42, "fixture", role = "drink", drinkPortion = portion)
        val dish = PersonalDish(name = "My juice", items = listOf(item), id = UUID.randomUUID().toString())
        val prefill = CompanionAssistantPolicy.savedDishPrefill(dish)!!
        val restored = savedDishInitialDrafts(prefill).single()
        assertEquals(portion, restored.drinkPortion)
        assertEquals("200 mL", DrinkPortionPolicy.format(restored.resolvedDrink()!!))
        assertEquals("216", restored.grams)
    }

    @Test fun `new unspecified drink draft uses confirmed portion without replacing explicit overrides`() {
        val food = CatalogFood(42, "Juice, orange", null, "Beverages", emptyMap(),
            listOf(CatalogPortion(216.0, "1 glass (200 mL)", 200.0)))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(food))
        val source = MealProposalItem("Juice", "orange juice", "drink", GramRange(100, 150), "", "high", role = "drink")
        val proposal = MealProposal(listOf(source), emptyList(), emptyList(), "high", memberDrinkDefaultsAllowed = true)
        val original = draft().copy(grams = "125", drinkPortion = null)
        val saved = MealEntry(recordedAt = Instant.parse("2026-10-21T10:00:00Z"),
            items = listOf(MealEntryItem("Juice", 216, null, 42, "fixture", role = "drink", drinkPortion = portion)),
            assumptions = emptyList(), confidence = "high", source = "member_confirmed_catalog")
        val learned = initialDraftsWithDrinkDefaults(listOf(original), proposal, catalog, emptyList(), listOf(saved), false).single()
        assertEquals("216", learned.grams)
        assertEquals(portion, learned.resolvedDrink())
        assertEquals(listOf(original), initialDraftsWithDrinkDefaults(listOf(original), proposal, catalog, emptyList(), listOf(saved), true))
        assertEquals(listOf(original), initialDraftsWithDrinkDefaults(listOf(original), proposal.copy(memberDrinkDefaultsAllowed = false), catalog, emptyList(), listOf(saved), false))
        assertEquals(listOf(original, original.copy(id = "second")), initialDraftsWithDrinkDefaults(listOf(original, original.copy(id = "second")), proposal, catalog, emptyList(), listOf(saved), false))
    }
}
