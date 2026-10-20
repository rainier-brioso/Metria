package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualVoiceResultPolicyTest {
    @Test fun `matching one-use transcript channel isolates assistant effects through explicit handoff`() {
        assertTrue(ContextualVoiceResultPolicy.isolatesAssistantResult("processing", 12, 12))
        assertTrue(ContextualVoiceResultPolicy.isolatesAssistantResult("ready", 12, 12))
        assertTrue(ContextualVoiceResultPolicy.isolatesAssistantResult("failed", 12, 12))
        assertTrue(ContextualVoiceResultPolicy.isolatesAssistantResult("cancelled", 12, 12))
        assertFalse(ContextualVoiceResultPolicy.isolatesAssistantResult("ready", 13, 12))
        assertFalse(ContextualVoiceResultPolicy.isolatesAssistantResult("idle", 12, 12))
        assertFalse(ContextualVoiceResultPolicy.isolatesAssistantResult("processing", null, 12))
    }
}
