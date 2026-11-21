package digital.alghad.protechmusic

/**
 * Hardware-independent safety bounds for optional audio-session effects.
 * Values are controls, not a guarantee that the device implements an effect.
 */
internal object AudioEffectMath {
    const val MAX_GAIN_MB = 600
    const val MAX_BASS_STRENGTH = 1000

    fun loudnessGainMb(percent: Int): Int = percent.coerceIn(0, 100) * 6
    fun bassStrength(percent: Int): Short = (percent.coerceIn(0, 100) * 10).toShort()

    fun equalizerRange(deviceMinMb: Int, deviceMaxMb: Int): IntRange? {
        val low = maxOf(deviceMinMb, -MAX_GAIN_MB)
        val high = minOf(deviceMaxMb, MAX_GAIN_MB)
        return if (low <= high) low..high else null
    }

    fun equalizerLevelMb(requestedMb: Int, safeRange: IntRange): Short =
        requestedMb.coerceIn(safeRange.first, safeRange.last).toShort()

    fun playbackSpeed(requested: Float): Float =
        if (requested.isFinite()) requested.coerceIn(0.5f, 2.0f) else 1.0f
}

internal data class AudioEffectsSupport(
    val equalizer: Boolean,
    val bass: Boolean,
    val loudness: Boolean
)
