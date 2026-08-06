package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    private val catalog = FoodCatalog.fromJson(
        """
        {"schema_version":1,"source":{"name":"USDA","release":"2026-04-30"},"foods":[
          {"fdc_id":1,"description":"Chicken breast, roasted","category":"Poultry","nutrients":[
            {"number":"208","amount_per_100g":165.0},
            {"number":"203","amount_per_100g":31.0},
            {"number":"205","amount_per_100g":0.0},
            {"number":"204","amount_per_100g":3.6}],
            "portions":[{"gram_weight":85.0,"amount":1,"measure":"piece"}]},
          {"fdc_id":2,"description":"Rice, cooked","category":"Cereal","nutrients":[
            {"number":"208","amount_per_100g":130.0}],"portions":[]}
        ]}
        """.trimIndent()
    )

    @Test fun `search is local, case insensitive, and accent insensitive`() {
        assertEquals("Rice, cooked", FoodCatalog.search(catalog, "RÍCE").single().description)
        assertEquals("Chicken breast, roasted", FoodCatalog.search(catalog, "chicken breast").single().description)
    }

    @Test fun `nutrition is deterministic for the confirmed grams`() {
        val result = FoodCatalog.calculate(catalog.foods.first(), 150)
        assertEquals(247.5, result.caloriesKcal!!, 0.001)
        assertEquals(46.5, result.proteinGrams!!, 0.001)
        assertEquals(5.4, result.fatGrams!!, 0.001)
        assertTrue(!result.caloriesDerivedFromMacros)
        assertEquals(85, FoodCatalog.suggestedGrams(catalog.foods.first()))
    }

    @Test fun `catalog source requires a food id and release in saved nutrition`() {
        val invalid = MealNutrition(1.0, 1.0, 1.0, 1.0, "member_confirmed_catalog")
        val valid = MealNutrition(1.0, 1.0, 1.0, 1.0, "member_confirmed_catalog", 1, "2026-04-30")
        assertTrue(!MealEntryValidation.valid(MealEntry(
            items = listOf(MealEntryItem("Rice", 100, null)), assumptions = emptyList(),
            confidence = "medium", source = "member_confirmed_catalog", nutrition = invalid,
        )))
        assertTrue(MealEntryValidation.valid(MealEntry(
            items = listOf(MealEntryItem("Rice", 100, null)), assumptions = emptyList(),
            confidence = "medium", source = "member_confirmed_catalog", nutrition = valid,
        )))
    }
}
