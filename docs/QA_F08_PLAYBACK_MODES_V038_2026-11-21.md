# ProTech Music — Phase F08 Playback Modes QA
Date: 2026-11-21. Build v0.3.8 DEBUG, Android 15 API35, isolated AVD ProTechMusicQA35_211126.
Synthetic WAV fixtures only: music.wav and download.wav (each 1 second), same playlist ID=3 QA_F06_2111_OK.

## Evidence
- Launched selected playlist in ordered mode from UI. Android shared_prefs/playback.xml contained exactly two tracks: Music music.wav then Downloads download.wav, shuffle=false; no unrelated device tracks included.
- MediaSession first reported PLAYING music at position=0. Natural completion advanced to download and ended with MediaSession PAUSED on download after playlist end and index=1 when repeat=off.
- From expanded Now Playing control, toggled repeat=off -> repeat one; screenshot qa/f08_repeat_one_ui.png displayed "تكرار 1". Restarted playback for same 2-track playlist. After more than 3 seconds, MediaSession still reported PLAYING music, index=0 rather than advancing to download.
- Toggled repeat one -> repeat all. Screenshot qa/f08_repeat_all_ui.png displayed "تكرار الكل". Five successive snapshots of Android MediaSession at ~0.9-second intervals captured:
  sample0 PLAYING music; sample1 PLAYING download; sample2 PLAYING music; sample3 PLAYING download; sample4 PLAYING music.
  This demonstrates real queue wrap-around, not a static UI state.
- Toggled repeat all -> off; screenshot qa/f08_repeat_off_ui.png displayed "بدون تكرار".
- Enabled shuffle from expanded Now Playing; SQLite not involved: playback SharedPreferences returned shuffle=true. Screenshot qa/f08_shuffle_ui.png displayed "خلط ✓".
- Launched playlist-only Shuffle from the playlist UI; playback queue still contained precisely the two track URIs; persisted shuffle remained true. Session reached PAUSED at queue end with repeat off.
- On previous v0.3.4 test, API35 Android system Sharesheet with 10 selected tracks and playlist-only shuffled order with three tracks confirmed in docs/QA_V034_PLAYBACK_BULK_DELETE_2026-11-21.md.
- No fatal app exception observed in this phase; QA screenshots show bottom gesture nav bar not overlapped by controls.

## Limitations
- Android 15 emulator and 1-second synthetic WAVs; no long-running real hardware/Bluetooth/audio focus/phone calls.
- Shuffle was verified enabled and confined to selected playlist; statistical uniformity of randomness was NOT validated.
- Repeat mode currently does not persist across service recreation; release recovery persistence is separate follow-up.
- No production release status; test existing source without changing app code.
Phase F08: PASS for defined selected-playlist next/ordered/shuffle/repeat 0/1/all acceptance. Original weighted task 16 was already PASS; do not increase canonical percentage twice.
