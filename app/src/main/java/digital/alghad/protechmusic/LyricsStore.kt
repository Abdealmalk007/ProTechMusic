package digital.alghad.protechmusic

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

class LyricsStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "lyrics")
    private fun storedFile(trackUri: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(trackUri.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(directory, "$digest.lrc")
    }

    suspend fun importFile(trackUri: String, documentUri: Uri): Int = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(documentUri)
            ?: throw IllegalArgumentException("Selected document is unavailable")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val amount = stream.read(chunk)
                if (amount < 0) break
                require(output.size() + amount <= 512 * 1024) { "Lyrics exceed 512 KiB" }
                output.write(chunk, 0, amount)
            }
            output.toByteArray()
        }
        val lines = LrcLyrics.parse(String(bytes, Charsets.UTF_8))
        require(lines.isNotEmpty()) { "No timestamped lyrics found" }
        check(directory.exists() || directory.mkdirs()) { "Cannot create lyrics directory" }
        val destination = storedFile(trackUri)
        val temp = File.createTempFile("lyrics-", ".tmp", directory)
        try {
            temp.writeBytes(bytes)
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
            }
        } finally {
            temp.delete()
        }
        lines.size
    }

    suspend fun load(trackUri: String): List<TimedLyric> = withContext(Dispatchers.IO) {
        val file = storedFile(trackUri)
        if (!file.isFile || file.length() > 512 * 1024) emptyList()
        else runCatching { LrcLyrics.parse(file.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
    }
}
