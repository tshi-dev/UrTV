"""docs/TVS.md says what to do when a TV has no ADB in its own menus (R-379).

Reports: an agent kept retrying the scan on a Droidlogic/CVTE TV (ADB only in the factory menu), on an operator's box (Developer
options locked) and on a Mi Box with no network ADB at all. TVS.md now has the three notes, with the one safety rule for the
factory menu and the plain way out.

    python3 tests/tvs_doc_test.py
"""
import sys
from pathlib import Path
doc = (Path(__file__).resolve().parent.parent / "docs/TVS.md").read_text(encoding="utf-8")
bad = []
def check(name, ok):
    print(("PASS " if ok else "FAIL ") + name)
    if not ok:
        bad.append(name)
sec = doc.split("## When there is no ADB in the TV's own menus", 1)[1].split("\n## ", 1)[0] if "## When there is no ADB in the TV's own menus" in doc else ""
check("TVS.md has the section on TVs with no ADB in their own menus", bool(sec))
check("Droidlogic/CVTE: ADB in the factory menu, and nothing else changed there", "Droidlogic" in sec and "CVTE" in sec and "factory (service) menu" in sec
      and "change nothing else there" in sec)
check("the operator's box: the operator's limit, never unlocked or rooted", "operator's box" in sec and "Never try to unlock or root" in sec)
check("the Mi Box with no ADB route: the setup cannot run, said plainly instead of retrying", "Mi Box with no ADB route" in sec and "instead of retrying the scan" in sec)
sys.exit(1 if bad else 0)
