#!/usr/bin/env python3
"""G3 (4.3.3): Net.cached, the apps' data files with a copy on the disk: ETag / If-Modified-Since (a 304 costs no body), calls for the same file merged, a back-off
for background calls after a failed connection, a size cap (tests/NetCacheRun.java on a plain JVM against a local server); and the callers that use it.

    python3 tests/net_cache_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return Path(p).read_text(encoding="utf-8")

check("the markets' files go through Net.cached (a background call: the back-off applies)", "Net.cached(new File(c.getCacheDir(), \"net\"), url," in text(HOME / "MarketFile.java") and "Net.get(url" not in text(HOME / "MarketFile.java"))
check("the next match goes through Net.cached (background)", "Net.cached(new java.io.File(c.getCacheDir(), \"net\"), url," in text(APP / "NextMatch.java"))
check("the leagues list goes through Net.cached, without the back-off (the person waits for it)", "Net.cached(new java.io.File(cache.getParentFile(), \"net\"), CatalogueLeagues.URL, 15000, 12000, Bounded.JSON_MAX, 0, false)" in text(APP / "SettingsActivity.java"))
if not shutil.which("javac"):
    print("SKIP the JVM run: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", tmp, str(APP / "Net.java"), str(APP / "NetLog.java"), str(ROOT / "tests/NetCacheRun.java")], capture_output=True, text=True)
    check("Net compiles on a plain JVM", c.returncode == 0, c.stderr[-500:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.NetCacheRun"], capture_output=True, text=True, timeout=120)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM run ended", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-800:])
sys.exit(1 if failed else 0)
