package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class PlaybackUiTimelineTest {
    @Test fun validTimelineClampsPositionAndMapsSeek() {
        assertEquals(0.5f, PlaybackUiTimeline.fraction(5000, 10000), 0.0001f)
        assertEquals(0f, PlaybackUiTimeline.fraction(-1, 10000), 0f)
        assertEquals(1f, PlaybackUiTimeline.fraction(20000, 10000), 0f)
        assertEquals(2500L, PlaybackUiTimeline.target(0.25f, 10000))
        assertEquals(10000L, PlaybackUiTimeline.target(2f, 10000))
        assertEquals(7500L, PlaybackUiTimeline.remaining(2500, 10000))
        assertEquals(0L, PlaybackUiTimeline.remaining(20000, 10000))
    }

    @Test fun unknownDurationOrInvalidDragDoesNotSeek() {
        assertEquals(0f, PlaybackUiTimeline.fraction(500, 0), 0f)
        assertNull(PlaybackUiTimeline.target(0.5f, 0))
        assertNull(PlaybackUiTimeline.target(0.5f, -1))
        assertNull(PlaybackUiTimeline.target(-1f, 10000))
        assertNull(PlaybackUiTimeline.target(Float.NaN, 10000))
        assertNull(PlaybackUiTimeline.target(Float.POSITIVE_INFINITY, 10000))
        assertEquals(0L, PlaybackUiTimeline.remaining(1, -1))
    }
}
