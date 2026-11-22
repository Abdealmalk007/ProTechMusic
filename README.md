# ProTech Music
Native Android offline-first music player by ALGHAD DIGITAL (ProTech).

**Status: Signed Beta Preview, NOT production-ready.** Current source is version `0.4.5-beta.7`; the original 25-task roadmap remains 44% verified, and feature-closure Phase F is 86.7% verified. F14 performance work is still in progress and is not certified as passed. Package: `digital.alghad.protechmusic`. Kotlin 2.1.20, Compose 1.8.2, Material3 1.3.2, Media3 ExoPlayer 1.6.1, Android SDK 35; Android 8+.

## Features implemented in source
- MediaStore scan of all user-accessible audio (not limited to IS_MUSIC=1), including short voice notes.
- Smart source categories: WhatsApp, Downloads, Recordings, Telegram, Bluetooth, Music, Podcasts, Audiobooks, Instagram, Signal, Snapchat, Messenger, and per-app paths. Folder browsing included.
- SAF multi-document import with persisted URI access where supported.
- Full track list, text search, source filters, favorite marks, multiselect.
- SQLite playlists: create, rename, remove, add many tracks, reorder playlist entries without deleting source files.
- ExoPlayer audio playback, background started MediaSession service, notification controls, repeat and shuffle, speed, sleep timer.
- Per-app audio effects when supported by underlying Android device: Equalizer presets, BassBoost, LoudnessEnhancer with +6 dB cap.
- Embedded audio artwork (bounded decoding) in Now Playing, MediaSession and notifications; SAF-imported, app-private synchronized local `.lrc` lyrics (API35 emulator verified).
- Arabic RTL default and persistent English LTR choice; responsive compact/landscape layout, semantic navigation labels and collapsible filters (F13 API35 matrix).
- Prepared, snapshot-bound search index built off the main UI thread. A printable-ASCII normalization fast path reduces large-library index construction overhead without altering Arabic/Unicode search semantics. Android API35 synthetic 1k/10k/50k benchmarks and 54 JVM tests are documented in `docs/QA_F14_FAST_INDEX_V20_20261122.md`; true 50k media-file scanning and physical battery QA remain pending.

## Build (Windows)
Set `ANDROID_HOME` to the installed Android SDK, JDK17, then:
`gradle --offline :app:assembleDebug :app:testDebugUnitTest`
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Android constraints
The app uses `READ_MEDIA_AUDIO` on Android 13+ and version-compatible audio read permissions on earlier versions. WhatsApp encrypted/sandbox-private recordings may be inaccessible. There is no unrestricted private app storage access. Source groups are virtual and cannot automatically extract hidden files.

## Development/release gates still open
- F12 and F13 emulator acceptance is documented; F14 synthetic large-library profiling is underway. Full 1k/10k/50k *real-media* integration, background physical battery profiling and F15 RC gate remain pending.
- Automated UI/instrumentation tests, Bluetooth/lockscreen proof, permission-denied and SAF persistence verification.
- A fully signed **stable** APK/AAB and final release certification are pending. GitHub/site Beta previews are explicitly not final.

The user-approved execution plan is `docs/EXECUTION_PLAN_V2_2026-11-21.md`. Never call a debug APK final without end-to-end gate proof.
