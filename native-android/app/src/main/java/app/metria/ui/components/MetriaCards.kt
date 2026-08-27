package app.metria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

@Composable
fun MetriaMetricCard(
    label: String,
    value: String,
    unit: String?,
    icon: ImageVector,
    supporting: String? = null,
    accessibilityLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetriaColors.Surface),
        modifier = modifier
            .semantics { contentDescription = accessibilityLabel }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
                Surface(color = MetriaColors.SurfaceSubtle, shape = CircleShape) {
                    Icon(icon, contentDescription = null, tint = MetriaColors.Primary, modifier = Modifier.padding(8.dp).size(18.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displaySmall)
                unit?.let { Text(it, color = MetriaColors.InkMuted, modifier = Modifier.padding(bottom = 4.dp)) }
            }
            supporting?.let { Text(it, color = MetriaColors.InkMuted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun MetriaEstimateBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MetriaColors.EstimatedSoft,
        contentColor = MetriaColors.Estimated,
        shape = CircleShape,
        modifier = modifier.semantics { contentDescription = text },
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("≈", style = MaterialTheme.typography.labelMedium)
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
