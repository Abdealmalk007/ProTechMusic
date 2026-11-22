param([int[]]$Counts = @(10), [switch]$SkipInstall)
$ErrorActionPreference="Stop"
$adb="E:\ProTech\AndroidToolchain\sdk\platform-tools\adb.exe"
$serial="emulator-5554"
$pkg="digital.alghad.protechmusic"
$apk="E:\ProTech\ProTechMusic\app\build\outputs\apk\debug\app-debug.apk"
$out="E:\ProTech\ProTechMusic\docs\evidence\f14_realmedia"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$devices=(& $adb devices -l | Out-String)
if($devices -notmatch 'emulator-5554\s+device\s'){throw "Dedicated QA emulator is not connected"}
$boot=(& $adb -s $serial shell getprop sys.boot_completed|Out-String).Trim()
if($boot -ne "1"){throw "QA emulator not booted"}
$model=(& $adb -s $serial shell getprop ro.product.model|Out-String).Trim()
if($model -notmatch 'sdk_|Emulator'){throw "QA runner requires an emulator, not a personal phone"}
if(-not $SkipInstall){
    & $adb -s $serial install --no-streaming -r $apk
    if($LASTEXITCODE -ne 0){throw "Debug APK update failed; never uninstall user data"}
}
& $adb -s $serial shell pm grant $pkg android.permission.READ_MEDIA_AUDIO
if($LASTEXITCODE -ne 0){throw "READ_MEDIA_AUDIO grant failed"}
foreach($n in $Counts){
    if($n -notin @(10,100,1000,10000)){throw "Disallowed count $n"}
    $base="f14_real_$n"
    & $adb -s $serial shell run-as $pkg rm -f ("files/"+$base+".json") ("files/"+$base+".error")
    if($LASTEXITCODE -ne 0){throw "Cannot clear stale QA result"}
    & $adb -s $serial shell am start -n ($pkg+"/.F14RealMediaQaActivity") --ei count $n
    if($LASTEXITCODE -ne 0){throw "QA Activity failed $n"}
    Write-Output "QA_REAL_MEDIA_STARTED=$n"
    $done=$false
    $max=if($n -le 10){80}elseif($n -le 100){180}else{400}
    for($i=0;$i -lt $max;$i++){
        Start-Sleep -Seconds 2
        $listing=(& $adb -s $serial shell run-as $pkg ls files |Out-String)
        if($LASTEXITCODE -ne 0){throw "Cannot inspect app QA reports"}
        if($listing.Contains($base+".error")){
            $errorText=(& $adb -s $serial shell run-as $pkg cat ("files/"+$base+".error")|Out-String)
            [IO.File]::WriteAllText((Join-Path $out ($base+".error.txt")),$errorText,[Text.UTF8Encoding]::new($false))
            throw "QA_FAIL=$n $($errorText.Substring(0,[Math]::Min(500,$errorText.Length)))"
        }
        if($listing.Contains($base+".json")){
            $txt=(& $adb -s $serial shell run-as $pkg cat ("files/"+$base+".json")|Out-String)
            $data=$txt|ConvertFrom-Json
            if([int]$data.verified_qa_files -ne $n -or [int]$data.real_scanned_total -lt $n){throw "Invalid real scan count"}
            [IO.File]::WriteAllText((Join-Path $out ($base+".json")),$txt,[Text.UTF8Encoding]::new($false))
            Write-Output ("QA_REAL_MEDIA_PASS="+$n+" SCAN_MS="+$data.real_scan_ms_last+" INDEX_MS="+$data.index_build_ms+" REAL_FILES="+$data.verified_qa_files)
            $done=$true
            break
        }
    }
    if(-not $done){throw "QA_TIMEOUT=$n (not a PASS)"}
}
& $adb -s $serial shell logcat -d -t 1000 'AndroidRuntime:E' '*:S' | Out-File -FilePath (Join-Path $out "android_runtime_v21.txt") -Encoding utf8
Write-Output "QA_REAL_MEDIA_COMPLETED=TRUE"
