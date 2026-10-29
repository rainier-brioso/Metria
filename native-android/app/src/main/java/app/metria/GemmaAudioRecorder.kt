package app.metria

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID
import kotlin.concurrent.thread

/** Captures a short WAV solely for the current Gemma request; callers must delete it after use. */
class GemmaAudioRecorder(private val context: Context) {
    data class Clip(val file: File, val durationMs: Long, val detectedSpeech: Boolean)

    private val sampleRate = 16_000
    private var recorder: AudioRecord? = null
    private var writer: Thread? = null
    private var clipFile: File? = null
    private var startedAt = 0L
    @Volatile private var recording = false
    @Volatile private var speechEvidence: RecordingSpeechEvidence? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cloudRecorder: OnDeviceVoiceRecorder? = null
    private companion object {
        const val voiceThreshold = 700
        const val silenceStopMs = 1_200L
        const val maximumRecordingMs = 12_000L
    }

    /** Records only until local speech ends; no sound is sent outside the current app process. */
    fun start(onAutoStop: (Clip) -> Unit): Result<Unit> = runCatching {
        if (CloudPhotoAccess(context).selectedMode() == "cloud") {
            val local = cloudRecorder ?: OnDeviceVoiceRecorder(context).also { cloudRecorder = it }
            local.start(onAutoStop).getOrThrow()
            return@runCatching
        }
        check(!recording) { "Already recording" }
        val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "This phone cannot record the required local audio format." }
        val directory = File(context.cacheDir, "metria-voice").apply { mkdirs() }
        val file = File(directory, "voice-${UUID.randomUUID()}.wav")
        val localRecorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minimum * 2)
        check(localRecorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone recording could not start." }
        val localSpeechEvidence = RecordingSpeechEvidence(voiceThreshold)
        val localStartedAt = android.os.SystemClock.elapsedRealtime()
        clipFile = file
        recorder = localRecorder
        speechEvidence = localSpeechEvidence
        startedAt = localStartedAt
        recording = true
        writer = thread(name = "metria-local-voice") {
            FileOutputStream(file).use { output ->
                output.write(ByteArray(44))
                val buffer = ByteArray(minimum)
                localRecorder.startRecording()
                var bytes = 0
                var lastVoiceAt = 0L
                var autoStopped = false
                while (recording) {
                    val count = localRecorder.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        output.write(buffer, 0, count); bytes += count
                        val peak = peakAmplitude(buffer, count)
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (localSpeechEvidence.observePeak(peak)) lastVoiceAt = now
                        if (now - localStartedAt >= maximumRecordingMs || (localSpeechEvidence.detectedSpeech && now - lastVoiceAt >= silenceStopMs)) {
                            recording = false
                            autoStopped = true
                        }
                    }
                }
                runCatching { localRecorder.stop() }
                writeWavHeader(file, bytes)
                if (autoStopped) {
                    localRecorder.release()
                    if (speechEvidence === localSpeechEvidence) {
                        recorder = null; writer = null; clipFile = null; speechEvidence = null
                    }
                    val clip = Clip(file, (android.os.SystemClock.elapsedRealtime() - localStartedAt).coerceAtLeast(0L), localSpeechEvidence.detectedSpeech)
                    mainHandler.post { onAutoStop(clip) }
                }
            }
        }
    }

    fun stop(): Clip? {
        if (cloudRecorder?.awaitingRecognition() == true) {
            cloudRecorder?.stop()
            return null
        }
        if (!recording) return null
        val localSpeechEvidence = speechEvidence
        recording = false
        runCatching { recorder?.stop() }
        writer?.join(1_000)
        recorder?.release()
        recorder = null; writer = null
        val file = clipFile ?: return null
        clipFile = null
        if (speechEvidence === localSpeechEvidence) speechEvidence = null
        return Clip(
            file,
            (android.os.SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            localSpeechEvidence?.detectedSpeech == true,
        )
    }

    fun awaitingRecognition(): Boolean = cloudRecorder?.awaitingRecognition() == true

    fun cancel() {
        cloudRecorder?.cancel()
        stop()?.file?.delete()
    }

    private fun writeWavHeader(file: File, dataBytes: Int) {
        RandomAccessFile(file, "rw").use { out ->
            fun text(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
            fun int(value: Int) { out.write(value and 0xff); out.write(value shr 8 and 0xff); out.write(value shr 16 and 0xff); out.write(value shr 24 and 0xff) }
            fun short(value: Int) { out.write(value and 0xff); out.write(value shr 8 and 0xff) }
            text("RIFF"); int(36 + dataBytes); text("WAVEfmt "); int(16); short(1); short(1)
            int(sampleRate); int(sampleRate * 2); short(2); short(16); text("data"); int(dataBytes)
        }
    }

    private fun peakAmplitude(buffer: ByteArray, count: Int): Int {
        var peak = 0
        var index = 0
        while (index + 1 < count) {
            val sample = (buffer[index].toInt() and 0xff) or (buffer[index + 1].toInt() shl 8)
            peak = maxOf(peak, kotlin.math.abs(sample.toShort().toInt()))
            index += 2
        }
        return peak
    }
}
