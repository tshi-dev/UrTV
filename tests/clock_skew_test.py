#!/usr/bin/env python3
"""4.4 beta 4: the screensaver showed a wrong Gregorian year on a TV whose clock was off. The app now asks the network for the time when the dream
starts (ClockCheck: one plain-http request to the host it already uses, its «Date:» header; at most once an hour), ClockSkew judges it (more than
12 h apart: off), and the page puts «ساعة التلفزيون غير مضبوطة» where the date was (UrtvNative.clockOff, tests/clock_off.test.mjs). ClockSkew (no
Android) on dates, then the wiring.
Run: python3 tests/clock_skew_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

cc = (APP / "ClockCheck.java").read_text(encoding="utf-8")
br = (APP / "NowPlayingBridge.java").read_text(encoding="utf-8")
dream = (APP / "ScreenDream.java").read_text(encoding="utf-8")
check("ClockCheck: plain http to the host the apps use (https cannot tell a clock a year off), through Net, its «date» header", 'URL = "http://raw.githubusercontent.com/"' in cc
      and "Net.get(URL, null," in cc and 'r.headers.get("date")' in cc and "ClockSkew.judge(System.currentTimeMillis()" in cc)
check("ClockCheck: at most once a day, also across processes (the last answer's time in the app's prefs), on a thread; no network: asked again at the next start",
      "EVERY_MS = 24 * 3600_000L" in cc and "wall - last < EVERY_MS" in cc and "sp.edit().putLong(AT, System.currentTimeMillis())" in cc and "new Thread(" in cc and "askedAt = 0;" in cc)
check("ClockCheck sends nothing of the TV: no extra header (Net's generic agent only)", "Net.get(URL, null," in cc)
check("the dream starts the check", "ClockCheck.start(this);" in dream)
check("the bridge gives the page \"1\", \"0\" or \"\"", "@JavascriptInterface\n    public String clockOff()" in br and 'return s < 0 ? "" : String.valueOf(s);' in br)
check("privacy: the check logs no time or address, only that it is off", "clock: the TV's clock is more than 12 h from the network's" in cc and "Log.i" in cc and cc.count("Log.") == 1)
for page in ("src/template.html", "dist/tv.html"):
    s = (ROOT / page).read_text(encoding="utf-8")
    td = s[s.index("function tickDates(){"):]
    td = td[:td.index("\n}\n")]
    check(f"{page}: tickDates ends with the clock line (over whichever date was written)", td.rstrip().split("\n")[-1].split("//")[0].strip() == "clockOffLine();")
check("tests/run.sh runs the browser half", "node tests/clock_off.test.mjs" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP clock skew: no javac here")
    sys.exit(1 if failed else 0)
NET = 1795865970000   # Sat, 28 Nov 2026 11:39:30 GMT
D = "Sat, 28 Nov 2026 11:39:30 GMT"
H = 3600_000
CASES = [
    ("the same time: right", f"{NET}|{D}", f"{NET} 0"),
    ("two minutes off: right (not worth a word)", f"{NET + 120_000}|{D}", f"{NET} 0"),
    ("11 h off: right", f"{NET - 11 * H}|{D}", f"{NET} 0"),
    ("13 h behind: off", f"{NET - 13 * H}|{D}", f"{NET} 1"),
    ("a year behind (the wrong Gregorian year): off", f"{NET - 365 * 24 * H}|{D}", f"{NET} 1"),
    ("years ahead: off", f"{NET + 3 * 365 * 24 * H}|{D}", f"{NET} 1"),
    ("no date header: not known", f"{NET}|", "-1 -1"),
    ("a header that is not a date: not known", f"{NET}|yesterday", "-1 -1"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(APP / "ClockSkew.java"), str(ROOT / "tests/ClockSkewRun.java")], capture_output=True, text=True)
    check("ClockSkew compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.ClockSkewRun", *[c[1] for c in CASES]], capture_output=True, text=True).stdout.strip().split("\n")
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
