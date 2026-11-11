package app.metria.compatible

import org.json.JSONObject

data class CompatibleModel(val id: String, val vision: Boolean? = null)

internal object CompatibleModelCatalog {
    fun ollamaModels(raw: String): List<CompatibleModel> {
        val models = JSONObject(raw).getJSONArray("models")
        require(models.length() <= 2000)
        return (0 until models.length()).map { models.getJSONObject(it).getString("name") }
            .filter { it.length in 1..160 && it.none { char -> char.isWhitespace() || char.isISOControl() } }
            .distinct().sorted().map { CompatibleModel(it) }
    }

    fun ollamaVision(raw: String): Boolean? {
        val capabilities = JSONObject(raw).optJSONArray("capabilities") ?: return null
        if (capabilities.length() == 0) return null
        return (0 until capabilities.length()).any { capabilities.getString(it) == "vision" }
    }
}
