#!/usr/bin/env python3
"""4.3.1 «شكل لوحة التحكم»: the control center is a wide page (default) or the tall panel; both fit 1080p/720p/4K, RTL and LTR, with the same tiles, order and focus rules.

    python3 tests/control_shape_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "ControlShape.java"), str(ROOT / "tests/ControlShapeRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.ControlShapeRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every shape rule ran", "ALL PASS" in out, out[-200:])
cc = (HOME / "ControlCenter.java").read_text(encoding="utf-8")
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
check("the panel is sized and its rows are moved by ControlShape, the wide page fades in where it stands and the tall one slides, and the choice is read when the panel opens (the next time after a change)",
      "ControlShape.size(W, H, wideNow)" in cc and "ControlShape.go(i, rowCells.size(), wideNow, Dir.rtl())" in cc and "if (wideNow) panel.setAlpha(0f);" in cc
      and 'wideNow = isWide(prefs().getString("layout", LAYOUT_WIDE));' in cc.split("final int[] sz", 1)[0][-200:])
check("«تخصيص» has the choice «شكل لوحة التحكم»: «عريض» / «طولي» (a button between the hint and the rows, in the focus walk, Back still goes to the panel), with English words",
      'S.t("شكل لوحة التحكم") + ": "' in cc and 'putString("layout"' in cc and "walk.add(shape);" in cc and all(w in tsv for w in ("شكل لوحة التحكم\t", "عريض\t", "طولي\t")))
check("the same tiles in both shapes: one row builder, the grid only changes where a cell is put", cc.count("new Cell(Cell.ROW, it)") == 1 and cc.count("for (Item it : shownRows)") == 1)
sys.exit(1 if failed else 0)
