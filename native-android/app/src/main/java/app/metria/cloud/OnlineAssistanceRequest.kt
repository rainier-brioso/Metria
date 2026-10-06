package app.metria.cloud

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.json.JSONObject

enum class OnlineLanguage { English, Spanish }

data class OnlineWeightSummary(
    val count: Int,
    val firstDate: LocalDate?, val firstKg: Double?,
    val latestDate: LocalDate?, val latestKg: Double?,
    val changeKg: Double?,
)

data class OnlineNutritionSummary(
    val recordedMeals: Int, val completeMeals: Int, val partialMeals: Int, val missingMeals: Int,
    val completeCalories: Double?, val completeProtein: Double?,
    val partialCalories: Double?, val partialProtein: Double?,
)

/** Explicit inputs only: this module cannot obtain a vault, chat, image, note, or credential. */
data class OnlineAssistanceRequest(
    val requestId: UUID,
    val question: String,
    val language: OnlineLanguage,
    val start: LocalDate,
    val end: LocalDate,
    val model: String,
    val weight: OnlineWeightSummary? = null,
    val nutrition: OnlineNutritionSummary? = null,
) {
    fun valid(today: LocalDate): Boolean {
        if (question.isBlank() || question.length > 1_000 ||
            question.any { Character.isISOControl(it) && it != '\n' && it != '\t' }) return false
        if (end < start || end > today || ChronoUnit.DAYS.between(start, end) !in 0..6) return false
        // Availability is verified by the future broker; no model is silently selected here.
        if (!model.matches(Regex("nvidia/[A-Za-z0-9][A-Za-z0-9._-]{0,119}"))) return false
        weight?.let { w ->
            if (w.count !in 0..10_000) return false
            if (w.count == 0) {
                if (listOf(w.firstDate, w.firstKg, w.latestDate, w.latestKg, w.changeKg).any { it != null }) return false
            } else {
                val firstDate = w.firstDate ?: return false
                val latestDate = w.latestDate ?: return false
                val firstKg = w.firstKg?.takeIf { it.isFinite() && it in 1.0..500.0 } ?: return false
                val latestKg = w.latestKg?.takeIf { it.isFinite() && it in 1.0..500.0 } ?: return false
                if (firstDate !in start..end || latestDate !in firstDate..end) return false
                if (w.count == 1) {
                    if (firstDate != latestDate || firstKg != latestKg || w.changeKg != null) return false
                } else {
                    if (w.changeKg == null || !w.changeKg.isFinite() ||
                        rounded(w.changeKg, 2) != rounded(latestKg - firstKg, 2)) return false
                }
            }
        }
        nutrition?.let { n ->
            val counts = listOf(n.recordedMeals, n.completeMeals, n.partialMeals, n.missingMeals)
            if (counts.any { it !in 0..10_000 } || n.recordedMeals != n.completeMeals + n.partialMeals + n.missingMeals) return false
            if (!validEstimate(n.completeMeals, n.completeCalories, n.completeProtein) ||
                !validEstimate(n.partialMeals, n.partialCalories, n.partialProtein)) return false
        }
        return true
    }

    /** Exact preview and future relay body share one immutable canonical representation. */
    fun canonicalPayload(today: LocalDate): String? {
        if (!valid(today)) return null
        val json = JSONObject().put("schema_version", 1).put("request_id", requestId.toString())
            .put("provider", "nebius").put("model", model).put("question", question)
            .put("language", language.name).put("period", JSONObject().put("start", start).put("end", end))
        weight?.let { w -> json.put("weight", JSONObject().put("count", w.count).put("unit", "kg")
            .put("first_date", w.firstDate ?: JSONObject.NULL).put("latest_date", w.latestDate ?: JSONObject.NULL)
            .put("first_kg", w.firstKg?.let { rounded(it, 2) } ?: JSONObject.NULL)
            .put("latest_kg", w.latestKg?.let { rounded(it, 2) } ?: JSONObject.NULL)
            // Keep the preview's displayed endpoints and displayed change consistent.
            .put("change_kg", if (w.count >= 2) rounded(w.latestKg!!, 2).subtract(rounded(w.firstKg!!, 2)) else JSONObject.NULL)) }
        nutrition?.let { n -> json.put("nutrition", JSONObject().put("recorded_meals", n.recordedMeals)
            .put("complete_meals", n.completeMeals).put("partial_meals", n.partialMeals).put("missing_meals", n.missingMeals)
            .put("complete_calories_kcal", n.completeCalories?.let { rounded(it, 0) } ?: JSONObject.NULL)
            .put("complete_protein_g", n.completeProtein?.let { rounded(it, 1) } ?: JSONObject.NULL)
            .put("partial_calories_kcal", n.partialCalories?.let { rounded(it, 0) } ?: JSONObject.NULL)
            .put("partial_protein_g", n.partialProtein?.let { rounded(it, 1) } ?: JSONObject.NULL)
            .put("interpretation", "recorded_estimates_not_total_intake")) }
        return canonical(json)
    }

    private fun validEstimate(count: Int, calories: Double?, protein: Double?): Boolean =
        if (count == 0) calories == null && protein == null else
            calories != null && protein != null && calories.isFinite() && protein.isFinite() &&
                calories in 0.0..(count * 10_000.0) && protein in 0.0..(count * 10_000.0)

    private fun rounded(value: Double, scale: Int): BigDecimal = BigDecimal.valueOf(value)
        .setScale(scale, RoundingMode.HALF_UP)

    private fun canonical(value: JSONObject): String = value.keys().asSequence().toList().sorted()
        .joinToString(",", "{", "}") { key ->
            val field = value.get(key)
            JSONObject.quote(key) + ":" + when (field) {
                is JSONObject -> canonical(field)
                JSONObject.NULL -> "null"
                is Number -> field.toString()
                else -> JSONObject.quote(field.toString())
            }
        }
}
