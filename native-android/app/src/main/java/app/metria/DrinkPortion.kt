package app.metria

import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt

/** A member-entered drink quantity and the immutable catalog factor used to derive grams. */
data class DrinkPortion(
    val amount: Double,
    /** `ml` or a catalog-backed `serving`. */
    val unit: String,
    /** Member-facing measure label, such as `mL` or `1 fl oz`. */
    val label: String,
    /** Grams represented by one unit of [amount]. */
    val gramsPerUnit: Double,
    val catalogFoodId: Long,
    val catalogRelease: String,
    /** Exact catalog portion label that supports this conversion factor. */
    val sourceLabel: String,
)

data class DrinkPortionOption(
    val label: String,
    val unit: String,
    val gramsPerUnit: Double,
    val catalogFoodId: Long,
    val catalogRelease: String,
    val sourceLabel: String,
)

/** Rounded member-facing text; the source portion remains the calculation authority. */
data class DrinkPortionDisplay(val amountText: String, val isApproximate: Boolean)

/**
 * Portion conversions are grounded in a USDA serving with both a gram weight and
 * explicit volume. Cups without a source volume remain servings; no density or
 * cup size is inferred. Nutrition continues to use the rounded canonical grams.
 */
object DrinkPortionPolicy {
    private const val ML = "ml"
    private const val SERVING = "serving"
    private const val MAX_AMOUNT_ML = 10_000.0
    private const val MAX_AMOUNT_SERVINGS = 100.0
    private val amountPattern = Regex("(?:[0-9]+(?:[.,][0-9]{1,6})?|[.,][0-9]{1,6})")

    fun optionOf(portion: DrinkPortion) = DrinkPortionOption(portion.label, portion.unit,
        portion.gramsPerUnit, portion.catalogFoodId, portion.catalogRelease, portion.sourceLabel)

    fun edited(portion: DrinkPortion?, text: String?): DrinkPortion? = portion?.let {
        if (text == null) it.takeIf { canonicalGrams(it) != null } else resolve(optionOf(it), text)
    }

    fun fromGrams(option: DrinkPortionOption, grams: Int): DrinkPortion? =
        if (grams !in 1..2_000 || !valid(option)) null else resolve(option,
            BigDecimal.valueOf(grams / option.gramsPerUnit).setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString())

    fun isDrink(food: CatalogFood): Boolean {
        val category = food.category.orEmpty().lowercase(Locale.ROOT)
        if (category.contains("beverage") || category.contains("coffee and tea")) return true
        val description = food.description.trim().lowercase(Locale.ROOT)
        if (Regex("\\bjuice\\b").containsMatchIn(description)) return true
        return listOf(
            "beverage", "coffee,", "coffee ", "tea,", "tea ", "water,", "water ",
            "juice,", "juice ", "milk,", "milk ", "soft drink", "drink,", "drink ",
            "espresso", "latte", "broth,", "broth ", "beer,", "wine,",
        ).any(description::startsWith)
    }

    /** Serving options always use their USDA gram weights. mL needs explicit volume evidence. */
    fun options(food: CatalogFood, catalogRelease: String): List<DrinkPortionOption> {
        if (!isDrink(food) || food.fdcId <= 0L || catalogRelease.isBlank()) return emptyList()
        val servings = food.portions.mapNotNull { portion ->
            val label = portion.label?.trim()?.takeIf { it.length in 1..120 } ?: return@mapNotNull null
            if (!validFactor(portion.grams)) return@mapNotNull null
            DrinkPortionOption(
                label = label,
                unit = SERVING,
                gramsPerUnit = portion.grams,
                catalogFoodId = food.fdcId,
                catalogRelease = catalogRelease,
                sourceLabel = label,
            )
        }.distinctBy { it.label to it.gramsPerUnit }

        // A larger explicitly measured serving usually has less relative source rounding.
        // Keep the USDA label as provenance so the factor can be rebound after review edits.
        val volumeBasis = food.portions.asSequence()
            .filter { portion ->
                val volume = portion.volumeMilliliters
                portion.label?.isNotBlank() == true &&
                    validFactor(portion.grams) && volume != null && volume.isFinite() && volume > 0.0 &&
                    volume <= 10_000.0 && validFactor(portion.grams / volume)
            }
            .maxByOrNull { it.volumeMilliliters!! }
        val milliliters = volumeBasis?.let { portion ->
            DrinkPortionOption(
                label = "mL",
                unit = ML,
                gramsPerUnit = portion.grams / portion.volumeMilliliters!!,
                catalogFoodId = food.fdcId,
                catalogRelease = catalogRelease,
                sourceLabel = portion.label!!.trim(),
            )
        }
        return (servings + listOfNotNull(milliliters)).distinctBy { Triple(it.unit, it.label, it.sourceLabel) }
    }

    /** Parse a plain member-entered amount without rounding its quantity. */
    fun resolve(option: DrinkPortionOption, amountText: String): DrinkPortion? {
        if (!valid(option)) return null
        val trimmed = amountText.trim()
        if (trimmed.length !in 1..24 || !amountPattern.matches(trimmed) || trimmed.count { it == ',' } > 1 ||
            (',' in trimmed && '.' in trimmed)
        ) return null
        val amount = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
        val portion = DrinkPortion(
            amount = amount,
            unit = option.unit,
            label = option.label,
            gramsPerUnit = option.gramsPerUnit,
            catalogFoodId = option.catalogFoodId,
            catalogRelease = option.catalogRelease,
            sourceLabel = option.sourceLabel,
        )
        return portion.takeIf { valid(it) && canonicalGrams(it) != null }
    }

    /** Convert once to canonical whole grams. Out-of-range conversions are rejected, never clamped. */
    fun canonicalGrams(portion: DrinkPortion): Int? {
        if (!valid(portion)) return null
        val grams = portion.amount * portion.gramsPerUnit
        if (!grams.isFinite() || grams < 0.5 || grams >= 2_000.5) return null
        return grams.roundToInt().takeIf { it in 1..2_000 }
    }

    fun valid(item: MealEntryItem): Boolean {
        val portion = item.drinkPortion ?: return true
        return item.role == "drink" && item.grams != null &&
            item.catalogFoodId == portion.catalogFoodId && item.catalogRelease == portion.catalogRelease &&
            canonicalGrams(portion) == item.grams
    }

    fun format(portion: DrinkPortion): String? {
        if (!valid(portion)) return null
        val display = display(portion) ?: return null
        return when (portion.unit) {
            ML -> "${if (display.isApproximate) "About " else ""}${display.amountText} mL"
            SERVING -> "${if (display.isApproximate) "About " else ""}${display.amountText} × ${portion.label}"
            else -> null
        }
    }

    /**
     * Makes a compact display value without changing [DrinkPortion.amount]. The value is
     * marked approximate whenever rounded text differs from the stored quantity. mL keeps one
     * decimal place; servings keep two, with extra precision only to avoid displaying a
     * positive amount as zero.
     */
    fun display(portion: DrinkPortion): DrinkPortionDisplay? {
        if (!valid(portion)) return null
        var scale = if (portion.unit == ML) 1 else 2
        var rounded = BigDecimal.valueOf(portion.amount).setScale(scale, java.math.RoundingMode.HALF_UP)
        while (rounded.compareTo(BigDecimal.ZERO) == 0 && scale < 6) {
            scale++
            rounded = BigDecimal.valueOf(portion.amount).setScale(scale, java.math.RoundingMode.HALF_UP)
        }
        val amountText = rounded.stripTrailingZeros().toPlainString()
        val displayAmount = amountText.toDoubleOrNull() ?: return null
        val approximate = BigDecimal.valueOf(displayAmount).compareTo(BigDecimal.valueOf(portion.amount)) != 0
        return DrinkPortionDisplay(amountText, approximate)
    }

    fun scale(portion: DrinkPortion, factor: Double): DrinkPortion? {
        if (!valid(portion) || !factor.isFinite() || factor <= 0.0) return null
        val amount = portion.amount * factor
        if (!amount.isFinite()) return null
        val scaled = portion.copy(amount = BigDecimal.valueOf(amount).setScale(6, java.math.RoundingMode.HALF_UP).toDouble())
        return scaled.takeIf { valid(it) && canonicalGrams(it) != null }
    }

    /** A catalog update retains the unit only when the same food, measure and factor still exist. */
    fun rebind(portion: DrinkPortion, food: CatalogFood, catalogRelease: String): DrinkPortion? {
        if (!valid(portion) || portion.catalogFoodId != food.fdcId) return null
        val option = options(food, catalogRelease).firstOrNull {
            it.unit == portion.unit && it.sourceLabel == portion.sourceLabel && it.label == portion.label
        } ?: return null
        if (kotlin.math.abs(option.gramsPerUnit - portion.gramsPerUnit) > 1e-9) return null
        return portion.copy(catalogRelease = catalogRelease).takeIf { canonicalGrams(it) != null }
    }

    fun forCatalog(portion: DrinkPortion, catalog: OfflineFoodCatalog?): DrinkPortion? {
        if (catalog == null) return portion
        val food = catalog.foods.singleOrNull { it.fdcId == portion.catalogFoodId } ?: return null
        return rebind(portion, food, catalog.sourceRelease)
    }

    private fun valid(option: DrinkPortionOption): Boolean =
        option.unit in setOf(ML, SERVING) && safeText(option.label) && safeText(option.sourceLabel) &&
            validFactor(option.gramsPerUnit) && option.catalogFoodId in 1L..9_000_000_000_000_000L && safeText(option.catalogRelease) &&
            (option.unit != ML || option.label == "mL")

    private fun valid(portion: DrinkPortion): Boolean {
        val maxAmount = when (portion.unit) {
            ML -> MAX_AMOUNT_ML
            SERVING -> MAX_AMOUNT_SERVINGS
            else -> return false
        }
        return portion.amount.isFinite() && portion.amount > 0.0 && portion.amount <= maxAmount &&
            valid(optionOf(portion))
    }

    private fun safeText(value: String) = value.trim().length in 1..120 && value.none(Character::isISOControl)

    private fun validFactor(value: Double): Boolean = value.isFinite() && value > 0.0 && value <= 2_000.0

    fun decimal(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    fun itemLabel(item: MealEntryItem): String? = item.drinkPortion?.takeIf { valid(item) }?.let(::format)
        ?: item.grams?.let { "$it g" }
}

/** Strict vault/archive representation for optional drink quantity provenance. */
object DrinkPortionCodec {
    fun readItem(item: JSONObject): DrinkPortion? {
        if (!item.has("drinkPortion") || item.isNull("drinkPortion")) return null
        return decode(item.getJSONObject("drinkPortion"))
    }
    fun encode(portion: DrinkPortion): JSONObject = JSONObject()
        .put("version", 1)
        .put("amount", portion.amount)
        .put("unit", portion.unit)
        .put("label", portion.label)
        .put("gramsPerUnit", portion.gramsPerUnit)
        .put("catalogFoodId", portion.catalogFoodId)
        .put("catalogRelease", portion.catalogRelease)
        .put("sourceLabel", portion.sourceLabel)

    fun decode(value: JSONObject): DrinkPortion {
        require(value.opt("version") == 1) { "Unsupported drink portion." }
        fun number(name: String): Double {
            val raw = value.opt(name)
            require(raw is Number) { "Invalid drink portion." }
            return raw.toDouble().also { require(it.isFinite()) { "Invalid drink portion." } }
        }
        fun text(name: String): String = value.opt(name) as? String
            ?: throw IllegalArgumentException("Invalid drink portion.")

        val rawId = value.opt("catalogFoodId")
        require(rawId is Number && rawId.toDouble().isFinite() && rawId.toDouble() % 1.0 == 0.0) {
            "Invalid drink portion."
        }
        return DrinkPortion(
            amount = number("amount"),
            unit = text("unit"),
            label = text("label"),
            gramsPerUnit = number("gramsPerUnit"),
            catalogFoodId = rawId.toLong(),
            catalogRelease = text("catalogRelease"),
            sourceLabel = text("sourceLabel"),
        ).also { require(DrinkPortionPolicy.canonicalGrams(it) != null) { "Invalid drink portion." } }
    }
}
