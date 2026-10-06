package app.metria.cloud

/** Model selects sections; personal facts and all member-facing copy remain host-owned. */
enum class OnlineSummarySection { weight, nutrition }
enum class OnlineGuidance { recorded_context_only, balanced_meal_information, professional_support }
class OnlineResponseSelection internal constructor(
    val sections: List<OnlineSummarySection>, val guidance: OnlineGuidance,
)

object OnlineResponseContract {
    const val INSTRUCTION = """Return only JSON with exactly schema_version (integer 1), request_id (the submitted ID), language (the submitted language), sections (a unique array of weight and/or nutrition, only if supplied), and guidance (recorded_context_only, balanced_meal_information, or professional_support). No prose, values, actions or additional fields. Select relevant supplied summaries. Use professional_support for medical or urgent health questions. This selection cannot authorize a save or fetch more records."""

    fun validate(body: String, submitted: OnlineAssistanceRequest): OnlineResponseSelection? = runCatching {
        require(body.length <= 2_048)
        val json = StrictOnlineJson.objectOrNull(body) ?: return null
        require(json.keys().asSequence().toSet() == setOf("schema_version", "request_id", "language", "sections", "guidance"))
        require(json.get("schema_version") is Int && json.getInt("schema_version") == 1)
        require(json.get("request_id") == submitted.requestId.toString())
        require(json.get("language") == submitted.language.name)
        val array = json.getJSONArray("sections")
        require(array.length() <= 2)
        val sections = (0 until array.length()).map { index ->
            val section = OnlineSummarySection.valueOf(array.get(index) as String)
            require(when (section) { OnlineSummarySection.weight -> submitted.weight != null; OnlineSummarySection.nutrition -> submitted.nutrition != null })
            section
        }
        require(sections.distinct().size == sections.size)
        val guidance = OnlineGuidance.valueOf(json.get("guidance") as String)
        OnlineResponseSelection(sections.toList(), guidance)
    }.getOrNull()
}
