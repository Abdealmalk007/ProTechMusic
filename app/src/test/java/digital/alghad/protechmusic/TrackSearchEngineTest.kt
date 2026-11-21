package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class TrackSearchEngineTest {
    private val tracks=listOf(
        AudioTrack(uri="content://media/audio/1",title="أغنية الْأمل",artist="أحمد",album="المساء",
            folder="WhatsApp Voice Notes",source="WhatsApp",durationMs=3000,sizeBytes=400,addedAtSeconds=10),
        AudioTrack(uri="content://media/audio/2",title="Echo Sound",artist="Alex",album="Blue Skies",
            folder="Downloads",source="Downloads",durationMs=9000,sizeBytes=700,addedAtSeconds=30),
        AudioTrack(uri="content://media/audio/3",title="يحيى",artist="سناء",album="قصة",folder="Recordings",
            source="Recordings",durationMs=1000,sizeBytes=50,addedAtSeconds=20)
    )

    @Test fun arabicDiacriticsHamzaAlifAndTatweel() {
        assertEquals(tracks[0],TrackSearchEngine.search(tracks,"اغنية الامل").single())
        assertEquals(tracks[0],TrackSearchEngine.search(tracks,"اَحْمَد").single())
        assertEquals(tracks[2],TrackSearchEngine.search(tracks,"يحيي").single())
        assertEquals(TrackSearchEngine.normalize("العربية"),TrackSearchEngine.normalize("الْـعَرَبِيَّة"))
    }

    @Test fun arabicEnglishMixedTermsMatchAcrossFields() {
        assertEquals(tracks[0],TrackSearchEngine.search(tracks,"whatsapp أمل").single())
        assertEquals(tracks[1],TrackSearchEngine.search(tracks,"download blue").single())
        assertEquals(tracks[2],TrackSearchEngine.search(tracks,"recordings سناء").single())
    }

    @Test fun caseAndWhitespaceInsensitive() {
        assertEquals(tracks[1],TrackSearchEngine.search(tracks,"   eCHo    ALEX ").single())
        assertEquals(tracks[1],TrackSearchEngine.search(tracks,"echo").single())
    }

    @Test fun emptyQueryPreservesOriginalPlaylistOrdering() {
        assertEquals(tracks,TrackSearchEngine.search(tracks,""))
        assertEquals(tracks,TrackSearchEngine.search(tracks,"     "))
    }

    @Test fun explicitSortModes() {
        val byDate=TrackSearchEngine.search(tracks,"",TrackSearchEngine.Order.RECENTLY_ADDED)
        assertEquals(listOf(tracks[1],tracks[2],tracks[0]),byDate)
        assertEquals(tracks[1],TrackSearchEngine.search(tracks,"",TrackSearchEngine.Order.DURATION).first())
        assertEquals(tracks[1],TrackSearchEngine.search(tracks,"",TrackSearchEngine.Order.SIZE).first())
        assertEquals(listOf(tracks[1],tracks[2],tracks[0]),
            TrackSearchEngine.search(tracks,"",TrackSearchEngine.Order.SOURCE))
    }

    @Test fun tokenSearchWithoutFalseMatches() {
        assertTrue(TrackSearchEngine.search(tracks,"unknown artist").isEmpty())
        assertTrue(TrackSearchEngine.search(tracks,"echo whatsapp").isEmpty())
        assertEquals(3,TrackSearchEngine.search(tracks,"").size)
    }

    @Test fun largeLibraryTenThousandUsesBoundedWork() {
        val many=(0 until 10_000).map { index ->
            AudioTrack(uri="content://media/external/audio/media/"+index,
                title="Track $index",artist="Artist "+(index%13),album="Album "+(index%17),
                folder="Downloads/2026",source="Downloads",durationMs=30_000,addedAtSeconds=index.toLong())
        }
        val start=System.nanoTime()
        val result=TrackSearchEngine.search(many,"track 9999 downloads")
        val ms=(System.nanoTime()-start)/1_000_000
        assertEquals(1,result.size)
        assertEquals("Track 9999",result.single().title)
        assertTrue("Search exceeded 4 seconds: "+ms+"ms",ms<4_000)
    }
}
