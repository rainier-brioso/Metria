package app.metria.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

/** One visual identity for voice capture, transcription and the resulting Chat operation. */
@Composable
internal fun AssistantActivityCard(
    title: String,
    busy: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MetriaColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(
                        modifier = Modifier.size(36.dp), strokeWidth = 2.dp,
                        color = MetriaColors.Primary,
                    )
                    Icon(Icons.Rounded.Spa, contentDescription = null,
                        tint = MetriaColors.Primary, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Metria", style = MaterialTheme.typography.labelMedium, color = MetriaColors.InkMuted)
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            content()
        }
    }
}
