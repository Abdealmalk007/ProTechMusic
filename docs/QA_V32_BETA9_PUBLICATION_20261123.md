# ProTech Music Beta9: published UX enhancement QA 2026-11-23

**Status:** GitHub and website deployment verified, signed Beta preview. Physical-device and full emulator QA not certified.

## Verified
- Centered 56dp / 68dp / 56dp Previous, Play/Pause, Next. The active song has turquoise text, gradient border and glow, including while paused. Album art removed from player and media notification metadata.
- Existing same-song Pause/Resume toggles without recreating queue; 10-second relative seek, background playback, MediaSession, Repeat and Shuffle preserved in source.
- Static contract: 15/15 PASS in docs/evidence/v31/source_contract.json.
- GitHub CI: https://github.com/Abdealmalk007/ProTechMusic/actions/runs/45118061988 SUCCESS, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease` and BUILD SUCCESSFUL; JUnit 54/54 PASS, 0 failures.
- Signed APK: v0.4.7-beta.9, versionCode 20, size 9008211 bytes, original signer SHA256 62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8, APK v2/v3 PASS.
- APK SHA256: 1B6CBC1A0129369E2F9D27BC4BD4287D7D1F8690D15817A061E7C40BB2744C88.
- GitHub prerelease: https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.7-beta.9 . Both APK and SHA256SUMS assets present; direct APK HEAD HTTP200.
- Website commit ebff0fe34ce16d0c22003c6257efdbb1711ca712, GitHub Pages run https://github.com/Abdealmalk007/alghad-digital/actions/runs/45122802282 SUCCESS.
- Live pages https://alghad.digital/programs/protech-music/ and https://alghad.digital/en/programs/protech-music/ both HTTP200 and matched exact version, APK filename and SHA. Site smoke/search/bilingual/dedup tests PASS.

## Unapproved remaining items
- Android emulator physical UI screenshot verification of centered transport, glow and real pause/resume in Beta9 not executed this run; ADB devices returned none. Source assertions are not runtime proof.
- Full true app-visible 10000 and 50000 real MediaStore scan and sustained performance.
- Physical Android battery, headset/Bluetooth, phone call interruption and audio focus.
- API26–36 complete compatibility and production QA; Stage A 6/15 (40%), Stage B 0/15, original overall 11/25 (44%).
- Never promote to stable without required evidence or change signing key.
