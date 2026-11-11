package app.metria.compatible

import org.junit.Assert.*
import org.junit.Test
import app.metria.cloud.CloudAssistantRequest
import org.json.JSONObject

class CompatibleProviderSettingsTest {
    @Test fun `HTTP is allowed only for exact loopback and preserves port and API prefix`() {
        assertEquals("http://127.0.0.1:8080/v1", CompatibleProviderSettings.normalizeBaseUrl("http://127.0.0.1:8080"))
        assertEquals("http://127.0.0.1:8080/v1", CompatibleProviderSettings.normalizeBaseUrl("http://127.0.0.1:8080/v1/"))
        for (input in listOf("http://192.168.1.2:8080", "http://localhost:8080", "http://127.0.0.2:8080",
            "http://127.0.0.1.example.test:8080", "http://127.0.0.1@models.example.test", "http://[::1]:8080")) {
            assertTrue(input, runCatching { CompatibleProviderSettings.normalizeBaseUrl(input) }.isFailure)
        }
    }

    @Test fun `API roots preserve proxy prefixes and normalize v1 exactly once`() {
        for ((input, expected) in listOf(
            "https://models.example.test" to "https://models.example.test/v1",
            "https://models.example.test/v1/" to "https://models.example.test/v1",
            "https://models.example.test:9443/ai" to "https://models.example.test:9443/ai/v1",
            "https://models.example.test:443/ai/v1" to "https://models.example.test/ai/v1",
        )) assertEquals(expected, CompatibleProviderSettings.normalizeBaseUrl(input))
    }

    @Test fun `rejects insecure ambiguous and secret-bearing destinations`() {
        for (input in listOf("http://models.example.test", "https://localhost/v1", "https://127.0.0.2",
            "https://[::1]/", "https://0.0.0.0", "https://user:pass@models.example.test", "https://models.example.test?key=secret",
            "https://models.example.test#fragment", "https://models.example.test:0", "https://models.example.test:65536",
            "https://models.example.test/a/../v1", "https://models.example.test/a%2fb", "https://models.example.test//v1")) {
            assertTrue(input, runCatching { CompatibleProviderSettings.normalizeBaseUrl(input) }.isFailure)
        }
    }

    @Test fun `model is explicit and connection type cannot invent local capabilities`() {
        assertTrue(runCatching { CompatibleProviderSettings(baseUrl = "https://models.example.test", model = "").validated() }.isFailure)
        assertTrue(runCatching { CompatibleProviderSettings("unknown", "https://models.example.test", "model").validated() }.isFailure)
        assertFalse(CompatibleProviderSettings("custom", "https://models.example.test", "model").validated().vision)
    }

    @Test fun `compatible identity and missing usage cannot weaken the Nebius parser`() {
        val turn = CloudAssistantRequest.build(CloudAssistantRequest.Task.CHAT, emptyList(), "Hello")!!
        val reply = JSONObject().put("request_id", turn.requestId).put("provider", "compatible")
            .put("model", "selected-model").put("reply", "Hello there.").put("tool", JSONObject.NULL)
            .put("usage", JSONObject.NULL)
        assertNull(CloudAssistantRequest.parse(reply.toString(), turn))
        val result = CloudAssistantRequest.parse(reply.toString(), turn, "compatible", "selected-model")!!
        assertFalse(result.usageKnown)
        assertNull(CloudAssistantRequest.parse(reply.toString(), turn, "compatible", "different-model"))
        reply.put("request_id", "different-request")
        assertNull(CloudAssistantRequest.parse(reply.toString(), turn, "compatible", "selected-model"))
    }
}
