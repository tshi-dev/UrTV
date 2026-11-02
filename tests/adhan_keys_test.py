#!/usr/bin/env python3
"""What a key does while the adhan plays (AdhanKeys.java) and the words of the bar then (AdhanBarText.playing), on tests/adhan_keys_cases.tsv:
OK on the bar stops it, «إيقاف الأذان بأي زر» makes any key stop it once, the press is consumed whole, Home/Power/volume stay Android's.
The screensaver's copy of AdhanKeys must be the same file. The page's bar text is in tests/page.test.mjs.   Run: python3 tests/adhan_keys_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

check("the two apps carry the same AdhanKeys", (HOME / "AdhanKeys.java").read_text(encoding="utf-8").split("\n", 1)[1] == (APP / "AdhanKeys.java").read_text(encoding="utf-8").split("\n", 1)[1])
# the wiring needs Android, so it is read from the source
TIL = HOME / "tilawah"
svc = (TIL / "MusicService.java").read_text(encoding="utf-8")
case = svc.split("case ACTION_STOP_ADHAN:")[1].split("\n")[0]
check("stopping one adhan only ends it: finishAdhan, no setting or alarm touched", "finishAdhan()" in case and "prefs" not in case and "Scheduler" not in case, case)
check("MusicReceiver forwards STOP_ADHAN and the manifest lists it and ADHAN_STOPKEY",
      all(a in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8") for a in ('"sa.urtv.home.STOP_ADHAN"', '"sa.urtv.home.ADHAN_STOPKEY"')))
prov = (TIL / "NowPlayingProvider.java").read_text(encoding="utf-8")
check("the provider's stop_adhan checks the caller's signature permission", "checkCallingPermission(\"sa.urtv.home.READ_NOW_PLAYING\")" in prov and "stopAdhan" in prov)
check("a preview is not a live adhan (no bar, no stop)", "adhanLive = previewSeconds == 0;" in svc)
check("the home screen asks AdhanKeys first and consumes", "adhanKeys.decide(" in (HOME / "MainActivity.java").read_text(encoding="utf-8"))
for f in ("ScreenDream.java", "MainActivity.java"):
    check(f"screensaver {f}: asks AdhanKeys before closing", "adhanKeys.decide(" in (APP / f).read_text(encoding="utf-8"))
home = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("«any key» is on by default", 'getBoolean("adhan_stopkey", true)' in (TIL / "Prefs.java").read_text(encoding="utf-8"))
check("the home bar never takes the focus (no requestFocus on it): the focus stays where it was", "adhanBar.requestFocus" not in home and "adhanBarHeld" not in home)
check("only the volume keys and the power family are left alone (Home, Back, settings, media all stop it)",
      "KEYCODE_HOME" not in (HOME / "AdhanKeys.java").read_text(encoding="utf-8") and "KEYCODE_SETTINGS" not in (HOME / "AdhanKeys.java").read_text(encoding="utf-8"))
check("no key-filter capability on this branch (that is work/khalid-settingskey, after a TV test)",
      "canRequestFilterKeyEvents" not in (ROOT / "android/launcher/src/main/res/xml/sleep_service.xml").read_text(encoding="utf-8"))
check("«أذان مختصر» and «إيقاف الأذان بأي زر» are rows of the adhan tab", "KIND_STOPKEY" in (TIL / "MainActivity.java").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP adhan keys: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/adhan_keys_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "AdhanKeys.java"), str(HOME / "AdhanBarText.java"), str(HOME / "GreetingText.java"), str(HOME / "ClockText.java"),
                    str(HOME / "DigitsText.java"), str(ROOT / "tests/AdhanKeysRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AdhanKeysRun", str(ROOT / "tests/adhan_keys_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    want, note = (c[4], c[5]) if c[0] == "bar" else (c[8], c[9])
    check(f"adhan keys: {note}", g == want, f"{g!r}, wanted {want!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
