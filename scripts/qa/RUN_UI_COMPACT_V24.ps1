$ErrorActionPreference="Stop"
$adb="E:\ProTech\AndroidToolchain\sdk\platform-tools\adb.exe"
$serial="emulator-5554"
$apk="E:\ProTech\ProTechMusic\app\build\outputs\apk\debug\app-debug.apk"
$out="E:\ProTech\ProTechMusic\docs\evidence\v23\ui_v24"
New-Item -Path $out -ItemType Directory -Force|Out-Null
$ready=$false
for($i=0;$i -lt 180;$i++){
    $devices=(& $adb devices -l|Out-String)
    if($devices -match 'emulator-5554\s+device\s'){
        $boot=(& $adb -s $serial shell getprop sys.boot_completed 2>$null|Out-String).Trim()
        if($boot -eq "1"){$ready=$true;break}
    }
    Start-Sleep -Seconds 2
}
if(-not $ready){throw "Dedicated V24 UI emulator boot timed out"}
$name=(& $adb -s $serial emu avd name|Out-String)
if($name -notmatch "ProTechMusicUI35_V24"){throw "Unexpected emulator identity: $name"}
Write-Output "V24_UI_EMULATOR_READY=TRUE"
& $adb -s $serial install --no-streaming -r $apk
if($LASTEXITCODE -ne 0){throw "Debug app install failed in isolated UI AVD"}
& $adb -s $serial shell pm grant digital.alghad.protechmusic android.permission.READ_MEDIA_AUDIO
if($LASTEXITCODE -ne 0){throw "Audio permission grant failed"}
& $adb -s $serial shell wm size 840x1800
if($LASTEXITCODE -ne 0){throw "Cannot set 320dp viewport"}
& $adb -s $serial shell am start -n digital.alghad.protechmusic/.MainActivity
if($LASTEXITCODE -ne 0){throw "MainActivity launch failed"}
Start-Sleep -Seconds 12
& $adb -s $serial shell screencap -p /sdcard/v24_compact_menu.png
if($LASTEXITCODE -ne 0){throw "Capture failed"}
& $adb -s $serial pull /sdcard/v24_compact_menu.png (Join-Path $out "compact_menu_after_load.png")|Out-Null
if($LASTEXITCODE -ne 0){throw "Screenshot pull failed"}
& $adb -s $serial shell uiautomator dump /sdcard/v24_ui_hierarchy.xml
if($LASTEXITCODE -eq 0){
    & $adb -s $serial pull /sdcard/v24_ui_hierarchy.xml (Join-Path $out "ui_hierarchy.xml")|Out-Null
}
Write-Output "V24_UI_SCREENSHOT_CAPTURED=TRUE"
