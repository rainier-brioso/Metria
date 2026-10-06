package app.metria.cloud

import org.json.JSONObject

/** JSON syntax gate before org.json: rejects duplicate keys and its permissive extensions. */
internal object StrictOnlineJson {
    fun objectOrNull(text: String): JSONObject? = runCatching {
        require(text.length <= 16_384)
        val reader = Reader(text)
        reader.value(0)
        reader.space()
        require(reader.position == text.length)
        JSONObject(text)
    }.getOrNull()

    private class Reader(val text: String) {
        var position = 0
        fun space() { while (position < text.length && text[position] in " \r\n\t") position++ }
        private fun take(c: Char): Boolean {
            space()
            if (position < text.length && text[position] == c) { position++; return true }
            return false
        }
        private fun string(): String {
            space()
            val start = position
            require(position < text.length && text[position++] == '"')
            while (position < text.length) {
                val c = text[position++]
                if (c == '"') return org.json.JSONTokener(text.substring(start, position)).nextValue() as String
                require(c.code >= 32)
                if (c == '\\') {
                    require(position < text.length)
                    val escaped = text[position++]
                    if (escaped == 'u') {
                        repeat(4) { require(position < text.length && text[position++].digitToIntOrNull(16) != null) }
                    } else require(escaped in "\"\\/bfnrt")
                }
            }
            error("Unterminated string")
        }
        fun value(depth: Int) {
            require(depth <= 8)
            space()
            require(position < text.length)
            when (text[position]) {
                '{' -> {
                    position++
                    val keys = mutableSetOf<String>()
                    if (take('}')) return
                    do {
                        require(keys.add(string()))
                        require(take(':'))
                        value(depth + 1)
                        if (take('}')) return
                        require(take(','))
                    } while (true)
                }
                '[' -> {
                    position++
                    if (take(']')) return
                    do {
                        value(depth + 1)
                        if (take(']')) return
                        require(take(','))
                    } while (true)
                }
                '"' -> string()
                't', 'f', 'n' -> {
                    val literal = when (text[position]) { 't' -> "true"; 'f' -> "false"; else -> "null" }
                    require(text.startsWith(literal, position))
                    position += literal.length
                }
                else -> {
                    val match = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
                        .find(text, position) ?: error("Invalid number")
                    require(match.range.first == position)
                    position = match.range.last + 1
                }
            }
        }
    }
}
