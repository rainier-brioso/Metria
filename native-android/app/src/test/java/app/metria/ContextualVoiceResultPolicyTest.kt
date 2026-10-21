package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContextualVoiceResultPolicyTest {
    @Test fun `clearing cancelled voice changes effect identity and permits one pending tool delivery`() {
        val cancelled = ContextualVoiceResultPolicy.EffectKey("cancelled", 12, 12)
        val idle = ContextualVoiceResultPolicy.EffectKey("idle", null, null)
        assertNotEquals(cancelled, idle)
        var observedKey: ContextualVoiceResultPolicy.EffectKey? = null
        var handledRequest: Long? = null
        var deliveries = 0
        fun observe(key: ContextualVoiceResultPolicy.EffectKey) {
            if (key == observedKey) return
            observedKey = key
            if (key.isolatesAssistantResults) return
            if (shouldConsumeAssistantResult(42L, handledRequest, 42L)) {
                handledRequest = 42L
                deliveries += 1
            }
        }
        observe(cancelled)
        observe(cancelled.copy())
        assertEquals(0, deliveries)
        observe(idle)
        assertEquals(1, deliveries)
        observe(idle.copy())
        observe(cancelled)
        observe(idle)
        assertEquals(1, deliveries)
    }

    @Test fun `every voice isolation input participates in effect identity`() {
        val ready = ContextualVoiceResultPolicy.EffectKey("ready", 12, 12)
        assertNotEquals(ready, ready.copy(state = "idle"))
        assertNotEquals(ready, ready.copy(activeCaptureToken = 13))
        assertNotEquals(ready, ready.copy(resultCaptureToken = 13))
        assertTrue(ready.isolatesAssistantResults)
        assertFalse(ready.copy(state = "idle").isolatesAssistantResults)
    }

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
