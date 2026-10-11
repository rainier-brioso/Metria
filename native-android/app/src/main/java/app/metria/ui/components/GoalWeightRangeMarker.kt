package app.metria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

@Composable
internal fun GoalWeightRangeMarker(
    current: Double,
    start: Double,
    end: Double,
    currentLabel: String,
    startLabel: String,
    endLabel: String,
) {
    GoalReferenceRangeMarker(current, start, end, currentLabel, startLabel, endLabel,
        "Weight marker. $currentLabel; range or target from $startLabel to $endLabel")
}

/** Shared static interval primitive; callers supply units, provenance and a complete description. */
@Composable
internal fun GoalReferenceRangeMarker(
    current: Double,
    start: Double,
    end: Double,
    currentLabel: String,
    startLabel: String,
    endLabel: String,
    description: String,
) {
    val low = minOf(start, end)
    val high = maxOf(start, end)
    val span = (high - low).takeIf { it > 0.0 } ?: 1.0
    val inset = 0.08f
    fun fraction(value: Double) = inset + (((value - low) / span).coerceIn(0.0, 1.0).toFloat() * (1f - inset * 2f))
    val startFraction = fraction(start)
    val endFraction = fraction(end)
    val currentFraction = fraction(current)
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val track = MetriaColors.Divider
    Column(Modifier.fillMaxWidth().semantics {
        contentDescription = description
    }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(currentLabel, style = MaterialTheme.typography.bodySmall, color = MetriaColors.InkMuted)
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val y = size.height / 2
            val x1 = size.width * startFraction
            val x2 = size.width * endFraction
            val markerX = size.width * currentFraction
            drawLine(track, Offset(size.width * inset, y), Offset(size.width * (1f - inset), y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawLine(primary, Offset(x1, y), Offset(x2, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(primary, radius = 7.dp.toPx(), center = Offset(markerX, y))
            drawCircle(onPrimary, radius = 2.5.dp.toPx(), center = Offset(markerX, y))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (start <= end) startLabel else endLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text(if (start <= end) endLabel else startLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
        }
    }
}
