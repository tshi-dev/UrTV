#!/usr/bin/env python3
"""4.4 beta 4: «المصادر» lists each socket once. A user saw the same HDMI device twice: the TV reported one socket both as a passthrough input and
as an input of its own (4.4 beta 3 began to list those), or the port and the CEC device behind it. InputDisplay.distinct (pure Java) on made-up
lists, then the wiring: InputLabels.reported runs it, so the popup, «أسماء المصادر» and `dock.sh input-names` all see one row per socket.
Run: python3 tests/input_once_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

labels = (HOME / "InputLabels.java").read_text(encoding="utf-8")
check("InputLabels.reported keeps each socket once (once -> InputDisplay.distinct), after the sort", "inputs = once(c, inputs);" in labels
      and labels.index("Collections.sort(inputs") < labels.index("inputs = once(c, inputs);") and "InputDisplay.distinct(key, same, port, pass)" in labels)
check("the same name is the type and the TV's own name, the port is an input with no parent",
      'same[i] = t.getType() + ":" + own(c, t);' in labels and "t.getParentId() == null || t.getParentId().isEmpty()" in labels)

CASES = [
    ("two sockets stay two", "hdmi1|1007:HDMI 1|1|1;hdmi2|1007:HDMI 2|1|1", "0,1"),
    ("a port and its CEC device: the port once", "hdmi1|1007:HDMI 1|1|1;hdmi1|1007:PlayStation 5|0|1", "0"),
    ("the CEC device listed first: the port takes its place", "hdmi1|1007:Box|0|1;hdmi1|1007:HDMI 1|1|1;hdmi2|1007:HDMI 2|1|1", "1,2"),
    ("one socket reported twice under its name: the passthrough one", "vendor/HW5|1007:HDMI 1|1|0;pass/HW5|1007:HDMI 1|1|1;av|1001:AV|1|1", "1,2"),
    ("the same name in another case and spaces is the same socket", "a|1007:HDMI 1 |1|1;b|1007:hdmi 1|1|0", "0"),
    ("the same name of another type stays (AV is not HDMI)", "a|1007:Input 1|1|1;b|1001:Input 1|1|1", "0,1"),
    ("nothing reported: nothing", "", ""),
]
if not shutil.which("javac"):
    print("SKIP input once: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "InputDisplay.java"),
                        str(HOME / "InputNames.java"), str(ROOT / "tests/InputOnceRun.java")], capture_output=True, text=True)
    check("InputDisplay.distinct compiles", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        lists = [c[1] for c in CASES if c[1]]
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.InputOnceRun", *lists], capture_output=True, text=True).stdout.split("\n")
        i = 0
        for name, lst, want in CASES:
            got = out[i] if lst else ""
            i += bool(lst)
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
