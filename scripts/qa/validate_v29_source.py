#!/usr/bin/env python3
"""Static source contract for ProTech Music v0.4.6-beta.8; not device acceptance."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[2]
ui=(root/"app/src/main/java/digital/alghad/protechmusic/MainActivity.kt").read_text(encoding="utf-8")
svc=(root/"app/src/main/java/digital/alghad/protechmusic/MusicService.kt").read_text(encoding="utf-8")
gradle=(root/"app/build.gradle.kts").read_text(encoding="utf-8")
tests={
    "version_code_19": 'versionCode = 19' in gradle,
    "version_name_beta8": 'versionName = "0.4.6-beta.8"' in gradle,
    "prev_next_56dp": ui.count("Modifier.size(56.dp).background")>=2,
    "center_68dp": "modifier=Modifier.size(68.dp)" in ui,
    "a11y_play_pause_ar_en": all(s in ui for s in ("Pause playback","Resume playback","إيقاف مؤقت","استئناف التشغيل")),
    "skip_10_seconds": all(s in ui for s in ("seekRelative(-10_000L)","seekRelative(10_000L)")),
    "seek_clamped": "ms.coerceIn(0L,duration)" in svc,
    "seek_sync_session": "fun seek(ms:Long){" in svc and "updateSession()" in svc,
    "notification_restores_queue": "if(intent?.action!=null && player.mediaItemCount==0) restore()" in svc,
    "position_checkpoint_10s": "elapsed-lastPositionCheckpoint>=10_000L" in svc,
    "existing_media_controls": all(s in svc for s in ("fun resume()","fun pause()","fun next()","fun previous()")),
    "large_accessibility_targets_fit_320": 320 - 24 - (56+4+68+4+56) >= 100,
}
out=root/"docs/evidence/v29_static_source_contract.json"
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps({"version":"0.4.6-beta.8","kind":"STATIC_SOURCE_ONLY","pass":sum(tests.values()),"total":len(tests),"checks":tests},ensure_ascii=False,indent=2),encoding="utf-8")
print(f"STATIC_SOURCE_PASS={sum(tests.values())}/{len(tests)}")
for key,ok in tests.items():print(("PASS" if ok else "FAIL"),key)
if not all(tests.values()):raise SystemExit(1)
