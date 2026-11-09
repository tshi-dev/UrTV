#!/usr/bin/env python3
"""4.3 security and speed round (S1 S4 S5 S6 S8 S9 S11, P1 P2 P3 P6 P8 P9): the file:// gate in Java, and the wiring of each fix.

    python3 tests/security_43_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FileGate.java"), str(ROOT / "tests/FileGateRun.java")], capture_output=True, text=True)
        check("FileGate compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.FileGateRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every FileGate case ran", "ALL PASS" in out, out[-200:])
check("FileGate is the same in both apps (only the package line differs)", text(HOME / "FileGate.java").split("\n", 1)[1] == text(SAVER / "FileGate.java").split("\n", 1)[1])
card, view, page = text(HOME / "HomeCard.java"), text(SAVER / "ScreenView.java"), text(HOME / "HomePage.java")
check("S1: a page is taken from the screensaver provider only when it is sa.urtv.screensaver with our signature; both WebViews refuse every file:// request but the page and the assets",
      "providerIsOurs(c)" in page and "checkSignatures(c.getPackageName(), pi.packageName) == android.content.pm.PackageManager.SIGNATURE_MATCH" in page
      and 'FileGate.allowed(r.getUrl().getPath(), url == null ? null : Uri.parse(url).getPath())' in card and "FileGate.allowed(r.getUrl().getPath(), pagePath)" in view and "403" in card and "403" in view)
pc = text(SAVER / "PhotoCache.java")
check("S4 S9 P6: a hosted photo that failed is an empty 404 (not null); a kept one is answered without the download lock, its time renewed at most once an hour",
      "notFound()" in pc and "private static final Object DOWNLOAD = new Object();" in pc and "if (kept(f)) return f;" in pc and "TOUCH_EVERY_MS" in pc and "static synchronized File hosted" not in pc)
rcv = text(SAVER / "SaverReceiver.java")
check("S5: the update broadcast is finished in a finally; S11: putInt's answer is read", "finally { pending.finish(); }" in rcv and "if (!Settings.System.putInt(" in rcv)
dream = text(SAVER / "ScreenDream.java")
check("S6: the dream's page is built again only when the address it is opened with changed; S11: the renderer guards count with elapsedRealtime",
      "if (new Prefs(c).screenUrl(c, false).equals(shownUrl)) return;" in dream and "currentTimeMillis" not in dream.split("private void gone")[1] and "SystemClock.elapsedRealtime()" in card and "SystemClock.elapsedRealtime()" in text(SAVER / "MainActivity.java"))
cc = text(HOME / "ControlCenter.java")
check("S8: the network tile reads one WifiInfo and any RuntimeException leaves the panel open", "WifiInfo info = wm == null ? null : wm.getConnectionInfo();" in cc and "catch (RuntimeException e) {   // S8" in cc)
check("S11: the card's rectangle from the page is ignored when it has NaN, Infinity or an absurd number", "Float.isNaN(f) || Float.isInfinite(f) || Math.abs(f) > 20000f" in card)
main = text(HOME / "MainActivity.java")
check("P1: the card's settings, address and page copy are read off the UI thread; the card that is there goes on at once", 'new Thread(new Runnable() {' in card.split("void refresh()")[1].split("private int refreshGen")[0] and "if (web != null) resumeWeb();" in card)
check("P2: the apps' rows are built again at the return, not while another app is in front", "appsStale = true; return; }" in main and "if (appsStale) { appsStale = false; buildApps(); }" in main)
check("P3: the minute tick reads the screensaver's settings off the UI thread, one read at a time", "private void updateClock(ConfigSnapshot cfg)" in main and '"home-config"' in main and "if (clockReading) return;" in main)
check("P6: the card's and the dock's prefs are written when they changed, not at every return", 'if (!frag.equals(sp.getString("frag", null))' in card and 'if (!dockNow.equals(prefs().getString("dock", null)))' in main)
check("P8: a hosted photo is kept at screen size (1920 on the long side) in both apps", "PhotoShrink.toScreen(data)" in pc and "PhotoShrink.toScreen(data)" in main and "static final int MAX = 1920" in text(SAVER / "PhotoShrink.java"))
check("P9: a weak TV lets go of the card's page after ten minutes behind another app", "Ram.low(activity)) idle.postDelayed(drop, IDLE_DROP_MS)" in card and "IDLE_DROP_MS = 10 * 60 * 1000" in card)
sys.exit(1 if failed else 0)
