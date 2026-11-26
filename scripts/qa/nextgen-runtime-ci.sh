#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-runtime-evidence
pkg=digital.alghad.protechmusic
runner="$pkg.test/$pkg.RcSmokeInstrumentation"
trap 'adb logcat -d -t 1800 > qa-runtime-evidence/logcat.txt || true' EXIT
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
test "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = 35
if adb shell pm path "$pkg" | grep -q '^package:'; then
  echo 'Unexpected existing installation on disposable runner' >&2
  exit 1
fi
adb install app/build/outputs/apk/debug/app-debug.apk | tee qa-runtime-evidence/install.txt
adb shell dumpsys package "$pkg" > qa-runtime-evidence/permissions-before.txt
grep -q 'android.permission.READ_MEDIA_AUDIO: granted=false' qa-runtime-evidence/permissions-before.txt
adb shell pm grant "$pkg" android.permission.READ_MEDIA_AUDIO
adb shell dumpsys package "$pkg" > qa-runtime-evidence/permissions-after.txt
grep -q 'android.permission.READ_MEDIA_AUDIO: granted=true' qa-runtime-evidence/permissions-after.txt
adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r "$runner" | tee qa-runtime-evidence/smoke.txt
grep -q RC_SMOKE_PASS qa-runtime-evidence/smoke.txt
! grep -q RC_SMOKE_FAIL qa-runtime-evidence/smoke.txt
adb shell am force-stop "$pkg"
adb shell am instrument -w -r -e mode playback "$runner" | tee qa-runtime-evidence/playback.txt
grep -q RC_PLAYBACK_PASS qa-runtime-evidence/playback.txt
! grep -q RC_PLAYBACK_FAIL qa-runtime-evidence/playback.txt
echo 'NEXTGEN_RUNTIME_PASS'
