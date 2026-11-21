# ProTech Music v0.3.8 — F04 Search, Sorting, Virtualized Pagination QA
Date: 2026-11-21
Computer: DESKTOP-PM75RQM
Status: DEBUG QA, not production.

## Scope
- Native Kotlin TrackSearchEngine, Unicode NFKD Arabic diacritics/tatweel removal, hamza/alif/ya variants, case folding, whitespace normalization.
- Multiword AND search across title, artist, album, source, folder and URI filename, with search controls in the library.
- Explicit sort modes: original playlist order (default), title, artist, source, recently added, duration longest and file size largest.
- Smart playlists/folder/song order unchanged unless sort explicitly chosen.
- UI Compose LazyColumn with 200 result batches; "عرض المزيد" increments by 200 and "عرض الكل" reveals all results. Query/filter/order resets batch window.
- Select all from search results still selects the complete filtered set, not just the currently displayed page. Shared search and sorting applies to all-audio, folder, artist, album, playlist, and smart filters.

## Evidence
- Gradle build :app:assembleDebug and :app:testDebugUnitTest succeeded on Windows for v0.3.8: docs/build_v038_final.log, BUILD SUCCESSFUL.
- JUnit total: 36 PASS, zero failures/errors: TrackSearchEngineTest 7, BackupTrackResolverTest 7, ListeningTrackerTest 8, M3uCodecTest 7, SourceClassifierTest 7.
- TrackSearchEngineTest.largeLibraryTenThousandUsesBoundedWork: 10,000 synthetic tracks, 1 intended search result, JUnit time=0.196 sec on this Windows host (not an Android device performance benchmark).
- Arabic/English case, normalized hamza/alif/tatweel, multi-field terms, zero results, explicit sorting and stable default original playlist order validated by unit tests.
- UI screenshot from previous v0.3.6 build on Android15 320dp with font scale 1.3: qa/v036_search_320.png; shows search and sort controls and navigation safe area; new pagination controls were compiled in v0.3.8, not yet shown with >200 media items.
- Source checkpoint: Git f36cda6; E:\ProTech\Releases\ProTechMusic\ProTechMusic_Source_f36cda6.zip.
- Android15 QA AVD isolated from original device: ProTechMusicQA35_211126. No personal device audio was altered.

## Remaining risk
- The new 200-item page controls have not been proven with an actual 200+ track Android MediaStore library. Treat long-library UI performance as preliminary pending F14.
- Android8-16 device matrix, large real-media libraries, long-running playback, signed release, Bluetooth, and actual phone SAF backup roundtrip remain open.
- No production release, GitHub upload or alghad.digital publication is authorized by this evidence alone.
