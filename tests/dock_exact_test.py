#!/usr/bin/env python3
"""4.4 (reports, cause 11): `optimize/dock.sh set` with 3 apps showed 5, topped up with games. The receiver saves the row as exact (Dock.saveExact),
and both places that build the row (Dock.shown, MainActivity.showApps) top it up only to AppsEdit.floor: 0 for an exact row that has an app here,
Dock.MIN otherwise. The home's own editors (Dock.save) keep the floor of five, as before.
Run: python3 tests/dock_exact_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
dock, rec, main = read(HOME / "Dock.java"), read(HOME / "DockReceiver.java"), read(HOME / "MainActivity.java")
check("dock.sh set saves the row exact", "Dock.saveExact(c, row);" in rec.split('case "sa.urtv.home.SET_DOCK":')[1].split("return;\n            }")[0])
check("Dock.save (the home's editors) clears it, reset removes it", '.putBoolean("dock_exact", false)' in dock and '.remove("dock_exact")' in dock and '.putBoolean("dock_exact", true)' in dock)
check("Dock.shown tops up only to the floor", "int floor = AppsEdit.floor(exact(c), row.size(), MIN);" in dock and "while (row.size() < floor && !rest.isEmpty())" in dock)
check("the home screen's row too", "AppsEdit.topUp(have, other, AppsEdit.floor(Dock.exact(this), have.size(), MIN_DOCK))" in main)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "AppsEdit.java"), str(HOME / "FavEdit.java"),
                        str(HOME / "ParentalPolicy.java"), str(HOME / "AdhanKeys.java"), str(ROOT / "tests/DockExactRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.DockExactRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case ran", "ALL PASS" in out, out[-300:])
sys.exit(1 if failed else 0)
