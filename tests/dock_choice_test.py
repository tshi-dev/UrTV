#!/usr/bin/env python3
"""4.3.2 (a report): `optimize/dock.sh set` with three apps showed five, the two extra ones games. The favourites row is the person's choice as it is: it is filled
from the other apps (by package name, as before) only when none of the chosen apps is on the TV (nothing saved, or all of it gone).   Run: python3 tests/dock_choice_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


main, dock = (HOME / "MainActivity.java").read_text(encoding="utf-8"), (HOME / "Dock.java").read_text(encoding="utf-8")
i = main.index("AppsEdit.topUp(have, other, MIN_DOCK)")
check("the home's row: the top-up runs only when no chosen app is there (dock.isEmpty())", "if (dock.isEmpty()) {" in main[i - 700:i], main[i - 300:i])
check("the favourites the phone and dock.sh read (Dock.shown): the same rule", "if (row.isEmpty()) while (row.size() < MIN && !rest.isEmpty()) row.add(rest.remove(0));" in dock)
check("with nothing chosen it is still filled by package name (never by label), as before", "java.util.Collections.sort(pool);" in (HOME / "AppsEdit.java").read_text(encoding="utf-8"))
check("dock.sh says so, and the test plan expects three to stay three",
      "3 stay 3" in (ROOT / "optimize/dock.sh").read_text(encoding="utf-8") and "stay three" in (ROOT / "docs/TEST-PLAN.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
