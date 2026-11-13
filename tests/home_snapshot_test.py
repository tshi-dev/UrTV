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
      "SCALE = 0.5f" in snap and "CompressFormat.WEBP" in snap and "MAX_BYTES = 300 * 1024" in snap and "best.length > MAX_BYTES) {" in snap)
check("private: only this app's cache folder, written through a temp file, cleared by clear()", 'new File(c.getCacheDir(), DIR)' in snap and '".tmp"' in snap and "static void clear(Context c)" in snap and "getExternal" not in snap)
check("the window is copied (PixelCopy, so the WebView cards are in it) and a failure keeps the old picture", "PixelCopy.request(a.getWindow()" in snap and "r == PixelCopy.SUCCESS" in snap)
check("hotfix 4.3: a cold start draws the picture when there is a good one; with none there is NO overlay (never the skeleton's black blocks); the root is the look's colour, never black",
      "HomeSnapshot.skeleton(" not in main and "root.setBackgroundColor(GlassSource.look(this).base)" in main and "root.setBackgroundColor(Color.BLACK)" not in main and "if (b == null) {" in main)
check("hotfix 4.3: drawn at once at a cold start only; coming back from another app (the Activity only stopped) shows NO overlay: its live views are still there", "showSnapshot();   // a cold start" in main and main.count("showSnapshot();") == 1 and "protected void onRestart()" in main)
check("a new picture when the home goes to the background (not while the stand-in is in front) and every ten minutes while it is idle in front",
      "if (snapOverlay == null && WallFirstFrame.snapshotWorthKeeping(fullPhotoShown, appsShown, wallTrimmed)) HomeSnapshot.capture(this, homeRoot, null);" in main and "IDLE_MS = 10 * 60 * 1000" in snap and "handler.postDelayed(snapTick, HomeSnapshot.IDLE_MS)" in main)
check("it stays until the live views have painted (no page loading, at least 400 ms), at most 2.5 s, then a 220 ms fade; never a gap in between",
      "!(homeCard != null && homeCard.loading())" in main and "MIN_MS = 400" in snap and "LIVE_WAIT_MS = 2500" in snap and "FADE_MS = 220" in snap and "animate().alpha(0f).setDuration(HomeSnapshot.FADE_MS)" in main
      and "loading = true;" in card and "loading = false;" in card.split("public void onPageFinished")[1][:80])
check("a debug picture holds it (DebugSnapShot, in the debug manifest only)", (ROOT / "android/launcher/src/debug/java/sa/urtv/home/DebugSnapShot.java").is_file()
      and "DebugSnapShot" in (ROOT / "android/launcher/src/debug/AndroidManifest.xml").read_text(encoding="utf-8") and "DebugSnapShot" not in (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
sv = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverSnapshot.java").read_text(encoding="utf-8")
dream = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/ScreenDream.java").read_text(encoding="utf-8")
ss = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SaverStart.java").read_text(encoding="utf-8")
check("the screensaver page has the same rules: 24 h, 300 KB, half size WebP, private cache, keyed by the page address and the screen size",
      "MAX_AGE_MS = 24L * 60 * 60 * 1000" in sv and "MAX_BYTES = 300 * 1024" in sv and "SCALE = 0.5f" in sv and "getCacheDir()" in sv and "url.hashCode()" in sv and "now - madeAt <= MAX_AGE_MS" in sv)
check("…drawn when the dream starts, held until the live page has painted (alpha 1, at most 3 s), then a 220 ms fade; captured only once painted, when the dream ends",
      "SaverSnapshot.load(c, url" in ss and "SaverStart.begin(" in dream and "page.getAlpha() >= 1f" in ss and "LIVE_WAIT_MS = 3000" in sv and "FADE_MS = 220" in sv and "page.getAlpha() < 1f" in sv and "SaverSnapshot.capture(ScreenDream.this, getWindow(), shownUrl, web)" in dream)
check("…the picture is taken from the dream's window while the page shows (PixelCopy: 10 s after it is live, then every 3 minutes, a flat or failed copy kept out), never from the detached view at the end (a blank)",
      "PixelCopy.request(win, null, b," in sv and "FIRST_MS = 10_000" in sv and "EVERY_MS = 3 * 60 * 1000" in sv and "static boolean flat(Bitmap b)" in sv and "page.draw(" not in sv
      and "SaverSnapshot.capture(this, web" not in dream and "snapHandler.removeCallbacks(snapTick)" in dream)
par = (HOME / "Parental.java").read_text(encoding="utf-8")
check("«dock.sh pin reset» and the portfolio's wipe clear the home's picture (it may show what was wiped)", "HomeSnapshot.clear(c);" in par.split("Portfolio.wipe(c);")[1][:120] and "HomeSnapshot.clear(this);" in (HOME / "MarketsActivity.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
