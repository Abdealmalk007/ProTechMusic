# ProTech Music v0.4.1-beta.3 — Signed Preview Publication
Date: 2026-11-22. Status: BETA/PREVIEW, not final.

## Release artifact and verified identity
- GitHub release: https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.1-beta.3
- GitHub files: ProTechMusic_v0.4.1_beta3_SIGNED.apk; SHA256SUMS_v041_beta3.txt.
- APK size: 8,991,827 bytes. SHA256: C58F4A242C8813F2FE67EDC47EAFF5DB68E86411EFF61901A988BD4E28AC2528.
- Same signing certificate as v0.4.0-beta.2 (certificate SHA-256 62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8). apksigner v2/v3 valid.
- Package digital.alghad.protechmusic; versionCode=14; versionName=0.4.1-beta.3; minSdk=26, targetSdk=35.
- Offline Android debug build and 50/50 JVM unit tests PASS. Release Gradle assembleRelease BUILD SUCCESSFUL.

## F13 Android QA evidence
- docs/QA_F13_RESPONSIVE_BILINGUAL_20261122.md
- Android 15/API35 emulator: Arabic RTL, English LTR persisted, portrait 320dp font 160%, portrait 411dp, landscape short viewport, collapsible filters and three-button OS nav.
- Screenshots from tests in docs/evidence/f13/.

## GitHub and site
- Source feature commit: 2331fa8 (F13 feature changes and screenshots).
- GitHub pre-release created: 2026-11-22T06:12:28Z.
- Site source commit: d6477b400bf7f65a772396b685a0cb32e0a586e3.
- Official release facts verified through GitHub API, four local site test suites PASS.
- Site URLs: https://alghad.digital/programs/protech-music/ and https://alghad.digital/en/programs/protech-music/.
- GitHub Pages deployment for commit d6477b4 completed SUCCESS (run 44992771944). Subsequent LIVE verification: both Arabic and English URL returned HTTP 200 and contained version 0.4.1-beta.3, exact signed APK filename and matching SHA256 C58F4A242C8813F2FE67EDC47EAFF5DB68E86411EFF61901A988BD4E28AC2528.

## Progress
- Original product roadmap 11/25 = 44% (unchanged).
- F milestone 13/15 = 86.7%, next F14 1k/10k/50k performance testing.
- R release-certification 0/15. Full real-device testing, Bluetooth/headset/interruption, Android API matrix, accessibility and battery measurement remain pending.
- Never declare a stable/final app based only on emulator/automated QA. Preserve app user data, keystore and signing secrets. Do not post source keystore/password.
