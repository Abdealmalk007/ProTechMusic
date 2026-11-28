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
import android.os.SystemClock
import kotlin.math.abs
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
    private fun awaitState(label: String, timeoutMs: Long = 8_000, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            var ok = false
            instrumentation.runOnMainSync { ok = predicate() }
            if (ok) return
            SystemClock.sleep(75)
        }
        error("Timed out: $label")
    }

    private fun verifyTransport(service: MusicService, context: Context, files: MutableList<File>) {
        // Long synthetic PCM fixtures keep transport checks independent of auto-next timing.
        val media = (0..2).map { index ->
            val file = File(context.cacheDir, "protech_qa_autonext_transport_$index.wav")
            val length = 8_000 * 12
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + length)
            header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
            header.putShort(1).putShort(1).putInt(8_000).putInt(8_000)
            header.putShort(1).putShort(8).put("data".toByteArray(Charsets.US_ASCII)).putInt(length)
            file.outputStream().use { it.write(header.array()); it.write(ByteArray(length) { 128.toByte() }) }
            files.add(file)
            AudioTrack(uri = Uri.fromFile(file).toString(), title = "QA Transport $index",
                durationMs = 12_000, sizeBytes = file.length())
        }
        val player = service.player
        fun sameQueue() = service.queueSnapshot().map { it.uri } == media.map { it.uri }
        instrumentation.runOnMainSync {
            service.pause()
            service.playQueue(media, 1)
        }
        awaitState("initial playback") { player.isPlaying && player.currentMediaItemIndex == 1 && player.currentPosition >= 350 }
        repeat(3) { iteration ->
            instrumentation.runOnMainSync { service.pause() }
            awaitState("pause $iteration") { !player.isPlaying }
            var paused = 0L
            instrumentation.runOnMainSync {
                check(sameQueue() && player.currentMediaItemIndex == 1)
                paused = player.currentPosition
            }
            SystemClock.sleep(350)
            instrumentation.runOnMainSync {
                check(abs(player.currentPosition - paused) <= 200) { "Position advanced while paused" }
                service.resume()
            }
            awaitState("resume $iteration") {
                player.isPlaying && player.currentMediaItemIndex == 1 && sameQueue() &&
                    player.currentPosition >= paused + 200
            }
        }
        instrumentation.runOnMainSync { service.pause(); service.seek(6_500) }
        awaitState("absolute seek") { abs(player.currentPosition - 6_500L) < 250 }
        instrumentation.runOnMainSync { service.seekRelative(-2_000) }
        awaitState("relative seek") { abs(player.currentPosition - 4_500L) < 250 }
        instrumentation.runOnMainSync { service.previous() }
        awaitState("previous restarts current") { player.currentMediaItemIndex == 1 && player.currentPosition < 250 }
        instrumentation.runOnMainSync { service.next() }
        awaitState("next") { player.currentMediaItemIndex == 2 && sameQueue() }
        instrumentation.runOnMainSync { service.previous() }
        awaitState("previous item") { player.currentMediaItemIndex == 1 && sameQueue() }
        instrumentation.runOnMainSync { service.seek(-500) }
        awaitState("seek lower clamp") { player.currentPosition == 0L }
    }

    private fun verifyPlaybackModes(service: MusicService, media: List<AudioTrack>) {
        val player = service.player
        val observed = mutableListOf<Pair<Int, Int>>()
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                observed.add(player.currentMediaItemIndex to reason)
            }
        }
        instrumentation.runOnMainSync { player.addListener(listener) }
        try {
            instrumentation.runOnMainSync {
                service.pause()
                service.setShuffle(false)
                while (player.repeatMode != Player.REPEAT_MODE_ONE) service.changeRepeat()
                service.playQueue(media, 1)
                observed.clear()
            }
            awaitState("repeat one actual end transition", 12_000) {
                observed.any { it.first == 1 && it.second == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT }
            }
            instrumentation.runOnMainSync {
                check(player.currentMediaItemIndex == 1)
                service.pause()
                while (player.repeatMode != Player.REPEAT_MODE_ALL) service.changeRepeat()
                service.playQueue(media, media.lastIndex)
                observed.clear()
            }
            awaitState("repeat all wraps last to first", 12_000) {
                observed.any { it.first == 0 && it.second == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO }
            }
            var expected = emptyList<Int>()
            instrumentation.runOnMainSync {
                service.pause()
                while (player.repeatMode != Player.REPEAT_MODE_OFF) service.changeRepeat()
                service.playQueue(media, 0)
                service.pause()
                service.setShuffle(true)
                val timeline = player.currentTimeline
                check(timeline.windowCount == media.size)
                val order = mutableListOf<Int>()
                var index = timeline.getFirstWindowIndex(true)
                while (index != androidx.media3.common.C.INDEX_UNSET) {
                    check(index !in order && index in media.indices)
                    order.add(index)
                    index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
                }
                check(order.size == media.size)
                expected = order
                player.seekTo(order.first(), 0L)
                observed.clear()
                service.resume()
            }
            awaitState("shuffle actual playback to end", 20_000) {
                player.playbackState == Player.STATE_ENDED
            }
            instrumentation.runOnMainSync {
                val actual = listOf(expected.first()) + observed.filter {
                    it.second == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                }.map { it.first }
                check(actual == expected) { "Shuffle order mismatch: expected=$expected actual=$actual" }
                check(service.queueSnapshot().map { it.uri } == media.map { it.uri })
                check(player.playerError == null)
                Log.i("ProTechRcPlayback", "A08 playback shuffle order=$actual")
            }
        } finally {
            instrumentation.runOnMainSync {
                service.pause()
                player.removeListener(listener)
                service.setShuffle(false)
                while (player.repeatMode != Player.REPEAT_MODE_OFF) service.changeRepeat()
            }
        }
    }

    private fun verifyMediaSession(service: MusicService, media: List<AudioTrack>, context: Context) {
        val player = service.player
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        instrumentation.runOnMainSync {
            service.setShuffle(false)
            while (player.repeatMode != Player.REPEAT_MODE_ALL) service.changeRepeat()
            service.playQueue(media, 0)
        }
        awaitState("media notification", 12_000) {
            player.isPlaying && manager.activeNotifications.any { it.id == 109 }
        }
        lateinit var controller: android.media.session.MediaController
        instrumentation.runOnMainSync {
            val notification = manager.activeNotifications.single { it.id == 109 }.notification
            check(notification.actions.map { it.title.toString() } == listOf("Previous", "Pause", "Next"))
            val token = if (Build.VERSION.SDK_INT >= 33) {
                notification.extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                notification.extras.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
            } ?: error("Missing MediaSession token")
            controller = android.media.session.MediaController(context, token)
            check(controller.packageName == context.packageName)
            controller.transportControls.pause()
        }
        awaitState("session pause") {
            !player.isPlaying && controller.playbackState?.state == android.media.session.PlaybackState.STATE_PAUSED
        }
        instrumentation.runOnMainSync {
            val n = manager.activeNotifications.single { it.id == 109 }.notification
            check(n.actions[1].title.toString() == "Play")
            n.actions[1].actionIntent.send()
        }
        awaitState("notification Play pending intent") {
            player.isPlaying && controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        }
        instrumentation.runOnMainSync { controller.transportControls.pause() }
        awaitState("session paused again") { !player.isPlaying }
        var nextIndex = 0
        instrumentation.runOnMainSync {
            nextIndex = (player.currentMediaItemIndex + 1) % media.size
            controller.transportControls.skipToNext()
        }
        awaitState("session next") {
            player.currentMediaItemIndex == nextIndex &&
                controller.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) == media[nextIndex].title
        }
        instrumentation.runOnMainSync {
            controller.transportControls.seekTo(1_000L)
        }
        awaitState("session seek") { abs(player.currentPosition - 1_000L) < 250 }
        instrumentation.runOnMainSync {
            service.pause()
            while (player.repeatMode != Player.REPEAT_MODE_OFF) service.changeRepeat()
        }
    }

    fun run() {
        val out = Bundle()
        val context = instrumentation.targetContext
        val files = ArrayList<File>()
        var connection: ServiceConnection? = null
        var bound = false
        var service: MusicService? = null
        var activity: Activity? = null
        try {
            check(Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.startsWith("sdk_")) {
                "Playback instrumentation refuses physical device"
            }
            // Instrumentation can restart the target process, invalidating an earlier adb launch.
            val visibleActivity = instrumentation.startActivitySync(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            activity = visibleActivity
            instrumentation.waitForIdleSync()
            awaitState("foreground activity for audio focus", 15_000) {
                !visibleActivity.isDestroyed && visibleActivity.hasWindowFocus()
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
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("ProTechRcPlayback", "Player error: " + error.errorCodeName, error)
                    }
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
            val transitionSnapshot = synchronized(transitions) { transitions.toList() }
            var diagnostic = ""
            instrumentation.runOnMainSync {
                val p = playingService.player
                diagnostic = "state=${p.playbackState}, playWhenReady=${p.playWhenReady}, isPlaying=${p.isPlaying}, suppression=${p.playbackSuppressionReason}, position=${p.currentPosition}, duration=${p.duration}, playerError=${p.playerError?.errorCodeName}, windowFocus=${visibleActivity.hasWindowFocus()}"
            }
            Log.i("ProTechRcPlayback", "Auto-next diagnostics: $diagnostic")
            check(gotNext && transitionSnapshot.indexOf(1) != -1 &&
                transitionSnapshot.indexOf(2) > transitionSnapshot.indexOf(1)) {
                "Automatic next/ordered transition failed: $transitionSnapshot; $diagnostic"
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
            verifyTransport(playingService, context, files)
            verifyPlaybackModes(playingService, media)
            verifyMediaSession(playingService, media, context)
            out.putString("stream", "RC_PLAYBACK_PASS: auto-next 0->1->2; repeat OFF/ONE/ALL; shuffle ON/OFF; TRANSPORT_PASS pause/resume x3, seek, next, previous; PLAYBACK_MODES_PASS repeat one actual, repeat all wrap, shuffle actual order; MEDIA_SESSION_PASS notification token, actions, pause/play, next, seek")
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
            activity?.runOnUiThread { activity?.finish() }
            for (file in files) {
                if (file.name.startsWith("protech_qa_autonext_") &&
                    file.parentFile == context.cacheDir) {
                    file.delete()
                }
            }
        }
    }
}
