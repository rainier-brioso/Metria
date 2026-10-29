package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudPhotoAccessEndpointTest {
    @Test
    fun `endpoint accepts normalized secure broker address`() {
        assertEquals("https://broker.example", CloudPhotoAccess.validateEndpoint(" https://Broker.Example/ "))
        assertEquals("https://broker.example:8443", CloudPhotoAccess.validateEndpoint("https://broker.example:8443"))
    }

    @Test
    fun `endpoint rejects insecure local or credential-bearing addresses`() {
        listOf(
            "http://broker.example",
            "https://user:secret@broker.example",
            "https://broker.example/path",
            "https://broker.example?token=secret",
            "https://localhost",
            "https://127.0.0.1",
            "https://[::1]",
            "https://broker.example:99999",
        ).forEach { endpoint -> assertNull(endpoint, CloudPhotoAccess.validateEndpoint(endpoint)) }
    }
}
