package app.metria

import java.util.Locale

/** Reuses only a confirmed, source-bound quantity for an otherwise unspecified drink. */
object DrinkDefaultsPolicy {
    private val amountWords = Regex(
        "\\b(?:zero|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|" +
            "sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|" +
            "half|quarter|third|double|single|triple|twice|couple|pair|dozen|another|second|" +
            "bit|handful|bunch|plenty|lots|loads|" +
            "small|medium|large|tall|short|grande|venti|little|more|less|extra|few|some|several|" +
            "cup|cups|mug|mugs|glass|glasses|bottle|bottles|can|cans|shot|shots|sip|sips|" +
            "ml|l|g|kg|milliliters?|millilitres?|liters?|litres?|grams?|kilograms?|ounces?|oz|servings?|" +
            "pints?|quarts?|gallons?|teaspoons?|tablespoons?|tsp|tbsp|" +
            "same|usual|different|instead|not|no|without)\\b", RegexOption.IGNORE_CASE,
    )

    /** Only host-authored text may grant this permission; photos/model JSON default to false. */
    fun allowsDefault(memberText: String): Boolean = memberText.length in 1..600 &&
        memberText.none { it.isDigit() || it in "¼½¾⅓⅔⅛⅜⅝⅞" } &&
        !amountWords.containsMatchIn(memberText)

    fun previousPortion(
        name: String,
        selectedFood: CatalogFood,
        catalog: OfflineFoodCatalog,
        savedMeals: List<MealEntry>,
    ): DrinkPortion? {
        if (!DrinkPortionPolicy.isDrink(selectedFood)) return null
        val key = identity(name).takeIf(String::isNotEmpty) ?: return null
        // An explicit subsequent gram-only save also retires a previous unit preference.
        val latestMeal = savedMeals.sortedByDescending(MealEntry::recordedAt).firstOrNull { meal ->
            meal.items.any { identity(it.name) == key && it.catalogFoodId == selectedFood.fdcId }
        } ?: return null
        val latest = latestMeal.items.filter { identity(it.name) == key && it.catalogFoodId == selectedFood.fdcId }
            .singleOrNull() ?: return null
        if (!DrinkPortionPolicy.valid(latest)) return null
        val portion = latest.drinkPortion ?: return null
        return DrinkPortionPolicy.rebind(portion, selectedFood, catalog.sourceRelease)
    }

    private fun identity(name: String): String = name.trim().lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
}
