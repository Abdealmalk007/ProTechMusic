package digital.alghad.protechmusic

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import org.json.JSONArray

data class Playlist(val id:Long,val name:String,val count:Int)

class MusicStore(context: Context) : SQLiteOpenHelper(context,"protech_music.db",null,1) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL COLLATE NOCASE UNIQUE,created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE entries (playlist_id INTEGER NOT NULL,uri TEXT NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(playlist_id,uri))")
        db.execSQL("CREATE INDEX idx_entries_position ON entries(playlist_id,position)")
        db.execSQL("CREATE TABLE favorites (uri TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE imported (uri TEXT PRIMARY KEY,record TEXT NOT NULL)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) { /* migrations required for version 2 */ }

    @Synchronized fun playlists(): List<Playlist> {
        val out= mutableListOf<Playlist>()
        readableDatabase.rawQuery("SELECT p.id,p.name,COUNT(e.uri) FROM playlists p LEFT JOIN entries e ON p.id=e.playlist_id GROUP BY p.id ORDER BY p.name COLLATE NOCASE",null).use{ c ->
            while(c.moveToNext()) out.add(Playlist(c.getLong(0),c.getString(1),c.getInt(2)))
        }
        return out
    }
    @Synchronized fun createPlaylist(name:String): Long {
        val trimmed=name.trim()
        require(trimmed.isNotEmpty() && trimmed.length<=100) { "Playlist name must be 1-100 characters" }
        val values=ContentValues().apply {put("name",trimmed); put("created_at",System.currentTimeMillis())}
        return writableDatabase.insertWithOnConflict("playlists",null,values,SQLiteDatabase.CONFLICT_IGNORE)
    }
    @Synchronized fun renamePlaylist(id:Long,name:String):Boolean {
        val trimmed=name.trim()
        if(trimmed.isEmpty()||trimmed.length>100)return false
        return try{writableDatabase.update("playlists",ContentValues().apply{put("name",trimmed)},"id=?",arrayOf(id.toString()))>0}
        catch (_:Exception){false}
    }
    @Synchronized fun deletePlaylist(id:Long) {
        val db=writableDatabase;db.beginTransaction()
        try { db.delete("entries","playlist_id=?",arrayOf("$id"));db.delete("playlists","id=?",arrayOf("$id"));db.setTransactionSuccessful() }
        finally { db.endTransaction() }
    }
    @Synchronized fun addToPlaylist(id:Long,uris:List<String>) {
        val db=writableDatabase;db.beginTransaction()
        try{
            val c=db.rawQuery("SELECT COALESCE(MAX(position),-1) FROM entries WHERE playlist_id=?",arrayOf("$id"))
            val max=c.use{it.moveToFirst();it.getInt(0)}
            var position=max+1
            for(uri in uris.distinct()){
                val v=ContentValues().apply{put("playlist_id",id);put("uri",uri);put("position",position)}
                if(db.insertWithOnConflict("entries",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1L)position++
            }
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    @Synchronized fun playlistTracks(id:Long):List<String> {
        val out= mutableListOf<String>()
        readableDatabase.rawQuery("SELECT uri FROM entries WHERE playlist_id=? ORDER BY position",arrayOf("$id")).use{
            while(it.moveToNext())out.add(it.getString(0))
        };return out
    }
    @Synchronized fun removeFromPlaylist(id:Long,uri:String) {
        writableDatabase.delete("entries","playlist_id=? AND uri=?",arrayOf("$id",uri))
    }
    @Synchronized fun moveInPlaylist(id:Long,uri:String,direction:Int) {
        val rows=playlistTracks(id).toMutableList()
        val index=rows.indexOf(uri);val to=index+direction
        if(index<0 || to !in rows.indices)return
        rows.removeAt(index);rows.add(to,uri)
        val db=writableDatabase;db.beginTransaction()
        try {
            rows.forEachIndexed{position,value->
                db.execSQL("UPDATE entries SET position=? WHERE playlist_id=? AND uri=?",arrayOf<Any>(position,id,value))
            };db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    @Synchronized fun isFavorite(uri:String):Boolean=readableDatabase.rawQuery(
        "SELECT 1 FROM favorites WHERE uri=?",arrayOf(uri)).use{it.moveToFirst()}
    @Synchronized fun toggleFavorite(uri:String):Boolean {
        val db=writableDatabase
        return if(isFavorite(uri)){db.delete("favorites","uri=?",arrayOf(uri));false}
        else {db.insertWithOnConflict("favorites",null,ContentValues().apply{put("uri",uri)},SQLiteDatabase.CONFLICT_IGNORE);true}
    }
    @Synchronized fun favorites():Set<String> {
        val out=mutableSetOf<String>()
        readableDatabase.rawQuery("SELECT uri FROM favorites",null).use{while(it.moveToNext())out.add(it.getString(0))}
        return out
    }
    @Synchronized fun saveImported(t:AudioTrack) {
        val record=JSONObject().apply{
            put("uri",t.uri);put("title",t.title);put("artist",t.artist);put("album",t.album)
            put("folder",t.folder);put("source",t.source);put("duration",t.durationMs);put("size",t.sizeBytes)
        }.toString()
        writableDatabase.insertWithOnConflict("imported",null,ContentValues().apply{
            put("uri",t.uri);put("record",record)
        },SQLiteDatabase.CONFLICT_REPLACE)
    }
    @Synchronized fun exportBackup():String {
        val root=JSONObject()
        root.put("format","ProTechMusicBackup")
        root.put("version",1)
        val lists=JSONArray()
        playlists().forEach { playlist ->
            val entries=JSONArray()
            playlistTracks(playlist.id).forEach{entries.put(it)}
            lists.put(JSONObject().put("name",playlist.name).put("tracks",entries))
        }
        root.put("playlists",lists)
        val fav=JSONArray()
        favorites().sorted().forEach{fav.put(it)}
        root.put("favorites",fav)
        return root.toString(2)
    }

    @Synchronized fun restoreBackup(content:String):Int {
        require(content.length <= 2_000_000){"Backup too large"}
        val root=JSONObject(content)
        require(root.optString("format")=="ProTechMusicBackup" &&
            root.optInt("version")==1){"Unsupported backup format"}
        val arr=root.getJSONArray("playlists")
        require(arr.length()<=1000){"Too many playlists"}
        val normalized=mutableListOf<Pair<String,List<String>>>()
        var total=0
        for(i in 0 until arr.length()){
            val row=arr.getJSONObject(i)
            val name=row.getString("name").trim()
            require(name.isNotEmpty() && name.length<=100){"Invalid playlist name"}
            val entries=row.getJSONArray("tracks")
            require(entries.length()<=50000){"Playlist too large"}
            val uris=mutableListOf<String>()
            for(j in 0 until entries.length()){
                val uri=entries.getString(j)
                require(uri.length<=2048 && uri.startsWith("content://")){"Invalid media URI"}
                uris.add(uri)
                total++
                require(total<=100000){"Too many entries"}
            }
            normalized.add(name to uris.distinct())
        }
        val favArr=root.optJSONArray("favorites")?:JSONArray()
        require(favArr.length()<=100000){"Too many favorites"}
        val fav=mutableListOf<String>()
        for(i in 0 until favArr.length()){
            val uri=favArr.getString(i)
            require(uri.length<=2048 && uri.startsWith("content://")){"Invalid favorite URI"}
            fav.add(uri)
        }
        val db=writableDatabase
        db.beginTransaction()
        try {
            for((name,items) in normalized){
                val existing=db.rawQuery("SELECT id FROM playlists WHERE name=? COLLATE NOCASE",
                    arrayOf(name)).use{if(it.moveToFirst())it.getLong(0) else -1L}
                val id=if(existing>0)existing else db.insertOrThrow("playlists",null,
                    ContentValues().apply{put("name",name);put("created_at",System.currentTimeMillis())})
                val last=db.rawQuery("SELECT COALESCE(MAX(position),-1) FROM entries WHERE playlist_id=?",
                    arrayOf(id.toString())).use{it.moveToFirst();it.getInt(0)}
                var pos=last+1
                for(uri in items){
                    val values=ContentValues().apply{put("playlist_id",id);put("uri",uri);put("position",pos)}
                    if(db.insertWithOnConflict("entries",null,values,SQLiteDatabase.CONFLICT_IGNORE)!=-1L)pos++
                }
            }
            for(uri in fav)db.insertWithOnConflict("favorites",null,
                ContentValues().apply{put("uri",uri)},SQLiteDatabase.CONFLICT_IGNORE)
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
        return normalized.size
    }

    @Synchronized fun importedTracks():List<AudioTrack> {
        val out=mutableListOf<AudioTrack>()
        readableDatabase.rawQuery("SELECT record FROM imported",null).use{ cursor ->
            while(cursor.moveToNext())try{
                val o=JSONObject(cursor.getString(0))
                out.add(AudioTrack(o.getString("uri"),o.optString("title"),o.optString("artist"),
                    o.optString("album"),o.optString("folder"),o.optString("source"),
                    o.optLong("duration"),o.optLong("size")))
            }catch(_:Exception){}
        };return out
    }
}
