#!/usr/bin/env python3
"""4.3.4: «each setting needs two presses before it runs». The settings build their whole list again when a late answer comes (the home's
«UrTV النظيف» question on each return, and again 1.5 s later; «النسخ التجريبية»'s check). A row built again between OK's press and its release
is a new view that never saw the press, so the first press did nothing (slow answers on a TCL make it likely). OkHold: while OK is held a build
waits, the pressed row takes its click at the release, then the list is built; never longer than 1 s. The rule runs on a JVM; the screen is
checked to use it.
Run: python3 tests/ok_hold_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

s = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
render = s.split("    private void render() {")[1].split("\n    }\n")[0]
disp = s.split("@Override public boolean dispatchKeyEvent(android.view.KeyEvent e) {")[1].split("\n    }\n")[0]
check("render() waits while OK is held (its first line)", render.lstrip().startswith("if (okHold.defer(android.os.SystemClock.uptimeMillis())) return;"))
check("every key goes through OkHold before the screen handles it", "okHold.key(e.getKeyCode(), e.getAction(), android.os.SystemClock.uptimeMillis())" in disp)
due = disp.split("okHold.key(")[1]
check("at the release: the click first (the row that saw the press), then the waiting build",
      "super.dispatchKeyEvent(e)" in due and "render()" in due and due.index("super.dispatchKeyEvent(e)") < due.index("render()"))
check("…never on a closing screen", "if (!isFinishing() && !isDestroyed()) render();" in due)
check("the late answers that build the list are still there (what this guards against)",
      "if (changed) render();" in s and "homeCleanSeen(); } }, 1500);" in s)

if not shutil.which("javac"):
    print("SKIP the rule on a JVM: no javac")
    sys.exit(1 if failed else 0)
C, E, UPK, DOWN, UP = 23, 66, 19, 0, 1
runs = [
    (["r 0"], ["go"], "nothing held: a build runs at once"),
    ([f"k {C} {DOWN} 100", "r 300", f"k {C} {UP} 400"], ["-", "wait", "due"], "OK held (DPAD_CENTER): the build waits and is due at the release"),
    ([f"k {E} {DOWN} 100", "r 200", f"k {E} {UP} 250"], ["-", "wait", "due"], "ENTER is OK too"),
    ([f"k {C} {DOWN} 100", f"k {C} {UP} 200"], ["-", "-"], "a press with no build waiting: nothing due"),
    ([f"k {C} {DOWN} 100", "r 1200", f"k {C} {UP} 1300"], ["-", "go", "-"], "held past 1 s (the release went elsewhere): the build runs, nothing left due"),
    ([f"k {UPK} {DOWN} 100", "r 150"], ["-", "go"], "an arrow held: never waits"),
    ([f"k {C} {DOWN} 100", f"k {C} {DOWN} 600", "r 1150"], ["-", "-", "go"], "OK's repeats keep the first press's time: still at most 1 s"),
    ([f"k {C} {DOWN} 100", "r 200", f"k {C} {UP} 300", "r 310"], ["-", "wait", "due", "go"], "after the release the next build runs at once"),
]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-d", tmp, str(APP / "OkHold.java"), str(ROOT / "tests/OkHoldRun.java")], check=True, capture_output=True)
    for steps, want, name in runs:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.OkHoldRun"] + steps, check=True, capture_output=True, text=True).stdout.split()
        check(name, got == want, f"{steps} -> {got}, want {want}")
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
