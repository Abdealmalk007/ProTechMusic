# ProTech Music — F09 Safe Permanent Deletion QA
Date: 2026-11-21
Build: ProTech Music v0.3.8 DEBUG, Android 15 API35 isolated AVD ProTechMusicQA35_211126, package digital.alghad.protechmusic.
Scope: ONE synthetic deletion-only test WAV. No user-device or real media files touched.

## Fixture
- Seeded E:\ProTech\ProTechMusic\qa\fixtures\qa_v034_delete_only.wav (16044 bytes)
  to /sdcard/Music/qa_F09_DELETE_only.wav on isolated Android emulator.
- MediaStore before test: _id=1000000070, _display_name=qa_F09_DELETE_only.wav, relative_path=Music/.
- Application All Audio search for qa_F09 showed exactly one test fixture; screenshot qa/f09_search_result.png.

## Confirmed app cancel, Android denial, and permitted deletion
1. Three-dot overflow on test audio showed separate red 'حذف الملف نهائيًا من الجهاز'; screenshot qa/f09_menu.png. Android app confirmation warned explicitly irreversible device deletion, not removal from a playlist only; screenshot qa/f09_app_confirm.png.
2. Pressed Cancel in application's confirmation dialog. MediaStore still included _id=1000000070, fixture intact.
3. Reopened and confirmed irreversible deletion in app. Android system PermissionActivity displayed "Allow ProTech Music to delete this audio file?" (screenshot qa/f09_system_request.png).
4. Pressed Deny in Android's system dialog. MediaStore still included fixture _id=1000000070; screenshot qa/f09_denied.png, no fatal exception.
5. Reopened and confirmed app dialog again. Android system PermissionActivity was verified as the top-resumed activity; screenshot qa/f09_allow_os_consent.png.
6. Pressed Android system Allow. MediaStore query showed **no matching row** for qa_F09_DELETE_only.wav. Android shell ls returned "No such file or directory" for the fixture path. Screenshot qa/f09_after_allow.png.
7. All seven earlier synthetic fixture rows remained in MediaStore: music.wav, download.wav, whatsapp.wav, telegram.wav, record.wav, qa_v034_auto_01.wav, qa_v034_auto_02.wav. No observed fatal exception.

## Limits
- Tested API35 only, on generated synthetic fixtures. Android 8-14 and 16, SAF file providers, app-private files, media batch >150, user denial halfway through batches, and physical phone storage behavior remain unverified and must be tested under release Phase R08.
- Users still must independently approve irreversible deletion in Android's system UI; READ_MEDIA_AUDIO alone does not authorize permanent delete of arbitrary app-private audio.
- This passes F09 on API35 emulator without asserting all Android deletion variants are production-ready.
- Current build is DEBUG; NOT a signed public production release.
