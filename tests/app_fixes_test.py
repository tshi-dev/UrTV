#!/usr/bin/env python3
"""Small correctness fixes in the apps, as far as a plain JVM cannot run them (they need the Android framework): the source says
what it must. Run: python3 tests/app_fixes_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + detail))
    failed += not ok

text = lambda p: p.read_text(encoding="utf-8")
prefs = text(APP / "Prefs.java")
body = prefs[prefs.index("static List<Task> parseTasks"):prefs.index("private void setTasks")]
check("Prefs.tasks: one entry that is not a task is skipped, inside the loop", body.index("try {", body.index("for (int k")) < body.index("out.add(new Task(") and "catch (JSONException bad)" in body)
check("Prefs.tasks: parsed by a static method that takes the JSON", "parseTasks(sp.getString(\"tasks\"" in prefs)
updater = text(APP / "Updater.java")
check("Updater compares by BUILD (version.txt's \"3.<build>\" against the app's versionCode), never the name: 4.0 builds only grow by build", "Versions.buildOf(new Prefs(c).latestVersion())" in updater and "int mine = BuildConfig.VERSION_CODE;" in updater and "newest <= mine" in updater and "Versions.compare(latest, mine)" not in updater)
cache = text(APP / "PhotoCache.java")
check("PhotoCache: a photo that is served is touched, so the prune keeps the ones in use", "f.setLastModified(System.currentTimeMillis())" in cache
      and cache.index("setLastModified") < cache.index("return f;"))
saver = text(APP / "SaverReceiver.java")
check("a place of your own without its time zone is refused, a built-in city is not",
      "tz == null && !inList(name)" in saver and "add the time zone" in saver and "private static boolean inList" in saver)
sys.exit(1 if failed else 0)
