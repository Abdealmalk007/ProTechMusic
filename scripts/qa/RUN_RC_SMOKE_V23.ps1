$ErrorActionPreference = "Stop"
$adb = "E:\ProTech\AndroidToolchain\sdk\platform-tools\adb.exe"
$serial = "emulator-5554"
$root = "E:\ProTech\ProTechMusic"
$appApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
$out = Join-Path $root "docs\evidence\v23"
New-Item -ItemType Directory -Path $out -Force | Out-Null
foreach($item in @($adb,$appApk,$testApk)){if(-not(Test-Path $item)){throw "Missing required file $item"}}
$devices=(& $adb devices -l |Out-String)
if($devices -notmatch 'emulator-5554\s+device\s'){throw "QA emulator not online"}
$boot=(& $adb -s $serial shell getprop sys.boot_completed |Out-String).Trim()
if($boot -ne "1"){throw "QA emulator not fully booted"}
$model=(& $adb -s $serial shell getprop ro.product.model|Out-String).Trim()
if($model -notmatch 'sdk_|Emulator'){throw "Refusing to install QA APK on non-emulator device"}
$api=(& $adb -s $serial shell getprop ro.build.version.sdk|Out-String).Trim()
if($api -ne "35"){throw "Unexpected API $api"}
Write-Output ("V23_EMULATOR_READY="+$model+" API="+$api)
& $adb -s $serial install --no-streaming -r $appApk
if($LASTEXITCODE -ne 0){throw "Debug APK install failed; never uninstall data as workaround"}
& $adb -s $serial install --no-streaming -r $testApk
if($LASTEXITCODE -ne 0){throw "Instrumentation APK install failed"}
& $adb -s $serial shell pm grant digital.alghad.protechmusic android.permission.READ_MEDIA_AUDIO
if($LASTEXITCODE -ne 0){throw "Audio read permission not granted"}
$registry=(& $adb -s $serial shell pm list instrumentation |Out-String)
[IO.File]::WriteAllText((Join-Path $out "instrumentation_registry.txt"),$registry,[Text.UTF8Encoding]::new($false))
if($registry -notmatch "RcSmokeInstrumentation"){throw "RC runner not registered: $registry"}
Write-Output "INSTRUMENTATION_REGISTERED=TRUE"
$log=(& $adb -s $serial shell am instrument -w -r digital.alghad.protechmusic.test/digital.alghad.protechmusic.RcSmokeInstrumentation 2>&1 |Out-String)
[IO.File]::WriteAllText((Join-Path $out "instrumentation_smoke.log"),$log,[Text.UTF8Encoding]::new($false))
Write-Output $log
if($LASTEXITCODE -ne 0 -or $log -notmatch 'RC_SMOKE_PASS' -or $log -notmatch 'INSTRUMENTATION_CODE:\s+-1'){
    throw "Instrumentation runner failed; full logs saved"
}
Write-Output "A14_ANDROID_INSTRUMENTATION_SMOKE_PASS=TRUE"
