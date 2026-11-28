#!/usr/bin/env python3
"""4.4 beta 4: Maghrib was about 2 hours off for a place of one's own in another time zone. The page keeps today's Aladhan answer for the next start
(localStorage «ptimes:…»), and Aladhan writes the times in the zone it is asked for (timezonestring); the key had the place and the method but not
the zone, so a place whose zone was corrected (OwnPlace, or a zone given later) kept showing the times of its old zone, and the countdown counted
to them. The key now carries CONFIG.timezone: the times are only reused for the same zone. The kept answer is evaluated here in node: the
same place with another zone is a different key, and the old place's keys are let go as before.
Run: python3 tests/prayer_cache_zone_test.py"""
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

for page in ("src/template.html", "dist/tv.html", "dist/tv-en.html"):
    s = (ROOT / page).read_text(encoding="utf-8")
    m = re.search(r"const PT = ([^;]+);", s)
    check(f"{page}: the kept prayer times are keyed by the place, the method AND the zone", bool(m) and "CONFIG.timezone" in m.group(1), m and m.group(1))
    check(f"{page}: Aladhan is asked in the same zone the key names", "timezonestring=${encodeURIComponent(CONFIG.timezone)}" in s)
    check(f"{page}: another place's or zone's kept day is let go", "k.startsWith('ptimes:') && k !== PT" in s)

if shutil.which("node"):
    s = (ROOT / "src/template.html").read_text(encoding="utf-8")
    expr = re.search(r"const PT = ([^;]+);", s).group(1)
    js = f"""const keyOf = CONFIG => {expr};
const a = keyOf({{latitude: 51.5, longitude: -0.12, prayerMethod: 3, timezone: 'Asia/Riyadh'}});
const b = keyOf({{latitude: 51.5, longitude: -0.12, prayerMethod: 3, timezone: 'Europe/London'}});
const c = keyOf({{latitude: 51.5, longitude: -0.12, prayerMethod: 3, timezone: 'Europe/London'}});
console.log(JSON.stringify([a !== b, b === c, b.startsWith('ptimes:')]));"""
    out = subprocess.run(["node", "-e", js], capture_output=True, text=True)
    got = json.loads(out.stdout or "[]")
    check("node: the same place in another zone is another key; the same zone the same key", got == [True, True, True], out.stdout + out.stderr)
sys.exit(1 if failed else 0)
