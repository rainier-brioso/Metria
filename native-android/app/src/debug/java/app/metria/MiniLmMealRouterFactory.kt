package app.metria

import android.content.Context

/** Creates the debug MiniLM implementation without exposing it to release source. */
internal object MiniLmMealRouterFactory {
    fun create(context: Context): MiniLmMealRouter = DebugMiniLmMealRouter(
        app.metria.debug.MiniLmMealRouter(context.applicationContext)
    )

    private class DebugMiniLmMealRouter(
        private val delegate: app.metria.debug.MiniLmMealRouter
    ) : MiniLmMealRouter {
        override fun routeMealRead(text: String, toolName: String): MiniLmRouteDecision = try {
            delegate.routeMealRead(text, toolName)
        } catch (_: Exception) {
            MiniLmRouteDecision(MiniLmRouteOutcome.RUNTIME_UNAVAILABLE)
        }
        override fun close() = delegate.close()
    }
}
