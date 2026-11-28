#!/usr/bin/env python3
"""4.4 GitHub #42 (R-473, TCL 65C7K on Google TV): the screensaver flashes and closes unless the app may draw over apps, and the grant was lost
at each reboot because the app did not declare SYSTEM_ALERT_WINDOW. Now it is declared, and the person is asked ONLY on a TV that needs it
(OverlayNeed on a JVM: tests/OverlayNeedRun.java), in plain words, once by itself and then from a row shown only while needed.
Run: python3 tests/overlay_need_test.py"""
import re
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
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "OverlayNeed.java"), str(ROOT / "tests/OverlayNeedRun.java")], capture_output=True, text=True)
        check("OverlayNeed compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            o = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.OverlayNeedRun"], capture_output=True)
            out = o.stdout.decode("utf-8")
            print(out, end="")
            check("every case of the decision", o.returncode == 0 and "BAD" not in out and out.count("ok ") >= 10, out[-300:])
else:
    print("SKIP the JVM part: no javac here")
man = re.sub(r"<!--.*?-->", "", read(ROOT / "android/app/src/main/AndroidManifest.xml"), flags=re.S)
check("the screensaver app declares SYSTEM_ALERT_WINDOW (a grant survives a reboot), once", man.count('<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />') == 1)
home = re.sub(r"<!--.*?-->", "", read(ROOT / "android/launcher/src/main/AndroidManifest.xml"), flags=re.S)
check("the home still has none (Play Protect, R-274)", "SYSTEM_ALERT_WINDOW" not in home)
prefs, dream, boot, st = read(APP / "Prefs.java"), read(APP / "ScreenDream.java"), read(APP / "BootReceiver.java"), read(APP / "SettingsActivity.java")
need = prefs.split("boolean overlayNeeded() {", 1)[1].split("\n    }\n", 1)[0]
check("Prefs reads the grant; while it is there it is noted (so a later loss is known) and the request may open again once",
      "Settings.canDrawOverlays(ctx)" in need and 'putBoolean("overlayHad", true).putBoolean("overlayAsked", false)' in need
      and 'OverlayNeed.needed(Build.VERSION.SDK_INT, googleTv(), false, sp.getBoolean("overlayHad", false), sp.getInt("overlayQuick", 0))' in need
      and '"com.google.android.apps.tv.launcherx"' in prefs)
check("the dream counts its quick ends: a key or our own end (the setup's prompt, the setup running, a lost renderer) never counts",
      "noteDreamEnd(android.os.SystemClock.elapsedRealtime() - shownAt, dreamKeyed, ownEnd)" in dream and dream.count("ownEnd = true;") == 3
      and "dreamKeyed = true;" in dream.split("public boolean dispatchKeyEvent(", 1)[1][:300])
check("every boot notes a grant that is there", "p.overlayNeeded();" in read(APP / "BootFirst.java") and "BootFirst.atBoot(app);" in boot)
check("the settings: asked once by itself only when needed; the row only while needed, in «شاشة التوقف»",
      "if (OverlayNeed.openByItself(prefs.overlayNeeded(), prefs.overlayAsked())) askSaverOverlay();" in st
      and "if (prefs.overlayNeeded())   // 4.4 #42" in st and 'row("إذن شاشة التوقف", OverlayNeed.WHY,' in st and 'ROW = "إذن شاشة التوقف";' in read(APP / "OverlayNeed.java"))
ask = st.split("private void askSaverOverlay() {", 1)[1].split("\n    }\n", 1)[0]
check("the request: the plain words, «اسمح» opens the TV's screen for this app, a TV that hides it says so; «مو الحين» does nothing",
      "prefs.setOverlayAsked();" in ask and "setTitle(OverlayNeed.TITLE).setMessage(OverlayNeed.WHY" in ask
      and '"android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())' in ask
      and "toast(OverlayNeed.HIDDEN);" in ask and '.setNegativeButton("مو الحين", null)' in ask)
sh = read(ROOT / "optimize/saver.sh")
check("the computer's way for a TV that hides the screen: saver.sh overlay allow, and it is documented",
      '"appops set sa.urtv.screensaver SYSTEM_ALERT_WINDOW allow"' in sh and "saver.sh overlay allow" in read(ROOT / "docs/SETTINGS.md"))
sys.exit(1 if failed else 0)
