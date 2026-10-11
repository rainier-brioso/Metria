package app.metria

/** Build-variant boundary for the optional MiniLM fast route. */
internal interface MiniLmMealRouter : AutoCloseable {
    fun acceptsTodayMealList(text: String): Boolean
}
