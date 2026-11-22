package digital.alghad.protechmusic

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AudioTrack(
    val uri: String, val title: String, val artist: String = "",
    val album: String = "", val folder: String = "", val source: String = "Other",
    val durationMs: Long = 0L, val sizeBytes: Long = 0L,
    val addedAtSeconds: Long = 0L
)

object SourceClassifier {
    fun classify(path: String): String {
        val x = path.replace('\\','/').lowercase(Locale.ROOT)
        return when {
            "whatsapp" in x || "com.whatsapp" in x -> "WhatsApp"
            "telegram" in x -> "Telegram"
            "download/" in x || "downloads/" in x || x.startsWith("download") -> "Downloads"
            "recording" in x || "recorder/" in x || "voice recorder" in x -> "Recordings"
            "bluetooth" in x -> "Bluetooth"
            "podcast" in x -> "Podcasts"
            "audiobook" in x -> "Audiobooks"
            "music/" in x || x.startsWith("music") -> "Music"
            "notification" in x || "ringtone" in x || "alarm/" in x -> "System sounds"
            "instagram" in x -> "Instagram"
            "signal" in x -> "Signal"
            "snapchat" in x -> "Snapchat"
            "messenger" in x -> "Messenger"
            "android/media/" in x -> "App: " + x.substringAfter("android/media/").substringBefore('/').take(45)
            else -> "Other"
        }
    }
}

class AudioRepository(private val context: Context, private val store: MusicStore) {
    suspend fun scan(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String,AudioTrack>()
        val collection = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = arrayListOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= 29) columns.add(MediaStore.Audio.Media.RELATIVE_PATH)
        else columns.add(MediaStore.Audio.Media.DATA)
        try {
            context.contentResolver.query(collection,columns.toTypedArray(),null,null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
                AudioCursorReader.appendTo(cursor, collection, found)
            }
        } catch (_: SecurityException) {
            // Permission may be denied or revoked; SAF imports below still remain accessible.
        } catch (_: Exception) {
            // Some device vendors expose partial or temporarily unavailable volumes.
        }
        for (doc in store.importedTracks()) {
            try {
                context.contentResolver.openFileDescriptor(Uri.parse(doc.uri),"r")?.use {
                    found.putIfAbsent(doc.uri,doc)
                }
            } catch (_: Exception) { /* revoked or moved SAF document */ }
        }
        found.values.toList()
    }

    fun importDocument(uri: Uri): AudioTrack? {
        val resolver=context.contentResolver
        val type=resolver.getType(uri).orEmpty()
        if(type.isNotEmpty() && !type.startsWith("audio/") && type!="application/ogg") return null
        try { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: Exception) { /* temporary Uri can still be used in current session */ }
        var title="Audio"
        var size=0L
        resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)?.use{
            if(it.moveToFirst()){
                title=it.getString(0) ?: "Audio"
                if(!it.isNull(1)) size=it.getLong(1)
            }
        }
        var duration=0L
        try {
            val retriever=MediaMetadataRetriever()
            retriever.setDataSource(context,uri)
            duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?:0L
            retriever.release()
        } catch (_: Exception){}
        val track=AudioTrack(uri.toString(),title.substringBeforeLast('.',title),"","",
            "Manual import","Imported",duration,size,System.currentTimeMillis()/1000L)
        store.saveImported(track)
        return track
    }
}
