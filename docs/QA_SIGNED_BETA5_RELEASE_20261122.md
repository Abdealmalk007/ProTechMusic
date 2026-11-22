# ProTech Music — v0.4.3-beta.5 signed beta publication
Date 2026-11-22; channel: PRE-RELEASE / BETA ONLY.

## Source and build
- Source commit: `2f93c7d347f93de496071a23d6ee006046ca8501` on `Abdealmalk007/ProTechMusic`.
- Android/Kotlin: `54/54` JVM tests PASS; debug APK build PASS; synthetic API35 stress 1k/10k/50k PASS.
- Release `assembleRelease`: BUILD SUCCESSFUL; APK v2/v3 signature verification PASS.
- Package `digital.alghad.protechmusic`, versionCode 16, versionName `0.4.3-beta.5`.
- APK size 8,991,827 bytes; SHA256 `B99ACDCDE1FBF1EFC63EEBBB181D06A0FAB88425E1D2E732437C4FC5E6B21A85`, matches `SHA256SUMS_v043_beta5.txt`.
- Signer SHA256 `62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8`, verified identical to previous beta4 signer.
- Debug-only `F14BenchmarkActivity` not present in signed release manifest.

## Verified publication
- GitHub prerelease `v0.4.3-beta.5`: https://github.com/Abdealmalk007/ProTechMusic/releases/tag/v0.4.3-beta.5
- APK https://github.com/Abdealmalk007/ProTechMusic/releases/download/v0.4.3-beta.5/ProTechMusic_v0.4.3_beta5_SIGNED.apk
- SHA256SUMS file included as separate release asset. GitHub release marked `prerelease=true`.
- Website commit `f46d8e571e93d940d86ba8eab5bbdc5a41d304b5` pushed to `Abdealmalk007/alghad-digital`.
- GitHub Pages deployment run 44999160566 completed SUCCESS, https://github.com/Abdealmalk007/alghad-digital/actions/runs/44999160566
- Live Arabic and English pages both responded HTTP200 and displayed exact release version, signed APK filename and SHA256:
  - https://alghad.digital/programs/protech-music/
  - https://alghad.digital/en/programs/protech-music/
- Local JS syntax, release-facts, SEO, bilingual, site-search, smoke and duplicate-program tests passed before website commit.

## Open gates
- F14 full real 1k/10k/50k audio-file MediaStore tests, physical battery/background CPU and long-run memory, and systemic emulator UI ANR validation remain outstanding.
- F15 release-candidate and R01–R15 full-production gates are not certified.
- Official progress unchanged: F 13/15=86.7%, original roadmap 11/25=44%, R=0%. Beta is not stable or final.
