#!/usr/bin/env python3
"""4.3 C16: a second recording of the person's own, for Fajr only («أذاني للفجر»): optimize/music.sh adhan add --fajr FILE keeps it in its own
file (adhan-own-fajr.m4a) and plays it at Fajr; the first recording and the other prayers' choice stay as they were; «مؤذن الفجر» lists it
first; a numbered Fajr voice or the first recording chosen again turns it off; own-clear --fajr removes it, own-clear removes both.
    python3 tests/own_adhan_fajr_test.py"""
import subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
read = lambda n: (T / n).read_text(encoding="utf-8")
own, prefs, svc, recv, act, sets = read("OwnAdhan.java"), read("Prefs.java"), read("MusicService.java"), read("MusicReceiver.java"), read("AdhanActivity.java"), read("AdhanSettings.java")
check("its own file and voice number, never a numbered one (1..21) or the first recording (0)", 'new File(c.getFilesDir(), "adhan-own-fajr.m4a")' in own and "static final int OWN_FAJR = 100;" in prefs)
check("Fajr plays it when chosen and on the TV, else as before (the first recording, else the Fajr voice)",
      "if (fajrOwnSecond() && OwnAdhan.fajrExists(c)) return OWN_FAJR;" in prefs and "return fajrOwn() && OwnAdhan.exists(c) ? OWN : fajrVoice();" in prefs)
check("choosing a numbered Fajr voice or the first recording for Fajr turns it off", prefs.count('.putBoolean("adhan_fajr_own_second", false)') == 2)
check("the player plays the right file, from the TV, and a removed one falls back", "OwnAdhan.fileOf(this, voice)" in svc and "OwnAdhan.isOwn(wanted) && !OwnAdhan.existsOf(this, wanted)" in svc)
check("added with fajr=true: only Fajr changes; a first recording added later leaves a chosen Fajr one", 'boolean forFajr = intent.getBooleanExtra("fajr", false);' in recv
      and "OwnAdhan.keep(context, src, forFajr)" in recv and "if (!prefs.fajrOwnSecond()) prefs.setFajrOwn(true);" in recv and 'case "clear-fajr":' in recv)
check("«مؤذن الفجر» lists it first; choosing it is saved as the Fajr recording", "ownFajr = fajr && OwnAdhan.fajrExists(this) ? 1 : 0;" in act
      and "if (ownFajr == 1 && position == 0) return Prefs.OWN_FAJR;" in act and "voices.length + own + ownFajr" in act and "prefs.setFajrOwnSecond(true); }" in act)
check("saver/phone/ADB set it the same way (AdhanSettings), refused when there is none", "if (voice == Prefs.OWN_FAJR) {" in sets and "adhan add --fajr FILE" in sets)
ms = (ROOT / "optimize/music.sh").read_text(encoding="utf-8")
check("music.sh: adhan add --fajr FILE, fajr-voice own-fajr, own-clear --fajr", 'OWN_FAJR=1 own_add "$3"' in ms and '--ez fajr true' in ms
      and 'FAJR_VOICE --ei voice 100' in ms and "op clear-fajr" in ms)
r = subprocess.run(["bash", "-n", str(ROOT / "optimize/music.sh")], capture_output=True, text=True)
check("music.sh parses", r.returncode == 0, r.stderr)
check("docs/SETTINGS.md names it", "adhan add --fajr" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
