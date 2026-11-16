#!/usr/bin/env python3
"""4.4 touch «أذان الجمعة»: a Dhuhr set silent in «أذان كل صلاة» is heard on Fridays (on by default; it changes nothing unless Dhuhr is silent).
PrayerSound run for real; the source checks: Friday in the city's own time, music.sh and docs/SETTINGS.md.   python3 tests/friday_dhuhr_test.py"""
import subprocess, sys, tempfile, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
prefs = (T / "Prefs.java").read_text(encoding="utf-8")
check("on by default", 'sp.getBoolean("friday_dhuhr", true)' in prefs)
check("Friday is the city's own day", "sa.urtv.home.ZoneFix.zone(Place.get(c).tz)" in prefs[prefs.index("int playFor("):] and "java.util.Calendar.FRIDAY" in prefs)
check("music.sh adhan friday, its status, docs/SETTINGS.md", '"sa.urtv.home.FRIDAY_DHUHR"' in (T / "MusicReceiver.java").read_text(encoding="utf-8")
      and "adhan friday on | off" in (ROOT / "optimize/music.sh").read_text(encoding="utf-8") and "| the Friday adhan (4.4) | أذان الجمعة |" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))
check("its row in «أذان كل صلاة»", 'S.t("أذان الجمعة")' in (T / "PrayerSoundActivity.java").read_text(encoding="utf-8"))
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
public class FridayRun { public static void main(String[] a) {
  int g = 3;
  boolean ok = PrayerSound.voice("silent", 2, g, false, false, true) == g          // Friday: the silent Dhuhr is heard
    && PrayerSound.voice("silent", 2, g, false, false, false) == PrayerSound.SILENT  // another day: silent
    && PrayerSound.voice("silent", 3, g, false, false, true) == PrayerSound.SILENT   // Asr stays silent on Friday
    && PrayerSound.voice("v5", 2, g, false, false, true) == 5                        // a chosen muezzin stays
    && PrayerSound.voice("", 2, g, false, false, true) == g;
  System.out.println(ok ? "ok" : "bad");
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "FridayRun.java").write_text(run)
        Path(tmp, "Prefs.java").write_text("package sa.urtv.home.tilawah;\nfinal class Prefs { static final int OWN = 0; static final int OWN_FAJR = 100; }\n")
        r = subprocess.run(["javac", "-d", tmp, str(T / "PrayerSound.java"), str(T / "AdhanVoices.java"), str(Path(tmp, "Prefs.java")), str(Path(tmp, "FridayRun.java"))], capture_output=True, text=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.FridayRun"], capture_output=True, text=True).stdout.strip() if r.returncode == 0 else r.stderr
        check("a silent Dhuhr is heard on Friday only; Asr stays silent; a chosen muezzin stays", out == "ok", out)
sys.exit(1 if failed else 0)
