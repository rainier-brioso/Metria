package app.metria.compatible

import java.net.URI

/** No credentials, vault data or service-launch settings belong in this value. */
data class CompatibleProviderSettings(
    val kind: String = "ollama",
    val baseUrl: String,
    val model: String,
    val vision: Boolean = false,
    val disableReasoning: Boolean = false,
) {
    fun validated(): CompatibleProviderSettings {
        require(kind in setOf("ollama", "custom")) { "Choose Ollama or a custom connection." }
        require(model.length in 1..160 && model.none { it.isISOControl() || it.isWhitespace() }) { "Enter a valid model name." }
        return copy(baseUrl = normalizeBaseUrl(baseUrl))
    }

    companion object {
        /** HTTP is restricted to exact IPv4 loopback, for an explicitly configured local/tunnel connection. */
        fun normalizeBaseUrl(value: String): String {
            require(value.length in 1..2048 && value.none { it.isWhitespace() || it.isISOControl() }) { "Enter a secure server address." }
            val uri = runCatching { URI(value) }.getOrNull()
            val loopbackHttp = uri?.scheme.equals("http", true) && uri?.host == "127.0.0.1"
            require(uri != null && (uri.scheme.equals("https", true) || loopbackHttp) && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
                (uri.port == -1 || uri.port in 1..65535)) { "Use an HTTPS address without a password or query." }
            val host = uri.host.lowercase().removeSurrounding("[", "]")
            require(loopbackHttp || (host !in setOf("localhost", "::1", "0.0.0.0", "::") && !host.startsWith("127."))) {
                "Use your computer's reachable secure address, not localhost."
            }
            val path = uri.rawPath.orEmpty().trimEnd('/')
            require(!path.contains('%') && path.split('/').none { it == "." || it == ".." } && !path.contains("//")) {
                "Check the server address path."
            }
            val authority = if (host.contains(':')) "[$host]" else host
            val scheme = if (loopbackHttp) "http" else "https"
            val defaultPort = if (loopbackHttp) 80 else 443
            return "$scheme://$authority${if (uri.port == -1 || uri.port == defaultPort) "" else ":${uri.port}"}${if (path.endsWith("/v1")) path else "$path/v1"}"
        }
    }
}
