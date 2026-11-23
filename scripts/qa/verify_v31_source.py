"""QA contract for ProTech Music Beta 9 transport and no-artwork UX.
Python 3.14, stdlib only. Run from project root.
"""
from __future__ import annotations
import json
from datetime import datetime, timezone
from pathlib import Path

root = Path(__file__).resolve().parents[2]
ui = (root / "app/src/main/java/digital/alghad/protechmusic/MainActivity.kt").read_text(encoding="utf-8")
service = (root / "app/src/main/java/digital/alghad/protechmusic/MusicService.kt").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
cases = {
    "versionCode20": "versionCode = 20" in gradle and 'versionName = "0.4.7-beta.9"' in gradle,
    "centeredTransportRow": "horizontalArrangement=Arrangement.Center" in ui,
    "previousNext56dp": ui.count("Modifier.size(56.dp)") >= 2,
    "mainPlayPause68dp": "Modifier.size(68.dp)" in ui,
    "accessibleTransport": "Pause playback" in ui and "Resume playback" in ui and "Previous track" in ui and "Next track" in ui,
    "noCoverImagesInApp": all(token not in ui for token in ("CoverArt.read", "asImageBitmap", "Image(cover", "val cover by produceState")),
    "noCoverImagesInNotification": all(token not in service for token in ("CoverArt.read", "setLargeIcon", "metadata.putBitmap(", "requestArtwork", "currentArtwork")),
    "playingTrackGlows": all(token in ui for token in (".shadow(12.dp, activeShape", ".border(1.5.dp, Color(0xFF77FFF2)", "isCurrentTrack = MusicRuntime.nowPlaying?.uri == track.uri")),
    "activeTrackContrastingTitle": "if(isCurrentTrack) Color(0xFF80F7E9)" in ui,
    "sameTrackDoesNotRestartQueue": "if (MusicRuntime.nowPlaying?.uri==track.uri && service!=null)" in ui and "if (index==queueCurrentIndex && service!=null)" in ui,
    "playPauseToggleFromTrackList": 'if(isCurrentTrack && MusicRuntime.playing)"⏸" else "▶"' in ui,
    "playerPauseResumePreserved": "fun pause()" in service and "fun resume()" in service and "remember()" in service,
    "resumeSavedPosition": 'getLong("position",0)' in service and 'putLong("position",' in service,
    "seek10sAndAutoNavigation": "seekRelative(-10_000L)" in ui and "seekRelative(10_000L)" in ui and "fun next()" in service and "fun previous()" in service,
    "mediaSessionNotification": "MediaSession" in service and '"PLAY" -> resume()' in service and '"PAUSE" -> pause()' in service,
}
out = {"version": "0.4.7-beta.9", "time_utc": datetime.now(timezone.utc).isoformat(), "count": len(cases), "pass": sum(cases.values()), "checks": cases}
ev = root / "docs/evidence/v31"
ev.mkdir(parents=True, exist_ok=True)
(ev / "source_contract.json").write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"V31_SOURCE_CONTRACT={out['pass']}/{out['count']}", flush=True)
for name, ok in cases.items(): print(("PASS" if ok else "FAIL") + " " + name)
if not all(cases.values()): raise SystemExit(1)
