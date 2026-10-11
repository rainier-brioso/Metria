package app.metria

import android.content.Context

/** Release builds keep the feature disabled until the debug routing gates pass. */
internal object MiniLmMealRouterFactory {
    fun create(context: Context): MiniLmMealRouter = ReleaseMiniLmMealRouter

    private object ReleaseMiniLmMealRouter : MiniLmMealRouter {
        override fun acceptsTodayMealList(text: String): Boolean = false
        override fun close() = Unit
    }
}
