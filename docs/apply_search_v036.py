from pathlib import Path
root=Path(r"E:\ProTech\ProTechMusic")
f=root/"app/src/main/java/digital/alghad/protechmusic/MainActivity.kt"
s=f.read_text(encoding="utf-8-sig")
def replace(old,new,name):
    global s
    n=s.count(old)
    if n!=1:raise Exception(name+" found "+str(n)+" occurrences")
    s=s.replace(old,new)
    print("PATCH",name)
replace('    var search by remember{mutableStateOf("")}', '''    var search by remember{mutableStateOf("")}
    var searchSort by remember{mutableStateOf(TrackSearchEngine.Order.ORIGINAL)}
    var showSortMenu by remember{mutableStateOf(false)}''',"sort state")
replace('    val sources=songs.groupBy{it.source}',
        '    val sources=remember(songs){songs.groupBy{it.source}}',"memoized groups")
replace('    val sourceLabels=sourceOrder+(sources.keys-sourceOrder.toSet()).sorted()',
        '    val sourceLabels=remember(sources){sourceOrder+(sources.keys-sourceOrder.toSet()).sorted()}',"memoized source labels")
replace('    val filtered=remember(songs,tab,smartFilter,playStats,source,folder,artist,album,playlistUris,favorites,search) {',
        '    val filtered=remember(songs,tab,smartFilter,playStats,source,folder,artist,album,playlistUris,favorites,search,searchSort) {',
        "remember explicit sort")
start='''        base.filter{it.title.contains(search,true) || it.artist.contains(search,true) ||
            it.album.contains(search,true)||it.folder.contains(search,true)}
'''
replace(start,'        TrackSearchEngine.search(base,search,searchSort)\n',"unified normalized text query")
# The search field stays in its current accessible location. The sort menu does not overlap miniplayer/nav.
old='''                OutlinedTextField(value=search,onValueChange={search=it},
                    label={Text("بحث عن مقطع أو فنان أو مجلد")},singleLine=true,
                    modifier=Modifier.fillMaxWidth().padding(vertical=6.dp))'''
assert s.count(old)==1,"search field expected exactly once"
new='''                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically){
                    OutlinedTextField(value=search,onValueChange={search=it},
                        label={Text("بحث باسم المقطع أو الفنان أو المصدر")},singleLine=true,
                        modifier=Modifier.weight(1f).padding(vertical=6.dp))
                    Box{
                        OutlinedButton(onClick={showSortMenu=true},modifier=Modifier.heightIn(min=48.dp)){
                            Text("فرز")
                        }
                        DropdownMenu(expanded=showSortMenu,onDismissRequest={showSortMenu=false}){
                            listOf(
                                TrackSearchEngine.Order.ORIGINAL to "ترتيب القائمة",
                                TrackSearchEngine.Order.TITLE to "اسم المقطع",
                                TrackSearchEngine.Order.ARTIST to "الفنان",
                                TrackSearchEngine.Order.SOURCE to "المصدر",
                                TrackSearchEngine.Order.RECENTLY_ADDED to "الأحدث إضافة",
                                TrackSearchEngine.Order.DURATION to "المدة (الأطول)",
                                TrackSearchEngine.Order.SIZE to "الحجم (الأكبر)"
                            ).forEach{ (value,label) ->
                                DropdownMenuItem(text={Text(label)},onClick={
                                    searchSort=value
                                    showSortMenu=false
                                })
                            }
                        }
                    }
                }'''
replace(old,new,"compact sorting toolbar")
f.write_text(s,encoding="utf-8",newline="\n")
g=root/"app/build.gradle.kts"
v=g.read_text(encoding="utf-8")
assert 'versionCode = 8' in v and 'versionName = "0.3.5"' in v
g.write_text(v.replace('versionCode = 8','versionCode = 9').replace('versionName = "0.3.5"','versionName = "0.3.6"'),encoding="utf-8",newline="\n")
print("DONE v0.3.6")
