package digital.alghad.protechmusic

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runs on disposable emulators only. Validates Media3 automatic queue transitions,
 * repeat and shuffle controls using locally generated, playable PCM WAV fixtures.
 */
internal class RcPlaybackHarness(private val instrumentation: Instrumentation) {
    fun run() {
        val out = Bundle()
        val context = instrumentation.targetContext
        val files = ArrayList<File>()
        var connection: ServiceConnection? = null
        var bound = false
        var service: MusicService? = null
        try {
            check(Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.startsWith("sdk_")) {
                "Playback instrumentation refuses physical device"
            }
            repeat(3) { index ->
                val file = File(context.cacheDir, "protech_qa_autonext_$index.wav")
                file.outputStream().use { output ->
                    val sampleCount = 8_000 * 2
                    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                    header.put("RIFF".toByteArray(Charsets.US_ASCII))
                    header.putInt(36 + sampleCount)
                    header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
                    header.putInt(16)
                    header.putShort(1)
                    header.putShort(1)
                    header.putInt(8_000)
                    header.putInt(8_000)
                    header.putShort(1)
                    header.putShort(8)
                    header.put("data".toByteArray(Charsets.US_ASCII))
                    header.putInt(sampleCount)
                    output.write(header.array())
                    output.write(ByteArray(sampleCount) { 128.toByte() })
                }
                files.add(file)
            }
            val ready = CountDownLatch(1)
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    service = (binder as MusicService.LocalBinder).service
                    ready.countDown()
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                }
            }
            bound = context.bindService(
                Intent(context, MusicService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
            check(bound && ready.await(10, TimeUnit.SECONDS)) {
                "MusicService binding timed out"
            }
            val playingService = service ?: error("MusicService unavailable")
            val media = files.mapIndexed { index, file ->
                AudioTrack(
                    uri = Uri.fromFile(file).toString(),
                    title = "QA Auto Next $index",
                    durationMs = 2_000,
                    sizeBytes = file.length()
                )
            }
            val transitions = Collections.synchronizedList(mutableListOf<Int>())
            val arrived = CountDownLatch(2)
            instrumentation.runOnMainSync {
                val player = playingService.player
                check(player.mediaItemCount >= 0)
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                playingService.setShuffle(false)
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                        val index = player.currentMediaItemIndex
                        transitions.add(index)
                        if (index == 1 || index == 2) arrived.countDown()
                    }
                })
                playingService.playQueue(media)
                check(playingService.queueSnapshot().map { it.uri } == media.map { it.uri }) {
                    "Initial queue contents differ from source order"
                }
            }
            val gotNext = arrived.await(16, TimeUnit.SECONDS)
            val transitionSnapshot = transitions.toList()
            check(gotNext && transitionSnapshot.indexOf(1) != -1 &&
                transitionSnapshot.indexOf(2) > transitionSnapshot.indexOf(1)) {
                "Automatic next/ordered transition failed: $transitionSnapshot"
            }
            val controlOk = booleanArrayOf(false)
            instrumentation.runOnMainSync {
                val player = playingService.player
                val saved = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
                playingService.changeRepeat()
                val single = player.repeatMode == Player.REPEAT_MODE_ONE &&
                    saved.getInt("repeat", -1) == Player.REPEAT_MODE_ONE
                playingService.changeRepeat()
                val all = player.repeatMode == Player.REPEAT_MODE_ALL &&
                    saved.getInt("repeat", -1) == Player.REPEAT_MODE_ALL
                playingService.changeRepeat()
                val off = player.repeatMode == Player.REPEAT_MODE_OFF &&
                    saved.getInt("repeat", -1) == Player.REPEAT_MODE_OFF
                playingService.setShuffle(true)
                val enabled = player.shuffleModeEnabled
                playingService.setShuffle(false)
                controlOk[0] = single && all && off && enabled && !player.shuffleModeEnabled
            }
            check(controlOk[0]) { "Shuffle/repeat control state mismatch" }
            out.putString("stream", "RC_PLAYBACK_PASS: auto-next 0->1->2; repeat OFF/ONE/ALL; shuffle ON/OFF")
            out.putString("transitions", transitionSnapshot.joinToString(","))
            instrumentation.sendStatus(0, out)
            instrumentation.finish(Activity.RESULT_OK, out)
        } catch (error: Throwable) {
            Log.e("ProTechRcPlayback", "Playback QA failed", error)
            out.putString("stream", "RC_PLAYBACK_FAIL: " + error.stackTraceToString())
            instrumentation.sendStatus(-1, out)
            instrumentation.finish(Activity.RESULT_CANCELED, out)
        } finally {
            val active = service
            if (active != null) {
                try {
                    instrumentation.runOnMainSync {
                        active.pause()
                        active.player.stop()
                        active.player.clearMediaItems()
                    }
                } catch (_: Exception) { }
            }
            if (bound && connection != null) {
                try { context.unbindService(connection) } catch (_: Exception) { }
            }
            for (file in files) {
                if (file.name.startsWith("protech_qa_autonext_") &&
                    file.parentFile == context.cacheDir) {
                    file.delete()
                }
            }
        }
    }
}
