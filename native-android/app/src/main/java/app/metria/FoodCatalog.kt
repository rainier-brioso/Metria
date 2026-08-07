package app.metria

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

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
    val spanishDisplayName: String?,
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
    /** Curated search aliases only; the displayed/saved USDA description stays authoritative. */
    private val spanishAliases = listOf(
        "pechuga de pollo" to "chicken breast",
        "arroz" to "rice",
        "pollo" to "chicken",
        "pechuga" to "breast",
        "huevo" to "egg",
        "huevos" to "egg",
        "ternera" to "beef",
        "carne" to "beef",
        "cerdo" to "pork",
        "patata" to "potato",
        "papas" to "potato",
        "pan" to "bread",
        "leche" to "milk",
        "queso" to "cheese",
        "yogur" to "yogurt",
        "manzana" to "apple",
        "platano" to "banana",
        "avena" to "oatmeal",
        "lentejas" to "lentils",
        "lenteja" to "lentils",
        "garbanzos" to "chickpeas",
        "garbanzo" to "chickpeas",
        "tomate" to "tomato",
        "tomates" to "tomato",
        "zanahoria" to "carrot",
        "zanahorias" to "carrot",
        "aceite" to "oil",
    ).sortedByDescending { it.first.length }
    private val spanishDescriptionRules = listOf(
        "chickpeas" to "garbanzos",
        "chicken" to "pollo",
        "lentils" to "lentejas",
        "tomatoes" to "tomates",
        "potatoes" to "patatas",
        "carrots" to "zanahorias",
        "bananas" to "plátanos",
        "oranges" to "naranjas",
        "apples" to "manzanas",
        "beans" to "alubias",
        "eggs" to "huevos",
        "yogurt" to "yogur",
        "cheese" to "queso",
        "breast" to "pechuga",
        "drumstick" to "jamoncito",
        "thigh" to "muslo",
        "wing" to "ala",
        "skinless" to "sin piel",
        "boneless" to "deshuesado",
        "whole milk" to "leche entera",
        "nonfat" to "desnatado",
        "lowfat" to "bajo en grasa",
        "reduced fat" to "semidesnatado",
        "with skin" to "con piel",
        "without skin" to "sin piel",
        "canned" to "en conserva",
        "cooked" to "cocido",
        "frozen" to "congelado",
        "pasteurized" to "pasteurizado",
        "drained" to "escurrido",
        "rinsed" to "enjuagado",
        "unenriched" to "sin enriquecer",
        "whole" to "entero",
        "black" to "negro",
        "brown" to "marrón",
        "white" to "blanco",
        "red" to "rojo",
        "raw" to "crudo",
        "dried" to "seco",
        "dry" to "seco",
        "rice" to "arroz",
        "milk" to "leche",
        "egg" to "huevo",
        "apple" to "manzana",
        "banana" to "plátano",
        "orange" to "naranja",
        "potato" to "patata",
        "carrot" to "zanahoria",
        "tomato" to "tomate",
    ).sortedByDescending { it.first.length }
    private val preparationTerms = setOf(
        "baked", "boiled", "steamed", "grilled", "fried", "roasted", "braised",
        "raw", "cooked", "piece", "pieces", "trozo", "trozos", "with", "without",
        "and", "or", "con", "sin", "y", "o",
    )
    private val spanishDisplayNames = mapOf(
        321358L to "Hummus comercial",
        323604L to "Huevo entero crudo pasteurizado",
        323697L to "Clara de huevo cruda pasteurizada",
        329596L to "Yema de huevo cruda pasteurizada",
        748967L to "Huevo entero grande, grado A",
        328637L to "Queso cheddar",
        328841L to "Requesón bajo en grasa",
        329370L to "Queso mozzarella semidesnatado",
        325036L to "Queso parmesano rallado",
        2259796L to "Queso feta de leche entera",
        2647442L to "Queso fresco",
        330137L to "Yogur griego natural desnatado",
        2259793L to "Yogur natural de leche entera",
        2647437L to "Yogur natural desnatado",
        746782L to "Leche entera",
        746772L to "Leche semidesnatada (1 %)",
        746778L to "Leche semidesnatada (2 %)",
        746776L to "Leche desnatada",
        2512380L to "Arroz integral de grano largo, crudo",
        2512381L to "Arroz blanco de grano largo, crudo",
        2644282L to "Garbanzos secos",
        2644288L to "Garbanzos cocidos en conserva",
        2644283L to "Lentejas secas",
        2644285L to "Alubias negras en conserva",
        2644287L to "Alubias cannellini en conserva",
        2644292L to "Alubias pintas en conserva",
        2644294L to "Alubias blancas en conserva",
        2646170L to "Pechuga de pollo deshuesada y sin piel, cruda",
        331960L to "Pechuga de pollo sin piel, cocida",
        2727569L to "Pechuga de pollo con piel, cruda",
        2646171L to "Muslo de pollo deshuesado y sin piel, crudo",
        2727567L to "Muslo de pollo con piel, crudo",
        2727566L to "Jamoncito de pollo con piel, crudo",
        331897L to "Jamoncito de pollo cocido",
        2727568L to "Ala de pollo con piel, cruda",
        2710825L to "Arroz negro integral, crudo",
        2710838L to "Arroz rojo integral, crudo",
        2346401L to "Patata russet sin piel, cruda",
        2346402L to "Patata roja sin piel, cruda",
        2346403L to "Patata amarilla sin piel, cruda",
        1750340L to "Manzana Fuji con piel, cruda",
        1750341L to "Manzana Gala con piel, cruda",
        1750342L to "Manzana Granny Smith con piel, cruda",
        1750343L to "Manzana Honeycrisp con piel, cruda",
        1105314L to "Plátano maduro, crudo",
        746771L to "Naranja Navel, cruda",
        2258586L to "Zanahoria cruda",
        1999634L to "Tomate Roma",
        321360L to "Tomate uva, crudo",
    )

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
        val termAlternatives = searchTermAlternatives(query)
        if (termAlternatives.isEmpty()) return emptyList()
        return catalog.foods.asSequence()
            .filter { food ->
                val candidate = searchKey("${food.description} ${food.category.orEmpty()}")
                termAlternatives.any { terms -> terms.all(candidate::contains) }
            }
            .sortedWith(compareBy<CatalogFood> { !termAlternatives.any { terms -> searchKey(it.description).startsWith(terms.first()) } }
                .thenBy { it.description.length }
                .thenBy { it.description })
            .take(limit)
            .toList()
    }

    /**
     * M3 local tool result: after the model identifies food words, this host-side
     * lookup supplies only relevant offline candidates. The model never calculates
     * macros and the member must still confirm one candidate per ingredient.
     */
    fun candidatesForAssistantItem(
        catalog: OfflineFoodCatalog,
        assistantFoodName: String,
        limit: Int = 3,
    ): List<CatalogFood> {
        val alternatives = searchTermAlternatives(assistantFoodName)
        return alternatives.asSequence().flatMap { terms ->
            val foodTerms = terms.filterNot(preparationTerms::contains)
            if (foodTerms.isEmpty()) return@flatMap emptySequence()
            catalog.foods.asSequence()
                .filter { food ->
                    val candidate = searchKey("${food.description} ${food.category.orEmpty()}")
                    foodTerms.all(candidate::contains)
                }
                .sortedWith(compareBy<CatalogFood> { it.description.length }.thenBy { it.description })
        }.distinctBy(CatalogFood::fdcId).take(limit).toList()
    }

    /** A conservative first candidate for the review form. */
    fun suggest(catalog: OfflineFoodCatalog, assistantFoodName: String): CatalogFood? =
        candidatesForAssistantItem(catalog, assistantFoodName, limit = 1).firstOrNull()

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

    fun memberDisplayName(food: CatalogFood): String =
        spanishDisplayName(food)?.takeIf { Locale.getDefault().language == "es" } ?: food.description

    internal fun spanishDisplayName(food: CatalogFood): String? =
        food.spanishDisplayName ?: generatedSpanishDisplayName(food.description)

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
            val fdcId = food.getLong("fdc_id")
            add(CatalogFood(
                fdcId = fdcId,
                description = food.getString("description"),
                spanishDisplayName = spanishDisplayNames[fdcId],
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

    private fun expandAliases(query: String): String {
        var normalized = " ${searchKey(query)} "
        spanishAliases.forEach { (spanish, english) ->
            normalized = normalized.replace(" $spanish ", " $english ")
        }
        return normalized.trim()
    }

    /** Two-character Spanish type-ahead, intentionally separate from exact matching. */
    private fun searchTermAlternatives(query: String): List<List<String>> {
        val normalized = searchKey(query)
        if (normalized.length < 2) return emptyList()
        val aliasPrefixes = spanishAliases.filter { (spanish, _) -> spanish.startsWith(normalized) }
            .map { (_, english) -> searchKey(english).split(' ').filter(String::isNotBlank) }
            .distinct()
        if (aliasPrefixes.isNotEmpty()) return aliasPrefixes
        return listOf(expandAliases(normalized).split(' ').filter(String::isNotBlank))
    }

    private fun generatedSpanishDisplayName(description: String): String? {
        var translated = description.lowercase(Locale.ROOT)
        var replacements = 0
        spanishDescriptionRules.forEach { (english, spanish) ->
            val expression = "\\b${Regex.escape(english)}\\b".toRegex()
            val replaced = translated.replace(expression, spanish)
            if (replaced != translated) replacements += 1
            translated = replaced
        }
        return translated.takeIf { replacements > 0 }?.replaceFirstChar { it.titlecase(Locale("es", "ES")) }
    }
}
