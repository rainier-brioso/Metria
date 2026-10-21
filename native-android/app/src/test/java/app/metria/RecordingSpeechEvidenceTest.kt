package app.metria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSpeechEvidenceTest {
    @Test fun `manual stop of a quiet recording has no speech evidence`() {
        val recording = RecordingSpeechEvidence(700)

        assertFalse(recording.detectedSpeech)
        assertFalse(recording.observePeak(699))
        assertFalse(recording.detectedSpeech)
    }

    @Test fun `manual and automatic stop observe the same monotonic speech evidence`() {
        val recording = RecordingSpeechEvidence(700)

        assertTrue(recording.observePeak(700))
        // Both stop paths read this recording-scoped value for Clip.detectedSpeech.
        assertTrue(recording.detectedSpeech)
        assertFalse(recording.observePeak(0))
        assertTrue(recording.detectedSpeech)
    }

    @Test fun `new recording starts without speech evidence from an earlier capture`() {
        val previous = RecordingSpeechEvidence(700)
        previous.observePeak(1_200)
        assertTrue(previous.detectedSpeech)

        val next = RecordingSpeechEvidence(700)
        assertFalse(next.detectedSpeech)
        assertFalse(next.observePeak(650))
        assertFalse(next.detectedSpeech)
    }
}
