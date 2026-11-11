package app.metria.cloud

import app.metria.MetriaNativeToolCallAdapter
import app.metria.PrivateCoachToolCall
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Minimal, versioned chat turn for the authenticated Metria broker. The broker
 * owns its system prompt and tool descriptions; the app may send only a bounded
 * visible conversation and one host-selected task. No vault data is attached
 * here. Record reads remain app-side host tools.
 */
object CloudAssistantRequest {
    const val VERSION = 1
    const val MAX_MESSAGES = 10
    const val MAX_MESSAGE_CHARS = 600
    const val MAX_BODY_BYTES = 16 * 1024

    enum class Task(val wireName: String) {
        CHAT("chat"), MEAL_DRAFT("meal_draft"), PORTION_EDIT("portion_edit"), FOOD_REPLACEMENT("food_replacement");

        companion object {
            fun fromWire(value: String): Task? = entries.firstOrNull { it.wireName == value }
        }
    }

    data class Turn(val requestId: String, val task: Task, val body: ByteArray)

    data class Result(
        val requestId: String,
        val reply: String?,
        val tool: PrivateCoachToolCall?,
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int,
        val usageKnown: Boolean = true,
    )

    /** Pure task selection keeps host reads and explicit confirmation ahead of meal generation. */
    fun selectTask(
        hasRequiredRead: Boolean,
        hasPortionEdit: Boolean,
        hasFoodReplacement: Boolean,
        hasMealConfirmation: Boolean,
        explicitlyConfirmsMeal: Boolean,
        isEligibleMealReport: Boolean,
    ): Task = when {
        hasRequiredRead -> Task.CHAT
        hasPortionEdit -> Task.PORTION_EDIT
        hasFoodReplacement -> Task.FOOD_REPLACEMENT
        hasMealConfirmation && explicitlyConfirmsMeal -> Task.MEAL_DRAFT
        !hasMealConfirmation && isEligibleMealReport -> Task.MEAL_DRAFT
        else -> Task.CHAT
    }

    /**
     * Drops local-only evidence, keeps the latest bounded member-visible turns,
     * then adds the current member message if it is not already the last turn.
     */
    fun build(task: Task, history: List<Pair<Boolean, String>>, currentText: String, requestId: String = UUID.randomUUID().toString()): Turn? {
        if (currentText.length !in 1..MAX_MESSAGE_CHARS || !safeText(currentText)) return null
        if (runCatching { UUID.fromString(requestId).toString() == requestId }.getOrDefault(false).not()) return null
        // Assistant messages can be the presentation of a local vault read. Keep the
        // cloud context member-authored only so later turns cannot echo those facts.
        val messages = history.asSequence()
            .filter { (fromMember, _) -> fromMember }
            .filter { (_, text) -> text.length in 1..MAX_MESSAGE_CHARS && safeText(text) }
            .map { (_, text) -> Message("user", text) }
            .toList()
            .takeLast(MAX_MESSAGES - 1)
            .toMutableList()
        if (messages.lastOrNull()?.let { it.role == "user" && it.content.trim() == currentText.trim() } != true) {
            messages += Message("user", currentText.trim())
        }
        val jsonMessages = JSONArray().apply {
            messages.forEach { message -> put(JSONObject().put("role", message.role).put("content", message.content)) }
        }
        val body = JSONObject()
            .put("request_id", requestId)
            .put("consent_version", VERSION)
            .put("system_version", VERSION)
            .put("task", task.wireName)
            .put("messages", jsonMessages)
            .toString()
            .toByteArray(Charsets.UTF_8)
            .takeIf { it.size <= MAX_BODY_BYTES }
            ?: return null
        return Turn(requestId, task, body)
    }

    /** Strictly parse the fixed broker envelope. Model prose never supplies host authority. */
    fun parse(raw: String, turn: Turn, expectedProvider: String = "nebius",
        expectedModel: String = "nvidia/Nemotron-3.5-Super-VL"): Result? = runCatching {
        if (raw.toByteArray(Charsets.UTF_8).size > 65_536) return null
        val json = JSONObject(raw)
        if (json.length() != 6 || json.getString("request_id") != turn.requestId ||
            json.getString("model") != expectedModel || json.getString("provider") != expectedProvider) return null
        val usage = json.optJSONObject("usage")
        if (usage == null && !(expectedProvider == "compatible" && json.has("usage") && json.isNull("usage"))) return null
        if (usage != null && usage.length() != 3) return null
        val promptTokens = usage?.getInt("prompt_tokens") ?: 0
        val completionTokens = usage?.getInt("completion_tokens") ?: 0
        val totalTokens = usage?.getInt("total_tokens") ?: 0
        if (promptTokens !in 0..100_000 || completionTokens !in 0..100_000 || totalTokens != promptTokens + completionTokens) return null
        val reply = json.optString("reply").takeIf { !json.isNull("reply") }
        val toolObject = json.optJSONObject("tool")
        val tool = if (json.isNull("tool")) null else {
            if (toolObject == null || toolObject.length() != 2 || toolObject.has("id")) return null
            val name = toolObject.getString("name")
            val argsObject = toolObject.getJSONObject("arguments")
            val args = buildMap<String, Any?> {
                val keys = argsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, argsObject.get(key))
                }
            }
            MetriaNativeToolCallAdapter.adapt(name, args) ?: return null
        }
        if ((reply == null) == (tool == null)) return null
        val replyLimit = if (turn.task == Task.MEAL_DRAFT) MealProposalParserLimit else 4_000
        if (reply != null && (reply.length !in 2..replyLimit || !safeText(reply))) return null
        if (tool != null && tool.intent !in app.metria.GeneratedAssistantContract.TASK_TOOLS[turn.task.wireName].orEmpty()) return null
        Result(turn.requestId, reply, tool, promptTokens, completionTokens, totalTokens, usageKnown = usage != null)
    }.getOrNull()

    private data class Message(val role: String, val content: String)

    private const val MealProposalParserLimit = 16_000

    private fun safeText(value: String): Boolean = value.none { it == '\u0000' || (it.isISOControl() && it !in "\n\r\t") }

}
