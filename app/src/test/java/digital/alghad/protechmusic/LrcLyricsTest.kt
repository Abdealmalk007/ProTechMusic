package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class LrcLyricsTest {
    @Test fun parsesTimestampFractionsAndMetadata() {
        val lines = LrcLyrics.parse("\uFEFF[ar:Artist]\n[00:01.5]First\n[01:02.34]Second\n[01:02.345]Third")
        assertEquals(listOf(1500L, 62340L, 62345L), lines.map { it.timeMs })
        assertEquals(listOf("First", "Second", "Third"), lines.map { it.text })
    }

    @Test fun repeatedTimestampsAndInvalidSeconds() {
        val lines = LrcLyrics.parse("[00:09.00][00:01.00]Again\n[00:99.00]Invalid\n[00:02]Next")
        assertEquals(listOf(1000L, 2000L, 9000L), lines.map { it.timeMs })
        assertEquals(2, lines.count { it.text == "Again" })
    }

    @Test fun findsActiveLineByBinarySearch() {
        val lines = LrcLyrics.parse("[00:01.00]One\n[00:02.00]Two\n[00:05.00]Three")
        assertEquals(-1, LrcLyrics.activeIndex(lines, 999))
        assertEquals(0, LrcLyrics.activeIndex(lines, 1000))
        assertEquals(1, LrcLyrics.activeIndex(lines, 3999))
        assertEquals(2, LrcLyrics.activeIndex(lines, 5000))
        assertEquals(-1, LrcLyrics.activeIndex(emptyList(), 5000))
    }

    @Test fun ignoresPlainTextAndEmptyLines() {
        assertTrue(LrcLyrics.parse("[ti:Hello]\nplain text\n[00:00.01]\n[00:00.10] Hello ").size == 1)
    }
}
