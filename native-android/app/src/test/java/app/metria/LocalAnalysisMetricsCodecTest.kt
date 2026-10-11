package app.metria

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LocalAnalysisMetricsCodecTest {
    @Test fun legacyAuditsAndMissingMeasurementsStayUnknown() {
        assertNull(LocalAnalysisMetricsCodec.decode(JSONObject()))
        assertNull(LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", JSONObject.NULL)))
        val partial = LocalAnalysisMetrics(firstTokenMillis = 420)
        val restored = LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", LocalAnalysisMetricsCodec.encode(partial)))
        assertEquals(partial, restored)
        assertNull(restored?.modelRequestMillis)
    }

    @Test fun completedRunRoundTripsOnlyTechnicalFields() {
        val metrics = LocalAnalysisMetrics(100, 50, 3000, 800, 20, 0, 3)
        val encoded = LocalAnalysisMetricsCodec.encode(metrics)
        assertEquals(metrics, LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", encoded)))
        assertEquals(setOf("imagePreparationMillis", "conversationSetupMillis", "modelRequestMillis",
            "firstTokenMillis", "validationMillis", "thermalStatusStart", "thermalStatusEnd"), encoded.keys().asSequence().toSet())
    }

    @Test fun malformedStoredMeasurementsRejectInsteadOfCoercing() {
        for (value in listOf<Any>("12", true, -1, 1.5, 600001)) {
            assertThrows(IllegalArgumentException::class.java) {
                LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", JSONObject().put("firstTokenMillis", value)))
            }
        }
        assertThrows(IllegalStateException::class.java) {
            LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", "invalid"))
        }
    }

    @Test fun invalidThermalValuesAndImpossibleFirstTokenReject() {
        assertFalse(LocalAnalysisAuditValidation.validMetrics(LocalAnalysisMetrics(thermalStatusEnd = 7)))
        assertFalse(LocalAnalysisAuditValidation.validMetrics(LocalAnalysisMetrics(thermalStatusStart = -1)))
        assertFalse(LocalAnalysisAuditValidation.validMetrics(LocalAnalysisMetrics(firstTokenMillis = 100, modelRequestMillis = 99)))
        assertTrue(LocalAnalysisAuditValidation.validMetrics(LocalAnalysisMetrics(firstTokenMillis = 0, modelRequestMillis = 0)))
        assertThrows(IllegalArgumentException::class.java) {
            LocalAnalysisMetricsCodec.decode(JSONObject().put("metrics", JSONObject().put("thermalStatusStart", 4294967296L)))
        }
    }
}
