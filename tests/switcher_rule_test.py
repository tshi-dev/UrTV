#!/usr/bin/env python3
"""4.5 «التطبيقات المفتوحة» (mockups docs/mockups/tools-45/03-switcher.png, 03b): what it lists and may stop (SwitcherRule, run in
tests/SwitcherRuleRun.java), and the home's own ADB for a stop for good and the one-time usage access (SetupHook.forceStop / grantUsage): never
during a setup, never without the kept key, only `am force-stop <package>` reaches the shell.
Run: python3 tests/switcher_rule_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
rule = (H / "SwitcherRule.java").read_text(encoding="utf-8")
hook = (H / "setup/SetupHook.java").read_text(encoding="utf-8")
check("no Android in the rule; tvOS Launcher credited", "import android" not in rule and "techgenius1/tvOS-Launcher" in rule)
fs = hook.split("public static String forceStop(")[1].split("\n    }\n")[0]
check("stop for good: not during a setup, needs the kept key, only force-stop of a package name reaches the shell",
      fs.index('return "busy"') < fs.index("AdbKeys.exists(app)") < fs.index("SetupSession.open(") and 'cmd.matches("am force-stop [A-Za-z][A-Za-z0-9_]*(\\\\.[A-Za-z0-9_]+)+")' in fs)
gu = hook.split("public static boolean grantUsage(")[1].split("\n    }\n")[0]
check("usage access: given over our own ADB (GET_USAGE_STATS to our home only), never during a setup", 'appops set " + SetupEngine.SELF + " GET_USAGE_STATS allow' in gu and "SetupRunner.get().running()" in gu)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "SwitcherRule.java"), str(ROOT / "tests/SwitcherRuleRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SwitcherRuleRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every SwitcherRule case ran", "ALL PASS" in out, out[-300:])
sys.exit(1 if failed else 0)
