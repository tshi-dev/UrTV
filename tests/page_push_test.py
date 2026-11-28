#!/usr/bin/env python3
"""4.4 beta 4: «شريط الأسواق» turned off did not reach a screensaver already on the screen. The dream now pushes a change the running page can take
(PagePush: only the markets line, #mkbar=) into it (window.urtvMkBar, tests/mkbar_push.test.mjs), and builds the page again for anything else, or
when an older page has no such function (it answers false). The settings' row tells a running screensaver (Prefs.CHANGED). PagePush (no Android)
on addresses, then the wiring in ScreenDream and SettingsActivity.
Run: python3 tests/page_push_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

dream = (APP / "ScreenDream.java").read_text(encoding="utf-8")
sett = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
rb = dream[dream.index("private void rebuildIfChanged("):dream.index("private void rebuild()")]
check("ScreenDream: a change the page can take is pushed into it (no rebuild), the address it shows is the new one",
      "PagePush.script(shownUrl, now)" in rb and "w.evaluateJavascript(push," in rb and "shownUrl = now;" in rb)
check("ScreenDream: an answer other than true (an older page) builds the page again; anything else is built again as before",
      'if (!"true".equals(r) && w == web) rebuild();' in rb and rb.count("rebuild();") == 2)
check("«شريط الأسواق» in the settings tells a running screensaver", "prefs.setMarketsOn(!prefs.marketsOn()); render(); sendBroadcast(new android.content.Intent(Prefs.CHANGED).setPackage(getPackageName()));" in sett)
for page in ("src/template.html", "dist/tv.html", "dist/tv-en.html"):
    s = (ROOT / page).read_text(encoding="utf-8")
    check(f"{page}: the page has urtvMkBar, and the line's requests follow it (WITH_LINE is recomputed)", "window.urtvMkBar = on =>" in s and "let WITH_LINE" in s)
check("tests/run.sh runs the browser half", "node tests/mkbar_push.test.mjs" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP page push: no javac here")
    sys.exit(1 if failed else 0)
B = "file:///android_asset/tv.html#city=x&tz=Asia/Riyadh&markets=tasi,spx&mkbar="
CASES = [
    ("on -> off, nothing else: pushed (false)", B + "on&clocksize=large", B + "off&clocksize=large", "(window.urtvMkBar?urtvMkBar(false):false)"),
    ("off -> on: pushed (true)", B + "off", B + "on", "(window.urtvMkBar?urtvMkBar(true):false)"),
    ("on -> off and the city changed too: built again", B + "on", B.replace("city=x", "city=y") + "off", "-"),
    ("the markets list changed: built again", B + "on", B.replace("tasi,spx", "tasi") + "on", "-"),
    ("another page file (the English one): built again", B + "on", B.replace("tv.html", "tv-en.html") + "off", "-"),
    ("the same address: nothing", B + "on", B + "on", "-"),
    ("a value that is not on or off: built again", B + "on", B + "maybe", "-"),
    ("the order of the other settings does not matter", "file:///p#a=1&mkbar=on&b=2", "file:///p#b=2&a=1&mkbar=off", "(window.urtvMkBar?urtvMkBar(false):false)"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(APP / "PagePush.java"), str(ROOT / "tests/PagePushRun.java")], capture_output=True, text=True)
    check("PagePush compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        args = [x for c in CASES for x in (c[1], c[2])]
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PagePushRun", *args], capture_output=True, text=True).stdout.strip().split("\n")
        for (name, _, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
