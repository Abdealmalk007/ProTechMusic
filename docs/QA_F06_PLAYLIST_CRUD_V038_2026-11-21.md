# ProTech Music — Phase F06 custom playlist CRUD QA
Date: 2026-11-21. Device: DESKTOP-PM75RQM emulator ProTechMusicQA35_211126, Android 15 API35.
Build under test: v0.3.8 DEBUG, Git 738138f, tests 36/36; no personal files touched.

## Test case
- Preserved pre-existing playlist ID=2 QA_BAK_038 with one synthetic download.wav fixture.
- From All Audio, selected download.wav and created a new playlist via UI: QA_F06_2111 (new ID=3). SQLite verified one entry.
- Added music.wav (Music) and telegram.wav (Telegram) to new playlist using native add-to-existing-list dialogs. SQL verified three unique entries, positions 0,1,2.
- Re-added music.wav to playlist ID=3. SQL COUNT(*), COUNT(DISTINCT uri) => 3|3: dedup succeeded.
- Entered playlist via bottom Playlists tab; UI showed three rows. Screenshot qa/f06_three.png.
- Opened three-dots menu for first row and selected Move Down. SQLite positions changed from download,music,telegram to music,download,telegram; screenshot qa/f06_overflow.png.
- Opened telegram.wav three-dots and chose Remove from Playlist Only (not Permanent Delete from Device). SQLite count dropped to 2; original telegram.wav still present in Android MediaStore row _id=1000000027 under Telegram/Telegram Audio/. Screenshot qa/f06_remove_menu.png.
- Scrolled playlist action chips, selected Rename and changed QA_F06_2111 to QA_F06_2111_OK via UI. SQLite kept ID=3, count=2; screenshot qa/f06_rename_dialog.png.
- Force-stopped and relaunched ProTech Music without clearing data. SQLite still showed ID=2 QA_BAK_038 count=1 and ID=3 QA_F06_2111_OK count=2. No observed fatal exception.
- Previous bulk N/all, add and share evidence: docs/QA_V034_PLAYBACK_BULK_DELETE_2026-11-21.md.
- Previous playlist-only deletion and retained original WAV: docs/QA_F10_BACKUP_V038_2026-11-21.md.

## Remaining limitations
- Android 15 emulator with synthetic audio only, not a real phone or huge playlist.
- SQLite UNIQUE rejects duplicate playlist names; current UI may dismiss without user-friendly conflict message (for F13 UX).
- Custom cover artwork per playlist is not supported in current build and is not part of adopted F06 acceptance gate.
- No application code change in this F06 QA; tested pre-existing functionality. No production signing or publication.
Phase F06: PASS. Canonical original roadmap task 12 was already PASS: no second increment to overall percentage.
