#!/usr/bin/env python3
"""Regression gate R-PACKS: the photo packs are downloaded on the TV (docs/PHOTO-PACKS.md), never put in an APK (an APK that carries
one grows by tens of MB for a setting most people never turn on). Checked: no assets source of any app names photos/, a pack or the
data-photo-packs branch; no image over 600 KB under any app's src/main (assets or res); the repository's assets/ has no packs folder.
Run: python3 tests/regressions/no_bundled_packs_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

for gr in sorted((ROOT / "android").glob("*/build.gradle")):
    src = gr.read_text(encoding="utf-8")
    bad = [ln.strip() for ln in src.splitlines() if re.search(r"from\s*\(|srcDirs", ln) and re.search(r"photos|packs?\b|photo-packs", ln.split("//")[0])]
    check(f"{gr.parent.name}/build.gradle copies no photo pack into the APK", not bad, bad)
big = [str(p.relative_to(ROOT)) for p in (ROOT / "android").glob("*/src/main/**/*")
       if p.is_file() and p.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp") and p.stat().st_size > 600_000]
check("no image over 600 KB inside an app's sources", not big, big)
check("assets/ has no packs folder", not any((ROOT / "assets").glob("*pack*")), list((ROOT / "assets").glob("*pack*")))
sys.exit(1 if failed else 0)
