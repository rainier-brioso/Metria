package app.metria

import org.json.JSONObject

/** Shared tool shape/limits precede the existing contextual host validators. */
internal object AssistantContractPolicy {
    private val tools by lazy {
        val entries = JSONObject(GeneratedAssistantContract.SOURCE_JSON).getJSONArray("tools")
        (0 until entries.length()).associate { index ->
            val tool = entries.getJSONObject(index)
            tool.getString("name") to tool
        }
    }

    fun accepts(name: String, arguments: Map<String, *>): Boolean = runCatching {
        val tool = tools[name] ?: return false
        val entries = tool.getJSONArray("parameters")
        val parameters = (0 until entries.length()).associate { index ->
            val parameter = entries.getJSONObject(index)
            parameter.getString("name") to parameter
        }
        if (!parameters.keys.containsAll(arguments.keys)) return false
        for ((key, parameter) in parameters) {
            if (!arguments.containsKey(key)) {
                if (parameter.getBoolean("required")) return false
                continue
            }
            val value = arguments[key]
            if (value == null || value === JSONObject.NULL) {
                if (!parameter.getBoolean("nullable")) return false
                continue
            }
            when (parameter.getString("type")) {
                "integer" -> {
                    val number = (value as? Number)?.toDouble() ?: return false
                    if (!number.isFinite() || number != kotlin.math.floor(number) ||
                        number < parameter.getInt("minimum") || number > parameter.getInt("maximum")) return false
                    if (parameter.has("maximum_from")) {
                        val maximum = (arguments[parameter.getString("maximum_from")] as? Number)?.toDouble()
                            ?: return false
                        if (number > maximum) return false
                    }
                }
                "string" -> {
                    val text = value as? String ?: return false
                    if (text.trim().length !in parameter.optInt("min_length", 1)..parameter.optInt("max_length", 120) ||
                        text.any { it.isISOControl() && it !in "\n\r\t" }) return false
                    if (parameter.has("enum")) {
                        val values = parameter.getJSONArray("enum")
                        if ((0 until values.length()).none { values.getString(it) == text }) return false
                    }
                }
                else -> return false
            }
        }
        true
    }.getOrDefault(false)
}
