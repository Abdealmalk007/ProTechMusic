package digital.alghad.protechmusic

import android.app.*
import android.content.*
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MusicRuntime {
    var nowPlaying by mutableStateOf<AudioTrack?>(null)
    var playing by mutableStateOf(false)
    var position by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(0)
    var boost by mutableStateOf(0)
    var bass by mutableStateOf(0)
    var speed by mutableStateOf(1f)
    var equalizerEnabled by mutableStateOf(false)
    var eqRevision by mutableIntStateOf(0)
    var queueRevision by mutableIntStateOf(0)
    var statsRevision by mutableIntStateOf(0)
}

data class EqBandInfo(val index:Int,val centerHz:Int,val minMb:Int,val maxMb:Int,val levelMb:Int)

class MusicService : Service() {
    inner class LocalBinder : Binder() { val service get() = this@MusicService }
    private val binder=LocalBinder()
    lateinit var player:ExoPlayer
        private set
    private lateinit var session:MediaSession
    private val clock=Handler(Looper.getMainLooper())
    private var eq:Equalizer?=null
    private var bassEffect:BassBoost?=null
    private var loudness:LoudnessEnhancer?=null
    private var tracks:List<AudioTrack> = emptyList()
    private val preferences by lazy {getSharedPreferences("playback",MODE_PRIVATE)}
    private val store by lazy{MusicStore(applicationContext)}
    private val statsScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val listening=ListeningTracker{uri ->
        val now=System.currentTimeMillis()
        statsScope.launch {
            try{
                store.recordQualifiedPlay(uri,now)
                withContext(Dispatchers.Main){MusicRuntime.statsRevision++}
            }catch(_:Exception){}
        }
    }
    private var sleepUntil=0L
    private val tick=object:Runnable {
        override fun run() {
            if(::player.isInitialized) {
                MusicRuntime.position=player.currentPosition.coerceAtLeast(0L)
                MusicRuntime.duration=player.duration.takeIf{it>0} ?: MusicRuntime.nowPlaying?.durationMs ?: 0L
                listening.tick(SystemClock.elapsedRealtime())
                if(sleepUntil>0L && SystemClock.elapsedRealtime()>=sleepUntil){
                    sleepUntil=0L; pause()
                }
            }
            clock.postDelayed(this,500L)
        }
    }
    override fun onCreate() {
        super.onCreate()
        val attrs=AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA).build()
        player=ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(attrs,true)
            setHandleAudioBecomingNoisy(true)
            addListener(object:Player.Listener {
                override fun onIsPlayingChanged(isPlaying:Boolean) {
                    listening.playingChanged(isPlaying,SystemClock.elapsedRealtime())
                    MusicRuntime.playing=isPlaying
                    updateSession();showNotification(isPlaying)
                }
                override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int) {
                    val track=tracks.getOrNull(currentMediaItemIndex)
                    listening.switchTo(track?.uri,track?.durationMs?:0L,SystemClock.elapsedRealtime(),isPlaying)
                    MusicRuntime.nowPlaying=track
                    updateSession();showNotification(isPlaying)
                    remember()
                }
                override fun onPlaybackStateChanged(playbackState:Int) {
                    // ExoPlayer advances to the next item in the selected queue.
                    updateSession()
                    if(playbackState==Player.STATE_ENDED){
                        listening.end(SystemClock.elapsedRealtime())
                        remember()
                    }
                }
                override fun onAudioSessionIdChanged(audioSessionId:Int) {
                    attachEffects(audioSessionId)
                }
            })
        }
        session=MediaSession(this,"ProTechMusic").apply{
            setCallback(object:MediaSession.Callback(){
                override fun onPlay()=resume()
                override fun onPause()=pause()
                override fun onSkipToNext()=next()
                override fun onSkipToPrevious()=previous()
                override fun onSeekTo(pos:Long)=seek(pos)
                override fun onStop()=pause()
            })
            isActive=true
        }
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("playback","Music playback",NotificationManager.IMPORTANCE_LOW))
        clock.post(tick)
        attachEffects(player.audioSessionId)
    }

    override fun onBind(intent:Intent?):IBinder=binder
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        when(intent?.action) {
            "PLAY" -> resume()
            "PAUSE" -> pause()
            "NEXT" -> next()
            "PREVIOUS" -> previous()
            "STOP" -> {pause();stopSelf()}
        }
        return START_STICKY
    }

    fun playQueue(newTracks:List<AudioTrack>,startIndex:Int=0){
        if(newTracks.isEmpty())return
        tracks=newTracks
        player.setMediaItems(tracks.map { MediaItem.fromUri(it.uri) }, startIndex.coerceIn(0,tracks.lastIndex),0L)
        player.prepare()
        player.play()
        MusicRuntime.nowPlaying=tracks.getOrNull(startIndex)
        MusicRuntime.queueRevision++
        showNotification(true)
        remember()
    }
    fun removeDeletedMedia(uris:Set<String>) {
        if(uris.isEmpty())return
        for(index in tracks.indices.reversed()) {
            if(tracks[index].uri in uris) {
                player.removeMediaItem(index)
                tracks=tracks.toMutableList().apply{removeAt(index)}
            }
        }
        MusicRuntime.nowPlaying=tracks.getOrNull(player.currentMediaItemIndex)
        MusicRuntime.queueRevision++
        remember()
    }
    fun queueSnapshot(): List<AudioTrack> = tracks.toList()
    fun queueCurrentIndex(): Int = player.currentMediaItemIndex
    fun playNext(track: AudioTrack) {
        if (tracks.isEmpty()) { playQueue(listOf(track)); return }
        val position=(player.currentMediaItemIndex+1).coerceIn(0,tracks.size)
        player.addMediaItem(position,MediaItem.fromUri(track.uri))
        tracks=tracks.toMutableList().apply { add(position,track) }
        MusicRuntime.queueRevision++
        remember()
    }
    fun enqueue(track: AudioTrack) {
        if (tracks.isEmpty()) { playQueue(listOf(track)); return }
        player.addMediaItem(MediaItem.fromUri(track.uri))
        tracks=tracks+track
        MusicRuntime.queueRevision++
        remember()
    }
    fun queueMove(index: Int, direction: Int) {
        val target=index+direction
        if(index !in tracks.indices || target !in tracks.indices)return
        player.moveMediaItem(index,target)
        tracks=tracks.toMutableList().apply { add(target,removeAt(index)) }
        MusicRuntime.queueRevision++
        remember()
    }
    fun queueRemove(index: Int) {
        if(index !in tracks.indices)return
        player.removeMediaItem(index)
        tracks=tracks.toMutableList().apply { removeAt(index) }
        MusicRuntime.queueRevision++
        remember()
    }
    fun queuePlay(index: Int) {
        if(index in tracks.indices) {player.seekTo(index,0L);player.play();remember()}
    }
    fun resume(){if(player.mediaItemCount>0)player.play()}
    fun pause(){if(::player.isInitialized)player.pause();remember()}
    fun next(){if(player.hasNextMediaItem())player.seekToNextMediaItem() else if (MusicRuntime.repeat==2) player.seekTo(0,0)}
    fun previous(){if(player.currentPosition>3500L)player.seekTo(0L) else if(player.hasPreviousMediaItem())player.seekToPreviousMediaItem() else player.seekTo(0L)}
    fun seek(ms:Long){player.seekTo(ms.coerceAtLeast(0L))}
    fun setShuffle(enabled:Boolean) {
        player.shuffleModeEnabled=enabled
        MusicRuntime.shuffle=enabled
        preferences.edit().putBoolean("shuffle",enabled).apply()
    }
    fun changeShuffle(){setShuffle(!player.shuffleModeEnabled)}
    fun changeRepeat() {
        MusicRuntime.repeat=(MusicRuntime.repeat+1)%3
        player.repeatMode=when(MusicRuntime.repeat){1->Player.REPEAT_MODE_ONE;2->Player.REPEAT_MODE_ALL;else->Player.REPEAT_MODE_OFF}
    }
    fun changeSpeed(speed:Float) {
        val v=speed.coerceIn(.5f,2f)
        player.playbackParameters=PlaybackParameters(v)
        MusicRuntime.speed=v
    }
    fun setSleep(minutes:Int) {
        sleepUntil=if(minutes<=0)0L else SystemClock.elapsedRealtime()+minutes*60_000L
    }
    fun setBoost(amount:Int) {MusicRuntime.boost=amount.coerceIn(0,100);applyEffects()}
    fun setBass(amount:Int) {MusicRuntime.bass=amount.coerceIn(0,100);applyEffects()}
    fun enableEqualizer(enabled:Boolean) {MusicRuntime.equalizerEnabled=enabled;applyEffects();MusicRuntime.eqRevision++}
    fun equalizerInfo():String = try {
        val e=eq ?: return "غير مدعوم"
        "${e.numberOfBands} bands / ${e.numberOfPresets} presets"
    } catch (_:Exception){"غير مدعوم"}
    fun applyEqPreset(preset:Short) {
        try {
            val effect=eq ?:return
            if(preset>=0 && preset<effect.numberOfPresets){effect.usePreset(preset);MusicRuntime.equalizerEnabled=true;applyEffects();MusicRuntime.eqRevision++}
        } catch (_:Exception){}
    }
    fun eqPresets():List<String> = try{
        val effect=eq ?:return emptyList()
        (0 until effect.numberOfPresets).map{effect.getPresetName(it.toShort())}
    } catch (_:Exception){emptyList()}
    fun eqBands():List<EqBandInfo> = try {
        val e=eq ?: return emptyList()
        val range=e.bandLevelRange
        val low=maxOf(range[0].toInt(),-600)
        val high=minOf(range[1].toInt(),600)
        (0 until e.numberOfBands.toInt()).map { n ->
            val band=n.toShort()
            EqBandInfo(n,e.getCenterFreq(band)/1000,low,high,
                e.getBandLevel(band).toInt().coerceIn(low,high))
        }
    } catch (_:Exception){emptyList()}

    fun setEqBand(index:Int,levelMb:Int){
        val e=eq ?: return
        if(index !in 0 until e.numberOfBands.toInt())return
        try {
            val range=e.bandLevelRange
            val lower=maxOf(range[0].toInt(),-600)
            val upper=minOf(range[1].toInt(),600)
            e.setBandLevel(index.toShort(),levelMb.coerceIn(lower,upper).toShort())
            MusicRuntime.equalizerEnabled=true
            applyEffects()
            MusicRuntime.eqRevision++
        }catch (_:Exception){}
    }

    private fun attachEffects(audioId:Int) {
        try{eq?.release()}catch(_:Exception){}
        try{bassEffect?.release()}catch(_:Exception){}
        try{loudness?.release()}catch(_:Exception){}
        eq=null;bassEffect=null;loudness=null
        if(audioId<=0 || audioId==C.AUDIO_SESSION_ID_UNSET)return
        eq=try{Equalizer(0,audioId)}catch(_:Throwable){null}
        bassEffect=try{BassBoost(0,audioId)}catch(_:Throwable){null}
        loudness=try{LoudnessEnhancer(audioId)}catch(_:Throwable){null}
        applyEffects()
        MusicRuntime.eqRevision++
    }
    private fun applyEffects(){
        try{eq?.enabled=MusicRuntime.equalizerEnabled}catch(_:Exception){}
        try{bassEffect?.setStrength((MusicRuntime.bass*10).toShort());bassEffect?.enabled=MusicRuntime.bass>0}catch(_:Exception){}
        try{loudness?.setTargetGain(MusicRuntime.boost*6);loudness?.enabled=MusicRuntime.boost>0}catch(_:Exception){}
    }
    private fun updateSession(){
        if(!::session.isInitialized || !::player.isInitialized)return
        val track=MusicRuntime.nowPlaying
        val state=if(player.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        session.setPlaybackState(PlaybackState.Builder().setActions(
            PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(state,player.currentPosition.coerceAtLeast(0L), if(player.isPlaying)MusicRuntime.speed else 0f).build())
        if(track!=null)session.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE,track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,track.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION,track.durationMs).build())
    }
    private fun actionIntent(action:String,requestCode:Int):PendingIntent=
        PendingIntent.getService(this,requestCode,Intent(this,MusicService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun showNotification(active:Boolean) {
        if(!::session.isInitialized || MusicRuntime.nowPlaying==null)return
        val current=MusicRuntime.nowPlaying ?:return
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val style=Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0,1,2)
        val n=Notification.Builder(this,"playback")
            .setSmallIcon(R.drawable.app_icon).setContentTitle(current.title)
            .setContentText(current.artist.ifBlank { current.source })
            .setContentIntent(open).setOngoing(active)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous,"Previous",actionIntent("PREVIOUS",1)).build())
            .addAction(Notification.Action.Builder(if(active)android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if(active)"Pause" else "Play",actionIntent(if(active)"PAUSE" else "PLAY",2)).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_next,"Next",actionIntent("NEXT",3)).build())
            .setStyle(style).build()
        if(active) startForeground(109,n)
        else getSystemService(NotificationManager::class.java).notify(109,n)
    }
    private fun remember() {
        try{
            val arr=JSONArray()
            tracks.forEach { t -> arr.put(JSONObject().apply{
                put("uri",t.uri);put("title",t.title);put("artist",t.artist);put("source",t.source);
                put("duration",t.durationMs)
            })}
            preferences.edit().putString("queue",arr.toString())
                .putInt("index",player.currentMediaItemIndex.coerceAtLeast(0))
                .putLong("position",player.currentPosition.coerceAtLeast(0)).apply()
        }catch(_:Exception){}
    }
    fun restore(){
        if(player.mediaItemCount>0)return
        try{
            val arr=JSONArray(preferences.getString("queue","[]"))
            val saved=(0 until arr.length()).map{arr.getJSONObject(it)}.map{
                AudioTrack(it.getString("uri"),it.optString("title"),it.optString("artist"),
                    source=it.optString("source","Other"),durationMs=it.optLong("duration"))
            }
            if(saved.isNotEmpty()){
                tracks=saved
                player.setMediaItems(saved.map{MediaItem.fromUri(it.uri)},preferences.getInt("index",0).coerceIn(0,saved.lastIndex),
                    preferences.getLong("position",0))
                player.prepare()
                setShuffle(preferences.getBoolean("shuffle",false))
                MusicRuntime.nowPlaying=saved.getOrNull(player.currentMediaItemIndex)
                MusicRuntime.queueRevision++
            }
        }catch(_:Exception){}
    }
    override fun onDestroy(){
        listening.end(SystemClock.elapsedRealtime())
        clock.removeCallbacks(tick)
        remember()
        try{eq?.release();bassEffect?.release();loudness?.release()}catch(_:Exception){}
        session.release();player.release()
        super.onDestroy()
    }
}
