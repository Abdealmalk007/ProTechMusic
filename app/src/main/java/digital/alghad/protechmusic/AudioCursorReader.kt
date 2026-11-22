package digital.alghad.protechmusic

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Production MediaStore cursor parser, also exercised with synthetic MatrixCursor stress rows.
 * Emits directly to the existing URI-keyed map to avoid duplicating large libraries.
 */
internal object AudioCursorReader {
    fun appendTo(
        cursor: Cursor,
        collection: Uri,
        output: MutableMap<String, AudioTrack>,
        modern: Boolean = Build.VERSION.SDK_INT >= 29
    ) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val filename = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val pathColumn = cursor.getColumnIndex(
            if (modern) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )

        while (cursor.moveToNext()) {
            val fileSize = cursor.getLong(size)
            if (fileSize <= 0L) continue
            val rawPath = if (pathColumn >= 0) cursor.getString(pathColumn).orEmpty() else ""
            val folder = rawPath.trimEnd('/').substringAfterLast('/', "Other")
            val uri = ContentUris.withAppendedId(collection, cursor.getLong(id)).toString()
            output[uri] = AudioTrack(
                uri = uri,
                title = cursor.getString(title).orEmpty().ifBlank { cursor.getString(filename).orEmpty() },
                artist = cursor.getString(artist).orEmpty().takeUnless { it == "<unknown>" }.orEmpty(),
                album = cursor.getString(album).orEmpty(),
                folder = folder,
                source = SourceClassifier.classify(rawPath),
                durationMs = cursor.getLong(duration),
                sizeBytes = fileSize,
                addedAtSeconds = cursor.getLong(added)
            )
        }
    }
}
