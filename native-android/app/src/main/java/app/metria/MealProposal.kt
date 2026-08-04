package app.metria

import org.json.JSONArray
import org.json.JSONObject

data class GramRange(val min: Int, val max: Int)

data class MealProposalItem(
    val name: String,
    val visiblePortion: String,
    val estimatedGramsRange: GramRange,
    val preparation: String,
    val confidence: String,
)

data class MealProposal(
    val items: List<MealProposalItem>,
    val assumptions: List<String>,
    val questions: List<String>,
    val overallConfidence: String,
)

/** Rejects any model shape outside the explicit M0 display-only proposal contract. */
object MealProposalParser {
    private val confidences = setOf("low", "medium", "high")
    private val proposalKeys = setOf("kind", "items", "assumptions", "questions", "overall_confidence")
    private val itemKeys = setOf(
        "name", "visible_portion", "estimated_grams_range", "preparation", "confidence"
    )
    private val rangeKeys = setOf("min", "max")

    fun parse(raw: String): MealProposal? = runCatching { parseOrThrow(raw) }.getOrNull()

    internal fun parseOrThrow(raw: String): MealProposal {
        val json = normalizeProposal(JSONObject(unwrapSingleJsonFence(raw)))
        require(keys(json) == proposalKeys)
        require(json.getString("kind") == "meal_proposal")
        val items = json.getJSONArray("items").parseItems()
        val assumptions = json.getJSONArray("assumptions").parseStrings()
        val questions = json.getJSONArray("questions").parseStrings()
        val overallConfidence = normalizedConfidence(json.getString("overall_confidence"))
        return MealProposal(items, assumptions, questions, overallConfidence)
    }

    /**
     * Product decision: normalize only explicit, lossless model variations before
     * validation. This is not best-effort parsing: unknown keys, inferred fields,
     * and ambiguous values still fail closed and never become a meal draft.
     */
    private fun normalizeProposal(source: JSONObject): JSONObject {
        val root = canonicalObject(source, mapOf("overallConfidence" to "overall_confidence"))
        val items = root.get("items").asArray("items").let { sourceItems -> JSONArray().apply {
            for (index in 0 until sourceItems.length()) put(normalizeItem(sourceItems.getJSONObject(index)))
        } }
        root.put("items", items)
        root.put("assumptions", root.get("assumptions").asStringArray("assumptions"))
        root.put("questions", root.get("questions").asStringArray("questions"))
        root.put("overall_confidence", normalizedConfidence(root.getString("overall_confidence")))
        return root
    }

    private fun normalizeItem(source: JSONObject): JSONObject {
        val item = canonicalObject(source, mapOf(
            "visiblePortion" to "visible_portion",
            "estimatedGramsRange" to "estimated_grams_range",
        ))
        val range = item.get("estimated_grams_range").asRangeObject()
        range.put("min", wholeNumber(range.get("min")))
        range.put("max", wholeNumber(range.get("max")))
        item.put("estimated_grams_range", range)
        item.put("confidence", normalizedConfidence(item.getString("confidence")))
        return item
    }

    private fun canonicalObject(source: JSONObject, aliases: Map<String, String>): JSONObject {
        val result = JSONObject()
        val iterator = source.keys()
        while (iterator.hasNext()) {
            val sourceKey = iterator.next()
            val key = aliases[sourceKey] ?: sourceKey
            require(!result.has(key)) { "Duplicate canonical key." }
            result.put(key, source.get(sourceKey))
        }
        return result
    }

    private fun Any.asArray(field: String): JSONArray = when (this) {
        is JSONArray -> this
        is JSONObject -> JSONArray().put(this)
        else -> throw IllegalArgumentException("$field must be an array or object.")
    }

    private fun Any.asStringArray(field: String): JSONArray = when (this) {
        is JSONArray -> this
        is String -> JSONArray().put(this)
        else -> throw IllegalArgumentException("$field must be a string or array.")
    }

    private fun Any.asRangeObject(): JSONObject = when (this) {
        is JSONObject -> canonicalObject(this, emptyMap())
        is JSONArray -> {
            require(length() == 2) { "Range arrays must contain exactly min and max." }
            JSONObject().put("min", get(0)).put("max", get(1))
        }
        else -> throw IllegalArgumentException("Range must be an object or two-value array.")
    }

    private fun wholeNumber(value: Any): Int = when (value) {
        is Number -> value.toInt().also { require(value.toDouble() == it.toDouble()) }
        is String -> value.trim().toInt()
        else -> throw IllegalArgumentException("Range value must be a whole number.")
    }

    private fun normalizedConfidence(value: String): String = value.trim().lowercase().also {
        require(it in confidences)
    }

    /** A content-free diagnostic for development builds; never exposes model text. */
    fun describeFormat(raw: String): String {
        val normalized = unwrapSingleJsonFence(raw)
        if (!normalized.startsWith("{")) {
            return "response was not a JSON object (length ${raw.length})"
        }
        return runCatching {
            val keys = keys(JSONObject(normalized)).sorted().joinToString(", ")
            "JSON object keys: $keys"
        }.getOrElse { "response started like JSON but could not be parsed (length ${raw.length})" }
    }

    private fun JSONArray.parseItems(): List<MealProposalItem> = buildList {
        require(length() <= 12)
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            require(keys(item) == itemKeys)
            val name = item.getString("name").boundedText(120)
            val visiblePortion = item.getString("visible_portion").boundedText(120)
            val preparation = item.getString("preparation").boundedText(120)
            val confidence = normalizedConfidence(item.getString("confidence"))
            val range = item.getJSONObject("estimated_grams_range")
            require(keys(range) == rangeKeys)
            val min = range.getInt("min")
            val max = range.getInt("max")
            require(min in 1..2_000 && max in min..2_000)
            add(MealProposalItem(name, visiblePortion, GramRange(min, max), preparation, confidence))
        }
    }

    private fun JSONArray.parseStrings(): List<String> = buildList {
        require(length() <= 12)
        for (index in 0 until length()) add(getString(index).boundedText(280))
    }

    private fun keys(json: JSONObject): Set<String> = buildSet {
        val iterator = json.keys()
        while (iterator.hasNext()) add(iterator.next())
    }

    private fun String.boundedText(maxLength: Int): String {
        val trimmed = trim()
        require(trimmed.isNotBlank() && trimmed.length <= maxLength)
        return trimmed
    }

    /**
     * Models commonly wrap an otherwise valid response in one Markdown JSON
     * fence. This normalizes only that exact transport wrapper; it never
     * accepts arbitrary prose around a JSON object.
     */
    private fun unwrapSingleJsonFence(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("^```(?:json)?\\s*(\\{[\\s\\S]*\\})\\s*```$").matchEntire(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }
}
