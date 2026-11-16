#!/usr/bin/env python3
"""4.4 touch «ساعات الهدوء»: off by default; inside the window no dua or reminder plays; the adhan in it is heard (default), Fajr only, or silent.
QuietHours and DuaPlan run for real (tests/QuietHoursRun.java); the source checks say the alarm and the receiver ask it, in the city's time.
    python3 tests/quiet_hours_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

prefs = read(T / "Prefs.java")
check("off by default, 23:00 to 05:00 when first turned on, the adhan heard", 'getBoolean("quiet_on", false)' in prefs and '"quiet_from", 23 * 60' in prefs
      and '"quiet_to", 5 * 60' in prefs and '"quiet_adhan", QuietHours.ADHAN_HEARD' in prefs)
check("judged in the city's own time", "sa.urtv.home.ZoneFix.zone(Place.get(c).tz)" in prefs[prefs.index("boolean quietNow"):])
check("the adhan's alarm asks it", "QuietHours.silencesAdhan(prefs.quietOn(), prefs.quietNow(context), prefs.quietAdhan(), prayer)" in read(T / "AdhanReceiver.java"))
check("the dua's alarm asks it", "new Prefs(c).quietNow(c));" in read(T / "DuaReceiver.java"))
check("music.sh adhan quiet, its status, docs/SETTINGS.md", '"sa.urtv.home.QUIET"' in read(T / "MusicReceiver.java") and "adhan quiet off |" in read(ROOT / "optimize/music.sh")
      and "| quiet hours (4.4) | ساعات الهدوء |" in read(ROOT / "docs/SETTINGS.md"))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "QuietHours.java"), str(T / "PrayerSound.java"), str(T / "AdhanVoices.java"), str(T / "DuaPlan.java"),
                            str(ROOT / "tests/QuietHoursRun.java"), "-implicit:none", "-sourcepath", str(T)], capture_output=True, text=True)
        if r.returncode != 0:   # PrayerSound names Prefs' numbers: a stub of them
            Path(tmp, "Prefs.java").write_text("package sa.urtv.home.tilawah;\nfinal class Prefs { static final int OWN = 0; static final int OWN_FAJR = 100; }\n")
            r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "QuietHours.java"), str(T / "PrayerSound.java"), str(T / "AdhanVoices.java"), str(T / "DuaPlan.java"),
                                str(Path(tmp, "Prefs.java")), str(ROOT / "tests/QuietHoursRun.java")], capture_output=True, text=True)
        check("QuietHours compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.QuietHoursRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 11, len(out.splitlines()))
else:
    print("SKIP the run: no javac")
sys.exit(1 if failed else 0)
