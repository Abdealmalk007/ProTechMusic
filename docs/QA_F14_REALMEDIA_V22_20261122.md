# ProTech Music — F14 actual MediaStore WAV integration, V21/V22

Date: 2026-11-22. **Status: IN_PROGRESS / BLOCKED ON EMULATOR QA, NOT PASS.**

## Scope and isolation
- Debug-only `F14RealMediaQaActivity` inserts small valid PCM WAV fixtures with `ContentResolver.insert` under dedicated emulator-only paths `Music/ProTechMusicQA_F14_V22_<count>/`. Non-emulators are explicitly rejected.
- `app/src/debug/AndroidManifest.xml` registers the QA Activity with `android.permission.DUMP`. The signed public Beta APK does **not** include it.
- The tests preserve user audio; only generated QA files in emulator-specific paths are created. No user songs are deleted or moved.

## Executed tests and findings
1. Original 100-file run generated real WAV files but failed its exact count expectation: previously created QA fixture files with matching name prefix inflated `AudioRepository.scan()` results to 176–178 matches. **FAILED fixture-isolation check**, original error at `docs/evidence/f14_realmedia/f14_real_100.error.txt`.
2. V21 count-specific isolated 10-file run discovered exactly 10 matching tracks via the real `AudioRepository.scan()` path, but the search assertion expected an unpadded title and produced zero hits. **FAILED test assertion** (not a production-search defect by itself); error at `docs/evidence/f14_realmedia/f14_real_10.error.txt`.
3. V22 updated the test to probe the **actual returned `AudioTrack.title`** and validate a matching `uri`; it uses a distinct per-count V22 folder to avoid stale data.
4. V22 compile first hit a missing local `probe` variable, which was repaired. A subsequent Gradle attempt hit a transient Windows `java.net.BindException` in its cache-lock listener. Retrying with `java.net.preferIPv4Stack=true` produced **BUILD SUCCESSFUL** for `:app:assembleDebug`, in `docs/F14_REAL_MEDIA_V22_REBUILD3.log`.
5. Runtime rerun on API35 emulator was blocked when ADB/package-manager commands became intermittently unresponsive. The dedicated test runner and QA AVD were stopped, and **no valid V22 result JSON was recorded**. **No V22 runtime PASS claim**.

## Other verified checks
- Earlier V21 debug Gradle `:app:assembleDebug :app:testDebugUnitTest`: BUILD SUCCESSFUL, **54 JVM tests, 0 failures**.
- Latest public signed preview remains `v0.4.3-beta.5`; production sources are not changed by this debug-only test harness.
- Previous F14 1k/10k/50k synthetic MatrixCursor measurements remain valid as synthetic load tests, not true file-provider 50k benchmarks.

## Remaining gates and next exact steps
- Obtain stable emulator/ADB; run `scripts/qa/RUN_F14_REAL_MEDIA_V22_SMOKE.ps1 -Counts @(10)`. Require JSON with exactly 10 real MediaStore-visible unique WAV and successful search URI assertion. Then scale to 100, 1000, 10k/50k as feasible, with safe QA-specific fixture storage.
- On an authorized physical Android device, measure background battery, sustained memory and playback behavior. No such device was connected in this session.
- F15 still requires an Android instrumentation suite, RC build, issue triage with zero P0/P1 and actual run evidence.
- Progress **unchanged and evidence-based**: feature F 13/15 = **86.7%**; original roadmap 11/25 = **44%**; release R 0/15 = **0%**.
