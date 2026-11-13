#!/usr/bin/env python3
"""4.3.1 «التطبيقات المعطّلة»: the list (apps off by hand, UrTV's first), the one confirmation, the result per app, the screen when no route exists, behind the family PIN, and the
engine turning apps ON only (the ledger kept exact). DisabledApps and SetupEngine.enableApps run on a plain JVM against SetupEngineRun's fake TV; the screen is read from the source.
    python3 tests/disabled_apps_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SETUP = HOME / "setup"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: Path(p).read_text(encoding="utf-8")
act, eng, mf = read(SETUP / "DisabledAppsActivity.java"), read(SETUP / "SetupEngine.java"), read(ROOT / "android/launcher/src/main/AndroidManifest.xml")
st, hub, words = read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java"), read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsHub.java"), read(ROOT / "android/app/src/main/java/sa/urtv/screensaver/Words.java")
check("behind the family PIN, a cancelled PIN leaves", "PinGuard.confirm(this" in act and "() -> list(), () -> finish()" in act)
check("it only ever turns apps on: no disable, uninstall or clear anywhere in the screen", "pm disable" not in act and "disable-user" not in act and "pm uninstall" not in act and "pm clear" not in act
      and "enableApps(" in act)
check("the engine's method uses `pm enable --user 0` only and never a disable", "sh.run(\"pm enable --user 0 \" + p)" in eng.split("List<AppResult> enableApps")[1].split("private void dropFrom")[0]
      and "pm disable" not in eng.split("List<AppResult> enableApps")[1].split("private void dropFrom")[0])
check("the list needs no connection (PackageManager), the screens are: list, one confirmation (focus on «إلغاء»), the result per app, no route", "getApplicationEnabledSetting" in act and "private void sure()" in act
      and "focus(cancel)" in act and "private void result(" in act and "private void noRoute(" in act)
check("«عطّلها UrTV» is small plain text, no outlined pill; Latin digits (String.valueOf), no «·» or «•»", 'S.t("عطّلها UrTV"), 16, "Regular"' in act and "String.valueOf(n)" in act and "·" not in act and "•" not in act)
check("a failed app can be tried again; the no-route screen points to the pairing steps and changes nothing", "جرّب اللي ما اشتغل من جديد" in act and "خطوات الإقران" in act and "ما تغيّر شيء" in act)
check("the connection is the TV's own adb first, then the paired route (SetupSession.openAny), the screensaver held back while the TV asks", "SetupSession.openAny(this," in act and "SaverHold.begin(" in act)
check("the activity is exported only behind the signature permission; its row is in the settings (home page) and in the hub and the word lists",
      'android:name=".setup.DisabledAppsActivity"' in mf and mf.split('android:name=".setup.DisabledAppsActivity"')[1][:200].count('android:permission="sa.urtv.home.OPEN_CLEAN"') == 1
      and 'row("التطبيقات المعطّلة"' in st and "DisabledAppsActivity" in st and '{"التطبيقات المعطّلة", HOME}' in hub and 'S.n("التطبيقات المعطّلة")' in words)
check("«إعداد تلفزيوني» is a row of the settings that always opens the setup, even when UrTV is already the home (4.3.1)", 'row("إعداد تلفزيوني"' in st and "openSetup()" in st.split('row("إعداد تلفزيوني"')[1][:200] and '{"إعداد تلفزيوني", HOME}' in hub and 'S.n("إعداد تلفزيوني")' in words)
if not shutil.which("javac"):
    print("SKIP logic: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in read(p)] + [HOME / "phone/UpdateRules.java", HOME / "SuggestedApps.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/DisabledAppsRun.java")], capture_output=True, text=True)
    check("the rows, the engine, the fake TV and the runner compile", r.returncode == 0, r.stderr[-800:])
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.DisabledAppsRun"], capture_output=True, text=True)
    lines = [l.split("\t") for l in out.stdout.splitlines() if "\t" in l]
    for n, v in lines:
        check(n, v == "ok")
    check("every scenario ran", len(lines) == 5, out.stdout + out.stderr[-400:])
sys.exit(1 if failed else 0)
