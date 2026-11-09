#!/usr/bin/env python3
"""The home's stand-in picture (HomeSnapshot): the rules pinned from the source: what the key is made of, the age limit, the size limit, where it is kept, when
it is drawn, when it fades and how long it may wait, the skeleton when there is no usable picture, no black root.   python3 tests/home_snapshot_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

snap = (HOME / "HomeSnapshot.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
check("the key is the look, light or dark, the screen size and the language: any change and the picture is not used",
      'GlassSource.lookName(c) + "|" + (GlassSource.light(c) ? "l" : "d") + "|" + w + "x" + h + "|" + S.lang()' in snap and "madeKey.equals(nowKey)" in snap)
check("stale guard: older than 24 h (or from the future) is not drawn, the skeleton is", "MAX_AGE_MS = 24L * 60 * 60 * 1000" in snap and "now - madeAt <= MAX_AGE_MS" in snap and "now - madeAt >= 0" in snap)
check("half size, WebP, at most 300 KB (the quality steps down; too heavy at the lowest means nothing is kept)",
      "SCALE = 0.5f" in snap and "CompressFormat.WEBP" in snap and "MAX_BYTES = 300 * 1024" in snap and "best.length > MAX_BYTES) return;" in snap)
check("private: only this app's cache folder, written through a temp file, cleared by clear()", 'new File(c.getCacheDir(), DIR)' in snap and '".tmp"' in snap and "static void clear(Context c)" in snap and "getExternal" not in snap)
check("the window is copied (PixelCopy, so the WebView cards are in it) and a failure keeps the old picture", "PixelCopy.request(a.getWindow()" in snap and "r == PixelCopy.SUCCESS" in snap)
check("there is always something: the picture, else the skeleton in the look's colours (blocks where the top row, the card and the tray are); the root is the look's colour, never black",
      "HomeSnapshot.skeleton(this, GlassSource.look(this))" in main and "root.setBackgroundColor(GlassSource.look(this).base)" in main and "root.setBackgroundColor(Color.BLACK)" not in main and "l.panel" in snap)
check("drawn at once at the start and when coming back from another app, before the first frame", "showSnapshot();   // the home's last picture" in main and "protected void onRestart()" in main.split("protected void onRestart()")[0] + "protected void onRestart()")
check("a new picture when the home goes to the background (not while the stand-in is in front) and every ten minutes while it is idle in front",
      "if (snapOverlay == null) HomeSnapshot.capture(this, homeRoot, null);" in main and "IDLE_MS = 10 * 60 * 1000" in snap and "handler.postDelayed(snapTick, HomeSnapshot.IDLE_MS)" in main)
check("it stays until the live views have painted (no page loading, at least 400 ms), at most 2.5 s, then a 220 ms fade; never a gap in between",
      "!(homeCard != null && homeCard.loading())" in main and "MIN_MS = 400" in snap and "LIVE_WAIT_MS = 2500" in snap and "FADE_MS = 220" in snap and "animate().alpha(0f).setDuration(HomeSnapshot.FADE_MS)" in main
      and "loading = true;" in card and "loading = false;" in card.split("public void onPageFinished")[1][:80])
check("a debug picture holds it (DebugSnapShot, in the debug manifest only)", (ROOT / "android/launcher/src/debug/java/sa/urtv/home/DebugSnapShot.java").is_file()
      and "DebugSnapShot" in (ROOT / "android/launcher/src/debug/AndroidManifest.xml").read_text(encoding="utf-8") and "DebugSnapShot" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
