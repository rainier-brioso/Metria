package app.metria.cloud

import java.time.LocalDate
import java.util.UUID
import org.json.JSONObject

/** Disconnected broker contract. Authentication, replay protection and transport are separate gates. */
object OnlineBrokerContract {
    fun parseRequest(body: String, today: LocalDate, allowedModels: Set<String>): OnlineAssistanceRequest? = runCatching {
        val json = StrictOnlineJson.objectOrNull(body) ?: return null
        require(json.keysSet() - setOf("weight", "nutrition") == setOf(
            "schema_version", "request_id", "provider", "model", "question", "language", "period"))
        require(json.integer("schema_version") == 1 && json.string("provider") == "nebius")
        val id = json.string("request_id")
        val uuid = UUID.fromString(id)
        require(uuid.toString() == id)
        val model = json.string("model")
        require(model in allowedModels)
        val period = json.getJSONObject("period")
        require(period.keysSet() == setOf("start", "end"))
        val weight = if (json.has("weight")) json.getJSONObject("weight").let { w ->
            require(w.keysSet() == setOf("count", "unit", "first_date", "latest_date", "first_kg", "latest_kg", "change_kg"))
            require(w.string("unit") == "kg")
            OnlineWeightSummary(w.integer("count"), w.dateOrNull("first_date"), w.numberOrNull("first_kg", 2),
                w.dateOrNull("latest_date"), w.numberOrNull("latest_kg", 2), w.numberOrNull("change_kg", 2))
        } else null
        val nutrition = if (json.has("nutrition")) json.getJSONObject("nutrition").let { n ->
            require(n.keysSet() == setOf("recorded_meals", "complete_meals", "partial_meals", "missing_meals",
                "complete_calories_kcal", "complete_protein_g", "partial_calories_kcal", "partial_protein_g", "interpretation"))
            require(n.string("interpretation") == "recorded_estimates_not_total_intake")
            OnlineNutritionSummary(n.integer("recorded_meals"), n.integer("complete_meals"), n.integer("partial_meals"),
                n.integer("missing_meals"), n.numberOrNull("complete_calories_kcal", 0), n.numberOrNull("complete_protein_g", 1),
                n.numberOrNull("partial_calories_kcal", 0), n.numberOrNull("partial_protein_g", 1))
        } else null
        OnlineAssistanceRequest(uuid, json.string("question"), OnlineLanguage.valueOf(json.string("language")),
            LocalDate.parse(period.string("start")), LocalDate.parse(period.string("end")), model, weight, nutrition)
            .takeIf { it.valid(today) }
    }.getOrNull()

    private fun JSONObject.keysSet() = keys().asSequence().toSet()
    private fun JSONObject.string(key: String) = (get(key) as? String) ?: error("Expected string")
    private fun JSONObject.integer(key: String): Int {
        val n = get(key)
        require(n is Int || n is Long)
        val value = (n as Number).toLong()
        require(value in Int.MIN_VALUE..Int.MAX_VALUE)
        return value.toInt()
    }
    private fun JSONObject.dateOrNull(key: String): LocalDate? =
        if (get(key) == JSONObject.NULL) null else LocalDate.parse(string(key))
    private fun JSONObject.numberOrNull(key: String, scale: Int): Double? {
        val value = get(key)
        if (value == JSONObject.NULL) return null
        require(value is Number)
        val decimal = java.math.BigDecimal(value.toString()).stripTrailingZeros()
        require(decimal.scale() <= scale)
        return value.toDouble().also { require(it.isFinite()) }
    }
}
