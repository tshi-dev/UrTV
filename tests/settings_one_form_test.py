#!/usr/bin/env python3
"""4.5 (the owner approved docs/ui/PROPOSAL.md, «with a protective way»): the settings have one form, couch-tv's compact rows, for every look, and
the 4.3.2 hub's tiles go. Nothing the person had set may be lost: every row and every tile of the hub is listed here with its place in the one form
(SettingsHub run on a JVM by tests/OneFormRun.java), and the activity never builds the hub while «إعدادات بشكل واحد» (prefs.uiOneForm) is on, nor opens again for a look.
Run: python3 tests/settings_one_form_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
prefs = (APP / "Prefs.java").read_text(encoding="utf-8")
check("4.4 beta 2: «إعدادات بشكل واحد» is a switch, off by default (and after an update: a key never stored before), and the hub is built only with it off",
      'boolean uiOneForm() { return sp.getBoolean("uiOneForm", false); }' in prefs and "classic = !prefs.uiOneForm() && !prefs.couchOn();" in st
      and 'row("إعدادات بشكل واحد", prefs.uiOneForm() ?' in st)
check("a look, a mix or «شكل كنبة» never opens the settings again (no restart, no flash): drawn again in place",
      st.count("if (prefs.uiOneForm() || wasClassic == !prefs.couchOn()) { render(); return; }") == 2
      and re.search(r"private void flipCouch\(\) \{\s*prefs\.setCouch\(!prefs\.couchOn\(\)\);\s*if \(prefs\.uiOneForm\(\)\) \{ render\(\); return; \}", st) is not None)
if not shutil.which("javac"):
    print("SKIP the places: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "SettingsHub.java"), str(APP / "FocusKey.java"), str(ROOT / "tests/OneFormRun.java")],
                   check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.OneFormRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t")[1:] for l in out.splitlines() if l.startswith("row\t")]
tiles = [l.split("\t")[1:] for l in out.splitlines() if l.startswith("tile\t")]
lost = [r[0] for r in rows if r[2] == "null"]
check(f"every one of the hub's {len(rows)} rows is a row of the one form (none lost)", len(rows) >= 140 and not lost, lost)
check("every row has a section in the one form with a name", all(r[3] and r[3] != "null" for r in rows if r[2] != "null"), [r for r in rows if not r[3]])
badt = [t for t in tiles if t[2] == "null" or not t[3] or t[3] == "null"]
check(f"every one of the hub's {len(tiles)} tiles lands in a section of the one form (a stored key or a link to it keeps working)", len(tiles) >= 20 and not badt, badt)
names = {r[0] for r in rows}
check("a tile that ran one row at once (المدينة, الأذان, مركز التحكم…) finds that row in the one form", all(t[4] in names for t in tiles if t[4]), [t for t in tiles if t[4] and t[4] not in names])
# the list, for the owner and the next agent: each place of the hub and where it is now
print("\n".join(f"  tile «{t[1]}» ({t[0]}) → «{t[3]}»" for t in tiles))
print(f"  ({len(rows)} rows: each in the section named beside it in tests/OneFormRun.java's output)")
sys.exit(1 if failed else 0)
