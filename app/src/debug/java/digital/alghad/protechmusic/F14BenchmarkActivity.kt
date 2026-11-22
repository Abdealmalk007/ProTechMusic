package digital.alghad.protechmusic

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.provider.MediaStore
import android.database.MatrixCursor
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Debug-build only: production cursor parser exercised with non-destructive synthetic rows. */
class F14BenchmarkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val count = intent.getIntExtra("count", 1000)
        if (count !in listOf(1000, 10000, 50000)) {
            Log.e("ProTechF14", "Invalid size=$count")
            finish()
            return
        }
        Thread({
            try {
                val metrics = benchmark(count)
                File(filesDir, "f14_benchmark_$count.json").writeText(metrics.toString(2))
                Log.i("ProTechF14", "RESULT " + metrics.toString())
            } catch (e: Throwable) {
                File(filesDir, "f14_benchmark_$count.error").writeText(e.stackTraceToString())
                Log.e("ProTechF14", "FAILED $count", e)
            } finally {
                runOnUiThread { finish() }
            }
        }, "F14-MetadataBenchmark").start()
    }

    private fun elapsed(start: Long) = (System.nanoTime() - start) / 1_000_000.0
    private fun heapUsed(): Long {
        val r = Runtime.getRuntime()
        return r.totalMemory() - r.freeMemory()
    }

    private fun benchmark(n: Int): JSONObject {
        val cpuBefore = Process.getElapsedCpuTime()
        val heapBefore = heapUsed()
        val pssBefore = Debug.getPss()
        val names = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH)
        val cursor = MatrixCursor(names, n + 2)
        val rowStart = System.nanoTime()
        for (i in 0 until n) cursor.addRow(arrayOf<Any>(
            (i + 1).toLong(), if (i == n - 1) "BenchmarkNeedle_$n" else "Track $i", "track_$i.mp3",
            "Artist ${i % 29}", "Album ${i % 41}", 45_000L,
            18_000L + i, 1_750_000_000L + i,
            if (i % 2 == 0) "Music/2026/" else "Download/Audio/"
        ))
        cursor.addRow(arrayOf<Any>(n + 1L, "Excluded Empty", "empty.mp3",
            "Artist", "Album", 0L, 0L, 1_750_000_000L, "Music/"))
        cursor.addRow(arrayOf<Any>(n.toLong(), "BenchmarkNeedle_$n", "track_${n - 1}.mp3",
            "Artist ${(n - 1) % 29}", "Album ${(n - 1) % 41}",
            45_000L, 18_000L + n - 1, 1_750_000_000L + n - 1,
            if ((n - 1) % 2 == 0) "Music/2026/" else "Download/Audio/"))
        val rowsMs = elapsed(rowStart)

        val scanned = LinkedHashMap<String,AudioTrack>(n * 4 / 3 + 1)
        val parseStart = System.nanoTime()
        cursor.use { AudioCursorReader.appendTo(
            it, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, scanned, true) }
        val parseMs = elapsed(parseStart)
        check(scanned.size == n) { "Expected $n, got ${scanned.size}" }
        val tracks = scanned.values.toList()
        val indexStart = System.nanoTime()
        val prepared = TrackSearchEngine.prepare(tracks)
        val indexMs = elapsed(indexStart)
        check(prepared.indexedCount == n)
        val searches = JSONArray()
        repeat(5) {
            val start = System.nanoTime()
            val found = prepared.search(tracks, "BenchmarkNeedle_$n")
            searches.put(elapsed(start))
            check(found.size == 1) { "Search should have one match, got ${found.size}" }
        }
        val sortStart = System.nanoTime()
        check(prepared.search(tracks, "", TrackSearchEngine.Order.ARTIST).size == n)
        val sortMs = elapsed(sortStart)
        val realStart = System.nanoTime()
        val actual = runBlocking { AudioRepository(this@F14BenchmarkActivity,
            MusicStore(this@F14BenchmarkActivity)).scan().size }
        val realScanMs = elapsed(realStart)
        return JSONObject()
            .put("type", "synthetic_cursor_stress_not_real_50000_media_files")
            .put("count", n).put("synthetic_rows", n + 2)
            .put("parsed_unique", scanned.size)
            .put("row_generation_ms", rowsMs)
            .put("cursor_parse_ms", parseMs)
            .put("index_build_ms", indexMs)
            .put("search_ms", searches)
            .put("sort_all_ms", sortMs)
            .put("actual_media_count", actual)
            .put("actual_media_scan_ms", realScanMs)
            .put("process_cpu_ms", Process.getElapsedCpuTime() - cpuBefore)
            .put("heap_start_bytes", heapBefore)
            .put("heap_end_bytes", heapUsed())
            .put("pss_start_kb", pssBefore)
            .put("pss_end_kb", Debug.getPss())
            .put("battery_mah", JSONObject.NULL)
            .put("battery_note", "Emulator does not provide calibrated physical battery drain")
    }
}
