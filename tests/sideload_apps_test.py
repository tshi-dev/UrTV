#!/usr/bin/env python3
"""#57: apps the person sideloaded show on the home. Next to a Google home an app with only a phone launcher activity stayed out,
as the maker's helpers do; now only one that came with the TV (a system app) stays out. AppRule.reason runs on a plain JVM; the
wiring (Dock's scan, the rebuild on a return after an install) is read from the source.   Run: python3 tests/sideload_apps_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

rule, dock, main = ((HOME / f).read_text(encoding="utf-8") for f in ("AppRule.java", "Dock.java", "MainActivity.java"))
check("AppRule has no Android in it", "import android" not in rule)
check("Dock's scan asks AppRule, with the app's system flags (FLAG_SYSTEM or an updated system app)",
      "AppRule.reason(" in dock and "FLAG_SYSTEM" in dock and "FLAG_UPDATED_SYSTEM_APP" in dock)
check("the favourites and «كل التطبيقات» both come from that one list (Dock.catalogue)",
      "Dock.catalogue(this)" in main and "for (ResolveInfo ri : catalogue(c))" in dock)
check("an app installed while the home was away: the rows are built again at the return (getChangedPackages, and the package receiver)",
      "getChangedPackages(packageSeq) != null) buildApps()" in main and "pf.addAction(Intent.ACTION_PACKAGE_ADDED)" in main and 'pf.addDataScheme("package")' in main)

CASES = [   # hidden shown config home usb googleHome launcherOnly system -> expected
    ((0, 0, 0, 0, 0, 1, 1, 0), "null", "#57: a sideloaded phone app next to a Google home shows"),
    ((0, 0, 0, 0, 0, 1, 1, 1), "NO_TV", "a maker's helper (system, phone activity only) next to a Google home stays out"),
    ((0, 0, 0, 0, 0, 1, 0, 1), "null", "a system TV app shows"),
    ((0, 0, 0, 0, 0, 0, 1, 1), "null", "next to a maker's own home, its phone-only apps show (its store, its settings)"),
    ((1, 0, 0, 0, 0, 1, 1, 0), "HIDDEN", "a sideloaded app the person hid stays hidden"),
    ((0, 1, 0, 0, 0, 1, 1, 1), "null", "a helper shown again with dock.sh show shows"),
    ((0, 0, 1, 0, 0, 0, 0, 0), "CONFIG", "config.xml's hidden apps stay out"),
    ((0, 0, 0, 1, 0, 0, 0, 0), "HOME", "another home screen stays out"),
    ((0, 0, 0, 0, 1, 1, 1, 1), "null", "a USB viewer shows next to a Google home"),
]
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    run = Path(tmp) / "AppRuleRun.java"
    lines = "\n".join('        say(AppRule.reason(%s));' % ", ".join("true" if v else "false" for v in c) for c, _, _ in CASES)
    run.write_text("package sa.urtv.home;\npublic final class AppRuleRun {\n"
                   "    static void say(String r) { System.out.println(r == null ? \"null\" : r.equals(AppRule.HIDDEN) ? \"HIDDEN\" : r.equals(AppRule.CONFIG) ? \"CONFIG\""
                   " : r.equals(AppRule.HOME) ? \"HOME\" : r.equals(AppRule.NO_TV) ? \"NO_TV\" : r); }\n"
                   "    public static void main(String[] a) {\n" + lines + "\n    }\n}\n", encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "AppRule.java"), str(run)], check=True, capture_output=True)
    got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.AppRuleRun"], check=True, capture_output=True, text=True).stdout.split()
    for (c, want, name), g in zip(CASES, got):
        check(name, g == want, g)
else:
    print("SKIP the Java cases: no javac")
print(f"{failed} failed" if failed else "all passed")
sys.exit(1 if failed else 0)
