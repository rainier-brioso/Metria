package app.metria

import android.content.Context

/** Short-lived local recorder controller; tokens reject permission and auto-stop callbacks after exit. */
internal class MealEntryAudioCapture internal constructor(private val recorder: MealEntryAudioRecorder) {
    constructor(context: Context) : this(GemmaAudioRecorderAdapter(GemmaAudioRecorder(context)))

    private val attempts = MealEntryAttemptGate()

    fun beginAttempt(): Long = attempts.begin()

    fun isActive(token: Long): Boolean = attempts.isCurrent(token)

    @Synchronized
    fun start(token: Long, onAutoStop: (GemmaAudioRecorder.Clip) -> Unit): Result<Unit> {
        if (!attempts.isCurrent(token)) return Result.failure(IllegalStateException("Recording was cancelled."))
        return recorder.start { clip ->
            val accepted = attempts.finish(token)
            if (accepted) onAutoStop(clip) else clip.file.delete()
        }.onFailure { attempts.finish(token) }
    }

    @Synchronized
    fun stop(token: Long): MealEntryStopResult<GemmaAudioRecorder.Clip> {
        if (!attempts.isCurrent(token)) return MealEntryStopResult.Cancelled
        val clip = recorder.stop()
        return when (val result = MealEntryStopPolicy.result(attempts.isCurrent(token), clip)) {
            is MealEntryStopResult.Captured -> {
                if (attempts.finish(token)) result
                else {
                    result.clip.file.delete()
                    MealEntryStopResult.Cancelled
                }
            }
            MealEntryStopResult.AwaitingAutoStop -> MealEntryStopResult.AwaitingAutoStop
            MealEntryStopResult.Cancelled -> {
                clip?.file?.delete()
                MealEntryStopResult.Cancelled
            }
        }
    }

    @Synchronized
    fun cancel() {
        attempts.cancel()
        recorder.cancel()
    }
}

private class GemmaAudioRecorderAdapter(private val recorder: GemmaAudioRecorder) : MealEntryAudioRecorder {
    override fun start(onAutoStop: (GemmaAudioRecorder.Clip) -> Unit): Result<Unit> = recorder.start(onAutoStop)
    override fun stop(): GemmaAudioRecorder.Clip? = recorder.stop()
    override fun cancel() = recorder.cancel()
}
