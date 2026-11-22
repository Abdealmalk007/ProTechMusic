package digital.alghad.protechmusic

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import java.io.ByteArrayOutputStream
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.util.Locale

private val deepBlue=Color(0xFF12132D)
private val purple=Color(0xFF9A55FA)
private val aqua=Color(0xFF42DCEB)

class MainActivity:ComponentActivity() {
    private val job=SupervisorJob()
    private val scope=CoroutineScope(Dispatchers.Main + job)
    private val db by lazy {MusicStore(this)}
    private val repository by lazy {AudioRepository(this,db)}
    private val lyricsStore by lazy {LyricsStore(applicationContext)}
    private var lyricsTargetUri: String? = null
    private val pickLyrics = registerForActivityResult(ActivityResultContracts.OpenDocument()) { document ->
        val target = lyricsTargetUri.also { lyricsTargetUri = null }
        if (document != null && target != null) scope.launch {
            try {
                val count = lyricsStore.importFile(target, document)
                MusicRuntime.lyricsRevision++
                Toast.makeText(this@MainActivity,"تم استيراد $count سطرًا من كلمات المقطع",Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity,"تعذر قراءة LRC أو لا يحتوي على توقيتات صحيحة",Toast.LENGTH_LONG).show()
            }
        }
    }
    private var songs by mutableStateOf<List<AudioTrack>>(emptyList())
    private var preparedLibrary by mutableStateOf(TrackSearchEngine.prepare(emptyList()))
    private val uiPreferences by lazy { getSharedPreferences("ui_language", MODE_PRIVATE) }
    private var musicService by mutableStateOf<MusicService?>(null)
    private var allowed by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var failure by mutableStateOf("")
    private var refreshCounter by mutableIntStateOf(0)
    private val deletion=MediaDeletionController(this,
        onDeleted={ refs ->
            try{db.purgeDeleted(refs.toList())}catch(_:Exception){}
            try{musicService?.removeDeletedMedia(refs)}catch(_:Exception){}
        },
        onFinished={message ->
            Toast.makeText(this,message,Toast.LENGTH_LONG).show()
            refresh()
            refreshCounter++
        }
    )
    private val requestAudio=registerForActivityResult(ActivityResultContracts.RequestPermission()){ granted->
        allowed=granted
        if(granted) refresh()
    }
    private val pickAudio=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){ list ->
        var imported=0
        list.forEach{ uri -> try{if(repository.importDocument(uri)!=null)imported++}catch(_:Exception){} }
        refresh()
    }
    private val createBackup=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null)scope.launch {
            try {
                val library=songs.toList()
                withContext(Dispatchers.IO) {
                    val stream=contentResolver.openOutputStream(uri,"w")
                        ?:throw IllegalStateException("Cannot write backup")
                    stream.use{it.write(PortableBackupCodec.export(db.exportBackup(),library).toByteArray(Charsets.UTF_8))}
                }
                Toast.makeText(this@MainActivity,"تم حفظ نسخة القوائم",Toast.LENGTH_SHORT).show()
            }catch(_:Exception){
                Toast.makeText(this@MainActivity,"تعذر حفظ النسخة الاحتياطية",Toast.LENGTH_LONG).show()
            }
        }
    }
    private val openBackup=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null)scope.launch {
            try {
                val library=songs.toList()
                val report=withContext(Dispatchers.IO){
                    val stream=contentResolver.openInputStream(uri)
                        ?:throw IllegalStateException("Cannot read backup")
                    val bytes=stream.use{input ->
                        val output=ByteArrayOutputStream()
                        val block=ByteArray(8192)
                        var total=0
                        while(true){
                            val n=input.read(block)
                            if(n<0)break
                            total+=n
                            require(total<=2_000_000){"File too large"}
                            output.write(block,0,n)
                        }
                        output.toByteArray()
                    }
                    val (normalized,details)=PortableBackupCodec.translateForRestore(
                        String(bytes,Charsets.UTF_8),library)
                    db.restoreBackup(normalized)
                    details
                }
                refreshCounter++
                Toast.makeText(this@MainActivity,
                    "تمت استعادة "+report.playlists+" قائمة، "+report.resolved+
                    " مقطع؛ غير موجود: "+report.missing+"؛ متشابه: "+report.ambiguous,
                    Toast.LENGTH_LONG).show()
            }catch(_:Exception){
                Toast.makeText(this@MainActivity,"ملف النسخة غير صالح أو تعذر استيراده",Toast.LENGTH_LONG).show()
            }
        }
    }

    private var pendingM3uPlaylistId = -1L
    private var m3uReport by mutableStateOf<String?>(null)
    private val createM3u=registerForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val playlistId=pendingM3uPlaylistId
        if(uri!=null && playlistId>0L)scope.launch {
            try {
                val snapshot=songs.associateBy { it.uri }
                val count=withContext(Dispatchers.IO){
                    val tracks=db.playlistTracks(playlistId).map { ref ->
                        snapshot[ref] ?: AudioTrack(ref,ref.substringAfterLast('/'),source="Missing reference")
                    }
                    val bytes=M3uCodec.encode(tracks).toByteArray(Charsets.UTF_8)
                    require(bytes.size<=2_000_000) { "Playlist too large" }
                    contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) }
                        ?:throw IllegalStateException("Cannot create playlist file")
                    tracks.size
                }
                Toast.makeText(this@MainActivity,"تم تصدير $count مقطع إلى M3U8",Toast.LENGTH_LONG).show()
            }catch(_:Exception){
                Toast.makeText(this@MainActivity,"تعذر تصدير قائمة M3U8",Toast.LENGTH_LONG).show()
            }
        }
    }
    private val openM3u=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null)scope.launch {
            try {
                val available=songs.toList()
                val message=withContext(Dispatchers.IO) {
                    val bytes=contentResolver.openInputStream(uri)?.use { stream ->
                        val out=ByteArrayOutputStream()
                        val block=ByteArray(8192)
                        while(true){
                            val n=stream.read(block)
                            if(n<0)break
                            require(out.size()+n<=2_000_000) { "Playlist too large" }
                            out.write(block,0,n)
                        }
                        out.toByteArray()
                    } ?:throw IllegalStateException("Cannot read playlist")
                    val result=M3uCodec.resolve(String(bytes,Charsets.UTF_8),available)
                    if(result.tracks.isNotEmpty()){
                        val rawName=contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)
                            ?.use{ cursor -> if(cursor.moveToFirst())cursor.getString(0) else null }
                            ?.substringBeforeLast('.')?.trim()?.take(75)
                            .orEmpty().ifBlank { "Imported M3U8" }
                        var id=db.createPlaylist(rawName)
                        if(id<0)id=db.createPlaylist("$rawName ${System.currentTimeMillis()}")
                        if(id>0)db.addToPlaylist(id,result.tracks.map{it.uri})
                    }
                    buildString{
                        append("تم العثور على ${result.tracks.size} مقطع قابل للإضافة.\n")
                        append("ملفات مفقودة: ${result.missing.size} | أسماء متشابهة: ${result.ambiguous.size}")
                        val unresolved=(result.missing+result.ambiguous).take(12)
                        if(unresolved.isNotEmpty())append("\n\n").append(unresolved.joinToString("\n"))
                        if(result.missing.size+result.ambiguous.size>12)append("\n…")
                        append("\n\nلا يتم حذف أو تعديل أي ملف صوتي أصلي.")
                    }
                }
                refreshCounter++
                m3uReport=message
            }catch(_:Exception){
                m3uReport="ملف M3U/M3U8 غير صالح أو كبير جدًا أو تعذر قراءته."
            }
        }
    }

    private val listener=object:ServiceConnection {
        override fun onServiceConnected(name:ComponentName?,binder:android.os.IBinder?){
            musicService=(binder as? MusicService.LocalBinder)?.service
            musicService?.restore()
        }
        override fun onServiceDisconnected(name:ComponentName?){musicService=null}
    }
    private var connected=false
    private var observerRegistered=false
    private var updateJob:Job?=null
    private val mediaObserver=object:android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())){
        override fun onChange(selfChange:Boolean){
            updateJob?.cancel()
            updateJob=scope.launch{delay(800L);if(hasMediaPermission())refresh()}
        }
    }
    override fun onStart(){
        super.onStart()
        allowed=hasMediaPermission()
        if(allowed)refresh()
        if(!observerRegistered)try{
            contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,true,mediaObserver)
            observerRegistered=true
        }catch(_:Exception){}
    }
    override fun onStop(){
        if(observerRegistered)try{contentResolver.unregisterContentObserver(mediaObserver)}catch(_:Exception){}
        observerRegistered=false
        super.onStop()
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        allowed=hasMediaPermission()
        UiLanguage.english=uiPreferences.getBoolean("english",false)
        connected=bindService(Intent(this,MusicService::class.java),listener,BIND_AUTO_CREATE)
        setContent { MaterialTheme(
            colorScheme=darkColorScheme(primary=aqua,secondary=purple,background=deepBlue,
                surface=Color(0xFF1C1D3D),onSurface=Color.White,onBackground=Color.White)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides
                if(UiLanguage.english) LayoutDirection.Ltr else LayoutDirection.Rtl) {
                MusicApp(
                    songs=songs,preparedSearch=preparedLibrary,allowed=allowed,busy=busy,error=failure,
                    service=musicService,db=db,revision=refreshCounter,
                    onGrant={grantPermission()},onRescan={refresh()},onPick={pickAudio.launch(arrayOf("audio/*"))},
                    onToggleLanguage={
                        UiLanguage.english=!UiLanguage.english
                        uiPreferences.edit().putBoolean("english",UiLanguage.english).apply()
                    },
                    onPlay={selected,from ->
                        val service=musicService
                        if(service!=null){
                            try{startService(Intent(this,MusicService::class.java))}catch(_:Exception){}
                            val idx=from.indexOfFirst{it.uri==selected.uri}.coerceAtLeast(0)
                            service.playQueue(from,idx)
                        }
                    },
                    onShare={shareTracks(it)},
                    onImportLyrics={ track ->
                        lyricsTargetUri=track.uri
                        pickLyrics.launch(arrayOf("text/plain","application/octet-stream","*/*"))
                    },
                    onDelete={deletion.begin(it)},
                    onPlayGroup={tracks,random ->
                        if(tracks.isNotEmpty()) {
                            try{startService(Intent(this,MusicService::class.java))}catch(_:Exception){}
                            musicService?.apply{playQueue(tracks,0);setShuffle(random)}
                        }
                    },
                    onExport={createBackup.launch("ProTechMusic_backup.json")},
                    onImport={openBackup.launch(arrayOf("application/json","text/plain"))},
                    onImportM3u={openM3u.launch(arrayOf("*/*"))},
                    onExportM3u={id ->
                        pendingM3uPlaylistId=id
                        val name=db.playlists().firstOrNull{it.id==id}?.name.orEmpty()
                            .ifBlank{"ProTechMusic"}
                        createM3u.launch("${name.take(60)}.m3u8")
                    },
                    importReport=m3uReport,
                    onDismissReport={m3uReport=null},
                    onChanged={refreshCounter++}
                )
            }
        }}
        if(allowed) refresh()
    }
    private fun shareTracks(tracks:List<AudioTrack>) {
        val uris=tracks.mapNotNull {
            try {
                Uri.parse(it.uri).takeIf{uri->uri.scheme=="content"}
            }catch (_:Exception){null}
        }.distinct()
        if(uris.isEmpty()){
            Toast.makeText(this,"لا يوجد ملف قابل للمشاركة",Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val clip=ClipData.newUri(contentResolver,"Audio",uris.first())
            uris.drop(1).forEach{clip.addItem(ClipData.Item(it))}
            val intent=Intent(if(uris.size==1)Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply{
                type="audio/*"
                if(uris.size==1)putExtra(Intent.EXTRA_STREAM,uris.first())
                else putParcelableArrayListExtra(Intent.EXTRA_STREAM,ArrayList(uris))
                clipData=clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent,"مشاركة الصوتيات"))
        } catch(e:Exception) {
            Toast.makeText(this,"تعذرت مشاركة الملفات الصوتية",Toast.LENGTH_LONG).show()
        }
    }
    private fun hasMediaPermission():Boolean {
        val p=if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED
    }
    private fun grantPermission(){
        val p=if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        requestAudio.launch(p)
    }
    private fun refresh(){
        if(busy)return
        busy=true;failure=""
        scope.launch{
            try {
                val discovered=repository.scan()
                val searchIndex=withContext(Dispatchers.Default){TrackSearchEngine.prepare(discovered)}
                preparedLibrary=searchIndex
                songs=discovered
            }
            catch(e:Exception) {failure=e.localizedMessage ?: "تعذر قراءة الملفات" }
            finally{busy=false;refreshCounter++}
        }
    }
    override fun onDestroy(){
        if(connected)try{unbindService(listener)}catch(_:Exception){}
        job.cancel()
        super.onDestroy()
    }
}

private fun readableTime(ms:Long):String {
    val s=(ms/1000).coerceAtLeast(0)
    return if(s>=3600) "%d:%02d:%02d".format(Locale.US,s/3600,(s/60)%60,s%60)
    else "%d:%02d".format(Locale.US,s/60,s%60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicApp(
    songs:List<AudioTrack>,preparedSearch:TrackSearchEngine.PreparedLibrary,
    allowed:Boolean,busy:Boolean,error:String,
    service:MusicService?,db:MusicStore,revision:Int,
    onGrant:()->Unit,onRescan:()->Unit,onPick:()->Unit,onToggleLanguage:()->Unit,
    onPlay:(AudioTrack,List<AudioTrack>)->Unit,
    onShare:(List<AudioTrack>)->Unit,
    onImportLyrics:(AudioTrack)->Unit,
    onDelete:(List<AudioTrack>)->Unit,
    onPlayGroup:(List<AudioTrack>,Boolean)->Unit,
    onExport:()->Unit,onImport:()->Unit,
    onExportM3u:(Long)->Unit,onImportM3u:()->Unit,
    importReport:String?,onDismissReport:()->Unit,onChanged:()->Unit
){
    val configuration=LocalConfiguration.current
    val isLandscape=configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompact=configuration.screenWidthDp<360
    var tab by remember{mutableStateOf("all")}
    var showLandscapeFilters by remember{mutableStateOf(false)}
    var showCompactActions by remember{mutableStateOf(false)}
    var smartFilter by remember{mutableStateOf("recentlyAdded")}
    var source by remember{mutableStateOf("All")}
    var folder by remember{mutableStateOf("")}
    var artist by remember{mutableStateOf("")}
    var album by remember{mutableStateOf("")}
    var playlist by remember{mutableLongStateOf(-1L)}
    var search by remember{mutableStateOf("")}
    var searchSort by remember{mutableStateOf(TrackSearchEngine.Order.ORIGINAL)}
    var showSortMenu by remember{mutableStateOf(false)}
    var visibleLimit by remember(tab,search,searchSort,smartFilter,source,folder,artist,album,playlist,revision){ mutableIntStateOf(200) }
    var adding by remember{mutableStateOf<List<AudioTrack>?>(null)}
    var creating by remember{mutableStateOf(false)}
    var playlistName by remember{mutableStateOf("")}
    var playlistForEdit by remember{mutableStateOf(-1L)}
    var confirmPlaylistDelete by remember{mutableStateOf(false)}
    var selection by remember{mutableStateOf(false)}
    val selected= remember{mutableStateListOf<String>()}
    var confirmPermanentDelete by remember{mutableStateOf<List<AudioTrack>>(emptyList())}
    var showPlayer by remember{mutableStateOf(false)}
    val playlists=remember(revision){ db.playlists() }
    val favorites=remember(revision){db.favorites()}
    val statsRevision=MusicRuntime.statsRevision
    val playStats=remember(revision,statsRevision){db.playStats()}
    val playlistUris=remember(revision,playlist){if(playlist>=0)db.playlistTracks(playlist) else emptyList()}
    val sourceOrder=listOf("WhatsApp","Downloads","Recordings","Telegram","Bluetooth","Music","Podcasts","Audiobooks","System sounds","Imported","Other")
    val sources=remember(songs){songs.groupBy{it.source}}
    val sourceLabels=remember(sources){sourceOrder+(sources.keys-sourceOrder.toSet()).sorted()}
    val folders=remember(songs){songs.map{it.folder}.filter{it.isNotBlank()}.distinct().sorted()}
    val artists=remember(songs){songs.map{it.artist.ifBlank{"غير معروف"}}.distinct().sorted()}
    val albums=remember(songs){songs.map{it.album.ifBlank{"غير معروف"}}.distinct().sorted()}
    val byUri=remember(songs){songs.associateBy{it.uri}}
    val queueRevision=MusicRuntime.queueRevision
    val queuedTracks=remember(service,queueRevision){service?.queueSnapshot().orEmpty()}
    val queueCurrentIndex=service?.queueCurrentIndex() ?: -1
    val filtered=remember(songs,tab,smartFilter,playStats,source,folder,artist,album,playlistUris,favorites,search,searchSort) {
        val base= when(tab) {
            "source" -> songs.filter{it.source==source}
            "playlist" -> playlistUris.mapNotNull{byUri[it]}
            "favorites" -> songs.filter{favorites.contains(it.uri)}
            "smart" -> when(smartFilter){
                "recentlyAdded" -> songs.sortedWith(compareByDescending<AudioTrack>{it.addedAtSeconds}.thenBy{it.title})
                "recentlyPlayed" -> songs.filter{playStats.containsKey(it.uri)}.sortedByDescending{playStats[it.uri]?.lastPlayedAt?:0L}
                "mostPlayed" -> songs.filter{playStats.containsKey(it.uri)}.sortedWith(
                    compareByDescending<AudioTrack>{playStats[it.uri]?.count?:0}.thenByDescending{playStats[it.uri]?.lastPlayedAt?:0L})
                "favorites" -> songs.filter{it.uri in favorites}
                else -> songs
            }
            "folder" -> songs.filter{it.folder==folder}
            "artist" -> songs.filter{it.artist.ifBlank{"غير معروف"}==artist}
            "album" -> songs.filter{it.album.ifBlank{"غير معروف"}==album}
            else -> songs
        }
        preparedSearch.search(base,search,searchSort)
    }
    if(importReport!=null){
        AlertDialog(onDismissRequest=onDismissReport,title={Text(UiLanguage.text("نتيجة استيراد M3U8"))},
            text={Text(importReport)},
            confirmButton={TextButton(onClick=onDismissReport){Text(UiLanguage.text("إغلاق"))}})
    }
    if(confirmPermanentDelete.isNotEmpty()){
        val candidates=confirmPermanentDelete
        AlertDialog(
            onDismissRequest={confirmPermanentDelete=emptyList()},
            title={Text(UiLanguage.text("حذف نهائي من الجهاز"),color=Color(0xFFFFAAAA))},
            text={Text("سيُحذف "+candidates.size+
                " مقطع صوتي نهائيًا من ذاكرة الجهاز، وليس من قائمة التشغيل فقط. لا يمكن التراجع. سيطلب Android موافقتك، والملفات التي لا يسمح بحذفها ستبقى.")},
            confirmButton={TextButton(onClick={
                confirmPermanentDelete=emptyList()
                onDelete(candidates)
                selected.clear()
                selection=false
            }){Text(UiLanguage.text("تأكيد الحذف النهائي"),color=Color(0xFFFF9999))}},
            dismissButton={TextButton(onClick={confirmPermanentDelete=emptyList()}){Text(UiLanguage.text("إلغاء"))}}
        )
    }
    if(confirmPlaylistDelete){
        AlertDialog(
            onDismissRequest={confirmPlaylistDelete=false},
            title={Text(UiLanguage.text("تأكيد حذف القائمة"))},
            text={Text(UiLanguage.text("سيتم حذف قائمة التشغيل فقط، ولن تُحذف الملفات الصوتية الأصلية."))},
            confirmButton={TextButton(onClick={
                if(playlist>=0L){db.deletePlaylist(playlist);tab="playlists";onChanged()}
                confirmPlaylistDelete=false
            }){Text(UiLanguage.text("حذف القائمة"))}},
            dismissButton={TextButton(onClick={confirmPlaylistDelete=false}){Text(UiLanguage.text("إلغاء"))}}
        )
    }
    if(creating){
        AlertDialog(onDismissRequest={creating=false},title={Text(if(playlistForEdit>=0)UiLanguage.text("تعديل اسم القائمة") else UiLanguage.text("قائمة تشغيل جديدة"))},
            text={OutlinedTextField(value=playlistName,onValueChange={playlistName=it.take(100)},
                singleLine=true,label={Text(UiLanguage.text("اسم القائمة"))})},
            confirmButton={TextButton(onClick={
                if(playlistName.isNotBlank()){
                    if(playlistForEdit>=0)db.renamePlaylist(playlistForEdit,playlistName)
                    else {
                        val id=db.createPlaylist(playlistName)
                        if(id>0)adding?.let{db.addToPlaylist(id,it.map{t->t.uri})}
                    }
                    onChanged();creating=false;adding=null;playlistName="";playlistForEdit=-1
                }
            }){Text(UiLanguage.text("حفظ"))}},
            dismissButton={TextButton(onClick={creating=false}){Text(UiLanguage.text("إلغاء"))}})
    }
    if(adding!=null && !creating){
        AlertDialog(onDismissRequest={adding=null},
            title={Text(UiLanguage.text("إضافة إلى قائمة تشغيل"))},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(UiLanguage.text("${adding?.size ?:0} مقطع محدد"))
                    OutlinedButton(onClick={playlistName="";playlistForEdit=-1;creating=true}){Text(UiLanguage.text("+ إنشاء قائمة جديدة"))}
                    playlists.forEach{p->
                        TextButton(onClick={
                            db.addToPlaylist(p.id,adding.orEmpty().map{it.uri})
                            onChanged(); adding=null;selected.clear();selection=false
                        }){Text("${p.name}  (${p.count})")}
                    }
                }
            },confirmButton={TextButton(onClick={adding=null}){Text(UiLanguage.text("إغلاق"))}})
    }
    if(showPlayer && MusicRuntime.nowPlaying != null){
        ModalBottomSheet(
            onDismissRequest={showPlayer=false},
            containerColor=Color(0xFF242647),
            contentColor=Color.White
        ){
            val maxSheetHeight=(LocalConfiguration.current.screenHeightDp*0.78f).dp
            Column(Modifier.fillMaxWidth().heightIn(max=maxSheetHeight)
                .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom=12.dp)){
                PlayerBar(
                    service=service,
                    expanded=true,
                    onExpand={showPlayer=false},
                    onShare={onShare(listOf(it))},
                    onImportLyrics=onImportLyrics
                )
            }
        }
    }
    Scaffold(
        containerColor=deepBlue,
        topBar={CenterAlignedTopAppBar(
            colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=deepBlue),
            title={Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text("ProTech Music",fontWeight=FontWeight.Bold,color=aqua,
                    fontSize=if(isCompact)18.sp else 22.sp,maxLines=1)
                Text(UiLanguage.text("مشغل الصوتيات • ALGHAD DIGITAL"),
                    style=MaterialTheme.typography.labelSmall,color=Color.LightGray,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
            }},
            actions={
                TextButton(onClick=onToggleLanguage,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)){
                    Text(if(UiLanguage.english) "عربي" else "EN",color=aqua)
                }
                IconButton(onClick=onRescan){Text("⟳",color=aqua,style=MaterialTheme.typography.headlineSmall)}
            }
        )},
        bottomBar={
            if(!isLandscape) Column(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF202243))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if(MusicRuntime.nowPlaying!=null){
                    PlayerBar(service=service,expanded=false,onExpand={showPlayer=true},onShare={onShare(listOf(it))},onImportLyrics=onImportLyrics)
                }
                NavigationBar(
                    modifier=Modifier.fillMaxWidth(),
                    containerColor=Color(0xFF202243),
                    windowInsets=WindowInsets(0,0,0,0)
                ) {
                    listOf(Triple("all",UiLanguage.text("الكل"),"♫"),Triple("sources",UiLanguage.text("المصادر"),"▦"),
                        Triple("playlists",UiLanguage.text("قوائمي"),"≡"),Triple("favorites",UiLanguage.text("المفضلة"),"♥"),
                        Triple("effects",UiLanguage.text("الصوت"),"◉")).forEach{item->
                        NavigationBarItem(selected=(tab==item.first)||
                                (tab=="playlist"&&item.first=="playlists")||
                                ((tab=="source"||tab=="folders"||tab=="folder"||
                                    tab=="artists"||tab=="artist"||tab=="albums"||tab=="album")&&item.first=="sources"),
                            onClick={tab=item.first;search="";selection=false;selected.clear();showPlayer=false},
                            icon={Text(item.third,fontSize=20.sp,
                                maxLines=1,modifier=Modifier.semantics { contentDescription=item.second })},
                            label={if(!isCompact && configuration.fontScale<=1.3f)
                                Text(item.second,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)},
                            alwaysShowLabel=(!isCompact && configuration.fontScale<=1.3f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    ){inner ->
        Row(Modifier.padding(inner).fillMaxSize()){
            if(isLandscape){
                Column(Modifier.width(80.dp).fillMaxHeight()
                    .background(Color(0xFF202243)).verticalScroll(rememberScrollState()),
                    horizontalAlignment=Alignment.CenterHorizontally){
                    listOf(Triple("all",UiLanguage.text("الكل"),"♫"),Triple("sources",UiLanguage.text("المصادر"),"▦"),
                        Triple("playlists",UiLanguage.text("قوائمي"),"≡"),Triple("favorites",UiLanguage.text("المفضلة"),"♥"),
                        Triple("effects",UiLanguage.text("الصوت"),"◉")).forEach{ item ->
                        Column(Modifier.fillMaxWidth().clickable{
                            tab=item.first;search="";selection=false;selected.clear();showPlayer=false
                        }.padding(vertical=6.dp,horizontal=3.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Text(item.third,color=if(tab==item.first)aqua else Color.White,fontSize=19.sp)
                            Text(UiLanguage.text(item.second),fontSize=10.sp,maxLines=1,
                                overflow=TextOverflow.Ellipsis,color=if(tab==item.first)aqua else Color.LightGray)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal=if(isCompact)6.dp else 12.dp)){
            if(!allowed){
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF292B58)),
                    modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)) {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Text(UiLanguage.text("السماح بقراءة الصوتيات"),fontWeight=FontWeight.Bold)
                        Text(UiLanguage.text("امنح صلاحية الصوتيات لعرض جميع الملفات الصوتية المتاحة، بما فيها مقاطع WhatsApp والتنزيلات والتسجيلات."))
                        Button(onClick=onGrant){Text(UiLanguage.text("منح صلاحية الصوتيات"))}
                        OutlinedButton(onClick=onPick){Text(UiLanguage.text("اختيار ملفات يدويًا"))}
                    }
                }
            }
            if(error.isNotBlank())Text(error,color=Color(0xFFFFA3A3))
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=aqua)
            if(tab=="all" || tab=="sources" || tab=="source" || tab=="folders" ||
                tab=="folder" || tab=="smart" || tab=="artists" || tab=="artist" || tab=="albums" || tab=="album"){
                if(!isLandscape || showLandscapeFilters) {
                    if(isCompact) {
                        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),
                            horizontalArrangement=Arrangement.spacedBy(8.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            AssistChip(
                                onClick={tab="all"},
                                label={Text(if(UiLanguage.english) "All (${songs.size})" else "الكل (${songs.size})",
                                    maxLines=1,overflow=TextOverflow.Ellipsis)},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp)
                            )
                            Box {
                                OutlinedButton(
                                    onClick={showCompactActions=true},
                                    modifier=Modifier.heightIn(min=48.dp)
                                ) { Text(if(UiLanguage.english) "More" else "المزيد") }
                                DropdownMenu(expanded=showCompactActions,
                                    onDismissRequest={showCompactActions=false}) {
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Import" else "استيراد")},
                                        onClick={showCompactActions=false;onPick()})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Queue" else "قائمة الانتظار")},
                                        onClick={showCompactActions=false;tab="queue"})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Recently added" else "أضيفت حديثًا")},
                                        onClick={showCompactActions=false;tab="smart";smartFilter="recentlyAdded";selection=false;selected.clear()})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Sources" else "المصادر")},
                                        onClick={showCompactActions=false;tab="sources"})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Folders" else "المجلدات")},
                                        onClick={showCompactActions=false;tab="folders"})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Artists" else "الفنانون")},
                                        onClick={showCompactActions=false;tab="artists"})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Albums" else "الألبومات")},
                                        onClick={showCompactActions=false;tab="albums"})
                                    DropdownMenuItem(text={Text(if(UiLanguage.english) "Select" else "تحديد")},
                                        onClick={showCompactActions=false;selection=!selection;selected.clear()})
                                }
                            }
                        }
                    } else Row(horizontalArrangement=Arrangement.spacedBy(6.dp),
                    modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=8.dp)) {
                    AssistChip(onClick=onPick,label={Text(UiLanguage.text("+ استيراد"))})
                    AssistChip(onClick={tab="all"},label={Text(UiLanguage.text("جميع الصوتيات (${songs.size})"))})
                    AssistChip(onClick={tab="queue"},label={Text(UiLanguage.text("قائمة الانتظار (${queuedTracks.size})"))})
                    AssistChip(onClick={tab="smart";smartFilter="recentlyAdded";selection=false;selected.clear()},label={Text(UiLanguage.text("قوائم ذكية"))})
                    AssistChip(onClick={tab="sources"},label={Text(UiLanguage.text("حسب التطبيق"))})
                    AssistChip(onClick={tab="folders"},label={Text(UiLanguage.text("حسب المجلد"))})
                    AssistChip(onClick={tab="artists"},label={Text(UiLanguage.text("الفنانون"))})
                    AssistChip(onClick={tab="albums"},label={Text(UiLanguage.text("الألبومات"))})
                    AssistChip(onClick={selection=!selection;selected.clear()},label={Text(if(selection)UiLanguage.text("إلغاء التحديد") else UiLanguage.text("تحديد متعدد"))})
                }
                }
                if(tab=="source"){
                    TextButton(onClick={tab="sources";selection=false;selected.clear()}){
                        Text(UiLanguage.text("‹ الرجوع إلى المصادر"))
                    }
                }
            }
            if(tab=="playlists"){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text(UiLanguage.text("قوائم التشغيل المخصصة"),modifier=Modifier.weight(1f),maxLines=1,
                        overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Button(onClick={playlistName="";playlistForEdit=-1;adding=null;creating=true}){Text(UiLanguage.text("+ إنشاء"))}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    OutlinedButton(onClick=onExport){Text(UiLanguage.text("تصدير نسخة احتياطية"))}
                    OutlinedButton(onClick=onImport){Text(UiLanguage.text("استيراد نسخة"))}
                    OutlinedButton(onClick=onImportM3u){Text(UiLanguage.text("استيراد M3U8"))}
                }
                LazyColumn {
                    items(playlists,key={it.id}){p->
                        ListItem(headlineContent={Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)},
                            supportingContent={Text(UiLanguage.text("${p.count} مقطع"))},
                            leadingContent={Text("♫",color=aqua)},
                            modifier=Modifier.clickable{tab="playlist";playlist=p.id})
                        HorizontalDivider(color=Color.DarkGray)
                    }
                }
            } else if(tab=="sources"){
                Text(UiLanguage.text("الصوتيات حسب التطبيق والمصدر"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                LazyColumn {
                    items(sourceLabels.filter{(sources[it]?.size?:0)>0},key={it}) { key ->
                        val name=when(key) {
                            "WhatsApp"->UiLanguage.text("واتساب")
                            "Downloads"->UiLanguage.text("التنزيلات")
                            "Recordings"->UiLanguage.text("التسجيلات")
                            "Telegram"->UiLanguage.text("تيليجرام")
                            "Bluetooth"->UiLanguage.text("بلوتوث")
                            "Music"->UiLanguage.text("الموسيقى")
                            "Imported"->UiLanguage.text("المستوردة يدويًا")
                            else->key
                        }
                        ListItem(
                            headlineContent={Text(name,fontWeight=FontWeight.SemiBold)},
                            supportingContent={Text(UiLanguage.text("${sources[key]?.size?:0} مقطع صوتي"))},
                            leadingContent={Text("▣",color=aqua)},
                            trailingContent={Text("‹",color=aqua)},
                            modifier=Modifier.clickable{source=key;search="";tab="source"}
                        )
                        HorizontalDivider(color=Color(0xFF333554))
                    }
                }
            } else if(tab=="artists" || tab=="albums"){
                val isArtist=tab=="artists"
                val names=if(isArtist)artists else albums
                Text(if(isArtist)UiLanguage.text("الفنانون") else UiLanguage.text("الألبومات"),
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                LazyColumn{
                    items(names){name->
                        val count=if(isArtist)songs.count{it.artist.ifBlank{"غير معروف"}==name}
                            else songs.count{it.album.ifBlank{"غير معروف"}==name}
                        ListItem(
                            headlineContent={Text(name,fontWeight=FontWeight.SemiBold)},
                            supportingContent={Text(UiLanguage.text("$count مقطع"))},
                            leadingContent={Text(if(isArtist)"♩" else "▤",color=aqua)},
                            modifier=Modifier.clickable{
                                if(isArtist){artist=name;tab="artist"}
                                else {album=name;tab="album"}
                            }
                        )
                        HorizontalDivider(color=Color(0xFF333554))
                    }
                }
            } else if(tab=="folders"){
                Text(UiLanguage.text("المجلدات (${folders.size})"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                LazyColumn {
                    items(folders){name->
                        ListItem(headlineContent={Text(name)},supportingContent={
                            Text(UiLanguage.text("${songs.count{it.folder==name}} مقطع"))
                        },leadingContent={Text("▣",color=aqua)},
                        modifier=Modifier.clickable{folder=name;tab="folder"})
                        HorizontalDivider(color=Color.DarkGray)
                    }
                }
            } else if(tab=="effects"){
                EffectsPanel(service)
            } else if(tab=="queue"){
                Text(UiLanguage.text("قائمة انتظار التشغيل (${queuedTracks.size})"),
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("اضغط على مقطع لتشغيله. اختر ⋮ لتغيير ترتيبه أو إزالته من قائمة الانتظار.",
                    color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.fillMaxSize()){
                    items(queuedTracks.indices.toList(),key={it}){index->
                        val track=queuedTracks[index]
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            if(index==queueCurrentIndex)Text("▶",color=aqua,modifier=Modifier.padding(4.dp))
                            Box(Modifier.weight(1f)){
                                TrackRow(track=track,selected=false,multiselect=false,
                                    inPlaylist=true,favorite=favorites.contains(track.uri),
                                    onPlay={service?.queuePlay(index)},onSelect={},
                                    onAdd={adding=listOf(track)},onShare={onShare(listOf(track))},
                                    onFavorite={db.toggleFavorite(track.uri);onChanged()},
                                    onRemove={service?.queueRemove(index)},
                                    onUp={service?.queueMove(index,-1)},
                                    onDown={service?.queueMove(index,1)},
                                    onPermanentDelete={confirmPermanentDelete=listOf(track)},
                                    showQueueActions=false)
                            }
                        }
                        HorizontalDivider(color=Color(0xFF333554),thickness=.5.dp)
                    }
                }
            } else {
                if(tab=="playlist"){
                    val p=playlists.firstOrNull{it.id==playlist}
                    Column {
                        Text(p?.name ?: UiLanguage.text("قائمة التشغيل"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            Button(onClick={onPlayGroup(playlistUris.mapNotNull{byUri[it]},false)},enabled=playlistUris.isNotEmpty()){
                                Text(UiLanguage.text("تشغيل القائمة بالترتيب"))
                            }
                            OutlinedButton(onClick={onPlayGroup(playlistUris.mapNotNull{byUri[it]},true)},enabled=playlistUris.isNotEmpty()){
                                Text(UiLanguage.text("تشغيل القائمة عشوائيًا"))
                            }
                            OutlinedButton(onClick={onExportM3u(playlist)},enabled=playlistUris.isNotEmpty()){
                                Text(UiLanguage.text("تصدير M3U8"))
                            }
                            OutlinedButton(onClick={onShare(playlistUris.mapNotNull{byUri[it]})},enabled=playlistUris.isNotEmpty()){
                                Text(UiLanguage.text("مشاركة القائمة"))
                            }
                            TextButton(onClick={playlistName=p?.name.orEmpty();playlistForEdit=playlist;creating=true}){Text(UiLanguage.text("تعديل"))}
                            TextButton(onClick={confirmPlaylistDelete=true}){Text(UiLanguage.text("حذف القائمة فقط"))}
                            OutlinedButton(onClick={selection=!selection;selected.clear()}){
                                Text(if(selection)UiLanguage.text("إلغاء التحديد") else UiLanguage.text("تحديد مقاطع"))
                            }
                        }
                    }
                } else if(tab=="smart"){
                    Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(UiLanguage.text("قوائم التشغيل الذكية"),style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(UiLanguage.text("ترتيب تلقائي حسب إضافة الملفات والاستماع الفعلي"),
                            color=Color.LightGray,style=MaterialTheme.typography.bodySmall,maxLines=1,
                            overflow=TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            listOf("recentlyAdded" to UiLanguage.text("المضافة حديثًا"),
                                "recentlyPlayed" to UiLanguage.text("المشغلة مؤخرًا"),
                                "mostPlayed" to UiLanguage.text("الأكثر تشغيلًا"),
                                "favorites" to UiLanguage.text("المفضلة")).forEach{(id,label)->
                                FilterChip(selected=smartFilter==id,
                                    onClick={smartFilter=id;search="";selected.clear()},
                                    label={Text(label)})
                            }
                            OutlinedButton(onClick={selection=!selection;selected.clear()}){
                                Text(if(selection)UiLanguage.text("إلغاء التحديد") else UiLanguage.text("تحديد مقاطع"))
                            }
                        }
                        if(smartFilter=="mostPlayed"){
                            Text(UiLanguage.text("يُحتسب التشغيل بعد سماع جزء كافٍ من المقطع، وليس بمجرد النقر عليه."),
                                color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if(tab=="favorites"){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text(UiLanguage.text("المفضلة ♥"),style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        TextButton(onClick={selection=!selection;selected.clear()}){
                            Text(if(selection)UiLanguage.text("إلغاء التحديد") else UiLanguage.text("تحديد"))
                        }
                    }
                } else if(tab=="source"){
                    Text(UiLanguage.text("المصدر: $source (${filtered.size})"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="folder"){
                    Text(UiLanguage.text("المجلد: $folder"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="artist"){
                    Text(UiLanguage.text("الفنان: $artist"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="album"){
                    Text(UiLanguage.text("الألبوم: $album"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="smart"){
                    Text(UiLanguage.text("النتائج: ")+filtered.size,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(!isLandscape) Text(UiLanguage.text("كل الصوتيات (${songs.size})"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                if(isLandscape) TextButton(onClick={showLandscapeFilters=!showLandscapeFilters}){
                    Text(UiLanguage.text(if(showLandscapeFilters) "إخفاء البحث والفلاتر" else "البحث والفلاتر"))
                }
                if(!isLandscape || showLandscapeFilters) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically){
                    OutlinedTextField(value=search,onValueChange={search=it},
                        label={Text(UiLanguage.text("بحث في الصوتيات"),maxLines=1)},singleLine=true,
                        modifier=Modifier.weight(1f).padding(vertical=6.dp))
                    Box{
                        OutlinedButton(onClick={showSortMenu=true},modifier=Modifier.heightIn(min=48.dp)){
                            Text(UiLanguage.text("فرز"))
                        }
                        DropdownMenu(expanded=showSortMenu,onDismissRequest={showSortMenu=false}){
                            listOf(
                                TrackSearchEngine.Order.ORIGINAL to UiLanguage.text("ترتيب القائمة"),
                                TrackSearchEngine.Order.TITLE to UiLanguage.text("اسم المقطع"),
                                TrackSearchEngine.Order.ARTIST to UiLanguage.text("الفنان"),
                                TrackSearchEngine.Order.SOURCE to UiLanguage.text("المصدر"),
                                TrackSearchEngine.Order.RECENTLY_ADDED to UiLanguage.text("الأحدث إضافة"),
                                TrackSearchEngine.Order.DURATION to UiLanguage.text("المدة (الأطول)"),
                                TrackSearchEngine.Order.SIZE to UiLanguage.text("الحجم (الأكبر)")
                            ).forEach{ (value,label) ->
                                DropdownMenuItem(text={Text(label)},onClick={
                                    searchSort=value
                                    showSortMenu=false
                                })
                            }
                        }
                    }
                }
                if(selection){
                    val chosen=selected.mapNotNull{byUri[it]}
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={
                            filtered.forEach{ if(it.uri !in selected)selected.add(it.uri) }
                        },enabled=filtered.isNotEmpty()){Text(UiLanguage.text("تحديد الكل (")+filtered.size+")")}
                        TextButton(onClick={selected.clear()}){Text(UiLanguage.text("إلغاء التحديد"))}
                        Text(chosen.size.toString()+UiLanguage.text(" محدد"),color=aqua,
                            modifier=Modifier.align(Alignment.CenterVertically))
                    }
                    if(chosen.isNotEmpty()){
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={adding=chosen}){Text(UiLanguage.text("إضافة إلى قائمة"))}
                            OutlinedButton(onClick={onShare(chosen)}){Text(UiLanguage.text("مشاركة"))}
                            OutlinedButton(onClick={onPlayGroup(chosen,false)}){Text(UiLanguage.text("تشغيل بالترتيب"))}
                            OutlinedButton(onClick={onPlayGroup(chosen,true)}){Text(UiLanguage.text("تشغيل عشوائي"))}
                            OutlinedButton(onClick={confirmPermanentDelete=chosen}){
                                Text(UiLanguage.text("حذف نهائي"),color=Color(0xFFFFAAAA))
                            }
                        }
                    }
                }
                if(filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(28.dp),contentAlignment=Alignment.Center){
                        Text(if(songs.isEmpty())UiLanguage.text("لا توجد صوتيات متاحة. جرّب الاستيراد أو منح الصلاحيات.") else UiLanguage.text("لا توجد نتائج"),color=Color.LightGray)
                    }
                }
                LazyColumn(Modifier.fillMaxSize()){
                    items(filtered.take(visibleLimit),key={it.uri}){track->
                        val isSelected=selected.contains(track.uri)
                        TrackRow(track=track,selected=isSelected,multiselect=selection,
                            inPlaylist=tab=="playlist",favorite=favorites.contains(track.uri),
                            onPlay={onPlay(track,filtered)},
                            onSelect={if(isSelected)selected.remove(track.uri) else selected.add(track.uri)},
                            onAdd={adding=listOf(track)},
                            onShare={onShare(listOf(track))},
                            onFavorite={db.toggleFavorite(track.uri);onChanged()},
                            onRemove={db.removeFromPlaylist(playlist,track.uri);onChanged()},
                            onUp={db.moveInPlaylist(playlist,track.uri,-1);onChanged()},
                            onDown={db.moveInPlaylist(playlist,track.uri,1);onChanged()},
                            onPlayNext={service?.playNext(track)},
                            onEnqueue={service?.enqueue(track)},
                            onPermanentDelete={confirmPermanentDelete=listOf(track)},
                            extraInfo=if(tab=="smart" && smartFilter=="mostPlayed")
                                (playStats[track.uri]?.count ?:0).toString()+" تشغيل"
                            else if(tab=="smart" && smartFilter=="recentlyPlayed")
                                playStats[track.uri]?.lastPlayedAt?.let{
                                    java.text.SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault())
                                        .format(java.util.Date(it))
                                }
                            else null)
                        HorizontalDivider(color=Color(0xFF333554),thickness=.5.dp)
                    }
                    if(visibleLimit<filtered.size) item(key="more-results") {
                        Column(Modifier.fillMaxWidth().padding(vertical=12.dp),
                            horizontalAlignment=Alignment.CenterHorizontally){
                            Text(UiLanguage.text("عرض ${minOf(visibleLimit,filtered.size)} من ${filtered.size}"),
                                color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                OutlinedButton(onClick={visibleLimit=(visibleLimit+200).coerceAtMost(filtered.size)}){Text(UiLanguage.text("عرض المزيد"))}
                                TextButton(onClick={visibleLimit=filtered.size}){Text(UiLanguage.text("عرض الكل"))}
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TrackRow(track:AudioTrack,selected:Boolean,multiselect:Boolean,inPlaylist:Boolean,
    favorite:Boolean,onPlay:()->Unit,onSelect:()->Unit,onAdd:()->Unit,onShare:()->Unit,onFavorite:()->Unit,
    onRemove:()->Unit,onUp:()->Unit,onDown:()->Unit,
    onPlayNext:()->Unit={},onEnqueue:()->Unit={},onPermanentDelete:()->Unit={},
    showQueueActions:Boolean=true,extraInfo:String?=null){
    var menuExpanded by remember(track.uri){mutableStateOf(false)}
    Row(
        Modifier.fillMaxWidth().heightIn(min=72.dp).padding(vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)
    ){
        if(multiselect){
            Checkbox(checked=selected,onCheckedChange={onSelect()},modifier=Modifier.size(48.dp))
        } else {
            Box(
                Modifier.size(48.dp)
                    .background(Brush.linearGradient(listOf(purple,aqua)),RoundedCornerShape(12.dp))
                    .clickable(onClick=onPlay)
                    .semantics{contentDescription=UiLanguage.text("تشغيل ${track.title}")},
                contentAlignment=Alignment.Center
            ){Text("▶",color=deepBlue,fontWeight=FontWeight.Bold)}
        }
        Column(
            Modifier.weight(1f).heightIn(min=48.dp)
                .clickable(onClick=if(multiselect)onSelect else onPlay),
            verticalArrangement=Arrangement.Center
        ){
            Text(track.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)
            Text(
                (track.artist.takeIf{it.isNotBlank()}?.let{"$it • "} ?: "")+
                    "${track.source} • ${readableTime(track.durationMs)}" +
                    (extraInfo?.let{" • "+it} ?: ""),
                maxLines=1,overflow=TextOverflow.Ellipsis,color=Color.LightGray,
                style=MaterialTheme.typography.bodySmall
            )
        }
        IconButton(
            onClick=onAdd,
            modifier=Modifier.size(48.dp).semantics{contentDescription=UiLanguage.text("إضافة ${track.title} إلى قائمة")}
        ){Text("+",color=aqua,style=MaterialTheme.typography.headlineSmall)}
        Box{
            IconButton(
                onClick={menuExpanded=true},
                modifier=Modifier.size(48.dp).semantics{contentDescription=UiLanguage.text("خيارات ${track.title}")}
            ){Text("⋮",color=Color.White,fontSize=26.sp)}
            DropdownMenu(
                expanded=menuExpanded,
                onDismissRequest={menuExpanded=false}
            ){
                DropdownMenuItem(
                    text={Text(if(favorite)UiLanguage.text("إزالة من المفضلة") else UiLanguage.text("إضافة إلى المفضلة"))},
                    onClick={menuExpanded=false;onFavorite()}
                )
                DropdownMenuItem(
                    text={Text(UiLanguage.text("مشاركة المقطع"))},
                    onClick={menuExpanded=false;onShare()}
                )
                if(showQueueActions){
                    DropdownMenuItem(text={Text(UiLanguage.text("تشغيل التالي"))},
                        onClick={menuExpanded=false;onPlayNext()})
                    DropdownMenuItem(text={Text(UiLanguage.text("إضافة إلى قائمة الانتظار"))},
                        onClick={menuExpanded=false;onEnqueue()})
                }
                DropdownMenuItem(
                    text={Text(UiLanguage.text("حذف الملف نهائيًا من الجهاز"),color=Color(0xFFFFAAAA))},
                    onClick={menuExpanded=false;onPermanentDelete()}
                )
                if(inPlaylist){
                    HorizontalDivider()
                    DropdownMenuItem(text={Text(UiLanguage.text("تحريك لأعلى"))},onClick={menuExpanded=false;onUp()})
                    DropdownMenuItem(text={Text(UiLanguage.text("تحريك لأسفل"))},onClick={menuExpanded=false;onDown()})
                    DropdownMenuItem(
                        text={Text(UiLanguage.text("إزالة من القائمة فقط"),color=Color(0xFFFFB0B0))},
                        onClick={menuExpanded=false;onRemove()}
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(service:MusicService?,expanded:Boolean,onExpand:()->Unit,onShare:(AudioTrack)->Unit,onImportLyrics:(AudioTrack)->Unit){
    val track=MusicRuntime.nowPlaying ?:return
    val context=LocalContext.current.applicationContext
    val lyricVersion=MusicRuntime.lyricsRevision
    val lyrics by produceState(initialValue=emptyList<TimedLyric>(),track.uri,lyricVersion){
        value=LyricsStore(context).load(track.uri)
    }
    val cover by produceState<android.graphics.Bitmap?>(initialValue=null,track.uri,expanded){
        value=if(expanded) withContext(Dispatchers.IO){CoverArt.read(context,track.uri)} else null
    }
    Surface(color=Color(0xFF2B2857),contentColor=Color.White,tonalElevation=5.dp){
        Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f).clickable(onClick=onExpand)){
                    Text(track.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(if(expanded)track.artist else if(MusicRuntime.playing)
                        (if(UiLanguage.english)"Playing • Open player" else "قيد التشغيل • افتح المشغل")
                        else (if(UiLanguage.english)"Paused • Tap ▶ to resume" else "متوقف مؤقتًا • ▶ للاستئناف"),
                        style=MaterialTheme.typography.bodySmall,color=Color.LightGray,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick={service?.previous()},
                    modifier=Modifier.size(56.dp).background(Color(0xFF393861),RoundedCornerShape(16.dp))
                        .semantics{contentDescription=if(UiLanguage.english)"Previous track" else "المقطع السابق"},
                    enabled=service!=null){Text("⏮",fontSize=29.sp,color=Color.White)}
                Spacer(Modifier.width(4.dp))
                Button(onClick={if(MusicRuntime.playing)service?.pause() else service?.resume()},
                    modifier=Modifier.size(68.dp)
                        .semantics{contentDescription=if(MusicRuntime.playing)
                            (if(UiLanguage.english)"Pause playback" else "إيقاف مؤقت")
                            else (if(UiLanguage.english)"Resume playback" else "استئناف التشغيل")},
                    enabled=service!=null,
                    shape=RoundedCornerShape(22.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=aqua,contentColor=deepBlue),
                    contentPadding=PaddingValues(0.dp)){
                    Text(if(MusicRuntime.playing)"⏸" else "▶",fontSize=33.sp,fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick={service?.next()},
                    modifier=Modifier.size(56.dp).background(Color(0xFF393861),RoundedCornerShape(16.dp))
                        .semantics{contentDescription=if(UiLanguage.english)"Next track" else "المقطع التالي"},
                    enabled=service!=null){Text("⏭",fontSize=29.sp,color=Color.White)}
            }
            if(expanded){
                Box(Modifier.fillMaxWidth().padding(vertical=6.dp),contentAlignment=Alignment.Center){
                    if(cover!=null) Image(cover!!.asImageBitmap(),UiLanguage.text("غلاف المقطع"),Modifier.size(144.dp),contentScale=ContentScale.Crop)
                    else Box(Modifier.size(112.dp).background(deepBlue,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){
                        Text("♪",fontSize=52.sp,color=aqua)
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(UiLanguage.text("كلمات الأغنية LRC"),Modifier.weight(1f),fontWeight=FontWeight.Medium)
                    TextButton(onClick={onImportLyrics(track)}){Text(UiLanguage.text("استيراد LRC"))}
                }
                if(lyrics.isNotEmpty()){
                    val index=LrcLyrics.activeIndex(lyrics,MusicRuntime.position)
                    Text(lyrics.getOrNull(index)?.text ?: lyrics.first().text,color=aqua,
                        modifier=Modifier.fillMaxWidth().padding(bottom=6.dp),maxLines=3)
                } else Text(UiLanguage.text("يمكن اختيار ملف LRC متزامن مع هذا المقطع"),color=Color.LightGray,
                    style=MaterialTheme.typography.bodySmall)
                val end=MusicRuntime.duration.coerceAtLeast(1L)
                var drag by remember{mutableFloatStateOf(-1f)}
                Slider(value=if(drag>=0)drag else (MusicRuntime.position.toFloat()/end).coerceIn(0f,1f),
                    onValueChange={drag=it},onValueChangeFinished={service?.seek((drag*end).toLong());drag=-1f})
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(readableTime(MusicRuntime.position),style=MaterialTheme.typography.labelSmall)
                    Text(readableTime(MusicRuntime.duration),style=MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth().padding(top=4.dp),
                    horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    OutlinedButton(onClick={service?.seekRelative(-10_000L)},
                        modifier=Modifier.weight(1f).heightIn(min=48.dp),enabled=service!=null){
                        Text(if(UiLanguage.english)"−10 sec" else "−10 ثوانٍ")
                    }
                    OutlinedButton(onClick={service?.seekRelative(10_000L)},
                        modifier=Modifier.weight(1f).heightIn(min=48.dp),enabled=service!=null){
                        Text(if(UiLanguage.english)"+10 sec" else "+10 ثوانٍ")
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    TextButton(onClick={onShare(track)}){Text(UiLanguage.text("مشاركة المقطع"))}
                    TextButton(onClick={service?.changeShuffle()}){Text(if(MusicRuntime.shuffle)UiLanguage.text("خلط ✓") else UiLanguage.text("خلط"))}
                    TextButton(onClick={service?.changeRepeat()}){Text(when(MusicRuntime.repeat){1->UiLanguage.text("تكرار 1");2->UiLanguage.text("تكرار الكل");else->UiLanguage.text("بدون تكرار")})}
                    TextButton(onClick={service?.changeSpeed(if(MusicRuntime.speed>=2f)1f else MusicRuntime.speed+.25f)}){
                        Text("${MusicRuntime.speed}×")
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectsPanel(service:MusicService?){
    var sleepMinutes by remember{mutableIntStateOf(0)}
    var selectedPreset by remember{mutableStateOf("")}
    val presets=remember(service){service?.eqPresets().orEmpty()}
    val eqRevision=MusicRuntime.eqRevision
    val support=remember(service,eqRevision){service?.effectsSupport()}
    val eqBands=remember(service,eqRevision){service?.eqBands().orEmpty()}
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp),
        contentPadding=PaddingValues(vertical=12.dp)){
        item{Text(UiLanguage.text("مضخم الصوت والمؤثرات"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall,color=aqua)}
        item{Text(UiLanguage.text("المؤثرات تعمل على جلسة هذا التطبيق فقط. تضخيم الصوت قد يسبب تشويهًا أو ضررًا للسمع؛ ابدأ بمستوى منخفض."),color=Color(0xFFFFCF91))}
        item{Text(if(service==null)UiLanguage.text("شغّل مقطعًا لتهيئة جلسة الصوت") else "دعم هذا الجهاز: مضخم ${if(support?.loudness==true)"✓" else "غير متاح"} • Bass ${if(support?.bass==true)"✓" else "غير متاح"} • EQ ${if(support?.equalizer==true)"✓" else "غير متاح"}",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)}
        item {
            Text(UiLanguage.text("مضخم الصوت / Loudness (${MusicRuntime.boost}%)"))
            Slider(value=MusicRuntime.boost.toFloat(),onValueChange={service?.setBoost(it.toInt())},valueRange=0f..100f,enabled=support?.loudness==true)
            Text(UiLanguage.text("الحد البرمجي: +6 dB؛ قد يكون التأثير غير متاح على بعض الأجهزة."),style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
        }
        item {
            Text(UiLanguage.text("تعزيز الجهير Bass Boost (${MusicRuntime.bass}%)"))
            Slider(value=MusicRuntime.bass.toFloat(),onValueChange={service?.setBass(it.toInt())},valueRange=0f..100f,enabled=support?.bass==true)
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(UiLanguage.text("Equalizer (حسب دعم الجهاز)"),modifier=Modifier.weight(1f))
                Switch(checked=support?.equalizer==true && MusicRuntime.equalizerEnabled,onCheckedChange={service?.enableEqualizer(it)},enabled=support?.equalizer==true)
            }
            Text(service?.equalizerInfo() ?: UiLanguage.text("المؤثرات بانتظار اتصال المحرك"),style=MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                presets.forEachIndexed{idx,name->
                    FilterChip(selected=selectedPreset==name,onClick={selectedPreset=name;service?.applyEqPreset(idx.toShort())},label={Text(name)})
                }
            }
        }
        item {
            Text(UiLanguage.text("ضبط الترددات يدويًا"),fontWeight=FontWeight.Bold)
            if(eqBands.isEmpty()){
                Text(UiLanguage.text("الضبط اليدوي غير مدعوم على هذا الجهاز"),color=Color.LightGray)
            } else {
                eqBands.forEach { band ->
                    val hz=if(band.centerHz>=1000)
                        String.format(Locale.US,"%.1f kHz",band.centerHz/1000f)
                        else "${band.centerHz} Hz"
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(hz,color=aqua)
                        Text(String.format(Locale.US,"%+.1f dB",band.levelMb/100f),color=Color.LightGray)
                    }
                    Slider(
                        value=band.levelMb.toFloat(),
                        onValueChange={service?.setEqBand(band.index,it.toInt())},
                        valueRange=band.minMb.toFloat()..band.maxMb.toFloat()
                    )
                }
                Text(UiLanguage.text("نطاق الضبط محدود إلى ±6 dB لتقليل التشويه"),style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
            }
        }
        item {
            Text(UiLanguage.text("مؤقت النوم"))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf(0,5,15,30,45,60,90).forEach{minutes->
                    FilterChip(selected=sleepMinutes==minutes,onClick={sleepMinutes=minutes;service?.setSleep(minutes)},
                        label={Text(if(minutes==0)UiLanguage.text("إيقاف") else "$minutes دقيقة")})
                }
            }
        }
        item{ Text(UiLanguage.text("مضخم الصوت بحد أقصى +6 dB، ومجال EQ بحد ±6 dB أو أقل إذا فرض الجهاز ذلك. الدعم والتأثير الفعلي يختلفان باختلاف الهاتف والخرج الصوتي."),color=Color.LightGray)}
    }
}
