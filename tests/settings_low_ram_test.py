#!/usr/bin/env python3
"""R-407: on a weak TV whose stock home was turned off the remote's Settings key did nothing outside our home: the stock home held its handler,
and the add-on asks for no key filter on a weak TV (the lag of a 4.1 report). Now, only there (a stock home installed but off, another app in
front, Android 9+), the add-on listens and the key opens Android's settings, behind the family lock as before (openTvSettings). A weak TV with
its stock home on, or none turned off, keeps «never». tests/SettingsLowRamRun.java runs the rules. Run: python3 tests/settings_low_ram_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

svc = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
fk = svc.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("filterKeys asks lowRamCatch with the stock home's state read on a weak TV when the home leaves",
      "SettingsKey.lowRamCatch(front, stockHome, lowRam, stockOff, Build.VERSION.SDK_INT)" in fk
      and "if (!front && lowRam) { stockHome = stockHomeOn(this); stockOff = stockHomeOff(this); }" in fk)
check("the old rule's call is kept as it was", "SettingsKey.filterWanted(front, stockHome, lowRam, homeKeyLost, Build.VERSION.SDK_INT)" in fk)
so = svc.split("private static boolean stockHomeOff(Context c) {")[1].split("\n    }\n")[0] if "stockHomeOff(Context c) {" in svc else ""
check("«off» = the HOME answerers with the disabled ones against the enabled ones", "PackageManager.MATCH_DISABLED_COMPONENTS" in so and "SettingsKey.stockHomeOff(all, enabled, HOME_PKG)" in so)
check("the press goes to openTvSettings (Android's settings, the family lock's PIN first)", "if (d == SettingsKey.OPEN_TV_SETTINGS) new Thread(this::openTvSettings" in svc
      and "ExtrasWords.SETTINGS_PIN" in svc.split("private void openTvSettings() {")[1].split("\n    }\n")[0])
if not shutil.which("javac"):
    print("SKIP SettingsLowRamRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SettingsKey.java"), str(ROOT / "tests/SettingsLowRamRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SettingsLowRamRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of SettingsLowRamRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
