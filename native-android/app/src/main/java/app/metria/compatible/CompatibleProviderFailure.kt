package app.metria.compatible

/** Fixed categories only: never carry server bodies, addresses, keys or member content. */
internal class CompatibleProviderFailure(val category: Category) : Exception(category.name) {
    enum class Category {
        REQUEST, CONNECTION, TIMEOUT, HTTP, MODEL, TRUNCATED, RESPONSE, HOST_RESPONSE, VISION_DISABLED, VISION_UNSUPPORTED,
    }

    val memberMessage: String get() = when (category) {
        Category.REQUEST -> "Metria could not prepare this server request. Try a shorter message."
        Category.CONNECTION -> "Could not connect to the server. Check that it and the phone connection are still running."
        Category.TIMEOUT -> "The server took too long to answer. Try again when it is ready."
        Category.HTTP -> "The server rejected the request. Check the selected model and connection settings."
        Category.MODEL -> "The server answered with a different model. Load available models in Settings and select the exact model."
        Category.TRUNCATED -> "The server stopped before completing its answer. Try turning off extended reasoning in this connection's settings."
        Category.RESPONSE, Category.HOST_RESPONSE -> "The server answered, but Metria could not verify its response. Your local records were not changed."
        Category.VISION_DISABLED -> "Photo support is off for this connection. Select an image-capable model in Settings before enabling meal photos."
        Category.VISION_UNSUPPORTED -> "The selected server model does not support photos. Choose an image-capable model for meal photos, or describe the foods in chat."
    }
}
