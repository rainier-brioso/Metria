package app.metria

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.ToolCall

/**
 * Approved Gate A: use LiteRT-LM's native tool protocol for bounded read-only chat.
 * Execution remains manual, so these declarations expose schemas to Gemma but never receive
 * vault access. MemberShell validates and executes the selected operation against a snapshot.
 */
class MetriaReadToolSet : ToolSet {
    @Tool(description = "Read a grounded overview of the member's nutrition, goals, meals, and weight trend for today or the last seven days. Use this for broad questions such as how am I doing or how is my week.")
    fun read_progress_overview(
        @ToolParam(description = "Exactly today or week. Week means the rolling last seven days including today.") period: String,
    ): Map<String, Any> = hostExecutionOnly("read_progress_overview", period)

    @Tool(description = "Read the member's calorie and protein summary for today or the last seven days.")
    fun read_nutrition_summary(
        @ToolParam(description = "Exactly today or week.") period: String,
    ): Map<String, Any> = hostExecutionOnly("read_nutrition_summary", period)

    @Tool(description = "Read the member's weight entries in a bounded relative-day range.")
    fun read_weight(
        @ToolParam(description = "Oldest day offset from today, from 0 through 90.") from_days_ago: Int,
        @ToolParam(description = "Newest day offset from today, from 0 through from_days_ago.") to_days_ago: Int,
    ): Map<String, Any> = hostExecutionOnly("read_weight", "$from_days_ago:$to_days_ago")

    @Tool(description = "Read meals saved on one relative day, optionally limited to one meal slot.")
    fun read_meals(
        @ToolParam(description = "Day offset from today, from 0 through 90. Zero is today.") days_ago: Int,
        @ToolParam(description = "Optional breakfast, lunch, dinner, or snack filter.") meal_slot: String? = null,
    ): Map<String, Any> = hostExecutionOnly("read_meals", "$days_ago:${meal_slot.orEmpty()}")

    @Tool(description = "Search the member's private saved dishes by a short food or dish name.")
    fun search_saved_dishes(
        @ToolParam(description = "A short dish or food query, between 2 and 120 characters.") query: String,
    ): Map<String, Any> = hostExecutionOnly("search_saved_dishes", query)

    private fun hostExecutionOnly(name: String, request: String): Map<String, Any> = mapOf(
        "status" to "host_execution_required",
        "tool" to name,
        "request" to request,
    )
}

/** Converts native LiteRT tool calls into Metria's existing host-side action allowlist. */
object MetriaNativeToolCallAdapter {
    fun adapt(call: ToolCall): PrivateCoachToolCall? = adapt(call.name, call.arguments)

    internal fun adapt(toolName: String, arguments: Map<String, *>): PrivateCoachToolCall? {
        val name = toolName.trim().lowercase()
        return when (name) {
            "read_progress_overview" -> {
                val period = arguments.string("period")?.takeIf { it in setOf("today", "week") }
                    ?: return null
                PrivateCoachToolCall(intent = name, period = period)
            }
            "read_nutrition_summary" -> {
                val period = arguments.string("period")?.takeIf { it in setOf("today", "week") }
                    ?: return null
                PrivateCoachToolCall(intent = name, period = period)
            }
            "read_weight" -> {
                val fromDaysAgo = arguments.int("from_days_ago") ?: return null
                val toDaysAgo = arguments.int("to_days_ago") ?: return null
                if (fromDaysAgo !in 0..90 || toDaysAgo !in 0..fromDaysAgo) return null
                PrivateCoachToolCall(
                    intent = name,
                    fromDaysAgo = fromDaysAgo,
                    toDaysAgo = toDaysAgo,
                )
            }
            "read_meals" -> {
                val daysAgo = arguments.int("days_ago")?.takeIf { it in 0..90 } ?: return null
                val mealSlot = arguments.string("meal_slot")
                if (mealSlot != null && mealSlot !in MealSlots.allowed) return null
                PrivateCoachToolCall(intent = name, daysAgo = daysAgo, mealSlot = mealSlot)
            }
            "search_saved_dishes" -> {
                val query = arguments.string("query")?.takeIf { it.length in 2..120 } ?: return null
                PrivateCoachToolCall(intent = name, query = query)
            }
            else -> null
        }
    }

    private fun Map<String, *>.string(key: String): String? = this[key]
        ?.toString()
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private fun Map<String, *>.int(key: String): Int? = when (val value = this[key]) {
        is Number -> value.toInt().takeIf { value.toDouble() == it.toDouble() }
        is String -> value.toIntOrNull()
        else -> null
    }
}

data class BoundedAgentRecords<T>(
    val records: List<T>,
    val totalCount: Int,
    val omittedCount: Int,
) {
    val coverage: String = if (omittedCount == 0) "full" else "latest_records_only"
}

/** Keeps local record payloads below the 4,096-token runtime budget. */
object MetriaAgentResultBounds {
    const val maxRecords = 24

    fun <T> latest(records: List<T>, limit: Int = maxRecords): BoundedAgentRecords<T> {
        require(limit > 0)
        val returned = records.takeLast(limit)
        return BoundedAgentRecords(
            records = returned,
            totalCount = records.size,
            omittedCount = (records.size - returned.size).coerceAtLeast(0),
        )
    }
}

object MetriaAgentReplyPolicy {
    private const val maxDisplayCharacters = 1_200
    private const val hardMaximumCharacters = 4_000

    fun acceptedText(message: com.google.ai.edge.litertlm.Message): String? = message.contents.contents
        .filterIsInstance<com.google.ai.edge.litertlm.Content.Text>()
        .joinToString("") { it.text }
        .let(::acceptedText)

    internal fun acceptedText(rawText: String): String? {
        val sanitized = rawText
            .replace(Regex("[\\p{Cc}&&[^\\n\\t]]"), " ")
            .trim()
            .takeIf { it.length in 2..hardMaximumCharacters }
            ?: return null
        if (
            listOf("system_instruction", "<start_of_turn>", "<end_of_turn>", "tool_response")
                .any { marker -> marker in sanitized.lowercase() }
        ) return null
        if (sanitized.length <= maxDisplayCharacters) return sanitized

        val candidate = sanitized.take(maxDisplayCharacters - 1).trimEnd()
        val boundary = candidate.indexOfLast { it in setOf('.', '!', '?', '\n') }
        return if (boundary >= maxDisplayCharacters / 2) {
            candidate.take(boundary + 1).trimEnd()
        } else {
            "$candidate…"
        }
    }
}

data class MetriaAgentFactAudit(
    val acceptedText: String?,
    val rejection: String? = null,
) {
    val retryable: Boolean = rejection == "unsupported_personal_fact"
}

/**
 * Immutable evidence for exactly one host-executed tool response.
 *
 * Design decision: the model may phrase grounded facts naturally, but every Arabic number and
 * weight direction in the visible answer must be recoverable from this latest bounded response.
 * Conversation history is deliberately never added to this allowlist.
 */
class MetriaAgentFactEvidence private constructor(
    val toolName: String,
    internal val allowedNumbers: Set<String>,
    internal val weightDirection: Int?,
    internal val containsVaultFacts: Boolean,
) {
    companion object {
        fun from(toolName: String, response: Map<String, Any?>): MetriaAgentFactEvidence {
            val numbers = linkedSetOf<String>()
            collectNumbers(response, numbers)
            return MetriaAgentFactEvidence(
                toolName = toolName,
                allowedNumbers = numbers,
                weightDirection = explicitWeightChange(response)?.compareTo(0.0)
                    ?: weightEntryChange(response)?.compareTo(0.0),
                containsVaultFacts = response["source"] == "metria_local_vault",
            )
        }

        private fun collectNumbers(value: Any?, destination: MutableSet<String>) {
            when (value) {
                is Map<*, *> -> value.values.forEach { collectNumbers(it, destination) }
                is Iterable<*> -> value.forEach { collectNumbers(it, destination) }
                is Array<*> -> value.forEach { collectNumbers(it, destination) }
                is Number -> numericCandidates(value.toString()).forEach(destination::add)
                is String -> numberToken.findAll(value).forEach { match ->
                    numericCandidates(match.value).forEach(destination::add)
                }
            }
        }

        private fun explicitWeightChange(response: Map<String, Any?>): Double? =
            (response["weight"] as? Map<*, *>)?.get("change_kg").asDouble()

        private fun weightEntryChange(response: Map<String, Any?>): Double? {
            val entries = response["entries"] as? List<*> ?: return null
            val first = (entries.firstOrNull() as? Map<*, *>)?.get("kilograms").asDouble() ?: return null
            val latest = (entries.lastOrNull() as? Map<*, *>)?.get("kilograms").asDouble() ?: return null
            return if (entries.size >= 2) latest - first else null
        }

        private fun Any?.asDouble(): Double? = when (this) {
            is Number -> toDouble()
            is String -> replace(',', '.').toDoubleOrNull()
            else -> null
        }
    }
}

/** Host-side final-output gate for unsupported personal record claims. */
object MetriaAgentFactAuditor {
    private val personalRecordClaim = Regex(
        """\b(?:comiste|has\s+comido|consumiste|tomaste|pesaste|pesas|has\s+registrado|""" +
            """llevas\s+registrad[oa]s?|registraste|guardad[oa]s?|you\s+ate|you\s+weigh|""" +
            """you\s+weighed|you\s+(?:have\s+)?logged|you\s+(?:have\s+)?saved)\b|""" +
            """\b(?:tu|tus|your)\b.{0,40}\b(?:peso|weight|comida|comidas|meal|meals|""" +
            """calor(?:ia|ias|ie|ies)|prote[ií]na|protein|objetivo|objetivos|goal|goals|""" +
            """registro|registros|record|records)\b|""" +
            """\b(?:hay|there\s+(?:is|are))\b.{0,30}\b(?:comidas?\s+registradas?|""" +
            """registros?\s+de\s+peso|recorded\s+meals?|weight\s+entries)\b""",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val weightContext = Regex("""\b(?:peso|weight|kg|kilogramos?|kilograms?)\b""", RegexOption.IGNORE_CASE)
    private val weightUp = Regex(
        """\b(?:subi[oó]|ha\s+subido|aument[oó]|ganaste|increased|gained|went\s+up)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val weightDown = Regex(
        """\b(?:baj[oó]|ha\s+bajado|disminuy[oó]|perdiste|decreased|lost|went\s+down)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun audit(message: com.google.ai.edge.litertlm.Message, evidence: MetriaAgentFactEvidence?): MetriaAgentFactAudit {
        val accepted = MetriaAgentReplyPolicy.acceptedText(message)
            ?: return MetriaAgentFactAudit(null, "reply_policy")
        return auditAccepted(accepted, evidence)
    }

    internal fun audit(rawText: String, evidence: MetriaAgentFactEvidence?): MetriaAgentFactAudit {
        val accepted = MetriaAgentReplyPolicy.acceptedText(rawText)
            ?: return MetriaAgentFactAudit(null, "reply_policy")
        return auditAccepted(accepted, evidence)
    }

    fun retryInstruction(hasLatestToolEvidence: Boolean): String = if (hasLatestToolEvidence) {
        "Grounding correction: rewrite the final member-facing answer using only personal values, " +
            "dates, counts, names, and trends explicitly present in the latest tool response. " +
            "Do not calculate or add facts. Return only the answer, with no tool or policy commentary."
    } else {
        "Grounding correction: the previous draft made an unsupported personal-record claim. " +
            "Call exactly one approved read tool if the member needs their records; otherwise rewrite " +
            "without claiming any personal meals, weights, goals, dates, nutrition values, or trends."
    }

    private fun auditAccepted(text: String, evidence: MetriaAgentFactEvidence?): MetriaAgentFactAudit {
        val normalized = normalizeWords(text)
        if (evidence == null || !evidence.containsVaultFacts) {
            return if (personalRecordClaim.containsMatchIn(normalized)) {
                MetriaAgentFactAudit(null, "unsupported_personal_fact")
            } else {
                MetriaAgentFactAudit(text)
            }
        }

        val withoutListOrdinals = text.replace(Regex("(?m)^\\s*\\d{1,2}[.)]\\s+"), "")
        val unsupportedNumber = numberToken.findAll(withoutListOrdinals)
            .map(MatchResult::value)
            .firstOrNull { token -> numericCandidates(token).none(evidence.allowedNumbers::contains) }
        if (unsupportedNumber != null) return MetriaAgentFactAudit(null, "unsupported_personal_fact")

        if (weightContext.containsMatchIn(normalized)) {
            val contradictsTrend = when (evidence.weightDirection) {
                -1 -> weightUp.containsMatchIn(normalized)
                1 -> weightDown.containsMatchIn(normalized)
                0 -> weightUp.containsMatchIn(normalized) || weightDown.containsMatchIn(normalized)
                else -> false
            }
            if (contradictsTrend) return MetriaAgentFactAudit(null, "unsupported_personal_fact")
        }
        return MetriaAgentFactAudit(text)
    }
}

private val numberToken = Regex("""(?<![\p{L}\p{N}])\d+(?:[.,]\d+)?(?![\p{L}\p{N}])""")

private fun numericCandidates(raw: String): Set<String> {
    val compact = raw.trim()
    val candidates = linkedSetOf<String>()
    fun add(value: String) {
        runCatching { java.math.BigDecimal(value).stripTrailingZeros().toPlainString() }
            .getOrNull()
            ?.let(candidates::add)
    }
    add(compact.replace(',', '.'))
    val separator = compact.singleOrNull { it == ',' || it == '.' }
    if (separator != null && compact.substringAfter(separator).length == 3) {
        add(compact.replace(separator.toString(), ""))
    }
    return candidates
}

private fun normalizeWords(value: String): String = java.text.Normalizer
    .normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")

data class RequiredAgentTool(
    val name: String,
    val period: String? = null,
    val query: String? = null,
) {
    val retryInstruction: String = when (name) {
        "read_progress_overview" ->
            "Grounding correction: this question asks about the member's personal progress. " +
                "Call $name with period=$period now. Do not answer with prose or a capability list before the tool result."
        "search_saved_dishes" ->
            "Grounding correction: this question asks about a private saved dish. " +
                "Call $name with query=${query.orEmpty()} now. Do not discuss AI memory or answer with prose before the tool result."
        else -> "Grounding correction: call $name now before answering."
    }
}

/**
 * Fail-safe for unambiguous personal-record questions and their short follow-ups. The model still
 * selects tools normally; this permits one corrective native turn when it returns unsupported
 * generic prose instead.
 */
object MetriaAgentGroundingPolicy {
    private val broadProgressPhrases = listOf(
        "how am i doing",
        "how is my week",
        "how's my week",
        "my progress",
        "my goals this week",
        "como voy",
        "como va mi semana",
        "mi progreso",
        "mis objetivos esta semana",
        "mis metas esta semana",
    )
    private val savedDishQuestion = Regex(
        """(?iu)^\s*[¿?]?\s*(?:recuerdas|recuerda|te\s+acuerdas\s+de|tienes\s+guardad[oa]|has\s+guardado|do\s+you\s+remember|remember)\s+(?:mi|mis|el|la|my|the)?\s*(.+?)\s*[?!.]*\s*$""",
    )

    fun requiredTool(
        memberText: String,
        history: List<PrivateConversationMessage> = emptyList(),
    ): RequiredAgentTool? {
        savedDishQuestion.matchEntire(memberText)?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.length in 2..120 }
            ?.let { query -> return RequiredAgentTool("search_saved_dishes", query = query) }
        val normalized = java.text.Normalizer.normalize(memberText.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        if (
            listOf("que lleva", "que contiene", "sus ingredientes", "what is in it", "what does it contain", "its ingredients")
                .any { phrase -> phrase in normalized }
        ) {
            history.asReversed()
                .firstOrNull(PrivateConversationMessage::fromMember)
                ?.text
                ?.let(savedDishQuestion::matchEntire)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.length in 2..120 }
                ?.let { query -> return RequiredAgentTool("search_saved_dishes", query = query) }
        }
        if (broadProgressPhrases.none { it in normalized }) return null
        val period = if ("today" in normalized || "hoy" in normalized) "today" else "week"
        return RequiredAgentTool("read_progress_overview", period)
    }
}

/** Transitional boundary: explicit writes keep their already-tested route until Gate D. */
object MetriaAgentMigrationPolicy {
    private val writeWords = setOf(
        "record", "log", "save", "add",
        "registra", "registrar", "guarda", "guardar", "añade", "anade", "agrega", "apunta",
    )

    fun useLegacyWriteRoute(memberText: String): Boolean {
        val words = memberText.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}.,]+"), " ")
            .split(' ')
            .filter(String::isNotBlank)
            .toSet()
        return words.any(writeWords::contains)
    }
}
