package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class M3uCodecTest {
    private val tracks = listOf(
        AudioTrack("content://media/audio/1", "نشيد عربي", "Artist A", durationMs = 123000),
        AudioTrack("content://media/audio/2", "Another Song", "Artist B", durationMs = 84000),
        AudioTrack("content://media/audio/3", "Another Song", "Artist C", durationMs = 96000)
    )

    @Test fun roundTripWithArabicAndArtist() {
        val exported = M3uCodec.encode(tracks.take(2))
        assertTrue(exported.startsWith("#EXTM3U\n"))
        assertEquals(tracks.take(2), M3uCodec.resolve(exported, tracks).tracks)
    }

    @Test fun missingFilesReportedWithoutGuessing() {
        val data = "#EXTM3U\n#EXTINF:12,Artist A - نشيد عربي\ncontent://old/id\n" +
            "#EXTINF:8,No Artist - Lost Song\nfile:///lost.mp3\n"
        val parsed = M3uCodec.resolve(data, tracks)
        assertEquals(1, parsed.tracks.size)
        assertEquals(1, parsed.missing.size)
        assertEquals("Lost Song", parsed.missing.single())
    }

    @Test fun duplicateTitleDoesNotAutoPickUnrelatedArtist() {
        val data = "#EXTM3U\nAnother Song.mp3\n"
        assertEquals(1, M3uCodec.resolve(data, tracks).ambiguous.size)
        val unique = "#EXTINF:5,Artist C - Another Song\nold/path/song.mp3\n"
        assertEquals(tracks.last(), M3uCodec.resolve(unique, tracks).tracks.single())
    }

    @Test fun uniqueLegacyPathMatchesLibraryTitle() {
        val data = "#EXTM3U\nMusic/%D9%86%D8%B4%D9%8A%D8%AF%20%D8%B9%D8%B1%D8%A8%D9%8A.mp3\n"
        assertEquals(tracks.first(), M3uCodec.resolve(data, tracks).tracks.single())
    }

    @Test fun duplicateEntriesNotImportedTwice() {
        val data = "content://media/audio/1\ncontent://media/audio/1\n"
        assertEquals(1, M3uCodec.resolve(data, tracks).tracks.size)
    }

    @Test fun rejectOversizedContentOrNonContentExport() {
        assertThrows(IllegalArgumentException::class.java) { M3uCodec.parse("x".repeat(2_000_001)) }
        assertThrows(IllegalArgumentException::class.java) {
            M3uCodec.encode(listOf(AudioTrack("http://host/file.mp3", "track")))
        }
    }

    @Test fun headersCommentsAndBomAreAllowed() {
        val s = "\uFEFF#EXTM3U\r\n#EXTINF:32,Artist B - Another Song\r\ncontent://media/audio/2\r\n"
        val out = M3uCodec.resolve(s, tracks)
        assertEquals(tracks[1], out.tracks.single())
    }
}
