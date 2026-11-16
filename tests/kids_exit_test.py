#!/usr/bin/env python3
"""4.3.3: kids mode always has a way out on the TV, and «الرقابة الأبوية» can be reached: the long OK's box on any app offers it (in kids mode
a lock per app means nothing), the Menu key and the star key open it where kids mode has no pills, and a long Settings key opens it where
the screensaver app (whose «المزيد» holds the other door) is missing. Every way goes through the PIN (ParentalPolicy.PARENTAL_SCREEN, and
the screen asks it itself). Read from the source: it needs Android.   Run: python3 tests/kids_exit_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main, opts, policy = ((HOME / f).read_text(encoding="utf-8") for f in ("MainActivity.java", "AppOptions.java", "ParentalPolicy.java"))
op = main.split("private void openParental() {")[1].split("\n    }\n")[0]
check("openParental opens «الرقابة الأبوية» behind its own lock (PARENTAL_SCREEN: always the PIN once one is set)",
      "launchGuarded(ParentalPolicy.PARENTAL_SCREEN, new Intent(this, ParentalActivity.class)" in op and "case PARENTAL_SCREEN:\n                t.add(PARENTAL);" in policy
      and "if (PARENTAL.equals(t) || s.locked.contains(t)) return true;" in policy)
menu = main.split("private void showAppMenuNow(")[1].split("\n    }\n")[0]
check("the long OK's box in kids mode: its lock slot reads «الرقابة الأبوية» and opens it",
      "if (kids) {" in menu and "o.lockLabel = KIDS_EXIT;" in menu and "o.lock = this::openParental;" in menu and 'KIDS_EXIT = S.n("الرقابة الأبوية")' in main
      and menu.index("o.lock = () -> {") < menu.index("o.lock = this::openParental;"))
check("AppOptions shows the label it is given for that slot", "s.lockLabel != null ? s.lockLabel : s.locked ? UNLOCK : LOCK" in opts)
check("the Menu key in kids mode (no pills, maybe no app): «الرقابة الأبوية»",
      "KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && Parental.state(this).kids) {\n            openParental();" in main)
keys = main.split("private void openRemoteKey(")[1].split("\n    }\n")[0]
check("the star key in kids mode: «الرقابة الأبوية»", "else if (Parental.state(this).kids) openParental();" in keys)
check("a long Settings key in kids mode with no screensaver app: «الرقابة الأبوية» (with it, its settings hold the row as before)",
      "if (Parental.state(this).kids && !isInstalled(SAVER_PKG)) openParental();" in keys and "\n                else openOurSettings(true);" in keys)
check("kids mode is left only with the PIN on that screen", "Parental.confirm(this, adhan, S.t(\"الخروج من وضع الأطفال\")" in (HOME / "ParentalActivity.java").read_text(encoding="utf-8"))
check("tests/run.sh runs this test", "tests/kids_exit_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
print(f"{failed} failed" if failed else "kids exit: all passed")
sys.exit(1 if failed else 0)
