#!/usr/bin/env python3
"""Choose a strictly newer Android version across release and preview tag formats."""
import re
import sys

versions = []
for tag in sys.stdin.read().splitlines():
    match = re.fullmatch(r"(?:preview-)?v1\.0\.(\d+)(?:-[A-Za-z0-9.-]+)?", tag.strip())
    if match:
        versions.append((int(match.group(1)), tag.strip()))
if not versions:
    raise SystemExit("No published APK version found; refuse an unverified version code")
previous, tag = max(versions)
print(f"APK_CODE={max(223, previous + 1)}")
print(f"PREVIOUS_RELEASE_TAG={tag}")
