package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class AudioEffectMathTest {
    @Test fun gainAtZeroAndHundred() {
        assertEquals(0, AudioEffectMath.loudnessGainMb(0))
        assertEquals(600, AudioEffectMath.loudnessGainMb(100))
    }

    @Test fun gainNeverExceedsSixDb() {
        assertEquals(600, AudioEffectMath.loudnessGainMb(Int.MAX_VALUE))
        assertEquals(0, AudioEffectMath.loudnessGainMb(Int.MIN_VALUE))
        assertEquals(300, AudioEffectMath.loudnessGainMb(50))
    }

    @Test fun bassStrengthBounds() {
        assertEquals(0, AudioEffectMath.bassStrength(-200).toInt())
        assertEquals(1000, AudioEffectMath.bassStrength(300).toInt())
        assertEquals(750, AudioEffectMath.bassStrength(75).toInt())
    }

    @Test fun eqRestrictsBroadHardwareRange() {
        assertEquals(-600..600, AudioEffectMath.equalizerRange(-1500, 1500))
    }

    @Test fun eqHonorsNarrowHardwareRange() {
        assertEquals(-300..500, AudioEffectMath.equalizerRange(-300, 500))
    }

    @Test fun eqRejectsUnsupportedGainWindow() {
        assertNull(AudioEffectMath.equalizerRange(900, 1200))
        assertNull(AudioEffectMath.equalizerRange(-1200, -900))
        assertNull(AudioEffectMath.equalizerRange(200, -200))
    }

    @Test fun eqClampsOutOfRangeRequestedLevel() {
        val window = AudioEffectMath.equalizerRange(-400, 400)!!
        assertEquals(-400, AudioEffectMath.equalizerLevelMb(-600, window).toInt())
        assertEquals(400, AudioEffectMath.equalizerLevelMb(600, window).toInt())
        assertEquals(275, AudioEffectMath.equalizerLevelMb(275, window).toInt())
    }

    @Test fun speedIsBoundedAndFinite() {
        assertEquals(0.5f, AudioEffectMath.playbackSpeed(-10f))
        assertEquals(2f, AudioEffectMath.playbackSpeed(5f))
        assertEquals(1.25f, AudioEffectMath.playbackSpeed(1.25f))
        assertEquals(1f, AudioEffectMath.playbackSpeed(Float.NaN))
        assertEquals(1f, AudioEffectMath.playbackSpeed(Float.POSITIVE_INFINITY))
    }
}
