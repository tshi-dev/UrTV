#!/usr/bin/env python3
"""G3 (4.3.3): the page's data through the HTTP cache. fetchT asks with cache:'no-cache' (sent again each time with the copy's ETag or date: an unchanged file is a
304, the data still fresh), the WebViews keep that cache (LOAD_DEFAULT), and a periodic load is not tried while the TV says it is offline: each skipped one runs once
when the network is back (periodic and OFF, run on node with stubs).

    python3 tests/page_cache_test.py
"""
import re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return (ROOT / p).read_text(encoding="utf-8")

t = text("src/template.html")
check("fetchT revalidates (cache:'no-cache'), never 'no-store'", "fetch(url,{cache:'no-cache', signal:c.signal})" in t and "no-store" not in t)
check("the page has one fetch( only, fetchT's (every data file goes through it)", len(re.findall(r"\bfetch\(", t)) == 1, len(re.findall(r"\bfetch\(", t)))
for p in ("android/app/src/main/java/sa/urtv/screensaver/ScreenView.java", "android/launcher/src/main/java/sa/urtv/home/HomeCard.java", "android/launcher/src/main/java/sa/urtv/home/MatchesActivity.java"):
    s = text(p)
    check(f"{Path(p).name}: the WebView keeps the HTTP cache (LOAD_DEFAULT), not LOAD_NO_CACHE", "setCacheMode(WebSettings.LOAD_DEFAULT)" in s and "LOAD_NO_CACHE" not in s)

m = re.search(r"\nconst OFF = new Set\(\);\n.*?\nconst periodic = [^\n]*\n", t, re.S)
check("periodic and OFF are in the page", m is not None)
if m and shutil.which("node"):
    js = """
let online = true; const ev = {}; const timers = []; const intervals = [];
const navigator = { get onLine(){ return online; } };
const addEventListener = (n, f) => { ev[n] = f; };
const setTimeout = (f) => timers.push(f); const setInterval = (f) => intervals.push(f);
const spread = () => 0;
""" + m.group(0) + """
let n = 0; const load = () => { n++; };
periodic(load, 1000);
timers.shift()();                       // the first run: online
const a = n;
online = false; intervals[0](); intervals[0]();   // offline: skipped, and remembered once
const b = n;
online = true; ev.online();             // back: the skipped one runs once
const c = n;
ev.online();                            // nothing left
console.log(JSON.stringify([a, b, c, n]));
"""
    r = subprocess.run(["node", "-e", js], capture_output=True, text=True)
    check("online: it runs; offline: skipped; back online: the skipped load runs once, not twice", r.stdout.strip() == "[1,1,2,2]", r.stdout + r.stderr[-300:])
sys.exit(1 if failed else 0)
