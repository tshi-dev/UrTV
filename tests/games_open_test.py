#!/usr/bin/env python3
"""4.3.4: the home's «الألعاب» (the control center's tile) opened the screensaver's SETTINGS, and no games. The home asks «تلفزيونك» (YourTvHubActivity)
for its "games" window, but launched it with NEW_TASK only: a screensaver task left open (its settings, its preview: startSaver uses the same app's task)
was brought back as it was, so the hub never saw the request. A window asked for starts a fresh task (NEW_TASK | CLEAR_TASK); the hub opens it.
Run: python3 tests/games_open_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

home = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
hub = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/YourTvHubActivity.java").read_text(encoding="utf-8")
body = home.split("private void openOurSettings(boolean orTvSettings, String window) {")[1].split("\n    }\n")[0]
win = body.split("Intent i = ")[0]

check("the control center's «الألعاب» asks for the games window", 'openGames() { MainActivity.this.openOurSettings(false, "games"); }' in home)
check("a window asked for: the hub gets it as the extra «open»", 'hub.putExtra("open", window)' in win, win)
check("a window asked for starts a fresh task (an open settings or preview never comes back instead)",
      "FLAG_ACTIVITY_NEW_TASK" in win and "FLAG_ACTIVITY_CLEAR_TASK" in win, win)
check("4.4 (beta-1: Home after the games, then «تلفزيونك» opened the games again): EVERY entry (the control center's tile, the pill, the Settings key) starts the hub at its own root; the flags are not behind «window != null»",
      "FLAG_ACTIVITY_CLEAR_TASK" in win and "if (window != null) hub.putExtra" in win and "if (window != null) {" not in win, win)
check("the hub reads «open» in onCreate and opens the games list",
      'getStringExtra("open")' in hub and '"games".equals(want)' in hub and "QuizGamesActivity.class" in hub)
check("the hub is the screensaver task's root (the launcher entry), so CLEAR_TASK leaves it alone on top",
      re.search(r'android:name="\.YourTvHubActivity"[^<]*?>\s*<intent-filter>\s*<action android:name="android.intent.action.MAIN"',
                (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")) is not None)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
