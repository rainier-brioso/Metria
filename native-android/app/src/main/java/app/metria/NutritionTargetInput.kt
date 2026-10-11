package app.metria

/** Parsing for optional member-entered references; limits are storage bounds, not advice. */
internal object NutritionTargetInput {
    fun parse(fields: List<String>): Result<NutritionTargets?> = runCatching {
        require(fields.size == 4)
        val names = listOf("calories", "protein", "carbohydrates", "fat")
        val maximums = listOf(10_000.0, 1_000.0, 2_000.0, 1_000.0)
        val values = fields.mapIndexed { index, raw ->
            val text = raw.trim()
            if (text.isEmpty()) null else {
                val value = text.replace(',', '.').toDoubleOrNull()
                require(value != null && value.isFinite() && value in 1.0..maximums[index]) {
                    "Check your ${names[index]} value, or leave it empty."
                }
                value
            }
        }
        NutritionTargets(values[0], values[1], values[2], values[3]).takeUnless(NutritionTargetValidation::empty)
    }
}
