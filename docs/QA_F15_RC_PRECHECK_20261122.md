# ProTech Music — F15 Release Candidate Precheck
Date: 2026-11-22. Status: **IN_PROGRESS / NOT PASSED**.

## Baseline
- Current signed public preview: `v0.4.3-beta.5`, package `digital.alghad.protechmusic`, versionCode 16.
- Production GitHub release and bilingual pages were verified in `docs/QA_SIGNED_BETA5_RELEASE_20261122.md`.
- Current source feature milestone F: **13/15 = 86.7%**; original roadmap: **11/25 = 44%**; R: **0/15 = 0%**.
- Latest Gradle `testDebugUnitTest` and `assembleDebug` (with debug-only real-file fixture harness): BUILD SUCCESSFUL. **54/54 JVM tests passed**.
- Debug test entrypoints are declared solely in `app/src/debug/AndroidManifest.xml`, protected with `android.permission.DUMP` and not packaged in the public release manifest.

## F14 true MediaStore fixture — independent coverage
- Added `app/src/debug/java/digital/alghad/protechmusic/F14RealMediaQaActivity.kt` to generate **actual PCM WAV files through MediaStore**, restricted to an API35 emulator and `Music/ProTechMusicQA_F14_V21_<count>/`, with names `ProTechQA_V21_<count>_XXXXX.wav`.
- QA can smoke-test 10 files, then 100 and 1,000 files, measures actual `AudioRepository.scan()` and `TrackSearchEngine.prepare()`, file counts, uniqueness, scan time and process PSS/CPU.
- Test fixtures are restricted to emulator-only WAV files. The initial 100-file run **FAILED** because older QA files using the same prefix were counted as duplicates (176-178 matches rather than 100); this is a fixture-isolation defect, not a verified application playback defect. The original failure evidence was retained in `docs/evidence/f14_realmedia/f14_real_100.error.txt`. The corrected V21 fixture uses a **count-specific folder and unique filename prefix** to isolate runs; the correction requires a fresh test before claiming PASS. No user media was deleted.
- Test runner `scripts/qa/RUN_F14_REAL_MEDIA_V22_SMOKE.ps1`; QA reports expected under `docs/evidence/f14_realmedia/`. **This test remains pending unless result JSON and emulator evidence exist.**
- Previous 1k/10k/50k `MatrixCursor` stress remains valid as synthetic metadata performance proof, not real media-file performance.

## RC gate blockers and acceptance
- Full true 10,000/50,000 audio-file MediaStore runs and low-end device responsiveness unverified.
- Calibrated foreground/background battery, sustained memory/CPU and headset/call interruption tests require physical Android hardware.
- Android `androidTest` instrumentation module and completed UI automation suite remain missing; do not claim instrumentation PASS.
- Prior emulator had transient system-level `System UI isn't responding` incident; repeat and triage separately from app process.
- The release-candidate gate requires real build and test evidence, issue triage with zero unresolved P0/P1, and approved F14 dependencies; **F15 remains IN_PROGRESS**.

## Current next actions
1. Complete 100/1k real-media fixture QA (if emulator becomes operational) and attach JSON.
2. Scale to 10k/50k on dedicated QA device without modifying user audio.
3. Integrate real Android instrumentation tests, run signed upgrade and playback regressions, then triage P0/P1.
4. Only after gates PASS may the percent change. Do not call a Beta stable/final.

- A subsequent V21 emulator installation attempt using streaming ADB stalled; non-streaming fallback was attempted. Do not mark real WAV scan PASS until the new debug APK installs and valid JSON evidence exists.

- V21 real-file scan found 10 files but the search assertion incorrectly assumed MediaStore would preserve the unpadded input TITLE. V22 queries the actual returned track title and verifies the matching URI; a count-specific V22 folder prevents mixing fixtures. This is a QA assertion correction, with re-test required.

- V22 debug APK build: Gradle :app:assembleDebug **BUILD SUCCESSFUL** (2026-11-22); JVM suite 54/54 PASS from V21. V22 fixed the actual-title search target after the prior ten-file scan returned the expected count but the hard-coded unpadded title probe failed. V22 real-file test has not been certified until result JSON is recorded.
