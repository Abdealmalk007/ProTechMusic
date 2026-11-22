#!/usr/bin/env python3
"""Evidence-gated verification of Gradle JVM tests and beta8 release artifact."""
from pathlib import Path
import json
import re
import sys
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[2]
log=root/"docs/build_v046_all.log"
if not log.is_file():
    raise SystemExit("ERROR: build log missing")
raw=log.read_bytes()
body=(raw.decode("utf-16",errors="replace") if raw.startswith(b"\xff\xfe")
      else raw.decode("utf-16-le",errors="replace") if raw.count(b"\x00")>len(raw)//6
      else raw.decode("utf-8",errors="replace"))
if "BUILD SUCCESSFUL" not in body or "BUILD FAILED" in body:
    raise SystemExit("ERROR: Gradle build not successful")
for task in (":app:testDebugUnitTest",":app:assembleDebug",":app:assembleRelease"):
    if not re.search(r"> Task\s+"+re.escape(task)+r"\b",body):
        raise SystemExit(f"ERROR: required task absent: {task}")
test_files=list((root/"app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
if not test_files:
    raise SystemExit("ERROR: JVM XML reports not found")
tests=failures=errors=skipped=0
for name in test_files:
    item=ET.parse(name).getroot()
    tests+=int(item.attrib.get("tests","0"))
    failures+=int(item.attrib.get("failures","0"))
    errors+=int(item.attrib.get("errors","0"))
    skipped+=int(item.attrib.get("skipped","0"))
if tests<54 or failures or errors:
    raise SystemExit(f"ERROR: JVM tests {tests} failures {failures} errors {errors}")
unsigned=root/"app/build/outputs/apk/release/app-release-unsigned.apk"
debug=root/"app/build/outputs/apk/debug/app-debug.apk"
if not unsigned.is_file() or not debug.is_file():
    raise SystemExit("ERROR: debug/release APK missing")
data={
  "version":"0.4.6-beta.8",
  "gradle":"BUILD SUCCESSFUL",
  "required_tasks":[":app:testDebugUnitTest",":app:assembleDebug",":app:assembleRelease"],
  "jvm_tests":{"tests":tests,"failures":failures,"errors":errors,"skipped":skipped,"xml_reports":len(test_files)},
  "release_unsigned_apk_bytes":unsigned.stat().st_size,
  "debug_apk_bytes":debug.stat().st_size,
  "evidence":"app/build/test-results/testDebugUnitTest/TEST-*.xml"
}
out=root/"docs/evidence/v29_build_verification.json"
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8")
print(json.dumps(data,indent=2))
