package app.metria

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedArchiveTest {
    @Test fun `archive round trips only with its passphrase`() {
        val archive = EncryptedArchive.create(JSONObject().put("version", 1).put("entries", 2), "correct horse battery".toCharArray())

        assertEquals(2, EncryptedArchive.open(archive, "correct horse battery".toCharArray()).getInt("entries"))
        assertTrue(runCatching { EncryptedArchive.open(archive, "wrong passphrase".toCharArray()) }.isFailure)
    }

    @Test fun `tampered archive is rejected`() {
        val archive = EncryptedArchive.create(JSONObject().put("version", 1), "correct horse battery".toCharArray())
        archive[archive.lastIndex] = (archive.last().toInt() xor 1).toByte()

        assertTrue(runCatching { EncryptedArchive.open(archive, "correct horse battery".toCharArray()) }.isFailure)
    }
}
