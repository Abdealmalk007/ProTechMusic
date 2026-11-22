# ProTech Music — V25 large-library and real playback QA

Date: 2026-11-22. Scope: Android 15 API35 disposable emulator.

## Confirmed tests
- `RcSmokeInstrumentation` debug Android instrumentation returned PASS for native launcher, internal (non-exported) MusicService, required vs disallowed permissions, and rendered window in previous Stage A tests.
- `RcPlaybackHarness` was compiled into Android instrumentation and actually executed on API35. Output `RC_PLAYBACK_PASS: auto-next 0->1->2; repeat OFF/ONE/ALL; shuffle ON/OFF` and `INSTRUMENTATION_CODE: -1`, stored in `docs/evidence/v25/playback_instrumentation.txt`. Tests used three 2-second real PCM WAV files created inside emulator app cache and safely cleaned up. This confirms actual ExoPlayer auto-advance and control changes in the emulator; it is not a physical headset/Bluetooth test.
- Previous Stage A actual MediaStore 10, 100, 1000 audio-file tests PASS with raw JSON evidence. JVM suite: 54 tests, 0 failures.
- V25 APK build and instrumentation build were successful (offline Gradle).

## Real 10,000 WAV files: blocked, not passed
- First individual `ContentResolver.insert/write/update` run entered prolonged media-provider stalls after around 4,046 files, even though ADB and the debug app process remained alive; original runner was stopped, no personal media was touched.
- To separate fixture creation from indexing, a QA-only compressed WAV archive was generated and extracted in `Music/ProTechMusicQA_F14_V22_10000/`. Exactly 10,000 unique valid WAV files were confirmed on disk and 10,000 rows appeared in the shell's `MediaStore` query.
- These shell-owned rows had `IS_PENDING=1`. Android hides pending shell-owned files from the app despite granted `READ_MEDIA_AUDIO`. `scanOnly` QA correctly failed with `expected 10000, found 0`, rather than falsely claiming a pass.
- A single-item, shell-owned `MediaStore` update could finalize QA pending status; updating 100 rows took 115 seconds and is unsuitable for scaling. A trial Android shell DEX bulk helper was killed before returning evidence. **No verified large-library app-level scan exists**. The pending state of QA files is an environment/fixture-generation limitation, not evidence that the production search engine crashed on 10,000 visible songs.
- The 50,000-file archive was also generated on host for later QA, but it has NOT been run as a complete app-visible Android library. No 50k PASS claim.

## Release gates and risks
- Physical phone ADB was not connected: calibrated battery, hardware Bluetooth/headset/telephony interruption tests remain BLOCKED.
- Original project progress stays **44%**, feature phase F **86.7%**, production R **0%**. Stage A two-stage checklist stays **6/15 = 40%** until another complete step is proven; Stage B 0/15.
- Signed Beta6 may be published as **prerelease preview only** if build, signer, version, checksum, GitHub and bilingual live site checks pass. No stable/production final claim without the outstanding gates.
