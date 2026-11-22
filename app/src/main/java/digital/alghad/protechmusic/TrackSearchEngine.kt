package digital.alghad.protechmusic

import java.text.Normalizer
import java.util.Locale

/** Pure, deterministic library search; no playlist-order changes unless a sort is selected. */
object TrackSearchEngine {
    enum class Order { ORIGINAL, TITLE, ARTIST, SOURCE, RECENTLY_ADDED, DURATION, SIZE }

    // Precompiled once; allocating Regex for every field dominated large-library indexing.
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String {
        // NFKD does not change printable ASCII. Handle this overwhelmingly common case
        // without Unicode decomposition, intermediate strings or regular expressions.
        // Non-ASCII/control input retains the original Unicode normalization semantics.
        if (raw.all { it.code in 32..126 }) {
            val output = StringBuilder(raw.length)
            var spacePending = false
            for (char in raw) {
                if (char == ' ') {
                    if (output.isNotEmpty()) spacePending = true
                } else {
                    if (spacePending) output.append(' ')
                    output.append(char.lowercaseChar())
                    spacePending = false
                }
            }
            return output.toString()
        }
        val text = Normalizer.normalize(raw, Normalizer.Form.NFKD)
        val result = StringBuilder(text.length)
        for (c in text) {
            when {
                c == '\u0640' -> Unit
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit
                c == '\u0623' || c == '\u0625' || c == '\u0622' || c == '\u0671' -> result.append('\u0627')
                c == '\u0649' -> result.append('\u064A')
                else -> result.append(c.lowercaseChar())
            }
        }
        return result.toString().trim().replace(whitespace, " ")
    }

    /**
     * Immutable, per-snapshot index. Created once in remember(songs); not stored on disk.
     * Search/artist/title/source strings are normalized once instead of on every keystroke.
     */
    class PreparedLibrary internal constructor(tracks: List<AudioTrack>) {
        private data class Keys(val text: String, val title: String, val artist: String, val source: String)

        private val byUri: Map<String, Keys> = HashMap<String, Keys>(tracks.size.coerceAtLeast(16) * 4 / 3 + 1).apply {
            for (track in tracks) {
                put(track.uri, Keys(
                    text = normalize(track.title + " " + track.artist + " " + track.album + " " +
                        track.folder + " " + track.source + " " + track.uri.substringAfterLast('/')),
                    title = normalize(track.title),
                    artist = normalize(track.artist),
                    source = normalize(track.source)
                ))
            }
        }

        val indexedCount: Int get() = byUri.size

        fun search(tracks: List<AudioTrack>, query: String, order: Order = Order.ORIGINAL): List<AudioTrack> {
            val words = normalize(query).split(' ').filter { it.isNotBlank() }
            val filtered = if (words.isEmpty()) tracks else tracks.filter { track ->
                val key = byUri[track.uri]?.text ?: searchableText(track)
                words.all { it in key }
            }
            return sort(filtered, order,
                titleOf = { byUri[it.uri]?.title ?: normalize(it.title) },
                artistOf = { byUri[it.uri]?.artist ?: normalize(it.artist) },
                sourceOf = { byUri[it.uri]?.source ?: normalize(it.source) })
        }
    }

    fun prepare(tracks: List<AudioTrack>): PreparedLibrary = PreparedLibrary(tracks)

    private fun searchableText(track: AudioTrack): String =
        normalize(track.title + " " + track.artist + " " + track.album + " " +
            track.folder + " " + track.source + " " + track.uri.substringAfterLast('/'))

    /** Legacy stateless API retained for callers with ad-hoc subsets and for parity tests. */
    fun search(tracks: List<AudioTrack>, query: String, order: Order = Order.ORIGINAL): List<AudioTrack> {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        val filtered = if (words.isEmpty()) tracks else tracks.filter { track ->
            val indexed = searchableText(track)
            words.all { it in indexed }
        }
        return sort(filtered, order, { normalize(it.title) }, { normalize(it.artist) }, { normalize(it.source) })
    }

    private fun sort(
        tracks: List<AudioTrack>, order: Order,
        titleOf: (AudioTrack) -> String,
        artistOf: (AudioTrack) -> String,
        sourceOf: (AudioTrack) -> String
    ): List<AudioTrack> = when (order) {
        Order.ORIGINAL -> tracks
        Order.TITLE -> tracks.sortedWith(compareBy<AudioTrack> { titleOf(it) }.thenBy { it.uri })
        Order.ARTIST -> tracks.sortedWith(compareBy<AudioTrack> { artistOf(it) }.thenBy { titleOf(it) })
        Order.SOURCE -> tracks.sortedWith(compareBy<AudioTrack> { sourceOf(it) }.thenBy { titleOf(it) })
        Order.RECENTLY_ADDED -> tracks.sortedWith(compareByDescending<AudioTrack> { it.addedAtSeconds }.thenBy { titleOf(it) })
        Order.DURATION -> tracks.sortedWith(compareByDescending<AudioTrack> { it.durationMs }.thenBy { titleOf(it) })
        Order.SIZE -> tracks.sortedWith(compareByDescending<AudioTrack> { it.sizeBytes }.thenBy { titleOf(it) })
    }
}
