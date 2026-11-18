#!/usr/bin/env python3
"""4.4 (step 100, the owner's TCL G08 on Android 14): the safe page's «افتح الشاشة الأصلية» did nothing where an earlier setup had turned Google TV's home off.
Now a known stock home that is off (StockHome.KNOWN, Google's first; never the setup wizard) is turned back on over this app's adb key first, then opened;
with no key, or a TV that will not turn it on, the way by hand is written on the page. Never a silent press (R2). Source pins and the fake TV
(tests/SafeStockRun.java). Run: python3 tests/safe_stock_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SETUP = HOME / "setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

safe, hook, stock = text(HOME / "SafeActivity.java"), text(SETUP / "SetupHook.java"), text(HOME / "StockHome.java")
op = safe[safe.index("private void openStock()"):safe.index("private void launch()")]
check("the button is there when a stock home is on OR a known one is off (no dead page)", "boolean hasStock = stockHome() != null || disabledStock() != null;" in safe)
check("the press: a known stock home that is off is turned on first over our key (off the UI thread), then opened; else the way by hand; never silent",
      "StockHome.decide(off, stockHome() != null, sa.urtv.home.setup.SetupHook.hasKey(this))" in op and "new Thread(" in op and "SetupHook.enableStock(app, off)" in op
      and "launch(); else say(manual(off));" in op and "case StockHome.MANUAL: say(manual(off)); return;" in op and "default: say(S.tv(NO_STOCK));" in op)
check("what it did is written on the page itself (and a toast), not only a toast", "state = text(\"\", 16);" in safe and "if (state != null) state.setText(s);" in safe)
check("the way by hand names the app as the TV shows it (a turned-off app's label), and is in the English table",
      "MATCH_DISABLED_COMPONENTS" in safe and "%s ← تفعيل" in safe and "%s ← تفعيل" in text(ROOT / "i18n/en/home.tsv"))
check("the turn-on: only where this app's key exists (no question pops up on the safe page), through StockEnable",
      "if (!AdbKeys.exists(c)) return false;" in hook and "return StockEnable.run(s::run, pkg);" in hook)
check("the known stock homes are tvOS Launcher's list (Apache-2.0), Google's first, and the setup wizard is never one",
      "tvOS Launcher" in stock and "Apache-2.0" in stock and stock.index('"com.google.android.apps.tv.launcherx"') < stock.index('"com.tcl.cyberui"') and "setupwraith" not in stock.split("KNOWN = {")[1].split("};")[0])

if not shutil.which("javac"):
    print("SKIP the safe page scenarios: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in text(p)] + [HOME / "phone/UpdateRules.java", HOME / "SuggestedApps.java", HOME / "ExtrasWords.java", HOME / "StockHome.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/SafeStockRun.java")], capture_output=True, text=True)
    check("StockHome, StockEnable, the fake TV and the runner compile", r.returncode == 0, r.stderr[-1500:])
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.SafeStockRun"], capture_output=True, text=True)
    got = {l.split(" ", 1)[0]: l for l in out.stdout.splitlines()}
LX = "com.google.android.apps.tv.launcherx"
check("disabledKnown(): Google TV's home first; the setup wizard and other apps never; another maker's home yes", got.get("PICK") == f"PICK {LX} null com.tcl.cyberui null", got.get("PICK"))
check("decide(): a known one off: turn on (key) or the way by hand (no key), even if another «home» answers; else open; else nothing",
      got.get("DECIDE") == "DECIDE 1 2 0 3", got.get("DECIDE"))
check("the owner's TCL G08 (launcherx off): turned back on, and the TV says so", got.get("ENABLE") == "ENABLE true off=false cmds=1", (got.get("ENABLE"), out.stderr[-500:]))
check("a TV that will not turn it on: false (the page then shows the way by hand)", got.get("STUCK") == "STUCK false", got.get("STUCK"))
check("never the setup wizard, never an app that is not a stock home: nothing sent", got.get("NEVER") == "NEVER false false cmds=0", got.get("NEVER"))
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
