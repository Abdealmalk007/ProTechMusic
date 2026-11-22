$ErrorActionPreference="Stop"
$adb="E:\ProTech\AndroidToolchain\sdk\platform-tools\adb.exe"
$serial="emulator-5554"
$out="E:\ProTech\ProTechMusic\docs\evidence\v23\layout"
$pkg="digital.alghad.protechmusic"
$runner="digital.alghad.protechmusic.test/digital.alghad.protechmusic.RcSmokeInstrumentation"
New-Item -ItemType Directory -Force -Path $out|Out-Null
$boot=(& $adb -s $serial shell getprop sys.boot_completed|Out-String).Trim()
if($boot -ne "1"){throw "QA emulator not ready"}
$model=(& $adb -s $serial shell getprop ro.product.model|Out-String).Trim()
if($model -notmatch 'sdk_|Emulator'){throw "Refusing layout changes outside emulator"}
$results=@()
try{
 foreach($test in @(
  [pscustomobject]@{name="compact_320dp";width=840;height=1800},
  [pscustomobject]@{name="standard_411dp";width=1080;height=2340},
  [pscustomobject]@{name="landscape";width=1800;height=840}
 )){
   $size=$test.width.ToString()+"x"+$test.height.ToString()
   & $adb -s $serial shell wm size $size
   if($LASTEXITCODE -ne 0){throw "Failed to set $size"}
   Start-Sleep -Seconds 2
   $result=(& $adb -s $serial shell am instrument -w -r $runner 2>&1|Out-String)
   [IO.File]::WriteAllText((Join-Path $out ($test.name+"_instrumentation.log")),$result,[Text.UTF8Encoding]::new($false))
   if($LASTEXITCODE -ne 0 -or $result -notmatch "RC_SMOKE_PASS" -or $result -notmatch 'INSTRUMENTATION_CODE:\s+-1'){
    throw "Instrumentation failed $($test.name)"
   }
   & $adb -s $serial shell am start -n ($pkg+"/.MainActivity")
   if($LASTEXITCODE -ne 0){throw "Activity did not launch $($test.name)"}
   Start-Sleep -Seconds 9
   $remote="/sdcard/v23_"+$test.name+".png"
   $dest=Join-Path $out ($test.name+".png")
   & $adb -s $serial shell screencap -p $remote
   if($LASTEXITCODE -ne 0){throw "Screenshot capture failed"}
   & $adb -s $serial pull $remote $dest|Out-Null
   if($LASTEXITCODE -ne 0 -or !(Test-Path $dest)){throw "Screenshot transfer failed"}
   $results+=([ordered]@{name=$test.name;size=$size;instrumentation="PASS";screenshot=$dest})
   Write-Output ("RC_LAYOUT_PASS="+$test.name+" DISPLAY="+$size)
  }
}finally{
 & $adb -s $serial shell wm size reset|Out-Null
}
[IO.File]::WriteAllText((Join-Path $out "matrix_results.json"),($results|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "RC_LAYOUT_MATRIX_COMPLETED=TRUE"
