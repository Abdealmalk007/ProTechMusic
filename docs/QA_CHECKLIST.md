# ProTech Music quality checklist — 2026-11-21
Android 15 (API 35) emulator: ProTechMusic_API35, emulator-5554.

- [x] Android Gradle build succeeded: docs/build_online.log
- [x] Unit tests: 7/7 SourceClassifierTest, docs/unit_tests.log
- [x] Install and launch debug APK on emulator with READ_MEDIA_AUDIO permission.
- [x] Seed 6 1-second WAV tracks: Music, Download, WhatsApp, Recordings, Telegram, Bluetooth.
- [x] MediaStore indexed all six; app displayed All Audio (6) and source labels correctly.
- [x] Playback proof: Android MediaSession changed to PLAYING then PAUSED after 1-second track, no fatal log.
- [x] Arabic RTL and dark design rendered in screenshots: qa/protech_ui.png
- [ ] Updated build with new-playlist add fix, folder browser, observer installed and retested.
- [ ] Create playlist from WhatsApp song, verify one entry; add download, remove, move; restart and verify persistence.
- [ ] Multiselect / add to existing list; favorites persistence.
- [ ] Equalizer and boost on supported device; verify effect availability, no distortion.
- [ ] Notification and lockscreen play/pause after app background.
- [ ] Bluetooth route and audio focus interruption on physical device.
- [ ] SAF manual audio import and URI persistence after reboot.
- [ ] Permission denial/revoke UI.
- [ ] Test 1k/10k/50k audio benchmark and incremental media updates.
- [ ] Export/import playlists and app metadata backups.
- [ ] Signed release, SHA256, release and public deployment review.

Note: demo audio fixtures are synthetic generated WAV tones (no copyrighted music).
