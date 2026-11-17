#!/usr/bin/env python3
"""4.5 «نسخة الإعدادات» (docs/BACKUP.md): the screensaver's part of the schema-2 file is made on the TV by SaverFile.fromGet (Java), and must be
exactly what optimize/settings_move.read_tv makes from the same GET answer (schema 1: the computer's export), so one file reads the same on both
paths. Both run on the same answers here and must give the same JSON. No TV, no Android.
Run: python3 tests/saver_file_test.py"""
import importlib.util, json, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
spec = importlib.util.spec_from_file_location("settings_move", ROOT / "optimize/settings_move.py")
sm = importlib.util.module_from_spec(spec); spec.loader.exec_module(sm)

FULL = """saved: ok
city: الرياض (24.7136, 46.6753)
timezone: Asia/Riyadh
country: SA
digits: hindi
clocksize: large
cardstyle: glass3
clock24: off
greeting: on
sunrise: off
prayerbar: on
wxmore: off
pagesdark: on
months: levant
lang: ar
listenpill: on
updatebell: off
pillshidden: markets,phone,nope
homephotos: own
homepacks: alula,riyadh-night,BAD
accent: teal
focusstyle: frame
baropacity: 70
walldim: 30
albummode: ten
albumdate: on
glow: light
cleartext: off
albumtitle: on
albumfit: fill
albumsize: tall
looksync: on
homeours: off
favbar: 7
prayermethod: gulf
prayeradj: on fajr=+2 maghrib=-1 bogus=3
hijrioffset: -1
panel: prayers
panel2: forecast
home_panel: nextprayer,matches,stocks
markets: off tasi,gold spx
mkstyle: arrows
teams: 650|الهلال, 2|#2, |x
"""
SPARSE = "city: Some Town (1.5, -3.0)\ntimezone: Europe/London\ncountry: \nhome_panel: none\nteams: \nmarkets: \n"
NOTGET = "error: no such action"
cases = {"a full answer": FULL, "a sparse one (an older app)": SPARSE, "no city line": "digits: hindi\n", "an error": NOTGET}

def py(text):
    def fake_tv(cmd):
        if cmd.endswith("GET_PLAYER"): raise sm.TvError("no home here")
        return text
    sm.tv = fake_tv
    try:
        return json.dumps(sm.read_tv(), ensure_ascii=False)
    except sm.TvError:
        return "null"

if not shutil.which("javac"):
    print("SKIP saver file parity: no javac here"); sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverFile.java"),
                    str(ROOT / "tests/SaverFileRun.java")], check=True, capture_output=True)
    for name, text in cases.items():
        f = Path(tmp) / "get.txt"; f.write_text(text, encoding="utf-8")
        java = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SaverFileRun", str(f)], capture_output=True, text=True, encoding="utf-8").stdout
        p = py(text)
        check(f"{name}: the TV's part is the computer's schema 1, byte for byte", java == p, f"\n java {java}\n py   {p}")
    # the write side: a file's "saver" part → the SET extras, the same as settings_move.ops(flat(file)) (the home's adhan and reciters aside)
    import shlex
    files = {
        "the full answer's own file": json.loads(py(FULL)),
        "a file with adhan, reciters, a negative adjustment and a city at 0": {"schema": 1, "city": {"name": "Null Island", "lat": 0.0, "lon": -0.0000001, "tz": "UTC"},
            "prayer": {"method": "auto", "adjust_on": False, "adjust": {"isha": -3, "fajr": 0}}, "hijri_offset": 0,
            "adhan": {"on": True, "bar": False, "voice": 3}, "reciter_favs": ["afasy"], "markets": {"list": [], "on": True}, "teams": [{"id": "9", "name": ""}]},
    }
    for name, f in files.items():
        p = Path(tmp) / "f.json"; p.write_text(json.dumps(f, ensure_ascii=False), encoding="utf-8")
        java = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.SaverFileRun", str(p), "extras"], capture_output=True, text=True, encoding="utf-8").stdout
        cmd, _home = sm.ops(sm.flat(f), sm.team_names(f))
        words = shlex.split(cmd)[len(shlex.split(sm.SAVER + "SET")):] if cmd else []
        pyx = "".join(f"{words[i + 1]}={words[i + 2]}\n" for i in range(0, len(words), 3))
        check(f"{name}: the TV's SET extras are the computer's, in order", java == pyx, f"\n java {java!r}\n py   {pyx!r}")
    for name, text, want in (("a key twice", '{"a": 1, "a": 2}', "given twice"), ("NaN", '{"a": NaN}', "a value expected"), ("cut short", '{"a": [1, 2', "expected"),
                             ("nested 50 deep", "[" * 50 + "]" * 50, "nested too deeply")):
        p = Path(tmp) / "bad.json"; p.write_text(text, encoding="utf-8")
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.SaverFileRun", str(p), "extras"], capture_output=True, text=True).stdout
        check(f"the JSON reader refuses {name}", out.startswith("refused:") and want in out, out)
src = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverFile.java").read_text(encoding="utf-8")
check("no Android in SaverFile", "import android" not in src)
for name, values in (("SWITCHES", sm.SWITCHES), ("MONTHS", sm.MONTHS), ("LANGS", sm.LANGS), ("FAV_BARS", sm.FAV_BARS), ("QUICK_BUTTONS", sm.QUICK_BUTTONS),
                     ("HOME_PHOTOS", sm.HOME_PHOTOS), ("ACCENTS", sm.ACCENTS), ("FOCUS_STYLES", sm.FOCUS_STYLES), ("ALBUM_MODES", sm.ALBUM_MODES),
                     ("GLOWS", sm.GLOWS), ("ALBUM_FITS", sm.ALBUM_FITS), ("ALBUM_SIZES", sm.ALBUM_SIZES), ("PRAYERS", sm.PRAYERS)):
    java_list = src.split(name + " = {", 1)[1].split("}", 1)[0]
    check(f"{name}: the same list as settings_move.py", [x.strip().strip('"') for x in java_list.split(",")] == list(values), java_list)
sys.exit(1 if failed else 0)
