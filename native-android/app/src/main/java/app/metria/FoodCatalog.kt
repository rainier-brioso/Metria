package app.metria

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

/**
 * M3 catalog boundary: this data is packaged with the application, read only
 * after the vault is open, and never queried over the network. A catalog match
 * is a draft; only the member confirmation flow can turn it into a vault meal.
 */
data class OfflineFoodCatalog(
    val sourceName: String,
    val sourceRelease: String,
    val foods: List<CatalogFood>,
)

data class CatalogFood(
    val fdcId: Long,
    val description: String,
    val category: String?,
    val nutrients: Map<String, Double>,
    val portions: List<CatalogPortion>,
)

data class CatalogPortion(val grams: Double, val label: String?)

data class CatalogNutrition(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val fatGrams: Double?,
    val caloriesDerivedFromMacros: Boolean,
)

object FoodCatalog {
    private const val assetName = "usda-foundation-2026-04-30.json"
    private var cached: OfflineFoodCatalog? = null

    fun load(context: Context): OfflineFoodCatalog = synchronized(this) {
        cached ?: context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { reader ->
            fromJson(reader.readText()).also { cached = it }
        }
    }

    internal fun fromJson(raw: String): OfflineFoodCatalog {
        val root = JSONObject(raw)
        require(root.getInt("schema_version") == 1) { "Unsupported offline food catalog." }
        val source = root.getJSONObject("source")
        val foods = root.getJSONArray("foods").toFoods()
        require(foods.isNotEmpty()) { "Offline food catalog is empty." }
        return OfflineFoodCatalog(
            sourceName = source.getString("name"),
            sourceRelease = source.getString("release"),
            foods = foods,
        )
    }

    fun search(catalog: OfflineFoodCatalog, query: String, limit: Int = 12): List<CatalogFood> {
        val terms = searchKey(query).split(' ').filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return catalog.foods.asSequence()
            .filter { food ->
                val candidate = searchKey("${food.description} ${food.category.orEmpty()}")
                terms.all(candidate::contains)
            }
            .sortedWith(compareBy<CatalogFood> { !searchKey(it.description).startsWith(terms.first()) }
                .thenBy { it.description.length }
                .thenBy { it.description })
            .take(limit)
            .toList()
    }

    fun calculate(food: CatalogFood, grams: Int): CatalogNutrition {
        require(grams in 1..2_000) { "Portion must be between 1 and 2,000 g." }
        fun nutrient(number: String): Double? = food.nutrients[number]?.times(grams / 100.0)
        val protein = nutrient("203")
        val carbohydrate = nutrient("205")
        val fat = nutrient("204")
        val reportedEnergy = nutrient("208")
        // Some Foundation Foods include macros without an Energy (208) row.
        // In that narrow case show a transparent 4/4/9 estimate instead of
        // hiding all available nutrition; never replace a reported USDA value.
        val derivedEnergy = if (protein != null && carbohydrate != null && fat != null) {
            protein * 4.0 + carbohydrate * 4.0 + fat * 9.0
        } else null
        return CatalogNutrition(
            caloriesKcal = reportedEnergy ?: derivedEnergy,
            proteinGrams = protein,
            carbohydrateGrams = carbohydrate,
            fatGrams = fat,
            caloriesDerivedFromMacros = reportedEnergy == null && derivedEnergy != null,
        )
    }

    fun suggestedGrams(food: CatalogFood): Int =
        food.portions.firstOrNull()?.grams?.toInt()?.takeIf { it in 1..2_000 } ?: 100

    private fun JSONArray.toFoods(): List<CatalogFood> = buildList {
        for (index in 0 until length()) {
            val food = getJSONObject(index)
            val nutrients = buildMap {
                val rows = food.getJSONArray("nutrients")
                for (nutrientIndex in 0 until rows.length()) {
                    val row = rows.getJSONObject(nutrientIndex)
                    row.optString("number").takeIf(String::isNotBlank)?.let { number ->
                        put(number, row.getDouble("amount_per_100g"))
                    }
                }
            }
            val portions = buildList {
                val rows = food.getJSONArray("portions")
                for (portionIndex in 0 until rows.length()) {
                    val row = rows.getJSONObject(portionIndex)
                    if (!row.isNull("gram_weight")) add(
                        CatalogPortion(
                            grams = row.getDouble("gram_weight"),
                            label = listOfNotNull(
                                row.optDouble("amount").takeIf { !it.isNaN() }?.let { "%g".format(it) },
                                row.optString("measure").takeIf(String::isNotBlank),
                            ).joinToString(" ").ifBlank { null },
                        )
                    )
                }
            }
            add(CatalogFood(
                fdcId = food.getLong("fdc_id"),
                description = food.getString("description"),
                category = food.optString("category").takeIf(String::isNotBlank),
                nutrients = nutrients,
                portions = portions,
            ))
        }
    }

    private fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
}
