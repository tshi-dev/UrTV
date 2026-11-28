#!/usr/bin/env python3
"""Known bug (tester steps 88, 92; last seen 427): the setup said the Home step failed while Home worked: it was judged before Android had bound the add-on. Build 413 waited up to 10 s;
on a slow TV that was still short. The wait is 30 s, read once a second, and ends the moment Android says the service is bound; the Home key is tried only after it."""
import re, sys
from pathlib import Path
s = (Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java").read_text(encoding="utf-8")
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
ms = int(re.search(r"static final int BOUND_WAIT_MS = ([\d_]+);", s).group(1).replace("_", ""))
check("the wait for the add-on to be bound is at least 20 s", ms >= 20000, ms)
bw = s[s.index("boolean boundWithin(int ms)"):]
bw = bw[:bw.index("\n    }\n")]
check("it asks Android once a second and stops the second the service is bound (a fast TV waits no longer)", "Bound services:" in bw and "sleeper.sleep(1000)" in bw and "return true;" in bw and "waited >= ms" in bw)
check("finishWatch tries the Home key only after the wait", "if (watchOk && boundWithin(BOUND_WAIT_MS) && homeKeyOpensOurs())" in s)
sys.exit(1 if failed else 0)
