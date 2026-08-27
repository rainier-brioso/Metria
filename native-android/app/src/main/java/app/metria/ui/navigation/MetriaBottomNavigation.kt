package app.metria.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MetriaBottomNavigation(
    current: String,
    assistantListening: Boolean,
    onNavigate: (String) -> Unit,
    onMetriaAction: () -> Unit,
) {
    val leading = listOf(
        Destination("Today", "Today", Icons.Rounded.Home),
        Destination("History", "Progress", Icons.Rounded.BarChart),
    )
    val trailing = Destination("Privacy", "Privacy", Icons.Rounded.Shield)
    val transition = rememberInfiniteTransition(label = "metria-navigation-listening")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "metria-navigation-pulse",
    )
    NavigationBar(containerColor = MetriaColors.Surface, tonalElevation = 0.dp) {
        leading.forEach { destination ->
            NavigationBarItem(
                selected = current == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = onMetriaAction,
                containerColor = if (assistantListening) MetriaColors.Verified else MetriaColors.Accent,
                contentColor = MetriaColors.Ink,
                modifier = Modifier.offset(y = (-8).dp).graphicsLayer {
                    val scale = if (assistantListening) pulse else 1f
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                Icon(
                    Icons.Rounded.SmartToy,
                    contentDescription = if (assistantListening) "Metria is listening" else "Open Metria actions",
                )
            }
        }
        NavigationBarItem(
            selected = current == trailing.route,
            onClick = { onNavigate(trailing.route) },
            icon = { Icon(trailing.icon, contentDescription = trailing.label) },
            label = { Text(trailing.label) },
            modifier = Modifier.weight(1f),
        )
    }
}
