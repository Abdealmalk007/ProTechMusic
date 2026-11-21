# ProTech Music — QA evidence report (2026-11-21)
Platform: Android 15 API35 (AVD ProTechMusic_API35, emulator-5554)
Status: DEBUG PROTOTYPE / NOT FINAL RELEASE
APK: app/build/outputs/apk/debug/app-debug.apk
APK byte size: 12072050
APK SHA256: 1EEE27492F88B8795E7F4D018E62E7E28F32C2B3308B410BCC5D27377D015E5E

## Verified PASS
- Android Gradle :app:assembleDebug PASSED (docs/build_v2.log).
- JUnit SourceClassifierTest 7/7 passed, errors=0, failures=0.
- Installed APK and started MainActivity on emulator.
- Granted READ_MEDIA_AUDIO.
- Generated six original WAV test tones in six distinct shared-media directories: Music, Download, WhatsApp, Recordings, Telegram, Bluetooth.
- MediaStore returned exactly six audio records, with correct relative_path values.
- App All Audio screen showed 6 tracks and labels for the six sources (qa/protech_ui.png and qa/protech_v2.png).
- Emulator media session log showed state PLAYING and then PAUSED for 1s audio clip; no FATAL EXCEPTION detected.
- First corrected version: selected bluetooth track, Add to playlist -> Create new -> QAPlaylist, UI displayed QAPlaylist (1 track). Screenshot qa/playlist_v2.png.
- Added download track to existing QAPlaylist, restarted process and app, playlist was retained with 2 tracks. Screenshot qa/playlist_persist2.png.
- Effects screen displayed supported Equalizer capabilities: 5 bands / 10 presets. Screenshot qa/effects_v2.png.
- Effects controls tapped and no immediate process crash. This is NOT a real measurement of gain/quality.

## Not yet validated / required for final
- Background media controls, audio focus, Android 13+ notifications permission, Bluetooth headset on physical device.
- Audio equalizer actual DSP and loudness output measured on physical device; clipping/hearing safety.
- Playlists: manual reorder/remove/rename/delete and bulk add, repeat operations for data integrity.
- MediaStore change observer fresh-file test and graceful stale/missing file tests.
- SAF document URI persistent import after reboot, revocation, corrupted data.
- Playlist M3U/M3U8 import-export, export backup and restore.
- English UI, albums and artists, artwork thumbnails and accessibility checks.
- 10k/50k item performance memory battery profiling.
- MinSDK and other Android versions, physical device QA.
- Release signed APK/AAB, SHA256 for release, GitHub and site publication.
- Production release gate shall remain BLOCKED until all P0/P1 issues fixed and critical user journeys proven.

## Notes
All mock audio files were generated using pure sine tone code (not copyrighted music).
APK only distributed locally under E:\ProTech\ProTechMusic\app\build\outputs\apk\debug.
