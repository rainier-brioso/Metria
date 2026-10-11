package app.metria

import org.json.JSONObject

/** Optional additive vault fields; legacy audits have no metrics object. */
object LocalAnalysisMetricsCodec {
    fun encode(metrics: LocalAnalysisMetrics): JSONObject {
        require(LocalAnalysisAuditValidation.validMetrics(metrics))
        return JSONObject()
            .put("imagePreparationMillis", metrics.imagePreparationMillis)
            .put("conversationSetupMillis", metrics.conversationSetupMillis)
            .put("modelRequestMillis", metrics.modelRequestMillis)
            .put("firstTokenMillis", metrics.firstTokenMillis)
            .put("validationMillis", metrics.validationMillis)
            .put("thermalStatusStart", metrics.thermalStatusStart)
            .put("thermalStatusEnd", metrics.thermalStatusEnd)
    }

    fun decode(audit: JSONObject): LocalAnalysisMetrics? {
        if (!audit.has("metrics") || audit.isNull("metrics")) return null
        val values = audit.get("metrics") as? JSONObject ?: error("Invalid analysis metrics.")
        fun measurement(key: String): Long? {
            if (!values.has(key) || values.isNull(key)) return null
            val value = values.get(key)
            require(value is Number && value.toDouble().isFinite() &&
                value.toDouble() == value.toLong().toDouble() && value.toDouble() in 0.0..600_000.0) {
                "Invalid analysis measurement."
            }
            return value.toLong()
        }
        return LocalAnalysisMetrics(
            imagePreparationMillis = measurement("imagePreparationMillis"),
            conversationSetupMillis = measurement("conversationSetupMillis"),
            modelRequestMillis = measurement("modelRequestMillis"),
            firstTokenMillis = measurement("firstTokenMillis"),
            validationMillis = measurement("validationMillis"),
            thermalStatusStart = measurement("thermalStatusStart")?.toInt(),
            thermalStatusEnd = measurement("thermalStatusEnd")?.toInt(),
        ).also { require(LocalAnalysisAuditValidation.validMetrics(it)) { "Invalid analysis metrics." } }
    }
}
