# ProTech Music v0.4.5-beta.7 — Playback repeat persistence and verified upgrade QA

**Channel:** PRE-RELEASE / BETA ONLY. Not a production/RC-certified release.

## User-visible change
- Repeat mode `OFF / ONE / ALL` is saved and restored across service restarts. The player, on-screen state and persisted preference now agree after repeat changes.
- Previous compact `All / More` controls, queue, Arabic RTL/English, MediaStore indexing and automatic next-track playback remain available.
- Tests extended to verify both Media3 repeat transitions and saved repeat values.

## Evidence and compatibility
- Signed Beta5 (`versionCode=16`) installed and signed Beta6 (`versionCode=17`) installed over it on the same isolated Android 15 API35 emulator without uninstalling. PackageManager showed code17 and screenshots were collected.
- Previously 3 temporary WAV tracks produced transitions `0 -> 1 -> 2`, with repeat/shuffle controls. 10/100/1000 actual MediaStore fixtures and 54 JVM tests passed in earlier milestones.
- Beta7 (`versionCode=18`) is signed with the same release certificate; verify build and public SHA-256 before publishing.
- During signed upgrade testing, an Android emulator **System UI/Pixel Launcher ANR** was observed. It does not establish application UI stability; detailed live UI and preference-retention manual tests remain pending.

## Remaining blockers
- No actual app-visible 10,000 or 50,000 audio-file scan PASS. Shell fixture rows were still `IS_PENDING`; synthetic benchmark is not equivalent.
- A real physical Android device is needed for battery, audio focus/headset/Bluetooth, interruption and sustained session testing.
- The original 25-task roadmap, Stage A 15 tasks and Stage B 15 tasks are not all certified. Do not call this beta stable or final.
- Preserve user audio, persisted application data, app package, and the existing release signing key.

ALGHAD DIGITAL · ProTech · All Rights Reserved.
