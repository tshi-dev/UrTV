#!/usr/bin/env python3
"""Every row of the تلاوة stations tab plays its own station: the row keeps the station's place in the catalog (stations.json), never its place
on the screen, so a station the check hides or the «متابعة» row on top moves nothing. Run: python3 tests/station_rows_test.py"""
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (T / "MainActivity.java").read_text(encoding="utf-8")
music = (T / "MusicService.java").read_text(encoding="utf-8")
catalog = (T / "Catalog.java").read_text(encoding="utf-8")
loop = re.search(r"for \(int i = 0; i < cat\.stations\.size\(\); i\+\+\) \{\s*if \(gone\[i\]\) continue;\s*"
                 r"rows\.add\(new Row\(false, KIND_STATION, cat\.stations\.get\(i\)\.name, getString\(R\.string\.live\), i\)\);", main)
check("a station's row is named by the catalog's station i and carries i (a hidden one is skipped, the others keep their own number)", loop is not None)
check("OK on a station row plays the number the row carries", "} else if (row.kind == KIND_STATION) {\n                MusicService.playRadio(this, row.index);" in main
      and "Row row = adapter.rows.get(position);" in main)
check("the service plays that number from the same catalog, in stations.json's order", "Catalog.Station st = cat.stations.get(index);" in music
      and 'JSONArray stations = new JSONObject(read(c, "quran/stations.json")).getJSONArray("stations");' in catalog
      and "catalog.stations.add(st);" in catalog and "continue" not in catalog[catalog.index("quran/stations.json"):catalog.index("catalog.stations.add(st);")])

# the tab's rows as the loop builds them, for every way of hiding stations, each played by its number: the name played is the row's name
names = [s["name"] for s in json.loads((ROOT / "android/launcher/src/main/assets/quran/stations.json").read_text(encoding="utf-8"))["stations"]]
bad = []
for mask in (0, 1 << 1, (1 << 1) | (1 << 12), 0b101010101010, (1 << len(names)) - 2):
    rows = [("متابعة الاستماع", -1)] + [(names[i], i) for i in range(len(names)) if not mask >> i & 1]
    bad += [(mask, shown, names[i]) for shown, i in rows if i >= 0 and names[i] != shown]
check(f"each of the {len(names)} stations' rows plays its own station with any stations hidden and the «متابعة» row on top", not bad and len(names) >= 13, bad[:3])
raise SystemExit(1 if failed else 0)
