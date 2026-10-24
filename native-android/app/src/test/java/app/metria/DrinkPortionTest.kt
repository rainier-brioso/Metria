package app.metria

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DrinkPortionTest {
    private val release = "fixture-v1"
    private val juice = CatalogFood(42, "Juice, orange", null, "Beverages",
        mapOf("208" to 50.0, "203" to 1.0, "204" to 0.0, "205" to 10.0),
        listOf(CatalogPortion(216.0, "1 glass (200 mL)", 200.0)))
    private fun ml() = DrinkPortionPolicy.options(juice, release).single { it.unit == "ml" }

    @Test fun `mL uses food specific mass instead of water density`() {
        val portion = DrinkPortionPolicy.resolve(ml(), "100")!!
        assertEquals(108, DrinkPortionPolicy.canonicalGrams(portion))
        assertEquals(54.0, FoodCatalog.calculate(juice, 108).caloriesKcal!!, 1e-9)
        assertEquals("100 mL", DrinkPortionPolicy.format(portion))
    }
    @Test fun `serving fractions and decimal comma convert once`() {
        val serving = DrinkPortionPolicy.options(juice, release).single { it.unit == "serving" }
        assertEquals(108, DrinkPortionPolicy.canonicalGrams(DrinkPortionPolicy.resolve(serving, "0,5")!!))
        assertEquals(324, DrinkPortionPolicy.canonicalGrams(DrinkPortionPolicy.resolve(serving, "1.5")!!))
    }
    @Test fun `bare cups support servings but never imply mL`() {
        val milk = juice.copy(description = "Milk, whole", portions = listOf(CatalogPortion(249.0, "1 cup")))
        assertEquals(listOf("serving"), DrinkPortionPolicy.options(milk, release).map { it.unit })
        assertTrue(DrinkPortionPolicy.options(milk.copy(portions = emptyList()), release).isEmpty())
    }
    @Test fun `invalid quantities and oversized mass reject rather than clamp`() {
        listOf("", "0", "-20", "NaN", "Infinity", "1e2", "1.2.3", "0.00001", "9999", "2".repeat(25)).forEach {
            assertNull(it, DrinkPortionPolicy.resolve(ml(), it))
        }
        assertNull(DrinkPortionPolicy.fromGrams(ml().copy(gramsPerUnit = 0.0), 100))
        assertNull(DrinkPortionPolicy.scale(DrinkPortionPolicy.resolve(ml(), "100")!!, Double.NaN))
    }
    @Test fun `unit switching and scaling preserve consistent mass`() {
        val initial = DrinkPortionPolicy.fromGrams(ml(), 125)!!
        assertEquals(125, DrinkPortionPolicy.canonicalGrams(initial))
        val serving = DrinkPortionPolicy.options(juice, release).single { it.unit == "serving" }
        val switched = DrinkPortionPolicy.fromGrams(serving, 125)!!
        assertEquals(125, DrinkPortionPolicy.canonicalGrams(switched))
        val scaled = DrinkPortionPolicy.scale(switched, 1.5)!!
        assertNotNull(DrinkPortionPolicy.edited(scaled, DrinkPortionPolicy.decimal(scaled.amount)))
        assertEquals(188, DrinkPortionPolicy.canonicalGrams(scaled))
    }
    @Test fun `rounded serving display does not replace exact conversion`() {
        val file = sequenceOf(File("src/main/assets/usda-beverages-sr-2018-v1.json"),
            File("app/src/main/assets/usda-beverages-sr-2018-v1.json")).first { it.isFile }
        val catalog = FoodCatalog.fromJson(file.readText())
        val coffee = catalog.foods.single { it.fdcId == 171890L }
        val serving = DrinkPortionPolicy.options(coffee, catalog.sourceRelease)
            .single { it.unit == "serving" && it.label == "1 cup (8 fl oz)" }
        val portion = DrinkPortionPolicy.fromGrams(serving, 200)!!
        val display = DrinkPortionPolicy.display(portion)!!
        assertEquals("0.84", display.amountText)
        assertTrue(display.isApproximate)
        assertEquals(200, DrinkPortionPolicy.canonicalGrams(portion))
        assertEquals(portion, DrinkPortionPolicy.edited(portion, null))
        assertEquals("About 0.84 × 1 cup (8 fl oz)", DrinkPortionPolicy.format(portion))
        // The rounded text only becomes authoritative after the member edits/submits it.
        assertEquals(199, DrinkPortionPolicy.canonicalGrams(
            DrinkPortionPolicy.edited(portion, display.amountText)!!
        ))
    }
    @Test fun `readable amount precision avoids showing a positive quantity as zero`() {
        val tiny = DrinkPortion(0.004, "serving", "1 cup", 237.0, 42, release, "1 cup")
        val tinyDisplay = DrinkPortionPolicy.display(tiny)!!
        assertEquals("0.004", tinyDisplay.amountText)
        assertFalse(tinyDisplay.isApproximate)
        assertTrue(DrinkPortionPolicy.display(tiny.copy(amount = 0.0041))!!.isApproximate)
        val exact = DrinkPortion(1.0, "serving", "1 cup", 237.0, 42, release, "1 cup")
        assertEquals(DrinkPortionDisplay("1", false), DrinkPortionPolicy.display(exact))
    }
    @Test fun `editing invalid text cannot reuse the previous valid amount`() {
        val original = DrinkPortionPolicy.resolve(ml(), "200")!!
        assertNull(DrinkPortionPolicy.edited(original, ""))
        assertNull(DrinkPortionPolicy.edited(original, "99999"))
        assertEquals(108, DrinkPortionPolicy.canonicalGrams(DrinkPortionPolicy.edited(original, "100")!!))
    }
    @Test fun `snapshot is bound to food release and canonical grams`() {
        val p = DrinkPortionPolicy.resolve(ml(), "100")!!
        val item = MealEntryItem("Juice", 108, null, 42, release, role = "drink", drinkPortion = p)
        assertTrue(DrinkPortionPolicy.valid(item))
        assertFalse(DrinkPortionPolicy.valid(item.copy(grams = 100)))
        assertFalse(DrinkPortionPolicy.valid(item.copy(catalogFoodId = 43)))
        assertFalse(DrinkPortionPolicy.valid(item.copy(catalogRelease = "other")))
        assertFalse(DrinkPortionPolicy.valid(item.copy(role = "side")))
        assertNull(DrinkPortionPolicy.rebind(p, juice.copy(fdcId = 43), release))
        val upgraded = DrinkPortionPolicy.rebind(p, juice, "new-release")!!
        assertEquals("new-release", upgraded.catalogRelease)
        assertEquals(p.amount, upgraded.amount, 0.0)
        assertTrue(DrinkPortionPolicy.valid(item.copy(catalogRelease = "new-release", drinkPortion = upgraded)))
        assertEquals(upgraded, DrinkPortionPolicy.forCatalog(p, OfflineFoodCatalog("Fixture", "new-release", listOf(juice))))
        assertNull(DrinkPortionPolicy.rebind(p, juice.copy(portions = listOf(CatalogPortion(220.0, "1 glass (200 mL)", 200.0))), release))
    }
    @Test fun `scaling rejects unit caps even when the mass could fit and avoids double rounding`() {
        val dilute = DrinkPortion(10_000.0, "ml", "mL", 0.1, 42, release, "fixture volume")
        assertEquals(1000, DrinkPortionPolicy.canonicalGrams(dilute))
        assertNull(DrinkPortionPolicy.scale(dilute, 1.5))
        val halfCup = DrinkPortion(0.5, "serving", "1 cup", 237.0, 42, release, "1 cup")
        assertEquals(119, DrinkPortionPolicy.canonicalGrams(halfCup))
        assertEquals(237, DrinkPortionPolicy.canonicalGrams(DrinkPortionPolicy.scale(halfCup, 2.0)!!))
    }
    @Test fun `vault and recovery metadata roundtrip with legacy defaults and strict failures`() {
        val p = DrinkPortionPolicy.resolve(ml(), "100")!!
        val item = JSONObject().put("drinkPortion", DrinkPortionCodec.encode(p))
        assertEquals(p, DrinkPortionCodec.readItem(JSONObject(item.toString())))
        assertNull(DrinkPortionCodec.readItem(JSONObject().put("grams", 108)))
        listOf("broken", JSONObject(), DrinkPortionCodec.encode(p).put("unit", "liters"),
            DrinkPortionCodec.encode(p).put("amount", "100"), DrinkPortionCodec.encode(p).put("label", "bad\nlabel")).forEach {
            assertTrue(runCatching { DrinkPortionCodec.readItem(JSONObject().put("drinkPortion", it)) }.isFailure)
        }
    }
    @Test fun `packaged coffee supports source based mL and espresso servings`() {
        val file = sequenceOf(File("src/main/assets/usda-beverages-sr-2018-v1.json"),
            File("app/src/main/assets/usda-beverages-sr-2018-v1.json")).first { it.isFile }
        val catalog = FoodCatalog.fromJson(file.readText())
        assertEquals(2, catalog.foods.size)
        val coffee = catalog.foods.single { it.fdcId == 171890L }
        val option = DrinkPortionPolicy.options(coffee, catalog.sourceRelease).single { it.unit == "ml" }
        assertEquals("1 cup (8 fl oz)", option.sourceLabel)
        assertEquals(237.0 / 236.58824, option.gramsPerUnit, 1e-9)
        assertEquals(200, DrinkPortionPolicy.canonicalGrams(DrinkPortionPolicy.resolve(option, "200")!!))
        val espresso = catalog.foods.single { it.fdcId == 171891L }
        assertTrue(DrinkPortionPolicy.options(espresso, release).any { it.unit == "serving" })
    }
}
