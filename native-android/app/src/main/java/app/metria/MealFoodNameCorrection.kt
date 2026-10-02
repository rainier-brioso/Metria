package app.metria

import org.json.JSONObject

data class MealFoodNameCorrectionStatus(
    val requestId: Long? = null,
    val state: String = "idle",
    val replacementName: String? = null,
    val diagnostic: String? = null,
)

object MealFoodNameCorrectionPresentation {
    fun suggestionFor(
        status: MealFoodNameCorrectionStatus,
        expectedRequestId: Long?,
        expectedItemId: String?,
        resultItemId: String?,
    ): String? = status.replacementName?.takeIf {
        expectedRequestId != null && status.requestId == expectedRequestId &&
            status.state == "ready" && expectedItemId != null && expectedItemId == resultItemId
    }
}

/** Accepts only a short replacement food label from the local correction helper. */
object MealFoodNameCorrectionParser {
    private val expectedKeys = setOf("replacement_name")

    fun parse(raw: String, currentName: String): String? = runCatching {
        val json = JSONObject(raw.trim())
        require(json.keys().asSequence().toSet() == expectedKeys)
        val replacement = json.getString("replacement_name").trim()
        require(replacement.length in 2..80)
        require(replacement.split(Regex("\\s+")).size in 1..6)
        require(replacement.none(Char::isDigit))
        require(replacement.none { it.isISOControl() || it == '{' || it == '}' || it == '<' || it == '>' })
        require(replacement != currentName.trim() && !replacement.equals(currentName.trim(), ignoreCase = true))
        replacement
    }.getOrNull()
}
