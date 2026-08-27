package app.metria.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MetriaColors {
    val Canvas = Color(0xFFF7F7F3)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSubtle = Color(0xFFF0F1EC)
    val Ink = Color(0xFF19181C)
    val InkMuted = Color(0xFF66636B)
    val Primary = Color(0xFF66507A)
    val PrimarySoft = Color(0xFFEEE8F2)
    val Accent = Color(0xFFA8D94F)
    val AccentSoft = Color(0xFFE4F5BE)
    val Verified = Color(0xFF287A5B)
    val Estimated = Color(0xFF9B5F0E)
    val EstimatedSoft = Color(0xFFFFF0D8)
    val Danger = Color(0xFFB3261E)
    val Divider = Color(0xFFE4E3DF)
}

private val MetriaColorScheme = lightColorScheme(
    primary = MetriaColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MetriaColors.PrimarySoft,
    onPrimaryContainer = MetriaColors.Ink,
    secondary = MetriaColors.Verified,
    onSecondary = Color.White,
    tertiary = MetriaColors.Accent,
    onTertiary = MetriaColors.Ink,
    background = MetriaColors.Canvas,
    onBackground = MetriaColors.Ink,
    surface = MetriaColors.Surface,
    onSurface = MetriaColors.Ink,
    surfaceVariant = MetriaColors.SurfaceSubtle,
    onSurfaceVariant = MetriaColors.InkMuted,
    error = MetriaColors.Danger,
    outline = MetriaColors.Divider,
)

private val MetriaTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
    displayMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val MetriaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MetriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MetriaColorScheme,
        typography = MetriaTypography,
        shapes = MetriaShapes,
        content = content,
    )
}
