#!/usr/bin/env python3
"""4.4 (a user: «after the TV has been on a while, every video in YouTube and the other apps plays black; only a power off and on fixes it»): no
page of ours may keep working behind another app. Three checks:
1. the screensaver's full-screen activity (the home's «شاشة التوقف», the sleep timer's end without the add-on) frees its page and closes when
   another app comes in front (onStop), a rotation aside; it kept a live renderer for hours under the video app;
2. every class that makes a WebView frees or pauses it when it leaves the screen (onStop, onPause, its own pause, or the window's detach);
3. the page itself (src/template.html) skips every interval's work while it is hidden, and catches up (the clock, the panels) when it is seen
   again: run on node with a made-up document.
Run: python3 tests/black_video_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def body(s, head):
    if head not in s: return ""
    i = s.index(head) + len(head)
    depth, j = 1, i
    while depth and j < len(s):
        depth += {"{": 1, "}": -1}.get(s[j], 0); j += 1
    return s[i:j]

# 1. the screensaver's activity
ma = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/MainActivity.java").read_text(encoding="utf-8")
stop = body(ma, "protected void onStop() {")
check("1. the screensaver's activity frees its page and closes on stop (not on a rotation)",
      "if (!isChangingConfigurations()) {" in stop and "ScreenView.release(web);" in stop and "finish();" in stop and "web = null;" in stop
      and "snapHandler.removeCallbacksAndMessages(null);" in stop)
check("…and the sleep timer's screen is the same activity (no wake lock: the TV sleeps from it)", 'getBooleanExtra("sleeping", false)' in ma)

# 2. every WebView owner lets it go when it leaves the screen
makers = re.compile(r"ScreenView\.create(Preview)?\(|new WebView\(|WebViews\.make\(")
frees = re.compile(r"ScreenView\.release\(|\.destroy\(\)|\.onPause\(\)|previewOff\.run\(\)")
owners = {}
for p in sorted((ROOT / "android").glob("*/src/main/java/**/*.java")):
    s = re.sub(r"//[^\n]*", "", p.read_text(encoding="utf-8"))
    if p.name in ("ScreenView.java", "WebViews.java") or not makers.search(s): continue   # the two factories
    leave = "".join(body(s, h) for h in ("protected void onStop() {", "protected void onPause() {", "void onPause() {",
                                          "void pause() {", "public void onDetachedFromWindow() {"))
    owners[p.name] = bool(frees.search(leave))
check("2. the WebView owners are the ones this test knows (a new one is checked the same way)",
      set(owners) >= {"MainActivity.java", "ScreenDream.java", "SettingsActivity.java", "HomeCard.java", "MatchesActivity.java"}, sorted(owners))
check("2. each frees or pauses its WebView when it leaves the screen", all(owners.values()), [k for k, v in owners.items() if not v])

# 3. the page
tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
m = re.search(r"let PAGE_SKIPPED = false[^;]*;[^\n]*\n\(function\(\)\{.*?\n\}\)\(\);", tpl, re.S)
check("3. the page has its hidden gate, before any interval is made", m is not None and tpl.index("let PAGE_SKIPPED") < tpl.index("setInterval(") if m else False)
if not shutil.which("node") or m is None:
    print("SKIP the page on node")
    sys.exit(1 if failed else 0)
js = """
const timers = [];
const doc = { hidden: false, l: null, addEventListener(t, f) { if (t === 'visibilitychange') this.l = f; } };
const window = { setInterval(fn, ms) { timers.push(fn); return timers.length; } };
const document = doc;
let ticks = 0, panels = 0, work = 0;
function tickClock(){ ticks++; }
function renderPanel(){ panels++; }
""" + m.group(0) + """
window.setInterval(() => { work++; }, 1000);   // as the page's bare setInterval (window's, the gated one)
const run = () => timers.forEach(f => f());
run();                                  // seen: the work runs
doc.hidden = true; run(); run();        // hidden: skipped
const hiddenWork = work;
doc.hidden = false; doc.l();            // seen again: the clock and the panels catch up at once
console.log(JSON.stringify({ first: 1, hiddenWork, ticks, panels, afterNext: (run(), work) }));
"""
with tempfile.TemporaryDirectory() as d:
    f = Path(d) / "gate.js"; f.write_text(js, encoding="utf-8")
    out = subprocess.run(["node", str(f)], capture_output=True, text=True, timeout=30)
got = out.stdout.strip()
check("3. hidden: an interval's work is skipped; seen again: the clock and the panels at once, then the work as before",
      got == '{"first":1,"hiddenWork":1,"ticks":1,"panels":1,"afterNext":2}', got or out.stderr[-300:])
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
