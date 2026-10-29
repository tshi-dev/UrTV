#!/usr/bin/env python3
"""Nothing of the screensaver app keeps working while no dream or preview is up, and the home screen's returns stay cheap
(the owner's TV: «moving between apps is slower», sa.urtv.screensaver at 45% CPU with no screensaver showing). Source-reading.
Run: python3 tests/perf_idle_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = False


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  " + str(detail)))
    failed = failed or not ok


dream, main, view, snap = (APP / "ScreenDream.java").read_text(), (APP / "MainActivity.java").read_text(), (APP / "ScreenView.java").read_text(), (HOME / "ConfigSnapshot.java").read_text()
settings = (APP / "SettingsActivity.java").read_text()

check("the dream lets its WebView go and unregisters its receiver when it ends",
      re.search(r"onDetachedFromWindow\(\).*?unregisterReceiver\(changed\).*?ScreenView\.release\(web\)", dream, re.S) is not None)
check("the preview does the same in onDestroy", re.search(r"onDestroy\(\).*?unregisterReceiver\(changed\).*?ScreenView\.release\(web\)", main, re.S) is not None)
check("release stops loading, takes the view out and destroys it", all(x in view for x in ("w.stopLoading()", "removeView(w)", "w.destroy()")))
check("the screensaver app has no timer, alarm or handler loop of its own outside the settings' picker",
      not re.search(r"java\.util\\.Timer|AlarmManager|scheduleAtFixedRate|ScheduledExecutor", "".join(p.read_text() for p in APP.glob("*.java"))))
check("a picker's live preview (a running WebView) is let go when the settings screen stops and drawn again when it starts",
      re.search(r"onStop\(\)\s*\{[^}]*previewOff\.run\(\)", settings) is not None and re.search(r"onStart\(\)[^}]*previewOn\.run\(\)", settings) is not None
      and "previewOff = previewOn = null" in settings)
check("the home screen's reads of the screensaver's settings within a few seconds share one answer (a return reads it four times), and a missing screensaver is asked again",
      "SHARE_MS" in snap and "LAST.read(android.os.SystemClock.elapsedRealtime(), SHARE_MS," in snap and "s.fragment != null" in snap
      and "now - lastAt < shareMs" in (HOME / "LastGood.java").read_text())
sys.exit(1 if failed else 0)
