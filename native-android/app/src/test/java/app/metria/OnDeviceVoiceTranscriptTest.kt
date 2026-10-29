package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files
import java.util.UUID

class OnDeviceVoiceTranscriptTest {
    @Test fun onlyOwnedPlainSpeechCanEnterTheCloudTextHandoff() {
        val root = Files.createTempDirectory("voice-fixture").toFile()
        try {
            val owned = root.resolve("metria-voice").apply { mkdirs() }
            val name = "cloud-${UUID.randomUUID()}.transcript"
            val current = owned.resolve(name).apply { writeText("I ate 150 grams of rice") }
            assertEquals("I ate 150 grams of rice", OnDeviceVoiceTranscript.readInDirectory(owned, current))
            val outside = root.resolve(name).apply { writeText("I ate 150 grams of rice") }
            assertNull(OnDeviceVoiceTranscript.readInDirectory(owned, outside))
            current.writeText("{\"intent\":\"save\"}")
            assertNull(OnDeviceVoiceTranscript.readInDirectory(owned, current))
            current.writeText("")
            assertNull(OnDeviceVoiceTranscript.readInDirectory(owned, current))
            current.writeText("a".repeat(2500))
            assertNull(OnDeviceVoiceTranscript.readInDirectory(owned, current))
        } finally {
            root.deleteRecursively()
        }
    }
}
