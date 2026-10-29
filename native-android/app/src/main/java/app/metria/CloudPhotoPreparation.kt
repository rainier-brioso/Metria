package app.metria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

/** Cloud's exact pixel boundary, separate from Gemma's decode sampling policy. */
internal object CloudPhotoSizePolicy {
    const val MAX_DIMENSION = 1536

    fun fit(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val largest = maxOf(width, height)
        if (largest <= MAX_DIMENSION) return width to height
        return (width.toLong() * MAX_DIMENSION / largest).toInt().coerceAtLeast(1) to
            (height.toLong() * MAX_DIMENSION / largest).toInt().coerceAtLeast(1)
    }
}

/** Takes only Metria's owned EXIF-free prepared JPEG; never changes the source. */
internal object CloudPhotoPreparation {
    fun encode(prepared: File): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(prepared.absolutePath, bounds)
        val (width, height) = CloudPhotoSizePolicy.fit(bounds.outWidth, bounds.outHeight)
        if (width == bounds.outWidth && height == bounds.outHeight &&
            prepared.length() in 1..CloudPhotoAccess.MAX_PHOTO_BYTES) return prepared.readBytes()
        val decoded = requireNotNull(BitmapFactory.decodeFile(prepared.absolutePath)) { "photo_decode_failed" }
        var scaled: Bitmap? = null
        try {
            scaled = if (decoded.width == width && decoded.height == height) decoded
                else Bitmap.createScaledBitmap(decoded, width, height, true)
            for (quality in listOf(90, 85, 80, 75, 70)) {
                val bytes = ByteArrayOutputStream().use { output ->
                    check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "photo_encode_failed" }
                    output.toByteArray()
                }
                if (bytes.size in 1..CloudPhotoAccess.MAX_PHOTO_BYTES) return bytes
            }
            throw IllegalArgumentException("photo_limit")
        } finally {
            if (scaled !== decoded) scaled?.recycle()
            decoded.recycle()
        }
    }
}
