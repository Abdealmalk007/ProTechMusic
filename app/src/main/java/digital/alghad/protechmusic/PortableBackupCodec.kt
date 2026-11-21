package digital.alghad.protechmusic

import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON v2 includes metadata needed to safely rebind local audio after a phone change.
 * It never copies sound files and never opens URLs or requests new storage permissions.
 */
object PortableBackupCodec {
    private const val MAX_CHARS=2_000_000
    private const val MAX_PLAYLISTS=1_000
    private const val MAX_TRACKS=100_000

    fun export(v1:String, library:List<AudioTrack>):String {
        val root=JSONObject(v1)
        require(root.optString("format")=="ProTechMusicBackup")
        val index=library.associateBy{it.uri}
        val lists=root.getJSONArray("playlists")
        for(i in 0 until lists.length()){
            val record=lists.getJSONObject(i)
            val source=record.getJSONArray("tracks")
            val entries=JSONArray()
            for(j in 0 until source.length())
                entries.put(pack(source.getString(j),index))
            record.put("tracks",entries)
        }
        val sourceFav=root.optJSONArray("favorites") ?: JSONArray()
        val fav=JSONArray()
        for(i in 0 until sourceFav.length())
            fav.put(pack(sourceFav.getString(i),index))
        root.put("favorites",fav)
        root.put("version",2)
        val text=root.toString(2)
        require(text.length<=MAX_CHARS){"Backup too large"}
        return text
    }

    fun translateForRestore(data:String, library:List<AudioTrack>):Pair<String,PortableRestoreReport> {
        require(data.length<=MAX_CHARS){"Backup too large"}
        val source=JSONObject(data)
        require(source.optString("format")=="ProTechMusicBackup"){"Invalid backup format"}
        require(source.optInt("version") in 1..2){"Unsupported backup version"}
        val lists=source.getJSONArray("playlists")
        require(lists.length()<=MAX_PLAYLISTS){"Too many playlists"}
        val resolver=BackupTrackResolver(library)
        var matched=0
        var missing=0
        var ambiguous=0
        var count=0
        val outLists=JSONArray()
        for(i in 0 until lists.length()){
            val sourceList=lists.getJSONObject(i)
            val name=sourceList.getString("name").trim()
            require(name.isNotBlank() && name.length<=100){"Invalid playlist name"}
            val input=sourceList.getJSONArray("tracks")
            require(input.length()<=50_000){"Playlist too large"}
            val unique=LinkedHashSet<String>()
            for(j in 0 until input.length()){
                count++
                require(count<=MAX_TRACKS){"Too many tracks"}
                val ref=unpack(input.get(j))
                when(val resolution=resolver.resolve(ref)){
                    is BackupMatch -> when(resolution.status){
                        MatchStatus.EXACT,MatchStatus.REBOUND ->{
                            unique.add(requireNotNull(resolution.track).uri)
                            matched++
                        }
                        MatchStatus.MISSING -> missing++
                        MatchStatus.AMBIGUOUS -> ambiguous++
                    }
                }
            }
            val stored=JSONArray()
            unique.forEach{stored.put(it)}
            outLists.put(JSONObject().put("name",name).put("tracks",stored))
        }
        val sourceFav=source.optJSONArray("favorites") ?: JSONArray()
        require(sourceFav.length()<=MAX_TRACKS){"Too many favorites"}
        val outFavorites=LinkedHashSet<String>()
        for(i in 0 until sourceFav.length()){
            val ref=unpack(sourceFav.get(i))
            when(val resolved=resolver.resolve(ref)){
                is BackupMatch -> when(resolved.status){
                    MatchStatus.EXACT,MatchStatus.REBOUND -> outFavorites.add(requireNotNull(resolved.track).uri)
                    MatchStatus.MISSING -> missing++
                    MatchStatus.AMBIGUOUS -> ambiguous++
                }
            }
        }
        val fav=JSONArray()
        outFavorites.forEach{fav.put(it)}
        val normalized=JSONObject()
            .put("format","ProTechMusicBackup").put("version",1)
            .put("playlists",outLists).put("favorites",fav).toString()
        require(normalized.length<=MAX_CHARS){"Restored result too large"}
        return normalized to PortableRestoreReport(
            playlists=lists.length(),resolved=matched,missing=missing,
            ambiguous=ambiguous,favoritesRestored=outFavorites.size
        )
    }

    private fun pack(uri:String,index:Map<String,AudioTrack>):JSONObject{
        require(uri.startsWith("content://")&&uri.length<=2048){"Invalid URI"}
        val song=index[uri]
        return JSONObject().put("uri",uri)
            .put("title",song?.title.orEmpty().take(250))
            .put("artist",song?.artist.orEmpty().take(250))
            .put("durationMs",song?.durationMs ?: 0L)
    }

    private fun unpack(obj:Any):BackupMediaRef {
        val ref=when(obj){
            is String -> BackupMediaRef(obj)
            is JSONObject -> BackupMediaRef(
                uri=obj.getString("uri"),
                title=obj.optString("title").take(250),
                artist=obj.optString("artist").take(250),
                durationMs=obj.optLong("durationMs").coerceAtLeast(0L)
            )
            else -> throw IllegalArgumentException("Invalid backup media reference")
        }
        require(ref.uri.startsWith("content://") && ref.uri.length<=2048){"Invalid reference URI"}
        return ref
    }
}
