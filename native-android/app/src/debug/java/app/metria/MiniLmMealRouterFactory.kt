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
        override fun acceptsTodayMealList(text: String): Boolean = delegate.acceptsTodayMealList(text)
        override fun close() = delegate.close()
    }
}
