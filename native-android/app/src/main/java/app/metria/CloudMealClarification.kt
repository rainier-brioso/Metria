package app.metria

import org.json.JSONObject

/** A question is never a reviewable meal proposal, including on the photo path. */
internal object CloudMealClarification {
    fun question(raw: String): String? = runCatching {
        require(raw.length <= 16_000)
        val root = JSONObject(raw)
        require(root.keys().asSequence().toSet() == setOf("kind", "items", "assumptions", "questions", "overall_confidence"))
        require(root.getString("kind") == "meal_proposal")
        require(root.getJSONArray("items").length() == 0)
        require(root.getString("overall_confidence") in setOf("low", "medium", "high"))
        val assumptions = root.getJSONArray("assumptions")
        require(assumptions.length() <= 1)
        if (assumptions.length() == 1) require(assumptions.get(0) is String && assumptions.getString(0).length <= 240)
        val questions = root.getJSONArray("questions")
        require(questions.length() == 1 && questions.get(0) is String)
        questions.getString(0).trim().also {
            require(it.length in 2..240 && it.none { char -> char.isISOControl() })
            require(!it.contains("http", ignoreCase = true))
        }
    }.getOrNull()
}
