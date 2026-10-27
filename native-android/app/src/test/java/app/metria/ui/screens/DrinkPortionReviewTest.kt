package app.metria.ui.screens

import app.metria.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.time.Instant

class DrinkPortionReviewTest {
    @Test fun `explicit grams replace an initial drink amount even when model range is fixed`() {
        val item = MealProposalItem("coffee", "brewed", "200 mL", GramRange(200, 200), "", "high", role = "drink")
        val proposal = MealProposal(listOf(item), emptyList(), emptyList(), "high",
            memberDrinkQuantity = ChatDrinkQuantity(200.0, "ml"))
        val override = ChatPortionOverride(5, 6, proposal, 0, "coffee", 150)
        val draft = initialDraftsWithPortionOverride(listOf(item), proposal, override).single()
        assertEquals("150", draft.grams)
        assertNull(draft.drinkPortion)
    }

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

    @Test fun `explicit member coffee amount becomes source-bound review quantity ahead of defaults`() {
        val coffee = CatalogFood(171890L, "Beverages, coffee, brewed, prepared with tap water", null, "Beverages", emptyMap(),
            listOf(CatalogPortion(237.0, "1 cup (8 fl oz)", 236.58824), CatalogPortion(29.6, "1 fl oz", 29.57353),
                CatalogPortion(178.0, "6 fl oz", 177.44118)))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(coffee))
        val item = MealProposalItem("coffee", "brewed", "drink", GramRange(100, 180), "", "medium", role = "drink")
        val source = MealProposal(listOf(item), emptyList(), emptyList(), "medium", memberDrinkQuantity = ChatDrinkQuantity(200.0, "ml"))
        val base = initialDrafts(listOf(item))
        val explicit = initialDraftsWithMemberDrinkQuantity(base, source, catalog, emptyList(), hasPortionOverride = false).single()
        assertEquals(200, DrinkPortionPolicy.canonicalGrams(explicit.resolvedDrink()!!))
        assertEquals("200 mL", DrinkPortionPolicy.format(explicit.resolvedDrink()!!))
        assertEquals(base, initialDraftsWithMemberDrinkQuantity(base, source, catalog, emptyList(), hasPortionOverride = true))
        assertEquals("1 cup (8 fl oz)", DrinkPortionPolicy.options(coffee, "fixture")
            .single { it.unit == "serving" && it.label == "1 cup (8 fl oz)" }.sourceLabel)
    }

    @Test fun `drink correction prefill requires the same catalog food and no label replacement`() {
        val coffee = CatalogFood(171890L, "Beverages, coffee, brewed, prepared with tap water", null, "Beverages", emptyMap(),
            listOf(CatalogPortion(237.0, "1 cup (8 fl oz)", 236.58824)))
        val catalog = OfflineFoodCatalog("fixture", "fixture", listOf(coffee))
        val item = MealProposalItem("coffee", "brewed", "drink", GramRange(100, 180), "", "medium", role = "drink")
        val proposal = MealProposal(listOf(item), emptyList(), emptyList(), "medium")
        val portion = DrinkPortionPolicy.resolve(DrinkPortionPolicy.options(coffee, "fixture").single { it.unit == "ml" }, "200")!!
        val override = ChatPortionOverride(5, 6, proposal, 0, "coffee", 200, portion)
        val drafts = initialDrafts(listOf(item))
        val applied = initialDraftsWithDrinkPortionOverride(drafts, proposal, override, null, catalog, emptyList()).single()
        assertEquals(portion, applied.drinkPortion)
        assertEquals(200, DrinkPortionPolicy.canonicalGrams(applied.resolvedDrink()!!))
        assertEquals(drafts, initialDraftsWithDrinkPortionOverride(drafts, proposal, override, null,
            catalog.copy(foods = listOf(coffee.copy(fdcId = 171891L))), emptyList()))
        val replacement = ChatFoodReplacementOverride(5, 6, proposal, 0, "coffee", "tea")
        assertEquals(drafts, initialDraftsWithDrinkPortionOverride(drafts, proposal, override, replacement, catalog, emptyList()))
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
