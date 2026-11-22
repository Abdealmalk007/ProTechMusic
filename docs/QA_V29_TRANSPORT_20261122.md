# ProTech Music v0.4.6-beta.8 — Transport UI QA and signed release

Date: 2026-11-22. Channel: signed BETA preview (not production-certified).

## Changes in this version
- Previous/next controls at 56dp and primary Play/Pause button at 68dp; larger glyphs and Arabic/English accessibility descriptions.
- Mini-player now displays active/paused status and resume instruction.
- Expanded player supports ±10-second skip; seek is clamped within known duration.
- Incoming notification transport action restores stored queue on service recreation.
- Resume position and current index saved every 10 seconds during playback without rewriting queue JSON.

## Verified build / security evidence
- `scripts/qa/validate_v29_source.py` → 12/12 source contract PASS (not runtime QA).
- `scripts/qa/verify_v29_build.py` → Gradle BUILD SUCCESSFUL, `testDebugUnitTest`, `assembleDebug`, `assembleRelease` all executed.
- 54/54 Kotlin JVM tests, 0 failures, 0 errors, 0 skipped, 9 independent JUnit XML files.
- Signed `ProTechMusic_v0.4.6_beta8_SIGNED.apk`, versionCode 19, 9,008,211 bytes.
- Signed artifact SHA256: `C2F6D33DBF70C3E6F94FCF8895A821D38F1BA734D2561A9D43F979E3600C96F0`.
- APK Signature Scheme v2 and v3 verified; same signing certificate as beta7: `62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8`.
- Safety: existing signing key and app ID preserved; no user files/audio deleted.

## Release verification
- GitHub prerelease https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.6-beta.8 published; signed APK and SHA256SUMS assets verified, direct APK HTTP200.
- Bilingual site https://alghad.digital/programs/protech-music/ and https://alghad.digital/en/programs/protech-music/ verified HTTP200, correct Beta8 version, APK filename and SHA256. Site commit 4b93e331201da386e9f9c233bcfc96e18ddcb43a, GitHub Pages run 45026730059 SUCCESS.
- Signed beta is a usable download but is not certified stable. No connected physical Android device during this test run, therefore physical Bluetooth/headphones, telephony, battery and interruption QA are BLOCKED.
- Full real app-visible 10k/50k MediaStore scans, API26–36 full matrix, accessibility audits and final release gates also remain unapproved.
- Original overall 11/25=44%, feature F13/15=86.7%, Stage A6/15=40%, Stage B0/15=0%, R0 unchanged until new evidence.
