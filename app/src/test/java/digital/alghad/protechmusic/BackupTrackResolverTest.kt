package digital.alghad.protechmusic

import org.junit.Assert.*
import org.junit.Test

class BackupTrackResolverTest {
    private val old=AudioTrack("content://media/external/audio/media/10","نشيد الأمل",
        "محمد","ألبوم","Music","Music",10_000)
    private val now=AudioTrack("content://media/external/audio/media/90","نشيد الأمل",
        "محمد","ألبوم","Music","Music",10_100)
    private val library=listOf(now,
        AudioTrack("content://media/external/audio/media/91","Duplicate","Artist 1",durationMs=10_000),
        AudioTrack("content://media/external/audio/media/92","Duplicate","Artist 2",durationMs=10_000))

    @Test fun exactUriAlwaysWins(){assertEquals(MatchStatus.EXACT,BackupTrackResolver(library).resolve(
        BackupMediaRef(now.uri,now.title,now.artist,now.durationMs)).status)}
    @Test fun rebindChangedUriByMetadata(){val r=BackupTrackResolver(library).resolve(
        BackupMediaRef(old.uri,old.title,old.artist,old.durationMs))
        assertEquals(MatchStatus.REBOUND,r.status);assertEquals(now,r.track)}
    @Test fun arabicDiacriticsRebinding(){assertEquals(now,
        BackupTrackResolver(library).resolve(
            BackupMediaRef("content://old","نشيد الامل","محمد",10_200)).track)}
    @Test fun ambiguousTitleIsNotGuessed(){val r=BackupTrackResolver(library).resolve(
        BackupMediaRef("content://old","Duplicate",durationMs=10_000))
        assertEquals(MatchStatus.AMBIGUOUS,r.status);assertNull(r.track)}
    @Test fun artistDisambiguates(){val r=BackupTrackResolver(library).resolve(
        BackupMediaRef("content://old","Duplicate","Artist 2",10_000))
        assertEquals("content://media/external/audio/media/92",r.track?.uri)}
    @Test fun incompatibleDurationDoesNotMatch(){assertEquals(MatchStatus.MISSING,
        BackupTrackResolver(library).resolve(
            BackupMediaRef("content://old","Duplicate","Artist 2",30_000)).status)}
    @Test fun noMetadataMeansUnresolved(){assertEquals(MatchStatus.MISSING,
        BackupTrackResolver(library).resolve(BackupMediaRef("content://old")).status)}
}
