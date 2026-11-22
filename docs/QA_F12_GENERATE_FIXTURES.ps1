$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "qa\fixtures\f12"
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$ffmpeg = (Get-Command ffmpeg.exe -ErrorAction Stop).Source

function Run-Ffmpeg([string[]] $Arguments) {
    & $ffmpeg -hide_banner -loglevel error -y @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed with exit $LASTEXITCODE" }
}
$base = Join-Path $outDir "protech-qa-a-raw.mp3"
$cover = Join-Path $outDir "protech-qa-cover.jpg"
$artTrack = Join-Path $outDir "protech-qa-a-cover.mp3"
$nextTrack = Join-Path $outDir "protech-qa-b-no-cover.mp3"
Run-Ffmpeg @("-f","lavfi","-i","sine=frequency=440:duration=9","-c:a","libmp3lame","-q:a","6","-metadata","title=ProTech QA A","-metadata","artist=ALGHAD QA","-metadata","album=Synthetic Test",$base)
Run-Ffmpeg @("-f","lavfi","-i","color=c=blue:s=200x200:d=1","-frames:v","1","-update","1",$cover)
Run-Ffmpeg @("-i",$base,"-i",$cover,"-map","0:a","-map","1:v","-c:a","copy","-c:v","mjpeg","-disposition:v","attached_pic","-id3v2_version","3","-metadata:s:v","title=Cover","-metadata:s:v","comment=Cover (front)",$artTrack)
Run-Ffmpeg @("-f","lavfi","-i","sine=frequency=660:duration=7","-c:a","libmp3lame","-q:a","6","-metadata","title=ProTech QA B","-metadata","artist=ALGHAD QA",$nextTrack)
$lyrics = "[00:00.20]QA First line`n[00:03.00]QA Second line`n[00:07.00]QA Last line`n"
[IO.File]::WriteAllText((Join-Path $outDir "protech-qa-a.lrc"), $lyrics, [Text.UTF8Encoding]::new($false))
foreach($f in @($artTrack,$nextTrack,$cover,(Join-Path $outDir "protech-qa-a.lrc"))) {
  $item = Get-Item -LiteralPath $f -ErrorAction Stop
  Write-Output ("QA_FIXTURE $($item.Name) $($item.Length) bytes")
}
