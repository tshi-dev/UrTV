#!/usr/bin/env python3
"""4.4 step 88 on the owner's Google TV (build 386): R2g won (64 of 64 Home presses, 3 of 3 boots, 30 minutes idle). Read from the source (the rules run
in HomeWatchRun, SetupEngineRun HF, SetupRunnerRun R35e): the watch's R2g rules; the home step never skipped for old failures (cleared by an update and
«تراجع»); TCL's AUTO_START and ASSOC_START written to disk; the result page says how Home works, in Arabic; «تراجع» takes the add-on out of the list.
    python3 tests/home_fix3_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


watch, eng, hook, act = text(SETUP.parent / "HomeWatch.java"), text(SETUP / "SetupEngine.java"), text(SETUP / "SetupHook.java"), text(SETUP / "SetupActivity.java")
svc = text(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java")
ev = svc[svc.index("public void onAccessibilityEvent(AccessibilityEvent e)"):svc.index("private static void watchLog(int what)")]
check("1. R2g: a press over another app always brought (even paused); over ours one count per 0.7 s burst, the 6th in 4 s pauses 30 s (build 413 T2)",
      "if (!fight) return true;" in watch and "BURST_MS = 700" in watch and "WINDOW_MS = 4_000, PAUSE_MS = 30_000" in watch and "recent >= MAX_BRINGS - 1" in watch
      and "boolean fight = before == null || before.equals(HOME_PKG);" in ev)
check("1. R2g: the app Home was pressed over, back within 1.5 s of our bring, gets ours once more (once); the app is kept in memory only, never logged",
      "COME_BACK_MS = 1_500" in watch and "HomeWatch.bringAgain(pkg, pressedOver, broughtAt, broughtAgain, t)" in ev and "broughtAgain = true;" in ev
      and 'Log.i("UrTVWatch", "the app came back after our bring brought again")' in svc)
check("1. not shipped from the tester's build: the forced key filter (R3 is a switch: on by default on a Google TV from build 413, tests/build413_test.py)",
      "want = true;" not in svc and "UrTVKey" not in svc and "if (e.getKeyCode() == KeyEvent.KEYCODE_HOME && homeKeyActive()) return homeKeyCaught(e);" in svc
      and 'getBoolean(ExtrasWords.HOME_KEY, false)' in svc)
check("2. the home step is never skipped for old failures; «تراجع» and an update clear the count",
      "HomeFails.givenUp" not in eng and "clearFile(HomeFails.FILE);" in eng[eng.index("Result undo()"):] and "clearOldFails(app);" in hook
      and "sa.urtv.home.BuildConfig.VERSION_CODE" in hook)
check("3. TCL: AUTO_START and ASSOC_START allowed for the add-on and written to disk at once (appops write-settings), in the app and in dock.sh",
      "ASSOC_START allow 2> /dev/null; appops write-settings" in eng and "sh.run(EXTRAS_APPOPS);" in eng
      and "appops set sa.urtv.extras ASSOC_START allow 2> /dev/null; appops write-settings" in text(ROOT / "optimize/dock.sh"))
check("4. the result page says, in Arabic, who holds Home and whether the add-on's watch runs (read only: the default home, Android's running services)",
      'S.t("دور الشاشة الرئيسية: تلفزيونك")' in act and 'S.t("مراقبة زر الرئيسية («إضافات تلفزيونك»): شغّالة، وهي اللي ترجّعك لتلفزيونك")' in act
      and "getEnabledAccessibilityServiceList" in act and "SetupHook.oursIsDefault(this)" in act)
uw = eng[eng.index("private void undoWatch()"):eng.index("private void undoWatch()") + 1200]
check("5. «تراجع» takes the add-on out of the list ALWAYS (also when nothing was recorded), the TV's other services kept, «wanted» cleared and told",
      "if (had == null || had.trim().isEmpty()) return;" not in uw and "WatchList.without(list, sa.urtv.home.ExtrasWords.SERVICE)" in uw
      and "clearFile(HOME_WANTED);" in eng and "SetupHook.tellWatchNow(app);" in text(SETUP / "SetupHost.java"))
sys.exit(1 if failed else 0)
