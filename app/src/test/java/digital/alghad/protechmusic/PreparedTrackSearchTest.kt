package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class PreparedTrackSearchTest {
    private fun sample(n: Int) = (0 until n).map { i ->
        AudioTrack(
            uri = "content://media/external/audio/media/$i",
            title = if (i % 71 == 0) "أغنية الأمل $i" else "Track $i",
            artist = "Artist ${i % 29}",
            album = "Album ${i % 47}",
            folder = if (i % 2 == 0) "WhatsApp Voice Notes" else "Downloads/2026",
            source = if (i % 2 == 0) "WhatsApp" else "Downloads",
            durationMs = (i % 135 + 10) * 1000L,
            sizeBytes = i * 100L,
            addedAtSeconds = i.toLong()
        )
    }

    @Test fun cachedResultsMatchUncachedForEveryOrderAndSubset() {
        val all = sample(303)
        val prepared = TrackSearchEngine.prepare(all)
        assertEquals(all.size, prepared.indexedCount)
        val subset = all.filter { it.source == "WhatsApp" }
        for (search in listOf("", "track", "whatsapp", "اغنية الامل", "artist 20", "missing")) {
            for (order in TrackSearchEngine.Order.values()) {
                assertEquals("search=$search order=$order all",
                    TrackSearchEngine.search(all, search, order), prepared.search(all, search, order))
                assertEquals("search=$search order=$order subset",
                    TrackSearchEngine.search(subset, search, order), prepared.search(subset, search, order))
            }
        }
    }

    @Test fun indexIsBoundedToItsSnapshotAndNeverMutatesSource() {
        val original = sample(100)
        val snapshot = original.toList()
        val prepared = TrackSearchEngine.prepare(original)
        assertSame(original, prepared.search(original, ""))
        assertEquals(snapshot, original)
        val adHoc = AudioTrack(uri = "content://test/new", title = "New Name")
        assertEquals(listOf(adHoc), prepared.search(listOf(adHoc), "new"))
        assertTrue(prepared.search(original, "unrelated", TrackSearchEngine.Order.TITLE).isEmpty())
    }

    @Test fun measuredOneThousandTenThousandFiftyThousandCorrectnessAndTiming() {
        val runtime = Runtime.getRuntime()
        for (n in listOf(1_000, 10_000, 50_000)) {
            val tracks = sample(n)
            val heapBefore = runtime.totalMemory() - runtime.freeMemory()
            val startIndex = System.nanoTime()
            val index = TrackSearchEngine.prepare(tracks)
            val indexMs = (System.nanoTime() - startIndex) / 1_000_000.0
            assertEquals(n, index.indexedCount)
            val query = "track ${n - 1} downloads"
            val startCached = System.nanoTime()
            val found = index.search(tracks, query)
            val cachedMs = (System.nanoTime() - startCached) / 1_000_000.0
            val startSort = System.nanoTime()
            val sorted = index.search(tracks, "", TrackSearchEngine.Order.ARTIST)
            val sortMs = (System.nanoTime() - startSort) / 1_000_000.0
            val startLegacy = System.nanoTime()
            val legacy = TrackSearchEngine.search(tracks, query)
            val legacyMs = (System.nanoTime() - startLegacy) / 1_000_000.0
            val heapAfter = runtime.totalMemory() - runtime.freeMemory()
            assertEquals(1, found.size)
            assertEquals("Track ${n-1}", found[0].title)
            assertEquals(found, legacy)
            assertEquals(n, sorted.size)
            assertTrue("Index non-finite", indexMs >= 0 && indexMs.isFinite())
            assertTrue("Search non-finite", cachedMs >= 0 && cachedMs.isFinite())
            println("F14_JVM_BENCH size=$n prepare_ms=%.3f cached_search_ms=%.3f cached_sort_ms=%.3f legacy_search_ms=%.3f heap_used_delta_bytes=%d".format(
                java.util.Locale.US, indexMs, cachedMs, sortMs, legacyMs, heapAfter-heapBefore))
        }
    }
}
