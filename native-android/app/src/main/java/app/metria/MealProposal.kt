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

    fun parse(raw: String): MealProposal? = runCatching {
        val json = JSONObject(unwrapSingleJsonFence(raw))
        require(keys(json) == proposalKeys)
        require(json.getString("kind") == "meal_proposal")
        val items = json.getJSONArray("items").parseItems()
        val assumptions = json.getJSONArray("assumptions").parseStrings()
        val questions = json.getJSONArray("questions").parseStrings()
        val overallConfidence = json.getString("overall_confidence")
        require(overallConfidence in confidences)
        MealProposal(items, assumptions, questions, overallConfidence)
    }.getOrNull()

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
            val confidence = item.getString("confidence")
            require(confidence in confidences)
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
        require(isNotBlank() && length <= maxLength)
        return this
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
