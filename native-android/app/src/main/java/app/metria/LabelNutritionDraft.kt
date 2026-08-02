package app.metria

data class LabelNutritionDraft(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val carbohydrateGrams: Double?,
    val totalFatGrams: Double?,
)

/** Deterministic extraction from local OCR text; it never treats OCR as exact. */
object LabelNutritionParser {
    private val calories = Regex(
        "\\b(?:calories?|calor[ií]as?|energ[ií]a)\\b[^\\d]{0,20}(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:kcal|cal)?",
        RegexOption.IGNORE_CASE,
    )
    private val protein = Regex(
        "\\b(?:protein(?:a|as)?|prote[ií]nas?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val carbohydrate = Regex(
        "\\b(?:total\\s+carbohydrates?|carbohydrates?|carbohidratos?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )
    private val totalFat = Regex(
        "\\b(?:total\\s+fat|fat|grasas?\\s+totales?)\\b[^\\d]{0,20}(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*g\\b",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String): LabelNutritionDraft? {
        val draft = LabelNutritionDraft(
            caloriesKcal = calories.valueFrom(text),
            proteinGrams = protein.valueFrom(text),
            carbohydrateGrams = carbohydrate.valueFrom(text),
            totalFatGrams = totalFat.valueFrom(text),
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
}
