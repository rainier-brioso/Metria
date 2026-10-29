package app.metria.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

/** Selected processing route, not a claim that a request or connection succeeded. */
@Composable
fun AssistantProviderLabel(cloud: Boolean, modifier: Modifier = Modifier, localName: String = "Gemma") {
    Surface(modifier.semantics(mergeDescendants = true) {}, color = MetriaColors.PrimarySoft,
        shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (cloud) Icons.Rounded.Cloud else Icons.Rounded.PhoneAndroid, null,
                modifier = Modifier.size(16.dp), tint = MetriaColors.InkMuted)
            Text(if (cloud) "Cloud · Nemotron" else "Local · $localName",
                style = MaterialTheme.typography.labelMedium, color = MetriaColors.InkMuted)
        }
    }
}
