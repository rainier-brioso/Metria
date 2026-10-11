package app.metria

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PreparedFoodCatalogTest {
    private fun asset(name: String): String {
        val directory = File("src/main/assets").takeIf { it.isDirectory } ?: File("app/src/main/assets")
        return File(directory, name).readText()
    }
    private val foundation = FoodCatalog.fromJson(asset("usda-foundation-2026-04-30.json"))
    private val prepared = FoodCatalog.fromJson(asset("usda-prepared-sr-2018-v1.json"))
    private val catalog = FoodCatalog.merge(foundation, prepared)

    @Test fun `bundled English and Spanish dish components find separate references`() {
        for (name in listOf("spaghetti", "spaghetti cooked", "espagueti", "espaguetis", "espagueti cocido")) {
            assertEquals(name, 169737L, FoodCatalog.suggest(catalog, name)?.fdcId)
        }
        assertEquals(listOf(169737L), FoodCatalog.search(catalog, "espagueti").map { it.fdcId })
        for (name in listOf("meatballs", "meatball", "albóndigas", "albóndiga", "albóndigas de carne")) {
            assertEquals(name, 171638L, FoodCatalog.suggest(catalog, name)?.fdcId)
        }
        assertEquals(2685579L, FoodCatalog.suggest(catalog, "salsa de tomate")?.fdcId)
        assertNotNull(FoodCatalog.suggest(catalog, "tomato sauce"))
        assertEquals("Pasta cocida enriquecida, sin sal añadida", FoodCatalog.spanishDisplayName(prepared.foods[0]))
        assertEquals("Albóndigas congeladas, estilo italiano", FoodCatalog.spanishDisplayName(prepared.foods[1]))
    }

    @Test fun `prepared references cannot silently replace raw pasta or a specific meat recipe`() {
        assertNull(FoodCatalog.suggest(catalog, "spaghetti dry"))
        assertNull(FoodCatalog.suggest(catalog, "espagueti crudo"))
        assertNull(FoodCatalog.suggest(catalog, "spaghetti", "uncooked pasta"))
        assertNull(FoodCatalog.suggest(catalog, "albóndigas de carne de cerdo"))
        assertNull(FoodCatalog.suggest(catalog, "turkey meatballs"))
        assertNull(FoodCatalog.suggest(catalog, "meatballs with tomato sauce"))
    }

    @Test fun `source bundle preserves old food values and identifies the extension`() {
        assertEquals("2026-04-30+sr-2018-v1", catalog.sourceRelease)
        assertEquals(foundation.foods.size + 2, catalog.foods.size)
        foundation.foods.forEach { original -> assertEquals(original, catalog.foods.first { it.fdcId == original.fdcId }) }
        assertThrows(IllegalArgumentException::class.java) { FoodCatalog.merge(foundation, foundation) }
    }

    @Test fun `separate pasta meatball and sauce portions have deterministic complete macros`() {
        val portions = listOf(169737L to 150, 171638L to 100, 2685579L to 50)
        val values = portions.map { (id, grams) -> FoodCatalog.calculate(catalog.foods.first { it.fdcId == id }, grams) }
        values.forEach { assertNotNull(it.caloriesKcal); assertNotNull(it.proteinGrams); assertNotNull(it.carbohydrateGrams); assertNotNull(it.fatGrams) }
        assertEquals(237.0, values[0].caloriesKcal!!, 0.0001)
        assertEquals(286.0, values[1].caloriesKcal!!, 0.0001)
        assertEquals(8.7, values[0].proteinGrams!!, 0.0001)
        assertEquals(22.21, values[1].fatGrams!!, 0.0001)
        assertFalse(values[0].caloriesDerivedFromMacros)
        assertFalse(values[1].caloriesDerivedFromMacros)
    }
}
