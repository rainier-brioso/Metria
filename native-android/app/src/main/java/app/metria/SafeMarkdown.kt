package app.metria

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Deliberately limited local Markdown: no links, HTML, images, or embedded actions. */
@Composable
fun SafeMarkdownText(markdown: String, color: Color) = Text(safeMarkdown(markdown), color = color)

private fun safeMarkdown(value: String): AnnotatedString = buildAnnotatedString {
    value.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append('\n')
        val line = rawLine.replace(Regex("^[-*]\\s+"), "• ")
        var position = 0
        while (position < line.length) {
            val bold = line.indexOf("**", position)
            val italic = line.indexOf('*', position)
            val next = listOf(bold, italic).filter { it >= position }.minOrNull() ?: line.length
            append(line.substring(position, next))
            if (next == line.length) break
            val marker = if (line.startsWith("**", next)) "**" else "*"
            val end = line.indexOf(marker, next + marker.length)
            if (end < 0) { append(marker); position = next + marker.length; continue }
            pushStyle(SpanStyle(fontWeight = if (marker == "**") FontWeight.Bold else FontWeight.Normal, fontStyle = if (marker == "*") FontStyle.Italic else FontStyle.Normal))
            append(line.substring(next + marker.length, end))
            pop()
            position = end + marker.length
        }
    }
}
