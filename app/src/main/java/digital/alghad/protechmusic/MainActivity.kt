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
                    onExport={createBackup.launch("ProTechMusic_backup.json")},
                    onImport={openBackup.launch(arrayOf("application/json","text/plain"))},
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
    onShare:(List<AudioTrack>)->Unit,onExport:()->Unit,onImport:()->Unit,onChanged:()->Unit
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
            Column {
                if(MusicRuntime.nowPlaying!=null){
                    PlayerBar(service=service,expanded=showPlayer,onExpand={showPlayer=!showPlayer},onShare={onShare(listOf(it))})
                }
                NavigationBar(containerColor=Color(0xFF202243)) {
                    listOf(Triple("all","الكل","♫"),Triple("sources","المصادر","▦"),
                        Triple("playlists","قوائمي","≡"),Triple("favorites","المفضلة","♥"),
                        Triple("effects","الصوت","◉")).forEach{item->
                        NavigationBarItem(selected=(tab==item.first)||
                                (tab=="playlist"&&item.first=="playlists")||
                                ((tab=="source"||tab=="folders"||tab=="folder"||
                                    tab=="artists"||tab=="artist"||tab=="albums"||tab=="album")&&item.first=="sources"),
                            onClick={tab=item.first;search="";selection=false;selected.clear();showPlayer=false},
                            icon={Text(item.third)},label={Text(item.second)},alwaysShowLabel=true)
                    }
                }
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
                    Text("قوائم التشغيل المخصصة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Button(onClick={playlistName="";playlistForEdit=-1;adding=null;creating=true}){Text("+ إنشاء")}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    OutlinedButton(onClick=onExport){Text("تصدير نسخة احتياطية")}
                    OutlinedButton(onClick=onImport){Text("استيراد نسخة")}
                }
                LazyColumn {
                    items(playlists,key={it.id}){p->
                        ListItem(headlineContent={Text(p.name,fontWeight=FontWeight.SemiBold)},
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
            } else {
                if(tab=="playlist"){
                    val p=playlists.firstOrNull{it.id==playlist}
                    Column {
                        Text(p?.name ?: "قائمة التشغيل",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(onClick={onShare(playlistUris.mapNotNull{byUri[it]})},enabled=playlistUris.isNotEmpty()){
                                Text("مشاركة القائمة")
                            }
                            TextButton(onClick={playlistName=p?.name.orEmpty();playlistForEdit=playlist;creating=true}){Text("تعديل")}
                            TextButton(onClick={confirmPlaylistDelete=true}){Text("حذف")}
                        }
                    }
                } else if(tab=="favorites"){
                    Text("المفضلة ♥",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
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
                if(selection && selected.isNotEmpty()){
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={adding=selected.mapNotNull{byUri[it]}}){
                            Text("إضافة ${selected.size} إلى قائمة")
                        }
                        OutlinedButton(onClick={onShare(selected.mapNotNull{byUri[it]})}){
                            Text("مشاركة المحدد")
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
                            onDown={db.moveInPlaylist(playlist,track.uri,1);onChanged()})
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
    onRemove:()->Unit,onUp:()->Unit,onDown:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=66.dp).padding(vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        if(multiselect)Checkbox(checked=selected,onCheckedChange={onSelect()})
        else Box(Modifier.size(45.dp).background(Brush.linearGradient(listOf(purple,aqua)),
            RoundedCornerShape(12.dp)).clickable(onClick=onPlay),
            contentAlignment=Alignment.Center){Text("▶",color=deepBlue,fontWeight=FontWeight.Bold)}
        Column(Modifier.weight(1f).clickable(onClick=if(multiselect)onSelect else onPlay)){
            Text(track.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium)
            Text((track.artist.takeIf{it.isNotBlank()}?.let{"$it • "} ?: "")+
                "${track.source} • ${readableTime(track.durationMs)}",
                maxLines=1,overflow=TextOverflow.Ellipsis,color=Color.LightGray,
                style=MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick=onFavorite,modifier=Modifier.size(32.dp)){
            Text(if(favorite)"♥" else "♡",color=if(favorite)Color(0xFFFF659D) else Color.White)
        }
        IconButton(onClick=onAdd,modifier=Modifier.size(32.dp)){
            Text("+",color=aqua,style=MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick=onShare,modifier=Modifier.size(32.dp)){
            Text("↗",color=aqua,style=MaterialTheme.typography.titleLarge)
        }
        if(inPlaylist){
            Column {
                Text("▲",modifier=Modifier.clickable(onClick=onUp),color=aqua)
                Text("▼",modifier=Modifier.clickable(onClick=onDown),color=aqua)
            }
            Text("×",modifier=Modifier.clickable(onClick=onRemove),color=Color(0xFFFFB3B3))
        }
    }
}

@Composable
private fun PlayerBar(service:MusicService?,expanded:Boolean,onExpand:()->Unit,onShare:(AudioTrack)->Unit){
    val track=MusicRuntime.nowPlaying ?:return
    Surface(color=Color(0xFF2B2857),tonalElevation=5.dp){
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
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
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
