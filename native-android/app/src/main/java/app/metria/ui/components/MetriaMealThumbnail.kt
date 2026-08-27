package app.metria.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.metria.ui.theme.MetriaColors

/** Keeps private meal imagery visually consistent without making it the row's only identifier. */
@Composable
fun MetriaMealThumbnail(
    encoded: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    val bitmap = remember(encoded) {
        encoded?.let { value ->
            runCatching {
                val bytes = Base64.decode(value.substringAfter("base64,", value), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    Surface(
        color = MetriaColors.AccentSoft,
        shape = shape,
        modifier = modifier.size(size),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Private meal thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MetriaColors.Primary)
            }
        }
    }
}
