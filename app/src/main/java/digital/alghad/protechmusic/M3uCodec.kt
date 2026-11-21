package digital.alghad.protechmusic

import java.net.URLDecoder
import java.util.Locale

data class M3uEntry(val location: String, val title: String?, val artist: String?)
data class M3uResolveResult(
    val tracks: List<AudioTrack>,
    val missing: List<String>,
    val ambiguous: List<String>
)

/** SAF-friendly M3U8: no direct filesystem access, remote playlist downloads or inferred permission grants. */
object M3uCodec {
    private const val MAX_CHARS = 2_000_000
    private const val MAX_ENTRIES = 50_000

    fun encode(tracks: List<AudioTrack>): String = buildString {
        require(tracks.size <= MAX_ENTRIES) { "Too many tracks" }
        append("#EXTM3U\n")
        tracks.forEach { track ->
            require(track.uri.startsWith("content://")) { "Unsafe media reference" }
            val title = track.title.replace('\r', ' ').replace('\n', ' ').trim()
            val artist = track.artist.replace('\r', ' ').replace('\n', ' ').trim()
            append("#EXTINF:").append((track.durationMs / 1000L).coerceAtLeast(0L))
            append(',').append(if (artist.isBlank()) title else "$artist - $title").append('\n')
            append(track.uri).append('\n')
        }
    }

    fun parse(text: String): List<M3uEntry> {
        require(text.length <= MAX_CHARS) { "Playlist is too large" }
        val out = ArrayList<M3uEntry>()
        var display: String? = null
        text.removePrefix("\uFEFF").lineSequence().forEach { sourceLine ->
            val line = sourceLine.trim().removeSuffix("\r")
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF:", ignoreCase = true) ->
                    display = line.substringAfter(',', "").trim().takeIf { it.isNotBlank() }
                line.startsWith("#") -> Unit
                else -> {
                    require(out.size < MAX_ENTRIES) { "Too many playlist entries" }
                    val label = display
                    val separator = label?.indexOf(" - ") ?: -1
                    val artist = if (separator > 0) label?.substring(0, separator)?.trim() else null
                    val title = if (separator > 0) label?.substring(separator + 3)?.trim() else label
                    out.add(M3uEntry(line, title, artist))
                    display = null
                }
            }
        }
        return out
    }

    fun resolve(text: String, library: List<AudioTrack>): M3uResolveResult {
        val byUri = library.associateBy { it.uri }
        val byTitle = library.groupBy { key(it.title) }
        val accepted = LinkedHashMap<String, AudioTrack>()
        val missing = ArrayList<String>()
        val ambiguous = ArrayList<String>()
        for (entry in parse(text)) {
            val direct = byUri[entry.location]
            val candidates = if (direct != null) listOf(direct) else {
                val name = entry.title?.takeIf { it.isNotBlank() }
                    ?: filenameStem(entry.location)
                val matches = byTitle[key(name)].orEmpty()
                if (entry.artist.isNullOrBlank()) matches else
                    matches.filter { key(it.artist) == key(entry.artist) }
            }
            when (candidates.size) {
                1 -> accepted.putIfAbsent(candidates.first().uri, candidates.first())
                0 -> missing.add(entry.title ?: entry.location.take(150))
                else -> ambiguous.add(entry.title ?: entry.location.take(150))
            }
        }
        return M3uResolveResult(accepted.values.toList(), missing, ambiguous)
    }

    private fun key(s: String): String = s.trim().lowercase(Locale.ROOT)
    private fun filenameStem(location: String): String {
        val safe = location.substringBefore('?').substringBefore('#')
        val basename = safe.substringAfterLast('/').substringAfterLast('\\')
        val decoded = try { URLDecoder.decode(basename.replace("+", "%2B"), "UTF-8") }
            catch (_: IllegalArgumentException) { basename }
        return decoded.substringBeforeLast('.', decoded).trim()
    }
}
