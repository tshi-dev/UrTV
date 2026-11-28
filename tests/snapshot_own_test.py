#!/usr/bin/env python3
"""4.4 beta 4: the picture the home shows first at a cold start (HomeSnapshot) was, on some TVs, another app's screen: the window's copy
(PixelCopy, which keeps the WebView cards in it) was taken as the home left for an app, and those TVs' copy already held the app. Now each copy is
held against the home's own views drawn small (View.draw never sees another window): SnapshotOwn.same on a 32 x 18 grid; a copy that is not the
home's keeps the old picture. SnapshotOwn (no Android) on made-up grids, then the wiring.
Run: python3 tests/snapshot_own_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

snap = (H / "HomeSnapshot.java").read_text(encoding="utf-8")
cap = snap[snap.index("static void capture(Activity a, View root, Runnable done)"):]
cap = cap[:cap.index("\n    }\n")]
check("capture draws the home's own views first (before the copy is asked for)", "final int[] own = ownGrid(root);" in cap and cap.index("ownGrid(root)") < cap.index("PixelCopy.request("))
check("a copy that is not the home's is not kept (the old picture stays) and that is logged",
      "if (r == PixelCopy.SUCCESS && own != null && !SnapshotOwn.same(grid(full), own)) {" in cap and "the copy is not the home's own screen" in cap
      and cap.index("SnapshotOwn.same") < cap.index("save(app, full, key)"))
own = snap[snap.index("static int[] ownGrid(View root)"):]
own = own[:own.index("\n    }\n")]
check("ownGrid: View.draw of the root into a small plain bitmap; one that cannot draw gives null (the copy kept, as before)",
      "root.draw(c);" in own and "Bitmap.Config.ARGB_8888" in own and "return null;" in own and "b.recycle()" in own)
check("the copy is still PixelCopy of the window (the WebView cards in it)", "PixelCopy.request(a.getWindow(), null, full," in cap)

if not shutil.which("javac"):
    print("SKIP snapshot own: no javac here")
    sys.exit(1 if failed else 0)
NAMES = ["the home's own copy: kept", "the home's copy with a page card the drawing leaves empty: kept", "the home's copy a little brighter: kept",
         "another app's white page: not kept", "another app's video: not kept", "nothing drawn: not kept"]
WANT = ["1", "1", "1", "0", "0", "0"]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(H / "SnapshotOwn.java"), str(ROOT / "tests/SnapshotOwnRun.java")], capture_output=True, text=True)
    check("SnapshotOwn compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SnapshotOwnRun"], capture_output=True, text=True).stdout.split()
        for name, want, got in zip(NAMES, WANT, out + [""] * 6):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
