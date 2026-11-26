#!/usr/bin/env python3
"""4.4 beta 3 (the owner's TV, 21 Nov, beta 577: «the remote reached the TV, OK opened an app, nothing moved on the screen»; the tester's
«كل التطبيقات» the same, no ANR): a picture left over the working home. Each cover the home puts over itself ends by a deadline that needs no
frame (a fade's end needs frames, and a TV with its screen off or a stalled start draws none): the look change's cover (HomeLook.HARD_MS),
the start picture (its fade plus a direct removal), the card's page (its own timers). And the watchdog: at a key, a cover past its end, or
nothing drawn for FROZEN_MS after the key, drops every cover, draws again and logs «frozen-screen recovered».   python3 tests/frozen_screen_test.py"""
import sys
from pathlib import Path

H = Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok):
    global failed
    print(("PASS " if ok else "FAIL ") + name)
    failed += not ok
look, main, card = (H / "HomeLook.java").read_text(encoding="utf-8"), (H / "MainActivity.java").read_text(encoding="utf-8"), (H / "HomeCard.java").read_text(encoding="utf-8")

check("the look change's cover ends by HARD_MS whatever happens to its fade (a plain timer, no frame needed)",
      "static final long HARD_MS = 4000;" in look and "ui.postDelayed(() -> { if (cover == mine) done(mine); }, HARD_MS);" in look)
check("…and can be dropped at once with no fade (drop: the animation cancelled, removed)", "boolean drop() {" in look and "cover.animate().cancel();" in look)
hide = main[main.index("    private void hideSnapshot() {"):main.index("    // ---- 4.4 beta 3, the frozen screen")]
check("the start picture: its fade, and a direct removal a little after it even when the fade never ends",
      "withEndAction(" in hide and "HomeSnapshot.FADE_MS + 400);" in hide and "removeView(v); }" in hide)
check("the card's page hidden until its first frame comes back by its own timer too (B7: 700 ms; a new page: HomeLook.WAIT_MS)",
      "w.postDelayed(new Runnable() { @Override public void run() { w.setAlpha(1f); dropStill(); } }, 700);" in card and "}, HomeLook.WAIT_MS);" in card)   # beta 3: the timer also drops the still (the card is let go, R-313)
w = main[main.index("    // ---- 4.4 beta 3, the frozen screen"):]
w = w[:w.index("    public boolean dispatchKeyEvent") if "    public boolean dispatchKeyEvent" in w else len(w)]
check("every key down (not a repeat) runs the watchdog first", "if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) watchKey();" in main)
check("the watchdog: a cover past its end at a key, or no frame drawn FROZEN_MS (2 s) after the key, recovers",
      "static final long FROZEN_MS = 2000;" in main and "if (coverStuck(now)) { recoverFrozen(" in main and "lastDrawAt < keyAt" in main
      and "addOnDrawListener(() -> lastDrawAt = SystemClock.uptimeMillis())" in main and "handler.postDelayed(frozenCheck, FROZEN_MS);" in main)
rec = main[main.index("    private void recoverFrozen(String why) {"):]
rec = rec[:rec.index("\n    }\n") + 6]
check("recovering drops every cover (the look's and the start picture), draws again, and logs the fixed word «frozen-screen recovered»",
      "homeLook.drop()" in rec and "snapOverlay = null;" in rec and "removeView(v)" in rec and "getDecorView().invalidate();" in rec and '"frozen-screen recovered (' in rec)
check("a recovery with no cover is logged once a run (build 620: a line every few seconds); one that dropped a cover is logged every time; the redraw always runs",
      "if (any || !frozenLogged) android.util.Log.w(" in rec and "if (!any) frozenLogged = true;" in rec
      and rec.index("getDecorView().invalidate();") < rec.index("if (any || !frozenLogged)") and "private boolean drawWatch, frozenLogged;" in main)
sys.exit(1 if failed else 0)
