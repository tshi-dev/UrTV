#!/usr/bin/env python3
"""4.4 «أذان كل صلاة»: which of the five prayers play the adhan's sound and with what; the others keep their time, card, bar and notice, silent.
PrayerSound (run for real, tests/PrayerSoundRun.java); the alarm uses it; nothing changes for a TV where nothing was chosen; music.sh and
docs/SETTINGS.md have the command.   python3 tests/prayer_sound_test.py"""
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

recv, prefs, ps, rec = read(T / "AdhanReceiver.java"), read(T / "Prefs.java"), read(T / "PrayerSound.java"), read(T / "MusicReceiver.java")
check("PrayerSound is plain Java", "import android" not in ps)
check("the default is the general adhan (an empty word): nothing changes for a TV where nothing was chosen", 'sp.getString("pp_" + prayer, "")' in prefs and 'if (!valid(choice) || choice.isEmpty()) return general;' in ps)
check("the alarm plays the prayer's own choice; a silent prayer still shows its notice and its screensaver", "prefs.playFor(context, prayer)" in recv
      and "boolean play = prefs.adhanOn() && AdhanPlan.mayPlay(prefs.adhanSaverOnly(), front) && !silent;" in recv and recv.index("boolean notice = prefs.adhanNotice() && prefs.screenOn(prayer);") > recv.index("boolean silent"))
check("music.sh adhan prayer, its status, and the line in docs/SETTINGS.md", '"sa.urtv.home.PRAYER_SOUND"' in rec and "|prayer_sound=" in rec
      and "adhan prayer <fajr|dhuhr|asr|maghrib|isha>" in read(ROOT / "optimize/music.sh") and "| the adhan per prayer (4.4) | أذان كل صلاة |" in read(ROOT / "docs/SETTINGS.md"))
check("the adhan tab opens it and says which prayers are silent", "startActivity(new Intent(this, PrayerSoundActivity.class));" in read(T / "MainActivity.java")
      and "PrayerSoundActivity.summary(prefs)" in read(T / "MainActivity.java"))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        stub = Path(tmp, "Prefs.java")   # Prefs is Android: only its two numbers are needed here
        stub.write_text("package sa.urtv.home.tilawah;\nfinal class Prefs { static final int OWN = 0; static final int OWN_FAJR = 100; }\n")
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "PrayerSound.java"), str(T / "AdhanVoices.java"), str(stub), str(ROOT / "tests/PrayerSoundRun.java")], capture_output=True, text=True)
        check("PrayerSound compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        check("the stub's numbers are Prefs' own", "static final int OWN = 0;" in prefs and "static final int OWN_FAJR = 100;" in prefs)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.PrayerSoundRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 8, len(out.splitlines()))
else:
    print("SKIP the run: no javac")
sys.exit(1 if failed else 0)
