#!/usr/bin/env python3
"""optimize/precheck.sh + precheck.py: is it an Android TV, and is the WebView 80 or newer. Canned answers of the TV (a fake
tv.py); what only a real TV can confirm is what `pm list features` and `dumpsys webviewupdate` print on each make.

    python3 tests/precheck_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def batch(features, maker="TCL", webviewupdate="", webviews=()):
    rows = "\n".join(f"{p}     versionName={v}" for p, v in webviews)
    return ("$ pm list features\n" + features + "\n$ getprop ro.product.manufacturer\n" + maker + "\n$ dumpsys webviewupdate\n"
            + webviewupdate + "\n$ for p in $(pm list packages webview); do echo ...; done\n" + rows + "\n")

TV_FEATS = "feature:android.hardware.wifi\nfeature:android.software.leanback\nfeature:android.software.live_tv\n"
PHONE = "feature:android.hardware.telephony\nfeature:android.hardware.touchscreen\n"
UPD = "Current WebView package (name, version): ({}, {})\nMinimum WebView version code: 3283\n"

def run_py(mode, text):
    r = subprocess.run([sys.executable, "-I", str(ROOT / "optimize/precheck.py"), mode], input=text, capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr

rc, out = run_py("check", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "120.0.6099.144")))
check("Android TV, WebView 120: ok, nothing to do", rc == 0 and "Android TV" in out and "120" in out and "!!" not in out, out)
rc, out = run_py("check", batch(PHONE, "samsung"))
check("a phone (no leanback): stops with exit 3 and an Arabic message", rc == 3 and "Tizen" in out and "webOS" in out and "مو تلفزيون" in out
      and "nothing was changed" in out, out)
rc, out = run_py("check", batch("", "LG"))
check("no features at all (the TV did not answer): stops, but says so (exit 5), not «this is a phone»", rc == 5 and "did not say what it is" in out and "Tizen" not in out and "nothing was changed" in out, out)
rc, out = run_py("check", batch(TV_FEATS + "feature:amazon.hardware.fire_tv\n", "Amazon", UPD.format("com.amazon.webview.chromium", "92.0.4515.1")))
check("Fire TV: goes on, says it may work partly and the home switch may be refused", rc == 0 and "Fire TV" in out and "may work partly" in out and "refused" in out, out)
rc, out = run_py("check", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "66.0.3359.158")))
check("WebView 66 (the report): goes on, a !! line, the Arabic words, the Play Store page",
      rc == 0 and "!! WebView: system WebView is 66" in out and "Android System WebView" in out and "تحديث" in out and "webview-store" in out, out)
rc, out = run_py("check", batch(TV_FEATS, webviews=[("com.android.webview", "79.0.3945.116")]))
check("WebView 79 read from the package list (no webviewupdate): too old", "!! WebView" in out and "79" in out, out)
rc, out = run_py("check", batch(TV_FEATS, webviews=[("com.android.webview", "80.0.3987.162")]))
check("WebView 80 exactly: new enough", rc == 0 and "!!" not in out and "80" in out, out)
rc, out = run_py("check", batch(TV_FEATS, webviews=[("com.tcl.android.webview", "88.0.4324.93"), ("com.android.webview", "66.0.1")]))
check("two WebViews, no webviewupdate: the system one (com.android.webview) counts before the maker's", "66" in out, out)
rc, out = run_py("check", batch(TV_FEATS, webviews=[("com.tcl.android.webview", "88.0.4324.93")]))
check("the TV's own WebView alone: its version counts", rc == 0 and "88" in out and "!!" not in out, out)
rc, out = run_py("check", batch(TV_FEATS))
check("no WebView version readable: goes on, says it could not read it", rc == 0 and "does not say" in out and "!!" not in out, out)
rc, out = run_py("report", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "120.0.6099.144")))
check("report lines", out.splitlines() == ["Device: Android TV / Google TV", "WebView: 120 (com.google.android.webview)"], out)
rc, out = run_py("report", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "66.0.1")))
check("report line for an old WebView starts with !!", out.splitlines()[1].startswith("!! WebView: system WebView is 66"), out)
rc, out = run_py("report", batch(PHONE))
check("report line for a non-TV", out.splitlines()[0].startswith("Device: !!"), out)

rc, out = run_py("held", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "66.0.1")))
check("held: an old WebView exits 4, silently", rc == 4 and out == "", f"{rc} {out}")
rc, out = run_py("held", batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "120.0.1")))
check("held: a new WebView exits 0", rc == 0 and out == "", f"{rc} {out}")
rc, out = run_py("held", batch(TV_FEATS))
check("held: an unreadable WebView exits 5 (unknown holds the screensaver back)", rc == 5 and out == "", f"{rc} {out}")

# ---- the shell script, with a fake tv.py
t = Path(tempfile.mkdtemp())
(t / "optimize").mkdir()
for f in ("precheck.sh", "precheck.py"):
    shutil.copy(ROOT / "optimize" / f, t / "optimize")
# the fake is called as: py -I optimize/tv.py --batch   and   py -I optimize/precheck.py check
(t / "py").write_text('#!/bin/bash\nif [ "$2" = optimize/tv.py ]; then\n  if [ "$3" = --batch ]; then cat > /dev/null; cat "$CANNED"; else echo "$3" >> "$LOG"; fi\n'
                      'else exec python3 "$@"; fi\n')
(t / "py").chmod(0o755)
def sh(canned, *args):
    (t / "canned").write_text(canned); (t / "log").write_text("")
    r = subprocess.run(["bash", str(t / "optimize/precheck.sh"), *args], env=dict(os.environ, PY=str(t / "py"), CANNED=str(t / "canned"), LOG=str(t / "log")),
                       capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr, (t / "log").read_text()
rc, out, _ = sh(batch(TV_FEATS, webviewupdate=UPD.format("com.google.android.webview", "120.1")))
check("precheck.sh: an Android TV passes", rc == 0 and "ok" in out, out)
rc, out, _ = sh(batch(PHONE))
check("precheck.sh: a phone gives exit 3", rc == 3, out)
rc, out, _ = sh(batch(TV_FEATS), "report")
check("precheck.sh report", rc == 0 and out.startswith("Device: Android TV"), out)
rc, out, log = sh("", "webview-store")
check("precheck.sh webview-store opens the Play Store page of Android System WebView",
      rc == 0 and "market://details?id=com.google.android.webview" in log and "am start -a android.intent.action.VIEW" in log, log + out)

# the commands it sends are read-only to tv.py (no identity guard for a plain look)
spec_dir = ROOT / "optimize"
sys.path.insert(0, str(spec_dir))
src = (ROOT / "optimize/precheck.sh").read_text()
import re, types
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = object
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.9"
import tv
cmds = re.findall(r'^\s*(?:"([^"\n]*)"|\'([^\'\n]*)\')\s*\\$', src, re.M)
cmds = [a or b for a, b in cmds]
check("precheck.sh sends only commands tv.py counts as reading", len(cmds) == 6 and all(tv.is_read_only(c) for c in cmds), str([c for c in cmds if not tv.is_read_only(c)]))
print("\nall precheck checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
