#!/usr/bin/env python3
"""4.4 review F1: a settings-changed broadcast that arrives while the page is still being started (picture decode, first frame) is kept and looked at again
once the page exists, and a start that a newer one replaced creates no page. StartGate runs on a JVM (tests/StartGateRun.java); the two hosts are pinned."""
import subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "StartGate.java"), str(ROOT / "tests/StartGateRun.java")], capture_output=True, text=True)
    if r.returncode != 0: check("StartGate compiles", False, r.stderr[-400:])
    else:
        o = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.StartGateRun"], capture_output=True, text=True)
        print(o.stdout, end="")
        failed += o.stdout.count("FAIL ") + (1 if o.returncode not in (0, 1) else 0)

for name in ("ScreenDream.java", "MainActivity.java"):
    s = (APP / name).read_text(encoding="utf-8")
    recv = s.split("new BroadcastReceiver()", 1)[1].split("};", 1)[0]
    check(name + ": the receiver no longer drops a change while web is null: it asks the gate", "web == null) return" not in recv and "gate.changedWhileStarting()" in recv, recv)
    check(name + ": every start takes a token, only the newest may create the page, a gone window cancels", "gate.begin()" in s and "gate.current(token)" in s and "gate.cancel()" in s)
    check(name + ": createPage hands the page to the gate and looks again at a change that came in during the start", "gate.created(token)" in s and "gate.takeDirty()" in s)
    check(name + ": the rebuild checks the page exists, so a rebuild never runs over a pending start", "web == null) return;" in s)
sd = (APP / "ScreenDream.java").read_text(encoding="utf-8")
check("the dream's rebuild is still only when the address differs", "screenUrl(c, false).equals(shownUrl)) return;" in sd)
sys.exit(1 if failed else 0)
