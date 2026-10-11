package app.metria

import java.time.LocalDate

/** Narrow host-composed answers for the one saved meal explicitly selected by the member. */
object SelectedSavedMealReply {
    private enum class Ask { SUMMARY, FOODS, CALORIES, PROTEIN, CARBS, FAT, UNSUPPORTED }

    fun supportsQuestion(question: String): Boolean = classify(question, spanish = false) != Ask.UNSUPPORTED

    /** Re-resolves one exact opaque vault ID; never falls back to title, date, or list position. */
    fun read(meals: List<MealEntry>, selectedMealId: String?, replyLanguage: String): Map<String, Any?>? {
        val id = selectedMealId?.takeIf(String::isNotBlank) ?: return null
        val meal = meals.singleOrNull { it.id == id }?.takeIf { MealEntryValidation.valid(it) && it.items.size <= 50 } ?: return null
        val nutrition = meal.nutrition
        return mapOf(
            "source" to "metria_local_vault",
            "record_id" to id,
            "reply_language" to replyLanguage,
            "meal" to mapOf(
                "id" to meal.id,
                "title" to MealIdentity.displayTitle(meal),
                "date" to meal.recordedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                "foods" to meal.items.take(50).map { item -> mapOf("name" to item.name, "grams" to item.grams) },
                "nutrition" to mapOf(
                    "calories_kcal" to nutrition?.caloriesKcal,
                    "protein_g" to nutrition?.proteinGrams,
                    "carbs_g" to nutrition?.carbohydrateGrams,
                    "fat_g" to nutrition?.fatGrams,
                    "coverage" to when {
                        nutrition == null -> "missing"
                        nutrition.estimateCoverage == "partial" -> "partial"
                        listOf(nutrition.caloriesKcal, nutrition.proteinGrams, nutrition.carbohydrateGrams, nutrition.fatGrams).all { it != null } -> "full"
                        else -> "partial"
                    },
                    "source" to nutrition?.source.orEmpty().ifBlank { "none" },
                ),
            ),
        )
    }

    fun from(required: RequiredAgentTool, response: Map<String, Any?>): String? {
        if (required.name != "read_selected_meal") return null
        val mealId = required.selectedMealId?.takeIf(String::isNotBlank) ?: return null
        if (response["source"] != "metria_local_vault" || response["record_id"] != mealId) return null
        val meal = response["meal"] as? Map<*, *> ?: return null
        if (meal["id"] != mealId) return null
        val title = (meal["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= 320 } ?: return null
        val date = (meal["date"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val rawFoods = meal["foods"] as? List<*> ?: return null
        if (rawFoods.isEmpty() || rawFoods.size > 12) return null
        val foods = rawFoods.map { raw ->
            val food = raw as? Map<*, *> ?: return null
            val name = (food["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= 320 } ?: return null
            val grams = when (val value = food["grams"]) {
                null -> null
                is Number -> value.toInt().takeIf { value.toDouble() == it.toDouble() && it in 1..2_000 } ?: return null
                else -> return null
            }
            name to grams
        }
        val nutrition = meal["nutrition"] as? Map<*, *> ?: return null
        val calories = number(nutrition["calories_kcal"]) ?: if (nutrition["calories_kcal"] == null) null else return null
        val protein = number(nutrition["protein_g"]) ?: if (nutrition["protein_g"] == null) null else return null
        val carbs = number(nutrition["carbs_g"]) ?: if (nutrition["carbs_g"] == null) null else return null
        val fat = number(nutrition["fat_g"]) ?: if (nutrition["fat_g"] == null) null else return null
        val coverage = (nutrition["coverage"] as? String)?.takeIf { it in setOf("full", "partial", "missing") } ?: return null
        val nutritionSource = (nutrition["source"] as? String)?.takeIf { it in setOf("verified_label", "member_confirmed_catalog", "manual_entry", "none") } ?: return null
        val values = listOf(calories, protein, carbs, fat)
        if ((coverage == "full" && values.any { it == null }) ||
            (coverage == "missing" && values.any { it != null }) ||
            (coverage == "partial" && values.all { it == null }) ||
            (nutritionSource == "none") != (coverage == "missing")
        ) return null
        val language = response["reply_language"] as? String ?: return null
        val spanish = when (language) { "Spanish" -> true; "English" -> false; else -> return null }
        val ask = classify(required.selectedMealQuestion.orEmpty(), spanish)
        val value = when (ask) {
            Ask.CALORIES -> calories?.let { "${display(it)} kcal" }
            Ask.PROTEIN -> protein?.let { "${display(it)} g" }
            Ask.CARBS -> carbs?.let { "${display(it)} g" }
            Ask.FAT -> fat?.let { "${display(it)} g" }
            else -> null
        }
        val metricName = when (ask) {
            Ask.CALORIES -> if (spanish) "calorías" else "calories"
            Ask.PROTEIN -> if (spanish) "proteína" else "protein"
            Ask.CARBS -> if (spanish) "carbohidratos" else "carbohydrates"
            Ask.FAT -> if (spanish) "grasa" else "fat"
            else -> ""
        }
        val sourceLabel = when (nutritionSource) {
            "verified_label" -> if (spanish) "etiqueta verificada" else "verified label"
            "member_confirmed_catalog" -> if (spanish) "estimación del catálogo confirmada por ti" else "member-confirmed catalog estimate"
            "manual_entry" -> if (spanish) "valor introducido por ti" else "member-entered value"
            else -> if (spanish) "sin estimación guardada" else "no saved estimate"
        }
        return when (ask) {
            Ask.UNSUPPORTED -> if (spanish) {
                "Puedo consultar los alimentos y valores nutricionales guardados de esta comida, pero no evaluar si es saludable ni inferir datos que no aparecen en el registro."
            } else {
                "I can check this meal's saved foods and nutrition values, but I can't judge whether it is healthy or infer details that are not in the record."
            }
            Ask.FOODS -> {
                val parts = foods.joinToString { (name, grams) -> if (grams == null) name else "$name (${grams} g)" }
                if (spanish) "En el registro de $title del $date aparecen: $parts." else "The saved $title record from $date lists: $parts."
            }
            Ask.CALORIES, Ask.PROTEIN, Ask.CARBS, Ask.FAT -> if (value == null) {
                if (spanish) "No hay un valor de $metricName guardado para $title del $date." else "No $metricName value is saved for $title from $date."
            } else {
                val coverageNote = when {
                    coverage == "partial" && spanish -> " El registro tiene una estimación parcial."
                    coverage == "partial" -> " The record has a partial estimate."
                    coverage == "missing" && spanish -> " No hay una estimación nutricional guardada."
                    coverage == "missing" -> " No nutrition estimate is saved."
                    else -> ""
                }
                if (spanish) "$metricName guardados para $title del $date: $value ($sourceLabel).$coverageNote"
                else "Saved $metricName for $title from $date: $value ($sourceLabel).$coverageNote"
            }
            Ask.SUMMARY -> {
                val items = foods.joinToString { (name, grams) -> if (grams == null) name else "$name (${grams} g)" }
                val metrics = listOf(
                    calories?.let { "${display(it)} kcal" } ?: if (spanish) "calorías sin dato" else "calories not recorded",
                    protein?.let { "${display(it)} g ${if (spanish) "proteína" else "protein"}" } ?: if (spanish) "proteína sin dato" else "protein not recorded",
                    carbs?.let { "${display(it)} g ${if (spanish) "carbohidratos" else "carbs"}" } ?: if (spanish) "carbohidratos sin dato" else "carbohydrates not recorded",
                    fat?.let { "${display(it)} g ${if (spanish) "grasa" else "fat"}" } ?: if (spanish) "grasa sin dato" else "fat not recorded",
                ).joinToString()
                val partial = when {
                    coverage == "partial" && spanish -> " Estimación parcial."
                    coverage == "partial" -> " Partial estimate."
                    coverage == "missing" && spanish -> " No hay estimación nutricional guardada."
                    coverage == "missing" -> " No nutrition estimate is saved."
                    else -> ""
                }
                if (spanish) "Registro guardado de $title ($date): $items. Valores nutricionales: $metrics ($sourceLabel).$partial"
                else "Saved record for $title ($date): $items. Nutrition values: $metrics ($sourceLabel).$partial"
            }
        }
    }

    private fun classify(question: String, spanish: Boolean): Ask {
        val text = java.text.Normalizer.normalize(question.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        if (Regex("\\b(healthy|healthier|good for me|bad for me|why|recommend|should i|saludable|sano|bueno para mi|malo para mi|recomiendas|deberia)\\b").containsMatchIn(text)) return Ask.UNSUPPORTED
        val metrics = buildList {
            if (Regex("\\b(calorie|calories|kcal|caloria|calorias)\\b").containsMatchIn(text)) add(Ask.CALORIES)
            if (Regex("\\b(protein|proteina|proteinas)\\b").containsMatchIn(text)) add(Ask.PROTEIN)
            if (Regex("\\b(carb|carbs|carbohydrate|carbohydrates|carbohidrato|carbohidratos)\\b").containsMatchIn(text)) add(Ask.CARBS)
            if (Regex("\\b(fat|fats|grasas?|lipids?)\\b").containsMatchIn(text)) add(Ask.FAT)
        }
        if (metrics.size == 1) return metrics.single()
        if (metrics.size > 1) return Ask.SUMMARY
        if (Regex("\\b(ingredients?|what foods? (?:are )?in|what is in|what's in|what does .+ contain|lleva|ingredientes?|que contiene|alimentos (?:de|en))\\b").containsMatchIn(text)) return Ask.FOODS
        if (question.isBlank()) return Ask.UNSUPPORTED
        if (Regex("\\b(about this meal|this meal|this record|esta comida|esta comida guardada|esta comida registrada|resumen de esta comida)\\b").containsMatchIn(text)) return Ask.SUMMARY
        return if (Regex("\\b(what is this|tell me about|summary|que es|resumen|cuentame sobre)\\b").containsMatchIn(text)) Ask.SUMMARY else Ask.UNSUPPORTED
    }

    private fun number(value: Any?): Double? = when (value) {
            is Number -> value.toDouble().takeIf { it.isFinite() && it in 0.0..10_000.0 }
        else -> null
    }

    private fun display(value: Double): String = java.math.BigDecimal.valueOf(value)
        .setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
