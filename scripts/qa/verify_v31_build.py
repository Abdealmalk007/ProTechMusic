"""Verify Beta 9 Gradle build and JVM test report (Python 3.14 stdlib)."""
from pathlib import Path
from datetime import datetime, timezone
import json
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
build_file = root / "docs/build_v047_beta9_all.log"
log = build_file.read_text(encoding="utf-8",errors="replace") if build_file.exists() else ""
required = (":app:testDebugUnitTest", ":app:assembleDebug", ":app:assembleRelease")
errors = []
if not re.search(r"\bBUILD SUCCESSFUL\b",log): errors.append("Missing BUILD SUCCESSFUL")
if any(token not in log for token in required): errors.append("Missing expected Gradle tasks")
reports = sorted((root / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
if not reports: errors.append("Missing JUnit XML")
tests = failures = exceptions = skipped = 0
for report in reports:
    node = ET.parse(report).getroot()
    tests += int(node.get("tests","0"))
    failures += int(node.get("failures","0"))
    exceptions += int(node.get("errors","0"))
    skipped += int(node.get("skipped","0"))
if tests < 54: errors.append(f"Only {tests} tests available, expected >=54")
if failures or exceptions or skipped:
    errors.append(f"Failed/errors/skipped: {failures}/{exceptions}/{skipped}")
result = {
    "version": "0.4.7-beta.9",
    "utc": datetime.now(timezone.utc).isoformat(),
    "build_succeeded": not errors,
    "expected_tasks": required,
    "junit_reports": len(reports),
    "tests": tests,
    "failures": failures,
    "errors": exceptions,
    "skipped": skipped,
    "issues": errors,
}
target = root / "docs/evidence/v31/build_verification.json"
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
print(json.dumps(result,indent=2,ensure_ascii=False),flush=True)
sys.exit(1 if errors else 0)
