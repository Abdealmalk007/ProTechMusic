param(
    [ValidateSet(10000,50000)][int]$Count,
    [switch]$SkipInstall
)
$ErrorActionPreference="Stop"
$adb="E:\ProTech\AndroidToolchain\sdk\platform-tools\adb.exe"
$apk="E:\ProTech\ProTechMusic\app\build\outputs\apk\debug\app-debug.apk"
$root="E:\ProTech\ProTechMusic\docs\evidence\v25"
$pkg="digital.alghad.protechmusic"
$serial="emulator-5554"
New-Item -ItemType Directory -Force -Path $root | Out-Null
$boot=(& $adb -s $serial shell getprop sys.boot_completed|Out-String).Trim()
if($boot -ne "1"){throw "Android emulator not booted"}
$model=(& $adb -s $serial shell getprop ro.product.model|Out-String).Trim()
if($model -notmatch 'sdk_|Emulator'){throw "Refusing physical device"}
if(-not $SkipInstall){
    & $adb -s $serial install --no-streaming -r $apk
    if($LASTEXITCODE -ne 0){throw "QA debug APK install failed"}
}
& $adb -s $serial shell pm grant $pkg android.permission.READ_MEDIA_AUDIO
if($LASTEXITCODE -ne 0){throw "Permission grant failed"}
$base="f14_real_$Count"
& $adb -s $serial shell run-as $pkg rm -f ("files/"+$base+".json") ("files/"+$base+".error")
if($LASTEXITCODE -ne 0){throw "Cannot clear stale QA results"}
& $adb -s $serial shell am start -n ($pkg+"/.F14RealMediaQaActivity") --ei count $Count --ez scanOnly true
if($LASTEXITCODE -ne 0){throw "QA Activity failed to launch"}
Write-Output ("QA_SCAN_ONLY_STARTED="+$Count)
for($i=0;$i -lt 200;$i++){
    Start-Sleep -Seconds 2
    $listing=(& $adb -s $serial shell run-as $pkg ls files|Out-String)
    if($LASTEXITCODE -ne 0){throw "Cannot list app QA outputs"}
    if($listing.Contains($base+".error")){
        $err=(& $adb -s $serial shell run-as $pkg cat ("files/"+$base+".error")|Out-String)
        [IO.File]::WriteAllText((Join-Path $root ($base+"_scan_only.error.txt")),$err,[Text.UTF8Encoding]::new($false))
        throw ("QA_SCAN_ONLY_FAILED: "+$err.Substring(0,[Math]::Min($err.Length,800)))
    }
    if($listing.Contains($base+".json")){
        $result=(& $adb -s $serial shell run-as $pkg cat ("files/"+$base+".json")|Out-String)
        $json=$result|ConvertFrom-Json
        if($json.scan_only -ne $true -or $json.mode -ne 'ACTUAL_MEDIASTORE_INSERTED_WAV_FILES_API35_EMULATOR' -or
            [int]$json.verified_qa_files -ne $Count){
            throw "QA_SCAN_ONLY_INVALID_RESULT"
        }
        [IO.File]::WriteAllText((Join-Path $root ($base+"_scan_only.json")),$result,[Text.UTF8Encoding]::new($false))
        Write-Output ("QA_SCAN_ONLY_PASS="+$Count+" SCAN_MS="+$json.real_scan_ms_last+" INDEX_MS="+$json.index_build_ms+" PSS_KB="+$json.pss_end_kb+" SEARCH_MS="+$json.search_ms)
        exit 0
    }
}
throw "QA_SCAN_ONLY_TIMEOUT; no PASS"
