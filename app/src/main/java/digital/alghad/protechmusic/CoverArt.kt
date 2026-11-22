package digital.alghad.protechmusic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

object CoverArt {
    /** Decode bounded embedded artwork off the UI thread. Null is a normal no-artwork state. */
    fun read(context: Context, trackUri: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(trackUri))
            val bytes = retriever.embeddedPicture ?: return null
            if (bytes.size > 3 * 1024 * 1024) return null
            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dimensions)
            var sampleSize = 1
            while (dimensions.outWidth / sampleSize > 400 || dimensions.outHeight / sampleSize > 400) {
                sampleSize *= 2
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
