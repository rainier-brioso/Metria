package app.metria

internal interface MealEntryAudioRecorder {
    fun start(onAutoStop: (GemmaAudioRecorder.Clip) -> Unit): Result<Unit>
    fun stop(): GemmaAudioRecorder.Clip?
    fun cancel()
}

/** A null recorder result can mean that its already-finalized auto-stop callback is queued. */
internal sealed interface MealEntryStopResult<out T> {
    data class Captured<T>(val clip: T) : MealEntryStopResult<T>
    data object AwaitingAutoStop : MealEntryStopResult<Nothing>
    data object Cancelled : MealEntryStopResult<Nothing>
}

internal object MealEntryStopPolicy {
    fun <T : Any> result(attemptActive: Boolean, clip: T?): MealEntryStopResult<T> = when {
        !attemptActive -> MealEntryStopResult.Cancelled
        clip == null -> MealEntryStopResult.AwaitingAutoStop
        else -> MealEntryStopResult.Captured(clip)
    }
}
