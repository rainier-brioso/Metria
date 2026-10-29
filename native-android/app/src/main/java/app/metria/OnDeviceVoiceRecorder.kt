package app.metria

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File
import java.util.UUID

/** Cloud mode speech input: Android's explicit on-device recognizer, never a network ASR fallback. */
internal class OnDeviceVoiceRecorder(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var callback: ((GemmaAudioRecorder.Clip) -> Unit)? = null
    private var generation = 0L
    private var startedAt = 0L
    private var pending = false
    private var timeout: Runnable? = null

    fun start(onResult: (GemmaAudioRecorder.Clip) -> Unit): Result<Unit> = runCatching {
        check(Looper.myLooper() == Looper.getMainLooper())
        check(Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
        check(!pending)
        val token = ++generation
        val service = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = service
        pending = true
        callback = onResult
        startedAt = android.os.SystemClock.elapsedRealtime()
        service.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) { finish(token, null) }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                finish(token, spoken)
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        service.startListening(intent)
        timeout = Runnable { if (token == generation && pending) finish(token, null) }
            .also { handler.postDelayed(it, 20_000) }
    }.onFailure { cancel() }

    fun awaitingRecognition(): Boolean = pending

    fun stop() {
        if (pending) runCatching { recognizer?.stopListening() }
    }

    fun cancel() {
        ++generation
        pending = false
        callback = null
        cleanup()
    }

    private fun cleanup() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        val service = recognizer
        recognizer = null
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching { service?.cancel(); service?.destroy() }
        } else handler.post { runCatching { service?.cancel(); service?.destroy() } }
    }

    private fun finish(token: Long, words: String?) {
        if (token != generation || !pending) return
        val deliver = callback
        callback = null
        pending = false
        cleanup()
        val text = (VoiceTranscriptParser.classify(words.orEmpty()) as? VoiceTranscriptParseResult.Accepted)?.text
        val directory = File(context.cacheDir, "metria-voice").apply { mkdirs() }
        val file = File(directory, "cloud-${UUID.randomUUID()}.transcript")
        runCatching {
            file.writeText(text.orEmpty(), Charsets.UTF_8)
            deliver?.invoke(GemmaAudioRecorder.Clip(file,
                (android.os.SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0), !text.isNullOrBlank()))
                ?: file.delete()
        }.onFailure { file.delete() }
    }
}

internal object OnDeviceVoiceTranscript {
    fun read(context: Context, file: File): String? = readInDirectory(File(context.cacheDir, "metria-voice"), file)

    internal fun readInDirectory(directory: File, file: File): String? {
        if (file.canonicalFile.parentFile != directory.canonicalFile ||
            !file.name.matches(Regex("cloud-[a-f0-9-]{36}\\.transcript")) ||
            !file.isFile || file.length() !in 1..2400) return null
        return (VoiceTranscriptParser.classify(file.readText(Charsets.UTF_8)) as? VoiceTranscriptParseResult.Accepted)?.text
    }
}
