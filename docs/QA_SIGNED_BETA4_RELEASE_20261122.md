# ProTech Music v0.4.2-beta.4 — Signed preview release evidence
Date: 2026-11-22. Release channel: **BETA / PREVIEW ONLY**.

## Verified signed APK and source
- Source: https://github.com/Abdealmalk007/ProTechMusic
- Feature commit: e6b738b12cb9c80f3796ed5d849f8757fa68077c; Android F14 test / QA evidence commit: 9c59098ffda7e24e84f44c12c9e5c13104957369.
- Published GitHub pre-release: https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.2-beta.4
- Release assets: `ProTechMusic_v0.4.2_beta4_SIGNED.apk`, `SHA256SUMS_v042_beta4.txt`.
- Release APK: `digital.alghad.protechmusic`, `versionCode=15`, `versionName=0.4.2-beta.4`, 8,991,827 bytes.
- SHA256: `74C54A66C424BC9DEA90F50C3AF1C3FAB2C69EA0EA1299BD312ACDC9321F2928` and matching attached checksum file.
- `assembleRelease` BUILD SUCCESSFUL, APK signature v2 and v3 VERIFIED. Certificate SHA256 `62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8` same as signed prior beta3; signing key not replaced.
- Release APK manifest **does not include** debug-only `F14BenchmarkActivity`.
- Earlier debug build, JVM 53/53 unit tests PASS; F14 Android API35 synthetic MatrixCursor stress passed at 1k/10k/50k without checked AndroidRuntime fatal exception. See `docs/QA_F14_LIBRARY_PERFORMANCE_20261122.md`.

## Site publication
- Site GitHub source commit: `f5b69ef45cb1c0a0da43121b7fe0856b3e5540ff` pushed.
- Python generators and four local Node suites passed: smoke-test, site-search, bilingual and no-duplicate-program.
- GitHub release-facts API correctly returned v0.4.2-beta.4 and the exact signed asset digest.
- GitHub Pages run `44996041224` for commit `f5b69ef` completed SUCCESS (2026-11-22T06:58:04Z).
- Independent LIVE HTTP checks: both Arabic and English ProTech Music pages returned `200` and contained `v0.4.2-beta.4`, exact signed APK filename and matching SHA256 `74C54A66C424BC9DEA90F50C3AF1C3FAB2C69EA0EA1299BD312ACDC9321F2928`.
- Arabic page: https://alghad.digital/programs/protech-music/
- English page: https://alghad.digital/en/programs/protech-music/

## Remaining gates
- Original 25 tasks: 11/25 = 44%. Feature F: 13/15 = 86.7%; **F14 remains IN_PROGRESS**, F15 pending. R: 0/15 PASS.
- True 1k/10k/50k local-media MediaStore integration, physical background-battery discharge/CPU memory tests, index build 50k latency optimization, broad Android/OEM/headset reliability and RC gate are outstanding.
- Signing and publication of a Beta **do not** certify a final or production-ready application.
