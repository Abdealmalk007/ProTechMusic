package digital.alghad.protechmusic

import java.text.Normalizer
import java.util.Locale

/** Pure, deterministic library search; never changes the order of source playlists by default. */
object TrackSearchEngine {
    enum class Order { ORIGINAL, TITLE, ARTIST, SOURCE, RECENTLY_ADDED, DURATION, SIZE }

    fun normalize(raw: String): String {
        val text=Normalizer.normalize(raw,Normalizer.Form.NFKD)
        val result=StringBuilder(text.length)
        for (c in text) {
            when {
                c == '\u0640' -> Unit // Arabic tatweel
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit
                c == '\u0623' || c == '\u0625' || c == '\u0622' || c == '\u0671' -> result.append('\u0627')
                c == '\u0649' -> result.append('\u064A')
                else -> result.append(c.lowercaseChar())
            }
        }
        return result.toString().trim().replace(Regex("\\s+")," ")
    }

    fun search(tracks: List<AudioTrack>, query: String, order: Order = Order.ORIGINAL): List<AudioTrack> {
        val words=normalize(query).split(' ').filter{it.isNotBlank()}
        val filtered=if(words.isEmpty())tracks else tracks.filter { track ->
            val indexed=normalize(track.title+" "+track.artist+" "+track.album+" "+
                track.folder+" "+track.source+" "+track.uri.substringAfterLast('/'))
            words.all { it in indexed }
        }
        return when(order) {
            Order.ORIGINAL -> filtered
            Order.TITLE -> filtered.sortedWith(compareBy<AudioTrack>{normalize(it.title)}.thenBy{it.uri})
            Order.ARTIST -> filtered.sortedWith(compareBy<AudioTrack>{normalize(it.artist)}.thenBy{normalize(it.title)})
            Order.SOURCE -> filtered.sortedWith(compareBy<AudioTrack>{normalize(it.source)}.thenBy{normalize(it.title)})
            Order.RECENTLY_ADDED -> filtered.sortedWith(compareByDescending<AudioTrack>{it.addedAtSeconds}.thenBy{normalize(it.title)})
            Order.DURATION -> filtered.sortedWith(compareByDescending<AudioTrack>{it.durationMs}.thenBy{normalize(it.title)})
            Order.SIZE -> filtered.sortedWith(compareByDescending<AudioTrack>{it.sizeBytes}.thenBy{normalize(it.title)})
        }
    }
}
