package app.metria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudPhotoSizePolicyTest {
    @Test fun sampledPhonePhotoFitsBrokerBoundary() {
        // Local power-of-two sampling may leave this typical camera image at
        // 2016 x 1512. The cloud broker rejects dimensions above 1536.
        assertEquals(1536 to 1152, CloudPhotoSizePolicy.fit(2016, 1512))
        assertEquals(1152 to 1536, CloudPhotoSizePolicy.fit(1512, 2016))
        assertEquals(1536 to 1152, CloudPhotoSizePolicy.fit(4032, 3024))
    }

    @Test fun smallImagesAreNotUpscaled() {
        assertEquals(800 to 600, CloudPhotoSizePolicy.fit(800, 600))
        assertEquals(1536 to 1536, CloudPhotoSizePolicy.fit(1536, 1536))
    }

    @Test fun ExtremeAspectRatiosStayPositiveAndWithinLimit() {
        assertEquals(1 to 1536, CloudPhotoSizePolicy.fit(1, Int.MAX_VALUE))
        for ((width, height) in listOf(1537 to 1000, 3072 to 2048, 2000 to 3000)) {
            val result = CloudPhotoSizePolicy.fit(width, height)
            assertTrue(result.first in 1..1536 && result.second in 1..1536)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDimensionsFailBeforeUpload() {
        CloudPhotoSizePolicy.fit(0, 1024)
    }
}
