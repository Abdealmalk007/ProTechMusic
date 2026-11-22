package digital.alghad.protechmusic

/** Timed lyrics parsed from user-selected local LRC files. */
data class TimedLyric(val timeMs: Long, val text: String)

object LrcLyrics {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): List<TimedLyric> {
        val result = ArrayList<TimedLyric>()
        for (line in raw.removePrefix("\uFEFF").lineSequence().take(5000)) {
            val matches = timestamp.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = timestamp.replace(line, "").trim()
            if (text.isEmpty()) continue
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                if (seconds > 59) continue
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                result.add(TimedLyric((minutes * 60L + seconds) * 1000L + millis, text))
            }
        }
        return result.sortedBy { it.timeMs }
    }

    fun activeIndex(lines: List<TimedLyric>, playbackMs: Long): Int {
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = low + (high - low) / 2
            if (lines[middle].timeMs <= playbackMs) {
                result = middle
                low = middle + 1
            } else high = middle - 1
        }
        return result
    }
}
