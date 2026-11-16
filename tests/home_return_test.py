#!/usr/bin/env python3
"""G4 (4.3.3): less work before the home's first frame. A cold start no longer looks for the TV's live TV app and source picker (a dozen package manager calls;
the inputs list and «التلفزيون» ask each time they open); a return no longer waits on the main thread for the screensaver's provider («أكمل المشاهدة» reads its
setting on the row's own thread), for the package manager and the update record (showUpdateResult reads them on a thread and opens the screen after), or for the
changed-packages check (after the first frame: AfterFrame).

    python3 tests/home_return_test.py
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return Path(p).read_text(encoding="utf-8")

main = text(HOME / "MainActivity.java")
def body(sig, end):
    return main.split(sig, 1)[1].split(end, 1)[0]
create = body("protected void onCreate(Bundle b) {", "\n    }\n")
resume = body("protected void onResume() {", "\n    }\n")
check("onCreate does not call findTvApps()", "findTvApps();" not in create)
calls = [m.start() for m in re.finditer(r"findTvApps\(\);", main)]
check("findTvApps is asked where its answers are used: the inputs list and «التلفزيون» (openLiveTv)", len(calls) == 2
      and "findTvApps();" in body("private void showInputsNow(View anchor) {", "\n    }\n") and "findTvApps();" in body("private void openLiveTv() {", "\n    }\n"), len(calls))
check("every use of liveTvPkg / inputPickerPkg is in the inputs list, its rows (openPassthroughInput) or openLiveTv, each after a findTvApps",
      all(any(n in main[:m.start()].rsplit("private void ", 1)[1][:40] for n in ("showInputsNow", "openPassthroughInput", "openLiveTv", "findTvApps")) for m in re.finditer(r"\b(liveTvPkg|inputPickerPkg)\b(?! *=)", main) if m.start() > main.index("private String liveTvPkg")+40))
load = body("private void loadTvRows() {", "\n    }\n")
check("«أكمل المشاهدة»: the setting (a provider query) is read inside the row's own thread, never before it",
      load.index("tvLoader.execute(") < load.index("ConfigSnapshot.read(") and "ConfigSnapshot.read(this)" not in load)
up = body("private void showUpdateResult() {", "\n    }\n")
check("showUpdateResult: the package manager and the record are read on a thread, the screen opens on the main thread, only while the home is in front",
      up.index("new Thread(") < up.index("PhoneUpdater.installedCode(") and up.index("new Thread(") < up.index("UpdateRecords.peek(") and "runOnUiThread(" in up
      and up.index("runOnUiThread(") < up.index("launchGuarded(") and "if (!resumed || isDestroyed()" in up)
check("onResume: the changed-packages check runs after the first frame (AfterFrame), not before it",
      "AfterFrame.run(getWindow().getDecorView(), () -> { if (resumed && !isDestroyed() && getPackageManager().getChangedPackages(packageSeq) != null) buildApps(); });" in resume
      and "if (getPackageManager().getChangedPackages(packageSeq) != null) buildApps();" not in resume)
af = text(HOME / "AfterFrame.java")
check("AfterFrame: once, after the next pre-draw (posted, so after the draw; the frame is not held), or after LATE_MS when no frame comes",
      "v.post(once);\n                return true;" in af and "postDelayed(" in af and "LATE_MS = 1000" in af and "if (done[0]) return; done[0] = true;" in af and "removeOnPreDrawListener" in af)
sys.exit(1 if failed else 0)
