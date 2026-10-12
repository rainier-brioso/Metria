package app.metria

/** Build-variant boundary for the optional MiniLM fast route. */
internal interface MiniLmMealRouter : AutoCloseable {
    fun routeMealRead(text: String, toolName: String): MiniLmRouteDecision
}

internal enum class MiniLmRouteOutcome {
    ACCEPTED,
    SCOPE_REJECTED,
    SIMILARITY_REJECTED,
    MARGIN_REJECTED,
    RUNTIME_UNAVAILABLE,
    INFERENCE_FAILED,
    INTERRUPTED,
}

/** Content-free route diagnostics. Scores are present only after model inference. */
internal data class MiniLmRouteDecision(
    val outcome: MiniLmRouteOutcome,
    val positiveScore: Float? = null,
    val competingScore: Float? = null,
    val fallbackScore: Float? = null,
) {
    fun diagnostic(toolName: String): String {
        val route = when (toolName) {
            "read_latest_meal" -> "latest-meal"
            "read_meals" -> "meal-list"
            "read_day_nutrition" -> "today-nutrition"
            else -> "meal"
        }
        val scoreText = listOfNotNull(
            positiveScore?.takeIf(Float::isFinite)?.let { "positive=${format(it)}" },
            competingScore?.takeIf(Float::isFinite)?.let { "competing=${format(it)}" },
            fallbackScore?.takeIf(Float::isFinite)?.let { "fallback=${format(it)}" },
        ).joinToString(", ").takeIf(String::isNotEmpty)
        val outcomeText = when (outcome) {
            MiniLmRouteOutcome.ACCEPTED -> "accepted"
            MiniLmRouteOutcome.SCOPE_REJECTED -> "scope rejected"
            MiniLmRouteOutcome.SIMILARITY_REJECTED -> "similarity below threshold"
            MiniLmRouteOutcome.MARGIN_REJECTED -> "margin below threshold"
            MiniLmRouteOutcome.RUNTIME_UNAVAILABLE -> "runtime unavailable"
            MiniLmRouteOutcome.INFERENCE_FAILED -> "inference failed"
            MiniLmRouteOutcome.INTERRUPTED -> "interrupted"
        }
        return buildString {
            append("MiniLM route: ").append(route).append(' ').append(outcomeText)
            if (scoreText != null) append(" (").append(scoreText).append(')')
        }
    }

    private fun format(value: Float) = "%.3f".format(java.util.Locale.ROOT, value)
}

/** Pure threshold decision so classifier outcomes remain explainable and testable. */
internal object MiniLmRouteScoring {
    const val MIN_POSITIVE_SIMILARITY = 0.58f
    const val MINIMUM_MARGIN = 0.06f

    fun decide(positive: Float, competing: Float, fallback: Float): MiniLmRouteDecision {
        if (!positive.isFinite() || !competing.isFinite() || !fallback.isFinite()) {
            return MiniLmRouteDecision(MiniLmRouteOutcome.INFERENCE_FAILED)
        }
        val bestCompetitor = maxOf(competing, fallback)
        val outcome = when {
            positive < MIN_POSITIVE_SIMILARITY -> MiniLmRouteOutcome.SIMILARITY_REJECTED
            positive - bestCompetitor < MINIMUM_MARGIN -> MiniLmRouteOutcome.MARGIN_REJECTED
            else -> MiniLmRouteOutcome.ACCEPTED
        }
        return MiniLmRouteDecision(outcome, positive, competing, fallback)
    }
}
