package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    private val catalog = FoodCatalog.fromJson(
        """
        {"schema_version":1,"source":{"name":"USDA","release":"2026-04-30"},"foods":[
          {"fdc_id":2646170,"description":"Chicken breast, roasted","category":"Poultry","nutrients":[
            {"number":"208","amount_per_100g":165.0},
            {"number":"203","amount_per_100g":31.0},
            {"number":"205","amount_per_100g":0.0},
            {"number":"204","amount_per_100g":3.6}],
            "portions":[{"gram_weight":85.0,"amount":1,"measure":"piece"}]},
          {"fdc_id":2,"description":"Rice, cooked","category":"Cereal","nutrients":[
            {"number":"208","amount_per_100g":130.0}],"portions":[]},
          {"fdc_id":3,"description":"Potatoes, baked","category":"Vegetables","nutrients":[
            {"number":"208","amount_per_100g":93.0}],"portions":[]},
          {"fdc_id":4,"description":"Chicken wing, roasted","category":"Poultry","nutrients":[
            {"number":"208","amount_per_100g":203.0}],"portions":[]}
        ]}
        """.trimIndent()
    )

    @Test fun `search is local, case insensitive, and accent insensitive`() {
        assertEquals("Rice, cooked", FoodCatalog.search(catalog, "RÍCE").single().description)
        assertEquals("Chicken breast, roasted", FoodCatalog.search(catalog, "chicken breast").single().description)
    }

    @Test fun `curated Spanish aliases search the same local catalog`() {
        assertEquals("Rice, cooked", FoodCatalog.search(catalog, "arroz").single().description)
        assertEquals("Chicken breast, roasted", FoodCatalog.search(catalog, "pechuga de pollo").single().description)
    }

    @Test fun `Spanish aliases support two-character local type-ahead`() {
        assertTrue(FoodCatalog.search(catalog, "po").any { it.description == "Chicken breast, roasted" })
        assertTrue(FoodCatalog.search(catalog, "p").isEmpty())
    }

    @Test fun `Spanish tomato sauce finds sauce without matching plain tomatoes or inventing pasta`() {
        val sauces = catalog.copy(foods = listOf(
            catalog.foods.first().copy(fdcId = 10, description = "Tomato, sauce, canned, with salt added"),
            catalog.foods.first().copy(fdcId = 11, description = "Tomatoes, raw"),
            catalog.foods.first().copy(fdcId = 12, description = "Squash, spaghetti, raw"),
        ))
        assertEquals(10L, FoodCatalog.suggest(sauces, "Salsa de tomate")?.fdcId)
        assertEquals(listOf(10L), FoodCatalog.search(sauces, "salsa de tomate").map { it.fdcId })
        assertTrue(FoodCatalog.candidatesForAssistantItem(sauces, "Espagueti").isEmpty())
        assertTrue(FoodCatalog.candidatesForAssistantItem(sauces, "Albóndigas de carne").isEmpty())
    }

    @Test fun `local assistant candidates ignore preparation but not food identity`() {
        assertEquals("Chicken breast, roasted", FoodCatalog.suggest(catalog, "baked chicken breast")?.description)
        assertEquals("Chicken breast, roasted", FoodCatalog.suggest(catalog, "fried chicken pieces")?.description)
        assertTrue(FoodCatalog.candidatesForAssistantItem(catalog, "fried chicken pieces").any { it.description == "Chicken breast, roasted" })
        assertEquals("Rice, cooked", FoodCatalog.suggest(catalog, "arroz")?.description)
    }

    @Test fun `unmatched joined foods split only when every part has a local candidate`() {
        assertEquals(listOf("chicken", "potatoes"), FoodCatalog.splitUnmatchedJoinedFood(catalog, "chicken and potatoes"))
        assertEquals(listOf("chicken and unknown sauce"), FoodCatalog.splitUnmatchedJoinedFood(catalog, "chicken and unknown sauce"))
    }

    @Test fun `assistant lookup falls back to a parenthetical label's core food`() {
        assertEquals("Potatoes, baked", FoodCatalog.suggest(catalog, "Potatoes (French fries)")?.description)
    }

    @Test fun `visible chicken cut evidence ranks catalog candidates`() {
        assertEquals("Chicken breast, roasted", FoodCatalog.suggest(catalog, "chicken pieces", "cuts of breast meat")?.description)
    }

    @Test fun `only meal-attributed corrections influence local suggestions`() {
        val legacy = FoodCorrection("chicken", 4, count = 20)
        val attributed = legacy.copy(sourceMealId = "meal-1")

        assertEquals("Chicken breast, roasted", FoodCatalog.suggest(catalog, "chicken", corrections = listOf(legacy))?.description)
        assertEquals("Chicken wing, roasted", FoodCatalog.suggest(catalog, "chicken", corrections = listOf(attributed))?.description)
    }

    @Test fun `dry herbs are not treated as a failed food lookup`() {
        assertTrue(FoodCatalog.isNonMeasuredSeasoning("Herbs/Seasoning"))
        assertTrue(!FoodCatalog.isNonMeasuredSeasoning("Cream sauce"))
    }

    @Test fun `known food ids retain a curated Spanish display name`() {
        assertEquals("Pechuga de pollo deshuesada y sin piel, cruda", catalog.foods.first().spanishDisplayName)
    }

    @Test fun `rice family translations are explicitly curated by USDA id`() {
        val rice = FoodCatalog.fromJson(
            """{"schema_version":1,"source":{"name":"USDA","release":"2026-04-30"},"foods":[
              {"fdc_id":2512380,"description":"Rice, brown, long grain, unenriched, raw","nutrients":[],"portions":[]},
              {"fdc_id":2710825,"description":"Rice, black, unenriched, raw","nutrients":[],"portions":[]},
              {"fdc_id":2710838,"description":"Rice, red, unenriched, dry, raw","nutrients":[],"portions":[]}
            ]}"""
        )
        assertEquals("Arroz integral de grano largo, crudo", rice.foods[0].spanishDisplayName)
        assertEquals("Arroz negro integral, crudo", rice.foods[1].spanishDisplayName)
        assertEquals("Arroz rojo integral, crudo", rice.foods[2].spanishDisplayName)
    }

    @Test fun `common food families are translated only through reviewed ids`() {
        val families = FoodCatalog.fromJson(
            """{"schema_version":1,"source":{"name":"USDA","release":"2026-04-30"},"foods":[
              {"fdc_id":331960,"description":"Chicken, breast, cooked","nutrients":[],"portions":[]},
              {"fdc_id":2644283,"description":"Lentils, dry","nutrients":[],"portions":[]},
              {"fdc_id":2346401,"description":"Potatoes, russet, without skin, raw","nutrients":[],"portions":[]},
              {"fdc_id":1105314,"description":"Bananas, ripe and slightly ripe, raw","nutrients":[],"portions":[]}
            ]}"""
        )
        assertEquals("Pechuga de pollo sin piel, cocida", families.foods[0].spanishDisplayName)
        assertEquals("Lentejas secas", families.foods[1].spanishDisplayName)
        assertEquals("Patata russet sin piel, cruda", families.foods[2].spanishDisplayName)
        assertEquals("Plátano maduro, crudo", families.foods[3].spanishDisplayName)
    }

    @Test fun `generic recognized USDA descriptions have deterministic Spanish fallbacks`() {
        val foods = FoodCatalog.fromJson(
            """{"schema_version":1,"source":{"name":"USDA","release":"2026-04-30"},"foods":[
              {"fdc_id":99,"description":"Egg, whole, dried","nutrients":[],"portions":[]},
              {"fdc_id":98,"description":"Tomatoes, canned, red, ripe","nutrients":[],"portions":[]}
            ]}"""
        )
        assertEquals("Huevo, entero, seco", FoodCatalog.spanishDisplayName(foods.foods[0]))
        assertEquals("Tomates, en conserva, rojo, ripe", FoodCatalog.spanishDisplayName(foods.foods[1]))
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
