#!/usr/bin/env python3
"""Left/right on the remote never goes the wrong way on our screens. Reported on a Sony and a TCL: «if I hold right it goes left».
What the code shows (checked here from the source, since the focus walk needs Android):
 1. the surah grid: at its two ends the first press used to fall to Android's own search, which counts cells the other way
    (CellWalk, tests/cell_walk_cases.tsv);
 2. the screens built in code (no layoutDirection) took the TV's language as their direction, but their nextFocus ids assume one: the
    rows of «ترتيب المفضلة», «المهام» and «الرقابة الأبوية» assume left-to-right, the phone screen right-to-left, so on a TV set to the
    other language their ◀▶ went the opposite way. Each now pins its direction.
Not changed, and why: the home screen forces right-to-left and walks its rows by hand (MainActivity.moveFocusSideways); its explicit
nextFocus ids are written in row order, but no focus can reach them (every left/right on a row is consumed by the walk; the cards that are
not in a row point at themselves).
 3. «اللغة» English (docs/ENGLISH.md): the D-pad follows Dir. Arabic keeps every constant above, byte for byte; English is the mirror: the
    home and the phone screen left to right, the screens pinned left-to-right for Arabic pinned the other way, every hand-written ◀▶ id
    swapped and every hand walk turned round.   Run: python3 tests/dpad_rtl_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

# Arabic: LTR for the first four (Dir.layout(false)), RTL for the phone screen (Dir.layout(true)); English the other way
for f, d, call in (("DockActivity", "LTR", "Dir.layout(!Dir.rtl())"), ("TasksActivity", "LTR", "Dir.layout(!Dir.rtl())"),
                   ("ParentalActivity", "LTR", "Dir.layout(!Dir.rtl())"), ("InputNamesActivity", "LTR", "Dir.layout(!Dir.rtl())"),
                   ("PhoneActivity", "RTL", "Dir.layout()")):
    s = (HOME / f"{f}.java").read_text(encoding="utf-8")
    i = s.find(f"scroll.setLayoutDirection({call});")
    check(f"{f}: its direction is pinned before setContentView ({d} in Arabic, mirrored in English)", i >= 0 and i < s.index("setContentView(scroll)"))
    check(f"{f}: no side or direction written as a constant any more", not re.search(r"Gravity\.(RIGHT|LEFT)\b|LAYOUT_DIRECTION_(RTL|LTR)|\b(left|right)Margin\s*=", s))
surah = (HOME / "tilawah/SurahActivity.java").read_text(encoding="utf-8")
check("SurahActivity: a first press at an end of the grid is consumed, not handed to Android's search", "CellWalk.consume(false, e.getRepeatCount(), onCell())" in surah)
check("SurahActivity: the walk uses CellWalk for the next cell, turned round in English (the grid reads from the left)", "CellWalk.next(position, grid.getCount(), right == sa.urtv.home.Dir.rtl())" in surah)
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("the home screen still forces right-to-left (Dir: English left-to-right) and walks its rows by hand", "root.setLayoutDirection(Dir.layout())" in main and "moveFocusSideways(" in main)
check("…and the walk turns round with the direction: in Arabic «right» is the item before, in English the item after", "int next = right == Dir.rtl() ? index - 1 : index + 1;" in main)
# every hand-written ◀▶ pair keeps its Arabic ids and swaps them in English
pairs = {
    "MainActivity.java": ["b.setNextFocusRightId(Dir.rtl() ? before : after);", "b.setNextFocusLeftId(Dir.rtl() ? after : before);"],
    "ControlCenter.java": ["t.setNextFocusRightId(Dir.rtl() ? before : after);", "t.setNextFocusLeftId(Dir.rtl() ? after : before);"],
    "ParentalActivity.java": ["cs[j].setNextFocusLeftId(Dir.rtl() ? before : after);", "cs[j].setNextFocusRightId(Dir.rtl() ? after : before);"],
    "DockActivity.java": ["if (Dir.rtl()) c.setNextFocusLeftId(remove.getId()); else c.setNextFocusRightId(remove.getId());",
                          "if (Dir.rtl()) remove.setNextFocusRightId(c.getId()); else remove.setNextFocusLeftId(c.getId());"],
    "TasksActivity.java": ["if (Dir.rtl()) card.setNextFocusLeftId(del.getId()); else card.setNextFocusRightId(del.getId());",
                           "if (Dir.rtl()) del.setNextFocusRightId(card.getId()); else del.setNextFocusLeftId(card.getId());",
                           "if (Dir.rtl()) addBtn.setNextFocusLeftId(mic.getId()); else addBtn.setNextFocusRightId(mic.getId());",
                           "if (Dir.rtl()) mic.setNextFocusRightId(addBtn.getId()); else mic.setNextFocusLeftId(addBtn.getId());"],
    "PhoneActivity.java": ["if (Dir.rtl()) deny.setNextFocusRightId(allow.getId()); else deny.setNextFocusLeftId(allow.getId());",
                           "if (Dir.rtl()) allow.setNextFocusLeftId(deny.getId()); else allow.setNextFocusRightId(deny.getId());"],
}
for f, want in pairs.items():
    src_f = (HOME / f).read_text(encoding="utf-8")
    check(f"{f}: its ◀▶ ids are the Arabic ones with Dir.rtl(), swapped in English", all(w in src_f for w in want), [w for w in want if w not in src_f])
listen = (HOME / "tilawah/MainActivity.java").read_text(encoding="utf-8")
check("«تلاوة وأذان»: the tabs and the adhan's volume follow Dir (◀ is forward in Arabic, ▶ in English)",
      "changeVolume(keyCode == sa.urtv.home.Dir.forwardKey() ? 10 : -10);" in listen and "int delta = keyCode == sa.urtv.home.Dir.backKey() ? -1 : 1;" in listen)
if not shutil.which("javac"):
    print("SKIP cell walk: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/cell_walk_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "tilawah/CellWalk.java"), str(ROOT / "tests/CellWalkRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.CellWalkRun", str(ROOT / "tests/cell_walk_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    want, note = (c[4], c[5]) if c[0] == "next" else (c[4], c[5])
    check(f"cell walk {c[0]} {c[1:4]}: {note}", g == want, f"{g!r}, wanted {want!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
