#!/usr/bin/env python3
"""4.4 beta 3 (GitHub #57: «cinemana leanback», installed from outside the store on a TCL, missing from «كل التطبيقات»): an app the person installed
that also declares HOME is an app, not another home screen. A home that came with the TV or a known home screen stays out. AppRule.otherHome runs
on a plain JVM; Dock's wiring is read from the source.   Run: python3 tests/home_apps_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

dock = (HOME / "Dock.java").read_text(encoding="utf-8")
check("Dock's scan asks AppRule.otherHome with the HOME query's answer, the package and its system flag",
      "AppRule.otherHome(homes.contains(p), p, system)" in dock and "homes.contains(p), UsbViewers" not in dock)
check("tests/run.sh runs this test", "tests/home_apps_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
CASES = [   # declaresHome, pkg, system -> otherHome
    ((True, "com.shabakaty.cinemana", False), "false", "#57: a sideloaded TV app that also declares HOME shows"),
    ((True, "com.tcl.vendor.home", True), "true", "a home that came with the TV stays out"),
    ((True, "com.spocky.projengmenu", False), "true", "a known home screen (Projectivy) stays out"),
    ((True, "me.efesser.flauncher", False), "true", "a known home screen (FLauncher) stays out"),
    ((False, "com.example.app", False), "false", "an ordinary app is no home"),
    ((False, "com.example.sys", True), "false", "an ordinary system app is no home"),
]
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    run = Path(tmp) / "OtherHomeRun.java"
    lines = "\n".join('        System.out.println(AppRule.otherHome(%s, "%s", %s));' % ("true" if a else "false", b, "true" if c else "false") for (a, b, c), _, _ in CASES)
    run.write_text("package sa.urtv.home;\npublic final class OtherHomeRun {\n    public static void main(String[] x) {\n" + lines + "\n    }\n}\n", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "AppRule.java"), str(run)], capture_output=True, text=True)
    check("AppRule.java compiles", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.OtherHomeRun"], capture_output=True, text=True).stdout.split()
        for (case, want, name), g in zip(CASES, got + [""] * len(CASES)):
            check(name, g == want, g)
else:
    print("SKIP AppRule cases: no javac here")
sys.exit(1 if failed else 0)
