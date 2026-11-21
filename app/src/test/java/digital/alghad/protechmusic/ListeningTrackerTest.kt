package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class ListeningTrackerTest {
    @Test fun skipWithoutAudibleTimeIsNotCounted() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("one",30_000,0,true)
        t.switchTo("two",30_000,300,true)
        t.end(400)
        assertTrue(entries.isEmpty())
    }
    @Test fun shortVoiceNoteCountsWhenHalfPlayed() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("voice",1000,0,true)
        t.tick(749)
        assertTrue(entries.isEmpty())
        t.tick(800)
        t.tick(900)
        assertEquals(listOf("voice"),entries)
    }
    @Test fun longAudioCapsThresholdAtFifteenSeconds() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("long",200_000,0,true)
        t.tick(14_999)
        assertTrue(entries.isEmpty())
        t.tick(15_100)
        assertEquals(listOf("long"),entries)
    }
    @Test fun pauseResumeSumsOnlyAudibleSegments() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("a",10_000,0,true)
        t.playingChanged(false,2_100)
        t.tick(90_000)
        assertTrue(entries.isEmpty())
        t.playingChanged(true,100_000)
        t.tick(102_900)
        assertEquals(listOf("a"),entries)
    }
    @Test fun changeTrackQualifiesPreviousAndFreshPlayback() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("first",2_000,0,true)
        t.switchTo("second",2_000,1_100,true)
        t.end(2_250)
        assertEquals(listOf("first","second"),entries)
    }
    @Test fun repeatSameTrackCountsIndependentRuns() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("x",1000,0,true)
        t.switchTo("x",1000,1000,true)
        t.end(2000)
        assertEquals(listOf("x","x"),entries)
    }
    @Test fun unknownDurationMustReachFifteenSeconds() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.switchTo("stream",0,0,true)
        t.tick(14_000)
        assertTrue(entries.isEmpty())
        t.end(15_001)
        assertEquals(listOf("stream"),entries)
    }
    @Test fun noCurrentTrackIsNotRecorded() {
        val entries=mutableListOf<String>()
        val t=ListeningTracker(entries::add)
        t.playingChanged(true,0)
        t.tick(30000)
        t.end(30000)
        assertTrue(entries.isEmpty())
    }
}
