package digital.alghad.protechmusic

import kotlin.math.abs

data class BackupMediaRef(
    val uri: String,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0
)
enum class MatchStatus { EXACT, REBOUND, MISSING, AMBIGUOUS }
data class BackupMatch(val track: AudioTrack?, val status: MatchStatus)

/** Cross-device metadata matching. Never silently picks among equally plausible files. */
class BackupTrackResolver(library: List<AudioTrack>) {
    private val byUri=library.associateBy{it.uri}
    private val byTitle=library.groupBy{TrackSearchEngine.normalize(it.title)}

    fun resolve(ref: BackupMediaRef): BackupMatch {
        byUri[ref.uri]?.let { exact ->
            val titleMatches=ref.title.isBlank() ||
                TrackSearchEngine.normalize(ref.title)==TrackSearchEngine.normalize(exact.title)
            val artistMatches=ref.artist.isBlank() || exact.artist.isBlank() ||
                TrackSearchEngine.normalize(ref.artist)==TrackSearchEngine.normalize(exact.artist)
            val durationMatches=ref.durationMs<=0 || exact.durationMs<=0 ||
                abs(exact.durationMs-ref.durationMs)<=2_000L
            if(titleMatches && artistMatches && durationMatches)
                return BackupMatch(exact,MatchStatus.EXACT)
        }
        if(ref.title.isBlank())return BackupMatch(null,MatchStatus.MISSING)
        val candidates=byTitle[TrackSearchEngine.normalize(ref.title)].orEmpty()
            .filter{
                ref.artist.isBlank() ||
                    TrackSearchEngine.normalize(it.artist)==TrackSearchEngine.normalize(ref.artist)
            }.filter{
                ref.durationMs<=0 || it.durationMs<=0 ||
                    abs(it.durationMs-ref.durationMs)<=2_000L
            }
        return when(candidates.size){
            0 -> BackupMatch(null,MatchStatus.MISSING)
            1 -> BackupMatch(candidates.single(),MatchStatus.REBOUND)
            else -> BackupMatch(null,MatchStatus.AMBIGUOUS)
        }
    }
}
data class PortableRestoreReport(
    val playlists: Int,
    val resolved: Int,
    val missing: Int,
    val ambiguous: Int,
    val favoritesRestored: Int
)
