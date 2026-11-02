#!/usr/bin/env python3
"""The four adhan options of 4.2, all off by default: «الأذان على شاشة التوقف فقط», «تنبيه عند الأذان», «تكبير فقط» and «شاشة التوقف وقت الأذان».
The decisions run for real (tests/AdhanOptionsRun.java, needs javac); how they are wired in the Android code and the screensaver app is read from
the source.   Run: python3 tests/adhan_options_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = HOME / "tilawah"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

text = lambda p: p.read_text(encoding="utf-8")
prefs, receiver, music, tmain = text(T / "Prefs.java"), text(T / "AdhanReceiver.java"), text(T / "MusicReceiver.java"), text(T / "MainActivity.java")
strings = text(ROOT / "android/launcher/src/main/res/values/strings.xml")
manifest = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")

# ---- «الأذان على شاشة التوقف فقط»
check("saver-only is off until it is turned on", 'getBoolean("adhan_saver_only", false)' in prefs)
check("saver-only: the alarm asks AdhanFront and AdhanPlan before it plays", "AdhanPlan.mayPlay(prefs.adhanSaverOnly(), front)" in receiver and "AdhanFront.now(context)" in receiver)
check("saver-only: a skipped adhan is still the alarm's own (it is marked played, never made up) and logs why",
      receiver.index("prefs.setLastDue(due)") < receiver.index("AdhanPlan.mayPlay") and "adhan skipped" in receiver)
front = text(T / "AdhanFront.java")
check("our home tells AdhanFront when it is in front and when it leaves", "AdhanFront.home(true)" in text(HOME / "MainActivity.java") and "AdhanFront.home(false)" in text(HOME / "MainActivity.java"))
check("the screensaver app is asked through its provider, the answer's column checked (an older app answers any path with its settings)",
      'config/dreaming' in front and '"dreaming".equals(cols[0])' in front and "cols.length != 1" in front)
check("an unreadable answer is not known, not «another app»", "return -1;" in front)
prov = text(APP / "ConfigProvider.java")
check("the screensaver's provider answers .../dreaming before anything else", '"dreaming".equals(uri.getLastPathSegment())' in prov and prov.index('"dreaming"') < prov.index("new Prefs(getContext())"))
check("the dream counts itself in and out, the preview too", "Showing.enter()" in text(APP / "ScreenDream.java") and "Showing.leave()" in text(APP / "ScreenDream.java")
      and "Showing.enter()" in text(APP / "MainActivity.java") and "Showing.leave()" in text(APP / "MainActivity.java"))
check("the adhan tab has the row, with the one plain label", "KIND_SAVER_ONLY" in tmain and "<string name=\"adhan_saver_only\">الأذان على شاشة التوقف فقط</string>" in strings)
check("the agent command is declared and answered", '"sa.urtv.home.ADHAN_SAVERONLY"' in manifest and 'case "sa.urtv.home.ADHAN_SAVERONLY"' in music and "adhan_saver_only=" in music)
music_sh = text(ROOT / "optimize/music.sh")
check("music.sh has adhan saver-only and shows it in the state", "saver-only)" in music_sh and "adhan_saver_only=" in music_sh)

if not shutil.which("javac"):
    print("SKIP adhan options: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(T / n) for n in ("AdhanPlan.java",)] + [str(ROOT / "tests/AdhanOptionsRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.AdhanOptionsRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t") for l in out.split("\n") if l]
for name, res in rows:
    check(name, res == "ok")
check("every check ran", len(rows) >= 12, len(rows))
sys.exit(1 if failed else 0)
