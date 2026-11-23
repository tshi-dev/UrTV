#!/usr/bin/env python3
"""4.4 beta 3 (GitHub #67, a TCL TV on Android 11, build 517: Settings → «مركز التحكم» → OK crashed the home, «Unable to add window -- token null
is not valid» at PopupWindow.showAtLocation in ControlCenter.open, two crashes then the safe screen). The panel opens only over a live window:
never over a finishing or gone activity; with no window token, once more when the anchor's window is attached (one retry, never a loop); a
refusal by the window manager between the check and the show is caught. Read from the source (it needs Android).
    python3 tests/control_center_token_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
cc = (ROOT / "android/launcher/src/main/java/sa/urtv/home/ControlCenter.java").read_text(encoding="utf-8")
failed = 0


def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok


op = cc[cc.index("    void open(View anchor, String start) {"):]
head = op[:op.index("GlassSource.look(act)")]
check("never over a finishing or destroyed activity (nor with no anchor)", "if (act.isFinishing() || act.isDestroyed() || anchor == null) return;" in head)
check("no window token yet: it waits for the anchor's window to be attached, then opens once more (the guard is before anything is built)",
      "if (anchor.getWindowToken() == null || !anchor.isAttachedToWindow()) {" in head and "addOnAttachStateChangeListener" in head
      and "v.removeOnAttachStateChangeListener(this);" in head and "open(v, at)" in head)
check("one retry at a time, never a loop: a second open while one waits is dropped, and the retry checks the token and the activity again",
      "if (retryPending) return;" in head and "retryPending = true;" in head and head.count("retryPending = false;") == 2
      and head.count("getWindowToken() != null && !act.isFinishing() && !act.isDestroyed()") == 2)
show = op[op.index("pw.showAtLocation(anchor.getRootView()") - 200:op.index("pw.showAtLocation(anchor.getRootView()") + 400]
check("the show itself cannot crash the home: BadTokenException (and an IllegalStateException) caught, the panel dropped",
      "try {" in show and "catch (android.view.WindowManager.BadTokenException | IllegalStateException e)" in show and "pw = null;" in show)
check("every showAtLocation of the panel is inside that guard", cc.count(".showAtLocation(") == 1)
sys.exit(1 if failed else 0)
