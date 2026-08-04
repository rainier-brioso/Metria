package app.metria

data class LabelNutritionDraft(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val totalFatGrams: Double?,
)

/** A redacted local OCR line with its image position; never persisted. */
data class OcrTextLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/** Deterministic extraction from local OCR text; it never treats OCR as exact. */
object LabelNutritionParser {
    private val caloriesLabel = Regex("\\b(?:calories?|calor[iÃ­]as?|energ[iÃ­]a)\\b", RegexOption.IGNORE_CASE)
    private val proteinLabel = Regex("\\b(?:protein(?:a|as)?|prote[iÃ­]nas?)\\b", RegexOption.IGNORE_CASE)
    private val carbohydrateLabel = Regex("\\b(?:total\\s+carb(?:ohydrates?)?|carbohydrates?|carbohidratos?)\\b", RegexOption.IGNORE_CASE)
    private val fatLabel = Regex("\\b(?:total\\s+fat|fat|grasas?\\s+totales?)\\b", RegexOption.IGNORE_CASE)
    private val calories = Regex(
        "\\b(?:calories?|calor[ií]as?|energ[ií]a)\\b[^\\d]{0,20}(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:kcal|cal)?",
        RegexOption.IGNORE_CASE,
    )
    private val protein = Regex(
        "\\b(?:protein(?:a|as)?|prote[ií]nas?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val carbohydrate = Regex(
        "\\b(?:total\\s+carb(?:ohydrates?)?|carbohydrates?|carbohidratos?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val totalFat = Regex(
        "\\b(?:total\\s+fat|fat|grasas?\\s+totales?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String, lines: List<OcrTextLine> = emptyList()): LabelNutritionDraft? {
        val draft = LabelNutritionDraft(
            caloriesKcal = calories.valueFrom(text) ?: spatialValue(lines, caloriesLabel, null),
            proteinGrams = protein.valueFrom(text) ?: spatialValue(lines, proteinLabel, "g"),
            carbohydrateGrams = carbohydrate.valueFrom(text) ?: spatialValue(lines, carbohydrateLabel, "g"),
            totalFatGrams = totalFat.valueFrom(text) ?: spatialValue(lines, fatLabel, "g"),
        )
        return draft.takeIf {
            it.caloriesKcal != null || it.proteinGrams != null ||
                it.carbohydrateGrams != null || it.totalFatGrams != null
        }
    }

    private fun Regex.valueFrom(text: String): Double? = find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it >= 0.0 }

    /**
     * Nutrition Facts tables are often emitted in column order by OCR. Match a
     * label to a numeric line beside it at the same vertical position instead.
     * The left-most matching value is the usual per-serving column.
     */
    private fun spatialValue(lines: List<OcrTextLine>, label: Regex, unit: String?): Double? {
        val labelLine = lines.firstOrNull { label.containsMatchIn(it.text) } ?: return null
        val tolerance = maxOf(28, labelLine.height * 2)
        return lines.asSequence()
            .filter { candidate -> candidate.left >= labelLine.right - 8 && kotlin.math.abs(candidate.centerY - labelLine.centerY) <= tolerance }
            .sortedWith(compareBy<OcrTextLine> { kotlin.math.abs(it.centerY - labelLine.centerY) }.thenBy(OcrTextLine::left))
            .mapNotNull { amountFrom(it.text, unit) }
            .firstOrNull()
    }

    private fun amountFrom(value: String, unit: String?): Double? {
        val pattern = if (unit == null) {
            "(\\d{1,4}(?:[.,]\\d{1,2})?|O(?:[.,]\\d{1,2})?)"
        } else {
            "(\\d{1,4}(?:[.,]\\d{1,2})?|O(?:[.,]\\d{1,2})?)\\s*${Regex.escape(unit)}\\b"
        }
        val match = Regex(pattern, RegexOption.IGNORE_CASE).find(value)
            ?: return null
        return match.groupValues[1].replace('O', '0').replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }
    }
}
