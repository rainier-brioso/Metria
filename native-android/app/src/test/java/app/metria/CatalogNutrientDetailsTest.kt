package app.metria

import org.junit.Assert.*
import org.junit.Test

class CatalogNutrientDetailsTest {
    @Test fun parserRetainsNamesUnitsAndNutrientsWithoutPublicNumbers() {
        val catalog = FoodCatalog.fromJson("""{"schema_version":1,"source":{"name":"USDA","release":"test"},
            "foods":[{"fdc_id":1,"description":"Example","nutrients":[
              {"id":1093,"number":"307","name":"Sodium","unit":"mg","amount_per_100g":5},
              {"id":9999,"number":null,"name":"Other nutrient","unit":"µg","amount_per_100g":2}],"portions":[]}]}""")
        val result = CatalogNutrientDetails.calculate(listOf(catalog.foods.single() to 200))
        assertEquals("Sodium", result.first { it.number == "307" }.name)
        assertEquals("µg", result.first { it.number == "fdc:9999" }.unit)
        assertEquals(4.0, result.first { it.number == "fdc:9999" }.amount, 0.0)
    }

    private fun food(values: Map<String, Double>, unit: String = "mg") = CatalogFood(
        1, "Example", null, null, values, emptyList(),
        values.mapValues { CatalogNutrientDefinition("Example nutrient ${it.key}", unit) },
    )

    @Test fun scalesEachNutrientAndRetainsItsUnit() {
        val result = CatalogNutrientDetails.calculate(listOf(food(mapOf("301" to 10.0)) to 150,
            food(mapOf("301" to 20.0)) to 50)).single()
        assertEquals(25.0, result.amount, 0.0001)
        assertEquals("mg", result.unit)
        assertEquals(2, result.includedFoods)
        assertEquals(2, result.totalFoods)
    }

    @Test fun zeroIsReportedButMissingFoodsArePartial() {
        val result = CatalogNutrientDetails.calculate(listOf(food(mapOf("301" to 0.0)) to 100,
            food(emptyMap()) to 100, null to 100)).single()
        assertEquals(0.0, result.amount, 0.0)
        assertEquals(1, result.includedFoods)
        assertEquals(3, result.totalFoods)
    }

    @Test fun preservesMicronutrientsWhenMacrosAreAbsentAndIncludesUnnumberedNutrients() {
        val result = CatalogNutrientDetails.calculate(listOf(food(mapOf("401" to 8.0, "fdc:999" to 2.0)) to 50))
        assertEquals(listOf("401", "fdc:999"), result.map { it.number })
        assertEquals(4.0, result.first().amount, 0.0)
    }

    @Test fun neverSumsUnlikeUnits() {
        assertTrue(CatalogNutrientDetails.calculate(listOf(food(mapOf("301" to 1.0), "mg") to 100,
            food(mapOf("301" to 1.0), "g") to 100)).isEmpty())
    }

    @Test fun rejectsInvalidAmountsAndPortionsWithoutInventingZero() {
        assertTrue(CatalogNutrientDetails.calculate(listOf(food(mapOf("301" to Double.NaN)) to 100,
            food(mapOf("301" to -1.0)) to 100, food(mapOf("301" to 10.0)) to null,
            food(mapOf("301" to 10.0)) to 0, food(mapOf("301" to 10.0)) to 2001)).isEmpty())
    }
}
