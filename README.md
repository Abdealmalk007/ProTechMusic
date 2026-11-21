# ProTech Music
Native Android offline-first music player by ALGHAD DIGITAL (ProTech).

**Status: Debug prototype, not production release.** Package: `digital.alghad.protechmusic`. Kotlin 2.1.20, Compose 1.8.2, Material3 1.3.2, Media3 ExoPlayer 1.6.1, Android SDK 35; Android 8+.

## Features implemented in source
- MediaStore scan of all user-accessible audio (not limited to IS_MUSIC=1), including short voice notes.
- Smart source categories: WhatsApp, Downloads, Recordings, Telegram, Bluetooth, Music, Podcasts, Audiobooks, Instagram, Signal, Snapchat, Messenger, and per-app paths. Folder browsing included.
- SAF multi-document import with persisted URI access where supported.
- Full track list, text search, source filters, favorite marks, multiselect.
- SQLite playlists: create, rename, remove, add many tracks, reorder playlist entries without deleting source files.
- ExoPlayer audio playback, background started MediaSession service, notification controls, repeat and shuffle, speed, sleep timer.
- Per-app audio effects when supported by underlying Android device: Equalizer presets, BassBoost, LoudnessEnhancer with +6 dB cap.

## Build (Windows)
Set `ANDROID_HOME` to the installed Android SDK, JDK17, then:
`gradle --offline :app:assembleDebug :app:testDebugUnitTest`
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Android constraints
The app uses `READ_MEDIA_AUDIO` on Android 13+ and version-compatible audio read permissions on earlier versions. WhatsApp encrypted/sandbox-private recordings may be inaccessible. There is no unrestricted private app storage access. Source groups are virtual and cannot automatically extract hidden files.

## Development/release gates still open
- Production-ready bilingual UI, artist/album browser, artwork rendering, playlist import/export/backups, manual EQ bands, long-library profiling, cross-device reliability.
- Automated UI/instrumentation tests, Bluetooth/lockscreen proof, permission denied and SAF persistence verification.
- Proper signed Release APK/AAB, real phone QA, GitHub/site publication.

The user-approved execution plan is `docs/EXECUTION_PLAN_V2_2026-11-21.md`. Never call a debug APK final without end-to-end gate proof.
