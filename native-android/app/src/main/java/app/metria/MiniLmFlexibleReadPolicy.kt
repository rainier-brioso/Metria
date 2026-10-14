package app.metria

/**
 * Small compositional English grammar for selected local reads. Every alternative is
 * anchored, and a date is extracted only from the beginning or end of a day request.
 */
internal object MiniLmFlexibleReadPolicy {
    enum class Kind {
        MEALS, DAY_CALORIES, DAY_PROTEIN, YESTERDAY_CALORIES, YESTERDAY_PROTEIN,
        LATEST_MEAL_CALORIES, LATEST_MEAL_PROTEIN, LATEST_WEIGHT, LAST_WEIGHED, RECENT_WEIGH_INS,
    }

    data class Candidate(val kind: Kind, val canonicalText: String)

    private const val HEAD = "(?:show(?: me)?|list(?: me)?|display(?: me)?|view(?: me)?|let me see|can i see|could i see|tell me)"
    private const val SUBJECT_MEALS = "(?:(?:my|the) )?(?:(?:recorded|logged|saved) )?(?:meals|foods)"
    private const val SUBJECT_LOG = "(?:my|the) (?:food log|meal diary)"
    private const val CAL = "(?:calories|calorie)"
    private const val WEIGHT_TEMPORAL = "(?:last|latest|most recent)"

    private val mealListBodies = listOf(
        Regex("^$HEAD $SUBJECT_MEALS$"),
        Regex("^$HEAD $SUBJECT_LOG$"),
        Regex("^what is in $SUBJECT_LOG$"),
        Regex("^what (?:are|were) $SUBJECT_MEALS$"),
        Regex("^which $SUBJECT_MEALS$"),
        Regex("^what did i (?:eat|have)$"),
        Regex("^what have i (?:eaten|had)$"),
        Regex("^remind me what i (?:ate|had)$"),
        Regex("^what did i (?:log|record|save) for meals$"),
        Regex("^which meals (?:have i (?:logged|recorded|saved)|are in my food log)$"),
    )
    private val nutritionBodies = listOf(
        Regex("^(?:what is|what are) (?:my )?(?:(?:recorded|logged|saved) )?(?:$CAL|protein)(?: total)?$"),
        Regex("^(?:tell|show|give) me (?:my )?(?:(?:recorded|logged|saved) )?(?:$CAL|protein)(?: total| amount)?$"),
        Regex("^(?:how many $CAL did i (?:log|record|save|eat|have)|how many $CAL have i (?:logged|recorded|saved|had))$"),
        Regex("^(?:how much protein did i (?:log|record|save|eat|have)|how much protein have i (?:logged|recorded|saved|had))$"),
        Regex("^(?:tell|show|give) me (?:how many $CAL|how much protein) (?:did i (?:log|record|save|eat|have)|have i (?:logged|recorded|saved|had))$"),
    )
    private val latestMealBodies = listOf(
        Regex("^(?:how many $CAL|how much protein) (?:(?:were|was|are|is) )?(?:in|from|of) my $WEIGHT_TEMPORAL meal$"),
        Regex("^(?:what is|what was) (?:the )?(?:$CAL|protein)(?: content)? (?:in|of|from) my $WEIGHT_TEMPORAL meal$"),
        Regex("^(?:tell|show|give) me (?:the )?(?:$CAL|protein)(?: content)? (?:in|of|from) my $WEIGHT_TEMPORAL meal$"),
        Regex("^(?:tell|show|give) me (?:how many $CAL|how much protein) (?:were|was|are|is) (?:in|from|of) my $WEIGHT_TEMPORAL meal$"),
        Regex("^(?:how many $CAL|how much protein) did my $WEIGHT_TEMPORAL meal have$"),
    )
    private val weightBodies = listOf(
        Regex("^(?:what is|show me|tell me|can i see|could i see|let me see) (?:my|the) $WEIGHT_TEMPORAL weight$") to Kind.LATEST_WEIGHT,
        Regex("^what is (?:my|the) $WEIGHT_TEMPORAL weight i recorded$") to Kind.LATEST_WEIGHT,
        Regex("^when did i last weigh myself$") to Kind.LAST_WEIGHED,
        Regex("^(?:when was|on what date was) (?:my|the) (?:last|most recent) weighin$") to Kind.LAST_WEIGHED,
        Regex("^(?:show(?: me)?|list(?: me)?|let me see|can i see|could i see) (?:my|the) recent weighins$") to Kind.RECENT_WEIGH_INS,
    )

    fun parse(text: String): Candidate? {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH || text.any { it.code < 0x20 && !it.isWhitespace() }) return null
        var normalized = MiniLmReadTextNormalization.normalize(text)
            .replace("what's", "what is")
            .replace("today's", "today")
            .replace("yesterday's", "yesterday")
            .replace(Regex("weigh[- ](ins?)"), "weigh$1")
        if (normalized.any(Char::isDigit)) return null
        normalized = normalized.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return null

        val date = extractDate(normalized) ?: return null
        val body = date.second.trim()
        if (body.any { !(it.isLetter() || it.isWhitespace()) }) return null
        val explicitDay = date.first

        // Day-scoped requests need one explicit supported date; latest meal reads reject dates.
        if (explicitDay != null && mealListBodies.any { it.matches(body) }) {
            return Candidate(Kind.MEALS, canonicalMeal(explicitDay))
        }
        if (explicitDay != null && nutritionBodies.any { it.matches(body) }) {
            val metric = metricFrom(body) ?: return null
            val kind = when {
                explicitDay == "today" && metric == "calories" -> Kind.DAY_CALORIES
                explicitDay == "today" -> Kind.DAY_PROTEIN
                metric == "calories" -> Kind.YESTERDAY_CALORIES
                else -> Kind.YESTERDAY_PROTEIN
            }
            return Candidate(kind, canonical(kind))
        }
        if (explicitDay == null && latestMealBodies.any { it.matches(body) }) {
            val kind = if (metricFrom(body) == "protein") Kind.LATEST_MEAL_PROTEIN else Kind.LATEST_MEAL_CALORIES
            return Candidate(kind, canonical(kind))
        }
        if (explicitDay == null) {
            val kind = weightBodies.singleOrNull { (pattern, _) -> pattern.matches(body) }?.second
                ?: return null
            return Candidate(kind, canonical(kind))
        }
        return null
    }

    fun classifierKey(kind: Kind): String = when (kind) {
        Kind.MEALS -> "read_meals"
        Kind.DAY_CALORIES, Kind.DAY_PROTEIN, Kind.YESTERDAY_CALORIES, Kind.YESTERDAY_PROTEIN -> "read_day_nutrition"
        Kind.LATEST_MEAL_CALORIES, Kind.LATEST_MEAL_PROTEIN -> "read_latest_meal"
        Kind.LATEST_WEIGHT, Kind.LAST_WEIGHED, Kind.RECENT_WEIGH_INS -> "read_weight"
    }

    private fun extractDate(text: String): Pair<String?, String>? {
        val prefix = Regex("^(?:for )?(today|yesterday)(,)? (.+)$").matchEntire(text)
        val suffix = Regex("(.+?)(,)? (?:for |from )?(today|yesterday)$").matchEntire(text)
        if (prefix != null && suffix != null) return null
        if (prefix != null) return prefix.groupValues[1] to prefix.groupValues[3]
        if (suffix != null) return suffix.groupValues[3] to suffix.groupValues[1]
        if (Regex("\\b(?:today|yesterday)\\b").containsMatchIn(text)) return null
        return null to text
    }

    private fun metricFrom(body: String): String? {
        val calories = Regex("\\bcalorie(?:s)?\\b").containsMatchIn(body)
        val protein = Regex("\\bprotein\\b").containsMatchIn(body)
        return when {
            calories && !protein -> "calories"
            protein && !calories -> "protein"
            else -> null
        }
    }

    private fun canonicalMeal(day: String) = "Show the meals I recorded $day"
    private fun canonical(kind: Kind): String = when (kind) {
        Kind.MEALS -> "Show the meals I recorded today"
        Kind.DAY_CALORIES -> "How many calories did I log today"
        Kind.DAY_PROTEIN -> "How much protein have I logged today"
        Kind.YESTERDAY_CALORIES -> "How many calories did I log yesterday"
        Kind.YESTERDAY_PROTEIN -> "How much protein have I logged yesterday"
        Kind.LATEST_MEAL_CALORIES -> "How many calories were in my latest meal"
        Kind.LATEST_MEAL_PROTEIN -> "How much protein was in my latest meal"
        Kind.LATEST_WEIGHT -> "What's my latest weight"
        Kind.LAST_WEIGHED -> "When was my last weigh-in"
        Kind.RECENT_WEIGH_INS -> "Show my recent weigh-ins"
    }

    private const val MAX_TEXT_LENGTH = 500
}
