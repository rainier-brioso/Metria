package app.metria

import android.content.Context

/** Release builds keep the feature disabled until the debug routing gates pass. */
internal object MiniLmMealRouterFactory {
    fun create(context: Context): MiniLmMealRouter = ReleaseMiniLmMealRouter

    private object ReleaseMiniLmMealRouter : MiniLmMealRouter {
        override fun routeMealRead(text: String, toolName: String) =
            MiniLmRouteDecision(MiniLmRouteOutcome.SCOPE_REJECTED)
        override fun close() = Unit
    }
}
