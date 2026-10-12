package app.metria.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

/**
 * Enables native word/range selection for one visible chat text region.
 * Clipboard reads are disabled in this scope; selection Copy writes only the
 * selected visible text using Metria's sensitive-clip handling.
 */
@Composable
fun SelectableChatText(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sensitiveClipboard = remember(context) {
        object : ClipboardManager {
            override fun getText(): AnnotatedString? = null

            override fun setText(annotatedString: AnnotatedString) {
                copyChatMessageToClipboard(context, annotatedString.text)
            }
        }
    }
    CompositionLocalProvider(LocalClipboardManager provides sensitiveClipboard) {
        SelectionContainer(content = content)
    }
}

/** Copies selected visible text to Android's clipboard with its sensitive flag set. */
private fun copyChatMessageToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboardManager
            ?: throw IllegalStateException("Clipboard unavailable")
        val clip = ClipData.newPlainText("Metria message", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        // Android 13+ displays its own clipboard confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
        }
    } catch (_: RuntimeException) {
        Toast.makeText(context, "Text could not be copied. Try again.", Toast.LENGTH_SHORT).show()
    }
}
