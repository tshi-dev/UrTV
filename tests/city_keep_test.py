#!/usr/bin/env python3
"""R-403: the city went back to Riyadh after a power cycle on some TCL Android 11 TVs. Prefs.setCity used apply() (a queued write a power cut
loses); now commit(), and the same keys are kept in a file of their own (CityKeep), read back only when the settings file has no city at all.
tests/CityKeepRun.java runs CityKeep across a restart on a fake storage (only commit() reaches the disk). Run: python3 tests/city_keep_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

prefs = (SAVER / "Prefs.java").read_text(encoding="utf-8")
sc = prefs.split("void setCity(String name, String lat, String lon, String tz, String country) {")[1].split("\n    }\n")[0]
th = sc.split("new Thread(")[1] if "new Thread(" in sc else ""
check("setCity forces the settings to the disk (an empty commit) and keeps the same five values, off the UI thread",
      "sp.edit().commit();" in th and "CityKeep.save(app, name, lat, lon, fTz, fCode);" in th and sc.index('.putString("cityCountry", code)') < sc.index("new Thread("))
ctor = prefs.split("    Prefs(Context c) {")[1].split("\n    }\n")[0]
check("the first Prefs of a start puts a lost city back, right after the file is opened (before anything reads it)",
      'if (!sp.contains("cityName")) CityKeep.restore(c, sp);' in ctor and ctor.index("CityKeep.restore") < ctor.index('sp.contains("tournaments")'))

STUBS = {
    "android/content/SharedPreferences.java": "package android.content;\npublic interface SharedPreferences {\n  boolean contains(String k);\n  String getString(String k, String d);\n"
        "  Editor edit();\n  interface Editor { Editor putString(String k, String v); boolean commit(); void apply(); }\n}\n",
    "android/content/Context.java": "package android.content;\npublic abstract class Context {\n  public static final int MODE_PRIVATE = 0;\n"
        "  public abstract SharedPreferences getSharedPreferences(String name, int mode);\n}\n",
    "android/util/Log.java": "package android.util;\npublic final class Log { public static int i(String t, String m) { return 0; } }\n",
}
if not shutil.which("javac"):
    print("SKIP CityKeepRun: no javac here"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)
    for rel, src in STUBS.items():
        (t / "src" / rel).parent.mkdir(parents=True, exist_ok=True); (t / "src" / rel).write_text(src)
    files = [str(t / "src" / r) for r in STUBS] + [str(SAVER / "CityKeep.java"), str(ROOT / "tests/CityKeepRun.java")]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(t / "out")] + files, capture_output=True, text=True)
    check("CityKeep compiles against the stubs", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", str(t / "out"), "sa.urtv.screensaver.CityKeepRun"], capture_output=True, text=True).stdout
        print(out.strip())
        check("every case of CityKeepRun ran", "ALL PASS" in out, out[-300:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
