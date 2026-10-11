package app.metria.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.metria.GoalDailyReferenceReview
import app.metria.GoalReferenceCompleteness

@Composable
internal fun GoalReferenceStatusLabel(review: GoalDailyReferenceReview, modifier: Modifier = Modifier) {
    val (label, icon) = when (review.completeness) {
        GoalReferenceCompleteness.SET -> "Set" to Icons.Rounded.CheckCircle
        GoalReferenceCompleteness.PARTLY_SET -> "Partly set" to Icons.Rounded.Info
        GoalReferenceCompleteness.NOT_SET -> "Not set" to Icons.Rounded.RemoveCircleOutline
    }
    Row(
        modifier.semantics { stateDescription = "$label, ${review.suppliedCount} of 4 daily reference values supplied" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$label · ${review.suppliedCount} of 4 daily reference values supplied", style = MaterialTheme.typography.bodyMedium)
    }
}
