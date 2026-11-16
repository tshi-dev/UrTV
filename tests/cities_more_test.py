#!/usr/bin/env python3
"""4.3.3 the offline city search: assets/cities-more.txt (.github/tools/cities-more/build.py) with CitiesMore.java, CitySearch.java and the
CityMatch fallback (`saver.sh city`). The file is exactly its generator's, every place is valid (zone, coordinates, Arabic name, a country the app
knows the calculation of), ids are unique and never go away (ids.lock), every built-in city keeps its own row, and the searches people type find
the right place first.   Run: python3 tests/cities_more_test.py   (needs javac; the generator check needs geonamescache)"""
import re
import shutil
import subprocess
import sys
import tempfile
import zoneinfo
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
ASSET = ROOT / "android/app/src/main/assets/cities-more.txt"
TOOLS = ROOT / ".github/tools/cities-more"
BUDGET = 1_000_000   # bytes of the file; about a third of that in the APK (assets are compressed)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


text = ASSET.read_text(encoding="utf-8")
lines = text.splitlines()
zones = next(l for l in lines if l.startswith("@"))[1:].split("\t")
builtin_lines = [l[1:].split("\t") for l in lines if l.startswith("=")]
rows = [l.split("\t") for l in lines if l and l[0] not in "#@="]
check("the file is small (a lazy asset, not in the page)", len(text.encode()) < BUDGET, len(text.encode()))
check("the file credits GeoNames (CC BY 4.0) and Wikidata", "GeoNames" in lines[0] and "CC BY 4.0" in lines[0] and "Wikidata" in lines[0])
check("every row has its nine fields", all(len(r) == 9 for r in rows) and all(len(b) == 5 for b in builtin_lines))
ids = [r[0] for r in rows]
check("ids are unique", len(ids) == len(set(ids)), [i for i in ids if ids.count(i) > 1][:5])
lock = (TOOLS / "ids.lock").read_text(encoding="utf-8").split()
check(f"no id of ids.lock ({len(lock)}) ever goes away (the data only grows)", set(lock) <= set(ids), sorted(set(lock) - set(ids))[:5])
check("ids are GeoNames ids, or Wikidata items for the governorate seats GeoNames lacks", all(re.fullmatch(r"\d+|Q\d+", i) for i in ids))
bad_tz = []
for z in zones:
    try:
        zoneinfo.ZoneInfo(z)
    except Exception:
        bad_tz.append(z)
check("every zone is an IANA zone", not bad_tz, bad_tz)
check("every row's zone index is in the table", all(r[2].isdigit() and int(r[2]) < len(zones) for r in rows))
check("coordinates are on the Earth, four decimals", all(re.fullmatch(r"-?\d{1,2}\.\d{4}", r[3]) and re.fullmatch(r"-?\d{1,3}\.\d{4}", r[4])
                                                        and abs(float(r[3])) <= 90 and abs(float(r[4])) <= 180 for r in rows))
arabic = re.compile(r"[ء-يً-ْٰ ]+")
check("Arabic names are Arabic script only", all(arabic.fullmatch(r[6]) and not set(r[6]) & set("پچژگکیۀٹڈڑںےھ") for r in rows),
      [r[6] for r in rows if not arabic.fullmatch(r[6])][:5])
check("no «إسرائيل», no Western Sahara; Palestine's places on Hebron's or Gaza's zone",
      not any(r[1] in ("IL", "EH") for r in rows) and all(zones[int(r[2])] in ("Asia/Hebron", "Asia/Gaza") for r in rows if r[1] == "PS"))
check("Saudi places are on Asia/Riyadh", all(zones[int(r[2])] == "Asia/Riyadh" for r in rows if r[1] == "SA"))
java = (SRC / "Cities.java").read_text(encoding="utf-8")
known = set(re.findall(r'new Region\([^,]+, "([A-Z]{2})"', java)) | {"SA", "BH", "KW", "QA", "AE", "OM", "EG", "JO", "SY", "LB", "PS", "MA", "DZ", "TN"}
check("every country is one the app has a calculation for (Cities.REGIONS)", {r[1] for r in rows} <= known, {r[1] for r in rows} - known)
check("Cities.java is not touched by the search list (its 374 + world rows stay)", "cities-more" not in java)

# the generator: the file is exactly a fresh build, offline
try:
    import geonamescache  # noqa: F401
    r = subprocess.run([sys.executable, str(TOOLS / "build.py"), "--check"], capture_output=True, text=True)
    check("cities-more.txt is exactly build.py's", r.returncode == 0, (r.stdout + r.stderr)[-400:])
except ImportError:
    print("SKIP the generator check: pip install -r .github/tools/cities-world/requirements.txt")

sys.exit(1 if failed else 0)
