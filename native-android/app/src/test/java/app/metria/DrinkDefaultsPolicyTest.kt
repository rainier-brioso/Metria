package app.metria

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class DrinkDefaultsPolicyTest {
    private val food = CatalogFood(42, "Coffee, brewed", null, "Beverages", emptyMap(),
        listOf(CatalogPortion(237.0, "1 cup (8 fl oz)", 236.58824)))
    private val catalog = OfflineFoodCatalog("fixture", "new", listOf(food))
    private val portion = DrinkPortionPolicy.resolve(DrinkPortionPolicy.options(food, "old").single { it.unit == "ml" }, "200")!!
    private fun item(name: String = "Coffee") = MealEntryItem(name, DrinkPortionPolicy.canonicalGrams(portion), null,
        42, "old", role = "drink", drinkPortion = portion)
    private fun meal(day: Int, items: List<MealEntryItem>) = MealEntry(recordedAt = Instant.parse("2026-10-${day}T10:00:00Z"),
        items = items, assumptions = emptyList(), confidence = "high", source = "member_confirmed_catalog")

    @Test fun `host source allows plain coffee but retains current quantities and sizes`() {
        assertTrue(DrinkDefaultsPolicy.allowsDefault("I had a coffee for breakfast"))
        listOf("I had 200 ml coffee", "I had two coffees", "I had a large coffee", "I drank half a coffee",
            "I had one hundred grams of coffee", "I had a pint of coffee", "I had ½ a coffee",
            "I had a couple of coffees", "I had a pair of coffees", "I had another coffee",
            "I drank twice the coffee").forEach {
            assertFalse(it, DrinkDefaultsPolicy.allowsDefault(it))
        }
    }
    @Test fun `latest confirmed matching drink supplies exact unit and amount across catalog release`() {
        val saved = listOf(meal(21, listOf(item())), meal(20, listOf(item().copy(drinkPortion = portion.copy(amount = 100.0), grams = 100))))
        val learned = DrinkDefaultsPolicy.previousPortion(" coffee ", food, catalog, saved)!!
        assertEquals(200.0, learned.amount, 0.0)
        assertEquals("ml", learned.unit)
        assertEquals("new", learned.catalogRelease)
        assertEquals(portion.gramsPerUnit, learned.gramsPerUnit, 0.0)
    }
    @Test fun `another drink name food or changed conversion cannot inherit defaults`() {
        val saved = listOf(meal(21, listOf(item())))
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee with milk", food, catalog, saved))
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee", food.copy(fdcId = 99), catalog, saved))
        val changed = food.copy(portions = listOf(CatalogPortion(250.0, "1 cup (8 fl oz)", 236.58824)))
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee", changed, catalog.copy(foods = listOf(changed)), saved))
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee", food, catalog, emptyList()))
    }
    @Test fun `later gram only save or ambiguous matching items retires older preference`() {
        val prior = meal(20, listOf(item()))
        val grams = item().copy(drinkPortion = null, grams = 150)
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee", food, catalog, listOf(prior, meal(21, listOf(grams)))))
        assertNull(DrinkDefaultsPolicy.previousPortion("Coffee", food, catalog, listOf(prior, meal(21, listOf(item(), item())))))
    }
}
