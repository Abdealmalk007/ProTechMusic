package digital.alghad.protechmusic

/**
 * Counts actual audible time, not seeks or queue selections. One count per
 * playback instance after a meaningful portion has really played.
 * Times are monotonic (elapsedRealtime), supplied explicitly for unit tests.
 */
class ListeningTracker(private val onQualifiedPlay:(String)->Unit) {
    private var currentUri:String? = null
    private var durationMs:Long = 0L
    private var listenedMs:Long = 0L
    private var segmentStartedMs:Long? = null
    private var counted:Boolean = false

    fun switchTo(uri:String?, duration:Long, nowMs:Long, isPlaying:Boolean) {
        accumulate(nowMs)
        qualify()
        currentUri=uri
        durationMs=duration.coerceAtLeast(0L)
        listenedMs=0
        counted=false
        segmentStartedMs=if(isPlaying && uri!=null)nowMs else null
    }

    fun playingChanged(playing:Boolean, nowMs:Long) {
        accumulate(nowMs)
        segmentStartedMs=if(playing && currentUri!=null)nowMs else null
        qualify()
    }

    fun tick(nowMs:Long) {
        accumulate(nowMs)
        qualify()
    }

    fun end(nowMs:Long) {
        accumulate(nowMs)
        qualify()
        currentUri=null
        segmentStartedMs=null
    }

    private fun accumulate(nowMs:Long) {
        val started=segmentStartedMs ?: return
        if(nowMs>started)listenedMs+=(nowMs-started).coerceAtMost(60_000L)
        segmentStartedMs=nowMs
    }

    private fun qualify() {
        val uri=currentUri ?:return
        if(counted)return
        // Short voice notes count after >=0.75 s; long audio at 15 s maximum.
        val threshold=if(durationMs<=0L)15_000L
            else (durationMs/2L).coerceIn(750L,15_000L)
        if(listenedMs>=threshold) {
            counted=true
            onQualifiedPlay(uri)
        }
    }
}
