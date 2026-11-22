# ProTech Music v0.4.0-beta.2 — Signed preview release evidence
Date: 2026-11-22. Status: BETA/PREVIEW — NOT FINAL.

## Artifacts
- GitHub source: https://github.com/Abdealmalk007/ProTechMusic (feature commit f4530341078fab57a92d7a390066fe16dcc6784c).
- Verified GitHub prerelease: https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.0-beta.2 (two assets: signed APK and SHA256SUMS_v040_beta2.txt; published 2026-11-22T04:54:14Z).
- APK: ProTechMusic_v0.4.0_beta2_SIGNED.apk; size: 8,991,827 bytes.
- SHA-256: C1FD4D09782E0430E48C8AFCB9B2FB57B6E071CC50E324746204E9C65E650345.
- APK package digital.alghad.protechmusic, versionCode 13, versionName 0.4.0-beta.2, minSdk 26, targetSdk 35.
- APK verified with Android apksigner v2 and v3. Signer SHA-256 certificate is the SAME as signed 0.3.9-beta.1. Existing signing key and DPAPI password were reused; no replacement signing identity generated.
- Local build log: docs/build_v040_signed_release.log (not tracked by Git ignore). Release Gradle assembleRelease SUCCESS; debug unit suite 48/48 PASS.

## Fresh signed installer smoke on Android 15
- Independent clean AVD ProTechMusicQA35_221126, API35; both synthetic MP3s pushed to its Music folder.
- `adb install` of the signed beta2 completed Success. `adb shell am start -n digital.alghad.protechmusic/.MainActivity` returned Success; running PID 2095; package manager reported versionCode 13 and versionName 0.4.0-beta.2. The first startup screenshot showed normal dark UI (initial 0 indexed tracks, immediately after fresh install). Screenshot: docs/evidence/f12/beta2_fresh_signed.png.
- Previous debug build tested synthetic MP3 discovery, cover art, synchronized local LRC import, MediaSession metadata, track transitions, notification actions, audio focus in docs/QA_F12_ANDROID35_ACCEPTANCE_20261122.md.

## Website publication
- Site repository: https://github.com/Abdealmalk007/alghad-digital, deployment commit c2bfcd814973c5e733fc5a703b8695b8bb4bd910.
- Official GitHub release facts refreshed and matched the signed artifact SHA-256.
- Both bilingual pages verified live with HTTP 200, displayed v0.4.0-beta.2, correct signed APK filename and SHA-256:
  https://alghad.digital/programs/protech-music/
  https://alghad.digital/en/programs/protech-music/
- Site CI local tests: smoke-test, site-search-test, bilingual-test, no-duplicate-program-test: ALL PASS.

## Progress and open gates
- Original 25-task roadmap 11/25 PASS = 44%, unchanged.
- Feature closure phase F 12/15 PASS = 80%; next F13: 320dp/411dp layouts, landscape, large fonts, EN LTR and gesture/three-button navigation.
- Gate R 0/15 PASS. Real-device/Bluetooth/headset/call QA, 1k/10k/50k indexing, accessibility, signed stable release and all-platform reliability NOT certified.
- Do not claim final/stable or 100%; no personal audio files were modified/deleted. Source and QA evidence are preserved.
