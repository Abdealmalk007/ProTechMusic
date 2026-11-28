package digital.alghad.protechmusic

/** Pure timeline mapping; invalid durations and non-finite input never seek. */
internal object PlaybackUiTimeline {
    fun fraction(position: Long, duration: Long): Float =
        if (duration <= 0L) 0f else
            (position.coerceIn(0L, duration).toDouble() / duration.toDouble()).toFloat()

    fun target(fraction: Float, duration: Long): Long? {
        if (duration <= 0L || !fraction.isFinite() || fraction < 0f) return null
        return (fraction.coerceAtMost(1f).toDouble() * duration.toDouble())
            .toLong().coerceIn(0L, duration)
    }

    fun remaining(position: Long, duration: Long): Long =
        if (duration <= 0L) 0L else duration - position.coerceIn(0L, duration)
}
