#!/usr/bin/env python3
"""4.3.3 «بحث» on the home: the TV's own search inside the apps (Android TV's global search), off by default, a row in the settings («زر البحث»,
`saver.sh searchpill on`, #searchpill=1). GlobalSearch.wanted runs on a plain JVM (with a stub of the two Android classes); the wiring is read
from the source.   Run: python3 tests/global_search_test.py"""
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
src = lambda p: p.read_text(encoding="utf-8")
gs, main, prefs, recv, st, hub, saver, man = (src(HOME / "GlobalSearch.java"), src(HOME / "MainActivity.java"), src(APP / "Prefs.java"), src(APP / "SaverReceiver.java"),
    src(APP / "SettingsActivity.java"), src(APP / "SettingsHub.java"), src(ROOT / "optimize/saver.sh"), src(ROOT / "android/launcher/src/main/AndroidManifest.xml"))

check("off by default: the setting is false until chosen, and #searchpill=1 only while on", 'sp.getBoolean("searchPill", false)' in prefs and '(searchPill() ? "&searchpill=1" : "")' in prefs)
check("a row in the settings (the home's category), saver.sh searchpill on|off, and saver.sh show", 'row("زر البحث", prefs.searchPill() ? "ظاهر" : "مخفي"' in st and '{"زر البحث", BUTTONS}' in hub
      and "set1 searchpill" in saver and 'p.setSearchPill("on".equals(searchPill));' in recv and '"\\nsearchpill: "' in recv)
check("the TV's own search: GLOBAL_SEARCH, then ASSIST, only one that resolves; both in the manifest's <queries>",
      '"android.search.action.GLOBAL_SEARCH", Intent.ACTION_ASSIST' in gs and "resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) != null" in gs
      and 'android.search.action.GLOBAL_SEARCH' in man and 'android.intent.action.ASSIST' in man)
check("the button follows the setting on every minute's settings read, before «الجوال», only where the TV has the search",
      "applyQuickPill(searchPill, GlobalSearch.wanted(cfg.fragment, Parental.state(this).kids, cleanShown) && GlobalSearch.intent(getPackageManager()) != null, phonePill" in main)
check("OK opens it behind the lock of the app that answers, never in kids mode", 'launchGuarded(ParentalPolicy.TILE, s, "بحث")' in main and "!Parental.state(this).kids) launchGuarded" in main)
check("tests/run.sh runs this test", "tests/global_search_test.py" in src(ROOT / "tests/run.sh"))

if shutil.which("javac") and shutil.which("java"):
    tmp = Path(tempfile.mkdtemp())
    (tmp / "android/content/pm").mkdir(parents=True)
    (tmp / "android/content/Intent.java").write_text("package android.content; public class Intent { public static final String ACTION_ASSIST = \"android.intent.action.ASSIST\"; public static final int FLAG_ACTIVITY_NEW_TASK = 1; public Intent(String a) {} public Intent addFlags(int f) { return this; } }")
    (tmp / "android/content/pm/PackageManager.java").write_text("package android.content.pm; public class PackageManager { public static final int MATCH_DEFAULT_ONLY = 1; public Object resolveActivity(android.content.Intent i, int f) { return null; } }")
    (tmp / "Run.java").write_text('package sa.urtv.home; public final class Run { public static void main(String[] a) { System.out.println('
        'GlobalSearch.wanted("city=x&searchpill=1", false, false) + " " + GlobalSearch.wanted("searchpill=1", true, false) + " " + GlobalSearch.wanted("searchpill=1", false, true) + " " '
        '+ GlobalSearch.wanted("city=x", false, false) + " " + GlobalSearch.wanted("searchpill=10", false, false) + " " + GlobalSearch.wanted(null, false, false)); } }')
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(tmp / "out"), str(tmp / "android/content/Intent.java"), str(tmp / "android/content/pm/PackageManager.java"), str(HOME / "GlobalSearch.java"), str(tmp / "Run.java")], capture_output=True, text=True)
    check("GlobalSearch compiles", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        got = subprocess.run(["java", "-cp", str(tmp / "out"), "sa.urtv.home.Run"], capture_output=True, text=True).stdout.strip()
        check("wanted: on with the setting; never in kids mode or on the clean home; off without it (searchpill=10 is not it)", got == "true false false false false false", got)
else:
    print("SKIP the Java cases: no javac")
print(f"{failed} failed" if failed else "global search: all passed")
sys.exit(1 if failed else 0)
