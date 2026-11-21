# ProTech Music v1.0 — Approved Execution Plan
Date: 2026-11-21
Status: APPROVED / IN IMPLEMENTATION
Platform: Android, Arabic RTL default + English optional
Root: E:\ProTech\ProTechMusic

## Non-negotiable user journeys
1. On first permission grant, "All audio" must show EVERY audio exposed in the shared Android MediaStore, including voice notes and short clips, not only IS_MUSIC=1.
2. Auto-source sections: WhatsApp, Downloads, Recordings, Telegram, Bluetooth, Music, Podcasts, Other. Source derived from MediaStore RELATIVE_PATH / BUCKET_DISPLAY_NAME where available. Source is a virtual grouping; never copies/moves originals. WhatsApp private/encrypted or non-indexed data cannot be accessed without authorized SAF access.
3. Every row can play, Add to Existing Playlist or Create New Playlist and Add, plus multi-select bulk add. Playlist membership cannot delete originals.
4. Manual SAF audio import for items MediaStore excludes, persist read permission and list these too.
5. Background playback and Bluetooth/lockscreen/notification controls, seek, queue, repeat/shuffle, last position.
6. Sound features: equalizer when supported, BassBoost, LoudnessEnhancer/volume booster with SAFE limits, warning against distortion and hearing damage. Never claim system-wide boosting. Effects not available on all Android devices.
7. Clear RTL dark-mode UI with search, albums/artists/folders, playlists, favorites, settings, sleep timer, speed control.
8. Test across Android versions/emulator and real hardware when available. Signed APK only when success is independently verified.

## Only 3 implementation phases (each with evidence gate)
Phase 1: Core UI + indexed all-audio + source grouping + SAF + persistent playlists + basic unit tests.
Phase 2: Playback service + foreground/lockscreen + audio effects + queue + persistence + visual polish.
Phase 3: Emulator/real-device test + fix all blocking bugs + sign debug/release build + archive + publish GitHub/website on explicit final verified gate.

## Acceptance evidence required
- APK build logs; installs/launches on emulator.
- Seed 6 fake audio files in Music, Download, WhatsApp, Telegram, Recordings directories. Verify all 6 in All Audio and correct source groups.
- Create playlist from row; add, remove, reorder; all source files untouched.
- Search in Arabic/English; rotate/reopen; playback in background/notifications; sleep timer.
- Equalizer/boost supported session audio id, no crash if effects unsupported.
- Filename for accepted release, SHA-256, app package, screenshots/logcat.
- When absent, mark BLOCKED, never report complete.

## Progress
Track actual implementation progress in docs/PROGRESS.json with links to build/test logs. The earlier roadmap remains a reference; the 3 phases group its 25 tasks.
