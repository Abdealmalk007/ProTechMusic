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
    private var songs by mutableStateOf<List<AudioTrack>>(emptyList())
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
                withContext(Dispatchers.IO) {
                    val stream=contentResolver.openOutputStream(uri,"w")
                        ?:throw IllegalStateException("Cannot write backup")
                    stream.use{it.write(db.exportBackup().toByteArray(Charsets.UTF_8))}
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
                val count=withContext(Dispatchers.IO){
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
                    db.restoreBackup(String(bytes,Charsets.UTF_8))
                }
                refreshCounter++
                Toast.makeText(this@MainActivity,"تم استيراد $count قائمة",Toast.LENGTH_SHORT).show()
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
        connected=bindService(Intent(this,MusicService::class.java),listener,BIND_AUTO_CREATE)
        setContent { MaterialTheme(
            colorScheme=darkColorScheme(primary=aqua,secondary=purple,background=deepBlue,
                surface=Color(0xFF1C1D3D),onSurface=Color.White,onBackground=Color.White)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MusicApp(
                    songs=songs,allowed=allowed,busy=busy,error=failure,
                    service=musicService,db=db,revision=refreshCounter,
                    onGrant={grantPermission()},onRescan={refresh()},onPick={pickAudio.launch(arrayOf("audio/*"))},
                    onPlay={selected,from ->
                        val service=musicService
                        if(service!=null){
                            try{startService(Intent(this,MusicService::class.java))}catch(_:Exception){}
                            val idx=from.indexOfFirst{it.uri==selected.uri}.coerceAtLeast(0)
                            service.playQueue(from,idx)
                        }
                    },
                    onShare={shareTracks(it)},
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
            try { songs=repository.scan() }
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
    songs:List<AudioTrack>,allowed:Boolean,busy:Boolean,error:String,
    service:MusicService?,db:MusicStore,revision:Int,
    onGrant:()->Unit,onRescan:()->Unit,onPick:()->Unit,
    onPlay:(AudioTrack,List<AudioTrack>)->Unit,
    onShare:(List<AudioTrack>)->Unit,
    onDelete:(List<AudioTrack>)->Unit,
    onPlayGroup:(List<AudioTrack>,Boolean)->Unit,
    onExport:()->Unit,onImport:()->Unit,
    onExportM3u:(Long)->Unit,onImportM3u:()->Unit,
    importReport:String?,onDismissReport:()->Unit,onChanged:()->Unit
){
    var tab by remember{mutableStateOf("all")}
    var source by remember{mutableStateOf("All")}
    var folder by remember{mutableStateOf("")}
    var artist by remember{mutableStateOf("")}
    var album by remember{mutableStateOf("")}
    var playlist by remember{mutableLongStateOf(-1L)}
    var search by remember{mutableStateOf("")}
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
    val playlistUris=remember(revision,playlist){if(playlist>=0)db.playlistTracks(playlist) else emptyList()}
    val sourceOrder=listOf("WhatsApp","Downloads","Recordings","Telegram","Bluetooth","Music","Podcasts","Audiobooks","System sounds","Imported","Other")
    val sources=songs.groupBy{it.source}
    val sourceLabels=sourceOrder+(sources.keys-sourceOrder.toSet()).sorted()
    val folders=remember(songs){songs.map{it.folder}.filter{it.isNotBlank()}.distinct().sorted()}
    val artists=remember(songs){songs.map{it.artist.ifBlank{"غير معروف"}}.distinct().sorted()}
    val albums=remember(songs){songs.map{it.album.ifBlank{"غير معروف"}}.distinct().sorted()}
    val byUri=remember(songs){songs.associateBy{it.uri}}
    val queueRevision=MusicRuntime.queueRevision
    val queuedTracks=remember(service,queueRevision){service?.queueSnapshot().orEmpty()}
    val queueCurrentIndex=service?.queueCurrentIndex() ?: -1
    val filtered=remember(songs,tab,source,folder,artist,album,playlistUris,favorites,search) {
        val base= when(tab) {
            "source" -> songs.filter{it.source==source}
            "playlist" -> playlistUris.mapNotNull{byUri[it]}
            "favorites" -> songs.filter{favorites.contains(it.uri)}
            "folder" -> songs.filter{it.folder==folder}
            "artist" -> songs.filter{it.artist.ifBlank{"غير معروف"}==artist}
            "album" -> songs.filter{it.album.ifBlank{"غير معروف"}==album}
            else -> songs
        }
        base.filter{it.title.contains(search,true) || it.artist.contains(search,true) ||
            it.album.contains(search,true)||it.folder.contains(search,true)}
    }
    if(importReport!=null){
        AlertDialog(onDismissRequest=onDismissReport,title={Text("نتيجة استيراد M3U8")},
            text={Text(importReport)},
            confirmButton={TextButton(onClick=onDismissReport){Text("إغلاق")}})
    }
    if(confirmPermanentDelete.isNotEmpty()){
        val candidates=confirmPermanentDelete
        AlertDialog(
            onDismissRequest={confirmPermanentDelete=emptyList()},
            title={Text("حذف نهائي من الجهاز",color=Color(0xFFFFAAAA))},
            text={Text("سيُحذف "+candidates.size+
                " مقطع صوتي نهائيًا من ذاكرة الجهاز، وليس من قائمة التشغيل فقط. لا يمكن التراجع. سيطلب Android موافقتك، والملفات التي لا يسمح بحذفها ستبقى.")},
            confirmButton={TextButton(onClick={
                confirmPermanentDelete=emptyList()
                onDelete(candidates)
                selected.clear()
                selection=false
            }){Text("تأكيد الحذف النهائي",color=Color(0xFFFF9999))}},
            dismissButton={TextButton(onClick={confirmPermanentDelete=emptyList()}){Text("إلغاء")}}
        )
    }
    if(confirmPlaylistDelete){
        AlertDialog(
            onDismissRequest={confirmPlaylistDelete=false},
            title={Text("تأكيد حذف القائمة")},
            text={Text("سيتم حذف قائمة التشغيل فقط، ولن تُحذف الملفات الصوتية الأصلية.")},
            confirmButton={TextButton(onClick={
                if(playlist>=0L){db.deletePlaylist(playlist);tab="playlists";onChanged()}
                confirmPlaylistDelete=false
            }){Text("حذف القائمة")}},
            dismissButton={TextButton(onClick={confirmPlaylistDelete=false}){Text("إلغاء")}}
        )
    }
    if(creating){
        AlertDialog(onDismissRequest={creating=false},title={Text(if(playlistForEdit>=0)"تعديل اسم القائمة" else "قائمة تشغيل جديدة")},
            text={OutlinedTextField(value=playlistName,onValueChange={playlistName=it.take(100)},
                singleLine=true,label={Text("اسم القائمة")})},
            confirmButton={TextButton(onClick={
                if(playlistName.isNotBlank()){
                    if(playlistForEdit>=0)db.renamePlaylist(playlistForEdit,playlistName)
                    else {
                        val id=db.createPlaylist(playlistName)
                        if(id>0)adding?.let{db.addToPlaylist(id,it.map{t->t.uri})}
                    }
                    onChanged();creating=false;adding=null;playlistName="";playlistForEdit=-1
                }
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={creating=false}){Text("إلغاء")}})
    }
    if(adding!=null && !creating){
        AlertDialog(onDismissRequest={adding=null},
            title={Text("إضافة إلى قائمة تشغيل")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("${adding?.size ?:0} مقطع محدد")
                    OutlinedButton(onClick={playlistName="";playlistForEdit=-1;creating=true}){Text("+ إنشاء قائمة جديدة")}
                    playlists.forEach{p->
                        TextButton(onClick={
                            db.addToPlaylist(p.id,adding.orEmpty().map{it.uri})
                            onChanged(); adding=null;selected.clear();selection=false
                        }){Text("${p.name}  (${p.count})")}
                    }
                }
            },confirmButton={TextButton(onClick={adding=null}){Text("إغلاق")}})
    }
    if(showPlayer && MusicRuntime.nowPlaying != null){
        ModalBottomSheet(
            onDismissRequest={showPlayer=false},
            containerColor=Color(0xFF242647),
            contentColor=Color.White
        ){
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom=12.dp)){
                PlayerBar(
                    service=service,
                    expanded=true,
                    onExpand={showPlayer=false},
                    onShare={onShare(listOf(it))}
                )
            }
        }
    }
    Scaffold(
        containerColor=deepBlue,
        topBar={CenterAlignedTopAppBar(
            colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=deepBlue),
            title={Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text("ProTech Music",fontWeight=FontWeight.Bold,color=aqua)
                Text("مشغل الصوتيات • ALGHAD DIGITAL",style=MaterialTheme.typography.labelSmall,color=Color.LightGray)
            }},
            actions={
                IconButton(onClick=onRescan){Text("⟳",color=aqua,style=MaterialTheme.typography.headlineSmall)}
            }
        )},
        bottomBar={
            Column(
                Modifier.fillMaxWidth()
                    .background(Color(0xFF202243))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if(MusicRuntime.nowPlaying!=null){
                    PlayerBar(service=service,expanded=false,onExpand={showPlayer=true},onShare={onShare(listOf(it))})
                }
                NavigationBar(
                    modifier=Modifier.fillMaxWidth(),
                    containerColor=Color(0xFF202243),
                    windowInsets=WindowInsets(0,0,0,0)
                ) {
                    listOf(Triple("all","الكل","♫"),Triple("sources","المصادر","▦"),
                        Triple("playlists","قوائمي","≡"),Triple("favorites","المفضلة","♥"),
                        Triple("effects","الصوت","◉")).forEach{item->
                        NavigationBarItem(selected=(tab==item.first)||
                                (tab=="playlist"&&item.first=="playlists")||
                                ((tab=="source"||tab=="folders"||tab=="folder"||
                                    tab=="artists"||tab=="artist"||tab=="albums"||tab=="album")&&item.first=="sources"),
                            onClick={tab=item.first;search="";selection=false;selected.clear();showPlayer=false},
                            icon={Text(item.third,fontSize=20.sp,maxLines=1)},
                            label={Text(item.second,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)},
                            alwaysShowLabel=true)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    ){inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal=12.dp)){
            if(!allowed){
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF292B58)),
                    modifier=Modifier.fillMaxWidth().padding(vertical=12.dp)) {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Text("السماح بقراءة الصوتيات",fontWeight=FontWeight.Bold)
                        Text("امنح صلاحية الصوتيات لعرض جميع الملفات الصوتية المتاحة، بما فيها مقاطع WhatsApp والتنزيلات والتسجيلات.")
                        Button(onClick=onGrant){Text("منح صلاحية الصوتيات")}
                        OutlinedButton(onClick=onPick){Text("اختيار ملفات يدويًا")}
                    }
                }
            }
            if(error.isNotBlank())Text(error,color=Color(0xFFFFA3A3))
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=aqua)
            if(tab=="all" || tab=="sources" || tab=="source" || tab=="folders" ||
                tab=="folder" || tab=="artists" || tab=="artist" || tab=="albums" || tab=="album"){
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp),
                    modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=8.dp)) {
                    AssistChip(onClick=onPick,label={Text("+ استيراد")})
                    AssistChip(onClick={tab="all"},label={Text("جميع الصوتيات (${songs.size})")})
                    AssistChip(onClick={tab="queue"},label={Text("قائمة الانتظار (${queuedTracks.size})")})
                    AssistChip(onClick={tab="sources"},label={Text("حسب التطبيق")})
                    AssistChip(onClick={tab="folders"},label={Text("حسب المجلد")})
                    AssistChip(onClick={tab="artists"},label={Text("الفنانون")})
                    AssistChip(onClick={tab="albums"},label={Text("الألبومات")})
                    AssistChip(onClick={selection=!selection;selected.clear()},label={Text(if(selection)"إلغاء التحديد" else "تحديد متعدد")})
                }
                if(tab=="source"){
                    TextButton(onClick={tab="sources";selection=false;selected.clear()}){
                        Text("‹ الرجوع إلى المصادر")
                    }
                }
            }
            if(tab=="playlists"){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text("قوائم التشغيل المخصصة",modifier=Modifier.weight(1f),maxLines=1,
                        overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Button(onClick={playlistName="";playlistForEdit=-1;adding=null;creating=true}){Text("+ إنشاء")}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    OutlinedButton(onClick=onExport){Text("تصدير نسخة احتياطية")}
                    OutlinedButton(onClick=onImport){Text("استيراد نسخة")}
                    OutlinedButton(onClick=onImportM3u){Text("استيراد M3U8")}
                }
                LazyColumn {
                    items(playlists,key={it.id}){p->
                        ListItem(headlineContent={Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)},
                            supportingContent={Text("${p.count} مقطع")},
                            leadingContent={Text("♫",color=aqua)},
                            modifier=Modifier.clickable{tab="playlist";playlist=p.id})
                        HorizontalDivider(color=Color.DarkGray)
                    }
                }
            } else if(tab=="sources"){
                Text("الصوتيات حسب التطبيق والمصدر",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                LazyColumn {
                    items(sourceLabels.filter{(sources[it]?.size?:0)>0},key={it}) { key ->
                        val name=when(key) {
                            "WhatsApp"->"واتساب"
                            "Downloads"->"التنزيلات"
                            "Recordings"->"التسجيلات"
                            "Telegram"->"تيليجرام"
                            "Bluetooth"->"بلوتوث"
                            "Music"->"الموسيقى"
                            "Imported"->"المستوردة يدويًا"
                            else->key
                        }
                        ListItem(
                            headlineContent={Text(name,fontWeight=FontWeight.SemiBold)},
                            supportingContent={Text("${sources[key]?.size?:0} مقطع صوتي")},
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
                Text(if(isArtist)"الفنانون" else "الألبومات",
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                LazyColumn{
                    items(names){name->
                        val count=if(isArtist)songs.count{it.artist.ifBlank{"غير معروف"}==name}
                            else songs.count{it.album.ifBlank{"غير معروف"}==name}
                        ListItem(
                            headlineContent={Text(name,fontWeight=FontWeight.SemiBold)},
                            supportingContent={Text("$count مقطع")},
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
                Text("المجلدات (${folders.size})",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                LazyColumn {
                    items(folders){name->
                        ListItem(headlineContent={Text(name)},supportingContent={
                            Text("${songs.count{it.folder==name}} مقطع")
                        },leadingContent={Text("▣",color=aqua)},
                        modifier=Modifier.clickable{folder=name;tab="folder"})
                        HorizontalDivider(color=Color.DarkGray)
                    }
                }
            } else if(tab=="effects"){
                EffectsPanel(service)
            } else if(tab=="queue"){
                Text("قائمة انتظار التشغيل (${queuedTracks.size})",
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
                        Text(p?.name ?: "قائمة التشغيل",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            Button(onClick={onPlayGroup(playlistUris.mapNotNull{byUri[it]},false)},enabled=playlistUris.isNotEmpty()){
                                Text("تشغيل القائمة بالترتيب")
                            }
                            OutlinedButton(onClick={onPlayGroup(playlistUris.mapNotNull{byUri[it]},true)},enabled=playlistUris.isNotEmpty()){
                                Text("تشغيل القائمة عشوائيًا")
                            }
                            OutlinedButton(onClick={onExportM3u(playlist)},enabled=playlistUris.isNotEmpty()){
                                Text("تصدير M3U8")
                            }
                            OutlinedButton(onClick={onShare(playlistUris.mapNotNull{byUri[it]})},enabled=playlistUris.isNotEmpty()){
                                Text("مشاركة القائمة")
                            }
                            TextButton(onClick={playlistName=p?.name.orEmpty();playlistForEdit=playlist;creating=true}){Text("تعديل")}
                            TextButton(onClick={confirmPlaylistDelete=true}){Text("حذف القائمة فقط")}
                            OutlinedButton(onClick={selection=!selection;selected.clear()}){
                                Text(if(selection)"إلغاء التحديد" else "تحديد مقاطع")
                            }
                        }
                    }
                } else if(tab=="favorites"){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("المفضلة ♥",style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        TextButton(onClick={selection=!selection;selected.clear()}){
                            Text(if(selection)"إلغاء التحديد" else "تحديد")
                        }
                    }
                } else if(tab=="source"){
                    Text("المصدر: $source (${filtered.size})",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="folder"){
                    Text("المجلد: $folder",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="artist"){
                    Text("الفنان: $artist",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else if(tab=="album"){
                    Text("الألبوم: $album",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                } else Text("كل الصوتيات (${songs.size})",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                OutlinedTextField(value=search,onValueChange={search=it},
                    label={Text("بحث عن مقطع أو فنان أو مجلد")},singleLine=true,
                    modifier=Modifier.fillMaxWidth().padding(vertical=6.dp))
                if(selection){
                    val chosen=selected.mapNotNull{byUri[it]}
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={
                            filtered.forEach{ if(it.uri !in selected)selected.add(it.uri) }
                        },enabled=filtered.isNotEmpty()){Text("تحديد الكل ("+filtered.size+")")}
                        TextButton(onClick={selected.clear()}){Text("إلغاء التحديد")}
                        Text(chosen.size.toString()+" محدد",color=aqua,
                            modifier=Modifier.align(Alignment.CenterVertically))
                    }
                    if(chosen.isNotEmpty()){
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={adding=chosen}){Text("إضافة إلى قائمة")}
                            OutlinedButton(onClick={onShare(chosen)}){Text("مشاركة")}
                            OutlinedButton(onClick={onPlayGroup(chosen,false)}){Text("تشغيل بالترتيب")}
                            OutlinedButton(onClick={onPlayGroup(chosen,true)}){Text("تشغيل عشوائي")}
                            OutlinedButton(onClick={confirmPermanentDelete=chosen}){
                                Text("حذف نهائي",color=Color(0xFFFFAAAA))
                            }
                        }
                    }
                }
                if(filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(28.dp),contentAlignment=Alignment.Center){
                        Text(if(songs.isEmpty())"لا توجد صوتيات متاحة. جرّب الاستيراد أو منح الصلاحيات." else "لا توجد نتائج",color=Color.LightGray)
                    }
                }
                LazyColumn(Modifier.fillMaxSize()){
                    items(filtered,key={it.uri}){track->
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
                            onPermanentDelete={confirmPermanentDelete=listOf(track)})
                        HorizontalDivider(color=Color(0xFF333554),thickness=.5.dp)
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
    showQueueActions:Boolean=true){
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
                    .semantics{contentDescription="تشغيل ${track.title}"},
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
                    "${track.source} • ${readableTime(track.durationMs)}",
                maxLines=1,overflow=TextOverflow.Ellipsis,color=Color.LightGray,
                style=MaterialTheme.typography.bodySmall
            )
        }
        IconButton(
            onClick=onAdd,
            modifier=Modifier.size(48.dp).semantics{contentDescription="إضافة ${track.title} إلى قائمة"}
        ){Text("+",color=aqua,style=MaterialTheme.typography.headlineSmall)}
        Box{
            IconButton(
                onClick={menuExpanded=true},
                modifier=Modifier.size(48.dp).semantics{contentDescription="خيارات ${track.title}"}
            ){Text("⋮",color=Color.White,fontSize=26.sp)}
            DropdownMenu(
                expanded=menuExpanded,
                onDismissRequest={menuExpanded=false}
            ){
                DropdownMenuItem(
                    text={Text(if(favorite)"إزالة من المفضلة" else "إضافة إلى المفضلة")},
                    onClick={menuExpanded=false;onFavorite()}
                )
                DropdownMenuItem(
                    text={Text("مشاركة المقطع")},
                    onClick={menuExpanded=false;onShare()}
                )
                if(showQueueActions){
                    DropdownMenuItem(text={Text("تشغيل التالي")},
                        onClick={menuExpanded=false;onPlayNext()})
                    DropdownMenuItem(text={Text("إضافة إلى قائمة الانتظار")},
                        onClick={menuExpanded=false;onEnqueue()})
                }
                DropdownMenuItem(
                    text={Text("حذف الملف نهائيًا من الجهاز",color=Color(0xFFFFAAAA))},
                    onClick={menuExpanded=false;onPermanentDelete()}
                )
                if(inPlaylist){
                    HorizontalDivider()
                    DropdownMenuItem(text={Text("تحريك لأعلى")},onClick={menuExpanded=false;onUp()})
                    DropdownMenuItem(text={Text("تحريك لأسفل")},onClick={menuExpanded=false;onDown()})
                    DropdownMenuItem(
                        text={Text("إزالة من القائمة فقط",color=Color(0xFFFFB0B0))},
                        onClick={menuExpanded=false;onRemove()}
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(service:MusicService?,expanded:Boolean,onExpand:()->Unit,onShare:(AudioTrack)->Unit){
    val track=MusicRuntime.nowPlaying ?:return
    Surface(color=Color(0xFF2B2857),contentColor=Color.White,tonalElevation=5.dp){
        Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f).clickable(onClick=onExpand)){
                    Text(track.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(if(expanded)track.artist else "اضغط لفتح المشغل",style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
                }
                TextButton(onClick={service?.previous()}){Text("⏮")}
                TextButton(onClick={if(MusicRuntime.playing)service?.pause() else service?.resume()}){Text(if(MusicRuntime.playing)"⏸" else "▶")}
                TextButton(onClick={service?.next()}){Text("⏭")}
            }
            if(expanded){
                val end=MusicRuntime.duration.coerceAtLeast(1L)
                var drag by remember{mutableFloatStateOf(-1f)}
                Slider(value=if(drag>=0)drag else (MusicRuntime.position.toFloat()/end).coerceIn(0f,1f),
                    onValueChange={drag=it},onValueChangeFinished={service?.seek((drag*end).toLong());drag=-1f})
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(readableTime(MusicRuntime.position),style=MaterialTheme.typography.labelSmall)
                    Text(readableTime(MusicRuntime.duration),style=MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    TextButton(onClick={onShare(track)}){Text("مشاركة المقطع")}
                    TextButton(onClick={service?.changeShuffle()}){Text(if(MusicRuntime.shuffle)"خلط ✓" else "خلط")}
                    TextButton(onClick={service?.changeRepeat()}){Text(when(MusicRuntime.repeat){1->"تكرار 1";2->"تكرار الكل";else->"بدون تكرار"})}
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
    val eqBands=remember(service,eqRevision){service?.eqBands().orEmpty()}
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp),
        contentPadding=PaddingValues(vertical=12.dp)){
        item{Text("مضخم الصوت والمؤثرات",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall,color=aqua)}
        item{Text("زيادة الصوت تؤثر على تشغيل التطبيق فقط. تجنب المستويات العالية والسماعات القريبة من الأذن.",color=Color(0xFFFFCF91))}
        item {
            Text("مضخم الصوت / Loudness (${MusicRuntime.boost}%)")
            Slider(value=MusicRuntime.boost.toFloat(),onValueChange={service?.setBoost(it.toInt())},valueRange=0f..100f)
            Text("الحد البرمجي: +6 dB؛ قد يكون التأثير غير متاح على بعض الأجهزة.",style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
        }
        item {
            Text("تعزيز الجهير Bass Boost (${MusicRuntime.bass}%)")
            Slider(value=MusicRuntime.bass.toFloat(),onValueChange={service?.setBass(it.toInt())},valueRange=0f..100f)
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Equalizer (حسب دعم الجهاز)",modifier=Modifier.weight(1f))
                Switch(checked=MusicRuntime.equalizerEnabled,onCheckedChange={service?.enableEqualizer(it)})
            }
            Text(service?.equalizerInfo() ?: "المؤثرات بانتظار اتصال المحرك",style=MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                presets.forEachIndexed{idx,name->
                    FilterChip(selected=selectedPreset==name,onClick={selectedPreset=name;service?.applyEqPreset(idx.toShort())},label={Text(name)})
                }
            }
        }
        item {
            Text("ضبط الترددات يدويًا",fontWeight=FontWeight.Bold)
            if(eqBands.isEmpty()){
                Text("الضبط اليدوي غير مدعوم على هذا الجهاز",color=Color.LightGray)
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
                Text("نطاق الضبط محدود إلى ±6 dB لتقليل التشويه",style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
            }
        }
        item {
            Text("مؤقت النوم")
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf(0,5,15,30,45,60,90).forEach{minutes->
                    FilterChip(selected=sleepMinutes==minutes,onClick={sleepMinutes=minutes;service?.setSleep(minutes)},
                        label={Text(if(minutes==0)"إيقاف" else "$minutes دقيقة")})
                }
            }
        }
        item{ Text("يعمل محرك Equalizer وBassBoost وLoudnessEnhancer بالجلسة الصوتية الخاصة بالمشغل، ولا يغيّر مستوى صوت تطبيقات أخرى.",color=Color.LightGray)}
    }
}
