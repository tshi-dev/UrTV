#!/usr/bin/env python3
"""4.3.1 «إذاعة القرآن الكريم - القاهرة»: the second live station, right after the Saudi one (https, radiojar, no local proxy), English «Quran Radio, Egypt», in the generator's list too; a station saved by
an older version after the first is shifted once; CI's «Check the live stations» probes it with the others (it reads stations.json).
    python3 tests/cairo_radio_test.py"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: Path(p).read_text(encoding="utf-8")
st = json.loads(read(ROOT / "android/launcher/src/main/assets/quran/stations.json"))["stations"]
check("the station is second, https radiojar, live, with its English name", st[1] == {"name": "إذاعة القرآن الكريم - القاهرة", "en": "Quran Radio, Egypt", "url": "https://stream.radiojar.com/8s5u5tpdtwzuv", "live": True}, st[1])
check("the Saudi station is still first and no station name is twice", st[0]["name"].endswith("السعودية") and len({s["name"] for s in st}) == len(st))
gen = read(ROOT / ".github/tools/fetch_quran.py").split("STATIONS = [")[1]
check("the generator lists it right after the Saudi one", '"إذاعة القرآن الكريم - القاهرة", "https://stream.radiojar.com/8s5u5tpdtwzuv"' in gen.split("\n")[2], gen.split("\n")[:3])
prefs = read(ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/Prefs.java")
check("a station saved after the first by an older version is shifted once (one flag)", 'sp.getBoolean("stationsCairo", false)' in prefs and "v = v >= 1 ? v + 1 : v;" in prefs and 'putBoolean("stationsCairo", true)' in prefs)
wf = read(ROOT / ".github/workflows/stations-check.yml")
check("CI probes every station of stations.json, and a push to a work branch that changes it runs the check", "'android/launcher/src/main/assets/quran/stations.json'" in wf and "'work/**'" in wf)
cfg = read(ROOT / "android/launcher/src/main/res/xml/network_security_config.xml")
check("cleartext stays for radiojar.com only (the https -> http redirect is followed by StreamUrl), no local proxy", "radiojar.com" in cfg and "127.0.0.1" not in cfg)
sys.exit(1 if failed else 0)
