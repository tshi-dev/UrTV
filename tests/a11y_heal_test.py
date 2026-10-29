#!/usr/bin/env python3
"""The TV test of 157 -> 159 (and 159 -> 160): after the app's own update the TV had our TypeService in enabled_accessibility_services, the
switch accessibility_enabled at 0 and no service bound. A11yList.java on a plain JVM (the list, the decision), then source checks of the wiring:
A11yHeal runs on MY_PACKAGE_REPLACED, at boot and once when the home shows; it only writes with WRITE_SECURE_SETTINGS, puts the list back exactly
as it was, never adds another app's entry, logs under UrTVA11y; the phone page says control of other apps is stopped instead of doing nothing.
Needs javac for the Java cases. Run: python3 tests/a11y_heal_test.py"""
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


HOME = "android/launcher/src/main/java/sa/urtv/home/"
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / HOME / "A11yList.java"), str(ROOT / "tests/A11yListRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.A11yListRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every A11yList case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

heal = text(HOME + "A11yHeal.java")
man = text("android/launcher/src/main/AndroidManifest.xml")
rec = re.search(r'<receiver\s+android:name="\.A11yHeal".*?</receiver>', man, re.S)
check("A11yHeal receives MY_PACKAGE_REPLACED and BOOT_COMPLETED, only from the system (DUMP)", rec is not None and "MY_PACKAGE_REPLACED" in rec.group(0)
      and "BOOT_COMPLETED" in rec.group(0) and 'android:permission="android.permission.DUMP"' in rec.group(0), rec and rec.group(0))
check("the home runs the check once per process when it shows", "A11yHeal.onStart(this);" in text(HOME + "MainActivity.java") and "checkedThisProcess" in heal)
check("it writes only with WRITE_SECURE_SETTINGS and only what A11yList.decide says", "checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)" in heal
      and "if (!A11yList.HEAL.equals(d)) return;" in heal)
writes = re.findall(r"Settings\.Secure\.put\w+\([^;]*;", heal)
check("the only writes: the switch to 1, the list without ours, the list exactly as it was read", len(writes) == 3
      and "SWITCH, 1)" in writes[0] and "A11yList.without(list, TYPE, SLEEP)" in writes[1] and writes[2].endswith("LIST, list);"), writes)
check("the list is written only when Android runs none of it, the switch only when it is not 1", "if (!running) {" in heal and 'if (!"1".equals(sw))' in heal)
check("an entry the list lost is logged under UrTVA11y, never put back", 'TAG = "UrTVA11y"' in heal and "A11yList.lost(" in heal and "Log.w(TAG" in heal)
others = [p.name for p in (ROOT / "android").rglob("*.java") if p.name != "A11yHeal.java" and re.search(r"ACCESSIBILITY_ENABLED|ENABLED_ACCESSIBILITY_SERVICES", p.read_text(encoding="utf-8"))]
check("nothing else of ours touches the accessibility settings", not others, others)

act = text(HOME + "phone/PhoneActions.java")
check("the phone's state says when our service is listed but not running", 'm.put("a11y_stuck", sa.urtv.home.A11yHeal.stuck(app));' in act)
check("a key or a text into another app answers a11y_stuck then, not «turn the service on»", "if (!RemoteNav.FAILED.equals(r) && sa.urtv.home.A11yHeal.stuck(app)) return Result.fail(\"a11y_stuck\");" in act
      and 'case "service_off": return Result.fail(sa.urtv.home.A11yHeal.stuck(app) ? "a11y_stuck" : r);' in act)
page = text("android/launcher/src/main/assets/phone/index.html")
check("the page says «التحكم في التطبيقات الثانية متوقف: …» for the key, the text and the note", "a11y_stuck: STK" in page and "التحكم في التطبيقات الثانية متوقف: " in page
      and "s.a11y_stuck" in page)

print("\n" + (f"{failed} failed" if failed else "a11y heal: all passed"))
raise SystemExit(1 if failed else 0)
