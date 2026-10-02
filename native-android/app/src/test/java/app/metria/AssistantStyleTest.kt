package app.metria

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantStyleTest {
    @Test
    fun `old vaults and unknown values use balanced style`() {
        assertEquals(EncryptedVault.AssistantStyle.BALANCED, EncryptedVault.AssistantStyle.fromStorage(null))
        assertEquals(EncryptedVault.AssistantStyle.BALANCED, EncryptedVault.AssistantStyle.fromStorage(""))
        assertEquals(EncryptedVault.AssistantStyle.BALANCED, EncryptedVault.AssistantStyle.fromStorage("future"))
    }

    @Test
    fun `styles have distinct member-facing labels`() {
        val styles = EncryptedVault.AssistantStyle.entries
        assertEquals(listOf("Balanced", "Coach", "Records assistant"), styles.map { it.title })
        assertEquals(listOf("balanced", "coach", "records"), styles.map { it.storageValue })
    }
}
