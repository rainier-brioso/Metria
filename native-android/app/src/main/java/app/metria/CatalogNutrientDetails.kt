package app.metria

data class CatalogNutrientAmount(
    val number: String,
    val name: String,
    val unit: String,
    val amount: Double,
    val includedFoods: Int,
    val totalFoods: Int,
)

/** Display-only values from the current, explicitly selected offline matches. */
object CatalogNutrientDetails {
    fun calculate(portions: List<Pair<CatalogFood?, Int?>>): List<CatalogNutrientAmount> {
        val numbers = portions.flatMap { it.first?.nutrientDefinitions?.keys.orEmpty() }.toSortedSet()
        return numbers.mapNotNull { number ->
            val definitions = portions.mapNotNull { it.first?.nutrientDefinitions?.get(number) }
            val definition = definitions.firstOrNull() ?: return@mapNotNull null
            // Do not combine unlike units or silently perform unit conversion.
            if (definitions.any { it.unit != definition.unit }) return@mapNotNull null
            var count = 0
            val amount = portions.sumOf { (food, grams) ->
                val value = food?.nutrients?.get(number)
                if (grams == null || grams !in 1..2_000 || value == null || !value.isFinite() || value < 0 ||
                    food.nutrientDefinitions[number] == null) 0.0
                else { count++; value * grams / 100.0 }
            }
            if (count == 0 || !amount.isFinite()) null else CatalogNutrientAmount(
                number, definition.name, definition.unit, amount, count, portions.size,
            )
        }
    }
}
