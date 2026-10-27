package app.metria

import java.util.Locale

/** A quantity stated by the member; it has no conversion authority until bound to a catalog food. */
data class ChatDrinkQuantity(val amount: Double, val unit: String, val measure: String? = null)

sealed interface ChatDrinkPortionReply {
    data class Quantity(val value: ChatDrinkQuantity) : ChatDrinkPortionReply
    data object Invalid : ChatDrinkPortionReply
    data object Unrelated : ChatDrinkPortionReply
}

/** English amount parsing for drinks. The catalog remains the only source of nutrition conversion. */
object ChatDrinkPortionPolicy {
    private val number = "(?:[0-9]{1,5}(?:[.,][0-9]{1,4})?|[0-9]{1,2}/[0-9]{1,2}|a|an|one|two|three|four|five|half|a half|quarter|a quarter)"
    private val measure = "(?:milliliters?|millilitres?|ml|cups?|fluid ounces?|fl\\s?oz|servings?|glass(?:es)?|mugs?|bottles?|shots?)"
    private val quantity = Regex("^\\s*($number)(?:\\s+a)?\\s*($measure)(?:\\s+of)?\\s*$", RegexOption.IGNORE_CASE)
    private val framedQuantity = Regex(
        "^\\s*(?:(?:actually|instead)\\s*,?\\s*)?(?:(?:make|change)\\s+(?:it|that)\\s+(?:to)?\\s*|(?:it|that)\\s+(?:was|is)\\s+|i\\s+(?:meant|mean)\\s+)?($number)(?:\\s+a)?\\s*($measure)(?:\\s+of)?[.!]?\\s*$",
        RegexOption.IGNORE_CASE,
    )
    private val amountMeasureAtStart = Regex("^\\s*(?:(?:actually|instead)\\s*,?\\s*|(?:make|change)\\s+(?:it|that)\\s+(?:to)?\\s*)?(?:(?:no|not)\\s+)?(?:$number|[-+]?[0-9]+(?:[.,][0-9]+)?(?:\\s*(?:-|to)\\s*[-+]?[0-9]+(?:[.,][0-9]+)?)?)(?:\\s+a)?\\s*(milliliters?|millilitres?|ml|cups?|fluid ounces?|fl\\s?oz|ounces?|oz|servings?|glass(?:es)?|mugs?|bottles?|shots?)\\b", RegexOption.IGNORE_CASE)
    private val consumption = Regex(
        "^\\s*(?:i had|i drank|i have had|i've had|i just had|i've just had|i have just had)\\s+($number)(?:\\s+a)?\\s*($measure)(?:\\s+of)?\\s+(.+?)(?:\\s+(?:for|as)\\s+(?:my\\s+)?(?:breakfast|lunch|dinner|a snack|snack))?[.!]?\\s*$",
        RegexOption.IGNORE_CASE,
    )
    private val knownReportDrink = Regex("^(?:a |an |some )?(coffee|tea|water|milk|espresso|latte|cappuccino|hot chocolate|orange juice|apple juice|juice)$", RegexOption.IGNORE_CASE)

    /** Classify a direct amount reply or natural correction. Whole g replies stay on the existing path. */
    fun parseReply(text: String): ChatDrinkPortionReply {
        val candidate = text.trim()
        val match = framedQuantity.matchEntire(candidate) ?: quantity.matchEntire(candidate)
        if (match == null) return if (amountMeasureAtStart.containsMatchIn(candidate) && !Regex("\\b(?:what|how|when|where|why|show|list|tell|can|could)\\b", RegexOption.IGNORE_CASE).containsMatchIn(candidate)) {
            ChatDrinkPortionReply.Invalid
        } else ChatDrinkPortionReply.Unrelated
        val value = parseNumber(match.groupValues[1]) ?: return ChatDrinkPortionReply.Invalid
        val token = match.groupValues[2].lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        val unit = if (token in setOf("ml", "milliliter", "milliliters", "millilitre", "millilitres")) "ml" else "serving"
        if (value <= 0.0 || value > if (unit == "ml") 10_000.0 else 100.0) return ChatDrinkPortionReply.Invalid
        val servingMeasure = if (unit == "serving") when {
            token.startsWith("cup") -> "cup"
            token in setOf("fluid ounce", "fluid ounces", "fl oz") -> "oz"
            token.startsWith("serving") -> "serving"
            token == "glass" || token == "glasses" -> "glass"
            token == "mug" || token == "mugs" -> "mug"
            token == "bottle" || token == "bottles" -> "bottle"
            token == "shot" || token == "shots" -> "shot"
            else -> null
        } else null
        if (unit == "serving" && servingMeasure == null) return ChatDrinkPortionReply.Invalid
        return ChatDrinkPortionReply.Quantity(ChatDrinkQuantity(value, unit, servingMeasure))
    }

    /** Only consume a closed positive report carrying one literal amount. */
    fun reportQuantity(text: String): ChatDrinkQuantity? {
        val match = consumption.matchEntire(text) ?: return null
        if (!knownReportDrink.matches(match.groupValues[3].trim())) return null
        val parsed = parseReply("${match.groupValues[1]} ${match.groupValues[2]}") as? ChatDrinkPortionReply.Quantity ?: return null
        return parsed.value
    }

    fun resolve(
        request: ChatDrinkQuantity,
        foodName: String,
        description: String,
        catalog: OfflineFoodCatalog?,
        corrections: List<FoodCorrection> = emptyList(),
    ): DrinkPortion? {
        catalog ?: return null
        if (FoodCatalog.isAmbiguousModelFood(foodName)) return null
        val food = FoodCatalog.suggest(catalog, foodName, description, corrections) ?: return null
        if (!DrinkPortionPolicy.isDrink(food)) return null
        val options = DrinkPortionPolicy.options(food, catalog.sourceRelease).filter { it.unit == request.unit }
        val resolvedOption = when (request.unit) {
            "ml" -> options.singleOrNull()
            "serving" -> options.filter { option ->
                val primaryMeasure = sourceMeasure(option.label)
                when (request.measure) {
                    "cup" -> primaryMeasure in setOf("cup", "cups")
                    "oz" -> primaryMeasure in setOf("fl oz", "fluid ounce", "fluid ounces")
                    else -> primaryMeasure == request.measure
                }
            }.let { candidates ->
                val exactBasis = candidates.filter { sourceAmount(it.label) == request.amount }
                val oneUnitBasis = candidates.filter { sourceAmount(it.label) == 1.0 }
                when {
                    request.measure == "cup" -> oneUnitBasis.singleOrNull()
                    exactBasis.size == 1 -> exactBasis.single()
                    exactBasis.size > 1 -> null
                    else -> oneUnitBasis.singleOrNull()
                }
            }
            else -> null
        } ?: return null
        val basis = if (resolvedOption.unit == "serving") sourceAmount(resolvedOption.label) ?: return null else 1.0
        if (basis <= 0.0 || resolvedOption.unit == "serving" && request.amount / basis > 100.0) return null
        return DrinkPortionPolicy.resolve(resolvedOption, DrinkPortionPolicy.decimal(request.amount / basis))
    }

    private fun parseNumber(raw: String): Double? {
        val value = when (raw.lowercase(Locale.ROOT).trim()) {
            "a", "an", "one" -> 1.0
            "two" -> 2.0
            "three" -> 3.0
            "four" -> 4.0
            "five" -> 5.0
            "half", "a half" -> 0.5
            "quarter", "a quarter" -> 0.25
            else -> raw.trim().replace(',', '.').let { text ->
                if ('/' in text) {
                    val pieces = text.split('/')
                    val numerator = pieces.getOrNull(0)?.toDoubleOrNull() ?: return null
                    val denominator = pieces.getOrNull(1)?.toDoubleOrNull()?.takeIf { it != 0.0 } ?: return null
                    numerator / denominator
                } else text.toDoubleOrNull()
            }
        }
        return value?.takeIf { it.isFinite() }
    }

    private fun sourceMeasure(label: String): String? = Regex(
        "^\\s*[0-9]+(?:[.,][0-9]+)?\\s*([a-z]+(?:\\s+[a-z]+)?)",
        RegexOption.IGNORE_CASE,
    ).find(label)?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT)?.replace(Regex("\\s+"), " ")

    private fun sourceAmount(label: String): Double? = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*[a-z]", RegexOption.IGNORE_CASE)
        .find(label)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
}
