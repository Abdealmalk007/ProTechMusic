# ProTech Music — V6 Approved Consolidated Execution and Acceptance Plan
Updated: 2026-11-21 (Asia/Riyadh)
Status: IN DEVELOPMENT — do not publish as final until complete release gate
Project root: E:\ProTech\ProTechMusic
Purpose: Offline audio player, auto-index all accessible audio, per-source virtual lists, custom playlists, audio enhancement and secure Android file sharing.

## Requirements (P0)
A1 All Audio: show every shared audio item accessible through MediaStore after READ_MEDIA_AUDIO (including WhatsApp voice clips and very short files; do not require IS_MUSIC=1).
A2 Source lists: WhatsApp, Downloads, Telegram, Recordings, Music, Bluetooth, podcasts, other third-party accessible media; show by-app and by-folder classification; do not duplicate media or promise access to app-private encrypted data.
A3 Add a single track, multiselect many tracks, and current playlist queue to existing/new playlists; preserve the real files when deleting from a playlist.
A4 Sharing: ACTION_SEND for single track, ACTION_SEND_MULTIPLE for selected tracks or playlist, use content://, per-intent FLAG_GRANT_READ_URI_PERMISSION, ClipData and Android Sharesheet. Never expose raw file paths. Reject missing/non-readable data without crashing.
A5 Playback: media focus, background audio, notification and lock-screen, wired/Bluetooth media keys, shuffle/repeat, seek, speed, persistent queue.
A6 Sound: Equalizer with supported device bands and presets, BassBoost and LoudnessEnhancer bounded to safe non-distorting levels, auto fallback if effects unavailable, clear hearing warning.
A7 Accessibility: Arabic RTL default + English LTR, large font, contrast and button labels, dark/light themes.
A8 Data: favorites, albums, artists, playlist ordered membership, JSON backup/restore + optional M3U/M3U8 with missing-media diagnostics.

## Three sequential execution phases — minimum stage count
Phase 1 (Core, 40%):
  01 verify Kotlin/Gradle build, repo history and working tree without overriding newer changes.
  02 scan available audio via MediaStore and SAF with permission-denied handling.
  03 enumerate All Audio, source cards, virtual folders, artists and albums, search.
  04 playlist create/rename/delete (confirm), add single/multiple, remove/reorder.
  05 user favorites, playlist persistence and test missing-file handling.
  06 share track via Android Sharesheet; share multiple; share complete playlist.
  07 backup export/import including JSON validation, atomic restore, no source deletion.
  08 unit tests for source classification and playlist schema; emulator QA evidence.
Gate 1: no crash; six source fixtures indexed; functional playlist add-and-restore; real sharesheet shows one file and multiple files.

Phase 2 (Audio, 35%):
  09 Media3 player/MediaSessionService: queue, play/pause/next, media focus.
  10 notification/background/lockscreen tests + Bluetooth disconnect.
  11 Equalizer bands/presets + BassBoost/LoudnessEnhancer, speed, timer.
  12 test audio files: WAV, MP3/AAC/FLAC, and corrupted files; no hang.
  13 scoped foreground service behavior on Android 13–16 and earlier.
  14 large-library paging/cache/battery/memory performance.
Gate 2: 30-minute real content playback, notification and physical Bluetooth control, audio effects verified where device permits.

Phase 3 (Release QA, 25%):
  15 test Android 8, 11, 13, 14, 15, 16 (or record BLOCKED with evidence).
  16 device testing of permission grant/deny/revoke and SAF persistent URI.
  17 UI/UX QA Arabic/English, RTL/LTR, TalkBack, responsive dark/light UI.
  18 missing/stale URI, offline, process restart and metadata backup migration.
  19 test M3U8 import/export (optional until included in release contract).
  20 signed release APK/AAB + SHA256 and reproducible clean build.
  21 publish only final passed release to GitHub and alghad.digital; verify links.
Gate 3: ALL P0 acceptance checks evidenced, no critical errors, signed distribution artifact verified; otherwise maintain DEBUG/QA status.

## Execution protocol
- Auto-advance to next independent step after every PASS in the same execution session.
- Never infer success from source code existence. Keep logs and screenshots per gate.
- For steps requiring physical Android hardware or unconfigured release signing, mark BLOCKED instead of PASS.
- Do not merge unverified changes, overwrite work tree, delete data or publish production builds prematurely.
- Store status in docs/PROGRESS.json; store evidence reports in docs/QA_V*.md.
- Debug code may be built and installed without a release signing key; final production requires a verified, persistent secure keystore.

## Evidence so far, independently verified
- Git commits 73c61f3, d6f356c are preserved; source branch has additional uncommitted work.
- BUILD SUCCESSFUL on newest working tree in docs/build_continuation_v6c.log.
- Android 15 emulator installed and launched app; All Audio shows 8 fixtures.
- ACTION_SEND single file opened Android system ChooserActivity (2026-11-21).
- Earlier docs/QA_V4_2026-11-21.md and docs/QA_V5_2026-11-21.md report multi-sharing, playlist backup roundtrip, source cards and EQ UI.
- Release gate remains not passed; real phone, full multiversion UI/behavior and distribution QA pending.
