package digital.alghad.protechmusic

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Debug-only, emulator-only real MediaStore integration fixture.
 * Inserts exclusively to Music/ProTechMusicQA_F14/, never touches user audio or deletes files.
 */
class F14RealMediaQaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val count = intent.getIntExtra("count", 100)
        val scanOnly = intent.getBooleanExtra("scanOnly", false)
        if (count !in listOf(10, 100, 1_000, 10_000, 50_000) || !isEmulator()) {
            Log.e(TAG, "Rejected: unsupported count or non-emulator device")
            finish()
            return
        }
        // Activity recreation may otherwise start another writer for the same fixture.
        if (!fixtureRunning.compareAndSet(false, true)) {
            Log.i(TAG, "Ignoring duplicate fixture activity while worker is active")
            return
        }
        Thread({
            try {
                val report = runFixture(count, scanOnly)
                File(filesDir, "f14_real_$count.json").writeText(report.toString(2))
                Log.i(TAG, "RESULT " + report.toString())
            } catch (error: Throwable) {
                File(filesDir, "f14_real_$count.error").writeText(error.stackTraceToString())
                Log.e(TAG, "FAILED $count", error)
            } finally {
                fixtureRunning.set(false)
                runOnUiThread { finish() }
            }
        }, "F14-RealMediaStoreQa").start()
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.startsWith("sdk_") || Build.MODEL.contains("Emulator")

    private fun wavFixture(): ByteArray {
        val audioBytes = 2_048
        val result = ByteBuffer.allocate(44 + audioBytes).order(ByteOrder.LITTLE_ENDIAN)
        result.put("RIFF".toByteArray(Charsets.US_ASCII))
        result.putInt(36 + audioBytes)
        result.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        result.putInt(16)
        result.putShort(1) // PCM
        result.putShort(1) // mono
        result.putInt(8_000)
        result.putInt(8_000)
        result.putShort(1)
        result.putShort(8)
        result.put("data".toByteArray(Charsets.US_ASCII))
        result.putInt(audioBytes)
        repeat(audioBytes) { result.put(128.toByte()) }
        return result.array()
    }

    private fun ms(start: Long): Double = (System.nanoTime() - start) / 1_000_000.0

    private fun runFixture(count: Int, scanOnly: Boolean): JSONObject {
        val cpuStart = Process.getElapsedCpuTime()
        val pssStart = Debug.getPss()
        val resolver = contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val folder = "Music/ProTechMusicQA_F14_V22_${count}/"
        val prefix = "ProTechQA_V22_${count}_"
        val current = HashSet<String>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
            "${MediaStore.Audio.Media.RELATIVE_PATH}=?",
            arrayOf(folder),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty()
                if (name.startsWith(prefix)) current.add(name)
            }
        }
        val wav = wavFixture()
        var inserted = 0
        val insertStart = System.nanoTime()
        if (scanOnly) {
            check(current.size == count) { "Scan-only fixture incomplete: expected $count, found ${current.size}" }
        }
        for (i in 0 until if (scanOnly) 0 else count) {
            val name = prefix + i.toString().padStart(5, '0') + ".wav"
            if (name in current) continue
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, folder)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
                put(MediaStore.Audio.Media.TITLE, prefix + i)
            }
            val uri = resolver.insert(collection, values)
                ?: error("MediaStore insertion returned null at $i")
            try {
                val output = resolver.openOutputStream(uri)
                    ?: error("MediaStore openOutputStream returned null at $i")
                output.use { it.write(wav) }
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }, null, null)
                inserted++
            } catch (error: Exception) {
                // Only discard the row inserted by this operation; never touch other media.
                resolver.delete(uri, null, null)
                throw error
            }
        }
        val insertionMs = ms(insertStart)
        var scan: List<AudioTrack> = emptyList()
        var scanMs = 0.0
        var matched: List<AudioTrack> = emptyList()
        val scanSamples = JSONArray()
        // Allow MediaProvider to finalize metadata; record actual times, don't invent.
        for (attempt in 0 until 5) {
            val before = System.nanoTime()
            scan = runBlocking { AudioRepository(this@F14RealMediaQaActivity,
                MusicStore(this@F14RealMediaQaActivity)).scan() }
            scanMs = ms(before)
            scanSamples.put(scanMs)
            matched = scan.filter { it.title.startsWith(prefix) && it.folder == "ProTechMusicQA_F14_V22_${count}" }
            if (matched.size >= count) break
            if (attempt < 4) Thread.sleep(1_000)
        }
        check(matched.size == count) {
            "Real MediaStore scan mismatch: expected $count, found ${matched.size} (total ${scan.size})"
        }
        check(matched.distinctBy { it.uri }.size == count)
        val finalNames = HashSet<String>()
        var rowCount = 0
        resolver.query(collection, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
            "${MediaStore.Audio.Media.RELATIVE_PATH}=?", arrayOf(folder), null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty()
                if (name.startsWith(prefix)) { rowCount++; finalNames.add(name) }
            }
        }
        val expectedNames = (0 until count).map { prefix + it.toString().padStart(5, '0') + ".wav" }.toSet()
        check(rowCount == count && finalNames == expectedNames) { "Fixture filename set mismatch: rows=$rowCount unique=${finalNames.size}" }
        val indexBefore = System.nanoTime()
        val prepared = TrackSearchEngine.prepare(scan)
        val indexMs = ms(indexBefore)
        val target = matched.last()
        val probe = target.title
        val queryBefore = System.nanoTime()
        val found = prepared.search(scan, probe)
        val searchMs = ms(queryBefore)
        check(found.any { it.uri == target.uri }) {
            "Expected unique real file '$probe'; got ${found.size}"
        }
        return JSONObject()
            .put("mode", "ACTUAL_MEDIASTORE_INSERTED_WAV_FILES_API35_EMULATOR")
            .put("count", count)
            .put("scan_only", scanOnly)
            .put("existing_fixture_count", current.size)
            .put("newly_inserted", inserted)
            .put("wav_bytes_each", wav.size)
            .put("media_insert_ms", insertionMs)
            .put("real_scan_ms_last", scanMs)
            .put("scan_samples_ms", scanSamples)
            .put("real_scanned_total", scan.size)
            .put("verified_qa_files", matched.size)
            .put("index_build_ms", indexMs)
            .put("search_ms", searchMs)
            .put("pss_start_kb", pssStart)
            .put("pss_end_kb", Debug.getPss())
            .put("cpu_ms", Process.getElapsedCpuTime() - cpuStart)
            .put("battery_mah", JSONObject.NULL)
            .put("limitation", "Dedicated emulator, actual MediaStore files; not physical battery QA")
    }

    private companion object {
        const val TAG = "ProTechF14Real"
        val fixtureRunning = java.util.concurrent.atomic.AtomicBoolean(false)
    }
}
