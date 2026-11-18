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

# searches: the first result (country|Arabic) for what people type; CityMatch (saver.sh city): the name it takes
SEARCH = [
    ("الرياض", "SA|الرياض"), ("رياض", "SA|الرياض"), ("Riyadh", "SA|الرياض"), ("riyadh", "SA|الرياض"),
    ("ابها", "SA|أبها"), ("أبها", "SA|أبها"), ("Abha", "SA|أبها"), ("خميس", "SA|خميس مشيط"), ("Khamis Mushait", "SA|خميس مشيط"), ("مشيط", "SA|خميس مشيط"),
    ("جده", "SA|جدة"), ("Jeddah", "SA|جدة"), ("مكه", "SA|مكة المكرمة"), ("Mecca", "SA|مكة المكرمة"), ("المدينه", "SA|المدينة المنورة"),
    ("الدمام", "SA|الدمام"), ("Dammam", "SA|الدمام"), ("الاحساء", "SA|الهفوف"), ("حفر", "SA|حفر الباطن"),
    ("برلين", "DE|برلين"), ("Berlin", "DE|برلين"), ("Munich", None), ("ميونخ", None), ("Hamburg", None), ("Frankfurt", None), ("Cologne", None),
    ("الأحساء", "SA|الهفوف"), ("Hofuf", "SA|الهفوف"), ("Al Ula", "SA|العلا"), ("العلا", "SA|العلا"), ("ديرة", "AE|ديرة"), ("الموصل", "IQ|الموصل"),
    ("Dearborn", "US|ديربورن"), ("ديربورن", "US|ديربورن"), ("Alexandria", "EG|الإسكندرية"), ("Toronto", "CA|تورنتو"), ("Bursa", "TR|بورصة"),
    ("Düsseldorf", "DE|دوسلدورف"), ("Dusseldorf", "DE|دوسلدورف"), ("Stuttgart", "DE|شتوتغارت"), ("شرورة", "SA|شرورة"), ("تنومه", "SA|تنومة"),
    ("Dubai", "AE|دبي"), ("Doha", "QA|الدوحة"), ("Muscat", "OM|مسقط"), ("Cairo", "EG|القاهرة"),
]
MATCH = [("Riyadh", "الرياض"), ("ابها", "أبها"), ("خميس", "خميس مشيط"), ("Alexandria", "الإسكندرية"), ("Dearborn", "ديربورن"), ("Munich", "ميونخ")]
javac, javaexe = shutil.which("javac"), shutil.which("java")
if not (javac and javaexe):
    print("SKIP the Java search: no javac here (CI's Build APKs compiles it)")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    tmp = Path(tmp)
    queries = [q for q, _ in SEARCH] + [q for q, _ in MATCH]
    (tmp / "q.txt").write_text("\n".join(queries) + "\n", encoding="utf-8")
    r = subprocess.run([javac, "-encoding", "UTF-8", "-d", str(tmp)] + [str(SRC / f) for f in ("Cities.java", "CityMatch.java", "TeamSearch.java", "CitiesMore.java", "CitySearch.java")]
                       + [str(ROOT / "tests/CitySearchRun.java")], capture_output=True, text=True)
    check("the search compiles", r.returncode == 0, r.stderr[-800:])
    if r.returncode:
        sys.exit(1)
    r = subprocess.run([javaexe, "-Dfile.encoding=UTF-8", "-cp", str(tmp), "sa.urtv.screensaver.CitySearchRun", str(ASSET), str(tmp / "q.txt")], capture_output=True)
    check("the search runs", r.returncode == 0, r.stderr.decode()[-800:])
    out = r.stdout.decode("utf-8").splitlines()
    first = {l.split("\t")[1]: (l.split("\t")[2:] + [""])[0] for l in out if l.startswith("Q\t")}
    match = {l.split("\t")[1]: l.split("\t")[2:] for l in out if l.startswith("M\t")}
    for q, want in SEARCH:
        got = first.get(q, "")
        if want is None:
            check(f"{q!r} finds a place first", bool(got), got)
        else:
            check(f"{q!r} finds {want} first", got.startswith(want + "|"), got)
    for q, want in [("Munich", "DE|"), ("ميونخ", "DE|"), ("Hamburg", "DE|"), ("Frankfurt", "DE|"), ("Cologne", "DE|"), ("الاحساء", "SA|")]:
        check(f"{q!r} is in {want[:2]}", first.get(q, "").startswith(want), first.get(q))
    for q, want in MATCH:
        check(f"saver.sh city {q!r} takes {want}", [m.split("|")[0] for m in match.get(q, [])] == [want], match.get(q))
    places = [l.split("\t") for l in out if l.startswith("P\t")]
    check("Java reads every place of the file and every built-in city", sum(p[7] == "false" for p in places) == len(rows)
          and sum(p[7] == "true" for p in places) == len(builtin_lines), (len(places), len(rows), len(builtin_lines)))
    check("Java knows every zone exactly (no GMT fallback)", all(p[3] == p[4] for p in places), [p[1:5] for p in places if p[3] != p[4]][:5])
    check("every built-in city (the old ids: name, latitude, longitude) still resolves", all(p[8] == "true" for p in places if p[7] == "true"))
sys.exit(1 if failed else 0)
